//
//  MusicViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 26.07.25.
//

import Foundation

@MainActor
class MusicViewModel: ObservableObject {
    @Published var tracks: [Track] = []

    // Toast
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let musicService: MusicServicing

    init(musicService: MusicServicing = ITunesMusicService()) {
        self.musicService = musicService
    }

    func fetchTracks(for artist: String) async {
        do {
            let filteredTracks = try await musicService.fetchTracks(for: artist)

            if filteredTracks.isEmpty {
                showUserToast("Keine Songs für \(artist) gefunden", style: .error)
            } else {
                tracks = filteredTracks
                showUserToast("Songs erfolgreich geladen", style: .success)
            }

        } catch {
            print("Dev Fehler fetchTracks:", error.localizedDescription)
            showUserToast("Fehler beim Laden der Daten: \(error.localizedDescription)", style: .error)
            tracks = []
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}
