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
    static let screenHorizontalPadding: CGFloat = 18
    static let screenTopPadding: CGFloat = 12
    static let screenBottomPadding: CGFloat = 18
    static let sectionSpacing: CGFloat = 16
    static let cardPadding: CGFloat = 16
    static let heroPadding: CGFloat = 20
    static let cardCornerRadius: CGFloat = 26
    static let heroCornerRadius: CGFloat = 30
    static let buttonCornerRadius: CGFloat = 20
}

enum SkydownMotion {
    static let screenTransition = Animation.spring(response: 0.54, dampingFraction: 0.88, blendDuration: 0.16)
    static let emphasizedTransition = Animation.spring(response: 0.42, dampingFraction: 0.82, blendDuration: 0.12)
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

private struct SkydownTabBarChromeModifier: ViewModifier {
    let colorScheme: ColorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .toolbar(.visible, for: .tabBar)
                .toolbarColorScheme(colorScheme, for: .tabBar)
                .background {
                    SkydownTabBarAppearanceView(colorScheme: colorScheme)
                        .frame(width: 0, height: 0)
                }
        } else {
            content
                .toolbar(.visible, for: .tabBar)
                .toolbarBackground(.visible, for: .tabBar)
                .toolbarBackground(.ultraThinMaterial, for: .tabBar)
                .toolbarColorScheme(colorScheme, for: .tabBar)
                .background {
                    SkydownTabBarAppearanceView(colorScheme: colorScheme)
                        .frame(width: 0, height: 0)
                }
        }
    }
}

#if canImport(UIKit)
private struct SkydownTabBarAppearanceView: UIViewRepresentable {
    let colorScheme: ColorScheme

    func makeUIView(context: Context) -> UIView {
        UIView(frame: .zero)
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        let appearance = UITabBarAppearance()
        appearance.configureWithTransparentBackground()
        appearance.shadowColor = UIColor(
            colorScheme == .dark
                ? Color.white.opacity(0.04)
                : Color.black.opacity(0.08)
        )
        appearance.backgroundEffect = UIBlurEffect(
            style: colorScheme == .dark ? .systemChromeMaterialDark : .systemThinMaterialLight
        )
        appearance.backgroundColor = UIColor(
            colorScheme == .dark
                ? AppColors.cardBackground(for: colorScheme).opacity(0.72)
                : Color.white.opacity(0.86)
        )
        appearance.selectionIndicatorImage = selectionIndicatorImage()

        let selectedColor = UIColor(AppColors.text(for: colorScheme))
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
        let size = CGSize(width: 82, height: 36)
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { _ in
            let rect = CGRect(origin: .zero, size: size).insetBy(dx: 3, dy: 3)
            let path = UIBezierPath(roundedRect: rect, cornerRadius: 18)
            UIColor(
                AppColors.accent(for: colorScheme)
                    .opacity(colorScheme == .dark ? 0.22 : 0.10)
            ).setFill()
            path.fill()

            UIColor(
                AppColors.accentHighlight(for: colorScheme)
                    .opacity(colorScheme == .dark ? 0.22 : 0.12)
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

    private var strokeColor: Color {
        (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.18 : 0.14)
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
                                AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.42 : 0.88),
                                AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.20 : 0.72),
                                (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.12 : 0.08),
                                AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.08 : 0.04)
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
                    color: .black.opacity(colorScheme == .dark ? 0.12 : 0.08),
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
                                            .opacity(colorScheme == .dark ? 0.70 : 0.92),
                                        AppColors.secondaryBackground(for: colorScheme)
                                            .opacity(colorScheme == .dark ? 0.26 : 0.70),
                                        (accent ?? AppColors.accent(for: colorScheme))
                                            .opacity(colorScheme == .dark ? 0.12 : 0.08),
                                        AppColors.accentHighlight(for: colorScheme)
                                            .opacity(colorScheme == .dark ? 0.08 : 0.04)
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
                    color: .black.opacity(colorScheme == .dark ? 0.12 : 0.08),
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
        (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.24 : 0.18)
    }

    private var fillGradient: LinearGradient {
        let resolvedAccent = accent ?? AppColors.accent(for: colorScheme)

        return LinearGradient(
            colors: [
                Color.white.opacity(colorScheme == .dark ? 0.05 : 0.42),
                AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.56 : 0.88),
                resolvedAccent.opacity(colorScheme == .dark ? 0.16 : 0.10),
                AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.12 : 0.08)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    @ViewBuilder
    func body(content: Content) -> some View {
        let shape = Capsule(style: .continuous)

        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.interactive(false), in: shape)
                .background {
                    shape.fill(fillGradient)
                }
                .overlay {
                    shape
                        .stroke(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(colorScheme == .dark ? 0.08 : 0.32),
                                    strokeColor,
                                    (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.30 : 0.22)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                }
                .clipShape(shape)
                .shadow(
                    color: (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.16 : 0.08),
                    radius: 10,
                    y: 4
                )
        } else {
            content
                .background {
                    shape
                        .fill(.ultraThinMaterial)
                        .overlay {
                            shape.fill(fillGradient)
                        }
                }
                .overlay {
                    shape
                        .stroke(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(colorScheme == .dark ? 0.08 : 0.32),
                                    strokeColor,
                                    (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.30 : 0.22)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                }
                .clipShape(shape)
                .shadow(
                    color: (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.16 : 0.08),
                    radius: 10,
                    y: 4
                )
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

    func skydownSelectionFeedback<Value: Equatable>(
        trigger: Value
    ) -> some View {
        modifier(
            SkydownSelectionFeedbackModifier(
                trigger: trigger
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
