import Foundation
#if canImport(FirebaseAnalytics)
import FirebaseAnalytics
#endif
#if canImport(FirebaseFunctions)
import FirebaseFunctions
#endif

struct MembershipAnalyticsTracker {
    func track(
        _ event: String,
        platform: String = "ios",
        reason: String? = nil,
        plan: String? = nil,
        annual: Bool? = nil,
        surface: String? = nil,
        currentPlan: String? = nil
    ) {
#if canImport(FirebaseAnalytics)
        var payload: [String: Any] = ["platform": platform]
        if let reason { payload["reason"] = reason }
        if let plan { payload["plan"] = plan }
        if let annual { payload["annual"] = annual ? "true" : "false" }
        if let surface { payload["surface"] = surface }
        if let currentPlan { payload["currentPlan"] = currentPlan }
        Analytics.logEvent(event, parameters: payload)
#endif
#if canImport(FirebaseFunctions)
        var mirrorPayload: [String: Any] = [
            "eventName": event,
            "platform": platform
        ]
        if let reason { mirrorPayload["reason"] = reason }
        if let plan { mirrorPayload["plan"] = plan }
        if let annual { mirrorPayload["annual"] = annual }
        if let surface { mirrorPayload["surface"] = surface }
        if let currentPlan { mirrorPayload["currentPlan"] = currentPlan }
        Task {
            do {
                _ = try await Functions.functions(region: "us-central1")
                    .httpsCallable("recordAiMembershipEvent")
                    .call(mirrorPayload)
            } catch {
                // Keep analytics fire-and-forget; failures should not impact caller flow.
            }
        }
#endif
    }
}
