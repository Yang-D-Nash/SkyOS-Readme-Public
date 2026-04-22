import Foundation

/// UI + logic state for the **Agent** (strategic layer: reasoning, planning, automation).
/// Distinct from `BotInteractionPhase`: queued offline work is not the same as “thinking”.
enum AgentInteractionPhase: Equatable, Sendable {
    case idle
    /// Callable round-trip in progress (Grok / Gemini / Manus path).
    case processing
    /// Prompts saved locally; will send when connectivity returns.
    case waitingReconnect

    /// Blocks duplicate sends while a network request is active.
    var shouldBlockSend: Bool {
        switch self {
        case .processing:
            true
        case .idle, .waitingReconnect:
            false
        }
    }

    /// Disables reset + primary send while the Agent round-trip runs.
    var shouldBlockComposerChrome: Bool {
        switch self {
        case .processing:
            true
        case .idle, .waitingReconnect:
            false
        }
    }

    var composerStatusLabel: String? {
        switch self {
        case .idle:
            nil
        case .processing:
            "Agent · Antwort entsteht"
        case .waitingReconnect:
            "Agent · Wartet auf Verbindung"
        }
    }
}
