import Foundation

/// UI + logic state for the **Bot** (fast creative assistant: text + image).
/// Kept separate from `AgentInteractionPhase` so flows never share one generic “busy” flag.
enum BotInteractionPhase: Equatable, Sendable {
    case idle
    case generatingText
    case generatingVisual

    /// True while a Bot request is in flight (blocks duplicate sends).
    var isBusy: Bool {
        switch self {
        case .idle:
            false
        case .generatingText, .generatingVisual:
            true
        }
    }

    /// Short line for composer chrome (premium, calm).
    var composerStatusLabel: String? {
        switch self {
        case .idle:
            nil
        case .generatingText:
            "Bot · Antwort entsteht"
        case .generatingVisual:
            "Bot · Visual entsteht"
        }
    }
}
