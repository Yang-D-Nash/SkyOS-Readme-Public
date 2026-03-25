import Foundation
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import GoogleSignIn
import UIKit

protocol AuthServicing {
    func observeAuthState(_ onChange: @escaping @MainActor (User?) -> Void) -> () -> Void
    func signIn(email: String, password: String) async throws
    func signInWithGoogle() async throws
    func register(username: String, email: String, whatsApp: String, password: String) async throws
    func signOut() throws
    func deleteCurrentAccount() async throws
    func fetchCurrentUser() async throws -> User?
}

enum AuthServiceError: LocalizedError {
    case missingGoogleClientID
    case missingPresentingViewController
    case missingGoogleIDToken

    var errorDescription: String? {
        switch self {
        case .missingGoogleClientID:
            return "Google Sign-In ist noch nicht vollständig konfiguriert. Lade eine aktuelle GoogleService-Info.plist mit CLIENT_ID herunter."
        case .missingPresentingViewController:
            return "Google Sign-In konnte nicht gestartet werden."
        case .missingGoogleIDToken:
            return "Google Sign-In hat kein gültiges Token geliefert."
        }
    }
}

final class FirebaseAuthService: AuthServicing {
    private let auth: Auth
    private let firestore: Firestore

    init(
        auth: Auth = Auth.auth(),
        firestore: Firestore = Firestore.firestore()
    ) {
        self.auth = auth
        self.firestore = firestore
    }

    func observeAuthState(_ onChange: @escaping @MainActor (User?) -> Void) -> () -> Void {
        let handle = auth.addStateDidChangeListener { [weak self] _, firebaseUser in
            guard let self else { return }

            Task {
                if let uid = firebaseUser?.uid {
                    let user = try? await self.fetchUser(uid: uid)
                    await onChange(user)
                } else {
                    await onChange(nil)
                }
            }
        }

        return { [weak auth] in
            auth?.removeStateDidChangeListener(handle)
        }
    }

    func signIn(email: String, password: String) async throws {
        _ = try await auth.signIn(withEmail: email, password: password)
    }

    func signInWithGoogle() async throws {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            throw AuthServiceError.missingGoogleClientID
        }

        guard let presentingViewController = topViewController() else {
            throw AuthServiceError.missingPresentingViewController
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController)

        guard let idToken = result.user.idToken?.tokenString else {
            throw AuthServiceError.missingGoogleIDToken
        }

        let credential = GoogleAuthProvider.credential(
            withIDToken: idToken,
            accessToken: result.user.accessToken.tokenString
        )
        let authResult = try await auth.signIn(with: credential)
        try await createUserDocumentIfNeeded(for: authResult.user)
    }

    func register(username: String, email: String, whatsApp: String, password: String) async throws {
        let result = try await auth.createUser(withEmail: email, password: password)
        let newUser = User(
            id: nil,
            email: email,
            username: username,
            whatsApp: whatsApp,
            registrationDate: Date(),
            isAdmin: false
        )

        try firestore.collection("users").document(result.user.uid).setData(from: newUser)
    }

    func signOut() throws {
        GIDSignIn.sharedInstance.signOut()
        try auth.signOut()
    }

    func deleteCurrentAccount() async throws {
        try await auth.currentUser?.delete()
    }

    func fetchCurrentUser() async throws -> User? {
        guard let uid = auth.currentUser?.uid else {
            return nil
        }

        return try await fetchUser(uid: uid)
    }

    private func fetchUser(uid: String) async throws -> User? {
        let snapshot = try await firestore.collection("users").document(uid).getDocument()
        guard snapshot.exists else {
            return nil
        }
        let data = snapshot.data() ?? [:]
        let authUser = auth.currentUser

        let email = data["email"] as? String ?? authUser?.email ?? ""
        let username = data["username"] as? String ?? authUser?.displayName ?? email.components(separatedBy: "@").first ?? "Skydown User"
        let whatsApp = data["whatsApp"] as? String
        let isAdmin = data["isAdmin"] as? Bool ?? false

        let registrationDate: Date
        if let timestamp = data["registrationDate"] as? Timestamp {
            registrationDate = timestamp.dateValue()
        } else if let date = data["registrationDate"] as? Date {
            registrationDate = date
        } else if let createdAt = authUser?.metadata.creationDate {
            registrationDate = createdAt
        } else {
            registrationDate = .now
        }

        return User(
            id: snapshot.documentID,
            email: email,
            username: username,
            whatsApp: whatsApp,
            registrationDate: registrationDate,
            isAdmin: isAdmin
        )
    }

    private func createUserDocumentIfNeeded(for authUser: FirebaseAuth.User) async throws {
        let documentReference = firestore.collection("users").document(authUser.uid)
        let snapshot = try await documentReference.getDocument()

        guard !snapshot.exists else { return }

        let email = authUser.email ?? ""
        let username = authUser.displayName ?? email.components(separatedBy: "@").first ?? "Skydown User"
        let newUser = User(
            id: nil,
            email: email,
            username: username,
            whatsApp: nil,
            registrationDate: authUser.metadata.creationDate ?? .now,
            isAdmin: false
        )

        try documentReference.setData(from: newUser)
    }

    private func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
        let keyWindow = scenes
            .flatMap(\.windows)
            .first { $0.isKeyWindow }

        var topController = keyWindow?.rootViewController
        while let presentedViewController = topController?.presentedViewController {
            topController = presentedViewController
        }

        return topController
    }
}
