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
            "Agent · plant"
        case .webhookPending:
            "Agent · Webhook ausstehend"
        case .externalRunning:
            "Agent · extern laeuft"
        case .awaitingExternalAuth:
            "Agent · externe Auth fehlt"
        case .externalFailed:
            "Agent · extern fehlgeschlagen"
        case .externalCompleted:
            "Agent · extern abgeschlossen"
        case .fallbackInternal:
            "Agent · interner Fallback"
        case .awaitingConfirmation:
            "Agent · wartet auf Bestaetigung"
        case .executing:
            "Agent · fuehrt aus"
        case .toolPending:
            "Agent · Tool wartet"
        case .completed:
            "Agent · abgeschlossen"
        case .partial:
            "Agent · teilweise abgeschlossen"
        case .blocked:
            "Agent · blockiert"
        case .failed:
            "Agent · fehlgeschlagen"
        case .retryable:
            "Agent · retry moeglich"
        case .cancelled:
            "Agent · abgebrochen"
        case .ownerDiagnostic:
            "Agent · Owner-Diagnostik"
        case .waitingReconnect:
            "Agent · Wartet auf Verbindung"
        }
    }
}
