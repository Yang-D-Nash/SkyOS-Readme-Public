import Foundation
import FirebaseFirestore

struct AIScriptHistoryRemoteSnapshot {
    let sessions: [AIScriptHistorySession]
    let entries: [AIScriptHistoryEntry]

    var isEmpty: Bool {
        sessions.isEmpty && entries.isEmpty
    }
}

protocol AIConversationSyncServicing {
    func fetchSnapshot(userID: String, source: AIScriptHistorySource) async throws -> AIScriptHistoryRemoteSnapshot
    func observeSnapshot(
        userID: String,
        source: AIScriptHistorySource,
        onChange: @escaping @Sendable (Result<AIScriptHistoryRemoteSnapshot, Error>) -> Void
    ) -> () -> Void
    func migrateLocalHistoryIfRemoteEmpty(
        userID: String,
        source: AIScriptHistorySource,
        localSessions: [AIScriptHistorySession],
        localEntries: [AIScriptHistoryEntry]
    ) async throws -> AIScriptHistoryRemoteSnapshot
    func upsertSession(userID: String, session: AIScriptHistorySession) async throws
    func upsertEntry(userID: String, entry: AIScriptHistoryEntry) async throws
    func deleteSession(userID: String, sessionID: UUID) async throws
    func pruneHistory(userID: String, source: AIScriptHistorySource, retentionDays: Int) async throws
}

final class FirestoreAIConversationSyncService: AIConversationSyncServicing {
    private let firestore: Firestore
    private let maximumEntriesPerSource = 360
    private let maximumSessionsPerSource = 40

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func fetchSnapshot(userID: String, source: AIScriptHistorySource) async throws -> AIScriptHistoryRemoteSnapshot {
        let sessionDocuments = try await sessionsCollection(for: userID)
            .order(by: "updatedAt", descending: true)
            .getDocuments()
        let entryDocuments = try await entriesCollection(for: userID)
            .order(by: "createdAt", descending: true)
            .getDocuments()

        let entries = entryDocuments.documents.compactMap { document in
            decodeEntry(document, expectedSource: source, userID: userID)
        }
        let sessions = normalizedSessions(
            from: sessionDocuments.documents.compactMap { document in
                decodeSession(document, expectedSource: source, userID: userID)
            },
            entries: entries,
            userID: userID,
            source: source
        )

        return AIScriptHistoryRemoteSnapshot(
            sessions: sessions.sorted { $0.updatedAt > $1.updatedAt },
            entries: entries.sorted { $0.createdAt > $1.createdAt }
        )
    }

    func observeSnapshot(
        userID: String,
        source: AIScriptHistorySource,
        onChange: @escaping @Sendable (Result<AIScriptHistoryRemoteSnapshot, Error>) -> Void
    ) -> () -> Void {
        let accumulator = RemoteSnapshotAccumulator()

        let sessionListener = sessionsCollection(for: userID)
            .order(by: "updatedAt", descending: true)
            .addSnapshotListener { [weak self, weak accumulator] snapshot, error in
                guard let self, let accumulator else { return }
                if let error {
                    onChange(.failure(error))
                    return
                }

                accumulator.sessions = snapshot?.documents.compactMap { document in
                    self.decodeSession(document, expectedSource: source, userID: userID)
                } ?? []
                self.deliverAccumulatedSnapshot(
                    accumulator,
                    userID: userID,
                    source: source,
                    onChange: onChange
                )
            }

        let entryListener = entriesCollection(for: userID)
            .order(by: "createdAt", descending: true)
            .addSnapshotListener { [weak self, weak accumulator] snapshot, error in
                guard let self, let accumulator else { return }
                if let error {
                    onChange(.failure(error))
                    return
                }

                accumulator.entries = snapshot?.documents.compactMap { document in
                    self.decodeEntry(document, expectedSource: source, userID: userID)
                } ?? []
                self.deliverAccumulatedSnapshot(
                    accumulator,
                    userID: userID,
                    source: source,
                    onChange: onChange
                )
            }

        return {
            sessionListener.remove()
            entryListener.remove()
        }
    }

    func migrateLocalHistoryIfRemoteEmpty(
        userID: String,
        source: AIScriptHistorySource,
        localSessions: [AIScriptHistorySession],
        localEntries: [AIScriptHistoryEntry]
    ) async throws -> AIScriptHistoryRemoteSnapshot {
        let remoteSnapshot = try await fetchSnapshot(userID: userID, source: source)
        guard remoteSnapshot.isEmpty else {
            return remoteSnapshot
        }

        guard !localSessions.isEmpty || !localEntries.isEmpty else {
            return remoteSnapshot
        }

        let batch = firestore.batch()
        localSessions
            .filter { $0.source == source }
            .forEach { session in
                batch.setData(
                    encodeSession(session),
                    forDocument: sessionsCollection(for: userID).document(session.id.uuidString),
                    merge: true
                )
            }
        localEntries
            .filter { $0.source == source }
            .forEach { entry in
                batch.setData(
                    encodeEntry(entry),
                    forDocument: entriesCollection(for: userID).document(entry.id.uuidString),
                    merge: true
                )
            }
        try await batch.commit()
        return try await fetchSnapshot(userID: userID, source: source)
    }

    func upsertSession(userID: String, session: AIScriptHistorySession) async throws {
        try await sessionsCollection(for: userID)
            .document(session.id.uuidString)
            .setData(encodeSession(session), merge: true)
    }

    func upsertEntry(userID: String, entry: AIScriptHistoryEntry) async throws {
        try await entriesCollection(for: userID)
            .document(entry.id.uuidString)
            .setData(encodeEntry(entry), merge: true)
    }

    func deleteSession(userID: String, sessionID: UUID) async throws {
        let entryDocuments = try await entriesCollection(for: userID)
            .whereField("sessionId", isEqualTo: sessionID.uuidString)
            .getDocuments()

        let batch = firestore.batch()
        batch.deleteDocument(sessionsCollection(for: userID).document(sessionID.uuidString))
        entryDocuments.documents.forEach { document in
            batch.deleteDocument(document.reference)
        }
        try await batch.commit()
    }

    func pruneHistory(userID: String, source: AIScriptHistorySource, retentionDays: Int) async throws {
        let snapshot = try await fetchSnapshot(userID: userID, source: source)
        guard !snapshot.isEmpty else { return }

        let cutoff = Calendar.current.date(byAdding: .day, value: -retentionDays, to: .now) ?? .distantPast
        let keptSessions = Array(
            snapshot.sessions
                .sorted { $0.updatedAt > $1.updatedAt }
                .prefix(maximumSessionsPerSource)
        )
        let keptSessionIDs = Set(keptSessions.map(\.id))

        let keptEntries = Array(
            snapshot.entries
                .sorted { $0.createdAt > $1.createdAt }
                .filter { entry in
                    guard let sessionID = entry.sessionID else { return false }
                    return entry.createdAt >= cutoff && keptSessionIDs.contains(sessionID)
                }
                .prefix(maximumEntriesPerSource)
        )
        let keptEntryIDs = Set(keptEntries.map(\.id))
        let keptSessionIDsWithEntries = Set(keptEntries.compactMap(\.sessionID))
        let finalKeptSessionIDs = Set(
            keptSessions
                .filter { keptSessionIDsWithEntries.contains($0.id) || $0.updatedAt >= cutoff }
                .map(\.id)
        )

        let batch = firestore.batch()
        var hasChanges = false

        snapshot.entries
            .filter { keptEntryIDs.contains($0.id) == false }
            .forEach { entry in
                batch.deleteDocument(entriesCollection(for: userID).document(entry.id.uuidString))
                hasChanges = true
            }

        snapshot.sessions
            .filter { finalKeptSessionIDs.contains($0.id) == false }
            .forEach { session in
                batch.deleteDocument(sessionsCollection(for: userID).document(session.id.uuidString))
                hasChanges = true
            }

        if hasChanges {
            try await batch.commit()
        }
    }

    private func sessionsCollection(for userID: String) -> CollectionReference {
        firestore.collection("users").document(userID).collection("aiSessions")
    }

    private func entriesCollection(for userID: String) -> CollectionReference {
        firestore.collection("users").document(userID).collection("aiEntries")
    }

    private func deliverAccumulatedSnapshot(
        _ accumulator: RemoteSnapshotAccumulator,
        userID: String,
        source: AIScriptHistorySource,
        onChange: @escaping @Sendable (Result<AIScriptHistoryRemoteSnapshot, Error>) -> Void
    ) {
        guard let entries = accumulator.entries,
              let rawSessions = accumulator.sessions else {
            return
        }

        let sessions = normalizedSessions(
            from: rawSessions,
            entries: entries,
            userID: userID,
            source: source
        )

        onChange(
            .success(
                AIScriptHistoryRemoteSnapshot(
                    sessions: sessions.sorted { $0.updatedAt > $1.updatedAt },
                    entries: entries.sorted { $0.createdAt > $1.createdAt }
                )
            )
        )
    }

    private func encodeSession(_ session: AIScriptHistorySession) -> [String: Any] {
        [
            "source": session.source.rawValue,
            "title": session.title,
            "createdAt": Timestamp(date: session.createdAt),
            "updatedAt": Timestamp(date: session.updatedAt)
        ]
    }

    private func encodeEntry(_ entry: AIScriptHistoryEntry) -> [String: Any] {
        [
            "sessionId": entry.sessionID?.uuidString ?? "",
            "source": entry.source.rawValue,
            "prompt": entry.prompt,
            "response": entry.response,
            "resultType": entry.resultType,
            "automationMessage": entry.automationMessage,
            "workflowName": entry.workflowName,
            "agentRunId": entry.agentRunID,
            "results": entry.structuredResults.map { result in
                [
                    "type": result.type,
                    "text": result.text,
                    "url": result.url,
                    "title": result.title,
                    "mimeType": result.mimeType,
                    "fileName": result.fileName,
                    "html": result.html,
                    "columns": result.columns,
                    "rows": result.rows,
                    "workflowName": result.workflowName,
                    "status": result.status,
                    "summary": result.summary,
                    "runId": result.runID
                ]
            },
            "createdAt": Timestamp(date: entry.createdAt)
        ]
    }

    private func decodeSession(
        _ document: QueryDocumentSnapshot,
        expectedSource: AIScriptHistorySource,
        userID: String
    ) -> AIScriptHistorySession? {
        guard let sessionID = UUID(uuidString: document.documentID) else { return nil }
        let data = document.data()
        guard (data["source"] as? String) == expectedSource.rawValue else { return nil }

        let rawTitle = (data["title"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let title = rawTitle.isEmpty ? AIScriptHistoryStore.defaultSessionTitle : rawTitle
        let createdAt = (data["createdAt"] as? Timestamp)?.dateValue() ?? .now
        let updatedAt = (data["updatedAt"] as? Timestamp)?.dateValue() ?? createdAt

        return AIScriptHistorySession(
            id: sessionID,
            userKey: userID.lowercased(),
            source: expectedSource,
            title: title,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }

    private func decodeEntry(
        _ document: QueryDocumentSnapshot,
        expectedSource: AIScriptHistorySource,
        userID: String
    ) -> AIScriptHistoryEntry? {
        guard let entryID = UUID(uuidString: document.documentID) else { return nil }
        let data = document.data()
        guard (data["source"] as? String) == expectedSource.rawValue,
              let sessionIDString = data["sessionId"] as? String,
              let sessionID = UUID(uuidString: sessionIDString) else {
            return nil
        }

        let prompt = (data["prompt"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let response = (data["response"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !prompt.isEmpty, !response.isEmpty else { return nil }
        let resultType = (data["resultType"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? "text"
        let automationMessage = (data["automationMessage"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let workflowName = (data["workflowName"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let agentRunID = (data["agentRunId"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let structuredResults: [AIScriptHistoryResultEntry] = (data["results"] as? [Any])?.compactMap { raw in
            let result = raw as? [String: Any]
            guard let result else { return nil }
            return AIScriptHistoryResultEntry(
                type: (result["type"] as? String) ?? "text",
                text: (result["text"] as? String) ?? "",
                url: (result["url"] as? String) ?? "",
                title: (result["title"] as? String) ?? "",
                mimeType: (result["mimeType"] as? String) ?? "",
                fileName: (result["fileName"] as? String) ?? "",
                html: (result["html"] as? String) ?? "",
                columns: (result["columns"] as? [String]) ?? [],
                rows: (result["rows"] as? [[String]]) ?? [],
                workflowName: (result["workflowName"] as? String) ?? "",
                status: (result["status"] as? String) ?? "",
                summary: (result["summary"] as? String) ?? "",
                runID: (result["runId"] as? String) ?? ""
            )
        } ?? []

        return AIScriptHistoryEntry(
            id: entryID,
            userKey: userID.lowercased(),
            source: expectedSource,
            sessionID: sessionID,
            prompt: prompt,
            response: response,
            imageFileName: nil,
            resultType: resultType.isEmpty ? "text" : resultType,
            automationMessage: automationMessage,
            workflowName: workflowName,
            agentRunID: agentRunID,
            structuredResults: structuredResults,
            createdAt: (data["createdAt"] as? Timestamp)?.dateValue() ?? .now
        )
    }

    private func normalizedSessions(
        from sessions: [AIScriptHistorySession],
        entries: [AIScriptHistoryEntry],
        userID: String,
        source: AIScriptHistorySource
    ) -> [AIScriptHistorySession] {
        var knownSessions = Dictionary(uniqueKeysWithValues: sessions.map { ($0.id, $0) })
        let groupedEntries = Dictionary(grouping: entries) { $0.sessionID }

        for (maybeSessionID, sessionEntries) in groupedEntries {
            guard let sessionID = maybeSessionID else { continue }
            if knownSessions[sessionID] != nil { continue }

            let newestDate = sessionEntries.map(\.createdAt).max() ?? .now
            let oldestDate = sessionEntries.map(\.createdAt).min() ?? newestDate
            let previewTitle = suggestedSessionTitle(from: sessionEntries.last?.prompt ?? "")
            knownSessions[sessionID] = AIScriptHistorySession(
                id: sessionID,
                userKey: userID.lowercased(),
                source: source,
                title: previewTitle,
                createdAt: oldestDate,
                updatedAt: newestDate
            )
        }

        return Array(knownSessions.values)
    }

    private func suggestedSessionTitle(from prompt: String) -> String {
        let collapsedPrompt = prompt
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !collapsedPrompt.isEmpty else { return AIScriptHistoryStore.defaultSessionTitle }
        return String(collapsedPrompt.prefix(38))
    }
}

private final class RemoteSnapshotAccumulator {
    var sessions: [AIScriptHistorySession]?
    var entries: [AIScriptHistoryEntry]?
}
