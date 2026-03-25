package com.skydown.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.skydown.shared.model.MerchandiseItem
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
            }.sortedBy { it.name }
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
            firestore.collection("users").document(uid).get().await().toSharedUser()
        }
    }

    override suspend fun addItem(item: MerchandiseItem, imageDataList: List<ByteArray>): Result<Unit> {
        return runCatching {
            val imageUrls = imageDataList.map { imageData ->
                val imageReference = storage.reference.child("merchandise/${UUID.randomUUID()}.jpg")
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()

                imageReference.putBytes(imageData, metadata).await()
                imageReference.downloadUrl.await().toString()
            }

            firestore.collection("merchandise")
                .add(item.copy(id = null, imageUrls = imageUrls))
                .await()
        }
    }

    override suspend fun updatePrice(itemId: String, newPrice: Double): Result<Unit> {
        return runCatching {
            firestore.collection("merchandise").document(itemId).update("price", newPrice).await()
        }
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> {
        return runCatching {
            firestore.collection("merchandise").document(itemId).delete().await()
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
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toSharedUser(): User? {
    val data = data ?: return null
    return User(
        id = id,
        email = data["email"] as? String ?: return null,
        username = data["username"] as? String ?: return null,
        whatsApp = data["whatsApp"] as? String,
        registrationDateEpochMillis = (data["registrationDateEpochMillis"] as? Number)?.toLong()
            ?: (data["registrationDate"] as? com.google.firebase.Timestamp)?.toDate()?.time
            ?: System.currentTimeMillis(),
        isAdmin = data["isAdmin"] as? Boolean ?: false,
    )
}
