package com.nash.skyos.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
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
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
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
    private const val defaultRetentionDays = 3
    private const val maximumEntries = 360
    private const val maximumSessions = 40

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        if (::sharedPreferences.isInitialized) return
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
        val remainingSessions = readSessions().filterNot { session ->
            session.id == normalizedSessionId &&
                session.userKey == normalizedUserKey &&
                session.source == source
        }
        val remainingEntries = readEntries().filterNot { entry ->
            entry.sessionId == normalizedSessionId &&
                entry.userKey == normalizedUserKey &&
                entry.source == source
        }
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
            sessionId = updatedSession.id,
            userKey = updatedSession.userKey,
            source = source,
            prompt = trimmedPrompt,
            response = trimmedResponse,
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

        val updatedSessions = buildList {
            addAll(normalizedSessions)
            addAll(
                readSessions().filterNot { session ->
                    session.userKey == normalizedUserKey && session.source == source
                },
            )
        }.sortedByDescending { it.updatedAtEpochMillis }

        val updatedEntries = buildList {
            addAll(normalizedEntries)
            addAll(
                readEntries().filterNot { entry ->
                    entry.userKey == normalizedUserKey && entry.source == source
                },
            )
        }.sortedByDescending { it.createdAtEpochMillis }

        writeSessions(updatedSessions)
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
        val retainedEntries = readEntries()
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

        writeEntries(
            retainedEntries.filter { entry ->
                retainedSessions.any { session -> session.id == entry.sessionId }
            },
        )
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

    private fun normalizeUserKey(userKey: String?): String {
        val trimmed = userKey?.trim().orEmpty()
        return trimmed.ifBlank { "guest" }.lowercase()
    }
}
