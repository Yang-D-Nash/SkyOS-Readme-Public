import SwiftUI

enum BrandMark: String, Identifiable {
    case sky22
    case skydown
    case zweizwei
    case skydownX22

    var id: String { rawValue }

    var imageName: String {
        switch self {
        case .sky22, .zweizwei:
            return "Sky22BrandLogo"
        case .skydown:
            return "SkydownBrandLogo"
        case .skydownX22:
            return "SkydownX22BrandLogo"
        }
    }

    var label: String {
        switch self {
        case .sky22:
            return "22"
        case .skydown:
            return "Skydown"
        case .zweizwei:
            return "22"
        case .skydownX22:
            return "SkyOS"
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
    let footer: Footer

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
        self.footer = footer()
    }

    var body: some View {
        let hasBackgroundImage = !(backgroundImageURL?.isEmpty ?? true)
        let titleColor = hasBackgroundImage ? Color.white : AppColors.text(for: colorScheme)
        let subtitleColor = hasBackgroundImage ? Color.white.opacity(0.84) : AppColors.secondaryText(for: colorScheme).opacity(0.96)
        let detailColor = hasBackgroundImage ? Color.white.opacity(0.86) : AppColors.text(for: colorScheme).opacity(0.86)
        let titleShadowColor = hasBackgroundImage ? Color.black.opacity(colorScheme == .dark ? 0.32 : 0.24) : .clear
        let subtitleShadowColor = hasBackgroundImage ? Color.black.opacity(colorScheme == .dark ? 0.28 : 0.18) : .clear

        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .center, spacing: 8) {
                        Text(eyebrow.uppercased())
                            .font(AppTypography.heroEyebrow)
                            .tracking(1.15)
                            .foregroundColor(accent)
                            .shadow(color: titleShadowColor.opacity(0.45), radius: 4, y: 2)

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
                            .frame(width: 46, height: 3)
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

                    if let detail, !detail.isEmpty {
                        Text(detail)
                            .font(AppTypography.editorialCaption)
                            .tracking(0.42)
                            .foregroundColor(detailColor)
                            .padding(.top, 1)
                            .shadow(color: subtitleShadowColor.opacity(0.45), radius: 4, y: 2)
                    }
                }

                if !marks.isEmpty {
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
                    .frame(width: marks.count == 1 ? 118 : 96)
                }
            }

            footer
                .padding(.top, 4)
        }
        .padding(.horizontal, SkydownLayout.heroPadding)
        .padding(.vertical, SkydownLayout.heroPadding + 1)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            let heroShape = RoundedRectangle(
                cornerRadius: SkydownLayout.heroCornerRadius,
                style: .continuous
            )

            ZStack {
                heroShape
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

                heroShape
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

                heroShape
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
                    .clipShape(heroShape)

                    heroShape
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

                    heroShape
                        .fill(Color.black.opacity(colorScheme == .dark ? 0.10 : 0.08))

                    heroShape
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
            shadowRadius: 12,
            shadowYOffset: 5
        )
        .overlay {
            GeometryReader { proxy in
                let shape = RoundedRectangle(
                    cornerRadius: SkydownLayout.heroCornerRadius,
                    style: .continuous
                )
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
                .mask(shape)
            }
            .allowsHitTesting(false)
        }
        .overlay {
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
        .overlay(alignment: .topLeading) {
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

struct BrandHeroPill: View {
    let text: String
    let colorScheme: ColorScheme
    let tint: Color
    var onTap: (() -> Void)? = nil

    var body: some View {
        Group {
            if let onTap {
                Button(action: onTap) {
                    pillContent
                }
                .buttonStyle(.plain)
                .skydownTactileAction()
            } else {
                SkydownMetaLabel(text: text, tint: tint)
            }
        }
    }

    private var pillContent: some View {
        HStack(spacing: 8) {
            Text(text)
                .font(AppTypography.editorialCaption)
                .tracking(0.35)
            if onTap != nil {
                Image(systemName: "arrow.up.right")
                    .font(.caption2.weight(.heavy))
                    .padding(5)
                    .background(
                        Circle()
                            .fill(tint.opacity(colorScheme == .dark ? 0.18 : 0.14))
                    )
            }
        }
        .foregroundColor(tint)
        .padding(.horizontal, onTap == nil ? 11 : 14)
        .padding(.vertical, onTap == nil ? 7 : 9)
        .skydownCapsuleSurface(colorScheme: colorScheme, accent: tint)
    }
}

struct SkydownMetaLabel: View {
    let text: String
    let tint: Color

    var body: some View {
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
}

private struct BrandMarkTile: View {
    let mark: BrandMark
    let colorScheme: ColorScheme
    let accent: Color
    let isFeatured: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(mark.imageName)
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity)
                .frame(height: isFeatured ? 76 : 52)
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous))

            Text(mark.label)
                .font(isFeatured ? AppTypography.editorialCaption : AppTypography.editorialFootnote)
                .tracking(0.5)
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.88))
                .lineLimit(1)
        }
        .padding(isFeatured ? 11 : 10)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(colorScheme == .dark ? 0.08 : 0.40),
                            accent.opacity(colorScheme == .dark ? 0.20 : 0.10),
                            AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.86 : 0.98),
                            Color.black.opacity(colorScheme == .dark ? 0.24 : 0.04)
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
                            accent.opacity(0.26),
                            Color.white.opacity(colorScheme == .dark ? 0.10 : 0.24)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1
                )
        )
        .shadow(
            color: accent.opacity(colorScheme == .dark ? 0.09 : 0.06),
            radius: 8,
            y: 3
        )
    }
}
