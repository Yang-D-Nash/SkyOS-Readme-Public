package com.skydown.android.data.repository

import com.skydown.shared.model.Track
import com.skydown.shared.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL

class AndroidMusicRepository : MusicRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchTracks(artist: String): Result<List<Track>> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val query = java.net.URLEncoder.encode(artist, "UTF-8")
                val payload = URL("https://itunes.apple.com/search?term=$query&entity=song&limit=50")
                    .readText()
                json.decodeFromString<com.skydown.shared.model.SearchResult>(payload).results
            }
        }
    }
}
