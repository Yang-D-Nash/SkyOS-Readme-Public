//
//  SkydownLayout.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import SwiftUI

enum SkydownLayout {
    static let screenHorizontalPadding: CGFloat = 16
    static let screenTopPadding: CGFloat = 16
    static let screenBottomPadding: CGFloat = 28
    static let sectionSpacing: CGFloat = 16
    static let cardPadding: CGFloat = 18
    static let heroPadding: CGFloat = 20
    static let cardCornerRadius: CGFloat = 24
    static let heroCornerRadius: CGFloat = 26
    static let buttonCornerRadius: CGFloat = 18
}

private struct SkydownNavigationChromeModifier: ViewModifier {
    let colorScheme: ColorScheme

    func body(content: Content) -> some View {
        content
            .toolbarBackground(AppColors.primaryBackground(for: colorScheme), for: .navigationBar)
            .toolbarColorScheme(colorScheme, for: .navigationBar)
            .toolbar(.visible, for: .navigationBar)
    }
}

extension View {
    func skydownNavigationChrome(colorScheme: ColorScheme) -> some View {
        modifier(SkydownNavigationChromeModifier(colorScheme: colorScheme))
    }
}
