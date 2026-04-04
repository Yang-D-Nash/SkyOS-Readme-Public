import Foundation
import FirebaseFirestore

struct ScreenHeaderSettings: Codable, Equatable {
    var homeImageURL: String = ""
    var homeEyebrow: String = ""
    var homeTitle: String = ""
    var homeSubtitle: String = ""
    var homeDetail: String = ""
    var musicHubImageURL: String = ""
    var musicHubEyebrow: String = ""
    var musicHubTitle: String = ""
    var musicHubSubtitle: String = ""
    var musicHubDetail: String = ""
    var shopImageURL: String = ""
    var shopEyebrow: String = ""
    var shopTitle: String = ""
    var shopSubtitle: String = ""
    var shopDetail: String = ""
    var videoHubImageURL: String = ""
    var videoHubEyebrow: String = ""
    var videoHubTitle: String = ""
    var videoHubSubtitle: String = ""
    var videoHubDetail: String = ""

    static let `default` = ScreenHeaderSettings()

    var configuredCount: Int {
        [
            [homeImageURL, homeEyebrow, homeTitle, homeSubtitle, homeDetail],
            [musicHubImageURL, musicHubEyebrow, musicHubTitle, musicHubSubtitle, musicHubDetail],
            [shopImageURL, shopEyebrow, shopTitle, shopSubtitle, shopDetail],
            [videoHubImageURL, videoHubEyebrow, videoHubTitle, videoHubSubtitle, videoHubDetail]
        ]
            .filter { fields in
                fields.contains { !$0.trimmed.isEmpty }
            }
            .count
    }

    var resolvedHomeImageURL: String? { homeImageURL.trimmed.nilIfEmpty }
    var resolvedHomeEyebrow: String? { homeEyebrow.trimmed.nilIfEmpty }
    var resolvedHomeTitle: String? { homeTitle.trimmed.nilIfEmpty }
    var resolvedHomeSubtitle: String? { homeSubtitle.trimmed.nilIfEmpty }
    var resolvedHomeDetail: String? { homeDetail.trimmed.nilIfEmpty }
    var resolvedMusicHubImageURL: String? { musicHubImageURL.trimmed.nilIfEmpty }
    var resolvedMusicHubEyebrow: String? { musicHubEyebrow.trimmed.nilIfEmpty }
    var resolvedMusicHubTitle: String? { musicHubTitle.trimmed.nilIfEmpty }
    var resolvedMusicHubSubtitle: String? { musicHubSubtitle.trimmed.nilIfEmpty }
    var resolvedMusicHubDetail: String? { musicHubDetail.trimmed.nilIfEmpty }
    var resolvedShopImageURL: String? { shopImageURL.trimmed.nilIfEmpty }
    var resolvedShopEyebrow: String? { shopEyebrow.trimmed.nilIfEmpty }
    var resolvedShopTitle: String? { shopTitle.trimmed.nilIfEmpty }
    var resolvedShopSubtitle: String? { shopSubtitle.trimmed.nilIfEmpty }
    var resolvedShopDetail: String? { shopDetail.trimmed.nilIfEmpty }
    var resolvedVideoHubImageURL: String? { videoHubImageURL.trimmed.nilIfEmpty }
    var resolvedVideoHubEyebrow: String? { videoHubEyebrow.trimmed.nilIfEmpty }
    var resolvedVideoHubTitle: String? { videoHubTitle.trimmed.nilIfEmpty }
    var resolvedVideoHubSubtitle: String? { videoHubSubtitle.trimmed.nilIfEmpty }
    var resolvedVideoHubDetail: String? { videoHubDetail.trimmed.nilIfEmpty }
}

protocol ScreenHeaderSettingsServicing {
    func observeSettings(_ onChange: @escaping @MainActor (Result<ScreenHeaderSettings, Error>) -> Void) -> () -> Void
    func updateSettings(_ settings: ScreenHeaderSettings) async throws
}

final class FirestoreScreenHeaderSettingsService: ScreenHeaderSettingsServicing {
    private let firestore: Firestore
    private let collectionName = "appConfig"
    private let documentName = "screenHeaders"

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func observeSettings(_ onChange: @escaping @MainActor (Result<ScreenHeaderSettings, Error>) -> Void) -> () -> Void {
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

    func updateSettings(_ settings: ScreenHeaderSettings) async throws {
        try await firestore.collection(collectionName).document(documentName).setData(Self.encode(settings), merge: true)
    }

    private static func decode(_ data: [String: Any]) -> ScreenHeaderSettings {
        ScreenHeaderSettings(
            homeImageURL: data["homeImageURL"] as? String ?? "",
            homeEyebrow: data["homeEyebrow"] as? String ?? "",
            homeTitle: data["homeTitle"] as? String ?? "",
            homeSubtitle: data["homeSubtitle"] as? String ?? "",
            homeDetail: data["homeDetail"] as? String ?? "",
            musicHubImageURL: data["musicHubImageURL"] as? String ?? "",
            musicHubEyebrow: data["musicHubEyebrow"] as? String ?? "",
            musicHubTitle: data["musicHubTitle"] as? String ?? "",
            musicHubSubtitle: data["musicHubSubtitle"] as? String ?? "",
            musicHubDetail: data["musicHubDetail"] as? String ?? "",
            shopImageURL: data["shopImageURL"] as? String ?? "",
            shopEyebrow: data["shopEyebrow"] as? String ?? "",
            shopTitle: data["shopTitle"] as? String ?? "",
            shopSubtitle: data["shopSubtitle"] as? String ?? "",
            shopDetail: data["shopDetail"] as? String ?? "",
            videoHubImageURL: data["videoHubImageURL"] as? String ?? "",
            videoHubEyebrow: data["videoHubEyebrow"] as? String ?? "",
            videoHubTitle: data["videoHubTitle"] as? String ?? "",
            videoHubSubtitle: data["videoHubSubtitle"] as? String ?? "",
            videoHubDetail: data["videoHubDetail"] as? String ?? ""
        )
    }

    private static func encode(_ settings: ScreenHeaderSettings) -> [String: Any] {
        [
            "homeImageURL": settings.homeImageURL.trimmed,
            "homeEyebrow": settings.homeEyebrow.trimmed,
            "homeTitle": settings.homeTitle.trimmed,
            "homeSubtitle": settings.homeSubtitle.trimmed,
            "homeDetail": settings.homeDetail.trimmed,
            "musicHubImageURL": settings.musicHubImageURL.trimmed,
            "musicHubEyebrow": settings.musicHubEyebrow.trimmed,
            "musicHubTitle": settings.musicHubTitle.trimmed,
            "musicHubSubtitle": settings.musicHubSubtitle.trimmed,
            "musicHubDetail": settings.musicHubDetail.trimmed,
            "shopImageURL": settings.shopImageURL.trimmed,
            "shopEyebrow": settings.shopEyebrow.trimmed,
            "shopTitle": settings.shopTitle.trimmed,
            "shopSubtitle": settings.shopSubtitle.trimmed,
            "shopDetail": settings.shopDetail.trimmed,
            "videoHubImageURL": settings.videoHubImageURL.trimmed,
            "videoHubEyebrow": settings.videoHubEyebrow.trimmed,
            "videoHubTitle": settings.videoHubTitle.trimmed,
            "videoHubSubtitle": settings.videoHubSubtitle.trimmed,
            "videoHubDetail": settings.videoHubDetail.trimmed,
            "updatedAt": FieldValue.serverTimestamp()
        ]
    }
}

@MainActor
final class ScreenHeaderSettingsStore: ObservableObject {
    static let shared = ScreenHeaderSettingsStore()

    @Published private(set) var settings: ScreenHeaderSettings = .default
    @Published private(set) var lastErrorMessage: String?

    private let service: ScreenHeaderSettingsServicing
    private var stopObserving: (() -> Void)?
    private let cache = CachedScreenHeaderSettingsStore()

    init(service: ScreenHeaderSettingsServicing = FirestoreScreenHeaderSettingsService()) {
        self.service = service
        self.settings = cache.load()
        startObserving()
    }

    func save(_ settings: ScreenHeaderSettings) async throws {
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
                self.cache.store(settings)
            case .failure(let error):
                self.lastErrorMessage = error.localizedDescription
            }
        }
    }

    deinit {
        stopObserving?()
    }
}

private final class CachedScreenHeaderSettingsStore {
    private let defaults: UserDefaults
    private let cacheKey = "skydown.cached.screen.headers"
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load() -> ScreenHeaderSettings {
        guard let data = defaults.data(forKey: cacheKey),
              let settings = try? decoder.decode(ScreenHeaderSettings.self, from: data) else {
            return .default
        }

        return settings
    }

    func store(_ settings: ScreenHeaderSettings) {
        guard let data = try? encoder.encode(settings) else {
            return
        }

        defaults.set(data, forKey: cacheKey)
    }
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}
