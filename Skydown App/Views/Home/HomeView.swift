import AVKit
import SwiftUI

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

struct HomeViewContent: View {
    @StateObject private var viewModel = HomeViewModel()
    @StateObject private var audioPlayerManager = AudioPlayerManager()
    @StateObject private var videoPlaybackManager = HomeInlineVideoPlaybackManager()
    @State private var sheetPresentation = SkydownQueuedPresentation<HomePresentedSheet>()
    @State private var fullscreenVideoTarget: HomeFullscreenVideoTarget?
    @State private var originalVideoViewerTarget: HomeOriginalVideoViewerTarget?
    @State private var hasLoadedInitialHomeContent = false
    @State private var isQuickActionCoolingDown = false
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

                                HomeProductivityOverviewSection(
                                    colorScheme: colorScheme,
                                    remindersToday: viewModel.dueTodayReminders,
                                    remindersUpcoming: viewModel.upcomingReminders,
                                    openTasks: viewModel.openTasks,
                                    recentNotes: viewModel.recentNotes,
                                    onOpenWorkflowWithPrompt: onOpenWorkflowWithPrompt,
                                    onOpenToday: {
                                        triggerQuickAction {
                                            if authManager.userSession == nil, let onGuestSignIn {
                                                onGuestSignIn()
                                            } else {
                                                presentSheet(.reminderManager)
                                            }
                                        }
                                    },
                                    onOpenUpcoming: {
                                        triggerQuickAction {
                                            if authManager.userSession == nil, let onGuestSignIn {
                                                onGuestSignIn()
                                            } else {
                                                presentSheet(.reminderManager)
                                            }
                                        }
                                    },
                                    onOpenTasks: {
                                        triggerQuickAction {
                                            if authManager.userSession == nil, let onGuestSignIn {
                                                onGuestSignIn()
                                            } else {
                                                presentSheet(.taskManager)
                                            }
                                        }
                                    },
                                    onOpenNotes: {
                                        triggerQuickAction {
                                            if authManager.userSession == nil, let onGuestSignIn {
                                                onGuestSignIn()
                                            } else {
                                                presentSheet(.noteManager)
                                            }
                                        }
                                    },
                                    onCreateReminder: {
                                        triggerQuickAction {
                                            if authManager.userSession == nil, let onGuestSignIn {
                                                onGuestSignIn()
                                            } else {
                                                presentSheet(.reminderComposer)
                                            }
                                        }
                                    },
                                    onCreateTask: {
                                        triggerQuickAction {
                                            if authManager.userSession == nil, let onGuestSignIn {
                                                onGuestSignIn()
                                            } else {
                                                presentSheet(.taskComposer)
                                            }
                                        }
                                    },
                                    onCreateNote: {
                                        triggerQuickAction {
                                            if authManager.userSession == nil, let onGuestSignIn {
                                                onGuestSignIn()
                                            } else {
                                                presentSheet(.noteComposer)
                                            }
                                        }
                                    }
                                )
                                .homeReveal(3)
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
                    Button(action: onEdit) {
                        Image(systemName: "pencil")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(AppColors.accent(for: colorScheme).opacity(0.92))
                            .frame(width: 36, height: 36)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .accessibilityLabel(AppLocalized.text("common.edit", fallback: "Edit"))

                    Button(action: onDelete) {
                        Image(systemName: "trash")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Color.red.opacity(0.72))
                            .frame(width: 36, height: 36)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .accessibilityLabel(AppLocalized.text("common.delete", fallback: "Delete"))
                }
            }

            if isEditing {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                    TextField(fieldPlaceholder, text: $draftTitle)
                        .textInputAutocapitalization(.sentences)
                        .padding(12)
                        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.55))
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
                    Button(action: onSave) {
                        Text(AppLocalized.text("common.save", fallback: "Save"))
                            .font(.caption.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
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
    @State private var editingID: String?
    @State private var draftTitle: String = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                if reminders.isEmpty {
                    Text(AppLocalized.text("home.manager.empty", fallback: "All clear."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 10)
                } else {
                    ForEach(reminders) { reminder in
                        let dueLine = reminder.dueAt.map {
                            DateFormatter.localizedString(from: $0, dateStyle: .short, timeStyle: .short)
                        }
                        HomeManageableItemRow(
                            colorScheme: colorScheme,
                            title: reminder.title,
                            subtitle: dueLine,
                            isEditing: editingID == reminder.id,
                            draftTitle: $draftTitle,
                            fieldPlaceholder: AppLocalized.text("home.manager.rename_placeholder", fallback: "New title"),
                            onEdit: {
                                withAnimation(SkydownMotion.statusTransition) {
                                    editingID = reminder.id
                                    draftTitle = reminder.title
                                }
                            },
                            onDelete: {
                                Task { try? await onDelete(reminder.id) }
                            },
                            onSave: {
                                let normalized = draftTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                                guard !normalized.isEmpty else { return }
                                Task {
                                    try? await onUpdateTitle(reminder.id, normalized)
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
        .navigationTitle(AppLocalized.text("home.manager.reminders.title", fallback: "Reminders"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct HomeTaskManagerSheet: View {
    let colorScheme: ColorScheme
    let tasks: [HomeViewModel.ProductivityTask]
    let onUpdateTitle: (_ id: String, _ title: String) async throws -> Void
    let onDelete: (_ id: String) async throws -> Void
    @State private var editingID: String?
    @State private var draftTitle: String = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                if tasks.isEmpty {
                    Text(AppLocalized.text("home.manager.empty", fallback: "All clear."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 10)
                } else {
                    ForEach(tasks) { task in
                        HomeManageableItemRow(
                            colorScheme: colorScheme,
                            title: task.title,
                            subtitle: nil,
                            isEditing: editingID == task.id,
                            draftTitle: $draftTitle,
                            fieldPlaceholder: AppLocalized.text("home.manager.rename_placeholder", fallback: "New title"),
                            onEdit: {
                                withAnimation(SkydownMotion.statusTransition) {
                                    editingID = task.id
                                    draftTitle = task.title
                                }
                            },
                            onDelete: {
                                Task { try? await onDelete(task.id) }
                            },
                            onSave: {
                                let normalized = draftTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                                guard !normalized.isEmpty else { return }
                                Task {
                                    try? await onUpdateTitle(task.id, normalized)
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
        .navigationTitle(AppLocalized.text("tasks.title", fallback: "Tasks"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct HomeNoteManagerSheet: View {
    let colorScheme: ColorScheme
    let notes: [HomeViewModel.ProductivityNote]
    let onUpdateTitle: (_ id: String, _ title: String) async throws -> Void
    let onDelete: (_ id: String) async throws -> Void
    @State private var editingID: String?
    @State private var draftTitle: String = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                if notes.isEmpty {
                    Text(AppLocalized.text("home.manager.empty", fallback: "All clear."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 10)
                } else {
                    ForEach(notes) { note in
                        let updatedLine = note.updatedAt.map {
                            DateFormatter.localizedString(from: $0, dateStyle: .short, timeStyle: .short)
                        }
                        HomeManageableItemRow(
                            colorScheme: colorScheme,
                            title: note.title,
                            subtitle: updatedLine,
                            isEditing: editingID == note.id,
                            draftTitle: $draftTitle,
                            fieldPlaceholder: AppLocalized.text("home.manager.rename_placeholder", fallback: "New title"),
                            onEdit: {
                                withAnimation(SkydownMotion.statusTransition) {
                                    editingID = note.id
                                    draftTitle = note.title
                                }
                            },
                            onDelete: {
                                Task { try? await onDelete(note.id) }
                            },
                            onSave: {
                                let normalized = draftTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                                guard !normalized.isEmpty else { return }
                                Task {
                                    try? await onUpdateTitle(note.id, normalized)
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
        .navigationTitle(AppLocalized.text("notes.title", fallback: "Notes"))
        .navigationBarTitleDisplayMode(.inline)
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
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                DatePicker(
                    AppLocalized.text("home.sheet.reminder.date_label", fallback: "Due at"),
                    selection: $dueAt,
                    displayedComponents: [.date, .hourAndMinute]
                )
                Button(AppLocalized.text("home.sheet.add", fallback: "Add")) {
                    let normalized = titleText.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !normalized.isEmpty else { return }
                    Task {
                        try? await onCreate(normalized, dueAt)
                        dismiss()
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(titleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding()
        }
        .navigationTitle(AppLocalized.text("home.quick.create_reminder", fallback: "Create Reminder"))
        .navigationBarTitleDisplayMode(.inline)
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
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                TextField(AppLocalized.text("tasks.input.details_hint", fallback: "Optional details"), text: $detailText)
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
                Button(AppLocalized.text("tasks.input.add", fallback: "Add task")) {
                    let title = titleText.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !title.isEmpty else { return }
                    Task {
                        let due: Date? = useDueAt ? dueAt : nil
                        try? await onCreate(title, detailText, due)
                        dismiss()
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(titleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding()
        }
        .navigationTitle(AppLocalized.text("home.quick.create_task", fallback: "Create Task"))
        .navigationBarTitleDisplayMode(.inline)
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
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                TextField(AppLocalized.text("notes.input.content_hint", fallback: "Write a quick note..."), text: $contentText, axis: .vertical)
                    .lineLimit(3...5)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                Button(AppLocalized.text("notes.input.add", fallback: "Add note")) {
                    let title = titleText.trimmingCharacters(in: .whitespacesAndNewlines)
                    let content = contentText.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !title.isEmpty || !content.isEmpty else { return }
                    Task {
                        try? await onCreate(title, content)
                        dismiss()
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(
                    titleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
                    contentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                )
            }
            .padding()
        }
        .navigationTitle(AppLocalized.text("home.quick.create_note", fallback: "Create Note"))
        .navigationBarTitleDisplayMode(.inline)
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

private struct HomeProductivityOverviewSection: View {
    let colorScheme: ColorScheme
    let remindersToday: [HomeViewModel.ProductivityReminder]
    let remindersUpcoming: [HomeViewModel.ProductivityReminder]
    let openTasks: [HomeViewModel.ProductivityTask]
    let recentNotes: [HomeViewModel.ProductivityNote]
    let onOpenWorkflowWithPrompt: ((String) -> Void)?
    let onOpenToday: () -> Void
    let onOpenUpcoming: () -> Void
    let onOpenTasks: () -> Void
    let onOpenNotes: () -> Void
    let onCreateReminder: () -> Void
    let onCreateTask: () -> Void
    let onCreateNote: () -> Void
    @State private var showsExtendedSignals = false

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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            Text(AppLocalized.text("home.productivity.ask_anything", fallback: "Ask SkyOS anything"))
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.55))

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
                } else {
                    Button {
                        showsExtendedSignals = true
                    } label: {
                        Text(
                            String(
                                format: AppLocalized.text(
                                    "home.productivity.collapse_summary",
                                    fallback: "%d more · R%02d · T%02d · N%02d"
                                ),
                                3,
                                reminderCount,
                                taskCount,
                                noteCount
                            )
                        )
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)
                }
            }

            if showsExtendedSignals {
                Button {
                    showsExtendedSignals = false
                } label: {
                    Text(AppLocalized.text("home.productivity.show_less_sections", fallback: "Show fewer sections"))
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(.secondary)
                }
                .buttonStyle(.plain)
            }

            Text(AppLocalized.text("home.productivity.quick_hint", fallback: "Tap a shortcut to open a calm capture sheet on Home — no tab switch."))
                .font(.caption2)
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.5))

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

            if let onOpenWorkflowWithPrompt {
                HomeOwnerWorkflowSection(
                    colorScheme: colorScheme,
                    reminderCount: reminderCount,
                    taskCount: taskCount,
                    noteCount: noteCount,
                    onOpenWorkflowWithPrompt: onOpenWorkflowWithPrompt
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

private struct HomeOwnerWorkflowSection: View {
    let colorScheme: ColorScheme
    let reminderCount: Int
    let taskCount: Int
    let noteCount: Int
    let onOpenWorkflowWithPrompt: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(AppLocalized.text("home.owner.workflows.title", fallback: "Owner shortcuts"))
                .font(.caption.weight(.semibold))
                .foregroundColor(.secondary)
            Text(AppLocalized.text(
                "home.owner.workflows.subtitle",
                fallback: "Opens AI → Agent with a seeded prompt. Tap compose to send, read the reply, then save in Tasks or Notes from the AI productivity dock—or add reminders from Home."
            ))
                .font(.caption2)
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.55))
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                HomeQuickActionButton(
                    title: AppLocalized.text("home.owner.workflows.plan", fallback: "Plan"),
                    badgeCount: taskCount,
                    colorScheme: colorScheme,
                    onTap: {
                        onOpenWorkflowWithPrompt("Review open tasks (\(taskCount)) and return a concise execution plan with priorities.")
                    }
                )
                HomeQuickActionButton(
                    title: AppLocalized.text("home.owner.workflows.followup", fallback: "Follow-up"),
                    badgeCount: reminderCount,
                    colorScheme: colorScheme,
                    onTap: {
                        onOpenWorkflowWithPrompt("Create follow-up actions from reminders (\(reminderCount)) and suggest what to do today first.")
                    }
                )
                HomeQuickActionButton(
                    title: AppLocalized.text("home.owner.workflows.summarize", fallback: "Summarize"),
                    badgeCount: noteCount,
                    colorScheme: colorScheme,
                    onTap: {
                        onOpenWorkflowWithPrompt("Summarize notes (\(noteCount)) into next actions and a short owner update.")
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
    let badgeCount: Int?
    let colorScheme: ColorScheme
    let onTap: () -> Void

    init(
        title: String,
        badgeCount: Int? = nil,
        colorScheme: ColorScheme,
        onTap: @escaping () -> Void
    ) {
        self.title = title
        self.badgeCount = badgeCount
        self.colorScheme = colorScheme
        self.onTap = onTap
    }

    var body: some View {
        Button(action: onTap) {
            Text(title)
                .font(.caption2.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)
                .padding(.horizontal, 8)
                .padding(.vertical, 7)
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
