package com.skydown.android.ui.model

import java.util.UUID

enum class AgentMessageRole {
    User,
    Assistant,
}

data class AgentMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: AgentMessageRole,
    val text: String,
    val isStreaming: Boolean = false,
)
