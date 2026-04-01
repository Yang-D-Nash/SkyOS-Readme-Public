package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class ShopifyMerchSyncClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun triggerSync(): Result<String> {
        return runCatching {
            val result = functions
                .getHttpsCallable("syncShopifyMerch")
                .call(emptyMap<String, Any>())
                .await()
            val data = result.data as? Map<*, *>
            val synced = (data?.get("syncedCount") as? Number)?.toInt() ?: 0
            val created = (data?.get("createdCount") as? Number)?.toInt() ?: 0
            val updated = (data?.get("updatedCount") as? Number)?.toInt() ?: 0
            val deactivated = (data?.get("deactivatedCount") as? Number)?.toInt() ?: 0
            val collectionHandle = (data?.get("collectionHandle") as? String).orEmpty().trim()

            if (collectionHandle.isNotBlank()) {
                "Shopify-Sync abgeschlossen: $collectionHandle, $synced Produkte, $created neu, $updated aktualisiert, $deactivated ausgeblendet."
            } else {
                "Shopify-Sync abgeschlossen: $synced Produkte, $created neu, $updated aktualisiert, $deactivated ausgeblendet."
            }
        }
    }
}
