package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.model.ShopUiState
import com.skydown.shared.model.MerchandiseItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
            refreshState()
        }
        viewModelScope.launch {
            AppContainer.currentUser.collectLatest { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        isAdmin = user?.isAdmin == true,
                    )
                }
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

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshState()
        }
    }

    fun addItem(
        name: String,
        description: String,
        priceInput: String,
        available: Boolean,
        imageDataList: List<ByteArray>,
        onSuccess: () -> Unit,
    ) {
        val price = priceInput.toDoubleOrNull()
        if (name.isBlank() || description.isBlank() || price == null || imageDataList.isEmpty()) {
            _uiState.update {
                it.copy(
                    toastMessage = "Bitte Name, Beschreibung, Preis und mindestens ein Bild angeben.",
                    isErrorToast = true,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val result = merchandiseService.addItem(
                item = MerchandiseItem(
                    name = name.trim(),
                    price = price,
                    description = description.trim(),
                    imageUrls = emptyList(),
                    available = available,
                ),
                imageDataList = imageDataList,
            )

            if (result.isSuccess) {
                refreshState()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        toastMessage = "Artikel hinzugefuegt.",
                        isErrorToast = false,
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        toastMessage = result.exceptionOrNull()?.message ?: "Artikel konnte nicht gespeichert werden.",
                        isErrorToast = true,
                    )
                }
            }
        }
    }

    fun updateItem(
        item: MerchandiseItem,
        name: String,
        description: String,
        priceInput: String,
        available: Boolean,
        imageDataList: List<ByteArray>,
        onSuccess: () -> Unit,
    ) {
        val trimmedName = name.trim()
        val trimmedDescription = description.trim()
        val price = priceInput.toDoubleOrNull()
        if (trimmedName.isBlank() || trimmedDescription.isBlank() || price == null) {
            _uiState.update {
                it.copy(
                    toastMessage = "Bitte Name, Beschreibung und einen gueltigen Preis angeben.",
                    isErrorToast = true,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val result = merchandiseService.updateItem(
                item = item.copy(
                    name = trimmedName,
                    description = trimmedDescription,
                    price = price,
                    available = available,
                ),
                imageDataList = imageDataList,
            )

            if (result.isSuccess) {
                refreshState()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        toastMessage = "Artikel aktualisiert.",
                        isErrorToast = false,
                        selectedItem = null,
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        toastMessage = result.exceptionOrNull()?.message ?: "Artikel konnte nicht aktualisiert werden.",
                        isErrorToast = true,
                    )
                }
            }
        }
    }

    fun updatePrice(item: MerchandiseItem, priceInput: String) {
        val itemId = item.id.orEmpty()
        val newPrice = priceInput.toDoubleOrNull()
        if (itemId.isBlank() || newPrice == null) {
            _uiState.update {
                it.copy(
                    toastMessage = "Bitte einen gueltigen Preis angeben.",
                    isErrorToast = true,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = merchandiseService.updatePrice(itemId, newPrice)
            if (result.isSuccess) {
                refreshState()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        toastMessage = "Preis aktualisiert.",
                        isErrorToast = false,
                        selectedItem = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        toastMessage = result.exceptionOrNull()?.message ?: "Preis konnte nicht aktualisiert werden.",
                        isErrorToast = true,
                    )
                }
            }
        }
    }

    fun deleteItem(item: MerchandiseItem) {
        val itemId = item.id.orEmpty()
        if (itemId.isBlank()) {
            _uiState.update {
                it.copy(
                    toastMessage = "Artikel hat keine gueltige ID.",
                    isErrorToast = true,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = merchandiseService.deleteItem(itemId)
            if (result.isSuccess) {
                refreshState()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        toastMessage = "Artikel geloescht.",
                        isErrorToast = false,
                        selectedItem = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        toastMessage = result.exceptionOrNull()?.message ?: "Artikel konnte nicht geloescht werden.",
                        isErrorToast = true,
                    )
                }
            }
        }
    }

    private suspend fun refreshState() {
        AppContainer.refreshCurrentUser()
        val itemsResult = merchandiseService.loadItems()
        val user = AppContainer.authService.currentUser()
        _uiState.update {
            it.copy(
                items = itemsResult.getOrDefault(emptyList()),
                isLoggedIn = user != null,
                isAdmin = user?.isAdmin == true,
                errorMessage = itemsResult.exceptionOrNull()?.message,
            )
        }
    }
}
