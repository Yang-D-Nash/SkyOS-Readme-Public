//
//  SkydownApp.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//
import SwiftUI
import FirebaseAppCheck
import FirebaseCore
import AVKit
import GoogleSignIn

@main
struct SkydownApp: App {
    @StateObject private var services = AppServices()

    init() {
        AppCheck.setAppCheckProviderFactory(SkydownAppCheckProviderFactory())
        FirebaseApp.configure()
        AppCheck.appCheck().isTokenAutoRefreshEnabled = true
    }

    var body: some Scene {
        WindowGroup {
            LaunchScreenView()
                .environmentObject(services)
                .environmentObject(services.featureFlags)
                .environmentObject(services.authManager)
                .environmentObject(services.cartViewModel)
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
