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

            VStack(alignment: .leading, spacing: 22) {
                Spacer(minLength: 0)

                BrandHeroSurface(
                    colorScheme: colorScheme,
                    eyebrow: "Skydown x Zweizwei",
                    title: "Wohin soll's zuerst gehen?",
                    subtitle: "Beide Brands fließen jetzt direkt in den Start der App ein und geben Home, Videography, Music und Merchandise einen klareren Premium-Rahmen.",
                    detail: "Du bleibst danach in der App und startest direkt im passenden Bereich.",
                    accent: AppColors.accent(for: colorScheme),
                    secondaryAccent: AppColors.accentMystic(for: colorScheme),
                    marks: [.skydown, .zweizwei]
                ) {
                    HStack(spacing: 10) {
                        BrandHeroPill(
                            text: "Merchandise",
                            colorScheme: colorScheme,
                            tint: AppColors.accentHighlight(for: colorScheme)
                        )
                        BrandHeroPill(
                            text: "Zweizwei",
                            colorScheme: colorScheme,
                            tint: AppColors.spotify(for: colorScheme)
                        )
                        BrandHeroPill(
                            text: "Skydown",
                            colorScheme: colorScheme,
                            tint: AppColors.accentMystic(for: colorScheme)
                        )
                    }
                }

                VStack(spacing: 14) {
                    LaunchLandingButton(
                        title: "MUSIK",
                        subtitle: "Du startest direkt im Zweizwei-Musikbereich.",
                        accent: AppColors.spotify(for: colorScheme),
                        action: onOpenMusic
                    )

                    LaunchLandingButton(
                        title: "VIDEOGRAPHY",
                        subtitle: "Du startest direkt bei Skydown Videography.",
                        accent: AppColors.accentMystic(for: colorScheme),
                        action: onOpenVideography
                    )

                    LaunchLandingButton(
                        title: "SHOP",
                        subtitle: "Du landest direkt im Merchandise-Bereich.",
                        accent: AppColors.accentHighlight(for: colorScheme),
                        action: onOpenShop
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
            .background(AppColors.cardBackground(for: colorScheme).opacity(0.96))
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
