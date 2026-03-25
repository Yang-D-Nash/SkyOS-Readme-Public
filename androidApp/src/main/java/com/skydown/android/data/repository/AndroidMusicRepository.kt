package com.skydown.android.data.repository

import com.skydown.android.data.SpotifyAuthManager
import com.skydown.shared.model.Track
import com.skydown.shared.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class AndroidMusicRepository : MusicRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchTracks(artist: String): Result<List<Track>> {
        return runCatching {
            val accessToken = SpotifyAuthManager.validAccessToken()
                ?: error("Verbinde zuerst Spotify.")
            val artistId = artistIds[artist]

            withContext(Dispatchers.IO) {
                val query = java.net.URLEncoder.encode("artist:$artist", "UTF-8")
                val connection = (URL("https://api.spotify.com/v1/search?q=$query&type=track&limit=50")
                    .openConnection() as HttpURLConnection).apply {
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }

                val payload = (if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }).bufferedReader().use { it.readText() }

                if (connection.responseCode !in 200..299) {
                    println("Spotify Search Error: ${connection.responseCode} $payload")
                    error("Spotify API Fehler ${connection.responseCode}: $payload")
                }

                json.decodeFromString(SpotifySearchResponse.serializer(), payload)
                    .tracks
                    .items
                    .map { track ->
                        Track(
                            trackId = kotlin.math.abs(track.id.hashCode()),
                            artistId = kotlin.math.abs((track.artists.firstOrNull()?.id ?: track.id).hashCode()),
                            spotifyArtistId = track.artists.firstOrNull()?.id,
                            artistName = track.artists.firstOrNull()?.name,
                            trackName = track.name,
                            collectionName = track.album.name,
                            artworkUrl100 = track.album.images.firstOrNull()?.url,
                            previewUrl = track.previewUrl,
                            externalUrl = track.externalUrls["spotify"],
                            wrapperType = "track",
                        )
                    }
                    .filter { track ->
                        if (artistId != null) {
                            track.spotifyArtistId == artistId
                        } else {
                            track.artistName.equals(artist, ignoreCase = true)
                        }
                    }
            }
        }
    }

    private val artistIds = mapOf(
        "Yang D. Nash" to "63Sh0kQAWW3ZWn2aKDksbo",
        "ThaDude" to "0Jmb7DXFkKxxRjqD70vi0e",
        "MAVE" to "0GXymtRaIk2ngbXSkcHtsp",
        "JANNO" to "7hpiHzP9aLLb5liDLxtwhM",
        "TANGAJOE007" to "0OA5dgpVdwzI8K82m8FPxN",
    )
}

@Serializable
private data class SpotifySearchResponse(
    val tracks: SpotifyTracks,
)

@Serializable
private data class SpotifyTracks(
    val items: List<SpotifyTrack>,
)

@Serializable
private data class SpotifyTrack(
    val id: String,
    val name: String,
    @SerialName("preview_url") val previewUrl: String? = null,
    val album: SpotifyAlbum,
    val artists: List<SpotifyArtist>,
    @SerialName("external_urls") val externalUrls: Map<String, String>,
)

@Serializable
private data class SpotifyAlbum(
    val name: String,
    val images: List<SpotifyImage>,
)

@Serializable
private data class SpotifyArtist(
    val id: String,
    val name: String,
)

@Serializable
private data class SpotifyImage(
    val url: String,
)
