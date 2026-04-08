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
            return Color(red: 48/255, green: 78/255, blue: 108/255)
        case .dark:
            return Color(red: 126/255, green: 160/255, blue: 191/255)
        @unknown default:
            return Color(red: 48/255, green: 78/255, blue: 108/255)
        }
    }
    
    static func accentMystic(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 70/255, green: 101/255, blue: 131/255)
        case .dark:
            return Color(red: 153/255, green: 181/255, blue: 209/255)
        @unknown default:
            return Color(red: 70/255, green: 101/255, blue: 131/255)
        }
    }

    static func accentHighlight(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 111/255, green: 137/255, blue: 162/255)
        case .dark:
            return Color(red: 199/255, green: 214/255, blue: 228/255)
        @unknown default:
            return Color(red: 111/255, green: 137/255, blue: 162/255)
        }
    }
    
    static func primaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 232/255, green: 236/255, blue: 241/255)
        case .dark:
            return Color(red: 4/255, green: 9/255, blue: 16/255)
        @unknown default:
            return Color(red: 232/255, green: 236/255, blue: 241/255)
        }
    }
    
    static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 218/255, green: 224/255, blue: 231/255)
        case .dark:
            return Color(red: 12/255, green: 20/255, blue: 30/255)
        @unknown default:
            return Color(red: 218/255, green: 224/255, blue: 231/255)
        }
    }
    
    static func cardBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 245/255, green: 247/255, blue: 250/255)
        case .dark:
            return Color(red: 10/255, green: 18/255, blue: 28/255).opacity(0.96)
        @unknown default:
            return Color(red: 245/255, green: 247/255, blue: 250/255)
        }
    }
    
    static func text(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 14/255, green: 24/255, blue: 35/255)
        case .dark:
            return Color(red: 240/255, green: 244/255, blue: 248/255)
        @unknown default:
            return Color(red: 14/255, green: 24/255, blue: 35/255)
        }
    }
    
    static func secondaryText(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 79/255, green: 93/255, blue: 108/255)
        case .dark:
            return Color(red: 166/255, green: 181/255, blue: 196/255)
        @unknown default:
            return Color(red: 79/255, green: 93/255, blue: 108/255)
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
            topSky = Color(red: 220/255, green: 228/255, blue: 236/255)
            midSky = Color(red: 205/255, green: 215/255, blue: 226/255)
            horizonGlow = Color(red: 184/255, green: 197/255, blue: 211/255)
            depthShadow = Color.black.opacity(0.08)
        case .dark:
            topSky = Color(red: 4/255, green: 8/255, blue: 14/255)
            midSky = Color(red: 8/255, green: 15/255, blue: 24/255)
            horizonGlow = Color(red: 14/255, green: 28/255, blue: 40/255)
            depthShadow = Color.black.opacity(0.42)
        @unknown default:
            topSky = Color(red: 220/255, green: 228/255, blue: 236/255)
            midSky = Color(red: 205/255, green: 215/255, blue: 226/255)
            horizonGlow = Color(red: 184/255, green: 197/255, blue: 211/255)
            depthShadow = Color.black.opacity(0.08)
        }

        return LinearGradient(
            colors: [
                topSky,
                midSky,
                horizonGlow,
                accent(for: colorScheme).opacity(colorScheme == .dark ? 0.05 : 0.022),
                secondary.opacity(colorScheme == .dark ? 0.04 : 0.018),
                depthShadow,
                Color.black.opacity(colorScheme == .dark ? 0.30 : 0.10),
                primaryBackground(for: colorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}
