package com.nash.skyos.ui.model

import java.util.Locale

val defaultZweizweiMusicArtists = listOf(
    "JANNO",
    "MAVE040",
    "Tangajoe007",
    "DANGU61",
    "Yang D. Nash",
    "ThaDude",
)

private val canonicalSpotifyArtistIdsByKey = mapOf(
    musicArtistKey("JANNO") to "7hpiHzP9aLLb5liDLxtwhM",
    musicArtistKey("MAVE040") to "0GXymtRaIk2ngbXSkcHtsp",
    musicArtistKey("Tangajoe007") to "0OA5dgpVdwzI8K82m8FPxN",
    musicArtistKey("DANGU61") to "08rIanUNO6en6coKEafyPO",
    musicArtistKey("Yang D. Nash") to "63Sh0kQAWW3ZWn2aKDksbo",
    musicArtistKey("ThaDude") to "0Jmb7DXFkKxxRjqD70vi0e",
)

fun canonicalSpotifyArtistUrlForMusicArtist(artist: String): String? {
    val artistId = canonicalSpotifyArtistIdsByKey[musicArtistKey(artist)] ?: return null
    return "https://open.spotify.com/artist/$artistId"
}

fun mergeZweizweiMusicArtists(liveArtists: Iterable<String>): List<String> {
    val merged = linkedMapOf<String, String>()

    defaultZweizweiMusicArtists.forEach { fallbackArtist ->
        val canonicalArtist = canonicalZweizweiArtistName(fallbackArtist)
        val key = musicArtistKey(canonicalArtist)
        merged[key] = canonicalArtist
    }
    liveArtists
        .map { canonicalZweizweiArtistName(it) }
        .filter { it.isNotBlank() }
        .forEach { artist ->
            merged.putIfAbsent(musicArtistKey(artist), artist)
        }

    return merged.values.toList()
}

fun musicArtistKey(artist: String): String {
    return artist
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "")
}

private fun canonicalZweizweiArtistName(artist: String): String {
    return when (musicArtistKey(artist)) {
        "janno" -> "JANNO"
        "mave", "mave040" -> "MAVE040"
        "tangajoe", "tangajoe007" -> "Tangajoe007"
        else -> artist.trim()
    }
}
