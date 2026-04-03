package com.skydown.android.ui.model

import android.net.Uri
import com.skydown.android.data.ExternalMediaProvider

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
    val sourceProvider: String = ExternalMediaProvider.FIREBASE_STORAGE.rawValue,
    val externalUrl: String = "",
    val sourceFileId: String = "",
    val createdAtMillis: Long,
) {
    val provider: ExternalMediaProvider
        get() = ExternalMediaProvider.from(sourceProvider)

    val openUrl: String
        get() = externalUrl.takeIf { it.isNotBlank() } ?: downloadUrl

    val isPlayable: Boolean
        get() = downloadUrl.isNotBlank() && (
            mimeType.startsWith("audio/") ||
                fileName.lowercase().endsWith(".mp3") ||
                fileName.lowercase().endsWith(".wav") ||
                fileName.lowercase().endsWith(".m4a") ||
                fileName.lowercase().endsWith(".aac") ||
                fileName.lowercase().endsWith(".flac")
            )
}

data class NicmaProducerUiState(
    val beatTitle: String = "",
    val artistName: String = "",
    val email: String = "",
    val notes: String = "",
    val externalBeatUrl: String = "",
    val selectedFiles: List<NicmaSelectedBeatFile> = emptyList(),
    val beats: List<NicmaBeatHubItem> = emptyList(),
    val isLoadingBeats: Boolean = true,
    val isUploading: Boolean = false,
    val isAdmin: Boolean = false,
    val validationMessage: String? = null,
    val feedbackMessage: String? = null,
    val feedbackIsError: Boolean = false,
)
