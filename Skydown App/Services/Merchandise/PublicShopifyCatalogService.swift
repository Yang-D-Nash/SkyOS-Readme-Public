import Foundation
import FirebaseFirestore

protocol PublicShopifyCatalogServicing {
    func fetchCatalog() async throws -> [MerchandiseItem]
}

struct PublicShopifyCatalogService: PublicShopifyCatalogServicing {
    private let firestore: Firestore
    private let session: URLSession

    init(
        firestore: Firestore = Firestore.firestore(),
        session: URLSession = .shared
    ) {
        self.firestore = firestore
        self.session = session
    }

    func fetchCatalog() async throws -> [MerchandiseItem] {
        let config = try await loadConfig()
        let requestedHandle = config.collectionHandle?.lowercased()

        var products = try await fetchProducts(
            storeDomain: config.storeDomain,
            collectionHandle: requestedHandle
        )

        if products.isEmpty, requestedHandle != nil {
            products = try await fetchProducts(
                storeDomain: config.storeDomain,
                collectionHandle: nil
            )
        }

        return products
            .compactMap { product in
                mapToMerchandiseItem(product)
            }
            .sorted {
                $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
            }
    }

    private func loadConfig() async throws -> ShopifyPublicCatalogConfig {
        let snapshot = try await firestore.collection("appConfig").document("shopifyMerch").getDocument()
        let data = snapshot.data() ?? [:]
        let storefrontURL = normalizeURL(data["storefrontURL"] as? String)
        let storeDomain = normalizeStoreDomain(data["storeDomain"] as? String)
            ?? normalizeStoreDomain(storefrontURL)
            ?? "k5t1sc-ps.myshopify.com"
        let collectionHandle = normalizeCollectionHandle(
            data["collectionHandle"] as? String,
            fallbackURL: storefrontURL
        )

        return ShopifyPublicCatalogConfig(
            storeDomain: storeDomain,
            collectionHandle: collectionHandle
        )
    }

    private func fetchProducts(
        storeDomain: String,
        collectionHandle: String?
    ) async throws -> [ShopifyPublicProduct] {
        let path = collectionHandle.flatMap { handle in
            handle.isEmpty ? nil : "/collections/\(handle)/products.json"
        } ?? "/products.json"
        guard let url = URL(string: "https://\(storeDomain)\(path)?limit=250") else {
            throw URLError(.badURL)
        }

        let (data, response) = try await session.data(from: url)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            throw NSError(
                domain: "PublicShopifyCatalogService",
                code: httpResponse.statusCode,
                userInfo: [
                    NSLocalizedDescriptionKey: "Shopify-Store antwortet mit \(httpResponse.statusCode)."
                ]
            )
        }

        let decoder = JSONDecoder()
        let payload = try decoder.decode(ShopifyPublicCatalogResponse.self, from: data)
        return payload.products
    }

    private func mapToMerchandiseItem(_ product: ShopifyPublicProduct) -> MerchandiseItem? {
        let variants = product.variants.map { variant in
            MerchandiseVariant(
                id: "gid://shopify/ProductVariant/\(variant.id)",
                title: variant.title,
                size: selectedOptionValue(
                    optionNameCandidates: ["size", "groesse", "größe"],
                    variant: variant,
                    optionDefinitions: product.options
                ),
                color: selectedOptionValue(
                    optionNameCandidates: ["color", "colour", "farbe"],
                    variant: variant,
                    optionDefinitions: product.options
                ),
                shopifyVariantId: "gid://shopify/ProductVariant/\(variant.id)",
                sku: variant.sku?.trimmedNonEmpty,
                price: Double(variant.price) ?? 0,
                currency: "EUR",
                availableForSale: variant.isAvailable
            )
        }

        guard !variants.isEmpty else { return nil }

        let images = ([product.image?.src] + product.images.map(\.src))
            .compactMap { $0?.trimmedNonEmpty }
            .removingDuplicates()
        let firstVariant = variants.first
        let isAvailable = variants.contains(where: \.availableForSale)

        return MerchandiseItem(
            id: "shopify_\(product.id)",
            name: product.title,
            price: firstVariant?.price ?? 0,
            description: product.bodyHTML?.strippingHTML() ?? "",
            imageURLs: images,
            available: isAvailable,
            currency: "EUR",
            sku: firstVariant?.sku,
            shopifyProductId: "gid://shopify/Product/\(product.id)",
            shopifyHandle: product.handle,
            availableForSale: isAvailable,
            shopifySyncActive: true,
            variants: variants,
            source: "shopify",
            isVisibleInApp: true,
            featured: false,
            sortOrder: 0,
            customBadge: "",
            customImageOverride: ""
        )
    }

    private func selectedOptionValue(
        optionNameCandidates: [String],
        variant: ShopifyPublicVariant,
        optionDefinitions: [ShopifyPublicOption]
    ) -> String? {
        let optionValues = [variant.option1, variant.option2, variant.option3]
        guard !optionDefinitions.isEmpty else { return nil }

        for (index, option) in optionDefinitions.enumerated() where index < optionValues.count {
            let normalizedName = option.name.lowercased()
            if optionNameCandidates.contains(normalizedName) {
                return optionValues[index]?.trimmedNonEmpty
            }
        }

        return nil
    }

    private func normalizeStoreDomain(_ value: String?) -> String? {
        guard let trimmed = value?.trimmedNonEmpty else { return nil }
        return trimmed
            .replacingOccurrences(of: "https://", with: "")
            .replacingOccurrences(of: "http://", with: "")
            .split(separator: "/")
            .first
            .map(String.init)?
            .lowercased()
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func normalizeURL(_ value: String?) -> String? {
        guard let trimmed = value?.trimmedNonEmpty else { return nil }
        if trimmed.hasPrefix("http://") || trimmed.hasPrefix("https://") {
            return trimmed
        }
        return "https://\(trimmed)"
    }

    private func normalizeCollectionHandle(_ value: String?, fallbackURL: String?) -> String? {
        if let direct = value?.trimmedNonEmpty {
            return direct
                .replacingOccurrences(of: "/collections/", with: "")
                .split(separator: "/")
                .first
                .map(String.init)?
                .trimmedNonEmpty
        }

        guard
            let fallbackURL,
            let url = URL(string: fallbackURL)
        else {
            return nil
        }

        let components = url.path
            .split(separator: "/")
            .map(String.init)
        guard
            let index = components.firstIndex(of: "collections"),
            components.indices.contains(index + 1)
        else {
            return nil
        }

        return components[index + 1].trimmedNonEmpty
    }
}

private struct ShopifyPublicCatalogConfig {
    let storeDomain: String
    let collectionHandle: String?
}

private struct ShopifyPublicCatalogResponse: Decodable {
    let products: [ShopifyPublicProduct]
}

private struct ShopifyPublicProduct: Decodable {
    let id: Int64
    let title: String
    let bodyHTML: String?
    let handle: String
    let image: ShopifyPublicImage?
    let images: [ShopifyPublicImage]
    let variants: [ShopifyPublicVariant]
    let options: [ShopifyPublicOption]

    private enum CodingKeys: String, CodingKey {
        case id
        case title
        case bodyHTML = "body_html"
        case handle
        case image
        case images
        case variants
        case options
    }
}

private struct ShopifyPublicImage: Decodable {
    let src: String?
}

private struct ShopifyPublicOption: Decodable {
    let name: String
}

private struct ShopifyPublicVariant: Decodable {
    let id: Int64
    let title: String
    let price: String
    let sku: String?
    let available: Bool?
    let inventoryQuantity: Int?
    let option1: String?
    let option2: String?
    let option3: String?

    private enum CodingKeys: String, CodingKey {
        case id
        case title
        case price
        case sku
        case available
        case inventoryQuantity = "inventory_quantity"
        case option1
        case option2
        case option3
    }

    var isAvailable: Bool {
        if let available {
            return available
        }
        if let inventoryQuantity {
            return inventoryQuantity > 0
        }
        return true
    }
}

private extension String {
    var trimmedNonEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }

    func strippingHTML() -> String {
        replacingOccurrences(of: "<[^>]+>", with: " ", options: .regularExpression)
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

private extension Array where Element == String {
    func removingDuplicates() -> [String] {
        var seen = Set<String>()
        return filter { seen.insert($0).inserted }
    }
}
