//
//  SettingsView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

// swiftlint:disable file_length
// swiftlint:disable multiple_closures_with_trailing_closure

import SwiftUI
import MessageUI

struct SettingsView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var environmentColorScheme

    @StateObject private var aiVisualReferenceLibrary = AIVisualReferenceLibraryStore.shared
    @StateObject private var commerceSettingsStore = CommerceSettingsStore.shared
    @StateObject private var merchStoreStatusStore = MerchStoreStatusStore.shared
    @StateObject private var paymentMethodSettingsStore = PaymentMethodSettingsStore.shared
    @StateObject private var shopifyAdminSettingsStore = ShopifyAdminSettingsStore.shared
    @StateObject private var workflowAutomationSettings = WorkflowAutomationSettingsStore.shared
    @Binding var colorScheme: String

    @State private var language = "Deutsch"
    @State private var notificationsEnabled = true

    @State private var activeAlert: SettingsAlert?
    @State private var showingLoginSheet = false
    @State private var showingRegistrationSheet = false
    @State private var showingTermsAndConditions = false
    @State private var showingPrivacyPolicy = false
    @State private var showingTermsOfService = false
    @State private var showingOrders = false
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success
    @State private var presentedAdminWorkspace: SettingsAdminWorkspaceSection?
    @State private var showingMailOptions = false
    @State private var showingMailView = false
    @State private var stripeAccountHintDraft = ""
    @State private var paypalAccountHintDraft = ""
    @State private var klarnaAccountHintDraft = ""
    @State private var bankAccountHolderDraft = ""
    @State private var bankIbanDraft = ""
    @State private var bankBicDraft = ""
    @State private var bankNameDraft = ""
    @State private var bankInstructionsDraft = ""
    @State private var domesticShippingDraft = ""
    @State private var euShippingDraft = ""
    @State private var internationalShippingDraft = ""
    @State private var freeShippingThresholdDraft = ""
    @State private var shippingNotesDraft = ""
    @State private var invoiceCompanyNameDraft = ""
    @State private var invoiceCompanyAddressDraft = ""
    @State private var invoiceTaxNumberDraft = ""
    @State private var invoiceVatIdDraft = ""
    @State private var invoiceTaxRateDraft = ""
    @State private var invoicePrefixDraft = ""
    @State private var invoiceSupportEmailDraft = ""
    @State private var shopifyStoreDomainDraft = ""
    @State private var shopifyStorefrontAccessTokenDraft = ""
    @State private var shopifyCollectionHandleDraft = ""

    private var effectiveColorScheme: ColorScheme {
        switch colorScheme {
        case "light": return .light
        case "dark": return .dark
        default: return environmentColorScheme
        }
    }

    private var appVersion: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String
        guard let build, !build.isEmpty, build != version else {
            return version
        }
        return "\(version) (\(build))"
    }

    private var shopifyCatalogURL: URL? {
        let normalizedDomain = shopifyAdminSettingsStore.settings.storeDomain
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "https://", with: "")
            .replacingOccurrences(of: "http://", with: "")
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !normalizedDomain.isEmpty else { return nil }

        let handle = shopifyAdminSettingsStore.settings.collectionHandle
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let path = handle.isEmpty ? "" : "/collections/\(handle)"
        return URL(string: "https://\(normalizedDomain)\(path)")
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    SettingsHeroCard(
                        colorScheme: effectiveColorScheme,
                        username: authManager.userSession?.username,
                        isLoggedIn: authManager.userSession != nil,
                        isAdmin: authManager.userSession?.isAdmin == true,
                        notificationsEnabled: notificationsEnabled,
                        appearance: currentAppearanceLabel
                    )

                    SettingsSectionCard(title: "Konto", colorScheme: effectiveColorScheme) {
                        if let user = authManager.userSession {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Angemeldet als \(user.username)")
                                    .font(.headline)
                                    .foregroundColor(AppColors.text(for: effectiveColorScheme))

                                Text(user.email)
                                    .font(.subheadline)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                                Text("Du kannst dich hier abmelden oder direkt mit einem anderen Konto neu anmelden.")
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                                VStack(spacing: 10) {
                                    Button(role: .destructive) {
                                        activeAlert = .logout
                                    } label: {
                                        Label("Abmelden", systemImage: "person.crop.circle.badge.xmark")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.borderedProminent)

                                    Button {
                                        Task {
                                            await authManager.signOut()
                                            showingLoginSheet = true
                                        }
                                    } label: {
                                        Text("Mit anderem Konto anmelden")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.bordered)

                                    Button(role: .destructive) {
                                        activeAlert = .deleteAccount
                                    } label: {
                                        Label("Konto loeschen", systemImage: "person.fill.xmark")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.bordered)
                                }
                            }
                        } else {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Melde dich an oder registriere dich, um Bestellungen und persoenliche Bereiche freizuschalten.")
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                                Button {
                                    showingLoginSheet = true
                                } label: {
                                    Label("Anmelden", systemImage: "person.crop.circle.fill.badge.plus")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(AppColors.accent(for: effectiveColorScheme))

                                Button {
                                    showingRegistrationSheet = true
                                } label: {
                                    Label("Registrieren", systemImage: "person.crop.circle.badge.plus")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.bordered)
                                .tint(AppColors.accentMystic(for: effectiveColorScheme))
                            }
                        }
                    }

                    adminWorkspaceSectionCard

                    SettingsSectionCard(title: "Allgemein", colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Sprache")
                                    .foregroundColor(AppColors.text(for: effectiveColorScheme))
                                Spacer()
                                Text(language)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                            }

                            SettingsToggleCard(
                                colorScheme: effectiveColorScheme,
                                title: "Benachrichtigungen",
                                subtitle: "Hinweise fuer Updates und wichtige App-Aktionen.",
                                isOn: $notificationsEnabled
                            )
                        }
                    }

                    SettingsSectionCard(title: "Anzeige", colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Aktuell: \(currentAppearanceLabel)")
                                .font(.subheadline)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                            ForEach(Appearance.allCases) { appearance in
                                AppearanceChoiceCard(
                                    colorScheme: effectiveColorScheme,
                                    title: appearance.rawValue.capitalized,
                                    isSelected: colorScheme == appearance.rawValue
                                ) {
                                    colorScheme = appearance.rawValue
                                }
                            }
                        }
                    }

                    SettingsSectionCard(title: "App-Info", colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Version \(appVersion)")
                                .font(.headline)
                                .foregroundColor(AppColors.text(for: effectiveColorScheme))

                            Button("AGB") {
                                showingTermsAndConditions = true
                            }
                            .buttonStyle(.bordered)

                            Button("Datenschutzbestimmungen") {
                                showingPrivacyPolicy = true
                            }
                            .buttonStyle(.bordered)

                            Button("Nutzungsbedingungen") {
                                showingTermsOfService = true
                            }
                            .buttonStyle(.bordered)

                            VStack(alignment: .leading, spacing: 8) {
                                Text("Support")
                                    .font(.headline)
                                    .foregroundColor(AppColors.text(for: effectiveColorScheme))

                                Text("skydownent@gmail.com")
                                    .font(.subheadline)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                            }

                            Button {
                                #if targetEnvironment(simulator)
                                if MFMailComposeViewController.canSendMail() {
                                    showingMailView = true
                                } else {
                                    showToastMessage("Mail kann im Simulator nicht gesendet werden", style: .error)
                                }
                                #else
                                showingMailOptions = true
                                #endif
                            } label: {
                                Label("Support-Anfrage senden", systemImage: "envelope.fill")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(AppColors.accent(for: effectiveColorScheme))

                            Text("Rechtstexte und Support-Infos sind hier direkt aus der App erreichbar.")
                                .font(.footnote)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                        }
                    }
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding)
            }
            .scrollIndicators(.hidden)
            .navigationTitle("Einstellungen")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Label("Schliessen", systemImage: "xmark")
                    }
                }
            }
            .background(
                AppColors.screenGradient(
                    for: effectiveColorScheme,
                    secondaryAccent: AppColors.accentHighlight(for: effectiveColorScheme)
                )
                .ignoresSafeArea()
            )
            .skydownNavigationChrome(colorScheme: effectiveColorScheme)
            .environment(\.colorScheme, effectiveColorScheme)
        }
        .sheet(isPresented: $showingLoginSheet) {
            LoginView()
        }
        .sheet(isPresented: $showingRegistrationSheet) {
            RegistrationSheet()
        }
        .sheet(isPresented: $showingOrders) {
            OrderView()
        }
        .sheet(isPresented: $showingTermsAndConditions) {
            PolicyView(title: "AGB", text: .termsAndConditionsText)
        }
        .sheet(isPresented: $showingPrivacyPolicy) {
            PolicyView(title: "Datenschutzbestimmungen", text: .privacyPolicyText)
        }
        .sheet(isPresented: $showingTermsOfService) {
            PolicyView(title: "Nutzungsbedingungen", text: .termsOfServiceText)
        }
        .sheet(item: $presentedAdminWorkspace) { section in
            NavigationStack {
                ScrollView {
                    adminWorkspaceContent(for: section)
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        .padding(.top, SkydownLayout.screenTopPadding * 0.75)
                        .padding(.bottom, SkydownLayout.screenBottomPadding)
                }
                .scrollIndicators(.hidden)
                .background(
                    AppColors.screenGradient(
                        for: effectiveColorScheme,
                        secondaryAccent: AppColors.accentHighlight(for: effectiveColorScheme)
                    )
                    .ignoresSafeArea()
                )
                .navigationTitle(section.rawValue)
                .navigationBarTitleDisplayMode(.inline)
                .skydownNavigationChrome(colorScheme: effectiveColorScheme)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button {
                            presentedAdminWorkspace = nil
                        } label: {
                            Label("Schliessen", systemImage: "xmark")
                        }
                    }
                }
            }
            .environment(\.colorScheme, effectiveColorScheme)
        }
        .confirmationDialog(
            "Support-Anfrage senden",
            isPresented: $showingMailOptions,
            titleVisibility: .visible
        ) {
            Button("In-App senden") {
                if MFMailComposeViewController.canSendMail() {
                    showingMailView = true
                } else {
                    showToastMessage("Mail kann auf diesem Geraet nicht gesendet werden", style: .error)
                }
            }

            Button("Mail-App oeffnen") {
                openMailAppFallback()
            }

            Button("Abbrechen", role: .cancel) {}
        }
        .sheet(isPresented: $showingMailView) {
            MailView(
                subject: supportMailSubject,
                body: supportMailBody,
                recipients: [supportMailbox],
                preferredSendingEmailAddress: preferredSupportSenderEmail
            )
        }
        .alert(item: $activeAlert) { alert in
            switch alert {
            case .logout:
                return Alert(
                    title: Text("Abmelden"),
                    message: Text("Moechten Sie sich wirklich abmelden?"),
                    primaryButton: .destructive(Text("Abmelden")) {
                        Task { await authManager.signOut() }
                    },
                    secondaryButton: .cancel()
                )
            case .deleteAccount:
                return Alert(
                    title: Text("Konto loeschen"),
                    message: Text("Moechten Sie Ihr Konto unwiderruflich loeschen?"),
                    primaryButton: .destructive(Text("Konto loeschen")) {
                        Task {
                            do {
                                try await authManager.deleteAccount()
                                showToastMessage("Konto erfolgreich geloescht", style: .success)
                            } catch {
                                showToastMessage("Fehler beim Loeschen: \(error.localizedDescription)", style: .error)
                            }
                        }
                    },
                    secondaryButton: .cancel()
                )
            }
        }
        .fancyToast(isPresented: $showToast, message: toastMessage, style: toastStyle)
        .onAppear {
            syncPaymentDrafts(with: paymentMethodSettingsStore.settings)
            syncCommerceDrafts(with: commerceSettingsStore.settings)
            syncShopifyDrafts(with: shopifyAdminSettingsStore.settings)
        }
        .onReceive(paymentMethodSettingsStore.$settings) { settings in
            syncPaymentDrafts(with: settings)
        }
        .onReceive(commerceSettingsStore.$settings) { settings in
            syncCommerceDrafts(with: settings)
        }
        .onReceive(shopifyAdminSettingsStore.$settings) { settings in
            syncShopifyDrafts(with: settings)
        }
    }

    private var currentAppearanceLabel: String {
        Appearance(rawValue: colorScheme)?.rawValue.capitalized ?? "System"
    }

    private var isAdminUser: Bool {
        authManager.userSession?.isAdmin == true
    }

    private var connectedPaymentMethodCount: Int {
        var count = 0
        if paymentMethodSettingsStore.settings.stripe.connected { count += 1 }
        if paymentMethodSettingsStore.settings.paypal.connected { count += 1 }
        if paymentMethodSettingsStore.settings.klarna.connected { count += 1 }
        if paymentMethodSettingsStore.settings.bankTransfer.isConfigured { count += 1 }
        return count
    }

    private var visiblePaymentMethodCount: Int {
        var count = 0
        if paymentMethodSettingsStore.settings.stripe.connected && paymentMethodSettingsStore.settings.stripe.enabled { count += 1 }
        if paymentMethodSettingsStore.settings.paypal.connected && paymentMethodSettingsStore.settings.paypal.enabled { count += 1 }
        if paymentMethodSettingsStore.settings.klarna.connected && paymentMethodSettingsStore.settings.klarna.enabled { count += 1 }
        if paymentMethodSettingsStore.settings.bankTransfer.isConfigured && paymentMethodSettingsStore.settings.bankTransfer.enabled { count += 1 }
        return count
    }

    @ViewBuilder
    private var adminWorkspaceSectionCard: some View {
        SettingsSectionCard(title: "Admin", colorScheme: effectiveColorScheme) {
            VStack(alignment: .leading, spacing: 14) {
                Text(isAdminUser ? "Die Admin-Bereiche sind jetzt wie kurze Stationen aufgebaut. Du gehst direkt in Zahlungen, Versand oder Visuals rein, statt alles in einer langen Seite aufzuklappen." : "Admin-Bereiche werden erst mit passender Berechtigung aktiv.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                Button {
                    showingOrders = true
                } label: {
                    Label("Bestellungen oeffnen", systemImage: "suitcase.cart")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .disabled(!isAdminUser)

                if isAdminUser {
                    VStack(spacing: 10) {
                        ForEach(SettingsAdminWorkspaceSection.allCases) { section in
                            SettingsAdminWorkspaceListRow(
                                section: section,
                                colorScheme: effectiveColorScheme,
                                detailText: adminWorkspaceStatusText(for: section)
                            ) {
                                presentedAdminWorkspace = section
                            }
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func adminWorkspaceContent(for section: SettingsAdminWorkspaceSection) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            SettingsAdminWorkspaceSummaryCard(
                section: section,
                colorScheme: effectiveColorScheme
            )

            switch section {
            case .overview:
                VStack(alignment: .leading, spacing: 12) {
                    Text("Heute im Blick")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: effectiveColorScheme))

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            SettingsBadge(
                                text: merchStoreStatusStore.status.isOpen ? "Store offen" : "Store pausiert",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: "\(connectedPaymentMethodCount) Zahlarten verbunden",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: "\(visiblePaymentMethodCount) im Checkout sichtbar",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: shopifyAdminSettingsStore.settings.hasCollectionFilter
                                    ? "Shopify: \(shopifyAdminSettingsStore.settings.activeCollectionLabel)"
                                    : "Shopify: Gesamter Store",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: aiVisualReferenceLibrary.settings.isEnabled ? "Visuals aktiv" : "Visuals aus",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: workflowAutomationSettings.settings.isPrepared ? "Automation vorbereitet" : "Automation offen",
                                colorScheme: effectiveColorScheme
                            )
                        }
                        .padding(.vertical, 2)
                    }

                    Text("Jeder Bereich oeffnet sich jetzt separat. So bleibt die Settings-Seite kurz und du bist schneller genau da, wo du arbeiten willst.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                }

            case .shopify:
                VStack(alignment: .leading, spacing: 14) {
                    Text("Fuer den Merch-Katalog braucht die App nur die Store-Domain, deinen Storefront Access Token und optional einen Collection-Handle. Danach laedt der Shop direkt aus Shopify.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    SettingsInputField(
                        title: "Store-Domain",
                        text: $shopifyStoreDomainDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "k5t1sc-ps.myshopify.com"
                    )

                    SettingsInputField(
                        title: "Storefront Access Token",
                        text: $shopifyStorefrontAccessTokenDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "shpat_... oder storefront token",
                        keyboardType: .asciiCapable
                    )

                    SettingsInputField(
                        title: "Collection-Handle",
                        text: $shopifyCollectionHandleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. spring-drop-2026"
                    )

                    Text("Den Collection-Handle kannst du leer lassen, dann nimmt die App den ganzen veroeffentlichten Store.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    SettingsBadge(
                        text: shopifyAdminSettingsStore.settings.hasCollectionFilter
                            ? "Aktuell: \(shopifyAdminSettingsStore.settings.activeCollectionLabel)"
                            : "Aktuell: Gesamter Shopify-Store",
                        colorScheme: effectiveColorScheme
                    )

                    HStack(spacing: 10) {
                        Button(action: saveShopifyAdminSettings) {
                            Label("Shopify speichern", systemImage: "shippingbox.and.arrow.backward")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.accent(for: effectiveColorScheme))

                        if let url = shopifyCatalogURL {
                            Button {
                                UIApplication.shared.open(url)
                            } label: {
                                Label("Link oeffnen", systemImage: "arrow.up.right.square")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                }

            case .payments:
                VStack(alignment: .leading, spacing: 14) {
                    Text("PayPal und Bankueberweisung kannst du sofort als manuellen Checkout-Handoff nutzen. Stripe und Klarna bleiben vorerst vorbereitete Live-Provider fuer spaeter.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    PaymentProviderSettingsCard(
                        colorScheme: effectiveColorScheme,
                        title: "Stripe",
                        statusText: paymentMethodSettingsStore.settings.stripe.connected ? "Verbunden" : "Nicht verbunden",
                        checkoutVisible: paymentMethodSettingsStore.settings.stripe.connected && paymentMethodSettingsStore.settings.stripe.enabled,
                        accountHintTitle: "Stripe Konto / Workspace",
                        accountHintPlaceholder: "z. B. Skydown Merch Workspace",
                        accountHint: $stripeAccountHintDraft,
                        actionTitle: paymentMethodSettingsStore.settings.stripe.connected ? "Verbindung aktualisieren" : "Mit Stripe verbinden",
                        secondaryActionTitle: paymentMethodSettingsStore.settings.stripe.connected ? "Trennen" : nil,
                        onPrimaryAction: { saveStripeConnection() },
                        onSecondaryAction: paymentMethodSettingsStore.settings.stripe.connected ? { disconnectStripe() } : nil
                    ) { isVisible in
                        setCheckoutVisibility(
                            keyPath: \.stripe,
                            isVisible: isVisible,
                            providerName: "Stripe"
                        )
                    }

                    PaymentProviderSettingsCard(
                        colorScheme: effectiveColorScheme,
                        title: "PayPal",
                        statusText: paymentMethodSettingsStore.settings.paypal.connected ? "Hinterlegt" : "Noch nicht hinterlegt",
                        checkoutVisible: paymentMethodSettingsStore.settings.paypal.connected && paymentMethodSettingsStore.settings.paypal.enabled,
                        accountHintTitle: "PayPal.Me Link oder Business-Mail",
                        accountHintPlaceholder: "z. B. https://paypal.me/deinname",
                        accountHint: $paypalAccountHintDraft,
                        actionTitle: paymentMethodSettingsStore.settings.paypal.connected ? "PayPal aktualisieren" : "PayPal hinterlegen",
                        secondaryActionTitle: paymentMethodSettingsStore.settings.paypal.connected ? "Entfernen" : nil,
                        onPrimaryAction: { savePayPalConnection() },
                        onSecondaryAction: paymentMethodSettingsStore.settings.paypal.connected ? { disconnectPayPal() } : nil
                    ) { isVisible in
                        setCheckoutVisibility(
                            keyPath: \.paypal,
                            isVisible: isVisible,
                            providerName: "PayPal"
                        )
                    }

                    PaymentProviderSettingsCard(
                        colorScheme: effectiveColorScheme,
                        title: "Klarna",
                        statusText: paymentMethodSettingsStore.settings.klarna.connected ? "Verbunden" : "Nicht verbunden",
                        checkoutVisible: paymentMethodSettingsStore.settings.klarna.connected && paymentMethodSettingsStore.settings.klarna.enabled,
                        accountHintTitle: "Klarna Merchant / Store ID",
                        accountHintPlaceholder: "z. B. Klarna Merchant EU",
                        accountHint: $klarnaAccountHintDraft,
                        actionTitle: paymentMethodSettingsStore.settings.klarna.connected ? "Verbindung aktualisieren" : "Mit Klarna verbinden",
                        secondaryActionTitle: paymentMethodSettingsStore.settings.klarna.connected ? "Trennen" : nil,
                        onPrimaryAction: { saveKlarnaConnection() },
                        onSecondaryAction: paymentMethodSettingsStore.settings.klarna.connected ? { disconnectKlarna() } : nil
                    ) { isVisible in
                        setCheckoutVisibility(
                            keyPath: \.klarna,
                            isVisible: isVisible,
                            providerName: "Klarna"
                        )
                    }

                    BankTransferSettingsCard(
                        colorScheme: effectiveColorScheme,
                        isConfigured: paymentMethodSettingsStore.settings.bankTransfer.isConfigured,
                        checkoutVisible: paymentMethodSettingsStore.settings.bankTransfer.enabled && paymentMethodSettingsStore.settings.bankTransfer.isConfigured,
                        accountHolder: $bankAccountHolderDraft,
                        iban: $bankIbanDraft,
                        bic: $bankBicDraft,
                        bankName: $bankNameDraft,
                        paymentInstructions: $bankInstructionsDraft,
                        onSave: { saveBankTransferDetails() },
                        onToggleCheckoutVisible: { isVisible in
                            setBankTransferVisibility(isVisible)
                        }
                    )
                }

            case .commerce:
                VStack(alignment: .leading, spacing: 14) {
                    Text("Der Checkout nutzt diese Werte direkt fuer Versand, MwSt.-Ausweisung und vorbereitete Bestellsummen. Der Store-Schalter aus Merchandise bleibt dabei die harte Freigabe fuer Kunden.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    HStack(spacing: 10) {
                        SettingsBadge(
                            text: merchStoreStatusStore.status.isOpen ? "Store offen" : "Store pausiert",
                            colorScheme: effectiveColorScheme
                        )
                        SettingsBadge(
                            text: commerceSettingsStore.settings.invoice.supportEmail.takeIfNotBlank() ?? "Support offen",
                            colorScheme: effectiveColorScheme
                        )
                    }

                    Text("Versand")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: effectiveColorScheme))

                    SettingsInputField(
                        title: "Versand Deutschland (EUR)",
                        text: $domesticShippingDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. 4.90",
                        keyboardType: .decimalPad
                    )

                    SettingsInputField(
                        title: "Versand EU (EUR)",
                        text: $euShippingDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. 6.90",
                        keyboardType: .decimalPad
                    )

                    SettingsInputField(
                        title: "Versand International (EUR)",
                        text: $internationalShippingDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. 11.90",
                        keyboardType: .decimalPad
                    )

                    SettingsInputField(
                        title: "Versand frei ab (EUR)",
                        text: $freeShippingThresholdDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. 89.00",
                        keyboardType: .decimalPad
                    )

                    SettingsInputField(
                        title: "Versandhinweis",
                        text: $shippingNotesDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Versand innerhalb von 3-5 Werktagen"
                    )

                    Divider()
                        .padding(.vertical, 4)

                    Text("Rechnung")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: effectiveColorScheme))

                    SettingsInputField(
                        title: "Firmenname",
                        text: $invoiceCompanyNameDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Skydown Entertainment"
                    )

                    SettingsInputField(
                        title: "Firmenadresse",
                        text: $invoiceCompanyAddressDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Strasse, PLZ Ort"
                    )

                    SettingsInputField(
                        title: "Steuernummer",
                        text: $invoiceTaxNumberDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "optional"
                    )

                    SettingsInputField(
                        title: "USt-IdNr.",
                        text: $invoiceVatIdDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "optional"
                    )

                    HStack(spacing: 10) {
                        SettingsInputField(
                            title: "MwSt. Satz (%)",
                            text: $invoiceTaxRateDraft,
                            colorScheme: effectiveColorScheme,
                            placeholder: "19.0",
                            keyboardType: .decimalPad
                        )

                        SettingsInputField(
                            title: "Rechnungs-Praefix",
                            text: $invoicePrefixDraft,
                            colorScheme: effectiveColorScheme,
                            placeholder: "SD"
                        )
                    }

                    SettingsInputField(
                        title: "Support / Rechnungs-Mail",
                        text: $invoiceSupportEmailDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "skydownent@gmail.com",
                        keyboardType: .emailAddress
                    )

                    Button {
                        saveCommerceSettings()
                    } label: {
                        Label("Versand & Rechnung speichern", systemImage: "shippingbox.and.arrow.backward")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accentMystic(for: effectiveColorScheme))
                }

            case .visuals:
                VStack(alignment: .leading, spacing: 12) {
                    Toggle(
                        "Visual Reference Pack aktiv",
                        isOn: Binding(
                            get: { aiVisualReferenceLibrary.settings.isEnabled },
                            set: { isEnabled in
                                aiVisualReferenceLibrary.update { settings in
                                    settings.isEnabled = isEnabled
                                }
                            }
                        )
                    )

                    Text("Lokal auf diesem Admin-Geraet gespeichert. Drive-Link und Referenzhinweise helfen der KI bei Benennung und Stilrichtung; echtes Drive-Sync folgt spaeter.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    SettingsInputField(
                        title: "Drive- oder Asset-Link",
                        text: Binding(
                            get: { aiVisualReferenceLibrary.settings.storageLink },
                            set: { value in
                                aiVisualReferenceLibrary.update { settings in
                                    settings.storageLink = value
                                }
                            }
                        ),
                        colorScheme: effectiveColorScheme,
                        placeholder: "https://drive.google.com/..."
                    )

                    SettingsInputField(
                        title: "Benennungs-Praefix",
                        text: Binding(
                            get: { aiVisualReferenceLibrary.settings.namingPrefix },
                            set: { value in
                                aiVisualReferenceLibrary.update { settings in
                                    settings.namingPrefix = value
                                }
                            }
                        ),
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. skydown_drop_"
                    )

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Referenzhinweise")
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: effectiveColorScheme))

                        Text("Bis zu 5 kurze Hinweise fuer Charaktere, Elemente, Moodboards oder Shots.")
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                        ForEach(Array(aiVisualReferenceLibrary.settings.referenceHints.indices), id: \.self) { index in
                            SettingsInputField(
                                title: "Referenz \(index + 1)",
                                text: Binding(
                                    get: { aiVisualReferenceLibrary.settings.referenceHints[index] },
                                    set: { value in
                                        aiVisualReferenceLibrary.update { settings in
                                            guard settings.referenceHints.indices.contains(index) else { return }
                                            settings.referenceHints[index] = value
                                        }
                                    }
                                ),
                                colorScheme: effectiveColorScheme,
                                placeholder: "z. B. Close-up Charakter mit starker Seitenkante und dunklem Hintergrund"
                            )
                        }
                    }
                }

            case .automation:
                VStack(alignment: .leading, spacing: 12) {
                    Toggle(
                        "Google fuer Automationen separat halten",
                        isOn: Binding(
                            get: { workflowAutomationSettings.settings.keepsGoogleSeparate },
                            set: { isEnabled in
                                workflowAutomationSettings.update { settings in
                                    settings.keepsGoogleSeparate = isEnabled
                                }
                            }
                        )
                    )

                    Toggle(
                        "Automation-Google vorbereitet",
                        isOn: Binding(
                            get: { workflowAutomationSettings.settings.isPrepared },
                            set: { isPrepared in
                                workflowAutomationSettings.update { settings in
                                    settings.isPrepared = isPrepared
                                }
                            }
                        )
                    )

                    Text("Das normale Google-Login der App bleibt damit getrennt von Google fuer spaetere n8n-, Drive-, Sheets- oder Calendar-Automationen.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    SettingsInputField(
                        title: "Automation Google Konto",
                        text: Binding(
                            get: { workflowAutomationSettings.settings.googleAccountHint },
                            set: { value in
                                workflowAutomationSettings.update { settings in
                                    settings.googleAccountHint = value
                                }
                            }
                        ),
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. automation@deinedomain.de"
                    )

                    SettingsInputField(
                        title: "Google Scope / Einsatz",
                        text: Binding(
                            get: { workflowAutomationSettings.settings.googleScopeHint },
                            set: { value in
                                workflowAutomationSettings.update { settings in
                                    settings.googleScopeHint = value
                                }
                            }
                        ),
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Drive, Sheets, Calendar"
                    )
                }
            }
        }
    }

    private func adminWorkspaceStatusText(for section: SettingsAdminWorkspaceSection) -> String {
        switch section {
        case .overview:
            return merchStoreStatusStore.status.isOpen ? "Store offen" : "Store pausiert"
        case .payments:
            return "\(visiblePaymentMethodCount) live im Checkout"
        case .shopify:
            return shopifyAdminSettingsStore.settings.activeCollectionLabel
        case .commerce:
            return commerceSettingsStore.settings.invoice.supportEmail.takeIfNotBlank() ?? "Versand & Rechnung"
        case .visuals:
            return aiVisualReferenceLibrary.settings.isEnabled ? "Visuals aktiv" : "Visuals aus"
        case .automation:
            return workflowAutomationSettings.settings.isPrepared ? "Vorbereitet" : "Noch offen"
        }
    }

    private var supportMailbox: String {
        "skydownent@gmail.com"
    }

    private var preferredSupportSenderEmail: String? {
        authManager.userSession?.email.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var supportMailSubject: String {
        guard let email = preferredSupportSenderEmail, !email.isEmpty else {
            return "Support-Anfrage"
        }
        return "Support-Anfrage - \(email)"
    }

    private var supportMailBody: String {
        let username = authManager.userSession?.username
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .takeIfNotBlank()
            ?? "Nicht verfuegbar"
        let email = preferredSupportSenderEmail?.takeIfNotBlank() ?? "Nicht verfuegbar"

        return """
        Hallo Skydown-Team,

        ich habe folgende Anfrage:

        Eingeloggter Account: \(username)
        Account-E-Mail: \(email)

        Nachricht:
        """
    }

    private func openMailAppFallback() {
        let encodedSubject = supportMailSubject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let encodedBody = supportMailBody.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""

        if let url = URL(string: "mailto:\(supportMailbox)?subject=\(encodedSubject)&body=\(encodedBody)"),
           UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
            showToastMessage("Mail-App geoeffnet", style: .success)
        } else {
            showToastMessage("Mail-App konnte nicht geoeffnet werden", style: .error)
        }
    }

    private func showToastMessage(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private func syncPaymentDrafts(with settings: PaymentMethodSettings) {
        stripeAccountHintDraft = settings.stripe.accountHint
        paypalAccountHintDraft = settings.paypal.accountHint
        klarnaAccountHintDraft = settings.klarna.accountHint
        bankAccountHolderDraft = settings.bankTransfer.accountHolder
        bankIbanDraft = settings.bankTransfer.iban
        bankBicDraft = settings.bankTransfer.bic
        bankNameDraft = settings.bankTransfer.bankName
        bankInstructionsDraft = settings.bankTransfer.paymentInstructions
    }

    private func syncCommerceDrafts(with settings: CommerceSettings) {
        domesticShippingDraft = settings.shipping.domesticCost.formattedCurrencyDraft
        euShippingDraft = settings.shipping.euCost.formattedCurrencyDraft
        internationalShippingDraft = settings.shipping.internationalCost.formattedCurrencyDraft
        freeShippingThresholdDraft = settings.shipping.freeShippingThreshold.formattedCurrencyDraft
        shippingNotesDraft = settings.shipping.shippingNotes
        invoiceCompanyNameDraft = settings.invoice.companyName
        invoiceCompanyAddressDraft = settings.invoice.companyAddress
        invoiceTaxNumberDraft = settings.invoice.taxNumber
        invoiceVatIdDraft = settings.invoice.vatId
        invoiceTaxRateDraft = settings.invoice.taxRate.formattedPercentDraft
        invoicePrefixDraft = settings.invoice.invoicePrefix
        invoiceSupportEmailDraft = settings.invoice.supportEmail
    }

    private func syncShopifyDrafts(with settings: ShopifyAdminSettings) {
        shopifyStoreDomainDraft = settings.storeDomain
        shopifyStorefrontAccessTokenDraft = settings.storefrontAccessToken
        shopifyCollectionHandleDraft = settings.collectionHandle
    }

    private func saveCommerceSettings() {
        let domesticCost = domesticShippingDraft.parseLocalizedDouble() ?? commerceSettingsStore.settings.shipping.domesticCost
        let euCost = euShippingDraft.parseLocalizedDouble() ?? commerceSettingsStore.settings.shipping.euCost
        let internationalCost = internationalShippingDraft.parseLocalizedDouble() ?? commerceSettingsStore.settings.shipping.internationalCost
        let freeShippingThreshold = freeShippingThresholdDraft.parseLocalizedDouble() ?? commerceSettingsStore.settings.shipping.freeShippingThreshold
        let taxRate = invoiceTaxRateDraft.parseLocalizedDouble() ?? commerceSettingsStore.settings.invoice.taxRate

        guard domesticCost >= 0, euCost >= 0, internationalCost >= 0, freeShippingThreshold >= 0, taxRate >= 0 else {
            showToastMessage("Bitte nur positive Werte fuer Versand und MwSt. eintragen.", style: .error)
            return
        }

        Task {
            var updated = commerceSettingsStore.settings
            updated.shipping.domesticCost = domesticCost
            updated.shipping.euCost = euCost
            updated.shipping.internationalCost = internationalCost
            updated.shipping.freeShippingThreshold = freeShippingThreshold
            updated.shipping.shippingNotes = shippingNotesDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.invoice.companyName = invoiceCompanyNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.invoice.companyAddress = invoiceCompanyAddressDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.invoice.taxNumber = invoiceTaxNumberDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.invoice.vatId = invoiceVatIdDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.invoice.taxRate = taxRate
            updated.invoice.invoicePrefix = invoicePrefixDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.invoice.supportEmail = invoiceSupportEmailDraft.trimmingCharacters(in: .whitespacesAndNewlines)

            do {
                try await commerceSettingsStore.save(updated)
                showToastMessage("Versand- und Rechnungsdaten gespeichert.", style: .success)
            } catch {
                showToastMessage("Versand- und Rechnungsdaten konnten nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func saveShopifyAdminSettings() {
        Task {
            var updated = shopifyAdminSettingsStore.settings
            updated.storeDomain = shopifyStoreDomainDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.storefrontAccessToken = shopifyStorefrontAccessTokenDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.collectionHandle = shopifyCollectionHandleDraft.trimmingCharacters(in: .whitespacesAndNewlines)

            do {
                try await shopifyAdminSettingsStore.save(updated)
                showToastMessage(
                    "Shopify-Einstellungen gespeichert. Der naechste Sync nutzt jetzt diesen Store, deinen Storefront Token und optional die Collection.",
                    style: .success
                )
            } catch {
                showToastMessage("Shopify-Einstellungen konnten nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func saveStripeConnection() {
        Task {
            var updated = paymentMethodSettingsStore.settings
            updated.stripe.connected = true
            updated.stripe.accountHint = stripeAccountHintDraft.takeIfNotBlank() ?? ""

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage("Stripe verbunden.", style: .success)
            } catch {
                showToastMessage("Stripe konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func disconnectStripe() {
        Task {
            var updated = paymentMethodSettingsStore.settings
            updated.stripe.connected = false
            updated.stripe.enabled = false
            updated.stripe.accountHint = ""

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage("Stripe getrennt.", style: .success)
            } catch {
                showToastMessage("Stripe konnte nicht getrennt werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func savePayPalConnection() {
        guard paypalAccountHintDraft.takeIfNotBlank() != nil else {
            showToastMessage("Bitte zuerst einen PayPal.Me-Link oder eine Business-Mail hinterlegen.", style: .error)
            return
        }

        Task {
            var updated = paymentMethodSettingsStore.settings
            updated.paypal.connected = true
            updated.paypal.accountHint = paypalAccountHintDraft.takeIfNotBlank() ?? ""

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage("PayPal verbunden.", style: .success)
            } catch {
                showToastMessage("PayPal konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func saveKlarnaConnection() {
        Task {
            var updated = paymentMethodSettingsStore.settings
            updated.klarna.connected = true
            updated.klarna.accountHint = klarnaAccountHintDraft.takeIfNotBlank() ?? ""

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage("Klarna verbunden.", style: .success)
            } catch {
                showToastMessage("Klarna konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func disconnectPayPal() {
        Task {
            var updated = paymentMethodSettingsStore.settings
            updated.paypal.connected = false
            updated.paypal.enabled = false
            updated.paypal.accountHint = ""

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage("PayPal getrennt.", style: .success)
            } catch {
                showToastMessage("PayPal konnte nicht getrennt werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func disconnectKlarna() {
        Task {
            var updated = paymentMethodSettingsStore.settings
            updated.klarna.connected = false
            updated.klarna.enabled = false
            updated.klarna.accountHint = ""

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage("Klarna getrennt.", style: .success)
            } catch {
                showToastMessage("Klarna konnte nicht getrennt werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func saveBankTransferDetails() {
        let accountHolder = bankAccountHolderDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        let iban = bankIbanDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        let bankName = bankNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !accountHolder.isEmpty, !iban.isEmpty, !bankName.isEmpty else {
            showToastMessage("Bitte mindestens Kontoinhaber, IBAN und Bankname hinterlegen.", style: .error)
            return
        }

        Task {
            var updated = paymentMethodSettingsStore.settings
            updated.bankTransfer.accountHolder = accountHolder
            updated.bankTransfer.iban = iban
            updated.bankTransfer.bic = bankBicDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bankTransfer.bankName = bankName
            updated.bankTransfer.paymentInstructions = bankInstructionsDraft.trimmingCharacters(in: .whitespacesAndNewlines)

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage("Bankdaten gespeichert.", style: .success)
            } catch {
                showToastMessage("Bankdaten konnten nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func setCheckoutVisibility(
        keyPath: WritableKeyPath<PaymentMethodSettings, PaymentProviderSettings>,
        isVisible: Bool,
        providerName: String
    ) {
        Task {
            var updated = paymentMethodSettingsStore.settings
            guard updated[keyPath: keyPath].connected else {
                showToastMessage("\(providerName) muss zuerst verbunden werden.", style: .error)
                return
            }
            updated[keyPath: keyPath].enabled = isVisible

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage(
                    isVisible
                    ? "\(providerName) ist jetzt im Checkout sichtbar."
                    : "\(providerName) ist nicht mehr im Checkout sichtbar.",
                    style: .success
                )
            } catch {
                showToastMessage("\(providerName) konnte nicht aktualisiert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func setBankTransferVisibility(_ isVisible: Bool) {
        Task {
            var updated = paymentMethodSettingsStore.settings
            guard updated.bankTransfer.isConfigured else {
                showToastMessage("Bitte zuerst Bankdaten hinterlegen.", style: .error)
                return
            }
            updated.bankTransfer.enabled = isVisible

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage(
                    isVisible
                    ? "Bankueberweisung ist jetzt im Checkout sichtbar."
                    : "Bankueberweisung ist nicht mehr im Checkout sichtbar.",
                    style: .success
                )
            } catch {
                showToastMessage("Bankueberweisung konnte nicht aktualisiert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }
}

private struct SettingsInputField: View {
    let title: String
    @Binding var text: String
    let colorScheme: ColorScheme
    let placeholder: String
    var keyboardType: UIKeyboardType = .default

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(placeholder, text: $text)
                .keyboardType(keyboardType)
                .textInputAutocapitalization(
                    keyboardType == .emailAddress || keyboardType == .asciiCapable || keyboardType == .URL
                        ? .never
                        : .sentences
                )
                .padding(.horizontal, 14)
                .padding(.vertical, 14)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                )
        }
    }
}

private struct PaymentProviderSettingsCard: View {
    let colorScheme: ColorScheme
    let title: String
    let statusText: String
    let checkoutVisible: Bool
    let accountHintTitle: String
    let accountHintPlaceholder: String
    @Binding var accountHint: String
    let actionTitle: String
    let secondaryActionTitle: String?
    let onPrimaryAction: () -> Void
    let onSecondaryAction: (() -> Void)?
    let onToggleCheckoutVisible: (Bool) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(statusText)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(
                            statusText == "Verbunden"
                            ? AppColors.accent(for: colorScheme)
                            : AppColors.secondaryText(for: colorScheme)
                        )
                }

                Spacer()

                SettingsBadge(
                    text: checkoutVisible ? "Im Checkout sichtbar" : "Ausgeblendet",
                    colorScheme: colorScheme
                )
            }

            SettingsInputField(
                title: accountHintTitle,
                text: $accountHint,
                colorScheme: colorScheme,
                placeholder: accountHintPlaceholder
            )

            SettingsToggleCard(
                colorScheme: colorScheme,
                title: "Fuer Kunden im Checkout anzeigen",
                subtitle: "Erst nach der Verbindung sichtbar schalten.",
                isOn: Binding(
                    get: { checkoutVisible },
                    set: { onToggleCheckoutVisible($0) }
                ),
                isEnabled: statusText == "Verbunden"
            )

            HStack(spacing: 10) {
                Button(actionTitle, action: onPrimaryAction)
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accent(for: colorScheme))

                if let secondaryActionTitle, let onSecondaryAction {
                    Button(secondaryActionTitle, action: onSecondaryAction)
                        .buttonStyle(.bordered)
                }
            }
        }
        .padding(16)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private extension Double {
    var formattedCurrencyDraft: String {
        String(format: "%.2f", self)
    }

    var formattedPercentDraft: String {
        String(format: "%.1f", self)
    }
}

private extension String {
    func parseLocalizedDouble() -> Double? {
        let normalized = trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: ",", with: ".")
        return Double(normalized)
    }
}

private struct BankTransferSettingsCard: View {
    let colorScheme: ColorScheme
    let isConfigured: Bool
    let checkoutVisible: Bool
    @Binding var accountHolder: String
    @Binding var iban: String
    @Binding var bic: String
    @Binding var bankName: String
    @Binding var paymentInstructions: String
    let onSave: () -> Void
    let onToggleCheckoutVisible: (Bool) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Bankueberweisung")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(isConfigured ? "Bankdaten hinterlegt" : "Noch nicht hinterlegt")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(
                            isConfigured
                            ? AppColors.accent(for: colorScheme)
                            : AppColors.secondaryText(for: colorScheme)
                        )
                }

                Spacer()

                SettingsBadge(
                    text: checkoutVisible ? "Im Checkout sichtbar" : "Ausgeblendet",
                    colorScheme: colorScheme
                )
            }

            SettingsInputField(
                title: "Kontoinhaber",
                text: $accountHolder,
                colorScheme: colorScheme,
                placeholder: "Vor- und Nachname oder Firmenname"
            )
            SettingsInputField(
                title: "IBAN",
                text: $iban,
                colorScheme: colorScheme,
                placeholder: "DE00 0000 0000 0000 0000 00"
            )
            SettingsInputField(
                title: "BIC",
                text: $bic,
                colorScheme: colorScheme,
                placeholder: "Optional"
            )
            SettingsInputField(
                title: "Bankname",
                text: $bankName,
                colorScheme: colorScheme,
                placeholder: "z. B. Deutsche Bank"
            )
            SettingsInputField(
                title: "Zahlungsanweisung",
                text: $paymentInstructions,
                colorScheme: colorScheme,
                placeholder: "z. B. Bitte Bestellnummer im Verwendungszweck angeben."
            )

            SettingsToggleCard(
                colorScheme: colorScheme,
                title: "Fuer Kunden im Checkout anzeigen",
                subtitle: "Erst aktivieren, wenn die Bankdaten vollstaendig sind.",
                isOn: Binding(
                    get: { checkoutVisible },
                    set: { onToggleCheckoutVisible($0) }
                ),
                isEnabled: isConfigured
            )

            Button(isConfigured ? "Bankdaten aktualisieren" : "Bankdaten hinterlegen", action: onSave)
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accentMystic(for: colorScheme))
        }
        .padding(16)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private extension String {
    func takeIfNotBlank() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

private struct SettingsHeroCard: View {
    let colorScheme: ColorScheme
    let username: String?
    let isLoggedIn: Bool
    let isAdmin: Bool
    let notificationsEnabled: Bool
    let appearance: String

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text(username ?? "Skydown Einstellungen")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Konto, Anzeige und Support sind auf iOS jetzt nicht mehr im Standard-Form-Look versteckt, sondern klar nach Aufgaben gruppiert.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "slider.horizontal.3")
                    .font(.title2)
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 10) {
                    SettingsBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
                    SettingsBadge(text: notificationsEnabled ? "Hinweise an" : "Hinweise aus", colorScheme: colorScheme)
                    SettingsBadge(text: appearance, colorScheme: colorScheme)
                }

                if isAdmin {
                    SettingsBadge(text: "Admin aktiv", colorScheme: colorScheme)
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

private struct SettingsSectionCard<Content: View>: View {
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

private struct SettingsToggleCard: View {
    let colorScheme: ColorScheme
    let title: String
    let subtitle: String
    @Binding var isOn: Bool
    var isEnabled: Bool = true

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(subtitle)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer()

            Toggle("", isOn: $isOn)
                .labelsHidden()
                .disabled(!isEnabled)
        }
        .padding(14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .opacity(isEnabled ? 1 : 0.6)
    }
}

private struct AppearanceChoiceCard: View {
    let colorScheme: ColorScheme
    let title: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.headline)
                    Text(isSelected ? "Aktiv" : "Tippen zum Wechseln")
                        .font(.caption)
                        .foregroundColor(
                            isSelected
                            ? Color.white.opacity(0.82)
                            : AppColors.secondaryText(for: colorScheme)
                        )
                }

                Spacer()

                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(AppColors.accent(for: colorScheme).opacity(isSelected ? 1 : 0.18), lineWidth: 1)
            )
            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
        }
    }
}

private struct SettingsBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(AppColors.accentMystic(for: colorScheme).opacity(0.12))
            .foregroundColor(AppColors.accentMystic(for: colorScheme))
            .clipShape(Capsule())
    }
}

private enum SettingsAdminWorkspaceSection: String, CaseIterable, Identifiable {
    case overview = "Uebersicht"
    case payments = "Zahlungen"
    case shopify = "Shopify"
    case commerce = "Versand"
    case visuals = "Visuals"
    case automation = "Automation"

    var id: String { rawValue }

    var iconName: String {
        switch self {
        case .overview:
            return "square.grid.2x2.fill"
        case .payments:
            return "creditcard.fill"
        case .shopify:
            return "bag.fill"
        case .commerce:
            return "shippingbox.fill"
        case .visuals:
            return "photo.stack.fill"
        case .automation:
            return "bolt.fill"
        }
    }

    var subtitle: String {
        switch self {
        case .overview:
            return "Schneller Status fuer Store, Zahlarten, Visuals und Automationen."
        case .payments:
            return "Provider verbinden, pruefen und fuer den Checkout sichtbar schalten."
        case .shopify:
            return "Store-Domain, Shopify-Link und Kollektion fuer den Merch-Sync pflegen."
        case .commerce:
            return "Versandkosten, MwSt. und Rechnungsdaten an einem Platz pflegen."
        case .visuals:
            return "Drive-Link, Namensschema und Referenzhinweise fuer Visual-Prompts pflegen."
        case .automation:
            return "Das getrennte Google-Setup fuer spaetere Workflows sauber vorbereiten."
        }
    }
}

private struct SettingsAdminWorkspaceChip: View {
    let section: SettingsAdminWorkspaceSection
    let isSelected: Bool
    let colorScheme: ColorScheme
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 8) {
                Image(systemName: section.iconName)
                    .font(.caption.weight(.bold))
                Text(section.rawValue)
                    .font(.subheadline.weight(.semibold))
            }
            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(
                Capsule()
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                Capsule()
                    .stroke(
                        AppColors.accent(for: colorScheme).opacity(isSelected ? 1 : 0.14),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

private struct SettingsAdminWorkspaceListRow: View {
    let section: SettingsAdminWorkspaceSection
    let colorScheme: ColorScheme
    let detailText: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(AppColors.accent(for: colorScheme).opacity(0.12))
                        .frame(width: 44, height: 44)

                    Image(systemName: section.iconName)
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(section.rawValue)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(section.subtitle)
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .multilineTextAlignment(.leading)
                }

                Spacer(minLength: 0)

                VStack(alignment: .trailing, spacing: 6) {
                    Text(detailText)
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                        .multilineTextAlignment(.trailing)

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
    }
}

private struct SettingsAdminWorkspaceSidebarButton: View {
    let section: SettingsAdminWorkspaceSection
    let isSelected: Bool
    let colorScheme: ColorScheme
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                Image(systemName: section.iconName)
                    .font(.subheadline.weight(.bold))
                    .frame(width: 20)

                Text(section.rawValue)
                    .font(.subheadline.weight(.semibold))
                    .multilineTextAlignment(.leading)

                Spacer(minLength: 0)
            }
            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(
                        AppColors.accent(for: colorScheme).opacity(isSelected ? 1 : 0.14),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

private struct SettingsAdminWorkspaceSummaryCard: View {
    let section: SettingsAdminWorkspaceSection
    let colorScheme: ColorScheme

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                Circle()
                    .fill(AppColors.accent(for: colorScheme).opacity(0.12))
                    .frame(width: 34, height: 34)

                Image(systemName: section.iconName)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 6) {
                Text(section.rawValue)
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(section.subtitle)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

#Preview {
    SettingsView(colorScheme: .constant("system"))
        .environmentObject(AuthManager())
        .environment(\.colorScheme, .light)
}
