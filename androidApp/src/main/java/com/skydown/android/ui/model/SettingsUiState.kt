package com.skydown.android.ui.model

import com.skydown.android.ui.theme.AppearanceMode
import com.skydown.android.data.AiPromptSettings
import com.skydown.android.data.AiVisualReferenceLibrarySettings
import com.skydown.android.data.CommerceSettings
import com.skydown.android.data.PaymentMethodsSettings
import com.skydown.android.data.ShopifyAdminSettings
import com.skydown.android.data.ShopifyCollectionOption
import com.skydown.android.data.StripeBackendSecretsStatus
import com.skydown.android.data.WorkflowAutomationSettings
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
    val isOwner: Boolean = false,
    val language: String = "Deutsch",
    val notificationsEnabled: Boolean = true,
    val colorScheme: AppearanceMode = AppearanceMode.System,
    val appVersion: String = "1.0 (14)",
    val isSavingProfile: Boolean = false,
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val accountErrorMessage: String? = null,
    val aiVisualReferenceLibrary: AiVisualReferenceLibrarySettings = AiVisualReferenceLibrarySettings(),
    val aiPromptSettings: AiPromptSettings = AiPromptSettings(),
    val workflowAutomationSettings: WorkflowAutomationSettings = WorkflowAutomationSettings(),
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
