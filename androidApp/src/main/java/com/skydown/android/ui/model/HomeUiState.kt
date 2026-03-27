package com.skydown.android.ui.model

import com.skydown.shared.model.Track

data class FeaturedBeatHighlight(
    val id: String,
    val title: String,
    val artistName: String,
    val notes: String,
    val downloadUrl: String,
    val isPlayable: Boolean,
)

data class FeaturedVideoHighlight(
    val title: String,
    val projectName: String,
    val notes: String,
    val downloadUrl: String,
)

data class HomeUiState(
    val featuredTrack: Track? = null,
    val featuredBeat: FeaturedBeatHighlight? = null,
    val featuredVideo: FeaturedVideoHighlight? = null,
    val homeTrackMessage: String? = null,
    val homeBeatMessage: String? = null,
    val homeVideoMessage: String? = null,
)
