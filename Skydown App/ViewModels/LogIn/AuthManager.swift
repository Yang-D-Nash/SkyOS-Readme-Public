//
//  AuthManager.swift
//  Skydown App
//
//  Created by Yang D. Nash on 29.07.25.
//

import Foundation
import FirebaseAuth
import FirebaseFirestore

@MainActor
class AuthManager: ObservableObject {
    @Published var userSession: User?

    private var handle: AuthStateDidChangeListenerHandle?

    init() {
        observeAuthState()
    }

    deinit {
        if let handle = handle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    private func observeAuthState() {
        handle = Auth.auth().addStateDidChangeListener { [weak self] _, firebaseUser in
            Task { @MainActor in
                if let firebaseUser = firebaseUser {
                    await self?.fetchUserData(uid: firebaseUser.uid)
                } else {
                    self?.userSession = nil
                }
            }
        }
    }

    func fetchUserData(uid: String) async {
        let docRef = Firestore.firestore().collection("users").document(uid)
        do {
            let snapshot = try await docRef.getDocument()
            guard snapshot.exists else {
                self.userSession = nil
                return
            }
            let user = try snapshot.data(as: User.self)
            self.userSession = user
        } catch {
            print("Fehler beim Laden/Decodieren des Users: \(error.localizedDescription)")
        }
    }

    func signOut() async {
        do {
            try Auth.auth().signOut()
            userSession = nil
        } catch {
            print("Fehler beim Abmelden: \(error.localizedDescription)")
        }
    }
}
