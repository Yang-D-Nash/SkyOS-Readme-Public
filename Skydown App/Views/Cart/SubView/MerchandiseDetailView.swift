//
//  MerchandiseDetailView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI

/// Full-screen merchandise product detail: variants, story, add to cart. Not a support/contact form.
struct MerchandiseDetailView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var cartVM: CartViewModel
    @EnvironmentObject var authManager: AuthManager
    let item: MerchandiseItem
    let storeIsOpen: Bool
    let onOpenCart: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var showingConfirmSheet = false

    private var merchVariantAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .easeInOut(duration: 0.16)
    }

    private var merchFooterAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .easeInOut(duration: 0.18)
    }

    @State private var selectedSize = "M"
    @State private var selectedColor = ""
    @State private var selectedQuantity = 1
    private let availableQuantities = Array(1...10)

    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success
    @State private var selectedHeroImageIndex = 0
    @State private var showingFullscreenGallery = false
    @State private var pendingRouteToCart = false

    init(
        item: MerchandiseItem,
        storeIsOpen: Bool,
        onOpenCart: @escaping () -> Void = {}
    ) {
        self.item = item
        self.storeIsOpen = storeIsOpen
        self.onOpenCart = onOpenCart
    }

    private var availableSizes: [String] {
        let resolved = MerchandiseVariantResolver.availableSizes(for: item)
        return resolved.isEmpty ? ["XS", "S", "M", "L", "XL"] : resolved
    }

    private var availableColors: [String] {
        MerchandiseVariantResolver.availableColors(for: item, size: selectedSize)
    }

    private var resolvedVariant: MerchandiseVariant? {
        guard !item.variants.isEmpty else { return nil }
        return try? MerchandiseVariantResolver.resolveVariant(
            for: item,
            size: selectedSize,
            color: selectedColor.trimmedNonEmpty
        )
    }

    private var isSelectionValid: Bool {
        !selectedSize.isEmpty &&
        selectedQuantity > 0 &&
        (item.variants.isEmpty || resolvedVariant != nil)
    }

    private var canOrder: Bool {
        isSelectionValid && authManager.userSession != nil && storeIsOpen && item.available
    }

    private var selectionSummary: String {
        var parts = ["\(selectedQuantity)x", selectedSize]
        if let color = selectedColor.trimmedNonEmpty {
            parts.append(color)
        }
        return parts.joined(separator: " • ")
    }

    private var optionSummary: String {
        let colorCount = availableColors.isEmpty ? 0 : availableColors.count
        if colorCount > 0 {
            return "\(availableSizes.count) \(AppLocalized.text("shop.detail.options.sizes", fallback: "sizes")) • \(colorCount) \(AppLocalized.text("shop.detail.options.colors", fallback: "colors"))"
        }
        return "\(availableSizes.count) \(AppLocalized.text("shop.detail.options.sizes", fallback: "sizes"))"
    }

    private var totalPriceLabel: String {
        String(format: "EUR %.2f", item.price * Double(selectedQuantity))
    }

    private var unitPriceLabel: String {
        String(format: "EUR %.2f", item.price)
    }

    private var availabilityTrustLine: String {
        if item.available && storeIsOpen {
            return "Drop aktuell verfuegbar."
        }
        if !storeIsOpen {
            return "Drop sichtbar, Checkout pausiert."
        }
        return "Drop aktuell nicht verfuegbar."
    }

    private var shippingTrustLine: String {
        "Versandstatus und finale Kosten siehst du vor dem Absenden klar im Checkout."
    }

    private var supportTrustLine: String {
        "Support ist jederzeit in den Einstellungen erreichbar."
    }

    private var shippingConfidenceLine: String {
        storeIsOpen
            ? "Versandkosten siehst du klar vor Absenden."
            : "Versand startet, sobald der Store live ist."
    }

    private var secureCheckoutLine: String {
        authManager.userSession != nil
            ? "Sicher kaufen mit bestaetigtem Konto."
            : "Login vor Kauf fuer sicheren Checkout."
    }

    private var supportConfidenceLine: String {
        "Support ist erreichbar, falls vor Kauf Fragen offen sind."
    }

    private var qualityConfidenceLine: String {
        "Skydown Original mit sauberer Variantenzuordnung."
    }

    private var heroImageURLs: [String] {
        let customOverride = item.customImageOverride.trimmedNonEmpty
        let ordered = item.imageURLs.filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        guard let customOverride else {
            return ordered.isEmpty ? [""] : ordered
        }
        let deduped = [customOverride] + ordered.filter { $0 != customOverride }
        return deduped.isEmpty ? [""] : deduped
    }

    private var readinessTitle: String {
        if canOrder {
            return AppLocalized.text("shop.detail.readiness.ready", fallback: "Bereit")
        }
        if authManager.userSession == nil {
            return AppLocalized.text("shop.detail.readiness.login", fallback: "Login")
        }
        if !storeIsOpen {
            return AppLocalized.text("shop.detail.readiness.store_paused", fallback: "Store pausiert")
        }
        if !item.available {
            return AppLocalized.text("shop.detail.readiness.not_live", fallback: "Nicht live")
        }
        return AppLocalized.text("shop.detail.readiness.check_selection", fallback: "Auswahl pruefen")
    }

    private var readinessDetail: String {
        if canOrder {
            return AppLocalized.text("shop.detail.readiness.detail.ready", fallback: "Bereit fuer den Warenkorb")
        }
        if authManager.userSession == nil {
            return AppLocalized.text("shop.detail.readiness.detail.login_required", fallback: "Login erforderlich")
        }
        if !storeIsOpen {
            return AppLocalized.text("shop.detail.readiness.detail.store_paused", fallback: "Store pausiert")
        }
        if !item.available {
            return AppLocalized.text("shop.detail.readiness.detail.not_live", fallback: "Nicht live")
        }
        return AppLocalized.text("shop.detail.readiness.detail.choose_variant", fallback: "Variante waehlen")
    }

    private func addToCart() {
        cartVM.addItem(
            item,
            size: selectedSize,
            color: selectedColor.trimmedNonEmpty,
            quantity: selectedQuantity,
            resolvedVariant: resolvedVariant
        )
    }

    private func continueShoppingAfterSuccess() {
        showingConfirmSheet = false
        toastMessage = AppLocalized.text("shop.detail.toast.cart_updated", fallback: "Warenkorb aktualisiert")
        toastStyle = .success
        showToast = true
    }

    private func goToCartAfterSuccess() {
        showingConfirmSheet = false
        toastMessage = AppLocalized.text("shop.detail.toast.cart_updated", fallback: "Warenkorb aktualisiert")
        toastStyle = .success
        showToast = true
        pendingRouteToCart = true
        dismiss()
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
                MerchDetailHeroCard(
                    colorScheme: colorScheme,
                    itemName: item.name,
                    price: item.price,
                    quantity: selectedQuantity,
                    isLoggedIn: authManager.userSession != nil,
                    storeIsOpen: storeIsOpen,
                    isAvailable: item.available,
                    imageURLs: heroImageURLs,
                    selectedImageIndex: $selectedHeroImageIndex,
                    onOpenFullscreen: {
                        showingFullscreenGallery = true
                    }
                )

                MerchDetailReadinessStrip(
                    colorScheme: colorScheme,
                    readinessTitle: readinessTitle,
                    readinessDetail: readinessDetail,
                    accountTitle: authManager.userSession != nil
                        ? AppLocalized.text("shop.detail.account.active", fallback: "Aktiv")
                        : AppLocalized.text("shop.detail.account.login", fallback: "Login"),
                    accountDetail: authManager.userSession != nil
                        ? AppLocalized.text("shop.detail.account.detail.in_app", fallback: "Checkout in der App")
                        : AppLocalized.text("shop.detail.account.detail.login_to_order", fallback: "Zum Bestellen anmelden"),
                    selectionTitle: selectionSummary,
                    selectionDetail: optionSummary
                )

                MerchDetailTrustModule(
                    colorScheme: colorScheme,
                    availability: availabilityTrustLine,
                    shipping: shippingTrustLine,
                    support: supportTrustLine
                )

                if let story = item.description.trimmedNonEmpty {
                    MerchDetailSectionCard(title: AppLocalized.text("shop.detail.section.story", fallback: "Story"), colorScheme: colorScheme) {
                        Text(story)
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        HStack(spacing: SkydownLayout.stackSpacingPill) {
                            MerchDetailBadge(
                                text: storeIsOpen
                                    ? AppLocalized.text("shop.detail.badge.live", fallback: "Live")
                                    : AppLocalized.text("shop.detail.badge.paused", fallback: "Pausiert"),
                                colorScheme: colorScheme
                            )
                            MerchDetailBadge(
                                text: item.available
                                    ? AppLocalized.text("shop.detail.badge.visible", fallback: "Sichtbar")
                                    : AppLocalized.text("shop.detail.badge.offline", fallback: "Offline"),
                                colorScheme: colorScheme
                            )
                        }
                    }
                }

                MerchDetailSectionCard(title: AppLocalized.text("shop.detail.section.product", fallback: "Produkt"), colorScheme: colorScheme) {
                    MerchandiseItemView(item: item)
                }

                MerchDetailSectionCard(title: AppLocalized.text("shop.detail.section.size", fallback: "Groesse"), colorScheme: colorScheme) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: SkydownLayout.stackSpacingMicro) {
                            ForEach(availableSizes, id: \.self) { size in
                                Button {
                                    selectedSize = size
                                    SkydownHaptics.selection()
                                } label: {
                                    MerchVariantPill(
                                        text: size,
                                        isSelected: selectedSize == size,
                                        selectedTint: AppColors.accent(for: colorScheme),
                                        colorScheme: colorScheme
                                    )
                                }
                                .buttonStyle(.plain)
                                .skydownTactileAction()
                                .animation(merchVariantAnimation, value: selectedSize)
                            }
                        }
                    }
                }

                if !availableColors.isEmpty {
                    MerchDetailSectionCard(title: AppLocalized.text("shop.detail.section.color", fallback: "Farbe"), colorScheme: colorScheme) {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                                ForEach(availableColors, id: \.self) { color in
                                    Button {
                                        selectedColor = color
                                        SkydownHaptics.selection()
                                    } label: {
                                        MerchVariantPill(
                                            text: color,
                                            isSelected: selectedColor == color,
                                            selectedTint: AppColors.accentMystic(for: colorScheme),
                                            colorScheme: colorScheme
                                        )
                                    }
                                    .buttonStyle(.plain)
                                    .skydownTactileAction()
                                    .animation(merchVariantAnimation, value: selectedColor)
                                }
                            }
                        }
                    }
                }

                MerchDetailSectionCard(title: AppLocalized.text("shop.detail.section.quantity", fallback: "Quantity"), colorScheme: colorScheme) {
                    HStack(spacing: SkydownLayout.stackSpacingRelaxed) {
                        Button {
                            if selectedQuantity > 1 { selectedQuantity -= 1 }
                            SkydownHaptics.selection()
                        } label: {
                            Image(systemName: "minus")
                                .font(.headline)
                                .frame(width: 44, height: 44)
                        }
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .clipShape(Circle())

                        VStack(spacing: SkydownLayout.stackSpacingNano) {
                            Text("\(selectedQuantity)")
                                .font(.title2.weight(.bold))
                                .foregroundColor(AppColors.text(for: colorScheme))

                            Text(AppLocalized.text("shop.detail.quantity.unit", fallback: "pcs"))
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                        .frame(maxWidth: .infinity)

                        Button {
                            if selectedQuantity < availableQuantities.count { selectedQuantity += 1 }
                            SkydownHaptics.selection()
                        } label: {
                            Image(systemName: "plus")
                                .font(.headline)
                                .frame(width: 44, height: 44)
                        }
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .clipShape(Circle())
                    }
                }

                if authManager.userSession == nil {
                    MerchDetailSectionCard(title: AppLocalized.text("shop.detail.section.notice", fallback: "Notice"), colorScheme: colorScheme) {
                        Text(AppLocalized.text("shop.detail.notice.login_required", fallback: "Sign in to use cart and checkout in app."))
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else if !storeIsOpen {
                    MerchDetailSectionCard(title: AppLocalized.text("shop.detail.section.store_paused", fallback: "Store paused"), colorScheme: colorScheme) {
                        Text(AppLocalized.text("shop.detail.notice.store_paused", fallback: "Store is paused. Products stay visible, checkout is unavailable."))
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else if !item.available {
                    MerchDetailSectionCard(title: AppLocalized.text("shop.detail.section.unavailable", fallback: "Unavailable"), colorScheme: colorScheme) {
                        Text(AppLocalized.text("shop.detail.notice.unavailable", fallback: "This drop is currently unavailable for checkout."))
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else if !item.variants.isEmpty && resolvedVariant == nil {
                    MerchDetailSectionCard(title: AppLocalized.text("shop.detail.section.variant_missing", fallback: "Variant missing"), colorScheme: colorScheme) {
                        Text(AppLocalized.text("shop.detail.notice.variant_missing", fallback: "Selected variant is unavailable. Check size and color."))
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 28)
        }
        .scrollIndicators(.hidden)
        .accessibilityIdentifier("shop.merch.detail.root")
        .background(backgroundGradient.ignoresSafeArea())
        .navigationTitle(item.name)
        .navigationBarTitleDisplayMode(.inline)
        .skydownNavigationChrome(colorScheme: colorScheme)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                SkydownBrandActionButton(
                    title: AppLocalized.text("common.close", fallback: "Close"),
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
                .accessibilityIdentifier("shop.merch.detail.close")
            }
        }
        .safeAreaInset(edge: .bottom, spacing: SkydownLayout.stackSpacingNone) {
            VStack(spacing: SkydownLayout.stackSpacingPill) {
                Divider()
                    .overlay(AppColors.accent(for: colorScheme).opacity(0.12))

                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                        Text(AppLocalized.text("shop.detail.footer.selection", fallback: "Selection"))
                            .font(.caption2.weight(.bold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        Text(selectionSummary)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .lineLimit(1)
                    }
                    Spacer(minLength: 0)
                    VStack(alignment: .trailing, spacing: SkydownLayout.stackSpacingHairline) {
                        Text(AppLocalized.text("shop.detail.footer.total", fallback: "Total"))
                            .font(.caption2.weight(.bold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        Text(totalPriceLabel)
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                    }
                }
                .padding(.horizontal, 20)

                SkydownBrandActionButton(
                    title: orderButtonTitle,
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme,
                    isEnabled: canOrder,
                    font: .headline,
                    cornerRadius: SkydownLayout.cardCornerRadius,
                    verticalPadding: 14,
                    action: { showingConfirmSheet = true }
                )
                .frame(minHeight: 50)
                .padding(.horizontal, 20)

                Text(
                    canOrder
                        ? AppLocalized.text("shop.detail.footer.secure_buy", fallback: "Secure buy · %@").replacingOccurrences(of: "%@", with: selectionSummary)
                        : AppLocalized.text("shop.detail.footer.prebuy_check", fallback: "Before purchase: check selection and status")
                )
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.horizontal, 20)

                MerchCheckoutConfidenceStrip(
                    colorScheme: colorScheme,
                    shippingLine: shippingConfidenceLine,
                    secureLine: secureCheckoutLine,
                    supportLine: supportConfidenceLine,
                    qualityLine: qualityConfidenceLine
                )
                .padding(.horizontal, 20)
                .padding(.top, 2)

                .padding(.bottom, 14)
            }
            .background(AppColors.cardBackground(for: colorScheme).opacity(0.98))
            .animation(merchFooterAnimation, value: canOrder)
            .animation(merchFooterAnimation, value: selectedQuantity)
            .animation(merchFooterAnimation, value: selectedSize)
            .animation(merchFooterAnimation, value: selectedColor)
        }
        .sheet(isPresented: $showingConfirmSheet) {
            MerchFinalConfirmSheet(
                colorScheme: colorScheme,
                itemName: item.name,
                selectionSummary: selectionSummary,
                quantity: selectedQuantity,
                unitPriceLabel: unitPriceLabel,
                totalPriceLabel: totalPriceLabel,
                onCancel: {
                    showingConfirmSheet = false
                },
                onConfirm: addToCart,
                onContinueShopping: continueShoppingAfterSuccess,
                onGoToCart: goToCartAfterSuccess
            )
            .presentationDetents([.height(356), .medium])
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(28)
        }
        .onAppear {
            selectedSize = availableSizes.first ?? "M"
            selectedColor = availableColors.first ?? ""
        }
        .onChange(of: selectedSize) {
            if !availableColors.contains(selectedColor) {
                selectedColor = availableColors.first ?? ""
            }
        }
        .onDisappear {
            guard pendingRouteToCart else { return }
            pendingRouteToCart = false
            DispatchQueue.main.async {
                onOpenCart()
            }
        }
        .fullScreenCover(isPresented: $showingFullscreenGallery) {
            NavigationStack {
                MerchDetailFullscreenGalleryView(
                    itemName: item.name,
                    imageURLs: heroImageURLs,
                    selectedImageIndex: $selectedHeroImageIndex
                )
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        SkydownBrandActionButton(
                            title: AppLocalized.text("common.close", fallback: "Close"),
                            systemImage: "xmark",
                            accent: AppColors.accent(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .subheadline.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: false,
                            action: { showingFullscreenGallery = false }
                        )
                        .skydownInteractiveFeedback()
                    }
                }
                .toolbarBackground(.hidden, for: .navigationBar)
                .toolbarColorScheme(.dark, for: .navigationBar)
                .navigationBarTitleDisplayMode(.inline)
            }
        }
        .fancyToast(isPresented: $showToast, message: toastMessage, style: toastStyle)
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

    private var orderButtonTitle: String {
        if authManager.userSession == nil {
            return AppLocalized.text("shop.detail.cta.account_required", fallback: "Account required")
        }
        if !storeIsOpen {
            return AppLocalized.text("shop.detail.cta.store_closed", fallback: "Store paused")
        }
        if !item.available {
            return AppLocalized.text("shop.detail.cta.unavailable", fallback: "Unavailable")
        }
        return AppLocalized.text("shop.detail.cta.secure_now", fallback: "Secure now · %@").replacingOccurrences(of: "%@", with: totalPriceLabel)
    }
}

private extension String {
    var trimmedNonEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

#Preview {
    let authManager = AuthManager()
    let cartVM = CartViewModel(authManager: authManager)

    NavigationStack {
        let sampleItem = MerchandiseItem(
            id: "1",
            name: "Skydown Shirt",
            price: 29.99,
            description: "Exklusives T-Shirt in limitierter Auflage",
            imageURLs: [],
            available: true
        )
        MerchandiseDetailView(item: sampleItem, storeIsOpen: true)
            .environmentObject(authManager)
            .environmentObject(cartVM)
    }
}

private struct MerchDetailHeroCard: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let colorScheme: ColorScheme
    let itemName: String
    let price: Double
    let quantity: Int
    let isLoggedIn: Bool
    let storeIsOpen: Bool
    let isAvailable: Bool
    let imageURLs: [String]
    @Binding var selectedImageIndex: Int
    let onOpenFullscreen: () -> Void

    private var merchHeroGalleryAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .easeInOut(duration: 0.22)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
            ZStack(alignment: .bottomTrailing) {
                TabView(selection: $selectedImageIndex) {
                    ForEach(Array(imageURLs.enumerated()), id: \.offset) { index, imageURL in
                        MerchDetailHeroImage(
                            imageURL: imageURL,
                            colorScheme: colorScheme
                        )
                        .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(height: 390)
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                )

                VStack(spacing: SkydownLayout.stackSpacingMicro) {
                    Button(action: onOpenFullscreen) {
                        Image(systemName: "arrow.up.left.and.arrow.down.right")
                            .font(.caption2.weight(.bold))
                            .foregroundColor(.white)
                            .padding(8)
                            .background(Color.black.opacity(0.34))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()

                    if imageURLs.count > 1 {
                        HStack(spacing: SkydownLayout.stackSpacingNano) {
                            ForEach(imageURLs.indices, id: \.self) { index in
                                Capsule(style: .continuous)
                                    .fill(index == selectedImageIndex ? Color.white : Color.white.opacity(0.42))
                                    .frame(width: index == selectedImageIndex ? 13 : 6, height: 6)
                            }
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .background(Color.black.opacity(0.26))
                        .clipShape(Capsule(style: .continuous))
                    }
                }
                .padding(14)
            }

            HStack(spacing: SkydownLayout.stackSpacingPill) {
                MerchDetailBadge(text: String(format: "EUR %.2f", price), colorScheme: colorScheme)
                MerchDetailBadge(
                    text: isLoggedIn
                        ? AppLocalized.text("shop.detail.badge.account_active", fallback: "Account active")
                        : AppLocalized.text("shop.detail.badge.login_required", fallback: "Login required"),
                    colorScheme: colorScheme
                )
                MerchDetailBadge(
                    text: storeIsOpen && isAvailable
                        ? AppLocalized.text("shop.detail.badge.ready", fallback: "Ready")
                        : AppLocalized.text("shop.detail.badge.check", fallback: "Check"),
                    colorScheme: colorScheme
                )
            }

            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingComfortable) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    Text(AppLocalized.text("shop.detail.hero.kicker", fallback: "Merch drop"))
                        .font(.caption.weight(.bold))
                        .tracking(1.2)
                        .foregroundColor(AppColors.accent(for: colorScheme))

                    Text(itemName)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(AppLocalized.text("shop.detail.hero.subtitle", fallback: "Curated selection, ready when you are."))
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    HStack(spacing: SkydownLayout.stackSpacingMicro) {
                        Text(String(format: "EUR %.2f", price))
                            .font(.title2.weight(.bold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                        if quantity > 1 {
                            Text("· \(quantity)x = \(String(format: "EUR %.2f", Double(quantity) * price))")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    }
                }

                EmptyView()
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
            RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous))
        .animation(merchHeroGalleryAnimation, value: selectedImageIndex)
    }
}

private struct MerchVariantPill: View {
    let text: String
    let isSelected: Bool
    let selectedTint: Color
    let colorScheme: ColorScheme

    var body: some View {
        Text(text)
            .font(.subheadline.weight(.semibold))
            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(
                Capsule(style: .continuous)
                    .fill(isSelected ? selectedTint : AppColors.secondaryBackground(for: colorScheme))
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(isSelected ? selectedTint : selectedTint.opacity(0.14), lineWidth: 1)
            )
    }
}

private struct MerchCheckoutConfidenceStrip: View {
    let colorScheme: ColorScheme
    let shippingLine: String
    let secureLine: String
    let supportLine: String
    let qualityLine: String

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            confidenceRow(icon: "shippingbox", text: shippingLine)
            confidenceRow(icon: "lock.shield", text: secureLine)
            confidenceRow(icon: "message", text: supportLine)
            confidenceRow(icon: "checkmark.seal", text: qualityLine)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.72))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
    }

    private func confidenceRow(icon: String, text: String) -> some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
            Image(systemName: icon)
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.accent(for: colorScheme).opacity(0.85))
                .padding(.top, 1)
            Text(text)
                .font(.caption2.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct MerchFinalConfirmSheet: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let colorScheme: ColorScheme
    let itemName: String
    let selectionSummary: String
    let quantity: Int
    let unitPriceLabel: String
    let totalPriceLabel: String
    let onCancel: () -> Void
    let onConfirm: () -> Void
    let onContinueShopping: () -> Void
    let onGoToCart: () -> Void
    @State private var confirmState: ConfirmState = .ready

    private enum ConfirmState {
        case ready
        case committing
        case success
    }

    private var successBlockTransition: AnyTransition {
        reduceMotion ? .opacity : .move(edge: .bottom).combined(with: .opacity)
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSection) {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                Text(AppLocalized.text("shop.detail.confirm.subtitle", fallback: "Quickly review your selection before adding to cart."))
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                detailRow(title: AppLocalized.text("shop.detail.confirm.item", fallback: "Item"), value: itemName)
                detailRow(title: AppLocalized.text("shop.detail.confirm.selection", fallback: "Selection"), value: selectionSummary)
                detailRow(title: AppLocalized.text("shop.detail.confirm.unit_price", fallback: "Unit price"), value: unitPriceLabel)
                detailRow(title: AppLocalized.text("shop.detail.confirm.total", fallback: "Total"), value: totalPriceLabel, emphasize: true)
            }
            .padding(14)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))

            if confirmState == .success {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                    HStack(spacing: SkydownLayout.stackSpacingMicro) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                        Text(AppLocalized.text("shop.detail.confirm.success", fallback: "Cart updated. Choose your next step."))
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        Spacer(minLength: 0)
                        Image(systemName: "cart.fill")
                            .font(.caption.weight(.bold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                            .scaleEffect(confirmState == .success ? 1.08 : 1.0)
                    }

                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        SkydownBrandActionButton(
                            title: AppLocalized.text("shop.detail.confirm.continue_shopping", fallback: "Continue shopping"),
                            accent: AppColors.accent(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .subheadline.weight(.semibold),
                            cornerRadius: SkydownLayout.compactRadius,
                            verticalPadding: 12,
                            action: onContinueShopping
                        )
                        .frame(minHeight: 44)

                        SkydownBrandActionButton(
                            title: AppLocalized.text("shop.detail.confirm.go_to_cart", fallback: "Go to cart"),
                            accent: AppColors.accent(for: colorScheme),
                            colorScheme: colorScheme,
                            font: .subheadline.weight(.bold),
                            cornerRadius: SkydownLayout.compactRadius,
                            verticalPadding: 12,
                            action: onGoToCart
                        )
                        .frame(minHeight: 44)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.72))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous))
                .transition(successBlockTransition)
            }

            if confirmState != .success {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.cancel", fallback: "Cancel"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        isEnabled: confirmState == .ready,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 12,
                        action: onCancel
                    )
                    .frame(minHeight: 46)

                    SkydownBrandActionButton(
                        title: confirmButtonTitle,
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        isEnabled: confirmState == .ready || confirmState == .committing,
                        isLoading: confirmState == .committing,
                        font: .subheadline.weight(.bold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 12,
                        action: handleConfirmTap
                    )
                    .frame(minHeight: 46)
                }
            }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, SkydownLayout.cardPadding)
            .background(AppColors.cardBackground(for: colorScheme))
            .animation(SkydownMotion.preferredStatusTransition(accessibilityReduceMotion: reduceMotion), value: confirmState)
            .navigationTitle(AppLocalized.text("shop.detail.confirm.title", fallback: "Confirm order"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.done", fallback: "Done"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        isEnabled: confirmState != .committing,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: onCancel
                    )
                    .skydownInteractiveFeedback()
                }
            }
        }
    }

    private func detailRow(title: String, value: String, emphasize: Bool = false) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Spacer(minLength: 12)
            Text(value)
                .font(emphasize ? .subheadline.weight(.bold) : .subheadline.weight(.semibold))
                .foregroundColor(emphasize ? AppColors.accent(for: colorScheme) : AppColors.text(for: colorScheme))
                .multilineTextAlignment(.trailing)
                .lineLimit(2)
        }
    }

    private var confirmButtonTitle: String {
        switch confirmState {
        case .ready:
            let base = AppLocalized.text("shop.detail.confirm.button.secure_now", fallback: "Secure now")
            return quantity > 1 ? "\(base) (\(quantity)x)" : base
        case .committing:
            return AppLocalized.text("shop.detail.confirm.button.adding", fallback: "Adding...")
        case .success:
            return AppLocalized.text("shop.detail.confirm.button.added", fallback: "Added")
        }
    }

    private func handleConfirmTap() {
        guard confirmState == .ready else { return }
        let commitEase = reduceMotion ? Animation.linear(duration: 0.01) : Animation.easeInOut(duration: 0.16)
        let successEase = reduceMotion ? Animation.linear(duration: 0.01) : SkydownMotion.screenTransition
        withAnimation(commitEase) {
            confirmState = .committing
        }
        let stepDelay = reduceMotion ? 0.0 : 0.18
        DispatchQueue.main.asyncAfter(deadline: .now() + stepDelay) {
            withAnimation(successEase) {
                confirmState = .success
            }
            SkydownHaptics.notification(.success)
            let confirmDelay = reduceMotion ? 0.0 : 0.26
            DispatchQueue.main.asyncAfter(deadline: .now() + confirmDelay) {
                onConfirm()
            }
        }
    }
}

private struct MerchDetailHeroImage: View {
    let imageURL: String
    let colorScheme: ColorScheme

    var body: some View {
        Group {
            if let url = merchImageURL(imageURL) {
                AsyncImage(url: url, transaction: Transaction(animation: nil)) { phase in
                    switch phase {
                    case .empty:
                        heroPlaceholder
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        heroPlaceholder
                    @unknown default:
                        heroPlaceholder
                    }
                }
            } else {
                heroPlaceholder
            }
        }
        .overlay(
            LinearGradient(
                colors: [
                    .clear,
                    Color.black.opacity(0.06),
                    Color.black.opacity(0.28)
                ],
                startPoint: .center,
                endPoint: .bottom
            )
        )
    }

    private var heroPlaceholder: some View {
        RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
            .fill(AppColors.secondaryBackground(for: colorScheme))
            .overlay {
                Image(systemName: "photo")
                    .font(.title2.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.34))
            }
    }
}

private struct MerchDetailFullscreenGalleryView: View {
    let itemName: String
    let imageURLs: [String]
    @Binding var selectedImageIndex: Int

    var body: some View {
        TabView(selection: $selectedImageIndex) {
            ForEach(Array(imageURLs.enumerated()), id: \.offset) { index, imageURL in
                ZStack {
                    Color.black.ignoresSafeArea()
                    MerchGalleryImage(urlString: imageURL)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                .tag(index)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .always))
        .background(Color.black.ignoresSafeArea())
        .navigationTitle(itemName)
        .navigationBarTitleDisplayMode(.inline)
        .skydownNavigationChrome(colorScheme: .dark)
    }
}

private func merchImageURL(_ raw: String) -> URL? {
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty, let url = URL(string: trimmed) else { return nil }
    return url
}

private struct MerchGalleryImage: View {
    let urlString: String

    var body: some View {
        Group {
            if let url = merchImageURL(urlString) {
                AsyncImage(url: url, transaction: Transaction(animation: nil)) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                            .tint(.white)
                            .scaleEffect(0.9)
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                    case .failure:
                        Image(systemName: "photo")
                            .font(.title2)
                            .foregroundColor(.white.opacity(0.5))
                    @unknown default:
                        Image(systemName: "photo")
                            .font(.title2)
                            .foregroundColor(.white.opacity(0.5))
                    }
                }
            } else {
                Image(systemName: "photo")
                    .font(.title2)
                    .foregroundColor(.white.opacity(0.5))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black)
    }
}

private struct MerchDetailReadinessStrip: View {
    let colorScheme: ColorScheme
    let readinessTitle: String
    let readinessDetail: String
    let accountTitle: String
    let accountDetail: String
    let selectionTitle: String
    let selectionDetail: String

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            Text(AppLocalized.text("shop.detail.overview.title", fallback: "Overview"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(AppLocalized.text("shop.detail.overview.subtitle", fallback: "Selection, account, and availability."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ViewThatFits(in: .horizontal) {
                HStack(spacing: SkydownLayout.stackSpacingCompact) {
                    statusCard(title: readinessTitle, detail: readinessDetail, accent: AppColors.accent(for: colorScheme))
                    statusCard(title: accountTitle, detail: accountDetail, accent: AppColors.accentMystic(for: colorScheme))
                    statusCard(title: selectionTitle, detail: selectionDetail, accent: AppColors.accentHighlight(for: colorScheme))
                }

                VStack(spacing: SkydownLayout.stackSpacingCompact) {
                    statusCard(title: readinessTitle, detail: readinessDetail, accent: AppColors.accent(for: colorScheme))
                    statusCard(title: accountTitle, detail: accountDetail, accent: AppColors.accentMystic(for: colorScheme))
                    statusCard(title: selectionTitle, detail: selectionDetail, accent: AppColors.accentHighlight(for: colorScheme))
                }
            }
        }
    }

    private func statusCard(title: String, detail: String, accent: Color) -> some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(title)
                .font(.headline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(2)

            Text(detail)
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .lineLimit(3)
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    accent.opacity(colorScheme == .dark ? 0.18 : 0.10)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(accent.opacity(0.20), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
    }
}

private struct MerchDetailTrustModule: View {
    let colorScheme: ColorScheme
    let availability: String
    let shipping: String
    let support: String

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(spacing: SkydownLayout.stackSpacingMicro) {
                Text(AppLocalized.text("shop.detail.trust.title", fallback: "Trust"))
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                    )
                Text("Kaufklarheit")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            trustRow("checkmark.seal.fill", availability)
            trustRow("shippingbox.fill", shipping)
            trustRow("message.fill", support)
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
    }

    private func trustRow(_ icon: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
            Image(systemName: icon)
                .font(.caption.weight(.bold))
                .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                .padding(.top, 1)
            Text(text)
                .font(.caption.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct MerchDetailSectionCard<Content: View>: View {
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

private struct MerchDetailBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.accent(for: colorScheme)
        )
    }
}
