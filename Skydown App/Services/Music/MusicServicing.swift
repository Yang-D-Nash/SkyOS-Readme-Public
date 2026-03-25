import AuthenticationServices
import CryptoKit
import Foundation
import UIKit

protocol MusicServicing {
    var isConnected: Bool { get }
    var debugConfigurationDescription: String { get }
    var lastAuthorizationURLString: String? { get }
    func connect() async throws
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
        static let artistIDs: [String: String] = [
            "Yang D. Nash": "63Sh0kQAWW3ZWn2aKDksbo",
            "ThaDude": "0Jmb7DXFkKxxRjqD70vi0e",
            "MAVE": "0GXymtRaIk2ngbXSkcHtsp",
            "JANNO": "7hpiHzP9aLLb5liDLxtwhM",
            "TANGAJOE007": "0OA5dgpVdwzI8K82m8FPxN",
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

    private struct SpotifySearchResponse: Decodable {
        let tracks: SpotifyTracks
    }

    private struct SpotifyTracks: Decodable {
        let items: [SpotifyTrack]
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

    func fetchTracks(for artist: String) async throws -> [Track] {
        let accessToken = try await validAccessToken(forceRefresh: false, allowInteractiveAuth: false)
        let artistID = Constants.artistIDs[artist]
        let query = "artist:\(artist)"
            .addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? artist
        let urlString = "\(Constants.apiBaseURL)/search?q=\(query)&type=track&limit=50"

        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }

        var request = URLRequest(url: url)
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")

        let (data, urlResponse) = try await session.data(for: request)
        if let httpResponse = urlResponse as? HTTPURLResponse, !(200...299).contains(httpResponse.statusCode) {
            let payload = String(data: data, encoding: .utf8) ?? "<empty>"
            print("Spotify Search Error:", httpResponse.statusCode, payload)
            throw NSError(
                domain: "SpotifyMusicService",
                code: httpResponse.statusCode,
                userInfo: [NSLocalizedDescriptionKey: "Spotify API Fehler \(httpResponse.statusCode): \(payload)"]
            )
        }
        let searchResponse = try decoder.decode(SpotifySearchResponse.self, from: data)

        let mappedTracks = searchResponse.tracks.items.map { item in
            let primaryArtistID = item.artists.first?.id
            let primaryArtistName = item.artists.first?.name
            let artworkURL = item.album.images.first?.url
            return Track(
                trackId: abs(item.id.hashValue),
                artistId: abs((primaryArtistID ?? item.id).hashValue),
                spotifyArtistID: primaryArtistID,
                artistName: primaryArtistName,
                trackName: item.name,
                collectionName: item.album.name,
                artworkUrl100: artworkURL,
                previewUrl: item.previewURL,
                externalURL: item.externalURLs["spotify"],
                wrapperType: "track"
            )
        }

        return mappedTracks.filter { track in
            if let artistID {
                return track.spotifyArtistID == artistID
            }
            return track.artistName?.caseInsensitiveCompare(artist) == .orderedSame
        }
    }

    private func validAccessToken(
        forceRefresh: Bool,
        allowInteractiveAuth: Bool
    ) async throws -> String {
        if let storedToken = loadStoredToken(), !forceRefresh, storedToken.expirationDate > Date().addingTimeInterval(60) {
            return storedToken.accessToken
        }

        if let refreshToken = loadStoredToken()?.refreshToken {
            let refreshed = try await refreshAccessToken(using: refreshToken)
            saveStoredToken(refreshed)
            return refreshed.accessToken
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
            URLQueryItem(name: "show_dialog", value: "false"),
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
        var request = URLRequest(url: URL(string: Constants.tokenURL)!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = formBody([
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": Constants.redirectURI,
            "client_id": Constants.clientID,
            "code_verifier": verifier,
        ])

        let (data, _) = try await session.data(for: request)
        let tokenResponse = try decoder.decode(SpotifyTokenResponse.self, from: data)
        return StoredToken(
            accessToken: tokenResponse.accessToken,
            refreshToken: tokenResponse.refreshToken,
            expirationDate: Date().addingTimeInterval(TimeInterval(tokenResponse.expiresIn))
        )
    }

    private func refreshAccessToken(using refreshToken: String) async throws -> StoredToken {
        var request = URLRequest(url: URL(string: Constants.tokenURL)!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = formBody([
            "grant_type": "refresh_token",
            "refresh_token": refreshToken,
            "client_id": Constants.clientID,
        ])

        let (data, _) = try await session.data(for: request)
        let tokenResponse = try decoder.decode(SpotifyTokenResponse.self, from: data)
        return StoredToken(
            accessToken: tokenResponse.accessToken,
            refreshToken: tokenResponse.refreshToken ?? refreshToken,
            expirationDate: Date().addingTimeInterval(TimeInterval(tokenResponse.expiresIn))
        )
    }

    private func formBody(_ values: [String: String]) -> Data? {
        let body = values
            .map { key, value in
                let encodedValue = value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? value
                return "\(key)=\(encodedValue)"
            }
            .joined(separator: "&")
        return body.data(using: .utf8)
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
