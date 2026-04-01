import Foundation

enum ShippingZone: String, Codable, CaseIterable {
    case germany = "DE"
    case europe = "EU"
    case international = "INTL"
}

struct ShippingQuote: Equatable {
    let zone: ShippingZone
    let countryCode: String
    let price: Double
    let freeShippingApplied: Bool
}

enum ShippingService {
    private static let euCountryCodes: Set<String> = [
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU",
        "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    ]

    private static let countryAliases: [String: String] = [
        "de": "DE",
        "deutschland": "DE",
        "germany": "DE",
        "at": "AT",
        "oesterreich": "AT",
        "österreich": "AT",
        "austria": "AT",
        "ch": "CH",
        "schweiz": "CH",
        "switzerland": "CH",
        "fr": "FR",
        "frankreich": "FR",
        "france": "FR",
        "es": "ES",
        "spanien": "ES",
        "spain": "ES",
        "it": "IT",
        "italien": "IT",
        "italy": "IT",
        "nl": "NL",
        "niederlande": "NL",
        "netherlands": "NL",
        "holland": "NL",
        "be": "BE",
        "belgien": "BE",
        "belgium": "BE",
        "lu": "LU",
        "luxemburg": "LU",
        "luxembourg": "LU",
        "pt": "PT",
        "portugal": "PT",
        "ie": "IE",
        "irland": "IE",
        "ireland": "IE",
        "pl": "PL",
        "polen": "PL",
        "poland": "PL",
        "cz": "CZ",
        "tschechien": "CZ",
        "czech republic": "CZ",
        "dk": "DK",
        "daenemark": "DK",
        "dänemark": "DK",
        "denmark": "DK",
        "se": "SE",
        "schweden": "SE",
        "sweden": "SE",
        "fi": "FI",
        "finnland": "FI",
        "finland": "FI",
        "hu": "HU",
        "ungarn": "HU",
        "hungary": "HU",
        "ro": "RO",
        "rumaenien": "RO",
        "rumänien": "RO",
        "romania": "RO",
        "bg": "BG",
        "bulgarien": "BG",
        "bulgaria": "BG",
        "hr": "HR",
        "kroatien": "HR",
        "croatia": "HR",
        "si": "SI",
        "slowenien": "SI",
        "slovenia": "SI",
        "sk": "SK",
        "slowakei": "SK",
        "slovakia": "SK",
        "gr": "GR",
        "griechenland": "GR",
        "greece": "GR",
        "ee": "EE",
        "estland": "EE",
        "estonia": "EE",
        "lv": "LV",
        "lettland": "LV",
        "latvia": "LV",
        "lt": "LT",
        "litauen": "LT",
        "lithuania": "LT",
        "cy": "CY",
        "zypern": "CY",
        "cyprus": "CY",
        "mt": "MT",
        "malta": "MT",
        "us": "US",
        "usa": "US",
        "united states": "US",
        "vereinigte staaten": "US",
        "gb": "GB",
        "uk": "GB",
        "united kingdom": "GB",
        "grossbritannien": "GB"
    ]

    static func resolveCountryCode(from input: String) throws -> String {
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            throw shippingError("Bitte gib ein Lieferland an.")
        }

        if trimmed.count == 2, trimmed.range(of: "^[A-Za-z]{2}$", options: .regularExpression) != nil {
            return trimmed.uppercased()
        }

        let normalized = trimmed.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .lowercased()

        if let alias = countryAliases[normalized] {
            return alias
        }

        throw shippingError("Das Lieferland \(trimmed) konnte nicht erkannt werden.")
    }

    static func resolveShippingZone(countryCode: String) throws -> ShippingZone {
        let normalized = countryCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !normalized.isEmpty else {
            throw shippingError("Der Ländercode fehlt.")
        }

        if normalized == "DE" {
            return .germany
        }

        if euCountryCodes.contains(normalized) {
            return .europe
        }

        return .international
    }

    static func calculateShippingPrice(
        settings: CommerceShippingSettings,
        countryCode: String,
        items: [CartItem],
        subtotal: Double
    ) throws -> ShippingQuote {
        guard !items.isEmpty else {
            throw shippingError("Es sind keine Artikel für den Versand ausgewählt.")
        }

        let zone = try resolveShippingZone(countryCode: countryCode)
        let baseRate: Double
        switch zone {
        case .germany:
            baseRate = settings.domesticCost
        case .europe:
            baseRate = settings.euCost
        case .international:
            baseRate = settings.internationalCost
        }

        let freeShippingApplied = settings.freeShippingThreshold > 0 && subtotal >= settings.freeShippingThreshold
        return ShippingQuote(
            zone: zone,
            countryCode: countryCode,
            price: freeShippingApplied ? 0 : max(baseRate, 0),
            freeShippingApplied: freeShippingApplied
        )
    }

    private static func shippingError(_ message: String) -> NSError {
        NSError(
            domain: "ShippingService",
            code: 400,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }
}
