package com.skydown.shared.repository

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order

interface OrderRepository {
    suspend fun loadOrders(): Result<List<Order>>
    suspend fun submitOrder(userEmail: String, items: List<CartItem>): Result<Unit>
    suspend fun toggleCompleted(orderId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteOrder(orderId: String): Result<Unit>
}
