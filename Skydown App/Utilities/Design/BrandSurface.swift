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
            return "SkyOs"
        }
    }
}

struct BrandHeroSurface<Footer: View>: View {
    @State private var sheenTravel: CGFloat = -1.2
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
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 16) {
                VStack(alignment: .leading, spacing: 10) {
                    Text(eyebrow.uppercased())
                        .font(AppTypography.heroEyebrow)
                        .tracking(2.1)
                        .foregroundColor(accent)

                    Text(title)
                        .font(AppTypography.heroTitle)
                        .lineSpacing(2)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(subtitle)
                        .font(AppTypography.editorialBody)
                        .lineSpacing(2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.96))

                    if let detail, !detail.isEmpty {
                        Text(detail)
                            .font(AppTypography.editorialCaption)
                            .tracking(0.3)
                            .foregroundColor(AppColors.text(for: colorScheme).opacity(0.86))
                    }
                }

                if !marks.isEmpty {
                    VStack(spacing: 10) {
                        ForEach(marks.prefix(2)) { mark in
                            BrandMarkTile(
                                mark: mark,
                                colorScheme: colorScheme,
                                accent: accent,
                                isFeatured: marks.count == 1
                            )
                        }
                    }
                    .frame(width: marks.count == 1 ? 126 : 104)
                }
            }

            footer
        }
        .padding(SkydownLayout.heroPadding)
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
                                Color.white.opacity(colorScheme == .dark ? 0.08 : 0.42),
                                AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.98 : 0.95),
                                accent.opacity(colorScheme == .dark ? 0.16 : 0.10),
                                secondaryAccent.opacity(colorScheme == .dark ? 0.14 : 0.08),
                                Color.black.opacity(colorScheme == .dark ? 0.08 : 0.02)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
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
                                    Color.black.opacity(colorScheme == .dark ? 0.28 : 0.18),
                                    accent.opacity(colorScheme == .dark ? 0.20 : 0.12),
                                    secondaryAccent.opacity(colorScheme == .dark ? 0.16 : 0.10)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )

                    heroShape
                        .fill(Color.black.opacity(colorScheme == .dark ? 0.20 : 0.12))
                }

                Circle()
                    .fill(accent.opacity(colorScheme == .dark ? 0.18 : 0.08))
                    .frame(width: 220, height: 220)
                    .blur(radius: 54)
                    .offset(x: 136, y: -82)

                Circle()
                    .fill(secondaryAccent.opacity(colorScheme == .dark ? 0.16 : 0.08))
                    .frame(width: 176, height: 176)
                    .blur(radius: 48)
                    .offset(x: -118, y: 108)
            }
        }
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: accent,
            cornerRadius: SkydownLayout.heroCornerRadius,
            shadowRadius: 20,
            shadowYOffset: 10
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
                            Color.white.opacity(colorScheme == .dark ? 0.10 : 0.18),
                            accent.opacity(colorScheme == .dark ? 0.08 : 0.12),
                            Color.white.opacity(0)
                        ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .frame(width: max(width * 0.34, 120), height: height * 1.7)
                .rotationEffect(.degrees(16))
                .offset(x: sheenTravel * width, y: -height * 0.16)
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
                            accent.opacity(0.22),
                            secondaryAccent.opacity(0.18),
                            Color.white.opacity(colorScheme == .dark ? 0.10 : 0.12)
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
                .opacity(0.92)
        }
        .onAppear {
            guard sheenTravel < 0 else { return }
            withAnimation(.linear(duration: 4.4).repeatForever(autoreverses: false)) {
                sheenTravel = 1.24
            }
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
        VStack(alignment: .leading, spacing: 10) {
            Image(mark.imageName)
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity)
                .frame(height: isFeatured ? 88 : 58)
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous))

            Text(mark.label)
                .font(isFeatured ? AppTypography.editorialCaption : AppTypography.editorialFootnote)
                .tracking(0.5)
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.88))
                .lineLimit(1)
        }
        .padding(isFeatured ? 13 : 11)
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
            color: accent.opacity(colorScheme == .dark ? 0.16 : 0.08),
            radius: 12,
            y: 6
        )
    }
}
