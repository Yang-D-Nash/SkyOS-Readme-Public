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
    static let screenTopPadding: CGFloat = 12
    static let screenBottomPadding: CGFloat = 16
    static let sectionSpacing: CGFloat = 12
    static let cardPadding: CGFloat = 12
    static let heroPadding: CGFloat = 17
    static let cardCornerRadius: CGFloat = 20
    static let heroCornerRadius: CGFloat = 30
    static let buttonCornerRadius: CGFloat = 16
}

enum SkydownMotion {
    static let screenTransition = Animation.spring(response: 0.58, dampingFraction: 0.90, blendDuration: 0.18)
    static let emphasizedTransition = Animation.spring(response: 0.46, dampingFraction: 0.84, blendDuration: 0.14)
    static let statusTransition = Animation.spring(response: 0.44, dampingFraction: 0.84, blendDuration: 0.12)
    static let ambientLoopDuration: Double = 6.2
}

enum SkydownMotionAxis {
    case horizontal
    case vertical
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

private struct SkydownSelectionFeedbackModifier<Value: Equatable>: ViewModifier {
    let trigger: Value

    func body(content: Content) -> some View {
        content
            .onChange(of: trigger) { _, _ in
                #if canImport(UIKit)
                SkydownHaptics.selection()
                #endif
            }
    }
}

private struct SkydownSceneMotionModifier<Trigger: Equatable>: ViewModifier {
    let trigger: Trigger
    let axis: SkydownMotionAxis
    let travel: CGFloat
    let blurRadius: CGFloat
    @State private var motionProgress: CGFloat = 1
    @State private var hasAnimatedInitialAppearance = false

    func body(content: Content) -> some View {
        content
            .opacity(0.76 + (0.24 * motionProgress))
            .scaleEffect(0.986 + (0.014 * motionProgress))
            .offset(
                x: axis == .horizontal ? (1 - motionProgress) * travel : 0,
                y: axis == .vertical ? (1 - motionProgress) * travel : 0
            )
            .blur(radius: (1 - motionProgress) * blurRadius)
            .animation(SkydownMotion.screenTransition, value: motionProgress)
            .task {
                guard !hasAnimatedInitialAppearance else { return }
                hasAnimatedInitialAppearance = true
                stageEntrance(from: 0.22)
            }
            .onChange(of: trigger) { _, _ in
                stageEntrance(from: 0.12)
            }
    }

    private func stageEntrance(from startingProgress: CGFloat) {
        var transaction = Transaction(animation: nil)
        transaction.disablesAnimations = true
        withTransaction(transaction) {
            motionProgress = startingProgress
        }

        Task { @MainActor in
            await Task.yield()
            withAnimation(SkydownMotion.screenTransition) {
                motionProgress = 1
            }
        }
    }
}

private struct SkydownSceneActivationModifier: ViewModifier {
    let isActive: Bool
    let axis: SkydownMotionAxis
    let travel: CGFloat
    let blurRadius: CGFloat
    @State private var motionProgress: CGFloat = 1
    @State private var hasPrimedInitialState = false

    func body(content: Content) -> some View {
        content
            .opacity(0.78 + (0.22 * motionProgress))
            .scaleEffect(0.988 + (0.012 * motionProgress))
            .offset(
                x: axis == .horizontal ? (1 - motionProgress) * travel : 0,
                y: axis == .vertical ? (1 - motionProgress) * travel : 0
            )
            .blur(radius: (1 - motionProgress) * blurRadius)
            .animation(SkydownMotion.screenTransition, value: motionProgress)
            .task {
                guard !hasPrimedInitialState else { return }
                hasPrimedInitialState = true
                if isActive {
                    stageEntrance(from: 0.18)
                }
            }
            .onChange(of: isActive) { _, newValue in
                guard newValue else { return }
                stageEntrance(from: 0.1)
            }
    }

    private func stageEntrance(from startingProgress: CGFloat) {
        var transaction = Transaction(animation: nil)
        transaction.disablesAnimations = true
        withTransaction(transaction) {
            motionProgress = startingProgress
        }

        Task { @MainActor in
            await Task.yield()
            withAnimation(SkydownMotion.screenTransition) {
                motionProgress = 1
            }
        }
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

private struct SkydownLuminousSweepModifier: ViewModifier {
    let cornerRadius: CGFloat
    let accent: Color
    let alpha: CGFloat

    func body(content: Content) -> some View {
        content
            .overlay {
                GeometryReader { proxy in
                    let width = proxy.size.width
                    let height = proxy.size.height

                    LinearGradient(
                        colors: [
                            Color.white.opacity(0),
                            Color.white.opacity(alpha * 0.92),
                            accent.opacity(alpha),
                            Color.white.opacity(0)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .frame(width: max(width * 0.28, 78), height: height * 1.8)
                    .rotationEffect(.degrees(18))
                    .offset(x: width * 0.22, y: -height * 0.18)
                    .blur(radius: 10)
                    .blendMode(.screen)
                    .mask(
                        RoundedRectangle(
                            cornerRadius: cornerRadius,
                            style: .continuous
                        )
                    )
                }
                .allowsHitTesting(false)
            }
    }
}

private struct SkydownTabBarChromeModifier: ViewModifier {
    let colorScheme: ColorScheme
    let accent: Color
    let isVisible: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if !isVisible {
            content
                .toolbar(.hidden, for: .tabBar)
        } else if #available(iOS 26.0, *) {
            content
                .toolbar(.visible, for: .tabBar)
                .toolbarColorScheme(colorScheme, for: .tabBar)
                .background {
                    SkydownTabBarAppearanceView(colorScheme: colorScheme, accent: accent)
                        .frame(width: 0, height: 0)
                }
        } else {
            content
                .toolbar(.visible, for: .tabBar)
                .toolbarBackground(.visible, for: .tabBar)
                .toolbarBackground(.ultraThinMaterial, for: .tabBar)
                .toolbarColorScheme(colorScheme, for: .tabBar)
                .background {
                    SkydownTabBarAppearanceView(colorScheme: colorScheme, accent: accent)
                        .frame(width: 0, height: 0)
                }
        }
    }
}

#if canImport(UIKit)
private struct SkydownTabBarAppearanceView: UIViewRepresentable {
    let colorScheme: ColorScheme
    let accent: Color

    func makeUIView(context: Context) -> UIView {
        UIView(frame: .zero)
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        let appearance = UITabBarAppearance()
        appearance.configureWithTransparentBackground()
        appearance.shadowColor = UIColor(
            colorScheme == .dark
                ? AppColors.luminanceLift(for: colorScheme).opacity(0.04)
                : AppColors.cinematicShadow(for: colorScheme).opacity(0.05)
        )
        appearance.backgroundEffect = UIBlurEffect(
            style: colorScheme == .dark ? .systemThinMaterialDark : .systemMaterialLight
        )
        appearance.backgroundColor = UIColor(
            colorScheme == .dark
                ? AppColors.cardBackground(for: colorScheme).opacity(0.48)
                : AppColors.cardBackground(for: colorScheme).opacity(0.78)
        )
        appearance.selectionIndicatorImage = selectionIndicatorImage()

        let selectedColor = UIColor(accent.opacity(colorScheme == .dark ? 0.98 : 0.90))
        let normalColor = UIColor(AppColors.secondaryText(for: colorScheme).opacity(0.74))
        let selectedAttributes: [NSAttributedString.Key: Any] = [
            .font: AppTypography.tabBarLabelUIFont,
            .foregroundColor: selectedColor
        ]
        let normalAttributes: [NSAttributedString.Key: Any] = [
            .font: AppTypography.tabBarLabelUIFont,
            .foregroundColor: normalColor
        ]

        for itemAppearance in [
            appearance.stackedLayoutAppearance,
            appearance.inlineLayoutAppearance,
            appearance.compactInlineLayoutAppearance
        ] {
            itemAppearance.normal.iconColor = normalColor
            itemAppearance.normal.titleTextAttributes = normalAttributes
            itemAppearance.selected.iconColor = selectedColor
            itemAppearance.selected.titleTextAttributes = selectedAttributes
        }

        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
        UITabBar.appearance().tintColor = selectedColor
        UITabBar.appearance().unselectedItemTintColor = normalColor
    }

    private func selectionIndicatorImage() -> UIImage {
        let size = CGSize(width: 88, height: 40)
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { _ in
            let rect = CGRect(origin: .zero, size: size).insetBy(dx: 3, dy: 3)
            let path = UIBezierPath(roundedRect: rect, cornerRadius: 20)
            guard let context = UIGraphicsGetCurrentContext() else { return }
            context.saveGState()
            path.addClip()

            let gradientColors = [
                UIColor(AppColors.luminanceLift(for: colorScheme).opacity(colorScheme == .dark ? 0.16 : 0.22)).cgColor,
                UIColor(accent.opacity(colorScheme == .dark ? 0.24 : 0.16)).cgColor,
                UIColor(AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.12 : 0.10)).cgColor,
            ] as CFArray
            let gradient = CGGradient(
                colorsSpace: CGColorSpaceCreateDeviceRGB(),
                colors: gradientColors,
                locations: [0, 0.58, 1]
            )

            if let gradient {
                context.drawLinearGradient(
                    gradient,
                    start: CGPoint(x: rect.minX, y: rect.minY),
                    end: CGPoint(x: rect.maxX, y: rect.maxY),
                    options: []
                )
            }

            context.restoreGState()

            UIColor(
                AppColors.luminanceLift(for: colorScheme)
                    .opacity(colorScheme == .dark ? 0.22 : 0.20)
            ).setStroke()
            path.lineWidth = 1
            path.stroke()
        }

        return image.resizableImage(
            withCapInsets: UIEdgeInsets(top: 18, left: 18, bottom: 18, right: 18),
            resizingMode: .stretch
        )
    }
}
#endif

private struct SkydownPanelSurfaceModifier: ViewModifier {
    let colorScheme: ColorScheme
    let accent: Color?
    let cornerRadius: CGFloat
    let shadowRadius: CGFloat
    let shadowYOffset: CGFloat

    private var resolvedAccent: Color {
        accent ?? AppColors.accent(for: colorScheme)
    }

    private var strokeGradient: LinearGradient {
        LinearGradient(
            colors: [
                AppColors.luminanceLift(for: colorScheme).opacity(colorScheme == .dark ? 0.16 : 0.24),
                AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.08 : 0.12),
                resolvedAccent.opacity(colorScheme == .dark ? 0.10 : 0.09),
                AppColors.cinematicShadow(for: colorScheme).opacity(colorScheme == .dark ? 0.02 : 0.03)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    @ViewBuilder
    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        content
            .background {
                ZStack {
                    shape.fill(
                        LinearGradient(
                            colors: [
                                AppColors.luminanceLift(for: colorScheme).opacity(colorScheme == .dark ? 0.08 : 0.16),
                                AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.97 : 0.99),
                                AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.28 : 0.40),
                                AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.024 : 0.024),
                                resolvedAccent.opacity(colorScheme == .dark ? 0.018 : 0.016)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )

                    shape.fill(
                        RadialGradient(
                            colors: [
                                AppColors.luminanceLift(for: colorScheme).opacity(colorScheme == .dark ? 0.05 : 0.12),
                                resolvedAccent.opacity(colorScheme == .dark ? 0.028 : 0.034),
                                .clear
                            ],
                            center: .topLeading,
                            startRadius: 10,
                            endRadius: cornerRadius * 7
                        )
                    )

                    shape.fill(
                        RadialGradient(
                            colors: [
                                AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.036 : 0.05),
                                resolvedAccent.opacity(colorScheme == .dark ? 0.024 : 0.03),
                                .clear
                            ],
                            center: UnitPoint(x: 0.88, y: 0.86),
                            startRadius: 12,
                            endRadius: cornerRadius * 8
                        )
                    )
                }
            }
            .overlay {
                shape
                    .stroke(strokeGradient, lineWidth: 0.9)
            }
            .clipShape(shape)
            .shadow(
                color: resolvedAccent.opacity(colorScheme == .dark ? 0.016 : 0.020),
                radius: max(shadowRadius - 8, 4),
                y: max(shadowYOffset - 6, 2)
            )
            .shadow(
                color: AppColors.cinematicShadow(for: colorScheme).opacity(colorScheme == .dark ? 0.10 : 0.09),
                radius: max(shadowRadius - 6, 5),
                y: max(shadowYOffset - 3, 2)
            )
    }
}

private struct SkydownCapsuleSurfaceModifier: ViewModifier {
    let colorScheme: ColorScheme
    let accent: Color?

    private var strokeColor: Color {
        (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.14 : 0.14)
    }

    private var fillGradient: LinearGradient {
        let resolvedAccent = accent ?? AppColors.accent(for: colorScheme)

        return LinearGradient(
            colors: [
                AppColors.luminanceLift(for: colorScheme).opacity(colorScheme == .dark ? 0.14 : 0.32),
                AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.80 : 0.92),
                resolvedAccent.opacity(colorScheme == .dark ? 0.05 : 0.05),
                AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.04 : 0.03)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    @ViewBuilder
    func body(content: Content) -> some View {
        let shape = Capsule(style: .continuous)
        content
            .background {
                ZStack {
                    shape.fill(fillGradient)
                    shape.fill(
                        RadialGradient(
                            colors: [
                                AppColors.luminanceLift(for: colorScheme).opacity(colorScheme == .dark ? 0.06 : 0.14),
                                .clear
                            ],
                            center: .topLeading,
                            startRadius: 4,
                            endRadius: 180
                        )
                    )
                    shape.fill(
                        RadialGradient(
                            colors: [
                                (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.04 : 0.05),
                                .clear
                            ],
                            center: UnitPoint(x: 0.92, y: 0.86),
                            startRadius: 8,
                            endRadius: 200
                        )
                    )
                }
            }
            .overlay {
                shape
                    .stroke(
                        LinearGradient(
                            colors: [
                                AppColors.luminanceLift(for: colorScheme).opacity(colorScheme == .dark ? 0.12 : 0.22),
                                strokeColor,
                                (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.08 : 0.08)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 0.9
                    )
            }
            .clipShape(shape)
            .shadow(
                color: AppColors.cinematicShadow(for: colorScheme).opacity(colorScheme == .dark ? 0.18 : 0.10),
                radius: 7,
                y: 3
            )
    }
}

extension View {
    func skydownNavigationChrome(colorScheme: ColorScheme) -> some View {
        modifier(SkydownNavigationChromeModifier(colorScheme: colorScheme))
    }

    func skydownTabBarChrome(colorScheme: ColorScheme, accent: Color, isVisible: Bool = true) -> some View {
        modifier(
            SkydownTabBarChromeModifier(
                colorScheme: colorScheme,
                accent: accent,
                isVisible: isVisible
            )
        )
    }

    func skydownPanelSurface(
        colorScheme: ColorScheme,
        accent: Color? = nil,
        cornerRadius: CGFloat = SkydownLayout.cardCornerRadius,
        shadowRadius: CGFloat = 10,
        shadowYOffset: CGFloat = 5
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

    func skydownSelectionFeedback<Value: Equatable>(
        trigger: Value
    ) -> some View {
        modifier(
            SkydownSelectionFeedbackModifier(
                trigger: trigger
            )
        )
    }

    func skydownLuminousSweep(
        cornerRadius: CGFloat,
        accent: Color,
        alpha: CGFloat = 0.18,
        duration: Double = SkydownMotion.ambientLoopDuration
    ) -> some View {
        _ = duration
        return modifier(
            SkydownLuminousSweepModifier(
                cornerRadius: cornerRadius,
                accent: accent,
                alpha: alpha
            )
        )
    }

    func skydownSceneMotion<Trigger: Equatable>(
        trigger: Trigger,
        axis: SkydownMotionAxis = .horizontal,
        travel: CGFloat = 24,
        blurRadius: CGFloat = 8
    ) -> some View {
        modifier(
            SkydownSceneMotionModifier(
                trigger: trigger,
                axis: axis,
                travel: travel,
                blurRadius: blurRadius
            )
        )
    }

    func skydownSceneActivation(
        isActive: Bool,
        axis: SkydownMotionAxis = .horizontal,
        travel: CGFloat = 24,
        blurRadius: CGFloat = 7
    ) -> some View {
        modifier(
            SkydownSceneActivationModifier(
                isActive: isActive,
                axis: axis,
                travel: travel,
                blurRadius: blurRadius
            )
        )
    }
}
