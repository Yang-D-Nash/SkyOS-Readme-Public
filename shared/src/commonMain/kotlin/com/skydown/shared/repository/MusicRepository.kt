package com.skydown.shared.repository

import com.skydown.shared.model.Track

interface MusicRepository {
    suspend fun fetchTracks(artist: String): Result<List<Track>>
}
