package com.skydown.android.ui.model

import com.skydown.shared.model.MerchandiseItem

data class ShopUiState(
    val items: List<MerchandiseItem> = emptyList(),
    val isCatalogLoading: Boolean = true,
    val isStoreOpen: Boolean = true,
    val isLoggedIn: Boolean = false,
    val isAdmin: Boolean = false,
    val selectedItem: MerchandiseItem? = null,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    val isErrorToast: Boolean = false,
    val isUpdatingStoreState: Boolean = false,
    val isSyncingCatalog: Boolean = false,
)
