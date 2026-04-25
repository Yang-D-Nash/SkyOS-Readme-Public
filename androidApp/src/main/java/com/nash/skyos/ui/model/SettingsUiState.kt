package com.nash.skyos.ui.model

import com.nash.skyos.ui.theme.AppearanceMode
import com.nash.skyos.data.AgentProfileSettings
import com.nash.skyos.data.AiPromptSettings
import com.nash.skyos.data.AiRuntimeSettings
import com.nash.skyos.data.AiVisualReferenceLibrarySettings
import com.nash.skyos.data.CommerceSettings
import com.nash.skyos.data.LegalContentSettings
import com.nash.skyos.data.ManusByosSettings
import com.nash.skyos.data.PaymentMethodsSettings
import com.nash.skyos.data.ShopifyAdminSettings
import com.nash.skyos.data.ShopifyCollectionOption
import com.nash.skyos.data.StripeBackendSecretsStatus
import com.nash.skyos.data.WorkflowAutomationSettings
import com.nash.skyos.data.AppLanguageSupport
import com.skydown.shared.model.User

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
    val currentUserId: String? = null,
    val username: String = "",
    val email: String = "",
    val whatsApp: String = "",
    val profileTagline: String = "",
    val profileBio: String = "",
    val instagramHandle: String = "",
    val aiAccessEnabled: Boolean = true,
    val isOwner: Boolean = false,
    val language: String = AppLanguageSupport.currentSystemLanguageDisplayName(),
    val notificationsEnabled: Boolean = true,
    val colorScheme: AppearanceMode = AppearanceMode.System,
    val appVersion: String = "1.0 (16)",
    val isSavingProfile: Boolean = false,
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val accountErrorMessage: String? = null,
    val aiVisualReferenceLibrary: AiVisualReferenceLibrarySettings = AiVisualReferenceLibrarySettings(),
    val agentProfileSettings: AgentProfileSettings = AgentProfileSettings(),
    val aiPromptSettings: AiPromptSettings = AiPromptSettings(),
    val aiRuntimeSettings: AiRuntimeSettings = AiRuntimeSettings(),
    val legalContentSettings: LegalContentSettings = LegalContentSettings(),
    val workflowAutomationSettings: WorkflowAutomationSettings = WorkflowAutomationSettings(),
    val manusByosSettings: ManusByosSettings = ManusByosSettings(),
    val manusValidationStatus: String = "unvalidated",
    val manusValidationMessage: String = "Noch nicht geprueft.",
    val commerceSettings: CommerceSettings = CommerceSettings(),
    val shopifyAdminSettings: ShopifyAdminSettings = ShopifyAdminSettings(),
    val availableShopifyCollections: List<ShopifyCollectionOption> = emptyList(),
    val isLoadingShopifyCollections: Boolean = false,
    val shopifyCollectionsErrorMessage: String? = null,
    val paymentMethods: PaymentMethodsSettings = PaymentMethodsSettings(),
    val stripeBackendSecretsStatus: StripeBackendSecretsStatus = StripeBackendSecretsStatus(),
    val managedUsers: List<User> = emptyList(),
    val managedUsersErrorMessage: String? = null,
    val paymentFeedbackMessage: String? = null,
    val isPaymentFeedbackError: Boolean = false,
)
