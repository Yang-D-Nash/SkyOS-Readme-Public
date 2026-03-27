package com.skydown.android.data.repository

import android.net.Uri
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
    private val searchPageSize = 10
    private val searchMaxResults = 50
    private val artistAlbumsPageSize = 10
    private val artistAlbumsMaxResults = 100
    private val albumTracksPageSize = 50
    private val catalogPageSize = 25

    override suspend fun fetchTracks(artist: String): Result<List<Track>> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val accessToken = SpotifyAuthManager.validAccessToken()
                if (accessToken.isNullOrBlank()) {
                    return@withContext fetchCatalogTracks(artist)
                }

                runCatching {
                    fetchSpotifyTracks(
                        accessToken = accessToken,
                        artist = artist,
                    )
                }.getOrElse {
                    fetchCatalogTracks(artist)
                }
            }
        }
    }

    private fun fetchSpotifyTracks(
        accessToken: String,
        artist: String,
    ): List<Track> {
        val artistId = artistIds[artist]

        if (artistId != null) {
            val directTracks = fetchKnownArtistTracks(
                accessToken = accessToken,
                artist = artist,
                artistId = artistId,
            )
            if (directTracks.isNotEmpty()) {
                return directTracks
            }
        }

        val queries = searchQueriesFor(artist)
        var lastErrorMessage: String? = null

        for ((index, query) in queries.withIndex()) {
            val searchResult = performSearch(
                accessToken = accessToken,
                query = query,
            )

            if (searchResult.errorMessage == null) {
                return searchResult.items
                    .map { track ->
                        val matchingArtist = if (artistId != null) {
                            track.artists.firstOrNull { it.id == artistId }
                        } else {
                            track.artists.firstOrNull()
                        }
                        Track(
                            trackId = kotlin.math.abs(track.id.hashCode()),
                            artistId = kotlin.math.abs((matchingArtist?.id ?: track.artists.firstOrNull()?.id ?: track.id).hashCode()),
                            spotifyArtistId = matchingArtist?.id ?: track.artists.firstOrNull()?.id,
                            artistName = matchingArtist?.name ?: track.artists.firstOrNull()?.name,
                            trackName = track.name,
                            collectionName = track.album.name,
                            artworkUrl100 = track.album.images.firstOrNull()?.url,
                            previewUrl = track.previewUrl,
                            externalUrl = track.externalUrls["spotify"],
                            wrapperType = "track",
                            releaseDate = track.album.releaseDate,
                        )
                    }
                    .filter { track ->
                        if (artistId != null) {
                            track.spotifyArtistId == artistId
                        } else {
                            artistMatches(expectedArtist = artist, actualArtist = track.artistName)
                        }
                    }
            }

            lastErrorMessage = searchResult.errorMessage

            if (searchResult.responseCode == 400 && index < queries.lastIndex) {
                continue
            }

            error(lastErrorMessage)
        }

        error(lastErrorMessage ?: "Tracks konnten gerade nicht geladen werden.")
    }

    private fun fetchCatalogTracks(artist: String): List<Track> {
        val response = performCatalogRequest(buildCatalogSearchUrl(artist))
        if (response.responseCode !in 200..299) {
            error("Tracks konnten gerade nicht geladen werden.")
        }

        return json.decodeFromString(CatalogSearchResponse.serializer(), response.payload).results
            .filter { result ->
                artistMatches(expectedArtist = artist, actualArtist = result.artistName)
            }
            .sortedByDescending { it.releaseDate.orEmpty() }
            .map { result ->
                Track(
                    trackId = result.trackId ?: kotlin.math.abs("${result.artistName}-${result.trackName}".hashCode()),
                    artistId = result.artistId ?: kotlin.math.abs(result.artistName.hashCode()),
                    spotifyArtistId = null,
                    artistName = result.artistName,
                    trackName = result.trackName,
                    collectionName = result.collectionName,
                    artworkUrl100 = result.artworkUrl100,
                    previewUrl = result.previewUrl,
                    externalUrl = buildSpotifySearchUrl(result.artistName, result.trackName),
                    wrapperType = result.wrapperType ?: result.kind,
                    releaseDate = result.releaseDate,
                )
            }
    }

    private fun fetchKnownArtistTracks(
        accessToken: String,
        artist: String,
        artistId: String,
    ): List<Track> {
        val albums = fetchArtistAlbums(
            accessToken = accessToken,
            artistId = artistId,
        )
        val tracksById = linkedMapOf<String, Track>()

        for (album in albums) {
            val albumTracks = fetchAlbumTracks(
                accessToken = accessToken,
                album = album,
                artistId = artistId,
            )

            for (track in albumTracks) {
                tracksById.putIfAbsent(
                    track.id,
                    Track(
                        trackId = kotlin.math.abs(track.id.hashCode()),
                        artistId = kotlin.math.abs(artistId.hashCode()),
                        spotifyArtistId = artistId,
                        artistName = track.artists.firstOrNull { it.id == artistId }?.name ?: artist,
                        trackName = track.name,
                        collectionName = album.name,
                        artworkUrl100 = album.images.firstOrNull()?.url,
                        previewUrl = track.previewUrl,
                        externalUrl = track.externalUrls["spotify"],
                        wrapperType = "track",
                        releaseDate = album.releaseDate,
                    ),
                )
            }
        }

        return tracksById.values.toList()
    }

    private fun fetchArtistAlbums(
        accessToken: String,
        artistId: String,
    ): List<SpotifyArtistAlbum> {
        val albums = linkedMapOf<String, SpotifyArtistAlbum>()

        for (offset in 0 until artistAlbumsMaxResults step artistAlbumsPageSize) {
            val response = performGetRequest(
                accessToken = accessToken,
                url = buildArtistAlbumsUrl(artistId, offset),
            )

            if (response.responseCode !in 200..299) {
                println("Spotify Artist Albums Error: ${response.responseCode} ${response.payload}")
                error("Spotify API Fehler ${response.responseCode}: ${response.payload}")
            }

            val page = json.decodeFromString(SpotifyArtistAlbumsResponse.serializer(), response.payload).items
            page.forEach { album -> albums.putIfAbsent(album.id, album) }

            if (page.size < artistAlbumsPageSize) {
                break
            }
        }

        return albums.values
            .sortedByDescending { it.releaseDate.orEmpty() }
    }

    private fun fetchAlbumTracks(
        accessToken: String,
        album: SpotifyArtistAlbum,
        artistId: String,
    ): List<SpotifyAlbumTrack> {
        val tracks = mutableListOf<SpotifyAlbumTrack>()

        for (offset in 0 until album.totalTracks step albumTracksPageSize) {
            val response = performGetRequest(
                accessToken = accessToken,
                url = buildAlbumTracksUrl(album.id, offset),
            )

            if (response.responseCode !in 200..299) {
                println("Spotify Album Tracks Error: ${response.responseCode} ${response.payload}")
                error("Spotify API Fehler ${response.responseCode}: ${response.payload}")
            }

            val page = json.decodeFromString(SpotifyAlbumTracksResponse.serializer(), response.payload).items
            tracks += page.filter { track -> track.artists.any { it.id == artistId } }

            if (page.size < albumTracksPageSize) {
                break
            }
        }

        return tracks
    }

    private fun performSearch(
        accessToken: String,
        query: String,
    ): SearchResult {
        val tracks = mutableListOf<SpotifyTrack>()

        for (offset in 0 until searchMaxResults step searchPageSize) {
            val response = performSearchRequest(
                accessToken = accessToken,
                query = query,
                offset = offset,
            )

            if (response.responseCode !in 200..299) {
                println("Spotify Search Error: ${response.responseCode} ${response.payload}")
                return SearchResult(
                    items = emptyList(),
                    responseCode = response.responseCode,
                    errorMessage = "Spotify API Fehler ${response.responseCode}: ${response.payload}",
                )
            }

            val page = json.decodeFromString(SpotifySearchResponse.serializer(), response.payload).tracks.items
            tracks += page

            if (page.size < searchPageSize) {
                break
            }
        }

        return SearchResult(
            items = tracks,
            responseCode = 200,
            errorMessage = null,
        )
    }

    private fun performSearchRequest(
        accessToken: String,
        query: String,
        offset: Int,
    ): SearchHttpResponse {
        return performGetRequest(
            accessToken = accessToken,
            url = buildSearchUrl(query, offset),
        )
    }

    private fun performGetRequest(
        accessToken: String,
        url: URL,
    ): SearchHttpResponse {
        val connection = (url
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val responseCode = connection.responseCode
            val stream = when {
                responseCode in 200..299 -> connection.inputStream
                connection.errorStream != null -> connection.errorStream
                else -> connection.inputStream
            }
            val payload = stream.bufferedReader().use { it.readText() }
            SearchHttpResponse(
                responseCode = responseCode,
                payload = payload,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun performCatalogRequest(url: URL): SearchHttpResponse {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val responseCode = connection.responseCode
            val stream = when {
                responseCode in 200..299 -> connection.inputStream
                connection.errorStream != null -> connection.errorStream
                else -> connection.inputStream
            }
            val payload = stream.bufferedReader().use { it.readText() }
            SearchHttpResponse(
                responseCode = responseCode,
                payload = payload,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun buildSearchUrl(query: String, offset: Int): URL {
        val uri = Uri.parse("https://api.spotify.com/v1/search")
            .buildUpon()
            .appendQueryParameter("q", query)
            .appendQueryParameter("type", "track")
            .appendQueryParameter("limit", searchPageSize.toString())
            .appendQueryParameter("offset", offset.toString())
            .build()
        return URL(uri.toString())
    }

    private fun buildArtistAlbumsUrl(artistId: String, offset: Int): URL {
        val uri = Uri.parse("https://api.spotify.com/v1/artists/$artistId/albums")
            .buildUpon()
            .appendQueryParameter("include_groups", "album,single,appears_on,compilation")
            .appendQueryParameter("limit", artistAlbumsPageSize.toString())
            .appendQueryParameter("offset", offset.toString())
            .build()
        return URL(uri.toString())
    }

    private fun buildAlbumTracksUrl(albumId: String, offset: Int): URL {
        val uri = Uri.parse("https://api.spotify.com/v1/albums/$albumId/tracks")
            .buildUpon()
            .appendQueryParameter("limit", albumTracksPageSize.toString())
            .appendQueryParameter("offset", offset.toString())
            .build()
        return URL(uri.toString())
    }

    private fun buildCatalogSearchUrl(artist: String): URL {
        val uri = Uri.parse("https://itunes.apple.com/search")
            .buildUpon()
            .appendQueryParameter("term", artist)
            .appendQueryParameter("media", "music")
            .appendQueryParameter("entity", "song")
            .appendQueryParameter("limit", catalogPageSize.toString())
            .build()
        return URL(uri.toString())
    }

    private fun buildSpotifySearchUrl(artist: String, trackName: String): String {
        val query = listOf(artist, trackName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return Uri.parse("https://open.spotify.com/search")
            .buildUpon()
            .appendPath(query)
            .build()
            .toString()
    }

    private fun searchQueriesFor(artist: String): List<String> {
        return listOf(
            "artist:\"$artist\"",
            artist,
        ).distinct()
    }

    private fun artistMatches(expectedArtist: String, actualArtist: String?): Boolean {
        if (actualArtist.isNullOrBlank()) return false
        val expected = expectedArtist.lowercase()
        val actual = actualArtist.lowercase()
        return actual == expected || actual.contains(expected) || expected.contains(actual)
    }

    private val artistIds = mapOf(
        "Yang D. Nash" to "63Sh0kQAWW3ZWn2aKDksbo",
        "ThaDude" to "0Jmb7DXFkKxxRjqD70vi0e",
        "MAVE" to "0GXymtRaIk2ngbXSkcHtsp",
        "JANNO" to "7hpiHzP9aLLb5liDLxtwhM",
        "TANGAJOE007" to "0OA5dgpVdwzI8K82m8FPxN",
        "Toprack941" to "4CoozMQ3B3I20day60N7QA",
    )

    private data class SearchHttpResponse(
        val responseCode: Int,
        val payload: String,
    )

    private data class SearchResult(
        val items: List<SpotifyTrack>,
        val responseCode: Int,
        val errorMessage: String?,
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
    val id: String? = null,
    val name: String,
    val images: List<SpotifyImage> = emptyList(),
    @SerialName("total_tracks") val totalTracks: Int = 0,
    @SerialName("release_date") val releaseDate: String? = null,
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

@Serializable
private data class SpotifyArtistAlbumsResponse(
    val items: List<SpotifyArtistAlbum>,
)

@Serializable
private data class SpotifyArtistAlbum(
    val id: String,
    val name: String,
    val images: List<SpotifyImage> = emptyList(),
    @SerialName("total_tracks") val totalTracks: Int = 0,
    @SerialName("release_date") val releaseDate: String? = null,
)

@Serializable
private data class SpotifyAlbumTracksResponse(
    val items: List<SpotifyAlbumTrack>,
)

@Serializable
private data class SpotifyAlbumTrack(
    val id: String,
    val name: String,
    @SerialName("preview_url") val previewUrl: String? = null,
    val artists: List<SpotifyArtist>,
    @SerialName("external_urls") val externalUrls: Map<String, String> = emptyMap(),
)

@Serializable
private data class CatalogSearchResponse(
    val results: List<CatalogTrack> = emptyList(),
)

@Serializable
private data class CatalogTrack(
    val trackId: Int? = null,
    val artistId: Int? = null,
    val artistName: String = "",
    val trackName: String = "",
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val wrapperType: String? = null,
    val kind: String? = null,
    val releaseDate: String? = null,
)
