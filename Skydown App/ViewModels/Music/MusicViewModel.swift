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

    func fetchTracks(for artist: String) async {
        let query = artist.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let urlString = "https://itunes.apple.com/search?term=\(query)&entity=song&limit=50"

        guard let url = URL(string: urlString) else {
            showUserToast("Ungültige URL", style: .error)
            tracks = []
            return
        }

        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let result = try JSONDecoder().decode(SearchResult.self, from: data)

            let correctArtistId = (artist == "ThaDude") ? 1677936430 : 1637910017

            let filteredTracks = result.results.filter { track in
                track.wrapperType == "track" && track.artistId == correctArtistId
            }

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
