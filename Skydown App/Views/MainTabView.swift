//
//  ContentView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI

struct MainTabView: View {
    @AppStorage("colorScheme") private var colorScheme: String = "system"
    @Environment(\.colorScheme) private var systemColorScheme
    @EnvironmentObject private var services: AppServices

    private var preferredScheme: ColorScheme? {
        switch colorScheme {
        case "light":
            return .light
        case "dark":
            return .dark
        default:
            return nil
        }
    }

    private var currentScheme: ColorScheme {
        preferredScheme ?? systemColorScheme
    }

    var body: some View {
        TabView {
            DeferredView {
                ShopView(
                    authManager: services.authManager,
                    merchandiseService: services.merchandiseService
                )
            }
                .tabItem { Label("Shop", systemImage: "cart.fill") }

            DeferredView {
                MusicView()
            }
                .tabItem { Label("Musik", systemImage: "music.note.list") }

            DeferredView {
                AIView(
                    aiChatService: services.aiChatService,
                    featureFlags: services.featureFlags
                )
            }
                .tabItem { Label("Bot", systemImage: "sparkles") }

            DeferredView {
                AgentView(
                    agentChatService: services.agentChatService,
                    featureFlags: services.featureFlags
                )
            }
                .tabItem { Label("Agent", systemImage: "bolt.fill") }

            DeferredView {
                CartView()
            }
                .tabItem { Label("Warenkorb", systemImage: "bag.fill") }

            DeferredView {
                SettingsView(colorScheme: $colorScheme)
            }
                .tabItem { Label("Einstellungen", systemImage: "gearshape.fill") }
        }
        .accentColor(AppColors.accent(for: currentScheme))
        .background(AppColors.primaryBackground(for: currentScheme).edgesIgnoringSafeArea(.all))
        .preferredColorScheme(preferredScheme)
    }
}

private struct DeferredView<Content: View>: View {
    private let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        content()
    }
}

#Preview {
    let services = AppServices()

    MainTabView()
        .environmentObject(services)
        .environmentObject(services.authManager)
        .environmentObject(services.cartViewModel)
}
