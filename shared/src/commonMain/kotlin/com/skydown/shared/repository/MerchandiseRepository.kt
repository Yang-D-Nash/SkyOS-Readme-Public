package com.skydown.shared.repository

import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.User

interface MerchandiseRepository {
    suspend fun loadItems(): Result<List<MerchandiseItem>>
    suspend fun currentUser(): Result<User?>
    suspend fun addItem(item: MerchandiseItem, imageDataList: List<ByteArray> = emptyList()): Result<Unit>
    suspend fun updatePrice(itemId: String, newPrice: Double): Result<Unit>
    suspend fun deleteItem(itemId: String): Result<Unit>
}
