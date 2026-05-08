//
//  RegistrationView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI

struct RegistrationSheet: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var viewModel = RegistrationViewModel()
    @ObservedObject private var legalContentStore = LegalContentStore.shared
    @Environment(\.colorScheme) private var colorScheme
    @State private var activeLegalDocument: RegistrationLegalDocument?
    @State private var hasTrackedSignupStart = false
    private let growthTracker = MembershipAnalyticsTracker()

    private func localized(_ key: String, _ fallback: String) -> String {
        AppLocalized.text(key, fallback: fallback)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                    SkydownRegistrationPanel(
                        title: localized("auth.register.section", "Create account"),
                        colorScheme: colorScheme,
                        accent: AppColors.accentMystic(for: colorScheme)
                    ) {
                        SkydownPremiumTextInput(
                            title: localized("auth.username", "Username"),
                            text: $viewModel.username,
                            colorScheme: colorScheme,
                            systemImage: "person.fill",
                            accent: AppColors.accentMystic(for: colorScheme),
                            autocapitalization: .never
                        )

                        SkydownPremiumTextInput(
                            title: localized("auth.email", "Email"),
                            text: $viewModel.email,
                            colorScheme: colorScheme,
                            systemImage: "envelope.fill",
                            accent: AppColors.accentMystic(for: colorScheme),
                            keyboardType: .emailAddress,
                            autocapitalization: .never
                        )

                        SkydownPremiumTextInput(
                            title: localized("auth.password", "Password"),
                            text: $viewModel.password,
                            colorScheme: colorScheme,
                            systemImage: "lock.fill",
                            accent: AppColors.accentMystic(for: colorScheme),
                            isSecure: true,
                            autocapitalization: .never
                        )

                        SkydownPremiumTextInput(
                            title: localized("auth.confirm_password", "Confirm password"),
                            text: $viewModel.confirmPassword,
                            colorScheme: colorScheme,
                            systemImage: "checkmark.shield.fill",
                            accent: AppColors.accentMystic(for: colorScheme),
                            isSecure: true,
                            autocapitalization: .never
                        )
                    }

                    SkydownRegistrationPanel(
                        title: localized("auth.register.legal.header", "Legal consent"),
                        colorScheme: colorScheme,
                        accent: AppColors.accent(for: colorScheme)
                    ) {
                        Text(localized("auth.register.legal.version", "Legal version: \(legalContentStore.settings.resolvedLastUpdatedLabel)"))
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        Toggle(isOn: $viewModel.acceptedTerms) {
                            Text(localized("auth.register.legal.terms", "I accept the Terms (AGB)."))
                        }
                        .toggleStyle(SkydownPremiumToggleStyle(colorScheme: colorScheme))

                        Toggle(isOn: $viewModel.acceptedPrivacyPolicy) {
                            Text(localized("auth.register.legal.privacy", "I accept the Privacy Policy."))
                        }
                        .toggleStyle(SkydownPremiumToggleStyle(colorScheme: colorScheme))

                        Toggle(isOn: $viewModel.aiConsentEnabled) {
                            Text(localized("auth.register.legal.ai", "Enable AI (change anytime in Settings)."))
                        }
                        .toggleStyle(SkydownPremiumToggleStyle(colorScheme: colorScheme))

                        VStack(spacing: SkydownLayout.stackSpacingCompact) {
                            SkydownBrandActionButton(
                                title: localized("auth.register.legal.open_terms", "Open AGB"),
                                accent: AppColors.accent(for: colorScheme),
                                colorScheme: colorScheme,
                                role: .muted,
                                font: .subheadline.weight(.semibold),
                                cornerRadius: SkydownLayout.denseRadius,
                                verticalPadding: 10,
                                expandToFullWidth: true,
                                action: { activeLegalDocument = .terms }
                            )
                            .skydownInteractiveFeedback()

                            SkydownBrandActionButton(
                                title: localized("auth.register.legal.open_privacy", "Open Privacy"),
                                accent: AppColors.accent(for: colorScheme),
                                colorScheme: colorScheme,
                                role: .muted,
                                font: .subheadline.weight(.semibold),
                                cornerRadius: SkydownLayout.denseRadius,
                                verticalPadding: 10,
                                expandToFullWidth: true,
                                action: { activeLegalDocument = .privacy }
                            )
                            .skydownInteractiveFeedback()
                        }
                    }

                    if let errorMessage = viewModel.errorMessage {
                        Text(errorMessage)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(AppColors.error(for: colorScheme))
                            .padding(SkydownLayout.inlineSurfacePadding)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(AppColors.error(for: colorScheme).opacity(colorScheme == .dark ? 0.16 : 0.09))
                            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
                    }

                    SkydownBrandActionButton(
                        title: localized("auth.register", "Register"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        isEnabled: !viewModel.isRegistrationButtonDisabled,
                        isLoading: viewModel.isLoading,
                        action: {
                            Task {
                                if await viewModel.registerUser() {
                                    dismiss()
                                }
                            }
                        }
                    )
                    .skydownInteractiveFeedback()

                    SkydownRegistrationPanel(
                        title: nil,
                        colorScheme: colorScheme,
                        accent: AppColors.accentMystic(for: colorScheme)
                    ) {
                        SkydownBrandActionButton(
                            title: localized("auth.register_google", "Register with Google"),
                            systemImage: "globe",
                            accent: AppColors.accentMystic(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            isEnabled: !viewModel.isLoading,
                            action: {
                                Task {
                                    if await viewModel.signInWithGoogle() {
                                        dismiss()
                                    }
                                }
                            }
                        )
                        .skydownInteractiveFeedback()

                        Text(localized("auth.register.google_hint", "Google: account on first sign-in."))
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        Text(localized("auth.register.username_hint", "Username above → your profile."))
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.vertical, SkydownLayout.sectionSpacing)
            }
            .navigationTitle(localized("auth.register.title", "New account"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    SkydownBrandActionButton(
                        title: localized("common.cancel", "Cancel"),
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
            }
            .skydownKeyboardDismissToolbar()
            .scrollDismissesKeyboard(.interactively)
            .skydownDismissKeyboardOnTap()
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
        }
        .onChange(of: legalContentStore.settings.resolvedLastUpdatedLabel) { _, newValue in
            viewModel.legalVersionLabel = newValue
        }
        .onAppear {
            guard !hasTrackedSignupStart else { return }
            hasTrackedSignupStart = true
            growthTracker.track("signup_start", surface: "registration_sheet")
        }
        .task {
            viewModel.legalVersionLabel = legalContentStore.settings.resolvedLastUpdatedLabel
        }
        .sheet(item: $activeLegalDocument) { document in
            switch document {
            case .terms:
                PolicyView(
                    title: AppLocalized.text("policy.title.agb", fallback: "Terms & Conditions (AGB)"),
                    text: legalContentStore.settings.termsAndConditionsText,
                    lastUpdated: legalContentStore.settings.resolvedLastUpdatedLabel,
                    supportEmail: legalContentStore.settings.resolvedSupportEmail
                )
            case .privacy:
                PolicyView(
                    title: AppLocalized.text("policy.title.privacy", fallback: "Privacy policy"),
                    text: legalContentStore.settings.privacyPolicyText,
                    lastUpdated: legalContentStore.settings.resolvedLastUpdatedLabel,
                    supportEmail: legalContentStore.settings.resolvedSupportEmail
                )
            }
        }
        .fancyToast(isPresented: $viewModel.showToast,
                    message: viewModel.toastMessage,
                    style: viewModel.toastStyle)
    }
}

private enum RegistrationLegalDocument: String, Identifiable {
    case terms
    case privacy

    var id: String { rawValue }
}

private struct SkydownRegistrationPanel<Content: View>: View {
    let title: String?
    let colorScheme: ColorScheme
    let accent: Color
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            if let title, !title.isEmpty {
                Text(title)
                    .font(AppTypography.sectionHeadline)
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            content
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: accent,
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 9,
            shadowYOffset: 4
        )
    }
}

#Preview {
    RegistrationSheet()
}
