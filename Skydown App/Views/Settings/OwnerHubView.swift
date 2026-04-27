import SwiftUI

/// Phase 1: Owner-facing overview + Daily Briefing entry (metrics are placeholders until backend wiring).
struct OwnerHubView: View {
    @Environment(\.colorScheme) private var colorScheme
    let onOpenAgentWithPrompt: ((String) -> Void)?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(AppLocalized.text("owner.hub.title", fallback: "Owner hub"))
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(AppLocalized.text("owner.hub.subtitle", fallback: "Health, growth, quality, and release at a glance."))
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                LazyVGrid(
                    columns: [
                        GridItem(.flexible(), spacing: 10),
                        GridItem(.flexible(), spacing: 10),
                    ],
                    spacing: 10
                ) {
                    OwnerHubMetricCard(
                        colorScheme: colorScheme,
                        titleKey: "owner.hub.card.health.title",
                        titleFallback: "Health",
                        subtitleKey: "owner.hub.card.health.subtitle",
                        subtitleFallback: "Stability & sync",
                        accent: AppColors.accent(for: colorScheme)
                    )
                    OwnerHubMetricCard(
                        colorScheme: colorScheme,
                        titleKey: "owner.hub.card.growth.title",
                        titleFallback: "Growth",
                        subtitleKey: "owner.hub.card.growth.subtitle",
                        subtitleFallback: "Reach & conversion",
                        accent: AppColors.spotify(for: colorScheme)
                    )
                    OwnerHubMetricCard(
                        colorScheme: colorScheme,
                        titleKey: "owner.hub.card.quality.title",
                        titleFallback: "Quality",
                        subtitleKey: "owner.hub.card.quality.subtitle",
                        subtitleFallback: "Support & content",
                        accent: AppColors.accentHighlight(for: colorScheme)
                    )
                    OwnerHubMetricCard(
                        colorScheme: colorScheme,
                        titleKey: "owner.hub.card.release.title",
                        titleFallback: "Release",
                        subtitleKey: "owner.hub.card.release.subtitle",
                        subtitleFallback: "Ship cadence",
                        accent: AppColors.accentMystic(for: colorScheme)
                    )
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text(AppLocalized.text("owner.hub.briefing.title", fallback: "Daily briefing"))
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .accessibilityIdentifier("owner.hub.briefing.title")
                    Text(AppLocalized.text("owner.hub.briefing.body", fallback: "Opens the Agent with a structured owner briefing prompt. Refine the output, then capture follow-ups in tasks or notes."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    Button {
                        onOpenAgentWithPrompt?(Self.dailyBriefingPrompt)
                    } label: {
                        Text(AppLocalized.text("owner.hub.briefing.cta", fallback: "Start briefing in Agent"))
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accentMystic(for: colorScheme))
                    .disabled(onOpenAgentWithPrompt == nil)
                    .accessibilityIdentifier("owner.hub.briefing.cta")
                }
                .padding(14)
                .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.accentMystic(for: colorScheme), shadowRadius: 8, shadowYOffset: 4)

                VStack(alignment: .leading, spacing: 8) {
                    Text(AppLocalized.text("owner.hub.roadmap.title", fallback: "Next on the roadmap"))
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Label(
                        AppLocalized.text("owner.hub.roadmap.action_queue", fallback: "Action queue — apply agent suggestions to tasks & notes"),
                        systemImage: "checklist"
                    )
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    Label(
                        AppLocalized.text("owner.hub.roadmap.weekly_pdf", fallback: "Weekly PDF / share summary for investors & team"),
                        systemImage: "doc.richtext"
                    )
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                .padding(.horizontal, 2)
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.vertical, 12)
        }
        .accessibilityIdentifier("owner.hub.root")
        .background(
            AppColors.screenGradient(
                for: colorScheme,
                secondaryAccent: AppColors.accentMystic(for: colorScheme)
            )
            .ignoresSafeArea()
        )
        .navigationTitle(AppLocalized.text("owner.hub.nav_title", fallback: "Owner"))
        .navigationBarTitleDisplayMode(.inline)
    }

    /// Daily Briefing Agent prompt. Keep verbatim in sync with `OwnerHubPrompts.dailyBriefing` in shared Kotlin.
    static var dailyBriefingPrompt: String {
        """
        You are SkyOS Daily Briefing for the product owner.

        Produce:
        1) Executive summary — max 5 bullets (plain language).
        2) Risks — max 3 bullets with severity (low/med/high).
        3) Recommended actions — numbered list; each item must start with VERB and be executable today.
        4) Suggested metrics to track next week for Health / Growth / Quality / Release (one line each).

        Constraints:
        - If data is missing, say what you need and propose the smallest next instrumentation step.
        - Do not claim real user metrics unless provided in this chat; label assumptions clearly.
        """
    }
}

private struct OwnerHubMetricCard: View {
    let colorScheme: ColorScheme
    let titleKey: String
    let titleFallback: String
    let subtitleKey: String
    let subtitleFallback: String
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(AppLocalized.text(titleKey, fallback: titleFallback))
                .font(.caption.weight(.bold))
                .foregroundColor(accent)
            Text(AppLocalized.text(subtitleKey, fallback: subtitleFallback))
                .font(.caption2)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
            Text(AppLocalized.text("owner.hub.metric.placeholder", fallback: "—"))
                .font(.title.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
        }
        .padding(12)
        .frame(maxWidth: .infinity, minHeight: 120, alignment: .topLeading)
        .skydownPanelSurface(colorScheme: colorScheme, accent: accent, cornerRadius: 18, shadowRadius: 7, shadowYOffset: 3)
    }
}
