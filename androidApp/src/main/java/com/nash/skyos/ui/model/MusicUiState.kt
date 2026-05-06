package com.nash.skyos.ui.model

import com.nash.skyos.R
import com.nash.skyos.data.AppTextResolver
import com.skydown.shared.model.Track
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    val selectedArtist: String = "Janno",
    val availableArtists: List<String> = defaultZweizweiMusicArtists,
    val tracks: List<Track> = emptyList(),
    val isSpotifyConnected: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedArtistSocialProfile: ArtistSocialProfile?
        get() = artistSocialProfiles[musicArtistKey(selectedArtist)]

    val instagramHubDestinations: List<MusicInstagramDestination>
        get() = buildList {
            addAll(
                availableArtists.map { artist ->
                    artistSocialProfiles[musicArtistKey(artist)]?.let { profile ->
                        MusicInstagramDestination(
                            title = profile.artist,
                            subtitle = AppTextResolver.string(
                                R.string.music_instagram_artist_subtitle,
                                profile.handle,
                            ),
                            instagramUrl = profile.instagramUrl,
                        )
                    } ?: MusicInstagramDestination(
                        title = artist,
                        subtitle = artist,
                        instagramUrl = instagramSearchUrlForArtist(artist),
                    )
                },
            )
        }
}

private val artistSocialProfiles = mapOf(
    musicArtistKey("Yang D. Nash") to ArtistSocialProfile(
        artist = "Yang D. Nash",
        handle = "@y.d.nash",
        instagramUrl = "https://www.instagram.com/y.d.nash/",
    ),
    musicArtistKey("Janno") to ArtistSocialProfile(
        artist = "Janno",
        handle = "@janno_official_",
        instagramUrl = "https://www.instagram.com/janno_official_/",
    ),
    musicArtistKey("Mave") to ArtistSocialProfile(
        artist = "Mave",
        handle = "@mave040_official",
        instagramUrl = "https://www.instagram.com/mave040_official/",
    ),
    musicArtistKey("Tangajoe007") to ArtistSocialProfile(
        artist = "Tangajoe007",
        handle = "@tangajoe007",
        instagramUrl = "https://www.instagram.com/tangajoe007/",
    ),
    musicArtistKey("ThaDude") to ArtistSocialProfile(
        artist = "ThaDude",
        handle = "@thadude_offizielle",
        instagramUrl = "https://www.instagram.com/thadude_offizielle/",
    ),
)

private fun instagramSearchUrlForArtist(artist: String): String {
    val encoded = URLEncoder.encode(artist, StandardCharsets.UTF_8.name())
    return "https://www.instagram.com/explore/search/keyword/?q=$encoded"
}
