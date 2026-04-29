import SwiftUI
import UIKit
import AVKit
import UniformTypeIdentifiers

enum AgentHomeProductivityTarget: String {
    case reminderManager = "reminder_manage"
    case taskManager = "task_manage"
    case noteManager = "note_manage"
}

struct AgentView: View {
    @StateObject private var viewModel: AgentChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @ObservedObject private var membershipCoordinator: AIMembershipCoordinator
    @ObservedObject private var aiRuntimeSettingsStore = AIRuntimeSettingsStore.shared
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ObservedObject private var taskStore = TaskStore.shared
    @ObservedObject private var noteStore = NoteStore.shared
    @State private var showingAttachmentImporter = false
    @State private var showingPromptComposer = false
    @State private var showingConversationSessions = false
    @State private var showingDeleteConversationDialog = false
    @State private var showingTaskSurface = false
    @State private var showingNoteSurface = false
    @State private var renameDraft = ""
    @State private var inputAttachments: [AgentInputAttachment] = []
    @State private var selectedNote: NoteItem?
    let prefilledPrompt: String?
    let onConsumePrefilledPrompt: (() -> Void)?
    let onOpenHomeProductivity: ((AgentHomeProductivityTarget) -> Void)?
    private let showsNavigation: Bool
    @State private var autoPresentedUpgradeHint = false

    init(
        agentChatService: AgentChatServicing = FirebaseFunctionsAgentService(),
        featureFlags: FeatureFlagsService,
        membershipCoordinator: AIMembershipCoordinator,
        prefilledPrompt: String? = nil,
        onConsumePrefilledPrompt: (() -> Void)? = nil,
        onOpenHomeProductivity: ((AgentHomeProductivityTarget) -> Void)? = nil,
        showsNavigation: Bool = true
    ) {
        self.showsNavigation = showsNavigation
        self.prefilledPrompt = prefilledPrompt
        self.onConsumePrefilledPrompt = onConsumePrefilledPrompt
        self.onOpenHomeProductivity = onOpenHomeProductivity
        _viewModel = StateObject(
            wrappedValue: AgentChatViewModel(service: agentChatService)
        )
        _featureFlags = ObservedObject(wrappedValue: featureFlags)
        _membershipCoordinator = ObservedObject(wrappedValue: membershipCoordinator)
    }

    var body: some View {
        let rootContent = showsNavigation ? AnyView(navigationContent) : AnyView(content)
        rootContent
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .fileImporter(
            isPresented: $showingAttachmentImporter,
            allowedContentTypes: [.item],
            allowsMultipleSelection: true
        ) { result in
            handleAttachmentImport(result)
        }
        .sheet(isPresented: $showingPromptComposer) {
            AgentPromptComposerSheet(
                colorScheme: colorScheme,
                draft: $viewModel.draft,
                selectedMode: $viewModel.selectedMode,
                selectedAutomationScope: $viewModel.selectedAutomationScope,
                shouldTriggerAutomation: $viewModel.shouldTriggerAutomation,
                socialInstagramEnabled: $viewModel.socialInstagramEnabled,
                socialInstagramHandle: $viewModel.socialInstagramHandle,
                socialTiktokEnabled: $viewModel.socialTiktokEnabled,
                socialTiktokHandle: $viewModel.socialTiktokHandle,
                socialYoutubeEnabled: $viewModel.socialYoutubeEnabled,
                socialYoutubeHandle: $viewModel.socialYoutubeHandle,
                socialFacebookEnabled: $viewModel.socialFacebookEnabled,
                socialFacebookHandle: $viewModel.socialFacebookHandle,
                socialSpotifyEnabled: $viewModel.socialSpotifyEnabled,
                socialSpotifyHandle: $viewModel.socialSpotifyHandle,
                shouldShowSocialSetupCard: viewModel.shouldShowSocialSetupCard,
                canTriggerAutomation: viewModel.canTriggerAutomation,
                canUseGlobalOwnerAutomationFlow: viewModel.canUseGlobalOwnerAutomationFlow,
                interactionPhase: viewModel.phase,
                attachments: inputAttachments,
                quickPrompts: viewModel.quickPrompts,
                onDismiss: {
                    showingPromptComposer = false
                },
                onAddFiles: {
                    showingPromptComposer = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                        showingAttachmentImporter = true
                    }
                },
                onResetSocialSetup: {
                    viewModel.resetSocialSetup()
                },
                onRemoveAttachment: removeAttachment,
                onClearAttachments: { inputAttachments.removeAll() },
                onSend: {
                    let urls = inputAttachments.compactMap { URL(string: $0.id) }
                    viewModel.sendDraft(attachmentURLs: urls)
                    inputAttachments.removeAll()
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $showingConversationSessions) {
            AIConversationSessionsSheet(
                title: AppLocalized.text("agent.chats.sheet.title", fallback: "Agent-Chats"),
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                sessions: viewModel.sessions,
                activeSessionID: viewModel.activeSessionID,
                isBusy: viewModel.phase.shouldBlockComposerChrome,
                renameDraft: $renameDraft,
                onSelectSession: { sessionID in
                    viewModel.openConversation(sessionID)
                    showingConversationSessions = false
                },
                onRenameActiveSession: {
                    viewModel.renameActiveConversation(renameDraft)
                },
                onDeleteActiveSession: {
                    showingDeleteConversationDialog = true
                }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $showingTaskSurface) {
            NavigationStack {
                ScrollView {
                    Button {
                        showingTaskSurface = false
                        viewModel.draft = AppLocalized.text(
                            "agent.prefill.create_task",
                            fallback: "Erstelle eine Aufgabe fuer "
                        )
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                            showingPromptComposer = true
                        }
                    } label: {
                        let taskTitle = AppLocalized.text("home.quick.create_task", fallback: "Aufgabe erstellen")
                        let taskHint = AppLocalized.text("home.quick.hint", fallback: "Tippe auf eine Aktion, um mit einem gefuehrten Prompt im Agent zu starten.")
                        let primaryTextColor = AppColors.text(for: colorScheme)
                        let secondaryTextColor = AppColors.secondaryText(for: colorScheme)
                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            Image(systemName: "wand.and.stars.inverse")
                                .font(.subheadline.weight(.bold))
                                .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                                Text(taskTitle)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundColor(primaryTextColor)
                                Text(taskHint)
                                    .font(.caption2)
                                    .foregroundColor(secondaryTextColor)
                                    .lineLimit(1)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.bold))
                                .foregroundColor(secondaryTextColor)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(AppColors.cardBackground(for: colorScheme))
                        .overlay(
                            RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.22), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.top, 8)

                    AgentTaskSectionCard(
                        colorScheme: colorScheme,
                        tasks: taskStore.tasks,
                        onRefresh: {
                            await taskStore.refresh()
                        },
                        onCreate: { title, details in
                            try await taskStore.create(title: title, details: details)
                            viewModel.showToastMessage(
                                AppLocalized.text("tasks.created", fallback: "Aufgabe erstellt"),
                                style: .success
                            )
                        },
                        onToggleStatus: { task in
                            if task.status == .completed {
                                try await taskStore.markOpen(taskID: task.id)
                            } else {
                                try await taskStore.markCompleted(taskID: task.id)
                                viewModel.showToastMessage(
                                    AppLocalized.text("tasks.completed", fallback: "Aufgabe erledigt"),
                                    style: .success
                                )
                            }
                        },
                        onDelete: { task in
                            try await taskStore.delete(taskID: task.id)
                        }
                    )
                    .padding(SkydownLayout.screenHorizontalPadding)
                    .padding(.top, 8)
                }
                .background(backgroundGradient.ignoresSafeArea())
                .navigationTitle(AppLocalized.text("tasks.title", fallback: "Aufgaben"))
                .navigationBarTitleDisplayMode(.inline)
                .skydownNavigationChrome(colorScheme: colorScheme)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        SkydownBrandActionButton(
                            title: AppLocalized.text("common.done", fallback: "Done"),
                            accent: AppColors.accentMystic(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .subheadline.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: false,
                            action: { showingTaskSurface = false }
                        )
                        .skydownInteractiveFeedback()
                    }
                }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $showingNoteSurface) {
            NavigationStack {
                ScrollView {
                    Button {
                        showingNoteSurface = false
                        viewModel.draft = AppLocalized.text(
                            "agent.prefill.create_note",
                            fallback: "Erstelle eine Notiz zu "
                        )
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                            showingPromptComposer = true
                        }
                    } label: {
                        let noteTitle = AppLocalized.text("home.quick.create_note", fallback: "Notiz erstellen")
                        let noteHint = AppLocalized.text("home.quick.hint", fallback: "Tippe auf eine Aktion, um mit einem gefuehrten Prompt im Agent zu starten.")
                        let primaryTextColor = AppColors.text(for: colorScheme)
                        let secondaryTextColor = AppColors.secondaryText(for: colorScheme)
                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            Image(systemName: "sparkles")
                                .font(.subheadline.weight(.bold))
                                .foregroundColor(AppColors.accentMystic(for: colorScheme))
                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                                Text(noteTitle)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundColor(primaryTextColor)
                                Text(noteHint)
                                    .font(.caption2)
                                    .foregroundColor(secondaryTextColor)
                                    .lineLimit(1)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.bold))
                                .foregroundColor(secondaryTextColor)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(AppColors.cardBackground(for: colorScheme))
                        .overlay(
                            RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.22), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.top, 8)

                    AgentNoteSectionCard(
                        colorScheme: colorScheme,
                        notes: noteStore.notes,
                        onRefresh: {
                            await noteStore.refresh()
                        },
                        onCreate: { title, content in
                            try await noteStore.create(title: title, content: content)
                            viewModel.showToastMessage(
                                AppLocalized.text("notes.saved", fallback: "Notiz gespeichert"),
                                style: .success
                            )
                        },
                        onOpenNote: { note in
                            selectedNote = note
                        },
                        onDelete: { note in
                            try await noteStore.delete(noteID: note.id)
                        }
                    )
                    .padding(SkydownLayout.screenHorizontalPadding)
                    .padding(.top, 8)
                }
                .background(backgroundGradient.ignoresSafeArea())
                .navigationTitle(AppLocalized.text("notes.title", fallback: "Notizen"))
                .navigationBarTitleDisplayMode(.inline)
                .skydownNavigationChrome(colorScheme: colorScheme)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        SkydownBrandActionButton(
                            title: AppLocalized.text("common.done", fallback: "Done"),
                            accent: AppColors.accentMystic(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .subheadline.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: false,
                            action: { showingNoteSurface = false }
                        )
                        .skydownInteractiveFeedback()
                    }
                }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
        .sheet(item: $selectedNote) { note in
            AgentNoteDetailSheet(
                colorScheme: colorScheme,
                note: note,
                onSave: { updatedTitle, updatedContent in
                    try await noteStore.update(noteID: note.id, title: updatedTitle, content: updatedContent)
                    viewModel.showToastMessage(
                        AppLocalized.text("notes.saved", fallback: "Notiz gespeichert"),
                        style: .success
                    )
                },
                onDelete: {
                    try await noteStore.delete(noteID: note.id)
                    viewModel.showToastMessage(
                        AppLocalized.text("notes.saved", fallback: "Notiz gespeichert"),
                        style: .success
                    )
                    selectedNote = nil
                }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
        .confirmationDialog(
            AppLocalized.text("agent.delete_chat.title", fallback: "Aktiven Chat loeschen?"),
            isPresented: $showingDeleteConversationDialog,
            titleVisibility: .visible
        ) {
            Button(AppLocalized.text("agent.delete_chat.confirm", fallback: "Loeschen"), role: .destructive) {
                viewModel.deleteActiveConversation()
                showingConversationSessions = false
            }
            Button(AppLocalized.text("common.cancel", fallback: "Abbrechen"), role: .cancel) { }
        }
        .task(id: sessionObservationKey) {
            viewModel.configureUser(user: authManager.userSession)
            await Task.yield()
            guard !Task.isCancelled else { return }
            configureIntegrationObservations(for: authManager.userSession)
            taskStore.observeTasks(for: authManager.userSession?.id)
            noteStore.observeNotes(for: authManager.userSession?.id)
        }
        .onChange(of: authManager.userSession?.id) { _, userID in
            taskStore.observeTasks(for: userID)
            noteStore.observeNotes(for: userID)
        }
        .onChange(of: featureFlags.isAIEnabled) { _, isEnabled in
            aiRuntimeSettingsStore.setObservationEnabled(isEnabled)
        }
        .onChange(of: viewModel.revenueUsage?.warningLevel) { _, level in
            handleCriticalUsageWarning(level)
        }
        .onChange(of: viewModel.lastIntegrationIssue) { _, message in
            guard shouldPromptUpgrade(for: message) else { return }
            membershipCoordinator.openMembership(reason: .featureLocked, surface: "agent_chat")
        }
        .onAppear {
            renameDraft = viewModel.activeSessionTitle
        }
        .onChange(of: viewModel.activeSessionID) { _, _ in
            renameDraft = viewModel.activeSessionTitle
        }
        .onChange(of: viewModel.activeSessionTitle) { _, title in
            renameDraft = title
        }
        .task(id: prefilledPrompt) {
            guard let prompt = prefilledPrompt else { return }
            let trimmed = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty else { return }
            viewModel.draft = prompt
            // Prefill (Home owner shortcuts, Owner hub, deep links): open the composer immediately
            // so it feels as direct as the bot’s inline field — FAB stays for unprompted sessions.
            showingPromptComposer = true
            onConsumePrefilledPrompt?()
        }
    }

    private func handleCriticalUsageWarning(_ level: String?) {
        guard !autoPresentedUpgradeHint else { return }
        guard level == "critical" else { return }
        autoPresentedUpgradeHint = true
        membershipCoordinator.openMembership(reason: .criticalUsage, surface: "agent_chat")
    }

    private func shouldPromptUpgrade(for message: String?) -> Bool {
        guard let message else { return false }
        let lowered = message.lowercased()
        return lowered.contains("abo")
            || lowered.contains("membership")
            || lowered.contains("pro oder creator")
            || lowered.contains("subscription")
            || lowered.contains("upgrade")
            || lowered.contains("plan")
    }

    private var sessionObservationKey: String {
        let session = authManager.userSession
        return [
            session?.id ?? "guest",
            session?.resolvedQuotaPlan.rawValue ?? UserQuotaPlan.free.rawValue,
            String(session?.resolvedAIHistoryRetentionDays ?? UserRole.user.defaultAIHistoryRetentionDays),
            session?.normalizedAISubscriptionProvider ?? "none",
            String(session?.aiAccessEnabled ?? false)
        ].joined(separator: "|")
    }

    private var usesCompactImmersiveLayout: Bool {
        !showsNavigation
    }

    private var composerReservedBottomSpace: CGFloat {
        guard featureFlags.isAIEnabled else { return 0 }
        if viewModel.messages.isEmpty {
            return 86
        }
        return 160
    }

    private var content: some View {
        let activeSessionSummary = viewModel.sessions.first { $0.id == viewModel.activeSessionID }
        let activeSessionSubtitle = {
            if let activeSessionSummary {
                if !activeSessionSummary.preview.isEmpty {
                    return activeSessionSummary.preview
                }
                    return activeSessionSummary.promptCount == 0
                        ? AppLocalized.text("agent.session.subtitle.empty", fallback: "Leer")
                        : String(
                            format: AppLocalized.text("agent.session.subtitle.request_count", fallback: "%d Anfragen"),
                            activeSessionSummary.promptCount
                        )
            }
            return AppLocalized.text("agent.session.subtitle.empty", fallback: "Leer")
        }()
        let pinnedSessionStrip = AIConversationSessionStrip(
            title: viewModel.activeSessionTitle,
            subtitle: activeSessionSubtitle,
            accent: AppColors.accentMystic(for: colorScheme),
            colorScheme: colorScheme,
            isBusy: viewModel.phase.shouldBlockComposerChrome,
            canDelete: viewModel.activeSessionID != nil,
            showsManagementActions: true,
            onOpenSessions: { showingConversationSessions = true },
            onRefreshChat: viewModel.refreshActiveConversation,
            onDeleteChat: { showingDeleteConversationDialog = true }
        )

        return ZStack(alignment: .bottom) {
            AgentAccessibilityMarker(
                identifier: "agent.screen.root",
                label: AppLocalized.text("agent.a11y.screen", fallback: "Agent-Bildschirm")
            )

            if !viewModel.lastAgentRunId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                AgentAccessibilityMarker(
                    identifier: "agent.lastRun.id",
                    label: viewModel.lastAgentRunId
                )
            }

            VStack(spacing: SkydownLayout.stackSpacingNone) {
                if featureFlags.isAIEnabled {
                    pinnedSessionStrip
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        .padding(.top, showsNavigation ? 12 : 8)
                        .padding(.bottom, 6)
                        .background(.ultraThinMaterial)
                        .zIndex(2)

                    AgentTheaterPhaseStrip(
                        phase: viewModel.phase,
                        colorScheme: colorScheme
                    )
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.bottom, 8)

                    AgentProductivityDockCard(
                        colorScheme: colorScheme,
                        openTaskCount: taskStore.tasks.filter { $0.status != .completed }.count,
                        noteCount: noteStore.notes.count,
                        onOpenTasks: { showingTaskSurface = true },
                        onOpenNotes: { showingNoteSurface = true }
                    )
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.bottom, 8)

                    if let errorMessage = Optional(viewModel.lastIntegrationIssue),
                       !errorMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        AgentInlineErrorCard(
                            message: errorMessage,
                            showUpgradeAction: shouldPromptUpgrade(for: errorMessage),
                            colorScheme: colorScheme,
                            onRetry: { viewModel.refreshActiveConversation() },
                            onUpgrade: {
                                membershipCoordinator.openMembership(reason: .featureLocked, surface: "agent_chat")
                            }
                        )
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        .padding(.bottom, 8)
                    }

                    if viewModel.messages.isEmpty {
                        Spacer(minLength: 0)
                    } else {
                        ScrollViewReader { proxy in
                            let scrollToken = viewModel.messages.last.map { message in
                                let workflowToken = message.workflowSummary.map {
                                    [
                                        $0.workflowName,
                                        $0.statusText,
                                        $0.runID ?? "no-run-id"
                                    ].joined(separator: "|")
                                } ?? "no-workflow"
                                return [
                                    message.id.uuidString,
                                    message.isStreaming.description,
                                    message.resultType.rawValue,
                                    message.text,
                                    workflowToken,
                                    message.results.map { result in
                                        [
                                            result.id,
                                            result.type,
                                            result.url,
                                            result.title,
                                            result.text
                                        ].joined(separator: ":")
                                    }.joined(separator: "|")
                                ].joined(separator: "|")
                            } ?? "agent-chat-empty"

                            ScrollView {
                                LazyVStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                                        ForEach(viewModel.messages) { message in
                                            AgentMessageBubble(
                                                message: message,
                                                colorScheme: colorScheme,
                                                onOpenHomeProductivity: { target in
                                                    onOpenHomeProductivity?(target)
                                                },
                                                onContinueInMode: { mode in
                                                    viewModel.continueInModeFromAssistant(mode, sourceMessage: message)
                                                    showingPromptComposer = true
                                                }
                                            )
                                            .id(message.id)
                                        }
                                    }
                                    .padding(.top, 2)

                                    Color.clear
                                        .frame(height: 4)
                                        .id("agent-chat-end")
                                }
                                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                                .padding(.top, 2)
                                .padding(.bottom, 6)
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .scrollIndicators(.hidden)
                            .scrollDismissesKeyboard(.interactively)
                            .onAppear {
                                DispatchQueue.main.async {
                                    if reduceMotion {
                                        var transaction = Transaction()
                                        transaction.disablesAnimations = true
                                        withTransaction(transaction) {
                                            proxy.scrollTo("agent-chat-end", anchor: .bottom)
                                        }
                                    } else {
                                        proxy.scrollTo("agent-chat-end", anchor: .bottom)
                                    }
                                }
                            }
                            .onChange(of: scrollToken) { _, _ in
                                if reduceMotion {
                                    var transaction = Transaction()
                                    transaction.disablesAnimations = true
                                    withTransaction(transaction) {
                                        proxy.scrollTo("agent-chat-end", anchor: .bottom)
                                    }
                                } else {
                                    withAnimation(.easeOut(duration: 0.25)) {
                                        proxy.scrollTo("agent-chat-end", anchor: .bottom)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    VStack {
                        Spacer(minLength: 24)
                        AgentDisabledCard(colorScheme: colorScheme)
                            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        Spacer()
                    }
                }
            }
            .safeAreaInset(edge: .bottom, spacing: SkydownLayout.stackSpacingNone) {
                Color.clear
                    .frame(height: composerReservedBottomSpace)
                    .allowsHitTesting(false)
            }

            if featureFlags.isAIEnabled {
                VStack(alignment: .trailing, spacing: 10) {
                    if !viewModel.messages.isEmpty {
                        AgentThreadFollowUpBar(
                            colorScheme: colorScheme,
                            draft: $viewModel.draft,
                            isWorking: viewModel.phase.shouldBlockComposerChrome,
                            onSend: {
                                viewModel.sendDraft(attachmentURLs: [])
                            },
                            onOpenFullComposer: { showingPromptComposer = true },
                            canSend: { viewModel.isCurrentDraftSendable }
                        )
                    }
                    HStack {
                        Spacer(minLength: 0)
                        AgentPromptFab(
                            isWorking: viewModel.phase.shouldBlockComposerChrome,
                            onOpen: { showingPromptComposer = true }
                        )
                    }
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.bottom, max(18, keyWindowSafeAreaBottomInset + 14))
                .zIndex(1)
            }
        }
        .ignoresSafeArea(.keyboard, edges: .bottom)
        .skydownDismissKeyboardOnTap()
        .skydownKeyboardDismissToolbar()
        .background(backgroundGradient.ignoresSafeArea())
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private var keyWindowSafeAreaBottomInset: CGFloat {
        let windowScenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let foregroundScene = windowScenes.first {
            $0.activationState == .foregroundActive || $0.activationState == .foregroundInactive
        }
        let keyWindow = foregroundScene?.windows.first(where: \.isKeyWindow) ?? foregroundScene?.windows.first
        return keyWindow?.safeAreaInsets.bottom ?? 0
    }

    private var backgroundGradient: LinearGradient {
        AppColors.screenGradient(
            for: colorScheme,
            secondaryAccent: AppColors.accentMystic(for: colorScheme)
        )
    }

    private func handleAttachmentImport(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            let attachments = urls.map(AgentInputAttachment.init)
            guard !attachments.isEmpty else { return }

            inputAttachments = (inputAttachments + attachments)
                .reduce(into: [AgentInputAttachment]()) { partialResult, attachment in
                    guard !partialResult.contains(where: { $0.id == attachment.id }) else { return }
                    partialResult.append(attachment)
                }
                .suffix(12)
                .map { $0 }
            viewModel.showToastMessage("Dateien hinzugefuegt.", style: .success)
        case .failure(let error):
            viewModel.showToastMessage("Dateien konnten nicht geladen werden: \(error.localizedDescription)", style: .error)
        }
    }

    private func removeAttachment(_ attachment: AgentInputAttachment) {
        inputAttachments.removeAll { $0.id == attachment.id }
    }

    private func configureIntegrationObservations(for user: User?) {
        _ = user
        aiRuntimeSettingsStore.setObservationEnabled(featureFlags.isAIEnabled)
    }
}

private extension AgentView {
    var navigationContent: some View {
        NavigationStack {
            content
                .navigationTitle(AppLocalized.text("agent.nav.title", fallback: "Agent"))
                .skydownNavigationChrome(colorScheme: colorScheme)
        }
    }
}

private struct AgentPlanPreviewCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(AppLocalized.text("ai.membership.plans.title_short", fallback: "AI-Mitgliedschaft"))
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.accentMystic(for: colorScheme))
            Text(AppLocalized.text("ai.membership.plans.tiers", fallback: "Free, Pro, Creator"))
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(AppLocalized.text("agent.membership.caption", fallback: "Creator schaltet Workflows und Premium-Output frei."))
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct AgentRevenueUsageCard: View {
    let usage: AgentChatViewModel.RevenueUsageState
    let colorScheme: ColorScheme

    private var progress: Double {
        guard usage.limit > 0 else { return 0 }
        return max(0, min(1, Double(usage.limit - usage.remaining) / Double(usage.limit)))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            HStack {
                Text("\(AppLocalized.text("ai.membership.current_plan", fallback: "Plan")): \(usage.planTitle)")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                Spacer()
                Text("\(usage.remaining)/\(usage.limit) \(AppLocalized.text("ai.membership.open", fallback: "offen"))")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
            ProgressView(value: progress)
                .tint(usage.warningLevel == "critical" ? .red : AppColors.accentHighlight(for: colorScheme))
            if usage.warningLevel != "ok" {
                Text(
                    usage.warningLevel == "critical"
                        ? AppLocalized.text("agent.usage.warning.critical", fallback: "Du bist nah am Limit. Ein Upgrade haelt Workflows stabil.")
                        : AppLocalized.text("agent.usage.warning.high", fallback: "Hohe Auslastung erkannt. Alles bleibt stabil, wird aber beobachtet.")
                )
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
            if !usage.userFacingReason.isEmpty {
                CalmUpgradeCard(reason: usage.userFacingReason, colorScheme: colorScheme)
            }
            if !usage.lowerCostOption.isEmpty {
                LowerCostOptionCard(option: usage.lowerCostOption, colorScheme: colorScheme)
            }
            if usage.retryAfterSeconds > 0 {
                RetryLaterCard(retryAfterSeconds: usage.retryAfterSeconds, colorScheme: colorScheme)
            }
            if !usage.suggestedUpgrade.isEmpty {
                Text("\(AppLocalized.text("ai.membership.next_step", fallback: "Naechster Schritt")): \(usage.suggestedUpgrade.uppercased())")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
            }
            if !usage.resetHint.isEmpty {
                Text(usage.resetHint)
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.15), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct CalmUpgradeCard: View {
    let reason: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(reason)
            .font(.caption2.weight(.semibold))
            .foregroundColor(AppColors.secondaryText(for: colorScheme))
            .padding(.horizontal, 9)
            .padding(.vertical, 7)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.accent(for: colorScheme).opacity(0.06))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct LowerCostOptionCard: View {
    let option: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(option)
            .font(.caption2)
            .foregroundColor(AppColors.secondaryText(for: colorScheme))
            .padding(.horizontal, 9)
            .padding(.vertical, 7)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.accentHighlight(for: colorScheme).opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct RetryLaterCard: View {
    let retryAfterSeconds: Int
    let colorScheme: ColorScheme

    var body: some View {
        Text("\(AppLocalized.text("ai.retry_in", fallback: "Bitte versuche es in etwa erneut in")) \(retryAfterSeconds)s.")
            .font(.caption2)
            .foregroundColor(AppColors.secondaryText(for: colorScheme))
            .padding(.horizontal, 9)
            .padding(.vertical, 7)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.72))
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

struct AgentMembershipSheet: View {
    let colorScheme: ColorScheme
    let isLoadingProducts: Bool
    let isSyncing: Bool
    let activePurchasePlan: UserQuotaPlan?
    let onSelectPlan: (UserQuotaPlan) -> Void
    let onRestore: () -> Void
    let onManage: () -> Void

    @Environment(\.dismiss) private var dismiss

    private var planActionsEnabled: Bool {
        !(isLoadingProducts || isSyncing)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                Text(AppLocalized.text("agent.membership.sheet.subtitle", fallback: "Upgrade als Fortschritt, nie als Druck."))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                AgentPlanTile(title: AppLocalized.text("membership.plan.free", fallback: "Free"), detail: AppLocalized.text("ai.membership.free_detail", fallback: "Kern-Bot, weniger Bilder, leichter Agent"), colorScheme: colorScheme)
                AgentPlanTile(title: AppLocalized.text("membership.plan.pro", fallback: "Pro"), detail: AppLocalized.text("ai.membership.pro_detail", fallback: "Mehr Reichweite, staerkerer Agent, Creator-Tagesflow"), colorScheme: colorScheme)
                AgentPlanTile(title: AppLocalized.text("membership.plan.creator", fallback: "Creator"), detail: AppLocalized.text("ai.membership.creator_detail", fallback: "Workflow-Tiefe, Premium-Outputs, Prioritaet"), colorScheme: colorScheme)
                Text(AppLocalized.text("ai.membership.annual_coming", fallback: "Eine Jahresoption folgt als naechstes im nativen Billing."))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                SkydownBrandActionButton(
                    title: activePurchasePlan == .creator
                        ? AppLocalized.text("membership.pro.starting", fallback: "Pro wird gestartet...")
                        : AppLocalized.text("membership.pro.activate", fallback: "Pro aktivieren"),
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    isEnabled: planActionsEnabled,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 11,
                    action: { onSelectPlan(.creator) }
                )
                SkydownBrandActionButton(
                    title: activePurchasePlan == .studio
                        ? AppLocalized.text("membership.creator.starting", fallback: "Creator wird gestartet...")
                        : AppLocalized.text("membership.creator.activate", fallback: "Creator aktivieren"),
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    isEnabled: planActionsEnabled,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 11,
                    action: { onSelectPlan(.studio) }
                )
                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("membership.restore", fallback: "Kaeufe wiederherstellen"),
                        accent: AppColors.accentMystic(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: true,
                        action: onRestore
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                    SkydownBrandActionButton(
                        title: AppLocalized.text("membership.manage", fallback: "Mitgliedschaft verwalten"),
                        accent: AppColors.accentMystic(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: onManage
                    )
                }
            }
            .padding()
            }
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
            .navigationTitle(AppLocalized.text("ai.membership.sheet.title", fallback: "SkyOS AI-Mitgliedschaft"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.done", fallback: "Done"),
                        accent: AppColors.accentMystic(for: colorScheme),
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

private struct AgentPlanTile: View {
    let title: String
    let detail: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
            Text(title)
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(detail)
                .font(.caption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct AgentEmptyStateHeader: View {
    let colorScheme: ColorScheme
    var isCompact: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: isCompact ? 6 : 8) {
            Text(AppLocalized.text("agent.empty.header.title", fallback: "Answers in the chat. Saves beside them."))
                .font(
                    isCompact
                        ? .system(size: 24, weight: .black, design: .rounded)
                        : .system(size: 28, weight: .black, design: .rounded)
                )
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("agent.empty.header.subtitle", fallback: "Store follow-ups in Tasks or Notes from the productivity dock. Reminders with push work from the agent or Home quick actions."))
                .font(isCompact ? .subheadline.weight(.semibold) : .subheadline.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(AppLocalized.text("agent.memory.retention.hint", fallback: "Memory Layer · 30 Tage pro Konto."))
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.accentMystic(for: colorScheme).opacity(0.9))

            if !isCompact {
                Text(
                    AppLocalized.text(
                        "agent.empty.hint.continuation",
                        fallback: "The agent also continues the thread per account so briefings and to-dos stay connected."
                    )
                )
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme).opacity(0.9))

                Text(AppLocalized.text("agent.empty.first_step", fallback: "Type below or pick a quick prompt. The agent keeps the run structured."))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }
        }
    }
}

private struct AgentStatusChip: View {
    let text: String
    let accent: Color
    let colorScheme: ColorScheme

    var body: some View {
        Text(text)
            .font(.caption2.weight(.black))
            .foregroundColor(accent)
            .lineLimit(1)
            .minimumScaleFactor(0.82)
            .padding(.horizontal, 9)
            .padding(.vertical, 6)
            .background(
                Capsule(style: .continuous)
                    .fill(accent.opacity(colorScheme == .dark ? 0.16 : 0.1))
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(accent.opacity(0.16), lineWidth: 1)
            )
    }
}

private struct AgentTheaterPhaseStrip: View {
    let phase: AgentInteractionPhase
    let colorScheme: ColorScheme

    private var stageTitle: String {
        phase.theaterStageTitle
    }

    private var stageDetail: String {
        phase.composerStatusLabel
            ?? AppLocalized.text("agent.session.ready_for_intent", fallback: "Ready for your next intent.")
    }

    private var accent: Color {
        switch phase {
        case .idle, .awaitingConfirmation, .awaitingExternalAuth, .waitingReconnect:
            return AppColors.accent(for: colorScheme)
        case .planning, .webhookPending, .externalRunning, .executing, .toolPending, .ownerDiagnostic:
            return AppColors.accentMystic(for: colorScheme)
        case .externalFailed, .externalCompleted, .fallbackInternal, .completed, .partial, .blocked, .failed, .retryable, .cancelled:
            return AppColors.accentHighlight(for: colorScheme)
        }
    }

    var body: some View {
        HStack(spacing: 8) {
            Text(stageTitle.uppercased())
                .font(.caption2.weight(.black))
                .foregroundColor(accent)

            Text("•")
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.55))

            Text(stageDetail)
                .font(.caption2.weight(.semibold))
                .lineLimit(1)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(accent.opacity(colorScheme == .dark ? 0.16 : 0.12))
        .overlay(
            Capsule(style: .continuous)
                .stroke(accent.opacity(0.24), lineWidth: 1)
        )
        .clipShape(Capsule(style: .continuous))
    }
}

private extension AgentInteractionPhase {
    var theaterStageTitle: String {
        switch self {
        case .idle, .awaitingConfirmation, .awaitingExternalAuth, .waitingReconnect:
            return AppLocalized.text("agent.phase.intent", fallback: "Intent")
        case .planning, .webhookPending, .externalRunning, .executing, .toolPending, .ownerDiagnostic:
            return AppLocalized.text("agent.phase.execution", fallback: "Execution")
        case .externalFailed, .externalCompleted, .fallbackInternal, .completed, .partial, .blocked, .failed, .retryable, .cancelled:
            return AppLocalized.text("agent.phase.resolution", fallback: "Resolution")
        }
    }
}

private struct AgentProductivityDockCard: View {
    let colorScheme: ColorScheme
    let openTaskCount: Int
    let noteCount: Int
    let onOpenTasks: () -> Void
    let onOpenNotes: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            Text(AppLocalized.text("agent.productivity.header", fallback: "Produktivitaetsbereiche"))
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))

            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Button(action: onOpenTasks) {
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                        Text(AppLocalized.text("tasks.title", fallback: "Aufgaben"))
                            .font(.caption.weight(.semibold))
                        Text(String(openTaskCount))
                            .font(.title3.weight(.black))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous))
                }
                .buttonStyle(.plain)

                Button(action: onOpenNotes) {
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                        Text(AppLocalized.text("notes.title", fallback: "Notizen"))
                            .font(.caption.weight(.semibold))
                        Text(String(noteCount))
                            .font(.title3.weight(.black))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(10)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct AgentFeatureStatusCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(AppLocalized.text("agent.feature_status.live_title", fallback: "HEUTE LIVE"))
                .font(.caption2.weight(.black))
                .foregroundColor(AppColors.accentMystic(for: colorScheme))
            Text(AppLocalized.text("agent.feature_status.live_body", fallback: "Erinnerungen + Push, Aufgaben + Notizen"))
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(AppLocalized.text("agent.feature_status.next_title", fallback: "ALS NAECHSTES"))
                .font(.caption2.weight(.black))
                .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                .padding(.top, 2)
            Text(AppLocalized.text("agent.feature_status.next_body", fallback: "Profilspeicher & Follow-ups"))
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct AgentTaskSectionCard: View {
    let colorScheme: ColorScheme
    let tasks: [TaskItem]
    let onRefresh: () async -> Void
    let onCreate: (_ title: String, _ details: String) async throws -> Void
    let onToggleStatus: (TaskItem) async throws -> Void
    let onDelete: (TaskItem) async throws -> Void
    @State private var createTitle: String = ""
    @State private var createDetails: String = ""

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter
    }()

    private var canAddTask: Bool {
        !createTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            HStack {
                Text(AppLocalized.text("tasks.title", fallback: "Aufgaben"))
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer()
                Button {
                    Task { await onRefresh() }
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .font(.caption.weight(.bold))
                }
                .buttonStyle(.plain)
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                Text(AppLocalized.text("tasks.create_with_ai", fallback: "Aufgaben mit AI erstellen"))
                    .font(.caption2.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                TextField(
                    AppLocalized.text("tasks.input.title_hint", fallback: "Aufgabentitel (z. B. Rechnung senden)"),
                    text: $createTitle
                )
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(AppColors.cardBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))

                TextField(
                    AppLocalized.text("tasks.input.details_hint", fallback: "Optionale Details"),
                    text: $createDetails
                )
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(AppColors.cardBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))

                SkydownBrandActionButton(
                    title: AppLocalized.text("tasks.input.add", fallback: "Aufgabe hinzufuegen"),
                    systemImage: "plus.circle.fill",
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    isEnabled: canAddTask,
                    font: .caption.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 9,
                    action: {
                        let title = createTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                        let details = createDetails.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !title.isEmpty else { return }
                        Task {
                            try? await onCreate(title, details)
                            createTitle = ""
                            createDetails = ""
                        }
                    }
                )
            }

            if tasks.isEmpty {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(AppLocalized.text("tasks.empty", fallback: "Noch keine Aufgaben"))
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    Text(AppLocalized.text("tasks.create_with_ai", fallback: "Aufgaben mit AI erstellen"))
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else {
                List {
                    ForEach(tasks.prefix(8)) { task in
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                                Circle()
                                    .fill(task.status == .completed ? AppColors.accentMystic(for: colorScheme) : AppColors.accentHighlight(for: colorScheme))
                                    .frame(width: 8, height: 8)
                                Text(task.title)
                                    .font(.footnote.weight(.semibold))
                                    .foregroundColor(AppColors.text(for: colorScheme))
                                Spacer()
                                Text(task.priority.localizedLabel.uppercased())
                                    .font(.caption2.weight(.bold))
                                    .foregroundColor(AppColors.accent(for: colorScheme))
                            }

                            if !task.description.isEmpty {
                                Text(task.description)
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    .lineLimit(2)
                            }

                            HStack(spacing: SkydownLayout.stackSpacingPill) {
                                if let dueAt = task.dueAt {
                                    Text(Self.dateFormatter.string(from: dueAt))
                                        .font(.caption2)
                                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                }
                                Text(task.status.rawValue.capitalized)
                                    .font(.caption2.weight(.bold))
                                    .foregroundColor(task.status == .completed ? AppColors.accentMystic(for: colorScheme) : AppColors.accentHighlight(for: colorScheme))
                            }
                        }
                        .listRowInsets(EdgeInsets(top: 6, leading: 2, bottom: 6, trailing: 2))
                        .listRowBackground(Color.clear)
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            Button(
                                task.status == .completed
                                    ? AppLocalized.text("tasks.action.open", fallback: "Offen")
                                    : AppLocalized.text("tasks.action.complete", fallback: "Erledigen")
                            ) {
                                Task { try? await onToggleStatus(task) }
                            }
                            .tint(.green)
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(AppLocalized.text("common.delete", fallback: "Loeschen"), role: .destructive) {
                                Task { try? await onDelete(task) }
                            }
                        }
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .refreshable {
                    await onRefresh()
                }
                .frame(minHeight: 120, maxHeight: 280)
            }
        }
        .padding(10)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct AgentNoteSectionCard: View {
    let colorScheme: ColorScheme
    let notes: [NoteItem]
    let onRefresh: () async -> Void
    let onCreate: (_ title: String, _ content: String) async throws -> Void
    let onOpenNote: (NoteItem) -> Void
    let onDelete: (NoteItem) async throws -> Void
    @State private var searchText: String = ""
    @State private var createTitle: String = ""
    @State private var createContent: String = ""

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter
    }()

    private var filteredNotes: [NoteItem] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !query.isEmpty else { return notes }
        return notes.filter {
            $0.title.lowercased().contains(query) || $0.content.lowercased().contains(query)
        }
    }

    private var canAddNote: Bool {
        let title = createTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let content = createContent.trimmingCharacters(in: .whitespacesAndNewlines)
        return !title.isEmpty || !content.isEmpty
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            HStack {
                Text(AppLocalized.text("notes.title", fallback: "Notizen"))
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer()
                Button {
                    Task { await onRefresh() }
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .font(.caption.weight(.bold))
                }
                .buttonStyle(.plain)
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                Text(AppLocalized.text("notes.create_with_ai", fallback: "Notizen mit AI erstellen"))
                    .font(.caption2.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                TextField(
                    AppLocalized.text("notes.input.title_hint", fallback: "Notiztitel (z. B. Projektideen)"),
                    text: $createTitle
                )
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(AppColors.cardBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))

                TextField(
                    AppLocalized.text("notes.input.content_hint", fallback: "Kurze Notiz schreiben..."),
                    text: $createContent,
                    axis: .vertical
                )
                .lineLimit(2...4)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(AppColors.cardBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))

                SkydownBrandActionButton(
                    title: AppLocalized.text("notes.input.add", fallback: "Notiz hinzufuegen"),
                    systemImage: "plus.circle.fill",
                    accent: AppColors.accentHighlight(for: colorScheme),
                    colorScheme: colorScheme,
                    isEnabled: canAddNote,
                    font: .caption.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 9,
                    action: {
                        let title = createTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                        let content = createContent.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !title.isEmpty || !content.isEmpty else { return }
                        Task {
                            try? await onCreate(title, content)
                            createTitle = ""
                            createContent = ""
                        }
                    }
                )
            }

            TextField(AppLocalized.text("common.search", fallback: "Suchen"), text: $searchText)
                .textInputAutocapitalization(.never)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(AppColors.cardBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))

            if filteredNotes.isEmpty {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(AppLocalized.text("notes.empty", fallback: "Noch keine Notizen"))
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    Text(AppLocalized.text("notes.create_with_ai", fallback: "Notizen mit AI erstellen"))
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else {
                List {
                    ForEach(filteredNotes.prefix(10)) { note in
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSubtle) {
                            Text(note.title)
                                .font(.footnote.weight(.semibold))
                                .foregroundColor(AppColors.text(for: colorScheme))
                            if !note.content.isEmpty {
                                Text(note.content)
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    .lineLimit(2)
                            }
                            if let updatedAt = note.updatedAt ?? note.createdAt {
                                Text(Self.dateFormatter.string(from: updatedAt))
                                    .font(.caption2)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            onOpenNote(note)
                        }
                        .listRowInsets(EdgeInsets(top: 6, leading: 2, bottom: 6, trailing: 2))
                        .listRowBackground(Color.clear)
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(AppLocalized.text("common.delete", fallback: "Loeschen"), role: .destructive) {
                                Task { try? await onDelete(note) }
                            }
                        }
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .refreshable {
                    await onRefresh()
                }
                .frame(minHeight: 120, maxHeight: 300)
            }
        }
        .padding(10)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct AgentNoteDetailSheet: View {
    let colorScheme: ColorScheme
    let note: NoteItem
    let onSave: (_ title: String, _ content: String) async throws -> Void
    let onDelete: () async throws -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var titleText: String = ""
    @State private var contentText: String = ""

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                TextField(AppLocalized.text("notes.field.title", fallback: "Titel"), text: $titleText)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                TextField(AppLocalized.text("notes.field.content", fallback: "Inhalt"), text: $contentText, axis: .vertical)
                    .lineLimit(6...12)
                    .padding(10)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous))
                HStack {
                    Button(AppLocalized.text("common.delete", fallback: "Loeschen"), role: .destructive) {
                        Task {
                            try? await onDelete()
                            dismiss()
                        }
                    }
                    Spacer()
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.save", fallback: "Speichern"),
                        systemImage: "checkmark.circle.fill",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 10,
                        expandToFullWidth: false,
                        action: {
                            Task {
                                try? await onSave(titleText, contentText)
                                dismiss()
                            }
                        }
                    )
                    .skydownInteractiveFeedback()
                }
            }
            .padding()
            .onAppear {
                titleText = note.title
                contentText = note.content
            }
            .navigationTitle(AppLocalized.text("notes.title", fallback: "Notizen"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.done", fallback: "Done"),
                        accent: AppColors.accentMystic(for: colorScheme),
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
            .scrollDismissesKeyboard(.interactively)
            .skydownPremiumInputSurface()
        }
    }
}

private struct AgentDisabledCard: View {
    @Environment(\.accessibilityReduceMotion) private var accessibilityReduceMotion
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: "lock.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                Text(AppLocalized.text("agent.paused", fallback: "SkyOS Agent pausiert"))
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
                Text(AppLocalized.text("common.status.idle", fallback: "Leerlauf"))
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentMystic(for: colorScheme).opacity(0.12))
                    )
            }

            Text(AppLocalized.text("agent.paused.detail", fallback: "Der Rest der App bleibt verfuegbar. Der Agent laeuft weiter, sobald er wieder aktiviert ist."))
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
        .transition(
            accessibilityReduceMotion
                ? .opacity
                : .opacity.combined(with: .move(edge: .top))
        )
        .animation(
            SkydownMotion.preferredStatusTransition(accessibilityReduceMotion: accessibilityReduceMotion),
            value: colorScheme
        )
    }
}

private enum AgentInputAttachmentKind: String {
    case text = "Text"
    case video = "Video"
    case audio = "Audio"
    case image = "Bild"
    case document = "Dokument"
    case file = "Datei"

    init(url: URL) {
        let ext = url.pathExtension.lowercased()
        if ["txt", "md", "rtf", "json", "csv", "xml", "html"].contains(ext) {
            self = .text
        } else if ["mp4", "mov", "m4v", "avi", "mkv", "webm"].contains(ext) {
            self = .video
        } else if ["mp3", "wav", "m4a", "aac", "flac", "aiff"].contains(ext) {
            self = .audio
        } else if ["png", "jpg", "jpeg", "webp", "heic", "gif"].contains(ext) {
            self = .image
        } else if ["pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx"].contains(ext) {
            self = .document
        } else {
            self = .file
        }
    }

    var iconName: String {
        switch self {
        case .text: return "text.alignleft"
        case .video: return "play.rectangle.fill"
        case .audio: return "waveform"
        case .image: return "photo.fill"
        case .document: return "doc.text.fill"
        case .file: return "paperclip"
        }
    }
}

private struct AgentInputAttachment: Identifiable, Equatable {
    let id: String
    let name: String
    let kind: AgentInputAttachmentKind

    init(url: URL) {
        self.id = url.absoluteString
        self.name = url.lastPathComponent.isEmpty
            ? AppLocalized.text("agent.attachments.default_name", fallback: "File")
            : url.lastPathComponent
        self.kind = AgentInputAttachmentKind(url: url)
    }
}

private struct AgentPromptFab: View {
    let isWorking: Bool
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: SkydownLayout.stackSpacingPill) {
                ZStack {
                    Circle()
                        .fill(.thinMaterial)
                        .frame(width: 34, height: 34)

                    if isWorking {
                        ProgressView()
                            .scaleEffect(0.68)
                    } else {
                        Image(systemName: "wand.and.stars")
                            .font(.subheadline.weight(.black))
                    }
                }

                Text(
                    isWorking
                        ? AppLocalized.text("agent.fab.state.working", fallback: "Arbeitet")
                        : AppLocalized.text("agent.fab.state.idle", fallback: "Agent")
                )
                    .font(.subheadline.weight(.black))
                    .lineLimit(1)

                Image(systemName: "plus")
                    .font(.caption.weight(.black))
                    .opacity(isWorking ? 0 : 0.86)
            }
            .foregroundColor(.primary)
            .padding(.leading, 10)
            .padding(.trailing, 15)
            .frame(height: 58)
            .background(.ultraThinMaterial)
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous)
                    .stroke(.white.opacity(0.18), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous))
            .shadow(color: .black.opacity(0.16), radius: 18, x: 0, y: 10)
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .accessibilityLabel(AppLocalized.text("agent.a11y.open_prompt", fallback: "Prompt oeffnen"))
        .accessibilityIdentifier("agent.prompt.open")
    }
}

private struct AgentAccessibilityMarker: View {
    let identifier: String
    let label: String

    var body: some View {
        Color.clear
            .frame(width: 1, height: 1)
            .accessibilityElement()
            .accessibilityLabel(label)
            .accessibilityIdentifier(identifier)
            .allowsHitTesting(false)
    }
}

/// Eingabe direkt im Thread, ohne jedes Mal nur das Floating-Sheet nutzen zu muessen.
private struct AgentThreadFollowUpBar: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    let isWorking: Bool
    let onSend: () -> Void
    let onOpenFullComposer: () -> Void
    let canSend: () -> Bool

    var body: some View {
        HStack(alignment: .bottom, spacing: 10) {
            TextField(
                "",
                text: $draft,
                prompt: Text(
                    AppLocalized.text(
                        "agent.thread.followup.placeholder",
                        fallback: "Im Thread weiterschreiben …"
                    )
                )
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.78)),
                axis: .vertical
            )
            .lineLimit(1...4)
            .font(.subheadline.weight(.medium))
            .foregroundColor(AppColors.text(for: colorScheme))
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(AppColors.cardBackground(for: colorScheme))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.2), lineWidth: 1)
            )
            .accessibilityIdentifier("agent.thread.followup.field")

            Button(action: onOpenFullComposer) {
                Image(systemName: "slider.horizontal.3")
                    .font(.body.weight(.semibold))
            }
            .buttonStyle(.plain)
            .foregroundColor(AppColors.accentMystic(for: colorScheme))
            .accessibilityLabel(
                AppLocalized.text("agent.thread.open_full_composer.a11y", fallback: "Einstellungen und Anhaenge")
            )

            Button(action: onSend) {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 30, weight: .semibold))
            }
            .buttonStyle(.plain)
            .foregroundColor(
                canSend() && !isWorking
                    ? AppColors.accentMystic(for: colorScheme)
                    : AppColors.secondaryText(for: colorScheme).opacity(0.35)
            )
            .disabled(!canSend() || isWorking)
            .accessibilityLabel(AppLocalized.text("agent.thread.send.a11y", fallback: "Senden"))
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
    }
}

private struct SocialHandleTextField: View {
    let colorScheme: ColorScheme
    let accessibilityLabel: String
    let placeholder: String
    @Binding var text: String

    var body: some View {
        TextField(
            "",
            text: $text,
            prompt: Text(placeholder)
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.75))
        )
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()
        .accessibilityLabel(accessibilityLabel)
    }
}

private struct AgentPromptComposerSheet: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var selectedMode: AgentExecutionMode
    @Binding var selectedAutomationScope: AgentAutomationScope
    @Binding var shouldTriggerAutomation: Bool
    @Binding var socialInstagramEnabled: Bool
    @Binding var socialInstagramHandle: String
    @Binding var socialTiktokEnabled: Bool
    @Binding var socialTiktokHandle: String
    @Binding var socialYoutubeEnabled: Bool
    @Binding var socialYoutubeHandle: String
    @Binding var socialFacebookEnabled: Bool
    @Binding var socialFacebookHandle: String
    @Binding var socialSpotifyEnabled: Bool
    @Binding var socialSpotifyHandle: String
    let shouldShowSocialSetupCard: Bool
    let canTriggerAutomation: Bool
    let canUseGlobalOwnerAutomationFlow: Bool
    let interactionPhase: AgentInteractionPhase
    let attachments: [AgentInputAttachment]
    let quickPrompts: [String]
    let onDismiss: () -> Void
    let onAddFiles: () -> Void
    let onResetSocialSetup: () -> Void
    let onRemoveAttachment: (AgentInputAttachment) -> Void
    let onClearAttachments: () -> Void
    let onSend: () -> Void
    @FocusState private var isFocused: Bool

    private var trimmedDraft: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var canSendPrompt: Bool {
        guard !trimmedDraft.isEmpty, !interactionPhase.shouldBlockSend else { return false }
        if selectedMode == .automation, canTriggerAutomation, shouldTriggerAutomation {
            return socialInstagramEnabled || socialTiktokEnabled || socialYoutubeEnabled
                || socialFacebookEnabled || socialSpotifyEnabled
        }
        return true
    }

    private func submitPromptIfPossible() {
        guard canSendPrompt else { return }
        isFocused = false
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        onSend()
    }

    private var workflowSocialBlockedHint: String? {
        guard !trimmedDraft.isEmpty,
              !interactionPhase.shouldBlockSend,
              selectedMode == .automation,
              canTriggerAutomation,
              shouldTriggerAutomation,
              !canSendPrompt
        else { return nil }
        return AppLocalized.text(
            "agent.workflow.social_required_hint",
            fallback: "Workflow ist aktiv — waehle mindestens eine Plattform zum Senden."
        )
    }

    private var agentAccent: Color {
        AppColors.accentMystic(for: colorScheme)
    }

    private var agentAutomationScopeChoices: [AgentAutomationScope] {
        AgentAutomationScope.allCases.filter { scope in
            scope != .owner || canUseGlobalOwnerAutomationFlow
        }
    }

    @ViewBuilder
    private var agentSettingsDropdownRows: some View {
        HStack(alignment: .center) {
            Text(AppLocalized.text("agent.settings.mode", fallback: "Modus"))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Spacer(minLength: 12)
            Picker(AppLocalized.text("agent.picker.mode", fallback: "Modus"), selection: $selectedMode) {
                ForEach(AgentExecutionMode.allCases) { mode in
                    Text(mode.title).tag(mode)
                }
            }
            .labelsHidden()
            .pickerStyle(.menu)
            .tint(agentAccent)
            .disabled(interactionPhase.shouldBlockComposerChrome)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)

        if canTriggerAutomation {
            Divider()
                .padding(.leading, 8)
            if canUseGlobalOwnerAutomationFlow {
                HStack(alignment: .center) {
                    Text(AppLocalized.text("agent.settings.scope", fallback: "Bereich"))
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    Spacer(minLength: 12)
                    Picker(AppLocalized.text("agent.picker.scope", fallback: "Bereich"), selection: $selectedAutomationScope) {
                        ForEach(agentAutomationScopeChoices) { scope in
                            Text(scope.title).tag(scope)
                        }
                    }
                    .labelsHidden()
                    .pickerStyle(.menu)
                    .tint(agentAccent)
                    .disabled(interactionPhase.shouldBlockComposerChrome)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)

                Divider()
                    .padding(.leading, 8)
            }
            Toggle(isOn: $shouldTriggerAutomation) {
                Text(
                    canUseGlobalOwnerAutomationFlow
                        ? AppLocalized.text("agent.automation.workflow", fallback: "Workflow")
                        : AppLocalized.text("agent.automation.workflow.personal", fallback: "Persoenlicher Workflow")
                )
                    .font(.subheadline)
                    .foregroundColor(AppColors.text(for: colorScheme))
            }
            .tint(agentAccent)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .disabled(interactionPhase.shouldBlockComposerChrome)
            .accessibilityLabel(
                canUseGlobalOwnerAutomationFlow
                    ? (shouldTriggerAutomation
                        ? AppLocalized.text("agent.a11y.workflow.on", fallback: "Workflow aktiv")
                        : AppLocalized.text("agent.a11y.workflow.off", fallback: "Workflow starten"))
                    : (shouldTriggerAutomation
                        ? AppLocalized.text("agent.a11y.personal_workflow.on", fallback: "Persoenlicher Workflow aktiv")
                        : AppLocalized.text("agent.a11y.personal_workflow.off", fallback: "Persoenlichen Workflow starten"))
            )
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSection + 2) {
                AgentAccessibilityMarker(
                    identifier: "agent.prompt.sheet",
                    label: AppLocalized.text("agent.a11y.prompt_composer", fallback: "Agent prompt composer")
                )

                PremiumPromptSheetHeader(
                    iconSystemName: "wand.and.stars",
                    title: AppLocalized.text("agent.sheet.composer_title", fallback: "Agent"),
                    subtitle: AppLocalized.text("agent.sheet.composer_subtitle", fallback: "Modus und mehr in den Dropdowns — ein Senden reicht."),
                    accent: agentAccent,
                    colorScheme: colorScheme,
                    onDismiss: onDismiss
                )

                if let status = interactionPhase.composerStatusLabel {
                    Text(status)
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(agentAccent)
                        .padding(SkydownLayout.compactRadius)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                                .fill(agentAccent.opacity(0.1))
                        )
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    PremiumPromptSectionHeader(
                        title: AppLocalized.text("agent.section.settings", fallback: "Einstellungen"),
                        footnote: AppLocalized.text("agent.section.settings.footnote", fallback: "Eine Zeile pro Option — leicht scanbar."),
                        accent: agentAccent,
                        colorScheme: colorScheme
                    )
                    PremiumPromptSettingsDropdownCard(
                        colorScheme: colorScheme,
                        emphasisAccent: agentAccent
                    ) {
                        agentSettingsDropdownRows
                    }
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    PremiumPromptSectionHeader(
                        title: AppLocalized.text("agent.section.ideas", fallback: "Ideas"),
                        footnote: AppLocalized.text("agent.section.ideas.footnote", fallback: "Adjust starter text and keep writing."),
                        accent: agentAccent,
                        colorScheme: colorScheme
                    )
                    AgentQuickPromptCard(
                        colorScheme: colorScheme,
                        showsInlineHeading: false,
                        prompts: quickPrompts,
                        onPromptSelected: { prompt in
                            draft = prompt
                        }
                    )
                }

                if shouldShowSocialSetupCard {
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                        HStack(alignment: .center) {
                            PremiumPromptSectionHeader(
                                title: AppLocalized.text("agent.section.social", fallback: "Social setup"),
                                footnote: AppLocalized.text("agent.section.social.footnote", fallback: "Optional: only enable the platforms to analyse now."),
                                accent: agentAccent,
                                colorScheme: colorScheme
                            )
                            Spacer()
                            Button(AppLocalized.text("agent.social.reset", fallback: "Reset")) {
                                onResetSocialSetup()
                            }
                            .font(.caption.weight(.semibold))
                        }
                        PremiumPromptCard(colorScheme: colorScheme) {
                            Text(
                                AppLocalized.text(
                                    "agent.social.handles.explainer",
                                    fallback: "Only enabled channels are included. Instagram/TikTok/YouTube/Meta use handles. Spotify uses a reference (URI, artist URL, artist ID, or search text)."
                                )
                            )
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            Text(
                                AppLocalized.text(
                                    "agent.social.provider.status.title",
                                    fallback: "Current provider status"
                                )
                            )
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme).opacity(0.82))

                            Text(
                                AppLocalized.text(
                                    "agent.social.provider.status.lines",
                                    fallback: "YouTube und TikTok sind live. Instagram kann als Fallback laufen, bis Meta-Graph-Scopes freigegeben sind. Spotify kann trotz gueltiger Keys durch Provider-Richtlinien eingeschraenkt sein."
                                )
                            )
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            Text(
                                AppLocalized.text(
                                    "agent.social.scope.workaround",
                                    fallback: "Scope-Workaround: TikTok/YouTube fuer Live-Daten aktiv lassen, bei blockierten Scopes den Instagram-Handle als Kontext nutzen und Spotify als Referenz-Input eintragen."
                                )
                            )
                            .font(.caption)
                            .foregroundColor(agentAccent.opacity(0.9))

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    AgentStatusChip(
                                        text: AppLocalized.text("agent.social.badge.youtube_live", fallback: "YouTube LIVE"),
                                        accent: AppColors.accent(for: colorScheme),
                                        colorScheme: colorScheme
                                    )
                                    AgentStatusChip(
                                        text: AppLocalized.text("agent.social.badge.tiktok_live", fallback: "TikTok LIVE"),
                                        accent: AppColors.accent(for: colorScheme),
                                        colorScheme: colorScheme
                                    )
                                    AgentStatusChip(
                                        text: AppLocalized.text("agent.social.badge.instagram_fallback", fallback: "Instagram FALLBACK"),
                                        accent: AppColors.accentMystic(for: colorScheme),
                                        colorScheme: colorScheme
                                    )
                                    AgentStatusChip(
                                        text: AppLocalized.text("agent.social.badge.spotify_restricted", fallback: "Spotify RESTRICTED"),
                                        accent: AppColors.secondaryText(for: colorScheme),
                                        colorScheme: colorScheme
                                    )
                                }
                            }

                            Toggle(isOn: $socialInstagramEnabled) {
                                Text(AppLocalized.text("agent.platform.instagram", fallback: "Instagram"))
                                    .font(.subheadline.weight(.semibold))
                            }
                            .tint(agentAccent)
                            if socialInstagramEnabled {
                                SocialHandleTextField(
                                    colorScheme: colorScheme,
                                    accessibilityLabel: AppLocalized.text("agent.a11y.instagram_handle", fallback: "Instagram handle (optional)"),
                                    placeholder: "@y.d.nash",
                                    text: $socialInstagramHandle
                                )
                            }

                            Divider()

                            Toggle(isOn: $socialTiktokEnabled) {
                                Text(AppLocalized.text("agent.platform.tiktok", fallback: "TikTok"))
                                    .font(.subheadline.weight(.semibold))
                            }
                            .tint(agentAccent)
                            if socialTiktokEnabled {
                                SocialHandleTextField(
                                    colorScheme: colorScheme,
                                    accessibilityLabel: AppLocalized.text("agent.a11y.tiktok_handle", fallback: "TikTok handle (optional)"),
                                    placeholder: "@name",
                                    text: $socialTiktokHandle
                                )
                            }

                            Divider()

                            Toggle(isOn: $socialYoutubeEnabled) {
                                Text(AppLocalized.text("agent.platform.youtube", fallback: "YouTube"))
                                    .font(.subheadline.weight(.semibold))
                            }
                            .tint(agentAccent)
                            if socialYoutubeEnabled {
                                SocialHandleTextField(
                                    colorScheme: colorScheme,
                                    accessibilityLabel: AppLocalized.text("agent.a11y.youtube_handle", fallback: "YouTube handle or channel (optional)"),
                                    placeholder: "@channel",
                                    text: $socialYoutubeHandle
                                )
                            }

                            Divider()

                            Toggle(isOn: $socialFacebookEnabled) {
                                Text(AppLocalized.text("agent.platform.facebook_meta", fallback: "Facebook / Meta"))
                                    .font(.subheadline.weight(.semibold))
                            }
                            .tint(agentAccent)
                            if socialFacebookEnabled {
                                SocialHandleTextField(
                                    colorScheme: colorScheme,
                                    accessibilityLabel: AppLocalized.text("agent.a11y.facebook_handle", fallback: "Page or @handle (optional)"),
                                    placeholder: "@seite",
                                    text: $socialFacebookHandle
                                )
                            }

                            Divider()

                            Toggle(isOn: $socialSpotifyEnabled) {
                                Text(AppLocalized.text("agent.platform.spotify", fallback: "Spotify"))
                                    .font(.subheadline.weight(.semibold))
                            }
                            .tint(agentAccent)
                            if socialSpotifyEnabled {
                                SocialHandleTextField(
                                    colorScheme: colorScheme,
                                    accessibilityLabel: AppLocalized.text("agent.a11y.spotify_handle", fallback: "Spotify reference (URI, URL, ID, or search text, optional)"),
                                    placeholder: "spotify:artist:... oder open.spotify.com/artist/...",
                                    text: $socialSpotifyHandle
                                )
                                Text(
                                    AppLocalized.text(
                                        "agent.social.spotify.helper",
                                        fallback: "Premium UX tip: Spotify references work without @handle. Example: spotify:artist:1Xyo4u8uXC1ZmMpatF05PJ"
                                    )
                                )
                                .font(.caption2)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                        }
                    }
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    PremiumPromptSectionHeader(
                        title: AppLocalized.text("agent.section.input", fallback: "Eingabe"),
                        footnote: AppLocalized.text("agent.section.input.footnote", fallback: "Aufgabe, Erwartung, Links. Ein klarer Satz reicht oft aus."),
                        accent: agentAccent,
                        colorScheme: colorScheme
                    )
                    PremiumPromptCard(colorScheme: colorScheme) {
                        TextField(selectedMode.placeholder, text: $draft, axis: .vertical)
                            .accessibilityIdentifier("agent.prompt.draft")
                            .lineLimit(4...9)
                            .focused($isFocused)
                            .submitLabel(.send)
                            .onSubmit {
                                submitPromptIfPossible()
                            }
                            .font(.body)
                            .foregroundColor(AppColors.text(for: colorScheme))
                    }
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    PremiumPromptSectionHeader(
                        title: AppLocalized.text("agent.section.attachments", fallback: "Attachments"),
                        footnote: AppLocalized.text("agent.section.attachments.footnote", fallback: "Optional. Reference files, PDFs, images."),
                        accent: agentAccent,
                        colorScheme: colorScheme
                    )
                    PremiumPromptCard(colorScheme: colorScheme) {
                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            Button(action: onAddFiles) {
                                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                                    Image(systemName: "paperclip")
                                        .font(.subheadline.weight(.semibold))
                                    Text(AppLocalized.text("agent.attachments.add", fallback: "Add"))
                                        .font(.subheadline.weight(.semibold))
                                }
                                .foregroundColor(agentAccent)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(
                                    RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous)
                                        .fill(agentAccent.opacity(0.12))
                                )
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(AppLocalized.text("agent.a11y.add_files", fallback: "Add files"))

                            Text(
                                attachments.isEmpty
                                    ? AppLocalized.text("agent.attachments.none", fallback: "No file")
                                    : String(
                                        format: AppLocalized.text("agent.attachments.count", fallback: "%d file(s)"),
                                        attachments.count
                                    )
                            )
                                .font(.subheadline.weight(.medium))
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }

                        if !attachments.isEmpty {
                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                                ForEach(attachments) { attachment in
                                    HStack(spacing: SkydownLayout.stackSpacingMicro) {
                                        Image(systemName: attachment.kind.iconName)
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundColor(AppColors.accent(for: colorScheme))
                                        Text(attachment.name)
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundColor(AppColors.text(for: colorScheme))
                                            .lineLimit(1)
                                        Spacer(minLength: 0)
                                        Button { onRemoveAttachment(attachment) } label: {
                                            Image(systemName: "xmark")
                                                .font(.caption2.weight(.bold))
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                                Button(AppLocalized.text("agent.attachments.remove_all", fallback: "Remove all"), action: onClearAttachments)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(AppColors.accent(for: colorScheme))
                            }
                            .padding(.top, 8)
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    PremiumPromptPrimaryButton(
                        title: AppLocalized.text("agent.primary.send", fallback: "Send"),
                        systemImage: "arrow.up.circle.fill",
                        accent: agentAccent,
                        colorScheme: colorScheme,
                        isEnabled: canSendPrompt,
                        action: submitPromptIfPossible
                    )
                    .accessibilityLabel(AppLocalized.text("agent.a11y.send_prompt", fallback: "Send prompt"))
                    .accessibilityIdentifier("agent.prompt.send")

                    if let hint = workflowSocialBlockedHint {
                        Text(hint)
                            .font(.caption2)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.92))
                    }
                }
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.top, 12)
            .padding(.bottom, 34)
        }
        .background(
            AppColors.primaryBackground(for: colorScheme)
                .ignoresSafeArea()
        )
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.28) {
                isFocused = true
            }
        }
    }
}

private struct AgentInlineErrorCard: View {
    let message: String
    let showUpgradeAction: Bool
    let colorScheme: ColorScheme
    let onRetry: () -> Void
    let onUpgrade: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                Text(AppLocalized.text("agent.error.title", fallback: "Agent needs attention"))
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
            }

            Text(message)
                .font(.caption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .lineLimit(3)

            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                SkydownBrandActionButton(
                    title: AppLocalized.text("agent.error.retry", fallback: "Retry"),
                    systemImage: "arrow.clockwise",
                    accent: AppColors.accentHighlight(for: colorScheme),
                    colorScheme: colorScheme,
                    font: .caption.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 8,
                    expandToFullWidth: showUpgradeAction,
                    action: onRetry
                )
                .frame(maxWidth: showUpgradeAction ? .infinity : nil, alignment: .leading)

                if showUpgradeAction {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("agent.error.upgrade", fallback: "Upgrade"),
                        accent: AppColors.accentMystic(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: onUpgrade
                    )
                }
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.9))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.25), lineWidth: 1)
        )
    }
}

private struct AgentQuickPromptCard: View {
    let colorScheme: ColorScheme
    var showsInlineHeading: Bool = true
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            if showsInlineHeading {
                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                    AgentStatusChip(
                        text: AppLocalized.text("agent.chip.prompts", fallback: "Prompts"),
                        accent: AppColors.accentMystic(for: colorScheme),
                        colorScheme: colorScheme
                    )

                    Text(AppLocalized.text("agent.quick_starts", fallback: "Quick starts"))
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    ForEach(Array(prompts.enumerated()), id: \.element) { index, prompt in
                        Button(action: { onPromptSelected(prompt) }, label: {
                            Text(prompt)
                                .font(.subheadline.weight(.semibold))
                                .multilineTextAlignment(.leading)
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .frame(width: 230, alignment: .leading)
                                .background(
                                    LinearGradient(
                                        colors: [
                                            AppColors.cardBackground(for: colorScheme).opacity(0.96),
                                            AppColors.accentMystic(for: colorScheme).opacity(0.1)
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                                        .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
                                )
                        })
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                        .accessibilityLabel(prompt)
                        .accessibilityIdentifier("agent.quick_prompt.\(index)")
                    }
                }
            }
        }
    }
}

private enum AgentHttpsLinkTextBuilder {
    static func attributed(text: String, baseColor: Color, linkColor: Color) -> AttributedString {
        let ns = text as NSString
        let fullRange = NSRange(location: 0, length: ns.length)
        guard let regex = try? NSRegularExpression(
            pattern: #"https://[\w\-._~:/?#\[\]@!$&'()*+,;=%]+"#,
            options: []
        ) else {
            var plain = AttributedString(text)
            plain.foregroundColor = baseColor
            return plain
        }
        let matches = regex.matches(in: text, options: [], range: fullRange)
        guard !matches.isEmpty else {
            var plain = AttributedString(text)
            plain.foregroundColor = baseColor
            return plain
        }
        var output = AttributedString()
        var cursor = 0
        for match in matches {
            let range = match.range
            if range.location > cursor {
                let before = ns.substring(with: NSRange(location: cursor, length: range.location - cursor))
                var segment = AttributedString(before)
                segment.foregroundColor = baseColor
                output.append(segment)
            }
            let urlString = ns.substring(with: range)
            if let url = URL(string: urlString),
               url.scheme?.lowercased() == "https",
               url.host != nil {
                var linkSegment = AttributedString(urlString)
                linkSegment.foregroundColor = linkColor
                linkSegment.link = url
                output.append(linkSegment)
            } else {
                var segment = AttributedString(urlString)
                segment.foregroundColor = baseColor
                output.append(segment)
            }
            cursor = range.location + range.length
        }
        if cursor < ns.length {
            let tail = ns.substring(from: cursor)
            var segment = AttributedString(tail)
            segment.foregroundColor = baseColor
            output.append(segment)
        }
        return output
    }
}

private struct AgentMessageBubble: View {
    let message: AgentChatMessage
    let colorScheme: ColorScheme
    let onOpenHomeProductivity: ((AgentHomeProductivityTarget) -> Void)?
    var onContinueInMode: (AgentExecutionMode) -> Void = { _ in }
    @State private var showingShareSheet = false
    @State private var showCopiedFeedback = false

    private var isUser: Bool {
        message.role == .user
    }

    private var continueTargetModes: [AgentExecutionMode] {
        if let r = message.responseMode {
            return AgentExecutionMode.allCases.filter { $0 != r }
        }
        return Array(AgentExecutionMode.allCases)
    }

    private var hasContinuationPayload: Bool {
        let t = message.text.trimmingCharacters(in: .whitespacesAndNewlines)
        if !t.isEmpty { return true }
        if message.workflowSummary != nil { return true }
        return !message.results.isEmpty
    }

    private var homeOpenTarget: AgentHomeProductivityTarget? {
        guard !isUser else { return nil }
        let text = message.text
        func count(for label: String) -> Int {
            let pattern = "\(label)\\s*:?\\s*(\\d+)"
            guard
                let regex = try? NSRegularExpression(pattern: pattern),
                let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)),
                let range = Range(match.range(at: 1), in: text)
            else {
                return 0
            }
            return Int(text[range]) ?? 0
        }
        let reminder = count(for: "Reminder")
        let task = count(for: "Tasks?")
        let note = count(for: "Notizen?")
        if reminder <= 0 && task <= 0 && note <= 0 { return nil }
        if reminder >= task && reminder >= note { return .reminderManager }
        if task >= note { return .taskManager }
        return .noteManager
    }

    var body: some View {
        HStack {
            if isUser { Spacer(minLength: 48) }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                Text(
                    isUser
                        ? AppLocalized.text("agent.bubble.user", fallback: "You")
                        : AppLocalized.text("agent.bubble.assistant", fallback: "SkyOS Agent")
                )
                    .font(.caption.weight(.bold))
                    .foregroundColor(isUser ? .white.opacity(0.9) : AppColors.accentMystic(for: colorScheme))

                if message.resultType == .progress {
                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        ProgressView()
                            .tint(AppColors.accentMystic(for: colorScheme))

                        Text(AppLocalized.text("agent.progress.structuring", fallback: "SkyOS Agent is structuring the answer…"))
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else {
                    if !isUser, let workflowSummary = message.workflowSummary {
                        AgentWorkflowResultCard(
                            summary: workflowSummary,
                            isError: message.resultType == .error,
                            colorScheme: colorScheme
                        )
                    }

                    if !message.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Text(
                            AgentHttpsLinkTextBuilder.attributed(
                                text: message.text,
                                baseColor: isUser ? .white : AppColors.text(for: colorScheme),
                                linkColor: isUser ? Color.white.opacity(0.92) : AppColors.accentMystic(for: colorScheme)
                            )
                        )
                        .font(.body)
                    }

                    if !isUser {
                        AgentStructuredResultsView(
                            results: message.results,
                            colorScheme: colorScheme
                        )
                    }

                    if !isUser, message.resultType != .progress,
                       hasContinuationPayload,
                       !continueTargetModes.isEmpty {
                        VStack(alignment: .leading, spacing: 0) {
                            Rectangle()
                                .fill(AppColors.accentMystic(for: colorScheme).opacity(0.14))
                                .frame(height: 1)
                                .padding(.top, 10)

                            Text(
                                AppLocalized.text(
                                    "agent.continue_in_mode_subtitle",
                                    fallback: "Ready to take this further?"
                                )
                            )
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.95))
                            .padding(.top, 12)

                            Text(
                                AppLocalized.text(
                                    "agent.continue_in_mode",
                                    fallback: "Continue in"
                                )
                            )
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accentMystic(for: colorScheme).opacity(0.92))
                            .padding(.top, 6)

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                                    ForEach(continueTargetModes, id: \.self) { mode in
                                        Button {
                                            onContinueInMode(mode)
                                        } label: {
                                            Text(mode.title)
                                                .font(.caption.weight(.semibold))
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 6)
                                        }
                                        .buttonStyle(.plain)
                                        .background(
                                            RoundedRectangle(cornerRadius: SkydownLayout.pillSoftRadius, style: .continuous)
                                                .fill(AppColors.accentMystic(for: colorScheme).opacity(0.12))
                                        )
                                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                                    }
                                }
                            }
                            .padding(.top, 4)
                        }
                    }

                    if !isUser {
                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            Button {
                                UIPasteboard.general.string = message.text
                                showCopiedFeedback = true
                                DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                                    showCopiedFeedback = false
                                }
                            } label: {
                                Text(
                                    showCopiedFeedback
                                        ? AppLocalized.text("agent.bubble.copied", fallback: "Copied")
                                        : AppLocalized.text("agent.bubble.copy", fallback: "Copy")
                                )
                            }
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accentMystic(for: colorScheme))

                            Button {
                                showingShareSheet = true
                            } label: {
                                Text(AppLocalized.text("agent.bubble.share", fallback: "Share"))
                            }
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accent(for: colorScheme))

                            if let target = homeOpenTarget {
                                Button {
                                    onOpenHomeProductivity?(target)
                                } label: {
                                    Text(AppLocalized.text("agent.bubble.open_in_home", fallback: "Open in Home"))
                                }
                                .font(.caption.weight(.semibold))
                                .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                            }
                        }
                        .padding(.top, 8)
                    }
                }
            }
            .padding(.horizontal, SkydownLayout.cardPadding)
            .padding(.vertical, 14)
            .frame(maxWidth: 360, alignment: .leading)
            .background(bubbleBackground)
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous)
                    .stroke(
                        isUser
                            ? Color.clear
                            : AppColors.accentMystic(for: colorScheme).opacity(0.14),
                        lineWidth: 1
                    )
            )

            if !isUser { Spacer(minLength: 48) }
        }
        .sheet(isPresented: $showingShareSheet) {
            AIShareSheet(activityItems: [message.text])
        }
    }

    private var bubbleBackground: some View {
        Group {
            if isUser {
                LinearGradient(
                    colors: [
                        AppColors.accent(for: colorScheme),
                        AppColors.accentMystic(for: colorScheme)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            } else {
                LinearGradient(
                    colors: [
                        AppColors.cardBackground(for: colorScheme),
                        AppColors.secondaryBackground(for: colorScheme).opacity(0.94)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            }
        }
    }
}

private struct AgentStructuredResultsView: View {
    let results: [AgentResultEntry]
    let colorScheme: ColorScheme

    private var visibleResults: [AgentResultEntry] {
        results.filter { result in
            let kind = result.agentOutputKind
            return kind != "text" && kind != "workflow" && result.hasVisibleAgentOutput
        }
    }

    var body: some View {
        if !visibleResults.isEmpty {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                ForEach(visibleResults) { result in
                    switch result.agentOutputKind {
                    case "image":
                        AgentImageResultCard(result: result, colorScheme: colorScheme)
                    case "video":
                        AgentVideoResultCard(result: result, colorScheme: colorScheme)
                    case "audio":
                        AgentAudioResultCard(result: result, colorScheme: colorScheme)
                    case "file":
                        AgentFileResultCard(result: result, colorScheme: colorScheme)
                    case "link":
                        AgentLinkResultCard(result: result, colorScheme: colorScheme)
                    case "table":
                        AgentTableResultCard(result: result, colorScheme: colorScheme)
                    case "html":
                        AgentHTMLResultCard(result: result, colorScheme: colorScheme)
                    default:
                        AgentFallbackResultCard(result: result, colorScheme: colorScheme)
                    }
                }
            }
            .padding(.top, 6)
        }
    }
}

private struct AgentResultCard<Content: View>: View {
    let title: String
    let subtitle: String
    let systemImage: String
    let colorScheme: ColorScheme
    let content: Content

    init(
        title: String,
        subtitle: String,
        systemImage: String,
        colorScheme: ColorScheme,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.subtitle = subtitle
        self.systemImage = systemImage
        self.colorScheme = colorScheme
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(spacing: SkydownLayout.stackSpacingSnug) {
                Image(systemName: systemImage)
                    .font(.caption.weight(.black))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                    .frame(width: 24, height: 24)
                    .background(AppColors.accentMystic(for: colorScheme).opacity(0.12))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                    Text(title)
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(2)
                    if !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.caption2.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(1)
                    }
                }

                Spacer(minLength: 0)
            }

            content
        }
        .padding(10)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.9))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
    }
}

private struct AgentImageResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(
                fallback: AppLocalized.text("agent.result.image", fallback: "Image")
            ),
            subtitle: result.agentSubtitle,
            systemImage: "photo",
            colorScheme: colorScheme
        ) {
            if let url = result.agentURL {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                            .frame(maxWidth: .infinity, minHeight: 160)
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    case .failure:
                        AgentFallbackResultText(result: result, colorScheme: colorScheme)
                            .frame(maxWidth: .infinity, minHeight: 120, alignment: .center)
                    @unknown default:
                        EmptyView()
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 190)
                .background(AppColors.cardBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
            } else {
                AgentFallbackResultText(result: result, colorScheme: colorScheme)
            }
        }
    }
}

private struct AgentVideoResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(
                fallback: AppLocalized.text("agent.result.video", fallback: "Video")
            ),
            subtitle: result.agentSubtitle,
            systemImage: "play.rectangle.fill",
            colorScheme: colorScheme
        ) {
            if let url = result.agentURL {
                AgentInlineVideoPlayer(url: url)
                    .frame(maxWidth: .infinity)
                    .frame(height: 210)
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
            } else {
                AgentFallbackResultText(result: result, colorScheme: colorScheme)
            }
        }
    }
}

private struct AgentInlineVideoPlayer: View {
    let url: URL
    @State private var player: AVPlayer

    init(url: URL) {
        self.url = url
        _player = State(initialValue: AVPlayer(url: url))
    }

    var body: some View {
        VideoPlayer(player: player)
            .background(Color.black)
            .onDisappear {
                player.pause()
            }
    }
}

private struct AgentAudioResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(
                fallback: AppLocalized.text("agent.result.audio", fallback: "Audio")
            ),
            subtitle: result.agentSubtitle,
            systemImage: "waveform",
            colorScheme: colorScheme
        ) {
            if let url = result.agentURL {
                AgentInlineAudioPlayer(
                    url: url,
                    title: result.agentDisplayTitle(
                        fallback: AppLocalized.text("agent.result.audio", fallback: "Audio")
                    ),
                    colorScheme: colorScheme
                )
            } else {
                AgentFallbackResultText(result: result, colorScheme: colorScheme)
            }
        }
    }
}

private struct AgentInlineAudioPlayer: View {
    let url: URL
    let title: String
    let colorScheme: ColorScheme
    @State private var player: AVPlayer
    @State private var isPlaying = false

    init(url: URL, title: String, colorScheme: ColorScheme) {
        self.url = url
        self.title = title
        self.colorScheme = colorScheme
        _player = State(initialValue: AVPlayer(url: url))
    }

    var body: some View {
        HStack(spacing: SkydownLayout.stackSpacingCompact) {
            Button {
                if isPlaying {
                    player.pause()
                } else {
                    player.play()
                }
                isPlaying.toggle()
            } label: {
                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                    .font(.headline.weight(.black))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(AppColors.accentMystic(for: colorScheme))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .skydownTactileAction()

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
                Text(title)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)
                Text(AppLocalized.text("agent.audio.player", fallback: "Audio player"))
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer(minLength: 0)
        }
        .padding(10)
        .background(AppColors.cardBackground(for: colorScheme).opacity(0.76))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
        .onDisappear {
            player.pause()
            isPlaying = false
        }
    }
}

private struct AgentFileResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme
    @Environment(\.openURL) private var openURL

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(
                fallback: AppLocalized.text("agent.result.file", fallback: "File")
            ),
            subtitle: result.agentSubtitle,
            systemImage: result.agentMimeLooksLikePDF ? "doc.richtext.fill" : "doc.fill",
            colorScheme: colorScheme
        ) {
            AgentOpenResultButton(
                title: AppLocalized.text("agent.result.open", fallback: "Open"),
                result: result,
                colorScheme: colorScheme
            )
        }
    }
}

private struct AgentLinkResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(
                fallback: AppLocalized.text("agent.result.link", fallback: "Link")
            ),
            subtitle: result.agentSubtitle,
            systemImage: "link",
            colorScheme: colorScheme
        ) {
            AgentOpenResultButton(
                title: result.text.isEmpty
                    ? AppLocalized.text("agent.result.open_link", fallback: "Open link")
                    : result.text,
                result: result,
                colorScheme: colorScheme
            )
        }
    }
}

private struct AgentOpenResultButton: View {
    let title: String
    let result: AgentResultEntry
    let colorScheme: ColorScheme
    @Environment(\.openURL) private var openURL

    var body: some View {
        Button {
            if let url = result.agentURL {
                openURL(url)
            }
        } label: {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Text(title)
                    .font(.caption.weight(.bold))
                    .lineLimit(1)
                Image(systemName: "arrow.up.right")
                    .font(.caption.weight(.black))
            }
            .foregroundColor(AppColors.accentMystic(for: colorScheme))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(AppColors.accentMystic(for: colorScheme).opacity(0.12))
            .clipShape(Capsule(style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(result.agentURL == nil)
        .opacity(result.agentURL == nil ? 0.55 : 1)
        .skydownTactileAction()
    }
}

private struct AgentTableResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    private var columnCount: Int {
        max(result.columns.count, result.rows.map(\.count).max() ?? 0)
    }

    private var columns: [String] {
        if !result.columns.isEmpty {
            return result.columns
        }
        return (0..<min(max(columnCount, 1), 8)).map { index in
            String(
                format: AppLocalized.text("agent.table.column", fallback: "Column %d"),
                index + 1
            )
        }
    }

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(
                fallback: AppLocalized.text("agent.result.table", fallback: "Table")
            ),
            subtitle: result.agentSubtitle,
            systemImage: "tablecells.fill",
            colorScheme: colorScheme
        ) {
            if !result.rows.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNone) {
                        AgentTableRow(cells: columns, isHeader: true, colorScheme: colorScheme)
                        ForEach(Array(result.rows.enumerated()), id: \.offset) { _, row in
                            AgentTableRow(cells: row, isHeader: false, colorScheme: colorScheme)
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                            .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.1), lineWidth: 1)
                    )
                }
            } else {
                AgentFallbackResultText(result: result, colorScheme: colorScheme)
            }
        }
    }
}

private struct AgentTableRow: View {
    let cells: [String]
    let isHeader: Bool
    let colorScheme: ColorScheme

    var body: some View {
        HStack(spacing: SkydownLayout.stackSpacingNone) {
            ForEach(Array(cells.enumerated()), id: \.offset) { _, cell in
                Text(cell.isEmpty ? "-" : cell)
                    .font(isHeader ? .caption.weight(.black) : .caption.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(2)
                    .frame(width: 118, alignment: .leading)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 9)
                    .background(
                        isHeader
                            ? AppColors.accentMystic(for: colorScheme).opacity(0.14)
                            : AppColors.cardBackground(for: colorScheme).opacity(0.72)
                    )
            }
        }
    }
}

private struct AgentHTMLResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(
                fallback: AppLocalized.text("agent.result.html", fallback: "HTML")
            ),
            subtitle: result.agentSubtitle,
            systemImage: "curlybraces.square.fill",
            colorScheme: colorScheme
        ) {
            if !result.html.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                AgentHTMLAttributedText(html: result.html, colorScheme: colorScheme)
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(AppColors.cardBackground(for: colorScheme).opacity(0.76))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
            } else {
                AgentFallbackResultText(result: result, colorScheme: colorScheme)
            }
        }
    }
}

private struct AgentHTMLAttributedText: UIViewRepresentable {
    let html: String
    let colorScheme: ColorScheme

    func makeUIView(context: Context) -> UILabel {
        let label = UILabel()
        label.numberOfLines = 0
        label.adjustsFontForContentSizeCategory = true
        return label
    }

    func updateUIView(_ label: UILabel, context: Context) {
        let cleanedHTML = html.replacingOccurrences(
            of: "(?is)<script.*?</script>",
            with: "",
            options: .regularExpression
        )
        let htmlWithFont = """
        <style>
        body { font: -apple-system-body; }
        </style>
        \(cleanedHTML)
        """
        if let data = htmlWithFont.data(using: .utf8),
           let attributed = try? NSMutableAttributedString(
            data: data,
            options: [
                .documentType: NSAttributedString.DocumentType.html,
                .characterEncoding: String.Encoding.utf8.rawValue
            ],
            documentAttributes: nil
           ) {
            attributed.addAttributes(
                [
                    .foregroundColor: colorScheme == .dark ? UIColor.white : UIColor.label,
                    .font: UIFont.preferredFont(forTextStyle: .subheadline)
                ],
                range: NSRange(location: 0, length: attributed.length)
            )
            label.attributedText = attributed
        } else {
            label.text = html
            label.textColor = colorScheme == .dark ? .white : .label
            label.font = .preferredFont(forTextStyle: .subheadline)
        }
    }
}

private struct AgentFallbackResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(
                fallback: AppLocalized.text("agent.result.output", fallback: "Output")
            ),
            subtitle: result.agentSubtitle,
            systemImage: "sparkles",
            colorScheme: colorScheme
        ) {
            AgentFallbackResultText(result: result, colorScheme: colorScheme)
        }
    }
}

private struct AgentFallbackResultText: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    var body: some View {
        Text(result.agentFallbackText)
            .font(.subheadline)
            .foregroundColor(AppColors.text(for: colorScheme))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .background(AppColors.cardBackground(for: colorScheme).opacity(0.76))
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
    }
}

private extension AgentResultEntry {
    var agentOutputKind: String {
        let normalized = type.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        switch normalized {
        case "url", "button":
            return "link"
        case "pdf", "document", "download":
            return "file"
        default:
            return normalized.isEmpty ? "text" : normalized
        }
    }

    var agentURL: URL? {
        let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        guard let parsed = URL(string: trimmed),
              let scheme = parsed.scheme?.lowercased(),
              scheme == "https" else {
            return nil
        }
        return parsed
    }

    var agentSubtitle: String {
        if !mimeType.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return mimeType
        }
        if let host = agentURL?.host, !host.isEmpty {
            return host
        }
        return agentOutputKind.uppercased()
    }

    var agentMimeLooksLikePDF: Bool {
        mimeType.lowercased().contains("pdf") || fileName.lowercased().hasSuffix(".pdf") || title.lowercased().hasSuffix(".pdf")
    }

    var hasVisibleAgentOutput: Bool {
        !url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
        !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
        !fileName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
        !html.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
        !rows.isEmpty
    }

    var agentFallbackText: String {
        [
            text,
            summary,
            url
        ]
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .first(where: { !$0.isEmpty }) ?? agentDisplayTitle(
                fallback: AppLocalized.text("agent.result.output_ready", fallback: "Output ready.")
            )
    }

    func agentDisplayTitle(fallback: String) -> String {
        [
            title,
            fileName,
            workflowName,
            url
        ]
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .first(where: { !$0.isEmpty }) ?? fallback
    }
}

private struct AgentWorkflowResultCard: View {
    let summary: AgentWorkflowSummary
    let isError: Bool
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(isError ? .red : AppColors.accentMystic(for: colorScheme))
                Text(summary.workflowName)
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
            }

            Text(summary.statusText)
                .font(.caption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let progress = summary.progressPercent {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    GeometryReader { geometry in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(AppColors.secondaryBackground(for: colorScheme))
                            Capsule()
                                .fill(AppColors.accentMystic(for: colorScheme))
                                .frame(width: max(8, geometry.size.width * CGFloat(progress) / 100.0))
                        }
                    }
                    .frame(height: 8)
                    Text(
                        String(
                            format: AppLocalized.text("agent.workflow.debug.progress", fallback: "%d%%"),
                            progress
                        )
                    )
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            if !summary.step.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(
                    String(
                        format: AppLocalized.text("agent.workflow.debug.step", fallback: "Step: %@"),
                        summary.step
                    )
                )
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if let eta = summary.etaSeconds, eta > 0 {
                Text(
                    String(
                        format: AppLocalized.text("agent.workflow.debug.eta", fallback: "ETA: %@s"),
                        String(eta)
                    )
                )
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if !summary.details.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(summary.details)
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(3)
            }

            if !summary.schemaVersion.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(
                    String(
                        format: AppLocalized.text("agent.workflow.debug.schema", fallback: "Schema: %@"),
                        summary.schemaVersion
                    )
                )
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .textSelection(.enabled)
            }

            if let runID = summary.runID {
                Text(
                    String(
                        format: AppLocalized.text("agent.workflow.debug.run", fallback: "Run: %@"),
                        runID
                    )
                )
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .textSelection(.enabled)
            }
        }
        .padding(10)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}
