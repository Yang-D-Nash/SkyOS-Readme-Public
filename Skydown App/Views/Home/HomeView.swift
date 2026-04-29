import AVKit
import FirebaseFunctions
import SwiftUI
import UIKit

extension Notification.Name {
    static let skydownOpenHomeProductivitySheet = Notification.Name("skydown.openHomeProductivitySheet")
}

/// Dedicated Home entry surface for clearer ownership and faster iteration.
struct HomeView: View {
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onGuestSignIn: (() -> Void)?
    let onOpenWorkflow: (() -> Void)?
    let onOpenWorkflowWithPrompt: ((String) -> Void)?

    init(
        onOpenCart: @escaping () -> Void = {},
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        onGuestSignIn: (() -> Void)? = nil,
        onOpenWorkflow: (() -> Void)? = nil,
        onOpenWorkflowWithPrompt: ((String) -> Void)? = nil
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onGuestSignIn = onGuestSignIn
        self.onOpenWorkflow = onOpenWorkflow
        self.onOpenWorkflowWithPrompt = onOpenWorkflowWithPrompt
    }

    var body: some View {
        HomeViewContent(
            onOpenCart: onOpenCart,
            onOpenProfile: onOpenProfile,
            onOpenSettings: onOpenSettings,
            onGuestSignIn: onGuestSignIn,
            onOpenWorkflow: onOpenWorkflow,
            onOpenWorkflowWithPrompt: onOpenWorkflowWithPrompt
        )
    }
}

private enum HomeSectionAnchor: String {
    case release
    case video
}

private enum FounderBriefingMode: String {
    case privateBriefing = "private"
    case group = "group"
}

private struct FounderBriefingPresentation: Identifiable {
    let id = UUID()
    let mode: FounderBriefingMode
    let title: String
    let body: String
    var metaLine: String? = nil
}

private enum FounderBriefingFeedbackStyle {
    case success
    case error
}

struct HomeViewContent: View {
    @StateObject private var viewModel = HomeViewModel()
    @StateObject private var audioPlayerManager = AudioPlayerManager()
    @StateObject private var videoPlaybackManager = HomeInlineVideoPlaybackManager()
    @State private var sheetPresentation = SkydownQueuedPresentation<HomePresentedSheet>()
    @State private var fullscreenVideoTarget: HomeFullscreenVideoTarget?
    @State private var originalVideoViewerTarget: HomeOriginalVideoViewerTarget?
    @State private var hasLoadedInitialHomeContent = false
    @State private var isQuickActionCoolingDown = false
    @State private var founderBriefingPresentation: FounderBriefingPresentation?
    @State private var founderBriefingModeInFlight: FounderBriefingMode?
    @State private var founderBriefingFeedbackMessage: String?
    @State private var founderBriefingFeedbackStyle: FounderBriefingFeedbackStyle = .success
    @State private var founderBriefingErrorMessage: String?
    @State private var founderBriefingShareItems: [Any] = []
    @State private var showsFounderBriefingShareSheet = false
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onGuestSignIn: (() -> Void)?
    let onOpenWorkflow: (() -> Void)?
    let onOpenWorkflowWithPrompt: ((String) -> Void)?

    init(
        onOpenCart: @escaping () -> Void = {},
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        onGuestSignIn: (() -> Void)? = nil,
        onOpenWorkflow: (() -> Void)? = nil,
        onOpenWorkflowWithPrompt: ((String) -> Void)? = nil
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onGuestSignIn = onGuestSignIn
        self.onOpenWorkflow = onOpenWorkflow
        self.onOpenWorkflowWithPrompt = onOpenWorkflowWithPrompt
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
                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
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

                                HomeArtistSocialLinksRow(
                                    colorScheme: colorScheme,
                                    onOpenArtistPage: { _ in openReleaseSection() }
                                )
                                    .homeReveal(3)

                                HomeProductivityOverviewSection(
                                    colorScheme: colorScheme,
                                    remindersToday: viewModel.dueTodayReminders,
                                    remindersUpcoming: viewModel.upcomingReminders,
                                    openTasks: viewModel.openTasks,
                                    recentNotes: viewModel.recentNotes,
                                    syncPaused: viewModel.syncPaused,
                                    recoverableError: viewModel.recoverableError,
                                    onRequestFounderBriefing: onOpenWorkflowWithPrompt == nil ? nil : { mode in
                                        Task {
                                            await runFounderBriefing(mode: mode)
                                        }
                                    },
                                    founderBriefingModeInFlight: founderBriefingModeInFlight,
                                    founderBriefingFeedbackMessage: founderBriefingFeedbackMessage,
                                    founderBriefingFeedbackStyle: founderBriefingFeedbackStyle,
                                    onOpenToday: {
                                        runProductivityAction(presenting: .reminderManager)
                                    },
                                    onOpenUpcoming: {
                                        runProductivityAction(presenting: .reminderManager)
                                    },
                                    onOpenTasks: {
                                        runProductivityAction(presenting: .taskManager)
                                    },
                                    onOpenNotes: {
                                        runProductivityAction(presenting: .noteManager)
                                    },
                                    onCreateReminder: {
                                        runProductivityAction(presenting: .reminderComposer)
                                    },
                                    onCreateTask: {
                                        runProductivityAction(presenting: .taskComposer)
                                    },
                                    onCreateNote: {
                                        runProductivityAction(presenting: .noteComposer)
                                    },
                                    onRetryRecovery: {
                                        SkydownHaptics.selection()
                                        viewModel.refresh()
                                    }
                                )
                                .homeReveal(4)
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
                            .homeReveal(4)

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
                                .homeReveal(5)
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
                        SkydownBrandActionButton(
                            title: "",
                            systemImage: "arrow.triangle.branch",
                            accent: AppColors.accentHighlight(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .subheadline.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: false,
                            action: onOpenWorkflow
                        )
                        .skydownInteractiveFeedback()
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
                case .reminderComposer:
                    HomeReminderComposerSheet(
                        colorScheme: colorScheme,
                        onCreate: { title, dueAt in
                            try await viewModel.createReminder(title: title, dueAt: dueAt)
                        }
                    )
                case .taskComposer:
                    HomeTaskComposerSheet(
                        colorScheme: colorScheme,
                        onCreate: { title, details, dueAt in
                            try await viewModel.createTask(title: title, details: details, dueAt: dueAt)
                        }
                    )
                case .noteComposer:
                    HomeNoteComposerSheet(
                        colorScheme: colorScheme,
                        onCreate: { title, content in
                            try await viewModel.createNote(title: title, content: content)
                        }
                    )
                case .reminderManager:
                    HomeReminderManagerSheet(
                        colorScheme: colorScheme,
                        reminders: Array((viewModel.dueTodayReminders + viewModel.upcomingReminders).prefix(12)),
                        onUpdateTitle: { id, title in
                            try await viewModel.updateReminderTitle(reminderID: id, title: title)
                        },
                        onDelete: { id in
                            try await viewModel.deleteReminder(reminderID: id)
                        }
                    )
                case .taskManager:
                    HomeTaskManagerSheet(
                        colorScheme: colorScheme,
                        tasks: Array(viewModel.openTasks.prefix(12)),
                        onUpdateTitle: { id, title in
                            try await viewModel.updateTaskTitle(taskID: id, title: title)
                        },
                        onDelete: { id in
                            try await viewModel.deleteTask(taskID: id)
                        }
                    )
                case .noteManager:
                    HomeNoteManagerSheet(
                        colorScheme: colorScheme,
                        notes: Array(viewModel.recentNotes.prefix(12)),
                        onUpdateTitle: { id, title in
                            try await viewModel.updateNoteTitle(noteID: id, title: title)
                        },
                        onDelete: { id in
                            try await viewModel.deleteNote(noteID: id)
                        }
                    )
                }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
        .fullScreenCover(item: $originalVideoViewerTarget) { target in
            SkydownOriginalVideoDestinationView(urlString: target.urlString, title: target.title)
        }
        .fullScreenCover(item: $fullscreenVideoTarget) { target in
            HomeFullscreenVideoViewer(video: target.video)
        }
        .onReceive(NotificationCenter.default.publisher(for: .skydownOpenHomeProductivitySheet)) { notification in
            guard let rawSheet = notification.userInfo?["sheet"] as? String else { return }
            switch rawSheet {
            case AgentHomeProductivityTarget.reminderManager.rawValue:
                presentSheet(.reminderManager)
            case AgentHomeProductivityTarget.taskManager.rawValue:
                presentSheet(.taskManager)
            case AgentHomeProductivityTarget.noteManager.rawValue:
                presentSheet(.noteManager)
            default:
                break
            }
        }
        .sheet(item: $founderBriefingPresentation) { presentation in
            FounderBriefingResultSheet(
                colorScheme: colorScheme,
                presentation: presentation,
                onCopy: {
                    UIPasteboard.general.string = presentation.body
                },
                onShare: {
                    founderBriefingShareItems = [presentation.body]
                    showsFounderBriefingShareSheet = true
                },
                onShareWhatsApp: {
                    shareFounderBriefingToWhatsApp(presentation.body)
                }
            )
        }
        .sheet(isPresented: $showsFounderBriefingShareSheet) {
            AIShareSheet(activityItems: founderBriefingShareItems)
        }
        .alert(
            AppLocalized.text("common.status.idle", fallback: "Status"),
            isPresented: Binding(
                get: { founderBriefingErrorMessage != nil },
                set: { shouldShow in
                    if !shouldShow { founderBriefingErrorMessage = nil }
                }
            ),
            actions: {
                Button(AppLocalized.text("common.close", fallback: "Close"), role: .cancel) {
                    founderBriefingErrorMessage = nil
                }
            },
            message: {
                Text(founderBriefingErrorMessage ?? "")
            }
        )
        .overlay {
            if founderBriefingModeInFlight != nil {
                ZStack {
                    Color.black.opacity(0.12).ignoresSafeArea()
                    ProgressView(AppLocalized.text("home.owner.founder.progress", fallback: "Composing your intelligence briefing…"))
                        .padding(14)
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
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

    private func triggerQuickAction(_ action: @escaping () -> Void) {
        guard !isQuickActionCoolingDown else { return }
        isQuickActionCoolingDown = true
        action()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) {
            isQuickActionCoolingDown = false
        }
    }

    private func runProductivityAction(presenting sheet: HomePresentedSheet) {
        triggerQuickAction {
            if authManager.userSession == nil, let onGuestSignIn {
                onGuestSignIn()
            } else {
                presentSheet(sheet)
            }
        }
    }

    private static func founderBriefingMetaLine(_ meta: [String: Any]?) -> String? {
        guard let meta, !meta.isEmpty else { return nil }
        var parts: [String] = []
        if let d = meta["businessDate"] as? String, !d.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let day = d.trimmingCharacters(in: .whitespacesAndNewlines)
            parts.append(String(
                format: AppLocalized.text("home.owner.founder.meta_report_day", fallback: "Report day %@"),
                day
            ))
        }
        if let qRaw = meta["dataQuality"] as? String {
            let q = qRaw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            if !q.isEmpty {
                let label: String
                switch q {
                case "complete": label = AppLocalized.text("home.owner.founder.meta_data_complete", fallback: "Data complete")
                case "partial": label = AppLocalized.text("home.owner.founder.meta_data_partial", fallback: "Partial data")
                case "no_kpi_doc": label = AppLocalized.text("home.owner.founder.meta_kpi_doc_missing", fallback: "KPI document missing")
                default: label = q
                }
                parts.append(label)
            }
        }
        if let k = meta["kpiUpdatedAt"] as? String, !k.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let t = k.trimmingCharacters(in: .whitespacesAndNewlines)
            let short = t.count > 36 ? String(t.prefix(36)) + "…" : t
            parts.append(String(
                format: AppLocalized.text("home.owner.founder.meta_kpi_label", fallback: "KPI %@"),
                short
            ))
        }
        if parts.isEmpty { return nil }
        return parts.joined(separator: " · ")
    }

    private func runFounderBriefing(mode: FounderBriefingMode) async {
        guard founderBriefingModeInFlight == nil else { return }
        guard let uid = authManager.userSession?.id else {
            founderBriefingErrorMessage = AppLocalized.text("home.owner.founder.error_login", fallback: "Please sign in first.")
            return
        }
        founderBriefingFeedbackMessage = nil
        founderBriefingModeInFlight = mode
        defer { founderBriefingModeInFlight = nil }
        do {
            let requestDate = Self.founderBriefingDateFormatter.string(from: Date())
            let requestId = "home-\(mode.rawValue)-\(Int(Date().timeIntervalSince1970))"

            let data: [String: Any]
            do {
                data = try await requestFounderBriefingWorkflowResult(
                    uid: uid,
                    mode: mode,
                    date: requestDate,
                    requestId: requestId
                )
            } catch {
                if shouldRetryFounderBriefing(after: error) {
                    let functions = Functions.functions(region: "us-central1")
                    _ = try? await functions.invokeCallable("syncCurrentUserClaims", payload: [:])
                    data = try await requestFounderBriefingWorkflowResult(
                        uid: uid,
                        mode: mode,
                        date: requestDate,
                        requestId: requestId
                    )
                } else {
                    throw error
                }
            }

            let textKey = mode == .privateBriefing ? "private" : "group"
            let text = (data[textKey] as? String)?
                .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if text.isEmpty {
                founderBriefingFeedbackMessage = AppLocalized.text("home.owner.founder.error_empty", fallback: "The briefing returned no content.")
                founderBriefingFeedbackStyle = .error
            let emptyMeta = Self.founderBriefingMetaLine(data["briefingMeta"] as? [String: Any])
            founderBriefingPresentation = FounderBriefingPresentation(
                mode: mode,
                title: mode == .privateBriefing ?
                    AppLocalized.text("home.owner.founder.title_private", fallback: "Private intelligence") :
                    AppLocalized.text("home.owner.founder.title_group", fallback: "Team update"),
                body: AppLocalized.text("home.owner.founder.error_empty_body", fallback: "The run completed, but no text was returned. Please try again."),
                metaLine: emptyMeta
            )
                return
            }
            founderBriefingFeedbackMessage = AppLocalized.text("home.owner.founder.success", fallback: "Briefing ready.")
            founderBriefingFeedbackStyle = .success
            let metaLine = Self.founderBriefingMetaLine(data["briefingMeta"] as? [String: Any])
            founderBriefingPresentation = FounderBriefingPresentation(
                mode: mode,
                title: mode == .privateBriefing ?
                    AppLocalized.text("home.owner.founder.title_private", fallback: "Private intelligence") :
                    AppLocalized.text("home.owner.founder.title_group", fallback: "Team update"),
                body: text,
                metaLine: metaLine
            )
        } catch {
            let reason = (error as NSError).localizedDescription
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let fallbackUnavailable = AppLocalized.text("home.owner.founder.error_unavailable", fallback: "Briefing is temporarily unavailable. Please try again shortly.")
            founderBriefingFeedbackMessage = reason.isEmpty ? fallbackUnavailable : reason
            founderBriefingFeedbackStyle = .error
            founderBriefingPresentation = FounderBriefingPresentation(
                mode: mode,
                title: mode == .privateBriefing ?
                    AppLocalized.text("home.owner.founder.title_private", fallback: "Private intelligence") :
                    AppLocalized.text("home.owner.founder.title_group", fallback: "Team update"),
                body: reason.isEmpty ? fallbackUnavailable : reason
            )
        }
    }

    private func requestFounderBriefingWorkflowResult(
        uid: String,
        mode: FounderBriefingMode,
        date: String,
        requestId: String
    ) async throws -> [String: Any] {
        let payload: [String: Any] = [
            "trigger": "home_founder_briefing",
            "source": "ios_home_owner_buttons",
            "automationScope": "owner",
            "data": [
                "uid": uid,
                "mode": "briefing",
                "briefingTarget": mode.rawValue,
                "date": date,
                "requestId": requestId,
                "isOwner": true,
            ],
        ]
        let result = try await Functions.functions(region: "us-central1")
            .invokeCallable("triggerWorkflowAutomation", payload: payload)
        guard let rootData = result.data as? [String: Any] else {
            throw NSError(
                domain: "HomeFounderBriefing",
                code: -1,
                userInfo: [
                    NSLocalizedDescriptionKey: AppLocalized.text(
                        "home.owner.founder.error_parse",
                        fallback: "Briefing response could not be read."
                    )
                ]
            )
        }
        let workflowData = (rootData["data"] as? [String: Any]) ?? rootData

        let workflowStatus = (workflowData["workflowStatus"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() ?? ""
        if workflowStatus == "failed" {
            let message = (workflowData["message"] as? String)?
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let resolvedMessage = (message?.isEmpty == false ? message : nil) ?? AppLocalized.text(
                "home.owner.founder.error_workflow_failed",
                fallback: "Workflow could not be completed."
            )
            throw NSError(
                domain: "HomeFounderBriefing",
                code: -2,
                userInfo: [NSLocalizedDescriptionKey: resolvedMessage]
            )
        }

        var resolved: [String: Any] = [:]
        var fallbackText: String?
        let bodyCandidate = workflowData["body"] ?? rootData["body"]
        var nestedBody = bodyCandidate as? [String: Any]
        let rawBodyText = (bodyCandidate as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if nestedBody == nil,
           !rawBodyText.isEmpty,
           let jsonData = rawBodyText.data(using: .utf8),
           let parsed = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any] {
            nestedBody = parsed
        }

        let topLevelPrivate = ((workflowData["private"] as? String) ??
            (rootData["private"] as? String) ??
            (nestedBody?["private"] as? String) ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let topLevelGroup = ((workflowData["group"] as? String) ??
            (rootData["group"] as? String) ??
            (nestedBody?["group"] as? String) ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if !topLevelPrivate.isEmpty {
            resolved["private"] = topLevelPrivate
            fallbackText = topLevelPrivate
        }
        if !topLevelGroup.isEmpty {
            resolved["group"] = topLevelGroup
            if fallbackText == nil {
                fallbackText = topLevelGroup
            }
        }
        let rawBodyPlainText = (nestedBody == nil) ? rawBodyText : ""
        if !rawBodyPlainText.isEmpty {
            if mode == .privateBriefing {
                resolved["private"] = rawBodyPlainText
            } else {
                resolved["group"] = rawBodyPlainText
            }
            if fallbackText == nil {
                fallbackText = rawBodyPlainText
            }
        }

        let resultsSource = (workflowData["results"] as? [[String: Any]]) ?? (rootData["results"] as? [[String: Any]])
        if let results = resultsSource {
            for entry in results {
                let entryType = ((entry["type"] as? String) ?? "")
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                    .lowercased()
                guard entryType == "text" else { continue }
                let title = ((entry["title"] as? String) ?? "")
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                    .lowercased()
                let content = ((entry["content"] as? String) ?? (entry["text"] as? String) ?? "")
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                guard !content.isEmpty else { continue }
                if fallbackText == nil {
                    fallbackText = content
                }
                if title.contains("private") || title.contains("only me") || title.contains("nur") {
                    resolved["private"] = content
                } else if title.contains("group") || title.contains("gruppe") || title.contains("team") {
                    resolved["group"] = content
                }
            }
        }

        if resolved["private"] == nil, mode == .privateBriefing, let fallbackText {
            resolved["private"] = fallbackText
        }
        if resolved["group"] == nil, mode == .group, let fallbackText {
            resolved["group"] = fallbackText
        }

        if resolved.isEmpty {
            let message = (workflowData["message"] as? String)?
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let resolvedMessage = (message?.isEmpty == false ? message : nil) ??
                AppLocalized.text(
                    "home.owner.founder.error_missing_text",
                    fallback: "Workflow ran, but no briefing text was returned."
                )
            throw NSError(
                domain: "HomeFounderBriefing",
                code: -3,
                userInfo: [NSLocalizedDescriptionKey: resolvedMessage]
            )
        }
        if let meta = (workflowData["briefingMeta"] as? [String: Any]) ?? (rootData["briefingMeta"] as? [String: Any]) {
            resolved["briefingMeta"] = meta
        }
        return resolved
    }

    private func shouldRetryFounderBriefing(after error: Error) -> Bool {
        let nsError = error as NSError
        guard nsError.domain == FunctionsErrorDomain,
              let code = FunctionsErrorCode(rawValue: nsError.code) else {
            return false
        }
        return code == .permissionDenied || code == .unauthenticated
    }

    private func shareFounderBriefingToWhatsApp(_ text: String) {
        guard let encoded = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "whatsapp://send?text=\(encoded)") else {
            founderBriefingShareItems = [text]
            showsFounderBriefingShareSheet = true
            return
        }
        if UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
            return
        }
        founderBriefingShareItems = [text]
        showsFounderBriefingShareSheet = true
    }

    private static let founderBriefingDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        return formatter
    }()

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
    case reminderComposer
    case taskComposer
    case noteComposer
    case reminderManager
    case taskManager
    case noteManager
    var id: String { rawValue }
}

private struct HomeManageableItemRow: View {
    let colorScheme: ColorScheme
    let title: String
    let subtitle: String?
    let isEditing: Bool
    @Binding var draftTitle: String
    let fieldPlaceholder: String
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onSave: () -> Void
    private var editAccessibilityLabel: String {
        String(
            format: AppLocalized.text("home.manager.edit_item", fallback: "Edit %@"),
            title
        )
    }
    private var deleteAccessibilityLabel: String {
        String(
            format: AppLocalized.text("home.manager.delete_item", fallback: "Delete %@"),
            title
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingPill) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(2)
                    if let subtitle, !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.caption2)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
                Spacer(minLength: 8)
                HStack(spacing: SkydownLayout.stackSpacingHairline) {
                    SkydownBrandActionButton(
                        title: "",
                        systemImage: "pencil",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: onEdit
                    )
                    .skydownInteractiveFeedback()
                    .accessibilityLabel(editAccessibilityLabel)

                    Button(role: .destructive, action: onDelete) {
                        Image(systemName: "trash")
                            .font(.caption.weight(.semibold))
                            .frame(width: 44, height: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .accessibilityLabel(deleteAccessibilityLabel)
                }
            }

            if isEditing {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                    TextField(fieldPlaceholder, text: $draftTitle)
                        .textInputAutocapitalization(.sentences)
                        .padding(12)
                        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.55))
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.save", fallback: "Save"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.compactRadius,
                        verticalPadding: 10,
                        action: onSave
                    )
                    .accessibilityLabel(AppLocalized.text("common.save", fallback: "Save"))
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(14)
        .skydownPanelSurface(colorScheme: colorScheme, shadowRadius: 7, shadowYOffset: 3)
        .animation(SkydownMotion.statusTransition, value: isEditing)
    }
}

private struct HomeReminderManagerSheet: View {
    let colorScheme: ColorScheme
    let reminders: [HomeViewModel.ProductivityReminder]
    let onUpdateTitle: (_ id: String, _ title: String) async throws -> Void
    let onDelete: (_ id: String) async throws -> Void

    var body: some View {
        HomeManageableListSheet(
            colorScheme: colorScheme,
            title: AppLocalized.text("home.manager.reminders.title", fallback: "Reminders"),
            items: reminders,
            itemTitle: { $0.title },
            itemSubtitle: { reminder in
                reminder.dueAt.map {
                    DateFormatter.localizedString(from: $0, dateStyle: .short, timeStyle: .short)
                }
            },
            onUpdateTitle: onUpdateTitle,
            onDelete: onDelete
        )
    }
}

private struct HomeTaskManagerSheet: View {
    let colorScheme: ColorScheme
    let tasks: [HomeViewModel.ProductivityTask]
    let onUpdateTitle: (_ id: String, _ title: String) async throws -> Void
    let onDelete: (_ id: String) async throws -> Void

    var body: some View {
        HomeManageableListSheet(
            colorScheme: colorScheme,
            title: AppLocalized.text("tasks.title", fallback: "Tasks"),
            items: tasks,
            itemTitle: { $0.title },
            itemSubtitle: { _ in nil },
            onUpdateTitle: onUpdateTitle,
            onDelete: onDelete
        )
    }
}

private struct HomeNoteManagerSheet: View {
    let colorScheme: ColorScheme
    let notes: [HomeViewModel.ProductivityNote]
    let onUpdateTitle: (_ id: String, _ title: String) async throws -> Void
    let onDelete: (_ id: String) async throws -> Void

    var body: some View {
        HomeManageableListSheet(
            colorScheme: colorScheme,
            title: AppLocalized.text("notes.title", fallback: "Notes"),
            items: notes,
            itemTitle: { $0.title },
            itemSubtitle: { note in
                note.updatedAt.map {
                    DateFormatter.localizedString(from: $0, dateStyle: .short, timeStyle: .short)
                }
            },
            onUpdateTitle: onUpdateTitle,
            onDelete: onDelete
        )
    }
}

private struct HomeManageableListSheet<Item: Identifiable>: View where Item.ID == String {
    let colorScheme: ColorScheme
    let title: String
    let items: [Item]
    let itemTitle: (Item) -> String
    let itemSubtitle: (Item) -> String?
    let onUpdateTitle: (_ id: String, _ title: String) async throws -> Void
    let onDelete: (_ id: String) async throws -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var editingID: String?
    @State private var draftTitle: String = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                if items.isEmpty {
                    Text(AppLocalized.text("home.manager.empty", fallback: "All clear."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 10)
                } else {
                    ForEach(items) { item in
                        let rowTitle = itemTitle(item)
                        let rowSubtitle = itemSubtitle(item)
                        HomeManageableItemRow(
                            colorScheme: colorScheme,
                            title: rowTitle,
                            subtitle: rowSubtitle,
                            isEditing: editingID == item.id,
                            draftTitle: $draftTitle,
                            fieldPlaceholder: AppLocalized.text("home.manager.rename_placeholder", fallback: "New title"),
                            onEdit: {
                                withAnimation(SkydownMotion.statusTransition) {
                                    editingID = item.id
                                    draftTitle = rowTitle
                                }
                            },
                            onDelete: {
                                Task { try? await onDelete(item.id) }
                            },
                            onSave: {
                                let normalized = draftTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                                guard !normalized.isEmpty else { return }
                                Task {
                                    try? await onUpdateTitle(item.id, normalized)
                                    await MainActor.run {
                                        withAnimation(SkydownMotion.statusTransition) {
                                            editingID = nil
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding - 4)
            .padding(.vertical, 8)
        }
        .navigationTitle(title)
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
                    action: { dismiss() }
                )
                .skydownInteractiveFeedback()
            }
        }
    }
}

private struct HomeReminderComposerSheet: View {
    let colorScheme: ColorScheme
    let onCreate: (_ title: String, _ dueAt: Date) async throws -> Void
    @State private var titleText = ""
    @State private var dueAt = Date().addingTimeInterval(60 * 60)
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                Text(AppLocalized.text("home.sheet.reminder.hint", fallback: "Pick a clear title, then set date and time. SkyOS uses the exact timestamp for push scheduling."))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                TextField(AppLocalized.text("home.sheet.reminder.title_hint", fallback: "Reminder title"), text: $titleText)
                    .submitLabel(.done)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                DatePicker(
                    AppLocalized.text("home.sheet.reminder.date_label", fallback: "Due at"),
                    selection: $dueAt,
                    displayedComponents: [.date, .hourAndMinute]
                )
                SkydownBrandActionButton(
                    title: AppLocalized.text("home.sheet.add", fallback: "Add"),
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    isEnabled: !titleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 12,
                    action: {
                        let normalized = titleText.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !normalized.isEmpty else { return }
                        Task {
                            try? await onCreate(normalized, dueAt)
                            dismiss()
                        }
                    }
                )
            }
            .padding()
        }
        .scrollDismissesKeyboard(.interactively)
        .skydownPremiumInputSurface()
        .navigationTitle(AppLocalized.text("home.quick.create_reminder", fallback: "Create Reminder"))
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
                    action: { dismiss() }
                )
                .skydownInteractiveFeedback()
            }
        }
    }
}

private struct HomeTaskComposerSheet: View {
    let colorScheme: ColorScheme
    let onCreate: (_ title: String, _ details: String, _ dueAt: Date?) async throws -> Void
    @State private var titleText = ""
    @State private var detailText = ""
    @State private var useDueAt = false
    @State private var dueAt = Date().addingTimeInterval(2 * 60 * 60)
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                Text(AppLocalized.text("home.sheet.task.hint", fallback: "Start with a verb and keep it actionable. Example: Send invoice to Alex."))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                TextField(AppLocalized.text("tasks.input.title_hint", fallback: "Task title"), text: $titleText)
                    .submitLabel(.done)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                TextField(AppLocalized.text("tasks.input.details_hint", fallback: "Optional details"), text: $detailText)
                    .submitLabel(.done)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                Toggle(
                    AppLocalized.text("home.task.due_sublabel", fallback: "Due (optional)"),
                    isOn: $useDueAt
                )
                .tint(AppColors.accentMystic(for: colorScheme))
                if useDueAt {
                    DatePicker(
                        AppLocalized.text("home.task.due_sublabel", fallback: "Due (optional)"),
                        selection: $dueAt,
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    .tint(AppColors.accentMystic(for: colorScheme))
                }
                SkydownBrandActionButton(
                    title: AppLocalized.text("tasks.input.add", fallback: "Create task"),
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    isEnabled: !titleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 12,
                    action: {
                        let title = titleText.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !title.isEmpty else { return }
                        Task {
                            let due: Date? = useDueAt ? dueAt : nil
                            try? await onCreate(title, detailText, due)
                            dismiss()
                        }
                    }
                )
            }
            .padding()
        }
        .scrollDismissesKeyboard(.interactively)
        .skydownPremiumInputSurface()
        .navigationTitle(AppLocalized.text("home.quick.create_task", fallback: "Create Task"))
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
                    action: { dismiss() }
                )
                .skydownInteractiveFeedback()
            }
        }
    }
}

private struct HomeNoteComposerSheet: View {
    let colorScheme: ColorScheme
    let onCreate: (_ title: String, _ content: String) async throws -> Void
    @State private var titleText = ""
    @State private var contentText = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                Text(AppLocalized.text("home.sheet.note.hint", fallback: "Use a short title and keep the first line specific."))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                TextField(AppLocalized.text("notes.input.title_hint", fallback: "Note title"), text: $titleText)
                    .submitLabel(.done)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                TextField(AppLocalized.text("notes.input.content_hint", fallback: "Write a quick note..."), text: $contentText, axis: .vertical)
                    .lineLimit(3...5)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                SkydownBrandActionButton(
                    title: AppLocalized.text("notes.input.add", fallback: "Create note"),
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    isEnabled: !titleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        || !contentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 12,
                    action: {
                        let title = titleText.trimmingCharacters(in: .whitespacesAndNewlines)
                        let content = contentText.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !title.isEmpty || !content.isEmpty else { return }
                        Task {
                            try? await onCreate(title, content)
                            dismiss()
                        }
                    }
                )
            }
            .padding()
        }
        .scrollDismissesKeyboard(.interactively)
        .skydownPremiumInputSurface()
        .navigationTitle(AppLocalized.text("home.quick.create_note", fallback: "Create Note"))
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
                    action: { dismiss() }
                )
                .skydownInteractiveFeedback()
            }
        }
    }
}

private struct FounderBriefingResultSheet: View {
    let colorScheme: ColorScheme
    let presentation: FounderBriefingPresentation
    let onCopy: () -> Void
    let onShare: () -> Void
    let onShareWhatsApp: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                Text(presentation.title)
                    .font(.headline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text(
                    presentation.mode == .privateBriefing ?
                    AppLocalized.text("home.owner.founder.caption_private", fallback: "Confidential — your daily signal") :
                    AppLocalized.text("home.owner.founder.caption_group", fallback: "Ready to share with your team")
                )
                .font(.caption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                if let meta = presentation.metaLine, !meta.isEmpty {
                    Text(meta)
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                }

                ScrollView {
                    Text(presentation.body)
                        .font(.footnote)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(AppColors.cardBackground(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
                }

                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                    SkydownBrandActionButton(
                        title: "WhatsApp",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.compactRadius,
                        verticalPadding: 8,
                        action: onShareWhatsApp
                    )
                    .frame(maxWidth: .infinity)
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.copy", fallback: "Copy"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.compactRadius,
                        verticalPadding: 8,
                        action: onCopy
                    )
                    .frame(maxWidth: .infinity)
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.open_link", fallback: "Share"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.compactRadius,
                        verticalPadding: 8,
                        action: onShare
                    )
                    .frame(maxWidth: .infinity)
                }
            }
            .padding()
            .navigationTitle(AppLocalized.text("home.owner.founder.sheet_nav", fallback: "Intelligence"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
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
                } else if let url = URL(string: video.nativePlaybackURLString), !video.nativePlaybackURLString.isEmpty {
                    VideoPlayer(player: player)
                        .ignoresSafeArea()
                        .onAppear {
                            player.replaceCurrentItem(with: AVPlayerItem(url: url))
                            player.play()
                            isPlaying = true
                        }
                } else {
                    VStack(spacing: SkydownLayout.stackSpacingCompact) {
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
        !video.usesEmbeddedPreview && !video.nativePlaybackURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            HomeSectionEyebrow(
                AppLocalized.text("home.section.current", fallback: "Current"),
                colorScheme: colorScheme
            )
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

private struct HomeProductivityOverviewSection: View {
    let colorScheme: ColorScheme
    let remindersToday: [HomeViewModel.ProductivityReminder]
    let remindersUpcoming: [HomeViewModel.ProductivityReminder]
    let openTasks: [HomeViewModel.ProductivityTask]
    let recentNotes: [HomeViewModel.ProductivityNote]
    let syncPaused: Bool
    let recoverableError: String?
    let onRequestFounderBriefing: ((FounderBriefingMode) -> Void)?
    let founderBriefingModeInFlight: FounderBriefingMode?
    let founderBriefingFeedbackMessage: String?
    let founderBriefingFeedbackStyle: FounderBriefingFeedbackStyle
    let onOpenToday: () -> Void
    let onOpenUpcoming: () -> Void
    let onOpenTasks: () -> Void
    let onOpenNotes: () -> Void
    let onCreateReminder: () -> Void
    let onCreateTask: () -> Void
    let onCreateNote: () -> Void
    let onRetryRecovery: () -> Void
    @State private var showsExtendedSignals = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .none
        formatter.timeStyle = .short
        return formatter
    }()

    var body: some View {
        let reminderCount = remindersToday.count + remindersUpcoming.count
        let taskCount = openTasks.count
        let noteCount = recentNotes.count
        let hiddenSectionCount = 3
        let hasNoProductivitySignals = reminderCount == 0 && taskCount == 0 && noteCount == 0
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HomeSectionEyebrow(
                AppLocalized.text("home.productivity.ask_anything", fallback: "Ask SkyOS anything"),
                colorScheme: colorScheme,
                emphasizesWeight: true
            )

            if syncPaused || (recoverableError?.isEmpty == false) {
                HomeRecoveryInlineBanner(
                    colorScheme: colorScheme,
                    message: recoverableError ?? AppLocalized.text("home.recovery.sync_paused", fallback: "Sync is paused. Tap Refresh to continue."),
                    onRetry: onRetryRecovery
                )
            }

            if hasNoProductivitySignals {
                HStack(spacing: SkydownLayout.stackSpacingTick) {
                    Image(systemName: "sparkles")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                    Text(
                        AppLocalized.text(
                            "home.productivity.empty_prompt",
                            fallback: "Nothing active yet. Start with a quick reminder, task, or note."
                        )
                    )
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(AppColors.cardBackground(for: colorScheme).opacity(0.72))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous)
                        .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
                )
                .transition(.opacity)
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                HomeProductivityRow(
                    title: AppLocalized.text("home.productivity.today", fallback: "Today"),
                    emptyText: AppLocalized.text("home.productivity.empty_today", fallback: "No reminders due today"),
                    onOpen: onOpenToday,
                    count: remindersToday.count,
                    items: remindersToday.map { item in
                        if let dueAt = item.dueAt {
                            return "\(item.title) • \(Self.dateFormatter.string(from: dueAt))"
                        }
                        return item.title
                    }
                )
                if showsExtendedSignals {
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                        HomeProductivityRow(
                            title: AppLocalized.text("home.productivity.upcoming", fallback: "Upcoming"),
                            emptyText: AppLocalized.text("home.productivity.empty_upcoming", fallback: "No upcoming reminders"),
                            onOpen: onOpenUpcoming,
                            count: remindersUpcoming.count,
                            items: remindersUpcoming.map { $0.title }
                        )
                        HomeTaskProductivityRow(
                            title: AppLocalized.text("home.productivity.open_tasks", fallback: "Open Tasks"),
                            emptyText: AppLocalized.text("home.productivity.empty_tasks", fallback: "No open tasks"),
                            onOpen: onOpenTasks,
                            count: openTasks.count,
                            tasks: openTasks,
                            colorScheme: colorScheme
                        )
                        HomeProductivityRow(
                            title: AppLocalized.text("home.productivity.recent_notes", fallback: "Recent Notes"),
                            emptyText: AppLocalized.text("home.productivity.empty_notes", fallback: "No recent notes"),
                            onOpen: onOpenNotes,
                            count: recentNotes.count,
                            items: recentNotes.map { $0.title }
                        )
                    }
                    .transition(.opacity.combined(with: .move(edge: .top)))
                } else {
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                        Button {
                            withAnimation(reduceMotion ? .linear(duration: 0.01) : SkydownMotion.statusTransition) {
                                SkydownHaptics.selection()
                                showsExtendedSignals = true
                            }
                        } label: {
                            Text(
                                String(
                                    format: AppLocalized.text(
                                        "home.productivity.collapse_summary",
                                        fallback: "%d more · R%02d · T%02d · N%02d"
                                    ),
                                    hiddenSectionCount,
                                    reminderCount,
                                    taskCount,
                                    noteCount
                                )
                            )
                            .font(.caption2.weight(.semibold))
                            .foregroundColor(.secondary)
                        }
                        .buttonStyle(.plain)

                        HStack(spacing: SkydownLayout.stackSpacingHairline) {
                            HomeCollapsedMetricChip(
                                title: AppLocalized.text("home.productivity.upcoming", fallback: "Upcoming"),
                                count: remindersUpcoming.count,
                                colorScheme: colorScheme,
                                onTap: onOpenUpcoming
                            )
                            HomeCollapsedMetricChip(
                                title: AppLocalized.text("home.productivity.open_tasks", fallback: "Open Tasks"),
                                count: openTasks.count,
                                colorScheme: colorScheme,
                                onTap: onOpenTasks
                            )
                            HomeCollapsedMetricChip(
                                title: AppLocalized.text("home.productivity.recent_notes", fallback: "Recent Notes"),
                                count: recentNotes.count,
                                colorScheme: colorScheme,
                                onTap: onOpenNotes
                            )
                        }
                    }
                    .transition(
                        reduceMotion
                            ? .opacity
                            : .opacity.combined(with: .move(edge: .bottom))
                    )
                }
            }
            .animation(
                reduceMotion ? .linear(duration: 0.01) : SkydownMotion.statusTransition,
                value: showsExtendedSignals
            )

            if showsExtendedSignals {
                Button {
                    withAnimation(reduceMotion ? .linear(duration: 0.01) : SkydownMotion.statusTransition) {
                        SkydownHaptics.selection()
                        showsExtendedSignals = false
                    }
                } label: {
                    Text(AppLocalized.text("home.productivity.show_less_sections", fallback: "Show fewer sections"))
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(.secondary)
                }
                .buttonStyle(.plain)
                .transition(
                    reduceMotion
                        ? .opacity
                        : .opacity.combined(with: .move(edge: .top))
                )
            }

            HomeSectionEyebrow(
                AppLocalized.text("home.productivity.quick_hint", fallback: "Tap a shortcut to open a calm capture sheet on Home — no tab switch."),
                colorScheme: colorScheme
            )

            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                HomeQuickActionButton(
                    title: AppLocalized.text("home.quick.create_reminder", fallback: "Create Reminder"),
                    colorScheme: colorScheme,
                    onTap: onCreateReminder
                )
                HomeQuickActionButton(
                    title: AppLocalized.text("home.quick.create_task", fallback: "Create Task"),
                    colorScheme: colorScheme,
                    onTap: onCreateTask
                )
                HomeQuickActionButton(
                    title: AppLocalized.text("home.quick.create_note", fallback: "Create Note"),
                    colorScheme: colorScheme,
                    onTap: onCreateNote
                )
            }

            if let onRequestFounderBriefing {
                HomeOwnerWorkflowSection(
                    colorScheme: colorScheme,
                    onRequestFounderBriefing: onRequestFounderBriefing,
                    founderBriefingModeInFlight: founderBriefingModeInFlight,
                    founderBriefingFeedbackMessage: founderBriefingFeedbackMessage,
                    founderBriefingFeedbackStyle: founderBriefingFeedbackStyle
                )
                .padding(.top, 2)
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.88))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct HomeSectionEyebrow: View {
    let text: String
    let colorScheme: ColorScheme
    var emphasizesWeight: Bool = false

    init(_ text: String, colorScheme: ColorScheme, emphasizesWeight: Bool = false) {
        self.text = text
        self.colorScheme = colorScheme
        self.emphasizesWeight = emphasizesWeight
    }

    var body: some View {
        Text(text)
            .font(emphasizesWeight ? .caption2.weight(.bold) : .caption2.weight(.medium))
            .foregroundColor(AppColors.text(for: colorScheme).opacity(emphasizesWeight ? 0.55 : 0.5))
    }
}

private struct HomeRecoveryInlineBanner: View {
    let colorScheme: ColorScheme
    let message: String
    let onRetry: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: SkydownLayout.stackSpacingTick) {
            Image(systemName: "arrow.clockwise.circle")
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.accentMystic(for: colorScheme))
            Text(message)
                .font(.caption2)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .lineLimit(2)
            Spacer(minLength: 6)
            SkydownBrandActionButton(
                title: AppLocalized.text("common.retry", fallback: "Retry"),
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                role: .muted,
                font: .caption.weight(.semibold),
                cornerRadius: SkydownLayout.compactRadius,
                verticalPadding: 6,
                expandToFullWidth: false,
                action: onRetry
            )
            .accessibilityLabel(AppLocalized.text("common.retry", fallback: "Retry"))
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(AppColors.cardBackground(for: colorScheme).opacity(0.72))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
    }
}

private struct HomeCollapsedMetricChip: View {
    let title: String
    let count: Int
    let colorScheme: ColorScheme
    let onTap: () -> Void

    var body: some View {
        Button {
            SkydownHaptics.selection()
            onTap()
        } label: {
            HStack(spacing: SkydownLayout.stackSpacingTick) {
                Text(title)
                    .font(.caption2.weight(.semibold))
                    .lineLimit(1)
                HomeCountBadge(count: count)
            }
            .frame(maxWidth: .infinity, minHeight: 34)
            .padding(.horizontal, 8)
            .background(AppColors.cardBackground(for: colorScheme).opacity(0.8))
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous)
                    .stroke(AppColors.secondaryText(for: colorScheme).opacity(0.14), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .accessibilityLabel("\(title), \(count)")
    }
}

private struct HomeOwnerWorkflowSection: View {
    let colorScheme: ColorScheme
    let onRequestFounderBriefing: (FounderBriefingMode) -> Void
    let founderBriefingModeInFlight: FounderBriefingMode?
    let founderBriefingFeedbackMessage: String?
    let founderBriefingFeedbackStyle: FounderBriefingFeedbackStyle

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(AppLocalized.text("home.owner.workflows.title", fallback: "Founder briefing"))
                .font(.caption.weight(.semibold))
                .foregroundColor(.secondary)
            Text(AppLocalized.text(
                "home.owner.workflows.subtitle",
                fallback: "Run founder analysis with real available data."
            ))
                .font(.caption2)
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.55))
                .fixedSize(horizontal: false, vertical: true)

            if founderBriefingModeInFlight != nil {
                HStack(spacing: 6) {
                    ProgressView()
                        .controlSize(.small)
                    Text(AppLocalized.text("home.owner.founder.progress", fallback: "Composing your intelligence briefing…"))
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }
            } else if let founderBriefingFeedbackMessage {
                Text(founderBriefingFeedbackMessage)
                    .font(.caption2.weight(.semibold))
                    .foregroundColor(
                        founderBriefingFeedbackStyle == .error ?
                            Color.red.opacity(0.86) :
                            AppColors.accentMystic(for: colorScheme)
                    )
            }

            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                HomeQuickActionButton(
                    title: founderBriefingModeInFlight == .privateBriefing ?
                        AppLocalized.text("home.owner.founder.creating", fallback: "Composing…") :
                        AppLocalized.text("home.owner.workflows.private_analysis", fallback: "Private"),
                    systemImage: "dollarsign.circle",
                    colorScheme: colorScheme,
                    isLoading: founderBriefingModeInFlight == .privateBriefing,
                    isDisabled: founderBriefingModeInFlight != nil,
                    onTap: {
                        onRequestFounderBriefing(.privateBriefing)
                    }
                )
                HomeQuickActionButton(
                    title: founderBriefingModeInFlight == .group ?
                        AppLocalized.text("home.owner.founder.creating", fallback: "Composing…") :
                        AppLocalized.text("home.owner.workflows.group_update", fallback: "Team"),
                    systemImage: "person.2",
                    colorScheme: colorScheme,
                    isLoading: founderBriefingModeInFlight == .group,
                    isDisabled: founderBriefingModeInFlight != nil,
                    onTap: {
                        onRequestFounderBriefing(.group)
                    }
                )
            }
        }
    }
}

private struct HomeTaskProductivityRow: View {
    let title: String
    let emptyText: String
    let onOpen: () -> Void
    let count: Int
    let tasks: [HomeViewModel.ProductivityTask]
    let colorScheme: ColorScheme
    private let maxVisibleItems = 2
    @State private var isExpanded = false
    private static let dueFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateStyle = .medium
        f.timeStyle = .short
        return f
    }()

    var body: some View {
        let now = Date()
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
            Button(action: onOpen) {
                HStack(spacing: SkydownLayout.stackSpacingDense) {
                    Text(title)
                        .font(.caption.weight(.semibold))
                    HomeCountBadge(count: count)
                    Image(systemName: "chevron.right")
                        .font(.caption2.weight(.bold))
                        .foregroundColor(.secondary)
                }
            }
            .buttonStyle(.plain)
            if tasks.isEmpty {
                Text(emptyText)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            } else {
                let visible = isExpanded ? tasks : Array(tasks.prefix(maxVisibleItems))
                ForEach(visible) { task in
                    let highlight = task.dueAt.map { $0 > now } == true
                    HStack(alignment: .top, spacing: 6) {
                        Text("•")
                            .font(.caption2)
                        Text(taskLineText(task))
                        .font(.caption2)
                        .lineLimit(1)
                    }
                    .padding(.horizontal, 6)
                    .padding(.vertical, 4)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(
                                highlight
                                ? AppColors.accentMystic(for: colorScheme).opacity(colorScheme == .dark ? 0.2 : 0.14)
                                : Color.clear
                            )
                    )
                }
                if tasks.count > maxVisibleItems {
                    Button {
                        isExpanded.toggle()
                    } label: {
                        let moreText = isExpanded
                            ? AppLocalized.text("home.productivity.show_less", fallback: "Show less")
                            : String(
                                format: AppLocalized.text("home.productivity.more_count", fallback: "+%d more"),
                                tasks.count - maxVisibleItems
                            )
                        Text(moreText)
                            .font(.caption2.weight(.semibold))
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func taskLineText(_ task: HomeViewModel.ProductivityTask) -> String {
        if let d = task.dueAt {
            return "\(task.title) · \(Self.dueFormatter.string(from: d))"
        }
        return task.title
    }
}

private struct HomeProductivityRow: View {
    let title: String
    let emptyText: String
    let onOpen: () -> Void
    let count: Int
    let items: [String]
    private let maxVisibleItems = 2
    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
            Button(action: onOpen) {
                HStack(spacing: SkydownLayout.stackSpacingDense) {
                    Text(title)
                        .font(.caption.weight(.semibold))
                    HomeCountBadge(count: count)
                    Image(systemName: "chevron.right")
                        .font(.caption2.weight(.bold))
                        .foregroundColor(.secondary)
                }
            }
            .buttonStyle(.plain)
            if items.isEmpty {
                Text(emptyText)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            } else {
                let visibleItems = isExpanded ? items : Array(items.prefix(maxVisibleItems))
                ForEach(Array(visibleItems.enumerated()), id: \.offset) { _, text in
                    Text("• \(text)")
                        .font(.caption2)
                        .lineLimit(1)
                }
                if items.count > maxVisibleItems {
                    Button {
                        isExpanded.toggle()
                    } label: {
                        Text(
                            isExpanded
                                ? AppLocalized.text("home.productivity.show_less", fallback: "Show less")
                                : String(
                                    format: AppLocalized.text(
                                        "home.productivity.more_count",
                                        fallback: "+%d more"
                                    ),
                                    items.count - maxVisibleItems
                                )
                        )
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

private struct HomeCountBadge: View {
    let count: Int

    var body: some View {
        Text("\(count)")
            .font(.caption2.weight(.bold))
            .foregroundColor(.secondary)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(
                Capsule(style: .continuous)
                    .fill(Color.secondary.opacity(0.14))
            )
    }
}

private struct HomeQuickActionButton: View {
    let title: String
    let systemImage: String?
    let badgeCount: Int?
    let colorScheme: ColorScheme
    let isLoading: Bool
    let isDisabled: Bool
    let onTap: () -> Void
    private var accessibilityLabelText: String {
        if let badgeCount {
            return "\(title), \(badgeCount)"
        }
        return title
    }

    init(
        title: String,
        systemImage: String? = nil,
        badgeCount: Int? = nil,
        colorScheme: ColorScheme,
        isLoading: Bool = false,
        isDisabled: Bool = false,
        onTap: @escaping () -> Void
    ) {
        self.title = title
        self.systemImage = systemImage
        self.badgeCount = badgeCount
        self.colorScheme = colorScheme
        self.isLoading = isLoading
        self.isDisabled = isDisabled
        self.onTap = onTap
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                if isLoading {
                    ProgressView()
                        .controlSize(.mini)
                }
                if let systemImage {
                    Image(systemName: systemImage)
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme).opacity(0.82))
                }
                Text(title)
                    .font(.caption2.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 7)
            .frame(minHeight: 44)
            .frame(maxWidth: .infinity)
            .background(AppColors.cardBackground(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
            .overlay(alignment: .topTrailing) {
                // Match productivity rows: show count including 0 when this chip carries metrics (owner shortcuts).
                if let badgeCount {
                    HomeCountBadge(count: badgeCount)
                        .offset(x: 4, y: -4)
                }
            }
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .contentShape(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabelText)
        .accessibilityAddTraits(.isButton)
        .disabled(isDisabled)
        .opacity(isDisabled ? 0.72 : 1)
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
