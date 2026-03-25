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
            text: "Ich bin Skydown AI. Frag mich nach Captions, Release-Ideen, Merch-Texten oder kurzen Kampagnenkonzepten."
        )
    ]
    @Published var draft = ""
    @Published var isSending = false
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info

    let quickPrompts = [
        "Schreib eine Instagram Caption fuer einen neuen Skydown Release.",
        "Gib mir drei Merch-Ideen fuer den naechsten Drop.",
        "Formuliere eine kurze Presseankuendigung fuer einen Artist-Launch.",
        "Brainstorme eine Hook fuer einen Song-Teaser auf TikTok."
    ]

    private let service: AIChatServicing
    private var chat: Chat

    init(service: AIChatServicing = FirebaseAIChatService()) {
        self.service = service
        self.chat = service.makeChat()
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
        chat = service.makeChat()
        messages = [
            AIChatMessage(
                role: .assistant,
                text: "Ich bin Skydown AI. Frag mich nach Captions, Release-Ideen, Merch-Texten oder kurzen Kampagnenkonzepten."
            )
        ]
    }

    private func updateAssistantMessage(id: UUID, text: String, isStreaming: Bool) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].text = text
        messages[index].isStreaming = isStreaming
    }

    // Keep responses concise, creative and grounded in the Skydown app context.
    private func buildPrompt(for userPrompt: String) -> String {
        """
        Du bist Skydown AI, der kreative Assistent fuer die Skydown App.
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
                ?? "Die Anfrage wurde von Firebase AI blockiert. Versuch bitte eine neutralere Formulierung."
        }

        return userFacingErrorMessage(for: error)
    }

    private func userFacingErrorMessage(for error: Error) -> String {
        if case let GenerateContentError.responseStoppedEarly(reason, _) = error {
            return finishReasonMessage(for: reason)
        }

        if case let GenerateContentError.promptBlocked(response) = error {
            return response.promptFeedback?.blockReasonMessage
                ?? "Die Anfrage wurde von Firebase AI blockiert."
        }

        if case let GenerateContentError.internalError(underlying) = error {
            return underlying.localizedDescription
        }

        return error.localizedDescription
    }

    private func finishReasonMessage(for reason: FinishReason) -> String {
        switch reason {
        case .maxTokens:
            return "Die Antwort wurde wegen des Antwortlimits gekuerzt."
        case .safety, .prohibitedContent, .blocklist, .spii:
            return "Firebase AI hat die Antwort aus Sicherheitsgruenden gestoppt."
        case .recitation:
            return "Firebase AI hat die Antwort wegen Zitat-Schutz gestoppt."
        case .malformedFunctionCall:
            return "Firebase AI hat eine unvollstaendige Tool-Antwort erzeugt."
        default:
            return "Firebase AI hat die Antwort vorzeitig beendet."
        }
    }
}
