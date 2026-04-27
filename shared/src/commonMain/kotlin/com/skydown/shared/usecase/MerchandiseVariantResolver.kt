package com.skydown.shared.usecase

import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.MerchandiseVariant
import com.skydown.shared.text.SharedText
import com.skydown.shared.text.formatTemplate

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
                IllegalStateException(SharedText.MERCH_VARIANT_NONE_AVAILABLE.formatTemplate(item.name)),
            )
        }

        val normalizedSize = size.normalizedValue()
            ?: return Result.failure(IllegalArgumentException(SharedText.MERCH_VARIANT_SIZE_REQUIRED))
        val normalizedColor = color.normalizedValue()

        val matches = item.variants.filter { variant ->
            variant.size.normalizedValue() == normalizedSize &&
                variant.color.normalizedValue() == normalizedColor
        }

        if (matches.size == 1) {
            return Result.success(matches.first())
        }

        if (matches.isEmpty()) {
            val colorPart = color?.let { " / $it" }.orEmpty()
            return Result.failure(
                IllegalArgumentException(SharedText.MERCH_VARIANT_NOT_FOUND.formatTemplate(size, colorPart)),
            )
        }

        val colorPart = color?.let { " / $it" }.orEmpty()
        return Result.failure(
            IllegalStateException(SharedText.MERCH_VARIANT_MULTIPLE_FOUND.formatTemplate(size, colorPart)),
        )
    }
}

private fun String?.normalizedValue(): String? {
    return this
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.lowercase()
}
