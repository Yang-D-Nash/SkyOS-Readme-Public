//
//  MerchEditView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import SwiftUI
import PhotosUI

struct MerchEditView: View {
    @Environment(\.dismiss) var dismiss
    @ObservedObject var viewModel: MerchandiseViewModel
    @Environment(\.colorScheme) private var environmentColorScheme

    @State private var name: String = ""
    @State private var description: String = ""
    @State private var price: String = ""
    @State private var available: Bool = true
    @State private var selectedPhotoItems: [PhotosPickerItem] = []
    @State private var selectedImageData: [Data] = []
    @State private var selectedPreviewImages: [Image] = []
    @State private var isLoadingImages = false
    @State private var isSaving = false
    @State private var validationMessage: String?

    var merchandiseItem: MerchandiseItem?

    private var isFormValid: Bool {
        guard Double(price) != nil else { return false }
        guard merchandiseItem != nil || !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
        guard merchandiseItem != nil || !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
        guard merchandiseItem != nil || !selectedImageData.isEmpty else { return false }
        return !isLoadingImages && !isSaving
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

                Section(merchandiseItem == nil ? "Bilder" : "Bestehende Bilder") {
                    if let merchandiseItem {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(merchandiseItem.imageURLs, id: \.self) { imageURL in
                                    AsyncImage(url: URL(string: imageURL)) { image in
                                        image
                                            .resizable()
                                            .aspectRatio(contentMode: .fill)
                                            .frame(width: 110, height: 110)
                                            .clipped()
                                            .cornerRadius(10)
                                    } placeholder: {
                                        ProgressView()
                                            .frame(width: 110, height: 110)
                                            .background(Color(.systemGray6))
                                            .cornerRadius(10)
                                    }
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    } else {
                        PhotosPicker(
                            selection: $selectedPhotoItems,
                            maxSelectionCount: 10,
                            matching: .images
                        ) {
                            Label("Bilder auswählen", systemImage: "photo.on.rectangle.angled")
                                .foregroundColor(AppColors.accent(for: environmentColorScheme))
                        }

                        if isLoadingImages {
                            ProgressView("Bilder werden geladen ...")
                                .font(.footnote)
                        }

                        if !selectedPreviewImages.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(Array(selectedPreviewImages.enumerated()), id: \.offset) { _, image in
                                        image
                                            .resizable()
                                            .aspectRatio(contentMode: .fill)
                                            .frame(width: 110, height: 110)
                                            .clipped()
                                            .cornerRadius(10)
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                        } else {
                            Text("Wähle mindestens ein Bild aus deiner Fotomediathek aus.")
                                .font(.footnote)
                                .foregroundColor(.gray)
                        }

                        if selectedPhotoItems.isEmpty && validationMessage == "Bitte mindestens ein Bild auswählen." {
                            Text("Bitte mindestens ein Bild auswählen.")
                            .font(.footnote)
                            .foregroundColor(.red)
                        }
                    }
                }

                if let validationMessage {
                    Section {
                        Text(validationMessage)
                            .font(.footnote)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(merchandiseItem == nil ? "Artikel hinzufügen" : "Preis bearbeiten")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Speichern") {
                        Task {
                            guard let priceValue = Double(price) else { return }
                            validationMessage = nil
                            isSaving = true
                            defer { isSaving = false }

                            if let item = merchandiseItem {
                                let didSave = await viewModel.updateMerchandisePrice(item, newPrice: priceValue)
                                if didSave {
                                    dismiss()
                                }
                            } else {
                                guard !selectedImageData.isEmpty else {
                                    validationMessage = "Bitte mindestens ein Bild auswählen."
                                    return
                                }

                                let newItem = MerchandiseItem(
                                    id: nil,
                                    name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                                    price: priceValue,
                                    description: description.trimmingCharacters(in: .whitespacesAndNewlines),
                                    imageURLs: [],
                                    available: available
                                )
                                let didSave = await viewModel.addMerchandise(newItem, imageDataList: selectedImageData)
                                if didSave {
                                    dismiss()
                                }
                            }
                        }
                    }
                    .disabled(!isFormValid)
                }

                ToolbarItem(placement: .cancellationAction) {
                    Button("Abbrechen") { dismiss() }
                }
            }
            .task(id: selectedPhotoItems) {
                await loadSelectedImages()
            }
            .onAppear {
                if let item = merchandiseItem {
                    name = item.name
                    description = item.description
                    price = String(item.price)
                    available = item.available
                }
            }
            .background(AppColors.primaryBackground(for: environmentColorScheme).edgesIgnoringSafeArea(.all))
        }
    }

    @MainActor
    private func loadSelectedImages() async {
        guard merchandiseItem == nil else { return }

        if selectedPhotoItems.isEmpty {
            selectedImageData = []
            selectedPreviewImages = []
            return
        }

        isLoadingImages = true
        defer { isLoadingImages = false }

        var loadedImageData: [Data] = []
        var loadedPreviewImages: [Image] = []

        for item in selectedPhotoItems {
            do {
                guard let data = try await item.loadTransferable(type: Data.self),
                      let uiImage = UIImage(data: data) else {
                    continue
                }

                loadedImageData.append(data)
                loadedPreviewImages.append(Image(uiImage: uiImage))
            } catch {
                validationMessage = "Mindestens ein Bild konnte nicht geladen werden."
            }
        }

        selectedImageData = loadedImageData
        selectedPreviewImages = loadedPreviewImages
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
