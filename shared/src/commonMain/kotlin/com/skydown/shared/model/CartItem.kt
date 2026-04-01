package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    val item: MerchandiseItem,
    val size: String,
    val color: String? = null,
    val quantity: Int,
    val shopifyVariantId: String? = null,
    val sku: String? = null,
    val unitPrice: Double? = null,
)
