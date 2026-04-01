//
//  MerchandiseItem.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import Foundation
import FirebaseFirestore

struct MerchandiseVariant: Codable, Hashable, Identifiable {
    var id: String = UUID().uuidString
    var title: String = ""
    var size: String?
    var color: String?
    var shopifyVariantId: String?
    var sku: String?
    var price: Double = 0
    var currency: String = "EUR"
    var availableForSale: Bool = true
}

struct MerchandiseItem: Codable, Identifiable {
    @DocumentID var id: String?
    var name: String
    var price: Double
    var description: String
    var imageURLs: [String]
    var available: Bool
    var currency: String = "EUR"
    var sku: String?
    var shopifyProductId: String?
    var shopifyHandle: String?
    var availableForSale: Bool = true
    var shopifySyncActive: Bool = true
    var variants: [MerchandiseVariant] = []
    var source: String = "manual"
    var isVisibleInApp: Bool = true
    var featured: Bool = false
    var sortOrder: Int = 0
    var customBadge: String = ""
    var customImageOverride: String = ""

    enum CodingKeys: String, CodingKey {
        case name
        case price
        case description
        case imageURLs
        case imageUrls
        case available
        case currency
        case sku
        case shopifyProductId
        case shopifyHandle
        case availableForSale
        case shopifySyncActive
        case variants
        case source
        case isVisibleInApp
        case featured
        case sortOrder
        case customBadge
        case customImageOverride
    }

    init(
        id: String? = nil,
        name: String,
        price: Double,
        description: String,
        imageURLs: [String],
        available: Bool,
        currency: String = "EUR",
        sku: String? = nil,
        shopifyProductId: String? = nil,
        shopifyHandle: String? = nil,
        availableForSale: Bool = true,
        shopifySyncActive: Bool = true,
        variants: [MerchandiseVariant] = [],
        source: String = "manual",
        isVisibleInApp: Bool = true,
        featured: Bool = false,
        sortOrder: Int = 0,
        customBadge: String = "",
        customImageOverride: String = ""
    ) {
        self.id = id
        self.name = name
        self.price = price
        self.description = description
        self.imageURLs = imageURLs
        self.available = available
        self.currency = currency
        self.sku = sku
        self.shopifyProductId = shopifyProductId
        self.shopifyHandle = shopifyHandle
        self.availableForSale = availableForSale
        self.shopifySyncActive = shopifySyncActive
        self.variants = variants
        self.source = source
        self.isVisibleInApp = isVisibleInApp
        self.featured = featured
        self.sortOrder = sortOrder
        self.customBadge = customBadge
        self.customImageOverride = customImageOverride
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        name = try container.decode(String.self, forKey: .name)
        price = try container.decode(Double.self, forKey: .price)
        description = try container.decodeIfPresent(String.self, forKey: .description) ?? ""
        imageURLs = try container.decodeIfPresent([String].self, forKey: .imageURLs)
            ?? container.decodeIfPresent([String].self, forKey: .imageUrls)
            ?? []
        available = try container.decodeIfPresent(Bool.self, forKey: .available) ?? true
        currency = try container.decodeIfPresent(String.self, forKey: .currency) ?? "EUR"
        sku = try container.decodeIfPresent(String.self, forKey: .sku)
        shopifyProductId = try container.decodeIfPresent(String.self, forKey: .shopifyProductId)
        shopifyHandle = try container.decodeIfPresent(String.self, forKey: .shopifyHandle)
        availableForSale = try container.decodeIfPresent(Bool.self, forKey: .availableForSale) ?? available
        shopifySyncActive = try container.decodeIfPresent(Bool.self, forKey: .shopifySyncActive) ?? true
        variants = try container.decodeIfPresent([MerchandiseVariant].self, forKey: .variants) ?? []
        source = try container.decodeIfPresent(String.self, forKey: .source) ?? "manual"
        isVisibleInApp = try container.decodeIfPresent(Bool.self, forKey: .isVisibleInApp) ?? true
        featured = try container.decodeIfPresent(Bool.self, forKey: .featured) ?? false
        sortOrder = try container.decodeIfPresent(Int.self, forKey: .sortOrder) ?? 0
        customBadge = try container.decodeIfPresent(String.self, forKey: .customBadge) ?? ""
        customImageOverride = try container.decodeIfPresent(String.self, forKey: .customImageOverride) ?? ""
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(name, forKey: .name)
        try container.encode(price, forKey: .price)
        try container.encode(description, forKey: .description)
        try container.encode(imageURLs, forKey: .imageURLs)
        try container.encode(imageURLs, forKey: .imageUrls)
        try container.encode(available, forKey: .available)
        try container.encode(currency, forKey: .currency)
        try container.encodeIfPresent(sku, forKey: .sku)
        try container.encodeIfPresent(shopifyProductId, forKey: .shopifyProductId)
        try container.encodeIfPresent(shopifyHandle, forKey: .shopifyHandle)
        try container.encode(availableForSale, forKey: .availableForSale)
        try container.encode(shopifySyncActive, forKey: .shopifySyncActive)
        try container.encode(variants, forKey: .variants)
        try container.encode(source, forKey: .source)
        try container.encode(isVisibleInApp, forKey: .isVisibleInApp)
        try container.encode(featured, forKey: .featured)
        try container.encode(sortOrder, forKey: .sortOrder)
        try container.encode(customBadge, forKey: .customBadge)
        try container.encode(customImageOverride, forKey: .customImageOverride)
    }
}
