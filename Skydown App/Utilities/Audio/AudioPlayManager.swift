//
//  AudioPlayManager.swift
//  Skydown App
//
//  Created by Yang D. Nash on 26.07.25.
//

import Foundation
import AVFoundation

class AudioPlayerManager: ObservableObject {
    @Published var currentlyPlayingId: Int?
    private var player: AVPlayer?

    func playPreview(for track: Track) {
        guard let urlString = track.previewUrl,
              let url = URL(string: urlString) else { return }

        if currentlyPlayingId == track.trackId {
            stop()
            return
        }

        stop()
        player = AVPlayer(url: url)
        player?.play()
        currentlyPlayingId = track.trackId
    }

    func stop() {
        player?.pause()
        player = nil
        currentlyPlayingId = nil
    }
}
