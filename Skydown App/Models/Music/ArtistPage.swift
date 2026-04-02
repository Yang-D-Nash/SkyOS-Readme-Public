import Foundation

enum ArtistPageBrand: String, Codable, CaseIterable {
    case zweizwei
    case skydown
    case nicma

    var displayTitle: String {
        switch self {
        case .zweizwei:
            return "ZweiZwei"
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
    var instagramURL: String?
    var spotifyURL: String?
    var youtubeURL: String?
    var editorUids: [String]
    var createdAt: Date
    var updatedAt: Date
    var isPlaceholder: Bool = false

    var slug: String {
        id
    }

    var hasCustomPresentation: Bool {
        [tagline, bio, profileImageURL, heroImageURL, instagramURL, spotifyURL, youtubeURL]
            .contains { ($0?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false) }
    }
}

struct ArtistPageSeed: Identifiable, Equatable {
    let brand: ArtistPageBrand
    let artistName: String
    let tagline: String?
    let bio: String?
    let instagramURL: String?
    let spotifyURL: String?
    let youtubeURL: String?

    init(
        brand: ArtistPageBrand,
        artistName: String,
        tagline: String? = nil,
        bio: String? = nil,
        instagramURL: String? = nil,
        spotifyURL: String? = nil,
        youtubeURL: String? = nil
    ) {
        self.brand = brand
        self.artistName = artistName
        self.tagline = tagline
        self.bio = bio
        self.instagramURL = instagramURL
        self.spotifyURL = spotifyURL
        self.youtubeURL = youtubeURL
    }

    var id: String {
        slug
    }

    var slug: String {
        artistPageSlug(from: artistName)
    }
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

extension ArtistPage {
    static func placeholder(
        seed: ArtistPageSeed,
        timestamp: Date = .now
    ) -> ArtistPage {
        ArtistPage(
            id: seed.slug,
            brand: seed.brand,
            artistName: seed.artistName,
            tagline: seed.tagline,
            bio: seed.bio,
            profileImageURL: nil,
            heroImageURL: nil,
            instagramURL: seed.instagramURL,
            spotifyURL: seed.spotifyURL,
            youtubeURL: seed.youtubeURL,
            editorUids: [],
            createdAt: timestamp,
            updatedAt: timestamp,
            isPlaceholder: true
        )
    }
}
