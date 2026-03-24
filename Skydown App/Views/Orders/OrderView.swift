//
//  OrderView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import SwiftUI

struct OrderView: View {
    @StateObject private var viewModel = OrderViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @State private var orderToDelete: Order?
    @State private var showingDeleteAlert = false

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView("Bestellungen werden geladen...")
                } else if viewModel.orders.isEmpty {
                    Text("Keine Bestellungen vorhanden.")
                        .foregroundColor(.secondary)
                } else {
                    List {
                        ForEach(viewModel.orders) { order in
                            VStack(alignment: .leading, spacing: 6) {

                                // --- Header: User + Status ---
                                HStack {
                                    Text(order.userEmail)
                                        .font(.headline)
                                    Spacer()
                                    Button {
                                        Task { await viewModel.toggleCompleted(for: order) }
                                    } label: {
                                        Image(systemName: order.isCompleted ? "checkmark.square.fill" : "square")
                                            .foregroundColor(order.isCompleted ? .green : .gray)
                                            .font(.title2)
                                    }
                                    .buttonStyle(.plain)
                                }

                                // --- Items ---
                                ForEach(order.items) { item in
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(item.name)
                                                .font(.subheadline)

                                            if let size = item.size, !size.isEmpty {
                                                Text("Größe: \(size)")
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                            }
                                        }

                                        Spacer()

                                        Text("x\(item.quantity)")
                                            .font(.subheadline)
                                            .foregroundColor(.secondary)
                                    }
                                }

                                // --- Timestamp ---
                                Text(order.timestamp, style: .date)
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                            .padding(.vertical, 6)
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    orderToDelete = order
                                    showingDeleteAlert = true
                                } label: {
                                    Label("Löschen", systemImage: "trash")
                                }

                                Button {
                                    Task { await viewModel.toggleCompleted(for: order) }
                                } label: {
                                    Label("Erledigt", systemImage: "checkmark")
                                }
                                .tint(.green)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Bestellungen")
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
            .alert("Bestellung löschen?", isPresented: $showingDeleteAlert, actions: {
                Button("Abbrechen", role: .cancel) {}
                Button("Löschen", role: .destructive) {
                    if let order = orderToDelete {
                        Task { await viewModel.deleteOrder(order) }
                        orderToDelete = nil
                    }
                }
            }, message: {
                Text("Möchten Sie diese Bestellung wirklich löschen?")
            })
        }
        .fancyToast(isPresented: $viewModel.showToast,
                    message: viewModel.toastMessage,
                    style: viewModel.toastStyle)
    }
}

#Preview {
    OrderView()
}
