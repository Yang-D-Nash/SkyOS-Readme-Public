//
//  ShopView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI
import FirebaseFirestore

struct ShopView: View {
    @StateObject private var viewModel = MerchandiseViewModel()
    @State private var showingAddSheet = false
    @State private var editingItem: MerchandiseItem?
    @State private var selectedItem: MerchandiseItem?
    @State private var itemToDelete: MerchandiseItem?
    @State private var showDeleteToast = false

    @Environment(\.colorScheme) private var environmentColorScheme

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView("Artikel werden geladen...")
                        .tint(AppColors.accent(for: environmentColorScheme))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if !viewModel.merchandiseItems.isEmpty {
                    List {
                        ForEach(viewModel.merchandiseItems) { item in
                            MerchandiseRowView(
                                item: item,
                                isAdmin: viewModel.currentUser?.isAdmin == true,
                                environmentColorScheme: environmentColorScheme,
                                onTap: { selectedItem = $0 },
                                onEdit: { editingItem = $0 },
                                onDelete: { itemToDelete = $0 }
                            )
                        }
                    }
                    .listStyle(.plain)
                    .background(AppColors.primaryBackground(for: environmentColorScheme))
                } else {
                    Text("Keine Artikel gefunden.")
                        .foregroundColor(AppColors.secondaryText(for: environmentColorScheme))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .navigationTitle("Skydown Merch")
            .background(AppColors.primaryBackground(for: environmentColorScheme).edgesIgnoringSafeArea(.all))
            .toolbar {
                if viewModel.currentUser?.isAdmin == true {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button {
                            showingAddSheet = true
                        } label: {
                            Image(systemName: "plus")
                                .foregroundColor(AppColors.accent(for: environmentColorScheme))
                                .accessibilityLabel("Neuen Artikel hinzufügen")
                        }
                    }
                }
            }
            .task {
                viewModel.fetchData()
            }
            .sheet(isPresented: $showingAddSheet) {
                NavigationStack {
                    MerchEditView(viewModel: viewModel)
                        .presentationDetents([.medium, .large])
                        .presentationDragIndicator(.visible)
                        .background(AppColors.primaryBackground(for: environmentColorScheme))
                }
            }
            .sheet(item: $editingItem) { item in
                NavigationStack {
                    MerchEditView(viewModel: viewModel, merchandiseItem: item)
                        .presentationDetents([.medium, .large])
                        .presentationDragIndicator(.visible)
                        .background(AppColors.primaryBackground(for: environmentColorScheme))
                }
            }
            .sheet(item: $selectedItem) { item in
                NavigationStack {
                    ContactFormView(item: item)
                        .background(AppColors.primaryBackground(for: environmentColorScheme))
                }
            }
            .confirmationDialog(
                "Soll dieser Artikel wirklich gelöscht werden?",
                isPresented: Binding(
                    get: { itemToDelete != nil },
                    set: { if !$0 { itemToDelete = nil } }
                ),
                titleVisibility: .visible
            ) {
                Button("Löschen", role: .destructive) {
                    if let item = itemToDelete {
                        Task {
                            await viewModel.deleteItem(item)
                            itemToDelete = nil
                            withAnimation { showDeleteToast = true }
                        }
                    }
                }
                Button("Abbrechen", role: .cancel) {
                    itemToDelete = nil
                }
            }
        }
        .fancyToast(isPresented: $showDeleteToast,
                    message: "Artikel gelöscht",
                    style: .error)
    }
}
#Preview {
    ShopView()
}
