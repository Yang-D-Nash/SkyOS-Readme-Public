import SwiftUI

struct HomeUtilityRow: View {
    let colorScheme: ColorScheme
    let onOpenMusic: () -> Void
    let onOpenVideo: () -> Void
    let onOpenMerch: () -> Void
    let onOpenSettings: () -> Void

    private enum UtilityKind: Hashable { case music, video, merch, settings }

    private struct UtilityItem {
        let kind: UtilityKind
        let title: String
        let icon: String
        let action: () -> Void
    }

    private var utilities: [UtilityItem] {
        [
            UtilityItem(
                kind: .music,
                title: AppLocalized.text("home.utility.music", fallback: "Music"),
                icon: "music.note",
                action: onOpenMusic
            ),
            UtilityItem(
                kind: .video,
                title: AppLocalized.text("home.utility.videos", fallback: "Videos"),
                icon: "play.rectangle",
                action: onOpenVideo
            ),
            UtilityItem(
                kind: .merch,
                title: AppLocalized.text("home.utility.merch", fallback: "Merch"),
                icon: "bag",
                action: onOpenMerch
            ),
            UtilityItem(
                kind: .settings,
                title: AppLocalized.text("home.utility.settings", fallback: "Settings"),
                icon: "gearshape",
                action: onOpenSettings
            )
        ]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            Text(
                AppLocalized.text("home.explore.title", fallback: "Portals")
            )
            .font(.caption2.weight(.medium))
            .foregroundColor(AppColors.text(for: colorScheme).opacity(0.44))
            .textCase(.uppercase)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    ForEach(Array(utilities.enumerated()), id: \.offset) { _, utility in
                        utilityButton(utility)
                    }
                }
            }
        }
        .padding(.top, 4)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func utilityButton(_ utility: UtilityItem) -> some View {
        let tint = utilityTint(for: utility.kind)
        Button {
            SkydownHaptics.selection()
            utility.action()
        } label: {
            HStack(spacing: SkydownLayout.stackSpacingDense) {
                Image(systemName: utility.icon)
                    .font(.caption.weight(.medium))
                Text(utility.title)
                    .font(.caption.weight(.semibold))
            }
            .padding(.horizontal, 13)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                    .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.58))
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                    .stroke(tint.opacity(0.16), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .foregroundColor(tint)
    }

    private func utilityTint(for kind: UtilityKind) -> Color {
        switch kind {
        case .music:
            return AppColors.accent(for: colorScheme).opacity(0.94)
        case .video:
            return AppColors.accentHighlight(for: colorScheme).opacity(0.94)
        case .merch:
            return AppColors.accentMystic(for: colorScheme).opacity(0.94)
        case .settings:
            return AppColors.text(for: colorScheme).opacity(0.76)
        }
    }
}

private struct HomeRevealModifier: ViewModifier {
    let order: Int
    @State private var isVisible = false

    func body(content: Content) -> some View {
        content
            .opacity(isVisible ? 1 : 0)
            .offset(y: isVisible ? 0 : 10)
            .animation(
                SkydownMotion.contentReveal.delay(Double(order) * SkydownMotion.listStaggerDelay),
                value: isVisible
            )
            .onAppear { isVisible = true }
    }
}

extension View {
    func homeReveal(_ order: Int) -> some View {
        modifier(HomeRevealModifier(order: order))
    }
}

struct HomeArtistSocialLinksRow: View {
    let colorScheme: ColorScheme
    let onOpenArtistPage: (String) -> Void

    private struct ArtistSocialEntry: Identifiable {
        let id: String
        let title: String
        let subtitle: String?
        let instagramURL: String
        let spotifyURL: String?
    }

    private let entries: [ArtistSocialEntry] = [
        ArtistSocialEntry(
            id: "yang-d-nash",
            title: "Yang D. Nash",
            subtitle: "Inhaber / Betreiber",
            instagramURL: "https://www.instagram.com/y.d.nash/",
            spotifyURL: nil
        ),
        ArtistSocialEntry(
            id: "zweizwei",
            title: "Zweizwei",
            subtitle: nil,
            instagramURL: "https://www.instagram.com/zweizwei_music/",
            spotifyURL: nil
        ),
        ArtistSocialEntry(
            id: "skydown",
            title: "Skydown",
            subtitle: nil,
            instagramURL: "https://www.instagram.com/skydown_entertainment/",
            spotifyURL: nil
        )
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            Text(AppLocalized.text("home.artist_links.title", fallback: "Artist links"))
                .font(.caption2.weight(.medium))
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.44))
                .textCase(.uppercase)

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                ForEach(entries) { entry in
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
                        Text(entry.title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                        if let subtitle = entry.subtitle {
                            Text(subtitle)
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }

                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            socialButton(
                                title: AppLocalized.text("home.artist_links.instagram", fallback: "Instagram"),
                                icon: "camera.circle.fill",
                                urlString: entry.instagramURL,
                                tint: AppColors.accentHighlight(for: colorScheme)
                            )
                            artistPageButton(
                                title: "Artist Page",
                                icon: "person.crop.circle",
                                tint: AppColors.accent(for: colorScheme)
                            ) {
                                onOpenArtistPage(entry.title)
                            }
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func socialButton(title: String, icon: String, urlString: String, tint: Color) -> some View {
        if let url = URL(string: urlString) {
            Link(destination: url) {
                HStack(spacing: SkydownLayout.stackSpacingTick) {
                    Image(systemName: icon)
                        .font(.caption.weight(.semibold))
                    Text(title)
                        .font(.caption.weight(.semibold))
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .padding(.horizontal, 12)
                .background(
                    RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                        .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.58))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                        .stroke(tint.opacity(0.18), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .foregroundColor(tint)
        }
    }

    private func artistPageButton(title: String, icon: String, tint: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: SkydownLayout.stackSpacingTick) {
                Image(systemName: icon)
                    .font(.caption.weight(.semibold))
                Text(title)
                    .font(.caption.weight(.semibold))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .padding(.horizontal, 12)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                    .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.58))
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                    .stroke(tint.opacity(0.18), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .foregroundColor(tint)
    }

}
