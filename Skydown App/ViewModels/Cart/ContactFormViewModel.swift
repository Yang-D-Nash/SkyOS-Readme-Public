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
    @Published var selectedColor = ""
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
        let colorPart = selectedColor.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "" : " in \(selectedColor)"
        message = "Hallo, ich bin an '\(item.name)' in Größe \(selectedSize)\(colorPart) x\(selectedQuantity) interessiert."
    }

    func addToCart() {
        guard isFormValid else {
            cartVM.showUserToast("Bitte Name und E-Mail ausfüllen.", style: .error)
            return
        }

        let variant = try? MerchandiseVariantResolver.resolveVariant(for: item, size: selectedSize, color: selectedColor)
        cartVM.addItem(
            item,
            size: selectedSize,
            color: selectedColor.trimmedNonEmpty,
            quantity: selectedQuantity,
            resolvedVariant: variant
        )
    }
}

private extension String {
    var trimmedNonEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
