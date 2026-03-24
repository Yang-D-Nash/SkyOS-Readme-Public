package com.skydown.android.ui.model

import com.skydown.shared.model.Track

data class MusicUiState(
    val selectedArtist: String = "Yang D. Nash",
    val availableArtists: List<String> = listOf("Yang D. Nash", "ThaDude"),
    val tracks: List<Track> = emptyList(),
    val currentlyPlayingId: Int? = null,
)
