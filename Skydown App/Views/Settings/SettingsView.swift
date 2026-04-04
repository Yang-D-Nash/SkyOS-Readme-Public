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

    @ObservedObject private var aiVisualReferenceLibrary = AIVisualReferenceLibraryStore.shared
    @ObservedObject private var adminUserManagementStore = AdminUserManagementStore.shared
    @ObservedObject private var commerceSettingsStore = CommerceSettingsStore.shared
    @ObservedObject private var merchStoreStatusStore = MerchStoreStatusStore.shared
    @ObservedObject private var paymentMethodSettingsStore = PaymentMethodSettingsStore.shared
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared
    @ObservedObject private var stripeBackendSecretsStore = StripeBackendSecretsStore.shared
    @ObservedObject private var artistPagesStore = ArtistPagesStore.shared
    @ObservedObject private var shopifyAdminSettingsStore = ShopifyAdminSettingsStore.shared
    @ObservedObject private var workflowAutomationSettings = WorkflowAutomationSettingsStore.shared
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
    @State private var showingProfileEditor = false
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success
    @State private var pendingEditableImageTarget: SettingsEditableImageTarget?
    @State private var activeEditableImageUploadTarget: SettingsEditableImageTarget?
    @State private var presentedAdminWorkspace: SettingsAdminWorkspaceSection?
    @State private var showingMailOptions = false
    @State private var showingMailView = false
    @State private var stripeAccountHintDraft = ""
    @State private var stripeSecretKeyDraft = ""
    @State private var stripeWebhookSecretDraft = ""
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
    @State private var homeHeaderImageURLDraft = ""
    @State private var homeHeaderEyebrowDraft = ""
    @State private var homeHeaderTitleDraft = ""
    @State private var homeHeaderSubtitleDraft = ""
    @State private var homeHeaderDetailDraft = ""
    @State private var musicHubHeaderImageURLDraft = ""
    @State private var musicHubHeaderEyebrowDraft = ""
    @State private var musicHubHeaderTitleDraft = ""
    @State private var musicHubHeaderSubtitleDraft = ""
    @State private var musicHubHeaderDetailDraft = ""
    @State private var shopHeaderImageURLDraft = ""
    @State private var shopHeaderEyebrowDraft = ""
    @State private var shopHeaderTitleDraft = ""
    @State private var shopHeaderSubtitleDraft = ""
    @State private var shopHeaderDetailDraft = ""
    @State private var videoHeaderImageURLDraft = ""
    @State private var videoHeaderEyebrowDraft = ""
    @State private var videoHeaderTitleDraft = ""
    @State private var videoHeaderSubtitleDraft = ""
    @State private var videoHeaderDetailDraft = ""
    @State private var automationEnabledDraft = false
    @State private var automationSendsUserContextDraft = true
    @State private var automationWorkflowNameDraft = ""
    @State private var automationBaseURLDraft = ""
    @State private var automationWebhookPathDraft = ""
    @State private var automationAuthHeaderNameDraft = ""
    @State private var automationAuthHeaderValueDraft = ""
    @State private var profileUsernameDraft = ""
    @State private var profileWhatsAppDraft = ""
    @State private var profileTaglineDraft = ""
    @State private var profileBioDraft = ""
    @State private var profileInstagramHandleDraft = ""
    @State private var isSavingProfile = false
    private let editableImageUploadService = EditableImageAssetUploadService()

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

    private var automationDraftResolvedWebhookURL: String? {
        let trimmedBaseURL = automationBaseURLDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedBaseURL.isEmpty else { return nil }

        let normalizedBaseURL: String
        if trimmedBaseURL.hasPrefix("https://") || trimmedBaseURL.hasPrefix("http://") {
            normalizedBaseURL = trimmedBaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        } else {
            normalizedBaseURL = "https://\(trimmedBaseURL)".trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        }

        let trimmedPath = automationWebhookPathDraft
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))

        guard !trimmedPath.isEmpty else {
            return normalizedBaseURL
        }

        return "\(normalizedBaseURL)/\(trimmedPath)"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    SettingsHeroCard(
                        colorScheme: effectiveColorScheme,
                        username: authManager.userSession?.username,
                        isLoggedIn: authManager.userSession != nil,
                        isOwner: authManager.userSession?.isPlatformOwner == true,
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

                                Button {
                                    showingProfileEditor = true
                                } label: {
                                    Label("Profil bearbeiten", systemImage: "person.crop.circle")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(AppColors.accent(for: effectiveColorScheme))

                                Text("Kontoaktionen")
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
                                        Text("Anderes Konto")
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
            .accessibilityIdentifier("settings.root")
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
        .sheet(isPresented: $showingProfileEditor) {
            ProfileView(
                authManager: authManager,
                startsInEditMode: true
            )
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
            syncProfileDrafts(with: authManager.userSession)
            syncPaymentDrafts(with: paymentMethodSettingsStore.settings)
            syncCommerceDrafts(with: commerceSettingsStore.settings)
            syncShopifyDrafts(with: shopifyAdminSettingsStore.settings)
            syncScreenHeaderDrafts(with: screenHeaderSettingsStore.settings)
            syncAutomationDrafts(with: workflowAutomationSettings.settings)
            refreshOwnerWorkspaceObservation(for: presentedAdminWorkspace)
        }
        .onChange(of: isOwnerUser) { _, isOwner in
            guard !isOwner else {
                refreshOwnerWorkspaceObservation(for: presentedAdminWorkspace)
                return
            }

            adminUserManagementStore.configureObservation(isAdmin: false)
            stripeBackendSecretsStore.setObservationEnabled(false)
            workflowAutomationSettings.configureObservation(isAdmin: false, userID: nil)
        }
        .onChange(of: authManager.userSession?.id) { _, userID in
            syncProfileDrafts(with: authManager.userSession)
            refreshOwnerWorkspaceObservation(for: presentedAdminWorkspace, userID: userID)
        }
        .onChange(of: presentedAdminWorkspace) { _, section in
            refreshOwnerWorkspaceObservation(for: section)
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
        .sheet(item: $pendingEditableImageTarget) { target in
            SingleImagePicker { provider in
                handleEditableImageProvider(provider, for: target)
            }
        }
        .onReceive(screenHeaderSettingsStore.$settings) { settings in
            syncScreenHeaderDrafts(with: settings)
        }
        .onReceive(workflowAutomationSettings.$settings) { settings in
            syncAutomationDrafts(with: settings)
        }
        .onDisappear {
            adminUserManagementStore.configureObservation(isAdmin: false)
            stripeBackendSecretsStore.setObservationEnabled(false)
            workflowAutomationSettings.configureObservation(isAdmin: false, userID: nil)
        }
    }

    private var currentAppearanceLabel: String {
        Appearance(rawValue: colorScheme)?.rawValue.capitalized ?? "System"
    }

    private func handleEditableImageProvider(
        _ temporaryFileURL: URL?,
        for target: SettingsEditableImageTarget
    ) {
        pendingEditableImageTarget = nil

        guard let temporaryFileURL else {
            return
        }

        Task {
            await MainActor.run {
                activeEditableImageUploadTarget = target
            }
            do {
                let previousURL = currentEditableImageURL(for: target)
                defer { try? FileManager.default.removeItem(at: temporaryFileURL) }
                let data = try await PickedImageUploadPreparation.normalizedJPEGData(fromTemporaryFileURL: temporaryFileURL)
                let url = try await editableImageUploadService.uploadImageData(data)
                if previousURL != url {
                    try? await editableImageUploadService.deleteImage(at: previousURL)
                }
                await MainActor.run {
                    applyEditableImageURL(url, for: target)
                    showToastMessage("Bild hochgeladen und uebernommen.", style: .success)
                }
            } catch {
                await MainActor.run {
                    showToastMessage(
                        "Bild konnte nicht hochgeladen werden: \(error.localizedDescription)",
                        style: .error
                    )
                }
            }

            await MainActor.run {
                activeEditableImageUploadTarget = nil
            }
        }
    }

    private var isOwnerUser: Bool {
        authManager.userSession?.isPlatformOwner == true
    }

    private var visiblePaymentMethodCount: Int {
        var count = 0
        if paymentMethodSettingsStore.settings.stripe.connected && paymentMethodSettingsStore.settings.stripe.enabled { count += 1 }
        if paymentMethodSettingsStore.settings.paypal.connected && paymentMethodSettingsStore.settings.paypal.enabled { count += 1 }
        if paymentMethodSettingsStore.settings.klarna.connected && paymentMethodSettingsStore.settings.klarna.enabled { count += 1 }
        if paymentMethodSettingsStore.settings.bankTransfer.isConfigured && paymentMethodSettingsStore.settings.bankTransfer.enabled { count += 1 }
        return count
    }

    private var managedShowcasePages: [ArtistPage] {
        (artistPagesStore.pages(for: .zweizwei) + artistPagesStore.pages(for: .nicma))
            .sorted { lhs, rhs in
                if lhs.brand != rhs.brand {
                    return lhs.brand.displayTitle < rhs.brand.displayTitle
                }
                return lhs.artistName.localizedCaseInsensitiveCompare(rhs.artistName) == .orderedAscending
            }
    }

    private var assignedArtistPageCount: Int {
        managedShowcasePages.filter { !$0.editorUids.isEmpty }.count
    }

    private var publishedArtistPageCount: Int {
        managedShowcasePages.filter(\.hasCustomPresentation).count
    }

    private var configuredScreenHeaderCount: Int {
        screenHeaderSettingsStore.settings.configuredCount
    }

    @ViewBuilder
    private var adminWorkspaceSectionCard: some View {
        SettingsSectionCard(title: "Owner", colorScheme: effectiveColorScheme) {
            VStack(alignment: .leading, spacing: 14) {
                Text(isOwnerUser ? "Diese Systembereiche gehoeren jetzt allein zum Owner-Konto. Shopify, Zahlarten, Versand, Nutzerrollen und n8n laufen damit bewusst ueber eine zentrale Hand." : "Die Systembereiche sind nur fuer das feste Owner-Konto aktiv.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                Button {
                    showingOrders = true
                } label: {
                    Label("Bestellungen oeffnen", systemImage: "suitcase.cart")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .disabled(!isOwnerUser)

                if isOwnerUser {
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
            case .users:
                VStack(alignment: .leading, spacing: 14) {
                    Text("Hier steuerst du, welche Konten normaler User, Subadmin, Admin oder Owner sind. Gleichzeitig legst du fest, ob KI fuer ein Konto aktiv ist und wie hoch die Tageslimits fuer Bot, Visuals und Agent liegen.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    HStack(spacing: 10) {
                        SettingsBadge(text: "4 Rollen", colorScheme: effectiveColorScheme)
                        SettingsBadge(text: "\(adminUserManagementStore.users.count) Konten", colorScheme: effectiveColorScheme)
                    }

                    SettingsAdminRoleGuideCard(colorScheme: effectiveColorScheme)

                    if let message = adminUserManagementStore.lastErrorMessage {
                        Text(message)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(AppColors.accentHighlight(for: effectiveColorScheme))
                    }

                    if adminUserManagementStore.users.isEmpty {
                        Text("Sobald weitere Konten in der App registriert sind, erscheinen sie hier direkt zur Rollen- und KI-Verwaltung.")
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                    } else {
                        VStack(spacing: 12) {
                            ForEach(adminUserManagementStore.users) { managedUser in
                                SettingsAdminUserCard(
                                    user: managedUser,
                                    isCurrentUser: managedUser.id == authManager.userSession?.id,
                                    colorScheme: effectiveColorScheme
                                ) { updatedUser in
                                    saveManagedUser(updatedUser)
                                }
                                .id(
                                    [
                                        managedUser.id ?? "unknown",
                                        managedUser.role,
                                        String(managedUser.aiAccessEnabled),
                                        String(managedUser.aiTextRequestsPerDay),
                                        String(managedUser.aiVisualRequestsPerDay),
                                        String(managedUser.aiAgentRequestsPerDay),
                                        String(managedUser.aiHistoryRetentionDays)
                                    ].joined(separator: "-")
                                )
                            }
                        }
                    }
                }

            case .artists:
                VStack(alignment: .leading, spacing: 14) {
                    Text("Hier bekommen ZweiZwei-Artists und NICMA ihre eigene repraesentative Seite. Du als Owner verteilst Editor-Rechte; nur diese Konten oder du selbst duerfen den Inhalt spaeter anpassen.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    HStack(spacing: 10) {
                        SettingsBadge(text: "\(publishedArtistPageCount) Seiten mit Inhalt", colorScheme: effectiveColorScheme)
                        SettingsBadge(text: "\(assignedArtistPageCount) mit Editoren", colorScheme: effectiveColorScheme)
                    }

                    if let message = artistPagesStore.lastErrorMessage {
                        Text(message)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(AppColors.accentHighlight(for: effectiveColorScheme))
                    }

                    VStack(spacing: 12) {
                        ForEach(managedShowcasePages) { page in
                            SettingsArtistPageCard(
                                page: page,
                                users: adminUserManagementStore.users.filter { !$0.isPlatformOwner },
                                colorScheme: effectiveColorScheme
                            ) { updatedPage in
                                saveArtistPage(updatedPage)
                            }
                            .id(page.slug + "-" + page.editorUids.joined(separator: ","))
                        }
                    }
                }

            case .headers:
                VStack(alignment: .leading, spacing: 14) {
                    Text("Diese Hero-Bereiche laufen direkt unter den Header-Karten von Home, Music, Shop und Video. Die App dunkelt die Bilder automatisch ab, damit Schrift und Badges lesbar bleiben. Fuer alle vier Bereiche kannst du Bild, Titel und kurze Positionierung pflegen.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    HStack(spacing: 10) {
                        SettingsBadge(text: "\(configuredScreenHeaderCount) angepasst", colorScheme: effectiveColorScheme)
                        SettingsBadge(text: "Overlay aktiv", colorScheme: effectiveColorScheme)
                        SettingsBadge(text: "CRUD bereit", colorScheme: effectiveColorScheme)
                    }

                    Text("Bilder und Texte kannst du neu setzen, ersetzen oder entfernen. Live gehen die Aenderungen erst, wenn du unten auf `Header speichern` tippst.")
                        .font(.footnote.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    if let message = screenHeaderSettingsStore.lastErrorMessage {
                        Text(message)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(AppColors.accentHighlight(for: effectiveColorScheme))
                    }

                    EditableImageField(
                        title: "Home Header",
                        imageURL: $homeHeaderImageURLDraft,
                        colorScheme: effectiveColorScheme,
                        isUploading: activeEditableImageUploadTarget == .homeHeader,
                        uploadStatusText: "Home Header wird uebernommen.",
                        onPickImage: { pendingEditableImageTarget = .homeHeader },
                        onRemoveImage: { removeEditableImage(for: .homeHeader) }
                    )

                    SettingsInputField(
                        title: "Home Eyebrow",
                        text: $homeHeaderEyebrowDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Willkommen bei Sky²²"
                    )

                    SettingsInputField(
                        title: "Home Titel",
                        text: $homeHeaderTitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Sky²²"
                    )

                    SettingsMultilineInputField(
                        title: "Home Untertitel",
                        text: $homeHeaderSubtitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Kurze Begruessung oder Positionierung fuer neue User.",
                        minHeight: 88
                    )

                    SettingsMultilineInputField(
                        title: "Home Detail / Willkommenstext",
                        text: $homeHeaderDetailDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Hier kannst du einen etwas laengeren Willkommenstext oder eine kurze Bio fuer die Home-Seite hinterlegen.",
                        minHeight: 104
                    )

                    EditableImageField(
                        title: "Music Hub Header",
                        imageURL: $musicHubHeaderImageURLDraft,
                        colorScheme: effectiveColorScheme,
                        isUploading: activeEditableImageUploadTarget == .musicHubHeader,
                        uploadStatusText: "Music Hub Header wird uebernommen.",
                        onPickImage: { pendingEditableImageTarget = .musicHubHeader },
                        onRemoveImage: { removeEditableImage(for: .musicHubHeader) }
                    )

                    SettingsInputField(
                        title: "Music Hub Eyebrow",
                        text: $musicHubHeaderEyebrowDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Music"
                    )

                    SettingsInputField(
                        title: "Music Hub Titel",
                        text: $musicHubHeaderTitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Music"
                    )

                    SettingsMultilineInputField(
                        title: "Music Hub Untertitel",
                        text: $musicHubHeaderSubtitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Releases, Artists und Studio an einem Ort.",
                        minHeight: 88
                    )

                    SettingsMultilineInputField(
                        title: "Music Hub Detail",
                        text: $musicHubHeaderDetailDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Kurzer Einstieg fuer Songs, Beats und Studio.",
                        minHeight: 96
                    )

                    EditableImageField(
                        title: "Shop Header",
                        imageURL: $shopHeaderImageURLDraft,
                        colorScheme: effectiveColorScheme,
                        isUploading: activeEditableImageUploadTarget == .shopHeader,
                        uploadStatusText: "Shop Header wird uebernommen.",
                        onPickImage: { pendingEditableImageTarget = .shopHeader },
                        onRemoveImage: { removeEditableImage(for: .shopHeader) }
                    )

                    SettingsInputField(
                        title: "Shop Eyebrow",
                        text: $shopHeaderEyebrowDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Store"
                    )

                    SettingsInputField(
                        title: "Shop Titel",
                        text: $shopHeaderTitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Shop"
                    )

                    SettingsMultilineInputField(
                        title: "Shop Untertitel",
                        text: $shopHeaderSubtitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Produkte direkt in der App.",
                        minHeight: 88
                    )

                    SettingsMultilineInputField(
                        title: "Shop Detail",
                        text: $shopHeaderDetailDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Eigener Willkommenstext fuer den Merch-Bereich.",
                        minHeight: 96
                    )

                    EditableImageField(
                        title: "Video Header",
                        imageURL: $videoHeaderImageURLDraft,
                        colorScheme: effectiveColorScheme,
                        isUploading: activeEditableImageUploadTarget == .videoHeader,
                        uploadStatusText: "Video Header wird uebernommen.",
                        onPickImage: { pendingEditableImageTarget = .videoHeader },
                        onRemoveImage: { removeEditableImage(for: .videoHeader) }
                    )

                    SettingsInputField(
                        title: "Video Eyebrow",
                        text: $videoHeaderEyebrowDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Video"
                    )

                    SettingsInputField(
                        title: "Video Titel",
                        text: $videoHeaderTitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Video"
                    )

                    SettingsMultilineInputField(
                        title: "Video Untertitel",
                        text: $videoHeaderSubtitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Reels, Visuals und starke Kollaborationen.",
                        minHeight: 88
                    )

                    SettingsMultilineInputField(
                        title: "Video Detail",
                        text: $videoHeaderDetailDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Kurzer Einstieg fuer Clips, Looks und Kollabos.",
                        minHeight: 96
                    )

                    Text("Leere Felder lassen den jeweiligen Screen wieder auf den nativen Farbverlauf zurueckfallen.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    Button(action: saveScreenHeaderSettings) {
                        Label("Header speichern", systemImage: "photo.on.rectangle.angled")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accent(for: effectiveColorScheme))
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
                    Text("PayPal und Bankueberweisung laufen als manueller Owner-Handoff. Stripe ist als sicherer Live-Checkout aktiv, und Klarna laeuft live ueber Stripe, sobald es im Stripe-Dashboard freigeschaltet und serverseitig konfiguriert ist.")
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

                    StripeBackendSecretsCard(
                        colorScheme: effectiveColorScheme,
                        status: stripeBackendSecretsStore.status,
                        stripeSecretKey: $stripeSecretKeyDraft,
                        stripeWebhookSecret: $stripeWebhookSecretDraft
                    ) {
                        saveStripeBackendSecrets()
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
                VStack(alignment: .leading, spacing: 14) {
                    Text("Die App bleibt normal ueber Firebase eingeloggt. Der Owner hinterlegt hier die zentrale n8n-Verbindung; nur gepruefter User-Kontext geht serverseitig an genau diesen Workflow.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    Toggle("n8n aktiv", isOn: $automationEnabledDraft)

                    Toggle("App-User-Kontext mitsenden", isOn: $automationSendsUserContextDraft)

                    SettingsInputField(
                        title: "Workflow Name",
                        text: $automationWorkflowNameDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. AI Script Pipeline"
                    )

                    SettingsInputField(
                        title: "n8n Base URL",
                        text: $automationBaseURLDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "https://n8n.deinedomain.de",
                        keyboardType: .URL
                    )

                    SettingsInputField(
                        title: "Webhook Path",
                        text: $automationWebhookPathDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "webhook/skydown-app"
                    )

                    SettingsInputField(
                        title: "Auth Header Name",
                        text: $automationAuthHeaderNameDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. X-Skydown-Automation-Key",
                        keyboardType: .asciiCapable
                    )

                    SettingsInputField(
                        title: "Auth Header Value",
                        text: $automationAuthHeaderValueDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "optional",
                        keyboardType: .asciiCapable
                    )

                    if let resolvedWebhookURL = automationDraftResolvedWebhookURL {
                        SettingsBadge(
                            text: "Webhook: \(resolvedWebhookURL)",
                            colorScheme: effectiveColorScheme
                        )
                    } else {
                        SettingsBadge(
                            text: "Webhook noch nicht vollstaendig",
                            colorScheme: effectiveColorScheme
                        )
                    }

                    HStack(spacing: 10) {
                        Button(action: saveAutomationSettings) {
                            Label("n8n speichern", systemImage: "bolt.circle.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.accentHighlight(for: effectiveColorScheme))

                        Button(action: runAutomationTest) {
                            Label("Test senden", systemImage: "paperplane.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .disabled(automationDraftResolvedWebhookURL == nil || !automationEnabledDraft)
                    }
                }
            }
        }
    }

    private func adminWorkspaceStatusText(for section: SettingsAdminWorkspaceSection) -> String {
        switch section {
        case .payments:
            return "\(visiblePaymentMethodCount) live im Checkout"
        case .users:
            return "\(adminUserManagementStore.users.count) Konten"
        case .artists:
            return "\(publishedArtistPageCount) Artist-Seiten"
        case .headers:
            return "\(configuredScreenHeaderCount) Header live"
        case .shopify:
            return shopifyAdminSettingsStore.settings.activeCollectionLabel
        case .commerce:
            return commerceSettingsStore.settings.invoice.supportEmail.takeIfNotBlank() ?? "Versand & Rechnung"
        case .visuals:
            return aiVisualReferenceLibrary.settings.isEnabled ? "Visuals aktiv" : "Visuals aus"
        case .automation:
            return workflowAutomationSettings.settings.isPrepared ? "n8n bereit" : "Noch offen"
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

    private func applyEditableImageURL(_ url: String, for target: SettingsEditableImageTarget) {
        switch target {
        case .homeHeader:
            homeHeaderImageURLDraft = url
        case .musicHubHeader:
            musicHubHeaderImageURLDraft = url
        case .shopHeader:
            shopHeaderImageURLDraft = url
        case .videoHeader:
            videoHeaderImageURLDraft = url
        }
    }

    private func currentEditableImageURL(for target: SettingsEditableImageTarget) -> String {
        switch target {
        case .homeHeader:
            return homeHeaderImageURLDraft
        case .musicHubHeader:
            return musicHubHeaderImageURLDraft
        case .shopHeader:
            return shopHeaderImageURLDraft
        case .videoHeader:
            return videoHeaderImageURLDraft
        }
    }

    private func removeEditableImage(for target: SettingsEditableImageTarget) {
        let previousURL = currentEditableImageURL(for: target)
        applyEditableImageURL("", for: target)

        Task {
            do {
                try await editableImageUploadService.deleteImage(at: previousURL)
                await MainActor.run {
                    showToastMessage("Bild entfernt.", style: .success)
                }
            } catch {
                await MainActor.run {
                    showToastMessage("Bild wurde entfernt. Alter Upload konnte nicht geloescht werden: \(error.localizedDescription)", style: .error)
                }
            }
        }
    }

    private func syncProfileDrafts(with user: User?) {
        profileUsernameDraft = user?.username ?? ""
        profileWhatsAppDraft = user?.whatsApp ?? ""
        profileTaglineDraft = user?.profileTagline ?? ""
        profileBioDraft = user?.profileBio ?? ""
        profileInstagramHandleDraft = user?.instagramHandle ?? ""
    }

    private func saveProfile() async {
        guard authManager.userSession != nil else {
            showToastMessage("Bitte erst anmelden.", style: .error)
            return
        }

        let trimmedUsername = profileUsernameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedUsername.isEmpty else {
            showToastMessage("Bitte einen Benutzernamen eintragen.", style: .error)
            return
        }

        isSavingProfile = true
        defer { isSavingProfile = false }

        do {
            try await authManager.updateProfile(
                username: trimmedUsername,
                whatsApp: profileWhatsAppDraft,
                profileTagline: profileTaglineDraft,
                profileBio: profileBioDraft,
                instagramHandle: profileInstagramHandleDraft
            )
            syncProfileDrafts(with: authManager.userSession)
            showToastMessage("Profil gespeichert.", style: .success)
        } catch {
            showToastMessage("Profil konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
        }
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

    private func syncScreenHeaderDrafts(with settings: ScreenHeaderSettings) {
        homeHeaderImageURLDraft = settings.homeImageURL
        homeHeaderEyebrowDraft = settings.homeEyebrow
        homeHeaderTitleDraft = settings.homeTitle
        homeHeaderSubtitleDraft = settings.homeSubtitle
        homeHeaderDetailDraft = settings.homeDetail
        musicHubHeaderImageURLDraft = settings.musicHubImageURL
        musicHubHeaderEyebrowDraft = settings.musicHubEyebrow
        musicHubHeaderTitleDraft = settings.musicHubTitle
        musicHubHeaderSubtitleDraft = settings.musicHubSubtitle
        musicHubHeaderDetailDraft = settings.musicHubDetail
        shopHeaderImageURLDraft = settings.shopImageURL
        shopHeaderEyebrowDraft = settings.shopEyebrow
        shopHeaderTitleDraft = settings.shopTitle
        shopHeaderSubtitleDraft = settings.shopSubtitle
        shopHeaderDetailDraft = settings.shopDetail
        videoHeaderImageURLDraft = settings.videoHubImageURL
        videoHeaderEyebrowDraft = settings.videoHubEyebrow
        videoHeaderTitleDraft = settings.videoHubTitle
        videoHeaderSubtitleDraft = settings.videoHubSubtitle
        videoHeaderDetailDraft = settings.videoHubDetail
    }

    private func syncAutomationDrafts(with settings: WorkflowAutomationSettings) {
        automationEnabledDraft = settings.isEnabled
        automationSendsUserContextDraft = settings.sendsUserContext
        automationWorkflowNameDraft = settings.workflowName
        automationBaseURLDraft = settings.baseURL
        automationWebhookPathDraft = settings.webhookPath
        automationAuthHeaderNameDraft = settings.authHeaderName
        automationAuthHeaderValueDraft = settings.authHeaderValue
    }

    private func refreshOwnerWorkspaceObservation(
        for section: SettingsAdminWorkspaceSection?,
        userID: String? = nil
    ) {
        let resolvedUserID = userID ?? authManager.userSession?.id
        let shouldObserveUsers = isOwnerUser && (section == .users || section == .artists)
        let shouldObserveStripeSecrets = isOwnerUser && section == .payments
        let shouldObserveAutomation = isOwnerUser && section == .automation

        adminUserManagementStore.configureObservation(isAdmin: shouldObserveUsers)
        stripeBackendSecretsStore.setObservationEnabled(shouldObserveStripeSecrets)
        workflowAutomationSettings.configureObservation(
            isAdmin: shouldObserveAutomation,
            userID: shouldObserveAutomation ? resolvedUserID : nil
        )
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

    private func saveScreenHeaderSettings() {
        Task {
            let updated = ScreenHeaderSettings(
                homeImageURL: homeHeaderImageURLDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                homeEyebrow: homeHeaderEyebrowDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                homeTitle: homeHeaderTitleDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                homeSubtitle: homeHeaderSubtitleDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                homeDetail: homeHeaderDetailDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                musicHubImageURL: musicHubHeaderImageURLDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                musicHubEyebrow: musicHubHeaderEyebrowDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                musicHubTitle: musicHubHeaderTitleDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                musicHubSubtitle: musicHubHeaderSubtitleDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                musicHubDetail: musicHubHeaderDetailDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                shopImageURL: shopHeaderImageURLDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                shopEyebrow: shopHeaderEyebrowDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                shopTitle: shopHeaderTitleDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                shopSubtitle: shopHeaderSubtitleDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                shopDetail: shopHeaderDetailDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                videoHubImageURL: videoHeaderImageURLDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                videoHubEyebrow: videoHeaderEyebrowDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                videoHubTitle: videoHeaderTitleDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                videoHubSubtitle: videoHeaderSubtitleDraft.trimmingCharacters(in: .whitespacesAndNewlines),
                videoHubDetail: videoHeaderDetailDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            )

            do {
                try await screenHeaderSettingsStore.save(updated)
                showToastMessage("Header gespeichert. Hero-Bilder und Texte wurden aktualisiert.", style: .success)
            } catch {
                showToastMessage("Header konnten nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func saveManagedUser(_ user: User) {
        Task {
            do {
                try await adminUserManagementStore.save(user)
                showToastMessage("Konto gespeichert. Rolle und KI-Limits wurden aktualisiert.", style: .success)
            } catch {
                showToastMessage("Konto konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func saveArtistPage(_ page: ArtistPage) {
        Task {
            do {
                try await artistPagesStore.save(page)
                showToastMessage("\(page.artistName) gespeichert.", style: .success)
            } catch {
                showToastMessage("Artist-Seite konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func saveAutomationSettings() {
        Task {
            var updated = workflowAutomationSettings.settings
            updated.isEnabled = automationEnabledDraft
            updated.sendsUserContext = automationSendsUserContextDraft
            updated.workflowName = automationWorkflowNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.baseURL = automationBaseURLDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.webhookPath = automationWebhookPathDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.authHeaderName = automationAuthHeaderNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.authHeaderValue = automationAuthHeaderValueDraft.trimmingCharacters(in: .whitespacesAndNewlines)

            do {
                try await workflowAutomationSettings.save(updated)
                showToastMessage(
                    "n8n gespeichert. Die App nutzt weiter den normalen Login und schickt nur serverseitig geprueften Kontext an deinen Workflow.",
                    style: .success
                )
            } catch {
                showToastMessage("n8n konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func runAutomationTest() {
        Task {
            do {
                var updated = workflowAutomationSettings.settings
                updated.isEnabled = automationEnabledDraft
                updated.sendsUserContext = automationSendsUserContextDraft
                updated.workflowName = automationWorkflowNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
                updated.baseURL = automationBaseURLDraft.trimmingCharacters(in: .whitespacesAndNewlines)
                updated.webhookPath = automationWebhookPathDraft.trimmingCharacters(in: .whitespacesAndNewlines)
                updated.authHeaderName = automationAuthHeaderNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
                updated.authHeaderValue = automationAuthHeaderValueDraft.trimmingCharacters(in: .whitespacesAndNewlines)

                try await workflowAutomationSettings.save(updated)
                let message = try await workflowAutomationSettings.triggerTest()
                showToastMessage(message, style: .success)
            } catch {
                showToastMessage("n8n-Test fehlgeschlagen: \(error.localizedDescription)", style: .error)
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

    private func saveStripeBackendSecrets() {
        let trimmedKey = stripeSecretKeyDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedWebhook = stripeWebhookSecretDraft.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedKey.isEmpty || !trimmedWebhook.isEmpty else {
            showToastMessage("Bitte mindestens einen Stripe-Wert eingeben.", style: .error)
            return
        }

        Task {
            do {
                try await stripeBackendSecretsStore.saveSecrets(
                    stripeSecretKey: trimmedKey,
                    stripeWebhookSecret: trimmedWebhook
                )
                stripeSecretKeyDraft = ""
                stripeWebhookSecretDraft = ""
                showToastMessage(
                    "Stripe-Backend sicher gespeichert. Die Werte liegen jetzt serverseitig im Secret Manager.",
                    style: .success
                )
            } catch {
                showToastMessage("Stripe-Secrets konnten nicht gespeichert werden: \(error.localizedDescription)", style: .error)
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
            SettingsFieldTitle(title: title, colorScheme: colorScheme)

            TextField(placeholder, text: $text)
                .keyboardType(keyboardType)
                .textInputAutocapitalization(
                    keyboardType == .emailAddress || keyboardType == .asciiCapable || keyboardType == .URL
                        ? .never
                        : .sentences
                )
                .settingsFieldChrome(colorScheme: colorScheme)
        }
    }
}

private struct SettingsSecureInputField: View {
    let title: String
    @Binding var text: String
    let colorScheme: ColorScheme
    let placeholder: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingsFieldTitle(title: title, colorScheme: colorScheme)

            SecureField(placeholder, text: $text)
                .textInputAutocapitalization(.never)
                .settingsFieldChrome(colorScheme: colorScheme)
        }
    }
}

private struct SettingsMultilineInputField: View {
    let title: String
    @Binding var text: String
    let colorScheme: ColorScheme
    let placeholder: String
    var minHeight: CGFloat = 110

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingsFieldTitle(title: title, colorScheme: colorScheme)

            ZStack(alignment: .topLeading) {
                if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(placeholder)
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .padding(.horizontal, 18)
                        .padding(.vertical, 16)
                }

                TextEditor(text: $text)
                    .scrollContentBackground(.hidden)
                    .frame(minHeight: minHeight)
                    .settingsFieldChrome(colorScheme: colorScheme, horizontalPadding: 14, verticalPadding: 10)
            }
        }
    }
}

private struct SettingsFieldTitle: View {
    let title: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundColor(AppColors.text(for: colorScheme))
    }
}

private extension View {
    func settingsFieldChrome(
        colorScheme: ColorScheme,
        horizontalPadding: CGFloat = 14,
        verticalPadding: CGFloat = 14
    ) -> some View {
        self
            .padding(.horizontal, horizontalPadding)
            .padding(.vertical, verticalPadding)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
            )
    }
}

private struct SettingsProfileEditorCard: View {
    let colorScheme: ColorScheme
    @Binding var username: String
    @Binding var whatsApp: String
    @Binding var tagline: String
    @Binding var bio: String
    @Binding var instagramHandle: String
    let isSaving: Bool
    let onSave: () -> Void

    private var initials: String {
        let trimmed = username.trimmingCharacters(in: .whitespacesAndNewlines)
        return String(trimmed.prefix(1)).uppercased()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(AppColors.accent(for: colorScheme).opacity(0.14))
                        .frame(width: 48, height: 48)

                    Text(initials.isEmpty ? "U" : initials)
                        .font(.headline.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text("Profil")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Username, Kurzinfo und Links.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            SettingsInputField(
                title: "Benutzername",
                text: $username,
                colorScheme: colorScheme,
                placeholder: "Dein Name"
            )

            SettingsInputField(
                title: "Kurzinfo",
                text: $tagline,
                colorScheme: colorScheme,
                placeholder: "Kurz und praegnant"
            )

            SettingsMultilineInputField(
                title: "Bio",
                text: $bio,
                colorScheme: colorScheme,
                placeholder: "Worum geht es bei dir?"
            )

            SettingsInputField(
                title: "Instagram",
                text: $instagramHandle,
                colorScheme: colorScheme,
                placeholder: "@handle",
                keyboardType: .asciiCapable
            )

            SettingsInputField(
                title: "WhatsApp",
                text: $whatsApp,
                colorScheme: colorScheme,
                placeholder: "+49 ..."
            )

            Button(action: onSave) {
                if isSaving {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Label("Profil speichern", systemImage: "checkmark.circle.fill")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accent(for: colorScheme))
            .disabled(isSaving)
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

private struct StripeBackendSecretsCard: View {
    let colorScheme: ColorScheme
    let status: StripeBackendSecretsStatus
    @Binding var stripeSecretKey: String
    @Binding var stripeWebhookSecret: String
    let onSave: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Sicheres Stripe-Backend")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(status.isReady ? "Backend bereit" : "Backend noch unvollstaendig")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(
                            status.isReady
                            ? AppColors.accent(for: colorScheme)
                            : AppColors.secondaryText(for: colorScheme)
                        )
                }

                Spacer()

                SettingsBadge(
                    text: status.isReady ? "Live bereit" : "Setup fehlt",
                    colorScheme: colorScheme
                )
            }

            HStack(spacing: 8) {
                SettingsBadge(
                    text: status.hasSecretKey ? "Secret Key gesetzt" : "Secret Key fehlt",
                    colorScheme: colorScheme
                )
                SettingsBadge(
                    text: status.hasWebhookSecret ? "Webhook Secret gesetzt" : "Webhook Secret fehlt",
                    colorScheme: colorScheme
                )
            }

            Text("Die Werte werden nie wieder ausgelesen oder in Firestore gespeichert. Leere Felder lassen bestehende Secrets unveraendert.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            SettingsSecureInputField(
                title: "Stripe Secret Key",
                text: $stripeSecretKey,
                colorScheme: colorScheme,
                placeholder: "sk_live_... oder rk_live_..."
            )

            SettingsSecureInputField(
                title: "Stripe Webhook Secret",
                text: $stripeWebhookSecret,
                colorScheme: colorScheme,
                placeholder: "whsec_..."
            )

            Button("Sicher speichern", action: onSave)
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
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
    let isOwner: Bool
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

                if isOwner {
                    SettingsBadge(text: "Owner aktiv", colorScheme: colorScheme)
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
    case payments = "Zahlungen"
    case users = "User"
    case artists = "Artists"
    case headers = "Header"
    case shopify = "Shopify"
    case commerce = "Versand"
    case visuals = "Visuals"
    case automation = "Automation"

    var id: String { rawValue }

    var iconName: String {
        switch self {
        case .payments:
            return "creditcard.fill"
        case .users:
            return "person.2.fill"
        case .artists:
            return "music.mic.circle.fill"
        case .headers:
            return "photo.on.rectangle.angled"
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
        case .payments:
            return "Provider verbinden, pruefen und fuer den Checkout sichtbar schalten."
        case .users:
            return "Rollen, KI-Zugriff, Tageslimits und History pro Konto steuern."
        case .artists:
            return "Artist-Seiten pflegen und Editor-Rechte pro Artist zuteilen."
        case .headers:
            return "Hero-Bilder und Texte fuer Home, Music, Shop und Video pflegen."
        case .shopify:
            return "Owner-Quelle fuer Store-Domain, Token und Collection des Merch-Syncs."
        case .commerce:
            return "Versandkosten, MwSt. und Rechnungsdaten an einem Platz pflegen."
        case .visuals:
            return "Drive-Link, Namensschema und Referenzhinweise fuer Visual-Prompts pflegen."
        case .automation:
            return "Owner-seitig n8n anbinden, User-Kontext mitschicken und den Webhook testen."
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

private struct SettingsAdminRoleGuideCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Rollen im System")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            VStack(spacing: 10) {
                ForEach(UserRole.allCases, id: \.self) { role in
                    HStack(alignment: .top, spacing: 10) {
                        SettingsBadge(text: role.displayTitle, colorScheme: colorScheme)

                        Text(role.roleSummary)
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
        }
        .padding(16)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct SettingsArtistPageCard: View {
    let page: ArtistPage
    let users: [User]
    let colorScheme: ColorScheme
    let onSave: (ArtistPage) -> Void

    @State private var selectedEditorUids: Set<String>

    init(
        page: ArtistPage,
        users: [User],
        colorScheme: ColorScheme,
        onSave: @escaping (ArtistPage) -> Void
    ) {
        self.page = page
        self.users = users
        self.colorScheme = colorScheme
        self.onSave = onSave
        _selectedEditorUids = State(initialValue: Set(page.editorUids))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 5) {
                    Text(page.artistName)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(page.hasCustomPresentation ? "Seite hat schon Inhalt." : "Noch als Platzhalter. Nach dem ersten Speichern ist die Artist-Seite live.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 6) {
                    SettingsBadge(text: page.hasCustomPresentation ? "Live" : "Platzhalter", colorScheme: colorScheme)
                    SettingsBadge(text: "\(selectedEditorUids.count) Editoren", colorScheme: colorScheme)
                }
            }

            if users.isEmpty {
                Text("Sobald weitere Konten registriert sind, kannst du hier Editoren fuer diese Artist-Seite zuweisen.")
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else {
                Text("Editoren")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 132), spacing: 8)],
                    alignment: .leading,
                    spacing: 8
                ) {
                    ForEach(users) { user in
                        let isSelected = selectedEditorUids.contains(user.id ?? "")

                        Button {
                            guard let userId = user.id, !userId.isEmpty else { return }
                            if isSelected {
                                selectedEditorUids.remove(userId)
                            } else {
                                selectedEditorUids.insert(userId)
                            }
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                                    .font(.caption.weight(.bold))
                                Text(user.username)
                                    .font(.footnote.weight(.semibold))
                                    .lineLimit(1)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 9)
                            .background(
                                Capsule()
                                    .fill(
                                        isSelected
                                        ? AppColors.accent(for: colorScheme)
                                        : AppColors.cardBackground(for: colorScheme)
                                    )
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            Button {
                onSave(
                    ArtistPage(
                        id: page.slug,
                        brand: page.brand,
                        artistName: page.artistName,
                        tagline: page.tagline,
                        bio: page.bio,
                        profileImageURL: page.profileImageURL,
                        heroImageURL: page.heroImageURL,
                        instagramURL: page.instagramURL,
                        spotifyURL: page.spotifyURL,
                        youtubeURL: page.youtubeURL,
                        editorUids: Array(selectedEditorUids).sorted(),
                        createdAt: page.createdAt,
                        updatedAt: .now,
                        isPlaceholder: false
                    )
                )
            } label: {
                Label("Editoren speichern", systemImage: "person.crop.circle.badge.checkmark")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accent(for: colorScheme))
        }
        .padding(16)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct SettingsAdminUserCard: View {
    let user: User
    let isCurrentUser: Bool
    let colorScheme: ColorScheme
    let onSave: (User) -> Void

    @State private var draftRole: UserRole
    @State private var draftQuotaPlan: UserQuotaPlan
    @State private var aiAccessEnabled: Bool
    @State private var textLimitDraft: String
    @State private var visualLimitDraft: String
    @State private var agentLimitDraft: String
    @State private var historyRetentionDays: Int
    @State private var canManageMusicCatalog: Bool
    @State private var canManageVideoCatalog: Bool
    @State private var canModerateProfiles: Bool

    init(
        user: User,
        isCurrentUser: Bool,
        colorScheme: ColorScheme,
        onSave: @escaping (User) -> Void
    ) {
        self.user = user
        self.isCurrentUser = isCurrentUser
        self.colorScheme = colorScheme
        self.onSave = onSave

        _draftRole = State(initialValue: user.resolvedRole)
        _draftQuotaPlan = State(initialValue: user.resolvedQuotaPlan)
        _aiAccessEnabled = State(initialValue: user.aiAccessEnabled)
        _textLimitDraft = State(initialValue: String(user.resolvedAITextRequestsPerDay))
        _visualLimitDraft = State(initialValue: String(user.resolvedAIVisualRequestsPerDay))
        _agentLimitDraft = State(initialValue: String(user.resolvedAIAgentRequestsPerDay))
        _historyRetentionDays = State(initialValue: user.resolvedAIHistoryRetentionDays)
        _canManageMusicCatalog = State(initialValue: user.canManageMusic)
        _canManageVideoCatalog = State(initialValue: user.canManageVideos)
        _canModerateProfiles = State(initialValue: user.canModerateUserProfiles)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(user.username)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(user.email)
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 6) {
                    SettingsBadge(text: draftRole.displayTitle, colorScheme: colorScheme)
                    if isCurrentUser {
                        SettingsBadge(text: "Du", colorScheme: colorScheme)
                    }
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Rolle")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Menu {
                    ForEach(UserRole.allCases, id: \.self) { role in
                        Button(role.displayTitle) {
                            draftRole = role
                        }
                        .disabled(user.isPlatformOwner || (isCurrentUser && role != user.resolvedRole))
                    }
                } label: {
                    HStack {
                        Text(draftRole.displayTitle)
                        Spacer()
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.caption.weight(.bold))
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)

                if user.isPlatformOwner {
                    Text("Das Owner-Konto ist fest an nash.lioncorna@gmail.com gebunden und bleibt immer Owner.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                } else if isCurrentUser {
                    Text("Dein eigenes Konto bleibt vor versehentlichen Rollenwechseln geschuetzt. Limits kannst du hier trotzdem anpassen.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            if draftRole == .owner {
                ownerControlNote
            } else if draftRole == .admin {
                adminCapabilitiesSection
            } else {
                quotaPlanSection
            }

            SettingsToggleCard(
                colorScheme: colorScheme,
                title: "KI fuer dieses Konto aktiv",
                subtitle: "Wenn aus, sind Bot, Visuals und Agent fuer dieses Konto gesperrt.",
                isOn: $aiAccessEnabled
            )

            VStack(alignment: .leading, spacing: 10) {
                Text("Tageslimits")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                HStack(spacing: 10) {
                    SettingsInputField(
                        title: "Bot",
                        text: $textLimitDraft,
                        colorScheme: colorScheme,
                        placeholder: "\(draftRole.defaultAITextRequestsPerDay)",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Visuals",
                        text: $visualLimitDraft,
                        colorScheme: colorScheme,
                        placeholder: "\(draftRole.defaultAIVisualRequestsPerDay)",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Agent",
                        text: $agentLimitDraft,
                        colorScheme: colorScheme,
                        placeholder: "\(draftRole.defaultAIAgentRequestsPerDay)",
                        keyboardType: .numberPad
                    )
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("History-Aufbewahrung")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Picker("History-Aufbewahrung", selection: $historyRetentionDays) {
                    Text("1 Tag").tag(1)
                    Text("3 Tage").tag(3)
                    Text("7 Tage").tag(7)
                    Text("30 Tage").tag(30)
                }
                .pickerStyle(.segmented)
            }

            Button {
                onSave(buildUpdatedUser())
            } label: {
                Label("Konto speichern", systemImage: "checkmark.circle.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accent(for: colorScheme))
        }
        .padding(16)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .onChange(of: draftRole) { _, newRole in
            let recommendedPlan = UserQuotaPlan.defaultPlan(for: newRole)
            switch newRole {
            case .owner:
                draftQuotaPlan = .ownerUnlimited
                canManageMusicCatalog = true
                canManageVideoCatalog = true
                canModerateProfiles = true
            case .admin:
                draftQuotaPlan = .internalTeam
                canManageMusicCatalog = user.canManageMusic
                canManageVideoCatalog = user.canManageVideos
                canModerateProfiles = user.canModerateUserProfiles
            case .subadmin:
                if draftQuotaPlan == .ownerUnlimited || draftQuotaPlan == .internalTeam || draftQuotaPlan == .free {
                    draftQuotaPlan = .creator
                }
                canManageMusicCatalog = false
                canManageVideoCatalog = false
                canModerateProfiles = false
            case .user:
                draftQuotaPlan = .free
                canManageMusicCatalog = false
                canManageVideoCatalog = false
                canModerateProfiles = false
            }
            textLimitDraft = String(recommendedPlan.aiTextRequestsPerDay)
            visualLimitDraft = String(recommendedPlan.aiVisualRequestsPerDay)
            agentLimitDraft = String(recommendedPlan.aiAgentRequestsPerDay)
            historyRetentionDays = recommendedPlan.aiHistoryRetentionDays
        }
        .onChange(of: draftQuotaPlan) { _, newPlan in
            guard draftRole == .subadmin || draftRole == .user else { return }
            textLimitDraft = String(newPlan.aiTextRequestsPerDay)
            visualLimitDraft = String(newPlan.aiVisualRequestsPerDay)
            agentLimitDraft = String(newPlan.aiAgentRequestsPerDay)
            historyRetentionDays = newPlan.aiHistoryRetentionDays
        }
    }

    private func buildUpdatedUser() -> User {
        var updatedUser = user
        updatedUser.role = draftRole.rawValue
        updatedUser.isAdmin = draftRole.hasStaffAccess
        let finalQuotaPlan: UserQuotaPlan
        switch draftRole {
        case .owner:
            finalQuotaPlan = .ownerUnlimited
            updatedUser.canManageMusicCatalog = true
            updatedUser.canManageVideoCatalog = true
            updatedUser.canModerateProfiles = true
        case .admin:
            finalQuotaPlan = .internalTeam
            updatedUser.canManageMusicCatalog = canManageMusicCatalog
            updatedUser.canManageVideoCatalog = canManageVideoCatalog
            updatedUser.canModerateProfiles = canModerateProfiles
        case .subadmin:
            finalQuotaPlan = draftQuotaPlan == .studio ? .studio : .creator
            updatedUser.canManageMusicCatalog = false
            updatedUser.canManageVideoCatalog = false
            updatedUser.canModerateProfiles = false
        case .user:
            finalQuotaPlan = .free
            updatedUser.canManageMusicCatalog = false
            updatedUser.canManageVideoCatalog = false
            updatedUser.canModerateProfiles = false
        }
        updatedUser.quotaPlan = finalQuotaPlan.rawValue
        updatedUser.aiAccessEnabled = aiAccessEnabled
        updatedUser.aiTextRequestsPerDay = sanitizedLimit(textLimitDraft, fallback: finalQuotaPlan.aiTextRequestsPerDay)
        updatedUser.aiVisualRequestsPerDay = sanitizedLimit(visualLimitDraft, fallback: finalQuotaPlan.aiVisualRequestsPerDay)
        updatedUser.aiAgentRequestsPerDay = sanitizedLimit(agentLimitDraft, fallback: finalQuotaPlan.aiAgentRequestsPerDay)
        updatedUser.aiHistoryRetentionDays = historyRetentionDays
        return updatedUser
    }

    private func sanitizedLimit(_ draft: String, fallback: Int) -> Int {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let value = Int(trimmed), value > 0 else {
            return fallback
        }

        return value
    }

    private var ownerControlNote: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Owner-Kontrolle")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Dieses Konto bleibt der zentrale Root-Zugang. Shopify, Zahlungen, Rollen, n8n und Recovery laufen nur hier.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
    }

    private var adminCapabilitiesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Zugewiesene Funktionen")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            SettingsToggleCard(
                colorScheme: colorScheme,
                title: "Music verwalten",
                subtitle: "Beats, Releases und Upload-Freigaben pflegen.",
                isOn: $canManageMusicCatalog
            )

            SettingsToggleCard(
                colorScheme: colorScheme,
                title: "Video verwalten",
                subtitle: "Video Hub, Uploads und Home-Highlights steuern.",
                isOn: $canManageVideoCatalog
            )

            SettingsToggleCard(
                colorScheme: colorScheme,
                title: "Profile moderieren",
                subtitle: "Profile und Galerie-Inhalte fuer Support und Moderation einsehen.",
                isOn: $canModerateProfiles
            )
        }
    }

    private var quotaPlanSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(draftRole == .subadmin ? "Kontingentmodell" : "Kontingent")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            if draftRole == .user {
                HStack(spacing: 10) {
                    SettingsBadge(text: UserQuotaPlan.free.displayTitle, colorScheme: colorScheme)
                    Text(UserQuotaPlan.free.planSummary)
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else {
                Menu {
                    ForEach([UserQuotaPlan.creator, UserQuotaPlan.studio], id: \.self) { plan in
                        Button(plan.displayTitle) {
                            draftQuotaPlan = plan
                        }
                    }
                } label: {
                    HStack {
                        Text(draftQuotaPlan.displayTitle)
                        Spacer()
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.caption.weight(.bold))
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)

                Text(draftQuotaPlan.planSummary)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
    }
}

private extension UserRole {
    var displayTitle: String {
        switch self {
        case .owner:
            return "Owner"
        case .admin:
            return "Admin"
        case .subadmin:
            return "Subadmin"
        case .user:
            return "User"
        }
    }

    var roleSummary: String {
        switch self {
        case .owner:
            return "Festes Hauptkonto der App. Fuer diese App ist nash.lioncorna@gmail.com immer der Owner. Root-Zugriff auf alles, inklusive Shopify, Zahlungen, Rollen, n8n und Recovery."
        case .admin:
            return "Teaminterne Leute. Der Owner weist ihnen gezielt Funktionen wie Music, Video oder Profil-Moderation zu. Kein Zugriff auf Owner-Systembereiche."
        case .subadmin:
            return "Externe Premium-Konten mit buchbarem Kontingentmodell. Kein Admin-Workspace, keine Owner-Rechte."
        case .user:
            return "Normales Nutzerkonto mit Free-Kontingent. Nicht eingeloggte Leute sind zusaetzlich Gast-Nutzer ohne gespeichertes Konto."
        }
    }
}

private extension UserQuotaPlan {
    var displayTitle: String {
        switch self {
        case .ownerUnlimited:
            return "Owner Unlimited"
        case .internalTeam:
            return "Internal Team"
        case .free:
            return "Free"
        case .creator:
            return "Creator"
        case .studio:
            return "Studio"
        }
    }

    var planSummary: String {
        switch self {
        case .ownerUnlimited:
            return "Praktisch unbegrenztes Owner-Kontingent fuer Systemsteuerung, Tests und Recovery."
        case .internalTeam:
            return "Internes Team-Kontingent fuer feste Mitarbeiter."
        case .free:
            return "Basiszugang mit kleinem Free-Kontingent."
        case .creator:
            return "Erweitertes Creator-Kontingent fuer regelmaessige Nutzung."
        case .studio:
            return "Grosses Studio-Kontingent fuer intensivere Nutzung und laengere History."
        }
    }
}

private enum SettingsEditableImageTarget: String, Identifiable {
    case homeHeader
    case musicHubHeader
    case shopHeader
    case videoHeader

    var id: String { rawValue }
}

#Preview {
    SettingsView(colorScheme: .constant("system"))
        .environmentObject(AuthManager())
        .environment(\.colorScheme, .light)
}
