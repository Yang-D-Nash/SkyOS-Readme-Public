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

    enum CodingKeys: String, CodingKey {
        case name, quantity, size
    }
}
