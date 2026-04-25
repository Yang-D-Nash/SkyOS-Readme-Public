package com.nash.skyos.ui.model

import com.skydown.shared.model.Order

data class OrderUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val canManageOrders: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val confirmingPaymentOrderIds: Set<String> = emptySet(),
)
