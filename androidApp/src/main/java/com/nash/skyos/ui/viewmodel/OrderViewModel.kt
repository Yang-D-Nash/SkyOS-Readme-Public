package com.nash.skyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.AppSessionStore
import com.nash.skyos.data.repository.AndroidOrderRepository
import com.nash.skyos.ui.model.OrderUiState
import com.skydown.shared.model.Order
import com.skydown.shared.model.isPlatformOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    private val orderService = AppContainer.orderService
    private val merchOrderPaymentClient = AppContainer.merchOrderPaymentClient
    private val liveOrderRepository = AppContainer.orderRepository as? AndroidOrderRepository
    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()
    private var ordersListener: ListenerRegistration? = null

    init {
        if (liveOrderRepository != null) {
            observeOrders()
        } else {
            viewModelScope.launch {
                loadOrders()
            }
        }
    }

    fun toggleCompleted(orderId: String) {
        viewModelScope.launch {
            val currentOrder = _uiState.value.orders.firstOrNull { it.id == orderId } ?: return@launch
            val result = orderService.toggleCompleted(orderId, currentOrder.isCompleted)
            if (result.isFailure) {
                postError(result.exceptionOrNull()?.message)
                return@launch
            }
            postSuccess(if (currentOrder.isCompleted) "Markiert als offen" else "Markiert als erledigt")
            loadOrders()
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            val result = orderService.deleteOrder(orderId)
            if (result.isFailure) {
                postError(result.exceptionOrNull()?.message)
                return@launch
            }
            postSuccess("Bestellung geloescht")
            loadOrders()
        }
    }

    fun confirmPayment(orderId: String) {
        viewModelScope.launch {
            val currentOrder = _uiState.value.orders.firstOrNull { it.id == orderId } ?: return@launch
            if (currentOrder.paymentStatus == "confirmed") {
                postSuccess("Diese Bestellung ist bereits als bezahlt markiert.")
                return@launch
            }

            _uiState.update {
                it.copy(
                    confirmingPaymentOrderIds = it.confirmingPaymentOrderIds + orderId,
                    errorMessage = null,
                    successMessage = null,
                )
            }

            val result = merchOrderPaymentClient.confirmPayment(
                orderId = orderId,
                paymentMethod = currentOrder.paymentMethod,
            )

            _uiState.update {
                it.copy(
                    confirmingPaymentOrderIds = it.confirmingPaymentOrderIds - orderId,
                    errorMessage = result.exceptionOrNull()?.message,
                    successMessage = if (result.isSuccess) {
                        result.getOrNull()
                    } else {
                        null
                    },
                )
            }

            if (result.isSuccess) {
                loadOrders()
            }
        }
    }

    private suspend fun loadOrders() {
        val canManageOrders = AppSessionStore.currentUser.value?.isPlatformOwner == true
        setOrdersLoading(canManageOrders)
        val result = orderService.loadOrders()
        applyOrdersResult(result, canManageOrders)
    }

    private fun observeOrders() {
        ordersListener?.remove()
        val canManageOrders = AppSessionStore.currentUser.value?.isPlatformOwner == true
        setOrdersLoading(canManageOrders)
        ordersListener = liveOrderRepository?.observeOrders { result ->
            val currentCanManageOrders = AppSessionStore.currentUser.value?.isPlatformOwner == true
            applyOrdersResult(result, currentCanManageOrders)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            loadOrders()
        }
    }

    override fun onCleared() {
        ordersListener?.remove()
        ordersListener = null
        super.onCleared()
    }

    private fun postError(message: String?) {
        _uiState.update {
            it.copy(
                errorMessage = message,
                successMessage = null,
            )
        }
    }

    private fun postSuccess(message: String) {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = message,
            )
        }
    }

    private fun setOrdersLoading(canManageOrders: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = true,
                canManageOrders = canManageOrders,
                errorMessage = null,
            )
        }
    }

    private fun applyOrdersResult(result: Result<List<Order>>, canManageOrders: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = false,
                orders = result.getOrDefault(emptyList()),
                canManageOrders = canManageOrders,
                errorMessage = result.exceptionOrNull()?.message,
            )
        }
    }
}
