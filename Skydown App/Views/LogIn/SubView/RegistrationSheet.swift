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
            Form {
                Section(header: Text(localized("auth.register.section", "Create account"))) {
                    TextField(localized("auth.username", "Username"), text: $viewModel.username)
                        .autocapitalization(.none)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    TextField(localized("auth.email", "Email"), text: $viewModel.email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    SecureField(localized("auth.password", "Password"), text: $viewModel.password)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    SecureField(localized("auth.confirm_password", "Confirm password"), text: $viewModel.confirmPassword)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .listRowBackground(AppColors.secondaryBackground(for: colorScheme))
                }

                Section(header: Text(localized("auth.register.legal.header", "Legal consent"))) {
                    Text(localized("auth.register.legal.version", "Legal version: \(legalContentStore.settings.resolvedLastUpdatedLabel)"))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .listRowBackground(AppColors.primaryBackground(for: colorScheme))

                    Toggle(isOn: $viewModel.acceptedTerms) {
                        Text(localized("auth.register.legal.terms", "I accept the Terms (AGB)."))
                    }
                    .toggleStyle(SwitchToggleStyle(tint: AppColors.accent(for: colorScheme)))
                    .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    Toggle(isOn: $viewModel.acceptedPrivacyPolicy) {
                        Text(localized("auth.register.legal.privacy", "I accept the Privacy Policy."))
                    }
                    .toggleStyle(SwitchToggleStyle(tint: AppColors.accent(for: colorScheme)))
                    .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    Toggle(isOn: $viewModel.aiConsentEnabled) {
                        Text(localized("auth.register.legal.ai", "Enable AI (change anytime in Settings)."))
                    }
                    .toggleStyle(SwitchToggleStyle(tint: AppColors.accent(for: colorScheme)))
                    .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    HStack(spacing: SkydownLayout.stackSpacingCompact) {
                        Button(localized("auth.register.legal.open_terms", "Open AGB")) {
                            activeLegalDocument = .terms
                        }
                        .buttonStyle(.bordered)

                        Button(localized("auth.register.legal.open_privacy", "Open Privacy")) {
                            activeLegalDocument = .privacy
                        }
                        .buttonStyle(.bordered)
                    }
                    .listRowBackground(AppColors.primaryBackground(for: colorScheme))
                }

                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .listRowBackground(Color.clear)
                }

                Button {
                    Task {
                        if await viewModel.registerUser() {
                            dismiss()
                        }
                    }
                } label: {
                    if viewModel.isLoading {
                        ProgressView()
                    } else {
                        Text(localized("auth.register", "Register"))
                    }
                }
                .disabled(viewModel.isRegistrationButtonDisabled)
                .listRowBackground(AppColors.primaryBackground(for: colorScheme))

                Section {
                    Button {
                        Task {
                            if await viewModel.signInWithGoogle() {
                                dismiss()
                            }
                        }
                    } label: {
                        HStack {
                            Spacer()
                            Label(localized("auth.register_google", "Register with Google"), systemImage: "globe")
                                .font(.headline)
                            Spacer()
                        }
                    }
                    .disabled(viewModel.isLoading)
                    .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    Text(localized("auth.register.google_hint", "Google: account on first sign-in."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .listRowBackground(AppColors.primaryBackground(for: colorScheme))

                    Text(localized("auth.register.username_hint", "Username above → your profile."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .listRowBackground(AppColors.primaryBackground(for: colorScheme))
                }
            }
            .navigationTitle(localized("auth.register.title", "New account"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(localized("common.cancel", "Cancel")) { dismiss() }
                }
            }
            .skydownKeyboardDismissToolbar()
            .scrollDismissesKeyboard(.interactively)
            .skydownDismissKeyboardOnTap()
            .scrollContentBackground(.hidden)
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

#Preview {
    RegistrationSheet()
}
