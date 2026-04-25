package com.nash.skyos.data

import com.google.firebase.functions.FirebaseFunctions

class ShopifyMerchSyncClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun triggerSync(): Result<String> {
        return runCatching {
            val result = functions.callWithAppCheckRetry(
                functionName = "syncShopifyMerch",
                payload = emptyMap<String, Any>(),
            )
            val data = result.data as? Map<*, *>
            val synced = (data?.get("syncedCount") as? Number)?.toInt() ?: 0
            val created = (data?.get("createdCount") as? Number)?.toInt() ?: 0
            val updated = (data?.get("updatedCount") as? Number)?.toInt() ?: 0
            val deactivated = (data?.get("deactivatedCount") as? Number)?.toInt() ?: 0
            val collectionHandles = when (val rawHandles = data?.get("collectionHandles")) {
                is List<*> -> rawHandles.mapNotNull { (it as? String)?.trim()?.takeIf(String::isNotBlank) }
                else -> listOfNotNull((data?.get("collectionHandle") as? String)?.trim()?.takeIf(String::isNotBlank))
            }

            if (collectionHandles.isNotEmpty()) {
                "Shopify-Sync abgeschlossen: ${collectionHandles.size} Collections, $synced Produkte, $created neu, $updated aktualisiert, $deactivated ausgeblendet."
            } else {
                "Shopify-Sync abgeschlossen: $synced Produkte, $created neu, $updated aktualisiert, $deactivated ausgeblendet."
            }
        }
    }
}
