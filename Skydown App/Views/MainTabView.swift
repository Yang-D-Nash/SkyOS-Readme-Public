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
                .tabItem { Label("Merchandise", systemImage: "bag.fill") }
                .tag(MainTab.merch)

                DeferredView {
                    ZweizweiTabView(
                        onOpenCart: { showingCart = true },
                        onOpenSettings: { showingSettings = true }
                    )
                }
                .tabItem { Label("Zweizwei", systemImage: "music.note.list") }
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
                .tabItem { Label("Home", systemImage: "square.grid.2x2.fill") }
                .tag(MainTab.hub)

                DeferredView {
                    VideoHubTabView(
                        onOpenCart: { showingCart = true },
                        onOpenSettings: { showingSettings = true }
                    )
                }
                .tabItem { Label("Skydown", systemImage: "video.fill") }
                .tag(MainTab.skydown)

                if hasAIAccess {
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
                    .tabItem { Label("Tools", systemImage: "wrench.and.screwdriver.fill") }
                    .tag(MainTab.tools)
                }
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
            CartView {
                showingSettings = true
            }
        }
        .sheet(isPresented: $showingLogin) {
            LoginView()
        }
        .onChange(of: hasAIAccess) { _, allowed in
            if !allowed && selectedTab == .tools {
                selectedTab = .hub
            }
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
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Zweizwei Music")
                                .font(.system(size: 38, weight: .black, design: .rounded))
                                .foregroundColor(AppColors.text(for: colorScheme))

                            Text("Hier hat Zweizwei seinen eigenen Musikbereich. Catalog, Beat Hub und NICMA Producer bleiben klar getrennt von Skydown Videography und Merchandise.")
                                .font(.headline)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                        .padding(SkydownLayout.heroPadding)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(AppColors.cardBackground(for: colorScheme))
                        .overlay(
                            RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                                .stroke(AppColors.spotify(for: colorScheme).opacity(0.22), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))

                        ShellActionCard(
                            title: "MUSIC CATALOG",
                            subtitle: "Artists, Releases, Preview-Playback und Spotify-Fokus unter Zweizwei.",
                            accent: AppColors.spotify(for: colorScheme)
                        ) {
                            destination = .catalog
                        }

                        ShellActionCard(
                            title: "BEAT HUB",
                            subtitle: "Eigene Beat-Logik, Preview-Library und Upload-/Listener-Flow.",
                            accent: AppColors.accent(for: colorScheme)
                        ) {
                            destination = .beatHub
                        }

                        ShellActionCard(
                            title: "NICMA PRODUCER",
                            subtitle: "Mixing, Mastering und Recording als eigener Music-Service.",
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
                .navigationTitle("Zweizwei")
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
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(title)
                        .font(.system(size: 24, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(subtitle)
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .multilineTextAlignment(.leading)
                }

                Spacer(minLength: 12)

                Image(systemName: "arrow.up.right")
                    .font(.title3.weight(.bold))
                    .foregroundColor(accent)
                    .padding(12)
                    .background(accent.opacity(0.14))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .padding(SkydownLayout.heroPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.cardBackground(for: colorScheme))
            .overlay {
                RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                    .stroke(accent.opacity(0.22), lineWidth: 1)
            }
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))
            .shadow(color: .black.opacity(colorScheme == .dark ? 0.26 : 0.08), radius: 18, y: 10)
        }
        .buttonStyle(.plain)
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
                        title: featureFlags.aiAccessMode == .adminOnly ? "KI nur fuer Admins" : "KI nur mit Konto",
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
                AIHubBadge(text: "Admin Only", color: AppColors.accentMystic(for: colorScheme))
                AIHubBadge(text: "Bot", color: AppColors.accent(for: colorScheme))
                AIHubBadge(text: "Agent", color: AppColors.accentHighlight(for: colorScheme))
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
                .buttonStyle(.plain)
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
            .buttonStyle(.plain)
        }
        .padding(12)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(accent.opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
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

            Text("Hier bereitest du versteckte Trigger und spaetere Automationen vor. Die Google-Verbindung dafuer bleibt bewusst getrennt vom normalen App-Login und wird in den Einstellungen vorbereitet.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 10) {
                AIHubBadge(text: "Versteckt", color: AppColors.accentHighlight(for: colorScheme))
                AIHubBadge(text: "Manuell", color: AppColors.accent(for: colorScheme))
                AIHubBadge(text: "Admin Setup", color: AppColors.accentMystic(for: colorScheme))
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
        .environmentObject(services.featureFlags)
        .environmentObject(services.authManager)
        .environmentObject(services.cartViewModel)
}
