package com.skydown.android.ui.model

import com.skydown.shared.model.Track

data class ArtistSocialProfile(
    val handle: String,
    val instagramUrl: String,
)

data class MusicUiState(
    val selectedArtist: String = "Yang D. Nash",
    val availableArtists: List<String> = listOf(
        "Yang D. Nash",
        "ThaDude",
        "MAVE",
        "JANNO",
        "TANGAJOE007",
    ),
    val tracks: List<Track> = emptyList(),
    val currentlyPlayingId: Int? = null,
    val currentPreviewUrl: String? = null,
    val isSpotifyConnected: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedArtistSocialProfile: ArtistSocialProfile?
        get() = artistSocialProfiles[selectedArtist]
}

private val artistSocialProfiles = mapOf(
    "Yang D. Nash" to ArtistSocialProfile(
        handle = "@y.d.nash",
        instagramUrl = "https://www.instagram.com/y.d.nash/",
    ),
    "ThaDude" to ArtistSocialProfile(
        handle = "@thadude_offizielle",
        instagramUrl = "https://www.instagram.com/thadude_offizielle/",
    ),
    "MAVE" to ArtistSocialProfile(
        handle = "@mave__official",
        instagramUrl = "https://www.instagram.com/mave__official/",
    ),
    "JANNO" to ArtistSocialProfile(
        handle = "@janno_official_",
        instagramUrl = "https://www.instagram.com/janno_official_/",
    ),
    "TANGAJOE007" to ArtistSocialProfile(
        handle = "@tangajoe007",
        instagramUrl = "https://www.instagram.com/tangajoe007/",
    ),
)
