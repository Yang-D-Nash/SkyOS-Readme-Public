package com.skydown.android.data

import java.util.Locale

object AppLanguageSupport {
    val supportedLanguageCodes: Set<String> = setOf(
        "de",
        "en",
        "es",
        "fr",
        "it",
        "pt",
        "nl",
        "pl",
        "tr",
        "ja",
    )

    fun currentSystemLanguageDisplayName(locale: Locale = Locale.getDefault()): String {
        val code = normalizedLanguageCode(locale)
        val displayName = Locale.forLanguageTag(code).getDisplayLanguage(locale).ifBlank { code.uppercase(locale) }
        return displayName.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase(locale) else first.toString()
        }
    }

    fun normalizedLanguageCode(locale: Locale = Locale.getDefault()): String {
        val code = locale.language.lowercase(Locale.ROOT)
        return if (supportedLanguageCodes.contains(code)) code else "en"
    }
}
