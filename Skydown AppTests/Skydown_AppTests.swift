//
//  Skydown_AppTests.swift
//  Skydown AppTests
//
//  Created by Yang D. Nash on 23.07.25.
//

import Foundation
import Testing
@testable import Skydown_App

struct Skydown_AppTests {

    @Test func sanitizedUsernamePrefersPreferredValueAndClampsToFirestoreLimit() async throws {
        let longName = "  This Display Name Is Definitely Longer Than Thirty Two Chars  "

        let sanitized = FirebaseAuthService.sanitizedUsername(
            longName,
            authUserDisplayName: "Ignored Display Name",
            fallbackEmail: "fallback@example.com"
        )

        #expect(sanitized == "This Display Name Is Definitely")
        #expect(sanitized.count <= 32)
    }

    @Test func sanitizedUsernameFallsBackToEmailPrefixWhenNeeded() async throws {
        let sanitized = FirebaseAuthService.sanitizedUsername(
            nil,
            authUserDisplayName: "   ",
            fallbackEmail: "nash.lioncorna@gmail.com"
        )

        #expect(sanitized == "nash.lioncorna")
    }

    @Test func ownerEmailAlwaysResolvesToOwnerRole() async throws {
        let role = UserRole.resolve(
            from: nil,
            isAdmin: false,
            email: "NASH.LIONCORNA@GMAIL.COM"
        )

        #expect(role == .owner)
        #expect(role.hasStaffAccess)
        #expect(role.hasAdminWorkspaceAccess)
    }

    @Test func screenHeaderSettingsCountOnlyConfiguredSurfaces() async throws {
        let settings = ScreenHeaderSettings(
            homeImageURL: "https://example.com/home.jpg",
            homeEyebrow: "",
            homeTitle: "",
            homeSubtitle: "",
            homeDetail: "",
            musicHubImageURL: "",
            musicHubEyebrow: "Music",
            musicHubTitle: "Hub",
            musicHubSubtitle: "",
            musicHubDetail: "",
            shopImageURL: "",
            shopEyebrow: "",
            shopTitle: "",
            shopSubtitle: "",
            shopDetail: "",
            videoHubImageURL: "",
            videoHubEyebrow: "",
            videoHubTitle: "",
            videoHubSubtitle: "",
            videoHubDetail: ""
        )

        #expect(settings.configuredCount == 2)
        #expect(settings.resolvedHomeImageURL == "https://example.com/home.jpg")
        #expect(settings.resolvedMusicHubEyebrow == "Music")
    }

    @MainActor
    @Test func hostedCheckoutCancelRestoresPendingCartItems() async throws {
        let authManager = AuthManager(authService: TestAuthService())
        authManager.userSession = User(
            id: "qa-user",
            email: "qa_checkout@example.com",
            username: "qa_checkout",
            registrationDate: .now
        )

        let viewModel = CartViewModel(
            authManager: authManager,
            orderService: TestOrderService(),
            hostedCheckoutService: TestHostedCheckoutService()
        )

        let item = MerchandiseItem(
            id: "qa-merch-item",
            name: "QA Hoodie",
            price: 79.0,
            description: "QA merch item",
            imageURLs: [],
            available: true
        )
        viewModel.items = [
            CartItem(
                item: item,
                size: "M",
                color: nil,
                quantity: 1,
                shopifyVariantId: "gid://shopify/ProductVariant/qa-hoodie-m",
                sku: "QA-HOODIE-M",
                unitPrice: 79.0
            ),
        ]

        let subtotal = viewModel.items.reduce(0) { $0 + ($1.effectiveUnitPrice * Double($1.quantity)) }
        let shippingQuote = try ShippingService.calculateShippingPrice(
            settings: CommerceSettingsStore.shared.settings.shipping,
            countryCode: "DE",
            items: viewModel.items,
            subtotal: subtotal
        )
        let taxRate = CommerceSettingsStore.shared.settings.invoice.taxRate
        let taxAmount = taxRate > 0 ? (subtotal + shippingQuote.price) * (taxRate / (100 + taxRate)) : 0

        let session = await viewModel.startHostedCheckout(
            customerName: "QA User",
            customerEmail: "qa_checkout@example.com",
            whatsApp: "",
            shippingAddress: "QA Street 1\n10115 Berlin\nDeutschland",
            message: "QA checkout",
            paymentMethod: "Stripe",
            subtotalAmount: subtotal,
            shippingAmount: shippingQuote.price,
            taxRate: taxRate,
            taxAmount: taxAmount,
            totalAmount: subtotal + shippingQuote.price,
            isCheckoutAvailable: true
        )

        #expect(session != nil)
        #expect(viewModel.items.isEmpty)

        let restored = viewModel.handleHostedCheckoutRedirect(status: .cancel)

        #expect(restored)
        #expect(viewModel.items.count == 1)
        #expect(viewModel.items.first?.item.name == "QA Hoodie")
    }

}

private struct TestAuthService: AuthServicing {
    func observeAuthState(_ onChange: @escaping @MainActor (User?) -> Void) -> () -> Void { {} }
    func signIn(email: String, password: String) async throws {}
    func signInWithGoogle(preferredUsername: String?, registrationConsent: RegistrationLegalConsent?) async throws {}
    func register(username: String, email: String, whatsApp: String, password: String, registrationConsent: RegistrationLegalConsent) async throws {}
    func updateCurrentProfile(username: String, whatsApp: String?, profileTagline: String?, profileBio: String?, instagramHandle: String?) async throws -> User {
        throw NSError(domain: "TestAuthService", code: 1)
    }
    func updateCurrentAIAccessEnabled(_ enabled: Bool) async throws -> User {
        throw NSError(domain: "TestAuthService", code: 1)
    }
    func signOut() throws {}
    func deleteCurrentAccount() async throws {}
    func fetchCurrentUser() async throws -> User? { nil }
}

private struct TestOrderService: OrderServicing {
    func observeOrders(_ onChange: @escaping @MainActor (Result<[Order], Error>) -> Void) -> () -> Void { {} }
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
        "qa-order"
    }
    func confirmPayment(orderID: String, paymentMethod: String?, paymentReference: String?) async throws {}
    func toggleCompleted(orderID: String, isCompleted: Bool) async throws {}
    func deleteOrder(orderID: String) async throws {}
}

private struct TestHostedCheckoutService: HostedCheckoutServicing {
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
        HostedCheckoutSession(
            orderID: "qa-order",
            checkoutURL: URL(string: "https://checkout.stripe.com/c/pay/cs_test_qa")!,
            sessionID: "cs_test_qa"
        )
    }
}
