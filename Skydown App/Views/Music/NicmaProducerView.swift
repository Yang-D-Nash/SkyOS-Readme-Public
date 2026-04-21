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

    init(onBack: (() -> Void)? = nil) {
        self.onBack = onBack
    }

    private var page: ArtistPage {
        artistPagesStore.page(for: .nicma, artistName: "NICMA MUSIC")
    }

    private var canEdit: Bool {
        artistPagesStore.canEdit(page, user: authManager.userSession)
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
                    subtitle: "Artist / Producer Profil",
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
                    subtitle: "Videos & Sessions",
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
                pricingCard
                contactCard
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
        .navigationTitle("NICMA MUSIC")
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
                    Button("Bearbeiten") {
                        showingEditor = true
                    }
                }
            }
        }
        .sheet(isPresented: $showingEditor) {
            ArtistPageView(
                authManager: authManager,
                store: artistPagesStore,
                brand: .nicma,
                artistName: "NICMA MUSIC"
            )
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(page.artistName)
                .font(.largeTitle.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(page.bio ?? "Mixing, Mastering und Recording mit klarer Preisliste, direktem Kontakt und sauberem Producer-Fokus.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 8) {
                MusicBadge(text: "Studio Services", isAccent: true)
                MusicBadge(text: "Mix & Master", isAccent: false)
                MusicBadge(text: "Recording", isAccent: false)
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
        .padding(18)
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

            Text(page.tagline ?? "Direkter Kontakt und oeffentliche Plattformen fuer NICMA.")
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
        .padding(18)
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
}

private struct NicmaPageLink: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let url: String
    let systemImage: String
    let tint: Color
}
