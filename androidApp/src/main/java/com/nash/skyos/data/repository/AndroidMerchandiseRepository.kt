package com.nash.skyos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.MerchandiseVariant
import com.skydown.shared.repository.MerchandiseRepository
import kotlinx.coroutines.tasks.await

class AndroidMerchandiseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : MerchandiseRepository {
    override suspend fun loadItems(): Result<List<MerchandiseItem>> {
        return runCatching {
            firestore.collection("merchandise").get().await().documents.mapNotNull { document ->
                document.toSharedMerchandiseItem()
            }.sortedWith(
                compareBy<MerchandiseItem> { !it.featured }
                    .thenBy { it.sortOrder }
                    .thenBy { it.name.lowercase() },
            )
        }.recoverCatching { error ->
            if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw IllegalStateException("Merch kann aktuell nicht geladen werden. Bitte zuerst anmelden oder die Firestore-Leserechte pruefen.")
            }
            throw error
        }
    }

}

private fun com.google.firebase.firestore.DocumentSnapshot.toSharedMerchandiseItem(): MerchandiseItem? {
    val data = data ?: return null
    return MerchandiseItem(
        id = id,
        name = data["name"] as? String ?: return null,
        price = (data["price"] as? Number)?.toDouble() ?: return null,
        description = data["description"] as? String ?: "",
        imageUrls = (data["imageURLs"] as? List<*>)?.mapNotNull { it as? String }
            ?: (data["imageUrls"] as? List<*>)?.mapNotNull { it as? String }
            ?: emptyList(),
        available = data["available"] as? Boolean ?: true,
        currency = data["currency"] as? String ?: "EUR",
        sku = data["sku"] as? String,
        shopifyProductId = data["shopifyProductId"] as? String,
        shopifyHandle = data["shopifyHandle"] as? String,
        availableForSale = data["availableForSale"] as? Boolean ?: (data["available"] as? Boolean ?: true),
        shopifySyncActive = data["shopifySyncActive"] as? Boolean ?: true,
        variants = (data["variants"] as? List<*>)?.mapNotNull { rawVariant ->
            val variant = rawVariant as? Map<*, *> ?: return@mapNotNull null
            MerchandiseVariant(
                id = variant["id"] as? String ?: "",
                title = variant["title"] as? String ?: "",
                size = variant["size"] as? String,
                color = variant["color"] as? String,
                shopifyVariantId = variant["shopifyVariantId"] as? String,
                sku = variant["sku"] as? String,
                price = (variant["price"] as? Number)?.toDouble() ?: 0.0,
                currency = variant["currency"] as? String ?: "EUR",
                availableForSale = variant["availableForSale"] as? Boolean ?: true,
            )
        } ?: emptyList(),
        source = data["source"] as? String ?: "manual",
        isVisibleInApp = data["isVisibleInApp"] as? Boolean ?: true,
        featured = data["featured"] as? Boolean ?: false,
        sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
        customBadge = data["customBadge"] as? String ?: "",
        customImageOverride = data["customImageOverride"] as? String ?: "",
        category = data["category"] as? String
            ?: data["collabCategory"] as? String
            ?: data["collection"] as? String
            ?: "",
        collabPartner = data["collabPartner"] as? String
            ?: data["collab"] as? String
            ?: "",
        shopifyCollectionHandles = (data["shopifyCollectionHandles"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
    )
}
