package com.skydown.android.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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

    suspend fun uploadVideoAsset(
        uri: Uri,
        context: Context,
    ): Result<EditableImageAssetUploadResult> = runCatching {
        val userId = auth.currentUser?.uid ?: error("Bitte zuerst anmelden.")
        val selectedFile = context.contentResolver.resolveSelectedVideoAsset(uri)
        val slot = requestUploadSlot(
            userId = userId,
            kind = "asset",
            mimeType = selectedFile.mimeType,
            fileExtension = selectedFile.fileName.substringAfterLast('.', "mp4"),
            byteSize = selectedFile.fileSizeBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        )

        val reference = storage.reference.child(slot.storagePath)
        val metadata = StorageMetadata.Builder()
            .setContentType(selectedFile.mimeType)
            .setCustomMetadata("uploadSlotId", slot.slotId)
            .setCustomMetadata("ownerUid", userId)
            .setCustomMetadata("originalFilename", selectedFile.fileName)
            .build()

        val stagedFile = context.stagePickerFileForUpload(
            sourceUri = uri,
            fileName = selectedFile.fileName,
        )

        try {
            reference.putStagedFile(
                stagedFile = stagedFile,
                metadata = metadata,
            )
        } catch (error: Exception) {
            throw error.toReadableStorageUploadError(selectedFile.fileName)
        } finally {
            stagedFile.delete()
        }

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

    suspend fun deleteAsset(assetUrl: String): Result<Unit> = deleteImageAsset(assetUrl)

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

private data class SelectedVideoAsset(
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
)

private fun ContentResolver.resolveSelectedVideoAsset(uri: Uri): SelectedVideoAsset {
    var fileName = "artist-hero-video.mp4"
    var fileSizeBytes = 0L

    query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex >= 0) {
                fileName = cursor.getString(nameIndex) ?: fileName
            }
            if (sizeIndex >= 0) {
                fileSizeBytes = cursor.getLong(sizeIndex)
            }
        }
    }

    val mimeType = getType(uri).orEmpty().ifBlank {
        when {
            fileName.lowercase().endsWith(".mov") -> "video/quicktime"
            fileName.lowercase().endsWith(".m4v") -> "video/x-m4v"
            else -> "video/mp4"
        }
    }

    if (!mimeType.startsWith("video/")) {
        error("Bitte waehle eine gueltige Videodatei aus.")
    }

    return SelectedVideoAsset(
        fileName = fileName,
        fileSizeBytes = fileSizeBytes,
        mimeType = mimeType,
    )
}
