import Foundation
import FirebaseFirestore
import FirebaseFunctions

protocol OrderServicing {
    func observeOrders(_ onChange: @escaping @MainActor (Result<[Order], Error>) -> Void) -> () -> Void
    func submitOrder(
        userEmail: String,
        customerName: String,
        customerEmail: String,
        whatsApp: String,
        shippingAddress: String,
        shippingAddressData: ShippingAddressData,
        shippingZone: String,
        shippingCountryCode: String,
        message: String,
        items: [CartItem],
        paymentMethod: String?,
        paymentStatus: String,
        subtotalAmount: Double,
        shippingAmount: Double,
        taxRate: Double,
        taxAmount: Double,
        totalAmount: Double,
        fulfillmentProvider: String
    ) async throws -> String
    func confirmPayment(
        orderID: String,
        paymentMethod: String?,
        paymentReference: String?
    ) async throws
    func toggleCompleted(orderID: String, isCompleted: Bool) async throws
    func deleteOrder(orderID: String) async throws
}

final class FirebaseOrderService: OrderServicing {
    private let firestore: Firestore
    private let functions: Functions

    init(
        firestore: Firestore = Firestore.firestore(),
        functions: Functions = Functions.functions(region: "us-central1")
    ) {
        self.firestore = firestore
        self.functions = functions
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
                                userInfo: [NSLocalizedDescriptionKey: "Keine Berechtigung zum Laden der Bestellungen. Pruefe Owner-Rechte und die Firestore Rules."]
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

    func submitOrder(
        userEmail: String,
        customerName: String,
        customerEmail: String,
        whatsApp: String,
        shippingAddress: String,
        shippingAddressData: ShippingAddressData,
        shippingZone: String,
        shippingCountryCode: String,
        message: String,
        items: [CartItem],
        paymentMethod: String?,
        paymentStatus: String,
        subtotalAmount: Double,
        shippingAmount: Double,
        taxRate: Double,
        taxAmount: Double,
        totalAmount: Double,
        fulfillmentProvider: String
    ) async throws -> String {
        let orderItems = items.map { item in
            [
                "productId": item.item.id as Any,
                "name": item.item.name,
                "quantity": item.quantity,
                "size": item.size,
                "color": item.color as Any,
                "shopifyVariantId": item.shopifyVariantId as Any,
                "sku": item.sku as Any,
                "unitPrice": item.unitPrice ?? item.item.price
            ]
        }

        let payload: [String: Any] = [
            "userEmail": userEmail,
            "customerName": customerName,
            "customerEmail": customerEmail,
            "whatsApp": whatsApp,
            "shippingAddress": shippingAddress,
            "shippingAddressData": [
                "address1": shippingAddressData.address1,
                "address2": shippingAddressData.address2,
                "city": shippingAddressData.city,
                "zip": shippingAddressData.zip,
                "countryCode": shippingAddressData.countryCode,
                "countryName": shippingAddressData.countryName
            ],
            "shippingZone": shippingZone,
            "shippingCountryCode": shippingCountryCode,
            "paymentMethod": paymentMethod ?? "",
            "paymentStatus": paymentStatus,
            "subtotalAmount": subtotalAmount,
            "shippingAmount": shippingAmount,
            "shippingPriceCharged": shippingAmount,
            "taxRate": taxRate,
            "taxAmount": taxAmount,
            "totalAmount": totalAmount,
            "fulfillmentProvider": fulfillmentProvider,
            "message": message,
            "items": orderItems
        ]
        let result = try await functions
            .httpsCallable("submitMerchOrder")
            .call(payload)

        if let data = result.data as? [String: Any],
           let orderID = data["orderId"] as? String,
           !orderID.isEmpty {
            return orderID
        }

        if let orderID = result.data as? String, !orderID.isEmpty {
            return orderID
        }

        throw NSError(
            domain: "OrderService",
            code: 500,
            userInfo: [NSLocalizedDescriptionKey: "Die Bestellung konnte serverseitig nicht angelegt werden."]
        )
    }

    func confirmPayment(
        orderID: String,
        paymentMethod: String?,
        paymentReference: String?
    ) async throws {
        var payload: [String: Any] = [
            "orderId": orderID
        ]

        if let paymentMethod = paymentMethod?.takeIfNotBlank() {
            payload["paymentMethod"] = paymentMethod
        }

        if let paymentReference = paymentReference?.takeIfNotBlank() {
            payload["paymentReference"] = paymentReference
        }

        _ = try await functions
            .httpsCallable("confirmMerchOrderPayment")
            .call(payload)
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
                size: itemData["size"] as? String,
                color: itemData["color"] as? String,
                productId: itemData["productId"] as? String,
                shopifyVariantId: itemData["shopifyVariantId"] as? String,
                sku: itemData["sku"] as? String,
                unitPrice: itemData["unitPrice"] as? Double ?? (itemData["unitPrice"] as? NSNumber)?.doubleValue
            )
        }

        return Order(
            id: document.documentID,
            userEmail: userEmail,
            customerName: data["customerName"] as? String,
            customerEmail: data["customerEmail"] as? String,
            whatsApp: data["whatsApp"] as? String,
            shippingAddress: data["shippingAddress"] as? String,
            shippingAddressData: data["shippingAddressData"].toShippingAddressData(),
            shippingZone: data["shippingZone"] as? String,
            shippingCountryCode: data["shippingCountryCode"] as? String,
            paymentMethod: (data["paymentMethod"] as? String)?.takeIfNotBlank(),
            paymentStatus: data["paymentStatus"] as? String,
            subtotalAmount: data["subtotalAmount"] as? Double ?? (data["subtotalAmount"] as? NSNumber)?.doubleValue,
            shippingAmount: data["shippingAmount"] as? Double ?? (data["shippingAmount"] as? NSNumber)?.doubleValue,
            shippingPriceCharged: data["shippingPriceCharged"] as? Double ?? (data["shippingPriceCharged"] as? NSNumber)?.doubleValue,
            taxRate: data["taxRate"] as? Double ?? (data["taxRate"] as? NSNumber)?.doubleValue,
            taxAmount: data["taxAmount"] as? Double ?? (data["taxAmount"] as? NSNumber)?.doubleValue,
            totalAmount: data["totalAmount"] as? Double ?? (data["totalAmount"] as? NSNumber)?.doubleValue,
            fulfillmentProvider: data["fulfillmentProvider"] as? String,
            fulfillmentStatus: data["fulfillmentStatus"] as? String,
            shopifyOrderId: data["shopifyOrderId"] as? String,
            shopifyOrderName: data["shopifyOrderName"] as? String,
            shopifySyncStatus: data["shopifySyncStatus"] as? String,
            message: data["message"] as? String,
            items: items,
            isCompleted: isCompleted,
            timestamp: timestamp
        )
    }
}

private extension String {
    func takeIfNotBlank() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

private extension Any? {
    func toShippingAddressData() -> ShippingAddressData? {
        guard let data = self as? [String: Any],
              let address1 = data["address1"] as? String,
              let city = data["city"] as? String,
              let zip = data["zip"] as? String,
              let countryCode = data["countryCode"] as? String,
              let countryName = data["countryName"] as? String else {
            return nil
        }

        return ShippingAddressData(
            address1: address1,
            address2: data["address2"] as? String ?? "",
            city: city,
            zip: zip,
            countryCode: countryCode,
            countryName: countryName
        )
    }
}
