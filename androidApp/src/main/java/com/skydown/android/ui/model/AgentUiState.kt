package com.skydown.android.ui.model

import com.skydown.android.data.AiRuntimeAgentProvider
import com.skydown.android.data.AiUsageSnapshot

enum class AgentExecutionMode(val rawValue: String, val title: String, val placeholder: String) {
    Release("release", "Release", "Zum Beispiel: Release-Plan fuer Freitag."),
    Briefing("briefing", "Briefing", "Zum Beispiel: Briefing fuer ein Video-Team."),
    Content("content", "Content", "Zum Beispiel: Content-Plan fuer Reels und Story."),
    Merch("merch", "Merch", "Zum Beispiel: Struktur fuer einen Merch-Drop."),
    Automation("automation", "Automation", "Zum Beispiel: Uebergabe fuer einen n8n-Workflow."),
}

data class AgentUiState(
    val messages: List<AgentMessage> = emptyList(),
    val draft: String = "",
    val agentPhase: AgentInteractionPhase = AgentInteractionPhase.Idle,
    val isAgentEnabled: Boolean = true,
    val errorMessage: String? = null,
    val selectedMode: AgentExecutionMode = AgentExecutionMode.Release,
    val canTriggerAutomation: Boolean = false,
    val shouldTriggerAutomation: Boolean = false,
    val quickPrompts: List<String> = agentQuickPromptsFor(AgentExecutionMode.Release),
    /** Last successful Agent response provider (from callable). */
    val lastAgentProvider: AiRuntimeAgentProvider = AiRuntimeAgentProvider.Grok,
    val lastProviderNotice: String = "",
    val usageSnapshot: AiUsageSnapshot? = null,
    val planLabel: String = "Free",
)

fun agentQuickPromptsFor(mode: AgentExecutionMode): List<String> = when (mode) {
    AgentExecutionMode.Release -> listOf(
        "Baue mir einen 7-Tage-Release-Plan mit Assets, Deadlines und Ownern.",
        "Plane den Launch fuer Freitag inklusive Story, Reel und CTA.",
        "Mach mir einen Mini-Release-Fahrplan fuer Song, Cover und Snippets.",
        "Welche Deliverables brauche ich fuer einen sauberen Release?",
    )
    AgentExecutionMode.Briefing -> listOf(
        "Mach ein Video-Briefing mit Ziel, Shotlist, Deliverables und Risiken.",
        "Schreib ein Briefing fuer einen Fotografen mit Mood und Must-have-Shots.",
        "Erstelle ein Copy-Briefing fuer einen externen Creator.",
        "Formuliere ein kreatives Briefing fuer Cover, Poster und Story-Assets.",
    )
    AgentExecutionMode.Content -> listOf(
        "Erstelle einen Content-Plan fuer TikTok, Reels und Story mit Timing.",
        "Plane 5 Tage Promo-Content fuer einen neuen Track.",
        "Mach eine Hook- und CTA-Strategie fuer Shortform-Content.",
        "Strukturiere eine Woche Content aus einem Dreh heraus.",
    )
    AgentExecutionMode.Merch -> listOf(
        "Strukturiere einen Merch-Drop in To-dos, Reihenfolge und Checkliste.",
        "Mach mir einen Launch-Plan fuer Hoodie und Shirt.",
        "Welche Assets und Texte braucht ein kleiner Shop-Drop?",
        "Plane eine Merch-Aktion mit Story, Shop und Follow-up.",
    )
    AgentExecutionMode.Automation -> listOf(
        "Erstelle eine n8n-Uebergabe fuer einen Content-Workflow mit Inputs und Outputs.",
        "Strukturiere einen Automations-Flow fuer Asset-Freigaben und Social-Copy.",
        "Mach ein Workflow-Briefing fuer einen Release-Reminder-Prozess.",
        "Welche Schritte und Fehlerfaelle muss eine Release-Automation abdecken?",
    )
}
