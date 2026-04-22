import SwiftUI

struct HomeSectionBanner: View {
    let title: String
    let subtitle: String
    let icon: String
    let colorScheme: ColorScheme
    let accent: Color

    var body: some View {
        HStack(spacing: 10) {
            ZStack {
                Circle().fill(accent.opacity(colorScheme == .dark ? 0.16 : 0.14))
                Image(systemName: icon).font(.caption.weight(.bold)).foregroundColor(accent)
            }
            .frame(width: 24, height: 24)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.subheadline.weight(.bold)).foregroundColor(AppColors.text(for: colorScheme))
                Text(subtitle).font(.caption).foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
            Spacer()
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.72)))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(accent.opacity(0.14), lineWidth: 1))
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
    let hasBeatSignal: Bool
    let hasVideoSignal: Bool
    let onRefresh: () -> Void
    let onOpenRelease: () -> Void
    let onOpenBeat: () -> Void
    let onOpenVideo: () -> Void

    private enum HomePriorityTarget { case music, beats, visuals }
    private var currentHour: Int { Calendar.current.component(.hour, from: Date()) }
    private var priorityTarget: HomePriorityTarget {
        if !hasTrackSignal { return .music }
        if !hasBeatSignal { return .beats }
        if !hasVideoSignal { return .visuals }
        switch currentHour { case 5..<12: return .music; case 12..<18: return .beats; default: return .visuals }
    }
    private var priorityTitle: String {
        switch priorityTarget {
        case .music: return hasTrackSignal ? "Jetzt wichtig: Musik" : "Jetzt wichtig: Musik herstellen"
        case .beats: return hasBeatSignal ? "Jetzt wichtig: Beats" : "Jetzt wichtig: Beats herstellen"
        case .visuals: return hasVideoSignal ? "Jetzt wichtig: Visuals" : "Jetzt wichtig: Visuals herstellen"
        }
    }
    private func triggerPriorityAction() {
        switch priorityTarget { case .music: onOpenRelease(); case .beats: onOpenBeat(); case .visuals: onOpenVideo() }
    }
    private var priorityHint: String {
        switch priorityTarget {
        case .music: return hasTrackSignal ? "Morgens zuerst Musik-Status checken." : "Musik-Signal ist noch nicht live."
        case .beats: return hasBeatSignal ? "Tagsueber zuerst Beats fokussieren." : "Beats-Signal fehlt noch."
        case .visuals: return hasVideoSignal ? "Abends zuerst Visuals-Status pruefen." : "Visuals-Signal fehlt noch."
        }
    }
    private var priorityAccent: Color {
        switch priorityTarget {
        case .music: return AppColors.accent(for: colorScheme)
        case .beats: return AppColors.accentMystic(for: colorScheme)
        case .visuals: return AppColors.accentHighlight(for: colorScheme)
        }
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "gauge.with.dots.needle.50percent").font(.caption.weight(.bold)).foregroundColor(AppColors.accent(for: colorScheme))
                Text("Priority Layer").font(.subheadline.weight(.bold)).foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
                Text("\(activeSignalCount)/\(totalSignalCount) live").font(.caption2.weight(.bold)).foregroundColor(priorityAccent).padding(.horizontal, 8).padding(.vertical, 5).background(Capsule(style: .continuous).fill(priorityAccent.opacity(0.12)))
            }
            Text("Ein Fokus. Ein klarer naechster Schritt.").font(.footnote).foregroundColor(AppColors.secondaryText(for: colorScheme))
            Button(action: triggerPriorityAction) { Label(priorityTitle, systemImage: "target").font(.caption.weight(.bold)).frame(maxWidth: .infinity) }
                .simultaneousGesture(TapGesture().onEnded { SkydownHaptics.selection() })
                .buttonStyle(.borderedProminent).tint(priorityAccent).controlSize(.small)
            Text(priorityHint).font(.caption).foregroundColor(AppColors.secondaryText(for: colorScheme))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    HomeStatusCapsule(title: hasTrackSignal ? "Music live" : "Music missing", accent: AppColors.accent(for: colorScheme), isActive: hasTrackSignal, colorScheme: colorScheme)
                    HomeStatusCapsule(title: hasBeatSignal ? "Beats live" : "Beats missing", accent: AppColors.accentMystic(for: colorScheme), isActive: hasBeatSignal, colorScheme: colorScheme)
                    HomeStatusCapsule(title: hasVideoSignal ? "Visuals live" : "Visuals missing", accent: AppColors.accentHighlight(for: colorScheme), isActive: hasVideoSignal, colorScheme: colorScheme)
                }
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(priorityAccent.opacity(0.16), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct HomeCommandDockStrip: View {
    let colorScheme: ColorScheme
    let priorityTarget: String
    let onOpenWorkflow: (() -> Void)?
    let onOpenCart: () -> Void
    let onOpenSettings: () -> Void
    private var priorityAccent: Color { priorityTarget == "music" ? AppColors.accent(for: colorScheme) : (priorityTarget == "beats" ? AppColors.accentMystic(for: colorScheme) : AppColors.accentHighlight(for: colorScheme)) }
    private func actionTint(_ target: String) -> Color {
        let base = target == "music" ? AppColors.accent(for: colorScheme) : (target == "beats" ? AppColors.accentMystic(for: colorScheme) : AppColors.accentHighlight(for: colorScheme))
        return target == priorityTarget ? base : base.opacity(0.66)
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "command").font(.caption.weight(.bold)).foregroundColor(priorityAccent)
                Text("Action Layer").font(.subheadline.weight(.bold)).foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
            }
            Text("Schnelle Systemaktionen ohne Kontextwechsel.").font(.footnote).foregroundColor(AppColors.secondaryText(for: colorScheme))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    if let onOpenWorkflow {
                        Button("KI-Agent", action: onOpenWorkflow).simultaneousGesture(TapGesture().onEnded { SkydownHaptics.selection() }).buttonStyle(.bordered).tint(actionTint("beats"))
                    }
                    Button("Warenkorb", action: onOpenCart).simultaneousGesture(TapGesture().onEnded { SkydownHaptics.selection() }).buttonStyle(.bordered).tint(actionTint("music"))
                    Button("Einstellungen", action: onOpenSettings).simultaneousGesture(TapGesture().onEnded { SkydownHaptics.selection() }).buttonStyle(.bordered).tint(actionTint("visuals"))
                }
            }
            .controlSize(.small)
        }
        .padding(10)
        .background(AppColors.cardBackground(for: colorScheme).opacity(0.35))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(priorityAccent.opacity(0.12), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct HomeLiveSignalSurface: View {
    let colorScheme: ColorScheme
    let hasTrackSignal: Bool
    let hasBeatSignal: Bool
    let hasVideoSignal: Bool
    let trackName: String?
    let beatName: String?
    let videoName: String?
    let aiUsageWarning: String? = nil
    let agentRunning: Bool? = nil
    let commerceHint: String? = nil
    let syncPaused: Bool? = nil
    let creatorLimitZone: Bool? = nil
    let workflowWaiting: Bool? = nil
    let recoverableError: String? = nil
    let contentSignal: String? = nil

    private var missingCount: Int { [hasTrackSignal, hasBeatSignal, hasVideoSignal].filter { !$0 }.count }
    private var nowText: String {
        if hasTrackSignal, let trackName, !trackName.isEmpty { return "Now: Music live - \(trackName)" }
        if hasBeatSignal, let beatName, !beatName.isEmpty { return "Now: Beat live - \(beatName)" }
        if hasVideoSignal, let videoName, !videoName.isEmpty { return "Now: Visual live - \(videoName)" }
        return "Now: Noch kein Kernsignal live."
    }
    private var nextText: String {
        if !hasTrackSignal { return "Next: Musik-Signal finalisieren." }
        if !hasBeatSignal { return "Next: Beat-Signal finalisieren." }
        if !hasVideoSignal { return "Next: Visual-Signal finalisieren." }
        return "Next: Fokus halten und direkt im Content arbeiten."
    }
    private var riskText: String? { missingCount > 0 ? "Risk: \(missingCount) Kernsignal(e) fehlen aktuell." : nil }
    private var federatedSignals: [String] {
        var signals: [String] = []
        if let aiUsageWarning, !aiUsageWarning.isEmpty { signals.append("AI: \(aiUsageWarning)") }
        if creatorLimitZone == true { signals.append("AI: Creator limit zone reached.") }
        if agentRunning == true { signals.append("AI: Agent currently running.") }
        if workflowWaiting == true { signals.append("AI: Workflow waiting for next step.") }
        if let commerceHint, !commerceHint.isEmpty { signals.append("Commerce: \(commerceHint)") }
        if syncPaused == true { signals.append("System: Sync currently paused.") }
        if let recoverableError, !recoverableError.isEmpty { signals.append("System: \(recoverableError)") }
        if let contentSignal, !contentSignal.isEmpty { signals.append("Content: \(contentSignal)") }
        return signals
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Live Signals").font(.caption.weight(.semibold)).foregroundColor(AppColors.text(for: colorScheme).opacity(0.86))
            Text(nowText).font(.footnote).foregroundColor(AppColors.text(for: colorScheme).opacity(0.80))
            Text(nextText).font(.footnote).foregroundColor(AppColors.secondaryText(for: colorScheme))
            if let riskText { Text(riskText).font(.footnote).foregroundColor(AppColors.secondaryText(for: colorScheme)) }
            ForEach(federatedSignals, id: \.self) { signal in
                Text(signal).font(.footnote).foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.58))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
