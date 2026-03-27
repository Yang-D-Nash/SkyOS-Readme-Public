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

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Konto erstellen")) {
                    TextField("Benutzername", text: $viewModel.username)
                        .autocapitalization(.none)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    TextField("E-Mail-Adresse", text: $viewModel.email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    SecureField("Passwort", text: $viewModel.password)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    SecureField("Passwort bestätigen", text: $viewModel.confirmPassword)
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
                        Text("Registrieren")
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
                            Label("Mit Google registrieren", systemImage: "globe")
                                .font(.headline)
                            Spacer()
                        }
                    }
                    .disabled(viewModel.isLoading)
                    .listRowBackground(AppColors.secondaryBackground(for: colorScheme))

                    Text("Beim ersten Google-Login wird dein Konto automatisch angelegt.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .listRowBackground(AppColors.primaryBackground(for: colorScheme))

                    Text("Wenn du oben einen Benutzernamen einträgst, wird er für dein Profil übernommen.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .listRowBackground(AppColors.primaryBackground(for: colorScheme))
                }
            }
            .navigationTitle("Neues Konto")
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Abbrechen") { dismiss() }
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
