//
//  ContentView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI

struct MainTabView: View {
    @AppStorage("colorScheme") private var colorScheme: String = "dark"
    @Environment(\.colorScheme) private var systemColorScheme

    @StateObject private var authManager = AuthManager()
    @StateObject private var cartVM: CartViewModel

    init() {
        let authManager = AuthManager()
        _cartVM = StateObject(wrappedValue: CartViewModel(authManager: authManager))
        _authManager = StateObject(wrappedValue: authManager)
    }

    private var currentScheme: ColorScheme {
        colorScheme == "dark" ? .dark : .light
    }

    var body: some View {
        TabView {
            ShopView()
                .tabItem { Label("Shop", systemImage: "cart.fill") }

            MusicView()
                .tabItem { Label("Musik", systemImage: "music.note.list") }

            CartView()
                .tabItem { Label("Warenkorb", systemImage: "bag.fill") }

            SettingsView(colorScheme: $colorScheme)
                .tabItem { Label("Einstellungen", systemImage: "gearshape.fill") }
        }
        .accentColor(AppColors.accent(for: currentScheme))
        .background(AppColors.primaryBackground(for: currentScheme).edgesIgnoringSafeArea(.all))
        .preferredColorScheme(currentScheme)
        .environmentObject(authManager)
        .environmentObject(cartVM)
    }
}

#Preview {
    MainTabView()
}
