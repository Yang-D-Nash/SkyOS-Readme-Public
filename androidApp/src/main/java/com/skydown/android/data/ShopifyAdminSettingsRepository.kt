package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class ShopifyAdminSettings(
    val storeDomain: String = "k5t1sc-ps.myshopify.com",
    val storefrontAccessToken: String = "",
    val collectionHandle: String = "",
) {
    val activeCollectionLabel: String
        get() = collectionHandle.trim().ifBlank { "Alle Produkte" }

    val hasCollectionFilter: Boolean
        get() = collectionHandle.isNotBlank()
}

class ShopifyAdminSettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
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
}

private fun Map<String, Any>.toShopifyAdminSettings(): ShopifyAdminSettings {
    val configuredStorefrontUrl = normalizeUrlString(this["storefrontURL"] as? String)
    val normalizedDomain = normalizeStoreDomain(this["storeDomain"] as? String)
        ?: normalizeStoreDomain(configuredStorefrontUrl)
        ?: ShopifyAdminSettings().storeDomain
    val normalizedCollectionHandle = normalizeCollectionHandle(
        value = this["collectionHandle"] as? String,
        fallbackUrl = configuredStorefrontUrl,
    ).orEmpty()

    return ShopifyAdminSettings(
        storeDomain = normalizedDomain,
        storefrontAccessToken = (this["storefrontAccessToken"] as? String).orEmpty().trim(),
        collectionHandle = normalizedCollectionHandle,
    )
}

private fun ShopifyAdminSettings.toMap(): Map<String, Any> {
    val normalizedDomain = normalizeStoreDomain(storeDomain)
        ?: ShopifyAdminSettings().storeDomain
    val normalizedCollectionHandle = normalizeCollectionHandle(
        value = collectionHandle,
        fallbackUrl = null,
    ).orEmpty()

    return mapOf(
        "storeDomain" to normalizedDomain,
        "storefrontAccessToken" to storefrontAccessToken.trim(),
        "collectionHandle" to normalizedCollectionHandle,
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

private fun normalizeCollectionHandle(value: String?, fallbackUrl: String?): String? {
    val direct = value?.trim().orEmpty()
    if (direct.isNotBlank()) {
        return direct
            .removePrefix("/collections/")
            .substringBefore("/")
            .trim()
            .ifBlank { null }
    }

    val url = normalizeUrlString(fallbackUrl) ?: return null
    val path = url.substringAfter("://", "")
        .substringAfter("/", "")
    val parts = path.split("/").filter { it.isNotBlank() }
    val collectionsIndex = parts.indexOf("collections")
    if (collectionsIndex == -1 || collectionsIndex + 1 >= parts.size) {
        return null
    }

    return parts[collectionsIndex + 1].trim().ifBlank { null }
}
