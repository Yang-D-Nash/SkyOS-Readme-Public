package com.nash.skyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.AppCartStore
import com.nash.skyos.data.AppTextResolver
import com.nash.skyos.data.HostedCheckoutSession
import com.nash.skyos.data.ShippingService
import com.nash.skyos.R
import com.nash.skyos.ui.model.CartUiState
import com.google.firebase.firestore.ListenerRegistration
import com.skydown.shared.model.CartItem
import com.skydown.shared.model.ContactRequest
import com.skydown.shared.model.OrderSubmission
import com.skydown.shared.model.ShippingAddressData
import com.skydown.shared.model.isPlatformOwner
import com.skydown.shared.usecase.CartUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private val orderService = AppContainer.orderService
    private val hostedMerchCheckoutClient = AppContainer.hostedMerchCheckoutClient
    private val commerceSettingsRepository = AppContainer.commerceSettingsRepository
    private val merchStoreStatusRepository = AppContainer.merchStoreStatusRepository
    private val paymentMethodsRepository = AppContainer.paymentMethodsRepository
    private var commerceSettingsListener: ListenerRegistration? = null
    private var merchStoreStatusListener: ListenerRegistration? = null
    private var paymentMethodsListener: ListenerRegistration? = null
    private var pendingHostedCheckoutItems: List<CartItem>? = null
    private val _uiState = MutableStateFlow(
        CartUiState(
            isLoggedIn = false,
        ),
    )
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        isAdmin = user?.isPlatformOwner == true,
                        name = user?.username.orEmpty(),
                        email = user?.email.orEmpty(),
                        whatsApp = user?.whatsApp.orEmpty(),
                        shippingCountry = if (it.shippingCountry.isBlank()) {
                            AppTextResolver.string(R.string.cart_default_country)
                        } else {
                            it.shippingCountry
                        },
                    )
                }
            }
        }

        viewModelScope.launch {
            AppCartStore.items.collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }

        commerceSettingsListener = commerceSettingsRepository.observeSettings { result ->
            result.onSuccess { settings ->
                _uiState.update { it.copy(commerceSettings = settings) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: AppTextResolver.string(R.string.cart_error_commerce_settings_load_failed))
                }
            }
        }

        merchStoreStatusListener = merchStoreStatusRepository.observeStatus { result ->
            result.onSuccess { status ->
                _uiState.update { it.copy(isStoreOpen = status.isOpen) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: AppTextResolver.string(R.string.cart_error_store_status_load_failed))
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

    fun removeItem(itemId: String, size: String, color: String? = null) {
        _uiState.update {
            val updatedItems = CartUseCase.removeItem(
                currentItems = it.items,
                itemId = itemId,
                size = size,
                color = color,
            )
            AppCartStore.setItems(updatedItems)
            it.copy(items = updatedItems)
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
                    errorMessage = AppTextResolver.string(R.string.cart_error_store_paused),
                    successMessage = null,
                )
            }
            return Result.failure(IllegalStateException(AppTextResolver.string(R.string.cart_error_store_paused_short)))
        }
        val mixedFulfillmentError = mixedFulfillmentError(state)
        if (mixedFulfillmentError != null) {
            _uiState.update {
                it.copy(
                    errorMessage = mixedFulfillmentError,
                    successMessage = null,
                )
            }
            return Result.failure(IllegalStateException(mixedFulfillmentError))
        }
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
        val shippingAddress = composeShippingAddress(state)
        val pricing = pricingSummary(state).getOrElse { error ->
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = error.message,
                    successMessage = null,
                )
            }
            return Result.failure(error)
        }
        val isZeroCostOrder = pricing.total <= 0.01
        val paymentLine = state.selectedPaymentMethod
            ?.takeIf { !isZeroCostOrder && it.isNotBlank() }
            ?.let { AppTextResolver.string(R.string.cart_payment_method_line, it) }
            .orEmpty()
        val countryCode = ShippingService.resolveCountryCode(state.shippingCountry).getOrElse { error ->
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = error.message,
                    successMessage = null,
                )
            }
            return Result.failure(error)
        }
        val submission = buildOrderSubmission(
            state = state,
            shippingAddress = shippingAddress,
            countryCode = countryCode,
            pricing = pricing,
            paymentLine = paymentLine,
        )
        val result = orderService.submitOrder(submission)
        if (result.isSuccess) {
            pendingHostedCheckoutItems = null
            AppCartStore.clear()
        }
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
                successMessage = if (result.isSuccess) {
                    if (isZeroCostOrder) {
                        AppTextResolver.string(R.string.cart_success_order_confirmed)
                    } else {
                        AppTextResolver.string(R.string.cart_success_order_submitted)
                    }
                } else {
                    null
                },
            )
        }
        return result.map { }
    }

    suspend fun startHostedCheckout(): Result<HostedCheckoutSession> {
        val state = _uiState.value
        if (!state.isStoreOpen && !state.isAdmin) {
            _uiState.update {
                it.copy(
                    errorMessage = AppTextResolver.string(R.string.cart_error_store_paused),
                    successMessage = null,
                )
            }
            return Result.failure(IllegalStateException(AppTextResolver.string(R.string.cart_error_store_paused_short)))
        }
        val mixedFulfillmentError = mixedFulfillmentError(state)
        if (mixedFulfillmentError != null) {
            _uiState.update {
                it.copy(
                    errorMessage = mixedFulfillmentError,
                    successMessage = null,
                )
            }
            return Result.failure(IllegalStateException(mixedFulfillmentError))
        }
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
        val shippingAddress = composeShippingAddress(state)
        val pricing = pricingSummary(state).getOrElse { error ->
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = error.message,
                    successMessage = null,
                )
            }
            return Result.failure(error)
        }
        val paymentMethod = state.selectedPaymentMethod.takeIf { it.isNotBlank() }.orEmpty()
        if (paymentMethod !in setOf("Stripe", "Klarna")) {
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = AppTextResolver.string(R.string.cart_error_hosted_checkout_unavailable),
                    successMessage = null,
                )
            }
            return Result.failure(IllegalArgumentException(AppTextResolver.string(R.string.cart_error_checkout_unavailable_short)))
        }
        val isZeroCostOrder = pricing.total <= 0.01
        val paymentLine = if (isZeroCostOrder) {
            ""
        } else {
            AppTextResolver.string(R.string.cart_payment_method_line, paymentMethod)
        }
        val countryCode = ShippingService.resolveCountryCode(state.shippingCountry).getOrElse { error ->
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = error.message,
                    successMessage = null,
                )
            }
            return Result.failure(error)
        }
        val submission = buildOrderSubmission(
            state = state,
            shippingAddress = shippingAddress,
            countryCode = countryCode,
            pricing = pricing,
            paymentLine = paymentLine,
        )
        val result = hostedMerchCheckoutClient.startCheckout(
            submission = submission,
            paymentMethod = paymentMethod,
            platform = "android",
        )
        if (result.isSuccess) {
            pendingHostedCheckoutItems = state.items
            AppCartStore.clear()
        }
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
                successMessage = if (result.isSuccess) {
                    if (isZeroCostOrder) {
                        AppTextResolver.string(R.string.cart_success_order_confirmed)
                    } else {
                        AppTextResolver.string(R.string.cart_success_checkout_opened)
                    }
                } else {
                    null
                },
            )
        }
        return result
    }

    fun handleCheckoutRedirect(status: com.nash.skyos.data.CheckoutRedirectStatus) {
        val restoredItems = if (status == com.nash.skyos.data.CheckoutRedirectStatus.Cancel) {
            pendingHostedCheckoutItems
        } else {
            null
        }
        if (restoredItems != null && _uiState.value.items.isEmpty()) {
            AppCartStore.setItems(restoredItems)
        }
        _uiState.update {
            it.copy(
                items = if (restoredItems != null && it.items.isEmpty()) restoredItems else it.items,
                errorMessage = when (status) {
                    com.nash.skyos.data.CheckoutRedirectStatus.Cancel -> if (restoredItems != null) {
                        AppTextResolver.string(R.string.cart_error_checkout_cancelled_restored)
                    } else {
                        AppTextResolver.string(R.string.cart_error_checkout_cancelled_unpaid)
                    }
                    com.nash.skyos.data.CheckoutRedirectStatus.Success -> null
                },
                successMessage = when (status) {
                    com.nash.skyos.data.CheckoutRedirectStatus.Success -> AppTextResolver.string(R.string.cart_success_checkout_completed)
                    com.nash.skyos.data.CheckoutRedirectStatus.Cancel -> null
                },
            )
        }
        pendingHostedCheckoutItems = null
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
        val country = state.shippingCountry.trim().ifBlank { AppTextResolver.string(R.string.cart_default_country) }

        return listOf(topLine, extraLine, postalAndCity, country)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun pricingSummary(state: CartUiState): Result<CartPricingSummary> {
        val subtotal = state.items.sumOf { (it.unitPrice ?: it.item.price) * it.quantity }
        val countryCode = ShippingService.resolveCountryCode(state.shippingCountry)
            .getOrElse { return Result.failure(it) }
        val shippingQuote = ShippingService.calculateShippingPrice(
            settings = state.commerceSettings.shipping,
            countryCode = countryCode,
            items = state.items,
            subtotal = subtotal,
        ).getOrElse { return Result.failure(it) }
        val total = subtotal + shippingQuote.price
        val taxRate = state.commerceSettings.invoice.taxRate
        val includedTax = if (taxRate > 0) total * (taxRate / (100.0 + taxRate)) else 0.0

        return Result.success(
            CartPricingSummary(
                subtotal = subtotal,
                shipping = shippingQuote.price,
                taxRate = taxRate,
                includedTax = includedTax,
                total = total,
                zone = shippingQuote.zone,
                countryCode = shippingQuote.countryCode,
            ),
        )
    }

    private fun deriveFulfillmentProvider(items: List<com.skydown.shared.model.CartItem>): String {
        return if (items.any { !it.shopifyVariantId.isNullOrBlank() }) "podpartner" else "manual"
    }

    private fun buildOrderSubmission(
        state: CartUiState,
        shippingAddress: String,
        countryCode: String,
        pricing: CartPricingSummary,
        paymentLine: String,
    ): OrderSubmission {
        return OrderSubmission(
            userEmail = state.email,
            items = state.items,
            customerName = state.name,
            customerEmail = state.email,
            whatsApp = state.whatsApp,
            shippingAddress = shippingAddress,
            shippingAddressData = ShippingAddressData(
                address1 = state.shippingStreet.trim(),
                address2 = state.shippingAddressExtra.trim(),
                city = state.shippingCity.trim(),
                zip = state.shippingPostalCode.trim(),
                countryCode = countryCode,
                countryName = state.shippingCountry.trim(),
            ),
            shippingZone = pricing.zone.name,
            shippingCountryCode = countryCode,
            message = paymentLine + state.message,
            paymentMethod = state.selectedPaymentMethod,
            paymentStatus = "pending",
            subtotalAmount = pricing.subtotal,
            shippingAmount = pricing.shipping,
            taxRate = pricing.taxRate,
            taxAmount = pricing.includedTax,
            totalAmount = pricing.total,
            fulfillmentProvider = deriveFulfillmentProvider(state.items),
        )
    }

    private fun mixedFulfillmentError(state: CartUiState): String? {
        val hasShopifyItems = state.items.any { !it.shopifyVariantId.isNullOrBlank() }
        val hasLegacyItems = state.items.any { it.shopifyVariantId.isNullOrBlank() }
        if (hasShopifyItems && hasLegacyItems) {
            return AppTextResolver.string(R.string.cart_error_mixed_fulfillment)
        }
        return null
    }
}

private data class CartPricingSummary(
    val subtotal: Double,
    val shipping: Double,
    val taxRate: Double,
    val includedTax: Double,
    val total: Double,
    val zone: com.nash.skyos.data.ShippingZone,
    val countryCode: String,
)
