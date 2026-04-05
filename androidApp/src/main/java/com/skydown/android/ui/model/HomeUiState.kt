package com.skydown.android.ui.model

import com.skydown.android.data.ExternalMediaProvider
import com.skydown.android.data.resolveYouTubeEmbedUrl
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

    val inlineEmbedUrl: String
        get() = embedUrl.takeIf { it.isNotBlank() }
            ?: resolveYouTubeEmbedUrl(externalUrl).orEmpty()

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
