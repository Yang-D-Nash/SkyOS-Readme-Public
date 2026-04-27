package com.nash.skyos.data

import com.google.firebase.functions.FirebaseFunctions
import com.nash.skyos.R

data class AiGeneratedTextResult(
    val text: String,
    val historyRetentionDays: Int,
    val usage: AiUsageSnapshot? = null,
    val decision: AiBotDecision? = null,
)

class AiChatClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun generateText(prompt: String, mode: String, aiLevel: String): AiGeneratedTextResult {
        if (!AppNetworkMonitor.isOnline.value) {
            error(AppTextResolver.string(R.string.ai_chat_error_offline))
        }

        val result = functions
            .callWithAppCheckRetry(
                functionName = "generateAiText",
                payload = mapOf(
                    "prompt" to prompt,
                    "mode" to mode,
                    "aiLevel" to aiLevel,
                ),
            )

        val data = result.data as? Map<*, *> ?: error(AppTextResolver.string(R.string.ai_chat_error_response_unreadable))
        val reply = data["reply"] as? String
        return AiGeneratedTextResult(
            text = reply?.takeIf { it.isNotBlank() } ?: error(AppTextResolver.string(R.string.ai_chat_error_response_missing)),
            historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt() ?: 3,
            usage = parseAiUsageSnapshot(data["usage"]),
            decision = parseAiBotDecision(data["botDecision"]),
        )
    }
}
