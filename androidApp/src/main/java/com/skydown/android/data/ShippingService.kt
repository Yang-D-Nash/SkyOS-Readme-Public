package com.skydown.android.data

import com.skydown.shared.model.CartItem

enum class ShippingZone {
    DE,
    EU,
    INTL,
}

data class ShippingQuote(
    val zone: ShippingZone,
    val countryCode: String,
    val price: Double,
    val freeShippingApplied: Boolean,
)

object ShippingService {
    private val euCountryCodes = setOf(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU",
        "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE",
    )

    private val aliases = mapOf(
        "de" to "DE",
        "deutschland" to "DE",
        "germany" to "DE",
        "at" to "AT",
        "österreich" to "AT",
        "oesterreich" to "AT",
        "austria" to "AT",
        "ch" to "CH",
        "schweiz" to "CH",
        "switzerland" to "CH",
        "fr" to "FR",
        "frankreich" to "FR",
        "france" to "FR",
        "es" to "ES",
        "spanien" to "ES",
        "spain" to "ES",
        "it" to "IT",
        "italien" to "IT",
        "italy" to "IT",
        "nl" to "NL",
        "niederlande" to "NL",
        "netherlands" to "NL",
        "holland" to "NL",
        "be" to "BE",
        "belgien" to "BE",
        "belgium" to "BE",
        "lu" to "LU",
        "luxemburg" to "LU",
        "luxembourg" to "LU",
        "pt" to "PT",
        "portugal" to "PT",
        "ie" to "IE",
        "irland" to "IE",
        "ireland" to "IE",
        "pl" to "PL",
        "polen" to "PL",
        "poland" to "PL",
        "cz" to "CZ",
        "tschechien" to "CZ",
        "czech republic" to "CZ",
        "dk" to "DK",
        "dänemark" to "DK",
        "daenemark" to "DK",
        "denmark" to "DK",
        "se" to "SE",
        "schweden" to "SE",
        "sweden" to "SE",
        "fi" to "FI",
        "finnland" to "FI",
        "finland" to "FI",
        "hu" to "HU",
        "ungarn" to "HU",
        "hungary" to "HU",
        "ro" to "RO",
        "rumänien" to "RO",
        "rumaenien" to "RO",
        "romania" to "RO",
        "bg" to "BG",
        "bulgarien" to "BG",
        "bulgaria" to "BG",
        "hr" to "HR",
        "kroatien" to "HR",
        "croatia" to "HR",
        "si" to "SI",
        "slowenien" to "SI",
        "slovenia" to "SI",
        "sk" to "SK",
        "slowakei" to "SK",
        "slovakia" to "SK",
        "gr" to "GR",
        "griechenland" to "GR",
        "greece" to "GR",
        "ee" to "EE",
        "estland" to "EE",
        "estonia" to "EE",
        "lv" to "LV",
        "lettland" to "LV",
        "latvia" to "LV",
        "lt" to "LT",
        "litauen" to "LT",
        "lithuania" to "LT",
        "cy" to "CY",
        "zypern" to "CY",
        "cyprus" to "CY",
        "mt" to "MT",
        "malta" to "MT",
        "us" to "US",
        "usa" to "US",
        "united states" to "US",
        "vereinigte staaten" to "US",
        "gb" to "GB",
        "uk" to "GB",
        "united kingdom" to "GB",
        "grossbritannien" to "GB",
    )

    fun resolveCountryCode(input: String): Result<String> {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Bitte gib ein Lieferland an."))
        }

        if (trimmed.length == 2 && trimmed.all { it.isLetter() }) {
            return Result.success(trimmed.uppercase())
        }

        return aliases[trimmed.lowercase()]
            ?.let { Result.success(it) }
            ?: Result.failure(
                IllegalArgumentException("Das Lieferland $trimmed konnte nicht erkannt werden."),
            )
    }

    fun resolveShippingZone(countryCode: String): Result<ShippingZone> {
        val normalized = countryCode.trim().uppercase()
        if (normalized.isBlank()) {
            return Result.failure(IllegalArgumentException("Der Laendercode fehlt."))
        }

        return Result.success(
            when {
                normalized == "DE" -> ShippingZone.DE
                euCountryCodes.contains(normalized) -> ShippingZone.EU
                else -> ShippingZone.INTL
            },
        )
    }

    fun calculateShippingPrice(
        settings: CommerceShippingSettings,
        countryCode: String,
        items: List<CartItem>,
        subtotal: Double,
    ): Result<ShippingQuote> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Es sind keine Artikel fuer den Versand ausgewaehlt."))
        }

        val zone = resolveShippingZone(countryCode).getOrElse { return Result.failure(it) }
        val baseRate = when (zone) {
            ShippingZone.DE -> settings.domesticCost
            ShippingZone.EU -> settings.euCost
            ShippingZone.INTL -> settings.internationalCost
        }
        val zeroSubtotalApplied = subtotal <= 0.01
        val freeShippingApplied = zeroSubtotalApplied ||
            (settings.freeShippingThreshold > 0 && subtotal >= settings.freeShippingThreshold)

        return Result.success(
            ShippingQuote(
                zone = zone,
                countryCode = countryCode,
                price = if (freeShippingApplied) 0.0 else baseRate.coerceAtLeast(0.0),
                freeShippingApplied = freeShippingApplied,
            ),
        )
    }
}
