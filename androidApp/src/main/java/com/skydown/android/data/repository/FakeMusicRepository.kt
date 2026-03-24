package com.skydown.android.data.repository

import com.skydown.shared.model.Track
import com.skydown.shared.model.sampleTracks
import com.skydown.shared.repository.MusicRepository

class FakeMusicRepository : MusicRepository {
    override suspend fun fetchTracks(artist: String): Result<List<Track>> {
        return Result.success(sampleTracks())
    }
}
