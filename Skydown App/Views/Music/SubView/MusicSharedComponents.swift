//
//  MusicSharedComponents.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import SwiftUI
import UIKit

let zweizweiCanonicalArtists = [
    "Janno",
    "Mave",
    "Tangajoe007",
    "Yang D. Nash",
    "ThaDude"
]

func musicArtistKey(_ artist: String) -> String {
    artist
        .trimmingCharacters(in: .whitespacesAndNewlines)
        .lowercased()
        .components(separatedBy: CharacterSet.alphanumerics.inverted)
        .joined()
}

func mergeZweizweiArtists(_ liveArtists: [String]) -> [String] {
    var merged: [String: String] = [:]
    var order: [String] = []

    for artist in zweizweiCanonicalArtists {
        let key = musicArtistKey(artist)
        if merged[key] == nil {
            order.append(key)
        }
        merged[key] = artist
    }

    for artist in liveArtists {
        let trimmed = artist.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { continue }
        let key = musicArtistKey(trimmed)
        if merged[key] == nil {
            order.append(key)
            merged[key] = trimmed
        }
    }

    return order.compactMap { merged[$0] }
}

struct MusicBadge: View {
    let text: String
    let isAccent: Bool
    var accentOverride: Color? = nil
    var accessibilityIdentifier: String? = nil
    var onTap: () -> Void = {}
    @Environment(\.colorScheme) private var colorScheme

    private var resolvedAccent: Color {
        accentOverride ?? AppColors.accent(for: colorScheme)
    }

    var body: some View {
        Group {
            if let id = accessibilityIdentifier {
                baseButton
                    .accessibilityIdentifier(id)
            } else {
                baseButton
            }
        }
    }

    private var baseButton: some View {
        Button(action: onTap) {
            badgeContent
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private var badgeContent: some View {
        HStack(spacing: SkydownLayout.stackSpacingSubtle) {
            Text(text)
                .font(.caption.weight(.semibold))
            Image(systemName: "arrow.right")
                .font(.caption2.weight(.bold))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(
            isAccent
            ? resolvedAccent.opacity(colorScheme == .dark ? 0.13 : 0.10)
            : AppColors.secondaryBackground(for: colorScheme)
        )
        .foregroundColor(
            isAccent
            ? resolvedAccent
            : AppColors.secondaryText(for: colorScheme)
        )
        .clipShape(Capsule())
    }
}

struct MusicInstagramHubCard: View {
    let selectedArtist: String
    let destinations: [MusicInstagramDestination]
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            Text("Instagram")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Community- und Brand-Profile auf einen Blick.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ForEach(destinations) { destination in
                if let url = destination.url {
                    Link(destination: url) {
                        HStack(spacing: SkydownLayout.stackSpacingCompact) {
                            ZStack {
                                Circle()
                                    .fill(
                                        LinearGradient(
                                            colors: [
                                                AppColors.instagramStart(for: colorScheme),
                                                AppColors.instagramEnd(for: colorScheme)
                                            ],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(width: 42, height: 42)

                                Image(systemName: "camera.fill")
                                    .font(.footnote.weight(.bold))
                                    .foregroundColor(.white)
                            }

                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                                Text(destination.title)
                                    .font(.headline)
                                    .foregroundColor(AppColors.text(for: colorScheme))
                                    .frame(maxWidth: .infinity, alignment: .leading)

                                Text(destination.subtitle(selectedArtist: selectedArtist))
                                    .font(.subheadline)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            LinearGradient(
                                colors: [
                                    AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.96 : 0.99),
                                    AppColors.instagramStart(for: colorScheme).opacity(colorScheme == .dark ? 0.16 : 0.10),
                                    AppColors.instagramEnd(for: colorScheme).opacity(colorScheme == .dark ? 0.12 : 0.08)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                                .stroke(AppColors.instagramStart(for: colorScheme).opacity(0.22), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                }
            }
        }
        .padding(SkydownLayout.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(AppColors.instagramStart(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
    }
}

struct NicmaProducerPriceCard: View {
    let package: NicmaProducerPackage
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            HStack(alignment: .top) {
                Text(package.title)
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Spacer()

                Text(package.price)
                    .font(.headline.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }

            Text(package.detail)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
    }
}

struct NicmaUploadField: View {
    let title: String
    @Binding var text: String
    let colorScheme: ColorScheme
    var keyboard: UIKeyboardType = .default
    var autocapitalization: TextInputAutocapitalization = .sentences

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(title, text: $text)
                .keyboardType(keyboard)
                .textInputAutocapitalization(autocapitalization)
                .padding(.horizontal, 14)
                .padding(.vertical, 14)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                )
        }
    }
}

struct NicmaSelectedFileRow: View {
    let file: NicmaSelectedFile
    let colorScheme: ColorScheme
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: SkydownLayout.stackSpacingCompact) {
            ZStack {
                Circle()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 42, height: 42)

                Image(systemName: "waveform")
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                Text(file.fileName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)

                Text(ByteCountFormatter.string(fromByteCount: file.fileSizeInBytes, countStyle: .file))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer()

            Button(role: .destructive, action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title3)
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
    }
}

struct NicmaProducerPackage: Identifiable {
    let id = UUID()
    let title: String
    let detail: String
    let price: String
}

private struct SkydownKeyboardDismissAccessory: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        SkydownBrandActionButton(
            title: AppLocalized.text("common.done", fallback: "Fertig"),
            accent: AppColors.accent(for: colorScheme),
            colorScheme: colorScheme,
            role: .muted,
            font: .subheadline.weight(.semibold),
            cornerRadius: SkydownLayout.denseRadius,
            verticalPadding: 8,
            expandToFullWidth: false,
            action: { UIApplication.shared.skydownDismissKeyboard() }
        )
        .skydownInteractiveFeedback()
    }
}

extension View {
    func skydownPremiumInputSurface() -> some View {
        self
            .skydownKeyboardDismissToolbar()
            .skydownDismissKeyboardOnTap()
    }

    func skydownKeyboardDismissToolbar() -> some View {
        toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                SkydownKeyboardDismissAccessory()
            }
        }
    }

    func skydownDismissKeyboardOnTap() -> some View {
        simultaneousGesture(
            TapGesture().onEnded {
                UIApplication.shared.skydownDismissKeyboard()
            }
        )
    }
}

private extension UIApplication {
    func skydownDismissKeyboard() {
        sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

struct MusicInstagramDestination: Identifiable {
    let id: String
    let title: String
    let handle: String
    let urlString: String
    let helper: String?
    let spotifyURLString: String?
    let artistPageName: String?

    var url: URL? {
        URL(string: urlString)
    }

    var spotifyURL: URL? {
        if let spotifyURLString, let explicitURL = URL(string: spotifyURLString) {
            return explicitURL
        }
        let query = title.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? title
        return URL(string: "https://open.spotify.com/search/\(query)")
    }

    func subtitle(selectedArtist: String) -> String {
        if let helper {
            return "\(handle) • \(helper)"
        }

        if title == selectedArtist {
            return "\(handle) • Artist aktuell ausgewaehlt"
        }

        return handle
    }
}

let artistInstagramDestinations: [String: MusicInstagramDestination] = [
    musicArtistKey("Yang D. Nash"): MusicInstagramDestination(
        id: "artist_yang_d_nash",
        title: "Yang D. Nash",
        handle: "@y.d.nash",
        urlString: "https://www.instagram.com/y.d.nash/",
        helper: "Artist aktuell ausgewaehlt",
        spotifyURLString: "https://open.spotify.com/artist/63Sh0kQAWW3ZWn2aKDksbo",
        artistPageName: "Yang D. Nash"
    ),
    musicArtistKey("Tangajoe007"): MusicInstagramDestination(
        id: "artist_tangajoe007",
        title: "Tangajoe007",
        handle: "@tangajoe007",
        urlString: "https://www.instagram.com/tangajoe007/",
        helper: "Artist aktuell ausgewaehlt",
        spotifyURLString: "https://open.spotify.com/artist/0OA5dgpVdwzI8K82m8FPxN",
        artistPageName: "Tangajoe007"
    ),
    musicArtistKey("Janno"): MusicInstagramDestination(
        id: "artist_janno",
        title: "Janno",
        handle: "@janno_official_",
        urlString: "https://www.instagram.com/janno_official_/",
        helper: "Artist aktuell ausgewaehlt",
        spotifyURLString: "https://open.spotify.com/artist/7hpiHzP9aLLb5liDLxtwhM",
        artistPageName: "Janno"
    ),
    musicArtistKey("Mave"): MusicInstagramDestination(
        id: "artist_mave",
        title: "Mave",
        handle: "@mave040_official",
        urlString: "https://www.instagram.com/mave040_official/",
        helper: "Artist aktuell ausgewaehlt",
        spotifyURLString: "https://open.spotify.com/artist/0GXymtRaIk2ngbXSkcHtsp",
        artistPageName: "Mave"
    ),
    musicArtistKey("ThaDude"): MusicInstagramDestination(
        id: "artist_thadude",
        title: "ThaDude",
        handle: "@thadude_offizielle",
        urlString: "https://www.instagram.com/thadude_offizielle/",
        helper: "Artist aktuell ausgewaehlt",
        spotifyURLString: "https://open.spotify.com/artist/0Jmb7DXFkKxxRjqD70vi0e",
        artistPageName: "ThaDude"
    )
]

let skydownMusicInstagramDestination = MusicInstagramDestination(
    id: "brand_skydown",
    title: "Skydown",
    handle: "@skydown_entertainment",
    urlString: "https://www.instagram.com/skydown_entertainment/",
    helper: "Label und Releases",
    spotifyURLString: nil,
    artistPageName: nil
)

let nicmaInstagramDestination = MusicInstagramDestination(
    id: "brand_nicma_music",
    title: "NICMA MUSIC",
    handle: "@nicma.music",
    urlString: "https://www.instagram.com/nicma.music/",
    helper: "Studio",
    spotifyURLString: "https://open.spotify.com/artist/1hY6W4D4P67f0cKkcUFoUi",
    artistPageName: "NICMA MUSIC"
)

let nicmaProducerPackages: [NicmaProducerPackage] = [
    NicmaProducerPackage(
        title: "Mixing",
        detail: "max. 24 Audio Files",
        price: "150 €"
    ),
    NicmaProducerPackage(
        title: "Mastering",
        detail: "2 stems",
        price: "70 €"
    ),
    NicmaProducerPackage(
        title: "Mastering",
        detail: "max. 5 stems",
        price: "90 €"
    ),
    NicmaProducerPackage(
        title: "Mixing + Mastering",
        detail: "max. 24 Audio Files",
        price: "200 €"
    ),
    NicmaProducerPackage(
        title: "Recording (ohne Mix/Master)",
        detail: "Session",
        price: "120 €"
    ),
    NicmaProducerPackage(
        title: "Recording + Mix/Master",
        detail: "Komplett",
        price: "250 €"
    ),
    NicmaProducerPackage(
        title: "8h Studio + Engineer",
        detail: "+ Nachbearbeitung",
        price: "400 € + Nachbearbeitung"
    )
]
