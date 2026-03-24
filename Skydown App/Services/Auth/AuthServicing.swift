import Foundation
import FirebaseAuth
import FirebaseFirestore

protocol AuthServicing {
    func observeAuthState(_ onChange: @escaping @MainActor (User?) -> Void) -> () -> Void
    func signIn(email: String, password: String) async throws
    func register(username: String, email: String, whatsApp: String, password: String) async throws
    func signOut() throws
    func deleteCurrentAccount() async throws
    func fetchCurrentUser() async throws -> User?
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

        return try snapshot.data(as: User.self)
    }
}
