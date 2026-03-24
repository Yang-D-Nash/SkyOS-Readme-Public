package com.skydown.android.ui.model

import com.skydown.shared.model.Order

data class OrderUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
