package com.skydown.android.ui.model

data class VideoEquipmentItem(
    val id: String,
    val title: String,
    val detail: String,
    val imageUrl: String? = null,
)

data class VideoYouTubeItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val highlight: String = "",
    val url: String,
)

data class ProducedWithArtist(
    val id: String,
    val name: String,
    val role: String,
    val highlight: String,
    val vibe: String,
    val imageUrl: String? = null,
    val spotifyArtistId: String? = null,
    val instagramUrl: String? = null,
    val youtubeUrl: String? = null,
)

data class VideoHubPublicConfig(
    val equipmentItems: List<VideoEquipmentItem>,
    val collaborationItems: List<ProducedWithArtist>,
) {
    companion object {
        fun default(): VideoHubPublicConfig = VideoHubPublicConfig(
            equipmentItems = listOf(
                VideoEquipmentItem(
                    id = "drones",
                    title = "Drohnen",
                    detail = "DJI Neo und DJI Avata 2 fuer bewegte Luftshots und FPV-Looks.",
                    imageUrl = null,
                ),
                VideoEquipmentItem(
                    id = "camera",
                    title = "Kamera",
                    detail = "Sony FX30 mit Sigma 18-50 mm f/2.8 plus Gimbals fuer saubere Motion-Shots.",
                    imageUrl = null,
                ),
                VideoEquipmentItem(
                    id = "mobile-capture",
                    title = "Mobile Capture",
                    detail = "iPhone 16 Pro mit Apple Log fuer flexible schnelle Shoots.",
                    imageUrl = null,
                ),
                VideoEquipmentItem(
                    id = "post-production",
                    title = "Postproduktion",
                    detail = "Adobe Premiere Pro, DaVinci Resolve Studio und Adobe After Effects.",
                    imageUrl = null,
                ),
            ),
            collaborationItems = defaultProducedWithArtists,
        )
    }
}

val defaultProducedWithArtists = listOf(
    ProducedWithArtist(
        id = "produced-with-toprack941",
        name = "Toprack941",
        role = "Performance & Release Visuals",
        highlight = "Straighte Performance-Shots mit rauer Energie.",
        vibe = "Street Energy",
        imageUrl = null,
        spotifyArtistId = "4CoozMQ3B3I20day60N7QA",
        instagramUrl = "https://www.instagram.com/toprack_941/",
    ),
    ProducedWithArtist(
        id = "produced-with-directa",
        name = "Directa",
        role = "Artist Visuals",
        highlight = "Direkte Bildsprache fuer dunkle, klare Releases.",
        vibe = "Dark Cut",
        imageUrl = null,
        spotifyArtistId = "2v4YJINKwkBY3DajPcmWm3",
        instagramUrl = "https://www.instagram.com/directascut/",
    ),
    ProducedWithArtist(
        id = "produced-with-michael-klotz",
        name = "Michael Klotz",
        role = "Cinematic Sessions",
        highlight = "Mehr Ruhe, mehr Fokus, mehr cineastische Naehe.",
        vibe = "Cinematic",
        imageUrl = null,
        spotifyArtistId = "4i8fdH54Ielws2ghRMPKGh",
        instagramUrl = "https://www.instagram.com/michael.ktz/",
    ),
    ProducedWithArtist(
        id = "produced-with-jojee",
        name = "Jojee",
        role = "Performance Clips",
        highlight = "Klarer Front-Fokus mit Hook-Momenten im Bild.",
        vibe = "Performance",
        imageUrl = null,
        spotifyArtistId = "2azLzTxCm662tbpPwPwQwI",
        instagramUrl = "https://www.instagram.com/iamjojee/",
    ),
    ProducedWithArtist(
        id = "produced-with-sowjet020",
        name = "Sowjet020",
        role = "Release Visuals",
        highlight = "Roh, direkt und auf Attitude geschnitten.",
        vibe = "Raw",
        imageUrl = null,
        spotifyArtistId = "2LLwIW8Nk30IYyC9sHr0Cm",
        instagramUrl = "https://www.instagram.com/sowjet020/",
    ),
    ProducedWithArtist(
        id = "produced-with-cemo",
        name = "C€MO",
        role = "Artist Portraits",
        highlight = "Mehr Charakter im Close-up, weniger Distanz.",
        vibe = "Portrait",
        imageUrl = null,
        spotifyArtistId = "0KD8un5on2oXfJ3dwxpw40",
        instagramUrl = "https://www.instagram.com/cemo_sd/",
    ),
    ProducedWithArtist(
        id = "produced-with-daco27",
        name = "Daco27",
        role = "Motion Visuals",
        highlight = "Vorwaertsdrang, Bewegung und Tempo im Frame.",
        vibe = "Motion",
        imageUrl = null,
        spotifyArtistId = "3oxSFNrPcoqo7qkAIsR74D",
        instagramUrl = "https://www.instagram.com/daco.27/",
    ),
    ProducedWithArtist(
        id = "produced-with-neo",
        name = "Neo",
        role = "Creative Sessions",
        highlight = "Lockerere Sessions mit persoenlicherem Touch.",
        vibe = "Session",
        imageUrl = null,
        instagramUrl = "https://www.instagram.com/sincity.neo/",
    ),
    ProducedWithArtist(
        id = "produced-with-phil",
        name = "Phil",
        role = "Portrait Moments",
        highlight = "Ruhige Frames, menschlicher Look, klare Praesenz.",
        vibe = "Portrait",
        imageUrl = null,
        instagramUrl = "https://www.instagram.com/philip_beau/",
    ),
)
