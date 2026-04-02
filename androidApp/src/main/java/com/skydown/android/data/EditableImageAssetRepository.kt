package com.skydown.android.data

import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

class EditableImageAssetRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun uploadImageAsset(
        uri: Uri,
        mimeType: String?,
    ): Result<String> = runCatching {
        val userId = auth.currentUser?.uid ?: error("Bitte zuerst anmelden.")
        val resolvedMimeType = normalizeMimeType(mimeType)
        val slot = requestUploadSlot(
            userId = userId,
            kind = "asset",
            mimeType = resolvedMimeType,
            fileExtension = resolveExtension(resolvedMimeType),
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

        reference.downloadUrl.await().toString()
    }

    private fun normalizeMimeType(mimeType: String?): String {
        return when (mimeType?.lowercase()) {
            "image/png" -> "image/png"
            "image/webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private fun resolveExtension(mimeType: String): String {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
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
