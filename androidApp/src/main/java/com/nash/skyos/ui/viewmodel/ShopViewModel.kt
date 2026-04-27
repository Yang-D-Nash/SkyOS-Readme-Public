package com.nash.skyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.AppCartStore
import com.nash.skyos.data.AppTextResolver
import com.nash.skyos.data.toAppCheckVerificationMessage
import com.nash.skyos.R
import com.nash.skyos.ui.model.ShopUiState
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.isPlatformOwner
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
    private val merchStoreStatusRepository = AppContainer.merchStoreStatusRepository
    private val shopifyMerchSyncClient = AppContainer.shopifyMerchSyncClient
    private val shopifyPublicCatalogClient = AppContainer.shopifyPublicCatalogClient
    private val _uiState = MutableStateFlow(
        ShopUiState(),
    )
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()
    private var storeStatusListener: ListenerRegistration? = null
    private var allItems: List<MerchandiseItem> = emptyList()
    private var hasAttemptedAutomaticShopifySync = false

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
                        isAdmin = user?.isPlatformOwner == true,
                    )
                }
                applyVisibleItems()
            }
        }
    }

    fun toggleStoreOpen() {
        if (!_uiState.value.isAdmin) {
            postErrorToast(AppTextResolver.string(R.string.shop_toast_owner_only_store_toggle))
            return
        }

        viewModelScope.launch {
            val nextState = !_uiState.value.isStoreOpen
            _uiState.update { it.copy(isUpdatingStoreState = true) }
            val result = merchStoreStatusRepository.updateStoreOpen(nextState)

            if (result.isSuccess) {
                postSuccessToast(
                    if (nextState) {
                        AppTextResolver.string(R.string.shop_toast_store_opened)
                    } else {
                        AppTextResolver.string(R.string.shop_toast_store_closed)
                    },
                    isUpdatingStoreState = false,
                )
            } else {
                postErrorToast(
                    result.exceptionOrNull()?.message
                        ?: AppTextResolver.string(R.string.shop_error_store_status_update_failed),
                    isUpdatingStoreState = false,
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
        clearTransientMessages(clearError = true, clearToast = false)
    }

    fun clearToast() {
        clearTransientMessages(clearError = false, clearToast = true)
    }

    fun syncShopifyCatalog() {
        if (!_uiState.value.isAdmin) {
            postErrorToast(AppTextResolver.string(R.string.shop_toast_owner_only_sync))
            return
        }

        viewModelScope.launch {
            setSyncInProgress(isCatalogLoading = false)
            val result = shopifyMerchSyncClient.triggerSync()
            val toastMessage = result.getOrElse(::resolveSyncErrorMessage)
            if (result.isSuccess) {
                postSuccessToast(toastMessage, isSyncingCatalog = false)
            } else {
                postErrorToast(toastMessage, isSyncingCatalog = false)
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

            postSuccessToast(AppTextResolver.string(R.string.shop_toast_added_to_cart))
        }.onFailure { error ->
            postErrorToast(error.message ?: AppTextResolver.string(R.string.shop_error_variant_add_failed))
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshState()
        }
    }

    private suspend fun refreshState() {
        _uiState.update {
            it.copy(
                isCatalogLoading = true,
                errorMessage = null,
            )
        }
        val itemsResult = merchandiseService.loadItems()
        val user = AppContainer.currentUser.value
        val isAdmin = user?.isPlatformOwner == true
        val resolvedItems = itemsResult.getOrDefault(emptyList())
        allItems = resolvedItems
        if (resolvedItems.isNotEmpty()) {
            hasAttemptedAutomaticShopifySync = false
        }
        val fallbackResult = if (resolvedItems.isEmpty() || (!isAdmin && !hasVisibleShopifyItems(resolvedItems, isAdmin = false))) {
            shopifyPublicCatalogClient.fetchCatalog()
        } else {
            Result.success(emptyList())
        }
        val fallbackItems = fallbackResult.getOrDefault(emptyList())
        val visibleItems = if (fallbackItems.isNotEmpty()) fallbackItems else resolvedItems
        val filteredItems = filterVisibleItems(visibleItems, isAdmin = isAdmin)
        _uiState.update {
            it.copy(
                items = filteredItems,
                isCatalogLoading = false,
                isLoggedIn = user != null,
                isAdmin = isAdmin,
                selectedItem = it.selectedItem?.let { selected ->
                    filteredItems.firstOrNull { item -> item.id == selected.id }
                },
                errorMessage = when {
                    filteredItems.isNotEmpty() -> null
                    fallbackResult.isFailure -> fallbackResult.exceptionOrNull()?.message
                    itemsResult.isFailure -> itemsResult.exceptionOrNull()?.message
                    else -> null
                },
            )
        }

        if (fallbackItems.isNotEmpty()) {
            allItems = fallbackItems
        }

        if (isAdmin && resolvedItems.isEmpty() && fallbackItems.isEmpty()) {
            maybeAutoSyncShopify()
        }
    }

    private fun observeStoreStatus() {
        storeStatusListener?.remove()
        storeStatusListener = merchStoreStatusRepository.observeStatus { result ->
            result.onSuccess { status ->
                _uiState.update { it.copy(isStoreOpen = status.isOpen) }
            }.onFailure { error ->
                postErrorToast(error.message ?: AppTextResolver.string(R.string.shop_error_store_status_load_failed))
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
        val activeItems = items.filter { it.source != "shopify" || it.shopifySyncActive }
        val visibleItems = activeItems.filter { isAdmin || it.isVisibleInApp }
        val hasVisibleShopifyItems = visibleItems.any {
            it.source == "shopify" && it.shopifySyncActive && !it.shopifyProductId.isNullOrBlank()
        }

        val prioritizedItems = when {
            isAdmin -> visibleItems
            hasVisibleShopifyItems -> visibleItems.filter {
                it.source == "shopify" && it.shopifySyncActive && !it.shopifyProductId.isNullOrBlank()
            }
            else -> visibleItems
        }

        return prioritizedItems
            .sortedWith(
                compareBy<MerchandiseItem> { !it.featured }
                    .thenBy { if (it.source == "shopify") 0 else 1 }
                    .thenBy { it.sortOrder }
                    .thenBy { it.name.lowercase() },
            )
    }

    private fun hasVisibleShopifyItems(items: List<MerchandiseItem>, isAdmin: Boolean): Boolean {
        return items.any { item ->
            (isAdmin || item.isVisibleInApp) &&
                item.source == "shopify" &&
                item.shopifySyncActive &&
                !item.shopifyProductId.isNullOrBlank()
        }
    }

    private suspend fun maybeAutoSyncShopify() {
        if (hasAttemptedAutomaticShopifySync || _uiState.value.isSyncingCatalog) {
            return
        }

        hasAttemptedAutomaticShopifySync = true
        setSyncInProgress(
            isCatalogLoading = true,
            loadingToast = AppTextResolver.string(R.string.shop_toast_sync_loading),
        )

        val syncResult = shopifyMerchSyncClient.triggerSync()
        if (syncResult.isSuccess) {
            val itemsResult = merchandiseService.loadItems()
            val user = AppContainer.currentUser.value
            val resolvedItems = itemsResult.getOrDefault(emptyList())
            allItems = resolvedItems
            val isAdmin = user?.isPlatformOwner == true
            _uiState.update {
                it.copy(
                    items = filterVisibleItems(resolvedItems, isAdmin = isAdmin),
                    isCatalogLoading = false,
                    isLoggedIn = user != null,
                    isAdmin = isAdmin,
                    errorMessage = if (resolvedItems.isNotEmpty()) null else itemsResult.exceptionOrNull()?.message,
                    isSyncingCatalog = false,
                    toastMessage = AppTextResolver.string(R.string.shop_toast_sync_reloaded),
                    isErrorToast = false,
                )
            }
        } else {
            val fallbackItems = shopifyPublicCatalogClient.fetchCatalog().getOrDefault(emptyList())
            val syncErrorMessage = resolveSyncErrorMessage(syncResult.exceptionOrNull())
            _uiState.update {
                it.copy(
                    isCatalogLoading = false,
                    isSyncingCatalog = false,
                    items = if (fallbackItems.isNotEmpty()) {
                        allItems = fallbackItems
                        filterVisibleItems(fallbackItems, isAdmin = it.isAdmin)
                    } else {
                        it.items
                    },
                    errorMessage = if (fallbackItems.isNotEmpty()) null else it.errorMessage,
                    toastMessage = if (fallbackItems.isNotEmpty()) {
                        AppTextResolver.string(R.string.shop_toast_sync_loaded_from_store)
                    } else {
                        syncErrorMessage
                    },
                    isErrorToast = fallbackItems.isEmpty(),
                )
            }
        }
    }

    private fun postSuccessToast(
        message: String,
        isUpdatingStoreState: Boolean? = null,
        isSyncingCatalog: Boolean? = null,
    ) {
        _uiState.update { state ->
            state.copy(
                toastMessage = message,
                isErrorToast = false,
                isUpdatingStoreState = isUpdatingStoreState ?: state.isUpdatingStoreState,
                isSyncingCatalog = isSyncingCatalog ?: state.isSyncingCatalog,
            )
        }
    }

    private fun postErrorToast(
        message: String,
        isUpdatingStoreState: Boolean? = null,
        isSyncingCatalog: Boolean? = null,
    ) {
        _uiState.update { state ->
            state.copy(
                toastMessage = message,
                isErrorToast = true,
                isUpdatingStoreState = isUpdatingStoreState ?: state.isUpdatingStoreState,
                isSyncingCatalog = isSyncingCatalog ?: state.isSyncingCatalog,
            )
        }
    }

    private fun clearTransientMessages(clearError: Boolean, clearToast: Boolean) {
        _uiState.update { state ->
            state.copy(
                errorMessage = if (clearError) null else state.errorMessage,
                toastMessage = if (clearToast) null else state.toastMessage,
            )
        }
    }

    private fun setSyncInProgress(isCatalogLoading: Boolean, loadingToast: String? = null) {
        _uiState.update {
            it.copy(
                isSyncingCatalog = true,
                isCatalogLoading = isCatalogLoading,
                toastMessage = loadingToast ?: it.toastMessage,
                isErrorToast = false,
            )
        }
    }

    private fun resolveSyncErrorMessage(error: Throwable?): String {
        return error
            ?.toAppCheckVerificationMessage("den Shopify-Sync erneut starten")
            ?: error?.message
            ?: AppTextResolver.string(R.string.shop_error_sync_failed)
    }
}
