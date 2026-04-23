import Foundation
import os

@MainActor
final class AIMembershipCoordinator: ObservableObject {
    enum OpenReason: String {
        case manual
        case criticalUsage = "critical_usage"
        case featureLocked = "feature_locked"
        case workflowLocked = "workflow_locked"
        case agentLimit = "agent_limit"
        case settings
    }

    @Published private(set) var isPresented = false
    @Published private(set) var lastOpenReason: OpenReason = .manual
    @Published private(set) var currentPlanCache: UserQuotaPlan = .free
    @Published private(set) var surface: String = "ai_chat"

    private let logger = Logger(subsystem: "com.skydown.app", category: "ai-membership")
    private let analytics = MembershipAnalyticsTracker()

    func openMembership(reason: OpenReason, surface: String = "ai_chat") {
        lastOpenReason = reason
        self.surface = surface
        isPresented = true
        track("membership_open_reason", ["reason": reason.rawValue])
        analytics.track("membership_open", reason: reason.rawValue, surface: surface, currentPlan: currentPlanCache.rawValue)
        analytics.track("membership_reason", reason: reason.rawValue, surface: surface, currentPlan: currentPlanCache.rawValue)
        if reason == .criticalUsage {
            track("upgrade_after_warning", [:])
            analytics.track("upgrade_after_warning", reason: reason.rawValue, surface: surface, currentPlan: currentPlanCache.rawValue)
        }
    }

    func closeMembership() {
        isPresented = false
        track("membership_close", [:])
    }

    func cacheCurrentPlan(_ plan: UserQuotaPlan?) {
        currentPlanCache = plan ?? .free
    }

    func postPurchaseRefresh(plan: UserQuotaPlan?, refresh: @escaping @Sendable () async -> Void) async {
        currentPlanCache = plan ?? currentPlanCache
        await refresh()
    }

    func restore(refresh: @escaping @Sendable () async -> Void) async {
        track("restore_success", [:])
        analytics.track("restore_success", reason: lastOpenReason.rawValue, surface: surface, currentPlan: currentPlanCache.rawValue)
        await refresh()
    }

    func track(_ event: String, _ payload: [String: String]) {
        logger.info("event=\(event, privacy: .public) payload=\(payload.description, privacy: .public)")
    }
}
