package com.skydown.shared.service

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order
import com.skydown.shared.model.OrderSubmission
import com.skydown.shared.repository.OrderRepository
import com.skydown.shared.text.SharedText

class OrderService(
    private val repository: OrderRepository,
) {
    suspend fun loadOrders(): Result<List<Order>> = repository.loadOrders()

    suspend fun submitOrder(submission: OrderSubmission): Result<String> {
        if (submission.items.isEmpty()) {
            return Result.failure(IllegalArgumentException(SharedText.ORDER_SUBMIT_CART_EMPTY))
        }
        if (submission.userEmail.isBlank()) {
            return Result.failure(IllegalArgumentException(SharedText.ORDER_SUBMIT_USER_NOT_SIGNED_IN))
        }
        if (submission.customerName.isBlank() || submission.customerEmail.isBlank()) {
            return Result.failure(IllegalArgumentException(SharedText.ORDER_SUBMIT_NAME_EMAIL_REQUIRED))
        }
        if (submission.shippingAddress.isBlank()) {
            return Result.failure(IllegalArgumentException(SharedText.ORDER_SUBMIT_ADDRESS_REQUIRED))
        }
        if (submission.shippingZone.isBlank() || submission.shippingCountryCode.isBlank()) {
            return Result.failure(IllegalArgumentException(SharedText.ORDER_SUBMIT_SHIPPING_REQUIRED))
        }

        return repository.submitOrder(submission)
    }

    suspend fun toggleCompleted(orderId: String, isCompleted: Boolean): Result<Unit> {
        if (orderId.isBlank()) {
            return Result.failure(IllegalArgumentException(SharedText.ORDER_INVALID_ID))
        }

        return repository.toggleCompleted(orderId, isCompleted)
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        if (orderId.isBlank()) {
            return Result.failure(IllegalArgumentException(SharedText.ORDER_INVALID_ID))
        }

        return repository.deleteOrder(orderId)
    }
}
