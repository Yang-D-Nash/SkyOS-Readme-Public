//
//  ShopView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI

struct ShopView: View {
    @ObservedObject private var authManager: AuthManager
    @StateObject private var viewModel: MerchandiseViewModel
    @State private var showingAddSheet = false
    @State private var editingItem: MerchandiseItem?
    @State private var selectedItem: MerchandiseItem?
    @State private var itemToDelete: MerchandiseItem?

    @Environment(\.colorScheme) private var environmentColorScheme

    init(
        authManager: AuthManager,
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService()
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        _viewModel = StateObject(
            wrappedValue: MerchandiseViewModel(
                merchandiseService: merchandiseService,
                authManager: authManager
            )
        )
    }

    private var isAdmin: Bool {
        authManager.userSession?.isAdmin == true
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView("Artikel werden geladen...")
                        .tint(AppColors.accent(for: environmentColorScheme))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        if isAdmin {
                            AdminMerchSection(
                                colorScheme: environmentColorScheme,
                                onAdd: { showingAddSheet = true }
                            )
                        }

                        if viewModel.merchandiseItems.isEmpty {
                            Text("Keine Artikel gefunden.")
                                .foregroundColor(AppColors.secondaryText(for: environmentColorScheme))
                                .listRowBackground(AppColors.primaryBackground(for: environmentColorScheme))
                        }

                        ForEach(viewModel.merchandiseItems) { item in
                            MerchandiseRowView(
                                item: item,
                                isAdmin: isAdmin,
                                environmentColorScheme: environmentColorScheme,
                                onTap: {
                                    if isAdmin {
                                        editingItem = $0
                                    } else {
                                        selectedItem = $0
                                    }
                                },
                                onEdit: { editingItem = $0 },
                                onDelete: { itemToDelete = $0 }
                            )
                        }
                    }
                    .listStyle(.plain)
                    .background(AppColors.primaryBackground(for: environmentColorScheme))
                }
            }
            .navigationTitle("Skydown Merch")
            .background(AppColors.primaryBackground(for: environmentColorScheme).edgesIgnoringSafeArea(.all))
            .task {
                viewModel.fetchData()
            }
            .sheet(isPresented: $showingAddSheet) {
                MerchEditView(viewModel: viewModel)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
                    .background(AppColors.primaryBackground(for: environmentColorScheme))
            }
            .sheet(item: $editingItem) { item in
                MerchEditView(viewModel: viewModel, merchandiseItem: item)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
                    .background(AppColors.primaryBackground(for: environmentColorScheme))
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
                        }
                    }
                }
                Button("Abbrechen", role: .cancel) {
                    itemToDelete = nil
                }
            }
        }
        .fancyToast(isPresented: $viewModel.showToast,
                    message: viewModel.toastMessage,
                    style: viewModel.toastStyle)
    }
}

private struct AdminMerchSection: View {
    let colorScheme: ColorScheme
    let onAdd: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Admin")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Artikel können hier direkt angelegt werden. Bilder werden wie auf Android direkt aus der Fotomediathek hochgeladen.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text("Bearbeiten und Löschen geht jetzt direkt über sichtbare Buttons unter jedem Artikel.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Button(action: onAdd) {
                Label("Artikel hinzufügen", systemImage: "photo.badge.plus")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accent(for: colorScheme))
        }
        .padding(.vertical, 8)
        .listRowBackground(AppColors.secondaryBackground(for: colorScheme))
    }
}

#Preview {
    let services = AppServices()

    ShopView(
        authManager: services.authManager,
        merchandiseService: services.merchandiseService
    )
}
