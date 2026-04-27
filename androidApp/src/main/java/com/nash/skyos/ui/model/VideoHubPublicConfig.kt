package com.nash.skyos.ui.model

import com.nash.skyos.R
import com.nash.skyos.data.AppTextResolver

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
                    title = AppTextResolver.string(R.string.video_public_equipment_drones_title),
                    detail = AppTextResolver.string(R.string.video_public_equipment_drones_detail),
                    imageUrl = null,
                ),
                VideoEquipmentItem(
                    id = "camera",
                    title = AppTextResolver.string(R.string.video_public_equipment_camera_title),
                    detail = AppTextResolver.string(R.string.video_public_equipment_camera_detail),
                    imageUrl = null,
                ),
                VideoEquipmentItem(
                    id = "mobile-capture",
                    title = AppTextResolver.string(R.string.video_public_equipment_mobile_title),
                    detail = AppTextResolver.string(R.string.video_public_equipment_mobile_detail),
                    imageUrl = null,
                ),
                VideoEquipmentItem(
                    id = "post-production",
                    title = AppTextResolver.string(R.string.video_public_equipment_post_title),
                    detail = AppTextResolver.string(R.string.video_public_equipment_post_detail),
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
        role = AppTextResolver.string(R.string.video_public_artist_toprack_role),
        highlight = AppTextResolver.string(R.string.video_public_artist_toprack_highlight),
        vibe = AppTextResolver.string(R.string.video_public_artist_toprack_vibe),
        imageUrl = null,
        spotifyArtistId = "4CoozMQ3B3I20day60N7QA",
        instagramUrl = "https://www.instagram.com/toprack_941/",
    ),
    ProducedWithArtist(
        id = "produced-with-directa",
        name = "Directa",
        role = AppTextResolver.string(R.string.video_public_artist_directa_role),
        highlight = AppTextResolver.string(R.string.video_public_artist_directa_highlight),
        vibe = AppTextResolver.string(R.string.video_public_artist_directa_vibe),
        imageUrl = null,
        spotifyArtistId = "2v4YJINKwkBY3DajPcmWm3",
        instagramUrl = "https://www.instagram.com/directascut/",
    ),
    ProducedWithArtist(
        id = "produced-with-michael-klotz",
        name = "Michael Klotz",
        role = AppTextResolver.string(R.string.video_public_artist_michael_role),
        highlight = AppTextResolver.string(R.string.video_public_artist_michael_highlight),
        vibe = AppTextResolver.string(R.string.video_public_artist_michael_vibe),
        imageUrl = null,
        spotifyArtistId = "4i8fdH54Ielws2ghRMPKGh",
        instagramUrl = "https://www.instagram.com/michael.ktz/",
    ),
    ProducedWithArtist(
        id = "produced-with-jojee",
        name = "Jojee",
        role = AppTextResolver.string(R.string.video_public_artist_jojee_role),
        highlight = AppTextResolver.string(R.string.video_public_artist_jojee_highlight),
        vibe = AppTextResolver.string(R.string.video_public_artist_jojee_vibe),
        imageUrl = null,
        spotifyArtistId = "2azLzTxCm662tbpPwPwQwI",
        instagramUrl = "https://www.instagram.com/iamjojee/",
    ),
    ProducedWithArtist(
        id = "produced-with-sowjet020",
        name = "Sowjet020",
        role = AppTextResolver.string(R.string.video_public_artist_sowjet_role),
        highlight = AppTextResolver.string(R.string.video_public_artist_sowjet_highlight),
        vibe = AppTextResolver.string(R.string.video_public_artist_sowjet_vibe),
        imageUrl = null,
        spotifyArtistId = "2LLwIW8Nk30IYyC9sHr0Cm",
        instagramUrl = "https://www.instagram.com/sowjet020/",
    ),
    ProducedWithArtist(
        id = "produced-with-cemo",
        name = "C€MO",
        role = AppTextResolver.string(R.string.video_public_artist_cemo_role),
        highlight = AppTextResolver.string(R.string.video_public_artist_cemo_highlight),
        vibe = AppTextResolver.string(R.string.video_public_artist_cemo_vibe),
        imageUrl = null,
        spotifyArtistId = "0KD8un5on2oXfJ3dwxpw40",
        instagramUrl = "https://www.instagram.com/cemo_sd/",
    ),
    ProducedWithArtist(
        id = "produced-with-daco27",
        name = "Daco27",
        role = AppTextResolver.string(R.string.video_public_artist_daco_role),
        highlight = AppTextResolver.string(R.string.video_public_artist_daco_highlight),
        vibe = AppTextResolver.string(R.string.video_public_artist_daco_vibe),
        imageUrl = null,
        spotifyArtistId = "3oxSFNrPcoqo7qkAIsR74D",
        instagramUrl = "https://www.instagram.com/daco.27/",
    ),
    ProducedWithArtist(
        id = "produced-with-neo",
        name = "Neo",
        role = AppTextResolver.string(R.string.video_public_artist_neo_role),
        highlight = AppTextResolver.string(R.string.video_public_artist_neo_highlight),
        vibe = AppTextResolver.string(R.string.video_public_artist_neo_vibe),
        imageUrl = null,
        instagramUrl = "https://www.instagram.com/sincity.neo/",
    ),
    ProducedWithArtist(
        id = "produced-with-phil",
        name = "Phil",
        role = AppTextResolver.string(R.string.video_public_artist_phil_role),
        highlight = AppTextResolver.string(R.string.video_public_artist_phil_highlight),
        vibe = AppTextResolver.string(R.string.video_public_artist_phil_vibe),
        imageUrl = null,
        instagramUrl = "https://www.instagram.com/philip_beau/",
    ),
)
