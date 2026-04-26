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
    case nicma
}

private enum MainTabModal: Identifiable, Equatable {
    case settings
    case profile
    case cart
    case login(AuthEntryContext)

    var id: String {
        switch self {
        case .settings: return "settings"
        case .profile: return "profile"
        case .cart: return "cart"
        case .login(let context): return "login-\(context.rawValue)"
        }
    }
}

private enum TopBarPreset {
    static func useMiniMode(horizontalSizeClass: UserInterfaceSizeClass?) -> Bool {
        horizontalSizeClass == .compact
    }

    static func sessionChipHorizontalPadding(useMiniMode: Bool) -> CGFloat {
        useMiniMode ? 9 : 11
    }

    static func sessionChipVerticalPadding(useMiniMode: Bool) -> CGFloat {
        useMiniMode ? 5 : 6
    }

    static func actionIconPadding(horizontalSizeClass: UserInterfaceSizeClass?) -> CGFloat {
        horizontalSizeClass == .compact ? 8 : 10
    }
}

struct MainTabView: View {
    @AppStorage("colorScheme") private var colorScheme: String = "system"
    @Environment(\.colorScheme) private var systemColorScheme
    @EnvironmentObject private var services: AppServices
    @EnvironmentObject private var featureFlags: FeatureFlagsService
    @EnvironmentObject private var authManager: AuthManager
    @State private var selectedTab: MainTab = .hub
    @State private var lastNonToolsTab: MainTab = .hub
    @State private var modalPresentation = SkydownQueuedPresentation<MainTabModal>()
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
        _lastNonToolsTab = State(initialValue: initialTab == .tools ? .hub : initialTab)
    }

    private var hasAIAccess: Bool {
        featureFlags.allowsAIAccess(for: authManager.userSession)
    }

    /// Single SkyOS chrome accent for tab bar + `.accentColor` — avoids per-tab Spotify/YouTube retail colors reading as five separate products.
    private var selectedTabAccent: Color {
        AppColors.accentMystic(for: currentScheme)
    }

    private func localized(_ key: String, _ fallback: String) -> String {
        AppLocalized.text(key, fallback: fallback)
    }

    var body: some View {
        Group {
            if SkydownPlatform.isDesktop {
                rootTabView
                    .inspector(isPresented: desktopAccessoryPresented) {
                        if let activeModal = modalPresentation.activeItem {
                            modalContent(for: activeModal)
                                .inspectorColumnWidth(min: 320, ideal: 420, max: 520)
                        }
                    }
            } else {
                rootTabView
                    .sheet(item: activeModalBinding) { modal in
                        modalContent(for: modal)
                            .presentationDragIndicator(.visible)
                            .presentationCornerRadius(28)
                    }
            }
        }
        .accentColor(selectedTabAccent)
        .background {
            AppColors.primaryBackground(for: currentScheme)
                .edgesIgnoringSafeArea(.all)
                .overlay {
                    SkydownAtmosphereBackdrop(colorScheme: currentScheme)
                }
        }
        .preferredColorScheme(preferredScheme)
        .animation(SkydownMotion.tabContextTransition, value: selectedTab)
        .onChange(of: hasAIAccess) { _, allowed in
            if !allowed {
                showsWorkflowWorkspace = false
            }
        }
        .onChange(of: selectedTab) { _, newTab in
            if newTab != .tools {
                lastNonToolsTab = newTab
                showsWorkflowWorkspace = false
            }
        }
        .task(id: selectedTab) {
            guard selectedTab == .tools else { return }
            await featureFlags.refresh()
            await authManager.refreshCurrentUser()
        }
    }

    private var rootTabView: some View {
        TabView(selection: selectedTabBinding) {
            Group {
                DeferredView {
                    ShopView(
                        authManager: services.authManager,
                        onOpenLogin: { presentLogin(.merchShop) },
                        onOpenCart: { presentModal(.cart) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() },
                        onGuestSignIn: { presentLogin(.standard) },
                        merchandiseService: services.merchandiseService
                    )
                    .skydownSceneActivation(isActive: selectedTab == .merch, axis: .horizontal, travel: 22)
                }
                .tabItem { Label(localized("tabs.merch", "Merch"), systemImage: "bag.fill") }
                .tag(MainTab.merch)

                DeferredView {
                    ZweizweiTabView(
                        onOpenCart: { presentModal(.cart) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() },
                        onGuestSignIn: { presentLogin(.music) }
                    )
                    .skydownSceneActivation(isActive: selectedTab == .zweizwei, axis: .horizontal, travel: 22)
                }
                .tabItem { Label(localized("tabs.music", "Music"), systemImage: "waveform.circle.fill") }
                .tag(MainTab.zweizwei)

                DeferredView {
                    HomeView(
                        onOpenCart: { presentModal(.cart) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() },
                        onGuestSignIn: { presentLogin(.standard) },
                        onOpenWorkflow: hasAIAccess ? {
                            withAnimation(SkydownMotion.screenTransition) {
                                showsWorkflowWorkspace = true
                                selectedTab = .tools
                            }
                        } : nil
                    )
                    .skydownSceneActivation(isActive: selectedTab == .hub, axis: .horizontal, travel: 22)
                }
                .tabItem { Label(localized("tabs.home", "Home"), systemImage: "house.fill") }
                .tag(MainTab.hub)

                DeferredView {
                    VideoHubTabView(
                        onOpenCart: { presentModal(.cart) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() },
                        onGuestSignIn: { presentLogin(.standard) }
                    )
                    .skydownSceneActivation(isActive: selectedTab == .skydown, axis: .horizontal, travel: 22)
                }
                .tabItem { Label(localized("tabs.videos", "Videos"), systemImage: "play.rectangle.fill") }
                .tag(MainTab.skydown)

                DeferredView {
                    AIHubView(
                        aiChatService: services.aiChatService,
                        agentChatService: services.agentChatService,
                        featureFlags: services.featureFlags,
                        showsWorkflowWorkspace: $showsWorkflowWorkspace,
                        onExitImmersive: exitAITools,
                        onOpenCart: { presentModal(.cart) },
                        onOpenLogin: { presentLogin(.ai) },
                        onGuestSignIn: { presentLogin(.standard) },
                        onOpenProfile: { presentModal(.profile) },
                        onOpenSettings: { presentSettings() },
                        onOpenAutomationSettings: { presentSettings(initialAdminWorkspaceRawValue: "Automation") }
                    )
                    .skydownSceneActivation(isActive: selectedTab == .tools, axis: .horizontal, travel: 22)
                }
                .tabItem { Label(localized("tabs.tools", "AI"), systemImage: "sparkles") }
                .tag(MainTab.tools)
            }
            .skydownTabBarChrome(
                colorScheme: currentScheme,
                accent: selectedTabAccent,
                isVisible: selectedTab != .tools || SkydownPlatform.isDesktop
            )
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
                if modalPresentation.activeItem == .settings {
                    modalPresentation.updatePresentedItem(nil)
                }
            }
        )
    }

    private var activeModalBinding: Binding<MainTabModal?> {
        Binding(
            get: { modalPresentation.activeItem },
            set: { modalPresentation.updatePresentedItem($0) }
        )
    }

    private func exitAITools() {
        let fallbackTab = lastNonToolsTab == .tools ? .hub : lastNonToolsTab
        withAnimation(SkydownMotion.screenTransition) {
            showsWorkflowWorkspace = false
            selectedTab = fallbackTab
        }
    }

    private var desktopAccessoryPresented: Binding<Bool> {
        Binding(
            get: { modalPresentation.activeItem != nil },
            set: { isPresented in
                if !isPresented {
                    modalPresentation.updatePresentedItem(nil)
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
            ProfileView(
                authManager: services.authManager,
                onOpenSettings: { presentSettings() }
            )
        case .cart:
            CartView(
                onOpenProfile: { presentModal(.profile) },
                onOpenSettings: { presentSettings() },
                onGuestSignIn: { presentLogin(.standard) }
            )
        case .login(let context):
            LoginView(entryContext: context)
        }
    }

    private func presentModal(_ modal: MainTabModal) {
        modalPresentation.request(modal)
    }

    private func presentLogin(_ context: AuthEntryContext) {
        presentModal(.login(context))
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
        ZStack {
            if isReady {
                SettingsView(
                    colorScheme: $colorScheme,
                    initialAdminWorkspaceRawValue: initialAdminWorkspaceRawValue
                )
                .transition(.opacity)
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
                .transition(.opacity)
            }
        }
        .task {
            guard !isReady else { return }
            await Task.yield()
            withAnimation(SkydownMotion.statusTransition) {
                isReady = true
            }
        }
    }
}

struct AppSessionToolbarActions: View {
    let onOpenCart: (() -> Void)?
    let onOpenProfile: (() -> Void)?
    let onOpenSettings: () -> Void
    let onGuestSignIn: (() -> Void)?
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    init(
        onOpenCart: (() -> Void)? = nil,
        onOpenProfile: (() -> Void)? = nil,
        onOpenSettings: @escaping () -> Void,
        onGuestSignIn: (() -> Void)? = nil
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onGuestSignIn = onGuestSignIn
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

    private var shouldCollapseIdentityLabel: Bool {
        horizontalSizeClass == .compact
    }

    // Balanced preset:
    // - compact (phone): mini menu + tighter chip
    // - regular (tablet/desktop): identity visible + direct actions
    private var useMiniMode: Bool {
        TopBarPreset.useMiniMode(horizontalSizeClass: horizontalSizeClass)
    }

    private var sessionChipHorizontalPadding: CGFloat {
        TopBarPreset.sessionChipHorizontalPadding(useMiniMode: useMiniMode)
    }

    private var sessionChipVerticalPadding: CGFloat {
        TopBarPreset.sessionChipVerticalPadding(useMiniMode: useMiniMode)
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

                    if !shouldCollapseIdentityLabel {
                        Text(displayName)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .lineLimit(1)

                        Image(systemName: "chevron.down")
                            .font(.caption.weight(.bold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.88))
                    }
                }
            }
            .contentShape(Capsule())
            .padding(.horizontal, sessionChipHorizontalPadding)
            .padding(.vertical, sessionChipVerticalPadding)
            .skydownCapsuleSurface(
                colorScheme: colorScheme,
                accent: sessionAccent
            )
            .buttonStyle(.plain)
            .skydownTactileAction()
            .accessibilityIdentifier("app.toolbar.session")
            .accessibilityLabel(authManager.userSession == nil ? "Gastmenü" : "Profilmenü")
            .accessibilityHint(authManager.userSession == nil ? "Öffnet Einstellungen" : "Öffnet Profil oder Einstellungen")

            if authManager.userSession == nil, let onGuestSignIn, !useMiniMode {
                Button(action: onGuestSignIn) {
                    Text(AppLocalized.text("auth.session.sign_in", fallback: "Sign in"))
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.72))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("app.toolbar.guest_sign_in")
            }

            if useMiniMode {
                Menu {
                    if let onOpenCart {
                        Button(action: onOpenCart) {
                            Label("Warenkorb", systemImage: "bag.fill")
                        }
                    }
                    if authManager.userSession != nil {
                        Button(action: onOpenProfile ?? onOpenSettings) {
                            Label("Profil", systemImage: "person.crop.circle")
                        }
                    }
                    if authManager.userSession == nil, let onGuestSignIn {
                        Button(action: onGuestSignIn) {
                            Label(AppLocalized.text("auth.session.sign_in", fallback: "Sign in"), systemImage: "person.badge.plus")
                        }
                    }
                    Button(action: onOpenSettings) {
                        Label("Einstellungen", systemImage: "gearshape.fill")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle.fill")
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .padding(8)
                        .skydownCapsuleSurface(
                            colorScheme: colorScheme,
                            accent: AppColors.accentMystic(for: colorScheme)
                        )
                }
                .frame(minWidth: 40, minHeight: 40)
                .contentShape(Rectangle())
                .accessibilityIdentifier("app.toolbar.more")
                .skydownTactileAction()
            } else if let onOpenCart {
                SessionToolbarIconButton(
                    systemName: "bag.fill",
                    accessibilityID: "app.open_cart",
                    accent: AppColors.accentHighlight(for: colorScheme),
                    colorScheme: colorScheme,
                    action: onOpenCart
                )
            }

            if !useMiniMode {
                SessionToolbarIconButton(
                    systemName: "gearshape.fill",
                    accessibilityID: "app.open_settings",
                    accent: AppColors.accentMystic(for: colorScheme),
                    colorScheme: colorScheme,
                    action: onOpenSettings
                )
            }
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
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    private var iconPadding: CGFloat {
        TopBarPreset.actionIconPadding(horizontalSizeClass: horizontalSizeClass)
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .padding(iconPadding)
                .skydownCapsuleSurface(
                    colorScheme: colorScheme,
                    accent: accent
                )
        }
        .frame(minWidth: 40, minHeight: 40)
        .contentShape(Rectangle())
        .accessibilityIdentifier(accessibilityID)
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private struct ZweizweiTabView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared
    @State private var destination: ZweizweiDestination = .hub
    @State private var catalogInitialArtist: String?
    @State private var catalogAutoPresentArtistPage = false
    @State private var highlightedSocialArtist = "JANNO"
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onGuestSignIn: (() -> Void)?

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
                        let isShortHubHeight = !layout.prefersDesktopChrome && proxy.size.height < 760
                        let sectionSpacing = isShortHubHeight ? max(layout.sectionSpacing - 4, 8) : layout.sectionSpacing
                        let topPadding = isShortHubHeight ? 10.0 : SkydownLayout.screenTopPadding
                        let bottomPadding = isShortHubHeight ? 14.0 : SkydownLayout.screenBottomPadding

                        ScrollView(.vertical, showsIndicators: true) {
                            LazyVStack(alignment: .leading, spacing: sectionSpacing) {
                                BrandHeroSurface(
                                    colorScheme: colorScheme,
                                    eyebrow: screenHeaderSettingsStore.settings.resolvedMusicHubEyebrow ?? "SkyOS",
                                    title: screenHeaderSettingsStore.settings.resolvedMusicHubTitle ?? "Music",
                                    subtitle: screenHeaderSettingsStore.settings.resolvedMusicHubSubtitle ?? "Ein Hub · drei Wege.",
                                    detail: screenHeaderSettingsStore.settings.resolvedMusicHubDetail ?? "Katalog, Releases, Studio.",
                                    backgroundImageURL: screenHeaderSettingsStore.settings.resolvedMusicHubImageURL,
                                    accent: AppColors.spotify(for: colorScheme),
                                    secondaryAccent: AppColors.accent(for: colorScheme),
                                    marks: [.zweizwei],
                                    onSurfaceTap: {
                                        catalogInitialArtist = "JANNO"
                                        catalogAutoPresentArtistPage = false
                                        withAnimation(SkydownMotion.screenTransition) {
                                            destination = .catalog
                                        }
                                    }
                                ) {
                                    HStack(spacing: 10) {
                                        BrandHeroPill(
                                            text: "Catalog",
                                            colorScheme: colorScheme,
                                            tint: AppColors.spotify(for: colorScheme),
                                            onTap: {
                                                catalogInitialArtist = "JANNO"
                                                catalogAutoPresentArtistPage = false
                                                withAnimation(SkydownMotion.screenTransition) {
                                                    destination = .catalog
                                                }
                                            }
                                        )
                                        BrandHeroPill(
                                            text: "Studio",
                                            colorScheme: colorScheme,
                                            tint: AppColors.accentMystic(for: colorScheme),
                                            onTap: {
                                                withAnimation(SkydownMotion.screenTransition) {
                                                    destination = .nicma
                                                }
                                            }
                                        )
                                    }
                                }

                                if layout.prefersThreeColumn && !isShortHubHeight {
                                    HStack(spacing: 12) {
                                        MusicHubStatusCard(
                                            title: "Catalog",
                                            value: "Artists · Tracks · Pages",
                                            accent: AppColors.spotify(for: colorScheme)
                                        )
                                        MusicHubStatusCard(
                                            title: "Studio",
                                            value: "Record · Mix · Master",
                                            accent: AppColors.accentMystic(for: colorScheme)
                                        )
                                    }
                                }

                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Direkter Einstieg")
                                        .font(.caption.weight(.semibold))
                                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    compactMusicHubAction(
                                        title: "Catalog",
                                        systemImage: "waveform.circle.fill",
                                        accent: AppColors.spotify(for: colorScheme)
                                    ) {
                                        catalogInitialArtist = "JANNO"
                                        catalogAutoPresentArtistPage = false
                                        withAnimation(SkydownMotion.screenTransition) {
                                            destination = .catalog
                                        }
                                    }
                                    HStack(spacing: 8) {
                                        compactMusicHubAction(
                                            title: "Studio",
                                            systemImage: "sparkles",
                                            accent: AppColors.accentMystic(for: colorScheme)
                                        ) {
                                            withAnimation(SkydownMotion.screenTransition) {
                                                destination = .nicma
                                            }
                                        }
                                    }
                                    VStack(alignment: .leading, spacing: 6) {
                                        Text("Artist Links")
                                            .font(.caption2.weight(.semibold))
                                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                        VStack(alignment: .leading, spacing: 6) {
                                            ForEach(musicHubSocialDestinations, id: \.id) { destination in
                                                compactMusicHubSocialLink(destination: destination)
                                            }
                                        }
                                    }
                                }
                            }
                            .frame(maxWidth: contentWidth, alignment: .leading)
                            .padding(.horizontal, layout.horizontalPadding)
                            .padding(.top, topPadding)
                            .padding(.bottom, bottomPadding)
                            .frame(maxWidth: .infinity, alignment: .top)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                        .accessibilityIdentifier("music.hub.root")
                    }
                }
                .background(
                    AppColors.screenGradient(
                        for: colorScheme,
                        secondaryAccent: AppColors.spotify(for: colorScheme)
                    )
                    .overlay {
                        SkydownAtmosphereBackdrop(colorScheme: colorScheme)
                    }
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
                            onOpenSettings: onOpenSettings,
                            onGuestSignIn: onGuestSignIn
                        )
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
                    onArtistContextChange: { highlightedSocialArtist = $0 },
                    onOpenCart: onOpenCart,
                    onOpenProfile: onOpenProfile,
                    onOpenSettings: onOpenSettings,
                    onGuestSignIn: onGuestSignIn
                )

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
        .skydownSceneMotion(trigger: destination, axis: .horizontal, travel: 24)
        .skydownSelectionFeedback(trigger: destination)
    }

    private func compactMusicHubAction(
        title: String,
        systemImage: String,
        accent: Color,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.footnote.weight(.semibold))
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Spacer(minLength: 0)
            }
            .foregroundColor(AppColors.text(for: colorScheme))
            .padding(.horizontal, 12)
            .padding(.vertical, 11)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(AppColors.secondaryBackground(for: colorScheme))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(accent.opacity(0.35), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private var musicHubSocialDestinations: [MusicInstagramDestination] {
        [
            zweizweiInstagramDestination,
            artistInstagramDestinations["JANNO"],
            artistInstagramDestinations["Yang D. Nash"],
            artistInstagramDestinations["MAVE"],
            artistInstagramDestinations["ThaDude"],
            artistInstagramDestinations["TANGAJOE007"]
        ].compactMap { $0 }
    }

    private func musicHubSocialAccent(for destination: MusicInstagramDestination) -> Color {
        switch destination.title {
        case "22 Music":
            return AppColors.spotify(for: colorScheme)
        case "JANNO":
            return AppColors.accent(for: colorScheme)
        case "Yang D. Nash":
            return AppColors.accentHighlight(for: colorScheme)
        case "MAVE":
            return AppColors.accentMystic(for: colorScheme)
        case "ThaDude":
            return AppColors.accent(for: colorScheme)
        default:
            return AppColors.spotify(for: colorScheme)
        }
    }

    private var instagramGradientColors: [Color] {
        [
            Color(red: 0.99, green: 0.74, blue: 0.28),
            Color(red: 0.98, green: 0.36, blue: 0.45),
            Color(red: 0.74, green: 0.20, blue: 0.73),
            Color(red: 0.32, green: 0.35, blue: 0.89)
        ]
    }

    private func compactMusicHubSocialLink(destination: MusicInstagramDestination) -> some View {
        let accent = musicHubSocialAccent(for: destination)
        let isActive = destination.title == highlightedSocialArtist
        return Button {
            highlightedSocialArtist = destination.title
            guard let url = destination.url else { return }
            openURL(url)
        } label: {
            HStack(alignment: .center, spacing: 10) {
                ZStack {
                    Circle()
                        .fill(accent.opacity(isActive ? 0.34 : 0.20))
                    Image(systemName: "arrow.up.right")
                        .font(.caption.weight(.bold))
                        .foregroundColor(isActive ? .white : accent)
                }
                .frame(width: 24, height: 24)

                VStack(alignment: .leading, spacing: 2) {
                    Text(destination.title)
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(destination.handle)
                        .font(.caption2.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(1)
                }

                Spacer(minLength: 6)

                if isActive {
                    Text("Aktiv")
                        .font(.caption2.weight(.bold))
                        .foregroundColor(accent.opacity(0.96))
                        .padding(.horizontal, 7)
                        .padding(.vertical, 4)
                        .background(
                            Capsule(style: .continuous)
                                .fill(accent.opacity(0.16))
                        )
                } else {
                    Image(systemName: "chevron.right")
                        .font(.caption2.weight(.bold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.72))
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                AppColors.secondaryBackground(for: colorScheme),
                                accent.opacity(0.08),
                                instagramGradientColors[1].opacity(isActive ? 0.18 : 0.10),
                                instagramGradientColors[2].opacity(isActive ? 0.18 : 0.10)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(accent.opacity(isActive ? 0.58 : 0.32), lineWidth: isActive ? 1.4 : 1)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(
                        LinearGradient(
                            colors: instagramGradientColors.map { $0.opacity(isActive ? 0.55 : 0.28) },
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: isActive ? 1.6 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
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
                                        accent.opacity(colorScheme == .dark ? 0.14 : 0.18),
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
            .frame(maxWidth: .infinity, minHeight: 168, alignment: .leading)
            .skydownPanelSurface(
                colorScheme: colorScheme,
                accent: accent,
                cornerRadius: SkydownLayout.heroCornerRadius,
                shadowRadius: 10,
                shadowYOffset: 5
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
    let onExitImmersive: () -> Void
    let onOpenCart: () -> Void
    let onOpenLogin: () -> Void
    let onGuestSignIn: (() -> Void)?
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onOpenAutomationSettings: () -> Void
    @State private var mode: AIHubMode = .bot
    @StateObject private var membershipCoordinator = AIMembershipCoordinator()
    @State private var showsMembershipToast = false
    @State private var membershipToastMessage = ""
    @State private var membershipToastStyle: ToastStyle = .info
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var aiSubscriptionStore: NativeAISubscriptionStore
    @EnvironmentObject private var authManager: AuthManager

    init(
        aiChatService: AIChatServicing,
        agentChatService: AgentChatServicing,
        featureFlags: FeatureFlagsService,
        showsWorkflowWorkspace: Binding<Bool>,
        onExitImmersive: @escaping () -> Void,
        onOpenCart: @escaping () -> Void,
        onOpenLogin: @escaping () -> Void,
        onGuestSignIn: (() -> Void)?,
        onOpenProfile: @escaping () -> Void,
        onOpenSettings: @escaping () -> Void,
        onOpenAutomationSettings: @escaping () -> Void
    ) {
        self.aiChatService = aiChatService
        self.agentChatService = agentChatService
        self.onExitImmersive = onExitImmersive
        self.onOpenCart = onOpenCart
        self.onOpenLogin = onOpenLogin
        self.onGuestSignIn = onGuestSignIn
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

    private var membershipObservationKey: String {
        let session = authManager.userSession
        return [
            session?.id ?? "guest",
            session?.resolvedQuotaPlan.rawValue ?? UserQuotaPlan.free.rawValue,
            String(session?.resolvedAIHistoryRetentionDays ?? UserRole.user.defaultAIHistoryRetentionDays),
            session?.normalizedAISubscriptionProvider ?? "none",
            String(session?.aiAccessEnabled ?? false),
            mode.rawValue
        ].joined(separator: "|")
    }

    private var currentUserQuotaPlan: UserQuotaPlan? {
        authManager.userSession?.resolvedQuotaPlan
    }

    var body: some View {
        NavigationStack {
            GeometryReader { geometry in
                let layout = SkydownResponsiveLayout(availableWidth: geometry.size.width)
                let contentWidth = min(
                    layout.contentMaxWidth,
                    max(geometry.size.width - (layout.horizontalPadding * 2), 0)
                )
                let aiLoginTitle = featureFlags.aiAccessMode == .adminOnly
                    ? AppLocalized.text("auth.ai.login.title_staff", fallback: "Staff access")
                    : AppLocalized.text("auth.ai.login.title", fallback: "Keep your AI momentum")
                let aiLoginMessage = featureFlags.aiAccessMode == .signedIn
                    ? AppLocalized.text("auth.ai.login.hint_signed_in", fallback: "Save workflows and return to the same thread without losing context.")
                    : featureFlags.aiAccessMessage(for: nil)

                VStack(spacing: 10) {
                    if authManager.userSession == nil {
                        AIHubLoginCard(
                            colorScheme: colorScheme,
                            title: aiLoginTitle,
                            message: aiLoginMessage,
                            ctaTitle: AppLocalized.text("auth.continue_with_account", fallback: "Continue with account"),
                            onOpenLogin: onOpenLogin
                        )
                        .frame(maxWidth: .infinity)
                    } else if !featureFlags.allowsAIAccess(for: authManager.userSession) {
                        AIHubRestrictedCard(
                            colorScheme: colorScheme,
                            message: featureFlags.aiAccessMessage(for: authManager.userSession),
                            onOpenSettings: onOpenSettings
                        )
                        .frame(maxWidth: .infinity)
                    } else {
                        AIHubCompactHeader(
                            mode: mode,
                            colorScheme: colorScheme,
                            showsWorkflowWorkspace: showsWorkflowWorkspace,
                            onSelectMode: { newMode in
                                withAnimation(SkydownMotion.screenTransition) {
                                    membershipCoordinator.closeMembership()
                                    showsWorkflowWorkspace = false
                                    mode = newMode
                                }
                            },
                            onToggleWorkflow: {
                                withAnimation(SkydownMotion.screenTransition) {
                                    membershipCoordinator.closeMembership()
                                    showsWorkflowWorkspace.toggle()
                                }
                            }
                        )
                        .frame(maxWidth: .infinity)
                        .padding(.top, 8)

                        Text(
                            AppLocalized.text(
                                "ai.legal_disclosure_short",
                                fallback: "AI can be wrong. Full policy: Settings → Privacy / Legal / Help → AI usage notice."
                            )
                        )
                        .font(.caption2.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 4)
                        .accessibilityLabel(
                            AppLocalized.text("ai.legal_disclosure_short", fallback: "AI can be wrong. Full policy: Settings → Privacy / Legal / Help → AI usage notice.")
                        )

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
                                .frame(maxWidth: .infinity, alignment: .leading)
                            } else if mode == .bot {
                                AIView(
                                    aiChatService: aiChatService,
                                    featureFlags: featureFlags,
                                    membershipCoordinator: membershipCoordinator,
                                    showsNavigation: false
                                )
                            } else {
                                AgentView(
                                    agentChatService: agentChatService,
                                    featureFlags: featureFlags,
                                    membershipCoordinator: membershipCoordinator,
                                    showsNavigation: false
                                )
                            }
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                        .skydownSceneMotion(trigger: motionState, axis: .horizontal, travel: 22)
                    }
                }
                .frame(maxWidth: contentWidth, maxHeight: .infinity, alignment: .top)
                .padding(.horizontal, layout.horizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding * 0.5)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            }
            .background(
                AppColors.screenGradient(
                    for: colorScheme,
                    secondaryAccent: AppColors.accentMystic(for: colorScheme)
                )
                .ignoresSafeArea()
            )
            .fancyToast(
                isPresented: $showsMembershipToast,
                message: membershipToastMessage,
                style: membershipToastStyle
            )
            .navigationTitle("AI")
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .task(id: membershipObservationKey) {
                membershipCoordinator.cacheCurrentPlan(currentUserQuotaPlan)
                await aiSubscriptionStore.prepareStorefront(for: authManager.userSession)
            }
            .sheet(
                isPresented: Binding(
                    get: { membershipCoordinator.isPresented },
                    set: { if !$0 { membershipCoordinator.closeMembership() } }
                )
            ) {
                membershipSheet
            }
            .onChange(of: authManager.userSession?.id) { _, userID in
                if userID == nil {
                    membershipCoordinator.closeMembership()
                }
            }
            .onChange(of: showsWorkflowWorkspace) { _, isVisible in
                if isVisible {
                    membershipCoordinator.closeMembership()
                }
            }
            .onChange(of: featureFlags.allowsAIAccess(for: authManager.userSession)) { _, allowed in
                if !allowed {
                    membershipCoordinator.closeMembership()
                }
            }
            .toolbar {
                if !SkydownPlatform.isDesktop {
                    ToolbarItem(placement: .topBarLeading) {
                        Button(action: onExitImmersive) {
                            Label("Zurueck", systemImage: "chevron.backward")
                                .labelStyle(.titleAndIcon)
                        }
                        .accessibilityIdentifier("ai.hub.exit")
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    AppSessionToolbarActions(
                        onOpenCart: onOpenCart,
                        onOpenProfile: onOpenProfile,
                        onOpenSettings: onOpenSettings,
                        onGuestSignIn: onGuestSignIn
                    )
                }
            }
        }
    }

    @ViewBuilder
    private var membershipSheet: some View {
        if mode == .bot {
            AIMembershipSheet(
                colorScheme: colorScheme,
                isLoadingProducts: aiSubscriptionStore.isLoadingProducts,
                isSyncing: aiSubscriptionStore.isSyncing,
                activePurchasePlan: aiSubscriptionStore.activePurchasePlan,
                onSelectPlan: { plan in
                    Task { await purchaseMembership(plan: plan) }
                },
                onRestore: {
                    Task { await restoreMembership() }
                },
                onManage: {
                    Task { await manageMembership() }
                }
            )
            .presentationDetents([.medium, .large])
        } else {
            AgentMembershipSheet(
                colorScheme: colorScheme,
                isLoadingProducts: aiSubscriptionStore.isLoadingProducts,
                isSyncing: aiSubscriptionStore.isSyncing,
                activePurchasePlan: aiSubscriptionStore.activePurchasePlan,
                onSelectPlan: { plan in
                    Task { await purchaseMembership(plan: plan) }
                },
                onRestore: {
                    Task { await restoreMembership() }
                },
                onManage: {
                    Task { await manageMembership() }
                }
            )
            .presentationDetents([.medium, .large])
        }
    }

    private func purchaseMembership(plan: UserQuotaPlan) async {
        membershipCoordinator.track("plan_selected", ["plan": plan.rawValue])
        membershipCoordinator.track("purchase_started", ["plan": plan.rawValue])
        if mode == .bot {
            membershipCoordinator.track("purchase_started_pipeline", ["surface": membershipCoordinator.surface])
        }
        MembershipAnalyticsTracker().track(
            "plan_selected",
            reason: membershipCoordinator.lastOpenReason.rawValue,
            plan: plan.rawValue,
            surface: membershipCoordinator.surface,
            currentPlan: membershipCoordinator.currentPlanCache.rawValue
        )
        MembershipAnalyticsTracker().track(
            "purchase_started",
            reason: membershipCoordinator.lastOpenReason.rawValue,
            plan: plan.rawValue,
            surface: membershipCoordinator.surface,
            currentPlan: membershipCoordinator.currentPlanCache.rawValue
        )

        do {
            let outcome = try await aiSubscriptionStore.purchase(plan: plan)
            switch outcome {
            case .success:
                membershipCoordinator.track("purchase_success", ["plan": plan.rawValue])
                MembershipAnalyticsTracker().track(
                    "purchase_success",
                    reason: membershipCoordinator.lastOpenReason.rawValue,
                    plan: plan.rawValue,
                    surface: membershipCoordinator.surface,
                    currentPlan: membershipCoordinator.currentPlanCache.rawValue
                )
                await membershipCoordinator.postPurchaseRefresh(plan: plan) {
                    _ = await authManager.refreshCurrentUser()
                }
                showMembershipToast(
                    mode == .bot
                        ? "Upgrade erfolgreich aktiviert."
                        : AppLocalized.text("membership.upgrade.success", fallback: "Upgrade activated successfully."),
                    style: .success
                )
            case .pending:
                showMembershipToast(
                    mode == .bot
                        ? "Kauf wartet auf App-Store-Freigabe."
                        : AppLocalized.text("membership.purchase.pending", fallback: "Purchase is pending App Store approval."),
                    style: .info
                )
            case .cancelled:
                membershipCoordinator.track("purchase_cancelled", ["plan": plan.rawValue])
                MembershipAnalyticsTracker().track(
                    "purchase_cancelled",
                    reason: membershipCoordinator.lastOpenReason.rawValue,
                    plan: plan.rawValue,
                    surface: membershipCoordinator.surface,
                    currentPlan: membershipCoordinator.currentPlanCache.rawValue
                )
                showMembershipToast(
                    mode == .bot
                        ? "Kauf abgebrochen."
                        : AppLocalized.text("membership.purchase.cancelled", fallback: "Purchase cancelled."),
                    style: .info
                )
            }
        } catch {
            showMembershipToast(
                mode == .bot
                    ? "Upgrade konnte nicht gestartet werden: \(error.localizedDescription)"
                    : "\(AppLocalized.text("membership.upgrade.failed", fallback: "Could not start upgrade")): \(error.localizedDescription)",
                style: .error
            )
        }
    }

    private func restoreMembership() async {
        do {
            let shouldForceEmptySync = authManager.userSession?.normalizedAISubscriptionProvider == "app_store"
            try await aiSubscriptionStore.restorePurchases(forceEmptySync: shouldForceEmptySync)
            await membershipCoordinator.restore {
                _ = await authManager.refreshCurrentUser()
            }
            showMembershipToast(
                mode == .bot
                    ? "App-Store-Kaeufe synchronisiert."
                    : AppLocalized.text("membership.restore.success", fallback: "App Store purchases synced."),
                style: .success
            )
        } catch {
            showMembershipToast(
                mode == .bot
                    ? "Synchronisierung fehlgeschlagen: \(error.localizedDescription)"
                    : "\(AppLocalized.text("membership.restore.failed", fallback: "Sync failed")): \(error.localizedDescription)",
                style: .error
            )
        }
    }

    private func manageMembership() async {
        do {
            try await aiSubscriptionStore.manageSubscriptions()
        } catch {
            showMembershipToast(
                mode == .bot
                    ? "Abo-Verwaltung konnte nicht geoeffnet werden."
                    : AppLocalized.text("membership.manage.failed", fallback: "Could not open subscription management."),
                style: .error
            )
        }
    }

    private func showMembershipToast(_ message: String, style: ToastStyle) {
        membershipToastMessage = message
        membershipToastStyle = style
        showsMembershipToast = true
    }
}

private struct VideoHubTabView: View {
    let onOpenCart: () -> Void
    let onOpenProfile: () -> Void
    let onOpenSettings: () -> Void
    let onGuestSignIn: (() -> Void)?

    var body: some View {
        NavigationStack {
            VideoHubView()
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        AppSessionToolbarActions(
                            onOpenCart: onOpenCart,
                            onOpenProfile: onOpenProfile,
                            onOpenSettings: onOpenSettings,
                            onGuestSignIn: onGuestSignIn
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
    let ctaTitle: String
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
                Text(ctaTitle)
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
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 7) {
                modeSelectionStrip
                workflowToggleButton
                    .fixedSize(horizontal: true, vertical: false)
            }

            VStack(spacing: 7) {
                modeSelectionStrip
                workflowToggleButton
                    .frame(maxWidth: .infinity)
            }
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

    private var modeSelectionStrip: some View {
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
                .accessibilityIdentifier("tools.mode.\(currentMode.rawValue.lowercased())")
            }
        }
    }

    private var workflowToggleButton: some View {
        Button(action: onToggleWorkflow) {
            HStack(spacing: 6) {
                Image(systemName: showsWorkflowWorkspace ? "xmark.circle.fill" : "bolt.horizontal.circle.fill")
                    .font(.headline)
                Text(showsWorkflowWorkspace ? "Zur AI" : "Automation")
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
}

private struct AIWorkflowWorkspaceCard: View {
    let colorScheme: ColorScheme
    let onOpenSettings: () -> Void
    let onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Automation")
                .font(.title2.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Verbinde Agent, Kontext und Aktionen so, dass der naechste Schritt klar bleibt.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(alignment: .leading, spacing: 10) {
                AIWorkflowStepRow(index: "01", title: "Briefing", detail: "Der Agent macht aus einer Idee einen klaren Plan.", colorScheme: colorScheme)
                AIWorkflowStepRow(index: "02", title: "Aktion", detail: "Optional wird dein Automationsservice mit Nutzerkontext gestartet.", colorScheme: colorScheme)
                AIWorkflowStepRow(index: "03", title: "Rueckweg", detail: "Du bleibst in der App und kannst direkt weiterarbeiten.", colorScheme: colorScheme)
            }

            HStack(spacing: 10) {
                AIHubBadge(text: "n8n", color: AppColors.accentHighlight(for: colorScheme))
                AIHubBadge(text: "Agent", color: AppColors.accent(for: colorScheme))
                AIHubBadge(text: "Kontext", color: AppColors.accentMystic(for: colorScheme))
            }

            Button(action: onOpenSettings) {
                Text("Automation einrichten")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.accentHighlight(for: colorScheme))

            Button(action: onClose) {
                Text("Zur AI zurueck")
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
