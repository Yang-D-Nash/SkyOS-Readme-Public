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
        get() = this == Owner || this == Admin

    val defaultAiTextRequestsPerDay: Int
        get() = when (this) {
            Owner -> 400
            Admin -> 240
            Subadmin -> 120
            User -> 30
        }

    val defaultAiVisualRequestsPerDay: Int
        get() = when (this) {
            Owner -> 80
            Admin -> 40
            Subadmin -> 20
            User -> 4
        }

    val defaultAiAgentRequestsPerDay: Int
        get() = when (this) {
            Owner -> 250
            Admin -> 140
            Subadmin -> 70
            User -> 18
        }

    val defaultAiHistoryRetentionDays: Int
        get() = when (this) {
            Owner, Admin -> 30
            Subadmin -> 7
            User -> 3
        }

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
data class User(
    val id: String? = null,
    val email: String,
    val username: String,
    val whatsApp: String? = null,
    val profileTagline: String? = null,
    val profileBio: String? = null,
    val instagramHandle: String? = null,
    val registrationDateEpochMillis: Long,
    val isAdmin: Boolean = false,
    val role: String = UserRole.User.rawValue,
    val aiAccessEnabled: Boolean = true,
    val aiTextRequestsPerDay: Int = 30,
    val aiVisualRequestsPerDay: Int = 4,
    val aiAgentRequestsPerDay: Int = 18,
    val aiHistoryRetentionDays: Int = 3,
)

val User.resolvedRole: UserRole
    get() = UserRole.resolve(role, isAdmin, email)

val User.hasStaffAccess: Boolean
    get() = resolvedRole.hasStaffAccess

val User.hasAdminWorkspaceAccess: Boolean
    get() = resolvedRole.hasAdminWorkspaceAccess

val User.resolvedAiTextRequestsPerDay: Int
    get() = maxOf(aiTextRequestsPerDay, resolvedRole.defaultAiTextRequestsPerDay)

val User.resolvedAiVisualRequestsPerDay: Int
    get() = maxOf(aiVisualRequestsPerDay, resolvedRole.defaultAiVisualRequestsPerDay)

val User.resolvedAiAgentRequestsPerDay: Int
    get() = maxOf(aiAgentRequestsPerDay, resolvedRole.defaultAiAgentRequestsPerDay)

val User.resolvedAiHistoryRetentionDays: Int
    get() = when (aiHistoryRetentionDays) {
        1, 3, 7, 30 -> aiHistoryRetentionDays
        else -> resolvedRole.defaultAiHistoryRetentionDays
    }

val User.isPlatformOwner: Boolean
    get() = resolvedRole == UserRole.Owner

fun sampleUser(): User = User(
    id = "demo-user",
    email = "demo@skydown.app",
    username = "Yang D. Nash",
    whatsApp = "+49 170 0000000",
    registrationDateEpochMillis = 1_725_000_000_000,
    isAdmin = true,
    role = UserRole.Owner.rawValue,
    aiTextRequestsPerDay = UserRole.Owner.defaultAiTextRequestsPerDay,
    aiVisualRequestsPerDay = UserRole.Owner.defaultAiVisualRequestsPerDay,
    aiAgentRequestsPerDay = UserRole.Owner.defaultAiAgentRequestsPerDay,
    aiHistoryRetentionDays = UserRole.Owner.defaultAiHistoryRetentionDays,
)
