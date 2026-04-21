import CoreText
import SwiftUI
import UIKit

enum AppTypography {
    private enum DisplayWeight {
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

    private enum BrushWeight {
        case regular

        var postScriptName: String {
            switch self {
            case .regular:
                return "Awergy-Regular"
            }
        }
    }

    private enum InterfaceWeight {
        case regular
        case medium
        case semibold
        case bold

        var displayWeight: DisplayWeight {
            switch self {
            case .regular:
                return .regular
            case .medium:
                return .medium
            case .semibold:
                return .semibold
            case .bold:
                return .bold
            }
        }

        var uiKitWeight: UIFont.Weight {
            switch self {
            case .regular:
                return .regular
            case .medium:
                return .medium
            case .semibold:
                return .semibold
            case .bold:
                return .bold
            }
        }

        var swiftUIWeight: Font.Weight {
            switch self {
            case .regular:
                return .regular
            case .medium:
                return .medium
            case .semibold:
                return .semibold
            case .bold:
                return .bold
            }
        }
    }

    private static let bundledFonts: [(name: String, ext: String)] = [
        ("Awergy-Regular", "otf"),
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
        displayFont(size: 12.5, relativeTo: .caption, weight: .bold)
    }

    static var sectionEyebrow: Font {
        displayFont(size: 11.5, relativeTo: .caption, weight: .semibold)
    }

    static var heroTitle: Font {
        brushFont(size: 38, relativeTo: .largeTitle)
    }

    static var heroSubtitle: Font {
        interfaceFont(size: 17.0, relativeTo: .body, weight: .medium)
    }

    static var sectionTitle: Font {
        displayFont(size: 26, relativeTo: .title2, weight: .bold)
    }

    static var sectionHeadline: Font {
        interfaceFont(size: 18.5, relativeTo: .headline, weight: .semibold)
    }

    static var cardTitle: Font {
        displayFont(size: 20, relativeTo: .title3, weight: .semibold)
    }

    static var body: Font {
        interfaceFont(size: 16.0, relativeTo: .body, weight: .regular)
    }

    static var bodyCaption: Font {
        interfaceFont(size: 13.4, relativeTo: .caption, weight: .medium)
    }

    static var buttonLabel: Font {
        interfaceFont(size: 14.2, relativeTo: .subheadline, weight: .semibold)
    }

    static var metricLabel: Font {
        interfaceFont(size: 16.2, relativeTo: .headline, weight: .semibold)
    }

    static var listTitle: Font {
        interfaceFont(size: 16.2, relativeTo: .headline, weight: .semibold)
    }

    static var listMeta: Font {
        interfaceFont(size: 12.8, relativeTo: .caption, weight: .medium)
    }

    static var editorialBody: Font {
        interfaceFont(size: 16.2, relativeTo: .body, weight: .regular)
    }

    static var editorialCaption: Font {
        interfaceFont(size: 13.4, relativeTo: .caption, weight: .medium)
    }

    static var editorialFootnote: Font {
        interfaceFont(size: 12.6, relativeTo: .footnote, weight: .medium)
    }

    static var tabBarLabelUIFont: UIFont {
        interfaceUIFont(size: 11.5, textStyle: .caption1, weight: .medium)
    }

    private static func displayFont(
        size: CGFloat,
        relativeTo textStyle: Font.TextStyle,
        weight: DisplayWeight = .semibold
    ) -> Font {
        if UIFont(name: weight.postScriptName, size: size) != nil {
            return .custom(weight.postScriptName, size: size, relativeTo: textStyle)
        }

        return fallbackDisplayFont(size: size, relativeTo: textStyle, weight: weight)
    }

    private static func brushFont(
        size: CGFloat,
        relativeTo textStyle: Font.TextStyle
    ) -> Font {
        if UIFont(name: BrushWeight.regular.postScriptName, size: size) != nil {
            return .custom(BrushWeight.regular.postScriptName, size: size, relativeTo: textStyle)
        }

        return fallbackDisplayFont(size: size, relativeTo: textStyle, weight: .bold)
    }

    private static func interfaceFont(
        size: CGFloat,
        relativeTo textStyle: Font.TextStyle,
        weight: InterfaceWeight = .regular
    ) -> Font {
        let displayWeight = weight.displayWeight
        if UIFont(name: displayWeight.postScriptName, size: size) != nil {
            return .custom(displayWeight.postScriptName, size: size, relativeTo: textStyle)
        }

        return .system(size: size, weight: weight.swiftUIWeight, design: .default)
    }

    private static func interfaceUIFont(
        size: CGFloat,
        textStyle: UIFont.TextStyle,
        weight: InterfaceWeight = .regular
    ) -> UIFont {
        let displayWeight = weight.displayWeight
        if let font = UIFont(name: displayWeight.postScriptName, size: size) {
            return UIFontMetrics(forTextStyle: textStyle).scaledFont(for: font)
        }

        let baseFont = UIFont.systemFont(ofSize: size, weight: weight.uiKitWeight)
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
            .font: interfaceUIFont(size: 18.5, textStyle: .headline, weight: .semibold)
        ]
        navigationAppearance.largeTitleTextAttributes = [
            .font: interfaceUIFont(size: 34, textStyle: .largeTitle, weight: .bold)
        ]

        UINavigationBar.appearance().standardAppearance = navigationAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navigationAppearance
        UINavigationBar.appearance().compactAppearance = navigationAppearance
    }

    private static func fallbackDisplayFont(
        size: CGFloat,
        relativeTo textStyle: Font.TextStyle,
        weight: DisplayWeight
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
