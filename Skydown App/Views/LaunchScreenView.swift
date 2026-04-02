//
//  LaunchScreenView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 21.08.25.
//

import AVFoundation
import AVKit
import SwiftUI

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

                GeometryReader { proxy in
                    VideoPlayer(player: player)
                        .frame(
                            width: proxy.size.width - 12,
                            height: proxy.size.height - 24
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 28, style: .continuous)
                                .stroke(Color.white.opacity(0.08), lineWidth: 1)
                        }
                        .shadow(color: .black.opacity(0.32), radius: 24, y: 12)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 12)
                }

                VStack {
                    Spacer()

                    Image(BrandMark.skydownX22.rawValue)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 88, height: 88)
                        .padding(10)
                        .background(.black.opacity(0.28))
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .stroke(.white.opacity(0.08), lineWidth: 1)
                        }
                        .padding(.bottom, 28)
                }
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

    private var hubColorScheme: ColorScheme { .dark }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 2/255, green: 6/255, blue: 12/255),
                    Color(red: 4/255, green: 10/255, blue: 18/255),
                    Color(red: 6/255, green: 14/255, blue: 24/255),
                    AppColors.accent(for: hubColorScheme).opacity(0.16),
                    Color(red: 2/255, green: 7/255, blue: 13/255)
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
                .fill(AppColors.accent(for: hubColorScheme).opacity(0.14))
                .frame(width: 260, height: 260)
                .blur(radius: 62)
                .offset(x: 150, y: -280)

            Circle()
                .fill(AppColors.accentMystic(for: hubColorScheme).opacity(0.12))
                .frame(width: 240, height: 240)
                .blur(radius: 68)
                .offset(x: -140, y: 260)

            VStack(alignment: .leading, spacing: 22) {
                Spacer(minLength: 0)

                VStack(alignment: .leading, spacing: 12) {
                    Text("Sky²²")
                        .font(.caption.weight(.semibold))
                        .tracking(1.6)
                        .foregroundColor(AppColors.text(for: hubColorScheme).opacity(0.78))

                    Text("Sky²²")
                        .font(.system(size: 32, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: hubColorScheme))
                        .shadow(color: .black.opacity(0.34), radius: 14, y: 8)

                    Text("Waehle deinen Start.")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(AppColors.text(for: hubColorScheme).opacity(0.72))
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(18)
                .background(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color.black.opacity(0.58),
                                    AppColors.cardBackground(for: hubColorScheme).opacity(0.68)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                )
                .overlay {
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke(AppColors.accentHighlight(for: hubColorScheme).opacity(0.18), lineWidth: 1)
                }

                HStack(alignment: .center, spacing: 14) {
                    Image(BrandMark.skydownX22.rawValue)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 72, height: 72)
                        .padding(8)
                        .background(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .fill(
                                    LinearGradient(
                                        colors: [
                                            AppColors.cardBackground(for: hubColorScheme).opacity(0.94),
                                            AppColors.accentHighlight(for: hubColorScheme).opacity(0.16)
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                        )
                        .overlay {
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .stroke(AppColors.accentHighlight(for: hubColorScheme).opacity(0.22), lineWidth: 1)
                        }
                        .shadow(color: .black.opacity(0.24), radius: 18, y: 10)

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Musik, Video, Merch.")
                            .font(.title3.weight(.black))
                            .foregroundColor(AppColors.text(for: hubColorScheme))
                            .shadow(color: .black.opacity(0.26), radius: 12, y: 6)

                        Text("Alles unten direkt griffbereit.")
                            .font(.footnote.weight(.medium))
                            .foregroundColor(AppColors.text(for: hubColorScheme).opacity(0.72))
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .padding(14)
                .skydownPanelSurface(
                    colorScheme: hubColorScheme,
                    accent: AppColors.accentHighlight(for: hubColorScheme),
                    cornerRadius: 24,
                    shadowRadius: 14,
                    shadowYOffset: 8
                )

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
                    .foregroundColor(AppColors.text(for: hubColorScheme).opacity(0.68))
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
                Image(brandMark.rawValue)
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
                            .font(.caption.weight(.semibold))
                            .tracking(1.2)
                            .foregroundColor(AppColors.text(for: hubColorScheme).opacity(0.72))

                        Text(title)
                            .font(.system(size: 22, weight: .black, design: .rounded))
                            .foregroundColor(AppColors.text(for: hubColorScheme))
                            .shadow(color: .black.opacity(0.24), radius: 10, y: 4)
                    }

                    Text(subtitle)
                        .font(.footnote.weight(.medium))
                        .foregroundColor(AppColors.text(for: hubColorScheme).opacity(0.70))
                        .multilineTextAlignment(.leading)
                }
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .overlay(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3, style: .continuous)
                    .fill(accent)
                    .frame(width: 4)
                    .padding(.vertical, 16)
                    .padding(.leading, 10)
            }
            .skydownPanelSurface(
                colorScheme: hubColorScheme,
                accent: accent,
                cornerRadius: 24,
                shadowRadius: 14,
                shadowYOffset: 8
            )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

#Preview {
    let services = AppServices()

    LaunchScreenView()
        .environmentObject(services)
        .environmentObject(services.authManager)
        .environmentObject(services.cartViewModel)
}
