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
    var customerName: String?
    var customerEmail: String?
    var whatsApp: String?
    var shippingAddress: String?
    var paymentMethod: String?
    var subtotalAmount: Double?
    var shippingAmount: Double?
    var taxRate: Double?
    var taxAmount: Double?
    var totalAmount: Double?
    var message: String?
    var items: [OrderItem]
    var isCompleted: Bool
    var timestamp: Date
}
