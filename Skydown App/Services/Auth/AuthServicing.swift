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
    func updateCurrentProfile(
        username: String,
        whatsApp: String?,
        profileTagline: String?,
        profileBio: String?,
        instagramHandle: String?
    ) async throws -> User
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
        let normalizedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let normalizedUsername = Self.sanitizedUsername(
            username,
            fallbackEmail: normalizedEmail
        )
        let result = try await auth.createUser(withEmail: normalizedEmail, password: password)
        try await refreshAuthToken(for: result.user)
        let registeredEmail = result.user.email?.trimmedNilIfEmpty ?? normalizedEmail
        let role = UserRole.resolve(from: nil, isAdmin: false, email: registeredEmail)
        let newUser = User(
            id: nil,
            email: registeredEmail,
            username: normalizedUsername,
            whatsApp: whatsApp.trimmedNilIfEmpty,
            registrationDate: Date(),
            isAdmin: role.hasStaffAccess,
            role: role.rawValue,
            aiAccessEnabled: true,
            aiTextRequestsPerDay: role.defaultAITextRequestsPerDay,
            aiVisualRequestsPerDay: role.defaultAIVisualRequestsPerDay,
            aiAgentRequestsPerDay: role.defaultAIAgentRequestsPerDay,
            aiHistoryRetentionDays: role.defaultAIHistoryRetentionDays
        )

        try firestore.collection("users").document(result.user.uid).setData(from: newUser)
    }

    func updateCurrentProfile(
        username: String,
        whatsApp: String?,
        profileTagline: String?,
        profileBio: String?,
        instagramHandle: String?
    ) async throws -> User {
        guard let authUser = auth.currentUser else {
            throw NSError(
                domain: "FirebaseAuthService",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "Kein Benutzer angemeldet."]
            )
        }

        let normalizedEmail = authUser.email?.trimmedNilIfEmpty?.lowercased() ?? ""
        let normalizedUsername = Self.sanitizedUsername(
            username,
            authUserDisplayName: authUser.displayName,
            fallbackEmail: normalizedEmail
        )
        if normalizedUsername.count > 32 {
            throw NSError(
                domain: "FirebaseAuthService",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Der Benutzername darf maximal 32 Zeichen lang sein."]
            )
        }
        let normalizedWhatsApp = whatsApp?.trimmedNilIfEmpty
        let normalizedTagline = profileTagline?.trimmedNilIfEmpty
        let normalizedBio = profileBio?.trimmedNilIfEmpty
        let normalizedInstagramHandle = instagramHandle?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "@", with: "")
            .trimmedNilIfEmpty
        if let normalizedTagline, normalizedTagline.count > 60 {
            throw NSError(
                domain: "FirebaseAuthService",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Die Kurzinfo darf maximal 60 Zeichen lang sein."]
            )
        }
        if let normalizedBio, normalizedBio.count > 240 {
            throw NSError(
                domain: "FirebaseAuthService",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Die Bio darf maximal 240 Zeichen lang sein."]
            )
        }
        if let normalizedInstagramHandle, normalizedInstagramHandle.count > 40 {
            throw NSError(
                domain: "FirebaseAuthService",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Der Instagram-Handle ist zu lang."]
            )
        }

        let changeRequest = authUser.createProfileChangeRequest()
        changeRequest.displayName = normalizedUsername
        try await changeRequest.commitChanges()

        let payload: [String: Any] = [
            "username": normalizedUsername,
            "whatsApp": normalizedWhatsApp ?? NSNull(),
            "profileTagline": normalizedTagline ?? NSNull(),
            "profileBio": normalizedBio ?? NSNull(),
            "instagramHandle": normalizedInstagramHandle ?? NSNull()
        ]

        try await firestore.collection("users").document(authUser.uid).setData(payload, merge: true)
        try await refreshAuthToken(for: authUser)
        return try await fetchUser(uid: authUser.uid) ?? authUser.toAppUser()
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
        let storedIsAdmin = data["isAdmin"] as? Bool ?? false
        let resolvedRole = UserRole.resolve(
            from: data["role"] as? String,
            isAdmin: storedIsAdmin,
            email: email
        )
        let isAdmin = resolvedRole.hasStaffAccess
        let aiAccessEnabled = data["aiAccessEnabled"] as? Bool ?? true
        let aiTextRequestsPerDay = (data["aiTextRequestsPerDay"] as? NSNumber)?.intValue
            ?? resolvedRole.defaultAITextRequestsPerDay
        let aiVisualRequestsPerDay = (data["aiVisualRequestsPerDay"] as? NSNumber)?.intValue
            ?? resolvedRole.defaultAIVisualRequestsPerDay
        let aiAgentRequestsPerDay = (data["aiAgentRequestsPerDay"] as? NSNumber)?.intValue
            ?? resolvedRole.defaultAIAgentRequestsPerDay
        let aiHistoryRetentionDays = (data["aiHistoryRetentionDays"] as? NSNumber)?.intValue
            ?? resolvedRole.defaultAIHistoryRetentionDays

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
            profileTagline: (data["profileTagline"] as? String)?.trimmedNilIfEmpty,
            profileBio: (data["profileBio"] as? String)?.trimmedNilIfEmpty,
            instagramHandle: (data["instagramHandle"] as? String)?.trimmedNilIfEmpty,
            registrationDate: registrationDate,
            isAdmin: isAdmin,
            role: resolvedRole.rawValue,
            aiAccessEnabled: aiAccessEnabled,
            aiTextRequestsPerDay: aiTextRequestsPerDay,
            aiVisualRequestsPerDay: aiVisualRequestsPerDay,
            aiAgentRequestsPerDay: aiAgentRequestsPerDay,
            aiHistoryRetentionDays: aiHistoryRetentionDays
        )
    }

    private func syncUserDocument(
        for authUser: FirebaseAuth.User,
        preferredUsername: String? = nil
    ) async throws {
        let documentReference = firestore.collection("users").document(authUser.uid)
        let snapshot = try await documentReference.getDocument()
        let email = authUser.email?.trimmedNilIfEmpty?.lowercased() ?? ""
        let username = Self.sanitizedUsername(
            preferredUsername,
            authUserDisplayName: authUser.displayName,
            fallbackEmail: email
        )
        let bootstrapRole = UserRole.resolve(from: nil, isAdmin: false, email: email)

        guard snapshot.exists else {
            try await refreshAuthToken(for: authUser)
            let newUser = User(
                id: nil,
                email: email,
                username: username,
                whatsApp: nil,
                registrationDate: authUser.metadata.creationDate ?? .now,
                isAdmin: bootstrapRole.hasStaffAccess,
                role: bootstrapRole.rawValue,
                aiAccessEnabled: true,
                aiTextRequestsPerDay: bootstrapRole.defaultAITextRequestsPerDay,
                aiVisualRequestsPerDay: bootstrapRole.defaultAIVisualRequestsPerDay,
                aiAgentRequestsPerDay: bootstrapRole.defaultAIAgentRequestsPerDay,
                aiHistoryRetentionDays: bootstrapRole.defaultAIHistoryRetentionDays
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

        let storedIsAdmin = data["isAdmin"] as? Bool ?? false
        let resolvedRole = UserRole.resolve(
            from: data["role"] as? String,
            isAdmin: storedIsAdmin,
            email: email
        )

        if (data["role"] as? String)?.trimmedNilIfEmpty == nil || resolvedRole == .owner {
            repairFields["role"] = resolvedRole.rawValue
        }

        if (data["email"] as? String)?.trimmedNilIfEmpty?.lowercased() != email {
            repairFields["email"] = email
        }

        if (data["isAdmin"] as? Bool) != resolvedRole.hasStaffAccess {
            repairFields["isAdmin"] = resolvedRole.hasStaffAccess
        }

        if data["aiAccessEnabled"] == nil {
            repairFields["aiAccessEnabled"] = true
        }

        if data["aiTextRequestsPerDay"] == nil || resolvedRole == .owner {
            repairFields["aiTextRequestsPerDay"] = resolvedRole.defaultAITextRequestsPerDay
        }

        if data["aiVisualRequestsPerDay"] == nil || resolvedRole == .owner {
            repairFields["aiVisualRequestsPerDay"] = resolvedRole.defaultAIVisualRequestsPerDay
        }

        if data["aiAgentRequestsPerDay"] == nil || resolvedRole == .owner {
            repairFields["aiAgentRequestsPerDay"] = resolvedRole.defaultAIAgentRequestsPerDay
        }

        if data["aiHistoryRetentionDays"] == nil || resolvedRole == .owner {
            repairFields["aiHistoryRetentionDays"] = resolvedRole.defaultAIHistoryRetentionDays
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
        let fallbackEmail = email?.trimmedNilIfEmpty?.lowercased() ?? ""
        let resolvedRole = UserRole.resolve(from: nil, isAdmin: isAdmin, email: fallbackEmail)
        return User(
            id: uid,
            email: fallbackEmail,
            username: FirebaseAuthService.sanitizedUsername(
                displayName,
                authUserDisplayName: displayName,
                fallbackEmail: fallbackEmail
            ),
            whatsApp: nil,
            profileTagline: nil,
            profileBio: nil,
            instagramHandle: nil,
            registrationDate: metadata.creationDate ?? .now,
            isAdmin: isAdmin,
            role: resolvedRole.rawValue,
            aiAccessEnabled: true,
            aiTextRequestsPerDay: resolvedRole.defaultAITextRequestsPerDay,
            aiVisualRequestsPerDay: resolvedRole.defaultAIVisualRequestsPerDay,
            aiAgentRequestsPerDay: resolvedRole.defaultAIAgentRequestsPerDay,
            aiHistoryRetentionDays: resolvedRole.defaultAIHistoryRetentionDays
        )
    }
}

private extension String {
    var trimmedNilIfEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}
