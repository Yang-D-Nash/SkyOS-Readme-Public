package com.nash.skyos.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class NativeSubscriptionPurchase(
    val productId: String,
    val purchaseToken: String,
    val orderId: String?,
)

class AiNativeBillingManager(
    context: Context,
) {
    val packageName: String = context.packageName
    private var pendingPurchaseContinuation: CancellableContinuation<NativeSubscriptionPurchase?>? = null
    private var pendingPurchaseProductId: String? = null
    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .setListener { result, purchases ->
            handlePurchaseUpdate(result, purchases.orEmpty())
        }
        .build()

    suspend fun queryMembershipProducts(config: AiMembershipRuntimeConfig): List<MembershipProduct> {
        ensureConnected()
        val proMonthly = config.androidProMonthlySku
        val proYearly = config.androidProYearlySku
        val creatorMonthly = config.androidCreatorMonthlySku
        val creatorYearly = config.androidCreatorYearlySku
        val products = queryProducts(listOf(proMonthly, proYearly, creatorMonthly, creatorYearly))
        return listOf(
            MembershipProduct(
                planLabel = "Pro",
                monthly = products.firstOrNull { it.productId == proMonthly },
                yearly = products.firstOrNull { it.productId == proYearly },
            ),
            MembershipProduct(
                planLabel = "Creator",
                monthly = products.firstOrNull { it.productId == creatorMonthly },
                yearly = products.firstOrNull { it.productId == creatorYearly },
            ),
        )
    }

    suspend fun launchSubscriptionPurchase(activity: Activity, product: ProductDetails): NativeSubscriptionPurchase? {
        ensureConnected()
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return null
        val billingParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        return withTimeout(120_000L) {
            suspendCancellableCoroutine { continuation ->
                if (pendingPurchaseContinuation != null) {
                    continuation.resumeWithException(IllegalStateException("Ein Billing Flow laeuft bereits."))
                    return@suspendCancellableCoroutine
                }

                pendingPurchaseContinuation = continuation
                pendingPurchaseProductId = product.productId
                continuation.invokeOnCancellation {
                    if (pendingPurchaseContinuation === continuation) {
                        pendingPurchaseContinuation = null
                        pendingPurchaseProductId = null
                    }
                }

                val result = billingClient.launchBillingFlow(activity, billingParams)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    pendingPurchaseContinuation = null
                    pendingPurchaseProductId = null
                    when (result.responseCode) {
                        BillingClient.BillingResponseCode.USER_CANCELED -> continuation.resume(null)
                        else -> continuation.resumeWithException(
                            IllegalStateException(result.debugMessage.ifBlank { "Billing Flow konnte nicht gestartet werden." }),
                        )
                    }
                }
            }
        }
    }

    suspend fun queryOwnedSubscriptions(): List<NativeSubscriptionPurchase> {
        ensureConnected()
        val purchases = suspendCancellableCoroutine<List<Purchase>> { continuation ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                PurchasesResponseListener { result: BillingResult, list: MutableList<Purchase> ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        continuation.resumeWithException(
                            IllegalStateException(result.debugMessage.ifBlank { "Kaeufe konnten nicht geladen werden." }),
                        )
                        return@PurchasesResponseListener
                    }
                    continuation.resume(list.toList())
                },
            )
        }
        return purchases.flatMap { purchase ->
            purchase.products.map { productId ->
                NativeSubscriptionPurchase(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    orderId = purchase.orderId,
                )
            }
        }
    }

    private suspend fun queryProducts(productIds: List<String>): List<ProductDetails> {
        val configuredProductIds = productIds
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (configuredProductIds.isEmpty()) return emptyList()
        return suspendCancellableCoroutine { continuation ->
            val queryProducts = configuredProductIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(queryProducts).build(),
            ) { result, details ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    continuation.resumeWithException(
                        IllegalStateException(result.debugMessage.ifBlank { "Produktdetails konnten nicht geladen werden." }),
                    )
                    return@queryProductDetailsAsync
                }
                continuation.resume(details.productDetailsList)
            }
        }
    }

    private suspend fun ensureConnected() {
        if (billingClient.isReady) return
        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(
                            IllegalStateException(result.debugMessage.ifBlank { "Billing Verbindung fehlgeschlagen." }),
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("Billing Service getrennt."))
                    }
                }
            })
        }
    }

    private fun handlePurchaseUpdate(result: BillingResult, purchases: List<Purchase>) {
        val continuation = pendingPurchaseContinuation ?: return
        val productId = pendingPurchaseProductId
        pendingPurchaseContinuation = null
        pendingPurchaseProductId = null

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                continuation.resume(purchases.toNativeSubscriptionPurchase(productId))
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                continuation.resume(null)
            }
            else -> {
                continuation.resumeWithException(
                    IllegalStateException(result.debugMessage.ifBlank { "Kauf konnte nicht abgeschlossen werden." }),
                )
            }
        }
    }

    private fun List<Purchase>.toNativeSubscriptionPurchase(preferredProductId: String?): NativeSubscriptionPurchase? {
        val matches = flatMap { purchase ->
            purchase.products.map { productId ->
                NativeSubscriptionPurchase(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    orderId = purchase.orderId,
                )
            }
        }
        return matches.firstOrNull { it.productId == preferredProductId } ?: matches.firstOrNull()
    }
}
