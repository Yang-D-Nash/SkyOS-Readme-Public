//
//  cartViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import Foundation

@MainActor
class CartViewModel: ObservableObject {
    @Published var items: [CartItem] = []
    @Published var userEmail: String = ""
    
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .success

    private let authManager: AuthManager
    private let orderService: OrderServicing
    private let hostedCheckoutService: HostedCheckoutServicing

    init(
        authManager: AuthManager,
        orderService: OrderServicing = FirebaseOrderService(),
        hostedCheckoutService: HostedCheckoutServicing = FirebaseHostedCheckoutService()
    ) {
        self.authManager = authManager
        self.orderService = orderService
        self.hostedCheckoutService = hostedCheckoutService
        self.userEmail = authManager.userSession?.email ?? ""

        Task { [weak self] in
            for await user in authManager.$userSession.values {
                self?.userEmail = user?.email ?? ""
            }
        }
    }

    func addItem(
        _ item: MerchandiseItem,
        size: String,
        color: String? = nil,
        quantity: Int,
        resolvedVariant: MerchandiseVariant? = nil
    ) {
        let normalizedColor = color?.trimmingCharacters(in: .whitespacesAndNewlines).takeIfNotBlank()
        if let index = items.firstIndex(where: {
            $0.item.id == item.id &&
            $0.size == size &&
            $0.color?.lowercased() == normalizedColor?.lowercased()
        }) {
            items[index].quantity += quantity
        } else {
            items.append(
                CartItem(
                    item: item,
                    size: size,
                    color: normalizedColor,
                    quantity: quantity,
                    shopifyVariantId: resolvedVariant?.shopifyVariantId,
                    sku: resolvedVariant?.sku,
                    unitPrice: resolvedVariant?.price
                )
            )
        }
        let colorSuffix = normalizedColor.map { " / \($0)" } ?? ""
        showUserToast("\(item.name) \(size)\(colorSuffix) x\(quantity) in Warenkorb gelegt", style: .success)
    }

    func removeItem(_ cartItem: CartItem) {
        items.removeAll { $0.id == cartItem.id }
    }

    func clearCart() {
        items.removeAll()
    }

    func submitCartAsOrder(
        customerName: String,
        customerEmail: String,
        whatsApp: String,
        shippingAddress: String,
        message: String,
        paymentMethod: String?,
        subtotalAmount: Double,
        shippingAmount: Double,
        taxRate: Double,
        taxAmount: Double,
        totalAmount: Double,
        isCheckoutAvailable: Bool,
    ) async -> Bool {
        guard !items.isEmpty else {
            showUserToast("Warenkorb ist leer.", style: .error)
            return false
        }
        guard isCheckoutAvailable || authManager.userSession?.isPlatformOwner == true else {
            showUserToast("Der Merchandise-Store ist gerade pausiert.", style: .error)
            return false
        }
        guard let email = authManager.userSession?.email else {
            showUserToast("Benutzer nicht angemeldet.", style: .error)
            return false
        }
        if hasMixedFulfillmentProviders {
            showUserToast("Bitte trenne Shopify-Merch und interne Legacy-Artikel in zwei Bestellungen.", style: .error)
            return false
        }

        let countryCode: String
        do {
            countryCode = try ShippingService.resolveCountryCode(from: shippingAddressCountryName(from: shippingAddress))
        } catch {
            showUserToast(error.localizedDescription, style: .error)
            return false
        }

        let subtotal = items.reduce(0.0) { partialResult, cartItem in
            partialResult + cartItem.effectiveUnitPrice * Double(cartItem.quantity)
        }

        let shippingQuote: ShippingQuote
        do {
            shippingQuote = try ShippingService.calculateShippingPrice(
                settings: CommerceSettingsStore.shared.settings.shipping,
                countryCode: countryCode,
                items: items,
                subtotal: subtotal
            )
        } catch {
            showUserToast(error.localizedDescription, style: .error)
            return false
        }

        do {
            let paymentLine = paymentMethod?
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .takeIfNotBlank()
                .map { "Gewuenschte Zahlart: \($0)\n\n" }
                ?? ""
            guard abs(subtotalAmount - subtotal) < 0.01,
                  abs(shippingAmount - shippingQuote.price) < 0.01,
                  abs(totalAmount - (subtotal + shippingQuote.price)) < 0.01 else {
                showUserToast("Die Bestellsumme ist nicht mehr aktuell. Bitte pruefe den Warenkorb noch einmal.", style: .error)
                return false
            }
            _ = try await orderService.submitOrder(
                userEmail: email,
                customerName: customerName,
                customerEmail: customerEmail,
                whatsApp: whatsApp,
                shippingAddress: shippingAddress,
                shippingAddressData: parseShippingAddressData(from: shippingAddress, countryCode: countryCode),
                shippingZone: shippingQuote.zone.rawValue,
                shippingCountryCode: countryCode,
                message: paymentLine + message,
                items: items,
                paymentMethod: paymentMethod,
                paymentStatus: "pending",
                subtotalAmount: subtotal,
                shippingAmount: shippingQuote.price,
                taxRate: taxRate,
                taxAmount: taxAmount,
                totalAmount: subtotal + shippingQuote.price,
                fulfillmentProvider: deriveFulfillmentProvider()
            )
            clearCart()
            showUserToast("Bestellung erfolgreich abgeschickt!", style: .success)
            return true
        } catch {
            skydownDebugLog("Dev Fehler submitCartAsOrder:", error.localizedDescription)
            showUserToast("Fehler beim Absenden der Bestellung.", style: .error)
            return false
        }
    }

    func startHostedCheckout(
        customerName: String,
        customerEmail: String,
        whatsApp: String,
        shippingAddress: String,
        message: String,
        paymentMethod: String,
        subtotalAmount: Double,
        shippingAmount: Double,
        taxRate: Double,
        taxAmount: Double,
        totalAmount: Double,
        isCheckoutAvailable: Bool,
        platform: String = "ios"
    ) async -> HostedCheckoutSession? {
        guard !items.isEmpty else {
            showUserToast("Warenkorb ist leer.", style: .error)
            return nil
        }
        guard isCheckoutAvailable || authManager.userSession?.isPlatformOwner == true else {
            showUserToast("Der Merchandise-Store ist gerade pausiert.", style: .error)
            return nil
        }
        guard let email = authManager.userSession?.email else {
            showUserToast("Benutzer nicht angemeldet.", style: .error)
            return nil
        }
        if hasMixedFulfillmentProviders {
            showUserToast("Bitte trenne Shopify-Merch und interne Legacy-Artikel in zwei Bestellungen.", style: .error)
            return nil
        }

        let countryCode: String
        do {
            countryCode = try ShippingService.resolveCountryCode(from: shippingAddressCountryName(from: shippingAddress))
        } catch {
            showUserToast(error.localizedDescription, style: .error)
            return nil
        }

        let subtotal = items.reduce(0.0) { partialResult, cartItem in
            partialResult + cartItem.effectiveUnitPrice * Double(cartItem.quantity)
        }

        let shippingQuote: ShippingQuote
        do {
            shippingQuote = try ShippingService.calculateShippingPrice(
                settings: CommerceSettingsStore.shared.settings.shipping,
                countryCode: countryCode,
                items: items,
                subtotal: subtotal
            )
        } catch {
            showUserToast(error.localizedDescription, style: .error)
            return nil
        }

        do {
            let paymentLine = paymentMethod
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .takeIfNotBlank()
                .map { "Gewuenschte Zahlart: \($0)\n\n" }
                ?? ""
            guard abs(subtotalAmount - subtotal) < 0.01,
                  abs(shippingAmount - shippingQuote.price) < 0.01,
                  abs(totalAmount - (subtotal + shippingQuote.price)) < 0.01 else {
                showUserToast("Die Bestellsumme ist nicht mehr aktuell. Bitte pruefe den Warenkorb noch einmal.", style: .error)
                return nil
            }

            let session = try await hostedCheckoutService.startCheckout(
                userEmail: email,
                customerName: customerName,
                customerEmail: customerEmail,
                whatsApp: whatsApp,
                shippingAddress: shippingAddress,
                shippingAddressData: parseShippingAddressData(from: shippingAddress, countryCode: countryCode),
                shippingZone: shippingQuote.zone.rawValue,
                shippingCountryCode: countryCode,
                message: paymentLine + message,
                items: items,
                paymentMethod: paymentMethod,
                subtotalAmount: subtotal,
                shippingAmount: shippingQuote.price,
                taxRate: taxRate,
                taxAmount: taxAmount,
                totalAmount: subtotal + shippingQuote.price,
                fulfillmentProvider: deriveFulfillmentProvider(),
                platform: platform
            )
            clearCart()
            showUserToast("Checkout geoeffnet. Zahlung jetzt sicher abschliessen.", style: .success)
            return session
        } catch {
            skydownDebugLog("Dev Fehler startHostedCheckout:", error.localizedDescription)
            showUserToast("Stripe Checkout konnte nicht gestartet werden.", style: .error)
            return nil
        }
    }

    func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private var hasMixedFulfillmentProviders: Bool {
        let hasShopifyItems = items.contains { $0.shopifyVariantId?.takeIfNotBlank() != nil }
        let hasLegacyItems = items.contains { $0.shopifyVariantId?.takeIfNotBlank() == nil }
        return hasShopifyItems && hasLegacyItems
    }

    private func deriveFulfillmentProvider() -> String {
        items.contains { $0.shopifyVariantId?.takeIfNotBlank() != nil } ? "podpartner" : "manual"
    }

    private func shippingAddressCountryName(from shippingAddress: String) -> String {
        shippingAddress
            .components(separatedBy: .newlines)
            .last?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .takeIfNotBlank()
            ?? "Deutschland"
    }

    private func parseShippingAddressData(from shippingAddress: String, countryCode: String) -> ShippingAddressData {
        let lines = shippingAddress
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        let address1 = lines.first ?? ""
        let countryName = lines.last ?? "Deutschland"
        let cityLine = lines.dropLast().last ?? ""
        let postalAndCity = cityLine.split(separator: " ", maxSplits: 1).map(String.init)

        return ShippingAddressData(
            address1: address1,
            address2: lines.count > 3 ? lines[1] : "",
            city: postalAndCity.count > 1 ? postalAndCity[1] : cityLine,
            zip: postalAndCity.first ?? "",
            countryCode: countryCode,
            countryName: countryName
        )
    }
}

private extension String {
    func takeIfNotBlank() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
