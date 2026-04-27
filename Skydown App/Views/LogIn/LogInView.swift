//
//  LogInView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//
import SwiftUI

struct LoginView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var authManager: AuthManager
    @StateObject private var viewModel = LoginViewModel()
    @State private var showingRegistrationSheet = false
    @Environment(\.colorScheme) private var colorScheme

    var entryContext: AuthEntryContext

    private func localized(_ key: String, _ fallback: String) -> String {
        AppLocalized.text(key, fallback: fallback)
    }

    init(entryContext: AuthEntryContext = .standard) {
        self.entryContext = entryContext
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: SkydownLayout.sectionSpacing) {
                Spacer()
                VStack(spacing: SkydownLayout.stackSpacingSubtle) {
                    Text(localized("auth.login.welcome", "Welcome to SkyOS"))
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(
                        localized(
                            entryContext.loginSubtitleLocalization.key,
                            entryContext.loginSubtitleLocalization.fallback
                        )
                    )
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                    Text(localized("feature.status.live.title", "Live now:"))
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                    Text(localized("feature.status.live.body", "Reminders with push, plus saved tasks and notes in the productivity dock."))
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(localized("feature.status.next.title", "Coming next:"))
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                    Text(localized("feature.status.next.body", "Longer-lived profile memory and smarter follow-up automations."))
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(SkydownLayout.cardPadding)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .cornerRadius(SkydownLayout.compactRadius)
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)

                VStack(spacing: SkydownLayout.stackSpacingLoft) {
                    TextField(localized("auth.email", "Email"), text: $viewModel.email)
                        .padding()
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .cornerRadius(SkydownLayout.cardCornerRadius)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .accessibilityIdentifier("login.email")

                    SecureField(localized("auth.password", "Password"), text: $viewModel.password)
                        .padding()
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .cornerRadius(SkydownLayout.cardCornerRadius)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .accessibilityIdentifier("login.password")
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)

                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        .multilineTextAlignment(.center)
                        .accessibilityIdentifier("login.error")
                }

                Button {
                    Task { await signInAndHydrateSession() }
                } label: {
                    if viewModel.isLoading {
                        ProgressView().progressViewStyle(.circular).tint(.white)
                    } else {
                        Text(localized("auth.sign_in", "Sign in")).font(.headline)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(AppColors.accent(for: colorScheme))
                .cornerRadius(SkydownLayout.cardCornerRadius)
                .foregroundColor(.white)
                .opacity(viewModel.isSignInButtonDisabled ? 0.6 : 1.0)
                .disabled(viewModel.isSignInButtonDisabled)
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .accessibilityIdentifier("login.submit")

                Button {
                    Task { await signInWithGoogleAndHydrateSession() }
                } label: {
                    HStack(spacing: SkydownLayout.stackSpacingCompact) {
                        Image(systemName: "globe")
                        Text(localized("auth.sign_in_google", "Sign in with Google"))
                            .font(.headline)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .cornerRadius(SkydownLayout.cardCornerRadius)
                    .foregroundColor(AppColors.text(for: colorScheme))
                }
                .disabled(viewModel.isLoading)
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .accessibilityIdentifier("login.google")

                Button {
                    showingRegistrationSheet = true
                } label: {
                    Text(localized("auth.no_account_register", "No account yet? Register"))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
                .accessibilityIdentifier("login.open_registration")
                .sheet(isPresented: $showingRegistrationSheet) {
                    RegistrationSheet()
                }

                Spacer()
            }
            .padding()
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
            .navigationTitle(localized("auth.login.title", "Sign in"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .onChange(of: viewModel.isAuthenticated) { _, newValue in
                if newValue { dismiss() }
            }
        }
        .fancyToast(isPresented: $viewModel.showToast,
                    message: viewModel.toastMessage,
                    style: viewModel.toastStyle)
        .accessibilityIdentifier("login.root")
    }

    private func signInAndHydrateSession() async {
        await viewModel.signIn()
        guard viewModel.isAuthenticated else { return }
        _ = await authManager.refreshCurrentUser()
    }

    private func signInWithGoogleAndHydrateSession() async {
        await viewModel.signInWithGoogle()
        guard viewModel.isAuthenticated else { return }
        _ = await authManager.refreshCurrentUser()
    }
}

#Preview {
    LoginView(entryContext: .standard)
}
