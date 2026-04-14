import CoreText
import SwiftUI
import UIKit

enum AppTypography {
    private enum TitleWeight {
        case medium
        case semibold
        case bold
        case extraBold

        var postScriptName: String {
            switch self {
            case .medium:
                return "Syne-Medium"
            case .semibold:
                return "Syne-SemiBold"
            case .bold:
                return "Syne-Bold"
            case .extraBold:
                return "Syne-ExtraBold"
            }
        }

        var fallbackWeight: UIFont.Weight {
            switch self {
            case .medium:
                return .medium
            case .semibold:
                return .semibold
            case .bold:
                return .bold
            case .extraBold:
                return .heavy
            }
        }

        var fallbackSwiftUIWeight: Font.Weight {
            switch self {
            case .medium:
                return .medium
            case .semibold:
                return .semibold
            case .bold:
                return .bold
            case .extraBold:
                return .heavy
            }
        }
    }

    private static let bundledFonts: [(name: String, ext: String)] = [
        ("Syne-Regular", "ttf"),
        ("Syne-Medium", "ttf"),
        ("Syne-SemiBold", "ttf"),
        ("Syne-Bold", "ttf"),
        ("Syne-ExtraBold", "ttf"),
    ]

    static func configure() {
        registerFontsIfNeeded()
        configureAppearance()
    }

    static var heroEyebrow: Font {
        scaledFont(size: 12, relativeTo: .caption, weight: .semibold)
    }

    static var heroTitle: Font {
        scaledFont(size: 30, relativeTo: .largeTitle, weight: .extraBold)
    }

    static var sectionTitle: Font {
        scaledFont(size: 22, relativeTo: .title2, weight: .bold)
    }

    static var cardTitle: Font {
        scaledFont(size: 20, relativeTo: .title3, weight: .semibold)
    }

    private static func scaledFont(
        size: CGFloat,
        relativeTo textStyle: Font.TextStyle,
        weight: TitleWeight = .semibold
    ) -> Font {
        if UIFont(name: weight.postScriptName, size: size) != nil {
            return .custom(weight.postScriptName, size: size, relativeTo: textStyle)
        }

        return fallbackFont(size: size, relativeTo: textStyle, weight: weight)
    }

    private static func scaledUIFont(
        size: CGFloat,
        textStyle: UIFont.TextStyle,
        weight: TitleWeight = .semibold
    ) -> UIFont {
        let baseFont = UIFont(name: weight.postScriptName, size: size) ?? UIFont.systemFont(ofSize: size, weight: weight.fallbackWeight)
        return UIFontMetrics(forTextStyle: textStyle).scaledFont(for: baseFont)
    }

    private static func registerFontsIfNeeded() {
        for font in bundledFonts {
            guard let fontURL = Bundle.main.url(
                forResource: font.name,
                withExtension: font.ext,
                subdirectory: "Resources/Fonts"
            ) ?? Bundle.main.url(forResource: font.name, withExtension: font.ext) else {
                continue
            }

            CTFontManagerRegisterFontsForURL(fontURL as CFURL, .process, nil)
        }
    }

    private static func configureAppearance() {
        let navigationAppearance = UINavigationBarAppearance()
        navigationAppearance.configureWithTransparentBackground()
        navigationAppearance.titleTextAttributes = [
            .font: scaledUIFont(size: 20, textStyle: .headline, weight: .bold)
        ]
        navigationAppearance.largeTitleTextAttributes = [
            .font: scaledUIFont(size: 32, textStyle: .largeTitle, weight: .extraBold)
        ]

        UINavigationBar.appearance().standardAppearance = navigationAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navigationAppearance
        UINavigationBar.appearance().compactAppearance = navigationAppearance
    }

    private static func fallbackFont(
        size: CGFloat,
        relativeTo textStyle: Font.TextStyle,
        weight: TitleWeight
    ) -> Font {
        switch textStyle {
        case .largeTitle:
            return .system(size: size, weight: .bold, design: .rounded)
        case .title, .title2, .title3, .headline:
            return .system(size: size, weight: weight.fallbackSwiftUIWeight, design: .rounded)
        case .caption, .caption2, .footnote:
            return .system(size: size, weight: .medium, design: .rounded)
        default:
            return .system(size: size, weight: .regular, design: .rounded)
        }
    }
}
