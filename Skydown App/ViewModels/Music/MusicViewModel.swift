//
//  MusicViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 26.07.25.
//

// swiftlint:disable file_length

import Foundation
import FirebaseFirestore
import FirebaseStorage
import UniformTypeIdentifiers

@MainActor
class MusicViewModel: ObservableObject {
    @Published var tracks: [Track] = []
    @Published var isSpotifyConnected = false
    @Published var isConnectingSpotify = false
    @Published var isLoadingTracks = false

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
            showUserToast("Spotify ist bereit. Du kannst nahtlos weiterhoeren.", style: .success)
        } catch {
            showUserToast(
                "Spotify ist gerade nicht erreichbar. Bitte etwas spaeter erneut versuchen.",
                style: .info
            )
        }
        isConnectingSpotify = false
    }

    func disconnectSpotify() {
        musicService.disconnect()
        isSpotifyConnected = false
        showUserToast("Spotify wurde pausiert. Dein Music-Flow bleibt aktiv.", style: .info)
    }

    func fetchTracks(for artist: String) async {
        isSpotifyConnected = musicService.isConnected
        isLoadingTracks = true

        do {
            let filteredTracks = try await musicService.fetchTracks(for: artist)
            tracks = filteredTracks

            if filteredTracks.isEmpty {
                showUserToast("Fuer \(artist) sind gerade noch keine Tracks im Fokus.", style: .info)
            } else {
                showUserToast("Tracks sind bereit.", style: .success)
            }

        } catch {
            showUserToast("Tracks sind gerade nicht erreichbar. Versuche es in einem Moment erneut.", style: .error)
            tracks = []
        }

        isLoadingTracks = false
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}

private extension String {
    var musicNilIfEmpty: String? {
        isEmpty ? nil : self
    }
}

enum ExternalMediaProvider: String {
    case firebaseStorage = "firebase_storage"
    case googleDrive = "google_drive"
    case youTube = "youtube"
    case mega = "mega"
    case externalLink = "external_link"

    init(rawValueOrDefault value: String?) {
        self = ExternalMediaProvider(rawValue: value ?? "") ?? .firebaseStorage
    }

    var badgeLabel: String {
        switch self {
        case .firebaseStorage:
            return "Storage"
        case .googleDrive:
            return "Drive"
        case .youTube:
            return "YouTube"
        case .mega:
            return "MEGA"
        case .externalLink:
            return "Extern"
        }
    }

    var originalVideoActionTitle: String {
        switch self {
        case .firebaseStorage:
            return "Original oeffnen"
        case .googleDrive:
            return "Drive oeffnen"
        case .youTube:
            return "YouTube oeffnen"
        case .mega:
            return "MEGA oeffnen"
        case .externalLink:
            return "Link oeffnen"
        }
    }
}

private struct ExternalMediaSource {
    let provider: ExternalMediaProvider
    let normalizedURL: URL
    let externalURL: URL
    let embedURL: URL?
    let downloadURL: URL?
    let sourceFileID: String?
    let mimeType: String
}

private enum ExternalMediaKind {
    case video
    case audio
}

private func resolveExternalVideoSource(from rawURL: String) -> ExternalMediaSource? {
    resolveExternalMediaSource(from: rawURL, kind: .video)
}

private func resolveExternalAudioSource(from rawURL: String) -> ExternalMediaSource? {
    resolveExternalMediaSource(from: rawURL, kind: .audio)
}

private func resolveExternalMediaSource(from rawURL: String, kind: ExternalMediaKind) -> ExternalMediaSource? {
    guard let normalizedURL = normalizedExternalMediaURL(from: rawURL),
          let host = normalizedURL.host?.lowercased() else {
        return nil
    }

    if kind == .video,
       let videoID = externalYouTubeVideoID(from: rawURL, normalizedURL: normalizedURL),
       let externalURL = URL(string: "https://www.youtube.com/watch?v=\(videoID)") {
        return ExternalMediaSource(
            provider: .youTube,
            normalizedURL: normalizedURL,
            externalURL: externalURL,
            embedURL: URL(string: "https://www.youtube-nocookie.com/embed/\(videoID)?playsinline=1&rel=0&modestbranding=1&controls=1"),
            downloadURL: nil,
            sourceFileID: videoID,
            mimeType: "video/youtube"
        )
    }

    if host.contains("drive.google.com") || host.contains("docs.google.com"),
       let fileID = googleDriveFileID(from: rawURL, normalizedURL: normalizedURL),
       let externalURL = URL(string: "https://drive.google.com/file/d/\(fileID)/view") {
        return ExternalMediaSource(
            provider: .googleDrive,
            normalizedURL: normalizedURL,
            externalURL: externalURL,
            embedURL: kind == .video ? URL(string: "https://drive.google.com/file/d/\(fileID)/preview") : nil,
            downloadURL: nil,
            sourceFileID: fileID,
            mimeType: kind == .video ? "video/external" : "audio/external"
        )
    }

    if host.contains("mega.nz") || host.contains("mega.io") {
        return ExternalMediaSource(
            provider: .mega,
            normalizedURL: normalizedURL,
            externalURL: normalizedURL,
            embedURL: nil,
            downloadURL: isDirectMediaURL(normalizedURL, kind: kind) ? normalizedURL : nil,
            sourceFileID: nil,
            mimeType: directMimeType(for: normalizedURL, kind: kind) ?? (kind == .video ? "video/external" : "audio/external")
        )
    }

    let isDirect = isDirectMediaURL(normalizedURL, kind: kind)
    return ExternalMediaSource(
        provider: .externalLink,
        normalizedURL: normalizedURL,
        externalURL: normalizedURL,
        embedURL: nil,
        downloadURL: isDirect ? normalizedURL : nil,
        sourceFileID: nil,
        mimeType: directMimeType(for: normalizedURL, kind: kind) ?? (kind == .video ? "video/external" : "audio/external")
    )
}

private func normalizedExternalMediaURL(from rawURL: String) -> URL? {
    let trimmed = rawURL.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else { return nil }
    if let directURL = URL(string: trimmed), directURL.scheme != nil, directURL.host != nil {
        return directURL
    }
    return URL(string: "https://\(trimmed)")
}

private func googleDriveFileID(from rawURL: String, normalizedURL: URL) -> String? {
    if let components = URLComponents(url: normalizedURL, resolvingAgainstBaseURL: false),
       let id = components.queryItems?.first(where: { $0.name == "id" })?.value,
       !id.isEmpty {
        return id
    }

    let pathComponents = normalizedURL.pathComponents
    if let markerIndex = pathComponents.firstIndex(of: "d"), markerIndex + 1 < pathComponents.count {
        return pathComponents[markerIndex + 1]
    }

    let pattern = #"\/d\/([A-Za-z0-9_-]+)"#
    guard let regex = try? NSRegularExpression(pattern: pattern) else {
        return nil
    }
    let range = NSRange(rawURL.startIndex..<rawURL.endIndex, in: rawURL)
    guard let match = regex.firstMatch(in: rawURL, range: range),
          let idRange = Range(match.range(at: 1), in: rawURL) else {
        return nil
    }
    return String(rawURL[idRange])
}

private func externalYouTubeVideoID(from rawURL: String, normalizedURL: URL) -> String? {
    guard let components = URLComponents(url: normalizedURL, resolvingAgainstBaseURL: false) else {
        return nil
    }

    if let host = components.host?.lowercased(), host.contains("youtu.be") {
        return components.path
            .split(separator: "/")
            .first
            .map(String.init)?
            .externalTakeIfYouTubeID()
    }

    let pathComponents = components.path.split(separator: "/").map(String.init)
    if let markerIndex = pathComponents.firstIndex(where: { ["embed", "shorts", "live", "watch"].contains($0.lowercased()) }),
       markerIndex + 1 < pathComponents.count {
        return pathComponents[markerIndex + 1].externalTakeIfYouTubeID()
    }

    if let queryID = components.queryItems?.first(where: { $0.name == "v" || $0.name == "vi" })?.value?.externalTakeIfYouTubeID() {
        return queryID
    }

    let trimmed = rawURL.trimmingCharacters(in: .whitespacesAndNewlines)
    let pattern = #"(?:(?<=v=)|(?<=vi=)|(?<=\/embed\/)|(?<=\/shorts\/)|(?<=youtu\.be\/)|(?<=\/live\/))([A-Za-z0-9_-]{11})"#
    guard let regex = try? NSRegularExpression(pattern: pattern) else {
        return nil
    }

    let range = NSRange(trimmed.startIndex..<trimmed.endIndex, in: trimmed)
    guard let match = regex.firstMatch(in: trimmed, range: range),
          let idRange = Range(match.range(at: 1), in: trimmed) else {
        return nil
    }

    return String(trimmed[idRange]).externalTakeIfYouTubeID()
}

private func isDirectMediaURL(_ url: URL, kind: ExternalMediaKind) -> Bool {
    directMimeType(for: url, kind: kind) != nil
}

private func directMimeType(for url: URL, kind: ExternalMediaKind) -> String? {
    let path = url.path.lowercased()
    switch kind {
    case .video:
        if path.hasSuffix(".mp4") { return "video/mp4" }
        if path.hasSuffix(".mov") { return "video/quicktime" }
        if path.hasSuffix(".m4v") { return "video/x-m4v" }
        if path.hasSuffix(".webm") { return "video/webm" }
        if path.hasSuffix(".m3u8") { return "application/x-mpegURL" }
        return nil
    case .audio:
        if path.hasSuffix(".mp3") { return "audio/mpeg" }
        if path.hasSuffix(".wav") { return "audio/wav" }
        if path.hasSuffix(".m4a") { return "audio/mp4" }
        if path.hasSuffix(".aac") { return "audio/aac" }
        if path.hasSuffix(".flac") { return "audio/flac" }
        return nil
    }
}

private extension String {
    func externalTakeIfYouTubeID() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count == 11 else { return nil }
        return trimmed
    }
}

private func isDirectVideoPlaybackURLString(_ value: String) -> Bool {
    let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
    guard let url = URL(string: trimmed) else { return false }
    return directMimeType(for: url, kind: .video) != nil
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
    let externalURL: String
    let notes: String
    let uploaderName: String
    let uploaderEmail: String
    let uploaderID: String
    let mimeType: String
    let storagePath: String
    let isPublic: Bool
    let sourceProvider: String
    let sourceFileID: String
    let createdAt: Date

    var provider: ExternalMediaProvider {
        ExternalMediaProvider(rawValueOrDefault: sourceProvider)
    }

    var openURLString: String {
        externalURL.musicNilIfEmpty ?? downloadURL
    }

    var isPlayable: Bool {
        !downloadURL.isEmpty && (
            mimeType.hasPrefix("audio/")
            || fileName.lowercased().hasSuffix(".mp3")
            || fileName.lowercased().hasSuffix(".wav")
            || fileName.lowercased().hasSuffix(".m4a")
            || fileName.lowercased().hasSuffix(".aac")
            || fileName.lowercased().hasSuffix(".flac")
        )
    }

    var supportsInlinePlayback: Bool {
        isPlayable
    }
}

struct NicmaBeatUploadRequest {
    let beatTitle: String
    let artistName: String
    let email: String
    let notes: String
    let files: [NicmaSelectedFile]
}

struct NicmaExternalBeatRequest {
    let beatTitle: String
    let artistName: String
    let email: String
    let notes: String
    let externalURL: String
}

protocol NicmaBeatHubServicing {
    func observeBeats(
        isAdmin: Bool,
        _ onChange: @escaping @MainActor (Result<[NicmaBeatHubItem], Error>) -> Void
    ) -> () -> Void
    func uploadBeats(_ request: NicmaBeatUploadRequest, currentUser: User?) async throws
    func addExternalBeat(_ request: NicmaExternalBeatRequest, currentUser: User?) async throws
    func updateBeatVisibility(beatID: String, isPublic: Bool) async throws
    func deleteBeat(_ beat: NicmaBeatHubItem) async throws
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
                    }
                    .sorted { left, right in
                        left.createdAt > right.createdAt
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

    func addExternalBeat(_ request: NicmaExternalBeatRequest, currentUser: User?) async throws {
        guard let source = resolveExternalAudioSource(from: request.externalURL) else {
            throw NSError(domain: "SkydownExternalBeat", code: 1, userInfo: [
                NSLocalizedDescriptionKey: "Externer Beat-Link konnte nicht erkannt werden."
            ])
        }

        let trimmedTitle = request.beatTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let beatTitle = trimmedTitle.isEmpty
            ? "\(request.artistName.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty ?? "Beat") Beat"
            : trimmedTitle
        let fallbackFileName = (source.sourceFileID ?? "").musicNilIfEmpty.map { "source-\($0)" } ?? "\(source.provider.rawValue)-beat"
        let candidateFileName = source.normalizedURL.lastPathComponent.musicNilIfEmpty
        let fileName = {
            guard let candidateFileName else { return fallbackFileName }
            let lowered = candidateFileName.lowercased()
            return lowered == "view" || lowered == "preview" ? fallbackFileName : candidateFileName
        }()

        let payload: [String: Any] = [
            "title": beatTitle,
            "artistName": request.artistName,
            "email": request.email,
            "notes": request.notes,
            "fileName": fileName,
            "mimeType": source.mimeType,
            "downloadURL": source.downloadURL?.absoluteString ?? "",
            "externalURL": source.externalURL.absoluteString,
            "storagePath": "",
            "uploaderName": currentUser?.username ?? request.artistName,
            "uploaderEmail": currentUser?.email ?? request.email,
            "uploaderID": currentUser?.id ?? "",
            "isPublic": true,
            "sourceProvider": source.provider.rawValue,
            "sourceFileID": source.sourceFileID ?? "",
            "createdAt": Timestamp()
        ]

        try await firestore.collection(collectionName).addDocument(data: payload)
    }

    func updateBeatVisibility(beatID: String, isPublic: Bool) async throws {
        try await firestore.collection(collectionName).document(beatID).updateData([
            "isPublic": isPublic
        ])
    }

    func deleteBeat(_ beat: NicmaBeatHubItem) async throws {
        if !beat.storagePath.isEmpty {
            try await deleteStorageObject(path: beat.storagePath)
        }

        try await firestore.collection(collectionName).document(beat.id).delete()
    }

    private func upload(
        _ file: NicmaSelectedFile,
        request: NicmaBeatUploadRequest,
        currentUser: User?,
    ) async throws {
        let safeArtist = sanitizePathComponent(request.artistName)
        let path = buildUploadPath(
            rootFolder: "beats",
            scopeFolder: safeArtist,
            fileName: file.fileName
        )
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
        let stagedURL = try stageSecurityScopedUploadFile(
            from: file.url,
            fileName: file.fileName
        )
        defer {
            try? FileManager.default.removeItem(at: stagedURL)
        }

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            reference.putFile(from: stagedURL, metadata: metadata) { _, error in
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
            "externalURL": "",
            "storagePath": path,
            "uploaderName": currentUser?.username ?? request.artistName,
            "uploaderEmail": currentUser?.email ?? request.email,
            "uploaderID": currentUser?.id ?? "",
            "isPublic": currentUser?.canManageMusic == true,
            "sourceProvider": ExternalMediaProvider.firebaseStorage.rawValue,
            "sourceFileID": "",
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

    private func sanitizeFileName(_ fileName: String) -> String {
        let baseName = (fileName as NSString).deletingPathExtension
        let fileExtension = (fileName as NSString).pathExtension
            .lowercased()
            .replacingOccurrences(of: "[^a-z0-9]+", with: "", options: .regularExpression)
        let safeBaseName = sanitizePathComponent(baseName)

        if fileExtension.isEmpty {
            return safeBaseName
        }

        return "\(safeBaseName).\(fileExtension)"
    }

    private func buildUploadPath(
        rootFolder: String,
        scopeFolder: String,
        fileName: String
    ) -> String {
        let uploadID = "\(Int(Date().timeIntervalSince1970))-\(UUID().uuidString)"
        return "\(rootFolder)/\(scopeFolder)/\(uploadID)/\(sanitizeFileName(fileName))"
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
    }

    private func mapBeat(document: QueryDocumentSnapshot) -> NicmaBeatHubItem? {
        let data = document.data()
        guard let title = data["title"] as? String,
              let artistName = data["artistName"] as? String else {
            return nil
        }
        let fileName = data["fileName"] as? String ?? title
        let downloadURL = data["downloadURL"] as? String ?? ""
        let externalURL = data["externalURL"] as? String ?? ""
        guard !downloadURL.isEmpty || !externalURL.isEmpty else {
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
            externalURL: externalURL,
            notes: data["notes"] as? String ?? "",
            uploaderName: data["uploaderName"] as? String ?? artistName,
            uploaderEmail: data["uploaderEmail"] as? String ?? (data["email"] as? String ?? ""),
            uploaderID: data["uploaderID"] as? String ?? "",
            mimeType: data["mimeType"] as? String ?? "application/octet-stream",
            storagePath: data["storagePath"] as? String ?? "",
            isPublic: data["isPublic"] as? Bool ?? false,
            sourceProvider: data["sourceProvider"] as? String ?? ExternalMediaProvider.firebaseStorage.rawValue,
            sourceFileID: data["sourceFileID"] as? String ?? "",
            createdAt: createdAt
        )
    }

    private func deleteStorageObject(path: String) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            storage.reference().child(path).delete { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }
}

@MainActor
final class NicmaProducerViewModel: ObservableObject {
    @Published var beatTitle = ""
    @Published var artistName = ""
    @Published var email = ""
    @Published var notes = ""
    @Published var externalBeatURL = ""
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

    var canAddExternalBeat: Bool {
        isAdmin &&
        !artistName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        email.contains("@") &&
        !externalBeatURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !isUploading
    }

    func configure(currentUser: User?) {
        self.currentUser = currentUser
        let nextIsAdmin = currentUser?.canManageMusic == true
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

    func deleteBeat(_ beat: NicmaBeatHubItem) async {
        guard isAdmin else { return }

        do {
            try await service.deleteBeat(beat)
            showUserToast("Beat geloescht.", style: .success)
        } catch {
            showUserToast("Der Beat konnte nicht geloescht werden.", style: .error)
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

    func addExternalBeat() async {
        guard isAdmin else {
            validationMessage = "Nur Admins koennen externe Beats freigeben."
            showUserToast("Externe Beats sind nur fuer Admins verfuegbar.", style: .info)
            return
        }

        let trimmedTitle = beatTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedArtist = artistName.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedNotes = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedURL = externalBeatURL.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedArtist.isEmpty else {
            validationMessage = "Bitte trag dein Projekt oder deinen Artist-Namen ein."
            return
        }

        guard trimmedEmail.contains("@") else {
            validationMessage = "Bitte trag eine gueltige E-Mail ein."
            return
        }

        guard !trimmedURL.isEmpty else {
            validationMessage = "Bitte trag einen Drive-, MEGA- oder anderen Audio-Link ein."
            return
        }

        isUploading = true
        validationMessage = nil

        do {
            try await service.addExternalBeat(
                NicmaExternalBeatRequest(
                    beatTitle: trimmedTitle,
                    artistName: trimmedArtist,
                    email: trimmedEmail,
                    notes: trimmedNotes,
                    externalURL: trimmedURL
                ),
                currentUser: currentUser
            )
            beatTitle = ""
            notes = ""
            externalBeatURL = ""
            showUserToast("Externer Beat wurde freigegeben.", style: .success)
        } catch {
            let detail = error.localizedDescription.trimmingCharacters(in: .whitespacesAndNewlines)
            if detail.isEmpty {
                showUserToast("Der externe Beat-Link konnte nicht gespeichert werden.", style: .error)
            } else {
                showUserToast("Der externe Beat-Link konnte nicht gespeichert werden: \(detail)", style: .error)
            }
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

struct SkydownSelectedVideoFile: Identifiable {
    let id = UUID()
    let url: URL
    let fileName: String
    let fileSizeInBytes: Int64
    let mimeType: String
}

struct SkydownVideoHubItem: Identifiable {
    let id: String
    let title: String
    let projectName: String
    let fileName: String
    let downloadURL: String
    let externalURL: String
    let embedURL: String
    let notes: String
    let uploaderName: String
    let uploaderEmail: String
    let uploaderID: String
    let mimeType: String
    let storagePath: String
    let isPublic: Bool
    let isHomeFeatured: Bool
    let sourceProvider: String
    let sourceFileID: String
    let createdAt: Date

    var provider: ExternalMediaProvider {
        ExternalMediaProvider(rawValueOrDefault: sourceProvider)
    }

    var nativePlaybackURLString: String {
        let trimmedDownloadURL = downloadURL.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedDownloadURL.isEmpty {
            return trimmedDownloadURL
        }

        let trimmedExternalURL = externalURL.trimmingCharacters(in: .whitespacesAndNewlines)
        return isDirectVideoPlaybackURLString(trimmedExternalURL) ? trimmedExternalURL : ""
    }

    var openURLString: String {
        externalURL.musicNilIfEmpty ?? downloadURL
    }

    var inAppOriginalURLString: String {
        openURLString.musicNilIfEmpty ?? embedURL.musicNilIfEmpty ?? ""
    }

    var youTubeItem: SkydownYouTubeVideoItem? {
        guard provider == .youTube else { return nil }
        let resolvedURL = openURLString.musicNilIfEmpty ?? embedURL.musicNilIfEmpty
        guard let resolvedURL else { return nil }
        return SkydownYouTubeVideoItem(
            id: id,
            title: title,
            subtitle: notes.musicNilIfEmpty ?? projectName,
            urlString: resolvedURL
        )
    }

    var usesEmbeddedPreview: Bool {
        provider != .firebaseStorage && provider != .youTube && !embedURL.isEmpty && nativePlaybackURLString.isEmpty
    }

    var supportsInlinePlayback: Bool {
        youTubeItem != nil || usesEmbeddedPreview || isPlayable
    }

    var opensOriginalInApp: Bool {
        !inAppOriginalURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var directOpenActionTitle: String {
        provider.originalVideoActionTitle
    }

    var originalDestinationDescription: String {
        if inAppOriginalURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Kein Original-Link verfuegbar."
        }

        if !nativePlaybackURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Dieser Clip startet direkt in der In-App-Ansicht."
        }

        return "Dieser Link startet in einer In-App-Webansicht mit Zurueck und Schliessen."
    }

    var isPlayable: Bool {
        let playbackURLString = nativePlaybackURLString.trimmingCharacters(in: .whitespacesAndNewlines)
        return !playbackURLString.isEmpty && (
            mimeType.hasPrefix("video/")
            || isDirectVideoPlaybackURLString(playbackURLString)
        )
    }
}

struct SkydownVideoUploadRequest {
    let title: String
    let projectName: String
    let email: String
    let notes: String
    let files: [SkydownSelectedVideoFile]
}

struct SkydownExternalVideoRequest {
    let title: String
    let projectName: String
    let email: String
    let notes: String
    let externalURL: String
}

struct SkydownVideoUpdateRequest {
    let title: String
    let projectName: String
    let notes: String
    let isPublic: Bool
}

protocol SkydownVideoHubServicing {
    func observeVideos(
        isAdmin: Bool,
        _ onChange: @escaping @MainActor (Result<[SkydownVideoHubItem], Error>) -> Void
    ) -> () -> Void
    func observePublicConfig(
        _ onChange: @escaping @MainActor (Result<SkydownVideoHubPublicConfig, Error>) -> Void
    ) -> () -> Void
    func uploadVideos(_ request: SkydownVideoUploadRequest, currentUser: User?) async throws
    func addExternalVideo(_ request: SkydownExternalVideoRequest, currentUser: User?) async throws
    func updateVideo(_ video: SkydownVideoHubItem, request: SkydownVideoUpdateRequest, currentUser: User?) async throws
    func setHomeFeaturedVideo(_ video: SkydownVideoHubItem?) async throws
    func deleteVideo(_ video: SkydownVideoHubItem) async throws
    func savePublicConfig(_ config: SkydownVideoHubPublicConfig, currentUser: User?) async throws
}

final class FirebaseSkydownVideoHubService: SkydownVideoHubServicing {
    private let firestore: Firestore
    private let storage: Storage
    private let collectionName = "videographyHub"
    private let configCollectionName = "videographyHubMeta"
    private let configDocumentID = "publicConfig"

    init(storage: Storage = Storage.storage()) {
        self.firestore = Firestore.firestore()
        self.storage = storage
    }

    func observeVideos(
        isAdmin: Bool,
        _ onChange: @escaping @MainActor (Result<[SkydownVideoHubItem], Error>) -> Void
    ) -> () -> Void {
        let query = videoQuery(isAdmin: isAdmin)
        let listener = query.addSnapshotListener { [weak self] snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                let videos = snapshot?.documents.compactMap { document in
                    self?.mapVideo(document: document)
                }
                .sorted { left, right in
                    if left.isHomeFeatured != right.isHomeFeatured {
                        return left.isHomeFeatured && !right.isHomeFeatured
                    }
                    return left.createdAt > right.createdAt
                } ?? []
                onChange(.success(videos))
            }
        }

        return {
            listener.remove()
        }
    }

    func observePublicConfig(
        _ onChange: @escaping @MainActor (Result<SkydownVideoHubPublicConfig, Error>) -> Void
    ) -> () -> Void {
        let listener = firestore.collection(configCollectionName)
            .document(configDocumentID)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        onChange(.failure(error))
                        return
                    }

                    guard let self else {
                        onChange(.success(.default))
                        return
                    }

                    onChange(.success(self.mapPublicConfig(snapshot: snapshot)))
                }
            }

        return {
            listener.remove()
        }
    }

    func uploadVideos(_ request: SkydownVideoUploadRequest, currentUser: User?) async throws {
        for file in request.files {
            try await upload(file, request: request, currentUser: currentUser)
        }
    }

    func addExternalVideo(_ request: SkydownExternalVideoRequest, currentUser: User?) async throws {
        guard let source = resolveExternalVideoSource(from: request.externalURL) else {
            throw NSError(domain: "SkydownExternalVideo", code: 1, userInfo: [
                NSLocalizedDescriptionKey: "Externer Video-Link konnte nicht erkannt werden."
            ])
        }

        let trimmedTitle = request.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let title = trimmedTitle.isEmpty
            ? "\(request.projectName.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty ?? "Video") Clip"
            : trimmedTitle
        let fallbackFileName = (source.sourceFileID ?? "").musicNilIfEmpty.map { "source-\($0)" } ?? "\(source.provider.rawValue)-video"
        let candidateFileName = source.normalizedURL.lastPathComponent.musicNilIfEmpty
        let fileName = {
            guard let candidateFileName else { return fallbackFileName }
            let lowered = candidateFileName.lowercased()
            return lowered == "view" || lowered == "preview" ? fallbackFileName : candidateFileName
        }()

        let payload: [String: Any] = [
            "title": title,
            "projectName": request.projectName,
            "email": request.email,
            "notes": request.notes,
            "fileName": fileName,
            "mimeType": source.mimeType,
            "downloadURL": source.downloadURL?.absoluteString ?? "",
            "externalURL": source.externalURL.absoluteString,
            "embedURL": source.embedURL?.absoluteString ?? "",
            "storagePath": "",
            "uploaderName": currentUser?.username ?? request.projectName,
            "uploaderEmail": currentUser?.email ?? request.email,
            "uploaderID": currentUser?.id ?? "",
            "isPublic": true,
            "isHomeFeatured": false,
            "sourceProvider": source.provider.rawValue,
            "sourceFileID": source.sourceFileID ?? "",
            "createdAt": Timestamp()
        ]

        try await firestore.collection(collectionName).addDocument(data: payload)
    }

    func updateVideo(
        _ video: SkydownVideoHubItem,
        request: SkydownVideoUpdateRequest,
        currentUser: User?
    ) async throws {
        var updates: [String: Any] = [
            "title": request.title,
            "projectName": request.projectName,
            "notes": request.notes,
            "isPublic": request.isPublic,
            "updatedAt": FieldValue.serverTimestamp(),
            "updatedBy": currentUser?.id ?? ""
        ]

        if !request.isPublic {
            updates["isHomeFeatured"] = false
        }

        try await firestore.collection(collectionName)
            .document(video.id)
            .setData(updates, merge: true)
    }

    func deleteVideo(_ video: SkydownVideoHubItem) async throws {
        if !video.storagePath.isEmpty {
            try await deleteStorageObject(path: video.storagePath)
        }

        try await firestore.collection(collectionName).document(video.id).delete()
    }

    func setHomeFeaturedVideo(_ video: SkydownVideoHubItem?) async throws {
        let collection = firestore.collection(collectionName)
        let currentFeatured = try await collection
            .whereField("isHomeFeatured", isEqualTo: true)
            .getDocuments()

        let batch = firestore.batch()
        currentFeatured.documents.forEach { document in
            batch.updateData(["isHomeFeatured": false], forDocument: document.reference)
        }

        if let video {
            batch.setData(
                ["isHomeFeatured": true],
                forDocument: collection.document(video.id),
                merge: true
            )
        }

        try await batch.commit()
    }

    func savePublicConfig(_ config: SkydownVideoHubPublicConfig, currentUser: User?) async throws {
        let equipmentItems = config.equipmentItems.map { item in
            [
                "id": item.id,
                "title": item.title,
                "detail": item.detail,
                "imageURLString": item.imageURLString ?? "",
                "imageUrl": item.imageURLString ?? ""
            ]
        }
        let youtubeItems = config.youtubeItems.map { item in
            [
                "id": item.id,
                "title": item.title,
                "subtitle": item.subtitle,
                "urlString": item.urlString
            ]
        }
        let collaborationItems = config.collaborationItems.map { item in
            [
                "id": item.id,
                "name": item.name,
                "role": item.role,
                "highlight": item.highlight,
                "vibe": item.vibe,
                "imageURLString": item.imageURLString ?? "",
                "imageUrl": item.imageURLString ?? "",
                "spotifyArtistID": item.spotifyArtistID ?? "",
                "spotifyArtistId": item.spotifyArtistID ?? "",
                "instagramURLString": item.instagramURLString ?? "",
                "instagramUrl": item.instagramURLString ?? "",
                "youtubeURLString": item.youtubeURLString ?? ""
            ]
        }

        try await firestore.collection(configCollectionName)
            .document(configDocumentID)
            .setData(
                [
                    "equipmentItems": equipmentItems,
                    "youtubeItems": youtubeItems,
                    "collaborationItems": collaborationItems,
                    "updatedAt": Timestamp(),
                    "updatedBy": currentUser?.id ?? ""
                ],
                merge: true
            )
    }

    private func upload(
        _ file: SkydownSelectedVideoFile,
        request: SkydownVideoUploadRequest,
        currentUser: User?
    ) async throws {
        let safeProject = sanitizePathComponent(request.projectName)
        let path = buildUploadPath(
            rootFolder: "videos",
            scopeFolder: safeProject,
            fileName: file.fileName
        )
        let reference = storage.reference().child(path)
        let metadata = StorageMetadata()
        metadata.contentType = file.mimeType
        metadata.customMetadata = [
            "projectName": request.projectName,
            "email": request.email,
            "notes": request.notes,
            "originalFilename": file.fileName,
            "uploadedAt": ISO8601DateFormatter().string(from: Date())
        ]
        let stagedURL = try stageSecurityScopedUploadFile(
            from: file.url,
            fileName: file.fileName
        )
        defer {
            try? FileManager.default.removeItem(at: stagedURL)
        }

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            reference.putFile(from: stagedURL, metadata: metadata) { _, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }

        let downloadURL = try await reference.downloadURL()
        let trimmedTitle = request.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let videoTitle = trimmedTitle.isEmpty ? displayTitle(from: file.fileName) : trimmedTitle

        let payload: [String: Any] = [
            "title": videoTitle,
            "projectName": request.projectName,
            "email": request.email,
            "notes": request.notes,
            "fileName": file.fileName,
            "mimeType": file.mimeType,
            "downloadURL": downloadURL.absoluteString,
            "externalURL": "",
            "embedURL": "",
            "storagePath": path,
            "uploaderName": currentUser?.username ?? request.projectName,
            "uploaderEmail": currentUser?.email ?? request.email,
            "uploaderID": currentUser?.id ?? "",
            "isPublic": currentUser?.canManageVideos == true,
            "isHomeFeatured": false,
            "sourceProvider": ExternalMediaProvider.firebaseStorage.rawValue,
            "sourceFileID": "",
            "createdAt": Timestamp()
        ]

        try await firestore.collection(collectionName).addDocument(data: payload)
    }

    private func deleteStorageObject(path: String) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            storage.reference().child(path).delete { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    private func videoQuery(isAdmin: Bool) -> Query {
        let baseCollection = firestore.collection(collectionName)
        if isAdmin {
            return baseCollection.order(by: "createdAt", descending: true)
        }

        return baseCollection
            .whereField("isPublic", isEqualTo: true)
    }

    private func mapVideo(document: QueryDocumentSnapshot) -> SkydownVideoHubItem? {
        let data = document.data()
        guard let title = data["title"] as? String,
              let projectName = data["projectName"] as? String else {
            return nil
        }
        let fileName = data["fileName"] as? String ?? title
        let downloadURL = data["downloadURL"] as? String ?? ""
        let externalURL = data["externalURL"] as? String ?? ""
        let embedURL = data["embedURL"] as? String ?? ""
        guard !downloadURL.isEmpty || !externalURL.isEmpty || !embedURL.isEmpty else {
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

        return SkydownVideoHubItem(
            id: document.documentID,
            title: title,
            projectName: projectName,
            fileName: fileName,
            downloadURL: downloadURL,
            externalURL: externalURL,
            embedURL: embedURL,
            notes: data["notes"] as? String ?? "",
            uploaderName: data["uploaderName"] as? String ?? projectName,
            uploaderEmail: data["uploaderEmail"] as? String ?? (data["email"] as? String ?? ""),
            uploaderID: data["uploaderID"] as? String ?? "",
            mimeType: data["mimeType"] as? String ?? "video/mp4",
            storagePath: data["storagePath"] as? String ?? "",
            isPublic: data["isPublic"] as? Bool ?? false,
            isHomeFeatured: data["isHomeFeatured"] as? Bool ?? false,
            sourceProvider: data["sourceProvider"] as? String ?? ExternalMediaProvider.firebaseStorage.rawValue,
            sourceFileID: data["sourceFileID"] as? String ?? "",
            createdAt: createdAt
        )
    }

    private func sanitizePathComponent(_ value: String) -> String {
        let raw = value
            .lowercased()
            .replacingOccurrences(of: "[^a-z0-9_-]+", with: "-", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "-"))

        return raw.isEmpty ? "upload" : raw
    }

    private func sanitizeFileName(_ fileName: String) -> String {
        let baseName = (fileName as NSString).deletingPathExtension
        let fileExtension = (fileName as NSString).pathExtension
            .lowercased()
            .replacingOccurrences(of: "[^a-z0-9]+", with: "", options: .regularExpression)
        let safeBaseName = sanitizePathComponent(baseName)

        if fileExtension.isEmpty {
            return safeBaseName
        }

        return "\(safeBaseName).\(fileExtension)"
    }

    private func buildUploadPath(
        rootFolder: String,
        scopeFolder: String,
        fileName: String
    ) -> String {
        let uploadID = "\(Int(Date().timeIntervalSince1970))-\(UUID().uuidString)"
        return "\(rootFolder)/\(scopeFolder)/\(uploadID)/\(sanitizeFileName(fileName))"
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

        return cleaned.isEmpty ? "Video Upload" : cleaned
    }

    private func mapPublicConfig(snapshot: DocumentSnapshot?) -> SkydownVideoHubPublicConfig {
        guard let data = snapshot?.data() else {
            return .default
        }

        let equipmentItems = (data["equipmentItems"] as? [[String: Any]])?
            .compactMap { mapEquipmentItem($0) } ?? SkydownVideoHubPublicConfig.default.equipmentItems
        let youtubeItems = (data["youtubeItems"] as? [[String: Any]])?
            .compactMap { mapYouTubeItem($0) } ?? SkydownVideoHubPublicConfig.default.youtubeItems
        let collaborationItems = (data["collaborationItems"] as? [[String: Any]])?
            .compactMap { mapCollaborationItem($0) } ?? SkydownVideoHubPublicConfig.default.collaborationItems

        return SkydownVideoHubPublicConfig(
            equipmentItems: equipmentItems.isEmpty ? SkydownVideoHubPublicConfig.default.equipmentItems : equipmentItems,
            youtubeItems: youtubeItems,
            collaborationItems: collaborationItems.isEmpty ? SkydownVideoHubPublicConfig.default.collaborationItems : collaborationItems
        )
    }

    private func mapEquipmentItem(_ value: [String: Any]) -> SkydownVideoEquipmentItem? {
        let title = (value["title"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let detail = (value["detail"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !title.isEmpty, !detail.isEmpty else { return nil }
        let rawID = (value["id"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        return SkydownVideoEquipmentItem(
            id: rawID.isEmpty ? UUID().uuidString : rawID,
            title: title,
            detail: detail,
            imageURLString: (
                (value["imageURLString"] as? String)
                ?? (value["imageUrl"] as? String)
            )?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty
        )
    }

    private func mapYouTubeItem(_ value: [String: Any]) -> SkydownYouTubeVideoItem? {
        let title = (value["title"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let subtitle = (value["subtitle"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let urlString = (value["urlString"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !title.isEmpty, !urlString.isEmpty else { return nil }
        let rawID = (value["id"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        return SkydownYouTubeVideoItem(
            id: rawID.isEmpty ? UUID().uuidString : rawID,
            title: title,
            subtitle: subtitle,
            urlString: urlString
        )
    }

    private func mapCollaborationItem(_ value: [String: Any]) -> SkydownProducedWithArtist? {
        let name = (value["name"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let role = (value["role"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let highlight = (value["highlight"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let vibe = (value["vibe"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !name.isEmpty, !role.isEmpty else { return nil }
        let rawID = (value["id"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        return SkydownProducedWithArtist(
            id: rawID.isEmpty ? UUID().uuidString : rawID,
            name: name,
            role: role,
            highlight: highlight,
            vibe: vibe,
            imageURLString: (
                (value["imageURLString"] as? String)
                ?? (value["imageUrl"] as? String)
            )?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty,
            spotifyArtistID: (
                (value["spotifyArtistID"] as? String)
                ?? (value["spotifyArtistId"] as? String)
            )?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty,
            instagramURLString: (
                (value["instagramURLString"] as? String)
                ?? (value["instagramUrl"] as? String)
            )?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty,
            youtubeURLString: (
                (value["youtubeURLString"] as? String)
                ?? (value["youtubeUrl"] as? String)
            )?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty
        )
    }
}

private func stageSecurityScopedUploadFile(
    from sourceURL: URL,
    fileName: String
) throws -> URL {
    let hasAccess = sourceURL.startAccessingSecurityScopedResource()
    defer {
        if hasAccess {
            sourceURL.stopAccessingSecurityScopedResource()
        }
    }

    let stagingDirectory = FileManager.default.temporaryDirectory
        .appendingPathComponent("upload-staging", isDirectory: true)
    try FileManager.default.createDirectory(
        at: stagingDirectory,
        withIntermediateDirectories: true
    )

    let fileExtension = (fileName as NSString).pathExtension
    let stagedURL = if fileExtension.isEmpty {
        stagingDirectory.appendingPathComponent(UUID().uuidString)
    } else {
        stagingDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension(fileExtension)
    }

    if FileManager.default.fileExists(atPath: stagedURL.path) {
        try FileManager.default.removeItem(at: stagedURL)
    }

    try FileManager.default.copyItem(at: sourceURL, to: stagedURL)
    return stagedURL
}

@MainActor
final class SkydownVideoHubViewModel: ObservableObject {
    @Published var videoTitle = ""
    @Published var projectName = ""
    @Published var email = ""
    @Published var notes = ""
    @Published var externalVideoURL = ""
    @Published var selectedFiles: [SkydownSelectedVideoFile] = []
    @Published var videos: [SkydownVideoHubItem] = []
    @Published var isLoadingVideos = true
    @Published var isUploading = false
    @Published var isSavingPublicConfig = false
    @Published var isAdmin = false
    @Published var publicConfig: SkydownVideoHubPublicConfig = .default
    @Published var validationMessage: String?
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .info

    private let service: SkydownVideoHubServicing
    private var currentUser: User?
    private var allVideos: [SkydownVideoHubItem] = []
    private var observationCancellation: (() -> Void)?
    private var configObservationCancellation: (() -> Void)?
    private var observedAdminState: Bool?
    private var hasLoadedVideosOnce = false

    deinit {
        observationCancellation?()
        configObservationCancellation?()
    }

    init(service: SkydownVideoHubServicing = FirebaseSkydownVideoHubService()) {
        self.service = service
        observePublicConfig()
    }

    var canUpload: Bool {
        isAdmin &&
        !projectName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        email.contains("@") &&
        !selectedFiles.isEmpty &&
        !isUploading
    }

    var canAddExternalVideo: Bool {
        isAdmin &&
        !projectName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        email.contains("@") &&
        !externalVideoURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !isUploading
    }

    func configure(currentUser: User?) {
        self.currentUser = currentUser
        let nextIsAdmin = currentUser?.canManageVideos == true
        isAdmin = nextIsAdmin

        if email.isEmpty {
            email = currentUser?.email ?? ""
        }

        if projectName.isEmpty {
            projectName = currentUser?.username ?? ""
        }

        if observationCancellation == nil || observedAdminState != nextIsAdmin {
            observeVideos(isAdmin: nextIsAdmin)
        } else {
            applyVisibleVideos()
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
                validationMessage = "Bitte waehle mindestens eine MP4-, MOV- oder M4V-Datei aus."
                showUserToast("Keine unterstuetzten Videoformate gefunden.", style: .error)
                return
            }
            selectedFiles = resolvedFiles
            validationMessage = nil
            showUserToast("\(resolvedFiles.count) Video-Datei(en) ausgewaehlt.", style: .success)
        case .failure:
            showUserToast("Die Videos konnten nicht geoeffnet werden.", style: .error)
        }
    }

    func removeFile(_ fileID: UUID) {
        selectedFiles.removeAll { $0.id == fileID }
    }

    func uploadSelectedVideos() async {
        guard isAdmin else {
            validationMessage = "Nur Admins koennen Videos hochladen."
            showUserToast("Uploads sind nur fuer Admins verfuegbar.", style: .info)
            return
        }

        let trimmedTitle = videoTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedProject = projectName.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedNotes = notes.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedProject.isEmpty else {
            validationMessage = "Bitte trag ein Projekt, einen Artist oder einen Videotitel ein."
            return
        }

        guard trimmedEmail.contains("@") else {
            validationMessage = "Bitte trag eine gueltige E-Mail ein."
            return
        }

        guard !selectedFiles.isEmpty else {
            validationMessage = "Bitte waehle mindestens eine MP4-, MOV- oder M4V-Datei aus."
            return
        }

        isUploading = true
        validationMessage = nil

        do {
            try await service.uploadVideos(
                SkydownVideoUploadRequest(
                    title: trimmedTitle,
                    projectName: trimmedProject,
                    email: trimmedEmail,
                    notes: trimmedNotes,
                    files: selectedFiles
                ),
                currentUser: currentUser
            )
            let uploadCount = selectedFiles.count
            selectedFiles = []
            videoTitle = ""
            notes = ""
            showUserToast("\(uploadCount) Video-Datei(en) hochgeladen.", style: .success)
        } catch {
            let detail = error.localizedDescription.trimmingCharacters(in: .whitespacesAndNewlines)
            if detail.isEmpty {
                showUserToast("Der Video-Upload ist fehlgeschlagen. Bitte versuch es noch einmal.", style: .error)
            } else {
                showUserToast("Der Video-Upload ist fehlgeschlagen: \(detail)", style: .error)
            }
        }

        isUploading = false
    }

    func addExternalVideo() async {
        guard isAdmin else {
            validationMessage = "Nur Admins koennen externe Reels freigeben."
            showUserToast("Externe Reels sind nur fuer Admins verfuegbar.", style: .info)
            return
        }

        let trimmedTitle = videoTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedProject = projectName.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedNotes = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedURL = externalVideoURL.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedProject.isEmpty else {
            validationMessage = "Bitte trag ein Projekt, einen Artist oder einen Videotitel ein."
            return
        }

        guard trimmedEmail.contains("@") else {
            validationMessage = "Bitte trag eine gueltige E-Mail ein."
            return
        }

        guard !trimmedURL.isEmpty else {
            validationMessage = "Bitte trag einen Google-Drive-, MEGA- oder anderen Video-Link ein."
            return
        }

        isUploading = true
        validationMessage = nil

        do {
            try await service.addExternalVideo(
                SkydownExternalVideoRequest(
                    title: trimmedTitle,
                    projectName: trimmedProject,
                    email: trimmedEmail,
                    notes: trimmedNotes,
                    externalURL: trimmedURL
                ),
                currentUser: currentUser
            )
            videoTitle = ""
            notes = ""
            externalVideoURL = ""
            showUserToast("Externes Reel wurde freigegeben.", style: .success)
        } catch {
            let detail = error.localizedDescription.trimmingCharacters(in: .whitespacesAndNewlines)
            if detail.isEmpty {
                showUserToast("Der externe Reel-Link konnte nicht gespeichert werden.", style: .error)
            } else {
                showUserToast("Der externe Reel-Link konnte nicht gespeichert werden: \(detail)", style: .error)
            }
        }

        isUploading = false
    }

    func updateVideo(
        _ video: SkydownVideoHubItem,
        title: String,
        projectName: String,
        notes: String,
        isPublic: Bool
    ) async {
        guard isAdmin else { return }

        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedProject = projectName.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedNotes = notes.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedTitle.isEmpty else {
            validationMessage = "Bitte trag einen Videotitel ein."
            showUserToast("Videotitel fehlt.", style: .error)
            return
        }

        guard !trimmedProject.isEmpty else {
            validationMessage = "Bitte trag ein Projekt oder einen Artist ein."
            showUserToast("Projekt oder Artist fehlt.", style: .error)
            return
        }

        do {
            try await service.updateVideo(
                video,
                request: SkydownVideoUpdateRequest(
                    title: trimmedTitle,
                    projectName: trimmedProject,
                    notes: trimmedNotes,
                    isPublic: isPublic
                ),
                currentUser: currentUser
            )
            validationMessage = nil
            showUserToast("Video gespeichert.", style: .success)
        } catch {
            showUserToast("Das Video konnte nicht gespeichert werden.", style: .error)
        }
    }

    func deleteVideo(_ video: SkydownVideoHubItem) async {
        guard isAdmin else { return }

        do {
            try await service.deleteVideo(video)
            showUserToast("Video entfernt.", style: .success)
        } catch {
            showUserToast("Das Video konnte nicht geloescht werden.", style: .error)
        }
    }

    func toggleHomeFeatured(_ video: SkydownVideoHubItem) async {
        guard isAdmin else { return }

        do {
            try await service.setHomeFeaturedVideo(video.isHomeFeatured ? nil : video)
            showUserToast(video.isHomeFeatured ? "Home-Video entfernt." : "Video fuer Home ausgewaehlt.", style: .success)
        } catch {
            showUserToast("Das Home-Video konnte nicht aktualisiert werden.", style: .error)
        }
    }

    func addEquipmentItem() {
        publicConfig.equipmentItems.append(
            SkydownVideoEquipmentItem(
                id: UUID().uuidString,
                title: "",
                detail: "",
                imageURLString: nil
            )
        )
    }

    func removeEquipmentItem(_ itemID: String) {
        publicConfig.equipmentItems.removeAll { $0.id == itemID }
    }

    func updateEquipmentItem(
        _ itemID: String,
        title: String? = nil,
        detail: String? = nil,
        imageURLString: String? = nil
    ) {
        guard let index = publicConfig.equipmentItems.firstIndex(where: { $0.id == itemID }) else {
            return
        }

        if let title {
            publicConfig.equipmentItems[index].title = title
        }

        if let detail {
            publicConfig.equipmentItems[index].detail = detail
        }

        if let imageURLString {
            publicConfig.equipmentItems[index].imageURLString = imageURLString
        }
    }

    func addYouTubeItem() {
        publicConfig.youtubeItems.append(
            SkydownYouTubeVideoItem(
                id: UUID().uuidString,
                title: "",
                subtitle: "",
                urlString: ""
            )
        )
    }

    func removeYouTubeItem(_ itemID: String) {
        publicConfig.youtubeItems.removeAll { $0.id == itemID }
    }

    func updateYouTubeItem(_ itemID: String, title: String? = nil, subtitle: String? = nil, urlString: String? = nil) {
        guard let index = publicConfig.youtubeItems.firstIndex(where: { $0.id == itemID }) else {
            return
        }

        if let title {
            publicConfig.youtubeItems[index].title = title
        }

        if let subtitle {
            publicConfig.youtubeItems[index].subtitle = subtitle
        }

        if let urlString {
            publicConfig.youtubeItems[index].urlString = urlString
        }
    }

    func addCollaborationItem() {
        publicConfig.collaborationItems.append(
            SkydownProducedWithArtist(
                id: UUID().uuidString,
                name: "",
                role: "",
                highlight: "",
                vibe: "",
                imageURLString: nil,
                spotifyArtistID: nil,
                instagramURLString: nil,
                youtubeURLString: nil
            )
        )
    }

    func removeCollaborationItem(_ itemID: String) {
        publicConfig.collaborationItems.removeAll { $0.id == itemID }
    }

    func updateCollaborationItem(
        _ itemID: String,
        name: String? = nil,
        role: String? = nil,
        highlight: String? = nil,
        vibe: String? = nil,
        imageURLString: String? = nil,
        spotifyArtistID: String? = nil,
        instagramURLString: String? = nil,
        youtubeURLString: String? = nil
    ) {
        guard let index = publicConfig.collaborationItems.firstIndex(where: { $0.id == itemID }) else {
            return
        }

        if let name {
            publicConfig.collaborationItems[index].name = name
        }
        if let role {
            publicConfig.collaborationItems[index].role = role
        }
        if let highlight {
            publicConfig.collaborationItems[index].highlight = highlight
        }
        if let vibe {
            publicConfig.collaborationItems[index].vibe = vibe
        }
        if let imageURLString {
            publicConfig.collaborationItems[index].imageURLString = imageURLString
        }
        if let spotifyArtistID {
            publicConfig.collaborationItems[index].spotifyArtistID = spotifyArtistID
        }
        if let instagramURLString {
            publicConfig.collaborationItems[index].instagramURLString = instagramURLString
        }
        if let youtubeURLString {
            publicConfig.collaborationItems[index].youtubeURLString = youtubeURLString
        }
    }

    func savePublicConfig() async {
        guard isAdmin else {
            showUserToast("Nur Admins koennen die Videography-Daten bearbeiten.", style: .info)
            return
        }

        let sanitizedEquipment = publicConfig.equipmentItems.compactMap { item -> SkydownVideoEquipmentItem? in
            let title = item.title.trimmingCharacters(in: .whitespacesAndNewlines)
            let detail = item.detail.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !title.isEmpty, !detail.isEmpty else { return nil }
            return SkydownVideoEquipmentItem(
                id: item.id,
                title: title,
                detail: detail,
                imageURLString: item.imageURLString?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty
            )
        }
        let sanitizedYouTube = publicConfig.youtubeItems.compactMap { item -> SkydownYouTubeVideoItem? in
            let title = item.title.trimmingCharacters(in: .whitespacesAndNewlines)
            let subtitle = item.subtitle.trimmingCharacters(in: .whitespacesAndNewlines)
            let urlString = item.urlString.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !title.isEmpty, !urlString.isEmpty else { return nil }
            return SkydownYouTubeVideoItem(id: item.id, title: title, subtitle: subtitle, urlString: urlString)
        }
        let sanitizedCollaborations = publicConfig.collaborationItems.compactMap { item -> SkydownProducedWithArtist? in
            let name = item.name.trimmingCharacters(in: .whitespacesAndNewlines)
            let role = item.role.trimmingCharacters(in: .whitespacesAndNewlines)
            let highlight = item.highlight.trimmingCharacters(in: .whitespacesAndNewlines)
            let vibe = item.vibe.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !name.isEmpty, !role.isEmpty else { return nil }
            return SkydownProducedWithArtist(
                id: item.id,
                name: name,
                role: role,
                highlight: highlight,
                vibe: vibe,
                imageURLString: item.imageURLString?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty,
                spotifyArtistID: item.spotifyArtistID?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty,
                instagramURLString: item.instagramURLString?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty,
                youtubeURLString: item.youtubeURLString?.trimmingCharacters(in: .whitespacesAndNewlines).musicNilIfEmpty
            )
        }

        let config = SkydownVideoHubPublicConfig(
            equipmentItems: sanitizedEquipment.isEmpty ? SkydownVideoHubPublicConfig.default.equipmentItems : sanitizedEquipment,
            youtubeItems: sanitizedYouTube,
            collaborationItems: sanitizedCollaborations.isEmpty ? SkydownVideoHubPublicConfig.default.collaborationItems : sanitizedCollaborations
        )

        isSavingPublicConfig = true
        do {
            try await service.savePublicConfig(config, currentUser: currentUser)
            publicConfig = config
            showUserToast("Videography-Daten gespeichert.", style: .success)
        } catch {
            showUserToast("Die Videography-Daten konnten nicht gespeichert werden.", style: .error)
        }
        isSavingPublicConfig = false
    }

    private func resolveSelectedFile(from url: URL) -> SkydownSelectedVideoFile? {
        let hasAccess = url.startAccessingSecurityScopedResource()
        defer {
            if hasAccess {
                url.stopAccessingSecurityScopedResource()
            }
        }

        let resourceValues = try? url.resourceValues(forKeys: [.nameKey, .fileSizeKey, .contentTypeKey])
        let contentType = resourceValues?.contentType ?? UTType(filenameExtension: url.pathExtension) ?? .movie
        let fileName = resourceValues?.name ?? url.lastPathComponent

        guard contentType.conforms(to: .movie) || fileName.lowercased().hasSuffix(".m4v") else {
            return nil
        }

        return SkydownSelectedVideoFile(
            url: url,
            fileName: fileName,
            fileSizeInBytes: Int64(resourceValues?.fileSize ?? 0),
            mimeType: contentType.preferredMIMEType ?? "video/mp4"
        )
    }

    private func observeVideos(isAdmin: Bool) {
        observedAdminState = isAdmin
        isLoadingVideos = true
        allVideos = []
        videos = []
        observationCancellation?()
        observationCancellation = service.observeVideos(isAdmin: isAdmin) { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let videos):
                hasLoadedVideosOnce = true
                allVideos = videos
                applyVisibleVideos()
            case .failure:
                allVideos = []
                isLoadingVideos = false
                applyVisibleVideos()
                if isAdmin || hasLoadedVideosOnce {
                    showUserToast("Die Videos konnten gerade nicht geladen werden.", style: .error)
                }
            }
        }
    }

    private func applyVisibleVideos() {
        videos = isAdmin ? allVideos : allVideos.filter(\.isPublic)
        isLoadingVideos = false
    }

    private func observePublicConfig() {
        configObservationCancellation?()
        configObservationCancellation = service.observePublicConfig { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let config):
                publicConfig = config
            case .failure:
                publicConfig = .default
            }
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}
