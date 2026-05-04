import Foundation

enum UITestRuntime {
    static var usesIsolatedAuthService: Bool {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        guard arguments.contains(where: { $0.hasPrefix("-ui_test") }) else {
            return false
        }

        let environment = ProcessInfo.processInfo.environment
        let liveBackendKeys = [
            "SKYOS_RUN_LIVE_PROFILE_UI_TESTS",
            "SKYOS_RUN_LIVE_MEMBERSHIP_UI_TEST",
            "SKYOS_RUN_LIVE_AGENT_UI_TEST",
        ]
        return !liveBackendKeys.contains { environment[$0] == "1" }
        #else
        return false
        #endif
    }
}

@MainActor
final class UITestAuthService: @preconcurrency AuthServicing {
    static let shared = UITestAuthService()

    private let defaults: UserDefaults
    private let persistedEmailKey = "skydown.ui_test.auth.email"
    private var currentUser: User?
    private var observers: [UUID: @MainActor (User?) -> Void] = [:]

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        let persistedEmail = defaults.string(forKey: persistedEmailKey)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if let persistedEmail, !persistedEmail.isEmpty {
            currentUser = Self.fixtureUser(email: persistedEmail)
        } else {
            currentUser = Self.fixtureUser()
        }
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
        let normalizedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        defaults.set(normalizedEmail, forKey: persistedEmailKey)
        currentUser = Self.fixtureUser(email: normalizedEmail)
        notifyObservers()
    }

    func signInWithGoogle(
        preferredUsername: String?,
        registrationConsent: RegistrationLegalConsent?
    ) async throws {
        defaults.removeObject(forKey: persistedEmailKey)
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
        let normalizedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        defaults.set(normalizedEmail, forKey: persistedEmailKey)
        currentUser = Self.fixtureUser(
            email: normalizedEmail,
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
        defaults.removeObject(forKey: persistedEmailKey)
        currentUser = nil
        notifyObservers()
    }

    func deleteCurrentAccount() async throws {
        defaults.removeObject(forKey: persistedEmailKey)
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
        let normalizedEmail = (email?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()).flatMap {
            $0.isEmpty ? nil : $0
        }
        let resolvedRole = UserRole.resolve(from: nil, isAdmin: false, email: normalizedEmail)
        let resolvedPlan = UserQuotaPlan.defaultPlan(for: resolvedRole)
        let platformOwnerSession = ProcessInfo.processInfo.arguments.contains("-ui_test_platform_owner")
            && email == nil
            && username == nil
            && whatsApp == nil
        if platformOwnerSession {
            let resolvedUsername = "SkyOS Owner (UI test)"
            return User(
                id: "ui-test-owner",
                email: UserRole.ownerEmail,
                username: resolvedUsername,
                profileImageURL: nil,
                whatsApp: whatsApp,
                profileTagline: "Owner UI test session.",
                profileBio: "UITest session for owner hub.",
                instagramHandle: "@skyos",
                registrationDate: Date(timeIntervalSince1970: 1_756_000_000),
                isAdmin: true,
                role: UserRole.owner.rawValue,
                quotaPlan: UserQuotaPlan.ownerUnlimited.rawValue,
                aiAccessEnabled: true,
                aiTextRequestsPerDay: UserRole.owner.defaultAITextRequestsPerDay,
                aiVisualRequestsPerDay: UserRole.owner.defaultAIVisualRequestsPerDay,
                aiAgentRequestsPerDay: UserRole.owner.defaultAIAgentRequestsPerDay,
                aiHistoryRetentionDays: UserRole.owner.defaultAIHistoryRetentionDays,
                aiSubscriptionStatus: "active",
                aiSubscriptionPlan: UserQuotaPlan.ownerUnlimited.rawValue,
                aiSubscriptionCurrentPeriodEndEpochSeconds: 1_786_204_800,
                aiSubscriptionCheckoutExpiresAtEpochSeconds: nil,
                aiSubscriptionCancelAtPeriodEnd: false,
                aiSubscriptionProvider: "app_store",
                aiSubscriptionSourcePlatform: "ios",
                aiSubscriptionProductID: "skyos.owner.unlimited",
                canManageMusicCatalog: false,
                canManageVideoCatalog: false,
                canModerateProfiles: false
            )
        }

        return User(
            id: "ui-test-user",
            email: normalizedEmail ?? "creator@skydown.app",
            username: (username?.trimmingCharacters(in: .whitespacesAndNewlines)).flatMap { $0.isEmpty ? nil : $0 } ?? "SkyOS Creator",
            profileImageURL: nil,
            whatsApp: whatsApp,
            profileTagline: "AI, media and merch in one place.",
            profileBio: "UITest session for premium screenshot capture.",
            instagramHandle: "@skyos",
            registrationDate: Date(timeIntervalSince1970: 1_756_000_000),
            isAdmin: resolvedRole.hasStaffAccess,
            role: resolvedRole.rawValue,
            quotaPlan: resolvedPlan.rawValue,
            aiAccessEnabled: true,
            aiTextRequestsPerDay: resolvedPlan.aiTextRequestsPerDay,
            aiVisualRequestsPerDay: resolvedPlan.aiVisualRequestsPerDay,
            aiAgentRequestsPerDay: resolvedPlan.aiAgentRequestsPerDay,
            aiHistoryRetentionDays: resolvedPlan.aiHistoryRetentionDays,
            aiSubscriptionStatus: "active",
            aiSubscriptionPlan: resolvedPlan.rawValue,
            aiSubscriptionCurrentPeriodEndEpochSeconds: 1_786_204_800,
            aiSubscriptionCheckoutExpiresAtEpochSeconds: nil,
            aiSubscriptionCancelAtPeriodEnd: false,
            aiSubscriptionProvider: "app_store",
            aiSubscriptionSourcePlatform: "ios",
            aiSubscriptionProductID: "skyos.creator.monthly",
            canManageMusicCatalog: resolvedRole == .owner,
            canManageVideoCatalog: resolvedRole == .owner,
            canModerateProfiles: resolvedRole == .owner
        )
    }
}
