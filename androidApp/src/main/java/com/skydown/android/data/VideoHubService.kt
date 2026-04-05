package com.skydown.android.data

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.skydown.android.ui.model.SelectedVideoFile
import com.skydown.android.ui.model.ProducedWithArtist
import com.skydown.android.ui.model.VideoEquipmentItem
import com.skydown.android.ui.model.VideoHubItem
import com.skydown.android.ui.model.VideoHubPublicConfig
import com.skydown.android.ui.model.VideoYouTubeItem
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

data class ExternalVideoHubRequest(
    val title: String,
    val projectName: String,
    val email: String,
    val notes: String,
    val externalUrl: String,
)

class VideoHubService(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "videographyHub"
    private val configCollectionName = "videographyHubMeta"
    private val configDocumentId = "publicConfig"

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

    fun observePublicConfig(
        onChange: (Result<VideoHubPublicConfig>) -> Unit,
    ): () -> Unit {
        val listener = firestore.collection(configCollectionName)
            .document(configDocumentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onChange(Result.failure(error))
                    return@addSnapshotListener
                }

                onChange(Result.success(mapPublicConfig(snapshot?.data)))
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

    suspend fun addExternalVideo(
        request: ExternalVideoHubRequest,
        currentUser: User?,
    ) {
        val source = resolveExternalVideoSource(request.externalUrl)
            ?: error("Externer Video-Link konnte nicht erkannt werden.")
        val trimmedTitle = request.title.trim()
        val title = trimmedTitle.ifBlank {
            "${request.projectName.trim().ifBlank { "Video" }} Clip"
        }
        val fallbackFileName = source.sourceFileId?.takeIf { it.isNotBlank() }?.let { "source-$it" }
            ?: "${source.provider.rawValue}-video"
        val fileName = source.normalizedUrl.substringAfterLast('/').substringBefore('?')
            .takeIf { candidate ->
                candidate.isNotBlank() &&
                    !candidate.equals("view", ignoreCase = true) &&
                    !candidate.equals("preview", ignoreCase = true)
            }
            ?: fallbackFileName

        firestore.collection(collectionName).add(
            hashMapOf(
                "title" to title,
                "projectName" to request.projectName,
                "email" to request.email,
                "notes" to request.notes,
                "fileName" to fileName,
                "mimeType" to source.mimeType,
                "downloadURL" to source.downloadUrl.orEmpty(),
                "storagePath" to "",
                "uploaderName" to (currentUser?.username ?: request.projectName),
                "uploaderEmail" to (currentUser?.email ?: request.email),
                "uploaderID" to currentUser?.id.orEmpty(),
                "isPublic" to true,
                "isHomeFeatured" to false,
                "sourceProvider" to source.provider.rawValue,
                "externalURL" to source.externalUrl,
                "embedURL" to source.embedUrl.orEmpty(),
                "sourceFileID" to source.sourceFileId.orEmpty(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
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

    suspend fun savePublicConfig(
        config: VideoHubPublicConfig,
        currentUser: User?,
    ) {
        val equipmentItems = config.equipmentItems.map { item ->
            mapOf(
                "id" to item.id,
                "title" to item.title,
                "detail" to item.detail,
                "imageUrl" to item.imageUrl.orEmpty(),
            )
        }
        val youtubeItems = config.youtubeItems.map { item ->
            mapOf(
                "id" to item.id,
                "title" to item.title,
                "subtitle" to item.subtitle,
                "highlight" to item.highlight,
                "url" to item.url,
            )
        }
        val collaborationItems = config.collaborationItems.map { item ->
            mapOf(
                "id" to item.id,
                "name" to item.name,
                "role" to item.role,
                "highlight" to item.highlight,
                "vibe" to item.vibe,
                "imageUrl" to item.imageUrl.orEmpty(),
                "spotifyArtistId" to item.spotifyArtistId.orEmpty(),
                "instagramUrl" to item.instagramUrl.orEmpty(),
                "youtubeUrl" to item.youtubeUrl.orEmpty(),
            )
        }

        firestore.collection(configCollectionName)
            .document(configDocumentId)
            .set(
                mapOf(
                    "equipmentItems" to equipmentItems,
                    "youtubeItems" to youtubeItems,
                    "collaborationItems" to collaborationItems,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to currentUser?.id.orEmpty(),
                ),
                SetOptions.merge(),
            )
            .await()
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
                "externalURL" to "",
                "embedURL" to "",
                "storagePath" to storagePath,
                "uploaderName" to (currentUser?.username ?: request.projectName),
                "uploaderEmail" to (currentUser?.email ?: request.email),
                "uploaderID" to (currentUser?.id.orEmpty()),
                "isPublic" to true,
                "isHomeFeatured" to false,
                "sourceProvider" to ExternalMediaProvider.FIREBASE_STORAGE.rawValue,
                "sourceFileID" to "",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun mapVideo(document: QueryDocumentSnapshot): VideoHubItem? {
        val data = document.data
        val title = data["title"] as? String ?: return null
        val projectName = data["projectName"] as? String ?: return null
        val fileName = data["fileName"] as? String ?: title
        val downloadUrl = data["downloadURL"] as? String ?: ""
        val externalUrl = data["externalURL"] as? String ?: ""
        val embedUrl = data["embedURL"] as? String ?: ""
        if (downloadUrl.isBlank() && externalUrl.isBlank() && embedUrl.isBlank()) {
            return null
        }

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
            sourceProvider = data["sourceProvider"] as? String ?: ExternalMediaProvider.FIREBASE_STORAGE.rawValue,
            externalUrl = externalUrl,
            embedUrl = embedUrl,
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

        return if (cleaned.isBlank()) "Video Upload" else cleaned
    }

    private fun videoQuery(isAdmin: Boolean): Query {
        val baseCollection = firestore.collection(collectionName)
        if (isAdmin) {
            return baseCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        }

        return baseCollection.whereEqualTo("isPublic", true)
    }

    private fun mapPublicConfig(data: Map<String, Any>?): VideoHubPublicConfig {
        if (data == null) {
            return VideoHubPublicConfig.default()
        }

        val equipmentItems = (data["equipmentItems"] as? List<*>)
            ?.mapNotNull { value -> mapEquipmentItem(value as? Map<*, *>) }
            ?.ifEmpty { null }
            ?: VideoHubPublicConfig.default().equipmentItems
        val youtubeItems = (data["youtubeItems"] as? List<*>)
            ?.mapNotNull { value -> mapYouTubeItem(value as? Map<*, *>) }
            ?: emptyList()
        val collaborationItems = (data["collaborationItems"] as? List<*>)
            ?.mapNotNull { value -> mapCollaborationItem(value as? Map<*, *>) }
            ?.ifEmpty { null }
            ?: VideoHubPublicConfig.default().collaborationItems

        return VideoHubPublicConfig(
            equipmentItems = equipmentItems,
            youtubeItems = youtubeItems,
            collaborationItems = collaborationItems,
        )
    }

    private fun mapEquipmentItem(value: Map<*, *>?): VideoEquipmentItem? {
        val map = value ?: return null
        val title = (map["title"] as? String)?.trim().orEmpty()
        val detail = (map["detail"] as? String)?.trim().orEmpty()
        if (title.isBlank() || detail.isBlank()) return null

        return VideoEquipmentItem(
            id = ((map["id"] as? String)?.trim()).takeUnless { it.isNullOrBlank() } ?: UUID.randomUUID().toString(),
            title = title,
            detail = detail,
            imageUrl = ((map["imageUrl"] as? String) ?: (map["imageURLString"] as? String))
                ?.trim()
                .takeUnless { it.isNullOrBlank() },
        )
    }

    private fun mapYouTubeItem(value: Map<*, *>?): VideoYouTubeItem? {
        val map = value ?: return null
        val title = (map["title"] as? String)?.trim().orEmpty()
        val subtitle = (map["subtitle"] as? String)?.trim().orEmpty()
        val highlight = (map["highlight"] as? String)?.trim().orEmpty()
        val url = ((map["url"] as? String) ?: (map["urlString"] as? String)).orEmpty().trim()
        if (title.isBlank() || url.isBlank()) return null

        return VideoYouTubeItem(
            id = ((map["id"] as? String)?.trim()).takeUnless { it.isNullOrBlank() } ?: UUID.randomUUID().toString(),
            title = title,
            subtitle = subtitle,
            highlight = highlight,
            url = url,
        )
    }

    private fun mapCollaborationItem(value: Map<*, *>?): ProducedWithArtist? {
        val map = value ?: return null
        val name = (map["name"] as? String)?.trim().orEmpty()
        val role = (map["role"] as? String)?.trim().orEmpty()
        val highlight = (map["highlight"] as? String)?.trim().orEmpty()
        val vibe = (map["vibe"] as? String)?.trim().orEmpty()
        if (name.isBlank() || role.isBlank()) return null

        return ProducedWithArtist(
            id = ((map["id"] as? String)?.trim()).takeUnless { it.isNullOrBlank() } ?: UUID.randomUUID().toString(),
            name = name,
            role = role,
            highlight = highlight,
            vibe = vibe,
            imageUrl = (map["imageUrl"] as? String)?.trim().takeUnless { it.isNullOrBlank() },
            spotifyArtistId = (map["spotifyArtistId"] as? String)?.trim().takeUnless { it.isNullOrBlank() },
            instagramUrl = (map["instagramUrl"] as? String)?.trim().takeUnless { it.isNullOrBlank() },
            youtubeUrl = ((map["youtubeUrl"] as? String) ?: (map["youtubeURLString"] as? String))
                ?.trim()
                .takeUnless { it.isNullOrBlank() },
        )
    }
}
