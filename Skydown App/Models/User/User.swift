//
//  User.swift
//  Skydown App
//
//  Created by Yang D. Nash on 18.08.25.
//


import Foundation
import FirebaseFirestore

enum UserRole: String, Codable, CaseIterable {
    case owner
    case admin
    case subadmin
    case user

    static let ownerEmail = "nash.lioncorna@gmail.com"

    static func resolve(from rawValue: String?, isAdmin: Bool, email: String? = nil) -> UserRole {
        if normalizedEmail(email) == ownerEmail {
            return .owner
        }

        if let normalized = rawValue?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased(),
           let role = UserRole(rawValue: normalized) {
            return role
        }

        return isAdmin ? .admin : .user
    }

    var hasStaffAccess: Bool {
        switch self {
        case .owner, .admin:
            return true
        case .subadmin, .user:
            return false
        }
    }

    var hasAdminWorkspaceAccess: Bool {
        switch self {
        case .owner:
            return true
        case .admin, .subadmin, .user:
            return false
        }
    }

    var defaultAITextRequestsPerDay: Int {
        UserQuotaPlan.defaultPlan(for: self).aiTextRequestsPerDay
    }

    var defaultAIVisualRequestsPerDay: Int {
        UserQuotaPlan.defaultPlan(for: self).aiVisualRequestsPerDay
    }

    var defaultAIAgentRequestsPerDay: Int {
        UserQuotaPlan.defaultPlan(for: self).aiAgentRequestsPerDay
    }

    var defaultAIHistoryRetentionDays: Int {
        UserQuotaPlan.defaultPlan(for: self).aiHistoryRetentionDays
    }

    private static func normalizedEmail(_ email: String?) -> String? {
        let trimmed = email?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
        return trimmed.isEmpty ? nil : trimmed
    }
}

enum UserQuotaPlan: String, Codable, CaseIterable {
    case ownerUnlimited = "owner_unlimited"
    case internalTeam = "internal_team"
    case free = "free"
    case creator = "creator"
    case studio = "studio"

    static func defaultPlan(for role: UserRole) -> UserQuotaPlan {
        switch role {
        case .owner:
            return .ownerUnlimited
        case .admin:
            return .creator
        case .subadmin:
            return .creator
        case .user:
            return .free
        }
    }

    static func resolve(from rawValue: String?, role: UserRole) -> UserQuotaPlan {
        let normalized = rawValue?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        return normalized.flatMap(UserQuotaPlan.init(rawValue:)) ?? defaultPlan(for: role)
    }

    var aiTextRequestsPerDay: Int {
        switch self {
        case .ownerUnlimited:
            return 5000
        case .internalTeam:
            return 240
        case .free:
            return 30
        case .creator:
            return 120
        case .studio:
            return 240
        }
    }

    var aiVisualRequestsPerDay: Int {
        switch self {
        case .ownerUnlimited:
            return 1200
        case .internalTeam:
            return 40
        case .free:
            return 4
        case .creator:
            return 20
        case .studio:
            return 40
        }
    }

    var aiAgentRequestsPerDay: Int {
        switch self {
        case .ownerUnlimited:
            return 3000
        case .internalTeam:
            return 140
        case .free:
            return 18
        case .creator:
            return 70
        case .studio:
            return 140
        }
    }

    var aiHistoryRetentionDays: Int {
        switch self {
        case .ownerUnlimited, .internalTeam, .studio:
            return 30
        case .creator:
            return 7
        case .free:
            return 3
        }
    }
}

struct User: Codable, Identifiable {
    @DocumentID var id: String?
    var email: String
    var username: String
    var profileImageURL: String?
    var whatsApp: String?
    var profileTagline: String?
    var profileBio: String?
    var instagramHandle: String?
    var registrationDate: Date
    var isAdmin: Bool = false
    var role: String = UserRole.user.rawValue
    var quotaPlan: String = UserQuotaPlan.free.rawValue
    var aiAccessEnabled: Bool = true
    var aiTextRequestsPerDay: Int = UserRole.user.defaultAITextRequestsPerDay
    var aiVisualRequestsPerDay: Int = UserRole.user.defaultAIVisualRequestsPerDay
    var aiAgentRequestsPerDay: Int = UserRole.user.defaultAIAgentRequestsPerDay
    var aiHistoryRetentionDays: Int = UserRole.user.defaultAIHistoryRetentionDays
    var aiSubscriptionStatus: String? = nil
    var aiSubscriptionPlan: String? = nil
    var aiSubscriptionCurrentPeriodEndEpochSeconds: Int? = nil
    var aiSubscriptionCheckoutExpiresAtEpochSeconds: Int? = nil
    var aiSubscriptionCancelAtPeriodEnd: Bool = false
    var aiSubscriptionProvider: String? = nil
    var aiSubscriptionSourcePlatform: String? = nil
    var aiSubscriptionProductID: String? = nil
    var canManageMusicCatalog: Bool = false
    var canManageVideoCatalog: Bool = false
    var canModerateProfiles: Bool = false

    var resolvedRole: UserRole {
        UserRole.resolve(from: role, isAdmin: isAdmin, email: email)
    }

    var resolvedQuotaPlan: UserQuotaPlan {
        UserQuotaPlan.resolve(from: quotaPlan, role: resolvedRole)
    }

    var hasStaffAccess: Bool {
        resolvedRole.hasStaffAccess
    }

    var hasAdminWorkspaceAccess: Bool {
        resolvedRole.hasAdminWorkspaceAccess
    }

    var canManageMusic: Bool {
        isPlatformOwner || (resolvedRole == .admin && canManageMusicCatalog)
    }

    var canManageVideos: Bool {
        isPlatformOwner || (resolvedRole == .admin && canManageVideoCatalog)
    }

    var canModerateUserProfiles: Bool {
        isPlatformOwner || (resolvedRole == .admin && canModerateProfiles)
    }

    var resolvedAITextRequestsPerDay: Int {
        max(aiTextRequestsPerDay, resolvedQuotaPlan.aiTextRequestsPerDay)
    }

    var resolvedAIVisualRequestsPerDay: Int {
        max(aiVisualRequestsPerDay, resolvedQuotaPlan.aiVisualRequestsPerDay)
    }

    var resolvedAIAgentRequestsPerDay: Int {
        max(aiAgentRequestsPerDay, resolvedQuotaPlan.aiAgentRequestsPerDay)
    }

    var resolvedAIHistoryRetentionDays: Int {
        switch aiHistoryRetentionDays {
        case 1, 3, 7, 30:
            return aiHistoryRetentionDays
        default:
            return resolvedRole.defaultAIHistoryRetentionDays
        }
    }

    var isPlatformOwner: Bool {
        resolvedRole == .owner
    }

    var normalizedAISubscriptionStatus: String {
        aiSubscriptionStatus?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() ?? ""
    }

    var normalizedAISubscriptionProvider: String {
        aiSubscriptionProvider?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() ?? ""
    }

    var resolvedAISubscriptionPlan: UserQuotaPlan? {
        guard let normalizedPlan = aiSubscriptionPlan?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() else {
            return nil
        }

        return UserQuotaPlan(rawValue: normalizedPlan)
    }

    var aiSubscriptionCurrentPeriodEndDate: Date? {
        guard let epochSeconds = aiSubscriptionCurrentPeriodEndEpochSeconds, epochSeconds > 0 else {
            return nil
        }

        return Date(timeIntervalSince1970: TimeInterval(epochSeconds))
    }

    var aiSubscriptionCheckoutExpiryDate: Date? {
        guard let epochSeconds = aiSubscriptionCheckoutExpiresAtEpochSeconds, epochSeconds > 0 else {
            return nil
        }

        return Date(timeIntervalSince1970: TimeInterval(epochSeconds))
    }

    var hasActiveAISubscription: Bool {
        ["active", "trialing"].contains(normalizedAISubscriptionStatus)
    }

    var hasBlockingAISubscriptionState: Bool {
        ["active", "trialing", "past_due", "unpaid"].contains(normalizedAISubscriptionStatus)
    }

    var hasOpenAISubscriptionCheckout: Bool {
        guard normalizedAISubscriptionStatus == "checkout_pending" else {
            return false
        }

        guard let expiryDate = aiSubscriptionCheckoutExpiryDate else {
            return true
        }

        return expiryDate > .now
    }
}

extension User {
    var registrationDateEpochMillis: Int64 {
        Int64((registrationDate.timeIntervalSince1970 * 1000).rounded())
    }

    var firestorePayload: [String: Any] {
        [
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "username": username.trimmingCharacters(in: .whitespacesAndNewlines),
            "profileImageURL": profileImageURL ?? NSNull(),
            "profileImagePath": NSNull(),
            "whatsApp": whatsApp ?? NSNull(),
            "profileTagline": profileTagline ?? NSNull(),
            "profileBio": profileBio ?? NSNull(),
            "instagramHandle": instagramHandle ?? NSNull(),
            "registrationDateEpochMillis": registrationDateEpochMillis,
            "isAdmin": isAdmin,
            "role": role,
            "quotaPlan": quotaPlan,
            "aiAccessEnabled": aiAccessEnabled,
            "aiTextRequestsPerDay": aiTextRequestsPerDay,
            "aiVisualRequestsPerDay": aiVisualRequestsPerDay,
            "aiAgentRequestsPerDay": aiAgentRequestsPerDay,
            "aiHistoryRetentionDays": aiHistoryRetentionDays,
            "canManageMusicCatalog": canManageMusicCatalog,
            "canManageVideoCatalog": canManageVideoCatalog,
            "canModerateProfiles": canModerateProfiles
        ]
    }

    static func registrationDate(from data: [String: Any], fallback: Date? = nil) -> Date {
        if let epochMillis = (data["registrationDateEpochMillis"] as? NSNumber)?.int64Value {
            return Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1000)
        }

        if let timestamp = data["registrationDate"] as? Timestamp {
            return timestamp.dateValue()
        }

        if let date = data["registrationDate"] as? Date {
            return date
        }

        return fallback ?? .now
    }
}
