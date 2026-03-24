//
//  CartView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import SwiftUI

struct CartView: View {
    @EnvironmentObject var cartVM: CartViewModel
    @EnvironmentObject var authManager: AuthManager

    @State private var name = ""
    @State private var email = ""
    @State private var whatsApp = ""
    @State private var message = "Ich interessiere mich für die Artikel in meinem Warenkorb."
    @State private var showConfirmationDialog = false
    @State private var isSubmitting = false
    
    // Neu: Sheet-State für Login
    @State private var showingLoginSheet = false

    private var isFormValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        !email.trimmingCharacters(in: .whitespaces).isEmpty &&
        !cartVM.items.isEmpty
    }

    var body: some View {
        NavigationStack {
            Group {
                if authManager.userSession == nil {
                    VStack(spacing: 20) {
                        Text("Du bist nicht angemeldet.")
                            .font(.headline)
                        
                        Button {
                            showingLoginSheet = true
                        } label: {
                            Label("Anmelden", systemImage: "person.crop.circle.fill.badge.plus")
                                .foregroundColor(.white)
                                .padding()
                                .frame(maxWidth: .infinity)
                                .background(Color.accentColor)
                                .cornerRadius(12)
                        }
                        .padding(.horizontal)
                    }
                } else {
                    Form {
                        CartItemSectionView(cartVM: cartVM)
                        ContactSectionView(name: $name, email: $email, whatsApp: $whatsApp)
                        MessageSectionView(message: $message)
                        SubmitSectionView(
                            isFormValid: isFormValid,
                            isSubmitting: $isSubmitting,
                            showConfirmationDialog: $showConfirmationDialog
                        ) {
                            await submitOrderAsync()
                        }
                    }
                    .onAppear {
                        if let user = authManager.userSession {
                            name = user.username
                            email = user.email
                            whatsApp = user.whatsApp ?? ""
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $showingLoginSheet) {
            LoginView()
        }
        .fancyToast(isPresented: $cartVM.showToast,
                    message: cartVM.toastMessage,
                    style: cartVM.toastStyle)
    }

    private func submitOrderAsync() async {
        guard !isSubmitting else { return }
        isSubmitting = true

        _ = await cartVM.submitCartAsOrder()

        isSubmitting = false
    }
}

#Preview {
    let authManager = AuthManager()
    let cartVM = CartViewModel(authManager: authManager)

    CartView()
        .environmentObject(authManager)
        .environmentObject(cartVM)
}
