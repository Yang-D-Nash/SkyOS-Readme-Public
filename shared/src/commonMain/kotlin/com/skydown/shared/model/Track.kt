package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val trackId: Int,
    val artistId: Int,
    val spotifyArtistId: String? = null,
    val artistName: String? = null,
    val trackName: String,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val externalUrl: String? = null,
    val wrapperType: String? = null,
)

@Serializable
data class SearchResult(
    val results: List<Track>,
)

fun sampleTracks(): List<Track> = listOf(
    Track(
        trackId = 1,
        artistId = 1637910017,
        spotifyArtistId = "sample-artist-id-1",
        artistName = "Yang D. Nash",
        trackName = "Skyline Dreams",
        collectionName = "Skydown Sessions",
        artworkUrl100 = "https://via.placeholder.com/200x200.png?text=Skyline+Dreams",
        previewUrl = "preview://skyline-dreams",
        externalUrl = "https://open.spotify.com",
        wrapperType = "track",
    ),
    Track(
        trackId = 2,
        artistId = 1637910017,
        spotifyArtistId = "sample-artist-id-1",
        artistName = "Yang D. Nash",
        trackName = "Late Night Echo",
        collectionName = "Skydown Sessions",
        artworkUrl100 = "https://via.placeholder.com/200x200.png?text=Late+Night+Echo",
        previewUrl = "preview://late-night-echo",
        externalUrl = "https://open.spotify.com",
        wrapperType = "track",
    ),
    Track(
        trackId = 3,
        artistId = 1677936430,
        spotifyArtistId = "sample-artist-id-2",
        artistName = "ThaDude",
        trackName = "ThaDude Intro",
        collectionName = "Street Wave",
        artworkUrl100 = "https://via.placeholder.com/200x200.png?text=ThaDude+Intro",
        previewUrl = "preview://thadude-intro",
        externalUrl = "https://open.spotify.com",
        wrapperType = "track",
    ),
)
