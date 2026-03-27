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
    static let introMessage = "Ich bin der Skydown x 22 Agent. Ich baue dir Briefings, Release-Plaene, Content-Strukturen, Checklisten und naechste Schritte. Fuer schnelle Hooks oder Captions nimm den Bot."

    @Published var messages: [AgentChatMessage] = [
        AgentChatMessage(
            role: .assistant,
            text: introMessage
        )
    ]
    @Published var draft = ""
    @Published var isSending = false
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info

    let quickPrompts = [
        "Baue mir einen 7-Tage-Release-Plan mit Assets, Deadlines und Ownern.",
        "Mach ein Video-Briefing mit Ziel, Shotlist, Deliverables und Risiken.",
        "Strukturiere einen Merch-Drop in To-dos, Reihenfolge und Checkliste.",
        "Erstelle einen Content-Plan fuer TikTok, Reels und Story mit Timing."
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
                text: Self.introMessage
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
                return "Der Skydown x 22 Agent ist fuer diesen Bereich gerade noch nicht verfuegbar."
            case .unavailable:
                return "Der Skydown x 22 Agent ist gerade nicht erreichbar."
            case .deadlineExceeded:
                return "Der Skydown x 22 Agent hat zu lange fuer die Antwort gebraucht."
            case .resourceExhausted:
                return "Der Skydown x 22 Agent ist gerade ausgelastet."
            case .invalidArgument:
                return "Die Anfrage konnte so nicht verarbeitet werden."
            case .unauthenticated:
                return "Bitte melde dich erneut an und versuch es noch einmal."
            default:
                break
            }
        }

        return "Der Skydown x 22 Agent konnte gerade nicht antworten."
    }
}

private extension Array where Element == AgentChatMessage {
    func filterNotIntroMessage() -> [AgentChatMessage] {
        filter { message in
            !(message.role == .assistant &&
              message.text == AgentChatViewModel.introMessage)
        }
    }
}
