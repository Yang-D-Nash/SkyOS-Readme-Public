package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions

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
    val agentProvider: String = "",
    val providerFallbackUsed: Boolean = false,
    val providerNotice: String = "",
    val agentRunId: String = "",
    val resultType: String = "text",
    val results: List<AgentResultEntry> = emptyList(),
    val usage: AiUsageSnapshot? = null,
    val decision: AgentDecision? = null,
)

data class AgentResultEntry(
    val type: String,
    val text: String = "",
    val workflowName: String = "",
    val status: String = "",
    val summary: String = "",
    val runId: String = "",
)

data class AgentDecision(
    val state: String,
    val requestedTask: String,
    val route: String,
    val selectedExternal: String,
    val allowedTasks: List<String>,
    val blockedTasks: List<String>,
    val allowedTools: List<String>,
    val policy: String,
    val diagnosticsMode: String,
    val ownerMode: String,
    val killSwitch: Boolean,
    val blocked: Boolean,
    val blockReason: String,
    val retryable: Boolean,
    val retryReason: String,
    val confirmationRequired: Boolean,
    val confirmationReason: String,
    val summary: String,
    val ownerDiagnosticActive: Boolean,
)

class AgentClient {
    private val functions = FirebaseFunctions.getInstance("us-central1")

    suspend fun sendMessage(
        prompt: String,
        history: List<AgentHistoryTurn>,
        mode: String,
        executeAutomation: Boolean,
        manusApiKeyOverride: String? = null,
    ): AgentResponse {
        if (!AppNetworkMonitor.isOnline.value) {
            error("Du bist offline. Der Agent arbeitet wieder, sobald Internet da ist.")
        }

        val payload = mutableMapOf<String, Any>(
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
        manusApiKeyOverride
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { payload["manusApiKeyOverride"] = it }

        val result = functions
            .callWithAppCheckRetry(
                functionName = "skydownAgent",
                payload = payload,
            )

        return when (val data = result.data) {
            is String -> AgentResponse(
                reply = data.takeIf { it.isNotBlank() }
                    ?: error("Der SkyOS Agent hat keine Antwort geliefert."),
                historyRetentionDays = 3,
                automationTriggered = false,
                automationAttempted = false,
                automationMessage = "",
                workflowName = "",
                resultType = "text",
                results = listOf(
                    AgentResultEntry(
                        type = "text",
                        text = data,
                    ),
                ),
                usage = null,
                decision = null,
            )
            is Map<*, *> -> AgentResponse(
                reply = (data["reply"] as? String)?.takeIf { it.isNotBlank() }
                    ?: error("Der SkyOS Agent hat keine Antwort geliefert."),
                historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt() ?: 3,
                automationTriggered = data["automationTriggered"] as? Boolean ?: false,
                automationAttempted = data["automationAttempted"] as? Boolean ?: false,
                automationMessage = (data["automationMessage"] as? String).orEmpty(),
                workflowName = (data["workflowName"] as? String).orEmpty(),
                agentProvider = (data["agentProvider"] as? String).orEmpty(),
                providerFallbackUsed = data["providerFallbackUsed"] as? Boolean ?: false,
                providerNotice = (data["providerNotice"] as? String).orEmpty(),
                agentRunId = (data["agentRunId"] as? String).orEmpty(),
                resultType = (data["resultType"] as? String).orEmpty().ifBlank { "text" },
                results = (data["results"] as? List<*>)?.mapNotNull { raw ->
                    val entry = raw as? Map<*, *> ?: return@mapNotNull null
                    AgentResultEntry(
                        type = (entry["type"] as? String).orEmpty().ifBlank { "text" },
                        text = (entry["text"] as? String).orEmpty(),
                        workflowName = (entry["workflowName"] as? String).orEmpty(),
                        status = (entry["status"] as? String).orEmpty(),
                        summary = (entry["summary"] as? String).orEmpty(),
                        runId = (entry["runId"] as? String).orEmpty(),
                    )
                }.orEmpty(),
                usage = parseAiUsageSnapshot(data["usage"]),
                decision = parseAgentDecision(data["agentDecision"]),
            )
            else -> error("Der SkyOS Agent hat keine Antwort geliefert.")
        }
    }
}

private fun parseAgentDecision(payload: Any?): AgentDecision? {
    val decision = payload as? Map<*, *> ?: return null
    return AgentDecision(
        state = (decision["state"] as? String).orEmpty().ifBlank { "completed" },
        requestedTask = (decision["requestedTask"] as? String).orEmpty(),
        route = (decision["route"] as? String).orEmpty().ifBlank { "internal" },
        selectedExternal = (decision["selectedExternal"] as? String).orEmpty(),
        allowedTasks = (decision["allowedTasks"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        blockedTasks = (decision["blockedTasks"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        allowedTools = (decision["allowedTools"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        policy = (decision["policy"] as? String).orEmpty(),
        diagnosticsMode = (decision["diagnosticsMode"] as? String).orEmpty(),
        ownerMode = (decision["ownerMode"] as? String).orEmpty(),
        killSwitch = decision["killSwitch"] as? Boolean ?: false,
        blocked = decision["blocked"] as? Boolean ?: false,
        blockReason = (decision["blockReason"] as? String).orEmpty(),
        retryable = decision["retryable"] as? Boolean ?: false,
        retryReason = (decision["retryReason"] as? String).orEmpty(),
        confirmationRequired = decision["confirmationRequired"] as? Boolean ?: false,
        confirmationReason = (decision["confirmationReason"] as? String).orEmpty(),
        summary = (decision["summary"] as? String).orEmpty(),
        ownerDiagnosticActive = decision["ownerDiagnosticActive"] as? Boolean ?: false,
    )
}
