package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class ShopifyAdminSettings(
    val storeDomain: String = "k5t1sc-ps.myshopify.com",
    val storefrontUrl: String = "https://k5t1sc-ps.myshopify.com",
    val collectionHandle: String = "",
    val collectionTitle: String = "",
    val adminApiToken: String = "",
) {
    val activeCollectionLabel: String
        get() = collectionTitle.trim().ifBlank {
            collectionHandle.trim().ifBlank { "Alle Produkte" }
        }

    val hasCollectionFilter: Boolean
        get() = collectionHandle.isNotBlank()
}

class ShopifyAdminSettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "appConfig"
    private val documentName = "shopifyMerch"
    private val privateCollectionName = "adminConfig"
    private val privateDocumentName = "shopifyMerchPrivate"

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
            firestore.collection(privateCollectionName).document(privateDocumentName).set(
                mapOf(
                    "adminApiToken" to settings.adminApiToken.trim(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }

    suspend fun fetchAdminApiToken(): Result<String> {
        return runCatching {
            val snapshot = firestore.collection(privateCollectionName).document(privateDocumentName).get().await()
            (snapshot.getString("adminApiToken") ?: "").trim()
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
    val normalizedStorefrontUrl = configuredStorefrontUrl
        ?: deriveStorefrontUrl(normalizedDomain, normalizedCollectionHandle)

    return ShopifyAdminSettings(
        storeDomain = normalizedDomain,
        storefrontUrl = normalizedStorefrontUrl,
        collectionHandle = normalizedCollectionHandle,
        collectionTitle = (this["collectionTitle"] as? String).orEmpty().trim(),
    )
}

private fun ShopifyAdminSettings.toMap(): Map<String, Any> {
    val normalizedDomain = normalizeStoreDomain(storeDomain)
        ?: normalizeStoreDomain(storefrontUrl)
        ?: ShopifyAdminSettings().storeDomain
    val normalizedCollectionHandle = normalizeCollectionHandle(
        value = collectionHandle,
        fallbackUrl = storefrontUrl,
    ).orEmpty()
    val normalizedStorefrontUrl = normalizeUrlString(storefrontUrl)
        ?: deriveStorefrontUrl(normalizedDomain, normalizedCollectionHandle)

    return mapOf(
        "storeDomain" to normalizedDomain,
        "storefrontURL" to normalizedStorefrontUrl,
        "collectionHandle" to normalizedCollectionHandle,
        "collectionTitle" to collectionTitle.trim(),
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

private fun deriveStorefrontUrl(storeDomain: String, collectionHandle: String): String {
    return if (collectionHandle.isNotBlank()) {
        "https://$storeDomain/collections/$collectionHandle"
    } else {
        "https://$storeDomain"
    }
}
