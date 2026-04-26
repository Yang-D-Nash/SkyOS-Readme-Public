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
                    onOpenHome: { openShell(tab: .hub) },
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
    let onOpenHome: () -> Void
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
            let isShortHeightLayout = !layout.prefersDesktopChrome && proxy.size.height < 760
            /// Kompaktere Kacheln/Abstände, damit die Landing-Page auf kleinen iPhones ohne Scrollen passt.
            let isTightPhoneHeight = !layout.prefersDesktopChrome && proxy.size.height < 780
            let sectionCadence = layout.prefersDesktopChrome
                ? layout.sectionSpacing + 2
                : (isTightPhoneHeight
                    ? 6
                    : (isShortHeightLayout ? 10 : max(layout.sectionSpacing - 2, 12)))
            let compactVerticalPadding = layout.prefersDesktopChrome ? 34.0 : (isShortHeightLayout ? 14.0 : 22.0)
            let verticalTopPad = isTightPhoneHeight ? 8.0 : compactVerticalPadding
            let verticalBottomPad = (isTightPhoneHeight ? 8.0 : compactVerticalPadding) + proxy.safeAreaInsets.bottom
            let heroBlockSpacing: CGFloat = isTightPhoneHeight ? 5 : 8
            let quickActionStackSpacing: CGFloat = isTightPhoneHeight ? 6 : 10
            let compactCardSpacing = isShortHeightLayout ? 8.0 : 10.0
            let musicDetail = isShortHeightLayout
                ? AppLocalized.text("landing.music.detail", fallback: "Open music.")
                : AppLocalized.text("landing.music.detail", fallback: "Open music.")
            let videoDetail = isShortHeightLayout
                ? AppLocalized.text("landing.video.detail", fallback: "Open videos.")
                : AppLocalized.text("landing.video.detail", fallback: "Open videos.")
            let merchDetail = isShortHeightLayout
                ? AppLocalized.text("landing.merch.detail", fallback: "Open merch.")
                : AppLocalized.text("landing.merch.detail", fallback: "Open merch.")
            let musicBackgroundURL = screenHeaderSettingsStore.settings.resolvedMusicHubImageURL
            let videoBackgroundURL = screenHeaderSettingsStore.settings.resolvedVideoHubImageURL
            let merchBackgroundURL = screenHeaderSettingsStore.settings.resolvedShopImageURL

            ZStack {
                AppColors.primaryBackground(for: hubColorScheme)
                    .ignoresSafeArea()

                SkydownAtmosphereBackdrop(colorScheme: hubColorScheme, showsLuminanceScrim: false)

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

                VStack(alignment: .leading, spacing: sectionCadence) {
                    HStack(spacing: 8) {
                        LaunchLandingMetaPill(
                            text: "SkyOS",
                            accent: AppColors.accent(for: hubColorScheme),
                            onTap: onOpenHome
                        )
                        if layout.prefersTwoColumn || layout.prefersThreeColumn {
                            LaunchLandingMetaPill(
                                text: "Living System",
                                accent: AppColors.accentMystic(for: hubColorScheme),
                                onTap: onOpenVideography
                            )
                        }
                    }

                    VStack(alignment: .leading, spacing: heroBlockSpacing) {
                        BrandHeroSurface(
                            colorScheme: hubColorScheme,
                            eyebrow: screenHeaderSettingsStore.settings.resolvedHomeEyebrow ?? "Willkommen",
                            title: screenHeaderSettingsStore.settings.resolvedHomeTitle ?? "SkyOS",
                            subtitle: screenHeaderSettingsStore.settings.resolvedHomeSubtitle ?? AppLocalized.text("landing.home.subtitle", fallback: "Open music, videos and merch."),
                            detail: screenHeaderSettingsStore.settings.resolvedHomeDetail ?? AppLocalized.text("landing.home.detail", fallback: "Choose where you want to start."),
                            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedHomeImageURL,
                            accent: AppColors.accent(for: hubColorScheme),
                            secondaryAccent: AppColors.accentMystic(for: hubColorScheme),
                            marks: [],
                            edgeToEdge: true,
                            onSurfaceTap: onOpenHome
                        ) {
                            VStack(alignment: .leading, spacing: 8) {
                                Text(AppLocalized.text("landing.home.recommended", fallback: "Start with Home"))
                                    .font(.footnote.weight(.semibold))
                                    .foregroundColor(AppColors.spotify(for: hubColorScheme).opacity(0.96))
                                Text(AppLocalized.text("landing.home.tap", fallback: "Tap to open"))
                                    .font(.caption.weight(.medium))
                                    .foregroundColor(.white.opacity(0.78))
                            }
                        }
                        .accessibilityIdentifier("launch.hero.open_home")
                        .accessibilityLabel("Home hero öffnen")
                        .accessibilityHint("Öffnet den empfohlenen Einstieg Home")

                        VStack(spacing: quickActionStackSpacing) {
                            LaunchLandingActionButton(
                                title: AppLocalized.text("landing.home.open", fallback: "Open Home"),
                                systemImage: "house.fill",
                                style: .primary,
                                action: onOpenHome
                            )
                            HStack(spacing: compactCardSpacing) {
                                LaunchLandingActionButton(
                                    title: "Music",
                                    systemImage: "music.note",
                                    style: .secondary,
                                    action: onOpenMusic
                                )
                                LaunchLandingActionButton(
                                    title: "Merch",
                                    systemImage: "bag.fill",
                                    style: .secondary,
                                    action: onOpenShop
                                )
                            }
                        }

                        Group {
                            VStack(alignment: .leading, spacing: compactCardSpacing) {
                                if !isShortHeightLayout && !isTightPhoneHeight {
                                    Text(AppLocalized.text("landing.home.note", fallback: "Choose any section to start."))
                                        .font(.subheadline)
                                        .foregroundColor(.white.opacity(0.62))
                                        .fixedSize(horizontal: false, vertical: true)
                                        .padding(.top, 2)
                                }

                                if layout.prefersThreeColumn {
                                    HStack(alignment: .top, spacing: layout.sectionSpacing) {
                                        LaunchLandingButton(
                                            eyebrow: "Music",
                                            title: "Music",
                                            subtitle: AppLocalized.text("landing.music.subtitle", fallback: "Songs and artists"),
                                            detail: musicDetail,
                                            accent: AppColors.spotify(for: hubColorScheme),
                                            systemImage: "music.note",
                                            badges: [],
                                            emphasis: .primary,
                                            pathLabel: AppLocalized.text("landing.path.recommended", fallback: "Start"),
                                            ctaLabel: AppLocalized.text("landing.music.open", fallback: "Open"),
                                            accessibilityID: "launch.open_music",
                                        backgroundImageURL: musicBackgroundURL,
                                            useTightHeights: isTightPhoneHeight,
                                            action: onOpenMusic
                                        )
                                        .frame(maxWidth: .infinity)
                                        .layoutPriority(2)
                                        .offset(y: -2)

                                        LaunchLandingButton(
                                            eyebrow: "Video",
                                            title: "Videos",
                                            subtitle: AppLocalized.text("landing.video.subtitle", fallback: "Clips and reels"),
                                            detail: videoDetail,
                                            accent: AppColors.accentMystic(for: hubColorScheme),
                                            systemImage: "play.rectangle.fill",
                                            badges: [],
                                            emphasis: .secondary,
                                            pathLabel: "",
                                            ctaLabel: AppLocalized.text("landing.video.open", fallback: "Open"),
                                            accessibilityID: "launch.open_video",
                                        backgroundImageURL: videoBackgroundURL,
                                            useTightHeights: isTightPhoneHeight,
                                            action: onOpenVideography
                                        )
                                        .frame(maxWidth: .infinity)
                                        .layoutPriority(1)

                                        LaunchLandingButton(
                                            eyebrow: "Store",
                                            title: "Merch",
                                            subtitle: AppLocalized.text("landing.merch.subtitle", fallback: "Drops and checkout"),
                                            detail: merchDetail,
                                            accent: AppColors.accentHighlight(for: hubColorScheme),
                                            systemImage: "bag.fill",
                                            badges: [],
                                            emphasis: .secondary,
                                            pathLabel: "",
                                            ctaLabel: AppLocalized.text("landing.merch.open", fallback: "Open"),
                                            accessibilityID: "launch.open_shop",
                                        backgroundImageURL: merchBackgroundURL,
                                            useTightHeights: isTightPhoneHeight,
                                            action: onOpenShop
                                        )
                                        .frame(maxWidth: .infinity)
                                        .layoutPriority(1)
                                    }
                                } else if layout.prefersTwoColumn {
                                    VStack(spacing: compactCardSpacing) {
                                        LaunchLandingButton(
                                            eyebrow: "Music",
                                            title: "Music",
                                            subtitle: AppLocalized.text("landing.music.subtitle", fallback: "Songs and artists"),
                                            detail: musicDetail,
                                            accent: AppColors.spotify(for: hubColorScheme),
                                            systemImage: "music.note",
                                            badges: [],
                                            emphasis: .primary,
                                            pathLabel: AppLocalized.text("landing.path.recommended", fallback: "Start"),
                                            ctaLabel: AppLocalized.text("landing.music.open", fallback: "Open"),
                                            accessibilityID: "launch.open_music",
                                        backgroundImageURL: musicBackgroundURL,
                                            useTightHeights: isTightPhoneHeight,
                                            action: onOpenMusic
                                        )
                                        .offset(y: -2)

                                        HStack(alignment: .top, spacing: compactCardSpacing) {
                                            LaunchLandingButton(
                                                eyebrow: "Video",
                                                title: "Videos",
                                                subtitle: AppLocalized.text("landing.video.subtitle", fallback: "Clips and reels"),
                                                detail: videoDetail,
                                                accent: AppColors.accentMystic(for: hubColorScheme),
                                                systemImage: "play.rectangle.fill",
                                                badges: [],
                                                emphasis: .secondary,
                                                pathLabel: "",
                                                ctaLabel: AppLocalized.text("landing.video.open", fallback: "Open"),
                                                accessibilityID: "launch.open_video",
                                                backgroundImageURL: videoBackgroundURL,
                                                useTightHeights: isTightPhoneHeight,
                                                action: onOpenVideography
                                            )
                                            LaunchLandingButton(
                                                eyebrow: "Store",
                                                title: "Merch",
                                                subtitle: AppLocalized.text("landing.merch.subtitle", fallback: "Drops and checkout"),
                                                detail: merchDetail,
                                                accent: AppColors.accentHighlight(for: hubColorScheme),
                                                systemImage: "bag.fill",
                                                badges: [],
                                                emphasis: .secondary,
                                                pathLabel: "",
                                                ctaLabel: AppLocalized.text("landing.merch.open", fallback: "Open"),
                                                accessibilityID: "launch.open_shop",
                                                backgroundImageURL: merchBackgroundURL,
                                                useTightHeights: isTightPhoneHeight,
                                                action: onOpenShop
                                            )
                                        }
                                    }
                                } else {
                                    VStack(spacing: compactCardSpacing) {
                                        LaunchLandingButton(
                                            eyebrow: "Music",
                                            title: "Music",
                                            subtitle: AppLocalized.text("landing.music.subtitle", fallback: "Songs and artists"),
                                            detail: musicDetail,
                                            accent: AppColors.spotify(for: hubColorScheme),
                                            systemImage: "music.note",
                                            badges: [],
                                            emphasis: .primary,
                                            pathLabel: AppLocalized.text("landing.path.recommended", fallback: "Start"),
                                            ctaLabel: AppLocalized.text("landing.music.open", fallback: "Open"),
                                            accessibilityID: "launch.open_music",
                                            backgroundImageURL: musicBackgroundURL,
                                            useTightHeights: isTightPhoneHeight,
                                            action: onOpenMusic
                                        )
                                        .offset(y: -2)

                                        HStack(alignment: .top, spacing: compactCardSpacing) {
                                            LaunchLandingButton(
                                                eyebrow: "Video",
                                                title: "Videos",
                                                subtitle: AppLocalized.text("landing.video.subtitle", fallback: "Clips and reels"),
                                                detail: videoDetail,
                                                accent: AppColors.accentMystic(for: hubColorScheme),
                                                systemImage: "play.rectangle.fill",
                                                badges: [],
                                                emphasis: .secondary,
                                                pathLabel: "",
                                                ctaLabel: AppLocalized.text("landing.video.open", fallback: "Open"),
                                                accessibilityID: "launch.open_video",
                                                backgroundImageURL: videoBackgroundURL,
                                                useTightHeights: isTightPhoneHeight,
                                                action: onOpenVideography
                                            )
                                            LaunchLandingButton(
                                                eyebrow: "Store",
                                                title: "Merch",
                                                subtitle: AppLocalized.text("landing.merch.subtitle", fallback: "Drops and checkout"),
                                                detail: merchDetail,
                                                accent: AppColors.accentHighlight(for: hubColorScheme),
                                                systemImage: "bag.fill",
                                                badges: [],
                                                emphasis: .secondary,
                                                pathLabel: "",
                                                ctaLabel: AppLocalized.text("landing.merch.open", fallback: "Open"),
                                                accessibilityID: "launch.open_shop",
                                                backgroundImageURL: merchBackgroundURL,
                                                useTightHeights: isTightPhoneHeight,
                                                action: onOpenShop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .frame(maxWidth: contentWidth, alignment: .leading)
                .padding(.horizontal, layout.horizontalPadding)
                .padding(.top, verticalTopPad)
                .padding(.bottom, verticalBottomPad)
                .frame(maxWidth: .infinity, alignment: .leading)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
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
    let systemImage: String
    let badges: [String]
    let emphasis: Emphasis
    let pathLabel: String
    let ctaLabel: String
    let accessibilityID: String?
    let backgroundImageURL: String?
    let useTightHeights: Bool
    let action: () -> Void
    @State private var backgroundLuminance: Double?

    private var hubColorScheme: ColorScheme { .dark }

    init(
        eyebrow: String,
        title: String,
        subtitle: String,
        detail: String,
        accent: Color,
        systemImage: String,
        badges: [String],
        emphasis: Emphasis = .standard,
        pathLabel: String = "",
        ctaLabel: String = "Eintreten",
        accessibilityID: String? = nil,
        backgroundImageURL: String? = nil,
        useTightHeights: Bool = false,
        action: @escaping () -> Void
    ) {
        self.eyebrow = eyebrow
        self.title = title
        self.subtitle = subtitle
        self.detail = detail
        self.accent = accent
        self.systemImage = systemImage
        self.badges = badges
        self.emphasis = emphasis
        self.pathLabel = pathLabel
        self.ctaLabel = ctaLabel
        self.accessibilityID = accessibilityID
        self.backgroundImageURL = backgroundImageURL
        self.useTightHeights = useTightHeights
        self.action = action
    }

    private var tileSpacing: CGFloat {
        if useTightHeights {
            switch emphasis {
            case .primary: return 12
            case .standard: return 10
            case .secondary: return 6
            }
        }
        switch emphasis {
        case .primary: return 16
        case .standard: return 13
        case .secondary: return 8
        }
    }

    private var tilePadding: CGFloat {
        if useTightHeights {
            switch emphasis {
            case .primary: return 15
            case .standard: return 14
            case .secondary: return 8
            }
        }
        switch emphasis {
        case .primary: return 20
        case .standard: return 17
        case .secondary: return 10
        }
    }

    private var minTileHeight: CGFloat {
        if useTightHeights {
            switch emphasis {
            case .primary: return 200
            case .standard: return 168
            case .secondary: return 108
            }
        }
        switch emphasis {
        case .primary: return 248
        case .standard: return 200
        case .secondary: return 132
        }
    }

    private var markSize: CGFloat {
        if useTightHeights {
            switch emphasis {
            case .primary, .standard: return 54
            case .secondary: return 40
            }
        }
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
        case .secondary: return []
        default: return badges
        }
    }

    private var shouldShowEyebrow: Bool {
        !eyebrow.isEmpty && !title.lowercased().hasPrefix(eyebrow.lowercased())
    }

    private var shouldShowDetail: Bool {
        let normalizedDetail = detail.normalizedLandingComparisonText
        guard !normalizedDetail.isEmpty else { return false }
        return normalizedDetail != subtitle.normalizedLandingComparisonText
    }

    private var cornerRadius: CGFloat {
        emphasis == .secondary ? 18 : 24
    }

    private var resolvedBackgroundURL: URL? {
        guard let backgroundImageURL else { return nil }
        let trimmed = backgroundImageURL.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        if let direct = URL(string: trimmed), direct.scheme != nil {
            return direct
        }
        let encoded = trimmed.addingPercentEncoding(withAllowedCharacters: .urlFragmentAllowed)
        guard let encoded, let url = URL(string: encoded), url.scheme != nil else { return nil }
        return url
    }

    private var hasBackgroundImage: Bool {
        resolvedBackgroundURL != nil
    }

    /// Readability floor: brighter images receive a stronger dark scrim.
    private var readabilityFloorOpacity: Double {
        guard hasBackgroundImage else { return 0 }
        let luminance = backgroundLuminance ?? 0.55
        let normalized = min(max((luminance - 0.45) / 0.45, 0), 1)
        return 0.14 + (normalized * 0.34)
    }

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: tileSpacing) {
                HStack(alignment: .top, spacing: emphasis == .secondary ? 11 : 14) {
                    Image(systemName: systemImage)
                        .font(.system(size: emphasis == .secondary ? 20 : 26, weight: .semibold))
                        .foregroundStyle(accent.opacity(emphasis == .secondary ? 0.55 : 0.88))
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
                        if shouldShowEyebrow {
                            Text(emphasis == .secondary ? eyebrow : eyebrow.uppercased())
                                .font(emphasis == .secondary ? .caption.weight(.medium) : AppTypography.heroEyebrow)
                                .tracking(emphasis == .secondary ? 0.6 : 1.4)
                                .foregroundColor(accent.opacity(emphasis == .secondary ? 0.52 : 0.92))
                        }

                        Text(title)
                            .font(titleFont)
                            .foregroundColor(.white.opacity(emphasis == .secondary ? 0.86 : 1))
                            .shadow(color: .black.opacity(emphasis == .secondary ? 0.08 : 0.24), radius: emphasis == .secondary ? 4 : 10, y: emphasis == .secondary ? 2 : 3)
                            .lineLimit(2)
                            .minimumScaleFactor(0.88)

                        Text(subtitle)
                            .font(emphasis == .secondary ? .caption.weight(.medium) : AppTypography.editorialCaption)
                            .foregroundColor(.white.opacity(emphasis == .secondary ? (hasBackgroundImage ? 0.78 : 0.64) : (hasBackgroundImage ? 0.88 : 0.82)))
                            .multilineTextAlignment(.leading)
                            .lineLimit(1)
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

                if shouldShowDetail {
                    Text(detail)
                        .font(AppTypography.editorialCaption)
                        .foregroundColor(.white.opacity(emphasis == .secondary ? (hasBackgroundImage ? 0.70 : 0.56) : (hasBackgroundImage ? 0.80 : 0.72)))
                        .multilineTextAlignment(.leading)
                        .lineLimit(1)
                }

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
                            .foregroundColor(accent.opacity(hasBackgroundImage ? 0.94 : 0.86))
                            .padding(.horizontal, emphasis == .secondary ? 9 : 11)
                            .padding(.vertical, emphasis == .secondary ? 5 : 7)
                            .background(
                                Capsule(style: .continuous)
                                    .fill(Color.white.opacity(emphasis == .secondary ? 0.03 : 0.035))
                            )
                    }
                }

                if emphasis != .secondary {
                    HStack(spacing: 10) {
                        Text(ctaLabel)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(.white.opacity(0.94))

                        Spacer(minLength: 10)

                        HStack(spacing: 6) {
                            Text("Start")
                                .font(.caption.weight(.semibold))
                            Image(systemName: "arrow.up.right")
                                .font(.caption.weight(.semibold))
                        }
                        .foregroundColor(.white.opacity(0.76))
                    }
                }
            }
            .padding(tilePadding)
            .frame(maxWidth: .infinity, minHeight: minTileHeight, alignment: .leading)
            .background(
                ZStack {
                    if let url = resolvedBackgroundURL {
                        GeometryReader { proxy in
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .success(let image):
                                    image
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: proxy.size.width, height: proxy.size.height)
                                default:
                                    Color.clear
                                        .frame(width: proxy.size.width, height: proxy.size.height)
                                }
                            }
                            .clipped()
                        }
                    }
                    LinearGradient(
                        colors: [
                            Color.white.opacity(emphasis == .primary ? 0.10 : (emphasis == .secondary ? 0.04 : 0.08)),
                            Color.black.opacity(
                                emphasis == .secondary
                                    ? (hasBackgroundImage ? 0.58 : 0.76)
                                    : (hasBackgroundImage ? 0.62 : 0.80)
                            ),
                            Color(red: 8/255, green: 14/255, blue: 24/255).opacity(
                                emphasis == .secondary
                                    ? (hasBackgroundImage ? 0.70 : 0.86)
                                    : (hasBackgroundImage ? 0.72 : 0.92)
                            ),
                            accent.opacity(emphasis == .primary ? 0.16 : (emphasis == .secondary ? 0.03 : 0.12))
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    if hasBackgroundImage {
                        LinearGradient(
                            colors: [
                                Color.black.opacity(readabilityFloorOpacity * 0.28),
                                Color.black.opacity(readabilityFloorOpacity * 0.52),
                                Color.black.opacity(readabilityFloorOpacity * 0.68)
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
                }
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
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
                        Color.white.opacity(emphasis == .primary ? 0.16 : (emphasis == .secondary ? 0.03 : 0.10)),
                        lineWidth: emphasis == .primary ? 1.2 : 1
                    )
            }
            .shadow(color: .black.opacity(emphasis == .primary ? 0.28 : (emphasis == .secondary ? 0.06 : 0.22)), radius: emphasis == .primary ? 16 : (emphasis == .secondary ? 4 : 14), y: emphasis == .secondary ? 2 : 8)
        }
        .accessibilityIdentifier(accessibilityID ?? "")
        .accessibilityLabel("\(title) öffnen")
        .accessibilityHint("Öffnet den Bereich \(title)")
        .buttonStyle(.plain)
        .skydownTactileAction()
        .task(id: resolvedBackgroundURL) {
            guard let url = resolvedBackgroundURL else {
                backgroundLuminance = nil
                return
            }
            backgroundLuminance = await Self.fetchAverageLuminance(from: url)
        }
    }

    private static func fetchAverageLuminance(from url: URL) async -> Double? {
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            return averageLuminance(from: data)
        } catch {
            return nil
        }
    }

    private static func averageLuminance(from data: Data) -> Double? {
        guard let image = UIImage(data: data),
              let cgImage = image.cgImage else {
            return nil
        }

        let context = CIContext(options: [.workingColorSpace: NSNull()])
        let input = CIImage(cgImage: cgImage)
        guard let filter = CIFilter(name: "CIAreaAverage") else { return nil }
        filter.setValue(input, forKey: kCIInputImageKey)
        filter.setValue(CIVector(cgRect: input.extent), forKey: kCIInputExtentKey)
        guard let output = filter.outputImage else { return nil }

        var bitmap = [UInt8](repeating: 0, count: 4)
        context.render(
            output,
            toBitmap: &bitmap,
            rowBytes: 4,
            bounds: CGRect(x: 0, y: 0, width: 1, height: 1),
            format: .RGBA8,
            colorSpace: nil
        )

        let r = Double(bitmap[0]) / 255.0
        let g = Double(bitmap[1]) / 255.0
        let b = Double(bitmap[2]) / 255.0
        return (0.2126 * r) + (0.7152 * g) + (0.0722 * b)
    }
}

private extension String {
    var normalizedLandingComparisonText: String {
        lowercased()
            .replacingOccurrences(of: "[^\\p{L}\\p{N}]+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

private struct LaunchLandingActionButton: View {
    enum ActionStyle {
        case primary
        case secondary
    }

    let title: String
    let systemImage: String
    let style: ActionStyle
    let action: () -> Void

    private var hubColorScheme: ColorScheme { .dark }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.footnote.weight(.bold))
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .foregroundColor(foregroundColor)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, minHeight: 52, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(backgroundColor)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(borderColor, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private var foregroundColor: Color {
        switch style {
        case .primary:
            return .white.opacity(0.96)
        case .secondary:
            return .white.opacity(0.90)
        }
    }

    private var backgroundColor: Color {
        switch style {
        case .primary:
            return AppColors.accent(for: hubColorScheme).opacity(0.56)
        case .secondary:
            return Color.white.opacity(0.06)
        }
    }

    private var borderColor: Color {
        switch style {
        case .primary:
            return AppColors.accent(for: hubColorScheme).opacity(0.62)
        case .secondary:
            return Color.white.opacity(0.18)
        }
    }
}

private struct LaunchLandingMetaPill: View {
    let text: String
    let accent: Color
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            Text(text)
                .font(.caption.weight(.bold))
                .foregroundColor(.white.opacity(0.90))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .frame(minHeight: 44)
                .background(
                    Capsule(style: .continuous)
                        .fill(Color.white.opacity(0.05))
                )
                .overlay(
                    Capsule(style: .continuous)
                        .stroke(accent.opacity(0.28), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .accessibilityLabel("\(text) öffnen")
    }
}

private struct LaunchLandingVisionBand: View {
    let colorScheme: ColorScheme
    let onOpenMusic: () -> Void
    let onOpenVideography: () -> Void

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
                    LaunchLandingInlineTag(
                        text: "Premium Technologie",
                        accent: AppColors.accent(for: colorScheme),
                        onTap: onOpenMusic
                    )
                    LaunchLandingInlineTag(
                        text: "Persoenliche Kontrolle",
                        accent: AppColors.accentMystic(for: colorScheme),
                        onTap: onOpenVideography
                    )
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
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
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
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private struct LaunchLandingNarrativeStep: View {
    let title: String
    let statement: String
    let accent: Color
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
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
        .buttonStyle(.plain)
        .skydownTactileAction()
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
