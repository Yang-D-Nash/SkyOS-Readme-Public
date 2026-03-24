package com.skydown.shared.service

import com.skydown.shared.model.Track
import com.skydown.shared.repository.MusicRepository
import com.skydown.shared.usecase.MusicUseCase

class MusicService(
    private val repository: MusicRepository,
) {
    suspend fun fetchTracks(artist: String): Result<List<Track>> {
        return repository.fetchTracks(artist).map { tracks ->
            MusicUseCase.filterArtistTracks(artist, tracks)
        }
    }
}
