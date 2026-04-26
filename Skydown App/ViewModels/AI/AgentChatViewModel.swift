import Foundation
import Combine
import FirebaseFunctions

enum AgentChatRole {
    case user
    case assistant
}

enum AgentResultType: String, Equatable {
    case text
    case workflow
    case progress
    case error
}

struct AgentWorkflowSummary: Equatable {
    let workflowName: String
    let statusText: String
    let runID: String?
}

struct AgentChatMessage: Identifiable, Equatable {
    let id: UUID
    let role: AgentChatRole
    var text: String
    var isStreaming: Bool
    var resultType: AgentResultType
    var workflowSummary: AgentWorkflowSummary?

    init(
        id: UUID = UUID(),
        role: AgentChatRole,
        text: String,
        isStreaming: Bool = false,
        resultType: AgentResultType = .text,
        workflowSummary: AgentWorkflowSummary? = nil
    ) {
        self.id = id
        self.role = role
        self.text = text
        self.isStreaming = isStreaming
        self.resultType = resultType
        self.workflowSummary = workflowSummary
    }
}

enum AgentExecutionMode: String, CaseIterable, Identifiable {
    case release = "release"
    case briefing = "briefing"
    case content = "content"
    case merch = "merch"
    case automation = "automation"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .release:
            return "Release"
        case .briefing:
            return "Briefing"
        case .content:
            return "Content"
        case .merch:
            return "Merch"
        case .automation:
            return "Automation"
        }
    }

    var placeholder: String {
        switch self {
        case .release:
            return "Zum Beispiel: Release-Plan fuer Freitag."
        case .briefing:
            return "Zum Beispiel: Briefing fuer ein Video-Team."
        case .content:
            return "Zum Beispiel: Content-Plan fuer Reels und Story."
        case .merch:
            return "Zum Beispiel: Struktur fuer einen Merch-Drop."
        case .automation:
            return "Zum Beispiel: Uebergabe fuer einen n8n-Workflow."
        }
    }

    var quickPrompts: [String] {
        switch self {
        case .release:
            return [
                "Baue mir einen 7-Tage-Release-Plan mit Assets, Deadlines und Ownern.",
                "Plane den Launch fuer Freitag inklusive Story, Reel und CTA.",
                "Mach mir einen Mini-Release-Fahrplan fuer Song, Cover und Snippets.",
                "Welche Deliverables brauche ich fuer einen sauberen Release?"
            ]
        case .briefing:
            return [
                "Mach ein Video-Briefing mit Ziel, Shotlist, Deliverables und Risiken.",
                "Schreib ein Briefing fuer einen Fotografen mit Mood und Must-have-Shots.",
                "Erstelle ein Copy-Briefing fuer einen externen Creator.",
                "Formuliere ein kreatives Briefing fuer Cover, Poster und Story-Assets."
            ]
        case .content:
            return [
                "Erstelle einen Content-Plan fuer TikTok, Reels und Story mit Timing.",
                "Plane 5 Tage Promo-Content fuer einen neuen Track.",
                "Mach eine Hook- und CTA-Strategie fuer Shortform-Content.",
                "Strukturiere eine Woche Content aus einem Dreh heraus."
            ]
        case .merch:
            return [
                "Strukturiere einen Merch-Drop in To-dos, Reihenfolge und Checkliste.",
                "Mach mir einen Launch-Plan fuer Hoodie und Shirt.",
                "Welche Assets und Texte braucht ein kleiner Shop-Drop?",
                "Plane eine Merch-Aktion mit Story, Shop und Follow-up."
            ]
        case .automation:
            return [
                "Erstelle eine n8n-Uebergabe fuer einen Content-Workflow mit Inputs und Outputs.",
                "Strukturiere einen Automations-Flow fuer Asset-Freigaben und Social-Copy.",
                "Mach ein Workflow-Briefing fuer einen Release-Reminder-Prozess.",
                "Welche Schritte und Fehlerfaelle muss eine Release-Automation abdecken?"
            ]
        }
    }
}

@MainActor
final class AgentChatViewModel: ObservableObject {
    struct RevenueUsageState {
        let planTitle: String
        let remaining: Int
        let limit: Int
        let warningLevel: String
        let userFacingReason: String
        let suggestedUpgrade: String
        let resetHint: String
        let retryAfterSeconds: Int
        let lowerCostOption: String
    }

    @Published var messages: [AgentChatMessage] = []
    @Published var draft = ""
    @Published var selectedMode: AgentExecutionMode = .release
    @Published var selectedLevel: AIExperienceLevel = .standard
    @Published var shouldTriggerAutomation = false
    @Published private(set) var canTriggerAutomation = false
    /// Agent-only lifecycle (distinct from `BotInteractionPhase`).
    @Published private(set) var phase: AgentInteractionPhase = .idle
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info
    @Published private(set) var lastAgentProvider: AIRuntimeAgentProvider = .grok
    @Published private(set) var lastProviderNotice: String = ""
    @Published private(set) var lastIntegrationIssue: String = ""
    /// Latest `users/{uid}/agentRuns/{id}` id returned by `skydownAgent` (empty if not recorded).
    @Published private(set) var lastAgentRunId: String = ""
    @Published private(set) var revenueUsage: RevenueUsageState?
    @Published private(set) var sessions: [AIScriptHistorySessionSummary] = []
    @Published private(set) var activeSessionID: UUID?
    @Published private(set) var activeSessionTitle: String = AIScriptHistoryStore.defaultSessionTitle

    var quickPrompts: [String] {
        selectedMode.quickPrompts
    }

    private let service: AgentChatServicing
    private let syncService: AIConversationSyncServicing
    private let historyStore: AIScriptHistoryStore
    private let pendingQueueStore: AgentPendingQueueStore
    private var currentUserID: String?
    private var currentUserKey: String?
    private var currentSessionID: UUID?
    private var pendingRequests: [PendingAgentRequest] = []
    private var networkObserver: AnyCancellable?
    private var currentPlanTitle: String = "Free"
    private var currentHistoryRetentionDays: Int = UserRole.user.defaultAIHistoryRetentionDays
    private var conversationRevision = 0
    private var activeRequestTask: Task<Void, Never>?
    private var pendingRetryTask: Task<Void, Never>?
    private var remoteHydrationTask: Task<Void, Never>?
    private var stopRemoteHistoryObservation: (() -> Void)?
    private var activeRequestContext: InFlightRequestContext?
    private var currentQuotaPlan: UserQuotaPlan = .free

    private struct PendingAgentRequest {
        let prompt: String
        let history: [AgentHistoryTurn]
        let mode: String
        let aiLevel: String
        let executeAutomation: Bool
        let assistantMessageID: UUID
        let createdAt: Date
    }

    private struct InFlightRequestContext: Equatable {
        let requestID: UUID
        let conversationRevision: Int
        let userKeyAtSend: String?
        let sessionIDAtSend: UUID?
        let aiLevelAtSend: AIExperienceLevel
        let requiresLevelMatch: Bool
    }

    init(
        service: AgentChatServicing = FirebaseFunctionsAgentService(),
        syncService: AIConversationSyncServicing = FirestoreAIConversationSyncService()
    ) {
        self.service = service
        self.syncService = syncService
        self.historyStore = AIScriptHistoryStore.shared
        self.pendingQueueStore = AgentPendingQueueStore.shared
        networkObserver = NetworkStatusMonitor.shared.$isOnline
            .removeDuplicates()
            .sink { [weak self] isOnline in
                guard isOnline else { return }
                Task { @MainActor in
                    self?.startPendingRetryIfNeeded()
                }
            }
    }

    deinit {
        activeRequestTask?.cancel()
        pendingRetryTask?.cancel()
        remoteHydrationTask?.cancel()
        stopRemoteHistoryObservation?()
    }

    func configureUser(user: User?) {
        let trimmedUserID = user?.id?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let normalizedUserID = trimmedUserID.isEmpty ? nil : trimmedUserID
        let normalizedUserKey = normalizedUserKey(for: user?.id ?? user?.email)
        if let user {
            currentQuotaPlan = user.resolvedQuotaPlan
            switch user.resolvedQuotaPlan {
            case .studio: currentPlanTitle = "Creator"
            case .creator: currentPlanTitle = "Pro"
            case .internalTeam: currentPlanTitle = "Team"
            case .ownerUnlimited: currentPlanTitle = "Owner"
            default: currentPlanTitle = "Free"
            }
        } else {
            currentQuotaPlan = .free
            currentPlanTitle = "Free"
        }
        currentHistoryRetentionDays = user?.resolvedAIHistoryRetentionDays ?? UserRole.user.defaultAIHistoryRetentionDays
        if !selectedLevel.isAvailable(for: currentQuotaPlan) {
            selectedLevel = .standard
        }
        historyStore.updateRetentionDays(currentHistoryRetentionDays)
        canTriggerAutomation = user != nil
        ManusBYOSStore.shared.setUserMode(userID: user?.id)
        if !canTriggerAutomation {
            shouldTriggerAutomation = false
        }
        let didChangeIdentity = normalizedUserKey != currentUserKey || normalizedUserID != currentUserID
        if !didChangeIdentity {
            pruneRemoteHistoryIfNeeded()
            return
        }
        invalidateConversation(cancelActiveWork: true)
        stopRemoteHistoryObservation?()
        stopRemoteHistoryObservation = nil
        currentUserID = normalizedUserID
        currentUserKey = normalizedUserKey
        restoreConversationState()
        hydrateRemoteHistoryIfNeeded(preferredSessionID: currentSessionID)
        startRemoteHistoryObservation()
        if NetworkStatusMonitor.shared.isOnline {
            startPendingRetryIfNeeded()
        }
    }

    func sendDraft() {
        sendPrompt(draft)
    }

    func sendPrompt(_ prompt: String) {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !phase.shouldBlockSend else { return }
        guard selectedLevel.isAvailable(for: currentQuotaPlan) else {
            showUserToast(AIExperienceLevel.unavailableMessage, style: .info)
            return
        }
        let session = historyStore.ensureSession(
            userKey: currentUserKey,
            source: .agent,
            preferredSessionID: currentSessionID
        )
        currentSessionID = session.id
        refreshConversationMetadata(preferredSessionID: session.id)
        guard NetworkStatusMonitor.shared.isOnline else {
            queuePromptForRetry(trimmedPrompt)
            return
        }

        phase = .planning

        let modeAtSend = selectedMode.rawValue
        let levelAtSend = selectedLevel
        let executeAutomationAtSend = shouldTriggerAutomation && canTriggerAutomation
        let assistantID = UUID()
        let userMessage = AgentChatMessage(role: .user, text: trimmedPrompt)
        let historyAtSend = buildHistory(from: messages + [userMessage])
        let requestContext = makeRequestContext(aiLevelAtSend: levelAtSend)

        messages.append(userMessage)
        messages.append(
            AgentChatMessage(
                id: assistantID,
                role: .assistant,
                text: "",
                isStreaming: true,
                resultType: .progress
            )
        )
        draft = ""

        activeRequestContext = requestContext
        activeRequestTask?.cancel()
        activeRequestTask = Task { @MainActor [weak self] in
            guard let self else { return }
            do {
                let result = try await service.sendMessage(
                    prompt: trimmedPrompt,
                    history: historyAtSend,
                    mode: modeAtSend,
                    aiLevel: levelAtSend.rawValue,
                    executeAutomation: executeAutomationAtSend,
                    manusApiKeyOverride: ManusBYOSStore.shared.currentAPIKeyOrNil()
                )
                guard isRequestContextValid(requestContext) else { return }
                updateProviderDiagnostics(from: result)
                applyUsage(result.usage)
                historyStore.updateRetentionDays(result.historyRetentionDays)
                let replyText = augmentedReplyText(from: result)
                updateAssistantMessage(
                    id: assistantID,
                    text: replyText,
                    isStreaming: false,
                    resultType: (result.automationTriggered || result.automationAttempted) ? .workflow : .text,
                    workflowSummary: buildWorkflowSummary(from: result)
                )
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .agent,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: trimmedPrompt,
                    response: replyText
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                if result.automationTriggered {
                    lastIntegrationIssue = ""
                    showUserToast(
                        result.automationMessage.isEmpty
                            ? AppLocalized.text("agent.automation.triggered", fallback: "Automation wurde gestartet.")
                            : result.automationMessage,
                        style: .success
                    )
                } else if result.automationAttempted && !result.automationMessage.isEmpty {
                    lastIntegrationIssue = result.automationMessage
                    showUserToast(result.automationMessage, style: .error)
                } else {
                    lastIntegrationIssue = ""
                }
                phase = resolvedTerminalPhase(for: result.decision, hasPendingQueue: !pendingRequests.isEmpty)
                clearActiveRequestIfNeeded(requestContext)
            } catch {
                if error is CancellationError || Task.isCancelled {
                    clearActiveRequestIfNeeded(requestContext)
                    return
                }
                guard isRequestContextValid(requestContext) else { return }
                if isOfflineError(error) {
                    pendingRequests.append(
                        PendingAgentRequest(
                            prompt: trimmedPrompt,
                            history: historyAtSend,
                            mode: modeAtSend,
                            aiLevel: levelAtSend.rawValue,
                            executeAutomation: executeAutomationAtSend,
                            assistantMessageID: assistantID,
                            createdAt: .now
                        )
                    )
                    persistPendingRequests()
                    updateAssistantMessage(
                        id: assistantID,
                        text: AppLocalized.text(
                            "agent.queue.placeholder",
                            fallback: "Zwischengespeichert: Diese Anfrage wird automatisch gesendet, sobald deine Verbindung wieder da ist."
                        ),
                        isStreaming: false,
                        resultType: .progress
                    )
                    showUserToast(
                        AppLocalized.text(
                            "agent.queue.toast.queued",
                            fallback: "Offline gespeichert. Wird automatisch gesendet, sobald du wieder online bist."
                        ),
                        style: .info
                    )
                    lastIntegrationIssue = AppLocalized.text(
                        "agent.offline.message",
                        fallback: "Du bist offline. Der Agent arbeitet weiter, sobald deine Verbindung wieder da ist."
                    )
                    phase = .waitingReconnect
                    clearActiveRequestIfNeeded(requestContext)
                    return
                }

                let message = userFacingErrorMessage(for: error)
                lastIntegrationIssue = message
                updateAssistantMessage(
                    id: assistantID,
                    text: message,
                    isStreaming: false,
                    resultType: .error
                )
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .agent,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: trimmedPrompt,
                    response: message
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                showUserToast(message, style: .error)
                phase = .idle
                clearActiveRequestIfNeeded(requestContext)
            }
        }
    }

    func resetConversation() {
        startNewConversation()
    }

    func startNewConversation() {
        invalidateConversation(cancelActiveWork: true)
        messages = []
        pendingRequests.removeAll()
        let newSession = historyStore.createSession(
            userKey: currentUserKey,
            source: .agent,
            title: AIScriptHistoryStore.defaultSessionTitle
        )
        currentSessionID = newSession.id
        refreshConversationMetadata(preferredSessionID: newSession.id)
        syncSessionToRemote(newSession)
        phase = .idle
        lastAgentRunId = ""
    }

    func openConversation(_ sessionID: UUID) {
        guard !phase.shouldBlockComposerChrome else { return }
        invalidateConversation(cancelActiveWork: true)
        lastAgentRunId = ""
        restoreConversationState(preferredSessionID: sessionID)
        if NetworkStatusMonitor.shared.isOnline {
            startPendingRetryIfNeeded()
        }
    }

    func renameActiveConversation(_ title: String) {
        guard let currentSessionID else { return }
        let renamedSession = historyStore.renameSession(
            userKey: currentUserKey,
            source: .agent,
            sessionID: currentSessionID,
            title: title
        )
        refreshConversationMetadata(preferredSessionID: currentSessionID)
        if let renamedSession {
            syncSessionToRemote(renamedSession)
        }
    }

    func deleteActiveConversation() {
        guard let currentSessionID else { return }
        invalidateConversation(cancelActiveWork: true)
        historyStore.deleteSession(
            userKey: currentUserKey,
            source: .agent,
            sessionID: currentSessionID
        )
        pendingQueueStore.clearEntries(
            for: currentUserKey,
            sessionID: currentSessionID.uuidString
        )
        pendingRequests.removeAll()
        messages = []
        lastAgentRunId = ""
        restoreConversationState()
        deleteSessionFromRemote(currentSessionID)
        if NetworkStatusMonitor.shared.isOnline {
            startPendingRetryIfNeeded()
        }
    }

    private func buildHistory(from messages: [AgentChatMessage]) -> [AgentHistoryTurn] {
        messages
            .map { message in
                AgentHistoryTurn(
                    role: message.role == .user ? "user" : "assistant",
                    text: message.text
                )
            }
    }

    private func restoreConversationState(preferredSessionID: UUID? = nil) {
        let session = historyStore.ensureSession(
            userKey: currentUserKey,
            source: .agent,
            preferredSessionID: preferredSessionID
        )
        currentSessionID = session.id
        refreshConversationMetadata(preferredSessionID: session.id)
        pendingQueueStore.migrateLegacyEntries(
            for: currentUserKey,
            to: session.id.uuidString
        )

        let restoredEntries = historyStore.entries(
            for: currentUserKey,
            source: .agent,
            sessionID: session.id
        ).reversed()
        let restoredHistoryMessages = restoredEntries.flatMap { entry in
            [
                AgentChatMessage(role: .user, text: entry.prompt),
                AgentChatMessage(role: .assistant, text: entry.response)
            ]
        }

        pendingRequests = pendingQueueStore.entries(
            for: currentUserKey,
            sessionID: session.id.uuidString
        ).compactMap { entry in
            guard let assistantID = UUID(uuidString: entry.assistantMessageID) else { return nil }
            return PendingAgentRequest(
                prompt: entry.prompt,
                history: entry.history.map { turn in
                    AgentHistoryTurn(role: turn.role, text: turn.text)
                },
                mode: entry.mode,
                aiLevel: entry.aiLevel,
                executeAutomation: entry.executeAutomation,
                assistantMessageID: assistantID,
                createdAt: entry.createdAt
            )
        }

        let pendingMessages = pendingRequests.flatMap { request in
            [
                AgentChatMessage(role: .user, text: request.prompt),
                AgentChatMessage(
                    id: request.assistantMessageID,
                    role: .assistant,
                    text: AppLocalized.text(
                        "agent.queue.placeholder",
                        fallback: "Zwischengespeichert: Diese Anfrage wird automatisch gesendet, sobald deine Verbindung wieder da ist."
                    ),
                    resultType: .progress
                )
            ]
        }

        messages = restoredHistoryMessages + pendingMessages
        phase = pendingRequests.isEmpty ? .idle : .waitingReconnect
    }

    private func refreshConversationMetadata(preferredSessionID: UUID? = nil) {
        let session = historyStore.ensureSession(
            userKey: currentUserKey,
            source: .agent,
            preferredSessionID: preferredSessionID ?? currentSessionID
        )
        currentSessionID = session.id
        activeSessionID = session.id
        activeSessionTitle = session.title
        sessions = historyStore.sessionSummaries(for: currentUserKey, source: .agent)
    }

    private func normalizedUserKey(for userKey: String?) -> String? {
        let trimmed = userKey?.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let trimmed, !trimmed.isEmpty else { return nil }
        return trimmed.lowercased()
    }

    private func updateAssistantMessage(
        id: UUID,
        text: String,
        isStreaming: Bool,
        resultType: AgentResultType = .text,
        workflowSummary: AgentWorkflowSummary? = nil
    ) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].text = text
        messages[index].isStreaming = isStreaming
        messages[index].resultType = resultType
        messages[index].workflowSummary = workflowSummary
    }

    private func makeRequestContext(
        conversationRevision: Int? = nil,
        userKeyAtSend: String? = nil,
        sessionIDAtSend: UUID? = nil,
        aiLevelAtSend: AIExperienceLevel? = nil,
        requiresLevelMatch: Bool = true
    ) -> InFlightRequestContext {
        InFlightRequestContext(
            requestID: UUID(),
            conversationRevision: conversationRevision ?? self.conversationRevision,
            userKeyAtSend: userKeyAtSend ?? currentUserKey,
            sessionIDAtSend: sessionIDAtSend ?? currentSessionID,
            aiLevelAtSend: aiLevelAtSend ?? selectedLevel,
            requiresLevelMatch: requiresLevelMatch
        )
    }

    private func isRequestContextValid(_ context: InFlightRequestContext) -> Bool {
        activeRequestContext == context &&
        context.conversationRevision == conversationRevision &&
        context.userKeyAtSend == currentUserKey &&
        context.sessionIDAtSend == currentSessionID &&
        (!context.requiresLevelMatch || context.aiLevelAtSend == selectedLevel)
    }

    private func isConversationSnapshotValid(
        conversationRevision: Int,
        userKeyAtSend: String?,
        sessionIDAtSend: UUID?
    ) -> Bool {
        self.conversationRevision == conversationRevision &&
        currentUserKey == userKeyAtSend &&
        currentSessionID == sessionIDAtSend
    }

    private func clearActiveRequestIfNeeded(_ context: InFlightRequestContext) {
        guard activeRequestContext == context else { return }
        activeRequestContext = nil
        activeRequestTask = nil
    }

    private func invalidateConversation(cancelActiveWork: Bool) {
        if cancelActiveWork {
            activeRequestTask?.cancel()
            activeRequestTask = nil
            pendingRetryTask?.cancel()
            pendingRetryTask = nil
            remoteHydrationTask?.cancel()
            remoteHydrationTask = nil
            activeRequestContext = nil
        }
        conversationRevision += 1
    }

    private func queuePromptForRetry(_ trimmedPrompt: String) {
        let session = historyStore.ensureSession(
            userKey: currentUserKey,
            source: .agent,
            preferredSessionID: currentSessionID
        )
        currentSessionID = session.id
        refreshConversationMetadata(preferredSessionID: session.id)
        let assistantID = UUID()
        let userMessage = AgentChatMessage(role: .user, text: trimmedPrompt)
        let history = buildHistory(from: messages + [userMessage])
        let modeAtSend = selectedMode.rawValue
        let levelAtSend = selectedLevel
        let executeAutomationAtSend = shouldTriggerAutomation && canTriggerAutomation

        messages.append(userMessage)
        messages.append(
            AgentChatMessage(
                id: assistantID,
                role: .assistant,
                text: AppLocalized.text(
                    "agent.queue.placeholder",
                    fallback: "Zwischengespeichert: Diese Anfrage wird automatisch gesendet, sobald deine Verbindung wieder da ist."
                ),
                isStreaming: false,
                resultType: .progress
            )
        )
        draft = ""

        pendingRequests.append(
            PendingAgentRequest(
                prompt: trimmedPrompt,
                history: history,
                mode: modeAtSend,
                aiLevel: levelAtSend.rawValue,
                executeAutomation: executeAutomationAtSend,
                assistantMessageID: assistantID,
                createdAt: .now
            )
        )
        persistPendingRequests()

        showUserToast(
            AppLocalized.text(
                "agent.queue.toast.queued",
                fallback: "Offline gespeichert. Wird automatisch gesendet, sobald du wieder online bist."
            ),
            style: .info
        )
        lastIntegrationIssue = AppLocalized.text(
            "agent.offline.message",
            fallback: "Du bist offline. Der Agent arbeitet weiter, sobald deine Verbindung wieder da ist."
        )
        phase = .waitingReconnect
    }

    private func startPendingRetryIfNeeded() {
        guard pendingRetryTask == nil else { return }
        guard activeRequestTask == nil else { return }
        guard !pendingRequests.isEmpty else { return }

        let retryConversationRevision = conversationRevision
        let retryUserKey = currentUserKey
        let retrySessionID = currentSessionID
        pendingRetryTask = Task { @MainActor [weak self] in
            await self?.runPendingRetryLoop(
                conversationRevision: retryConversationRevision,
                userKeyAtSend: retryUserKey,
                sessionIDAtSend: retrySessionID
            )
        }
    }

    private func runPendingRetryLoop(
        conversationRevision: Int,
        userKeyAtSend: String?,
        sessionIDAtSend: UUID?
    ) async {
        guard isConversationSnapshotValid(
            conversationRevision: conversationRevision,
            userKeyAtSend: userKeyAtSend,
            sessionIDAtSend: sessionIDAtSend
        ) else {
            pendingRetryTask = nil
            return
        }

        phase = .executing
        defer {
            pendingRetryTask = nil
            activeRequestContext = nil
        }

        while NetworkStatusMonitor.shared.isOnline, !pendingRequests.isEmpty {
            guard isConversationSnapshotValid(
                conversationRevision: conversationRevision,
                userKeyAtSend: userKeyAtSend,
                sessionIDAtSend: sessionIDAtSend
            ) else { return }

            let request = pendingRequests.removeFirst()
            let requestContext = makeRequestContext(
                conversationRevision: conversationRevision,
                userKeyAtSend: userKeyAtSend,
                sessionIDAtSend: sessionIDAtSend,
                aiLevelAtSend: AIExperienceLevel(rawValue: request.aiLevel) ?? .standard,
                requiresLevelMatch: false
            )
            activeRequestContext = requestContext

            do {
                let result = try await service.sendMessage(
                    prompt: request.prompt,
                    history: request.history,
                    mode: request.mode,
                    aiLevel: request.aiLevel,
                    executeAutomation: request.executeAutomation,
                    manusApiKeyOverride: ManusBYOSStore.shared.currentAPIKeyOrNil()
                )
                guard isRequestContextValid(requestContext) else { return }

                updateProviderDiagnostics(from: result)
                applyUsage(result.usage)
                historyStore.updateRetentionDays(result.historyRetentionDays)
                let replyText = augmentedReplyText(from: result)
                updateAssistantMessage(
                    id: request.assistantMessageID,
                    text: replyText,
                    isStreaming: false,
                    resultType: (result.automationTriggered || result.automationAttempted) ? .workflow : .text,
                    workflowSummary: buildWorkflowSummary(from: result)
                )
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .agent,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: request.prompt,
                    response: replyText
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                if result.automationTriggered {
                    lastIntegrationIssue = ""
                    showUserToast(
                        result.automationMessage.isEmpty
                            ? AppLocalized.text("agent.automation.triggered", fallback: "Automation wurde gestartet.")
                            : result.automationMessage,
                        style: .success
                    )
                } else if result.automationAttempted && !result.automationMessage.isEmpty {
                    lastIntegrationIssue = result.automationMessage
                    showUserToast(result.automationMessage, style: .error)
                } else {
                    lastIntegrationIssue = ""
                    showUserToast(
                        AppLocalized.text(
                            "agent.queue.toast.sent",
                            fallback: "Zwischengespeicherte Anfrage erfolgreich gesendet."
                        ),
                        style: .success
                    )
                }
                let hasMorePendingRequests = !pendingRequests.isEmpty
                persistPendingRequests()
                phase = hasMorePendingRequests
                    ? .executing
                    : resolvedTerminalPhase(for: result.decision, hasPendingQueue: false)
                activeRequestContext = nil
            } catch {
                if error is CancellationError || Task.isCancelled {
                    activeRequestContext = nil
                    return
                }
                guard isRequestContextValid(requestContext) else { return }

                if isOfflineError(error) || !NetworkStatusMonitor.shared.isOnline {
                    pendingRequests.insert(request, at: 0)
                    persistPendingRequests()
                    updateAssistantMessage(
                        id: request.assistantMessageID,
                        text: AppLocalized.text(
                            "agent.queue.placeholder",
                            fallback: "Zwischengespeichert: Diese Anfrage wird automatisch gesendet, sobald deine Verbindung wieder da ist."
                        ),
                        isStreaming: false,
                        resultType: .progress
                    )
                    activeRequestContext = nil
                    break
                }

                let message = userFacingErrorMessage(for: error)
                lastIntegrationIssue = message
                updateAssistantMessage(
                    id: request.assistantMessageID,
                    text: message,
                    isStreaming: false,
                    resultType: .error
                )
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .agent,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: request.prompt,
                    response: message
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                showUserToast(message, style: .error)
                persistPendingRequests()
                activeRequestContext = nil
            }
        }

        guard isConversationSnapshotValid(
            conversationRevision: conversationRevision,
            userKeyAtSend: userKeyAtSend,
            sessionIDAtSend: sessionIDAtSend
        ) else { return }

        if !pendingRequests.isEmpty {
            phase = .waitingReconnect
        } else if phase == .executing {
            phase = .idle
        }
    }

    private func persistPendingRequests() {
        let normalizedUserKey = normalizedUserKey(for: currentUserKey)
        let fallbackUserKey = normalizedUserKey ?? "guest"
        let activeSessionIdentifier = currentSessionID?.uuidString ?? ""
        let entries = pendingRequests.map { request in
            AgentPendingQueueEntry(
                userKey: fallbackUserKey,
                sessionID: activeSessionIdentifier,
                prompt: request.prompt,
                history: request.history.map { turn in
                    AgentPendingQueueTurn(role: turn.role, text: turn.text)
                },
                mode: request.mode,
                aiLevel: request.aiLevel,
                executeAutomation: request.executeAutomation,
                assistantMessageID: request.assistantMessageID.uuidString,
                createdAt: request.createdAt
            )
        }
        pendingQueueStore.saveEntries(
            entries,
            for: normalizedUserKey,
            sessionID: activeSessionIdentifier
        )
    }

    private func hydrateRemoteHistoryIfNeeded(preferredSessionID: UUID?) {
        remoteHydrationTask?.cancel()
        guard let currentUserID else { return }

        let source: AIScriptHistorySource = .agent
        let userID = currentUserID
        let userKey = currentUserKey
        let retentionDays = currentHistoryRetentionDays
        let localSessions = historyStore.sessionRecords(for: userKey, source: source)
        let localEntries = historyStore.entries(for: userKey, source: source)

        remoteHydrationTask = Task { @MainActor [weak self] in
            guard let self else { return }
            do {
                _ = try await syncService.migrateLocalHistoryIfRemoteEmpty(
                    userID: userID,
                    source: source,
                    localSessions: localSessions,
                    localEntries: localEntries
                )
                try? await syncService.pruneHistory(
                    userID: userID,
                    source: source,
                    retentionDays: retentionDays
                )
                let refreshedSnapshot = try await syncService.fetchSnapshot(userID: userID, source: source)
                guard self.currentUserID == userID, self.currentUserKey == userKey else { return }
                historyStore.replaceRemoteState(
                    userKey: userKey,
                    source: source,
                    sessions: refreshedSnapshot.sessions,
                    entries: refreshedSnapshot.entries
                )
                restoreConversationState(preferredSessionID: preferredSessionID)
                if NetworkStatusMonitor.shared.isOnline {
                    startPendingRetryIfNeeded()
                }
            } catch {
                // Keep local history as fallback when remote sync is unavailable.
            }
        }
    }

    private func startRemoteHistoryObservation() {
        guard let currentUserID else { return }
        let userID = currentUserID
        let userKey = currentUserKey
        stopRemoteHistoryObservation = syncService.observeSnapshot(
            userID: userID,
            source: .agent
        ) { [weak self] result in
            Task { @MainActor [weak self] in
                guard let self,
                      self.currentUserID == userID,
                      self.currentUserKey == userKey else {
                    return
                }

                switch result {
                case .success(let snapshot):
                    self.applyRemoteSnapshot(snapshot, userKey: userKey)
                case .failure:
                    break
                }
            }
        }
    }

    private func applyRemoteSnapshot(_ snapshot: AIScriptHistoryRemoteSnapshot, userKey: String?) {
        historyStore.replaceRemoteState(
            userKey: userKey,
            source: .agent,
            sessions: snapshot.sessions,
            entries: snapshot.entries
        )

        if phase.shouldBlockComposerChrome {
            refreshConversationMetadata(preferredSessionID: currentSessionID)
        } else {
            restoreConversationState(preferredSessionID: currentSessionID)
            if NetworkStatusMonitor.shared.isOnline {
                startPendingRetryIfNeeded()
            }
        }
    }

    private func syncSessionToRemote(_ session: AIScriptHistorySession) {
        guard let currentUserID else { return }
        let retentionDays = currentHistoryRetentionDays
        Task { @MainActor in
            do {
                try await syncService.upsertSession(userID: currentUserID, session: session)
                try? await syncService.pruneHistory(userID: currentUserID, source: .agent, retentionDays: retentionDays)
            } catch {
                // Local cache stays authoritative until the next successful sync.
            }
        }
    }

    private func syncSavedEntryToRemote(_ saveResult: AIScriptHistorySaveResult) {
        guard let currentUserID else { return }
        let retentionDays = currentHistoryRetentionDays
        Task { @MainActor in
            do {
                try await syncService.upsertSession(userID: currentUserID, session: saveResult.session)
                try await syncService.upsertEntry(userID: currentUserID, entry: saveResult.entry)
                try? await syncService.pruneHistory(userID: currentUserID, source: .agent, retentionDays: retentionDays)
            } catch {
                // Local cache stays authoritative until the next successful sync.
            }
        }
    }

    private func deleteSessionFromRemote(_ sessionID: UUID) {
        guard let currentUserID else { return }
        Task { @MainActor in
            do {
                try await syncService.deleteSession(userID: currentUserID, sessionID: sessionID)
            } catch {
                // Ignore and let the next hydration pull the remote truth again.
            }
        }
    }

    private func pruneRemoteHistoryIfNeeded() {
        guard let currentUserID else { return }
        let retentionDays = currentHistoryRetentionDays
        Task { @MainActor in
            try? await syncService.pruneHistory(
                userID: currentUserID,
                source: .agent,
                retentionDays: retentionDays
            )
        }
    }

    private func augmentedReplyText(from response: AgentChatResponse) -> String {
        guard response.automationTriggered else { return response.reply }
        let workflowLabel = response.workflowName.trimmingCharacters(in: .whitespacesAndNewlines)
        let automationLabel = workflowLabel.isEmpty ? "Workflow" : workflowLabel
        let message = response.automationMessage.trimmingCharacters(in: .whitespacesAndNewlines)
        let suffix = message.isEmpty ? "An \(automationLabel) uebergeben." : message
        return "\(response.reply)\n\nWorkflow:\n\(suffix)"
    }

    private func buildWorkflowSummary(from response: AgentChatResponse) -> AgentWorkflowSummary? {
        let structuredWorkflow = response.results.first(where: { $0.type == "workflow" })
        guard response.automationTriggered || response.automationAttempted || structuredWorkflow != nil else { return nil }
        let workflowLabel = response.workflowName.trimmingCharacters(in: .whitespacesAndNewlines)
        let resolvedWorkflowLabel = (structuredWorkflow?.workflowName ?? workflowLabel)
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .isEmpty ? "External Workflow" : (structuredWorkflow?.workflowName ?? workflowLabel)
        let statusText: String
        if let structuredSummary = structuredWorkflow?.summary, !structuredSummary.isEmpty {
            statusText = structuredSummary
        } else if response.automationTriggered {
            statusText = response.automationMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?
            "Workflow wurde gestartet." :
            response.automationMessage
        } else {
            statusText = response.automationMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?
            "Workflow konnte nicht gestartet werden." :
            response.automationMessage
        }
        let runId = (structuredWorkflow?.runId ?? response.agentRunId).trimmingCharacters(in: .whitespacesAndNewlines)
        return AgentWorkflowSummary(
            workflowName: resolvedWorkflowLabel,
            statusText: statusText,
            runID: runId.isEmpty ? nil : runId
        )
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    func showToastMessage(_ message: String, style: ToastStyle) {
        showUserToast(message, style: style)
    }

    private func applyUsage(_ usage: AIUsageSnapshot?) {
        guard let usage else { return }
        revenueUsage = RevenueUsageState(
            planTitle: currentPlanTitle,
            remaining: max(usage.remainingForKind, 0),
            limit: max(usage.limitForKind, 0),
            warningLevel: usage.warningLevel,
            userFacingReason: usage.userFacingReason,
            suggestedUpgrade: usage.suggestedUpgrade,
            resetHint: usage.resetHint,
            retryAfterSeconds: usage.retryAfterSeconds,
            lowerCostOption: usage.lowerCostOption
        )
    }

    private func updateProviderDiagnostics(from response: AgentChatResponse) {
        let raw = response.agentProvider
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        if !raw.isEmpty, let resolved = AIRuntimeAgentProvider(rawValue: raw) {
            lastAgentProvider = resolved
        }

        let trimmedNotice = response.providerNotice.trimmingCharacters(in: .whitespacesAndNewlines)
        if response.providerFallbackUsed || !trimmedNotice.isEmpty {
            lastProviderNotice = trimmedNotice.isEmpty
                ? "Provider-Fallback aktiv."
                : trimmedNotice
        } else {
            lastProviderNotice = ""
        }

        let trimmedRun = response.agentRunId.trimmingCharacters(in: .whitespacesAndNewlines)
        lastAgentRunId = trimmedRun
    }

    private func resolvedTerminalPhase(for decision: AgentDecision?, hasPendingQueue: Bool) -> AgentInteractionPhase {
        guard let decision else {
            return hasPendingQueue ? .waitingReconnect : .completed
        }
        if decision.ownerDiagnosticActive {
            return .ownerDiagnostic
        }
        switch decision.state {
        case "planning":
            return .planning
        case "webhook_pending":
            return .webhookPending
        case "external_running":
            return .externalRunning
        case "awaiting_external_auth":
            return .awaitingExternalAuth
        case "external_failed":
            return .externalFailed
        case "external_completed":
            return .externalCompleted
        case "fallback_internal":
            return .fallbackInternal
        case "awaiting_confirmation":
            return .awaitingConfirmation
        case "executing":
            return .executing
        case "tool_pending":
            return .toolPending
        case "completed":
            return .completed
        case "partial":
            return .partial
        case "blocked":
            return .blocked
        case "failed":
            return .failed
        case "retryable":
            return .retryable
        case "cancelled":
            return .cancelled
        case "idle":
            return hasPendingQueue ? .waitingReconnect : .idle
        default:
            return hasPendingQueue ? .waitingReconnect : .completed
        }
    }

    private func isOfflineError(_ error: Error) -> Bool {
        let nsError = error as NSError
        return nsError.code == NSURLErrorNotConnectedToInternet || nsError.code == -1009
    }

    private func userFacingErrorMessage(for error: Error) -> String {
        let nsError = error as NSError

        if isOfflineError(error) {
            return AppLocalized.text(
                "agent.offline.message",
                fallback: "Du bist offline. Der Agent setzt automatisch fort, sobald die Verbindung wieder da ist."
            )
        }

        if nsError.domain == FunctionsErrorDomain,
           let code = FunctionsErrorCode(rawValue: nsError.code) {
            switch code {
            case .notFound, .unimplemented:
                return "Dieser Agent-Bereich wird gerade vorbereitet."
            case .unavailable:
                return "Der SkyOS Agent ist gerade kurz nicht erreichbar."
            case .deadlineExceeded:
                return "Die Agent-Antwort dauert gerade laenger als gewohnt."
            case .resourceExhausted:
                return AIExperienceLevel.limitReachedMessage
            case .invalidArgument:
                return "Die Anfrage braucht noch etwas mehr Kontext."
            case .failedPrecondition:
                if nsError.localizedDescription.localizedCaseInsensitiveContains("App Check") {
                    return "Der Sicherheitscheck laeuft noch. Bitte kurz erneut versuchen."
                }
                return "Der Agent ist gerade noch nicht voll bereit. Bitte in einem Moment erneut versuchen."
            case .permissionDenied:
                return AIExperienceLevel.unavailableMessage
            case .unauthenticated:
                return "Bitte melde dich erneut an und versuch es noch einmal."
            default:
                break
            }
        }

        return "Der SkyOS Agent ist gerade kurz pausiert. Bitte in einem Moment erneut versuchen."
    }
}
