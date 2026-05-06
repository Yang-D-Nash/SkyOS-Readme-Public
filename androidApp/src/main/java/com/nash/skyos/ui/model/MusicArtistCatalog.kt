package com.nash.skyos.ui.model

import java.util.Locale

val defaultZweizweiMusicArtists = listOf(
    "Janno",
    "Mave",
    "Tangajoe007",
    "Yang D. Nash",
    "ThaDude",
)

private val canonicalSpotifyArtistIdsByKey = mapOf(
    musicArtistKey("Janno") to "7hpiHzP9aLLb5liDLxtwhM",
    musicArtistKey("Mave") to "0GXymtRaIk2ngbXSkcHtsp",
    musicArtistKey("Tangajoe007") to "0OA5dgpVdwzI8K82m8FPxN",
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
        val key = musicArtistKey(fallbackArtist)
        merged[key] = fallbackArtist
    }
    liveArtists
        .map { it.trim() }
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
