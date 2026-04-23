import Foundation

@MainActor
final class UITestAuthService: @preconcurrency AuthServicing {
    private var currentUser: User?
    private var observers: [UUID: @MainActor (User?) -> Void] = [:]

    init() {
        currentUser = Self.fixtureUser()
    }

    func observeAuthState(_ onChange: @escaping @MainActor (User?) -> Void) -> () -> Void {
        let id = UUID()
        observers[id] = onChange
        onChange(currentUser)
        return { [weak self] in
            Task { @MainActor in
                self?.observers.removeValue(forKey: id)
            }
        }
    }

    func signIn(email: String, password: String) async throws {
        currentUser = Self.fixtureUser(email: email)
        notifyObservers()
    }

    func signInWithGoogle(
        preferredUsername: String?,
        registrationConsent: RegistrationLegalConsent?
    ) async throws {
        currentUser = Self.fixtureUser(username: preferredUsername)
        notifyObservers()
    }

    func register(
        username: String,
        email: String,
        whatsApp: String,
        password: String,
        registrationConsent: RegistrationLegalConsent
    ) async throws {
        currentUser = Self.fixtureUser(
            email: email,
            username: username,
            whatsApp: whatsApp
        )
        notifyObservers()
    }

    func updateCurrentProfile(
        username: String,
        whatsApp: String?,
        profileTagline: String?,
        profileBio: String?,
        instagramHandle: String?
    ) async throws -> User {
        var updated = currentUser ?? Self.fixtureUser()
        updated.username = username
        updated.whatsApp = whatsApp
        updated.profileTagline = profileTagline
        updated.profileBio = profileBio
        updated.instagramHandle = instagramHandle
        currentUser = updated
        notifyObservers()
        return updated
    }

    func updateCurrentAIAccessEnabled(_ enabled: Bool) async throws -> User {
        var updated = currentUser ?? Self.fixtureUser()
        updated.aiAccessEnabled = enabled
        currentUser = updated
        notifyObservers()
        return updated
    }

    func signOut() throws {
        currentUser = nil
        notifyObservers()
    }

    func deleteCurrentAccount() async throws {
        currentUser = nil
        notifyObservers()
    }

    func fetchCurrentUser() async throws -> User? {
        currentUser
    }

    private func notifyObservers() {
        for observer in observers.values {
            observer(currentUser)
        }
    }

    private static func fixtureUser(
        email: String? = nil,
        username: String? = nil,
        whatsApp: String? = nil
    ) -> User {
        User(
            id: "ui-test-user",
            email: (email?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()).flatMap { $0.isEmpty ? nil : $0 } ?? "creator@skydown.app",
            username: (username?.trimmingCharacters(in: .whitespacesAndNewlines)).flatMap { $0.isEmpty ? nil : $0 } ?? "SkyOS Creator",
            profileImageURL: nil,
            whatsApp: whatsApp,
            profileTagline: "AI, media and merch in one place.",
            profileBio: "UITest session for premium screenshot capture.",
            instagramHandle: "@skyos",
            registrationDate: Date(timeIntervalSince1970: 1_756_000_000),
            isAdmin: false,
            role: UserRole.user.rawValue,
            quotaPlan: UserQuotaPlan.creator.rawValue,
            aiAccessEnabled: true,
            aiTextRequestsPerDay: UserQuotaPlan.creator.aiTextRequestsPerDay,
            aiVisualRequestsPerDay: UserQuotaPlan.creator.aiVisualRequestsPerDay,
            aiAgentRequestsPerDay: UserQuotaPlan.creator.aiAgentRequestsPerDay,
            aiHistoryRetentionDays: UserQuotaPlan.creator.aiHistoryRetentionDays,
            aiSubscriptionStatus: "active",
            aiSubscriptionPlan: UserQuotaPlan.creator.rawValue,
            aiSubscriptionCurrentPeriodEndEpochSeconds: 1_786_204_800,
            aiSubscriptionCheckoutExpiresAtEpochSeconds: nil,
            aiSubscriptionCancelAtPeriodEnd: false,
            aiSubscriptionProvider: "app_store",
            aiSubscriptionSourcePlatform: "ios",
            aiSubscriptionProductID: "skyos.creator.monthly",
            canManageMusicCatalog: false,
            canManageVideoCatalog: false,
            canModerateProfiles: false
        )
    }
}
