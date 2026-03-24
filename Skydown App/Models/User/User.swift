//
//  User.swift
//  Skydown App
//
//  Created by Yang D. Nash on 18.08.25.
//


import Foundation
import FirebaseFirestore

struct User: Codable, Identifiable {
    @DocumentID var id: String?
    var email: String
    var username: String
    var whatsApp: String?
    var registrationDate: Date
    var isAdmin: Bool = false
}
