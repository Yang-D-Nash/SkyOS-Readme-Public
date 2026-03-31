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
    private val commerceSettingsRepository = AppContainer.commerceSettingsRepository
    private val merchStoreStatusRepository = AppContainer.merchStoreStatusRepository
    private val paymentMethodsRepository = AppContainer.paymentMethodsRepository
    private var commerceSettingsListener: ListenerRegistration? = null
    private var merchStoreStatusListener: ListenerRegistration? = null
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
                        isAdmin = user?.isAdmin == true,
                        name = user?.username.orEmpty(),
                        email = user?.email.orEmpty(),
                        whatsApp = user?.whatsApp.orEmpty(),
                        shippingCountry = if (it.shippingCountry.isBlank()) "Deutschland" else it.shippingCountry,
                    )
                }
            }
        }

        commerceSettingsListener = commerceSettingsRepository.observeSettings { result ->
            result.onSuccess { settings ->
                _uiState.update { it.copy(commerceSettings = settings) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Commerce-Einstellungen konnten nicht geladen werden.")
                }
            }
        }

        merchStoreStatusListener = merchStoreStatusRepository.observeStatus { result ->
            result.onSuccess { status ->
                _uiState.update { it.copy(isStoreOpen = status.isOpen) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Store-Status konnte nicht geladen werden.")
                }
            }
        }

        paymentMethodsListener = paymentMethodsRepository.observeSettings { result ->
            result.onSuccess { settings ->
                _uiState.update { current ->
                    val selected = current.selectedPaymentMethod
                        .takeIf { it.isNotBlank() && settings.checkoutMethodLabels.contains(it) }
                        ?: settings.checkoutMethodLabels.firstOrNull().orEmpty()
                    current.copy(
                        paymentMethods = settings,
                        selectedPaymentMethod = selected,
                    )
                }
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

    fun updateShippingStreet(value: String) {
        _uiState.update { it.copy(shippingStreet = value, errorMessage = null, successMessage = null) }
    }

    fun updateShippingAddressExtra(value: String) {
        _uiState.update { it.copy(shippingAddressExtra = value, errorMessage = null, successMessage = null) }
    }

    fun updateShippingPostalCode(value: String) {
        _uiState.update { it.copy(shippingPostalCode = value, errorMessage = null, successMessage = null) }
    }

    fun updateShippingCity(value: String) {
        _uiState.update { it.copy(shippingCity = value, errorMessage = null, successMessage = null) }
    }

    fun updateShippingCountry(value: String) {
        _uiState.update { it.copy(shippingCountry = value, errorMessage = null, successMessage = null) }
    }

    fun updateMessage(value: String) {
        _uiState.update { it.copy(message = value, errorMessage = null, successMessage = null) }
    }

    fun selectPaymentMethod(value: String) {
        _uiState.update { it.copy(selectedPaymentMethod = value, errorMessage = null, successMessage = null) }
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
        ) == null &&
            state.shippingStreet.isNotBlank() &&
            state.shippingPostalCode.isNotBlank() &&
            state.shippingCity.isNotBlank() &&
            state.items.isNotEmpty() &&
            (state.isStoreOpen || state.isAdmin) &&
            (state.paymentMethods.checkoutMethodLabels.isEmpty() || state.selectedPaymentMethod.isNotBlank())
    }

    suspend fun submitOrder(): Result<Unit> {
        val state = _uiState.value
        if (!state.isStoreOpen && !state.isAdmin) {
            _uiState.update {
                it.copy(
                    errorMessage = "Der Merchandise-Store ist gerade pausiert.",
                    successMessage = null,
                )
            }
            return Result.failure(IllegalStateException("Merchandise-Store pausiert."))
        }
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
        val shippingAddress = composeShippingAddress(state)
        val pricing = pricingSummary(state)
        val paymentLine = state.selectedPaymentMethod
            .takeIf { it.isNotBlank() }
            ?.let { "Gewuenschte Zahlart: $it\n\n" }
            .orEmpty()
        val result = orderService.submitOrder(
            userEmail = state.email,
            items = state.items,
            customerName = state.name,
            customerEmail = state.email,
            whatsApp = state.whatsApp,
            shippingAddress = shippingAddress,
            message = paymentLine + state.message,
            paymentMethod = state.selectedPaymentMethod,
            subtotalAmount = pricing.subtotal,
            shippingAmount = pricing.shipping,
            taxRate = pricing.taxRate,
            taxAmount = pricing.includedTax,
            totalAmount = pricing.total,
        )
        _uiState.update {
            it.copy(
                isSubmitting = false,
                items = if (result.isSuccess) emptyList() else it.items,
                selectedPaymentMethod = if (result.isSuccess) {
                    it.paymentMethods.checkoutMethodLabels.firstOrNull().orEmpty()
                } else {
                    it.selectedPaymentMethod
                },
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
        commerceSettingsListener?.remove()
        commerceSettingsListener = null
        merchStoreStatusListener?.remove()
        merchStoreStatusListener = null
        paymentMethodsListener?.remove()
        paymentMethodsListener = null
        super.onCleared()
    }

    private fun composeShippingAddress(state: CartUiState): String {
        val topLine = state.shippingStreet.trim()
        val extraLine = state.shippingAddressExtra.trim()
        val postalAndCity = listOf(
            state.shippingPostalCode.trim(),
            state.shippingCity.trim(),
        ).filter { it.isNotBlank() }.joinToString(" ")
        val country = state.shippingCountry.trim().ifBlank { "Deutschland" }

        return listOf(topLine, extraLine, postalAndCity, country)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun pricingSummary(state: CartUiState): CartPricingSummary {
        val subtotal = state.items.sumOf { it.item.price * it.quantity }
        val baseShipping = state.commerceSettings.shipping.shippingCostFor(state.shippingCountry)
        val shipping = if (
            state.commerceSettings.shipping.freeShippingThreshold > 0 &&
            subtotal >= state.commerceSettings.shipping.freeShippingThreshold
        ) {
            0.0
        } else {
            baseShipping
        }
        val total = subtotal + shipping
        val taxRate = state.commerceSettings.invoice.taxRate
        val includedTax = if (taxRate > 0) total * (taxRate / (100.0 + taxRate)) else 0.0

        return CartPricingSummary(
            subtotal = subtotal,
            shipping = shipping,
            taxRate = taxRate,
            includedTax = includedTax,
            total = total,
        )
    }
}

private data class CartPricingSummary(
    val subtotal: Double,
    val shipping: Double,
    val taxRate: Double,
    val includedTax: Double,
    val total: Double,
)
