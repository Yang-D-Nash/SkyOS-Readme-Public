package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String? = null,
    val userEmail: String,
    val customerName: String? = null,
    val customerEmail: String? = null,
    val whatsApp: String? = null,
    val shippingAddress: String? = null,
    val paymentMethod: String? = null,
    val subtotalAmount: Double? = null,
    val shippingAmount: Double? = null,
    val taxRate: Double? = null,
    val taxAmount: Double? = null,
    val totalAmount: Double? = null,
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
        shippingAddress = "Beispielstrasse 8\n10115 Berlin\nDeutschland",
        paymentMethod = "PayPal",
        subtotalAmount = 58.0,
        shippingAmount = 4.9,
        taxRate = 19.0,
        taxAmount = 10.05,
        totalAmount = 62.9,
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
        shippingAddress = "Musterweg 3\n50667 Koeln\nDeutschland",
        paymentMethod = "Bankueberweisung",
        subtotalAmount = 79.0,
        shippingAmount = 0.0,
        taxRate = 19.0,
        taxAmount = 12.61,
        totalAmount = 79.0,
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
