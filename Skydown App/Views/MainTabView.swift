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
            .toolbarBackground(.hidden, for: .tabBar)
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
                    .padding(.top, 6)
                } else {
                    AIHubControlCard(
                        mode: mode,
                        colorScheme: colorScheme,
                        onSelectMode: { mode = $0 }
                    )
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.top, 6)

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

private struct AIHubControlCard: View {
    let mode: AIHubMode
    let colorScheme: ColorScheme
    let onSelectMode: (AIHubMode) -> Void

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
            return "Bot fuer schnelle Ideen"
        case .agent:
            return "Agent fuer klare Struktur"
        }
    }

    private var message: String {
        switch mode {
        case .bot:
            return "Hooks, Captions und Visual-Ideen ohne Umwege."
        case .agent:
            return "Briefings, To-dos und Release-Plaene in einem Flow."
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center, spacing: 14) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline.bold())
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(message)
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                ZStack {
                    Circle()
                        .fill(accent.opacity(0.14))
                        .frame(width: 42, height: 42)

                    Image(systemName: mode.iconName)
                        .font(.headline)
                        .foregroundColor(accent)
                }
            }

            HStack(spacing: 8) {
                ForEach(AIHubMode.allCases) { currentMode in
                    Button {
                        onSelectMode(currentMode)
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: currentMode.iconName)
                            Text(currentMode.rawValue)
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(
                                    mode == currentMode
                                    ? accent
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
        .padding(14)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 22)
                .stroke(accent.opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 22))
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
