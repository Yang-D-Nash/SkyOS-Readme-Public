import Foundation
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

    var quickPrompts: [String] {
        selectedMode.quickPrompts
    }

    private let service: AgentChatServicing
    private let historyStore: AIScriptHistoryStore
    private var currentUserKey: String?

    init(
        service: AgentChatServicing = FirebaseFunctionsAgentService()
    ) {
        self.service = service
        self.historyStore = AIScriptHistoryStore.shared
    }

    func configureUser(user: User?) {
        let normalizedUserKey = normalizedUserKey(for: user?.id ?? user?.email)
        historyStore.updateRetentionDays(user?.resolvedAIHistoryRetentionDays ?? UserRole.user.defaultAIHistoryRetentionDays)
        canTriggerAutomation = user != nil
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
        isSending = true

        var appendedAssistantID: UUID?
        Task {
            do {
                let assistantID = UUID()
                appendedAssistantID = assistantID
                let userMessage = AgentChatMessage(role: .user, text: trimmedPrompt)
                let history = buildHistory(from: messages + [userMessage])

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
                    mode: selectedMode.rawValue,
                    executeAutomation: shouldTriggerAutomation && canTriggerAutomation
                )
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
                    showUserToast(result.automationMessage.isEmpty ? "n8n-Workflow angestossen." : result.automationMessage, style: .success)
                } else if result.automationAttempted && !result.automationMessage.isEmpty {
                    showUserToast(result.automationMessage, style: .error)
                }
                isSending = false
            } catch {
                let message = userFacingErrorMessage(for: error)
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
        messages = restoredEntries.flatMap { entry in
            [
                AgentChatMessage(role: .user, text: entry.prompt),
                AgentChatMessage(role: .assistant, text: entry.response)
            ]
        }
    }

    private func normalizedUserKey(for userKey: String?) -> String? {
        let trimmed = userKey?.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed?.isEmpty == false ? trimmed : nil
    }

    private func updateAssistantMessage(id: UUID, text: String, isStreaming: Bool) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].text = text
        messages[index].isStreaming = isStreaming
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

    private func userFacingErrorMessage(for error: Error) -> String {
        let nsError = error as NSError

        if nsError.code == NSURLErrorNotConnectedToInternet || nsError.code == -1009 {
            return "Du bist offline. Der Agent wird wieder verfuegbar, sobald Internet da ist."
        }

        if nsError.domain == FunctionsErrorDomain,
           let code = FunctionsErrorCode(rawValue: nsError.code) {
            switch code {
            case .notFound, .unimplemented:
                return "Der Skydown x 22 Agent ist fuer diesen Bereich gerade noch nicht verfuegbar."
            case .unavailable:
                return "Der Skydown x 22 Agent ist gerade nicht erreichbar."
            case .deadlineExceeded:
                return "Der Skydown x 22 Agent hat zu lange fuer die Antwort gebraucht."
            case .resourceExhausted:
                return nsError.localizedDescription.isEmpty ? "Dein heutiges Agent-Limit ist erreicht." : nsError.localizedDescription
            case .invalidArgument:
                return "Die Anfrage konnte so nicht verarbeitet werden."
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

        return "Der Skydown x 22 Agent konnte gerade nicht antworten."
    }
}
