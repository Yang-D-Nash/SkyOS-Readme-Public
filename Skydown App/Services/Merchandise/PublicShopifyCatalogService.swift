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
            storefrontAccessToken: config.storefrontAccessToken,
            collectionHandle: requestedHandle
        )

        if products.isEmpty, requestedHandle != nil {
            products = try await fetchProducts(
                storeDomain: config.storeDomain,
                storefrontAccessToken: config.storefrontAccessToken,
                collectionHandle: nil
            )
        }

        return products
            .compactMap(mapToMerchandiseItem)
            .sorted {
                $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
            }
    }

    private func loadConfig() async throws -> ShopifyStorefrontCatalogConfig {
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

        return ShopifyStorefrontCatalogConfig(
            storeDomain: storeDomain,
            storefrontAccessToken: ((data["storefrontAccessToken"] as? String) ?? "").trimmed,
            collectionHandle: collectionHandle
        )
    }

    private func fetchProducts(
        storeDomain: String,
        storefrontAccessToken: String,
        collectionHandle: String?
    ) async throws -> [ShopifyStorefrontProduct] {
        var products: [ShopifyStorefrontProduct] = []
        var cursor: String?
        var hasNextPage = true

        while hasNextPage {
            let connection = try await fetchProductConnection(
                storeDomain: storeDomain,
                storefrontAccessToken: storefrontAccessToken,
                collectionHandle: collectionHandle,
                cursor: cursor
            )

            products.append(contentsOf: connection.nodes)
            hasNextPage = connection.pageInfo.hasNextPage
            cursor = connection.pageInfo.endCursor
        }

        return products
    }

    private func fetchProductConnection(
        storeDomain: String,
        storefrontAccessToken: String,
        collectionHandle: String?,
        cursor: String?
    ) async throws -> ShopifyStorefrontProductConnection {
        guard let url = URL(string: "https://\(storeDomain)/api/2026-01/graphql.json") else {
            throw URLError(.badURL)
        }

        let query = collectionHandle == nil
            ? shopifyStorefrontProductsQuery
            : shopifyStorefrontCollectionProductsQuery
        var variables: [String: Any] = ["cursor": cursor ?? NSNull()]
        if let collectionHandle {
            variables["handle"] = collectionHandle
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if storefrontAccessToken.isEmpty == false {
            request.setValue(storefrontAccessToken, forHTTPHeaderField: "X-Shopify-Storefront-Access-Token")
        }
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "query": query,
            "variables": variables
        ])

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        let decoder = JSONDecoder()
        let payload = try decoder.decode(ShopifyStorefrontEnvelope.self, from: data)

        guard (200...299).contains(httpResponse.statusCode) else {
            let message = payload.errors?.map(\.message).joined(separator: " | ")
                ?? "Shopify-Store antwortet mit \(httpResponse.statusCode)."
            throw NSError(
                domain: "PublicShopifyCatalogService",
                code: httpResponse.statusCode,
                userInfo: [NSLocalizedDescriptionKey: message]
            )
        }

        if let errors = payload.errors, errors.isEmpty == false {
            throw NSError(
                domain: "PublicShopifyCatalogService",
                code: 0,
                userInfo: [NSLocalizedDescriptionKey: errors.map(\.message).joined(separator: " | ")]
            )
        }

        if let connection = payload.data?.products {
            return connection
        }

        if let connection = payload.data?.collection?.products {
            return connection
        }

        return ShopifyStorefrontProductConnection(
            nodes: [],
            pageInfo: ShopifyStorefrontPageInfo(hasNextPage: false, endCursor: nil)
        )
    }

    private func mapToMerchandiseItem(_ product: ShopifyStorefrontProduct) -> MerchandiseItem? {
        let variants = product.variants.nodes.map { variant in
            MerchandiseVariant(
                id: variant.id,
                title: variant.title,
                size: selectedOptionValue(
                    optionNameCandidates: ["size", "groesse", "größe"],
                    selectedOptions: variant.selectedOptions
                ),
                color: selectedOptionValue(
                    optionNameCandidates: ["color", "colour", "farbe"],
                    selectedOptions: variant.selectedOptions
                ),
                shopifyVariantId: variant.id,
                sku: variant.sku?.trimmedNonEmpty,
                price: Double(variant.price.amount) ?? 0,
                currency: variant.price.currencyCode,
                availableForSale: variant.availableForSale
            )
        }

        guard variants.isEmpty == false else { return nil }

        let images = ([product.featuredImage?.url] + product.images.nodes.map { $0.url })
            .compactMap { $0?.trimmedNonEmpty }
            .removingDuplicates()
        let firstVariant = variants.first
        let isAvailable = variants.contains { $0.availableForSale }
        let collabPartner = resolvedShopifyCollabPartner(for: product)
        let category = resolvedShopifyCategory(for: product, collabPartner: collabPartner)

        return MerchandiseItem(
            id: "shopify_\(extractNumericId(from: product.id) ?? product.id)",
            name: product.title,
            price: firstVariant?.price ?? 0,
            description: product.description,
            imageURLs: images,
            available: isAvailable,
            currency: firstVariant?.currency ?? "EUR",
            sku: firstVariant?.sku,
            shopifyProductId: product.id,
            shopifyHandle: product.handle,
            availableForSale: isAvailable,
            shopifySyncActive: true,
            variants: variants,
            source: "shopify",
            isVisibleInApp: true,
            featured: false,
            sortOrder: 0,
            customBadge: "",
            customImageOverride: "",
            category: category,
            collabPartner: collabPartner ?? ""
        )
    }

    private func selectedOptionValue(
        optionNameCandidates: [String],
        selectedOptions: [ShopifyStorefrontSelectedOption]
    ) -> String? {
        selectedOptions.first { option in
            optionNameCandidates.contains(option.name.lowercased())
        }?.value.trimmedNonEmpty
    }

    private func resolvedShopifyCollabPartner(for product: ShopifyStorefrontProduct) -> String? {
        taggedMetadataValue(
            in: product.tags,
            prefixes: ["collab:", "partner:", "artist:", "creator:"]
        ) ?? externalVendorName(product.vendor)
    }

    private func resolvedShopifyCategory(
        for product: ShopifyStorefrontProduct,
        collabPartner: String?
    ) -> String {
        taggedMetadataValue(
            in: product.tags,
            prefixes: ["category:", "collection:", "lane:"]
        ) ?? collabPartner
        ?? curatedProductType(product.productType)
        ?? "Sky22 Essentials"
    }

    private func taggedMetadataValue(
        in tags: [String],
        prefixes: [String]
    ) -> String? {
        for tag in tags {
            let trimmedTag = tag.trimmed
            let loweredTag = trimmedTag.lowercased()
            for prefix in prefixes {
                let normalizedPrefix = prefix.lowercased()
                guard loweredTag.hasPrefix(normalizedPrefix) else { continue }
                let value = String(trimmedTag.dropFirst(prefix.count)).trimmed
                if !value.isEmpty {
                    return value
                }
            }
        }

        return nil
    }

    private func externalVendorName(_ vendor: String?) -> String? {
        guard let vendor = vendor?.trimmedNonEmpty else { return nil }
        let normalizedVendor = vendor.lowercased()
        let internalVendors = [
            "skydown",
            "skydown x 22",
            "skydownx22",
            "sky22",
            "sky 22",
            "sky²²"
        ]

        guard !internalVendors.contains(where: { normalizedVendor.contains($0) }) else {
            return nil
        }

        return vendor
    }

    private func curatedProductType(_ productType: String?) -> String? {
        guard let productType = productType?.trimmedNonEmpty else { return nil }
        let genericTypes = ["apparel", "clothing", "merch", "merchandise", "accessories", "accessory"]
        guard !genericTypes.contains(productType.lowercased()) else { return nil }
        return productType
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
            .trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
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

    private func extractNumericId(from gid: String) -> String? {
        gid.split(separator: "/").last.map(String.init)
    }
}

private let shopifyStorefrontProductFields = """
id
title
description
handle
vendor
productType
tags
featuredImage {
  url
}
images(first: 10) {
  nodes {
    url
  }
}
variants(first: 100) {
  nodes {
    id
    title
    sku
    availableForSale
    price {
      amount
      currencyCode
    }
    selectedOptions {
      name
      value
    }
  }
}
"""

private let shopifyStorefrontProductsQuery = """
query AppProducts($cursor: String) {
  products(first: 100, after: $cursor) {
    pageInfo {
      hasNextPage
      endCursor
    }
    nodes {
\(shopifyStorefrontProductFields)
    }
  }
}
"""

private let shopifyStorefrontCollectionProductsQuery = """
query AppCollectionProducts($handle: String!, $cursor: String) {
  collection(handle: $handle) {
    products(first: 100, after: $cursor) {
      pageInfo {
        hasNextPage
        endCursor
      }
      nodes {
\(shopifyStorefrontProductFields)
      }
    }
  }
}
"""

private struct ShopifyStorefrontCatalogConfig {
    let storeDomain: String
    let storefrontAccessToken: String
    let collectionHandle: String?
}

private struct ShopifyStorefrontEnvelope: Decodable {
    let data: ShopifyStorefrontCatalogData?
    let errors: [ShopifyStorefrontGraphQLError]?
}

private struct ShopifyStorefrontCatalogData: Decodable {
    let products: ShopifyStorefrontProductConnection?
    let collection: ShopifyStorefrontCollection?
}

private struct ShopifyStorefrontCollection: Decodable {
    let products: ShopifyStorefrontProductConnection?
}

private struct ShopifyStorefrontProductConnection: Decodable {
    let nodes: [ShopifyStorefrontProduct]
    let pageInfo: ShopifyStorefrontPageInfo
}

private struct ShopifyStorefrontPageInfo: Decodable {
    let hasNextPage: Bool
    let endCursor: String?
}

private struct ShopifyStorefrontProduct: Decodable {
    let id: String
    let title: String
    let description: String
    let handle: String
    let vendor: String?
    let productType: String?
    let tags: [String]
    let featuredImage: ShopifyStorefrontImage?
    let images: ShopifyStorefrontImageConnection
    let variants: ShopifyStorefrontVariantConnection
}

private struct ShopifyStorefrontImageConnection: Decodable {
    let nodes: [ShopifyStorefrontImage]
}

private struct ShopifyStorefrontImage: Decodable {
    let url: String?
}

private struct ShopifyStorefrontVariantConnection: Decodable {
    let nodes: [ShopifyStorefrontVariant]
}

private struct ShopifyStorefrontVariant: Decodable {
    let id: String
    let title: String
    let sku: String?
    let availableForSale: Bool
    let price: ShopifyStorefrontMoney
    let selectedOptions: [ShopifyStorefrontSelectedOption]
}

private struct ShopifyStorefrontMoney: Decodable {
    let amount: String
    let currencyCode: String
}

private struct ShopifyStorefrontSelectedOption: Decodable {
    let name: String
    let value: String
}

private struct ShopifyStorefrontGraphQLError: Decodable {
    let message: String
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
    }

    var trimmedNonEmpty: String? {
        let value = trimmed
        return value.isEmpty ? nil : value
    }
}

private extension Array where Element == String {
    func removingDuplicates() -> [String] {
        var seen = Set<String>()
        return filter { value in
            if seen.contains(value) {
                return false
            }
            seen.insert(value)
            return true
        }
    }
}
