package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val size: String? = null,
)
