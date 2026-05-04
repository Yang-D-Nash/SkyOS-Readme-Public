package com.skydown.shared.usecase

import com.skydown.shared.model.Track

object MusicUseCase {
    private val artistIds = mapOf(
        "Yang D. Nash" to "63Sh0kQAWW3ZWn2aKDksbo",
        "JANNO" to "7hpiHzP9aLLb5liDLxtwhM",
        "Tangajoe" to "0OA5dgpVdwzI8K82m8FPxN",
        "TANGAJOE007" to "0OA5dgpVdwzI8K82m8FPxN",
        "NICMA MUSIC" to "0OoRIo7pJjtLgg3qyf1oDS",
        "NICMA" to "0OoRIo7pJjtLgg3qyf1oDS",
    )

    fun filterArtistTracks(artist: String, tracks: List<Track>): List<Track> {
        val artistId = artistIds[artist]
        return tracks.filter { track ->
            track.wrapperType == "track" &&
                if (artistId != null) {
                    track.spotifyArtistId == artistId ||
                        (track.spotifyArtistId == null && artistMatches(artist, track.artistName))
                } else {
                    artistMatches(artist, track.artistName)
                }
        }
    }

    private fun artistMatches(expectedArtist: String, actualArtist: String?): Boolean {
        if (actualArtist.isNullOrBlank()) return false

        val expected = normalizeArtistName(expectedArtist)
        val actual = normalizeArtistName(actualArtist)

        return actual == expected || actual.contains(expected) || expected.contains(actual)
    }

    private fun normalizeArtistName(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "")
    }
}
