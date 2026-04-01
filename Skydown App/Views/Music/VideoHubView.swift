// swiftlint:disable file_length
//
//  VideoHubView.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import AVFoundation
import AVKit
import SwiftUI
import UniformTypeIdentifiers

struct VideoHubView: View {
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = SkydownVideoHubViewModel()
    @StateObject private var playbackManager = VideoPlaybackManager()
    @State private var showingFileImporter = false
    @State private var showingUploadSheet = false
    @State private var showingReelViewer = false
    @State private var selectedYouTubeItem: SkydownYouTubeVideoItem?
    @State private var hasHandledInitialSelection = false
    let onBack: (() -> Void)?
    private let initialSelectedVideoID: String?
    private let autoplayInitialSelection: Bool

    init(
        onBack: (() -> Void)? = nil,
        initialSelectedVideoID: String? = nil,
        autoplayInitialSelection: Bool = false
    ) {
        self.onBack = onBack
        self.initialSelectedVideoID = initialSelectedVideoID
        self.autoplayInitialSelection = autoplayInitialSelection
    }

    private var selectedVideo: SkydownVideoHubItem? {
        viewModel.videos.first { $0.id == playbackManager.selectedVideoID } ?? viewModel.videos.first
    }

    private var selectedVideoIndex: Int {
        guard let selectedVideo else { return 0 }
        return viewModel.videos.firstIndex { $0.id == selectedVideo.id } ?? 0
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    heroCard
                    collaborationsCard
                    VideoEquipmentCard(
                        colorScheme: colorScheme,
                        items: viewModel.publicConfig.equipmentItems
                    )
                    VideoYouTubeCard(
                        colorScheme: colorScheme,
                        items: viewModel.publicConfig.youtubeItems
                    ) { item in
                        selectedYouTubeItem = item
                    }
                    .id(VideoHubQuickActionTarget.youtube.sectionID)
                    playerCard
                    libraryCard

                    if viewModel.isAdmin {
                        formatCard
                        VideoPublicConfigEditorCard(
                            colorScheme: colorScheme,
                            viewModel: viewModel
                        )
                    }
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding + 92)
            }
            .overlay(alignment: .bottomTrailing) {
                VideoHubQuickActionDock(
                    colorScheme: colorScheme,
                    showsUpload: viewModel.isAdmin,
                    onOpenYouTube: {
                        withAnimation(.snappy(duration: 0.35)) {
                            proxy.scrollTo(VideoHubQuickActionTarget.youtube.sectionID, anchor: .top)
                        }
                    },
                    onOpenUpload: {
                        showingUploadSheet = true
                    }
                )
                .padding(.trailing, SkydownLayout.screenHorizontalPadding)
                .padding(.bottom, 20)
            }
        }
        .scrollIndicators(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .skydownDismissKeyboardOnTap()
        .background(
            AppColors.screenGradient(
                for: colorScheme,
                secondaryAccent: AppColors.accentHighlight(for: colorScheme)
            )
            .ignoresSafeArea()
        )
        .navigationTitle("Video")
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

            ToolbarItemGroup(placement: .topBarTrailing) {
                if viewModel.isUploading {
                    ProgressView()
                        .controlSize(.small)
                }

                if viewModel.isAdmin {
                    Button {
                        showingUploadSheet = true
                    } label: {
                        Image(systemName: "arrow.up.circle")
                            .font(.headline.weight(.semibold))
                    }
                }
            }
        }
        .task {
            viewModel.configure(currentUser: authManager.userSession)
        }
        .onReceive(authManager.$userSession) { user in
            viewModel.configure(currentUser: user)
        }
        .onReceive(viewModel.$videos) { videos in
            activateInitialSelectionIfNeeded(with: videos)
        }
        .onDisappear {
            playbackManager.stop()
        }
        .fileImporter(
            isPresented: $showingFileImporter,
            allowedContentTypes: supportedVideoContentTypes,
            allowsMultipleSelection: true
        ) { result in
            viewModel.handleFileImport(result)
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .sheet(isPresented: $showingUploadSheet) {
            NavigationStack {
                ScrollView {
                    uploadCard
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        .padding(.top, SkydownLayout.screenTopPadding)
                        .padding(.bottom, SkydownLayout.screenBottomPadding)
                }
                .scrollDismissesKeyboard(.interactively)
                .skydownDismissKeyboardOnTap()
                .background(
                    AppColors.screenGradient(
                        for: colorScheme,
                        secondaryAccent: AppColors.accentHighlight(for: colorScheme)
                    )
                    .ignoresSafeArea()
                )
                .navigationTitle("Video Upload")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button("Schliessen") {
                            showingUploadSheet = false
                        }
                    }

                    if viewModel.isUploading {
                        ToolbarItem(placement: .topBarTrailing) {
                            ProgressView()
                                .controlSize(.small)
                        }
                    }
                }
            }
            .presentationDetents([.large])
        }
        .sheet(item: $selectedYouTubeItem) { item in
            YouTubeEmbedPlayerView(item: item)
        }
        .fullScreenCover(isPresented: $showingReelViewer) {
            if !viewModel.videos.isEmpty {
                VideoReelViewer(
                    videos: viewModel.videos,
                    initialIndex: selectedVideoIndex
                )
            }
        }
        .skydownKeyboardDismissToolbar()
    }

    @ViewBuilder
    private var collaborationsCard: some View {
        if !skydownProducedWithArtists.isEmpty {
            VStack(alignment: .leading, spacing: 14) {
                Text("Produced with")
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Artists und Creatives, mit denen in Produktionen zusammengearbeitet wurde.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                ForEach(skydownProducedWithArtists) { artist in
                    ProducedWithArtistRow(
                        artist: artist,
                        colorScheme: colorScheme
                    )
                }
            }
            .padding(SkydownLayout.cardPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .skydownPanelSurface(
                colorScheme: colorScheme,
                accent: AppColors.accentMystic(for: colorScheme),
                cornerRadius: SkydownLayout.cardCornerRadius,
                shadowRadius: 12,
                shadowYOffset: 6
            )
        }
    }

    private func activateInitialSelectionIfNeeded(with videos: [SkydownVideoHubItem]) {
        guard !videos.isEmpty else {
            playbackManager.stop()
            return
        }

        if !hasHandledInitialSelection,
           let initialSelectedVideoID,
           let video = videos.first(where: { $0.id == initialSelectedVideoID }) {
            playbackManager.load(video: video)
            if autoplayInitialSelection {
                playbackManager.togglePlayback(for: video)
            }
            hasHandledInitialSelection = true
            return
        }

        guard videos.contains(where: { $0.id == playbackManager.selectedVideoID }) else {
            playbackManager.load(video: videos.first)
            return
        }
    }

    private var heroCard: some View {
        BrandHeroSurface(
            colorScheme: colorScheme,
            eyebrow: "Video",
            title: "Video",
            subtitle: "Wenn du schauen willst, bist du hier richtig: Reels, Clips und neue Uploads.",
            detail: "Von hier aus gehst du direkt weiter zu YouTube, Equipment oder Kontakt.",
            accent: AppColors.accentMystic(for: colorScheme),
            secondaryAccent: AppColors.accentHighlight(for: colorScheme),
            marks: [.skydown]
        ) {
            HStack(spacing: 8) {
                MusicBadge(text: "Videos", isAccent: true)
                MusicBadge(text: "Equipment", isAccent: false)
                MusicBadge(text: "YouTube", isAccent: false)
                MusicBadge(text: "Collabs", isAccent: false)
            }
        }
    }

    private var formatCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Format-Hinweis")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Empfohlen sind MP4, MOV oder M4V. Am stabilsten laufen H.264 oder H.265 mit sauberem Export fuer mobile Wiedergabe.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text("Querformat und Hochformat funktionieren beide. Fuer schnellere Uploads sind komprimierte Social-Cuts besser als rohe Master-Dateien.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accent(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 12,
            shadowYOffset: 6
        )
    }

    private var uploadCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Video Upload")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Nur Admins sehen diesen Bereich. Die Videos landen direkt in Firebase Storage und erscheinen danach in der Library.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            NicmaUploadField(
                title: "Titel",
                text: $viewModel.videoTitle,
                colorScheme: colorScheme
            )

            NicmaUploadField(
                title: "Projekt / Artist",
                text: $viewModel.projectName,
                colorScheme: colorScheme
            )

            NicmaUploadField(
                title: "E-Mail",
                text: $viewModel.email,
                colorScheme: colorScheme,
                keyboard: .emailAddress,
                autocapitalization: .never
            )

            VStack(alignment: .leading, spacing: 8) {
                Text("Notiz (optional)")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                TextEditor(text: $viewModel.notes)
                    .frame(minHeight: 110)
                    .padding(12)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                    )
            }

            Button {
                showingFileImporter = true
            } label: {
                Label("Videos auswaehlen", systemImage: "video.badge.plus")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.bordered)
            .tint(AppColors.accentMystic(for: colorScheme))

            if !viewModel.selectedFiles.isEmpty {
                VStack(spacing: 10) {
                    ForEach(viewModel.selectedFiles) { file in
                        SkydownSelectedVideoRow(
                            file: file,
                            colorScheme: colorScheme
                        ) {
                            viewModel.removeFile(file.id)
                        }
                    }
                }
            }

            if let validationMessage = viewModel.validationMessage {
                Text(validationMessage)
                    .font(.footnote)
                    .foregroundColor(.red)
            }

            Button {
                Task {
                    await viewModel.uploadSelectedVideos()
                }
            } label: {
                if viewModel.isUploading {
                    ProgressView()
                        .tint(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                } else {
                    Label("Videos hochladen", systemImage: "arrow.up.circle.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
            }
            .background(AppColors.accent(for: colorScheme))
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .disabled(!viewModel.canUpload)
            .opacity(viewModel.canUpload ? 1 : 0.6)
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

    private var playerCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Reel Player")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Der Clip bleibt jetzt gross, vertikal und direkt im Fokus. Fuer den ganzen Feed kannst du jederzeit in den Reel-Modus springen.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let selectedVideo {
                ZStack(alignment: .bottomLeading) {
                    VideoPlayer(player: playbackManager.player)
                        .frame(height: 560)
                        .clipShape(RoundedRectangle(cornerRadius: 24))

                    LinearGradient(
                        colors: [
                            .clear,
                            Color.black.opacity(0.24),
                            Color.black.opacity(0.78)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 24))

                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 8) {
                            MusicBadge(text: "Reel", isAccent: true)
                            MusicBadge(text: selectedVideo.projectName, isAccent: false)
                        }

                        Text(selectedVideo.title)
                            .font(.title3.weight(.bold))
                            .foregroundColor(.white)

                        if !selectedVideo.notes.isEmpty {
                            Text(selectedVideo.notes)
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.78))
                                .lineLimit(3)
                        }
                    }
                    .padding(18)
                }

                Button {
                    playbackManager.player.pause()
                    showingReelViewer = true
                } label: {
                    Label("Im Reel ansehen", systemImage: "rectangle.portrait.and.arrow.right")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accentMystic(for: colorScheme))

                if !selectedVideo.notes.isEmpty {
                    Text(selectedVideo.notes)
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else {
                Text("Noch kein Video ausgewaehlt. Sobald Uploads live sind, kannst du sie hier direkt abspielen.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
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

    private var libraryCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Video Library")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if viewModel.isLoadingVideos {
                ProgressView("Videos werden geladen ...")
            } else if viewModel.videos.isEmpty {
                Text(
                    viewModel.isAdmin
                    ? "Noch keine Videos im Hub. Neue Uploads tauchen hier sofort auf."
                    : "Noch keine freigegebenen Videos. Sobald ein Clip live ist, kannst du ihn hier abspielen."
                )
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else {
                ForEach(viewModel.videos) { video in
                    VideoHubLibraryRow(
                        video: video,
                        isAdmin: viewModel.isAdmin,
                        isSelected: playbackManager.selectedVideoID == video.id,
                        isPlaying: playbackManager.playingVideoID == video.id,
                        colorScheme: colorScheme,
                        onSelect: { playbackManager.load(video: video) },
                        onPlayToggle: { playbackManager.togglePlayback(for: video) },
                        onOpenReel: {
                            playbackManager.load(video: video)
                            playbackManager.player.pause()
                            showingReelViewer = true
                        },
                        onToggleHomeFeatured: {
                            Task {
                                await viewModel.toggleHomeFeatured(video)
                            }
                        },
                        onDelete: {
                            Task {
                                await viewModel.deleteVideo(video)
                            }
                        }
                    )
                }
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
}

struct VideoHubLibraryRow: View {
    let video: SkydownVideoHubItem
    let isAdmin: Bool
    let isSelected: Bool
    let isPlaying: Bool
    let colorScheme: ColorScheme
    let onSelect: () -> Void
    let onPlayToggle: () -> Void
    let onOpenReel: () -> Void
    let onToggleHomeFeatured: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    Circle()
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                        .frame(width: 44, height: 44)

                    Image(systemName: isSelected ? "play.rectangle.fill" : "video.fill")
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(video.title)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("\(video.projectName) • \(skydownVideoDateFormatter.string(from: video.createdAt))")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if !video.notes.isEmpty {
                        Text(video.notes)
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }

                Spacer()
            }

            HStack(spacing: 8) {
                MusicBadge(text: video.isPublic ? "Public" : "Private", isAccent: video.isPublic)
                if isAdmin && video.isHomeFeatured {
                    MusicBadge(text: "Home", isAccent: true)
                }
                if isAdmin {
                    MusicBadge(text: video.fileName, isAccent: false)
                } else {
                    MusicBadge(text: "Clip", isAccent: false)
                }
            }

            if isAdmin {
                HStack(spacing: 10) {
                    Button(action: onSelect) {
                        Label(isSelected ? "Im Player" : "Auswaehlen", systemImage: "rectangle.on.rectangle")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button(action: onPlayToggle) {
                        Label(isPlaying ? "Stoppen" : "Abspielen", systemImage: isPlaying ? "stop.fill" : "play.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accent(for: colorScheme))
                    .disabled(!video.isPlayable)
                }
            } else {
                Button(action: onOpenReel) {
                    Label("Direkt im Reel", systemImage: "play.rectangle.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accentMystic(for: colorScheme))
                .disabled(!video.isPlayable)
            }

            if isAdmin {
                HStack(spacing: 10) {
                    Button(action: onToggleHomeFeatured) {
                        Label(video.isHomeFeatured ? "Home aktiv" : "Im Home zeigen", systemImage: "house.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button(role: .destructive, action: onDelete) {
                        Label("Loeschen", systemImage: "trash")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct VideoReelViewer: View {
    let videos: [SkydownVideoHubItem]
    let initialIndex: Int
    @Environment(\.dismiss) private var dismiss
    @State private var currentIndex: Int
    @State private var player = AVPlayer()

    init(
        videos: [SkydownVideoHubItem],
        initialIndex: Int
    ) {
        self.videos = videos
        self.initialIndex = initialIndex
        _currentIndex = State(initialValue: initialIndex)
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .top) {
                Color.black
                    .ignoresSafeArea()

                TabView(selection: $currentIndex) {
                    ForEach(Array(videos.enumerated()), id: \.element.id) { index, video in
                        ZStack(alignment: .bottomLeading) {
                            if index == currentIndex {
                                VideoPlayer(player: player)
                                    .ignoresSafeArea()
                            } else {
                                Rectangle()
                                    .fill(Color.black)
                                    .overlay {
                                        VStack(spacing: 12) {
                                            Image(systemName: "play.rectangle.fill")
                                                .font(.system(size: 48, weight: .bold))
                                                .foregroundColor(.white.opacity(0.72))

                                            Text(video.title)
                                                .font(.headline.weight(.bold))
                                                .foregroundColor(.white)

                                            Text(video.projectName)
                                                .font(.subheadline)
                                                .foregroundColor(.white.opacity(0.68))
                                        }
                                    }
                            }

                            LinearGradient(
                                colors: [
                                    .clear,
                                    Color.black.opacity(0.20),
                                    Color.black.opacity(0.84)
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            )

                            VStack(alignment: .leading, spacing: 8) {
                                Text(video.projectName.uppercased())
                                    .font(.caption.weight(.semibold))
                                    .foregroundColor(.white.opacity(0.72))

                                Text(video.title)
                                    .font(.title2.weight(.bold))
                                    .foregroundColor(.white)

                                if !video.notes.isEmpty {
                                    Text(video.notes)
                                        .font(.subheadline)
                                        .foregroundColor(.white.opacity(0.78))
                                        .lineLimit(4)
                                }
                            }
                            .padding(.horizontal, 22)
                            .padding(.bottom, 34)
                        }
                        .rotationEffect(.degrees(90))
                        .frame(width: proxy.size.width, height: proxy.size.height)
                        .tag(index)
                    }
                }
                .frame(width: proxy.size.height, height: proxy.size.width)
                .rotationEffect(.degrees(-90))
                .offset(
                    x: (proxy.size.width - proxy.size.height) / 2,
                    y: (proxy.size.height - proxy.size.width) / 2
                )
                .tabViewStyle(.page(indexDisplayMode: .never))

                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Skydown Reel")
                            .font(.headline.weight(.bold))
                            .foregroundColor(.white)

                        Text("\(currentIndex + 1) von \(videos.count)")
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.72))
                    }

                    Spacer()

                    Button(action: { dismiss() }, label: {
                        Image(systemName: "xmark")
                            .font(.headline.weight(.bold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(Color.white.opacity(0.14))
                            .clipShape(Circle())
                    })
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.top, 18)
            }
        }
        .onAppear {
            playCurrent()
        }
        .onChange(of: currentIndex) { _, _ in
            playCurrent()
        }
        .onDisappear {
            player.pause()
            player.replaceCurrentItem(with: nil)
        }
    }

    private func playCurrent() {
        guard videos.indices.contains(currentIndex),
              let url = URL(string: videos[currentIndex].downloadURL) else {
            player.pause()
            player.replaceCurrentItem(with: nil)
            return
        }

        player.pause()
        player.replaceCurrentItem(with: AVPlayerItem(url: url))
        player.seek(to: .zero)
        player.play()
    }
}

struct VideoEquipmentCard: View {
    let colorScheme: ColorScheme
    let items: [SkydownVideoEquipmentItem]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Equipment & Software")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Damit direkt klar ist, womit Skydown die Videography umsetzt.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(spacing: 10) {
                ForEach(items) { item in
                    VideoEquipmentRow(
                        title: item.title,
                        detail: item.detail,
                        colorScheme: colorScheme
                    )
                }
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
}

struct VideoYouTubeCard: View {
    let colorScheme: ColorScheme
    let items: [SkydownYouTubeVideoItem]
    let onPlayItem: (SkydownYouTubeVideoItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("YouTube")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Hier koennen oeffentliche YouTube-Arbeiten, Making-ofs oder Musikvideos gesammelt werden.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if items.isEmpty {
                Text("Noch keine YouTube-Videos hinterlegt.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .padding(.top, 4)
            } else {
                VStack(spacing: 10) {
                    ForEach(items) { item in
                        VideoYouTubeRow(
                            item: item,
                            colorScheme: colorScheme
                        ) {
                            onPlayItem(item)
                        }
                    }
                }
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
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }
}

private enum VideoHubQuickActionTarget {
    case youtube

    var sectionID: String {
        switch self {
        case .youtube:
            return "video-youtube-section"
        }
    }
}

private struct VideoHubQuickActionDock: View {
    let colorScheme: ColorScheme
    let showsUpload: Bool
    let onOpenYouTube: () -> Void
    let onOpenUpload: () -> Void

    var body: some View {
        VStack(alignment: .trailing, spacing: 10) {
            if showsUpload {
                VideoHubQuickActionButton(
                    title: "Upload",
                    systemImage: "arrow.up.circle.fill",
                    tint: AppColors.accent(for: colorScheme),
                    textColor: .white,
                    action: onOpenUpload
                )
            }

            VideoHubQuickActionButton(
                title: "YouTube",
                systemImage: "play.rectangle.fill",
                tint: AppColors.accentHighlight(for: colorScheme),
                textColor: AppColors.text(for: colorScheme),
                action: onOpenYouTube
            )
        }
    }
}

private struct VideoHubQuickActionButton: View {
    let title: String
    let systemImage: String
    let tint: Color
    let textColor: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
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
            .shadow(color: tint.opacity(0.22), radius: 14, y: 8)
        }
        .buttonStyle(.plain)
    }
}

struct VideoEquipmentRow: View {
    let title: String
    let detail: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(detail)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

struct ProducedWithArtistRow: View {
    let artist: SkydownProducedWithArtist
    let colorScheme: ColorScheme

    private var spotifyURL: URL? {
        guard let spotifyArtistID = artist.spotifyArtistID, !spotifyArtistID.isEmpty else {
            return nil
        }

        return URL(string: "https://open.spotify.com/artist/\(spotifyArtistID)")
    }

    private var instagramURL: URL? {
        guard let instagramURLString = artist.instagramURLString, !instagramURLString.isEmpty else {
            return nil
        }

        return URL(string: instagramURLString)
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(artist.name)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(artist.role)
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer()

            HStack(spacing: 8) {
                if let spotifyURL {
                    SocialLinkButton(
                        title: "Spotify",
                        systemImage: "music.note",
                        foregroundColor: .white,
                        background: LinearGradient(
                            colors: [
                                AppColors.spotify(for: colorScheme),
                                AppColors.spotify(for: colorScheme).opacity(0.72)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        destination: spotifyURL
                    )
                }

                if let instagramURL {
                    SocialLinkButton(
                        title: "Instagram",
                        systemImage: "camera.fill",
                        foregroundColor: .white,
                        background: LinearGradient(
                            colors: [
                                AppColors.instagramStart(for: colorScheme),
                                AppColors.instagramEnd(for: colorScheme)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        destination: instagramURL
                    )
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct SocialLinkButton<Background: View>: View {
    let title: String
    let systemImage: String
    let foregroundColor: Color
    let background: Background
    let destination: URL

    var body: some View {
        Link(destination: destination) {
            Label(title, systemImage: systemImage)
                .font(.caption.weight(.bold))
                .foregroundColor(foregroundColor)
                .padding(.horizontal, 13)
                .padding(.vertical, 9)
                .background(background)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

struct VideoYouTubeRow: View {
    let item: SkydownYouTubeVideoItem
    let colorScheme: ColorScheme
    let onPlay: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                if !item.subtitle.isEmpty {
                    Text(item.subtitle)
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            Spacer()

            Button(action: onPlay) {
                Label("Abspielen", systemImage: "play.rectangle.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

struct VideoPublicConfigEditorCard: View {
    let colorScheme: ColorScheme
    @ObservedObject var viewModel: SkydownVideoHubViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Videography Editor")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Admins steuern hier die oeffentliche Equipment-Liste und die YouTube-Sparte fuer alle Nutzer.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(alignment: .leading, spacing: 12) {
                Text("Equipment")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                ForEach(Array(viewModel.publicConfig.equipmentItems.enumerated()), id: \.element.id) { _, item in
                    VStack(alignment: .leading, spacing: 8) {
                        NicmaUploadField(
                            title: "Titel",
                            text: Binding(
                                get: { item.title },
                                set: { viewModel.updateEquipmentItem(item.id, title: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: "Detail",
                            text: Binding(
                                get: { item.detail },
                                set: { viewModel.updateEquipmentItem(item.id, detail: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        Button(role: .destructive) {
                            viewModel.removeEquipmentItem(item.id)
                        } label: {
                            Label("Eintrag entfernen", systemImage: "trash")
                                .font(.caption.weight(.semibold))
                        }
                    }
                    .padding(14)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }

                Button {
                    viewModel.addEquipmentItem()
                } label: {
                    Label("Equipment hinzufuegen", systemImage: "plus.circle.fill")
                        .font(.subheadline.weight(.semibold))
                }
                .buttonStyle(.bordered)
                .tint(AppColors.accent(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 12) {
                Text("YouTube")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                ForEach(Array(viewModel.publicConfig.youtubeItems.enumerated()), id: \.element.id) { _, item in
                    VStack(alignment: .leading, spacing: 8) {
                        NicmaUploadField(
                            title: "Titel",
                            text: Binding(
                                get: { item.title },
                                set: { viewModel.updateYouTubeItem(item.id, title: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: "Untertitel",
                            text: Binding(
                                get: { item.subtitle },
                                set: { viewModel.updateYouTubeItem(item.id, subtitle: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: "URL",
                            text: Binding(
                                get: { item.urlString },
                                set: { viewModel.updateYouTubeItem(item.id, urlString: $0) }
                            ),
                            colorScheme: colorScheme,
                            keyboard: .URL,
                            autocapitalization: .never
                        )
                        Button(role: .destructive) {
                            viewModel.removeYouTubeItem(item.id)
                        } label: {
                            Label("Eintrag entfernen", systemImage: "trash")
                                .font(.caption.weight(.semibold))
                        }
                    }
                    .padding(14)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }

                Button {
                    viewModel.addYouTubeItem()
                } label: {
                    Label("YouTube-Video hinzufuegen", systemImage: "plus.circle.fill")
                        .font(.subheadline.weight(.semibold))
                }
                .buttonStyle(.bordered)
                .tint(AppColors.accentHighlight(for: colorScheme))
            }

            Button {
                Task {
                    await viewModel.savePublicConfig()
                }
            } label: {
                if viewModel.isSavingPublicConfig {
                    ProgressView()
                        .tint(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                } else {
                    Label("Oeffentliche Daten speichern", systemImage: "square.and.arrow.down.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accentMystic(for: colorScheme))
            .disabled(viewModel.isSavingPublicConfig)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }
}

struct SkydownSelectedVideoRow: View {
    let file: SkydownSelectedVideoFile
    let colorScheme: ColorScheme
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 42, height: 42)

                Image(systemName: "video.fill")
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 4) {
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
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

final class VideoPlaybackManager: ObservableObject {
    @Published var selectedVideoID: String?
    @Published var playingVideoID: String?
    let player = AVPlayer()
    private var playbackObserver: NSObjectProtocol?

    deinit {
        stop()
    }

    func load(video: SkydownVideoHubItem?) {
        clearPlaybackObserver()
        player.pause()

        guard let video,
              let url = URL(string: video.downloadURL) else {
            player.replaceCurrentItem(with: nil)
            selectedVideoID = nil
            playingVideoID = nil
            return
        }

        if selectedVideoID != video.id || player.currentItem == nil {
            player.replaceCurrentItem(with: AVPlayerItem(url: url))
        }

        selectedVideoID = video.id
        playingVideoID = nil
        player.seek(to: .zero)
    }

    func togglePlayback(for video: SkydownVideoHubItem) {
        guard video.isPlayable else { return }

        if selectedVideoID != video.id || player.currentItem == nil {
            play(video: video)
            return
        }

        if playingVideoID == video.id {
            player.pause()
            playingVideoID = nil
        } else {
            observePlaybackFinished()
            player.play()
            playingVideoID = video.id
        }
    }

    func play(video: SkydownVideoHubItem) {
        guard video.isPlayable else { return }
        load(video: video)
        observePlaybackFinished()
        player.play()
        playingVideoID = video.id
    }

    func stop() {
        player.pause()
        player.replaceCurrentItem(with: nil)
        clearPlaybackObserver()
        selectedVideoID = nil
        playingVideoID = nil
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
            self?.playingVideoID = nil
        }
    }

    private func clearPlaybackObserver() {
        if let playbackObserver {
            NotificationCenter.default.removeObserver(playbackObserver)
            self.playbackObserver = nil
        }
    }
}
