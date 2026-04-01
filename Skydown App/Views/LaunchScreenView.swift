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
    @Environment(\.colorScheme) private var colorScheme
    let onOpenMusic: () -> Void
    let onOpenVideography: () -> Void
    let onOpenShop: () -> Void

    var body: some View {
        ZStack {
            AppColors.screenGradient(
                for: colorScheme,
                secondaryAccent: AppColors.accentHighlight(for: colorScheme)
            )
            .ignoresSafeArea()

            Circle()
                .fill(AppColors.accent(for: colorScheme).opacity(colorScheme == .dark ? 0.20 : 0.12))
                .frame(width: 260, height: 260)
                .blur(radius: 50)
                .offset(x: 150, y: -280)

            Circle()
                .fill(AppColors.accentMystic(for: colorScheme).opacity(colorScheme == .dark ? 0.18 : 0.10))
                .frame(width: 240, height: 240)
                .blur(radius: 56)
                .offset(x: -140, y: 260)

            VStack(alignment: .leading, spacing: 28) {
                Spacer(minLength: 0)

                VStack(alignment: .leading, spacing: 14) {
                    Text("Sky²²")
                        .font(.caption.weight(.semibold))
                        .tracking(1.6)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    Text("Sky²²")
                        .font(.system(size: 38, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Such dir einfach aus, womit du anfangen willst. Unten kannst du danach jederzeit weiterwechseln.")
                        .font(.body.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .fixedSize(horizontal: false, vertical: true)
                }

                VStack(spacing: 14) {
                    LaunchLandingButton(
                        step: "01",
                        eyebrow: "Listen & Artists",
                        title: "Music",
                        subtitle: "Wenn du hoeren, Artists entdecken oder direkt zu Beats willst.",
                        accent: AppColors.spotify(for: colorScheme),
                        action: onOpenMusic
                    )

                    LaunchLandingButton(
                        step: "02",
                        eyebrow: "Clips & Reels",
                        title: "Video",
                        subtitle: "Wenn du Reels schauen, Produktionen sehen oder Kontakt aufnehmen willst.",
                        accent: AppColors.accentMystic(for: colorScheme),
                        action: onOpenVideography
                    )

                    LaunchLandingButton(
                        step: "03",
                        eyebrow: "Store",
                        title: "Shop",
                        subtitle: "Wenn du stoebbern, Produkte gross ansehen oder direkt bestellen willst.",
                        accent: AppColors.accentHighlight(for: colorScheme),
                        action: onOpenShop
                    )
                }

                Text("Danach bleibst du im normalen App-Flow und kannst unten jederzeit wechseln.")
                    .font(.footnote.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .padding(.top, 4)

                Spacer(minLength: 0)
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.vertical, 32)
        }
    }
}

private struct LaunchLandingButton: View {
    @Environment(\.colorScheme) private var colorScheme
    let step: String
    let eyebrow: String
    let title: String
    let subtitle: String
    let accent: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 16) {
                VStack(alignment: .leading, spacing: 12) {
                    Text(step)
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                        .foregroundColor(accent)

                    VStack(alignment: .leading, spacing: 5) {
                        Text(eyebrow.uppercased())
                            .font(.caption.weight(.semibold))
                            .tracking(1.2)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        Text(title)
                            .font(.system(size: 28, weight: .black, design: .rounded))
                            .foregroundColor(AppColors.text(for: colorScheme))
                    }

                    Text(subtitle)
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .multilineTextAlignment(.leading)
                }
            }
            .padding(22)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(AppColors.cardBackground(for: colorScheme).opacity(0.94))
            )
            .overlay {
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .stroke(accent.opacity(0.18), lineWidth: 1)
            }
            .overlay(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3, style: .continuous)
                    .fill(accent)
                    .frame(width: 4)
                    .padding(.vertical, 18)
                    .padding(.leading, 10)
            }
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
            .shadow(color: .black.opacity(colorScheme == .dark ? 0.20 : 0.06), radius: 16, y: 10)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    let services = AppServices()

    LaunchScreenView()
        .environmentObject(services)
        .environmentObject(services.authManager)
        .environmentObject(services.cartViewModel)
}
