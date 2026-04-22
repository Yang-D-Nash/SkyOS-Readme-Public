package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions

data class MembershipOpsRecommendation(
    val id: String,
    val title: String,
    val summary: String,
    val recommendationType: String,
    val confidenceScore: Double,
    val severity: String,
    val priorityScore: Int = 0,
)

data class MembershipOpsExperimentDraft(
    val lifecycleId: String,
    val recommendationId: String,
    val recommendationType: String,
    val notes: String,
)

class MembershipOpsAdminRepository {
    private val functions = FirebaseFunctions.getInstance("us-central1")

    suspend fun loadDashboard(): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry("getAiMembershipDashboard", emptyMap<String, Any>())
        return result.data.asStringKeyedAnyMap()
    }

    suspend fun loadTimeseries(windowDays: Int = 30): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry(
            "getAiMembershipDashboardTimeseries",
            mapOf("windowDays" to windowDays),
        )
        return result.data.asStringKeyedAnyMap()
    }

    suspend fun loadRecommendations(): List<MembershipOpsRecommendation> {
        val result = functions.callWithAppCheckRetry(
            "getAiMembershipTrendRecommendations",
            emptyMap<String, Any>(),
        )
        val data = result.data.asStringKeyedAnyMap()
        val list = data["recommendations"] as? List<*> ?: emptyList<Any>()
        return list.mapNotNull { entry ->
            val map = entry.asStringKeyedAnyMap()
            val id = map["id"] as? String ?: return@mapNotNull null
            MembershipOpsRecommendation(
                id = id,
                title = map["title"] as? String ?: id,
                summary = map["summary"] as? String ?: "",
                recommendationType = map["recommendationType"] as? String ?: "unknown",
                confidenceScore = (map["confidenceScore"] as? Number)?.toDouble() ?: 0.0,
                severity = map["severity"] as? String ?: "medium",
            )
        }
    }

    suspend fun simulateImpact(recommendationIds: List<String>, horizonDays: Int = 14): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry(
            "simulateAiMembershipOpsImpact",
            mapOf(
                "recommendationIds" to recommendationIds,
                "timeHorizonDays" to horizonDays,
            ),
        )
        return result.data.asStringKeyedAnyMap()
    }

    suspend fun startExperiment(draft: MembershipOpsExperimentDraft): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry(
            "startMembershipExperiment",
            mapOf(
                "lifecycleId" to draft.lifecycleId,
                "recommendationId" to draft.recommendationId,
                "recommendationType" to draft.recommendationType,
                "notes" to draft.notes,
            ),
        )
        return result.data.asStringKeyedAnyMap()
    }

    suspend fun completeExperiment(
        lifecycleId: String,
        cvrDelta: Double,
        annualDelta: Double,
        creatorDelta: Double,
        cancelDelta: Double,
        observedWindowDays: Int,
        success: Boolean,
        learnings: String,
    ): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry(
            "completeMembershipExperiment",
            mapOf(
                "lifecycleId" to lifecycleId,
                "actualImpact" to mapOf(
                    "cvrDelta" to cvrDelta,
                    "annualDelta" to annualDelta,
                    "creatorDelta" to creatorDelta,
                    "cancelDelta" to cancelDelta,
                    "observedWindowDays" to observedWindowDays,
                    "success" to success,
                    "learnings" to learnings,
                ),
            ),
        )
        return result.data.asStringKeyedAnyMap()
    }

    suspend fun rejectRecommendation(
        recommendationId: String,
        recommendationType: String,
        notes: String,
    ): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry(
            "rejectMembershipRecommendation",
            mapOf(
                "recommendationId" to recommendationId,
                "recommendationType" to recommendationType,
                "notes" to notes,
            ),
        )
        return result.data.asStringKeyedAnyMap()
    }

    suspend fun loadLearningInsights(): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry(
            "getMembershipLearningInsights",
            mapOf("lookbackDays" to 180),
        )
        return result.data.asStringKeyedAnyMap()
    }

    suspend fun loadTimeline(
        range: String = "30d",
        types: List<String> = emptyList(),
        plans: List<String> = emptyList(),
        severities: List<String> = emptyList(),
    ): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry(
            "getMembershipLifecycleTimeline",
            mapOf(
                "range" to range,
                "types" to types,
                "plans" to plans,
                "severities" to severities,
            ),
        )
        return result.data.asStringKeyedAnyMap()
    }

    suspend fun saveHygieneControls(
        values: Map<String, Any>,
        resetToDefaults: Boolean = false,
    ): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry(
            "setMembershipHygieneControls",
            mapOf(
                "membershipHygiene" to values,
                "resetToDefaults" to resetToDefaults,
            ),
        )
        return result.data.asStringKeyedAnyMap()
    }

    suspend fun loadHygieneControls(): Map<String, Any?> {
        val result = functions.callWithAppCheckRetry(
            "getMembershipHygieneControls",
            emptyMap<String, Any>(),
        )
        return result.data.asStringKeyedAnyMap()
    }
}

private fun Any?.asStringKeyedAnyMap(): Map<String, Any?> {
    val raw = this as? Map<*, *> ?: return emptyMap()
    return raw.entries.mapNotNull { (k, v) ->
        (k as? String)?.let { it to v }
    }.toMap()
}
