package com.nash.skyos.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

data class ShopifyAdminSettings(
    val storeDomain: String = "k5t1sc-ps.myshopify.com",
    val storefrontAccessToken: String = "",
    val collectionHandles: List<String> = emptyList(),
) {
    val activeCollectionLabel: String
        get() = when (collectionHandles.size) {
            0 -> "Alle Produkte"
            1 -> collectionHandles.first()
            else -> "${collectionHandles.size} Collections"
        }

    val hasCollectionFilter: Boolean
        get() = collectionHandles.isNotEmpty()

    val primaryCollectionHandle: String?
        get() = collectionHandles.firstOrNull()

    val collectionHandlesDraft: String
        get() = collectionHandles.joinToString(", ")
}

data class ShopifyCollectionOption(
    val handle: String,
    val title: String,
    val productCount: Int? = null,
) {
    val displayTitle: String
        get() = title.ifBlank { handle }
}

class ShopifyAdminSettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    private val collectionName = "appConfig"
    private val documentName = "shopifyMerch"

    fun observeSettings(onChange: (Result<ShopifyAdminSettings>) -> Unit): ListenerRegistration {
        return firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onChange(Result.failure(error))
                return@addSnapshotListener
            }

            onChange(Result.success(snapshot?.data.orEmpty().toShopifyAdminSettings()))
        }
    }

    suspend fun updateSettings(settings: ShopifyAdminSettings): Result<Unit> {
        return runCatching {
            firestore.collection(collectionName).document(documentName).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
        }
    }

    suspend fun fetchAvailableCollections(): Result<List<ShopifyCollectionOption>> {
        return runCatching {
            val response = functions.callWithAppCheckRetry(
                functionName = "listShopifyCollections",
                payload = emptyMap<String, Any>(),
            )

            val data = response.data as? Map<*, *>
            val collections = data?.get("collections") as? List<*>
            collections.orEmpty().mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val handle = (map["handle"] as? String)?.trim().orEmpty()
                if (handle.isBlank()) {
                    return@mapNotNull null
                }

                ShopifyCollectionOption(
                    handle = handle,
                    title = (map["title"] as? String)?.trim().orEmpty().ifBlank { handle },
                    productCount = (map["productCount"] as? Number)?.toInt(),
                )
            }
        }
    }
}

private fun Map<String, Any>.toShopifyAdminSettings(): ShopifyAdminSettings {
    val configuredStorefrontUrl = normalizeUrlString(this["storefrontURL"] as? String)
    val normalizedDomain = normalizeStoreDomain(this["storeDomain"] as? String)
        ?: normalizeStoreDomain(configuredStorefrontUrl)
        ?: ShopifyAdminSettings().storeDomain
    val normalizedCollectionHandles = normalizeCollectionHandles(
        rawValue = this["collectionHandles"],
        legacyValue = this["collectionHandle"] as? String,
        fallbackUrl = configuredStorefrontUrl,
    )

    return ShopifyAdminSettings(
        storeDomain = normalizedDomain,
        storefrontAccessToken = (this["storefrontAccessToken"] as? String).orEmpty().trim(),
        collectionHandles = normalizedCollectionHandles,
    )
}

private fun ShopifyAdminSettings.toMap(): Map<String, Any> {
    val normalizedDomain = normalizeStoreDomain(storeDomain)
        ?: ShopifyAdminSettings().storeDomain
    val normalizedCollectionHandles = normalizeCollectionHandles(
        rawValue = collectionHandles,
        legacyValue = null,
        fallbackUrl = null,
    )

    return mapOf(
        "storeDomain" to normalizedDomain,
        "storefrontAccessToken" to storefrontAccessToken.trim(),
        "collectionHandles" to normalizedCollectionHandles,
        "collectionHandle" to FieldValue.delete(),
        "storefrontURL" to FieldValue.delete(),
        "collectionTitle" to FieldValue.delete(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

private fun normalizeStoreDomain(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return null
    }

    val withoutScheme = trimmed
        .removePrefix("https://")
        .removePrefix("http://")
    val domain = withoutScheme.substringBefore("/").trim().lowercase()
    return domain.ifBlank { null }
}

private fun normalizeUrlString(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return null
    }

    return when {
        trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed.trimEnd('/')
        else -> "https://$trimmed".trimEnd('/')
    }
}

private fun normalizeCollectionHandle(value: String?): String? {
    val direct = value?.trim().orEmpty()
    if (direct.isBlank()) {
        return null
    }

    return direct
        .removePrefix("/collections/")
        .substringBefore("/")
        .trim()
        .ifBlank { null }
}

private fun normalizeCollectionHandles(
    rawValue: Any?,
    legacyValue: String?,
    fallbackUrl: String?,
): List<String> {
    val hasExplicitRawValue = rawValue != null
    val candidates = when (rawValue) {
        is List<*> -> rawValue.mapNotNull { it as? String }
        is String -> rawValue.split('\n', ',')
        else -> legacyValue?.split('\n', ',').orEmpty()
    }.map { it.trim() }
        .filter { it.isNotBlank() }

    val normalized = candidates.mapNotNull(::normalizeCollectionHandle).distinct()
    if (normalized.isNotEmpty()) {
        return normalized
    }
    if (hasExplicitRawValue) {
        return emptyList()
    }

    val url = normalizeUrlString(fallbackUrl) ?: return emptyList()
    val path = url.substringAfter("://", "")
        .substringAfter("/", "")
    val parts = path.split("/").filter { it.isNotBlank() }
    val collectionsIndex = parts.indexOf("collections")
    if (collectionsIndex == -1 || collectionsIndex + 1 >= parts.size) {
        return emptyList()
    }

    return listOfNotNull(normalizeCollectionHandle(parts[collectionsIndex + 1]))
}
