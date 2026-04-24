import SwiftUI
import UIKit

struct AgentView: View {
    @StateObject private var viewModel: AgentChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @ObservedObject private var membershipCoordinator: AIMembershipCoordinator
    @ObservedObject private var manusByosStore = ManusBYOSStore.shared
    @ObservedObject private var workflowAutomationSettingsStore = WorkflowAutomationSettingsStore.shared
    @ObservedObject private var aiRuntimeSettingsStore = AIRuntimeSettingsStore.shared
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @FocusState private var isComposerFocused: Bool
    @StateObject private var keyboardObserver = SkydownKeyboardObserver()
    @State private var composerBarHeight: CGFloat = 0
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
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button(AppLocalized.text("common.done", fallback: "Done")) {
                    isComposerFocused = false
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .task(id: sessionObservationKey) {
            viewModel.configureUser(user: authManager.userSession)
            configureIntegrationObservations(for: authManager.userSession)
        }
        .onChange(of: featureFlags.isAIEnabled) { _, isEnabled in
            aiRuntimeSettingsStore.setObservationEnabled(isEnabled)
            workflowAutomationSettingsStore.configureObservation(
                isEnabled: isEnabled,
                userID: authManager.userSession?.id
            )
        }
        .onDisappear {
            isComposerFocused = false
        }
        .onChange(of: viewModel.revenueUsage?.warningLevel) { _, level in
            handleCriticalUsageWarning(level)
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

    /// Single line for the workspace status without exposing backend routing.
    private var agentProviderStatusLine: String {
        let hasRoutingNotice = !viewModel.lastProviderNotice.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        return AppLocalized.text(
            hasRoutingNotice ? "agent.workspace.status.line.adjusted" : "agent.workspace.status.line.ready",
            fallback: hasRoutingNotice ? "Agent · Workspace adjusted" : "Agent · Workspace"
        )
    }

    private var agentWorkspaceHeroBadges: [String] {
        [agentProviderStatusLine, "Modus: \(viewModel.selectedMode.title)"]
    }

    private var usesCompactImmersiveLayout: Bool {
        !showsNavigation
    }

    private var composerKeyboardOffset: CGFloat {
        isComposerFocused ? keyboardObserver.bottomInset : 0
    }

    private var composerReservedBottomSpace: CGFloat {
        featureFlags.isAIEnabled ? composerBarHeight + composerKeyboardOffset : 0
    }

    private var content: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                if featureFlags.isAIEnabled {
                    if viewModel.messages.isEmpty {
                        ScrollView {
                            VStack(alignment: .leading, spacing: usesCompactImmersiveLayout ? 12 : 14) {
                                if !usesCompactImmersiveLayout {
                                    AgentHeroCard(colorScheme: colorScheme, badges: agentWorkspaceHeroBadges)
                                }

                                AgentEmptyStateHeader(
                                    colorScheme: colorScheme,
                                    isCompact: usesCompactImmersiveLayout
                                )

                                AgentQuickPromptCard(
                                    colorScheme: colorScheme,
                                    prompts: viewModel.quickPrompts,
                                    onPromptSelected: { prompt in
                                        isComposerFocused = false
                                        viewModel.sendPrompt(prompt)
                                    }
                                )

                                if let usage = viewModel.revenueUsage {
                                    AgentRevenueUsageCard(usage: usage, colorScheme: colorScheme)
                                        .onTapGesture {
                                            if !usage.userFacingReason.isEmpty {
                                                MembershipAnalyticsTracker().track(
                                                    "upgrade_after_deny",
                                                    reason: membershipCoordinator.lastOpenReason.rawValue,
                                                    surface: "agent_empty",
                                                    currentPlan: membershipCoordinator.currentPlanCache.rawValue
                                                )
                                            }
                                            membershipCoordinator.openMembership(reason: .manual, surface: "agent_empty")
                                        }
                                } else {
                                    AgentPlanPreviewCard(colorScheme: colorScheme)
                                        .onTapGesture { membershipCoordinator.openMembership(reason: .manual, surface: "agent_empty") }
                                }

                                if !usesCompactImmersiveLayout {
                                    serviceStatusCard
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                            .padding(.top, showsNavigation ? 8 : 6)
                            .padding(.bottom, usesCompactImmersiveLayout ? 16 : 12)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                        .scrollIndicators(.hidden)
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
                                    workflowToken
                                ].joined(separator: "|")
                            } ?? "agent-chat-empty"

                            ScrollView {
                                LazyVStack(alignment: .leading, spacing: 10) {
                                    if let usage = viewModel.revenueUsage {
                                        AgentRevenueUsageCard(usage: usage, colorScheme: colorScheme)
                                            .onTapGesture {
                                                if !usage.userFacingReason.isEmpty {
                                                    MembershipAnalyticsTracker().track(
                                                        "upgrade_after_deny",
                                                        reason: membershipCoordinator.lastOpenReason.rawValue,
                                                        surface: "agent_chat",
                                                        currentPlan: membershipCoordinator.currentPlanCache.rawValue
                                                    )
                                                }
                                                membershipCoordinator.openMembership(reason: .manual, surface: "agent_chat")
                                            }
                                    }
                                    serviceStatusCard

                                    VStack(alignment: .leading, spacing: 0) {
                                        AgentWorkspaceContextCard(
                                            colorScheme: colorScheme,
                                            selectedMode: viewModel.selectedMode,
                                            messageCount: viewModel.messages.count,
                                            phase: viewModel.phase,
                                            providerLine: agentProviderStatusLine
                                        )

                                        WorkspaceSectionHeader(colorScheme: colorScheme)

                                        VStack(alignment: .leading, spacing: 10) {
                                            ForEach(viewModel.messages) { message in
                                                AgentMessageBubble(
                                                    message: message,
                                                    colorScheme: colorScheme
                                                )
                                                .id(message.id)
                                            }
                                        }
                                        .padding(.top, 4)
                                        .padding(.horizontal, 4)
                                        .padding(.bottom, 5)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .background(
                                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                                .fill(AppColors.primaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.38 : 0.5))
                                        )
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.08), lineWidth: 1)
                                        )
                                    }

                                    Color.clear
                                        .frame(height: 4)
                                        .id("agent-chat-end")
                                }
                                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                                .padding(.top, showsNavigation ? 6 : 2)
                                .padding(.bottom, 6)
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .scrollIndicators(.hidden)
                            .scrollDismissesKeyboard(.interactively)
                            .simultaneousGesture(
                                TapGesture().onEnded {
                                    isComposerFocused = false
                                }
                            )
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
                AgentComposerBar(
                    colorScheme: colorScheme,
                    draft: $viewModel.draft,
                    selectedMode: $viewModel.selectedMode,
                    selectedLevel: $viewModel.selectedLevel,
                    shouldTriggerAutomation: $viewModel.shouldTriggerAutomation,
                    canTriggerAutomation: viewModel.canTriggerAutomation,
                    isFocused: $isComposerFocused,
                    interactionPhase: viewModel.phase,
                    onReset: viewModel.resetConversation,
                    onSend: viewModel.sendDraft
                )
                .background(
                    GeometryReader { proxy in
                        Color.clear.preference(
                            key: AgentComposerBarHeightPreferenceKey.self,
                            value: proxy.size.height
                        )
                    }
                )
                .padding(.bottom, composerKeyboardOffset)
                .animation(.easeOut(duration: 0.22), value: composerKeyboardOffset)
                .zIndex(1)
            }
        }
        .onPreferenceChange(AgentComposerBarHeightPreferenceKey.self) { height in
            guard abs(composerBarHeight - height) > 0.5 else { return }
            composerBarHeight = height
        }
        .ignoresSafeArea(.keyboard, edges: .bottom)
        .background(backgroundGradient.ignoresSafeArea())
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private var backgroundGradient: LinearGradient {
        AppColors.screenGradient(
            for: colorScheme,
            secondaryAccent: AppColors.accentMystic(for: colorScheme)
        )
    }

    private var serviceStatusCard: some View {
        AgentServiceStatusCard(
            colorScheme: colorScheme,
            isOnline: NetworkStatusMonitor.shared.isOnline,
            canTriggerAutomation: viewModel.canTriggerAutomation,
            shouldTriggerAutomation: viewModel.shouldTriggerAutomation,
            runtimeSettings: aiRuntimeSettingsStore.settings,
            runtimeErrorMessage: aiRuntimeSettingsStore.lastErrorMessage,
            workflowSettings: workflowAutomationSettingsStore.settings,
            workflowErrorMessage: workflowAutomationSettingsStore.lastErrorMessage,
            manusSettings: manusByosStore.settings,
            lastAgentProvider: viewModel.lastAgentProvider,
            providerNotice: viewModel.lastProviderNotice,
            integrationIssue: viewModel.lastIntegrationIssue,
            lastAgentRunId: viewModel.lastAgentRunId
        )
    }

    private func configureIntegrationObservations(for user: User?) {
        let userID = user?.id?.trimmingCharacters(in: .whitespacesAndNewlines)
        workflowAutomationSettingsStore.configureObservation(
            isEnabled: featureFlags.isAIEnabled,
            userID: userID?.isEmpty == true ? nil : userID
        )
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
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
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
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.15), lineWidth: 1)
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
            .background(AppColors.accentHighlight(for: colorScheme).opacity(0.08))
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
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct AgentServiceStatusCard: View {
    let colorScheme: ColorScheme
    let isOnline: Bool
    let canTriggerAutomation: Bool
    let shouldTriggerAutomation: Bool
    let runtimeSettings: AIRuntimeSettings
    let runtimeErrorMessage: String?
    let workflowSettings: WorkflowAutomationSettings
    let workflowErrorMessage: String?
    let manusSettings: ManusBYOSSettings
    let lastAgentProvider: AIRuntimeAgentProvider
    let providerNotice: String
    let integrationIssue: String
    let lastAgentRunId: String

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(AppLocalized.text("agent.service.eyebrow", fallback: "Runtime & handoffs"))
                .font(.caption2.weight(.black))
                .foregroundColor(AppColors.accentMystic(for: colorScheme).opacity(0.85))

            HStack(spacing: 8) {
                Image(systemName: "point.3.connected.trianglepath.dotted")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                VStack(alignment: .leading, spacing: 2) {
                    Text(AppLocalized.text("agent.service.rail.title", fallback: "Where the work is wired in"))
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(AppLocalized.text("agent.service.rail.sub", fallback: "Workspace, automations, and your tools in one line — not a settings page."))
                        .font(.caption2.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                }
                Spacer(minLength: 0)
                AgentServicePill(
                    title: isOnline ? "Online" : "Offline",
                    tone: isOnline ? .ready : .blocked,
                    colorScheme: colorScheme
                )
            }

            HStack(spacing: 8) {
                AgentServicePill(
                    title: "Workspace: \(providerLabel)",
                    tone: providerTone,
                    colorScheme: colorScheme
                )
                AgentServicePill(
                    title: manusLabel,
                    tone: manusTone,
                    colorScheme: colorScheme
                )
            }

            HStack(spacing: 8) {
                AgentServicePill(
                    title: n8nLabel,
                    tone: n8nTone,
                    colorScheme: colorScheme
                )
                if shouldTriggerAutomation {
                    AgentServicePill(
                        title: "Agent-Aktion AN",
                        tone: n8nReady ? .ready : .blocked,
                        colorScheme: colorScheme
                    )
                }
            }

            if !lastAgentRunId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                VStack(alignment: .leading, spacing: 3) {
                    Text(AppLocalized.text("agent.service.run.caption", fallback: "Run reference (support)"))
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    Text(lastAgentRunId)
                        .font(.caption2)
                        .monospaced()
                        .foregroundColor(AppColors.text(for: colorScheme).opacity(0.92))
                        .textSelection(.enabled)
                        .accessibilityIdentifier("agent.lastRun.id")
                }
                .padding(.horizontal, 9)
                .padding(.vertical, 7)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.05))
                )
            }

            if let detail = statusDetailMessage {
                Text(detail)
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(
                        detailTone == .blocked
                            ? Color(red: 214 / 255, green: 43 / 255, blue: 84 / 255)
                            : AppColors.secondaryText(for: colorScheme)
                    )
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .transition(.opacity.combined(with: .move(edge: .top)))
        .animation(SkydownMotion.statusTransition, value: statusDetailMessage ?? "")
    }

    private var providerLabel: String {
        if let runtimeErrorMessage, !runtimeErrorMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Status unklar"
        }

        let runtimeProvider = runtimeSettings.agentProvider
        if runtimeProvider != lastAgentProvider && !providerNotice.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Fallback aktiv"
        }
        return "Aktiv"
    }

    private var providerTone: AgentServicePill.Tone {
        if runtimeErrorMessage != nil {
            return .warning
        }
        return .ready
    }

    private var manusLabel: String {
        if runtimeSettings.agentProvider != .manus {
            return "Externe Skills: aus"
        }
        if !runtimeSettings.manus.isEnabled {
            return "Externe Skills: aus"
        }
        if manusSettings.isEnabled && manusSettings.hasAPIKey {
            return "Eigener Zugang: aktiv"
        }
        if manusSettings.hasAPIKey && !manusSettings.isEnabled {
            return "Eigener Zugang: pausiert"
        }
        return "Externe Skills: vorbereitet"
    }

    private var manusTone: AgentServicePill.Tone {
        if runtimeSettings.agentProvider != .manus {
            return .neutral
        }
        if !runtimeSettings.manus.isEnabled {
            return .blocked
        }
        if manusSettings.isEnabled && manusSettings.hasAPIKey {
            return .ready
        }
        return .warning
    }

    private var n8nReady: Bool {
        canTriggerAutomation && workflowSettings.isPrepared
    }

    private var n8nLabel: String {
        if !canTriggerAutomation {
            return "Agent-Aktionen: Login fehlt"
        }
        if let workflowErrorMessage,
           !workflowErrorMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Agent-Aktionen: Status unklar"
        }
        if workflowSettings.isPrepared {
            return "Agent-Aktionen: bereit"
        }
        if workflowSettings.isEnabled {
            return "Agent-Aktionen: unvollstaendig"
        }
        return "Agent-Aktionen: aus"
    }

    private var n8nTone: AgentServicePill.Tone {
        if !canTriggerAutomation {
            return .blocked
        }
        if workflowErrorMessage != nil {
            return .warning
        }
        if workflowSettings.isPrepared {
            return .ready
        }
        if workflowSettings.isEnabled {
            return .warning
        }
        return .neutral
    }

    private var statusDetailMessage: String? {
        let trimmedIssue = integrationIssue.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedIssue.isEmpty {
            return trimmedIssue
        }

        let trimmedNotice = providerNotice.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedNotice.isEmpty {
            return AppLocalized.text(
                "agent.service.status.adjusted",
                fallback: "The agent adjusted the workspace path for this request."
            )
        }

        if runtimeSettings.agentProvider == .manus && !runtimeSettings.manus.isEnabled {
            return "Owner-Setup fehlt: Externe Agent-Laeufe muessen aktiviert werden."
        }

        if shouldTriggerAutomation && !n8nReady {
            return "Agent-Aktion aktiv, aber die Automationsverbindung ist fuer dieses Konto noch nicht vollstaendig eingerichtet."
        }

        if !isOnline {
            return AppLocalized.text(
                "agent.offline.message",
                fallback: "You are offline. The agent will continue once your connection is back."
            )
        }

        return nil
    }

    private var detailTone: AgentServicePill.Tone {
        guard let message = statusDetailMessage else { return .neutral }
        if message.lowercased().contains("fallback") {
            return .warning
        }
        if message.lowercased().contains("offline") || message.lowercased().contains("fehlt") || message.lowercased().contains("nicht") {
            return .blocked
        }
        return .warning
    }
}

private struct AgentServicePill: View {
    enum Tone {
        case ready
        case warning
        case blocked
        case neutral
    }

    let title: String
    let tone: Tone
    let colorScheme: ColorScheme

    var body: some View {
        Text(title)
            .font(.caption.weight(.bold))
            .foregroundColor(foregroundColor)
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .background(
                Capsule()
                    .fill(backgroundColor)
            )
    }

    private var foregroundColor: Color {
        switch tone {
        case .ready:
            return Color.white
        case .warning, .blocked, .neutral:
            return AppColors.text(for: colorScheme)
        }
    }

    private var backgroundColor: Color {
        switch tone {
        case .ready:
            return AppColors.accentMystic(for: colorScheme)
        case .warning:
            return Color(red: 1, green: 191 / 255, blue: 102 / 255).opacity(0.28)
        case .blocked:
            return Color(red: 214 / 255, green: 43 / 255, blue: 84 / 255).opacity(0.2)
        case .neutral:
            return AppColors.secondaryBackground(for: colorScheme)
        }
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

private struct AgentWorkspaceContextCard: View {
    let colorScheme: ColorScheme
    let selectedMode: AgentExecutionMode
    let messageCount: Int
    let phase: AgentInteractionPhase
    let providerLine: String

    private var phasePillText: String {
        phase.composerStatusLabel
            ?? AppLocalized.text("agent.workspace.status.ready", fallback: "Ready")
    }

    private var modeChipText: String {
        String(
            format: AppLocalized.text("agent.workspace.mode.format", fallback: "Mode · %@"),
            selectedMode.title
        )
    }

    private var sessionChipText: String {
        String(
            format: AppLocalized.text("agent.workspace.session.format", fallback: "%d messages in this session"),
            messageCount
        )
    }

    private var phasePillTone: AgentServicePill.Tone {
        if phase.shouldBlockComposerChrome { return .ready }
        switch phase {
        case .failed, .externalFailed, .blocked:
            return .blocked
        case .partial, .retryable, .cancelled:
            return .warning
        case .idle:
            return .neutral
        default:
            return .ready
        }
    }

    private var isSettledSuccess: Bool {
        switch phase {
        case .completed, .externalCompleted: return true
        default: return false
        }
    }

    private var isError: Bool {
        switch phase {
        case .failed, .externalFailed, .blocked: return true
        default: return false
        }
    }

    private var borderColor: Color {
        if isError { return Color(red: 214 / 255, green: 43 / 255, blue: 84 / 255).opacity(0.55) }
        if phase.shouldBlockComposerChrome { return AppColors.accentMystic(for: colorScheme).opacity(0.52) }
        if isSettledSuccess { return AppColors.accentMystic(for: colorScheme).opacity(0.32) }
        return AppColors.accentMystic(for: colorScheme).opacity(0.12)
    }

    private var borderWidth: CGFloat {
        (isError || phase.shouldBlockComposerChrome) ? 1.5 : 1.0
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 2, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: statusBarColors,
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .frame(width: 3)
                .frame(maxHeight: .infinity, alignment: .top)

            VStack(alignment: .leading, spacing: 10) {
                Text(AppLocalized.text("agent.workspace.context.title", fallback: "Workspace").uppercased())
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                Text(providerLine)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme).opacity(0.92))
                HStack(alignment: .top, spacing: 8) {
                    AgentServicePill(title: modeChipText, tone: .ready, colorScheme: colorScheme)
                    AgentServicePill(title: sessionChipText, tone: .ready, colorScheme: colorScheme)
                }
                AgentServicePill(title: phasePillText, tone: phasePillTone, colorScheme: colorScheme)
            }
            .padding(.vertical, 12)
            .padding(.leading, 13)
            .padding(.trailing, 12)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(borderColor, lineWidth: borderWidth)
        )
    }

    private var statusBarColors: [Color] {
        if isError {
            return [Color(red: 214 / 255, green: 43 / 255, blue: 84 / 255), Color(red: 214 / 255, green: 43 / 255, blue: 84 / 255).opacity(0.25)]
        }
        if phase.shouldBlockComposerChrome {
            return [AppColors.accentMystic(for: colorScheme), AppColors.accentMystic(for: colorScheme).opacity(0.35)]
        }
        if isSettledSuccess {
            return [AppColors.accentMystic(for: colorScheme).opacity(0.85), AppColors.accentMystic(for: colorScheme).opacity(0.2)]
        }
        if phase == .partial {
            return [AppColors.accentMystic(for: colorScheme).opacity(0.6), AppColors.accentMystic(for: colorScheme).opacity(0.15)]
        }
        return [AppColors.accentMystic(for: colorScheme).opacity(0.3), AppColors.accentMystic(for: colorScheme).opacity(0.1)]
    }
}

private struct WorkspaceSectionHeader: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            RoundedRectangle(cornerRadius: 1, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            .clear,
                            AppColors.accentMystic(for: colorScheme).opacity(0.2),
                            .clear
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .frame(maxWidth: .infinity)
                .frame(height: 1)

            Text(AppLocalized.text("agent.section.conversation", fallback: "This thread").uppercased())
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(AppLocalized.text("agent.section.conversation.sub", fallback: "The agent refines this turn with you — same workspace, not a side chat."))
                .font(.caption2.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.88))
        }
        .padding(.top, 8)
        .padding(.bottom, 2)
    }
}

private struct AgentHeroCard: View {
    let colorScheme: ColorScheme
    let badges: [String]

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("SkyOS Agent")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Fuer Briefings, Release-Plaene, Shotlists und naechste Schritte. Du bleibst im selben Flow, nur strukturierter.")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                AppColors.accentMystic(for: colorScheme).opacity(0.5),
                                AppColors.accentMystic(for: colorScheme).opacity(0.14)
                            ],
                            center: .center,
                            startRadius: 2,
                            endRadius: 32
                        )
                    )
                    .frame(width: 54, height: 54)
                    .overlay(
                        Circle()
                            .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.12), lineWidth: 1)
                    )

                Image(systemName: "bolt.fill")
                    .font(.title3.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 10) {
                ForEach(badges, id: \.self) { badge in
                    AgentBadge(text: badge, colorScheme: colorScheme)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
    }

    private var cardBackground: some View {
        LinearGradient(
            colors: [
                AppColors.cardBackground(for: colorScheme),
                AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
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
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .transition(.opacity.combined(with: .move(edge: .top)))
        .animation(SkydownMotion.statusTransition, value: colorScheme)
    }
}

private struct AgentQuickPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text("Agent")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentMystic(for: colorScheme).opacity(0.12))
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text("Schnell planen")
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("Starte mit einer konkreten Aufgabe und lass dir direkt Struktur bauen.")
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
                                    RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                                        .fill(AppColors.primaryBackground(for: colorScheme).opacity(0.88))
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

                    Text(message.text)
                        .font(.body)
                        .foregroundColor(isUser ? .white : AppColors.text(for: colorScheme))

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
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
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
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct AgentComposerBar: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var selectedMode: AgentExecutionMode
    @Binding var selectedLevel: AIExperienceLevel
    @Binding var shouldTriggerAutomation: Bool
    let canTriggerAutomation: Bool
    let isFocused: FocusState<Bool>.Binding
    let interactionPhase: AgentInteractionPhase
    let onReset: () -> Void
    let onSend: () -> Void

    private var trimmedDraft: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var restingBottomSafeAreaInset: CGFloat {
        isFocused.wrappedValue ? 0 : keyWindowSafeAreaBottomInset
    }

    private var keyWindowSafeAreaBottomInset: CGFloat {
        let windowScenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let foregroundScene = windowScenes.first {
            $0.activationState == .foregroundActive || $0.activationState == .foregroundInactive
        }
        let keyWindow = foregroundScene?.windows.first(where: \.isKeyWindow) ?? foregroundScene?.windows.first
        return keyWindow?.safeAreaInsets.bottom ?? 0
    }

    var body: some View {
        VStack(spacing: 0) {
            if let status = interactionPhase.composerStatusLabel {
                HStack {
                    Text(status)
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
            }
            if !AgentExecutionMode.allCases.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(AgentExecutionMode.allCases) { mode in
                            Button {
                                selectedMode = mode
                            } label: {
                                Text(mode.title)
                                    .font(.caption.weight(.bold))
                                    .foregroundColor(
                                        selectedMode == mode
                                            ? .white
                                            : AppColors.text(for: colorScheme)
                                    )
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(
                                        Capsule()
                                            .fill(
                                                selectedMode == mode
                                                    ? AppColors.accentMystic(for: colorScheme)
                                                    : AppColors.secondaryBackground(for: colorScheme)
                                            )
                                    )
                            }
                            .buttonStyle(.plain)
                            .skydownTactileAction()
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                }
            }

            VStack(alignment: .leading, spacing: 5) {
                Picker(AppLocalized.text("ai.level.picker.title", fallback: "AI Level"), selection: $selectedLevel) {
                    ForEach(AIExperienceLevel.allCases) { level in
                        Text(level.title).tag(level)
                    }
                }
                .pickerStyle(.segmented)
                .disabled(interactionPhase.shouldBlockComposerChrome)
                .skydownSelectionFeedback(trigger: selectedLevel)

                Text(selectedLevel.subtitle)
                    .font(.caption2.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(1)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)

            if canTriggerAutomation {
                HStack {
                    Button {
                        shouldTriggerAutomation.toggle()
                    } label: {
                        Label(
                            shouldTriggerAutomation ? "Aktion aktiv" : "Aktion ausfuehren",
                            systemImage: shouldTriggerAutomation ? "bolt.fill" : "bolt.badge.clock"
                        )
                        .font(.caption.weight(.bold))
                        .foregroundColor(
                            shouldTriggerAutomation
                                ? .white
                                : AppColors.accentMystic(for: colorScheme)
                        )
                        .padding(.horizontal, 14)
                        .padding(.vertical, 9)
                        .background(
                            Capsule()
                                .fill(
                                    shouldTriggerAutomation
                                        ? AppColors.accentMystic(for: colorScheme)
                                        : AppColors.accentMystic(for: colorScheme).opacity(0.12)
                                )
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()

                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }

            HStack(alignment: .bottom, spacing: 10) {
                TextField(
                    selectedMode.placeholder,
                    text: $draft,
                    axis: .vertical
                )
                .lineLimit(1...4)
                .focused(isFocused)
                .submitLabel(.done)
                .onSubmit {
                    isFocused.wrappedValue = false
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                        .fill(AppColors.secondaryBackground(for: colorScheme))
                )
                .foregroundColor(AppColors.text(for: colorScheme))

                HStack(spacing: 8) {
                    if isFocused.wrappedValue {
                        Button {
                            isFocused.wrappedValue = false
                        } label: {
                            Image(systemName: "keyboard.chevron.compact.down")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .frame(width: 38, height: 38)
                                .background(
                                    Circle()
                                        .fill(AppColors.secondaryBackground(for: colorScheme))
                                )
                        }
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                        .transition(.offset(y: 6).combined(with: .opacity))
                    }

                    Button(action: onReset) {
                        Image(systemName: "arrow.counterclockwise")
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .frame(width: 38, height: 38)
                            .background(
                                Circle()
                                    .fill(AppColors.secondaryBackground(for: colorScheme))
                            )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .disabled(interactionPhase.shouldBlockComposerChrome)

                    Button(action: {
                        isFocused.wrappedValue = false
                        onSend()
                    }, label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.title3.weight(.bold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(
                                Circle()
                                    .fill(
                                        LinearGradient(
                                            colors: [
                                                AppColors.accent(for: colorScheme),
                                                AppColors.accentMystic(for: colorScheme)
                                            ],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                            )
                    })
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .disabled(trimmedDraft.isEmpty || interactionPhase.shouldBlockSend)
                    .opacity(trimmedDraft.isEmpty || interactionPhase.shouldBlockSend ? 0.6 : 1)
                }
                .animation(SkydownMotion.pressInteraction, value: isFocused.wrappedValue)
            }
            .padding(.horizontal, 16)
            .padding(.top, canTriggerAutomation || !AgentExecutionMode.allCases.isEmpty ? 8 : 10)
            .padding(.bottom, 10 + restingBottomSafeAreaInset)
            .background(
                Rectangle()
                    .fill(AppColors.primaryBackground(for: colorScheme).opacity(0.96))
                    .ignoresSafeArea(.container, edges: .bottom)
            )
            .overlay(alignment: .top) {
                Divider().opacity(0.25)
            }
        }
    }
}

private struct AgentComposerBarHeightPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}

private struct AgentBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.accentMystic(for: colorScheme)
        )
    }
}
