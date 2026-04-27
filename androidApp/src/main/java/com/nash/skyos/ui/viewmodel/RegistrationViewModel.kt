package com.nash.skyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nash.skyos.data.AppContainer
import com.nash.skyos.ui.model.AuthUiState
import com.skydown.shared.model.RegistrationConsentInput
import com.skydown.shared.model.RegistrationInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistrationViewModel : ViewModel() {
    private val authService = AppContainer.authService
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private val consentRequiredMessage = "Bitte akzeptiere AGB und Datenschutz, um fortzufahren."

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun updateAcceptedTerms(value: Boolean) {
        _uiState.update { it.copy(acceptedTerms = value, errorMessage = null) }
    }

    fun updateAcceptedPrivacyPolicy(value: Boolean) {
        _uiState.update { it.copy(acceptedPrivacyPolicy = value, errorMessage = null) }
    }

    fun updateAiConsentEnabled(value: Boolean) {
        _uiState.update { it.copy(aiConsentEnabled = value, errorMessage = null) }
    }

    fun updateLegalVersionLabel(value: String) {
        _uiState.update { it.copy(legalVersionLabel = value, errorMessage = null) }
    }

    fun register(onSuccess: () -> Unit) {
        val current = _uiState.value
        if (!hasRequiredConsent(current)) {
            failEmailSignUp(consentRequiredMessage)
            return
        }
        viewModelScope.launch {
            setEmailSignUpLoading()

            val result = authService.register(
                RegistrationInput(
                    username = current.username,
                    email = current.email,
                    whatsApp = "",
                    password = current.password,
                    confirmPassword = current.confirmPassword,
                    consent = RegistrationConsentInput(
                        acceptedTerms = current.acceptedTerms,
                        acceptedPrivacyPolicy = current.acceptedPrivacyPolicy,
                        aiConsentEnabled = current.aiConsentEnabled,
                        legalVersionLabel = current.legalVersionLabel,
                        consentSource = "android_registration",
                    ),
                ),
            )

            if (result.isSuccess) {
                finalizeEmailSignUp()
                onSuccess()
            } else {
                failEmailSignUp(result.exceptionOrNull()?.message)
            }
        }
    }

    fun beginGoogleSignIn() {
        setGoogleSignInLoading()
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        val preferredUsername = _uiState.value.username.trim().ifBlank { null }
        val current = _uiState.value
        if (!hasRequiredConsent(current)) {
            failGoogleSignIn(consentRequiredMessage)
            return
        }
        viewModelScope.launch {
            val result = authService.signInWithGoogle(
                idToken = idToken,
                preferredUsername = preferredUsername,
                registrationConsent = RegistrationConsentInput(
                    acceptedTerms = current.acceptedTerms,
                    acceptedPrivacyPolicy = current.acceptedPrivacyPolicy,
                    aiConsentEnabled = current.aiConsentEnabled,
                    legalVersionLabel = current.legalVersionLabel,
                    consentSource = "android_registration_google",
                ),
            )

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

    private fun hasRequiredConsent(state: AuthUiState): Boolean {
        return state.acceptedTerms && state.acceptedPrivacyPolicy
    }

    private fun setEmailSignUpLoading() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    private fun finalizeEmailSignUp() {
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun failEmailSignUp(message: String?) {
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
