package com.skydown.shared.usecase

import com.skydown.shared.model.Track

object MusicUseCase {
    private val artistIds = mapOf(
        "Yang D. Nash" to "63Sh0kQAWW3ZWn2aKDksbo",
        "ThaDude" to "0Jmb7DXFkKxxRjqD70vi0e",
        "MAVE" to "0GXymtRaIk2ngbXSkcHtsp",
        "JANNO" to "7hpiHzP9aLLb5liDLxtwhM",
        "TANGAJOE007" to "0OA5dgpVdwzI8K82m8FPxN",
        "Toprack941" to "4CoozMQ3B3I20day60N7QA",
    )

    fun filterArtistTracks(artist: String, tracks: List<Track>): List<Track> {
        val artistId = artistIds[artist]
        return tracks.filter { track ->
            track.wrapperType == "track" &&
                if (artistId != null) {
                    track.spotifyArtistId == artistId
                } else {
                    track.artistName?.equals(artist, ignoreCase = true) == true
                }
        }
    }
}
