package com.nash.skyos.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order
import com.skydown.shared.model.OrderItem
import com.skydown.shared.model.OrderSubmission
import com.skydown.shared.model.ShippingAddressData
import com.skydown.shared.repository.OrderRepository
import kotlinx.coroutines.tasks.await

class AndroidOrderRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) : OrderRepository {
    fun observeOrders(onChange: (Result<List<Order>>) -> Unit): ListenerRegistration? {
        val currentUser = auth.currentUser
            ?: run {
                onChange(Result.failure(IllegalStateException("Bitte melde dich an, um Bestellungen zu laden.")))
                return null
            }

        return ordersQueryFor(currentUser).addSnapshotListener { snapshot, error ->
            when {
                error != null -> onChange(Result.failure(mapOrdersReadError(error)))
                snapshot != null -> onChange(Result.success(snapshot.documents.mapNotNull(::mapOrder).sortedByDescending { it.timestampEpochMillis }))
            }
        }
    }

    override suspend fun loadOrders(): Result<List<Order>> {
        return runCatching {
            val currentUser = auth.currentUser ?: error("Bitte melde dich an, um Bestellungen zu laden.")
            ordersQueryFor(currentUser).get().await()
                .documents
                .mapNotNull(::mapOrder)
                .sortedByDescending { it.timestampEpochMillis }
        }
    }

    override suspend fun submitOrder(submission: OrderSubmission): Result<String> {
        return runCatching {
            val orderItems = submission.items.map { item ->
                mapOf(
                    "productId" to item.item.id,
                    "name" to item.item.name,
                    "quantity" to item.quantity,
                    "size" to item.size,
                    "color" to item.color,
                    "shopifyVariantId" to item.shopifyVariantId,
                    "sku" to item.sku,
                    "unitPrice" to (item.unitPrice ?: item.item.price),
                )
            }
            val response = functions
                .getHttpsCallable("submitMerchOrder")
                .call(
                    mapOf(
                        "userEmail" to submission.userEmail,
                        "customerName" to submission.customerName,
                        "customerEmail" to submission.customerEmail,
                        "whatsApp" to submission.whatsApp,
                        "shippingAddress" to submission.shippingAddress,
                        "shippingAddressData" to mapOf(
                            "address1" to submission.shippingAddressData.address1,
                            "address2" to submission.shippingAddressData.address2,
                            "city" to submission.shippingAddressData.city,
                            "zip" to submission.shippingAddressData.zip,
                            "countryCode" to submission.shippingAddressData.countryCode,
                            "countryName" to submission.shippingAddressData.countryName,
                        ),
                        "shippingZone" to submission.shippingZone,
                        "shippingCountryCode" to submission.shippingCountryCode,
                        "paymentMethod" to submission.paymentMethod,
                        "paymentStatus" to submission.paymentStatus,
                        "subtotalAmount" to submission.subtotalAmount,
                        "shippingAmount" to submission.shippingAmount,
                        "taxRate" to submission.taxRate,
                        "taxAmount" to submission.taxAmount,
                        "totalAmount" to submission.totalAmount,
                        "fulfillmentProvider" to submission.fulfillmentProvider,
                        "message" to submission.message,
                        "items" to orderItems,
                    ),
                )
                .await()

            val data = response.data
            when (data) {
                is Map<*, *> -> (data["orderId"] as? String)?.takeIf { it.isNotBlank() }
                is String -> data.takeIf { it.isNotBlank() }
                else -> null
            } ?: error("Die Bestellung konnte serverseitig nicht angelegt werden.")
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

    private fun ordersQueryFor(currentUser: FirebaseUser): Query {
        val currentEmail = currentUser.email?.trim()?.lowercase().orEmpty()
        return if (currentEmail == UserRole.OWNER_EMAIL) {
            firestore.collection("orders").orderBy("timestamp")
        } else {
            firestore.collection("orders").whereEqualTo("orderOwnerUid", currentUser.uid)
        }
    }

    private fun mapOrder(document: DocumentSnapshot): Order? {
        val data = document.data ?: return null
        val rawItems = (data["items"] as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            .orEmpty()

        return Order(
            id = document.id,
            userEmail = data["userEmail"] as? String ?: "",
            customerName = data["customerName"] as? String,
            customerEmail = data["customerEmail"] as? String,
            whatsApp = data["whatsApp"] as? String,
            shippingAddress = data["shippingAddress"] as? String,
            shippingAddressData = data["shippingAddressData"].toShippingAddressData(),
            shippingZone = data["shippingZone"] as? String,
            shippingCountryCode = data["shippingCountryCode"] as? String,
            paymentMethod = (data["paymentMethod"] as? String)?.takeIf { it.isNotBlank() },
            paymentProvider = (data["paymentProvider"] as? String)?.takeIf { it.isNotBlank() },
            paymentStatus = data["paymentStatus"] as? String,
            paymentReference = (data["paymentReference"] as? String)?.takeIf { it.isNotBlank() },
            subtotalAmount = (data["subtotalAmount"] as? Number)?.toDouble(),
            shippingAmount = (data["shippingAmount"] as? Number)?.toDouble(),
            shippingPriceCharged = (data["shippingPriceCharged"] as? Number)?.toDouble(),
            taxRate = (data["taxRate"] as? Number)?.toDouble(),
            taxAmount = (data["taxAmount"] as? Number)?.toDouble(),
            totalAmount = (data["totalAmount"] as? Number)?.toDouble(),
            fulfillmentProvider = data["fulfillmentProvider"] as? String,
            fulfillmentStatus = data["fulfillmentStatus"] as? String,
            shopifyOrderId = data["shopifyOrderId"] as? String,
            shopifyOrderName = data["shopifyOrderName"] as? String,
            shopifySyncStatus = data["shopifySyncStatus"] as? String,
            stripeCheckoutSessionId = (data["stripeCheckoutSessionId"] as? String)?.takeIf { it.isNotBlank() },
            stripePaymentIntentId = (data["stripePaymentIntentId"] as? String)?.takeIf { it.isNotBlank() },
            stripeCheckoutStatus = (data["stripeCheckoutStatus"] as? String)?.takeIf { it.isNotBlank() },
            message = data["message"] as? String,
            items = rawItems.mapIndexed { index, item ->
                OrderItem(
                    id = item["id"] as? String ?: "${document.id}-$index",
                    name = item["name"] as? String ?: "",
                    quantity = (item["quantity"] as? Number)?.toInt() ?: 0,
                    size = item["size"] as? String,
                    color = item["color"] as? String,
                    productId = item["productId"] as? String,
                    shopifyVariantId = item["shopifyVariantId"] as? String,
                    sku = item["sku"] as? String,
                    unitPrice = (item["unitPrice"] as? Number)?.toDouble(),
                )
            },
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            timestampEpochMillis = (data["timestamp"] as? Timestamp)?.toDate()?.time ?: 0L,
        )
    }

    private fun mapOrdersReadError(error: Exception): Exception {
        val firestoreError = error as? FirebaseFirestoreException
        return if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            IllegalStateException("Keine Berechtigung zum Laden der Bestellungen. Pruefe Owner-Rechte und die Firestore Rules.")
        } else {
            error
        }
    }
}

private fun Any?.toShippingAddressData(): ShippingAddressData? {
    val data = this as? Map<*, *> ?: return null
    val address1 = data["address1"] as? String ?: return null
    val city = data["city"] as? String ?: return null
    val zip = data["zip"] as? String ?: return null
    val countryCode = data["countryCode"] as? String ?: return null
    val countryName = data["countryName"] as? String ?: return null

    return ShippingAddressData(
        address1 = address1,
        address2 = data["address2"] as? String ?: "",
        city = city,
        zip = zip,
        countryCode = countryCode,
        countryName = countryName,
    )
}
