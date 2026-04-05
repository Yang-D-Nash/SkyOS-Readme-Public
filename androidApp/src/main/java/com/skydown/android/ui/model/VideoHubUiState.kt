package com.skydown.android.ui.model

import android.net.Uri
import com.skydown.android.data.ExternalMediaProvider
import com.skydown.android.data.resolveYouTubeEmbedUrl

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
    val sourceProvider: String = ExternalMediaProvider.FIREBASE_STORAGE.rawValue,
    val externalUrl: String = "",
    val embedUrl: String = "",
    val sourceFileId: String = "",
    val createdAtMillis: Long,
) {
    val provider: ExternalMediaProvider
        get() = ExternalMediaProvider.from(sourceProvider)

    val nativePlaybackUrl: String
        get() = downloadUrl.takeIf { it.isNotBlank() }.orEmpty()

    val openUrl: String
        get() = externalUrl.takeIf { it.isNotBlank() } ?: nativePlaybackUrl

    val inlineEmbedUrl: String
        get() = embedUrl.takeIf { it.isNotBlank() }
            ?: resolveYouTubeEmbedUrl(externalUrl).orEmpty()

    val usesEmbeddedPreview: Boolean
        get() = provider != ExternalMediaProvider.FIREBASE_STORAGE &&
            inlineEmbedUrl.isNotBlank() &&
            nativePlaybackUrl.isBlank()

    val supportsInlinePlayback: Boolean
        get() = usesEmbeddedPreview || isPlayable

    val providerBadge: String
        get() = when (provider) {
            ExternalMediaProvider.FIREBASE_STORAGE -> "Storage"
            ExternalMediaProvider.GOOGLE_DRIVE -> "Drive"
            ExternalMediaProvider.MEGA -> "MEGA"
            ExternalMediaProvider.EXTERNAL_LINK -> "Extern"
        }

    val isPlayable: Boolean
        get() = nativePlaybackUrl.isNotBlank() && (
            mimeType.startsWith("video/") ||
            fileName.lowercase().endsWith(".mp4") ||
            fileName.lowercase().endsWith(".mov") ||
            fileName.lowercase().endsWith(".m4v")
        )
}

data class VideoHubUiState(
    val videoTitle: String = "",
    val projectName: String = "",
    val email: String = "",
    val notes: String = "",
    val externalVideoUrl: String = "",
    val publicConfig: VideoHubPublicConfig = VideoHubPublicConfig.default(),
    val selectedFiles: List<SelectedVideoFile> = emptyList(),
    val videos: List<VideoHubItem> = emptyList(),
    val isLoadingVideos: Boolean = true,
    val isUploading: Boolean = false,
    val isSavingPublicConfig: Boolean = false,
    val isAdmin: Boolean = false,
    val validationMessage: String? = null,
    val feedbackMessage: String? = null,
    val feedbackIsError: Boolean = false,
)
