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
    @Environment(\.dismiss) private var dismiss
    @State private var orderToDelete: Order?
    @State private var showingDeleteAlert = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    OrdersHeroCard(
                        colorScheme: colorScheme,
                        orderCount: viewModel.orders.count,
                        completedCount: viewModel.orders.filter { $0.isCompleted }.count
                    )

                    if viewModel.isLoading {
                        OrdersSectionCard(title: "Synchronisierung", colorScheme: colorScheme) {
                            ProgressView("Bestellungen werden geladen...")
                                .tint(AppColors.accent(for: colorScheme))
                        }
                    } else if viewModel.orders.isEmpty {
                        OrdersSectionCard(title: "Keine Bestellungen", colorScheme: colorScheme) {
                            Text("Sobald Orders eingehen, erscheinen sie hier direkt als Karten statt in einer Standardliste.")
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    } else {
                        ForEach(viewModel.orders) { order in
                            OrdersOrderCard(
                                order: order,
                                colorScheme: colorScheme,
                                isConfirmingPayment: viewModel.confirmingPaymentOrderIDs.contains(order.id ?? ""),
                                onConfirmPayment: {
                                    Task { await viewModel.confirmPayment(for: order) }
                                },
                                onToggleCompleted: {
                                    Task { await viewModel.toggleCompleted(for: order) }
                                },
                                onDelete: {
                                    orderToDelete = order
                                    showingDeleteAlert = true
                                }
                            )
                        }
                    }
                }
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.top, SkydownLayout.screenTopPadding)
            .padding(.bottom, SkydownLayout.screenBottomPadding)
            .scrollIndicators(.hidden)
            .refreshable {
                viewModel.fetchOrders()
            }
            .background(backgroundGradient.ignoresSafeArea())
            .navigationTitle("Bestellungen")
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Fertig") {
                        dismiss()
                    }
                }
            }
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

    private var backgroundGradient: LinearGradient {
        LinearGradient(
            colors: [
                AppColors.primaryBackground(for: colorScheme),
                AppColors.accent(for: colorScheme).opacity(0.14),
                AppColors.accentMystic(for: colorScheme).opacity(0.10),
                AppColors.primaryBackground(for: colorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

#Preview {
    OrderView()
}

private struct OrdersHeroCard: View {
    let colorScheme: ColorScheme
    let orderCount: Int
    let completedCount: Int

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Owner Queue")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Kontakt, Status und Rueckstand liegen direkt auf den Karten, damit du Bestellungen als Owner sauber pruefen und freigeben kannst.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "shippingbox.fill")
                    .font(.title2)
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }
        }
        .padding(20)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 10) {
                OrdersBadge(text: "\(orderCount) Orders", colorScheme: colorScheme)
                OrdersBadge(text: "\(completedCount) erledigt", colorScheme: colorScheme)
                OrdersBadge(text: "\(max(orderCount - completedCount, 0)) offen", colorScheme: colorScheme)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
    }
}

private struct OrdersSectionCard<Content: View>: View {
    let title: String
    let colorScheme: ColorScheme
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            content
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

private struct OrdersOrderCard: View {
    let order: Order
    let colorScheme: ColorScheme
    let isConfirmingPayment: Bool
    let onConfirmPayment: () -> Void
    let onToggleCompleted: () -> Void
    let onDelete: () -> Void

    private var title: String {
        if let customerName = order.customerName, !customerName.isEmpty {
            return customerName
        }
        return order.userEmail
    }

    private var contactEmail: String {
        if let customerEmail = order.customerEmail, !customerEmail.isEmpty {
            return customerEmail
        }
        return order.userEmail
    }

    private var whatsApp: String? {
        guard let whatsApp = order.whatsApp, !whatsApp.isEmpty else { return nil }
        return whatsApp
    }

    private var shippingAddress: String? {
        guard let shippingAddress = order.shippingAddress, !shippingAddress.isEmpty else { return nil }
        return shippingAddress
    }

    private var message: String? {
        guard let message = order.message, !message.isEmpty else { return nil }
        return message
    }

    private var paymentMethod: String? {
        guard let paymentMethod = order.paymentMethod, !paymentMethod.isEmpty else { return nil }
        return paymentMethod
    }

    private var paymentProvider: String? {
        order.paymentProvider?.takeIfNotBlank()
    }

    private var paymentStatus: String? {
        order.paymentStatus?.takeIfNotBlank()
    }

    private var paymentReference: String? {
        order.paymentReference?.takeIfNotBlank()
    }

    private var shippingZone: String? {
        order.shippingZone?.takeIfNotBlank()
    }

    private var fulfillmentProvider: String? {
        order.fulfillmentProvider?.takeIfNotBlank()
    }

    private var fulfillmentStatus: String? {
        order.fulfillmentStatus?.takeIfNotBlank()
    }

    private var shopifyOrderName: String? {
        order.shopifyOrderName?.takeIfNotBlank()
    }

    private var shopifyOrderId: String? {
        order.shopifyOrderId?.takeIfNotBlank()
    }

    private var shopifySyncStatus: String? {
        order.shopifySyncStatus?.takeIfNotBlank()
    }

    private var stripeCheckoutStatus: String? {
        order.stripeCheckoutStatus?.takeIfNotBlank()
    }

    private var stripeCheckoutSessionId: String? {
        order.stripeCheckoutSessionId?.takeIfNotBlank()
    }

    private var stripePaymentIntentId: String? {
        order.stripePaymentIntentId?.takeIfNotBlank()
    }

    private var totalItems: Int {
        order.items.reduce(0) { partialResult, item in
            partialResult + item.quantity
        }
    }

    var body: some View {
        OrdersSectionCard(title: title, colorScheme: colorScheme) {
            HStack {
                OrdersBadge(
                    text: order.isCompleted ? "Erledigt" : "Offen",
                    colorScheme: colorScheme
                )
                OrdersBadge(
                    text: "\(totalItems) Teile",
                    colorScheme: colorScheme
                )
                if let paymentStatus {
                    OrdersBadge(
                        text: paymentStatus,
                        colorScheme: colorScheme
                    )
                }
                Spacer()
                Text(order.timestamp, style: .date)
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 10) {
                OrderMetaBlock(
                    label: "Kontakt",
                    value: contactEmail,
                    colorScheme: colorScheme
                )

                if let whatsApp {
                    OrderMetaBlock(
                        label: "WhatsApp",
                        value: whatsApp,
                        colorScheme: colorScheme
                    )
                }

                if let shippingAddress {
                    OrderMetaBlock(
                        label: "Adresse",
                        value: shippingAddress,
                        colorScheme: colorScheme
                    )
                }

                if let paymentMethod {
                    OrderMetaBlock(
                        label: "Zahlart",
                        value: paymentMethod,
                        colorScheme: colorScheme
                    )
                }

                if let paymentProvider {
                    OrderMetaBlock(
                        label: "Provider",
                        value: paymentProvider,
                        colorScheme: colorScheme
                    )
                }

                if let paymentStatus {
                    OrderMetaBlock(
                        label: "Zahlstatus",
                        value: paymentStatus,
                        colorScheme: colorScheme
                    )
                }

                if let paymentReference {
                    OrderMetaBlock(
                        label: "Zahlreferenz",
                        value: paymentReference,
                        colorScheme: colorScheme
                    )
                }

                if let shippingZone {
                    OrderMetaBlock(
                        label: "Versandzone",
                        value: shippingZone,
                        colorScheme: colorScheme
                    )
                }

                if let fulfillmentProvider {
                    OrderMetaBlock(
                        label: "Fulfillment",
                        value: fulfillmentProvider,
                        colorScheme: colorScheme
                    )
                }

                if let fulfillmentStatus {
                    OrderMetaBlock(
                        label: "Fulfillment-Status",
                        value: fulfillmentStatus,
                        colorScheme: colorScheme
                    )
                }

                if let shopifyOrderName {
                    OrderMetaBlock(
                        label: "Shopify Order",
                        value: shopifyOrderName,
                        colorScheme: colorScheme
                    )
                }

                if let shopifyOrderId {
                    OrderMetaBlock(
                        label: "Shopify ID",
                        value: shopifyOrderId,
                        colorScheme: colorScheme
                    )
                }

                if let shopifySyncStatus {
                    OrderMetaBlock(
                        label: "Shopify Sync",
                        value: shopifySyncStatus,
                        colorScheme: colorScheme
                    )
                }

                if let stripeCheckoutStatus {
                    OrderMetaBlock(
                        label: "Stripe Checkout",
                        value: stripeCheckoutStatus,
                        colorScheme: colorScheme
                    )
                }

                if let stripeCheckoutSessionId {
                    OrderMetaBlock(
                        label: "Stripe Session",
                        value: stripeCheckoutSessionId,
                        colorScheme: colorScheme
                    )
                }

                if let stripePaymentIntentId {
                    OrderMetaBlock(
                        label: "Stripe Payment",
                        value: stripePaymentIntentId,
                        colorScheme: colorScheme
                    )
                }

                if order.userEmail != contactEmail {
                    OrderMetaBlock(
                        label: "Login-Mail",
                        value: order.userEmail,
                        colorScheme: colorScheme
                    )
                }

                if let message {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Nachricht")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        Text(message)
                            .font(.subheadline)
                            .foregroundColor(AppColors.text(for: colorScheme))
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }
            }

            VStack(spacing: 10) {
                ForEach(order.items) { item in
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.name)
                                .font(.headline)
                                .foregroundColor(AppColors.text(for: colorScheme))

                            if let size = item.size, !size.isEmpty {
                                Text("Größe: \(size)")
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }

                            if let color = item.color?.takeIfNotBlank() {
                                Text("Farbe: \(color)")
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                        }

                        Spacer()

                        VStack(alignment: .trailing, spacing: 4) {
                            Text("x\(item.quantity)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            if let unitPrice = item.unitPrice {
                                Text(String(format: "EUR %.2f", unitPrice))
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                        }
                    }
                    .padding(14)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }
            }

            if order.subtotalAmount != nil || order.totalAmount != nil {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Bestellsumme")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if let subtotalAmount = order.subtotalAmount {
                        OrderMetaBlock(label: "Zwischensumme", value: String(format: "EUR %.2f", subtotalAmount), colorScheme: colorScheme)
                    }
                    if let shippingAmount = order.shippingAmount {
                        OrderMetaBlock(label: "Versand", value: String(format: "EUR %.2f", shippingAmount), colorScheme: colorScheme)
                    }
                    if let taxAmount = order.taxAmount, let taxRate = order.taxRate {
                        OrderMetaBlock(label: "inkl. MwSt.", value: String(format: "EUR %.2f bei %.1f%%", taxAmount, taxRate), colorScheme: colorScheme)
                    }
                    if let totalAmount = order.totalAmount {
                        OrderMetaBlock(label: "Gesamt", value: String(format: "EUR %.2f", totalAmount), colorScheme: colorScheme)
                    }
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 18))
            }

            HStack(spacing: 10) {
                if paymentStatus != "confirmed" {
                    Button(action: onConfirmPayment) {
                        Label(
                            isConfirmingPayment ? "Bestaetige..." : "Zahlung bestaetigen",
                            systemImage: "creditcard.and.123"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .disabled(isConfirmingPayment)
                }

                Button(action: onToggleCompleted) {
                    Label(
                        order.isCompleted ? "Wieder öffnen" : "Als erledigt markieren",
                        systemImage: order.isCompleted ? "arrow.uturn.backward.circle" : "checkmark.circle.fill"
                    )
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))

                Button(role: .destructive, action: onDelete) {
                    Label("Löschen", systemImage: "trash")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
    }
}

private struct OrderMetaBlock: View {
    let label: String
    let value: String
    let colorScheme: ColorScheme

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(label)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Spacer()

            Text(value)
                .font(.subheadline)
                .foregroundColor(AppColors.text(for: colorScheme))
                .multilineTextAlignment(.trailing)
        }
        .padding(14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct OrdersBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.accent(for: colorScheme)
        )
    }
}

private extension String {
    func takeIfNotBlank() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
