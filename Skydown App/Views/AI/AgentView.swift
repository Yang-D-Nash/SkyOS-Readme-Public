import SwiftUI
import UIKit

struct AgentView: View {
    @StateObject private var viewModel: AgentChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @ObservedObject private var manusByosStore = ManusBYOSStore.shared
    @ObservedObject private var workflowAutomationSettingsStore = WorkflowAutomationSettingsStore.shared
    @ObservedObject private var aiRuntimeSettingsStore = AIRuntimeSettingsStore.shared
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @FocusState private var isComposerFocused: Bool
    private let showsNavigation: Bool

    init(
        agentChatService: AgentChatServicing = FirebaseFunctionsAgentService(),
        featureFlags: FeatureFlagsService,
        showsNavigation: Bool = true
    ) {
        self.showsNavigation = showsNavigation
        _viewModel = StateObject(
            wrappedValue: AgentChatViewModel(service: agentChatService)
        )
        _featureFlags = ObservedObject(wrappedValue: featureFlags)
    }

    var body: some View {
        Group {
            if showsNavigation {
                NavigationStack {
                    content
                        .navigationTitle("Agent")
                        .skydownNavigationChrome(colorScheme: colorScheme)
                }
            } else {
                content
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Fertig") {
                    isComposerFocused = false
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .task {
            viewModel.configureUser(user: authManager.userSession)
            configureIntegrationObservations(for: authManager.userSession)
        }
        .onChange(of: authManager.userSession?.id) { _, _ in
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
    }

    private var content: some View {
        VStack(spacing: 0) {
            if featureFlags.isAIEnabled {
                if viewModel.messages.isEmpty {
                    VStack(alignment: .leading, spacing: 14) {
                        Spacer(minLength: showsNavigation ? 8 : 4)

                        AgentEmptyStateHeader(colorScheme: colorScheme)
                        serviceStatusCard

                        AgentQuickPromptCard(
                            colorScheme: colorScheme,
                            prompts: viewModel.quickPrompts,
                            onPromptSelected: { prompt in
                                isComposerFocused = false
                                viewModel.sendPrompt(prompt)
                            }
                        )

                        Spacer(minLength: 12)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.bottom, 6)
                } else {
                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: 10) {
                                serviceStatusCard

                                ForEach(viewModel.messages) { message in
                                    AgentMessageBubble(
                                        message: message,
                                        colorScheme: colorScheme
                                    )
                                    .id(message.id)
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
                        .onChange(of: viewModel.messages.count) { _, _ in
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
            if featureFlags.isAIEnabled {
                AgentComposerBar(
                    colorScheme: colorScheme,
                    draft: $viewModel.draft,
                    selectedMode: $viewModel.selectedMode,
                    shouldTriggerAutomation: $viewModel.shouldTriggerAutomation,
                    canTriggerAutomation: viewModel.canTriggerAutomation,
                    isFocused: $isComposerFocused,
                    isSending: viewModel.isSending,
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
            secondaryAccent: AppColors.accentHighlight(for: colorScheme)
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
            integrationIssue: viewModel.lastIntegrationIssue
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

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "bolt.horizontal.circle")
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                Text("Agent-Verbindung")
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
                AgentServicePill(
                    title: isOnline ? "Online" : "Offline",
                    tone: isOnline ? .ready : .blocked,
                    colorScheme: colorScheme
                )
            }

            HStack(spacing: 8) {
                AgentServicePill(
                    title: "Provider: \(providerLabel)",
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
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
    }

    private var providerLabel: String {
        if let runtimeErrorMessage, !runtimeErrorMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Status unklar"
        }

        let runtimeProvider = runtimeSettings.agentProvider
        if runtimeProvider != lastAgentProvider && !providerNotice.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "\(runtimeProvider.displayTitle) (Fallback \(lastAgentProvider.displayTitle))"
        }
        return runtimeProvider.displayTitle
    }

    private var providerTone: AgentServicePill.Tone {
        if runtimeErrorMessage != nil {
            return .warning
        }
        return .ready
    }

    private var manusLabel: String {
        if runtimeSettings.agentProvider != .manus {
            return "Manus: aus (Gemini aktiv)"
        }
        if !runtimeSettings.manus.isEnabled {
            return "Manus: runtime aus"
        }
        if manusSettings.isEnabled && manusSettings.hasAPIKey {
            return "Manus: BYOS aktiv"
        }
        if manusSettings.hasAPIKey && !manusSettings.isEnabled {
            return "Manus: Key da, BYOS aus"
        }
        return "Manus: Backend/BYOS"
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
            return "n8n: Login fehlt"
        }
        if let workflowErrorMessage,
           !workflowErrorMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "n8n: Status unklar"
        }
        if workflowSettings.isPrepared {
            return "n8n: bereit"
        }
        if workflowSettings.isEnabled {
            return "n8n: unvollstaendig"
        }
        return "n8n: aus"
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
            return trimmedNotice
        }

        if runtimeSettings.agentProvider == .manus && !runtimeSettings.manus.isEnabled {
            return "Owner-Setup fehlt: In der Runtime muss Manus aktiviert werden."
        }

        if shouldTriggerAutomation && !n8nReady {
            return "Agent-Aktion aktiv, aber n8n ist fuer dieses Konto noch nicht vollstaendig eingerichtet."
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

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Wobei soll ich dich strukturieren?")
                .font(.system(size: 28, weight: .black, design: .rounded))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Nutze den Agent fuer Briefings, Shotlists, Release-Plaene und klare naechste Schritte. Schreib direkt unten los oder starte mit einem Prompt.")
                .font(.subheadline.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text("Auch der Agent fuehrt den Verlauf pro Konto weiter, damit Briefings und To-dos anschlussfaehig bleiben.")
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.accentMystic(for: colorScheme).opacity(0.9))
        }
    }
}

private struct AgentHeroCard: View {
    let colorScheme: ColorScheme
    let badges: [String]

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("SkyOs Agent")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Fuer Briefings, Release-Plaene, Shotlists und naechste Schritte. Du bleibst im selben Flow, nur strukturierter.")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                Circle()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 54, height: 54)

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
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.14))
                        .frame(width: 50, height: 50)

                    Image(systemName: "lock.fill")
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text("SkyOs Agent pausiert")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Voruebergehend nicht verfuegbar")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
    }
}

private struct AgentQuickPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Schnell planen")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Starte mit einer konkreten Aufgabe und lass dir direkt Struktur bauen.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

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
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
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
                Text(isUser ? "Du" : "SkyOs Agent")
                    .font(.caption.weight(.bold))
                    .foregroundColor(isUser ? .white.opacity(0.9) : AppColors.accentMystic(for: colorScheme))

                if message.isStreaming && message.text.isEmpty {
                    HStack(spacing: 10) {
                        ProgressView()
                            .tint(AppColors.accentMystic(for: colorScheme))

                        Text("SkyOs Agent plant gerade...")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else {
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

private struct AgentComposerBar: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var selectedMode: AgentExecutionMode
    @Binding var shouldTriggerAutomation: Bool
    let canTriggerAutomation: Bool
    let isFocused: FocusState<Bool>.Binding
    let isSending: Bool
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
                        .transition(.scale.combined(with: .opacity))
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
                    .disabled(isSending)

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
                    .disabled(trimmedDraft.isEmpty || isSending)
                    .opacity(trimmedDraft.isEmpty || isSending ? 0.6 : 1)
                }
                .animation(.easeOut(duration: 0.16), value: isFocused.wrappedValue)
            }
            .padding(.horizontal, 16)
            .padding(.top, canTriggerAutomation || !AgentExecutionMode.allCases.isEmpty ? 8 : 10)
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
