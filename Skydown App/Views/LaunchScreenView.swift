//
//  LaunchScreenView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 21.08.25.
//

import AVFoundation
import SwiftUI
import UIKit

struct LaunchScreenView: View {
    @State private var phase: LaunchPhase = .intro
    @State private var player: AVPlayer?
    @State private var hasCompletedIntro = false
    @State private var hasTrackedOnboardingStarted = false
    @State private var hasTrackedOnboardingCompleted = false
    @State private var hasTrackedFirstValueMoment = false
    private let growthTracker = MembershipAnalyticsTracker()

    var body: some View {
        Group {
            switch phase {
            case .intro:
                introContent
            case .landing:
                LaunchLandingView(
                    onOpenMusic: { openShell(tab: .zweizwei) },
                    onOpenVideography: { openShell(tab: .skydown) },
                    onOpenShop: { openShell(tab: .merch) }
                )
            case .shell(let initialTab):
                MainTabView(initialTab: initialTab)
            }
        }
        .onAppear {
            guard player == nil else { return }
            if !hasTrackedOnboardingStarted {
                hasTrackedOnboardingStarted = true
                growthTracker.track("onboarding_started", surface: "launch_intro")
            }
            if let url = Bundle.main.url(forResource: "Intro Launch", withExtension: "mp4") {
                player = AVPlayer(url: url)
            } else {
                skydownDebugLog("Video 'Intro Launch.mp4' nicht gefunden!")
                transitionToLanding()
            }
        }
        .onDisappear {
            player?.pause()
            deactivateIntroAudioSession()
        }
    }

    @ViewBuilder
    private var introContent: some View {
        if let player = player {
            ZStack {
                Color.black
                    .ignoresSafeArea()

                IntroVideoSurface(player: player)
                    .ignoresSafeArea()

                LinearGradient(
                    colors: [
                        Color.black.opacity(0.10),
                        Color.clear,
                        Color.black.opacity(0.18)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
            }
            .onAppear {
                configureIntroAudioSession()
                player.volume = 1.0
                player.isMuted = false
                player.play()
            }
            .onReceive(
                NotificationCenter.default.publisher(
                    for: .AVPlayerItemDidPlayToEndTime,
                    object: player.currentItem
                )
            ) { _ in
                transitionToLanding()
            }
            .onReceive(
                NotificationCenter.default.publisher(
                    for: .AVPlayerItemFailedToPlayToEndTime,
                    object: player.currentItem
                )
            ) { _ in
                transitionToLanding()
            }
        } else {
            Color.black
                .ignoresSafeArea()
        }
    }

    private func configureIntroAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .moviePlayback, options: [])
            try session.setActive(true)
        } catch {
            skydownDebugLog("Audio-Session fuer Intro konnte nicht aktiviert werden: \(error.localizedDescription)")
        }
    }

    private func deactivateIntroAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
        } catch {
            skydownDebugLog("Audio-Session fuer Intro konnte nicht deaktiviert werden: \(error.localizedDescription)")
        }
    }

    private func transitionToLanding() {
        guard !hasCompletedIntro else { return }
        hasCompletedIntro = true
        if !hasTrackedOnboardingCompleted {
            hasTrackedOnboardingCompleted = true
            growthTracker.track("onboarding_completed", surface: "launch_landing")
        }
        player?.pause()
        deactivateIntroAudioSession()
        withAnimation(SkydownMotion.contentReveal) {
            phase = .landing
        }
    }

    private func openShell(tab: MainTab) {
        if !hasTrackedFirstValueMoment {
            hasTrackedFirstValueMoment = true
            growthTracker.track("first_value_moment", surface: "launch_entry_\(surfaceName(for: tab))")
        }
        withAnimation(SkydownMotion.screenTransition) {
            phase = .shell(tab)
        }
    }

    private func surfaceName(for tab: MainTab) -> String {
        switch tab {
        case .hub:
            return "home"
        case .zweizwei:
            return "music"
        case .skydown:
            return "video"
        case .merch:
            return "shop"
        case .tools:
            return "ai"
        }
    }
}

private enum LaunchPhase {
    case intro
    case landing
    case shell(MainTab)
}

private struct LaunchLandingView: View {
    let onOpenMusic: () -> Void
    let onOpenVideography: () -> Void
    let onOpenShop: () -> Void
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared

    private var hubColorScheme: ColorScheme { .dark }

    var body: some View {
        GeometryReader { proxy in
            let layout = SkydownResponsiveLayout(availableWidth: proxy.size.width)
            let contentWidth = min(
                layout.contentMaxWidth,
                max(proxy.size.width - (layout.horizontalPadding * 2), 0)
            )
            let sectionCadence = layout.prefersDesktopChrome ? layout.sectionSpacing + 2 : max(layout.sectionSpacing - 2, 12)
            let compactVerticalPadding = layout.prefersDesktopChrome ? 34.0 : 22.0

            ZStack {
                LinearGradient(
                    colors: [
                        Color(red: 10/255, green: 14/255, blue: 20/255),
                        Color(red: 16/255, green: 22/255, blue: 30/255),
                        Color(red: 22/255, green: 30/255, blue: 42/255),
                        Color(red: 32/255, green: 48/255, blue: 68/255).opacity(0.32),
                        Color(red: 24/255, green: 32/255, blue: 44/255),
                        Color(red: 14/255, green: 18/255, blue: 26/255)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                LinearGradient(
                    colors: [
                        Color.black.opacity(0.14),
                        Color.black.opacity(0.04),
                        Color.black.opacity(0.18)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()

                Circle()
                    .fill(Color(red: 91/255, green: 149/255, blue: 216/255).opacity(0.08))
                    .frame(width: layout.prefersDesktopChrome ? 360 : 260, height: layout.prefersDesktopChrome ? 360 : 260)
                    .blur(radius: 70)
                    .offset(x: proxy.size.width * 0.26, y: -280)

                Circle()
                    .fill(Color(red: 138/255, green: 170/255, blue: 206/255).opacity(0.055))
                    .frame(width: layout.prefersDesktopChrome ? 320 : 240, height: layout.prefersDesktopChrome ? 320 : 240)
                    .blur(radius: 76)
                    .offset(x: -proxy.size.width * 0.18, y: 280)

                Ellipse()
                    .fill(
                        LinearGradient(
                            colors: [
                                AppColors.accentMystic(for: hubColorScheme).opacity(0.18),
                                AppColors.spotify(for: hubColorScheme).opacity(0.04)
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .frame(width: layout.prefersDesktopChrome ? 520 : 320, height: layout.prefersDesktopChrome ? 260 : 180)
                    .blur(radius: 48)
                    .offset(x: proxy.size.width * 0.16, y: 210)

                RoundedRectangle(cornerRadius: 44, style: .continuous)
                    .fill(AppColors.spotify(for: hubColorScheme).opacity(0.06))
                    .frame(width: layout.prefersDesktopChrome ? 320 : 220, height: layout.prefersDesktopChrome ? 440 : 320)
                    .blur(radius: 40)
                    .rotationEffect(.degrees(18))
                    .offset(x: -proxy.size.width * 0.24, y: -160)

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: sectionCadence) {
                        HStack(spacing: 8) {
                            LaunchLandingMetaPill(text: "SkyOS", accent: AppColors.accent(for: hubColorScheme))
                            if layout.prefersTwoColumn || layout.prefersThreeColumn {
                                LaunchLandingMetaPill(text: "Living System", accent: AppColors.accentMystic(for: hubColorScheme))
                            }
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            BrandHeroSurface(
                                colorScheme: hubColorScheme,
                                eyebrow: screenHeaderSettingsStore.settings.resolvedHomeEyebrow ?? "Willkommen",
                                title: screenHeaderSettingsStore.settings.resolvedHomeTitle ?? "SkyOS",
                                subtitle: screenHeaderSettingsStore.settings.resolvedHomeSubtitle ?? "Ein ruhiger Einstieg in Music, Video und Merch.",
                                detail: screenHeaderSettingsStore.settings.resolvedHomeDetail ?? "Alles greift ineinander — dein naechster Move folgt direkt hier, Music ist der empfohlene Einstieg.",
                                backgroundImageURL: screenHeaderSettingsStore.settings.resolvedHomeImageURL,
                                accent: AppColors.accent(for: hubColorScheme),
                                secondaryAccent: AppColors.accentMystic(for: hubColorScheme),
                                marks: [.skydown]
                            ) {
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack(spacing: 10) {
                                        BrandHeroPill(
                                            text: "Music",
                                            colorScheme: hubColorScheme,
                                            tint: AppColors.spotify(for: hubColorScheme)
                                        )
                                        BrandHeroPill(
                                            text: "Video",
                                            colorScheme: hubColorScheme,
                                            tint: AppColors.accentMystic(for: hubColorScheme)
                                        )
                                        BrandHeroPill(
                                            text: "Merch",
                                            colorScheme: hubColorScheme,
                                            tint: AppColors.accentHighlight(for: hubColorScheme)
                                        )
                                    }

                                    Text("Drei Einstiege, eine Oberflaeche — ohne Dashboard-Gefuehl.")
                                        .font(AppTypography.editorialCaption)
                                        .foregroundColor(.white.opacity(0.82))
                                }
                            }

                            Group {
                                VStack(alignment: .leading, spacing: 10) {
                                Text("Music ist der natuerliche Start; Video und Merch bleiben als ruhige Alternativen.")
                                    .font(.subheadline)
                                    .foregroundColor(.white.opacity(0.62))
                                    .fixedSize(horizontal: false, vertical: true)
                                    .padding(.top, 2)

                            if layout.prefersThreeColumn {
                                HStack(alignment: .top, spacing: layout.sectionSpacing) {
                                    LaunchLandingButton(
                                        eyebrow: "Music",
                                        title: "Music",
                                        subtitle: "Artists · Beats · Studio",
                                        detail: "Wenn du direkt in kreative Tiefe starten willst.",
                                        accent: AppColors.spotify(for: hubColorScheme),
                                        brandMark: .zweizwei,
                                        badges: ["Catalog", "Beats", "Studio"],
                                        emphasis: .primary,
                                        pathLabel: "Empfohlener Einstieg",
                                        ctaLabel: "In Music eintreten",
                                        accessibilityID: "launch.open_music",
                                        action: onOpenMusic
                                    )
                                    .frame(maxWidth: .infinity)
                                    .layoutPriority(2)
                                    .offset(y: -2)
                                    LaunchLandingButton(
                                        eyebrow: "Video",
                                        title: "Videos",
                                        subtitle: "Reels · Clips · YouTube",
                                        detail: "Wenn du Story, Bewegung und Playback zuerst erleben willst.",
                                        accent: AppColors.accentMystic(for: hubColorScheme),
                                        brandMark: .zweizwei,
                                        badges: ["Playback", "Reels", "YouTube"],
                                        emphasis: .secondary,
                                        pathLabel: "",
                                        ctaLabel: "In Videos eintreten",
                                        accessibilityID: "launch.open_video",
                                        action: onOpenVideography
                                    )
                                    .frame(maxWidth: .infinity)
                                    .layoutPriority(1)
                                    LaunchLandingButton(
                                        eyebrow: "Store",
                                        title: "Merch",
                                        subtitle: "Drops · Checkout",
                                        detail: "Wenn du direkt in Produkte, Fits und den klaren Kaufweg willst.",
                                        accent: AppColors.accentHighlight(for: hubColorScheme),
                                        brandMark: .skydownX22,
                                        badges: ["Drops", "Fits", "Checkout"],
                                        emphasis: .secondary,
                                        pathLabel: "",
                                        ctaLabel: "In Merch eintreten",
                                        accessibilityID: "launch.open_shop",
                                        action: onOpenShop
                                    )
                                    .frame(maxWidth: .infinity)
                                    .layoutPriority(1)
                                }
                            } else if layout.prefersTwoColumn {
                                VStack(spacing: 10) {
                                    LaunchLandingButton(
                                        eyebrow: "Music",
                                        title: "Music",
                                        subtitle: "Artists · Beats · Studio",
                                        detail: "Wenn du direkt in kreative Tiefe starten willst.",
                                        accent: AppColors.spotify(for: hubColorScheme),
                                        brandMark: .zweizwei,
                                        badges: ["Catalog", "Beats", "Studio"],
                                        emphasis: .primary,
                                        pathLabel: "Empfohlener Einstieg",
                                        ctaLabel: "In Music eintreten",
                                        accessibilityID: "launch.open_music",
                                        action: onOpenMusic
                                    )
                                    .offset(y: -2)

                                    HStack(alignment: .top, spacing: 10) {
                                        LaunchLandingButton(
                                            eyebrow: "Video",
                                            title: "Videos",
                                            subtitle: "Reels · Clips · YouTube",
                                            detail: "Wenn du Story, Bewegung und Playback zuerst erleben willst.",
                                            accent: AppColors.accentMystic(for: hubColorScheme),
                                            brandMark: .zweizwei,
                                            badges: ["Playback", "Reels", "YouTube"],
                                            emphasis: .secondary,
                                            pathLabel: "",
                                            ctaLabel: "In Videos eintreten",
                                            accessibilityID: "launch.open_video",
                                            action: onOpenVideography
                                        )
                                        LaunchLandingButton(
                                            eyebrow: "Store",
                                            title: "Merch",
                                            subtitle: "Drops · Checkout",
                                            detail: "Wenn du direkt in Produkte, Fits und den klaren Kaufweg willst.",
                                            accent: AppColors.accentHighlight(for: hubColorScheme),
                                            brandMark: .skydownX22,
                                            badges: ["Drops", "Fits", "Checkout"],
                                            emphasis: .secondary,
                                            pathLabel: "",
                                            ctaLabel: "In Merch eintreten",
                                            accessibilityID: "launch.open_shop",
                                            action: onOpenShop
                                        )
                                    }
                                }
                            } else {
                                VStack(spacing: 10) {
                                    LaunchLandingButton(
                                        eyebrow: "Music",
                                        title: "Music",
                                        subtitle: "Artists · Beats · Studio",
                                        detail: "Wenn du direkt in kreative Tiefe starten willst.",
                                        accent: AppColors.spotify(for: hubColorScheme),
                                        brandMark: .zweizwei,
                                        badges: ["Catalog", "Beats", "Studio"],
                                        emphasis: .primary,
                                        pathLabel: "Empfohlener Einstieg",
                                        ctaLabel: "In Music eintreten",
                                        accessibilityID: "launch.open_music",
                                        action: onOpenMusic
                                    )
                                    .offset(y: -2)

                                    HStack(alignment: .top, spacing: 10) {
                                        LaunchLandingButton(
                                            eyebrow: "Video",
                                            title: "Videos",
                                            subtitle: "Reels · Clips · YouTube",
                                            detail: "Wenn du Story, Bewegung und Playback zuerst erleben willst.",
                                            accent: AppColors.accentMystic(for: hubColorScheme),
                                            brandMark: .zweizwei,
                                            badges: ["Playback", "Reels", "YouTube"],
                                            emphasis: .secondary,
                                            pathLabel: "",
                                            ctaLabel: "In Videos eintreten",
                                            accessibilityID: "launch.open_video",
                                            action: onOpenVideography
                                        )
                                        LaunchLandingButton(
                                            eyebrow: "Store",
                                            title: "Merch",
                                            subtitle: "Drops · Checkout",
                                            detail: "Wenn du direkt in Produkte, Fits und den klaren Kaufweg willst.",
                                            accent: AppColors.accentHighlight(for: hubColorScheme),
                                            brandMark: .skydownX22,
                                            badges: ["Drops", "Fits", "Checkout"],
                                            emphasis: .secondary,
                                            pathLabel: "",
                                            ctaLabel: "In Merch eintreten",
                                            accessibilityID: "launch.open_shop",
                                            action: onOpenShop
                                        )
                                    }
                                }
                            }
                                }
                            }
                        }

                        LaunchLandingVisionBand(colorScheme: hubColorScheme)
                            .padding(.top, 6)

                        if layout.prefersThreeColumn {
                            HStack(spacing: 12) {
                                LaunchLandingNarrativeStep(
                                    title: "01",
                                    statement: "SkyOS startet weit und ruhig statt laut und technisch.",
                                    accent: AppColors.accent(for: hubColorScheme)
                                )
                                LaunchLandingNarrativeStep(
                                    title: "02",
                                    statement: "Jeder Bereich bleibt verbunden, damit Orientierung nie bricht.",
                                    accent: AppColors.accentMystic(for: hubColorScheme)
                                )
                                LaunchLandingNarrativeStep(
                                    title: "03",
                                    statement: "Kontrolle fuehlt sich persoenlich an, nicht wie ein Dashboard.",
                                    accent: AppColors.accentHighlight(for: hubColorScheme)
                                )
                            }
                            .padding(.top, 8)
                        } else {
                            VStack(spacing: 10) {
                                LaunchLandingNarrativeStep(
                                    title: "01",
                                    statement: "SkyOS startet weit und ruhig statt laut und technisch.",
                                    accent: AppColors.accent(for: hubColorScheme)
                                )
                                LaunchLandingNarrativeStep(
                                    title: "02",
                                    statement: "Bereiche bleiben verbunden, damit Orientierung nie bricht.",
                                    accent: AppColors.accentMystic(for: hubColorScheme)
                                )
                            }
                            .padding(.top, 8)
                        }

                        Text("Der Flow bleibt zusammen.")
                            .font(AppTypography.editorialFootnote)
                            .foregroundColor(.white.opacity(0.74))
                            .padding(.top, 2)
                    }
                    .frame(maxWidth: contentWidth, alignment: .leading)
                    .padding(.horizontal, layout.horizontalPadding)
                    .padding(.vertical, compactVerticalPadding)
                    .frame(maxWidth: .infinity)
                }
                .scrollBounceBehavior(.basedOnSize, axes: .vertical)
            }
        }
    }
}

private struct LaunchLandingButton: View {
    enum Emphasis {
        case primary
        case standard
        case secondary
    }

    let eyebrow: String
    let title: String
    let subtitle: String
    let detail: String
    let accent: Color
    let brandMark: BrandMark
    let badges: [String]
    let emphasis: Emphasis
    let pathLabel: String
    let ctaLabel: String
    let accessibilityID: String?
    let action: () -> Void

    private var hubColorScheme: ColorScheme { .dark }

    init(
        eyebrow: String,
        title: String,
        subtitle: String,
        detail: String,
        accent: Color,
        brandMark: BrandMark,
        badges: [String],
        emphasis: Emphasis = .standard,
        pathLabel: String = "",
        ctaLabel: String = "Eintreten",
        accessibilityID: String? = nil,
        action: @escaping () -> Void
    ) {
        self.eyebrow = eyebrow
        self.title = title
        self.subtitle = subtitle
        self.detail = detail
        self.accent = accent
        self.brandMark = brandMark
        self.badges = badges
        self.emphasis = emphasis
        self.pathLabel = pathLabel
        self.ctaLabel = ctaLabel
        self.accessibilityID = accessibilityID
        self.action = action
    }

    private var tileSpacing: CGFloat {
        switch emphasis {
        case .primary: return 16
        case .standard: return 13
        case .secondary: return 10
        }
    }

    private var tilePadding: CGFloat {
        switch emphasis {
        case .primary: return 20
        case .standard: return 17
        case .secondary: return 12
        }
    }

    private var minTileHeight: CGFloat {
        switch emphasis {
        case .primary: return 248
        case .standard: return 200
        case .secondary: return 150
        }
    }

    private var markSize: CGFloat {
        switch emphasis {
        case .primary, .standard: return 62
        case .secondary: return 46
        }
    }

    private var titleFont: Font {
        switch emphasis {
        case .primary: return AppTypography.sectionTitle
        case .standard: return AppTypography.sectionTitle
        case .secondary: return .headline.weight(.semibold)
        }
    }

    private var displayedBadges: [String] {
        switch emphasis {
        case .secondary: return Array(badges.prefix(2))
        default: return badges
        }
    }

    private var cornerRadius: CGFloat {
        emphasis == .secondary ? 18 : 24
    }

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: tileSpacing) {
                HStack(alignment: .top, spacing: emphasis == .secondary ? 11 : 14) {
                    Image(brandMark.imageName)
                        .resizable()
                        .renderingMode(.original)
                        .scaledToFit()
                        .opacity(emphasis == .secondary ? 0.52 : 0.62)
                        .padding(emphasis == .secondary ? 7 : 9)
                        .frame(width: markSize, height: markSize)
                        .background(
                            RoundedRectangle(cornerRadius: emphasis == .secondary ? 14 : 18, style: .continuous)
                                .fill(
                                    LinearGradient(
                                        colors: [
                                            Color.white.opacity(0.05),
                                            AppColors.cardBackground(for: hubColorScheme).opacity(0.94),
                                            accent.opacity(emphasis == .secondary ? 0.14 : 0.22)
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                        )
                        .shadow(color: accent.opacity(emphasis == .secondary ? 0.04 : 0.18), radius: emphasis == .secondary ? 0 : 10, y: emphasis == .secondary ? 0 : 3)

                    VStack(alignment: .leading, spacing: emphasis == .secondary ? 4 : 6) {
                        Text(emphasis == .secondary ? eyebrow : eyebrow.uppercased())
                            .font(emphasis == .secondary ? .caption.weight(.medium) : AppTypography.heroEyebrow)
                            .tracking(emphasis == .secondary ? 0.6 : 1.4)
                            .foregroundColor(accent.opacity(emphasis == .secondary ? 0.52 : 0.92))

                        Text(title)
                            .font(titleFont)
                            .foregroundColor(.white.opacity(emphasis == .secondary ? 0.86 : 1))
                            .shadow(color: .black.opacity(emphasis == .secondary ? 0.08 : 0.24), radius: emphasis == .secondary ? 4 : 10, y: emphasis == .secondary ? 2 : 3)
                            .lineLimit(2)
                            .minimumScaleFactor(0.88)

                        Text(subtitle)
                            .font(emphasis == .secondary ? .caption.weight(.medium) : AppTypography.editorialCaption)
                            .foregroundColor(.white.opacity(emphasis == .secondary ? 0.64 : 0.82))
                            .multilineTextAlignment(.leading)
                            .lineLimit(emphasis == .secondary ? 1 : 2)
                    }

                    Spacer(minLength: 6)

                    if emphasis == .secondary {
                        Image(systemName: "arrow.up.right")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.36))
                            .padding(.top, 2)
                    } else {
                        Image(systemName: "arrow.up.right")
                            .font(.headline.weight(.bold))
                            .foregroundColor(.white.opacity(0.82))
                            .padding(10)
                            .background(
                                Circle()
                                    .fill(accent.opacity(0.18))
                            )
                    }
                }

                Text(detail)
                    .font(AppTypography.editorialCaption)
                    .foregroundColor(.white.opacity(emphasis == .secondary ? 0.56 : 0.72))
                    .multilineTextAlignment(.leading)
                    .lineLimit(emphasis == .secondary ? 2 : 4)

                if !pathLabel.isEmpty {
                    Text(pathLabel.uppercased())
                        .font(.caption2.weight(.bold))
                        .foregroundColor(accent.opacity(0.92))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(
                            Capsule(style: .continuous)
                                .fill(accent.opacity(0.14))
                        )
                }

                HStack(spacing: 8) {
                    ForEach(displayedBadges, id: \.self) { badge in
                        Text(badge)
                            .font(emphasis == .secondary ? .caption2.weight(.semibold) : .caption.weight(.semibold))
                            .foregroundColor(accent)
                            .padding(.horizontal, emphasis == .secondary ? 9 : 11)
                            .padding(.vertical, emphasis == .secondary ? 5 : 7)
                            .background(
                                Capsule(style: .continuous)
                                    .fill(Color.white.opacity(0.05))
                            )
                            .overlay(
                                Capsule(style: .continuous)
                                    .stroke(accent.opacity(emphasis == .secondary ? 0.09 : 0.24), lineWidth: 1)
                            )
                    }
                }

                if emphasis == .secondary {
                    Text("Oeffnen")
                        .font(.caption.weight(.medium))
                        .foregroundColor(.white.opacity(0.42))
                } else {
                    HStack(spacing: 10) {
                        Text(ctaLabel)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(.white.opacity(0.94))

                        Spacer(minLength: 10)

                        LaunchLandingActionPill(
                            text: "Start",
                            accent: accent
                        )
                    }
                }
            }
            .padding(tilePadding)
            .frame(maxWidth: .infinity, minHeight: minTileHeight, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(emphasis == .primary ? 0.07 : (emphasis == .secondary ? 0.02 : 0.05)),
                                Color.black.opacity(emphasis == .secondary ? 0.72 : 0.82),
                                Color(red: 8/255, green: 14/255, blue: 24/255).opacity(emphasis == .secondary ? 0.88 : 0.94),
                                accent.opacity(emphasis == .primary ? 0.24 : (emphasis == .secondary ? 0.06 : 0.16))
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            )
            .overlay(alignment: .topTrailing) {
                if emphasis != .secondary {
                    Circle()
                        .fill(accent.opacity(0.22))
                        .frame(width: 132, height: 132)
                        .blur(radius: 30)
                        .offset(x: 28, y: -20)
                }
            }
            .overlay(alignment: .leading) {
                if emphasis != .secondary {
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(accent)
                        .frame(width: 4)
                        .padding(.vertical, 16)
                        .padding(.leading, 10)
                }
            }
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(
                        Color.white.opacity(emphasis == .primary ? 0.16 : (emphasis == .secondary ? 0.045 : 0.10)),
                        lineWidth: emphasis == .primary ? 1.2 : 1
                    )
            }
            .shadow(color: .black.opacity(emphasis == .primary ? 0.28 : (emphasis == .secondary ? 0.10 : 0.22)), radius: emphasis == .primary ? 16 : (emphasis == .secondary ? 6 : 14), y: emphasis == .secondary ? 3 : 8)
        }
        .accessibilityIdentifier(accessibilityID ?? "")
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private struct LaunchLandingMetaPill: View {
    let text: String
    let accent: Color

    var body: some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundColor(.white.opacity(0.90))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                Capsule(style: .continuous)
                    .fill(Color.white.opacity(0.05))
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(accent.opacity(0.28), lineWidth: 1)
            )
    }
}

private struct LaunchLandingActionPill: View {
    let text: String
    let accent: Color

    var body: some View {
        HStack(spacing: 6) {
            Text(text)
                .font(.caption.weight(.bold))
            Image(systemName: "arrow.up.right")
                .font(.caption.weight(.bold))
        }
        .foregroundColor(.white.opacity(0.94))
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(
            Capsule(style: .continuous)
                .fill(accent.opacity(0.20))
        )
        .overlay(
            Capsule(style: .continuous)
                .stroke(accent.opacity(0.30), lineWidth: 1)
        )
    }
}

private struct LaunchLandingVisionBand: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Sky trifft Weite. Down trifft Tiefe.")
                .font(.system(size: 24, weight: .semibold, design: .rounded))
                .foregroundColor(.white.opacity(0.96))
                .fixedSize(horizontal: false, vertical: true)

            Text("SkyOS verbindet Vision und Realitaet in einem ruhigen Einstieg, der modern wirkt und menschlich bleibt.")
                .font(AppTypography.editorialBody)
                .foregroundColor(.white.opacity(0.82))
                .fixedSize(horizontal: false, vertical: true)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    LaunchLandingInlineTag(text: "Premium Technologie", accent: AppColors.accent(for: colorScheme))
                    LaunchLandingInlineTag(text: "Persoenliche Kontrolle", accent: AppColors.accentMystic(for: colorScheme))
                }
            }
        }
        .padding(SkydownLayout.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(0.06),
                            AppColors.accent(for: colorScheme).opacity(0.10),
                            Color.black.opacity(0.20)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color.white.opacity(0.10), lineWidth: 1)
        )
    }
}

private struct LaunchLandingInlineTag: View {
    let text: String
    let accent: Color

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundColor(.white.opacity(0.90))
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(
                Capsule(style: .continuous)
                    .fill(accent.opacity(0.18))
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(accent.opacity(0.30), lineWidth: 1)
            )
    }
}

private struct LaunchLandingNarrativeStep: View {
    let title: String
    let statement: String
    let accent: Color

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(title)
                .font(.caption.weight(.bold))
                .foregroundColor(accent.opacity(0.95))
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
        .background(
                    Capsule(style: .continuous)
                        .fill(accent.opacity(0.16))
                )

            Text(statement)
                .font(AppTypography.editorialCaption)
                .foregroundColor(.white.opacity(0.86))
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(0.04),
                            Color.black.opacity(0.20)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
    }
}

private struct IntroVideoSurface: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> IntroPlayerView {
        let view = IntroPlayerView()
        view.playerLayer?.player = player
        return view
    }

    func updateUIView(_ uiView: IntroPlayerView, context: Context) {
        uiView.playerLayer?.player = player
    }
}

private final class IntroPlayerView: UIView {
    // swiftlint:disable:next static_over_final_class
    override class var layerClass: AnyClass {
        AVPlayerLayer.self
    }

    var playerLayer: AVPlayerLayer? {
        layer as? AVPlayerLayer
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .black
        playerLayer?.videoGravity = .resizeAspectFill
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        backgroundColor = .black
        playerLayer?.videoGravity = .resizeAspectFill
    }
}

#Preview {
    let services = AppServices()

    LaunchScreenView()
        .environmentObject(services)
        .environmentObject(services.authManager)
        .environmentObject(services.cartViewModel)
}
