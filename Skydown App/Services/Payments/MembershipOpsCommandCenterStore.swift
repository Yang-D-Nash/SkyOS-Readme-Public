import Foundation
import FirebaseFunctions

struct MembershipOpsRecommendationRow: Identifiable, Equatable {
    let id: String
    let title: String
    let summary: String
    let recommendationType: String
    let confidenceScore: Double
    let severity: String
    let priorityScore: Int
}

@MainActor
final class MembershipOpsCommandCenterStore: ObservableObject {
    static let shared = MembershipOpsCommandCenterStore()

    @Published private(set) var isLoading = false
    @Published private(set) var dashboard: [String: Any] = [:]
    @Published private(set) var timeseries: [String: Any] = [:]
    @Published private(set) var recommendations: [MembershipOpsRecommendationRow] = []
    @Published private(set) var simulations: [String: Any] = [:]
    @Published private(set) var learnings: [String: Any] = [:]
    @Published private(set) var timeline: [String: Any] = [:]
    @Published private(set) var hygiene: [String: Any] = [:]
    @Published private(set) var lastErrorMessage: String?

    private let functions = Functions.functions(region: "us-central1")

    func refreshAll() async {
        isLoading = true
        lastErrorMessage = nil
        do {
            async let dashboardResult = fetchMap(callable: "getAiMembershipDashboard", payload: [:])
            async let timeseriesResult = fetchMap(callable: "getAiMembershipDashboardTimeseries", payload: ["windowDays": 30])
            async let recommendationResult = fetchMap(callable: "getAiMembershipTrendRecommendations", payload: [:])
            async let learningsResult = fetchMap(callable: "getMembershipLearningInsights", payload: ["lookbackDays": 180])
            async let hygieneResult = fetchMap(callable: "getMembershipHygieneControls", payload: [:])

            let dashboardData = try await dashboardResult
            let timeseriesData = try await timeseriesResult
            let recommendationData = try await recommendationResult
            let learningsData = try await learningsResult
            let hygieneData = try await hygieneResult
            let timelineData = try await fetchMap(callable: "getMembershipLifecycleTimeline", payload: ["range": "30d"])

            dashboard = dashboardData
            timeseries = timeseriesData
            learnings = learningsData
            hygiene = hygieneData
            timeline = timelineData
            recommendations = decodeRecommendations(recommendationData)
            if !recommendations.isEmpty {
                let impactData = try await fetchMap(
                    callable: "simulateAiMembershipOpsImpact",
                    payload: [
                        "recommendationIds": recommendations.prefix(6).map(\.id),
                        "timeHorizonDays": 14
                    ]
                )
                simulations = impactData
            } else {
                simulations = [:]
            }
        } catch {
            lastErrorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func startExperiment(for recommendation: MembershipOpsRecommendationRow) async -> Result<Void, Error> {
        do {
            let lifecycleId = "lifecycle_\(recommendation.id)_\(Int(Date().timeIntervalSince1970 * 1000))"
            _ = try await fetchMap(
                callable: "startMembershipExperiment",
                payload: [
                    "lifecycleId": lifecycleId,
                    "recommendationId": recommendation.id,
                    "recommendationType": recommendation.recommendationType,
                    "notes": "Started from iOS Membership Command Center"
                ]
            )
            return .success(())
        } catch {
            return .failure(error)
        }
    }

    func rejectRecommendation(_ recommendation: MembershipOpsRecommendationRow) async -> Result<Void, Error> {
        do {
            _ = try await fetchMap(
                callable: "rejectMembershipRecommendation",
                payload: [
                    "recommendationId": recommendation.id,
                    "recommendationType": recommendation.recommendationType,
                    "notes": "Rejected from iOS Membership Command Center"
                ]
            )
            return .success(())
        } catch {
            return .failure(error)
        }
    }

    func completeExperiment(
        lifecycleId: String,
        cvrDelta: Double,
        annualDelta: Double,
        creatorDelta: Double,
        cancelDelta: Double,
        observedWindowDays: Int,
        success: Bool,
        learnings: String
    ) async -> Result<Void, Error> {
        do {
            _ = try await fetchMap(
                callable: "completeMembershipExperiment",
                payload: [
                    "lifecycleId": lifecycleId,
                    "actualImpact": [
                        "cvrDelta": cvrDelta,
                        "annualDelta": annualDelta,
                        "creatorDelta": creatorDelta,
                        "cancelDelta": cancelDelta,
                        "observedWindowDays": observedWindowDays,
                        "success": success,
                        "learnings": learnings
                    ]
                ]
            )
            return .success(())
        } catch {
            return .failure(error)
        }
    }

    func loadTimeline(range: String) async -> Result<[String: Any], Error> {
        do {
            let data = try await fetchMap(
                callable: "getMembershipLifecycleTimeline",
                payload: ["range": range]
            )
            timeline = data
            return .success(data)
        } catch {
            return .failure(error)
        }
    }

    func saveHygieneControls(values: [String: Any], resetToDefaults: Bool) async -> Result<[String: Any], Error> {
        do {
            let payload: [String: Any] = [
                "membershipHygiene": values,
                "resetToDefaults": resetToDefaults
            ]
            let data = try await fetchMap(callable: "setMembershipHygieneControls", payload: payload)
            hygiene = data
            return .success(data)
        } catch {
            return .failure(error)
        }
    }

    private func fetchMap(callable: String, payload: [String: Any]) async throws -> [String: Any] {
        let result = try await functions.invokeCallable(callable, payload: payload)
        return result.data as? [String: Any] ?? [:]
    }

    private func decodeRecommendations(_ payload: [String: Any]) -> [MembershipOpsRecommendationRow] {
        guard let raw = payload["recommendations"] as? [[String: Any]] else { return [] }
        return raw.compactMap { item in
            guard let id = item["id"] as? String else { return nil }
            return MembershipOpsRecommendationRow(
                id: id,
                title: item["title"] as? String ?? id,
                summary: item["summary"] as? String ?? "",
                recommendationType: item["recommendationType"] as? String ?? "unknown",
                confidenceScore: item["confidenceScore"] as? Double ?? 0,
                severity: item["severity"] as? String ?? "medium",
                priorityScore: item["priorityScore"] as? Int ?? 0
            )
        }
    }
}
