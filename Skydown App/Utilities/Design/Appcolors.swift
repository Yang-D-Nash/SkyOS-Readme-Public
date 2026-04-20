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
            return Color(red: 64/255, green: 93/255, blue: 122/255)
        case .dark:
            return Color(red: 171/255, green: 197/255, blue: 223/255)
        @unknown default:
            return Color(red: 64/255, green: 93/255, blue: 122/255)
        }
    }
    
    static func accentMystic(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 98/255, green: 122/255, blue: 147/255)
        case .dark:
            return Color(red: 201/255, green: 216/255, blue: 231/255)
        @unknown default:
            return Color(red: 98/255, green: 122/255, blue: 147/255)
        }
    }

    static func accentHighlight(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 143/255, green: 164/255, blue: 184/255)
        case .dark:
            return Color(red: 235/255, green: 241/255, blue: 247/255)
        @unknown default:
            return Color(red: 143/255, green: 164/255, blue: 184/255)
        }
    }
    
    static func primaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 236/255, green: 240/255, blue: 245/255)
        case .dark:
            return Color(red: 8/255, green: 12/255, blue: 18/255)
        @unknown default:
            return Color(red: 236/255, green: 240/255, blue: 245/255)
        }
    }
    
    static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 226/255, green: 232/255, blue: 239/255)
        case .dark:
            return Color(red: 14/255, green: 21/255, blue: 31/255)
        @unknown default:
            return Color(red: 226/255, green: 232/255, blue: 239/255)
        }
    }
    
    static func cardBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 248/255, green: 250/255, blue: 252/255)
        case .dark:
            return Color(red: 20/255, green: 29/255, blue: 40/255).opacity(0.94)
        @unknown default:
            return Color(red: 248/255, green: 250/255, blue: 252/255)
        }
    }
    
    static func text(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 16/255, green: 27/255, blue: 37/255)
        case .dark:
            return Color(red: 248/255, green: 250/255, blue: 252/255)
        @unknown default:
            return Color(red: 16/255, green: 27/255, blue: 37/255)
        }
    }
    
    static func secondaryText(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 82/255, green: 98/255, blue: 115/255)
        case .dark:
            return Color(red: 195/255, green: 205/255, blue: 216/255)
        @unknown default:
            return Color(red: 82/255, green: 98/255, blue: 115/255)
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

        let topSky: Color
        let midSky: Color
        let horizonGlow: Color
        let depthShadow: Color

        switch colorScheme {
        case .light:
            topSky = Color(red: 232/255, green: 237/255, blue: 243/255)
            midSky = Color(red: 218/255, green: 226/255, blue: 234/255)
            horizonGlow = Color(red: 196/255, green: 208/255, blue: 221/255)
            depthShadow = Color.black.opacity(0.07)
        case .dark:
            topSky = Color(red: 7/255, green: 11/255, blue: 16/255)
            midSky = Color(red: 11/255, green: 17/255, blue: 25/255)
            horizonGlow = Color(red: 23/255, green: 34/255, blue: 49/255)
            depthShadow = Color.black.opacity(0.20)
        @unknown default:
            topSky = Color(red: 232/255, green: 237/255, blue: 243/255)
            midSky = Color(red: 218/255, green: 226/255, blue: 234/255)
            horizonGlow = Color(red: 196/255, green: 208/255, blue: 221/255)
            depthShadow = Color.black.opacity(0.07)
        }

        return LinearGradient(
            colors: [
                topSky,
                midSky,
                horizonGlow,
                accent(for: colorScheme).opacity(colorScheme == .dark ? 0.11 : 0.028),
                secondary.opacity(colorScheme == .dark ? 0.08 : 0.022),
                depthShadow,
                Color.black.opacity(colorScheme == .dark ? 0.22 : 0.11),
                primaryBackground(for: colorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}
