package com.skydown.android.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.skydown.shared.model.User
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.UserRole

internal fun FirebaseUser.toSharedUser(
    isAdmin: Boolean = false,
): User {
    val fallbackEmail = email.orEmpty().lowercase()
    val resolvedRole = UserRole.resolve(rawValue = null, isAdmin = isAdmin, email = fallbackEmail)
    val quotaPlan = UserQuotaPlan.defaultPlanFor(resolvedRole)
    return User(
        id = uid,
        email = fallbackEmail,
        username = (displayName
            ?.takeIf { it.isNotBlank() }
            ?: fallbackEmail.substringBefore("@").ifBlank { "Skydown User" }
        ).trim().take(32).trim().ifBlank { "Skydown User" },
        profileImageURL = null,
        whatsApp = null,
        profileTagline = null,
        profileBio = null,
        instagramHandle = null,
        registrationDateEpochMillis = metadata?.creationTimestamp ?: System.currentTimeMillis(),
        isAdmin = isAdmin,
        role = resolvedRole.rawValue,
        quotaPlan = quotaPlan.rawValue,
        aiAccessEnabled = true,
        aiTextRequestsPerDay = quotaPlan.aiTextRequestsPerDay,
        aiVisualRequestsPerDay = quotaPlan.aiVisualRequestsPerDay,
        aiAgentRequestsPerDay = quotaPlan.aiAgentRequestsPerDay,
        aiHistoryRetentionDays = quotaPlan.aiHistoryRetentionDays,
        canManageMusicCatalog = resolvedRole == UserRole.Owner,
        canManageVideoCatalog = resolvedRole == UserRole.Owner,
        canModerateProfiles = resolvedRole == UserRole.Owner,
    )
}

internal fun DocumentSnapshot.toSharedUser(authUser: FirebaseUser? = null): User? {
    val data = data
    if (data == null) {
        return authUser?.toSharedUser()
    }

    val fallbackEmail = authUser?.email.orEmpty()
    val email = (data["email"] as? String)?.takeIf { it.isNotBlank() }
        ?: fallbackEmail.takeIf { it.isNotBlank() }
        ?: return null
    val username = (data["username"] as? String)?.takeIf { it.isNotBlank() }
        ?: authUser?.displayName?.takeIf { it.isNotBlank() }
        ?: email.substringBefore("@").ifBlank { "Skydown User" }
    val storedIsAdmin = data["isAdmin"] as? Boolean ?: false
    val resolvedRole = UserRole.resolve(
        rawValue = data["role"] as? String,
        isAdmin = storedIsAdmin,
        email = email,
    )
    val resolvedQuotaPlan = UserQuotaPlan.resolve(
        data["quotaPlan"] as? String,
        resolvedRole,
    )

    return User(
        id = id,
        email = email,
        username = username,
        profileImageURL = data["profileImageURL"] as? String,
        whatsApp = data["whatsApp"] as? String,
        profileTagline = data["profileTagline"] as? String,
        profileBio = data["profileBio"] as? String,
        instagramHandle = data["instagramHandle"] as? String,
        registrationDateEpochMillis = (data["registrationDateEpochMillis"] as? Number)?.toLong()
            ?: (data["registrationDate"] as? com.google.firebase.Timestamp)?.toDate()?.time
            ?: authUser?.metadata?.creationTimestamp
            ?: System.currentTimeMillis(),
        isAdmin = resolvedRole.hasStaffAccess,
        role = resolvedRole.rawValue,
        quotaPlan = resolvedQuotaPlan.rawValue,
        aiAccessEnabled = data["aiAccessEnabled"] as? Boolean ?: true,
        aiTextRequestsPerDay = (data["aiTextRequestsPerDay"] as? Number)?.toInt()
            ?: resolvedQuotaPlan.aiTextRequestsPerDay,
        aiVisualRequestsPerDay = (data["aiVisualRequestsPerDay"] as? Number)?.toInt()
            ?: resolvedQuotaPlan.aiVisualRequestsPerDay,
        aiAgentRequestsPerDay = (data["aiAgentRequestsPerDay"] as? Number)?.toInt()
            ?: resolvedQuotaPlan.aiAgentRequestsPerDay,
        aiHistoryRetentionDays = (data["aiHistoryRetentionDays"] as? Number)?.toInt()
            ?: resolvedQuotaPlan.aiHistoryRetentionDays,
        canManageMusicCatalog = data["canManageMusicCatalog"] as? Boolean ?: (resolvedRole == UserRole.Owner),
        canManageVideoCatalog = data["canManageVideoCatalog"] as? Boolean ?: (resolvedRole == UserRole.Owner),
        canModerateProfiles = data["canModerateProfiles"] as? Boolean ?: (resolvedRole == UserRole.Owner),
    )
}
