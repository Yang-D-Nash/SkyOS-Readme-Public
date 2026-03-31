package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.model.CartUiState
import com.google.firebase.firestore.ListenerRegistration
import com.skydown.shared.model.ContactRequest
import com.skydown.shared.model.sampleMerchandiseItems
import com.skydown.shared.usecase.CartUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private val orderService = AppContainer.orderService
    private val paymentMethodsRepository = AppContainer.paymentMethodsRepository
    private var paymentMethodsListener: ListenerRegistration? = null
    private val initialItems = listOf(
        CartUseCase.addItem(
            currentItems = emptyList(),
            item = sampleMerchandiseItems().first(),
            size = "M",
            quantity = 1,
        ).first(),
    )

    private val _uiState = MutableStateFlow(
        CartUiState(
            isLoggedIn = false,
            items = initialItems,
        ),
    )
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.refreshCurrentUser()
            AppContainer.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        name = user?.username.orEmpty(),
                        email = user?.email.orEmpty(),
                        whatsApp = user?.whatsApp.orEmpty(),
                    )
                }
            }
        }

        paymentMethodsListener = paymentMethodsRepository.observeSettings { result ->
            result.onSuccess { settings ->
                _uiState.update { it.copy(paymentMethods = settings) }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null, successMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, successMessage = null) }
    }

    fun updateWhatsApp(value: String) {
        _uiState.update { it.copy(whatsApp = value, errorMessage = null, successMessage = null) }
    }

    fun updateMessage(value: String) {
        _uiState.update { it.copy(message = value, errorMessage = null, successMessage = null) }
    }

    fun removeItem(itemId: String, size: String) {
        _uiState.update {
            it.copy(items = CartUseCase.removeItem(it.items, itemId = itemId, size = size))
        }
    }

    fun isFormValid(): Boolean {
        val state = _uiState.value
        return CartUseCase.validateContact(
            ContactRequest(
                name = state.name,
                email = state.email,
                whatsAppNumber = state.whatsApp,
                message = state.message,
            ),
        ) == null && state.items.isNotEmpty()
    }

    suspend fun submitOrder(): Result<Unit> {
        val state = _uiState.value
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
        val result = orderService.submitOrder(
            userEmail = state.email,
            items = state.items,
            customerName = state.name,
            customerEmail = state.email,
            whatsApp = state.whatsApp,
            message = state.message,
        )
        _uiState.update {
            it.copy(
                isSubmitting = false,
                items = if (result.isSuccess) emptyList() else it.items,
                errorMessage = result.exceptionOrNull()?.message,
                successMessage = if (result.isSuccess) "Bestellung erfolgreich abgeschickt!" else null,
            )
        }
        return result
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    override fun onCleared() {
        paymentMethodsListener?.remove()
        paymentMethodsListener = null
        super.onCleared()
    }
}
