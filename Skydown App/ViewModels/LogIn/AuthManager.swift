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

    init(authService: AuthServicing = FirebaseAuthService()) {
        self.authService = authService
        observeAuthState()
    }

    deinit {
        authStateCancellation?()
    }

    private func observeAuthState() {
        authStateCancellation = authService.observeAuthState { [weak self] user in
            self?.userSession = user
        }
    }

    func signOut() async {
        do {
            try authService.signOut()
            userSession = nil
        } catch {
            print("Fehler beim Abmelden: \(error.localizedDescription)")
        }
    }

    func deleteAccount() async throws {
        try await authService.deleteCurrentAccount()
        userSession = nil
    }

    func refreshCurrentUser() async {
        do {
            userSession = try await authService.fetchCurrentUser()
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
    }
}
