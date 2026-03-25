import Foundation
import FirebaseRemoteConfig

@MainActor
final class FeatureFlagsService: ObservableObject {
    private enum Keys {
        static let aiEnabled = "ai_enabled"
    }

    @Published private(set) var isAIEnabled = true

    private let remoteConfig: RemoteConfig

    init(remoteConfig: RemoteConfig = .remoteConfig()) {
        self.remoteConfig = remoteConfig

        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 3600
        remoteConfig.configSettings = settings
        remoteConfig.setDefaults([
            Keys.aiEnabled: true as NSObject,
        ])
        isAIEnabled = remoteConfig.configValue(forKey: Keys.aiEnabled).boolValue

        Task {
            await refresh()
        }
    }

    func refresh() async {
        do {
            _ = try await remoteConfig.fetchAndActivate()
        } catch {
            print("Remote Config konnte nicht aktualisiert werden: \(error.localizedDescription)")
        }

        isAIEnabled = remoteConfig.configValue(forKey: Keys.aiEnabled).boolValue
    }
}
