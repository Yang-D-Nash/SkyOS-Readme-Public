package com.skydown.shared.usecase

import com.skydown.shared.model.ContactRequest
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.OrderItem
import com.skydown.shared.text.SharedText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class CartAndOrderUseCaseTest {
    @Test
    fun addItem_mergesExistingItem_whenColorOnlyDiffersByCaseAndWhitespace() {
        val item = merchandiseItem(id = "hoodie-1", name = "Hoodie")
        val cartAfterFirstAdd = CartUseCase.addItem(
            currentItems = emptyList(),
            item = item,
            size = "M",
            color = " Black ",
            quantity = 1,
        )

        val cartAfterSecondAdd = CartUseCase.addItem(
            currentItems = cartAfterFirstAdd,
            item = item,
            size = "M",
            color = "black",
            quantity = 2,
        )

        assertEquals(1, cartAfterSecondAdd.size)
        assertEquals(3, cartAfterSecondAdd.first().quantity)
        assertEquals("Black", cartAfterSecondAdd.first().color)
    }

    @Test
    fun addItem_mergesExistingItem_whenSizeOnlyDiffersByCaseAndWhitespace() {
        val item = merchandiseItem(id = "hoodie-2", name = "Hoodie")
        val cartAfterFirstAdd = CartUseCase.addItem(
            currentItems = emptyList(),
            item = item,
            size = " M ",
            color = "Black",
            quantity = 1,
        )

        val cartAfterSecondAdd = CartUseCase.addItem(
            currentItems = cartAfterFirstAdd,
            item = item,
            size = "m",
            color = "BLACK",
            quantity = 2,
        )

        assertEquals(1, cartAfterSecondAdd.size)
        assertEquals(3, cartAfterSecondAdd.first().quantity)
        assertEquals("M", cartAfterSecondAdd.first().size)
        assertEquals("Black", cartAfterSecondAdd.first().color)
    }

    @Test
    fun addItem_throws_whenQuantityIsZeroOrNegative() {
        val item = merchandiseItem(id = "hoodie-3", name = "Hoodie")

        assertFailsWith<IllegalArgumentException> {
            CartUseCase.addItem(
                currentItems = emptyList(),
                item = item,
                size = "M",
                color = "Black",
                quantity = 0,
            )
        }
    }

    @Test
    fun removeItem_removesMatchingVariant_caseInsensitiveForColor() {
        val item = merchandiseItem(id = "shirt-1", name = "Shirt")
        val initial = listOf(
            CartUseCase.addItem(emptyList(), item, size = "S", color = "Blue", quantity = 1).first(),
            CartUseCase.addItem(emptyList(), item, size = "M", color = "Black", quantity = 1).first(),
        )

        val result = CartUseCase.removeItem(
            currentItems = initial,
            itemId = "shirt-1",
            size = "S",
            color = " blue ",
        )

        assertEquals(1, result.size)
        assertEquals("M", result.first().size)
    }

    @Test
    fun buildContactMessage_includesColorSegmentOnlyWhenProvided() {
        val withColor = CartUseCase.buildContactMessage(
            itemName = "Premium Tee",
            size = "L",
            color = "Sand",
            quantity = 2,
        )
        val withoutColor = CartUseCase.buildContactMessage(
            itemName = "Premium Tee",
            size = "L",
            color = "   ",
            quantity = 2,
        )

        assertEquals("Hello, I am interested in 'Premium Tee' in size L in Sand x2.", withColor)
        assertEquals("Hello, I am interested in 'Premium Tee' in size L x2.", withoutColor)
    }

    @Test
    fun validateContact_returnsErrorForMissingNameOrEmail() {
        val missingName = ContactRequest(name = "", email = "buyer@example.com", message = "Hi")
        val valid = ContactRequest(name = "Buyer", email = "buyer@example.com", message = "Hi")

        assertEquals(SharedText.CART_CONTACT_NAME_EMAIL_REQUIRED, CartUseCase.validateContact(missingName))
        assertNull(CartUseCase.validateContact(valid))
    }

    @Test
    fun mapCartItems_mapsCartStateAndGeneratesDistinctOrderIds() {
        val cartItems = listOf(
            CartUseCase.addItem(
                currentItems = emptyList(),
                item = merchandiseItem(id = "cap-1", name = "Cap"),
                size = "One Size",
                quantity = 1,
            ).first(),
            CartUseCase.addItem(
                currentItems = emptyList(),
                item = merchandiseItem(id = "tee-2", name = "Tee"),
                size = "M",
                quantity = 3,
            ).first(),
        )

        val orderItems = OrderUseCase.mapCartItems(cartItems)

        assertEquals(2, orderItems.size)
        assertOrderItemMatches(orderItems[0], "Cap", 1, "One Size")
        assertOrderItemMatches(orderItems[1], "Tee", 3, "M")
        assertNotEquals(orderItems[0].id, orderItems[1].id)
        assertEquals("cap-1", orderItems[0].productId)
        assertEquals("tee-2", orderItems[1].productId)
        assertEquals(cartItems[0].color, orderItems[0].color)
    }

    @Test
    fun mapCartItems_generatesStableIds_forSameInput() {
        val cartItems = listOf(
            CartUseCase.addItem(
                currentItems = emptyList(),
                item = merchandiseItem(id = "hoodie-10", name = "Hoodie"),
                size = "L",
                color = " Black ",
                quantity = 2,
            ).first(),
        )

        val firstMapping = OrderUseCase.mapCartItems(cartItems)
        val secondMapping = OrderUseCase.mapCartItems(cartItems)

        assertEquals(firstMapping.first().id, secondMapping.first().id)
    }

    private fun assertOrderItemMatches(orderItem: OrderItem, name: String, quantity: Int, size: String) {
        assertEquals(name, orderItem.name)
        assertEquals(quantity, orderItem.quantity)
        assertEquals(size, orderItem.size)
    }

    private fun merchandiseItem(id: String, name: String): MerchandiseItem {
        return MerchandiseItem(
            id = id,
            name = name,
            price = 39.0,
            description = "$name description",
            imageUrls = emptyList(),
            available = true,
        )
    }
}
