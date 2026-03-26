package com.skydown.android.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order
import com.skydown.shared.model.OrderItem
import com.skydown.shared.repository.OrderRepository
import kotlinx.coroutines.tasks.await

class AndroidOrderRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : OrderRepository {
    override suspend fun loadOrders(): Result<List<Order>> {
        return runCatching {
            firestore.collection("orders")
                .orderBy("timestamp")
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    val rawItems = data["items"] as? List<Map<String, Any?>> ?: emptyList()
                    Order(
                        id = document.id,
                        userEmail = data["userEmail"] as? String ?: "",
                        customerName = data["customerName"] as? String,
                        customerEmail = data["customerEmail"] as? String,
                        whatsApp = data["whatsApp"] as? String,
                        message = data["message"] as? String,
                        items = rawItems.mapIndexed { index, item ->
                            OrderItem(
                                id = item["id"] as? String ?: "${document.id}-$index",
                                name = item["name"] as? String ?: "",
                                quantity = (item["quantity"] as? Number)?.toInt() ?: 0,
                                size = item["size"] as? String,
                            )
                        },
                        isCompleted = data["isCompleted"] as? Boolean ?: false,
                        timestampEpochMillis = (data["timestamp"] as? Timestamp)?.toDate()?.time ?: 0L,
                    )
                }
                .sortedByDescending { it.timestampEpochMillis }
        }
    }

    override suspend fun submitOrder(
        userEmail: String,
        items: List<CartItem>,
        customerName: String,
        customerEmail: String,
        whatsApp: String,
        message: String,
    ): Result<Unit> {
        return runCatching {
            val orderItems = items.map { item ->
                mapOf(
                    "name" to item.item.name,
                    "quantity" to item.quantity,
                    "size" to item.size,
                )
            }

            firestore.collection("orders").add(
                mapOf(
                    "userEmail" to userEmail,
                    "customerName" to customerName,
                    "customerEmail" to customerEmail,
                    "whatsApp" to whatsApp,
                    "message" to message,
                    "items" to orderItems,
                    "isCompleted" to false,
                    "timestamp" to Timestamp.now(),
                ),
            ).await()
        }
    }

    override suspend fun toggleCompleted(orderId: String, isCompleted: Boolean): Result<Unit> {
        return runCatching {
            firestore.collection("orders").document(orderId)
                .update("isCompleted", !isCompleted)
                .await()
        }
    }

    override suspend fun deleteOrder(orderId: String): Result<Unit> {
        return runCatching {
            firestore.collection("orders").document(orderId).delete().await()
        }
    }
}
