package com.skydown.android.ui.model

/**
 * UI + logic state for the **Agent** (strategic layer). Separate from [BotInteractionPhase].
 */
enum class AgentInteractionPhase {
    Idle,
    Processing,
    WaitingReconnect;

    val shouldBlockSend: Boolean
        get() = this == Processing

    val shouldBlockComposerChrome: Boolean
        get() = this == Processing

    val composerStatusLabel: String?
        get() = when (this) {
            Idle -> null
            Processing -> "Agent · Antwort entsteht"
            WaitingReconnect -> "Agent · Wartet auf Verbindung"
        }
}
