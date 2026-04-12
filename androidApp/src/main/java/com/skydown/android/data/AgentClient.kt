package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

data class AgentHistoryTurn(
    val role: String,
    val text: String,
)

data class AgentResponse(
    val reply: String,
    val historyRetentionDays: Int,
    val automationTriggered: Boolean,
    val automationAttempted: Boolean,
    val automationMessage: String,
    val workflowName: String,
)

class AgentClient {
    private val functions = FirebaseFunctions.getInstance("us-central1")

    suspend fun sendMessage(
        prompt: String,
        history: List<AgentHistoryTurn>,
        mode: String,
        executeAutomation: Boolean,
    ): AgentResponse {
        if (!AppNetworkMonitor.isOnline.value) {
            error("Du bist offline. Der Agent arbeitet wieder, sobald Internet da ist.")
        }

        val payload = mapOf(
            "prompt" to prompt,
            "history" to history.map { turn ->
                mapOf(
                    "role" to turn.role,
                    "text" to turn.text,
                )
            },
            "mode" to mode,
            "executeAutomation" to executeAutomation,
        )

        val result = functions
            .getHttpsCallable("skydownAgent")
            .call(payload)
            .await()

        return when (val data = result.data) {
            is String -> AgentResponse(
                reply = data.takeIf { it.isNotBlank() }
                    ?: error("Der Skydown x 22 Agent hat keine Antwort geliefert."),
                historyRetentionDays = 3,
                automationTriggered = false,
                automationAttempted = false,
                automationMessage = "",
                workflowName = "",
            )
            is Map<*, *> -> AgentResponse(
                reply = (data["reply"] as? String)?.takeIf { it.isNotBlank() }
                    ?: error("Der Skydown x 22 Agent hat keine Antwort geliefert."),
                historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt() ?: 3,
                automationTriggered = data["automationTriggered"] as? Boolean ?: false,
                automationAttempted = data["automationAttempted"] as? Boolean ?: false,
                automationMessage = (data["automationMessage"] as? String).orEmpty(),
                workflowName = (data["workflowName"] as? String).orEmpty(),
            )
            else -> error("Der Skydown x 22 Agent hat keine Antwort geliefert.")
        }
    }
}
