package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.skydown.android.data.repository.toSharedUser
import com.skydown.shared.model.User
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.canManageMusic
import com.skydown.shared.model.canManageVideos
import com.skydown.shared.model.canModerateUserProfiles
import com.skydown.shared.model.resolvedAiHistoryRetentionDays
import com.skydown.shared.model.resolvedQuotaPlan
import com.skydown.shared.model.resolvedRole
import kotlinx.coroutines.tasks.await

class AdminUserManagementRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "users"

    fun observeUsers(onChange: (Result<List<User>>) -> Unit): ListenerRegistration {
        return firestore.collection(collectionName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onChange(Result.failure(error))
                return@addSnapshotListener
            }

            val users = snapshot?.documents
                .orEmpty()
                .mapNotNull { document -> document.toSharedUser() }
                .sortedWith(::compareUsers)

            onChange(Result.success(users))
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return runCatching {
            val userId = user.id?.takeIf { it.isNotBlank() }
                ?: error("Dieses Konto hat keine gueltige Benutzer-ID.")
            val resolvedRole = user.resolvedRole
            val resolvedQuotaPlan = user.resolvedQuotaPlan

            firestore.collection(collectionName).document(userId).set(
                mapOf(
                    "email" to user.email.trim().lowercase(),
                    "role" to resolvedRole.rawValue,
                    "isAdmin" to resolvedRole.hasStaffAccess,
                    "quotaPlan" to resolvedQuotaPlan.rawValue,
                    "aiAccessEnabled" to user.aiAccessEnabled,
                    "aiTextRequestsPerDay" to user.aiTextRequestsPerDay.coerceAtLeast(1),
                    "aiVisualRequestsPerDay" to user.aiVisualRequestsPerDay.coerceAtLeast(1),
                    "aiAgentRequestsPerDay" to user.aiAgentRequestsPerDay.coerceAtLeast(1),
                    "aiHistoryRetentionDays" to user.resolvedAiHistoryRetentionDays,
                    "canManageMusicCatalog" to user.canManageMusic,
                    "canManageVideoCatalog" to user.canManageVideos,
                    "canModerateProfiles" to user.canModerateUserProfiles,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }
}

private fun compareUsers(left: User, right: User): Int {
    val roleDifference = roleSortRank(left.resolvedRole) - roleSortRank(right.resolvedRole)
    if (roleDifference != 0) {
        return roleDifference
    }

    val leftName = left.username.trim().lowercase()
    val rightName = right.username.trim().lowercase()
    if (leftName != rightName) {
        return leftName.compareTo(rightName)
    }

    return left.email.lowercase().compareTo(right.email.lowercase())
}

private fun roleSortRank(role: UserRole): Int {
    return when (role) {
        UserRole.Owner -> 0
        UserRole.Admin -> 1
        UserRole.Subadmin -> 2
        UserRole.User -> 3
    }
}
