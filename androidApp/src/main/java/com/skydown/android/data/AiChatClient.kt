package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions

data class AiGeneratedTextResult(
    val text: String,
    val historyRetentionDays: Int,
    val usage: AiUsageSnapshot? = null,
)

class AiChatClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun generateText(prompt: String, mode: String): AiGeneratedTextResult {
        if (!AppNetworkMonitor.isOnline.value) {
            error("Du bist offline. Der Bot ist wieder verfuegbar, sobald Internet da ist.")
        }

        val result = functions
            .callWithAppCheckRetry(
                functionName = "generateAiText",
                payload = mapOf(
                    "prompt" to prompt,
                    "mode" to mode,
                ),
            )

        val data = result.data as? Map<*, *> ?: error("Die Bot-Antwort konnte nicht gelesen werden.")
        val reply = data["reply"] as? String
        return AiGeneratedTextResult(
            text = reply?.takeIf { it.isNotBlank() } ?: error("Die Bot-Antwort fehlt."),
            historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt() ?: 3,
            usage = parseAiUsageSnapshot(data["usage"]),
        )
    }
}
