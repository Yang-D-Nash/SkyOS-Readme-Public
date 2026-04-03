package com.skydown.shared.model

import kotlinx.serialization.Serializable

enum class UserRole(val rawValue: String) {
    Owner("owner"),
    Admin("admin"),
    Subadmin("subadmin"),
    User("user");

    val hasStaffAccess: Boolean
        get() = this == Owner || this == Admin

    val hasAdminWorkspaceAccess: Boolean
        get() = this == Owner

    val defaultAiTextRequestsPerDay: Int
        get() = UserQuotaPlan.defaultPlanFor(this).aiTextRequestsPerDay

    val defaultAiVisualRequestsPerDay: Int
        get() = UserQuotaPlan.defaultPlanFor(this).aiVisualRequestsPerDay

    val defaultAiAgentRequestsPerDay: Int
        get() = UserQuotaPlan.defaultPlanFor(this).aiAgentRequestsPerDay

    val defaultAiHistoryRetentionDays: Int
        get() = UserQuotaPlan.defaultPlanFor(this).aiHistoryRetentionDays

    companion object {
        const val OWNER_EMAIL = "nash.lioncorna@gmail.com"

        fun resolve(rawValue: String?, isAdmin: Boolean, email: String? = null): UserRole {
            if (email?.trim()?.lowercase() == OWNER_EMAIL) {
                return Owner
            }

            val normalized = rawValue?.trim()?.lowercase()
            return entries.firstOrNull { it.rawValue == normalized } ?: if (isAdmin) Admin else User
        }
    }
}

@Serializable
enum class UserQuotaPlan(val rawValue: String) {
    OwnerUnlimited("owner_unlimited"),
    InternalTeam("internal_team"),
    Free("free"),
    Creator("creator"),
    Studio("studio");

    val aiTextRequestsPerDay: Int
        get() = when (this) {
            OwnerUnlimited -> 5000
            InternalTeam -> 240
            Free -> 30
            Creator -> 120
            Studio -> 240
        }

    val aiVisualRequestsPerDay: Int
        get() = when (this) {
            OwnerUnlimited -> 1200
            InternalTeam -> 40
            Free -> 4
            Creator -> 20
            Studio -> 40
        }

    val aiAgentRequestsPerDay: Int
        get() = when (this) {
            OwnerUnlimited -> 3000
            InternalTeam -> 140
            Free -> 18
            Creator -> 70
            Studio -> 140
        }

    val aiHistoryRetentionDays: Int
        get() = when (this) {
            OwnerUnlimited, InternalTeam, Studio -> 30
            Creator -> 7
            Free -> 3
        }

    companion object {
        fun defaultPlanFor(role: UserRole): UserQuotaPlan = when (role) {
            UserRole.Owner -> OwnerUnlimited
            UserRole.Admin -> InternalTeam
            UserRole.Subadmin -> Creator
            UserRole.User -> Free
        }

        fun resolve(rawValue: String?, role: UserRole): UserQuotaPlan {
            val normalized = rawValue?.trim()?.lowercase()
            return entries.firstOrNull { it.rawValue == normalized } ?: defaultPlanFor(role)
        }
    }
}

@Serializable
data class User(
    val id: String? = null,
    val email: String,
    val username: String,
    val profileImageURL: String? = null,
    val whatsApp: String? = null,
    val profileTagline: String? = null,
    val profileBio: String? = null,
    val instagramHandle: String? = null,
    val registrationDateEpochMillis: Long,
    val isAdmin: Boolean = false,
    val role: String = UserRole.User.rawValue,
    val quotaPlan: String = UserQuotaPlan.Free.rawValue,
    val aiAccessEnabled: Boolean = true,
    val aiTextRequestsPerDay: Int = 30,
    val aiVisualRequestsPerDay: Int = 4,
    val aiAgentRequestsPerDay: Int = 18,
    val aiHistoryRetentionDays: Int = 3,
    val canManageMusicCatalog: Boolean = false,
    val canManageVideoCatalog: Boolean = false,
    val canModerateProfiles: Boolean = false,
)

val User.resolvedRole: UserRole
    get() = UserRole.resolve(role, isAdmin, email)

val User.hasStaffAccess: Boolean
    get() = resolvedRole.hasStaffAccess

val User.hasAdminWorkspaceAccess: Boolean
    get() = resolvedRole.hasAdminWorkspaceAccess

val User.resolvedQuotaPlan: UserQuotaPlan
    get() = UserQuotaPlan.resolve(quotaPlan, resolvedRole)

val User.canManageMusic: Boolean
    get() = isPlatformOwner || (resolvedRole == UserRole.Admin && canManageMusicCatalog)

val User.canManageVideos: Boolean
    get() = isPlatformOwner || (resolvedRole == UserRole.Admin && canManageVideoCatalog)

val User.canModerateUserProfiles: Boolean
    get() = isPlatformOwner || (resolvedRole == UserRole.Admin && canModerateProfiles)

val User.resolvedAiTextRequestsPerDay: Int
    get() = maxOf(aiTextRequestsPerDay, resolvedQuotaPlan.aiTextRequestsPerDay)

val User.resolvedAiVisualRequestsPerDay: Int
    get() = maxOf(aiVisualRequestsPerDay, resolvedQuotaPlan.aiVisualRequestsPerDay)

val User.resolvedAiAgentRequestsPerDay: Int
    get() = maxOf(aiAgentRequestsPerDay, resolvedQuotaPlan.aiAgentRequestsPerDay)

val User.resolvedAiHistoryRetentionDays: Int
    get() = when (aiHistoryRetentionDays) {
        1, 3, 7, 30 -> aiHistoryRetentionDays
        else -> resolvedRole.defaultAiHistoryRetentionDays
    }

val User.isPlatformOwner: Boolean
    get() = resolvedRole == UserRole.Owner
