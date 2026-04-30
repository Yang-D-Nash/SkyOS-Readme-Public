import Foundation
import Combine
import FirebaseFunctions
import FirebaseFirestore
import FirebaseAuth

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
    let progressPercent: Int?
    let step: String
    let etaSeconds: Int?
    let details: String
    let schemaVersion: String
}

struct AgentChatMessage: Identifiable, Equatable {
    let id: UUID
    let role: AgentChatRole
    var text: String
    var isStreaming: Bool
    var resultType: AgentResultType
    var workflowSummary: AgentWorkflowSummary?
    var results: [AgentResultEntry]
    /// Modus der Anfrage, die diese Antwort erzeugt hat (fuer „Weiter in …“).
    var responseMode: AgentExecutionMode?

    init(
        id: UUID = UUID(),
        role: AgentChatRole,
        text: String,
        isStreaming: Bool = false,
        resultType: AgentResultType = .text,
        workflowSummary: AgentWorkflowSummary? = nil,
        results: [AgentResultEntry] = [],
        responseMode: AgentExecutionMode? = nil
    ) {
        self.id = id
        self.role = role
        self.text = text
        self.isStreaming = isStreaming
        self.resultType = resultType
        self.workflowSummary = workflowSummary
        self.results = results
        self.responseMode = responseMode
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
            return "Analyse"
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
            return "Zum Beispiel: Instagram, TikTok, YouTube, Meta oder Spotify analysieren."
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
                "Analysiere Instagram und TikTok: welche Hooks, Formate und naechsten Tests sind sinnvoll?",
                "Vergleiche YouTube, Meta und Spotify und gib mir eine klare Content-Prioritaet.",
                "Pruefe dieses Profil fuer Release-Potenzial: Signale, Risiken, naechste drei Schritte.",
                "Baue aus der Social-Analyse eine speicherbare Note mit Tasks fuer den naechsten Workflow."
            ]
        }
    }
}

enum AgentAutomationScope: String, CaseIterable, Identifiable {
    case owner = "owner"
    case personal = "personal"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .owner: return "App-Flow"
        case .personal: return "Eigener Flow"
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
    @Published var selectedAutomationScope: AgentAutomationScope = .owner
    @Published var shouldTriggerAutomation = false
    @Published var socialInstagramEnabled = false { didSet { persistSocialSetupDraft() } }
    @Published var socialInstagramHandle = "" { didSet { persistSocialSetupDraft() } }
    @Published var socialTiktokEnabled = false { didSet { persistSocialSetupDraft() } }
    @Published var socialTiktokHandle = "" { didSet { persistSocialSetupDraft() } }
    @Published var socialYoutubeEnabled = false { didSet { persistSocialSetupDraft() } }
    @Published var socialYoutubeHandle = "" { didSet { persistSocialSetupDraft() } }
    @Published var socialFacebookEnabled = false { didSet { persistSocialSetupDraft() } }
    @Published var socialFacebookHandle = "" { didSet { persistSocialSetupDraft() } }
    @Published var socialSpotifyEnabled = false { didSet { persistSocialSetupDraft() } }
    @Published var socialSpotifyHandle = "" { didSet { persistSocialSetupDraft() } }
    @Published private(set) var canTriggerAutomation = false
    /// When false, the global "App-Flow" (owner webhook) is hidden; use personal automation only.
    @Published private(set) var canUseGlobalOwnerAutomationFlow = false
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
    private var workflowStatusTask: Task<Void, Never>?
    private var remoteHydrationTask: Task<Void, Never>?
    private var stopRemoteHistoryObservation: (() -> Void)?
    private var activeRequestContext: InFlightRequestContext?
    private var currentQuotaPlan: UserQuotaPlan = .free
    private let firestore = Firestore.firestore()
    private var isRestoringSocialSetup = false

    private struct PendingAgentRequest {
        let prompt: String
        let history: [AgentHistoryTurn]
        let mode: String
        let aiLevel: String
        let executeAutomation: Bool
        let automationScope: String
        let assistantMessageID: UUID
        let createdAt: Date
        let attachments: [AgentOutboundAttachment]
        let idempotencyKey: String
        let socialSetup: AgentSocialSetupInput
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
        workflowStatusTask?.cancel()
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
        selectedLevel = resolvedAgentExperienceLevel(for: currentQuotaPlan)
        historyStore.updateRetentionDays(currentHistoryRetentionDays)
        canTriggerAutomation = user != nil
        let previousCouldUseGlobal = canUseGlobalOwnerAutomationFlow
        let isPlatformOwner = user?.isPlatformOwner == true
        canUseGlobalOwnerAutomationFlow = isPlatformOwner
        if !isPlatformOwner {
            selectedAutomationScope = .personal
        } else if !previousCouldUseGlobal {
            selectedAutomationScope = .owner
        }
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
        restoreSocialSetupDraft()
        restoreConversationState()
        hydrateRemoteHistoryIfNeeded(preferredSessionID: currentSessionID)
        startRemoteHistoryObservation()
        if NetworkStatusMonitor.shared.isOnline {
            startPendingRetryIfNeeded()
        }
        if normalizedUserID != nil {
            startNewConversation()
        }
    }

    func sendDraft(attachmentURLs: [URL] = []) {
        sendPrompt(draft, attachmentURLs: attachmentURLs)
    }

    func sendDraftInNewConversation(attachmentURLs: [URL] = []) {
        let prompt = draft
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !phase.shouldBlockSend else { return }
        guard canSendAgentMessage(trimmedPrompt: trimmedPrompt) else {
            let msg = AppLocalized.text(
                "agent.send.requires_social_workflow",
                fallback: "Fuer Analyse mit Workflow: mindestens eine oeffentliche Plattform aktivieren."
            )
            showUserToast(msg, style: .error)
            return
        }
        guard selectedLevel.isAvailable(for: currentQuotaPlan) else {
            showUserToast(AIExperienceLevel.unavailableMessage, style: .info)
            return
        }
        startNewConversation()
        sendPrompt(prompt, attachmentURLs: attachmentURLs)
    }

    private func hasAnySocialToggleOn() -> Bool {
        socialInstagramEnabled || socialTiktokEnabled || socialYoutubeEnabled
            || socialFacebookEnabled || socialSpotifyEnabled
    }

    /// Analyse + aktiver Workflow: mindestens eine Plattform. Ohne Workflow: kein Zwang.
    func canSendAgentMessage(trimmedPrompt: String) -> Bool {
        guard !trimmedPrompt.isEmpty, !phase.shouldBlockSend else { return false }
        if selectedMode == .automation, canTriggerAutomation, shouldTriggerAutomation {
            return hasAnySocialToggleOn()
        }
        return true
    }

    /// Fuer die Thread-Eingabezeile und den Prompt — gleiche Regeln wie `sendPrompt`.
    var isCurrentDraftSendable: Bool {
        canSendAgentMessage(trimmedPrompt: draft.trimmingCharacters(in: .whitespacesAndNewlines))
    }

    /// Nach Assistenten-Antwort: anderen Modus waehlen; Entwurf enthaelt Text, Workflow-Status und strukturierte Ergebnisse.
    func continueInModeFromAssistant(_ target: AgentExecutionMode, sourceMessage: AgentChatMessage) {
        let snippet = buildAssistantContinuationContext(from: sourceMessage)
        guard !snippet.isEmpty else { return }
        draft = "Anknuepfend an die letzte Antwort — bitte vertiefen:\n\n\(snippet)"
        selectedMode = target
        if target == .automation, canTriggerAutomation {
            shouldTriggerAutomation = true
        } else {
            shouldTriggerAutomation = false
        }
    }

    private func buildAssistantContinuationContext(from message: AgentChatMessage) -> String {
        var sections: [String] = []
        let body = message.text.trimmingCharacters(in: .whitespacesAndNewlines)
        if !body.isEmpty {
            sections.append(body)
        }
        if let ws = message.workflowSummary {
            var w = "[Workflow]\n"
            w += ws.workflowName.trimmingCharacters(in: .whitespacesAndNewlines) + "\n"
            w += ws.statusText.trimmingCharacters(in: .whitespacesAndNewlines)
            if !ws.details.isEmpty { w += "\n" + ws.details }
            if !ws.step.isEmpty { w += "\nSchritt: " + ws.step }
            if let run = ws.runID, !run.isEmpty { w += "\nRun: " + run }
            sections.append(w)
        }
        if !message.results.isEmpty {
            let lines = message.results.compactMap { continuationSnippet(from: $0) }
            if !lines.isEmpty {
                sections.append("[Ergebnisse]\n" + lines.joined(separator: "\n"))
            }
        }
        return String(sections.joined(separator: "\n\n---\n\n").prefix(4_000))
    }

    private func continuationSnippet(from entry: AgentResultEntry) -> String? {
        let kind = entry.type.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let primary: String?
        if !entry.text.isEmpty {
            primary = entry.text
        } else if !entry.summary.isEmpty {
            primary = entry.summary
        } else if !entry.title.isEmpty, !entry.url.isEmpty {
            primary = "\(entry.title): \(entry.url)"
        } else if !entry.url.isEmpty {
            primary = entry.url
        } else if !entry.title.isEmpty {
            primary = entry.title
        } else if !entry.fileName.isEmpty {
            primary = entry.fileName
        } else if !entry.html.isEmpty {
            primary = String(entry.html.prefix(600))
        } else if !entry.rows.isEmpty {
            primary = entry.rows.prefix(8).map { $0.joined(separator: " | ") }.joined(separator: "\n")
        } else if !entry.workflowName.isEmpty || !entry.status.isEmpty || !entry.runId.isEmpty {
            let parts: [String] = [
                entry.workflowName,
                entry.status,
                entry.runId.isEmpty ? "" : "Run: \(entry.runId)",
            ].compactMap { s in
                let t = s.trimmingCharacters(in: .whitespacesAndNewlines)
                return t.isEmpty ? nil : t
            }
            primary = parts.isEmpty ? nil : parts.joined(separator: " — ")
        } else {
            primary = nil
        }
        guard let p = primary else { return nil }
        return "(\(kind)) " + String(p.prefix(1_200))
    }

    func sendPrompt(_ prompt: String, attachmentURLs: [URL] = []) {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !phase.shouldBlockSend else { return }
        guard canSendAgentMessage(trimmedPrompt: trimmedPrompt) else {
            let msg = AppLocalized.text(
                "agent.send.requires_social_workflow",
                fallback: "Fuer Analyse mit Workflow: mindestens eine oeffentliche Plattform aktivieren."
            )
            showUserToast(msg, style: .error)
            return
        }
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
        let preparedAttachments = AgentOutboundAttachment.batchInline(fileURLs: attachmentURLs)
        guard NetworkStatusMonitor.shared.isOnline else {
            queuePromptForRetry(trimmedPrompt, attachments: preparedAttachments)
            return
        }

        phase = .planning

        let modeAtSend = selectedMode.rawValue
        let levelAtSend = selectedLevel
        let executeAutomationAtSend = shouldTriggerAutomation && canTriggerAutomation
        let automationScopeAtSend = selectedAutomationScope.rawValue
        let socialSetupAtSend = resolveSocialSetupForOutgoing(mode: modeAtSend)
        let idempotencyKeyAtSend = executeAutomationAtSend ? UUID().uuidString : ""
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
                resultType: .progress,
                responseMode: selectedMode
            )
        )
        draft = ""

        activeRequestContext = requestContext
        activeRequestTask?.cancel()
        activeRequestTask = Task { @MainActor [weak self] in
            guard let self else { return }
            do {
                let result = try await service.sendMessage(
                    prompt: await memoryEnrichedPrompt(from: trimmedPrompt),
                    history: historyAtSend,
                    mode: modeAtSend,
                    aiLevel: levelAtSend.rawValue,
                    executeAutomation: executeAutomationAtSend,
                    automationScope: automationScopeAtSend,
                    manusApiKeyOverride: ManusBYOSStore.shared.currentAPIKeyOrNil(),
                    idempotencyKey: idempotencyKeyAtSend.isEmpty ? nil : idempotencyKeyAtSend,
                    attachments: preparedAttachments,
                    socialSetup: socialSetupAtSend.hasAnySelection ? socialSetupAtSend : nil
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
                    workflowSummary: buildWorkflowSummary(from: result),
                    results: result.results
                )
                startWorkflowStatusPollingIfNeeded(
                    response: result,
                    assistantMessageID: assistantID,
                    requestContext: requestContext
                )
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .agent,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: trimmedPrompt,
                    response: replyText,
                    resultType: result.resultType,
                    automationMessage: result.automationMessage,
                    workflowName: result.workflowName,
                    agentRunID: result.agentRunId,
                    structuredResults: result.results.map(\.historyResultEntry)
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                if result.automationTriggered {
                    lastIntegrationIssue = ""
                    let successMessage = resultLooksLikeReminderCreation(result)
                        ? "Reminder erstellt"
                        : (resultLooksLikeTaskCreation(result)
                        ? AppLocalized.text("tasks.created", fallback: "Task created")
                        : (resultLooksLikeNoteCreation(result)
                            ? AppLocalized.text("notes.saved", fallback: "Note saved")
                        : (
                            result.automationMessage.isEmpty
                            ? AppLocalized.text("agent.automation.triggered", fallback: "Automation wurde gestartet.")
                            : result.automationMessage
                        )))
                    showUserToast(successMessage, style: .success)
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
                            automationScope: automationScopeAtSend,
                            assistantMessageID: assistantID,
                            createdAt: .now,
                            attachments: preparedAttachments,
                            idempotencyKey: idempotencyKeyAtSend,
                            socialSetup: socialSetupAtSend
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

    func refreshActiveConversation() {
        guard !phase.shouldBlockComposerChrome else { return }
        invalidateConversation(cancelActiveWork: true)
        lastAgentRunId = ""
        restoreConversationState(preferredSessionID: currentSessionID)
        if NetworkStatusMonitor.shared.isOnline {
            startPendingRetryIfNeeded()
        }
        showUserToast("Chat aktualisiert.", style: .success)
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
                automationScope: entry.automationScope,
                assistantMessageID: assistantID,
                createdAt: entry.createdAt,
                attachments: entry.attachments,
                idempotencyKey: entry.idempotencyKey,
                socialSetup: entry.socialSetup
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
                    resultType: .progress,
                    responseMode: AgentExecutionMode(rawValue: request.mode)
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
        workflowSummary: AgentWorkflowSummary? = nil,
        results: [AgentResultEntry] = []
    ) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].text = text
        messages[index].isStreaming = isStreaming
        messages[index].resultType = resultType
        messages[index].workflowSummary = workflowSummary
        messages[index].results = results
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
            workflowStatusTask?.cancel()
            workflowStatusTask = nil
            remoteHydrationTask?.cancel()
            remoteHydrationTask = nil
            activeRequestContext = nil
        }
        conversationRevision += 1
    }

    private func queuePromptForRetry(_ trimmedPrompt: String, attachments: [AgentOutboundAttachment] = []) {
        guard canSendAgentMessage(trimmedPrompt: trimmedPrompt) else {
            let msg = AppLocalized.text(
                "agent.send.requires_social_workflow",
                fallback: "Fuer Analyse mit Workflow: mindestens eine oeffentliche Plattform aktivieren."
            )
            showUserToast(msg, style: .error)
            return
        }
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
        let automationScopeAtSend = selectedAutomationScope.rawValue
        let socialSetupAtQueue = resolveSocialSetupForOutgoing(mode: modeAtSend)
        let idempotencyKeyAtQueue = executeAutomationAtSend ? UUID().uuidString : ""

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
                resultType: .progress,
                responseMode: selectedMode
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
                automationScope: automationScopeAtSend,
                assistantMessageID: assistantID,
                createdAt: .now,
                attachments: attachments,
                idempotencyKey: idempotencyKeyAtQueue,
                socialSetup: socialSetupAtQueue
            )
        )
        persistPendingRequests()

        let toastKey = attachments.isEmpty ? "agent.queue.toast.queued" : "agent.queue.toast.queued.with_attachments"
        let toastFallback = attachments.isEmpty ?
            "Offline gespeichert. Wird automatisch gesendet, sobald du wieder online bist." :
            "Offline gespeichert inkl. Dateien. Wird automatisch gesendet, sobald du wieder online bist."
        showUserToast(
            AppLocalized.text(toastKey, fallback: toastFallback),
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
                    prompt: await memoryEnrichedPrompt(from: request.prompt),
                    history: request.history,
                    mode: request.mode,
                    aiLevel: request.aiLevel,
                    executeAutomation: request.executeAutomation,
                    automationScope: request.automationScope,
                    manusApiKeyOverride: ManusBYOSStore.shared.currentAPIKeyOrNil(),
                    idempotencyKey: request.idempotencyKey.isEmpty ? nil : request.idempotencyKey,
                    attachments: request.attachments,
                    socialSetup: request.socialSetup.hasAnySelection ? request.socialSetup : nil
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
                    workflowSummary: buildWorkflowSummary(from: result),
                    results: result.results
                )
                startWorkflowStatusPollingIfNeeded(
                    response: result,
                    assistantMessageID: request.assistantMessageID,
                    requestContext: requestContext
                )
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .agent,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: request.prompt,
                    response: replyText,
                    resultType: result.resultType,
                    automationMessage: result.automationMessage,
                    workflowName: result.workflowName,
                    agentRunID: result.agentRunId,
                    structuredResults: result.results.map(\.historyResultEntry)
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                if result.automationTriggered {
                    lastIntegrationIssue = ""
                    let successMessage = resultLooksLikeReminderCreation(result)
                        ? "Reminder erstellt"
                        : (resultLooksLikeTaskCreation(result)
                        ? AppLocalized.text("tasks.created", fallback: "Task created")
                        : (resultLooksLikeNoteCreation(result)
                            ? AppLocalized.text("notes.saved", fallback: "Note saved")
                        : (
                            result.automationMessage.isEmpty
                            ? AppLocalized.text("agent.automation.triggered", fallback: "Automation wurde gestartet.")
                            : result.automationMessage
                        )))
                    showUserToast(successMessage, style: .success)
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
                automationScope: request.automationScope,
                assistantMessageID: request.assistantMessageID.uuidString,
                createdAt: request.createdAt,
                attachments: request.attachments,
                idempotencyKey: request.idempotencyKey,
                socialSetup: request.socialSetup
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
        let statusText = readableAutomationMessage(
            response.automationMessage,
            workflowLabel: automationLabel,
            wasTriggered: true
        )
        let createdSummary = createdAutomationSummary(from: response)
        var sections: [String] = []
        let reply = response.reply.trimmingCharacters(in: .whitespacesAndNewlines)
        if !reply.isEmpty {
            sections.append(reply)
        }
        sections.append("Workflow\n\(statusText)")
        sections.append("Angelegt\n\(createdSummary)")
        sections.append("Home\nOeffne Home > Productivity, um Eintraege zu pruefen oder zu bearbeiten.")
        return sections.joined(separator: "\n\n")
    }

    private func isGenericAutomationMessage(_ value: String, workflowLabel: String) -> Bool {
        let message = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if message.isEmpty { return true }
        let lower = message.lowercased()
        if ["{}", "[]", "null", "undefined", "ok", "done", "completed", "complete", "success", "successful", "erledigt", "fertig"].contains(lower) {
            return true
        }
        if lower.hasPrefix("test an ") { return true }
        if lower.hasPrefix("workflow completed") || lower.hasPrefix("workflow status") || lower.hasPrefix("workflow ok") {
            return true
        }
        return lower == workflowLabel.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    private func readableAutomationMessage(_ value: String, workflowLabel: String, wasTriggered: Bool) -> String {
        let collapsed = value
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
        if !isGenericAutomationMessage(collapsed, workflowLabel: workflowLabel) {
            if collapsed.count > 600 {
                return String(collapsed.prefix(600)) + "..."
            }
            return collapsed
        }
        return wasTriggered ? "Workflow abgeschlossen." : "Workflow konnte nicht abgeschlossen werden."
    }

    private func buildWorkflowSummary(from response: AgentChatResponse) -> AgentWorkflowSummary? {
        let structuredWorkflow = response.results.first(where: { $0.type == "workflow" })
        guard response.automationTriggered || response.automationAttempted || structuredWorkflow != nil else { return nil }
        let workflowLabel = response.workflowName.trimmingCharacters(in: .whitespacesAndNewlines)
        let resolvedWorkflowLabel = (structuredWorkflow?.workflowName ?? workflowLabel)
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .isEmpty ? "External Workflow" : (structuredWorkflow?.workflowName ?? workflowLabel)
        let statusText: String
        if let structuredSummary = structuredWorkflow?.summary,
           !structuredSummary.isEmpty,
           !isGenericAutomationMessage(structuredSummary, workflowLabel: resolvedWorkflowLabel) {
            statusText = structuredSummary
        } else if response.automationTriggered {
            statusText = readableAutomationMessage(
                response.automationMessage,
                workflowLabel: resolvedWorkflowLabel,
                wasTriggered: true
            )
        } else {
            statusText = readableAutomationMessage(
                response.automationMessage,
                workflowLabel: resolvedWorkflowLabel,
                wasTriggered: false
            )
        }
        let runId = (structuredWorkflow?.runId ?? response.agentRunId).trimmingCharacters(in: .whitespacesAndNewlines)
        return AgentWorkflowSummary(
            workflowName: resolvedWorkflowLabel,
            statusText: statusText,
            runID: runId.isEmpty ? nil : runId,
            progressPercent: nil,
            step: "",
            etaSeconds: nil,
            details: "",
            schemaVersion: response.automationSchemaVersion.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private func resultLooksLikeTaskCreation(_ response: AgentChatResponse) -> Bool {
        if response.results.contains(where: { $0.type.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == "task" }) {
            return true
        }
        let workflowName = response.workflowName.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let automationMessage = response.automationMessage.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return workflowName.contains("task") || automationMessage.contains("task")
    }

    private func resultLooksLikeNoteCreation(_ response: AgentChatResponse) -> Bool {
        if response.results.contains(where: { $0.type.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == "note" }) {
            return true
        }
        let workflowName = response.workflowName.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let automationMessage = response.automationMessage.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return workflowName.contains("note") || automationMessage.contains("note")
    }

    private func resultLooksLikeReminderCreation(_ response: AgentChatResponse) -> Bool {
        if response.results.contains(where: { $0.type.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == "reminder" }) {
            return true
        }
        let workflowName = response.workflowName.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let automationMessage = response.automationMessage.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return workflowName.contains("reminder") || automationMessage.contains("erinner")
    }

    private func createdAutomationSummary(from response: AgentChatResponse) -> String {
        let reminderCount = response.results.filter {
            $0.type.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == "reminder"
        }.count
        let taskCount = response.results.filter {
            $0.type.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == "task"
        }.count
        let noteCount = response.results.filter {
            $0.type.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == "note"
        }.count
        let resolvedReminderCount = max(reminderCount, resultLooksLikeReminderCreation(response) ? 1 : 0)
        let resolvedTaskCount = max(taskCount, resultLooksLikeTaskCreation(response) ? 1 : 0)
        let resolvedNoteCount = max(noteCount, resultLooksLikeNoteCreation(response) ? 1 : 0)
        if resolvedReminderCount == 0 && resolvedTaskCount == 0 && resolvedNoteCount == 0 {
            return "- Keine neuen Reminder, Tasks oder Notizen erkannt."
        }
        var lines: [String] = []
        if resolvedReminderCount > 0 { lines.append("- Reminder \(resolvedReminderCount)") }
        if resolvedTaskCount > 0 { lines.append("- Tasks \(resolvedTaskCount)") }
        if resolvedNoteCount > 0 { lines.append("- Notizen \(resolvedNoteCount)") }
        return lines.joined(separator: "\n")
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

    var shouldShowSocialSetupCard: Bool {
        selectedMode == .automation
    }

    /// Entfernt fuehrende @ fuer API/Backend; Nutzer:innen duerfen trotzdem `@handle` tippen.
    static func normalizeSocialHandleForOutbound(_ raw: String) -> String {
        var s = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        while s.hasPrefix("@") {
            s.removeFirst()
        }
        return s.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func resolveSocialSetupForOutgoing(mode: String) -> AgentSocialSetupInput {
        let normalizedMode = mode.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard normalizedMode == "automation" else {
            return .empty
        }
        return AgentSocialSetupInput(
            instagramEnabled: socialInstagramEnabled,
            instagramHandle: Self.normalizeSocialHandleForOutbound(socialInstagramHandle),
            tiktokEnabled: socialTiktokEnabled,
            tiktokHandle: Self.normalizeSocialHandleForOutbound(socialTiktokHandle),
            youtubeEnabled: socialYoutubeEnabled,
            youtubeHandle: Self.normalizeSocialHandleForOutbound(socialYoutubeHandle),
            facebookEnabled: socialFacebookEnabled,
            facebookHandle: Self.normalizeSocialHandleForOutbound(socialFacebookHandle),
            spotifyEnabled: socialSpotifyEnabled,
            spotifyHandle: Self.normalizeSocialHandleForOutbound(socialSpotifyHandle)
        )
    }

    private func persistSocialSetupDraft() {
        guard !isRestoringSocialSetup else { return }
        pendingQueueStore.saveSocialSetup(
            AgentSocialSetupInput(
                instagramEnabled: socialInstagramEnabled,
                instagramHandle: socialInstagramHandle,
                tiktokEnabled: socialTiktokEnabled,
                tiktokHandle: socialTiktokHandle,
                youtubeEnabled: socialYoutubeEnabled,
                youtubeHandle: socialYoutubeHandle,
                facebookEnabled: socialFacebookEnabled,
                facebookHandle: socialFacebookHandle,
                spotifyEnabled: socialSpotifyEnabled,
                spotifyHandle: socialSpotifyHandle
            ),
            for: currentUserKey
        )
    }

    private func restoreSocialSetupDraft() {
        isRestoringSocialSetup = true
        let setup = pendingQueueStore.socialSetup(for: currentUserKey)
        socialInstagramEnabled = setup.instagramEnabled
        socialInstagramHandle = setup.instagramHandle
        socialTiktokEnabled = setup.tiktokEnabled
        socialTiktokHandle = setup.tiktokHandle
        socialYoutubeEnabled = setup.youtubeEnabled
        socialYoutubeHandle = setup.youtubeHandle
        socialFacebookEnabled = setup.facebookEnabled
        socialFacebookHandle = setup.facebookHandle
        socialSpotifyEnabled = setup.spotifyEnabled
        socialSpotifyHandle = setup.spotifyHandle
        isRestoringSocialSetup = false
    }

    func resetSocialSetup() {
        isRestoringSocialSetup = true
        socialInstagramEnabled = false
        socialInstagramHandle = ""
        socialTiktokEnabled = false
        socialTiktokHandle = ""
        socialYoutubeEnabled = false
        socialYoutubeHandle = ""
        socialFacebookEnabled = false
        socialFacebookHandle = ""
        socialSpotifyEnabled = false
        socialSpotifyHandle = ""
        isRestoringSocialSetup = false
        persistSocialSetupDraft()
    }

    private func memoryEnrichedPrompt(from userPrompt: String) async -> String {
        let prompt = userPrompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !prompt.isEmpty else { return userPrompt }

        let taskLines = TaskStore.shared.tasks
            .filter { $0.status == .open }
            .prefix(5)
            .map { task -> String in
                if let due = task.dueAt {
                    return "- \(task.title) (due \(ISO8601DateFormatter().string(from: due)))"
                }
                return "- \(task.title)"
            }

        let noteLines = NoteStore.shared.notes
            .prefix(5)
            .map { note in
                let title = note.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ? String(note.content.prefix(40))
                    : note.title
                return "- \(title)"
            }

        let reminderLines = await loadUpcomingReminderLines(limit: 5)

        guard !taskLines.isEmpty || !noteLines.isEmpty || !reminderLines.isEmpty else {
            return userPrompt
        }

        let memoryBlock = """
        [SkyOS Memory Context]
        Open tasks:
        \(taskLines.isEmpty ? "- none" : taskLines.joined(separator: "\n"))
        Recent notes:
        \(noteLines.isEmpty ? "- none" : noteLines.joined(separator: "\n"))
        Upcoming reminders:
        \(reminderLines.isEmpty ? "- none" : reminderLines.joined(separator: "\n"))
        Execution policy:
        - Reminder: nur fuer zeitgebundene Zusagen mit Datum/Uhrzeit.
        - Task: fuer konkrete umsetzbare Arbeit ohne feste Erinnerungszeit.
        - Note: fuer Referenzwissen ohne direkte Aktion.
        - Bei aehnlichen offenen Eintraegen zuerst aktualisieren statt duplizieren.
        [/SkyOS Memory Context]
        """

        return "\(memoryBlock)\n\nUser request:\n\(userPrompt)"
    }

    private func loadUpcomingReminderLines(limit: Int) async -> [String] {
        guard let uid = Auth.auth().currentUser?.uid, !uid.isEmpty else { return [] }
        let now = Date()
        guard let snapshot = try? await firestore
            .collection("users")
            .document(uid)
            .collection("reminders")
            .limit(to: 80)
            .getDocuments() else {
            return []
        }

        let items: [(String, Date?)] = snapshot.documents.compactMap { document in
            let data = document.data()
            let title = ((data["title"] as? String) ?? (data["text"] as? String) ?? (data["message"] as? String) ?? "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            guard !title.isEmpty else { return nil }
            let date = Self.resolveReminderDate(
                data["dueAt"],
                data["scheduledAt"],
                data["scheduledFor"],
                data["remindAt"],
                data["triggerAt"],
                data["date"]
            )
            guard let date, date >= now else { return nil }
            return (title, date)
        }
            .sorted { ($0.1 ?? .distantFuture) < ($1.1 ?? .distantFuture) }
            .prefix(limit)
            .map { $0 }

        let formatter = ISO8601DateFormatter()
        return items.map { title, date in
            if let date {
                return "- \(title) (\(formatter.string(from: date)))"
            }
            return "- \(title)"
        }
    }

    private static func resolveReminderDate(_ candidates: Any?...) -> Date? {
        for candidate in candidates {
            switch candidate {
            case let timestamp as Timestamp:
                return timestamp.dateValue()
            case let date as Date:
                return date
            case let number as NSNumber:
                let value = number.doubleValue
                return Date(timeIntervalSince1970: value > 10_000_000_000 ? value / 1000 : value)
            case let seconds as TimeInterval:
                return Date(timeIntervalSince1970: seconds > 10_000_000_000 ? seconds / 1000 : seconds)
            default:
                continue
            }
        }
        return nil
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

    private func startWorkflowStatusPollingIfNeeded(
        response: AgentChatResponse,
        assistantMessageID: UUID,
        requestContext: InFlightRequestContext
    ) {
        let runID = response.agentRunId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !runID.isEmpty else { return }
        let workflowResult = response.results.first(where: { $0.type == "workflow" })
        let initialStatus = workflowResult?.status.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
        guard initialStatus == "queued" || initialStatus == "running" else { return }

        workflowStatusTask?.cancel()
        workflowStatusTask = Task { @MainActor [weak self] in
            guard let self else { return }
            for attempt in 0..<12 {
                if Task.isCancelled { return }
                let waitSeconds: UInt64 = attempt < 4 ? 3 : 6
                try? await Task.sleep(nanoseconds: waitSeconds * 1_000_000_000)
                if Task.isCancelled { return }
                guard isConversationSnapshotValid(
                    conversationRevision: requestContext.conversationRevision,
                    userKeyAtSend: requestContext.userKeyAtSend,
                    sessionIDAtSend: requestContext.sessionIDAtSend
                ) else { return }
                guard NetworkStatusMonitor.shared.isOnline else { continue }
                guard let status = try? await service.fetchRunStatus(runId: runID) else { continue }
                let normalized = status.state.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
                let summaryText = status.automationMessage.trimmingCharacters(in: .whitespacesAndNewlines)
                let resolvedSummary = summaryText.isEmpty ? (
                    normalized == "running" ? "Workflow wird gerade ausgefuehrt." :
                    (normalized == "queued" ? "Workflow wurde in die Warteschlange gestellt." :
                    (normalized == "completed" ? "Workflow abgeschlossen." : "Workflow fehlgeschlagen."))
                ) : summaryText
                updateWorkflowSummary(
                    messageID: assistantMessageID,
                    workflowName: status.workflowName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? response.workflowName : status.workflowName,
                    statusText: resolvedSummary,
                    progressPercent: status.progressPercent,
                    step: status.step,
                    etaSeconds: status.etaSeconds > 0 ? status.etaSeconds : nil,
                    details: status.details,
                    schemaVersion: status.automationSchemaVersion
                )
                if normalized == "completed" || normalized == "failed" {
                    return
                }
            }
        }
    }

    private func updateWorkflowSummary(
        messageID: UUID,
        workflowName: String,
        statusText: String,
        progressPercent: Int?,
        step: String,
        etaSeconds: Int?,
        details: String,
        schemaVersion: String
    ) {
        guard let index = messages.firstIndex(where: { $0.id == messageID }) else { return }
        let existing = messages[index].workflowSummary
        messages[index].workflowSummary = AgentWorkflowSummary(
            workflowName: workflowName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?
                (existing?.workflowName ?? "External Workflow") :
                workflowName,
            statusText: statusText,
            runID: existing?.runID,
            progressPercent: progressPercent ?? existing?.progressPercent,
            step: step.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? (existing?.step ?? "") : step,
            etaSeconds: etaSeconds ?? existing?.etaSeconds,
            details: details.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? (existing?.details ?? "") : details,
            schemaVersion: schemaVersion.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? (existing?.schemaVersion ?? "") : schemaVersion
        )
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
                return AIExperienceLevel.resourceExhaustedUserMessage(from: error)
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

private func resolvedAgentExperienceLevel(for quotaPlan: UserQuotaPlan) -> AIExperienceLevel {
    switch quotaPlan {
    case .creator, .studio, .internalTeam, .ownerUnlimited:
        return .pro
    case .free:
        return .standard
    }
}

private extension AgentResultEntry {
    var historyResultEntry: AIScriptHistoryResultEntry {
        AIScriptHistoryResultEntry(
            type: type,
            text: text,
            url: url,
            title: title,
            mimeType: mimeType,
            fileName: fileName,
            html: html,
            columns: columns,
            rows: rows,
            workflowName: workflowName,
            status: status,
            summary: summary,
            runID: runId
        )
    }
}
