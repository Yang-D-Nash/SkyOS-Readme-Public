//
//  ContactFormView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI

struct ContactFormView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var cartVM: CartViewModel
    @EnvironmentObject var authManager: AuthManager
    let item: MerchandiseItem
    
    @Environment(\.colorScheme) private var colorScheme
    @State private var alertType: AlertType?

    @State private var selectedSize = "M"
    @State private var selectedQuantity = 1
    private let availableSizes = ["XS", "S", "M", "L", "XL"]
    private let availableQuantities = Array(1...10)
    
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success

    private enum AlertType: Identifiable {
        case confirm
        var id: Int { hashValue }
    }

    private var isFormValid: Bool { !selectedSize.isEmpty && selectedQuantity > 0 }
    
    private func addToCart() {
        cartVM.addItem(item, size: selectedSize, quantity: selectedQuantity)
        toastMessage = "Artikel erfolgreich zum Warenkorb hinzugefügt"
        toastStyle = .success
        showToast = true
        alertType = nil
        dismiss()
    }
    
    var body: some View {
        Form {
            Section {
                MerchandiseItemView(item: item)
                    .listRowBackground(AppColors.cardBackground(for: colorScheme))
            } header: {
                Text("Artikel auswählen")
            }
            
            Section {
                Picker("Größe", selection: $selectedSize) {
                    ForEach(availableSizes, id: \.self) { Text($0) }
                }
                
                Picker("Anzahl", selection: $selectedQuantity) {
                    ForEach(availableQuantities, id: \.self) { Text("\($0)") }
                }
            } header: {
                Text("Deine Bestellung")
            }
            
            Section {
                Button(authManager.userSession != nil ? "In Warenkorb legen" : "Account erforderlich") {
                    if authManager.userSession != nil {
                        alertType = .confirm
                    }
                }
                .disabled(!isFormValid || authManager.userSession == nil)
                .frame(maxWidth: .infinity)
                .padding()
                .background((isFormValid && authManager.userSession != nil) ? AppColors.accent(for: colorScheme) : .gray)
                .foregroundColor(.white)
                .cornerRadius(10)
            }
        }
        .navigationTitle("Kontaktformular")
        .navigationBarTitleDisplayMode(.inline)
        .alert(item: $alertType) { _ in
            Alert(
                title: Text("Artikel in Warenkorb legen"),
                message: Text("Der Artikel wird deinem Warenkorb hinzugefügt."),
                primaryButton: .default(Text("Einverstanden"), action: addToCart),
                secondaryButton: .cancel()
            )
        }
        .fancyToast(isPresented: $showToast, message: toastMessage, style: toastStyle)
    }
}
#Preview {
    let authManager = AuthManager()
    let cartVM = CartViewModel(authManager: authManager)
    
    NavigationStack {
        let sampleItem = MerchandiseItem(
            id: "1",
            name: "Skydown Shirt",
            price: 29.99,
            description: "Exklusives T-Shirt in limitierter Auflage",
            imageURLs: ["https://via.placeholder.com/150"],
            available: true
        )
        ContactFormView(item: sampleItem)
            .environmentObject(authManager)
            .environmentObject(cartVM)
    }
}
