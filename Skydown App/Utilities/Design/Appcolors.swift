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
            return Color(red: 22/255, green: 93/255, blue: 1.0)
        case .dark:
            return Color(red: 140/255, green: 178/255, blue: 1.0)
        @unknown default:
            return Color(red: 22/255, green: 93/255, blue: 1.0)
        }
    }
    
    static func accentMystic(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 0/255, green: 167/255, blue: 196/255)
        case .dark:
            return Color(red: 99/255, green: 225/255, blue: 238/255)
        @unknown default:
            return Color(red: 0/255, green: 167/255, blue: 196/255)
        }
    }

    static func accentHighlight(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 239/255, green: 159/255, blue: 40/255)
        case .dark:
            return Color(red: 1.0, green: 199/255, blue: 106/255)
        @unknown default:
            return Color(red: 239/255, green: 159/255, blue: 40/255)
        }
    }
    
    static func primaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 246/255, green: 243/255, blue: 237/255)
        case .dark:
            return Color(red: 9/255, green: 11/255, blue: 17/255)
        @unknown default:
            return Color(red: 246/255, green: 243/255, blue: 237/255)
        }
    }
    
    static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 241/255, green: 244/255, blue: 250/255)
        case .dark:
            return Color(red: 26/255, green: 33/255, blue: 48/255)
        @unknown default:
            return Color(red: 241/255, green: 244/255, blue: 250/255)
        }
    }
    
    static func cardBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color.white.opacity(0.92)
        case .dark:
            return Color(red: 18/255, green: 22/255, blue: 33/255).opacity(0.92)
        @unknown default:
            return Color.white.opacity(0.92)
        }
    }
    
    static func text(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 17/255, green: 19/255, blue: 24/255)
        case .dark:
            return Color(red: 245/255, green: 247/255, blue: 251/255)
        @unknown default:
            return Color(red: 17/255, green: 19/255, blue: 24/255)
        }
    }
    
    static func secondaryText(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 89/255, green: 97/255, blue: 113/255)
        case .dark:
            return Color(red: 154/255, green: 164/255, blue: 181/255)
        @unknown default:
            return Color(red: 89/255, green: 97/255, blue: 113/255)
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
                accent(for: colorScheme).opacity(0.14),
                secondary.opacity(0.08),
                primaryBackground(for: colorScheme),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}
