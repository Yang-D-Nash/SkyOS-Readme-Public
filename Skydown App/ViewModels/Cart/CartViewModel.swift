//
//  cartViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import Foundation

@MainActor
class CartViewModel: ObservableObject {
    struct CartHandoverContext: Equatable {
        let itemName: String
        let variantSummary: String
    }

    @Published var items: [CartItem] = []
    @Published var userEmail: String = ""
    @Published var handoverContext: CartHandoverContext?
    
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .success

    private let authManager: AuthManager
    private let orderService: OrderServicing
    private let hostedCheckoutService: HostedCheckoutServicing
    private var pendingHostedCheckoutItems: [CartItem]?

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
        let variantSummary = [size, normalizedColor, "x\(quantity)"]
            .compactMap { $0 }
            .joined(separator: " · ")
        handoverContext = CartHandoverContext(
            itemName: item.name,
            variantSummary: variantSummary
        )
        showUserToast("\(item.name) \(size)\(colorSuffix) x\(quantity) in Warenkorb gelegt", style: .success)
    }

    func removeItem(_ cartItem: CartItem) {
        items.removeAll { $0.id == cartItem.id }
    }

    func updateQuantity(for cartItem: CartItem, delta: Int) {
        guard delta != 0 else { return }
        guard let index = items.firstIndex(where: {
            $0.item.id == cartItem.item.id &&
            $0.size == cartItem.size &&
            $0.color?.lowercased() == cartItem.color?.lowercased()
        }) else {
            return
        }

        let nextQuantity = max(1, min(10, items[index].quantity + delta))
        guard nextQuantity != items[index].quantity else { return }
        items[index].quantity = nextQuantity
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
            showUserToast("Dein Warenkorb ist leer. Fuege zuerst einen Artikel hinzu.", style: .error)
            return false
        }
        guard isCheckoutAvailable || authManager.userSession?.isPlatformOwner == true else {
            showUserToast("Der Store ist gerade pausiert. Deine Auswahl bleibt gespeichert.", style: .error)
            return false
        }
        guard let email = authManager.userSession?.email else {
            showUserToast("Bitte melde dich an, damit wir die Bestellung sicher zuordnen koennen.", style: .error)
            return false
        }
        if hasMixedFulfillmentProviders {
            showUserToast("Bitte trenne unterschiedliche Fulfillment-Typen in zwei Bestellungen.", style: .error)
            return false
        }

        let countryCode: String
        do {
            countryCode = try ShippingService.resolveCountryCode(from: shippingAddressCountryName(from: shippingAddress))
        } catch {
            skydownDebugLog("Shipping country resolve:", error.localizedDescription)
            showUserToast(
                "Das Lieferland ist noch unklar. Bitte pruefe kurz die Adresse.",
                style: .error
            )
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
            skydownDebugLog("Shipping quote:", error.localizedDescription)
            showUserToast(
                "Versand ist gerade noch nicht eindeutig. Bitte pruefe Adresse oder Auswahl.",
                style: .error
            )
            return false
        }

        do {
            let isZeroCostOrder = subtotal + shippingQuote.price <= 0.01
            let paymentLine = isZeroCostOrder ? "" : paymentMethod?
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .takeIfNotBlank()
                .map { "Gewuenschte Zahlart: \($0)\n\n" }
                ?? ""
            guard abs(subtotalAmount - subtotal) < 0.01,
                  abs(shippingAmount - shippingQuote.price) < 0.01,
                  abs(totalAmount - (subtotal + shippingQuote.price)) < 0.01 else {
                showUserToast("Die Bestellsumme hat sich geaendert. Bitte kurz bestaetigen und erneut fortfahren.", style: .error)
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
            showUserToast(
                isZeroCostOrder
                    ? "Bestellung ist bestaetigt. Wir aktualisieren jetzt den Status."
                    : "Bestellung ist eingegangen. Wir melden uns mit dem naechsten Schritt.",
                style: .success
            )
            return true
        } catch {
            skydownDebugLog("Dev Fehler submitCartAsOrder:", error.localizedDescription)
            showUserToast("Absenden war gerade nicht moeglich. Bitte in einem Moment erneut versuchen.", style: .error)
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
            showUserToast("Dein Warenkorb ist leer. Fuege zuerst einen Artikel hinzu.", style: .error)
            return nil
        }
        guard isCheckoutAvailable || authManager.userSession?.isPlatformOwner == true else {
            showUserToast("Der Store ist gerade pausiert. Deine Auswahl bleibt gespeichert.", style: .error)
            return nil
        }
        guard let email = authManager.userSession?.email else {
            showUserToast("Bitte melde dich an, damit wir die Bestellung sicher zuordnen koennen.", style: .error)
            return nil
        }
        if hasMixedFulfillmentProviders {
            showUserToast("Bitte trenne unterschiedliche Fulfillment-Typen in zwei Bestellungen.", style: .error)
            return nil
        }

        let countryCode: String
        do {
            countryCode = try ShippingService.resolveCountryCode(from: shippingAddressCountryName(from: shippingAddress))
        } catch {
            skydownDebugLog("Hosted checkout country resolve:", error.localizedDescription)
            showUserToast(
                "Das Lieferland ist noch unklar. Bitte pruefe kurz die Adresse.",
                style: .error
            )
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
            skydownDebugLog("Hosted checkout shipping quote:", error.localizedDescription)
            showUserToast(
                "Versand ist gerade noch nicht eindeutig. Bitte pruefe Adresse oder Auswahl.",
                style: .error
            )
            return nil
        }

        do {
            let isZeroCostOrder = subtotal + shippingQuote.price <= 0.01
            let paymentLine = isZeroCostOrder ? "" : paymentMethod
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .takeIfNotBlank()
                .map { "Gewuenschte Zahlart: \($0)\n\n" }
                ?? ""
            guard abs(subtotalAmount - subtotal) < 0.01,
                  abs(shippingAmount - shippingQuote.price) < 0.01,
                  abs(totalAmount - (subtotal + shippingQuote.price)) < 0.01 else {
                showUserToast("Die Bestellsumme hat sich geaendert. Bitte kurz bestaetigen und erneut fortfahren.", style: .error)
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
            pendingHostedCheckoutItems = items
            clearCart()
            showUserToast(
                isZeroCostOrder
                    ? "Bestellung ist bestaetigt. Wir aktualisieren jetzt den Status."
                    : "Sicherer Checkout ist geoeffnet. Du kannst die Zahlung jetzt abschliessen.",
                style: .success
            )
            return session
        } catch {
            skydownDebugLog("Dev Fehler startHostedCheckout:", error.localizedDescription)
            showUserToast("Checkout startet gerade nicht. Bitte in einem Moment erneut versuchen.", style: .error)
            return nil
        }
    }

    @discardableResult
    func handleHostedCheckoutRedirect(status: HostedCheckoutRedirectStatus) -> Bool {
        let canRestorePendingCheckout = status == .cancel &&
            items.isEmpty &&
            !(pendingHostedCheckoutItems ?? []).isEmpty

        if canRestorePendingCheckout, let pendingHostedCheckoutItems {
            items = pendingHostedCheckoutItems
        }

        pendingHostedCheckoutItems = nil
        return canRestorePendingCheckout
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
