import Foundation
import Network
import UIKit
import UserNotifications

@MainActor
final class AppServices: ObservableObject {
    let authService: AuthServicing
    let aiChatService: AIChatServicing
    let agentChatService: AgentChatServicing
    let featureFlags: FeatureFlagsService
    let merchandiseService: MerchandiseServicing
    let orderService: OrderServicing
    let musicService: MusicServicing
    let hostedCheckoutRedirectStore: HostedCheckoutRedirectStore
    let aiSubscriptionStore: NativeAISubscriptionStore
    let networkStatusMonitor: NetworkStatusMonitor
    let notificationPermissionStore: NotificationPermissionStore

    let authManager: AuthManager
    let cartViewModel: CartViewModel

    init(
        authService: AuthServicing = FirebaseAuthService(),
        aiChatService: AIChatServicing = FirebaseFunctionsAIChatService(),
        agentChatService: AgentChatServicing = FirebaseFunctionsAgentService(),
        merchandiseService: MerchandiseServicing? = nil,
        orderService: OrderServicing = FirebaseOrderService(),
        musicService: MusicServicing = SpotifyMusicService()
    ) {
        let launchArguments = ProcessInfo.processInfo.arguments
        let resolvedMerchandiseService = merchandiseService ?? {
            if launchArguments.contains("-ui_test_merch_flow") {
                return UITestMerchandiseService()
            }
            return FirebaseMerchandiseService()
        }()

        self.authService = authService
        self.aiChatService = aiChatService
        self.agentChatService = agentChatService
        self.featureFlags = FeatureFlagsService()
        self.merchandiseService = resolvedMerchandiseService
        self.orderService = orderService
        self.musicService = musicService
        self.hostedCheckoutRedirectStore = HostedCheckoutRedirectStore()
        self.networkStatusMonitor = NetworkStatusMonitor.shared
        self.notificationPermissionStore = NotificationPermissionStore.shared

        let authManager = AuthManager(authService: authService)
        self.authManager = authManager
        self.aiSubscriptionStore = NativeAISubscriptionStore(
            onSubscriptionSynced: { [weak authManager] in
                guard let authManager else { return }
                _ = await authManager.refreshCurrentUser()
            }
        )
        self.cartViewModel = CartViewModel(
            authManager: authManager,
            orderService: orderService
        )
    }
}

@MainActor
final class NetworkStatusMonitor: ObservableObject {
    static let shared = NetworkStatusMonitor()

    @Published private(set) var isOnline = true

    private let monitor: NWPathMonitor
    private let monitorQueue = DispatchQueue(label: "com.skydown.network-monitor")

    init(monitor: NWPathMonitor = NWPathMonitor()) {
        self.monitor = monitor
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in
                self?.isOnline = path.status == .satisfied
            }
        }
        monitor.start(queue: monitorQueue)
    }

    deinit {
        monitor.cancel()
    }
}

@MainActor
final class NotificationPermissionStore: ObservableObject {
    static let shared = NotificationPermissionStore()

    @Published private(set) var authorizationStatus: UNAuthorizationStatus = .notDetermined

    private let center: UNUserNotificationCenter
    private let defaults: UserDefaults
    private let promptedAtLaunchKey = "skydown.notifications.prompted.once"

    init(
        center: UNUserNotificationCenter = .current(),
        defaults: UserDefaults = .standard
    ) {
        self.center = center
        self.defaults = defaults
        Task {
            await refresh()
        }
    }

    var notificationsEnabled: Bool {
        switch authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        case .notDetermined, .denied:
            return false
        @unknown default:
            return false
        }
    }

    func refresh() async {
        let settings = await center.notificationSettings()
        authorizationStatus = settings.authorizationStatus
    }

    func requestAuthorizationIfNeededOnLaunch() async {
        await refresh()
        guard authorizationStatus == .notDetermined else { return }
        guard defaults.bool(forKey: promptedAtLaunchKey) == false else { return }
        _ = await requestAuthorization()
    }

    @discardableResult
    func requestAuthorization() async -> Bool {
        do {
            let granted = try await center.requestAuthorization(options: [.alert, .badge, .sound])
            defaults.set(true, forKey: promptedAtLaunchKey)
            await refresh()
            return granted
        } catch {
            defaults.set(true, forKey: promptedAtLaunchKey)
            await refresh()
            return false
        }
    }

    func openSystemSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }
}

enum AppLanguageSupport {
    static let supportedLanguageCodes = ["de", "en", "es", "fr", "it", "pt", "nl", "pl", "tr", "ja"]

    static var supportedLanguagesSummary: String {
        AppLocalized.text(
            "settings.supported_languages_summary",
            fallback: "10 languages: DE, EN, ES, FR, IT, PT, NL, PL, TR, JA"
        )
    }

    static func currentSystemLanguageDisplayName(locale: Locale = .current) -> String {
        let code = normalizedLanguageCode()
        let resolvedLocale = Locale(identifier: code)
        let displayName = locale.localizedString(forLanguageCode: code)
            ?? resolvedLocale.localizedString(forLanguageCode: code)
            ?? code.uppercased()
        return displayName.prefix(1).uppercased() + displayName.dropFirst()
    }

    static func normalizedLanguageCode() -> String {
        guard let rawIdentifier = Locale.preferredLanguages.first, !rawIdentifier.isEmpty else {
            return "de"
        }
        let code = Locale(identifier: rawIdentifier).language.languageCode?.identifier
            ?? rawIdentifier.components(separatedBy: "-").first
            ?? "de"
        let normalizedCode = code.lowercased()
        if supportedLanguageCodes.contains(normalizedCode) {
            return normalizedCode
        }
        return "en"
    }
}
