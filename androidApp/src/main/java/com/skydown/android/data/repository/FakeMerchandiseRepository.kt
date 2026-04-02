package com.skydown.android.data.repository

import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.sampleMerchandiseItems
import com.skydown.shared.repository.MerchandiseRepository

class FakeMerchandiseRepository : MerchandiseRepository {
    private var items = sampleMerchandiseItems()

    override suspend fun loadItems(): Result<List<MerchandiseItem>> = Result.success(items)
}
