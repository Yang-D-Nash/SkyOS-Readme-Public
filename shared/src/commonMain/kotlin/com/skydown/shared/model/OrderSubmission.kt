package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderSubmission(
    val userEmail: String,
    val items: List<CartItem>,
    val customerName: String,
    val customerEmail: String,
    val whatsApp: String,
    val shippingAddress: String,
    val shippingAddressData: ShippingAddressData,
    val shippingZone: String,
    val shippingCountryCode: String,
    val message: String,
    val paymentMethod: String,
    val paymentStatus: String = "pending",
    val subtotalAmount: Double,
    val shippingAmount: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val fulfillmentProvider: String,
)
