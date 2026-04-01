package com.skydown.shared.service

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order
import com.skydown.shared.model.OrderSubmission
import com.skydown.shared.repository.OrderRepository

class OrderService(
    private val repository: OrderRepository,
) {
    suspend fun loadOrders(): Result<List<Order>> = repository.loadOrders()

    suspend fun submitOrder(submission: OrderSubmission): Result<String> {
        if (submission.items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Warenkorb ist leer."))
        }
        if (submission.userEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Benutzer nicht angemeldet."))
        }
        if (submission.customerName.isBlank() || submission.customerEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Name und E-Mail sind erforderlich."))
        }
        if (submission.shippingAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Adresse ist erforderlich."))
        }
        if (submission.shippingZone.isBlank() || submission.shippingCountryCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Versandzone und Land muessen gesetzt sein."))
        }

        return repository.submitOrder(submission)
    }

    suspend fun toggleCompleted(orderId: String, isCompleted: Boolean): Result<Unit> {
        if (orderId.isBlank()) {
            return Result.failure(IllegalArgumentException("Bestellung hat keine gueltige ID."))
        }

        return repository.toggleCompleted(orderId, isCompleted)
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        if (orderId.isBlank()) {
            return Result.failure(IllegalArgumentException("Bestellung hat keine gueltige ID."))
        }

        return repository.deleteOrder(orderId)
    }
}
