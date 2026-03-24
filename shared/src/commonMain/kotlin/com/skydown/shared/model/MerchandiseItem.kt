package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class MerchandiseItem(
    val id: String? = null,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrls: List<String>,
    val available: Boolean,
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
    ),
)
