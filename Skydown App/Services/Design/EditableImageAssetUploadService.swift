import Foundation
import FirebaseAuth
import FirebaseFunctions
import FirebaseStorage
import ImageIO
import PhotosUI
import UniformTypeIdentifiers

protocol EditableImageAssetUploading {
    func uploadImageData(_ data: Data) async throws -> String
    func uploadVideoFile(
        from fileURL: URL,
        fileName: String,
        mimeType: String?
    ) async throws -> String
    func deleteImage(at imageURL: String) async throws
    func deleteAsset(at assetURL: String) async throws
}

final class EditableImageAssetUploadService: EditableImageAssetUploading {
    private let functions: Functions
    private let storage: Storage

    init(
        functions: Functions = .functions(region: "us-central1"),
        storage: Storage = .storage()
    ) {
        self.functions = functions
        self.storage = storage
    }

    func uploadImageData(_ data: Data) async throws -> String {
        guard let userId = Auth.auth().currentUser?.uid else {
            throw NSError(
                domain: "EditableImageAssetUploadService",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "Bitte zuerst anmelden."]
            )
        }

        let fileInfo = detectFileInfo(from: data)
        let slot = try await requestUploadSlot(
            kind: "asset",
            userId: userId,
            mimeType: fileInfo.mimeType,
            fileExtension: fileInfo.fileExtension,
            byteSize: data.count
        )
        let reference = storage.reference().child(slot.storagePath)
        let metadata = StorageMetadata()
        metadata.contentType = fileInfo.mimeType
        metadata.customMetadata = slot.metadata

        try await putData(data, to: reference, metadata: metadata)
        return try await reference.awaitStableDownloadURL().absoluteString
    }

    func uploadVideoFile(
        from fileURL: URL,
        fileName: String,
        mimeType: String?
    ) async throws -> String {
        guard let userId = Auth.auth().currentUser?.uid else {
            throw NSError(
                domain: "EditableImageAssetUploadService",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "Bitte zuerst anmelden."]
            )
        }

        let fileInfo = detectVideoFileInfo(fileURL: fileURL, fallbackMimeType: mimeType)
        let slot = try await requestUploadSlot(
            kind: "asset",
            userId: userId,
            mimeType: fileInfo.mimeType,
            fileExtension: fileInfo.fileExtension,
            byteSize: fileInfo.byteSize
        )
        let reference = storage.reference().child(slot.storagePath)
        let metadata = StorageMetadata()
        metadata.contentType = fileInfo.mimeType
        metadata.customMetadata = slot.metadata.merging(
            ["originalFilename": fileName],
            uniquingKeysWith: { _, new in new }
        )

        let stagedURL = try stageSecurityScopedAssetFile(
            from: fileURL,
            fileName: fileName
        )
        defer {
            try? FileManager.default.removeItem(at: stagedURL)
        }

        try await putFile(stagedURL, to: reference, metadata: metadata)
        return try await reference.awaitStableDownloadURL().absoluteString
    }

    func deleteImage(at imageURL: String) async throws {
        try await deleteAsset(at: imageURL)
    }

    func deleteAsset(at assetURL: String) async throws {
        let trimmedURL = assetURL.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedURL.isEmpty else { return }
        guard let userId = Auth.auth().currentUser?.uid else { return }

        guard let reference = ownedAssetReferenceIfPossible(for: trimmedURL, userId: userId) else {
            return
        }

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            reference.delete { error in
                if let nsError = error as NSError?,
                   nsError.domain == StorageErrorDomain,
                   StorageErrorCode(rawValue: nsError.code) == .objectNotFound {
                    continuation.resume(returning: ())
                    return
                }

                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    private func requestUploadSlot(
        kind: String,
        userId: String,
        mimeType: String,
        fileExtension: String,
        byteSize: Int
    ) async throws -> EditableImageAssetUploadSlot {
        let response = try await functions.invokeCallable("requestUploadSlot", payload: [
                "userId": userId,
                "kind": kind,
                "mimeType": mimeType,
                "fileExtension": fileExtension,
                "byteSize": byteSize
            ])

        guard let data = response.data as? [String: Any] else {
            throw NSError(
                domain: "EditableImageAssetUploadService",
                code: 500,
                userInfo: [NSLocalizedDescriptionKey: "Upload-Freigabe konnte nicht gelesen werden."]
            )
        }

        let allowed = data["allowed"] as? Bool ?? false
        if !allowed {
            throw NSError(
                domain: "EditableImageAssetUploadService",
                code: 403,
                userInfo: [NSLocalizedDescriptionKey: (data["message"] as? String) ?? "Upload wurde abgelehnt."]
            )
        }

        guard let slotId = data["slotId"] as? String,
              let storagePath = data["storagePath"] as? String else {
            throw NSError(
                domain: "EditableImageAssetUploadService",
                code: 500,
                userInfo: [NSLocalizedDescriptionKey: "Upload-Slot ist unvollstaendig."]
            )
        }

        return EditableImageAssetUploadSlot(
            slotId: slotId,
            storagePath: storagePath,
            metadata: data["metadata"] as? [String: String] ?? [:]
        )
    }

    private func putData(
        _ data: Data,
        to reference: StorageReference,
        metadata: StorageMetadata
    ) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            reference.putData(data, metadata: metadata) { _, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    private func putFile(
        _ fileURL: URL,
        to reference: StorageReference,
        metadata: StorageMetadata
    ) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            reference.putFile(from: fileURL, metadata: metadata) { _, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    private func ownedAssetReferenceIfPossible(
        for imageURL: String,
        userId: String
    ) -> StorageReference? {
        if imageURL.hasPrefix("gs://") {
            let reference = storage.reference(forURL: imageURL)
            return reference.fullPath.hasPrefix("users/\(userId)/assets/") ? reference : nil
        }

        guard let url = URL(string: imageURL),
              let host = url.host?.lowercased(),
              host.contains("firebasestorage.googleapis.com") else {
            return nil
        }

        let reference = storage.reference(forURL: imageURL)
        return reference.fullPath.hasPrefix("users/\(userId)/assets/") ? reference : nil
    }

    private func detectFileInfo(from data: Data) -> EditableImageAssetFileInfo {
        if data.count >= 8,
           data.starts(with: [0x89, 0x50, 0x4E, 0x47]) {
            return EditableImageAssetFileInfo(mimeType: "image/png", fileExtension: "png", byteSize: data.count)
        }

        if data.count >= 12,
           data.prefix(4).elementsEqual("RIFF".utf8),
           data.dropFirst(8).prefix(4).elementsEqual("WEBP".utf8) {
            return EditableImageAssetFileInfo(mimeType: "image/webp", fileExtension: "webp", byteSize: data.count)
        }

        if data.count >= 2,
           data.starts(with: [0xFF, 0xD8]) {
            return EditableImageAssetFileInfo(mimeType: "image/jpeg", fileExtension: "jpg", byteSize: data.count)
        }

        return EditableImageAssetFileInfo(mimeType: "image/jpeg", fileExtension: "jpg", byteSize: data.count)
    }

    private func detectVideoFileInfo(
        fileURL: URL,
        fallbackMimeType: String?
    ) -> EditableImageAssetFileInfo {
        let resourceValues = try? fileURL.resourceValues(forKeys: [.contentTypeKey, .fileSizeKey])
        let contentType = resourceValues?.contentType ?? UTType(filenameExtension: fileURL.pathExtension)
        let mimeType = fallbackMimeType?.trimmedNilIfEmpty
            ?? contentType?.preferredMIMEType
            ?? "video/mp4"
        let fileExtension = contentType?.preferredFilenameExtension
            ?? fileURL.pathExtension.trimmedNilIfEmpty
            ?? "mp4"
        let byteSize = resourceValues?.fileSize ?? ((try? Data(contentsOf: fileURL, options: [.mappedIfSafe]).count) ?? 0)
        return EditableImageAssetFileInfo(
            mimeType: mimeType,
            fileExtension: fileExtension,
            byteSize: byteSize
        )
    }
}

private struct EditableImageAssetUploadSlot {
    let slotId: String
    let storagePath: String
    let metadata: [String: String]
}

private struct EditableImageAssetFileInfo {
    let mimeType: String
    let fileExtension: String
    let byteSize: Int
}

enum PickedImageUploadPreparation {
    static func stableTemporaryImageURL(from itemProvider: NSItemProvider) async throws -> URL {
        let typeIdentifier = itemProvider.registeredTypeIdentifiers.first {
            UTType($0)?.conforms(to: .image) == true
        } ?? UTType.image.identifier

        do {
            if let fileURL = try await loadImageFileURL(from: itemProvider, typeIdentifier: typeIdentifier) {
                return fileURL
            }
        } catch {
            // Fall through to data representation as a compatibility fallback.
        }

        let rawData = try await loadImageDataRepresentation(from: itemProvider, typeIdentifier: typeIdentifier)
        let fileURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("jpg")
        try rawData.write(to: fileURL, options: [.atomic])
        return fileURL
    }

    static func normalizedJPEGData(
        from itemProvider: NSItemProvider,
        maxPixelSize: Int = 2048,
        compressionQuality: Double = 0.82
    ) async throws -> Data {
        let fileURL = try await stableTemporaryImageURL(from: itemProvider)
        defer { try? FileManager.default.removeItem(at: fileURL) }
        return try await normalizedJPEGData(
            fromTemporaryFileURL: fileURL,
            maxPixelSize: maxPixelSize,
            compressionQuality: compressionQuality
        )
    }

    static func normalizedJPEGData(
        fromTemporaryFileURL fileURL: URL,
        maxPixelSize: Int = 2048,
        compressionQuality: Double = 0.82
    ) async throws -> Data {
        try await normalizedJPEGDataOffMain(
            fromFileURL: fileURL,
            maxPixelSize: maxPixelSize,
            compressionQuality: compressionQuality
        )
    }

    private static func normalizedJPEGDataOffMain(
        from rawData: Data,
        maxPixelSize: Int,
        compressionQuality: Double
    ) async throws -> Data {
        try await Task.detached(priority: .userInitiated) {
            try autoreleasepool {
                try normalizedJPEGData(
                    from: rawData,
                    maxPixelSize: maxPixelSize,
                    compressionQuality: compressionQuality
                )
            }
        }.value
    }

    private static func normalizedJPEGDataOffMain(
        fromFileURL fileURL: URL,
        maxPixelSize: Int,
        compressionQuality: Double
    ) async throws -> Data {
        try await Task.detached(priority: .userInitiated) {
            try autoreleasepool {
                try normalizedJPEGData(
                    fromFileURL: fileURL,
                    maxPixelSize: maxPixelSize,
                    compressionQuality: compressionQuality
                )
            }
        }.value
    }

    static func normalizedJPEGData(
        from rawData: Data,
        maxPixelSize: Int = 2048,
        compressionQuality: Double = 0.82
    ) throws -> Data {
        guard let source = CGImageSourceCreateWithData(
            rawData as CFData,
            [kCGImageSourceShouldCache: false] as CFDictionary
        ) else {
            return rawData
        }

        return try normalizedJPEGData(
            from: source,
            maxPixelSize: maxPixelSize,
            compressionQuality: compressionQuality
        )
    }

    private static func normalizedJPEGData(
        fromFileURL fileURL: URL,
        maxPixelSize: Int,
        compressionQuality: Double
    ) throws -> Data {
        guard let source = CGImageSourceCreateWithURL(
            fileURL as CFURL,
            [kCGImageSourceShouldCache: false] as CFDictionary
        ) else {
            let rawData = try Data(contentsOf: fileURL, options: [.mappedIfSafe])
            return try normalizedJPEGData(
                from: rawData,
                maxPixelSize: maxPixelSize,
                compressionQuality: compressionQuality
            )
        }

        return try normalizedJPEGData(
            from: source,
            maxPixelSize: maxPixelSize,
            compressionQuality: compressionQuality
        )
    }

    private static func normalizedJPEGData(
        from source: CGImageSource,
        maxPixelSize: Int,
        compressionQuality: Double
    ) throws -> Data {

        let options: CFDictionary = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceShouldCache: false,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize
        ] as CFDictionary

        guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options)
            ?? CGImageSourceCreateImageAtIndex(
                source,
                0,
                [kCGImageSourceShouldCache: false] as CFDictionary
            )
        else {
            throw NSError(
                domain: "PickedImageUploadPreparation",
                code: 500,
                userInfo: [NSLocalizedDescriptionKey: "Bild konnte nicht vorbereitet werden."]
            )
        }

        let destinationData = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(
            destinationData,
            UTType.jpeg.identifier as CFString,
            1,
            nil
        ) else {
            throw NSError(
                domain: "PickedImageUploadPreparation",
                code: 500,
                userInfo: [NSLocalizedDescriptionKey: "Bild konnte nicht vorbereitet werden."]
            )
        }

        let destinationOptions: CFDictionary = [
            kCGImageDestinationLossyCompressionQuality: compressionQuality
        ] as CFDictionary
        CGImageDestinationAddImage(destination, image, destinationOptions)

        guard CGImageDestinationFinalize(destination) else {
            throw NSError(
                domain: "PickedImageUploadPreparation",
                code: 500,
                userInfo: [NSLocalizedDescriptionKey: "Bild konnte nicht optimiert werden."]
            )
        }

        return destinationData as Data
    }

    private static func loadImageFileURL(
        from itemProvider: NSItemProvider,
        typeIdentifier: String
    ) async throws -> URL? {
        try await withCheckedThrowingContinuation { continuation in
            itemProvider.loadFileRepresentation(forTypeIdentifier: typeIdentifier) { fileURL, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let fileURL else {
                    continuation.resume(returning: nil)
                    return
                }

                let fileManager = FileManager.default
                let fileExtension = fileURL.pathExtension.isEmpty ? "img" : fileURL.pathExtension
                let copiedURL = fileManager.temporaryDirectory
                    .appendingPathComponent(UUID().uuidString)
                    .appendingPathExtension(fileExtension)

                do {
                    if fileManager.fileExists(atPath: copiedURL.path) {
                        try fileManager.removeItem(at: copiedURL)
                    }
                    try fileManager.copyItem(at: fileURL, to: copiedURL)
                    continuation.resume(returning: copiedURL)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    private static func loadImageDataRepresentation(
        from itemProvider: NSItemProvider,
        typeIdentifier: String
    ) async throws -> Data {
        try await withCheckedThrowingContinuation { continuation in
            itemProvider.loadDataRepresentation(forTypeIdentifier: typeIdentifier) { data, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let data {
                    continuation.resume(returning: data)
                } else {
                    continuation.resume(
                        throwing: NSError(
                            domain: "PickedImageUploadPreparation",
                            code: 500,
                            userInfo: [NSLocalizedDescriptionKey: "Bild konnte nicht geladen werden."]
                        )
                    )
                }
            }
        }
    }
}

private func stageSecurityScopedAssetFile(
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
        .appendingPathComponent("asset-upload-staging", isDirectory: true)
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

private extension String {
    var trimmedNilIfEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
