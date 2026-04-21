import Foundation
import FirebaseAppCheck
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import FirebaseFunctions
import GoogleSignIn
import UIKit

protocol AuthServicing {
    func observeAuthState(_ onChange: @escaping @MainActor (User?) -> Void) -> () -> Void
    func signIn(email: String, password: String) async throws
    func signInWithGoogle(
        preferredUsername: String?,
        registrationConsent: RegistrationLegalConsent?
    ) async throws
    func register(
        username: String,
        email: String,
        whatsApp: String,
        password: String,
        registrationConsent: RegistrationLegalConsent
    ) async throws
    func updateCurrentProfile(
        username: String,
        whatsApp: String?,
        profileTagline: String?,
        profileBio: String?,
        instagramHandle: String?
    ) async throws -> User
    func updateCurrentAIAccessEnabled(_ enabled: Bool) async throws -> User
    func signOut() throws
    func deleteCurrentAccount() async throws
    func fetchCurrentUser() async throws -> User?
}

extension AuthServicing {
    func signInWithGoogle() async throws {
        try await signInWithGoogle(preferredUsername: nil, registrationConsent: nil)
    }
}

struct RegistrationLegalConsent: Sendable {
    var acceptedTerms: Bool
    var acceptedPrivacyPolicy: Bool
    var aiConsentEnabled: Bool
    var legalVersionLabel: String
    var consentSource: String

    var isValid: Bool {
        acceptedTerms &&
        acceptedPrivacyPolicy &&
        !normalizedLegalVersionLabel.isEmpty &&
        !normalizedConsentSource.isEmpty
    }

    var normalizedLegalVersionLabel: String {
        legalVersionLabel.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var normalizedConsentSource: String {
        consentSource.trimmingCharacters(in: .whitespacesAndNewlines)
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
    private static let claimsSyncCooldown: TimeInterval = 60 * 60
    private static let claimsSyncTimestampKeyPrefix = "skydown.auth.claimsSync.lastAttempt."

    private let auth: Auth
    private let firestore: Firestore
    private let functions: Functions
    private var currentUserAccessListener: ListenerRegistration?
    private var currentUserAccessFingerprint: String?

    init(
        auth: Auth = Auth.auth(),
        firestore: Firestore = Firestore.firestore(),
        functions: Functions = Functions.functions(region: "us-central1")
    ) {
        self.auth = auth
        self.firestore = firestore
        self.functions = functions
    }

    func observeAuthState(_ onChange: @escaping @MainActor (User?) -> Void) -> () -> Void {
        let handle = auth.addStateDidChangeListener { [weak self] _, firebaseUser in
            guard let self else { return }
            self.resetCurrentUserAccessObservation()

            Task {
                if let firebaseUser {
                    let expectedUID = firebaseUser.uid
                    let user = await self.currentSessionUser(for: firebaseUser)
                    guard self.auth.currentUser?.uid == expectedUID else { return }
                    self.startCurrentUserAccessObservation(
                        for: firebaseUser,
                        baseline: self.sessionAccessFingerprint(for: user),
                        onChange: onChange
                    )
                    await onChange(user)
                } else {
                    await onChange(nil)
                }
            }
        }

        return { [weak self, weak auth] in
            auth?.removeStateDidChangeListener(handle)
            self?.resetCurrentUserAccessObservation()
        }
    }

    func signIn(email: String, password: String) async throws {
        let result = try await auth.signIn(withEmail: email, password: password)
        await syncSessionClaimsIfPossible(for: result.user)
    }

    func signInWithGoogle(
        preferredUsername: String? = nil,
        registrationConsent: RegistrationLegalConsent? = nil
    ) async throws {
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
        let isNewUser = authResult.additionalUserInfo?.isNewUser ?? false

        if isNewUser, let registrationConsent, registrationConsent.isValid {
            await syncUserDocumentIfPossible(
                for: authResult.user,
                preferredUsername: preferredUsername,
                registrationConsent: registrationConsent
            )
            return
        }

        if isNewUser {
            try? await authResult.user.delete()
            try? auth.signOut()
            throw NSError(
                domain: "FirebaseAuthService",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Bitte nutze den Registrieren-Flow und bestaetige AGB sowie Datenschutz."]
            )
        }

        await syncUserDocumentIfPossible(for: authResult.user, preferredUsername: preferredUsername)
    }

    func register(
        username: String,
        email: String,
        whatsApp: String,
        password: String,
        registrationConsent: RegistrationLegalConsent
    ) async throws {
        try await ensureRegistrationsOpen()
        guard registrationConsent.isValid else {
            throw NSError(
                domain: "FirebaseAuthService",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Bitte akzeptiere AGB und Datenschutz, um fortzufahren."]
            )
        }
        let normalizedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let normalizedUsername = Self.sanitizedUsername(
            username,
            fallbackEmail: normalizedEmail
        )
        let result = try await auth.createUser(withEmail: normalizedEmail, password: password)
        await syncSessionClaimsIfPossible(for: result.user)
        let registeredEmail = result.user.email?.trimmedNilIfEmpty ?? normalizedEmail
        let role: UserRole = .user
        let quotaPlan = UserQuotaPlan.defaultPlan(for: role)
        let newUser = User(
            id: nil,
            email: registeredEmail,
            username: normalizedUsername,
            whatsApp: whatsApp.trimmedNilIfEmpty,
            registrationDate: Date(),
            isAdmin: role.hasStaffAccess,
            role: role.rawValue,
            quotaPlan: quotaPlan.rawValue,
            aiAccessEnabled: registrationConsent.aiConsentEnabled,
            aiTextRequestsPerDay: quotaPlan.aiTextRequestsPerDay,
            aiVisualRequestsPerDay: quotaPlan.aiVisualRequestsPerDay,
            aiAgentRequestsPerDay: quotaPlan.aiAgentRequestsPerDay,
            aiHistoryRetentionDays: quotaPlan.aiHistoryRetentionDays,
            canManageMusicCatalog: false,
            canManageVideoCatalog: false,
            canModerateProfiles: false
        )

        var payload = newUser.firestorePayload
        payload.merge(consentPayload(from: registrationConsent), uniquingKeysWith: { _, new in new })
        try await firestore.collection("users").document(result.user.uid).setData(payload)
        try await syncPublicProfileDocument(
            uid: result.user.uid,
            username: normalizedUsername,
            profileImageURL: nil,
            profileImagePath: nil,
            profileTagline: nil,
            profileBio: nil,
            instagramHandle: nil,
            whatsApp: whatsApp.trimmedNilIfEmpty
        )
        try await syncSessionClaims(for: result.user)
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
        let currentSnapshot = try await firestore.collection("users").document(authUser.uid).getDocument()
        try await syncPublicProfileDocument(
            uid: authUser.uid,
            username: normalizedUsername,
            profileImageURL: (currentSnapshot.data()?["profileImageURL"] as? String)?.trimmedNilIfEmpty,
            profileImagePath: (currentSnapshot.data()?["profileImagePath"] as? String)?.trimmedNilIfEmpty,
            profileTagline: normalizedTagline,
            profileBio: normalizedBio,
            instagramHandle: normalizedInstagramHandle,
            whatsApp: normalizedWhatsApp
        )
        try await refreshAuthToken(for: authUser)
        return try await fetchUser(uid: authUser.uid) ?? authUser.toAppUser()
    }

    func updateCurrentAIAccessEnabled(_ enabled: Bool) async throws -> User {
        guard let authUser = auth.currentUser else {
            throw NSError(
                domain: "FirebaseAuthService",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "Kein Benutzer angemeldet."]
            )
        }

        try await firestore.collection("users").document(authUser.uid).setData([
            "aiAccessEnabled": enabled,
            "aiConsentGiven": enabled,
            "aiConsentUpdatedAt": Timestamp(date: .now),
            "aiConsentSource": "ios_settings"
        ], merge: true)

        try await refreshAuthToken(for: authUser)
        return try await fetchUser(uid: authUser.uid) ?? authUser.toAppUser()
    }

    func signOut() throws {
        GIDSignIn.sharedInstance.signOut()
        try auth.signOut()
    }

    func deleteCurrentAccount() async throws {
        guard auth.currentUser != nil else {
            throw NSError(
                domain: "FirebaseAuthService",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "Kein Benutzer angemeldet."]
            )
        }

        do {
            _ = try await callCloudFunction("deleteCurrentUserAccount")
        } catch {
            throw readableAccountDeletionError(error)
        }

        GIDSignIn.sharedInstance.signOut()
        try? auth.signOut()
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
        let resolvedQuotaPlan = UserQuotaPlan.resolve(
            from: data["quotaPlan"] as? String,
            role: resolvedRole
        )
        let aiTextRequestsPerDay = (data["aiTextRequestsPerDay"] as? NSNumber)?.intValue
            ?? resolvedQuotaPlan.aiTextRequestsPerDay
        let aiVisualRequestsPerDay = (data["aiVisualRequestsPerDay"] as? NSNumber)?.intValue
            ?? resolvedQuotaPlan.aiVisualRequestsPerDay
        let aiAgentRequestsPerDay = (data["aiAgentRequestsPerDay"] as? NSNumber)?.intValue
            ?? resolvedQuotaPlan.aiAgentRequestsPerDay
        let aiHistoryRetentionDays = (data["aiHistoryRetentionDays"] as? NSNumber)?.intValue
            ?? resolvedQuotaPlan.aiHistoryRetentionDays
        let aiSubscriptionCurrentPeriodEndEpochSeconds =
            (data["aiSubscriptionCurrentPeriodEndEpochSeconds"] as? NSNumber)?.intValue
        let aiSubscriptionCheckoutExpiresAtEpochSeconds =
            (data["aiSubscriptionCheckoutExpiresAtEpochSeconds"] as? NSNumber)?.intValue

        let registrationDate = User.registrationDate(
            from: data,
            fallback: authUser?.metadata.creationDate
        )

        return User(
            id: snapshot.documentID,
            email: email,
            username: username,
            profileImageURL: (data["profileImageURL"] as? String)?.trimmedNilIfEmpty,
            whatsApp: whatsApp?.trimmedNilIfEmpty,
            profileTagline: (data["profileTagline"] as? String)?.trimmedNilIfEmpty,
            profileBio: (data["profileBio"] as? String)?.trimmedNilIfEmpty,
            instagramHandle: (data["instagramHandle"] as? String)?.trimmedNilIfEmpty,
            registrationDate: registrationDate,
            isAdmin: isAdmin,
            role: resolvedRole.rawValue,
            quotaPlan: resolvedQuotaPlan.rawValue,
            aiAccessEnabled: aiAccessEnabled,
            aiTextRequestsPerDay: aiTextRequestsPerDay,
            aiVisualRequestsPerDay: aiVisualRequestsPerDay,
            aiAgentRequestsPerDay: aiAgentRequestsPerDay,
            aiHistoryRetentionDays: aiHistoryRetentionDays,
            aiSubscriptionStatus: (data["aiSubscriptionStatus"] as? String)?.trimmedNilIfEmpty,
            aiSubscriptionPlan: (data["aiSubscriptionPlan"] as? String)?.trimmedNilIfEmpty,
            aiSubscriptionCurrentPeriodEndEpochSeconds: aiSubscriptionCurrentPeriodEndEpochSeconds,
            aiSubscriptionCheckoutExpiresAtEpochSeconds: aiSubscriptionCheckoutExpiresAtEpochSeconds,
            aiSubscriptionCancelAtPeriodEnd: data["aiSubscriptionCancelAtPeriodEnd"] as? Bool ?? false,
            aiSubscriptionProvider: (data["aiSubscriptionProvider"] as? String)?.trimmedNilIfEmpty,
            aiSubscriptionSourcePlatform: (data["aiSubscriptionSourcePlatform"] as? String)?.trimmedNilIfEmpty,
            aiSubscriptionProductID: (data["aiSubscriptionProductId"] as? String)?.trimmedNilIfEmpty,
            canManageMusicCatalog: (data["canManageMusicCatalog"] as? Bool) ?? (resolvedRole == .owner),
            canManageVideoCatalog: (data["canManageVideoCatalog"] as? Bool) ?? (resolvedRole == .owner),
            canModerateProfiles: (data["canModerateProfiles"] as? Bool) ?? (resolvedRole == .owner)
        )
    }

    private func syncUserDocument(
        for authUser: FirebaseAuth.User,
        preferredUsername: String? = nil,
        registrationConsent: RegistrationLegalConsent? = nil
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
        let bootstrapQuotaPlan = UserQuotaPlan.defaultPlan(for: bootstrapRole)

        guard snapshot.exists else {
            try await ensureRegistrationAllowedForBootstrap(authUser: authUser, bootstrapRole: bootstrapRole)
            let newUser = User(
                id: nil,
                email: email,
                username: username,
                profileImageURL: nil,
                whatsApp: nil,
                registrationDate: authUser.metadata.creationDate ?? .now,
                isAdmin: bootstrapRole.hasStaffAccess,
                role: bootstrapRole.rawValue,
                quotaPlan: bootstrapQuotaPlan.rawValue,
                aiAccessEnabled: registrationConsent?.aiConsentEnabled ?? true,
                aiTextRequestsPerDay: bootstrapQuotaPlan.aiTextRequestsPerDay,
                aiVisualRequestsPerDay: bootstrapQuotaPlan.aiVisualRequestsPerDay,
                aiAgentRequestsPerDay: bootstrapQuotaPlan.aiAgentRequestsPerDay,
                aiHistoryRetentionDays: bootstrapQuotaPlan.aiHistoryRetentionDays,
                canManageMusicCatalog: bootstrapRole == .owner,
                canManageVideoCatalog: bootstrapRole == .owner,
                canModerateProfiles: bootstrapRole == .owner
            )
            let effectiveConsent = registrationConsent ?? .legacy(
                aiConsentEnabled: newUser.aiAccessEnabled,
                source: "ios_legacy_sync"
            )
            var payload = newUser.firestorePayload
            payload.merge(consentPayload(from: effectiveConsent), uniquingKeysWith: { _, new in new })
            try await documentReference.setData(payload)
            try await syncPublicProfileDocument(
                uid: authUser.uid,
                username: username,
                profileImageURL: nil,
                profileImagePath: nil,
                profileTagline: nil,
                profileBio: nil,
                instagramHandle: nil,
                whatsApp: nil
            )
            await syncSessionClaimsIfPossible(for: authUser)
            return
        }

        let data = snapshot.data() ?? [:]
        var repairFields: [String: Any] = [:]

        if (data["username"] as? String)?.trimmedNilIfEmpty == nil {
            repairFields["username"] = username
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

        let resolvedQuotaPlan = UserQuotaPlan.resolve(
            from: data["quotaPlan"] as? String,
            role: resolvedRole
        )

        if (data["quotaPlan"] as? String)?.trimmedNilIfEmpty != resolvedQuotaPlan.rawValue || resolvedRole == .owner {
            repairFields["quotaPlan"] = resolvedQuotaPlan.rawValue
        }

        if data["aiAccessEnabled"] == nil {
            repairFields["aiAccessEnabled"] = true
        }

        if data["termsAcceptedAt"] == nil {
            repairFields["termsAcceptedAt"] = Timestamp(date: .now)
        }

        if data["privacyAcceptedAt"] == nil {
            repairFields["privacyAcceptedAt"] = Timestamp(date: .now)
        }

        if (data["termsVersion"] as? String)?.trimmedNilIfEmpty == nil {
            repairFields["termsVersion"] = "legacy"
        }

        if (data["privacyVersion"] as? String)?.trimmedNilIfEmpty == nil {
            repairFields["privacyVersion"] = "legacy"
        }

        if (data["legalConsentSource"] as? String)?.trimmedNilIfEmpty == nil {
            repairFields["legalConsentSource"] = "ios_legacy_sync"
        }

        if data["aiConsentGiven"] == nil {
            repairFields["aiConsentGiven"] = data["aiAccessEnabled"] as? Bool ?? true
        }

        if data["aiConsentUpdatedAt"] == nil {
            repairFields["aiConsentUpdatedAt"] = Timestamp(date: .now)
        }

        if (data["aiConsentSource"] as? String)?.trimmedNilIfEmpty == nil {
            repairFields["aiConsentSource"] = "ios_legacy_sync"
        }

        if let registrationConsent, registrationConsent.isValid {
            repairFields.merge(consentPayload(from: registrationConsent), uniquingKeysWith: { _, new in new })
            repairFields["aiAccessEnabled"] = registrationConsent.aiConsentEnabled
            repairFields["aiConsentGiven"] = registrationConsent.aiConsentEnabled
        }

        if data["aiTextRequestsPerDay"] == nil || resolvedRole == .owner {
            repairFields["aiTextRequestsPerDay"] = resolvedQuotaPlan.aiTextRequestsPerDay
        }

        if data["aiVisualRequestsPerDay"] == nil || resolvedRole == .owner {
            repairFields["aiVisualRequestsPerDay"] = resolvedQuotaPlan.aiVisualRequestsPerDay
        }

        if data["aiAgentRequestsPerDay"] == nil || resolvedRole == .owner {
            repairFields["aiAgentRequestsPerDay"] = resolvedQuotaPlan.aiAgentRequestsPerDay
        }

        if data["aiHistoryRetentionDays"] == nil || resolvedRole == .owner {
            repairFields["aiHistoryRetentionDays"] = resolvedQuotaPlan.aiHistoryRetentionDays
        }

        if data["canManageMusicCatalog"] == nil || resolvedRole == .owner {
            repairFields["canManageMusicCatalog"] = resolvedRole == .owner
        }

        if data["canManageVideoCatalog"] == nil || resolvedRole == .owner {
            repairFields["canManageVideoCatalog"] = resolvedRole == .owner
        }

        if data["canModerateProfiles"] == nil || resolvedRole == .owner {
            repairFields["canModerateProfiles"] = resolvedRole == .owner
        }

        if !repairFields.isEmpty {
            try await refreshAuthToken(for: authUser)
            try await documentReference.setData(repairFields, merge: true)
        }

        try await syncPublicProfileDocument(
            uid: authUser.uid,
            username: (data["username"] as? String)?.trimmedNilIfEmpty ?? username,
            profileImageURL: (data["profileImageURL"] as? String)?.trimmedNilIfEmpty,
            profileImagePath: (data["profileImagePath"] as? String)?.trimmedNilIfEmpty,
            profileTagline: (data["profileTagline"] as? String)?.trimmedNilIfEmpty,
            profileBio: (data["profileBio"] as? String)?.trimmedNilIfEmpty,
            instagramHandle: (data["instagramHandle"] as? String)?.trimmedNilIfEmpty,
            whatsApp: (data["whatsApp"] as? String)?.trimmedNilIfEmpty
        )
        scheduleSessionClaimsSyncIfNeeded(for: authUser)
    }

    private func currentSessionUser(for firebaseUser: FirebaseAuth.User) async -> User {
        do {
            try await syncUserDocument(for: firebaseUser)
            return try await fetchUser(uid: firebaseUser.uid) ?? firebaseUser.toAppUser()
        } catch {
            return firebaseUser.toAppUser()
        }
    }

    private func startCurrentUserAccessObservation(
        for authUser: FirebaseAuth.User,
        baseline: String?,
        onChange: @escaping @MainActor (User?) -> Void
    ) {
        resetCurrentUserAccessObservation()
        currentUserAccessFingerprint = baseline
        let observedUID = authUser.uid

        currentUserAccessListener = firestore.collection("users").document(observedUID).addSnapshotListener { [weak self] snapshot, error in
            guard let self else { return }
            guard error == nil else { return }
            guard let nextFingerprint = self.sessionAccessFingerprint(from: snapshot) else { return }
            guard nextFingerprint != self.currentUserAccessFingerprint else { return }

            self.currentUserAccessFingerprint = nextFingerprint

            Task {
                guard let currentAuthUser = self.auth.currentUser, currentAuthUser.uid == observedUID else { return }
                try? await self.refreshAuthToken(for: currentAuthUser)
                let refreshedUser = await self.currentSessionUser(for: currentAuthUser)
                await onChange(refreshedUser)
            }
        }
    }

    private func resetCurrentUserAccessObservation() {
        currentUserAccessListener?.remove()
        currentUserAccessListener = nil
        currentUserAccessFingerprint = nil
    }

    private func sessionAccessFingerprint(for user: User?) -> String? {
        guard let user else { return nil }
        let userID = user.id?.trimmedNilIfEmpty ?? auth.currentUser?.uid
        guard let userID else { return nil }

        return [
            userID,
            user.role.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            String(user.isAdmin),
            String(user.canManageMusicCatalog),
            String(user.canManageVideoCatalog),
            String(user.canModerateProfiles)
        ].joined(separator: "|")
    }

    private func sessionAccessFingerprint(from snapshot: DocumentSnapshot?) -> String? {
        guard let snapshot, snapshot.exists else { return nil }
        let data = snapshot.data() ?? [:]

        return [
            snapshot.documentID,
            ((data["role"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()) ?? "",
            String((data["isAdmin"] as? Bool) == true),
            String((data["canManageMusicCatalog"] as? Bool) == true),
            String((data["canManageVideoCatalog"] as? Bool) == true),
            String((data["canModerateProfiles"] as? Bool) == true)
        ].joined(separator: "|")
    }

    private func refreshAuthToken(for authUser: FirebaseAuth.User) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            let resumeGate = ContinuationResumeGate()

            authUser.getIDTokenForcingRefresh(true) { _, error in
                if let error {
                    resumeGate.resume {
                        continuation.resume(throwing: error)
                    }
                } else {
                    resumeGate.resume {
                        continuation.resume(returning: ())
                    }
                }
            }
        }
    }

    private func syncSessionClaims(for authUser: FirebaseAuth.User) async throws {
        do {
            _ = try await callCloudFunction("syncCurrentUserClaims")
            try await refreshAuthToken(for: authUser)
        } catch {
            let nsError = error as NSError
            let functionsCode = FunctionsErrorCode(rawValue: nsError.code)
            if nsError.domain == FunctionsErrorDomain,
               functionsCode == .notFound || functionsCode == .unimplemented {
                return
            }
            throw error
        }
    }

    private func callCloudFunction(
        _ functionName: String,
        payload: Any = [:]
    ) async throws -> HTTPSCallableResult {
        try await functions.invokeCallable(functionName, payload: payload)
    }

    private func syncSessionClaimsIfPossible(for authUser: FirebaseAuth.User) async {
        do {
            try await syncSessionClaims(for: authUser)
        } catch {
            print("Dev Hinweis: Session Claims konnten nicht synchronisiert werden: \(error.localizedDescription)")
        }
    }

    private func scheduleSessionClaimsSyncIfNeeded(for authUser: FirebaseAuth.User) {
        let cacheKey = Self.claimsSyncTimestampKeyPrefix + authUser.uid
        let now = Date().timeIntervalSince1970
        let lastAttempt = UserDefaults.standard.double(forKey: cacheKey)
        guard now - lastAttempt >= Self.claimsSyncCooldown else { return }
        UserDefaults.standard.set(now, forKey: cacheKey)

        let expectedUID = authUser.uid
        Task { [weak self] in
            guard let self else { return }
            guard let currentUser = self.auth.currentUser, currentUser.uid == expectedUID else { return }
            await self.syncSessionClaimsIfPossible(for: currentUser)
        }
    }

    private func readableAccountDeletionError(_ error: Error) -> Error {
        let nsError = error as NSError

        if nsError.domain == FunctionsErrorDomain,
           let functionsCode = FunctionsErrorCode(rawValue: nsError.code) {
            let message: String
            switch functionsCode {
            case .failedPrecondition:
                message = "Bitte melde dich erneut an, bevor du dein Konto loeschst."
            case .unauthenticated:
                message = "Bitte melde dich an, bevor du dein Konto loeschst."
            case .notFound, .unimplemented:
                message = "Die serverseitige Kontoloeschung ist noch nicht verfuegbar."
            default:
                message = nsError.localizedDescription
            }

            return NSError(
                domain: "FirebaseAuthService",
                code: nsError.code,
                userInfo: [NSLocalizedDescriptionKey: message]
            )
        }

        if let authCode = AuthErrorCode(rawValue: nsError.code),
           authCode == .requiresRecentLogin {
            return NSError(
                domain: "FirebaseAuthService",
                code: nsError.code,
                userInfo: [NSLocalizedDescriptionKey: "Bitte melde dich erneut an, bevor du dein Konto loeschst."]
            )
        }

        return error
    }

    private func syncUserDocumentIfPossible(
        for authUser: FirebaseAuth.User,
        preferredUsername: String? = nil,
        registrationConsent: RegistrationLegalConsent? = nil
    ) async {
        do {
            try await syncUserDocument(
                for: authUser,
                preferredUsername: preferredUsername,
                registrationConsent: registrationConsent
            )
        } catch {
            print("Dev Hinweis: Benutzerdokument konnte nicht synchronisiert werden: \(error.localizedDescription)")
        }
    }

    private func ensureRegistrationsOpen() async throws {
        let snapshot = try await firestore.collection("system").document("runtimeConfig").getDocument()
        let data = snapshot.data() ?? [:]
        let registrationsEnabled = (data["registrationsEnabled"] as? Bool) ?? true
        let lockdown = (data["lockdown"] as? Bool) ?? false

        if !registrationsEnabled || lockdown {
            throw NSError(
                domain: "FirebaseAuthService",
                code: 423,
                userInfo: [NSLocalizedDescriptionKey: "Registrierungen sind derzeit pausiert."]
            )
        }
    }

    private func ensureRegistrationAllowedForBootstrap(
        authUser: FirebaseAuth.User,
        bootstrapRole: UserRole
    ) async throws {
        guard bootstrapRole != .owner else { return }

        do {
            try await ensureRegistrationsOpen()
        } catch {
            try? await authUser.delete()
            throw error
        }
    }

    private func syncPublicProfileDocument(
        uid: String,
        username: String,
        profileImageURL: String?,
        profileImagePath: String?,
        profileTagline: String?,
        profileBio: String?,
        instagramHandle: String?,
        whatsApp: String?
    ) async throws {
        let documentReference = firestore.collection("userProfiles").document(uid)
        let snapshot = try await documentReference.getDocument()
        let createdAt = (snapshot.data()?["createdAt"] as? Timestamp) ?? Timestamp(date: .now)
        var payload: [String: Any] = [
            "ownerUid": uid,
            "username": username,
            "createdAt": createdAt,
            "updatedAt": Timestamp(date: .now)
        ]

        payload["profileImageURL"] = profileImageURL ?? NSNull()
        payload["profileImagePath"] = profileImagePath ?? NSNull()
        payload["profileTagline"] = profileTagline ?? NSNull()
        payload["profileBio"] = profileBio ?? NSNull()
        payload["instagramHandle"] = instagramHandle ?? NSNull()
        payload["whatsApp"] = whatsApp ?? NSNull()

        do {
            try await documentReference.setData(payload, merge: true)
        } catch {
            let nsError = error as NSError
            if nsError.domain == FirestoreErrorDomain,
               nsError.code == FirestoreErrorCode.permissionDenied.rawValue {
                return
            }

            throw error
        }
    }

    private func consentPayload(from consent: RegistrationLegalConsent) -> [String: Any] {
        let now = Timestamp(date: .now)
        let version = consent.normalizedLegalVersionLabel
        let source = consent.normalizedConsentSource
        return [
            "termsAcceptedAt": now,
            "privacyAcceptedAt": now,
            "termsVersion": version,
            "privacyVersion": version,
            "legalConsentSource": source,
            "aiConsentGiven": consent.aiConsentEnabled,
            "aiConsentUpdatedAt": now,
            "aiConsentSource": source
        ]
    }

    static func sanitizedUsername(
        _ username: String?,
        authUserDisplayName: String? = nil,
        fallbackEmail: String
    ) -> String {
        let candidate = username?.trimmedNilIfEmpty
            ?? authUserDisplayName?.trimmedNilIfEmpty
            ?? fallbackEmail.split(separator: "@").first.map(String.init)?.trimmedNilIfEmpty
            ?? "SkyOS User"
        let trimmedCandidate = candidate.trimmingCharacters(in: .whitespacesAndNewlines)

        if trimmedCandidate.count <= 32 {
            return trimmedCandidate
        }

        return String(trimmedCandidate.prefix(32)).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .sorted { lhs, rhs in
                rank(for: lhs.activationState) < rank(for: rhs.activationState)
            }

        let candidateWindows = scenes.flatMap { scene in
            scene.windows.sorted { lhs, rhs in
                score(for: lhs) > score(for: rhs)
            }
        }

        for window in candidateWindows {
            if let topController = resolvedTopViewController(from: window.rootViewController) {
                return topController
            }
        }

        return nil
    }

    private func resolvedTopViewController(from root: UIViewController?) -> UIViewController? {
        guard let root else { return nil }

        if let presented = root.presentedViewController {
            return resolvedTopViewController(from: presented)
        }

        if let navigationController = root as? UINavigationController {
            return resolvedTopViewController(from: navigationController.visibleViewController)
        }

        if let tabBarController = root as? UITabBarController {
            return resolvedTopViewController(from: tabBarController.selectedViewController)
        }

        if let splitViewController = root as? UISplitViewController {
            return resolvedTopViewController(from: splitViewController.viewControllers.last)
        }

        return root
    }

    private func rank(for state: UIScene.ActivationState) -> Int {
        switch state {
        case .foregroundActive: return 0
        case .foregroundInactive: return 1
        case .background: return 2
        case .unattached: return 3
        @unknown default: return 4
        }
    }

    private func score(for window: UIWindow) -> Int {
        var value = 0
        if window.isKeyWindow { value += 100 }
        if !window.isHidden { value += 10 }
        if window.alpha > 0 { value += 1 }
        return value
    }
}

extension Functions {
    /// Uses the callback-based API to avoid runtime crashes seen with the async wrapper on some iOS builds.
    func invokeCallable(
        _ functionName: String,
        payload: Any = [:]
    ) async throws -> HTTPSCallableResult {
        do {
            return try await invokeCallableOnce(functionName, payload: payload)
        } catch {
            guard shouldRetryAfterRefreshingAppCheckToken(for: error) else {
                throw error
            }

            try await refreshAppCheckToken()
            return try await invokeCallableOnce(functionName, payload: payload)
        }
    }

    private func invokeCallableOnce(
        _ functionName: String,
        payload: Any = [:]
    ) async throws -> HTTPSCallableResult {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<HTTPSCallableResult, Error>) in
            let resumeGate = ContinuationResumeGate()

            httpsCallable(functionName).call(payload) { result, error in
                if let error {
                    resumeGate.resume {
                        continuation.resume(throwing: error)
                    }
                    return
                }

                guard let result else {
                    resumeGate.resume {
                        continuation.resume(
                            throwing: NSError(
                            domain: "FirebaseFunctions",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "Cloud Function \(functionName) lieferte kein Ergebnis."]
                        )
                        )
                    }
                    return
                }

                resumeGate.resume {
                    continuation.resume(returning: result)
                }
            }
        }
    }

    private func shouldRetryAfterRefreshingAppCheckToken(for error: Error) -> Bool {
        let nsError = error as NSError
        guard nsError.domain == FunctionsErrorDomain,
              FunctionsErrorCode(rawValue: nsError.code) == .failedPrecondition else {
            return false
        }

        let message = nsError.localizedDescription.lowercased()
        return message.contains("app check") || message.contains("validierung")
    }

    private func refreshAppCheckToken() async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            AppCheck.appCheck().token(forcingRefresh: true) { token, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }

                guard token != nil else {
                    continuation.resume(
                        throwing: NSError(
                            domain: "FirebaseAppCheck",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "App Check Token konnte nicht aktualisiert werden."]
                        )
                    )
                    return
                }

                continuation.resume(returning: ())
            }
        }
    }
}

private final class ContinuationResumeGate {
    private let lock = NSLock()
    private var hasResumed = false

    func resume(_ block: () -> Void) {
        lock.lock()
        defer { lock.unlock() }

        guard !hasResumed else { return }
        hasResumed = true
        block()
    }
}

private extension FirebaseAuth.User {
    func toAppUser(isAdmin: Bool = false) -> User {
        let fallbackEmail = email?.trimmedNilIfEmpty?.lowercased() ?? ""
        let resolvedRole = UserRole.resolve(from: nil, isAdmin: isAdmin, email: fallbackEmail)
        let quotaPlan = UserQuotaPlan.defaultPlan(for: resolvedRole)
        return User(
            id: uid,
            email: fallbackEmail,
            username: FirebaseAuthService.sanitizedUsername(
                displayName,
                authUserDisplayName: displayName,
                fallbackEmail: fallbackEmail
            ),
            profileImageURL: nil,
            whatsApp: nil,
            profileTagline: nil,
            profileBio: nil,
            instagramHandle: nil,
            registrationDate: metadata.creationDate ?? .now,
            isAdmin: isAdmin,
            role: resolvedRole.rawValue,
            quotaPlan: quotaPlan.rawValue,
            aiAccessEnabled: true,
            aiTextRequestsPerDay: quotaPlan.aiTextRequestsPerDay,
            aiVisualRequestsPerDay: quotaPlan.aiVisualRequestsPerDay,
            aiAgentRequestsPerDay: quotaPlan.aiAgentRequestsPerDay,
            aiHistoryRetentionDays: quotaPlan.aiHistoryRetentionDays,
            canManageMusicCatalog: resolvedRole == .owner,
            canManageVideoCatalog: resolvedRole == .owner,
            canModerateProfiles: resolvedRole == .owner
        )
    }
}

private extension RegistrationLegalConsent {
    static func legacy(aiConsentEnabled: Bool, source: String) -> RegistrationLegalConsent {
        RegistrationLegalConsent(
            acceptedTerms: true,
            acceptedPrivacyPolicy: true,
            aiConsentEnabled: aiConsentEnabled,
            legalVersionLabel: "legacy",
            consentSource: source
        )
    }
}

private extension String {
    var trimmedNilIfEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}
