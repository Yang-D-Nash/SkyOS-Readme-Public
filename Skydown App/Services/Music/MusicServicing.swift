import AuthenticationServices
import CryptoKit
import Foundation
import UIKit

protocol MusicServicing {
    var isConnected: Bool { get }
    var debugConfigurationDescription: String { get }
    var lastAuthorizationURLString: String? { get }
    func connect() async throws
    func disconnect()
    func fetchTracks(for artist: String) async throws -> [Track]
}

enum SpotifyAuthError: LocalizedError {
    case invalidCallback
    case invalidState
    case missingAuthorizationCode
    case missingToken

    var errorDescription: String? {
        switch self {
        case .invalidCallback:
            return "Spotify-Autorisierung konnte nicht abgeschlossen werden."
        case .invalidState:
            return "Spotify-Login ist ungueltig. Bitte erneut verbinden."
        case .missingAuthorizationCode:
            return "Spotify hat keinen Autorisierungscode geliefert."
        case .missingToken:
            return "Spotify hat kein Zugriffstoken geliefert."
        }
    }
}

final class SpotifyMusicService: NSObject, MusicServicing {
    private enum Constants {
        static let clientID = "e22e102e5cd847bb8f59a140fda76bcc"
        static let redirectURI = "skydown://spotify-auth"
        static let authorizeURL = "https://accounts.spotify.com/authorize"
        static let tokenURL = "https://accounts.spotify.com/api/token"
        static let apiBaseURL = "https://api.spotify.com/v1"
        static let callbackScheme = "skydown"
        static let searchPageSize = 10
        static let searchMaxResults = 50
        static let artistAlbumsPageSize = 10
        static let artistAlbumsMaxResults = 100
        static let albumTracksPageSize = 50
        static let artistIDs: [String: String] = [
            "Yang D. Nash": "63Sh0kQAWW3ZWn2aKDksbo",
            "ThaDude": "0Jmb7DXFkKxxRjqD70vi0e",
            "MAVE": "0GXymtRaIk2ngbXSkcHtsp",
            "JANNO": "7hpiHzP9aLLb5liDLxtwhM",
            "TANGAJOE007": "0OA5dgpVdwzI8K82m8FPxN",
            "Toprack941": "4CoozMQ3B3I20day60N7QA",
        ]
    }

    private struct SpotifyTokenResponse: Decodable {
        let accessToken: String
        let tokenType: String
        let expiresIn: Int
        let refreshToken: String?

        enum CodingKeys: String, CodingKey {
            case accessToken = "access_token"
            case tokenType = "token_type"
            case expiresIn = "expires_in"
            case refreshToken = "refresh_token"
        }
    }

    private struct SpotifyErrorResponse: Decodable {
        let error: String?
        let errorDescription: String?

        enum CodingKeys: String, CodingKey {
            case error
            case errorDescription = "error_description"
        }
    }

    private struct SpotifyHTTPError: LocalizedError {
        let statusCode: Int
        let payload: String

        var errorDescription: String? {
            "Spotify API Fehler \(statusCode): \(payload)"
        }
    }

    private struct SpotifySearchResponse: Decodable {
        let tracks: SpotifyTracks
    }

    private struct SpotifyTracks: Decodable {
        let items: [SpotifyTrack]
    }

    private struct SpotifyArtistAlbumsResponse: Decodable {
        let items: [SpotifyArtistAlbum]
    }

    private struct SpotifyArtistAlbum: Decodable {
        let id: String
        let name: String
        let images: [SpotifyImage]
        let totalTracks: Int
        let releaseDate: String?

        enum CodingKeys: String, CodingKey {
            case id
            case name
            case images
            case totalTracks = "total_tracks"
            case releaseDate = "release_date"
        }
    }

    private struct SpotifyAlbumTracksResponse: Decodable {
        let items: [SpotifyAlbumTrack]
    }

    private struct SpotifyAlbumTrack: Decodable {
        let id: String
        let name: String
        let previewURL: String?
        let artists: [SpotifyArtist]
        let externalURLs: [String: String]

        enum CodingKeys: String, CodingKey {
            case id
            case name
            case previewURL = "preview_url"
            case artists
            case externalURLs = "external_urls"
        }
    }

    private struct SpotifyTrack: Decodable {
        let id: String
        let name: String
        let previewURL: String?
        let album: SpotifyAlbum
        let artists: [SpotifyArtist]
        let externalURLs: [String: String]

        enum CodingKeys: String, CodingKey {
            case id
            case name
            case previewURL = "preview_url"
            case album
            case artists
            case externalURLs = "external_urls"
        }
    }

    private struct SpotifyAlbum: Decodable {
        let name: String
        let images: [SpotifyImage]
        let releaseDate: String?

        enum CodingKeys: String, CodingKey {
            case name
            case images
            case releaseDate = "release_date"
        }
    }

    private struct SpotifyArtist: Decodable {
        let id: String
        let name: String
    }

    private struct SpotifyImage: Decodable {
        let url: String
    }

    private struct StoredToken: Codable {
        let accessToken: String
        let refreshToken: String?
        let expirationDate: Date
    }

    private final class PresentationAnchorProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
        func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
            let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            return scenes
                .flatMap(\.windows)
                .first(where: \.isKeyWindow) ?? ASPresentationAnchor()
        }
    }

    private let session = URLSession.shared
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()
    private let userDefaults = UserDefaults.standard
    private let tokenStorageKey = "spotify.token.storage"
    private let anchorProvider = PresentationAnchorProvider()
    private var authorizationURLString: String?

    var isConnected: Bool {
        guard let token = loadStoredToken() else { return false }
        return token.expirationDate > Date().addingTimeInterval(60)
    }

    var debugConfigurationDescription: String {
        "client_id=\(Constants.clientID) redirect_uri=\(Constants.redirectURI)"
    }

    var lastAuthorizationURLString: String? {
        authorizationURLString
    }

    func connect() async throws {
        _ = try await validAccessToken(forceRefresh: false, allowInteractiveAuth: true)
    }

    func disconnect() {
        userDefaults.removeObject(forKey: tokenStorageKey)
        authorizationURLString = nil
    }

    func fetchTracks(for artist: String) async throws -> [Track] {
        let accessToken = try await validAccessToken(forceRefresh: false, allowInteractiveAuth: false)
        let artistID = Constants.artistIDs[artist]
        if let artistID {
            let directTracks = try await fetchKnownArtistTracks(
                accessToken: accessToken,
                artist: artist,
                artistID: artistID
            )
            if !directTracks.isEmpty {
                return directTracks
            }
        }

        let searchQueries = searchQueries(for: artist)
        var lastError: Error?
        var searchItems: [SpotifyTrack] = []

        for (index, query) in searchQueries.enumerated() {
            do {
                searchItems = try await performSearch(query: query, accessToken: accessToken)
                lastError = nil
                break
            } catch let error as SpotifyHTTPError where error.statusCode == 400 && index < searchQueries.count - 1 {
                print("Spotify Search Retry:", error.statusCode, error.payload)
                lastError = error
                continue
            } catch {
                throw error
            }
        }

        guard !searchItems.isEmpty || lastError == nil else {
            throw lastError ?? SpotifyAuthError.missingToken
        }

        let mappedTracks = searchItems.map { item in
            let matchingArtist: SpotifyArtist?
            if let artistID {
                matchingArtist = item.artists.first(where: { $0.id == artistID })
            } else {
                matchingArtist = item.artists.first
            }
            let artworkURL = item.album.images.first?.url
            return Track(
                trackId: abs(item.id.hashValue),
                artistId: abs((matchingArtist?.id ?? item.artists.first?.id ?? item.id).hashValue),
                spotifyArtistID: matchingArtist?.id ?? item.artists.first?.id,
                artistName: matchingArtist?.name ?? item.artists.first?.name,
                trackName: item.name,
                collectionName: item.album.name,
                artworkUrl100: artworkURL,
                previewUrl: item.previewURL,
                externalURL: item.externalURLs["spotify"],
                wrapperType: "track",
                releaseDate: item.album.releaseDate
            )
        }

        return mappedTracks.filter { track in
            if let artistID {
                return track.spotifyArtistID == artistID
            }
            return track.artistName?.caseInsensitiveCompare(artist) == .orderedSame
        }
    }

    private func fetchKnownArtistTracks(
        accessToken: String,
        artist: String,
        artistID: String
    ) async throws -> [Track] {
        let albums = try await fetchArtistAlbums(
            accessToken: accessToken,
            artistID: artistID
        ).sorted { ($0.releaseDate ?? "") > ($1.releaseDate ?? "") }

        var orderedTrackIDs: [String] = []
        var tracksByID: [String: Track] = [:]

        for album in albums {
            let albumTracks = try await fetchAlbumTracks(
                accessToken: accessToken,
                album: album,
                artistID: artistID
            )

            for item in albumTracks where tracksByID[item.id] == nil {
                let matchingArtist = item.artists.first(where: { $0.id == artistID })
                orderedTrackIDs.append(item.id)
                tracksByID[item.id] = Track(
                    trackId: abs(item.id.hashValue),
                    artistId: abs(artistID.hashValue),
                    spotifyArtistID: artistID,
                    artistName: matchingArtist?.name ?? artist,
                    trackName: item.name,
                    collectionName: album.name,
                    artworkUrl100: album.images.first?.url,
                    previewUrl: item.previewURL,
                    externalURL: item.externalURLs["spotify"],
                    wrapperType: "track",
                    releaseDate: album.releaseDate
                )
            }
        }

        return orderedTrackIDs.compactMap { tracksByID[$0] }
    }

    private func fetchArtistAlbums(
        accessToken: String,
        artistID: String
    ) async throws -> [SpotifyArtistAlbum] {
        var albumsByID: [String: SpotifyArtistAlbum] = [:]
        var albumOrder: [String] = []

        for offset in stride(from: 0, to: Constants.artistAlbumsMaxResults, by: Constants.artistAlbumsPageSize) {
            let data = try await performGetRequest(
                url: try artistAlbumsURL(for: artistID, offset: offset),
                accessToken: accessToken
            )
            let page = try decoder.decode(SpotifyArtistAlbumsResponse.self, from: data).items

            for album in page where albumsByID[album.id] == nil {
                albumOrder.append(album.id)
                albumsByID[album.id] = album
            }

            if page.count < Constants.artistAlbumsPageSize {
                break
            }
        }

        return albumOrder.compactMap { albumsByID[$0] }
    }

    private func fetchAlbumTracks(
        accessToken: String,
        album: SpotifyArtistAlbum,
        artistID: String
    ) async throws -> [SpotifyAlbumTrack] {
        guard album.totalTracks > 0 else { return [] }

        var tracks: [SpotifyAlbumTrack] = []

        for offset in stride(from: 0, to: album.totalTracks, by: Constants.albumTracksPageSize) {
            let data = try await performGetRequest(
                url: try albumTracksURL(for: album.id, offset: offset),
                accessToken: accessToken
            )
            let page = try decoder.decode(SpotifyAlbumTracksResponse.self, from: data).items
            tracks.append(contentsOf: page.filter { track in
                track.artists.contains(where: { $0.id == artistID })
            })

            if page.count < Constants.albumTracksPageSize {
                break
            }
        }

        return tracks
    }

    private func validAccessToken(
        forceRefresh: Bool,
        allowInteractiveAuth: Bool
    ) async throws -> String {
        if let storedToken = loadStoredToken(), !forceRefresh, storedToken.expirationDate > Date().addingTimeInterval(60) {
            return storedToken.accessToken
        }

        if let refreshToken = loadStoredToken()?.refreshToken {
            do {
                let refreshed = try await refreshAccessToken(using: refreshToken)
                saveStoredToken(refreshed)
                return refreshed.accessToken
            } catch {
                disconnect()
                if allowInteractiveAuth {
                    let authorized = try await authorizeWithPKCE()
                    saveStoredToken(authorized)
                    return authorized.accessToken
                }
                throw SpotifyAuthError.missingToken
            }
        }

        guard allowInteractiveAuth else {
            throw SpotifyAuthError.missingToken
        }

        let authorized = try await authorizeWithPKCE()
        saveStoredToken(authorized)
        return authorized.accessToken
    }

    private func authorizeWithPKCE() async throws -> StoredToken {
        let verifier = randomVerifier()
        let challenge = codeChallenge(for: verifier)
        let state = UUID().uuidString
        let authURL = try authorizationURL(codeChallenge: challenge, state: state)
        authorizationURLString = authURL.absoluteString
        print("Spotify Authorize URL:", authURL.absoluteString)

        let callbackURL = try await startAuthenticationSession(url: authURL)
        guard let components = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false) else {
            throw SpotifyAuthError.invalidCallback
        }

        let returnedState = components.queryItems?.first(where: { $0.name == "state" })?.value
        guard returnedState == state else {
            throw SpotifyAuthError.invalidState
        }

        guard let code = components.queryItems?.first(where: { $0.name == "code" })?.value else {
            throw SpotifyAuthError.missingAuthorizationCode
        }

        return try await exchangeCodeForToken(code: code, verifier: verifier)
    }

    private func authorizationURL(codeChallenge: String, state: String) throws -> URL {
        var components = URLComponents(string: Constants.authorizeURL)
        components?.queryItems = [
            URLQueryItem(name: "client_id", value: Constants.clientID),
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "redirect_uri", value: Constants.redirectURI),
            URLQueryItem(name: "code_challenge_method", value: "S256"),
            URLQueryItem(name: "code_challenge", value: codeChallenge),
            URLQueryItem(name: "state", value: state),
            URLQueryItem(name: "show_dialog", value: "true"),
        ]

        guard let url = components?.url else {
            throw URLError(.badURL)
        }
        return url
    }

    private func startAuthenticationSession(url: URL) async throws -> URL {
        try await withCheckedThrowingContinuation { continuation in
            let session = ASWebAuthenticationSession(
                url: url,
                callbackURLScheme: Constants.callbackScheme
            ) { callbackURL, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let callbackURL else {
                    continuation.resume(throwing: SpotifyAuthError.invalidCallback)
                    return
                }

                continuation.resume(returning: callbackURL)
            }
            session.presentationContextProvider = anchorProvider
            session.prefersEphemeralWebBrowserSession = false
            session.start()
        }
    }

    private func exchangeCodeForToken(code: String, verifier: String) async throws -> StoredToken {
        try await performTokenRequest([
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": Constants.redirectURI,
            "client_id": Constants.clientID,
            "code_verifier": verifier,
        ])
    }

    private func refreshAccessToken(using refreshToken: String) async throws -> StoredToken {
        try await performTokenRequest([
            "grant_type": "refresh_token",
            "refresh_token": refreshToken,
            "client_id": Constants.clientID,
        ], fallbackRefreshToken: refreshToken)
    }

    private func performTokenRequest(
        _ parameters: [String: String],
        fallbackRefreshToken: String? = nil
    ) async throws -> StoredToken {
        var request = URLRequest(url: URL(string: Constants.tokenURL)!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.httpBody = formBody(parameters)

        let (data, response) = try await session.data(for: request)
        if let httpResponse = response as? HTTPURLResponse, !(200...299).contains(httpResponse.statusCode) {
            let apiError = try? decoder.decode(SpotifyErrorResponse.self, from: data)
            let payload = apiError?.errorDescription
                ?? String(data: data, encoding: .utf8)
                ?? "<empty>"
            throw SpotifyHTTPError(statusCode: httpResponse.statusCode, payload: payload)
        }

        let tokenResponse = try decoder.decode(SpotifyTokenResponse.self, from: data)
        return StoredToken(
            accessToken: tokenResponse.accessToken,
            refreshToken: tokenResponse.refreshToken ?? fallbackRefreshToken,
            expirationDate: Date().addingTimeInterval(TimeInterval(tokenResponse.expiresIn))
        )
    }

    private func formBody(_ values: [String: String]) -> Data? {
        let body = values
            .map { key, value in
                "\(key.percentEncodedFormValue())=\(value.percentEncodedFormValue())"
            }
            .joined(separator: "&")
        return body.data(using: .utf8)
    }

    private func searchURL(forQuery query: String, offset: Int) throws -> URL {
        guard var components = URLComponents(string: "\(Constants.apiBaseURL)/search") else {
            throw URLError(.badURL)
        }

        components.queryItems = [
            URLQueryItem(name: "q", value: query),
            URLQueryItem(name: "type", value: "track"),
            URLQueryItem(name: "limit", value: "\(Constants.searchPageSize)"),
            URLQueryItem(name: "offset", value: "\(offset)"),
        ]

        guard let url = components.url else {
            throw URLError(.badURL)
        }
        return url
    }

    private func artistAlbumsURL(for artistID: String, offset: Int) throws -> URL {
        guard var components = URLComponents(string: "\(Constants.apiBaseURL)/artists/\(artistID)/albums") else {
            throw URLError(.badURL)
        }

        components.queryItems = [
            URLQueryItem(name: "include_groups", value: "album,single,appears_on,compilation"),
            URLQueryItem(name: "limit", value: "\(Constants.artistAlbumsPageSize)"),
            URLQueryItem(name: "offset", value: "\(offset)")
        ]

        guard let url = components.url else {
            throw URLError(.badURL)
        }
        return url
    }

    private func albumTracksURL(for albumID: String, offset: Int) throws -> URL {
        guard var components = URLComponents(string: "\(Constants.apiBaseURL)/albums/\(albumID)/tracks") else {
            throw URLError(.badURL)
        }

        components.queryItems = [
            URLQueryItem(name: "limit", value: "\(Constants.albumTracksPageSize)"),
            URLQueryItem(name: "offset", value: "\(offset)")
        ]

        guard let url = components.url else {
            throw URLError(.badURL)
        }
        return url
    }

    private func searchQueries(for artist: String) -> [String] {
        [
            "artist:\"\(artist)\"",
            artist,
        ]
    }

    private func performSearch(query: String, accessToken: String) async throws -> [SpotifyTrack] {
        var items: [SpotifyTrack] = []

        for offset in stride(from: 0, to: Constants.searchMaxResults, by: Constants.searchPageSize) {
            let data = try await performSearchRequest(
                query: query,
                accessToken: accessToken,
                offset: offset
            )
            let page = try decoder.decode(SpotifySearchResponse.self, from: data).tracks.items
            items.append(contentsOf: page)

            if page.count < Constants.searchPageSize {
                break
            }
        }

        return items
    }

    private func performSearchRequest(query: String, accessToken: String, offset: Int) async throws -> Data {
        let url = try searchURL(forQuery: query, offset: offset)

        return try await performGetRequest(url: url, accessToken: accessToken)
    }

    private func performGetRequest(url: URL, accessToken: String) async throws -> Data {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, urlResponse) = try await session.data(for: request)
        if let httpResponse = urlResponse as? HTTPURLResponse, !(200...299).contains(httpResponse.statusCode) {
            let payload = String(data: data, encoding: .utf8) ?? "<empty>"
            print("Spotify Search Error:", httpResponse.statusCode, payload)
            throw SpotifyHTTPError(statusCode: httpResponse.statusCode, payload: payload)
        }

        return data
    }

    private func randomVerifier() -> String {
        let data = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
        return data.base64URLEncodedString()
    }

    private func codeChallenge(for verifier: String) -> String {
        let digest = SHA256.hash(data: Data(verifier.utf8))
        return Data(digest).base64URLEncodedString()
    }

    private func loadStoredToken() -> StoredToken? {
        guard let data = userDefaults.data(forKey: tokenStorageKey) else { return nil }
        return try? decoder.decode(StoredToken.self, from: data)
    }

    private func saveStoredToken(_ token: StoredToken) {
        guard let data = try? encoder.encode(token) else { return }
        userDefaults.set(data, forKey: tokenStorageKey)
    }
}

private extension Data {
    func base64URLEncodedString() -> String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

private extension String {
    func percentEncodedFormValue() -> String {
        let allowedCharacters = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-._*"))
        return addingPercentEncoding(withAllowedCharacters: allowedCharacters)?
            .replacingOccurrences(of: "%20", with: "+") ?? self
    }
}
