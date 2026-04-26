import SwiftUI
import UIKit
import UniformTypeIdentifiers

struct AgentView: View {
    @StateObject private var viewModel: AgentChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @ObservedObject private var membershipCoordinator: AIMembershipCoordinator
    @ObservedObject private var workflowAutomationSettingsStore = WorkflowAutomationSettingsStore.shared
    @ObservedObject private var aiRuntimeSettingsStore = AIRuntimeSettingsStore.shared
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @State private var showingAttachmentImporter = false
    @State private var showingPromptComposer = false
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
                selectedLevel: $viewModel.selectedLevel,
                shouldTriggerAutomation: $viewModel.shouldTriggerAutomation,
                canTriggerAutomation: viewModel.canTriggerAutomation,
                interactionPhase: viewModel.phase,
                attachments: inputAttachments,
                onAddFiles: {
                    showingPromptComposer = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                        showingAttachmentImporter = true
                    }
                },
                onRemoveAttachment: removeAttachment,
                onClearAttachments: { inputAttachments.removeAll() },
                onReset: {
                    viewModel.resetConversation()
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

    private var usesCompactImmersiveLayout: Bool {
        !showsNavigation
    }

    private var composerReservedBottomSpace: CGFloat {
        featureFlags.isAIEnabled ? 86 : 0
    }

    private var content: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                if featureFlags.isAIEnabled {
                    if viewModel.messages.isEmpty {
                        ScrollView {
                            VStack(alignment: .leading, spacing: usesCompactImmersiveLayout ? 12 : 14) {
                                AgentEmptyStateHeader(
                                    colorScheme: colorScheme,
                                    isCompact: usesCompactImmersiveLayout
                                )
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
                                    AgentEmptyStateHeader(
                                        colorScheme: colorScheme,
                                        isCompact: true
                                    )

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
                                .padding(.top, showsNavigation ? 6 : 2)
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
            ZStack {
                Circle()
                    .fill(.ultraThinMaterial)
                    .frame(width: 58, height: 58)
                    .shadow(color: .black.opacity(0.16), radius: 16, x: 0, y: 8)

                if isWorking {
                    ProgressView()
                        .scaleEffect(0.82)
                } else {
                    Image(systemName: "plus")
                        .font(.title3.weight(.black))
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Prompt oeffnen")
    }
}

private struct AgentPromptComposerSheet: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var selectedMode: AgentExecutionMode
    @Binding var selectedLevel: AIExperienceLevel
    @Binding var shouldTriggerAutomation: Bool
    let canTriggerAutomation: Bool
    let interactionPhase: AgentInteractionPhase
    let attachments: [AgentInputAttachment]
    let onAddFiles: () -> Void
    let onRemoveAttachment: (AgentInputAttachment) -> Void
    let onClearAttachments: () -> Void
    let onReset: () -> Void
    let onSend: () -> Void
    @FocusState private var isFocused: Bool

    private var trimmedDraft: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        ScrollView {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 3) {
                Text("Neue Anfrage")
                    .font(.title3.weight(.black))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text("Erst Optionen waehlen, dann Prompt schreiben.")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
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

                Menu {
                    ForEach(AIExperienceLevel.allCases) { level in
                        Button(level.title) { selectedLevel = level }
                    }
                } label: {
                    Label(selectedLevel.title, systemImage: "sparkles")
                        .font(.caption.weight(.bold))
                }
                .disabled(interactionPhase.shouldBlockComposerChrome)

                if canTriggerAutomation {
                    Button { shouldTriggerAutomation.toggle() } label: {
                        Image(systemName: shouldTriggerAutomation ? "bolt.fill" : "bolt")
                            .font(.caption.weight(.bold))
                    }
                    .disabled(interactionPhase.shouldBlockComposerChrome)
                    .accessibilityLabel(shouldTriggerAutomation ? "Aktion aktiv" : "Aktion ausfuehren")
                }
            }

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
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous))
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
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }

                    Button("Alle entfernen", action: onClearAttachments)
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
            }

            HStack(spacing: 10) {
                Spacer(minLength: 0)
                Button("Reset", action: onReset)
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
