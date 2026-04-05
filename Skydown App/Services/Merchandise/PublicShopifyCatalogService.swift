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
        let requestedHandles = config.collectionHandles.map { $0.lowercased() }

        let items = try await fetchItems(
            storeDomain: config.storeDomain,
            storefrontAccessToken: config.storefrontAccessToken,
            collectionHandles: requestedHandles
        )

        if items.isEmpty, requestedHandles.isEmpty == false {
            return try await fetchItems(
                storeDomain: config.storeDomain,
                storefrontAccessToken: config.storefrontAccessToken,
                collectionHandles: []
            )
        }

        return items.sorted {
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
        let collectionHandles = normalizeCollectionHandles(
            rawValue: data["collectionHandles"],
            legacyValue: data["collectionHandle"] as? String,
            fallbackURL: storefrontURL
        )

        return ShopifyStorefrontCatalogConfig(
            storeDomain: storeDomain,
            storefrontAccessToken: ((data["storefrontAccessToken"] as? String) ?? "").trimmed,
            collectionHandles: collectionHandles
        )
    }

    private func fetchItems(
        storeDomain: String,
        storefrontAccessToken: String,
        collectionHandles: [String]
    ) async throws -> [MerchandiseItem] {
        if collectionHandles.isEmpty {
            return try await fetchProducts(
                storeDomain: storeDomain,
                storefrontAccessToken: storefrontAccessToken,
                collectionHandle: nil
            ).compactMap { mapToMerchandiseItem($0, fallbackCollectionHandle: nil) }
        }

        var mergedItems: [MerchandiseItem] = []
        var seenProductIDs = Set<String>()

        for handle in collectionHandles {
            let products = try await fetchProducts(
                storeDomain: storeDomain,
                storefrontAccessToken: storefrontAccessToken,
                collectionHandle: handle
            )

            for product in products where seenProductIDs.insert(product.id).inserted {
                if let item = mapToMerchandiseItem(product, fallbackCollectionHandle: handle) {
                    mergedItems.append(item)
                }
            }
        }

        return mergedItems
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

    private func mapToMerchandiseItem(
        _ product: ShopifyStorefrontProduct,
        fallbackCollectionHandle: String?
    ) -> MerchandiseItem? {
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
        let category = resolvedShopifyCategory(
            for: product,
            collabPartner: collabPartner,
            fallbackCollectionHandle: fallbackCollectionHandle
        )
        let shopifyCollectionHandles = resolvedCollectionHandles(
            for: product,
            fallbackCollectionHandle: fallbackCollectionHandle
        )

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
            collabPartner: collabPartner ?? "",
            shopifyCollectionHandles: shopifyCollectionHandles
        )
    }

    private func resolvedCollectionHandles(
        for product: ShopifyStorefrontProduct,
        fallbackCollectionHandle: String?
    ) -> [String] {
        let productHandles = product.collections.nodes
            .compactMap { $0.handle?.trimmedNonEmpty?.lowercased() }
        let fallback = fallbackCollectionHandle?.trimmedNonEmpty?.lowercased()
        let combined = productHandles + [fallback].compactMap { $0 }
        return Array(NSOrderedSet(array: combined)) as? [String] ?? combined
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
        collabPartner: String?,
        fallbackCollectionHandle: String?
    ) -> String {
        taggedMetadataValue(
            in: product.tags,
            prefixes: ["category:", "collection:", "lane:"]
        ) ?? collabPartner
        ?? prettifiedCollectionHandle(fallbackCollectionHandle)
        ?? curatedProductType(product.productType)
        ?? "Sky22 Essentials"
    }

    private func prettifiedCollectionHandle(_ handle: String?) -> String? {
        guard let handle = handle?.trimmedNonEmpty else { return nil }
        return handle
            .split(separator: "-")
            .map { segment in
                let value = String(segment)
                return value.prefix(1).uppercased() + value.dropFirst()
            }
            .joined(separator: " ")
            .trimmedNonEmpty
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
            "sky²²",
            "podpartner",
            "printful",
            "printify",
            "gelato"
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

    private func normalizeCollectionHandle(_ value: String?) -> String? {
        if let direct = value?.trimmedNonEmpty {
            return direct
                .replacingOccurrences(of: "/collections/", with: "")
                .split(separator: "/")
                .first
                .map(String.init)?
                .trimmedNonEmpty
        }

        return nil
    }

    private func normalizeCollectionHandles(
        rawValue: Any?,
        legacyValue: String?,
        fallbackURL: String?
    ) -> [String] {
        let candidates: [String]

        if let values = rawValue as? [String], values.isEmpty == false {
            candidates = values
        } else if let rawString = rawValue as? String, rawString.trimmedNonEmpty != nil {
            candidates = rawString
                .split(whereSeparator: \.isNewline)
                .flatMap { $0.split(separator: ",") }
                .map(String.init)
        } else if let legacyValue, legacyValue.trimmedNonEmpty != nil {
            candidates = legacyValue
                .split(whereSeparator: \.isNewline)
                .flatMap { $0.split(separator: ",") }
                .map(String.init)
        } else {
            candidates = []
        }

        let normalized = candidates.compactMap(normalizeCollectionHandle)
        if normalized.isEmpty == false {
            return Array(NSOrderedSet(array: normalized)) as? [String] ?? normalized
        }

        guard
            let fallbackURL,
            let url = URL(string: fallbackURL)
        else {
            return []
        }

        let components = url.path
            .split(separator: "/")
            .map(String.init)
        guard
            let index = components.firstIndex(of: "collections"),
            components.indices.contains(index + 1)
        else {
            return []
        }

        return [components[index + 1]].compactMap(normalizeCollectionHandle)
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
collections(first: 20) {
  nodes {
    handle
    title
  }
}
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
    let collectionHandles: [String]
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
    let collections: ShopifyStorefrontCollectionConnection
    let featuredImage: ShopifyStorefrontImage?
    let images: ShopifyStorefrontImageConnection
    let variants: ShopifyStorefrontVariantConnection
}

private struct ShopifyStorefrontCollectionConnection: Decodable {
    let nodes: [ShopifyStorefrontCollectionNode]
}

private struct ShopifyStorefrontCollectionNode: Decodable {
    let handle: String?
    let title: String?
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
