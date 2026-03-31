package com.skydown.android.ui.model

import com.skydown.android.ui.theme.AppearanceMode
import com.skydown.android.data.AiVisualReferenceLibrarySettings

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
)
