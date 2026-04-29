//
//  CartView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

// swiftlint:disable file_length

import SwiftUI
import MessageUI

struct CartView: View {
    @EnvironmentObject var cartVM: CartViewModel
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject private var hostedCheckoutRedirectStore: HostedCheckoutRedirectStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ObservedObject private var commerceSettingsStore = CommerceSettingsStore.shared
    @ObservedObject private var merchStoreStatusStore = MerchStoreStatusStore.shared
    @ObservedObject private var paymentMethodSettingsStore = PaymentMethodSettingsStore.shared
    @AppStorage("orders.postCheckoutHighlight") private var postCheckoutHighlight = ""
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onGuestSignIn: (() -> Void)?

    init(
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        onGuestSignIn: (() -> Void)? = nil
    ) {
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onGuestSignIn = onGuestSignIn
    }

    @State private var name = ""
    @State private var email = ""
    @State private var whatsApp = ""
    @State private var shippingStreet = ""
    @State private var shippingAddressExtra = ""
    @State private var shippingPostalCode = ""
    @State private var shippingCity = ""
    @State private var shippingCountry = AppLocalized.text("cart.default.country", fallback: "Deutschland")
    @State private var message = AppLocalized.text("cart.default.message", fallback: "Ich interessiere mich fuer die Artikel in meinem Warenkorb.")
    @State private var showCheckoutConfirmSheet = false
    @State private var isSubmitting = false
    @State private var sheetPresentation = SkydownQueuedPresentation<CartPresentedSheet>()
    @State private var selectedPaymentMethod = ""
    @State private var showOptionalContactFields = false
    @State private var showOptionalAddressFields = false
    @State private var showOptionalMessageField = false

    private let defaultMessageText = AppLocalized.text("cart.default.message", fallback: "Ich interessiere mich fuer die Artikel in meinem Warenkorb.")

    private var optionalRevealAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .easeInOut(duration: 0.18)
    }

    private var cartFormLiftAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .easeInOut(duration: 0.2)
    }

    private var cartMicroLiftAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .easeInOut(duration: 0.18)
    }

    private var cartLayoutSyncAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .easeInOut(duration: 0.22)
    }

    private var cartRevealFieldTransition: AnyTransition {
        reduceMotion ? .opacity : .opacity.combined(with: .move(edge: .top))
    }

    private var isFormValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        !email.trimmingCharacters(in: .whitespaces).isEmpty &&
        !shippingStreet.trimmingCharacters(in: .whitespaces).isEmpty &&
        !shippingPostalCode.trimmingCharacters(in: .whitespaces).isEmpty &&
        !shippingCity.trimmingCharacters(in: .whitespaces).isEmpty &&
        !cartVM.items.isEmpty
    }

    private var availableCheckoutMethods: [String] {
        paymentMethodSettingsStore.settings.checkoutMethodLabels
    }

    private var isAdmin: Bool {
        authManager.userSession?.isPlatformOwner == true
    }

    private var isCheckoutAvailable: Bool {
        merchStoreStatusStore.status.isOpen || isAdmin
    }

    private var canSubmitOrder: Bool {
        isCheckoutAvailable &&
        isFormValid &&
        (availableCheckoutMethods.isEmpty || !selectedPaymentMethod.isEmpty)
    }

    private var isHostedCheckoutSelection: Bool {
        ["Stripe", "Klarna"].contains(selectedPaymentMethod)
    }

    private var isZeroCostHostedCheckout: Bool {
        isHostedCheckoutSelection &&
        !cartVM.items.isEmpty &&
        pricingSummary.total <= 0.01
    }

    private var hasCustomMessage: Bool {
        message.trimmingCharacters(in: .whitespacesAndNewlines) != defaultMessageText
    }

    private var canPresentInAppMailComposer: Bool {
        SkydownPlatform.supportsInAppMailComposer && MFMailComposeViewController.canSendMail()
    }

    private var totalPrice: Double {
        cartVM.items.reduce(0.0) { partialResult, cartItem in
            partialResult + cartItem.effectiveUnitPrice * Double(cartItem.quantity)
        }
    }

    private var pricingSummary: CartPricingSummary {
        let settings = commerceSettingsStore.settings
        let shippingQuote = (try? ShippingService.calculateShippingPrice(
            settings: settings.shipping,
            countryCode: ShippingService.resolveCountryCode(from: shippingCountry),
            items: cartVM.items,
            subtotal: totalPrice
        )) ?? ShippingQuote(
            zone: .international,
            countryCode: "--",
            price: 0,
            freeShippingApplied: false
        )
        let shippingCost = shippingQuote.price
        let total = totalPrice + shippingCost
        let taxRate = settings.invoice.taxRate
        let includedTax = taxRate > 0
            ? total * (taxRate / (100 + taxRate))
            : 0

        return CartPricingSummary(
            subtotal: totalPrice,
            shipping: shippingCost,
            taxRate: taxRate,
            includedTax: includedTax,
            total: total,
            zoneLabel: shippingQuote.zone.rawValue,
            shippingError: shippingQuote.countryCode == "--"
                ? AppLocalized.text("cart.shipping.error.country_missing", fallback: "Bitte Lieferland ergaenzen, damit Versand und Gesamtpreis korrekt berechnet werden.")
                : nil
        )
    }

    private var checkoutReadinessTitle: String {
        if canSubmitOrder { return AppLocalized.text("cart.readiness.ready", fallback: "Alles bereit") }
        if authManager.userSession == nil {
            return AppLocalized.text("auth.cart.readiness.missing_account", fallback: "Account needed")
        }
        if !isCheckoutAvailable { return AppLocalized.text("cart.readiness.paused", fallback: "Pausiert") }
        if !isFormValid { return AppLocalized.text("cart.readiness.fields_open", fallback: "Angaben offen") }
        if !availableCheckoutMethods.isEmpty && selectedPaymentMethod.isEmpty { return AppLocalized.text("cart.readiness.payment_open", fallback: "Zahlart offen") }
        return AppLocalized.text("cart.readiness.review", fallback: "Kurz pruefen")
    }

    private var checkoutReadinessDetail: String {
        if canSubmitOrder { return AppLocalized.text("cart.readiness.detail.ready", fallback: "Du kannst jetzt sicher fortfahren.") }
        if authManager.userSession == nil {
            return AppLocalized.text("auth.cart.readiness.detail_unsigned", fallback: "Sign in to align checkout and order updates with your profile.")
        }
        if !isCheckoutAvailable { return AppLocalized.text("cart.readiness.detail.paused", fallback: "Der Checkout startet wieder, sobald der Store geoeffnet ist.") }
        if !isFormValid { return AppLocalized.text("cart.readiness.detail.fields_open", fallback: "Bitte fehlende Pflichtfelder ergaenzen.") }
        if !availableCheckoutMethods.isEmpty && selectedPaymentMethod.isEmpty { return AppLocalized.text("cart.readiness.detail.payment_open", fallback: "Bitte eine Zahlart waehlen.") }
        return AppLocalized.text("cart.readiness.detail.review", fallback: "Ein kurzer Check, dann weiter.")
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
                    CartHeroCard(
                        colorScheme: colorScheme,
                        itemCount: cartVM.items.count,
                        totalPrice: pricingSummary.total,
                        isLoggedIn: authManager.userSession != nil
                    )

                    if let handover = cartVM.handoverContext, !cartVM.items.isEmpty {
                        CartHandoverStrip(
                            colorScheme: colorScheme,
                            itemName: handover.itemName,
                            variantSummary: handover.variantSummary
                        )
                        .transition(cartRevealFieldTransition)
                    }

                    if authManager.userSession == nil {
                        CartSectionCard(
                            title: AppLocalized.text("auth.cart.login.title", fallback: "Bereit fuer den Checkout"),
                            colorScheme: colorScheme
                        ) {
                            Text(
                                AppLocalized.text(
                                    "auth.cart.login.subtitle",
                                    fallback: "Melde dich an, um den Warenkorb zu sichern und deine Bestellung entspannt abzuschliessen."
                                )
                            )
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            SkydownBrandActionButton(
                                title: AppLocalized.text("auth.cart.login.cta", fallback: "Weiter mit Konto"),
                                systemImage: "person.crop.circle.fill.badge.plus",
                                accent: AppColors.accent(for: colorScheme),
                                colorScheme: colorScheme,
                                action: { presentSheet(.login(.cart)) }
                            )
                            .padding(.top, 4)
                        }
                    } else {
                        CartSectionCard(
                            title: AppLocalized.text("cart.selection.title", fallback: "Deine Auswahl"),
                            colorScheme: colorScheme
                        ) {
                            if cartVM.items.isEmpty {
                                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                                    Text(AppLocalized.text("cart.selection.empty.title", fallback: "Dein Warenkorb ist leer."))
                                        .font(.body.weight(.semibold))
                                        .foregroundColor(AppColors.text(for: colorScheme))
                                    Text(AppLocalized.text("cart.selection.empty.body", fallback: "Waehle im Shop einen Artikel und komme danach zum Checkout zurueck."))
                                        .font(.footnote)
                                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    SkydownBrandActionButton(
                                        title: AppLocalized.text("cart_action_continue_shopping", fallback: "Continue shopping"),
                                        accent: AppColors.accent(for: colorScheme),
                                        colorScheme: colorScheme,
                                        font: .subheadline.weight(.semibold),
                                        verticalPadding: 12,
                                        action: { dismiss() }
                                    )
                                    .padding(.top, 2)
                                }
                            } else {
                                ForEach(cartVM.items) { cartItem in
                                    CartItemCard(
                                        cartItem: cartItem,
                                        colorScheme: colorScheme
                                    ) { delta in
                                        cartVM.updateQuantity(for: cartItem, delta: delta)
                                        SkydownHaptics.selection()
                                    } onRemove: {
                                        cartVM.removeItem(cartItem)
                                        SkydownHaptics.selection()
                                    }
                                }
                            }
                        }

                        if !cartVM.items.isEmpty {
                            CartSectionCard(
                                title: AppLocalized.text("cart.summary.title", fallback: "Bestellsumme"),
                                colorScheme: colorScheme
                            ) {
                                PricingSummaryCard(
                                    colorScheme: colorScheme,
                                    summary: pricingSummary,
                                    shippingNote: commerceSettingsStore.settings.shipping.shippingNotes,
                                    companyName: commerceSettingsStore.settings.invoice.companyName
                                )
                            }

                            CartSectionCard(
                                title: AppLocalized.text("cart.contact.title", fallback: "Kontaktdaten"),
                                colorScheme: colorScheme
                            ) {
                                VStack(spacing: SkydownLayout.stackSpacingPill) {
                                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                                        CartInputField(
                                            title: "Name*",
                                            text: $name,
                                            colorScheme: colorScheme
                                        )
                                        CartInputField(
                                            title: "E-Mail*",
                                            text: $email,
                                            colorScheme: colorScheme,
                                            keyboard: .emailAddress,
                                            autocapitalization: .never
                                        )
                                    }

                                    if showOptionalContactFields || !whatsApp.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                        CartInputField(
                                            title: "WhatsApp (optional)",
                                            text: $whatsApp,
                                            colorScheme: colorScheme,
                                            keyboard: .phonePad
                                        )
                                        .transition(cartRevealFieldTransition)
                                    } else {
                                        CartOptionalRevealButton(
                                            title: AppLocalized.text("cart.contact.add_whatsapp", fallback: "WhatsApp hinzufuegen"),
                                            colorScheme: colorScheme
                                        ) {
                                            withAnimation(optionalRevealAnimation) {
                                                showOptionalContactFields = true
                                            }
                                        }
                                    }
                                }
                                .padding(.top, 4)
                            }

                            CartSectionCard(
                                title: AppLocalized.text("cart.shipping.title", fallback: "Lieferadresse"),
                                colorScheme: colorScheme
                            ) {
                                VStack(spacing: SkydownLayout.stackSpacingPill) {
                                    CartInputField(
                                        title: "Strasse, Hausnr.*",
                                        text: $shippingStreet,
                                        colorScheme: colorScheme
                                    )
                                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                                        CartInputField(
                                            title: "PLZ*",
                                            text: $shippingPostalCode,
                                            colorScheme: colorScheme,
                                            keyboard: .numbersAndPunctuation
                                        )
                                        CartInputField(
                                            title: "Ort*",
                                            text: $shippingCity,
                                            colorScheme: colorScheme
                                        )
                                    }
                                    if showOptionalAddressFields || !shippingAddressExtra.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                        CartInputField(
                                            title: "Adresszusatz (optional)",
                                            text: $shippingAddressExtra,
                                            colorScheme: colorScheme
                                        )
                                        .transition(cartRevealFieldTransition)
                                    } else {
                                        CartOptionalRevealButton(
                                            title: AppLocalized.text("cart.shipping.add_address_extra", fallback: "Adresszusatz hinzufuegen"),
                                            colorScheme: colorScheme
                                        ) {
                                            withAnimation(optionalRevealAnimation) {
                                                showOptionalAddressFields = true
                                            }
                                        }
                                    }
                                    CartInputField(
                                        title: "Land",
                                        text: $shippingCountry,
                                        colorScheme: colorScheme
                                    )
                                }
                                .padding(.top, 4)
                            }

                            if !isCheckoutAvailable {
                                CartInlineStatusStrip(
                                    colorScheme: colorScheme,
                                    icon: "pause.circle.fill",
                                    title: AppLocalized.text("cart_checkout_paused_title", fallback: "Checkout pausiert")
                                ) {
                                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                                        Text(AppLocalized.text("cart_checkout_paused_body", fallback: "Der Checkout ist kurz pausiert. Deine Auswahl bleibt gespeichert."))
                                            .font(.footnote.weight(.medium))
                                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                            .fixedSize(horizontal: false, vertical: true)
                                        HStack(spacing: SkydownLayout.stackSpacingMicro) {
                                            SkydownBrandActionButton(
                                                title: AppLocalized.text("common.settings", fallback: "Einstellungen"),
                                                accent: AppColors.accent(for: colorScheme),
                                                colorScheme: colorScheme,
                                                role: .muted,
                                                font: .caption.weight(.semibold),
                                                cornerRadius: SkydownLayout.compactRadius,
                                                verticalPadding: 8,
                                                action: onOpenSettings
                                            )
                                            SkydownBrandActionButton(
                                                title: AppLocalized.text("cart.action.open_shop", fallback: "Shop ansehen"),
                                                accent: AppColors.accent(for: colorScheme),
                                                colorScheme: colorScheme,
                                                role: .muted,
                                                font: .caption.weight(.semibold),
                                                cornerRadius: SkydownLayout.compactRadius,
                                                verticalPadding: 8,
                                                action: { dismiss() }
                                            )
                                        }
                                    }
                                }
                                .transition(cartRevealFieldTransition)
                            } else if !availableCheckoutMethods.isEmpty {
                                CartSectionCard(
                                    title: AppLocalized.text("cart.payment.select_title", fallback: "Zahlart waehlen"),
                                    colorScheme: colorScheme
                                ) {
                                    PaymentMethodSelectionCard(
                                        colorScheme: colorScheme,
                                        methods: availableCheckoutMethods,
                                        selectedMethod: $selectedPaymentMethod,
                                        isZeroCostOrder: !cartVM.items.isEmpty && pricingSummary.total <= 0.01
                                    )
                                }

                                if !selectedPaymentMethod.isEmpty {
                                    CartSectionCard(
                                        title: AppLocalized.text("cart.payment.info_title", fallback: "Zahlungsinfo"),
                                        colorScheme: colorScheme
                                    ) {
                                        SelectedPaymentMethodInfoCard(
                                            colorScheme: colorScheme,
                                            settings: paymentMethodSettingsStore.settings,
                                            selectedMethod: selectedPaymentMethod,
                                            isZeroCostOrder: !cartVM.items.isEmpty && pricingSummary.total <= 0.01
                                        )
                                    }
                                }
                            }

                            CartSectionCard(
                                title: AppLocalized.text("cart.message.title", fallback: "Nachricht"),
                                colorScheme: colorScheme
                            ) {
                                if showOptionalMessageField || hasCustomMessage {
                                    TextEditor(text: $message)
                                        .frame(minHeight: 108)
                                        .padding(14)
                                        .background(AppColors.secondaryBackground(for: colorScheme))
                                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                                                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                                        )
                                        .transition(cartRevealFieldTransition)
                                } else {
                                    CartOptionalRevealButton(
                                        title: AppLocalized.text("cart.message.add_optional", fallback: "Hinweis hinzufuegen (optional)"),
                                        colorScheme: colorScheme
                                    ) {
                                        withAnimation(optionalRevealAnimation) {
                                            showOptionalMessageField = true
                                        }
                                    }
                                }
                            }

                            CartCheckoutSafetyZone(
                                colorScheme: colorScheme,
                                supportMailbox: supportMailbox
                            )
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding)
            }
            .scrollIndicators(.hidden)
            .scrollDismissesKeyboard(.interactively)
            .skydownDismissKeyboardOnTap()
            .background(backgroundGradient.ignoresSafeArea())
            .navigationTitle(AppLocalized.text("cart_title", fallback: "Warenkorb"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .skydownKeyboardDismissToolbar()
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.back", fallback: "Zurueck"),
                        systemImage: "chevron.backward",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: { dismiss() }
                    )
                    .skydownInteractiveFeedback()
                }

                ToolbarItem(placement: .topBarTrailing) {
                    AppSessionToolbarActions(
                        onOpenProfile: onOpenProfile,
                        onOpenSettings: onOpenSettings,
                        onGuestSignIn: onGuestSignIn
                    )
                }
            }
            .safeAreaInset(edge: .bottom, spacing: SkydownLayout.stackSpacingNone) {
                if authManager.userSession != nil && !cartVM.items.isEmpty {
                    CartSubmitBar(
                        colorScheme: colorScheme,
                        itemCount: cartVM.items.count,
                        totalPrice: pricingSummary.total,
                        readinessTitle: checkoutReadinessTitle,
                        readinessDetail: checkoutReadinessDetail,
                        buttonTitle: isZeroCostHostedCheckout
                            ? AppLocalized.text("cart.action.confirm_order", fallback: "Confirm order")
                            : (
                                isHostedCheckoutSelection
                                    ? AppLocalized.text("cart.action.continue_securely", fallback: "Continue securely")
                                    : AppLocalized.text("cart.action.review_order", fallback: "Review order")
                            ),
                        trustHint: isZeroCostHostedCheckout
                            ? AppLocalized.text("cart.trust.zero_eur", fallback: "For this 0 EUR test item, no payment is needed. The order is confirmed directly.")
                            : (
                                isHostedCheckoutSelection
                                    ? AppLocalized.text("cart.trust.redirect", fallback: "Secure redirect with clear feedback.")
                                    : AppLocalized.text("cart.trust.submit", fallback: "Secure submission with support available.")
                            ),
                        isReady: canSubmitOrder,
                        isSubmitting: isSubmitting
                    ) {
                        SkydownHaptics.selection()
                        showCheckoutConfirmSheet = true
                    }
                }
            }
            .onAppear {
                populateContactDetails()
                syncSelectedPaymentMethod(with: availableCheckoutMethods)
                showOptionalContactFields = !whatsApp.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                showOptionalAddressFields = !shippingAddressExtra.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                showOptionalMessageField = hasCustomMessage
            }
            .onReceive(paymentMethodSettingsStore.$settings) { settings in
                syncSelectedPaymentMethod(with: settings.checkoutMethodLabels)
            }
            .onChange(of: hostedCheckoutRedirectStore.latestEvent) { _, event in
                guard let event else { return }
                let restoredCart = cartVM.handleHostedCheckoutRedirect(status: event.status)

                switch event.status {
                case .success:
                    cartVM.showUserToast(
                        AppLocalized.text("cart.toast.checkout_completed", fallback: "Checkout completed. We are now verifying payment confirmation and updating status here."),
                        style: .success
                    )
                    postCheckoutHighlight = AppLocalized.text(
                        "cart.highlight.checkout_completed",
                        fallback: "Checkout completed. We keep you updated here on payment and shipping status."
                    )
                case .cancel:
                    if restoredCart {
                        cartVM.showUserToast(
                            AppLocalized.text("cart.toast.checkout_cancelled_restored", fallback: "Checkout cancelled. Your cart has been restored."),
                            style: .info
                        )
                        postCheckoutHighlight = AppLocalized.text(
                            "cart.highlight.checkout_cancelled_restored",
                            fallback: "Checkout cancelled. Your selection is back in the cart."
                        )
                    } else {
                        cartVM.showUserToast(
                            AppLocalized.text("cart.toast.checkout_cancelled_unpaid", fallback: "Checkout cancelled. The order remains unpaid."),
                            style: .info
                        )
                    }
                }

                hostedCheckoutRedirectStore.clear()
            }
        }
        .animation(cartLayoutSyncAnimation, value: isCheckoutAvailable)
        .animation(cartLayoutSyncAnimation, value: availableCheckoutMethods.count)
        .animation(cartLayoutSyncAnimation, value: cartVM.handoverContext)
        .sheet(item: activePresentedSheetBinding) { sheet in
            switch sheet {
            case .login(let context):
                LoginView(entryContext: context)
            case .mail(let draft):
                MailView(
                    subject: draft.subject,
                    body: draft.body,
                    recipients: [supportMailbox],
                    preferredSendingEmailAddress: draft.preferredSendingEmailAddress
                )
            }
        }
        .sheet(isPresented: $showCheckoutConfirmSheet) {
            CartCheckoutConfirmSheet(
                colorScheme: colorScheme,
                items: cartVM.items,
                totalPrice: pricingSummary.total,
                isHostedCheckout: isHostedCheckoutSelection,
                isZeroCostOrder: isZeroCostHostedCheckout,
                isSubmitting: isSubmitting,
                onCancel: {
                    showCheckoutConfirmSheet = false
                },
                onConfirm: {
                    Task {
                        let didSubmit = await submitOrderAsync()
                        if didSubmit {
                            showCheckoutConfirmSheet = false
                        }
                    }
                }
            )
            .presentationDetents([.height(420), .medium])
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(28)
        }
        .fancyToast(isPresented: $cartVM.showToast,
                    message: cartVM.toastMessage,
                    style: cartVM.toastStyle)
    }

    private var activePresentedSheetBinding: Binding<CartPresentedSheet?> {
        Binding(
            get: { sheetPresentation.activeItem },
            set: { sheetPresentation.updatePresentedItem($0) }
        )
    }

    private func presentSheet(_ sheet: CartPresentedSheet) {
        sheetPresentation.request(sheet)
    }

    private func submitOrderAsync() async -> Bool {
        guard !isSubmitting else { return false }
        withAnimation(cartFormLiftAnimation) {
            isSubmitting = true
        }

        guard availableCheckoutMethods.isEmpty || !selectedPaymentMethod.isEmpty else {
            cartVM.showUserToast(
                AppLocalized.text("cart.toast.select_payment_first", fallback: "Please select a payment method first."),
                style: .error
            )
            withAnimation(cartFormLiftAnimation) {
                isSubmitting = false
            }
            return false
        }
        if let shippingError = pricingSummary.shippingError {
            cartVM.showUserToast(shippingError, style: .error)
            withAnimation(cartFormLiftAnimation) {
                isSubmitting = false
            }
            return false
        }

        let shippingAddress = composedShippingAddress

        if ["Stripe", "Klarna"].contains(selectedPaymentMethod) {
            if let session = await cartVM.startHostedCheckout(
                customerName: name,
                customerEmail: email,
                whatsApp: whatsApp,
                shippingAddress: shippingAddress,
                message: message,
                paymentMethod: selectedPaymentMethod,
                subtotalAmount: pricingSummary.subtotal,
                shippingAmount: pricingSummary.shipping,
                taxRate: pricingSummary.taxRate,
                taxAmount: pricingSummary.includedTax,
                totalAmount: pricingSummary.total,
                isCheckoutAvailable: isCheckoutAvailable
            ) {
                openURL(session.checkoutURL)
                withAnimation(cartFormLiftAnimation) {
                    isSubmitting = false
                }
                postCheckoutHighlight = isZeroCostHostedCheckout
                    ? AppLocalized.text("cart.toast.order_confirmed_sync", fallback: "Order confirmed. Status and shop sync appear here directly.")
                    : AppLocalized.text("cart.toast.checkout_started_status", fallback: "Checkout started. After payment confirmation, you see current status here.")
                return true
            }

            withAnimation(cartFormLiftAnimation) {
                isSubmitting = false
            }
            return false
        }

        let draft = makeOrderMailDraft(items: cartVM.items)

        let didSubmit = await cartVM.submitCartAsOrder(
            customerName: name,
            customerEmail: email,
            whatsApp: whatsApp,
            shippingAddress: shippingAddress,
            message: message,
            paymentMethod: selectedPaymentMethod,
            subtotalAmount: pricingSummary.subtotal,
            shippingAmount: pricingSummary.shipping,
            taxRate: pricingSummary.taxRate,
            taxAmount: pricingSummary.includedTax,
            totalAmount: pricingSummary.total,
            isCheckoutAvailable: isCheckoutAvailable
        )

        if didSubmit {
            presentOrderMailDraft(draft)
            postCheckoutHighlight = AppLocalized.text(
                "cart.highlight.order_submitted",
                fallback: "Order submitted. The team now confirms payment and shipping step."
            )
        }

        withAnimation(cartFormLiftAnimation) {
            isSubmitting = false
        }
        return didSubmit
    }

    private func populateContactDetails() {
        guard let user = authManager.userSession else { return }

        if name.isEmpty { name = user.username }
        if email.isEmpty { email = user.email }
        if whatsApp.isEmpty { whatsApp = user.whatsApp ?? "" }
    }

    private var composedShippingAddress: String {
        let topLine = shippingStreet.trimmingCharacters(in: .whitespacesAndNewlines)
        let extraLine = shippingAddressExtra.trimmingCharacters(in: .whitespacesAndNewlines)
        let postalAndCity = [shippingPostalCode.trimmedForAddress, shippingCity.trimmedForAddress]
            .filter { !$0.isEmpty }
            .joined(separator: " ")
        let country = shippingCountry.trimmedForAddress.isEmpty
            ? AppLocalized.text("cart.default.country", fallback: "Germany")
            : shippingCountry.trimmedForAddress

        return [topLine, extraLine, postalAndCity, country]
            .filter { !$0.isEmpty }
            .joined(separator: "\n")
    }

    private var backgroundGradient: LinearGradient {
        LinearGradient(
            colors: [
                AppColors.primaryBackground(for: colorScheme),
                AppColors.accent(for: colorScheme).opacity(0.14),
                AppColors.accentMystic(for: colorScheme).opacity(0.10),
                AppColors.primaryBackground(for: colorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private var supportMailbox: String {
        PlatformContactEmails.defaultSupportEmail
    }

    private func makeOrderMailDraft(items: [CartItem]) -> OrderMailDraft {
        let preferredEmail = authManager.userSession?.email
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .takeIfNotBlank()
        let itemSummary = items.isEmpty
            ? "- \(AppLocalized.text("cart.mail.items.none", fallback: "No items"))"
            : items.map { cartItem in
                let linePrice = cartItem.effectiveUnitPrice * Double(cartItem.quantity)
                let colorPart = cartItem.color?.takeIfNotBlank().map {
                    " | \(AppLocalized.text("cart.mail.field.color", fallback: "Color")): \($0)"
                } ?? ""
                return "- \(cartItem.item.name) | \(AppLocalized.text("cart.mail.field.size", fallback: "Size")): \(cartItem.size)\(colorPart) | \(AppLocalized.text("cart.mail.field.quantity", fallback: "Quantity")): \(cartItem.quantity) | \(AppLocalized.text("cart.mail.field.price", fallback: "Price")): \(String(format: "EUR %.2f", linePrice))"
            }.joined(separator: "\n")
        let orderTotal = items.reduce(0.0) { partialResult, cartItem in
            partialResult + cartItem.effectiveUnitPrice * Double(cartItem.quantity)
        }
        let subject = preferredEmail.map {
            AppLocalized.text("cart.mail.subject.with_email", fallback: "New order - %@")
                .replacingOccurrences(of: "%@", with: $0)
        } ?? AppLocalized.text("cart.mail.subject", fallback: "New order")
        let body = """
        \(AppLocalized.text("cart.mail.greeting", fallback: "Hello SkyOS team,"))

        \(AppLocalized.text("cart.mail.intro", fallback: "a new order has been prepared in SkyOS."))

        \(AppLocalized.text("cart.mail.field.name", fallback: "Name")): \(name.isEmpty ? AppLocalized.text("cart.mail.value.not_provided", fallback: "Not provided") : name)
        \(AppLocalized.text("cart.mail.field.email", fallback: "Email")): \(email.isEmpty ? AppLocalized.text("cart.mail.value.not_provided", fallback: "Not provided") : email)
        WhatsApp: \(whatsApp.isEmpty ? AppLocalized.text("cart.mail.value.not_provided", fallback: "Not provided") : whatsApp)
        \(AppLocalized.text("cart.mail.field.shipping_address", fallback: "Shipping address")):
        \(composedShippingAddress.isEmpty ? AppLocalized.text("cart.mail.value.not_provided", fallback: "Not provided") : composedShippingAddress)

        \(AppLocalized.text("cart.mail.field.items", fallback: "Cart")):
        \(itemSummary)

        \(AppLocalized.text("cart.pricing.subtotal", fallback: "Subtotal")): \(String(format: "EUR %.2f", orderTotal))
        \(AppLocalized.text("cart.pricing.shipping_zone", fallback: "Shipping zone")): \(pricingSummary.zoneLabel)
        \(AppLocalized.text("cart.pricing.shipping", fallback: "Shipping")): \(String(format: "EUR %.2f", pricingSummary.shipping))
        \(AppLocalized.text("cart.pricing.tax_included", fallback: "Included VAT")) (\(String(format: "%.1f", pricingSummary.taxRate))%): \(String(format: "EUR %.2f", pricingSummary.includedTax))
        \(AppLocalized.text("cart.pricing.total", fallback: "Total")): \(String(format: "EUR %.2f", pricingSummary.total))

        \(AppLocalized.text("cart.mail.field.payment", fallback: "Payment")):
        \(selectedPaymentMethod.isEmpty ? AppLocalized.text("cart.mail.payment.pending", fallback: "Pending / via follow-up") : selectedPaymentMethod)

        \(AppLocalized.text("cart.mail.field.message", fallback: "Message")):
        \(message.isEmpty ? AppLocalized.text("cart.mail.message.none", fallback: "No additional message.") : message)
        """

        return OrderMailDraft(
            subject: subject,
            body: body,
            preferredSendingEmailAddress: preferredEmail
        )
    }

    private func presentOrderMailDraft(_ draft: OrderMailDraft) {
        if canPresentInAppMailComposer {
            presentSheet(.mail(draft))
            return
        }

        let encodedSubject = draft.subject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let encodedBody = draft.body.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        guard let url = URL(string: "mailto:\(supportMailbox)?subject=\(encodedSubject)&body=\(encodedBody)") else {
            cartVM.showUserToast(AppLocalized.text("cart.mail.error.prepare", fallback: "Could not prepare the mail app."), style: .error)
            return
        }

        openURL(url) { accepted in
            if !accepted {
                cartVM.showUserToast(AppLocalized.text("cart.mail.error.open", fallback: "Could not open the mail app."), style: .error)
            }
        }
    }

    private func syncSelectedPaymentMethod(with methods: [String]) {
        if methods.isEmpty {
            selectedPaymentMethod = ""
        } else if !methods.contains(selectedPaymentMethod) {
            selectedPaymentMethod = methods.first ?? ""
        }
    }
}

private struct PricingSummaryCard: View {
    let colorScheme: ColorScheme
    let summary: CartPricingSummary
    let shippingNote: String
    let companyName: String

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            pricingLine("Zwischensumme", summary.subtotal)
            pricingTextLine("Versandzone", summary.zoneLabel)
            pricingLine("Versand", summary.shipping)
            pricingLine("inkl. MwSt. (\(String(format: "%.1f", summary.taxRate))%)", summary.includedTax)

            Divider()

            pricingLine("Gesamt", summary.total, isEmphasized: true)

            if let shippingError = summary.shippingError {
                HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
                    Image(systemName: "info.circle.fill")
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                        .padding(.top, 1)
                    Text(shippingError)
                        .font(.footnote.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            if !shippingNote.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(shippingNote)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Text(
                AppLocalized.text("cart.invoice.company", fallback: "Invoice by %@")
                    .replacingOccurrences(of: "%@", with: companyName.takeIfNotBlank() ?? "Ngoc Anh Nguyen (Yang D. Nash - Skydown)")
            )
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
    }

    @ViewBuilder
    private func pricingLine(_ title: String, _ value: Double, isEmphasized: Bool = false) -> some View {
        HStack {
            Text(title)
                .font(isEmphasized ? .headline : .subheadline)
                .foregroundColor(AppColors.text(for: colorScheme))
            Spacer()
            Text(String(format: "EUR %.2f", value))
                .font(isEmphasized ? .headline.weight(.bold) : .subheadline.weight(.semibold))
                .foregroundColor(isEmphasized ? AppColors.accent(for: colorScheme) : AppColors.text(for: colorScheme))
        }
    }

    @ViewBuilder
    private func pricingTextLine(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title)
                .font(.subheadline)
                .foregroundColor(AppColors.text(for: colorScheme))
            Spacer()
            Text(value)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
        }
    }
}

private struct PaymentMethodSelectionCard: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let colorScheme: ColorScheme
    let methods: [String]
    @Binding var selectedMethod: String
    let isZeroCostOrder: Bool

    private var paymentSelectionAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .easeInOut(duration: 0.18)
    }

    private var paymentHintTransition: AnyTransition {
        reduceMotion ? .opacity : .opacity.combined(with: .move(edge: .top))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: "lock.shield")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                Text(AppLocalized.text("cart.payment.secure_selection", fallback: "Secure payment selection"))
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: 132), spacing: SkydownLayout.stackSpacingCompact, alignment: .leading)],
                alignment: .leading,
                spacing: SkydownLayout.stackSpacingCompact
            ) {
                ForEach(methods, id: \.self) { method in
                    Button {
                        withAnimation(paymentSelectionAnimation) {
                        selectedMethod = method
                        }
                        SkydownHaptics.selection()
                    } label: {
                        let isSelected = selectedMethod == method
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSnug) {
                            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
                                Image(systemName: isSelected ? "checkmark.seal.fill" : "circle")
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(isSelected ? AppColors.accent(for: colorScheme) : AppColors.secondaryText(for: colorScheme))
                                Text(method)
                                    .font(.subheadline.weight(.semibold))
                                    .lineLimit(2)
                                Spacer(minLength: 0)
                                if isSelected {
                                    Text(AppLocalized.text("common.selected", fallback: "Selected"))
                                        .font(.caption2.weight(.bold))
                                        .foregroundColor(AppColors.accent(for: colorScheme))
                                }
                            }

                            Text(paymentRouteDetail(for: method))
                                .font(.caption.weight(.medium))
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                .lineLimit(2)
                        }
                        .foregroundColor(
                            isSelected
                                ? AppColors.text(for: colorScheme)
                                : AppColors.secondaryText(for: colorScheme)
                        )
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                                .fill(
                                    isSelected
                                        ? AppColors.accent(for: colorScheme).opacity(0.14)
                                        : AppColors.secondaryBackground(for: colorScheme)
                                )
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                                .stroke(
                                    isSelected
                                        ? AppColors.accent(for: colorScheme)
                                        : AppColors.accent(for: colorScheme).opacity(0.10),
                                    lineWidth: isSelected ? 1.4 : 1
                                )
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .animation(paymentSelectionAnimation, value: selectedMethod)
                }
            }

            if !selectedMethod.isEmpty {
                Text(
                    isZeroCostOrder && ["Stripe", "Klarna"].contains(selectedMethod)
                        ? AppLocalized.text("cart.payment.zero_hint", fallback: "For this 0 EUR test item, no payment is opened.")
                        : AppLocalized.text("cart.payment.next_step_hint", fallback: "Your payment choice continues securely in the next step.")
                )
                    .font(.caption.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .transition(paymentHintTransition)
            }
        }
        .animation(paymentSelectionAnimation, value: selectedMethod)
    }

    private func paymentRouteDetail(for method: String) -> String {
        switch method {
        case "Stripe":
            return AppLocalized.text("cart.route.detail.stripe", fallback: "Card, Apple Pay, Google Pay, and more secure options")
        case "Klarna":
            return AppLocalized.text("cart.route.detail.klarna", fallback: "Klarna via Stripe")
        case "PayPal":
            return AppLocalized.text("cart.route.detail.paypal", fallback: "PayPal follow-up")
        case "Bankueberweisung":
            return AppLocalized.text("cart.route.detail.bank", fallback: "Direct bank transfer")
        default:
            return AppLocalized.text("cart.route.detail.default", fallback: "Available payment route")
        }
    }
}

private struct SelectedPaymentMethodInfoCard: View {
    let colorScheme: ColorScheme
    let settings: PaymentMethodSettings
    let selectedMethod: String
    let isZeroCostOrder: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: "checkmark.shield")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                Text(paymentTrustLine)
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            switch selectedMethod {
            case "PayPal":
                Text(AppLocalized.text("cart.payment.paypal.start", fallback: "PayPal starts after a short team confirmation."))
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                if settings.paypal.accountHint.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(AppLocalized.text("cart.payment.paypal.link_followup", fallback: "PayPal link is shared in the follow-up."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                } else {
                    CartBadge(text: settings.paypal.accountHint, colorScheme: colorScheme)
                }

            case "Bankueberweisung":
                Text(AppLocalized.text("cart.payment.bank.intro", fallback: "Bank transfer with clear account details for this order."))
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                    if !settings.bankTransfer.accountHolder.isEmpty {
                        paymentInfoLine(AppLocalized.text("cart.payment.bank.account_holder", fallback: "Account holder"), settings.bankTransfer.accountHolder)
                    }
                    if !settings.bankTransfer.bankName.isEmpty {
                        paymentInfoLine(AppLocalized.text("cart.payment.bank.name", fallback: "Bank"), settings.bankTransfer.bankName)
                    }
                    if !settings.bankTransfer.iban.isEmpty {
                        paymentInfoLine("IBAN", settings.bankTransfer.iban)
                    }
                    if !settings.bankTransfer.bic.isEmpty {
                        paymentInfoLine("BIC", settings.bankTransfer.bic)
                    }
                    if !settings.bankTransfer.paymentInstructions.isEmpty {
                        paymentInfoLine(AppLocalized.text("cart.payment.bank.note", fallback: "Note"), settings.bankTransfer.paymentInstructions)
                    }
                }

            case "Stripe":
                Text(
                    isZeroCostOrder
                        ? AppLocalized.text("cart.payment.stripe.zero", fallback: "For this 0 EUR test item, no payment is needed. The order is confirmed directly.")
                        : AppLocalized.text("cart.payment.stripe.standard", fallback: "Stripe then opens secure live checkout. Depending on device and availability, card, Apple Pay, Google Pay, and other compatible methods may appear.")
                )
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

            case "Klarna":
                Text(
                    isZeroCostOrder
                        ? AppLocalized.text("cart.payment.klarna.zero", fallback: "For this 0 EUR test item, no payment is needed. The order is confirmed directly.")
                        : AppLocalized.text("cart.payment.klarna.standard", fallback: "Klarna then continues securely via Stripe.")
                )
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

            default:
                EmptyView()
            }
        }
    }

    private var paymentTrustLine: String {
        if isZeroCostOrder && ["Stripe", "Klarna"].contains(selectedMethod) {
            return AppLocalized.text("cart.payment.trust.zero", fallback: "No payment amount. Direct order confirmation with clear status.")
        }
        switch selectedMethod {
        case "Stripe", "Klarna":
            return AppLocalized.text("cart.payment.trust.hosted", fallback: "Secure checkout with protected redirect.")
        case "PayPal", "Bankueberweisung":
            return AppLocalized.text("cart.payment.trust.followup", fallback: "Clear payment route with direct follow-up.")
        default:
            return AppLocalized.text("cart.payment.trust.default", fallback: "Secure payment handling.")
        }
    }

    @ViewBuilder
    private func paymentInfoLine(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Text(value)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
        }
    }
}

private struct OrderMailDraft: Identifiable, Equatable {
    let id = UUID()
    let subject: String
    let body: String
    let preferredSendingEmailAddress: String?
}

private enum CartPresentedSheet: Identifiable, Equatable {
    case login(AuthEntryContext)
    case mail(OrderMailDraft)

    var id: String {
        switch self {
        case .login(let context):
            return "login-\(context.rawValue)"
        case .mail(let draft):
            return "mail-\(draft.id.uuidString)"
        }
    }
}

private struct CartPricingSummary {
    let subtotal: Double
    let shipping: Double
    let taxRate: Double
    let includedTax: Double
    let total: Double
    let zoneLabel: String
    let shippingError: String?
}

private extension String {
    func takeIfNotBlank() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    var trimmedForAddress: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

#Preview {
    let authManager = AuthManager()
    let cartVM = CartViewModel(authManager: authManager)

    CartView()
        .environmentObject(authManager)
        .environmentObject(cartVM)
}

private struct CartHeroCard: View {
    let colorScheme: ColorScheme
    let itemCount: Int
    let totalPrice: Double
    let isLoggedIn: Bool

    var body: some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingComfortable) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                Text(AppLocalized.text("cart_title", fallback: "Cart"))
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(AppLocalized.text("cart.hero.subtitle", fallback: "Review calmly, finish securely."))
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                    .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "bag.fill")
                    .font(.title2)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.14 : 0.08), radius: 12, y: 5)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: SkydownLayout.stackSpacingCompact) {
                CartBadge(
                    text: AppLocalized.text("cart.items.count_format", fallback: "%d items")
                        .replacingOccurrences(of: "%d", with: "\(itemCount)"),
                    colorScheme: colorScheme
                )
                CartBadge(
                    text: isLoggedIn
                        ? AppLocalized.text("cart.account.active", fallback: "Account active")
                        : AppLocalized.text("cart.account.guest", fallback: "Guest"),
                    colorScheme: colorScheme
                )
                if itemCount > 0 {
                    CartBadge(text: String(format: "EUR %.2f", totalPrice), colorScheme: colorScheme)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
    }

    private var cardBackground: some View {
        LinearGradient(
            colors: [
                AppColors.cardBackground(for: colorScheme),
                AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

private struct CartHandoverStrip: View {
    let colorScheme: ColorScheme
    let itemName: String
    let variantSummary: String

    var body: some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingPill) {
            Image(systemName: "checkmark.circle.fill")
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.accent(for: colorScheme))
                .padding(.top, 1)
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                Text(AppLocalized.text("cart.handover.just_added", fallback: "Just added"))
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                Text(itemName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(2)
                Text(variantSummary)
                    .font(.caption.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.78))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
    }
}

private struct CartSectionCard<Content: View>: View {
    let title: String
    let colorScheme: ColorScheme
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            content
        }
        .padding(20)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
    }
}

private struct CartInlineStatusStrip<Content: View>: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let colorScheme: ColorScheme
    let icon: String
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(spacing: SkydownLayout.stackSpacingPill) {
                Image(systemName: icon)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            content
        }
        .padding(.horizontal, SkydownLayout.cardPadding)
        .padding(.vertical, 14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
        .animation(SkydownMotion.preferredStatusTransition(accessibilityReduceMotion: reduceMotion), value: title)
    }
}

private struct CartItemCard: View {
    let cartItem: CartItem
    let colorScheme: ColorScheme
    let onQuantityChange: (Int) -> Void
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                    Text(cartItem.item.name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(2)

                    HStack(spacing: SkydownLayout.stackSpacingMicro) {
                        CartBadge(text: cartItem.size, colorScheme: colorScheme)
                        if let color = cartItem.color?.takeIfNotBlank() {
                            CartBadge(text: color, colorScheme: colorScheme)
                        }
                        CartBadge(text: "x\(cartItem.quantity)", colorScheme: colorScheme, isEmphasized: true)
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: SkydownLayout.stackSpacingNano) {
                    Text(String(format: "EUR %.2f", cartItem.effectiveUnitPrice * Double(cartItem.quantity)))
                        .font(.headline.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                    Text(String(format: "EUR %.2f / Stk", cartItem.effectiveUnitPrice))
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            HStack(spacing: SkydownLayout.stackSpacingPill) {
                quantityStepperButton(systemName: "minus") {
                    onQuantityChange(-1)
                }
                Text("\(cartItem.quantity)")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .frame(minWidth: 24)
                quantityStepperButton(systemName: "plus") {
                    onQuantityChange(1)
                }
                Spacer()
                SkydownBrandActionButton(
                    title: AppLocalized.text("cart.line_item.remove", fallback: "Entfernen"),
                    systemImage: "trash",
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    font: .caption2.weight(.semibold),
                    cornerRadius: SkydownLayout.tightRadius,
                    verticalPadding: 6,
                    expandToFullWidth: false,
                    action: onRemove
                )
                .skydownInteractiveFeedback()
                .accessibilityLabel("Artikel entfernen")
                .accessibilityHint("Entfernt den Artikel aus dem Warenkorb")
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.08), lineWidth: 1)
        )
    }

    @ViewBuilder
    private func quantityStepperButton(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.caption.weight(.bold))
                .frame(width: 44, height: 44)
        }
        .buttonStyle(.plain)
        .background(
            Circle()
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            Circle()
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .foregroundColor(AppColors.text(for: colorScheme))
        .accessibilityLabel(systemName == "minus" ? "Menge reduzieren" : "Menge erhoehen")
    }
}

private struct CartInputField: View {
    let title: String
    @Binding var text: String
    let colorScheme: ColorScheme
    var keyboard: UIKeyboardType = .default
    var autocapitalization: TextInputAutocapitalization = .sentences

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(title, text: $text)
                .keyboardType(keyboard)
                .textInputAutocapitalization(autocapitalization)
                .padding(14)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                )
                .foregroundColor(AppColors.text(for: colorScheme))
        }
    }
}

private struct CartOptionalRevealButton: View {
    let title: String
    let colorScheme: ColorScheme
    let action: () -> Void

    var body: some View {
        SkydownBrandActionButton(
            title: title,
            accent: AppColors.accent(for: colorScheme),
            colorScheme: colorScheme,
            role: .muted,
            font: .caption.weight(.semibold),
            cornerRadius: SkydownLayout.denseRadius,
            verticalPadding: 7,
            expandToFullWidth: false,
            action: {
                SkydownHaptics.selection()
                action()
            }
        )
    }
}

private struct CartCheckoutConfirmSheet: View {
    let colorScheme: ColorScheme
    let items: [CartItem]
    let totalPrice: Double
    let isHostedCheckout: Bool
    let isZeroCostOrder: Bool
    let isSubmitting: Bool
    let onCancel: () -> Void
    let onConfirm: () -> Void

    private var totalQuantity: Int {
        items.reduce(0) { $0 + $1.quantity }
    }

    private var summaryItems: [CartItem] {
        Array(items.prefix(3))
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                Text(
                    isZeroCostOrder
                        ? "Fuer diesen 0-EUR-Testartikel wird die Bestellung direkt bestaetigt."
                        : (isHostedCheckout ? "Du wirst danach sicher zum Checkout weitergeleitet." : "Danach uebermitteln wir deine Bestellung sicher an das Team.")
                )
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                HStack {
                    Text("\(items.count) Artikel")
                    Spacer()
                    Text("\(totalQuantity)x Gesamtmenge")
                }
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                ForEach(summaryItems) { item in
                    HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
                        Text(item.item.name)
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .lineLimit(2)
                        Spacer(minLength: 8)
                        Text("x\(item.quantity) · \(String(format: "EUR %.2f", item.effectiveUnitPrice * Double(item.quantity)))")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }

                if items.count > summaryItems.count {
                    Text("+\(items.count - summaryItems.count) weitere Artikel")
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Divider()
                    .overlay(AppColors.accent(for: colorScheme).opacity(0.12))

                HStack {
                    Text("Gesamt")
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Spacer()
                    Text(String(format: "EUR %.2f", totalPrice))
                        .font(.headline.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
            }
            .padding(14)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))

            Text(AppLocalized.text("cart.confirm.safety_hint", fallback: "Secure flow, clear feedback, and support when needed."))
                .font(.caption.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: SkydownLayout.stackSpacingPill) {
                SkydownBrandActionButton(
                    title: AppLocalized.text("common.cancel", fallback: "Cancel"),
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    isEnabled: !isSubmitting,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 14,
                    expandToFullWidth: true,
                    action: onCancel
                )
                .skydownInteractiveFeedback()
                .frame(maxWidth: .infinity, alignment: .leading)

                SkydownBrandActionButton(
                    title: isZeroCostOrder
                        ? "Bestellung bestaetigen"
                        : (isHostedCheckout ? "Sicher fortfahren" : "Bestellung senden"),
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    isLoading: isSubmitting,
                    font: .subheadline.weight(.bold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 14,
                    expandToFullWidth: true,
                    action: onConfirm
                )
                .skydownInteractiveFeedback()
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, SkydownLayout.cardPadding)
            .background(AppColors.cardBackground(for: colorScheme))
            .navigationTitle(AppLocalized.text("cart.confirm.title", fallback: "Confirm order"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.done", fallback: "Done"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        isEnabled: !isSubmitting,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: onCancel
                    )
                    .skydownInteractiveFeedback()
                }
            }
        }
    }
}

private struct CartCheckoutSafetyZone: View {
    let colorScheme: ColorScheme
    let supportMailbox: String

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSnug) {
            safetyLine("lock.shield.fill", AppLocalized.text("cart.safety.payment", fallback: "Secure payment paths and clear redirect in the next step."))
            safetyLine("doc.text.magnifyingglass", AppLocalized.text("cart.safety.review", fallback: "Data and total are reviewed once more before sending."))
            safetyLine(
                "book.pages",
                AppLocalized.text(
                    "cart.legal.checkout_references",
                    fallback: "Binding terms, cancellation and returns follow the information shown in checkout and the legal documents in Settings."
                )
            )
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: "message.badge.fill")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                    .padding(.top, 2)
                    Text(
                        AppLocalized.text("cart.safety.support", fallback: "Support directly reachable: %@")
                            .replacingOccurrences(of: "%@", with: supportMailbox)
                    )
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .textSelection(.enabled)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.74))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
    }

    private func safetyLine(_ icon: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
            Image(systemName: icon)
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.accent(for: colorScheme))
                .padding(.top, 2)
            Text(text)
                .font(.caption.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct CartSubmitBar: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let colorScheme: ColorScheme
    let itemCount: Int
    let totalPrice: Double
    let readinessTitle: String
    let readinessDetail: String
    let buttonTitle: String
    let trustHint: String
    let isReady: Bool
    let isSubmitting: Bool
    let onSubmit: () -> Void

    var body: some View {
        VStack(spacing: SkydownLayout.stackSpacingCompact) {
            Divider()
                .overlay(AppColors.accent(for: colorScheme).opacity(0.12))

            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingPill) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSubtle) {
                    Text("Checkout")
                        .font(.caption2.weight(.bold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    Text(itemCount > 0 ? String(format: "EUR %.2f", totalPrice) : "Noch leer")
                        .font(.title3.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                    Text(itemCount > 0 ? "\(itemCount) Artikel · \(readinessTitle)" : readinessTitle)
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(readinessDetail)
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                SkydownBrandActionButton(
                    title: isSubmitting ? "Wird vorbereitet" : buttonTitle,
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    isEnabled: isReady,
                    isLoading: isSubmitting,
                    font: .subheadline.weight(.semibold),
                    cornerRadius: SkydownLayout.denseRadius,
                    verticalPadding: 10,
                    action: onSubmit
                )
                .frame(minWidth: 132, minHeight: 44)
                .animation(SkydownMotion.preferredStatusTransition(accessibilityReduceMotion: reduceMotion), value: isSubmitting)
                .accessibilityLabel(isSubmitting ? "Checkout wird vorbereitet" : buttonTitle)
                .accessibilityHint(isSubmitting ? "Bitte kurz warten" : "Oeffnet die finale Bestaetigung")
            }
            .padding(.horizontal, 20)

            HStack(spacing: SkydownLayout.stackSpacingDense) {
                Image(systemName: "checkmark.shield.fill")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                Text(isSubmitting ? "Wir pruefen jetzt sicher die Daten und den naechsten Schritt." : trustHint)
                    .font(.caption2.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(2)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 14)
            .transition(.opacity)
        }
        .background(AppColors.cardBackground(for: colorScheme).opacity(0.98))
        .animation(SkydownMotion.preferredStatusTransition(accessibilityReduceMotion: reduceMotion), value: isSubmitting)
    }
}

private struct CartBadge: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let text: String
    let colorScheme: ColorScheme
    var isEmphasized: Bool = false

    var body: some View {
        Text(text)
            .font(.caption2.weight(.semibold))
            .foregroundColor(
                isEmphasized
                    ? AppColors.accent(for: colorScheme)
                    : AppColors.secondaryText(for: colorScheme)
            )
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(
                Capsule(style: .continuous)
                    .fill(
                        isEmphasized
                            ? AppColors.accent(for: colorScheme).opacity(0.14)
                            : AppColors.cardBackground(for: colorScheme)
                    )
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(
                        isEmphasized
                            ? AppColors.accent(for: colorScheme).opacity(0.3)
                            : AppColors.accent(for: colorScheme).opacity(0.10),
                        lineWidth: 1
                    )
            )
            .animation(SkydownMotion.preferredContentReveal(accessibilityReduceMotion: reduceMotion), value: text)
    }
}
