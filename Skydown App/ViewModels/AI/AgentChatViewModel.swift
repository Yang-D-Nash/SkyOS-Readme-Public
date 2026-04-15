import Foundation
import Combine
import FirebaseFunctions

enum AgentChatRole {
    case user
    case assistant
}

struct AgentChatMessage: Identifiable, Equatable {
    let id: UUID
    let role: AgentChatRole
    var text: String
    var isStreaming: Bool

    init(
        id: UUID = UUID(),
        role: AgentChatRole,
        text: String,
        isStreaming: Bool = false
    ) {
        self.id = id
        self.role = role
        self.text = text
        self.isStreaming = isStreaming
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
    @Published var messages: [AgentChatMessage] = []
    @Published var draft = ""
    @Published var selectedMode: AgentExecutionMode = .release
    @Published var shouldTriggerAutomation = false
    @Published private(set) var canTriggerAutomation = false
    @Published var isSending = false
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info
    @Published private(set) var lastAgentProvider: AIRuntimeAgentProvider = .gemini
    @Published private(set) var lastProviderNotice: String = ""
    @Published private(set) var lastIntegrationIssue: String = ""

    var quickPrompts: [String] {
        selectedMode.quickPrompts
    }

    private let service: AgentChatServicing
    private let historyStore: AIScriptHistoryStore
    private let pendingQueueStore: AgentPendingQueueStore
    private var currentUserKey: String?
    private var pendingRequests: [PendingAgentRequest] = []
    private var networkObserver: AnyCancellable?

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
        guard !trimmedPrompt.isEmpty, !isSending else { return }
        guard NetworkStatusMonitor.shared.isOnline else {
            queuePromptForRetry(trimmedPrompt)
            return
        }

        isSending = true

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
                        isStreaming: true
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
                historyStore.updateRetentionDays(result.historyRetentionDays)
                let replyText = augmentedReplyText(from: result)
                updateAssistantMessage(
                    id: assistantID,
                    text: replyText,
                    isStreaming: false
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
                            ? AppLocalized.text("agent.automation.triggered", fallback: "n8n workflow started.")
                            : result.automationMessage,
                        style: .success
                    )
                } else if result.automationAttempted && !result.automationMessage.isEmpty {
                    lastIntegrationIssue = result.automationMessage
                    showUserToast(result.automationMessage, style: .error)
                } else {
                    lastIntegrationIssue = ""
                }
                isSending = false
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
                            fallback: "Queued: this request will send automatically when your connection is back."
                        ),
                        isStreaming: false
                    )
                    isSending = false
                    showUserToast(
                        AppLocalized.text(
                            "agent.queue.toast.queued",
                            fallback: "Saved offline. Sending automatically once you are online."
                        ),
                        style: .info
                    )
                    lastIntegrationIssue = AppLocalized.text(
                        "agent.offline.message",
                        fallback: "You are offline. The agent will continue once your connection is back."
                    )
                    return
                }

                let message = userFacingErrorMessage(for: error)
                lastIntegrationIssue = message
                if let assistantID = appendedAssistantID {
                    updateAssistantMessage(
                        id: assistantID,
                        text: message,
                        isStreaming: false
                    )
                    historyStore.saveEntry(
                        userKey: currentUserKey,
                        source: .agent,
                        prompt: trimmedPrompt,
                        response: message
                    )
                }
                isSending = false
                showUserToast(message, style: .error)
            }
        }
    }

    func resetConversation() {
        historyStore.clearEntries(userKey: currentUserKey, source: .agent)
        pendingRequests.removeAll()
        pendingQueueStore.clearEntries(for: currentUserKey)
        messages = []
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
                        fallback: "Queued: this request will send automatically when your connection is back."
                    )
                )
            ]
        }

        messages = restoredHistoryMessages + pendingMessages
    }

    private func normalizedUserKey(for userKey: String?) -> String? {
        let trimmed = userKey?.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let trimmed, !trimmed.isEmpty else { return nil }
        return trimmed.lowercased()
    }

    private func updateAssistantMessage(id: UUID, text: String, isStreaming: Bool) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].text = text
        messages[index].isStreaming = isStreaming
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
                    fallback: "Queued: this request will send automatically when your connection is back."
                ),
                isStreaming: false
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
                fallback: "Saved offline. Sending automatically once you are online."
            ),
            style: .info
        )
        lastIntegrationIssue = AppLocalized.text(
            "agent.offline.message",
            fallback: "You are offline. The agent will continue once your connection is back."
        )
    }

    private func retryPendingRequestsIfNeeded() async {
        guard !isSending, !pendingRequests.isEmpty else { return }
        isSending = true
        defer { isSending = false }

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
                historyStore.updateRetentionDays(result.historyRetentionDays)
                let replyText = augmentedReplyText(from: result)
                updateAssistantMessage(
                    id: request.assistantMessageID,
                    text: replyText,
                    isStreaming: false
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
                            ? AppLocalized.text("agent.automation.triggered", fallback: "n8n workflow started.")
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
                            fallback: "Queued request sent successfully."
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
                            fallback: "Queued: this request will send automatically when your connection is back."
                        ),
                        isStreaming: false
                    )
                    break
                }

                let message = userFacingErrorMessage(for: error)
                lastIntegrationIssue = message
                updateAssistantMessage(
                    id: request.assistantMessageID,
                    text: message,
                    isStreaming: false
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

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private func updateProviderDiagnostics(from response: AgentChatResponse) {
        let resolvedProvider = AIRuntimeAgentProvider(
            rawValue: response.agentProvider
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased()
        ) ?? .gemini
        lastAgentProvider = resolvedProvider

        let trimmedNotice = response.providerNotice.trimmingCharacters(in: .whitespacesAndNewlines)
        if response.providerFallbackUsed || !trimmedNotice.isEmpty {
            lastProviderNotice = trimmedNotice.isEmpty
                ? "Provider-Fallback aktiv."
                : trimmedNotice
        } else {
            lastProviderNotice = ""
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
                fallback: "You are offline. The agent will continue once your connection is back."
            )
        }

        if nsError.domain == FunctionsErrorDomain,
           let code = FunctionsErrorCode(rawValue: nsError.code) {
            switch code {
            case .notFound, .unimplemented:
                return "Der 22xSky Agent ist fuer diesen Bereich gerade noch nicht verfuegbar."
            case .unavailable:
                return "Der 22xSky Agent ist gerade nicht erreichbar."
            case .deadlineExceeded:
                return "Der 22xSky Agent hat zu lange fuer die Antwort gebraucht."
            case .resourceExhausted:
                return nsError.localizedDescription.isEmpty ? "Dein heutiges Agent-Limit ist erreicht." : nsError.localizedDescription
            case .invalidArgument:
                return "Die Anfrage konnte so nicht verarbeitet werden."
            case .failedPrecondition:
                if nsError.localizedDescription.localizedCaseInsensitiveContains("App Check") {
                    return "Sicherheitscheck laeuft noch. Bitte die App kurz neu oeffnen und erneut versuchen."
                }
                return nsError.localizedDescription.isEmpty
                    ? "Der Agent ist noch nicht vollstaendig eingerichtet."
                    : nsError.localizedDescription
            case .permissionDenied:
                return nsError.localizedDescription.isEmpty ? "Der Agent ist fuer dein Konto gerade nicht freigeschaltet." : nsError.localizedDescription
            case .unauthenticated:
                return "Bitte melde dich erneut an und versuch es noch einmal."
            default:
                break
            }
        }

        if !nsError.localizedDescription.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return nsError.localizedDescription
        }

        return "Der 22xSky Agent konnte gerade nicht antworten."
    }
}
