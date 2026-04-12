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
    @Environment(\.colorScheme) private var colorScheme

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

                    Text(localized("auth.register.google_hint", "Your account is created automatically on first Google login."))
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .listRowBackground(AppColors.primaryBackground(for: colorScheme))

                    Text(localized("auth.register.username_hint", "If you enter a username above, it is used for your profile."))
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
            .scrollContentBackground(.hidden)
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
        }
        .fancyToast(isPresented: $viewModel.showToast,
                    message: viewModel.toastMessage,
                    style: viewModel.toastStyle)
    }
}

#Preview {
    RegistrationSheet()
}
