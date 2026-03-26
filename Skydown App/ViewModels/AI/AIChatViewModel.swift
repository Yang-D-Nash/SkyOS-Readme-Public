import Foundation
import FirebaseAI

enum AIChatRole {
    case user
    case assistant
}

struct AIChatMessage: Identifiable, Equatable {
    let id: UUID
    let role: AIChatRole
    var text: String
    var isStreaming: Bool

    init(
        id: UUID = UUID(),
        role: AIChatRole,
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
final class AIChatViewModel: ObservableObject {
    @Published var messages: [AIChatMessage] = [
        AIChatMessage(
            role: .assistant,
            text: "Ich bin der Skydown x 22 Bot. Frag mich nach Captions, Release-Ideen, Merch-Texten oder kurzen Kampagnenkonzepten."
        )
    ]
    @Published var draft = ""
    @Published var isSending = false
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info

    let quickPrompts = [
        "Schreib eine Instagram Caption fuer einen neuen Skydown x 22 Release.",
        "Gib mir drei Merch-Ideen fuer den naechsten Skydown x 22 Drop.",
        "Formuliere eine kurze Presseankuendigung fuer einen Skydown x 22 Artist-Launch.",
        "Brainstorme eine Hook fuer einen Skydown x 22 Song-Teaser auf TikTok."
    ]

    private let service: AIChatServicing
    private var chat: Chat?

    init(service: AIChatServicing = FirebaseAIChatService()) {
        self.service = service
    }

    func sendDraft() {
        sendPrompt(draft)
    }

    func sendPrompt(_ prompt: String) {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !isSending else { return }

        let assistantID = UUID()
        messages.append(AIChatMessage(role: .user, text: trimmedPrompt))
        messages.append(AIChatMessage(id: assistantID, role: .assistant, text: "", isStreaming: true))
        draft = ""
        isSending = true

        var responseBuffer = ""
        Task {
            do {
                let chat = activeChat()
                let responseStream = try chat.sendMessageStream(buildPrompt(for: trimmedPrompt))

                for try await chunk in responseStream {
                    guard let text = chunk.text, !text.isEmpty else { continue }
                    responseBuffer += text
                    updateAssistantMessage(
                        id: assistantID,
                        text: responseBuffer,
                        isStreaming: true
                    )
                }

                updateAssistantMessage(
                    id: assistantID,
                    text: responseBuffer.isEmpty
                        ? "Ich habe gerade keine Antwort erhalten. Versuch es bitte noch einmal."
                        : responseBuffer,
                    isStreaming: false
                )
                isSending = false
            } catch {
                let partialText = responseBuffer.trimmingCharacters(in: .whitespacesAndNewlines)
                let assistantText = assistantMessageText(
                    for: error,
                    partialText: partialText
                )
                updateAssistantMessage(
                    id: assistantID,
                    text: assistantText,
                    isStreaming: false
                )
                isSending = false
                showUserToast(userFacingErrorMessage(for: error), style: .error)
            }
        }
    }

    func resetConversation() {
        chat = nil
        messages = [
            AIChatMessage(
                role: .assistant,
                text: "Ich bin der Skydown x 22 Bot. Frag mich nach Captions, Release-Ideen, Merch-Texten oder kurzen Kampagnenkonzepten."
            )
        ]
    }

    private func activeChat() -> Chat {
        if let chat {
            return chat
        }

        let newChat = service.makeChat()
        chat = newChat
        return newChat
    }

    private func updateAssistantMessage(id: UUID, text: String, isStreaming: Bool) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].text = text
        messages[index].isStreaming = isStreaming
    }

    // Keep responses concise, creative and grounded in the Skydown app context.
    private func buildPrompt(for userPrompt: String) -> String {
        """
        Du bist der Skydown x 22 Bot, der kreative Assistent fuer die Skydown x 22 App.
        Antworte auf Deutsch, direkt, modern und hilfreich.
        Wenn sinnvoll, liefere kompakte Listen, Hooks, Captions oder kurze Konzepte.
        Bleib markentauglich und kreativ statt generisch.

        Nutzeranfrage:
        \(userPrompt)
        """
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private func assistantMessageText(for error: Error, partialText: String) -> String {
        if !partialText.isEmpty {
            return "\(partialText)\n\n\(userFacingErrorMessage(for: error))"
        }

        if case let GenerateContentError.promptBlocked(response) = error {
            return response.promptFeedback?.blockReasonMessage
                ?? "Die Anfrage konnte so nicht verarbeitet werden. Versuch es etwas neutraler oder konkreter."
        }

        return userFacingErrorMessage(for: error)
    }

    private func userFacingErrorMessage(for error: Error) -> String {
        if case let GenerateContentError.responseStoppedEarly(reason, _) = error {
            return finishReasonMessage(for: reason)
        }

        if case let GenerateContentError.promptBlocked(response) = error {
            return response.promptFeedback?.blockReasonMessage
                ?? "Die Anfrage konnte so nicht verarbeitet werden."
        }

        if case GenerateContentError.internalError = error {
            return "Der Skydown x 22 Bot ist gerade nicht verfuegbar."
        }

        return "Der Skydown x 22 Bot ist gerade nicht verfuegbar."
    }

    private func finishReasonMessage(for reason: FinishReason) -> String {
        switch reason {
        case .maxTokens:
            return "Die Antwort wurde wegen des Antwortlimits gekuerzt."
        case .safety, .prohibitedContent, .blocklist, .spii:
            return "Die Antwort wurde aus Sicherheitsgruenden gestoppt."
        case .recitation:
            return "Die Antwort wurde wegen Zitat-Schutz gestoppt."
        case .malformedFunctionCall:
            return "Die Antwort konnte nicht sauber abgeschlossen werden. Versuch es bitte noch einmal."
        default:
            return "Die Antwort wurde vorzeitig beendet."
        }
    }
}
