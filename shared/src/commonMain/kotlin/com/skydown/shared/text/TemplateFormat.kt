package com.skydown.shared.text

private val indexedPlaceholderRegex = Regex("%(\\d+)\\$[sd]")

fun String.formatTemplate(vararg args: Any?): String {
    return indexedPlaceholderRegex.replace(this) { match ->
        val oneBasedIndex = match.groupValues[1].toIntOrNull() ?: return@replace match.value
        args.getOrNull(oneBasedIndex - 1)?.toString() ?: ""
    }
}
