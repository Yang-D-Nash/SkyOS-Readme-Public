import Foundation

enum AIScriptHistorySource: String, Codable, CaseIterable, Identifiable {
    case bot
    case agent

    var id: String { rawValue }

    var title: String {
        switch self {
        case .bot:
            return "Bot"
        case .agent:
            return "Agent"
        }
    }
}

enum AIScriptHistoryRetention: Int, CaseIterable, Identifiable {
    case oneDay = 1
    case threeDays = 3
    case sevenDays = 7
    case thirtyDays = 30

    var id: Int { rawValue }

    var title: String {
        switch self {
        case .oneDay:
            return "1 Tag"
        case .threeDays:
            return "3 Tage"
        case .sevenDays:
            return "7 Tage"
        case .thirtyDays:
            return "30 Tage"
        }
    }
}

struct AIScriptHistorySession: Identifiable, Codable, Equatable {
    let id: UUID
    let userKey: String
    let source: AIScriptHistorySource
    var title: String
    let createdAt: Date
    var updatedAt: Date

    init(
        id: UUID = UUID(),
        userKey: String,
        source: AIScriptHistorySource,
        title: String = AIScriptHistoryStore.defaultSessionTitle,
        createdAt: Date = .now,
        updatedAt: Date = .now
    ) {
        self.id = id
        self.userKey = userKey
        self.source = source
        self.title = title
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

struct AIScriptHistorySessionSummary: Identifiable, Equatable {
    let id: UUID
    let title: String
    let preview: String
    let promptCount: Int
    let createdAt: Date
    let updatedAt: Date
}

struct AIScriptHistoryEntry: Identifiable, Codable, Equatable {
    let id: UUID
    let userKey: String
    let source: AIScriptHistorySource
    var sessionID: UUID?
    let prompt: String
    let response: String
    var imageFileName: String?
    var resultType: String
    var automationMessage: String
    var workflowName: String
    var agentRunID: String
    var structuredResults: [AIScriptHistoryResultEntry]
    let createdAt: Date

    init(
        id: UUID = UUID(),
        userKey: String,
        source: AIScriptHistorySource,
        sessionID: UUID?,
        prompt: String,
        response: String,
        imageFileName: String? = nil,
        resultType: String = "text",
        automationMessage: String = "",
        workflowName: String = "",
        agentRunID: String = "",
        structuredResults: [AIScriptHistoryResultEntry] = [],
        createdAt: Date = .now
    ) {
        self.id = id
        self.userKey = userKey
        self.source = source
        self.sessionID = sessionID
        self.prompt = prompt
        self.response = response
        self.imageFileName = imageFileName
        self.resultType = resultType
        self.automationMessage = automationMessage
        self.workflowName = workflowName
        self.agentRunID = agentRunID
        self.structuredResults = structuredResults
        self.createdAt = createdAt
    }
}

struct AIScriptHistoryResultEntry: Codable, Equatable {
    let type: String
    let text: String
    let url: String
    let title: String
    let mimeType: String
    let fileName: String
    let html: String
    let columns: [String]
    let rows: [[String]]
    let workflowName: String
    let status: String
    let summary: String
    let runID: String
}

struct AIScriptHistorySaveResult {
    let session: AIScriptHistorySession
    let entry: AIScriptHistoryEntry
}

@MainActor
final class AIScriptHistoryStore: ObservableObject {
    static let shared = AIScriptHistoryStore()
    nonisolated static let defaultSessionTitle = "Neuer Chat"

    @Published private(set) var entries: [AIScriptHistoryEntry] = []
    @Published private(set) var sessions: [AIScriptHistorySession] = []
    @Published private(set) var retention: AIScriptHistoryRetention = .threeDays

    private let entriesKey = "ai_script_history_entries"
    private let sessionsKey = "ai_script_history_sessions"
    private let retentionKey = "ai_script_history_retention"
    private let visualImageDirectoryName = "ai_visual_history"
    private let maximumEntries = 360
    private let maximumSessions = 40

    private init() {
        retention = loadRetention()
        entries = loadEntries()
        sessions = loadSessions()
        migrateLegacyEntriesIfNeeded()
        pruneExpiredEntries()
        pruneOverflowIfNeeded()
    }

    func entries(
        for userKey: String?,
        source: AIScriptHistorySource? = nil,
        sessionID: UUID? = nil
    ) -> [AIScriptHistoryEntry] {
        let normalizedUserKey = normalizeUserKey(userKey)
        return entries
            .filter { entry in
                entry.userKey == normalizedUserKey &&
                (source == nil || entry.source == source) &&
                (sessionID == nil || entry.sessionID == sessionID)
            }
            .sorted { $0.createdAt > $1.createdAt }
    }

    func sessionSummaries(for userKey: String?, source: AIScriptHistorySource) -> [AIScriptHistorySessionSummary] {
        let normalizedUserKey = normalizeUserKey(userKey)
        return sessions
            .filter { $0.userKey == normalizedUserKey && $0.source == source }
            .map { session in
                let sessionEntries = entries(for: normalizedUserKey, source: source, sessionID: session.id)
                let previewSource = sessionEntries.first?.prompt ?? sessionEntries.first?.response ?? ""
                return AIScriptHistorySessionSummary(
                    id: session.id,
                    title: session.title,
                    preview: previewText(from: previewSource),
                    promptCount: sessionEntries.count,
                    createdAt: session.createdAt,
                    updatedAt: session.updatedAt
                )
            }
            .sorted { $0.updatedAt > $1.updatedAt }
    }

    func sessionRecords(for userKey: String?, source: AIScriptHistorySource) -> [AIScriptHistorySession] {
        let normalizedUserKey = normalizeUserKey(userKey)
        return sessions
            .filter { $0.userKey == normalizedUserKey && $0.source == source }
            .sorted { $0.updatedAt > $1.updatedAt }
    }

    func ensureSession(
        userKey: String?,
        source: AIScriptHistorySource,
        preferredSessionID: UUID? = nil
    ) -> AIScriptHistorySession {
        let normalizedUserKey = normalizeUserKey(userKey)
        if let preferredSessionID,
           let preferredSession = sessions.first(where: {
               $0.userKey == normalizedUserKey && $0.source == source && $0.id == preferredSessionID
           }) {
            return preferredSession
        }

        if let existingSession = sessions
            .filter({ $0.userKey == normalizedUserKey && $0.source == source })
            .sorted(by: { $0.updatedAt > $1.updatedAt })
            .first {
            return existingSession
        }

        return createSession(
            userKey: normalizedUserKey,
            source: source,
            title: Self.defaultSessionTitle
        )
    }

    @discardableResult
    func createSession(
        userKey: String?,
        source: AIScriptHistorySource,
        title: String = AIScriptHistoryStore.defaultSessionTitle
    ) -> AIScriptHistorySession {
        let normalizedUserKey = normalizeUserKey(userKey)
        let session = AIScriptHistorySession(
            userKey: normalizedUserKey,
            source: source,
            title: sanitizeSessionTitle(title)
        )
        sessions.append(session)
        persistSessions()
        pruneOverflowIfNeeded()
        return session
    }

    @discardableResult
    func renameSession(
        userKey: String?,
        source: AIScriptHistorySource,
        sessionID: UUID,
        title: String
    ) -> AIScriptHistorySession? {
        let normalizedUserKey = normalizeUserKey(userKey)
        guard let sessionIndex = sessions.firstIndex(where: {
            $0.userKey == normalizedUserKey && $0.source == source && $0.id == sessionID
        }) else {
            return nil
        }

        sessions[sessionIndex].title = sanitizeSessionTitle(title)
        sessions[sessionIndex].updatedAt = .now
        persistSessions()
        return sessions[sessionIndex]
    }

    func deleteSession(
        userKey: String?,
        source: AIScriptHistorySource,
        sessionID: UUID
    ) {
        let normalizedUserKey = normalizeUserKey(userKey)
        let removedEntries = entries.filter {
            $0.userKey == normalizedUserKey &&
            $0.source == source &&
            $0.sessionID == sessionID
        }
        entries.removeAll {
            $0.userKey == normalizedUserKey &&
            $0.source == source &&
            $0.sessionID == sessionID
        }
        sessions.removeAll {
            $0.userKey == normalizedUserKey &&
            $0.source == source &&
            $0.id == sessionID
        }
        removeStoredImages(for: removedEntries)
        persistEntries()
        persistSessions()
    }

    @discardableResult
    func saveEntry(
        userKey: String?,
        source: AIScriptHistorySource,
        sessionID: UUID? = nil,
        prompt: String,
        response: String,
        imageData: Data? = nil,
        resultType: String = "text",
        automationMessage: String = "",
        workflowName: String = "",
        agentRunID: String = "",
        structuredResults: [AIScriptHistoryResultEntry] = []
    ) -> AIScriptHistorySaveResult? {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedResponse = response.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !trimmedResponse.isEmpty else { return nil }

        var session = ensureSession(
            userKey: userKey,
            source: source,
            preferredSessionID: sessionID
        )

        let sessionWasEmpty = entries.contains {
            $0.userKey == session.userKey &&
            $0.source == source &&
            $0.sessionID == session.id
        } == false

        if sessionWasEmpty && session.title == Self.defaultSessionTitle {
            session.title = suggestedSessionTitle(from: trimmedPrompt)
        }
        session.updatedAt = .now
        upsertSession(session)

        let entryID = UUID()
        let imageFileName = imageData.flatMap { storeImageData($0, for: entryID) }
        let entry = AIScriptHistoryEntry(
            id: entryID,
            userKey: session.userKey,
            source: source,
            sessionID: session.id,
            prompt: trimmedPrompt,
            response: trimmedResponse,
            imageFileName: imageFileName,
            resultType: resultType,
            automationMessage: automationMessage,
            workflowName: workflowName,
            agentRunID: agentRunID,
            structuredResults: structuredResults
        )
        entries.insert(entry, at: 0)
        persistEntries()
        pruneExpiredEntries()
        pruneOverflowIfNeeded()
        return AIScriptHistorySaveResult(session: session, entry: entry)
    }

    func deleteEntry(_ entry: AIScriptHistoryEntry) {
        entries.removeAll { $0.id == entry.id }
        removeStoredImage(for: entry)
        persistEntries()
    }

    func clearEntries(
        userKey: String?,
        source: AIScriptHistorySource? = nil,
        sessionID: UUID? = nil
    ) {
        let normalizedUserKey = normalizeUserKey(userKey)
        let removedEntries = entries.filter { entry in
            entry.userKey == normalizedUserKey &&
            (source == nil || entry.source == source) &&
            (sessionID == nil || entry.sessionID == sessionID)
        }
        entries.removeAll { entry in
            entry.userKey == normalizedUserKey &&
            (source == nil || entry.source == source) &&
            (sessionID == nil || entry.sessionID == sessionID)
        }
        removeStoredImages(for: removedEntries)
        persistEntries()
    }

    func updateRetention(_ retention: AIScriptHistoryRetention) {
        self.retention = retention
        UserDefaults.standard.set(retention.rawValue, forKey: retentionKey)
        pruneExpiredEntries()
    }

    func updateRetentionDays(_ days: Int) {
        updateRetention(AIScriptHistoryRetention(rawValue: days) ?? .threeDays)
    }

    func imageData(for entry: AIScriptHistoryEntry) -> Data? {
        guard let imageFileName = entry.imageFileName,
              let imageURL = storedImageURL(for: imageFileName) else { return nil }
        return try? Data(contentsOf: imageURL)
    }

    func replaceRemoteState(
        userKey: String?,
        source: AIScriptHistorySource,
        sessions remoteSessions: [AIScriptHistorySession],
        entries remoteEntries: [AIScriptHistoryEntry]
    ) {
        let normalizedUserKey = normalizeUserKey(userKey)
        let existingEntries = entries.filter {
            $0.userKey == normalizedUserKey && $0.source == source
        }
        // Build without Dictionary(uniqueKeysWithValues:): duplicate entry IDs in local state must not crash.
        var existingImageFileNamesByEntryID: [UUID: String] = [:]
        for entry in existingEntries {
            if let imageFileName = entry.imageFileName {
                existingImageFileNamesByEntryID[entry.id] = imageFileName
            }
        }

        let normalizedSessions = remoteSessions
            .filter { $0.userKey == normalizedUserKey && $0.source == source }
            .map { session in
                AIScriptHistorySession(
                    id: session.id,
                    userKey: normalizedUserKey,
                    source: source,
                    title: sanitizeSessionTitle(session.title),
                    createdAt: session.createdAt,
                    updatedAt: session.updatedAt
                )
            }
            .sorted { $0.updatedAt > $1.updatedAt }

        let normalizedEntries = remoteEntries
            .filter { $0.userKey == normalizedUserKey && $0.source == source }
            .map { entry in
                AIScriptHistoryEntry(
                    id: entry.id,
                    userKey: normalizedUserKey,
                    source: source,
                    sessionID: entry.sessionID,
                    prompt: entry.prompt,
                    response: entry.response,
                    imageFileName: existingImageFileNamesByEntryID[entry.id] ?? entry.imageFileName,
                    resultType: entry.resultType,
                    automationMessage: entry.automationMessage,
                    workflowName: entry.workflowName,
                    agentRunID: entry.agentRunID,
                    structuredResults: entry.structuredResults,
                    createdAt: entry.createdAt
                )
            }
            .sorted { $0.createdAt > $1.createdAt }

        let retainedEntryIDs = Set(normalizedEntries.map { $0.id })
        let removedEntries = existingEntries.filter { retainedEntryIDs.contains($0.id) == false }
        removeStoredImages(for: removedEntries)

        sessions.removeAll { $0.userKey == normalizedUserKey && $0.source == source }
        entries.removeAll { $0.userKey == normalizedUserKey && $0.source == source }
        sessions.append(contentsOf: normalizedSessions)
        entries.append(contentsOf: normalizedEntries)
        sessions.sort { $0.updatedAt > $1.updatedAt }
        entries.sort { $0.createdAt > $1.createdAt }
        persistSessions()
        persistEntries()
    }

    private func migrateLegacyEntriesIfNeeded() {
        var updatedEntries = entries
        var changedEntries = false
        var knownSessions = sessions

        for index in updatedEntries.indices {
            let entry = updatedEntries[index]
            if let sessionID = entry.sessionID,
               knownSessions.contains(where: { $0.id == sessionID }) {
                continue
            }

            let matchingSession = knownSessions
                .filter { $0.userKey == entry.userKey && $0.source == entry.source }
                .sorted(by: { $0.updatedAt > $1.updatedAt })
                .first

            let session: AIScriptHistorySession
            if let matchingSession {
                session = matchingSession
            } else {
                let createdSession = AIScriptHistorySession(
                    userKey: entry.userKey,
                    source: entry.source,
                    title: suggestedSessionTitle(from: entry.prompt),
                    createdAt: entry.createdAt,
                    updatedAt: entry.createdAt
                )
                knownSessions.append(createdSession)
                session = createdSession
            }

            updatedEntries[index].sessionID = session.id
            changedEntries = true
        }

        let normalizedSessions = knownSessions.map { session in
            AIScriptHistorySession(
                id: session.id,
                userKey: normalizeUserKey(session.userKey),
                source: session.source,
                title: sanitizeSessionTitle(session.title),
                createdAt: session.createdAt,
                updatedAt: session.updatedAt
            )
        }

        let sessionsChanged = normalizedSessions != sessions
        if changedEntries || sessionsChanged {
            entries = updatedEntries.sorted { $0.createdAt > $1.createdAt }
            sessions = normalizedSessions.sorted { $0.updatedAt > $1.updatedAt }
            persistEntries()
            persistSessions()
        }
    }

    private func pruneExpiredEntries() {
        let cutoff = Calendar.current.date(byAdding: .day, value: -retention.rawValue, to: .now) ?? .distantPast
        let retainedEntries = entries
            .filter { $0.createdAt >= cutoff }
            .sorted { $0.createdAt > $1.createdAt }
        if retainedEntries != entries {
            let removedEntries = entries.filter { existingEntry in
                retainedEntries.contains(where: { $0.id == existingEntry.id }) == false
            }
            removeStoredImages(for: removedEntries)
            entries = retainedEntries
            persistEntries()
        }
    }

    private func pruneOverflowIfNeeded() {
        if entries.count > maximumEntries {
            let retainedEntries = Array(entries.prefix(maximumEntries))
            let overflowEntries = Array(entries.dropFirst(maximumEntries))
            removeStoredImages(for: overflowEntries)
            entries = retainedEntries
            persistEntries()
        }

        let groupedSessions = Dictionary(grouping: sessions) { session in
            "\(session.userKey)|\(session.source.rawValue)"
        }

        let retainedSessionIDs = groupedSessions.values.reduce(into: Set<UUID>()) { partialResult, sessionGroup in
            let keptSessions = sessionGroup
                .sorted { $0.updatedAt > $1.updatedAt }
                .prefix(maximumSessions)
            partialResult.formUnion(keptSessions.map(\.id))
        }

        if retainedSessionIDs.count != sessions.count {
            let removedSessionIDs = Set(sessions.map(\.id)).subtracting(retainedSessionIDs)
            let removedEntries = entries.filter { entry in
                guard let sessionID = entry.sessionID else { return false }
                return removedSessionIDs.contains(sessionID)
            }
            removeStoredImages(for: removedEntries)
            entries.removeAll { entry in
                guard let sessionID = entry.sessionID else { return false }
                return removedSessionIDs.contains(sessionID)
            }
            sessions = sessions.filter { retainedSessionIDs.contains($0.id) }
            persistEntries()
            persistSessions()
        }
    }

    private func loadEntries() -> [AIScriptHistoryEntry] {
        guard let data = UserDefaults.standard.data(forKey: entriesKey) else { return [] }
        return ((try? JSONDecoder().decode([AIScriptHistoryEntry].self, from: data)) ?? [])
            .sorted { $0.createdAt > $1.createdAt }
    }

    private func persistEntries() {
        guard let data = try? JSONEncoder().encode(entries) else { return }
        UserDefaults.standard.set(data, forKey: entriesKey)
    }

    private func loadSessions() -> [AIScriptHistorySession] {
        guard let data = UserDefaults.standard.data(forKey: sessionsKey) else { return [] }
        return ((try? JSONDecoder().decode([AIScriptHistorySession].self, from: data)) ?? [])
            .sorted { $0.updatedAt > $1.updatedAt }
    }

    private func persistSessions() {
        guard let data = try? JSONEncoder().encode(sessions) else { return }
        UserDefaults.standard.set(data, forKey: sessionsKey)
    }

    private func loadRetention() -> AIScriptHistoryRetention {
        let rawValue = UserDefaults.standard.integer(forKey: retentionKey)
        return AIScriptHistoryRetention(rawValue: rawValue) ?? .threeDays
    }

    private func normalizeUserKey(_ userKey: String?) -> String {
        let trimmed = userKey?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? "guest" : trimmed.lowercased()
    }

    private func sanitizeSessionTitle(_ title: String) -> String {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? Self.defaultSessionTitle : trimmed
    }

    private func suggestedSessionTitle(from prompt: String) -> String {
        let collapsedPrompt = prompt
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !collapsedPrompt.isEmpty else { return Self.defaultSessionTitle }
        return String(collapsedPrompt.prefix(38))
    }

    private func previewText(from text: String) -> String {
        text
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func upsertSession(_ session: AIScriptHistorySession) {
        if let index = sessions.firstIndex(where: { $0.id == session.id }) {
            sessions[index] = session
        } else {
            sessions.append(session)
        }
        sessions.sort { $0.updatedAt > $1.updatedAt }
        persistSessions()
    }

    private func storeImageData(_ imageData: Data, for entryID: UUID) -> String? {
        let fileName = "\(entryID.uuidString).img"
        guard let imageURL = storedImageURL(for: fileName, createDirectory: true) else { return nil }
        do {
            try imageData.write(to: imageURL, options: .atomic)
            return fileName
        } catch {
            return nil
        }
    }

    private func removeStoredImages(for entries: [AIScriptHistoryEntry]) {
        entries.forEach(removeStoredImage(for:))
    }

    private func removeStoredImage(for entry: AIScriptHistoryEntry) {
        guard let imageFileName = entry.imageFileName,
              let imageURL = storedImageURL(for: imageFileName) else { return }
        try? FileManager.default.removeItem(at: imageURL)
    }

    private func storedImageURL(for fileName: String, createDirectory: Bool = false) -> URL? {
        guard let directoryURL = visualImageDirectoryURL(createIfNeeded: createDirectory) else { return nil }
        return directoryURL.appendingPathComponent(fileName)
    }

    private func visualImageDirectoryURL(createIfNeeded: Bool) -> URL? {
        guard let baseURL = try? FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: createIfNeeded
        ) else {
            return nil
        }

        let directoryURL = baseURL.appendingPathComponent(visualImageDirectoryName, isDirectory: true)
        if createIfNeeded, FileManager.default.fileExists(atPath: directoryURL.path) == false {
            try? FileManager.default.createDirectory(at: directoryURL, withIntermediateDirectories: true)
        }
        return directoryURL
    }
}
