package com.skydown.android.ui.model

import com.skydown.android.data.CommerceSettings
import com.skydown.android.data.PaymentMethodsSettings
import com.skydown.shared.model.CartItem

data class CartUiState(
    val isLoggedIn: Boolean = true,
    val isAdmin: Boolean = false,
    val isStoreOpen: Boolean = true,
    val items: List<CartItem> = emptyList(),
    val name: String = "",
    val email: String = "",
    val whatsApp: String = "",
    val shippingStreet: String = "",
    val shippingAddressExtra: String = "",
    val shippingPostalCode: String = "",
    val shippingCity: String = "",
    val shippingCountry: String = "Deutschland",
    val message: String = "Ich interessiere mich fuer die Artikel in meinem Warenkorb.",
    val selectedPaymentMethod: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val commerceSettings: CommerceSettings = CommerceSettings(),
    val paymentMethods: PaymentMethodsSettings = PaymentMethodsSettings(),
)
