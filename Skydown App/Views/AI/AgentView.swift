import SwiftUI
import UIKit
import AVKit
import UniformTypeIdentifiers

struct AgentView: View {
    @StateObject private var viewModel: AgentChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @ObservedObject private var membershipCoordinator: AIMembershipCoordinator
    @ObservedObject private var aiRuntimeSettingsStore = AIRuntimeSettingsStore.shared
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @State private var showingAttachmentImporter = false
    @State private var showingPromptComposer = false
    @State private var showingConversationSessions = false
    @State private var showingDeleteConversationDialog = false
    @State private var renameDraft = ""
    @State private var inputAttachments: [AgentInputAttachment] = []
    private let showsNavigation: Bool
    @State private var autoPresentedUpgradeHint = false

    init(
        agentChatService: AgentChatServicing = FirebaseFunctionsAgentService(),
        featureFlags: FeatureFlagsService,
        membershipCoordinator: AIMembershipCoordinator,
        showsNavigation: Bool = true
    ) {
        self.showsNavigation = showsNavigation
        _viewModel = StateObject(
            wrappedValue: AgentChatViewModel(service: agentChatService)
        )
        _featureFlags = ObservedObject(wrappedValue: featureFlags)
        _membershipCoordinator = ObservedObject(wrappedValue: membershipCoordinator)
    }

    var body: some View {
        Group {
            if showsNavigation {
                navigationContent
            } else {
                content
            }
        }
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
                canTriggerAutomation: viewModel.canTriggerAutomation,
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
                onRemoveAttachment: removeAttachment,
                onClearAttachments: { inputAttachments.removeAll() },
                onCreateNewChat: {
                    viewModel.startNewConversation()
                    showingPromptComposer = false
                },
                onSend: {
                    let urls = inputAttachments.compactMap { URL(string: $0.id) }
                    viewModel.sendDraft(attachmentURLs: urls)
                    inputAttachments.removeAll()
                    showingPromptComposer = false
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $showingConversationSessions) {
            AIConversationSessionsSheet(
                title: "Agent Chats",
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                sessions: viewModel.sessions,
                activeSessionID: viewModel.activeSessionID,
                isBusy: viewModel.phase.shouldBlockComposerChrome,
                renameDraft: $renameDraft,
                onCreateNewChat: {
                    viewModel.startNewConversation()
                    showingConversationSessions = false
                },
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
        .confirmationDialog(
            "Aktiven Chat loeschen?",
            isPresented: $showingDeleteConversationDialog,
            titleVisibility: .visible
        ) {
            Button("Loeschen", role: .destructive) {
                viewModel.deleteActiveConversation()
                showingConversationSessions = false
            }
            Button("Abbrechen", role: .cancel) { }
        }
        .task(id: sessionObservationKey) {
            viewModel.configureUser(user: authManager.userSession)
            configureIntegrationObservations(for: authManager.userSession)
        }
        .onChange(of: featureFlags.isAIEnabled) { _, isEnabled in
            aiRuntimeSettingsStore.setObservationEnabled(isEnabled)
        }
        .onChange(of: viewModel.revenueUsage?.warningLevel) { _, level in
            handleCriticalUsageWarning(level)
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
    }

    private func handleCriticalUsageWarning(_ level: String?) {
        guard !autoPresentedUpgradeHint else { return }
        guard level == "critical" else { return }
        autoPresentedUpgradeHint = true
        membershipCoordinator.openMembership(reason: .criticalUsage, surface: "agent_chat")
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
        featureFlags.isAIEnabled ? 86 : 0
    }

    private var content: some View {
        let activeSessionSummary = viewModel.sessions.first { $0.id == viewModel.activeSessionID }
        let activeSessionSubtitle = {
            if let activeSessionSummary {
                if !activeSessionSummary.preview.isEmpty {
                    return activeSessionSummary.preview
                }
                return activeSessionSummary.promptCount == 0 ? "Noch leer" : "\(activeSessionSummary.promptCount) Anfragen"
            }
            return "Noch leer"
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
            onCreateNewChat: viewModel.startNewConversation,
            onRefreshChat: viewModel.refreshActiveConversation,
            onDeleteChat: { showingDeleteConversationDialog = true }
        )

        return ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                if featureFlags.isAIEnabled {
                    pinnedSessionStrip
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        .padding(.top, showsNavigation ? 12 : 8)
                        .padding(.bottom, 10)
                        .background(.ultraThinMaterial)
                        .zIndex(2)

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
                                LazyVStack(alignment: .leading, spacing: 10) {
                                    VStack(alignment: .leading, spacing: 10) {
                                        ForEach(viewModel.messages) { message in
                                            AgentMessageBubble(
                                                message: message,
                                                colorScheme: colorScheme
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
                                    proxy.scrollTo("agent-chat-end", anchor: .bottom)
                                }
                            }
                            .onChange(of: scrollToken) { _, _ in
                                withAnimation(.easeOut(duration: 0.25)) {
                                    proxy.scrollTo("agent-chat-end", anchor: .bottom)
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
            .safeAreaInset(edge: .bottom, spacing: 0) {
                Color.clear
                    .frame(height: composerReservedBottomSpace)
                    .allowsHitTesting(false)
            }

            if featureFlags.isAIEnabled {
                HStack {
                    Spacer(minLength: 0)
                    AgentPromptFab(
                        isWorking: viewModel.phase.shouldBlockComposerChrome,
                        onOpen: { showingPromptComposer = true }
                    )
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.bottom, max(18, keyWindowSafeAreaBottomInset + 14))
                .zIndex(1)
            }
        }
        .ignoresSafeArea(.keyboard, edges: .bottom)
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
                .navigationTitle("Agent")
                .skydownNavigationChrome(colorScheme: colorScheme)
        }
    }
}

private struct AgentPlanPreviewCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(AppLocalized.text("ai.membership.plans.title_short", fallback: "AI Membership"))
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.accentMystic(for: colorScheme))
            Text(AppLocalized.text("ai.membership.plans.tiers", fallback: "Free, Pro, Creator"))
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(AppLocalized.text("agent.membership.caption", fallback: "Creator unlocks workflows and premium output."))
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
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
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("\(AppLocalized.text("ai.membership.current_plan", fallback: "Plan")): \(usage.planTitle)")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                Spacer()
                Text("\(usage.remaining)/\(usage.limit) \(AppLocalized.text("ai.membership.open", fallback: "open"))")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
            ProgressView(value: progress)
                .tint(usage.warningLevel == "critical" ? .red : AppColors.accentHighlight(for: colorScheme))
            if usage.warningLevel != "ok" {
                Text(
                    usage.warningLevel == "critical"
                        ? AppLocalized.text("agent.usage.warning.critical", fallback: "Close to the limit. Upgrade keeps workflows steady.")
                        : AppLocalized.text("agent.usage.warning.high", fallback: "High usage detected. Everything is stable, but monitored.")
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
                Text("\(AppLocalized.text("ai.membership.next_step", fallback: "Next step")): \(usage.suggestedUpgrade.uppercased())")
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
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.15), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
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
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
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
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct RetryLaterCard: View {
    let retryAfterSeconds: Int
    let colorScheme: ColorScheme

    var body: some View {
        Text("\(AppLocalized.text("ai.retry_in", fallback: "Please retry in about")) \(retryAfterSeconds)s.")
            .font(.caption2)
            .foregroundColor(AppColors.secondaryText(for: colorScheme))
            .padding(.horizontal, 9)
            .padding(.vertical, 7)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.72))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
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

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(AppLocalized.text("ai.membership.sheet.title", fallback: "SkyOS AI Membership"))
                    .font(.title3.weight(.black))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text(AppLocalized.text("agent.membership.sheet.subtitle", fallback: "Upgrade as progress, never as pressure."))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                AgentPlanTile(title: AppLocalized.text("membership.plan.free", fallback: "Free"), detail: AppLocalized.text("ai.membership.free_detail", fallback: "Core bot, fewer images, light agent"), colorScheme: colorScheme)
                AgentPlanTile(title: AppLocalized.text("membership.plan.pro", fallback: "Pro"), detail: AppLocalized.text("ai.membership.pro_detail", fallback: "More reach, stronger agent, creator daily flow"), colorScheme: colorScheme)
                AgentPlanTile(title: AppLocalized.text("membership.plan.creator", fallback: "Creator"), detail: AppLocalized.text("ai.membership.creator_detail", fallback: "Workflow depth, premium outputs, priority"), colorScheme: colorScheme)
                Text(AppLocalized.text("ai.membership.annual_coming", fallback: "Annual option is coming next in native billing."))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                Button(activePurchasePlan == .creator ? AppLocalized.text("membership.pro.starting", fallback: "Starting Pro...") : AppLocalized.text("membership.pro.activate", fallback: "Activate Pro")) {
                    onSelectPlan(.creator)
                }
                .buttonStyle(.bordered)
                .disabled(isLoadingProducts || isSyncing)

                Button(activePurchasePlan == .studio ? AppLocalized.text("membership.creator.starting", fallback: "Starting Creator...") : AppLocalized.text("membership.creator.activate", fallback: "Activate Creator")) {
                    onSelectPlan(.studio)
                }
                .buttonStyle(.borderedProminent)
                .disabled(isLoadingProducts || isSyncing)

                Button(AppLocalized.text("membership.restore", fallback: "Restore purchases")) { onRestore() }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                Button(AppLocalized.text("membership.manage", fallback: "Manage subscription")) { onManage() }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            }
            .padding()
        }
        .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
    }
}

private struct AgentPlanTile: View {
    let title: String
    let detail: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
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
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct AgentEmptyStateHeader: View {
    let colorScheme: ColorScheme
    var isCompact: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: isCompact ? 6 : 8) {
            Text("Wobei soll ich dich strukturieren?")
                .font(
                    isCompact
                        ? .system(size: 24, weight: .black, design: .rounded)
                        : .system(size: 28, weight: .black, design: .rounded)
                )
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Nutze den Agent fuer Briefings, Shotlists, Release-Plaene und klare naechste Schritte. Schreib direkt unten los oder starte mit einem Prompt.")
                .font(isCompact ? .subheadline.weight(.semibold) : .subheadline.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if !isCompact {
                Text("Auch der Agent fuehrt den Verlauf pro Konto weiter, damit Briefings und To-dos anschlussfaehig bleiben.")
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

private struct AgentDisabledCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "lock.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                Text(AppLocalized.text("agent.paused", fallback: "SkyOS Agent paused"))
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
                Text(AppLocalized.text("common.status.idle", fallback: "Idle"))
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentMystic(for: colorScheme).opacity(0.12))
                    )
            }

            Text(AppLocalized.text("agent.paused.detail", fallback: "The rest of the app stays available. The agent continues once re-enabled."))
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .transition(.opacity.combined(with: .move(edge: .top)))
        .animation(SkydownMotion.statusTransition, value: colorScheme)
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
        self.name = url.lastPathComponent.isEmpty ? "Datei" : url.lastPathComponent
        self.kind = AgentInputAttachmentKind(url: url)
    }
}

private struct AgentPromptFab: View {
    let isWorking: Bool
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: 10) {
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

                Text(isWorking ? "Arbeitet" : "Agent")
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
                RoundedRectangle(cornerRadius: 26, style: .continuous)
                    .stroke(.white.opacity(0.18), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
            .shadow(color: .black.opacity(0.16), radius: 18, x: 0, y: 10)
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .accessibilityLabel("Prompt oeffnen")
    }
}

private struct AgentPromptComposerSheet: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var selectedMode: AgentExecutionMode
    @Binding var selectedAutomationScope: AgentAutomationScope
    @Binding var shouldTriggerAutomation: Bool
    let canTriggerAutomation: Bool
    let interactionPhase: AgentInteractionPhase
    let attachments: [AgentInputAttachment]
    let quickPrompts: [String]
    let onDismiss: () -> Void
    let onAddFiles: () -> Void
    let onRemoveAttachment: (AgentInputAttachment) -> Void
    let onClearAttachments: () -> Void
    let onCreateNewChat: () -> Void
    let onSend: () -> Void
    @FocusState private var isFocused: Bool

    private var trimmedDraft: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        ScrollView {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center, spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.14))
                    Image(systemName: "wand.and.stars")
                        .font(.headline.weight(.black))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }
                .frame(width: 48, height: 48)

                VStack(alignment: .leading, spacing: 3) {
                    Text("Neue Agent-Anfrage")
                        .font(.title3.weight(.black))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("Modus, Workflow und Prompt in einem ruhigen Flow.")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer(minLength: 0)

                AgentStatusChip(
                    text: selectedMode.title,
                    accent: AppColors.accentMystic(for: colorScheme),
                    colorScheme: colorScheme
                )

                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.caption.weight(.black))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .frame(width: 36, height: 36)
                        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.9))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .skydownTactileAction()
                .accessibilityLabel("Prompt schliessen")
            }

            if let status = interactionPhase.composerStatusLabel {
                Text(status)
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }

            Text("Optionen")
                .font(.caption2.weight(.black))
                .foregroundColor(AppColors.accentMystic(for: colorScheme))

            HStack(spacing: 10) {
                Menu {
                    ForEach(AgentExecutionMode.allCases) { mode in
                        Button(mode.title) { selectedMode = mode }
                    }
                } label: {
                    Label(selectedMode.title, systemImage: "slider.horizontal.3")
                        .font(.caption.weight(.bold))
                }
                .disabled(interactionPhase.shouldBlockComposerChrome)

                if canTriggerAutomation {
                    Menu {
                        ForEach(AgentAutomationScope.allCases) { scope in
                            Button(scope.title) { selectedAutomationScope = scope }
                        }
                    } label: {
                        Label(selectedAutomationScope.title, systemImage: "point.3.connected.trianglepath.dotted")
                            .font(.caption.weight(.bold))
                    }
                    .disabled(interactionPhase.shouldBlockComposerChrome)

                    Button { shouldTriggerAutomation.toggle() } label: {
                        HStack(spacing: 7) {
                            Image(systemName: shouldTriggerAutomation ? "play.circle.fill" : "play.circle")
                                .font(.subheadline.weight(.black))
                            Text(shouldTriggerAutomation ? "Workflow aktiv" : "Workflow")
                                .font(.caption.weight(.bold))
                                .lineLimit(1)
                        }
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                        .padding(.horizontal, 11)
                        .padding(.vertical, 8)
                        .background(
                            Capsule(style: .continuous)
                                .fill(AppColors.accentMystic(for: colorScheme).opacity(shouldTriggerAutomation ? 0.16 : 0.09))
                        )
                        .overlay(
                            Capsule(style: .continuous)
                                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .disabled(interactionPhase.shouldBlockComposerChrome)
                    .accessibilityLabel(shouldTriggerAutomation ? "Workflow aktiv" : "Workflow starten")
                }
            }

            AgentQuickPromptCard(
                colorScheme: colorScheme,
                prompts: quickPrompts,
                onPromptSelected: { prompt in
                    draft = prompt
                }
            )

            Text("Prompt")
                .font(.caption2.weight(.black))
                .foregroundColor(AppColors.accentMystic(for: colorScheme))

            TextField(selectedMode.placeholder, text: $draft, axis: .vertical)
                .lineLimit(4...8)
                .focused($isFocused)
                .submitLabel(.send)
                .onSubmit {
                    if !trimmedDraft.isEmpty && !interactionPhase.shouldBlockSend {
                        onSend()
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .foregroundColor(AppColors.text(for: colorScheme))

            HStack(spacing: 10) {
                Button(action: onAddFiles) {
                    Image(systemName: "paperclip")
                        .font(.subheadline.weight(.bold))
                        .frame(width: 34, height: 34)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Dateien hinzufuegen")

                Text(attachments.isEmpty ? "Keine Dateien" : "\(attachments.count) Datei(en)")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if !attachments.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(attachments) { attachment in
                        HStack(spacing: 8) {
                            Image(systemName: attachment.kind.iconName)
                                .font(.caption.weight(.bold))
                                .foregroundColor(AppColors.accent(for: colorScheme))
                            Text(attachment.name)
                                .font(.caption.weight(.semibold))
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .lineLimit(1)
                            Spacer(minLength: 0)
                            Button { onRemoveAttachment(attachment) } label: {
                                Image(systemName: "xmark")
                                    .font(.caption2.weight(.bold))
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.78))
                        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    }

                    Button("Alle entfernen", action: onClearAttachments)
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
            }

            HStack(spacing: 10) {
                Spacer(minLength: 0)
                Button("Neuer Chat", action: onCreateNewChat)
                    .font(.caption.weight(.bold))
                    .disabled(interactionPhase.shouldBlockComposerChrome)

                Button(action: onSend) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title2.weight(.black))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                        .background(AppColors.accentMystic(for: colorScheme))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .disabled(trimmedDraft.isEmpty || interactionPhase.shouldBlockSend)
                .opacity(trimmedDraft.isEmpty || interactionPhase.shouldBlockSend ? 0.55 : 1)
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 18)
        .padding(.bottom, 22)
        .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                isFocused = true
            }
        }
    }
}

private struct AgentQuickPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                AgentStatusChip(
                    text: "Prompts",
                    accent: AppColors.accentMystic(for: colorScheme),
                    colorScheme: colorScheme
                )

                Text("Schnelle Starts")
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(prompts, id: \.self) { prompt in
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
                                            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                                        .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
                                )
                        })
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                    }
                }
            }
        }
    }
}

private struct AgentMessageBubble: View {
    let message: AgentChatMessage
    let colorScheme: ColorScheme
    @State private var showingShareSheet = false
    @State private var copyLabel = "Kopieren"

    private var isUser: Bool {
        message.role == .user
    }

    var body: some View {
        HStack {
            if isUser { Spacer(minLength: 48) }

            VStack(alignment: .leading, spacing: 8) {
                Text(isUser ? "Du" : "SkyOS Agent")
                    .font(.caption.weight(.bold))
                    .foregroundColor(isUser ? .white.opacity(0.9) : AppColors.accentMystic(for: colorScheme))

                if message.resultType == .progress {
                    HStack(spacing: 10) {
                        ProgressView()
                            .tint(AppColors.accentMystic(for: colorScheme))

                        Text("SkyOS Agent strukturiert gerade die Antwort...")
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
                        Text(message.text)
                            .font(.body)
                            .foregroundColor(isUser ? .white : AppColors.text(for: colorScheme))
                    }

                    if !isUser {
                        AgentStructuredResultsView(
                            results: message.results,
                            colorScheme: colorScheme
                        )
                    }

                    if !isUser {
                        HStack(spacing: 10) {
                            Button(copyLabel) {
                                UIPasteboard.general.string = message.text
                                copyLabel = "Kopiert"
                                DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                                    copyLabel = "Kopieren"
                                }
                            }
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accentMystic(for: colorScheme))

                            Button("Teilen") {
                                showingShareSheet = true
                            }
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                        }
                        .padding(.top, 8)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: 360, alignment: .leading)
            .background(bubbleBackground)
            .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 26, style: .continuous)
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
            VStack(alignment: .leading, spacing: 10) {
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
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 9) {
                Image(systemName: systemImage)
                    .font(.caption.weight(.black))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                    .frame(width: 24, height: 24)
                    .background(AppColors.accentMystic(for: colorScheme).opacity(0.12))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
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
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
    }
}

private struct AgentImageResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(fallback: "Bild"),
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
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
            title: result.agentDisplayTitle(fallback: "Video"),
            subtitle: result.agentSubtitle,
            systemImage: "play.rectangle.fill",
            colorScheme: colorScheme
        ) {
            if let url = result.agentURL {
                AgentInlineVideoPlayer(url: url)
                    .frame(maxWidth: .infinity)
                    .frame(height: 210)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
            title: result.agentDisplayTitle(fallback: "Audio"),
            subtitle: result.agentSubtitle,
            systemImage: "waveform",
            colorScheme: colorScheme
        ) {
            if let url = result.agentURL {
                AgentInlineAudioPlayer(url: url, title: result.agentDisplayTitle(fallback: "Audio"), colorScheme: colorScheme)
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
        HStack(spacing: 12) {
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

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)
                Text("Audio Player")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer(minLength: 0)
        }
        .padding(10)
        .background(AppColors.cardBackground(for: colorScheme).opacity(0.76))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
            title: result.agentDisplayTitle(fallback: "Datei"),
            subtitle: result.agentSubtitle,
            systemImage: result.agentMimeLooksLikePDF ? "doc.richtext.fill" : "doc.fill",
            colorScheme: colorScheme
        ) {
            AgentOpenResultButton(title: "Oeffnen", result: result, colorScheme: colorScheme)
        }
    }
}

private struct AgentLinkResultCard: View {
    let result: AgentResultEntry
    let colorScheme: ColorScheme

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(fallback: "Link"),
            subtitle: result.agentSubtitle,
            systemImage: "link",
            colorScheme: colorScheme
        ) {
            AgentOpenResultButton(title: result.text.isEmpty ? "Link oeffnen" : result.text, result: result, colorScheme: colorScheme)
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
            HStack(spacing: 8) {
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
        return (0..<min(max(columnCount, 1), 8)).map { "Spalte \($0 + 1)" }
    }

    var body: some View {
        AgentResultCard(
            title: result.agentDisplayTitle(fallback: "Tabelle"),
            subtitle: result.agentSubtitle,
            systemImage: "tablecells.fill",
            colorScheme: colorScheme
        ) {
            if !result.rows.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 0) {
                        AgentTableRow(cells: columns, isHeader: true, colorScheme: colorScheme)
                        ForEach(Array(result.rows.enumerated()), id: \.offset) { _, row in
                            AgentTableRow(cells: row, isHeader: false, colorScheme: colorScheme)
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
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
        HStack(spacing: 0) {
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
            title: result.agentDisplayTitle(fallback: "HTML"),
            subtitle: result.agentSubtitle,
            systemImage: "curlybraces.square.fill",
            colorScheme: colorScheme
        ) {
            if !result.html.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                AgentHTMLAttributedText(html: result.html, colorScheme: colorScheme)
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(AppColors.cardBackground(for: colorScheme).opacity(0.76))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
            title: result.agentDisplayTitle(fallback: "Output"),
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
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
            .first(where: { !$0.isEmpty }) ?? agentDisplayTitle(fallback: "Output bereit.")
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
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
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
                VStack(alignment: .leading, spacing: 4) {
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
                    Text("\(progress)%")
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            if !summary.step.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text("Step: \(summary.step)")
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if let eta = summary.etaSeconds, eta > 0 {
                Text("ETA: \(eta)s")
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if !summary.details.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(summary.details)
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(3)
            }

            if let runID = summary.runID {
                Text("Run: \(runID)")
                    .font(.caption2)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .textSelection(.enabled)
            }
        }
        .padding(10)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}
