package com.skydown.android.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.data.VideoHubService
import com.skydown.android.data.VideoHubUploadRequest
import com.skydown.android.ui.model.VideoEquipmentItem
import com.skydown.android.ui.model.ProducedWithArtist
import com.skydown.android.ui.model.SelectedVideoFile
import com.skydown.android.ui.model.VideoHubItem
import com.skydown.android.ui.model.VideoHubUiState
import com.skydown.android.ui.model.VideoHubPublicConfig
import com.skydown.android.ui.model.VideoYouTubeItem
import com.skydown.shared.model.User
import com.skydown.shared.model.canManageVideos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoHubViewModel(
    private val videoHubService: VideoHubService = VideoHubService(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoHubUiState())
    val uiState: StateFlow<VideoHubUiState> = _uiState.asStateFlow()

    private var currentUser: User? = null
    private var videoObservationCancellation: (() -> Unit)? = null
    private var publicConfigObservationCancellation: (() -> Unit)? = null
    private var observedAdminState: Boolean? = null
    private var hasLoadedVideosOnce = false

    init {
        observePublicConfig()
        viewModelScope.launch {
            AppContainer.refreshCurrentUser()
            AppContainer.currentUser.collectLatest { user ->
                currentUser = user
                val nextIsAdmin = user?.canManageVideos == true
                _uiState.update { state ->
                    state.copy(
                        isAdmin = nextIsAdmin,
                        projectName = if (state.projectName.isBlank()) {
                            user?.username ?: state.projectName
                        } else {
                            state.projectName
                        },
                        email = if (state.email.isBlank()) {
                            user?.email ?: state.email
                        } else {
                            state.email
                        },
                    )
                }

                if (videoObservationCancellation == null || observedAdminState != nextIsAdmin) {
                    observeVideos(isAdmin = nextIsAdmin)
                }
            }
        }
    }

    override fun onCleared() {
        videoObservationCancellation?.invoke()
        publicConfigObservationCancellation?.invoke()
        super.onCleared()
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(videoTitle = value) }
    }

    fun updateProjectName(value: String) {
        _uiState.update { it.copy(projectName = value) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun setSelectedFiles(
        context: Context,
        uris: List<android.net.Uri>,
    ) {
        if (!_uiState.value.isAdmin) {
            _uiState.update {
                it.copy(
                    validationMessage = "Nur Admins koennen Videos hochladen.",
                    feedbackMessage = "Uploads sind nur fuer Admins verfuegbar.",
                    feedbackIsError = true,
                )
            }
            return
        }

        val files = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            context.resolveSelectedVideoFile(uri)
        }

        _uiState.update {
            it.copy(
                selectedFiles = files,
                validationMessage = if (files.isEmpty()) {
                    "Bitte waehle mindestens eine MP4-, MOV- oder M4V-Datei aus."
                } else {
                    null
                },
                feedbackMessage = if (files.isEmpty()) {
                    "Keine unterstuetzten Videoformate gefunden."
                } else {
                    "${files.size} Video-Datei(en) ausgewaehlt."
                },
                feedbackIsError = files.isEmpty(),
            )
        }
    }

    fun removeFile(file: SelectedVideoFile) {
        _uiState.update {
            it.copy(
                selectedFiles = it.selectedFiles - file,
                validationMessage = null,
            )
        }
    }

    fun upload(context: Context) {
        val currentState = _uiState.value
        if (!currentState.isAdmin) {
            _uiState.update {
                it.copy(
                    validationMessage = "Nur Admins koennen Videos hochladen.",
                    feedbackMessage = "Uploads sind nur fuer Admins verfuegbar.",
                    feedbackIsError = true,
                )
            }
            return
        }

        val trimmedProject = currentState.projectName.trim()
        val trimmedEmail = currentState.email.trim()
        val trimmedTitle = currentState.videoTitle.trim()
        val trimmedNotes = currentState.notes.trim()

        when {
            trimmedProject.isBlank() -> {
                _uiState.update {
                    it.copy(validationMessage = "Bitte trag ein Projekt, einen Artist oder einen Videotitel ein.")
                }
                return
            }

            !trimmedEmail.contains("@") -> {
                _uiState.update {
                    it.copy(validationMessage = "Bitte trag eine gueltige E-Mail ein.")
                }
                return
            }

            currentState.selectedFiles.isEmpty() -> {
                _uiState.update {
                    it.copy(validationMessage = "Bitte waehle mindestens eine MP4-, MOV- oder M4V-Datei aus.")
                }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    validationMessage = null,
                    feedbackMessage = null,
                )
            }

            runCatching {
                videoHubService.uploadVideos(
                    context = context,
                    request = VideoHubUploadRequest(
                        title = trimmedTitle,
                        projectName = trimmedProject,
                        email = trimmedEmail,
                        notes = trimmedNotes,
                        files = currentState.selectedFiles,
                    ),
                    currentUser = currentUser,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        videoTitle = "",
                        notes = "",
                        selectedFiles = emptyList(),
                        isUploading = false,
                        feedbackMessage = "${currentState.selectedFiles.size} Video-Datei(en) hochgeladen.",
                        feedbackIsError = false,
                    )
                }
            }.onFailure {
                val detail = it.localizedMessage?.takeIf { message -> message.isNotBlank() }
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        feedbackMessage = detail?.let { message ->
                            "Der Video-Upload ist fehlgeschlagen: $message"
                        } ?: "Der Video-Upload ist fehlgeschlagen. Bitte versuch es noch einmal.",
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    fun deleteVideo(video: VideoHubItem) {
        if (!_uiState.value.isAdmin) return

        viewModelScope.launch {
            runCatching {
                videoHubService.deleteVideo(video)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Video entfernt.",
                        feedbackIsError = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Das Video konnte nicht geloescht werden.",
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    fun toggleHomeFeatured(video: VideoHubItem) {
        if (!_uiState.value.isAdmin) return

        viewModelScope.launch {
            runCatching {
                videoHubService.setHomeFeaturedVideo(
                    video = if (video.isHomeFeatured) null else video,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        feedbackMessage = if (video.isHomeFeatured) {
                            "Home-Video entfernt."
                        } else {
                            "Video fuer Home ausgewaehlt."
                        },
                        feedbackIsError = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Das Home-Video konnte nicht aktualisiert werden.",
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun addEquipmentItem() {
        _uiState.update { state ->
            state.copy(
                publicConfig = state.publicConfig.copy(
                    equipmentItems = state.publicConfig.equipmentItems + VideoEquipmentItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "",
                        detail = "",
                        imageUrl = null,
                    ),
                ),
            )
        }
    }

    fun updateEquipmentItem(
        itemId: String,
        title: String? = null,
        detail: String? = null,
        imageUrl: String? = null,
    ) {
        _uiState.update { state ->
            state.copy(
                publicConfig = state.publicConfig.copy(
                    equipmentItems = state.publicConfig.equipmentItems.map { item ->
                        if (item.id != itemId) {
                            item
                        } else {
                            item.copy(
                                title = title ?: item.title,
                                detail = detail ?: item.detail,
                                imageUrl = imageUrl ?: item.imageUrl,
                            )
                        }
                    },
                ),
            )
        }
    }

    fun removeEquipmentItem(itemId: String) {
        _uiState.update { state ->
            state.copy(
                publicConfig = state.publicConfig.copy(
                    equipmentItems = state.publicConfig.equipmentItems.filterNot { it.id == itemId },
                ),
            )
        }
    }

    fun addYouTubeItem() {
        _uiState.update { state ->
            state.copy(
                publicConfig = state.publicConfig.copy(
                    youtubeItems = state.publicConfig.youtubeItems + VideoYouTubeItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "",
                        subtitle = "",
                        url = "",
                    ),
                ),
            )
        }
    }

    fun updateYouTubeItem(
        itemId: String,
        title: String? = null,
        subtitle: String? = null,
        url: String? = null,
    ) {
        _uiState.update { state ->
            state.copy(
                publicConfig = state.publicConfig.copy(
                    youtubeItems = state.publicConfig.youtubeItems.map { item ->
                        if (item.id != itemId) {
                            item
                        } else {
                            item.copy(
                                title = title ?: item.title,
                                subtitle = subtitle ?: item.subtitle,
                                url = url ?: item.url,
                            )
                        }
                    },
                ),
            )
        }
    }

    fun removeYouTubeItem(itemId: String) {
        _uiState.update { state ->
            state.copy(
                publicConfig = state.publicConfig.copy(
                    youtubeItems = state.publicConfig.youtubeItems.filterNot { it.id == itemId },
                ),
            )
        }
    }

    fun addCollaborationItem() {
        _uiState.update { state ->
            state.copy(
                publicConfig = state.publicConfig.copy(
                    collaborationItems = state.publicConfig.collaborationItems + ProducedWithArtist(
                        id = java.util.UUID.randomUUID().toString(),
                        name = "",
                        role = "",
                        highlight = "",
                        vibe = "",
                        imageUrl = null,
                        spotifyArtistId = null,
                        instagramUrl = null,
                        youtubeUrl = null,
                    ),
                ),
            )
        }
    }

    fun updateCollaborationItem(
        itemId: String,
        name: String? = null,
        role: String? = null,
        highlight: String? = null,
        vibe: String? = null,
        imageUrl: String? = null,
        spotifyArtistId: String? = null,
        instagramUrl: String? = null,
        youtubeUrl: String? = null,
    ) {
        _uiState.update { state ->
            state.copy(
                publicConfig = state.publicConfig.copy(
                    collaborationItems = state.publicConfig.collaborationItems.map { item ->
                        if (item.id != itemId) {
                            item
                        } else {
                            item.copy(
                                name = name ?: item.name,
                                role = role ?: item.role,
                                highlight = highlight ?: item.highlight,
                                vibe = vibe ?: item.vibe,
                                imageUrl = imageUrl ?: item.imageUrl,
                                spotifyArtistId = spotifyArtistId ?: item.spotifyArtistId,
                                instagramUrl = instagramUrl ?: item.instagramUrl,
                                youtubeUrl = youtubeUrl ?: item.youtubeUrl,
                            )
                        }
                    },
                ),
            )
        }
    }

    fun removeCollaborationItem(itemId: String) {
        _uiState.update { state ->
            state.copy(
                publicConfig = state.publicConfig.copy(
                    collaborationItems = state.publicConfig.collaborationItems.filterNot { it.id == itemId },
                ),
            )
        }
    }

    fun savePublicConfig() {
        if (!_uiState.value.isAdmin) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "Nur Admins koennen die Videography-Daten bearbeiten.",
                    feedbackIsError = true,
                )
            }
            return
        }

        val sanitizedEquipment = _uiState.value.publicConfig.equipmentItems.mapNotNull { item ->
            val title = item.title.trim()
            val detail = item.detail.trim()
            if (title.isBlank() || detail.isBlank()) {
                null
            } else {
                item.copy(
                    title = title,
                    detail = detail,
                    imageUrl = item.imageUrl?.trim()?.ifBlank { null },
                )
            }
        }
        val sanitizedYouTube = _uiState.value.publicConfig.youtubeItems.mapNotNull { item ->
            val title = item.title.trim()
            val subtitle = item.subtitle.trim()
            val url = item.url.trim()
            if (title.isBlank() || url.isBlank()) {
                null
            } else {
                item.copy(title = title, subtitle = subtitle, url = url)
            }
        }
        val sanitizedCollaborations = _uiState.value.publicConfig.collaborationItems.mapNotNull { item ->
            val name = item.name.trim()
            val role = item.role.trim()
            val highlight = item.highlight.trim()
            val vibe = item.vibe.trim()
            if (name.isBlank() || role.isBlank()) {
                null
            } else {
                item.copy(
                    name = name,
                    role = role,
                    highlight = highlight,
                    vibe = vibe,
                    imageUrl = item.imageUrl?.trim()?.ifBlank { null },
                    spotifyArtistId = item.spotifyArtistId?.trim()?.ifBlank { null },
                    instagramUrl = item.instagramUrl?.trim()?.ifBlank { null },
                    youtubeUrl = item.youtubeUrl?.trim()?.ifBlank { null },
                )
            }
        }
        val config = VideoHubPublicConfig(
            equipmentItems = sanitizedEquipment.ifEmpty { VideoHubPublicConfig.default().equipmentItems },
            youtubeItems = sanitizedYouTube,
            collaborationItems = sanitizedCollaborations.ifEmpty { VideoHubPublicConfig.default().collaborationItems },
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingPublicConfig = true,
                    feedbackMessage = null,
                )
            }

            runCatching {
                videoHubService.savePublicConfig(config, currentUser)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        publicConfig = config,
                        isSavingPublicConfig = false,
                        feedbackMessage = "Videography-Daten gespeichert.",
                        feedbackIsError = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSavingPublicConfig = false,
                        feedbackMessage = "Die Videography-Daten konnten nicht gespeichert werden.",
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    private fun observeVideos(isAdmin: Boolean) {
        observedAdminState = isAdmin
        _uiState.update { it.copy(isLoadingVideos = true, videos = emptyList()) }
        videoObservationCancellation?.invoke()
        videoObservationCancellation = videoHubService.observeVideos(isAdmin = isAdmin) { result ->
            result.onSuccess { videos ->
                hasLoadedVideosOnce = true
                _uiState.update {
                    it.copy(
                        videos = videos,
                        isLoadingVideos = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        videos = emptyList(),
                        isLoadingVideos = false,
                        feedbackMessage = if (currentUser?.canManageVideos == true || hasLoadedVideosOnce) {
                            "Die Videos konnten gerade nicht geladen werden."
                        } else {
                            null
                        },
                        feedbackIsError = currentUser?.canManageVideos == true || hasLoadedVideosOnce,
                    )
                }
            }
        }
    }

    private fun observePublicConfig() {
        publicConfigObservationCancellation?.invoke()
        publicConfigObservationCancellation = videoHubService.observePublicConfig { result ->
            result.onSuccess { config ->
                _uiState.update { it.copy(publicConfig = config) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        publicConfig = VideoHubPublicConfig.default(),
                    )
                }
            }
        }
    }
}

private fun Context.resolveSelectedVideoFile(
    uri: android.net.Uri,
): SelectedVideoFile? {
    var fileName = "Video Upload"
    var fileSizeBytes = 0L

    contentResolver.query(
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

    val mimeType = contentResolver.getType(uri).orEmpty()
    if (!mimeType.isSupportedVideoType(fileName)) {
        return null
    }

    return SelectedVideoFile(
        uri = uri,
        fileName = fileName,
        fileSizeBytes = fileSizeBytes,
        mimeType = mimeType.ifBlank { "video/mp4" },
    )
}

private fun String.isSupportedVideoType(fileName: String): Boolean {
    if (startsWith("video/")) {
        return true
    }

    val lowerFileName = fileName.lowercase()
    return lowerFileName.endsWith(".mp4") ||
        lowerFileName.endsWith(".mov") ||
        lowerFileName.endsWith(".m4v")
}
