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

    // Toast
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let merchandiseService: MerchandiseServicing
    private let authManager: AuthManager
    private var stopObservingItems: (() -> Void)?

    private var canManageMerchandise: Bool {
        authManager.userSession?.isAdmin == true
    }

    init(
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService(),
        authManager: AuthManager
    ) {
        self.merchandiseService = merchandiseService
        self.authManager = authManager
    }

    func fetchData() {
        isLoading = true
        errorMessage = nil

        stopObservingItems?()

        stopObservingItems = merchandiseService.observeItems { [weak self] result in
            guard let self else { return }
            self.isLoading = false

            switch result {
            case .success(let items):
                self.merchandiseItems = items
                if items.isEmpty {
                    self.showUserToast("Keine Artikel gefunden.", style: .error)
                }
            case .failure(let error):
                print("Dev Fehler fetchData:", error.localizedDescription)
                self.showUserToast("Fehler beim Laden der Artikel: \(error.localizedDescription)", style: .error)
                self.merchandiseItems = []
            }
        }
    }

    func addMerchandise(_ item: MerchandiseItem, imageDataList: [Data]) async -> Bool {
        guard canManageMerchandise else {
            showUserToast("Nur Admins dürfen Artikel hinzufügen.", style: .error)
            return false
        }

        do {
            let imageURLs = try await merchandiseService.uploadImages(imageDataList)
            var itemToSave = item
            itemToSave.imageURLs = imageURLs

            try await merchandiseService.addItem(itemToSave)
            showUserToast("Artikel hinzugefügt: \(item.name)", style: .success)
            return true
        } catch {
            print("Dev Fehler addMerchandise:", error.localizedDescription)
            showUserToast("Fehler beim Hinzufügen des Artikels: \(error.localizedDescription)", style: .error)
            return false
        }
    }

    func updateMerchandisePrice(_ item: MerchandiseItem, newPrice: Double) async -> Bool {
        guard canManageMerchandise else {
            showUserToast("Nur Admins dürfen Artikel bearbeiten.", style: .error)
            return false
        }

        guard let id = item.id else {
            showUserToast("Artikel hat keine gültige ID.", style: .error)
            return false
        }

        do {
            try await merchandiseService.updatePrice(itemID: id, newPrice: newPrice)
            showUserToast("Preis aktualisiert: \(item.name)", style: .success)
            return true
        } catch {
            print("Dev Fehler updateMerchandisePrice:", error.localizedDescription)
            showUserToast("Update fehlgeschlagen: \(error.localizedDescription)", style: .error)
            return false
        }
    }

    func updateMerchandise(_ item: MerchandiseItem, imageDataList: [Data]) async -> Bool {
        guard canManageMerchandise else {
            showUserToast("Nur Admins dürfen Artikel bearbeiten.", style: .error)
            return false
        }

        guard item.id != nil else {
            showUserToast("Artikel hat keine gültige ID.", style: .error)
            return false
        }

        do {
            try await merchandiseService.updateItem(item, imageDataList: imageDataList)
            showUserToast("Artikel aktualisiert: \(item.name)", style: .success)
            return true
        } catch {
            print("Dev Fehler updateMerchandise:", error.localizedDescription)
            showUserToast("Update fehlgeschlagen: \(error.localizedDescription)", style: .error)
            return false
        }
    }

    func deleteItem(_ item: MerchandiseItem) async {
        guard canManageMerchandise else {
            showUserToast("Nur Admins dürfen Artikel löschen.", style: .error)
            return
        }

        guard let id = item.id else {
            showUserToast("Artikel hat keine gültige ID.", style: .error)
            return
        }

        do {
            try await merchandiseService.deleteItem(itemID: id)
            showUserToast("Artikel gelöscht: \(item.name)", style: .success)
        } catch {
            print("Dev Fehler deleteItem:", error.localizedDescription)
            showUserToast("Fehler beim Löschen: \(error.localizedDescription)", style: .error)
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    deinit {
        stopObservingItems?()
    }
}

struct FeaturedHomeVideo {
    let title: String
    let projectName: String
    let notes: String
}

struct FeaturedHomeBeat {
    let id: String
    let title: String
    let artistName: String
    let notes: String
    let downloadURL: String
    let isPlayable: Bool
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
    private let featuredArtists = ["Yang D. Nash", "ThaDude", "MAVE", "JANNO", "TANGAJOE007", "Toprack941"]

    init(musicService: MusicServicing = SpotifyMusicService()) {
        self.musicService = musicService
    }

    func refresh() {
        Task {
            featuredTrack = await loadLatestTrack()
            featuredBeat = await loadLatestBeat()
            featuredVideo = await loadLatestVideo()
            homeTrackMessage = featuredTrack == nil
                ? "Sobald ein neuer Release verfuegbar ist, taucht er hier direkt auf."
                : nil
            homeBeatMessage = featuredBeat == nil
                ? "Sobald ein freigegebener Beat live ist, taucht er hier direkt auf."
                : nil
            homeVideoMessage = featuredVideo == nil
                ? "Sobald ein oeffentliches Video live ist, taucht hier dein Highlight auf."
                : nil
        }
    }

    private func loadLatestTrack() async -> Track? {
        var latestTrack: Track?

        for artist in featuredArtists {
            let tracks = (try? await musicService.fetchTracks(for: artist)) ?? []
            for track in tracks {
                let candidateDate = track.releaseDate ?? ""
                let currentDate = latestTrack?.releaseDate ?? ""
                if latestTrack == nil || candidateDate > currentDate {
                    latestTrack = track
                }
            }
        }

        return latestTrack
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
            title: title,
            projectName: document.data()["projectName"] as? String ?? "Skydown Visual",
            notes: document.data()["notes"] as? String ?? ""
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
