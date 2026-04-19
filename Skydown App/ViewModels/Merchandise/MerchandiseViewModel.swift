//
//  MerchandiseViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import Foundation
import FirebaseFirestore

@MainActor
final class MerchandiseViewModel: ObservableObject {
    @Published var merchandiseItems: [MerchandiseItem] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isStoreOpen = true
    @Published var isUpdatingStoreState = false
    @Published var isSyncingCatalog = false

    // Toast
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let merchandiseService: MerchandiseServicing
    private let merchStoreStatusService: MerchStoreStatusServicing
    private let shopifySyncService: ShopifyMerchSyncServicing
    private let publicShopifyCatalogService: PublicShopifyCatalogServicing
    private let authManager: AuthManager
    private var stopObservingItems: (() -> Void)?
    private var stopObservingStoreStatus: (() -> Void)?
    private var allItems: [MerchandiseItem] = []
    private var hasAttemptedAutomaticShopifySync = false

    private var canManageMerchandise: Bool {
        authManager.userSession?.isPlatformOwner == true
    }

    init(
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService(),
        merchStoreStatusService: MerchStoreStatusServicing = FirestoreMerchStoreStatusService(),
        shopifySyncService: ShopifyMerchSyncServicing = FirebaseFunctionsShopifyMerchSyncService(),
        publicShopifyCatalogService: PublicShopifyCatalogServicing = PublicShopifyCatalogService(),
        authManager: AuthManager
    ) {
        self.merchandiseService = merchandiseService
        self.merchStoreStatusService = merchStoreStatusService
        self.shopifySyncService = shopifySyncService
        self.publicShopifyCatalogService = publicShopifyCatalogService
        self.authManager = authManager

        Task { [weak self] in
            for await _ in authManager.$userSession.values {
                guard let self else { return }
                self.merchandiseItems = self.filterVisibleItems(self.allItems)
            }
        }
    }

    func fetchData() {
        isLoading = true
        errorMessage = nil
        if canManageMerchandise && allItems.isEmpty {
            hasAttemptedAutomaticShopifySync = false
        }

        stopObservingItems?()
        observeStoreStatusIfNeeded()

        stopObservingItems = merchandiseService.observeItems { [weak self] result in
            guard let self else { return }
            self.isLoading = false

            switch result {
            case .success(let items):
                self.allItems = items
                self.merchandiseItems = self.filterVisibleItems(items)
                if self.shouldLoadShopifyFallback(for: items) {
                    self.handleEmptyCatalogSnapshot()
                } else {
                    self.hasAttemptedAutomaticShopifySync = false
                }
            case .failure(let error):
                Task { @MainActor in
                    let loadedFallback = await self.loadPublicCatalogFallback(showToast: false)
                    if loadedFallback {
                        return
                    }

                    print("Dev Fehler fetchData:", error.localizedDescription)
                    self.showUserToast("Fehler beim Laden der Artikel: \(error.localizedDescription)", style: .error)
                    self.merchandiseItems = []
                    self.allItems = []
                }
            }
        }
    }

    func toggleStoreOpen() async {
        guard canManageMerchandise else {
            showUserToast("Nur der Owner darf den Merch Store schalten.", style: .error)
            return
        }

        isUpdatingStoreState = true
        defer { isUpdatingStoreState = false }

        do {
            let nextState = !isStoreOpen
            try await merchStoreStatusService.updateStoreOpen(nextState)
            showUserToast(nextState ? "Merch Store geoeffnet." : "Merch Store geschlossen.", style: .success)
        } catch {
            showUserToast("Store-Status konnte nicht aktualisiert werden: \(error.localizedDescription)", style: .error)
        }
    }

    func syncShopifyCatalog(automatic: Bool = false) async {
        guard canManageMerchandise else {
            showUserToast("Nur der Owner darf den Shopify-Sync starten.", style: .error)
            return
        }

        isSyncingCatalog = true
        defer { isSyncingCatalog = false }

        do {
            let message = try await shopifySyncService.triggerSync()
            showUserToast(
                automatic ? "Shopify-Katalog wurde neu geladen." : message,
                style: .success
            )
            fetchData()
        } catch {
            let loadedFallback = await loadPublicCatalogFallback(showToast: automatic == false)
            if !loadedFallback {
                showUserToast("Shopify-Sync fehlgeschlagen: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private func observeStoreStatusIfNeeded() {
        guard stopObservingStoreStatus == nil else { return }

        stopObservingStoreStatus = merchStoreStatusService.observeStatus { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let status):
                self.isStoreOpen = status.isOpen
            case .failure(let error):
                self.showUserToast("Store-Status konnte nicht geladen werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func handleEmptyCatalogSnapshot() {
        Task {
            let loadedFallback = await loadPublicCatalogFallback(showToast: false)
            guard canManageMerchandise else { return }
            guard !isSyncingCatalog else { return }
            guard !hasAttemptedAutomaticShopifySync else { return }
            if loadedFallback {
                hasAttemptedAutomaticShopifySync = true
                return
            }

            hasAttemptedAutomaticShopifySync = true
            await syncShopifyCatalog(automatic: true)
        }
    }

    private func shouldLoadShopifyFallback(for items: [MerchandiseItem]) -> Bool {
        if items.isEmpty {
            return true
        }

        if canManageMerchandise {
            return merchandiseItems.isEmpty
        }

        return !hasVisibleShopifyItems(items)
    }

    private func hasVisibleShopifyItems(_ items: [MerchandiseItem]) -> Bool {
        items.contains { item in
            item.isVisibleInApp &&
                item.source == "shopify" &&
                item.shopifySyncActive &&
                item.shopifyProductId?.isEmpty == false
        }
    }

    private func loadPublicCatalogFallback(showToast: Bool) async -> Bool {
        do {
            let items = try await publicShopifyCatalogService.fetchCatalog()
            guard !items.isEmpty else { return false }

            allItems = items
            merchandiseItems = filterVisibleItems(items)
            if showToast {
                showUserToast("Shopify-Katalog direkt aus dem Store geladen.", style: .success)
            }
            return true
        } catch {
            return false
        }
    }

    private func filterVisibleItems(_ items: [MerchandiseItem]) -> [MerchandiseItem] {
        let activeItems = items.filter { item in
            item.source != "shopify" || item.shopifySyncActive
        }
        let visibleItems = activeItems.filter { canManageMerchandise || $0.isVisibleInApp }
        let hasVisibleShopifyItems = visibleItems.contains { item in
            item.source == "shopify" && item.shopifySyncActive && item.shopifyProductId?.isEmpty == false
        }

        let prioritizedItems: [MerchandiseItem]
        if canManageMerchandise {
            prioritizedItems = visibleItems
        } else if hasVisibleShopifyItems {
            prioritizedItems = visibleItems.filter { item in
                item.source == "shopify" && item.shopifySyncActive && item.shopifyProductId?.isEmpty == false
            }
        } else {
            prioritizedItems = visibleItems
        }

        return prioritizedItems.sorted {
            if $0.featured != $1.featured {
                return $0.featured && !$1.featured
            }
            if $0.source != $1.source {
                return $0.source == "shopify"
            }
            if $0.sortOrder != $1.sortOrder {
                return $0.sortOrder < $1.sortOrder
            }
            return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
        }
    }

    deinit {
        stopObservingItems?()
        stopObservingStoreStatus?()
    }
}

struct FeaturedHomeVideo: Identifiable {
    let id: String
    let title: String
    let projectName: String
    let notes: String
    let downloadURL: String
    let externalURL: String
    let embedURL: String
    let sourceProvider: String

    var provider: ExternalMediaProvider {
        ExternalMediaProvider(rawValueOrDefault: sourceProvider)
    }

    var usesEmbeddedPreview: Bool {
        provider != .firebaseStorage && !embedURL.isEmpty && downloadURL.isEmpty
    }

    var supportsInlinePlayback: Bool {
        usesEmbeddedPreview || !downloadURL.isEmpty
    }

    var opensOriginalInApp: Bool {
        !openURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var originalDestinationDescription: String {
        if openURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Kein Original-Link verfuegbar."
        }

        if !downloadURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Dieser Clip startet direkt in der In-App-Ansicht."
        }

        return "Dieser Link startet in einer In-App-Webansicht mit Zurueck und Schliessen."
    }

    var openURLString: String {
        externalURL.isEmpty ? downloadURL : externalURL
    }
}

struct FeaturedHomeBeat {
    let id: String
    let title: String
    let artistName: String
    let notes: String
    let downloadURL: String
    let externalURL: String
    let sourceProvider: String
    let isPlayable: Bool

    var openURLString: String {
        externalURL.isEmpty ? downloadURL : externalURL
    }

    var provider: ExternalMediaProvider {
        ExternalMediaProvider(rawValueOrDefault: sourceProvider)
    }
}

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var featuredTrack: Track?
    @Published var featuredBeat: FeaturedHomeBeat?
    @Published var featuredVideo: FeaturedHomeVideo?
    @Published var homeTrackMessage: String?
    @Published var homeBeatMessage: String?
    @Published var homeVideoMessage: String?

    private let musicService: MusicServicing
    private let firestore = Firestore.firestore()
    private let featuredArtists = ["JANNO", "Yang D. Nash", "ThaDude", "MAVE", "TANGAJOE007"]

    init(musicService: MusicServicing = SpotifyMusicService()) {
        self.musicService = musicService
    }

    func refresh() {
        Task {
            await withTaskGroup(of: HomeRefreshResult.self) { group in
                group.addTask { .track(await self.loadLatestTrack()) }
                group.addTask { .beat(await self.loadLatestBeat()) }
                group.addTask { .video(await self.loadLatestVideo()) }

                for await result in group {
                    switch result {
                    case .track(let track):
                        featuredTrack = track
                        homeTrackMessage = track == nil
                            ? "Sobald ein neuer Release verfuegbar ist, taucht er hier direkt auf."
                            : nil
                    case .beat(let beat):
                        featuredBeat = beat
                        homeBeatMessage = beat == nil
                            ? "Sobald ein freigegebener Beat live ist, taucht er hier direkt auf."
                            : nil
                    case .video(let video):
                        featuredVideo = video
                        homeVideoMessage = video == nil
                            ? "Sobald ein oeffentliches Video live ist, taucht hier dein Highlight auf."
                            : nil
                    }
                }
            }
        }
    }

    private func loadLatestTrack() async -> Track? {
        let trackGroups = await withTaskGroup(of: [Track].self) { group in
            for artist in featuredArtists {
                group.addTask {
                    (try? await self.musicService.fetchTracks(for: artist)) ?? []
                }
            }

            var collectedTracks: [[Track]] = []
            for await tracks in group {
                collectedTracks.append(tracks)
            }
            return collectedTracks
        }

        let tracks = trackGroups.flatMap { $0 }
        guard !tracks.isEmpty else { return nil }

        var bestTrack = tracks.first
        for track in tracks.dropFirst() {
            guard let currentBest = bestTrack else {
                bestTrack = track
                continue
            }

            if compareTracksForHomePriority(track, currentBest) {
                bestTrack = track
            }
        }

        return bestTrack
    }

    private func compareTracksForHomePriority(_ lhs: Track, _ rhs: Track) -> Bool {
        let lhsDate = parsedTrackReleaseDate(lhs.releaseDate)
        let rhsDate = parsedTrackReleaseDate(rhs.releaseDate)

        if lhsDate != rhsDate {
            return (lhsDate ?? .distantPast) > (rhsDate ?? .distantPast)
        }

        let lhsHasPlayback = !(lhs.previewUrl ?? "").isEmpty || !(lhs.externalURL ?? "").isEmpty
        let rhsHasPlayback = !(rhs.previewUrl ?? "").isEmpty || !(rhs.externalURL ?? "").isEmpty
        if lhsHasPlayback != rhsHasPlayback {
            return lhsHasPlayback && !rhsHasPlayback
        }

        return lhs.trackName.localizedCaseInsensitiveCompare(rhs.trackName) == .orderedAscending
    }

    private func parsedTrackReleaseDate(_ value: String?) -> Date? {
        guard let rawValue = value?.trimmingCharacters(in: .whitespacesAndNewlines), !rawValue.isEmpty else {
            return nil
        }

        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = isoFormatter.date(from: rawValue) {
            return date
        }

        let fallbackISOFormatter = ISO8601DateFormatter()
        if let date = fallbackISOFormatter.date(from: rawValue) {
            return date
        }

        let formatters: [DateFormatter] = [
            {
                let formatter = DateFormatter()
                formatter.locale = Locale(identifier: "en_US_POSIX")
                formatter.timeZone = TimeZone(secondsFromGMT: 0)
                formatter.dateFormat = "yyyy-MM-dd"
                return formatter
            }(),
            {
                let formatter = DateFormatter()
                formatter.locale = Locale(identifier: "en_US_POSIX")
                formatter.timeZone = TimeZone(secondsFromGMT: 0)
                formatter.dateFormat = "yyyy-MM"
                return formatter
            }(),
            {
                let formatter = DateFormatter()
                formatter.locale = Locale(identifier: "en_US_POSIX")
                formatter.timeZone = TimeZone(secondsFromGMT: 0)
                formatter.dateFormat = "yyyy"
                return formatter
            }()
        ]

        for formatter in formatters {
            if let date = formatter.date(from: rawValue) {
                return date
            }
        }

        return nil
    }

    private func loadLatestBeat() async -> FeaturedHomeBeat? {
        do {
            let snapshot = try await firestore.collection("nicmaBeatHub")
                .whereField("isPublic", isEqualTo: true)
                .limit(to: 20)
                .getDocuments()

            let latestDocument = snapshot.documents.max { lhs, rhs in
                documentDate(lhs) < documentDate(rhs)
            }

            guard let latestDocument else { return nil }
            return mapFeaturedBeat(from: latestDocument)
        } catch {
            return nil
        }
    }

    private func loadLatestVideo() async -> FeaturedHomeVideo? {
        do {
            let featuredSnapshot = try await firestore.collection("videographyHub")
                .whereField("isPublic", isEqualTo: true)
                .whereField("isHomeFeatured", isEqualTo: true)
                .limit(to: 1)
                .getDocuments()

            if let featuredDocument = featuredSnapshot.documents.first,
               let video = mapFeaturedVideo(from: featuredDocument) {
                return video
            }

            let snapshot = try await firestore.collection("videographyHub")
                .whereField("isPublic", isEqualTo: true)
                .limit(to: 12)
                .getDocuments()

            let latestDocument = snapshot.documents.max { lhs, rhs in
                documentDate(lhs) < documentDate(rhs)
            }

            guard let latestDocument else { return nil }
            return mapFeaturedVideo(from: latestDocument)
        } catch {
            return nil
        }
    }

    private func mapFeaturedVideo(from document: QueryDocumentSnapshot) -> FeaturedHomeVideo? {
        guard let title = document.data()["title"] as? String,
              !title.isEmpty else {
            return nil
        }

        return FeaturedHomeVideo(
            id: document.documentID,
            title: title,
            projectName: document.data()["projectName"] as? String ?? "Skydown Visual",
            notes: document.data()["notes"] as? String ?? "",
            downloadURL: document.data()["downloadURL"] as? String ?? "",
            externalURL: document.data()["externalURL"] as? String ?? "",
            embedURL: document.data()["embedURL"] as? String ?? "",
            sourceProvider: document.data()["sourceProvider"] as? String ?? ExternalMediaProvider.firebaseStorage.rawValue
        )
    }

    private func mapFeaturedBeat(from document: QueryDocumentSnapshot) -> FeaturedHomeBeat? {
        let data = document.data()
        guard let title = data["title"] as? String,
              !title.isEmpty else {
            return nil
        }

        let fileName = data["fileName"] as? String ?? ""
        let mimeType = data["mimeType"] as? String ?? ""

        return FeaturedHomeBeat(
            id: document.documentID,
            title: title,
            artistName: data["artistName"] as? String ?? "Skydown Beat",
            notes: data["notes"] as? String ?? "",
            downloadURL: data["downloadURL"] as? String ?? "",
            externalURL: data["externalURL"] as? String ?? "",
            sourceProvider: data["sourceProvider"] as? String ?? ExternalMediaProvider.firebaseStorage.rawValue,
            isPlayable: mimeType.hasPrefix("audio/") ||
                fileName.lowercased().hasSuffix(".mp3") ||
                fileName.lowercased().hasSuffix(".wav") ||
                fileName.lowercased().hasSuffix(".m4a")
        )
    }

    private func documentDate(_ document: QueryDocumentSnapshot) -> Date {
        let data = document.data()
        if let timestamp = data["createdAt"] as? Timestamp {
            return timestamp.dateValue()
        }
        if let date = data["createdAt"] as? Date {
            return date
        }
        return .distantPast
    }
}

private enum HomeRefreshResult {
    case track(Track?)
    case beat(FeaturedHomeBeat?)
    case video(FeaturedHomeVideo?)
}
