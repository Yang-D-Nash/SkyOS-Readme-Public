import Foundation
import FirebaseFirestore

struct ScreenHeaderSettings: Equatable {
    var homeImageURL: String = ""
    var musicHubImageURL: String = ""
    var shopImageURL: String = ""
    var videoHubImageURL: String = ""

    static let `default` = ScreenHeaderSettings()

    var configuredCount: Int {
        [homeImageURL, musicHubImageURL, shopImageURL, videoHubImageURL]
            .map(\.trimmed)
            .filter { !$0.isEmpty }
            .count
    }

    var resolvedHomeImageURL: String? { homeImageURL.trimmed.nilIfEmpty }
    var resolvedMusicHubImageURL: String? { musicHubImageURL.trimmed.nilIfEmpty }
    var resolvedShopImageURL: String? { shopImageURL.trimmed.nilIfEmpty }
    var resolvedVideoHubImageURL: String? { videoHubImageURL.trimmed.nilIfEmpty }
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
            musicHubImageURL: data["musicHubImageURL"] as? String ?? "",
            shopImageURL: data["shopImageURL"] as? String ?? "",
            videoHubImageURL: data["videoHubImageURL"] as? String ?? ""
        )
    }

    private static func encode(_ settings: ScreenHeaderSettings) -> [String: Any] {
        [
            "homeImageURL": settings.homeImageURL.trimmed,
            "musicHubImageURL": settings.musicHubImageURL.trimmed,
            "shopImageURL": settings.shopImageURL.trimmed,
            "videoHubImageURL": settings.videoHubImageURL.trimmed,
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

    init(service: ScreenHeaderSettingsServicing = FirestoreScreenHeaderSettingsService()) {
        self.service = service
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

    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}
