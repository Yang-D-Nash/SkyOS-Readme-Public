import AuthenticationServices
import Foundation
import UIKit

enum SpotifyMusicConstants {
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
    static let publicFallbackTargetTrackCount = 6
    static let publicFallbackMaxAlbumPages = 8
    static let artistIDs: [String: String] = [
        // Canonical names used across Music Hub.
        "JANNO": "7hpiHzP9aLLb5liDLxtwhM",
        "MAVE040": "0GXymtRaIk2ngbXSkcHtsp",
        "Tangajoe007": "0OA5dgpVdwzI8K82m8FPxN",
        "DANGU61": "08rIanUNO6en6coKEafyPO",
        "Yang D. Nash": "63Sh0kQAWW3ZWn2aKDksbo",
        "ThaDude": "0Jmb7DXFkKxxRjqD70vi0e",
        "NICMA MUSIC": "0OoRIo7pJjtLgg3qyf1oDS",
        // Legacy aliases kept for compatibility with old data/content.
        "Janno": "7hpiHzP9aLLb5liDLxtwhM",
        "Mave": "0GXymtRaIk2ngbXSkcHtsp",
        "MAVE": "0GXymtRaIk2ngbXSkcHtsp",
        "Tangajoe": "0OA5dgpVdwzI8K82m8FPxN",
        "TANGAJOE007": "0OA5dgpVdwzI8K82m8FPxN",
        "THADUDE": "0Jmb7DXFkKxxRjqD70vi0e",
        "NICMA": "0OoRIo7pJjtLgg3qyf1oDS"
    ]
}

struct SpotifyTokenResponse: Decodable {
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

struct SpotifyErrorResponse: Decodable {
    let error: String?
    let errorDescription: String?

    enum CodingKeys: String, CodingKey {
        case error
        case errorDescription = "error_description"
    }
}

struct SpotifyHTTPError: LocalizedError {
    let statusCode: Int
    let payload: String

    var errorDescription: String? {
        "Spotify API Fehler \(statusCode): \(payload)"
    }
}

struct SpotifySearchResponse: Decodable {
    let tracks: SpotifyTracks
}

struct SpotifyTracks: Decodable {
    let items: [SpotifyTrack]
}

struct SpotifyArtistAlbumsResponse: Decodable {
    let items: [SpotifyArtistAlbum]
}

struct SpotifyArtistAlbum: Decodable {
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

struct SpotifyAlbumTracksResponse: Decodable {
    let items: [SpotifyAlbumTrack]
}

struct SpotifyAlbumTrack: Decodable {
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

struct SpotifyTrack: Decodable {
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

struct SpotifyAlbum: Decodable {
    let name: String
    let images: [SpotifyImage]
    let releaseDate: String?

    enum CodingKeys: String, CodingKey {
        case name
        case images
        case releaseDate = "release_date"
    }
}

struct SpotifyArtist: Decodable {
    let id: String
    let name: String
}

struct SpotifyImage: Decodable {
    let url: String
}

struct CatalogSearchResponse: Decodable {
    let results: [CatalogTrack]
}

struct CatalogTrack: Decodable {
    let trackId: Int?
    let artistId: Int?
    let artistName: String
    let trackName: String
    let collectionName: String?
    let artworkUrl100: String?
    let previewUrl: String?
    let wrapperType: String?
    let kind: String?
    let releaseDate: String?
}

struct PublicAlbumReference {
    let albumID: String
    let releaseDate: String?
}

struct StoredToken: Codable {
    let accessToken: String
    let refreshToken: String?
    let expirationDate: Date
}

final class PresentationAnchorProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes
            .flatMap(\.windows)
            .first(where: \.isKeyWindow) ?? ASPresentationAnchor()
    }
}
