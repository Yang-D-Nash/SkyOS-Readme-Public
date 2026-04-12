import Foundation
import Combine
import FirebaseFirestore

struct PaymentProviderSettings: Equatable {
    var connected: Bool = false
    var enabled: Bool = false
    var accountHint: String = ""
}

struct BankTransferSettings: Equatable {
    var enabled: Bool = false
    var accountHolder: String = ""
    var iban: String = ""
    var bic: String = ""
    var bankName: String = ""
    var paymentInstructions: String = ""

    var isConfigured: Bool {
        !accountHolder.trimmed.isEmpty &&
        !iban.trimmed.isEmpty &&
        !bankName.trimmed.isEmpty
    }
}

struct AISubscriptionPricingSettings: Equatable {
    var enabled: Bool = false
    var creatorPriceID: String = ""
    var studioPriceID: String = ""

    var isConfigured: Bool {
        !creatorPriceID.trimmed.isEmpty && !studioPriceID.trimmed.isEmpty
    }
}

struct PaymentMethodSettings: Equatable {
    var stripe: PaymentProviderSettings = .init()
    var paypal: PaymentProviderSettings = .init()
    var klarna: PaymentProviderSettings = .init()
    var bankTransfer: BankTransferSettings = .init()
    var aiSubscriptions: AISubscriptionPricingSettings = .init()

    static let `default` = PaymentMethodSettings()

    var checkoutMethodLabels: [String] {
        var labels: [String] = []
        if stripe.connected && stripe.enabled {
            labels.append("Stripe")
        }
        if paypal.connected && paypal.enabled {
            labels.append("PayPal")
        }
        if klarna.connected && klarna.enabled {
            labels.append("Klarna")
        }
        if bankTransfer.enabled && bankTransfer.isConfigured {
            labels.append("Bankueberweisung")
        }
        return labels
    }
}

protocol PaymentMethodSettingsServicing {
    func observeSettings(_ onChange: @escaping @MainActor (Result<PaymentMethodSettings, Error>) -> Void) -> () -> Void
    func updateSettings(_ settings: PaymentMethodSettings) async throws
}

final class FirestorePaymentMethodSettingsService: PaymentMethodSettingsServicing {
    private let firestore: Firestore
    private let collectionName = "appConfig"
    private let documentName = "paymentMethods"

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func observeSettings(_ onChange: @escaping @MainActor (Result<PaymentMethodSettings, Error>) -> Void) -> () -> Void {
        let listener = firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                let data = snapshot?.data() ?? [:]
                onChange(.success(Self.decode(data)))
            }
        }

        return {
            listener.remove()
        }
    }

    func updateSettings(_ settings: PaymentMethodSettings) async throws {
        try await firestore.collection(collectionName).document(documentName).setData(Self.encode(settings), merge: true)
    }

    private static func decode(_ data: [String: Any]) -> PaymentMethodSettings {
        let stripe = decodeProvider(data["stripe"] as? [String: Any])
        let paypal = decodeProvider(data["paypal"] as? [String: Any])
        let klarna = decodeProvider(data["klarna"] as? [String: Any])
        let bankTransfer = decodeBankTransfer(data["bankTransfer"] as? [String: Any])
        let aiSubscriptions = decodeAISubscriptions(data["aiSubscriptions"] as? [String: Any])
        return PaymentMethodSettings(
            stripe: stripe,
            paypal: paypal,
            klarna: klarna,
            bankTransfer: bankTransfer,
            aiSubscriptions: aiSubscriptions
        )
    }

    private static func decodeProvider(_ data: [String: Any]?) -> PaymentProviderSettings {
        PaymentProviderSettings(
            connected: data?["connected"] as? Bool ?? false,
            enabled: data?["enabled"] as? Bool ?? false,
            accountHint: data?["accountHint"] as? String ?? ""
        )
    }

    private static func decodeBankTransfer(_ data: [String: Any]?) -> BankTransferSettings {
        BankTransferSettings(
            enabled: data?["enabled"] as? Bool ?? false,
            accountHolder: data?["accountHolder"] as? String ?? "",
            iban: data?["iban"] as? String ?? "",
            bic: data?["bic"] as? String ?? "",
            bankName: data?["bankName"] as? String ?? "",
            paymentInstructions: data?["paymentInstructions"] as? String ?? ""
        )
    }

    private static func decodeAISubscriptions(_ data: [String: Any]?) -> AISubscriptionPricingSettings {
        AISubscriptionPricingSettings(
            enabled: data?["enabled"] as? Bool ?? false,
            creatorPriceID: data?["creatorPriceId"] as? String ?? "",
            studioPriceID: data?["studioPriceId"] as? String ?? ""
        )
    }

    private static func encode(_ settings: PaymentMethodSettings) -> [String: Any] {
        [
            "stripe": [
                "connected": settings.stripe.connected,
                "enabled": settings.stripe.enabled,
                "accountHint": settings.stripe.accountHint.trimmed
            ],
            "paypal": [
                "connected": settings.paypal.connected,
                "enabled": settings.paypal.enabled,
                "accountHint": settings.paypal.accountHint.trimmed
            ],
            "klarna": [
                "connected": settings.klarna.connected,
                "enabled": settings.klarna.enabled,
                "accountHint": settings.klarna.accountHint.trimmed
            ],
            "bankTransfer": [
                "enabled": settings.bankTransfer.enabled,
                "accountHolder": settings.bankTransfer.accountHolder.trimmed,
                "iban": settings.bankTransfer.iban.trimmed,
                "bic": settings.bankTransfer.bic.trimmed,
                "bankName": settings.bankTransfer.bankName.trimmed,
                "paymentInstructions": settings.bankTransfer.paymentInstructions.trimmed
            ],
            "aiSubscriptions": [
                "enabled": settings.aiSubscriptions.enabled,
                "creatorPriceId": settings.aiSubscriptions.creatorPriceID.trimmed,
                "studioPriceId": settings.aiSubscriptions.studioPriceID.trimmed
            ],
            "updatedAt": FieldValue.serverTimestamp()
        ]
    }
}

@MainActor
final class PaymentMethodSettingsStore: ObservableObject {
    static let shared = PaymentMethodSettingsStore()

    @Published private(set) var settings: PaymentMethodSettings = .default
    @Published private(set) var lastErrorMessage: String?

    private let service: PaymentMethodSettingsServicing
    private var stopObserving: (() -> Void)?

    init(service: PaymentMethodSettingsServicing = FirestorePaymentMethodSettingsService()) {
        self.service = service
        startObserving()
    }

    func save(_ settings: PaymentMethodSettings) async throws {
        try await service.updateSettings(settings)
    }

    private func startObserving() {
        stopObserving?()
        stopObserving = service.observeSettings { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let settings):
                self.settings = settings
                self.lastErrorMessage = nil
            case .failure(let error):
                self.lastErrorMessage = error.localizedDescription
            }
        }
    }

    deinit {
        stopObserving?()
    }
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
