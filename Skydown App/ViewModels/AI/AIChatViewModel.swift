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

enum AITextMode: String, CaseIterable, Identifiable {
    case general = "general"
    case caption = "caption"
    case releasePlan = "release_plan"
    case briefing = "briefing"
    case merchCopy = "merch_copy"
    case videoConcept = "video_concept"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .general:
            return "Allgemein"
        case .caption:
            return "Captions"
        case .releasePlan:
            return "Release"
        case .briefing:
            return "Briefing"
        case .merchCopy:
            return "Merch"
        case .videoConcept:
            return "Video"
        }
    }

    var placeholder: String {
        switch self {
        case .general:
            return "Zum Beispiel: Copy fuer den naechsten Drop."
        case .caption:
            return "Zum Beispiel: Caption fuer den neuen Track."
        case .releasePlan:
            return "Zum Beispiel: 7-Tage-Plan fuer den Release."
        case .briefing:
            return "Zum Beispiel: Briefing fuer Foto- oder Video-Team."
        case .merchCopy:
            return "Zum Beispiel: Copy fuer einen neuen Merch-Drop."
        case .videoConcept:
            return "Zum Beispiel: Konzept fuer Reel oder Musikvideo."
        }
    }

    var quickPrompts: [String] {
        switch self {
        case .general:
            return [
                "Schreib 3 starke Copy-Ideen fuer einen neuen Skydown-Post.",
                "Gib mir eine markentaugliche Ansage fuer einen Story-Slide.",
                "Mach mir eine klare Promo-Line fuer einen neuen Drop.",
                "Ueberarbeite diesen Text moderner und druckvoller."
            ]
        case .caption:
            return [
                "Schreib 3 Instagram Captions fuer einen neuen Track mit CTA und 5 Hashtags.",
                "Mach mir 4 Story-Captions fuer einen dunklen Teaser mit kurzer Hook.",
                "Schreib eine Caption fuer ein Studio-Snippet mit Hamburg-Vibe.",
                "Formuliere einen Release-Post, kurz, druckvoll und nicht generisch."
            ]
        case .releasePlan:
            return [
                "Baue mir einen 7-Tage-Release-Plan fuer einen neuen Track mit Assets und Deadlines.",
                "Strukturiere den Launch fuer Freitag von Teaser bis Post-Release.",
                "Mach einen Mini-Plan fuer Song, Story, Reel und CTA.",
                "Welche Assets brauche ich fuer einen sauberen Track-Release?"
            ]
        case .briefing:
            return [
                "Schreib ein Briefing fuer einen Fotografen mit Mood, Shots und Deliverables.",
                "Mach ein Briefing fuer einen Cover-Designer, urban und cinematic.",
                "Erstelle ein kompaktes Creator-Briefing fuer einen Collab-Post.",
                "Formuliere ein Team-Briefing fuer einen Promo-Dreh."
            ]
        case .merchCopy:
            return [
                "Formuliere einen Merch-Drop-Post mit Headline, Caption und Story-CTA.",
                "Schreib Copy fuer ein limitiertes Hoodie-Release.",
                "Gib mir 3 Shop-Claims fuer einen Premium-Streetwear-Drop.",
                "Mach eine kurze Produktbeschreibung fuer Shirt und Hoodie."
            ]
        case .videoConcept:
            return [
                "Mach mir ein 15-Sekunden Reel-Skript mit Hook, Shots und On-Screen-Text.",
                "Gib mir 5 kurze Hook-Ideen fuer einen dunklen Song-Teaser.",
                "Baue ein Storyboard fuer ein moody Performance-Visual.",
                "Mach ein Konzept fuer ein vertikales Promo-Video mit 3 Szenen."
            ]
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
    @Published var textMode: AITextMode = .general
    @Published var isSending = false
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info

    var quickPrompts: [String] {
        textMode.quickPrompts
    }

    let visualPrompts = [
        AIVisualPrompt(
            label: "Cover Art",
            prompt: "Generiere ein quadratisches Cover-Art fuer einen dunklen Hip-Hop-Release von 22xSky mit cineastischer Nachtstimmung und starkem Fokus auf Mood statt Schrift."
        ),
        AIVisualPrompt(
            label: "Release Poster",
            prompt: "Generiere ein vertikales Release-Poster fuer 22xSky, urban, premium, moody, mit Platz fuer einen kuenftigen Tracktitel."
        ),
        AIVisualPrompt(
            label: "Story Visual",
            prompt: "Generiere ein starkes 9:16 Story-Visual fuer einen neuen 22xSky Drop, street, cinematic, klarer Fokus und wenig Text im Bild."
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
                    prompt: buildPrompt(for: trimmedPrompt, history: history),
                    mode: textMode.rawValue
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
        return """
        Bisheriger Verlauf:
        \(history)

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
                return "Der 22xSky Bot ist fuer diesen Bereich gerade noch nicht verfuegbar."
            case .unavailable:
                return "Der 22xSky Bot ist gerade nicht erreichbar."
            case .deadlineExceeded:
                return "Der 22xSky Bot hat zu lange fuer die Antwort gebraucht."
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
            ? "Der 22xSky Bot ist gerade nicht verfuegbar."
            : error.localizedDescription
    }
}
