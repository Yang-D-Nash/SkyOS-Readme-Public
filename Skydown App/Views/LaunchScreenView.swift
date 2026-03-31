//
//  LaunchScreenView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 21.08.25.
//

import SwiftUI
import AVKit
import AVFoundation

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
                    onOpenMusic: { openMusicLane() },
                    onOpenVideography: { openVideographyLane() },
                    onOpenN8N: { openN8NLanding() }
                )
            case .n8n:
                N8NLaunchView(onBack: { transitionBackToLanding() })
            case .musicLane:
                ZweizweiMusicLaneView(onBackToLanding: { transitionBackToLanding() })
            case .videographyLane:
                NavigationStack {
                    VideoHubView(onBack: { transitionBackToLanding() })
                }
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

    private func openMusicLane() {
        withAnimation(.easeInOut(duration: 0.3)) {
            phase = .musicLane
        }
    }

    private func openVideographyLane() {
        withAnimation(.easeInOut(duration: 0.3)) {
            phase = .videographyLane
        }
    }

    private func openN8NLanding() {
        withAnimation(.easeInOut(duration: 0.3)) {
            phase = .n8n
        }
    }

    private func transitionBackToLanding() {
        withAnimation(.easeInOut(duration: 0.3)) {
            phase = .landing
        }
    }
}

private enum LaunchPhase {
    case intro
    case landing
    case n8n
    case musicLane
    case videographyLane
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

                    Text("Spring direkt in Zweizwei Music oder in die Skydown Videography-Welt der App.")
                        .font(.headline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                VStack(spacing: 14) {
                    LaunchLandingButton(
                        title: "MUSIK",
                        subtitle: "Zweizwei steht hier fuer Songs, Releases, Beats und den kompletten Music-Hub.",
                        accent: AppColors.spotify(for: colorScheme),
                        action: onOpenMusic
                    )

                    LaunchLandingButton(
                        title: "VIDEOGRAPHY",
                        subtitle: "Skydown buendelt hier Clips, Reels, Sessions und den gesamten Video-Hub.",
                        accent: AppColors.accentMystic(for: colorScheme),
                        action: onOpenVideography
                    )

                    LaunchLandingButton(
                        title: "N8N TRIGGER",
                        subtitle: "Leere Workflow-Landing-Page als Startpunkt fuer spaetere Automationen.",
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

private struct ZweizweiMusicLaneView: View {
    private enum Destination {
        case hub
        case catalog
        case beatHub
        case nicma
    }

    @Environment(\.colorScheme) private var colorScheme
    @State private var destination: Destination = .hub
    let onBackToLanding: () -> Void

    var body: some View {
        switch destination {
        case .hub:
            laneHub
        case .catalog:
            MusicView(
                brand: .zweizwei,
                onBack: { destination = .hub }
            )
        case .beatHub:
            NavigationStack {
                BeatHubView(onBack: { destination = .hub })
            }
        case .nicma:
            NavigationStack {
                NicmaProducerView(onBack: { destination = .hub })
            }
        }
    }

    private var laneHub: some View {
        ZStack {
            AppColors.screenGradient(
                for: colorScheme,
                secondaryAccent: AppColors.spotify(for: colorScheme)
            )
            .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 18) {
                Button(action: onBackToLanding) {
                    Label("Zurueck", systemImage: "chevron.left")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(AppColors.cardBackground(for: colorScheme))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                Spacer(minLength: 0)

                VStack(alignment: .leading, spacing: 12) {
                    Text("Zweizwei Music")
                        .font(.system(size: 38, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Hier lebt die eigene Musik-Logik: Catalog, Beat Hub und NICMA Producer Services laufen getrennt von Skydown Videography.")
                        .font(.headline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                VStack(spacing: 14) {
                    LaunchLandingButton(
                        title: "MUSIC CATALOG",
                        subtitle: "Artists, Releases, Preview-Playback und Spotify-Fokus unter Zweizwei.",
                        accent: AppColors.spotify(for: colorScheme),
                        action: { destination = .catalog }
                    )

                    LaunchLandingButton(
                        title: "BEAT HUB",
                        subtitle: "Eigene Beat-Logik, Preview-Library und Upload-/Listener-Flow.",
                        accent: AppColors.accent(for: colorScheme),
                        action: { destination = .beatHub }
                    )

                    LaunchLandingButton(
                        title: "NICMA PRODUCER",
                        subtitle: "Mixing, Mastering und Recording als eigener Music-Service.",
                        accent: AppColors.accentMystic(for: colorScheme),
                        action: { destination = .nicma }
                    )
                }

                Spacer(minLength: 0)
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.vertical, 28)
        }
    }
}

private struct N8NLaunchView: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var hasTriggeredPlaceholder = false
    let onBack: () -> Void

    var body: some View {
        ZStack {
            AppColors.screenGradient(
                for: colorScheme,
                secondaryAccent: AppColors.accentMystic(for: colorScheme)
            )
            .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 18) {
                Button(action: onBack) {
                    Label("Zurueck", systemImage: "chevron.left")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(AppColors.cardBackground(for: colorScheme))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                Spacer(minLength: 0)

                VStack(alignment: .leading, spacing: 14) {
                    Text("N8N Trigger")
                        .font(.system(size: 36, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Hier kann spaeter dein automatisierter Workflow andocken. Fuer jetzt bleibt der Bereich absichtlich leer und gibt dir nur einen sichtbaren Trigger-Startpunkt.")
                        .font(.headline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    LaunchLandingButton(
                        title: "TRIGGER STARTEN",
                        subtitle: hasTriggeredPlaceholder
                            ? "Platzhalter wurde ausgelost. Hier kann als naechstes der N8N-Flow andocken."
                            : "Aktuell noch ohne Webhook. Nutze den Button als Trigger-Slot fuer den spaeteren N8N-Workflow.",
                        accent: AppColors.accentHighlight(for: colorScheme),
                        action: {
                            hasTriggeredPlaceholder = true
                        }
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
