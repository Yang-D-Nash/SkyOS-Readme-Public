package com.skydown.android.ui.model

import com.skydown.android.data.ExternalMediaProvider
import com.skydown.android.data.resolveYouTubeVideoId
import com.skydown.shared.model.Track

data class FeaturedBeatHighlight(
    val id: String,
    val title: String,
    val artistName: String,
    val notes: String,
    val downloadUrl: String,
    val externalUrl: String,
    val sourceProvider: String,
    val isPlayable: Boolean,
) {
    val provider: ExternalMediaProvider
        get() = ExternalMediaProvider.from(sourceProvider)

    val openUrl: String
        get() = externalUrl.ifBlank { downloadUrl }
}

data class FeaturedVideoHighlight(
    val id: String,
    val title: String,
    val projectName: String,
    val notes: String,
    val downloadUrl: String,
    val externalUrl: String,
    val embedUrl: String,
    val sourceProvider: String,
) {
    val provider: ExternalMediaProvider
        get() = ExternalMediaProvider.from(sourceProvider)

    val usesEmbeddedPreview: Boolean
        get() = provider != ExternalMediaProvider.FIREBASE_STORAGE &&
            inlineEmbedUrl.isNotBlank() &&
            downloadUrl.isBlank()

    val supportsInlinePlayback: Boolean
        get() = usesEmbeddedPreview || downloadUrl.isNotBlank()

    val isYouTubeSource: Boolean
        get() = resolveYouTubeVideoId(embedUrl.ifBlank { externalUrl }) != null

    val inlineEmbedUrl: String
        get() = if (isYouTubeSource) {
            ""
        } else {
            embedUrl.takeIf { it.isNotBlank() }.orEmpty()
        }

    val openUrl: String
        get() = externalUrl.ifBlank { downloadUrl }
}

data class HomeUiState(
    val featuredTrack: Track? = null,
    val featuredBeat: FeaturedBeatHighlight? = null,
    val featuredVideo: FeaturedVideoHighlight? = null,
    val homeTrackMessage: String? = null,
    val homeBeatMessage: String? = null,
    val homeVideoMessage: String? = null,
)
