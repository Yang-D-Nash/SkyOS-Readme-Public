package com.nash.skyos.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AgentPendingQueueEntry(
    val userKey: String,
    val sessionId: String,
    val prompt: String,
    val history: List<AgentHistoryTurn>,
    val mode: String,
    val aiLevel: String = "standard",
    val executeAutomation: Boolean,
    val automationScope: String = "owner",
    val assistantMessageId: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val attachments: List<AgentOutboundAttachment> = emptyList(),
    val idempotencyKey: String = "",
)

object AgentPendingQueueStore {
    private const val preferencesName = "agent_pending_queue"
    private const val entriesKey = "entries"
    private const val maximumEntries = 80
    private const val maximumAgeMillis = 14L * 24L * 60L * 60L * 1000L

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        if (::sharedPreferences.isInitialized) {
            return
        }
        sharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        pruneExpiredEntries()
    }

    fun entriesFor(userKey: String?, sessionId: String?): List<AgentPendingQueueEntry> {
        val normalized = normalizeUserKey(userKey)
        val normalizedSessionId = normalizeSessionId(sessionId)
        return readEntries()
            .filter { it.userKey == normalized && it.sessionId == normalizedSessionId }
            .sortedBy { it.createdAtEpochMillis }
    }

    fun saveEntriesForSession(userKey: String?, sessionId: String?, entries: List<AgentPendingQueueEntry>) {
        val normalized = normalizeUserKey(userKey)
        val normalizedSessionId = normalizeSessionId(sessionId)
        val others = readEntries().filterNot { it.userKey == normalized && it.sessionId == normalizedSessionId }
        val sanitized = entries
            .filter { it.prompt.isNotBlank() && it.assistantMessageId.isNotBlank() }
            .sortedBy { it.createdAtEpochMillis }
            .map { entry ->
                entry.copy(
                    userKey = normalized,
                    sessionId = normalizedSessionId,
                    assistantMessageId = entry.assistantMessageId.ifBlank { UUID.randomUUID().toString() },
                )
            }

        val merged = (others + sanitized)
            .sortedByDescending { it.createdAtEpochMillis }
            .take(maximumEntries)
        writeEntries(merged)
        pruneExpiredEntries()
    }

    fun clearEntriesForSession(userKey: String?, sessionId: String?) {
        saveEntriesForSession(userKey, sessionId, emptyList())
    }

    private fun pruneExpiredEntries() {
        val cutoff = System.currentTimeMillis() - maximumAgeMillis
        val filtered = readEntries().filter { it.createdAtEpochMillis >= cutoff }
        writeEntries(filtered)
    }

    private fun readEntries(): List<AgentPendingQueueEntry> {
        check(::sharedPreferences.isInitialized) {
            "AgentPendingQueueStore.initialize(context) must be called before reading."
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
                    if (prompt.isBlank()) continue

                    val assistantMessageId = item.optString("assistantMessageId")
                        .trim()
                        .ifBlank { UUID.randomUUID().toString() }
                    val history = parseHistory(item.optJSONArray("history"))

                    add(
                        AgentPendingQueueEntry(
                            userKey = normalizeUserKey(item.optString("userKey")),
                            sessionId = normalizeSessionId(item.optString("sessionId")),
                            prompt = prompt,
                            history = history,
                            mode = item.optString("mode").trim().ifBlank { "release" },
                            aiLevel = item.optString("aiLevel").trim().ifBlank { "standard" },
                            executeAutomation = item.optBoolean("executeAutomation", false),
                            automationScope = item.optString("automationScope").trim().ifBlank { "owner" },
                            assistantMessageId = assistantMessageId,
                            createdAtEpochMillis = item.optLong("createdAtEpochMillis", System.currentTimeMillis()),
                            attachments = parseAttachments(item.optJSONArray("attachments")),
                            idempotencyKey = item.optString("idempotencyKey").trim(),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun parseAttachments(array: JSONArray?): List<AgentOutboundAttachment> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val o = array.optJSONObject(index) ?: continue
                val name = o.optString("name").trim()
                if (name.isBlank()) continue
                val b64 = o.optString("inlineBase64").trim()
                if (b64.isBlank()) continue
                add(
                    AgentOutboundAttachment(
                        id = o.optString("id").trim().ifBlank { "att_${size + 1}" },
                        name = name,
                        kind = o.optString("kind").trim().ifBlank { "file" },
                        mimeType = o.optString("mimeType").trim().ifBlank { "application/octet-stream" },
                        source = o.optString("source").trim().ifBlank { "inline" },
                        inlineBase64 = b64,
                    ),
                )
            }
        }
    }

    private fun parseHistory(array: JSONArray?): List<AgentHistoryTurn> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val role = item.optString("role").trim()
                val text = item.optString("text").trim()
                if (role.isBlank() || text.isBlank()) {
                    continue
                }
                add(AgentHistoryTurn(role = role, text = text))
            }
        }
    }

    private fun writeEntries(entries: List<AgentPendingQueueEntry>) {
        check(::sharedPreferences.isInitialized) {
            "AgentPendingQueueStore.initialize(context) must be called before writing."
        }
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("userKey", normalizeUserKey(entry.userKey))
                    .put("sessionId", normalizeSessionId(entry.sessionId))
                    .put("prompt", entry.prompt)
                    .put("mode", entry.mode)
                    .put("aiLevel", entry.aiLevel.ifBlank { "standard" })
                    .put("executeAutomation", entry.executeAutomation)
                    .put("automationScope", entry.automationScope.ifBlank { "owner" })
                    .put("assistantMessageId", entry.assistantMessageId)
                    .put("createdAtEpochMillis", entry.createdAtEpochMillis)
                    .put("idempotencyKey", entry.idempotencyKey)
                    .put(
                        "history",
                        JSONArray().apply {
                            entry.history.forEach { turn ->
                                put(
                                    JSONObject()
                                        .put("role", turn.role)
                                        .put("text", turn.text),
                                )
                            }
                        },
                    )
                    .put(
                        "attachments",
                        JSONArray().apply {
                            entry.attachments.forEach { att ->
                                put(
                                    JSONObject()
                                        .put("id", att.id)
                                        .put("name", att.name)
                                        .put("kind", att.kind)
                                        .put("mimeType", att.mimeType)
                                        .put("source", att.source)
                                        .put("inlineBase64", att.inlineBase64),
                                )
                            }
                        },
                    ),
            )
        }
        sharedPreferences.edit().putString(entriesKey, array.toString()).apply()
    }

    private fun normalizeUserKey(userKey: String?): String {
        val trimmed = userKey?.trim().orEmpty()
        return trimmed.ifBlank { "guest" }.lowercase()
    }

    private fun normalizeSessionId(sessionId: String?): String {
        return sessionId?.trim().orEmpty()
    }
}
