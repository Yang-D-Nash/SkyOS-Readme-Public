package com.skydown.android.ui.model

import com.skydown.shared.model.Track

data class FeaturedVideoHighlight(
    val title: String,
    val projectName: String,
    val notes: String,
)

data class HomeUiState(
    val featuredTrack: Track? = null,
    val featuredVideo: FeaturedVideoHighlight? = null,
    val homeTrackMessage: String? = null,
    val homeVideoMessage: String? = null,
)
