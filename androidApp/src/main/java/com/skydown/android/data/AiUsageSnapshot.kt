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
