import Foundation
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import GoogleSignIn
import UIKit

protocol AuthServicing {
    func observeAuthState(_ onChange: @escaping @MainActor (User?) -> Void) -> () -> Void
    func signIn(email: String, password: String) async throws
    func signInWithGoogle(preferredUsername: String?) async throws
    func register(username: String, email: String, whatsApp: String, password: String) async throws
    func signOut() throws
    func deleteCurrentAccount() async throws
    func fetchCurrentUser() async throws -> User?
}

extension AuthServicing {
    func signInWithGoogle() async throws {
        try await signInWithGoogle(preferredUsername: nil)
    }
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
                if let firebaseUser {
                    let user = await self.currentSessionUser(for: firebaseUser)
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

    func signInWithGoogle(preferredUsername: String? = nil) async throws {
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
        try await syncUserDocument(for: authResult.user, preferredUsername: preferredUsername)
    }

    func register(username: String, email: String, whatsApp: String, password: String) async throws {
        let normalizedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedUsername = Self.sanitizedUsername(
            username,
            fallbackEmail: normalizedEmail
        )
        let result = try await auth.createUser(withEmail: normalizedEmail, password: password)
        try await refreshAuthToken(for: result.user)
        let registeredEmail = result.user.email?.trimmedNilIfEmpty ?? normalizedEmail
        let newUser = User(
            id: nil,
            email: registeredEmail,
            username: normalizedUsername,
            whatsApp: whatsApp.trimmedNilIfEmpty,
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
        guard let currentAuthUser = auth.currentUser else {
            return nil
        }
        return await currentSessionUser(for: currentAuthUser)
    }

    private func fetchUser(uid: String) async throws -> User? {
        let snapshot = try await firestore.collection("users").document(uid).getDocument()
        let authUser = auth.currentUser
        guard snapshot.exists else {
            if let authUser, authUser.uid == uid {
                try? await syncUserDocument(for: authUser)
                return authUser.toAppUser()
            }

            return nil
        }

        let data = snapshot.data() ?? [:]

        let email = (data["email"] as? String)?.trimmedNilIfEmpty
            ?? authUser?.email?.trimmedNilIfEmpty
            ?? ""
        let username = Self.sanitizedUsername(
            data["username"] as? String,
            authUserDisplayName: authUser?.displayName,
            fallbackEmail: email
        )
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
            whatsApp: whatsApp?.trimmedNilIfEmpty,
            registrationDate: registrationDate,
            isAdmin: isAdmin
        )
    }

    private func syncUserDocument(
        for authUser: FirebaseAuth.User,
        preferredUsername: String? = nil
    ) async throws {
        let documentReference = firestore.collection("users").document(authUser.uid)
        let snapshot = try await documentReference.getDocument()
        let email = authUser.email?.trimmedNilIfEmpty ?? ""
        let username = Self.sanitizedUsername(
            preferredUsername,
            authUserDisplayName: authUser.displayName,
            fallbackEmail: email
        )

        guard snapshot.exists else {
            try await refreshAuthToken(for: authUser)
            let newUser = User(
                id: nil,
                email: email,
                username: username,
                whatsApp: nil,
                registrationDate: authUser.metadata.creationDate ?? .now,
                isAdmin: false
            )

            try documentReference.setData(from: newUser)
            return
        }

        let data = snapshot.data() ?? [:]
        var repairFields: [String: Any] = [:]

        if (data["username"] as? String)?.trimmedNilIfEmpty == nil {
            repairFields["username"] = username
        }

        if data["registrationDate"] == nil && data["registrationDateEpochMillis"] == nil {
            repairFields["registrationDate"] = authUser.metadata.creationDate ?? Date()
        }

        if !repairFields.isEmpty {
            try await refreshAuthToken(for: authUser)
            try await documentReference.setData(repairFields, merge: true)
        }
    }

    private func currentSessionUser(for firebaseUser: FirebaseAuth.User) async -> User {
        do {
            try await syncUserDocument(for: firebaseUser)
            return try await fetchUser(uid: firebaseUser.uid) ?? firebaseUser.toAppUser()
        } catch {
            return firebaseUser.toAppUser()
        }
    }

    private func refreshAuthToken(for authUser: FirebaseAuth.User) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            authUser.getIDTokenForcingRefresh(true) { _, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    fileprivate static func sanitizedUsername(
        _ username: String?,
        authUserDisplayName: String? = nil,
        fallbackEmail: String
    ) -> String {
        username?.trimmedNilIfEmpty
            ?? authUserDisplayName?.trimmedNilIfEmpty
            ?? fallbackEmail.split(separator: "@").first.map(String.init)?.trimmedNilIfEmpty
            ?? "Skydown User"
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

private extension FirebaseAuth.User {
    func toAppUser(isAdmin: Bool = false) -> User {
        let fallbackEmail = email?.trimmedNilIfEmpty ?? ""
        return User(
            id: uid,
            email: fallbackEmail,
            username: FirebaseAuthService.sanitizedUsername(
                displayName,
                authUserDisplayName: displayName,
                fallbackEmail: fallbackEmail
            ),
            whatsApp: nil,
            registrationDate: metadata.creationDate ?? .now,
            isAdmin: isAdmin
        )
    }
}

private extension String {
    var trimmedNilIfEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}
