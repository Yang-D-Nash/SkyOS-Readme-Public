//
//  RegistrationViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 18.08.25.
//

import Foundation

@MainActor
class RegistrationViewModel: ObservableObject {
    @Published var username = ""
    @Published var email = ""
    @Published var whatsapp = ""
    @Published var password = ""
    @Published var confirmPassword = ""
    @Published var errorMessage: String?
    @Published var isLoading = false

    // Toast
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let authService: AuthServicing

    init(authService: AuthServicing = FirebaseAuthService()) {
        self.authService = authService
    }

    var isRegistrationButtonDisabled: Bool {
        username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            || email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            || password.isEmpty
            || confirmPassword.isEmpty
            || password != confirmPassword
            || isLoading
    }

    func registerUser() async -> Bool {
        guard !username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            showUserToast("Bitte einen Benutzernamen angeben.", style: .error)
            return false
        }

        guard password == confirmPassword else {
            showUserToast("Passwörter stimmen nicht überein.", style: .error)
            return false
        }

        isLoading = true
        errorMessage = nil

        do {
            try await authService.register(
                username: username.trimmingCharacters(in: .whitespacesAndNewlines),
                email: email.trimmingCharacters(in: .whitespacesAndNewlines),
                whatsApp: whatsapp,
                password: password
            )
            showUserToast("Registrierung erfolgreich!", style: .success)
            isLoading = false
            return true
        } catch {
            print("Dev Fehler bei Registrierung oder Firestore: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
            showUserToast("Fehler bei der Registrierung: \(error.localizedDescription)", style: .error)
            isLoading = false
            return false
        }
    }

    func signInWithGoogle() async -> Bool {
        isLoading = true
        errorMessage = nil

        do {
            try await authService.signInWithGoogle()
            showUserToast("Google-Anmeldung erfolgreich!", style: .success)
            isLoading = false
            return true
        } catch {
            print("Dev Fehler bei Google-Anmeldung: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
            showUserToast("Fehler bei Google-Anmeldung: \(error.localizedDescription)", style: .error)
            isLoading = false
            return false
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}
