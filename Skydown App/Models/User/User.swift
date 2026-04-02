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
        case .owner, .admin:
            return true
        case .subadmin, .user:
            return false
        }
    }

    var defaultAITextRequestsPerDay: Int {
        switch self {
        case .owner:
            return 400
        case .admin:
            return 240
        case .subadmin:
            return 120
        case .user:
            return 30
        }
    }

    var defaultAIVisualRequestsPerDay: Int {
        switch self {
        case .owner:
            return 80
        case .admin:
            return 40
        case .subadmin:
            return 20
        case .user:
            return 4
        }
    }

    var defaultAIAgentRequestsPerDay: Int {
        switch self {
        case .owner:
            return 250
        case .admin:
            return 140
        case .subadmin:
            return 70
        case .user:
            return 18
        }
    }

    var defaultAIHistoryRetentionDays: Int {
        switch self {
        case .owner, .admin:
            return 30
        case .subadmin:
            return 7
        case .user:
            return 3
        }
    }

    private static func normalizedEmail(_ email: String?) -> String? {
        let trimmed = email?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
        return trimmed.isEmpty ? nil : trimmed
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
    var aiAccessEnabled: Bool = true
    var aiTextRequestsPerDay: Int = UserRole.user.defaultAITextRequestsPerDay
    var aiVisualRequestsPerDay: Int = UserRole.user.defaultAIVisualRequestsPerDay
    var aiAgentRequestsPerDay: Int = UserRole.user.defaultAIAgentRequestsPerDay
    var aiHistoryRetentionDays: Int = UserRole.user.defaultAIHistoryRetentionDays

    var resolvedRole: UserRole {
        UserRole.resolve(from: role, isAdmin: isAdmin, email: email)
    }

    var hasStaffAccess: Bool {
        resolvedRole.hasStaffAccess
    }

    var hasAdminWorkspaceAccess: Bool {
        resolvedRole.hasAdminWorkspaceAccess
    }

    var resolvedAITextRequestsPerDay: Int {
        max(aiTextRequestsPerDay, resolvedRole.defaultAITextRequestsPerDay)
    }

    var resolvedAIVisualRequestsPerDay: Int {
        max(aiVisualRequestsPerDay, resolvedRole.defaultAIVisualRequestsPerDay)
    }

    var resolvedAIAgentRequestsPerDay: Int {
        max(aiAgentRequestsPerDay, resolvedRole.defaultAIAgentRequestsPerDay)
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
}
