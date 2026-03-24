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
                        onChange(.failure(error))
                        return
                    }

                    let orders = snapshot?.documents.compactMap { document in
                        try? document.data(as: Order.self)
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
}
