import Foundation
import FirebaseAuth
import FirebaseFirestore
import FirebaseFunctions
import FirebaseStorage

protocol UserProfileServicing {
    @discardableResult
    func observeGallery(
        for userId: String,
        onChange: @escaping (Result<[ProfileGalleryItem], Error>) -> Void
    ) -> ListenerRegistration
    func uploadAvatar(userId: String, imageData: Data) async throws -> String
    func uploadGalleryData(
        userId: String,
        data: Data,
        type: ProfileMediaType,
        fileExtension: String,
        mimeType: String?,
        title: String,
        caption: String?
    ) async throws
    func uploadGalleryFile(
        userId: String,
        fileURL: URL,
        type: ProfileMediaType,
        mimeType: String?,
        title: String,
        caption: String?
    ) async throws
}

final class FirebaseUserProfileService: UserProfileServicing {
    private let firestore: Firestore
    private let storage: Storage
    private let functions: Functions

    init(
        firestore: Firestore = .firestore(),
        storage: Storage = .storage(),
        functions: Functions = .functions(region: "us-central1")
    ) {
        self.firestore = firestore
        self.storage = storage
        self.functions = functions
    }

    @discardableResult
    func observeGallery(
        for userId: String,
        onChange: @escaping (Result<[ProfileGalleryItem], Error>) -> Void
    ) -> ListenerRegistration {
        guard Auth.auth().currentUser?.uid == userId else {
            onChange(.success([]))
            return NoopListenerRegistration()
        }

        return firestore
            .collection("galleryMeta")
            .document(userId)
            .collection("items")
            .order(by: "createdAt", descending: true)
            .addSnapshotListener { snapshot, error in
                if let error {
                    let nsError = error as NSError
                    let firestoreCode = FirestoreErrorCode.Code(rawValue: nsError.code)
                    if nsError.domain == FirestoreErrorDomain,
                       firestoreCode == .permissionDenied {
                        onChange(.success([]))
                        return
                    }
                    onChange(.failure(error))
                    return
                }

                let items: [ProfileGalleryItem] = snapshot?.documents.compactMap { document in
                    let data = document.data()
                    guard
                        let mediaURL = data["mediaURL"] as? String,
                        let type = data["type"] as? String,
                        let title = data["title"] as? String
                    else {
                        return nil
                    }

                    let createdAt = (data["createdAt"] as? Timestamp)?.dateValue() ?? .now
                    return ProfileGalleryItem(
                        id: document.documentID,
                        ownerId: data["ownerUid"] as? String ?? userId,
                        type: type,
                        title: title,
                        caption: data["caption"] as? String,
                        mediaURL: mediaURL,
                        thumbnailURL: data["thumbnailURL"] as? String,
                        createdAt: createdAt
                    )
                } ?? []
                onChange(.success(items))
            }
    }

    func uploadAvatar(userId: String, imageData: Data) async throws -> String {
        let slot = try await requestUploadSlot(
            kind: "profile",
            userId: userId,
            mimeType: "image/jpeg",
            fileExtension: "jpg",
            byteSize: imageData.count
        )
        let avatarReference = storage.reference()
            .child(slot.storagePath)
        let metadata = StorageMetadata()
        metadata.contentType = "image/jpeg"
        metadata.customMetadata = slot.metadata

        try await putData(imageData, to: avatarReference, metadata: metadata)
        let downloadURL = try await avatarReference.awaitStableDownloadURL()
        let now = Timestamp(date: .now)
        let userSnapshot = try await firestore.collection("users").document(userId).getDocument()
        let username = (userSnapshot.data()?["username"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines)
            .nilIfBlank
            ?? "Skydown User"
        let profileSnapshot = try await firestore.collection("userProfiles").document(userId).getDocument()
        let createdAt = (profileSnapshot.data()?["createdAt"] as? Timestamp) ?? now

        try await firestore.collection("users").document(userId).setData(
            [
                "profileImageURL": downloadURL.absoluteString,
                "profileImagePath": slot.storagePath
            ],
            merge: true
        )
        try await firestore.collection("userProfiles").document(userId).setData(
            [
                "ownerUid": userId,
                "username": username,
                "profileImageURL": downloadURL.absoluteString,
                "profileImagePath": slot.storagePath,
                "updatedAt": now,
                "createdAt": createdAt
            ],
            merge: true
        )

        return downloadURL.absoluteString
    }

    func uploadGalleryData(
        userId: String,
        data: Data,
        type: ProfileMediaType,
        fileExtension: String,
        mimeType: String?,
        title: String,
        caption: String?
    ) async throws {
        guard type == .image else {
            throw NSError(
                domain: "FirebaseUserProfileService",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Im Testbetrieb sind aktuell nur Bilder aktiviert."]
            )
        }

        let slot = try await requestUploadSlot(
            kind: "gallery",
            userId: userId,
            mimeType: mimeType ?? "image/jpeg",
            fileExtension: fileExtension,
            byteSize: data.count
        )
        let reference = storage.reference()
            .child(slot.storagePath)
        let metadata = StorageMetadata()
        metadata.contentType = mimeType ?? "image/jpeg"
        metadata.customMetadata = slot.metadata

        try await putData(data, to: reference, metadata: metadata)
        try await persistGalleryEntry(
            userId: userId,
            itemId: slot.slotId,
            reference: reference,
            type: type,
            title: title,
            caption: caption,
            storagePath: slot.storagePath,
            contentType: metadata.contentType ?? "image/jpeg"
        )
    }

    func uploadGalleryFile(
        userId: String,
        fileURL: URL,
        type: ProfileMediaType,
        mimeType: String?,
        title: String,
        caption: String?
    ) async throws {
        guard type == .image else {
            throw NSError(
                domain: "FirebaseUserProfileService",
                code: 400,
                userInfo: [NSLocalizedDescriptionKey: "Im Testbetrieb sind aktuell nur Bilder aktiviert."]
            )
        }

        let ext = fileURL.pathExtension.isEmpty ? fallbackFileExtension(for: type) : fileURL.pathExtension
        let slot = try await requestUploadSlot(
            kind: "gallery",
            userId: userId,
            mimeType: mimeType ?? "image/jpeg",
            fileExtension: ext,
            byteSize: fileSize(for: fileURL)
        )
        let reference = storage.reference()
            .child(slot.storagePath)
        let metadata = StorageMetadata()
        metadata.contentType = mimeType ?? "image/jpeg"
        metadata.customMetadata = slot.metadata

        try await putFile(fileURL, to: reference, metadata: metadata)
        try await persistGalleryEntry(
            userId: userId,
            itemId: slot.slotId,
            reference: reference,
            type: type,
            title: title,
            caption: caption,
            storagePath: slot.storagePath,
            contentType: metadata.contentType ?? "image/jpeg"
        )
    }

    private func persistGalleryEntry(
        userId: String,
        itemId: String,
        reference: StorageReference,
        type: ProfileMediaType,
        title: String,
        caption: String?,
        storagePath: String,
        contentType: String
    ) async throws {
        let downloadURL = try await reference.awaitStableDownloadURL()
        var item: [String: Any] = [
            "ownerUid": userId,
            "type": type.rawValue,
            "title": title,
            "mediaURL": downloadURL.absoluteString,
            "thumbnailURL": downloadURL.absoluteString,
            "storagePath": storagePath,
            "contentType": contentType,
            "createdAt": Timestamp(date: .now),
            "updatedAt": Timestamp(date: .now)
        ]
        if let trimmedCaption = caption?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfBlank {
            item["caption"] = trimmedCaption
        }

        try await firestore.collection("galleryMeta")
            .document(userId)
            .collection("items")
            .document(itemId)
            .setData(item, merge: true)
    }

    private func fallbackFileExtension(for type: ProfileMediaType) -> String {
        switch type {
        case .image:
            return "jpg"
        }
    }

    private func requestUploadSlot(
        kind: String,
        userId: String,
        mimeType: String,
        fileExtension: String,
        byteSize: Int
    ) async throws -> ProfileUploadSlot {
        let result = try await functions
            .httpsCallable("requestUploadSlot")
            .call([
                "kind": kind,
                "userId": userId,
                "mimeType": mimeType,
                "fileExtension": fileExtension,
                "byteSize": byteSize
            ])

        guard
            let data = result.data as? [String: Any],
            let allowed = data["allowed"] as? Bool
        else {
            throw NSError(
                domain: "FirebaseUserProfileService",
                code: 500,
                userInfo: [NSLocalizedDescriptionKey: "Upload-Freigabe konnte nicht gelesen werden."]
            )
        }

        if !allowed {
            let message = (data["message"] as? String)?.nilIfBlank ?? "Upload wurde abgelehnt."
            throw NSError(
                domain: "FirebaseUserProfileService",
                code: 403,
                userInfo: [NSLocalizedDescriptionKey: message]
            )
        }

        guard
            let slotId = data["slotId"] as? String,
            let storagePath = data["storagePath"] as? String
        else {
            throw NSError(
                domain: "FirebaseUserProfileService",
                code: 500,
                userInfo: [NSLocalizedDescriptionKey: "Upload-Slot ist unvollstaendig."]
            )
        }

        let metadata = (data["metadata"] as? [String: Any])?
            .compactMapValues { value in
                value as? String
            } ?? [:]
        return ProfileUploadSlot(slotId: slotId, storagePath: storagePath, metadata: metadata)
    }

    private func fileSize(for fileURL: URL) -> Int {
        let values = try? fileURL.resourceValues(forKeys: [.fileSizeKey])
        return values?.fileSize ?? 0
    }

    private func putData(
        _ data: Data,
        to reference: StorageReference,
        metadata: StorageMetadata?
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
        metadata: StorageMetadata?
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

}

private final class NoopListenerRegistration: NSObject, ListenerRegistration {
    func remove() {}
}

private struct ProfileUploadSlot {
    let slotId: String
    let storagePath: String
    let metadata: [String: String]
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
