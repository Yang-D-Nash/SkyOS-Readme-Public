package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.model.ShopUiState
import com.skydown.shared.model.MerchandiseItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShopViewModel : ViewModel() {
    private val merchandiseService = AppContainer.merchandiseService
    private val _uiState = MutableStateFlow(
        ShopUiState(),
    )
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.refreshCurrentUser()
            val itemsResult = merchandiseService.loadItems()
            val user = AppContainer.authService.currentUser()
            _uiState.update {
                it.copy(
                    items = itemsResult.getOrDefault(emptyList()),
                    isAdmin = user?.isAdmin == true,
                    errorMessage = itemsResult.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun selectItem(item: MerchandiseItem) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun dismissSelectedItem() {
        _uiState.update { it.copy(selectedItem = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
