//
//  LogInView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//
import SwiftUI

struct LoginView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = LoginViewModel()
    @State private var showingRegistrationSheet = false
    @Environment(\.colorScheme) private var colorScheme

    private func localized(_ key: String, _ fallback: String) -> String {
        AppLocalized.text(key, fallback: fallback)
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Spacer()
                VStack(spacing: 5) {
                    Text(localized("auth.login.welcome", "Welcome to SkyOS"))
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(localized("auth.login.subtitle", "Sign in to access your account and creator tools."))
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                VStack(spacing: 15) {
                    TextField(localized("auth.email", "Email"), text: $viewModel.email)
                        .padding()
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .cornerRadius(10)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    
                    SecureField(localized("auth.password", "Password"), text: $viewModel.password)
                        .padding()
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .cornerRadius(10)
                        .foregroundColor(AppColors.text(for: colorScheme))
                }
                .padding(.horizontal)
                
                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .padding(.horizontal)
                        .multilineTextAlignment(.center)
                }
                
                Button {
                    Task { await viewModel.signIn() }
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
                .cornerRadius(10)
                .foregroundColor(.white)
                .opacity(viewModel.isSignInButtonDisabled ? 0.6 : 1.0)
                .disabled(viewModel.isSignInButtonDisabled)
                .padding(.horizontal)

                Button {
                    Task { await viewModel.signInWithGoogle() }
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "globe")
                        Text(localized("auth.sign_in_google", "Sign in with Google"))
                            .font(.headline)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .cornerRadius(10)
                    .foregroundColor(AppColors.text(for: colorScheme))
                }
                .disabled(viewModel.isLoading)
                .padding(.horizontal)
                
                Button {
                    showingRegistrationSheet = true
                } label: {
                    Text(localized("auth.no_account_register", "No account yet? Register"))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
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
    }
}

#Preview {
    LoginView()
}
