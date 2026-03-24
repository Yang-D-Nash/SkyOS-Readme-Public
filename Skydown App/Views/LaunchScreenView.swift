//
//  LaunchScreenView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 21.08.25.
//

import SwiftUI
import AVKit

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
                    VideoPlayer(player: player)
                        .ignoresSafeArea()
                        .scaledToFill()
                        .onAppear {
                            player.play()
                            player.isMuted = false
                        }
                        .onReceive(Timer.publish(every: 4, on: .main, in: .common).autoconnect()) { _ in
                            if !timerFired {
                                timerFired = true
                                isActive = true
                                player.pause()
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
    }
}

#Preview {
    LaunchScreenView()
        .environmentObject(AuthManager())
}
