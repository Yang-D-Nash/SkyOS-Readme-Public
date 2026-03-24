//
//  SettingsAlert.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import Foundation

enum SettingsAlert: Identifiable {
    case logout
    case deleteAccount

    var id: Int {
        hashValue
    }
}
