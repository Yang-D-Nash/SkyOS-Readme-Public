package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppCartStore
import com.skydown.android.data.MerchStoreStatusRepository
import com.skydown.android.ui.model.ShopUiState
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.usecase.CartUseCase
import com.skydown.shared.usecase.MerchandiseVariantResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShopViewModel : ViewModel() {
    private val merchandiseService = AppContainer.merchandiseService
    private val merchStoreStatusRepository = MerchStoreStatusRepository()
    private val shopifyMerchSyncClient = AppContainer.shopifyMerchSyncClient
    private val _uiState = MutableStateFlow(
        ShopUiState(),
    )
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()
    private var storeStatusListener: ListenerRegistration? = null
    private var allItems: List<MerchandiseItem> = emptyList()

    init {
        viewModelScope.launch {
            refreshState()
        }
        observeStoreStatus()
        viewModelScope.launch {
            AppContainer.currentUser.collectLatest { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        isAdmin = user?.isAdmin == true,
                    )
                }
                applyVisibleItems()
            }
        }
    }

    fun toggleStoreOpen() {
        if (!_uiState.value.isAdmin) {
            _uiState.update {
                it.copy(
                    toastMessage = "Nur Admins duerfen den Merch Store schalten.",
                    isErrorToast = true,
                )
            }
            return
        }

        viewModelScope.launch {
            val nextState = !_uiState.value.isStoreOpen
            _uiState.update { it.copy(isUpdatingStoreState = true) }
            val result = merchStoreStatusRepository.updateStoreOpen(nextState)

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isUpdatingStoreState = false,
                        toastMessage = if (nextState) "Merch Store geoeffnet." else "Merch Store geschlossen.",
                        isErrorToast = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isUpdatingStoreState = false,
                        toastMessage = result.exceptionOrNull()?.message ?: "Store-Status konnte nicht aktualisiert werden.",
                        isErrorToast = true,
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

    fun syncShopifyCatalog() {
        if (!_uiState.value.isAdmin) {
            _uiState.update {
                it.copy(
                    toastMessage = "Nur Admins duerfen den Shopify-Sync starten.",
                    isErrorToast = true,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingCatalog = true) }
            val result = shopifyMerchSyncClient.triggerSync()
            _uiState.update {
                it.copy(
                    isSyncingCatalog = false,
                    toastMessage = result.getOrElse { error ->
                        error.message ?: "Shopify-Sync fehlgeschlagen."
                    },
                    isErrorToast = result.isFailure,
                )
            }
            if (result.isSuccess) {
                refreshState()
            }
        }
    }

    fun addSelectionToCart(
        item: MerchandiseItem,
        size: String,
        color: String?,
        quantity: Int,
    ): Result<Unit> {
        return runCatching {
            val variant = if (!item.shopifyProductId.isNullOrBlank() && item.variants.isNotEmpty()) {
                MerchandiseVariantResolver.resolveVariant(
                    item = item,
                    size = size,
                    color = color,
                ).getOrThrow()
            } else {
                null
            }

            AppCartStore.update { currentItems ->
                CartUseCase.addItem(
                    currentItems = currentItems,
                    item = item,
                    size = size,
                    color = color,
                    quantity = quantity,
                    shopifyVariantId = variant?.shopifyVariantId,
                    sku = variant?.sku,
                    unitPrice = variant?.price,
                )
            }

            _uiState.update {
                it.copy(
                    toastMessage = "Zum Warenkorb hinzugefuegt.",
                    isErrorToast = false,
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    toastMessage = error.message ?: "Die Variante konnte nicht hinzugefuegt werden.",
                    isErrorToast = true,
                )
            }
        }
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
        isVisibleInApp: Boolean,
        featured: Boolean,
        sortOrderInput: String,
        customBadge: String,
        customImageOverride: String,
        imageDataList: List<ByteArray>,
        onSuccess: () -> Unit,
    ) {
        val price = priceInput.toDoubleOrNull()
        val sortOrder = sortOrderInput.toIntOrNull() ?: 0
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
                    isVisibleInApp = isVisibleInApp,
                    featured = featured,
                    sortOrder = sortOrder,
                    customBadge = customBadge.trim(),
                    customImageOverride = customImageOverride.trim(),
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
        isVisibleInApp: Boolean,
        featured: Boolean,
        sortOrderInput: String,
        customBadge: String,
        customImageOverride: String,
        imageDataList: List<ByteArray>,
        onSuccess: () -> Unit,
    ) {
        val trimmedName = name.trim()
        val trimmedDescription = description.trim()
        val price = priceInput.toDoubleOrNull()
        val sortOrder = sortOrderInput.toIntOrNull() ?: item.sortOrder
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
                    isVisibleInApp = isVisibleInApp,
                    featured = featured,
                    sortOrder = sortOrder,
                    customBadge = customBadge.trim(),
                    customImageOverride = customImageOverride.trim(),
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
        val resolvedItems = itemsResult.getOrDefault(emptyList())
        allItems = resolvedItems
        _uiState.update {
            it.copy(
                items = filterVisibleItems(resolvedItems, isAdmin = user?.isAdmin == true),
                isLoggedIn = user != null,
                isAdmin = user?.isAdmin == true,
                errorMessage = itemsResult.exceptionOrNull()?.message,
            )
        }
    }

    private fun observeStoreStatus() {
        storeStatusListener?.remove()
        storeStatusListener = merchStoreStatusRepository.observeStatus { result ->
            result.onSuccess { status ->
                _uiState.update { it.copy(isStoreOpen = status.isOpen) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        toastMessage = error.message ?: "Store-Status konnte nicht geladen werden.",
                        isErrorToast = true,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        storeStatusListener?.remove()
        super.onCleared()
    }

    private fun applyVisibleItems() {
        _uiState.update {
            it.copy(
                items = filterVisibleItems(allItems, isAdmin = it.isAdmin),
            )
        }
    }

    private fun filterVisibleItems(items: List<MerchandiseItem>, isAdmin: Boolean): List<MerchandiseItem> {
        return items
            .filter { isAdmin || it.isVisibleInApp }
            .sortedWith(
                compareBy<MerchandiseItem> { !it.featured }
                    .thenBy { it.sortOrder }
                    .thenBy { it.name.lowercase() },
            )
    }
}
