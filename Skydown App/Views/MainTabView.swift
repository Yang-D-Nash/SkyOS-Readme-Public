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

private enum ZweizweiDestination: Equatable {
    case hub
    case catalog
    case beatHub
    case nicma
}

private enum MainTabModal: String, Identifiable, Equatable {
    case settings
    case profile
    case cart
    case login

    var id: String { rawValue }
}

struct MainTabView: View {
    @AppStorage("colorScheme") private var colorScheme: String = "system"
    @Environment(\.colorScheme) private var systemColorScheme
    @EnvironmentObject private var services: AppServices
    @EnvironmentObject private var featureFlags: FeatureFlagsService
    @EnvironmentObject private var authManager: AuthManager
    @State private var selectedTab: MainTab = .hub
    @State private var activeModal: MainTabModal?
    @State private var queuedModal: MainTabModal?
    @State private var showsWorkflowWorkspace = false
    @State private var settingsInitialAdminWorkspaceRawValue: String?

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

    private var selectedTabAccent: Color {
        switch selectedTab {
        case .merch:
            return AppColors.accentHighlight(for: currentScheme)
        case .zweizwei:
            return AppColors.spotify(for: currentScheme)
        case .hub:
            return AppColors.accent(for: currentScheme)
        case .skydown:
            return AppColors.youtube(for: currentScheme)
        case .tools:
            return AppColors.accentMystic(for: currentScheme)
        }
    }

    private func localized(_ key: String, _ fallback: String) -> String {
        AppLocalized.text(key, fallback: fallback)
    }

    var body: some View {
        Group {
            if SkydownPlatform.isDesktop {
                rootTabView
                    .inspector(isPresented: desktopAccessoryPresented) {
                        if let activeModal {
                            modalContent(for: activeModal)
                                .inspectorColumnWidth(min: 320, ideal: 420, max: 520)
                        }
                    }
            } else {
                rootTabView
                    .sheet(item: $activeModal) { modal in
                        modalContent(for: modal)
                    }
            }
        }
        .accentColor(selectedTabAccent)
        .background(AppColors.primaryBackground(for: currentScheme).edgesIgnoringSafeArea(.all))
        .preferredColorScheme(preferredScheme)
        .animation(SkydownMotion.emphasizedTransition, value: selectedTab)
        .onChange(of: hasAIAccess) { _, allowed in
            if !allowed {
                showsWorkflowWorkspace = false
            }
        }
        .onChange(of: selectedTab) { _, newTab in
            if newTab == .tools {
                Task {
                    await featureFlags.refresh()
                    await authManager.refreshCurrentUser()
                }
            } else {
                showsWorkflowWorkspace = false
            }
        }
        .onChange(of: activeModal) { _, modal in
            guard modal == nil, let queuedModal else { return }
            self.queuedModal = nil
            DispatchQueue.main.async {
                activeModal = queuedModal
            }
        }
    }

    private var rootTabView: some View {
        TabView(selection: selectedTabBinding) {
            Group {
                DeferredView {
                    ShopView(
                        authManager: services.authManager,
                        onOpenLogin: { presentModal(.login) },
                        onOpenCart: { presentModal(.cart) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() },
                        merchandiseService: services.merchandiseService
                    )
                    .skydownSceneActivation(isActive: selectedTab == .merch, axis: .horizontal, travel: 28)
                }
                .tabItem { Label(localized("tabs.merch", "Merch"), systemImage: "bag.fill") }
                .tag(MainTab.merch)

                DeferredView {
                    ZweizweiTabView(
                        onOpenCart: { presentModal(.cart) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() }
                    )
                    .skydownSceneActivation(isActive: selectedTab == .zweizwei, axis: .horizontal, travel: 28)
                }
                .tabItem { Label(localized("tabs.music", "Music"), systemImage: "waveform.circle.fill") }
                .tag(MainTab.zweizwei)

                DeferredView {
                    HomeView(
                        onOpenCart: { presentModal(.cart) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() },
                        onOpenWorkflow: hasAIAccess ? {
                            withAnimation(SkydownMotion.screenTransition) {
                                showsWorkflowWorkspace = true
                                selectedTab = .tools
                            }
                        } : nil
                    )
                    .skydownSceneActivation(isActive: selectedTab == .hub, axis: .horizontal, travel: 28)
                }
                .tabItem { Label(localized("tabs.home", "Home"), systemImage: "house.fill") }
                .tag(MainTab.hub)

                DeferredView {
                    VideoHubTabView(
                        onOpenCart: { presentModal(.cart) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() }
                    )
                    .skydownSceneActivation(isActive: selectedTab == .skydown, axis: .horizontal, travel: 28)
                }
                .tabItem { Label(localized("tabs.videos", "Videos"), systemImage: "play.rectangle.fill") }
                .tag(MainTab.skydown)

                DeferredView {
                    AIHubView(
                        aiChatService: services.aiChatService,
                        agentChatService: services.agentChatService,
                        featureFlags: services.featureFlags,
                        showsWorkflowWorkspace: $showsWorkflowWorkspace,
                        onOpenCart: { presentModal(.cart) },
                        onOpenLogin: { presentModal(.login) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() },
                        onOpenAutomationSettings: { presentSettings(initialAdminWorkspaceRawValue: "Automation") }
                    )
                    .skydownSceneActivation(isActive: selectedTab == .tools, axis: .horizontal, travel: 28)
                }
                .tabItem { Label(localized("tabs.tools", "Tools"), systemImage: "sparkles") }
                .tag(MainTab.tools)
            }
            .skydownTabBarChrome(colorScheme: currentScheme, accent: selectedTabAccent)
        }
        .skydownSelectionFeedback(trigger: selectedTab)
    }

    private var selectedTabBinding: Binding<MainTab> {
        Binding(
            get: { selectedTab },
            set: { newTab in
                withAnimation(SkydownMotion.screenTransition) {
                    selectedTab = newTab
                }
                if activeModal == .settings {
                    activeModal = nil
                }
            }
        )
    }

    private var desktopAccessoryPresented: Binding<Bool> {
        Binding(
            get: { activeModal != nil },
            set: { isPresented in
                if !isPresented {
                    activeModal = nil
                }
            }
        )
    }

    @ViewBuilder
    private func modalContent(for modal: MainTabModal) -> some View {
        switch modal {
        case .settings:
            DeferredSettingsPresentation(
                colorScheme: $colorScheme,
                initialAdminWorkspaceRawValue: settingsInitialAdminWorkspaceRawValue
            )
        case .profile:
            ProfileView(authManager: services.authManager)
        case .cart:
            CartView(
                onOpenProfile: { presentModal(.profile) },
                onOpenSettings: { presentSettings() }
            )
        case .login:
            LoginView()
        }
    }

    private func presentModal(_ modal: MainTabModal) {
        guard activeModal != modal else { return }
        guard activeModal == nil else {
            queuedModal = modal
            activeModal = nil
            return
        }

        withAnimation(SkydownMotion.emphasizedTransition) {
            activeModal = modal
        }
    }

    private func presentSettings(initialAdminWorkspaceRawValue: String? = nil) {
        settingsInitialAdminWorkspaceRawValue = initialAdminWorkspaceRawValue
        presentModal(.settings)
    }
}

private struct DeferredSettingsPresentation: View {
    @Binding var colorScheme: String
    let initialAdminWorkspaceRawValue: String?
    @Environment(\.colorScheme) private var systemColorScheme
    @State private var isReady = false

    private var effectiveColorScheme: ColorScheme {
        switch colorScheme {
        case "light":
            return .light
        case "dark":
            return .dark
        default:
            return systemColorScheme
        }
    }

    var body: some View {
        Group {
            if isReady {
                SettingsView(
                    colorScheme: $colorScheme,
                    initialAdminWorkspaceRawValue: initialAdminWorkspaceRawValue
                )
            } else {
                ZStack {
                    AppColors.screenGradient(
                        for: effectiveColorScheme,
                        secondaryAccent: AppColors.accentHighlight(for: effectiveColorScheme)
                    )
                    .ignoresSafeArea()

                    VStack(spacing: 14) {
                        ProgressView()
                            .progressViewStyle(.circular)
                            .tint(AppColors.accent(for: effectiveColorScheme))

                        Text(AppLocalized.text("settings.loading", fallback: "Loading settings"))
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: effectiveColorScheme))
                    }
                }
                .task {
                    guard !isReady else { return }
                    await Task.yield()
                    isReady = true
                }
            }
        }
    }
}

struct AppSessionToolbarActions: View {
    let onOpenCart: (() -> Void)?
    let onOpenProfile: (() -> Void)?
    let onOpenSettings: () -> Void
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme

    init(
        onOpenCart: (() -> Void)? = nil,
        onOpenProfile: (() -> Void)? = nil,
        onOpenSettings: @escaping () -> Void
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
    }

    private var displayName: String {
        let trimmed = authManager.userSession?.username.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? AppLocalized.text("common.guest", fallback: "Guest") : trimmed
    }

    private var initials: String {
        String(displayName.prefix(1)).uppercased()
    }

    private var sessionAccent: Color {
        authManager.userSession == nil
            ? AppColors.accentMystic(for: colorScheme)
            : AppColors.accent(for: colorScheme)
    }

    var body: some View {
        HStack(spacing: 10) {
            Button(action: authManager.userSession == nil ? onOpenSettings : (onOpenProfile ?? onOpenSettings)) {
                HStack(spacing: 10) {
                    ZStack(alignment: .bottomTrailing) {
                        if let profileImageURL = authManager.userSession?.profileImageURL,
                           let url = URL(string: profileImageURL) {
                            AsyncImage(url: url) { image in
                                image
                                    .resizable()
                                    .scaledToFill()
                            } placeholder: {
                                profileFallbackAvatar
                            }
                            .frame(width: 28, height: 28)
                            .clipShape(Circle())
                        } else {
                            profileFallbackAvatar
                        }

                        Circle()
                            .fill(sessionAccent)
                            .frame(width: 9, height: 9)
                            .overlay(
                                Circle()
                                    .stroke(AppColors.cardBackground(for: colorScheme), lineWidth: 1.5)
                            )
                            .offset(x: 2, y: 1)
                    }

                    Text(displayName)
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(1)

                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.88))
                }
            }
            .padding(.horizontal, 11)
            .padding(.vertical, 6)
            .skydownCapsuleSurface(
                colorScheme: colorScheme,
                accent: sessionAccent
            )
            .buttonStyle(.plain)
            .skydownTactileAction()

            if let onOpenCart {
                SessionToolbarIconButton(
                    systemName: "bag.fill",
                    accessibilityID: "app.open_cart",
                    accent: AppColors.accentHighlight(for: colorScheme),
                    colorScheme: colorScheme,
                    action: onOpenCart
                )
            }

            SessionToolbarIconButton(
                systemName: "gearshape.fill",
                accessibilityID: "app.open_settings",
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                action: onOpenSettings
            )
        }
    }

    private var profileFallbackAvatar: some View {
        ZStack {
            Circle()
                .fill(sessionAccent.opacity(0.16))
                .frame(width: 28, height: 28)

            Text(initials)
                .font(.caption.weight(.bold))
                .foregroundColor(sessionAccent)
        }
    }
}

private struct SessionToolbarIconButton: View {
    let systemName: String
    let accessibilityID: String
    let accent: Color
    let colorScheme: ColorScheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .padding(10)
                .skydownCapsuleSurface(
                    colorScheme: colorScheme,
                    accent: accent
                )
        }
        .accessibilityIdentifier(accessibilityID)
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private struct ZweizweiTabView: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared
    @State private var destination: ZweizweiDestination = .hub
    @State private var catalogInitialArtist: String?
    @State private var catalogAutoPresentArtistPage = false
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void

    var body: some View {
        Group {
            switch destination {
            case .hub:
                NavigationStack {
                    GeometryReader { proxy in
                        let layout = SkydownResponsiveLayout(availableWidth: proxy.size.width)
                        let contentWidth = min(
                            layout.contentMaxWidth,
                            max(proxy.size.width - (layout.horizontalPadding * 2), 0)
                        )

                        ScrollView {
                            VStack(alignment: .leading, spacing: layout.sectionSpacing) {
                            BrandHeroSurface(
                                colorScheme: colorScheme,
                                eyebrow: screenHeaderSettingsStore.settings.resolvedMusicHubEyebrow ?? "Music",
                                title: screenHeaderSettingsStore.settings.resolvedMusicHubTitle ?? "Music",
                                subtitle: screenHeaderSettingsStore.settings.resolvedMusicHubSubtitle ?? "Releases, Artists und Studio an einem Ort.",
                                detail: screenHeaderSettingsStore.settings.resolvedMusicHubDetail ?? "Hoer rein, entdecke Artists und spring direkt zu Beats oder Recording.",
                                backgroundImageURL: screenHeaderSettingsStore.settings.resolvedMusicHubImageURL,
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

                            if layout.prefersThreeColumn {
                                HStack(spacing: 12) {
                                    MusicHubStatusCard(
                                        title: "Catalog",
                                        value: "Artists, Tracks, Pages",
                                        accent: AppColors.spotify(for: colorScheme)
                                    )
                                    MusicHubStatusCard(
                                        title: "Beat Hub",
                                        value: "Schnell den Vibe finden",
                                        accent: AppColors.accent(for: colorScheme)
                                    )
                                    MusicHubStatusCard(
                                        title: "Studio",
                                        value: "Recording, Mix, Master",
                                        accent: AppColors.accentMystic(for: colorScheme)
                                    )
                                }
                            }

                            if layout.prefersTwoColumn {
                                VStack(spacing: 12) {
                                    ShellActionCard(
                                        eyebrow: "Catalog",
                                        title: "Songs & Artists",
                                        subtitle: "Starte mit JANNO und finde direkt alle Artists, Songs und Pages.",
                                        detail: "Das ist der schnellste Weg in den Music-Katalog mit Preview, Spotify und Artist-Pages.",
                                        accent: AppColors.spotify(for: colorScheme),
                                        systemImage: "waveform.circle.fill",
                                        badges: ["Tracks", "Spotify", "Pages"],
                                        accessibilityID: "music.hub.open_catalog"
                                    ) {
                                        catalogInitialArtist = "JANNO"
                                        catalogAutoPresentArtistPage = false
                                        withAnimation(SkydownMotion.screenTransition) {
                                            destination = .catalog
                                        }
                                    }

                                    HStack(alignment: .top, spacing: 12) {
                                        ShellActionCard(
                                            eyebrow: "Beat Hub",
                                            title: "Beat Library",
                                            subtitle: "Beats anhoeren und schnell den richtigen Vibe finden.",
                                            detail: "Direkt in Beat-Playback, Auswahl und Upload-nahe Workflows springen.",
                                            accent: AppColors.accent(for: colorScheme),
                                            systemImage: "speaker.wave.3.fill",
                                            badges: ["Playback", "Selection", "Flow"]
                                        ) {
                                            withAnimation(SkydownMotion.screenTransition) {
                                                destination = .beatHub
                                            }
                                        }

                                        ShellActionCard(
                                            eyebrow: "Studio",
                                            title: "Studio Services",
                                            subtitle: "Recording, Mixing und Mastering direkt anfragen.",
                                            detail: "Die Services bleiben auf einen Blick erreichbar, ohne den Music-Flow zu verlassen.",
                                            accent: AppColors.accentMystic(for: colorScheme),
                                            systemImage: "sparkles",
                                            badges: ["Record", "Mix", "Master"]
                                        ) {
                                            withAnimation(SkydownMotion.screenTransition) {
                                                destination = .nicma
                                            }
                                        }
                                    }
                                }
                            } else {
                                VStack(spacing: 12) {
                                    ShellActionCard(
                                        eyebrow: "Catalog",
                                        title: "Songs & Artists",
                                        subtitle: "Starte mit JANNO und finde direkt alle Artists, Songs und Pages.",
                                        detail: "Das ist der schnellste Weg in den Music-Katalog mit Preview, Spotify und Artist-Pages.",
                                        accent: AppColors.spotify(for: colorScheme),
                                        systemImage: "waveform.circle.fill",
                                        badges: ["Tracks", "Spotify", "Pages"],
                                        accessibilityID: "music.hub.open_catalog"
                                    ) {
                                        catalogInitialArtist = "JANNO"
                                        catalogAutoPresentArtistPage = false
                                        withAnimation(SkydownMotion.screenTransition) {
                                            destination = .catalog
                                        }
                                    }

                                    ShellActionCard(
                                        eyebrow: "Beat Hub",
                                        title: "Beat Library",
                                        subtitle: "Beats anhoeren und schnell den richtigen Vibe finden.",
                                        detail: "Direkt in Beat-Playback, Auswahl und Upload-nahe Workflows springen.",
                                        accent: AppColors.accent(for: colorScheme),
                                        systemImage: "speaker.wave.3.fill",
                                        badges: ["Playback", "Selection", "Flow"]
                                    ) {
                                        withAnimation(SkydownMotion.screenTransition) {
                                            destination = .beatHub
                                        }
                                    }

                                    ShellActionCard(
                                        eyebrow: "Studio",
                                        title: "Studio Services",
                                        subtitle: "Recording, Mixing und Mastering direkt anfragen.",
                                        detail: "Die Services bleiben auf einen Blick erreichbar, ohne den Music-Flow zu verlassen.",
                                        accent: AppColors.accentMystic(for: colorScheme),
                                        systemImage: "sparkles",
                                        badges: ["Record", "Mix", "Master"]
                                    ) {
                                        withAnimation(SkydownMotion.screenTransition) {
                                            destination = .nicma
                                        }
                                    }
                                }
                            }
                        }
                        .frame(maxWidth: contentWidth, alignment: .leading)
                        .padding(.horizontal, layout.horizontalPadding)
                        .padding(.top, SkydownLayout.screenTopPadding)
                        .padding(.bottom, SkydownLayout.screenBottomPadding)
                        .frame(maxWidth: .infinity)
                    }
                    .accessibilityIdentifier("music.hub.root")
                    .scrollIndicators(.hidden)
                }
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
                            onOpenProfile: onOpenProfile,
                            onOpenSettings: onOpenSettings
                        )
                    }
                }
            }
        case .catalog:
            MusicView(
                brand: .zweizwei,
                initialArtist: catalogInitialArtist,
                autoPresentArtistPageOnAppear: catalogAutoPresentArtistPage,
                onBack: {
                    catalogInitialArtist = nil
                    catalogAutoPresentArtistPage = false
                    withAnimation(SkydownMotion.screenTransition) {
                        destination = .hub
                    }
                },
                onOpenCart: onOpenCart,
                onOpenProfile: onOpenProfile,
                onOpenSettings: onOpenSettings
            )
        case .beatHub:
            NavigationStack {
                BeatHubView {
                    withAnimation(SkydownMotion.screenTransition) {
                        destination = .hub
                    }
                }
            }
        case .nicma:
            NavigationStack {
                NicmaProducerView {
                    withAnimation(SkydownMotion.screenTransition) {
                        destination = .hub
                    }
                    }
                }
            }
        }
        .skydownSceneMotion(trigger: destination, axis: .horizontal, travel: 30)
        .skydownSelectionFeedback(trigger: destination)
    }
}

private struct ShellActionCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let eyebrow: String
    let title: String
    let subtitle: String
    let detail: String
    let accent: Color
    let systemImage: String
    let badges: [String]
    let accessibilityID: String?
    let action: () -> Void

    init(
        eyebrow: String,
        title: String,
        subtitle: String,
        detail: String,
        accent: Color,
        systemImage: String,
        badges: [String],
        accessibilityID: String? = nil,
        action: @escaping () -> Void
    ) {
        self.eyebrow = eyebrow
        self.title = title
        self.subtitle = subtitle
        self.detail = detail
        self.accent = accent
        self.systemImage = systemImage
        self.badges = badges
        self.accessibilityID = accessibilityID
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 16) {
                HStack(alignment: .top, spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .fill(
                                LinearGradient(
                                    colors: [
                                        accent.opacity(colorScheme == .dark ? 0.24 : 0.18),
                                        AppColors.cardBackground(for: colorScheme)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )

                        Image(systemName: systemImage)
                            .font(.title3.weight(.bold))
                            .foregroundColor(accent)
                    }
                    .frame(width: 54, height: 54)

                    VStack(alignment: .leading, spacing: 6) {
                        Text(eyebrow.uppercased())
                            .font(AppTypography.heroEyebrow)
                            .tracking(1.1)
                            .foregroundColor(accent)

                        Text(title)
                            .font(.system(size: 20, weight: .black, design: .rounded))
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(subtitle)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .multilineTextAlignment(.leading)
                    }

                    Spacer(minLength: 10)

                    Image(systemName: "arrow.up.right")
                        .font(.headline.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .padding(10)
                        .background(
                            Circle()
                                .fill(accent.opacity(colorScheme == .dark ? 0.14 : 0.10))
                        )
                }

                Text(detail)
                    .font(.footnote.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .multilineTextAlignment(.leading)

                HStack(spacing: 8) {
                    ForEach(badges, id: \.self) { badge in
                        Text(badge)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(accent)
                            .padding(.horizontal, 11)
                            .padding(.vertical, 7)
                            .skydownCapsuleSurface(colorScheme: colorScheme, accent: accent)
                    }
                }
            }
            .padding(SkydownLayout.heroPadding)
            .frame(maxWidth: .infinity, minHeight: 210, alignment: .leading)
            .skydownPanelSurface(
                colorScheme: colorScheme,
                accent: accent,
                cornerRadius: SkydownLayout.heroCornerRadius,
                shadowRadius: 14,
                shadowYOffset: 8
            )
        }
        .accessibilityIdentifier(accessibilityID ?? title)
        .buttonStyle(SkydownTactileButtonStyle())
    }
}

private struct MusicHubStatusCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let value: String
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title.uppercased())
                .font(AppTypography.heroEyebrow)
                .tracking(1.1)
                .foregroundColor(accent)

            Text(value)
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .multilineTextAlignment(.leading)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: accent,
            cornerRadius: 20,
            shadowRadius: 10,
            shadowYOffset: 6
        )
    }
}

private struct AIHubView: View {
    let aiChatService: AIChatServicing
    let agentChatService: AgentChatServicing
    @ObservedObject private var featureFlags: FeatureFlagsService
    @Binding var showsWorkflowWorkspace: Bool
    let onOpenCart: () -> Void
    let onOpenLogin: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onOpenAutomationSettings: () -> Void
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
        onOpenProfile: @escaping () -> Void,
        onOpenSettings: @escaping () -> Void,
        onOpenAutomationSettings: @escaping () -> Void
    ) {
        self.aiChatService = aiChatService
        self.agentChatService = agentChatService
        self.onOpenCart = onOpenCart
        self.onOpenLogin = onOpenLogin
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onOpenAutomationSettings = onOpenAutomationSettings
        _featureFlags = ObservedObject(wrappedValue: featureFlags)
        _showsWorkflowWorkspace = showsWorkflowWorkspace
    }

    private var motionState: String {
        if authManager.userSession == nil {
            return "login"
        }

        if !featureFlags.allowsAIAccess(for: authManager.userSession) {
            return "restricted"
        }

        if showsWorkflowWorkspace {
            return "workflow"
        }

        return "mode-\(mode.rawValue)"
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 10) {
                if authManager.userSession == nil {
                    AIHubLoginCard(
                        colorScheme: colorScheme,
                        title: featureFlags.aiAccessMode == .adminOnly ? "KI nur fuer freigegebene Konten" : "KI nur mit Konto",
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
                        onSelectMode: { newMode in
                            withAnimation(SkydownMotion.screenTransition) {
                                showsWorkflowWorkspace = false
                                mode = newMode
                            }
                        },
                        onToggleWorkflow: {
                            withAnimation(SkydownMotion.screenTransition) {
                                showsWorkflowWorkspace.toggle()
                            }
                        }
                    )
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.top, 8)

                    Group {
                        if showsWorkflowWorkspace {
                            AIWorkflowWorkspaceCard(
                                colorScheme: colorScheme,
                                onOpenSettings: onOpenAutomationSettings
                            ) {
                                withAnimation(SkydownMotion.screenTransition) {
                                    showsWorkflowWorkspace = false
                                }
                            }
                            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
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
                    .skydownSceneMotion(trigger: motionState, axis: .horizontal, travel: 26)
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
                        onOpenProfile: onOpenProfile,
                        onOpenSettings: onOpenSettings
                    )
                }
            }
        }
    }
}

private struct VideoHubTabView: View {
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void

    var body: some View {
        NavigationStack {
            VideoHubView()
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        AppSessionToolbarActions(
                            onOpenCart: onOpenCart,
                            onOpenProfile: onOpenProfile,
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
        HStack(spacing: 7) {
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
                    .padding(.vertical, 9)
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
                    Text(showsWorkflowWorkspace ? "Zur KI" : "Workflow")
                        .font(.subheadline.weight(.semibold))
                }
                .foregroundColor(AppColors.text(for: colorScheme))
                .padding(.horizontal, 12)
                .frame(height: 42)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(
                            LinearGradient(
                                colors: [
                                    AppColors.accentHighlight(for: colorScheme).opacity(colorScheme == .dark ? 0.18 : 0.22),
                                    AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.08 : 0.84)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(AppColors.accentHighlight(for: colorScheme).opacity(0.22), lineWidth: 1)
                )
            }
            .buttonStyle(SkydownTactileButtonStyle())
        }
        .padding(8)
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
        VStack(alignment: .leading, spacing: 14) {
            Text("Workflow Cockpit")
                .font(.title2.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Verbinde Agent, Kontext und Aktionen so, dass auch Laien sofort wissen, was passiert.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(alignment: .leading, spacing: 10) {
                AIWorkflowStepRow(index: "01", title: "Briefing", detail: "Der Agent macht aus einer Idee einen klaren Plan.", colorScheme: colorScheme)
                AIWorkflowStepRow(index: "02", title: "Aktion", detail: "Optional wird dein n8n-Workflow mit User-Kontext gestartet.", colorScheme: colorScheme)
                AIWorkflowStepRow(index: "03", title: "Rueckweg", detail: "Du bleibst in der App und kannst direkt weiterarbeiten.", colorScheme: colorScheme)
            }

            HStack(spacing: 10) {
                AIHubBadge(text: "n8n", color: AppColors.accentHighlight(for: colorScheme))
                AIHubBadge(text: "Agent", color: AppColors.accent(for: colorScheme))
                AIHubBadge(text: "Kontext", color: AppColors.accentMystic(for: colorScheme))
            }

            Button(action: onOpenSettings) {
                Text("Workflow einrichten")
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

private struct AIWorkflowStepRow: View {
    let index: String
    let title: String
    let detail: String
    let colorScheme: ColorScheme

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(index)
                .font(.caption.weight(.bold))
                .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                .frame(width: 38, height: 38)
                .background(
                    Circle()
                        .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                )

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text(detail)
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
    }
}

private struct AIHubBadge: View {
    let text: String
    let color: Color

    var body: some View {
        SkydownMetaLabel(text: text, tint: color)
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
