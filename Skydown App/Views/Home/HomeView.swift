import SwiftUI

/// Dedicated Home entry surface for clearer ownership and faster iteration.
struct HomeView: View {
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onOpenWorkflow: (() -> Void)?

    init(
        onOpenCart: @escaping () -> Void = {},
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        onOpenWorkflow: (() -> Void)? = nil
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onOpenWorkflow = onOpenWorkflow
    }

    var body: some View {
        HomeViewContent(
            onOpenCart: onOpenCart,
            onOpenProfile: onOpenProfile,
            onOpenSettings: onOpenSettings,
            onOpenWorkflow: onOpenWorkflow
        )
    }
}

private enum HomeSectionAnchor: String {
    case release
    case beat
    case video
}

struct HomeViewContent: View {
    @StateObject private var viewModel = HomeViewModel()
    @StateObject private var beatPlaybackManager = BeatPlaybackManager()
    @StateObject private var audioPlayerManager = AudioPlayerManager()
    @StateObject private var videoPlaybackManager = HomeInlineVideoPlaybackManager()
    @State private var activePresentedSheet: HomePresentedSheet?
    @State private var queuedPresentedSheet: HomePresentedSheet?
    @State private var videoHubLaunchTarget: HomeVideoHubLaunchTarget?
    @State private var originalVideoViewerTarget: HomeOriginalVideoViewerTarget?
    @State private var hasLoadedInitialHomeContent = false
    @Environment(\.colorScheme) private var colorScheme
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onOpenWorkflow: (() -> Void)?

    init(
        onOpenCart: @escaping () -> Void = {},
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        onOpenWorkflow: (() -> Void)? = nil
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onOpenWorkflow = onOpenWorkflow
    }

    var body: some View {
        NavigationStack {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                        let scrollAnimation = Animation.spring(response: 0.36, dampingFraction: 0.86)
                        let openReleaseSection = {
                            withAnimation(scrollAnimation) {
                                proxy.scrollTo(HomeSectionAnchor.release.rawValue, anchor: .top)
                            }
                        }
                        let openBeatSection = {
                            withAnimation(scrollAnimation) {
                                proxy.scrollTo(HomeSectionAnchor.beat.rawValue, anchor: .top)
                            }
                        }
                        let openVideoSection = {
                            withAnimation(scrollAnimation) {
                                proxy.scrollTo(HomeSectionAnchor.video.rawValue, anchor: .top)
                            }
                        }
                        let featuredTrack = viewModel.featuredTrack
                        let featuredBeat = viewModel.featuredBeat
                        let featuredVideo = viewModel.featuredVideo
                        let hasTrackSignal = featuredTrack != nil
                        let hasBeatSignal = featuredBeat != nil
                        let hasVideoSignal = featuredVideo != nil

                        HomeHeroIntroCard(
                            viewModel: viewModel,
                            colorScheme: colorScheme,
                            onOpenTrack: openReleaseSection,
                            onOpenBeat: openBeatSection,
                            onOpenVideo: openVideoSection
                        )
                        .homeReveal(0)

                        HomeDailyOpsStrip(
                            colorScheme: colorScheme,
                            activeSignalCount: homeTrackedSignalCount(viewModel),
                            totalSignalCount: 3,
                            hasTrackSignal: hasTrackSignal,
                            hasBeatSignal: hasBeatSignal,
                            hasVideoSignal: hasVideoSignal,
                            onRefresh: { viewModel.refresh() },
                            onOpenRelease: openReleaseSection,
                            onOpenBeat: openBeatSection,
                            onOpenVideo: openVideoSection
                        )
                        .homeReveal(1)

                        let commandPriorityTarget = homeCommandPriorityTarget(viewModel)

                        HomeCommandDockStrip(
                            colorScheme: colorScheme,
                            priorityTarget: commandPriorityTarget,
                            onOpenWorkflow: onOpenWorkflow,
                            onOpenCart: onOpenCart,
                            onOpenSettings: onOpenSettings
                        )
                        .homeReveal(2)

                        HomeUtilityRow(
                            colorScheme: colorScheme,
                            onOpenAI: { (onOpenWorkflow ?? onOpenSettings)() },
                            onOpenMusic: openReleaseSection,
                            onOpenCreate: { presentSheet(.nicmaProducer) },
                            onOpenOrders: onOpenCart,
                            onOpenSearch: openReleaseSection,
                            onOpenSettings: onOpenSettings
                        )
                        .homeReveal(3)

                        HomeLiveSignalSection(
                            colorScheme: colorScheme,
                            hasTrackSignal: hasTrackSignal,
                            hasBeatSignal: hasBeatSignal,
                            hasVideoSignal: hasVideoSignal,
                            trackName: featuredTrack?.trackName,
                            beatName: featuredBeat?.title,
                            videoName: featuredVideo?.title,
                            aiUsageWarning: viewModel.aiUsageWarning,
                            creatorLimitZone: viewModel.creatorLimitZone,
                            agentRunning: viewModel.agentRunning,
                            workflowWaiting: viewModel.workflowWaiting,
                            commerceHint: viewModel.commerceSignal,
                            syncPaused: viewModel.syncPaused,
                            recoverableError: viewModel.recoverableError,
                            contentSignal: viewModel.contentSignal
                        )
                        .homeReveal(4)

                        HomeMediaClusterSection(
                            colorScheme: colorScheme,
                            viewModel: viewModel,
                            playbackManager: audioPlayerManager,
                            beatPlaybackManager: beatPlaybackManager,
                            videoPlaybackManager: videoPlaybackManager,
                            onOpenVideoHub: openVideoHubFromMediaCluster(video:),
                            onOpenOriginal: openOriginalFromMediaCluster(video:)
                        )
                        .homeReveal(5)

                        Text("SkyOS Home fuehrt ruhig: Signal lesen, Fokus setzen, naechsten Schritt ausfuehren.")
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .padding(.horizontal, 4)
                            .padding(.vertical, 6)
                            .homeReveal(8)
                    }
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.top, SkydownLayout.screenTopPadding * 0.5)
                    .padding(.bottom, SkydownLayout.screenBottomPadding)
                }
            }
            .scrollIndicators(.hidden)
            .refreshable {
                viewModel.refresh()
                SkydownHaptics.notification(.success)
            }
            .background {
                AppColors.screenGradient(
                    for: colorScheme,
                    secondaryAccent: AppColors.accentMystic(for: colorScheme)
                )
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
                        .accessibilityLabel("Automationen oeffnen")
                    }
                    AppSessionToolbarActions(
                        onOpenCart: onOpenCart,
                        onOpenProfile: onOpenProfile,
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
        .sheet(item: $activePresentedSheet) { sheet in
            NavigationStack {
                switch sheet {
                case .beatHub: BeatHubView { activePresentedSheet = nil }
                case .nicmaProducer: NicmaProducerView { activePresentedSheet = nil }
                }
            }
        }
        .fullScreenCover(item: $originalVideoViewerTarget) { target in
            SkydownOriginalVideoDestinationView(urlString: target.urlString, title: target.title)
        }
        .fullScreenCover(item: $videoHubLaunchTarget) { target in
            NavigationStack {
                VideoHubView(
                    onBack: { videoHubLaunchTarget = nil },
                    initialSelectedVideoID: target.videoID,
                    autoplayInitialSelection: true
                )
            }
        }
        .onChange(of: activePresentedSheet) { _, sheet in
            guard sheet == nil, let queuedPresentedSheet else { return }
            self.queuedPresentedSheet = nil
            DispatchQueue.main.async { activePresentedSheet = queuedPresentedSheet }
        }
    }

    private func presentSheet(_ sheet: HomePresentedSheet) {
        guard activePresentedSheet == nil else {
            queuedPresentedSheet = sheet
            activePresentedSheet = nil
            return
        }
        activePresentedSheet = sheet
    }

    private func openOriginalVideo(_ video: FeaturedHomeVideo) {
        let urlString = {
            let primary = video.openURLString.trimmingCharacters(in: .whitespacesAndNewlines)
            if !primary.isEmpty { return primary }
            return video.embedURL.trimmingCharacters(in: .whitespacesAndNewlines)
        }()
        guard !urlString.isEmpty, let url = URL(string: urlString) else { return }
        beatPlaybackManager.stop()
        audioPlayerManager.stop()
        videoPlaybackManager.stop()
        activePresentedSheet = nil
        originalVideoViewerTarget = HomeOriginalVideoViewerTarget(
            urlString: url.absoluteString,
            title: video.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Original" : video.title
        )
    }

    private func stopAllMediaPlayback() {
        beatPlaybackManager.stop()
        audioPlayerManager.stop()
        videoPlaybackManager.stop()
    }

    private func openVideoHubFromMediaCluster(video: FeaturedHomeVideo) {
        stopAllMediaPlayback()
        videoHubLaunchTarget = HomeVideoHubLaunchTarget(videoID: video.id)
    }

    private func openOriginalFromMediaCluster(video: FeaturedHomeVideo) {
        openOriginalVideo(video)
    }
}

@MainActor
private func homeTrackedSignalCount(_ viewModel: HomeViewModel) -> Int {
    [viewModel.featuredTrack != nil, viewModel.featuredBeat != nil, viewModel.featuredVideo != nil]
        .filter { $0 }
        .count
}

@MainActor
private func homeCommandPriorityTarget(_ viewModel: HomeViewModel) -> String {
    if viewModel.featuredTrack == nil { return "music" }
    if viewModel.featuredBeat == nil { return "beats" }
    if viewModel.featuredVideo == nil { return "visuals" }
    let hour = Calendar.current.component(.hour, from: Date())
    switch hour {
    case 5..<12: return "music"
    case 12..<18: return "beats"
    default: return "visuals"
    }
}

private enum HomePresentedSheet: String, Identifiable, Equatable {
    case beatHub
    case nicmaProducer
    var id: String { rawValue }
}

private struct HomeOriginalVideoViewerTarget: Identifiable {
    let id = UUID()
    let urlString: String
    let title: String
}

private struct HomeMediaClusterSection: View {
    let colorScheme: ColorScheme
    let viewModel: HomeViewModel
    let playbackManager: AudioPlayerManager
    let beatPlaybackManager: BeatPlaybackManager
    let videoPlaybackManager: HomeInlineVideoPlaybackManager
    let onOpenVideoHub: (FeaturedHomeVideo) -> Void
    let onOpenOriginal: (FeaturedHomeVideo) -> Void

    var body: some View {
        HomeMediaCluster(
            colorScheme: colorScheme,
            viewModel: viewModel,
            playbackManager: playbackManager,
            beatPlaybackManager: beatPlaybackManager,
            videoPlaybackManager: videoPlaybackManager,
            onOpenVideoHub: onOpenVideoHub,
            onOpenOriginal: onOpenOriginal
        )
    }
}

private struct HomeLiveSignalSection: View {
    let colorScheme: ColorScheme
    let hasTrackSignal: Bool
    let hasBeatSignal: Bool
    let hasVideoSignal: Bool
    let trackName: String?
    let beatName: String?
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
            hasBeatSignal: hasBeatSignal,
            hasVideoSignal: hasVideoSignal,
            trackName: trackName,
            beatName: beatName,
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
