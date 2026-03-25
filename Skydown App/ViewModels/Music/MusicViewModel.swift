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
    @Published var isSpotifyConnected = false
    @Published var isConnectingSpotify = false

    // Toast
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let musicService: MusicServicing

    init(musicService: MusicServicing = SpotifyMusicService()) {
        self.musicService = musicService
        self.isSpotifyConnected = musicService.isConnected
    }

    func connectSpotify() async {
        isConnectingSpotify = true
        do {
            try await musicService.connect()
            isSpotifyConnected = musicService.isConnected
            showUserToast("Spotify verbunden", style: .success)
        } catch {
            print("Dev Fehler connectSpotify:", error.localizedDescription)
            print("Spotify Konfiguration:", musicService.debugConfigurationDescription)
            if let authorizationURL = musicService.lastAuthorizationURLString {
                print("Spotify Authorize URL:", authorizationURL)
            }
            showUserToast(
                "Spotify-Verbindung fehlgeschlagen: \(error.localizedDescription)\n\(musicService.debugConfigurationDescription)\nauthorize_url=\(musicService.lastAuthorizationURLString ?? "-")",
                style: .error
            )
        }
        isConnectingSpotify = false
    }

    func fetchTracks(for artist: String) async {
        isSpotifyConnected = musicService.isConnected
        guard isSpotifyConnected else {
            tracks = []
            showUserToast("Verbinde zuerst Spotify, um Songs zu laden.", style: .info)
            return
        }

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
