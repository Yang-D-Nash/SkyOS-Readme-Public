package com.nash.skyos.data

import com.google.firebase.functions.FirebaseFunctions

data class AndroidSubscriptionSyncResult(
    val status: String,
    val provider: String,
    val plan: String,
    val eventId: String,
)

class AiSubscriptionSyncClient {
    private val functions = FirebaseFunctions.getInstance("us-central1")

    suspend fun requestAndroidSubscriptionSync(
        productId: String,
        purchaseToken: String,
        packageName: String,
        orderId: String? = null,
    ): AndroidSubscriptionSyncResult {
        val payload = mutableMapOf<String, Any>(
            "productId" to productId,
            "purchaseToken" to purchaseToken,
            "packageName" to packageName,
        )
        orderId?.trim()?.takeIf { it.isNotEmpty() }?.let { payload["orderId"] = it }

        val result = functions.callWithAppCheckRetry(
            functionName = "syncAndroidAiSubscriptionStatus",
            payload = payload,
        )
        val data = result.data as? Map<*, *> ?: error("Android Billing Sync konnte nicht verarbeitet werden.")
        return AndroidSubscriptionSyncResult(
            status = (data["status"] as? String).orEmpty(),
            provider = (data["provider"] as? String).orEmpty(),
            plan = (data["plan"] as? String).orEmpty(),
            eventId = (data["eventId"] as? String).orEmpty(),
        )
    }
}
