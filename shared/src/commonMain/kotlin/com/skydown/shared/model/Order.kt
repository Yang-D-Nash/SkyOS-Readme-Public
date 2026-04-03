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
    val shippingAddressData: ShippingAddressData? = null,
    val shippingZone: String? = null,
    val shippingCountryCode: String? = null,
    val paymentMethod: String? = null,
    val paymentProvider: String? = null,
    val paymentStatus: String? = null,
    val paymentReference: String? = null,
    val subtotalAmount: Double? = null,
    val shippingAmount: Double? = null,
    val shippingPriceCharged: Double? = null,
    val taxRate: Double? = null,
    val taxAmount: Double? = null,
    val totalAmount: Double? = null,
    val fulfillmentProvider: String? = null,
    val fulfillmentStatus: String? = null,
    val shopifyOrderId: String? = null,
    val shopifyOrderName: String? = null,
    val shopifySyncStatus: String? = null,
    val stripeCheckoutSessionId: String? = null,
    val stripePaymentIntentId: String? = null,
    val stripeCheckoutStatus: String? = null,
    val message: String? = null,
    val items: List<OrderItem>,
    val isCompleted: Boolean,
    val timestampEpochMillis: Long,
)
