import CoreText
import SwiftUI
import UIKit

enum AppTypography {
    private enum TitleWeight {
        case regular
        case medium
        case semibold
        case bold
        case extraBold

        var postScriptName: String {
            switch self {
            case .regular:
                return "Syne-Regular"
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
            case .regular:
                return .regular
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
            case .regular:
                return .regular
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
        scaledFont(size: 12.5, relativeTo: .caption, weight: .bold)
    }

    static var sectionEyebrow: Font {
        scaledFont(size: 11.5, relativeTo: .caption, weight: .semibold)
    }

    static var heroTitle: Font {
        scaledFont(size: 34, relativeTo: .largeTitle, weight: .extraBold)
    }

    static var sectionTitle: Font {
        scaledFont(size: 24, relativeTo: .title2, weight: .bold)
    }

    static var sectionHeadline: Font {
        scaledFont(size: 18.5, relativeTo: .headline, weight: .bold)
    }

    static var cardTitle: Font {
        scaledFont(size: 21, relativeTo: .title3, weight: .bold)
    }

    static var body: Font {
        scaledFont(size: 15.5, relativeTo: .body, weight: .regular)
    }

    static var bodyStrong: Font {
        scaledFont(size: 15.5, relativeTo: .body, weight: .semibold)
    }

    static var bodyCaption: Font {
        scaledFont(size: 13.5, relativeTo: .caption, weight: .medium)
    }

    static var buttonLabel: Font {
        scaledFont(size: 14.5, relativeTo: .subheadline, weight: .semibold)
    }

    static var metricLabel: Font {
        scaledFont(size: 16.5, relativeTo: .headline, weight: .bold)
    }

    static var editorialBody: Font {
        scaledFont(size: 15.5, relativeTo: .body, weight: .regular)
    }

    static var editorialCaption: Font {
        scaledFont(size: 13.5, relativeTo: .caption, weight: .medium)
    }

    static var editorialFootnote: Font {
        scaledFont(size: 12.5, relativeTo: .footnote, weight: .medium)
    }

    static var tabBarLabelUIFont: UIFont {
        scaledUIFont(size: 11.5, textStyle: .caption1, weight: .medium)
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
            .font: scaledUIFont(size: 21, textStyle: .headline, weight: .bold)
        ]
        navigationAppearance.largeTitleTextAttributes = [
            .font: scaledUIFont(size: 34, textStyle: .largeTitle, weight: .extraBold)
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
