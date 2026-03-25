package com.skydown.android.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.skydown.shared.model.User

internal fun FirebaseUser.toSharedUser(
    isAdmin: Boolean = false,
): User {
    val fallbackEmail = email.orEmpty()
    return User(
        id = uid,
        email = fallbackEmail,
        username = displayName
            ?.takeIf { it.isNotBlank() }
            ?: fallbackEmail.substringBefore("@").ifBlank { "Skydown User" },
        whatsApp = null,
        registrationDateEpochMillis = metadata?.creationTimestamp ?: System.currentTimeMillis(),
        isAdmin = isAdmin,
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

    return User(
        id = id,
        email = email,
        username = username,
        whatsApp = data["whatsApp"] as? String,
        registrationDateEpochMillis = (data["registrationDateEpochMillis"] as? Number)?.toLong()
            ?: (data["registrationDate"] as? com.google.firebase.Timestamp)?.toDate()?.time
            ?: authUser?.metadata?.creationTimestamp
            ?: System.currentTimeMillis(),
        isAdmin = data["isAdmin"] as? Boolean ?: false,
    )
}
