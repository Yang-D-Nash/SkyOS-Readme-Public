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

    var quickPrompts: [String] {
        selectedMode.quickPrompts
    }

    private let service: AgentChatServicing
    private let historyStore: AIScriptHistoryStore
    private let pendingQueueStore: AgentPendingQueueStore
    private var currentUserKey: String?
    private var pendingRequests: [PendingAgentRequest] = []
    private var networkObserver: AnyCancellable?
    private var currentPlanTitle: String = "Free"

    private struct PendingAgentRequest {
        let prompt: String
        let history: [AgentHistoryTurn]
        let mode: String
        let executeAutomation: Bool
        let assistantMessageID: UUID
        let createdAt: Date
    }

    init(
        service: AgentChatServicing = FirebaseFunctionsAgentService()
    ) {
        self.service = service
        self.historyStore = AIScriptHistoryStore.shared
        self.pendingQueueStore = AgentPendingQueueStore.shared
        networkObserver = NetworkStatusMonitor.shared.$isOnline
            .removeDuplicates()
            .sink { [weak self] isOnline in
                guard isOnline else { return }
                Task { @MainActor in
                    await self?.retryPendingRequestsIfNeeded()
                }
            }
    }

    func configureUser(user: User?) {
        let normalizedUserKey = normalizedUserKey(for: user?.id ?? user?.email)
        if let user {
            switch user.resolvedQuotaPlan {
            case .studio: currentPlanTitle = "Creator"
            case .creator: currentPlanTitle = "Pro"
            default: currentPlanTitle = "Free"
            }
        } else {
            currentPlanTitle = "Free"
        }
        historyStore.updateRetentionDays(user?.resolvedAIHistoryRetentionDays ?? UserRole.user.defaultAIHistoryRetentionDays)
        canTriggerAutomation = user != nil
        ManusBYOSStore.shared.setUserMode(userID: user?.id)
        if !canTriggerAutomation {
            shouldTriggerAutomation = false
        }
        guard normalizedUserKey != currentUserKey else { return }
        currentUserKey = normalizedUserKey
        restoreHistory()
    }

    func sendDraft() {
        sendPrompt(draft)
    }

    func sendPrompt(_ prompt: String) {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !phase.shouldBlockSend else { return }
        guard NetworkStatusMonitor.shared.isOnline else {
            queuePromptForRetry(trimmedPrompt)
            return
        }

        phase = .processing

        var appendedAssistantID: UUID?
        let modeAtSend = selectedMode.rawValue
        let executeAutomationAtSend = shouldTriggerAutomation && canTriggerAutomation
        var historyAtSend: [AgentHistoryTurn] = []
        Task {
            do {
                let assistantID = UUID()
                appendedAssistantID = assistantID
                let userMessage = AgentChatMessage(role: .user, text: trimmedPrompt)
                let history = buildHistory(from: messages + [userMessage])
                historyAtSend = history

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

                let result = try await service.sendMessage(
                    prompt: trimmedPrompt,
                    history: history,
                    mode: modeAtSend,
                    executeAutomation: executeAutomationAtSend,
                    manusApiKeyOverride: ManusBYOSStore.shared.currentAPIKeyOrNil()
                )
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
                historyStore.saveEntry(
                    userKey: currentUserKey,
                    source: .agent,
                    prompt: trimmedPrompt,
                    response: replyText
                )
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
                phase = pendingRequests.isEmpty ? .idle : .waitingReconnect
            } catch {
                if let assistantID = appendedAssistantID, isOfflineError(error) {
                    pendingRequests.append(
                        PendingAgentRequest(
                            prompt: trimmedPrompt,
                            history: historyAtSend,
                            mode: modeAtSend,
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
                    return
                }

                let message = userFacingErrorMessage(for: error)
                lastIntegrationIssue = message
                if let assistantID = appendedAssistantID {
                    updateAssistantMessage(
                        id: assistantID,
                        text: message,
                        isStreaming: false,
                        resultType: .error
                    )
                    historyStore.saveEntry(
                        userKey: currentUserKey,
                        source: .agent,
                        prompt: trimmedPrompt,
                        response: message
                    )
                }
                showUserToast(message, style: .error)
                phase = .idle
            }
        }
    }

    func resetConversation() {
        historyStore.clearEntries(userKey: currentUserKey, source: .agent)
        pendingRequests.removeAll()
        pendingQueueStore.clearEntries(for: currentUserKey)
        messages = []
        phase = .idle
        lastAgentRunId = ""
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

    private func restoreHistory() {
        let restoredEntries = historyStore.entries(for: currentUserKey, source: .agent).reversed()
        let restoredHistoryMessages = restoredEntries.flatMap { entry in
            [
                AgentChatMessage(role: .user, text: entry.prompt),
                AgentChatMessage(role: .assistant, text: entry.response)
            ]
        }

        pendingRequests = pendingQueueStore.entries(for: currentUserKey).compactMap { entry in
            guard let assistantID = UUID(uuidString: entry.assistantMessageID) else { return nil }
            return PendingAgentRequest(
                prompt: entry.prompt,
                history: entry.history.map { turn in
                    AgentHistoryTurn(role: turn.role, text: turn.text)
                },
                mode: entry.mode,
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

    private func queuePromptForRetry(_ trimmedPrompt: String) {
        let assistantID = UUID()
        let userMessage = AgentChatMessage(role: .user, text: trimmedPrompt)
        let history = buildHistory(from: messages + [userMessage])
        let modeAtSend = selectedMode.rawValue
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

    private func retryPendingRequestsIfNeeded() async {
        guard phase != .processing, !pendingRequests.isEmpty else { return }
        phase = .processing
        defer {
            phase = pendingRequests.isEmpty ? .idle : .waitingReconnect
        }

        while NetworkStatusMonitor.shared.isOnline, !pendingRequests.isEmpty {
            let request = pendingRequests.removeFirst()
            do {
                let result = try await service.sendMessage(
                    prompt: request.prompt,
                    history: request.history,
                    mode: request.mode,
                    executeAutomation: request.executeAutomation,
                    manusApiKeyOverride: ManusBYOSStore.shared.currentAPIKeyOrNil()
                )
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
                historyStore.saveEntry(
                    userKey: currentUserKey,
                    source: .agent,
                    prompt: request.prompt,
                    response: replyText
                )
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
                persistPendingRequests()
            } catch {
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
                historyStore.saveEntry(
                    userKey: currentUserKey,
                    source: .agent,
                    prompt: request.prompt,
                    response: message
                )
                showUserToast(message, style: .error)
                persistPendingRequests()
            }
        }
    }

    private func persistPendingRequests() {
        let normalizedUserKey = normalizedUserKey(for: currentUserKey)
        let fallbackUserKey = normalizedUserKey ?? "guest"
        let entries = pendingRequests.map { request in
            AgentPendingQueueEntry(
                userKey: fallbackUserKey,
                prompt: request.prompt,
                history: request.history.map { turn in
                    AgentPendingQueueTurn(role: turn.role, text: turn.text)
                },
                mode: request.mode,
                executeAutomation: request.executeAutomation,
                assistantMessageID: request.assistantMessageID.uuidString,
                createdAt: request.createdAt
            )
        }
        pendingQueueStore.saveEntries(entries, for: normalizedUserKey)
    }

    private func augmentedReplyText(from response: AgentChatResponse) -> String {
        guard response.automationTriggered else { return response.reply }
        let workflowLabel = response.workflowName.trimmingCharacters(in: .whitespacesAndNewlines)
        let automationLabel = workflowLabel.isEmpty ? "n8n" : workflowLabel
        let message = response.automationMessage.trimmingCharacters(in: .whitespacesAndNewlines)
        let suffix = message.isEmpty ? "An \(automationLabel) uebergeben." : message
        return "\(response.reply)\n\nn8n:\n\(suffix)"
    }

    private func buildWorkflowSummary(from response: AgentChatResponse) -> AgentWorkflowSummary? {
        let structuredWorkflow = response.results.first(where: { $0.type == "workflow" })
        guard response.automationTriggered || response.automationAttempted || structuredWorkflow != nil else { return nil }
        let workflowLabel = response.workflowName.trimmingCharacters(in: .whitespacesAndNewlines)
        let resolvedWorkflowLabel = (structuredWorkflow?.workflowName ?? workflowLabel)
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .isEmpty ? "n8n Workflow" : (structuredWorkflow?.workflowName ?? workflowLabel)
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
                return "Dein heutiges Agent-Limit ist erreicht. Bitte spaeter erneut versuchen."
            case .invalidArgument:
                return "Die Anfrage braucht noch etwas mehr Kontext."
            case .failedPrecondition:
                if nsError.localizedDescription.localizedCaseInsensitiveContains("App Check") {
                    return "Der Sicherheitscheck laeuft noch. Bitte kurz erneut versuchen."
                }
                return "Der Agent ist gerade noch nicht voll bereit. Bitte in einem Moment erneut versuchen."
            case .permissionDenied:
                return "Der Agent ist fuer dein Konto gerade nicht freigeschaltet."
            case .unauthenticated:
                return "Bitte melde dich erneut an und versuch es noch einmal."
            default:
                break
            }
        }

        return "Der SkyOS Agent ist gerade kurz pausiert. Bitte in einem Moment erneut versuchen."
    }
}
