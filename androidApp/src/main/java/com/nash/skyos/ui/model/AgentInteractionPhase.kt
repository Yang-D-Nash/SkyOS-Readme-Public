package com.nash.skyos.ui.model

/**
 * UI + logic state for the **Agent** (strategic layer). Separate from [BotInteractionPhase].
 */
enum class AgentInteractionPhase {
    Idle,
    Planning,
    WebhookPending,
    ExternalRunning,
    AwaitingExternalAuth,
    ExternalFailed,
    ExternalCompleted,
    FallbackInternal,
    AwaitingConfirmation,
    Executing,
    ToolPending,
    Completed,
    Partial,
    Blocked,
    Failed,
    Retryable,
    Cancelled,
    OwnerDiagnostic,
    WaitingReconnect;

    val shouldBlockSend: Boolean
        get() = this == Planning || this == WebhookPending || this == ExternalRunning || this == Executing || this == ToolPending

    val shouldBlockComposerChrome: Boolean
        get() = this == Planning || this == WebhookPending || this == ExternalRunning || this == Executing || this == ToolPending

    val composerStatusLabel: String?
        get() = when (this) {
            Idle -> null
            Planning -> "Agent · plant"
            WebhookPending -> "Agent · Webhook ausstehend"
            ExternalRunning -> "Agent · extern laeuft"
            AwaitingExternalAuth -> "Agent · externe Auth fehlt"
            ExternalFailed -> "Agent · extern fehlgeschlagen"
            ExternalCompleted -> "Agent · extern abgeschlossen"
            FallbackInternal -> "Agent · interner Fallback"
            AwaitingConfirmation -> "Agent · wartet auf Bestaetigung"
            Executing -> "Agent · fuehrt aus"
            ToolPending -> "Agent · Tool wartet"
            Completed -> "Agent · abgeschlossen"
            Partial -> "Agent · teilweise abgeschlossen"
            Blocked -> "Agent · blockiert"
            Failed -> "Agent · fehlgeschlagen"
            Retryable -> "Agent · retry moeglich"
            Cancelled -> "Agent · abgebrochen"
            OwnerDiagnostic -> "Agent · Owner-Diagnostik"
            WaitingReconnect -> "Agent · Wartet auf Verbindung"
        }
}
