package com.nash.skyos.data.repository

import com.skydown.shared.model.Track
import com.skydown.shared.repository.MusicRepository

object UiTestMusicRepository : MusicRepository {
    override suspend fun fetchTracks(artist: String): Result<List<Track>> = Result.success(
        listOf(
            Track(
                trackId = 2201,
                artistId = 77,
                spotifyArtistId = "7hpiHzP9aLLb5liDLxtwhM",
                spotifyTrackId = "73HRKI6lyEEdBUFpHybBi9",
                artistName = "Janno",
                trackName = "UI Test Night Drive",
                collectionName = "Skydown Device Flow",
                externalUrl = "https://open.spotify.com/track/73HRKI6lyEEdBUFpHybBi9",
                wrapperType = "track",
                releaseDate = "2026-04-19T00:00:00Z",
            ),
        ),
    )
}
