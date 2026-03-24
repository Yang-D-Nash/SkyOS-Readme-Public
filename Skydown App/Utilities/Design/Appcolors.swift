//
//  Appcolors.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI

struct AppColors {
    static func accent(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 135/255, green: 206/255, blue: 250/255)
        case .dark:
            return Color.blue
        @unknown default:
            return Color.blue
        }
    }
    
    static func accentMystic(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 176/255, green: 224/255, blue: 230/255)
        case .dark:
            return Color.cyan
        @unknown default:
            return Color.cyan
        }
    }
    
    static func primaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 225/255, green: 245/255, blue: 254/255)
        case .dark:
            return Color.black
        @unknown default:
            return Color.white
        }
    }
    
    static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color.white
        case .dark:
            return Color.gray.opacity(0.3)
        @unknown default:
            return Color.gray.opacity(0.1)
        }
    }
    
    static func cardBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color.white.opacity(0.9)
        case .dark:
            return Color.gray.opacity(0.1)
        @unknown default:
            return Color.white
        }
    }
    
    static func text(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color.black
        case .dark:
            return Color.white
        @unknown default:
            return Color.black
        }
    }
    
    static func secondaryText(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color.gray
        case .dark:
            return Color.gray.opacity(0.7)
        @unknown default:
            return Color.gray
        }
    }
}
