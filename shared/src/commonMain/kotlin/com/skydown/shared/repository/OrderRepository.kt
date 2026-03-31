package com.skydown.shared.repository

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order

interface OrderRepository {
    suspend fun loadOrders(): Result<List<Order>>
    suspend fun submitOrder(
        userEmail: String,
        items: List<CartItem>,
        customerName: String,
        customerEmail: String,
        whatsApp: String,
        shippingAddress: String,
        message: String,
        paymentMethod: String,
        subtotalAmount: Double,
        shippingAmount: Double,
        taxRate: Double,
        taxAmount: Double,
        totalAmount: Double,
    ): Result<Unit>
    suspend fun toggleCompleted(orderId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteOrder(orderId: String): Result<Unit>
}
