package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions

data class AiFaqReviewTriggerEntry(
    val triggerKey: String,
    val triggers: Int,
    val conversionRate: Double,
    val repeatRate: Double,
)

data class AiFaqReviewTopicEntry(
    val key: String,
    val value: Int,
    val share: Double,
)

data class AiFaqReviewRecommendationEntry(
    val id: String,
    val title: String,
    val summary: String,
    val actionType: String,
    val targetField: String,
    val suggestedValueLabel: String,
    val priority: String,
    val confidence: Double,
)

data class AiFaqReviewStrategyInsightEntry(
    val id: String,
    val title: String,
    val summary: String,
    val expectedImpact: String,
    val confidence: Double,
    val severity: String,
)

data class AiFaqOwnerReviewLoop(
    val strongestTriggers: List<AiFaqReviewTriggerEntry> = emptyList(),
    val weakTriggers: List<AiFaqReviewTriggerEntry> = emptyList(),
    val likelyUselessTriggers: List<AiFaqReviewTriggerEntry> = emptyList(),
    val repeatHeavyTopics: List<AiFaqReviewTopicEntry> = emptyList(),
    val recommendations: List<AiFaqReviewRecommendationEntry> = emptyList(),
    val strategyInsights: List<AiFaqReviewStrategyInsightEntry> = emptyList(),
)

data class AiFaqRecommendationActionResult(
    val status: String,
    val message: String,
    val metricsSnapshot: String = "",
)

class AiFaqOwnerReviewRepository {
    private val functions = FirebaseFunctions.getInstance("us-central1")

    suspend fun loadReviewLoop(windowDays: Int = 30): AiFaqOwnerReviewLoop {
        val result = functions.callWithAppCheckRetry(
            "getAiFaqOwnerIntelligence",
            mapOf("windowDays" to windowDays),
        )
        val payload = result.data.asStringKeyedAnyMap()
        val reviewLoop = payload["reviewLoop"].asStringKeyedAnyMap()
        return AiFaqOwnerReviewLoop(
            strongestTriggers = decodeTriggerEntries(reviewLoop["strongestTriggers"]),
            weakTriggers = decodeTriggerEntries(reviewLoop["weakTriggers"]),
            likelyUselessTriggers = decodeTriggerEntries(reviewLoop["likelyUselessTriggers"]),
            repeatHeavyTopics = decodeTopicEntries(reviewLoop["repeatHeavyTopics"]),
            recommendations = decodeRecommendationEntries(reviewLoop["recommendations"]),
            strategyInsights = decodeStrategyInsights(reviewLoop["strategyInsights"]),
        )
    }

    suspend fun previewRecommendation(
        recommendation: AiFaqReviewRecommendationEntry,
        windowDays: Int = 30,
    ): AiFaqRecommendationActionResult {
        val result = functions.callWithAppCheckRetry(
            "previewAiFaqReviewRecommendation",
            mapOf(
                "windowDays" to windowDays,
                "recommendation" to mapOf(
                    "id" to recommendation.id,
                    "actionType" to recommendation.actionType,
                    "targetField" to recommendation.targetField,
                    "suggestedValue" to recommendation.suggestedValueLabel,
                ),
            ),
        )
        val payload = result.data.asStringKeyedAnyMap()
        val allowed = payload["allowed"] as? Boolean ?: false
        val safeguards = payload["safeguards"] as? List<*> ?: emptyList<Any>()
        val safeguardsText = safeguards.joinToString(" | ") { it?.toString().orEmpty() }.trim()
        return if (allowed) {
            AiFaqRecommendationActionResult(
                status = "ok",
                message = if (safeguardsText.isBlank()) "Preview: sicher anwendbar." else "Preview: anwendbar mit Hinweisen: $safeguardsText",
                metricsSnapshot = extractMetricsSnapshot(payload),
            )
        } else {
            AiFaqRecommendationActionResult(
                status = "blocked",
                message = if (safeguardsText.isBlank()) "Preview blockiert." else "Preview blockiert: $safeguardsText",
                metricsSnapshot = extractMetricsSnapshot(payload),
            )
        }
    }

    suspend fun applyRecommendation(
        recommendationId: String,
        windowDays: Int = 30,
    ): AiFaqRecommendationActionResult {
        val result = functions.callWithAppCheckRetry(
            "applyAiFaqReviewRecommendation",
            mapOf(
                "windowDays" to windowDays,
                "recommendationId" to recommendationId,
            ),
        )
        val payload = result.data.asStringKeyedAnyMap()
        val status = payload["status"] as? String ?: "unknown"
        val safeguards = payload["safeguards"] as? List<*> ?: emptyList<Any>()
        val safeguardsText = safeguards.joinToString(" | ") { it?.toString().orEmpty() }.trim()
        val message = when (status) {
            "applied" -> if (safeguardsText.isBlank()) "Recommendation angewendet." else "Angewendet mit Hinweisen: $safeguardsText"
            "blocked" -> if (safeguardsText.isBlank()) "Apply blockiert." else "Apply blockiert: $safeguardsText"
            else -> "Apply ohne klares Ergebnis."
        }
        return AiFaqRecommendationActionResult(
            status = status,
            message = message,
            metricsSnapshot = extractMetricsSnapshot(payload),
        )
    }

    suspend fun revertLastChange(): AiFaqRecommendationActionResult {
        val result = functions.callWithAppCheckRetry(
            "revertLastAiFaqReviewChange",
            emptyMap<String, Any>(),
        )
        val payload = result.data.asStringKeyedAnyMap()
        val status = payload["status"] as? String ?: "unknown"
        val message = when (status) {
            "reverted" -> "Letzte Aenderung wurde revertiert."
            "noop" -> payload["message"] as? String ?: "Kein Revert verfuegbar."
            else -> "Revert ohne klares Ergebnis."
        }
        return AiFaqRecommendationActionResult(status = status, message = message)
    }

    private fun extractMetricsSnapshot(payload: Map<String, Any?>): String {
        val before = payload["before"].asStringKeyedAnyMap()
        val after = payload["after"].asStringKeyedAnyMap()
        val beforeMetrics = before["metrics"].asStringKeyedAnyMap()
        val afterMetrics = after["metrics"].asStringKeyedAnyMap()
        val beforeUpgrades = (beforeMetrics["hintAttributedUpgrades"] as? Number)?.toInt()
        val afterUpgrades = (afterMetrics["hintAttributedUpgrades"] as? Number)?.toInt()
        if (beforeUpgrades == null || afterUpgrades == null) return ""
        return "Before/After Snapshot: attributed upgrades $beforeUpgrades -> $afterUpgrades"
    }

    private fun decodeTriggerEntries(value: Any?): List<AiFaqReviewTriggerEntry> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { entry ->
            val map = entry.asStringKeyedAnyMap()
            val triggerKey = map["triggerKey"] as? String ?: return@mapNotNull null
            AiFaqReviewTriggerEntry(
                triggerKey = triggerKey,
                triggers = (map["triggers"] as? Number)?.toInt() ?: 0,
                conversionRate = (map["conversionRate"] as? Number)?.toDouble() ?: 0.0,
                repeatRate = (map["repeatRate"] as? Number)?.toDouble() ?: 0.0,
            )
        }
    }

    private fun decodeTopicEntries(value: Any?): List<AiFaqReviewTopicEntry> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { entry ->
            val map = entry.asStringKeyedAnyMap()
            val key = map["key"] as? String ?: return@mapNotNull null
            AiFaqReviewTopicEntry(
                key = key,
                value = (map["value"] as? Number)?.toInt() ?: 0,
                share = (map["share"] as? Number)?.toDouble() ?: 0.0,
            )
        }
    }

    private fun decodeRecommendationEntries(value: Any?): List<AiFaqReviewRecommendationEntry> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { entry ->
            val map = entry.asStringKeyedAnyMap()
            val id = map["id"] as? String ?: return@mapNotNull null
            AiFaqReviewRecommendationEntry(
                id = id,
                title = map["title"] as? String ?: id,
                summary = map["summary"] as? String ?: "",
                actionType = map["actionType"] as? String ?: "",
                targetField = map["targetField"] as? String ?: "",
                suggestedValueLabel = map["suggestedValue"]?.toString() ?: "",
                priority = map["priority"] as? String ?: "medium",
                confidence = (map["confidence"] as? Number)?.toDouble() ?: 0.0,
            )
        }
    }

    private fun decodeStrategyInsights(value: Any?): List<AiFaqReviewStrategyInsightEntry> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { entry ->
            val map = entry.asStringKeyedAnyMap()
            val id = map["id"] as? String ?: return@mapNotNull null
            AiFaqReviewStrategyInsightEntry(
                id = id,
                title = map["title"] as? String ?: id,
                summary = map["summary"] as? String ?: "",
                expectedImpact = map["expectedImpact"] as? String ?: "",
                confidence = (map["confidence"] as? Number)?.toDouble() ?: 0.0,
                severity = map["severity"] as? String ?: "medium",
            )
        }
    }
}

private fun Any?.asStringKeyedAnyMap(): Map<String, Any?> {
    val raw = this as? Map<*, *> ?: return emptyMap()
    return raw.entries.mapNotNull { (k, v) ->
        (k as? String)?.let { it to v }
    }.toMap()
}
