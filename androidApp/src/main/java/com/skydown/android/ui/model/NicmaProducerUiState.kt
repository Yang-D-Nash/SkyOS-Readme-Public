package com.skydown.android.ui.model

import android.net.Uri

data class NicmaSelectedBeatFile(
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
)

data class NicmaBeatHubItem(
    val id: String,
    val title: String,
    val artistName: String,
    val fileName: String,
    val downloadUrl: String,
    val notes: String,
    val uploaderName: String,
    val uploaderEmail: String,
    val uploaderId: String,
    val mimeType: String,
    val storagePath: String,
    val isPublic: Boolean,
    val createdAtMillis: Long,
) {
    val isPlayable: Boolean
        get() = mimeType.startsWith("audio/")
}

data class NicmaProducerUiState(
    val beatTitle: String = "",
    val artistName: String = "",
    val email: String = "",
    val notes: String = "",
    val selectedFiles: List<NicmaSelectedBeatFile> = emptyList(),
    val beats: List<NicmaBeatHubItem> = emptyList(),
    val isLoadingBeats: Boolean = true,
    val isUploading: Boolean = false,
    val isAdmin: Boolean = false,
    val validationMessage: String? = null,
    val feedbackMessage: String? = null,
    val feedbackIsError: Boolean = false,
)
