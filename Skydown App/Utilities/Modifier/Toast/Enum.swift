//
//  Enum.swift
//  Skydown App
//
//  Created by Yang D. Nash on 22.08.25.
//

import Foundation
import SwiftUI

enum ToastStyle {
    case success
    case error
    case warning
    case info

    var color: Color {
        switch self {
        case .success: return Color(red: 67 / 255, green: 196 / 255, blue: 123 / 255)
        case .error: return Color(red: 1, green: 106 / 255, blue: 106 / 255)
        case .warning: return Color(red: 1, green: 191 / 255, blue: 102 / 255)
        case .info: return Color(red: 93 / 255, green: 175 / 255, blue: 1)
        }
    }

    var icon: String {
        switch self {
        case .success: return "checkmark.circle.fill"
        case .error: return "xmark.octagon.fill"
        case .warning: return "exclamationmark.triangle.fill"
        case .info: return "info.circle.fill"
        }
    }

    var title: String {
        switch self {
        case .success: return "Erfolgreich"
        case .error: return "Aktion fehlgeschlagen"
        case .warning: return "Kurz prüfen"
        case .info: return "Info"
        }
    }

    var secondaryColor: Color {
        switch self {
        case .success: return Color(red: 13 / 255, green: 48 / 255, blue: 27 / 255)
        case .error: return Color(red: 60 / 255, green: 17 / 255, blue: 21 / 255)
        case .warning: return Color(red: 58 / 255, green: 34 / 255, blue: 8 / 255)
        case .info: return Color(red: 10 / 255, green: 34 / 255, blue: 56 / 255)
        }
    }
}
