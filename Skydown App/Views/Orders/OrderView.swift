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
    @AppStorage("orders.postCheckoutHighlight") private var postCheckoutHighlight = ""
    @State private var orderToDelete: Order?
    @State private var showingDeleteAlert = false
    private let sectionSpacing: CGFloat = 14

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: sectionSpacing) {
                    OrdersHeroCard(
                        colorScheme: colorScheme,
                        orderCount: viewModel.orders.count,
                        completedCount: viewModel.orders.filter { $0.isCompleted }.count
                    )

                    if !postCheckoutHighlight.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        OrdersPostCheckoutStrip(
                            colorScheme: colorScheme,
                            message: postCheckoutHighlight
                        ) {
                            postCheckoutHighlight = ""
                        }
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    }

                    if viewModel.isLoading {
                        OrdersInlineStatusStrip(
                            colorScheme: colorScheme,
                            icon: "arrow.triangle.2.circlepath.circle.fill",
                            title: "Wird aktualisiert"
                        ) {
                            VStack(alignment: .leading, spacing: 10) {
                                ProgressView("Bestellungen werden geladen …")
                                    .tint(AppColors.accent(for: colorScheme))

                                Text("Deine Bestellungen werden sicher aktualisiert.")
                                    .font(.footnote.weight(.medium))
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                            }
                        }
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    } else if let loadError = viewModel.errorMessage {
                        OrdersInlineStatusStrip(
                            colorScheme: colorScheme,
                            icon: "exclamationmark.triangle.fill",
                            title: "Bestellungen gerade nicht verfuegbar"
                        ) {
                            VStack(alignment: .leading, spacing: 14) {
                                Text(loadError)
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    .fixedSize(horizontal: false, vertical: true)

                                Text("Support findest du in den Einstellungen.")
                                    .font(.footnote.weight(.medium))
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))

                                Button {
                                    viewModel.fetchOrders()
                                } label: {
                                    Text("Erneut laden")
                                        .font(.subheadline.weight(.semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(AppColors.accent(for: colorScheme))
                            }
                        }
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    } else if viewModel.orders.isEmpty {
                        OrdersInlineStatusStrip(
                            colorScheme: colorScheme,
                            icon: "shippingbox",
                            title: "Noch keine Bestellung"
                        ) {
                            VStack(alignment: .leading, spacing: 10) {
                                Text(
                                    "Neue Bestellungen erscheinen hier automatisch mit aktuellem Status."
                                )
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                .fixedSize(horizontal: false, vertical: true)

                                Text("Zum Aktualisieren nach unten ziehen.")
                                    .font(.footnote.weight(.medium))
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))

                                Text("Support findest du jederzeit in den Einstellungen.")
                                    .font(.footnote.weight(.medium))
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                            }
                        }
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    } else {
                        ForEach(Array(viewModel.orders.enumerated()), id: \.offset) { index, order in
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
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                            .animation(.easeInOut(duration: 0.2).delay(Double(index) * 0.01), value: viewModel.orders.count)
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
            .alert("Bestellung entfernen?", isPresented: $showingDeleteAlert, actions: {
                Button("Abbrechen", role: .cancel) {}
                Button("Entfernen", role: .destructive) {
                    if let order = orderToDelete {
                        Task { await viewModel.deleteOrder(order) }
                        orderToDelete = nil
                    }
                }
            }, message: {
                Text("Diese Bestellung wird aus deiner Liste entfernt. Das kann nicht rueckgaengig gemacht werden.")
            })
        }
        .animation(.easeInOut(duration: 0.22), value: viewModel.isLoading)
        .animation(.easeInOut(duration: 0.22), value: viewModel.errorMessage != nil)
        .animation(.easeInOut(duration: 0.22), value: viewModel.orders.isEmpty)
        .animation(.easeInOut(duration: 0.22), value: viewModel.orders.count)
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
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "shippingbox.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                Text("Bestellungen")
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
            }

            Text("Status und Versand immer klar im Blick.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 8) {
                OrdersBadge(text: orderCount == 1 ? "1 Bestellung" : "\(orderCount) Bestellungen", colorScheme: colorScheme)
                OrdersBadge(text: "\(completedCount) erledigt", colorScheme: colorScheme)
                OrdersBadge(text: "\(max(orderCount - completedCount, 0)) offen", colorScheme: colorScheme)
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct OrdersSectionCard<Content: View>: View {
    let title: String
    let colorScheme: ColorScheme
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            content
        }
        .padding(20)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct OrdersInlineStatusStrip<Content: View>: View {
    let colorScheme: ColorScheme
    let icon: String
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            content
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .animation(.easeInOut(duration: 0.2), value: title)
    }
}

private struct OrdersPostCheckoutStrip: View {
    let colorScheme: ColorScheme
    let message: String
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "checkmark.shield.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                Text("Nach dem Kauf")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                Spacer(minLength: 0)
                Button("Verstanden", action: onDismiss)
                    .font(.caption.weight(.semibold))
                    .buttonStyle(.bordered)
            }

            Text(message)
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.text(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)

            Text("Naechster Schritt: Neue Status erscheinen hier automatisch.")
                .font(.caption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.78))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct OrdersOrderCard: View {
    let order: Order
    let colorScheme: ColorScheme
    let isConfirmingPayment: Bool
    let onConfirmPayment: () -> Void
    let onToggleCompleted: () -> Void
    let onDelete: () -> Void
    @State private var showTechnicalDetails = false

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
        order.paymentStatus?.takeIfNotBlank()?.asUserFacingOrderStatus
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
        order.fulfillmentStatus?.takeIfNotBlank()?.asUserFacingOrderStatus
    }

    private var shopifyOrderName: String? {
        order.shopifyOrderName?.takeIfNotBlank()
    }

    private var shopifyOrderId: String? {
        order.shopifyOrderId?.takeIfNotBlank()
    }

    private var shopifySyncStatus: String? {
        order.shopifySyncStatus?.takeIfNotBlank()?.asUserFacingOrderStatus
    }

    private var stripeCheckoutStatus: String? {
        order.stripeCheckoutStatus?.takeIfNotBlank()?.asUserFacingOrderStatus
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

    private var primaryStatusLabel: String {
        if order.isCompleted { return "Abgeschlossen" }
        if let fulfillmentStatus { return fulfillmentStatus }
        if let paymentStatus { return paymentStatus }
        return "In Bearbeitung"
    }

    private var progressDetail: String {
        if order.isCompleted {
            return "Bestellung abgeschlossen und dokumentiert."
        }
        if let fulfillmentStatus {
            return "Versandstatus: \(fulfillmentStatus)."
        }
        if let paymentStatus {
            return "Zahlstatus: \(paymentStatus)."
        }
        return "Wir halten dich hier ueber die naechsten Schritte auf dem Laufenden."
    }

    var body: some View {
        OrdersSectionCard(title: title, colorScheme: colorScheme) {
            HStack {
                OrdersBadge(text: primaryStatusLabel, colorScheme: colorScheme)
                OrdersBadge(text: "\(totalItems) Teile", colorScheme: colorScheme)
                if let shippingZone {
                    OrdersBadge(text: shippingZone, colorScheme: colorScheme)
                }
                Spacer()
                Text(order.timestamp, style: .date)
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            OrdersInlineStatusStrip(
                colorScheme: colorScheme,
                icon: "clock.badge.checkmark.fill",
                title: "Naechster Schritt"
            ) {
                Text(progressDetail)
                    .font(.footnote.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .fixedSize(horizontal: false, vertical: true)
            }

            VStack(alignment: .leading, spacing: 8) {
                OrderMetaBlock(label: "Kontakt", value: contactEmail, colorScheme: colorScheme)
                if let paymentMethod {
                    OrderMetaBlock(label: "Zahlart", value: paymentMethod, colorScheme: colorScheme)
                }
                if let paymentStatus {
                    OrderMetaBlock(label: "Zahlstatus", value: paymentStatus, colorScheme: colorScheme)
                }
                if let fulfillmentStatus {
                    OrderMetaBlock(label: "Versandstatus", value: fulfillmentStatus, colorScheme: colorScheme)
                }
                if let shippingAddress {
                    OrderMetaBlock(label: "Adresse", value: shippingAddress, colorScheme: colorScheme)
                }
            }
            .padding(12)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.08), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))

            VStack(spacing: 10) {
                ForEach(order.items) { item in
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.name)
                                .font(.subheadline.weight(.semibold))
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
                    .padding(12)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.08), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }

            if order.subtotalAmount != nil || order.totalAmount != nil {
                VStack(alignment: .leading, spacing: 6) {
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
                        OrderMetaBlock(label: "MwSt. inkl.", value: String(format: "EUR %.2f bei %.1f%%", taxAmount, taxRate), colorScheme: colorScheme)
                    }
                    if let totalAmount = order.totalAmount {
                        OrderMetaBlock(label: "Gesamt", value: String(format: "EUR %.2f", totalAmount), colorScheme: colorScheme)
                    }
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.08), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }

            if hasTechnicalDetails {
                DisclosureGroup(isExpanded: $showTechnicalDetails) {
                    VStack(alignment: .leading, spacing: 8) {
                        if let paymentProvider {
                            OrderMetaBlock(label: "Zahlanbieter", value: paymentProvider, colorScheme: colorScheme)
                        }
                        if let paymentReference {
                            OrderMetaBlock(label: "Zahlreferenz", value: paymentReference, colorScheme: colorScheme)
                        }
                        if let fulfillmentProvider {
                            OrderMetaBlock(label: "Versanddienst", value: fulfillmentProvider, colorScheme: colorScheme)
                        }
                        if let shopifyOrderName {
                            OrderMetaBlock(label: "Shop-Bestellnummer", value: shopifyOrderName, colorScheme: colorScheme)
                        }
                        if let shopifyOrderId {
                            OrderMetaBlock(label: "Shop-Referenz", value: shopifyOrderId, colorScheme: colorScheme)
                        }
                        if let shopifySyncStatus {
                            OrderMetaBlock(label: "Shop-Synchronisierung", value: shopifySyncStatus, colorScheme: colorScheme)
                        }
                        if let stripeCheckoutStatus {
                            OrderMetaBlock(label: "Checkout-Status", value: stripeCheckoutStatus, colorScheme: colorScheme)
                        }
                        if let stripeCheckoutSessionId {
                            OrderMetaBlock(label: "Checkout-Referenz", value: stripeCheckoutSessionId, colorScheme: colorScheme)
                        }
                        if let stripePaymentIntentId {
                            OrderMetaBlock(label: "Zahlungsreferenz", value: stripePaymentIntentId, colorScheme: colorScheme)
                        }
                        if let whatsApp {
                            OrderMetaBlock(label: "WhatsApp", value: whatsApp, colorScheme: colorScheme)
                        }
                        if order.userEmail != contactEmail {
                            OrderMetaBlock(label: "Kontomail", value: order.userEmail, colorScheme: colorScheme)
                        }
                        if let message {
                            OrderMetaBlock(label: "Nachricht", value: message, colorScheme: colorScheme)
                        }
                    }
                    .padding(12)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.08), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .padding(.top, 6)
                } label: {
                    Text("Details anzeigen")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                .padding(.horizontal, 4)
                .tint(AppColors.accent(for: colorScheme))
                .contentShape(Rectangle())
                .accessibilityHint("Zeigt weitere Bestellinformationen")
            }

            VStack(alignment: .leading, spacing: 10) {
                Button(action: onToggleCompleted) {
                    Label(
                        order.isCompleted ? "Bestellung wieder oeffnen" : "Bestellung als erledigt markieren",
                        systemImage: order.isCompleted ? "arrow.uturn.backward.circle" : "checkmark.circle.fill"
                    )
                    .frame(minHeight: 44)
                    .frame(maxWidth: .infinity)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
                .controlSize(.large)
                .accessibilityHint(order.isCompleted ? "Markiert die Bestellung wieder als offen" : "Markiert die Bestellung als abgeschlossen")

                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 10) {
                        if !order.hasFinalPaymentStatus {
                            Button(action: onConfirmPayment) {
                                Label(
                                isConfirmingPayment ? "Wird bestaetigt..." : "Zahlung als eingegangen markieren",
                                    systemImage: "creditcard.and.123"
                                )
                                .frame(minHeight: 44)
                                .frame(maxWidth: .infinity)
                                .lineLimit(2)
                                .multilineTextAlignment(.center)
                            }
                            .buttonStyle(.bordered)
                            .disabled(isConfirmingPayment)
                            .controlSize(.regular)
                            .accessibilityHint("Bestaetigt den Zahlungseingang fuer diese Bestellung")
                        }

                        Button(action: onDelete) {
                            Label("Aus Liste entfernen", systemImage: "trash")
                                .frame(minHeight: 44)
                                .frame(maxWidth: .infinity)
                                .lineLimit(2)
                                .multilineTextAlignment(.center)
                        }
                        .buttonStyle(.plain)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .contentShape(Rectangle())
                        .accessibilityHint("Entfernt die Bestellung aus deiner Liste")
                    }

                    VStack(spacing: 8) {
                        if !order.hasFinalPaymentStatus {
                            Button(action: onConfirmPayment) {
                                Label(
                                    isConfirmingPayment ? "Wird bestaetigt..." : "Zahlung bestaetigen",
                                    systemImage: "creditcard.and.123"
                                )
                                .frame(minHeight: 44)
                                .frame(maxWidth: .infinity)
                                .lineLimit(2)
                                .multilineTextAlignment(.center)
                            }
                            .buttonStyle(.bordered)
                            .disabled(isConfirmingPayment)
                            .controlSize(.regular)
                            .accessibilityHint("Bestaetigt den Zahlungseingang fuer diese Bestellung")
                        }

                        Button(action: onDelete) {
                            Label("Aus Liste entfernen", systemImage: "trash")
                                .frame(minHeight: 44)
                                .frame(maxWidth: .infinity)
                                .lineLimit(2)
                                .multilineTextAlignment(.center)
                        }
                        .buttonStyle(.plain)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .contentShape(Rectangle())
                        .accessibilityHint("Entfernt die Bestellung aus deiner Liste")
                    }
                }
            }
        }
    }

    private var hasTechnicalDetails: Bool {
        paymentProvider != nil ||
            paymentReference != nil ||
            fulfillmentProvider != nil ||
            shopifyOrderName != nil ||
            shopifyOrderId != nil ||
            shopifySyncStatus != nil ||
            stripeCheckoutStatus != nil ||
            stripeCheckoutSessionId != nil ||
            stripePaymentIntentId != nil ||
            whatsApp != nil ||
            order.userEmail != contactEmail ||
            message != nil
    }
}

private struct OrderMetaBlock: View {
    let label: String
    let value: String
    let colorScheme: ColorScheme

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Text(label)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Spacer()

            Text(value)
                .font(.subheadline)
                .foregroundColor(AppColors.text(for: colorScheme))
                .multilineTextAlignment(.trailing)
        }
        .padding(.vertical, 3)
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
        .animation(.easeInOut(duration: 0.18), value: text)
    }
}

private extension String {
    func takeIfNotBlank() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    var asUserFacingOrderStatus: String {
        switch lowercased() {
        case "pending":
            return "In Klärung"
        case "open":
            return "Offen"
        case "confirmed":
            return "Bestaetigt"
        case "paid":
            return "Bezahlt"
        case "processing":
            return "In Bearbeitung"
        case "fulfilled":
            return "Versendet"
        case "unfulfilled":
            return "Nicht versendet"
        case "success", "succeeded":
            return "Abgeschlossen"
        case "failed":
            return "Nicht erfolgreich"
        case "expired":
            return "Abgelaufen"
        case "canceled", "cancelled":
            return "Storniert"
        default:
            return replacingOccurrences(of: "_", with: " ").capitalized
        }
    }
}

private extension Order {
    var hasFinalPaymentStatus: Bool {
        guard let raw = paymentStatus?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), !raw.isEmpty else {
            return false
        }
        return ["confirmed", "paid", "success", "succeeded"].contains(raw)
    }
}
