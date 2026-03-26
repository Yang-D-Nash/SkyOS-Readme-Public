package com.skydown.android.data

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
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
                    .sortedByDescending { video -> video.createdAtMillis }

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

    private suspend fun uploadSingleFile(
        context: Context,
        request: VideoHubUploadRequest,
        file: SelectedVideoFile,
        currentUser: User?,
    ) {
        val safeProject = sanitizePathComponent(request.projectName)
        val safeFileName = sanitizePathComponent(file.fileName.replace(".", "-"))
        val storagePath = buildString {
            append("videography/videos/")
            append(safeProject)
            append("/")
            append(System.currentTimeMillis())
            append("-")
            append(UUID.randomUUID())
            append("-")
            append(safeFileName)
        }
        val reference = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType(file.mimeType)
            .setCustomMetadata("projectName", request.projectName)
            .setCustomMetadata("email", request.email)
            .setCustomMetadata("notes", request.notes)
            .setCustomMetadata("originalFilename", file.fileName)
            .build()

        context.contentResolver.openInputStream(file.uri)?.close()
            ?: error("Die Datei ${file.fileName} konnte nicht gelesen werden.")

        reference.putFile(file.uri, metadata).await()
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
