package com.skydown.shared.usecase

import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.MerchandiseVariant

object MerchandiseVariantResolver {
    fun availableSizes(item: MerchandiseItem): List<String> {
        return item.variants
            .mapNotNull { it.size?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy { it.lowercase() }
    }

    fun availableColors(item: MerchandiseItem, size: String?): List<String> {
        val normalizedSize = size?.normalizedValue()
        return item.variants
            .filter { variant ->
                normalizedSize == null || variant.size.normalizedValue() == normalizedSize
            }
            .mapNotNull { it.color?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy { it.lowercase() }
    }

    fun resolveVariant(
        item: MerchandiseItem,
        size: String,
        color: String?,
    ): Result<MerchandiseVariant> {
        if (item.variants.isEmpty()) {
            return Result.failure(
                IllegalStateException("Fuer ${item.name} sind keine Shopify-Varianten vorhanden."),
            )
        }

        val normalizedSize = size.normalizedValue()
            ?: return Result.failure(IllegalArgumentException("Groesse ist erforderlich."))
        val normalizedColor = color.normalizedValue()

        val matches = item.variants.filter { variant ->
            variant.size.normalizedValue() == normalizedSize &&
                variant.color.normalizedValue() == normalizedColor
        }

        if (matches.size == 1) {
            return Result.success(matches.first())
        }

        if (matches.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Keine passende Variante fuer Groesse $size${color?.let { " / $it" }.orEmpty()} gefunden."),
            )
        }

        return Result.failure(
            IllegalStateException("Mehrere Varianten fuer Groesse $size${color?.let { " / $it" }.orEmpty()} gefunden."),
        )
    }
}

private fun String?.normalizedValue(): String? {
    return this
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.lowercase()
}
