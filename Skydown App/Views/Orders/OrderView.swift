//
//  OrderView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import SwiftUI

struct OrderView: View {
    @StateObject private var viewModel = OrderViewModel()
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage("orders.postCheckoutHighlight") private var postCheckoutHighlight = ""
    @State private var orderToDelete: Order?
    @State private var showingDeleteAlert = false
    private let sectionSpacing: CGFloat = 14

    private var orderContentStateAnimation: Animation {
        SkydownMotion.preferredContentReveal(accessibilityReduceMotion: reduceMotion)
    }

    private var orderStripTransition: AnyTransition {
        reduceMotion ? .opacity : .opacity.combined(with: .move(edge: .top))
    }

    private var orderCardTransition: AnyTransition {
        reduceMotion ? .opacity : .opacity.combined(with: .move(edge: .bottom))
    }

    private func orderRowInsertionAnimation(index: Int) -> Animation {
        if reduceMotion {
            return .linear(duration: 0.01)
        }
        return .easeInOut(duration: 0.2).delay(Double(index) * 0.01)
    }

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
                        .transition(orderStripTransition)
                    }

                    if viewModel.isLoading {
                        OrdersInlineStatusStrip(
                            colorScheme: colorScheme,
                            icon: "arrow.triangle.2.circlepath.circle.fill",
                            title: AppLocalized.text("orders.loading.title", fallback: "Updating")
                        ) {
                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                                ProgressView(AppLocalized.text("orders.loading.progress", fallback: "Loading orders…"))
                                    .tint(AppColors.accent(for: colorScheme))

                                Text(AppLocalized.text("orders.loading.hint", fallback: "Your orders are refreshed securely."))
                                    .font(.footnote.weight(.medium))
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                            }
                        }
                        .transition(orderStripTransition)
                    } else if let loadError = viewModel.errorMessage {
                        OrdersInlineStatusStrip(
                            colorScheme: colorScheme,
                            icon: "exclamationmark.triangle.fill",
                            title: AppLocalized.text("orders.error.title", fallback: "Orders unavailable right now")
                        ) {
                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                                Text(loadError)
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    .fixedSize(horizontal: false, vertical: true)

                                Text(AppLocalized.text("orders.error.support_hint", fallback: "You will find support in Settings."))
                                    .font(.footnote.weight(.medium))
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))

                                SkydownBrandActionButton(
                                    title: AppLocalized.text("orders.retry", fallback: "Try again"),
                                    accent: AppColors.accent(for: colorScheme),
                                    colorScheme: colorScheme,
                                    font: .subheadline.weight(.semibold),
                                    cornerRadius: SkydownLayout.denseRadius,
                                    verticalPadding: 12,
                                    action: { viewModel.fetchOrders() }
                                )
                                .skydownInteractiveFeedback()
                            }
                        }
                        .transition(orderStripTransition)
                    } else if viewModel.orders.isEmpty {
                        OrdersInlineStatusStrip(
                            colorScheme: colorScheme,
                            icon: "shippingbox",
                            title: AppLocalized.text("orders.empty.title", fallback: "No orders yet")
                        ) {
                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                                Text(AppLocalized.text("orders.empty.body", fallback: "New orders appear here automatically with the latest status."))
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                .fixedSize(horizontal: false, vertical: true)

                                Text(AppLocalized.text("orders.empty.pull_hint", fallback: "Pull down to refresh."))
                                    .font(.footnote.weight(.medium))
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))

                                Text(AppLocalized.text("orders.empty.support_hint", fallback: "You can always find support in Settings."))
                                    .font(.footnote.weight(.medium))
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                            }
                        }
                        .transition(orderStripTransition)
                    } else {
                        ForEach(Array(viewModel.orders.enumerated()), id: \.offset) { index, order in
                            OrdersOrderCard(
                                order: order,
                                colorScheme: colorScheme,
                                isConfirmingPayment: viewModel.confirmingPaymentOrderIDs.contains(order.id ?? ""),
                                canManageOrders: canManageOrders,
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
                            .transition(orderCardTransition)
                            .animation(orderRowInsertionAnimation(index: index), value: viewModel.orders.count)
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
            .navigationTitle(AppLocalized.text("orders.nav_title", fallback: "Orders"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.done", fallback: "Fertig"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: { dismiss() }
                    )
                    .skydownInteractiveFeedback()
                }
            }
            .alert(AppLocalized.text("orders.alert.delete_title", fallback: "Remove order?"), isPresented: $showingDeleteAlert, actions: {
                Button(AppLocalized.text("common.cancel", fallback: "Cancel"), role: .cancel) {}
                Button(AppLocalized.text("orders.alert.remove", fallback: "Remove"), role: .destructive) {
                    if let order = orderToDelete {
                        Task { await viewModel.deleteOrder(order) }
                        orderToDelete = nil
                    }
                }
            }, message: {
                Text(AppLocalized.text("orders.alert.delete_message", fallback: "This order will be removed from your list. This cannot be undone."))
            })
        }
        .animation(orderContentStateAnimation, value: viewModel.isLoading)
        .animation(orderContentStateAnimation, value: viewModel.errorMessage != nil)
        .animation(orderContentStateAnimation, value: viewModel.orders.isEmpty)
        .animation(orderContentStateAnimation, value: viewModel.orders.count)
        .fancyToast(isPresented: $viewModel.showToast,
                    message: viewModel.toastMessage,
                    style: viewModel.toastStyle)
    }

    private var canManageOrders: Bool {
        authManager.userSession?.isPlatformOwner == true
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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: "shippingbox.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
                Text(AppLocalized.text("orders.hero.title", fallback: "Orders"))
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
            }

            Text(AppLocalized.text("orders.hero.subtitle", fallback: "Status and shipping, always clear."))
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                OrdersBadge(
                    text: orderCount == 1
                        ? AppLocalized.text("orders.hero.badge_order_one", fallback: "1 order")
                        : String(format: AppLocalized.text("orders.hero.badge_order_many", fallback: "%d orders"), orderCount),
                    colorScheme: colorScheme
                )
                OrdersBadge(
                    text: String(format: AppLocalized.text("orders.hero.badge_completed", fallback: "%d completed"), completedCount),
                    colorScheme: colorScheme
                )
                OrdersBadge(
                    text: String(format: AppLocalized.text("orders.hero.badge_open", fallback: "%d open"), max(orderCount - completedCount, 0)),
                    colorScheme: colorScheme
                )
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
    }
}

private struct OrdersSectionCard<Content: View>: View {
    let title: String
    let colorScheme: ColorScheme
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            content
        }
        .padding(20)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
    }
}

private struct OrdersInlineStatusStrip<Content: View>: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let colorScheme: ColorScheme
    let icon: String
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(spacing: SkydownLayout.stackSpacingPill) {
                Image(systemName: icon)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            content
        }
        .padding(.horizontal, SkydownLayout.cardPadding)
        .padding(.vertical, 14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
        .animation(SkydownMotion.preferredStatusTransition(accessibilityReduceMotion: reduceMotion), value: title)
    }
}

private struct OrdersPostCheckoutStrip: View {
    let colorScheme: ColorScheme
    let message: String
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Image(systemName: "checkmark.shield.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                Text(AppLocalized.text("orders.post_checkout.header", fallback: "After checkout"))
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                Spacer(minLength: 0)
                SkydownBrandActionButton(
                    title: AppLocalized.text("orders.post_checkout.dismiss", fallback: "Got it"),
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    role: .muted,
                    font: .caption.weight(.semibold),
                    cornerRadius: SkydownLayout.compactRadius,
                    verticalPadding: 6,
                    expandToFullWidth: false,
                    action: onDismiss
                )
                .skydownInteractiveFeedback()
            }

            Text(message)
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.text(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)

            Text(AppLocalized.text("orders.post_checkout.footer_hint", fallback: "Next: new statuses appear here automatically."))
                .font(.caption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.78))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
    }
}

private struct OrdersOrderCard: View {
    let order: Order
    let colorScheme: ColorScheme
    let isConfirmingPayment: Bool
    let canManageOrders: Bool
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
        if order.isCompleted { return AppLocalized.text("orders.card.completed", fallback: "Completed") }
        if let fulfillmentStatus { return fulfillmentStatus }
        if let paymentStatus { return paymentStatus }
        return AppLocalized.text("orders.card.in_progress", fallback: "In progress")
    }

    private var progressDetail: String {
        if order.isCompleted {
            return AppLocalized.text("orders.card.progress.completed", fallback: "Order completed and recorded.")
        }
        if let fulfillmentStatus {
            return String(format: AppLocalized.text("orders.card.progress.fulfillment", fallback: "Shipping status: %@."), fulfillmentStatus)
        }
        if let paymentStatus {
            return String(format: AppLocalized.text("orders.card.progress.payment", fallback: "Payment status: %@."), paymentStatus)
        }
        return AppLocalized.text("orders.card.progress.updating", fallback: "We will keep you posted on the next steps here.")
    }

    var body: some View {
        OrdersSectionCard(title: title, colorScheme: colorScheme) {
            HStack {
                OrdersBadge(text: primaryStatusLabel, colorScheme: colorScheme)
                OrdersBadge(
                    text: String(format: AppLocalized.text("orders.parts_count", fallback: "%d items"), totalItems),
                    colorScheme: colorScheme
                )
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
                title: AppLocalized.text("orders.card.next_step_title", fallback: "Next step")
            ) {
                Text(progressDetail)
                    .font(.footnote.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .fixedSize(horizontal: false, vertical: true)
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                OrderMetaBlock(label: AppLocalized.text("orders.meta.contact", fallback: "Contact"), value: contactEmail, colorScheme: colorScheme)
                if let paymentMethod {
                    OrderMetaBlock(label: AppLocalized.text("orders.meta.payment_method", fallback: "Payment method"), value: paymentMethod, colorScheme: colorScheme)
                }
                if let paymentStatus {
                    OrderMetaBlock(label: AppLocalized.text("orders.meta.payment_status", fallback: "Payment status"), value: paymentStatus, colorScheme: colorScheme)
                }
                if let fulfillmentStatus {
                    OrderMetaBlock(label: AppLocalized.text("orders.meta.fulfillment_status", fallback: "Shipping status"), value: fulfillmentStatus, colorScheme: colorScheme)
                }
                if let shippingAddress {
                    OrderMetaBlock(label: AppLocalized.text("orders.meta.address", fallback: "Address"), value: shippingAddress, colorScheme: colorScheme)
                }
            }
            .padding(12)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.08), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))

            VStack(spacing: SkydownLayout.stackSpacingPill) {
                ForEach(order.items) { item in
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                            Text(item.name)
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.text(for: colorScheme))

                            if let size = item.size, !size.isEmpty {
                                Text(String(format: AppLocalized.text("orders.meta.size_format", fallback: "Size: %@"), size))
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }

                            if let color = item.color?.takeIfNotBlank() {
                                Text(String(format: AppLocalized.text("orders.meta.color_format", fallback: "Color: %@"), color))
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                        }

                        Spacer()

                        VStack(alignment: .trailing, spacing: SkydownLayout.stackSpacingNano) {
                            Text("x\(item.quantity)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            if let unitPrice = item.unitPrice {
                                Text(String(format: AppLocalized.text("orders.currency.eur", fallback: "EUR %.2f"), unitPrice))
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                        }
                    }
                    .padding(12)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.08), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
                }
            }

            if order.subtotalAmount != nil || order.totalAmount != nil {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                    Text(AppLocalized.text("orders.meta.order_summary", fallback: "Order total"))
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if let subtotalAmount = order.subtotalAmount {
                        OrderMetaBlock(
                            label: AppLocalized.text("orders.meta.subtotal", fallback: "Subtotal"),
                            value: String(format: AppLocalized.text("orders.currency.eur", fallback: "EUR %.2f"), subtotalAmount),
                            colorScheme: colorScheme
                        )
                    }
                    if let shippingAmount = order.shippingAmount {
                        OrderMetaBlock(
                            label: AppLocalized.text("orders.meta.shipping", fallback: "Shipping"),
                            value: String(format: AppLocalized.text("orders.currency.eur", fallback: "EUR %.2f"), shippingAmount),
                            colorScheme: colorScheme
                        )
                    }
                    if let taxAmount = order.taxAmount, let taxRate = order.taxRate {
                        OrderMetaBlock(
                            label: AppLocalized.text("orders.meta.tax_included", fallback: "VAT incl."),
                            value: String(format: AppLocalized.text("orders.meta.tax_detail", fallback: "EUR %.2f at %.1f%%"), taxAmount, taxRate),
                            colorScheme: colorScheme
                        )
                    }
                    if let totalAmount = order.totalAmount {
                        OrderMetaBlock(
                            label: AppLocalized.text("orders.meta.total", fallback: "Total"),
                            value: String(format: AppLocalized.text("orders.currency.eur", fallback: "EUR %.2f"), totalAmount),
                            colorScheme: colorScheme
                        )
                    }
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.08), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
            }

            if hasTechnicalDetails {
                DisclosureGroup(isExpanded: $showTechnicalDetails) {
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                        if let paymentProvider {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.payment_provider", fallback: "Payment provider"), value: paymentProvider, colorScheme: colorScheme)
                        }
                        if let paymentReference {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.payment_reference", fallback: "Payment reference"), value: paymentReference, colorScheme: colorScheme)
                        }
                        if let fulfillmentProvider {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.fulfillment_provider", fallback: "Carrier"), value: fulfillmentProvider, colorScheme: colorScheme)
                        }
                        if let shopifyOrderName {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.shop_order_name", fallback: "Shop order no."), value: shopifyOrderName, colorScheme: colorScheme)
                        }
                        if let shopifyOrderId {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.shop_reference", fallback: "Shop reference"), value: shopifyOrderId, colorScheme: colorScheme)
                        }
                        if let shopifySyncStatus {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.shop_sync", fallback: "Shop sync"), value: shopifySyncStatus, colorScheme: colorScheme)
                        }
                        if let stripeCheckoutStatus {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.checkout_status", fallback: "Checkout status"), value: stripeCheckoutStatus, colorScheme: colorScheme)
                        }
                        if let stripeCheckoutSessionId {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.checkout_reference", fallback: "Checkout reference"), value: stripeCheckoutSessionId, colorScheme: colorScheme)
                        }
                        if let stripePaymentIntentId {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.payment_intent_ref", fallback: "Payment intent"), value: stripePaymentIntentId, colorScheme: colorScheme)
                        }
                        if let whatsApp {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.whatsapp", fallback: "WhatsApp"), value: whatsApp, colorScheme: colorScheme)
                        }
                        if order.userEmail != contactEmail {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.account_email", fallback: "Account email"), value: order.userEmail, colorScheme: colorScheme)
                        }
                        if let message {
                            OrderMetaBlock(label: AppLocalized.text("orders.meta.message", fallback: "Message"), value: message, colorScheme: colorScheme)
                        }
                    }
                    .padding(12)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.08), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
                    .padding(.top, 6)
                } label: {
                    Text(AppLocalized.text("orders.details.show", fallback: "Show details"))
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                .padding(.horizontal, 4)
                .tint(AppColors.accent(for: colorScheme))
                .contentShape(Rectangle())
                .accessibilityHint(AppLocalized.text("orders.details.a11y_hint", fallback: "Shows more order information"))
            }

            if canManageOrders {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    SkydownBrandActionButton(
                        title: order.isCompleted
                            ? AppLocalized.text("orders.action.reopen", fallback: "Reopen order")
                            : AppLocalized.text("orders.action.mark_done", fallback: "Mark order complete"),
                        systemImage: order.isCompleted ? "arrow.uturn.backward.circle" : "checkmark.circle.fill",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 12,
                        action: onToggleCompleted
                    )
                    .skydownInteractiveFeedback()
                    .frame(minHeight: 44)
                    .accessibilityHint(
                        order.isCompleted
                            ? AppLocalized.text("orders.a11y.toggle_done_hint_open", fallback: "Marks the order as open again")
                            : AppLocalized.text("orders.a11y.toggle_done_hint_done", fallback: "Marks the order as completed")
                    )

                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            if !order.hasFinalPaymentStatus {
                                SkydownBrandActionButton(
                                    title: isConfirmingPayment
                                        ? AppLocalized.text("orders.action.confirming_payment", fallback: "Confirming…")
                                        : AppLocalized.text("orders.action.confirm_payment", fallback: "Mark payment received"),
                                    systemImage: "creditcard.and.123",
                                    accent: AppColors.accent(for: colorScheme),
                                    colorScheme: colorScheme,
                                    role: .muted,
                                    isLoading: isConfirmingPayment,
                                    font: .subheadline.weight(.semibold),
                                    cornerRadius: SkydownLayout.denseRadius,
                                    verticalPadding: 12,
                                    action: onConfirmPayment
                                )
                                .skydownInteractiveFeedback()
                                .frame(minHeight: 44)
                                .accessibilityHint(AppLocalized.text("orders.a11y.confirm_payment_hint", fallback: "Confirms payment received for this order"))
                            }

                            SkydownBrandActionButton(
                                title: AppLocalized.text("orders.action.remove_from_list", fallback: "Remove from list"),
                                systemImage: "trash",
                                accent: AppColors.accent(for: colorScheme),
                                colorScheme: colorScheme,
                                role: .muted,
                                font: .caption.weight(.semibold),
                                cornerRadius: SkydownLayout.denseRadius,
                                verticalPadding: 10,
                                expandToFullWidth: true,
                                action: onDelete
                            )
                            .skydownInteractiveFeedback()
                            .frame(minHeight: 44)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .accessibilityHint(AppLocalized.text("orders.a11y.remove_hint", fallback: "Removes the order from your list"))
                        }

                        VStack(spacing: SkydownLayout.stackSpacingMicro) {
                            if !order.hasFinalPaymentStatus {
                                SkydownBrandActionButton(
                                    title: isConfirmingPayment
                                        ? AppLocalized.text("orders.action.confirming_payment", fallback: "Confirming…")
                                        : AppLocalized.text("orders.action.confirm_payment_compact", fallback: "Confirm payment"),
                                    systemImage: "creditcard.and.123",
                                    accent: AppColors.accent(for: colorScheme),
                                    colorScheme: colorScheme,
                                    role: .muted,
                                    isLoading: isConfirmingPayment,
                                    font: .subheadline.weight(.semibold),
                                    cornerRadius: SkydownLayout.denseRadius,
                                    verticalPadding: 12,
                                    action: onConfirmPayment
                                )
                                .skydownInteractiveFeedback()
                                .frame(minHeight: 44)
                                .accessibilityHint(AppLocalized.text("orders.a11y.confirm_payment_hint", fallback: "Confirms payment received for this order"))
                            }

                            SkydownBrandActionButton(
                                title: AppLocalized.text("orders.action.remove_from_list", fallback: "Remove from list"),
                                systemImage: "trash",
                                accent: AppColors.accent(for: colorScheme),
                                colorScheme: colorScheme,
                                role: .muted,
                                font: .caption.weight(.semibold),
                                cornerRadius: SkydownLayout.denseRadius,
                                verticalPadding: 10,
                                expandToFullWidth: true,
                                action: onDelete
                            )
                            .skydownInteractiveFeedback()
                            .frame(minHeight: 44)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .accessibilityHint(AppLocalized.text("orders.a11y.remove_hint", fallback: "Removes the order from your list"))
                        }
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
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingPill) {
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
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.accent(for: colorScheme)
        )
        .animation(SkydownMotion.preferredContentReveal(accessibilityReduceMotion: reduceMotion), value: text)
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
            return AppLocalized.text("orders.status.pending", fallback: "Pending")
        case "open":
            return AppLocalized.text("orders.status.open", fallback: "Open")
        case "confirmed":
            return AppLocalized.text("orders.status.confirmed", fallback: "Confirmed")
        case "paid":
            return AppLocalized.text("orders.status.paid", fallback: "Paid")
        case "processing":
            return AppLocalized.text("orders.status.processing", fallback: "Processing")
        case "fulfilled":
            return AppLocalized.text("orders.status.fulfilled", fallback: "Shipped")
        case "unfulfilled":
            return AppLocalized.text("orders.status.unfulfilled", fallback: "Not shipped")
        case "success", "succeeded":
            return AppLocalized.text("orders.status.success", fallback: "Completed")
        case "failed":
            return AppLocalized.text("orders.status.failed", fallback: "Failed")
        case "expired":
            return AppLocalized.text("orders.status.expired", fallback: "Expired")
        case "canceled", "cancelled":
            return AppLocalized.text("orders.status.canceled", fallback: "Canceled")
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
