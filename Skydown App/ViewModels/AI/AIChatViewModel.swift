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
    case faq = "faq"
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
        case .faq:
            return "FAQ"
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
        case .faq:
            return "Zum Beispiel: Wie funktioniert Membership oder Restore?"
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
        case .faq:
            return [
                "Wie stelle ich mein Abo wieder her?",
                "Wo ist meine Bestellung und wie pruefe ich den Status sauber?",
                "Welche Membership habe ich gerade und was bringt ein Upgrade?",
                "Warum ist AI gesperrt und was kann ich konkret tun?"
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

enum AIExperienceLevel: String, CaseIterable, Identifiable {
    case standard = "standard"
    case advanced = "advanced"
    case pro = "pro"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .standard:
            return "Standard"
        case .advanced:
            return "Advanced"
        case .pro:
            return "Pro"
        }
    }

    var subtitle: String {
        switch self {
        case .standard:
            return AppLocalized.text("ai.level.standard.subtitle", fallback: "Für den Alltag")
        case .advanced:
            return AppLocalized.text("ai.level.advanced.subtitle", fallback: "Für komplexere Aufgaben")
        case .pro:
            return AppLocalized.text("ai.level.pro.subtitle", fallback: "Für tiefere Analysen")
        }
    }

    static var limitReachedMessage: String {
        AppLocalized.text(
            "ai.level.limit_reached",
            fallback: "Dein heutiges AI-Limit ist erreicht. Morgen kannst du weiterarbeiten oder einen höheren Modus nutzen, sobald er freigeschaltet ist."
        )
    }

    static var unavailableMessage: String {
        AppLocalized.text(
            "ai.level.unavailable",
            fallback: "Dieser Modus ist für deinen aktuellen Zugang noch nicht verfügbar."
        )
    }

    func isAvailable(for quotaPlan: UserQuotaPlan) -> Bool {
        switch self {
        case .standard, .advanced:
            return true
        case .pro:
            switch quotaPlan {
            case .creator, .studio, .internalTeam, .ownerUnlimited:
                return true
            case .free:
                return false
            }
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

    @Published var messages: [AIChatMessage] = []
    @Published var draft = "" {
        didSet {
            guard !phase.isBusy else { return }
            let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.isEmpty {
                if phase == .typing {
                    phase = .idle
                }
            } else {
                phase = .typing
            }
        }
    }
    @Published var composerMode: AIComposerMode = .text
    @Published var textMode: AITextMode = .general
    @Published var selectedLevel: AIExperienceLevel = .standard
    /// Bot-only lifecycle (text vs visual). Use `phase.isBusy` instead of a shared `isSending` flag.
    @Published private(set) var phase: BotInteractionPhase = .idle
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info
    @Published private(set) var revenueUsage: RevenueUsageState?
    @Published private(set) var lastDecision: AIBotDecision?
    @Published private(set) var sessions: [AIScriptHistorySessionSummary] = []
    @Published private(set) var activeSessionID: UUID?
    @Published private(set) var activeSessionTitle: String = AIScriptHistoryStore.defaultSessionTitle

    var quickPrompts: [String] {
        textMode.quickPrompts
    }

    let visualPrompts = [
        AIVisualPrompt(
            label: "Artist Foto",
            prompt: "Generiere ein cineastisches Artist-Foto fuer SkyOS wie ein hochwertiger ARRI-Frame: 35mm Prime, offene Blende um f/1.4, organisches Bokeh, natuerliche Tiefenstaffelung, moody Licht, realistisches Editorial-Foto, keine Illustration, kein CGI und kein generischer AI-Look."
        ),
        AIVisualPrompt(
            label: "Cover Art",
            prompt: "Generiere ein quadratisches Cover-Art fuer einen dunklen Hip-Hop-Release von SkyOS mit cineastischer Nachtstimmung und starkem Fokus auf Mood statt Schrift."
        ),
        AIVisualPrompt(
            label: "Release Poster",
            prompt: "Generiere ein vertikales Release-Poster fuer SkyOS, urban, premium, moody, mit Platz fuer einen kuenftigen Tracktitel."
        ),
        AIVisualPrompt(
            label: "Story Visual",
            prompt: "Generiere ein starkes 9:16 Story-Visual fuer einen neuen SkyOS Drop, street, cinematic, klarer Fokus und wenig Text im Bild."
        )
    ]

    private let service: AIChatServicing
    private let syncService: AIConversationSyncServicing
    private let historyStore: AIScriptHistoryStore
    private var currentUserID: String?
    private var currentUserKey: String?
    private var currentSessionID: UUID?
    private var currentPlanTitle: String = "Free"
    private var currentHistoryRetentionDays: Int = UserRole.user.defaultAIHistoryRetentionDays
    private var conversationRevision = 0
    private var activeRequestTask: Task<Void, Never>?
    private var remoteHydrationTask: Task<Void, Never>?
    private var stopRemoteHistoryObservation: (() -> Void)?
    private var activeRequestContext: InFlightRequestContext?
    private var currentQuotaPlan: UserQuotaPlan = .free

    private struct InFlightRequestContext: Equatable {
        let requestID: UUID
        let conversationRevision: Int
        let userKeyAtSend: String?
        let sessionIDAtSend: UUID?
        let aiLevelAtSend: AIExperienceLevel
    }

    init(
        service: AIChatServicing = FirebaseFunctionsAIChatService(),
        syncService: AIConversationSyncServicing = FirestoreAIConversationSyncService()
    ) {
        self.service = service
        self.syncService = syncService
        self.historyStore = AIScriptHistoryStore.shared
    }

    deinit {
        activeRequestTask?.cancel()
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
        if !selectedLevel.isAvailable(for: currentQuotaPlan) {
            selectedLevel = .standard
        }
        historyStore.updateRetentionDays(currentHistoryRetentionDays)
        let didChangeIdentity = normalizedUserKey != currentUserKey || normalizedUserID != currentUserID
        if !didChangeIdentity {
            pruneRemoteHistoryIfNeeded()
            return
        }
        invalidateConversation(cancelActiveRequest: true)
        stopRemoteHistoryObservation?()
        stopRemoteHistoryObservation = nil
        currentUserID = normalizedUserID
        currentUserKey = normalizedUserKey
        restoreConversationState()
        hydrateRemoteHistoryIfNeeded(preferredSessionID: currentSessionID)
        startRemoteHistoryObservation()
        if normalizedUserID != nil {
            startNewConversation()
        }
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
        guard !trimmedPrompt.isEmpty, !phase.isBusy else { return }
        guard selectedLevel.isAvailable(for: currentQuotaPlan) else {
            showUserToast(AIExperienceLevel.unavailableMessage, style: .info)
            return
        }
        let session = historyStore.ensureSession(
            userKey: currentUserKey,
            source: .bot,
            preferredSessionID: currentSessionID
        )
        currentSessionID = session.id
        refreshConversationMetadata(preferredSessionID: session.id)
        phase = .sending
        lastDecision = nil

        let assistantID = UUID()
        let history = buildHistoryContext()
        let levelAtSend = selectedLevel
        let requestContext = makeRequestContext()
        messages.append(AIChatMessage(role: .user, text: trimmedPrompt))
        messages.append(AIChatMessage(id: assistantID, role: .assistant, text: "", isStreaming: true))
        draft = ""
        phase = .streaming

        activeRequestContext = requestContext
        activeRequestTask?.cancel()
        activeRequestTask = Task { @MainActor [weak self] in
            guard let self else { return }
            do {
                let result = try await service.generateText(
                    prompt: buildPrompt(for: trimmedPrompt, history: history),
                    mode: textMode.rawValue,
                    aiLevel: levelAtSend.rawValue
                )
                guard isRequestContextValid(requestContext) else { return }
                historyStore.updateRetentionDays(result.historyRetentionDays)
                applyUsage(result.usage)
                lastDecision = result.decision

                updateAssistantMessage(
                    id: assistantID,
                    text: result.text,
                    isStreaming: false
                )
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .bot,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: trimmedPrompt,
                    response: result.text
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                phase = resolvedTerminalPhase(for: result.decision)
                clearActiveRequestIfNeeded(requestContext)
            } catch {
                if error is CancellationError || Task.isCancelled {
                    clearActiveRequestIfNeeded(requestContext)
                    return
                }
                guard isRequestContextValid(requestContext) else { return }
                let assistantText = userFacingErrorMessage(for: error)
                lastDecision = decision(for: error)
                phase = resolvedTerminalPhase(for: lastDecision)
                updateAssistantMessage(id: assistantID, text: assistantText, isStreaming: false)
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .bot,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: trimmedPrompt,
                    response: assistantText
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                showUserToast(userFacingErrorMessage(for: error), style: .error)
                clearActiveRequestIfNeeded(requestContext)
            }
        }
    }

    func generateVisual(_ prompt: String) {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !phase.isBusy else { return }
        guard selectedLevel.isAvailable(for: currentQuotaPlan) else {
            showUserToast(AIExperienceLevel.unavailableMessage, style: .info)
            return
        }
        let session = historyStore.ensureSession(
            userKey: currentUserKey,
            source: .bot,
            preferredSessionID: currentSessionID
        )
        currentSessionID = session.id
        refreshConversationMetadata(preferredSessionID: session.id)
        phase = .sending
        lastDecision = nil

        let assistantID = UUID()
        let levelAtSend = selectedLevel
        let requestContext = makeRequestContext()
        messages.append(AIChatMessage(role: .user, text: trimmedPrompt))
        messages.append(AIChatMessage(id: assistantID, role: .assistant, text: "", isStreaming: true))
        draft = ""
        phase = .toolPending

        activeRequestContext = requestContext
        activeRequestTask?.cancel()
        activeRequestTask = Task { @MainActor [weak self] in
            guard let self else { return }
            do {
                let result = try await service.generateVisual(
                    prompt: buildVisualPrompt(for: trimmedPrompt),
                    aiLevel: levelAtSend.rawValue
                )
                guard isRequestContextValid(requestContext) else { return }
                historyStore.updateRetentionDays(result.historyRetentionDays)
                applyUsage(result.usage)
                lastDecision = result.decision
                updateAssistantMessage(
                    id: assistantID,
                    text: result.text,
                    isStreaming: false,
                    imageData: result.imageData
                )
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .bot,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: trimmedPrompt,
                    response: result.text,
                    imageData: result.imageData
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                phase = resolvedTerminalPhase(for: result.decision)
                clearActiveRequestIfNeeded(requestContext)
            } catch {
                if error is CancellationError || Task.isCancelled {
                    clearActiveRequestIfNeeded(requestContext)
                    return
                }
                guard isRequestContextValid(requestContext) else { return }
                let assistantText = userFacingErrorMessage(for: error)
                lastDecision = decision(for: error)
                phase = resolvedTerminalPhase(for: lastDecision)
                updateAssistantMessage(id: assistantID, text: assistantText, isStreaming: false)
                if let savedResult = historyStore.saveEntry(
                    userKey: requestContext.userKeyAtSend,
                    source: .bot,
                    sessionID: requestContext.sessionIDAtSend,
                    prompt: trimmedPrompt,
                    response: assistantText
                ) {
                    syncSavedEntryToRemote(savedResult)
                }
                refreshConversationMetadata(preferredSessionID: requestContext.sessionIDAtSend)
                showUserToast(userFacingErrorMessage(for: error), style: .error)
                clearActiveRequestIfNeeded(requestContext)
            }
        }
    }

    func resetConversation() {
        startNewConversation()
    }

    func startNewConversation() {
        invalidateConversation(cancelActiveRequest: true)
        messages = []
        lastDecision = nil
        let newSession = historyStore.createSession(
            userKey: currentUserKey,
            source: .bot,
            title: AIScriptHistoryStore.defaultSessionTitle
        )
        currentSessionID = newSession.id
        refreshConversationMetadata(preferredSessionID: newSession.id)
        syncSessionToRemote(newSession)
        phase = draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .idle : .typing
    }

    func openConversation(_ sessionID: UUID) {
        guard !phase.isBusy else { return }
        invalidateConversation(cancelActiveRequest: true)
        lastDecision = nil
        restoreConversationState(preferredSessionID: sessionID)
    }

    func refreshActiveConversation() {
        guard !phase.isBusy else { return }
        invalidateConversation(cancelActiveRequest: true)
        lastDecision = nil
        restoreConversationState(preferredSessionID: currentSessionID)
        showUserToast("Chat aktualisiert.", style: .success)
    }

    func renameActiveConversation(_ title: String) {
        guard let currentSessionID else { return }
        let renamedSession = historyStore.renameSession(
            userKey: currentUserKey,
            source: .bot,
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
        invalidateConversation(cancelActiveRequest: true)
        historyStore.deleteSession(
            userKey: currentUserKey,
            source: .bot,
            sessionID: currentSessionID
        )
        messages = []
        lastDecision = nil
        restoreConversationState()
        deleteSessionFromRemote(currentSessionID)
    }

    private func updateAssistantMessage(id: UUID, text: String, isStreaming: Bool, imageData: Data? = nil) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].text = text
        messages[index].isStreaming = isStreaming
        messages[index].imageData = imageData ?? messages[index].imageData
    }

    private func makeRequestContext() -> InFlightRequestContext {
        InFlightRequestContext(
            requestID: UUID(),
            conversationRevision: conversationRevision,
            userKeyAtSend: currentUserKey,
            sessionIDAtSend: currentSessionID,
            aiLevelAtSend: selectedLevel
        )
    }

    private func isRequestContextValid(_ context: InFlightRequestContext) -> Bool {
        activeRequestContext == context &&
        context.conversationRevision == conversationRevision &&
        context.userKeyAtSend == currentUserKey &&
        context.sessionIDAtSend == currentSessionID &&
        context.aiLevelAtSend == selectedLevel
    }

    private func clearActiveRequestIfNeeded(_ context: InFlightRequestContext) {
        guard activeRequestContext == context else { return }
        activeRequestContext = nil
        activeRequestTask = nil
    }

    private func invalidateConversation(cancelActiveRequest: Bool) {
        if cancelActiveRequest {
            activeRequestTask?.cancel()
            activeRequestTask = nil
            remoteHydrationTask?.cancel()
            remoteHydrationTask = nil
            activeRequestContext = nil
        }
        conversationRevision += 1
    }

    private func buildPrompt(for userPrompt: String, history: String) -> String {
        return """
        Bisheriger Verlauf:
        \(history)

        Nutzeranfrage:
        \(userPrompt)
        """
    }

    private func restoreConversationState(preferredSessionID: UUID? = nil) {
        let session = historyStore.ensureSession(
            userKey: currentUserKey,
            source: .bot,
            preferredSessionID: preferredSessionID
        )
        currentSessionID = session.id
        refreshConversationMetadata(preferredSessionID: session.id)
        let restoredEntries = historyStore.entries(
            for: currentUserKey,
            source: .bot,
            sessionID: session.id
        ).reversed()
        messages = restoredEntries.flatMap { entry in
            [
                AIChatMessage(role: .user, text: entry.prompt),
                AIChatMessage(
                    role: .assistant,
                    text: entry.response,
                    imageData: historyStore.imageData(for: entry)
                )
            ]
        }
        phase = draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .idle : .typing
    }

    private func refreshConversationMetadata(preferredSessionID: UUID? = nil) {
        let session = historyStore.ensureSession(
            userKey: currentUserKey,
            source: .bot,
            preferredSessionID: preferredSessionID ?? currentSessionID
        )
        currentSessionID = session.id
        activeSessionID = session.id
        activeSessionTitle = session.title
        sessions = historyStore.sessionSummaries(for: currentUserKey, source: .bot)
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
        let referenceContext = AIVisualReferenceLibraryStore.promptContext()?
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard let referenceContext, !referenceContext.isEmpty else {
            return userPrompt
        }

        return """
        \(referenceContext)

        \(userPrompt)
        """
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private func hydrateRemoteHistoryIfNeeded(preferredSessionID: UUID?) {
        remoteHydrationTask?.cancel()
        guard let currentUserID else { return }

        let source: AIScriptHistorySource = .bot
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
                guard Task.isCancelled == false else { return }
                guard self.currentUserID == userID, self.currentUserKey == userKey else { return }
                self.historyStore.replaceRemoteState(
                    userKey: userKey,
                    source: source,
                    sessions: refreshedSnapshot.sessions,
                    entries: refreshedSnapshot.entries
                )
                self.restoreConversationState(preferredSessionID: preferredSessionID)
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
            source: .bot
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
            source: .bot,
            sessions: snapshot.sessions,
            entries: snapshot.entries
        )

        if phase.isBusy {
            refreshConversationMetadata(preferredSessionID: currentSessionID)
        } else {
            restoreConversationState(preferredSessionID: currentSessionID)
        }
    }

    private func syncSessionToRemote(_ session: AIScriptHistorySession) {
        guard let currentUserID else { return }
        let retentionDays = currentHistoryRetentionDays
        Task { @MainActor in
            do {
                try await syncService.upsertSession(userID: currentUserID, session: session)
                try? await syncService.pruneHistory(userID: currentUserID, source: .bot, retentionDays: retentionDays)
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
                try? await syncService.pruneHistory(userID: currentUserID, source: .bot, retentionDays: retentionDays)
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
                source: .bot,
                retentionDays: retentionDays
            )
        }
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

    private func resolvedTerminalPhase(for decision: AIBotDecision?) -> BotInteractionPhase {
        guard let decision else { return .complete }
        if decision.ownerDiagnosticActive {
            return .ownerDiagnostic
        }

        switch decision.state {
        case "faq_answer":
            return .faqAnswer
        case "degraded":
            return .degraded
        case "blocked":
            return .blocked
        case "retryable":
            return .retryable
        default:
            return .complete
        }
    }

    private func decision(for error: Error) -> AIBotDecision {
        let nsError = error as NSError
        let description = userFacingErrorMessage(for: error)

        if nsError.domain == FunctionsErrorDomain,
           let code = FunctionsErrorCode(rawValue: nsError.code) {
            switch code {
            case .unavailable, .deadlineExceeded, .internal:
                return AIBotDecision(
                    state: "retryable",
                    route: composerMode == .visual ? "visual" : "assistant",
                    topic: "",
                    summary: "Antwort ist aktuell retrybar.",
                    promptVersion: "",
                    qualityMode: "",
                    faqMode: "",
                    ownerMode: "",
                    answerLength: "",
                    personalityStyle: "",
                    loggingLevel: "",
                    diagnosticsMode: "",
                    ownerDiagnosticActive: false,
                    selectedModel: "",
                    selectedProvider: "",
                    fallbackActivated: false,
                    fallbackReason: "",
                    responseLimited: false,
                    responseLimitReason: "",
                    blocked: false,
                    blockReason: "",
                    retryable: true,
                    retryReason: description,
                    trace: [description]
                )
            default:
                return AIBotDecision(
                    state: "blocked",
                    route: composerMode == .visual ? "visual" : "assistant",
                    topic: "",
                    summary: "Antwort wurde blockiert oder gestoppt.",
                    promptVersion: "",
                    qualityMode: "",
                    faqMode: "",
                    ownerMode: "",
                    answerLength: "",
                    personalityStyle: "",
                    loggingLevel: "",
                    diagnosticsMode: "",
                    ownerDiagnosticActive: false,
                    selectedModel: "",
                    selectedProvider: "",
                    fallbackActivated: false,
                    fallbackReason: "",
                    responseLimited: false,
                    responseLimitReason: "",
                    blocked: true,
                    blockReason: description,
                    retryable: false,
                    retryReason: "",
                    trace: [description]
                )
            }
        }

        return AIBotDecision(
            state: "retryable",
            route: composerMode == .visual ? "visual" : "assistant",
            topic: "",
            summary: "Antwort ist aktuell retrybar.",
            promptVersion: "",
            qualityMode: "",
            faqMode: "",
            ownerMode: "",
            answerLength: "",
            personalityStyle: "",
            loggingLevel: "",
            diagnosticsMode: "",
            ownerDiagnosticActive: false,
            selectedModel: "",
            selectedProvider: "",
            fallbackActivated: false,
            fallbackReason: "",
            responseLimited: false,
            responseLimitReason: "",
            blocked: false,
            blockReason: "",
            retryable: true,
            retryReason: description,
            trace: [description]
        )
    }

    private func userFacingErrorMessage(for error: Error) -> String {
        let nsError = error as NSError

        if nsError.domain == FunctionsErrorDomain,
           let code = FunctionsErrorCode(rawValue: nsError.code) {
            switch code {
            case .notFound, .unimplemented:
                return "Dieser Bot-Bereich wird gerade vorbereitet."
            case .unavailable:
                return "Der SkyOS Bot ist gerade kurz nicht erreichbar."
            case .deadlineExceeded:
                return "Die Antwort dauert gerade laenger als gewohnt."
            case .resourceExhausted:
                return AIExperienceLevel.limitReachedMessage
            case .failedPrecondition:
                if nsError.localizedDescription.localizedCaseInsensitiveContains("App Check") {
                    return "Der Sicherheitscheck laeuft noch. Bitte kurz erneut versuchen."
                }
                return "Die KI ist gerade noch nicht voll bereit. Bitte in einem Moment erneut versuchen."
            case .permissionDenied:
                return AIExperienceLevel.unavailableMessage
            case .unauthenticated:
                return "Bitte melde dich erneut an und versuch es noch einmal."
            case .invalidArgument:
                return "Die Anfrage braucht noch etwas mehr Kontext."
            case .internal:
                if nsError.localizedDescription.localizedCaseInsensitiveContains("server responded with an error") {
                    return "Der Visual-Server antwortet gerade unruhig. Bitte kurz erneut versuchen."
                }
                return "Der Visual-Server antwortet gerade unruhig. Bitte kurz erneut versuchen."
            default:
                break
            }
        }

        if nsError.localizedDescription.localizedCaseInsensitiveContains("server responded with an error") {
            return "Der Visual-Server antwortet gerade unruhig. Bitte kurz erneut versuchen."
        }

        return "Der SkyOS Bot ist gerade kurz pausiert. Bitte in einem Moment erneut versuchen."
    }
}
