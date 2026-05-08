package com.nash.skyos.data.repository

import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.MerchandiseVariant
import com.skydown.shared.repository.MerchandiseRepository

object UiTestMerchandiseRepository : MerchandiseRepository {
    override suspend fun loadItems(): Result<List<MerchandiseItem>> = Result.success(
        listOf(
            MerchandiseItem(
                id = "ui-test-merch-item",
                name = "Skydown Atelier Hoodie",
                price = 89.0,
                description = "Schwerer Signature-Drop fuer den Android-Merch-Flow.",
                imageUrls = listOf(
                    "https://picsum.photos/seed/skydown-merch-1/1080/1440",
                    "https://picsum.photos/seed/skydown-merch-2/1080/1440",
                ),
                available = true,
                source = "shopify",
                featured = true,
                customBadge = "ATELIER",
                category = "Signature",
                collabPartner = "Skydown",
                shopifyProductId = "gid://shopify/Product/ui-test-merch",
                shopifyHandle = "skydown-atelier-hoodie",
                variants = listOf(
                    MerchandiseVariant(
                        id = "ui-test-variant-black-m",
                        title = "Black / M",
                        size = "M",
                        color = "Black",
                        shopifyVariantId = "gid://shopify/ProductVariant/ui-test-merch-black-m",
                        sku = "SKY-ATELIER-HOODIE-BLK-M",
                        price = 89.0,
                    ),
                ),
            ),
        ),
    )
}
