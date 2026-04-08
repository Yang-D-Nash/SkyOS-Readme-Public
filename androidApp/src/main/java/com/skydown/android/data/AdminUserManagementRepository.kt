package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
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
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
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
            val existingSnapshot = firestore.collection(collectionName).document(userId).get().await()
            val existingRole = existingSnapshot.getString("role")
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
            val shouldSyncRoleClaims = existingRole == null || existingRole != resolvedRole.rawValue

            if (shouldSyncRoleClaims) {
                try {
                    functions
                        .getHttpsCallable("setUserRole")
                        .call(
                            mapOf(
                                "uid" to userId,
                                "role" to resolvedRole.rawValue,
                            ),
                        )
                        .await()
                } catch (error: FirebaseFunctionsException) {
                    throw error.toReadableManagedUserError()
                }
            }

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

private fun FirebaseFunctionsException.toReadableManagedUserError(): Throwable {
    val message = when (code) {
        FirebaseFunctionsException.Code.NOT_FOUND ->
            "Konto nicht gefunden. Dieses Profil hat keinen aktiven Login mehr. Bitte neu registrieren oder das alte Profil entfernen."
        FirebaseFunctionsException.Code.PERMISSION_DENIED ->
            "Rollen duerfen nur vom festen Owner-Konto geaendert werden. Bitte als Owner erneut anmelden."
        FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
            "Sicherheitscheck fehlgeschlagen. Bitte die App neu oeffnen und die Aktion auf einem echten Geraet erneut probieren. Bei Debug-Builds muss das App-Check-Debug-Token in Firebase hinterlegt sein."
        FirebaseFunctionsException.Code.UNAUTHENTICATED ->
            "Bitte neu anmelden und das Konto danach erneut speichern."
        else -> localizedMessage
    }

    return IllegalStateException(
        message?.takeIf { it.isNotBlank() } ?: "Konto konnte nicht gespeichert werden.",
        this,
    )
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
