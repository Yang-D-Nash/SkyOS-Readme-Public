package com.skydown.android.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.data.NicmaBeatUploadRequest
import com.skydown.android.data.NicmaBeatUploadService
import com.skydown.android.ui.model.NicmaBeatHubItem
import com.skydown.android.ui.model.NicmaProducerUiState
import com.skydown.android.ui.model.NicmaSelectedBeatFile
import com.skydown.shared.model.User
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
            AppContainer.refreshCurrentUser()
            AppContainer.currentUser.collectLatest { user ->
                currentUser = user
                val nextIsAdmin = user?.isAdmin == true
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

    fun setSelectedFiles(
        context: Context,
        uris: List<android.net.Uri>,
    ) {
        if (!_uiState.value.isAdmin) {
            _uiState.update {
                it.copy(
                    validationMessage = "Nur Admins koennen Beats hochladen.",
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

            context.resolveNicmaSelectedFile(uri)
        }

        _uiState.update {
            it.copy(
                selectedFiles = files,
                validationMessage = if (files.isEmpty()) {
                    "Bitte waehle mindestens eine Audio-Datei oder eine ZIP aus."
                } else {
                    null
                },
                feedbackMessage = if (files.isEmpty()) {
                    "Keine unterstuetzten Dateien gefunden."
                } else {
                    "${files.size} Datei(en) fuer den Beat Hub ausgewaehlt."
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
                            "Beat wieder verborgen."
                        } else {
                            "Beat fuer alle freigegeben."
                        },
                        feedbackIsError = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Der Beat-Status konnte nicht aktualisiert werden.",
                        feedbackIsError = true,
                    )
                }
            }
        }
    }

    fun upload(context: Context) {
        val currentState = _uiState.value
        if (!currentState.isAdmin) {
            _uiState.update {
                it.copy(
                    validationMessage = "Nur Admins koennen Beats hochladen.",
                    feedbackMessage = "Uploads sind nur fuer Admins verfuegbar.",
                    feedbackIsError = true,
                )
            }
            return
        }

        val trimmedTitle = currentState.beatTitle.trim()
        val trimmedArtist = currentState.artistName.trim()
        val trimmedEmail = currentState.email.trim()
        val trimmedNotes = currentState.notes.trim()

        when {
            trimmedArtist.isBlank() -> {
                _uiState.update {
                    it.copy(validationMessage = "Bitte trag dein Projekt oder deinen Artist-Namen ein.")
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
                    it.copy(validationMessage = "Bitte waehle mindestens eine Audio-Datei oder eine ZIP aus.")
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
                        feedbackMessage = "${currentState.selectedFiles.size} Beat-Datei(en) hochgeladen.",
                        feedbackIsError = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        feedbackMessage = "Der Upload ist fehlgeschlagen. Bitte versuch es noch einmal.",
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
                        feedbackMessage = "Der Beat Hub konnte gerade nicht geladen werden.",
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
