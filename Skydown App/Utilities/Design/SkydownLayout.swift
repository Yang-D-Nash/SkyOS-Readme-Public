//
//  SkydownLayout.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

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

#if canImport(UIKit)
@MainActor
final class SkydownKeyboardObserver: NSObject, ObservableObject {
    @Published private(set) var bottomInset: CGFloat = 0

    private let notificationCenter: NotificationCenter

    init(notificationCenter: NotificationCenter = .default) {
        self.notificationCenter = notificationCenter
        super.init()

        notificationCenter.addObserver(
            self,
            selector: #selector(handleKeyboardNotification(_:)),
            name: UIResponder.keyboardWillChangeFrameNotification,
            object: nil
        )
        notificationCenter.addObserver(
            self,
            selector: #selector(handleKeyboardNotification(_:)),
            name: UIResponder.keyboardWillHideNotification,
            object: nil
        )
    }

    deinit {
        notificationCenter.removeObserver(self)
    }

    @objc private func handleKeyboardNotification(_ notification: Notification) {
        handle(notification)
    }

    private func handle(_ notification: Notification) {
        let userInfo = notification.userInfo ?? [:]
        let duration = userInfo[UIResponder.keyboardAnimationDurationUserInfoKey] as? Double ?? 0.25
        let targetInset: CGFloat

        if notification.name == UIResponder.keyboardWillHideNotification {
            targetInset = 0
        } else {
            let endFrame = (userInfo[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect) ?? .zero
            let window = Self.activeWindow
            let screenBottom = window?.screen.bounds.maxY ?? UIScreen.main.bounds.maxY
            let safeAreaBottom = window?.safeAreaInsets.bottom ?? 0
            targetInset = max(0, screenBottom - endFrame.minY - safeAreaBottom)
        }

        withAnimation(.easeOut(duration: duration)) {
            bottomInset = targetInset
        }
    }

    private static var activeWindow: UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
    }
}
#endif

struct SkydownResponsiveLayout {
    let availableWidth: CGFloat

    private var isDesktop: Bool {
        #if targetEnvironment(macCatalyst)
        return true
        #else
        return false
        #endif
    }

    var contentMaxWidth: CGFloat {
        if isDesktop {
            switch availableWidth {
            case ..<900:
                return .infinity
            case ..<1200:
                return 1040
            case ..<1600:
                return 1280
            default:
                return 1440
            }
        } else {
            switch availableWidth {
            case ..<720:
                return .infinity
            case ..<960:
                return 880
            case ..<1320:
                return 1040
            default:
                return 1220
            }
        }
    }

    var horizontalPadding: CGFloat {
        if isDesktop {
            switch availableWidth {
            case ..<900:
                return SkydownLayout.screenHorizontalPadding
            case ..<1400:
                return 32
            default:
                return 40
            }
        } else {
            switch availableWidth {
            case ..<720:
                return SkydownLayout.screenHorizontalPadding
            case ..<1320:
                return 24
            default:
                return 32
            }
        }
    }

    var sectionSpacing: CGFloat {
        prefersTwoColumn ? 18 : SkydownLayout.sectionSpacing
    }

    var prefersTwoColumn: Bool {
        availableWidth >= 920
    }

    var prefersThreeColumn: Bool {
        availableWidth >= 1120
    }

    var prefersDesktopChrome: Bool {
        availableWidth >= 1320
    }
}

struct SkydownTactileButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        SkydownTactileButtonBody(configuration: configuration)
    }
}

private struct SkydownTactileButtonBody: View {
    let configuration: ButtonStyle.Configuration
    @State private var emittedPressHaptic = false

    var body: some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.972 : 1)
            .saturation(configuration.isPressed ? 1.05 : 1)
            .brightness(configuration.isPressed ? -0.018 : 0)
            .offset(y: configuration.isPressed ? 1.8 : 0)
            .shadow(
                color: Color.black.opacity(configuration.isPressed ? 0.12 : 0.06),
                radius: configuration.isPressed ? 5 : 10,
                y: configuration.isPressed ? 2 : 6
            )
            .animation(.spring(response: 0.24, dampingFraction: 0.72), value: configuration.isPressed)
            .onChange(of: configuration.isPressed) { _, isPressed in
                if isPressed && !emittedPressHaptic {
                    emittedPressHaptic = true
                    #if canImport(UIKit)
                    SkydownHaptics.impact(.light)
                    #endif
                } else if !isPressed {
                    emittedPressHaptic = false
                }
            }
    }
}

private struct SkydownPressFeedbackModifier: ViewModifier {
    let pressedScale: CGFloat
    let pressedOffsetY: CGFloat
    @State private var isPressed = false
    @State private var emittedPressHaptic = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(isPressed ? pressedScale : 1)
            .saturation(isPressed ? 1.04 : 1)
            .brightness(isPressed ? -0.014 : 0)
            .offset(y: isPressed ? pressedOffsetY : 0)
            .shadow(
                color: Color.black.opacity(isPressed ? 0.14 : 0.06),
                radius: isPressed ? 5 : 10,
                y: isPressed ? 2 : 6
            )
            .animation(.spring(response: 0.22, dampingFraction: 0.74), value: isPressed)
            .onLongPressGesture(
                minimumDuration: 0,
                maximumDistance: 56,
                pressing: { pressing in
                    isPressed = pressing
                    if pressing && !emittedPressHaptic {
                        emittedPressHaptic = true
                        #if canImport(UIKit)
                        SkydownHaptics.impact(.light)
                        #endif
                    } else if !pressing {
                        emittedPressHaptic = false
                    }
                },
                perform: {}
            )
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

    func skydownInteractiveFeedback(
        pressedScale: CGFloat = 0.976,
        pressedOffsetY: CGFloat = 1.4
    ) -> some View {
        modifier(
            SkydownPressFeedbackModifier(
                pressedScale: pressedScale,
                pressedOffsetY: pressedOffsetY
            )
        )
    }
}
