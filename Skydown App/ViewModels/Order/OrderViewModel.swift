//
//  OrderViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import Foundation
import FirebaseFirestore
import FirebaseAuth

@MainActor
class OrderViewModel: ObservableObject {
    @Published var orders: [Order] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info

    private var firestore = Firestore.firestore()
    private var listener: ListenerRegistration?

    init() {
        fetchOrders()
    }

    func fetchOrders() {
        isLoading = true
        listener?.remove()

        listener = firestore.collection("orders")
            .order(by: "timestamp", descending: true)
            .addSnapshotListener { [weak self] snapshot, error in
                guard let self = self else { return }
                self.isLoading = false

                if let error = error {
                    print("Dev Fehler fetchOrders:", error.localizedDescription)
                    self.showUserToast("Fehler beim Laden: \(error.localizedDescription)", style: .error)
                    self.orders = []
                    return
                }

                self.orders = snapshot?.documents.compactMap { doc in
                    try? doc.data(as: Order.self)
                } ?? []
            }
    }

    func toggleCompleted(for order: Order) async {
        guard let id = order.id else { return }

        do {
            try await firestore.collection("orders").document(id).updateData([
                "isCompleted": !order.isCompleted
            ])
            let msg = order.isCompleted ? "Markiert als offen" : "Markiert als erledigt"
            showUserToast(msg, style: .success)
        } catch {
            print("Dev Fehler toggleCompleted:", error.localizedDescription)
            let msg = "Update fehlgeschlagen: \(error.localizedDescription)"
            errorMessage = msg
            showUserToast(msg, style: .error)
        }
    }

    func deleteOrder(_ order: Order) async {
        guard let id = order.id else { return }

        do {
            try await firestore.collection("orders").document(id).delete()
            showUserToast("Bestellung gelöscht", style: .success)
        } catch {
            print("Dev Fehler deleteOrder:", error.localizedDescription)
            let msg = "Löschen fehlgeschlagen: \(error.localizedDescription)"
            errorMessage = msg
            showUserToast(msg, style: .error)
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    deinit {
        listener?.remove()
    }
}
