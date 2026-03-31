package com.skydown.shared.service

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order
import com.skydown.shared.repository.OrderRepository

class OrderService(
    private val repository: OrderRepository,
) {
    suspend fun loadOrders(): Result<List<Order>> = repository.loadOrders()

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
    ): Result<Unit> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Warenkorb ist leer."))
        }
        if (userEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Benutzer nicht angemeldet."))
        }
        if (customerName.isBlank() || customerEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Name und E-Mail sind erforderlich."))
        }
        if (shippingAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Adresse ist erforderlich."))
        }

        return repository.submitOrder(
            userEmail = userEmail,
            items = items,
            customerName = customerName,
            customerEmail = customerEmail,
            whatsApp = whatsApp,
            shippingAddress = shippingAddress,
            message = message,
            paymentMethod = paymentMethod,
            subtotalAmount = subtotalAmount,
            shippingAmount = shippingAmount,
            taxRate = taxRate,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
        )
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
