//
//  OrderItem.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import Foundation

struct OrderItem: Codable, Identifiable {
    var id: String = UUID().uuidString
    var name: String
    var quantity: Int
    var size: String?
    var color: String?
    var productId: String?
    var shopifyVariantId: String?
    var sku: String?
    var unitPrice: Double?

    enum CodingKeys: String, CodingKey {
        case name
        case quantity
        case size
        case color
        case productId
        case shopifyVariantId
        case sku
        case unitPrice
    }
}
