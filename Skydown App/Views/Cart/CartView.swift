//
//  CartView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import SwiftUI
import MessageUI

struct CartView: View {
    @EnvironmentObject var cartVM: CartViewModel
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    let onOpenSettings: () -> Void = {}

    @State private var name = ""
    @State private var email = ""
    @State private var whatsApp = ""
    @State private var message = "Ich interessiere mich für die Artikel in meinem Warenkorb."
    @State private var showConfirmationDialog = false
    @State private var isSubmitting = false
    @State private var showingLoginSheet = false
    @State private var orderMailDraft: OrderMailDraft?

    private var isFormValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        !email.trimmingCharacters(in: .whitespaces).isEmpty &&
        !cartVM.items.isEmpty
    }

    private var totalPrice: Double {
        cartVM.items.reduce(0.0) { partialResult, cartItem in
            partialResult + cartItem.item.price * Double(cartItem.quantity)
        }
    }
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    CartHeroCard(
                        colorScheme: colorScheme,
                        itemCount: cartVM.items.count,
                        totalPrice: totalPrice,
                        isLoggedIn: authManager.userSession != nil
                    )

                    if authManager.userSession == nil {
                        CartSectionCard(
                            title: "Konto erforderlich",
                            colorScheme: colorScheme
                        ) {
                            Text("Melde dich an, damit du deinen Warenkorb speichern und die Bestellung abschicken kannst.")
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            Button {
                                showingLoginSheet = true
                            } label: {
                                Label("Anmelden", systemImage: "person.crop.circle.fill.badge.plus")
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(AppColors.accent(for: colorScheme))
                            .padding(.top, 4)
                        }
                    } else {
                        CartSectionCard(
                            title: "Deine Auswahl",
                            colorScheme: colorScheme
                        ) {
                            if cartVM.items.isEmpty {
                                Text("Dein Warenkorb ist leer. Sobald du etwas hinzufügst, erscheint es hier direkt als Karte.")
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            } else {
                                ForEach(cartVM.items) { cartItem in
                                    CartItemCard(
                                        cartItem: cartItem,
                                        colorScheme: colorScheme
                                    ) {
                                        cartVM.removeItem(cartItem)
                                    }
                                }
                            }
                        }

                        CartSectionCard(
                            title: "Kontaktdaten",
                            colorScheme: colorScheme
                        ) {
                            Text("Diese Angaben helfen uns beim Rückkontakt zu deiner Bestellung.")
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            VStack(spacing: 10) {
                                CartInputField(
                                    title: "Name*",
                                    text: $name,
                                    colorScheme: colorScheme
                                )
                                CartInputField(
                                    title: "E-Mail*",
                                    text: $email,
                                    colorScheme: colorScheme,
                                    keyboard: .emailAddress,
                                    autocapitalization: .never
                                )
                                CartInputField(
                                    title: "WhatsApp (optional)",
                                    text: $whatsApp,
                                    colorScheme: colorScheme,
                                    keyboard: .phonePad
                                )
                            }
                            .padding(.top, 4)
                        }

                        CartSectionCard(
                            title: "Nachricht",
                            colorScheme: colorScheme
                        ) {
                            Text("Optional für Hinweise zu Lieferung, Verfügbarkeit oder Sonderwünschen.")
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            TextEditor(text: $message)
                                .frame(minHeight: 120)
                                .padding(12)
                                .background(AppColors.secondaryBackground(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 18))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18)
                                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                                )
                                .padding(.top, 4)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 20)
                .padding(.bottom, 28)
            }
            .scrollIndicators(.hidden)
            .background(backgroundGradient.ignoresSafeArea())
            .navigationTitle("Warenkorb")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    AppSessionToolbarActions(onOpenSettings: onOpenSettings)
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if authManager.userSession != nil {
                    CartSubmitBar(
                        colorScheme: colorScheme,
                        totalPrice: totalPrice,
                        isFormValid: isFormValid,
                        isSubmitting: isSubmitting,
                        onSubmit: {
                            showConfirmationDialog = true
                        }
                    )
                }
            }
            .onAppear(perform: populateContactDetails)
        }
        .sheet(isPresented: $showingLoginSheet) {
            LoginView()
        }
        .sheet(item: $orderMailDraft) { draft in
            MailView(
                subject: draft.subject,
                body: draft.body,
                recipients: [supportMailbox],
                preferredSendingEmailAddress: draft.preferredSendingEmailAddress
            )
        }
        .confirmationDialog(
            "Bestellung abschicken",
            isPresented: $showConfirmationDialog,
            titleVisibility: .visible
        ) {
            Button("Einverstanden") {
                Task {
                    await submitOrderAsync()
                }
            }
            Button("Abbrechen", role: .cancel) {}
        } message: {
            Text("Sie werden in den nächsten Minuten per E-Mail oder WhatsApp kontaktiert.")
        }
        .fancyToast(isPresented: $cartVM.showToast,
                    message: cartVM.toastMessage,
                    style: cartVM.toastStyle)
    }

    private func submitOrderAsync() async {
        guard !isSubmitting else { return }
        isSubmitting = true
        let draft = makeOrderMailDraft(items: cartVM.items)

        let didSubmit = await cartVM.submitCartAsOrder(
            customerName: name,
            customerEmail: email,
            whatsApp: whatsApp,
            message: message
        )

        if didSubmit {
            presentOrderMailDraft(draft)
        }

        isSubmitting = false
    }

    private func populateContactDetails() {
        guard let user = authManager.userSession else { return }

        if name.isEmpty { name = user.username }
        if email.isEmpty { email = user.email }
        if whatsApp.isEmpty { whatsApp = user.whatsApp ?? "" }
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

    private var supportMailbox: String {
        "skydownent@gmail.com"
    }

    private func makeOrderMailDraft(items: [CartItem]) -> OrderMailDraft {
        let preferredEmail = authManager.userSession?.email
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .takeIfNotBlank()
        let itemSummary = items.isEmpty
            ? "- Keine Artikel"
            : items.map { cartItem in
                let linePrice = cartItem.item.price * Double(cartItem.quantity)
                return "- \(cartItem.item.name) | Groesse: \(cartItem.size) | Menge: \(cartItem.quantity) | Preis: \(String(format: "EUR %.2f", linePrice))"
            }.joined(separator: "\n")
        let orderTotal = items.reduce(0.0) { partialResult, cartItem in
            partialResult + cartItem.item.price * Double(cartItem.quantity)
        }
        let subject = preferredEmail.map { "Neue Bestellung - \($0)" } ?? "Neue Bestellung"
        let body = """
        Hallo Skydown-Team,

        es wurde eine neue Bestellung in der Skydown App vorbereitet.

        Name: \(name.isEmpty ? "Nicht angegeben" : name)
        E-Mail: \(email.isEmpty ? "Nicht angegeben" : email)
        WhatsApp: \(whatsApp.isEmpty ? "Nicht angegeben" : whatsApp)

        Warenkorb:
        \(itemSummary)

        Gesamt: \(String(format: "EUR %.2f", orderTotal))

        Nachricht:
        \(message.isEmpty ? "Keine zusaetzliche Nachricht." : message)
        """

        return OrderMailDraft(
            subject: subject,
            body: body,
            preferredSendingEmailAddress: preferredEmail
        )
    }

    private func presentOrderMailDraft(_ draft: OrderMailDraft) {
        if MFMailComposeViewController.canSendMail() {
            orderMailDraft = draft
            return
        }

        let encodedSubject = draft.subject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let encodedBody = draft.body.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        if let url = URL(string: "mailto:\(supportMailbox)?subject=\(encodedSubject)&body=\(encodedBody)"),
           UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
        }
    }
}

private struct OrderMailDraft: Identifiable {
    let id = UUID()
    let subject: String
    let body: String
    let preferredSendingEmailAddress: String?
}

private extension String {
    func takeIfNotBlank() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

#Preview {
    let authManager = AuthManager()
    let cartVM = CartViewModel(authManager: authManager)

    CartView()
        .environmentObject(authManager)
        .environmentObject(cartVM)
}

private struct CartHeroCard: View {
    let colorScheme: ColorScheme
    let itemCount: Int
    let totalPrice: Double
    let isLoggedIn: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Checkout bereit")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Der Warenkorb folgt jetzt derselben klaren Karten- und Section-Struktur wie Music, Bot und Settings.")
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
                CartBadge(text: "\(itemCount) Artikel", colorScheme: colorScheme)
                CartBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
                if itemCount > 0 {
                    CartBadge(text: String(format: "EUR %.2f", totalPrice), colorScheme: colorScheme)
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

private struct CartSectionCard<Content: View>: View {
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

private struct CartItemCard: View {
    let cartItem: CartItem
    let colorScheme: ColorScheme
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(cartItem.item.name)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    HStack(spacing: 8) {
                        CartBadge(text: "Größe \(cartItem.size)", colorScheme: colorScheme)
                        CartBadge(text: "x\(cartItem.quantity)", colorScheme: colorScheme)
                    }
                }

                Spacer()

                Text(String(format: "EUR %.2f", cartItem.item.price * Double(cartItem.quantity)))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }

            Button(role: .destructive, action: onRemove) {
                Label("Entfernen", systemImage: "trash")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
        }
        .padding(14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct CartInputField: View {
    let title: String
    @Binding var text: String
    let colorScheme: ColorScheme
    var keyboard: UIKeyboardType = .default
    var autocapitalization: TextInputAutocapitalization = .sentences

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(title, text: $text)
                .keyboardType(keyboard)
                .textInputAutocapitalization(autocapitalization)
                .padding(14)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                )
                .foregroundColor(AppColors.text(for: colorScheme))
        }
    }
}

private struct CartSubmitBar: View {
    let colorScheme: ColorScheme
    let totalPrice: Double
    let isFormValid: Bool
    let isSubmitting: Bool
    let onSubmit: () -> Void

    var body: some View {
        VStack(spacing: 10) {
            Divider()
                .overlay(AppColors.accent(for: colorScheme).opacity(0.12))

            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Bestellung abschicken")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(totalPrice > 0 ? String(format: "EUR %.2f gesamt", totalPrice) : "Warenkorb aktuell leer")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                Button(action: onSubmit) {
                    if isSubmitting {
                        ProgressView()
                            .tint(.white)
                            .frame(minWidth: 110)
                    } else {
                        Text("Senden")
                            .font(.headline)
                            .frame(minWidth: 110)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
                .disabled(!isFormValid || isSubmitting)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .background(AppColors.cardBackground(for: colorScheme).opacity(0.98))
    }
}

private struct CartBadge: View {
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
