package com.skydown.shared.usecase

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.ContactRequest
import com.skydown.shared.model.MerchandiseItem

object CartUseCase {
    fun addItem(
        currentItems: List<CartItem>,
        item: MerchandiseItem,
        size: String,
        quantity: Int,
    ): List<CartItem> {
        val mutableItems = currentItems.toMutableList()
        val existingIndex = mutableItems.indexOfFirst { it.item.id == item.id && it.size == size }

        if (existingIndex >= 0) {
            val existingItem = mutableItems[existingIndex]
            mutableItems[existingIndex] = existingItem.copy(quantity = existingItem.quantity + quantity)
        } else {
            mutableItems += CartItem(item = item, size = size, quantity = quantity)
        }

        return mutableItems
    }

    fun removeItem(currentItems: List<CartItem>, itemId: String, size: String): List<CartItem> {
        return currentItems.filterNot { it.item.id == itemId && it.size == size }
    }

    fun buildContactMessage(itemName: String, size: String, quantity: Int): String {
        return "Hallo, ich bin an '$itemName' in Groesse $size x$quantity interessiert."
    }

    fun validateContact(request: ContactRequest): String? {
        if (request.name.isBlank() || request.email.isBlank()) {
            return "Bitte Name und E-Mail ausfuellen."
        }
        return null
    }
}
