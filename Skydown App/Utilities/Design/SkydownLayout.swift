//
//  SkydownLayout.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import SwiftUI

enum SkydownLayout {
    static let screenHorizontalPadding: CGFloat = 14
    static let screenTopPadding: CGFloat = 12
    static let screenBottomPadding: CGFloat = 24
    static let sectionSpacing: CGFloat = 14
    static let cardPadding: CGFloat = 16
    static let heroPadding: CGFloat = 18
    static let cardCornerRadius: CGFloat = 22
    static let heroCornerRadius: CGFloat = 24
    static let buttonCornerRadius: CGFloat = 16
}

private struct SkydownNavigationChromeModifier: ViewModifier {
    let colorScheme: ColorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .toolbarColorScheme(colorScheme, for: .navigationBar)
                .toolbar(.visible, for: .navigationBar)
        } else {
            content
                .toolbarBackground(.hidden, for: .navigationBar)
                .toolbarColorScheme(colorScheme, for: .navigationBar)
                .toolbar(.visible, for: .navigationBar)
        }
    }
}

private struct SkydownTabBarChromeModifier: ViewModifier {
    let colorScheme: ColorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .toolbar(.visible, for: .tabBar)
                .toolbarColorScheme(colorScheme, for: .tabBar)
        } else {
            content
                .toolbar(.visible, for: .tabBar)
                .toolbarBackground(.visible, for: .tabBar)
                .toolbarBackground(.ultraThinMaterial, for: .tabBar)
                .toolbarColorScheme(colorScheme, for: .tabBar)
        }
    }
}

private struct SkydownPanelSurfaceModifier: ViewModifier {
    let colorScheme: ColorScheme
    let accent: Color?
    let cornerRadius: CGFloat
    let shadowRadius: CGFloat
    let shadowYOffset: CGFloat

    private var strokeColor: Color {
        (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.18 : 0.12)
    }

    @ViewBuilder
    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)

        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.interactive(false), in: shape)
                .overlay {
                    shape
                        .stroke(strokeColor, lineWidth: 1)
                }
                .clipShape(shape)
                .shadow(
                    color: .black.opacity(colorScheme == .dark ? 0.18 : 0.06),
                    radius: shadowRadius,
                    y: shadowYOffset
                )
        } else {
            content
                .background {
                    shape
                        .fill(.ultraThinMaterial)
                        .overlay {
                            shape.fill(
                                AppColors.cardBackground(for: colorScheme)
                                    .opacity(colorScheme == .dark ? 0.40 : 0.24)
                            )
                        }
                }
                .overlay {
                    shape
                        .stroke(strokeColor, lineWidth: 1)
                }
                .clipShape(shape)
                .shadow(
                    color: .black.opacity(colorScheme == .dark ? 0.18 : 0.06),
                    radius: shadowRadius,
                    y: shadowYOffset
                )
        }
    }
}

private struct SkydownCapsuleSurfaceModifier: ViewModifier {
    let colorScheme: ColorScheme
    let accent: Color?

    private var strokeColor: Color {
        (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.20 : 0.14)
    }

    @ViewBuilder
    func body(content: Content) -> some View {
        let shape = Capsule(style: .continuous)

        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.interactive(false), in: shape)
                .overlay {
                    shape
                        .stroke(strokeColor, lineWidth: 1)
                }
                .clipShape(shape)
        } else {
            content
                .background {
                    shape
                        .fill(.ultraThinMaterial)
                        .overlay {
                            shape.fill(
                                AppColors.cardBackground(for: colorScheme)
                                    .opacity(colorScheme == .dark ? 0.32 : 0.18)
                            )
                        }
                }
                .overlay {
                    shape
                        .stroke(strokeColor, lineWidth: 1)
                }
                .clipShape(shape)
        }
    }
}

extension View {
    func skydownNavigationChrome(colorScheme: ColorScheme) -> some View {
        modifier(SkydownNavigationChromeModifier(colorScheme: colorScheme))
    }

    func skydownTabBarChrome(colorScheme: ColorScheme) -> some View {
        modifier(SkydownTabBarChromeModifier(colorScheme: colorScheme))
    }

    func skydownPanelSurface(
        colorScheme: ColorScheme,
        accent: Color? = nil,
        cornerRadius: CGFloat = SkydownLayout.cardCornerRadius,
        shadowRadius: CGFloat = 14,
        shadowYOffset: CGFloat = 8
    ) -> some View {
        modifier(
            SkydownPanelSurfaceModifier(
                colorScheme: colorScheme,
                accent: accent,
                cornerRadius: cornerRadius,
                shadowRadius: shadowRadius,
                shadowYOffset: shadowYOffset
            )
        )
    }

    func skydownCapsuleSurface(
        colorScheme: ColorScheme,
        accent: Color? = nil
    ) -> some View {
        modifier(
            SkydownCapsuleSurfaceModifier(
                colorScheme: colorScheme,
                accent: accent
            )
        )
    }
}
