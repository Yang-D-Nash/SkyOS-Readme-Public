import Foundation
import FirebaseFirestore
import FirebaseFunctions

protocol AdminUserManagementServicing {
    func observeUsers(_ onChange: @escaping @MainActor (Result<[User], Error>) -> Void) -> () -> Void
    func updateUser(_ user: User) async throws
}

final class FirestoreAdminUserManagementService: AdminUserManagementServicing {
    private let firestore: Firestore
    private let functions: Functions
    private let collectionName = "users"

    init(
        firestore: Firestore = Firestore.firestore(),
        functions: Functions = Functions.functions(region: "us-central1")
    ) {
        self.firestore = firestore
        self.functions = functions
    }

    func observeUsers(_ onChange: @escaping @MainActor (Result<[User], Error>) -> Void) -> () -> Void {
        let listener = firestore.collection(collectionName).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                let users = (snapshot?.documents ?? [])
                    .compactMap(mapManagedUser)
                    .sorted(by: sortUsers)
                onChange(.success(users))
            }
        }

        return {
            listener.remove()
        }
    }

    func updateUser(_ user: User) async throws {
        guard let userID = user.id, !userID.isEmpty else {
            throw NSError(
                domain: "AdminUserManagementStore",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Dieses Konto hat keine gueltige Benutzer-ID."]
            )
        }

        let resolvedRole = user.resolvedRole
        let resolvedQuotaPlan = user.resolvedQuotaPlan

        _ = try await functions
            .httpsCallable("setUserRole")
            .call([
                "uid": userID,
                "role": resolvedRole.rawValue
            ])

        let payload: [String: Any] = [
            "email": user.email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "registrationDateEpochMillis": user.registrationDateEpochMillis,
            "role": resolvedRole.rawValue,
            "isAdmin": resolvedRole.hasStaffAccess,
            "quotaPlan": resolvedQuotaPlan.rawValue,
            "aiAccessEnabled": user.aiAccessEnabled,
            "aiTextRequestsPerDay": max(1, user.aiTextRequestsPerDay),
            "aiVisualRequestsPerDay": max(1, user.aiVisualRequestsPerDay),
            "aiAgentRequestsPerDay": max(1, user.aiAgentRequestsPerDay),
            "aiHistoryRetentionDays": user.resolvedAIHistoryRetentionDays,
            "canManageMusicCatalog": user.canManageMusic,
            "canManageVideoCatalog": user.canManageVideos,
            "canModerateProfiles": user.canModerateUserProfiles,
            "updatedAt": FieldValue.serverTimestamp()
        ]

        try await firestore.collection(collectionName).document(userID).setData(payload, merge: true)
    }
}

private func mapManagedUser(document: QueryDocumentSnapshot) -> User? {
    let data = document.data()
    let email = normalizedManagedString(data["email"] as? String) ?? ""
    guard !email.isEmpty else { return nil }

    let username = sanitizedManagedUsername(
        data["username"] as? String,
        fallbackEmail: email
    )
    let storedIsAdmin = data["isAdmin"] as? Bool ?? false
    let resolvedRole = UserRole.resolve(
        from: data["role"] as? String,
        isAdmin: storedIsAdmin,
        email: email
    )
    let resolvedQuotaPlan = UserQuotaPlan.resolve(
        from: data["quotaPlan"] as? String,
        role: resolvedRole
    )
    let isAdmin = resolvedRole.hasStaffAccess
    let registrationDate = User.registrationDate(from: data)

    return User(
        id: document.documentID,
        email: email,
        username: username,
        whatsApp: normalizedManagedString(data["whatsApp"] as? String),
        registrationDate: registrationDate,
        isAdmin: isAdmin,
        role: resolvedRole.rawValue,
        quotaPlan: resolvedQuotaPlan.rawValue,
        aiAccessEnabled: data["aiAccessEnabled"] as? Bool ?? true,
        aiTextRequestsPerDay: (data["aiTextRequestsPerDay"] as? NSNumber)?.intValue
            ?? resolvedQuotaPlan.aiTextRequestsPerDay,
        aiVisualRequestsPerDay: (data["aiVisualRequestsPerDay"] as? NSNumber)?.intValue
            ?? resolvedQuotaPlan.aiVisualRequestsPerDay,
        aiAgentRequestsPerDay: (data["aiAgentRequestsPerDay"] as? NSNumber)?.intValue
            ?? resolvedQuotaPlan.aiAgentRequestsPerDay,
        aiHistoryRetentionDays: (data["aiHistoryRetentionDays"] as? NSNumber)?.intValue
            ?? resolvedQuotaPlan.aiHistoryRetentionDays,
        canManageMusicCatalog: (data["canManageMusicCatalog"] as? Bool) ?? (resolvedRole == .owner),
        canManageVideoCatalog: (data["canManageVideoCatalog"] as? Bool) ?? (resolvedRole == .owner),
        canModerateProfiles: (data["canModerateProfiles"] as? Bool) ?? (resolvedRole == .owner)
    )
}

@MainActor
final class AdminUserManagementStore: ObservableObject {
    static let shared = AdminUserManagementStore()

    @Published private(set) var users: [User] = []
    @Published private(set) var lastErrorMessage: String?

    private let service: AdminUserManagementServicing
    private var stopObserving: (() -> Void)?
    private var isObserving = false

    init(service: AdminUserManagementServicing = FirestoreAdminUserManagementService()) {
        self.service = service
    }

    func configureObservation(isAdmin: Bool) {
        guard isAdmin else {
            stopObserving?()
            stopObserving = nil
            isObserving = false
            users = []
            lastErrorMessage = nil
            return
        }

        guard !isObserving else { return }
        isObserving = true
        stopObserving?()
        stopObserving = service.observeUsers { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let users):
                self.users = users
                self.lastErrorMessage = nil
            case .failure(let error):
                self.lastErrorMessage = error.localizedDescription
            }
        }
    }

    func save(_ user: User) async throws {
        try await service.updateUser(user)
    }

    deinit {
        stopObserving?()
    }
}

private func sortUsers(lhs: User, rhs: User) -> Bool {
    let leftRoleRank = roleSortRank(lhs.resolvedRole)
    let rightRoleRank = roleSortRank(rhs.resolvedRole)

    if leftRoleRank != rightRoleRank {
        return leftRoleRank < rightRoleRank
    }

    let leftName = lhs.username.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    let rightName = rhs.username.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    if leftName != rightName {
        return leftName < rightName
    }

    return lhs.email.lowercased() < rhs.email.lowercased()
}

private func sanitizedManagedUsername(_ username: String?, fallbackEmail: String) -> String {
    normalizedManagedString(username)
        ?? normalizedManagedString(fallbackEmail.split(separator: "@").first.map(String.init))
        ?? "Skydown User"
}

private func normalizedManagedString(_ value: String?) -> String? {
    let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    return trimmed.isEmpty ? nil : trimmed
}

private func roleSortRank(_ role: UserRole) -> Int {
    switch role {
    case .owner:
        return 0
    case .admin:
        return 1
    case .subadmin:
        return 2
    case .user:
        return 3
    }
}
