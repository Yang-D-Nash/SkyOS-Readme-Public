import SwiftUI

/// Type-erased `Shape` for home hero (round vs. top-only roundrect).
private struct AnyShape: Shape, @unchecked Sendable {
    private let build: (CGRect) -> Path
    init<S: Shape>(_ shape: S) {
        build = { shape.path(in: $0) }
    }
    func path(in rect: CGRect) -> Path { build(rect) }
}

@inline(__always)
private func skydownHomeHeroContainerShape(immersive: Bool) -> AnyShape {
    if #available(iOS 16, *), immersive {
        return AnyShape(
            UnevenRoundedRectangle(
                topLeadingRadius: SkydownLayout.heroCornerRadius,
                bottomLeadingRadius: 0,
                bottomTrailingRadius: 0,
                topTrailingRadius: SkydownLayout.heroCornerRadius,
                style: .continuous
            )
        )
    } else {
        return AnyShape(
            RoundedRectangle(
                cornerRadius: SkydownLayout.heroCornerRadius,
                style: .continuous
            )
        )
    }
}

enum BrandMark: String, Identifiable {
    case skyos
    case sky22
    case skydown
    case zweizwei
    case skydownX22

    var id: String { rawValue }

    var imageName: String {
        switch self {
        case .skyos:
            return "SkyOSBrandMark"
        case .sky22:
            return "Sky22BrandLogo"
        case .zweizwei:
            return "ZweiZweiBrandLogo"
        case .skydown:
            return "SkydownBrandLogo"
        case .skydownX22:
            return "SkydownX22BrandLogo"
        }
    }

    var label: String {
        switch self {
        case .skyos:
            return NSLocalizedString("brand.system.name", comment: "Brand system name")
        case .sky22:
            return NSLocalizedString("brand.music.name", comment: "Brand music name")
        case .skydown:
            return NSLocalizedString("brand.product.name", comment: "Brand product name")
        case .zweizwei:
            return NSLocalizedString("brand.music.name", comment: "Brand music name")
        case .skydownX22:
            return NSLocalizedString("brand.merch.name", comment: "Brand merch name")
        }
    }
}

struct BrandHeroSurface<Footer: View>: View {
    let colorScheme: ColorScheme
    let eyebrow: String
    let title: String
    let subtitle: String
    let detail: String?
    let backgroundImageURL: String?
    let accent: Color
    let secondaryAccent: Color
    let marks: [BrandMark]
    /// Flache Unterkante, leichter Schatten — Home-Intro als Atmosphäre statt Karte.
    var immersive: Bool
    /// Tap auf Titel- und Textbereich (nicht den Footer) — vermeidet verschachtelte Buttons in den Pills.
    var onSurfaceTap: (() -> Void)?
    let footer: Footer
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    init(
        colorScheme: ColorScheme,
        eyebrow: String,
        title: String,
        subtitle: String,
        detail: String? = nil,
        backgroundImageURL: String? = nil,
        accent: Color,
        secondaryAccent: Color,
        marks: [BrandMark] = [],
        immersive: Bool = false,
        onSurfaceTap: (() -> Void)? = nil,
        @ViewBuilder footer: () -> Footer
    ) {
        self.colorScheme = colorScheme
        self.eyebrow = eyebrow
        self.title = title
        self.subtitle = subtitle
        self.detail = detail
        self.backgroundImageURL = backgroundImageURL
        self.accent = accent
        self.secondaryAccent = secondaryAccent
        self.marks = marks
        self.immersive = immersive
        self.onSurfaceTap = onSurfaceTap
        self.footer = footer()
    }

    var body: some View {
        let isCompactHero = horizontalSizeClass == .compact
        let capShape = skydownHomeHeroContainerShape(immersive: immersive)
        let hasBackgroundImage = !(backgroundImageURL?.isEmpty ?? true)
        let titleColor = hasBackgroundImage ? Color.white : AppColors.text(for: colorScheme)
        let subtitleColor = hasBackgroundImage ? Color.white.opacity(0.84) : AppColors.secondaryText(for: colorScheme).opacity(0.96)
        let detailColor = hasBackgroundImage ? Color.white.opacity(0.86) : AppColors.text(for: colorScheme).opacity(0.86)
        let titleShadowColor = hasBackgroundImage ? Color.black.opacity(colorScheme == .dark ? 0.32 : 0.24) : .clear
        let subtitleShadowColor = hasBackgroundImage ? Color.black.opacity(colorScheme == .dark ? 0.28 : 0.18) : .clear
        let shouldShowEyebrow = !eyebrow.isEmpty && !title.lowercased().hasPrefix(eyebrow.lowercased())
        let shouldShowDetail = {
            guard let detail, !detail.isEmpty else { return false }
            return detail.normalizedHeroComparisonText != subtitle.normalizedHeroComparisonText
        }()

        VStack(alignment: .leading, spacing: isCompactHero ? 11 : 14) {
            HStack(alignment: .top, spacing: isCompactHero ? 10 : 14) {
                VStack(alignment: .leading, spacing: isCompactHero ? 6 : 8) {
                    HStack(alignment: .center, spacing: 8) {
                        if shouldShowEyebrow {
                            Text(eyebrow.uppercased())
                                .font(AppTypography.heroEyebrow)
                                .tracking(1.15)
                                .foregroundColor(accent)
                                .shadow(color: titleShadowColor.opacity(0.45), radius: 4, y: 2)
                        }

                        Capsule(style: .continuous)
                            .fill(
                                LinearGradient(
                                    colors: [
                                        Color.white.opacity(colorScheme == .dark ? 0.28 : 0.72),
                                        accent.opacity(0.78),
                                        secondaryAccent.opacity(0.32)
                                    ],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: isCompactHero ? 36 : 46, height: 3)
                    }

                    Text(title)
                        .font(AppTypography.heroTitle)
                        .lineSpacing(2)
                        .foregroundColor(titleColor)
                        .multilineTextAlignment(.leading)
                        .shadow(color: titleShadowColor, radius: 7, y: 3)

                    Text(subtitle)
                        .font(AppTypography.heroSubtitle)
                        .lineSpacing(3)
                        .foregroundColor(subtitleColor)
                        .shadow(color: subtitleShadowColor, radius: 5, y: 2)
                        .lineLimit(isCompactHero ? 1 : 2)

                    if let detail, shouldShowDetail {
                        Text(detail)
                            .font(AppTypography.editorialCaption)
                            .tracking(0.42)
                            .foregroundColor(detailColor)
                            .padding(.top, 1)
                            .shadow(color: subtitleShadowColor.opacity(0.45), radius: 4, y: 2)
                            .lineLimit(isCompactHero ? 1 : 2)
                    }
                }

                if !marks.isEmpty && !isCompactHero {
                    VStack(spacing: 8) {
                        ForEach(marks.prefix(2)) { mark in
                            BrandMarkTile(
                                mark: mark,
                                colorScheme: colorScheme,
                                accent: accent,
                                isFeatured: marks.count == 1
                            )
                        }
                    }
                    .frame(width: marks.count == 1 ? 96 : 88)
                }
            }
            .contentShape(Rectangle())
            .modifier(SkydownOptionalHeroHeaderTapModifier(onSurfaceTap: onSurfaceTap))

            footer
                .padding(.top, isCompactHero ? 2 : 4)
        }
        .padding(.horizontal, isCompactHero ? 16 : SkydownLayout.heroPadding)
        .padding(.vertical, isCompactHero ? 15 : (SkydownLayout.heroPadding + 1))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            ZStack {
                capShape
                    .fill(
                        LinearGradient(
                            colors: [
                                AppColors.luminanceLift(for: colorScheme).opacity(colorScheme == .dark ? 0.12 : 0.28),
                                AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.99 : 0.975),
                                AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.36 : 0.28),
                                accent.opacity(colorScheme == .dark ? 0.05 : 0.05),
                                secondaryAccent.opacity(colorScheme == .dark ? 0.04 : 0.04),
                                Color.black.opacity(colorScheme == .dark ? 0.015 : 0.010)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )

                capShape
                    .fill(
                        RadialGradient(
                            colors: [
                                Color.white.opacity(colorScheme == .dark ? 0.08 : 0.18),
                                accent.opacity(colorScheme == .dark ? 0.08 : 0.10),
                                .clear
                            ],
                            center: .topLeading,
                            startRadius: 10,
                            endRadius: 320
                        )
                    )

                capShape
                    .fill(
                        RadialGradient(
                            colors: [
                                secondaryAccent.opacity(colorScheme == .dark ? 0.08 : 0.10),
                                .clear
                            ],
                            center: UnitPoint(x: 0.88, y: 0.84),
                            startRadius: 16,
                            endRadius: 340
                        )
                    )

                if let backgroundImageURL, let url = URL(string: backgroundImageURL) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .clipped()
                        default:
                            Color.clear
                        }
                    }
                    .clipShape(capShape)

                    capShape
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(colorScheme == .dark ? 0.10 : 0.10),
                                    Color.black.opacity(colorScheme == .dark ? 0.08 : 0.08),
                                    accent.opacity(colorScheme == .dark ? 0.08 : 0.07),
                                    secondaryAccent.opacity(colorScheme == .dark ? 0.06 : 0.05)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )

                    capShape
                        .fill(Color.black.opacity(colorScheme == .dark ? 0.10 : 0.08))

                    capShape
                        .fill(
                            RadialGradient(
                                colors: [
                                    Color.white.opacity(colorScheme == .dark ? 0.08 : 0.14),
                                    accent.opacity(colorScheme == .dark ? 0.08 : 0.10),
                                    .clear
                                ],
                                center: .topLeading,
                                startRadius: 8,
                                endRadius: 280
                            )
                        )
                }

                Circle()
                    .fill(accent.opacity(colorScheme == .dark ? 0.045 : 0.05))
                    .frame(width: 260, height: 260)
                    .blur(radius: 44)
                    .offset(x: 142, y: -88)

                Circle()
                    .fill(secondaryAccent.opacity(colorScheme == .dark ? 0.038 : 0.05))
                    .frame(width: 210, height: 210)
                    .blur(radius: 40)
                    .offset(x: -124, y: 112)
            }
        }
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: accent,
            cornerRadius: SkydownLayout.heroCornerRadius,
            shadowRadius: immersive ? 0 : 12,
            shadowYOffset: immersive ? 0 : 5
        )
        .overlay {
            if !immersive {
                GeometryReader { proxy in
                    let width = proxy.size.width
                    let height = proxy.size.height

                    LinearGradient(
                        colors: [
                            Color.white.opacity(0),
                            Color.white.opacity(colorScheme == .dark ? 0.10 : 0.14),
                            accent.opacity(colorScheme == .dark ? 0.08 : 0.09),
                            Color.white.opacity(0)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .frame(width: max(width * 0.34, 120), height: height * 1.7)
                    .rotationEffect(.degrees(16))
                    .offset(x: width * 0.34, y: -height * 0.16)
                    .blur(radius: 7)
                    .blendMode(.screen)
                    .mask(capShape)
                }
                .allowsHitTesting(false)
            }
        }
        .overlay {
            if !immersive {
                RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                    .stroke(
                        LinearGradient(
                            colors: [
                                AppColors.luminanceLift(for: colorScheme).opacity(colorScheme == .dark ? 0.16 : 0.24),
                                accent.opacity(0.12),
                                secondaryAccent.opacity(0.08)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
            }
        }
        .overlay(alignment: .topLeading) {
            if !immersive {
                Capsule(style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(colorScheme == .dark ? 0.28 : 0.78),
                                accent.opacity(0.72),
                                secondaryAccent.opacity(0.36)
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: 132, height: 3)
                    .padding(.top, 14)
                    .padding(.leading, 18)
                    .opacity(0.58)
            }
        }
    }
}

private extension String {
    var normalizedHeroComparisonText: String {
        lowercased()
            .replacingOccurrences(of: "[^\\p{L}\\p{N}]+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

/// Optionaler Tap auf den Hero-Textblock; Footer bleibt separat (Pills/Badges).
private struct SkydownOptionalHeroHeaderTapModifier: ViewModifier {
    let onSurfaceTap: (() -> Void)?

    func body(content: Content) -> some View {
        if let onSurfaceTap {
            content
                .onTapGesture(perform: onSurfaceTap)
                .accessibilityAddTraits(.isButton)
        } else {
            content
        }
    }
}

struct BrandHeroPill: View {
    let text: String
    let colorScheme: ColorScheme
    let tint: Color
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            pillContent
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private var pillContent: some View {
        HStack(spacing: 8) {
            Text(text)
                .font(AppTypography.editorialCaption)
                .tracking(0.35)
            Image(systemName: "arrow.up.right")
                .font(.caption2.weight(.heavy))
                .padding(5)
                .background(
                    Circle()
                        .fill(tint.opacity(colorScheme == .dark ? 0.18 : 0.14))
                )
        }
        .foregroundColor(tint)
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .skydownCapsuleSurface(colorScheme: colorScheme, accent: tint)
    }
}

struct SkydownMetaLabel: View {
    let text: String
    let tint: Color
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                Capsule(style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                tint.opacity(0.92),
                                tint.opacity(0.52)
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: 12, height: 4)

                Text(text)
                    .font(AppTypography.editorialFootnote)
                    .tracking(0.25)
                    .foregroundColor(tint.opacity(0.92))
            }
            .padding(.vertical, 2)
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private struct BrandMarkTile: View {
    let mark: BrandMark
    let colorScheme: ColorScheme
    let accent: Color
    let isFeatured: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(mark.imageName)
                .resizable()
                .renderingMode(.original)
                .scaledToFit()
                .frame(maxWidth: .infinity)
                .frame(height: isFeatured ? 58 : 42)
                .opacity(0.5)
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous))

            Text(mark.label)
                .font(isFeatured ? AppTypography.editorialFootnote : AppTypography.editorialFootnote)
                .tracking(0.35)
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.58))
                .lineLimit(1)
        }
        .padding(isFeatured ? 9 : 8)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(colorScheme == .dark ? 0.04 : 0.18),
                            accent.opacity(colorScheme == .dark ? 0.10 : 0.05),
                            AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.45 : 0.72)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [
                            accent.opacity(0.12),
                            Color.white.opacity(colorScheme == .dark ? 0.06 : 0.12)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 0.5
                )
        )
        .shadow(
            color: accent.opacity(colorScheme == .dark ? 0.04 : 0.03),
            radius: 4,
            y: 2
        )
    }
}
