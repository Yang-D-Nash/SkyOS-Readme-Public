package com.skydown.shared.model

data class ProfileUpdateInput(
    val username: String,
    val whatsApp: String? = null,
    val profileTagline: String? = null,
    val profileBio: String? = null,
    val instagramHandle: String? = null,
)
