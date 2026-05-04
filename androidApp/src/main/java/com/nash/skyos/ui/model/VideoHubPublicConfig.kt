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

val defaultProducedWithArtists = emptyList<ProducedWithArtist>()
