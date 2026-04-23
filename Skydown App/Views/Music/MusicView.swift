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
            return "Katalog · Previews · Spotify"
        case .zweizwei:
            return "Releases · Tracks · Beats"
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
            return "Record · Mix · Master"
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
    case spotlight
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
    @State private var sheetPresentation = SkydownQueuedPresentation<MusicPresentedSheet>()
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
            return "Spotify ist bereit, wenn du nahtlos weiterhoeren willst."
        }
        return "In-App-Previews laufen ruhig weiter. Spotify bleibt optional."
    }

    private var tracksStatusText: String {
        "\(viewModel.tracks.count) Titel fuer \(selectedArtist)"
    }

    private var selectedTrack: Track? {
        viewModel.tracks.first { $0.trackId == selectedTrackID } ?? viewModel.tracks.first
    }

    private var selectedTrackQueuePosition: Int? {
        guard let selectedTrackID,
              let index = viewModel.tracks.firstIndex(where: { $0.trackId == selectedTrackID }) else {
            return nil
        }
        return index + 1
    }

    private var queueStatusText: String {
        guard !viewModel.tracks.isEmpty else { return "Queue wird vorbereitet" }
        if let position = selectedTrackQueuePosition {
            return "Queue \(position)/\(viewModel.tracks.count)"
        }
        return "Queue \(viewModel.tracks.count) Titel"
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
                                onOpenSpotlight: {
                                    withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                        scrollProxy.scrollTo(MusicSectionAnchor.spotlight.rawValue, anchor: .top)
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
                                .id(MusicSectionAnchor.spotlight.rawValue)

                            if layout.prefersTwoColumn {
                                HStack(alignment: .top, spacing: layout.sectionSpacing) {
                                    VStack(alignment: .leading, spacing: layout.sectionSpacing) {
                                        artistsCard
                                            .id(MusicSectionAnchor.artists.rawValue)
                                        shortcutHubCard
                                    }
                                    .frame(maxWidth: .infinity, alignment: .topLeading)

                                    VStack(alignment: .leading, spacing: layout.sectionSpacing) {
                                        musicPlayerCard
                                        tracksCard
                                            .id(MusicSectionAnchor.tracks.rawValue)
                                        spotifyCard
                                            .id(MusicSectionAnchor.spotify.rawValue)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .topLeading)
                                }
                            } else {
                                musicPlayerCard
                                tracksCard
                                    .id(MusicSectionAnchor.tracks.rawValue)
                                artistsCard
                                    .id(MusicSectionAnchor.artists.rawValue)
                                spotifyCard
                                    .id(MusicSectionAnchor.spotify.rawValue)
                                shortcutHubCard
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
                    .background(musicBackground)
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
        .sheet(item: activePresentedSheetBinding) { sheet in
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
    }

    private var activePresentedSheetBinding: Binding<MusicPresentedSheet?> {
        Binding(
            get: { sheetPresentation.activeItem },
            set: { sheetPresentation.updatePresentedItem($0) }
        )
    }

    private func presentSheet(_ sheet: MusicPresentedSheet) {
        sheetPresentation.request(sheet)
    }

    private var musicBackground: some View {
        ZStack {
            AppColors.screenGradient(
                for: colorScheme,
                secondaryAccent: AppColors.spotify(for: colorScheme)
            )

            RadialGradient(
                colors: [
                    AppColors.spotify(for: colorScheme).opacity(colorScheme == .dark ? 0.09 : 0.08),
                    Color.clear
                ],
                center: .topTrailing,
                startRadius: 12,
                endRadius: 320
            )

            RadialGradient(
                colors: [
                    AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.07 : 0.06),
                    Color.clear
                ],
                center: .bottomLeading,
                startRadius: 20,
                endRadius: 300
            )

            LinearGradient(
                colors: [
                    Color.white.opacity(colorScheme == .dark ? 0.035 : 0.10),
                    Color.clear,
                    Color.black.opacity(colorScheme == .dark ? 0.03 : 0.02)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
        .ignoresSafeArea()
    }

    private func musicCardBackground(
        accent: Color,
        secondaryAccent: Color? = nil,
        cornerRadius: CGFloat = SkydownLayout.cardCornerRadius
    ) -> some View {
        let secondary = secondaryAccent ?? AppColors.accentHighlight(for: colorScheme)

        return RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(
                LinearGradient(
                    colors: [
                        AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.985 : 0.995),
                        AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.76 : 0.68),
                        accent.opacity(colorScheme == .dark ? 0.12 : 0.07),
                        secondary.opacity(colorScheme == .dark ? 0.08 : 0.05)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(colorScheme == .dark ? 0.06 : 0.14),
                                Color.clear
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottom
                        )
                    )
                    .blendMode(.screen)
            }
    }

    private func musicCardStroke(
        accent: Color,
        secondaryAccent: Color? = nil,
        cornerRadius: CGFloat = SkydownLayout.cardCornerRadius
    ) -> some View {
        let secondary = secondaryAccent ?? AppColors.accentHighlight(for: colorScheme)

        return RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .stroke(
                LinearGradient(
                    colors: [
                        accent.opacity(colorScheme == .dark ? 0.20 : 0.14),
                        secondary.opacity(colorScheme == .dark ? 0.14 : 0.10),
                        Color.white.opacity(colorScheme == .dark ? 0.06 : 0.12)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                lineWidth: 1
            )
    }

    private func heroCard(
        onOpenArtistHub: @escaping () -> Void,
        onOpenSpotlight: @escaping () -> Void,
        onOpenTracks: @escaping () -> Void,
        onOpenSpotifyStatus: @escaping () -> Void
    ) -> some View {
        BrandHeroSurface(
            colorScheme: colorScheme,
            eyebrow: screenHeaderSettingsStore.settings.resolvedMusicHubEyebrow ?? "Music",
            title: screenHeaderSettingsStore.settings.resolvedMusicHubTitle ?? brand.heroTitle,
            subtitle: screenHeaderSettingsStore.settings.resolvedMusicHubSubtitle ?? "Atmosphaere, Artist-Fokus und Premium Listening in einem ruhigen Flow.",
            detail: screenHeaderSettingsStore.settings.resolvedMusicHubDetail ?? "\(selectedArtist) im Fokus · Featured Drop · direkte Wiedergabe",
            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedMusicHubImageURL,
            accent: AppColors.spotify(for: colorScheme),
            secondaryAccent: AppColors.accent(for: colorScheme),
            marks: brand == .zweizwei ? [.zweizwei] : [.skydown]
        ) {
            VStack(alignment: .leading, spacing: 10) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        MusicBadge(text: selectedArtist, isAccent: true, onTap: onOpenArtistHub)
                        MusicBadge(text: "Featured Drop", isAccent: false, onTap: onOpenSpotlight)
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

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        musicListeningModeChip(title: "Focus", accent: AppColors.spotify(for: colorScheme), onTap: onOpenSpotlight)
                        musicListeningModeChip(title: "Discovery", accent: AppColors.accent(for: colorScheme), onTap: onOpenTracks)
                        musicListeningModeChip(title: "Artist Hub", accent: AppColors.accentHighlight(for: colorScheme), onTap: onOpenArtistHub)
                        musicListeningModeChip(title: "Live Link", accent: AppColors.accentMystic(for: colorScheme), onTap: onOpenSpotifyStatus)
                    }
                }
            }
        }
    }

    private func musicListeningModeChip(title: String, accent: Color, onTap: @escaping () -> Void) -> some View {
        Button(action: onTap) {
            Text(title)
                .font(.caption.weight(.bold))
                .foregroundColor(accent)
                .padding(.horizontal, 11)
                .padding(.vertical, 8)
                .background(
                    Capsule(style: .continuous)
                        .fill(accent.opacity(colorScheme == .dark ? 0.20 : 0.12))
                )
                .overlay(
                    Capsule(style: .continuous)
                        .stroke(accent.opacity(colorScheme == .dark ? 0.36 : 0.24), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
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
            .background(
                musicCardBackground(
                    accent: AppColors.spotify(for: colorScheme),
                    secondaryAccent: AppColors.accentHighlight(for: colorScheme)
                )
            )
            .overlay(
                musicCardStroke(
                    accent: AppColors.spotify(for: colorScheme),
                    secondaryAccent: AppColors.accentHighlight(for: colorScheme)
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
            .shadow(
                color: .black.opacity(colorScheme == .dark ? 0.14 : 0.07),
                radius: 14,
                y: 8
            )
        }
    }

    @ViewBuilder
    private var spotlightPrimaryActions: some View {
        if let selectedTrack {
            if selectedTrack.previewUrl != nil {
                Button {
                    withAnimation(.easeInOut(duration: 0.20)) {
                        audioManager.playPreview(for: selectedTrack)
                    }
                } label: {
                    Label(
                            audioManager.currentlyPlayingId == selectedTrack.trackId ? "Preview pausieren" : "Preview anhoeren",
                        systemImage: audioManager.currentlyPlayingId == selectedTrack.trackId ? "pause.fill" : "play.fill"
                    )
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(AppColors.accent(for: colorScheme))
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .skydownTactileAction()
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
                .skydownTactileAction()
            }
        }
    }

    @ViewBuilder
    private var shortcutHubCard: some View {
        if brand.showsBeatHubShortcut || brand.workflowTitle != nil {
            VStack(alignment: .leading, spacing: 14) {
                Text("Quick Access")
                    .font(.headline)

                Text("Optionaler Schnellzugriff fuer Studio und Beat Hub.")
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
            .padding(max(14, SkydownLayout.cardPadding - 4))
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                musicCardBackground(
                    accent: AppColors.accent(for: colorScheme),
                    secondaryAccent: AppColors.spotify(for: colorScheme)
                )
            )
            .overlay(
                musicCardStroke(
                    accent: AppColors.accent(for: colorScheme),
                    secondaryAccent: AppColors.spotify(for: colorScheme)
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
            .shadow(
                color: .black.opacity(colorScheme == .dark ? 0.12 : 0.06),
                radius: 12,
                y: 7
            )
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
            musicCardBackground(
                accent: accent,
                secondaryAccent: AppColors.spotify(for: colorScheme),
                cornerRadius: 18
            )
        )
        .overlay(
            musicCardStroke(
                accent: accent,
                secondaryAccent: AppColors.spotify(for: colorScheme),
                cornerRadius: 18
            )
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
            musicCardBackground(
                accent: accent,
                secondaryAccent: AppColors.accentHighlight(for: colorScheme),
                cornerRadius: 20
            )
        )
        .overlay(
            musicCardStroke(
                accent: accent,
                secondaryAccent: AppColors.accentHighlight(for: colorScheme),
                cornerRadius: 20
            )
        )
    }

    private var artistsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Alle Artists")
                .font(.headline)

            if brand.showsArtistPages {
                Text("Alle Artists sind direkt anwaehlbar. Nutze die Schnellwahl fuer den direkten Einstieg oder swipe unten durch den Showcase.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                artistMapSection

                VStack(alignment: .leading, spacing: 10) {
                    Text("Schnellwahl")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    ForEach(artists, id: \.self) { artist in
                        artistOverviewRow(for: artist)
                    }
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("Swipe Showcase")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Fuer einen schnellen visuellen Durchlauf mit grosser Vorschau.")
                        .font(.footnote)
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
                }
            } else {
                ForEach(artists, id: \.self) { artist in
                    artistButton(for: artist)
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            musicCardBackground(
                accent: AppColors.accentHighlight(for: colorScheme),
                secondaryAccent: AppColors.spotify(for: colorScheme)
            )
        )
        .overlay(
            musicCardStroke(
                accent: AppColors.accentHighlight(for: colorScheme),
                secondaryAccent: AppColors.spotify(for: colorScheme)
            )
        )
    }

    private var artistMapSection: some View {
        let liveCount = artists.filter { artistPage(for: $0).hasCustomPresentation }.count
        let connectedCount = artists.filter { artistReachCount(for: artistPage(for: $0)) > 1 }.count

        return VStack(alignment: .leading, spacing: 12) {
            Text("Artist Map")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Alle Artists auf einen Blick mit Fokus, Reach und Status. Tippe direkt auf eine Karte fuer den schnellen Wechsel.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ViewThatFits(in: .horizontal) {
                HStack(spacing: 12) {
                    artistMetricCard(title: "Artists", value: "\(artists.count)", accent: AppColors.accent(for: colorScheme))
                    artistMetricCard(title: "Live Pages", value: "\(liveCount)", accent: AppColors.spotify(for: colorScheme))
                    artistMetricCard(title: "Connected", value: "\(connectedCount)", accent: AppColors.accentHighlight(for: colorScheme))
                }

                VStack(spacing: 12) {
                    artistMetricCard(title: "Artists", value: "\(artists.count)", accent: AppColors.accent(for: colorScheme))
                    artistMetricCard(title: "Live Pages", value: "\(liveCount)", accent: AppColors.spotify(for: colorScheme))
                    artistMetricCard(title: "Connected", value: "\(connectedCount)", accent: AppColors.accentHighlight(for: colorScheme))
                }
            }

            LazyVGrid(columns: [GridItem(.adaptive(minimum: 156), spacing: 12)], spacing: 12) {
                ForEach(artists, id: \.self) { artist in
                    artistSignalTile(for: artist)
                }
            }
        }
    }

    private func artistMetricCard(title: String, value: String, accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(accent)

            Text(value)
                .font(.headline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(accent.opacity(0.22), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func artistSignalTile(for artist: String) -> some View {
        let page = artistPage(for: artist)
        let isSelected = selectedArtist == artist
        let statusText = isSelected ? "Im Fokus" : (page.hasCustomPresentation ? "Live" : "Page")
        let reachText = artistReachSummary(for: page)

        return Button {
            selectedArtist = artist
        } label: {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 10) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(artist)
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .lineLimit(1)

                        Text(page.tagline ?? "\(brand.artistPageBrand.displayTitle) Artist")
                            .font(.caption.weight(.medium))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(2)
                    }

                    Spacer(minLength: 8)

                    Text(statusText)
                        .font(.caption2.weight(.bold))
                        .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(
                            Capsule(style: .continuous)
                                .fill(
                                    isSelected
                                    ? AppColors.spotify(for: colorScheme)
                                    : AppColors.secondaryBackground(for: colorScheme)
                                )
                        )
                }

                HStack(spacing: 8) {
                    artistSignalPill(text: reachText, accent: AppColors.accent(for: colorScheme))
                    artistSignalPill(
                        text: page.hasCustomPresentation ? "Custom Page" : "Direkter Einstieg",
                        accent: AppColors.spotify(for: colorScheme)
                    )
                }

                Text(isSelected ? "Aktuell im Fokus" : "Tippen zum Fokussieren")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(
                        isSelected
                        ? AppColors.spotify(for: colorScheme)
                        : AppColors.secondaryText(for: colorScheme)
                    )
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 11)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(
                        (isSelected ? AppColors.spotify(for: colorScheme) : AppColors.accent(for: colorScheme)).opacity(0.24),
                        lineWidth: 1
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private func artistSignalPill(text: String, accent: Color) -> some View {
        Text(text)
            .font(.caption2.weight(.semibold))
            .foregroundColor(accent)
            .padding(.horizontal, 9)
            .padding(.vertical, 6)
            .background(
                Capsule(style: .continuous)
                    .fill(accent.opacity(colorScheme == .dark ? 0.18 : 0.12))
            )
    }

    private func artistPage(for artist: String) -> ArtistPage {
        artistPagesStore.page(for: brand.artistPageBrand, artistName: artist)
    }

    private func artistReachCount(for page: ArtistPage) -> Int {
        [page.spotifyURL, page.instagramURL, page.youtubeURL]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .count
    }

    private func artistReachSummary(for page: ArtistPage) -> String {
        let channels = [
            page.spotifyURL == nil ? nil : "Spotify",
            page.instagramURL == nil ? nil : "Instagram",
            page.youtubeURL == nil ? nil : "YouTube"
        ]
            .compactMap { $0 }

        if channels.isEmpty {
            return "Noch keine Links"
        }

        return channels.prefix(2).joined(separator: " • ")
    }

    private func artistOverviewRow(for artist: String) -> some View {
        let page = artistPage(for: artist)
        let isSelected = selectedArtist == artist

        return VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(artist)
                        .font(.headline.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(1)

                    Text(page.tagline ?? "\(brand.artistPageBrand.displayTitle) Artist")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(2)
                }

                Spacer(minLength: 8)

                if isSelected {
                    Text("Im Fokus")
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

            HStack(spacing: 10) {
                Button {
                    selectedArtist = artist
                } label: {
                    Label(isSelected ? "Im Fokus" : "Artist waehlen", systemImage: isSelected ? "checkmark.circle.fill" : "music.mic")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .background(AppColors.accent(for: colorScheme))
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .buttonStyle(.plain)
                .skydownTactileAction()

                Button {
                    selectedArtist = artist
                    presentSheet(.artistPage)
                } label: {
                    Label("Page oeffnen", systemImage: "person.crop.square.fill")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .background(AppColors.spotifySurface(for: colorScheme))
                .foregroundColor(AppColors.spotify(for: colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .buttonStyle(.plain)
                .skydownTactileAction()
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            musicCardBackground(
                accent: AppColors.spotify(for: colorScheme),
                secondaryAccent: AppColors.accentHighlight(for: colorScheme),
                cornerRadius: 20
            )
        )
        .overlay(
            musicCardStroke(
                accent: AppColors.spotify(for: colorScheme),
                secondaryAccent: AppColors.accentHighlight(for: colorScheme),
                cornerRadius: 20
            )
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
        let page = artistPage(for: artist)
        let isSelected = selectedArtist == artist
        let openArtistPage = {
            selectedArtist = artist
            presentSheet(.artistPage)
        }

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
                page.bio ?? "\(artist) ist in Sky OS angelegt. Story, Visuals und Links koennen hier live geschaltet werden."
            )
            .font(.footnote)
            .foregroundColor(AppColors.secondaryText(for: colorScheme))
            .lineLimit(3)

            HStack(spacing: 8) {
                MusicBadge(
                    text: page.spotifyURL == nil ? "Page" : "Spotify",
                    isAccent: true,
                    onTap: openArtistPage
                )
                if page.instagramURL != nil {
                    MusicBadge(text: "Instagram", isAccent: false, onTap: openArtistPage)
                }
                if page.youtubeURL != nil {
                    MusicBadge(text: "YouTube", isAccent: false, onTap: openArtistPage)
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
                .accessibilityIdentifier("music.artist.open_page.\(artist)")
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .background(
            musicCardBackground(
                accent: AppColors.accentHighlight(for: colorScheme),
                secondaryAccent: AppColors.spotify(for: colorScheme)
            )
        )
        .overlay(
            musicCardStroke(
                accent: AppColors.accentHighlight(for: colorScheme),
                secondaryAccent: AppColors.spotify(for: colorScheme)
            )
        )
    }

    private var spotifyCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Spotify")
                .font(.headline)

            Text("Externer Handover bleibt optional und ruhig im Hintergrund. \(spotifyStatusText)")
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
        .padding(max(14, SkydownLayout.cardPadding - 4))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            musicCardBackground(
                accent: AppColors.spotify(for: colorScheme),
                secondaryAccent: AppColors.accentHighlight(for: colorScheme)
            )
        )
        .overlay(
            musicCardStroke(
                accent: AppColors.spotify(for: colorScheme),
                secondaryAccent: AppColors.accentHighlight(for: colorScheme)
            )
        )
    }

    private var tracksCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Tracks")
                .font(.headline)

            Text(queueStatusText)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            tracksContent
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            musicCardBackground(
                accent: AppColors.accent(for: colorScheme),
                secondaryAccent: AppColors.spotify(for: colorScheme)
            )
        )
        .overlay(
            musicCardStroke(
                accent: AppColors.accent(for: colorScheme),
                secondaryAccent: AppColors.spotify(for: colorScheme)
            )
        )
    }

    @ViewBuilder
    private var musicPlayerCard: some View {
        if let selectedTrack {
            VStack(alignment: .leading, spacing: 14) {
                Text("Now Listening")
                    .font(.headline)

                HStack(spacing: 8) {
                    Text(audioManager.currentlyPlayingId == selectedTrack.trackId ? "Live Preview" : "Bereit")
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                        .padding(.horizontal, 11)
                        .padding(.vertical, 8)
                        .background(
                            Capsule(style: .continuous)
                                .fill(AppColors.accentMystic(for: colorScheme).opacity(colorScheme == .dark ? 0.20 : 0.12))
                        )
                        .overlay(
                            Capsule(style: .continuous)
                                .stroke(AppColors.accentMystic(for: colorScheme).opacity(colorScheme == .dark ? 0.36 : 0.24), lineWidth: 1)
                        )
                    Text(queueStatusText)
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(1)
                }

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
                            withAnimation(.easeInOut(duration: 0.20)) {
                                audioManager.playPreview(for: selectedTrack)
                            }
                        } label: {
                            Label(
                                audioManager.currentlyPlayingId == selectedTrack.trackId ? "Preview pausieren" : "Preview anhoeren",
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
                            Label("In Spotify weiter", systemImage: "music.note.tv")
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
                        .skydownTactileAction()
                    } else if let externalURL = selectedTrack.externalURL, let url = URL(string: externalURL) {
                        Link(destination: url) {
                            Label(
                                resolvedMusicViewSpotifyArtistID(selectedTrack) != nil ? "Zum Spotify Artist" : "In Spotify suchen",
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
                musicCardBackground(
                    accent: AppColors.accentMystic(for: colorScheme),
                    secondaryAccent: AppColors.spotify(for: colorScheme)
                )
            )
            .overlay(
                musicCardStroke(
                    accent: AppColors.accentMystic(for: colorScheme),
                    secondaryAccent: AppColors.spotify(for: colorScheme)
                )
            )
            .animation(.easeInOut(duration: 0.22), value: audioManager.currentlyPlayingId)
            .animation(.easeInOut(duration: 0.22), value: selectedTrackID)
        }
    }

    @ViewBuilder
    private var tracksContent: some View {
        if viewModel.isLoadingTracks {
            ProgressView("Tracks werden ruhig vorbereitet...")
                .frame(maxWidth: .infinity, alignment: .leading)
        } else if viewModel.tracks.isEmpty {
                Text("Fuer \(selectedArtist) ist gerade noch kein Track live. Versuche es in einem Moment erneut.")
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
