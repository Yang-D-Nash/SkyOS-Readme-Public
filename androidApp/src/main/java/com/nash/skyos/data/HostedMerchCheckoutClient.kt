package com.nash.skyos.data

import com.google.firebase.functions.FirebaseFunctions
import com.skydown.shared.model.OrderSubmission
import kotlinx.coroutines.tasks.await

data class HostedCheckoutSession(
    val orderId: String,
    val checkoutUrl: String,
    val sessionId: String? = null,
)

class HostedMerchCheckoutClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun startCheckout(
        submission: OrderSubmission,
        paymentMethod: String,
        platform: String = "android",
    ): Result<HostedCheckoutSession> {
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
                .getHttpsCallable("startMerchCheckout")
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
                        "paymentMethod" to paymentMethod,
                        "subtotalAmount" to submission.subtotalAmount,
                        "shippingAmount" to submission.shippingAmount,
                        "taxRate" to submission.taxRate,
                        "taxAmount" to submission.taxAmount,
                        "totalAmount" to submission.totalAmount,
                        "fulfillmentProvider" to submission.fulfillmentProvider,
                        "message" to submission.message,
                        "items" to orderItems,
                        "platform" to platform,
                    ),
                )
                .await()

            val data = response.data as? Map<*, *>
                ?: error("Hosted Checkout konnte serverseitig nicht vorbereitet werden.")

            val orderId = (data["orderId"] as? String)?.takeIf { it.isNotBlank() }
                ?: error("Hosted Checkout lieferte keine orderId.")
            val checkoutUrl = (data["checkoutUrl"] as? String)?.takeIf { it.isNotBlank() }
                ?: error("Hosted Checkout lieferte keine URL.")

            HostedCheckoutSession(
                orderId = orderId,
                checkoutUrl = checkoutUrl,
                sessionId = (data["sessionId"] as? String)?.takeIf { it.isNotBlank() },
            )
        }
    }
}
