import Foundation
import FirebaseFirestore

struct MerchStoreStatus: Equatable {
    let isOpen: Bool

    static let `default` = MerchStoreStatus(isOpen: true)
}

protocol MerchStoreStatusServicing {
    func observeStatus(_ onChange: @escaping @MainActor (Result<MerchStoreStatus, Error>) -> Void) -> () -> Void
    func updateStoreOpen(_ isOpen: Bool) async throws
}

final class FirestoreMerchStoreStatusService: MerchStoreStatusServicing {
    private let firestore: Firestore
    private let collectionName = "appConfig"
    private let documentName = "merchandiseStore"

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func observeStatus(_ onChange: @escaping @MainActor (Result<MerchStoreStatus, Error>) -> Void) -> () -> Void {
        let listener = firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                let isOpen = snapshot?.data()?["isOpen"] as? Bool ?? true
                onChange(.success(MerchStoreStatus(isOpen: isOpen)))
            }
        }

        return {
            listener.remove()
        }
    }

    func updateStoreOpen(_ isOpen: Bool) async throws {
        try await firestore.collection(collectionName).document(documentName).setData([
            "isOpen": isOpen,
            "updatedAt": FieldValue.serverTimestamp()
        ], merge: true)
    }
}
