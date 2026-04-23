import Foundation
import FirebaseFirestore
import FirebaseFunctions

struct ShopifyAdminSettings: Equatable {
    var storeDomain: String = "k5t1sc-ps.myshopify.com"
    var storefrontAccessToken: String = ""
    var collectionHandles: [String] = []

    static let `default` = ShopifyAdminSettings()

    var activeCollectionLabel: String {
        switch collectionHandles.count {
        case 0:
            return "Alle Produkte"
        case 1:
            return collectionHandles[0]
        default:
            return "\(collectionHandles.count) Collections"
        }
    }

    var hasCollectionFilter: Bool {
        collectionHandles.isEmpty == false
    }

    var primaryCollectionHandle: String? {
        collectionHandles.first
    }

    var collectionHandlesDraft: String {
        collectionHandles.joined(separator: ", ")
    }
}

struct ShopifyCollectionOption: Equatable, Identifiable {
    let handle: String
    let title: String
    let productCount: Int?

    var id: String { handle }

    var displayTitle: String {
        title.trimmedNonEmpty ?? handle
    }
}

protocol ShopifyAdminSettingsServicing {
    func observeSettings(_ onChange: @escaping @MainActor (Result<ShopifyAdminSettings, Error>) -> Void) -> () -> Void
    func updateSettings(_ settings: ShopifyAdminSettings) async throws
    func fetchAvailableCollections() async throws -> [ShopifyCollectionOption]
}

final class FirestoreShopifyAdminSettingsService: ShopifyAdminSettingsServicing {
    private let firestore: Firestore
    private let functions: Functions
    private let collectionName = "appConfig"
    private let documentName = "shopifyMerch"

    init(
        firestore: Firestore = Firestore.firestore(),
        functions: Functions = Functions.functions(region: "us-central1")
    ) {
        self.firestore = firestore
        self.functions = functions
    }

    func observeSettings(_ onChange: @escaping @MainActor (Result<ShopifyAdminSettings, Error>) -> Void) -> () -> Void {
        let listener = firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                onChange(.success(Self.decode(snapshot?.data() ?? [:])))
            }
        }

        return {
            listener.remove()
        }
    }

    func updateSettings(_ settings: ShopifyAdminSettings) async throws {
        try await firestore.collection(collectionName).document(documentName).setData(Self.encode(settings), merge: true)
    }

    func fetchAvailableCollections() async throws -> [ShopifyCollectionOption] {
        let result = try await functions.invokeCallable("listShopifyCollections", payload: [:])
        let data = result.data as? [String: Any]
        let rawCollections = data?["collections"] as? [[String: Any]] ?? []

        return rawCollections.compactMap { entry in
            guard let handle = (entry["handle"] as? String)?.trimmedNonEmpty else {
                return nil
            }

            let title = (entry["title"] as? String)?.trimmedNonEmpty ?? handle
            let productCount = (entry["productCount"] as? NSNumber)?.intValue
            return ShopifyCollectionOption(handle: handle, title: title, productCount: productCount)
        }
    }

    private static func decode(_ data: [String: Any]) -> ShopifyAdminSettings {
        let configuredStorefrontURL = normalizeURLString(data["storefrontURL"] as? String)
        let normalizedDomain = normalizeStoreDomain(data["storeDomain"] as? String)
            ?? normalizeStoreDomain(configuredStorefrontURL)
            ?? ShopifyAdminSettings.default.storeDomain
        let normalizedCollectionHandles = normalizeCollectionHandles(
            rawValue: data["collectionHandles"],
            legacyValue: data["collectionHandle"] as? String,
            fallbackURL: configuredStorefrontURL
        )

        return ShopifyAdminSettings(
            storeDomain: normalizedDomain,
            storefrontAccessToken: (data["storefrontAccessToken"] as? String)?.trimmed ?? "",
            collectionHandles: normalizedCollectionHandles
        )
    }

    private static func encode(_ settings: ShopifyAdminSettings) -> [String: Any] {
        let normalizedDomain = normalizeStoreDomain(settings.storeDomain)
            ?? ShopifyAdminSettings.default.storeDomain
        let normalizedCollectionHandles = normalizeCollectionHandles(
            rawValue: settings.collectionHandles,
            legacyValue: nil,
            fallbackURL: nil
        )

        return [
            "storeDomain": normalizedDomain,
            "storefrontAccessToken": settings.storefrontAccessToken.trimmed,
            "collectionHandles": normalizedCollectionHandles,
            "collectionHandle": FieldValue.delete(),
            "storefrontURL": FieldValue.delete(),
            "collectionTitle": FieldValue.delete(),
            "updatedAt": FieldValue.serverTimestamp()
        ]
    }
}

@MainActor
final class ShopifyAdminSettingsStore: ObservableObject {
    static let shared = ShopifyAdminSettingsStore()

    @Published private(set) var settings: ShopifyAdminSettings = .default
    @Published private(set) var lastErrorMessage: String?
    @Published private(set) var availableCollections: [ShopifyCollectionOption] = []
    @Published private(set) var isLoadingCollections = false
    @Published private(set) var collectionsErrorMessage: String?

    private let service: ShopifyAdminSettingsServicing
    private var stopObserving: (() -> Void)?
    private var lastCollectionsRefreshAt: Date?
    private var lastCollectionsSourceKey: String?
    private let collectionsCacheLifetime: TimeInterval = 30

    init(service: ShopifyAdminSettingsServicing = FirestoreShopifyAdminSettingsService()) {
        self.service = service
        startObserving()
    }

    func save(_ settings: ShopifyAdminSettings) async throws {
        try await service.updateSettings(settings)
    }

    func refreshAvailableCollections(force: Bool = false) async {
        if isLoadingCollections { return }
        let sourceKey = collectionsSourceKey(for: settings)
        if force == false &&
            availableCollections.isEmpty == false &&
            collectionsErrorMessage == nil &&
            lastCollectionsSourceKey == sourceKey &&
            lastCollectionsRefreshAt.map({ Date().timeIntervalSince($0) < collectionsCacheLifetime }) == true {
            return
        }

        isLoadingCollections = true
        defer { isLoadingCollections = false }

        do {
            availableCollections = try await service.fetchAvailableCollections()
            collectionsErrorMessage = nil
            lastCollectionsRefreshAt = Date()
            lastCollectionsSourceKey = sourceKey
        } catch {
            collectionsErrorMessage = error.localizedDescription
        }
    }

    private func startObserving() {
        stopObserving?()
        stopObserving = service.observeSettings { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let settings):
                let previousSourceKey = self.collectionsSourceKey(for: self.settings)
                let nextSourceKey = self.collectionsSourceKey(for: settings)
                self.settings = settings
                self.lastErrorMessage = nil
                if previousSourceKey != nextSourceKey {
                    self.availableCollections = []
                    self.collectionsErrorMessage = nil
                    self.lastCollectionsRefreshAt = nil
                    self.lastCollectionsSourceKey = nil
                }
            case .failure(let error):
                self.lastErrorMessage = error.localizedDescription
            }
        }
    }

    deinit {
        stopObserving?()
    }

    private func collectionsSourceKey(for settings: ShopifyAdminSettings) -> String {
        let normalizedDomain = settings.storeDomain.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let normalizedToken = settings.storefrontAccessToken.trimmingCharacters(in: .whitespacesAndNewlines)
        return "\(normalizedDomain)|\(normalizedToken)"
    }
}

private func normalizeStoreDomain(_ value: String?) -> String? {
    guard let value else { return nil }
    let trimmed = value.trimmed
    guard !trimmed.isEmpty else { return nil }

    let withoutScheme = trimmed
        .replacingOccurrences(of: "https://", with: "")
        .replacingOccurrences(of: "http://", with: "")
    let domain = withoutScheme
        .split(separator: "/")
        .first
        .map(String.init)?
        .trimmingCharacters(in: .whitespacesAndNewlines)
        .lowercased()

    return domain?.isEmpty == false ? domain : nil
}

private func normalizeURLString(_ value: String?) -> String? {
    guard let value else { return nil }
    let trimmed = value.trimmed
    guard !trimmed.isEmpty else { return nil }

    if let url = URL(string: trimmed), let scheme = url.scheme, !scheme.isEmpty {
        return url.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    if let url = URL(string: "https://\(trimmed)") {
        return url.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    return nil
}

private func normalizeCollectionHandle(_ value: String?) -> String? {
    guard let direct = value?.trimmedNonEmpty else { return nil }

    return direct
        .replacingOccurrences(of: "/collections/", with: "")
        .split(separator: "/")
        .first
        .map(String.init)?
        .trimmedNonEmpty
}

private func normalizeCollectionHandles(
    rawValue: Any?,
    legacyValue: String?,
    fallbackURL: String?
) -> [String] {
    let candidates: [String]

    if let values = rawValue as? [String], values.isEmpty == false {
        candidates = values
    } else if let rawString = rawValue as? String, rawString.trimmedNonEmpty != nil {
        candidates = rawString
            .split(whereSeparator: \.isNewline)
            .flatMap { $0.split(separator: ",") }
            .map(String.init)
    } else if let legacyValue, legacyValue.trimmedNonEmpty != nil {
        candidates = legacyValue
            .split(whereSeparator: \.isNewline)
            .flatMap { $0.split(separator: ",") }
            .map(String.init)
    } else {
        candidates = []
    }

    let normalized = candidates.compactMap(normalizeCollectionHandle)
    if normalized.isEmpty == false {
        return Array(NSOrderedSet(array: normalized)) as? [String] ?? normalized
    }

    guard
        let fallbackURL,
        let url = URL(string: fallbackURL),
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
    else {
        return []
    }

    let pathComponents = components.path
        .split(separator: "/")
        .map(String.init)

    guard
        let collectionsIndex = pathComponents.firstIndex(of: "collections"),
        pathComponents.indices.contains(collectionsIndex + 1)
    else {
        return []
    }

    return [pathComponents[collectionsIndex + 1].trimmed].compactMap(normalizeCollectionHandle)
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
