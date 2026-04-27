package com.nash.skyos.data

import com.google.firebase.functions.FirebaseFunctions
import com.nash.skyos.R

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
    val automationSchemaVersion: String = "",
    val agentProvider: String = "",
    val providerFallbackUsed: Boolean = false,
    val providerNotice: String = "",
    val agentRunId: String = "",
    val resultType: String = "text",
    val results: List<AgentResultEntry> = emptyList(),
    val usage: AiUsageSnapshot? = null,
    val decision: AgentDecision? = null,
    val automationIdempotentReplay: Boolean = false,
)

data class AgentResultEntry(
    val type: String,
    val text: String = "",
    val url: String = "",
    val title: String = "",
    val mimeType: String = "",
    val fileName: String = "",
    val html: String = "",
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
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

data class AgentRunStatus(
    val runId: String,
    val state: String,
    val automationAttempted: Boolean,
    val automationTriggered: Boolean,
    val workflowName: String,
    val automationMessage: String,
    val provider: String = "",
    val automationSchemaVersion: String = "",
    val workflowProgressPercent: Int = 0,
    val workflowStep: String = "",
    val workflowEtaSeconds: Int = 0,
    val workflowDetails: String = "",
)

class AgentClient {
    private val functions = FirebaseFunctions.getInstance("us-central1")

    suspend fun sendMessage(
        prompt: String,
        history: List<AgentHistoryTurn>,
        mode: String,
        aiLevel: String,
        executeAutomation: Boolean,
        automationScope: String,
        manusApiKeyOverride: String? = null,
        idempotencyKey: String? = null,
        attachments: List<AgentOutboundAttachment> = emptyList(),
    ): AgentResponse {
        if (!AppNetworkMonitor.isOnline.value) {
            error(AppTextResolver.string(R.string.agent_error_offline))
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
            "aiLevel" to aiLevel,
            "executeAutomation" to executeAutomation,
            "automationScope" to automationScope,
        )
        manusApiKeyOverride
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { payload["manusApiKeyOverride"] = it }
        if (attachments.isNotEmpty()) {
            payload["attachments"] = attachments.map { it.toPayloadMap() }
        }
        idempotencyKey
            ?.trim()
            ?.takeIf { it.length >= 8 }
            ?.let { payload["idempotencyKey"] = it }

        val result = functions
            .callWithAppCheckRetry(
                functionName = "skydownAgent",
                payload = payload,
            )

        return when (val data = result.data) {
            is String -> AgentResponse(
                reply = data.takeIf { it.isNotBlank() }
                    ?: error(AppTextResolver.string(R.string.agent_error_no_response)),
                historyRetentionDays = 3,
                automationTriggered = false,
                automationAttempted = false,
                automationMessage = "",
                workflowName = "",
                automationSchemaVersion = "",
                resultType = "text",
                results = listOf(
                    AgentResultEntry(
                        type = "text",
                        text = data,
                    ),
                ),
                usage = null,
                decision = null,
                automationIdempotentReplay = false,
            )
            is Map<*, *> -> {
                val parsedResults = parseAgentResults(data["results"])
                val reply = ((data["reply"] as? String) ?: (data["message"] as? String)).orEmpty().trim()
                val resolvedReply = reply.ifBlank {
                    if (parsedResults.isNotEmpty()) {
                        AppTextResolver.string(R.string.agent_result_ready_fallback)
                    } else {
                        error(AppTextResolver.string(R.string.agent_error_no_response))
                    }
                }
                AgentResponse(
                reply = resolvedReply,
                historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt() ?: 3,
                automationTriggered = data["automationTriggered"] as? Boolean ?: false,
                automationAttempted = data["automationAttempted"] as? Boolean ?: false,
                automationMessage = (data["automationMessage"] as? String).orEmpty(),
                workflowName = (data["workflowName"] as? String).orEmpty(),
                automationSchemaVersion = (data["automationSchemaVersion"] as? String).orEmpty(),
                agentProvider = (data["agentProvider"] as? String).orEmpty(),
                providerFallbackUsed = data["providerFallbackUsed"] as? Boolean ?: false,
                providerNotice = (data["providerNotice"] as? String).orEmpty(),
                agentRunId = (data["agentRunId"] as? String).orEmpty(),
                resultType = (data["resultType"] as? String).orEmpty().ifBlank { "text" },
                results = parsedResults.ifEmpty {
                    listOf(AgentResultEntry(type = "text", text = resolvedReply))
                },
                usage = parseAiUsageSnapshot(data["usage"]),
                decision = parseAgentDecision(data["agentDecision"]),
                automationIdempotentReplay = data["automationIdempotentReplay"] as? Boolean ?: false,
            )
            }
            else -> error(AppTextResolver.string(R.string.agent_error_no_response))
        }
    }

    suspend fun fetchRunStatus(runId: String): AgentRunStatus {
        val normalizedRunId = runId.trim()
        if (normalizedRunId.isBlank()) {
            error(AppTextResolver.string(R.string.agent_error_no_response))
        }
        val result = functions
            .callWithAppCheckRetry(
                functionName = "getAgentRunStatus",
                payload = mapOf("runId" to normalizedRunId),
            )
        val data = result.data as? Map<*, *>
            ?: error(AppTextResolver.string(R.string.agent_error_no_response))
        return AgentRunStatus(
            runId = (data["runId"] as? String).orEmpty().ifBlank { normalizedRunId },
            state = (data["state"] as? String).orEmpty().trim().lowercase().ifBlank { "completed" },
            automationAttempted = data["automationAttempted"] as? Boolean ?: false,
            automationTriggered = data["automationTriggered"] as? Boolean ?: false,
            workflowName = (data["workflowName"] as? String).orEmpty(),
            automationMessage = (data["automationMessage"] as? String).orEmpty(),
            provider = (data["provider"] as? String).orEmpty(),
            automationSchemaVersion = (data["automationSchemaVersion"] as? String).orEmpty(),
            workflowProgressPercent = ((data["workflowProgressPercent"] as? Number)?.toInt() ?: 0).coerceIn(0, 100),
            workflowStep = (data["workflowStep"] as? String).orEmpty(),
            workflowEtaSeconds = ((data["workflowEtaSeconds"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
            workflowDetails = (data["workflowDetails"] as? String).orEmpty(),
        )
    }
}

private fun parseAgentResults(payload: Any?): List<AgentResultEntry> {
    val values = payload as? List<*> ?: return emptyList()
    return values.mapIndexedNotNull { index, raw ->
        when (raw) {
            is String -> raw.trim().takeIf { it.isNotEmpty() }?.let {
                AgentResultEntry(type = "text", text = it)
            }
            is Map<*, *> -> {
                val columns = parseAgentStringList(raw["columns"])
                AgentResultEntry(
                    type = (raw["type"] as? String).orEmpty().trim().lowercase().ifBlank { "text" },
                    text = (raw["text"] as? String).orEmpty(),
                    url = (raw["url"] as? String).orEmpty(),
                    title = (raw["title"] as? String).orEmpty(),
                    mimeType = (raw["mimeType"] as? String).orEmpty(),
                    fileName = ((raw["fileName"] as? String) ?: (raw["filename"] as? String)).orEmpty(),
                    html = (raw["html"] as? String).orEmpty(),
                    columns = columns,
                    rows = parseAgentTableRows(raw["rows"], columns),
                    workflowName = (raw["workflowName"] as? String).orEmpty(),
                    status = (raw["status"] as? String).orEmpty(),
                    summary = (raw["summary"] as? String).orEmpty(),
                    runId = (raw["runId"] as? String).orEmpty(),
                )
            }
            else -> null
        }
    }
}

private fun parseAgentStringList(value: Any?): List<String> {
    return (value as? List<*>)?.mapNotNull { item ->
        when (item) {
            is String -> item.trim().takeIf { it.isNotEmpty() }
            is Map<*, *> -> listOf("title", "label", "name", "key")
                .firstNotNullOfOrNull { key -> (item[key] as? String)?.trim()?.takeIf { it.isNotEmpty() } }
            else -> null
        }
    }.orEmpty()
}

private fun parseAgentTableRows(value: Any?, columns: List<String>): List<List<String>> {
    return (value as? List<*>)?.mapNotNull { row ->
        val cells = when (row) {
            is List<*> -> row.map(::stringifyAgentResultValue)
            is Map<*, *> -> {
                val keys = columns.ifEmpty { row.keys.mapNotNull { it as? String }.take(8) }
                keys.map { key -> stringifyAgentResultValue(row[key]) }
            }
            else -> listOf(stringifyAgentResultValue(row))
        }
        cells.takeIf { it.any(String::isNotBlank) }
    }.orEmpty()
}

private fun stringifyAgentResultValue(value: Any?): String = when (value) {
    null -> ""
    is String -> value.trim()
    is Number -> value.toString()
    is Boolean -> value.toString()
    else -> value.toString()
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
