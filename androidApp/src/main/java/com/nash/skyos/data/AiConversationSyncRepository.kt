package com.nash.skyos.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class AiConversationRemoteSnapshot(
    val sessions: List<AiConversationHistorySession> = emptyList(),
    val entries: List<AiConversationHistoryEntry> = emptyList(),
) {
    val isEmpty: Boolean
        get() = sessions.isEmpty() && entries.isEmpty()
}

class AiConversationSyncRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val maximumEntriesPerSource = 360
    private val maximumSessionsPerSource = 40

    suspend fun fetchSnapshot(
        userId: String,
        source: AiConversationHistorySource,
    ): AiConversationRemoteSnapshot {
        val sessionDocuments = sessionsCollection(userId)
            .orderBy("updatedAt")
            .get()
            .await()
            .documents
        val entryDocuments = entriesCollection(userId)
            .orderBy("createdAt")
            .get()
            .await()
            .documents

        val entries = entryDocuments.mapNotNull { document ->
            decodeEntry(document.data.orEmpty(), document.id, source, userId)
        }.sortedByDescending { it.createdAtEpochMillis }
        val sessions = normalizeSessions(
            sessions = sessionDocuments.mapNotNull { document ->
                decodeSession(document.data.orEmpty(), document.id, source, userId)
            },
            entries = entries,
            userId = userId,
            source = source,
        ).sortedByDescending { it.updatedAtEpochMillis }

        return AiConversationRemoteSnapshot(
            sessions = sessions,
            entries = entries,
        )
    }

    fun observeSnapshot(
        userId: String,
        source: AiConversationHistorySource,
        onChange: (Result<AiConversationRemoteSnapshot>) -> Unit,
    ): ListenerRegistration {
        val accumulator = AiConversationRemoteSnapshotAccumulator()

        val sessionListener = sessionsCollection(userId)
            .orderBy("updatedAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onChange(Result.failure(error))
                    return@addSnapshotListener
                }

                accumulator.sessions = snapshot?.documents.orEmpty().mapNotNull { document ->
                    decodeSession(document.data.orEmpty(), document.id, source, userId)
                }
                deliverAccumulatedSnapshot(accumulator, userId, source, onChange)
            }

        val entryListener = entriesCollection(userId)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onChange(Result.failure(error))
                    return@addSnapshotListener
                }

                accumulator.entries = snapshot?.documents.orEmpty().mapNotNull { document ->
                    decodeEntry(document.data.orEmpty(), document.id, source, userId)
                }
                deliverAccumulatedSnapshot(accumulator, userId, source, onChange)
            }

        return CompositeListenerRegistration(
            listOf(sessionListener, entryListener),
        )
    }

    suspend fun migrateLocalHistoryIfRemoteEmpty(
        userId: String,
        source: AiConversationHistorySource,
        localSessions: List<AiConversationHistorySession>,
        localEntries: List<AiConversationHistoryEntry>,
    ): AiConversationRemoteSnapshot {
        val remoteSnapshot = fetchSnapshot(userId, source)
        if (!remoteSnapshot.isEmpty) {
            return remoteSnapshot
        }
        if (localSessions.isEmpty() && localEntries.isEmpty()) {
            return remoteSnapshot
        }

        val batch = firestore.batch()
        localSessions
            .filter { it.source == source }
            .forEach { session ->
                batch.set(
                    sessionsCollection(userId).document(session.id),
                    encodeSession(session),
                )
            }
        localEntries
            .filter { it.source == source }
            .forEach { entry ->
                batch.set(
                    entriesCollection(userId).document(entry.id),
                    encodeEntry(entry),
                )
            }
        batch.commit().await()
        return fetchSnapshot(userId, source)
    }

    suspend fun upsertSession(
        userId: String,
        session: AiConversationHistorySession,
    ) {
        sessionsCollection(userId)
            .document(session.id)
            .set(encodeSession(session))
            .await()
    }

    suspend fun upsertEntry(
        userId: String,
        entry: AiConversationHistoryEntry,
    ) {
        entriesCollection(userId)
            .document(entry.id)
            .set(encodeEntry(entry))
            .await()
    }

    suspend fun deleteSession(
        userId: String,
        sessionId: String,
    ) {
        val entryDocuments = entriesCollection(userId)
            .whereEqualTo("sessionId", sessionId)
            .get()
            .await()
            .documents

        val batch = firestore.batch()
        batch.delete(sessionsCollection(userId).document(sessionId))
        entryDocuments.forEach { document ->
            batch.delete(document.reference)
        }
        batch.commit().await()
    }

    suspend fun pruneHistory(
        userId: String,
        source: AiConversationHistorySource,
        retentionDays: Int,
    ) {
        val snapshot = fetchSnapshot(userId, source)
        if (snapshot.isEmpty) {
            return
        }

        val cutoffEpochMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        val keptSessions = snapshot.sessions
            .sortedByDescending { it.updatedAtEpochMillis }
            .take(maximumSessionsPerSource)
        val keptSessionIds = keptSessions.map { it.id }.toSet()

        val keptEntries = snapshot.entries
            .sortedByDescending { it.createdAtEpochMillis }
            .filter { entry ->
                entry.sessionId in keptSessionIds && entry.createdAtEpochMillis >= cutoffEpochMillis
            }
            .take(maximumEntriesPerSource)
        val keptEntryIds = keptEntries.map { it.id }.toSet()
        val keptSessionIdsWithEntries = keptEntries.map { it.sessionId }.toSet()
        val finalKeptSessionIds = keptSessions
            .filter { session ->
                session.id in keptSessionIdsWithEntries || session.updatedAtEpochMillis >= cutoffEpochMillis
            }
            .map { it.id }
            .toSet()

        val batch = firestore.batch()
        var hasChanges = false

        snapshot.entries
            .filter { it.id !in keptEntryIds }
            .forEach { entry ->
                batch.delete(entriesCollection(userId).document(entry.id))
                hasChanges = true
            }

        snapshot.sessions
            .filter { it.id !in finalKeptSessionIds }
            .forEach { session ->
                batch.delete(sessionsCollection(userId).document(session.id))
                hasChanges = true
            }

        if (hasChanges) {
            batch.commit().await()
        }
    }

    private fun sessionsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("aiSessions")

    private fun entriesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("aiEntries")

    private fun deliverAccumulatedSnapshot(
        accumulator: AiConversationRemoteSnapshotAccumulator,
        userId: String,
        source: AiConversationHistorySource,
        onChange: (Result<AiConversationRemoteSnapshot>) -> Unit,
    ) {
        val entries = accumulator.entries ?: return
        val sessions = accumulator.sessions ?: return
        onChange(
            Result.success(
                AiConversationRemoteSnapshot(
                    sessions = normalizeSessions(
                        sessions = sessions,
                        entries = entries,
                        userId = userId,
                        source = source,
                    ).sortedByDescending { it.updatedAtEpochMillis },
                    entries = entries.sortedByDescending { it.createdAtEpochMillis },
                ),
            ),
        )
    }

    private fun encodeSession(session: AiConversationHistorySession): Map<String, Any> {
        return mapOf(
            "source" to session.source.name.lowercase(),
            "title" to session.title,
            "createdAt" to Timestamp(session.createdAtEpochMillis / 1000, ((session.createdAtEpochMillis % 1000) * 1_000_000).toInt()),
            "updatedAt" to Timestamp(session.updatedAtEpochMillis / 1000, ((session.updatedAtEpochMillis % 1000) * 1_000_000).toInt()),
        )
    }

    private fun encodeEntry(entry: AiConversationHistoryEntry): Map<String, Any> {
        return mapOf(
            "sessionId" to entry.sessionId,
            "source" to entry.source.name.lowercase(),
            "prompt" to entry.prompt,
            "response" to entry.response,
            "resultType" to entry.resultType,
            "automationMessage" to entry.automationMessage,
            "workflowName" to entry.workflowName,
            "agentRunId" to entry.agentRunId,
            "results" to entry.structuredResults.map { result ->
                mapOf(
                    "type" to result.type,
                    "text" to result.text,
                    "url" to result.url,
                    "title" to result.title,
                    "mimeType" to result.mimeType,
                    "fileName" to result.fileName,
                    "html" to result.html,
                    "columns" to result.columns,
                    "rows" to result.rows,
                    "workflowName" to result.workflowName,
                    "status" to result.status,
                    "summary" to result.summary,
                    "runId" to result.runId,
                )
            },
            "createdAt" to Timestamp(entry.createdAtEpochMillis / 1000, ((entry.createdAtEpochMillis % 1000) * 1_000_000).toInt()),
        )
    }

    private fun decodeSession(
        data: Map<String, Any>,
        documentId: String,
        expectedSource: AiConversationHistorySource,
        userId: String,
    ): AiConversationHistorySession? {
        val normalizedSource = (data["source"] as? String)?.trim()?.lowercase().orEmpty()
        if (normalizedSource != expectedSource.name.lowercase()) {
            return null
        }
        val title = (data["title"] as? String).orEmpty().trim()
        val createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        val updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: createdAt
        return AiConversationHistorySession(
            id = documentId,
            userKey = userId.lowercase(),
            source = expectedSource,
            title = title.ifBlank { AiConversationHistorySession.DEFAULT_SESSION_TITLE },
            createdAtEpochMillis = createdAt,
            updatedAtEpochMillis = updatedAt,
        )
    }

    private fun decodeEntry(
        data: Map<String, Any>,
        documentId: String,
        expectedSource: AiConversationHistorySource,
        userId: String,
    ): AiConversationHistoryEntry? {
        val normalizedSource = (data["source"] as? String)?.trim()?.lowercase().orEmpty()
        if (normalizedSource != expectedSource.name.lowercase()) {
            return null
        }
        val sessionId = (data["sessionId"] as? String).orEmpty().trim()
        val prompt = (data["prompt"] as? String).orEmpty().trim()
        val response = (data["response"] as? String).orEmpty().trim()
        if (sessionId.isBlank() || prompt.isBlank() || response.isBlank()) {
            return null
        }
        val resultType = (data["resultType"] as? String).orEmpty().trim().ifBlank { "text" }
        val automationMessage = (data["automationMessage"] as? String).orEmpty().trim()
        val workflowName = (data["workflowName"] as? String).orEmpty().trim()
        val agentRunId = (data["agentRunId"] as? String).orEmpty().trim()
        val executionMode = (data["executionMode"] as? String).orEmpty().trim()
        val structuredResults = (data["results"] as? List<*>)?.mapNotNull { raw ->
            val row = raw as? Map<*, *> ?: return@mapNotNull null
            AiConversationHistoryResultEntry(
                type = (row["type"] as? String).orEmpty().trim().ifBlank { "text" },
                text = (row["text"] as? String).orEmpty(),
                url = (row["url"] as? String).orEmpty(),
                title = (row["title"] as? String).orEmpty(),
                mimeType = (row["mimeType"] as? String).orEmpty(),
                fileName = (row["fileName"] as? String).orEmpty(),
                html = (row["html"] as? String).orEmpty(),
                columns = (row["columns"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                rows = (row["rows"] as? List<*>)?.mapNotNull { list ->
                    (list as? List<*>)?.map { it?.toString().orEmpty() }
                }.orEmpty(),
                workflowName = (row["workflowName"] as? String).orEmpty(),
                status = (row["status"] as? String).orEmpty(),
                summary = (row["summary"] as? String).orEmpty(),
                runId = (row["runId"] as? String).orEmpty(),
            )
        }.orEmpty()
        return AiConversationHistoryEntry(
            id = documentId,
            sessionId = sessionId,
            userKey = userId.lowercase(),
            source = expectedSource,
            prompt = prompt,
            response = response,
            resultType = resultType,
            automationMessage = automationMessage,
            workflowName = workflowName,
            agentRunId = agentRunId,
            executionMode = executionMode,
            structuredResults = structuredResults,
            createdAtEpochMillis = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
        )
    }

    private fun normalizeSessions(
        sessions: List<AiConversationHistorySession>,
        entries: List<AiConversationHistoryEntry>,
        userId: String,
        source: AiConversationHistorySource,
    ): List<AiConversationHistorySession> {
        val knownSessions = sessions.associateBy { it.id }.toMutableMap()
        entries.groupBy { it.sessionId }.forEach { (sessionId, sessionEntries) ->
            if (knownSessions.containsKey(sessionId)) {
                return@forEach
            }
            val newest = sessionEntries.maxOfOrNull { it.createdAtEpochMillis } ?: System.currentTimeMillis()
            val oldest = sessionEntries.minOfOrNull { it.createdAtEpochMillis } ?: newest
            val previewTitle = suggestedSessionTitle(sessionEntries.lastOrNull()?.prompt.orEmpty())
            knownSessions[sessionId] = AiConversationHistorySession(
                id = sessionId,
                userKey = userId.lowercase(),
                source = source,
                title = previewTitle,
                createdAtEpochMillis = oldest,
                updatedAtEpochMillis = newest,
            )
        }
        return knownSessions.values.toList()
    }

    private fun suggestedSessionTitle(prompt: String): String {
        val collapsedPrompt = prompt.replace("\\s+".toRegex(), " ").trim()
        return collapsedPrompt.take(42).ifBlank { AiConversationHistorySession.DEFAULT_SESSION_TITLE }
    }
}

private class AiConversationRemoteSnapshotAccumulator {
    var sessions: List<AiConversationHistorySession>? = null
    var entries: List<AiConversationHistoryEntry>? = null
}

private class CompositeListenerRegistration(
    private val listeners: List<ListenerRegistration>,
) : ListenerRegistration {
    override fun remove() {
        listeners.forEach { it.remove() }
    }
}
