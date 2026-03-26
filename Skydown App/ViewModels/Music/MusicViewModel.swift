//
//  MusicViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 26.07.25.
//

import Foundation
import FirebaseFirestore
import FirebaseStorage
import UniformTypeIdentifiers

@MainActor
class MusicViewModel: ObservableObject {
    @Published var tracks: [Track] = []
    @Published var isSpotifyConnected = false
    @Published var isConnectingSpotify = false

    // Toast
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let musicService: MusicServicing

    init(musicService: MusicServicing = SpotifyMusicService()) {
        self.musicService = musicService
        self.isSpotifyConnected = musicService.isConnected
    }

    func connectSpotify() async {
        isConnectingSpotify = true
        do {
            try await musicService.connect()
            isSpotifyConnected = musicService.isConnected
            showUserToast("Spotify verbunden", style: .success)
        } catch {
            print("Dev Fehler connectSpotify:", error.localizedDescription)
            print("Spotify Konfiguration:", musicService.debugConfigurationDescription)
            if let authorizationURL = musicService.lastAuthorizationURLString {
                print("Spotify Authorize URL:", authorizationURL)
            }
            showUserToast(
                "Spotify-Verbindung fehlgeschlagen: \(error.localizedDescription)\n\(musicService.debugConfigurationDescription)\nauthorize_url=\(musicService.lastAuthorizationURLString ?? "-")",
                style: .error
            )
        }
        isConnectingSpotify = false
    }

    func disconnectSpotify() {
        musicService.disconnect()
        isSpotifyConnected = false
        tracks = []
        showUserToast("Spotify getrennt", style: .info)
    }

    func fetchTracks(for artist: String) async {
        isSpotifyConnected = musicService.isConnected
        guard isSpotifyConnected else {
            tracks = []
            showUserToast("Verbinde zuerst Spotify, um Songs zu laden.", style: .info)
            return
        }

        do {
            let filteredTracks = try await musicService.fetchTracks(for: artist)

            if filteredTracks.isEmpty {
                showUserToast("Keine Songs für \(artist) gefunden", style: .error)
            } else {
                tracks = filteredTracks
                showUserToast("Songs erfolgreich geladen", style: .success)
            }

        } catch {
            print("Dev Fehler fetchTracks:", error.localizedDescription)
            showUserToast("Fehler beim Laden der Daten: \(error.localizedDescription)", style: .error)
            tracks = []
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}

struct NicmaSelectedFile: Identifiable {
    let id = UUID()
    let url: URL
    let fileName: String
    let fileSizeInBytes: Int64
    let mimeType: String
}

struct NicmaBeatHubItem: Identifiable {
    let id: String
    let title: String
    let artistName: String
    let fileName: String
    let downloadURL: String
    let notes: String
    let uploaderName: String
    let uploaderEmail: String
    let uploaderID: String
    let mimeType: String
    let isPublic: Bool
    let createdAt: Date

    var isPlayable: Bool {
        mimeType.hasPrefix("audio/")
    }
}

struct NicmaBeatUploadRequest {
    let beatTitle: String
    let artistName: String
    let email: String
    let notes: String
    let files: [NicmaSelectedFile]
}

protocol NicmaBeatHubServicing {
    func observeBeats(
        isAdmin: Bool,
        _ onChange: @escaping @MainActor (Result<[NicmaBeatHubItem], Error>) -> Void
    ) -> () -> Void
    func uploadBeats(_ request: NicmaBeatUploadRequest, currentUser: User?) async throws
    func updateBeatVisibility(beatID: String, isPublic: Bool) async throws
}

final class FirebaseNicmaBeatHubService: NicmaBeatHubServicing {
    private let firestore: Firestore
    private let storage: Storage
    private let collectionName = "nicmaBeatHub"

    init(storage: Storage = Storage.storage()) {
        self.firestore = Firestore.firestore()
        self.storage = storage
    }

    func observeBeats(
        isAdmin: Bool,
        _ onChange: @escaping @MainActor (Result<[NicmaBeatHubItem], Error>) -> Void
    ) -> () -> Void {
        let query = beatQuery(isAdmin: isAdmin)
        let listener = query
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        onChange(.failure(error))
                        return
                    }

                    let beats = snapshot?.documents.compactMap { document in
                        self?.mapBeat(document: document)
                    } ?? []
                    onChange(.success(beats))
                }
            }

        return {
            listener.remove()
        }
    }

    func uploadBeats(_ request: NicmaBeatUploadRequest, currentUser: User?) async throws {
        for file in request.files {
            try await upload(file, request: request, currentUser: currentUser)
        }
    }

    func updateBeatVisibility(beatID: String, isPublic: Bool) async throws {
        try await firestore.collection(collectionName).document(beatID).updateData([
            "isPublic": isPublic
        ])
    }

    private func upload(
        _ file: NicmaSelectedFile,
        request: NicmaBeatUploadRequest,
        currentUser: User?,
    ) async throws {
        let safeArtist = sanitizePathComponent(request.artistName)
        let safeFileName = sanitizePathComponent(file.fileName.replacingOccurrences(of: ".", with: "-"))
        let path = "nicma_music/beats/\(safeArtist)/\(Int(Date().timeIntervalSince1970))-\(UUID().uuidString)-\(safeFileName)"
        let reference = storage.reference().child(path)
        let metadata = StorageMetadata()
        metadata.contentType = file.mimeType
        metadata.customMetadata = [
            "artistName": request.artistName,
            "email": request.email,
            "notes": request.notes,
            "originalFilename": file.fileName,
            "uploadedAt": ISO8601DateFormatter().string(from: Date())
        ]

        let hasAccess = file.url.startAccessingSecurityScopedResource()
        defer {
            if hasAccess {
                file.url.stopAccessingSecurityScopedResource()
            }
        }

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            reference.putFile(from: file.url, metadata: metadata) { _, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }

        let downloadURL = try await reference.downloadURL()
        let title = request.beatTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let beatTitle = title.isEmpty ? displayTitle(from: file.fileName) : title

        let payload: [String: Any] = [
            "title": beatTitle,
            "artistName": request.artistName,
            "email": request.email,
            "notes": request.notes,
            "fileName": file.fileName,
            "mimeType": file.mimeType,
            "downloadURL": downloadURL.absoluteString,
            "storagePath": path,
            "uploaderName": currentUser?.username ?? request.artistName,
            "uploaderEmail": currentUser?.email ?? request.email,
            "uploaderID": currentUser?.id ?? "",
            "isPublic": currentUser?.isAdmin == true,
            "createdAt": Timestamp()
        ]

        try await firestore.collection(collectionName).addDocument(data: payload)
    }

    private func sanitizePathComponent(_ value: String) -> String {
        let raw = value
            .lowercased()
            .replacingOccurrences(of: "[^a-z0-9_-]+", with: "-", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "-"))

        return raw.isEmpty ? "upload" : raw
    }

    private func displayTitle(from fileName: String) -> String {
        let baseName = fileName.replacingOccurrences(
            of: "\\.[A-Za-z0-9]+$",
            with: "",
            options: .regularExpression
        )
        let cleaned = baseName
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        return cleaned.isEmpty ? "Beat Upload" : cleaned
    }

    private func beatQuery(isAdmin: Bool) -> Query {
        let baseCollection = firestore.collection(collectionName)
        if isAdmin {
            return baseCollection.order(by: "createdAt", descending: true)
        }

        return baseCollection
            .whereField("isPublic", isEqualTo: true)
            .order(by: "createdAt", descending: true)
    }

    private func mapBeat(document: QueryDocumentSnapshot) -> NicmaBeatHubItem? {
        let data = document.data()
        guard let title = data["title"] as? String,
              let artistName = data["artistName"] as? String,
              let fileName = data["fileName"] as? String,
              let downloadURL = data["downloadURL"] as? String else {
            return nil
        }

        let createdAt: Date
        if let timestamp = data["createdAt"] as? Timestamp {
            createdAt = timestamp.dateValue()
        } else if let date = data["createdAt"] as? Date {
            createdAt = date
        } else {
            createdAt = .now
        }

        return NicmaBeatHubItem(
            id: document.documentID,
            title: title,
            artistName: artistName,
            fileName: fileName,
            downloadURL: downloadURL,
            notes: data["notes"] as? String ?? "",
            uploaderName: data["uploaderName"] as? String ?? artistName,
            uploaderEmail: data["uploaderEmail"] as? String ?? (data["email"] as? String ?? ""),
            uploaderID: data["uploaderID"] as? String ?? "",
            mimeType: data["mimeType"] as? String ?? "application/octet-stream",
            isPublic: data["isPublic"] as? Bool ?? false,
            createdAt: createdAt
        )
    }
}

@MainActor
final class NicmaProducerViewModel: ObservableObject {
    @Published var beatTitle = ""
    @Published var artistName = ""
    @Published var email = ""
    @Published var notes = ""
    @Published var selectedFiles: [NicmaSelectedFile] = []
    @Published var beats: [NicmaBeatHubItem] = []
    @Published var isLoadingBeats = true
    @Published var isUploading = false
    @Published var isAdmin = false
    @Published var validationMessage: String?
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info

    private let service: NicmaBeatHubServicing
    private var currentUser: User?
    private var allBeats: [NicmaBeatHubItem] = []
    private var observationCancellation: (() -> Void)?
    private var observedAdminState: Bool?

    deinit {
        observationCancellation?()
    }

    init(service: NicmaBeatHubServicing = FirebaseNicmaBeatHubService()) {
        self.service = service
    }

    var canUpload: Bool {
        isAdmin &&
        !artistName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        email.contains("@") &&
        !selectedFiles.isEmpty &&
        !isUploading
    }

    func configure(currentUser: User?) {
        self.currentUser = currentUser
        let nextIsAdmin = currentUser?.isAdmin == true
        isAdmin = nextIsAdmin

        if email.isEmpty {
            email = currentUser?.email ?? ""
        }

        if artistName.isEmpty {
            artistName = currentUser?.username ?? ""
        }

        if observationCancellation == nil || observedAdminState != nextIsAdmin {
            observeBeats(isAdmin: nextIsAdmin)
        } else {
            applyVisibleBeats()
        }
    }

    func handleFileImport(_ result: Result<[URL], Error>) {
        guard isAdmin else {
            showUserToast("Uploads sind nur fuer Admins verfuegbar.", style: .info)
            return
        }

        switch result {
        case .success(let urls):
            let resolvedFiles = urls.compactMap(resolveSelectedFile(from:))
            guard !resolvedFiles.isEmpty else {
                validationMessage = "Bitte waehle mindestens eine Audio-Datei oder eine ZIP aus."
                showUserToast("Keine unterstuetzten Dateien gefunden.", style: .error)
                return
            }
            selectedFiles = resolvedFiles
            validationMessage = nil
            showUserToast("\(resolvedFiles.count) Datei(en) fuer den Beat Hub ausgewaehlt.", style: .success)
        case .failure:
            showUserToast("Die Dateien konnten nicht geoeffnet werden.", style: .error)
        }
    }

    func removeFile(_ fileID: UUID) {
        selectedFiles.removeAll { $0.id == fileID }
    }

    func toggleBeatVisibility(_ beat: NicmaBeatHubItem) async {
        guard isAdmin else { return }

        do {
            try await service.updateBeatVisibility(beatID: beat.id, isPublic: !beat.isPublic)
            showUserToast(
                beat.isPublic ? "Beat wurde wieder verborgen." : "Beat wurde fuer alle freigegeben.",
                style: .success
            )
        } catch {
            showUserToast("Der Beat-Status konnte nicht aktualisiert werden.", style: .error)
        }
    }

    func uploadSelectedBeats() async {
        guard isAdmin else {
            validationMessage = "Nur Admins koennen Beats hochladen."
            showUserToast("Uploads sind nur fuer Admins verfuegbar.", style: .info)
            return
        }

        let trimmedTitle = beatTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedArtist = artistName.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedNotes = notes.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedArtist.isEmpty else {
            validationMessage = "Bitte trag dein Projekt oder deinen Artist-Namen ein."
            return
        }

        guard trimmedEmail.contains("@") else {
            validationMessage = "Bitte trag eine gueltige E-Mail ein."
            return
        }

        guard !selectedFiles.isEmpty else {
            validationMessage = "Bitte waehle mindestens eine Audio-Datei oder eine ZIP aus."
            return
        }

        isUploading = true
        validationMessage = nil

        do {
            try await service.uploadBeats(
                NicmaBeatUploadRequest(
                    beatTitle: trimmedTitle,
                    artistName: trimmedArtist,
                    email: trimmedEmail,
                    notes: trimmedNotes,
                    files: selectedFiles,
                ),
                currentUser: currentUser
            )
            let uploadCount = selectedFiles.count
            selectedFiles = []
            beatTitle = ""
            notes = ""
            showUserToast("\(uploadCount) Beat-Datei(en) in den Beat Hub hochgeladen.", style: .success)
        } catch {
            showUserToast("Der Upload ist fehlgeschlagen. Bitte versuch es noch einmal.", style: .error)
        }

        isUploading = false
    }

    private func resolveSelectedFile(from url: URL) -> NicmaSelectedFile? {
        let hasAccess = url.startAccessingSecurityScopedResource()
        defer {
            if hasAccess {
                url.stopAccessingSecurityScopedResource()
            }
        }

        let resourceValues = try? url.resourceValues(forKeys: [.nameKey, .fileSizeKey, .contentTypeKey])
        let contentType = resourceValues?.contentType ?? UTType(filenameExtension: url.pathExtension) ?? .data

        guard contentType.conforms(to: .audio) || contentType.conforms(to: .zip) else {
            return nil
        }

        return NicmaSelectedFile(
            url: url,
            fileName: resourceValues?.name ?? url.lastPathComponent,
            fileSizeInBytes: Int64(resourceValues?.fileSize ?? 0),
            mimeType: contentType.preferredMIMEType ?? "application/octet-stream"
        )
    }

    private func observeBeats(isAdmin: Bool) {
        observedAdminState = isAdmin
        isLoadingBeats = true
        allBeats = []
        beats = []
        observationCancellation?()
        observationCancellation = service.observeBeats(isAdmin: isAdmin) { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let beats):
                allBeats = beats
                applyVisibleBeats()
            case .failure:
                isLoadingBeats = false
                showUserToast("Der Beat Hub konnte gerade nicht geladen werden.", style: .error)
            }
        }
    }

    private func applyVisibleBeats() {
        beats = isAdmin ? allBeats : allBeats.filter(\.isPublic)
        isLoadingBeats = false
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}
