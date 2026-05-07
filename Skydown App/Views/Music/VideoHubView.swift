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
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
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
                            let scrollAnimation = SkydownMotion.preferredSmoothScroll(accessibilityReduceMotion: reduceMotion)
                            withAnimation(scrollAnimation) {
                                scrollProxy.scrollTo(VideoHubSectionAnchor.equipment.rawValue, anchor: .top)
                            }
                        },
                        onOpenCollaborations: {
                            let scrollAnimation = SkydownMotion.preferredSmoothScroll(accessibilityReduceMotion: reduceMotion)
                            withAnimation(scrollAnimation) {
                                scrollProxy.scrollTo(VideoHubSectionAnchor.collaborations.rawValue, anchor: .top)
                            }
                        }
                    )
                    if viewModel.isAdmin && showingUploadComposer {
                        uploadCard
                            .transition(
                                reduceMotion
                                    ? .opacity
                                    : .move(edge: .top).combined(with: .opacity)
                            )
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
                    withAnimation(SkydownMotion.preferredScreenTransition(accessibilityReduceMotion: reduceMotion)) {
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
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.back", fallback: "Zurueck"),
                        systemImage: "chevron.backward",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: onBack
                    )
                    .skydownInteractiveFeedback()
                }
            }

            ToolbarItemGroup(placement: .topBarTrailing) {
                if viewModel.isUploading {
                    SkydownPremiumCircularProgress(
                        tint: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        scale: 0.72
                    )
                }

                if viewModel.isAdmin {
                    SkydownBrandActionButton(
                        title: showingUploadComposer
                            ? AppLocalized.text("common.close", fallback: "Close")
                            : AppLocalized.text("videohub.dock.upload", fallback: "Upload"),
                        systemImage: showingUploadComposer ? "xmark.circle" : "arrow.up.circle",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: {
                            withAnimation(SkydownMotion.preferredScreenTransition(accessibilityReduceMotion: reduceMotion)) {
                                showingUploadComposer.toggle()
                            }
                        }
                    )
                    .skydownInteractiveFeedback()
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
            NavigationStack {
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
                .navigationTitle(AppLocalized.text("videohub.admin.title", fallback: "Video admin"))
                .navigationBarTitleDisplayMode(.inline)
                .skydownNavigationChrome(colorScheme: colorScheme)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        SkydownBrandActionButton(
                            title: AppLocalized.text("common.done", fallback: "Done"),
                            accent: AppColors.accent(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .subheadline.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: false,
                            action: { showingAdminEditor = false }
                        )
                        .skydownInteractiveFeedback()
                    }
                }
            }
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
            Text(AppLocalized.text("videohub.collabs.title", fallback: "Empfohlene Kollaborationen"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.collabs.subtitle", fallback: "Kreative hinter den Visuals."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if collaborationItems.isEmpty {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                    Text(AppLocalized.text("videohub.collabs.empty", fallback: "Empfohlene Kollaborationen sind in Vorbereitung. Dieser Bereich fuellt sich nach und nach."))
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .padding(.top, 4)
                    if viewModel.isAdmin {
                        SkydownBrandActionButton(
                            title: AppLocalized.text("videohub.admin.open_editor", fallback: "Editor oeffnen"),
                            systemImage: "slider.horizontal.3",
                            accent: AppColors.accentMystic(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .subheadline.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 9,
                            expandToFullWidth: false,
                            action: { showingAdminEditor = true }
                        )
                        Text(AppLocalized.text("videohub.admin.hint", fallback: "Nur fuer VideoHub-Admins sichtbar."))
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
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
                    fallback: "Einstiegspunkt — der Hauptfokus liegt im Player und in der Clip-Liste darunter."
                ),
            detail: settings.resolvedVideoHubDetail
                ?? AppLocalized.text(
                    "videohub.hero.fallback_detail",
                    fallback: "Hier den Ueberblick waehlen, unten fokussieren — ohne Feed-Rauschen."
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
                Text(AppLocalized.text("videohub.social.section_title", fallback: "Links"))
                    .font(.caption2.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

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
                        text: AppLocalized.text("videohub.badge.collabs", fallback: "Kollaborationen"),
                        isAccent: false,
                        onTap: onOpenCollaborations
                    )
                }
            }
        }
    }

    private var formatCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            Text(AppLocalized.text("videohub.format.title", fallback: "Format-Hinweis"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.format.body", fallback: "MP4, MOV oder M4V."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(AppLocalized.text("videohub.format.smaller_faster", fallback: "Kleinere Dateien laden schneller hoch."))
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
            Text(AppLocalized.text("videohub.admin.title", fallback: "Video-Admin"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.admin.subtitle", fallback: "Equipment und Kollaborationen im Editor."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            SkydownBrandActionButton(
                title: AppLocalized.text("videohub.admin.open_editor", fallback: "Editor oeffnen"),
                systemImage: "slider.horizontal.3",
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                font: .headline,
                cornerRadius: SkydownLayout.denseRadius,
                verticalPadding: 14,
                action: { showingAdminEditor = true }
            )
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
            Text(AppLocalized.text("videohub.upload.title", fallback: "Video-Upload"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.upload.admin_only", fallback: "Nur fuer Admins."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.title", fallback: "Titel"),
                text: $viewModel.videoTitle,
                colorScheme: colorScheme
            )

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.project_artist", fallback: "Projekt / Artist"),
                text: $viewModel.projectName,
                colorScheme: colorScheme
            )

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.email", fallback: "E-Mail"),
                text: $viewModel.email,
                colorScheme: colorScheme,
                keyboard: .emailAddress,
                autocapitalization: .never
            )

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                Text(AppLocalized.text("videohub.field.note_optional", fallback: "Hinweis (optional)"))
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

            SkydownBrandActionButton(
                title: AppLocalized.text("videohub.upload.pick_videos", fallback: "Videos auswaehlen"),
                systemImage: "video.badge.plus",
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                role: .muted,
                font: .headline,
                cornerRadius: SkydownLayout.denseRadius,
                verticalPadding: 14,
                action: { showingFileImporter = true }
            )

            Text(AppLocalized.text("videohub.upload.or_link", fallback: "Oder einen Videolink einfuegen."))
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.external_link", fallback: "Google Drive / MEGA / anderer Videolink"),
                text: $viewModel.externalVideoURL,
                colorScheme: colorScheme,
                keyboard: .URL,
                autocapitalization: .never
            )

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
                    .foregroundColor(AppColors.error(for: colorScheme))
            }

            SkydownBrandActionButton(
                title: AppLocalized.text("videohub.upload.submit", fallback: "Videos hochladen"),
                systemImage: "arrow.up.circle.fill",
                accent: AppColors.accent(for: colorScheme),
                colorScheme: colorScheme,
                isEnabled: viewModel.canUpload,
                isLoading: viewModel.isUploading,
                font: .headline,
                cornerRadius: SkydownLayout.messageBubbleRadius,
                verticalPadding: 14,
                action: {
                    Task {
                        await viewModel.uploadSelectedVideos()
                    }
                }
            )

            SkydownBrandActionButton(
                title: AppLocalized.text("videohub.upload.share_external", fallback: "Externes Video teilen"),
                systemImage: "link.badge.plus",
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                role: .muted,
                isEnabled: viewModel.canAddExternalVideo,
                font: .headline,
                cornerRadius: SkydownLayout.denseRadius,
                verticalPadding: 14,
                action: {
                    Task {
                        await viewModel.addExternalVideo()
                    }
                }
            )
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
                fallback: "Der Clip bleibt gross, vertikal und im Fokus. Du kannst jederzeit in den Vollmodus wechseln."
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

                                    Text(AppLocalized.text("videohub.player.youtube_title", fallback: "YouTube-Clip"))
                                        .font(.title3.weight(.bold))
                                        .foregroundColor(.white)

                                    Text(AppLocalized.text(
                                        "videohub.player.youtube_subtitle",
                                        fallback: "Der Clip ist als YouTube-Video hinterlegt und spielt direkt in der App."
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
                    SkydownBrandActionButton(
                        title: AppLocalized.text("videohub.player.open_fullscreen", fallback: "Vollbild oeffnen"),
                        systemImage: "rectangle.portrait.and.arrow.right",
                        accent: AppColors.accentMystic(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .headline,
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 14,
                        action: { openVideoPlayer() }
                    )
                } else if let youTubeItem = selectedVideo.youTubeItem {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("videohub.player.open_youtube", fallback: "Auf YouTube ansehen"),
                        systemImage: "play.rectangle.fill",
                        accent: AppColors.youtube(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .headline,
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 14,
                        action: { presentSheet(.youTube(youTubeItem)) }
                    )
                } else if selectedVideo.opensOriginalInApp {
                    SkydownBrandActionButton(
                        title: selectedVideo.directOpenActionTitle,
                        systemImage: selectedVideo.supportsInlinePlayback
                            ? "rectangle.portrait.and.arrow.right"
                            : "arrow.up.forward.square",
                        accent: selectedVideo.supportsInlinePlayback
                            ? AppColors.accentMystic(for: colorScheme)
                            : AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .headline,
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 14,
                        action: { openOriginalVideo(selectedVideo) }
                    )
                }

                if !selectedVideo.notes.isEmpty {
                    Text(selectedVideo.notes)
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else {
                Text(AppLocalized.text("videohub.player.no_focus", fallback: "Noch kein Fokus-Video. Sobald ein Clip veroeffentlicht ist, erscheint er hier."))
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
                        Text(AppLocalized.text("videohub.library.owner_title", fallback: "Video-Steuerung"))
                            .font(.headline.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                        Text(AppLocalized.text("videohub.library.owner_subtitle", fallback: "Liste durchsuchen, Home-Video setzen, Clips bearbeiten oder loeschen."))
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }

                Spacer()

                if viewModel.isAdmin {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("videohub.library.new", fallback: "Neu"),
                        systemImage: "plus",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: {
                            editingVideoID = nil
                            showingAdminEditor = false
                            withAnimation(SkydownMotion.preferredScreenTransition(accessibilityReduceMotion: reduceMotion)) {
                                showingUploadComposer = true
                            }
                        }
                    )
                    .accessibilityLabel(AppLocalized.text("videohub.library.new_a11y", fallback: "Neues Video erstellen"))
                }
            }
            .accessibilityIdentifier("video.hub.library.header")

            if viewModel.isLoadingVideos {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    SkydownPremiumCircularProgress(
                        tint: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        scale: 0.72
                    )
                    Text(AppLocalized.text("videohub.library.loading", fallback: "Videos werden vorbereitet…"))
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else if viewModel.videos.isEmpty {
                Text(
                    viewModel.isAdmin
                    ? AppLocalized.text("videohub.library.empty_admin", fallback: "Noch keine Videos im Hub. Neue Uploads erscheinen hier automatisch.")
                    : AppLocalized.text("videohub.library.empty_guest", fallback: "Noch keine Videos veroeffentlicht. Schau spaeter nochmal vorbei.")
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
                            withAnimation(SkydownMotion.preferredScreenTransition(accessibilityReduceMotion: reduceMotion)) {
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
                                withAnimation(SkydownMotion.preferredScreenTransition(accessibilityReduceMotion: reduceMotion)) {
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
                                    withAnimation(SkydownMotion.preferredScreenTransition(accessibilityReduceMotion: reduceMotion)) {
                                        editingVideoID = nil
                                    }
                                }
                            }
                        )
                        .transition(
                            reduceMotion
                                ? .opacity
                                : .move(edge: .top).combined(with: .opacity)
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
            return AppLocalized.text("videohub.route.reel", fallback: "Inline-Reel")
        }
        if video.opensOriginalInApp {
            return AppLocalized.text("videohub.route.original_in_app", fallback: "Original in App")
        }
        return AppLocalized.text("videohub.route.external", fallback: "Externer Clip")
    }

    private var routeDetail: String {
        if isAdmin {
            if video.isPlayable {
                return isSelected
                    ? AppLocalized.text(
                        "videohub.route.detail.admin_playable_selected",
                        fallback: "Dieser Clip ist im Player und kann sofort gestartet oder gestoppt werden."
                    )
                    : AppLocalized.text(
                        "videohub.route.detail.admin_playable_unselected",
                        fallback: "Ein Tap laedt den Clip in den Player. Er bleibt als Fokus-Video sichtbar."
                    )
            }
            if video.supportsInlinePlayback {
                return AppLocalized.text("videohub.route.detail.admin_reel_flow", fallback: "Der Clip laeuft als In-App-Reel mit schnellem Preview-Flow.")
            }
            if video.opensOriginalInApp {
                return AppLocalized.text(
                    "videohub.route.detail.admin_original_flow",
                    fallback: "Das Original bleibt in der App erreichbar, inklusive Schliessen und sicherem Rueckweg."
                )
            }
            return AppLocalized.text("videohub.route.detail.admin_external_only", fallback: "Hier ist nur das externe Oeffnen verfuegbar.")
        }

        if video.supportsInlinePlayback {
            return AppLocalized.text("videohub.route.detail.user_reel", fallback: "Ein Tap startet die direkte Videoansicht ohne Zwischenschritt.")
        }
        if video.opensOriginalInApp {
            return AppLocalized.text(
                "videohub.route.detail.user_original",
                fallback: "Ein Tap oeffnet die In-App-Originalansicht mit sicherem Rueckweg zur App."
            )
        }
        return AppLocalized.text("videohub.route.detail.user_external", fallback: "Dieses Video oeffnet aktuell ueber einen externen Link.")
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

    private var primaryPlaybackEnabled: Bool {
        video.isPlayable || video.supportsInlinePlayback || video.opensOriginalInApp
    }

    private var primaryPlaybackAccent: Color {
        video.provider == .youTube ? AppColors.youtube(for: colorScheme) : AppColors.accent(for: colorScheme)
    }

    private var primaryPlaybackTitle: String {
        if video.isPlayable {
            return isPlaying
                ? AppLocalized.text("videohub.action.stop", fallback: "Stop")
                : AppLocalized.text("videohub.action.play", fallback: "Abspielen")
        }
        if video.supportsInlinePlayback {
            return AppLocalized.text("videohub.action.watch", fallback: "Ansehen")
        }
        return AppLocalized.text("videohub.action.open", fallback: "Oeffnen")
    }

    private var primaryPlaybackSystemImage: String {
        if video.isPlayable {
            return isPlaying ? "stop.fill" : "play.fill"
        }
        if video.supportsInlinePlayback {
            return "play.rectangle.fill"
        }
        return "arrow.up.forward.square"
    }

    private func invokePrimaryPlayback() {
        if video.isPlayable {
            onPlayToggle()
        } else if video.supportsInlinePlayback {
            onOpenReel()
        } else {
            onOpenOriginal()
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
                    ? AppLocalized.text("videohub.visibility.public", fallback: "Oeffentlich")
                    : AppLocalized.text("videohub.visibility.private", fallback: "Privat")
                line += " · \(visibilityLabel)"
                if video.isHomeFeatured { line += " · \(AppLocalized.text("videohub.meta.home", fallback: "Home"))" }
            }
            return line
        case .featured:
            var line = "\(video.projectName) · \(date)"
            if isAdmin {
                let visibilityLabel = video.isPublic
                    ? AppLocalized.text("videohub.visibility.public", fallback: "Oeffentlich")
                    : AppLocalized.text("videohub.visibility.private", fallback: "Privat")
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
                        SkydownBrandActionButton(
                            title: isSelected
                                ? AppLocalized.text("videohub.action.in_player", fallback: "Im Player")
                                : AppLocalized.text("videohub.action.select", fallback: "Auswaehlen"),
                            systemImage: "rectangle.on.rectangle",
                            accent: AppColors.accent(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: true,
                            action: onSelect
                        )
                        .frame(maxWidth: .infinity, alignment: .leading)
                        SkydownBrandActionButton(
                            title: primaryPlaybackTitle,
                            systemImage: primaryPlaybackSystemImage,
                            accent: primaryPlaybackAccent,
                            colorScheme: colorScheme,
                            isEnabled: primaryPlaybackEnabled,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: true,
                            action: invokePrimaryPlayback
                        )
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                } else {
                    if video.supportsInlinePlayback {
                        SkydownBrandActionButton(
                            title: AppLocalized.text("videohub.action.direct_video", fallback: "Inline abspielen"),
                            systemImage: "play.rectangle.fill",
                            accent: AppColors.accentMystic(for: colorScheme),
                            colorScheme: colorScheme,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 9,
                            action: onOpenReel
                        )
                    } else if video.opensOriginalInApp {
                        SkydownBrandActionButton(
                            title: video.directOpenActionTitle,
                            systemImage: "arrow.up.forward.square",
                            accent: AppColors.accent(for: colorScheme),
                            colorScheme: colorScheme,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 9,
                            action: onOpenOriginal
                        )
                    }
                }

                if isAdmin {
                    VStack(spacing: SkydownLayout.stackSpacingPill) {
                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            SkydownBrandActionButton(
                                title: video.isHomeFeatured
                                    ? AppLocalized.text("videohub.action.home_on", fallback: "Auf Home")
                                    : AppLocalized.text("videohub.action.show_home", fallback: "Auf Home zeigen"),
                                systemImage: "house.fill",
                                accent: AppColors.accentHighlight(for: colorScheme),
                                colorScheme: colorScheme,
                                role: .muted,
                                font: .caption.weight(.semibold),
                                cornerRadius: SkydownLayout.denseRadius,
                                verticalPadding: 8,
                                expandToFullWidth: true,
                                action: onToggleHomeFeatured
                            )
                            .frame(maxWidth: .infinity, alignment: .leading)
                            SkydownBrandActionButton(
                                title: AppLocalized.text("videohub.action.edit", fallback: "Bearbeiten"),
                                systemImage: "slider.horizontal.3",
                                accent: AppColors.accentMystic(for: colorScheme),
                                colorScheme: colorScheme,
                                role: .muted,
                                font: .caption.weight(.semibold),
                                cornerRadius: SkydownLayout.denseRadius,
                                verticalPadding: 8,
                                expandToFullWidth: true,
                                action: onEdit
                            )
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        SkydownBrandActionButton(
                            title: AppLocalized.text("videohub.action.delete", fallback: "Loeschen"),
                            systemImage: "trash",
                            accent: AppColors.error(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: true,
                            action: onDelete
                        )
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
                            ? AppLocalized.text("videohub.route.secondary_admin", fallback: "Deine Auswahl geht direkt in den Player.")
                            : AppLocalized.text("videohub.route.secondary_user", fallback: "Ein Tap setzt den Fokus; der Player folgt oben.")
                        ) : routeDetail
                )
                .font(.caption2)
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.92))
                .lineLimit(presentation == .secondary ? 1 : 2)
            }
            }

            if isAdmin {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    SkydownBrandActionButton(
                        title: isSelected
                            ? AppLocalized.text("videohub.action.in_player", fallback: "Im Player")
                            : AppLocalized.text("videohub.action.select", fallback: "Auswaehlen"),
                        systemImage: "rectangle.on.rectangle",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: true,
                        action: onSelect
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                    SkydownBrandActionButton(
                        title: primaryPlaybackTitle,
                        systemImage: primaryPlaybackSystemImage,
                        accent: primaryPlaybackAccent,
                        colorScheme: colorScheme,
                        isEnabled: primaryPlaybackEnabled,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: true,
                        action: invokePrimaryPlayback
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else {
                if video.supportsInlinePlayback {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("videohub.action.direct_video", fallback: "Inline abspielen"),
                        systemImage: "play.rectangle.fill",
                        accent: AppColors.accentMystic(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 9,
                        action: onOpenReel
                    )
                } else if video.opensOriginalInApp {
                    SkydownBrandActionButton(
                        title: video.directOpenActionTitle,
                        systemImage: "arrow.up.forward.square",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 9,
                        action: onOpenOriginal
                    )
                }
            }

            if isAdmin {
                VStack(spacing: SkydownLayout.stackSpacingPill) {
                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SkydownBrandActionButton(
                            title: video.isHomeFeatured
                                ? AppLocalized.text("videohub.action.home_on", fallback: "Auf Home")
                                : AppLocalized.text("videohub.action.show_home", fallback: "Auf Home zeigen"),
                            systemImage: "house.fill",
                            accent: AppColors.accentHighlight(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: true,
                            action: onToggleHomeFeatured
                        )
                        .frame(maxWidth: .infinity, alignment: .leading)
                        SkydownBrandActionButton(
                            title: AppLocalized.text("videohub.action.edit", fallback: "Bearbeiten"),
                            systemImage: "slider.horizontal.3",
                            accent: AppColors.accentMystic(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: true,
                            action: onEdit
                        )
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    SkydownBrandActionButton(
                        title: AppLocalized.text("videohub.action.delete", fallback: "Loeschen"),
                        systemImage: "trash",
                        accent: AppColors.error(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: true,
                        action: onDelete
                    )
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
                    Text(AppLocalized.text("videohub.edit.title", fallback: "Video bearbeiten"))
                        .font(.headline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("\(sourceLabel) · \(skydownVideoDateFormatter.string(from: video.createdAt))")
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(1)
                }
                Spacer()
                SkydownBrandActionButton(
                    title: "",
                    systemImage: "xmark",
                    accent: AppColors.accentMystic(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    font: .caption.weight(.bold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 6,
                    expandToFullWidth: false,
                    action: onCancel
                )
                .accessibilityLabel(AppLocalized.text("videohub.edit.close_a11y", fallback: "Editor schliessen"))
            }

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.title", fallback: "Titel"),
                text: $title,
                colorScheme: colorScheme
            )

            NicmaUploadField(
                title: AppLocalized.text("videohub.field.project_artist", fallback: "Projekt / Artist"),
                text: $projectName,
                colorScheme: colorScheme
            )

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                Text(AppLocalized.text("videohub.field.notes", fallback: "Notizen"))
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
                        ? AppLocalized.text("videohub.visibility.public_visible", fallback: "Oeffentlich (sichtbar)")
                        : AppLocalized.text("videohub.visibility.private_hidden", fallback: "Privat (ausgeblendet)"),
                    systemImage: isPublic ? "checkmark.circle.fill" : "eye.slash.fill"
                )
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
            }
            .toggleStyle(SkydownPremiumToggleStyle(colorScheme: colorScheme))

            HStack(spacing: SkydownLayout.stackSpacingPill) {
                SkydownBrandActionButton(
                    title: AppLocalized.text("common.cancel", fallback: "Abbrechen"),
                    systemImage: "xmark",
                    accent: AppColors.accentMystic(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 11,
                    expandToFullWidth: true,
                    action: onCancel
                )
                .frame(maxWidth: .infinity, alignment: .leading)

                SkydownBrandActionButton(
                    title: AppLocalized.text("common.save", fallback: "Speichern"),
                    systemImage: "checkmark.circle.fill",
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 11,
                    expandToFullWidth: true,
                    action: { onSave(title, projectName, notes, isPublic) }
                )
                .frame(maxWidth: .infinity, alignment: .leading)
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
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var currentIndex: Int
    @State private var player = AVPlayer()
    @State private var isTransitioning = false
    @State private var isPlaying = false

    private var reelPreparingOverlayShowAnimation: Animation {
        SkydownMotion.preferredEmphasizedTransition(accessibilityReduceMotion: reduceMotion)
    }

    private var reelPreparingOverlayHideAnimation: Animation {
        SkydownMotion.preferredStatusTransition(accessibilityReduceMotion: reduceMotion)
    }

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
                                format: AppLocalized.text("videohub.reel.position_format", fallback: "%1$d von %2$d"),
                                currentIndex + 1,
                                videos.count
                            )
                        )
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.70))

                        if videos.count > 1 {
                            Text(AppLocalized.text("videohub.reel.swipe_hint", fallback: "Vertikal durch alle Clips wischen."))
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
                        SkydownPremiumCircularProgress(
                            tint: .white,
                            colorScheme: .dark,
                            scale: 0.78
                        )
                        Text(AppLocalized.text("videohub.reel.preparing", fallback: "Clip wird vorbereitet…"))
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.86))
                    }
                    .padding(.horizontal, SkydownLayout.cardPadding)
                    .padding(.vertical, 12)
                    .background(
                        LinearGradient(
                            colors: [
                                Color.black.opacity(0.58),
                                Color.white.opacity(0.10)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        in: RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                            .stroke(Color.white.opacity(0.18), lineWidth: 1)
                    )
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                    .transition(.opacity)
                    .zIndex(12)
                }
            }
        }
        .onAppear {
            withAnimation(reelPreparingOverlayShowAnimation) {
                isTransitioning = true
            }
            playCurrent()
            let outDelay = reduceMotion ? 0.0 : 0.28
            DispatchQueue.main.asyncAfter(deadline: .now() + outDelay) {
                withAnimation(reelPreparingOverlayHideAnimation) {
                    isTransitioning = false
                }
            }
        }
        .onChange(of: currentIndex) { _, _ in
            withAnimation(reelPreparingOverlayShowAnimation) {
                isTransitioning = true
            }
            playCurrent()
            let outDelay = reduceMotion ? 0.0 : 0.24
            DispatchQueue.main.asyncAfter(deadline: .now() + outDelay) {
                withAnimation(reelPreparingOverlayHideAnimation) {
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
            Text(AppLocalized.text("videohub.equipment.section_title", fallback: "Equipment & Software"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.equipment.section_subtitle", fallback: "Visueller Stack fuer Aufnahme, Edit und Finish."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if items.isEmpty {
                Text(AppLocalized.text("videohub.equipment.empty", fallback: "Equipment wird vorbereitet und erscheint hier in Kuerze."))
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
            SkydownPremiumIconAction(
                systemImage: isUploadOpen ? "xmark.circle.fill" : "arrow.up.circle.fill",
                tint: AppColors.accent(for: colorScheme),
                colorScheme: colorScheme,
                isSelected: isUploadOpen,
                size: 54,
                iconSize: 22,
                accessibilityLabel: isUploadOpen
                    ? AppLocalized.text("common.close", fallback: "Schliessen")
                    : AppLocalized.text("videohub.dock.upload", fallback: "Upload"),
                action: onOpenUpload
            )
        }
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
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.done", fallback: "Fertig"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: { dismiss() }
                    )
                    .skydownInteractiveFeedback()
                }
            }
        }
    }
}

struct ProducedWithArtistRow: View {
    let artist: SkydownProducedWithArtist
    let colorScheme: ColorScheme
    let onOpenYouTube: (String) -> Void

    private var roleLabel: String {
        let trimmedRole = artist.role.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedRole.isEmpty {
            return AppLocalized.text("videohub.label.collab_default", fallback: "Kollaboration")
        }
        return trimmedRole
    }

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

    private var hasAnySocialLink: Bool {
        spotifyURL != nil || instagramURL != nil || youtubeURL != nil
    }

    var body: some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
            previewArtwork
                .frame(width: 56, height: 56)
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                Text(
                    artist.vibe.isEmpty
                        ? roleLabel
                        : "\(roleLabel) · \(artist.vibe)"
                )
                .font(.caption2.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.92))
                .lineLimit(1)

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
                } else {
                    Text(AppLocalized.text("videohub.collab.highlight.pending", fallback: "Profil wird erweitert."))
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.82))
                        .lineLimit(1)
                }

                ScrollView(.horizontal, showsIndicators: false) {
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

                        if !hasAnySocialLink {
                            Text(AppLocalized.text("videohub.social.pending", fallback: "Social-Links folgen"))
                                .font(.caption2.weight(.semibold))
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                .padding(.horizontal, 9)
                                .padding(.vertical, 6)
                                .background(AppColors.secondaryBackground(for: colorScheme))
                                .clipShape(Capsule())
                        }
                    }
                    .padding(.trailing, 2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.97 : 0.99),
                    AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.58 : 0.54)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.09), lineWidth: 0.8)
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
                    AppColors.accentMystic(for: colorScheme).opacity(0.82),
                    AppColors.accent(for: colorScheme).opacity(0.70)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            Text(String(artist.name.prefix(1)).uppercased())
                .font(.title2.weight(.bold))
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
        .accessibilityHint(AppLocalized.text("videohub.social.open_hint", fallback: "Oeffnet das Profil in der App oder im Browser."))
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private var buttonBody: some View {
        HStack(spacing: 5) {
            Image(systemName: systemImage)
                .font(.system(size: 10, weight: .bold))
            Text(accessibilityTitle)
                .font(.caption2.weight(.semibold))
                .lineLimit(1)
        }
        .foregroundColor(foregroundColor)
        .padding(.horizontal, 9)
        .padding(.vertical, 6)
        .frame(minHeight: 28)
        .background(background)
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.10), lineWidth: 1)
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
        .shadow(color: .black.opacity(0.10), radius: 6, y: 3)
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

            SkydownPremiumLinkSurface(
                title: AppLocalized.text("videohub.youtube_row.play", fallback: "YouTube"),
                systemImage: "play.rectangle.fill",
                tint: AppColors.youtube(for: colorScheme),
                colorScheme: colorScheme,
                isExpanded: false,
                action: onPlay
            )
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
            Text(AppLocalized.text("videohub.editor.title", fallback: "Videography-Editor"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("videohub.editor.intro", fallback: "Owner und Video-Admins verwalten hier Equipment und empfohlene Kollaborationen. Bilder sind picker-first und werden mit Live-Vorschau angewendet."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(AppLocalized.text("videohub.editor.save_hint", fallback: "Du kannst Eintraege hinzufuegen, ersetzen oder entfernen. Oeffentliche Daten gehen erst nach dem Speichern live."))
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                Text(AppLocalized.text("videohub.editor.equipment_heading", fallback: "Equipment"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                ForEach(Array(viewModel.publicConfig.equipmentItems.enumerated()), id: \.element.id) { _, item in
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.title", fallback: "Titel"),
                            text: Binding(
                                get: { item.title },
                                set: { viewModel.updateEquipmentItem(item.id, title: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        NicmaUploadField(
                            title: AppLocalized.text("videohub.field.detail", fallback: "Details"),
                            text: Binding(
                                get: { item.detail },
                                set: { viewModel.updateEquipmentItem(item.id, detail: $0) }
                            ),
                            colorScheme: colorScheme
                        )
                        EditableImageField(
                            title: AppLocalized.text("videohub.editor.equipment_image_title", fallback: "Equipment-Bild"),
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
                        SkydownBrandActionButton(
                            title: AppLocalized.text("videohub.editor.remove_entry", fallback: "Remove entry"),
                            systemImage: "trash",
                            accent: AppColors.error(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: true,
                            action: { viewModel.removeEquipmentItem(item.id) }
                        )
                    }
                    .padding(14)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                }

                SkydownBrandActionButton(
                    title: AppLocalized.text("videohub.editor.add_equipment", fallback: "Add equipment"),
                    systemImage: "plus.circle.fill",
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 10,
                    action: { viewModel.addEquipmentItem() }
                )
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                Text(AppLocalized.text("videohub.editor.collabs_heading", fallback: "Empfohlene Kollaborationen"))
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
                            title: AppLocalized.text("videohub.editor.collab_image_title", fallback: "Kollaborationsbild"),
                            imageURL: Binding(
                                get: { item.imageURLString ?? "" },
                                set: { viewModel.updateCollaborationItem(item.id, imageURLString: $0) }
                            ),
                            colorScheme: colorScheme,
                            isUploading: activeUploadTarget == .collaboration(item.id),
                            uploadStatusText: AppLocalized.text("videohub.editor.collab_upload_status", fallback: "Kollaborationsbild wird angewendet…"),
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
                        SkydownBrandActionButton(
                            title: AppLocalized.text("videohub.editor.remove_collab", fallback: "Kollaboration entfernen"),
                            systemImage: "trash",
                            accent: AppColors.error(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: true,
                            action: { viewModel.removeCollaborationItem(item.id) }
                        )
                    }
                    .padding(14)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                }

                SkydownBrandActionButton(
                    title: AppLocalized.text("videohub.editor.add_collab", fallback: "Kollaboration hinzufuegen"),
                    systemImage: "plus.circle.fill",
                    accent: AppColors.accentMystic(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 10,
                    action: { viewModel.addCollaborationItem() }
                )
            }

            SkydownBrandActionButton(
                title: AppLocalized.text("videohub.editor.save_public", fallback: "Oeffentliche Daten speichern"),
                systemImage: "square.and.arrow.down.fill",
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                isEnabled: !viewModel.isSavingPublicConfig,
                isLoading: viewModel.isSavingPublicConfig,
                font: .headline,
                cornerRadius: SkydownLayout.denseRadius,
                verticalPadding: 14,
                action: {
                    Task {
                        await viewModel.savePublicConfig()
                    }
                }
            )
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
            SkydownPremiumIconSurface(
                systemImage: "video.fill",
                tint: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                size: SkydownLayout.iconActionSurfaceSize,
                iconSize: 15
            )

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

            SkydownPremiumIconAction(
                systemImage: "xmark",
                tint: AppColors.error(for: colorScheme),
                colorScheme: colorScheme,
                size: SkydownLayout.iconActionCompactSurfaceSize,
                iconSize: 12,
                accessibilityLabel: AppLocalized.text("common.remove", fallback: "Remove"),
                action: onRemove
            )
        }
        .padding(12)
        .background(
            LinearGradient(
                colors: [
                    AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.78 : 0.62),
                    AppColors.accentMystic(for: colorScheme).opacity(colorScheme == .dark ? 0.12 : 0.08)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
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
    @Environment(\.colorScheme) private var colorScheme
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
                                    title: AppLocalized.text("videohub.viewer.external", fallback: "Extern oeffnen"),
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
                                        title: AppLocalized.text("videohub.viewer.external", fallback: "Extern oeffnen"),
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
                    .skydownNavigationChrome(colorScheme: colorScheme)
                    .toolbar {
                        ToolbarItem(placement: .topBarLeading) {
                            SkydownBrandActionButton(
                                title: AppLocalized.text("common.close", fallback: "Close"),
                                systemImage: "xmark",
                                accent: AppColors.accent(for: colorScheme),
                                colorScheme: colorScheme,
                                role: .muted,
                                font: .subheadline.weight(.semibold),
                                cornerRadius: SkydownLayout.denseRadius,
                                verticalPadding: 8,
                                expandToFullWidth: false,
                                action: { dismiss() }
                            )
                            .skydownInteractiveFeedback()
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
        SkydownPremiumIconAction(
            systemImage: "xmark",
            tint: .white,
            colorScheme: .dark,
            size: 52,
            iconSize: 18,
            accessibilityLabel: AppLocalized.text("videohub.a11y.close_video", fallback: "Close video"),
            action: action
        )
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
                    : AppLocalized.text("videohub.a11y.play_video", fallback: "Video abspielen"),
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
        SkydownPremiumIconAction(
            systemImage: systemImage,
            tint: .white,
            colorScheme: .dark,
            isSelected: isProminent,
            isEnabled: isEnabled,
            size: isProminent ? 40 : 36,
            iconSize: isProminent ? 15 : 14,
            accessibilityLabel: accessibilityLabel,
            action: action
        )
    }
}

private struct SkydownViewerToolbarButton: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let systemImage: String
    let isPrimary: Bool
    var isEnabled = true
    let action: () -> Void

    var body: some View {
        SkydownBrandActionButton(
            title: title,
            systemImage: systemImage,
            accent: isPrimary ? AppColors.accentHighlight(for: colorScheme) : .white,
            colorScheme: .dark,
            role: isPrimary ? .primary : .muted,
            isEnabled: isEnabled,
            font: .footnote.weight(.semibold),
            cornerRadius: SkydownLayout.denseRadius,
            verticalPadding: 12,
            expandToFullWidth: true,
            action: action
        )
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
                            title: AppLocalized.text("videohub.viewer.external", fallback: "Extern oeffnen"),
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
                            title: AppLocalized.text("videohub.viewer.external", fallback: "Extern oeffnen"),
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
