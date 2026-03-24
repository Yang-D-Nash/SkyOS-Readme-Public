package com.skydown.android.ui.model

import com.skydown.shared.model.MerchandiseItem

data class ShopUiState(
    val items: List<MerchandiseItem> = emptyList(),
    val isAdmin: Boolean = false,
    val selectedItem: MerchandiseItem? = null,
    val errorMessage: String? = null,
)
