import Foundation
import FirebaseRemoteConfig

enum AIAccessMode: String {
    case off = "off"
    case adminOnly = "admin_only"
    case signedIn = "signed_in"
}

@MainActor
final class FeatureFlagsService: ObservableObject {
    private enum Keys {
        static let aiEnabled = "ai_enabled"
        static let aiAccessMode = "ai_access_mode"
    }

    @Published private(set) var isAIEnabled = true
    @Published private(set) var aiAccessMode: AIAccessMode = .adminOnly

    private let remoteConfig: RemoteConfig

    init(remoteConfig: RemoteConfig = .remoteConfig()) {
        self.remoteConfig = remoteConfig

        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 3600
        remoteConfig.configSettings = settings
        remoteConfig.setDefaults([
            Keys.aiEnabled: true as NSObject,
            Keys.aiAccessMode: AIAccessMode.adminOnly.rawValue as NSObject
        ])
        applyRemoteConfig()

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

        applyRemoteConfig()
    }

    func allowsAIAccess(for user: User?) -> Bool {
        guard isAIEnabled else { return false }

        guard let user else { return false }

        guard user.aiAccessEnabled else {
            return false
        }

        switch aiAccessMode {
        case .off:
            return false
        case .adminOnly:
            return user.hasStaffAccess
        case .signedIn:
            return true
        }
    }

    func aiAccessMessage(for user: User?) -> String {
        guard isAIEnabled else {
            return "Die KI ist gerade pausiert."
        }

        switch aiAccessMode {
        case .off:
            return "Die KI ist gerade pausiert."
        case .adminOnly:
            if user == nil {
                return "Melde dich an, dann sagen wir dir Bescheid, sobald die KI fuer dein Konto verfuegbar ist."
            }

            if user?.aiAccessEnabled == false {
                return "Die KI ist fuer dein Konto gerade pausiert."
            }

            return "Die KI ist gerade nur fuer Staff-Konten freigeschaltet."
        case .signedIn:
            if user?.aiAccessEnabled == false {
                return "Die KI ist fuer dein Konto gerade pausiert."
            }
            return "Melde dich an, um Bot, Agent und Visuals zu nutzen."
        }
    }

    private func applyRemoteConfig() {
        isAIEnabled = remoteConfig.configValue(forKey: Keys.aiEnabled).boolValue
        aiAccessMode = AIAccessMode(
            rawValue: remoteConfig.configValue(forKey: Keys.aiAccessMode).stringValue
        ) ?? .adminOnly
    }
}
