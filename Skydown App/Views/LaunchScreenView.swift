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
                        Color(red: 33/255, green: 63/255, blue: 96/255).opacity(0.34),
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
                    .blur(radius: 62)
                    .offset(x: proxy.size.width * 0.26, y: -280)

                Circle()
                    .fill(Color(red: 138/255, green: 170/255, blue: 206/255).opacity(0.08))
                    .frame(width: layout.prefersDesktopChrome ? 320 : 240, height: layout.prefersDesktopChrome ? 320 : 240)
                    .blur(radius: 68)
                    .offset(x: -proxy.size.width * 0.18, y: 280)

                RoundedRectangle(cornerRadius: 44, style: .continuous)
                    .fill(AppColors.spotify(for: hubColorScheme).opacity(0.06))
                    .frame(width: layout.prefersDesktopChrome ? 320 : 220, height: layout.prefersDesktopChrome ? 440 : 320)
                    .blur(radius: 40)
                    .rotationEffect(.degrees(18))
                    .offset(x: -proxy.size.width * 0.24, y: -160)

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: layout.sectionSpacing + 4) {
                        BrandHeroSurface(
                            colorScheme: hubColorScheme,
                            eyebrow: screenHeaderSettingsStore.settings.resolvedHomeEyebrow ?? "Sky²² Home",
                            title: screenHeaderSettingsStore.settings.resolvedHomeTitle ?? "Sky²²",
                            subtitle: screenHeaderSettingsStore.settings.resolvedHomeSubtitle ?? "Waehle deinen Start.",
                            detail: screenHeaderSettingsStore.settings.resolvedHomeDetail ?? "Musik, Video, Merch und Tools direkt im Einstieg.",
                            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedHomeImageURL,
                            accent: AppColors.accent(for: hubColorScheme),
                            secondaryAccent: AppColors.accentMystic(for: hubColorScheme),
                            marks: [.skydownX22]
                        ) {
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
                        }

                        if layout.prefersThreeColumn {
                            HStack(spacing: 12) {
                                LaunchLandingSignalCard(
                                    title: "3 Lanes",
                                    value: "Music, Video, Shop",
                                    accent: AppColors.spotify(for: hubColorScheme)
                                )
                                LaunchLandingSignalCard(
                                    title: "Direkter Start",
                                    value: "Kein Umweg nach dem Intro",
                                    accent: AppColors.accentMystic(for: hubColorScheme)
                                )
                                LaunchLandingSignalCard(
                                    title: "Sync",
                                    value: "iOS und Android gleich gedacht",
                                    accent: AppColors.accentHighlight(for: hubColorScheme)
                                )
                            }
                        } else {
                            VStack(spacing: 10) {
                                LaunchLandingSignalCard(
                                    title: "3 Lanes",
                                    value: "Music, Video, Shop",
                                    accent: AppColors.spotify(for: hubColorScheme)
                                )
                                LaunchLandingSignalCard(
                                    title: "Direkter Start",
                                    value: "Kein Umweg nach dem Intro",
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
                                        detail: "Catalog, Beat Hub und Studio direkt in einem Flow.",
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
                                        detail: "Playback, Creator-Flows und fertige Clips ohne Reibung.",
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
                                        detail: "Direkt in neue Pieces springen und sauber durch den Checkout gehen.",
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
                                        detail: "Catalog, Beat Hub und Studio direkt in einem Flow.",
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
                                            detail: "Playback, Creator-Flows und fertige Clips ohne Reibung.",
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
                                            detail: "Direkt in neue Pieces springen und sauber durch den Checkout gehen.",
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
                                        detail: "Catalog, Beat Hub und Studio direkt in einem Flow.",
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
                                        detail: "Playback, Creator-Flows und fertige Clips ohne Reibung.",
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
                                        detail: "Direkt in neue Pieces springen und sauber durch den Checkout gehen.",
                                        accent: AppColors.accentHighlight(for: hubColorScheme),
                                        brandMark: .skydownX22,
                                        badges: ["Drops", "Fits", "Checkout"],
                                        accessibilityID: "launch.open_shop",
                                        action: onOpenShop
                                    )
                                }
                            }
                        }

                        Text("Unten wechselst du spaeter jederzeit zwischen allen Bereichen.")
                            .font(.caption.weight(.medium))
                            .foregroundColor(.white.opacity(0.66))
                            .padding(.top, 4)
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
            VStack(alignment: .leading, spacing: 16) {
                HStack(alignment: .top, spacing: 14) {
                    Image(brandMark.imageName)
                        .resizable()
                        .scaledToFit()
                        .padding(8)
                        .frame(width: 58, height: 58)
                        .background(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .fill(
                                    LinearGradient(
                                        colors: [
                                            AppColors.cardBackground(for: hubColorScheme).opacity(0.96),
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
                            .tracking(1.2)
                            .foregroundColor(.white.opacity(0.70))

                        Text(title)
                            .font(AppTypography.sectionTitle)
                            .foregroundColor(.white)
                            .shadow(color: .black.opacity(0.24), radius: 10, y: 4)

                        Text(subtitle)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(.white.opacity(0.78))
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
                    .font(.footnote.weight(.medium))
                    .foregroundColor(.white.opacity(0.68))
                    .multilineTextAlignment(.leading)

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
            }
            .padding(20)
            .frame(maxWidth: .infinity, minHeight: 210, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.black.opacity(0.82),
                                Color(red: 8/255, green: 14/255, blue: 24/255).opacity(0.94),
                                accent.opacity(0.15)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            )
            .overlay(alignment: .topTrailing) {
                Circle()
                    .fill(accent.opacity(0.18))
                    .frame(width: 120, height: 120)
                    .blur(radius: 24)
                    .offset(x: 28, y: -24)
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

private struct LaunchLandingSignalCard: View {
    let title: String
    let value: String
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title.uppercased())
                .font(AppTypography.heroEyebrow)
                .tracking(1.2)
                .foregroundColor(accent)

            Text(value)
                .font(.footnote.weight(.semibold))
                .foregroundColor(.white.opacity(0.84))
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
                            accent.opacity(0.10),
                            Color.black.opacity(0.22)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
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
