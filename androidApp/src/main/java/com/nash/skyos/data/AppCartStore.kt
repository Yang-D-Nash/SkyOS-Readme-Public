package com.nash.skyos.data

import com.skydown.shared.model.CartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object AppCartStore {
    private val mutableItems = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = mutableItems

    fun setItems(items: List<CartItem>) {
        mutableItems.value = items
    }

    fun update(transform: (List<CartItem>) -> List<CartItem>) {
        mutableItems.update(transform)
    }

    fun clear() {
        mutableItems.value = emptyList()
    }
}
