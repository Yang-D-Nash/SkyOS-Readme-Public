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
        email.isEmpty || password.isEmpty || confirmPassword.isEmpty || password != confirmPassword || isLoading
    }

    func registerUser() async -> Bool {
        guard password == confirmPassword else {
            showUserToast("Passwörter stimmen nicht überein.", style: .error)
            return false
        }

        isLoading = true
        errorMessage = nil

        do {
            try await authService.register(
                username: username,
                email: email,
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

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}
