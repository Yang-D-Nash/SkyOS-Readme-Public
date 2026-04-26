package com.nash.skyos.ui.model

import android.net.Uri
import com.nash.skyos.data.ExternalMediaProvider
import com.nash.skyos.data.resolveYouTubeVideoId

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
        get() = downloadUrl.takeIf { it.isNotBlank() }
            ?: externalUrl.takeIf(::isDirectVideoPlaybackUrl)
            ?: ""

    val openUrl: String
        get() = externalUrl.takeIf { it.isNotBlank() } ?: nativePlaybackUrl

    val inAppOriginalUrl: String
        get() = openUrl.takeIf { it.isNotBlank() } ?: inlineEmbedUrl.takeIf { it.isNotBlank() }.orEmpty()

    val isYouTubeSource: Boolean
        get() = resolveYouTubeVideoId(embedUrl.takeIf { it.isNotBlank() } ?: externalUrl) != null

    val inlineEmbedUrl: String
        get() = if (isYouTubeSource) {
            ""
        } else {
            embedUrl.takeIf { it.isNotBlank() }.orEmpty()
        }

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

    val opensOriginalInApp: Boolean
        get() = inAppOriginalUrl.isNotBlank()

    val originalActionLabel: String
        get() = provider.originalVideoActionLabel

    val directOpenActionLabel: String
        get() = originalActionLabel

    val originalDestinationDescription: String
        get() = when {
            inAppOriginalUrl.isBlank() -> "Kein Original-Link verfuegbar."
            nativePlaybackUrl.isNotBlank() -> "Dieser Clip startet direkt in der In-App-Ansicht."
            else -> "Dieser Link startet in einer In-App-Webansicht mit Zurueck und Schliessen."
        }

    val isPlayable: Boolean
        get() = nativePlaybackUrl.isNotBlank() &&
            (mimeType.startsWith("video/") || isDirectVideoPlaybackUrl(nativePlaybackUrl))
}

private fun isDirectVideoPlaybackUrl(rawValue: String): Boolean {
    val normalizedPath = runCatching {
        Uri.parse(rawValue.trim()).path.orEmpty().lowercase()
    }.getOrDefault("")
    return normalizedPath.endsWith(".mp4") ||
        normalizedPath.endsWith(".mov") ||
        normalizedPath.endsWith(".m4v") ||
        normalizedPath.endsWith(".webm") ||
        normalizedPath.endsWith(".m3u8")
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
