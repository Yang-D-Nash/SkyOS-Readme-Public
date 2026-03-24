//
//  RegistrationViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 18.08.25.
//

import Foundation
import FirebaseAuth
import FirebaseFirestore

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
            let result = try await Auth.auth().createUser(withEmail: email, password: password)
            let userId = result.user.uid
            print("Dev Registrierung erfolgreich, UID: \(userId)")

            let newUser = User(
                id: nil,
                email: email,
                username: username,
                whatsApp: whatsapp,
                registrationDate: Date(),
                isAdmin: false
            )

            try Firestore.firestore().collection("users").document(userId).setData(from: newUser)
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
