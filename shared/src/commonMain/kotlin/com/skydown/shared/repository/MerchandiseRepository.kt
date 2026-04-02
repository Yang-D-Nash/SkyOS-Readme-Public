package com.skydown.shared.repository

import com.skydown.shared.model.MerchandiseItem

interface MerchandiseRepository {
    suspend fun loadItems(): Result<List<MerchandiseItem>>
}
