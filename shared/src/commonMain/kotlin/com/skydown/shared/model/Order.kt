package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String? = null,
    val userEmail: String,
    val customerName: String? = null,
    val customerEmail: String? = null,
    val whatsApp: String? = null,
    val message: String? = null,
    val items: List<OrderItem>,
    val isCompleted: Boolean,
    val timestampEpochMillis: Long,
)

fun sampleOrders(): List<Order> = listOf(
    Order(
        id = "order-1",
        userEmail = "demo@skydown.app",
        customerName = "Yang D. Nash",
        customerEmail = "demo@skydown.app",
        whatsApp = "+49 170 0000000",
        message = "Bitte kurze Rueckmeldung per Mail.",
        items = listOf(
            OrderItem(
                id = "item-1",
                name = "Skydown T-Shirt",
                quantity = 2,
                size = "M",
            ),
        ),
        isCompleted = false,
        timestampEpochMillis = 1_725_100_000_000,
    ),
    Order(
        id = "order-2",
        userEmail = "fans@skydown.app",
        customerName = "Skydown Fan",
        customerEmail = "fans@skydown.app",
        whatsApp = "",
        message = "",
        items = listOf(
            OrderItem(
                id = "item-2",
                name = "Skydown Hoodie",
                quantity = 1,
                size = "L",
            ),
        ),
        isCompleted = true,
        timestampEpochMillis = 1_725_200_000_000,
    ),
)
