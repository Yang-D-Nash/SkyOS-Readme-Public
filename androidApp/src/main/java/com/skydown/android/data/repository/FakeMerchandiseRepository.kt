package com.skydown.android.data.repository

import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.User
import com.skydown.shared.model.sampleMerchandiseItems
import com.skydown.shared.model.sampleUser
import com.skydown.shared.repository.MerchandiseRepository

class FakeMerchandiseRepository : MerchandiseRepository {
    private var items = sampleMerchandiseItems()

    override suspend fun loadItems(): Result<List<MerchandiseItem>> = Result.success(items)

    override suspend fun currentUser(): Result<User?> = Result.success(sampleUser())

    override suspend fun addItem(item: MerchandiseItem, imageDataList: List<ByteArray>): Result<Unit> {
        items = items + item
        return Result.success(Unit)
    }

    override suspend fun updatePrice(itemId: String, newPrice: Double): Result<Unit> {
        items = items.map { item ->
            if (item.id == itemId) item.copy(price = newPrice) else item
        }
        return Result.success(Unit)
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> {
        items = items.filterNot { it.id == itemId }
        return Result.success(Unit)
    }
}
