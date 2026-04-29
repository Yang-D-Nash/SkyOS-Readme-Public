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
        SkydownPortalChip(
            title: utility.title,
            systemImage: utility.icon,
            tint: utilityTint(for: utility.kind),
            colorScheme: colorScheme,
            action: utility.action
        )
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
    @ObservedObject private var artistPagesStore = ArtistPagesStore.shared

    private struct ArtistSocialEntry: Identifiable {
        let id: String
        let title: String
        let subtitle: String?
        let instagramURL: String
        let spotifyURL: String?
    }

    private var entries: [ArtistSocialEntry] {
        let yangPage = artistPagesStore.page(for: .zweizwei, artistName: "Yang D. Nash")
        let skydownPage = artistPagesStore.page(for: .skydown, artistName: "Skydown")
        let zweizweiPage = artistPagesStore.page(for: .zweizwei, artistName: "JANNO")

        return [
            ArtistSocialEntry(
                id: "yang-d-nash",
                title: "Yang D. Nash",
                subtitle: "Inhaber / Betreiber",
                instagramURL: resolvedURL(
                    yangPage.instagramURL,
                    fallback: "https://www.instagram.com/y.d.nash/"
                ),
                spotifyURL: resolvedOptionalURL(
                    yangPage.spotifyURL,
                    fallback: "https://open.spotify.com/search/Yang%20D.%20Nash"
                )
            ),
            ArtistSocialEntry(
                id: "skydown",
                title: "Skydown",
                subtitle: nil,
                instagramURL: resolvedURL(
                    skydownPage.instagramURL,
                    fallback: "https://www.instagram.com/skydown_entertainment/"
                ),
                spotifyURL: nil
            ),
            ArtistSocialEntry(
                id: "zweizwei",
                title: "Zweizwei",
                subtitle: nil,
                instagramURL: resolvedURL(
                    zweizweiPage.instagramURL,
                    fallback: "https://www.instagram.com/zweizwei_music/"
                ),
                spotifyURL: nil
            )
        ]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                ForEach(entries) { entry in
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
                        if entry.id == "yang-d-nash" {
                            Text(entry.title)
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.text(for: colorScheme))
                            if let subtitle = entry.subtitle {
                                Text(subtitle)
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }

                            HStack(spacing: SkydownLayout.stackSpacingPill) {
                                artistPageButton(
                                    title: "Artist Page",
                                    icon: "person.crop.circle",
                                    tint: AppColors.accent(for: colorScheme)
                                ) {
                                    onOpenArtistPage(entry.title)
                                }
                                socialButton(
                                    title: AppLocalized.text("home.artist_links.instagram", fallback: "Instagram"),
                                    icon: "camera.circle.fill",
                                    urlString: entry.instagramURL,
                                    tint: AppColors.accentHighlight(for: colorScheme)
                                )
                                if let spotifyURL = entry.spotifyURL {
                                    socialButton(
                                        title: AppLocalized.text("home.artist_links.spotify", fallback: "Spotify"),
                                        icon: "music.note",
                                        urlString: spotifyURL,
                                        tint: AppColors.spotify(for: colorScheme)
                                    )
                                }
                            }
                        } else {
                            socialButton(
                                title: "\(entry.title) · Instagram",
                                icon: "camera.circle.fill",
                                urlString: entry.instagramURL,
                                tint: AppColors.accentHighlight(for: colorScheme)
                            )
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
                        .font(.caption2.weight(.semibold))
                    Text(title)
                        .font(.caption2.weight(.bold))
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 7)
                .padding(.horizontal, 9)
                .background(
                    Capsule(style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [
                                    AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.78 : 0.66),
                                    tint.opacity(colorScheme == .dark ? 0.22 : 0.14),
                                    tint.opacity(colorScheme == .dark ? 0.12 : 0.08)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                )
                .overlay(
                    Capsule(style: .continuous)
                        .stroke(tint.opacity(0.30), lineWidth: 1)
                )
                .overlay(
                    Capsule(style: .continuous)
                        .stroke(
                            LinearGradient(
                                colors: [
                                    .white.opacity(colorScheme == .dark ? 0.16 : 0.28),
                                    tint.opacity(0.18)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.8
                        )
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
                    .font(.caption2.weight(.semibold))
                Text(title)
                    .font(.caption2.weight(.bold))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 7)
            .padding(.horizontal, 9)
            .background(
                Capsule(style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.78 : 0.66),
                                tint.opacity(colorScheme == .dark ? 0.22 : 0.14),
                                tint.opacity(colorScheme == .dark ? 0.12 : 0.08)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(tint.opacity(0.30), lineWidth: 1)
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(
                        LinearGradient(
                            colors: [
                                .white.opacity(colorScheme == .dark ? 0.16 : 0.28),
                                tint.opacity(0.18)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 0.8
                    )
            )
        }
        .buttonStyle(.plain)
        .foregroundColor(tint)
    }

    private func resolvedURL(_ value: String?, fallback: String) -> String {
        let normalized = value?.trimmingCharacters(in: .whitespacesAndNewlines)
        return (normalized?.isEmpty == false ? normalized : nil) ?? fallback
    }

    private func resolvedOptionalURL(_ value: String?, fallback: String?) -> String? {
        if let value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return value.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return fallback
    }

}
