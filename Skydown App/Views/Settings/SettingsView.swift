//
//  SettingsView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI
import MessageUI

struct SettingsView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.colorScheme) private var environmentColorScheme

    @Binding var colorScheme: String

    @State private var language = "Deutsch"
    @State private var notificationsEnabled = true
    @State private var appVersion = "1.0.0"

    @State private var activeAlert: SettingsAlert?
    @State private var showingLoginSheet = false
    @State private var showingRegistrationSheet = false
    @State private var showingPrivacyPolicy = false
    @State private var showingTermsOfService = false
    @State private var showingOrders = false
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success
    @State private var showingMailOptions = false
    @State private var showingMailView = false

    private var effectiveColorScheme: ColorScheme {
        switch colorScheme {
        case "light": return .light
        case "dark": return .dark
        default: return environmentColorScheme
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    SettingsHeroCard(
                        colorScheme: effectiveColorScheme,
                        username: authManager.userSession?.username,
                        isLoggedIn: authManager.userSession != nil,
                        isAdmin: authManager.userSession?.isAdmin == true,
                        notificationsEnabled: notificationsEnabled,
                        appearance: currentAppearanceLabel
                    )

                    SettingsSectionCard(title: "Konto", colorScheme: effectiveColorScheme) {
                        if let user = authManager.userSession {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Angemeldet als \(user.username)")
                                    .font(.headline)
                                    .foregroundColor(AppColors.text(for: effectiveColorScheme))

                                Text(user.email)
                                    .font(.subheadline)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                                Text("Du kannst dich hier abmelden oder direkt mit einem anderen Konto neu anmelden.")
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                                VStack(spacing: 10) {
                                    Button(role: .destructive) {
                                        activeAlert = .logout
                                    } label: {
                                        Label("Abmelden", systemImage: "person.crop.circle.badge.xmark")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.borderedProminent)

                                    Button {
                                        Task {
                                            await authManager.signOut()
                                            showingLoginSheet = true
                                        }
                                    } label: {
                                        Text("Mit anderem Konto anmelden")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.bordered)

                                    Button(role: .destructive) {
                                        activeAlert = .deleteAccount
                                    } label: {
                                        Label("Konto loeschen", systemImage: "person.fill.xmark")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.bordered)
                                }
                            }
                        } else {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Melde dich an oder registriere dich, um Bestellungen und persoenliche Bereiche freizuschalten.")
                                    .font(.body)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                                Button {
                                    showingLoginSheet = true
                                } label: {
                                    Label("Anmelden", systemImage: "person.crop.circle.fill.badge.plus")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(AppColors.accent(for: effectiveColorScheme))

                                Button {
                                    showingRegistrationSheet = true
                                } label: {
                                    Label("Registrieren", systemImage: "person.crop.circle.badge.plus")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.bordered)
                                .tint(AppColors.accentMystic(for: effectiveColorScheme))
                            }
                        }
                    }

                    SettingsSectionCard(title: "Admin", colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text((authManager.userSession?.isAdmin ?? false) ? "Bestellungen verfuegbar" : "Keine Admin-Berechtigung")
                                .font(.headline)
                                .foregroundColor(
                                    (authManager.userSession?.isAdmin ?? false)
                                    ? AppColors.accent(for: effectiveColorScheme)
                                    : AppColors.secondaryText(for: effectiveColorScheme)
                                )

                            Text("Admin-Bereiche bleiben auf iOS sichtbar, aber nur mit passender Berechtigung aktiv.")
                                .font(.body)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                            Button {
                                showingOrders = true
                            } label: {
                                Label("Bestellungen oeffnen", systemImage: "suitcase.cart")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)
                            .disabled(!(authManager.userSession?.isAdmin ?? false))
                        }
                    }

                    SettingsSectionCard(title: "Allgemein", colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Sprache")
                                    .foregroundColor(AppColors.text(for: effectiveColorScheme))
                                Spacer()
                                Text(language)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                            }

                            SettingsToggleCard(
                                colorScheme: effectiveColorScheme,
                                title: "Benachrichtigungen",
                                subtitle: "Hinweise fuer Updates und wichtige App-Aktionen.",
                                isOn: $notificationsEnabled
                            )
                        }
                    }

                    SettingsSectionCard(title: "Anzeige", colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Aktuell: \(currentAppearanceLabel)")
                                .font(.subheadline)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))

                            ForEach(Appearance.allCases) { appearance in
                                AppearanceChoiceCard(
                                    colorScheme: effectiveColorScheme,
                                    title: appearance.rawValue.capitalized,
                                    isSelected: colorScheme == appearance.rawValue
                                ) {
                                    colorScheme = appearance.rawValue
                                }
                            }
                        }
                    }

                    SettingsSectionCard(title: "App-Info", colorScheme: effectiveColorScheme) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Version \(appVersion)")
                                .font(.headline)
                                .foregroundColor(AppColors.text(for: effectiveColorScheme))

                            Button("Datenschutzbestimmungen") {
                                showingPrivacyPolicy = true
                            }
                            .buttonStyle(.bordered)

                            Button("Nutzungsbedingungen") {
                                showingTermsOfService = true
                            }
                            .buttonStyle(.bordered)

                            VStack(alignment: .leading, spacing: 8) {
                                Text("Support")
                                    .font(.headline)
                                    .foregroundColor(AppColors.text(for: effectiveColorScheme))

                                Text("skydownent@gmail.com")
                                    .font(.subheadline)
                                    .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                            }

                            Button {
                                #if targetEnvironment(simulator)
                                if MFMailComposeViewController.canSendMail() {
                                    showingMailView = true
                                } else {
                                    showToastMessage("Mail kann im Simulator nicht gesendet werden", style: .error)
                                }
                                #else
                                showingMailOptions = true
                                #endif
                            } label: {
                                Label("Support-Anfrage senden", systemImage: "envelope.fill")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(AppColors.accent(for: effectiveColorScheme))

                            Text("Weitere rechtliche Infos folgen direkt in einem der naechsten Updates.")
                                .font(.footnote)
                                .foregroundColor(AppColors.secondaryText(for: effectiveColorScheme))
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 20)
                .padding(.bottom, 28)
            }
            .scrollIndicators(.hidden)
            .navigationTitle("Einstellungen")
            .background(backgroundGradient.ignoresSafeArea())
            .toolbarBackground(AppColors.primaryBackground(for: effectiveColorScheme), for: .navigationBar)
            .toolbarColorScheme(effectiveColorScheme, for: .navigationBar)
            .toolbar(.visible, for: .navigationBar)
            .environment(\.colorScheme, effectiveColorScheme)
        }
        .sheet(isPresented: $showingLoginSheet) {
            LoginView()
        }
        .sheet(isPresented: $showingRegistrationSheet) {
            RegistrationSheet()
        }
        .sheet(isPresented: $showingOrders) {
            OrderView()
        }
        .sheet(isPresented: $showingPrivacyPolicy) {
            PolicyView(title: "Datenschutzbestimmungen", text: .privacyPolicyText)
        }
        .sheet(isPresented: $showingTermsOfService) {
            PolicyView(title: "Nutzungsbedingungen", text: .termsOfServiceText)
        }
        .confirmationDialog(
            "Support-Anfrage senden",
            isPresented: $showingMailOptions,
            titleVisibility: .visible
        ) {
            Button("In-App senden") {
                if MFMailComposeViewController.canSendMail() {
                    showingMailView = true
                } else {
                    showToastMessage("Mail kann auf diesem Geraet nicht gesendet werden", style: .error)
                }
            }

            Button("Mail-App oeffnen") {
                openMailAppFallback()
            }

            Button("Abbrechen", role: .cancel) {}
        }
        .sheet(isPresented: $showingMailView) {
            MailView(
                subject: supportMailSubject,
                body: supportMailBody,
                recipients: [supportMailbox],
                preferredSendingEmailAddress: preferredSupportSenderEmail
            )
        }
        .alert(item: $activeAlert) { alert in
            switch alert {
            case .logout:
                return Alert(
                    title: Text("Abmelden"),
                    message: Text("Moechten Sie sich wirklich abmelden?"),
                    primaryButton: .destructive(Text("Abmelden")) {
                        Task { await authManager.signOut() }
                    },
                    secondaryButton: .cancel()
                )
            case .deleteAccount:
                return Alert(
                    title: Text("Konto loeschen"),
                    message: Text("Moechten Sie Ihr Konto unwiderruflich loeschen?"),
                    primaryButton: .destructive(Text("Konto loeschen")) {
                        Task {
                            do {
                                try await authManager.deleteAccount()
                                showToastMessage("Konto erfolgreich geloescht", style: .success)
                            } catch {
                                showToastMessage("Fehler beim Loeschen: \(error.localizedDescription)", style: .error)
                            }
                        }
                    },
                    secondaryButton: .cancel()
                )
            }
        }
        .fancyToast(isPresented: $showToast, message: toastMessage, style: toastStyle)
    }

    private var currentAppearanceLabel: String {
        Appearance(rawValue: colorScheme)?.rawValue.capitalized ?? "System"
    }

    private var backgroundGradient: LinearGradient {
        LinearGradient(
            colors: [
                AppColors.primaryBackground(for: effectiveColorScheme),
                AppColors.accent(for: effectiveColorScheme).opacity(0.12),
                AppColors.accentMystic(for: effectiveColorScheme).opacity(0.10),
                AppColors.primaryBackground(for: effectiveColorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private var supportMailbox: String {
        "skydownent@gmail.com"
    }

    private var preferredSupportSenderEmail: String? {
        authManager.userSession?.email.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var supportMailSubject: String {
        guard let email = preferredSupportSenderEmail, !email.isEmpty else {
            return "Support-Anfrage"
        }
        return "Support-Anfrage - \(email)"
    }

    private var supportMailBody: String {
        let username = authManager.userSession?.username
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .takeIfNotBlank()
            ?? "Nicht verfuegbar"
        let email = preferredSupportSenderEmail?.takeIfNotBlank() ?? "Nicht verfuegbar"

        return """
        Hallo Skydown-Team,

        ich habe folgende Anfrage:

        Eingeloggter Account: \(username)
        Account-E-Mail: \(email)

        Nachricht:
        """
    }

    private func openMailAppFallback() {
        let encodedSubject = supportMailSubject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let encodedBody = supportMailBody.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""

        if let url = URL(string: "mailto:\(supportMailbox)?subject=\(encodedSubject)&body=\(encodedBody)"),
           UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
            showToastMessage("Mail-App geoeffnet", style: .success)
        } else {
            showToastMessage("Mail-App konnte nicht geoeffnet werden", style: .error)
        }
    }

    private func showToastMessage(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}

private extension String {
    func takeIfNotBlank() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

private struct SettingsHeroCard: View {
    let colorScheme: ColorScheme
    let username: String?
    let isLoggedIn: Bool
    let isAdmin: Bool
    let notificationsEnabled: Bool
    let appearance: String

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text(username ?? "Skydown Einstellungen")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Konto, Anzeige und Support sind auf iOS jetzt nicht mehr im Standard-Form-Look versteckt, sondern klar nach Aufgaben gruppiert.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "slider.horizontal.3")
                    .font(.title2)
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 10) {
                    SettingsBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
                    SettingsBadge(text: notificationsEnabled ? "Hinweise an" : "Hinweise aus", colorScheme: colorScheme)
                    SettingsBadge(text: appearance, colorScheme: colorScheme)
                }

                if isAdmin {
                    SettingsBadge(text: "Admin aktiv", colorScheme: colorScheme)
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

private struct SettingsSectionCard<Content: View>: View {
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

private struct SettingsToggleCard: View {
    let colorScheme: ColorScheme
    let title: String
    let subtitle: String
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(subtitle)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer()

            Toggle("", isOn: $isOn)
                .labelsHidden()
        }
        .padding(14)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct AppearanceChoiceCard: View {
    let colorScheme: ColorScheme
    let title: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.headline)
                    Text(isSelected ? "Aktiv" : "Tippen zum Wechseln")
                        .font(.caption)
                        .foregroundColor(
                            isSelected
                            ? Color.white.opacity(0.82)
                            : AppColors.secondaryText(for: colorScheme)
                        )
                }

                Spacer()

                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(AppColors.accent(for: colorScheme).opacity(isSelected ? 1 : 0.18), lineWidth: 1)
            )
            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
        }
    }
}

private struct SettingsBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(AppColors.accentMystic(for: colorScheme).opacity(0.12))
            .foregroundColor(AppColors.accentMystic(for: colorScheme))
            .clipShape(Capsule())
    }
}

#Preview {
    SettingsView(colorScheme: .constant("system"))
        .environmentObject(AuthManager())
        .environment(\.colorScheme, .light)
}
