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
    @Published var acceptedTerms = false
    @Published var acceptedPrivacyPolicy = false
    @Published var aiConsentEnabled = false
    @Published var legalVersionLabel = LegalContentStore.shared.settings.resolvedLastUpdatedLabel
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
            || !acceptedTerms
            || !acceptedPrivacyPolicy
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

        guard acceptedTerms, acceptedPrivacyPolicy else {
            showUserToast("Bitte AGB und Datenschutz akzeptieren.", style: .error)
            return false
        }

        isLoading = true
        errorMessage = nil

        do {
            let consent = RegistrationLegalConsent(
                acceptedTerms: acceptedTerms,
                acceptedPrivacyPolicy: acceptedPrivacyPolicy,
                aiConsentEnabled: aiConsentEnabled,
                legalVersionLabel: legalVersionLabel,
                consentSource: "ios_registration"
            )
            try await authService.register(
                username: username.trimmingCharacters(in: .whitespacesAndNewlines),
                email: email.trimmingCharacters(in: .whitespacesAndNewlines),
                whatsApp: whatsapp,
                password: password,
                registrationConsent: consent
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
        guard acceptedTerms, acceptedPrivacyPolicy else {
            showUserToast("Bitte AGB und Datenschutz akzeptieren.", style: .error)
            return false
        }
        isLoading = true
        errorMessage = nil
        let preferredUsername = username.trimmingCharacters(in: .whitespacesAndNewlines).trimmedNilIfEmpty

        do {
            try await authService.signInWithGoogle(
                preferredUsername: preferredUsername,
                registrationConsent: RegistrationLegalConsent(
                    acceptedTerms: acceptedTerms,
                    acceptedPrivacyPolicy: acceptedPrivacyPolicy,
                    aiConsentEnabled: aiConsentEnabled,
                    legalVersionLabel: legalVersionLabel,
                    consentSource: "ios_registration_google"
                )
            )
            showUserToast("Google-Registrierung erfolgreich!", style: .success)
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

private extension String {
    var trimmedNilIfEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}
