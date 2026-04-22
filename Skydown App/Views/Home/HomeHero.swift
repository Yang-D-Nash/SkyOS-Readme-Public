import SwiftUI

struct HomeMapBackdrop: View {
    let colorScheme: ColorScheme

    var body: some View {
        ZStack {
            HomeBackdropHalo(tint: AppColors.accent(for: colorScheme), size: 280, opacity: colorScheme == .dark ? 0.09 : 0.07)
                .offset(x: 168, y: -138)
            HomeBackdropHalo(tint: AppColors.accentMystic(for: colorScheme), size: 240, opacity: colorScheme == .dark ? 0.10 : 0.08)
                .offset(x: -172, y: 210)
            HomeBackdropHalo(tint: AppColors.accentHighlight(for: colorScheme), size: 320, opacity: colorScheme == .dark ? 0.08 : 0.06)
                .offset(x: 154, y: 498)
        }
        .allowsHitTesting(false)
    }
}

private struct HomeBackdropHalo: View {
    let tint: Color
    let size: CGFloat
    let opacity: Double

    var body: some View {
        ZStack {
            Circle().fill(tint.opacity(opacity)).frame(width: size, height: size).blur(radius: 34)
            ForEach(0..<3, id: \.self) { index in
                Circle()
                    .stroke(tint.opacity(opacity - Double(index) * 0.02), lineWidth: 1)
                    .frame(width: size * (1 - CGFloat(index) * 0.22), height: size * (1 - CGFloat(index) * 0.22))
            }
        }
    }
}

struct HomeHeroIntroCard: View {
    @ObservedObject var viewModel: HomeViewModel
    let colorScheme: ColorScheme
    let onOpenTrack: () -> Void
    let onOpenBeat: () -> Void
    let onOpenVideo: () -> Void
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared

    private enum HeroPriorityTarget { case track, beat, video }

    private var activeSignalCount: Int {
        [viewModel.featuredTrack != nil, viewModel.featuredBeat != nil, viewModel.featuredVideo != nil].filter { $0 }.count
    }
    private var daypartGreeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour { case 5..<12: return "Guten Morgen"; case 12..<18: return "Guten Tag"; default: return "Guten Abend" }
    }
    private var dynamicTitle: String { "\(daypartGreeting), Creator." }
    private var dynamicSubtitle: String {
        let signalLine = activeSignalCount == 3 ? "Alle Kernsignale sind live." : "Nicht alle Kernsignale sind live."
        return "Weitblick oben. Fokus unten. \(signalLine)"
    }
    private var dynamicDetail: String { "\(activeSignalCount)/3 live · Daily Ops bereit" }
    private var heroPriorityTarget: HeroPriorityTarget {
        if viewModel.featuredTrack == nil { return .track }
        if viewModel.featuredBeat == nil { return .beat }
        if viewModel.featuredVideo == nil { return .video }
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour { case 5..<12: return .track; case 12..<18: return .beat; default: return .video }
    }
    private func heroPillTint(for target: HeroPriorityTarget) -> Color {
        let base: Color
        switch target {
        case .track: base = AppColors.accent(for: colorScheme)
        case .beat: base = AppColors.accentMystic(for: colorScheme)
        case .video: base = AppColors.accentHighlight(for: colorScheme)
        }
        return heroPriorityTarget == target ? base : base.opacity(0.66)
    }

    var body: some View {
        BrandHeroSurface(
            colorScheme: colorScheme,
            eyebrow: screenHeaderSettingsStore.settings.resolvedHomeEyebrow ?? "SkyOS Home",
            title: screenHeaderSettingsStore.settings.resolvedHomeTitle ?? dynamicTitle,
            subtitle: screenHeaderSettingsStore.settings.resolvedHomeSubtitle ?? dynamicSubtitle,
            detail: screenHeaderSettingsStore.settings.resolvedHomeDetail ?? dynamicDetail,
            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedHomeImageURL,
            accent: AppColors.accent(for: colorScheme),
            secondaryAccent: AppColors.accentMystic(for: colorScheme),
            marks: [.skydownX22]
        ) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    BrandHeroPill(
                        text: heroPriorityTarget == .track ? (viewModel.featuredTrack == nil ? "Next: Musik laden" : "Next: Musik") : (viewModel.featuredTrack == nil ? "Musik laedt" : "Musik live"),
                        colorScheme: colorScheme,
                        tint: heroPillTint(for: .track),
                        onTap: onOpenTrack
                    )
                    BrandHeroPill(
                        text: heroPriorityTarget == .beat ? (viewModel.featuredBeat == nil ? "Next: Beats laden" : "Next: Beats") : (viewModel.featuredBeat == nil ? "Beats laden" : "Beats live"),
                        colorScheme: colorScheme,
                        tint: heroPillTint(for: .beat),
                        onTap: onOpenBeat
                    )
                    BrandHeroPill(
                        text: heroPriorityTarget == .video ? (viewModel.featuredVideo == nil ? "Next: Visual laden" : "Next: Visual") : (viewModel.featuredVideo == nil ? "Video laedt" : "Video live"),
                        colorScheme: colorScheme,
                        tint: heroPillTint(for: .video),
                        onTap: onOpenVideo
                    )
                }
            }
        }
    }
}
