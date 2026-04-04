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
            "slug": artistPageSlug(from: page.artistName),
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

        let documentID = artistPageDocumentID(brand: page.brand, artistName: page.artistName)
        try await firestore.collection(collectionName).document(documentID).setData(payload, merge: true)
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
            id: artistPageDocumentID(brand: brand, artistName: artistName),
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
        let slug = artistPageDocumentID(brand: brand, artistName: artistName)
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
        let seeded = Dictionary(uniqueKeysWithValues: ArtistPagesStore.seedPages().map { ($0.documentID, $0) })
        var pagesByID: [String: ArtistPage] = [:]

        for page in remotePages {
            if let existing = pagesByID[page.slug] {
                pagesByID[page.slug] = resolvedPreferredPage(between: existing, and: page)
            } else {
                pagesByID[page.slug] = page
            }
        }

        for (documentID, seed) in seeded where pagesByID[documentID] == nil {
            pagesByID[documentID] = .placeholder(seed: seed)
        }

        return pagesByID.values.sorted { lhs, rhs in
            if lhs.brand != rhs.brand {
                return lhs.brand.rawValue < rhs.brand.rawValue
            }
            return lhs.artistName.localizedCaseInsensitiveCompare(rhs.artistName) == .orderedAscending
        }
    }

    private func resolvedPreferredPage(between lhs: ArtistPage, and rhs: ArtistPage) -> ArtistPage {
        if lhs.updatedAt != rhs.updatedAt {
            return lhs.updatedAt > rhs.updatedAt ? lhs : rhs
        }
        if lhs.isPlaceholder != rhs.isPlaceholder {
            return lhs.isPlaceholder ? rhs : lhs
        }
        return lhs
    }

    private static func seedPages() -> [ArtistPageSeed] {
        [
            ArtistPageSeed(
                brand: .zweizwei,
                artistName: "JANNO",
                tagline: "Melodic street energy und klare Hooks.",
                bio: "JANNO bringt Druck, Gefuehl und direkte Hook-Momente zusammen. Auf dieser Seite laufen Releases, Top Songs und die wichtigsten Links direkt fuer neue Hoerer zusammen.",
                instagramURL: "https://www.instagram.com/janno_official_/",
                spotifyURL: "https://open.spotify.com/artist/7hpiHzP9aLLb5liDLxtwhM",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .zweizwei,
                artistName: "Yang D. Nash",
                tagline: "Zwischen Skydown, Vision und Release-Fokus.",
                bio: "Yang D. Nash verbindet Artist-Energie mit Creative Direction. Songs, Visuals und der ganze Skydown-Kosmos laufen hier zusammen.",
                instagramURL: "https://www.instagram.com/y.d.nash/",
                spotifyURL: "https://open.spotify.com/artist/63Sh0kQAWW3ZWn2aKDksbo",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .zweizwei,
                artistName: "ThaDude",
                tagline: "Roh, direkt und mit klarer Attitude.",
                bio: "ThaDude steht fuer druckvolle Tracks, direkte Delivery und den schnellen Weg von der Hook in den Kopf. Hier finden User Songs, Profil und Links an einem Ort.",
                instagramURL: "https://www.instagram.com/thadude_offizielle/",
                spotifyURL: "https://open.spotify.com/artist/0Jmb7DXFkKxxRjqD70vi0e",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .zweizwei,
                artistName: "MAVE",
                tagline: "Melodien, Atmosphaere und naechtlicher Zug.",
                bio: "MAVE bringt melodische Momente, dunklere Stimmungen und eine klare Release-Aesthetik zusammen. Die Artist-Page ist der direkte Einstieg fuer neue Hoerer.",
                instagramURL: "https://www.instagram.com/mave__official/",
                spotifyURL: "https://open.spotify.com/artist/0GXymtRaIk2ngbXSkcHtsp",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .zweizwei,
                artistName: "TANGAJOE007",
                tagline: "Raw voice, klare Kante, direkter Vibe.",
                bio: "TANGAJOE007 steht fuer direkte Energie und eine praesente Stimme. Hier landen Songs, Profil und Socials gebuendelt in einem starken Artist-Entrance.",
                instagramURL: "https://www.instagram.com/tangajoe007/",
                spotifyURL: "https://open.spotify.com/artist/0OA5dgpVdwzI8K82m8FPxN",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .skydown,
                artistName: "Yang D. Nash",
                tagline: "Skydown founder energy trifft Release-Fokus.",
                bio: "Yang D. Nash verbindet Music, Creative Direction und Storytelling. Die Seite gibt neuen Usern direkt einen sauberen Einstieg in Songs, Releases und Kanaele.",
                instagramURL: "https://www.instagram.com/y.d.nash/",
                spotifyURL: "https://open.spotify.com/artist/63Sh0kQAWW3ZWn2aKDksbo",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .skydown,
                artistName: "ThaDude",
                tagline: "Direkter Rap mit Kante und Haltung.",
                bio: "ThaDude liefert rohe Energie und markante Delivery. Diese Seite fuehrt direkt zu Songs, Profil und den wichtigsten Plattformen.",
                instagramURL: "https://www.instagram.com/thadude_offizielle/",
                spotifyURL: "https://open.spotify.com/artist/0Jmb7DXFkKxxRjqD70vi0e",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .skydown,
                artistName: "MAVE",
                tagline: "Melodic mood und klare Release-Atmosphaere.",
                bio: "MAVE bringt Stimmung, Timing und melodische Flaechen zusammen. User bekommen hier den schnellen Einstieg in Songs und Socials.",
                instagramURL: "https://www.instagram.com/mave__official/",
                spotifyURL: "https://open.spotify.com/artist/0GXymtRaIk2ngbXSkcHtsp",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .skydown,
                artistName: "JANNO",
                tagline: "Melodic street energy und klare Hooks.",
                bio: "JANNO verbindet Druck, Gefuehl und direkte Hook-Momente. Diese Seite holt neue Hoerer direkt in die Releases und Songs rein.",
                instagramURL: "https://www.instagram.com/janno_official_/",
                spotifyURL: "https://open.spotify.com/artist/7hpiHzP9aLLb5liDLxtwhM",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .skydown,
                artistName: "TANGAJOE007",
                tagline: "Roh, direkt und voller Praesenz.",
                bio: "TANGAJOE007 bringt Stimme, Kante und Attitude in jeden Track. Songs und Links sind hier bewusst direkt erreichbar.",
                instagramURL: "https://www.instagram.com/tangajoe007/",
                spotifyURL: "https://open.spotify.com/artist/0OA5dgpVdwzI8K82m8FPxN",
                youtubeURL: nil
            ),
            ArtistPageSeed(
                brand: .nicma,
                artistName: "NICMA MUSIC",
                tagline: "Studio, Production und Sound-Handwerk.",
                bio: "NICMA MUSIC ist die Producer- und Studio-Seite fuer Recording, Mix, Master und Sound-Entwicklung. Hier finden User Sound, Referenzen und direkte Kontaktwege.",
                instagramURL: "https://www.instagram.com/nicma.music/",
                spotifyURL: "https://open.spotify.com/artist/0OoRIo7pJjtLgg3qyf1oDS",
                youtubeURL: nil
            )
        ]
    }
}

private extension String {
    var trimmedNilIfEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
