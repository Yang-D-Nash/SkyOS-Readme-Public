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
    @ObservedObject private var commerceSettingsStore = CommerceSettingsStore.shared
    @ObservedObject private var merchStoreStatusStore = MerchStoreStatusStore.shared
    @ObservedObject private var paymentMethodSettingsStore = PaymentMethodSettingsStore.shared
    @AppStorage("orders.postCheckoutHighlight") private var postCheckoutHighlight = ""
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void

    init(
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {}
    ) {
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
    }

    @State private var name = ""
    @State private var email = ""
    @State private var whatsApp = ""
    @State private var shippingStreet = ""
    @State private var shippingAddressExtra = ""
    @State private var shippingPostalCode = ""
    @State private var shippingCity = ""
    @State private var shippingCountry = "Deutschland"
    @State private var message = "Ich interessiere mich fuer die Artikel in meinem Warenkorb."
    @State private var showCheckoutConfirmSheet = false
    @State private var isSubmitting = false
    @State private var activePresentedSheet: CartPresentedSheet?
    @State private var queuedPresentedSheet: CartPresentedSheet?
    @State private var selectedPaymentMethod = ""
    @State private var showOptionalContactFields = false
    @State private var showOptionalAddressFields = false
    @State private var showOptionalMessageField = false

    private let defaultMessageText = "Ich interessiere mich fuer die Artikel in meinem Warenkorb."

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
            shippingError: shippingQuote.countryCode == "--" ? "Lieferland bitte kurz ergaenzen, damit Versand und Gesamtpreis exakt berechnet werden." : nil
        )
    }

    private var checkoutReadinessTitle: String {
        if canSubmitOrder { return "Alles bereit" }
        if authManager.userSession == nil { return "Anmeldung fehlt" }
        if !isCheckoutAvailable { return "Kurz pausiert" }
        if !isFormValid { return "Angaben offen" }
        if !availableCheckoutMethods.isEmpty && selectedPaymentMethod.isEmpty { return "Zahlart offen" }
        return "Kurz pruefen"
    }

    private var checkoutReadinessDetail: String {
        if canSubmitOrder { return "Du kannst jetzt sicher fortfahren." }
        if authManager.userSession == nil { return "Bitte zuerst anmelden." }
        if !isCheckoutAvailable { return "Der Checkout startet wieder nach der Oeffnung." }
        if !isFormValid { return "Bitte fehlende Pflichtfelder ergaenzen." }
        if !availableCheckoutMethods.isEmpty && selectedPaymentMethod.isEmpty { return "Bitte eine Zahlart waehlen." }
        return "Ein kurzer Check, dann weiter."
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
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
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    }

                    if authManager.userSession == nil {
                        CartSectionCard(
                            title: "Konto erforderlich",
                            colorScheme: colorScheme
                        ) {
                            Text("Bitte anmelden, damit wir deine Bestellung sicher zuordnen koennen.")
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            Button {
                                presentSheet(.login)
                            } label: {
                                Label("Anmelden", systemImage: "person.crop.circle.fill.badge.plus")
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(AppColors.accent(for: colorScheme))
                            .padding(.top, 4)
                        }
                    } else {
                        CartSectionCard(
                            title: "Deine Auswahl",
                            colorScheme: colorScheme
                        ) {
                            if cartVM.items.isEmpty {
                                VStack(alignment: .leading, spacing: 10) {
                                    Text("Dein Warenkorb ist noch leer.")
                                        .font(.body.weight(.semibold))
                                        .foregroundColor(AppColors.text(for: colorScheme))
                                    Text("Waehle im Shop einen Artikel und komme danach direkt hierher zum Abschluss.")
                                        .font(.footnote)
                                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    Button("Weiter shoppen") {
                                        SkydownHaptics.selection()
                                        dismiss()
                                    }
                                    .font(.subheadline.weight(.semibold))
                                    .buttonStyle(.bordered)
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

                        CartSectionCard(
                            title: "Bestellsumme",
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
                            title: "Kontaktdaten",
                            colorScheme: colorScheme
                        ) {
                            VStack(spacing: 10) {
                                HStack(spacing: 10) {
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
                                    .transition(.opacity.combined(with: .move(edge: .top)))
                                } else {
                                    Button("WhatsApp hinzufügen") {
                                        SkydownHaptics.selection()
                                        withAnimation(.easeInOut(duration: 0.18)) {
                                            showOptionalContactFields = true
                                        }
                                    }
                                    .font(.caption.weight(.semibold))
                                    .buttonStyle(.bordered)
                                }
                            }
                            .padding(.top, 4)
                        }

                        CartSectionCard(
                            title: "Lieferadresse",
                            colorScheme: colorScheme
                        ) {
                            VStack(spacing: 10) {
                                CartInputField(
                                    title: "Strasse, Hausnr.*",
                                    text: $shippingStreet,
                                    colorScheme: colorScheme
                                )
                                HStack(spacing: 10) {
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
                                    .transition(.opacity.combined(with: .move(edge: .top)))
                                } else {
                                    Button("Adresszusatz hinzufügen") {
                                        SkydownHaptics.selection()
                                        withAnimation(.easeInOut(duration: 0.18)) {
                                            showOptionalAddressFields = true
                                        }
                                    }
                                    .font(.caption.weight(.semibold))
                                    .buttonStyle(.bordered)
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
                                title: "Checkout pausiert"
                            ) {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Der Checkout ist kurz pausiert. Deine Auswahl bleibt sicher gespeichert.")
                                        .font(.footnote.weight(.medium))
                                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                        .fixedSize(horizontal: false, vertical: true)
                                    HStack(spacing: 8) {
                                        Button("Einstellungen") {
                                            SkydownHaptics.selection()
                                            onOpenSettings()
                                        }
                                        .font(.caption.weight(.semibold))
                                        .buttonStyle(.bordered)
                                        Button("Shop ansehen") {
                                            SkydownHaptics.selection()
                                            dismiss()
                                        }
                                        .font(.caption.weight(.semibold))
                                        .buttonStyle(.bordered)
                                    }
                                }
                            }
                            .transition(.opacity.combined(with: .move(edge: .top)))
                        } else if !availableCheckoutMethods.isEmpty {
                            CartSectionCard(
                                title: "Zahlart waehlen",
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
                                    title: "Zahlungsinfo",
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
                            title: "Nachricht",
                            colorScheme: colorScheme
                        ) {
                            if showOptionalMessageField || hasCustomMessage {
                                TextEditor(text: $message)
                                    .frame(minHeight: 108)
                                    .padding(14)
                                    .background(AppColors.secondaryBackground(for: colorScheme))
                                    .clipShape(RoundedRectangle(cornerRadius: 20))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 20)
                                            .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                                    )
                                    .transition(.opacity.combined(with: .move(edge: .top)))
                            } else {
                                Button("Hinweis hinzufügen (optional)") {
                                    SkydownHaptics.selection()
                                    withAnimation(.easeInOut(duration: 0.18)) {
                                        showOptionalMessageField = true
                                    }
                                }
                                .font(.caption.weight(.semibold))
                                .buttonStyle(.bordered)
                            }
                        }

                        CartCheckoutSafetyZone(
                            colorScheme: colorScheme,
                            supportMailbox: supportMailbox
                        )
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding)
            }
            .scrollIndicators(.hidden)
            .background(backgroundGradient.ignoresSafeArea())
            .navigationTitle("Warenkorb")
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.headline.weight(.bold))
                    }
                }

                ToolbarItem(placement: .topBarTrailing) {
                    AppSessionToolbarActions(
                        onOpenProfile: onOpenProfile,
                        onOpenSettings: onOpenSettings
                    )
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if authManager.userSession != nil {
                    CartSubmitBar(
                        colorScheme: colorScheme,
                        itemCount: cartVM.items.count,
                        totalPrice: pricingSummary.total,
                        readinessTitle: checkoutReadinessTitle,
                        readinessDetail: checkoutReadinessDetail,
                        buttonTitle: isZeroCostHostedCheckout
                            ? "Bestellung bestaetigen"
                            : (isHostedCheckoutSelection ? "Sicher fortfahren" : "Bestellung pruefen"),
                        trustHint: isZeroCostHostedCheckout
                            ? "Fuer diesen 0-EUR-Testartikel ist keine Zahlung noetig. Die Bestellung wird direkt bestaetigt."
                            : (isHostedCheckoutSelection
                                ? "Sichere Weiterleitung mit klarer Rueckmeldung."
                                : "Sichere Uebermittlung, Support ist erreichbar."),
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
                    cartVM.showUserToast("Checkout abgeschlossen. Wir pruefen jetzt die Zahlungsbestaetigung und aktualisieren den Status hier.", style: .success)
                    postCheckoutHighlight = "Checkout abgeschlossen. Wir halten dich hier ueber Zahlung und Versandstatus auf dem Laufenden."
                case .cancel:
                    if restoredCart {
                        cartVM.showUserToast("Checkout abgebrochen. Dein Warenkorb wurde wiederhergestellt.", style: .info)
                        postCheckoutHighlight = "Checkout abgebrochen. Deine Auswahl ist wieder im Warenkorb."
                    } else {
                        cartVM.showUserToast("Checkout abgebrochen. Die Bestellung bleibt unbezahlt.", style: .info)
                    }
                }

                hostedCheckoutRedirectStore.clear()
            }
        }
        .animation(.easeInOut(duration: 0.22), value: isCheckoutAvailable)
        .animation(.easeInOut(duration: 0.22), value: availableCheckoutMethods.count)
        .animation(.easeInOut(duration: 0.22), value: cartVM.handoverContext)
        .sheet(item: $activePresentedSheet) { sheet in
            switch sheet {
            case .login:
                LoginView()
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
        .onChange(of: activePresentedSheet) { _, sheet in
            guard sheet == nil, let queuedPresentedSheet else { return }
            self.queuedPresentedSheet = nil
            DispatchQueue.main.async {
                activePresentedSheet = queuedPresentedSheet
            }
        }
    }

    private func presentSheet(_ sheet: CartPresentedSheet) {
        guard activePresentedSheet == nil else {
            queuedPresentedSheet = sheet
            activePresentedSheet = nil
            return
        }

        activePresentedSheet = sheet
    }

    private func submitOrderAsync() async -> Bool {
        guard !isSubmitting else { return false }
        withAnimation(.easeInOut(duration: 0.2)) {
            isSubmitting = true
        }

        guard availableCheckoutMethods.isEmpty || !selectedPaymentMethod.isEmpty else {
            cartVM.showUserToast("Bitte waehle zuerst eine Zahlart.", style: .error)
            withAnimation(.easeInOut(duration: 0.2)) {
                isSubmitting = false
            }
            return false
        }
        if let shippingError = pricingSummary.shippingError {
            cartVM.showUserToast(shippingError, style: .error)
            withAnimation(.easeInOut(duration: 0.2)) {
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
                withAnimation(.easeInOut(duration: 0.2)) {
                    isSubmitting = false
                }
                postCheckoutHighlight = isZeroCostHostedCheckout
                    ? "Bestellung bestaetigt. Status und Shop-Sync erscheinen hier direkt."
                    : "Checkout gestartet. Nach der Zahlungsbestaetigung siehst du hier direkt den aktuellen Status."
                return true
            }

            withAnimation(.easeInOut(duration: 0.2)) {
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
            postCheckoutHighlight = "Bestellung uebermittelt. Das Team bestaetigt jetzt Zahlung und Versandschritt."
        }

        withAnimation(.easeInOut(duration: 0.2)) {
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
        let country = shippingCountry.trimmedForAddress.isEmpty ? "Deutschland" : shippingCountry.trimmedForAddress

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
        "skydownent@gmail.com"
    }

    private func makeOrderMailDraft(items: [CartItem]) -> OrderMailDraft {
        let preferredEmail = authManager.userSession?.email
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .takeIfNotBlank()
        let itemSummary = items.isEmpty
            ? "- Keine Artikel"
            : items.map { cartItem in
                let linePrice = cartItem.effectiveUnitPrice * Double(cartItem.quantity)
                let colorPart = cartItem.color?.takeIfNotBlank().map { " | Farbe: \($0)" } ?? ""
                return "- \(cartItem.item.name) | Groesse: \(cartItem.size)\(colorPart) | Menge: \(cartItem.quantity) | Preis: \(String(format: "EUR %.2f", linePrice))"
            }.joined(separator: "\n")
        let orderTotal = items.reduce(0.0) { partialResult, cartItem in
            partialResult + cartItem.effectiveUnitPrice * Double(cartItem.quantity)
        }
        let subject = preferredEmail.map { "Neue Bestellung - \($0)" } ?? "Neue Bestellung"
        let body = """
        Hallo SkyOS-Team,

        es wurde eine neue Bestellung in SkyOS vorbereitet.

        Name: \(name.isEmpty ? "Nicht angegeben" : name)
        E-Mail: \(email.isEmpty ? "Nicht angegeben" : email)
        WhatsApp: \(whatsApp.isEmpty ? "Nicht angegeben" : whatsApp)
        Adresse:
        \(composedShippingAddress.isEmpty ? "Nicht angegeben" : composedShippingAddress)

        Warenkorb:
        \(itemSummary)

        Zwischensumme: \(String(format: "EUR %.2f", orderTotal))
        Versandzone: \(pricingSummary.zoneLabel)
        Versand: \(String(format: "EUR %.2f", pricingSummary.shipping))
        Enthaltene MwSt. (\(String(format: "%.1f", pricingSummary.taxRate))%): \(String(format: "EUR %.2f", pricingSummary.includedTax))
        Gesamt: \(String(format: "EUR %.2f", pricingSummary.total))

        Zahlart:
        \(selectedPaymentMethod.isEmpty ? "Noch offen / per Rueckkontakt" : selectedPaymentMethod)

        Nachricht:
        \(message.isEmpty ? "Keine zusaetzliche Nachricht." : message)
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
            cartVM.showUserToast("Die Mail-App konnte nicht vorbereitet werden.", style: .error)
            return
        }

        openURL(url) { accepted in
            if !accepted {
                cartVM.showUserToast("Die Mail-App konnte nicht geoeffnet werden.", style: .error)
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
        VStack(alignment: .leading, spacing: 12) {
            pricingLine("Zwischensumme", summary.subtotal)
            pricingTextLine("Versandzone", summary.zoneLabel)
            pricingLine("Versand", summary.shipping)
            pricingLine("inkl. MwSt. (\(String(format: "%.1f", summary.taxRate))%)", summary.includedTax)

            Divider()

            pricingLine("Gesamt", summary.total, isEmphasized: true)

            if let shippingError = summary.shippingError {
                HStack(alignment: .top, spacing: 8) {
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

            Text("Rechnung über \(companyName.takeIfNotBlank() ?? "Skydown OS").")
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
    let colorScheme: ColorScheme
    let methods: [String]
    @Binding var selectedMethod: String
    let isZeroCostOrder: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "lock.shield")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                Text("Sichere Zahlungswahl")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: 132), spacing: 12, alignment: .leading)],
                alignment: .leading,
                spacing: 12
            ) {
                ForEach(methods, id: \.self) { method in
                    Button {
                        withAnimation(.easeInOut(duration: 0.18)) {
                        selectedMethod = method
                        }
                        SkydownHaptics.selection()
                    } label: {
                        let isSelected = selectedMethod == method
                        VStack(alignment: .leading, spacing: 9) {
                            HStack(alignment: .top, spacing: 8) {
                                Image(systemName: isSelected ? "checkmark.seal.fill" : "circle")
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(isSelected ? AppColors.accent(for: colorScheme) : AppColors.secondaryText(for: colorScheme))
                                Text(method)
                                    .font(.subheadline.weight(.semibold))
                                    .lineLimit(2)
                                Spacer(minLength: 0)
                                if isSelected {
                                    Text("Ausgewählt")
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
                            RoundedRectangle(cornerRadius: 20)
                                .fill(
                                    isSelected
                                        ? AppColors.accent(for: colorScheme).opacity(0.14)
                                        : AppColors.secondaryBackground(for: colorScheme)
                                )
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
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
                    .animation(.easeInOut(duration: 0.18), value: selectedMethod)
                }
            }

            if !selectedMethod.isEmpty {
                Text(
                    isZeroCostOrder && ["Stripe", "Klarna"].contains(selectedMethod)
                        ? "Fuer diesen 0-EUR-Testartikel wird keine Zahlung geoeffnet."
                        : "Deine Zahlungswahl wird im naechsten Schritt sicher fortgefuehrt."
                )
                    .font(.caption.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .animation(.easeInOut(duration: 0.18), value: selectedMethod)
    }

    private func paymentRouteDetail(for method: String) -> String {
        switch method {
        case "Stripe":
            return "Karte, Apple Pay, Google Pay und weitere sichere Optionen"
        case "Klarna":
            return "Klarna via Stripe"
        case "PayPal":
            return "PayPal Rueckmeldung"
        case "Bankueberweisung":
            return "Direkte Ueberweisung"
        default:
            return "Verfuegbare Zahlungsroute"
        }
    }
}

private struct SelectedPaymentMethodInfoCard: View {
    let colorScheme: ColorScheme
    let settings: PaymentMethodSettings
    let selectedMethod: String
    let isZeroCostOrder: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "checkmark.shield")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                Text(paymentTrustLine)
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            switch selectedMethod {
            case "PayPal":
                Text("PayPal startet nach kurzer Bestaetigung durch das Team.")
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                if settings.paypal.accountHint.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text("PayPal-Link wird bei der Rueckmeldung geteilt.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                } else {
                    CartBadge(text: settings.paypal.accountHint, colorScheme: colorScheme)
                }

            case "Bankueberweisung":
                Text("Ueberweisung mit klaren Bankdaten fuer diese Bestellung.")
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                VStack(alignment: .leading, spacing: 8) {
                    if !settings.bankTransfer.accountHolder.isEmpty {
                        paymentInfoLine("Kontoinhaber", settings.bankTransfer.accountHolder)
                    }
                    if !settings.bankTransfer.bankName.isEmpty {
                        paymentInfoLine("Bank", settings.bankTransfer.bankName)
                    }
                    if !settings.bankTransfer.iban.isEmpty {
                        paymentInfoLine("IBAN", settings.bankTransfer.iban)
                    }
                    if !settings.bankTransfer.bic.isEmpty {
                        paymentInfoLine("BIC", settings.bankTransfer.bic)
                    }
                    if !settings.bankTransfer.paymentInstructions.isEmpty {
                        paymentInfoLine("Hinweis", settings.bankTransfer.paymentInstructions)
                    }
                }

            case "Stripe":
                Text(
                    isZeroCostOrder
                        ? "Fuer diesen 0-EUR-Testartikel ist keine Zahlung noetig. Die Bestellung wird direkt bestaetigt."
                        : "Stripe startet danach den sicheren Live-Checkout. Je nach Geraet und Verfuegbarkeit koennen Karte, Apple Pay, Google Pay oder weitere kompatible Zahlarten erscheinen."
                )
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

            case "Klarna":
                Text(
                    isZeroCostOrder
                        ? "Fuer diesen 0-EUR-Testartikel ist keine Zahlung noetig. Die Bestellung wird direkt bestaetigt."
                        : "Klarna wird danach sicher ueber Stripe fortgefuehrt."
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
            return "Kein Zahlbetrag. Direkte Bestellbestaetigung mit klarem Status."
        }
        switch selectedMethod {
        case "Stripe", "Klarna":
            return "Sicherer Checkout mit geschuetzter Weiterleitung."
        case "PayPal", "Bankueberweisung":
            return "Klare Zahlungsroute mit direkter Rueckmeldung."
        default:
            return "Sichere Zahlungsabwicklung."
        }
    }

    @ViewBuilder
    private func paymentInfoLine(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
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
    case login
    case mail(OrderMailDraft)

    var id: String {
        switch self {
        case .login:
            return "login"
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
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Warenkorb")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Ruhig pruefen, sicher abschliessen.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 20)
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
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.14 : 0.08), radius: 12, y: 5)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 12) {
                CartBadge(text: "\(itemCount) Artikel", colorScheme: colorScheme)
                CartBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
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
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.accent(for: colorScheme))
                .padding(.top, 1)
            VStack(alignment: .leading, spacing: 4) {
                Text("Gerade hinzugefuegt")
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
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct CartSectionCard<Content: View>: View {
    let title: String
    let colorScheme: ColorScheme
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            content
        }
        .padding(20)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct CartInlineStatusStrip<Content: View>: View {
    let colorScheme: ColorScheme
    let icon: String
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            content
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .animation(.easeInOut(duration: 0.2), value: title)
    }
}

private struct CartItemCard: View {
    let cartItem: CartItem
    let colorScheme: ColorScheme
    let onQuantityChange: (Int) -> Void
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(cartItem.item.name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(2)

                    HStack(spacing: 8) {
                        CartBadge(text: cartItem.size, colorScheme: colorScheme)
                        if let color = cartItem.color?.takeIfNotBlank() {
                            CartBadge(text: color, colorScheme: colorScheme)
                        }
                        CartBadge(text: "x\(cartItem.quantity)", colorScheme: colorScheme, isEmphasized: true)
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    Text(String(format: "EUR %.2f", cartItem.effectiveUnitPrice * Double(cartItem.quantity)))
                        .font(.headline.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                    Text(String(format: "EUR %.2f / Stk", cartItem.effectiveUnitPrice))
                        .font(.caption2)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            HStack(spacing: 10) {
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
                Button(action: onRemove) {
                    Label("Entfernen", systemImage: "trash")
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Artikel entfernen")
                .accessibilityHint("Entfernt den Artikel aus dem Warenkorb")
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
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
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(title, text: $text)
                .keyboardType(keyboard)
                .textInputAutocapitalization(autocapitalization)
                .padding(14)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                )
                .foregroundColor(AppColors.text(for: colorScheme))
        }
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
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Bestellung bestaetigen")
                    .font(.title3.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text(
                    isZeroCostOrder
                        ? "Fuer diesen 0-EUR-Testartikel wird die Bestellung direkt bestaetigt."
                        : (isHostedCheckout ? "Du wirst danach sicher zum Checkout weitergeleitet." : "Danach uebermitteln wir deine Bestellung sicher an das Team.")
                )
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("\(items.count) Artikel")
                    Spacer()
                    Text("\(totalQuantity)x Gesamtmenge")
                }
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                ForEach(summaryItems) { item in
                    HStack(alignment: .top, spacing: 8) {
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
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            Text("Sicherer Ablauf, klare Rueckmeldung und Support bei Bedarf.")
                .font(.caption.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 10) {
                Button("Abbrechen", action: onCancel)
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 46)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .disabled(isSubmitting)

                Button(action: onConfirm) {
                    if isSubmitting {
                        ProgressView()
                            .tint(.white)
                            .frame(maxWidth: .infinity, minHeight: 46)
                    } else {
                        Text(
                            isZeroCostOrder
                                ? "Bestellung bestaetigen"
                                : (isHostedCheckout ? "Sicher fortfahren" : "Bestellung senden")
                        )
                            .font(.subheadline.weight(.bold))
                            .frame(maxWidth: .infinity, minHeight: 46)
                    }
                }
                .background(AppColors.accent(for: colorScheme))
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .disabled(isSubmitting)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 16)
        .background(AppColors.cardBackground(for: colorScheme))
    }
}

private struct CartCheckoutSafetyZone: View {
    let colorScheme: ColorScheme
    let supportMailbox: String

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            safetyLine("lock.shield.fill", "Sichere Zahlwege und klare Weiterleitung im naechsten Schritt.")
            safetyLine("doc.text.magnifyingglass", "Daten und Gesamtpreis pruefen wir vor dem Senden noch einmal.")
            HStack(alignment: .top, spacing: 8) {
                Image(systemName: "message.badge.fill")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                    .padding(.top, 2)
                    Text("Support direkt erreichbar: \(supportMailbox)")
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
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func safetyLine(_ icon: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
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
        VStack(spacing: 12) {
            Divider()
                .overlay(AppColors.accent(for: colorScheme).opacity(0.12))

            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 5) {
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

                Button(action: onSubmit) {
                    if isSubmitting {
                        HStack(spacing: 8) {
                            ProgressView()
                                .tint(.white)
                                .scaleEffect(0.9)
                            Text("Wird vorbereitet")
                                .font(.subheadline.weight(.semibold))
                        }
                        .frame(minWidth: 132, minHeight: 44)
                        .transition(.opacity.combined(with: .scale(scale: 0.96)))
                    } else {
                        Text(buttonTitle)
                            .font(.subheadline.weight(.bold))
                            .frame(minWidth: 132, minHeight: 44)
                            .transition(.opacity.combined(with: .scale(scale: 0.98)))
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
                .disabled(!isReady || isSubmitting)
                .controlSize(.large)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .animation(.easeInOut(duration: 0.2), value: isSubmitting)
                .accessibilityLabel(isSubmitting ? "Checkout wird vorbereitet" : buttonTitle)
                .accessibilityHint(isSubmitting ? "Bitte kurz warten" : "Oeffnet die finale Bestaetigung")
            }
            .padding(.horizontal, 20)

            HStack(spacing: 6) {
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
        .animation(.easeInOut(duration: 0.2), value: isSubmitting)
    }
}

private struct CartBadge: View {
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
            .animation(.easeInOut(duration: 0.18), value: text)
    }
}
