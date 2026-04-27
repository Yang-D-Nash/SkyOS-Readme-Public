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
            return Color(red: 81/255, green: 104/255, blue: 132/255)
        case .dark:
            return Color(red: 198/255, green: 214/255, blue: 232/255)
        @unknown default:
            return Color(red: 81/255, green: 104/255, blue: 132/255)
        }
    }

    static func accentMystic(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 121/255, green: 135/255, blue: 156/255)
        case .dark:
            return Color(red: 219/255, green: 227/255, blue: 236/255)
        @unknown default:
            return Color(red: 121/255, green: 135/255, blue: 156/255)
        }
    }

    static func accentHighlight(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 181/255, green: 159/255, blue: 125/255)
        case .dark:
            return Color(red: 242/255, green: 227/255, blue: 201/255)
        @unknown default:
            return Color(red: 181/255, green: 159/255, blue: 125/255)
        }
    }

    static func primaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 247/255, green: 246/255, blue: 243/255)
        case .dark:
            return Color(red: 9/255, green: 15/255, blue: 24/255)
        @unknown default:
            return Color(red: 247/255, green: 246/255, blue: 243/255)
        }
    }

    static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 236/255, green: 238/255, blue: 241/255)
        case .dark:
            return Color(red: 19/255, green: 28/255, blue: 41/255)
        @unknown default:
            return Color(red: 236/255, green: 238/255, blue: 241/255)
        }
    }

    static func cardBackground(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 255/255, green: 251/255, blue: 248/255)
        case .dark:
            return Color(red: 26/255, green: 37/255, blue: 52/255).opacity(0.97)
        @unknown default:
            return Color(red: 255/255, green: 251/255, blue: 248/255)
        }
    }

    static func text(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 14/255, green: 20/255, blue: 28/255)
        case .dark:
            return Color(red: 247/255, green: 243/255, blue: 236/255)
        @unknown default:
            return Color(red: 15/255, green: 22/255, blue: 31/255)
        }
    }

    static func secondaryText(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 97/255, green: 108/255, blue: 121/255)
        case .dark:
            return Color(red: 198/255, green: 206/255, blue: 216/255)
        @unknown default:
            return Color(red: 97/255, green: 108/255, blue: 121/255)
        }
    }

    static func luminanceLift(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 255/255, green: 250/255, blue: 241/255)
        case .dark:
            return Color(red: 240/255, green: 234/255, blue: 223/255)
        @unknown default:
            return Color(red: 255/255, green: 250/255, blue: 241/255)
        }
    }

    static func cinematicShadow(for colorScheme: ColorScheme) -> Color {
        switch colorScheme {
        case .light:
            return Color(red: 7/255, green: 13/255, blue: 22/255)
        case .dark:
            return Color(red: 2/255, green: 6/255, blue: 12/255)
        @unknown default:
            return Color(red: 7/255, green: 13/255, blue: 22/255)
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
            return Color(red: 30/255, green: 215/255, blue: 96/255).opacity(0.11)
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
        let surfaceWash = secondaryBackground(for: colorScheme)
        let pearlWash = cardBackground(for: colorScheme)

        let topSky: Color
        let midSky: Color
        let horizonGlow: Color
        let depthShadow: Color
        let luminanceLift = luminanceLift(for: colorScheme)

        switch colorScheme {
        case .light:
            topSky = Color(red: 250/255, green: 247/255, blue: 243/255)
            midSky = Color(red: 238/255, green: 240/255, blue: 243/255)
            horizonGlow = Color(red: 225/255, green: 230/255, blue: 236/255)
            depthShadow = cinematicShadow(for: colorScheme).opacity(0.045)
        case .dark:
            topSky = Color(red: 22/255, green: 28/255, blue: 38/255)
            midSky = Color(red: 26/255, green: 34/255, blue: 46/255)
            horizonGlow = Color(red: 38/255, green: 48/255, blue: 62/255)
            depthShadow = cinematicShadow(for: colorScheme).opacity(0.055)
        @unknown default:
            topSky = Color(red: 250/255, green: 247/255, blue: 243/255)
            midSky = Color(red: 238/255, green: 240/255, blue: 243/255)
            horizonGlow = Color(red: 225/255, green: 230/255, blue: 236/255)
            depthShadow = cinematicShadow(for: colorScheme).opacity(0.045)
        }

        return LinearGradient(
            colors: [
                luminanceLift.opacity(colorScheme == .dark ? 0.11 : 0.52),
                topSky,
                pearlWash.opacity(colorScheme == .dark ? 0.07 : 0.16),
                midSky,
                surfaceWash.opacity(colorScheme == .dark ? 0.12 : 0.22),
                horizonGlow,
                accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.028 : 0.024),
                accent(for: colorScheme).opacity(colorScheme == .dark ? 0.034 : 0.022),
                secondary.opacity(colorScheme == .dark ? 0.022 : 0.016),
                depthShadow.opacity(colorScheme == .dark ? 0.42 : 0.60),
                cinematicShadow(for: colorScheme).opacity(colorScheme == .dark ? 0.038 : 0.030),
                primaryBackground(for: colorScheme)
            ],
            startPoint: .topLeading,
            endPoint: .bottom
        )
    }
}
