package com.skydown.android.data

import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.skydown.android.ui.model.ProfileGalleryItem
import com.skydown.android.ui.model.ProfileMediaType
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
        mimeType: String?,
    ): Result<String> = runCatching {
        val resolvedMimeType = mimeType ?: "image/jpeg"
        val slot = requestUploadSlot(
            userId = userId,
            kind = "profile",
            mimeType = resolvedMimeType,
            fileExtension = resolveExtension(ProfileMediaType.Image, resolvedMimeType),
            byteSize = 0,
        )

        val reference = storage.reference.child(slot.storagePath)
        reference.putFile(
            uri,
            StorageMetadata.Builder()
                .setContentType(resolvedMimeType)
                .setCustomMetadata("uploadSlotId", slot.slotId)
                .setCustomMetadata("ownerUid", userId)
                .build(),
        ).await()

        val downloadUrl = reference.downloadUrl.await().toString()
        val now = com.google.firebase.Timestamp.now()
        val userSnapshot = firestore.collection("users").document(userId).get().await()
        val username = (userSnapshot.data?.get("username") as? String)?.takeIf { it.isNotBlank() } ?: "Skydown User"
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
        downloadUrl
    }

    suspend fun uploadGallery(
        userId: String,
        uri: Uri,
        type: ProfileMediaType,
        mimeType: String?,
    ): Result<Unit> = runCatching {
        require(type == ProfileMediaType.Image) {
            "Im Testbetrieb sind aktuell nur Bilder aktiviert."
        }

        val resolvedMimeType = mimeType ?: fallbackMimeType(type)
        val slot = requestUploadSlot(
            userId = userId,
            kind = "gallery",
            mimeType = resolvedMimeType,
            fileExtension = resolveExtension(type, resolvedMimeType),
            byteSize = 0,
        )

        val reference = storage.reference.child(slot.storagePath)
        reference.putFile(
            uri,
            StorageMetadata.Builder()
                .setContentType(resolvedMimeType)
                .setCustomMetadata("uploadSlotId", slot.slotId)
                .setCustomMetadata("ownerUid", userId)
                .build(),
        ).await()

        val downloadUrl = reference.downloadUrl.await().toString()
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
                    "contentType" to resolvedMimeType,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "updatedAt" to com.google.firebase.Timestamp.now(),
                ),
            )
            .await()
    }

    private fun resolveExtension(type: ProfileMediaType, mimeType: String?): String {
        val fromMimeType = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return fromMimeType ?: type.fallbackExtension
    }

    private fun fallbackMimeType(type: ProfileMediaType): String = when (type) {
        ProfileMediaType.Image -> "image/jpeg"
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
}

private data class UploadSlot(
    val slotId: String,
    val storagePath: String,
)

private class NoopListenerRegistration : ListenerRegistration {
    override fun remove() = Unit
}
