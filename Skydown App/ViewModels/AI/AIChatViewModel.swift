import Foundation
import FirebaseFunctions

enum AIChatRole {
    case user
    case assistant
}

enum AIComposerMode: String, CaseIterable, Identifiable {
    case text
    case visual

    var id: String { rawValue }

    var title: String {
        switch self {
        case .text:
            return "Text"
        case .visual:
            return "Visual"
        }
    }
}

struct AIVisualPrompt: Identifiable, Equatable {
    let id = UUID()
    let label: String
    let prompt: String
}

struct AIChatMessage: Identifiable, Equatable {
    let id: UUID
    let role: AIChatRole
    var text: String
    var isStreaming: Bool
    var imageData: Data?

    init(
        id: UUID = UUID(),
        role: AIChatRole,
        text: String,
        isStreaming: Bool = false,
        imageData: Data? = nil
    ) {
        self.id = id
        self.role = role
        self.text = text
        self.isStreaming = isStreaming
        self.imageData = imageData
    }
}

@MainActor
final class AIChatViewModel: ObservableObject {
    @Published var messages: [AIChatMessage] = []
    @Published var draft = ""
    @Published var composerMode: AIComposerMode = .text
    @Published var isSending = false
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info

    let quickPrompts = [
        "Schreib 3 Instagram Captions fuer einen neuen Track mit CTA und 5 Hashtags.",
        "Gib mir 5 kurze Hook-Ideen fuer einen dunklen Song-Teaser.",
        "Mach mir ein 15-Sekunden Reel-Skript mit Hook, Shots und On-Screen-Text.",
        "Formuliere einen Merch-Drop-Post mit Headline, Caption und Story-CTA."
    ]

    let visualPrompts = [
        AIVisualPrompt(
            label: "Cover Art",
            prompt: "Generiere ein quadratisches Cover-Art fuer einen dunklen Hip-Hop-Release von Skydown x 22 mit cineastischer Nachtstimmung und starkem Fokus auf Mood statt Schrift."
        ),
        AIVisualPrompt(
            label: "Release Poster",
            prompt: "Generiere ein vertikales Release-Poster fuer Skydown x 22, urban, premium, moody, mit Platz fuer einen kuenftigen Tracktitel."
        ),
        AIVisualPrompt(
            label: "Story Visual",
            prompt: "Generiere ein starkes 9:16 Story-Visual fuer einen neuen Skydown x 22 Drop, street, cinematic, klarer Fokus und wenig Text im Bild."
        )
    ]

    private let service: AIChatServicing
    private let historyStore: AIScriptHistoryStore
    private var currentUserKey: String?

    init(
        service: AIChatServicing = FirebaseFunctionsAIChatService()
    ) {
        self.service = service
        self.historyStore = AIScriptHistoryStore.shared
    }

    func configureUser(user: User?) {
        let normalizedUserKey = normalizedUserKey(for: user?.id ?? user?.email)
        historyStore.updateRetentionDays(user?.resolvedAIHistoryRetentionDays ?? UserRole.user.defaultAIHistoryRetentionDays)
        guard normalizedUserKey != currentUserKey else { return }
        currentUserKey = normalizedUserKey
        restoreHistory()
    }

    func sendDraft() {
        switch composerMode {
        case .text:
            sendPrompt(draft)
        case .visual:
            generateVisual(draft)
        }
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
                let history = buildHistoryContext()
                messages.append(AIChatMessage(role: .user, text: trimmedPrompt))
                messages.append(AIChatMessage(id: assistantID, role: .assistant, text: "", isStreaming: true))
                draft = ""

                let result = try await service.generateText(
                    prompt: buildPrompt(for: trimmedPrompt, history: history)
                )
                historyStore.updateRetentionDays(result.historyRetentionDays)

                updateAssistantMessage(
                    id: assistantID,
                    text: result.text,
                    isStreaming: false
                )
                historyStore.saveEntry(
                    userKey: currentUserKey,
                    source: .bot,
                    prompt: trimmedPrompt,
                    response: result.text
                )
                isSending = false
            } catch {
                isSending = false
                let assistantText = userFacingErrorMessage(for: error)
                if let assistantID = appendedAssistantID {
                    updateAssistantMessage(id: assistantID, text: assistantText, isStreaming: false)
                }
                historyStore.saveEntry(
                    userKey: currentUserKey,
                    source: .bot,
                    prompt: trimmedPrompt,
                    response: assistantText
                )
                showUserToast(userFacingErrorMessage(for: error), style: .error)
            }
        }
    }

    func generateVisual(_ prompt: String) {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !isSending else { return }
        isSending = true

        var appendedAssistantID: UUID?
        Task {
            do {
                let assistantID = UUID()
                appendedAssistantID = assistantID
                messages.append(AIChatMessage(role: .user, text: trimmedPrompt))
                messages.append(AIChatMessage(id: assistantID, role: .assistant, text: "", isStreaming: true))
                draft = ""

                let result = try await service.generateVisual(prompt: buildVisualPrompt(for: trimmedPrompt))
                historyStore.updateRetentionDays(result.historyRetentionDays)
                updateAssistantMessage(
                    id: assistantID,
                    text: result.text,
                    isStreaming: false,
                    imageData: result.imageData
                )
                historyStore.saveEntry(
                    userKey: currentUserKey,
                    source: .bot,
                    prompt: trimmedPrompt,
                    response: result.text
                )
                isSending = false
            } catch {
                isSending = false
                let assistantText = userFacingErrorMessage(for: error)
                if let assistantID = appendedAssistantID {
                    updateAssistantMessage(id: assistantID, text: assistantText, isStreaming: false)
                }
                historyStore.saveEntry(
                    userKey: currentUserKey,
                    source: .bot,
                    prompt: trimmedPrompt,
                    response: assistantText
                )
                showUserToast(userFacingErrorMessage(for: error), style: .error)
            }
        }
    }

    func resetConversation() {
        historyStore.clearEntries(userKey: currentUserKey, source: .bot)
        messages = []
    }

    private func updateAssistantMessage(id: UUID, text: String, isStreaming: Bool, imageData: Data? = nil) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].text = text
        messages[index].isStreaming = isStreaming
        messages[index].imageData = imageData ?? messages[index].imageData
    }

    private func buildPrompt(for userPrompt: String, history: String) -> String {
        let lowerPrompt = userPrompt.lowercased()
        let formatHint: String

        if ["caption", "captions", "instagram", "post", "story", "claim", "headline"].contains(where: { lowerPrompt.contains($0) }) {
            formatHint = "Liefere zuerst die beste Version, danach 3 weitere Varianten und am Ende optional 5 passende Hashtags."
        } else if ["hook", "hooks", "teaser", "intro"].contains(where: { lowerPrompt.contains($0) }) {
            formatHint = "Liefere 5 kurze Hook-Optionen mit maximal 10 Woertern pro Option."
        } else if ["reel", "tiktok", "skript", "script", "video"].contains(where: { lowerPrompt.contains($0) }) {
            formatHint = "Liefere die Antwort als Hook, Ablauf in 3 bis 5 Beats, On-Screen-Text und Caption."
        } else if ["merch", "drop"].contains(where: { lowerPrompt.contains($0) }) {
            formatHint = "Liefere die Antwort als Headline, Hauptcaption, Story-CTA und 3 kurze Zusatzvarianten."
        } else {
            formatHint = "Liefere eine direkt nutzbare Hauptantwort und wenn passend 3 starke Varianten."
        }

        return """
        Du bist der Skydown x 22 Bot, der kreative Copy- und Content-Assistent fuer Skydown Entertainment.
        Markenkontext:
        - Skydown Entertainment kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
        - Die App verbindet Musik, Videos, Merch und Creator-Tools.
        - Yang D. Nash ist Kern der Marke und Entwickler der App.

        Antworte auf Deutsch.
        Sei direkt nutzbar, markentauglich, modern und nicht generisch.
        Keine langen Vorreden, keine Erklaerungen ueber deinen Prozess.
        Schreibe lieber Ergebnisse als Theorie.
        Wenn die Anfrage nach Caption, Hook, Claim, Reel oder Post klingt, liefere echte copy-pastebare Optionen.
        Wenn die Anfrage eher nach Planung, Freigaben, Briefing oder To-dos klingt, antworte kurz hilfreich, verweise aber auf den Agent fuer die tiefe Struktur.

        Bisheriger Verlauf:
        \(history)

        Ausgabeformat:
        \(formatHint)

        Nutzeranfrage:
        \(userPrompt)
        """
    }

    private func restoreHistory() {
        let restoredEntries = historyStore.entries(for: currentUserKey, source: .bot).reversed()
        messages = restoredEntries.flatMap { entry in
            [
                AIChatMessage(role: .user, text: entry.prompt),
                AIChatMessage(role: .assistant, text: entry.response)
            ]
        }
    }

    private func buildHistoryContext() -> String {
        let relevantMessages = messages.suffix(12)
        guard !relevantMessages.isEmpty else {
            return "Noch kein Verlauf."
        }

        return relevantMessages
            .map { message in
                let prefix = message.role == .user ? "Nutzer" : "Bot"
                return "\(prefix): \(message.text)"
            }
            .joined(separator: "\n")
    }

    private func normalizedUserKey(for userKey: String?) -> String? {
        let trimmed = userKey?.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed?.isEmpty == false ? trimmed : nil
    }

    private func buildVisualPrompt(for userPrompt: String) -> String {
        let referenceContext = AIVisualReferenceLibraryStore.promptContext()

        return """
        Du bist der Skydown x 22 Bot und generierst genau ein starkes Key-Visual fuer Skydown Entertainment.
        Markenkontext:
        - Skydown Entertainment kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
        - Die Marke lebt von Musik, Videos, Street-Culture und Premium-Underground-Aesthetik.
        - Yang D. Nash ist Kern der Marke und Entwickler der App.

        Erzeuge ein modernes, hochwertiges Visual mit klarer Stimmung.
        Stil: cinematic, urban, moody, premium, nicht kitschig, nicht generisch.
        Nutze nur sehr wenig Text im Bild. Wenn Text im Motiv vorkommt, dann maximal eine kurze Headline.
        Liefere neben dem Bild nur eine kurze Ein-Zeilen-Beschreibung des Looks.
        Antworte auf Deutsch.

        \(referenceContext ?? "")

        Nutzeranfrage:
        \(userPrompt)
        """
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
                return "Der Skydown x 22 Bot ist fuer diesen Bereich gerade noch nicht verfuegbar."
            case .unavailable:
                return "Der Skydown x 22 Bot ist gerade nicht erreichbar."
            case .deadlineExceeded:
                return "Der Skydown x 22 Bot hat zu lange fuer die Antwort gebraucht."
            case .resourceExhausted:
                return nsError.localizedDescription.isEmpty ? "Dein heutiges KI-Limit ist erreicht." : nsError.localizedDescription
            case .permissionDenied:
                return nsError.localizedDescription.isEmpty ? "Die KI ist fuer dein Konto gerade nicht freigeschaltet." : nsError.localizedDescription
            case .unauthenticated:
                return "Bitte melde dich erneut an und versuch es noch einmal."
            case .invalidArgument:
                return "Die KI-Anfrage konnte so nicht gestartet werden."
            default:
                break
            }
        }

        return error.localizedDescription.isEmpty
            ? "Der Skydown x 22 Bot ist gerade nicht verfuegbar."
            : error.localizedDescription
    }
}
