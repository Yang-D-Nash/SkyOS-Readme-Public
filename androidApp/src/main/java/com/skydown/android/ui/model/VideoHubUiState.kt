package com.skydown.android.ui.model

import android.net.Uri

data class SelectedVideoFile(
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
)

data class VideoHubItem(
    val id: String,
    val title: String,
    val projectName: String,
    val fileName: String,
    val downloadUrl: String,
    val notes: String,
    val uploaderName: String,
    val uploaderEmail: String,
    val uploaderId: String,
    val mimeType: String,
    val storagePath: String,
    val isPublic: Boolean,
    val isHomeFeatured: Boolean,
    val createdAtMillis: Long,
) {
    val isPlayable: Boolean
        get() = mimeType.startsWith("video/") ||
            fileName.lowercase().endsWith(".mp4") ||
            fileName.lowercase().endsWith(".mov") ||
            fileName.lowercase().endsWith(".m4v")
}

data class VideoHubUiState(
    val videoTitle: String = "",
    val projectName: String = "",
    val email: String = "",
    val notes: String = "",
    val selectedFiles: List<SelectedVideoFile> = emptyList(),
    val videos: List<VideoHubItem> = emptyList(),
    val isLoadingVideos: Boolean = true,
    val isUploading: Boolean = false,
    val isAdmin: Boolean = false,
    val validationMessage: String? = null,
    val feedbackMessage: String? = null,
    val feedbackIsError: Boolean = false,
)
