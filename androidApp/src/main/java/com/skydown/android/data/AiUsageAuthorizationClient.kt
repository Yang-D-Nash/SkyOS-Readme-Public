package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions

enum class AiUsageAuthorizationKind(val rawValue: String) {
    Text("text"),
    Visual("visual"),
    Agent("agent"),
}

data class AiUsageAuthorizationResult(
    val role: String,
    val remainingForKind: Int,
    val limitForKind: Int,
    val historyRetentionDays: Int,
)

class AiUsageAuthorizationClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun authorize(kind: AiUsageAuthorizationKind): AiUsageAuthorizationResult {
        val payload = mapOf("kind" to kind.rawValue)
        val result = functions
            .callWithAppCheckRetry(
                functionName = "authorizeAiUsage",
                payload = payload,
            )

        val data = result.data as? Map<*, *> ?: error("Die KI-Kostenkontrolle konnte nicht gelesen werden.")
        return AiUsageAuthorizationResult(
            role = data["role"] as? String ?: error("Rolle fehlt in der KI-Kontrolle."),
            remainingForKind = (data["remainingForKind"] as? Number)?.toInt()
                ?: error("Restlimit fehlt in der KI-Kontrolle."),
            limitForKind = (data["limitForKind"] as? Number)?.toInt()
                ?: error("Limit fehlt in der KI-Kontrolle."),
            historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt()
                ?: 3,
        )
    }
}
