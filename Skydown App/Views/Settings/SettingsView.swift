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
import FirebaseFunctions

struct SettingsView: View {
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject private var aiSubscriptionStore: NativeAISubscriptionStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var environmentColorScheme
    @Environment(\.openURL) private var openURL


    @ObservedObject private var aiVisualReferenceLibrary = AIVisualReferenceLibraryStore.shared
    @ObservedObject private var aiPromptSettingsStore = AIPromptSettingsStore.shared
    @ObservedObject private var aiRuntimeSettingsStore = AIRuntimeSettingsStore.shared
    @ObservedObject private var aiFaqOwnerReviewLoopStore = AIFaqOwnerReviewLoopStore.shared
    @ObservedObject private var adminUserManagementStore = AdminUserManagementStore.shared
    @ObservedObject private var commerceSettingsStore = CommerceSettingsStore.shared
    @ObservedObject private var merchStoreStatusStore = MerchStoreStatusStore.shared
    @ObservedObject private var paymentMethodSettingsStore = PaymentMethodSettingsStore.shared
    @ObservedObject private var membershipOpsStore = MembershipOpsCommandCenterStore.shared
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared
    @ObservedObject private var stripeBackendSecretsStore = StripeBackendSecretsStore.shared
    @ObservedObject private var artistPagesStore = ArtistPagesStore.shared
    @ObservedObject private var shopifyAdminSettingsStore = ShopifyAdminSettingsStore.shared
    @ObservedObject private var workflowAutomationSettings = WorkflowAutomationSettingsStore.shared
    @ObservedObject private var manusByosStore = ManusBYOSStore.shared
    @ObservedObject private var legalContentStore = LegalContentStore.shared
    @ObservedObject private var notificationPermissionStore = NotificationPermissionStore.shared
    @Binding var colorScheme: String
    private let initialAdminWorkspaceRawValue: String?
    /// When set, Owner Hub can hand off a prefilled Agent prompt (e.g. Daily Briefing) and dismiss Settings.
    private let onOpenAgentWithPrompt: ((String) -> Void)?

    init(
        colorScheme: Binding<String>,
        initialAdminWorkspaceRawValue: String? = nil,
        onOpenAgentWithPrompt: ((String) -> Void)? = nil
    ) {
        _colorScheme = colorScheme
        self.initialAdminWorkspaceRawValue = initialAdminWorkspaceRawValue
        self.onOpenAgentWithPrompt = onOpenAgentWithPrompt
    }

    @State private var systemLanguage = AppLanguageSupport.currentSystemLanguageDisplayName()

    @State private var activeAlert: SettingsAlert?
    @State private var sheetPresentation = SkydownQueuedPresentation<SettingsPresentedSheet>()
    @State private var didPresentInitialAdminWorkspace = false
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success
    @State private var isRunningControlCenterCheck = false
    @State private var activeEditableImageUploadTarget: SettingsEditableImageTarget?
    @State private var activeAdminWorkspace: SettingsAdminWorkspaceSection?
    @State private var showingMailOptions = false
    @State private var stripeAccountHintDraft = ""
    @State private var stripeSecretKeyDraft = ""
    @State private var stripeWebhookSecretDraft = ""
    @State private var aiSubscriptionsEnabledDraft = false
    @State private var aiCreatorPriceIDDraft = ""
    @State private var aiStudioPriceIDDraft = ""
    @State private var aiIOSCreatorProductIDDraft = ""
    @State private var aiIOSStudioProductIDDraft = ""
    @State private var aiIOSAppAppleIDDraft = ""
    @State private var aiAndroidCreatorProductIDDraft = ""
    @State private var aiAndroidStudioProductIDDraft = ""
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
    @State private var shopifyCollectionHandlesDraft = ""
    @State private var shopifyCollectionSearchDraft = ""
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
    @State private var videoHeaderHeroVideoURLDraft = ""
    @State private var videoHeaderEyebrowDraft = ""
    @State private var videoHeaderTitleDraft = ""
    @State private var videoHeaderSubtitleDraft = ""
    @State private var videoHeaderDetailDraft = ""
    @State private var automationProviderDraft = "activepieces"
    @State private var automationEnabledDraft = false
    @State private var automationSendsUserContextDraft = true
    @State private var automationWorkflowNameDraft = ""
    @State private var automationBaseURLDraft = ""
    @State private var automationWebhookPathDraft = ""
    @State private var automationAuthHeaderNameDraft = ""
    @State private var automationAuthHeaderValueDraft = ""
    @State private var automationKnowledgeContextDraft = ""
    @State private var isSavingAutomationSettings = false
    @State private var isRunningAutomationTest = false
    @State private var automationInlineFeedbackMessage = ""
    @State private var automationInlineFeedbackStyle: ToastStyle = .info
    @State private var automationInlineFeedbackTimestamp: Date?
    @State private var manusByosEnabledDraft = false
    @State private var manusByosAPIKeyDraft = ""
    @State private var isValidatingManusKey = false
    @State private var manusValidationStatus = "unvalidated"
    @State private var manusValidationMessage = "Noch nicht geprueft."
    @State private var aiTextInstructionDraft = ""
    @State private var aiVisualInstructionDraft = ""
    @State private var aiAgentSystemInstructionDraft = ""
    @State private var aiFAQInstructionDraft = ""
    @State private var aiFAQKnowledgeBaseDraft = ""
    @State private var aiAssetLibraryLinkDraft = ""
    @State private var aiAssetReferenceNotesDraft = ""
    @State private var aiCostGuardEnabledDraft = true
    @State private var aiBotPromptVersionDraft = ""
    @State private var aiBotQualityModeDraft = "balanced"
    @State private var aiBotFAQModeDraft = "auto"
    @State private var aiBotOwnerModeDraft = "standard"
    @State private var aiBotAnswerLengthDraft = "adaptive"
    @State private var aiBotPersonalityStyleDraft = ""
    @State private var aiBotLoggingLevelDraft = ""
    @State private var aiBotDiagnosticsModeDraft = "owner_only"
    @State private var aiBotKillSwitchDraft = false
    @State private var aiBotTextPrimaryModelDraft = ""
    @State private var aiBotTextFallbackModelDraft = ""
    @State private var aiBotVisualPrimaryModelDraft = ""
    @State private var aiBotVisualFallbackModelDraft = ""
    @State private var aiBotCostGuardEnabledDraft = true
    @State private var aiBotPreferBriefCriticalDraft = true
    @State private var aiBotShortAnswerMaxTokensDraft = ""
    @State private var aiBotStandardAnswerMaxTokensDraft = ""
    @State private var aiBotPreferFaqRoutingDraft = true
    @State private var aiBotPreferProductGuideDraft = true
    @State private var aiBotAllowVisualGenerationDraft = true
    @State private var aiBotAllowTextFallbackDraft = true
    @State private var aiBotAllowVisualFallbackDraft = true
    @State private var aiBotExposeFallbackReasonDraft = true
    @State private var aiBotSafeModeEnabledDraft = true
    @State private var aiBotStrictUnknownHandlingDraft = true
    @State private var aiBotBlockSpeculativeFAQDraft = true
    @State private var aiBotProactiveHintsEnabledDraft = true
    @State private var aiBotTriggerAiLimitNearEnabledDraft = true
    @State private var aiBotTriggerRestoreAvailableEnabledDraft = true
    @State private var aiBotTriggerOrderShippedEnabledDraft = true
    @State private var aiBotTriggerPaymentMethodsChangedEnabledDraft = true
    @State private var aiBotTriggerUsageBasedUpgradeEnabledDraft = true
    @State private var aiBotWarningThresholdPercentDraft = ""
    @State private var aiBotCriticalThresholdPercentDraft = ""
    @State private var aiBotUpgradeHintFreeToProTextDraft = ""
    @State private var aiBotUpgradeHintProToCreatorTextDraft = ""
    @State private var aiBotFaqPriorityModeDraft = "live_owner_generic"
    @State private var aiBotPromptVersionAliasDraft = ""
    @State private var aiFaqReviewLoopWindowDays = 30
    @State private var aiAgentProviderDraft: AIRuntimeAgentProvider = .grok
    @State private var aiFallbackAgentProviderDraft: AIRuntimeAgentProvider = .gemini
    @State private var aiManusEnabledDraft = false
    @State private var aiManusRequestTimeoutMsDraft = ""
    @State private var aiManusPollIntervalMsDraft = ""
    @State private var aiManusMaxPollAttemptsDraft = ""
    @State private var aiManusListMessagesLimitDraft = ""
    @State private var aiManusMaxPromptCharsDraft = ""
    @State private var aiManusMaxHistoryTurnsDraft = ""
    @State private var aiManusAutoStopOnWaitingDraft = true
    @State private var aiManusBlockHighCreditEventsDraft = true
    @State private var aiManusIncludeVerboseEventsDraft = false
    @State private var aiHardTextLimitDraft = ""
    @State private var aiHardVisualLimitDraft = ""
    @State private var aiHardAgentLimitDraft = ""
    @State private var aiGlobalTextLimitDraft = ""
    @State private var aiGlobalVisualLimitDraft = ""
    @State private var aiGlobalAgentLimitDraft = ""
    @State private var legalBrandNameDraft = ""
    @State private var legalOperatorNameDraft = ""
    @State private var legalRightsHolderNameDraft = ""
    @State private var legalSupportEmailDraft = ""
    @State private var legalLastUpdatedLabelDraft = ""
    @State private var legalImprintReferenceDraft = ""
    @State private var legalMasterNumberMeaningDraft = ""
    @State private var legalBrandManifestoDraft = ""
    @State private var legalSymbolicNumericCodeDraft = ""
    @State private var legalSymbolicLeetCodeDraft = ""
    @State private var legalSymbolicCodeExplanationDraft = ""
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

    private var canPresentInAppMailComposer: Bool {
        SkydownPlatform.supportsInAppMailComposer && MFMailComposeViewController.canSendMail()
    }

    private var shopifyCatalogURL: URL? {
        let normalizedDomain = shopifyAdminSettingsStore.settings.storeDomain
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "https://", with: "")
            .replacingOccurrences(of: "http://", with: "")
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !normalizedDomain.isEmpty else { return nil }

        let handle = shopifyAdminSettingsStore.settings.primaryCollectionHandle?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let path = handle.isEmpty ? "" : "/collections/\(handle)"
        return URL(string: "https://\(normalizedDomain)\(path)")
    }

    private var automationWorkflowNameTrimmed: String {
        automationWorkflowNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var automationBaseURLTrimmed: String {
        automationBaseURLDraft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var automationWebhookPathTrimmed: String {
        automationWebhookPathDraft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var automationAuthHeaderNameTrimmed: String {
        automationAuthHeaderNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var automationAuthHeaderValueTrimmed: String {
        automationAuthHeaderValueDraft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var automationKnowledgeContextTrimmed: String {
        automationKnowledgeContextDraft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var automationNormalizedBaseURLDraft: String? {
        normalizeAutomationBaseURLDraft(automationBaseURLTrimmed)
    }

    private var automationNormalizedWebhookPathDraft: String? {
        normalizeAutomationWebhookPathDraft(automationWebhookPathTrimmed)
    }

    private var automationDraftResolvedWebhookURL: String? {
        guard let normalizedBaseURL = automationNormalizedBaseURLDraft else { return nil }
        guard let normalizedPath = automationNormalizedWebhookPathDraft, !normalizedPath.isEmpty else {
            return normalizedBaseURL
        }
        return "\(normalizedBaseURL)/\(normalizedPath)"
    }

    private var automationDraftNormalizedSettings: WorkflowAutomationSettings {
        let scope = isOwnerUser ? "owner_global" : "user_personal"
        let provider = scope == "user_personal" && automationProviderDraft == "n8n" ? "n8n" : "activepieces"
        return WorkflowAutomationSettings(
            provider: provider,
            scope: scope,
            isEnabled: automationEnabledDraft,
            sendsUserContext: automationSendsUserContextDraft,
            workflowName: automationWorkflowNameTrimmed.isEmpty ? WorkflowAutomationSettings.default.workflowName : automationWorkflowNameTrimmed,
            baseURL: automationNormalizedBaseURLDraft ?? "",
            webhookPath: automationNormalizedWebhookPathDraft ?? "",
            authHeaderName: automationAuthHeaderNameTrimmed,
            authHeaderValue: automationAuthHeaderValueTrimmed,
            knowledgeContext: automationKnowledgeContextTrimmed
        )
    }

    private var automationPersistedNormalizedSettings: WorkflowAutomationSettings {
        normalizedAutomationSettings(workflowAutomationSettings.settings)
    }

    private var automationHasUnsavedChanges: Bool {
        automationDraftNormalizedSettings != automationPersistedNormalizedSettings
    }

    private var automationBlockingValidationMessages: [String] {
        var messages: [String] = []

        if automationEnabledDraft {
            if automationBaseURLTrimmed.isEmpty {
                messages.append("Activepieces Base URL fehlt.")
            } else if automationNormalizedBaseURLDraft == nil {
                messages.append("Activepieces Base URL ist ungueltig.")
            }
        }

        if !automationAuthHeaderValueTrimmed.isEmpty && automationAuthHeaderNameTrimmed.isEmpty {
            messages.append("Auth Header Name fehlt, obwohl ein Auth Header Value gesetzt ist.")
        }

        return messages
    }

    private var automationAdvisoryMessages: [String] {
        var messages: [String] = []

        if automationEnabledDraft && automationWebhookPathTrimmed.isEmpty {
            messages.append("Webhook Path ist leer. Es wird nur die Base URL verwendet.")
        }

        if !automationAuthHeaderNameTrimmed.isEmpty && automationAuthHeaderValueTrimmed.isEmpty {
            messages.append("Auth Header Name ist gesetzt. Ohne Value wird trotzdem kein Auth Header gesendet.")
        }

        if !automationEnabledDraft && (!automationBaseURLTrimmed.isEmpty || !automationWebhookPathTrimmed.isEmpty) {
            messages.append("Workflow ist gerade deaktiviert. Speichern aktiviert dieses Setup erst beim naechsten Einschalten.")
        }

        return messages
    }

    private var automationSaveDisabledReasons: [String] {
        var reasons: [String] = []

        if isSavingAutomationSettings {
            reasons.append("Workflow wird gerade gespeichert.")
        }
        if isRunningAutomationTest {
            reasons.append("Ein Workflow-Test laeuft bereits.")
        }
        if !automationHasUnsavedChanges {
            reasons.append("Keine ungespeicherten Aenderungen.")
        }

        reasons.append(contentsOf: automationBlockingValidationMessages)
        return reasons
    }

    private var automationTestDisabledReasons: [String] {
        var reasons: [String] = []

        if isRunningAutomationTest {
            reasons.append("Ein Workflow-Test laeuft bereits.")
        }
        if isSavingAutomationSettings {
            reasons.append("Bitte warten, bis Speichern abgeschlossen ist.")
        }
        if !automationEnabledDraft {
            reasons.append("Workflow ist deaktiviert.")
        }

        reasons.append(contentsOf: automationBlockingValidationMessages)
        return reasons
    }

    private var automationStatusStyle: ToastStyle {
        if !automationBlockingValidationMessages.isEmpty {
            return .error
        }
        if !automationInlineFeedbackMessage.isEmpty && !automationHasUnsavedChanges {
            return automationInlineFeedbackStyle
        }
        if automationHasUnsavedChanges {
            return .warning
        }
        return automationEnabledDraft ? .success : .info
    }

    private var automationStatusTitle: String {
        if !automationBlockingValidationMessages.isEmpty {
            return "Konfiguration braucht Korrektur"
        }
        if automationHasUnsavedChanges {
            return "Aenderungen noch nicht gespeichert"
        }
        if isOwnerUser {
            return automationEnabledDraft ? "Owner-Flow ist einsatzbereit" : "Owner-Flow ist deaktiviert"
        }
        return automationEnabledDraft ? "Eigener Workflow ist einsatzbereit" : "Eigener Workflow ist deaktiviert"
    }

    private var automationStatusMessage: String {
        if !automationBlockingValidationMessages.isEmpty {
            return "Bitte korrigiere die Punkte unten, dann kann gespeichert und getestet werden."
        }
        if automationHasUnsavedChanges {
            return isOwnerUser
                ? "Die Eingaben sind erfasst. Speichere, damit sie fuer den globalen Owner-Flow aktiv werden."
                : "Die Eingaben sind erfasst. Speichere, damit dein eigener Workflow triggerbar wird."
        }
        if isOwnerUser {
            return automationEnabledDraft
                ? "Alle Rollen laufen ueber den globalen Owner-Flow. Das Backend prueft Rolle, Abo und Limits vor jedem Trigger."
                : "Der globale Owner-Flow ist vorbereitet, aber fuer Agent-Aktionen noch pausiert."
        }
        return automationEnabledDraft
            ? "Dein eigener Workflow kann im Agent bewusst ausgefuehrt werden."
            : "Du kannst den Workflow vorbereiten und spaeter aktivieren."
    }

    private var automationInlineFeedbackSummary: String? {
        guard !automationInlineFeedbackMessage.isEmpty else { return nil }
        guard let timestamp = automationInlineFeedbackTimestamp else {
            return automationInlineFeedbackMessage
        }
        let timeLabel = timestamp.formatted(date: .omitted, time: .shortened)
        return "\(automationInlineFeedbackMessage) (\(timeLabel))"
    }

    private var automationStatusDetails: [String] {
        var details: [String] = []

        if let feedback = automationInlineFeedbackSummary {
            details.append("Letztes Feedback: \(feedback)")
        }

        if let resolvedWebhookURL = automationDraftResolvedWebhookURL {
            details.append("Webhook: \(resolvedWebhookURL)")
        } else if automationEnabledDraft {
            details.append("Webhook wird nach einer gueltigen Base URL erzeugt.")
        }

        details.append(contentsOf: automationBlockingValidationMessages)
        details.append(contentsOf: automationAdvisoryMessages)
        return details
    }

    var body: some View {
        NavigationStack {
            GeometryReader { geometry in
                let layout = SkydownResponsiveLayout(availableWidth: geometry.size.width)
                let contentWidth = min(
                    layout.contentMaxWidth,
                    max(geometry.size.width - (layout.horizontalPadding * 2), 0)
                )

                ScrollView {
                    LazyVStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    SettingsHeroCard(
                        colorScheme: effectiveColorScheme,
                        username: authManager.userSession?.username,
                        isLoggedIn: authManager.userSession != nil,
                        isOwner: authManager.userSession?.isPlatformOwner == true,
                        notificationsEnabled: notificationPermissionStore.notificationsEnabled,
                        appearance: currentAppearanceLabel
                    )

                    if authManager.userSession?.isPlatformOwner == true {
                        NavigationLink {
                            OwnerHubView(onOpenAgentWithPrompt: onOpenAgentWithPrompt)
                        } label: {
                            HStack(spacing: SkydownLayout.stackSpacingCompact) {
                                Image(systemName: "chart.bar.doc.horizontal")
                                    .font(.title3.weight(.semibold))
                                    .foregroundColor(AppColors.accentMystic(for: effectiveColorScheme))
                                    .frame(width: 40, height: 40)
                                    .background(
                                        Circle()
                                            .fill(AppColors.accentMystic(for: effectiveColorScheme).opacity(0.14))
                                    )

                                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                                    Text(AppLocalized.text("settings.owner_hub.link_title", fallback: "Owner hub"))
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundColor(AppColors.text(for: effectiveColorScheme))
                                    Text(AppLocalized.text("settings.owner_hub.link_subtitle", fallback: "Same flow as Home: Agent briefing, then Tasks, Notes, or reminders."))
                                        .font(.caption)
                                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                                        .fixedSize(horizontal: false, vertical: true)
                                }

                                Spacer(minLength: 0)

                                Image(systemName: "chevron.right")
                                    .font(.caption.weight(.bold))
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme).opacity(0.7))
                            }
                            .padding(14)
                            .skydownPanelSurface(
                                colorScheme: effectiveColorScheme,
                                accent: AppColors.accentMystic(for: effectiveColorScheme),
                                cornerRadius: SkydownLayout.messageBubbleRadius,
                                shadowRadius: 8,
                                shadowYOffset: 4
                            )
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("settings.open_owner_hub")
                    }

                    controlCenterSectionCard

                    SettingsSectionCard(title: AppLocalized.text("settings.section.profile_account", fallback: "Profile / Account"), colorScheme: effectiveColorScheme) {
                        if let user = authManager.userSession {
                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                                Text("\(AppLocalized.text("settings.logged_in_as", fallback: "Signed in as")) \(user.username)")
                                    .font(.headline)
                                    .foregroundColor(AppColors.text(for: effectiveColorScheme))

                                Button {
                                    presentSheet(.profileEditor)
                                } label: {
                                    Label(AppLocalized.text("settings.profile.edit", fallback: "Edit profile"), systemImage: "person.crop.circle")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent)
                                .skydownInteractiveFeedback()
                                .tint(AppColors.accent(for: effectiveColorScheme))
                                .accessibilityIdentifier("settings.open_profile_editor")

                                VStack(spacing: SkydownLayout.stackSpacingPill) {
                                    Button(role: .destructive) {
                                        activeAlert = .logout
                                    } label: {
                                        Label(AppLocalized.text("settings.logout", fallback: "Log out"), systemImage: "person.crop.circle.badge.xmark")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .skydownInteractiveFeedback()
                                    .accessibilityIdentifier("settings.logout")

                                    Button {
                                        Task {
                                            await authManager.signOut()
                                            await MainActor.run {
                                                presentSheet(.login(.settings))
                                            }
                                        }
                                    } label: {
                                        Text(AppLocalized.text("settings.switch_account", fallback: "Switch account"))
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.bordered)
                                    .skydownInteractiveFeedback()
                                    .accessibilityIdentifier("settings.switch_account")

                                    Button(role: .destructive) {
                                        activeAlert = .deleteAccount
                                    } label: {
                                        Label(AppLocalized.text("settings.delete_account", fallback: "Delete account"), systemImage: "person.fill.xmark")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.bordered)
                                    .skydownInteractiveFeedback()
                                }
                            }
                        } else {
                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                                Button {
                                    presentSheet(.login(.settings))
                                } label: {
                                    Label(AppLocalized.text("auth.sign_in", fallback: "Sign in"), systemImage: "person.crop.circle.fill.badge.plus")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent)
                                .skydownInteractiveFeedback()
                                .tint(AppColors.accent(for: effectiveColorScheme))
                                .accessibilityIdentifier("settings.open_login")

                                Button {
                                    presentSheet(.registration)
                                } label: {
                                    Label(AppLocalized.text("auth.register", fallback: "Register"), systemImage: "person.crop.circle.badge.plus")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.bordered)
                                .skydownInteractiveFeedback()
                                .tint(AppColors.accentMystic(for: effectiveColorScheme))
                                .accessibilityIdentifier("settings.open_registration")
                            }
                        }
                    }

                    if authManager.userSession != nil {
                        personalAgentServiceSectionCard
                    }

                    membershipSectionCard

                    if isOwnerUser {
                        adminWorkspaceSectionCard
                    }

                    SettingsSectionCard(title: AppLocalized.text("settings.section.system", fallback: "System"), colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                            HStack {
                                Text(
                                    AppLocalized.text(
                                        "settings.system_language",
                                        fallback: "System language"
                                    )
                                )
                                    .foregroundColor(AppColors.text(for: effectiveColorScheme))
                                Spacer()
                                Text(systemLanguage)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                            }

                            SettingsToggleCard(
                                colorScheme: effectiveColorScheme,
                                title: AppLocalized.text(
                                    "settings.notifications.title",
                                    fallback: "Notifications"
                                ),
                                subtitle: AppLocalized.text(
                                    "settings.notifications.subtitle",
                                    fallback: "Push for important actions."
                                ),
                                isOn: notificationsToggleBinding
                            )
                        }
                    }

                    SettingsSectionCard(title: AppLocalized.text("settings.section.theme", fallback: "Theme"), colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
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

                    SettingsSectionCard(title: AppLocalized.text("settings.section.privacy_legal_help", fallback: "Privacy / Legal / Help"), colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                            Text("\(AppLocalized.text("settings.version", fallback: "Version")) \(appVersion)")
                                .font(.headline)
                                .foregroundColor(AppColors.text(for: effectiveColorScheme))

                            Button(AppLocalized.text("settings.faq_guide", fallback: "FAQ / Guide")) {
                                presentSheet(.appGuide)
                            }
                            .buttonStyle(.bordered)
                            .skydownInteractiveFeedback()

                            Button(AppLocalized.text("settings.terms_local", fallback: "Terms (local)")) {
                                presentSheet(.termsAndConditions)
                            }
                            .buttonStyle(.bordered)
                            .skydownInteractiveFeedback()

                            Button(AppLocalized.text("settings.privacy", fallback: "Privacy")) {
                                presentSheet(.privacyPolicy)
                            }
                            .buttonStyle(.bordered)
                            .skydownInteractiveFeedback()

                            Button(AppLocalized.text("settings.terms", fallback: "Terms")) {
                                presentSheet(.termsOfService)
                            }
                            .buttonStyle(.bordered)
                            .skydownInteractiveFeedback()

                            Button(AppLocalized.text("settings.legal.subscription_terms", fallback: "Subscription terms")) {
                                presentSheet(.subscriptionTerms)
                            }
                            .buttonStyle(.bordered)
                            .skydownInteractiveFeedback()

                            Button(AppLocalized.text("settings.legal.ai_usage", fallback: "AI usage notice")) {
                                presentSheet(.aiUsageNotice)
                            }
                            .buttonStyle(.bordered)
                            .skydownInteractiveFeedback()

                            Button(AppLocalized.text("settings.legal.imprint", fallback: "Imprint / company info")) {
                                presentSheet(.imprintInfo)
                            }
                            .buttonStyle(.bordered)
                            .skydownInteractiveFeedback()

                            Text(legalContentStore.settings.resolvedSupportEmail)
                                .font(.subheadline)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                            Button {
                                #if targetEnvironment(simulator)
                                if canPresentInAppMailComposer {
                                    presentSheet(.mailComposer)
                                } else {
                                    showToastMessage("Mail kann im Simulator nicht gesendet werden", style: .error)
                                }
                                #else
                                if SkydownPlatform.isDesktop {
                                    openMailAppFallback()
                                } else {
                                    showingMailOptions = true
                                }
                                #endif
                            } label: {
                                Label(AppLocalized.text("settings.support.send", fallback: "Send support request"), systemImage: "envelope.fill")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .skydownInteractiveFeedback()
                            .tint(AppColors.accent(for: effectiveColorScheme))

                            if isOwnerUser {
                                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                                    Text(AppLocalized.text("settings.legal.owner_title", fallback: "Legal (owner)"))
                                        .font(.headline)
                                        .foregroundColor(AppColors.text(for: effectiveColorScheme))

                                    Text(AppLocalized.text("settings.legal.owner_subtitle", fallback: "Maintain terms, privacy, and usage conditions without a release."))
                                        .font(.footnote)
                                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                                    SettingsInputField(
                                        title: "Brandname",
                                        text: $legalBrandNameDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "z. B. SkyOS"
                                    )

                                    SettingsInputField(
                                        title: "Betreiber / Vertragspartner",
                                        text: $legalOperatorNameDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "z. B. Yang D. Nash - Skydown"
                                    )

                                    SettingsInputField(
                                        title: "Rechteinhaber",
                                        text: $legalRightsHolderNameDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "z. B. Yang D. Nash - Skydown"
                                    )

                                    SettingsInputField(
                                        title: "Support E-Mail",
                                        text: $legalSupportEmailDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: AppLocalized.text(
                                            "settings.legal.support_email_placeholder",
                                            fallback: "Use your organization's real public support address (not a sample domain)."
                                        )
                                    )

                                    SettingsInputField(
                                        title: "Zuletzt aktualisiert",
                                        text: $legalLastUpdatedLabelDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "z. B. 12. April 2026"
                                    )

                                    SettingsMultilineInputField(
                                        title: "Impressum-Hinweis",
                                        text: $legalImprintReferenceDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "Hinweis zur Anbieterkennzeichnung.",
                                        minHeight: 94
                                    )

                                    SettingsMultilineInputField(
                                        title: "Meisterzahl 22 - Bedeutung",
                                        text: $legalMasterNumberMeaningDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "Die Meisterzahl 22 als Master Builder...",
                                        minHeight: 100
                                    )

                                    SettingsMultilineInputField(
                                        title: "Wer wir sind (Manifest)",
                                        text: $legalBrandManifestoDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "Dort, wo der Himmel faellt...",
                                        minHeight: 160
                                    )

                                    SettingsInputField(
                                        title: "Symbolcode (numerisch)",
                                        text: $legalSymbolicNumericCodeDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "z. B. 1337-514-731"
                                    )

                                    SettingsInputField(
                                        title: "Leet-Code",
                                        text: $legalSymbolicLeetCodeDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "z. B. 7H3_F4LL_0F_H34/3N"
                                    )

                                    SettingsMultilineInputField(
                                        title: "Code-Erklaerung",
                                        text: $legalSymbolicCodeExplanationDraft,
                                        colorScheme: effectiveColorScheme,
                                        placeholder: "Bedeutung, Zerlegung und Alternativen",
                                        minHeight: 140
                                    )

                                    Button(action: saveLegalContentSettings) {
                                        Label(AppLocalized.text("settings.legal.save", fallback: "Save legal content"), systemImage: "doc.text.fill")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .skydownInteractiveFeedback()
                                    .tint(AppColors.accentHighlight(for: effectiveColorScheme))
                                }
                            }

                            Text(AppLocalized.text("settings.legal.availability_note", fallback: "All help and legal texts are directly available."))
                                .font(.footnote)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                        }
                    }
                    }
                    .frame(maxWidth: contentWidth, alignment: .leading)
                    .padding(.horizontal, layout.horizontalPadding)
                    .padding(.top, SkydownLayout.screenTopPadding)
                    .padding(.bottom, SkydownLayout.screenBottomPadding)
                    .frame(maxWidth: .infinity)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .scrollIndicators(.hidden)
            .navigationTitle(AppLocalized.text("settings.title", fallback: "Settings"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Label(AppLocalized.text("common.close", fallback: "Close"), systemImage: "xmark")
                    }
                    .accessibilityIdentifier("settings.close")
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
        .sheet(item: activePresentedSheetBinding) { sheet in
            settingsSheetContent(for: sheet)
                .fancyToast(isPresented: $showToast, message: toastMessage, style: toastStyle)
        }
        .confirmationDialog(
            AppLocalized.text("settings.support.send", fallback: "Send support request"),
            isPresented: $showingMailOptions,
            titleVisibility: .visible
        ) {
            Button(AppLocalized.text("settings.support.send_in_app", fallback: "Send in app")) {
                if canPresentInAppMailComposer {
                    presentSheet(.mailComposer)
                } else {
                    showToastMessage("Mail kann auf diesem Geraet nicht gesendet werden", style: .error)
                }
            }

            Button(AppLocalized.text("settings.support.open_mail_app", fallback: "Open Mail app")) {
                openMailAppFallback()
            }

            Button(AppLocalized.text("common.cancel", fallback: "Cancel"), role: .cancel) {}
        }
        .alert(item: $activeAlert) { alert in
            switch alert {
            case .logout:
                return Alert(
                    title: Text(AppLocalized.text("settings.logout", fallback: "Log out")),
                    message: Text(AppLocalized.text("settings.logout.confirm", fallback: "Do you really want to log out?")),
                    primaryButton: .destructive(Text(AppLocalized.text("settings.logout", fallback: "Log out"))) {
                        Task { await authManager.signOut() }
                    },
                    secondaryButton: .cancel()
                )
            case .deleteAccount:
                return Alert(
                    title: Text(AppLocalized.text("settings.delete_account", fallback: "Delete account")),
                    message: Text(AppLocalized.text("settings.delete_account.confirm", fallback: "Do you want to permanently delete your account?")),
                    primaryButton: .destructive(Text(AppLocalized.text("settings.delete_account", fallback: "Delete account"))) {
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
            systemLanguage = AppLanguageSupport.currentSystemLanguageDisplayName()
            manusByosStore.setUserMode(userID: authManager.userSession?.id)
            manusByosAPIKeyDraft = ""
            syncProfileDrafts(with: authManager.userSession)
            syncPaymentDrafts(with: paymentMethodSettingsStore.settings)
            syncCommerceDrafts(with: commerceSettingsStore.settings)
            syncShopifyDrafts(with: shopifyAdminSettingsStore.settings)
            syncScreenHeaderDrafts(with: screenHeaderSettingsStore.settings)
            syncAutomationDrafts(with: workflowAutomationSettings.settings)
            syncManusBYOSDrafts(with: manusByosStore.settings)
            syncAIPromptDrafts(with: aiPromptSettingsStore.settings)
            syncAIRuntimeDrafts(with: aiRuntimeSettingsStore.settings)
            syncLegalContentDrafts(with: legalContentStore.settings)
            refreshOwnerWorkspaceObservation(for: activeAdminWorkspace)
            presentInitialAdminWorkspaceIfNeeded()
        }
        .task {
            await notificationPermissionStore.refresh()
        }
        .task(id: authManager.userSession?.id) {
            guard canUseAISelfPaySubscription else { return }
            await aiSubscriptionStore.prepareStorefront(for: authManager.userSession)
        }
        .onReceive(NotificationCenter.default.publisher(for: NSLocale.currentLocaleDidChangeNotification)) { _ in
            systemLanguage = AppLanguageSupport.currentSystemLanguageDisplayName()
        }
        .task(id: authManager.userSession?.isPlatformOwner == true) {
            guard authManager.userSession?.isPlatformOwner == true else { return }
            await shopifyAdminSettingsStore.refreshAvailableCollections()
        }
        .onChange(of: isOwnerUser) { _, isOwner in
            guard !isOwner else {
                refreshOwnerWorkspaceObservation(for: activeAdminWorkspace)
                return
            }

            adminUserManagementStore.configureObservation(isAdmin: false)
            stripeBackendSecretsStore.setObservationEnabled(false)
            aiPromptSettingsStore.setObservationEnabled(false)
            aiRuntimeSettingsStore.setObservationEnabled(false)
            refreshOwnerWorkspaceObservation(for: activeAdminWorkspace)
        }
        .onChange(of: authManager.userSession?.id) { _, userID in
            manusByosStore.setUserMode(userID: userID)
            syncManusBYOSDrafts(with: manusByosStore.settings)
            manusByosAPIKeyDraft = ""
            syncProfileDrafts(with: authManager.userSession)
            refreshOwnerWorkspaceObservation(for: activeAdminWorkspace, userID: userID)
        }
        .onChange(of: activeAdminWorkspace) { _, section in
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
        .onReceive(screenHeaderSettingsStore.$settings) { settings in
            syncScreenHeaderDrafts(with: settings)
        }
        .onReceive(workflowAutomationSettings.$settings) { settings in
            syncAutomationDrafts(with: settings)
        }
        .onReceive(manusByosStore.$settings) { settings in
            syncManusBYOSDrafts(with: settings)
        }
        .onReceive(aiPromptSettingsStore.$settings) { settings in
            syncAIPromptDrafts(with: settings)
        }
        .onReceive(aiRuntimeSettingsStore.$settings) { settings in
            syncAIRuntimeDrafts(with: settings)
        }
        .onReceive(legalContentStore.$settings) { settings in
            syncLegalContentDrafts(with: settings)
        }
        .onChange(of: activePresentedSheetBinding.wrappedValue) { _, sheet in
            switch sheet {
            case .adminWorkspace(let section):
                activeAdminWorkspace = section
            default:
                activeAdminWorkspace = nil
            }
        }
        .onDisappear {
            adminUserManagementStore.configureObservation(isAdmin: false)
            stripeBackendSecretsStore.setObservationEnabled(false)
            workflowAutomationSettings.configureObservation(isEnabled: false, userID: nil)
            aiPromptSettingsStore.setObservationEnabled(false)
            aiRuntimeSettingsStore.setObservationEnabled(false)
        }
    }

    private var currentAppearanceLabel: String {
        Appearance(rawValue: colorScheme)?.rawValue.capitalized ?? "System"
    }

    private var notificationsToggleBinding: Binding<Bool> {
        Binding(
            get: { notificationPermissionStore.notificationsEnabled },
            set: { isEnabled in
                Task {
                    if isEnabled {
                        let granted = await notificationPermissionStore.requestAuthorization()
                        if granted {
                            showToastMessage(
                                AppLocalized.text(
                                    "settings.notifications.toast.enabled",
                                    fallback: "Notifications are enabled."
                                ),
                                style: .success
                            )
                        } else {
                            showToastMessage(
                                AppLocalized.text(
                                    "settings.notifications.toast.disabled",
                                    fallback: "Notifications are off. You can enable them in iOS Settings."
                                ),
                                style: .warning
                            )
                            notificationPermissionStore.openSystemSettings()
                        }
                    } else {
                        showToastMessage(
                            AppLocalized.text(
                                "settings.notifications.toast.manage_in_settings",
                                fallback: "Manage notifications in iOS Settings."
                            ),
                            style: .info
                        )
                        notificationPermissionStore.openSystemSettings()
                        await notificationPermissionStore.refresh()
                    }
                }
            }
        )
    }

    private func handleEditableImageProvider(
        _ temporaryFileURL: URL?,
        for target: SettingsEditableImageTarget
    ) {
        activePresentedSheetBinding.wrappedValue = nil

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

    private var activePresentedSheetBinding: Binding<SettingsPresentedSheet?> {
        Binding(
            get: { sheetPresentation.activeItem },
            set: { sheetPresentation.updatePresentedItem($0) }
        )
    }

    private var canUseAISelfPaySubscription: Bool {
        guard let user = authManager.userSession else {
            return false
        }

        return !user.isPlatformOwner
    }

    private func aiSubscriptionStatusLine(for user: User) -> String? {
        let statusTitle: String
        switch user.normalizedAISubscriptionStatus {
        case "active":
            statusTitle = "Abo aktiv"
        case "trialing":
            statusTitle = "Abo im Testzeitraum"
        case "past_due":
            statusTitle = "Zahlung offen"
        case "checkout_pending":
            statusTitle = "Checkout offen"
        case "canceled":
            statusTitle = "Abo beendet"
        case "expired":
            statusTitle = "Checkout abgelaufen"
        default:
            statusTitle = ""
        }

        guard !statusTitle.isEmpty else {
            return nil
        }

        if let subscriptionPlan = user.resolvedAISubscriptionPlan {
            return "Status: \(statusTitle) · \(subscriptionPlan.displayTitle)"
        }

        return "Status: \(statusTitle)"
    }

    private func aiSubscriptionDetailLine(for user: User) -> String? {
        if user.hasOpenAISubscriptionCheckout, let expiryDate = user.aiSubscriptionCheckoutExpiryDate {
            return "Dein letzter Checkout bleibt bis \(formattedSettingsDate(expiryDate)) offen."
        }

        if let periodEndDate = user.aiSubscriptionCurrentPeriodEndDate {
            let prefix = user.aiSubscriptionCancelAtPeriodEnd ? "Laeuft noch bis" : "Naechste Verlaengerung"
            return "\(prefix): \(formattedSettingsDate(periodEndDate))"
        }

        return nil
    }

    private func aiSubscriptionPurchaseBlockedReason(for user: User) -> String? {
        if user.hasBlockingAISubscriptionState && user.normalizedAISubscriptionProvider == "stripe" {
            return "Dieses Konto hat bereits ein Web-Abo ueber Stripe. Bitte verwalte es erst dort, bevor du im App Store neu abschliesst."
        }

        if user.hasOpenAISubscriptionCheckout && user.normalizedAISubscriptionProvider == "stripe" {
            return "Fuer dieses Konto ist noch ein offener Web-Checkout aktiv. Bitte schliesse ihn erst ab oder lass ihn auslaufen, bevor du im App Store startest."
        }

        return nil
    }

    private func formattedSettingsDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "de_DE")
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
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
        let existingPages = artistPagesStore.pages(for: .zweizwei) + artistPagesStore.pages(for: .nicma)
        let requiredNicmaPages = ["NICMA MUSIC", "NICMA STUDIO"].map { name in
            artistPagesStore.page(for: .nicma, artistName: name)
        }
        var pagesBySlug: [String: ArtistPage] = [:]
        (existingPages + requiredNicmaPages).forEach { page in
            pagesBySlug[page.slug] = page
        }

        return pagesBySlug.values.sorted { lhs, rhs in
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
    private var controlCenterSectionCard: some View {
        SettingsSectionCard(title: "Control Center", colorScheme: effectiveColorScheme) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                SettingsInlineStatusStrip(
                    icon: "slider.horizontal.3",
                    title: "System aktiv",
                    message: "Konto, AI-Service, Zahlungen und Sicherheit zentral im Blick.",
                    detail: [
                        authManager.userSession == nil ? "Gastmodus" : "Konto aktiv",
                        aiRuntimeSettingsStore.settings.costGuardEnabled ? "KI Guard aktiv" : "KI Guard pruefen",
                        "\(visiblePaymentMethodCount) Checkout-Routen"
                    ].joined(separator: " · "),
                    accent: AppColors.accentMystic(for: effectiveColorScheme),
                    colorScheme: effectiveColorScheme
                )

                VStack(spacing: SkydownLayout.stackSpacingPill) {
                    Button {
                        Task { await runControlCenterHealthCheck() }
                    } label: {
                        Label(
                            isRunningControlCenterCheck ? "System wird geprueft ..." : "System pruefen",
                            systemImage: "waveform.path.ecg"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .skydownInteractiveFeedback()
                    .tint(AppColors.accentMystic(for: effectiveColorScheme))
                    .disabled(isRunningControlCenterCheck)

                    Button {
                        if authManager.userSession == nil {
                            presentSheet(.login(.settings))
                        } else {
                            presentSheet(.adminWorkspace(.automation))
                        }
                    } label: {
                        Label(
                            authManager.userSession == nil ? "Anmelden und Agent-Service starten" : "Agent-Service oeffnen",
                            systemImage: "bolt.horizontal.circle"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .skydownInteractiveFeedback()
                    .tint(AppColors.accent(for: effectiveColorScheme))

                    SettingsUtilityRow(
                        colorScheme: effectiveColorScheme,
                        actions: [
                            SettingsUtilityAction(
                                title: isOwnerUser ? "Zahlungen" : "Support",
                                systemImage: isOwnerUser ? "creditcard.fill" : "envelope.fill",
                                accent: AppColors.accentMystic(for: effectiveColorScheme),
                                action: {
                                    if isOwnerUser {
                                        presentSheet(.adminWorkspace(.payments))
                                    } else {
                                        presentSheet(.mailComposer)
                                    }
                                }
                            ),
                            SettingsUtilityAction(
                                title: "Datenschutz",
                                systemImage: "lock.shield",
                                accent: AppColors.accent(for: effectiveColorScheme),
                                action: { presentSheet(.privacyPolicy) }
                            ),
                            SettingsUtilityAction(
                                title: "Orders",
                                systemImage: "shippingbox.fill",
                                accent: AppColors.accentHighlight(for: effectiveColorScheme),
                                action: { presentSheet(.orders) }
                            )
                        ]
                    )
                }
            }
        }
        .accessibilityIdentifier("settings.control_center")
    }

    @ViewBuilder
    private var membershipSectionCard: some View {
        SettingsSectionCard(title: "Membership", colorScheme: effectiveColorScheme) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
                if let user = authManager.userSession {
                    if canUseAISelfPaySubscription {
                        NativeAISubscriptionStatusCard(
                            colorScheme: effectiveColorScheme,
                            user: user,
                            products: aiSubscriptionStore.products,
                            isStorefrontReady: aiSubscriptionStore.isStorefrontReady,
                            isLoadingProducts: aiSubscriptionStore.isLoadingProducts,
                            isSyncing: aiSubscriptionStore.isSyncing,
                            activePurchasePlan: aiSubscriptionStore.activePurchasePlan,
                            lastErrorMessage: aiSubscriptionStore.lastErrorMessage,
                            statusLine: aiSubscriptionStatusLine(for: user),
                            detailLine: aiSubscriptionDetailLine(for: user),
                            purchaseDisabledReason: aiSubscriptionPurchaseBlockedReason(for: user),
                            onPurchase: { plan in purchaseAISubscription(plan) },
                            onRestore: { restoreAISubscriptionPurchases() },
                            onManage: { manageAISubscription() }
                        )
                    } else {
                        SettingsLockedHintCard(
                            colorScheme: effectiveColorScheme,
                            text: "Membership wird fuer dieses Konto zentral serverseitig verwaltet."
                        )
                    }
                } else {
                    Button {
                        presentSheet(.login(.settings))
                    } label: {
                        Label(AppLocalized.text("auth.sign_in", fallback: "Sign in"), systemImage: "person.crop.circle.fill.badge.plus")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .skydownInteractiveFeedback()
                    .tint(AppColors.accent(for: effectiveColorScheme))
                }
            }
        }
        .accessibilityIdentifier("settings.membership.section")
    }

    @ViewBuilder
    private var adminWorkspaceSectionCard: some View {
        SettingsSectionCard(title: "Owner-Bereich", colorScheme: effectiveColorScheme) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                Text("Admin & Revenue zentral steuern.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                OwnerCommandCenterCard(
                    colorScheme: effectiveColorScheme,
                    isOwner: isOwnerUser,
                    paymentStatus: "\(visiblePaymentMethodCount) Zahlungsrouten",
                    userStatus: "\(adminUserManagementStore.users.count) Konten",
                    headerStatus: "\(configuredScreenHeaderCount) Header",
                    membershipStatus: adminWorkspaceStatusText(for: .membershipOps),
                    aiStatus: aiRuntimeSettingsStore.settings.costGuardEnabled ? "AI Guard aktiv" : "AI Guard pruefen",
                    onOpenUsers: { presentSheet(.adminWorkspace(.users)) },
                    onOpenPayments: { presentSheet(.adminWorkspace(.payments)) },
                    onOpenHeaders: { presentSheet(.adminWorkspace(.headers)) },
                    onOpenMembershipOps: { presentSheet(.adminWorkspace(.membershipOps)) },
                    onOpenAI: { presentSheet(.adminWorkspace(.aiPrompts)) }
                )
                .accessibilityIdentifier("settings.owner.command_center")

                Button {
                    presentSheet(.adminWorkspace(.users))
                } label: {
                    Label("Alle Admin-Bereiche", systemImage: "rectangle.grid.2x2")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .skydownInteractiveFeedback()
                .accessibilityIdentifier("settings.owner.open_workspace")

                Button {
                    presentSheet(.orders)
                } label: {
                    Label(AppLocalized.text("settings.orders.open", fallback: "Open orders"), systemImage: "suitcase.cart")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .skydownInteractiveFeedback()
                .accessibilityIdentifier("settings.owner.open_orders")
            }
        }
        .accessibilityIdentifier("settings.owner.section")
    }

    @ViewBuilder
    private var personalAgentServiceSectionCard: some View {
        SettingsSectionCard(title: "AI Control", colorScheme: effectiveColorScheme) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: SkydownLayout.stackSpacingPill) {
                    SettingsBadge(
                        text: workflowAutomationSettings.settings.isPrepared ? "Workflow bereit" : "Workflow offen",
                        colorScheme: effectiveColorScheme,
                        onTap: { presentSheet(.adminWorkspace(.automation)) }
                    )
                    if !workflowAutomationSettings.settings.workflowName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        SettingsBadge(
                            text: String(workflowAutomationSettings.settings.workflowName.prefix(26)),
                            colorScheme: effectiveColorScheme,
                            onTap: { presentSheet(.adminWorkspace(.automation)) }
                        )
                    }
                    SettingsBadge(
                        text: manusByosStore.settings.isEnabled && manusByosStore.settings.hasAPIKey ? "Manus BYOS aktiv" : "Manus BYOS aus",
                        colorScheme: effectiveColorScheme,
                        onTap: { presentSheet(.adminWorkspace(.automation)) }
                    )
                    }
                }

                Button {
                    presentSheet(.adminWorkspace(.automation))
                } label: {
                    Label(AppLocalized.text("settings.agent_service.manage", fallback: "Manage agent service"), systemImage: "bolt.horizontal.circle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .skydownInteractiveFeedback()
                .tint(AppColors.accent(for: effectiveColorScheme))

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                    Text(AppLocalized.text("settings.workflow_status.title", fallback: "Workflow status"))
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: effectiveColorScheme))
                    Text(AppLocalized.text("settings.workflow_status.reminder", fallback: "Reminder + Push: fully active"))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                    Text(AppLocalized.text("settings.workflow_status.tasks", fallback: "Tasks: live for capture and management"))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                    Text(AppLocalized.text("settings.workflow_status.notes", fallback: "Notes: live for capture and management"))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                }
                .padding(.top, 4)
            }
        }
    }

    @ViewBuilder
    private func adminWorkspaceContent(for section: SettingsAdminWorkspaceSection) -> some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            SettingsAdminWorkspaceSummaryCard(
                section: section,
                colorScheme: effectiveColorScheme
            )

            switch section {
            case .users:
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                    Text(AppLocalized.text("settings.system_control.roles_limits", fallback: "Roles, AI enablement, and daily limits per account."))
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
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
                        Text("Neue Konten erscheinen hier.")
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                    } else {
                        VStack(spacing: SkydownLayout.stackSpacingCompact) {
                            ForEach(adminUserManagementStore.users) { managedUser in
                                SettingsAdminUserCard(
                                    user: managedUser,
                                    isCurrentUser: managedUser.id == authManager.userSession?.id,
                                    colorScheme: effectiveColorScheme
                                ) { updatedUser in
                                    await saveManagedUser(updatedUser)
                                }
                            }
                        }
                    }
                }

            case .artists:
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                    Text("Artist-Seiten: Owner vergibt Editoren.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SettingsBadge(text: "\(publishedArtistPageCount) Seiten mit Inhalt", colorScheme: effectiveColorScheme)
                        SettingsBadge(text: "\(assignedArtistPageCount) mit Editoren", colorScheme: effectiveColorScheme)
                    }

                    if let message = artistPagesStore.lastErrorMessage {
                        Text(message)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(AppColors.accentHighlight(for: effectiveColorScheme))
                    }

                    VStack(spacing: SkydownLayout.stackSpacingCompact) {
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
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                    Text("Hero unter Home, Music, Shop, Video — Bilder werden für Lesbarkeit abgedunkelt.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SettingsBadge(text: "\(configuredScreenHeaderCount) angepasst", colorScheme: effectiveColorScheme)
                        SettingsBadge(text: "Overlay aktiv", colorScheme: effectiveColorScheme)
                        SettingsBadge(text: "CRUD bereit", colorScheme: effectiveColorScheme)
                    }

                    Text("Live erst nach „Header speichern“.")
                        .font(.footnote.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    if let message = screenHeaderSettingsStore.lastErrorMessage {
                        Text(message)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(AppColors.accentHighlight(for: effectiveColorScheme))
                    }

                    EditableImageField(
                        title: AppLocalized.text("settings.admin.headers.home_header", fallback: "Home header"),
                        imageURL: $homeHeaderImageURLDraft,
                        colorScheme: effectiveColorScheme,
                        isUploading: activeEditableImageUploadTarget == .homeHeader,
                        uploadStatusText: AppLocalized.text("settings.admin.headers.home_header.uploading", fallback: "Home header is being applied."),
                        onPickImage: { presentSheet(.editableImage(.homeHeader)) },
                        onRemoveImage: { removeEditableImage(for: .homeHeader) }
                    )

                    SettingsInputField(
                        title: AppLocalized.text("settings.admin.headers.home_eyebrow", fallback: "Home eyebrow"),
                        text: $homeHeaderEyebrowDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.home_eyebrow.placeholder", fallback: "e.g. Welcome to SkyOS")
                    )

                    SettingsInputField(
                        title: AppLocalized.text("settings.admin.headers.home_title", fallback: "Home title"),
                        text: $homeHeaderTitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.home_title.placeholder", fallback: "e.g. Your space for music, store, and visuals")
                    )

                    SettingsMultilineInputField(
                        title: AppLocalized.text("settings.admin.headers.home_subtitle", fallback: "Home subtitle"),
                        text: $homeHeaderSubtitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.home_subtitle.placeholder", fallback: "Short, clear positioning for new and returning users."),
                        minHeight: 88
                    )

                    SettingsMultilineInputField(
                        title: AppLocalized.text("settings.admin.headers.home_detail", fallback: "Home detail / welcome text"),
                        text: $homeHeaderDetailDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.home_detail.placeholder", fallback: "Longer intro copy with value, orientation, and next step."),
                        minHeight: 104
                    )

                    EditableImageField(
                        title: AppLocalized.text("settings.admin.headers.music_header", fallback: "Music hub header"),
                        imageURL: $musicHubHeaderImageURLDraft,
                        colorScheme: effectiveColorScheme,
                        isUploading: activeEditableImageUploadTarget == .musicHubHeader,
                        uploadStatusText: AppLocalized.text("settings.admin.headers.music_header.uploading", fallback: "Music hub header is being applied."),
                        onPickImage: { presentSheet(.editableImage(.musicHubHeader)) },
                        onRemoveImage: { removeEditableImage(for: .musicHubHeader) }
                    )

                    SettingsInputField(
                        title: AppLocalized.text("settings.admin.headers.music_eyebrow", fallback: "Music hub eyebrow"),
                        text: $musicHubHeaderEyebrowDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.music_eyebrow.placeholder", fallback: "e.g. Music")
                    )

                    SettingsInputField(
                        title: AppLocalized.text("settings.admin.headers.music_title", fallback: "Music hub title"),
                        text: $musicHubHeaderTitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.music_title.placeholder", fallback: "e.g. Music")
                    )

                    SettingsMultilineInputField(
                        title: AppLocalized.text("settings.admin.headers.music_subtitle", fallback: "Music hub subtitle"),
                        text: $musicHubHeaderSubtitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.music_subtitle.placeholder", fallback: "Releases, artists, and studio in one place."),
                        minHeight: 88
                    )

                    SettingsMultilineInputField(
                        title: AppLocalized.text("settings.admin.headers.music_detail", fallback: "Music hub detail"),
                        text: $musicHubHeaderDetailDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.music_detail.placeholder", fallback: "Clear entry into songs, beats, artists, and studio."),
                        minHeight: 96
                    )

                    EditableImageField(
                        title: AppLocalized.text("settings.admin.headers.shop_header", fallback: "Shop header"),
                        imageURL: $shopHeaderImageURLDraft,
                        colorScheme: effectiveColorScheme,
                        isUploading: activeEditableImageUploadTarget == .shopHeader,
                        uploadStatusText: AppLocalized.text("settings.admin.headers.shop_header.uploading", fallback: "Shop header is being applied."),
                        onPickImage: { presentSheet(.editableImage(.shopHeader)) },
                        onRemoveImage: { removeEditableImage(for: .shopHeader) }
                    )

                    SettingsInputField(
                        title: AppLocalized.text("settings.admin.headers.shop_eyebrow", fallback: "Shop eyebrow"),
                        text: $shopHeaderEyebrowDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.shop_eyebrow.placeholder", fallback: "e.g. Store")
                    )

                    SettingsInputField(
                        title: AppLocalized.text("settings.admin.headers.shop_title", fallback: "Shop title"),
                        text: $shopHeaderTitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.shop_title.placeholder", fallback: "e.g. Shop")
                    )

                    SettingsMultilineInputField(
                        title: AppLocalized.text("settings.admin.headers.shop_subtitle", fallback: "Shop subtitle"),
                        text: $shopHeaderSubtitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.shop_subtitle.placeholder", fallback: "Products directly in the app."),
                        minHeight: 88
                    )

                    SettingsMultilineInputField(
                        title: AppLocalized.text("settings.admin.headers.shop_detail", fallback: "Shop detail"),
                        text: $shopHeaderDetailDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.shop_detail.placeholder", fallback: "Briefly explain what users find in the shop and why it matters."),
                        minHeight: 96
                    )

                    EditableImageField(
                        title: AppLocalized.text("settings.admin.headers.video_header", fallback: "Video header"),
                        imageURL: $videoHeaderImageURLDraft,
                        colorScheme: effectiveColorScheme,
                        isUploading: activeEditableImageUploadTarget == .videoHeader,
                        uploadStatusText: AppLocalized.text("settings.admin.headers.video_header.uploading", fallback: "Video header is being applied."),
                        onPickImage: { presentSheet(.editableImage(.videoHeader)) },
                        onRemoveImage: { removeEditableImage(for: .videoHeader) }
                    )

                    SettingsInputField(
                        title: AppLocalized.text("settings.admin.headers.video_hero_video_url", fallback: "Video hub hero video URL"),
                        text: $videoHeaderHeroVideoURLDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.video_hero_video_url.placeholder", fallback: "Direct video URL (e.g. MP4) — opens when tapping the Video hub top card"),
                        keyboardType: .URL
                    )

                    SettingsInputField(
                        title: AppLocalized.text("settings.admin.headers.video_eyebrow", fallback: "Video eyebrow"),
                        text: $videoHeaderEyebrowDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.video_eyebrow.placeholder", fallback: "e.g. Video")
                    )

                    SettingsInputField(
                        title: AppLocalized.text("settings.admin.headers.video_title", fallback: "Video title"),
                        text: $videoHeaderTitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.video_title.placeholder", fallback: "e.g. Video")
                    )

                    SettingsMultilineInputField(
                        title: AppLocalized.text("settings.admin.headers.video_subtitle", fallback: "Video subtitle"),
                        text: $videoHeaderSubtitleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.video_subtitle.placeholder", fallback: "Clips, visuals, and strong collaborations."),
                        minHeight: 88
                    )

                    SettingsMultilineInputField(
                        title: AppLocalized.text("settings.admin.headers.video_detail", fallback: "Video detail"),
                        text: $videoHeaderDetailDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: AppLocalized.text("settings.admin.headers.video_detail.placeholder", fallback: "Context for clips, visuals, and current collaborations."),
                        minHeight: 96
                    )

                    Text(AppLocalized.text("settings.admin.headers.empty_gradient", fallback: "Empty = system gradient."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    Button(action: saveScreenHeaderSettings) {
                        Label(AppLocalized.text("settings.admin.headers.save", fallback: "Save headers"), systemImage: "photo.on.rectangle.angled")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .skydownInteractiveFeedback()
                    .tint(AppColors.accent(for: effectiveColorScheme))
                }

            case .shopify:
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                    Text("Domain, Token, Collections — dann lädt Shopify.")
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
                        title: "Collection-Handles",
                        text: $shopifyCollectionHandlesDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. spring-drop-2026, hoodies, accessories"
                    )

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        Button {
                            Task {
                                await shopifyAdminSettingsStore.refreshAvailableCollections(force: true)
                                if let message = shopifyAdminSettingsStore.collectionsErrorMessage {
                                    showToastMessage("Shopify-Collections konnten nicht geladen werden: \(message)", style: .error)
                                }
                            }
                        } label: {
                            Label(
                                shopifyAdminSettingsStore.isLoadingCollections ? "Collections laden..." : "Collections abrufen",
                                systemImage: "arrow.clockwise"
                            )
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .skydownInteractiveFeedback()
                        .disabled(shopifyAdminSettingsStore.isLoadingCollections)

                        if shopifyAdminSettingsStore.availableCollections.isEmpty == false {
                            SettingsBadge(
                                text: "\(shopifyAdminSettingsStore.availableCollections.count) gefunden",
                                colorScheme: effectiveColorScheme
                            )
                        }
                    }

                    if shopifyAdminSettingsStore.availableCollections.isEmpty == false {
                        Text("Verfuegbare Collections")
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: effectiveColorScheme))

                        SettingsInputField(
                            title: "Collections suchen",
                            text: $shopifyCollectionSearchDraft,
                            colorScheme: effectiveColorScheme,
                            placeholder: "Nach Titel oder Handle filtern"
                        )

                        if selectedShopifyCollectionHandles.isEmpty == false {
                            SettingsBadge(
                                text: "\(selectedShopifyCollectionHandles.count) ausgewaehlt",
                                colorScheme: effectiveColorScheme
                            )
                        }

                        LazyVGrid(
                            columns: [GridItem(.adaptive(minimum: 190), spacing: SkydownLayout.stackSpacingPill)],
                            alignment: .leading,
                            spacing: SkydownLayout.stackSpacingPill
                        ) {
                            ForEach(filteredShopifyCollectionOptions) { collection in
                                ShopifyCollectionToggleCard(
                                    collection: collection,
                                    isSelected: selectedShopifyCollectionHandles.contains(collection.handle),
                                    colorScheme: effectiveColorScheme
                                ) {
                                    toggleShopifyCollectionHandle(collection.handle)
                                }
                            }
                        }
                    } else if let message = shopifyAdminSettingsStore.collectionsErrorMessage {
                        Text("Collections konnten nicht geladen werden: \(message)")
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                    }

                    Text("Antippen oder Handles. Leer = ganzer Store.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    SettingsBadge(
                        text: shopifyAdminSettingsStore.settings.hasCollectionFilter
                            ? "Aktuell: \(shopifyAdminSettingsStore.settings.activeCollectionLabel)"
                            : "Aktuell: Gesamter Shopify-Store",
                        colorScheme: effectiveColorScheme
                    )

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        Button(action: saveShopifyAdminSettings) {
                            Label("Shopify speichern", systemImage: "shippingbox.and.arrow.backward")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .skydownInteractiveFeedback()
                        .tint(AppColors.accent(for: effectiveColorScheme))

                        if let url = shopifyCatalogURL {
                            Button {
                                openURL(url)
                            } label: {
                                Label("Link oeffnen", systemImage: "arrow.up.right.square")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)
                            .skydownInteractiveFeedback()
                        }
                    }
                }

            case .payments:
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                    Text("PayPal/Bank: manuell. Stripe live. Klarna über Stripe, wenn aktiv.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    PaymentProviderSettingsCard(
                        colorScheme: effectiveColorScheme,
                        title: "Stripe",
                        statusText: paymentMethodSettingsStore.settings.stripe.connected ? "Verbunden" : "Nicht verbunden",
                        checkoutVisible: paymentMethodSettingsStore.settings.stripe.connected && paymentMethodSettingsStore.settings.stripe.enabled,
                        accountHintTitle: "Stripe Konto / Workspace",
                        accountHintPlaceholder: "z. B. SkyOS Merch Workspace",
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

                    AISubscriptionPricingCard(
                        colorScheme: effectiveColorScheme,
                        isEnabled: $aiSubscriptionsEnabledDraft,
                        creatorPriceID: $aiCreatorPriceIDDraft,
                        studioPriceID: $aiStudioPriceIDDraft,
                        iosCreatorProductID: $aiIOSCreatorProductIDDraft,
                        iosStudioProductID: $aiIOSStudioProductIDDraft,
                        iosAppAppleID: $aiIOSAppAppleIDDraft,
                        androidCreatorProductID: $aiAndroidCreatorProductIDDraft,
                        androidStudioProductID: $aiAndroidStudioProductIDDraft
                    ) {
                        saveAISubscriptionPricing()
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
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                    Text("Versand & Steuer für Checkout. Store-Schalter = harte Freigabe.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
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
                        placeholder: "Skydown"
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

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
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
                    .skydownInteractiveFeedback()
                    .tint(AppColors.accentMystic(for: effectiveColorScheme))
                }

            case .visuals:
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
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

                    Text("Lokal. Link + Hinweise für KI. Sync später.")
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

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
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
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                    Text(isOwnerUser
                        ? "Globaler Owner-Flow fuer die ganze App. User triggern nie direkt Activepieces; das Backend prueft Rolle, Abo, Limits und Kontext vor jedem Lauf."
                        : "Optionaler eigener Workflow. Du kannst Activepieces oder n8n anbinden und ihn im Agent bewusst triggern.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    Toggle(isOwnerUser ? "Globalen Activepieces-Flow aktivieren" : "Eigenen Workflow aktivieren", isOn: $automationEnabledDraft)

                    Toggle("App-User-Kontext mitsenden", isOn: $automationSendsUserContextDraft)

                    if !isOwnerUser {
                        Picker("Provider", selection: $automationProviderDraft) {
                            Text("Activepieces").tag("activepieces")
                            Text("n8n").tag("n8n")
                        }
                        .pickerStyle(.segmented)
                    }

                    SettingsInputField(
                        title: "Workflow Name",
                        text: $automationWorkflowNameDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. SkyOS Owner Flow"
                    )

                    SettingsInputField(
                        title: "Activepieces Base URL",
                        text: $automationBaseURLDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "https://cloud.activepieces.com",
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
                        placeholder: "z. B. X-SkyOS-Automation-Key",
                        keyboardType: .asciiCapable
                    )

                    SettingsInputField(
                        title: "Auth Header Value",
                        text: $automationAuthHeaderValueDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "optional",
                        keyboardType: .asciiCapable
                    )

                    SettingsMultilineInputField(
                        title: "Knowledge-Kontext (optional)",
                        text: $automationKnowledgeContextDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. Drive-Ordner, Projektregeln, Brand-Guidelines oder SOPs fuer den globalen Owner-Flow.",
                        minHeight: 100
                    )

                    SettingsStatusCard(
                        style: automationStatusStyle,
                        title: automationStatusTitle,
                        message: automationStatusMessage,
                        details: automationStatusDetails,
                        colorScheme: effectiveColorScheme
                    )

                    if automationHasUnsavedChanges {
                        SettingsBadge(
                            text: "Ungespeicherte Aenderungen",
                            colorScheme: effectiveColorScheme
                        )
                    }

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                        if let saveReason = automationSaveDisabledReasons.first {
                            Text("Speichern: \(saveReason)")
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                        }

                        if let testReason = automationTestDisabledReasons.first {
                            Text("Test: \(testReason)")
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                        }
                    }

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        Button(action: saveAutomationSettings) {
                            Label(
                                isSavingAutomationSettings ? "Speichert..." : "Workflow speichern",
                                systemImage: isSavingAutomationSettings ? "arrow.triangle.2.circlepath.circle.fill" : "bolt.circle.fill"
                            )
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .skydownInteractiveFeedback()
                        .tint(AppColors.accentHighlight(for: effectiveColorScheme))
                        .disabled(!automationSaveDisabledReasons.isEmpty)

                        Button(action: runAutomationTest) {
                            Label(
                                isRunningAutomationTest ? "Test laeuft..." : "Test senden",
                                systemImage: isRunningAutomationTest ? "hourglass.circle.fill" : "paperplane.fill"
                            )
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .skydownInteractiveFeedback()
                        .disabled(!automationTestDisabledReasons.isEmpty)
                    }

                    Divider()

                    Text("Persoenlicher Manus-Account (optional)")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: effectiveColorScheme))

                    Text("Aktiv = Key pro Anfrage — nur Keychain.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    Toggle("Eigenen Manus-Account verwenden", isOn: $manusByosEnabledDraft)

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SettingsBadge(
                            text: manusByosStore.settings.hasAPIKey ? "Key gespeichert" : "Key fehlt",
                            colorScheme: effectiveColorScheme
                        )
                        SettingsBadge(
                            text: manusByosStore.settings.isEnabled && manusByosStore.settings.hasAPIKey ? "BYOS aktiv" : "BYOS aus",
                            colorScheme: effectiveColorScheme
                        )
                    }

                    SettingsSecureInputField(
                        title: "Manus API Key",
                        text: $manusByosAPIKeyDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "sk-..."
                    )

                    Text("Key ersetzen oder löschen — ohne Key: Backend.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    SettingsBadge(
                        text: "Validate: \(manusValidationLabel())",
                        colorScheme: effectiveColorScheme
                    )

                    Text(manusValidationMessage)
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        Button(action: saveManusBYOSSettings) {
                            Label("Manus speichern", systemImage: "lock.shield")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .skydownInteractiveFeedback()
                        .tint(AppColors.accent(for: effectiveColorScheme))

                        Button(action: clearManusBYOSAPIKey) {
                            Label("Key entfernen", systemImage: "trash")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .skydownInteractiveFeedback()
                        .disabled(!manusByosStore.settings.hasAPIKey && manusByosAPIKeyDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }

                    Button(action: validateManusBYOSKey) {
                        Label(
                            isValidatingManusKey ? "Prueft..." : "Validate Manus Key",
                            systemImage: "checkmark.shield"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .skydownInteractiveFeedback()
                    .disabled(isValidatingManusKey || resolveEffectiveManusKey().isEmpty)
                }

            case .aiPrompts:
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                    Text("Bot / Visual / Agent — `adminConfig/aiPromptSettings`. Optional: Asset-Link global.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            SettingsBadge(
                                text: "Text \(aiTextInstructionDraft.trimmingCharacters(in: .whitespacesAndNewlines).count)",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: "Visual \(aiVisualInstructionDraft.trimmingCharacters(in: .whitespacesAndNewlines).count)",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: "Agent \(aiAgentSystemInstructionDraft.trimmingCharacters(in: .whitespacesAndNewlines).count)",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: "FAQ \(aiFAQInstructionDraft.trimmingCharacters(in: .whitespacesAndNewlines).count)",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: aiAssetLibraryLinkDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Assets aus" : "Assets aktiv",
                                colorScheme: effectiveColorScheme
                            )
                        }
                    }

                    SettingsMultilineInputField(
                        title: "Bot Text-Anweisung",
                        text: $aiTextInstructionDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Globale Anweisung fuer Text-Antworten.",
                        minHeight: 120
                    )

                    SettingsMultilineInputField(
                        title: "Visual-Anweisung",
                        text: $aiVisualInstructionDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Globale Anweisung fuer Bild-Generierung.",
                        minHeight: 120
                    )

                    SettingsMultilineInputField(
                        title: "Agent System-Anweisung",
                        text: $aiAgentSystemInstructionDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Globale Systemrolle fuer den Agent.",
                        minHeight: 140
                    )

                    SettingsMultilineInputField(
                        title: "FAQ System-Anweisung",
                        text: $aiFAQInstructionDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Globale Regel fuer ehrliche, klare FAQ- und Help-Antworten.",
                        minHeight: 120
                    )

                    SettingsMultilineInputField(
                        title: "FAQ / Owner Knowledge",
                        text: $aiFAQKnowledgeBaseDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Owner-definierte Fragen, Produktwissen, Help-Center-Fakten, Membership-Hinweise, Shipping-/Support-Regeln.",
                        minHeight: 160
                    )

                    SettingsInputField(
                        title: "Asset- / Referenzbibliothek",
                        text: $aiAssetLibraryLinkDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. https://mega.nz/folder/...",
                        keyboardType: .URL
                    )

                    SettingsMultilineInputField(
                        title: "Asset-Hinweise fuer Bot, Visuals und Agent",
                        text: $aiAssetReferenceNotesDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Welche Assets, Moodboards oder Ordnerlinks sollen global beruecksichtigt werden?",
                        minHeight: 110
                    )

                    Text("Leere Felder fallen automatisch auf den Standard zurueck.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    Button(action: saveAIPromptSettings) {
                        Label("KI-Anweisungen speichern", systemImage: "sparkles")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .skydownInteractiveFeedback()
                    .tint(AppColors.accentHighlight(for: effectiveColorScheme))

                    Divider()
                        .padding(.vertical, 4)

                    Text("Runtime & Provider (`adminConfig/aiRuntime`)")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: effectiveColorScheme))

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                        HStack {
                            Text("FAQ Review Loop (30d)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.text(for: effectiveColorScheme))
                            Spacer()
                            if aiFaqOwnerReviewLoopStore.isLoading {
                                ProgressView()
                                    .controlSize(.small)
                            } else {
                                Button("Aktualisieren") {
                                    Task { await aiFaqOwnerReviewLoopStore.refresh(windowDays: aiFaqReviewLoopWindowDays) }
                                }
                                .font(.caption.weight(.semibold))
                            }
                        }
                        .padding(.top, 2)
                        if let actionMessage = aiFaqOwnerReviewLoopStore.lastActionMessage, !actionMessage.isEmpty {
                            Text(actionMessage)
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                        }
                        if let metricsSnapshot = aiFaqOwnerReviewLoopStore.lastMetricsSnapshot, !metricsSnapshot.isEmpty {
                            Text(metricsSnapshot)
                                .font(.caption2)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                        }

                        if let errorMessage = aiFaqOwnerReviewLoopStore.lastErrorMessage, !errorMessage.isEmpty {
                            Text("Review Loop konnte nicht geladen werden: \(errorMessage)")
                                .font(.footnote)
                                .foregroundColor(.red)
                        } else {
                            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                                SettingsBadge(text: "Strong \(aiFaqOwnerReviewLoopStore.strongestTriggers.count)", colorScheme: effectiveColorScheme)
                                SettingsBadge(text: "Weak \(aiFaqOwnerReviewLoopStore.weakTriggers.count)", colorScheme: effectiveColorScheme)
                                SettingsBadge(text: "Useless \(aiFaqOwnerReviewLoopStore.likelyUselessTriggers.count)", colorScheme: effectiveColorScheme)
                                SettingsBadge(text: "Repeat \(aiFaqOwnerReviewLoopStore.repeatHeavyTopics.count)", colorScheme: effectiveColorScheme)
                            }
                            .padding(.bottom, 2)

                            if let topStrong = aiFaqOwnerReviewLoopStore.strongestTriggers.first {
                                Text("Strongest Trigger: \(topStrong.triggerKey) · Conv \(Int(topStrong.conversionRate * 100))% · Repeat \(Int(topStrong.repeatRate * 100))%")
                                    .font(.footnote)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                            }
                            if let topWeak = aiFaqOwnerReviewLoopStore.weakTriggers.first {
                                Text("Weak Trigger: \(topWeak.triggerKey) · Conv \(Int(topWeak.conversionRate * 100))% · Repeat \(Int(topWeak.repeatRate * 100))%")
                                    .font(.footnote)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                            }
                            if let topRepeat = aiFaqOwnerReviewLoopStore.repeatHeavyTopics.first {
                                Text("Repeat-Heavy Topic: \(topRepeat.key) (\(topRepeat.value)x, \(Int(topRepeat.share * 100))%)")
                                    .font(.footnote)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                            }

                            ForEach(aiFaqOwnerReviewLoopStore.strategyInsights.prefix(3)) { insight in
                                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                                    Text(insight.title)
                                        .font(.footnote.weight(.semibold))
                                        .foregroundColor(AppColors.text(for: effectiveColorScheme))
                                    Text(insight.summary)
                                        .font(.caption)
                                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                                    Text("Erwartete Wirkung: \(insight.expectedImpact)")
                                        .font(.caption2)
                                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                                }
                                .padding(10)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(
                                    RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                                        .fill(AppColors.cardBackground(for: effectiveColorScheme))
                                )
                            }

                            ForEach(aiFaqOwnerReviewLoopStore.recommendations.prefix(3)) { recommendation in
                                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                                    Text(recommendation.title)
                                        .font(.footnote.weight(.semibold))
                                        .foregroundColor(AppColors.text(for: effectiveColorScheme))
                                    Text(recommendation.summary)
                                        .font(.caption)
                                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                                    Text("Action: \(recommendation.actionType) · Target: \(recommendation.targetField) · Suggest: \(recommendation.suggestedValueLabel)")
                                        .font(.caption2)
                                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                                    HStack(spacing: SkydownLayout.stackSpacingMicro) {
                                        Button("Preview") {
                                            Task {
                                                let message = await aiFaqOwnerReviewLoopStore.preview(
                                                    recommendation: recommendation,
                                                    windowDays: aiFaqReviewLoopWindowDays
                                                )
                                                showToastMessage(message, style: .info)
                                            }
                                        }
                                        .font(.caption.weight(.semibold))
                                        .buttonStyle(.bordered)

                                        Button("Apply") {
                                            Task {
                                                let message = await aiFaqOwnerReviewLoopStore.apply(
                                                    recommendation: recommendation,
                                                    windowDays: aiFaqReviewLoopWindowDays
                                                )
                                                showToastMessage(message, style: message.contains("blockiert") ? .warning : .success)
                                            }
                                        }
                                        .font(.caption.weight(.semibold))
                                        .buttonStyle(.borderedProminent)
                                    }
                                }
                                .padding(10)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(
                                    RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                                        .fill(AppColors.cardBackground(for: effectiveColorScheme))
                                )
                            }
                            Button("Revert Last Change") {
                                Task {
                                    let message = await aiFaqOwnerReviewLoopStore.revertLastChange(windowDays: aiFaqReviewLoopWindowDays)
                                    showToastMessage(message, style: message.contains("revert") ? .success : .info)
                                }
                            }
                            .font(.caption.weight(.semibold))
                            .buttonStyle(.bordered)
                        }
                    }

                    Text("Bot-Core + Agent-Provider — gleiche Governance fuer iOS und Android.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            SettingsBadge(
                                text: aiBotPromptVersionDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Prompt v?" : aiBotPromptVersionDraft,
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: "FAQ \(aiBotFAQModeDraft)",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: "Qualitaet \(aiBotQualityModeDraft)",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: "Provider \(aiAgentProviderDraft.displayTitle)",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: aiCostGuardEnabledDraft ? "Kosten-Guard an" : "Kosten-Guard aus",
                                colorScheme: effectiveColorScheme
                            )
                            SettingsBadge(
                                text: aiManusEnabledDraft ? "Manus aktiv" : "Manus aus",
                                colorScheme: effectiveColorScheme
                            )
                        }
                    }

                    Text("Bot Core (`adminConfig/aiRuntime.bot`)")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: effectiveColorScheme))

                    SettingsInputField(
                        title: "Prompt Version",
                        text: $aiBotPromptVersionDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "bot-max-v1"
                    )

                    SettingsInputField(
                        title: "AI Personality Stil",
                        text: $aiBotPersonalityStyleDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "z. B. calm_precise"
                    )

                    SettingsInputField(
                        title: "Logging Level",
                        text: $aiBotLoggingLevelDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "standard / verbose"
                    )

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        SettingsFieldTitle(title: "Quality Mode", colorScheme: effectiveColorScheme)
                        Picker("Quality Mode", selection: $aiBotQualityModeDraft) {
                            Text("Balanced").tag("balanced")
                            Text("High").tag("high")
                        }
                        .pickerStyle(.segmented)
                    }

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        SettingsFieldTitle(title: "FAQ Mode", colorScheme: effectiveColorScheme)
                        Picker("FAQ Mode", selection: $aiBotFAQModeDraft) {
                            Text("Off").tag("off")
                            Text("Auto").tag("auto")
                            Text("Prefer").tag("prefer_faq")
                        }
                        .pickerStyle(.segmented)
                    }

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        SettingsFieldTitle(title: "Owner Mode", colorScheme: effectiveColorScheme)
                        Picker("Owner Mode", selection: $aiBotOwnerModeDraft) {
                            Text("Standard").tag("standard")
                            Text("Diagnostic").tag("diagnostic")
                        }
                        .pickerStyle(.segmented)
                    }

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        SettingsFieldTitle(title: "Antwortlaenge", colorScheme: effectiveColorScheme)
                        Picker("Antwortlaenge", selection: $aiBotAnswerLengthDraft) {
                            Text("Adaptive").tag("adaptive")
                            Text("Kurz").tag("short")
                            Text("Tief").tag("detailed")
                        }
                        .pickerStyle(.segmented)
                    }

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        SettingsFieldTitle(title: "Diagnostics", colorScheme: effectiveColorScheme)
                        Picker("Diagnostics", selection: $aiBotDiagnosticsModeDraft) {
                            Text("Off").tag("off")
                            Text("Owner").tag("owner_only")
                            Text("Verbose").tag("verbose")
                        }
                        .pickerStyle(.segmented)
                    }

                    Toggle("Kill Switch aktiv", isOn: $aiBotKillSwitchDraft)
                    Toggle("Bot Cost Guard aktiv", isOn: $aiBotCostGuardEnabledDraft)
                    Toggle("Kurze Antworten bei Critical Guard", isOn: $aiBotPreferBriefCriticalDraft)
                    Toggle("FAQ priorisieren bei Topic-Match", isOn: $aiBotPreferFaqRoutingDraft)
                    Toggle("Produkt-Guide fuer neue Nutzer bevorzugen", isOn: $aiBotPreferProductGuideDraft)
                    Toggle("Visual-Generierung erlauben", isOn: $aiBotAllowVisualGenerationDraft)
                    Toggle("Text-Fallback erlauben", isOn: $aiBotAllowTextFallbackDraft)
                    Toggle("Visual-Fallback erlauben", isOn: $aiBotAllowVisualFallbackDraft)
                    Toggle("Fallback-Grund anzeigen", isOn: $aiBotExposeFallbackReasonDraft)
                    Toggle("Safe Mode aktiv", isOn: $aiBotSafeModeEnabledDraft)
                    Toggle("Strenges Unknown-Handling", isOn: $aiBotStrictUnknownHandlingDraft)
                    Toggle("Spekulative FAQ blocken", isOn: $aiBotBlockSpeculativeFAQDraft)
                    Toggle("Proaktive Hinweise aktiv", isOn: $aiBotProactiveHintsEnabledDraft)
                    Toggle("Trigger: AI-Limit fast erreicht", isOn: $aiBotTriggerAiLimitNearEnabledDraft)
                    Toggle("Trigger: Restore verfuegbar", isOn: $aiBotTriggerRestoreAvailableEnabledDraft)
                    Toggle("Trigger: Bestellung versendet", isOn: $aiBotTriggerOrderShippedEnabledDraft)
                    Toggle("Trigger: Payment-Methode geaendert", isOn: $aiBotTriggerPaymentMethodsChangedEnabledDraft)
                    Toggle("Trigger: Upgrade nach Nutzung", isOn: $aiBotTriggerUsageBasedUpgradeEnabledDraft)

                    SettingsInputField(
                        title: "Proaktiv Warning Threshold (%)",
                        text: $aiBotWarningThresholdPercentDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "70",
                        keyboardType: .numberPad
                    )

                    SettingsInputField(
                        title: "Proaktiv Critical Threshold (%)",
                        text: $aiBotCriticalThresholdPercentDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "90",
                        keyboardType: .numberPad
                    )

                    SettingsInputField(
                        title: "Upgrade Hint Free -> Pro",
                        text: $aiBotUpgradeHintFreeToProTextDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Hint bei hoher Free-Nutzung"
                    )

                    SettingsInputField(
                        title: "Upgrade Hint Pro -> Creator",
                        text: $aiBotUpgradeHintProToCreatorTextDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "Hint bei hoher Pro-Nutzung"
                    )

                    SettingsInputField(
                        title: "Prompt Version Alias",
                        text: $aiBotPromptVersionAliasDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "bot-max-v1"
                    )

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        SettingsFieldTitle(title: "FAQ Prioritaet", colorScheme: effectiveColorScheme)
                        Picker("FAQ Prioritaet", selection: $aiBotFaqPriorityModeDraft) {
                            Text("Live -> Owner -> Generic").tag("live_owner_generic")
                            Text("Owner -> Live -> Generic").tag("owner_live_generic")
                            Text("Balanced").tag("balanced")
                        }
                        .pickerStyle(.segmented)
                    }

                    SettingsInputField(
                        title: "Text Primary Model",
                        text: $aiBotTextPrimaryModelDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "gemini-2.5-flash-lite"
                    )

                    SettingsInputField(
                        title: "Text Fallback Model",
                        text: $aiBotTextFallbackModelDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "gemini-2.5-flash-lite"
                    )

                    SettingsInputField(
                        title: "Visual Primary Model",
                        text: $aiBotVisualPrimaryModelDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "gemini-2.5-flash-image"
                    )

                    SettingsInputField(
                        title: "Visual Fallback Model",
                        text: $aiBotVisualFallbackModelDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "imagen-3.0-generate-002"
                    )

                    SettingsInputField(
                        title: "Short Answer Max Tokens",
                        text: $aiBotShortAnswerMaxTokensDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "240",
                        keyboardType: .numberPad
                    )

                    SettingsInputField(
                        title: "Standard Answer Max Tokens",
                        text: $aiBotStandardAnswerMaxTokensDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "768",
                        keyboardType: .numberPad
                    )

                    Divider()
                        .padding(.vertical, 4)

                    Toggle("Kosten-Guard aktiv", isOn: $aiCostGuardEnabledDraft)

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        SettingsFieldTitle(title: "Agent Provider", colorScheme: effectiveColorScheme)
                        Picker("Agent Provider", selection: $aiAgentProviderDraft) {
                            ForEach(AIRuntimeAgentProvider.allCases, id: \.self) { provider in
                                Text(provider.displayTitle).tag(provider)
                            }
                        }
                        .pickerStyle(.segmented)
                    }

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        SettingsFieldTitle(title: "Fallback Provider", colorScheme: effectiveColorScheme)
                        Picker("Fallback Provider", selection: $aiFallbackAgentProviderDraft) {
                            ForEach(AIRuntimeAgentProvider.allCases, id: \.self) { provider in
                                Text(provider.displayTitle).tag(provider)
                            }
                        }
                        .pickerStyle(.segmented)
                    }

                    Toggle("Manus freigeben", isOn: $aiManusEnabledDraft)

                    Text("Manus Runtime (`adminConfig/aiRuntime.manus`)")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: effectiveColorScheme))

                    Text("Secret nur in Functions — nicht in der App.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                    SettingsInputField(
                        title: "Request Timeout (ms)",
                        text: $aiManusRequestTimeoutMsDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "12000",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Poll Interval (ms)",
                        text: $aiManusPollIntervalMsDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "1500",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Max Poll Attempts",
                        text: $aiManusMaxPollAttemptsDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "18",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "List Messages Limit",
                        text: $aiManusListMessagesLimitDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "30",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Max Prompt Chars",
                        text: $aiManusMaxPromptCharsDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "2400",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Max History Turns",
                        text: $aiManusMaxHistoryTurnsDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "12",
                        keyboardType: .numberPad
                    )

                    Toggle("Auto Stop bei Waiting-Event", isOn: $aiManusAutoStopOnWaitingDraft)
                    Toggle("High-Credit Events blocken", isOn: $aiManusBlockHighCreditEventsDraft)
                    Toggle("Verbose Events einblenden", isOn: $aiManusIncludeVerboseEventsDraft)

                    Divider()
                        .padding(.vertical, 4)

                    SettingsInputField(
                        title: "Hard Cap Text / Tag",
                        text: $aiHardTextLimitDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "120",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Hard Cap Visual / Tag",
                        text: $aiHardVisualLimitDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "20",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Hard Cap Agent / Tag",
                        text: $aiHardAgentLimitDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "40",
                        keyboardType: .numberPad
                    )

                    SettingsInputField(
                        title: "Global Cap Text / Tag",
                        text: $aiGlobalTextLimitDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "1500",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Global Cap Visual / Tag",
                        text: $aiGlobalVisualLimitDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "180",
                        keyboardType: .numberPad
                    )
                    SettingsInputField(
                        title: "Global Cap Agent / Tag",
                        text: $aiGlobalAgentLimitDraft,
                        colorScheme: effectiveColorScheme,
                        placeholder: "350",
                        keyboardType: .numberPad
                    )

                    Button(action: saveAIRuntimeSettings) {
                        Label("KI Runtime speichern", systemImage: "switch.2")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .skydownInteractiveFeedback()
                }

            case .membershipOps:
                SettingsMembershipCommandCenterView(
                    store: membershipOpsStore,
                    colorScheme: effectiveColorScheme
                ) { message, style in
                    showToastMessage(message, style: style)
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
            if workflowAutomationSettings.settings.isPrepared &&
                manusByosStore.settings.isEnabled &&
                manusByosStore.settings.hasAPIKey {
                return "Workflow + Manus bereit"
            }
            if workflowAutomationSettings.settings.isPrepared {
                return "Workflow bereit"
            }
            if manusByosStore.settings.hasAPIKey {
                return "Manus bereit"
            }
            return "Noch offen"
        case .aiPrompts:
            return aiPromptSettingsStore.settings.assetLibraryLink.isEmpty ?
                "Text \(aiPromptSettingsStore.settings.textInstruction.count)" :
                "Assets + Prompts"
        case .membershipOps:
            return membershipOpsStore.isLoading ? "Command Center laedt" : "Revenue Ops bereit"
        }
    }

    private var supportMailbox: String {
        legalContentStore.settings.resolvedSupportEmail
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
        Hallo SkyOS-Team,

        ich habe folgende Anfrage:

        Eingeloggter Account: \(username)
        Account-E-Mail: \(email)

        Nachricht:
        """
    }

    private func openMailAppFallback() {
        let encodedSubject = supportMailSubject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let encodedBody = supportMailBody.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""

        guard let url = URL(string: "mailto:\(supportMailbox)?subject=\(encodedSubject)&body=\(encodedBody)") else {
            showToastMessage("Mail-App konnte nicht geoeffnet werden", style: .error)
            return
        }

        openURL(url) { accepted in
            showToastMessage(
                accepted ? "Mail-App geoeffnet" : "Mail-App konnte nicht geoeffnet werden",
                style: accepted ? .success : .error
            )
        }
    }

    private func showToastMessage(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private func runControlCenterHealthCheck() async {
        guard !isRunningControlCenterCheck else { return }
        isRunningControlCenterCheck = true
        defer { isRunningControlCenterCheck = false }

        await notificationPermissionStore.refresh()
        _ = await authManager.refreshCurrentUser()

        if canUseAISelfPaySubscription {
            await aiSubscriptionStore.prepareStorefront(for: authManager.userSession)
        }

        if isOwnerUser {
            await shopifyAdminSettingsStore.refreshAvailableCollections(force: true)
        }

        let accountState = authManager.userSession == nil ? "Gast" : "aktiv"
        let notificationState = notificationPermissionStore.notificationsEnabled ? "Push an" : "Push aus"
        showToastMessage(
            "System geprueft: Konto \(accountState), \(notificationState).",
            style: .success
        )
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
        aiSubscriptionsEnabledDraft = settings.aiSubscriptions.enabled
        aiCreatorPriceIDDraft = settings.aiSubscriptions.creatorPriceID
        aiStudioPriceIDDraft = settings.aiSubscriptions.studioPriceID
        aiIOSCreatorProductIDDraft = settings.aiSubscriptions.iosCreatorProductID
        aiIOSStudioProductIDDraft = settings.aiSubscriptions.iosStudioProductID
        aiIOSAppAppleIDDraft = settings.aiSubscriptions.iosAppAppleID
        aiAndroidCreatorProductIDDraft = settings.aiSubscriptions.androidCreatorProductID
        aiAndroidStudioProductIDDraft = settings.aiSubscriptions.androidStudioProductID
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
        shopifyCollectionHandlesDraft = settings.collectionHandlesDraft
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
        videoHeaderHeroVideoURLDraft = settings.videoHubHeroVideoURL
        videoHeaderEyebrowDraft = settings.videoHubEyebrow
        videoHeaderTitleDraft = settings.videoHubTitle
        videoHeaderSubtitleDraft = settings.videoHubSubtitle
        videoHeaderDetailDraft = settings.videoHubDetail
    }

    private func syncAutomationDrafts(with settings: WorkflowAutomationSettings) {
        automationProviderDraft = settings.provider == "n8n" ? "n8n" : "activepieces"
        automationEnabledDraft = settings.isEnabled
        automationSendsUserContextDraft = settings.sendsUserContext
        automationWorkflowNameDraft = settings.workflowName
        automationBaseURLDraft = settings.baseURL
        automationWebhookPathDraft = settings.webhookPath
        automationAuthHeaderNameDraft = settings.authHeaderName
        automationAuthHeaderValueDraft = settings.authHeaderValue
        automationKnowledgeContextDraft = settings.knowledgeContext
    }

    private func normalizedAutomationSettings(_ settings: WorkflowAutomationSettings) -> WorkflowAutomationSettings {
        let workflowName = settings.workflowName.trimmingCharacters(in: .whitespacesAndNewlines)
        let scope = isOwnerUser ? "owner_global" : "user_personal"
        let provider = scope == "user_personal" && settings.provider == "n8n" ? "n8n" : "activepieces"

        return WorkflowAutomationSettings(
            provider: provider,
            scope: scope,
            isEnabled: settings.isEnabled,
            sendsUserContext: settings.sendsUserContext,
            workflowName: workflowName.isEmpty ? WorkflowAutomationSettings.default.workflowName : workflowName,
            baseURL: normalizeAutomationBaseURLDraft(settings.baseURL) ?? "",
            webhookPath: normalizeAutomationWebhookPathDraft(settings.webhookPath) ?? "",
            authHeaderName: settings.authHeaderName.trimmingCharacters(in: .whitespacesAndNewlines),
            authHeaderValue: settings.authHeaderValue.trimmingCharacters(in: .whitespacesAndNewlines),
            knowledgeContext: settings.knowledgeContext.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }

    private func normalizeAutomationBaseURLDraft(_ rawValue: String) -> String? {
        let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        if let url = URL(string: trimmed), let scheme = url.scheme, !scheme.isEmpty {
            return url.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        }

        if let url = URL(string: "https://\(trimmed)") {
            return url.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        }

        return nil
    }

    private func normalizeAutomationWebhookPathDraft(_ rawValue: String) -> String? {
        let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        if let url = URL(string: trimmed), let scheme = url.scheme, !scheme.isEmpty {
            let withoutScheme = url.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            return withoutScheme.isEmpty ? nil : withoutScheme
        }

        return trimmed.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    private func updateAutomationInlineFeedback(_ message: String, style: ToastStyle) {
        automationInlineFeedbackMessage = message
        automationInlineFeedbackStyle = style
        automationInlineFeedbackTimestamp = Date()
    }

    private func syncManusBYOSDrafts(with settings: ManusBYOSSettings) {
        manusByosEnabledDraft = settings.isEnabled
        if settings.isEnabled && settings.hasAPIKey && manusValidationStatus == "unvalidated" {
            manusValidationStatus = "fallback_internal"
            manusValidationMessage = "Noch nicht validiert. Agent nutzt aktuell BYOS oder faellt intern zurueck."
        } else if !settings.hasAPIKey {
            manusValidationStatus = "awaiting_external_auth"
            manusValidationMessage = "Kein Key gespeichert. Externer Lauf wartet auf Auth."
        } else if !settings.isEnabled {
            manusValidationStatus = "fallback_internal"
            manusValidationMessage = "BYOS pausiert. Agent nutzt internen Fallback."
        }
    }

    private func syncAIPromptDrafts(with settings: AIPromptSettings) {
        aiTextInstructionDraft = settings.textInstruction
        aiVisualInstructionDraft = settings.visualInstruction
        aiAgentSystemInstructionDraft = settings.agentSystemInstruction
        aiFAQInstructionDraft = settings.faqInstruction
        aiFAQKnowledgeBaseDraft = settings.faqKnowledgeBase
        aiAssetLibraryLinkDraft = settings.assetLibraryLink
        aiAssetReferenceNotesDraft = settings.assetReferenceNotes
    }

    private func syncAIRuntimeDrafts(with settings: AIRuntimeSettings) {
        aiCostGuardEnabledDraft = settings.costGuardEnabled
        aiAgentProviderDraft = settings.agentProvider
        aiFallbackAgentProviderDraft = settings.fallbackAgentProvider
        aiManusEnabledDraft = settings.manus.isEnabled
        aiManusRequestTimeoutMsDraft = String(settings.manus.requestTimeoutMs)
        aiManusPollIntervalMsDraft = String(settings.manus.pollIntervalMs)
        aiManusMaxPollAttemptsDraft = String(settings.manus.maxPollAttempts)
        aiManusListMessagesLimitDraft = String(settings.manus.listMessagesLimit)
        aiManusMaxPromptCharsDraft = String(settings.manus.maxPromptChars)
        aiManusMaxHistoryTurnsDraft = String(settings.manus.maxHistoryTurns)
        aiManusAutoStopOnWaitingDraft = settings.manus.autoStopOnWaiting
        aiManusBlockHighCreditEventsDraft = settings.manus.blockHighCreditEvents
        aiManusIncludeVerboseEventsDraft = settings.manus.includeVerboseEvents
        aiHardTextLimitDraft = String(settings.hardDailyCaps.text)
        aiHardVisualLimitDraft = String(settings.hardDailyCaps.visual)
        aiHardAgentLimitDraft = String(settings.hardDailyCaps.agent)
        aiGlobalTextLimitDraft = String(settings.globalDailyCaps.text)
        aiGlobalVisualLimitDraft = String(settings.globalDailyCaps.visual)
        aiGlobalAgentLimitDraft = String(settings.globalDailyCaps.agent)
        aiBotPromptVersionDraft = settings.bot.promptVersion
        aiBotQualityModeDraft = settings.bot.qualityMode
        aiBotFAQModeDraft = settings.bot.faqMode
        aiBotOwnerModeDraft = settings.bot.ownerMode
        aiBotAnswerLengthDraft = settings.bot.answerLength
        aiBotPersonalityStyleDraft = settings.bot.personalityStyle
        aiBotLoggingLevelDraft = settings.bot.loggingLevel
        aiBotDiagnosticsModeDraft = settings.bot.diagnosticsMode
        aiBotKillSwitchDraft = settings.bot.killSwitchEnabled
        aiBotTextPrimaryModelDraft = settings.bot.modelPolicy.textPrimaryModel
        aiBotTextFallbackModelDraft = settings.bot.modelPolicy.textFallbackModel
        aiBotVisualPrimaryModelDraft = settings.bot.modelPolicy.visualPrimaryModel
        aiBotVisualFallbackModelDraft = settings.bot.modelPolicy.visualFallbackModel
        aiBotCostGuardEnabledDraft = settings.bot.costGuard.enabled
        aiBotPreferBriefCriticalDraft = settings.bot.costGuard.preferBriefAnswersWhenCritical
        aiBotShortAnswerMaxTokensDraft = String(settings.bot.costGuard.shortAnswerMaxOutputTokens)
        aiBotStandardAnswerMaxTokensDraft = String(settings.bot.costGuard.standardAnswerMaxOutputTokens)
        aiBotPreferFaqRoutingDraft = settings.bot.routingPolicy.preferFaqWhenTopicMatched
        aiBotPreferProductGuideDraft = settings.bot.routingPolicy.preferProductGuideForNewUsers
        aiBotAllowVisualGenerationDraft = settings.bot.routingPolicy.allowVisualGeneration
        aiBotAllowTextFallbackDraft = settings.bot.fallbackPolicy.allowTextFallback
        aiBotAllowVisualFallbackDraft = settings.bot.fallbackPolicy.allowVisualFallback
        aiBotExposeFallbackReasonDraft = settings.bot.fallbackPolicy.exposeFallbackReason
        aiBotSafeModeEnabledDraft = settings.bot.safetyPolicy.safeModeEnabled
        aiBotStrictUnknownHandlingDraft = settings.bot.safetyPolicy.strictUnknownHandling
        aiBotBlockSpeculativeFAQDraft = settings.bot.safetyPolicy.blockSpeculativeFaqAnswers
        aiBotProactiveHintsEnabledDraft = settings.bot.actionLayer.proactiveHintsEnabled
        aiBotTriggerAiLimitNearEnabledDraft = settings.bot.actionLayer.triggerAiLimitNearEnabled
        aiBotTriggerRestoreAvailableEnabledDraft = settings.bot.actionLayer.triggerRestoreAvailableEnabled
        aiBotTriggerOrderShippedEnabledDraft = settings.bot.actionLayer.triggerOrderShippedEnabled
        aiBotTriggerPaymentMethodsChangedEnabledDraft = settings.bot.actionLayer.triggerPaymentMethodsChangedEnabled
        aiBotTriggerUsageBasedUpgradeEnabledDraft = settings.bot.actionLayer.triggerUsageBasedUpgradeEnabled
        aiBotWarningThresholdPercentDraft = String(settings.bot.actionLayer.warningThresholdPercent)
        aiBotCriticalThresholdPercentDraft = String(settings.bot.actionLayer.criticalThresholdPercent)
        aiBotUpgradeHintFreeToProTextDraft = settings.bot.actionLayer.upgradeHintFreeToProText
        aiBotUpgradeHintProToCreatorTextDraft = settings.bot.actionLayer.upgradeHintProToCreatorText
        aiBotFaqPriorityModeDraft = settings.bot.actionLayer.faqPriorityMode
        aiBotPromptVersionAliasDraft = settings.bot.actionLayer.promptVersionAlias
    }

    private func syncLegalContentDrafts(with settings: LegalContentSettings) {
        legalBrandNameDraft = settings.resolvedBrandName
        legalOperatorNameDraft = settings.resolvedOperatorName
        legalRightsHolderNameDraft = settings.resolvedRightsHolderName
        legalSupportEmailDraft = settings.resolvedSupportEmail
        legalLastUpdatedLabelDraft = settings.resolvedLastUpdatedLabel
        legalImprintReferenceDraft = settings.resolvedImprintReference
        legalMasterNumberMeaningDraft = settings.resolvedMasterNumberMeaning
        legalBrandManifestoDraft = settings.resolvedBrandManifesto
        legalSymbolicNumericCodeDraft = settings.resolvedSymbolicNumericCode
        legalSymbolicLeetCodeDraft = settings.resolvedSymbolicLeetCode
        legalSymbolicCodeExplanationDraft = settings.resolvedSymbolicCodeExplanation
    }

    private func refreshOwnerWorkspaceObservation(
        for section: SettingsAdminWorkspaceSection?,
        userID: String? = nil
    ) {
        let shouldObserveUsers = isOwnerUser && (section == .users || section == .artists)
        let shouldObserveStripeSecrets = isOwnerUser && section == .payments
        let shouldObserveAutomation = (userID ?? authManager.userSession?.id) != nil
        let shouldObserveAIPrompts = isOwnerUser && section == .aiPrompts

        adminUserManagementStore.configureObservation(isAdmin: shouldObserveUsers)
        stripeBackendSecretsStore.setObservationEnabled(shouldObserveStripeSecrets)
        workflowAutomationSettings.configureObservation(
            isEnabled: shouldObserveAutomation,
            userID: shouldObserveAutomation ? (userID ?? authManager.userSession?.id) : nil,
            scope: isOwnerUser ? "owner_global" : "user_personal"
        )
        aiPromptSettingsStore.setObservationEnabled(shouldObserveAIPrompts)
        aiRuntimeSettingsStore.setObservationEnabled(shouldObserveAIPrompts)
        if shouldObserveAIPrompts {
            Task { await aiFaqOwnerReviewLoopStore.refresh(windowDays: aiFaqReviewLoopWindowDays) }
        } else {
            aiFaqOwnerReviewLoopStore.clear()
        }
    }

    private func presentSheet(_ sheet: SettingsPresentedSheet) {
        sheetPresentation.request(sheet)
    }

    private func presentInitialAdminWorkspaceIfNeeded() {
        guard !didPresentInitialAdminWorkspace,
              let initialAdminWorkspaceRawValue,
              let section = SettingsAdminWorkspaceSection(rawValue: initialAdminWorkspaceRawValue) else {
            return
        }

        didPresentInitialAdminWorkspace = true
        Task { @MainActor in
            presentSheet(.adminWorkspace(section))
        }
    }

    @ViewBuilder
    private func policyDocumentView(title: String, text: String) -> some View {
        PolicyView(
            title: title,
            text: text,
            lastUpdated: legalContentStore.settings.resolvedLastUpdatedLabel,
            supportEmail: legalContentStore.settings.resolvedSupportEmail
        )
    }

    @ViewBuilder
    private func settingsSheetContent(for sheet: SettingsPresentedSheet) -> some View {
        switch sheet {
        case .login(let context):
            LoginView(entryContext: context)
        case .registration:
            RegistrationSheet()
        case .orders:
            OrderView()
        case .profileEditor:
            ProfileView(
                authManager: authManager,
                startsInEditMode: true
            )
        case .appGuide:
            policyDocumentView(
                title: AppLocalized.text("settings.legal.guide_title", fallback: "README / App Guide"),
                text: legalContentStore.settings.appGuideText
            )
        case .termsAndConditions:
            policyDocumentView(
                title: AppLocalized.text("policy.title.agb", fallback: "Terms & Conditions (AGB)"),
                text: legalContentStore.settings.termsAndConditionsText
            )
        case .privacyPolicy:
            policyDocumentView(
                title: AppLocalized.text("policy.title.privacy", fallback: "Privacy policy"),
                text: legalContentStore.settings.privacyPolicyText
            )
        case .termsOfService:
            policyDocumentView(
                title: AppLocalized.text("policy.title.termsOfService", fallback: "Terms of use"),
                text: legalContentStore.settings.termsOfServiceText
            )
        case .subscriptionTerms:
            policyDocumentView(
                title: AppLocalized.text("settings.legal.subscription_terms", fallback: "Subscription terms"),
                text: legalContentStore.settings.subscriptionTermsText
            )
        case .aiUsageNotice:
            policyDocumentView(
                title: AppLocalized.text("settings.legal.ai_usage", fallback: "AI usage notice"),
                text: legalContentStore.settings.aiUsageNoticeText
            )
        case .imprintInfo:
            policyDocumentView(
                title: AppLocalized.text("settings.legal.imprint", fallback: "Imprint / company info"),
                text: legalContentStore.settings.imprintInfoText
            )
        case .adminWorkspace(let section):
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
                            activePresentedSheetBinding.wrappedValue = nil
                        } label: {
                            Label("Schliessen", systemImage: "xmark")
                        }
                    }
                }
            }
            .environment(\.colorScheme, effectiveColorScheme)
        case .mailComposer:
            MailView(
                subject: supportMailSubject,
                body: supportMailBody,
                recipients: [supportMailbox],
                preferredSendingEmailAddress: preferredSupportSenderEmail
            )
        case .editableImage(let target):
            SingleImagePicker { provider in
                handleEditableImageProvider(provider, for: target)
            }
        }
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
            updated.collectionHandles = normalizedShopifyCollectionHandles(from: shopifyCollectionHandlesDraft)

            do {
                try await shopifyAdminSettingsStore.save(updated)
                await shopifyAdminSettingsStore.refreshAvailableCollections(force: true)
                showToastMessage(
                    "Shopify-Einstellungen gespeichert. Der naechste Sync nutzt jetzt diesen Store, deinen Storefront Token und die ausgewaehlten Collections.",
                    style: .success
                )
            } catch {
                showToastMessage("Shopify-Einstellungen konnten nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private var selectedShopifyCollectionHandles: [String] {
        normalizedShopifyCollectionHandles(from: shopifyCollectionHandlesDraft)
    }

    private var filteredShopifyCollectionOptions: [ShopifyCollectionOption] {
        let query = shopifyCollectionSearchDraft.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard query.isEmpty == false else {
            return shopifyAdminSettingsStore.availableCollections
        }

        return shopifyAdminSettingsStore.availableCollections.filter { collection in
            collection.handle.lowercased().contains(query) ||
                collection.displayTitle.lowercased().contains(query)
        }
    }

    private func normalizedShopifyCollectionHandles(from rawValue: String) -> [String] {
        Array(NSOrderedSet(array: rawValue
            .split(whereSeparator: \.isNewline)
            .flatMap { $0.split(separator: ",") }
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty })) as? [String] ?? []
    }

    private func toggleShopifyCollectionHandle(_ handle: String) {
        var handles = selectedShopifyCollectionHandles
        if let index = handles.firstIndex(of: handle) {
            handles.remove(at: index)
        } else {
            handles.append(handle)
        }
        shopifyCollectionHandlesDraft = handles.joined(separator: ", ")
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
                videoHubHeroVideoURL: videoHeaderHeroVideoURLDraft.trimmingCharacters(in: .whitespacesAndNewlines),
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

    @MainActor
    private func saveManagedUser(_ user: User) async -> Result<String, Error> {
        do {
            try await adminUserManagementStore.save(user)
            let message = "Konto gespeichert. Rolle, Rechte und KI-Limits wurden aktualisiert."
            showToastMessage(message, style: .success)
            return .success(message)
        } catch {
            showToastMessage("Konto konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            return .failure(error)
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
        guard automationSaveDisabledReasons.isEmpty else {
            if let firstReason = automationSaveDisabledReasons.first {
                updateAutomationInlineFeedback("Workflow nicht gespeichert: \(firstReason)", style: .warning)
                showToastMessage("Workflow nicht gespeichert: \(firstReason)", style: .warning)
            }
            return
        }

        let updated = automationDraftNormalizedSettings
        isSavingAutomationSettings = true

        Task {
            do {
                try await workflowAutomationSettings.save(updated)
                await MainActor.run {
                    let message = isOwnerUser
                        ? "Globaler Activepieces Owner-Flow gespeichert."
                        : "Eigener Workflow gespeichert."
                    isSavingAutomationSettings = false
                    syncAutomationDrafts(with: updated)
                    updateAutomationInlineFeedback(
                        isOwnerUser
                            ? "Globaler Activepieces Owner-Flow gespeichert. Rollen und Limits bleiben backendgefuehrt."
                            : "Eigener Workflow gespeichert. Der Agent kann ihn jetzt bewusst triggern.",
                        style: .success
                    )
                    showToastMessage(
                        message,
                        style: .success
                    )
                }
            } catch {
                await MainActor.run {
                    isSavingAutomationSettings = false
                    updateAutomationInlineFeedback("Workflow konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
                    showToastMessage("Workflow konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
                }
            }
        }
    }

    private func saveAIPromptSettings() {
        Task {
            var updated = aiPromptSettingsStore.settings
            updated.textInstruction = aiTextInstructionDraft
            updated.visualInstruction = aiVisualInstructionDraft
            updated.agentSystemInstruction = aiAgentSystemInstructionDraft
            updated.faqInstruction = aiFAQInstructionDraft
            updated.faqKnowledgeBase = aiFAQKnowledgeBaseDraft
            updated.assetLibraryLink = aiAssetLibraryLinkDraft
            updated.assetReferenceNotes = aiAssetReferenceNotesDraft

            do {
                try await aiPromptSettingsStore.save(updated)
                showToastMessage(
                    "KI-Anweisungen gespeichert. Neue Prompts gelten serverseitig sofort.",
                    style: .success
                )
            } catch {
                showToastMessage("KI-Anweisungen konnten nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func saveLegalContentSettings() {
        Task {
            var updated = legalContentStore.settings
            updated.brandName = legalBrandNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.operatorName = legalOperatorNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.rightsHolderName = legalRightsHolderNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.supportEmail = legalSupportEmailDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.lastUpdatedLabel = legalLastUpdatedLabelDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.imprintReference = legalImprintReferenceDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.masterNumberMeaning = legalMasterNumberMeaningDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.brandManifesto = legalBrandManifestoDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.symbolicNumericCode = legalSymbolicNumericCodeDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.symbolicLeetCode = legalSymbolicLeetCodeDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.symbolicCodeExplanation = legalSymbolicCodeExplanationDraft.trimmingCharacters(in: .whitespacesAndNewlines)

            do {
                try await legalContentStore.save(updated)
                showToastMessage("Rechtliche Module gespeichert. AGB, Datenschutz und Nutzungsbedingungen wurden aktualisiert.", style: .success)
            } catch {
                showToastMessage("Rechtliche Module konnten nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func saveAIRuntimeSettings() {
        Task {
            var updated = aiRuntimeSettingsStore.settings
            updated.costGuardEnabled = aiCostGuardEnabledDraft
            updated.agentProvider = aiAgentProviderDraft
            updated.fallbackAgentProvider = aiFallbackAgentProviderDraft
            updated.hardDailyCaps = AIRuntimeKindLimits(
                text: parsePositiveIntegerDraft(aiHardTextLimitDraft, fallback: updated.hardDailyCaps.text),
                visual: parsePositiveIntegerDraft(aiHardVisualLimitDraft, fallback: updated.hardDailyCaps.visual),
                agent: parsePositiveIntegerDraft(aiHardAgentLimitDraft, fallback: updated.hardDailyCaps.agent)
            )
            updated.globalDailyCaps = AIRuntimeKindLimits(
                text: parsePositiveIntegerDraft(aiGlobalTextLimitDraft, fallback: updated.globalDailyCaps.text),
                visual: parsePositiveIntegerDraft(aiGlobalVisualLimitDraft, fallback: updated.globalDailyCaps.visual),
                agent: parsePositiveIntegerDraft(aiGlobalAgentLimitDraft, fallback: updated.globalDailyCaps.agent)
            )
            updated.manus.isEnabled = aiManusEnabledDraft
            updated.manus.requestTimeoutMs = parseIntegerDraft(
                aiManusRequestTimeoutMsDraft,
                fallback: updated.manus.requestTimeoutMs,
                min: 3_000,
                max: 30_000
            )
            updated.manus.pollIntervalMs = parseIntegerDraft(
                aiManusPollIntervalMsDraft,
                fallback: updated.manus.pollIntervalMs,
                min: 500,
                max: 5_000
            )
            updated.manus.maxPollAttempts = parseIntegerDraft(
                aiManusMaxPollAttemptsDraft,
                fallback: updated.manus.maxPollAttempts,
                min: 2,
                max: 60
            )
            updated.manus.listMessagesLimit = parseIntegerDraft(
                aiManusListMessagesLimitDraft,
                fallback: updated.manus.listMessagesLimit,
                min: 5,
                max: 100
            )
            updated.manus.maxPromptChars = parseIntegerDraft(
                aiManusMaxPromptCharsDraft,
                fallback: updated.manus.maxPromptChars,
                min: 300,
                max: 12_000
            )
            updated.manus.maxHistoryTurns = parseIntegerDraft(
                aiManusMaxHistoryTurnsDraft,
                fallback: updated.manus.maxHistoryTurns,
                min: 0,
                max: 24
            )
            updated.manus.autoStopOnWaiting = aiManusAutoStopOnWaitingDraft
            updated.manus.blockHighCreditEvents = aiManusBlockHighCreditEventsDraft
            updated.manus.includeVerboseEvents = aiManusIncludeVerboseEventsDraft
            updated.bot.promptVersion = aiBotPromptVersionDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bot.qualityMode = aiBotQualityModeDraft
            updated.bot.faqMode = aiBotFAQModeDraft
            updated.bot.ownerMode = aiBotOwnerModeDraft
            updated.bot.answerLength = aiBotAnswerLengthDraft
            updated.bot.personalityStyle = aiBotPersonalityStyleDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bot.loggingLevel = aiBotLoggingLevelDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bot.diagnosticsMode = aiBotDiagnosticsModeDraft
            updated.bot.killSwitchEnabled = aiBotKillSwitchDraft
            updated.bot.modelPolicy.textPrimaryModel = aiBotTextPrimaryModelDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bot.modelPolicy.textFallbackModel = aiBotTextFallbackModelDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bot.modelPolicy.visualPrimaryModel = aiBotVisualPrimaryModelDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bot.modelPolicy.visualFallbackModel = aiBotVisualFallbackModelDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bot.costGuard.enabled = aiBotCostGuardEnabledDraft
            updated.bot.costGuard.preferBriefAnswersWhenCritical = aiBotPreferBriefCriticalDraft
            updated.bot.costGuard.shortAnswerMaxOutputTokens = parseIntegerDraft(
                aiBotShortAnswerMaxTokensDraft,
                fallback: updated.bot.costGuard.shortAnswerMaxOutputTokens,
                min: 80,
                max: 1_200
            )
            updated.bot.costGuard.standardAnswerMaxOutputTokens = parseIntegerDraft(
                aiBotStandardAnswerMaxTokensDraft,
                fallback: updated.bot.costGuard.standardAnswerMaxOutputTokens,
                min: 120,
                max: 2_400
            )
            updated.bot.routingPolicy.preferFaqWhenTopicMatched = aiBotPreferFaqRoutingDraft
            updated.bot.routingPolicy.preferProductGuideForNewUsers = aiBotPreferProductGuideDraft
            updated.bot.routingPolicy.allowVisualGeneration = aiBotAllowVisualGenerationDraft
            updated.bot.fallbackPolicy.allowTextFallback = aiBotAllowTextFallbackDraft
            updated.bot.fallbackPolicy.allowVisualFallback = aiBotAllowVisualFallbackDraft
            updated.bot.fallbackPolicy.exposeFallbackReason = aiBotExposeFallbackReasonDraft
            updated.bot.safetyPolicy.safeModeEnabled = aiBotSafeModeEnabledDraft
            updated.bot.safetyPolicy.strictUnknownHandling = aiBotStrictUnknownHandlingDraft
            updated.bot.safetyPolicy.blockSpeculativeFaqAnswers = aiBotBlockSpeculativeFAQDraft
            updated.bot.actionLayer.proactiveHintsEnabled = aiBotProactiveHintsEnabledDraft
            updated.bot.actionLayer.triggerAiLimitNearEnabled = aiBotTriggerAiLimitNearEnabledDraft
            updated.bot.actionLayer.triggerRestoreAvailableEnabled = aiBotTriggerRestoreAvailableEnabledDraft
            updated.bot.actionLayer.triggerOrderShippedEnabled = aiBotTriggerOrderShippedEnabledDraft
            updated.bot.actionLayer.triggerPaymentMethodsChangedEnabled = aiBotTriggerPaymentMethodsChangedEnabledDraft
            updated.bot.actionLayer.triggerUsageBasedUpgradeEnabled = aiBotTriggerUsageBasedUpgradeEnabledDraft
            updated.bot.actionLayer.warningThresholdPercent = parseIntegerDraft(
                aiBotWarningThresholdPercentDraft,
                fallback: updated.bot.actionLayer.warningThresholdPercent,
                min: 50,
                max: 99
            )
            updated.bot.actionLayer.criticalThresholdPercent = parseIntegerDraft(
                aiBotCriticalThresholdPercentDraft,
                fallback: updated.bot.actionLayer.criticalThresholdPercent,
                min: 60,
                max: 100
            )
            updated.bot.actionLayer.upgradeHintFreeToProText = aiBotUpgradeHintFreeToProTextDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bot.actionLayer.upgradeHintProToCreatorText = aiBotUpgradeHintProToCreatorTextDraft.trimmingCharacters(in: .whitespacesAndNewlines)
            updated.bot.actionLayer.faqPriorityMode = aiBotFaqPriorityModeDraft
            updated.bot.actionLayer.promptVersionAlias = aiBotPromptVersionAliasDraft.trimmingCharacters(in: .whitespacesAndNewlines)

            do {
                try await aiRuntimeSettingsStore.save(updated)
                showToastMessage(
                    "KI-Runtime gespeichert. Provider und Kosten-Guard gelten serverseitig sofort.",
                    style: .success
                )
            } catch {
                showToastMessage("KI-Runtime konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func parsePositiveIntegerDraft(_ draft: String, fallback: Int) -> Int {
        parseIntegerDraft(
            draft,
            fallback: fallback,
            min: 1,
            max: 100_000
        )
    }

    private func parseIntegerDraft(
        _ draft: String,
        fallback: Int,
        min: Int,
        max: Int
    ) -> Int {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let value = Int(trimmed) else {
            return fallback
        }

        return Swift.max(min, Swift.min(max, value))
    }

    private func runAutomationTest() {
        guard automationTestDisabledReasons.isEmpty else {
            if let firstReason = automationTestDisabledReasons.first {
                updateAutomationInlineFeedback("Workflow-Test nicht gestartet: \(firstReason)", style: .warning)
                showToastMessage("Workflow-Test nicht gestartet: \(firstReason)", style: .warning)
            }
            return
        }

        let updated = automationDraftNormalizedSettings
        isRunningAutomationTest = true

        Task {
            do {
                try await workflowAutomationSettings.save(updated)
                let message = try await workflowAutomationSettings.triggerTest()
                await MainActor.run {
                    isRunningAutomationTest = false
                    syncAutomationDrafts(with: updated)
                    updateAutomationInlineFeedback(message, style: .success)
                    showToastMessage(message, style: .success)
                }
            } catch {
                await MainActor.run {
                    isRunningAutomationTest = false
                    updateAutomationInlineFeedback("Workflow-Test fehlgeschlagen: \(error.localizedDescription)", style: .error)
                    showToastMessage("Workflow-Test fehlgeschlagen: \(error.localizedDescription)", style: .error)
                }
            }
        }
    }

    private func saveManusBYOSSettings() {
        let trimmedAPIKey = manusByosAPIKeyDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedAPIKey.isEmpty && manusByosEnabledDraft && !manusByosStore.settings.hasAPIKey {
            showToastMessage("Bitte hinterlege zuerst einen Manus API Key.", style: .error)
            return
        }

        do {
            if !trimmedAPIKey.isEmpty {
                try manusByosStore.saveAPIKey(trimmedAPIKey)
                manusByosAPIKeyDraft = ""
            }
            try manusByosStore.updateEnabled(manusByosEnabledDraft)
            if manusByosEnabledDraft {
                showToastMessage("Manus BYOS aktiv. Der Agent nutzt jetzt deinen persoenlichen Key.", style: .success)
            } else {
                showToastMessage("Manus BYOS pausiert. Der Agent nutzt wieder das Backend-Setup.", style: .success)
            }
        } catch {
            showToastMessage("Manus BYOS konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
        }
    }

    private func clearManusBYOSAPIKey() {
        manusByosStore.clearAPIKey()
        manusByosAPIKeyDraft = ""
        manusValidationStatus = "awaiting_external_auth"
        manusValidationMessage = "Key entfernt. Externer Manus-Run wartet auf Auth oder nutzt internen Fallback."
        showToastMessage("Manus API Key lokal entfernt. BYOS ist fuer dieses Konto aus.", style: .success)
    }

    private func validateManusBYOSKey() {
        let key = resolveEffectiveManusKey()
        guard !key.isEmpty else {
            manusValidationStatus = "awaiting_external_auth"
            manusValidationMessage = "Kein Manus-Key vorhanden. Externer Lauf wartet auf Auth oder faellt intern zurueck."
            showToastMessage("Bitte hinterlege zuerst einen Manus API Key.", style: .warning)
            return
        }

        isValidatingManusKey = true
        Task {
            do {
                let result = try await Functions.functions(region: "us-central1").invokeCallable(
                    "validateManusApiKey",
                    payload: ["apiKey": key]
                )
                let payload = result.data as? [String: Any]
                let valid = payload?["valid"] as? Bool ?? false
                let message = (payload?["message"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines)
                await MainActor.run {
                    isValidatingManusKey = false
                    manusValidationStatus = valid ? "key_valid" : "key_invalid"
                    manusValidationMessage = message?.isEmpty == false ? message! :
                        (valid ? "Manus-Key ist gueltig." : "Manus-Key ist ungueltig oder nicht erreichbar.")
                    showToastMessage(manusValidationMessage, style: valid ? .success : .error)
                }
            } catch {
                await MainActor.run {
                    isValidatingManusKey = false
                    manusValidationStatus = "external_failed"
                    manusValidationMessage = "Validierung fehlgeschlagen: \(error.localizedDescription)"
                    showToastMessage(manusValidationMessage, style: .error)
                }
            }
        }
    }

    private func resolveEffectiveManusKey() -> String {
        let draft = manusByosAPIKeyDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        if !draft.isEmpty {
            return draft
        }
        return ManusBYOSStore.shared.currentAPIKeyOrNil() ?? ""
    }

    private func manusValidationLabel() -> String {
        switch manusValidationStatus {
        case "key_valid":
            return "key valid"
        case "key_invalid":
            return "key invalid"
        case "awaiting_external_auth":
            return "awaiting external auth"
        case "fallback_internal":
            return "fallback internal"
        case "external_failed":
            return "external failed"
        default:
            return manusByosStore.settings.hasAPIKey ? "fallback internal" : "awaiting external auth"
        }
    }

    private func saveAISubscriptionPricing() {
        let creatorPriceID = aiCreatorPriceIDDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        let studioPriceID = aiStudioPriceIDDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        let iosCreatorProductID = aiIOSCreatorProductIDDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        let iosStudioProductID = aiIOSStudioProductIDDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        let iosAppAppleID = aiIOSAppAppleIDDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        let androidCreatorProductID = aiAndroidCreatorProductIDDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        let androidStudioProductID = aiAndroidStudioProductIDDraft.trimmingCharacters(in: .whitespacesAndNewlines)

        if aiSubscriptionsEnabledDraft &&
            (creatorPriceID.isEmpty ||
             studioPriceID.isEmpty ||
             iosCreatorProductID.isEmpty ||
             iosStudioProductID.isEmpty ||
             iosAppAppleID.isEmpty ||
             androidCreatorProductID.isEmpty ||
             androidStudioProductID.isEmpty) {
            showToastMessage("Bitte Stripe-Price-IDs, iOS-/Android-Produkt-IDs und die Apple App ID eintragen, bevor du KI-Abo aktivierst.", style: .error)
            return
        }

        Task {
            var updated = paymentMethodSettingsStore.settings
            updated.aiSubscriptions.enabled = aiSubscriptionsEnabledDraft
            updated.aiSubscriptions.creatorPriceID = creatorPriceID
            updated.aiSubscriptions.studioPriceID = studioPriceID
            updated.aiSubscriptions.iosCreatorProductID = iosCreatorProductID
            updated.aiSubscriptions.iosStudioProductID = iosStudioProductID
            updated.aiSubscriptions.iosAppAppleID = iosAppAppleID
            updated.aiSubscriptions.androidCreatorProductID = androidCreatorProductID
            updated.aiSubscriptions.androidStudioProductID = androidStudioProductID

            do {
                try await paymentMethodSettingsStore.save(updated)
                showToastMessage("KI-Abo-Konfiguration gespeichert.", style: .success)
            } catch {
                showToastMessage("KI-Abo-Konfiguration konnte nicht gespeichert werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func purchaseAISubscription(_ plan: UserQuotaPlan) {
        if let user = authManager.userSession,
           let blockedReason = aiSubscriptionPurchaseBlockedReason(for: user) {
            showToastMessage(blockedReason, style: .error)
            return
        }

        Task {
            do {
                let outcome = try await aiSubscriptionStore.purchase(plan: plan)
                switch outcome {
                case .success(let activatedPlan):
                    showToastMessage("\(activatedPlan.displayTitle) wurde im App Store aktiviert.", style: .success)
                case .pending:
                    showToastMessage("Der Kauf wartet noch auf die Freigabe im App Store.", style: .info)
                case .cancelled:
                    showToastMessage("Der App-Store-Kauf wurde abgebrochen.", style: .info)
                }
            } catch {
                showToastMessage("Das KI-Abo konnte nicht gestartet werden: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func restoreAISubscriptionPurchases() {
        let shouldForceEmptySync = authManager.userSession?.normalizedAISubscriptionProvider == "app_store"

        Task {
            do {
                try await aiSubscriptionStore.restorePurchases(forceEmptySync: shouldForceEmptySync)
                showToastMessage("App-Store-Kaeufe wurden synchronisiert.", style: .success)
            } catch {
                showToastMessage("Die App-Store-Synchronisierung ist fehlgeschlagen: \(error.localizedDescription)", style: .error)
            }
        }
    }

    private func manageAISubscription() {
        Task {
            do {
                try await aiSubscriptionStore.manageSubscriptions()
            } catch {
                showToastMessage("Die Abo-Verwaltung konnte nicht geoeffnet werden: \(error.localizedDescription)", style: .error)
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            SettingsFieldTitle(title: title, colorScheme: colorScheme)

            ZStack(alignment: .topLeading) {
                if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(placeholder)
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .padding(.horizontal, 18)
                        .padding(.vertical, SkydownLayout.cardPadding)
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
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            HStack(spacing: SkydownLayout.stackSpacingCompact) {
                ZStack {
                    Circle()
                        .fill(AppColors.accent(for: colorScheme).opacity(0.14))
                        .frame(width: 48, height: 48)

                    Text(initials.isEmpty ? "U" : initials)
                        .font(.headline.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
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
            .skydownInteractiveFeedback()
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
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

            HStack(spacing: SkydownLayout.stackSpacingPill) {
                Button(actionTitle, action: onPrimaryAction)
                    .buttonStyle(.borderedProminent)
                    .skydownInteractiveFeedback()
                    .tint(AppColors.accent(for: colorScheme))

                if let secondaryActionTitle, let onSecondaryAction {
                    Button(secondaryActionTitle, action: onSecondaryAction)
                        .buttonStyle(.bordered)
                        .skydownInteractiveFeedback()
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct StripeBackendSecretsCard: View {
    let colorScheme: ColorScheme
    let status: StripeBackendSecretsStatus
    @Binding var stripeSecretKey: String
    @Binding var stripeWebhookSecret: String
    let onSave: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
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
                    text: status.isReady ? "Backend bereit" : "Setup fehlt",
                    colorScheme: colorScheme
                )
            }

            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                SettingsBadge(
                    text: status.hasSecretKey ? "Secret Key gesetzt" : "Secret Key fehlt",
                    colorScheme: colorScheme
                )
                SettingsBadge(
                    text: status.hasWebhookSecret ? "Webhook Secret gesetzt" : "Webhook Secret fehlt",
                    colorScheme: colorScheme
                )
            }

            Text("Nur beim Speichern — nicht in Firestore. Live- oder Test-Keys sind moeglich. Leer = unveraendert.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            SettingsSecureInputField(
                title: "Stripe Secret Key",
                text: $stripeSecretKey,
                colorScheme: colorScheme,
                placeholder: "sk_live_..., rk_live_..., sk_test_... oder rk_test_..."
            )

            SettingsSecureInputField(
                title: "Stripe Webhook Secret",
                text: $stripeWebhookSecret,
                colorScheme: colorScheme,
                placeholder: "whsec_..."
            )

            Button("Sicher speichern", action: onSave)
                .buttonStyle(.borderedProminent)
                .skydownInteractiveFeedback()
                .tint(AppColors.accent(for: colorScheme))
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct NativeAISubscriptionStatusCard: View {
    let colorScheme: ColorScheme
    let user: User
    let products: [NativeAISubscriptionProduct]
    let isStorefrontReady: Bool
    let isLoadingProducts: Bool
    let isSyncing: Bool
    let activePurchasePlan: UserQuotaPlan?
    let lastErrorMessage: String?
    let statusLine: String?
    let detailLine: String?
    let purchaseDisabledReason: String?
    let onPurchase: (UserQuotaPlan) -> Void
    let onRestore: () -> Void
    let onManage: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingPill) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text("KI-Plan")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Creator / Studio über App Store — Konto synchron.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                SettingsBadge(
                    text: isStorefrontReady ? "StoreKit bereit" : "StoreKit in Vorbereitung",
                    colorScheme: colorScheme
                )
            }

            Text("Aktueller Quota-Plan: \(user.resolvedQuotaPlan.displayTitle)")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let statusLine {
                Text(statusLine)
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            if let detailLine {
                Text(detailLine)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            if let purchaseDisabledReason {
                Text(purchaseDisabledReason)
                    .font(.footnote)
                    .foregroundColor(Color(red: 214 / 255, green: 43 / 255, blue: 84 / 255))
            }

            if isLoadingProducts || isSyncing {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    ProgressView()
                    Text(isLoadingProducts ? "StoreKit-Produkte werden geladen..." : "KI-Abo wird synchronisiert...")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            } else if isStorefrontReady && !products.isEmpty {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    ForEach(products) { product in
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                            HStack(alignment: .center, spacing: SkydownLayout.stackSpacingPill) {
                                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                                    Text(product.plan.displayTitle)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundColor(AppColors.text(for: colorScheme))

                                    Text(product.displayPrice)
                                        .font(.footnote.weight(.semibold))
                                        .foregroundColor(AppColors.accent(for: colorScheme))
                                }

                                Spacer()

                                if user.hasActiveAISubscription && user.resolvedAISubscriptionPlan == product.plan {
                                    SettingsBadge(text: "Aktiv", colorScheme: colorScheme)
                                }
                            }

                            if !product.description.isEmpty {
                                Text(product.description)
                                    .font(.footnote)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }

                            Button {
                                onPurchase(product.plan)
                            } label: {
                                Text(activePurchasePlan == product.plan ? "Wird gestartet..." : "\(product.plan.displayTitle) im App Store aktivieren")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .skydownInteractiveFeedback()
                            .tint(AppColors.accent(for: colorScheme))
                            .disabled(activePurchasePlan != nil || purchaseDisabledReason != nil)
                        }
                        .padding(12)
                        .background(AppColors.cardBackground(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
                    }
                }
            } else if isStorefrontReady {
                Text("StoreKit aktiv — keine KI-Produkte in diesem Build.")
                    .font(.footnote)
                    .foregroundColor(Color(red: 214 / 255, green: 43 / 255, blue: 84 / 255))
            } else {
                Text("Abos in Vorbereitung — IDs folgen.")
                    .font(.footnote)
                    .foregroundColor(Color(red: 214 / 255, green: 43 / 255, blue: 84 / 255))
            }

            if let lastErrorMessage, !lastErrorMessage.isEmpty {
                Text(lastErrorMessage)
                    .font(.footnote)
                    .foregroundColor(Color(red: 214 / 255, green: 43 / 255, blue: 84 / 255))
            }

            HStack(spacing: SkydownLayout.stackSpacingPill) {
                Button("Kaeufe synchronisieren", action: onRestore)
                    .buttonStyle(.bordered)
                    .skydownInteractiveFeedback()

                Button("Abo verwalten", action: onManage)
                    .buttonStyle(.bordered)
                    .skydownInteractiveFeedback()
                    .disabled(!isStorefrontReady)
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
    }
}

private struct AISubscriptionPricingCard: View {
    let colorScheme: ColorScheme
    @Binding var isEnabled: Bool
    @Binding var creatorPriceID: String
    @Binding var studioPriceID: String
    @Binding var iosCreatorProductID: String
    @Binding var iosStudioProductID: String
    @Binding var iosAppAppleID: String
    @Binding var androidCreatorProductID: String
    @Binding var androidStudioProductID: String
    let onSave: () -> Void

    private var hasStripePricing: Bool {
        !creatorPriceID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !studioPriceID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var hasIOSProducts: Bool {
        !iosCreatorProductID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !iosStudioProductID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !iosAppAppleID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var hasAndroidProducts: Bool {
        !androidCreatorProductID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !androidStudioProductID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text("KI-Abos")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(isEnabled ? "Native Vorbereitung aktiv" : "Vorbereitung pausiert")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(
                            isEnabled
                                ? AppColors.accent(for: colorScheme)
                                : AppColors.secondaryText(for: colorScheme)
                        )
                }

                Spacer()

                VStack(alignment: .trailing, spacing: SkydownLayout.stackSpacingDense) {
                    SettingsBadge(text: hasStripePricing ? "Stripe ok" : "Stripe fehlt", colorScheme: colorScheme)
                    SettingsBadge(text: hasIOSProducts ? "iOS ok" : "iOS fehlt", colorScheme: colorScheme)
                    SettingsBadge(text: hasAndroidProducts ? "Android ok" : "Android offen", colorScheme: colorScheme)
                }
            }

            Text("Stripe-Preise + native Produkt-IDs (iOS / später Android). iOS: Price-IDs, Produkt-IDs, App-ID.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            SettingsToggleCard(
                colorScheme: colorScheme,
                title: "KI-Abo live schalten",
                subtitle: "Nur aktivieren, wenn Stripe, StoreKit und die serverseitige Pruefung fertig sind.",
                isOn: $isEnabled
            )

            SettingsInputField(
                title: "Creator Price ID",
                text: $creatorPriceID,
                colorScheme: colorScheme,
                placeholder: "price_..."
            )

            SettingsInputField(
                title: "Studio Price ID",
                text: $studioPriceID,
                colorScheme: colorScheme,
                placeholder: "price_..."
            )

            SettingsInputField(
                title: "iOS Creator Product ID",
                text: $iosCreatorProductID,
                colorScheme: colorScheme,
                placeholder: "com.skydown.ai.creator"
            )

            SettingsInputField(
                title: "iOS Studio Product ID",
                text: $iosStudioProductID,
                colorScheme: colorScheme,
                placeholder: "com.skydown.ai.studio"
            )

            SettingsInputField(
                title: "Apple App ID",
                text: $iosAppAppleID,
                colorScheme: colorScheme,
                placeholder: "1234567890"
            )

            SettingsInputField(
                title: "Android Creator Product ID",
                text: $androidCreatorProductID,
                colorScheme: colorScheme,
                placeholder: "skydown_ai_creator"
            )

            SettingsInputField(
                title: "Android Studio Product ID",
                text: $androidStudioProductID,
                colorScheme: colorScheme,
                placeholder: "skydown_ai_studio"
            )

            Button("KI-Abo speichern", action: onSave)
                .buttonStyle(.borderedProminent)
                .skydownInteractiveFeedback()
                .tint(AppColors.accent(for: colorScheme))
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
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
                .skydownInteractiveFeedback()
                .tint(AppColors.accentMystic(for: colorScheme))
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingComfortable) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    Text(username ?? "SkyOS Einstellungen")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .fixedSize(horizontal: false, vertical: true)

                    Text("Konto, Anzeige und Support sauber an einem Ort.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                ZStack {
                    RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                        .frame(width: 58, height: 58)

                    Image(systemName: "slider.horizontal.3")
                        .font(.title2)
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }
            }

            ViewThatFits(in: .horizontal) {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    SettingsBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
                    SettingsBadge(text: notificationsEnabled ? "Hinweise an" : "Hinweise aus", colorScheme: colorScheme)
                    SettingsBadge(text: appearance, colorScheme: colorScheme)
                    if isOwner {
                        SettingsBadge(text: "Owner aktiv", colorScheme: colorScheme)
                    }
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SettingsBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
                        SettingsBadge(text: notificationsEnabled ? "Hinweise an" : "Hinweise aus", colorScheme: colorScheme)
                    }

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SettingsBadge(text: appearance, colorScheme: colorScheme)
                        if isOwner {
                            SettingsBadge(text: "Owner aktiv", colorScheme: colorScheme)
                        }
                    }
                }
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
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

private struct OwnerCommandCenterCard: View {
    let colorScheme: ColorScheme
    let isOwner: Bool
    let paymentStatus: String
    let userStatus: String
    let headerStatus: String
    let membershipStatus: String
    let aiStatus: String
    let onOpenUsers: () -> Void
    let onOpenPayments: () -> Void
    let onOpenHeaders: () -> Void
    let onOpenMembershipOps: () -> Void
    let onOpenAI: () -> Void

    private var columns: [GridItem] {
        [
            GridItem(.flexible(), spacing: SkydownLayout.stackSpacingPill),
            GridItem(.flexible(), spacing: SkydownLayout.stackSpacingPill)
        ]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                    Text(isOwner ? "Owner-Steuerung" : "Owner-Steuerung gesperrt")
                        .font(.headline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(isOwner ? "Direkte Kontrollpunkte fuer Release, Commerce und KI-Kosten." : "Nur das feste Owner-Konto kann diese Live-Systeme veraendern.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                SettingsBadge(
                    text: isOwner ? "Aktiv" : "Gesperrt",
                    colorScheme: colorScheme
                )
            }

            LazyVGrid(columns: columns, alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                OwnerCommandSignalButton(
                    colorScheme: colorScheme,
                    title: "Membership-Steuerung",
                    detail: membershipStatus,
                    iconName: "chart.xyaxis.line",
                    isEnabled: isOwner,
                    action: onOpenMembershipOps
                )
                OwnerCommandSignalButton(
                    colorScheme: colorScheme,
                    title: "Rollen",
                    detail: userStatus,
                    iconName: "person.2.badge.key",
                    isEnabled: isOwner,
                    action: onOpenUsers
                )
                OwnerCommandSignalButton(
                    colorScheme: colorScheme,
                    title: "Zahlungen",
                    detail: paymentStatus,
                    iconName: "creditcard",
                    isEnabled: isOwner,
                    action: onOpenPayments
                )
                OwnerCommandSignalButton(
                    colorScheme: colorScheme,
                    title: "Header",
                    detail: headerStatus,
                    iconName: "photo.on.rectangle.angled",
                    isEnabled: isOwner,
                    action: onOpenHeaders
                )
                OwnerCommandSignalButton(
                    colorScheme: colorScheme,
                    title: "KI Schutz",
                    detail: aiStatus,
                    iconName: "bolt.shield",
                    isEnabled: isOwner,
                    action: onOpenAI
                )
            }
        }
        .padding(SkydownLayout.cardPadding)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.accent(for: colorScheme).opacity(colorScheme == .dark ? 0.18 : 0.1),
                    AppColors.secondaryBackground(for: colorScheme)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
    }
}

private struct OwnerCommandSignalButton: View {
    let colorScheme: ColorScheme
    let title: String
    let detail: String
    let iconName: String
    let isEnabled: Bool
    let accessibilityIdentifier: String?
    let action: () -> Void

    init(
        colorScheme: ColorScheme,
        title: String,
        detail: String,
        iconName: String,
        isEnabled: Bool,
        accessibilityIdentifier: String? = nil,
        action: @escaping () -> Void
    ) {
        self.colorScheme = colorScheme
        self.title = title
        self.detail = detail
        self.iconName = iconName
        self.isEnabled = isEnabled
        self.accessibilityIdentifier = accessibilityIdentifier
        self.action = action
    }

    var body: some View {
        let button = Button(action: action) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                HStack {
                    Image(systemName: iconName)
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))

                    Spacer()

                    Image(systemName: "arrow.up.right")
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .opacity(isEnabled ? 1 : 0.35)
                }

                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(detail)
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(AppColors.cardBackground(for: colorScheme).opacity(isEnabled ? 0.95 : 0.45))
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(isEnabled ? 0.14 : 0.06), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .skydownInteractiveFeedback()
        .disabled(!isEnabled)

        if let accessibilityIdentifier, !accessibilityIdentifier.isEmpty {
            button.accessibilityIdentifier(accessibilityIdentifier)
        } else {
            button
        }
    }
}

private struct SettingsSectionCard<Content: View>: View {
    let title: String
    let colorScheme: ColorScheme
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            content
        }
        .padding(SkydownLayout.panelPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
    }
}

private struct SettingsToggleCard: View {
    let colorScheme: ColorScheme
    let title: String
    let subtitle: String
    @Binding var isOn: Bool
    var isEnabled: Bool = true

    var body: some View {
        HStack(spacing: SkydownLayout.stackSpacingCompact) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
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
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
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
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
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
                RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(isSelected ? 1 : 0.18), lineWidth: 1)
            )
            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
        }
        .skydownTactileAction()
    }
}

private struct SettingsBadge: View {
    let text: String
    let colorScheme: ColorScheme
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            badgeContent
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private var badgeContent: some View {
        HStack(spacing: SkydownLayout.stackSpacingSubtle) {
            Text(text)
                .font(.caption.weight(.semibold))
            Image(systemName: "arrow.right")
                .font(.caption2.weight(.bold))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(AppColors.accentMystic(for: colorScheme).opacity(0.12))
        .foregroundColor(AppColors.accentMystic(for: colorScheme))
        .clipShape(Capsule())
    }
}

private struct SettingsLockedHintCard: View {
    let colorScheme: ColorScheme
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
            Image(systemName: "lock.fill")
                .font(.caption.weight(.bold))
                .foregroundColor(.white)
                .padding(.top, 2)

            Text(text)
                .font(.footnote)
                .foregroundColor(.white)
                .multilineTextAlignment(.leading)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                .fill(Color.red.opacity(colorScheme == .dark ? 0.34 : 0.76))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                .stroke(Color.white.opacity(0.24), lineWidth: 1)
        )
    }
}

private struct SettingsInlineStatusStrip: View {
    let icon: String
    let title: String
    let message: String
    let detail: String?
    let accent: Color
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingChrome) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: icon)
                    .font(.caption.weight(.bold))
                    .foregroundColor(accent)
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
            }

            Text(message)
                .font(.caption.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let detail, !detail.isEmpty {
                HStack(spacing: SkydownLayout.stackSpacingDense) {
                    Image(systemName: "circle.fill")
                        .font(.system(size: 5))
                    Text(detail)
                        .lineLimit(1)
                }
                .font(.caption.weight(.semibold))
                .foregroundColor(accent.opacity(0.88))
                .padding(.top, 1)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                .stroke(accent.opacity(0.12), lineWidth: 1)
        )
    }
}

private struct SettingsUtilityAction {
    let title: String
    let systemImage: String
    let accent: Color
    let action: () -> Void
}

private struct SettingsUtilityRow: View {
    let colorScheme: ColorScheme
    let actions: [SettingsUtilityAction]

    var body: some View {
        HStack(spacing: SkydownLayout.stackSpacingMicro) {
            ForEach(Array(actions.enumerated()), id: \.offset) { _, item in
                Button(action: item.action) {
                    HStack(spacing: SkydownLayout.stackSpacingDense) {
                        Image(systemName: item.systemImage)
                            .font(.caption2.weight(.bold))
                        Text(item.title)
                            .font(.caption.weight(.semibold))
                            .lineLimit(1)
                    }
                    .foregroundColor(item.accent)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(
                        Capsule(style: .continuous)
                            .fill(item.accent.opacity(colorScheme == .dark ? 0.16 : 0.12))
                    )
                    .overlay(
                        Capsule(style: .continuous)
                            .stroke(item.accent.opacity(0.22), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
                .skydownInteractiveFeedback()
            }
        }
        .padding(.top, 2)
        .transition(.opacity.combined(with: .move(edge: .top)))
    }
}

private struct SettingsStatusCard: View {
    let style: ToastStyle
    let title: String
    let message: String
    let details: [String]
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: style.icon)
                    .foregroundColor(style.color)
                    .font(.subheadline.weight(.semibold))

                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Spacer()
            }

            Text(message)
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if !details.isEmpty {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    ForEach(Array(details.enumerated()), id: \.offset) { _, detail in
                        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingDense) {
                            Image(systemName: "circle.fill")
                                .font(.system(size: 5))
                                .foregroundColor(style.color.opacity(0.9))
                                .padding(.top, 6)

                            Text(detail)
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    }
                }
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                .stroke(style.color.opacity(0.35), lineWidth: 1)
        )
    }
}

private struct ShopifyCollectionToggleCard: View {
    let collection: ShopifyCollectionOption
    let isSelected: Bool
    let colorScheme: ColorScheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                HStack {
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                        .foregroundColor(isSelected ? AppColors.accent(for: colorScheme) : AppColors.secondaryText(for: colorScheme))

                    Spacer(minLength: 8)

                    if let productCount = collection.productCount {
                        Text("\(productCount)")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }

                Text(collection.displayTitle)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .multilineTextAlignment(.leading)

                Text(collection.handle)
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .multilineTextAlignment(.leading)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                    .fill(isSelected ? AppColors.accent(for: colorScheme).opacity(0.18) : AppColors.secondaryBackground(for: colorScheme))
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                    .stroke(
                        isSelected ? AppColors.accent(for: colorScheme) : AppColors.secondaryText(for: colorScheme).opacity(0.18),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private enum SettingsAdminWorkspaceSection: String, CaseIterable, Identifiable, Equatable {
    case payments = "Zahlungen"
    case users = "User"
    case artists = "Artists"
    case headers = "Header"
    case shopify = "Shopify"
    case commerce = "Versand"
    case visuals = "Visuals"
    case automation = "Automation"
    case aiPrompts = "KI Prompts"
    case membershipOps = "Membership Ops"

    var id: String { rawValue }

    var accessibilityKey: String {
        switch self {
        case .payments:
            return "payments"
        case .users:
            return "users"
        case .artists:
            return "artists"
        case .headers:
            return "headers"
        case .shopify:
            return "shopify"
        case .commerce:
            return "commerce"
        case .visuals:
            return "visuals"
        case .automation:
            return "automation"
        case .aiPrompts:
            return "aiPrompts"
        case .membershipOps:
            return "membershipOps"
        }
    }

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
        case .aiPrompts:
            return "sparkles"
        case .membershipOps:
            return "chart.xyaxis.line"
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
            return "Owner-Flow global oder eigenen Activepieces/n8n-Flow pro Konto sauber anbinden."
        case .aiPrompts:
            return "Serverseitige Anweisungen fuer Bot, Visuals und Agent zentral pflegen."
        case .membershipOps:
            return "KPI, Trends, Recommendations und Experiments fuer Revenue Operations steuern."
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
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
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
        .skydownTactileAction()
    }
}

private struct SettingsAdminWorkspaceListRow: View {
    let section: SettingsAdminWorkspaceSection
    let colorScheme: ColorScheme
    let detailText: String
    let accessibilityIdentifier: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: SkydownLayout.stackSpacingCompact) {
                ZStack {
                    RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                        .fill(AppColors.accent(for: colorScheme).opacity(0.12))
                        .frame(width: 44, height: 44)

                    Image(systemName: section.iconName)
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(section.rawValue)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(section.subtitle)
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .multilineTextAlignment(.leading)
                }

                Spacer(minLength: 0)

                VStack(alignment: .trailing, spacing: SkydownLayout.stackSpacingDense) {
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
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
        }
        .accessibilityIdentifier(accessibilityIdentifier)
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private struct SettingsAdminWorkspaceSidebarButton: View {
    let section: SettingsAdminWorkspaceSection
    let isSelected: Bool
    let colorScheme: ColorScheme
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: SkydownLayout.stackSpacingPill) {
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
                RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                    .stroke(
                        AppColors.accent(for: colorScheme).opacity(isSelected ? 1 : 0.14),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private struct SettingsAdminWorkspaceSummaryCard: View {
    let section: SettingsAdminWorkspaceSection
    let colorScheme: ColorScheme

    var body: some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
            ZStack {
                Circle()
                    .fill(AppColors.accent(for: colorScheme).opacity(0.12))
                    .frame(width: 34, height: 34)

                Image(systemName: section.iconName)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
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
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
    }
}

private struct SettingsAdminRoleGuideCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            Text("Rollen im System")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            VStack(spacing: SkydownLayout.stackSpacingPill) {
                ForEach(UserRole.allCases, id: \.self) { role in
                    HStack(alignment: .top, spacing: SkydownLayout.stackSpacingPill) {
                        SettingsBadge(text: role.displayTitle, colorScheme: colorScheme)

                        Text(role.roleSummary)
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
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
        let initialEditors = page.brand == .nicma ? Array(page.editorUids.prefix(1)) : page.editorUids
        _selectedEditorUids = State(initialValue: Set(initialEditors))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSubtle) {
                    Text(page.artistName)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(page.hasCustomPresentation ? "Seite hat schon Inhalt." : "Noch als Platzhalter. Nach dem ersten Speichern ist die Artist-Seite live.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                VStack(alignment: .trailing, spacing: SkydownLayout.stackSpacingDense) {
                    SettingsBadge(text: page.hasCustomPresentation ? "Live" : "Platzhalter", colorScheme: colorScheme)
                    SettingsBadge(
                        text: page.brand == .nicma
                            ? (selectedEditorUids.isEmpty ? "Kein Editor" : "1 Editor")
                            : "\(selectedEditorUids.count) Editoren",
                        colorScheme: colorScheme
                    )
                }
            }

            if users.isEmpty {
                Text("Mehr Konten = Editoren hier.")
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else {
                Text(page.brand == .nicma ? "Editor" : "Editoren")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 132), spacing: SkydownLayout.stackSpacingMicro)],
                    alignment: .leading,
                    spacing: SkydownLayout.stackSpacingMicro
                ) {
                    ForEach(users) { user in
                        let isSelected = selectedEditorUids.contains(user.id ?? "")

                        Button {
                            guard let userId = user.id, !userId.isEmpty else { return }
                            if page.brand == .nicma {
                                selectedEditorUids = isSelected ? [] : [userId]
                            } else {
                                if isSelected {
                                    selectedEditorUids.remove(userId)
                                } else {
                                    selectedEditorUids.insert(userId)
                                }
                            }
                        } label: {
                            HStack(spacing: SkydownLayout.stackSpacingDense) {
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
                        .skydownTactileAction()
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
                        heroVideoURL: page.heroVideoURL,
                        instagramURL: page.instagramURL,
                        spotifyURL: page.spotifyURL,
                        youtubeURL: page.youtubeURL,
                        studioPriceList: page.studioPriceList,
                        editorUids: page.brand == .nicma
                            ? Array(selectedEditorUids.prefix(1))
                            : Array(selectedEditorUids).sorted(),
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
            .skydownInteractiveFeedback()
            .tint(AppColors.accent(for: colorScheme))
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct SettingsAdminUserCard: View {
    let user: User
    let isCurrentUser: Bool
    let colorScheme: ColorScheme
    let onSave: (User) async -> Result<String, Error>

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
    @State private var isSaving = false
    @State private var saveErrorMessage: String?
    @State private var saveSuccessCount = 0

    init(
        user: User,
        isCurrentUser: Bool,
        colorScheme: ColorScheme,
        onSave: @escaping (User) async -> Result<String, Error>
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
        _canManageMusicCatalog = State(initialValue: user.canManageMusicCatalog)
        _canManageVideoCatalog = State(initialValue: user.canManageVideoCatalog)
        _canModerateProfiles = State(initialValue: user.canModerateProfiles)
    }

    private var canAssignOwnerRoleToUser: Bool {
        user.email
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() == UserRole.ownerEmail
    }

    private func isRoleSelectionDisabled(_ role: UserRole) -> Bool {
        if user.isPlatformOwner {
            return true
        }

        if isCurrentUser && role != user.resolvedRole {
            return true
        }

        if role == .owner && !canAssignOwnerRoleToUser {
            return true
        }

        return false
    }

    private var draftUser: User {
        buildUpdatedUser()
    }

    private var hasPendingChanges: Bool {
        managedUserSettingsSignature(for: draftUser) != managedUserSettingsSignature(for: user)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingPill) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(user.username)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(user.email)
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                VStack(alignment: .trailing, spacing: SkydownLayout.stackSpacingDense) {
                    SettingsBadge(text: draftRole.displayTitle, colorScheme: colorScheme)
                    if isCurrentUser {
                        SettingsBadge(text: "Du", colorScheme: colorScheme)
                    }
                    if isSaving {
                        SettingsBadge(text: "Speichert...", colorScheme: colorScheme)
                    } else if hasPendingChanges {
                        SettingsBadge(text: "Entwurf", colorScheme: colorScheme)
                    } else if saveSuccessCount > 0 {
                        SettingsBadge(text: "Gespeichert", colorScheme: colorScheme)
                    }
                }
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                Text("Rolle")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Menu {
                    ForEach(UserRole.allCases, id: \.self) { role in
                        Button(role.displayTitle) {
                            draftRole = role
                        }
                        .disabled(isRoleSelectionDisabled(role))
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
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
                .skydownTactileAction()

                if user.isPlatformOwner {
                    Text("Owner fest: nash.lioncorna@gmail.com")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                } else if isCurrentUser {
                    Text("Eigenes Konto: Rolle geschützt. Limits anpassbar.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                } else if !canAssignOwnerRoleToUser {
                    Text("Owner nur Hauptkonto. KI: Admin + Freigabe.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            if draftRole == .owner {
                ownerControlNote
            } else {
                if draftRole == .admin {
                    adminCapabilitiesSection
                }
                quotaPlanSection
            }

            SettingsToggleCard(
                colorScheme: colorScheme,
                title: "KI fuer dieses Konto aktiv",
                subtitle: "Aus = Bot, Visuals, Agent gesperrt.",
                isOn: $aiAccessEnabled
            )

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                Text("Tageslimits")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))

                HStack(spacing: SkydownLayout.stackSpacingPill) {
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

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
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
                guard !isSaving, hasPendingChanges else { return }

                isSaving = true
                saveErrorMessage = nil
                let pendingUser = draftUser

                Task {
                    let result = await onSave(pendingUser)
                    await MainActor.run {
                        isSaving = false
                        switch result {
                        case .success:
                            saveSuccessCount += 1
                            saveErrorMessage = nil
                        case .failure(let error):
                            saveErrorMessage = error.localizedDescription
                        }
                    }
                }
            } label: {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    if isSaving {
                        ProgressView()
                            .tint(Color.white)
                    }

                    Label(
                        isSaving ? "Speichert..." : "Konto speichern",
                        systemImage: isSaving ? "hourglass" : "checkmark.circle.fill"
                    )
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .skydownInteractiveFeedback()
            .tint(AppColors.accent(for: colorScheme))
            .disabled(isSaving || !hasPendingChanges)

            if let saveErrorMessage {
                Text(saveErrorMessage)
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(.red)
            } else if isSaving {
                Text("Rolle, Rechte und KI-Limits werden gerade serverseitig synchronisiert.")
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else if hasPendingChanges {
                Text("Ungespeichert — Speichern für Live-Claims & Limits.")
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else if saveSuccessCount > 0 {
                Text("Gespeichert. Die letzte Aenderung wurde serverseitig bestaetigt.")
                    .font(.footnote)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
        .overlay(alignment: .topTrailing) {
            if isSaving {
                ProgressView()
                    .tint(AppColors.accent(for: colorScheme))
                    .padding(14)
            }
        }
        .onChange(of: draftRole) { _, newRole in
            let recommendedPlan = UserQuotaPlan.defaultPlan(for: newRole)
            switch newRole {
            case .owner:
                draftQuotaPlan = .ownerUnlimited
                canManageMusicCatalog = true
                canManageVideoCatalog = true
                canModerateProfiles = true
            case .admin:
                if draftQuotaPlan == .ownerUnlimited || draftQuotaPlan == .internalTeam || draftQuotaPlan == .free {
                    draftQuotaPlan = .creator
                }
                canManageMusicCatalog = user.canManageMusicCatalog
                canManageVideoCatalog = user.canManageVideoCatalog
                canModerateProfiles = user.canModerateProfiles
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
            guard draftRole == .admin || draftRole == .subadmin || draftRole == .user else { return }
            textLimitDraft = String(newPlan.aiTextRequestsPerDay)
            visualLimitDraft = String(newPlan.aiVisualRequestsPerDay)
            agentLimitDraft = String(newPlan.aiAgentRequestsPerDay)
            historyRetentionDays = newPlan.aiHistoryRetentionDays
        }
        .onChange(of: managedUserSettingsSignature(for: draftUser)) { _, newSignature in
            if newSignature != managedUserSettingsSignature(for: user) {
                saveErrorMessage = nil
            }
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
            finalQuotaPlan = draftQuotaPlan == .studio ? .studio : .creator
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

    private func managedUserSettingsSignature(for user: User) -> String {
        [
            user.resolvedRole.rawValue,
            user.resolvedQuotaPlan.rawValue,
            String(user.aiAccessEnabled),
            String(max(1, user.aiTextRequestsPerDay)),
            String(max(1, user.aiVisualRequestsPerDay)),
            String(max(1, user.aiAgentRequestsPerDay)),
            String(user.resolvedAIHistoryRetentionDays),
            String(user.canManageMusicCatalog),
            String(user.canManageVideoCatalog),
            String(user.canModerateProfiles),
        ].joined(separator: "|")
    }

    private var ownerControlNote: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text("Owner-Kontrolle")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Root: Shopify, Zahlungen, Rollen, KI — nur hier.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
    }

    private var adminCapabilitiesSection: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            Text((draftRole == .admin || draftRole == .subadmin) ? "Abo-Modell" : "Kontingent")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            if draftRole == .user {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
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
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
                .skydownTactileAction()

                Text(draftQuotaPlan.planSummary)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                if draftRole == .admin {
                    Text("Admin-KI bleibt auf Creator- und Studio-Kontingenten ausgerichtet.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
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
            return "Premium"
        case .user:
            return "User"
        }
    }

    var roleSummary: String {
        switch self {
        case .owner:
            return "Festes Hauptkonto der App. Fuer diese App ist nash.lioncorna@gmail.com immer der Owner. Root-Zugriff auf alles, inklusive Shopify, Zahlungen, Rollen, KI-Defaults und Recovery."
        case .admin:
            return "Staff-Konto mit zuweisbaren Funktionen fuer Music, Video und Profil-Moderation. Kein Zugriff auf Owner-Systembereiche. Fuer bezahlte KI-Plaene bleiben Creator und Studio die vorgesehenen Abo-Stufen."
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

private enum SettingsEditableImageTarget: String, Identifiable, Equatable {
    case homeHeader
    case musicHubHeader
    case shopHeader
    case videoHeader

    var id: String { rawValue }
}

private enum SettingsPresentedSheet: Identifiable, Equatable {
    case login(AuthEntryContext)
    case registration
    case orders
    case profileEditor
    case appGuide
    case termsAndConditions
    case privacyPolicy
    case termsOfService
    case subscriptionTerms
    case aiUsageNotice
    case imprintInfo
    case adminWorkspace(SettingsAdminWorkspaceSection)
    case mailComposer
    case editableImage(SettingsEditableImageTarget)

    var id: String {
        switch self {
        case .login(let context):
            return "login-\(context.rawValue)"
        case .registration:
            return "registration"
        case .orders:
            return "orders"
        case .profileEditor:
            return "profileEditor"
        case .appGuide:
            return "appGuide"
        case .termsAndConditions:
            return "termsAndConditions"
        case .privacyPolicy:
            return "privacyPolicy"
        case .termsOfService:
            return "termsOfService"
        case .subscriptionTerms:
            return "subscriptionTerms"
        case .aiUsageNotice:
            return "aiUsageNotice"
        case .imprintInfo:
            return "imprintInfo"
        case .adminWorkspace(let section):
            return "adminWorkspace-\(section.rawValue)"
        case .mailComposer:
            return "mailComposer"
        case .editableImage(let target):
            return "editableImage-\(target.rawValue)"
        }
    }
}

#Preview {
    SettingsView(colorScheme: .constant("system"))
        .environmentObject(AuthManager())
        .environment(\.colorScheme, .light)
}
