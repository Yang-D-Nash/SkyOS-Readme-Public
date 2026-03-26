//
//  ContentView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI

private enum MainTab: Hashable {
    case shop
    case music
    case home
    case video
    case ai
}

private enum AIHubMode: String, CaseIterable, Identifiable {
    case bot = "Bot"
    case agent = "Agent"

    var id: String { rawValue }

    var iconName: String {
        switch self {
        case .bot:
            return "sparkles"
        case .agent:
            return "bolt.fill"
        }
    }
}

struct MainTabView: View {
    @AppStorage("colorScheme") private var colorScheme: String = "system"
    @Environment(\.colorScheme) private var systemColorScheme
    @EnvironmentObject private var services: AppServices
    @State private var selectedTab: MainTab = .home
    @State private var showingSettings = false
    @State private var showingCart = false
    @State private var showingLogin = false

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
        TabView(selection: $selectedTab) {
            DeferredView {
                ShopView(
                    authManager: services.authManager,
                    onOpenCart: { showingCart = true },
                    onOpenSettings: { showingSettings = true },
                    merchandiseService: services.merchandiseService
                )
            }
            .tabItem { Label("Shop", systemImage: "bag.fill") }
            .tag(MainTab.shop)

            DeferredView {
                MusicView(
                    onOpenCart: { showingCart = true },
                    onOpenSettings: { showingSettings = true }
                )
            }
            .tabItem { Label("Musik", systemImage: "music.note.list") }
            .tag(MainTab.music)

            DeferredView {
                HomeView(
                    onOpenCart: { showingCart = true },
                    onOpenSettings: { showingSettings = true }
                )
            }
            .tabItem { Label("Home", systemImage: "house.fill") }
            .tag(MainTab.home)

            DeferredView {
                VideoHubTabView(
                    onOpenCart: { showingCart = true },
                    onOpenSettings: { showingSettings = true }
                )
            }
            .tabItem { Label("Video", systemImage: "video.fill") }
            .tag(MainTab.video)

            DeferredView {
                AIHubView(
                    aiChatService: services.aiChatService,
                    agentChatService: services.agentChatService,
                    featureFlags: services.featureFlags,
                    onOpenCart: { showingCart = true },
                    onOpenLogin: { showingLogin = true },
                    onOpenSettings: { showingSettings = true }
                )
            }
            .tabItem { Label("KI", systemImage: "sparkles") }
            .tag(MainTab.ai)
        }
        .accentColor(AppColors.accent(for: currentScheme))
        .background(AppColors.primaryBackground(for: currentScheme).edgesIgnoringSafeArea(.all))
        .preferredColorScheme(preferredScheme)
        .sheet(isPresented: $showingSettings) {
            SettingsView(colorScheme: $colorScheme)
        }
        .sheet(isPresented: $showingCart) {
            CartView(onOpenSettings: { showingSettings = true })
        }
        .sheet(isPresented: $showingLogin) {
            LoginView()
        }
    }
}

struct AppSessionToolbarActions: View {
    let onOpenCart: (() -> Void)? = nil
    let onOpenSettings: () -> Void
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme

    private var displayName: String {
        let trimmed = authManager.userSession?.username.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? "Gast" : trimmed
    }

    private var initials: String {
        String(displayName.prefix(1)).uppercased()
    }

    var body: some View {
        HStack(spacing: 10) {
            HStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(AppColors.accent(for: colorScheme).opacity(0.14))
                        .frame(width: 26, height: 26)

                    Text(initials)
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }

                Text(displayName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .clipShape(Capsule())

            if let onOpenCart {
                Button(action: onOpenCart) {
                    Image(systemName: "bag.fill")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .padding(10)
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .clipShape(Circle())
                }
            }

            Button(action: onOpenSettings) {
                Image(systemName: "gearshape.fill")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .padding(10)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(Circle())
            }
        }
    }
}

private struct AIHubView: View {
    let aiChatService: AIChatServicing
    let agentChatService: AgentChatServicing
    @ObservedObject private var featureFlags: FeatureFlagsService
    let onOpenCart: () -> Void
    let onOpenLogin: () -> Void
    let onOpenSettings: () -> Void
    @State private var mode: AIHubMode = .bot
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var authManager: AuthManager

    init(
        aiChatService: AIChatServicing,
        agentChatService: AgentChatServicing,
        featureFlags: FeatureFlagsService,
        onOpenCart: @escaping () -> Void,
        onOpenLogin: @escaping () -> Void,
        onOpenSettings: @escaping () -> Void
    ) {
        self.aiChatService = aiChatService
        self.agentChatService = agentChatService
        self.onOpenCart = onOpenCart
        self.onOpenLogin = onOpenLogin
        self.onOpenSettings = onOpenSettings
        _featureFlags = ObservedObject(wrappedValue: featureFlags)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if authManager.userSession == nil {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("KI nur mit Konto")
                            .font(.title2.bold())
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text("Bot und Agent sind jetzt an den Login gebunden, damit anonyme Nutzer keine AI-Kosten ausloesen.")
                            .font(.body)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        Button(action: onOpenLogin) {
                            Text("Jetzt anmelden")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.accent(for: colorScheme))
                    }
                    .padding(18)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                    .padding(.horizontal, 20)
                    .padding(.top, 14)
                } else {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack(spacing: 10) {
                            ForEach(AIHubMode.allCases) { currentMode in
                                Button {
                                    mode = currentMode
                                } label: {
                                    HStack(spacing: 8) {
                                        Image(systemName: currentMode.iconName)
                                        Text(currentMode.rawValue)
                                            .fontWeight(.semibold)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(
                                        RoundedRectangle(cornerRadius: 18)
                                            .fill(
                                                mode == currentMode
                                                ? AppColors.accent(for: colorScheme)
                                                : AppColors.secondaryBackground(for: colorScheme)
                                            )
                                    )
                                    .foregroundColor(
                                        mode == currentMode
                                        ? .white
                                        : AppColors.text(for: colorScheme)
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(18)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                    .padding(.horizontal, 20)
                    .padding(.top, 14)

                    Group {
                        if mode == .bot {
                            AIView(
                                aiChatService: aiChatService,
                                featureFlags: featureFlags,
                                showsNavigation: false
                            )
                        } else {
                            AgentView(
                                agentChatService: agentChatService,
                                featureFlags: featureFlags,
                                showsNavigation: false
                            )
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                }
            }
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
            .navigationTitle("KI")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    AppSessionToolbarActions(
                        onOpenCart: onOpenCart,
                        onOpenSettings: onOpenSettings
                    )
                }
            }
        }
    }
}

private struct VideoHubTabView: View {
    let onOpenCart: () -> Void
    let onOpenSettings: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        NavigationStack {
            VideoHubView()
                .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
                .navigationTitle("Videography")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        AppSessionToolbarActions(
                            onOpenCart: onOpenCart,
                            onOpenSettings: onOpenSettings
                        )
                    }
                }
        }
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
