package com.skydown.android.ui.model

data class SettingsUiState(
    val isLoggedIn: Boolean = true,
    val username: String = "",
    val isAdmin: Boolean = false,
    val language: String = "Deutsch",
    val notificationsEnabled: Boolean = true,
    val colorScheme: String = "dark",
    val appVersion: String = "1.0.0",
)
