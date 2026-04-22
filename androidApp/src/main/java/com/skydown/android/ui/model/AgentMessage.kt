package com.skydown.android.ui.model

import java.util.UUID

enum class AgentMessageRole {
    User,
    Assistant,
}

enum class AgentResultType {
    Text,
    Workflow,
    Progress,
    Error,
}

data class AgentWorkflowSummary(
    val workflowName: String,
    val statusText: String,
    val runId: String? = null,
)

data class AgentMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: AgentMessageRole,
    val text: String,
    val isStreaming: Boolean = false,
    val resultType: AgentResultType = AgentResultType.Text,
    val workflowSummary: AgentWorkflowSummary? = null,
)
