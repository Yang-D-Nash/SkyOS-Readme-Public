//
//  MusicView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI

enum MusicExperienceBrand {
    case skydown
    case zweizwei

    var navigationTitle: String {
        switch self {
        case .skydown:
            return "Music"
        case .zweizwei:
            return "Music"
        }
    }

    var heroTitle: String {
        navigationTitle
    }

    var heroSubtitle: String {
        switch self {
        case .skydown:
            return "Artists, Tracks, Spotify."
        case .zweizwei:
            return "Releases, Tracks, Beats."
        }
    }

    var artists: [String] {
        switch self {
        case .skydown:
            return ["Yang D. Nash", "ThaDude", "MAVE", "JANNO", "TANGAJOE007"]
        case .zweizwei:
            return ["JANNO", "Yang D. Nash", "ThaDude", "MAVE", "TANGAJOE007"]
        }
    }

    var fallbackArtistName: String {
        switch self {
        case .skydown:
            return "Skydown"
        case .zweizwei:
            return "22"
        }
    }

    var workflowTitle: String? {
        switch self {
        case .skydown:
            return nil
        case .zweizwei:
            return "Studio Services"
        }
    }

    var workflowSubtitle: String? {
        switch self {
        case .skydown:
            return nil
        case .zweizwei:
            return "Recording, Mix, Master."
        }
    }

    var showsBeatHubShortcut: Bool {
        switch self {
        case .skydown:
            return false
        case .zweizwei:
            return true
        }
    }

    var showsArtistPages: Bool {
        switch self {
        case .skydown:
            return false
        case .zweizwei:
            return true
        }
    }

    var artistPageBrand: ArtistPageBrand {
        switch self {
        case .skydown:
            return .skydown
        case .zweizwei:
            return .zweizwei
        }
    }
}

private enum MusicSectionAnchor: String {
    case artists
    case tracks
    case spotify
}

struct MusicView: View {
    @StateObject private var viewModel = MusicViewModel()
    @StateObject private var audioManager = AudioPlayerManager()
    @ObservedObject private var artistPagesStore = ArtistPagesStore.shared
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared
    @State private var selectedArtist: String
    @State private var selectedTrackID: Int?
    @State private var activePresentedSheet: MusicPresentedSheet?
    @State private var queuedPresentedSheet: MusicPresentedSheet?
    @State private var hasHandledInitialSelection = false
    @State private var hasAutoPresentedArtistPage = false
    @EnvironmentObject private var services: AppServices
    @Environment(\.colorScheme) private var colorScheme

    let brand: MusicExperienceBrand
    let onBack: (() -> Void)?
    let onOpenCart: (() -> Void)?
    let onOpenProfile: (() -> Void)?
    let onOpenSettings: (() -> Void)?
    private let initialArtist: String?
    private let initialTrackID: Int?
    private let autoplaySelectedTrackPreview: Bool
    private let autoPresentSelectedTrackSpotifyPlayer: Bool
    private let autoPresentArtistPageOnAppear: Bool

    init(
        brand: MusicExperienceBrand = .skydown,
        initialArtist: String? = nil,
        initialTrackID: Int? = nil,
        autoplaySelectedTrackPreview: Bool = false,
        autoPresentSelectedTrackSpotifyPlayer: Bool = false,
        autoPresentArtistPageOnAppear: Bool = false,
        onBack: (() -> Void)? = nil,
        onOpenCart: (() -> Void)? = nil,
        onOpenProfile: (() -> Void)? = nil,
        onOpenSettings: (() -> Void)? = nil
    ) {
        self.brand = brand
        self.onBack = onBack
        self.initialArtist = initialArtist
        self.initialTrackID = initialTrackID
        self.autoplaySelectedTrackPreview = autoplaySelectedTrackPreview
        self.autoPresentSelectedTrackSpotifyPlayer = autoPresentSelectedTrackSpotifyPlayer
        self.autoPresentArtistPageOnAppear = autoPresentArtistPageOnAppear
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        let resolvedInitialArtist = initialArtist.flatMap { requestedArtist in
            brand.artists.contains(requestedArtist) ? requestedArtist : nil
        } ?? brand.artists.first ?? "Yang D. Nash"
        _selectedArtist = State(initialValue: resolvedInitialArtist)
        _selectedTrackID = State(initialValue: initialTrackID)
    }

    private var artists: [String] {
        brand.artists
    }

    private var spotifyStatusText: String {
        if viewModel.isSpotifyConnected {
            return "Spotify ist verbunden. Du kannst Previews testen oder den Spotify Player direkt in der App oeffnen."
        }
        return "Previews laufen direkt in der App. Spotify ist optional, falls du kompatible Tracks zusaetzlich im In-App-Player oeffnen willst."
    }

    private var tracksStatusText: String {
        "\(viewModel.tracks.count) Titel fuer \(selectedArtist)"
    }

    private var selectedTrack: Track? {
        viewModel.tracks.first { $0.trackId == selectedTrackID } ?? viewModel.tracks.first
    }

    private var selectedArtistPage: ArtistPage {
        artistPagesStore.page(for: brand.artistPageBrand, artistName: selectedArtist)
    }

    private var trackIDs: [Int] {
        viewModel.tracks.map(\.trackId)
    }

    var body: some View {
        GeometryReader { proxy in
            let layout = SkydownResponsiveLayout(availableWidth: proxy.size.width)
            let contentWidth = min(
                layout.contentMaxWidth,
                max(proxy.size.width - (layout.horizontalPadding * 2), 0)
            )

            NavigationStack {
                ScrollViewReader { scrollProxy in
                    ScrollView {
                        VStack(alignment: .leading, spacing: layout.sectionSpacing) {
                            heroCard(
                                onOpenArtistHub: {
                                    withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                        scrollProxy.scrollTo(MusicSectionAnchor.artists.rawValue, anchor: .top)
                                    }
                                },
                                onOpenTracks: {
                                    withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                        scrollProxy.scrollTo(MusicSectionAnchor.tracks.rawValue, anchor: .top)
                                    }
                                },
                                onOpenSpotifyStatus: {
                                    withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                        scrollProxy.scrollTo(MusicSectionAnchor.spotify.rawValue, anchor: .top)
                                    }
                                }
                            )
                            spotlightCard

                            if layout.prefersTwoColumn {
                                HStack(alignment: .top, spacing: layout.sectionSpacing) {
                                    VStack(alignment: .leading, spacing: layout.sectionSpacing) {
                                        shortcutHubCard
                                        artistsCard
                                            .id(MusicSectionAnchor.artists.rawValue)
                                        instagramCard
                                    }
                                    .frame(maxWidth: .infinity, alignment: .topLeading)

                                    VStack(alignment: .leading, spacing: layout.sectionSpacing) {
                                        spotifyCard
                                            .id(MusicSectionAnchor.spotify.rawValue)
                                        musicPlayerCard
                                        tracksCard
                                            .id(MusicSectionAnchor.tracks.rawValue)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .topLeading)
                                }
                            } else {
                                shortcutHubCard
                                artistsCard
                                    .id(MusicSectionAnchor.artists.rawValue)
                                instagramCard
                                spotifyCard
                                    .id(MusicSectionAnchor.spotify.rawValue)
                                musicPlayerCard
                                tracksCard
                                    .id(MusicSectionAnchor.tracks.rawValue)
                            }
                        }
                        .frame(maxWidth: contentWidth, alignment: .leading)
                        .padding(.horizontal, layout.horizontalPadding)
                        .padding(.top, SkydownLayout.screenTopPadding)
                        .padding(.bottom, SkydownLayout.screenBottomPadding + (brand.showsBeatHubShortcut ? 52 : 0))
                        .frame(maxWidth: .infinity)
                    }
                    .accessibilityIdentifier("music.catalog.root")
                    .scrollIndicators(.hidden)
                    .background(AppColors.screenGradient(for: colorScheme).ignoresSafeArea())
                    .navigationTitle(brand.navigationTitle)
                    .navigationBarTitleDisplayMode(.inline)
                    .skydownNavigationChrome(colorScheme: colorScheme)
                    .overlay(alignment: .bottomTrailing) {
                        if brand.showsBeatHubShortcut {
                            NavigationLink {
                                BeatHubView()
                            } label: {
                                MusicShortcutFab(
                                    title: "Beat Hub",
                                    systemImage: "waveform.circle.fill",
                                    tint: AppColors.accent(for: colorScheme),
                                    textColor: .white
                                )
                            }
                            .buttonStyle(.plain)
                            .skydownTactileAction()
                            .padding(.trailing, layout.horizontalPadding)
                            .padding(.bottom, 20)
                        }
                    }
                }
                .toolbar {
                    if let onBack {
                        ToolbarItem(placement: .topBarLeading) {
                            Button(action: onBack) {
                                Image(systemName: "chevron.left")
                                    .font(.headline.weight(.bold))
                            }
                        }
                    }

                    if let onOpenSettings {
                        ToolbarItem(placement: .topBarTrailing) {
                            AppSessionToolbarActions(
                                onOpenCart: onOpenCart,
                                onOpenProfile: onOpenProfile,
                                onOpenSettings: onOpenSettings
                            )
                        }
                    } else if viewModel.isSpotifyConnected {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(role: .destructive) {
                                audioManager.stop()
                                viewModel.disconnectSpotify()
                            } label: {
                                Image(systemName: "rectangle.portrait.and.arrow.right")
                                    .font(.subheadline.weight(.bold))
                            }
                        }
                    }
                }
                .task(id: selectedArtist) {
                    await reloadTracksIfNeeded()
                }
                .onChange(of: trackIDs) {
                    guard !viewModel.tracks.isEmpty else {
                        selectedTrackID = nil
                        return
                    }

                    if selectedTrackID == nil || !viewModel.tracks.contains(where: { $0.trackId == selectedTrackID }) {
                        selectedTrackID = viewModel.tracks.first?.trackId
                    }

                    activateInitialSelectionIfNeeded()
                }
                .onChange(of: audioManager.currentlyPlayingId) { _, playingID in
                    if let playingID {
                        selectedTrackID = playingID
                    }
                }
                .onAppear {
                    activateInitialSelectionIfNeeded()
                    if autoPresentArtistPageOnAppear && brand.showsArtistPages && !hasAutoPresentedArtistPage {
                        presentSheet(.artistPage)
                        hasAutoPresentedArtistPage = true
                    }
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .sheet(item: $activePresentedSheet) { sheet in
            switch sheet {
            case .spotifyPlayer:
                if let selectedTrack {
                    SpotifyEmbedPlayerView(track: selectedTrack)
                }
            case .artistPage:
                ArtistPageView(
                    authManager: services.authManager,
                    store: artistPagesStore,
                    brand: brand.artistPageBrand,
                    artistName: selectedArtist
                )
            }
        }
        .onChange(of: activePresentedSheet) { _, sheet in
            guard sheet == nil, let queuedPresentedSheet else { return }
            self.queuedPresentedSheet = nil
            DispatchQueue.main.async {
                activePresentedSheet = queuedPresentedSheet
            }
        }
    }

    private func presentSheet(_ sheet: MusicPresentedSheet) {
        guard activePresentedSheet == nil else {
            queuedPresentedSheet = sheet
            activePresentedSheet = nil
            return
        }

        activePresentedSheet = sheet
    }

    private func heroCard(
        onOpenArtistHub: @escaping () -> Void,
        onOpenTracks: @escaping () -> Void,
        onOpenSpotifyStatus: @escaping () -> Void
    ) -> some View {
        BrandHeroSurface(
            colorScheme: colorScheme,
            eyebrow: screenHeaderSettingsStore.settings.resolvedMusicHubEyebrow ?? "Music",
            title: screenHeaderSettingsStore.settings.resolvedMusicHubTitle ?? brand.heroTitle,
            subtitle: screenHeaderSettingsStore.settings.resolvedMusicHubSubtitle ?? brand.heroSubtitle,
            detail: screenHeaderSettingsStore.settings.resolvedMusicHubDetail ?? "\(selectedArtist) und alle Artists direkt im Katalog.",
            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedMusicHubImageURL,
            accent: AppColors.spotify(for: colorScheme),
            secondaryAccent: AppColors.accent(for: colorScheme),
            marks: brand == .zweizwei ? [.zweizwei] : [.skydown]
        ) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    MusicBadge(text: selectedArtist, isAccent: true, onTap: onOpenArtistHub)
                    MusicBadge(text: tracksStatusText, isAccent: false, onTap: onOpenTracks)
                    if brand.showsArtistPages {
                        MusicBadge(text: "Artist Pages", isAccent: false, onTap: {
                            presentSheet(.artistPage)
                        })
                    }
                    MusicBadge(
                        text: viewModel.isSpotifyConnected ? "Spotify live" : "Preview ready",
                        isAccent: false,
                        onTap: onOpenSpotifyStatus
                    )
                }
            }
        }
    }

    @ViewBuilder
    private var spotlightCard: some View {
        if let selectedTrack {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .top, spacing: 16) {
                    AsyncImage(url: URL(string: selectedTrack.artworkUrl100 ?? "")) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        RoundedRectangle(cornerRadius: 24)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    }
                    .frame(width: 108, height: 108)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .stroke(AppColors.spotify(for: colorScheme).opacity(0.22), lineWidth: 1)
                    )

                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .center, spacing: 8) {
                            Text("ARTIST DECK")
                                .font(.caption.weight(.bold))
                                .foregroundColor(AppColors.spotify(for: colorScheme))

                            if viewModel.isSpotifyConnected {
                                Text("Live")
                                    .font(.caption.weight(.bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 5)
                                    .background(AppColors.accent(for: colorScheme))
                                    .clipShape(Capsule())
                            }
                        }

                        Text(selectedTrack.trackName)
                            .font(AppTypography.cardTitle)
                            .fontWeight(.black)
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .lineLimit(2)

                        Text(selectedArtistPage.tagline ?? (selectedTrack.artistName ?? brand.fallbackArtistName))
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(2)

                        Text(
                            selectedArtistPage.bio
                            ?? "Der aktuelle Fokus verbindet Artist-Page, Preview und Spotify direkt in einem Hub."
                        )
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(4)
                    }
                }

                HStack(spacing: 10) {
                    spotlightMetricCard(
                        title: "Track",
                        value: tracksStatusText,
                        accent: AppColors.spotify(for: colorScheme)
                    )

                    spotlightMetricCard(
                        title: "Artist",
                        value: selectedArtist,
                        accent: AppColors.accent(for: colorScheme)
                    )

                    spotlightMetricCard(
                        title: "Status",
                        value: viewModel.isSpotifyConnected ? "Spotify live" : "Preview ready",
                        accent: AppColors.accentMystic(for: colorScheme)
                    )
                }

                Text(
                    selectedTrack.collectionName?.isEmpty == false
                    ? "Aktuell aus \(selectedTrack.collectionName ?? ""). Preview, Spotify und Artist-Page bleiben direkt in Reichweite."
                    : "Preview, Spotify und Artist-Page bleiben direkt in Reichweite, ohne aus dem Music-Flow zu springen."
                )
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 10) {
                        spotlightPrimaryActions
                    }

                    VStack(spacing: 10) {
                        spotlightPrimaryActions
                    }
                }

                spotlightSpotifyAction(for: selectedTrack)
            }
            .padding(SkydownLayout.cardPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.spotify(for: colorScheme))
        }
    }

    @ViewBuilder
    private var spotlightPrimaryActions: some View {
        if let selectedTrack {
            if selectedTrack.previewUrl != nil {
                Button {
                    audioManager.playPreview(for: selectedTrack)
                } label: {
                    Label(
                        audioManager.currentlyPlayingId == selectedTrack.trackId ? "Preview stoppen" : "Preview starten",
                        systemImage: audioManager.currentlyPlayingId == selectedTrack.trackId ? "pause.fill" : "play.fill"
                    )
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(AppColors.accent(for: colorScheme))
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
            }

            if brand.showsArtistPages {
                Button {
                    presentSheet(.artistPage)
                } label: {
                    Label("Artist Page", systemImage: "person.crop.square.fill")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
            }
        }
    }

    @ViewBuilder
    private var shortcutHubCard: some View {
        if brand.showsBeatHubShortcut || brand.workflowTitle != nil {
            VStack(alignment: .leading, spacing: 14) {
                Text("Quick Access")
                    .font(.headline)

                Text("Die wichtigsten Wege bleiben direkt auf der Music-Seite sichtbar.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                if brand.showsBeatHubShortcut, brand.workflowTitle != nil {
                    HStack(spacing: 12) {
                        NavigationLink {
                            BeatHubView()
                        } label: {
                            quickAccessTile(
                                title: "Beat Hub",
                                subtitle: "Playback, Auswahl, Vibe",
                                accent: AppColors.accent(for: colorScheme),
                                systemImage: "waveform.circle.fill"
                            )
                        }
                        .buttonStyle(.plain)
                        .skydownTactileAction()

                        NavigationLink {
                            NicmaProducerView()
                        } label: {
                            quickAccessTile(
                                title: "Studio",
                                subtitle: "Record, Mix, Master",
                                accent: AppColors.accentMystic(for: colorScheme),
                                systemImage: "sparkles"
                            )
                        }
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                    }
                } else if brand.showsBeatHubShortcut {
                    NavigationLink {
                        BeatHubView()
                    } label: {
                        quickAccessTile(
                            title: "Beat Hub",
                            subtitle: "Playback, Auswahl, Vibe",
                            accent: AppColors.accent(for: colorScheme),
                            systemImage: "waveform.circle.fill"
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                } else if let workflowTitle = brand.workflowTitle,
                          let workflowSubtitle = brand.workflowSubtitle {
                    NavigationLink {
                        NicmaProducerView()
                    } label: {
                        quickAccessTile(
                            title: workflowTitle,
                            subtitle: workflowSubtitle,
                            accent: AppColors.accentMystic(for: colorScheme),
                            systemImage: "sparkles"
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                }
            }
            .padding(SkydownLayout.cardPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.accent(for: colorScheme))
        }
    }

    private func spotlightMetricCard(title: String, value: String, accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title.uppercased())
                .font(.caption.weight(.bold))
                .foregroundColor(accent)

            Text(value)
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(2)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(accent.opacity(0.18), lineWidth: 1)
        )
    }

    @ViewBuilder
    private func spotlightSpotifyAction(for track: Track) -> some View {
        if resolvedMusicViewSpotifyTrackID(track) != nil {
            Button {
                presentSheet(.spotifyPlayer)
            } label: {
                Label("Spotify Player", systemImage: "music.note.tv")
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(AppColors.spotifySurface(for: colorScheme))
                    .foregroundColor(AppColors.spotify(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
        } else if let externalURL = track.externalURL,
                  let url = URL(string: externalURL) {
            Link(destination: url) {
                Label(
                    resolvedMusicViewSpotifyArtistID(track) != nil ? "Spotify Artist" : "Spotify Suche",
                    systemImage: "arrow.up.forward.square"
                )
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(AppColors.spotifySurface(for: colorScheme))
                .foregroundColor(AppColors.spotify(for: colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
        }
    }

    private func quickAccessTile(
        title: String,
        subtitle: String,
        accent: Color,
        systemImage: String
    ) -> some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(accent.opacity(0.16))

                Image(systemName: systemImage)
                    .font(.headline.weight(.bold))
                    .foregroundColor(accent)
            }
            .frame(width: 42, height: 42)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(subtitle)
                    .font(.caption.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .multilineTextAlignment(.leading)
            }

            Spacer(minLength: 8)

            Image(systemName: "arrow.up.right")
                .font(.caption.weight(.bold))
                .foregroundColor(accent)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(accent.opacity(0.18), lineWidth: 1)
        )
    }

    private var artistsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Alle Artists")
                .font(.headline)

            if brand.showsArtistPages {
                Text("Scroll durch alle Artist-Pages, setz einen Artist in den Fokus und oeffne direkt das passende Profil.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 12) {
                        ForEach(artists, id: \.self) { artist in
                            artistPagerCard(for: artist)
                                .frame(width: 296, height: 236)
                        }
                    }
                    .padding(.vertical, 2)
                }
                .accessibilityIdentifier("music.artists.rail")
            } else {
                ForEach(artists, id: \.self) { artist in
                    artistButton(for: artist)
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    private func artistButton(for artist: String) -> some View {
        let isSelected = selectedArtist == artist

        return Button {
            selectedArtist = artist
        } label: {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "music.mic")
                    .font(.title3)

                VStack(alignment: .leading, spacing: 2) {
                    Text(artist)
                        .font(.headline)
                        .lineLimit(1)

                    Text(isSelected ? "Aktiv" : "Waehlen")
                        .font(.caption)
                        .foregroundColor(
                            isSelected
                            ? Color.white.opacity(0.82)
                            : AppColors.secondaryText(for: colorScheme)
                        )
                }

                Spacer()
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                    .stroke(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.accent(for: colorScheme).opacity(0.18),
                        lineWidth: 1
                    )
            )
            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private func artistPagerCard(for artist: String) -> some View {
        let page = artistPagesStore.page(for: brand.artistPageBrand, artistName: artist)
        let isSelected = selectedArtist == artist

        return VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(artist)
                        .font(.title3.weight(.black))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(page.tagline ?? "\(brand.artistPageBrand.displayTitle) Artist")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                if isSelected {
                    Text("Aktiv")
                        .font(.caption.weight(.bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(AppColors.spotify(for: colorScheme))
                        .clipShape(Capsule())
                } else if page.hasCustomPresentation {
                    Text("Live")
                        .font(.caption.weight(.bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(AppColors.accent(for: colorScheme))
                        .clipShape(Capsule())
                }
            }

            Text(
                page.bio ?? "\(artist) hat eine eigene Page mit Songs, Story und den wichtigsten Links."
            )
            .font(.footnote)
            .foregroundColor(AppColors.secondaryText(for: colorScheme))
            .lineLimit(3)

            HStack(spacing: 8) {
                MusicBadge(text: page.spotifyURL == nil ? "Page" : "Spotify", isAccent: true)
                if page.instagramURL != nil {
                    MusicBadge(text: "Instagram", isAccent: false)
                }
                if page.youtubeURL != nil {
                    MusicBadge(text: "YouTube", isAccent: false)
                }
            }

            HStack(spacing: 10) {
                Button {
                    selectedArtist = artist
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: isSelected ? "checkmark.circle.fill" : "music.mic")
                            .font(.headline.weight(.bold))
                        Text(isSelected ? "Im Fokus" : "Artist waehlen")
                            .font(.subheadline.weight(.semibold))
                        Spacer()
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(AppColors.accent(for: colorScheme).opacity(0.94))
                    )
                }
                .buttonStyle(.plain)
                .skydownTactileAction()

                Button {
                    selectedArtist = artist
                    presentSheet(.artistPage)
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "person.crop.square.fill")
                            .font(.headline.weight(.bold))
                        Text("Page")
                            .font(.subheadline.weight(.semibold))
                    }
                    .foregroundColor(AppColors.spotify(for: colorScheme))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(AppColors.spotifySurface(for: colorScheme))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
                .skydownTactileAction()
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    private var spotifyCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Spotify")
                .font(.headline)

            Text(spotifyStatusText)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if viewModel.isSpotifyConnected {
                Button(role: .destructive) {
                    audioManager.stop()
                    viewModel.disconnectSpotify()
                } label: {
                    Label("Spotify trennen", systemImage: "rectangle.portrait.and.arrow.right")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .background(AppColors.spotifySurface(for: colorScheme))
                .foregroundColor(AppColors.spotify(for: colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                Button {
                    Task {
                        await connectSpotifyAndLoadTracks()
                    }
                } label: {
                    if viewModel.isConnectingSpotify {
                        ProgressView()
                            .tint(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    } else {
                        Label("Spotify verbinden", systemImage: "music.note")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                }
                .background(AppColors.spotify(for: colorScheme))
                .foregroundColor(.black)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    @ViewBuilder
    private var instagramCard: some View {
        let destinations = musicInstagramDestinations()
        if !destinations.isEmpty {
            MusicInstagramHubCard(
                selectedArtist: selectedArtist,
                destinations: destinations,
                colorScheme: colorScheme
            )
        }
    }

    private var tracksCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Tracks")
                .font(.headline)

            tracksContent
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    @ViewBuilder
    private var musicPlayerCard: some View {
        if let selectedTrack {
            VStack(alignment: .leading, spacing: 14) {
                Text("Song Player")
                    .font(.headline)

                HStack(alignment: .top, spacing: 14) {
                    AsyncImage(url: URL(string: selectedTrack.artworkUrl100 ?? "")) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        RoundedRectangle(cornerRadius: 20)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    }
                    .frame(width: 86, height: 86)
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                    VStack(alignment: .leading, spacing: 6) {
                        Text(selectedTrack.trackName)
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(selectedTrack.artistName ?? brand.fallbackArtistName)
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        if let album = selectedTrack.collectionName, !album.isEmpty {
                            Text(album)
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    }
                }

                HStack(spacing: 10) {
                    if selectedTrack.previewUrl != nil {
                        Button {
                            audioManager.playPreview(for: selectedTrack)
                        } label: {
                            Label(
                                audioManager.currentlyPlayingId == selectedTrack.trackId ? "Preview stoppen" : "Preview starten",
                                systemImage: audioManager.currentlyPlayingId == selectedTrack.trackId ? "pause.fill" : "play.fill"
                            )
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(AppColors.accent(for: colorScheme))
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    }

                    if resolvedMusicViewSpotifyTrackID(selectedTrack) != nil {
                        Button {
                            presentSheet(.spotifyPlayer)
                        } label: {
                            Label("Spotify Player", systemImage: "music.note.tv")
                                .font(.subheadline.weight(.semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(AppColors.spotifySurface(for: colorScheme))
                                .foregroundColor(AppColors.spotify(for: colorScheme))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14)
                                        .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    } else if let externalURL = selectedTrack.externalURL, let url = URL(string: externalURL) {
                        Link(destination: url) {
                            Label(
                                resolvedMusicViewSpotifyArtistID(selectedTrack) != nil ? "Spotify Artist" : "Spotify Suche",
                                systemImage: "arrow.up.forward.square"
                            )
                                .font(.subheadline.weight(.semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(AppColors.spotifySurface(for: colorScheme))
                                .foregroundColor(AppColors.spotify(for: colorScheme))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14)
                                        .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    }
                }
            }
            .padding(SkydownLayout.cardPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                    .fill(AppColors.cardBackground(for: colorScheme))
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
            )
        }
    }

    @ViewBuilder
    private var tracksContent: some View {
        if viewModel.isLoadingTracks {
            ProgressView("Lade Songs...")
                .frame(maxWidth: .infinity, alignment: .leading)
        } else if viewModel.tracks.isEmpty {
                Text("Noch keine Songs fuer \(selectedArtist).")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        } else {
            Text(tracksStatusText)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            LazyVStack(spacing: 12) {
                ForEach(viewModel.tracks) { track in
                    TrackView(
                        track: track,
                        audioManager: audioManager,
                        isSelected: selectedTrackID == track.trackId
                    ) {
                        selectedTrackID = track.trackId
                    }
                }
            }
        }
    }

    private func connectSpotifyAndLoadTracks() async {
        await viewModel.connectSpotify()
        if viewModel.isSpotifyConnected {
            await viewModel.fetchTracks(for: selectedArtist)
        }
    }

    private func reloadTracksIfNeeded() async {
        audioManager.stop()
        await viewModel.fetchTracks(for: selectedArtist)
    }

    private func activateInitialSelectionIfNeeded() {
        guard !hasHandledInitialSelection,
              !viewModel.tracks.isEmpty else {
            return
        }

        let initialTrack = initialTrackID.flatMap { targetID in
            viewModel.tracks.first { $0.trackId == targetID }
        }
        let track = initialTrack ?? selectedTrack ?? viewModel.tracks.first

        guard let track else { return }

        selectedTrackID = track.trackId

        if autoplaySelectedTrackPreview, track.previewUrl != nil {
            audioManager.playPreview(for: track)
        } else if autoPresentSelectedTrackSpotifyPlayer,
                  resolvedMusicViewSpotifyTrackID(track) != nil {
            presentSheet(.spotifyPlayer)
        }

        hasHandledInitialSelection = true
    }

    private func musicInstagramDestinations() -> [MusicInstagramDestination] {
        let artistDestinations = artists.compactMap { artistInstagramDestinations[$0] }

        switch brand {
        case .skydown:
            return artistDestinations + [skydownMusicInstagramDestination]
        case .zweizwei:
            return [zweizweiInstagramDestination] + artistDestinations
        }
    }
}

private enum MusicPresentedSheet: String, Identifiable, Equatable {
    case spotifyPlayer
    case artistPage

    var id: String { rawValue }
}

private struct MusicShortcutFab: View {
    let title: String
    let systemImage: String
    let tint: Color
    let textColor: Color

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .font(.headline.weight(.bold))

            Text(title)
                .font(.subheadline.weight(.bold))
        }
        .foregroundColor(textColor)
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .background(tint)
        .clipShape(Capsule())
        .shadow(color: tint.opacity(0.24), radius: 16, y: 8)
    }
}

private func musicViewTrackSpotifyID(externalURL: String?) -> String? {
    guard let externalURL,
          let webURL = URL(string: externalURL),
          let components = URLComponents(url: webURL, resolvingAgainstBaseURL: false) else {
        return nil
    }

    let pathComponents = components.path.split(separator: "/")
    guard let trackIndex = pathComponents.firstIndex(of: "track"),
          trackIndex + 1 < pathComponents.count else {
        return nil
    }

    return String(pathComponents[trackIndex + 1])
}

private func resolvedMusicViewSpotifyTrackID(_ track: Track) -> String? {
    if let spotifyTrackID = track.spotifyTrackID, !spotifyTrackID.isEmpty {
        return spotifyTrackID
    }
    return musicViewTrackSpotifyID(externalURL: track.externalURL)
}

private func resolvedMusicViewSpotifyArtistID(_ track: Track) -> String? {
    if let spotifyArtistID = track.spotifyArtistID, !spotifyArtistID.isEmpty {
        return spotifyArtistID
    }

    guard let externalURL = track.externalURL,
          let webURL = URL(string: externalURL),
          let components = URLComponents(url: webURL, resolvingAgainstBaseURL: false) else {
        return nil
    }

    let pathComponents = components.path.split(separator: "/")
    guard let artistIndex = pathComponents.firstIndex(of: "artist"),
          artistIndex + 1 < pathComponents.count else {
        return nil
    }

    return String(pathComponents[artistIndex + 1])
}

#Preview {
    MusicView()
}
