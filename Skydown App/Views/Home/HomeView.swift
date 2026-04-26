import AVKit
import SwiftUI

/// Dedicated Home entry surface for clearer ownership and faster iteration.
struct HomeView: View {
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onGuestSignIn: (() -> Void)?
    let onOpenWorkflow: (() -> Void)?

    init(
        onOpenCart: @escaping () -> Void = {},
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        onGuestSignIn: (() -> Void)? = nil,
        onOpenWorkflow: (() -> Void)? = nil
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onGuestSignIn = onGuestSignIn
        self.onOpenWorkflow = onOpenWorkflow
    }

    var body: some View {
        HomeViewContent(
            onOpenCart: onOpenCart,
            onOpenProfile: onOpenProfile,
            onOpenSettings: onOpenSettings,
            onGuestSignIn: onGuestSignIn,
            onOpenWorkflow: onOpenWorkflow
        )
    }
}

private enum HomeSectionAnchor: String {
    case release
    case video
}

struct HomeViewContent: View {
    @StateObject private var viewModel = HomeViewModel()
    @StateObject private var audioPlayerManager = AudioPlayerManager()
    @StateObject private var videoPlaybackManager = HomeInlineVideoPlaybackManager()
    @State private var sheetPresentation = SkydownQueuedPresentation<HomePresentedSheet>()
    @State private var fullscreenVideoTarget: HomeFullscreenVideoTarget?
    @State private var originalVideoViewerTarget: HomeOriginalVideoViewerTarget?
    @State private var hasLoadedInitialHomeContent = false
    @Environment(\.colorScheme) private var colorScheme
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onGuestSignIn: (() -> Void)?
    let onOpenWorkflow: (() -> Void)?

    init(
        onOpenCart: @escaping () -> Void = {},
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        onGuestSignIn: (() -> Void)? = nil,
        onOpenWorkflow: (() -> Void)? = nil
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onGuestSignIn = onGuestSignIn
        self.onOpenWorkflow = onOpenWorkflow
    }

    var body: some View {
        NavigationStack {
            GeometryReader { geometry in
                let layout = SkydownResponsiveLayout(availableWidth: geometry.size.width)
                let contentWidth = min(
                    layout.contentMaxWidth,
                    max(geometry.size.width - (layout.horizontalPadding * 2), 0)
                )

                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                            let scrollAnimation = SkydownMotion.smoothScroll
                            let openReleaseSection = {
                                withAnimation(scrollAnimation) {
                                    proxy.scrollTo(HomeSectionAnchor.release.rawValue, anchor: .top)
                                }
                            }
                            let openVideoSection = {
                                withAnimation(scrollAnimation) {
                                    proxy.scrollTo(HomeSectionAnchor.video.rawValue, anchor: .top)
                                }
                            }
                            VStack(alignment: .leading, spacing: 6) {
                                Spacer()
                                    .frame(height: 4)
                                HomeHeroIntroCard(
                                    viewModel: viewModel,
                                    colorScheme: colorScheme,
                                    onOpenProfile: onOpenProfile,
                                    onOpenTrack: openReleaseSection,
                                    onOpenVideo: openVideoSection
                                )
                                .padding(.vertical, 4)
                                .homeReveal(0)

                                HomeUtilityRow(
                                    colorScheme: colorScheme,
                                    onOpenMusic: openReleaseSection,
                                    onOpenVideo: openVideoSection,
                                    onOpenMerch: onOpenCart,
                                    onOpenSettings: onOpenSettings
                                )
                                .homeReveal(2)
                            }
                            .background {
                                LinearGradient(
                                    colors: [
                                        AppColors.accent(for: colorScheme).opacity(0.026),
                                        AppColors.accentMystic(for: colorScheme).opacity(0.016),
                                        AppColors.accentHighlight(for: colorScheme).opacity(0.012),
                                        AppColors.primaryBackground(for: colorScheme)
                                    ],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            }

                            HomeMediaClusterSection(
                                colorScheme: colorScheme,
                                viewModel: viewModel,
                                playbackManager: audioPlayerManager,
                                videoPlaybackManager: videoPlaybackManager,
                                onOpenVideoHub: openVideoHubFromMediaCluster(video:),
                                onOpenOriginal: openOriginalFromMediaCluster(video:)
                            )
                            .homeReveal(3)

                            Text(
                                AppLocalized.text(
                                    "home.hero.tagline",
                                    fallback: "Open and play."
                                )
                            )
                                .font(.footnote)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                .padding(.horizontal, 4)
                                .padding(.vertical, 6)
                                .homeReveal(4)
                        }
                        .frame(maxWidth: contentWidth, alignment: .leading)
                        .padding(.horizontal, layout.horizontalPadding)
                        .padding(.top, SkydownLayout.screenTopPadding * 0.5)
                        .padding(.bottom, SkydownLayout.screenBottomPadding)
                        .frame(maxWidth: .infinity)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .scrollIndicators(.hidden)
            .scrollBounceBehavior(.basedOnSize, axes: .vertical)
            .refreshable {
                viewModel.refresh()
                SkydownHaptics.notification(.success)
            }
            .background {
                AppColors.screenGradient(
                    for: colorScheme,
                    secondaryAccent: AppColors.accentMystic(for: colorScheme)
                )
                .overlay {
                    SkydownAtmosphereBackdrop(colorScheme: colorScheme)
                }
                .overlay { HomeMapBackdrop(colorScheme: colorScheme) }
                .ignoresSafeArea()
            }
            .navigationTitle("SkyOS")
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
                                .background(Circle().fill(AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.18 : 0.20)))
                                .overlay(Circle().stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.22), lineWidth: 1))
                        }
                        .skydownTactileAction()
                        .accessibilityLabel(AppLocalized.text("home.toolbar.workflow", fallback: "Open automations"))
                    }
                    AppSessionToolbarActions(
                        onOpenCart: onOpenCart,
                        onOpenProfile: onOpenProfile,
                        onOpenSettings: onOpenSettings,
                        onGuestSignIn: onGuestSignIn
                    )
                }
            }
            .task {
                guard !hasLoadedInitialHomeContent else { return }
                hasLoadedInitialHomeContent = true
                viewModel.refresh()
            }
            .onDisappear {
                audioPlayerManager.stop()
                videoPlaybackManager.stop()
            }
        }
        .sheet(item: activePresentedSheetBinding) { sheet in
            NavigationStack {
                switch sheet {
                case .nicmaProducer: NicmaProducerView { activePresentedSheetBinding.wrappedValue = nil }
                }
            }
        }
        .fullScreenCover(item: $originalVideoViewerTarget) { target in
            SkydownOriginalVideoDestinationView(urlString: target.urlString, title: target.title)
        }
        .fullScreenCover(item: $fullscreenVideoTarget) { target in
            HomeFullscreenVideoViewer(video: target.video)
        }
    }

    private var activePresentedSheetBinding: Binding<HomePresentedSheet?> {
        Binding(
            get: { sheetPresentation.activeItem },
            set: { sheetPresentation.updatePresentedItem($0) }
        )
    }

    private func presentSheet(_ sheet: HomePresentedSheet) {
        sheetPresentation.request(sheet)
    }

    private func openOriginalVideo(_ video: FeaturedHomeVideo) {
        let urlString = {
            let primary = video.openURLString.trimmingCharacters(in: .whitespacesAndNewlines)
            if !primary.isEmpty { return primary }
            return video.embedURL.trimmingCharacters(in: .whitespacesAndNewlines)
        }()
        guard !urlString.isEmpty, let url = URL(string: urlString) else { return }
        audioPlayerManager.stop()
        videoPlaybackManager.stop()
        activePresentedSheetBinding.wrappedValue = nil
        originalVideoViewerTarget = HomeOriginalVideoViewerTarget(
            urlString: url.absoluteString,
            title: video.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Original" : video.title
        )
    }

    private func stopAllMediaPlayback() {
        audioPlayerManager.stop()
        videoPlaybackManager.stop()
    }

    private func openVideoHubFromMediaCluster(video: FeaturedHomeVideo) {
        stopAllMediaPlayback()
        activePresentedSheetBinding.wrappedValue = nil
        fullscreenVideoTarget = HomeFullscreenVideoTarget(video: video)
    }

    private func openOriginalFromMediaCluster(video: FeaturedHomeVideo) {
        if video.supportsInlinePlayback {
            openVideoHubFromMediaCluster(video: video)
            return
        }
        openOriginalVideo(video)
    }
}

@MainActor
private func homeTrackedSignalCount(_ viewModel: HomeViewModel) -> Int {
    [viewModel.featuredTrack != nil, viewModel.featuredVideo != nil]
        .filter { $0 }
        .count
}

@MainActor
private func homeCommandPriorityTarget(_ viewModel: HomeViewModel) -> String {
    if viewModel.featuredTrack == nil { return "music" }
    if viewModel.featuredVideo == nil { return "visuals" }
    let hour = Calendar.current.component(.hour, from: Date())
    return hour < 12 ? "music" : "visuals"
}

private enum HomePresentedSheet: String, Identifiable, Equatable {
    case nicmaProducer
    var id: String { rawValue }
}

private struct HomeOriginalVideoViewerTarget: Identifiable {
    let id = UUID()
    let urlString: String
    let title: String
}

private struct HomeFullscreenVideoTarget: Identifiable {
    let video: FeaturedHomeVideo
    var id: String { video.id }
}

private struct HomeFullscreenVideoViewer: View {
    let video: FeaturedHomeVideo
    @Environment(\.dismiss) private var dismiss
    @State private var player = AVPlayer()
    @State private var isPlaying = false

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color.black.ignoresSafeArea()

                if video.usesEmbeddedPreview {
                    ExternalVideoEmbedSurface(urlString: video.embedURL)
                        .ignoresSafeArea()
                } else if let url = URL(string: video.downloadURL), !video.downloadURL.isEmpty {
                    VideoPlayer(player: player)
                        .ignoresSafeArea()
                        .onAppear {
                            player.replaceCurrentItem(with: AVPlayerItem(url: url))
                            player.play()
                            isPlaying = true
                        }
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "play.rectangle.fill")
                            .font(.system(size: 48, weight: .bold))
                            .foregroundColor(.white.opacity(0.72))
                        Text(video.title)
                            .font(.headline.weight(.bold))
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)
                    }
                    .padding(24)
                }

                if supportsAppPlaybackControls {
                    HStack {
                        Spacer()

                        SkydownVideoFullscreenControlBar(
                            isPlaying: isPlaying,
                            showsClipNavigation: false,
                            canGoToPreviousClip: false,
                            canGoToNextClip: false,
                            onPreviousClip: {},
                            onRewind: { seekCurrentVideo(by: -10) },
                            onPlayPause: togglePlayback,
                            onForward: { seekCurrentVideo(by: 10) },
                            onNextClip: {},
                            onClose: { dismiss() }
                        )
                    }
                    .padding(.trailing, 18)
                    .padding(.bottom, max(proxy.safeAreaInsets.bottom + 82, 94))
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                    .zIndex(14)
                }

                VStack {
                    HStack {
                        Spacer()

                        SkydownVideoFullscreenCloseButton {
                            dismiss()
                        }
                    }
                    .padding(.top, max(proxy.safeAreaInsets.top + 12, 22))
                    .padding(.trailing, 18)

                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                .zIndex(1_100)
            }
            .ignoresSafeArea()
        }
        .onDisappear {
            player.pause()
            player.replaceCurrentItem(with: nil)
            isPlaying = false
        }
    }

    private var supportsAppPlaybackControls: Bool {
        !video.usesEmbeddedPreview && !video.downloadURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func togglePlayback() {
        guard supportsAppPlaybackControls else { return }
        if isPlaying {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }

    private func seekCurrentVideo(by seconds: Double) {
        guard supportsAppPlaybackControls else { return }
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
}

private struct HomeMediaClusterSection: View {
    let colorScheme: ColorScheme
    let viewModel: HomeViewModel
    let playbackManager: AudioPlayerManager
    let videoPlaybackManager: HomeInlineVideoPlaybackManager
    let onOpenVideoHub: (FeaturedHomeVideo) -> Void
    let onOpenOriginal: (FeaturedHomeVideo) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(
                AppLocalized.text("home.section.current", fallback: "Current")
            )
                .font(.caption2.weight(.medium))
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.5))
            HomeMediaCluster(
                colorScheme: colorScheme,
                viewModel: viewModel,
                playbackManager: playbackManager,
                videoPlaybackManager: videoPlaybackManager,
                onOpenVideoHub: onOpenVideoHub,
                onOpenOriginal: onOpenOriginal
            )
        }
    }
}

private struct HomeLiveSignalSection: View {
    let colorScheme: ColorScheme
    let hasTrackSignal: Bool
    let hasVideoSignal: Bool
    let trackName: String?
    let videoName: String?
    let aiUsageWarning: String?
    let creatorLimitZone: Bool
    let agentRunning: Bool
    let workflowWaiting: Bool
    let commerceHint: String?
    let syncPaused: Bool
    let recoverableError: String?
    let contentSignal: String?

    var body: some View {
        HomeLiveSignalSurface(
            colorScheme: colorScheme,
            hasTrackSignal: hasTrackSignal,
            hasVideoSignal: hasVideoSignal,
            trackName: trackName,
            videoName: videoName,
            aiUsageWarning: aiUsageWarning,
            agentRunning: agentRunning,
            commerceHint: commerceHint,
            syncPaused: syncPaused,
            creatorLimitZone: creatorLimitZone,
            workflowWaiting: workflowWaiting,
            recoverableError: recoverableError,
            contentSignal: contentSignal
        )
    }
}

private struct HomeVideoHubLaunchTarget: Identifiable {
    let id = UUID()
    let videoID: String
}
