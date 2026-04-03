import Foundation
import FirebaseAuth
import FirebaseFunctions
import FirebaseStorage
import ImageIO
import PhotosUI
import UniformTypeIdentifiers

protocol EditableImageAssetUploading {
    func uploadImageData(_ data: Data) async throws -> String
    func deleteImage(at imageURL: String) async throws
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

    func deleteImage(at imageURL: String) async throws {
        let trimmedURL = imageURL.trimmingCharacters(in: .whitespacesAndNewlines)
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
        let response = try await functions
            .httpsCallable("requestUploadSlot")
            .call([
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
            return EditableImageAssetFileInfo(mimeType: "image/png", fileExtension: "png")
        }

        if data.count >= 12,
           data.prefix(4).elementsEqual("RIFF".utf8),
           data.dropFirst(8).prefix(4).elementsEqual("WEBP".utf8) {
            return EditableImageAssetFileInfo(mimeType: "image/webp", fileExtension: "webp")
        }

        if data.count >= 2,
           data.starts(with: [0xFF, 0xD8]) {
            return EditableImageAssetFileInfo(mimeType: "image/jpeg", fileExtension: "jpg")
        }

        return EditableImageAssetFileInfo(mimeType: "image/jpeg", fileExtension: "jpg")
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
}

enum PickedImageUploadPreparation {
    static func normalizedJPEGData(
        from itemProvider: NSItemProvider,
        maxPixelSize: Int = 2048,
        compressionQuality: Double = 0.82
    ) async throws -> Data {
        let typeIdentifier = itemProvider.registeredTypeIdentifiers.first {
            UTType($0)?.conforms(to: .image) == true
        } ?? UTType.image.identifier

        do {
            if let fileURL = try await loadImageFileURL(from: itemProvider, typeIdentifier: typeIdentifier) {
                defer { try? FileManager.default.removeItem(at: fileURL) }
                return try await normalizedJPEGDataOffMain(
                    fromFileURL: fileURL,
                    maxPixelSize: maxPixelSize,
                    compressionQuality: compressionQuality
                )
            }
        } catch {
            // Fall through to data representation as a compatibility fallback.
        }

        let rawData = try await loadImageDataRepresentation(from: itemProvider, typeIdentifier: typeIdentifier)
        return try await normalizedJPEGDataOffMain(
            from: rawData,
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
