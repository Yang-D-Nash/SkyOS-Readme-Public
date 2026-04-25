package com.nash.skyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nash.skyos.data.AppContainer
import com.nash.skyos.ui.model.AuthUiState
import com.skydown.shared.model.LoginInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val authService = AppContainer.authService
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun signIn(onSuccess: () -> Unit) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authService.signIn(
                LoginInput(
                    email = current.email,
                    password = current.password,
                ),
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message,
                    )
                }
            }
        }
    }

    fun beginGoogleSignIn() {
        _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null) }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = authService.signInWithGoogle(idToken)

            if (result.isSuccess) {
                _uiState.update { it.copy(isGoogleLoading = false) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isGoogleLoading = false,
                        errorMessage = result.exceptionOrNull()?.message,
                    )
                }
            }
        }
    }

    fun onGoogleSignInCancelled(message: String = "Google-Anmeldung wurde abgebrochen.") {
        _uiState.update {
            it.copy(
                isGoogleLoading = false,
                errorMessage = message,
            )
        }
    }
}
