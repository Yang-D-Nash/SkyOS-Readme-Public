package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class MerchandiseVariant(
    val id: String = "",
    val title: String = "",
    val size: String? = null,
    val color: String? = null,
    val shopifyVariantId: String? = null,
    val sku: String? = null,
    val price: Double = 0.0,
    val currency: String = "EUR",
    val availableForSale: Boolean = true,
)

@Serializable
data class MerchandiseItem(
    val id: String? = null,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrls: List<String>,
    val available: Boolean,
    val currency: String = "EUR",
    val sku: String? = null,
    val shopifyProductId: String? = null,
    val shopifyHandle: String? = null,
    val availableForSale: Boolean = true,
    val shopifySyncActive: Boolean = true,
    val variants: List<MerchandiseVariant> = emptyList(),
    val source: String = "manual",
    val isVisibleInApp: Boolean = true,
    val featured: Boolean = false,
    val sortOrder: Int = 0,
    val customBadge: String = "",
    val customImageOverride: String = "",
)
