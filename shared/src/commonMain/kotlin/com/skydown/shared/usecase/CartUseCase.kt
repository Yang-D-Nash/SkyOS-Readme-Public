package com.skydown.shared.usecase

import com.skydown.shared.model.CartItem
import com.skydown.shared.model.ContactRequest
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.text.SharedText
import com.skydown.shared.text.formatTemplate

object CartUseCase {
    fun addItem(
        currentItems: List<CartItem>,
        item: MerchandiseItem,
        size: String,
        color: String? = null,
        quantity: Int,
        shopifyVariantId: String? = null,
        sku: String? = null,
        unitPrice: Double? = null,
    ): List<CartItem> {
        val mutableItems = currentItems.toMutableList()
        val normalizedColor = color?.trim()?.takeIf { it.isNotEmpty() }
        val existingIndex = mutableItems.indexOfFirst {
            it.item.id == item.id &&
                it.size == size &&
                it.color.equals(normalizedColor, ignoreCase = true)
        }

        if (existingIndex >= 0) {
            val existingItem = mutableItems[existingIndex]
            mutableItems[existingIndex] = existingItem.copy(quantity = existingItem.quantity + quantity)
        } else {
            mutableItems += CartItem(
                item = item,
                size = size,
                color = normalizedColor,
                quantity = quantity,
                shopifyVariantId = shopifyVariantId,
                sku = sku,
                unitPrice = unitPrice,
            )
        }

        return mutableItems
    }

    fun removeItem(currentItems: List<CartItem>, itemId: String, size: String, color: String? = null): List<CartItem> {
        return currentItems.filterNot {
            it.item.id == itemId &&
                it.size == size &&
                it.color.equals(color?.trim()?.takeIf { value -> value.isNotEmpty() }, ignoreCase = true)
        }
    }

    fun buildContactMessage(itemName: String, size: String, color: String?, quantity: Int): String {
        val colorPart = color?.takeIf { it.isNotBlank() }?.let { " in $it" }.orEmpty()
        return SharedText.CART_CONTACT_MESSAGE_TEMPLATE.formatTemplate(itemName, size, colorPart, quantity)
    }

    fun validateContact(request: ContactRequest): String? {
        if (request.name.isBlank() || request.email.isBlank()) {
            return SharedText.CART_CONTACT_NAME_EMAIL_REQUIRED
        }
        return null
    }
}
