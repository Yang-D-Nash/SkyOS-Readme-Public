import Foundation
import FirebaseFirestore

protocol MerchandiseServicing {
    func observeItems(_ onChange: @escaping @MainActor (Result<[MerchandiseItem], Error>) -> Void) -> () -> Void
    func addItem(_ item: MerchandiseItem) async throws
    func updatePrice(itemID: String, newPrice: Double) async throws
    func deleteItem(itemID: String) async throws
}

final class FirebaseMerchandiseService: MerchandiseServicing {
    private let firestore: Firestore

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
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

    func updatePrice(itemID: String, newPrice: Double) async throws {
        try await firestore.collection("merchandise").document(itemID).updateData([
            "price": newPrice
        ])
    }

    func deleteItem(itemID: String) async throws {
        try await firestore.collection("merchandise").document(itemID).delete()
    }
}
