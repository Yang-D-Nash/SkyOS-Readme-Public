//
//  LogInViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 29.07.25.
//

import Foundation

@MainActor
class LoginViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var errorMessage: String?
    @Published var isAuthenticated = false
    @Published var isLoading = false

    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let authService: AuthServicing

    init(authService: AuthServicing = FirebaseAuthService()) {
        self.authService = authService
    }

    var isSignInButtonDisabled: Bool {
        email.isEmpty || password.isEmpty || isLoading
    }

    func signIn() async {
        guard !email.isEmpty, !password.isEmpty else {
            showUserToast("Bitte E-Mail und Passwort ausfüllen.", style: .error)
            return
        }

        isLoading = true
        errorMessage = nil

        do {
            try await authService.signIn(email: email, password: password)
            isAuthenticated = true
            showUserToast("Anmeldung erfolgreich!", style: .success)
        } catch {
            print("Dev Fehler beim Anmelden: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
            isAuthenticated = false
            showUserToast("Fehler beim Anmelden: \(error.localizedDescription)", style: .error)
        }

        isLoading = false
    }

    func signInWithGoogle() async {
        isLoading = true
        errorMessage = nil

        do {
            try await authService.signInWithGoogle()
            isAuthenticated = true
            showUserToast("Google-Anmeldung erfolgreich!", style: .success)
        } catch {
            print("Dev Fehler bei Google-Anmeldung: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
            isAuthenticated = false
            showUserToast("Fehler bei Google-Anmeldung: \(error.localizedDescription)", style: .error)
        }

        isLoading = false
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}
