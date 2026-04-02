import Foundation
import Combine
import FirebaseFirestore

protocol ArtistPagesServicing {
    func observePages(_ onChange: @escaping @MainActor (Result<[ArtistPage], Error>) -> Void) -> () -> Void
    func save(page: ArtistPage) async throws
}

final class FirebaseArtistPagesService: ArtistPagesServicing {
    private let firestore: Firestore
    private let collectionName = "artistPages"

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func observePages(_ onChange: @escaping @MainActor (Result<[ArtistPage], Error>) -> Void) -> () -> Void {
        let listener = firestore.collection(collectionName).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                let pages = (snapshot?.documents ?? [])
                    .compactMap(Self.mapPage(document:))
                onChange(.success(pages))
            }
        }

        return {
            listener.remove()
        }
    }

    func save(page: ArtistPage) async throws {
        let payload: [String: Any] = [
            "slug": page.slug,
            "brand": page.brand.rawValue,
            "artistName": page.artistName.trimmingCharacters(in: .whitespacesAndNewlines),
            "tagline": page.tagline?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "bio": page.bio?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "profileImageURL": page.profileImageURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "heroImageURL": page.heroImageURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "instagramURL": page.instagramURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "spotifyURL": page.spotifyURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "youtubeURL": page.youtubeURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "editorUids": Array(Set(page.editorUids.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty })).sorted(),
            "createdAt": Timestamp(date: page.createdAt),
            "updatedAt": FieldValue.serverTimestamp()
        ]

        try await firestore.collection(collectionName).document(page.slug).setData(payload, merge: true)
    }

    private static func mapPage(document: QueryDocumentSnapshot) -> ArtistPage? {
        let data = document.data()
        guard
            let brandRaw = data["brand"] as? String,
            let brand = ArtistPageBrand(rawValue: brandRaw),
            let artistName = data["artistName"] as? String
        else {
            return nil
        }

        let createdAt = (data["createdAt"] as? Timestamp)?.dateValue() ?? .now
        let updatedAt = (data["updatedAt"] as? Timestamp)?.dateValue() ?? createdAt
        let editorUids = (data["editorUids"] as? [String] ?? [])
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        return ArtistPage(
            id: document.documentID,
            brand: brand,
            artistName: artistName,
            tagline: (data["tagline"] as? String)?.trimmedNilIfEmpty,
            bio: (data["bio"] as? String)?.trimmedNilIfEmpty,
            profileImageURL: (data["profileImageURL"] as? String)?.trimmedNilIfEmpty,
            heroImageURL: (data["heroImageURL"] as? String)?.trimmedNilIfEmpty,
            instagramURL: (data["instagramURL"] as? String)?.trimmedNilIfEmpty,
            spotifyURL: (data["spotifyURL"] as? String)?.trimmedNilIfEmpty,
            youtubeURL: (data["youtubeURL"] as? String)?.trimmedNilIfEmpty,
            editorUids: editorUids,
            createdAt: createdAt,
            updatedAt: updatedAt,
            isPlaceholder: false
        )
    }
}

@MainActor
final class ArtistPagesStore: ObservableObject {
    static let shared = ArtistPagesStore()

    @Published private(set) var pages: [ArtistPage] = ArtistPagesStore.seedPages().map { ArtistPage.placeholder(seed: $0) }
    @Published private(set) var lastErrorMessage: String?

    private let service: ArtistPagesServicing
    private var stopObserving: (() -> Void)?

    init(service: ArtistPagesServicing = FirebaseArtistPagesService()) {
        self.service = service
        startObserving()
    }

    func pages(for brand: ArtistPageBrand) -> [ArtistPage] {
        pages.filter { $0.brand == brand }
            .sorted { $0.artistName.localizedCaseInsensitiveCompare($1.artistName) == .orderedAscending }
    }

    func page(for brand: ArtistPageBrand, artistName: String) -> ArtistPage {
        let slug = artistPageSlug(from: artistName)
        if let page = pages.first(where: { $0.brand == brand && $0.slug == slug }) {
            return page
        }
        return ArtistPage.placeholder(seed: ArtistPageSeed(brand: brand, artistName: artistName))
    }

    func canEdit(_ page: ArtistPage, user: User?) -> Bool {
        guard let user else { return false }
        if user.isPlatformOwner {
            return true
        }

        guard let userId = user.id, !userId.isEmpty else { return false }
        return page.editorUids.contains(userId)
    }

    func save(_ page: ArtistPage) async throws {
        try await service.save(page: page)
    }

    private func startObserving() {
        stopObserving?()
        stopObserving = service.observePages { [weak self] result in
            guard let store = self else { return }

            switch result {
            case .success(let remotePages):
                let merged = store.merge(remotePages: remotePages)
                store.pages = merged
                store.lastErrorMessage = nil
            case .failure(let error):
                store.lastErrorMessage = error.localizedDescription
            }
        }
    }

    private func merge(remotePages: [ArtistPage]) -> [ArtistPage] {
        let seeded = Dictionary(uniqueKeysWithValues: ArtistPagesStore.seedPages().map { ($0.slug, $0) })
        var pagesBySlug = Dictionary(uniqueKeysWithValues: remotePages.map { ($0.slug, $0) })

        for (_, seed) in seeded where pagesBySlug[seed.slug] == nil {
            pagesBySlug[seed.slug] = .placeholder(seed: seed)
        }

        return pagesBySlug.values.sorted { lhs, rhs in
            if lhs.brand != rhs.brand {
                return lhs.brand.rawValue < rhs.brand.rawValue
            }
            return lhs.artistName.localizedCaseInsensitiveCompare(rhs.artistName) == .orderedAscending
        }
    }

    private static func seedPages() -> [ArtistPageSeed] {
        var seededPages = [
            ArtistPageBrand.zweizwei: MusicExperienceBrand.zweizwei.artists,
            ArtistPageBrand.skydown: MusicExperienceBrand.skydown.artists
        ]
        .flatMap { brand, artists in
            artists.map { ArtistPageSeed(brand: brand, artistName: $0) }
        }

        seededPages.append(ArtistPageSeed(brand: .nicma, artistName: "NICMA MUSIC"))
        return seededPages
    }
}

private extension String {
    var trimmedNilIfEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
