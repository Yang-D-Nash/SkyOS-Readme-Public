package com.skydown.android.ui.model

import java.util.UUID

enum class AiMessageRole {
    User,
    Assistant,
}

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: AiMessageRole,
    val text: String,
    val isStreaming: Boolean = false,
)
