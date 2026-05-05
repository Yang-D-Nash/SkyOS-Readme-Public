package com.nash.skyos.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class AiConversationHistorySource {
    Bot,
    Agent,
}

data class AiConversationHistorySession(
    val id: String = UUID.randomUUID().toString(),
    val userKey: String,
    val source: AiConversationHistorySource,
    val title: String = DEFAULT_SESSION_TITLE,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
) {
    companion object {
        const val DEFAULT_SESSION_TITLE = "Neuer Chat"
    }
}

data class AiConversationHistorySessionSnapshot(
    val sessionId: String,
    val title: String,
    val preview: String,
    val promptCount: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class AiConversationHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val userKey: String,
    val source: AiConversationHistorySource,
    val prompt: String,
    val response: String,
    val imageFileName: String = "",
    val imageMimeType: String = "",
    val resultType: String = "text",
    val automationMessage: String = "",
    val workflowName: String = "",
    val agentRunId: String = "",
    val executionMode: String = "",
    val structuredResults: List<AiConversationHistoryResultEntry> = emptyList(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

data class AiConversationHistoryResultEntry(
    val type: String,
    val text: String = "",
    val url: String = "",
    val title: String = "",
    val mimeType: String = "",
    val fileName: String = "",
    val html: String = "",
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val workflowName: String = "",
    val status: String = "",
    val summary: String = "",
    val runId: String = "",
)

data class AiConversationHistorySaveResult(
    val session: AiConversationHistorySession,
    val entry: AiConversationHistoryEntry,
)

private data class LegacyHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val userKey: String,
    val source: AiConversationHistorySource,
    val prompt: String,
    val response: String,
    val createdAtEpochMillis: Long,
)

object AiConversationHistoryStore {
    private const val preferencesName = "ai_conversation_history"
    private const val sessionsKey = "sessions"
    private const val entriesKey = "entries"
    private const val retentionDaysKey = "retention_days"
    private const val visualImageDirectoryName = "ai_visual_history"
    private const val defaultRetentionDays = 3
    private const val maximumEntries = 360
    private const val maximumSessions = 40

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        if (::sharedPreferences.isInitialized) return
        appContext = context.applicationContext
        sharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        migrateLegacyEntriesIfNeeded()
        pruneExpiredEntries()
    }

    fun updateRetentionDays(days: Int) {
        val normalizedDays = when (days) {
            1, 3, 7, 30 -> days
            else -> defaultRetentionDays
        }
        sharedPreferences.edit().putInt(retentionDaysKey, normalizedDays).apply()
        pruneExpiredEntries()
    }

    fun sessionSnapshotsFor(
        userKey: String?,
        source: AiConversationHistorySource,
    ): List<AiConversationHistorySessionSnapshot> {
        val normalizedUserKey = normalizeUserKey(userKey)
        val sessionEntries = readEntries()
            .filter { entry ->
                entry.userKey == normalizedUserKey && entry.source == source
            }
            .groupBy { it.sessionId }

        return readSessions()
            .filter { session ->
                session.userKey == normalizedUserKey && session.source == source
            }
            .map { session ->
                val entries = sessionEntries[session.id].orEmpty().sortedBy { it.createdAtEpochMillis }
                val latestEntry = entries.lastOrNull()
                AiConversationHistorySessionSnapshot(
                    sessionId = session.id,
                    title = session.title.ifBlank { AiConversationHistorySession.DEFAULT_SESSION_TITLE },
                    preview = latestEntry?.response?.trim().takeUnless { it.isNullOrBlank() }
                        ?: latestEntry?.prompt?.trim().orEmpty(),
                    promptCount = entries.size,
                    createdAtEpochMillis = session.createdAtEpochMillis,
                    updatedAtEpochMillis = maxOf(
                        session.updatedAtEpochMillis,
                        latestEntry?.createdAtEpochMillis ?: session.updatedAtEpochMillis,
                    ),
                )
            }
            .sortedByDescending { it.updatedAtEpochMillis }
    }

    fun sessionsFor(
        userKey: String?,
        source: AiConversationHistorySource,
    ): List<AiConversationHistorySession> {
        val normalizedUserKey = normalizeUserKey(userKey)
        return readSessions()
            .filter { session ->
                session.userKey == normalizedUserKey && session.source == source
            }
            .sortedByDescending { it.updatedAtEpochMillis }
    }

    fun ensureSession(
        userKey: String?,
        source: AiConversationHistorySource,
        preferredSessionId: String? = null,
    ): AiConversationHistorySession {
        val normalizedUserKey = normalizeUserKey(userKey)
        val sessions = readSessions()
            .filter { session ->
                session.userKey == normalizedUserKey && session.source == source
            }

        preferredSessionId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { preferredId ->
                sessions.firstOrNull { it.id == preferredId }
            }
            ?.let { return it }

        sessions.maxByOrNull { it.updatedAtEpochMillis }?.let { return it }
        return createSession(userKey = normalizedUserKey, source = source)
    }

    fun createSession(
        userKey: String?,
        source: AiConversationHistorySource,
        title: String = AiConversationHistorySession.DEFAULT_SESSION_TITLE,
    ): AiConversationHistorySession {
        val session = AiConversationHistorySession(
            userKey = normalizeUserKey(userKey),
            source = source,
            title = title.trim().ifBlank { AiConversationHistorySession.DEFAULT_SESSION_TITLE },
        )
        val updatedSessions = buildList {
            add(session)
            addAll(readSessions())
        }
            .sortedByDescending { it.updatedAtEpochMillis }
            .take(maximumSessions)

        writeSessions(updatedSessions)
        pruneExpiredEntries()
        return session
    }

    fun renameSession(
        userKey: String?,
        source: AiConversationHistorySource,
        sessionId: String?,
        title: String,
    ): AiConversationHistorySession? {
        val normalizedUserKey = normalizeUserKey(userKey)
        val normalizedSessionId = sessionId?.trim().orEmpty()
        val normalizedTitle = title.trim().ifBlank { return null }
        var updatedSession: AiConversationHistorySession? = null
        val updatedSessions = readSessions().map { session ->
            if (
                session.id == normalizedSessionId &&
                session.userKey == normalizedUserKey &&
                session.source == source
            ) {
                session.copy(
                    title = normalizedTitle,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ).also { updatedSession = it }
            } else {
                session
            }
        }

        if (updatedSession == null) {
            return null
        }
        writeSessions(updatedSessions)
        return updatedSession
    }

    fun deleteSession(
        userKey: String?,
        source: AiConversationHistorySource,
        sessionId: String?,
    ) {
        val normalizedUserKey = normalizeUserKey(userKey)
        val normalizedSessionId = sessionId?.trim().orEmpty()
        val existingEntries = readEntries()
        val remainingSessions = readSessions().filterNot { session ->
            session.id == normalizedSessionId &&
                session.userKey == normalizedUserKey &&
                session.source == source
        }
        val removedEntries = existingEntries.filter { entry ->
            entry.sessionId == normalizedSessionId &&
                entry.userKey == normalizedUserKey &&
                entry.source == source
        }
        val removedEntryIds = removedEntries.map { it.id }.toSet()
        val remainingEntries = existingEntries.filterNot { entry ->
            entry.id in removedEntryIds
        }
        removeStoredImages(removedEntries)
        writeSessions(remainingSessions)
        writeEntries(remainingEntries)
    }

    fun entriesForSession(
        userKey: String?,
        source: AiConversationHistorySource,
        sessionId: String?,
    ): List<AiConversationHistoryEntry> {
        val normalizedUserKey = normalizeUserKey(userKey)
        val normalizedSessionId = sessionId?.trim().orEmpty()
        if (normalizedSessionId.isBlank()) {
            return emptyList()
        }
        return readEntries()
            .filter { entry ->
                entry.sessionId == normalizedSessionId &&
                    entry.userKey == normalizedUserKey &&
                    entry.source == source
            }
            .sortedBy { it.createdAtEpochMillis }
    }

    fun entriesFor(
        userKey: String?,
        source: AiConversationHistorySource,
    ): List<AiConversationHistoryEntry> {
        val normalizedUserKey = normalizeUserKey(userKey)
        return readEntries()
            .filter { entry ->
                entry.userKey == normalizedUserKey && entry.source == source
            }
            .sortedByDescending { it.createdAtEpochMillis }
    }

    fun saveEntry(
        userKey: String?,
        source: AiConversationHistorySource,
        sessionId: String?,
        prompt: String,
        response: String,
        imageBytes: ByteArray? = null,
        imageMimeType: String = "",
        resultType: String = "text",
        automationMessage: String = "",
        workflowName: String = "",
        agentRunId: String = "",
        executionMode: String = "",
        structuredResults: List<AiConversationHistoryResultEntry> = emptyList(),
    ): AiConversationHistorySaveResult? {
        val trimmedPrompt = prompt.trim()
        val trimmedResponse = response.trim()
        if (trimmedPrompt.isBlank() || trimmedResponse.isBlank()) {
            return null
        }

        val baseSession = ensureSession(
            userKey = userKey,
            source = source,
            preferredSessionId = sessionId,
        )
        val now = System.currentTimeMillis()
        val entryId = UUID.randomUUID().toString()
        val imageFileName = imageBytes
            ?.takeIf { it.isNotEmpty() }
            ?.let { storeImageBytes(it, entryId, imageMimeType) }
            .orEmpty()
        val normalizedImageMimeType = imageMimeType.trim()
            .ifBlank { if (imageFileName.isNotBlank()) "image/png" else "" }
        val normalizedResultType = when {
            imageFileName.isNotBlank() && resultType == "text" -> "image"
            else -> resultType
        }
        val updatedSession = baseSession.copy(
            title = resolvedSessionTitle(baseSession.title, trimmedPrompt),
            updatedAtEpochMillis = now,
        )

        val updatedSessions = buildList {
            add(updatedSession)
            addAll(
                readSessions().filterNot { existing ->
                    existing.id == updatedSession.id
                },
            )
        }
            .sortedByDescending { it.updatedAtEpochMillis }
            .take(maximumSessions)

        val savedEntry = AiConversationHistoryEntry(
            id = entryId,
            sessionId = updatedSession.id,
            userKey = updatedSession.userKey,
            source = source,
            prompt = trimmedPrompt,
            response = trimmedResponse,
            imageFileName = imageFileName,
            imageMimeType = normalizedImageMimeType,
            resultType = normalizedResultType,
            automationMessage = automationMessage,
            workflowName = workflowName,
            agentRunId = agentRunId,
            executionMode = executionMode.trim(),
            structuredResults = structuredResults,
            createdAtEpochMillis = now,
        )
        val updatedEntries = buildList {
            add(savedEntry)
            addAll(readEntries())
        }
            .sortedByDescending { it.createdAtEpochMillis }
            .take(maximumEntries)

        writeSessions(updatedSessions)
        writeEntries(updatedEntries)
        pruneExpiredEntries()
        return AiConversationHistorySaveResult(
            session = updatedSession,
            entry = savedEntry,
        )
    }

    fun imageBytesFor(entry: AiConversationHistoryEntry): ByteArray? {
        val fileName = sanitizeStoredImageFileName(entry.imageFileName)
        if (fileName.isBlank()) return null
        val file = storedImageFile(fileName, createDirectory = false) ?: return null
        return runCatching {
            file.takeIf { it.exists() && it.isFile }?.readBytes()
        }.getOrNull()
    }

    fun replaceRemoteState(
        userKey: String?,
        source: AiConversationHistorySource,
        sessions: List<AiConversationHistorySession>,
        entries: List<AiConversationHistoryEntry>,
    ) {
        val normalizedUserKey = normalizeUserKey(userKey)
        val normalizedSessions = sessions
            .filter { it.userKey == normalizedUserKey && it.source == source }
            .map { session ->
                session.copy(
                    userKey = normalizedUserKey,
                    source = source,
                    title = session.title.trim().ifBlank { AiConversationHistorySession.DEFAULT_SESSION_TITLE },
                )
            }
            .sortedByDescending { it.updatedAtEpochMillis }
        val normalizedEntries = entries
            .filter { it.userKey == normalizedUserKey && it.source == source }
            .map { entry ->
                entry.copy(
                    userKey = normalizedUserKey,
                    source = source,
                )
            }
            .sortedByDescending { it.createdAtEpochMillis }

        val existingForScope = readSessions().filter { it.userKey == normalizedUserKey && it.source == source }
        val existingEntriesForScope = readEntries().filter { it.userKey == normalizedUserKey && it.source == source }
        val existingImageDataByEntryId = existingEntriesForScope
            .filter { it.imageFileName.isNotBlank() }
            .associate { it.id to (it.imageFileName to it.imageMimeType) }
        val remoteSessionIds = normalizedSessions.map { it.id }.toSet()
        val remoteEntryIds = normalizedEntries.map { it.id }.toSet()
        // Firestore can deliver a snapshot before our own upsert lands; without this merge the UI
        // drops the new local row and the active thread repaints as empty.
        val maxLocalOrphanAgeMs = 20 * 60 * 1000L
        val now = System.currentTimeMillis()
        val localEntryOrphans = existingEntriesForScope.filter { local ->
            local.id !in remoteEntryIds && (now - local.createdAtEpochMillis) <= maxLocalOrphanAgeMs
        }
        val entryOrphanSessionIds = localEntryOrphans.map { it.sessionId }.toSet()
        val localSessionOrphans = existingForScope.filter { local ->
            local.id !in remoteSessionIds &&
                ((now - local.updatedAtEpochMillis) <= maxLocalOrphanAgeMs || local.id in entryOrphanSessionIds)
        }
        val mergedSessions = (normalizedSessions + localSessionOrphans)
            .sortedByDescending { it.updatedAtEpochMillis }
        val mergedEntries = (normalizedEntries + localEntryOrphans)
            .map { entry ->
                val imageData = existingImageDataByEntryId[entry.id]
                if (imageData != null && entry.imageFileName.isBlank()) {
                    entry.copy(imageFileName = imageData.first, imageMimeType = imageData.second)
                } else {
                    entry
                }
            }
            .sortedByDescending { it.createdAtEpochMillis }

        val updatedSessions = buildList {
            addAll(mergedSessions)
            addAll(
                readSessions().filterNot { session ->
                    session.userKey == normalizedUserKey && session.source == source
                },
            )
        }.sortedByDescending { it.updatedAtEpochMillis }

        val updatedEntries = buildList {
            addAll(mergedEntries)
            addAll(
                readEntries().filterNot { entry ->
                    entry.userKey == normalizedUserKey && entry.source == source
                },
            )
        }.sortedByDescending { it.createdAtEpochMillis }

        writeSessions(updatedSessions)
        val finalEntryIds = updatedEntries.map { it.id }.toSet()
        removeStoredImages(existingEntriesForScope.filter { it.id !in finalEntryIds })
        writeEntries(updatedEntries)
    }

    private fun migrateLegacyEntriesIfNeeded() {
        check(::sharedPreferences.isInitialized) {
            "AiConversationHistoryStore.initialize(context) must be called before migration."
        }
        if (sharedPreferences.contains(sessionsKey)) {
            return
        }

        val legacyEntries = readLegacyEntries().sortedBy { it.createdAtEpochMillis }
        if (legacyEntries.isEmpty()) {
            writeSessions(emptyList())
            return
        }

        val sessionsByGroup = linkedMapOf<Pair<String, AiConversationHistorySource>, AiConversationHistorySession>()
        val migratedEntries = legacyEntries.map { legacyEntry ->
            val groupingKey = legacyEntry.userKey to legacyEntry.source
            val existingSession = sessionsByGroup[groupingKey]
            val session = if (existingSession == null) {
                AiConversationHistorySession(
                    userKey = legacyEntry.userKey,
                    source = legacyEntry.source,
                    title = resolvedSessionTitle(
                        currentTitle = AiConversationHistorySession.DEFAULT_SESSION_TITLE,
                        prompt = legacyEntry.prompt,
                    ),
                    createdAtEpochMillis = legacyEntry.createdAtEpochMillis,
                    updatedAtEpochMillis = legacyEntry.createdAtEpochMillis,
                )
            } else {
                existingSession.copy(
                    updatedAtEpochMillis = maxOf(
                        existingSession.updatedAtEpochMillis,
                        legacyEntry.createdAtEpochMillis,
                    ),
                )
            }
            sessionsByGroup[groupingKey] = session
            AiConversationHistoryEntry(
                id = legacyEntry.id,
                sessionId = session.id,
                userKey = legacyEntry.userKey,
                source = legacyEntry.source,
                prompt = legacyEntry.prompt,
                response = legacyEntry.response,
                resultType = "text",
                automationMessage = "",
                workflowName = "",
                agentRunId = "",
                executionMode = "",
                structuredResults = emptyList(),
                createdAtEpochMillis = legacyEntry.createdAtEpochMillis,
            )
        }

        writeSessions(
            sessionsByGroup.values
                .sortedByDescending { it.updatedAtEpochMillis }
                .take(maximumSessions),
        )
        writeEntries(
            migratedEntries
                .sortedByDescending { it.createdAtEpochMillis }
                .take(maximumEntries),
        )
    }

    private fun pruneExpiredEntries() {
        val retentionDays = sharedPreferences.getInt(retentionDaysKey, defaultRetentionDays)
        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L
        val existingEntries = readEntries()
        val retainedEntries = existingEntries
            .filter { it.createdAtEpochMillis >= cutoff }
            .sortedByDescending { it.createdAtEpochMillis }
            .take(maximumEntries)
        val retainedSessionIds = retainedEntries.map { it.sessionId }.toSet()
        val retainedSessions = readSessions()
            .filter { session ->
                session.id in retainedSessionIds || session.updatedAtEpochMillis >= cutoff
            }
            .sortedByDescending { it.updatedAtEpochMillis }
            .take(maximumSessions)

        val finalEntries = retainedEntries.filter { entry ->
            retainedSessions.any { session -> session.id == entry.sessionId }
        }
        val finalEntryIds = finalEntries.map { it.id }.toSet()
        removeStoredImages(existingEntries.filter { it.id !in finalEntryIds })
        writeEntries(finalEntries)
        writeSessions(retainedSessions)
    }

    private fun readSessions(): List<AiConversationHistorySession> {
        check(::sharedPreferences.isInitialized) {
            "AiConversationHistoryStore.initialize(context) must be called before reading sessions."
        }

        val raw = sharedPreferences.getString(sessionsKey, null).orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val source = item.optString("source")
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { AiConversationHistorySource.valueOf(it) }.getOrNull() }
                        ?: AiConversationHistorySource.Bot
                    add(
                        AiConversationHistorySession(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            userKey = normalizeUserKey(item.optString("userKey")),
                            source = source,
                            title = item.optString("title")
                                .trim()
                                .ifBlank { AiConversationHistorySession.DEFAULT_SESSION_TITLE },
                            createdAtEpochMillis = item.optLong(
                                "createdAtEpochMillis",
                                System.currentTimeMillis(),
                            ),
                            updatedAtEpochMillis = item.optLong(
                                "updatedAtEpochMillis",
                                System.currentTimeMillis(),
                            ),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun readEntries(): List<AiConversationHistoryEntry> {
        check(::sharedPreferences.isInitialized) {
            "AiConversationHistoryStore.initialize(context) must be called before reading history."
        }

        val raw = sharedPreferences.getString(entriesKey, null).orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val prompt = item.optString("prompt").trim()
                    val response = item.optString("response").trim()
                    val sessionId = item.optString("sessionId").trim()
                    val source = item.optString("source")
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { AiConversationHistorySource.valueOf(it) }.getOrNull() }
                        ?: AiConversationHistorySource.Bot

                    if (prompt.isBlank() || response.isBlank() || sessionId.isBlank()) {
                        continue
                    }

                    add(
                        AiConversationHistoryEntry(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            sessionId = sessionId,
                            userKey = normalizeUserKey(item.optString("userKey")),
                            source = source,
                            prompt = prompt,
                            response = response,
                            imageFileName = sanitizeStoredImageFileName(item.optString("imageFileName")),
                            imageMimeType = item.optString("imageMimeType").trim(),
                            resultType = item.optString("resultType").trim().ifBlank { "text" },
                            automationMessage = item.optString("automationMessage").trim(),
                            workflowName = item.optString("workflowName").trim(),
                            agentRunId = item.optString("agentRunId").trim(),
                            executionMode = item.optString("executionMode").trim(),
                            structuredResults = item.optJSONArray("results").toHistoryResults(),
                            createdAtEpochMillis = item.optLong(
                                "createdAtEpochMillis",
                                System.currentTimeMillis(),
                            ),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun readLegacyEntries(): List<LegacyHistoryEntry> {
        check(::sharedPreferences.isInitialized) {
            "AiConversationHistoryStore.initialize(context) must be called before reading legacy history."
        }

        val raw = sharedPreferences.getString(entriesKey, null).orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val prompt = item.optString("prompt").trim()
                    val response = item.optString("response").trim()
                    val sessionId = item.optString("sessionId").trim()
                    if (prompt.isBlank() || response.isBlank() || sessionId.isNotBlank()) {
                        continue
                    }
                    val source = item.optString("source")
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { AiConversationHistorySource.valueOf(it) }.getOrNull() }
                        ?: AiConversationHistorySource.Bot
                    add(
                        LegacyHistoryEntry(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            userKey = normalizeUserKey(item.optString("userKey")),
                            source = source,
                            prompt = prompt,
                            response = response,
                            createdAtEpochMillis = item.optLong(
                                "createdAtEpochMillis",
                                System.currentTimeMillis(),
                            ),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeSessions(sessions: List<AiConversationHistorySession>) {
        check(::sharedPreferences.isInitialized) {
            "AiConversationHistoryStore.initialize(context) must be called before writing sessions."
        }

        val array = JSONArray()
        sessions.forEach { session ->
            array.put(
                JSONObject()
                    .put("id", session.id)
                    .put("userKey", session.userKey)
                    .put("source", session.source.name)
                    .put("title", session.title)
                    .put("createdAtEpochMillis", session.createdAtEpochMillis)
                    .put("updatedAtEpochMillis", session.updatedAtEpochMillis),
            )
        }
        sharedPreferences.edit().putString(sessionsKey, array.toString()).apply()
    }

    private fun writeEntries(entries: List<AiConversationHistoryEntry>) {
        check(::sharedPreferences.isInitialized) {
            "AiConversationHistoryStore.initialize(context) must be called before writing history."
        }

        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("sessionId", entry.sessionId)
                    .put("userKey", entry.userKey)
                    .put("source", entry.source.name)
                    .put("prompt", entry.prompt)
                    .put("response", entry.response)
                    .put("imageFileName", sanitizeStoredImageFileName(entry.imageFileName))
                    .put("imageMimeType", entry.imageMimeType)
                    .put("resultType", entry.resultType)
                    .put("automationMessage", entry.automationMessage)
                    .put("workflowName", entry.workflowName)
                    .put("agentRunId", entry.agentRunId)
                    .put("executionMode", entry.executionMode)
                    .put("results", entry.structuredResults.toJsonArray())
                    .put("createdAtEpochMillis", entry.createdAtEpochMillis),
            )
        }

        sharedPreferences.edit().putString(entriesKey, array.toString()).apply()
    }

    private fun resolvedSessionTitle(currentTitle: String, prompt: String): String {
        return if (isDefaultSessionTitle(currentTitle)) {
            prompt.lineSequence()
                .firstOrNull()
                .orEmpty()
                .trim()
                .take(42)
                .ifBlank { AiConversationHistorySession.DEFAULT_SESSION_TITLE }
        } else {
            currentTitle.trim().ifBlank { AiConversationHistorySession.DEFAULT_SESSION_TITLE }
        }
    }

    private fun isDefaultSessionTitle(title: String): Boolean {
        val normalized = title.trim()
        return normalized.isBlank() ||
            normalized.equals(AiConversationHistorySession.DEFAULT_SESSION_TITLE, ignoreCase = true)
    }

    private fun storeImageBytes(imageBytes: ByteArray, entryId: String, mimeType: String): String? {
        val extension = imageExtensionFor(mimeType)
        val fileName = sanitizeStoredImageFileName("$entryId.$extension")
        if (fileName.isBlank()) return null
        val file = storedImageFile(fileName, createDirectory = true) ?: return null
        return runCatching {
            file.writeBytes(imageBytes)
            fileName
        }.getOrNull()
    }

    private fun storedImageFile(fileName: String, createDirectory: Boolean): File? {
        if (!::appContext.isInitialized) return null
        val sanitizedFileName = sanitizeStoredImageFileName(fileName)
        if (sanitizedFileName.isBlank()) return null
        val directory = File(appContext.filesDir, visualImageDirectoryName)
        if (createDirectory && !directory.exists() && !directory.mkdirs()) return null
        return File(directory, sanitizedFileName)
    }

    private fun removeStoredImages(entries: List<AiConversationHistoryEntry>) {
        entries.forEach { entry ->
            val file = storedImageFile(entry.imageFileName, createDirectory = false)
            runCatching { file?.delete() }
        }
    }

    private fun sanitizeStoredImageFileName(fileName: String): String =
        fileName.trim().substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "")
            .take(96)

    private fun imageExtensionFor(mimeType: String): String =
        when (mimeType.trim().lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/png" -> "png"
            else -> "png"
        }

    private fun normalizeUserKey(userKey: String?): String {
        val trimmed = userKey?.trim().orEmpty()
        return trimmed.ifBlank { "guest" }.lowercase()
    }
}

private fun JSONArray?.toHistoryResults(): List<AiConversationHistoryResultEntry> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val type = item.optString("type").trim().ifBlank { "text" }
            val rows = item.optJSONArray("rows").toStringGrid()
            val columns = item.optJSONArray("columns").toStringList()
            add(
                AiConversationHistoryResultEntry(
                    type = type,
                    text = item.optString("text").trim(),
                    url = item.optString("url").trim(),
                    title = item.optString("title").trim(),
                    mimeType = item.optString("mimeType").trim(),
                    fileName = item.optString("fileName").trim(),
                    html = item.optString("html").trim(),
                    columns = columns,
                    rows = rows,
                    workflowName = item.optString("workflowName").trim(),
                    status = item.optString("status").trim(),
                    summary = item.optString("summary").trim(),
                    runId = item.optString("runId").trim(),
                ),
            )
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val value = optString(index).trim()
            if (value.isNotBlank()) add(value)
        }
    }
}

private fun JSONArray?.toStringGrid(): List<List<String>> {
    if (this == null) return emptyList()
    return buildList {
        for (rowIndex in 0 until length()) {
            val rowArray = optJSONArray(rowIndex)
            if (rowArray != null) {
                add(rowArray.toStringList())
            } else {
                val rowValue = optString(rowIndex).trim()
                if (rowValue.isNotBlank()) add(listOf(rowValue))
            }
        }
    }
}

private fun List<AiConversationHistoryResultEntry>.toJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { result ->
        val rows = JSONArray().apply {
            result.rows.forEach { row ->
                put(JSONArray().apply { row.forEach(::put) })
            }
        }
        val columns = JSONArray().apply { result.columns.forEach(::put) }
        array.put(
            JSONObject()
                .put("type", result.type)
                .put("text", result.text)
                .put("url", result.url)
                .put("title", result.title)
                .put("mimeType", result.mimeType)
                .put("fileName", result.fileName)
                .put("html", result.html)
                .put("columns", columns)
                .put("rows", rows)
                .put("workflowName", result.workflowName)
                .put("status", result.status)
                .put("summary", result.summary)
                .put("runId", result.runId),
        )
    }
    return array
}
