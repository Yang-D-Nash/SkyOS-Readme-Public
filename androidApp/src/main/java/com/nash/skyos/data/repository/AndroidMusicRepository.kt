package com.nash.skyos.data.repository

import android.net.Uri
import android.util.Log
import com.nash.skyos.R
import com.nash.skyos.data.AppTextResolver
import com.nash.skyos.data.SpotifyAuthManager
import com.skydown.shared.model.Track
import com.skydown.shared.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

class AndroidMusicRepository : MusicRepository {
    private val logTag = "AndroidMusicRepository"
    private val json = Json { ignoreUnknownKeys = true }
    private val searchPageSize = 10
    private val searchMaxResults = 50
    private val artistAlbumsPageSize = 10
    private val artistAlbumsMaxResults = 100
    private val albumTracksPageSize = 50
    private val catalogPageSize = 50
    private val publicFallbackTargetTrackCount = 6
    private val publicFallbackMaxAlbumPages = 8
    private val publicSpotifyUserAgent =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/135.0.0.0 Mobile Safari/537.36"
    private val publicSpotifyAcceptLanguage = "en-US,en;q=0.9,de;q=0.8"

    private data class PublicAlbumReference(
        val albumId: String,
        val releaseDate: String?,
    )

    override suspend fun fetchTracks(artist: String): Result<List<Track>> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val knownArtistId = artistIds[artist]
                if (knownArtistId != null) {
                    return@withContext fetchKnownArtistTracksWithFallbacks(
                        artist = artist,
                        artistId = knownArtistId,
                    )
                }

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

    private suspend fun fetchKnownArtistTracksWithFallbacks(
        artist: String,
        artistId: String,
    ): List<Track> {
        var publicTracks: List<Track>? = null
        var publicError: Throwable? = null

        try {
            publicTracks = fetchPublicSpotifyTracks(
                artist = artist,
                artistId = artistId,
            )
        } catch (error: Throwable) {
            publicError = error
        }

        var apiTracks: List<Track>? = null
        var apiError: Throwable? = null

        val accessToken = SpotifyAuthManager.validAccessToken()
        if (accessToken.isNullOrBlank()) {
            apiError = IllegalStateException(AppTextResolver.string(R.string.spotify_error_access_token_missing))
        } else {
            try {
                apiTracks = fetchKnownArtistTracks(
                    accessToken = accessToken,
                    artist = artist,
                    artistId = artistId,
                )
            } catch (error: Throwable) {
                apiError = error
            }
        }

        val mergedTracks = mergeKnownArtistTracks(
            publicTracks = publicTracks.orEmpty(),
            apiTracks = apiTracks.orEmpty(),
        )

        if (mergedTracks.isNotEmpty()) {
            return mergedTracks
        }

        if (publicTracks != null || apiTracks != null) {
            return emptyList()
        }

        throw publicError ?: apiError ?: IllegalStateException(AppTextResolver.string(R.string.music_error_tracks_load_failed))
    }

    private fun fetchSpotifyTracks(
        accessToken: String,
        artist: String,
    ): List<Track> {
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
                        val matchingArtist = track.artists.firstOrNull()
                        Track(
                            trackId = kotlin.math.abs(track.id.hashCode()),
                            artistId = kotlin.math.abs((matchingArtist?.id ?: track.artists.firstOrNull()?.id ?: track.id).hashCode()),
                            spotifyArtistId = matchingArtist?.id ?: track.artists.firstOrNull()?.id,
                            spotifyTrackId = track.id,
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
                        artistMatches(expectedArtist = artist, actualArtist = track.artistName)
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
        val knownArtistId = artistIds[artist]
        val resultsByKey = linkedMapOf<String, CatalogTrack>()

        catalogQueriesFor(artist).forEach { query ->
            val response = performCatalogRequest(
                buildCatalogSearchUrl(
                    artist = query.term,
                    attribute = query.attribute,
                ),
            )
            if (response.responseCode !in 200..299) {
                return@forEach
            }

            json.decodeFromString(CatalogSearchResponse.serializer(), response.payload).results
                .filter { result ->
                    artistMatches(expectedArtist = artist, actualArtist = result.artistName)
                }
                .forEach { result ->
                    resultsByKey.putIfAbsent(catalogTrackKey(result), result)
                }
        }

        if (resultsByKey.isEmpty()) {
            error(AppTextResolver.string(R.string.music_error_tracks_load_failed))
        }

        return resultsByKey.values
            .sortedByDescending { it.releaseDate.orEmpty() }
            .map { result ->
                Track(
                    trackId = result.trackId ?: kotlin.math.abs("${result.artistName}-${result.trackName}".hashCode()),
                    artistId = result.artistId ?: kotlin.math.abs(result.artistName.hashCode()),
                    spotifyArtistId = knownArtistId,
                    spotifyTrackId = null,
                    artistName = result.artistName,
                    trackName = result.trackName,
                    collectionName = result.collectionName,
                    artworkUrl100 = result.artworkUrl100,
                    previewUrl = result.previewUrl,
                    externalUrl = buildSpotifyArtistUrl(knownArtistId)
                        ?: buildSpotifySearchUrl(result.artistName, result.trackName),
                    wrapperType = result.wrapperType ?: result.kind,
                    releaseDate = result.releaseDate,
                )
            }
    }

    private fun fetchPublicSpotifyTracks(
        artist: String,
        artistId: String,
    ): List<Track> {
        val root = fetchPublicInitialStateRoot(
            url = buildPublicArtistUrl(artistId),
            pageErrorMessage = "Spotify Artist Seite konnte nicht geladen werden.",
            dataErrorMessage = "Spotify Artist Daten konnten nicht gelesen werden.",
        )
        val artistEntity = root
            .jsonObject("entities")
            ?.jsonObject("items")
            ?.jsonObject("spotify:artist:$artistId")
            ?: error(AppTextResolver.string(R.string.spotify_error_artist_data_missing))
        val discography = artistEntity.jsonObject("discography")
            ?: error(AppTextResolver.string(R.string.spotify_error_discography_missing))

        val releaseDatesByAlbumUri = mutableMapOf<String, String>()
        appendReleaseDates(
            listOfNotNull(discography["latest"]),
            releaseDatesByAlbumUri,
        )
        appendReleaseDates(
            discography.jsonObject("popularReleasesAlbums")?.jsonArray("items").orEmpty(),
            releaseDatesByAlbumUri,
        )
        appendReleaseDates(
            discography.jsonObject("singles")?.jsonArray("items").orEmpty(),
            releaseDatesByAlbumUri,
        )
        appendReleaseDates(
            discography.jsonObject("albums")?.jsonArray("items").orEmpty(),
            releaseDatesByAlbumUri,
        )
        appendReleaseDates(
            discography.jsonObject("compilations")?.jsonArray("items").orEmpty(),
            releaseDatesByAlbumUri,
        )

        val tracksById = linkedMapOf<String, Track>()
        val artistArtworkUrl = artistEntity
            .jsonObject("visuals")
            ?.jsonObject("avatarImage")
            ?.coverArtUrl()

        discography
            .jsonObject("topTracks")
            ?.jsonArray("items")
            .orEmpty()
            .forEach { item ->
                val track = publicArtistTrack(
                    item = item,
                    artist = artist,
                    artistId = artistId,
                    artistArtworkUrl = artistArtworkUrl,
                    releaseDatesByAlbumUri = releaseDatesByAlbumUri,
                ) ?: return@forEach
                tracksById.putIfAbsent(track.spotifyTrackId.orEmpty(), track)
            }

        if (tracksById.size < publicFallbackTargetTrackCount) {
            val albumReferences = publicAlbumReferences(
                artistEntity = artistEntity,
                discography = discography,
                releaseDatesByAlbumUri = releaseDatesByAlbumUri,
            )

            for (albumReference in albumReferences.take(publicFallbackMaxAlbumPages)) {
                val albumTracks = fetchPublicAlbumTracks(
                    artist = artist,
                    artistId = artistId,
                    albumReference = albumReference,
                )

                for (track in albumTracks) {
                    tracksById.putIfAbsent(track.spotifyTrackId.orEmpty(), track)
                }

                if (tracksById.size >= publicFallbackTargetTrackCount) {
                    break
                }
            }
        }

        return tracksById.values.toList()
    }

    private fun mergeKnownArtistTracks(
        publicTracks: List<Track>,
        apiTracks: List<Track>,
    ): List<Track> {
        val tracksByKey = linkedMapOf<String, Track>()

        (publicTracks + apiTracks).forEach { track ->
            val key = knownArtistTrackKey(track) ?: return@forEach
            tracksByKey.putIfAbsent(key, track)
        }

        return tracksByKey.values.toList()
    }

    private fun knownArtistTrackKey(track: Track): String? {
        if (!track.spotifyTrackId.isNullOrBlank()) {
            return "spotify-${track.spotifyTrackId}"
        }

        if (!track.externalUrl.isNullOrBlank()) {
            return "external-${track.externalUrl}"
        }

        val artistName = track.artistName?.takeIf { it.isNotBlank() } ?: return null
        return listOf(
            normalizeArtistName(artistName),
            track.trackName.lowercase(),
            track.releaseDate.orEmpty(),
        ).joinToString(separator = "|")
    }

    private fun publicArtistTrack(
        item: JsonElement,
        artist: String,
        artistId: String,
        artistArtworkUrl: String?,
        releaseDatesByAlbumUri: Map<String, String>,
    ): Track? {
        val track = item.jsonObjectOrNull()?.jsonObject("track") ?: return null
        val trackId = spotifyIdFromUri(track.string("uri")) ?: return null
        val trackName = track.string("name")?.trim().orEmpty()
        if (trackName.isBlank()) return null

        val trackArtists = track
            .jsonObject("artists")
            ?.jsonArray("items")
            .orEmpty()
        if (trackArtists.none { spotifyIdFromUri(it.jsonObjectOrNull()?.string("uri")) == artistId }) {
            return null
        }

        val matchingArtist = trackArtists.firstOrNull {
            spotifyIdFromUri(it.jsonObjectOrNull()?.string("uri")) == artistId
        }?.jsonObjectOrNull()
        val album = track.jsonObject("albumOfTrack")
        val albumUri = album?.string("uri")

        return Track(
            trackId = kotlin.math.abs(trackId.hashCode()),
            artistId = kotlin.math.abs(artistId.hashCode()),
            spotifyArtistId = artistId,
            spotifyTrackId = trackId,
            artistName = matchingArtist
                ?.jsonObject("profile")
                ?.string("name")
                ?: artist,
            trackName = trackName,
            collectionName = album?.string("name"),
            artworkUrl100 = album
                ?.jsonObject("coverArt")
                ?.coverArtUrl()
                ?: artistArtworkUrl,
            previewUrl = track
                .jsonObject("previews")
                ?.jsonObject("audioPreviews")
                ?.jsonArray("items")
                ?.firstOrNull()
                ?.jsonObjectOrNull()
                ?.string("url"),
            externalUrl = "https://open.spotify.com/track/$trackId",
            wrapperType = "track",
            releaseDate = albumUri?.let(releaseDatesByAlbumUri::get),
        )
    }

    private fun fetchPublicAlbumTracks(
        artist: String,
        artistId: String,
        albumReference: PublicAlbumReference,
    ): List<Track> {
        val root = fetchPublicInitialStateRoot(
            url = buildPublicAlbumUrl(albumReference.albumId),
            pageErrorMessage = "Spotify Album Seite konnte nicht geladen werden.",
            dataErrorMessage = "Spotify Album Daten konnten nicht gelesen werden.",
        )
        val albumEntity = root
            .jsonObject("entities")
            ?.jsonObject("items")
            ?.jsonObject("spotify:album:${albumReference.albumId}")
            ?: return emptyList()

        val releaseDate = albumEntity
            .jsonObject("date")
            ?.releaseDateString()
            ?: albumReference.releaseDate

        return albumEntity
            .jsonObject("tracksV2")
            ?.jsonArray("items")
            .orEmpty()
            .mapNotNull { item ->
                val track = item.jsonObjectOrNull()?.jsonObject("track") ?: return@mapNotNull null
                val trackId = track.string("id") ?: spotifyIdFromUri(track.string("uri")) ?: return@mapNotNull null
                val trackName = track.string("name")?.trim().orEmpty()
                if (trackName.isBlank()) return@mapNotNull null

                val trackArtists = track
                    .jsonObject("artists")
                    ?.jsonArray("items")
                    .orEmpty()
                if (trackArtists.none { spotifyIdFromUri(it.jsonObjectOrNull()?.string("uri")) == artistId }) {
                    return@mapNotNull null
                }

                val matchingArtist = trackArtists.firstOrNull {
                    spotifyIdFromUri(it.jsonObjectOrNull()?.string("uri")) == artistId
                }?.jsonObjectOrNull()

                Track(
                    trackId = kotlin.math.abs(trackId.hashCode()),
                    artistId = kotlin.math.abs(artistId.hashCode()),
                    spotifyArtistId = artistId,
                    spotifyTrackId = trackId,
                    artistName = matchingArtist
                        ?.jsonObject("profile")
                        ?.string("name")
                        ?: artist,
                    trackName = trackName,
                    collectionName = albumEntity.string("name"),
                    artworkUrl100 = albumEntity
                        .jsonObject("coverArt")
                        ?.coverArtUrl(),
                    previewUrl = track
                        .jsonObject("previews")
                        ?.jsonObject("audioPreviews")
                        ?.jsonArray("items")
                        ?.firstOrNull()
                        ?.jsonObjectOrNull()
                        ?.string("url"),
                    externalUrl = "https://open.spotify.com/track/$trackId",
                    wrapperType = "track",
                    releaseDate = releaseDate,
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
                        spotifyTrackId = track.id,
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
                Log.w(logTag, "Spotify artist albums request failed with code ${response.responseCode}.")
                error(
                    AppTextResolver.string(
                        R.string.spotify_error_api_with_code_payload,
                        response.responseCode,
                        response.payload,
                    ),
                )
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
                Log.w(logTag, "Spotify album tracks request failed with code ${response.responseCode}.")
                error(
                    AppTextResolver.string(
                        R.string.spotify_error_api_with_code_payload,
                        response.responseCode,
                        response.payload,
                    ),
                )
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
                Log.w(logTag, "Spotify search request failed with code ${response.responseCode}.")
                return SearchResult(
                    items = emptyList(),
                    responseCode = response.responseCode,
                    errorMessage = AppTextResolver.string(
                        R.string.spotify_error_api_with_code_payload,
                        response.responseCode,
                        response.payload,
                    ),
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

    private fun performPublicSpotifyPageRequest(url: URL): SearchHttpResponse {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", publicSpotifyAcceptLanguage)
            // Spotify serves an "Unsupported browser" page to Android's default Dalvik user agent.
            setRequestProperty("User-Agent", publicSpotifyUserAgent)
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
            .appendQueryParameter("include_groups", "album,single")
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

    private fun buildCatalogSearchUrl(artist: String, attribute: String? = null): URL {
        val uriBuilder = Uri.parse("https://itunes.apple.com/search")
            .buildUpon()
            .appendQueryParameter("term", artist)
            .appendQueryParameter("media", "music")
            .appendQueryParameter("entity", "song")
            .appendQueryParameter("limit", catalogPageSize.toString())

        attribute?.let { uriBuilder.appendQueryParameter("attribute", it) }

        return URL(uriBuilder.build().toString())
    }

    private fun buildPublicArtistUrl(artistId: String): URL {
        return URL("https://open.spotify.com/artist/$artistId")
    }

    private fun buildPublicAlbumUrl(albumId: String): URL {
        return URL("https://open.spotify.com/album/$albumId")
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

    private fun buildSpotifyArtistUrl(artistId: String?): String? {
        if (artistId.isNullOrBlank()) return null
        return Uri.parse("https://open.spotify.com/artist/$artistId").toString()
    }

    private fun extractInitialStatePayload(payload: String): String? {
        return Regex(
            pattern = """<script[^>]*id=["']initialState["'][^>]*>([^<]+)</script>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
            .find(payload)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { encoded ->
                runCatching {
                    String(java.util.Base64.getDecoder().decode(encoded))
                }.getOrNull()
            }
    }

    private fun fetchPublicInitialStateRoot(
        url: URL,
        pageErrorMessage: String,
        dataErrorMessage: String,
    ): JsonObject {
        val response = performPublicSpotifyPageRequest(url)
        if (response.responseCode !in 200..299) {
            error(pageErrorMessage)
        }

        val initialStatePayload = extractInitialStatePayload(response.payload)
            ?: error(dataErrorMessage)
        return json.parseToJsonElement(initialStatePayload).jsonObjectOrNull()
            ?: error(dataErrorMessage)
    }

    private fun searchQueriesFor(artist: String): List<String> {
        return listOf(
            "artist:\"$artist\"",
            artist,
        ).distinct()
    }

    private fun appendReleaseDates(
        items: List<JsonElement>,
        releaseDatesByAlbumUri: MutableMap<String, String>,
    ) {
        items.forEach { element ->
            val item = element.jsonObjectOrNull() ?: return@forEach
            val releases = item
                .jsonObject("releases")
                ?.jsonArray("items")

            if (!releases.isNullOrEmpty()) {
                appendReleaseDates(releases, releaseDatesByAlbumUri)
                return@forEach
            }

            val uri = item.string("uri") ?: return@forEach
            val releaseDate = item.jsonObject("date")?.releaseDateString() ?: return@forEach
            releaseDatesByAlbumUri[uri] = releaseDate
        }
    }

    private fun publicAlbumReferences(
        artistEntity: JsonObject,
        discography: JsonObject,
        releaseDatesByAlbumUri: Map<String, String>,
    ): List<PublicAlbumReference> {
        val releaseDatesByAlbumId = linkedMapOf<String, String>()
        val orderedAlbumIds = linkedSetOf<String>()

        appendPublicAlbumReferences(
            items = listOfNotNull(discography["latest"]),
            releaseDatesByAlbumUri = releaseDatesByAlbumUri,
            orderedAlbumIds = orderedAlbumIds,
            releaseDatesByAlbumId = releaseDatesByAlbumId,
        )
        appendPublicAlbumReferences(
            items = discography.jsonObject("popularReleasesAlbums")?.jsonArray("items").orEmpty(),
            releaseDatesByAlbumUri = releaseDatesByAlbumUri,
            orderedAlbumIds = orderedAlbumIds,
            releaseDatesByAlbumId = releaseDatesByAlbumId,
        )
        appendPublicAlbumReferences(
            items = discography.jsonObject("singles")?.jsonArray("items").orEmpty(),
            releaseDatesByAlbumUri = releaseDatesByAlbumUri,
            orderedAlbumIds = orderedAlbumIds,
            releaseDatesByAlbumId = releaseDatesByAlbumId,
        )
        appendPublicAlbumReferences(
            items = discography.jsonObject("albums")?.jsonArray("items").orEmpty(),
            releaseDatesByAlbumUri = releaseDatesByAlbumUri,
            orderedAlbumIds = orderedAlbumIds,
            releaseDatesByAlbumId = releaseDatesByAlbumId,
        )
        appendPublicAlbumReferences(
            items = discography.jsonObject("compilations")?.jsonArray("items").orEmpty(),
            releaseDatesByAlbumUri = releaseDatesByAlbumUri,
            orderedAlbumIds = orderedAlbumIds,
            releaseDatesByAlbumId = releaseDatesByAlbumId,
        )
        appendPublicAlbumReferences(
            items = artistEntity
                .jsonObject("relatedContent")
                ?.jsonObject("appearsOn")
                ?.jsonArray("items")
                .orEmpty(),
            releaseDatesByAlbumUri = releaseDatesByAlbumUri,
            orderedAlbumIds = orderedAlbumIds,
            releaseDatesByAlbumId = releaseDatesByAlbumId,
        )

        return orderedAlbumIds.map { albumId ->
            PublicAlbumReference(
                albumId = albumId,
                releaseDate = releaseDatesByAlbumId[albumId],
            )
        }
    }

    private fun appendPublicAlbumReferences(
        items: List<JsonElement>,
        releaseDatesByAlbumUri: Map<String, String>,
        orderedAlbumIds: MutableSet<String>,
        releaseDatesByAlbumId: MutableMap<String, String>,
    ) {
        items.forEach { element ->
            val item = element.jsonObjectOrNull() ?: return@forEach
            val releases = item
                .jsonObject("releases")
                ?.jsonArray("items")

            if (!releases.isNullOrEmpty()) {
                appendPublicAlbumReferences(
                    items = releases,
                    releaseDatesByAlbumUri = releaseDatesByAlbumUri,
                    orderedAlbumIds = orderedAlbumIds,
                    releaseDatesByAlbumId = releaseDatesByAlbumId,
                )
                return@forEach
            }

            val fallbackReleaseDate = item
                .jsonObject("date")
                ?.releaseDateString()
            val albumId = item.string("uri")?.let(::spotifyIdFromUri) ?: item.string("id")
            if (albumId.isNullOrBlank()) return@forEach

            item.string("uri")?.let { uri ->
                val releaseDate = fallbackReleaseDate ?: releaseDatesByAlbumUri[uri]
                if (releaseDate != null && albumId !in releaseDatesByAlbumId) {
                    releaseDatesByAlbumId[albumId] = releaseDate
                }
            } ?: run {
                if (fallbackReleaseDate != null && albumId !in releaseDatesByAlbumId) {
                    releaseDatesByAlbumId[albumId] = fallbackReleaseDate
                }
            }

            orderedAlbumIds += albumId
        }
    }

    private fun spotifyIdFromUri(uri: String?): String? {
        return uri?.substringAfterLast(':')
    }

    private fun catalogQueriesFor(artist: String): List<CatalogQuery> {
        val cleanedArtist = artist
            .replace(Regex("[^a-zA-Z0-9]+"), " ")
            .trim()

        return listOf(
            CatalogQuery(term = artist, attribute = "artistTerm"),
            CatalogQuery(term = cleanedArtist, attribute = "artistTerm"),
            CatalogQuery(term = artist, attribute = null),
            CatalogQuery(term = cleanedArtist, attribute = null),
        )
            .filter { it.term.isNotBlank() }
            .distinct()
    }

    private fun artistMatches(expectedArtist: String, actualArtist: String?): Boolean {
        if (actualArtist.isNullOrBlank()) return false
        val expected = normalizeArtistName(expectedArtist)
        val actual = normalizeArtistName(actualArtist)
        return actual == expected || actual.contains(expected) || expected.contains(actual)
    }

    private fun catalogTrackKey(track: CatalogTrack): String {
        return track.trackId?.let { "id-$it" }
            ?: "${normalizeArtistName(track.artistName)}-${track.trackName.lowercase()}"
    }

    private fun normalizeArtistName(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "")
    }

    private val artistIds = mapOf(
        "Yang D. Nash" to "63Sh0kQAWW3ZWn2aKDksbo",
        "Janno" to "7hpiHzP9aLLb5liDLxtwhM",
        "JANNO" to "7hpiHzP9aLLb5liDLxtwhM",
        "Tangajoe" to "0OA5dgpVdwzI8K82m8FPxN",
        "Tangajoe007" to "0OA5dgpVdwzI8K82m8FPxN",
        "TANGAJOE007" to "0OA5dgpVdwzI8K82m8FPxN",
        "NICMA MUSIC" to "0OoRIo7pJjtLgg3qyf1oDS",
        "NICMA" to "0OoRIo7pJjtLgg3qyf1oDS",
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

private data class CatalogQuery(
    val term: String,
    val attribute: String?,
)
}

private fun JsonObject.string(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull
}

private fun JsonObject.jsonObject(key: String): JsonObject? {
    return this[key]?.jsonObjectOrNull()
}

private fun JsonObject.jsonArray(key: String): List<JsonElement>? {
    return this[key]?.jsonArrayOrNull()
}

private fun JsonElement.jsonObjectOrNull(): JsonObject? {
    return this as? JsonObject
}

private fun JsonElement.jsonArrayOrNull(): JsonArray? {
    return this as? JsonArray
}

private fun JsonObject.releaseDateString(): String? {
    val year = string("year")?.toIntOrNull() ?: return null
    val month = string("month")?.toIntOrNull()
    val day = string("day")?.toIntOrNull()

    return when {
        month != null && day != null -> "%04d-%02d-%02d".format(year, month, day)
        month != null -> "%04d-%02d".format(year, month)
        else -> "%04d".format(year)
    }
}

private fun JsonObject.coverArtUrl(): String? {
    val sources = jsonArray("sources").orEmpty()
    return sources
        .mapNotNull { element ->
            val source = element.jsonObjectOrNull() ?: return@mapNotNull null
            source to (source.string("height")?.toIntOrNull() ?: 0)
        }
        .sortedByDescending { it.second }
        .firstOrNull()
        ?.first
        ?.string("url")
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
