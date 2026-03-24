package com.skydown.shared.usecase

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.OrderItem
import kotlin.random.Random

object OrderUseCase {
    fun mapCartItems(items: List<CartItem>): List<OrderItem> {
        return items.map { cartItem ->
            OrderItem(
                id = Random.nextLong().toString(),
                name = cartItem.item.name,
                quantity = cartItem.quantity,
                size = cartItem.size,
            )
        }
    }
}
