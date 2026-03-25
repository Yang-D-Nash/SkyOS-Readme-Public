import Foundation
import FirebaseFirestore
import FirebaseStorage

protocol MerchandiseServicing {
    func observeItems(_ onChange: @escaping @MainActor (Result<[MerchandiseItem], Error>) -> Void) -> () -> Void
    func addItem(_ item: MerchandiseItem) async throws
    func uploadImages(_ imageDataList: [Data]) async throws -> [String]
    func updatePrice(itemID: String, newPrice: Double) async throws
    func deleteItem(itemID: String) async throws
}

final class FirebaseMerchandiseService: MerchandiseServicing {
    private let firestore: Firestore
    private let storage: Storage

    init(
        firestore: Firestore = Firestore.firestore(),
        storage: Storage = Storage.storage()
    ) {
        self.firestore = firestore
        self.storage = storage
    }

    func observeItems(_ onChange: @escaping @MainActor (Result<[MerchandiseItem], Error>) -> Void) -> () -> Void {
        let listener = firestore.collection("merchandise").addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                let items = snapshot?.documents.compactMap { document in
                    try? document.data(as: MerchandiseItem.self)
                }.sorted { $0.name < $1.name } ?? []

                onChange(.success(items))
            }
        }

        return {
            listener.remove()
        }
    }

    func addItem(_ item: MerchandiseItem) async throws {
        _ = try firestore.collection("merchandise").addDocument(from: item)
    }

    func uploadImages(_ imageDataList: [Data]) async throws -> [String] {
        try await withThrowingTaskGroup(of: (Int, String).self) { taskGroup in
            for (index, imageData) in imageDataList.enumerated() {
                taskGroup.addTask { [storage] in
                    let fileName = UUID().uuidString + ".jpg"
                    let reference = storage.reference().child("merchandise/\(fileName)")
                    let metadata = StorageMetadata()
                    metadata.contentType = "image/jpeg"

                    _ = try await reference.putDataAsync(imageData, metadata: metadata)
                    let downloadURL = try await reference.downloadURL()
                    return (index, downloadURL.absoluteString)
                }
            }

            var indexedURLs: [(Int, String)] = []
            for try await result in taskGroup {
                indexedURLs.append(result)
            }

            return indexedURLs
                .sorted { $0.0 < $1.0 }
                .map(\.1)
        }
    }

    func updatePrice(itemID: String, newPrice: Double) async throws {
        try await firestore.collection("merchandise").document(itemID).updateData([
            "price": newPrice
        ])
    }

    func deleteItem(itemID: String) async throws {
        try await firestore.collection("merchandise").document(itemID).delete()
    }
}
