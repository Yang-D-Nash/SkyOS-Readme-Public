package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.model.OrderUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    private val orderService = AppContainer.orderService
    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadOrders()
        }
    }

    fun toggleCompleted(orderId: String) {
        viewModelScope.launch {
            val currentOrder = _uiState.value.orders.firstOrNull { it.id == orderId } ?: return@launch
            val result = orderService.toggleCompleted(orderId, currentOrder.isCompleted)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    successMessage = if (currentOrder.isCompleted) "Markiert als offen" else "Markiert als erledigt",
                )
            }
            loadOrders()
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            val result = orderService.deleteOrder(orderId)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    successMessage = "Bestellung geloescht",
                )
            }
            loadOrders()
        }
    }

    private suspend fun loadOrders() {
        _uiState.update { it.copy(isLoading = true) }
        val result = orderService.loadOrders()
        _uiState.update {
            it.copy(
                isLoading = false,
                orders = result.getOrDefault(emptyList()),
                errorMessage = result.exceptionOrNull()?.message ?: it.errorMessage,
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
