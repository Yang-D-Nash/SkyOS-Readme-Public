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
    @State private var isActive = false
    @State private var player: AVPlayer?
    @State private var timerFired = false
    
    var body: some View {
        Group {
            if isActive {
                MainTabView()
            } else {
                if let player = player {
                    ZStack {
                        Color.black
                            .ignoresSafeArea()

                        VideoPlayer(player: player)
                            .aspectRatio(contentMode: .fit)
                            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                            .overlay {
                                RoundedRectangle(cornerRadius: 28, style: .continuous)
                                    .stroke(Color.white.opacity(0.08), lineWidth: 1)
                            }
                            .shadow(color: .black.opacity(0.32), radius: 24, y: 12)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 24)
                    }
                    .onAppear {
                        configureIntroAudioSession()
                        player.volume = 1.0
                        player.play()
                        player.isMuted = false
                    }
                    .onReceive(Timer.publish(every: 4, on: .main, in: .common).autoconnect()) { _ in
                        if !timerFired {
                            timerFired = true
                            isActive = true
                            player.pause()
                            deactivateIntroAudioSession()
                        }
                    }
                } else {
                    Color.black
                        .ignoresSafeArea()
                        .onReceive(Timer.publish(every: 2, on: .main, in: .common).autoconnect()) { _ in
                            if !timerFired {
                                timerFired = true
                                withAnimation {
                                    isActive = true
                                }
                            }
                        }
                }
            }
        }
        .onAppear {
            if let url = Bundle.main.url(forResource: "Intro Launch", withExtension: "mp4") {
                player = AVPlayer(url: url)
            } else {
                print("Video 'Intro Launch.mp4' nicht gefunden!")
            }
        }
        .onDisappear {
            deactivateIntroAudioSession()
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
}

#Preview {
    let services = AppServices()

    LaunchScreenView()
        .environmentObject(services)
        .environmentObject(services.authManager)
        .environmentObject(services.cartViewModel)
}
