import SwiftUI

enum BrandMark: String, Identifiable {
    case skydown = "SkydownBrandLogo"
    case zweizwei = "ZweizweiBrandLogo"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .skydown:
            return "Skydown"
        case .zweizwei:
            return "Zweizwei"
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
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 16) {
                VStack(alignment: .leading, spacing: 10) {
                    Text(eyebrow.uppercased())
                        .font(.system(size: 12, weight: .semibold, design: .rounded))
                        .tracking(1.4)
                        .foregroundColor(accent)

                    Text(title)
                        .font(.system(size: 34, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(subtitle)
                        .font(.headline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if let detail, !detail.isEmpty {
                        Text(detail)
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme).opacity(0.86))
                    }
                }

                if !marks.isEmpty {
                    VStack(spacing: 10) {
                        ForEach(marks.prefix(2)) { mark in
                            BrandMarkTile(
                                mark: mark,
                                colorScheme: colorScheme,
                                accent: accent
                            )
                        }
                    }
                    .frame(width: 112)
                }
            }

            footer
        }
        .padding(SkydownLayout.heroPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            ZStack {
                RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                    .fill(AppColors.cardBackground(for: colorScheme))

                RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                accent.opacity(colorScheme == .dark ? 0.22 : 0.16),
                                secondaryAccent.opacity(colorScheme == .dark ? 0.14 : 0.10),
                                Color.clear
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )

                Circle()
                    .fill(accent.opacity(colorScheme == .dark ? 0.18 : 0.12))
                    .frame(width: 220, height: 220)
                    .blur(radius: 50)
                    .offset(x: 140, y: -70)

                Circle()
                    .fill(secondaryAccent.opacity(colorScheme == .dark ? 0.16 : 0.10))
                    .frame(width: 180, height: 180)
                    .blur(radius: 44)
                    .offset(x: -120, y: 120)
            }
        }
        .overlay {
            RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [
                            accent.opacity(0.26),
                            secondaryAccent.opacity(0.18),
                            Color.white.opacity(colorScheme == .dark ? 0.08 : 0.48)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1
                )
        }
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.30 : 0.10), radius: 22, y: 10)
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
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                Capsule(style: .continuous)
                    .fill(tint.opacity(colorScheme == .dark ? 0.18 : 0.12))
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(tint.opacity(0.16), lineWidth: 1)
            )
    }
}

private struct BrandMarkTile: View {
    let mark: BrandMark
    let colorScheme: ColorScheme
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(mark.rawValue)
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity)
                .frame(height: 64)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            Text(mark.label)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
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
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(accent.opacity(0.20), lineWidth: 1)
        )
    }
}
