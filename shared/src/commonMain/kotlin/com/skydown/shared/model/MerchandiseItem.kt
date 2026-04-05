package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class MerchandiseVariant(
    val id: String = "",
    val title: String = "",
    val size: String? = null,
    val color: String? = null,
    val shopifyVariantId: String? = null,
    val sku: String? = null,
    val price: Double = 0.0,
    val currency: String = "EUR",
    val availableForSale: Boolean = true,
)

@Serializable
data class MerchandiseItem(
    val id: String? = null,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrls: List<String>,
    val available: Boolean,
    val currency: String = "EUR",
    val sku: String? = null,
    val shopifyProductId: String? = null,
    val shopifyHandle: String? = null,
    val availableForSale: Boolean = true,
    val shopifySyncActive: Boolean = true,
    val variants: List<MerchandiseVariant> = emptyList(),
    val source: String = "manual",
    val isVisibleInApp: Boolean = true,
    val featured: Boolean = false,
    val sortOrder: Int = 0,
    val customBadge: String = "",
    val customImageOverride: String = "",
    val category: String = "",
    val collabPartner: String = "",
    val shopifyCollectionHandles: List<String> = emptyList(),
)

val MerchandiseItem.hasCuratedMerchCategory: Boolean
    get() = category.trim().isNotEmpty() || collabPartner.trim().isNotEmpty()

val MerchandiseItem.merchCategoryTitle: String
    get() = if (!shopifyProductId.isNullOrBlank()) {
        category.trim().takeIf { it.isNotEmpty() }
            ?: collabPartner.trim().takeIf { it.isNotEmpty() }
            ?: "Sky22 Essentials"
    } else {
        collabPartner.trim().takeIf { it.isNotEmpty() }
            ?: category.trim().takeIf { it.isNotEmpty() }
            ?: "Sky22 Essentials"
    }

val MerchandiseItem.merchCategorySubtitle: String
    get() {
        val normalizedCollab = collabPartner.trim().takeIf { it.isNotEmpty() }
        val normalizedCategory = category.trim().takeIf { it.isNotEmpty() }

        return when {
            normalizedCollab != null &&
                normalizedCategory != null &&
                !normalizedCollab.equals(normalizedCategory, ignoreCase = true) -> normalizedCategory
            normalizedCollab != null -> "Collab lane"
            !shopifyProductId.isNullOrBlank() -> "Shopify drop"
            else -> "House line"
        }
    }

val MerchandiseItem.merchCategoryKey: String
    get() = merchCategoryTitle.slugifiedMerchCategory()

private fun String.slugifiedMerchCategory(): String {
    val normalized = lowercase()
        .replace(Regex("[^\\p{L}\\p{Nd}]+"), "-")
        .trim('-')
    return if (normalized.isBlank()) "all-drops" else normalized
}
