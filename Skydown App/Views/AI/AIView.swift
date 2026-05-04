import SwiftUI
import UIKit

struct AIView: View {
    @StateObject private var viewModel: AIChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @ObservedObject private var membershipCoordinator: AIMembershipCoordinator
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var showingPromptComposer = false
    @State private var showingConversationSessions = false
    @State private var showingDeleteConversationDialog = false
    @State private var renameDraft = ""
    private let showsNavigation: Bool
    @State private var autoPresentedUpgradeHint = false

    init(
        aiChatService: AIChatServicing = FirebaseFunctionsAIChatService(),
        featureFlags: FeatureFlagsService,
        membershipCoordinator: AIMembershipCoordinator,
        showsNavigation: Bool = true
    ) {
        self.showsNavigation = showsNavigation
        _viewModel = StateObject(
            wrappedValue: AIChatViewModel(service: aiChatService)
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
        .sheet(isPresented: $showingPromptComposer) {
            AIPromptComposerSheet(
                colorScheme: colorScheme,
                draft: $viewModel.draft,
                composerMode: $viewModel.composerMode,
                textMode: $viewModel.textMode,
                selectedLevel: $viewModel.selectedLevel,
                interactionPhase: viewModel.phase,
                quickPrompts: viewModel.quickPrompts,
                visualPrompts: viewModel.visualPrompts,
                onDismiss: {
                    showingPromptComposer = false
                },
                onSend: {
                    viewModel.sendDraftInNewConversation()
                    showingPromptComposer = false
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $showingConversationSessions) {
            AIConversationSessionsSheet(
                title: AppLocalized.text("ai.sessions.list_title", fallback: "Chats"),
                accent: AppColors.accent(for: colorScheme),
                colorScheme: colorScheme,
                sessions: viewModel.sessions,
                activeSessionID: viewModel.activeSessionID,
                isBusy: viewModel.phase.isBusy,
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
        .confirmationDialog(
            AppLocalized.text("agent.delete_chat.title", fallback: "Delete active chat?"),
            isPresented: $showingDeleteConversationDialog,
            titleVisibility: .visible
        ) {
            Button(AppLocalized.text("agent.delete_chat.confirm", fallback: "Delete"), role: .destructive) {
                viewModel.deleteActiveConversation()
                showingConversationSessions = false
            }
            Button(AppLocalized.text("common.cancel", fallback: "Cancel"), role: .cancel) { }
        }
        .task(id: sessionObservationKey) {
            viewModel.configureUser(user: authManager.userSession)
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
        membershipCoordinator.openMembership(reason: .criticalUsage, surface: "ai_chat")
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
            accent: AppColors.accent(for: colorScheme),
            colorScheme: colorScheme,
            isBusy: viewModel.phase.isBusy,
            canDelete: viewModel.activeSessionID != nil,
            showsManagementActions: true,
            onOpenSessions: { showingConversationSessions = true },
            onRefreshChat: viewModel.refreshActiveConversation,
            onDeleteChat: { showingDeleteConversationDialog = true }
        )

        return ZStack(alignment: .bottom) {
            VStack(spacing: SkydownLayout.stackSpacingNone) {
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
                                "\(message.id.uuidString)-\(message.isStreaming)-\(message.text.count)-\(message.imageData?.count ?? 0)"
                            } ?? "chat-empty"

                            ScrollView {
                                LazyVStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                                    ForEach(viewModel.messages) { message in
                                        AIMessageBubble(
                                            message: message,
                                            colorScheme: colorScheme
                                        )
                                        .id(message.id)
                                    }

                                    Color.clear
                                        .frame(height: 4)
                                        .id("chat-end")
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
                                            proxy.scrollTo("chat-end", anchor: .bottom)
                                        }
                                    } else {
                                        proxy.scrollTo("chat-end", anchor: .bottom)
                                    }
                                }
                            }
                            .onChange(of: scrollToken) { _, _ in
                                DispatchQueue.main.async {
                                    if reduceMotion {
                                        var transaction = Transaction()
                                        transaction.disablesAnimations = true
                                        withTransaction(transaction) {
                                            proxy.scrollTo("chat-end", anchor: .bottom)
                                        }
                                    } else {
                                        withAnimation(.easeOut(duration: 0.25)) {
                                            proxy.scrollTo("chat-end", anchor: .bottom)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    VStack {
                        Spacer(minLength: 24)
                        AIDisabledCard(colorScheme: colorScheme)
                            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        Spacer()
                    }
                }
            }
            .safeAreaInset(edge: .bottom, spacing: SkydownLayout.stackSpacingNone) {
                Color.clear
                    .frame(height: featureFlags.isAIEnabled ? (viewModel.messages.isEmpty ? 86 : 148) : 0)
                    .allowsHitTesting(false)
            }

            if featureFlags.isAIEnabled {
                VStack(alignment: .trailing, spacing: SkydownLayout.stackSpacingCompact) {
                    AIInlineNotice(
                        isPresented: $viewModel.showToast,
                        message: viewModel.toastMessage,
                        style: viewModel.toastStyle,
                        colorScheme: colorScheme
                    )

                    if !viewModel.messages.isEmpty {
                        AIThreadFollowUpBar(
                            colorScheme: colorScheme,
                            draft: $viewModel.draft,
                            isWorking: viewModel.phase.isBusy,
                            onSend: {
                                viewModel.sendTextFollowUp()
                            },
                            onOpenFullComposer: { showingPromptComposer = true },
                            canSend: { viewModel.isCurrentTextFollowUpSendable }
                        )
                        .frame(maxWidth: 680)
                    }

                    HStack {
                        Spacer(minLength: 0)
                        AIPromptFab(
                            isWorking: viewModel.phase.isBusy,
                            onOpen: { showingPromptComposer = true }
                        )
                    }
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.bottom, max(18, keyWindowSafeAreaBottomInset + 14))
                .zIndex(1)
            }
        }
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

    private var aiSessionDeck: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: SkydownLayout.stackSpacingPill) {
                AISessionSignalCard(
                    title: AppLocalized.text("ai.session.signal.mode", fallback: "Mode"),
                    value: viewModel.composerMode.title,
                    detail: viewModel.composerMode == .visual
                        ? AppLocalized.text("ai.session.signal.mode_detail_visual", fallback: "Image generation")
                        : AppLocalized.text("ai.session.signal.mode_detail_text", fallback: "Text generation"),
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme
                )
                AISessionSignalCard(
                    title: AppLocalized.text("ai.session.signal.focus", fallback: "Focus"),
                    value: viewModel.composerMode == .visual ? "Cinematic" : viewModel.textMode.title,
                    detail: viewModel.composerMode == .visual
                        ? AppLocalized.text("ai.session.signal.focus_detail_visual", fallback: "Prompt → visual")
                        : AppLocalized.text("ai.session.signal.focus_detail_text", fallback: "Tone mode direct control"),
                    accent: AppColors.accentMystic(for: colorScheme),
                    colorScheme: colorScheme
                )
                AISessionSignalCard(
                    title: AppLocalized.text("ai.session.signal.memory", fallback: "Memory"),
                    value: viewModel.messages.isEmpty
                        ? AppLocalized.text("ai.sessions.status.new", fallback: "New")
                        : String(
                            format: AppLocalized.text("ai.session.signal.memory_steps", fallback: "%d steps"),
                            viewModel.messages.count
                        ),
                    detail: AppLocalized.text("ai.session.signal.memory_detail", fallback: "History stays active per account"),
                    accent: AppColors.spotify(for: colorScheme),
                    colorScheme: colorScheme
                )
            }
        }
    }

    private func prefillPrompt(_ prompt: String, mode: AIComposerMode) {
        guard !viewModel.phase.isBusy else { return }
        viewModel.composerMode = mode
        viewModel.draft = prompt
        withAnimation(SkydownMotion.preferredSheetPresentation(accessibilityReduceMotion: reduceMotion)) {
            showingPromptComposer = true
        }
    }
}

private struct AIPlanPreviewCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(AppLocalized.text("ai.membership.plans.title", fallback: "SkyOS AI plans"))
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.accent(for: colorScheme))
            Text(AppLocalized.text("ai.membership.plans.tiers", fallback: "Free, Pro, Creator"))
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(AppLocalized.text("ai.membership.plans.caption", fallback: "Unlock more reach and priority when you need it."))
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct AIRevenueUsageCard: View {
    let usage: AIChatViewModel.RevenueUsageState
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
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                Spacer()
                Text("\(usage.remaining)/\(usage.limit) \(AppLocalized.text("ai.membership.available", fallback: "available"))")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ProgressView(value: progress)
                .tint(usage.warningLevel == "critical" ? .red : AppColors.accent(for: colorScheme))

            if usage.warningLevel != "ok" {
                Text(
                    usage.warningLevel == "critical"
                        ? AppLocalized.text("ai.usage.warning.critical", fallback: "Quota is almost used up.")
                        : AppLocalized.text("ai.usage.warning.high", fallback: "Usage is approaching the limit.")
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
                Text("\(AppLocalized.text("ai.membership.upgrade_hint", fallback: "Upgrade hint")): \(usage.suggestedUpgrade.uppercased())")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
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
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.15), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct AIDecisionTransparencyCard: View {
    let decision: AIBotDecision
    let colorScheme: ColorScheme

    private var stateLabel: String {
        switch decision.state {
        case "faq_answer":
            return AppLocalized.text("ai.decision.state.faq_answer", fallback: "FAQ")
        case "degraded":
            return AppLocalized.text("ai.decision.state.degraded", fallback: "Degraded")
        case "blocked":
            return AppLocalized.text("ai.decision.state.blocked", fallback: "Blocked")
        case "retryable":
            return AppLocalized.text("ai.decision.state.retryable", fallback: "Retry")
        default:
            return AppLocalized.text("ai.decision.state.default", fallback: "Live")
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Text(AppLocalized.text("ai.decision.badge.why", fallback: "Why"))
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentMystic(for: colorScheme).opacity(0.12))
                    )
                Text(stateLabel)
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accent(for: colorScheme).opacity(0.12))
                    )
                Spacer(minLength: 0)
            }

            Text(
                decision.summary.isEmpty
                    ? AppLocalized.text("ai.decision.summary.placeholder", fallback: "Answer path documented.")
                    : decision.summary
            )
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            if !decision.topic.isEmpty {
                Text(String(format: AppLocalized.text("ai.decision.topic_format", fallback: "Topic: %@"), decision.topic))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if decision.fallbackActivated && !decision.fallbackReason.isEmpty {
                Text(String(format: AppLocalized.text("ai.decision.fallback_format", fallback: "Fallback: %@"), decision.fallbackReason))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if decision.responseLimited && !decision.responseLimitReason.isEmpty {
                Text(String(format: AppLocalized.text("ai.decision.limit_format", fallback: "Limit: %@"), decision.responseLimitReason))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if decision.blocked && !decision.blockReason.isEmpty {
                Text(String(format: AppLocalized.text("ai.decision.block_format", fallback: "Block: %@"), decision.blockReason))
                    .font(.caption)
                    .foregroundColor(.red)
            }

            if decision.retryable && !decision.retryReason.isEmpty {
                Text(String(format: AppLocalized.text("ai.decision.retry_format", fallback: "Retry: %@"), decision.retryReason))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if !decision.trace.isEmpty {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    ForEach(decision.trace.prefix(3), id: \.self) { item in
                        Text(item)
                            .font(.caption2)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
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
            .background(AppColors.accentMystic(for: colorScheme).opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
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
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

struct AIMembershipSheet: View {
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
                Text(AppLocalized.text("ai.membership.sheet.subtitle", fallback: "Capability-first. No credit shop."))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                AIPlanTile(title: AppLocalized.text("membership.plan.free", fallback: "Free"), detail: AppLocalized.text("ai.membership.free_detail", fallback: "Core bot, fewer images, light agent"), colorScheme: colorScheme)
                AIPlanTile(title: AppLocalized.text("membership.plan.pro", fallback: "Pro"), detail: AppLocalized.text("ai.membership.pro_detail", fallback: "More reach, stronger agent, creator daily flow"), colorScheme: colorScheme)
                AIPlanTile(title: AppLocalized.text("membership.plan.creator", fallback: "Creator"), detail: AppLocalized.text("ai.membership.creator_detail", fallback: "Workflow depth, premium outputs, priority"), colorScheme: colorScheme)
                Text(AppLocalized.text("ai.membership.annual_coming", fallback: "Annual option is coming next in native billing."))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                SkydownBrandActionButton(
                    title: activePurchasePlan == .creator
                        ? AppLocalized.text("membership.pro.starting", fallback: "Starting Pro...")
                        : AppLocalized.text("membership.pro.activate", fallback: "Activate Pro"),
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
                        ? AppLocalized.text("membership.creator.starting", fallback: "Starting Creator...")
                        : AppLocalized.text("membership.creator.activate", fallback: "Activate Creator"),
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
                        title: AppLocalized.text("membership.restore", fallback: "Restore purchases"),
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
                        title: AppLocalized.text("membership.manage", fallback: "Manage subscription"),
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
            .navigationTitle(AppLocalized.text("ai.membership.sheet.title", fallback: "SkyOS AI Membership"))
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
}

private extension AIView {
    var navigationContent: some View {
        NavigationStack {
            content
                .navigationTitle(AppLocalized.text("ai.bot.nav.title", fallback: "Bot"))
                .skydownNavigationChrome(colorScheme: colorScheme)
        }
    }
}

private struct AIPlanTile: View {
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
        .padding(SkydownLayout.stackSpacingPill)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct AIEmptyStateHeader: View {
    let colorScheme: ColorScheme
    var isCompact: Bool = false

    var body: some View {
        let accent = AppColors.accent(for: colorScheme)
        let mystic = AppColors.accentMystic(for: colorScheme)

        VStack(alignment: .leading, spacing: isCompact ? SkydownLayout.stackSpacingCompact : SkydownLayout.stackSpacingRelaxed) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
                ZStack {
                    RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [
                                    accent.opacity(0.92),
                                    mystic.opacity(0.82)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )

                    Image(systemName: "sparkles")
                        .font(.title3.weight(.black))
                        .foregroundColor(.white)
                }
                .frame(width: isCompact ? 48 : 54, height: isCompact ? 48 : 54)
                .shadow(color: accent.opacity(0.22), radius: 14, y: 8)

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSubtle) {
                    Text(AppLocalized.text("ai.bot.hero.title", fallback: "SkyOS AI"))
                        .font(
                            isCompact
                                ? .system(size: 25, weight: .black, design: .rounded)
                                : .system(size: 30, weight: .black, design: .rounded)
                        )
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(AppLocalized.text("ai.bot.hero.subtitle", fallback: "Copy, release ideas, and visual prompts in one calm flow."))
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                AIStatusChip(
                    text: AppLocalized.text("ai.bot.chip.live", fallback: "Live"),
                    accent: accent,
                    colorScheme: colorScheme
                )
            }

            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                AIStatusChip(text: AppLocalized.text("ai.bot.chip.text_mode", fallback: "Text"), accent: accent, colorScheme: colorScheme)
                AIStatusChip(text: AppLocalized.text("ai.bot.chip.visual_mode", fallback: "Visual"), accent: mystic, colorScheme: colorScheme)
                AIStatusChip(text: AppLocalized.text("ai.bot.chip.memory", fallback: "Memory"), accent: AppColors.spotify(for: colorScheme), colorScheme: colorScheme)
            }
        }
        .padding(isCompact ? SkydownLayout.compactRadius : SkydownLayout.cardPadding)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme).opacity(0.96),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.88),
                    accent.opacity(colorScheme == .dark ? 0.12 : 0.06)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.spotlightRadius, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [
                            .white.opacity(colorScheme == .dark ? 0.12 : 0.72),
                            accent.opacity(0.18),
                            mystic.opacity(0.12)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1
                )
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.spotlightRadius, style: .continuous))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.18 : 0.06), radius: 18, y: 10)
    }
}

private struct AIStatusChip: View {
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

private struct AIDisabledCard: View {
    @Environment(\.accessibilityReduceMotion) private var accessibilityReduceMotion
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: "lock.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                Text(AppLocalized.text("ai.bot.paused", fallback: "SkyOS Bot paused"))
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
                Text(AppLocalized.text("common.status.idle", fallback: "Idle"))
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accent(for: colorScheme).opacity(0.12))
                    )
            }

            Text(AppLocalized.text("ai.bot.paused.detail", fallback: "The rest of the app stays available. The bot continues once re-enabled."))
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
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

private struct AIQuickPromptCard: View {
    let colorScheme: ColorScheme
    var showsInlineHeading: Bool = true
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            if showsInlineHeading {
                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                    AIStatusChip(
                        text: AppLocalized.text("ai.bot.chip.text_mode", fallback: "Text"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme
                    )
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                        Text(AppLocalized.text("ai.quick_prompts.title", fallback: "Prompts"))
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                        Text(AppLocalized.text("ai.quick_prompts.hint", fallback: "Quick to dive in."))
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
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
                                            AppColors.accent(for: colorScheme).opacity(0.08)
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                                )
                        })
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                    }
                }
            }
        }
        .padding(.vertical, 2)
    }
}

private struct AIVisualPromptCard: View {
    let colorScheme: ColorScheme
    var showsInlineHeading: Bool = true
    let prompts: [AIVisualPrompt]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            if showsInlineHeading {
                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                    AIStatusChip(
                        text: AppLocalized.text("ai.bot.chip.visual_mode", fallback: "Visual"),
                        accent: AppColors.accentHighlight(for: colorScheme),
                        colorScheme: colorScheme
                    )
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                        Text(AppLocalized.text("ai.visual_prompts.title", fallback: "Visuals"))
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                        Text(AppLocalized.text("ai.visual_prompts.hint", fallback: "One prompt is enough."))
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    ForEach(prompts) { prompt in
                        Button(action: { onPromptSelected(prompt.prompt) }, label: {
                            Text(prompt.label)
                                .font(.subheadline.weight(.semibold))
                                .multilineTextAlignment(.leading)
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .frame(width: 170, alignment: .leading)
                                .background(
                                    LinearGradient(
                                        colors: [
                                            AppColors.cardBackground(for: colorScheme).opacity(0.96),
                                            AppColors.accentHighlight(for: colorScheme).opacity(0.1)
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                                        .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.14), lineWidth: 1)
                                )
                        })
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                    }
                }
            }
        }
        .padding(.vertical, 2)
    }
}

private struct AIMessageBubble: View {
    let message: AIChatMessage
    let colorScheme: ColorScheme
    @State private var showingShareSheet = false
    @State private var shareItems: [Any] = []
    @State private var showCopiedFeedback = false

    private var isUser: Bool {
        message.role == .user
    }

    var body: some View {
        HStack {
            if isUser { Spacer(minLength: 48) }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                Text(
                    isUser
                        ? AppLocalized.text("agent.bubble.user", fallback: "You")
                        : AppLocalized.text("ai.bot.bubble.assistant", fallback: "SkyOS Bot")
                )
                    .font(.caption.weight(.bold))
                    .foregroundColor(isUser ? .white.opacity(0.9) : AppColors.accent(for: colorScheme))

                if message.isStreaming && message.text.isEmpty {
                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        ProgressView()
                            .tint(AppColors.accent(for: colorScheme))

                        Text(AppLocalized.text("ai.bot.progress.building", fallback: "SkyOS Bot is drafting a calm answer…"))
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else {
                    Text(message.text)
                        .font(.body)
                        .foregroundColor(isUser ? .white : AppColors.text(for: colorScheme))

                    if let imageData = message.imageData,
                       let image = UIImage(data: imageData) {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 220)
                            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
                            .padding(.top, 4)
                    }

                    if !isUser {
                        HStack(spacing: SkydownLayout.stackSpacingMicro) {
                            SkydownBrandActionButton(
                                title: showCopiedFeedback
                                    ? AppLocalized.text("agent.bubble.copied", fallback: "Copied")
                                    : AppLocalized.text("agent.bubble.copy", fallback: "Copy"),
                                systemImage: "doc.on.doc",
                                accent: AppColors.accent(for: colorScheme),
                                colorScheme: colorScheme,
                                role: .muted,
                                font: .caption.weight(.semibold),
                                cornerRadius: SkydownLayout.tightRadius,
                                verticalPadding: 8,
                                expandToFullWidth: false,
                                action: {
                                    UIPasteboard.general.string = message.text
                                    showCopiedFeedback = true
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                                        showCopiedFeedback = false
                                    }
                                }
                            )
                            .skydownInteractiveFeedback()

                            SkydownBrandActionButton(
                                title: AppLocalized.text("ai.bot.action.share_save", fallback: "Share / save"),
                                systemImage: "square.and.arrow.up",
                                accent: AppColors.accentMystic(for: colorScheme),
                                colorScheme: colorScheme,
                                role: .muted,
                                font: .caption.weight(.semibold),
                                cornerRadius: SkydownLayout.tightRadius,
                                verticalPadding: 8,
                                expandToFullWidth: false,
                                action: {
                                    shareItems = sharePayload()
                                    showingShareSheet = true
                                }
                            )
                            .skydownInteractiveFeedback()
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
                            : AppColors.accent(for: colorScheme).opacity(0.14),
                        lineWidth: 1
                    )
            )

            if !isUser { Spacer(minLength: 48) }
        }
        .sheet(isPresented: $showingShareSheet) {
            AIShareSheet(activityItems: shareItems)
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

    private func sharePayload() -> [Any] {
        var items: [Any] = [message.text]
        if let imageData = message.imageData,
           let image = UIImage(data: imageData) {
            items.append(image)
        }
        return items
    }
}

private struct AIThreadFollowUpBar: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    let isWorking: Bool
    let onSend: () -> Void
    let onOpenFullComposer: () -> Void
    let canSend: () -> Bool
    @FocusState private var isFocused: Bool

    private func submit() {
        guard canSend(), !isWorking else { return }
        isFocused = false
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        onSend()
    }

    var body: some View {
        HStack(alignment: .bottom, spacing: 10) {
            TextField(
                "",
                text: $draft,
                prompt: Text(
                    AppLocalized.text(
                        "ai.thread.followup.placeholder",
                        fallback: "Continue in this chat..."
                    )
                )
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.78)),
                axis: .vertical
            )
            .lineLimit(1...4)
            .focused($isFocused)
            .submitLabel(.send)
            .onSubmit(submit)
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
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.2), lineWidth: 1)
            )
            .accessibilityIdentifier("ai.thread.followup.field")

            Button(action: onOpenFullComposer) {
                Image(systemName: "slider.horizontal.3")
                    .font(.body.weight(.semibold))
            }
            .buttonStyle(.plain)
            .foregroundColor(AppColors.accent(for: colorScheme))
            .accessibilityLabel(
                AppLocalized.text("ai.thread.open_full_composer.a11y", fallback: "Mode and details")
            )

            Button(action: submit) {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 30, weight: .semibold))
            }
            .buttonStyle(.plain)
            .foregroundColor(
                canSend() && !isWorking
                    ? AppColors.accent(for: colorScheme)
                    : AppColors.secondaryText(for: colorScheme).opacity(0.35)
            )
            .disabled(!canSend() || isWorking)
            .accessibilityLabel(AppLocalized.text("ai.thread.send.a11y", fallback: "Send"))
        }
    }
}

private struct AIPromptFab: View {
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
                        Image(systemName: "sparkles")
                            .font(.subheadline.weight(.black))
                    }
                }

                Text(
                    isWorking
                        ? AppLocalized.text("ai.fab.state.working", fallback: "Working")
                        : AppLocalized.text("ai.fab.state.prompt", fallback: "Prompt")
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
        .accessibilityLabel(AppLocalized.text("agent.a11y.open_prompt", fallback: "Open prompt"))
    }
}

private struct AIInlineNotice: View {
    @Binding var isPresented: Bool
    let message: String
    let style: ToastStyle
    let colorScheme: ColorScheme
    @Environment(\.accessibilityReduceMotion) private var accessibilityReduceMotion

    var body: some View {
        Group {
            if isPresented && !message.isEmpty {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    Image(systemName: style.icon)
                        .font(.subheadline.weight(.black))
                        .foregroundColor(style.color)
                        .frame(width: 28, height: 28)
                        .background(
                            Circle()
                                .fill(style.color.opacity(0.12))
                        )

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                        Text(style.title)
                            .font(.caption2.weight(.black))
                            .foregroundColor(style.color)
                            .lineLimit(1)

                        Text(message)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .lineLimit(2)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .frame(maxWidth: 330, alignment: .leading)
                .background(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                        .stroke(style.color.opacity(0.22), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
                .shadow(color: style.color.opacity(0.16), radius: 18, y: 8)
                .transition(
                    accessibilityReduceMotion
                        ? .opacity
                        : .move(edge: .bottom)
                            .combined(with: .opacity)
                            .combined(with: .scale(scale: 0.97, anchor: .bottom))
                )
                .task(id: message + style.title) {
                    SkydownHaptics.announce(message)
                    switch style {
                    case .success:
                        SkydownHaptics.notification(.success)
                    case .warning:
                        SkydownHaptics.notification(.warning)
                    case .error:
                        SkydownHaptics.notification(.error)
                    case .info:
                        SkydownHaptics.impact(.soft)
                    }

                    let duration = min(max(2.4, Double(message.count) / 22.0), 5.2)
                    try? await Task.sleep(for: .seconds(duration))
                    guard !Task.isCancelled else { return }
                    await MainActor.run {
                        withAnimation(
                            SkydownMotion.preferredStatusTransition(
                                accessibilityReduceMotion: accessibilityReduceMotion
                            )
                        ) {
                            isPresented = false
                        }
                    }
                }
            }
        }
        .animation(
            SkydownMotion.preferredStatusTransition(accessibilityReduceMotion: accessibilityReduceMotion),
            value: isPresented
        )
        .accessibilityElement(children: .combine)
    }
}

private struct AIPromptComposerSheet: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var composerMode: AIComposerMode
    @Binding var textMode: AITextMode
    @Binding var selectedLevel: AIExperienceLevel
    let interactionPhase: BotInteractionPhase
    let quickPrompts: [String]
    let visualPrompts: [AIVisualPrompt]
    let onDismiss: () -> Void
    let onSend: () -> Void
    @FocusState private var isFocused: Bool

    private var trimmedDraft: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var composerAccent: Color {
        composerMode == .visual
            ? AppColors.accentMystic(for: colorScheme)
            : AppColors.accent(for: colorScheme)
    }

    @ViewBuilder
    private var aiSettingsDropdownRows: some View {
        HStack(alignment: .center) {
            Text(AppLocalized.text("ai.composer.format", fallback: "Format"))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Spacer(minLength: 12)
            Picker(AppLocalized.text("ai.composer.format", fallback: "Format"), selection: $composerMode) {
                ForEach(AIComposerMode.allCases) { mode in
                    Text(mode.title).tag(mode)
                }
            }
            .labelsHidden()
            .pickerStyle(.menu)
            .tint(composerAccent)
            .disabled(interactionPhase.isBusy)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)

        if composerMode == .text {
            Divider()
                .padding(.leading, 8)
            HStack(alignment: .center) {
                Text(AppLocalized.text("ai.composer.tone", fallback: "Tone"))
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                Spacer(minLength: 12)
                Picker(AppLocalized.text("ai.composer.tone", fallback: "Tone"), selection: $textMode) {
                    ForEach(AITextMode.allCases) { mode in
                        Text(mode.title).tag(mode)
                    }
                }
                .labelsHidden()
                .pickerStyle(.menu)
                .tint(composerAccent)
                .disabled(interactionPhase.isBusy)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }

        Divider()
            .padding(.leading, 8)

        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
            HStack(alignment: .center) {
                Text(AppLocalized.text("ai.level.picker.title", fallback: "Tiefe"))
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                Spacer(minLength: 12)
                Picker(AppLocalized.text("ai.composer.picker.depth", fallback: "Depth"), selection: $selectedLevel) {
                    ForEach(AIExperienceLevel.allCases) { level in
                        Text(level.title).tag(level)
                    }
                }
                .labelsHidden()
                .pickerStyle(.menu)
                .tint(composerAccent)
                .disabled(interactionPhase.isBusy)
            }
            Text(selectedLevel.subtitle)
                .font(.caption2)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .lineLimit(3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSection) {
                PremiumPromptSheetHeader(
                    iconSystemName: composerMode == .visual ? "camera.aperture" : "text.bubble.fill",
                    title: AppLocalized.text("ai.composer.title", fallback: "New request"),
                    subtitle: AppLocalized.text("ai.composer.subtitle", fallback: "Everything below is dropdowns — one send is enough."),
                    accent: composerAccent,
                    colorScheme: colorScheme,
                    onDismiss: onDismiss
                )

                if let status = interactionPhase.composerStatusLabel {
                    Text(status)
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                        .padding(SkydownLayout.compactRadius)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                                .fill(AppColors.accentMystic(for: colorScheme).opacity(0.1))
                        )
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    PremiumPromptSectionHeader(
                        title: AppLocalized.text("ai.composer.section.settings", fallback: "Settings"),
                        footnote: AppLocalized.text("ai.composer.section.settings_footnote", fallback: "Compact menus — pick, tap, send."),
                        accent: composerAccent,
                        colorScheme: colorScheme
                    )
                    PremiumPromptSettingsDropdownCard(
                        colorScheme: colorScheme,
                        emphasisAccent: composerAccent
                    ) {
                        aiSettingsDropdownRows
                    }
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    PremiumPromptSectionHeader(
                        title: AppLocalized.text("ai.composer.section.inspiration", fallback: "Inspiration"),
                        footnote: AppLocalized.text("ai.composer.section.inspiration_footnote", fallback: "Quick starters — tap and adjust."),
                        accent: composerAccent,
                        colorScheme: colorScheme
                    )
                    if composerMode == .text {
                        AIQuickPromptCard(
                            colorScheme: colorScheme,
                            showsInlineHeading: false,
                            prompts: quickPrompts,
                            onPromptSelected: { prompt in
                                draft = prompt
                            }
                        )
                    } else {
                        AIVisualPromptCard(
                            colorScheme: colorScheme,
                            showsInlineHeading: false,
                            prompts: visualPrompts,
                            onPromptSelected: { prompt in
                                draft = prompt
                            }
                        )
                    }
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    PremiumPromptSectionHeader(
                        title: AppLocalized.text("ai.composer.section.input", fallback: "Input"),
                        footnote: AppLocalized.text("ai.composer.section.input_footnote", fallback: "Write clearly — the more precise, the better the result."),
                        accent: composerAccent,
                        colorScheme: colorScheme
                    )
                    PremiumPromptCard(colorScheme: colorScheme) {
                        TextField(
                            composerMode == .text
                                ? textMode.placeholder
                                : AppLocalized.text(
                                    "ai.composer.visual_placeholder",
                                    fallback: "Describe scene light, mood, style. One sentence is enough to start."
                                ),
                            text: $draft,
                            axis: .vertical
                        )
                        .lineLimit(4...9)
                        .focused($isFocused)
                        .submitLabel(.send)
                        .onSubmit {
                            if !trimmedDraft.isEmpty && !interactionPhase.isBusy {
                                onSend()
                            }
                        }
                        .font(.body)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    }
                }

                PremiumPromptPrimaryButton(
                    title: composerMode == .text
                        ? AppLocalized.text("ai.composer.send", fallback: "Send")
                        : AppLocalized.text("ai.composer.generate", fallback: "Generate"),
                    systemImage: composerMode == .text ? "arrow.up.circle.fill" : "sparkles",
                    accent: composerAccent,
                    colorScheme: colorScheme,
                    isEnabled: !trimmedDraft.isEmpty && !interactionPhase.isBusy,
                    action: onSend
                )
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.top, 10)
            .padding(.bottom, 28)
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

private struct AIBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.accent(for: colorScheme)
        )
    }
}

private struct AISessionSignalCard: View {
    let title: String
    let value: String
    let detail: String
    let accent: Color
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSubtle) {
            Text(title.uppercased())
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(value)
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)

            Text(detail)
                .font(.caption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .lineLimit(1)
        }
        .frame(width: 142, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(accent.opacity(0.16), lineWidth: 1)
        )
    }
}

#Preview {
    let services = AppServices()

    AIView(
        aiChatService: services.aiChatService,
        featureFlags: services.featureFlags,
        membershipCoordinator: AIMembershipCoordinator()
    )
}
