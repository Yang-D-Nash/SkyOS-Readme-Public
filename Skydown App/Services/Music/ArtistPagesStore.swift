import Foundation
import Combine
import FirebaseFirestore

@MainActor
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
            "heroVideoURL": page.heroVideoURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "instagramURL": page.instagramURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "spotifyURL": page.spotifyURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "youtubeURL": page.youtubeURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? NSNull(),
            "studioPriceList": page.studioPriceList
                .map { item in
                    [
                        "title": item.title.trimmingCharacters(in: .whitespacesAndNewlines),
                        "detail": item.detail.trimmingCharacters(in: .whitespacesAndNewlines),
                        "price": item.price.trimmingCharacters(in: .whitespacesAndNewlines)
                    ]
                }
                .filter { entry in
                    (entry["title"]?.isEmpty == false)
                    && (entry["detail"]?.isEmpty == false)
                    && (entry["price"]?.isEmpty == false)
                },
            "editorUids": Array(Set(page.editorUids.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty })).sorted(),
            "createdAt": Timestamp(date: page.createdAt),
            "updatedAt": FieldValue.serverTimestamp()
        ]

        let documentID = artistPageDocumentID(brand: page.brand, artistName: page.artistName)
        try await firestore.collection(collectionName).document(documentID).setData(payload, merge: true)
    }

    private static func firstURLString(_ data: [String: Any], _ keys: String...) -> String? {
        for key in keys {
            if let s = (data[key] as? String)?.trimmedNilIfEmpty {
                return s
            }
        }
        return nil
    }

    private static func firstURLStringInNestedMaps(
        _ data: [String: Any],
        mapNames: [String],
        keys: [String]
    ) -> String? {
        for mapName in mapNames {
            guard let m = data[mapName] as? [String: Any] else { continue }
            for k in keys {
                if let s = (m[k] as? String)?.trimmedNilIfEmpty {
                    return s
                }
            }
        }
        return nil
    }

    private static func mapPage(document: QueryDocumentSnapshot) -> ArtistPage? {
        let data = document.data()
        let brandKey = (data["brand"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() ?? ""
        guard
            !brandKey.isEmpty,
            let brand = ArtistPageBrand(rawValue: brandKey),
            let fieldArtistName = data["artistName"] as? String
        else {
            return nil
        }

        let trimmed = fieldArtistName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        // Firestore `artistName` can be copy-paste wrong; document ID (same as on save) is source of truth.
        let artistName: String
        if brand == .nicma {
            switch document.documentID {
            case "nicma-nicma-music": artistName = "NICMA MUSIC"
            case "nicma-nicma-studio": artistName = "NICMA STUDIO"
            default: artistName = trimmed
            }
        } else {
            artistName = trimmed
        }

        let createdAt = (data["createdAt"] as? Timestamp)?.dateValue() ?? .now
        let updatedAt = (data["updatedAt"] as? Timestamp)?.dateValue() ?? createdAt
        let editorUids = (data["editorUids"] as? [String] ?? [])
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        let studioPriceList = (data["studioPriceList"] as? [[String: Any]] ?? [])
            .compactMap { entry -> StudioPriceItem? in
                guard
                    let title = (entry["title"] as? String)?.trimmedNilIfEmpty,
                    let detail = (entry["detail"] as? String)?.trimmedNilIfEmpty,
                    let price = (entry["price"] as? String)?.trimmedNilIfEmpty
                else {
                    return nil
                }
                return StudioPriceItem(title: title, detail: detail, price: price)
            }

        return ArtistPage(
            id: artistPageDocumentID(brand: brand, artistName: artistName),
            brand: brand,
            artistName: artistName,
            tagline: (data["tagline"] as? String)?.trimmedNilIfEmpty,
            bio: (data["bio"] as? String)?.trimmedNilIfEmpty,
            profileImageURL: (data["profileImageURL"] as? String)?.trimmedNilIfEmpty,
            heroImageURL: (data["heroImageURL"] as? String)?.trimmedNilIfEmpty,
            heroVideoURL: (data["heroVideoURL"] as? String)?.trimmedNilIfEmpty,
            instagramURL: firstURLString(
                data,
                "instagramURL", "instagramUrl", "instagram", "instagramURLString"
            ) ?? firstURLStringInNestedMaps(
                data,
                mapNames: ["social", "links", "link", "instagram"],
                keys: ["instagram", "instagramURL", "instagramUrl", "url", "link", "href", "u"]
            ),
            spotifyURL: firstURLString(data, "spotifyURL", "spotifyUrl", "spotify", "spotifyUrlString")
                ?? firstURLStringInNestedMaps(
                    data,
                    mapNames: ["social", "links", "link", "spotify"],
                    keys: ["spotify", "spotifyURL", "spotifyUrl", "url", "link", "href", "u"]
                ),
            youtubeURL: firstURLString(
                data,
                "youtubeURL", "youtubeUrl", "youtube", "youtubeURLString", "youTubeURL", "youTube"
            ) ?? firstURLStringInNestedMaps(
                data,
                mapNames: ["social", "links", "link", "youtube"],
                keys: ["youtube", "youtubeURL", "youtubeUrl", "yt", "youTube", "url", "link", "href", "u"]
            ),
            studioPriceList: studioPriceList,
            editorUids: editorUids,
            createdAt: createdAt,
            updatedAt: updatedAt,
            isPlaceholder: false
        )
    }
}

final class UITestArtistPagesService: ArtistPagesServicing {
    private var pages: [ArtistPage] = []
    private var observers: [UUID: @MainActor (Result<[ArtistPage], Error>) -> Void] = [:]

    func observePages(_ onChange: @escaping @MainActor (Result<[ArtistPage], Error>) -> Void) -> () -> Void {
        let id = UUID()
        observers[id] = onChange
        onChange(.success(pages))
        return { [weak self] in
            Task { @MainActor in
                self?.observers.removeValue(forKey: id)
            }
        }
    }

    func save(page: ArtistPage) async throws {
        if let index = pages.firstIndex(where: { $0.slug == page.slug && $0.brand == page.brand }) {
            pages[index] = page
        } else {
            pages.append(page)
        }
        notifyObservers()
    }

    private func notifyObservers() {
        for observer in observers.values {
            observer(.success(pages))
        }
    }
}

@MainActor
final class ArtistPagesStore: ObservableObject {
    static let shared = ArtistPagesStore()

    @Published private(set) var pages: [ArtistPage] = []
    @Published private(set) var lastErrorMessage: String?

    private let service: ArtistPagesServicing
    private var stopObserving: (() -> Void)?

    init(service: ArtistPagesServicing? = nil) {
        self.service = service ?? (UITestRuntime.usesIsolatedAuthService ? UITestArtistPagesService() : FirebaseArtistPagesService())
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
        return ArtistPage.draft(brand: brand, artistName: artistName)
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
        var pagesByID: [String: ArtistPage] = [:]

        for page in remotePages {
            if let existing = pagesByID[page.slug] {
                pagesByID[page.slug] = resolvedPreferredPage(between: existing, and: page)
            } else {
                pagesByID[page.slug] = page
            }
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
}

private extension String {
    var trimmedNilIfEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
