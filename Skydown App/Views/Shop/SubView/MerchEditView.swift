//
//  MerchEditView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import SwiftUI

struct MerchEditView: View {
    @Environment(\.dismiss) var dismiss
    @ObservedObject var viewModel: MerchandiseViewModel
    @Environment(\.colorScheme) private var environmentColorScheme

    @State var name: String = ""
    @State var description: String = ""
    @State var price: String = ""
    @State var available: Bool = true
    @State var imageURLs: String = ""

    var merchandiseItem: MerchandiseItem?

    private var isFormValid: Bool {
        Double(price) != nil
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Name") {
                    TextField("Name", text: $name)
                        .disabled(merchandiseItem != nil)
                        .foregroundColor(AppColors.text(for: environmentColorScheme))
                }

                Section("Beschreibung") {
                    TextField("Beschreibung", text: $description)
                        .disabled(merchandiseItem != nil)
                        .foregroundColor(AppColors.text(for: environmentColorScheme))
                }

                Section("Preis") {
                    TextField("Preis", text: $price)
                        .keyboardType(.decimalPad)
                        .foregroundColor(AppColors.text(for: environmentColorScheme))
                }

                Section("Verfügbarkeit") {
                    Toggle("Verfügbar", isOn: $available)
                        .disabled(merchandiseItem != nil)
                        .tint(AppColors.accent(for: environmentColorScheme))
                }

                Section("Bilder (URLs, Komma getrennt)") {
                    TextField("Bild-URLs", text: $imageURLs)
                        .disabled(merchandiseItem != nil)
                        .foregroundColor(AppColors.text(for: environmentColorScheme))
                    Text("„Tipp: Du kannst https://cloudinary.com/ verwenden, um Bild-URLs für deinen Merch hochzuladen und zu verwalten.“")
                            .font(.footnote)
                            .foregroundColor(.gray)
                            .padding(.top, 2)
                }
            }
            .navigationTitle(merchandiseItem == nil ? "Artikel hinzufügen" : "Preis bearbeiten")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Speichern") {
                        Task {
                            guard let priceValue = Double(price) else { return }

                            if let item = merchandiseItem {
                                await viewModel.updateMerchandisePrice(item, newPrice: priceValue)
                            } else {
                                let newItem = MerchandiseItem(
                                    id: nil,
                                    name: name,
                                    price: priceValue,
                                    description: description,
                                    imageURLs: imageURLs.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) },
                                    available: available
                                )
                                await viewModel.addMerchandise(newItem)
                            }
                            dismiss()
                        }
                    }
                    .disabled(!isFormValid)
                }

                ToolbarItem(placement: .cancellationAction) {
                    Button("Abbrechen") { dismiss() }
                }
            }
            .onAppear {
                if let item = merchandiseItem {
                    name = item.name
                    description = item.description
                    price = String(item.price)
                    available = item.available
                    imageURLs = item.imageURLs.joined(separator: ", ")
                }
            }
            .background(AppColors.primaryBackground(for: environmentColorScheme).edgesIgnoringSafeArea(.all))
        }
    }
}


#Preview {
    let sampleItem = MerchandiseItem(
        id: "1",
        name: "Skydown Shirt",
        price: 29.99,
        description: "Exklusives T-Shirt in limitierter Auflage",
        imageURLs: ["https://via.placeholder.com/150"],
        available: true
    )
    
    let viewModel = MerchandiseViewModel()

    NavigationStack {
        MerchEditView(viewModel: viewModel, merchandiseItem: sampleItem)
            .environment(\.colorScheme, .dark)
    }
}
