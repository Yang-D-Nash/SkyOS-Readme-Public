import Foundation
import FirebaseFirestore

struct ShopifyAdminSettings: Equatable {
    var storeDomain: String = "k5t1sc-ps.myshopify.com"
    var storefrontAccessToken: String = ""
    var collectionHandle: String = ""

    static let `default` = ShopifyAdminSettings()

    var activeCollectionLabel: String {
        collectionHandle.trimmedNonEmpty ?? "Alle Produkte"
    }

    var hasCollectionFilter: Bool {
        collectionHandle.trimmedNonEmpty != nil
    }
}

protocol ShopifyAdminSettingsServicing {
    func observeSettings(_ onChange: @escaping @MainActor (Result<ShopifyAdminSettings, Error>) -> Void) -> () -> Void
    func updateSettings(_ settings: ShopifyAdminSettings) async throws
}

final class FirestoreShopifyAdminSettingsService: ShopifyAdminSettingsServicing {
    private let firestore: Firestore
    private let collectionName = "appConfig"
    private let documentName = "shopifyMerch"

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
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

    private static func decode(_ data: [String: Any]) -> ShopifyAdminSettings {
        let configuredStorefrontURL = normalizeURLString(data["storefrontURL"] as? String)
        let normalizedDomain = normalizeStoreDomain(data["storeDomain"] as? String)
            ?? normalizeStoreDomain(configuredStorefrontURL)
            ?? ShopifyAdminSettings.default.storeDomain
        let normalizedCollectionHandle = normalizeCollectionHandle(
            data["collectionHandle"] as? String,
            fallbackURL: configuredStorefrontURL
        ) ?? ""

        return ShopifyAdminSettings(
            storeDomain: normalizedDomain,
            storefrontAccessToken: (data["storefrontAccessToken"] as? String)?.trimmed ?? "",
            collectionHandle: normalizedCollectionHandle
        )
    }

    private static func encode(_ settings: ShopifyAdminSettings) -> [String: Any] {
        let normalizedDomain = normalizeStoreDomain(settings.storeDomain)
            ?? ShopifyAdminSettings.default.storeDomain
        let normalizedCollectionHandle = normalizeCollectionHandle(
            settings.collectionHandle,
            fallbackURL: nil
        ) ?? ""

        return [
            "storeDomain": normalizedDomain,
            "storefrontAccessToken": settings.storefrontAccessToken.trimmed,
            "collectionHandle": normalizedCollectionHandle,
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

    private let service: ShopifyAdminSettingsServicing
    private var stopObserving: (() -> Void)?

    init(service: ShopifyAdminSettingsServicing = FirestoreShopifyAdminSettingsService()) {
        self.service = service
        startObserving()
    }

    func save(_ settings: ShopifyAdminSettings) async throws {
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

private func normalizeCollectionHandle(_ value: String?, fallbackURL: String?) -> String? {
    if let direct = value?.trimmedNonEmpty {
        return direct
            .replacingOccurrences(of: "/collections/", with: "")
            .split(separator: "/")
            .first
            .map(String.init)?
            .trimmed
    }

    guard
        let fallbackURL,
        let url = URL(string: fallbackURL),
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
    else {
        return nil
    }

    let pathComponents = components.path
        .split(separator: "/")
        .map(String.init)

    guard
        let collectionsIndex = pathComponents.firstIndex(of: "collections"),
        pathComponents.indices.contains(collectionsIndex + 1)
    else {
        return nil
    }

    return pathComponents[collectionsIndex + 1].trimmed
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
