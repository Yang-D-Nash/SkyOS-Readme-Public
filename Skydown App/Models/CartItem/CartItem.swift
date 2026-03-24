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
    var quantity: Int
}
