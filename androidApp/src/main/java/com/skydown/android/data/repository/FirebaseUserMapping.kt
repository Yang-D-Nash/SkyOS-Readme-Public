package com.skydown.android.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.skydown.shared.model.User
import com.skydown.shared.model.UserRole

internal fun FirebaseUser.toSharedUser(
    isAdmin: Boolean = false,
): User {
    val fallbackEmail = email.orEmpty().lowercase()
    val resolvedRole = UserRole.resolve(rawValue = null, isAdmin = isAdmin, email = fallbackEmail)
    return User(
        id = uid,
        email = fallbackEmail,
        username = displayName
            ?.takeIf { it.isNotBlank() }
            ?: fallbackEmail.substringBefore("@").ifBlank { "Skydown User" },
        whatsApp = null,
        registrationDateEpochMillis = metadata?.creationTimestamp ?: System.currentTimeMillis(),
        isAdmin = isAdmin,
        role = resolvedRole.rawValue,
        aiAccessEnabled = true,
        aiTextRequestsPerDay = resolvedRole.defaultAiTextRequestsPerDay,
        aiVisualRequestsPerDay = resolvedRole.defaultAiVisualRequestsPerDay,
        aiAgentRequestsPerDay = resolvedRole.defaultAiAgentRequestsPerDay,
        aiHistoryRetentionDays = resolvedRole.defaultAiHistoryRetentionDays,
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

    return User(
        id = id,
        email = email,
        username = username,
        whatsApp = data["whatsApp"] as? String,
        registrationDateEpochMillis = (data["registrationDateEpochMillis"] as? Number)?.toLong()
            ?: (data["registrationDate"] as? com.google.firebase.Timestamp)?.toDate()?.time
            ?: authUser?.metadata?.creationTimestamp
            ?: System.currentTimeMillis(),
        isAdmin = resolvedRole.hasStaffAccess,
        role = resolvedRole.rawValue,
        aiAccessEnabled = data["aiAccessEnabled"] as? Boolean ?: true,
        aiTextRequestsPerDay = (data["aiTextRequestsPerDay"] as? Number)?.toInt()
            ?: resolvedRole.defaultAiTextRequestsPerDay,
        aiVisualRequestsPerDay = (data["aiVisualRequestsPerDay"] as? Number)?.toInt()
            ?: resolvedRole.defaultAiVisualRequestsPerDay,
        aiAgentRequestsPerDay = (data["aiAgentRequestsPerDay"] as? Number)?.toInt()
            ?: resolvedRole.defaultAiAgentRequestsPerDay,
        aiHistoryRetentionDays = (data["aiHistoryRetentionDays"] as? Number)?.toInt()
            ?: resolvedRole.defaultAiHistoryRetentionDays,
    )
}
