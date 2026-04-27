package com.nash.skyos.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.AppTextResolver
import com.nash.skyos.data.ExternalBeatUploadRequest
import com.nash.skyos.data.NicmaBeatUploadRequest
import com.nash.skyos.data.NicmaBeatUploadService
import com.nash.skyos.R
import com.nash.skyos.ui.model.NicmaBeatHubItem
import com.nash.skyos.ui.model.NicmaProducerUiState
import com.nash.skyos.ui.model.NicmaSelectedBeatFile
import com.skydown.shared.model.User
import com.skydown.shared.model.canManageMusic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NicmaProducerViewModel(
    private val beatHubService: NicmaBeatUploadService = NicmaBeatUploadService(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(NicmaProducerUiState())
    val uiState: StateFlow<NicmaProducerUiState> = _uiState.asStateFlow()

    private var currentUser: User? = null
    private var allBeats: List<NicmaBeatHubItem> = emptyList()
    private var beatObservationCancellation: (() -> Unit)? = null
    private var observedAdminState: Boolean? = null

    init {
        viewModelScope.launch {
            AppContainer.currentUser.collectLatest { user ->
                currentUser = user
                val nextIsAdmin = user?.canManageMusic == true
                _uiState.update { state ->
                    state.copy(
                        isAdmin = nextIsAdmin,
                        artistName = if (state.artistName.isBlank()) {
                            user?.username ?: state.artistName
                        } else {
                            state.artistName
                        },
                        email = if (state.email.isBlank()) {
                            user?.email ?: state.email
                        } else {
                            state.email
                        },
                    )
                }

                if (beatObservationCancellation == null || observedAdminState != nextIsAdmin) {
                    observeBeats(isAdmin = nextIsAdmin)
                } else {
                    applyVisibleBeats()
                }
            }
        }
    }

    override fun onCleared() {
        beatObservationCancellation?.invoke()
        super.onCleared()
    }

    fun updateBeatTitle(value: String) {
        _uiState.update { it.copy(beatTitle = value) }
    }

    fun updateArtistName(value: String) {
        _uiState.update { it.copy(artistName = value) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun updateExternalBeatUrl(value: String) {
        _uiState.update { it.copy(externalBeatUrl = value) }
    }

    fun setSelectedFiles(
        context: Context,
        uris: List<android.net.Uri>,
    ) {
        if (!_uiState.value.isAdmin) {
            showAdminOnlyUploadError()
            return
        }

        val files = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }

            context.resolveNicmaSelectedFile(uri)
        }

        _uiState.update {
            it.copy(
                selectedFiles = files,
                validationMessage = if (files.isEmpty()) {
                    AppTextResolver.string(R.string.nicma_upload_select_audio_or_zip)
                } else {
                    null
                },
                feedbackMessage = if (files.isEmpty()) {
                    AppTextResolver.string(R.string.nicma_upload_no_supported_files)
                } else {
                    AppTextResolver.string(R.string.nicma_upload_files_selected, files.size)
                },
                feedbackIsError = files.isEmpty(),
            )
        }
    }

    fun removeFile(file: NicmaSelectedBeatFile) {
        _uiState.update {
            it.copy(
                selectedFiles = it.selectedFiles - file,
                validationMessage = null,
            )
        }
    }

    fun toggleBeatVisibility(beat: NicmaBeatHubItem) {
        if (!_uiState.value.isAdmin) return

        viewModelScope.launch {
            runCatching {
                beatHubService.updateBeatVisibility(
                    beatId = beat.id,
                    isPublic = !beat.isPublic,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        feedbackMessage = if (beat.isPublic) {
                            AppTextResolver.string(R.string.nicma_beat_hidden_again)
                        } else {
                            AppTextResolver.string(R.string.nicma_beat_released_public)
                        },
                        feedbackIsError = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        feedbackMessage = AppTextResolver.string(R.string.nicma_beat_status_update_failed),
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    fun deleteBeat(beat: NicmaBeatHubItem) {
        if (!_uiState.value.isAdmin) return

        viewModelScope.launch {
            runCatching {
                beatHubService.deleteBeat(beat)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        feedbackMessage = AppTextResolver.string(R.string.nicma_beat_deleted),
                        feedbackIsError = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        feedbackMessage = AppTextResolver.string(R.string.nicma_beat_delete_failed),
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    fun upload(context: Context) {
        val currentState = _uiState.value
        if (!currentState.isAdmin) {
            showAdminOnlyUploadError()
            return
        }

        val trimmedTitle = currentState.beatTitle.trim()
        val trimmedArtist = currentState.artistName.trim()
        val trimmedEmail = currentState.email.trim()
        val trimmedNotes = currentState.notes.trim()

        when {
            trimmedArtist.isBlank() -> {
                _uiState.update {
                    it.copy(validationMessage = AppTextResolver.string(R.string.nicma_validation_artist_required))
                }
                return
            }

            !trimmedEmail.contains("@") -> {
                _uiState.update {
                    it.copy(validationMessage = AppTextResolver.string(R.string.nicma_validation_email_required))
                }
                return
            }

            currentState.selectedFiles.isEmpty() -> {
                _uiState.update {
                    it.copy(validationMessage = AppTextResolver.string(R.string.nicma_upload_select_audio_or_zip))
                }
                return
            }
        }

        viewModelScope.launch {
            setUploadInProgress()

            runCatching {
                beatHubService.uploadBeats(
                    context = context,
                    request = NicmaBeatUploadRequest(
                        beatTitle = trimmedTitle,
                        artistName = trimmedArtist,
                        email = trimmedEmail,
                        notes = trimmedNotes,
                        files = currentState.selectedFiles,
                    ),
                    currentUser = currentUser,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        beatTitle = "",
                        notes = "",
                        selectedFiles = emptyList(),
                        isUploading = false,
                        feedbackMessage = AppTextResolver.string(
                            R.string.nicma_upload_files_uploaded,
                            currentState.selectedFiles.size,
                        ),
                        feedbackIsError = false,
                    )
                }
            }.onFailure {
                val detail = it.localizedMessage?.takeIf { message -> message.isNotBlank() }
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        feedbackMessage = detail?.let { message ->
                            AppTextResolver.string(R.string.nicma_upload_failed_with_detail, message)
                        } ?: AppTextResolver.string(R.string.nicma_upload_failed_retry),
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    fun addExternalBeat() {
        val currentState = _uiState.value
        if (!currentState.isAdmin) {
            showAdminOnlyExternalError()
            return
        }

        val trimmedArtist = currentState.artistName.trim()
        val trimmedEmail = currentState.email.trim()
        val trimmedTitle = currentState.beatTitle.trim()
        val trimmedNotes = currentState.notes.trim()
        val trimmedUrl = currentState.externalBeatUrl.trim()

        when {
            trimmedArtist.isBlank() -> {
                _uiState.update {
                    it.copy(validationMessage = AppTextResolver.string(R.string.nicma_validation_artist_required))
                }
                return
            }

            !trimmedEmail.contains("@") -> {
                _uiState.update {
                    it.copy(validationMessage = AppTextResolver.string(R.string.nicma_validation_email_required))
                }
                return
            }

            trimmedUrl.isBlank() -> {
                _uiState.update {
                    it.copy(validationMessage = AppTextResolver.string(R.string.nicma_external_link_required))
                }
                return
            }
        }

        viewModelScope.launch {
            setUploadInProgress()

            runCatching {
                beatHubService.addExternalBeat(
                    request = ExternalBeatUploadRequest(
                        beatTitle = trimmedTitle,
                        artistName = trimmedArtist,
                        email = trimmedEmail,
                        notes = trimmedNotes,
                        externalUrl = trimmedUrl,
                    ),
                    currentUser = currentUser,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        beatTitle = "",
                        notes = "",
                        externalBeatUrl = "",
                        isUploading = false,
                        feedbackMessage = AppTextResolver.string(R.string.nicma_external_released),
                        feedbackIsError = false,
                    )
                }
            }.onFailure { error ->
                val detail = error.localizedMessage?.takeIf { message -> message.isNotBlank() }
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        feedbackMessage = detail?.let { message ->
                            AppTextResolver.string(R.string.nicma_external_failed_with_detail, message)
                        } ?: AppTextResolver.string(R.string.nicma_external_failed_save),
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    private fun observeBeats(isAdmin: Boolean) {
        observedAdminState = isAdmin
        _uiState.update { it.copy(isLoadingBeats = true) }
        allBeats = emptyList()
        _uiState.update { it.copy(beats = emptyList()) }
        beatObservationCancellation?.invoke()
        beatObservationCancellation = beatHubService.observeBeats(isAdmin = isAdmin) { result ->
            result.onSuccess { beats ->
                allBeats = beats
                applyVisibleBeats()
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoadingBeats = false,
                        feedbackMessage = AppTextResolver.string(R.string.nicma_hub_load_failed),
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    private fun applyVisibleBeats() {
        val isAdmin = _uiState.value.isAdmin
        val visibleBeats = if (isAdmin) {
            allBeats
        } else {
            allBeats.filter { beat -> beat.isPublic }
        }

        _uiState.update {
            it.copy(
                beats = visibleBeats,
                isLoadingBeats = false,
            )
        }
    }

    private fun setUploadInProgress() {
        _uiState.update {
            it.copy(
                isUploading = true,
                validationMessage = null,
                feedbackMessage = null,
            )
        }
    }

    private fun showAdminOnlyUploadError() {
        _uiState.update {
            it.copy(
                validationMessage = AppTextResolver.string(R.string.nicma_upload_admin_only),
                feedbackMessage = AppTextResolver.string(R.string.nicma_upload_admin_only_detail),
                feedbackIsError = true,
            )
        }
    }

    private fun showAdminOnlyExternalError() {
        _uiState.update {
            it.copy(
                validationMessage = AppTextResolver.string(R.string.nicma_external_admin_only),
                feedbackMessage = AppTextResolver.string(R.string.nicma_external_admin_only_detail),
                feedbackIsError = true,
            )
        }
    }
}

private fun Context.resolveNicmaSelectedFile(
    uri: android.net.Uri,
): NicmaSelectedBeatFile? {
    var fileName = "Beat Upload"
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
    if (!mimeType.isSupportedNicmaType(fileName)) {
        return null
    }

    return NicmaSelectedBeatFile(
        uri = uri,
        fileName = fileName,
        fileSizeBytes = fileSizeBytes,
        mimeType = mimeType.ifBlank { "application/octet-stream" },
    )
}

private fun String.isSupportedNicmaType(fileName: String): Boolean {
    if (startsWith("audio/")) {
        return true
    }

    val lowerFileName = fileName.lowercase()
    return this == "application/zip" ||
        this == "application/x-zip-compressed" ||
        lowerFileName.endsWith(".zip")
}
