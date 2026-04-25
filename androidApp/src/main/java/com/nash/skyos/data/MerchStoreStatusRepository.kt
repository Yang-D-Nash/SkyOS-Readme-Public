package com.nash.skyos.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class MerchStoreStatus(
    val isOpen: Boolean = true,
)

class MerchStoreStatusRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "appConfig"
    private val documentName = "merchandiseStore"

    fun observeStatus(onChange: (Result<MerchStoreStatus>) -> Unit): ListenerRegistration {
        return firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onChange(Result.failure(error))
                return@addSnapshotListener
            }

            val isOpen = snapshot?.getBoolean("isOpen") ?: true
            onChange(Result.success(MerchStoreStatus(isOpen = isOpen)))
        }
    }

    suspend fun updateStoreOpen(isOpen: Boolean): Result<Unit> {
        return runCatching {
            firestore.collection(collectionName).document(documentName).set(
                mapOf(
                    "isOpen" to isOpen,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }
}
