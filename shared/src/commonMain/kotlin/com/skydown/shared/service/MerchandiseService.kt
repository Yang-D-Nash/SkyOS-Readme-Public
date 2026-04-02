package com.skydown.shared.service

import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.repository.MerchandiseRepository

class MerchandiseService(
    private val repository: MerchandiseRepository,
) {
    suspend fun loadItems(): Result<List<MerchandiseItem>> = repository.loadItems()
}
