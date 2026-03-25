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
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Spacer()
                VStack(spacing: 5) {
                    Text("Willkommen bei Skydown")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("Melden Sie sich an, um exklusive Inhalte zu sehen.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                VStack(spacing: 15) {
                    TextField("E-Mail-Adresse", text: $viewModel.email)
                        .padding()
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .cornerRadius(10)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    
                    SecureField("Passwort", text: $viewModel.password)
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
                        Text("Anmelden").font(.headline)
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
                        Text("Mit Google anmelden")
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
                    Text("Noch kein Konto? Registrieren")
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
                .sheet(isPresented: $showingRegistrationSheet) {
                    RegistrationSheet()
                }

                Spacer()
            }
            .padding()
            .background(AppColors.primaryBackground(for: colorScheme))
            .navigationTitle("Anmelden")
            .navigationBarTitleDisplayMode(.inline)
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
