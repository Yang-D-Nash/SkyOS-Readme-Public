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
    @State private var message = "Ich interessiere mich für die Artikel in meinem Warenkorb."
    @State private var showConfirmationDialog = false
    @State private var isSubmitting = false
    @State private var activePresentedSheet: CartPresentedSheet?
    @State private var queuedPresentedSheet: CartPresentedSheet?
    @State private var selectedPaymentMethod = ""

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
            shippingError: shippingQuote.countryCode == "--" ? "Das Lieferland konnte noch nicht eindeutig erkannt werden." : nil
        )
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

                    CartSectionCard(
                        title: "Zahlungsarten",
                        colorScheme: colorScheme
                    ) {
                        PaymentMethodsCheckoutInfo(
                            colorScheme: colorScheme,
                            settings: paymentMethodSettingsStore.settings,
                            isCheckoutAvailable: isCheckoutAvailable
                        )
                    }

                    if authManager.userSession == nil {
                        CartSectionCard(
                            title: "Konto erforderlich",
                            colorScheme: colorScheme
                        ) {
                            Text("Melde dich an, damit du deinen Warenkorb speichern und die Bestellung abschicken kannst.")
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
                                Text("Dein Warenkorb ist leer. Sobald du etwas hinzufügst, erscheint es hier direkt als Karte.")
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            } else {
                                ForEach(cartVM.items) { cartItem in
                                    CartItemCard(
                                        cartItem: cartItem,
                                        colorScheme: colorScheme
                                    ) {
                                        cartVM.removeItem(cartItem)
                                    }
                                }
                            }
                        }

                        CartSectionCard(
                            title: "Kontaktdaten",
                            colorScheme: colorScheme
                        ) {
                            Text("Diese Angaben helfen uns beim Rückkontakt zu deiner Bestellung.")
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            VStack(spacing: 10) {
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
                                CartInputField(
                                    title: "WhatsApp (optional)",
                                    text: $whatsApp,
                                    colorScheme: colorScheme,
                                    keyboard: .phonePad
                                )
                            }
                            .padding(.top, 4)
                        }

                        CartSectionCard(
                            title: "Lieferadresse",
                            colorScheme: colorScheme
                        ) {
                            Text("Die Versandadresse wird fuer Rueckmeldung, Versand und Bestellabwicklung benoetigt.")
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            VStack(spacing: 10) {
                                CartInputField(
                                    title: "Strasse und Hausnummer*",
                                    text: $shippingStreet,
                                    colorScheme: colorScheme
                                )
                                CartInputField(
                                    title: "Adresszusatz (optional)",
                                    text: $shippingAddressExtra,
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
                                CartInputField(
                                    title: "Land",
                                    text: $shippingCountry,
                                    colorScheme: colorScheme
                                )
                            }
                            .padding(.top, 4)
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

                        if !isCheckoutAvailable {
                            CartSectionCard(
                                title: "Checkout pausiert",
                                colorScheme: colorScheme
                            ) {
                                Text("Der Merchandise-Store ist gerade pausiert. Deine Auswahl bleibt sichtbar, aber neue Bestellungen werden erst wieder freigeschaltet, sobald der Store geoeffnet ist.")
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                        } else if !availableCheckoutMethods.isEmpty {
                            CartSectionCard(
                                title: "Zahlart waehlen",
                                colorScheme: colorScheme
                            ) {
                                PaymentMethodSelectionCard(
                                    colorScheme: colorScheme,
                                    methods: availableCheckoutMethods,
                                    selectedMethod: $selectedPaymentMethod
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
                                        selectedMethod: selectedPaymentMethod
                                    )
                                }
                            }
                        }

                        CartSectionCard(
                            title: "Nachricht",
                            colorScheme: colorScheme
                        ) {
                            Text("Optional für Hinweise zu Lieferung, Verfügbarkeit oder Sonderwünschen.")
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            TextEditor(text: $message)
                                .frame(minHeight: 120)
                                .padding(12)
                                .background(AppColors.secondaryBackground(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 18))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18)
                                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                                )
                                .padding(.top, 4)
                        }
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
                        totalPrice: pricingSummary.total,
                        title: isHostedCheckoutSelection ? "Sicherer Checkout" : "Bestellung abschicken",
                        buttonTitle: isHostedCheckoutSelection ? "Zum Checkout" : "Senden",
                        isFormValid: canSubmitOrder,
                        isSubmitting: isSubmitting
                    ) {
                        showConfirmationDialog = true
                    }
                }
            }
            .onAppear {
                populateContactDetails()
                syncSelectedPaymentMethod(with: availableCheckoutMethods)
            }
            .onReceive(paymentMethodSettingsStore.$settings) { settings in
                syncSelectedPaymentMethod(with: settings.checkoutMethodLabels)
            }
            .onChange(of: hostedCheckoutRedirectStore.latestEvent) { _, event in
                guard let event else { return }

                switch event.status {
                case .success:
                    cartVM.showUserToast("Checkout abgeschlossen. Zahlung wird jetzt synchronisiert.", style: .success)
                case .cancel:
                    cartVM.showUserToast("Checkout abgebrochen. Die Bestellung bleibt unbezahlt.", style: .info)
                }

                hostedCheckoutRedirectStore.clear()
            }
        }
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
        .confirmationDialog(
            isHostedCheckoutSelection ? "Zum sicheren Checkout" : "Bestellung abschicken",
            isPresented: $showConfirmationDialog,
            titleVisibility: .visible
        ) {
            Button("Einverstanden") {
                Task {
                    await submitOrderAsync()
                }
            }
            Button("Abbrechen", role: .cancel) {}
        } message: {
            Text(
                isHostedCheckoutSelection
                    ? "Du wirst jetzt zu Stripe Checkout weitergeleitet und schliesst die Zahlung dort sicher ab."
                    : "Sie werden in den naechsten Minuten per E-Mail oder WhatsApp kontaktiert."
            )
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

    private func submitOrderAsync() async {
        guard !isSubmitting else { return }
        isSubmitting = true

        guard availableCheckoutMethods.isEmpty || !selectedPaymentMethod.isEmpty else {
            cartVM.showUserToast("Bitte waehle zuerst eine Zahlart aus.", style: .error)
            isSubmitting = false
            return
        }
        if let shippingError = pricingSummary.shippingError {
            cartVM.showUserToast(shippingError, style: .error)
            isSubmitting = false
            return
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
            }

            isSubmitting = false
            return
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
        }

        isSubmitting = false
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
        Hallo 22xSky-Team,

        es wurde eine neue Bestellung in 22xSky vorbereitet.

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
            cartVM.showUserToast("Mail-App konnte nicht vorbereitet werden.", style: .error)
            return
        }

        openURL(url) { accepted in
            if !accepted {
                cartVM.showUserToast("Mail-App konnte nicht geoeffnet werden.", style: .error)
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

private struct PaymentMethodsCheckoutInfo: View {
    let colorScheme: ColorScheme
    let settings: PaymentMethodSettings
    let isCheckoutAvailable: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if !isCheckoutAvailable {
                Text("Der Merchandise-Store ist aktuell pausiert. Zahlarten und Checkout werden erst wieder aktiv, sobald der Store geoeffnet ist.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else if settings.checkoutMethodLabels.isEmpty {
                Text("Aktuell ist noch keine Zahlart fuer Kunden sichtbar. Der Merch-Checkout bleibt bis dahin auf Anfrage und Rueckkontakt ausgelegt.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else {
                Text("Diese Zahlarten sind aktuell aktiv:")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                HStack(spacing: 8) {
                    ForEach(settings.checkoutMethodLabels, id: \.self) { method in
                        CartBadge(text: method, colorScheme: colorScheme)
                    }
                }

                if settings.bankTransfer.enabled && settings.bankTransfer.isConfigured {
                    Text("Bankdaten und genaue Anweisung folgen nach der Bestellbestaetigung direkt durch das Team.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                } else {
                    Text("Stripe und Klarna laufen als sicherer Live-Checkout. PayPal und Bankueberweisung bleiben manuell owner-geprueft.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }
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
                Text(shippingError)
                    .font(.footnote)
                    .foregroundColor(.red)
            }

            if !shippingNote.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(shippingNote)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Text("Rechnung und Rueckmeldung laufen ueber \(companyName.takeIfNotBlank() ?? "Skydown Entertainment").")
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

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Waehle die Zahlart fuer diese Bestellung.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: 132), spacing: 10, alignment: .leading)],
                alignment: .leading,
                spacing: 10
            ) {
                ForEach(methods, id: \.self) { method in
                    Button {
                        selectedMethod = method
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: selectedMethod == method ? "checkmark.circle.fill" : "circle")
                                .font(.subheadline.weight(.semibold))
                            Text(method)
                                .font(.subheadline.weight(.semibold))
                                .lineLimit(1)
                        }
                        .foregroundColor(
                            selectedMethod == method
                                ? AppColors.text(for: colorScheme)
                                : AppColors.secondaryText(for: colorScheme)
                        )
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: 18)
                                .fill(
                                    selectedMethod == method
                                        ? AppColors.accent(for: colorScheme).opacity(0.14)
                                        : AppColors.secondaryBackground(for: colorScheme)
                                )
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(
                                    selectedMethod == method
                                        ? AppColors.accent(for: colorScheme)
                                        : AppColors.accent(for: colorScheme).opacity(0.10),
                                    lineWidth: 1
                                )
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                }
            }

            if selectedMethod == "Klarna" {
                Text("Klarna oeffnet nach dem Absenden einen sicheren Live-Checkout ueber Stripe.")
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
    }
}

private struct SelectedPaymentMethodInfoCard: View {
    let colorScheme: ColorScheme
    let settings: PaymentMethodSettings
    let selectedMethod: String

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            switch selectedMethod {
            case "PayPal":
                Text("PayPal wird hier als sicherer manueller Handoff genutzt. Fuer einen direkten Flow hinterlege am besten einen PayPal.Me-Link.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                if settings.paypal.accountHint.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text("Im Admin-Bereich ist noch kein PayPal.Me-Link oder keine Business-Mail hinterlegt.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                } else {
                    CartBadge(text: settings.paypal.accountHint, colorScheme: colorScheme)
                }

            case "Bankueberweisung":
                Text("Die Bankueberweisung laeuft direkt und ohne Gateway-Kosten. Die hinterlegten Daten gelten fuer diese Bestellung.")
                    .font(.body)
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
                Text("Stripe startet nach dem Absenden einen sicheren Live-Checkout fuer Kartenzahlungen.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

            case "Klarna":
                Text("Klarna startet nach dem Absenden einen sicheren Live-Checkout ueber Stripe und bestaetigt die Zahlung automatisch im Backend.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

            default:
                EmptyView()
            }
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
                Text("Checkout bereit")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Der Warenkorb folgt jetzt derselben klaren Karten- und Section-Struktur wie Music, Bot und Settings.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 18)
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
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 10) {
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

private struct CartSectionCard<Content: View>: View {
    let title: String
    let colorScheme: ColorScheme
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            content
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct CartItemCard: View {
    let cartItem: CartItem
    let colorScheme: ColorScheme
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(cartItem.item.name)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    HStack(spacing: 8) {
                        CartBadge(text: "Größe \(cartItem.size)", colorScheme: colorScheme)
                        if let color = cartItem.color?.takeIfNotBlank() {
                            CartBadge(text: color, colorScheme: colorScheme)
                        }
                        CartBadge(text: "x\(cartItem.quantity)", colorScheme: colorScheme)
                    }
                }

                Spacer()

                Text(String(format: "EUR %.2f", cartItem.effectiveUnitPrice * Double(cartItem.quantity)))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }

            Button(role: .destructive, action: onRemove) {
                Label("Entfernen", systemImage: "trash")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
        }
        .padding(14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
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
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                )
                .foregroundColor(AppColors.text(for: colorScheme))
        }
    }
}

private struct CartSubmitBar: View {
    let colorScheme: ColorScheme
    let totalPrice: Double
    let title: String
    let buttonTitle: String
    let isFormValid: Bool
    let isSubmitting: Bool
    let onSubmit: () -> Void

    var body: some View {
        VStack(spacing: 10) {
            Divider()
                .overlay(AppColors.accent(for: colorScheme).opacity(0.12))

            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(totalPrice > 0 ? String(format: "EUR %.2f gesamt", totalPrice) : "Warenkorb aktuell leer")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                Button(action: onSubmit) {
                    if isSubmitting {
                        ProgressView()
                            .tint(.white)
                            .frame(minWidth: 110)
                    } else {
                        Text(buttonTitle)
                            .font(.headline)
                            .frame(minWidth: 110)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
                .disabled(!isFormValid || isSubmitting)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .background(AppColors.cardBackground(for: colorScheme).opacity(0.98))
    }
}

private struct CartBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.accent(for: colorScheme)
        )
    }
}
