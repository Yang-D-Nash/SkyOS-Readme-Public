package com.skydown.android.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.skydown.shared.model.CartItem
import com.skydown.shared.model.Order
import com.skydown.shared.model.OrderItem
import com.skydown.shared.model.OrderSubmission
import com.skydown.shared.model.ShippingAddressData
import com.skydown.shared.repository.OrderRepository
import kotlinx.coroutines.tasks.await

class AndroidOrderRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
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
                    val rawItems = (data["items"] as? List<*>)
                        ?.mapNotNull { it as? Map<*, *> }
                        .orEmpty()
                    Order(
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
                        paymentStatus = data["paymentStatus"] as? String,
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
