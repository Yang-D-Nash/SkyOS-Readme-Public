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
            setEmailSignInLoading()

            val result = authService.signIn(
                LoginInput(
                    email = current.email,
                    password = current.password,
                ),
            )

            if (result.isSuccess) {
                finalizeEmailSignIn()
                onSuccess()
            } else {
                failEmailSignIn(result.exceptionOrNull()?.message)
            }
        }
    }

    fun beginGoogleSignIn() {
        setGoogleSignInLoading()
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = authService.signInWithGoogle(idToken)

            if (result.isSuccess) {
                finalizeGoogleSignIn()
                onSuccess()
            } else {
                failGoogleSignIn(result.exceptionOrNull()?.message)
            }
        }
    }

    fun onGoogleSignInCancelled(message: String = "Google-Anmeldung wurde abgebrochen.") {
        failGoogleSignIn(message)
    }

    private fun setEmailSignInLoading() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    private fun finalizeEmailSignIn() {
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun failEmailSignIn(message: String?) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message,
            )
        }
    }

    private fun setGoogleSignInLoading() {
        _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null) }
    }

    private fun finalizeGoogleSignIn() {
        _uiState.update { it.copy(isGoogleLoading = false) }
    }

    private fun failGoogleSignIn(message: String?) {
        _uiState.update {
            it.copy(
                isGoogleLoading = false,
                errorMessage = message,
            )
        }
    }
}
