package com.skydown.android.ui.model

data class VideoEquipmentItem(
    val id: String,
    val title: String,
    val detail: String,
)

data class VideoYouTubeItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val url: String,
)

data class ProducedWithArtist(
    val id: String,
    val name: String,
    val role: String,
    val spotifyArtistId: String? = null,
    val instagramUrl: String? = null,
)

data class VideoHubPublicConfig(
    val equipmentItems: List<VideoEquipmentItem>,
    val youtubeItems: List<VideoYouTubeItem>,
) {
    companion object {
        fun default(): VideoHubPublicConfig = VideoHubPublicConfig(
            equipmentItems = listOf(
                VideoEquipmentItem(
                    id = "drones",
                    title = "Drohnen",
                    detail = "DJI Neo und DJI Avata 2 fuer bewegte Luftshots und FPV-Looks.",
                ),
                VideoEquipmentItem(
                    id = "camera",
                    title = "Kamera",
                    detail = "Sony FX30 mit Sigma 18-50 mm f/2.8 plus Gimbals fuer saubere Motion-Shots.",
                ),
                VideoEquipmentItem(
                    id = "mobile-capture",
                    title = "Mobile Capture",
                    detail = "iPhone 16 Pro mit Apple Log fuer flexible schnelle Shoots.",
                ),
                VideoEquipmentItem(
                    id = "post-production",
                    title = "Postproduktion",
                    detail = "Adobe Premiere Pro, DaVinci Resolve Studio und Adobe After Effects.",
                ),
            ),
            youtubeItems = emptyList(),
        )
    }
}

val skydownProducedWithArtists = listOf(
    ProducedWithArtist(
        id = "produced-with-toprack941",
        name = "Toprack941",
        role = "Video-Collaboration mit Skydown",
        spotifyArtistId = "4CoozMQ3B3I20day60N7QA",
        instagramUrl = "https://www.instagram.com/toprack_941/",
    ),
    ProducedWithArtist(
        id = "produced-with-directa",
        name = "Directa",
        role = "Produced With",
        spotifyArtistId = "2v4YJINKwkBY3DajPcmWm3",
        instagramUrl = "https://www.instagram.com/directascut/",
    ),
    ProducedWithArtist(
        id = "produced-with-michael-klotz",
        name = "Michael Klotz",
        role = "Produced With",
        spotifyArtistId = "4i8fdH54Ielws2ghRMPKGh",
        instagramUrl = "https://www.instagram.com/michael.ktz/",
    ),
    ProducedWithArtist(
        id = "produced-with-jojee",
        name = "Jojee",
        role = "Produced With",
        spotifyArtistId = "2azLzTxCm662tbpPwPwQwI",
        instagramUrl = "https://www.instagram.com/iamjojee/",
    ),
    ProducedWithArtist(
        id = "produced-with-sowjet020",
        name = "Sowjet020",
        role = "Produced With",
        spotifyArtistId = "2LLwIW8Nk30IYyC9sHr0Cm",
        instagramUrl = "https://www.instagram.com/sowjet020/",
    ),
    ProducedWithArtist(
        id = "produced-with-cemo",
        name = "C€MO",
        role = "Produced With",
        spotifyArtistId = "0KD8un5on2oXfJ3dwxpw40",
        instagramUrl = "https://www.instagram.com/cemo_sd/",
    ),
    ProducedWithArtist(
        id = "produced-with-daco27",
        name = "Daco27",
        role = "Produced With",
        spotifyArtistId = "3oxSFNrPcoqo7qkAIsR74D",
        instagramUrl = "https://www.instagram.com/daco.27/",
    ),
    ProducedWithArtist(
        id = "produced-with-neo",
        name = "Neo",
        role = "Produced With",
        instagramUrl = "https://www.instagram.com/sincity.neo/",
    ),
)
