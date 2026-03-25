package com.skydown.shared.service

import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.repository.MerchandiseRepository

class MerchandiseService(
    private val repository: MerchandiseRepository,
) {
    suspend fun loadItems(): Result<List<MerchandiseItem>> = repository.loadItems()

    suspend fun addItem(
        item: MerchandiseItem,
        imageDataList: List<ByteArray> = emptyList(),
    ): Result<Unit> {
        val currentUser = repository.currentUser().getOrNull()
        if (currentUser?.isAdmin != true) {
            return Result.failure(IllegalAccessException("Nur Admins duerfen Artikel hinzufuegen."))
        }

        return repository.addItem(item, imageDataList)
    }

    suspend fun updatePrice(itemId: String, newPrice: Double): Result<Unit> {
        val currentUser = repository.currentUser().getOrNull()
        if (currentUser?.isAdmin != true) {
            return Result.failure(IllegalAccessException("Nur Admins duerfen Artikel bearbeiten."))
        }

        if (itemId.isBlank()) {
            return Result.failure(IllegalArgumentException("Artikel hat keine gueltige ID."))
        }

        return repository.updatePrice(itemId, newPrice)
    }

    suspend fun deleteItem(itemId: String): Result<Unit> {
        val currentUser = repository.currentUser().getOrNull()
        if (currentUser?.isAdmin != true) {
            return Result.failure(IllegalAccessException("Nur Admins duerfen Artikel loeschen."))
        }

        if (itemId.isBlank()) {
            return Result.failure(IllegalArgumentException("Artikel hat keine gueltige ID."))
        }

        return repository.deleteItem(itemId)
    }
}
