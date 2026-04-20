//
//  ContactFormView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI

struct ContactFormView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var cartVM: CartViewModel
    @EnvironmentObject var authManager: AuthManager
    let item: MerchandiseItem
    let storeIsOpen: Bool
    
    @Environment(\.colorScheme) private var colorScheme
    @State private var alertType: AlertType?

    @State private var selectedSize = "M"
    @State private var selectedColor = ""
    @State private var selectedQuantity = 1
    private let availableQuantities = Array(1...10)
    
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success

    private enum AlertType: Identifiable {
        case confirm
        var id: Int { hashValue }
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

    private var isFormValid: Bool {
        !selectedSize.isEmpty &&
        selectedQuantity > 0 &&
        (item.variants.isEmpty || resolvedVariant != nil)
    }
    private var canOrder: Bool {
        isFormValid && authManager.userSession != nil && storeIsOpen && item.available
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

    private var readinessTitle: String {
        if canOrder {
            return "Ready"
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
            return "Direkt in den Warenkorb"
        }
        if authManager.userSession == nil {
            return "Einloggen und im App-Flow bleiben"
        }
        if !storeIsOpen {
            return "Der Drop ist gerade geschlossen"
        }
        if !item.available {
            return "Dieses Produkt ist aktuell offline"
        }
        return "Bitte Variante vervollstaendigen"
    }
    
    private func addToCart() {
        cartVM.addItem(
            item,
            size: selectedSize,
            color: selectedColor.trimmedNonEmpty,
            quantity: selectedQuantity,
            resolvedVariant: resolvedVariant
        )
        toastMessage = "Artikel erfolgreich zum Warenkorb hinzugefügt"
        toastStyle = .success
        showToast = true
        alertType = nil
        dismiss()
    }
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                ContactHeroCard(
                    colorScheme: colorScheme,
                    itemName: item.name,
                    price: item.price,
                    isLoggedIn: authManager.userSession != nil,
                    storeIsOpen: storeIsOpen,
                    isAvailable: item.available
                )

                ContactReadinessStrip(
                    colorScheme: colorScheme,
                    readinessTitle: readinessTitle,
                    readinessDetail: readinessDetail,
                    accountTitle: authManager.userSession != nil ? "Aktiv" : "Login noetig",
                    accountDetail: authManager.userSession != nil ? "Checkout bleibt in der App" : "Danach bleibt der Bestellweg direkt im Flow",
                    selectionTitle: selectionSummary,
                    selectionDetail: optionSummary
                )

                if let story = item.description.trimmedNonEmpty {
                    ContactSectionCard(title: "Drop Story", colorScheme: colorScheme) {
                        Text(story)
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        HStack(spacing: 10) {
                            ContactBadge(text: storeIsOpen ? "Store live" : "Store pausiert", colorScheme: colorScheme)
                            ContactBadge(text: item.available ? "Produkt sichtbar" : "Aktuell offline", colorScheme: colorScheme)
                        }
                    }
                }

                ContactSectionCard(title: "Produkt", colorScheme: colorScheme) {
                    MerchandiseItemView(item: item)
                }

                ContactSectionCard(title: "Größe", colorScheme: colorScheme) {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 64), spacing: 10)], spacing: 10) {
                        ForEach(availableSizes, id: \.self) { size in
                            Button {
                                selectedSize = size
                            } label: {
                                Text(size)
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                            }
                            .buttonStyle(.plain)
                            .skydownTactileAction()
                            .background(
                                RoundedRectangle(cornerRadius: 16)
                                    .fill(
                                        selectedSize == size
                                        ? AppColors.accent(for: colorScheme)
                                        : AppColors.secondaryBackground(for: colorScheme)
                                    )
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(
                                        selectedSize == size
                                        ? AppColors.accent(for: colorScheme)
                                        : AppColors.accent(for: colorScheme).opacity(0.14),
                                        lineWidth: 1
                                    )
                            )
                            .foregroundColor(selectedSize == size ? .white : AppColors.text(for: colorScheme))
                        }
                    }
                }

                if !availableColors.isEmpty {
                    ContactSectionCard(title: "Farbe", colorScheme: colorScheme) {
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 88), spacing: 10)], spacing: 10) {
                            ForEach(availableColors, id: \.self) { color in
                                Button {
                                    selectedColor = color
                                } label: {
                                    Text(color)
                                        .font(.headline)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                }
                                .buttonStyle(.plain)
                                .skydownTactileAction()
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(
                                            selectedColor == color
                                            ? AppColors.accentMystic(for: colorScheme)
                                            : AppColors.secondaryBackground(for: colorScheme)
                                        )
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(
                                            selectedColor == color
                                            ? AppColors.accentMystic(for: colorScheme)
                                            : AppColors.accentMystic(for: colorScheme).opacity(0.14),
                                            lineWidth: 1
                                        )
                                )
                                .foregroundColor(selectedColor == color ? .white : AppColors.text(for: colorScheme))
                            }
                        }
                    }
                }

                ContactSectionCard(title: "Anzahl", colorScheme: colorScheme) {
                    HStack(spacing: 14) {
                        Button {
                            if selectedQuantity > 1 { selectedQuantity -= 1 }
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
                    ContactSectionCard(title: "Hinweis", colorScheme: colorScheme) {
                        Text("Zum Hinzufügen in den Warenkorb brauchst du ein Konto. Danach bleibt der Flow direkt in der App.")
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else if !storeIsOpen {
                    ContactSectionCard(title: "Store pausiert", colorScheme: colorScheme) {
                        Text("Der Merch Store ist aktuell geschlossen. Produkte bleiben sichtbar, neue Kaeufe sind voruebergehend pausiert.")
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else if !item.available {
                    ContactSectionCard(title: "Nicht verfuegbar", colorScheme: colorScheme) {
                        Text("Dieses Produkt ist aktuell nicht kaufbar. Sobald der Drop wieder live ist, kannst du es direkt bestellen.")
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else if !item.variants.isEmpty && resolvedVariant == nil {
                    ContactSectionCard(title: "Variante fehlt", colorScheme: colorScheme) {
                        Text("Diese Auswahl ist aktuell nicht verfuegbar. Bitte pruefe Größe und Farbe noch einmal.")
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

                Button(orderButtonTitle) {
                    if canOrder {
                        alertType = .confirm
                    }
                }
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    canOrder
                    ? AppColors.accent(for: colorScheme)
                    : Color.gray
                )
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .disabled(!canOrder)
                .padding(.horizontal, 20)
                .padding(.bottom, 12)
            }
            .background(AppColors.cardBackground(for: colorScheme).opacity(0.98))
        }
        .alert(item: $alertType) { _ in
            Alert(
                title: Text("Artikel in Warenkorb legen"),
                message: Text("Der Artikel wird deinem Warenkorb hinzugefügt."),
                primaryButton: .default(Text("Einverstanden"), action: addToCart),
                secondaryButton: .cancel()
            )
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
            return "Account erforderlich"
        }
        if !storeIsOpen {
            return "Store aktuell geschlossen"
        }
        if !item.available {
            return "Aktuell nicht verfuegbar"
        }
        return String(format: "In Warenkorb · EUR %.2f", item.price)
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
        ContactFormView(item: sampleItem, storeIsOpen: true)
            .environmentObject(authManager)
            .environmentObject(cartVM)
    }
}

private struct ContactHeroCard: View {
    let colorScheme: ColorScheme
    let itemName: String
    let price: Double
    let isLoggedIn: Bool
    let storeIsOpen: Bool
    let isAvailable: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 10) {
                ContactBadge(text: String(format: "EUR %.2f", price), colorScheme: colorScheme)
                ContactBadge(text: isLoggedIn ? "Konto aktiv" : "Login noetig", colorScheme: colorScheme)
                ContactBadge(text: storeIsOpen && isAvailable ? "Ready to order" : "Drop check", colorScheme: colorScheme)
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

                    Text("Waehle Variante, Menge und Checkout in einem kompakten In-App-Flow, der direkt kommuniziert und nicht aus der App herausfuehrt.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                ZStack {
                    RoundedRectangle(cornerRadius: 18)
                        .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                        .frame(width: 58, height: 58)

                    Image(systemName: "bag.badge.plus")
                        .font(.title2)
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
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
    }
}

private struct ContactReadinessStrip: View {
    let colorScheme: ColorScheme
    let readinessTitle: String
    let readinessDetail: String
    let accountTitle: String
    let accountDetail: String
    let selectionTitle: String
    let selectionDetail: String

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Bestell-Ueberblick")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Die wichtigsten Signale liegen vor dem Formular, damit der Flow schnell lesbar bleibt.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ViewThatFits(in: .horizontal) {
                HStack(spacing: 12) {
                    statusCard(title: readinessTitle, detail: readinessDetail, accent: AppColors.accent(for: colorScheme))
                    statusCard(title: accountTitle, detail: accountDetail, accent: AppColors.accentMystic(for: colorScheme))
                    statusCard(title: selectionTitle, detail: selectionDetail, accent: AppColors.spotify(for: colorScheme))
                }

                VStack(spacing: 12) {
                    statusCard(title: readinessTitle, detail: readinessDetail, accent: AppColors.accent(for: colorScheme))
                    statusCard(title: accountTitle, detail: accountDetail, accent: AppColors.accentMystic(for: colorScheme))
                    statusCard(title: selectionTitle, detail: selectionDetail, accent: AppColors.spotify(for: colorScheme))
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

private struct ContactSectionCard<Content: View>: View {
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

private struct ContactBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.accent(for: colorScheme)
        )
    }
}
