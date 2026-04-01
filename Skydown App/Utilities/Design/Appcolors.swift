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
            return Color(red: 68/255, green: 107/255, blue: 144/255)
        case .dark:
            return Color(red: 160/255, green: 200/255, blue: 243/255)
        @unknown default:
            return Color(red: 68/255, green: 107/255, blue: 144/255)
        }
    }
    
    static func accentMystic(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 107/255, green: 142/255, blue: 183/255)
        case .dark:
            return Color(red: 196/255, green: 223/255, blue: 248/255)
        @unknown default:
            return Color(red: 107/255, green: 142/255, blue: 183/255)
        }
    }

    static func accentHighlight(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 169/255, green: 198/255, blue: 231/255)
        case .dark:
            return Color(red: 220/255, green: 233/255, blue: 246/255)
        @unknown default:
            return Color(red: 169/255, green: 198/255, blue: 231/255)
        }
    }
    
    static func primaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 238/255, green: 244/255, blue: 249/255)
        case .dark:
            return Color(red: 6/255, green: 11/255, blue: 18/255)
        @unknown default:
            return Color(red: 238/255, green: 244/255, blue: 249/255)
        }
    }
    
    static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 225/255, green: 234/255, blue: 243/255)
        case .dark:
            return Color(red: 16/255, green: 24/255, blue: 36/255)
        @unknown default:
            return Color(red: 225/255, green: 234/255, blue: 243/255)
        }
    }
    
    static func cardBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color.white.opacity(0.90)
        case .dark:
            return Color(red: 12/255, green: 20/255, blue: 31/255).opacity(0.94)
        @unknown default:
            return Color.white.opacity(0.90)
        }
    }
    
    static func text(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 14/255, green: 21/255, blue: 32/255)
        case .dark:
            return Color(red: 243/255, green: 247/255, blue: 252/255)
        @unknown default:
            return Color(red: 14/255, green: 21/255, blue: 32/255)
        }
    }
    
    static func secondaryText(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 88/255, green: 101/255, blue: 120/255)
        case .dark:
            return Color(red: 161/255, green: 173/255, blue: 190/255)
        @unknown default:
            return Color(red: 88/255, green: 101/255, blue: 120/255)
        }
    }

    static func spotify(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 30/255, green: 215/255, blue: 96/255)
        case .dark:
            return Color(red: 30/255, green: 215/255, blue: 96/255)
        @unknown default:
            return Color(red: 30/255, green: 215/255, blue: 96/255)
        }
    }

    static func spotifySurface(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 30/255, green: 215/255, blue: 96/255).opacity(0.12)
        case .dark:
            return Color(red: 30/255, green: 215/255, blue: 96/255).opacity(0.18)
        @unknown default:
            return Color(red: 30/255, green: 215/255, blue: 96/255).opacity(0.12)
        }
    }

    static func screenGradient(
        for colorScheme: ColorScheme,
        secondaryAccent: Color? = nil
    ) -> LinearGradient {
        let secondary = secondaryAccent ?? accentMystic(for: colorScheme)
        return LinearGradient(
            colors: [
                primaryBackground(for: colorScheme),
                accent(for: colorScheme).opacity(colorScheme == .dark ? 0.18 : 0.14),
                secondary.opacity(colorScheme == .dark ? 0.14 : 0.10),
                accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.10 : 0.08),
                primaryBackground(for: colorScheme)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}
