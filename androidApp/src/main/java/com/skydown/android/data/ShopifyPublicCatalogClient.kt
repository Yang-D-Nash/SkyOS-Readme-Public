package com.skydown.android.data

import com.google.firebase.firestore.FirebaseFirestore
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.MerchandiseVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ShopifyPublicCatalogClient(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun fetchCatalog(): Result<List<MerchandiseItem>> = runCatching {
        val config = loadConfig()
        var products = fetchProducts(
            storeDomain = config.storeDomain,
            collectionHandle = config.collectionHandle,
        )

        if (products.isEmpty() && !config.collectionHandle.isNullOrBlank()) {
            products = fetchProducts(
                storeDomain = config.storeDomain,
                collectionHandle = null,
            )
        }

        products
            .mapNotNull { product ->
                product.toMerchandiseItem()
            }
            .sortedBy { it.name.lowercase() }
    }

    private suspend fun loadConfig(): ShopifyPublicCatalogConfig {
        val snapshot = firestore.collection("appConfig").document("shopifyMerch").get().await()
        val storefrontUrl = normalizeUrlString(snapshot.getString("storefrontURL"))
        val storeDomain = normalizeStoreDomain(snapshot.getString("storeDomain"))
            ?: normalizeStoreDomain(storefrontUrl)
            ?: "k5t1sc-ps.myshopify.com"
        val collectionHandle = normalizeCollectionHandle(
            value = snapshot.getString("collectionHandle"),
            fallbackUrl = storefrontUrl,
        )

        return ShopifyPublicCatalogConfig(
            storeDomain = storeDomain,
            collectionHandle = collectionHandle,
        )
    }

    private suspend fun fetchProducts(
        storeDomain: String,
        collectionHandle: String?,
    ): List<ShopifyPublicProduct> = withContext(Dispatchers.IO) {
        val path = if (!collectionHandle.isNullOrBlank()) {
            "/collections/$collectionHandle/products.json"
        } else {
            "/products.json"
        }
        val url = URL("https://$storeDomain$path?limit=250")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw IllegalStateException("Shopify-Store antwortet mit $statusCode.")
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)
            val products = root.optJSONArray("products") ?: return@withContext emptyList()

            buildList {
                for (index in 0 until products.length()) {
                    val rawProduct = products.optJSONObject(index) ?: continue
                    add(ShopifyPublicProduct.fromJson(rawProduct))
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}

private data class ShopifyPublicCatalogConfig(
    val storeDomain: String,
    val collectionHandle: String?,
)

private data class ShopifyPublicProduct(
    val id: Long,
    val title: String,
    val bodyHtml: String,
    val handle: String,
    val imageUrls: List<String>,
    val options: List<String>,
    val variants: List<ShopifyPublicVariant>,
) {
    fun toMerchandiseItem(): MerchandiseItem? {
        if (variants.isEmpty()) return null

        val mappedVariants = variants.map { variant ->
            MerchandiseVariant(
                id = "gid://shopify/ProductVariant/${variant.id}",
                title = variant.title,
                size = variant.resolveOptionValue(options, setOf("size", "groesse", "größe")),
                color = variant.resolveOptionValue(options, setOf("color", "colour", "farbe")),
                shopifyVariantId = "gid://shopify/ProductVariant/${variant.id}",
                sku = variant.sku,
                price = variant.price.toDoubleOrNull() ?: 0.0,
                currency = "EUR",
                availableForSale = variant.isAvailable,
            )
        }

        return MerchandiseItem(
            id = "shopify_$id",
            name = title,
            price = mappedVariants.firstOrNull()?.price ?: 0.0,
            description = bodyHtml.stripHtml(),
            imageUrls = imageUrls.distinct(),
            available = mappedVariants.any { it.availableForSale },
            currency = "EUR",
            sku = mappedVariants.firstOrNull()?.sku,
            shopifyProductId = "gid://shopify/Product/$id",
            shopifyHandle = handle,
            availableForSale = mappedVariants.any { it.availableForSale },
            shopifySyncActive = true,
            variants = mappedVariants,
            source = "shopify",
            isVisibleInApp = true,
            featured = false,
            sortOrder = 0,
            customBadge = "",
            customImageOverride = "",
        )
    }

    companion object {
        fun fromJson(json: JSONObject): ShopifyPublicProduct {
            val imagesArray = json.optJSONArray("images")
            val imageUrls = buildList {
                json.optJSONObject("image")?.optString("src")?.takeIf { it.isNotBlank() }?.let(::add)
                if (imagesArray != null) {
                    for (index in 0 until imagesArray.length()) {
                        imagesArray.optJSONObject(index)?.optString("src")
                            ?.takeIf { it.isNotBlank() }
                            ?.let(::add)
                    }
                }
            }

            val optionsArray = json.optJSONArray("options")
            val options = buildList {
                if (optionsArray != null) {
                    for (index in 0 until optionsArray.length()) {
                        optionsArray.optJSONObject(index)?.optString("name")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { add(it.lowercase()) }
                    }
                }
            }

            val variantsArray = json.optJSONArray("variants")
            val variants = buildList {
                if (variantsArray != null) {
                    for (index in 0 until variantsArray.length()) {
                        variantsArray.optJSONObject(index)?.let { add(ShopifyPublicVariant.fromJson(it)) }
                    }
                }
            }

            return ShopifyPublicProduct(
                id = json.optLong("id"),
                title = json.optString("title"),
                bodyHtml = json.optString("body_html"),
                handle = json.optString("handle"),
                imageUrls = imageUrls,
                options = options,
                variants = variants,
            )
        }
    }
}

private data class ShopifyPublicVariant(
    val id: Long,
    val title: String,
    val price: String,
    val sku: String?,
    val available: Boolean?,
    val inventoryQuantity: Int?,
    val option1: String?,
    val option2: String?,
    val option3: String?,
) {
    val isAvailable: Boolean
        get() = available ?: inventoryQuantity?.let { it > 0 } ?: true

    fun resolveOptionValue(optionNames: List<String>, supportedKeys: Set<String>): String? {
        val values = listOf(option1, option2, option3)
        optionNames.forEachIndexed { index, optionName ->
            if (supportedKeys.contains(optionName) && index < values.size) {
                return values[index]?.trim()?.takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    companion object {
        fun fromJson(json: JSONObject): ShopifyPublicVariant {
            return ShopifyPublicVariant(
                id = json.optLong("id"),
                title = json.optString("title"),
                price = json.optString("price"),
                sku = json.optString("sku").takeIf { it.isNotBlank() },
                available = if (json.has("available")) json.optBoolean("available") else null,
                inventoryQuantity = if (json.has("inventory_quantity")) json.optInt("inventory_quantity") else null,
                option1 = json.optString("option1").takeIf { it.isNotBlank() },
                option2 = json.optString("option2").takeIf { it.isNotBlank() },
                option3 = json.optString("option3").takeIf { it.isNotBlank() },
            )
        }
    }
}

private fun String.stripHtml(): String {
    return replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun normalizeStoreDomain(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    return trimmed
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore("/")
        .trim()
        .lowercase()
        .ifBlank { null }
}

private fun normalizeUrlString(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    return if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
        trimmed.trimEnd('/')
    } else {
        "https://$trimmed".trimEnd('/')
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
