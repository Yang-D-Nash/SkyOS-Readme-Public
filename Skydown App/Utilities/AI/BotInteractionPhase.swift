import Foundation

/// UI + logic state for the **Bot** (fast creative assistant: text + image).
/// Kept separate from `AgentInteractionPhase` so flows never share one generic “busy” flag.
enum BotInteractionPhase: Equatable, Sendable {
    case idle
    case typing
    case sending
    case streaming
    case complete
    case degraded
    case blocked
    case retryable
    case faqAnswer
    case toolPending
    case ownerDiagnostic

    /// True while a Bot request is in flight (blocks duplicate sends).
    var isBusy: Bool {
        switch self {
        case .idle, .typing, .complete, .degraded, .blocked, .retryable, .faqAnswer, .ownerDiagnostic:
            false
        case .sending, .streaming, .toolPending:
            true
        }
    }

    /// Short line for composer chrome (premium, calm).
    var composerStatusLabel: String? {
        switch self {
        case .idle:
            nil
        case .typing:
            "Bot · bereit"
        case .sending:
            "Bot · sendet"
        case .streaming:
            "Bot · Antwort entsteht"
        case .complete:
            "Bot · fertig"
        case .degraded:
            "Bot · abgesichert"
        case .blocked:
            "Bot · blockiert"
        case .retryable:
            "Bot · retry moeglich"
        case .faqAnswer:
            "Bot · FAQ-Antwort"
        case .toolPending:
            "Bot · Visual-Pipeline"
        case .ownerDiagnostic:
            "Bot · Owner-Diagnostik"
        }
    }
}
