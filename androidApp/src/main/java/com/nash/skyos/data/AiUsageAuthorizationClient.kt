package com.nash.skyos.data

import com.google.firebase.functions.FirebaseFunctions
import com.nash.skyos.R

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

        val data = result.data as? Map<*, *> ?: error(AppTextResolver.string(R.string.ai_usage_auth_error_unreadable))
        return AiUsageAuthorizationResult(
            role = data["role"] as? String ?: error(AppTextResolver.string(R.string.ai_usage_auth_error_role_missing)),
            remainingForKind = (data["remainingForKind"] as? Number)?.toInt()
                ?: error(AppTextResolver.string(R.string.ai_usage_auth_error_remaining_missing)),
            limitForKind = (data["limitForKind"] as? Number)?.toInt()
                ?: error(AppTextResolver.string(R.string.ai_usage_auth_error_limit_missing)),
            historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt()
                ?: 3,
        )
    }
}
