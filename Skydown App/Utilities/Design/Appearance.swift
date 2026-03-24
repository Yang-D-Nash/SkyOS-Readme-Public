//
//  Appearance.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import Foundation

enum Appearance: String, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }
}
