package com.nash.skyos.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
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
        return AiConversationHistoryEntry(
            id = documentId,
            sessionId = sessionId,
            userKey = userId.lowercase(),
            source = expectedSource,
            prompt = prompt,
            response = response,
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
