//
//  SkydownApp.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//
import SwiftUI
import FirebaseCore
import AVKit

@main
struct SkydownApp: App {
    @StateObject private var services = AppServices()

    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            LaunchScreenView()
                .environmentObject(services.authManager)
        }
    }
}
