package com.skydown.android.data

import android.content.ContentResolver
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await

data class EditableImageAssetUploadResult(
    val downloadUrl: String,
    val storagePath: String,
)

class EditableImageAssetRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun uploadImageAsset(
        uri: Uri,
        contentResolver: ContentResolver,
    ): Result<EditableImageAssetUploadResult> = runCatching {
        val userId = auth.currentUser?.uid ?: error("Bitte zuerst anmelden.")
        val preparedUpload = ImageUploadPreparation.prepare(contentResolver, uri)
        val slot = requestUploadSlot(
            userId = userId,
            kind = "asset",
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

        EditableImageAssetUploadResult(
            downloadUrl = reference.awaitStableDownloadUrl(),
            storagePath = slot.storagePath,
        )
    }

    suspend fun deleteImageAsset(
        imageUrl: String,
    ): Result<Unit> = runCatching {
        val userId = auth.currentUser?.uid ?: return@runCatching
        val trimmedUrl = imageUrl.trim()
        if (trimmedUrl.isEmpty()) {
            return@runCatching
        }

        val reference = runCatching { storage.getReferenceFromUrl(trimmedUrl) }.getOrNull() ?: return@runCatching
        if (!reference.path.startsWith("/users/$userId/assets/")) {
            return@runCatching
        }

        try {
            reference.delete().await()
        } catch (error: Exception) {
            if (error is StorageException && error.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                return@runCatching
            }
            throw error
        }
    }

    private suspend fun requestUploadSlot(
        userId: String,
        kind: String,
        mimeType: String,
        fileExtension: String,
        byteSize: Int,
    ): AssetUploadSlot {
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

        return AssetUploadSlot(
            slotId = data["slotId"] as? String ?: error("Upload-Slot fehlt."),
            storagePath = data["storagePath"] as? String ?: error("Upload-Pfad fehlt."),
        )
    }
}

private data class AssetUploadSlot(
    val slotId: String,
    val storagePath: String,
)
