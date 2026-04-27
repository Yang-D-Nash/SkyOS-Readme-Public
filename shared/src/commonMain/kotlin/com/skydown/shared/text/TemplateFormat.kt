package com.skydown.shared.text

private val indexedPlaceholderRegex = Regex("%(\\d+)\\$[sd]")

fun String.formatTemplate(vararg args: Any?): String {
    return indexedPlaceholderRegex.replace(this) { match ->
        val oneBasedIndex = match.toPlaceholderIndex() ?: return@replace match.value
        args.getOrNull(oneBasedIndex - 1).orEmptyString()
    }
}

private fun MatchResult.toPlaceholderIndex(): Int? = groupValues[1].toIntOrNull()

private fun Any?.orEmptyString(): String = this?.toString() ?: ""
