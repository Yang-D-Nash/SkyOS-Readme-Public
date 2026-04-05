import CoreText
import SwiftUI
import UIKit

enum AppTypography {
    private static let postScriptName = "TheBraveFREE"
    private static let fontFileName = "Awergy-Regular"
    private static let fontFileExtension = "otf"

    static func configure() {
        registerFontIfNeeded()
        configureAppearance()
    }

    static var heroEyebrow: Font {
        scaledFont(size: 12, relativeTo: .caption)
    }

    static var heroTitle: Font {
        scaledFont(size: 30, relativeTo: .largeTitle)
    }

    static var sectionTitle: Font {
        scaledFont(size: 22, relativeTo: .title2)
    }

    static var cardTitle: Font {
        scaledFont(size: 20, relativeTo: .title3)
    }

    static func scaledFont(size: CGFloat, relativeTo textStyle: Font.TextStyle) -> Font {
        if UIFont(name: postScriptName, size: size) != nil {
            return .custom(postScriptName, size: size, relativeTo: textStyle)
        }

        return fallbackFont(size: size, relativeTo: textStyle)
    }

    static func scaledUIFont(size: CGFloat, textStyle: UIFont.TextStyle) -> UIFont {
        let baseFont = UIFont(name: postScriptName, size: size) ?? UIFont.systemFont(ofSize: size, weight: .regular)
        return UIFontMetrics(forTextStyle: textStyle).scaledFont(for: baseFont)
    }

    private static func registerFontIfNeeded() {
        guard let fontURL = Bundle.main.url(
            forResource: fontFileName,
            withExtension: fontFileExtension,
            subdirectory: "Resources/Fonts"
        ) ?? Bundle.main.url(forResource: fontFileName, withExtension: fontFileExtension) else {
            return
        }

        CTFontManagerRegisterFontsForURL(fontURL as CFURL, .process, nil)
    }

    private static func configureAppearance() {
        let navigationAppearance = UINavigationBarAppearance()
        navigationAppearance.configureWithTransparentBackground()
        navigationAppearance.titleTextAttributes = [
            .font: scaledUIFont(size: 20, textStyle: .headline)
        ]
        navigationAppearance.largeTitleTextAttributes = [
            .font: scaledUIFont(size: 32, textStyle: .largeTitle)
        ]

        UINavigationBar.appearance().standardAppearance = navigationAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navigationAppearance
        UINavigationBar.appearance().compactAppearance = navigationAppearance
    }

    private static func fallbackFont(size: CGFloat, relativeTo textStyle: Font.TextStyle) -> Font {
        switch textStyle {
        case .largeTitle:
            return .system(size: size, weight: .bold, design: .rounded)
        case .title, .title2, .title3, .headline:
            return .system(size: size, weight: .semibold, design: .rounded)
        case .caption, .caption2, .footnote:
            return .system(size: size, weight: .medium, design: .rounded)
        default:
            return .system(size: size, weight: .regular, design: .rounded)
        }
    }
}
