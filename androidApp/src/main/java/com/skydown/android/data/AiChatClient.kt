package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

data class AiGeneratedTextResult(
    val text: String,
    val historyRetentionDays: Int,
)

class AiChatClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun generateText(prompt: String): AiGeneratedTextResult {
        val result = functions
            .getHttpsCallable("generateAiText")
            .call(mapOf("prompt" to prompt))
            .await()

        val data = result.data as? Map<*, *> ?: error("Die Bot-Antwort konnte nicht gelesen werden.")
        val reply = data["reply"] as? String
        return AiGeneratedTextResult(
            text = reply?.takeIf { it.isNotBlank() } ?: error("Die Bot-Antwort fehlt."),
            historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt() ?: 3,
        )
    }
}
