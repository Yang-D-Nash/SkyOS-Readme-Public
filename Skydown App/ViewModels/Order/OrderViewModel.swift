//
//  OrderViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import Foundation

@MainActor
class OrderViewModel: ObservableObject {
    @Published var orders: [Order] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let orderService: OrderServicing
    private var stopObservingOrders: (() -> Void)?

    init(orderService: OrderServicing = FirebaseOrderService()) {
        self.orderService = orderService
        fetchOrders()
    }

    func fetchOrders() {
        isLoading = true
        stopObservingOrders?()

        stopObservingOrders = orderService.observeOrders { [weak self] result in
            guard let self else { return }
            self.isLoading = false

            switch result {
            case .success(let orders):
                self.orders = orders
            case .failure(let error):
                print("Dev Fehler fetchOrders:", error.localizedDescription)
                self.showUserToast("Fehler beim Laden: \(error.localizedDescription)", style: .error)
                self.orders = []
            }
        }
    }

    func toggleCompleted(for order: Order) async {
        guard let id = order.id else { return }

        do {
            try await orderService.toggleCompleted(orderID: id, isCompleted: order.isCompleted)
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
            try await orderService.deleteOrder(orderID: id)
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
        stopObservingOrders?()
    }
}
