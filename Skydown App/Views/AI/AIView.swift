import SwiftUI
import UIKit

struct AIView: View {
    @StateObject private var viewModel: AIChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @EnvironmentObject private var authManager: AuthManager
    @EnvironmentObject private var aiSubscriptionStore: NativeAISubscriptionStore
    @Environment(\.colorScheme) private var colorScheme
    @FocusState private var isComposerFocused: Bool
    private let showsNavigation: Bool
    @StateObject private var membershipCoordinator = AIMembershipCoordinator.shared
    @State private var autoPresentedUpgradeHint = false

    init(
        aiChatService: AIChatServicing = FirebaseFunctionsAIChatService(),
        featureFlags: FeatureFlagsService,
        showsNavigation: Bool = true
    ) {
        self.showsNavigation = showsNavigation
        _viewModel = StateObject(
            wrappedValue: AIChatViewModel(service: aiChatService)
        )
        _featureFlags = ObservedObject(wrappedValue: featureFlags)
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
        .task {
            viewModel.configureUser(user: authManager.userSession)
            membershipCoordinator.cacheCurrentPlan(currentUserQuotaPlan)
        }
        .onChange(of: authManager.userSession?.id) { _, _ in
            viewModel.configureUser(user: authManager.userSession)
            membershipCoordinator.cacheCurrentPlan(currentUserQuotaPlan)
        }
        .onDisappear {
            isComposerFocused = false
        }
        .sheet(
            isPresented: Binding(
                get: { membershipCoordinator.isPresented },
                set: { if !$0 { membershipCoordinator.closeMembership() } }
            )
        ) { membershipSheet }
        .task {
            await aiSubscriptionStore.prepareStorefront(for: authManager.userSession)
        }
        .onChange(of: authManager.userSession?.id) { _, _ in
            Task {
                await aiSubscriptionStore.prepareStorefront(for: authManager.userSession)
            }
        }
        .onChange(of: viewModel.revenueUsage?.warningLevel) { _, level in
            handleCriticalUsageWarning(level)
        }
    }

    private var membershipSheet: some View {
        AIMembershipSheet(
            colorScheme: colorScheme,
            isLoadingProducts: aiSubscriptionStore.isLoadingProducts,
            isSyncing: aiSubscriptionStore.isSyncing,
            activePurchasePlan: aiSubscriptionStore.activePurchasePlan,
            onSelectPlan: { plan in
                Task {
                    membershipCoordinator.track("plan_selected", ["plan": plan.rawValue])
                    membershipCoordinator.track("purchase_started", ["plan": plan.rawValue])
                    membershipCoordinator.track("purchase_started_pipeline", ["surface": membershipCoordinator.surface])
                    MembershipAnalyticsTracker().track(
                        "plan_selected",
                        reason: membershipCoordinator.lastOpenReason.rawValue,
                        plan: plan.rawValue,
                        surface: membershipCoordinator.surface,
                        currentPlan: membershipCoordinator.currentPlanCache.rawValue
                    )
                    MembershipAnalyticsTracker().track(
                        "purchase_started",
                        reason: membershipCoordinator.lastOpenReason.rawValue,
                        plan: plan.rawValue,
                        surface: membershipCoordinator.surface,
                        currentPlan: membershipCoordinator.currentPlanCache.rawValue
                    )
                    do {
                        let outcome = try await aiSubscriptionStore.purchase(plan: plan)
                        switch outcome {
                        case .success:
                            membershipCoordinator.track("purchase_success", ["plan": plan.rawValue])
                            MembershipAnalyticsTracker().track(
                                "purchase_success",
                                reason: membershipCoordinator.lastOpenReason.rawValue,
                                plan: plan.rawValue,
                                surface: membershipCoordinator.surface,
                                currentPlan: membershipCoordinator.currentPlanCache.rawValue
                            )
                            await membershipCoordinator.postPurchaseRefresh(plan: plan) {
                                await MainActor.run {
                                    viewModel.configureUser(user: authManager.userSession)
                                }
                            }
                            viewModel.showToastMessage("Upgrade erfolgreich aktiviert.", style: .success)
                        case .pending:
                            viewModel.showToastMessage("Kauf wartet auf App-Store-Freigabe.", style: .info)
                        case .cancelled:
                            membershipCoordinator.track("purchase_cancelled", ["plan": plan.rawValue])
                            MembershipAnalyticsTracker().track(
                                "purchase_cancelled",
                                reason: membershipCoordinator.lastOpenReason.rawValue,
                                plan: plan.rawValue,
                                surface: membershipCoordinator.surface,
                                currentPlan: membershipCoordinator.currentPlanCache.rawValue
                            )
                            viewModel.showToastMessage("Kauf abgebrochen.", style: .info)
                        }
                    } catch {
                        viewModel.showToastMessage("Upgrade konnte nicht gestartet werden: \(error.localizedDescription)", style: .error)
                    }
                }
            },
            onRestore: {
                Task {
                    do {
                        let shouldForceEmptySync = authManager.userSession?.normalizedAISubscriptionProvider == "app_store"
                        try await aiSubscriptionStore.restorePurchases(forceEmptySync: shouldForceEmptySync)
                        await membershipCoordinator.restore {
                            await MainActor.run {
                                viewModel.configureUser(user: authManager.userSession)
                            }
                        }
                        viewModel.showToastMessage("App-Store-Kaeufe synchronisiert.", style: .success)
                    } catch {
                        viewModel.showToastMessage("Synchronisierung fehlgeschlagen: \(error.localizedDescription)", style: .error)
                    }
                }
            },
            onManage: {
                Task {
                    do {
                        try await aiSubscriptionStore.manageSubscriptions()
                    } catch {
                        viewModel.showToastMessage("Abo-Verwaltung konnte nicht geoeffnet werden.", style: .error)
                    }
                }
            }
        )
        .presentationDetents([.medium, .large])
    }

    private func handleCriticalUsageWarning(_ level: String?) {
        guard !autoPresentedUpgradeHint else { return }
        guard level == "critical" else { return }
        autoPresentedUpgradeHint = true
        membershipCoordinator.openMembership(reason: .criticalUsage, surface: "ai_chat")
    }

    private var currentUserQuotaPlan: UserQuotaPlan? {
        UserQuotaPlan(rawValue: authManager.userSession?.quotaPlan ?? "")
    }

    private var content: some View {
        VStack(spacing: 0) {
            if featureFlags.isAIEnabled {
                if viewModel.messages.isEmpty {
                    VStack(alignment: .leading, spacing: 14) {
                        Spacer(minLength: showsNavigation ? 8 : 4)

                        AIEmptyStateHeader(colorScheme: colorScheme)
                        if let usage = viewModel.revenueUsage {
                            AIRevenueUsageCard(usage: usage, colorScheme: colorScheme)
                                .onTapGesture {
                                    if !usage.userFacingReason.isEmpty {
                                        MembershipAnalyticsTracker().track(
                                            "upgrade_after_deny",
                                            reason: membershipCoordinator.lastOpenReason.rawValue,
                                            surface: "ai_empty",
                                            currentPlan: membershipCoordinator.currentPlanCache.rawValue
                                        )
                                    }
                                    membershipCoordinator.openMembership(reason: .manual, surface: "ai_empty")
                                }
                        } else {
                            AIPlanPreviewCard(colorScheme: colorScheme)
                                .onTapGesture {
                                    membershipCoordinator.openMembership(reason: .manual, surface: "ai_empty")
                                }
                        }
                        aiSessionDeck

                        AIQuickPromptCard(
                            colorScheme: colorScheme,
                            prompts: viewModel.quickPrompts,
                            onPromptSelected: { prompt in
                                isComposerFocused = false
                                viewModel.sendPrompt(prompt)
                            }
                        )

                        AIVisualPromptCard(
                            colorScheme: colorScheme,
                            prompts: viewModel.visualPrompts,
                            onPromptSelected: { prompt in
                                isComposerFocused = false
                                viewModel.generateVisual(prompt)
                            }
                        )

                        Spacer(minLength: 12)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.bottom, 6)
                } else {
                    ScrollViewReader { proxy in
                        let scrollToken = viewModel.messages.last.map { message in
                            "\(message.id.uuidString)-\(message.isStreaming)-\(message.text.count)-\(message.imageData?.count ?? 0)"
                        } ?? "chat-empty"

                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: 10) {
                                if let usage = viewModel.revenueUsage {
                                    AIRevenueUsageCard(usage: usage, colorScheme: colorScheme)
                                        .onTapGesture {
                                            if !usage.userFacingReason.isEmpty {
                                                MembershipAnalyticsTracker().track(
                                                    "upgrade_after_deny",
                                                    reason: membershipCoordinator.lastOpenReason.rawValue,
                                                    surface: "ai_chat",
                                                    currentPlan: membershipCoordinator.currentPlanCache.rawValue
                                                )
                                            }
                                            membershipCoordinator.openMembership(reason: .manual, surface: "ai_chat")
                                        }
                                }
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
                        .simultaneousGesture(
                            TapGesture().onEnded {
                                isComposerFocused = false
                            }
                        )
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
            if featureFlags.isAIEnabled {
                AIComposerBar(
                    colorScheme: colorScheme,
                    draft: $viewModel.draft,
                    composerMode: $viewModel.composerMode,
                    textMode: $viewModel.textMode,
                    isFocused: $isComposerFocused,
                    interactionPhase: viewModel.phase,
                    onReset: viewModel.resetConversation,
                    onSend: viewModel.sendDraft
                )
            }
        }
        .task {
            await featureFlags.refresh()
        }
        .background(backgroundGradient.ignoresSafeArea())
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
            Text(AppLocalized.text("ai.membership.plans.caption", fallback: "No tokens. Unlock capable creator workflows."))
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

private struct AIMembershipSheet: View {
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

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(AppLocalized.text("ai.empty.title", fallback: "What do you need?"))
                .font(.system(size: 28, weight: .black, design: .rounded))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("ai.empty.subtitle", fallback: "Write briefly and start calmly."))
                .font(.subheadline.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(AppLocalized.text("ai.empty.memory", fallback: "Your history stays per account, so the bot does not restart from zero each time."))
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.accent(for: colorScheme).opacity(0.9))
        }
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
                Text("Text")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accent(for: colorScheme).opacity(0.12))
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

private struct AIVisualPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [AIVisualPrompt]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text("Visual")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
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

private struct AIComposerBar: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var composerMode: AIComposerMode
    @Binding var textMode: AITextMode
    let isFocused: FocusState<Bool>.Binding
    let interactionPhase: BotInteractionPhase
    let onReset: () -> Void
    let onSend: () -> Void
    @StateObject private var keyboardObserver = SkydownKeyboardObserver()

    private var trimmedDraft: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var activeKeyboardInset: CGFloat {
        isFocused.wrappedValue ? keyboardObserver.bottomInset : 0
    }

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 10) {
                if let status = interactionPhase.composerStatusLabel {
                    HStack {
                        Text(status)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accentMystic(for: colorScheme))
                        Spacer(minLength: 0)
                    }
                }
                HStack(spacing: 10) {
                    Picker("Modus", selection: $composerMode) {
                        ForEach(AIComposerMode.allCases) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)
                    .skydownSelectionFeedback(trigger: composerMode)

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
                    .disabled(interactionPhase.isBusy)
                }

                if composerMode == .text {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(AITextMode.allCases) { mode in
                                Button {
                                    textMode = mode
                                } label: {
                                    Text(mode.title)
                                        .font(.caption.weight(.bold))
                                        .foregroundColor(
                                            textMode == mode
                                                ? .white
                                                : AppColors.text(for: colorScheme)
                                        )
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .background(
                                            Capsule()
                                                .fill(
                                                    textMode == mode
                                                        ? AppColors.accent(for: colorScheme)
                                                        : AppColors.secondaryBackground(for: colorScheme)
                                                )
                                        )
                                }
                                .buttonStyle(.plain)
                                .skydownTactileAction()
                            }
                        }
                    }
                    .skydownSelectionFeedback(trigger: textMode)
                }

                HStack(alignment: .bottom, spacing: 10) {
                    TextField(
                        composerMode == .text
                            ? textMode.placeholder
                            : "Zum Beispiel: Dunkles Cover-Art fuer einen neuen Release.",
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
                            .transition(.scale.combined(with: .opacity))
                        }

                        Button(action: {
                            isFocused.wrappedValue = false
                            onSend()
                        }, label: {
                            Image(systemName: composerMode == .text ? "arrow.up.circle.fill" : "sparkles")
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
                        .disabled(trimmedDraft.isEmpty || interactionPhase.isBusy)
                        .opacity(trimmedDraft.isEmpty || interactionPhase.isBusy ? 0.6 : 1)
                    }
                    .animation(.easeOut(duration: 0.16), value: isFocused.wrappedValue)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
            .padding(.bottom, 10)
            .background(
                Rectangle()
                    .fill(AppColors.primaryBackground(for: colorScheme).opacity(0.96))
                    .ignoresSafeArea()
            )
            .overlay(alignment: .top) {
                Divider().opacity(0.25)
            }
        }
        .padding(.bottom, activeKeyboardInset)
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
        featureFlags: services.featureFlags
    )
}
