//
//  SettingsView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI
import FirebaseAuth
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
    
    // Toast State
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success
    
    // Mail States
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
            Form {
                Section(header: Text("Konto")) {
                    if let user = authManager.userSession {
                        HStack {
                            Text("Angemeldet als")
                            Spacer()
                            Text(user.username)
                                .foregroundColor(.secondary)
                        }
                        Button(role: .destructive) { activeAlert = .logout } label: {
                            Label("Abmelden", systemImage: "person.crop.circle.badge.xmark")
                        }
                        Button(role: .destructive) { activeAlert = .deleteAccount } label: {
                            Label("Konto löschen", systemImage: "person.fill.xmark")
                        }
                    } else {
                        Button { showingLoginSheet = true } label: {
                            Label("Anmelden", systemImage: "person.crop.circle.fill.badge.plus")
                                .foregroundColor(AppColors.accent(for: environmentColorScheme))
                        }
                        .sheet(isPresented: $showingLoginSheet) { LoginView() }
                        
                        Button { showingRegistrationSheet = true } label: {
                            Label("Registrieren", systemImage: "person.crop.circle.badge.plus")
                                .foregroundColor(AppColors.accentMystic(for: environmentColorScheme))
                        }
                        .sheet(isPresented: $showingRegistrationSheet) { RegistrationSheet() }
                    }
                }
                
                Section(header: Text("Admin")) {
                    Button {
                        showingOrders = true
                    } label: {
                        Label("Bestellungen", systemImage: "suitcase.cart")
                    }
                    .disabled(!(authManager.userSession?.isAdmin ?? false))
                    .foregroundColor((authManager.userSession?.isAdmin ?? false) ?
                        AppColors.accent(for: environmentColorScheme) : .gray)
                    .sheet(isPresented: $showingOrders) { OrderView() }
                }
                .listRowBackground(AppColors.secondaryBackground(for: effectiveColorScheme))
                
                Section(header: Text("Allgemein")) {
                    HStack {
                        Text("Sprache")
                        Spacer()
                        Text(language)
                            .foregroundColor(.secondary)
                    }
                    Toggle("Benachrichtigungen", isOn: $notificationsEnabled)
                }
                .listRowBackground(AppColors.secondaryBackground(for: effectiveColorScheme))
                
                Section(header: Text("Anzeige")) {
                    Picker("Erscheinungsbild", selection: $colorScheme) {
                        ForEach(Appearance.allCases) { appearance in
                            Text(appearance.rawValue.capitalized).tag(appearance.rawValue)
                        }
                    }
                }
                .listRowBackground(AppColors.secondaryBackground(for: effectiveColorScheme))
                
                Section(header: Text("App-Info")) {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text(appVersion)
                    }
                    
                    Button("Datenschutzbestimmungen") { showingPrivacyPolicy = true }
                        .sheet(isPresented: $showingPrivacyPolicy) {
                            PolicyView(title: "Datenschutzbestimmungen", text: .privacyPolicyText)
                        }
                    
                    Button("Nutzungsbedingungen") { showingTermsOfService = true }
                        .sheet(isPresented: $showingTermsOfService) {
                            PolicyView(title: "Nutzungsbedingungen", text: .termsOfServiceText)
                        }
                    
                    // Support-Anfrage Button
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
                    }
                    .confirmationDialog("Support-Anfrage senden",
                                        isPresented: $showingMailOptions,
                                        titleVisibility: .visible) {
                        Button("In-App senden") {
                            if MFMailComposeViewController.canSendMail() {
                                showingMailView = true
                            } else {
                                showToastMessage("Mail kann auf diesem Gerät nicht gesendet werden", style: .error)
                            }
                        }
                        
                        Button("Mail-App öffnen") {
                            openMailAppFallback()
                        }
                        
                        Button("Abbrechen", role: .cancel) {}
                    }
                    .sheet(isPresented: $showingMailView) {
                        MailView(
                            subject: "Support-Anfrage",
                            body: "Hallo Skydown-Team,\n\nich habe folgende Anfrage:\n",
                            recipients: ["skydownent@gmail.com"]
                        )
                    }
                }
                .listRowBackground(AppColors.secondaryBackground(for: effectiveColorScheme))
            }
            .navigationTitle("Einstellungen")
            .scrollContentBackground(.hidden)
            .background(AppColors.primaryBackground(for: effectiveColorScheme).ignoresSafeArea())
            .toolbarBackground(AppColors.primaryBackground(for: effectiveColorScheme), for: .navigationBar)
            .toolbarColorScheme(effectiveColorScheme, for: .navigationBar)
            .toolbar(.visible, for: .navigationBar)
            .environment(\.colorScheme, effectiveColorScheme)
            .alert(item: $activeAlert) { alert in
                switch alert {
                case .logout:
                    return Alert(
                        title: Text("Abmelden"),
                        message: Text("Möchten Sie sich wirklich abmelden?"),
                        primaryButton: .destructive(Text("Abmelden")) {
                            Task { await authManager.signOut() }
                        },
                        secondaryButton: .cancel()
                    )
                case .deleteAccount:
                    return Alert(
                        title: Text("Konto löschen"),
                        message: Text("Möchten Sie Ihr Konto unwiderruflich löschen?"),
                        primaryButton: .destructive(Text("Konto löschen")) {
                            Task {
                                do {
                                    try await Auth.auth().currentUser?.delete()
                                    authManager.userSession = nil
                                    showToastMessage("Konto erfolgreich gelöscht", style: .success)
                                } catch {
                                    showToastMessage("Fehler beim Löschen: \(error.localizedDescription)", style: .error)
                                }
                            }
                        },
                        secondaryButton: .cancel()
                    )
                }
            }
        }
        .fancyToast(isPresented: $showToast, message: toastMessage, style: toastStyle)
    }
    
    private func openMailAppFallback() {
        let email = "skydownent@gmail.com"
        let subject = "Support-Anfrage"
        let body = "Hallo Skydown-Team,\n\nich habe folgende Anfrage:\n"
        
        let encodedSubject = subject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let encodedBody = body.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        
        if let url = URL(string: "mailto:\(email)?subject=\(encodedSubject)&body=\(encodedBody)"),
           UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
            showToastMessage("Mail-App geöffnet", style: .success)
        } else {
            showToastMessage("Mail-App konnte nicht geöffnet werden", style: .error)
        }
    }
    
    private func showToastMessage(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }
}
#Preview {
    SettingsView(colorScheme: .constant("system"))
        .environmentObject(AuthManager())
        .environment(\.colorScheme, .light)
}
