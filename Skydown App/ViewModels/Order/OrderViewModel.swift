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
    @Published private(set) var confirmingPaymentOrderIDs: Set<String> = []

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
        errorMessage = nil
        stopObservingOrders?()

        stopObservingOrders = orderService.observeOrders { [weak self] result in
            guard let self else { return }
            self.isLoading = false

            switch result {
            case .success(let orders):
                self.errorMessage = nil
                self.orders = orders
            case .failure(let error):
                skydownDebugLog("Dev Fehler fetchOrders:", error.localizedDescription)
                self.orders = []
                self.errorMessage = Self.userFacingListLoadError(error)
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
            skydownDebugLog("Dev Fehler toggleCompleted:", error.localizedDescription)
            showUserToast(
                "Status konnte nicht aktualisiert werden. Bitte spaeter erneut.",
                style: .error
            )
        }
    }

    func deleteOrder(_ order: Order) async {
        guard let id = order.id else { return }

        do {
            try await orderService.deleteOrder(orderID: id)
            showUserToast("Bestellung entfernt.", style: .success)
        } catch {
            skydownDebugLog("Dev Fehler deleteOrder:", error.localizedDescription)
            showUserToast(
                "Bestellung konnte nicht entfernt werden. Bitte spaeter erneut.",
                style: .error
            )
        }
    }

    func confirmPayment(for order: Order) async {
        guard let id = order.id else { return }
        guard !Self.hasFinalPaymentStatus(order.paymentStatus) else {
            showUserToast("Diese Bestellung ist bereits als bezahlt markiert.", style: .info)
            return
        }

        confirmingPaymentOrderIDs.insert(id)
        defer { confirmingPaymentOrderIDs.remove(id) }

        do {
            try await orderService.confirmPayment(
                orderID: id,
                paymentMethod: order.paymentMethod,
                paymentReference: nil
            )
            showUserToast("Zahlung bestaetigt. Der naechste Versandschritt folgt jetzt.", style: .success)
        } catch {
            skydownDebugLog("Dev Fehler confirmPayment:", error.localizedDescription)
            showUserToast(
                "Zahlung konnte gerade nicht bestaetigt werden. Bitte in einem Moment erneut versuchen.",
                style: .error
            )
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private static func userFacingListLoadError(_ error: Error) -> String {
        let ns = error as NSError
        if ns.domain == NSURLErrorDomain {
            switch ns.code {
            case NSURLErrorNotConnectedToInternet, NSURLErrorNetworkConnectionLost, NSURLErrorTimedOut, -1009:
                return "Keine Verbindung. Bestellungen koennen gerade nicht geladen werden."
            default:
                break
            }
        }
        return "Bestellungen konnten gerade nicht geladen werden. Bitte spaeter erneut."
    }

    private static func hasFinalPaymentStatus(_ status: String?) -> Bool {
        guard let normalized = status?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), !normalized.isEmpty else {
            return false
        }
        return ["confirmed", "paid", "success", "succeeded"].contains(normalized)
    }

    deinit {
        stopObservingOrders?()
    }
}
