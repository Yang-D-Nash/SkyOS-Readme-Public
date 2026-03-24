//
//  cartViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import Foundation
import FirebaseFirestore

@MainActor
class CartViewModel: ObservableObject {
    @Published var items: [CartItem] = []
    @Published var userEmail: String = ""
    
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .success

    private let authManager: AuthManager
    private let firestore = Firestore.firestore()

    init(authManager: AuthManager) {
        self.authManager = authManager
        self.userEmail = authManager.userSession?.email ?? ""

        Task { [weak self] in
            for await user in authManager.$userSession.values {
                self?.userEmail = user?.email ?? ""
            }
        }
    }

    func addItem(_ item: MerchandiseItem, size: String, quantity: Int) {
        if let index = items.firstIndex(where: { $0.item.id == item.id && $0.size == size }) {
            items[index].quantity += quantity
        } else {
            items.append(CartItem(item: item, size: size, quantity: quantity))
        }
        showUserToast("\(item.name) x\(quantity) in Warenkorb gelegt", style: .success)
    }

    func removeItem(_ cartItem: CartItem) {
        items.removeAll { $0.id == cartItem.id }
    }

    func clearCart() {
        items.removeAll()
    }

    func submitCartAsOrder() async -> Bool {
        guard !items.isEmpty else {
            showUserToast("Warenkorb ist leer.", style: .error)
            return false
        }
        guard let email = authManager.userSession?.email else {
            showUserToast("Benutzer nicht angemeldet.", style: .error)
            return false
        }

        let orderItems = items.map { [
            "name": $0.item.name,
            "quantity": $0.quantity,
            "size": $0.size]
        }

        let orderData: [String: Any] = [
            "userEmail": email,
            "items": orderItems,
            "isCompleted": false,
            "timestamp": Timestamp()
        ]

        do {
            try await firestore.collection("orders").addDocument(data: orderData)
            clearCart()
            showUserToast("Bestellung erfolgreich abgeschickt!", style: .success)
            return true
        } catch {
            print("Dev Fehler submitCartAsOrder:", error.localizedDescription)
            showUserToast("Fehler beim Absenden der Bestellung.", style: .error)
            return false
        }
    }

    func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}
