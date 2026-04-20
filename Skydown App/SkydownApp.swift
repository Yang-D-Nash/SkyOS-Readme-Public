//
//  SkydownApp.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//
import SwiftUI
import FirebaseAppCheck
import FirebaseCore
import FirebaseFirestore
import AVKit
import GoogleSignIn
import UIKit

final class SkydownApplicationDelegate: NSObject, UIApplicationDelegate {}

@main
struct SkydownApp: App {
    @UIApplicationDelegateAdaptor(SkydownApplicationDelegate.self) private var appDelegate
    @StateObject private var services: AppServices

    init() {
        AppTypography.configure()
        Self.configureFirebaseIfNeeded()
        _services = StateObject(wrappedValue: AppServices())
    }

    var body: some Scene {
        WindowGroup {
            ZStack(alignment: .top) {
                LaunchScreenView()
                    .skydownTactileAction()
                    .environmentObject(services)
                    .environmentObject(services.featureFlags)
                    .environmentObject(services.authManager)
                    .environmentObject(services.cartViewModel)
                    .environmentObject(services.hostedCheckoutRedirectStore)
                    .environmentObject(services.aiSubscriptionStore)
                    .environmentObject(services.networkStatusMonitor)
                    .environmentObject(services.notificationPermissionStore)
                    .onOpenURL { url in
                        if services.hostedCheckoutRedirectStore.handle(url) {
                            return
                        }

                        _ = GIDSignIn.sharedInstance.handle(url)
                    }

                if !services.networkStatusMonitor.isOnline {
                    ConnectivityStatusBanner(
                        title: AppLocalized.text("offline.banner.title", fallback: "Offline"),
                        message: AppLocalized.text(
                            "offline.banner.message",
                            fallback: "No connection. You are seeing cached content and can continue navigating."
                        )
                    )
                    .padding(.top, 10)
                    .transition(
                        .move(edge: .top)
                        .combined(with: .opacity)
                        .combined(with: .scale(scale: 0.985, anchor: .top))
                    )
                }
            }
            .animation(SkydownMotion.statusTransition, value: services.networkStatusMonitor.isOnline)
            .task {
                await services.notificationPermissionStore.requestAuthorizationIfNeededOnLaunch()
            }
        }
    }
}

private extension SkydownApp {
    static func configureFirebaseIfNeeded() {
        if FirebaseApp.app() == nil {
            AppCheck.setAppCheckProviderFactory(SkydownAppCheckProviderFactory())
            FirebaseApp.configure()
        }

        guard FirebaseApp.app() != nil else { return }
        configureFirestoreCache()
        AppCheck.appCheck().isTokenAutoRefreshEnabled = true
    }

    static func configureFirestoreCache() {
        let firestore = Firestore.firestore()
        let settings = firestore.settings
        settings.cacheSettings = PersistentCacheSettings(
            sizeBytes: NSNumber(value: FirestoreCacheSizeUnlimited)
        )
        firestore.settings = settings
    }
}

private struct ConnectivityStatusBanner: View {
    let title: String
    let message: String

    var body: some View {
        HStack(alignment: .center, spacing: 10) {
            Image(systemName: "wifi.slash")
                .font(.headline)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption.bold())
                Text(message)
                    .font(.caption2)
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .foregroundColor(Color.white)
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(
            LinearGradient(
                colors: [
                    Color(red: 61 / 255, green: 36 / 255, blue: 12 / 255),
                    Color(red: 22 / 255, green: 33 / 255, blue: 48 / 255),
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.white.opacity(0.16), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.16), radius: 18, y: 8)
        .skydownLuminousSweep(
            cornerRadius: 14,
            accent: Color(red: 1.0, green: 0.78, blue: 0.42),
            alpha: 0.16
        )
        .padding(.horizontal, 12)
    }
}
