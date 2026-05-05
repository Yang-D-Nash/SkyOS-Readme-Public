package com.nash.skyos.ui.model

import java.util.Locale

val defaultZweizweiMusicArtists = listOf(
    "Janno",
    "Mave",
    "Tangajoe007",
    "Yang D. Nash",
    "ThaDude",
)

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
