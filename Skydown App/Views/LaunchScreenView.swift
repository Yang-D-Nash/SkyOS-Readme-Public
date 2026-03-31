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
                    onOpenN8N: { openShell(tab: .tools) }
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
    let onOpenN8N: () -> Void

    var body: some View {
        ZStack {
            AppColors.screenGradient(
                for: colorScheme,
                secondaryAccent: AppColors.accentHighlight(for: colorScheme)
            )
            .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 22) {
                Spacer(minLength: 0)

                VStack(alignment: .leading, spacing: 12) {
                    Text("Skydown x 22")
                        .font(.system(size: 18, weight: .semibold, design: .rounded))
                        .foregroundColor(AppColors.accent(for: colorScheme))

                    Text("Choose your lane.")
                        .font(.system(size: 42, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Du landest danach immer in der globalen App-Shell und startest nur im passenden Bereich.")
                        .font(.headline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                VStack(spacing: 14) {
                    LaunchLandingButton(
                        title: "MUSIK",
                        subtitle: "Startet direkt in Zweizwei Music innerhalb der globalen Shell.",
                        accent: AppColors.spotify(for: colorScheme),
                        action: onOpenMusic
                    )

                    LaunchLandingButton(
                        title: "VIDEOGRAPHY",
                        subtitle: "Startet direkt in Skydown Videography innerhalb der globalen Shell.",
                        accent: AppColors.accentMystic(for: colorScheme),
                        action: onOpenVideography
                    )

                    LaunchLandingButton(
                        title: "N8N TRIGGER",
                        subtitle: "Springt in den globalen Tools-Bereich, wo spaeter AI und N8N neutral andocken.",
                        accent: AppColors.accentHighlight(for: colorScheme),
                        action: onOpenN8N
                    )
                }

                Spacer(minLength: 0)
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.vertical, 28)
        }
    }
}

private struct LaunchLandingButton: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let subtitle: String
    let accent: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(title)
                        .font(.system(size: 24, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(subtitle)
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .multilineTextAlignment(.leading)
                }

                Spacer(minLength: 12)

                Image(systemName: "arrow.up.right")
                    .font(.title3.weight(.bold))
                    .foregroundColor(accent)
                    .padding(12)
                    .background(accent.opacity(0.14))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .padding(SkydownLayout.heroPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.cardBackground(for: colorScheme))
            .overlay {
                RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                    .stroke(accent.opacity(0.22), lineWidth: 1)
            }
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))
            .shadow(color: .black.opacity(colorScheme == .dark ? 0.26 : 0.08), radius: 18, y: 10)
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
