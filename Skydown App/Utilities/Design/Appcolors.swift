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
            return Color(red: 53/255, green: 92/255, blue: 134/255)
        case .dark:
            return Color(red: 106/255, green: 182/255, blue: 244/255)
        @unknown default:
            return Color(red: 53/255, green: 92/255, blue: 134/255)
        }
    }
    
    static func accentMystic(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 77/255, green: 129/255, blue: 182/255)
        case .dark:
            return Color(red: 139/255, green: 200/255, blue: 255/255)
        @unknown default:
            return Color(red: 77/255, green: 129/255, blue: 182/255)
        }
    }

    static func accentHighlight(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 117/255, green: 173/255, blue: 229/255)
        case .dark:
            return Color(red: 194/255, green: 230/255, blue: 255/255)
        @unknown default:
            return Color(red: 117/255, green: 173/255, blue: 229/255)
        }
    }
    
    static func primaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 231/255, green: 237/255, blue: 243/255)
        case .dark:
            return Color(red: 2/255, green: 7/255, blue: 13/255)
        @unknown default:
            return Color(red: 231/255, green: 237/255, blue: 243/255)
        }
    }
    
    static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 214/255, green: 224/255, blue: 234/255)
        case .dark:
            return Color(red: 10/255, green: 19/255, blue: 29/255)
        @unknown default:
            return Color(red: 214/255, green: 224/255, blue: 234/255)
        }
    }
    
    static func cardBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 245/255, green: 248/255, blue: 251/255)
        case .dark:
            return Color(red: 8/255, green: 17/255, blue: 26/255).opacity(0.96)
        @unknown default:
            return Color(red: 245/255, green: 248/255, blue: 251/255)
        }
    }
    
    static func text(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 10/255, green: 18/255, blue: 26/255)
        case .dark:
            return Color(red: 239/255, green: 244/255, blue: 250/255)
        @unknown default:
            return Color(red: 10/255, green: 18/255, blue: 26/255)
        }
    }
    
    static func secondaryText(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 85/255, green: 98/255, blue: 115/255)
        case .dark:
            return Color(red: 149/255, green: 168/255, blue: 185/255)
        @unknown default:
            return Color(red: 85/255, green: 98/255, blue: 115/255)
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
                accent(for: colorScheme).opacity(colorScheme == .dark ? 0.18 : 0.10),
                secondary.opacity(colorScheme == .dark ? 0.14 : 0.09),
                accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.10 : 0.07),
                Color.black.opacity(colorScheme == .dark ? 0.20 : 0.03),
                primaryBackground(for: colorScheme)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}
