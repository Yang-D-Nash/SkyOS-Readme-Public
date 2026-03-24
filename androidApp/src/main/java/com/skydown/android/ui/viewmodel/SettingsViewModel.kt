package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.refreshCurrentUser()
            AppContainer.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        username = user?.username.orEmpty(),
                        isAdmin = user?.isAdmin == true,
                    )
                }
            }
        }
    }

    fun updateNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun updateColorScheme(colorScheme: String) {
        _uiState.update { it.copy(colorScheme = colorScheme) }
    }
}
