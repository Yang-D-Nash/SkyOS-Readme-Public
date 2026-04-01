import Foundation

enum MerchandiseVariantResolver {
    static func availableSizes(for item: MerchandiseItem) -> [String] {
        deduplicated(
            item.variants.compactMap { $0.size?.trimmedNonEmpty }
        )
    }

    static func availableColors(for item: MerchandiseItem, size: String?) -> [String] {
        let normalizedSize = size?.normalizedVariantValue
        return deduplicated(
            item.variants.compactMap { variant in
                guard normalizedSize == nil || variant.size?.normalizedVariantValue == normalizedSize else {
                    return nil
                }
                return variant.color?.trimmedNonEmpty
            }
        )
    }

    static func resolveVariant(
        for item: MerchandiseItem,
        size: String,
        color: String?
    ) throws -> MerchandiseVariant {
        guard !item.variants.isEmpty else {
            throw NSError(
                domain: "MerchandiseVariantResolver",
                code: 404,
                userInfo: [NSLocalizedDescriptionKey: "Für \(item.name) sind keine Shopify-Varianten hinterlegt."]
            )
        }

        guard let normalizedSize = size.normalizedVariantValue else {
            throw NSError(
                domain: "MerchandiseVariantResolver",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Bitte wähle eine Größe."]
            )
        }

        let normalizedColor = color?.normalizedVariantValue
        let matches = item.variants.filter { variant in
            variant.size?.normalizedVariantValue == normalizedSize &&
            variant.color?.normalizedVariantValue == normalizedColor
        }

        if let exactMatch = matches.only {
            return exactMatch
        }

        if matches.isEmpty {
            let detail = color?.trimmedNonEmpty.map { " / \($0)" } ?? ""
            throw NSError(
                domain: "MerchandiseVariantResolver",
                code: 404,
                userInfo: [NSLocalizedDescriptionKey: "Keine passende Variante für \(size)\(detail) gefunden."]
            )
        }

        throw NSError(
            domain: "MerchandiseVariantResolver",
            code: 409,
            userInfo: [NSLocalizedDescriptionKey: "Mehrere Varianten für diese Auswahl gefunden."]
        )
    }

    private static func deduplicated(_ values: [String]) -> [String] {
        var seen: Set<String> = []
        return values.filter {
            let normalized = $0.lowercased()
            return seen.insert(normalized).inserted
        }
    }
}

private extension String {
    var trimmedNonEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    var normalizedVariantValue: String? {
        trimmedNonEmpty?.lowercased()
    }
}

private extension Array {
    var only: Element? {
        count == 1 ? first : nil
    }
}
