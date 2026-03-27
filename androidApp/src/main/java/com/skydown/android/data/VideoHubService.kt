package com.skydown.android.data

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.skydown.android.ui.model.SelectedVideoFile
import com.skydown.android.ui.model.VideoHubItem
import com.skydown.shared.model.User
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class VideoHubUploadRequest(
    val title: String,
    val projectName: String,
    val email: String,
    val notes: String,
    val files: List<SelectedVideoFile>,
)

class VideoHubService(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "videographyHub"

    fun observeVideos(
        isAdmin: Boolean,
        onChange: (Result<List<VideoHubItem>>) -> Unit,
    ): () -> Unit {
        val listener = videoQuery(isAdmin)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onChange(Result.failure(error))
                    return@addSnapshotListener
                }

                val videos = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        mapVideo(document as? QueryDocumentSnapshot ?: return@mapNotNull null)
                    }
                    .sortedWith(
                        compareByDescending<VideoHubItem> { it.isHomeFeatured }
                            .thenByDescending { it.createdAtMillis },
                    )

                onChange(Result.success(videos))
            }

        return { listener.remove() }
    }

    suspend fun uploadVideos(
        context: Context,
        request: VideoHubUploadRequest,
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

    suspend fun deleteVideo(video: VideoHubItem) {
        if (video.storagePath.isNotBlank()) {
            runCatching {
                storage.reference.child(video.storagePath).delete().await()
            }
        }

        firestore.collection(collectionName)
            .document(video.id)
            .delete()
            .await()
    }

    suspend fun setHomeFeaturedVideo(video: VideoHubItem?) {
        val collection = firestore.collection(collectionName)
        val currentFeatured = collection
            .whereEqualTo("isHomeFeatured", true)
            .get()
            .await()

        val batch = firestore.batch()
        currentFeatured.documents.forEach { document ->
            batch.update(document.reference, "isHomeFeatured", false)
        }

        video?.let {
            batch.set(
                collection.document(it.id),
                mapOf("isHomeFeatured" to true),
                SetOptions.merge(),
            )
        }

        batch.commit().await()
    }

    private suspend fun uploadSingleFile(
        context: Context,
        request: VideoHubUploadRequest,
        file: SelectedVideoFile,
        currentUser: User?,
    ) {
        val safeProject = sanitizePathComponent(request.projectName)
        val storagePath = buildUploadPath(
            rootFolder = "videos",
            scopeFolder = safeProject,
            fileName = file.fileName,
        )
        val reference = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType(file.mimeType)
            .setCustomMetadata("projectName", request.projectName)
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
            reference.putFile(Uri.fromFile(stagedFile), metadata).await()
        } catch (error: Exception) {
            throw error.toReadableStorageUploadError(file.fileName)
        } finally {
            stagedFile.delete()
        }

        val downloadUrl = reference.downloadUrl.await().toString()
        val title = request.title.trim().ifBlank { displayTitle(file.fileName) }

        firestore.collection(collectionName).add(
            hashMapOf(
                "title" to title,
                "projectName" to request.projectName,
                "email" to request.email,
                "notes" to request.notes,
                "fileName" to file.fileName,
                "mimeType" to file.mimeType,
                "downloadURL" to downloadUrl,
                "storagePath" to storagePath,
                "uploaderName" to (currentUser?.username ?: request.projectName),
                "uploaderEmail" to (currentUser?.email ?: request.email),
                "uploaderID" to (currentUser?.id.orEmpty()),
                "isPublic" to true,
                "isHomeFeatured" to false,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun mapVideo(document: QueryDocumentSnapshot): VideoHubItem? {
        val data = document.data
        val title = data["title"] as? String ?: return null
        val projectName = data["projectName"] as? String ?: return null
        val fileName = data["fileName"] as? String ?: return null
        val downloadUrl = data["downloadURL"] as? String ?: return null

        val createdAtMillis = when (val createdAt = data["createdAt"]) {
            is Timestamp -> createdAt.toDate().time
            is java.util.Date -> createdAt.time
            is Number -> createdAt.toLong()
            else -> System.currentTimeMillis()
        }

        return VideoHubItem(
            id = document.id,
            title = title,
            projectName = projectName,
            fileName = fileName,
            downloadUrl = downloadUrl,
            notes = data["notes"] as? String ?: "",
            uploaderName = data["uploaderName"] as? String ?: projectName,
            uploaderEmail = data["uploaderEmail"] as? String ?: (data["email"] as? String ?: ""),
            uploaderId = data["uploaderID"] as? String ?: "",
            mimeType = data["mimeType"] as? String ?: "application/octet-stream",
            storagePath = data["storagePath"] as? String ?: "",
            isPublic = data["isPublic"] as? Boolean ?: true,
            isHomeFeatured = data["isHomeFeatured"] as? Boolean ?: false,
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

        return if (cleaned.isBlank()) "Video Upload" else cleaned
    }

    private fun videoQuery(isAdmin: Boolean): Query {
        val baseCollection = firestore.collection(collectionName)
        if (isAdmin) {
            return baseCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        }

        return baseCollection.whereEqualTo("isPublic", true)
    }
}
