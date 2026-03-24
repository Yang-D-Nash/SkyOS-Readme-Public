import Foundation

protocol MusicServicing {
    func fetchTracks(for artist: String) async throws -> [Track]
}

final class ITunesMusicService: MusicServicing {
    func fetchTracks(for artist: String) async throws -> [Track] {
        let query = artist.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let urlString = "https://itunes.apple.com/search?term=\(query)&entity=song&limit=50"

        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }

        let (data, _) = try await URLSession.shared.data(from: url)
        let result = try JSONDecoder().decode(SearchResult.self, from: data)

        let artistId = artist == "ThaDude" ? 1677936430 : 1637910017
        return result.results.filter { track in
            track.wrapperType == "track" && track.artistId == artistId
        }
    }
}
