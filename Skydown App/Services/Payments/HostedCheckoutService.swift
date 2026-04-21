import Foundation
import FirebaseFunctions

struct HostedCheckoutSession {
    let orderID: String
    let checkoutURL: URL
    let sessionID: String?
}

protocol HostedCheckoutServicing {
    func startCheckout(
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
        paymentMethod: String,
        subtotalAmount: Double,
        shippingAmount: Double,
        taxRate: Double,
        taxAmount: Double,
        totalAmount: Double,
        fulfillmentProvider: String,
        platform: String
    ) async throws -> HostedCheckoutSession
}

final class FirebaseHostedCheckoutService: HostedCheckoutServicing {
    private let functions: Functions

    init(functions: Functions = Functions.functions(region: "us-central1")) {
        self.functions = functions
    }

    func startCheckout(
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
        paymentMethod: String,
        subtotalAmount: Double,
        shippingAmount: Double,
        taxRate: Double,
        taxAmount: Double,
        totalAmount: Double,
        fulfillmentProvider: String,
        platform: String
    ) async throws -> HostedCheckoutSession {
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
            "paymentMethod": paymentMethod,
            "subtotalAmount": subtotalAmount,
            "shippingAmount": shippingAmount,
            "shippingPriceCharged": shippingAmount,
            "taxRate": taxRate,
            "taxAmount": taxAmount,
            "totalAmount": totalAmount,
            "fulfillmentProvider": fulfillmentProvider,
            "message": message,
            "items": orderItems,
            "platform": platform
        ]

        let result = try await functions.invokeCallable("startMerchCheckout", payload: payload)

        guard
            let data = result.data as? [String: Any],
            let orderID = data["orderId"] as? String,
            let checkoutURLString = data["checkoutUrl"] as? String,
            let checkoutURL = URL(string: checkoutURLString)
        else {
            throw NSError(
                domain: "HostedCheckoutService",
                code: 500,
                userInfo: [NSLocalizedDescriptionKey: "Stripe Checkout konnte serverseitig nicht vorbereitet werden."]
            )
        }

        return HostedCheckoutSession(
            orderID: orderID,
            checkoutURL: checkoutURL,
            sessionID: (data["sessionId"] as? String)?.trimmedNonEmpty
        )
    }
}

private extension String {
    var trimmedNonEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
