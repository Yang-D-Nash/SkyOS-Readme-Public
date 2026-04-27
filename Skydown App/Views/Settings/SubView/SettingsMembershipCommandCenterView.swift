import SwiftUI

private enum MembershipOpsTab: String, CaseIterable, Identifiable {
    case dashboard = "Dashboard"
    case recommendations = "Recommendations"
    case experiments = "Experiments"
    case learnings = "Learnings"
    case timeline = "Timeline"

    var id: String { rawValue }
}

struct SettingsMembershipCommandCenterView: View {
    @ObservedObject var store: MembershipOpsCommandCenterStore
    let colorScheme: ColorScheme
    let onFeedback: (String, ToastStyle) -> Void

    @State private var selectedTab: MembershipOpsTab = .dashboard
    @State private var lifecycleIdDraft = ""
    @State private var cvrDeltaDraft = "0.00"
    @State private var annualDeltaDraft = "0.00"
    @State private var creatorDeltaDraft = "0.00"
    @State private var cancelDeltaDraft = "0.00"
    @State private var observedDaysDraft = "14"
    @State private var learningsDraft = ""
    @State private var timelineRange = "30d"
    @State private var cooldownDaysCompletedDraft = "10"
    @State private var cooldownDaysRejectedDraft = "21"
    @State private var cooldownDaysProposedDraft = "7"
    @State private var similarityStrictnessDraft = "balanced"
    @State private var recurringPenaltyDraft = "0.18"
    @State private var freshnessFloorDraft = "0.20"
    @State private var duplicateMergeWindowDaysDraft = "14"
    @State private var hygieneProfileLabel = "balanced"

    private func t(_ key: String, _ fallback: String) -> String {
        AppLocalized.text(key, fallback: fallback)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(t("settings.membership_ops.title", "Membership Control"))
                .font(.headline)
            Text(t("settings.membership_ops.subtitle", "Owner area for KPIs, experiments, and learnings."))
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(MembershipOpsTab.allCases) { tab in
                        Button(tab.rawValue) { selectedTab = tab }
                            .buttonStyle(.bordered)
                            .controlSize(.small)
                            .tint(selectedTab == tab ? AppColors.accent(for: colorScheme) : AppColors.secondaryText(for: colorScheme))
                    }
                }
            }

            if store.isLoading {
                ProgressView(t("settings.membership_ops.loading", "Loading membership control..."))
            } else {
                switch selectedTab {
                case .dashboard:
                    dashboardView
                case .recommendations:
                    recommendationsView
                case .experiments:
                    experimentsView
                case .learnings:
                    learningsView
                case .timeline:
                    timelineView
                }
            }
        }
        .task {
            await store.refreshAll()
            hydrateHygieneDrafts()
        }
    }

    private var dashboardView: some View {
        VStack(alignment: .leading, spacing: 8) {
            let windows = store.dashboard["windows"] as? [String: Any] ?? [:]
            let d7 = windows["d7"] as? [String: Any] ?? [:]
            let alerts = store.dashboard["alerts"] as? [[String: Any]] ?? []
            let cost = store.dashboard["costOverlay"] as? [String: Any] ?? [:]
            Text("7d Opens: \(d7["membershipOpens"] as? Int ?? 0) · Purchases: \(d7["purchaseSuccess"] as? Int ?? 0)")
                .font(.subheadline.weight(.semibold))
            Text("CVR: \(String(format: "%.3f", d7["cvr"] as? Double ?? 0)) · Alerts: \(alerts.count)")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Text("Free Load Ratio: \(String(format: "%.3f", cost["freePlanLoadRatio"] as? Double ?? 0))")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
    }

    private var recommendationsView: some View {
        VStack(alignment: .leading, spacing: 10) {
            if store.recommendations.isEmpty {
                Text(t("settings.membership_ops.empty_recommendations", "No open recommendations."))
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else {
                ForEach(store.recommendations) { recommendation in
                    SkydownCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(recommendation.title).font(.subheadline.weight(.semibold))
                            Text(recommendation.summary)
                                .font(.footnote)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            Text("\(t("settings.membership_ops.confidence", "Confidence")) \(Int(recommendation.confidenceScore * 100))% · \(recommendation.severity)")
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            HStack(spacing: 8) {
                                Button(t("settings.membership_ops.start_experiment", "Start experiment")) {
                                    Task {
                                        let result = await store.startExperiment(for: recommendation)
                                        switch result {
                                        case .success:
                                            onFeedback(t("settings.membership_ops.experiment_started", "Experiment started."), .success)
                                        case .failure(let error):
                                            onFeedback(error.localizedDescription, .error)
                                        }
                                    }
                                }
                                .buttonStyle(.borderedProminent)
                                Button(t("settings.membership_ops.reject", "Reject")) {
                                    Task {
                                        let result = await store.rejectRecommendation(recommendation)
                                        switch result {
                                        case .success:
                                            onFeedback(t("settings.membership_ops.recommendation_rejected", "Recommendation rejected."), .warning)
                                        case .failure(let error):
                                            onFeedback(error.localizedDescription, .error)
                                        }
                                    }
                                }
                                .buttonStyle(.bordered)
                                .controlSize(.small)
                            }
                        }
                    }
                }
            }
        }
    }

    private var experimentsView: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingsInputField(title: "Lifecycle ID", text: $lifecycleIdDraft, colorScheme: colorScheme, placeholder: "lifecycle_...")
            SettingsInputField(title: "CVR Delta", text: $cvrDeltaDraft, colorScheme: colorScheme, placeholder: "0.02")
            SettingsInputField(title: "Annual Delta", text: $annualDeltaDraft, colorScheme: colorScheme, placeholder: "0.01")
            SettingsInputField(title: "Creator Delta", text: $creatorDeltaDraft, colorScheme: colorScheme, placeholder: "0.01")
            SettingsInputField(title: "Cancel Delta", text: $cancelDeltaDraft, colorScheme: colorScheme, placeholder: "-0.02")
            SettingsInputField(title: "Observed Days", text: $observedDaysDraft, colorScheme: colorScheme, placeholder: "14")
            SettingsInputField(title: "Learnings", text: $learningsDraft, colorScheme: colorScheme, placeholder: "Kurz und konkret.")
            Button(t("settings.membership_ops.complete_experiment", "Complete experiment")) {
                Task {
                    let result = await store.completeExperiment(
                        lifecycleId: lifecycleIdDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                        cvrDelta: Double(cvrDeltaDraft) ?? 0,
                        annualDelta: Double(annualDeltaDraft) ?? 0,
                        creatorDelta: Double(creatorDeltaDraft) ?? 0,
                        cancelDelta: Double(cancelDeltaDraft) ?? 0,
                        observedWindowDays: Int(observedDaysDraft) ?? 14,
                        success: (Double(cvrDeltaDraft) ?? 0) > 0,
                        learnings: learningsDraft
                    )
                    switch result {
                    case .success:
                        onFeedback(t("settings.membership_ops.experiment_completed", "Experiment completed."), .success)
                    case .failure(let error):
                        onFeedback(error.localizedDescription, .error)
                    }
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accent(for: colorScheme))
        }
    }

    private var learningsView: some View {
        VStack(alignment: .leading, spacing: 8) {
            let insights = store.learnings["insights"] as? [String: Any] ?? [:]
            Text("\(t("settings.membership_ops.data_strength", "Data strength")): \((store.learnings["dataStrength"] as? String) ?? "unknown")")
                .font(.subheadline.weight(.semibold))
            Text("\(t("settings.membership_ops.calibration", "Calibration")): \(String(format: "%.3f", insights["confidenceCalibrationScore"] as? Double ?? 0))")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Text("\(t("settings.membership_ops.simulation_accuracy", "Simulation accuracy")): \(String(format: "%.3f", insights["simulationAccuracyTrend"] as? Double ?? 0))")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            let bestTypes = insights["recommendationTypePerformance"] as? [[String: Any]] ?? []
            if let first = bestTypes.first {
                Text("\(t("settings.membership_ops.top_type", "Top type")): \((first["recommendationType"] as? String) ?? "n/a") · \(t("settings.membership_ops.success", "Success")) \(String(format: "%.2f", first["successRate"] as? Double ?? 0))")
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            SkydownCard {
                VStack(alignment: .leading, spacing: 8) {
                    Text(t("settings.membership_ops.hygiene.title", "Hygiene controls"))
                        .font(.subheadline.weight(.semibold))
                    Text("\(t("settings.membership_ops.hygiene.profile", "Current profile")): \(hygieneProfileLabel)")
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    SettingsInputField(title: "cooldownDaysCompleted", text: $cooldownDaysCompletedDraft, colorScheme: colorScheme, placeholder: "10")
                    Text(t("settings.membership_ops.hygiene.cooldown_completed.help", "Higher means less rotation directly after completed experiments."))
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    SettingsInputField(title: "cooldownDaysRejected", text: $cooldownDaysRejectedDraft, colorScheme: colorScheme, placeholder: "21")
                    Text(t("settings.membership_ops.hygiene.cooldown_rejected.help", "Increase when no-fit recommendations appear too often."))
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    SettingsInputField(title: "cooldownDaysProposed", text: $cooldownDaysProposedDraft, colorScheme: colorScheme, placeholder: "7")
                    Text(t("settings.membership_ops.hygiene.cooldown_proposed.help", "Prevents fresh duplicate waves."))
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    SettingsInputField(title: "similarityStrictness", text: $similarityStrictnessDraft, colorScheme: colorScheme, placeholder: "balanced")
                    Text(t("settings.membership_ops.hygiene.strictness.help", "strict | balanced | loose"))
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    SettingsInputField(title: "recurringPenalty", text: $recurringPenaltyDraft, colorScheme: colorScheme, placeholder: "0.18")
                    Text(t("settings.membership_ops.hygiene.recurring_penalty.help", "Higher means stronger priority penalty for repeats."))
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    SettingsInputField(title: "freshnessFloor", text: $freshnessFloorDraft, colorScheme: colorScheme, placeholder: "0.20")
                    Text(t("settings.membership_ops.hygiene.freshness_floor.help", "Minimum freshness for new recommendations."))
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    SettingsInputField(title: "duplicateMergeWindowDays", text: $duplicateMergeWindowDaysDraft, colorScheme: colorScheme, placeholder: "14")
                    Text(t("settings.membership_ops.hygiene.duplicate_window.help", "Window for duplicate compression."))
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    HStack(spacing: 8) {
                        Button(t("common.save", "Save")) {
                            Task {
                                let payload: [String: Any] = [
                                    "cooldownDaysCompleted": Int(cooldownDaysCompletedDraft) ?? 10,
                                    "cooldownDaysRejected": Int(cooldownDaysRejectedDraft) ?? 21,
                                    "cooldownDaysProposed": Int(cooldownDaysProposedDraft) ?? 7,
                                    "similarityStrictness": similarityStrictnessDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                                    "recurringPenalty": Double(recurringPenaltyDraft) ?? 0.18,
                                    "freshnessFloor": Double(freshnessFloorDraft) ?? 0.20,
                                    "duplicateMergeWindowDays": Int(duplicateMergeWindowDaysDraft) ?? 14
                                ]
                                let result = await store.saveHygieneControls(values: payload, resetToDefaults: false)
                                switch result {
                                case .success:
                                    hydrateHygieneDrafts()
                                    onFeedback(t("settings.membership_ops.hygiene.saved", "Hygiene controls saved."), .success)
                                case .failure(let error):
                                    onFeedback(error.localizedDescription, .error)
                                }
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.accent(for: colorScheme))

                        Button(t("settings.membership_ops.hygiene.reset_defaults", "Reset defaults")) {
                            Task {
                                let result = await store.saveHygieneControls(values: [:], resetToDefaults: true)
                                switch result {
                                case .success:
                                    hydrateHygieneDrafts()
                                    onFeedback(t("settings.membership_ops.hygiene.defaults_restored", "Default hygiene restored."), .success)
                                case .failure(let error):
                                    onFeedback(error.localizedDescription, .error)
                                }
                            }
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                    }
                }
            }
        }
    }

    private func hydrateHygieneDrafts() {
        let hygieneMap = store.hygiene["membershipHygiene"] as? [String: Any] ?? [:]
        cooldownDaysCompletedDraft = String((hygieneMap["cooldownDaysCompleted"] as? Int) ?? 10)
        cooldownDaysRejectedDraft = String((hygieneMap["cooldownDaysRejected"] as? Int) ?? 21)
        cooldownDaysProposedDraft = String((hygieneMap["cooldownDaysProposed"] as? Int) ?? 7)
        similarityStrictnessDraft = (hygieneMap["similarityStrictness"] as? String) ?? "balanced"
        recurringPenaltyDraft = String((hygieneMap["recurringPenalty"] as? Double) ?? 0.18)
        freshnessFloorDraft = String((hygieneMap["freshnessFloor"] as? Double) ?? 0.20)
        duplicateMergeWindowDaysDraft = String((hygieneMap["duplicateMergeWindowDays"] as? Int) ?? 14)
        hygieneProfileLabel = (store.hygiene["profile"] as? String) ?? "balanced"
    }

    private var timelineView: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                ForEach(["7d", "30d", "90d", "all"], id: \.self) { range in
                    Button(range) {
                        timelineRange = range
                        Task {
                            let result = await store.loadTimeline(range: range)
                            if case .failure(let error) = result {
                                onFeedback(error.localizedDescription, .error)
                            }
                        }
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .tint(timelineRange == range ? AppColors.accent(for: colorScheme) : AppColors.secondaryText(for: colorScheme))
                }
            }

            let entries = store.timeline["entries"] as? [[String: Any]] ?? []
            if entries.isEmpty {
                Text(t("settings.membership_ops.timeline.empty", "No timeline entries for the current filter."))
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else {
                ForEach(Array(entries.prefix(40).enumerated()), id: \.offset) { _, row in
                    SkydownCard {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("\(row["dateKey"] as? String ?? "-") · \(row["type"] as? String ?? "event")")
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            Text(row["title"] as? String ?? "Event")
                                .font(.subheadline.weight(.semibold))
                            Text(row["summary"] as? String ?? "")
                                .font(.footnote)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            if let ownerAction = row["ownerAction"] as? String, !ownerAction.isEmpty {
                                Text("\(t("settings.membership_ops.timeline.owner_action", "Owner step")): \(ownerAction)")
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                            if let learnings = row["learnings"] as? String, !learnings.isEmpty {
                                Text("\(t("settings.membership_ops.timeline.learnings", "Learnings")): \(learnings)")
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                            if let recommendationId = row["recommendationId"] as? String, !recommendationId.isEmpty {
                                Button(t("settings.membership_ops.timeline.rerun", "Re-run similar experiment")) {
                                    lifecycleIdDraft = "lifecycle_\(recommendationId)_\(Int(Date().timeIntervalSince1970 * 1000))"
                                    learningsDraft = "Re-run based on timeline insight for \(recommendationId)."
                                    selectedTab = .experiments
                                }
                                .buttonStyle(.bordered)
                                .controlSize(.small)
                                .tint(AppColors.secondaryText(for: colorScheme))
                            }
                        }
                    }
                }
            }
        }
    }
}

private struct SkydownCard<Content: View>: View {
    let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8, content: content)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color.primary.opacity(0.05))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(Color.primary.opacity(0.06), lineWidth: 1)
            )
    }
}

private struct SettingsInputField: View {
    let title: String
    @Binding var text: String
    let colorScheme: ColorScheme
    let placeholder: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            TextField(placeholder, text: $text)
                .textFieldStyle(.roundedBorder)
        }
    }
}
