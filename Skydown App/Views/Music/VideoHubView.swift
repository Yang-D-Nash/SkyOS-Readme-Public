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
import WebKit

struct VideoHubView: View {
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared
    @StateObject private var viewModel = SkydownVideoHubViewModel()
    @StateObject private var playbackManager = VideoPlaybackManager()
    @State private var showingFileImporter = false
    @State private var showingUploadComposer = false
    @State private var activePresentedSheet: VideoHubPresentedSheet?
    @State private var queuedPresentedSheet: VideoHubPresentedSheet?
    @State private var showingReelViewer = false
    @State private var hasHandledInitialSelection = false
    @State private var showingAdminEditor = false
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
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                heroCard
                if viewModel.isAdmin && showingUploadComposer {
                    uploadCard
                        .transition(.move(edge: .top).combined(with: .opacity))
                }
                VideoEquipmentCard(
                    colorScheme: colorScheme,
                    items: viewModel.publicConfig.equipmentItems
                )
                playerCard
                libraryCard
                VideoYouTubeCard(
                    colorScheme: colorScheme,
                    items: viewModel.publicConfig.youtubeItems
                ) { item in
                    presentSheet(.youTube(item))
                }
                collaborationsCard

                if viewModel.isAdmin {
                    adminToolsCard
                }
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.top, SkydownLayout.screenTopPadding)
            .padding(.bottom, SkydownLayout.screenBottomPadding + (viewModel.isAdmin ? 92 : 0))
        }
        .overlay(alignment: .bottomTrailing) {
            if viewModel.isAdmin {
                VideoHubQuickActionDock(
                    colorScheme: colorScheme,
                    isUploadOpen: showingUploadComposer
                ) {
                    withAnimation(.spring(response: 0.32, dampingFraction: 0.86)) {
                        showingUploadComposer.toggle()
                    }
                }
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
                        withAnimation(.spring(response: 0.32, dampingFraction: 0.86)) {
                            showingUploadComposer.toggle()
                        }
                    } label: {
                        Image(systemName: showingUploadComposer ? "xmark.circle" : "arrow.up.circle")
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
        .sheet(item: $activePresentedSheet) { sheet in
            switch sheet {
            case .youTube(let item):
                YouTubeEmbedPlayerView(item: item)
            }
        }
        .sheet(isPresented: $showingAdminEditor) {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    formatCard
                    VideoPublicConfigEditorCard(
                        colorScheme: colorScheme,
                        viewModel: viewModel
                    )
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding)
            }
            .background(
                AppColors.screenGradient(
                    for: colorScheme,
                    secondaryAccent: AppColors.accentHighlight(for: colorScheme)
                )
                .ignoresSafeArea()
            )
        }
        .fullScreenCover(isPresented: $showingReelViewer) {
            if !viewModel.videos.isEmpty {
                VideoReelViewer(
                    videos: viewModel.videos,
                    initialIndex: selectedVideoIndex
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
        .skydownKeyboardDismissToolbar()
    }

    private func presentSheet(_ sheet: VideoHubPresentedSheet) {
        guard activePresentedSheet == nil else {
            queuedPresentedSheet = sheet
            activePresentedSheet = nil
            return
        }

        activePresentedSheet = sheet
    }

    @ViewBuilder
    private var collaborationsCard: some View {
        let collaborationItems = viewModel.publicConfig.collaborationItems
        VStack(alignment: .leading, spacing: 14) {
            Text("Featured Collabs")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Artists und Creatives, mit denen die Visuals entstehen.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if collaborationItems.isEmpty {
                Text("Noch keine Featured Collabs hinterlegt.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .padding(.top, 4)
            } else {
                ForEach(collaborationItems) { artist in
                    ProducedWithArtistRow(
                        artist: artist,
                        colorScheme: colorScheme
                    ) { urlString in
                        presentSheet(.youTube(SkydownYouTubeVideoItem(
                            id: "collab-\(artist.id)",
                            title: artist.name,
                            subtitle: artist.highlight.isEmpty ? artist.role : artist.highlight,
                            urlString: urlString
                        )))
                    }
                }
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
            eyebrow: screenHeaderSettingsStore.settings.resolvedVideoHubEyebrow ?? "Video",
            title: screenHeaderSettingsStore.settings.resolvedVideoHubTitle ?? "Video",
            subtitle: screenHeaderSettingsStore.settings.resolvedVideoHubSubtitle ?? "Clips, Visuals und starke Kollaborationen.",
            detail: screenHeaderSettingsStore.settings.resolvedVideoHubDetail ?? "Clips, Looks und Leute hinter dem Vibe.",
            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedVideoHubImageURL,
            accent: AppColors.accentMystic(for: colorScheme),
            secondaryAccent: AppColors.accentHighlight(for: colorScheme),
            marks: [.skydown]
        ) {
            HStack(spacing: 8) {
                MusicBadge(text: "Videos", isAccent: true)
                MusicBadge(text: "Equipment", isAccent: false)
                MusicBadge(text: "Collabs", isAccent: false)
            }
        }
    }

    private var formatCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Format-Hinweis")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("MP4, MOV oder M4V.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text("Komprimierte Cuts laden schneller.")
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

    private var adminToolsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Video Admin")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Equipment, Featured Collabs und Format-Hinweise kompakt im Editor.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Button {
                showingAdminEditor = true
            } label: {
                Label("Editor oeffnen", systemImage: "slider.horizontal.3")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accentMystic(for: colorScheme))
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

    private var uploadCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Video Upload")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Nur fuer Admins.")
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

            Text("Oder als externer Video-Link freigeben.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            NicmaUploadField(
                title: "Google Drive / MEGA / anderer Video-Link",
                text: $viewModel.externalVideoURL,
                colorScheme: colorScheme,
                keyboard: .URL,
                autocapitalization: .never
            )
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

            Button {
                Task {
                    await viewModel.addExternalVideo()
                }
            } label: {
                Label("Externes Video freigeben", systemImage: "link.badge.plus")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.bordered)
            .disabled(!viewModel.canAddExternalVideo)
            .opacity(viewModel.canAddExternalVideo ? 1 : 0.6)
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
            Text("Video Player")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Der Clip bleibt jetzt gross, vertikal und direkt im Fokus. Fuer den ganzen Feed kannst du jederzeit in den Video-Modus springen.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let selectedVideo {
                ZStack(alignment: .bottomLeading) {
                    Group {
                        if selectedVideo.youTubeItem != nil {
                            ZStack {
                                RoundedRectangle(cornerRadius: 24)
                                    .fill(
                                        LinearGradient(
                                            colors: [
                                                Color(red: 0.18, green: 0.02, blue: 0.03),
                                                Color(red: 0.45, green: 0.05, blue: 0.06),
                                                Color.black
                                            ],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )

                                VStack(spacing: 16) {
                                    Image(systemName: "play.rectangle.fill")
                                        .font(.system(size: 54, weight: .bold))
                                        .foregroundColor(.white.opacity(0.95))

                                    Text("YouTube Clip")
                                        .font(.title3.weight(.bold))
                                        .foregroundColor(.white)

                                    Text("Der Clip ist als YouTube-Video hinterlegt und laeuft direkt in der App.")
                                        .font(.subheadline.weight(.semibold))
                                        .multilineTextAlignment(.center)
                                        .foregroundColor(.white.opacity(0.84))
                                        .padding(.horizontal, 28)
                                }
                            }
                        } else if selectedVideo.usesEmbeddedPreview {
                            ExternalVideoEmbedSurface(urlString: selectedVideo.embedURL)
                        } else if selectedVideo.isPlayable {
                            VideoPlayer(player: playbackManager.player)
                        } else {
                            ZStack {
                                RoundedRectangle(cornerRadius: 24)
                                    .fill(AppColors.secondaryBackground(for: colorScheme))

                                VStack(spacing: 12) {
                                    Image(systemName: "link")
                                        .font(.system(size: 34, weight: .bold))
                                        .foregroundColor(.white.opacity(0.86))

                                    Text("Dieser Clip wird ueber einen externen Link ausgeliefert.")
                                        .font(.subheadline.weight(.semibold))
                                        .multilineTextAlignment(.center)
                                        .foregroundColor(.white.opacity(0.86))
                                        .padding(.horizontal, 28)
                                }
                            }
                        }
                    }
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
                            MusicBadge(text: "Video", isAccent: true)
                            MusicBadge(text: selectedVideo.projectName, isAccent: false)
                            MusicBadge(text: selectedVideo.provider.badgeLabel, isAccent: false)
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

                if let youTubeItem = selectedVideo.youTubeItem {
                    Button {
                        presentSheet(.youTube(youTubeItem))
                    } label: {
                        Label("In YouTube ansehen", systemImage: "play.rectangle.fill")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.youtube(for: colorScheme))
                } else if selectedVideo.supportsInlinePlayback {
                    Button {
                        playbackManager.player.pause()
                        activePresentedSheet = nil
                        showingReelViewer = true
                    } label: {
                        Label("Im Video ansehen", systemImage: "rectangle.portrait.and.arrow.right")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accentMystic(for: colorScheme))
                }

                if let originalURL = URL(string: selectedVideo.openURLString), !selectedVideo.openURLString.isEmpty {
                    Button {
                        openURL(originalURL)
                    } label: {
                        Label("Original oeffnen", systemImage: "arrow.up.forward.square")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.bordered)
                }

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
                            activePresentedSheet = nil
                            showingReelViewer = true
                        },
                        onOpenOriginal: {
                            if let url = URL(string: video.openURLString), !video.openURLString.isEmpty {
                                openURL(url)
                            }
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
    let onOpenOriginal: () -> Void
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
                MusicBadge(text: video.provider.badgeLabel, isAccent: false)
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

                    Button(action: video.isPlayable ? onPlayToggle : onOpenReel) {
                        Label(
                            video.isPlayable
                                ? (isPlaying ? "Stoppen" : "Abspielen")
                                : (video.supportsInlinePlayback ? "Ansehen" : "Oeffnen"),
                            systemImage: video.isPlayable
                                ? (isPlaying ? "stop.fill" : "play.fill")
                                : (video.supportsInlinePlayback ? "play.rectangle.fill" : "arrow.up.forward.square")
                        )
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(video.provider == .youTube ? AppColors.youtube(for: colorScheme) : AppColors.accent(for: colorScheme))
                    .disabled(!video.supportsInlinePlayback && video.openURLString.isEmpty)
                }
            } else {
                VStack(spacing: 10) {
                    if video.supportsInlinePlayback {
                        Button(action: onOpenReel) {
                            Label("Direkt im Video", systemImage: "play.rectangle.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.accentMystic(for: colorScheme))
                    }

                    if !video.openURLString.isEmpty {
                        Button(action: onOpenOriginal) {
                            Label("Original oeffnen", systemImage: "arrow.up.forward.square")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                }
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
                            if index == currentIndex && video.usesEmbeddedPreview {
                                ExternalVideoEmbedSurface(urlString: video.embedURL)
                                    .ignoresSafeArea()
                            } else if index == currentIndex && video.isPlayable {
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
                        Text("Skydown Video")
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
                    .skydownTactileAction()
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
              let url = URL(string: videos[currentIndex].nativePlaybackURLString),
              !videos[currentIndex].nativePlaybackURLString.isEmpty else {
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

            Text("Setup.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if items.isEmpty {
                Text("Noch kein Setup hinterlegt.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .padding(.top, 4)
            } else {
                VStack(spacing: 10) {
                    ForEach(items) { item in
                        VideoEquipmentRow(
                            item: item,
                            colorScheme: colorScheme
                        )
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

            Text("Videos & Making-ofs.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if items.isEmpty {
                Text("Noch nichts hinterlegt.")
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

private struct VideoHubQuickActionDock: View {
    let colorScheme: ColorScheme
    let isUploadOpen: Bool
    let onOpenUpload: () -> Void

    var body: some View {
        VStack(alignment: .trailing, spacing: 10) {
            VideoHubQuickActionButton(
                title: isUploadOpen ? "Schliessen" : "Upload",
                systemImage: isUploadOpen ? "xmark.circle.fill" : "arrow.up.circle.fill",
                tint: AppColors.accent(for: colorScheme),
                textColor: .white,
                action: onOpenUpload
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
        .skydownTactileAction()
    }
}

struct VideoEquipmentRow: View {
    let item: SkydownVideoEquipmentItem
    let colorScheme: ColorScheme

    private var imageURL: URL? {
        guard let value = item.imageURLString, !value.isEmpty else { return nil }
        return URL(string: value)
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            equipmentArtwork

            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(item.detail)
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private var equipmentArtwork: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16)
                .fill(
                    LinearGradient(
                        colors: [
                            AppColors.accentMystic(for: colorScheme).opacity(0.88),
                            AppColors.accent(for: colorScheme).opacity(0.68)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            if let imageURL {
                AsyncImage(url: imageURL) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        fallbackEquipmentArtwork
                    }
                }
            } else {
                fallbackEquipmentArtwork
            }
        }
        .frame(width: 72, height: 72)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var fallbackEquipmentArtwork: some View {
        Image(systemName: "camera.metering.spot")
            .font(.title3.weight(.bold))
            .foregroundColor(.white.opacity(0.9))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ProducedWithArtistRow: View {
    let artist: SkydownProducedWithArtist
    let colorScheme: ColorScheme
    let onOpenYouTube: (String) -> Void

    private var imageURL: URL? {
        guard let imageURLString = artist.imageURLString, !imageURLString.isEmpty else {
            return nil
        }

        return URL(string: imageURLString)
    }

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

    private var youtubeURL: URL? {
        guard let youtubeURLString = artist.youtubeURLString, !youtubeURLString.isEmpty else {
            return nil
        }

        return URL(string: youtubeURLString)
    }

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            collaborationBackground

            LinearGradient(
                colors: [
                    Color.black.opacity(0.18),
                    Color.clear,
                    Color.black.opacity(0.82)
                ],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .top, spacing: 10) {
                    Text(artist.role.uppercased())
                        .font(.caption2.weight(.bold))
                        .foregroundColor(.white.opacity(0.94))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(.black.opacity(0.34), in: Capsule())

                    Spacer(minLength: 0)

                    if !artist.vibe.isEmpty {
                        Text(artist.vibe)
                            .font(.caption.weight(.bold))
                            .foregroundColor(.white)
                            .lineLimit(1)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 5)
                            .background(AppColors.accent(for: colorScheme).opacity(0.72), in: Capsule())
                    }
                }

                Spacer(minLength: 8)

                VStack(alignment: .leading, spacing: 4) {
                    Text(artist.name)
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(.white)
                        .lineLimit(1)
                        .shadow(color: .black.opacity(0.35), radius: 12, y: 4)

                    if !artist.highlight.isEmpty {
                        Text(artist.highlight)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.92))
                            .lineLimit(1)
                            .shadow(color: .black.opacity(0.30), radius: 10, y: 4)
                    }
                }

                HStack(spacing: 8) {
                    if let spotifyURL {
                        SocialLinkButton(
                            accessibilityTitle: "Spotify",
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
                            accessibilityTitle: "Instagram",
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

                    if let youtubeURL {
                        SocialLinkButton(
                            accessibilityTitle: "YouTube",
                            systemImage: "play.rectangle.fill",
                            foregroundColor: .white,
                            background: LinearGradient(
                                colors: [
                                    Color(red: 0.78, green: 0.14, blue: 0.12),
                                    Color(red: 0.55, green: 0.07, blue: 0.08)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            destination: youtubeURL
                        ) {
                                onOpenYouTube(youtubeURL.absoluteString)
                        }
                    }
                }
            }
            .padding(12)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(height: 164)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.97 : 0.99),
                    AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.74 : 0.68)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.10), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private var collaborationBackground: some View {
        Group {
            if let imageURL {
                AsyncImage(url: imageURL) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        fallbackArtwork
                    }
                }
            } else {
                fallbackArtwork
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var fallbackArtwork: some View {
        VStack(spacing: 8) {
            Spacer()

            Text(String(artist.name.prefix(1)).uppercased())
                .font(.title.weight(.black))
                .foregroundColor(.white)

            if !artist.vibe.isEmpty {
                Text(artist.vibe.uppercased())
                    .font(.caption2.weight(.bold))
                    .foregroundColor(.white.opacity(0.86))
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(
            LinearGradient(
                colors: [
                    AppColors.accentMystic(for: colorScheme).opacity(0.94),
                    AppColors.accent(for: colorScheme).opacity(0.82)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }
}

private struct SocialLinkButton<Background: View>: View {
    let accessibilityTitle: String
    let systemImage: String
    let foregroundColor: Color
    let background: Background
    let destination: URL
    let action: (() -> Void)?

    init(
        accessibilityTitle: String,
        systemImage: String,
        foregroundColor: Color,
        background: Background,
        destination: URL,
        action: (() -> Void)? = nil
    ) {
        self.accessibilityTitle = accessibilityTitle
        self.systemImage = systemImage
        self.foregroundColor = foregroundColor
        self.background = background
        self.destination = destination
        self.action = action
    }

    var body: some View {
        Group {
            if let action {
                Button(action: action) {
                    buttonBody
                }
            } else {
                Link(destination: destination) {
                    buttonBody
                }
            }
        }
        .accessibilityLabel(accessibilityTitle)
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private var buttonBody: some View {
        Image(systemName: systemImage)
            .font(.system(size: 12, weight: .bold))
            .foregroundColor(foregroundColor)
            .frame(width: 34, height: 34)
            .background(background)
            .clipShape(Circle())
            .overlay(
                Circle()
                    .stroke(Color.white.opacity(0.18), lineWidth: 1)
            )
            .overlay(
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(0.22),
                                Color.white.opacity(0)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .scaleEffect(0.92)
            )
            .shadow(color: .black.opacity(0.18), radius: 10, y: 5)
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
                Label("YouTube", systemImage: "play.rectangle.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(
                        LinearGradient(
                            colors: [
                                AppColors.youtube(for: colorScheme),
                                AppColors.youtubeDeep(for: colorScheme)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .overlay(
                        Capsule()
                            .stroke(Color.white.opacity(0.16), lineWidth: 1)
                    )
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .skydownTactileAction()
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
    @State private var pendingUploadTarget: VideoPublicConfigImageTarget?
    @State private var activeUploadTarget: VideoPublicConfigImageTarget?
    private let editableImageUploadService = EditableImageAssetUploadService()

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Videography Editor")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Owner und Video-Admins steuern hier Equipment und Featured Collabs. Bilder laufen jetzt picker-first und werden direkt mit Vorschau uebernommen.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text("Eintraege kannst du neu anlegen, ersetzen oder entfernen. Oeffentliche Daten werden erst nach `Oeffentliche Daten speichern` live.")
                .font(.footnote.weight(.medium))
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
                        EditableImageField(
                            title: "Equipment-Bild",
                            imageURL: Binding(
                                get: { item.imageURLString ?? "" },
                                set: { viewModel.updateEquipmentItem(item.id, imageURLString: $0) }
                            ),
                            colorScheme: colorScheme,
                            isUploading: activeUploadTarget == .equipment(item.id),
                            uploadStatusText: "Equipment-Bild wird uebernommen.",
                            onPickImage: { pendingUploadTarget = .equipment(item.id) },
                            onRemoveImage: { removeEditableImage(for: .equipment(item.id)) }
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
                Text("Featured Collabs")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                ForEach(Array(viewModel.publicConfig.collaborationItems.enumerated()), id: \.element.id) { _, item in
                    VStack(alignment: .leading, spacing: 8) {
                        NicmaUploadField(
                            title: "Name",
                            text: Binding(
                                get: { item.name },
                                set: { viewModel.updateCollaborationItem(item.id, name: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: "Rolle",
                            text: Binding(
                                get: { item.role },
                                set: { viewModel.updateCollaborationItem(item.id, role: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: "Highlight",
                            text: Binding(
                                get: { item.highlight },
                                set: { viewModel.updateCollaborationItem(item.id, highlight: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: "Vibe",
                            text: Binding(
                                get: { item.vibe },
                                set: { viewModel.updateCollaborationItem(item.id, vibe: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        EditableImageField(
                            title: "Collab-Bild",
                            imageURL: Binding(
                                get: { item.imageURLString ?? "" },
                                set: { viewModel.updateCollaborationItem(item.id, imageURLString: $0) }
                            ),
                            colorScheme: colorScheme,
                            isUploading: activeUploadTarget == .collaboration(item.id),
                            uploadStatusText: "Collab-Bild wird uebernommen.",
                            onPickImage: { pendingUploadTarget = .collaboration(item.id) },
                            onRemoveImage: { removeEditableImage(for: .collaboration(item.id)) }
                        )
                        NicmaUploadField(
                            title: "Spotify Artist ID",
                            text: Binding(
                                get: { item.spotifyArtistID ?? "" },
                                set: { viewModel.updateCollaborationItem(item.id, spotifyArtistID: $0) }
                            ),
                            colorScheme: colorScheme,
                            autocapitalization: .never
                        )
                        NicmaUploadField(
                            title: "Instagram URL",
                            text: Binding(
                                get: { item.instagramURLString ?? "" },
                                set: { viewModel.updateCollaborationItem(item.id, instagramURLString: $0) }
                            ),
                            colorScheme: colorScheme,
                            keyboard: .URL,
                            autocapitalization: .never
                        )
                        NicmaUploadField(
                            title: "YouTube URL",
                            text: Binding(
                                get: { item.youtubeURLString ?? "" },
                                set: { viewModel.updateCollaborationItem(item.id, youtubeURLString: $0) }
                            ),
                            colorScheme: colorScheme,
                            keyboard: .URL,
                            autocapitalization: .never
                        )
                        Button(role: .destructive) {
                            viewModel.removeCollaborationItem(item.id)
                        } label: {
                            Label("Collab entfernen", systemImage: "trash")
                                .font(.caption.weight(.semibold))
                        }
                    }
                    .padding(14)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }

                Button {
                    viewModel.addCollaborationItem()
                } label: {
                    Label("Collab hinzufuegen", systemImage: "plus.circle.fill")
                        .font(.subheadline.weight(.semibold))
                }
                .buttonStyle(.bordered)
                .tint(AppColors.accentMystic(for: colorScheme))
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
        .sheet(item: $pendingUploadTarget) { target in
            SingleImagePicker { provider in
                handleEditableImageProvider(provider, for: target)
            }
        }
    }

    private func handleEditableImageProvider(
        _ temporaryFileURL: URL?,
        for target: VideoPublicConfigImageTarget
    ) {
        pendingUploadTarget = nil

        guard let temporaryFileURL else {
            return
        }

        Task {
            await MainActor.run {
                activeUploadTarget = target
            }
            do {
                let previousURL = currentEditableImageURL(for: target)
                defer { try? FileManager.default.removeItem(at: temporaryFileURL) }
                let data = try await PickedImageUploadPreparation.normalizedJPEGData(fromTemporaryFileURL: temporaryFileURL)
                let url = try await editableImageUploadService.uploadImageData(data)
                if previousURL != url {
                    try? await editableImageUploadService.deleteImage(at: previousURL)
                }
                await MainActor.run {
                    switch target {
                    case .equipment(let itemId):
                        viewModel.updateEquipmentItem(itemId, imageURLString: url)
                    case .collaboration(let itemId):
                        viewModel.updateCollaborationItem(itemId, imageURLString: url)
                    }
                    viewModel.toastMessage = "Bild hochgeladen und uebernommen."
                    viewModel.toastStyle = .success
                    viewModel.showToast = true
                }
            } catch {
                await MainActor.run {
                    viewModel.toastMessage = "Bild konnte nicht hochgeladen werden: \(error.localizedDescription)"
                    viewModel.toastStyle = .error
                    viewModel.showToast = true
                }
            }

            await MainActor.run {
                activeUploadTarget = nil
            }
        }
    }

    private func currentEditableImageURL(for target: VideoPublicConfigImageTarget) -> String {
        switch target {
        case .equipment(let itemId):
            return viewModel.publicConfig.equipmentItems.first { $0.id == itemId }?.imageURLString ?? ""
        case .collaboration(let itemId):
            return viewModel.publicConfig.collaborationItems.first { $0.id == itemId }?.imageURLString ?? ""
        }
    }

    private func removeEditableImage(for target: VideoPublicConfigImageTarget) {
        let previousURL = currentEditableImageURL(for: target)
        switch target {
        case .equipment(let itemId):
            viewModel.updateEquipmentItem(itemId, imageURLString: "")
        case .collaboration(let itemId):
            viewModel.updateCollaborationItem(itemId, imageURLString: "")
        }

        Task {
            do {
                try await editableImageUploadService.deleteImage(at: previousURL)
                await MainActor.run {
                    viewModel.toastMessage = "Bild entfernt."
                    viewModel.toastStyle = .success
                    viewModel.showToast = true
                }
            } catch {
                await MainActor.run {
                    viewModel.toastMessage = "Bild wurde entfernt. Alter Upload konnte nicht geloescht werden: \(error.localizedDescription)"
                    viewModel.toastStyle = .error
                    viewModel.showToast = true
                }
            }
        }
    }
}

private enum VideoPublicConfigImageTarget: Equatable, Identifiable {
    case equipment(String)
    case collaboration(String)

    var id: String {
        switch self {
        case .equipment(let itemId):
            return "equipment-\(itemId)"
        case .collaboration(let itemId):
            return "collaboration-\(itemId)"
        }
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

        guard let video else {
            player.replaceCurrentItem(with: nil)
            selectedVideoID = nil
            playingVideoID = nil
            return
        }

        guard let url = URL(string: video.nativePlaybackURLString), !video.nativePlaybackURLString.isEmpty else {
            player.replaceCurrentItem(with: nil)
            selectedVideoID = video.id
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

private enum VideoHubPresentedSheet: Identifiable, Equatable {
    case youTube(SkydownYouTubeVideoItem)

    var id: String {
        switch self {
        case .youTube(let item):
            return "youtube-\(item.id)"
        }
    }
}

struct ExternalVideoEmbedSurface: UIViewRepresentable {
    let urlString: String

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard let url = URL(string: urlString) else { return }
        if webView.url?.absoluteString != url.absoluteString {
            webView.load(URLRequest(url: url))
        }
    }
}
