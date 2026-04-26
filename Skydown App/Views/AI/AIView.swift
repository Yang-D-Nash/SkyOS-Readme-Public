import SwiftUI
import UIKit

struct AIView: View {
    @StateObject private var viewModel: AIChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @ObservedObject private var membershipCoordinator: AIMembershipCoordinator
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
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
                onCreateNewChat: {
                    viewModel.startNewConversation()
                    showingPromptComposer = false
                },
                onSend: {
                    viewModel.sendDraft()
                    showingPromptComposer = false
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $showingConversationSessions) {
            AIConversationSessionsSheet(
                title: "Chats",
                accent: AppColors.accent(for: colorScheme),
                colorScheme: colorScheme,
                sessions: viewModel.sessions,
                activeSessionID: viewModel.activeSessionID,
                isBusy: viewModel.phase.isBusy,
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

        return ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                if featureFlags.isAIEnabled {
                    if viewModel.messages.isEmpty {
                        ScrollView {
                            VStack(alignment: .leading, spacing: usesCompactImmersiveLayout ? 12 : 14) {
                                AIConversationSessionStrip(
                                    title: viewModel.activeSessionTitle,
                                    subtitle: activeSessionSubtitle,
                                    accent: AppColors.accent(for: colorScheme),
                                    colorScheme: colorScheme,
                                    isBusy: viewModel.phase.isBusy,
                                    onOpenSessions: { showingConversationSessions = true },
                                    onCreateNewChat: viewModel.startNewConversation
                                )

                                AIEmptyStateHeader(
                                    colorScheme: colorScheme,
                                    isCompact: usesCompactImmersiveLayout
                                )

                                aiSessionDeck

                                AIQuickPromptCard(
                                    colorScheme: colorScheme,
                                    prompts: viewModel.quickPrompts,
                                    onPromptSelected: { prompt in
                                        prefillPrompt(prompt, mode: .text)
                                    }
                                )

                                AIVisualPromptCard(
                                    colorScheme: colorScheme,
                                    prompts: viewModel.visualPrompts,
                                    onPromptSelected: { prompt in
                                        prefillPrompt(prompt, mode: .visual)
                                    }
                                )

                                if let revenueUsage = viewModel.revenueUsage {
                                    AIRevenueUsageCard(
                                        usage: revenueUsage,
                                        colorScheme: colorScheme
                                    )
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                            .padding(.top, showsNavigation ? 20 : 10)
                            .padding(.bottom, usesCompactImmersiveLayout ? 16 : 28)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                        .scrollIndicators(.hidden)
                    } else {
                        ScrollViewReader { proxy in
                            let scrollToken = viewModel.messages.last.map { message in
                                "\(message.id.uuidString)-\(message.isStreaming)-\(message.text.count)-\(message.imageData?.count ?? 0)"
                            } ?? "chat-empty"

                            ScrollView {
                                LazyVStack(alignment: .leading, spacing: 10) {
                                    AIConversationSessionStrip(
                                        title: viewModel.activeSessionTitle,
                                        subtitle: activeSessionSubtitle,
                                        accent: AppColors.accent(for: colorScheme),
                                        colorScheme: colorScheme,
                                        isBusy: viewModel.phase.isBusy,
                                        onOpenSessions: { showingConversationSessions = true },
                                        onCreateNewChat: viewModel.startNewConversation
                                    )

                                    aiSessionDeck

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
                                .padding(.top, showsNavigation ? 6 : 2)
                                .padding(.bottom, 6)
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .scrollIndicators(.hidden)
                            .scrollDismissesKeyboard(.interactively)
                            .onAppear {
                                DispatchQueue.main.async {
                                    proxy.scrollTo("chat-end", anchor: .bottom)
                                }
                            }
                            .onChange(of: scrollToken) { _, _ in
                                DispatchQueue.main.async {
                                    withAnimation(.easeOut(duration: 0.25)) {
                                        proxy.scrollTo("chat-end", anchor: .bottom)
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
            .safeAreaInset(edge: .bottom, spacing: 0) {
                Color.clear
                    .frame(height: featureFlags.isAIEnabled ? 86 : 0)
                    .allowsHitTesting(false)
            }

            if featureFlags.isAIEnabled {
                VStack(alignment: .trailing, spacing: 12) {
                    AIInlineNotice(
                        isPresented: $viewModel.showToast,
                        message: viewModel.toastMessage,
                        style: viewModel.toastStyle,
                        colorScheme: colorScheme
                    )

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
            HStack(spacing: 10) {
                AISessionSignalCard(
                    title: "Mode",
                    value: viewModel.composerMode.title,
                    detail: viewModel.composerMode == .visual ? "Bild-Generierung" : "Text-Produktion",
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme
                )
                AISessionSignalCard(
                    title: "Focus",
                    value: viewModel.composerMode == .visual ? "Cinematic" : viewModel.textMode.title,
                    detail: viewModel.composerMode == .visual ? "Prompt -> Visual" : "Textmodus direkt steuerbar",
                    accent: AppColors.accentMystic(for: colorScheme),
                    colorScheme: colorScheme
                )
                AISessionSignalCard(
                    title: "Memory",
                    value: viewModel.messages.isEmpty ? "Neu" : "\(viewModel.messages.count) Steps",
                    detail: "Verlauf bleibt pro Konto aktiv",
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
        withAnimation(SkydownMotion.sheetPresentation) {
            showingPromptComposer = true
        }
    }
}

private extension AIView {
    func l(_ key: String, _ fallback: String) -> String {
        AppLocalized.text(key, fallback: fallback)
    }
}

private struct AIPlanPreviewCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
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
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
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
        VStack(alignment: .leading, spacing: 8) {
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
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.15), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct AIDecisionTransparencyCard: View {
    let decision: AIBotDecision
    let colorScheme: ColorScheme

    private var stateLabel: String {
        switch decision.state {
        case "faq_answer":
            return "FAQ"
        case "degraded":
            return "Degraded"
        case "blocked":
            return "Blocked"
        case "retryable":
            return "Retry"
        default:
            return "Live"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text("Why")
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

            Text(decision.summary.isEmpty ? "Antwortpfad dokumentiert." : decision.summary)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            if !decision.topic.isEmpty {
                Text("Topic: \(decision.topic)")
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if decision.fallbackActivated && !decision.fallbackReason.isEmpty {
                Text("Fallback: \(decision.fallbackReason)")
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if decision.responseLimited && !decision.responseLimitReason.isEmpty {
                Text("Limit: \(decision.responseLimitReason)")
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if decision.blocked && !decision.blockReason.isEmpty {
                Text("Block: \(decision.blockReason)")
                    .font(.caption)
                    .foregroundColor(.red)
            }

            if decision.retryable && !decision.retryReason.isEmpty {
                Text("Retry: \(decision.retryReason)")
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if !decision.trace.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
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
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
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
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
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
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
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
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
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

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(AppLocalized.text("ai.membership.sheet.title", fallback: "SkyOS AI Membership"))
                    .font(.title3.weight(.black))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text(AppLocalized.text("ai.membership.sheet.subtitle", fallback: "Capability-first. No credit shop."))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                AIPlanTile(title: AppLocalized.text("membership.plan.free", fallback: "Free"), detail: AppLocalized.text("ai.membership.free_detail", fallback: "Core bot, fewer images, light agent"), colorScheme: colorScheme)
                AIPlanTile(title: AppLocalized.text("membership.plan.pro", fallback: "Pro"), detail: AppLocalized.text("ai.membership.pro_detail", fallback: "More reach, stronger agent, creator daily flow"), colorScheme: colorScheme)
                AIPlanTile(title: AppLocalized.text("membership.plan.creator", fallback: "Creator"), detail: AppLocalized.text("ai.membership.creator_detail", fallback: "Workflow depth, premium outputs, priority"), colorScheme: colorScheme)
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

private extension AIView {
    var navigationContent: some View {
        NavigationStack {
            content
                .navigationTitle("Bot")
                .skydownNavigationChrome(colorScheme: colorScheme)
        }
    }
}

private struct AIPlanTile: View {
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
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct AIEmptyStateHeader: View {
    let colorScheme: ColorScheme
    var isCompact: Bool = false

    var body: some View {
        let accent = AppColors.accent(for: colorScheme)
        let mystic = AppColors.accentMystic(for: colorScheme)

        VStack(alignment: .leading, spacing: isCompact ? 12 : 14) {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
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

                VStack(alignment: .leading, spacing: 5) {
                    Text("SkyOS AI")
                        .font(
                            isCompact
                                ? .system(size: 25, weight: .black, design: .rounded)
                                : .system(size: 30, weight: .black, design: .rounded)
                        )
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Copy, Release-Ideen und Visual-Prompts in einem ruhigen Flow.")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                AIStatusChip(
                    text: "Live",
                    accent: accent,
                    colorScheme: colorScheme
                )
            }

            HStack(spacing: 8) {
                AIStatusChip(text: "Text", accent: accent, colorScheme: colorScheme)
                AIStatusChip(text: "Visual", accent: mystic, colorScheme: colorScheme)
                AIStatusChip(text: "Memory", accent: AppColors.spotify(for: colorScheme), colorScheme: colorScheme)
            }
        }
        .padding(isCompact ? 14 : 16)
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
            RoundedRectangle(cornerRadius: 24, style: .continuous)
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
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
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
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
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
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .transition(.opacity.combined(with: .move(edge: .top)))
        .animation(SkydownMotion.statusTransition, value: colorScheme)
    }
}

private struct AIQuickPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                AIStatusChip(
                    text: "Text",
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme
                )
                VStack(alignment: .leading, spacing: 2) {
                    Text("Prompts")
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("Schnell rein.")
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
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
                                            AppColors.accent(for: colorScheme).opacity(0.08)
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous))
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous)
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
    let prompts: [AIVisualPrompt]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                AIStatusChip(
                    text: "Visual",
                    accent: AppColors.accentHighlight(for: colorScheme),
                    colorScheme: colorScheme
                )
                VStack(alignment: .leading, spacing: 2) {
                    Text("Visuals")
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("Ein Prompt reicht.")
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
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
                                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous))
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous)
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
    @State private var copyLabel = "Kopieren"

    private var isUser: Bool {
        message.role == .user
    }

    var body: some View {
        HStack {
            if isUser { Spacer(minLength: 48) }

            VStack(alignment: .leading, spacing: 8) {
                Text(isUser ? "Du" : "SkyOS Bot")
                    .font(.caption.weight(.bold))
                    .foregroundColor(isUser ? .white.opacity(0.9) : AppColors.accent(for: colorScheme))

                if message.isStreaming && message.text.isEmpty {
                    HStack(spacing: 10) {
                        ProgressView()
                            .tint(AppColors.accent(for: colorScheme))

                        Text("SkyOS Bot baut gerade eine ruhige Antwort auf...")
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
                            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius))
                            .padding(.top, 4)
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
                            .foregroundColor(AppColors.accent(for: colorScheme))

                            Button("Teilen / Speichern") {
                                shareItems = sharePayload()
                                showingShareSheet = true
                            }
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accentMystic(for: colorScheme))
                        }
                        .padding(.top, 8)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: 360, alignment: .leading)
            .background(bubbleBackground)
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
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

private struct AIPromptFab: View {
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
                        Image(systemName: "sparkles")
                            .font(.subheadline.weight(.black))
                    }
                }

                Text(isWorking ? "Arbeitet" : "Prompt")
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
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(.white.opacity(0.18), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .shadow(color: .black.opacity(0.16), radius: 18, x: 0, y: 10)
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .accessibilityLabel("Prompt oeffnen")
    }
}

private struct AIInlineNotice: View {
    @Binding var isPresented: Bool
    let message: String
    let style: ToastStyle
    let colorScheme: ColorScheme

    var body: some View {
        Group {
            if isPresented && !message.isEmpty {
                HStack(spacing: 10) {
                    Image(systemName: style.icon)
                        .font(.subheadline.weight(.black))
                        .foregroundColor(style.color)
                        .frame(width: 28, height: 28)
                        .background(
                            Circle()
                                .fill(style.color.opacity(0.12))
                        )

                    VStack(alignment: .leading, spacing: 2) {
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
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(style.color.opacity(0.22), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .shadow(color: style.color.opacity(0.16), radius: 18, y: 8)
                .transition(
                    .move(edge: .bottom)
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
                        withAnimation(SkydownMotion.statusTransition) {
                            isPresented = false
                        }
                    }
                }
            }
        }
        .animation(SkydownMotion.statusTransition, value: isPresented)
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
    let onCreateNewChat: () -> Void
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

    var body: some View {
        ScrollView {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center, spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(composerAccent.opacity(0.14))
                    Image(systemName: composerMode == .visual ? "camera.aperture" : "text.bubble.fill")
                        .font(.headline.weight(.black))
                        .foregroundColor(composerAccent)
                }
                .frame(width: 48, height: 48)

                VStack(alignment: .leading, spacing: 3) {
                    Text("Neue AI-Anfrage")
                        .font(.title3.weight(.black))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("Optionen waehlen, Prompt schaerfen, senden.")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer(minLength: 0)

                AIStatusChip(
                    text: composerMode.title,
                    accent: composerAccent,
                    colorScheme: colorScheme
                )
            }

            if let status = interactionPhase.composerStatusLabel {
                Text(status)
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }

            Text("Optionen")
                .font(.caption2.weight(.black))
                .foregroundColor(composerAccent)

            Picker("Modus", selection: $composerMode) {
                ForEach(AIComposerMode.allCases) { mode in
                    Text(mode.title).tag(mode)
                }
            }
            .pickerStyle(.segmented)
            .disabled(interactionPhase.isBusy)
            .skydownSelectionFeedback(trigger: composerMode)

            if composerMode == .text {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(AITextMode.allCases) { mode in
                            Button {
                                textMode = mode
                            } label: {
                                Text(mode.title)
                                    .font(.caption.weight(.bold))
                                    .foregroundColor(textMode == mode ? .white : AppColors.text(for: colorScheme))
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(
                                        Capsule()
                                            .fill(textMode == mode ? composerAccent : AppColors.secondaryBackground(for: colorScheme))
                                    )
                            }
                            .buttonStyle(.plain)
                            .skydownTactileAction()
                        }
                    }
                }
                .skydownSelectionFeedback(trigger: textMode)
            }

            VStack(alignment: .leading, spacing: 5) {
                Picker(AppLocalized.text("ai.level.picker.title", fallback: "AI Level"), selection: $selectedLevel) {
                    ForEach(AIExperienceLevel.allCases) { level in
                        Text(level.title).tag(level)
                    }
                }
                .pickerStyle(.segmented)
                .disabled(interactionPhase.isBusy)
                .skydownSelectionFeedback(trigger: selectedLevel)

                Text(selectedLevel.subtitle)
                    .font(.caption2.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(2)
            }

            Text("Prompt")
                .font(.caption2.weight(.black))
                .foregroundColor(composerAccent)

            TextField(
                composerMode == .text
                    ? textMode.placeholder
                    : "Zum Beispiel: Dunkles Cover-Art fuer einen neuen Release.",
                text: $draft,
                axis: .vertical
            )
            .lineLimit(4...8)
            .focused($isFocused)
            .submitLabel(.send)
            .onSubmit {
                if !trimmedDraft.isEmpty && !interactionPhase.isBusy {
                    onSend()
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                    .fill(AppColors.secondaryBackground(for: colorScheme))
            )
            .foregroundColor(AppColors.text(for: colorScheme))

            HStack(spacing: 10) {
                Spacer(minLength: 0)
                Button("Neuer Chat", action: onCreateNewChat)
                    .font(.caption.weight(.bold))
                    .disabled(interactionPhase.isBusy)

                Button(action: onSend) {
                    Image(systemName: composerMode == .text ? "arrow.up.circle.fill" : "sparkles")
                        .font(.title2.weight(.bold))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                        .background(composerAccent)
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .skydownTactileAction()
                .disabled(trimmedDraft.isEmpty || interactionPhase.isBusy)
                .opacity(trimmedDraft.isEmpty || interactionPhase.isBusy ? 0.55 : 1)
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
        VStack(alignment: .leading, spacing: 5) {
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
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
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
