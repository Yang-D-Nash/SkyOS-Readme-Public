import Combine
import CryptoKit
import FirebaseAuth
import FirebaseFunctions
import StoreKit
import UIKit

struct NativeAISubscriptionProduct: Identifiable, Equatable {
    let plan: UserQuotaPlan
    let productID: String
    let displayName: String
    let displayPrice: String
    let description: String

    var id: UserQuotaPlan { plan }
}

enum NativeAISubscriptionPurchaseOutcome: Equatable {
    case success(plan: UserQuotaPlan)
    case pending
    case cancelled
}

struct AISubscriptionSyncResult: Equatable {
    let status: String
    let plan: UserQuotaPlan?
    let provider: String?
    let currentPeriodEndEpochSeconds: Int?
}

protocol AISubscriptionSyncServicing {
    func syncIOSSubscription(signedTransactions: [String]) async throws -> AISubscriptionSyncResult
}

enum AISubscriptionSyncError: LocalizedError {
    case invalidResponse
    case missingSignedTransaction
    case missingCurrentUser
    case missingWindowScene
    case productNotConfigured

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Das KI-Abo konnte serverseitig nicht bestaetigt werden."
        case .missingSignedTransaction:
            return "StoreKit hat keine signierte Transaktion geliefert."
        case .missingCurrentUser:
            return "Bitte melde dich an, bevor du ein KI-Abo startest."
        case .missingWindowScene:
            return "Die Abo-Verwaltung konnte gerade nicht geoeffnet werden."
        case .productNotConfigured:
            return "Der gewaehlte KI-Plan ist in StoreKit noch nicht fertig konfiguriert."
        }
    }
}

struct FirebaseAISubscriptionSyncService: AISubscriptionSyncServicing {
    private let functions: Functions

    init(functions: Functions = Functions.functions(region: "us-central1")) {
        self.functions = functions
    }

    func syncIOSSubscription(signedTransactions: [String]) async throws -> AISubscriptionSyncResult {
        let result = try await functions.invokeCallable(
            "syncIosAiSubscriptionStatus",
            payload: ["signedTransactions": signedTransactions]
        )

        guard let payload = result.data as? [String: Any],
              let status = (payload["status"] as? String)?.trimmedNonEmpty else {
            throw AISubscriptionSyncError.invalidResponse
        }

        return AISubscriptionSyncResult(
            status: status,
            plan: (payload["plan"] as? String)?.trimmedNonEmpty.flatMap { UserQuotaPlan(rawValue: $0) },
            provider: (payload["provider"] as? String)?.trimmedNonEmpty,
            currentPeriodEndEpochSeconds: (payload["currentPeriodEndEpochSeconds"] as? NSNumber)?.intValue
        )
    }
}

@MainActor
final class NativeAISubscriptionStore: ObservableObject {
    @Published private(set) var products: [NativeAISubscriptionProduct] = []
    @Published private(set) var isLoadingProducts = false
    @Published private(set) var isSyncing = false
    @Published private(set) var activePurchasePlan: UserQuotaPlan?
    @Published private(set) var lastErrorMessage: String?

    private let paymentSettingsStore: PaymentMethodSettingsStore
    private let syncService: AISubscriptionSyncServicing
    private let onSubscriptionSynced: (@Sendable () async -> Void)?
    private var settingsCancellable: AnyCancellable?
    private var transactionUpdatesTask: Task<Void, Never>?
    private var currentConfig: NativeAISubscriptionConfig
    private var loadedProductCatalog: [UserQuotaPlan: Product] = [:]
    private var lastSyncedEntitlementSignature = ""

    init(
        paymentSettingsStore: PaymentMethodSettingsStore? = nil,
        syncService: AISubscriptionSyncServicing = FirebaseAISubscriptionSyncService(),
        onSubscriptionSynced: (@Sendable () async -> Void)? = nil
    ) {
        self.paymentSettingsStore = paymentSettingsStore ?? .shared
        self.syncService = syncService
        self.onSubscriptionSynced = onSubscriptionSynced
        self.currentConfig = NativeAISubscriptionConfig(settings: self.paymentSettingsStore.settings)
        observePaymentSettings()
        startTransactionUpdatesListener()
    }

    deinit {
        settingsCancellable?.cancel()
        transactionUpdatesTask?.cancel()
    }

    var isStorefrontReady: Bool {
        currentConfig.isStoreKitReady
    }

    func product(for plan: UserQuotaPlan) -> NativeAISubscriptionProduct? {
        products.first(where: { $0.plan == plan })
    }

    func prepareStorefront(for user: User?) async {
        await refreshProductsIfNeeded(force: false)

        guard currentConfig.isStoreKitReady else {
            return
        }

        let shouldForceEmptySync = user?.normalizedAISubscriptionProvider == "app_store"
        do {
            _ = try await synchronizeCurrentEntitlements(forceEmptySync: shouldForceEmptySync)
        } catch {
            lastErrorMessage = error.localizedDescription
        }
    }

    func purchase(plan: UserQuotaPlan) async throws -> NativeAISubscriptionPurchaseOutcome {
        await refreshProductsIfNeeded(force: false)

        guard let product = loadedProductCatalog[plan] else {
            throw AISubscriptionSyncError.productNotConfigured
        }

        guard let currentUserID = currentUserID else {
            throw AISubscriptionSyncError.missingCurrentUser
        }

        let appAccountToken = try Self.deterministicAppAccountToken(for: currentUserID)
        activePurchasePlan = plan
        defer { activePurchasePlan = nil }

        let result = try await product.purchase(options: [.appAccountToken(appAccountToken)])
        switch result {
        case .success(let verificationResult):
            guard case .verified(let transaction) = verificationResult else {
                throw AISubscriptionSyncError.missingSignedTransaction
            }

            let jwsRepresentation = verificationResult.jwsRepresentation.trimmedNonEmpty
            guard let jwsRepresentation else {
                throw AISubscriptionSyncError.missingSignedTransaction
            }

            _ = try await syncSignedTransactions([jwsRepresentation], forceEmptySync: false)
            await transaction.finish()
            return .success(plan: plan)
        case .pending:
            return .pending
        case .userCancelled:
            return .cancelled
        @unknown default:
            throw AISubscriptionSyncError.invalidResponse
        }
    }

    func restorePurchases(forceEmptySync: Bool = false) async throws {
        try await AppStore.sync()
        _ = try await synchronizeCurrentEntitlements(forceEmptySync: forceEmptySync)
    }

    func manageSubscriptions() async throws {
        guard let scene = currentWindowScene else {
            throw AISubscriptionSyncError.missingWindowScene
        }

        try await AppStore.showManageSubscriptions(in: scene)
    }

    private func observePaymentSettings() {
        settingsCancellable = paymentSettingsStore.$settings
            .sink { [weak self] settings in
                guard let self else { return }
                Task { @MainActor in
                    await self.handleSettingsUpdate(settings)
                }
            }
    }

    private func handleSettingsUpdate(_ settings: PaymentMethodSettings) async {
        let updatedConfig = NativeAISubscriptionConfig(settings: settings)
        guard updatedConfig != currentConfig else {
            return
        }

        currentConfig = updatedConfig
        lastSyncedEntitlementSignature = ""

        if !updatedConfig.isStoreKitReady {
            loadedProductCatalog = [:]
            products = []
            lastErrorMessage = nil
            return
        }

        await refreshProductsIfNeeded(force: true)
    }

    private func refreshProductsIfNeeded(force: Bool) async {
        guard currentConfig.isStoreKitReady else {
            loadedProductCatalog = [:]
            products = []
            return
        }

        guard force || loadedProductCatalog.keys.sortedByRawValue != currentConfig.productIDsByPlan.keys.sortedByRawValue else {
            return
        }

        isLoadingProducts = true
        defer { isLoadingProducts = false }

        do {
            let productIDs = Array(currentConfig.productIDsByPlan.values)
            let loadedProducts = try await Product.products(for: productIDs)
            var catalog: [UserQuotaPlan: Product] = [:]

            for product in loadedProducts {
                if let plan = currentConfig.plan(for: product.id) {
                    catalog[plan] = product
                }
            }

            loadedProductCatalog = catalog
            products = currentConfig.displayProducts(from: catalog)
            lastErrorMessage = nil
        } catch {
            loadedProductCatalog = [:]
            products = []
            lastErrorMessage = "StoreKit-Produkte konnten nicht geladen werden: \(error.localizedDescription)"
        }
    }

    private func startTransactionUpdatesListener() {
        transactionUpdatesTask = Task(priority: .background) { [weak self] in
            guard let self else { return }

            for await result in Transaction.updates {
                await self.handleTransactionUpdate(result)
            }
        }
    }

    private func handleTransactionUpdate(_ result: VerificationResult<Transaction>) async {
        guard currentConfig.isStoreKitReady else {
            return
        }

        switch result {
        case .verified(let transaction):
            guard currentConfig.plan(for: transaction.productID) != nil,
                  let jwsRepresentation = result.jwsRepresentation.trimmedNonEmpty else {
                return
            }

            do {
                _ = try await syncSignedTransactions([jwsRepresentation], forceEmptySync: false)
                await transaction.finish()
            } catch {
                lastErrorMessage = error.localizedDescription
            }
        case .unverified:
            lastErrorMessage = "Eine StoreKit-Transaktion konnte nicht verifiziert werden."
        }
    }

    private func synchronizeCurrentEntitlements(forceEmptySync: Bool) async throws -> AISubscriptionSyncResult? {
        let signedTransactions = await collectCurrentEntitlementJWS()
        if signedTransactions.isEmpty && !forceEmptySync {
            return nil
        }

        return try await syncSignedTransactions(signedTransactions, forceEmptySync: forceEmptySync)
    }

    private func syncSignedTransactions(
        _ signedTransactions: [String],
        forceEmptySync: Bool
    ) async throws -> AISubscriptionSyncResult {
        guard currentConfig.isStoreKitReady else {
            throw AISubscriptionSyncError.productNotConfigured
        }

        guard let currentUserID = currentUserID else {
            throw AISubscriptionSyncError.missingCurrentUser
        }

        if signedTransactions.isEmpty && !forceEmptySync {
            throw AISubscriptionSyncError.missingSignedTransaction
        }

        let signature = ([currentUserID] + signedTransactions.sorted()).joined(separator: "|")
        if signature == lastSyncedEntitlementSignature && !forceEmptySync {
            return AISubscriptionSyncResult(
                status: "cached",
                plan: nil,
                provider: nil,
                currentPeriodEndEpochSeconds: nil
            )
        }

        isSyncing = true
        defer { isSyncing = false }

        let result = try await syncService.syncIOSSubscription(signedTransactions: signedTransactions)
        lastSyncedEntitlementSignature = signature
        lastErrorMessage = nil
        if let onSubscriptionSynced {
            await onSubscriptionSynced()
        }
        return result
    }

    private func collectCurrentEntitlementJWS() async -> [String] {
        guard currentConfig.isStoreKitReady else {
            return []
        }

        var signedTransactions: [String] = []
        for await result in Transaction.currentEntitlements {
            guard case .verified(let transaction) = result,
                  currentConfig.plan(for: transaction.productID) != nil,
                  let jwsRepresentation = result.jwsRepresentation.trimmedNonEmpty else {
                continue
            }

            signedTransactions.append(jwsRepresentation)
        }

        return signedTransactions
    }

    private var currentUserID: String? {
        Auth.auth().currentUser?.uid.trimmedNonEmpty
    }

    private var currentWindowScene: UIWindowScene? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .sorted { left, right in
                Self.sceneRank(for: left.activationState) < Self.sceneRank(for: right.activationState)
            }
            .first
    }

    private static func sceneRank(for state: UIScene.ActivationState) -> Int {
        switch state {
        case .foregroundActive: return 0
        case .foregroundInactive: return 1
        case .background: return 2
        case .unattached: return 3
        @unknown default: return 4
        }
    }

    private static func deterministicAppAccountToken(for userID: String) throws -> UUID {
        guard let namespace = UUID(uuidString: "8fadb2be-b2a4-4cd4-b17d-7aa3c3ddf4e5") else {
            throw AISubscriptionSyncError.invalidResponse
        }

        var data = Data()
        var namespaceBytes = namespace.uuid
        withUnsafeBytes(of: &namespaceBytes) { data.append(contentsOf: $0) }
        data.append(contentsOf: userID.utf8)

        let digest = Insecure.SHA1.hash(data: data)
        var bytes = Array(digest.prefix(16))
        bytes[6] = (bytes[6] & 0x0F) | 0x50
        bytes[8] = (bytes[8] & 0x3F) | 0x80

        return UUID(uuid: (
            bytes[0], bytes[1], bytes[2], bytes[3],
            bytes[4], bytes[5], bytes[6], bytes[7],
            bytes[8], bytes[9], bytes[10], bytes[11],
            bytes[12], bytes[13], bytes[14], bytes[15]
        ))
    }
}

private struct NativeAISubscriptionConfig: Equatable {
    let isEnabled: Bool
    let creatorProductID: String
    let studioProductID: String
    let appleAppID: String

    init(settings: PaymentMethodSettings) {
        let aiSubscriptions = settings.aiSubscriptions
        self.isEnabled = aiSubscriptions.enabled
        self.creatorProductID = aiSubscriptions.iosCreatorProductID.trimmed
        self.studioProductID = aiSubscriptions.iosStudioProductID.trimmed
        self.appleAppID = aiSubscriptions.iosAppAppleID.trimmed
    }

    var isStoreKitReady: Bool {
        isEnabled &&
        !creatorProductID.isEmpty &&
        !studioProductID.isEmpty &&
        !appleAppID.isEmpty
    }

    var productIDsByPlan: [UserQuotaPlan: String] {
        var values: [UserQuotaPlan: String] = [:]
        if !creatorProductID.isEmpty {
            values[.creator] = creatorProductID
        }
        if !studioProductID.isEmpty {
            values[.studio] = studioProductID
        }
        return values
    }

    func plan(for productID: String) -> UserQuotaPlan? {
        productIDsByPlan.first(where: { $0.value == productID })?.key
    }

    func displayProducts(from catalog: [UserQuotaPlan: Product]) -> [NativeAISubscriptionProduct] {
        let orderedPlans: [UserQuotaPlan] = [.creator, .studio]
        return orderedPlans.compactMap { plan in
            guard let product = catalog[plan] else {
                return nil
            }

            return NativeAISubscriptionProduct(
                plan: plan,
                productID: product.id,
                displayName: product.displayName,
                displayPrice: product.displayPrice,
                description: product.description
            )
        }
    }
}

private extension Collection where Element == UserQuotaPlan {
    var sortedByRawValue: [UserQuotaPlan] {
        sorted { $0.rawValue < $1.rawValue }
    }
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var trimmedNonEmpty: String? {
        let value = trimmed
        return value.isEmpty ? nil : value
    }
}
