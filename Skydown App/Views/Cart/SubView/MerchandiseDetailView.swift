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
    @State private var showingConfirmSheet = false

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
            return "\(availableSizes.count) Groessen • \(colorCount) Farben"
        }
        return "\(availableSizes.count) Groessen"
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
            return "Bereit"
        }
        if authManager.userSession == nil {
            return "Login"
        }
        if !storeIsOpen {
            return "Store pausiert"
        }
        if !item.available {
            return "Nicht live"
        }
        return "Auswahl pruefen"
    }

    private var readinessDetail: String {
        if canOrder {
            return "Bereit für Warenkorb"
        }
        if authManager.userSession == nil {
            return "Login nötig"
        }
        if !storeIsOpen {
            return "Drop zu"
        }
        if !item.available {
            return "Offline"
        }
        return "Variante wählen"
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
        toastMessage = "Im Warenkorb aktualisiert"
        toastStyle = .success
        showToast = true
    }

    private func goToCartAfterSuccess() {
        showingConfirmSheet = false
        toastMessage = "Im Warenkorb aktualisiert"
        toastStyle = .success
        showToast = true
        pendingRouteToCart = true
        dismiss()
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
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
                    accountTitle: authManager.userSession != nil ? "Aktiv" : "Login",
                    accountDetail: authManager.userSession != nil ? "Checkout in-app" : "Dann bestellen",
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
                    MerchDetailSectionCard(title: "Story", colorScheme: colorScheme) {
                        Text(story)
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        HStack(spacing: 10) {
                            MerchDetailBadge(text: storeIsOpen ? "Live" : "Pause", colorScheme: colorScheme)
                            MerchDetailBadge(text: item.available ? "Sichtbar" : "Offline", colorScheme: colorScheme)
                        }
                    }
                }

                MerchDetailSectionCard(title: "Produkt", colorScheme: colorScheme) {
                    MerchandiseItemView(item: item)
                }

                MerchDetailSectionCard(title: "Größe", colorScheme: colorScheme) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
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
                                .animation(.easeInOut(duration: 0.16), value: selectedSize)
                            }
                        }
                    }
                }

                if !availableColors.isEmpty {
                    MerchDetailSectionCard(title: "Farbe", colorScheme: colorScheme) {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
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
                                    .animation(.easeInOut(duration: 0.16), value: selectedColor)
                                }
                            }
                        }
                    }
                }

                MerchDetailSectionCard(title: "Anzahl", colorScheme: colorScheme) {
                    HStack(spacing: 14) {
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

                        VStack(spacing: 4) {
                            Text("\(selectedQuantity)")
                                .font(.title2.weight(.bold))
                                .foregroundColor(AppColors.text(for: colorScheme))

                            Text("Stück")
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
                    MerchDetailSectionCard(title: "Hinweis", colorScheme: colorScheme) {
                        Text("Warenkorb = Konto nötig. Alles in-app.")
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else if !storeIsOpen {
                    MerchDetailSectionCard(title: "Store pausiert", colorScheme: colorScheme) {
                        Text("Store zu — sichtbar, kein Kauf.")
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else if !item.available {
                    MerchDetailSectionCard(title: "Nicht verfuegbar", colorScheme: colorScheme) {
                        Text("Nicht kaufbar — bei Live-Drop wieder.")
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else if !item.variants.isEmpty && resolvedVariant == nil {
                    MerchDetailSectionCard(title: "Variante fehlt", colorScheme: colorScheme) {
                        Text("Variante nicht verfügbar — Größe/Farbe prüfen.")
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
                Button("Schliessen") {
                    dismiss()
                }
                .accessibilityIdentifier("shop.merch.detail.close")
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            VStack(spacing: 10) {
                Divider()
                    .overlay(AppColors.accent(for: colorScheme).opacity(0.12))

                HStack(spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Auswahl")
                            .font(.caption2.weight(.bold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        Text(selectionSummary)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .lineLimit(1)
                    }
                    Spacer(minLength: 0)
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("Gesamt")
                            .font(.caption2.weight(.bold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        Text(totalPriceLabel)
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                    }
                }
                .padding(.horizontal, 20)

                Button(orderButtonTitle) {
                    if canOrder {
                        SkydownHaptics.selection()
                        showingConfirmSheet = true
                    }
                }
                .font(.headline)
                .frame(minHeight: 44)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    canOrder
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                )
                .foregroundColor(
                    canOrder
                        ? .white
                        : AppColors.secondaryText(for: colorScheme)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(
                            canOrder
                                ? Color.clear
                                : AppColors.accent(for: colorScheme).opacity(0.12),
                            lineWidth: 1
                        )
                )
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .disabled(!canOrder)
                .padding(.horizontal, 20)

                Text(canOrder ? "Sicher kaufen · \(selectionSummary)" : "Vor dem Kauf: Auswahl und Status pruefen")
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

                .padding(.bottom, 14)
            }
            .background(AppColors.cardBackground(for: colorScheme).opacity(0.98))
            .animation(.easeInOut(duration: 0.18), value: canOrder)
            .animation(.easeInOut(duration: 0.18), value: selectedQuantity)
            .animation(.easeInOut(duration: 0.18), value: selectedSize)
            .animation(.easeInOut(duration: 0.18), value: selectedColor)
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
                        Button("Schliessen") {
                            showingFullscreenGallery = false
                        }
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
            return "Konto nötig"
        }
        if !storeIsOpen {
            return "Store zu"
        }
        if !item.available {
            return "Nicht verfügbar"
        }
        return "Jetzt sichern · \(totalPriceLabel)"
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

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
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
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                )

                VStack(spacing: 8) {
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
                        HStack(spacing: 4) {
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

            HStack(spacing: 10) {
                MerchDetailBadge(text: String(format: "EUR %.2f", price), colorScheme: colorScheme)
                MerchDetailBadge(text: isLoggedIn ? "Konto aktiv" : "Login noetig", colorScheme: colorScheme)
                MerchDetailBadge(text: storeIsOpen && isAvailable ? "Bereit" : "Prüfen", colorScheme: colorScheme)
            }

            HStack(alignment: .top, spacing: 16) {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Merch Drop")
                        .font(.caption.weight(.bold))
                        .tracking(1.2)
                        .foregroundColor(AppColors.accent(for: colorScheme))

                    Text(itemName)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Jetzt kuratiert sichern.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    HStack(spacing: 8) {
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
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .animation(.easeInOut(duration: 0.22), value: selectedImageIndex)
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
        VStack(alignment: .leading, spacing: 8) {
            confidenceRow(icon: "shippingbox", text: shippingLine)
            confidenceRow(icon: "lock.shield", text: secureLine)
            confidenceRow(icon: "message", text: supportLine)
            confidenceRow(icon: "checkmark.seal", text: qualityLine)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.72))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func confidenceRow(icon: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
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

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Kauf bestaetigen")
                    .font(.title3.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text("Pruefe kurz deine Auswahl. Danach liegt der Artikel im Warenkorb.")
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 10) {
                detailRow(title: "Artikel", value: itemName)
                detailRow(title: "Auswahl", value: selectionSummary)
                detailRow(title: "Stueckpreis", value: unitPriceLabel)
                detailRow(title: "Gesamt", value: totalPriceLabel, emphasize: true)
            }
            .padding(14)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            if confirmState == .success {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                        Text("Warenkorb aktualisiert. Waehle deinen naechsten Schritt.")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        Spacer(minLength: 0)
                        Image(systemName: "cart.fill")
                            .font(.caption.weight(.bold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                            .scaleEffect(confirmState == .success ? 1.08 : 1.0)
                    }

                    HStack(spacing: 10) {
                        Button("Weiter shoppen") {
                            SkydownHaptics.selection()
                            onContinueShopping()
                        }
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .background(AppColors.cardBackground(for: colorScheme))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                        Button("Zum Warenkorb") {
                            SkydownHaptics.notification(.success)
                            onGoToCart()
                        }
                        .font(.subheadline.weight(.bold))
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .background(AppColors.accent(for: colorScheme))
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(AppColors.secondaryBackground(for: colorScheme).opacity(0.72))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }

            if confirmState != .success {
                HStack(spacing: 10) {
                    Button("Abbrechen", action: onCancel)
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity, minHeight: 46)
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .disabled(confirmState != .ready)
                        .opacity(confirmState == .ready ? 1.0 : 0.6)

                    Button(confirmButtonTitle, action: handleConfirmTap)
                        .font(.subheadline.weight(.bold))
                        .frame(maxWidth: .infinity, minHeight: 46)
                        .background(confirmButtonBackground)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .disabled(confirmState != .ready)
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 16)
        .background(AppColors.cardBackground(for: colorScheme))
        .animation(.easeInOut(duration: 0.2), value: confirmState)
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
            return quantity > 1 ? "Jetzt sichern (\(quantity)x)" : "Jetzt sichern"
        case .committing:
            return "Wird hinzugefuegt..."
        case .success:
            return "Hinzugefuegt"
        }
    }

    private var confirmButtonBackground: Color {
        switch confirmState {
        case .ready:
            return AppColors.accent(for: colorScheme)
        case .committing:
            return AppColors.accent(for: colorScheme).opacity(0.82)
        case .success:
            return AppColors.accentHighlight(for: colorScheme)
        }
    }

    private func handleConfirmTap() {
        guard confirmState == .ready else { return }
        SkydownHaptics.selection()
        withAnimation(.easeInOut(duration: 0.16)) {
            confirmState = .committing
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) {
            withAnimation(.spring(response: 0.28, dampingFraction: 0.86)) {
                confirmState = .success
            }
            SkydownHaptics.notification(.success)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.26) {
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
        RoundedRectangle(cornerRadius: 24, style: .continuous)
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
        VStack(alignment: .leading, spacing: 12) {
            Text("Überblick")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Auswahl, Konto und Verfügbarkeit.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ViewThatFits(in: .horizontal) {
                HStack(spacing: 12) {
                    statusCard(title: readinessTitle, detail: readinessDetail, accent: AppColors.accent(for: colorScheme))
                    statusCard(title: accountTitle, detail: accountDetail, accent: AppColors.accentMystic(for: colorScheme))
                    statusCard(title: selectionTitle, detail: selectionDetail, accent: AppColors.accentHighlight(for: colorScheme))
                }

                VStack(spacing: 12) {
                    statusCard(title: readinessTitle, detail: readinessDetail, accent: AppColors.accent(for: colorScheme))
                    statusCard(title: accountTitle, detail: accountDetail, accent: AppColors.accentMystic(for: colorScheme))
                    statusCard(title: selectionTitle, detail: selectionDetail, accent: AppColors.accentHighlight(for: colorScheme))
                }
            }
        }
    }

    private func statusCard(title: String, detail: String, accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(2)

            Text(detail)
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .lineLimit(3)
        }
        .padding(16)
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
            RoundedRectangle(cornerRadius: 20)
                .stroke(accent.opacity(0.20), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct MerchDetailTrustModule: View {
    let colorScheme: ColorScheme
    let availability: String
    let shipping: String
    let support: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text("Trust")
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
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func trustRow(_ icon: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
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
