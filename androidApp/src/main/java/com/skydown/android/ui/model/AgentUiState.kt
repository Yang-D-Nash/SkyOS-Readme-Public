package com.skydown.android.ui.model

data class AgentUiState(
    val messages: List<AgentMessage> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val isAgentEnabled: Boolean = true,
    val errorMessage: String? = null,
    val quickPrompts: List<String> = listOf(
        "Baue mir einen 7-Tage-Release-Plan mit Assets, Deadlines und Ownern.",
        "Mach ein Video-Briefing mit Ziel, Shotlist, Deliverables und Risiken.",
        "Strukturiere einen Merch-Drop in To-dos, Reihenfolge und Checkliste.",
        "Erstelle einen Content-Plan fuer TikTok, Reels und Story mit Timing.",
    ),
)
