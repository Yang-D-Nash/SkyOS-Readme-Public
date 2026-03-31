//
//  ShopView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import AVKit
import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    @StateObject private var beatPlaybackManager = BeatPlaybackManager()
    @StateObject private var audioPlayerManager = AudioPlayerManager()
    @StateObject private var videoPlaybackManager = HomeInlineVideoPlaybackManager()
    @State private var showingBeatHub = false
    @State private var showingNicmaProducer = false
    @State private var hasLoadedInitialHomeContent = false
    @Environment(\.colorScheme) private var colorScheme
    let onOpenCart: () -> Void
    let onOpenSettings: () -> Void
    let onOpenWorkflow: (() -> Void)?

    init(
        onOpenCart: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        onOpenWorkflow: (() -> Void)? = nil
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenSettings = onOpenSettings
        self.onOpenWorkflow = onOpenWorkflow
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    HomeHeroIntroCard(colorScheme: colorScheme)
                        .homeReveal(0)
                    HomeLatestReleaseCard(
                        viewModel: viewModel,
                        playbackManager: audioPlayerManager,
                        colorScheme: colorScheme
                    ) { track in
                        beatPlaybackManager.stop()
                        videoPlaybackManager.stop()
                        audioPlayerManager.playPreview(for: track)
                    }
                    .homeReveal(1)
                    HomeLatestBeatCard(
                        viewModel: viewModel,
                        playbackManager: beatPlaybackManager,
                        colorScheme: colorScheme
                    ) { beat in
                        audioPlayerManager.stop()
                        videoPlaybackManager.stop()
                        beatPlaybackManager.togglePlayback(for: beat.asBeatHubItem)
                    }
                    .homeReveal(2)
                    HomeLatestVideoCard(
                        viewModel: viewModel,
                        playbackManager: videoPlaybackManager,
                        colorScheme: colorScheme
                    ) { video in
                        beatPlaybackManager.stop()
                        audioPlayerManager.stop()
                        videoPlaybackManager.togglePlayback(for: video)
                    }
                    .homeReveal(3)
                    HomeStoryCard(
                        colorScheme: colorScheme,
                        onOpenBeatHub: {
                            beatPlaybackManager.stop()
                            audioPlayerManager.stop()
                            videoPlaybackManager.stop()
                            showingBeatHub = true
                        },
                        onOpenNicma: {
                            beatPlaybackManager.stop()
                            audioPlayerManager.stop()
                            videoPlaybackManager.stop()
                            showingNicmaProducer = true
                        }
                    )
                    .homeReveal(4)
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding * 0.5)
                .padding(.bottom, SkydownLayout.screenBottomPadding)
            }
            .scrollIndicators(.hidden)
            .refreshable {
                viewModel.refresh()
            }
            .background(AppColors.screenGradient(for: colorScheme).ignoresSafeArea())
            .navigationTitle("Home")
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    if let onOpenWorkflow {
                        Button(action: onOpenWorkflow) {
                            Image(systemName: "arrow.triangle.branch")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .padding(10)
                                .background(AppColors.secondaryBackground(for: colorScheme))
                                .clipShape(Circle())
                        }
                        .accessibilityLabel("Automationen oeffnen")
                    }

                    AppSessionToolbarActions(
                        onOpenCart: onOpenCart,
                        onOpenSettings: onOpenSettings
                    )
                }
            }
            .task {
                guard !hasLoadedInitialHomeContent else { return }
                hasLoadedInitialHomeContent = true
                viewModel.refresh()
            }
            .onDisappear {
                beatPlaybackManager.stop()
                audioPlayerManager.stop()
                videoPlaybackManager.stop()
            }
        }
        .sheet(isPresented: $showingBeatHub) {
            NavigationStack {
                BeatHubView()
            }
        }
        .sheet(isPresented: $showingNicmaProducer) {
            NavigationStack {
                NicmaProducerView()
            }
        }
    }
}

struct ShopView: View {
    @ObservedObject private var authManager: AuthManager
    @StateObject private var viewModel: MerchandiseViewModel
    private let onOpenLogin: () -> Void
    private let onOpenCart: () -> Void
    private let onOpenSettings: () -> Void
    @State private var showingAddSheet = false
    @State private var editingItem: MerchandiseItem?
    @State private var selectedItem: MerchandiseItem?
    @State private var itemToDelete: MerchandiseItem?
    @Environment(\.colorScheme) private var colorScheme

    init(
        authManager: AuthManager,
        onOpenLogin: @escaping () -> Void = {},
        onOpenCart: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService()
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        self.onOpenLogin = onOpenLogin
        self.onOpenCart = onOpenCart
        self.onOpenSettings = onOpenSettings
        _viewModel = StateObject(
            wrappedValue: MerchandiseViewModel(
                merchandiseService: merchandiseService,
                authManager: authManager
            )
        )
    }

    private var isAdmin: Bool {
        authManager.userSession?.isAdmin == true
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView("Artikel werden geladen...")
                        .tint(AppColors.accent(for: colorScheme))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                            ShopHeroCard(
                                colorScheme: colorScheme,
                                itemCount: viewModel.merchandiseItems.count,
                                isStoreOpen: viewModel.isStoreOpen,
                                isLoggedIn: authManager.userSession != nil,
                                isAdmin: isAdmin,
                                isUpdatingStoreState: viewModel.isUpdatingStoreState,
                                onToggleStore: isAdmin ? {
                                    Task {
                                        await viewModel.toggleStoreOpen()
                                    }
                                } : nil
                            )

                            if let errorMessage = viewModel.errorMessage, !isAdmin {
                                ShopInfoCard(
                                    colorScheme: colorScheme,
                                    title: "Anmeldung",
                                    message: errorMessage,
                                    actionTitle: "Anmelden",
                                    action: onOpenLogin
                                )
                            }

                            if !viewModel.isStoreOpen && !isAdmin {
                                ShopInfoCard(
                                    colorScheme: colorScheme,
                                    title: "Merch Store pausiert",
                                    message: "Produkte bleiben sichtbar, aber neue Kaeufe sind gerade geschlossen. Sobald du den Store wieder oeffnest, kann direkt wieder bestellt werden."
                                )
                            }

                            if viewModel.merchandiseItems.isEmpty {
                                ShopInfoCard(
                                    colorScheme: colorScheme,
                                    title: "Noch keine Artikel",
                                    message: "Sobald neuer Merch live ist, taucht er hier direkt als Card auf."
                                )
                            }

                            ForEach(viewModel.merchandiseItems) { item in
                                MerchandiseRowView(
                                    item: item,
                                    isAdmin: isAdmin,
                                    environmentColorScheme: colorScheme,
                                    onTap: {
                                        if isAdmin {
                                            editingItem = $0
                                        } else {
                                            selectedItem = $0
                                        }
                                    },
                                    onEdit: { editingItem = $0 },
                                    onDelete: { itemToDelete = $0 }
                                )
                            }
                        }
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        .padding(.top, SkydownLayout.screenTopPadding)
                        .padding(.bottom, SkydownLayout.screenBottomPadding)
                    }
                    .scrollIndicators(.hidden)
                    .refreshable {
                        viewModel.fetchData()
                    }
                }
            }
            .background(AppColors.screenGradient(for: colorScheme).ignoresSafeArea())
            .navigationTitle("Merch Hub")
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    HStack(spacing: 10) {
                        if isAdmin {
                            Button {
                                showingAddSheet = true
                            } label: {
                                Image(systemName: "plus")
                                    .font(.headline)
                            }
                        }

                        AppSessionToolbarActions(
                            onOpenCart: onOpenCart,
                            onOpenSettings: onOpenSettings
                        )
                    }
                }
            }
            .task {
                if viewModel.merchandiseItems.isEmpty {
                    viewModel.fetchData()
                }
            }
            .sheet(isPresented: $showingAddSheet) {
                MerchEditView(viewModel: viewModel)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
                    .background(AppColors.primaryBackground(for: colorScheme))
            }
            .sheet(item: $editingItem) { item in
                MerchEditView(viewModel: viewModel, merchandiseItem: item)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
                    .background(AppColors.primaryBackground(for: colorScheme))
            }
            .sheet(item: $selectedItem) { item in
                NavigationStack {
                    ContactFormView(
                        item: item,
                        storeIsOpen: viewModel.isStoreOpen || isAdmin
                    )
                        .background(AppColors.primaryBackground(for: colorScheme))
                }
            }
            .confirmationDialog(
                "Soll dieser Artikel wirklich geloescht werden?",
                isPresented: Binding(
                    get: { itemToDelete != nil },
                    set: { if !$0 { itemToDelete = nil } }
                ),
                titleVisibility: .visible
            ) {
                Button("Loeschen", role: .destructive) {
                    if let item = itemToDelete {
                        Task {
                            await viewModel.deleteItem(item)
                            itemToDelete = nil
                        }
                    }
                }
                Button("Abbrechen", role: .cancel) {
                    itemToDelete = nil
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
    }
}

private struct HomeHeroIntroCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Home")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Hier laufen Zweizwei, Skydown, Merchandise und Tools als globale Bereiche zusammen.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                Text("Entwickelt und koordiniert von Yang D. Nash als zentralem Ansprechpartner fuer Musik, Videography und Merchandise.")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "house.fill")
                    .font(.title2)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(20)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
    }
}

private struct HomeLatestReleaseCard: View {
    @ObservedObject var viewModel: HomeViewModel
    @ObservedObject var playbackManager: AudioPlayerManager
    let colorScheme: ColorScheme
    let onPreviewToggle: (Track) -> Void
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Zweizwei Release")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if let track = viewModel.featuredTrack {
                let hasPreview = !(track.previewUrl?.isEmpty ?? true)
                let hasSpotifyTarget = homeSpotifyTargetURL(for: track) != nil

                HStack(spacing: 14) {
                    AsyncImage(url: URL(string: track.artworkUrl100 ?? "")) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        RoundedRectangle(cornerRadius: 22)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    }
                    .frame(width: 82, height: 82)
                    .clipShape(RoundedRectangle(cornerRadius: 22))

                    VStack(alignment: .leading, spacing: 6) {
                        Text(track.trackName)
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(track.artistName ?? "Zweizwei")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        Text(homeReleaseLine(for: track))
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }

                    Spacer()
                }

                HStack(spacing: 10) {
                    Text(
                        hasPreview
                            ? "Preview bleibt direkt im Home. Den Spotify-Player findest du im Musik-Tab."
                            : (hasSpotifyTarget
                               ? "Neuester Song direkt ueber Spotify erreichbar."
                               : "Neuester Song.")
                    )
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                VStack(spacing: 10) {
                    if hasPreview {
                        HomeActionButton(
                            title: playbackManager.currentlyPlayingId == track.trackId ? "Release stoppen" : "Release abspielen",
                            icon: playbackManager.currentlyPlayingId == track.trackId ? "stop.fill" : "play.fill",
                            colorScheme: colorScheme,
                            isPrimary: true
                        ) {
                            onPreviewToggle(track)
                        }
                    }

                    if let spotifyURL = homeSpotifyTargetURL(for: track) {
                        HomeActionButton(
                            title: homeSpotifyActionTitle(for: track),
                            icon: "music.note",
                            colorScheme: colorScheme,
                            isPrimary: !hasPreview
                        ) {
                            openURL(spotifyURL)
                        }
                    }
                }
            } else {
                Text(viewModel.homeTrackMessage ?? "Neuer Song erscheint hier.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private func homeReleaseLine(for track: Track) -> String {
        let collection = track.collectionName ?? "Spotify Release"
        if let releaseDate = track.releaseDate, !releaseDate.isEmpty {
            return "\(collection) • \(releaseDate)"
        }
        return collection
    }
}

private struct HomeLatestBeatCard: View {
    @ObservedObject var viewModel: HomeViewModel
    @ObservedObject var playbackManager: BeatPlaybackManager
    let colorScheme: ColorScheme
    let onPlayToggle: (FeaturedHomeBeat) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Zweizwei Beat")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if let beat = viewModel.featuredBeat {
                HStack(alignment: .top, spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 22)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                            .frame(width: 82, height: 82)

                        Image(systemName: "waveform.circle.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.accent(for: colorScheme))
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(beat.title)
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(beat.artistName)
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        if !beat.notes.isEmpty {
                            Text(beat.notes)
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    }

                    Spacer()
                }

                if beat.isPlayable, !beat.downloadURL.isEmpty {
                    HomeActionButton(
                        title: playbackManager.currentBeatID == beat.id ? "Beat stoppen" : "Beat abspielen",
                        icon: playbackManager.currentBeatID == beat.id ? "stop.fill" : "play.fill",
                        colorScheme: colorScheme,
                        isPrimary: true
                    ) {
                        onPlayToggle(beat)
                    }
                }
            } else {
                Text(viewModel.homeBeatMessage ?? "Neuer Beat erscheint hier.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct HomeLatestVideoCard: View {
    @ObservedObject var viewModel: HomeViewModel
    @ObservedObject var playbackManager: HomeInlineVideoPlaybackManager
    let colorScheme: ColorScheme
    let onPlayToggle: (FeaturedHomeVideo) -> Void
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Skydown Video Highlight")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if let video = viewModel.featuredVideo {
                HStack(alignment: .top, spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 22)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                            .frame(width: 82, height: 82)

                        Image(systemName: "video.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.accent(for: colorScheme))
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(video.title)
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(video.projectName)
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        if !video.notes.isEmpty {
                            Text(video.notes)
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    }

                    Spacer()
                }

                Text("Aus dem Videography Hub fuers Home ausgewaehlt.")
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                if !video.downloadURL.isEmpty {
                    VideoPlayer(player: playbackManager.player)
                        .frame(height: 220)
                        .clipShape(RoundedRectangle(cornerRadius: 20))
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                        )
                        .onAppear {
                            playbackManager.prepare(video: video)
                        }
                        .onChange(of: video.id) { _, _ in
                            playbackManager.prepare(video: video)
                        }
                }

                VStack(spacing: 10) {
                    if !video.downloadURL.isEmpty {
                        HomeActionButton(
                            title: playbackManager.isPlaying && playbackManager.currentVideoID == video.id ? "Video stoppen" : "Video abspielen",
                            icon: playbackManager.isPlaying && playbackManager.currentVideoID == video.id ? "stop.fill" : "play.rectangle.fill",
                            colorScheme: colorScheme,
                            isPrimary: true
                        ) {
                            onPlayToggle(video)
                        }
                    }

                    if let videoURL = URL(string: video.downloadURL), !video.downloadURL.isEmpty {
                        HomeActionButton(
                            title: "Original-Datei oeffnen",
                            icon: "video.fill",
                            colorScheme: colorScheme,
                            isPrimary: false
                        ) {
                            openURL(videoURL)
                        }
                    }
                }
            } else {
                Text(viewModel.homeVideoMessage ?? "Neues Video erscheint hier.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct HomeStoryCard: View {
    let colorScheme: ColorScheme
    let onOpenBeatHub: () -> Void
    let onOpenNicma: () -> Void
    @Environment(\.openURL) private var openURL

    private let contactEmailURL = URL(string: "mailto:skydownent@gmail.com?subject=Skydown%20Videography%20Kontakt")

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Bereiche")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Zweizwei und Skydown behalten hier jeweils ihren eigenen Fokus, damit Musik und Videography sauber getrennt bleiben.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(spacing: 12) {
                HomeActionButton(
                    title: "Yang D. Nash • Ansprechpartner",
                    icon: "person.crop.circle.badge.checkmark",
                    colorScheme: colorScheme,
                    isPrimary: true
                ) {
                    if let url = artistInstagramDestinations["Yang D. Nash"]?.url {
                        openURL(url)
                    }
                }

                HomeLaneSection(
                    title: "Zweizwei Music",
                    subtitle: "Releases, Artists, Beat Hub und NICMA Producer bleiben hier im Zweizwei-Bereich gebuendelt.",
                    colorScheme: colorScheme
                ) {
                    ForEach(homeZweizweiInstagramDestinations) { destination in
                        HomeActionButton(
                            title: destination.title,
                            icon: destination.id == zweizweiInstagramDestination.id ? "music.note.house.fill" : "person.2.fill",
                            colorScheme: colorScheme,
                            isPrimary: destination.id == zweizweiInstagramDestination.id
                        ) {
                            if let url = destination.url {
                                openURL(url)
                            }
                        }
                    }

                    HomeActionButton(
                        title: "Beat Hub oeffnen",
                        icon: "waveform.circle.fill",
                        colorScheme: colorScheme,
                        isPrimary: false
                    ) {
                        onOpenBeatHub()
                    }

                    HomeActionButton(
                        title: "NICMA MUSIC oeffnen",
                        icon: "slider.horizontal.3",
                        colorScheme: colorScheme,
                        isPrimary: false
                    ) {
                        onOpenNicma()
                    }
                }

                HomeLaneSection(
                    title: "Skydown Videography",
                    subtitle: "Visuals, Clips und Kontakt laufen hier gesammelt im Skydown-Bereich.",
                    colorScheme: colorScheme
                ) {
                    HomeActionButton(
                        title: "Skydown auf Instagram",
                        icon: "sparkles.tv.fill",
                        colorScheme: colorScheme,
                        isPrimary: false
                    ) {
                        if let url = skydownMusicInstagramDestination.url {
                            openURL(url)
                        }
                    }

                    HomeActionButton(
                        title: "Kontakt per E-Mail",
                        icon: "envelope.fill",
                        colorScheme: colorScheme,
                        isPrimary: false
                    ) {
                        if let contactEmailURL {
                            openURL(contactEmailURL)
                        }
                    }
                }
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct HomeLaneSection<Content: View>: View {
    let title: String
    let subtitle: String
    let colorScheme: ColorScheme
    let content: Content

    init(
        title: String,
        subtitle: String,
        colorScheme: ColorScheme,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.subtitle = subtitle
        self.colorScheme = colorScheme
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(subtitle)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(spacing: 10) {
                content
            }
        }
        .padding(14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private let homeFeaturedArtists = [
    "Zweizwei Music",
    "ThaDude",
    "MAVE",
    "JANNO",
    "TANGAJOE007",
    "Toprack941"
]

private let homeZweizweiInstagramDestinations: [MusicInstagramDestination] = [
    zweizweiInstagramDestination,
    MusicInstagramDestination(
        id: "artist_thadude_home",
        title: "ThaDude",
        handle: "@thadude_offizielle",
        urlString: "https://www.instagram.com/thadude_offizielle/",
        helper: "Zweizwei Artist"
    ),
    MusicInstagramDestination(
        id: "artist_mave_home",
        title: "MAVE",
        handle: "@mave__official",
        urlString: "https://www.instagram.com/mave__official/",
        helper: "Zweizwei Artist"
    ),
    MusicInstagramDestination(
        id: "artist_janno_home",
        title: "JANNO",
        handle: "@janno_official_",
        urlString: "https://www.instagram.com/janno_official_/",
        helper: "Zweizwei Artist"
    ),
    MusicInstagramDestination(
        id: "artist_tangajoe_home",
        title: "TANGAJOE007",
        handle: "@tangajoe007",
        urlString: "https://www.instagram.com/tangajoe007/",
        helper: "Zweizwei Artist"
    )
]

private func homeMusicArtist(for track: Track) -> String {
    guard let artistName = track.artistName, !artistName.isEmpty else {
        return homeFeaturedArtists.first ?? "Yang D. Nash"
    }

    let normalizedArtist = homeNormalizeArtistName(artistName)
    return homeFeaturedArtists.first { candidate in
        let normalizedCandidate = homeNormalizeArtistName(candidate)
        return normalizedArtist == normalizedCandidate ||
            normalizedArtist.contains(normalizedCandidate) ||
            normalizedCandidate.contains(normalizedArtist)
    } ?? artistName
}

private func homeNormalizeArtistName(_ value: String) -> String {
    value
        .lowercased()
        .replacingOccurrences(of: "[^a-z0-9]+", with: "", options: .regularExpression)
}

private struct HomeRevealModifier: ViewModifier {
    let order: Int
    @State private var isVisible = false

    func body(content: Content) -> some View {
        content
            .opacity(isVisible ? 1 : 0)
            .offset(y: isVisible ? 0 : 18)
            .scaleEffect(isVisible ? 1 : 0.985)
            .animation(
                .spring(response: 0.52, dampingFraction: 0.88)
                .delay(Double(order) * 0.05),
                value: isVisible
            )
            .onAppear {
                isVisible = true
            }
    }
}

private extension View {
    func homeReveal(_ order: Int) -> some View {
        modifier(HomeRevealModifier(order: order))
    }
}

private func homeSpotifyTargetURL(for track: Track) -> URL? {
    if let spotifyTrackID = track.spotifyTrackID, !spotifyTrackID.isEmpty {
        return URL(string: "https://open.spotify.com/track/\(spotifyTrackID)")
    }

    if let spotifyArtistID = track.spotifyArtistID, !spotifyArtistID.isEmpty {
        return URL(string: "https://open.spotify.com/artist/\(spotifyArtistID)")
    }

    if let externalURL = track.externalURL,
       let url = URL(string: externalURL) {
        return url
    }

    return nil
}

private func homeSpotifyActionTitle(for track: Track) -> String {
    if let spotifyTrackID = track.spotifyTrackID, !spotifyTrackID.isEmpty {
        return "Song auf Spotify"
    }

    if let spotifyArtistID = track.spotifyArtistID, !spotifyArtistID.isEmpty {
        return "Artist auf Spotify"
    }

    if let externalURL = track.externalURL, externalURL.contains("/artist/") {
        return "Artist auf Spotify"
    }

    return "Spotify oeffnen"
}

private extension FeaturedHomeBeat {
    var asBeatHubItem: NicmaBeatHubItem {
        NicmaBeatHubItem(
            id: id,
            title: title,
            artistName: artistName,
            fileName: title,
            downloadURL: downloadURL,
            notes: notes,
            uploaderName: artistName,
            uploaderEmail: "",
            uploaderID: "",
            mimeType: "audio/mpeg",
            storagePath: "",
            isPublic: true,
            createdAt: .now
        )
    }
}

private struct HomeActionButton: View {
    let title: String
    let icon: String
    let colorScheme: ColorScheme
    let isPrimary: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.subheadline.weight(.bold))
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Spacer()
            }
            .foregroundColor(isPrimary ? .white : AppColors.text(for: colorScheme))
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(
                        isPrimary
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(
                        isPrimary
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.accent(for: colorScheme).opacity(0.16),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

final class HomeInlineVideoPlaybackManager: ObservableObject {
    @Published var currentVideoID: String?
    @Published var isPlaying = false
    let player = AVPlayer()
    private var playbackObserver: NSObjectProtocol?

    deinit {
        clearPlaybackObserver()
        player.pause()
        player.replaceCurrentItem(with: nil)
    }

    func prepare(video: FeaturedHomeVideo?) {
        guard let video,
              let url = URL(string: video.downloadURL) else {
            stop()
            player.replaceCurrentItem(with: nil)
            currentVideoID = nil
            return
        }

        guard currentVideoID != video.id || player.currentItem == nil else { return }

        clearPlaybackObserver()
        player.pause()
        player.replaceCurrentItem(with: AVPlayerItem(url: url))
        currentVideoID = video.id
        isPlaying = false
        observePlaybackFinished()
    }

    func togglePlayback(for video: FeaturedHomeVideo) {
        prepare(video: video)

        guard currentVideoID == video.id else { return }

        if isPlaying {
            stop()
        } else {
            player.play()
            isPlaying = true
        }
    }

    func stop() {
        player.pause()
        player.seek(to: .zero)
        isPlaying = false
    }

    private func observePlaybackFinished() {
        clearPlaybackObserver()
        guard let currentItem = player.currentItem else { return }

        playbackObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: currentItem,
            queue: .main
        ) { [weak self] _ in
            self?.player.seek(to: .zero)
            self?.player.pause()
            self?.isPlaying = false
        }
    }

    private func clearPlaybackObserver() {
        if let playbackObserver {
            NotificationCenter.default.removeObserver(playbackObserver)
            self.playbackObserver = nil
        }
    }
}

private struct ShopHeroCard: View {
    let colorScheme: ColorScheme
    let itemCount: Int
    let isStoreOpen: Bool
    let isLoggedIn: Bool
    let isAdmin: Bool
    let isUpdatingStoreState: Bool
    let onToggleStore: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 16) {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Merch Hub")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Global Drops, Apparel & Cart.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    Text(isStoreOpen ? "Kaufen ist aktuell freigeschaltet." : "Kaufen ist aktuell pausiert.")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(isStoreOpen ? AppColors.accent(for: colorScheme) : AppColors.accentMystic(for: colorScheme))
                }

                ZStack {
                    RoundedRectangle(cornerRadius: 18)
                        .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                        .frame(width: 58, height: 58)

                    Image(systemName: "bag.fill")
                        .font(.title2)
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
            }

            if let onToggleStore {
                Button(action: onToggleStore) {
                    Text(isUpdatingStoreState ? "Store wird aktualisiert..." : (isStoreOpen ? "Store schliessen" : "Store oeffnen"))
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(isStoreOpen ? AppColors.accentMystic(for: colorScheme) : AppColors.accent(for: colorScheme))
                .disabled(isUpdatingStoreState)
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 10) {
                ShopBadge(text: "\(itemCount) Produkte", colorScheme: colorScheme)
                ShopBadge(text: isStoreOpen ? "Store offen" : "Store pausiert", colorScheme: colorScheme)
                ShopBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
                if isAdmin {
                    ShopBadge(text: "Admin", colorScheme: colorScheme)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
    }

    private var cardBackground: some View {
        LinearGradient(
            colors: [
                AppColors.cardBackground(for: colorScheme),
                AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

private struct ShopInfoCard: View {
    let colorScheme: ColorScheme
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(message)
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let actionTitle, let action {
                Button(action: action) {
                    Text(actionTitle)
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct ShopBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(AppColors.accent(for: colorScheme).opacity(0.12))
            .foregroundColor(AppColors.accent(for: colorScheme))
            .clipShape(Capsule())
    }
}

#Preview {
    let services = AppServices()

    ShopView(
        authManager: services.authManager,
        merchandiseService: services.merchandiseService
    )
}
