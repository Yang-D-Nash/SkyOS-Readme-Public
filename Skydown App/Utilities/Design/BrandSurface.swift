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
            return "22xSky"
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
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(eyebrow.uppercased())
                        .font(AppTypography.heroEyebrow)
                        .tracking(1.4)
                        .foregroundColor(accent)

                    Text(title)
                        .font(AppTypography.heroTitle)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(subtitle)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if let detail, !detail.isEmpty {
                        Text(detail)
                            .font(.footnote.weight(.semibold))
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
                    .frame(width: marks.count == 1 ? 118 : 96)
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
                                AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.98 : 0.94),
                                accent.opacity(colorScheme == .dark ? 0.10 : 0.06),
                                secondaryAccent.opacity(colorScheme == .dark ? 0.08 : 0.05),
                                Color.black.opacity(colorScheme == .dark ? 0.16 : 0.03)
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
                                    Color.black.opacity(colorScheme == .dark ? 0.46 : 0.26),
                                    accent.opacity(colorScheme == .dark ? 0.14 : 0.09),
                                    secondaryAccent.opacity(colorScheme == .dark ? 0.10 : 0.07)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )

                    heroShape
                        .fill(Color.black.opacity(colorScheme == .dark ? 0.34 : 0.18))
                }

                Circle()
                    .fill(accent.opacity(colorScheme == .dark ? 0.10 : 0.05))
                    .frame(width: 190, height: 190)
                    .blur(radius: 44)
                    .offset(x: 130, y: -74)

                Circle()
                    .fill(secondaryAccent.opacity(colorScheme == .dark ? 0.08 : 0.05))
                    .frame(width: 150, height: 150)
                    .blur(radius: 42)
                    .offset(x: -110, y: 102)
            }
        }
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: accent,
            cornerRadius: SkydownLayout.heroCornerRadius,
            shadowRadius: 16,
            shadowYOffset: 8
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
                            Color.white.opacity(colorScheme == .dark ? 0.06 : 0.12)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1
                )
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

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundColor(tint)
            .padding(.horizontal, 11)
            .padding(.vertical, 7)
            .skydownCapsuleSurface(colorScheme: colorScheme, accent: tint)
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
                .frame(height: isFeatured ? 84 : 56)
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous))

            Text(mark.label)
                .font((isFeatured ? Font.caption.weight(.bold) : Font.caption.weight(.semibold)))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)
        }
        .padding(isFeatured ? 12 : 10)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color.black.opacity(colorScheme == .dark ? 0.34 : 0.74),
                            AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.80 : 0.96)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(accent.opacity(0.16), lineWidth: 1)
        )
    }
}
