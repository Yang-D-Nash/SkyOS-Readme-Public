import SwiftUI

struct HomeSectionBanner: View {
    let title: String
    let subtitle: String
    let icon: String
    let colorScheme: ColorScheme
    let accent: Color

    var body: some View {
        HStack(spacing: SkydownLayout.stackSpacingPill) {
            ZStack {
                Circle().fill(accent.opacity(colorScheme == .dark ? 0.16 : 0.14))
                Image(systemName: icon).font(.caption.weight(.bold)).foregroundColor(accent)
            }
            .frame(width: 24, height: 24)
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                Text(title).font(.subheadline.weight(.bold)).foregroundColor(AppColors.text(for: colorScheme))
                Text(subtitle).font(.caption).foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
            Spacer()
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous).fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.72)))
        .overlay(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous).stroke(accent.opacity(0.14), lineWidth: 1))
    }
}

private struct HomeStatusCapsule: View {
    let title: String
    let accent: Color
    let isActive: Bool
    let colorScheme: ColorScheme

    var body: some View {
        Text(title)
            .font(.caption2.weight(.semibold))
            .foregroundColor(isActive ? accent : AppColors.secondaryText(for: colorScheme))
            .padding(.horizontal, 9)
            .padding(.vertical, 6)
            .background(Capsule(style: .continuous).fill(accent.opacity(isActive ? 0.14 : 0.08)))
            .overlay(Capsule(style: .continuous).stroke(accent.opacity(isActive ? 0.26 : 0.12), lineWidth: 1))
    }
}

// Moved as-is from legacy Home container for ownership cleanup.
struct HomeDailyOpsStrip: View {
    let colorScheme: ColorScheme
    let activeSignalCount: Int
    let totalSignalCount: Int
    let hasTrackSignal: Bool
    let hasVideoSignal: Bool
    let onRefresh: () -> Void
    let onOpenRelease: () -> Void
    let onOpenVideo: () -> Void

    private enum HomePriorityTarget { case music, visuals }
    private var currentHour: Int { Calendar.current.component(.hour, from: Date()) }
    private var priorityTarget: HomePriorityTarget {
        if !hasTrackSignal { return .music }
        if !hasVideoSignal { return .visuals }
        return currentHour < 12 ? .music : .visuals
    }
    private var priorityTitle: String {
        switch priorityTarget {
        case .music:
            return hasTrackSignal
                ? AppLocalized.text("home.dailyops.priority.music.ready", fallback: "Now priority: Music")
                : AppLocalized.text("home.dailyops.priority.music.build", fallback: "Now priority: Add music")
        case .visuals:
            return hasVideoSignal
                ? AppLocalized.text("home.dailyops.priority.visuals.ready", fallback: "Now priority: Visuals")
                : AppLocalized.text("home.dailyops.priority.visuals.build", fallback: "Now priority: Add visuals")
        }
    }
    private func triggerPriorityAction() {
        switch priorityTarget { case .music: onOpenRelease(); case .visuals: onOpenVideo() }
    }
    private var priorityHint: String {
        switch priorityTarget {
        case .music:
            return hasTrackSignal
                ? AppLocalized.text("home.dailyops.hint.music.morning", fallback: "Check music status first in the morning.")
                : AppLocalized.text("home.dailyops.hint.music.missing", fallback: "Music signal is not live yet.")
        case .visuals:
            return hasVideoSignal
                ? AppLocalized.text("home.dailyops.hint.visuals.evening", fallback: "Check visuals status first in the evening.")
                : AppLocalized.text("home.dailyops.hint.visuals.missing", fallback: "Visuals signal is not live yet.")
        }
    }
    private var priorityAccent: Color {
        switch priorityTarget {
        case .music: return AppColors.accent(for: colorScheme)
        case .visuals: return AppColors.accentHighlight(for: colorScheme)
        }
    }
    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            HStack(spacing: SkydownLayout.stackSpacingDense) {
                Image(systemName: "gauge.with.dots.needle.50percent")
                    .font(.caption2.weight(.medium))
                    .foregroundColor(priorityAccent.opacity(0.5))
                Text(
                    AppLocalized.text("home.dailyops.current_focus", fallback: "Current focus")
                )
                    .font(.subheadline.weight(.regular))
                    .foregroundColor(AppColors.text(for: colorScheme).opacity(0.6))
                Spacer(minLength: 0)
                Button {
                    onRefresh()
                    SkydownHaptics.selection()
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(priorityAccent.opacity(0.55))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(
                    AppLocalized.text("home.dailyops.refresh", fallback: "Refresh")
                )
                Text(
                    String(
                        format: AppLocalized.text("home.dailyops.live_count", fallback: "%d/%d live"),
                        activeSignalCount,
                        totalSignalCount
                    )
                )
                    .font(.caption2.weight(.medium))
                    .foregroundColor(priorityAccent.opacity(0.7))
            }
            Text(
                AppLocalized.text(
                    "home.dailyops.hint",
                    fallback: "Here: one step that fits your core signals."
                )
            )
                .font(.footnote)
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.5))
            SkydownBrandActionButton(
                title: priorityTitle,
                systemImage: "target",
                accent: priorityAccent,
                colorScheme: colorScheme,
                font: .caption.weight(.bold),
                cornerRadius: SkydownLayout.denseRadius,
                verticalPadding: 8,
                action: triggerPriorityAction
            )
            Text(priorityHint)
                .font(.caption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.85))
        }
        .padding(10)
    }
}

struct HomeCommandDockStrip: View {
    let colorScheme: ColorScheme
    let priorityTarget: String
    let onOpenWorkflow: (() -> Void)?
    let onOpenCart: () -> Void
    let onOpenSettings: () -> Void
    private var priorityAccent: Color { priorityTarget == "music" ? AppColors.accent(for: colorScheme) : AppColors.accentHighlight(for: colorScheme) }
    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(spacing: SkydownLayout.stackSpacingDense) {
                Image(systemName: "command")
                    .font(.caption2.weight(.medium))
                    .foregroundColor(priorityAccent.opacity(0.48))
                Text(
                    AppLocalized.text("home.shortcuts.title", fallback: "Shortcuts")
                )
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(AppColors.text(for: colorScheme).opacity(0.58))
                Spacer(minLength: 0)
            }
            Text(
                AppLocalized.text(
                    "home.shortcuts.subtitle",
                    fallback: "Agent, cart, and settings without leaving the page."
                )
            )
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.72))
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                if let onOpenWorkflow {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("home.command.ai_agent", fallback: "AI agent"),
                        systemImage: "sparkles",
                        accent: AppColors.accentMystic(for: colorScheme),
                        colorScheme: colorScheme,
                        isEnabled: true,
                        font: .caption.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: true,
                        action: onOpenWorkflow
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                SkydownBrandActionButton(
                    title: AppLocalized.text("home.command.cart", fallback: "Cart"),
                    systemImage: "bag",
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    font: .caption.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 8,
                    expandToFullWidth: true,
                    action: onOpenCart
                )
                .frame(maxWidth: .infinity, alignment: .leading)
                SkydownBrandActionButton(
                    title: AppLocalized.text("home.command.settings", fallback: "Settings"),
                    systemImage: "gearshape",
                    accent: AppColors.accentHighlight(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    font: .caption.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 8,
                    expandToFullWidth: true,
                    action: onOpenSettings
                )
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.top, 2)
    }
}

struct HomeLiveSignalSurface: View {
    let colorScheme: ColorScheme
    let hasTrackSignal: Bool
    let hasVideoSignal: Bool
    let trackName: String?
    let videoName: String?
    let aiUsageWarning: String?
    let agentRunning: Bool?
    let commerceHint: String?
    let syncPaused: Bool?
    let creatorLimitZone: Bool?
    let workflowWaiting: Bool?
    let recoverableError: String?
    let contentSignal: String?

    private var missingCount: Int { [hasTrackSignal, hasVideoSignal].filter { !$0 }.count }
    private var nowText: String {
        if hasTrackSignal, let trackName, !trackName.isEmpty {
            return String(
                format: AppLocalized.text("home.live.now.music", fallback: "Now: Music live - %@"),
                trackName
            )
        }
        if hasVideoSignal, let videoName, !videoName.isEmpty {
            return String(
                format: AppLocalized.text("home.live.now.visual", fallback: "Now: Visual live - %@"),
                videoName
            )
        }
        return AppLocalized.text("home.live.now.empty", fallback: "Now: No core signal live yet.")
    }
    private var nextText: String {
        if !hasTrackSignal {
            return AppLocalized.text("home.live.next.track", fallback: "Next: Finalize the music signal.")
        }
        if !hasVideoSignal {
            return AppLocalized.text("home.live.next.video", fallback: "Next: Finalize the video signal.")
        }
        return AppLocalized.text("home.live.next.focus", fallback: "Next: Keep focus and work directly in content.")
    }
    private var riskText: String? {
        guard missingCount > 0 else { return nil }
        return String(
            format: AppLocalized.text("home.live.risk", fallback: "Risk: %d core signal(s) missing right now."),
            missingCount
        )
    }
    private var federatedSignals: [String] {
        var signals: [String] = []
        if let aiUsageWarning, !aiUsageWarning.isEmpty { signals.append("AI: \(aiUsageWarning)") }
        if creatorLimitZone == true { signals.append("AI: Creator limit zone reached.") }
        if agentRunning == true { signals.append("AI: Agent currently running.") }
        if workflowWaiting == true { signals.append("AI: Workflow waiting for next step.") }
        if let commerceHint, !commerceHint.isEmpty { signals.append("Commerce: \(commerceHint)") }
        if syncPaused == true { signals.append("System: Sync currently paused.") }
        if let recoverableError, !recoverableError.isEmpty { signals.append("System: \(recoverableError)") }
        if let contentSignal, !contentSignal.isEmpty {
            signals.append(
                String(
                    format: AppLocalized.text("home.federated.content", fallback: "Content: %@"),
                    contentSignal
                )
            )
        }
        return signals
    }
    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            Text(
                AppLocalized.text("home.status.signals", fallback: "Status / signals")
            )
                .font(.caption2.weight(.medium))
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.5))
            Text(nowText)
                .font(.footnote)
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.66))
            Text(nextText)
                .font(.footnote)
                .foregroundColor(AppColors.text(for: colorScheme).opacity(0.55))
            if let riskText {
                Text(riskText)
                    .font(.footnote)
                    .foregroundColor(AppColors.text(for: colorScheme).opacity(0.5))
            }
            ForEach(federatedSignals, id: \.self) { signal in
                Text(signal)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
            }
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 2)
    }
}
