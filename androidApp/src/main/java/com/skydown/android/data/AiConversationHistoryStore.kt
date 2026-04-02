package com.skydown.android.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AiConversationHistorySource {
    Bot,
    Agent,
}

data class AiConversationHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val userKey: String,
    val source: AiConversationHistorySource,
    val prompt: String,
    val response: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

object AiConversationHistoryStore {
    private const val preferencesName = "ai_conversation_history"
    private const val entriesKey = "entries"
    private const val retentionDaysKey = "retention_days"
    private const val defaultRetentionDays = 3
    private const val maximumEntries = 120

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        if (::sharedPreferences.isInitialized) return
        sharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
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

    fun entriesFor(userKey: String?, source: AiConversationHistorySource): List<AiConversationHistoryEntry> {
        return readEntries()
            .filter { entry ->
                entry.userKey == normalizeUserKey(userKey) && entry.source == source
            }
            .sortedByDescending { it.createdAtEpochMillis }
    }

    fun saveEntry(
        userKey: String?,
        source: AiConversationHistorySource,
        prompt: String,
        response: String,
    ) {
        val trimmedPrompt = prompt.trim()
        val trimmedResponse = response.trim()
        if (trimmedPrompt.isBlank() || trimmedResponse.isBlank()) {
            return
        }

        val updatedEntries = buildList {
            add(
                AiConversationHistoryEntry(
                    userKey = normalizeUserKey(userKey),
                    source = source,
                    prompt = trimmedPrompt,
                    response = trimmedResponse,
                ),
            )
            addAll(readEntries())
        }.take(maximumEntries)

        writeEntries(updatedEntries)
        pruneExpiredEntries()
    }

    fun clearEntries(userKey: String?, source: AiConversationHistorySource) {
        val normalizedUserKey = normalizeUserKey(userKey)
        val filteredEntries = readEntries().filterNot { entry ->
            entry.userKey == normalizedUserKey && entry.source == source
        }
        writeEntries(filteredEntries)
    }

    private fun pruneExpiredEntries() {
        val retentionDays = sharedPreferences.getInt(retentionDaysKey, defaultRetentionDays)
        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L
        val prunedEntries = readEntries()
            .filter { it.createdAtEpochMillis >= cutoff }
            .sortedByDescending { it.createdAtEpochMillis }
        writeEntries(prunedEntries)
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
                    val userKey = item.optString("userKey").trim().ifBlank { "guest" }
                    val prompt = item.optString("prompt").trim()
                    val response = item.optString("response").trim()
                    val source = item.optString("source")
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { AiConversationHistorySource.valueOf(it) }.getOrNull() }
                        ?: AiConversationHistorySource.Bot

                    if (prompt.isBlank() || response.isBlank()) {
                        continue
                    }

                    add(
                        AiConversationHistoryEntry(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            userKey = userKey,
                            source = source,
                            prompt = prompt,
                            response = response,
                            createdAtEpochMillis = item.optLong("createdAtEpochMillis", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
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
                    .put("userKey", entry.userKey)
                    .put("source", entry.source.name)
                    .put("prompt", entry.prompt)
                    .put("response", entry.response)
                    .put("createdAtEpochMillis", entry.createdAtEpochMillis),
            )
        }

        sharedPreferences.edit().putString(entriesKey, array.toString()).apply()
    }

    private fun normalizeUserKey(userKey: String?): String {
        val trimmed = userKey?.trim().orEmpty()
        return trimmed.ifBlank { "guest" }.lowercase()
    }
}
