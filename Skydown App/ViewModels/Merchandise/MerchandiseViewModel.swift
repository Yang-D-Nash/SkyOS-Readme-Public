//
//  MerchandiseViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import Foundation
import FirebaseFirestore
import FirebaseAuth

@MainActor
class MerchandiseViewModel: ObservableObject {
    @Published var merchandiseItems: [MerchandiseItem] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var currentUser: User?

    // Toast
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info

    private var firestoreMerch = Firestore.firestore()
    private var listenerRegistration: ListenerRegistration?

    init() {
        Task { await fetchCurrentUser() }
    }

    func fetchData() {
        isLoading = true
        errorMessage = nil

        listenerRegistration?.remove()

        listenerRegistration = firestoreMerch.collection("merchandise").addSnapshotListener { [weak self] querySnapshot, error in
            guard let self = self else { return }
            self.isLoading = false

            if let error = error {
                print("Dev Fehler fetchData:", error.localizedDescription)
                self.showUserToast("Fehler beim Laden der Artikel: \(error.localizedDescription)", style: .error)
                self.merchandiseItems = []
                return
            }

            guard let documents = querySnapshot?.documents else {
                self.showUserToast("Keine Artikel gefunden.", style: .error)
                self.merchandiseItems = []
                return
            }

            self.merchandiseItems = documents.compactMap { doc in
                try? doc.data(as: MerchandiseItem.self)
            }.sorted { $0.name < $1.name }
        }
    }

    func fetchCurrentUser() async {
        guard let uid = Auth.auth().currentUser?.uid else { return }

        do {
            let snapshot = try await firestoreMerch.collection("users").document(uid).getDocument()
            if let user = try? snapshot.data(as: User.self) {
                self.currentUser = user
            }
        } catch {
            print("Dev Fehler fetchCurrentUser:", error.localizedDescription)
            showUserToast("Fehler beim Laden des Benutzers: \(error.localizedDescription)", style: .error)
        }
    }

    func addMerchandise(_ item: MerchandiseItem) async {
        guard currentUser?.isAdmin == true else {
            showUserToast("Nur Admins dürfen Artikel hinzufügen.", style: .error)
            return
        }

        do {
            _ = try firestoreMerch.collection("merchandise").addDocument(from: item)
            showUserToast("Artikel hinzugefügt: \(item.name)", style: .success)
        } catch {
            print("Dev Fehler addMerchandise:", error.localizedDescription)
            showUserToast("Fehler beim Hinzufügen des Artikels: \(error.localizedDescription)", style: .error)
        }
    }

    func updateMerchandisePrice(_ item: MerchandiseItem, newPrice: Double) async {
        guard currentUser?.isAdmin == true else {
            showUserToast("Nur Admins dürfen Artikel bearbeiten.", style: .error)
            return
        }

        guard let id = item.id else {
            showUserToast("Artikel hat keine gültige ID.", style: .error)
            return
        }

        do {
            try await firestoreMerch.collection("merchandise").document(id).updateData([
                "price": newPrice
            ])
            showUserToast("Preis aktualisiert: \(item.name)", style: .success)
        } catch {
            print("Dev Fehler updateMerchandisePrice:", error.localizedDescription)
            showUserToast("Update fehlgeschlagen: \(error.localizedDescription)", style: .error)
        }
    }

    func deleteItem(_ item: MerchandiseItem) async {
        guard currentUser?.isAdmin == true else {
            showUserToast("Nur Admins dürfen Artikel löschen.", style: .error)
            return
        }

        guard let id = item.id else {
            showUserToast("Artikel hat keine gültige ID.", style: .error)
            return
        }

        do {
            try await firestoreMerch.collection("merchandise").document(id).delete()
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
        listenerRegistration?.remove()
    }
}
