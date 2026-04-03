package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val trackId: Int,
    val artistId: Int,
    val spotifyArtistId: String? = null,
    val spotifyTrackId: String? = null,
    val artistName: String? = null,
    val trackName: String,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val externalUrl: String? = null,
    val wrapperType: String? = null,
    val releaseDate: String? = null,
)

@Serializable
data class SearchResult(
    val results: List<Track>,
)
