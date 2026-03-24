//
//  MerchandiseItem.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import Foundation
import FirebaseFirestore

struct MerchandiseItem: Codable, Identifiable {
    @DocumentID var id: String?
    var name: String
    var price: Double
    var description: String
    var imageURLs: [String]
    var available: Bool
}
