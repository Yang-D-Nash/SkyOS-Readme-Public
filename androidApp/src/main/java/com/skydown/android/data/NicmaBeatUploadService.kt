package com.skydown.android.data

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.skydown.android.ui.model.NicmaBeatHubItem
import com.skydown.android.ui.model.NicmaSelectedBeatFile
import com.skydown.shared.model.User
import com.skydown.shared.model.canManageMusic
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class NicmaBeatUploadRequest(
    val beatTitle: String,
    val artistName: String,
    val email: String,
    val notes: String,
    val files: List<NicmaSelectedBeatFile>,
)

data class ExternalBeatUploadRequest(
    val beatTitle: String,
    val artistName: String,
    val email: String,
    val notes: String,
    val externalUrl: String,
)

class NicmaBeatUploadService(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "nicmaBeatHub"

    fun observeBeats(
        isAdmin: Boolean,
        onChange: (Result<List<NicmaBeatHubItem>>) -> Unit,
    ): () -> Unit {
        val listener = beatQuery(isAdmin)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onChange(Result.failure(error))
                    return@addSnapshotListener
                }

                val beats = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        mapBeat(document as? QueryDocumentSnapshot ?: return@mapNotNull null)
                    }
                    .sortedByDescending { beat -> beat.createdAtMillis }
                onChange(Result.success(beats))
            }

        return {
            listener.remove()
        }
    }

    suspend fun uploadBeats(
        context: Context,
        request: NicmaBeatUploadRequest,
        currentUser: User?,
    ) {
        request.files.forEach { file ->
            uploadSingleFile(
                context = context,
                request = request,
                file = file,
                currentUser = currentUser,
            )
        }
    }

    suspend fun addExternalBeat(
        request: ExternalBeatUploadRequest,
        currentUser: User?,
    ) {
        val source = resolveExternalAudioSource(request.externalUrl)
            ?: error("Externer Beat-Link konnte nicht erkannt werden.")
        val trimmedTitle = request.beatTitle.trim()
        val beatTitle = trimmedTitle.ifBlank {
            "${request.artistName.trim().ifBlank { "Beat" }} Beat"
        }
        val fallbackFileName = source.sourceFileId?.takeIf { it.isNotBlank() }?.let { "source-$it" }
            ?: "${source.provider.rawValue}-beat"
        val fileName = source.normalizedUrl.substringAfterLast('/').substringBefore('?')
            .takeIf { candidate ->
                candidate.isNotBlank() &&
                    !candidate.equals("view", ignoreCase = true) &&
                    !candidate.equals("preview", ignoreCase = true)
            }
            ?: fallbackFileName

        firestore.collection(collectionName).add(
            hashMapOf(
                "title" to beatTitle,
                "artistName" to request.artistName,
                "email" to request.email,
                "notes" to request.notes,
                "fileName" to fileName,
                "mimeType" to source.mimeType,
                "downloadURL" to source.downloadUrl.orEmpty(),
                "storagePath" to "",
                "uploaderName" to (currentUser?.username ?: request.artistName),
                "uploaderEmail" to (currentUser?.email ?: request.email),
                "uploaderID" to currentUser?.id.orEmpty(),
                "isPublic" to true,
                "sourceProvider" to source.provider.rawValue,
                "externalURL" to source.externalUrl,
                "sourceFileID" to source.sourceFileId.orEmpty(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun updateBeatVisibility(
        beatId: String,
        isPublic: Boolean,
    ) {
        firestore.collection(collectionName)
            .document(beatId)
            .update("isPublic", isPublic)
            .await()
    }

    suspend fun deleteBeat(beat: NicmaBeatHubItem) {
        if (beat.storagePath.isNotBlank()) {
            runCatching {
                storage.reference.child(beat.storagePath).delete().await()
            }
        }

        firestore.collection(collectionName)
            .document(beat.id)
            .delete()
            .await()
    }

    private suspend fun uploadSingleFile(
        context: Context,
        request: NicmaBeatUploadRequest,
        file: NicmaSelectedBeatFile,
        currentUser: User?,
    ) {
        val safeArtist = sanitizePathComponent(request.artistName)
        val storagePath = buildUploadPath(
            rootFolder = "beats",
            scopeFolder = safeArtist,
            fileName = file.fileName,
        )
        val reference = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType(file.mimeType)
            .setCustomMetadata("artistName", request.artistName)
            .setCustomMetadata("email", request.email)
            .setCustomMetadata("notes", request.notes)
            .setCustomMetadata("originalFilename", file.fileName)
            .setCustomMetadata("uploadedAt", Timestamp.now().toDate().time.toString())
            .build()

        val stagedFile = context.stagePickerFileForUpload(
            sourceUri = file.uri,
            fileName = file.fileName,
        )

        try {
            reference.putStagedFile(
                stagedFile = stagedFile,
                metadata = metadata,
            )
        } catch (error: Exception) {
            throw error.toReadableStorageUploadError(file.fileName)
        } finally {
            stagedFile.delete()
        }

        val downloadUrl = reference.downloadUrl.await().toString()
        val beatTitle = request.beatTitle.trim().ifBlank { displayTitle(file.fileName) }

        firestore.collection(collectionName).add(
            hashMapOf(
                "title" to beatTitle,
                "artistName" to request.artistName,
                "email" to request.email,
                "notes" to request.notes,
                "fileName" to file.fileName,
                "mimeType" to file.mimeType,
                "downloadURL" to downloadUrl,
                "externalURL" to "",
                "storagePath" to storagePath,
                "uploaderName" to (currentUser?.username ?: request.artistName),
                "uploaderEmail" to (currentUser?.email ?: request.email),
                "uploaderID" to (currentUser?.id.orEmpty()),
                "isPublic" to (currentUser?.canManageMusic == true),
                "sourceProvider" to ExternalMediaProvider.FIREBASE_STORAGE.rawValue,
                "sourceFileID" to "",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun mapBeat(document: QueryDocumentSnapshot): NicmaBeatHubItem? {
        val data = document.data
        val title = data["title"] as? String ?: return null
        val artistName = data["artistName"] as? String ?: return null
        val fileName = data["fileName"] as? String ?: title
        val downloadUrl = data["downloadURL"] as? String ?: ""
        val externalUrl = data["externalURL"] as? String ?: ""
        if (downloadUrl.isBlank() && externalUrl.isBlank()) {
            return null
        }

        val createdAtMillis = when (val createdAt = data["createdAt"]) {
            is Timestamp -> createdAt.toDate().time
            is java.util.Date -> createdAt.time
            is Number -> createdAt.toLong()
            else -> System.currentTimeMillis()
        }

        return NicmaBeatHubItem(
            id = document.id,
            title = title,
            artistName = artistName,
            fileName = fileName,
            downloadUrl = downloadUrl,
            notes = data["notes"] as? String ?: "",
            uploaderName = data["uploaderName"] as? String ?: artistName,
            uploaderEmail = data["uploaderEmail"] as? String ?: (data["email"] as? String ?: ""),
            uploaderId = data["uploaderID"] as? String ?: "",
            mimeType = data["mimeType"] as? String ?: "application/octet-stream",
            storagePath = data["storagePath"] as? String ?: "",
            isPublic = data["isPublic"] as? Boolean ?: false,
            sourceProvider = data["sourceProvider"] as? String ?: ExternalMediaProvider.FIREBASE_STORAGE.rawValue,
            externalUrl = externalUrl,
            sourceFileId = data["sourceFileID"] as? String ?: "",
            createdAtMillis = createdAtMillis,
        )
    }

    private fun sanitizePathComponent(value: String): String {
        val raw = value
            .lowercase()
            .replace(Regex("[^a-z0-9_-]+"), "-")
            .trim('-')

        return if (raw.isBlank()) "upload" else raw
    }

    private fun sanitizeFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
            .replace(Regex("[^a-z0-9]+"), "")
        val baseName = fileName.substringBeforeLast('.', fileName)
        val safeBaseName = sanitizePathComponent(baseName)

        return if (extension.isBlank()) {
            safeBaseName
        } else {
            "$safeBaseName.$extension"
        }
    }

    private fun buildUploadPath(
        rootFolder: String,
        scopeFolder: String,
        fileName: String,
    ): String {
        val uploadId = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
        return "$rootFolder/$scopeFolder/$uploadId/${sanitizeFileName(fileName)}"
    }

    private fun displayTitle(fileName: String): String {
        val baseName = fileName.substringBeforeLast(".")
        val cleaned = baseName
            .replace("_", " ")
            .replace("-", " ")
            .trim()

        return if (cleaned.isBlank()) "Beat Upload" else cleaned
    }

    private fun beatQuery(isAdmin: Boolean): Query {
        val baseCollection = firestore.collection(collectionName)
        if (isAdmin) {
            return baseCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        }

        return baseCollection
            .whereEqualTo("isPublic", true)
    }
}
