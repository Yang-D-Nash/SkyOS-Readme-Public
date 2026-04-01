package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class MerchOrderPaymentClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun confirmPayment(
        orderId: String,
        paymentMethod: String?,
        paymentReference: String? = null,
    ): Result<String> {
        return runCatching {
            val payload = buildMap<String, Any> {
                put("orderId", orderId)
                paymentMethod?.takeIf { it.isNotBlank() }?.let { put("paymentMethod", it) }
                paymentReference?.takeIf { it.isNotBlank() }?.let { put("paymentReference", it) }
            }

            val response = functions
                .getHttpsCallable("confirmMerchOrderPayment")
                .call(payload)
                .await()

            val data = response.data as? Map<*, *>
            data?.get("message") as? String
                ?: "Zahlung bestaetigt. Shopify und Fulfillment werden jetzt vorbereitet."
        }
    }
}
