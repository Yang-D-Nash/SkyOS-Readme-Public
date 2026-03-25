//
//  MerchandiseViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import Foundation

@MainActor
final class MerchandiseViewModel: ObservableObject {
    @Published var merchandiseItems: [MerchandiseItem] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    // Toast
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let merchandiseService: MerchandiseServicing
    private let authManager: AuthManager
    private var stopObservingItems: (() -> Void)?

    private var canManageMerchandise: Bool {
        authManager.userSession?.isAdmin == true
    }

    init(
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService(),
        authManager: AuthManager
    ) {
        self.merchandiseService = merchandiseService
        self.authManager = authManager
    }

    func fetchData() {
        isLoading = true
        errorMessage = nil

        stopObservingItems?()

        stopObservingItems = merchandiseService.observeItems { [weak self] result in
            guard let self else { return }
            self.isLoading = false

            switch result {
            case .success(let items):
                self.merchandiseItems = items
                if items.isEmpty {
                    self.showUserToast("Keine Artikel gefunden.", style: .error)
                }
            case .failure(let error):
                print("Dev Fehler fetchData:", error.localizedDescription)
                self.showUserToast("Fehler beim Laden der Artikel: \(error.localizedDescription)", style: .error)
                self.merchandiseItems = []
            }
        }
    }

    func addMerchandise(_ item: MerchandiseItem, imageDataList: [Data]) async -> Bool {
        guard canManageMerchandise else {
            showUserToast("Nur Admins dürfen Artikel hinzufügen.", style: .error)
            return false
        }

        do {
            let imageURLs = try await merchandiseService.uploadImages(imageDataList)
            var itemToSave = item
            itemToSave.imageURLs = imageURLs

            try await merchandiseService.addItem(itemToSave)
            showUserToast("Artikel hinzugefügt: \(item.name)", style: .success)
            return true
        } catch {
            print("Dev Fehler addMerchandise:", error.localizedDescription)
            showUserToast("Fehler beim Hinzufügen des Artikels: \(error.localizedDescription)", style: .error)
            return false
        }
    }

    func updateMerchandisePrice(_ item: MerchandiseItem, newPrice: Double) async -> Bool {
        guard canManageMerchandise else {
            showUserToast("Nur Admins dürfen Artikel bearbeiten.", style: .error)
            return false
        }

        guard let id = item.id else {
            showUserToast("Artikel hat keine gültige ID.", style: .error)
            return false
        }

        do {
            try await merchandiseService.updatePrice(itemID: id, newPrice: newPrice)
            showUserToast("Preis aktualisiert: \(item.name)", style: .success)
            return true
        } catch {
            print("Dev Fehler updateMerchandisePrice:", error.localizedDescription)
            showUserToast("Update fehlgeschlagen: \(error.localizedDescription)", style: .error)
            return false
        }
    }

    func updateMerchandise(_ item: MerchandiseItem, imageDataList: [Data]) async -> Bool {
        guard canManageMerchandise else {
            showUserToast("Nur Admins dürfen Artikel bearbeiten.", style: .error)
            return false
        }

        guard item.id != nil else {
            showUserToast("Artikel hat keine gültige ID.", style: .error)
            return false
        }

        do {
            try await merchandiseService.updateItem(item, imageDataList: imageDataList)
            showUserToast("Artikel aktualisiert: \(item.name)", style: .success)
            return true
        } catch {
            print("Dev Fehler updateMerchandise:", error.localizedDescription)
            showUserToast("Update fehlgeschlagen: \(error.localizedDescription)", style: .error)
            return false
        }
    }

    func deleteItem(_ item: MerchandiseItem) async {
        guard canManageMerchandise else {
            showUserToast("Nur Admins dürfen Artikel löschen.", style: .error)
            return
        }

        guard let id = item.id else {
            showUserToast("Artikel hat keine gültige ID.", style: .error)
            return
        }

        do {
            try await merchandiseService.deleteItem(itemID: id)
            showUserToast("Artikel gelöscht: \(item.name)", style: .success)
        } catch {
            print("Dev Fehler deleteItem:", error.localizedDescription)
            showUserToast("Fehler beim Löschen: \(error.localizedDescription)", style: .error)
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    deinit {
        stopObservingItems?()
    }
}
