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
    private var playbackObserver: NSObjectProtocol?

    func playPreview(for track: Track) {
        guard let urlString = track.previewUrl,
              let url = URL(string: urlString) else { return }

        if currentlyPlayingId == track.trackId {
            stop()
            return
        }

        stop()
        configureAudioSession()
        player = AVPlayer(url: url)
        observePlaybackFinished()
        player?.play()
        currentlyPlayingId = track.trackId
    }

    func stop() {
        player?.pause()
        if let playbackObserver {
            NotificationCenter.default.removeObserver(playbackObserver)
            self.playbackObserver = nil
        }
        player = nil
        currentlyPlayingId = nil
    }

    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try session.setActive(true)
        } catch {
            print("Dev Fehler AudioSession:", error.localizedDescription)
        }
    }

    private func observePlaybackFinished() {
        guard let currentItem = player?.currentItem else { return }
        playbackObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: currentItem,
            queue: .main
        ) { [weak self] _ in
            self?.stop()
        }
    }
}
