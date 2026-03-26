package com.skydown.android.ui.model

data class AgentUiState(
    val messages: List<AgentMessage> = listOf(
        AgentMessage(
            role = AgentMessageRole.Assistant,
            text = "Ich bin der Skydown x 22 Agent. Ich helfe dir bei Briefings, Release-Planung, Freigaben und naechsten Schritten.",
        ),
    ),
    val draft: String = "",
    val isSending: Boolean = false,
    val isAgentEnabled: Boolean = true,
    val errorMessage: String? = null,
    val quickPrompts: List<String> = listOf(
        "Mach mir einen 7-Tage-Release-Plan fuer einen neuen Skydown x 22 Track.",
        "Baue mir ein kurzes Briefing fuer einen Skydown x 22 TikTok-Teaser.",
        "Strukturiere die naechsten Schritte fuer einen Skydown x 22 Merchandise-Drop.",
        "Erstell mir einen kleinen Skydown x 22 Launch-Plan mit Content, Timing und To-dos.",
    ),
)
