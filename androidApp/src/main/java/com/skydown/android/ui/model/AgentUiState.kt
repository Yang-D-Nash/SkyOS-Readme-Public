package com.skydown.android.ui.model

data class AgentUiState(
    val messages: List<AgentMessage> = listOf(
        AgentMessage(
            role = AgentMessageRole.Assistant,
            text = "Ich bin der Skydown x 22 Agent. Ich bin fuer Struktur, Briefings, To-dos und Release-Plaene da. Wenn du nur schnelle Ideen oder Captions brauchst, nimm den Bot.",
        ),
    ),
    val draft: String = "",
    val isSending: Boolean = false,
    val isAgentEnabled: Boolean = true,
    val errorMessage: String? = null,
    val quickPrompts: List<String> = listOf(
        "Mach mir einen klaren 7-Tage-Release-Plan fuer einen neuen Skydown x 22 Track.",
        "Baue mir ein kurzes Briefing mit Ziel, Content und Format fuer einen TikTok-Teaser.",
        "Strukturiere die naechsten Schritte fuer einen Merchandise-Drop in To-dos.",
        "Erstell mir einen kleinen Launch-Plan mit Content, Timing und Abfolge.",
    ),
)
