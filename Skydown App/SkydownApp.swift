//
//  SkydownApp.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//
import SwiftUI
import FirebaseCore
import FirebaseAuth
import AVKit

@main
struct SkydownApp: App {
    init() {
        FirebaseApp.configure()
    }

    @StateObject var authManager = AuthManager()

    var body: some Scene {
        WindowGroup {
            LaunchScreenView()
            .environmentObject(authManager)
        }
    }
}
