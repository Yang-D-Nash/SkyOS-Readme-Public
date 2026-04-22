package com.skydown.android.ui.model

/**
 * UI + logic state for the **Bot** (fast text + image). Separate from [AgentInteractionPhase].
 */
enum class BotInteractionPhase {
    Idle,
    GeneratingText,
    GeneratingVisual;

    val isBusy: Boolean
        get() = this != Idle

    val composerStatusLabel: String?
        get() = when (this) {
            Idle -> null
            GeneratingText -> "Bot · Antwort entsteht"
            GeneratingVisual -> "Bot · Visual entsteht"
        }
}
