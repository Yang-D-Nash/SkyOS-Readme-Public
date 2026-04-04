//
//  AuthManager.swift
//  Skydown App
//
//  Created by Yang D. Nash on 29.07.25.
//

import Foundation

@MainActor
class AuthManager: ObservableObject {
    @Published var userSession: User?
    private let authService: AuthServicing
    private var authStateCancellation: (() -> Void)?
    private let sessionCache = CachedAuthSessionStore()

    init(authService: AuthServicing = FirebaseAuthService()) {
        self.authService = authService
        self.userSession = sessionCache.load()
        observeAuthState()
    }

    deinit {
        authStateCancellation?()
    }

    private func observeAuthState() {
        authStateCancellation = authService.observeAuthState { [weak self] user in
            self?.userSession = user
            self?.sessionCache.store(user)
        }
    }

    func signOut() async {
        do {
            try authService.signOut()
            userSession = nil
            sessionCache.clear()
        } catch {
            print("Fehler beim Abmelden: \(error.localizedDescription)")
        }
    }

    func deleteAccount() async throws {
        try await authService.deleteCurrentAccount()
        userSession = nil
        sessionCache.clear()
    }

    func refreshCurrentUser() async {
        do {
            userSession = try await authService.fetchCurrentUser()
            sessionCache.store(userSession)
        } catch {
            print("Fehler beim Laden des Profils: \(error.localizedDescription)")
        }
    }

    func updateProfile(
        username: String,
        whatsApp: String?,
        profileTagline: String?,
        profileBio: String?,
        instagramHandle: String?
    ) async throws {
        userSession = try await authService.updateCurrentProfile(
            username: username,
            whatsApp: whatsApp,
            profileTagline: profileTagline,
            profileBio: profileBio,
            instagramHandle: instagramHandle
        )
        sessionCache.store(userSession)
    }
}

private struct CachedAuthSessionPayload: Codable {
    var id: String?
    var email: String
    var username: String
    var profileImageURL: String?
    var whatsApp: String?
    var profileTagline: String?
    var profileBio: String?
    var instagramHandle: String?
    var registrationDate: Date
    var isAdmin: Bool
    var role: String
    var quotaPlan: String
    var aiAccessEnabled: Bool
    var aiTextRequestsPerDay: Int
    var aiVisualRequestsPerDay: Int
    var aiAgentRequestsPerDay: Int
    var aiHistoryRetentionDays: Int
    var canManageMusicCatalog: Bool
    var canManageVideoCatalog: Bool
    var canModerateProfiles: Bool

    init(user: User) {
        self.id = user.id
        self.email = user.email
        self.username = user.username
        self.profileImageURL = user.profileImageURL
        self.whatsApp = user.whatsApp
        self.profileTagline = user.profileTagline
        self.profileBio = user.profileBio
        self.instagramHandle = user.instagramHandle
        self.registrationDate = user.registrationDate
        self.isAdmin = user.isAdmin
        self.role = user.role
        self.quotaPlan = user.quotaPlan
        self.aiAccessEnabled = user.aiAccessEnabled
        self.aiTextRequestsPerDay = user.aiTextRequestsPerDay
        self.aiVisualRequestsPerDay = user.aiVisualRequestsPerDay
        self.aiAgentRequestsPerDay = user.aiAgentRequestsPerDay
        self.aiHistoryRetentionDays = user.aiHistoryRetentionDays
        self.canManageMusicCatalog = user.canManageMusicCatalog
        self.canManageVideoCatalog = user.canManageVideoCatalog
        self.canModerateProfiles = user.canModerateProfiles
    }

    func makeUser() -> User {
        User(
            id: id,
            email: email,
            username: username,
            profileImageURL: profileImageURL,
            whatsApp: whatsApp,
            profileTagline: profileTagline,
            profileBio: profileBio,
            instagramHandle: instagramHandle,
            registrationDate: registrationDate,
            isAdmin: isAdmin,
            role: role,
            quotaPlan: quotaPlan,
            aiAccessEnabled: aiAccessEnabled,
            aiTextRequestsPerDay: aiTextRequestsPerDay,
            aiVisualRequestsPerDay: aiVisualRequestsPerDay,
            aiAgentRequestsPerDay: aiAgentRequestsPerDay,
            aiHistoryRetentionDays: aiHistoryRetentionDays,
            canManageMusicCatalog: canManageMusicCatalog,
            canManageVideoCatalog: canManageVideoCatalog,
            canModerateProfiles: canModerateProfiles
        )
    }
}

private final class CachedAuthSessionStore {
    private let defaults: UserDefaults
    private let cacheKey = "skydown.cached.auth.session"
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load() -> User? {
        guard let data = defaults.data(forKey: cacheKey),
              let payload = try? decoder.decode(CachedAuthSessionPayload.self, from: data) else {
            return nil
        }

        return payload.makeUser()
    }

    func store(_ user: User?) {
        guard let user else {
            clear()
            return
        }

        guard let data = try? encoder.encode(CachedAuthSessionPayload(user: user)) else {
            return
        }

        defaults.set(data, forKey: cacheKey)
    }

    func clear() {
        defaults.removeObject(forKey: cacheKey)
    }
}
