package com.skydown.android.data

data class AiUsageSnapshot(
    val remainingForKind: Int,
    val limitForKind: Int,
    val warningLevel: String,
    val userFacingReason: String,
    val suggestedUpgrade: String,
    val resetHint: String,
    val retryAfterSeconds: Int,
    val lowerCostOption: String,
)

data class AiBotDecision(
    val state: String,
    val route: String,
    val topic: String,
    val summary: String,
    val promptVersion: String,
    val qualityMode: String,
    val faqMode: String,
    val ownerMode: String,
    val answerLength: String,
    val personalityStyle: String,
    val loggingLevel: String,
    val diagnosticsMode: String,
    val ownerDiagnosticActive: Boolean,
    val selectedModel: String,
    val selectedProvider: String,
    val fallbackActivated: Boolean,
    val fallbackReason: String,
    val responseLimited: Boolean,
    val responseLimitReason: String,
    val blocked: Boolean,
    val blockReason: String,
    val retryable: Boolean,
    val retryReason: String,
    val trace: List<String>,
)

internal fun parseAiUsageSnapshot(payload: Any?): AiUsageSnapshot? {
    val usage = payload as? Map<*, *> ?: return null
    val hints = usage["guardrailHints"] as? Map<*, *>
    return AiUsageSnapshot(
        remainingForKind = (usage["remainingForKind"] as? Number)?.toInt() ?: 0,
        limitForKind = (usage["limitForKind"] as? Number)?.toInt() ?: 0,
        warningLevel = (usage["warningLevel"] as? String).orEmpty().lowercase().ifBlank { "ok" },
        userFacingReason = (hints?.get("userFacingReason") as? String).orEmpty(),
        suggestedUpgrade = (hints?.get("suggestedUpgrade") as? String).orEmpty(),
        resetHint = (hints?.get("resetHint") as? String).orEmpty(),
        retryAfterSeconds = (hints?.get("retryAfterSeconds") as? Number)?.toInt() ?: 0,
        lowerCostOption = (hints?.get("lowerCostOption") as? String).orEmpty(),
    )
}

internal fun parseAiBotDecision(payload: Any?): AiBotDecision? {
    val decision = payload as? Map<*, *> ?: return null
    return AiBotDecision(
        state = (decision["state"] as? String).orEmpty().ifBlank { "complete" },
        route = (decision["route"] as? String).orEmpty().ifBlank { "assistant" },
        topic = (decision["topic"] as? String).orEmpty(),
        summary = (decision["summary"] as? String).orEmpty(),
        promptVersion = (decision["promptVersion"] as? String).orEmpty(),
        qualityMode = (decision["qualityMode"] as? String).orEmpty(),
        faqMode = (decision["faqMode"] as? String).orEmpty(),
        ownerMode = (decision["ownerMode"] as? String).orEmpty(),
        answerLength = (decision["answerLength"] as? String).orEmpty(),
        personalityStyle = (decision["personalityStyle"] as? String).orEmpty(),
        loggingLevel = (decision["loggingLevel"] as? String).orEmpty(),
        diagnosticsMode = (decision["diagnosticsMode"] as? String).orEmpty(),
        ownerDiagnosticActive = decision["ownerDiagnosticActive"] as? Boolean ?: false,
        selectedModel = (decision["selectedModel"] as? String).orEmpty(),
        selectedProvider = (decision["selectedProvider"] as? String).orEmpty(),
        fallbackActivated = decision["fallbackActivated"] as? Boolean ?: false,
        fallbackReason = (decision["fallbackReason"] as? String).orEmpty(),
        responseLimited = decision["responseLimited"] as? Boolean ?: false,
        responseLimitReason = (decision["responseLimitReason"] as? String).orEmpty(),
        blocked = decision["blocked"] as? Boolean ?: false,
        blockReason = (decision["blockReason"] as? String).orEmpty(),
        retryable = decision["retryable"] as? Boolean ?: false,
        retryReason = (decision["retryReason"] as? String).orEmpty(),
        trace = (decision["trace"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
    )
}
