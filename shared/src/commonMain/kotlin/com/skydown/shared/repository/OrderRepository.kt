package com.skydown.shared.repository

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order
import com.skydown.shared.model.OrderSubmission

interface OrderRepository {
    suspend fun loadOrders(): Result<List<Order>>
    suspend fun submitOrder(submission: OrderSubmission): Result<String>
    suspend fun toggleCompleted(orderId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteOrder(orderId: String): Result<Unit>
}
