package com.skydown.shared.usecase

import com.skydown.shared.model.Track

object MusicUseCase {
    fun filterArtistTracks(artist: String, tracks: List<Track>): List<Track> {
        val artistId = when (artist) {
            "ThaDude" -> 1677936430
            else -> 1637910017
        }

        return tracks.filter { track ->
            track.wrapperType == "track" && track.artistId == artistId
        }
    }
}
