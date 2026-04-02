//
//  SkydownLayout.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import SwiftUI

enum SkydownLayout {
    static let screenHorizontalPadding: CGFloat = 16
    static let screenTopPadding: CGFloat = 10
    static let screenBottomPadding: CGFloat = 22
    static let sectionSpacing: CGFloat = 14
    static let cardPadding: CGFloat = 15
    static let heroPadding: CGFloat = 18
    static let cardCornerRadius: CGFloat = 24
    static let heroCornerRadius: CGFloat = 28
    static let buttonCornerRadius: CGFloat = 18
}

struct SkydownTactileButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.976 : 1)
            .saturation(configuration.isPressed ? 1.05 : 1)
            .brightness(configuration.isPressed ? -0.015 : 0)
            .offset(y: configuration.isPressed ? 1.5 : 0)
            .animation(.spring(response: 0.24, dampingFraction: 0.72), value: configuration.isPressed)
    }
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
        (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.15 : 0.14)
    }

    @ViewBuilder
    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)

        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.interactive(false), in: shape)
                .background {
                    shape.fill(
                        LinearGradient(
                            colors: [
                                AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.30 : 0.88),
                                AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.12 : 0.72),
                                (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.10 : 0.08),
                                AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.06 : 0.04)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                }
                .overlay {
                    shape
                        .stroke(strokeColor, lineWidth: 1)
                }
                .clipShape(shape)
                .shadow(
                    color: .black.opacity(colorScheme == .dark ? 0.16 : 0.08),
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
                                LinearGradient(
                                    colors: [
                                        AppColors.cardBackground(for: colorScheme)
                                            .opacity(colorScheme == .dark ? 0.56 : 0.92),
                                        AppColors.secondaryBackground(for: colorScheme)
                                            .opacity(colorScheme == .dark ? 0.18 : 0.70),
                                        (accent ?? AppColors.accent(for: colorScheme))
                                            .opacity(colorScheme == .dark ? 0.10 : 0.08),
                                        AppColors.accentHighlight(for: colorScheme)
                                            .opacity(colorScheme == .dark ? 0.06 : 0.04)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                        }
                }
                .overlay {
                    shape
                        .stroke(strokeColor, lineWidth: 1)
                }
                .clipShape(shape)
                .shadow(
                    color: .black.opacity(colorScheme == .dark ? 0.16 : 0.08),
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
        (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.20 : 0.18)
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
                                    .opacity(colorScheme == .dark ? 0.32 : 0.78)
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

    func skydownTactileAction() -> some View {
        buttonStyle(SkydownTactileButtonStyle())
    }
}
