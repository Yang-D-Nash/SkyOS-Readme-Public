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
            if let url = Bundle.main.url(forResource: "Intro Launch", withExtension: "mp4") {
                player = AVPlayer(url: url)
            } else {
                print("Video 'Intro Launch.mp4' nicht gefunden!")
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
            print("Audio-Session fuer Intro konnte nicht aktiviert werden: \(error.localizedDescription)")
        }
    }

    private func deactivateIntroAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
        } catch {
            print("Audio-Session fuer Intro konnte nicht deaktiviert werden: \(error.localizedDescription)")
        }
    }

    private func transitionToLanding() {
        guard !hasCompletedIntro else { return }
        hasCompletedIntro = true
        player?.pause()
        deactivateIntroAudioSession()
        withAnimation(.easeInOut(duration: 0.35)) {
            phase = .landing
        }
    }

    private func openShell(tab: MainTab) {
        withAnimation(.easeInOut(duration: 0.3)) {
            phase = .shell(tab)
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

            ZStack {
                LinearGradient(
                    colors: [
                        Color.black,
                        Color(red: 2/255, green: 7/255, blue: 13/255),
                        Color(red: 7/255, green: 16/255, blue: 26/255),
                        Color(red: 20/255, green: 42/255, blue: 68/255).opacity(0.48),
                        Color(red: 33/255, green: 63/255, blue: 96/255).opacity(0.30),
                        Color(red: 5/255, green: 11/255, blue: 18/255),
                        Color.black
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                LinearGradient(
                    colors: [
                        Color.black.opacity(0.26),
                        Color.black.opacity(0.08),
                        Color.black.opacity(0.34)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()

                Circle()
                    .fill(Color(red: 91/255, green: 149/255, blue: 216/255).opacity(0.12))
                    .frame(width: layout.prefersDesktopChrome ? 360 : 260, height: layout.prefersDesktopChrome ? 360 : 260)
                    .blur(radius: 70)
                    .offset(x: proxy.size.width * 0.26, y: -280)

                Circle()
                    .fill(Color(red: 138/255, green: 170/255, blue: 206/255).opacity(0.08))
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
                    VStack(alignment: .leading, spacing: layout.sectionSpacing + 4) {
                        HStack(spacing: 8) {
                            LaunchLandingMetaPill(text: "Sky OS", accent: AppColors.accent(for: hubColorScheme))
                            LaunchLandingMetaPill(text: "One Flow", accent: AppColors.accentMystic(for: hubColorScheme))
                            LaunchLandingMetaPill(text: "Direct", accent: AppColors.spotify(for: hubColorScheme))
                        }

                        BrandHeroSurface(
                            colorScheme: hubColorScheme,
                            eyebrow: screenHeaderSettingsStore.settings.resolvedHomeEyebrow ?? "SkyOS Home",
                            title: screenHeaderSettingsStore.settings.resolvedHomeTitle ?? "SkyOS",
                            subtitle: screenHeaderSettingsStore.settings.resolvedHomeSubtitle ?? "Alles fuehlt sich wie eine einzige App an.",
                            detail: screenHeaderSettingsStore.settings.resolvedHomeDetail ?? "Waehle deinen ersten Move. Music, Video, Merch und Tools bleiben danach in einem klaren Flow verbunden.",
                            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedHomeImageURL,
                            accent: AppColors.accent(for: hubColorScheme),
                            secondaryAccent: AppColors.accentMystic(for: hubColorScheme),
                            marks: [.skydownX22]
                        ) {
                            VStack(alignment: .leading, spacing: 12) {
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
                                    BrandHeroPill(
                                        text: "AI",
                                        colorScheme: hubColorScheme,
                                        tint: AppColors.accent(for: hubColorScheme)
                                    )
                                }

                                Text("Direkt reingehen, jederzeit unten wechseln, nie in einer Sackgasse haengen bleiben.")
                                    .font(AppTypography.editorialCaption)
                                    .foregroundColor(.white.opacity(0.82))
                            }
                        }

                        if layout.prefersThreeColumn {
                            HStack(spacing: 12) {
                                LaunchLandingSignalCard(
                                    title: "1 Tap Start",
                                    value: "Direkt in Music, Video oder Shop.",
                                    accent: AppColors.spotify(for: hubColorScheme)
                                )
                                LaunchLandingSignalCard(
                                    title: "Immer Zurueck",
                                    value: "Navigation bleibt klar und frei.",
                                    accent: AppColors.accentMystic(for: hubColorScheme)
                                )
                                LaunchLandingSignalCard(
                                    title: "Ein System",
                                    value: "Discovery, Playback und Checkout greifen zusammen.",
                                    accent: AppColors.accentHighlight(for: hubColorScheme)
                                )
                            }
                        } else {
                            VStack(spacing: 10) {
                                LaunchLandingSignalCard(
                                    title: "1 Tap Start",
                                    value: "Direkt in Music, Video oder Shop.",
                                    accent: AppColors.spotify(for: hubColorScheme)
                                )
                                LaunchLandingSignalCard(
                                    title: "Immer Zurueck",
                                    value: "Navigation bleibt klar und frei.",
                                    accent: AppColors.accentMystic(for: hubColorScheme)
                                )
                            }
                        }

                        Group {
                            if layout.prefersThreeColumn {
                                HStack(alignment: .top, spacing: layout.sectionSpacing) {
                                    LaunchLandingButton(
                                        eyebrow: "Music",
                                        title: "Music",
                                        subtitle: "Releases, Artists, Beats.",
                                        detail: "Catalog, Beat Hub und Studio in einer direkten Lane ohne Umwege.",
                                        accent: AppColors.spotify(for: hubColorScheme),
                                        brandMark: .zweizwei,
                                        badges: ["Catalog", "Beats", "Studio"],
                                        accessibilityID: "launch.open_music",
                                        action: onOpenMusic
                                    )
                                    LaunchLandingButton(
                                        eyebrow: "Video",
                                        title: "Videos",
                                        subtitle: "Reels, Clips, YouTube.",
                                        detail: "Playback, Creator-Flows und finale Clips greifen wie ein Produkt zusammen.",
                                        accent: AppColors.accentMystic(for: hubColorScheme),
                                        brandMark: .skydown,
                                        badges: ["Playback", "Reels", "YouTube"],
                                        accessibilityID: "launch.open_video",
                                        action: onOpenVideography
                                    )
                                    LaunchLandingButton(
                                        eyebrow: "Store",
                                        title: "Merch",
                                        subtitle: "Drops, Styles, Checkout.",
                                        detail: "Direkt in neue Pieces springen und sauber im Checkout bleiben.",
                                        accent: AppColors.accentHighlight(for: hubColorScheme),
                                        brandMark: .skydownX22,
                                        badges: ["Drops", "Fits", "Checkout"],
                                        accessibilityID: "launch.open_shop",
                                        action: onOpenShop
                                    )
                                }
                            } else if layout.prefersTwoColumn {
                                VStack(spacing: 12) {
                                    LaunchLandingButton(
                                        eyebrow: "Music",
                                        title: "Music",
                                        subtitle: "Releases, Artists, Beats.",
                                        detail: "Catalog, Beat Hub und Studio in einer direkten Lane ohne Umwege.",
                                        accent: AppColors.spotify(for: hubColorScheme),
                                        brandMark: .zweizwei,
                                        badges: ["Catalog", "Beats", "Studio"],
                                        accessibilityID: "launch.open_music",
                                        action: onOpenMusic
                                    )

                                    HStack(alignment: .top, spacing: 12) {
                                        LaunchLandingButton(
                                            eyebrow: "Video",
                                            title: "Videos",
                                            subtitle: "Reels, Clips, YouTube.",
                                            detail: "Playback, Creator-Flows und finale Clips greifen wie ein Produkt zusammen.",
                                            accent: AppColors.accentMystic(for: hubColorScheme),
                                            brandMark: .skydown,
                                            badges: ["Playback", "Reels", "YouTube"],
                                            accessibilityID: "launch.open_video",
                                            action: onOpenVideography
                                        )
                                        LaunchLandingButton(
                                            eyebrow: "Store",
                                            title: "Merch",
                                            subtitle: "Drops, Styles, Checkout.",
                                            detail: "Direkt in neue Pieces springen und sauber im Checkout bleiben.",
                                            accent: AppColors.accentHighlight(for: hubColorScheme),
                                            brandMark: .skydownX22,
                                            badges: ["Drops", "Fits", "Checkout"],
                                            accessibilityID: "launch.open_shop",
                                            action: onOpenShop
                                        )
                                    }
                                }
                            } else {
                                VStack(spacing: 12) {
                                    LaunchLandingButton(
                                        eyebrow: "Music",
                                        title: "Music",
                                        subtitle: "Releases, Artists, Beats.",
                                        detail: "Catalog, Beat Hub und Studio in einer direkten Lane ohne Umwege.",
                                        accent: AppColors.spotify(for: hubColorScheme),
                                        brandMark: .zweizwei,
                                        badges: ["Catalog", "Beats", "Studio"],
                                        accessibilityID: "launch.open_music",
                                        action: onOpenMusic
                                    )

                                    LaunchLandingButton(
                                        eyebrow: "Video",
                                        title: "Videos",
                                        subtitle: "Reels, Clips, YouTube.",
                                        detail: "Playback, Creator-Flows und finale Clips greifen wie ein Produkt zusammen.",
                                        accent: AppColors.accentMystic(for: hubColorScheme),
                                        brandMark: .skydown,
                                        badges: ["Playback", "Reels", "YouTube"],
                                        accessibilityID: "launch.open_video",
                                        action: onOpenVideography
                                    )

                                    LaunchLandingButton(
                                        eyebrow: "Store",
                                        title: "Merch",
                                        subtitle: "Drops, Styles, Checkout.",
                                        detail: "Direkt in neue Pieces springen und sauber im Checkout bleiben.",
                                        accent: AppColors.accentHighlight(for: hubColorScheme),
                                        brandMark: .skydownX22,
                                        badges: ["Drops", "Fits", "Checkout"],
                                        accessibilityID: "launch.open_shop",
                                        action: onOpenShop
                                    )
                                }
                            }
                        }

                        Text("Unten wechselst du spaeter jederzeit zwischen allen Bereichen. Der Einstieg bleibt direkt, die App bleibt offen und lesbar.")
                            .font(AppTypography.editorialFootnote)
                            .foregroundColor(.white.opacity(0.74))
                            .padding(.top, 6)
                    }
                    .frame(maxWidth: contentWidth, alignment: .leading)
                    .padding(.horizontal, layout.horizontalPadding)
                    .padding(.vertical, layout.prefersDesktopChrome ? 36 : 28)
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }
}

private struct LaunchLandingButton: View {
    let eyebrow: String
    let title: String
    let subtitle: String
    let detail: String
    let accent: Color
    let brandMark: BrandMark
    let badges: [String]
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
        self.accessibilityID = accessibilityID
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 18) {
                HStack(alignment: .top, spacing: 14) {
                    Image(brandMark.imageName)
                        .resizable()
                        .scaledToFit()
                        .padding(9)
                        .frame(width: 62, height: 62)
                        .background(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .fill(
                                    LinearGradient(
                                        colors: [
                                            Color.white.opacity(0.05),
                                            AppColors.cardBackground(for: hubColorScheme).opacity(0.94),
                                            accent.opacity(0.22)
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                        )
                        .shadow(color: accent.opacity(0.34), radius: 16, y: 8)

                    VStack(alignment: .leading, spacing: 6) {
                        Text(eyebrow.uppercased())
                            .font(AppTypography.heroEyebrow)
                            .tracking(1.4)
                            .foregroundColor(accent.opacity(0.92))

                        Text(title)
                            .font(AppTypography.sectionTitle)
                            .foregroundColor(.white)
                            .shadow(color: .black.opacity(0.24), radius: 10, y: 4)

                        Text(subtitle)
                            .font(AppTypography.editorialCaption)
                            .foregroundColor(.white.opacity(0.82))
                            .multilineTextAlignment(.leading)
                    }

                    Spacer(minLength: 10)

                    Image(systemName: "arrow.up.right")
                        .font(.headline.weight(.bold))
                        .foregroundColor(.white.opacity(0.82))
                        .padding(10)
                        .background(
                            Circle()
                                .fill(accent.opacity(0.18))
                        )
                }

                Text(detail)
                    .font(AppTypography.editorialCaption)
                    .foregroundColor(.white.opacity(0.72))
                    .multilineTextAlignment(.leading)

                HStack(alignment: .center, spacing: 10) {
                    HStack(spacing: 8) {
                        ForEach(badges, id: \.self) { badge in
                            Text(badge)
                                .font(.caption.weight(.semibold))
                                .foregroundColor(accent)
                                .padding(.horizontal, 11)
                                .padding(.vertical, 7)
                                .background(
                                    Capsule(style: .continuous)
                                        .fill(Color.white.opacity(0.05))
                                )
                                .overlay(
                                    Capsule(style: .continuous)
                                        .stroke(accent.opacity(0.24), lineWidth: 1)
                                )
                        }
                    }

                    Spacer(minLength: 0)
                }

                HStack(spacing: 10) {
                    Text("Direkt in \(title)")
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(.white.opacity(0.94))

                    Spacer(minLength: 10)

                    LaunchLandingActionPill(
                        text: "Open",
                        accent: accent
                    )
                }
            }
            .padding(22)
            .frame(maxWidth: .infinity, minHeight: 226, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(0.05),
                                Color.black.opacity(0.82),
                                Color(red: 8/255, green: 14/255, blue: 24/255).opacity(0.94),
                                accent.opacity(0.16)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            )
            .overlay(alignment: .topTrailing) {
                Circle()
                    .fill(accent.opacity(0.22))
                    .frame(width: 132, height: 132)
                    .blur(radius: 30)
                    .offset(x: 28, y: -20)
            }
            .overlay(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3, style: .continuous)
                    .fill(accent)
                    .frame(width: 4)
                    .padding(.vertical, 16)
                    .padding(.leading, 10)
            }
            .overlay {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(Color.white.opacity(0.10), lineWidth: 1)
            }
            .shadow(color: .black.opacity(0.22), radius: 14, y: 8)
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

private struct LaunchLandingSignalCard: View {
    let title: String
    let value: String
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title.uppercased())
                .font(AppTypography.heroEyebrow)
                .tracking(1.4)
                .foregroundColor(accent)

            Text(value)
                .font(AppTypography.editorialCaption)
                .foregroundColor(.white.opacity(0.86))
                .multilineTextAlignment(.leading)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(0.05),
                            accent.opacity(0.12),
                            Color.black.opacity(0.24)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .overlay(alignment: .topLeading) {
            Capsule(style: .continuous)
                .fill(accent.opacity(0.94))
                .frame(width: 42, height: 3)
                .padding(.top, 12)
                .padding(.leading, 16)
        }
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
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
