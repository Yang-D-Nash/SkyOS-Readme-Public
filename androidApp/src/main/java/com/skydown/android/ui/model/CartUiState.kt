package com.skydown.android.ui.model

import com.skydown.shared.model.CartItem

data class CartUiState(
    val isLoggedIn: Boolean = true,
    val items: List<CartItem> = emptyList(),
    val name: String = "",
    val email: String = "",
    val whatsApp: String = "",
    val message: String = "Ich interessiere mich fuer die Artikel in meinem Warenkorb.",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
