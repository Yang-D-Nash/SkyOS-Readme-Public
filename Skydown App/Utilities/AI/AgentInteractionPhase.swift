import Foundation

/// UI + logic state for the **Agent** (strategic layer: reasoning, planning, automation).
/// Distinct from `BotInteractionPhase`: queued offline work is not the same as “thinking”.
enum AgentInteractionPhase: Equatable, Sendable {
    case idle
    case planning
    case webhookPending
    case externalRunning
    case awaitingExternalAuth
    case externalFailed
    case externalCompleted
    case fallbackInternal
    case awaitingConfirmation
    case executing
    case toolPending
    case completed
    case partial
    case blocked
    case failed
    case retryable
    case cancelled
    case ownerDiagnostic
    /// Prompts saved locally; will send when connectivity returns.
    case waitingReconnect

    /// Blocks duplicate sends while a network request is active.
    var shouldBlockSend: Bool {
        switch self {
        case .planning, .webhookPending, .externalRunning, .executing, .toolPending:
            true
        case .idle, .awaitingExternalAuth, .externalFailed, .externalCompleted, .fallbackInternal, .awaitingConfirmation, .completed, .partial, .blocked, .failed, .retryable, .cancelled, .ownerDiagnostic, .waitingReconnect:
            false
        }
    }

    /// Disables reset + primary send while the Agent round-trip runs.
    var shouldBlockComposerChrome: Bool {
        switch self {
        case .planning, .webhookPending, .externalRunning, .executing, .toolPending:
            true
        case .idle, .awaitingExternalAuth, .externalFailed, .externalCompleted, .fallbackInternal, .awaitingConfirmation, .completed, .partial, .blocked, .failed, .retryable, .cancelled, .ownerDiagnostic, .waitingReconnect:
            false
        }
    }

    var composerStatusLabel: String? {
        switch self {
        case .idle:
            nil
        case .planning:
            AppLocalized.text("agent.composer.status.planning", fallback: "Agent · planning")
        case .webhookPending:
            AppLocalized.text("agent.composer.status.webhook_pending", fallback: "Agent · webhook pending")
        case .externalRunning:
            AppLocalized.text("agent.composer.status.external_running", fallback: "Agent · external running")
        case .awaitingExternalAuth:
            AppLocalized.text("agent.composer.status.external_auth", fallback: "Agent · external auth missing")
        case .externalFailed:
            AppLocalized.text("agent.composer.status.external_failed", fallback: "Agent · external failed")
        case .externalCompleted:
            AppLocalized.text("agent.composer.status.external_done", fallback: "Agent · external completed")
        case .fallbackInternal:
            AppLocalized.text("agent.composer.status.fallback_internal", fallback: "Agent · internal fallback")
        case .awaitingConfirmation:
            AppLocalized.text("agent.composer.status.awaiting_confirmation", fallback: "Agent · awaiting confirmation")
        case .executing:
            AppLocalized.text("agent.composer.status.executing", fallback: "Agent · executing")
        case .toolPending:
            AppLocalized.text("agent.composer.status.tool_pending", fallback: "Agent · tool pending")
        case .completed:
            AppLocalized.text("agent.composer.status.completed", fallback: "Agent · completed")
        case .partial:
            AppLocalized.text("agent.composer.status.partial", fallback: "Agent · partially completed")
        case .blocked:
            AppLocalized.text("agent.composer.status.blocked", fallback: "Agent · blocked")
        case .failed:
            AppLocalized.text("agent.composer.status.failed", fallback: "Agent · failed")
        case .retryable:
            AppLocalized.text("agent.composer.status.retryable", fallback: "Agent · retryable")
        case .cancelled:
            AppLocalized.text("agent.composer.status.cancelled", fallback: "Agent · cancelled")
        case .ownerDiagnostic:
            AppLocalized.text("agent.composer.status.owner_diagnostic", fallback: "Agent · owner diagnostic")
        case .waitingReconnect:
            AppLocalized.text("agent.composer.status.waiting_reconnect", fallback: "Agent · waiting for connection")
        }
    }
}
