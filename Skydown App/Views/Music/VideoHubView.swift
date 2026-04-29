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
    @State private var sheetPresentation = SkydownQueuedPresentation<VideoHubPresentedSheet>()
    @State private var showingReelViewer = false
    @State private var hasHandledInitialSelection = false
    @State private var showingAdminEditor = false
    @State private var editingVideoID: String?
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
                            openVideoPlayer()
                        },
                        onOpenEquipment: {
                            withAnimation(SkydownMotion.smoothScroll) {
                                scrollProxy.scrollTo(VideoHubSectionAnchor.equipment.rawValue, anchor: .top)
                            }
                        },
                        onOpenCollaborations: {
                            withAnimation(SkydownMotion.smoothScroll) {
                                scrollProxy.scrollTo(VideoHubSectionAnchor.collaborations.rawValue, anchor: .top)
                            }
                        }
                    )
                    if viewModel.isAdmin && showingUploadComposer {
                        uploadCard
                            .transition(.move(edge: .top).combined(with: .opacity))
                    }
                    playerCard
                    VideoEquipmentCard(
                        colorScheme: colorScheme,
                        items: viewModel.publicConfig.equipmentItems,
                        onSelectItem: { item in
                            selectedEquipmentItem = item
                        }
                    )
                    .id(VideoHubSectionAnchor.equipment.rawValue)
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
                    withAnimation(SkydownMotion.screenTransition) {
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
                secondaryAccent: AppColors.accentMystic(for: colorScheme)
            )
            .ignoresSafeArea()
        )
        .navigationTitle(AppLocalized.text("videohub.nav.title", fallback: "Video"))
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
                        withAnimation(SkydownMotion.screenTransition) {
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
        .sheet(item: activePresentedSheetBinding) { sheet in
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
                    libraryCard
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
                    secondaryAccent: AppColors.accentMystic(for: colorScheme)
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
        .skydownKeyboardDismissToolbar()
    }

    private var activePresentedSheetBinding: Binding<VideoHubPresentedSheet?> {
        Binding(
            get: { sheetPresentation.activeItem },
            set: { sheetPresentation.updatePresentedItem($0) }
        )
    }

    private func presentSheet(_ sheet: VideoHubPresentedSheet) {
        sheetPresentation.request(sheet)
    }

    private func openOriginalVideo(_ video: SkydownVideoHubItem) {
        if video.supportsInlinePlayback {
            playbackManager.load(video: video)
            activePresentedSheetBinding.wrappedValue = nil
            showingReelViewer = true
            return
        }

        let trimmedURL = video.inAppOriginalURLString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedURL.isEmpty, let url = URL(string: trimmedURL) else { return }
        playbackManager.player.pause()

        activePresentedSheetBinding.wrappedValue = nil
        originalViewerTarget = VideoOriginalViewerTarget(
            urlString: url.absoluteString,
            title: video.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? AppLocalized.text("videohub.original_title", fallback: "Original")
                : video.title
        )
    }

    private func openVideoPlayer() {
        guard !viewModel.videos.isEmpty else { return }
        playbackManager.player.pause()
        activePresentedSheetBinding.wrappedValue = nil
        showingReelViewer = true
    }

    private func videoHubRowPresentation(index: Int, total: Int) -> VideoHubLibraryRowPresentation {
        if index == 0 { return .featured }
        if index == 1, total > 1 { return .secondary }
        return .catalog
    }

    @ViewBuilder
    private var collaborationsCard: some View {
        let collaborationItems = viewModel.publicConfig.collaborationItems
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            Text(AppLocalized.text("videohub.collabs.title", fallback: "Featured collabs"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.collabs.subtitle", fallback: "Creatives behind the visuals."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if collaborationItems.isEmpty {
                Text(AppLocalized.text("videohub.collabs.empty", fallback: "Featured collabs are on the way. This space stays ready and will fill in over time."))
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
                activePresentedSheetBinding.wrappedValue = nil
                showingReelViewer = true
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
        let settings = screenHeaderSettingsStore.settings
        let heroVideoURL = settings.resolvedVideoHubHeroVideoURL?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let heroMediaURL = settings.resolvedVideoHubImageURL?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let heroTitle = settings.resolvedVideoHubTitle
            ?? AppLocalized.text("videohub.hero.fallback_title", fallback: "Video")
        return BrandHeroSurface(
            colorScheme: colorScheme,
            eyebrow: settings.resolvedVideoHubEyebrow
                ?? AppLocalized.text("videohub.hero.fallback_eyebrow", fallback: "Video"),
            title: settings.resolvedVideoHubTitle
                ?? AppLocalized.text("videohub.hero.fallback_title", fallback: "Video"),
            subtitle: settings.resolvedVideoHubSubtitle
                ?? AppLocalized.text(
                    "videohub.hero.fallback_subtitle",
                    fallback: "Entry point — the main view lives in the player and the clip list below."
                ),
            detail: settings.resolvedVideoHubDetail
                ?? AppLocalized.text(
                    "videohub.hero.fallback_detail",
                    fallback: "Pick an overview here, focus below — without feed noise."
                ),
            backgroundImageURL: settings.resolvedVideoHubImageURL,
            accent: AppColors.accentMystic(for: colorScheme),
            secondaryAccent: AppColors.accentHighlight(for: colorScheme),
            marks: [.zweizwei],
            edgeToEdge: true,
            onSurfaceTap: {
                if let heroVideoURL, !heroVideoURL.isEmpty {
                    playbackManager.player.pause()
                    originalViewerTarget = VideoOriginalViewerTarget(
                        urlString: heroVideoURL,
                        title: heroTitle
                    )
                } else if let heroMediaURL, !heroMediaURL.isEmpty, skydownIsLikelyDirectVideoURL(heroMediaURL) {
                    playbackManager.player.pause()
                    originalViewerTarget = VideoOriginalViewerTarget(
                        urlString: heroMediaURL,
                        title: heroTitle
                    )
                } else {
                    onOpenVideos()
                }
            }
        ) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                    MusicBadge(
                        text: AppLocalized.text("videohub.badge.videos", fallback: "Videos"),
                        isAccent: true,
                        onTap: onOpenVideos
                    )
                    MusicBadge(
                        text: AppLocalized.text("videohub.badge.equipment", fallback: "Equipment"),
                        isAccent: false,
                        onTap: onOpenEquipment
                    )
                    MusicBadge(
                        text: AppLocalized.text("videohub.badge.collabs", fallback: "Collabs"),
                        isAccent: false,
                        onTap: onOpenCollaborations
                    )
                }
            }
        }
    }

    private var formatCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            Text(AppLocalized.text("videohub.format.title", fallback: "Format note"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.format.body", fallback: "MP4, MOV, or M4V."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(AppLocalized.text("videohub.format.smaller_faster", fallback: "Smaller files upload faster."))
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            Text(AppLocalized.text("videohub.admin.title", fallback: "Video admin"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.admin.subtitle", fallback: "Equipment & collabs in the editor."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Button {
                showingAdminEditor = true
            } label: {
                Label(AppLocalized.text("videohub.admin.open_editor", fallback: "Open editor"), systemImage: "slider.horizontal.3")
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            Text(AppLocalized.text("videohub.upload.title", fallback: "Video upload"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.upload.admin_only", fallback: "Admins only."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.title", fallback: "Title"),
                text: $viewModel.videoTitle,
                colorScheme: colorScheme
            )

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.project_artist", fallback: "Project / artist"),
                text: $viewModel.projectName,
                colorScheme: colorScheme
            )

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.email", fallback: "Email"),
                text: $viewModel.email,
                colorScheme: colorScheme,
                keyboard: .emailAddress,
                autocapitalization: .never
            )

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                Text(AppLocalized.text("videohub.field.note_optional", fallback: "Note (optional)"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                TextEditor(text: $viewModel.notes)
                    .frame(minHeight: 110)
                    .padding(12)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                    )
            }

            Button {
                showingFileImporter = true
            } label: {
                Label(AppLocalized.text("videohub.upload.pick_videos", fallback: "Choose videos"), systemImage: "video.badge.plus")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }

            Text(AppLocalized.text("videohub.upload.or_link", fallback: "Or paste a video link."))
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.external_link", fallback: "Google Drive / MEGA / other video link"),
                text: $viewModel.externalVideoURL,
                colorScheme: colorScheme,
                keyboard: .URL,
                autocapitalization: .never
            )
            .buttonStyle(.bordered)
            .tint(AppColors.accentMystic(for: colorScheme))

            if !viewModel.selectedFiles.isEmpty {
                VStack(spacing: SkydownLayout.stackSpacingPill) {
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
                    Label(AppLocalized.text("videohub.upload.submit", fallback: "Upload videos"), systemImage: "arrow.up.circle.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
            }
            .background(AppColors.accent(for: colorScheme))
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
            .disabled(!viewModel.canUpload)
            .opacity(viewModel.canUpload ? 1 : 0.6)

            Button {
                Task {
                    await viewModel.addExternalVideo()
                }
            } label: {
                Label(AppLocalized.text("videohub.upload.share_external", fallback: "Share external video"), systemImage: "link.badge.plus")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.bordered)
            .disabled(!viewModel.canAddExternalVideo)
            .opacity(viewModel.canAddExternalVideo ? 1 : 0.6)
        }
        .padding(SkydownLayout.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    private var playerCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            Text(AppLocalized.text(
                "videohub.player.focus_hint",
                fallback: "The clip stays large, vertical, and in focus. You can jump into full video mode anytime."
            ))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let selectedVideo {
                ZStack(alignment: .bottomLeading) {
                    Group {
                        if selectedVideo.youTubeItem != nil {
                            ZStack {
                                RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
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

                                VStack(spacing: SkydownLayout.stackSpacingComfortable) {
                                    Image(systemName: "play.rectangle.fill")
                                        .font(.system(size: 54, weight: .bold))
                                        .foregroundColor(.white.opacity(0.95))

                                    Text(AppLocalized.text("videohub.player.youtube_title", fallback: "YouTube clip"))
                                        .font(.title3.weight(.bold))
                                        .foregroundColor(.white)

                                    Text(AppLocalized.text(
                                        "videohub.player.youtube_subtitle",
                                        fallback: "The clip is set up as a YouTube video and plays right in the app."
                                    ))
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
                                RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                                    .fill(AppColors.secondaryBackground(for: colorScheme))

                                VStack(spacing: SkydownLayout.stackSpacingCompact) {
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
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))

                    LinearGradient(
                        colors: [
                            .clear,
                            Color.black.opacity(0.24),
                            Color.black.opacity(0.78)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSubtle) {
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
                    .padding(SkydownLayout.cardPadding)
                }

                if selectedVideo.supportsInlinePlayback && selectedVideo.youTubeItem == nil {
                    Button {
                        openVideoPlayer()
                    } label: {
                        Label(AppLocalized.text("videohub.player.open_fullscreen", fallback: "Open fullscreen"), systemImage: "rectangle.portrait.and.arrow.right")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accentMystic(for: colorScheme))
                } else if let youTubeItem = selectedVideo.youTubeItem {
                    Button {
                        presentSheet(.youTube(youTubeItem))
                    } label: {
                        Label(AppLocalized.text("videohub.player.open_youtube", fallback: "Watch on YouTube"), systemImage: "play.rectangle.fill")
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
                }

                if !selectedVideo.notes.isEmpty {
                    Text(selectedVideo.notes)
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else {
                Text(AppLocalized.text("videohub.player.no_focus", fallback: "No focus video yet. When a clip is published, it appears here."))
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
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
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    private var libraryCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(
                        viewModel.isAdmin
                        ? String(
                            format: AppLocalized.text("videohub.library.count_admin", fallback: "%d titles in hub"),
                            viewModel.videos.count
                        )
                        : String(
                            format: AppLocalized.text("videohub.library.count_viewer", fallback: "%d ready to watch"),
                            viewModel.videos.count
                        )
                    )
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if viewModel.isAdmin {
                        Text(AppLocalized.text("videohub.library.owner_title", fallback: "Owner video control"))
                            .font(.headline.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                        Text(AppLocalized.text("videohub.library.owner_subtitle", fallback: "Browse the list, set the home video, edit or delete clips."))
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }

                Spacer()

                if viewModel.isAdmin {
                    Button {
                        editingVideoID = nil
                        showingAdminEditor = false
                        withAnimation(SkydownMotion.screenTransition) {
                            showingUploadComposer = true
                        }
                    } label: {
                        Label(AppLocalized.text("videohub.library.new", fallback: "New"), systemImage: "plus")
                            .labelStyle(.iconOnly)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accent(for: colorScheme))
                    .accessibilityLabel(AppLocalized.text("videohub.library.new_a11y", fallback: "Create new video"))
                }
            }
            .accessibilityIdentifier("video.hub.library.header")

            if viewModel.isLoadingVideos {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    ProgressView()
                        .controlSize(.small)
                    Text(AppLocalized.text("videohub.library.loading", fallback: "Preparing videos…"))
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else if viewModel.videos.isEmpty {
                Text(
                    viewModel.isAdmin
                    ? AppLocalized.text("videohub.library.empty_admin", fallback: "No videos in the hub yet. New uploads appear here automatically.")
                    : AppLocalized.text("videohub.library.empty_guest", fallback: "No videos are published yet. Please check back later.")
                )
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else {
                ForEach(Array(viewModel.videos.enumerated()), id: \.element.id) { index, video in
                    VideoHubLibraryRow(
                        video: video,
                        isAdmin: viewModel.isAdmin,
                        isSelected: playbackManager.selectedVideoID == video.id,
                        isPlaying: playbackManager.playingVideoID == video.id,
                        colorScheme: colorScheme,
                        rowIndex: index,
                        presentation: videoHubRowPresentation(index: index, total: viewModel.videos.count),
                        onSelect: { playbackManager.load(video: video) },
                        onPlayToggle: { playbackManager.togglePlayback(for: video) },
                        onOpenReel: {
                            playbackManager.load(video: video)
                            playbackManager.player.pause()
                            activePresentedSheetBinding.wrappedValue = nil
                            showingAdminEditor = false
                            showingReelViewer = true
                        },
                        onOpenOriginal: {
                            showingAdminEditor = false
                            openOriginalVideo(video)
                        },
                        onToggleHomeFeatured: {
                            Task {
                                await viewModel.toggleHomeFeatured(video)
                            }
                        },
                        onEdit: {
                            withAnimation(SkydownMotion.screenTransition) {
                                editingVideoID = editingVideoID == video.id ? nil : video.id
                            }
                        },
                        onDelete: {
                            Task {
                                if editingVideoID == video.id {
                                    editingVideoID = nil
                                }
                                await viewModel.deleteVideo(video)
                            }
                        }
                    )
                    .padding(
                        .top,
                        (index == 0 && viewModel.videos.count > 1) ? 4
                            : ((index == 2 && viewModel.videos.count > 2) ? 8 : 0)
                    )
                    .padding(
                        .bottom,
                        (index == 0 && viewModel.videos.count > 1) ? 8 : 0
                    )

                    if editingVideoID == video.id {
                        VideoOwnerEditPanel(
                            video: video,
                            colorScheme: colorScheme,
                            onCancel: {
                                withAnimation(SkydownMotion.screenTransition) {
                                    editingVideoID = nil
                                }
                            },
                            onSave: { title, projectName, notes, isPublic in
                                Task {
                                    await viewModel.updateVideo(
                                        video,
                                        title: title,
                                        projectName: projectName,
                                        notes: notes,
                                        isPublic: isPublic
                                    )
                                    withAnimation(SkydownMotion.screenTransition) {
                                        editingVideoID = nil
                                    }
                                }
                            }
                        )
                        .transition(.move(edge: .top).combined(with: .opacity))
                    }
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
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

}

private enum VideoHubLibraryRowPresentation {
    case featured
    case secondary
    case catalog
}

fileprivate func youTubeLibraryPosterURL(for video: SkydownVideoHubItem) -> URL? {
    guard let item = video.youTubeItem else { return nil }
    let raw = item.urlString.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !raw.isEmpty else { return nil }
    let url = URL(string: raw) ?? URL(string: "https://\(raw)")
    guard let url else { return nil }
    let comp = URLComponents(url: url, resolvingAgainstBaseURL: false)
    if let v = comp?.queryItems?.first(where: { $0.name == "v" || $0.name == "vi" })?.value,
       v.count == 11 {
        return URL(string: "https://img.youtube.com/vi/\(v)/hqdefault.jpg")
    }
    if let host = url.host?.lowercased(), host.contains("youtu.be") {
        if let id = url.path.split(separator: "/").map(String.init).last, id.count == 11 {
            return URL(string: "https://img.youtube.com/vi/\(id)/hqdefault.jpg")
        }
    }
    let pathParts = url.path.split(separator: "/").map(String.init)
    if let embed = pathParts.firstIndex(of: "embed"), pathParts.indices.contains(embed + 1) {
        let id = pathParts[embed + 1]
        if id.count == 11 {
            return URL(string: "https://img.youtube.com/vi/\(id)/hqdefault.jpg")
        }
    }
    return nil
}

private struct VideoHubLibraryRow: View {
    let video: SkydownVideoHubItem
    let isAdmin: Bool
    let isSelected: Bool
    let isPlaying: Bool
    let colorScheme: ColorScheme
    let rowIndex: Int
    let presentation: VideoHubLibraryRowPresentation
    let onSelect: () -> Void
    let onPlayToggle: () -> Void
    let onOpenReel: () -> Void
    let onOpenOriginal: () -> Void
    let onToggleHomeFeatured: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    private var routeTitle: String {
        if video.supportsInlinePlayback {
            return AppLocalized.text("videohub.route.reel", fallback: "Inline reel")
        }
        if video.opensOriginalInApp {
            return AppLocalized.text("videohub.route.original_in_app", fallback: "Original in app")
        }
        return AppLocalized.text("videohub.route.external", fallback: "External clip")
    }

    private var routeDetail: String {
        if isAdmin {
            if video.isPlayable {
                return isSelected
                    ? AppLocalized.text(
                        "videohub.route.detail.admin_playable_selected",
                        fallback: "This clip is in the player and can be started or stopped immediately."
                    )
                    : AppLocalized.text(
                        "videohub.route.detail.admin_playable_unselected",
                        fallback: "One tap loads the clip into the player. It stays visible as the focus video."
                    )
            }
            if video.supportsInlinePlayback {
                return AppLocalized.text("videohub.route.detail.admin_reel_flow", fallback: "The clip runs as an in-app reel with a fast preview flow.")
            }
            if video.opensOriginalInApp {
                return AppLocalized.text(
                    "videohub.route.detail.admin_original_flow",
                    fallback: "The original stays reachable in the app, including close and a safe way back."
                )
            }
            return AppLocalized.text("videohub.route.detail.admin_external_only", fallback: "Only the external open action is available here.")
        }

        if video.supportsInlinePlayback {
            return AppLocalized.text("videohub.route.detail.user_reel", fallback: "One tap starts the direct video view without an extra step.")
        }
        if video.opensOriginalInApp {
            return AppLocalized.text(
                "videohub.route.detail.user_original",
                fallback: "One tap opens the in-app original view with a safe return to the app."
            )
        }
        return AppLocalized.text("videohub.route.detail.user_external", fallback: "This video currently opens via an external link.")
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
        switch presentation {
        case .catalog:
            return "\(video.projectName) · \(date)"
        case .secondary:
            var line = "\(video.projectName) · \(date)"
            if isAdmin {
                let visibilityLabel = video.isPublic
                    ? AppLocalized.text("videohub.visibility.public", fallback: "Public")
                    : AppLocalized.text("videohub.visibility.private", fallback: "Private")
                line += " · \(visibilityLabel)"
                if video.isHomeFeatured { line += " · \(AppLocalized.text("videohub.meta.home", fallback: "Home"))" }
            }
            return line
        case .featured:
            var line = "\(video.projectName) · \(date)"
            if isAdmin {
                let visibilityLabel = video.isPublic
                    ? AppLocalized.text("videohub.visibility.public", fallback: "Public")
                    : AppLocalized.text("videohub.visibility.private", fallback: "Private")
                line += " · \(visibilityLabel)"
                if video.isHomeFeatured { line += " · \(AppLocalized.text("videohub.meta.home", fallback: "Home"))" }
            }
            return line
        }
    }

    private var isCatalogOddStripe: Bool {
        presentation == .catalog && rowIndex % 2 == 1
    }

    private var thumbSize: (w: CGFloat, h: CGFloat) {
        switch presentation {
        case .secondary: (96, 54)
        case .catalog: (70, 40)
        case .featured: (0, 0)
        }
    }

    private var posterUrl: URL? { youTubeLibraryPosterURL(for: video) }

    var body: some View {
        Group {
            if presentation == .featured {
                featuredImmersive
            } else {
                listRow
            }
        }
        .padding(
            presentation == .featured
                ? EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0)
                : EdgeInsets(
                    top: presentation == .secondary ? 12 : 9,
                    leading: presentation == .secondary ? 15 : 12,
                    bottom: presentation == .secondary ? 12 : 9,
                    trailing: presentation == .secondary ? 15 : 12
                )
        )
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(
                cornerRadius: presentation == .featured ? 26
                    : (presentation == .secondary ? 20 : 17),
                style: .continuous
            )
            .fill(
                AppColors.cardBackground(for: colorScheme)
                    .opacity(
                        isCatalogOddStripe
                            ? 0.88
                            : (presentation == .catalog ? 0.91 : 0.98)
                    )
            )
        )
        .overlay(
            RoundedRectangle(
                cornerRadius: presentation == .featured ? 26
                    : (presentation == .secondary ? 20 : 17),
                style: .continuous
            )
            .stroke(
                AppColors.accent(for: colorScheme).opacity(
                    presentation == .featured ? 0.12
                        : (isCatalogOddStripe ? 0.11 : (presentation == .secondary ? 0.1 : 0.08))
                ),
                lineWidth: presentation == .featured ? 1.0 : 0.75
            )
        )
    }

    @ViewBuilder
    private var featuredImmersive: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNone) {
            ZStack(alignment: .bottomLeading) {
                Group {
                    if let url = posterUrl {
                        AsyncImage(url: url) { phase in
                            if case .success(let image) = phase {
                                image
                                    .resizable()
                                    .scaledToFill()
                            } else {
                                LinearGradient(
                                    colors: [
                                        routeAccent.opacity(0.9),
                                        AppColors.cinematicShadow(for: colorScheme).opacity(0.82)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            }
                        }
                    } else {
                        LinearGradient(
                            colors: [
                                routeAccent.opacity(0.9),
                                AppColors.cinematicShadow(for: colorScheme).opacity(0.82)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    }
                }
                .frame(maxWidth: .infinity)
                .aspectRatio(16.0 / 9.0, contentMode: .fit)
                .clipped()
                VStack {
                    Spacer(minLength: 0)
                    LinearGradient(
                        colors: [Color.clear, Color.black.opacity(0.72)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 104)
                }
                HStack(alignment: .lastTextBaseline, spacing: SkydownLayout.stackSpacingCompact) {
                    Text(video.title)
                        .font(.title2.weight(.semibold))
                        .foregroundColor(.white)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                        .shadow(color: .black.opacity(0.4), radius: 3, y: 1)
                    Spacer()
                    Image(systemName: isSelected ? "play.rectangle.fill" : "video.fill")
                        .font(.body)
                        .foregroundColor(.white.opacity(0.45))
                }
                .padding(SkydownLayout.cardPadding)
            }
            .clipped()
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                Text(libraryRowMetaLine)
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.95))
                if isAdmin, !video.fileName.isEmpty {
                    Text(video.fileName)
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                        .lineLimit(1)
                }
                if isAdmin, !video.notes.isEmpty {
                    Text(video.notes)
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                        .lineLimit(1)
                }
                if isAdmin {
                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        Button(action: onSelect) {
                            Label(
                                isSelected
                                    ? AppLocalized.text("videohub.action.in_player", fallback: "In player")
                                    : AppLocalized.text("videohub.action.select", fallback: "Select"),
                                systemImage: "rectangle.on.rectangle"
                            )
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)

                        Button(action: video.isPlayable ? onPlayToggle : (video.supportsInlinePlayback ? onOpenReel : onOpenOriginal)) {
                            Label(
                                video.isPlayable
                                    ? (isPlaying
                                        ? AppLocalized.text("videohub.action.stop", fallback: "Stop")
                                        : AppLocalized.text("videohub.action.play", fallback: "Play"))
                                    : (video.supportsInlinePlayback
                                        ? AppLocalized.text("videohub.action.watch", fallback: "Watch")
                                        : AppLocalized.text("videohub.action.open", fallback: "Open")),
                                systemImage: video.isPlayable
                                    ? (isPlaying ? "stop.fill" : "play.fill")
                                    : (video.supportsInlinePlayback ? "play.rectangle.fill" : "arrow.up.forward.square")
                            )
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(video.provider == .youTube ? AppColors.youtube(for: colorScheme) : AppColors.accent(for: colorScheme))
                        .disabled(!video.isPlayable && !video.supportsInlinePlayback && !video.opensOriginalInApp)
                    }
                } else {
                    if video.supportsInlinePlayback {
                        Button(action: onOpenReel) {
                            Label(AppLocalized.text("videohub.action.direct_video", fallback: "Play inline"), systemImage: "play.rectangle.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.accentMystic(for: colorScheme))
                    } else if video.opensOriginalInApp {
                        Button(action: onOpenOriginal) {
                            Label(
                                video.directOpenActionTitle,
                                systemImage: "arrow.up.forward.square"
                            )
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.accent(for: colorScheme))
                    }
                }

                if isAdmin {
                    VStack(spacing: SkydownLayout.stackSpacingPill) {
                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            Button(action: onToggleHomeFeatured) {
                                Label(
                                    video.isHomeFeatured
                                        ? AppLocalized.text("videohub.action.home_on", fallback: "On home")
                                        : AppLocalized.text("videohub.action.show_home", fallback: "Show on home"),
                                    systemImage: "house.fill"
                                )
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)

                            Button(action: onEdit) {
                                Label(AppLocalized.text("videohub.action.edit", fallback: "Edit"), systemImage: "slider.horizontal.3")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)
                        }

                        Button(role: .destructive, action: onDelete) {
                            Label(AppLocalized.text("videohub.action.delete", fallback: "Delete"), systemImage: "trash")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }
            .padding(SkydownLayout.cardPadding)
        }
    }

    @ViewBuilder
    private var listRow: some View {
        VStack(alignment: .leading, spacing: presentation == .catalog ? 7 : 9) {
            HStack(alignment: .top, spacing: presentation == .secondary ? 12 : 7) {
                listThumb
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(video.title)
                        .font(
                            presentation == .secondary
                                ? .subheadline.weight(.semibold)
                                : .subheadline
                        )
                        .foregroundColor(
                            AppColors.text(for: colorScheme)
                                .opacity(presentation == .catalog ? 0.9 : 1.0)
                        )
                        .lineLimit(2)

                    Text(libraryRowMetaLine)
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                        .lineLimit(1)

                    if isAdmin, !video.fileName.isEmpty, presentation == .secondary {
                        Text(video.fileName)
                            .font(.caption2)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                            .lineLimit(1)
                    }
                    if !video.notes.isEmpty, presentation == .secondary {
                        Text(video.notes)
                            .font(.caption2)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.85))
                            .lineLimit(1)
                    }
                }
                Spacer()
            }

            if presentation != .catalog {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
                Text(routeTitle)
                    .font(.caption.weight(.semibold))
                    .foregroundColor(routeAccent.opacity(0.95))
                Text(
                    presentation == .secondary
                        ? (isAdmin
                            ? AppLocalized.text("videohub.route.secondary_admin", fallback: "Your pick goes straight to the player.")
                            : AppLocalized.text("videohub.route.secondary_user", fallback: "Tap sets focus; the player follows above.")
                        ) : routeDetail
                )
                .font(.caption2)
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.92))
                .lineLimit(presentation == .secondary ? 1 : 2)
            }
            }

            if isAdmin {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    Button(action: onSelect) {
                        Label(
                            isSelected
                                ? AppLocalized.text("videohub.action.in_player", fallback: "In player")
                                : AppLocalized.text("videohub.action.select", fallback: "Select"),
                            systemImage: "rectangle.on.rectangle"
                        )
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button(action: video.isPlayable ? onPlayToggle : (video.supportsInlinePlayback ? onOpenReel : onOpenOriginal)) {
                        Label(
                            video.isPlayable
                                ? (isPlaying
                                    ? AppLocalized.text("videohub.action.stop", fallback: "Stop")
                                    : AppLocalized.text("videohub.action.play", fallback: "Play"))
                                : (video.supportsInlinePlayback
                                    ? AppLocalized.text("videohub.action.watch", fallback: "Watch")
                                    : AppLocalized.text("videohub.action.open", fallback: "Open")),
                            systemImage: video.isPlayable
                                ? (isPlaying ? "stop.fill" : "play.fill")
                                : (video.supportsInlinePlayback ? "play.rectangle.fill" : "arrow.up.forward.square")
                        )
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(video.provider == .youTube ? AppColors.youtube(for: colorScheme) : AppColors.accent(for: colorScheme))
                    .disabled(!video.isPlayable && !video.supportsInlinePlayback && !video.opensOriginalInApp)
                }
            } else {
                if video.supportsInlinePlayback {
                    Button(action: onOpenReel) {
                        Label(AppLocalized.text("videohub.action.direct_video", fallback: "Play inline"), systemImage: "play.rectangle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accentMystic(for: colorScheme))
                } else if video.opensOriginalInApp {
                    Button(action: onOpenOriginal) {
                        Label(
                            video.directOpenActionTitle,
                            systemImage: "arrow.up.forward.square"
                        )
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accent(for: colorScheme))
                }
            }

            if isAdmin {
                VStack(spacing: SkydownLayout.stackSpacingPill) {
                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        Button(action: onToggleHomeFeatured) {
                            Label(
                                video.isHomeFeatured
                                    ? AppLocalized.text("videohub.action.home_on", fallback: "On home")
                                    : AppLocalized.text("videohub.action.show_home", fallback: "Show on home"),
                                systemImage: "house.fill"
                            )
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)

                        Button(action: onEdit) {
                            Label(AppLocalized.text("videohub.action.edit", fallback: "Edit"), systemImage: "slider.horizontal.3")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }

                    Button(role: .destructive, action: onDelete) {
                        Label(AppLocalized.text("videohub.action.delete", fallback: "Delete"), systemImage: "trash")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
    }

    @ViewBuilder
    private var listThumb: some View {
        ZStack(alignment: .bottomTrailing) {
            RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            routeAccent.opacity(0.88),
                            AppColors.cinematicShadow(for: colorScheme).opacity(0.72)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            if let url = posterUrl {
                AsyncImage(url: url) { phase in
                    if case .success(let image) = phase {
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } else {
                        Color.clear
                    }
                }
            }
            LinearGradient(
                colors: [Color.clear, Color.black.opacity(0.35)],
                startPoint: .top,
                endPoint: .bottom
            )
            Image(systemName: isSelected ? "play.rectangle.fill" : "video.fill")
                .font(.system(size: presentation == .secondary ? 18 : 14, weight: .semibold))
                .foregroundColor(.white.opacity(0.85))
                .padding(5)
        }
        .frame(width: thumbSize.w, height: thumbSize.h)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                .stroke(Color.white.opacity(0.1), lineWidth: 0.5)
        )
    }
}

private struct VideoOwnerEditPanel: View {
    let video: SkydownVideoHubItem
    let colorScheme: ColorScheme
    let onCancel: () -> Void
    let onSave: (String, String, String, Bool) -> Void

    @State private var title: String
    @State private var projectName: String
    @State private var notes: String
    @State private var isPublic: Bool

    private var sourceLabel: String {
        let fileName = video.fileName.trimmingCharacters(in: .whitespacesAndNewlines)
        return fileName.isEmpty ? video.provider.rawValue : fileName
    }

    init(
        video: SkydownVideoHubItem,
        colorScheme: ColorScheme,
        onCancel: @escaping () -> Void,
        onSave: @escaping (String, String, String, Bool) -> Void
    ) {
        self.video = video
        self.colorScheme = colorScheme
        self.onCancel = onCancel
        self.onSave = onSave
        _title = State(initialValue: video.title)
        _projectName = State(initialValue: video.projectName)
        _notes = State(initialValue: video.notes)
        _isPublic = State(initialValue: video.isPublic)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(AppLocalized.text("videohub.edit.title", fallback: "Edit video"))
                        .font(.headline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("\(sourceLabel) · \(skydownVideoDateFormatter.string(from: video.createdAt))")
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(1)
                }
                Spacer()
                Button(action: onCancel) {
                    Image(systemName: "xmark")
                        .font(.caption.weight(.bold))
                }
                .buttonStyle(.bordered)
                .accessibilityLabel(AppLocalized.text("videohub.edit.close_a11y", fallback: "Close editor"))
            }

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.title", fallback: "Title"),
                text: $title,
                colorScheme: colorScheme
            )

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.project_artist", fallback: "Project / artist"),
                text: $projectName,
                colorScheme: colorScheme
            )

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                Text(AppLocalized.text("videohub.field.notes", fallback: "Notes"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                TextEditor(text: $notes)
                    .frame(minHeight: 92)
                    .padding(12)
                    .scrollContentBackground(.hidden)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                    )
            }

            Toggle(isOn: $isPublic) {
                Label(
                    isPublic
                        ? AppLocalized.text("videohub.visibility.public_visible", fallback: "Public (visible)")
                        : AppLocalized.text("videohub.visibility.private_hidden", fallback: "Private (hidden)"),
                    systemImage: isPublic ? "checkmark.circle.fill" : "eye.slash.fill"
                )
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
            }
            .tint(AppColors.accent(for: colorScheme))

            HStack(spacing: SkydownLayout.stackSpacingPill) {
                Button(action: onCancel) {
                    Label(AppLocalized.text("common.cancel", fallback: "Cancel"), systemImage: "xmark")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                Button {
                    onSave(title, projectName, notes, isPublic)
                } label: {
                    Label(AppLocalized.text("common.save", fallback: "Save"), systemImage: "checkmark.circle.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
            }
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.74))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .accessibilityIdentifier("video.hub.owner.edit")
    }
}

private struct VideoReelViewer: View {
    let videos: [SkydownVideoHubItem]
    let initialIndex: Int
    @Environment(\.dismiss) private var dismiss
    @State private var currentIndex: Int
    @State private var player = AVPlayer()
    @State private var isTransitioning = false
    @State private var isPlaying = false

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
                                        VStack(spacing: SkydownLayout.stackSpacingCompact) {
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

                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
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
                            }
                            .padding(.horizontal, 20)
                            .padding(.bottom, 30)
                            .padding(.trailing, 54)

                            if index == currentIndex && video.isPlayable && !video.usesEmbeddedPreview {
                                VStack {
                                    Spacer()

                                    HStack {
                                        Spacer()

                                        SkydownVideoFullscreenControlBar(
                                            isPlaying: isPlaying,
                                            showsClipNavigation: videos.count > 1,
                                            canGoToPreviousClip: currentIndex > 0,
                                            canGoToNextClip: currentIndex < videos.count - 1,
                                            onPreviousClip: goToPreviousVideo,
                                            onRewind: { seekCurrentVideo(by: -10) },
                                            onPlayPause: togglePlayback,
                                            onForward: { seekCurrentVideo(by: 10) },
                                            onNextClip: goToNextVideo,
                                            onClose: { dismiss() }
                                        )
                                    }
                                    .padding(.trailing, 20)
                                    .padding(.bottom, 82)
                                    .frame(maxWidth: .infinity)
                                }
                                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                                .zIndex(6)
                            }
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
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
                        Text(AppLocalized.text("videohub.reel.brand_title", fallback: "SkyOS Video"))
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(.white.opacity(0.92))

                        Text(
                            String(
                                format: AppLocalized.text("videohub.reel.position_format", fallback: "%1$d of %2$d"),
                                currentIndex + 1,
                                videos.count
                            )
                        )
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.70))

                        if videos.count > 1 {
                            Text(AppLocalized.text("videohub.reel.swipe_hint", fallback: "Swipe vertically through all clips."))
                                .font(.caption.weight(.medium))
                                .foregroundColor(.white.opacity(0.58))
                        }
                    }

                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.top, max(proxy.safeAreaInsets.top + 12, 22))
                .frame(maxWidth: .infinity, alignment: .top)
                .zIndex(1_100)

                SkydownVideoFullscreenCloseButton {
                    dismiss()
                }
                .position(
                    x: max(proxy.size.width - 46, 46),
                    y: max(proxy.safeAreaInsets.top + 48, 56)
                )
                .zIndex(2_000)

                if isTransitioning {
                    VStack(spacing: SkydownLayout.stackSpacingPill) {
                        ProgressView()
                            .tint(.white)
                        Text(AppLocalized.text("videohub.reel.preparing", fallback: "Preparing clip…"))
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.86))
                    }
                    .padding(.horizontal, SkydownLayout.cardPadding)
                    .padding(.vertical, 12)
                    .background(Color.black.opacity(0.46), in: RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
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
            isPlaying = false
        }
    }

    private var currentVideoSupportsAppControls: Bool {
        guard videos.indices.contains(currentIndex) else { return false }
        let video = videos[currentIndex]
        return video.isPlayable && !video.usesEmbeddedPreview
    }

    private func togglePlayback() {
        guard currentVideoSupportsAppControls else { return }
        if isPlaying {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }

    private func goToPreviousVideo() {
        guard currentIndex > 0 else { return }
        currentIndex -= 1
    }

    private func goToNextVideo() {
        guard currentIndex < videos.count - 1 else { return }
        currentIndex += 1
    }

    private func seekCurrentVideo(by seconds: Double) {
        guard currentVideoSupportsAppControls else { return }
        let currentSeconds = player.currentTime().seconds
        guard currentSeconds.isFinite else { return }

        var targetSeconds = currentSeconds + seconds
        if let durationSeconds = player.currentItem?.duration.seconds,
           durationSeconds.isFinite,
           durationSeconds > 0 {
            targetSeconds = min(targetSeconds, durationSeconds)
        }
        targetSeconds = max(0, targetSeconds)

        player.seek(
            to: CMTime(seconds: targetSeconds, preferredTimescale: 600),
            toleranceBefore: .zero,
            toleranceAfter: .zero
        )
    }

    private func playCurrent() {
        guard videos.indices.contains(currentIndex),
              let url = URL(string: videos[currentIndex].nativePlaybackURLString),
              !videos[currentIndex].nativePlaybackURLString.isEmpty else {
            player.pause()
            player.replaceCurrentItem(with: nil)
            isPlaying = false
            return
        }

        player.pause()
        player.replaceCurrentItem(with: AVPlayerItem(url: url))
        player.seek(to: .zero)
        player.play()
        isPlaying = true
    }
}

private struct ReelProgressRail: View {
    let currentIndex: Int
    let count: Int

    var body: some View {
        VStack(spacing: SkydownLayout.stackSpacingDense) {
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            Text(AppLocalized.text("videohub.equipment.section_title", fallback: "Equipment & software"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.equipment.section_subtitle", fallback: "Visual stack for shoot, edit, and finish."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if items.isEmpty {
                Text(AppLocalized.text("videohub.equipment.empty", fallback: "Equipment is being prepared and will appear here soon."))
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .padding(.top, 4)
            } else {
                VStack(spacing: SkydownLayout.stackSpacingPill) {
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
        .padding(SkydownLayout.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }
}

private struct VideoHubQuickActionDock: View {
    let colorScheme: ColorScheme
    let isUploadOpen: Bool
    let onOpenUpload: () -> Void

    var body: some View {
        VStack(alignment: .trailing, spacing: SkydownLayout.stackSpacingPill) {
            VideoHubQuickActionButton(
                title: isUploadOpen
                    ? AppLocalized.text("common.close", fallback: "Close")
                    : AppLocalized.text("videohub.dock.upload", fallback: "Upload"),
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
            HStack(spacing: SkydownLayout.stackSpacingPill) {
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
    var onTap: () -> Void = {}

    private var imageURL: URL? {
        guard let value = item.imageURLString, !value.isEmpty else { return nil }
        return URL(string: value)
    }

    var body: some View {
        Button(action: onTap) {
            rowContent
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private var rowContent: some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
            equipmentArtwork

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
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
            RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
    }

    private var equipmentArtwork: some View {
        ZStack {
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
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
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
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
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
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
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, SkydownLayout.cardPadding)
                .padding(.bottom, 28)
            }
            .background(AppColors.screenGradient(for: colorScheme).ignoresSafeArea())
            .navigationTitle(AppLocalized.text("videohub.equipment.nav_title", fallback: "Equipment"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(AppLocalized.text("common.done", fallback: "Done")) { dismiss() }
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
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
            previewArtwork
                .frame(width: 76, height: 76)
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                HStack(spacing: SkydownLayout.stackSpacingMicro) {
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

                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                    if let spotifyURL {
                        SocialLinkButton(
                            accessibilityTitle: AppLocalized.text("videohub.social.spotify", fallback: "Spotify"),
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
                            accessibilityTitle: AppLocalized.text("videohub.social.instagram", fallback: "Instagram"),
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
                            accessibilityTitle: AppLocalized.text("videohub.social.youtube", fallback: "YouTube"),
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
            RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.10), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
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
        HStack(spacing: 6) {
            Image(systemName: systemImage)
                .font(.system(size: 11, weight: .bold))
            Text(accessibilityTitle)
                .font(.caption2.weight(.semibold))
                .lineLimit(1)
        }
        .foregroundColor(foregroundColor)
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(background)
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.16), lineWidth: 1)
        )
        .overlay(
            Capsule()
                .fill(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(0.20),
                            Color.white.opacity(0)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .scaleEffect(0.94)
        )
        .shadow(color: .black.opacity(0.16), radius: 8, y: 4)
    }
}

struct VideoYouTubeRow: View {
    let item: SkydownYouTubeVideoItem
    let colorScheme: ColorScheme
    let onPlay: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
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
                Label(AppLocalized.text("videohub.youtube_row.play", fallback: "YouTube"), systemImage: "play.rectangle.fill")
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
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
    }
}

struct VideoPublicConfigEditorCard: View {
    let colorScheme: ColorScheme
    @ObservedObject var viewModel: SkydownVideoHubViewModel
    @State private var pendingUploadTarget: VideoPublicConfigImageTarget?
    @State private var activeUploadTarget: VideoPublicConfigImageTarget?
    private let editableImageUploadService = EditableImageAssetUploadService()

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
            Text(AppLocalized.text("videohub.editor.title", fallback: "Videography editor"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.editor.intro", fallback: "Owners and video admins manage equipment and featured collabs here. Images are picker-first and applied with a live preview."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(AppLocalized.text("videohub.editor.save_hint", fallback: "You can add, replace, or remove entries. Public data goes live only after you save."))
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                Text(AppLocalized.text("videohub.editor.equipment_heading", fallback: "Equipment"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                ForEach(Array(viewModel.publicConfig.equipmentItems.enumerated()), id: \.element.id) { _, item in
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.title", fallback: "Title"),
                            text: Binding(
                                get: { item.title },
                                set: { viewModel.updateEquipmentItem(item.id, title: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.detail", fallback: "Detail"),
                            text: Binding(
                                get: { item.detail },
                                set: { viewModel.updateEquipmentItem(item.id, detail: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        EditableImageField(
                            title: AppLocalized.text("videohub.editor.equipment_image_title", fallback: "Equipment image"),
                            imageURL: Binding(
                                get: { item.imageURLString ?? "" },
                                set: { viewModel.updateEquipmentItem(item.id, imageURLString: $0) }
                            ),
                            colorScheme: colorScheme,
                            isUploading: activeUploadTarget == .equipment(item.id),
                            uploadStatusText: AppLocalized.text("videohub.editor.equipment_upload_status", fallback: "Applying equipment image…"),
                            onPickImage: { pendingUploadTarget = .equipment(item.id) },
                            onRemoveImage: { removeEditableImage(for: .equipment(item.id)) }
                        )
                        Button(role: .destructive) {
                            viewModel.removeEquipmentItem(item.id)
                        } label: {
                            Label(AppLocalized.text("videohub.editor.remove_entry", fallback: "Remove entry"), systemImage: "trash")
                                .font(.caption.weight(.semibold))
                        }
                    }
                    .padding(14)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                }

                Button {
                    viewModel.addEquipmentItem()
                } label: {
                    Label(AppLocalized.text("videohub.editor.add_equipment", fallback: "Add equipment"), systemImage: "plus.circle.fill")
                        .font(.subheadline.weight(.semibold))
                }
                .buttonStyle(.bordered)
                .tint(AppColors.accent(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                Text(AppLocalized.text("videohub.editor.collabs_heading", fallback: "Featured collabs"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                ForEach(Array(viewModel.publicConfig.collaborationItems.enumerated()), id: \.element.id) { _, item in
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.name", fallback: "Name"),
                            text: Binding(
                                get: { item.name },
                                set: { viewModel.updateCollaborationItem(item.id, name: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.role", fallback: "Role"),
                            text: Binding(
                                get: { item.role },
                                set: { viewModel.updateCollaborationItem(item.id, role: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.highlight", fallback: "Highlight"),
                            text: Binding(
                                get: { item.highlight },
                                set: { viewModel.updateCollaborationItem(item.id, highlight: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.vibe", fallback: "Vibe"),
                            text: Binding(
                                get: { item.vibe },
                                set: { viewModel.updateCollaborationItem(item.id, vibe: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        EditableImageField(
                            title: AppLocalized.text("videohub.editor.collab_image_title", fallback: "Collab image"),
                            imageURL: Binding(
                                get: { item.imageURLString ?? "" },
                                set: { viewModel.updateCollaborationItem(item.id, imageURLString: $0) }
                            ),
                            colorScheme: colorScheme,
                            isUploading: activeUploadTarget == .collaboration(item.id),
                            uploadStatusText: AppLocalized.text("videohub.editor.collab_upload_status", fallback: "Applying collab image…"),
                            onPickImage: { pendingUploadTarget = .collaboration(item.id) },
                            onRemoveImage: { removeEditableImage(for: .collaboration(item.id)) }
                        )
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.spotify_artist_id", fallback: "Spotify artist ID"),
                            text: Binding(
                                get: { item.spotifyArtistID ?? "" },
                                set: { viewModel.updateCollaborationItem(item.id, spotifyArtistID: $0) }
                            ),
                            colorScheme: colorScheme,
                            autocapitalization: .never
                        )
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.instagram_url", fallback: "Instagram URL"),
                            text: Binding(
                                get: { item.instagramURLString ?? "" },
                                set: { viewModel.updateCollaborationItem(item.id, instagramURLString: $0) }
                            ),
                            colorScheme: colorScheme,
                            keyboard: .URL,
                            autocapitalization: .never
                        )
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.youtube_url", fallback: "YouTube URL"),
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
                            Label(AppLocalized.text("videohub.editor.remove_collab", fallback: "Remove collab"), systemImage: "trash")
                                .font(.caption.weight(.semibold))
                        }
                    }
                    .padding(14)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                }

                Button {
                    viewModel.addCollaborationItem()
                } label: {
                    Label(AppLocalized.text("videohub.editor.add_collab", fallback: "Add collab"), systemImage: "plus.circle.fill")
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
                    Label(AppLocalized.text("videohub.editor.save_public", fallback: "Save public data"), systemImage: "square.and.arrow.down.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accentMystic(for: colorScheme))
            .disabled(viewModel.isSavingPublicConfig)
        }
        .padding(SkydownLayout.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
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
                    viewModel.toastMessage = AppLocalized.text("videohub.toast.image_saved", fallback: "Image uploaded and applied.")
                    viewModel.toastStyle = .success
                    viewModel.showToast = true
                }
            } catch {
                await MainActor.run {
                    viewModel.toastMessage = String(
                        format: AppLocalized.text(
                            "videohub.toast.image_upload_failed_format",
                            fallback: "Could not upload image: %@"
                        ),
                        error.localizedDescription
                    )
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
                    viewModel.toastMessage = AppLocalized.text("videohub.toast.image_removed", fallback: "Image removed.")
                    viewModel.toastStyle = .success
                    viewModel.showToast = true
                }
            } catch {
                await MainActor.run {
                    viewModel.toastMessage = String(
                        format: AppLocalized.text(
                            "videohub.toast.image_removed_delete_failed_format",
                            fallback: "Image removed. Old upload could not be deleted: %@"
                        ),
                        error.localizedDescription
                    )
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
        HStack(spacing: SkydownLayout.stackSpacingCompact) {
            ZStack {
                Circle()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 42, height: 42)

                Image(systemName: "video.fill")
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

                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                            Text(title)
                                .font(.headline.weight(.bold))
                                .foregroundColor(.white)
                                .lineLimit(2)

                            Text(AppLocalized.text("videohub.original.inline_hint", fallback: "In-app playback. Close returns you right away."))
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.76))

                            HStack(spacing: SkydownLayout.stackSpacingPill) {
                                SkydownViewerToolbarButton(
                                    title: AppLocalized.text("videohub.viewer.external", fallback: "Open externally"),
                                    systemImage: "arrow.up.forward.square",
                                    isPrimary: false
                                ) {
                                    if let resolvedURL {
                                        openURL(resolvedURL)
                                    }
                                }

                                SkydownViewerToolbarButton(
                                    title: AppLocalized.text("common.close", fallback: "Close"),
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

                            VStack(spacing: SkydownLayout.stackSpacingPill) {
                                HStack(spacing: SkydownLayout.stackSpacingPill) {
                                    SkydownViewerToolbarButton(
                                        title: isPlaying
                                            ? AppLocalized.text("videohub.viewer.pause", fallback: "Pause")
                                            : AppLocalized.text("videohub.viewer.play", fallback: "Play"),
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
                                        title: AppLocalized.text("videohub.viewer.external", fallback: "Open externally"),
                                        systemImage: "arrow.up.forward.square",
                                        isPrimary: false
                                    ) {
                                        if let resolvedURL {
                                            openURL(resolvedURL)
                                        }
                                    }
                                }

                                SkydownViewerToolbarButton(
                                    title: AppLocalized.text("videohub.viewer.back_to_app", fallback: "Back to app"),
                                    systemImage: "xmark",
                                    isPrimary: true
                                ) {
                                    dismiss()
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.bottom, max(proxy.safeAreaInsets.bottom, SkydownLayout.cardPadding))
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .zIndex(3)
                    }
                    .overlay(alignment: .topTrailing) {
                        SkydownVideoFullscreenCloseButton {
                            dismiss()
                        }
                        .padding(.top, max(proxy.safeAreaInsets.top + 12, 22))
                        .padding(.trailing, 18)
                        .zIndex(1_100)
                    }
                }
            } else if let resolvedURL {
                SkydownManagedBrowserView(
                    url: resolvedURL,
                    title: title
                )
            } else {
                NavigationStack {
                    ContentUnavailableView {
                        Label(
                            AppLocalized.text("videohub.original.unavailable_title", fallback: "Original unavailable"),
                            systemImage: "play.rectangle"
                        )
                    } description: {
                        Text(AppLocalized.text("videohub.original.unavailable_message", fallback: "The original could not be loaded. Please try again in a moment."))
                    }
                    .navigationTitle(title.isEmpty ? AppLocalized.text("videohub.original_title", fallback: "Original") : title)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .topBarLeading) {
                            Button(AppLocalized.text("common.close", fallback: "Close")) {
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

struct SkydownVideoFullscreenCloseButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(Color.black.opacity(0.62))
                Circle()
                    .fill(.ultraThinMaterial)
                    .opacity(0.72)
                Image(systemName: "xmark")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
            }
            .frame(width: 52, height: 52)
            .overlay(
                Circle()
                    .stroke(Color.white.opacity(0.30), lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.50), radius: 18, x: 0, y: 7)
        }
        .buttonStyle(.plain)
        .contentShape(Circle())
        .skydownTactileAction()
        .accessibilityLabel(AppLocalized.text("videohub.a11y.close_video", fallback: "Close video"))
        .accessibilityIdentifier("video.fullscreen.close")
    }
}

struct SkydownVideoFullscreenControlBar: View {
    let isPlaying: Bool
    let showsClipNavigation: Bool
    let canGoToPreviousClip: Bool
    let canGoToNextClip: Bool
    let onPreviousClip: () -> Void
    let onRewind: () -> Void
    let onPlayPause: () -> Void
    let onForward: () -> Void
    let onNextClip: () -> Void
    let onClose: () -> Void

    var body: some View {
        HStack(spacing: SkydownLayout.stackSpacingDense) {
            if showsClipNavigation {
                SkydownVideoFullscreenIconButton(
                    systemImage: "backward.end.fill",
                    accessibilityLabel: AppLocalized.text("videohub.a11y.previous_clip", fallback: "Previous clip"),
                    isEnabled: canGoToPreviousClip,
                    action: onPreviousClip
                )
            }

            SkydownVideoFullscreenIconButton(
                systemImage: "gobackward.10",
                accessibilityLabel: AppLocalized.text("videohub.a11y.rewind_10", fallback: "Rewind 10 seconds"),
                action: onRewind
            )

            SkydownVideoFullscreenIconButton(
                systemImage: isPlaying ? "pause.fill" : "play.fill",
                accessibilityLabel: isPlaying
                    ? AppLocalized.text("videohub.a11y.pause_video", fallback: "Pause video")
                    : AppLocalized.text("videohub.a11y.play_video", fallback: "Play video"),
                isProminent: true,
                action: onPlayPause
            )

            SkydownVideoFullscreenIconButton(
                systemImage: "goforward.10",
                accessibilityLabel: AppLocalized.text("videohub.a11y.forward_10", fallback: "Forward 10 seconds"),
                action: onForward
            )

            if showsClipNavigation {
                SkydownVideoFullscreenIconButton(
                    systemImage: "forward.end.fill",
                    accessibilityLabel: AppLocalized.text("videohub.a11y.next_clip", fallback: "Next clip"),
                    isEnabled: canGoToNextClip,
                    action: onNextClip
                )
            }

            Rectangle()
                .fill(Color.white.opacity(0.18))
                .frame(width: 1, height: 24)
                .padding(.horizontal, 2)

            SkydownVideoFullscreenIconButton(
                systemImage: "xmark",
                accessibilityLabel: AppLocalized.text("videohub.a11y.close_video", fallback: "Close video"),
                isProminent: true,
                action: onClose
            )
        }
        .padding(6)
        .background(
            Capsule()
                .fill(Color.black.opacity(0.58))
        )
        .background(.ultraThinMaterial, in: Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.22), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.36), radius: 16, x: 0, y: 7)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("video.fullscreen.controls")
    }
}

private struct SkydownVideoFullscreenIconButton: View {
    let systemImage: String
    let accessibilityLabel: String
    var isProminent = false
    var isEnabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: isProminent ? 15 : 14, weight: .bold))
                .foregroundColor(.white.opacity(isEnabled ? 0.96 : 0.38))
                .frame(width: isProminent ? 40 : 36, height: 36)
                .background(
                    Circle()
                        .fill(isProminent ? Color.white.opacity(0.16) : Color.white.opacity(0.08))
                )
        }
        .buttonStyle(.plain)
        .contentShape(Circle())
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1 : 0.48)
        .skydownTactileAction()
        .accessibilityLabel(accessibilityLabel)
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
                    RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                        .fill(isPrimary ? Color.white : Color.white.opacity(0.12))
                )
                .foregroundColor(isPrimary ? .black : .white)
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
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

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                    Text(title.isEmpty ? AppLocalized.text("videohub.original_title", fallback: "Original") : title)
                        .font(.headline.weight(.bold))
                        .foregroundColor(.white)
                        .lineLimit(2)

                    Text(AppLocalized.text("videohub.viewer.web_hint", fallback: "Web view with visible actions for back, forward, external, and close."))
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.76))

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SkydownViewerToolbarButton(
                            title: AppLocalized.text("videohub.viewer.back", fallback: "Back"),
                            systemImage: "chevron.left",
                            isPrimary: false,
                            isEnabled: browserState.canGoBack
                        ) {
                            browserState.goBack()
                        }

                        SkydownViewerToolbarButton(
                            title: AppLocalized.text("videohub.viewer.forward", fallback: "Forward"),
                            systemImage: "chevron.right",
                            isPrimary: false,
                            isEnabled: browserState.canGoForward
                        ) {
                            browserState.goForward()
                        }
                    }

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SkydownViewerToolbarButton(
                            title: AppLocalized.text("videohub.viewer.external", fallback: "Open externally"),
                            systemImage: "arrow.up.forward.square",
                            isPrimary: false
                        ) {
                            openURL(browserState.currentURL ?? url)
                        }

                        SkydownViewerToolbarButton(
                            title: AppLocalized.text("common.close", fallback: "Close"),
                            systemImage: "xmark",
                            isPrimary: true
                        ) {
                            dismiss()
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, max(proxy.safeAreaInsets.top, 12))
                .padding(.bottom, SkydownLayout.cardPadding)
                .background(Color.black.opacity(0.42), in: RoundedRectangle(cornerRadius: SkydownLayout.spotlightRadius, style: .continuous))
                .padding(.horizontal, SkydownLayout.cardPadding)

                VStack {
                    Spacer()

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SkydownViewerToolbarButton(
                            title: AppLocalized.text("videohub.viewer.external", fallback: "Open externally"),
                            systemImage: "arrow.up.forward.square",
                            isPrimary: false
                        ) {
                            openURL(browserState.currentURL ?? url)
                        }

                        SkydownViewerToolbarButton(
                            title: AppLocalized.text("videohub.viewer.back_to_app", fallback: "Back to app"),
                            systemImage: "xmark",
                            isPrimary: true
                        ) {
                            dismiss()
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, max(proxy.safeAreaInsets.bottom, SkydownLayout.cardPadding))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
                .overlay(alignment: .topTrailing) {
                    SkydownVideoFullscreenCloseButton {
                        dismiss()
                    }
                    .padding(.top, max(proxy.safeAreaInsets.top + 12, 22))
                    .padding(.trailing, 18)
                    .zIndex(1_100)
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
        guard skydownAllowsEmbeddedWebNavigation(initialURL) else {
            refresh(from: webView)
            return
        }
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
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false

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

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            decisionHandler(skydownAllowsEmbeddedWebNavigation(navigationAction.request.url) ? .allow : .cancel)
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

private func skydownAllowsEmbeddedWebNavigation(_ url: URL?) -> Bool {
    guard let scheme = url?.scheme?.lowercased() else { return false }
    return scheme == "https" || scheme == "http" || scheme == "about"
}

struct ExternalVideoEmbedSurface: UIViewRepresentable {
    let urlString: String

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard let url = URL(string: urlString) else { return }
        guard skydownAllowsEmbeddedWebNavigation(url) else { return }
        if webView.url?.absoluteString != url.absoluteString {
            webView.load(URLRequest(url: url))
        }
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            decisionHandler(skydownAllowsEmbeddedWebNavigation(navigationAction.request.url) ? .allow : .cancel)
        }
    }
}
