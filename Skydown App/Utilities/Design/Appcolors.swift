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
            return Color(red: 28/255, green: 86/255, blue: 146/255)
        case .dark:
            return Color(red: 134/255, green: 216/255, blue: 255/255)
        @unknown default:
            return Color(red: 28/255, green: 86/255, blue: 146/255)
        }
    }
    
    static func accentMystic(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 63/255, green: 122/255, blue: 194/255)
        case .dark:
            return Color(red: 188/255, green: 213/255, blue: 255/255)
        @unknown default:
            return Color(red: 63/255, green: 122/255, blue: 194/255)
        }
    }

    static func accentHighlight(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 36/255, green: 136/255, blue: 214/255)
        case .dark:
            return Color(red: 220/255, green: 243/255, blue: 255/255)
        @unknown default:
            return Color(red: 36/255, green: 136/255, blue: 214/255)
        }
    }
    
    static func primaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 229/255, green: 237/255, blue: 247/255)
        case .dark:
            return Color(red: 4/255, green: 10/255, blue: 19/255)
        @unknown default:
            return Color(red: 229/255, green: 237/255, blue: 247/255)
        }
    }
    
    static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 212/255, green: 223/255, blue: 238/255)
        case .dark:
            return Color(red: 13/255, green: 23/255, blue: 38/255)
        @unknown default:
            return Color(red: 212/255, green: 223/255, blue: 238/255)
        }
    }
    
    static func cardBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 248/255, green: 251/255, blue: 255/255)
        case .dark:
            return Color(red: 9/255, green: 18/255, blue: 30/255).opacity(0.96)
        @unknown default:
            return Color(red: 248/255, green: 251/255, blue: 255/255)
        }
    }
    
    static func text(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 9/255, green: 16/255, blue: 27/255)
        case .dark:
            return Color(red: 243/255, green: 247/255, blue: 252/255)
        @unknown default:
            return Color(red: 9/255, green: 16/255, blue: 27/255)
        }
    }
    
    static func secondaryText(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 63/255, green: 75/255, blue: 93/255)
        case .dark:
            return Color(red: 161/255, green: 173/255, blue: 190/255)
        @unknown default:
            return Color(red: 63/255, green: 75/255, blue: 93/255)
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

    static func youtube(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 255/255, green: 49/255, blue: 81/255)
        case .dark:
            return Color(red: 255/255, green: 92/255, blue: 116/255)
        @unknown default:
            return Color(red: 255/255, green: 49/255, blue: 81/255)
        }
    }

    static func youtubeDeep(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 184/255, green: 21/255, blue: 56/255)
        case .dark:
            return Color(red: 214/255, green: 43/255, blue: 84/255)
        @unknown default:
            return Color(red: 184/255, green: 21/255, blue: 56/255)
        }
    }

    static func youtubeSurface(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return youtube(for: colorScheme).opacity(0.12)
        case .dark:
            return youtube(for: colorScheme).opacity(0.18)
        @unknown default:
            return youtube(for: colorScheme).opacity(0.12)
        }
    }

    static func instagramStart(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 131/255, green: 58/255, blue: 180/255)
        case .dark:
            return Color(red: 176/255, green: 92/255, blue: 255/255)
        @unknown default:
            return Color(red: 131/255, green: 58/255, blue: 180/255)
        }
    }

    static func instagramEnd(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 253/255, green: 29/255, blue: 29/255)
        case .dark:
            return Color(red: 255/255, green: 154/255, blue: 111/255)
        @unknown default:
            return Color(red: 253/255, green: 29/255, blue: 29/255)
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
                accent(for: colorScheme).opacity(colorScheme == .dark ? 0.24 : 0.16),
                secondary.opacity(colorScheme == .dark ? 0.18 : 0.12),
                accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.16 : 0.11),
                Color.white.opacity(colorScheme == .dark ? 0.02 : 0.10),
                primaryBackground(for: colorScheme)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}
