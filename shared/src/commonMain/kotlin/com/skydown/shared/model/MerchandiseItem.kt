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

fun sampleMerchandiseItems(): List<MerchandiseItem> = listOf(
    MerchandiseItem(
        id = "1",
        name = "Skydown T-Shirt",
        price = 24.99,
        description = "Ein cleanes Shirt im Skydown-Look fuer Alltag und Shows.",
        imageUrls = listOf(
            "https://i.imgur.com/8QG3tQJ.png",
            "https://i.imgur.com/G20y5iQ.png",
            "https://i.imgur.com/R38w6kS.png",
        ),
        available = true,
        variants = listOf(
            MerchandiseVariant(
                id = "shirt-black-m",
                title = "Skydown T-Shirt / Black / M",
                size = "M",
                color = "Black",
                sku = "SKY-SHIRT-BLACK-M",
                price = 24.99,
            ),
        ),
    ),
    MerchandiseItem(
        id = "2",
        name = "Skydown Hoodie",
        price = 54.99,
        description = "Schwerer Hoodie mit ruhiger Front und klarem Backprint.",
        imageUrls = listOf(
            "https://i.imgur.com/G20y5iQ.png",
            "https://i.imgur.com/R38w6kS.png",
        ),
        available = true,
        variants = listOf(
            MerchandiseVariant(
                id = "hoodie-black-l",
                title = "Skydown Hoodie / Black / L",
                size = "L",
                color = "Black",
                sku = "SKY-HOODIE-BLACK-L",
                price = 54.99,
            ),
        ),
    ),
)
