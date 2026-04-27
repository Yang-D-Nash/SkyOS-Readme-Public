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
        val normalizedColor = color.normalizedOptionalValue()
        val existingIndex = mutableItems.indexOfFirst {
            it.matches(itemId = item.id, size = size, color = normalizedColor)
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
        val normalizedColor = color.normalizedOptionalValue()
        return currentItems.filterNot {
            it.matches(itemId = itemId, size = size, color = normalizedColor)
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

private fun CartItem.matches(itemId: String?, size: String, color: String?): Boolean {
    return item.id == itemId &&
        this.size == size &&
        this.color.equals(color, ignoreCase = true)
}

private fun String?.normalizedOptionalValue(): String? {
    return this?.trim()?.takeIf(String::isNotEmpty)
}
