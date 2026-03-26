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
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class NicmaBeatUploadRequest(
    val beatTitle: String,
    val artistName: String,
    val email: String,
    val notes: String,
    val files: List<NicmaSelectedBeatFile>,
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

                val beats = snapshot?.documents.orEmpty().mapNotNull { document ->
                    mapBeat(document as? QueryDocumentSnapshot ?: return@mapNotNull null)
                }
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

    suspend fun updateBeatVisibility(
        beatId: String,
        isPublic: Boolean,
    ) {
        firestore.collection(collectionName)
            .document(beatId)
            .update("isPublic", isPublic)
            .await()
    }

    private suspend fun uploadSingleFile(
        context: Context,
        request: NicmaBeatUploadRequest,
        file: NicmaSelectedBeatFile,
        currentUser: User?,
    ) {
        val safeArtist = sanitizePathComponent(request.artistName)
        val safeFileName = sanitizePathComponent(file.fileName.replace(".", "-"))
        val storagePath = buildString {
            append("nicma_music/beats/")
            append(safeArtist)
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
            .setCustomMetadata("artistName", request.artistName)
            .setCustomMetadata("email", request.email)
            .setCustomMetadata("notes", request.notes)
            .setCustomMetadata("originalFilename", file.fileName)
            .setCustomMetadata("uploadedAt", Timestamp.now().toDate().time.toString())
            .build()

        context.contentResolver.openInputStream(file.uri)?.close()
            ?: error("Die Datei ${file.fileName} konnte nicht gelesen werden.")

        reference.putFile(file.uri, metadata).await()
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
                "storagePath" to storagePath,
                "uploaderName" to (currentUser?.username ?: request.artistName),
                "uploaderEmail" to (currentUser?.email ?: request.email),
                "uploaderID" to (currentUser?.id.orEmpty()),
                "isPublic" to (currentUser?.isAdmin == true),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun mapBeat(document: QueryDocumentSnapshot): NicmaBeatHubItem? {
        val data = document.data
        val title = data["title"] as? String ?: return null
        val artistName = data["artistName"] as? String ?: return null
        val fileName = data["fileName"] as? String ?: return null
        val downloadUrl = data["downloadURL"] as? String ?: return null

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
            isPublic = data["isPublic"] as? Boolean ?: false,
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

        return if (cleaned.isBlank()) "Beat Upload" else cleaned
    }

    private fun beatQuery(isAdmin: Boolean): Query {
        val baseCollection = firestore.collection(collectionName)
        if (isAdmin) {
            return baseCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        }

        return baseCollection
            .whereEqualTo("isPublic", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
    }
}
