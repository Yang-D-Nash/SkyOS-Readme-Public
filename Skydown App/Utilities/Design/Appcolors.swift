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
            return Color(red: 154/255, green: 176/255, blue: 198/255)
        case .dark:
            return Color(red: 240/255, green: 245/255, blue: 250/255)
        @unknown default:
            return Color(red: 154/255, green: 176/255, blue: 198/255)
        }
    }
    
    static func primaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 240/255, green: 244/255, blue: 248/255)
        case .dark:
            return Color(red: 15/255, green: 23/255, blue: 33/255)
        @unknown default:
            return Color(red: 240/255, green: 244/255, blue: 248/255)
        }
    }
    
    static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 231/255, green: 237/255, blue: 244/255)
        case .dark:
            return Color(red: 24/255, green: 34/255, blue: 47/255)
        @unknown default:
            return Color(red: 231/255, green: 237/255, blue: 244/255)
        }
    }
    
    static func cardBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 251/255, green: 253/255, blue: 255/255)
        case .dark:
            return Color(red: 31/255, green: 42/255, blue: 55/255).opacity(0.96)
        @unknown default:
            return Color(red: 251/255, green: 253/255, blue: 255/255)
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
            return Color(red: 88/255, green: 104/255, blue: 122/255)
        case .dark:
            return Color(red: 220/255, green: 228/255, blue: 236/255)
        @unknown default:
            return Color(red: 88/255, green: 104/255, blue: 122/255)
        }
    }

    static func luminanceLift(for colorScheme: ColorScheme) -> Color {
        Color.white
    }

    static func cinematicShadow(for colorScheme: ColorScheme) -> Color {
        Color.black
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
        let luminanceLift = luminanceLift(for: colorScheme)

        switch colorScheme {
        case .light:
            topSky = Color(red: 242/255, green: 246/255, blue: 250/255)
            midSky = Color(red: 228/255, green: 235/255, blue: 243/255)
            horizonGlow = Color(red: 209/255, green: 221/255, blue: 233/255)
            depthShadow = Color.black.opacity(0.05)
        case .dark:
            topSky = Color(red: 20/255, green: 29/255, blue: 40/255)
            midSky = Color(red: 28/255, green: 39/255, blue: 53/255)
            horizonGlow = Color(red: 42/255, green: 58/255, blue: 76/255)
            depthShadow = Color.black.opacity(0.07)
        @unknown default:
            topSky = Color(red: 242/255, green: 246/255, blue: 250/255)
            midSky = Color(red: 228/255, green: 235/255, blue: 243/255)
            horizonGlow = Color(red: 209/255, green: 221/255, blue: 233/255)
            depthShadow = Color.black.opacity(0.05)
        }

        return LinearGradient(
            colors: [
                luminanceLift.opacity(colorScheme == .dark ? 0.18 : 0.70),
                topSky,
                midSky,
                horizonGlow,
                accent(for: colorScheme).opacity(colorScheme == .dark ? 0.09 : 0.036),
                secondary.opacity(colorScheme == .dark ? 0.06 : 0.026),
                depthShadow,
                cinematicShadow(for: colorScheme).opacity(colorScheme == .dark ? 0.09 : 0.06),
                primaryBackground(for: colorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}
