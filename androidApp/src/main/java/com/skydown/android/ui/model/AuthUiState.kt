package com.skydown.android.ui.model

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val confirmPassword: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
)
