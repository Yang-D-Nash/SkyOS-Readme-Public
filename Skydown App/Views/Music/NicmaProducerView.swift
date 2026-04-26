//
//  NicmaProducerView.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import SwiftUI

struct NicmaProducerView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @EnvironmentObject private var authManager: AuthManager
    @ObservedObject private var artistPagesStore = ArtistPagesStore.shared
    let onBack: (() -> Void)?
    @State private var showingEditor = false
    @State private var selectedProfile = "NICMA MUSIC"
    @State private var editingProfile: String?
    @State private var viewingProfile: String?

    init(onBack: (() -> Void)? = nil) {
        self.onBack = onBack
    }

    private var page: ArtistPage {
        artistPagesStore.page(for: .nicma, artistName: selectedProfile)
    }

    private var canEdit: Bool {
        artistPagesStore.canEdit(page, user: authManager.userSession)
    }
    
    private var isStudioProfile: Bool {
        selectedProfile == "NICMA STUDIO"
    }
    
    private var profileFallbackBio: String {
        isStudioProfile
            ? "Studio Page: Preise, Production und Recording."
            : "Artist Page: NICMA MUSIC, Links und Profil."
    }

    private var nicmaInstagramURL: URL? {
        let fallbackInstagramURL = nicmaInstagramDestination.urlString
        let resolvedURL = (page.instagramURL ?? fallbackInstagramURL).trimmingCharacters(in: .whitespacesAndNewlines)
        guard !resolvedURL.isEmpty else { return nil }
        return URL(string: resolvedURL)
    }

    private var nicmaLinks: [NicmaPageLink] {
        var links: [NicmaPageLink] = []

        let instagramURL = page.instagramURL ?? nicmaInstagramDestination.urlString
        if !instagramURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            links.append(
                NicmaPageLink(
                    title: "Instagram",
                    subtitle: "@nicma.music",
                    url: instagramURL,
                    systemImage: "camera.fill",
                    tint: AppColors.instagramStart(for: colorScheme)
                )
            )
        }

        if let spotifyURL = page.spotifyURL?.trimmingCharacters(in: .whitespacesAndNewlines), !spotifyURL.isEmpty {
            links.append(
                NicmaPageLink(
                    title: "Spotify",
                    subtitle: "Artist · Producer",
                    url: spotifyURL,
                    systemImage: "music.note",
                    tint: AppColors.spotify(for: colorScheme)
                )
            )
        }

        if let youtubeURL = page.youtubeURL?.trimmingCharacters(in: .whitespacesAndNewlines), !youtubeURL.isEmpty {
            links.append(
                NicmaPageLink(
                    title: "YouTube",
                    subtitle: "Videos · Sessions",
                    url: youtubeURL,
                    systemImage: "play.rectangle.fill",
                    tint: AppColors.youtube(for: colorScheme)
                )
            )
        }

        return links
    }

    private var resolvedPriceList: [NicmaProducerPackage] {
        if !page.studioPriceList.isEmpty {
            return page.studioPriceList.map {
                NicmaProducerPackage(title: $0.title, detail: $0.detail, price: $0.price)
            }
        }
        return nicmaProducerPackages
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                heroCard
                seiteInfoCard
                pricingCard
                contactCard
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
        .navigationTitle(selectedProfile)
        .navigationBarTitleDisplayMode(.inline)
        .skydownNavigationChrome(colorScheme: colorScheme)
        .toolbar {
            if let onBack {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.headline.weight(.bold))
                    }
                }
            }

            if canEdit {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        editingProfile = selectedProfile
                    } label: {
                        Image(systemName: "square.and.pencil")
                            .font(.headline.weight(.semibold))
                    }
                    .accessibilityLabel("Bearbeiten")
                }
            }
        }
        .sheet(item: $viewingProfile) { profile in
            ArtistPageView(
                authManager: authManager,
                store: artistPagesStore,
                brand: .nicma,
                artistName: profile
            )
        }
        .sheet(item: $editingProfile) { profile in
            ArtistPageView(
                authManager: authManager,
                store: artistPagesStore,
                brand: .nicma,
                artistName: profile
            )
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                nicmaProfileSwitchButton(title: "NICMA MUSIC")
                nicmaProfileSwitchButton(title: "NICMA STUDIO")
            }
            Text(page.artistName)
                .font(.largeTitle.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(page.bio ?? profileFallbackBio)
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 8) {
                if isStudioProfile {
                    MusicBadge(text: "Studio Services", isAccent: true)
                    MusicBadge(text: "Mix & Master", isAccent: false)
                    MusicBadge(text: "Recording", isAccent: false)
                } else {
                    MusicBadge(text: "Artist Page", isAccent: true)
                    MusicBadge(text: "Katalog", isAccent: false)
                    MusicBadge(text: "Links", isAccent: false)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 26)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .contentShape(RoundedRectangle(cornerRadius: 26))
        .onTapGesture {
            viewingProfile = selectedProfile
        }
    }

    private var seiteInfoCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(isStudioProfile ? "NICMA STUDIO Seite" : "NICMA MUSIC Seite")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(
                page.tagline
                    ?? (isStudioProfile
                        ? "Studio-Profil, Preisliste, Production und Links."
                        : "Artist Profil und Links.")
            )
            .font(.subheadline)
            .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(SkydownLayout.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
    }

    private var pricingCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Preisliste")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            ForEach(resolvedPriceList) { package in
                NicmaProducerPriceCard(
                    package: package,
                    colorScheme: colorScheme
                )
            }
        }
        .padding(SkydownLayout.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    private var contactCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Links")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(page.tagline ?? "Kontakt & Plattformen.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ForEach(nicmaLinks) { link in
                Button {
                    if let url = URL(string: link.url) {
                        openURL(url)
                    }
                } label: {
                    Label(link.title, systemImage: link.systemImage)
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .foregroundColor(AppColors.text(for: colorScheme))
                .background(AppColors.secondaryBackground(for: colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(link.tint.opacity(0.18), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 18))
            }
        }
        .padding(SkydownLayout.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
    }

    private func nicmaProfileSwitchButton(title: String) -> some View {
        Button {
            selectedProfile = title
        } label: {
            Text(selectedProfile == title ? "\(title) aktiv" : title)
                .font(.caption.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
        }
        .buttonStyle(.borderedProminent)
        .tint(selectedProfile == title ? AppColors.accent(for: colorScheme) : AppColors.secondaryBackground(for: colorScheme))
        .foregroundColor(selectedProfile == title ? .white : AppColors.text(for: colorScheme))
    }

}

extension String: @retroactive Identifiable {
    public var id: String { self }
}

private struct NicmaPageLink: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let url: String
    let systemImage: String
    let tint: Color
}
