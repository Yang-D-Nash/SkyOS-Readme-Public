package com.skydown.android.data

import android.content.ContentResolver
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.skydown.android.ui.model.ProfileGalleryItem
import com.skydown.android.ui.model.ProfileMediaType
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.UserRole
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    fun observeGallery(
        userId: String,
        onResult: (Result<List<ProfileGalleryItem>>) -> Unit,
    ): ListenerRegistration {
        if (auth.currentUser?.uid != userId) {
            onResult(Result.success(emptyList()))
            return NoopListenerRegistration()
        }

        return firestore.collection("galleryMeta")
            .document(userId)
            .collection("items")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        onResult(Result.success(emptyList()))
                        return@addSnapshotListener
                    }
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val items = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    ProfileGalleryItem(
                        id = document.id,
                        ownerId = data["ownerUid"] as? String ?: userId,
                        type = ProfileMediaType.fromRawValue(data["type"] as? String),
                        title = data["title"] as? String ?: "Profil",
                        caption = data["caption"] as? String,
                        mediaUrl = data["mediaURL"] as? String ?: return@mapNotNull null,
                        thumbnailUrl = data["thumbnailURL"] as? String,
                        storagePath = data["storagePath"] as? String,
                        createdAtEpochMillis = (data["createdAt"] as? com.google.firebase.Timestamp)
                            ?.toDate()
                            ?.time
                            ?: System.currentTimeMillis(),
                    )
                }

                onResult(Result.success(items))
            }
    }

    suspend fun uploadAvatar(
        userId: String,
        uri: Uri,
        contentResolver: ContentResolver,
    ): Result<String> = runCatching {
        ensureProfileDocumentsExist(userId)
        val preparedUpload = ImageUploadPreparation.prepare(contentResolver, uri)
        val slot = requestUploadSlot(
            userId = userId,
            kind = "profile",
            mimeType = preparedUpload.mimeType,
            fileExtension = preparedUpload.fileExtension,
            byteSize = preparedUpload.data.size,
        )

        val reference = storage.reference.child(slot.storagePath)
        reference.putBytes(
            preparedUpload.data,
            StorageMetadata.Builder()
                .setContentType(preparedUpload.mimeType)
                .setCustomMetadata("uploadSlotId", slot.slotId)
                .setCustomMetadata("ownerUid", userId)
                .build(),
        ).await()

        val downloadUrl = reference.awaitStableDownloadUrl()
        val now = com.google.firebase.Timestamp.now()
        val userSnapshot = firestore.collection("users").document(userId).get().await()
        val previousAvatarPath = userSnapshot.data?.get("profileImagePath") as? String
        val username = (userSnapshot.data?.get("username") as? String)?.takeIf { it.isNotBlank() } ?: "SkyOs User"
        val profileSnapshot = firestore.collection("userProfiles").document(userId).get().await()
        val createdAt = profileSnapshot.data?.get("createdAt") ?: now
        firestore.collection("users")
            .document(userId)
            .set(
                mapOf(
                    "profileImageURL" to downloadUrl,
                    "profileImagePath" to slot.storagePath,
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )
            .await()
        firestore.collection("userProfiles")
            .document(userId)
            .set(
                mapOf(
                    "ownerUid" to userId,
                    "username" to username,
                    "profileImageURL" to downloadUrl,
                    "profileImagePath" to slot.storagePath,
                    "createdAt" to createdAt,
                    "updatedAt" to now,
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )
            .await()
        if (!previousAvatarPath.isNullOrBlank() && previousAvatarPath != slot.storagePath) {
            deleteStorageObjectIfOwned(previousAvatarPath, userId, "profile")
        }
        downloadUrl
    }

    suspend fun uploadGallery(
        userId: String,
        uri: Uri,
        contentResolver: ContentResolver,
        type: ProfileMediaType,
    ): Result<Unit> = runCatching {
        require(type == ProfileMediaType.Image) {
            "Im Testbetrieb sind aktuell nur Bilder aktiviert."
        }

        ensureProfileDocumentsExist(userId)
        val preparedUpload = ImageUploadPreparation.prepare(contentResolver, uri)
        val slot = requestUploadSlot(
            userId = userId,
            kind = "gallery",
            mimeType = preparedUpload.mimeType,
            fileExtension = preparedUpload.fileExtension,
            byteSize = preparedUpload.data.size,
        )

        val reference = storage.reference.child(slot.storagePath)
        reference.putBytes(
            preparedUpload.data,
            StorageMetadata.Builder()
                .setContentType(preparedUpload.mimeType)
                .setCustomMetadata("uploadSlotId", slot.slotId)
                .setCustomMetadata("ownerUid", userId)
                .build(),
        ).await()

        val downloadUrl = reference.awaitStableDownloadUrl()
        firestore.collection("galleryMeta")
            .document(userId)
            .collection("items")
            .document(slot.slotId)
            .set(
                mapOf(
                    "ownerUid" to userId,
                    "type" to type.rawValue,
                    "title" to defaultTitle(type),
                    "caption" to null,
                    "mediaURL" to downloadUrl,
                    "thumbnailURL" to downloadUrl,
                    "storagePath" to slot.storagePath,
                    "contentType" to preparedUpload.mimeType,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "updatedAt" to com.google.firebase.Timestamp.now(),
                ),
            )
            .await()
    }

    suspend fun deleteAvatar(
        userId: String,
    ): Result<Unit> = runCatching {
        ensureProfileDocumentsExist(userId)
        val userReference = firestore.collection("users").document(userId)
        val profileReference = firestore.collection("userProfiles").document(userId)
        val userSnapshot = userReference.get().await()
        val profileSnapshot = profileReference.get().await()
        val avatarPath = (userSnapshot.data?.get("profileImagePath") as? String)
            ?: (profileSnapshot.data?.get("profileImagePath") as? String)

        if (!avatarPath.isNullOrBlank()) {
            deleteStorageObjectIfOwned(avatarPath, userId, "profile")
        }

        userReference.update(
            mapOf(
                "profileImageURL" to com.google.firebase.firestore.FieldValue.delete(),
                "profileImagePath" to com.google.firebase.firestore.FieldValue.delete(),
            ),
        ).await()

        profileReference.update(
            mapOf(
                "profileImageURL" to com.google.firebase.firestore.FieldValue.delete(),
                "profileImagePath" to com.google.firebase.firestore.FieldValue.delete(),
                "updatedAt" to com.google.firebase.Timestamp.now(),
            ),
        ).await()
    }

    suspend fun deleteGalleryItem(
        userId: String,
        item: ProfileGalleryItem,
    ): Result<Unit> = runCatching {
        ensureProfileDocumentsExist(userId)
        if (!item.storagePath.isNullOrBlank()) {
            deleteStorageObjectIfOwned(item.storagePath, userId, "gallery")
        }

        firestore.collection("galleryMeta")
            .document(userId)
            .collection("items")
            .document(item.id)
            .delete()
            .await()
    }

    private suspend fun ensureProfileDocumentsExist(userId: String) {
        val authUser = auth.currentUser ?: error("Bitte zuerst anmelden.")
        require(authUser.uid == userId) {
            "Bitte zuerst mit dem richtigen Konto anmelden."
        }

        val userReference = firestore.collection("users").document(userId)
        val profileReference = firestore.collection("userProfiles").document(userId)
        val userSnapshot = userReference.get().await()
        val profileSnapshot = profileReference.get().await()
        val userData = userSnapshot.data.orEmpty()
        val email = authUser.email.orEmpty().trim().lowercase()
        val resolvedRole = UserRole.resolve(
            rawValue = userData["role"] as? String,
            isAdmin = userData["isAdmin"] as? Boolean ?: false,
            email = email,
        )
        val resolvedQuotaPlan = UserQuotaPlan.resolve(
            rawValue = userData["quotaPlan"] as? String,
            role = resolvedRole,
        )
        val username = (userData["username"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: (profileSnapshot.data?.get("username") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: authUser.displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: email.substringBefore("@").trim().takeIf { it.isNotEmpty() }
            ?: "SkyOs User"
        val registrationDateEpochMillis = (userData["registrationDateEpochMillis"] as? Number)?.toLong()
            ?: authUser.metadata?.creationTimestamp
            ?: System.currentTimeMillis()

        if (!userSnapshot.exists()) {
            userReference.set(
                mapOf(
                    "email" to email,
                    "username" to username,
                    "profileImageURL" to (userData["profileImageURL"] as? String),
                    "profileImagePath" to (userData["profileImagePath"] as? String),
                    "whatsApp" to (userData["whatsApp"] as? String),
                    "profileTagline" to (userData["profileTagline"] as? String),
                    "profileBio" to (userData["profileBio"] as? String),
                    "instagramHandle" to (userData["instagramHandle"] as? String),
                    "registrationDateEpochMillis" to registrationDateEpochMillis,
                    "isAdmin" to resolvedRole.hasStaffAccess,
                    "role" to resolvedRole.rawValue,
                    "quotaPlan" to resolvedQuotaPlan.rawValue,
                    "aiAccessEnabled" to true,
                    "termsAcceptedAt" to com.google.firebase.Timestamp.now(),
                    "privacyAcceptedAt" to com.google.firebase.Timestamp.now(),
                    "termsVersion" to "legacy",
                    "privacyVersion" to "legacy",
                    "legalConsentSource" to "android_profile_bootstrap",
                    "aiConsentGiven" to true,
                    "aiConsentUpdatedAt" to com.google.firebase.Timestamp.now(),
                    "aiConsentSource" to "android_profile_bootstrap",
                    "aiTextRequestsPerDay" to resolvedQuotaPlan.aiTextRequestsPerDay,
                    "aiVisualRequestsPerDay" to resolvedQuotaPlan.aiVisualRequestsPerDay,
                    "aiAgentRequestsPerDay" to resolvedQuotaPlan.aiAgentRequestsPerDay,
                    "aiHistoryRetentionDays" to resolvedQuotaPlan.aiHistoryRetentionDays,
                    "canManageMusicCatalog" to (resolvedRole == UserRole.Owner),
                    "canManageVideoCatalog" to (resolvedRole == UserRole.Owner),
                    "canModerateProfiles" to (resolvedRole == UserRole.Owner),
                ),
            ).await()
        }

        if (!profileSnapshot.exists()) {
            val now = com.google.firebase.Timestamp.now()
            profileReference.set(
                mapOf(
                    "ownerUid" to userId,
                    "username" to username,
                    "profileImageURL" to (userData["profileImageURL"] as? String),
                    "profileImagePath" to (userData["profileImagePath"] as? String),
                    "profileTagline" to (userData["profileTagline"] as? String),
                    "profileBio" to (userData["profileBio"] as? String),
                    "instagramHandle" to (userData["instagramHandle"] as? String),
                    "whatsApp" to (userData["whatsApp"] as? String),
                    "createdAt" to now,
                    "updatedAt" to now,
                ),
            ).await()
        }
    }

    private fun defaultTitle(type: ProfileMediaType): String {
        val dateLabel = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())
        return when (type) {
            ProfileMediaType.Image -> "Bild $dateLabel"
        }
    }

    private suspend fun requestUploadSlot(
        userId: String,
        kind: String,
        mimeType: String,
        fileExtension: String,
        byteSize: Int,
    ): UploadSlot {
        val response = functions
            .getHttpsCallable("requestUploadSlot")
            .call(
                mapOf(
                    "userId" to userId,
                    "kind" to kind,
                    "mimeType" to mimeType,
                    "fileExtension" to fileExtension,
                    "byteSize" to byteSize,
                ),
            )
            .await()

        val data = response.data as? Map<*, *> ?: error("Upload-Freigabe konnte nicht gelesen werden.")
        val allowed = data["allowed"] as? Boolean ?: false
        if (!allowed) {
            error((data["message"] as? String)?.takeIf { it.isNotBlank() } ?: "Upload wurde abgelehnt.")
        }

        return UploadSlot(
            slotId = data["slotId"] as? String ?: error("Upload-Slot fehlt."),
            storagePath = data["storagePath"] as? String ?: error("Upload-Pfad fehlt."),
        )
    }

    private suspend fun deleteStorageObjectIfOwned(
        path: String,
        userId: String,
        expectedFolder: String,
    ) {
        val normalizedPath = path.trim()
        if (!normalizedPath.startsWith("users/$userId/$expectedFolder/")) {
            return
        }

        try {
            storage.reference.child(normalizedPath).delete().await()
        } catch (error: Exception) {
            if (error is StorageException && error.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                return
            }
            throw error
        }
    }
}

private data class UploadSlot(
    val slotId: String,
    val storagePath: String,
)

private class NoopListenerRegistration : ListenerRegistration {
    override fun remove() = Unit
}
