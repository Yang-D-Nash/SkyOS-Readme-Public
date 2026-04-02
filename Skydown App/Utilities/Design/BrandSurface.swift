import SwiftUI

enum BrandMark: String, Identifiable {
    case sky22 = "Sky22BrandLogo"
    case skydown = "SkydownBrandLogo"
    case zweizwei = "ZweizweiBrandLogo"
    case skydownX22 = "SkydownX22BrandLogo"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .sky22:
            return "22"
        case .skydown:
            return "Skydown"
        case .zweizwei:
            return "Zweizwei"
        case .skydownX22:
            return "Sky²²"
        }
    }
}

struct BrandHeroSurface<Footer: View>: View {
    let colorScheme: ColorScheme
    let eyebrow: String
    let title: String
    let subtitle: String
    let detail: String?
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
                        .font(.system(size: 12, weight: .semibold, design: .rounded))
                        .tracking(1.4)
                        .foregroundColor(accent)

                    Text(title)
                        .font(.system(size: 30, weight: .black, design: .rounded))
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
            ZStack {
                RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                accent.opacity(colorScheme == .dark ? 0.14 : 0.09),
                                secondaryAccent.opacity(colorScheme == .dark ? 0.10 : 0.08),
                                AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.04 : 0.58)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )

                Circle()
                    .fill(accent.opacity(colorScheme == .dark ? 0.14 : 0.08))
                    .frame(width: 190, height: 190)
                    .blur(radius: 44)
                    .offset(x: 130, y: -74)

                Circle()
                    .fill(secondaryAccent.opacity(colorScheme == .dark ? 0.12 : 0.08))
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
            Image(mark.rawValue)
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
