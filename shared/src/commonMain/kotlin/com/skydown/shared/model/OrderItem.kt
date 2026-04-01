package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val size: String? = null,
    val color: String? = null,
    val productId: String? = null,
    val shopifyVariantId: String? = null,
    val sku: String? = null,
    val unitPrice: Double? = null,
)
