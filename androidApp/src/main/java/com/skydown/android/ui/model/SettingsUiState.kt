package com.skydown.android.ui.model

import com.skydown.android.ui.theme.AppearanceMode
import com.skydown.android.data.AiVisualReferenceLibrarySettings
import com.skydown.android.data.CommerceSettings
import com.skydown.android.data.PaymentMethodsSettings
import com.skydown.android.data.ShopifyAdminSettings
import com.skydown.android.data.WorkflowAutomationSettings

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val email: String = "",
    val isAdmin: Boolean = false,
    val language: String = "Deutsch",
    val notificationsEnabled: Boolean = true,
    val colorScheme: AppearanceMode = AppearanceMode.System,
    val appVersion: String = "1.0 (7)",
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val accountErrorMessage: String? = null,
    val aiVisualReferenceLibrary: AiVisualReferenceLibrarySettings = AiVisualReferenceLibrarySettings(),
    val workflowAutomationSettings: WorkflowAutomationSettings = WorkflowAutomationSettings(),
    val commerceSettings: CommerceSettings = CommerceSettings(),
    val shopifyAdminSettings: ShopifyAdminSettings = ShopifyAdminSettings(),
    val paymentMethods: PaymentMethodsSettings = PaymentMethodsSettings(),
    val paymentFeedbackMessage: String? = null,
    val isPaymentFeedbackError: Boolean = false,
)
