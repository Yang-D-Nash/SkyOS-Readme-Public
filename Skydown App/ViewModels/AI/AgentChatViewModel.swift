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

@MainActor
final class AgentChatViewModel: ObservableObject {
    @Published var messages: [AgentChatMessage] = [
        AgentChatMessage(
            role: .assistant,
            text: "Ich bin Skydown Agent. Ich helfe dir bei Briefings, Release-Planung, Freigaben und naechsten Schritten."
        )
    ]
    @Published var draft = ""
    @Published var isSending = false
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info

    let quickPrompts = [
        "Mach mir einen 7-Tage-Release-Plan fuer einen neuen Track.",
        "Baue mir ein kurzes Briefing fuer einen TikTok-Teaser.",
        "Strukturiere die naechsten Schritte fuer einen Merchandise-Drop.",
        "Erstell mir einen kleinen Launch-Plan mit Content, Timing und To-dos."
    ]

    private let service: AgentChatServicing

    init(service: AgentChatServicing = FirebaseFunctionsAgentService()) {
        self.service = service
    }

    func sendDraft() {
        sendPrompt(draft)
    }

    func sendPrompt(_ prompt: String) {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !isSending else { return }

        let assistantID = UUID()
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
        isSending = true

        Task {
            do {
                let reply = try await service.sendMessage(
                    prompt: trimmedPrompt,
                    history: history
                )
                updateAssistantMessage(
                    id: assistantID,
                    text: reply,
                    isStreaming: false
                )
                isSending = false
            } catch {
                let message = userFacingErrorMessage(for: error)
                updateAssistantMessage(
                    id: assistantID,
                    text: message,
                    isStreaming: false
                )
                isSending = false
                showUserToast(message, style: .error)
            }
        }
    }

    func resetConversation() {
        messages = [
            AgentChatMessage(
                role: .assistant,
                text: "Ich bin Skydown Agent. Ich helfe dir bei Briefings, Release-Planung, Freigaben und naechsten Schritten."
            )
        ]
    }

    private func buildHistory(from messages: [AgentChatMessage]) -> [AgentHistoryTurn] {
        messages
            .filterNotIntroMessage()
            .map { message in
                AgentHistoryTurn(
                    role: message.role == .user ? "user" : "assistant",
                    text: message.text
                )
            }
    }

    private func updateAssistantMessage(id: UUID, text: String, isStreaming: Bool) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].text = text
        messages[index].isStreaming = isStreaming
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private func userFacingErrorMessage(for error: Error) -> String {
        let nsError = error as NSError

        if nsError.domain == FunctionsErrorDomain,
           let code = FunctionsErrorCode(rawValue: nsError.code) {
            switch code {
            case .notFound, .unimplemented:
                return "Der Genkit-Agent ist im Firebase-Projekt noch nicht live."
            case .unavailable:
                return "Skydown Agent ist gerade nicht erreichbar."
            case .deadlineExceeded:
                return "Skydown Agent hat zu lange fuer die Antwort gebraucht."
            case .resourceExhausted:
                return "Skydown Agent ist gerade ausgelastet."
            case .invalidArgument:
                return "Die Agent-Anfrage war ungueltig."
            case .unauthenticated:
                return "Die Anfrage an den Agenten konnte nicht verifiziert werden."
            default:
                break
            }
        }

        return error.localizedDescription
    }
}

private extension Array where Element == AgentChatMessage {
    func filterNotIntroMessage() -> [AgentChatMessage] {
        filter { message in
            !(message.role == .assistant &&
              message.text == "Ich bin Skydown Agent. Ich helfe dir bei Briefings, Release-Planung, Freigaben und naechsten Schritten.")
        }
    }
}
