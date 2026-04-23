import Foundation
import FirebaseFunctions

@MainActor
final class AIFaqOwnerReviewLoopStore: ObservableObject {
    static let shared = AIFaqOwnerReviewLoopStore()

    struct TriggerEntry: Identifiable, Equatable {
        let triggerKey: String
        let triggers: Int
        let conversionRate: Double
        let repeatRate: Double

        var id: String { triggerKey }
    }

    struct TopicEntry: Identifiable, Equatable {
        let key: String
        let value: Int
        let share: Double

        var id: String { key }
    }

    struct RecommendationEntry: Identifiable, Equatable {
        let id: String
        let title: String
        let summary: String
        let actionType: String
        let targetField: String
        let suggestedValueLabel: String
        let priority: String
        let confidence: Double
    }

    struct StrategyInsightEntry: Identifiable, Equatable {
        let id: String
        let title: String
        let summary: String
        let expectedImpact: String
        let confidence: Double
        let severity: String
    }

    @Published private(set) var isLoading = false
    @Published private(set) var strongestTriggers: [TriggerEntry] = []
    @Published private(set) var weakTriggers: [TriggerEntry] = []
    @Published private(set) var likelyUselessTriggers: [TriggerEntry] = []
    @Published private(set) var repeatHeavyTopics: [TopicEntry] = []
    @Published private(set) var recommendations: [RecommendationEntry] = []
    @Published private(set) var strategyInsights: [StrategyInsightEntry] = []
    @Published private(set) var lastActionMessage: String?
    @Published private(set) var lastMetricsSnapshot: String?
    @Published private(set) var lastErrorMessage: String?

    private let functions = Functions.functions(region: "us-central1")

    func clear() {
        strongestTriggers = []
        weakTriggers = []
        likelyUselessTriggers = []
        repeatHeavyTopics = []
        recommendations = []
        strategyInsights = []
        lastErrorMessage = nil
        lastActionMessage = nil
        lastMetricsSnapshot = nil
        isLoading = false
    }

    func refresh(windowDays: Int = 30) async {
        isLoading = true
        lastErrorMessage = nil
        defer { isLoading = false }

        do {
            let result = try await functions.invokeCallable(
                "getAiFaqOwnerIntelligence",
                payload: ["windowDays": windowDays]
            )
            let payload = result.data as? [String: Any] ?? [:]
            let reviewLoop = payload["reviewLoop"] as? [String: Any] ?? [:]

            strongestTriggers = decodeTriggerEntries(reviewLoop["strongestTriggers"])
            weakTriggers = decodeTriggerEntries(reviewLoop["weakTriggers"])
            likelyUselessTriggers = decodeTriggerEntries(reviewLoop["likelyUselessTriggers"])
            repeatHeavyTopics = decodeTopicEntries(reviewLoop["repeatHeavyTopics"])
            recommendations = decodeRecommendationEntries(reviewLoop["recommendations"])
            strategyInsights = decodeStrategyInsights(reviewLoop["strategyInsights"])
        } catch {
            lastErrorMessage = error.localizedDescription
        }
    }

    func preview(recommendation: RecommendationEntry, windowDays: Int = 30) async -> String {
        do {
            let result = try await functions.invokeCallable(
                "previewAiFaqReviewRecommendation",
                payload: [
                    "windowDays": windowDays,
                    "recommendation": [
                        "id": recommendation.id,
                        "actionType": recommendation.actionType,
                        "targetField": recommendation.targetField,
                        "suggestedValue": recommendation.suggestedValueLabel
                    ]
                ]
            )
            let payload = result.data as? [String: Any] ?? [:]
            let allowed = payload["allowed"] as? Bool ?? false
            let safeguards = payload["safeguards"] as? [String] ?? []
            if allowed {
                let message = safeguards.isEmpty ? "Preview: sicher anwendbar." : "Preview: anwendbar mit Hinweisen: \(safeguards.joined(separator: " | "))"
                lastActionMessage = message
                lastMetricsSnapshot = metricsSummary(payload: payload)
                return message
            }
            let message = safeguards.isEmpty ? "Preview blockiert." : "Preview blockiert: \(safeguards.joined(separator: " | "))"
            lastActionMessage = message
            lastMetricsSnapshot = metricsSummary(payload: payload)
            return message
        } catch {
            let message = "Preview fehlgeschlagen: \(error.localizedDescription)"
            lastActionMessage = message
            return message
        }
    }

    func apply(recommendation: RecommendationEntry, windowDays: Int = 30) async -> String {
        do {
            let result = try await functions.invokeCallable(
                "applyAiFaqReviewRecommendation",
                payload: [
                    "windowDays": windowDays,
                    "recommendationId": recommendation.id
                ]
            )
            let payload = result.data as? [String: Any] ?? [:]
            let status = payload["status"] as? String ?? "unknown"
            let safeguards = payload["safeguards"] as? [String] ?? []
            let message: String
            if status == "applied" {
                message = safeguards.isEmpty ? "Recommendation angewendet." : "Angewendet mit Hinweisen: \(safeguards.joined(separator: " | "))"
                lastMetricsSnapshot = metricsSummary(payload: payload)
                await refresh(windowDays: windowDays)
            } else {
                message = safeguards.isEmpty ? "Apply blockiert." : "Apply blockiert: \(safeguards.joined(separator: " | "))"
                lastMetricsSnapshot = metricsSummary(payload: payload)
            }
            lastActionMessage = message
            return message
        } catch {
            let message = "Apply fehlgeschlagen: \(error.localizedDescription)"
            lastActionMessage = message
            return message
        }
    }

    func revertLastChange(windowDays: Int = 30) async -> String {
        do {
            let result = try await functions.invokeCallable("revertLastAiFaqReviewChange", payload: [:])
            let payload = result.data as? [String: Any] ?? [:]
            let status = payload["status"] as? String ?? "unknown"
            let message: String
            if status == "reverted" {
                message = "Letzte Aenderung wurde revertiert."
                await refresh(windowDays: windowDays)
            } else {
                message = payload["message"] as? String ?? "Kein Revert durchgefuehrt."
            }
            lastActionMessage = message
            return message
        } catch {
            let message = "Revert fehlgeschlagen: \(error.localizedDescription)"
            lastActionMessage = message
            return message
        }
    }

    private func metricsSummary(payload: [String: Any]) -> String? {
        let before = payload["before"] as? [String: Any]
        let after = payload["after"] as? [String: Any]
        let beforeMetrics = before?["metrics"] as? [String: Any]
        let afterMetrics = after?["metrics"] as? [String: Any]
        guard let beforeHint = (beforeMetrics?["hintAttributedUpgrades"] as? NSNumber)?.doubleValue,
              let afterHint = (afterMetrics?["hintAttributedUpgrades"] as? NSNumber)?.doubleValue else {
            return nil
        }
        return "Before/After Snapshot: attributed upgrades \(Int(beforeHint)) -> \(Int(afterHint))"
    }

    private func decodeTriggerEntries(_ value: Any?) -> [TriggerEntry] {
        guard let rows = value as? [[String: Any]] else { return [] }
        return rows.compactMap { row in
            guard let triggerKey = row["triggerKey"] as? String else { return nil }
            return TriggerEntry(
                triggerKey: triggerKey,
                triggers: row["triggers"] as? Int ?? 0,
                conversionRate: row["conversionRate"] as? Double ?? 0,
                repeatRate: row["repeatRate"] as? Double ?? 0
            )
        }
    }

    private func decodeTopicEntries(_ value: Any?) -> [TopicEntry] {
        guard let rows = value as? [[String: Any]] else { return [] }
        return rows.compactMap { row in
            guard let key = row["key"] as? String else { return nil }
            return TopicEntry(
                key: key,
                value: row["value"] as? Int ?? 0,
                share: row["share"] as? Double ?? 0
            )
        }
    }

    private func decodeRecommendationEntries(_ value: Any?) -> [RecommendationEntry] {
        guard let rows = value as? [[String: Any]] else { return [] }
        return rows.compactMap { row in
            guard let id = row["id"] as? String else { return nil }
            return RecommendationEntry(
                id: id,
                title: row["title"] as? String ?? id,
                summary: row["summary"] as? String ?? "",
                actionType: row["actionType"] as? String ?? "",
                targetField: row["targetField"] as? String ?? "",
                suggestedValueLabel: String(describing: row["suggestedValue"] ?? ""),
                priority: row["priority"] as? String ?? "medium",
                confidence: row["confidence"] as? Double ?? 0
            )
        }
    }

    private func decodeStrategyInsights(_ value: Any?) -> [StrategyInsightEntry] {
        guard let rows = value as? [[String: Any]] else { return [] }
        return rows.compactMap { row in
            guard let id = row["id"] as? String else { return nil }
            return StrategyInsightEntry(
                id: id,
                title: row["title"] as? String ?? id,
                summary: row["summary"] as? String ?? "",
                expectedImpact: row["expectedImpact"] as? String ?? "",
                confidence: row["confidence"] as? Double ?? 0,
                severity: row["severity"] as? String ?? "medium"
            )
        }
    }
}
