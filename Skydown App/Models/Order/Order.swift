//
//  OrderItem.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import Foundation
import FirebaseFirestore

struct Order: Codable, Identifiable {
    @DocumentID var id: String?
    var userEmail: String
    var items: [OrderItem]
    var isCompleted: Bool
    var timestamp: Date
}
