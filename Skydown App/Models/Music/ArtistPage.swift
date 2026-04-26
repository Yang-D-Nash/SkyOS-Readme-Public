import Foundation

enum ArtistPageBrand: String, Codable, CaseIterable {
    case zweizwei
    case skydown
    case nicma

    var displayTitle: String {
        switch self {
        case .zweizwei:
            return "22"
        case .skydown:
            return "Skydown"
        case .nicma:
            return "Nicma"
        }
    }
}

struct ArtistPage: Identifiable, Equatable {
    let id: String
    let brand: ArtistPageBrand
    var artistName: String
    var tagline: String?
    var bio: String?
    var profileImageURL: String?
    var heroImageURL: String?
    var heroVideoURL: String?
    var instagramURL: String?
    var spotifyURL: String?
    var youtubeURL: String?
    var studioPriceList: [StudioPriceItem]
    var editorUids: [String]
    var createdAt: Date
    var updatedAt: Date
    var isPlaceholder: Bool = false

    var slug: String {
        id
    }

    var hasCustomPresentation: Bool {
        [tagline, bio, profileImageURL, heroImageURL, heroVideoURL, instagramURL, spotifyURL, youtubeURL]
            .contains { ($0?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false) }
        || !studioPriceList.isEmpty
    }
}

struct StudioPriceItem: Equatable {
    var title: String
    var detail: String
    var price: String
}

func artistPageSlug(from artistName: String) -> String {
    let normalized = artistName
        .trimmingCharacters(in: .whitespacesAndNewlines)
        .lowercased()
        .replacingOccurrences(
            of: "[^a-z0-9]+",
            with: "-",
            options: .regularExpression
        )
        .trimmingCharacters(in: CharacterSet(charactersIn: "-"))

    return normalized.isEmpty ? "artist" : normalized
}

func artistPageDocumentID(brand: ArtistPageBrand, artistName: String) -> String {
    "\(brand.rawValue)-\(artistPageSlug(from: artistName))"
}

private let nicmaPublicDefaultInstagramURL = "https://www.instagram.com/nicma.music/"
private let nicmaPublicDefaultSpotifyArtistURL = "https://open.spotify.com/artist/0OoRIo7pJjtLgg3qyf1oDS"

extension ArtistPage {
    /// Wenn Firestore leer: oeffentliche Kanaele (wie Android / Spaotify-Mapping), damit Connect nicht leer bleibt.
    func withNicmaMusicPublicLinkDefaults() -> ArtistPage {
        var m = self
        if (m.instagramURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "").isEmpty {
            m.instagramURL = nicmaPublicDefaultInstagramURL
        }
        if (m.spotifyURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "").isEmpty {
            m.spotifyURL = nicmaPublicDefaultSpotifyArtistURL
        }
        return m
    }

    /// Wenn im Music-Doc keine Kanaele stehen, z. B. STUDIO-Doc als Fallback (ein Admin-Set, zwei Tabs).
    func mergedNicmaConnectFromStudio(_ studio: ArtistPage) -> ArtistPage {
        var m = self
        if (m.instagramURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "").isEmpty {
            m.instagramURL = studio.instagramURL
        }
        if (m.spotifyURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "").isEmpty {
            m.spotifyURL = studio.spotifyURL
        }
        if (m.youtubeURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "").isEmpty {
            m.youtubeURL = studio.youtubeURL
        }
        return m
    }
}

extension ArtistPage {
    static func draft(
        brand: ArtistPageBrand,
        artistName: String,
        timestamp: Date = .now
    ) -> ArtistPage {
        ArtistPage(
            id: artistPageDocumentID(brand: brand, artistName: artistName),
            brand: brand,
            artistName: artistName,
            tagline: nil,
            bio: nil,
            profileImageURL: nil,
            heroImageURL: nil,
            heroVideoURL: nil,
            instagramURL: nil,
            spotifyURL: nil,
            youtubeURL: nil,
            studioPriceList: [],
            editorUids: [],
            createdAt: timestamp,
            updatedAt: timestamp,
            isPlaceholder: true
        )
    }
}
