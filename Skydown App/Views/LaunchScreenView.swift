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
                .frame(width: 260, height: 260)
                .blur(radius: 62)
                .offset(x: 150, y: -280)

            Circle()
                .fill(Color(red: 138/255, green: 170/255, blue: 206/255).opacity(0.08))
                .frame(width: 240, height: 240)
                .blur(radius: 68)
                .offset(x: -140, y: 260)

            VStack(alignment: .leading, spacing: 22) {
                Spacer(minLength: 0)

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

                VStack(spacing: 12) {
                    LaunchLandingButton(
                        eyebrow: "Music",
                        title: "Music",
                        subtitle: "Releases, Artists, Beats.",
                        accent: AppColors.spotify(for: hubColorScheme),
                        brandMark: .zweizwei,
                        action: onOpenMusic
                    )

                    LaunchLandingButton(
                        eyebrow: "Video",
                        title: "Videos",
                        subtitle: "Reels, Clips, YouTube.",
                        accent: AppColors.accentMystic(for: hubColorScheme),
                        brandMark: .skydown,
                        action: onOpenVideography
                    )

                    LaunchLandingButton(
                        eyebrow: "Store",
                        title: "Merch",
                        subtitle: "Drops, Styles, Checkout.",
                        accent: AppColors.accentHighlight(for: hubColorScheme),
                        brandMark: .skydownX22,
                        action: onOpenShop
                    )
                }

                Text("Unten wechselst du jederzeit.")
                    .font(.caption.weight(.medium))
                    .foregroundColor(.white.opacity(0.66))
                    .padding(.top, 4)

                Spacer(minLength: 0)
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.vertical, 28)
        }
    }
}

private struct LaunchLandingButton: View {
    let eyebrow: String
    let title: String
    let subtitle: String
    let accent: Color
    let brandMark: BrandMark
    let action: () -> Void

    private var hubColorScheme: ColorScheme { .dark }

    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 14) {
                Image(brandMark.imageName)
                    .resizable()
                    .scaledToFit()
                    .padding(6)
                    .frame(width: 52, height: 52)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(
                                LinearGradient(
                                    colors: [
                                        AppColors.cardBackground(for: hubColorScheme).opacity(0.96),
                                        accent.opacity(0.16)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                    )
                    .shadow(color: accent.opacity(0.34), radius: 16, y: 8)

                VStack(alignment: .leading, spacing: 12) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text(eyebrow.uppercased())
                            .font(AppTypography.heroEyebrow)
                            .tracking(1.2)
                            .foregroundColor(.white.opacity(0.70))

                        Text(title)
                            .font(AppTypography.sectionTitle)
                            .foregroundColor(.white)
                            .shadow(color: .black.opacity(0.24), radius: 10, y: 4)
                    }

                    Text(subtitle)
                        .font(.footnote.weight(.medium))
                        .foregroundColor(.white.opacity(0.74))
                        .multilineTextAlignment(.leading)
                }
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.black.opacity(0.82),
                                Color(red: 8/255, green: 14/255, blue: 24/255).opacity(0.94),
                                accent.opacity(0.12)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            )
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
