//
//  ContactFormViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import Foundation

@MainActor
class ContactFormViewModel: ObservableObject {
    @Published var selectedSize = "M"
    @Published var selectedQuantity = 1
    @Published var name = ""
    @Published var email = ""
    @Published var whatsAppNumber = ""
    @Published var message = ""

    let item: MerchandiseItem
    private let cartVM: CartViewModel

    init(cartVM: CartViewModel, authManager: AuthManager, item: MerchandiseItem) {
        self.cartVM = cartVM
        self.item = item

        if let user = authManager.userSession {
            self.name = user.username
            self.email = user.email
            self.whatsAppNumber = user.whatsApp ?? ""
        }

        updateMessage()
    }

    var isFormValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        !email.trimmingCharacters(in: .whitespaces).isEmpty
    }

    func updateMessage() {
        message = "Hallo, ich bin an '\(item.name)' in Größe \(selectedSize) x\(selectedQuantity) interessiert."
    }

    func addToCart() {
        guard isFormValid else {
            cartVM.showUserToast("Bitte Name und E-Mail ausfüllen.", style: .error)
            return
        }

        cartVM.addItem(item, size: selectedSize, quantity: selectedQuantity)
    }
}
