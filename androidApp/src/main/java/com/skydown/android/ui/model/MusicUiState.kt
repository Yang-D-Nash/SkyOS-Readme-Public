package com.skydown.android.ui.model

import com.skydown.shared.model.Track

data class ArtistSocialProfile(
    val artist: String,
    val handle: String,
    val instagramUrl: String,
)

data class MusicInstagramDestination(
    val title: String,
    val subtitle: String,
    val instagramUrl: String,
)

data class MusicUiState(
    val selectedArtist: String = "JANNO",
    val availableArtists: List<String> = listOf(
        "JANNO",
        "Yang D. Nash",
        "ThaDude",
        "MAVE",
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

    val instagramHubDestinations: List<MusicInstagramDestination>
        get() = buildList {
            add(
                MusicInstagramDestination(
                    title = "Zweizwei Music",
                    subtitle = "@zweizwei_music • Label und Releases",
                    instagramUrl = "https://www.instagram.com/zweizwei_music/",
                ),
            )
            addAll(
                availableArtists.mapNotNull { artist ->
                    artistSocialProfiles[artist]?.let { profile ->
                        MusicInstagramDestination(
                            title = profile.artist,
                            subtitle = "${profile.handle} • Artist",
                            instagramUrl = profile.instagramUrl,
                        )
                    }
                },
            )
        }
}

private val artistSocialProfiles = mapOf(
    "Yang D. Nash" to ArtistSocialProfile(
        artist = "Yang D. Nash",
        handle = "@y.d.nash",
        instagramUrl = "https://www.instagram.com/y.d.nash/",
    ),
    "ThaDude" to ArtistSocialProfile(
        artist = "ThaDude",
        handle = "@thadude_offizielle",
        instagramUrl = "https://www.instagram.com/thadude_offizielle/",
    ),
    "MAVE" to ArtistSocialProfile(
        artist = "MAVE",
        handle = "@mave__official",
        instagramUrl = "https://www.instagram.com/mave__official/",
    ),
    "JANNO" to ArtistSocialProfile(
        artist = "JANNO",
        handle = "@janno_official_",
        instagramUrl = "https://www.instagram.com/janno_official_/",
    ),
    "TANGAJOE007" to ArtistSocialProfile(
        artist = "TANGAJOE007",
        handle = "@tangajoe007",
        instagramUrl = "https://www.instagram.com/tangajoe007/",
    ),
)
