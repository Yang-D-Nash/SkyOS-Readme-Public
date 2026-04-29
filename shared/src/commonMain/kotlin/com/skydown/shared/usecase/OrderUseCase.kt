package com.skydown.shared.usecase

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.OrderItem

object OrderUseCase {
    fun mapCartItems(items: List<CartItem>): List<OrderItem> {
        return items.mapIndexed { index, cartItem ->
            OrderItem(
                id = buildDeterministicOrderItemId(cartItem, index),
                name = cartItem.item.name,
                quantity = cartItem.quantity,
                size = cartItem.size,
                color = cartItem.color,
                productId = cartItem.item.id,
                shopifyVariantId = cartItem.shopifyVariantId,
                sku = cartItem.sku,
                unitPrice = cartItem.unitPrice,
            )
        }
    }

    private fun buildDeterministicOrderItemId(cartItem: CartItem, index: Int): String {
        val seed = listOf(
            cartItem.item.id,
            cartItem.size.trim().lowercase(),
            cartItem.color?.trim()?.lowercase().orEmpty(),
            cartItem.shopifyVariantId.orEmpty(),
            cartItem.sku.orEmpty(),
            cartItem.quantity.toString(),
            index.toString(),
        ).joinToString(separator = "|")

        return "oi_${seed.hashCode().toUInt().toString(16)}"
    }
}
