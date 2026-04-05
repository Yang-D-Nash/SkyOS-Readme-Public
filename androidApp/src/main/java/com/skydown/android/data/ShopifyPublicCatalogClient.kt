package com.skydown.android.data

import com.google.firebase.firestore.FirebaseFirestore
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.MerchandiseVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
            storefrontAccessToken = config.storefrontAccessToken,
            collectionHandle = config.collectionHandle,
        )

        if (products.isEmpty() && !config.collectionHandle.isNullOrBlank()) {
            products = fetchProducts(
                storeDomain = config.storeDomain,
                storefrontAccessToken = config.storefrontAccessToken,
                collectionHandle = null,
            )
        }

        products
            .mapNotNull { product -> product.toMerchandiseItem() }
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
            storefrontAccessToken = snapshot.getString("storefrontAccessToken").orEmpty().trim(),
            collectionHandle = collectionHandle,
        )
    }

    private suspend fun fetchProducts(
        storeDomain: String,
        storefrontAccessToken: String,
        collectionHandle: String?,
    ): List<ShopifyStorefrontProduct> {
        val products = mutableListOf<ShopifyStorefrontProduct>()
        var cursor: String? = null
        var hasNextPage = true

        while (hasNextPage) {
            val page = fetchProductConnection(
                storeDomain = storeDomain,
                storefrontAccessToken = storefrontAccessToken,
                collectionHandle = collectionHandle,
                cursor = cursor,
            )
            products += page.products
            hasNextPage = page.hasNextPage
            cursor = page.endCursor
        }

        return products
    }

    private suspend fun fetchProductConnection(
        storeDomain: String,
        storefrontAccessToken: String,
        collectionHandle: String?,
        cursor: String?,
    ): ShopifyStorefrontProductPage = withContext(Dispatchers.IO) {
        val url = URL("https://$storeDomain/api/2026-01/graphql.json")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            if (storefrontAccessToken.isNotBlank()) {
                setRequestProperty("X-Shopify-Storefront-Access-Token", storefrontAccessToken)
            }
        }

        val variables = JSONObject().apply {
            put("cursor", cursor ?: JSONObject.NULL)
            if (!collectionHandle.isNullOrBlank()) {
                put("handle", collectionHandle)
            }
        }
        val payload = JSONObject().apply {
            put(
                "query",
                if (collectionHandle.isNullOrBlank()) {
                    SHOPIFY_STOREFRONT_PRODUCTS_QUERY
                } else {
                    SHOPIFY_STOREFRONT_COLLECTION_PRODUCTS_QUERY
                },
            )
            put("variables", variables)
        }

        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(payload.toString())
        }

        try {
            val statusCode = connection.responseCode
            val responseStream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val root = if (responseText.isNotBlank()) JSONObject(responseText) else JSONObject()
            val errors = root.optJSONArray("errors")

            if (statusCode !in 200..299) {
                throw IllegalStateException(graphQlErrorMessage(errors, "Shopify-Store antwortet mit $statusCode."))
            }
            if (errors != null && errors.length() > 0) {
                throw IllegalStateException(graphQlErrorMessage(errors, "Shopify Storefront GraphQL Fehler."))
            }

            val data = root.optJSONObject("data")
            val productsConnection = if (collectionHandle.isNullOrBlank()) {
                data?.optJSONObject("products")
            } else {
                data?.optJSONObject("collection")?.optJSONObject("products")
            } ?: return@withContext ShopifyStorefrontProductPage.EMPTY

            val pageInfo = productsConnection.optJSONObject("pageInfo")
            val nodes = productsConnection.optJSONArray("nodes") ?: JSONArray()
            val products = buildList {
                for (index in 0 until nodes.length()) {
                    val product = nodes.optJSONObject(index) ?: continue
                    add(ShopifyStorefrontProduct.fromJson(product))
                }
            }

            ShopifyStorefrontProductPage(
                products = products,
                hasNextPage = pageInfo?.optBoolean("hasNextPage") == true,
                endCursor = pageInfo?.optString("endCursor").takeUnless { it.isNullOrBlank() },
            )
        } finally {
            connection.disconnect()
        }
    }
}

private const val SHOPIFY_STOREFRONT_PRODUCT_FIELDS = """
id
title
description
handle
vendor
productType
tags
featuredImage {
  url
}
images(first: 10) {
  nodes {
    url
  }
}
variants(first: 100) {
  nodes {
    id
    title
    sku
    availableForSale
    price {
      amount
      currencyCode
    }
    selectedOptions {
      name
      value
    }
  }
}
"""

private val SHOPIFY_STOREFRONT_PRODUCTS_QUERY = """
query AppProducts(${ '$' }cursor: String) {
  products(first: 100, after: ${ '$' }cursor) {
    pageInfo {
      hasNextPage
      endCursor
    }
    nodes {
""" + SHOPIFY_STOREFRONT_PRODUCT_FIELDS + """
    }
  }
}
"""

private val SHOPIFY_STOREFRONT_COLLECTION_PRODUCTS_QUERY = """
query AppCollectionProducts(${ '$' }handle: String!, ${ '$' }cursor: String) {
  collection(handle: ${ '$' }handle) {
    products(first: 100, after: ${ '$' }cursor) {
      pageInfo {
        hasNextPage
        endCursor
      }
      nodes {
""" + SHOPIFY_STOREFRONT_PRODUCT_FIELDS + """
      }
    }
  }
}
"""

private data class ShopifyPublicCatalogConfig(
    val storeDomain: String,
    val storefrontAccessToken: String,
    val collectionHandle: String?,
)

private data class ShopifyStorefrontProductPage(
    val products: List<ShopifyStorefrontProduct>,
    val hasNextPage: Boolean,
    val endCursor: String?,
) {
    companion object {
        val EMPTY = ShopifyStorefrontProductPage(
            products = emptyList(),
            hasNextPage = false,
            endCursor = null,
        )
    }
}

private data class ShopifyStorefrontProduct(
    val id: String,
    val title: String,
    val description: String,
    val handle: String,
    val vendor: String?,
    val productType: String?,
    val tags: List<String>,
    val imageUrls: List<String>,
    val variants: List<ShopifyStorefrontVariant>,
) {
    fun toMerchandiseItem(): MerchandiseItem? {
        if (variants.isEmpty()) return null

        val mappedVariants = variants.map { variant ->
            MerchandiseVariant(
                id = variant.id,
                title = variant.title,
                size = variant.resolveOptionValue(setOf("size", "groesse", "größe")),
                color = variant.resolveOptionValue(setOf("color", "colour", "farbe")),
                shopifyVariantId = variant.id,
                sku = variant.sku,
                price = variant.price.amount.toDoubleOrNull() ?: 0.0,
                currency = variant.price.currencyCode,
                availableForSale = variant.availableForSale,
            )
        }
        val collabPartner = resolveShopifyCollabPartner(tags = tags, vendor = vendor)
        val category = resolveShopifyCategory(
            tags = tags,
            collabPartner = collabPartner,
            productType = productType,
        )

        return MerchandiseItem(
            id = "shopify_${id.substringAfterLast("/")}",
            name = title,
            price = mappedVariants.firstOrNull()?.price ?: 0.0,
            description = description,
            imageUrls = imageUrls.distinct(),
            available = mappedVariants.any { it.availableForSale },
            currency = mappedVariants.firstOrNull()?.currency ?: "EUR",
            sku = mappedVariants.firstOrNull()?.sku,
            shopifyProductId = id,
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
            category = category,
            collabPartner = collabPartner.orEmpty(),
        )
    }

    companion object {
        fun fromJson(json: JSONObject): ShopifyStorefrontProduct {
            val featuredImage = json.optJSONObject("featuredImage")?.optString("url").takeUnless { it.isNullOrBlank() }
            val imageNodes = json.optJSONObject("images")?.optJSONArray("nodes")
            val imageUrls = buildList {
                featuredImage?.let(::add)
                if (imageNodes != null) {
                    for (index in 0 until imageNodes.length()) {
                        imageNodes.optJSONObject(index)?.optString("url")
                            ?.takeUnless { it.isNullOrBlank() }
                            ?.let(::add)
                    }
                }
            }

            val variantsNodes = json.optJSONObject("variants")?.optJSONArray("nodes")
            val variants = buildList {
                if (variantsNodes != null) {
                    for (index in 0 until variantsNodes.length()) {
                        variantsNodes.optJSONObject(index)?.let { add(ShopifyStorefrontVariant.fromJson(it)) }
                    }
                }
            }

            return ShopifyStorefrontProduct(
                id = json.optString("id"),
                title = json.optString("title"),
                description = json.optString("description"),
                handle = json.optString("handle"),
                vendor = json.optString("vendor").takeIf { it.isNotBlank() },
                productType = json.optString("productType").takeIf { it.isNotBlank() },
                tags = json.optJSONArray("tags").toStringList(),
                imageUrls = imageUrls,
                variants = variants,
            )
        }
    }
}

private data class ShopifyStorefrontVariant(
    val id: String,
    val title: String,
    val sku: String?,
    val availableForSale: Boolean,
    val price: ShopifyStorefrontMoney,
    val selectedOptions: List<ShopifyStorefrontSelectedOption>,
) {
    fun resolveOptionValue(supportedKeys: Set<String>): String? {
        return selectedOptions.firstOrNull { option ->
            supportedKeys.contains(option.name.lowercase())
        }?.value?.trim()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        fun fromJson(json: JSONObject): ShopifyStorefrontVariant {
            val selectedOptionsArray = json.optJSONArray("selectedOptions")
            val selectedOptions = buildList {
                if (selectedOptionsArray != null) {
                    for (index in 0 until selectedOptionsArray.length()) {
                        selectedOptionsArray.optJSONObject(index)?.let {
                            add(
                                ShopifyStorefrontSelectedOption(
                                    name = it.optString("name"),
                                    value = it.optString("value"),
                                ),
                            )
                        }
                    }
                }
            }

            return ShopifyStorefrontVariant(
                id = json.optString("id"),
                title = json.optString("title"),
                sku = json.optString("sku").takeIf { it.isNotBlank() },
                availableForSale = json.optBoolean("availableForSale"),
                price = ShopifyStorefrontMoney.fromJson(json.optJSONObject("price") ?: JSONObject()),
                selectedOptions = selectedOptions,
            )
        }
    }
}

private data class ShopifyStorefrontMoney(
    val amount: String,
    val currencyCode: String,
) {
    companion object {
        fun fromJson(json: JSONObject): ShopifyStorefrontMoney {
            return ShopifyStorefrontMoney(
                amount = json.optString("amount"),
                currencyCode = json.optString("currencyCode").ifBlank { "EUR" },
            )
        }
    }
}

private data class ShopifyStorefrontSelectedOption(
    val name: String,
    val value: String,
)

private fun graphQlErrorMessage(errors: JSONArray?, fallback: String): String {
    if (errors == null || errors.length() == 0) {
        return fallback
    }

    val messages = buildList {
        for (index in 0 until errors.length()) {
            val message = errors.optJSONObject(index)?.optString("message").orEmpty().trim()
            if (message.isNotBlank()) {
                add(message)
            }
        }
    }

    return messages.joinToString(" | ").ifBlank { fallback }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
        }
    }
}

private fun resolveShopifyCollabPartner(tags: List<String>, vendor: String?): String? {
    return taggedMetadataValue(
        tags = tags,
        prefixes = listOf("collab:", "partner:", "artist:", "creator:"),
    ) ?: externalVendorName(vendor)
}

private fun resolveShopifyCategory(
    tags: List<String>,
    collabPartner: String?,
    productType: String?,
): String {
    return taggedMetadataValue(
        tags = tags,
        prefixes = listOf("category:", "collection:", "lane:"),
    ) ?: collabPartner
        ?: curatedProductType(productType)
        ?: "Sky22 Essentials"
}

private fun taggedMetadataValue(tags: List<String>, prefixes: List<String>): String? {
    for (tag in tags) {
        val trimmedTag = tag.trim()
        val loweredTag = trimmedTag.lowercase()
        for (prefix in prefixes) {
            val loweredPrefix = prefix.lowercase()
            if (!loweredTag.startsWith(loweredPrefix)) continue
            val value = trimmedTag.drop(prefix.length).trim()
            if (value.isNotEmpty()) {
                return value
            }
        }
    }

    return null
}

private fun externalVendorName(vendor: String?): String? {
    val trimmedVendor = vendor?.trim().orEmpty()
    if (trimmedVendor.isBlank()) return null

    val normalizedVendor = trimmedVendor.lowercase()
    val internalVendors = listOf(
        "skydown",
        "skydown x 22",
        "skydownx22",
        "sky22",
        "sky 22",
        "sky²²",
    )
    if (internalVendors.any { normalizedVendor.contains(it) }) {
        return null
    }

    return trimmedVendor
}

private fun curatedProductType(productType: String?): String? {
    val trimmedType = productType?.trim().orEmpty()
    if (trimmedType.isBlank()) return null

    val genericTypes = setOf("apparel", "clothing", "merch", "merchandise", "accessories", "accessory")
    return trimmedType.takeUnless { genericTypes.contains(it.lowercase()) }
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
