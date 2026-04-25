package com.nash.skyos.ui.model

/**
 * UI + logic state for the **Bot** (fast text + image). Separate from [AgentInteractionPhase].
 */
enum class BotInteractionPhase {
    Idle,
    Typing,
    Sending,
    Streaming,
    Complete,
    Degraded,
    Blocked,
    Retryable,
    FaqAnswer,
    ToolPending,
    OwnerDiagnostic;

    val isBusy: Boolean
        get() = this == Sending || this == Streaming || this == ToolPending

    val composerStatusLabel: String?
        get() = when (this) {
            Idle -> null
            Typing -> "Bot · bereit"
            Sending -> "Bot · sendet"
            Streaming -> "Bot · Antwort entsteht"
            Complete -> "Bot · fertig"
            Degraded -> "Bot · abgesichert"
            Blocked -> "Bot · blockiert"
            Retryable -> "Bot · retry moeglich"
            FaqAnswer -> "Bot · FAQ-Antwort"
            ToolPending -> "Bot · Visual-Pipeline"
            OwnerDiagnostic -> "Bot · Owner-Diagnostik"
        }
}
