import Foundation
import FirebaseFirestore

protocol OrderServicing {
    func observeOrders(_ onChange: @escaping @MainActor (Result<[Order], Error>) -> Void) -> () -> Void
    func submitOrder(userEmail: String, items: [CartItem]) async throws
    func toggleCompleted(orderID: String, isCompleted: Bool) async throws
    func deleteOrder(orderID: String) async throws
}

final class FirebaseOrderService: OrderServicing {
    private let firestore: Firestore

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func observeOrders(_ onChange: @escaping @MainActor (Result<[Order], Error>) -> Void) -> () -> Void {
        let listener = firestore.collection("orders")
            .order(by: "timestamp", descending: true)
            .addSnapshotListener { snapshot, error in
                Task { @MainActor in
                    if let error {
                        let nsError = error as NSError
                        if nsError.domain == FirestoreErrorDomain,
                           nsError.code == FirestoreErrorCode.permissionDenied.rawValue {
                            onChange(.failure(NSError(
                                domain: "Firestore",
                                code: FirestoreErrorCode.permissionDenied.rawValue,
                                userInfo: [NSLocalizedDescriptionKey: "Keine Berechtigung zum Laden der Bestellungen. Prüfe isAdmin und die Firestore Rules."]
                            )))
                        } else {
                            onChange(.failure(error))
                        }
                        return
                    }

                    let orders = snapshot?.documents.compactMap { document in
                        self.mapOrder(document: document)
                    } ?? []

                    onChange(.success(orders))
                }
            }

        return {
            listener.remove()
        }
    }

    func submitOrder(userEmail: String, items: [CartItem]) async throws {
        let orderItems = items.map { item in
            [
                "name": item.item.name,
                "quantity": item.quantity,
                "size": item.size
            ]
        }

        let orderData: [String: Any] = [
            "userEmail": userEmail,
            "items": orderItems,
            "isCompleted": false,
            "timestamp": Timestamp()
        ]

        try await firestore.collection("orders").addDocument(data: orderData)
    }

    func toggleCompleted(orderID: String, isCompleted: Bool) async throws {
        try await firestore.collection("orders").document(orderID).updateData([
            "isCompleted": !isCompleted
        ])
    }

    func deleteOrder(orderID: String) async throws {
        try await firestore.collection("orders").document(orderID).delete()
    }

    private func mapOrder(document: QueryDocumentSnapshot) -> Order? {
        let data = document.data()
        guard let userEmail = data["userEmail"] as? String else {
            return nil
        }

        let isCompleted = data["isCompleted"] as? Bool ?? false

        let timestamp: Date
        if let firestoreTimestamp = data["timestamp"] as? Timestamp {
            timestamp = firestoreTimestamp.dateValue()
        } else if let date = data["timestamp"] as? Date {
            timestamp = date
        } else {
            timestamp = .now
        }

        let itemsData = data["items"] as? [[String: Any]] ?? []
        let items: [OrderItem] = itemsData.compactMap { itemData -> OrderItem? in
            guard let name = itemData["name"] as? String else {
                return nil
            }

            return OrderItem(
                name: name,
                quantity: itemData["quantity"] as? Int ?? 1,
                size: itemData["size"] as? String
            )
        }

        return Order(
            id: document.documentID,
            userEmail: userEmail,
            items: items,
            isCompleted: isCompleted,
            timestamp: timestamp
        )
    }
}
