//
//  CartItem.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import Foundation

struct CartItem: Identifiable {
    let id = UUID()
    let item: MerchandiseItem
    let size: String
    let color: String?
    var quantity: Int
    let shopifyVariantId: String?
    let sku: String?
    let unitPrice: Double?

    var effectiveUnitPrice: Double {
        unitPrice ?? item.price
    }
}
