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
    private let onOpenLogin: () -> Void
    @State private var showingAddSheet = false
    @State private var editingItem: MerchandiseItem?
    @State private var selectedItem: MerchandiseItem?
    @State private var itemToDelete: MerchandiseItem?
    @Environment(\.colorScheme) private var colorScheme

    init(
        authManager: AuthManager,
        onOpenLogin: @escaping () -> Void = {},
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService()
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        self.onOpenLogin = onOpenLogin
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
                        .tint(AppColors.accent(for: colorScheme))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 16) {
                            ShopHeroCard(
                                colorScheme: colorScheme,
                                itemCount: viewModel.merchandiseItems.count,
                                isLoggedIn: authManager.userSession != nil,
                                isAdmin: isAdmin
                            )

                            if let errorMessage = viewModel.errorMessage, !isAdmin {
                                ShopInfoCard(
                                    colorScheme: colorScheme,
                                    title: "Anmeldung",
                                    message: errorMessage,
                                    actionTitle: "Anmelden",
                                    action: onOpenLogin
                                )
                            }

                            if viewModel.merchandiseItems.isEmpty {
                                ShopInfoCard(
                                    colorScheme: colorScheme,
                                    title: "Noch keine Artikel",
                                    message: "Sobald neuer Merch live ist, taucht er hier direkt als Card auf."
                                )
                            }

                            ForEach(viewModel.merchandiseItems) { item in
                                MerchandiseRowView(
                                    item: item,
                                    isAdmin: isAdmin,
                                    environmentColorScheme: colorScheme,
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
                        .padding(.horizontal, 20)
                        .padding(.top, 20)
                        .padding(.bottom, 28)
                    }
                    .scrollIndicators(.hidden)
                    .refreshable {
                        viewModel.fetchData()
                    }
                }
            }
            .background(backgroundGradient.ignoresSafeArea())
            .navigationTitle("Skydown Merch")
            .toolbar {
                if isAdmin {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            showingAddSheet = true
                        } label: {
                            Image(systemName: "plus")
                                .font(.headline)
                        }
                    }
                }
            }
            .task {
                if viewModel.merchandiseItems.isEmpty {
                    viewModel.fetchData()
                }
            }
            .sheet(isPresented: $showingAddSheet) {
                MerchEditView(viewModel: viewModel)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
                    .background(AppColors.primaryBackground(for: colorScheme))
            }
            .sheet(item: $editingItem) { item in
                MerchEditView(viewModel: viewModel, merchandiseItem: item)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
                    .background(AppColors.primaryBackground(for: colorScheme))
            }
            .sheet(item: $selectedItem) { item in
                NavigationStack {
                    ContactFormView(item: item)
                        .background(AppColors.primaryBackground(for: colorScheme))
                }
            }
            .confirmationDialog(
                "Soll dieser Artikel wirklich geloescht werden?",
                isPresented: Binding(
                    get: { itemToDelete != nil },
                    set: { if !$0 { itemToDelete = nil } }
                ),
                titleVisibility: .visible
            ) {
                Button("Loeschen", role: .destructive) {
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
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
    }

    private var backgroundGradient: LinearGradient {
        LinearGradient(
            colors: [
                AppColors.primaryBackground(for: colorScheme),
                AppColors.accent(for: colorScheme).opacity(0.14),
                AppColors.accentMystic(for: colorScheme).opacity(0.08),
                AppColors.primaryBackground(for: colorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

private struct ShopHeroCard: View {
    let colorScheme: ColorScheme
    let itemCount: Int
    let isLoggedIn: Bool
    let isAdmin: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Skydown Merch")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Produkte, Drops und spaetere Kaufwege wirken jetzt auf iOS ruhiger, direkter und naeher an der aktualisierten Android-App.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "bag.fill")
                    .font(.title2)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 10) {
                ShopBadge(text: "\(itemCount) Produkte", colorScheme: colorScheme)
                ShopBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
                if isAdmin {
                    ShopBadge(text: "Admin", colorScheme: colorScheme)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
    }

    private var cardBackground: some View {
        LinearGradient(
            colors: [
                AppColors.cardBackground(for: colorScheme),
                AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

private struct ShopInfoCard: View {
    let colorScheme: ColorScheme
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(message)
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let actionTitle, let action {
                Button(action: action) {
                    Text(actionTitle)
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct ShopBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(AppColors.accent(for: colorScheme).opacity(0.12))
            .foregroundColor(AppColors.accent(for: colorScheme))
            .clipShape(Capsule())
    }
}

#Preview {
    let services = AppServices()

    ShopView(
        authManager: services.authManager,
        merchandiseService: services.merchandiseService
    )
}
