package com.nash.skyos.ui.model

data class ProfileGalleryItem(
    val id: String,
    val ownerId: String,
    val type: ProfileMediaType,
    val title: String,
    val caption: String?,
    val mediaUrl: String,
    val thumbnailUrl: String?,
    val storagePath: String?,
    val createdAtEpochMillis: Long,
)

enum class ProfileMediaType(
    val rawValue: String,
    val title: String,
    val fallbackExtension: String,
) {
    Image("image", "Bilder", "jpg");

    companion object {
        fun fromRawValue(rawValue: String?): ProfileMediaType = when (rawValue?.lowercase()) {
            "image" -> Image
            else -> Image
        }
    }
}
