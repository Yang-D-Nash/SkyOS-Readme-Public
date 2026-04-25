package com.nash.skyos.ui.model

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false,
    val acceptedPrivacyPolicy: Boolean = false,
    val aiConsentEnabled: Boolean = false,
    val legalVersionLabel: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
)
