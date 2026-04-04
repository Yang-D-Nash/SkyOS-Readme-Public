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
import UIKit

final class SkydownApplicationDelegate: NSObject, UIApplicationDelegate {}

@main
struct SkydownApp: App {
    @UIApplicationDelegateAdaptor(SkydownApplicationDelegate.self) private var appDelegate
    @StateObject private var services = AppServices()

    init() {
        AppTypography.configure()
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
                .environmentObject(services.hostedCheckoutRedirectStore)
                .onOpenURL { url in
                    if services.hostedCheckoutRedirectStore.handle(url) {
                        return
                    }

                    _ = GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
