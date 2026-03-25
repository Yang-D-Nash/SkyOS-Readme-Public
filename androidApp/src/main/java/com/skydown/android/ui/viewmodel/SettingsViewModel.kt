package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppearancePreferences
import com.skydown.android.ui.model.SettingsUiState
import com.skydown.android.ui.theme.AppearanceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val authService = AppContainer.authService
    private val _uiState = MutableStateFlow(
        SettingsUiState(),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.refreshCurrentUser()
            AppContainer.currentUser.collect { user ->
                val displayName = user?.username
                    ?.takeIf { it.isNotBlank() }
                    ?: user?.email
                        ?.substringBefore("@")
                        ?.takeIf { it.isNotBlank() }
                    ?: if (user != null) "Skydown User" else ""

                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        username = displayName,
                        email = user?.email.orEmpty(),
                        isAdmin = user?.isAdmin == true,
                        accountErrorMessage = null,
                    )
                }
            }
        }

        viewModelScope.launch {
            AppearancePreferences.appearanceMode.collect { mode ->
                _uiState.update { it.copy(colorScheme = mode) }
            }
        }
    }

    fun updateNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun updateColorScheme(colorScheme: AppearanceMode) {
        AppearancePreferences.updateAppearanceMode(colorScheme)
    }

    fun signOut(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSigningOut = true,
                    accountErrorMessage = null,
                )
            }

            val result = authService.signOut()

            if (result.isSuccess) {
                _uiState.update { it.copy(isSigningOut = false) }
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isSigningOut = false,
                        accountErrorMessage = result.exceptionOrNull()?.message
                            ?: "Abmelden fehlgeschlagen.",
                    )
                }
            }
        }
    }
}
