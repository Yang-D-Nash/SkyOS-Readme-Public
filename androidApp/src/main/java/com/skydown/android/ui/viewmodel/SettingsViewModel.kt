package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppearancePreferences
import com.skydown.android.data.AgentProfilePreferences
import com.skydown.android.data.AiVisualReferenceLibraryPreferences
import com.skydown.android.data.AppLanguageSupport
import com.skydown.android.data.BankTransferSettings
import com.skydown.android.data.CommerceSettings
import com.skydown.android.data.LegalContentSettings
import com.skydown.android.data.ManusByosPreferences
import com.skydown.android.data.PaymentMethodsSettings
import com.skydown.android.data.ShopifyAdminSettings
import com.skydown.android.data.WorkflowAutomationPreferences
import com.skydown.android.data.callWithAppCheckRetry
import com.skydown.shared.model.User
import com.skydown.shared.model.ProfileUpdateInput
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
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
    private val aiPromptSettingsRepository = AppContainer.aiPromptSettingsRepository
    private val aiRuntimeSettingsRepository = AppContainer.aiRuntimeSettingsRepository
    private val commerceSettingsRepository = AppContainer.commerceSettingsRepository
    private val legalContentRepository = AppContainer.legalContentRepository
    private val paymentMethodsRepository = AppContainer.paymentMethodsRepository
    private val stripeBackendSecretsRepository = AppContainer.stripeBackendSecretsRepository
    private val shopifyAdminSettingsRepository = AppContainer.shopifyAdminSettingsRepository
    private val adminUserManagementRepository = AppContainer.adminUserManagementRepository
    private var commerceSettingsListener: ListenerRegistration? = null
    private var paymentMethodsListener: ListenerRegistration? = null
    private var stripeBackendSecretsListener: ListenerRegistration? = null
    private var aiPromptSettingsListener: ListenerRegistration? = null
    private var aiRuntimeSettingsListener: ListenerRegistration? = null
    private var shopifyAdminSettingsListener: ListenerRegistration? = null
    private var adminUsersListener: ListenerRegistration? = null
    private val functions = FirebaseFunctions.getInstance("us-central1")
    private val _uiState = MutableStateFlow(
        SettingsUiState(),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                language = AppLanguageSupport.currentSystemLanguageDisplayName(),
            )
        }

        viewModelScope.launch {
            AppContainer.currentUser.collect { user ->
                val isOwner = user?.isPlatformOwner == true
                WorkflowAutomationPreferences.setUserMode(user?.id)
                AgentProfilePreferences.setUserMode(user?.id)
                ManusByosPreferences.setUserMode(user?.id)
                val displayName = user?.username
                    ?.takeIf { it.isNotBlank() }
                    ?: user?.email
                        ?.substringBefore("@")
                        ?.takeIf { it.isNotBlank() }
                    ?: if (user != null) "SkyOS User" else ""

                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        currentUserId = user?.id,
                        username = displayName,
                        email = user?.email.orEmpty(),
                        whatsApp = user?.whatsApp.orEmpty(),
                        profileTagline = user?.profileTagline.orEmpty(),
                        profileBio = user?.profileBio.orEmpty(),
                        instagramHandle = user?.instagramHandle.orEmpty(),
                        aiAccessEnabled = user?.aiAccessEnabled ?: true,
                        isOwner = isOwner,
                        accountErrorMessage = null,
                    )
                }

                if (isOwner) {
                    refreshShopifyCollections()
                } else {
                    _uiState.update {
                        it.copy(
                            availableShopifyCollections = emptyList(),
                            isLoadingShopifyCollections = false,
                            shopifyCollectionsErrorMessage = null,
                        )
                    }
                }

                configureStripeBackendSecretsObservation(isEnabled = isOwner)
                configureAiPromptSettingsObservation(isEnabled = isOwner)
                configureAiRuntimeSettingsObservation(isEnabled = isOwner)
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

        viewModelScope.launch {
            AgentProfilePreferences.settings.collect { settings ->
                _uiState.update { it.copy(agentProfileSettings = settings) }
            }
        }

        viewModelScope.launch {
            ManusByosPreferences.settings.collect { settings ->
                _uiState.update { current ->
                    val validationStatus = when {
                        !settings.hasApiKey -> "awaiting_external_auth"
                        !settings.isEnabled -> "fallback_internal"
                        current.manusValidationStatus == "unvalidated" -> "fallback_internal"
                        else -> current.manusValidationStatus
                    }
                    val validationMessage = when (validationStatus) {
                        "awaiting_external_auth" -> "Kein Key gespeichert. Externer Lauf wartet auf Auth."
                        "fallback_internal" -> "BYOS pausiert oder ungeprueft. Agent nutzt internen Fallback."
                        else -> current.manusValidationMessage
                    }
                    current.copy(
                        manusByosSettings = settings,
                        manusValidationStatus = validationStatus,
                        manusValidationMessage = validationMessage,
                    )
                }
            }
        }

        viewModelScope.launch {
            legalContentRepository.settings.collect { settings ->
                _uiState.update { it.copy(legalContentSettings = settings) }
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

    fun refreshSystemLanguage() {
        _uiState.update {
            it.copy(language = AppLanguageSupport.currentSystemLanguageDisplayName())
        }
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
            if (_uiState.value.currentUserId.isNullOrBlank()) {
                showPaymentFeedback(
                    message = "Bitte melde dich an, um deinen Agent-Service zu speichern.",
                    isError = true,
                )
                return@launch
            }
            val result = WorkflowAutomationPreferences.saveSettings(settings)
            if (result.isSuccess) {
                _uiState.update { it.copy(workflowAutomationSettings = settings) }
                showPaymentFeedback(
                    message = "Agent-Service gespeichert. Dein Konto nutzt jetzt diesen Workflow fuer Aktionen.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "Agent-Service konnte nicht gespeichert werden.",
                    isError = true,
                )
            }
        }
    }

    fun saveManusByosSettings(
        enabled: Boolean,
        apiKeyDraft: String,
    ) {
        viewModelScope.launch {
            if (_uiState.value.currentUserId.isNullOrBlank()) {
                showPaymentFeedback(
                    message = "Bitte melde dich an, um deinen Manus-Service zu speichern.",
                    isError = true,
                )
                return@launch
            }

            val trimmedApiKey = apiKeyDraft.trim()
            val hasExistingKey = _uiState.value.manusByosSettings.hasApiKey
            if (trimmedApiKey.isBlank() && enabled && !hasExistingKey) {
                showPaymentFeedback(
                    message = "Bitte hinterlege zuerst einen Manus API Key.",
                    isError = true,
                )
                return@launch
            }

            if (trimmedApiKey.isNotBlank()) {
                val saveKeyResult = ManusByosPreferences.saveApiKey(trimmedApiKey)
                if (saveKeyResult.isFailure) {
                    showPaymentFeedback(
                        message = saveKeyResult.exceptionOrNull()?.message
                            ?: "Manus API Key konnte nicht lokal gespeichert werden.",
                        isError = true,
                    )
                    return@launch
                }
            }

            val updateEnabledResult = ManusByosPreferences.updateEnabled(enabled)
            if (updateEnabledResult.isSuccess) {
                updateEnabledResult.getOrNull()?.let { settings ->
                    _uiState.update { it.copy(manusByosSettings = settings) }
                }
                _uiState.update {
                    it.copy(
                        manusValidationStatus = "fallback_internal",
                        manusValidationMessage = if (enabled) {
                            "Noch nicht validiert. Agent nutzt BYOS oder faellt intern zurueck."
                        } else {
                            "BYOS pausiert. Agent nutzt internen Fallback."
                        },
                    )
                }
                showPaymentFeedback(
                    message = if (enabled) {
                        "Manus BYOS aktiv. Der Agent nutzt jetzt deinen persoenlichen Key."
                    } else {
                        "Manus BYOS pausiert. Der Agent nutzt wieder das Backend-Setup."
                    },
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = updateEnabledResult.exceptionOrNull()?.message
                        ?: "Manus BYOS konnte nicht aktualisiert werden.",
                    isError = true,
                )
            }
        }
    }

    fun clearManusByosApiKey() {
        viewModelScope.launch {
            if (_uiState.value.currentUserId.isNullOrBlank()) {
                showPaymentFeedback(
                    message = "Bitte melde dich an, um deinen Manus API Key zu entfernen.",
                    isError = true,
                )
                return@launch
            }

            val result = ManusByosPreferences.clearApiKey()
            if (result.isSuccess) {
                result.getOrNull()?.let { settings ->
                    _uiState.update { it.copy(manusByosSettings = settings) }
                }
                _uiState.update {
                    it.copy(
                        manusValidationStatus = "awaiting_external_auth",
                        manusValidationMessage = "Key entfernt. Externer Lauf wartet auf Auth oder faellt intern zurueck.",
                    )
                }
                showPaymentFeedback(
                    message = "Manus API Key lokal entfernt. BYOS ist fuer dieses Konto aus.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message
                        ?: "Manus API Key konnte nicht entfernt werden.",
                    isError = true,
                )
            }
        }
    }

    fun validateManusByosKey(apiKeyDraft: String) {
        val effectiveKey = apiKeyDraft.trim().ifBlank { ManusByosPreferences.currentManusApiKeyOrNull().orEmpty() }
        if (effectiveKey.isBlank()) {
            _uiState.update {
                it.copy(
                    manusValidationStatus = "awaiting_external_auth",
                    manusValidationMessage = "Kein Manus-Key vorhanden. Externer Lauf wartet auf Auth oder nutzt internen Fallback.",
                )
            }
            showPaymentFeedback(
                message = "Bitte hinterlege zuerst einen Manus API Key.",
                isError = true,
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                functions.callWithAppCheckRetry(
                    functionName = "validateManusApiKey",
                    payload = mapOf("apiKey" to effectiveKey),
                )
            }.onSuccess { result ->
                val payload = result.data as? Map<*, *> ?: emptyMap<String, Any>()
                val valid = payload["valid"] as? Boolean ?: false
                val message = (payload["message"] as? String).orEmpty().ifBlank {
                    if (valid) "Manus-Key ist gueltig." else "Manus-Key ist ungueltig oder nicht erreichbar."
                }
                _uiState.update {
                    it.copy(
                        manusValidationStatus = if (valid) "key_valid" else "key_invalid",
                        manusValidationMessage = message,
                    )
                }
                showPaymentFeedback(message = message, isError = !valid)
            }.onFailure { error ->
                val message = "Validierung fehlgeschlagen: ${error.localizedMessage ?: "Unbekannter Fehler"}"
                _uiState.update {
                    it.copy(
                        manusValidationStatus = "external_failed",
                        manusValidationMessage = message,
                    )
                }
                showPaymentFeedback(message = message, isError = true)
            }
        }
    }

    fun saveAgentProfileSettings(settings: com.skydown.android.data.AgentProfileSettings) {
        viewModelScope.launch {
            if (_uiState.value.currentUserId.isNullOrBlank()) {
                showPaymentFeedback(
                    message = "Bitte melde dich an, um dein Agent-Profil zu speichern.",
                    isError = true,
                )
                return@launch
            }

            val result = AgentProfilePreferences.saveSettings(settings)
            if (result.isSuccess) {
                _uiState.update { it.copy(agentProfileSettings = settings) }
                showPaymentFeedback(
                    message = "Agent-Profil gespeichert. Dein Agent nutzt jetzt deine Skills und Vorgaben.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "Agent-Profil konnte nicht gespeichert werden.",
                    isError = true,
                )
            }
        }
    }

    fun saveAiPromptSettings(settings: com.skydown.android.data.AiPromptSettings) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf KI-Anweisungen verwalten.",
                    isError = true,
                )
                return@launch
            }

            val result = aiPromptSettingsRepository.updateSettings(settings)
            if (result.isSuccess) {
                _uiState.update { it.copy(aiPromptSettings = settings) }
                showPaymentFeedback(
                    message = "KI-Anweisungen gespeichert. Neue Prompts gelten ohne Release sofort serverseitig.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "KI-Anweisungen konnten nicht gespeichert werden.",
                    isError = true,
                )
            }
        }
    }

    fun saveAiRuntimeSettings(settings: com.skydown.android.data.AiRuntimeSettings) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf KI-Runtime steuern.",
                    isError = true,
                )
                return@launch
            }

            val result = aiRuntimeSettingsRepository.updateSettings(settings)
            if (result.isSuccess) {
                _uiState.update { it.copy(aiRuntimeSettings = settings) }
                showPaymentFeedback(
                    message = "KI-Runtime gespeichert. Provider und Kosten-Guard gelten serverseitig sofort.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "KI-Runtime konnte nicht gespeichert werden.",
                    isError = true,
                )
            }
        }
    }

    fun testWorkflowAutomationSettings(settings: com.skydown.android.data.WorkflowAutomationSettings) {
        viewModelScope.launch {
            if (_uiState.value.currentUserId.isNullOrBlank()) {
                showPaymentFeedback(
                    message = "Bitte melde dich an, um deinen Agent-Service zu testen.",
                    isError = true,
                )
                return@launch
            }
            val saveResult = WorkflowAutomationPreferences.saveSettings(settings)
            if (saveResult.isFailure) {
                showPaymentFeedback(
                    message = saveResult.exceptionOrNull()?.message ?: "Agent-Service konnte nicht gespeichert werden.",
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
                    message = testResult.exceptionOrNull()?.message ?: "Agent-Service-Test fehlgeschlagen.",
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

    fun saveLegalContentSettings(settings: LegalContentSettings) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf rechtliche Module verwalten.",
                    isError = true,
                )
                return@launch
            }

            val result = legalContentRepository.updateSettings(settings)
            if (result.isSuccess) {
                _uiState.update { it.copy(legalContentSettings = settings) }
                showPaymentFeedback(
                    message = "Rechtliche Module gespeichert. AGB, Datenschutz und Nutzungsbedingungen wurden aktualisiert.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "Rechtliche Module konnten nicht gespeichert werden.",
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
                refreshShopifyCollections(force = true)
                showPaymentFeedback(
                    message = "Shopify-Einstellungen gespeichert. Der naechste Sync nutzt jetzt diesen Store, deinen Storefront Token und die ausgewaehlten Collections.",
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

    fun refreshShopifyCollections(force: Boolean = false) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                return@launch
            }
            if (_uiState.value.isLoadingShopifyCollections) {
                return@launch
            }
            if (!force && _uiState.value.availableShopifyCollections.isNotEmpty()) {
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoadingShopifyCollections = true,
                    shopifyCollectionsErrorMessage = null,
                )
            }

            val result = shopifyAdminSettingsRepository.fetchAvailableCollections()
            result.onSuccess { collections ->
                _uiState.update {
                    it.copy(
                        availableShopifyCollections = collections,
                        isLoadingShopifyCollections = false,
                        shopifyCollectionsErrorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingShopifyCollections = false,
                        shopifyCollectionsErrorMessage = error.message ?: "Shopify-Collections konnten nicht geladen werden.",
                    )
                }
            }
        }
    }

    suspend fun saveManagedUser(user: User): Result<String> {
        if (!_uiState.value.isOwner) {
            val message = "Nur der Owner darf Konten verwalten."
            showPaymentFeedback(
                message = message,
                isError = true,
            )
            return Result.failure(IllegalStateException(message))
        }

        val result = adminUserManagementRepository.updateUser(user)
        return if (result.isSuccess) {
            val message = "Konto gespeichert. Rolle, Rechte und KI-Limits wurden aktualisiert."
            showPaymentFeedback(
                message = message,
                isError = false,
            )
            Result.success(message)
        } else {
            val message = result.exceptionOrNull()?.message ?: "Konto konnte nicht gespeichert werden."
            showPaymentFeedback(
                message = message,
                isError = true,
            )
            Result.failure(result.exceptionOrNull() ?: IllegalStateException(message))
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

    fun saveStripeBackendSecrets(
        stripeSecretKey: String,
        stripeWebhookSecret: String,
    ) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                showPaymentFeedback(
                    message = "Nur der Owner darf Stripe-Backend-Secrets setzen.",
                    isError = true,
                )
                return@launch
            }

            val trimmedKey = stripeSecretKey.trim()
            val trimmedWebhookSecret = stripeWebhookSecret.trim()
            if (trimmedKey.isBlank() && trimmedWebhookSecret.isBlank()) {
                showPaymentFeedback(
                    message = "Bitte mindestens einen Stripe-Wert eingeben.",
                    isError = true,
                )
                return@launch
            }

            val result = stripeBackendSecretsRepository.saveSecrets(
                stripeSecretKey = trimmedKey,
                stripeWebhookSecret = trimmedWebhookSecret,
            )
            if (result.isSuccess) {
                result.getOrNull()?.let { status ->
                    _uiState.update { current ->
                        current.copy(stripeBackendSecretsStatus = status)
                    }
                }
                showPaymentFeedback(
                    message = "Stripe-Backend sicher gespeichert. Die Werte liegen jetzt serverseitig im Secret Manager.",
                    isError = false,
                )
            } else {
                showPaymentFeedback(
                    message = result.exceptionOrNull()?.message ?: "Stripe-Secrets konnten nicht gespeichert werden.",
                    isError = true,
                )
            }
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

    fun saveProfile(
        username: String,
        whatsApp: String,
        profileTagline: String,
        profileBio: String,
        instagramHandle: String,
    ) {
        viewModelScope.launch {
            val trimmedUsername = username.trim()
            if (trimmedUsername.isEmpty()) {
                _uiState.update {
                    it.copy(accountErrorMessage = "Bitte gib einen Benutzernamen ein.")
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSavingProfile = true,
                    accountErrorMessage = null,
                )
            }

            val result = authService.updateCurrentProfile(
                ProfileUpdateInput(
                    username = trimmedUsername,
                    whatsApp = whatsApp,
                    profileTagline = profileTagline,
                    profileBio = profileBio,
                    instagramHandle = instagramHandle,
                ),
            )

            if (result.isSuccess) {
                val user = result.getOrNull()
                _uiState.update {
                    it.copy(
                        username = user?.username ?: trimmedUsername,
                        email = user?.email ?: it.email,
                        whatsApp = user?.whatsApp.orEmpty(),
                        profileTagline = user?.profileTagline.orEmpty(),
                        profileBio = user?.profileBio.orEmpty(),
                        instagramHandle = user?.instagramHandle.orEmpty(),
                        isSavingProfile = false,
                        accountErrorMessage = null,
                    )
                }
                AppContainer.refreshCurrentUser()
                showPaymentFeedback(message = "Profil gespeichert.", isError = false)
            } else {
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        accountErrorMessage = result.exceptionOrNull()?.message
                            ?: "Profil konnte nicht gespeichert werden.",
                    )
                }
            }
        }
    }

    fun saveAiAccessConsent(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingProfile = true,
                    accountErrorMessage = null,
                )
            }

            val result = authService.updateCurrentAiAccessEnabled(enabled)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _uiState.update {
                    it.copy(
                        aiAccessEnabled = user?.aiAccessEnabled ?: enabled,
                        isSavingProfile = false,
                        accountErrorMessage = null,
                    )
                }
                AppContainer.refreshCurrentUser()
                showPaymentFeedback(
                    message = if (enabled) {
                        "KI-Zugriff fuer dein Konto aktiviert."
                    } else {
                        "KI-Zugriff fuer dein Konto pausiert."
                    },
                    isError = false,
                )
            } else {
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        accountErrorMessage = result.exceptionOrNull()?.message
                            ?: "KI-Einwilligung konnte nicht gespeichert werden.",
                    )
                }
            }
        }
    }

    fun signOut(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingProfile = false,
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
                        whatsApp = "",
                        profileTagline = "",
                        profileBio = "",
                        instagramHandle = "",
                        aiAccessEnabled = true,
                        isOwner = false,
                        isSavingProfile = false,
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
        stripeBackendSecretsListener?.remove()
        stripeBackendSecretsListener = null
        aiPromptSettingsListener?.remove()
        aiPromptSettingsListener = null
        aiRuntimeSettingsListener?.remove()
        aiRuntimeSettingsListener = null
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

    private fun configureAiPromptSettingsObservation(isEnabled: Boolean) {
        if (!isEnabled) {
            aiPromptSettingsListener?.remove()
            aiPromptSettingsListener = null
            _uiState.update { it.copy(aiPromptSettings = com.skydown.android.data.AiPromptSettings()) }
            return
        }

        if (aiPromptSettingsListener != null) {
            return
        }

        aiPromptSettingsListener = aiPromptSettingsRepository.observeSettings { result ->
            result.onSuccess { settings ->
                _uiState.update { it.copy(aiPromptSettings = settings) }
            }.onFailure { error ->
                showPaymentFeedback(
                    message = error.message ?: "KI-Anweisungen konnten nicht geladen werden.",
                    isError = true,
                )
            }
        }
    }

    private fun configureAiRuntimeSettingsObservation(isEnabled: Boolean) {
        if (!isEnabled) {
            aiRuntimeSettingsListener?.remove()
            aiRuntimeSettingsListener = null
            _uiState.update { it.copy(aiRuntimeSettings = com.skydown.android.data.AiRuntimeSettings()) }
            return
        }

        if (aiRuntimeSettingsListener != null) {
            return
        }

        aiRuntimeSettingsListener = aiRuntimeSettingsRepository.observeSettings { result ->
            result.onSuccess { settings ->
                _uiState.update { it.copy(aiRuntimeSettings = settings) }
            }.onFailure { error ->
                showPaymentFeedback(
                    message = error.message ?: "KI-Runtime konnte nicht geladen werden.",
                    isError = true,
                )
            }
        }
    }

    private fun configureStripeBackendSecretsObservation(isEnabled: Boolean) {
        if (!isEnabled) {
            stripeBackendSecretsListener?.remove()
            stripeBackendSecretsListener = null
            _uiState.update {
                it.copy(stripeBackendSecretsStatus = com.skydown.android.data.StripeBackendSecretsStatus())
            }
            return
        }

        if (stripeBackendSecretsListener != null) {
            return
        }

        stripeBackendSecretsListener = stripeBackendSecretsRepository.observeStatus { result ->
            result.onSuccess { status ->
                _uiState.update {
                    it.copy(stripeBackendSecretsStatus = status)
                }
            }.onFailure { error ->
                showPaymentFeedback(
                    message = error.message ?: "Stripe-Backend-Status konnte nicht geladen werden.",
                    isError = true,
                )
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
