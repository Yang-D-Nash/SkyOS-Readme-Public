package com.skydown.android.ui.model

import com.skydown.shared.model.Track

data class MusicUiState(
    val selectedArtist: String = "Yang D. Nash",
    val availableArtists: List<String> = listOf(
        "Yang D. Nash",
        "ThaDude",
        "MAVE",
        "JANNO",
        "TANGAJOE007",
        "Toprack941",
    ),
    val tracks: List<Track> = emptyList(),
    val currentlyPlayingId: Int? = null,
    val currentPreviewUrl: String? = null,
    val isSpotifyConnected: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
