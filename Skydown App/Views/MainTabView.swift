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
            Group {
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
            .toolbar(.visible, for: .tabBar)
            .toolbarBackground(.ultraThinMaterial, for: .tabBar)
            .toolbarColorScheme(currentScheme, for: .tabBar)
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
    let onOpenCart: (() -> Void)?
    let onOpenSettings: () -> Void
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme

    init(onOpenCart: (() -> Void)? = nil, onOpenSettings: @escaping () -> Void) {
        self.onOpenCart = onOpenCart
        self.onOpenSettings = onOpenSettings
    }

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

    private var activeAccent: Color {
        switch mode {
        case .bot:
            return AppColors.accent(for: colorScheme)
        case .agent:
            return AppColors.accentMystic(for: colorScheme)
        }
    }

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
            VStack(spacing: SkydownLayout.sectionSpacing) {
                if authManager.userSession == nil {
                    AIHubLoginCard(
                        colorScheme: colorScheme,
                        onOpenLogin: onOpenLogin
                    )
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.top, SkydownLayout.screenTopPadding)
                } else {
                    AIHubSpotlightCard(
                        mode: mode,
                        colorScheme: colorScheme
                    )
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.top, SkydownLayout.screenTopPadding)

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
                                            ? activeAccent
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
                    .padding(6)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 22)
                            .stroke(activeAccent.opacity(0.14), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 22))
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)

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
            .background(
                AppColors.screenGradient(
                    for: colorScheme,
                    secondaryAccent: AppColors.accentMystic(for: colorScheme)
                )
                .ignoresSafeArea()
            )
            .navigationTitle("KI")
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
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
                .navigationBarTitleDisplayMode(.inline)
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

private struct AIHubLoginCard: View {
    let colorScheme: ColorScheme
    let onOpenLogin: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("KI nur mit Konto")
                .font(.title2.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Melde dich an und starte direkt mit Hooks, Captions, Briefings, Release-Plaenen oder Visual-Ideen in einem gemeinsamen Bereich.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 10) {
                AIHubBadge(text: "Bot", color: AppColors.accent(for: colorScheme))
                AIHubBadge(text: "Agent", color: AppColors.accentMystic(for: colorScheme))
                AIHubBadge(text: "Visuals", color: AppColors.accentHighlight(for: colorScheme))
            }

            Button(action: onOpenLogin) {
                Text("Jetzt anmelden")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accent(for: colorScheme))
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
    }
}

private struct AIHubSpotlightCard: View {
    let mode: AIHubMode
    let colorScheme: ColorScheme

    private var accent: Color {
        switch mode {
        case .bot:
            return AppColors.accent(for: colorScheme)
        case .agent:
            return AppColors.accentMystic(for: colorScheme)
        }
    }

    private var title: String {
        switch mode {
        case .bot:
            return "Hooks, Captions und Visual-Ideen ohne Reibung."
        case .agent:
            return "Briefings, To-dos und Release-Plaene mit Struktur."
        }
    }

    private var message: String {
        switch mode {
        case .bot:
            return "Nutze den Bot fuer schnelle kreative Varianten, Claims, Caption-Ideen und erste Bildrichtungen."
        case .agent:
            return "Nutze den Agent fuer klare Kampagnenplaene, Aufgabenlisten, Timings und naechste Schritte."
        }
    }

    private var badges: [String] {
        switch mode {
        case .bot:
            return ["Hooks", "Captions", "Visuals"]
        case .agent:
            return ["Briefing", "Plan", "Checkliste"]
        }
    }

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text(title)
                    .font(.title2.bold())
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(message)
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                HStack(spacing: 10) {
                    ForEach(badges, id: \.self) { badge in
                        AIHubBadge(text: badge, color: accent)
                    }
                }
            }

            ZStack {
                Circle()
                    .fill(accent.opacity(0.16))
                    .frame(width: 56, height: 56)

                Image(systemName: mode.iconName)
                    .font(.title2)
                    .foregroundColor(accent)
            }
        }
        .padding(SkydownLayout.heroPadding)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius)
                .stroke(accent.opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius))
    }
}

private struct AIHubBadge: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundColor(color)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(color.opacity(0.12))
            .clipShape(Capsule())
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
