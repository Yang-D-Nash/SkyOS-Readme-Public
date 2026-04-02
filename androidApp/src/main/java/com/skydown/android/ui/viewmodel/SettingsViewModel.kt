package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppearancePreferences
import com.skydown.android.data.AiVisualReferenceLibraryPreferences
import com.skydown.android.data.BankTransferSettings
import com.skydown.android.data.CommerceSettings
import com.skydown.android.data.PaymentMethodsSettings
import com.skydown.android.data.ShopifyAdminSettings
import com.skydown.android.data.WorkflowAutomationPreferences
import com.skydown.shared.model.User
import com.google.firebase.firestore.ListenerRegistration
import com.skydown.android.ui.model.SettingsUiState
import com.skydown.android.ui.theme.AppearanceMode
import com.skydown.shared.model.isPlatformOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val authService = AppContainer.authService
    private val commerceSettingsRepository = AppContainer.commerceSettingsRepository
    private val paymentMethodsRepository = AppContainer.paymentMethodsRepository
    private val shopifyAdminSettingsRepository = AppContainer.shopifyAdminSettingsRepository
    private val adminUserManagementRepository = AppContainer.adminUserManagementRepository
    private var commerceSettingsListener: ListenerRegistration? = null
    private var paymentMethodsListener: ListenerRegistration? = null
    private var shopifyAdminSettingsListener: ListenerRegistration? = null
    private var adminUsersListener: ListenerRegistration? = null
    private val _uiState = MutableStateFlow(
        SettingsUiState(),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.refreshCurrentUser()
            AppContainer.currentUser.collect { user ->
                val isOwner = user?.isPlatformOwner == true
                WorkflowAutomationPreferences.setAdminMode(
                    if (isOwner) user?.id else null,
                )
                val displayName = user?.username
                    ?.takeIf { it.isNotBlank() }
                    ?: user?.email
                        ?.substringBefore("@")
                        ?.takeIf { it.isNotBlank() }
                    ?: if (user != null) "Skydown User" else ""

                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        currentUserId = user?.id,
                        username = displayName,
                        email = user?.email.orEmpty(),
                        isOwner = isOwner,
                        accountErrorMessage = null,
                    )
                }

                configureManagedUsersObservation(isEnabled = isOwner)
            }
        }

        viewModelScope.launch {
            AppearancePreferences.appearanceMode.collect { mode ->
                _uiState.update { it.copy(colorScheme = mode) }
            }
        }

        viewModelScope.launch {
            AiVisualReferenceLibraryPreferences.settings.collect { settings ->
                _uiState.update { it.copy(aiVisualReferenceLibrary = settings) }
            }
        }

        viewModelScope.launch {
            WorkflowAutomationPreferences.settings.collect { settings ->
                _uiState.update { it.copy(workflowAutomationSettings = settings) }
            }
        }

        paymentMethodsListener = paymentMethodsRepository.observeSettings { result ->
            result.onSuccess { settings ->
                _uiState.update {
                    it.copy(
                        paymentMethods = settings,
                        paymentFeedbackMessage = null,
                        isPaymentFeedbackError = false,
                    )
                }
            }.onFailure { error ->
                showPaymentFeedback(
                    message = error.message ?: "Zahlungseinstellungen konnten nicht geladen werden.",
                    isError = true,
                )
            }
        }

        commerceSettingsListener = commerceSettingsRepository.observeSettings { result ->
            result.onSuccess { settings ->
                _uiState.update { it.copy(commerceSettings = settings) }
            }.onFailure { error ->
                showPaymentFeedback(
                    message = error.message ?: "Commerce-Einstellungen konnten nicht geladen werden.",
                    isError = true,
                )
            }
        }

        shopifyAdminSettingsListener = shopifyAdminSettingsRepository.observeSettings { result ->
            result.onSuccess { settings ->
                _uiState.update { it.copy(shopifyAdminSettings = settings) }
            }.onFailure { error ->
                showPaymentFeedback(
                    message = error.message ?: "Shopify-Einstellungen konnten nicht geladen werden.",
                    isError = true,
                )
            }
        }
    }

    fun updateNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun updateColorScheme(colorScheme: AppearanceMode) {
        AppearancePreferences.updateAppearanceMode(colorScheme)
    }

    fun updateAiVisualReferenceEnabled(isEnabled: Boolean) {
        AiVisualReferenceLibraryPreferences.updateEnabled(isEnabled)
    }

    fun updateAiVisualStorageLink(storageLink: String) {
        AiVisualReferenceLibraryPreferences.updateStorageLink(storageLink)
    }

    fun updateAiVisualNamingPrefix(namingPrefix: String) {
        AiVisualReferenceLibraryPreferences.updateNamingPrefix(namingPrefix)
    }

    fun updateAiVisualReferenceHint(index: Int, value: String) {
        AiVisualReferenceLibraryPreferences.updateReferenceHint(index, value)
    }

    fun saveWorkflowAutomationSettings(settings: com.skydown.android.data.WorkflowAutomationSettings) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf n8n verwalten.",
                    isError = true,
                )
                return@launch
            }
            val result = WorkflowAutomationPreferences.saveSettings(settings)
            if (result.isSuccess) {
                _uiState.update { it.copy(workflowAutomationSettings = settings) }
                showPaymentFeedback(
                    message = "n8n gespeichert. Die App bleibt normal eingeloggt und schickt nur serverseitig geprueften User-Kontext an deinen Workflow.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "n8n konnte nicht gespeichert werden.",
                    isError = true,
                )
            }
        }
    }

    fun testWorkflowAutomationSettings(settings: com.skydown.android.data.WorkflowAutomationSettings) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf n8n testen.",
                    isError = true,
                )
                return@launch
            }
            val saveResult = WorkflowAutomationPreferences.saveSettings(settings)
            if (saveResult.isFailure) {
                showPaymentFeedback(
                    message = saveResult.exceptionOrNull()?.message ?: "n8n konnte nicht gespeichert werden.",
                    isError = true,
                )
                return@launch
            }

            _uiState.update { it.copy(workflowAutomationSettings = settings) }

            val testResult = WorkflowAutomationPreferences.triggerTest()
            if (testResult.isSuccess) {
                showPaymentFeedback(
                    message = testResult.getOrNull() ?: "Test an n8n gesendet.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = testResult.exceptionOrNull()?.message ?: "n8n-Test fehlgeschlagen.",
                    isError = true,
                )
            }
        }
    }

    fun saveCommerceSettings(settings: CommerceSettings, successMessage: String = "Commerce-Einstellungen gespeichert.") {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf Versand- und Commerce-Einstellungen verwalten.",
                    isError = true,
                )
                return@launch
            }
            val result = commerceSettingsRepository.updateSettings(settings)
            if (result.isSuccess) {
                showPaymentFeedback(
                    message = successMessage,
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "Commerce-Einstellungen konnten nicht gespeichert werden.",
                    isError = true,
                )
            }
        }
    }

    fun saveShopifyAdminSettings(settings: ShopifyAdminSettings) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf Shopify verwalten.",
                    isError = true,
                )
                return@launch
            }
            val result = shopifyAdminSettingsRepository.updateSettings(settings)
            if (result.isSuccess) {
                _uiState.update { it.copy(shopifyAdminSettings = settings) }
                showPaymentFeedback(
                    message = "Shopify-Einstellungen gespeichert. Der naechste Sync nutzt jetzt diesen Store, deinen Storefront Token und optional die Collection.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "Shopify-Einstellungen konnten nicht gespeichert werden.",
                    isError = true,
                )
            }
        }
    }

    fun saveManagedUser(user: User) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf Konten verwalten.",
                    isError = true,
                )
                return@launch
            }

            val result = adminUserManagementRepository.updateUser(user)
            if (result.isSuccess) {
                showPaymentFeedback(
                    message = "Konto gespeichert. Rolle und KI-Limits wurden aktualisiert.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "Konto konnte nicht gespeichert werden.",
                    isError = true,
                )
            }
        }
    }

    fun connectStripe(accountHint: String) {
        updatePaymentMethods("Stripe verbunden.") { current ->
            current.copy(
                stripe = current.stripe.copy(
                    connected = true,
                    accountHint = accountHint.trim(),
                ),
            )
        }
    }

    fun disconnectStripe() {
        updatePaymentMethods("Stripe getrennt.") { current ->
            current.copy(
                stripe = current.stripe.copy(
                    connected = false,
                    enabled = false,
                    accountHint = "",
                ),
            )
        }
    }

    fun setStripeEnabled(enabled: Boolean) {
        updatePaymentMethods(
            message = if (enabled) "Stripe im Checkout sichtbar." else "Stripe im Checkout ausgeblendet.",
        ) { current ->
            current.copy(
                stripe = current.stripe.copy(
                    enabled = enabled && current.stripe.connected,
                ),
            )
        }
    }

    fun connectPayPal(accountHint: String) {
        if (accountHint.isBlank()) {
            showPaymentFeedback(
                message = "Bitte zuerst einen PayPal.Me-Link oder eine Business-Mail hinterlegen.",
                isError = true,
            )
            return
        }

        updatePaymentMethods("PayPal verbunden.") { current ->
            current.copy(
                paypal = current.paypal.copy(
                    connected = true,
                    accountHint = accountHint.trim(),
                ),
            )
        }
    }

    fun connectKlarna(accountHint: String) {
        updatePaymentMethods("Klarna verbunden.") { current ->
            current.copy(
                klarna = current.klarna.copy(
                    connected = true,
                    accountHint = accountHint.trim(),
                ),
            )
        }
    }

    fun disconnectPayPal() {
        updatePaymentMethods("PayPal getrennt.") { current ->
            current.copy(
                paypal = current.paypal.copy(
                    connected = false,
                    enabled = false,
                    accountHint = "",
                ),
            )
        }
    }

    fun disconnectKlarna() {
        updatePaymentMethods("Klarna getrennt.") { current ->
            current.copy(
                klarna = current.klarna.copy(
                    connected = false,
                    enabled = false,
                    accountHint = "",
                ),
            )
        }
    }

    fun setPayPalEnabled(enabled: Boolean) {
        updatePaymentMethods(
            message = if (enabled) "PayPal im Checkout sichtbar." else "PayPal im Checkout ausgeblendet.",
        ) { current ->
            current.copy(
                paypal = current.paypal.copy(
                    enabled = enabled && current.paypal.connected,
                ),
            )
        }
    }

    fun setKlarnaEnabled(enabled: Boolean) {
        updatePaymentMethods(
            message = if (enabled) "Klarna im Checkout sichtbar." else "Klarna im Checkout ausgeblendet.",
        ) { current ->
            current.copy(
                klarna = current.klarna.copy(
                    enabled = enabled && current.klarna.connected,
                ),
            )
        }
    }

    fun saveBankTransfer(
        accountHolder: String,
        iban: String,
        bic: String,
        bankName: String,
        paymentInstructions: String,
    ) {
        val updatedBankSettings = BankTransferSettings(
            enabled = _uiState.value.paymentMethods.bankTransfer.enabled,
            accountHolder = accountHolder.trim(),
            iban = iban.trim(),
            bic = bic.trim(),
            bankName = bankName.trim(),
            paymentInstructions = paymentInstructions.trim(),
        )

        if (!updatedBankSettings.isConfigured) {
            showPaymentFeedback(
                message = "Bitte mindestens Kontoinhaber, IBAN und Bankname hinterlegen.",
                isError = true,
            )
            return
        }

        updatePaymentMethods("Bankdaten gespeichert.") { current ->
            current.copy(bankTransfer = updatedBankSettings)
        }
    }

    fun setBankTransferEnabled(enabled: Boolean) {
        updatePaymentMethods(
            message = if (enabled) "Bankueberweisung im Checkout sichtbar." else "Bankueberweisung im Checkout ausgeblendet.",
        ) { current ->
            current.copy(
                bankTransfer = current.bankTransfer.copy(
                    enabled = enabled && current.bankTransfer.isConfigured,
                ),
            )
        }
    }

    fun clearPaymentFeedback() {
        _uiState.update {
            it.copy(
                paymentFeedbackMessage = null,
                isPaymentFeedbackError = false,
            )
        }
    }

    fun signOut(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSigningOut = true,
                    accountErrorMessage = null,
                )
            }

            val result = authService.signOut()

            if (result.isSuccess) {
                _uiState.update { it.copy(isSigningOut = false) }
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isSigningOut = false,
                        accountErrorMessage = result.exceptionOrNull()?.message
                            ?: "Abmelden fehlgeschlagen.",
                    )
                }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeletingAccount = true,
                    accountErrorMessage = null,
                )
            }

            val result = authService.deleteCurrentAccount()

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        isLoggedIn = false,
                        username = "",
                        email = "",
                        isOwner = false,
                        accountErrorMessage = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        accountErrorMessage = result.exceptionOrNull()?.message
                            ?: "Konto konnte nicht geloescht werden.",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        commerceSettingsListener?.remove()
        commerceSettingsListener = null
        paymentMethodsListener?.remove()
        paymentMethodsListener = null
        shopifyAdminSettingsListener?.remove()
        shopifyAdminSettingsListener = null
        adminUsersListener?.remove()
        adminUsersListener = null
        super.onCleared()
    }

    private fun configureManagedUsersObservation(isEnabled: Boolean) {
        if (!isEnabled) {
            adminUsersListener?.remove()
            adminUsersListener = null
            _uiState.update {
                it.copy(
                    managedUsers = emptyList(),
                    managedUsersErrorMessage = null,
                )
            }
            return
        }

        if (adminUsersListener != null) {
            return
        }

        adminUsersListener = adminUserManagementRepository.observeUsers { result ->
            result.onSuccess { users ->
                _uiState.update {
                    it.copy(
                        managedUsers = users,
                        managedUsersErrorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        managedUsersErrorMessage = error.message ?: "Konten konnten nicht geladen werden.",
                    )
                }
            }
        }
    }

    private fun updatePaymentMethods(
        message: String,
        transform: (PaymentMethodsSettings) -> PaymentMethodsSettings,
    ) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf Zahlarten verwalten.",
                    isError = true,
                )
                return@launch
            }

            val current = _uiState.value.paymentMethods
            val updated = transform(current)
            val result = paymentMethodsRepository.updateSettings(updated)

            if (result.isSuccess) {
                showPaymentFeedback(message = message, isError = false)
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message
                        ?: "Zahlungseinstellungen konnten nicht gespeichert werden.",
                    isError = true,
                )
            }
        }
    }

    private fun showPaymentFeedback(message: String, isError: Boolean) {
        _uiState.update {
            it.copy(
                paymentFeedbackMessage = message,
                isPaymentFeedbackError = isError,
            )
        }
    }
}
