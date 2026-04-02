import Foundation
import FirebaseAuth
import FirebaseFunctions
import FirebaseStorage

protocol EditableImageAssetUploading {
    func uploadImageData(_ data: Data) async throws -> String
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
        return try await fetchDownloadURL(for: reference).absoluteString
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

    private func fetchDownloadURL(for reference: StorageReference) async throws -> URL {
        try await withCheckedThrowingContinuation { continuation in
            reference.downloadURL { url, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let url {
                    continuation.resume(returning: url)
                } else {
                    continuation.resume(
                        throwing: NSError(
                            domain: "EditableImageAssetUploadService",
                            code: 500,
                            userInfo: [NSLocalizedDescriptionKey: "Download-URL fehlt."]
                        )
                    )
                }
            }
        }
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
