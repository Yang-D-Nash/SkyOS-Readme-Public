package com.skydown.android.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AgentPendingQueueEntry(
    val userKey: String,
    val prompt: String,
    val history: List<AgentHistoryTurn>,
    val mode: String,
    val aiLevel: String = "standard",
    val executeAutomation: Boolean,
    val assistantMessageId: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
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

    fun entriesFor(userKey: String?): List<AgentPendingQueueEntry> {
        val normalized = normalizeUserKey(userKey)
        return readEntries()
            .filter { it.userKey == normalized }
            .sortedBy { it.createdAtEpochMillis }
    }

    fun saveEntriesForUser(userKey: String?, entries: List<AgentPendingQueueEntry>) {
        val normalized = normalizeUserKey(userKey)
        val others = readEntries().filterNot { it.userKey == normalized }
        val sanitized = entries
            .filter { it.prompt.isNotBlank() && it.assistantMessageId.isNotBlank() }
            .sortedBy { it.createdAtEpochMillis }
            .map { entry ->
                entry.copy(
                    userKey = normalized,
                    assistantMessageId = entry.assistantMessageId.ifBlank { UUID.randomUUID().toString() },
                )
            }

        val merged = (others + sanitized)
            .sortedByDescending { it.createdAtEpochMillis }
            .take(maximumEntries)
        writeEntries(merged)
        pruneExpiredEntries()
    }

    fun clearEntriesForUser(userKey: String?) {
        saveEntriesForUser(userKey, emptyList())
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
                            prompt = prompt,
                            history = history,
                            mode = item.optString("mode").trim().ifBlank { "release" },
                            aiLevel = item.optString("aiLevel").trim().ifBlank { "standard" },
                            executeAutomation = item.optBoolean("executeAutomation", false),
                            assistantMessageId = assistantMessageId,
                            createdAtEpochMillis = item.optLong("createdAtEpochMillis", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
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
                    .put("prompt", entry.prompt)
                    .put("mode", entry.mode)
                    .put("aiLevel", entry.aiLevel.ifBlank { "standard" })
                    .put("executeAutomation", entry.executeAutomation)
                    .put("assistantMessageId", entry.assistantMessageId)
                    .put("createdAtEpochMillis", entry.createdAtEpochMillis)
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
                    ),
            )
        }
        sharedPreferences.edit().putString(entriesKey, array.toString()).apply()
    }

    private fun normalizeUserKey(userKey: String?): String {
        val trimmed = userKey?.trim().orEmpty()
        return trimmed.ifBlank { "guest" }.lowercase()
    }
}
