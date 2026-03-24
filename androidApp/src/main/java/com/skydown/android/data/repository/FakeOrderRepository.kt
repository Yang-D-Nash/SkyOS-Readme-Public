package com.skydown.android.data.repository

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order
import com.skydown.shared.model.sampleOrders
import com.skydown.shared.repository.OrderRepository

class FakeOrderRepository : OrderRepository {
    private var orders = sampleOrders()

    override suspend fun loadOrders(): Result<List<Order>> = Result.success(orders)

    override suspend fun submitOrder(userEmail: String, items: List<CartItem>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun toggleCompleted(orderId: String, isCompleted: Boolean): Result<Unit> {
        orders = orders.map { order ->
            if (order.id == orderId) order.copy(isCompleted = !isCompleted) else order
        }
        return Result.success(Unit)
    }

    override suspend fun deleteOrder(orderId: String): Result<Unit> {
        orders = orders.filterNot { it.id == orderId }
        return Result.success(Unit)
    }
}
