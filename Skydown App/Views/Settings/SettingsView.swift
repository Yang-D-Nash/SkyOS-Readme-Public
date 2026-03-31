//
//  SettingsView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI
import MessageUI

struct SettingsView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var environmentColorScheme

    @StateObject private var aiVisualReferenceLibrary = AIVisualReferenceLibraryStore.shared
    @StateObject private var paymentMethodSettingsStore = PaymentMethodSettingsStore.shared
    @StateObject private var workflowAutomationSettings = WorkflowAutomationSettingsStore.shared
    @Binding var colorScheme: String

    @State private var language = "Deutsch"
    @State private var notificationsEnabled = true

    @State private var activeAlert: SettingsAlert?
    @State private var showingLoginSheet = false
    @State private var showingRegistrationSheet = false
    @State private var showingPrivacyPolicy = false
    @State private var showingTermsOfService = false
    @State private var showingOrders = false
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success
    @State private var showingMailOptions = false
    @State private var showingMailView = false
    @State private var stripeAccountHintDraft = ""
    @State private var paypalAccountHintDraft = ""
    @State private var bankAccountHolderDraft = ""
    @State private var bankIbanDraft = ""
    @State private var bankBicDraft = ""
    @State private var bankNameDraft = ""
    @State private var bankInstructionsDraft = ""

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

                    SettingsSectionCard(title: "Admin", colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text((authManager.userSession?.isAdmin ?? false) ? "Bestellungen verfuegbar" : "Keine Admin-Berechtigung")
                                .font(.headline)
                                .foregroundColor(
                                    (authManager.userSession?.isAdmin ?? false)
                                    ? AppColors.accent(for: effectiveColorScheme)
                                    : AppColors.secondaryText(for: effectiveColorScheme)
                                )

                            Text("Admin-Bereiche bleiben auf iOS sichtbar, aber nur mit passender Berechtigung aktiv.")
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                            Button {
                                showingOrders = true
                            } label: {
                                Label("Bestellungen oeffnen", systemImage: "suitcase.cart")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)
                            .disabled(!(authManager.userSession?.isAdmin ?? false))

                            if authManager.userSession?.isAdmin == true {
                                Divider()
                                    .padding(.vertical, 4)

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

                                Divider()
                                    .padding(.vertical, 4)

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

                    if authManager.userSession?.isAdmin == true {
                        SettingsSectionCard(title: "Zahlungen", colorScheme: effectiveColorScheme) {
                            VStack(alignment: .leading, spacing: 14) {
                                Text("Verbinde Zahlarten getrennt vom Checkout. Erst danach kannst du sie fuer Kunden sichtbar schalten.")
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                                PaymentProviderSettingsCard(
                                    colorScheme: effectiveColorScheme,
                                    title: "Stripe",
                                    statusText: paymentMethodSettingsStore.settings.stripe.connected ? "Verbunden" : "Nicht verbunden",
                                    checkoutVisible: paymentMethodSettingsStore.settings.stripe.connected && paymentMethodSettingsStore.settings.stripe.enabled,
                                    accountHintTitle: "Stripe Konto / Workspace",
                                    accountHint: $stripeAccountHintDraft,
                                    actionTitle: paymentMethodSettingsStore.settings.stripe.connected ? "Verbindung aktualisieren" : "Mit Stripe verbinden",
                                    secondaryActionTitle: paymentMethodSettingsStore.settings.stripe.connected ? "Trennen" : nil,
                                    onPrimaryAction: { saveStripeConnection() },
                                    onSecondaryAction: paymentMethodSettingsStore.settings.stripe.connected ? { disconnectStripe() } : nil,
                                    onToggleCheckoutVisible: { isVisible in
                                        setCheckoutVisibility(
                                            keyPath: \.stripe,
                                            isVisible: isVisible,
                                            providerName: "Stripe"
                                        )
                                    }
                                )

                                PaymentProviderSettingsCard(
                                    colorScheme: effectiveColorScheme,
                                    title: "PayPal",
                                    statusText: paymentMethodSettingsStore.settings.paypal.connected ? "Verbunden" : "Nicht verbunden",
                                    checkoutVisible: paymentMethodSettingsStore.settings.paypal.connected && paymentMethodSettingsStore.settings.paypal.enabled,
                                    accountHintTitle: "PayPal Konto / Business-Mail",
                                    accountHint: $paypalAccountHintDraft,
                                    actionTitle: paymentMethodSettingsStore.settings.paypal.connected ? "Verbindung aktualisieren" : "Mit PayPal verbinden",
                                    secondaryActionTitle: paymentMethodSettingsStore.settings.paypal.connected ? "Trennen" : nil,
                                    onPrimaryAction: { savePayPalConnection() },
                                    onSecondaryAction: paymentMethodSettingsStore.settings.paypal.connected ? { disconnectPayPal() } : nil,
                                    onToggleCheckoutVisible: { isVisible in
                                        setCheckoutVisibility(
                                            keyPath: \.paypal,
                                            isVisible: isVisible,
                                            providerName: "PayPal"
                                        )
                                    }
                                )

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
                        }
                    }

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

                            Text("Weitere rechtliche Infos folgen direkt in einem der naechsten Updates.")
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
        .sheet(isPresented: $showingPrivacyPolicy) {
            PolicyView(title: "Datenschutzbestimmungen", text: .privacyPolicyText)
        }
        .sheet(isPresented: $showingTermsOfService) {
            PolicyView(title: "Nutzungsbedingungen", text: .termsOfServiceText)
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
        }
        .onReceive(paymentMethodSettingsStore.$settings) { settings in
            syncPaymentDrafts(with: settings)
        }
    }

    private var currentAppearanceLabel: String {
        Appearance(rawValue: colorScheme)?.rawValue.capitalized ?? "System"
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
        bankAccountHolderDraft = settings.bankTransfer.accountHolder
        bankIbanDraft = settings.bankTransfer.iban
        bankBicDraft = settings.bankTransfer.bic
        bankNameDraft = settings.bankTransfer.bankName
        bankInstructionsDraft = settings.bankTransfer.paymentInstructions
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

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(placeholder, text: $text)
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
                placeholder: title == "Stripe" ? "z. B. Skydown Merch Workspace" : "z. B. paypal@deinedomain.de"
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

#Preview {
    SettingsView(colorScheme: .constant("system"))
        .environmentObject(AuthManager())
        .environment(\.colorScheme, .light)
}
