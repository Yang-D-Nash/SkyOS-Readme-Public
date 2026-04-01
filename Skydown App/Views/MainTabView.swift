//
//  ContentView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI

enum MainTab: Hashable {
    case hub
    case zweizwei
    case skydown
    case merch
    case tools
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

private enum ZweizweiDestination {
    case hub
    case catalog
    case beatHub
    case nicma
}

struct MainTabView: View {
    @AppStorage("colorScheme") private var colorScheme: String = "system"
    @Environment(\.colorScheme) private var systemColorScheme
    @EnvironmentObject private var services: AppServices
    @EnvironmentObject private var featureFlags: FeatureFlagsService
    @EnvironmentObject private var authManager: AuthManager
    @State private var selectedTab: MainTab = .hub
    @State private var showingSettings = false
    @State private var showingCart = false
    @State private var showingLogin = false
    @State private var showsWorkflowWorkspace = false

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

    init(initialTab: MainTab = .hub) {
        _selectedTab = State(initialValue: initialTab)
    }

    private var hasAIAccess: Bool {
        featureFlags.allowsAIAccess(for: authManager.userSession)
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            Group {
                DeferredView {
                    ShopView(
                        authManager: services.authManager,
                        onOpenLogin: { showingLogin = true },
                        onOpenCart: { showingCart = true },
                        onOpenSettings: { showingSettings = true },
                        merchandiseService: services.merchandiseService
                    )
                }
                .tabItem { Label("Shop", systemImage: "bag.fill") }
                .tag(MainTab.merch)

                DeferredView {
                    ZweizweiTabView(
                        onOpenCart: { showingCart = true },
                        onOpenSettings: { showingSettings = true }
                    )
                }
                .tabItem { Label("Music", systemImage: "music.note") }
                .tag(MainTab.zweizwei)

                DeferredView {
                    HomeView(
                        onOpenCart: { showingCart = true },
                        onOpenSettings: { showingSettings = true },
                        onOpenWorkflow: hasAIAccess ? {
                            showsWorkflowWorkspace = true
                            selectedTab = .tools
                        } : nil
                    )
                }
                .tabItem { Label("Home", systemImage: "house.fill") }
                .tag(MainTab.hub)

                DeferredView {
                    VideoHubTabView(
                        onOpenCart: { showingCart = true },
                        onOpenSettings: { showingSettings = true }
                    )
                }
                .tabItem { Label("Video", systemImage: "video.fill") }
                .tag(MainTab.skydown)

                DeferredView {
                    AIHubView(
                        aiChatService: services.aiChatService,
                        agentChatService: services.agentChatService,
                        featureFlags: services.featureFlags,
                        showsWorkflowWorkspace: $showsWorkflowWorkspace,
                        onOpenCart: { showingCart = true },
                        onOpenLogin: { showingLogin = true },
                        onOpenSettings: { showingSettings = true }
                    )
                }
                .tabItem { Label("Tools", systemImage: "sparkles") }
                .tag(MainTab.tools)
            }
            .skydownTabBarChrome(colorScheme: currentScheme)
        }
        .accentColor(AppColors.accent(for: currentScheme))
        .background(AppColors.primaryBackground(for: currentScheme).edgesIgnoringSafeArea(.all))
        .preferredColorScheme(preferredScheme)
        .sheet(isPresented: $showingSettings) {
            SettingsView(colorScheme: $colorScheme)
        }
        .sheet(isPresented: $showingCart) {
            CartView {
                showingSettings = true
            }
        }
        .sheet(isPresented: $showingLogin) {
            LoginView()
        }
        .onChange(of: hasAIAccess) { _, allowed in
            if !allowed {
                showsWorkflowWorkspace = false
            }
        }
        .onChange(of: selectedTab) { _, newTab in
            if newTab != .tools {
                showsWorkflowWorkspace = false
            }
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
                        .frame(width: 24, height: 24)

                    Text(initials)
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }

                Text(displayName)
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .skydownCapsuleSurface(
                colorScheme: colorScheme,
                accent: AppColors.accent(for: colorScheme)
            )

            if let onOpenCart {
                Button(action: onOpenCart) {
                    Image(systemName: "bag.fill")
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .padding(9)
                        .skydownCapsuleSurface(colorScheme: colorScheme)
                }
                .skydownTactileAction()
            }

            Button(action: onOpenSettings) {
                Image(systemName: "gearshape.fill")
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .padding(9)
                    .skydownCapsuleSurface(colorScheme: colorScheme)
            }
            .skydownTactileAction()
        }
    }
}

private struct ZweizweiTabView: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var destination: ZweizweiDestination = .hub
    let onOpenCart: () -> Void
    let onOpenSettings: () -> Void

    var body: some View {
        switch destination {
        case .hub:
            NavigationStack {
                ScrollView {
                    VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                        BrandHeroSurface(
                            colorScheme: colorScheme,
                            eyebrow: "Music",
                            title: "Music",
                            subtitle: "Hier findest du Releases, Artists und alles rund ums Produzieren.",
                            detail: "Hoer rein, entdecke Artists und geh von hier direkt weiter zu Beats oder Studio.",
                            accent: AppColors.spotify(for: colorScheme),
                            secondaryAccent: AppColors.accent(for: colorScheme),
                            marks: [.zweizwei]
                        ) {
                            HStack(spacing: 10) {
                                BrandHeroPill(
                                    text: "Catalog",
                                    colorScheme: colorScheme,
                                    tint: AppColors.spotify(for: colorScheme)
                                )
                                BrandHeroPill(
                                    text: "Beats",
                                    colorScheme: colorScheme,
                                    tint: AppColors.accent(for: colorScheme)
                                )
                                BrandHeroPill(
                                    text: "Studio",
                                    colorScheme: colorScheme,
                                    tint: AppColors.accentMystic(for: colorScheme)
                                )
                            }
                        }

                        ShellActionCard(
                            title: "Songs & Artists",
                            subtitle: "Zum Hoeren, Stoebern und direkten Weiterklicken.",
                            accent: AppColors.spotify(for: colorScheme)
                        ) {
                            destination = .catalog
                        }

                        ShellActionCard(
                            title: "Beat Library",
                            subtitle: "Wenn du schnell ein Gefuehl fuer Beats bekommen willst.",
                            accent: AppColors.accent(for: colorScheme)
                        ) {
                            destination = .beatHub
                        }

                        ShellActionCard(
                            title: "Studio Services",
                            subtitle: "Wenn du Recording, Mixing oder Mastering anfragen willst.",
                            accent: AppColors.accentMystic(for: colorScheme)
                        ) {
                            destination = .nicma
                        }
                    }
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.top, SkydownLayout.screenTopPadding)
                    .padding(.bottom, SkydownLayout.screenBottomPadding)
                }
                .scrollIndicators(.hidden)
                .background(
                    AppColors.screenGradient(
                        for: colorScheme,
                        secondaryAccent: AppColors.spotify(for: colorScheme)
                    )
                    .ignoresSafeArea()
                )
                .navigationTitle("Music")
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
        case .catalog:
            MusicView(
                brand: .zweizwei,
                onBack: { destination = .hub },
                onOpenCart: onOpenCart,
                onOpenSettings: onOpenSettings
            )
        case .beatHub:
            NavigationStack {
                BeatHubView {
                    destination = .hub
                }
            }
        case .nicma:
            NavigationStack {
                NicmaProducerView {
                    destination = .hub
                }
            }
        }
    }
}

private struct ShellActionCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let subtitle: String
    let accent: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.system(size: 20, weight: .black, design: .rounded))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text(subtitle)
                    .font(.footnote.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .multilineTextAlignment(.leading)
            }
            .padding(SkydownLayout.heroPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .skydownPanelSurface(
                colorScheme: colorScheme,
                accent: accent,
                cornerRadius: SkydownLayout.heroCornerRadius,
                shadowRadius: 14,
                shadowYOffset: 8
            )
        }
        .buttonStyle(SkydownTactileButtonStyle())
    }
}

private struct AIHubView: View {
    let aiChatService: AIChatServicing
    let agentChatService: AgentChatServicing
    @ObservedObject private var featureFlags: FeatureFlagsService
    @Binding var showsWorkflowWorkspace: Bool
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
        showsWorkflowWorkspace: Binding<Bool>,
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
        _showsWorkflowWorkspace = showsWorkflowWorkspace
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: SkydownLayout.sectionSpacing) {
                if authManager.userSession == nil {
                    AIHubLoginCard(
                        colorScheme: colorScheme,
                        title: featureFlags.aiAccessMode == .adminOnly ? "KI wird vorbereitet" : "KI nur mit Konto",
                        message: featureFlags.aiAccessMessage(for: nil),
                        onOpenLogin: onOpenLogin
                    )
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                } else if !featureFlags.allowsAIAccess(for: authManager.userSession) {
                    AIHubRestrictedCard(
                        colorScheme: colorScheme,
                        message: featureFlags.aiAccessMessage(for: authManager.userSession),
                        onOpenSettings: onOpenSettings
                    )
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                } else {
                    AIHubCompactHeader(
                        mode: mode,
                        colorScheme: colorScheme,
                        showsWorkflowWorkspace: showsWorkflowWorkspace,
                        onSelectMode: {
                            showsWorkflowWorkspace = false
                            mode = $0
                        },
                        onToggleWorkflow: {
                            showsWorkflowWorkspace.toggle()
                        }
                    )
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.top, 12)

                    Group {
                        if showsWorkflowWorkspace {
                            AIWorkflowWorkspaceCard(
                                colorScheme: colorScheme,
                                onOpenSettings: onOpenSettings
                            ) {
                                showsWorkflowWorkspace = false
                            }
                        } else if mode == .bot {
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
            .navigationTitle("Tools")
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

    var body: some View {
        NavigationStack {
            VideoHubView()
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
    let title: String
    let message: String
    let onOpenLogin: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.title2.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(message)
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

private struct AIHubRestrictedCard: View {
    let colorScheme: ColorScheme
    let message: String
    let onOpenSettings: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("KI derzeit gesperrt")
                .font(.title2.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(message)
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 10) {
                AIHubBadge(text: "Bot", color: AppColors.accent(for: colorScheme))
                AIHubBadge(text: "Agent", color: AppColors.accentHighlight(for: colorScheme))
                AIHubBadge(text: "Visuals", color: AppColors.accentMystic(for: colorScheme))
            }

            Button(action: onOpenSettings) {
                Text("Einstellungen")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accentMystic(for: colorScheme))
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
    }
}

private struct AIHubCompactHeader: View {
    let mode: AIHubMode
    let colorScheme: ColorScheme
    let showsWorkflowWorkspace: Bool
    let onSelectMode: (AIHubMode) -> Void
    let onToggleWorkflow: () -> Void

    private var accent: Color {
        switch mode {
        case .bot:
            return AppColors.accent(for: colorScheme)
        case .agent:
            return AppColors.accentMystic(for: colorScheme)
        }
    }

    var body: some View {
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
                .buttonStyle(SkydownTactileButtonStyle())
            }

            Button(action: onToggleWorkflow) {
                HStack(spacing: 6) {
                    Image(systemName: showsWorkflowWorkspace ? "xmark.circle.fill" : "bolt.horizontal.circle.fill")
                        .font(.headline)
                    Text(showsWorkflowWorkspace ? "Zur KI" : "Automation")
                        .font(.subheadline.weight(.semibold))
                }
                .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                .padding(.horizontal, 12)
                .frame(height: 44)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                )
            }
            .buttonStyle(SkydownTactileButtonStyle())
        }
        .padding(10)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: accent,
            cornerRadius: 18,
            shadowRadius: 10,
            shadowYOffset: 6
        )
    }
}

private struct AIWorkflowWorkspaceCard: View {
    let colorScheme: ColorScheme
    let onOpenSettings: () -> Void
    let onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Automation")
                .font(.title2.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Hier bereitest du Automationen vor. Verbindungen dafuer bleiben getrennt vom normalen App-Login und werden in den Einstellungen vorbereitet.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 10) {
                AIHubBadge(text: "Automation", color: AppColors.accentHighlight(for: colorScheme))
                AIHubBadge(text: "Manuell", color: AppColors.accent(for: colorScheme))
                AIHubBadge(text: "Setup", color: AppColors.accentMystic(for: colorScheme))
            }

            Button(action: onOpenSettings) {
                Text("Einstellungen oeffnen")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accentHighlight(for: colorScheme))

            Button(action: onClose) {
                Text("Zur KI zurueck")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.bordered)
            .tint(AppColors.accent(for: colorScheme))
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
    }
}

private struct AIHubBadge: View {
    @Environment(\.colorScheme) private var colorScheme
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundColor(color)
            .padding(.horizontal, 11)
            .padding(.vertical, 7)
            .skydownCapsuleSurface(colorScheme: colorScheme, accent: color)
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
        .environmentObject(services.featureFlags)
        .environmentObject(services.authManager)
        .environmentObject(services.cartViewModel)
}
