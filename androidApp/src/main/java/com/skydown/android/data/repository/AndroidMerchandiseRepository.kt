package com.skydown.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.MerchandiseVariant
import com.skydown.shared.model.User
import com.skydown.shared.repository.MerchandiseRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AndroidMerchandiseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
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

    override suspend fun currentUser(): Result<User?> {
        return runCatching {
            val uid = auth.currentUser?.uid ?: return@runCatching null
            firestore.collection("users").document(uid).get().await().toSharedUser(auth.currentUser)
        }
    }

    override suspend fun addItem(item: MerchandiseItem, imageDataList: List<ByteArray>): Result<Unit> {
        return runCatching {
            val imageUrls = uploadImages(imageDataList)
            firestore.collection("merchandise")
                .add(merchandisePayload(item = item, imageUrls = imageUrls))
                .await()
        }
    }

    override suspend fun updateItem(item: MerchandiseItem, imageDataList: List<ByteArray>): Result<Unit> {
        return runCatching {
            val itemId = item.id ?: error("Artikel hat keine gueltige ID.")
            val documentReference = firestore.collection("merchandise").document(itemId)
            val replacementImageUrls = if (imageDataList.isNotEmpty()) {
                uploadImages(imageDataList)
            } else {
                null
            }
            val updatedImageUrls = replacementImageUrls ?: item.imageUrls

            documentReference
                .set(
                    merchandisePayload(item = item, imageUrls = updatedImageUrls),
                    SetOptions.merge(),
                )
                .await()

            if (replacementImageUrls != null) {
                deleteImages(item.imageUrls)
            }
        }
    }

    override suspend fun updatePrice(itemId: String, newPrice: Double): Result<Unit> {
        return runCatching {
            firestore.collection("merchandise").document(itemId).update("price", newPrice).await()
        }
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> {
        return runCatching {
            val documentReference = firestore.collection("merchandise").document(itemId)
            val snapshot = documentReference.get().await()
            val imageUrls = snapshot.toSharedMerchandiseItem()?.imageUrls.orEmpty()
            documentReference.delete().await()
            deleteImages(imageUrls)
        }
    }

    private suspend fun uploadImages(imageDataList: List<ByteArray>): List<String> {
        return imageDataList.mapIndexed { index, imageData ->
            val uploadId = UUID.randomUUID().toString()
            val imageReference = storage.reference.child(
                "merchandise/items/$uploadId/image-${index + 1}.jpg",
            )
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            imageReference.putBytes(imageData, metadata).await()
            imageReference.downloadUrl.await().toString()
        }
    }

    private suspend fun deleteImages(imageUrls: List<String>) {
        imageUrls.forEach { imageUrl ->
            runCatching {
                storage.getReferenceFromUrl(imageUrl).delete().await()
            }
        }
    }

    private fun merchandisePayload(item: MerchandiseItem, imageUrls: List<String>): Map<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "name" to item.name,
            "price" to item.price,
            "description" to item.description,
            "imageUrls" to imageUrls,
            "imageURLs" to imageUrls,
            "available" to item.available,
            "currency" to item.currency,
            "availableForSale" to item.availableForSale,
            "variants" to item.variants.map { variant ->
                buildMap<String, Any> {
                    put("id", variant.id)
                    put("title", variant.title)
                    variant.size?.let { put("size", it) }
                    variant.color?.let { put("color", it) }
                    variant.shopifyVariantId?.let { put("shopifyVariantId", it) }
                    variant.sku?.let { put("sku", it) }
                    put("price", variant.price)
                    put("currency", variant.currency)
                    put("availableForSale", variant.availableForSale)
                }
            },
            "source" to item.source,
            "isVisibleInApp" to item.isVisibleInApp,
            "featured" to item.featured,
            "sortOrder" to item.sortOrder,
            "customBadge" to item.customBadge,
            "customImageOverride" to item.customImageOverride,
        )

        item.sku?.let { payload["sku"] = it }
        item.shopifyProductId?.let { payload["shopifyProductId"] = it }
        item.shopifyHandle?.let { payload["shopifyHandle"] = it }

        return payload
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
    )
}
