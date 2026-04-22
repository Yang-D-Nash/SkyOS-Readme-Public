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
import UIKit
import UniformTypeIdentifiers
import WebKit

private enum VideoHubSectionAnchor: String {
    case videos
    case equipment
    case collaborations
}

struct VideoHubView: View {
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
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
    @State private var selectedEquipmentItem: SkydownVideoEquipmentItem?
    @State private var originalViewerTarget: VideoOriginalViewerTarget?
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
        ScrollViewReader { scrollProxy in
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    heroCard(
                        onOpenVideos: {
                            withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                scrollProxy.scrollTo(VideoHubSectionAnchor.videos.rawValue, anchor: .top)
                            }
                        },
                        onOpenEquipment: {
                            withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                scrollProxy.scrollTo(VideoHubSectionAnchor.equipment.rawValue, anchor: .top)
                            }
                        },
                        onOpenCollaborations: {
                            withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                scrollProxy.scrollTo(VideoHubSectionAnchor.collaborations.rawValue, anchor: .top)
                            }
                        }
                    )
                    if viewModel.isAdmin && showingUploadComposer {
                        uploadCard
                            .transition(.move(edge: .top).combined(with: .opacity))
                    }
                    VideoEquipmentCard(
                        colorScheme: colorScheme,
                        items: viewModel.publicConfig.equipmentItems,
                        onSelectItem: { item in
                            selectedEquipmentItem = item
                        }
                    )
                    .id(VideoHubSectionAnchor.equipment.rawValue)
                    playerCard
                    libraryCard
                        .id(VideoHubSectionAnchor.videos.rawValue)
                    collaborationsCard
                        .id(VideoHubSectionAnchor.collaborations.rawValue)

                    if viewModel.isAdmin {
                        adminToolsCard
                    }
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding + (viewModel.isAdmin ? 56 : 0))
            }
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
                secondaryAccent: AppColors.youtube(for: colorScheme)
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
        .sheet(item: $selectedEquipmentItem) { item in
            VideoEquipmentDetailSheet(
                item: item,
                colorScheme: colorScheme
            )
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
                    secondaryAccent: AppColors.youtube(for: colorScheme)
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
        .fullScreenCover(item: $originalViewerTarget) { target in
            SkydownOriginalVideoDestinationView(
                urlString: target.urlString,
                title: target.title
            )
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

    private func openOriginalVideo(_ video: SkydownVideoHubItem) {
        let trimmedURL = video.inAppOriginalURLString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedURL.isEmpty, let url = URL(string: trimmedURL) else { return }
        playbackManager.player.pause()

        activePresentedSheet = nil
        originalViewerTarget = VideoOriginalViewerTarget(
            urlString: url.absoluteString,
            title: video.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Original" : video.title
        )
    }

    @ViewBuilder
    private var collaborationsCard: some View {
        let collaborationItems = viewModel.publicConfig.collaborationItems
        VStack(alignment: .leading, spacing: 14) {
            Text("Featured Collabs")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Creatives hinter den Visuals.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if collaborationItems.isEmpty {
                Text("Featured Collabs folgen. Der Bereich bleibt bereit und wird laufend ergaenzt.")
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
            shadowRadius: 9,
            shadowYOffset: 4
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

    private func heroCard(
        onOpenVideos: @escaping () -> Void,
        onOpenEquipment: @escaping () -> Void,
        onOpenCollaborations: @escaping () -> Void
    ) -> some View {
        BrandHeroSurface(
            colorScheme: colorScheme,
            eyebrow: screenHeaderSettingsStore.settings.resolvedVideoHubEyebrow ?? "Video",
            title: screenHeaderSettingsStore.settings.resolvedVideoHubTitle ?? "Video",
            subtitle: screenHeaderSettingsStore.settings.resolvedVideoHubSubtitle ?? "Visuals · Collabs · Cinematic Flow",
            detail: screenHeaderSettingsStore.settings.resolvedVideoHubDetail ?? "Atmosphaerisch, klar und direkt spielbar.",
            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedVideoHubImageURL,
            accent: AppColors.accentMystic(for: colorScheme),
            secondaryAccent: AppColors.accentHighlight(for: colorScheme),
            marks: [.skydown]
        ) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    MusicBadge(text: "Videos", isAccent: true, onTap: onOpenVideos)
                    MusicBadge(text: "Equipment", isAccent: false, onTap: onOpenEquipment)
                    MusicBadge(text: "Collabs", isAccent: false, onTap: onOpenCollaborations)
                }
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

            Text("Kleinere Dateien = schneller.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accent(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 9,
            shadowYOffset: 4
        )
    }

    private var adminToolsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Video Admin")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Equipment & Collabs im Editor.")
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
            shadowRadius: 9,
            shadowYOffset: 4
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

            Text("Oder Video-Link.")
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

                                    Text(selectedVideo.originalDestinationDescription)
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

                    VStack(alignment: .leading, spacing: 5) {
                        Text(selectedVideo.title)
                            .font(.title3.weight(.semibold))
                            .foregroundColor(.white)
                            .lineLimit(2)

                        Text("\(selectedVideo.projectName) · \(selectedVideo.provider.badgeLabel)")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.78))
                            .lineLimit(1)

                        if !selectedVideo.notes.isEmpty {
                            Text(selectedVideo.notes)
                                .font(.caption)
                                .foregroundColor(.white.opacity(0.72))
                                .lineLimit(2)
                        }
                    }
                    .padding(16)
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
                } else if selectedVideo.opensOriginalInApp {
                    Button {
                        openOriginalVideo(selectedVideo)
                    } label: {
                        Label(
                            selectedVideo.directOpenActionTitle,
                            systemImage: selectedVideo.supportsInlinePlayback
                                ? "rectangle.portrait.and.arrow.right"
                                : "arrow.up.forward.square"
                        )
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(
                        selectedVideo.supportsInlinePlayback
                            ? AppColors.accentMystic(for: colorScheme)
                            : AppColors.accent(for: colorScheme)
                    )
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

                if !selectedVideo.notes.isEmpty {
                    Text(selectedVideo.notes)
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else {
                Text("Noch kein Fokus-Video aktiv. Waehle einen Clip aus der Library oder aktualisiere den Hub.")
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
                HStack(spacing: 10) {
                    ProgressView()
                        .controlSize(.small)
                    Text("Videos werden ruhig vorbereitet ...")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else if viewModel.videos.isEmpty {
                Text(
                    viewModel.isAdmin
                    ? "Noch keine Videos im Hub. Neue Uploads erscheinen hier automatisch."
                    : "Aktuell sind noch keine Videos freigegeben. Bitte spaeter erneut pruefen."
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
                            openOriginalVideo(video)
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

    private var routeTitle: String {
        if video.opensOriginalInApp {
            return video.supportsInlinePlayback ? "Direkt in App + Original" : "Original in App"
        }
        if video.supportsInlinePlayback {
            return "Direkt im Reel"
        }
        return "Externer Clip"
    }

    private var routeDetail: String {
        if isAdmin {
            if video.isPlayable {
                return isSelected
                    ? "Der Clip sitzt gerade im Player und kann sofort gestartet oder gestoppt werden."
                    : "Ein Tap setzt den Clip in den Player. Von dort bleibt er als Fokus-Video sichtbar."
            }
            if video.opensOriginalInApp {
                return "Das Original bleibt in der App erreichbar, inklusive Schliessen und Rueckweg."
            }
            if video.supportsInlinePlayback {
                return "Der Clip laeuft direkt als In-App-Reel mit schnellem Preview-Flow."
            }
            return "Aktuell steht hier nur der externe Aufruf zur Verfuegung."
        }

        if video.opensOriginalInApp {
            return video.supportsInlinePlayback
                ? "Ein Tap oeffnet den Clip direkt in der App, ohne Zwischenweg ueber den Browser."
                : "Ein Tap bringt dich in die In-App-Originalansicht mit sicherem Zurueck in die App."
        }
        if video.supportsInlinePlayback {
            return "Ein Tap startet die direkte Videoansicht ohne weiteren Zwischenscreen."
        }
        return "Dieses Video wird derzeit ueber einen externen Link geoeffnet."
    }

    private var routeAccent: Color {
        switch video.provider {
        case .youTube:
            return AppColors.youtube(for: colorScheme)
        case .mega:
            return AppColors.accentMystic(for: colorScheme)
        case .googleDrive:
            return AppColors.accent(for: colorScheme)
        case .firebaseStorage, .externalLink:
            return AppColors.accentMystic(for: colorScheme)
        }
    }

    private var libraryRowMetaLine: String {
        let date = skydownVideoDateFormatter.string(from: video.createdAt)
        var parts = [video.projectName, video.provider.badgeLabel, date]
        if isAdmin {
            parts.append(video.isPublic ? "Public" : "Private")
            if video.isHomeFeatured {
                parts.append("Home")
            }
        }
        return parts.joined(separator: " · ")
    }

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
                        .lineLimit(2)

                    Text(libraryRowMetaLine)
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(2)

                    if isAdmin, !video.fileName.isEmpty {
                        Text(video.fileName)
                            .font(.caption2)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                            .lineLimit(1)
                    }

                    if !video.notes.isEmpty {
                        Text(video.notes)
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(2)
                    }
                }

                Spacer()
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(routeTitle)
                    .font(.caption.weight(.bold))
                    .foregroundColor(routeAccent)

                Text(routeDetail)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(2)
            }

            if isAdmin {
                HStack(spacing: 10) {
                    Button(action: onSelect) {
                        Label(isSelected ? "Im Player" : "Auswaehlen", systemImage: "rectangle.on.rectangle")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button(action: video.isPlayable ? onPlayToggle : (video.opensOriginalInApp ? onOpenOriginal : onOpenReel)) {
                        Label(
                            video.isPlayable
                                ? (isPlaying ? "Stoppen" : "Abspielen")
                                : (video.opensOriginalInApp ? "Direkt oeffnen" : (video.supportsInlinePlayback ? "Ansehen" : "Oeffnen")),
                            systemImage: video.isPlayable
                                ? (isPlaying ? "stop.fill" : "play.fill")
                                : (video.opensOriginalInApp
                                    ? "rectangle.portrait.and.arrow.right"
                                    : (video.supportsInlinePlayback ? "play.rectangle.fill" : "arrow.up.forward.square"))
                        )
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(video.provider == .youTube ? AppColors.youtube(for: colorScheme) : AppColors.accent(for: colorScheme))
                    .disabled(!video.isPlayable && !video.supportsInlinePlayback && !video.opensOriginalInApp)
                }
            } else {
                if video.opensOriginalInApp {
                    Button(action: onOpenOriginal) {
                        Label(
                            video.directOpenActionTitle,
                            systemImage: video.supportsInlinePlayback
                                ? "rectangle.portrait.and.arrow.right"
                                : "arrow.up.forward.square"
                        )
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(video.supportsInlinePlayback ? AppColors.accentMystic(for: colorScheme) : AppColors.accent(for: colorScheme))
                } else if video.supportsInlinePlayback {
                    Button(action: onOpenReel) {
                        Label("Direkt im Video", systemImage: "play.rectangle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accentMystic(for: colorScheme))
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
    @State private var originalViewerTarget: VideoOriginalViewerTarget?
    @State private var isTransitioning = false

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

                            VStack(alignment: .leading, spacing: 6) {
                                Text(video.projectName.uppercased())
                                    .font(.caption2.weight(.semibold))
                                    .foregroundColor(.white.opacity(0.68))

                                Text(video.title)
                                    .font(.title3.weight(.semibold))
                                    .foregroundColor(.white)
                                    .lineLimit(2)

                                if !video.notes.isEmpty {
                                    Text(video.notes)
                                        .font(.caption)
                                        .foregroundColor(.white.opacity(0.72))
                                        .lineLimit(3)
                                }

                                if video.opensOriginalInApp {
                                    Button {
                                        originalViewerTarget = VideoOriginalViewerTarget(
                                            urlString: video.inAppOriginalURLString,
                                            title: video.title.isEmpty ? "Original" : video.title
                                        )
                                    } label: {
                                        Label(video.directOpenActionTitle, systemImage: "arrow.up.forward.square")
                                            .font(.subheadline.weight(.semibold))
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 12)
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .tint(.white)
                                    .foregroundColor(.black)
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.bottom, 30)
                            .padding(.trailing, 54)
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

                if videos.count > 1 {
                    HStack {
                        Spacer()
                        ReelProgressRail(currentIndex: currentIndex, count: videos.count)
                    }
                    .padding(.trailing, 14)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .zIndex(8)
                }

                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("SkyOS Video")
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(.white.opacity(0.92))

                        Text("\(currentIndex + 1) von \(videos.count)")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.70))

                        if videos.count > 1 {
                            Text("Vertikal wischen durch alle Clips")
                                .font(.caption.weight(.medium))
                                .foregroundColor(.white.opacity(0.58))
                        }
                    }

                    Spacer()

                    Button(action: { dismiss() }, label: {
                        Label("Zurueck zur App", systemImage: "xmark")
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .frame(height: 48)
                            .background(Color.black.opacity(0.42))
                            .clipShape(Capsule())
                            .overlay(
                                Capsule()
                                    .stroke(Color.white.opacity(0.22), lineWidth: 1)
                            )
                    })
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                }
                .padding(.horizontal, 20)
                .padding(.top, max(proxy.safeAreaInsets.top, 12))
                .zIndex(10)

                if isTransitioning {
                    VStack(spacing: 10) {
                        ProgressView()
                            .tint(.white)
                        Text("Clip wird vorbereitet ...")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.86))
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color.black.opacity(0.46), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                    .transition(.opacity)
                    .zIndex(12)
                }
            }
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 0.16)) {
                isTransitioning = true
            }
            playCurrent()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.28) {
                withAnimation(.easeInOut(duration: 0.18)) {
                    isTransitioning = false
                }
            }
        }
        .onChange(of: currentIndex) { _, _ in
            withAnimation(.easeInOut(duration: 0.14)) {
                isTransitioning = true
            }
            playCurrent()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.24) {
                withAnimation(.easeInOut(duration: 0.18)) {
                    isTransitioning = false
                }
            }
        }
        .onDisappear {
            player.pause()
            player.replaceCurrentItem(with: nil)
        }
        .fullScreenCover(item: $originalViewerTarget) { target in
            SkydownOriginalVideoDestinationView(
                urlString: target.urlString,
                title: target.title
            )
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

private struct ReelProgressRail: View {
    let currentIndex: Int
    let count: Int

    var body: some View {
        VStack(spacing: 6) {
            ForEach(0..<count, id: \.self) { index in
                Capsule()
                    .fill(Color.white.opacity(index == currentIndex ? 0.96 : 0.34))
                    .frame(width: 4, height: index == currentIndex ? 28 : 9)
            }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 10)
        .background(Color.black.opacity(0.30), in: Capsule())
    }
}

struct VideoEquipmentCard: View {
    let colorScheme: ColorScheme
    let items: [SkydownVideoEquipmentItem]
    let onSelectItem: (SkydownVideoEquipmentItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Equipment & Software")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Visual Stack fuer Shoot, Edit und Finish.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if items.isEmpty {
                Text("Equipment wird aktuell vorbereitet und erscheint hier in Kuerze.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .padding(.top, 4)
            } else {
                VStack(spacing: 10) {
                    ForEach(items) { item in
                        VideoEquipmentRow(
                            item: item,
                            colorScheme: colorScheme,
                            onTap: {
                                onSelectItem(item)
                            }
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
    let onTap: (() -> Void)?

    private var imageURL: URL? {
        guard let value = item.imageURLString, !value.isEmpty else { return nil }
        return URL(string: value)
    }

    var body: some View {
        Group {
            if let onTap {
                Button(action: onTap) {
                    rowContent
                }
                .buttonStyle(.plain)
                .skydownTactileAction()
            } else {
                rowContent
            }
        }
    }

    private var rowContent: some View {
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
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
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

struct VideoEquipmentDetailSheet: View {
    let item: SkydownVideoEquipmentItem
    let colorScheme: ColorScheme
    @Environment(\.dismiss) private var dismiss

    private var imageURL: URL? {
        guard let value = item.imageURLString, !value.isEmpty else { return nil }
        return URL(string: value)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text(item.title)
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(item.detail)
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if let imageURL {
                        AsyncImage(url: imageURL) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                            default:
                                Rectangle()
                                    .fill(AppColors.secondaryBackground(for: colorScheme))
                                    .overlay {
                                        Image(systemName: "camera.metering.spot")
                                            .font(.title2.weight(.bold))
                                            .foregroundColor(AppColors.accent(for: colorScheme))
                                    }
                            }
                        }
                        .frame(height: 220)
                        .frame(maxWidth: .infinity)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 28)
            }
            .background(AppColors.screenGradient(for: colorScheme).ignoresSafeArea())
            .navigationTitle("Equipment")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Fertig") { dismiss() }
                }
            }
        }
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
        HStack(alignment: .top, spacing: 12) {
            previewArtwork
                .frame(width: 76, height: 76)
                .clipShape(RoundedRectangle(cornerRadius: 16))

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Text(artist.role.uppercased())
                        .font(.caption2.weight(.bold))
                        .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                        .lineLimit(1)
                        .minimumScaleFactor(0.82)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(
                            Capsule()
                                .fill(AppColors.secondaryBackground(for: colorScheme))
                        )

                    if !artist.vibe.isEmpty {
                        Text(artist.vibe)
                            .font(.caption2.weight(.bold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .lineLimit(1)
                            .minimumScaleFactor(0.82)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 5)
                            .background(
                                Capsule()
                                    .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                            )
                    }
                }

                Text(artist.name)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.84)

                if !artist.highlight.isEmpty {
                    Text(artist.highlight)
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(1)
                        .minimumScaleFactor(0.84)
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

            Spacer(minLength: 0)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(height: 138)
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

    private var previewArtwork: some View {
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
    }

    private var fallbackArtwork: some View {
        ZStack {
            LinearGradient(
                colors: [
                    AppColors.accentMystic(for: colorScheme).opacity(0.94),
                    AppColors.accent(for: colorScheme).opacity(0.82)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            Text(String(artist.name.prefix(1)).uppercased())
                .font(.title.weight(.black))
                .foregroundColor(.white)
        }
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

private struct VideoOriginalViewerTarget: Identifiable {
    let id = UUID()
    let urlString: String
    let title: String
}

struct SkydownOriginalVideoDestinationView: View {
    let urlString: String
    let title: String

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var player = AVPlayer()
    @State private var isPlaying = false

    private var directVideoURL: URL? {
        guard skydownIsLikelyDirectVideoURL(urlString) else { return nil }
        return URL(string: urlString)
    }

    private var resolvedURL: URL? {
        URL(string: urlString)
    }

    var body: some View {
        Group {
            if let directVideoURL {
                GeometryReader { proxy in
                    ZStack(alignment: .top) {
                        Color.black
                            .ignoresSafeArea()

                        SkydownInlineVideoSurface(player: player)
                            .ignoresSafeArea()
                            .onAppear {
                                player.pause()
                                player.replaceCurrentItem(with: AVPlayerItem(url: directVideoURL))
                                player.seek(to: .zero)
                                player.play()
                                isPlaying = true
                            }

                        LinearGradient(
                            colors: [
                                Color.black.opacity(0.82),
                                Color.black.opacity(0.52),
                                .clear
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(height: 220)
                        .ignoresSafeArea(edges: .top)

                        VStack(alignment: .leading, spacing: 12) {
                            Text(title)
                                .font(.headline.weight(.bold))
                                .foregroundColor(.white)
                                .lineLimit(2)

                            Text("Direkt in der In-App-Ansicht. Schliessen bringt dich direkt zurueck.")
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.76))

                            HStack(spacing: 10) {
                                SkydownViewerToolbarButton(
                                    title: "Extern",
                                    systemImage: "arrow.up.forward.square",
                                    isPrimary: false
                                ) {
                                    if let resolvedURL {
                                        openURL(resolvedURL)
                                    }
                                }

                                SkydownViewerToolbarButton(
                                    title: "Schliessen",
                                    systemImage: "xmark",
                                    isPrimary: true
                                ) {
                                    dismiss()
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, max(proxy.safeAreaInsets.top, 12))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .zIndex(2)

                        VStack {
                            Spacer()

                            VStack(spacing: 10) {
                                HStack(spacing: 10) {
                                    SkydownViewerToolbarButton(
                                        title: isPlaying ? "Pause" : "Play",
                                        systemImage: isPlaying ? "pause.fill" : "play.fill",
                                        isPrimary: false
                                    ) {
                                        if isPlaying {
                                            player.pause()
                                        } else {
                                            player.play()
                                        }
                                        isPlaying.toggle()
                                    }

                                    SkydownViewerToolbarButton(
                                        title: "Extern",
                                        systemImage: "arrow.up.forward.square",
                                        isPrimary: false
                                    ) {
                                        if let resolvedURL {
                                            openURL(resolvedURL)
                                        }
                                    }
                                }

                                SkydownViewerToolbarButton(
                                    title: "Zurueck zur App",
                                    systemImage: "xmark",
                                    isPrimary: true
                                ) {
                                    dismiss()
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.bottom, max(proxy.safeAreaInsets.bottom, 16))
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .zIndex(3)
                    }
                }
            } else if let resolvedURL {
                SkydownManagedBrowserView(
                    url: resolvedURL,
                    title: title
                )
            } else {
                NavigationStack {
                    ContentUnavailableView(
                        "Original aktuell nicht verfuegbar",
                        systemImage: "play.rectangle",
                        description: Text("Das Original laesst sich gerade nicht laden. Bitte versuche es in einem Moment erneut.")
                    )
                    .navigationTitle(title.isEmpty ? "Original" : title)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .topBarLeading) {
                            Button("Schliessen") {
                                dismiss()
                            }
                        }
                    }
                }
            }
        }
        .onDisappear {
            player.pause()
            player.replaceCurrentItem(with: nil)
            isPlaying = false
        }
    }
}

private struct SkydownViewerToolbarButton: View {
    let title: String
    let systemImage: String
    let isPrimary: Bool
    var isEnabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(.footnote.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(isPrimary ? Color.white : Color.white.opacity(0.12))
                )
                .foregroundColor(isPrimary ? .black : .white)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color.white.opacity(isPrimary ? 0 : 0.18), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1 : 0.46)
    }
}

struct SkydownManagedBrowserView: View {
    let url: URL
    let title: String

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @StateObject private var browserState = SkydownManagedBrowserState()

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .top) {
                SkydownManagedBrowserWebView(
                    url: url,
                    browserState: browserState
                )
                .ignoresSafeArea()

                LinearGradient(
                    colors: [
                        Color.black.opacity(0.86),
                        Color.black.opacity(0.54),
                        .clear
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 240)
                .ignoresSafeArea(edges: .top)

                VStack(alignment: .leading, spacing: 12) {
                    Text(title.isEmpty ? "Original" : title)
                        .font(.headline.weight(.bold))
                        .foregroundColor(.white)
                        .lineLimit(2)

                    Text("Web-Ansicht mit sichtbaren Aktionen fuer Zurueck, Weiter, Extern und Schliessen.")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.76))

                    HStack(spacing: 10) {
                        SkydownViewerToolbarButton(
                            title: "Zurueck",
                            systemImage: "chevron.left",
                            isPrimary: false,
                            isEnabled: browserState.canGoBack
                        ) {
                            browserState.goBack()
                        }

                        SkydownViewerToolbarButton(
                            title: "Weiter",
                            systemImage: "chevron.right",
                            isPrimary: false,
                            isEnabled: browserState.canGoForward
                        ) {
                            browserState.goForward()
                        }
                    }

                    HStack(spacing: 10) {
                        SkydownViewerToolbarButton(
                            title: "Extern",
                            systemImage: "arrow.up.forward.square",
                            isPrimary: false
                        ) {
                            openURL(browserState.currentURL ?? url)
                        }

                        SkydownViewerToolbarButton(
                            title: "Schliessen",
                            systemImage: "xmark",
                            isPrimary: true
                        ) {
                            dismiss()
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, max(proxy.safeAreaInsets.top, 12))
                .padding(.bottom, 16)
                .background(Color.black.opacity(0.42), in: RoundedRectangle(cornerRadius: 28, style: .continuous))
                .padding(.horizontal, 16)

                VStack {
                    Spacer()

                    HStack(spacing: 10) {
                        SkydownViewerToolbarButton(
                            title: "Extern",
                            systemImage: "arrow.up.forward.square",
                            isPrimary: false
                        ) {
                            openURL(browserState.currentURL ?? url)
                        }

                        SkydownViewerToolbarButton(
                            title: "Zurueck zur App",
                            systemImage: "xmark",
                            isPrimary: true
                        ) {
                            dismiss()
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, max(proxy.safeAreaInsets.bottom, 16))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .onDisappear {
            browserState.stopLoading()
        }
    }
}

private struct SkydownInlineVideoSurface: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> SkydownInlinePlayerView {
        let view = SkydownInlinePlayerView()
        view.playerLayer?.player = player
        return view
    }

    func updateUIView(_ uiView: SkydownInlinePlayerView, context: Context) {
        uiView.playerLayer?.player = player
    }
}

private final class SkydownInlinePlayerView: UIView {
    override class var layerClass: AnyClass {
        AVPlayerLayer.self
    }

    var playerLayer: AVPlayerLayer? {
        layer as? AVPlayerLayer
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .black
        playerLayer?.videoGravity = .resizeAspect
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        backgroundColor = .black
        playerLayer?.videoGravity = .resizeAspect
    }
}

private final class SkydownManagedBrowserState: NSObject, ObservableObject {
    @Published var canGoBack = false
    @Published var canGoForward = false
    @Published var currentURL: URL?

    private weak var webView: WKWebView?
    private var loadedInitialURL: URL?

    func attach(_ webView: WKWebView, initialURL: URL) {
        self.webView = webView
        if loadedInitialURL != initialURL {
            loadedInitialURL = initialURL
            webView.load(URLRequest(url: initialURL))
        }
        refresh(from: webView)
    }

    func goBack() {
        webView?.goBack()
        refresh(from: webView)
    }

    func goForward() {
        webView?.goForward()
        refresh(from: webView)
    }

    func stopLoading() {
        webView?.stopLoading()
    }

    func refresh(from webView: WKWebView?) {
        canGoBack = webView?.canGoBack ?? false
        canGoForward = webView?.canGoForward ?? false
        currentURL = webView?.url ?? loadedInitialURL
    }
}

private struct SkydownManagedBrowserWebView: UIViewRepresentable {
    let url: URL
    @ObservedObject var browserState: SkydownManagedBrowserState

    func makeCoordinator() -> Coordinator {
        Coordinator(browserState: browserState)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        browserState.attach(webView, initialURL: url)
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        browserState.attach(webView, initialURL: url)
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        let browserState: SkydownManagedBrowserState

        init(browserState: SkydownManagedBrowserState) {
            self.browserState = browserState
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            browserState.refresh(from: webView)
        }

        func webView(_ webView: WKWebView, didCommit navigation: WKNavigation!) {
            browserState.refresh(from: webView)
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            browserState.refresh(from: webView)
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            browserState.refresh(from: webView)
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            browserState.refresh(from: webView)
        }

        func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
            browserState.refresh(from: webView)
        }
    }
}

private func skydownIsLikelyDirectVideoURL(_ rawValue: String) -> Bool {
    let normalized = rawValue
        .lowercased()
        .split(separator: "?")
        .first?
        .split(separator: "#")
        .first
        .map(String.init) ?? rawValue.lowercased()
    return normalized.hasSuffix(".mp4")
        || normalized.hasSuffix(".mov")
        || normalized.hasSuffix(".m4v")
        || normalized.hasSuffix(".webm")
        || normalized.hasSuffix(".m3u8")
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
