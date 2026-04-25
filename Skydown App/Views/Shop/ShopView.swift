//
//  ShopView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

// swiftlint:disable file_length

import SwiftUI

// Home view moved to Views/Home/*.

struct ShopView: View {
    @ObservedObject private var authManager: AuthManager
    @StateObject private var viewModel: MerchandiseViewModel
    private let onOpenLogin: () -> Void
    private let onOpenCart: () -> Void
    private let onOpenProfile: () -> Void
    private let onOpenSettings: () -> Void
    private let onGuestSignIn: (() -> Void)?
    @State private var selectedItem: MerchandiseItem?
    @State private var selectedCollabLaneID = MerchandiseCollabLane.allID
    @State private var openedCollectionLane: MerchandiseCollabLane?
    @Environment(\.colorScheme) private var colorScheme

    init(
        authManager: AuthManager,
        onOpenLogin: @escaping () -> Void = {},
        onOpenCart: @escaping () -> Void = {},
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        onGuestSignIn: (() -> Void)? = nil,
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService()
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        self.onOpenLogin = onOpenLogin
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onGuestSignIn = onGuestSignIn
        _viewModel = StateObject(
            wrappedValue: MerchandiseViewModel(
                merchandiseService: merchandiseService,
                authManager: authManager
            )
        )
    }

    private var isAdmin: Bool {
        authManager.userSession?.isPlatformOwner == true
    }

    private var merchCollabLanes: [MerchandiseCollabLane] {
        MerchandiseCollabLane.build(from: viewModel.merchandiseItems)
    }

    private var selectedCollabLane: MerchandiseCollabLane {
        merchCollabLanes.first { $0.id == selectedCollabLaneID }
            ?? merchCollabLanes.first
            ?? MerchandiseCollabLane(
                id: MerchandiseCollabLane.allID,
                title: "All Drops",
                subtitle: "Alle Collabs und Core Pieces.",
                itemCount: viewModel.merchandiseItems.count,
                isCoreLane: false
            )
    }

    private var filteredMerchandiseItems: [MerchandiseItem] {
        guard selectedCollabLaneID != MerchandiseCollabLane.allID else {
            return viewModel.merchandiseItems
        }

        return viewModel.merchandiseItems.filter { item in
            item.belongsToLane(id: selectedCollabLaneID)
        }
    }

    private var merchLaneCount: Int {
        let resolvedCount = merchCollabLanes.filter { $0.id != MerchandiseCollabLane.allID }.count
        if resolvedCount == 0 {
            return viewModel.merchandiseItems.isEmpty ? 0 : 1
        }
        return resolvedCount
    }

    private var featuredDropItem: MerchandiseItem? {
        viewModel.merchandiseItems.first(where: { $0.featured && $0.available })
        ?? viewModel.merchandiseItems.first(where: { $0.available })
        ?? viewModel.merchandiseItems.first
    }

    private var editorialPickItems: [MerchandiseItem] {
        let sorted = viewModel.merchandiseItems.sorted { lhs, rhs in
            if lhs.featured != rhs.featured {
                return lhs.featured && !rhs.featured
            }
            if lhs.sortOrder != rhs.sortOrder {
                return lhs.sortOrder < rhs.sortOrder
            }
            return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
        }
        return Array(sorted.prefix(5))
    }

    private func shopLaneTitle(_ lane: MerchandiseCollabLane) -> String {
        if lane.id == MerchandiseCollabLane.allID {
            return AppLocalized.text("shop.lane.all_drops_title", fallback: "All pieces")
        }
        return lane.title
    }

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                let layout = SkydownResponsiveLayout(availableWidth: proxy.size.width)
                let contentWidth = min(
                    layout.contentMaxWidth,
                    max(proxy.size.width - (layout.horizontalPadding * 2), 0)
                )
                let sidebarWidth = layout.prefersDesktopChrome
                    ? min(max(contentWidth * 0.26, 260), 320)
                    : min(max(contentWidth * 0.27, 220), 272)
                let laneSignature = merchCollabLanes.map(\.id).joined(separator: "|")

                Group {
                    if viewModel.isLoading {
                        ProgressView {
                            Text(AppLocalized.text("shop.loading", fallback: "Opening the shop…"))
                        }
                        .tint(AppColors.accent(for: colorScheme))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        ScrollView {
                            VStack(alignment: .leading, spacing: layout.sectionSpacing) {
                                ShopHeroCard(
                                    colorScheme: colorScheme,
                                    itemCount: viewModel.merchandiseItems.count,
                                    laneCount: merchLaneCount,
                                    isStoreOpen: viewModel.isStoreOpen,
                                    isLoggedIn: authManager.userSession != nil,
                                    isUpdatingStoreState: viewModel.isUpdatingStoreState,
                                    isSyncingCatalog: viewModel.isSyncingCatalog,
                                    onSurfaceTap: onOpenCart
                                )

                                ShopWelcomeQuickEntryCard(
                                    colorScheme: colorScheme,
                                    lanes: merchCollabLanes,
                                    selectedLaneID: selectedCollabLaneID
                                ) { lane in
                                    selectedCollabLaneID = lane.id
                                } onOpenLane: { lane in
                                    selectedCollabLaneID = lane.id
                                    openedCollectionLane = lane
                                }

                                if !viewModel.merchandiseItems.isEmpty {
                                    ShopMerchOpeningBlock(
                                        showFeatured: !editorialPickItems.isEmpty,
                                        colorScheme: colorScheme,
                                        featuredItem: featuredDropItem,
                                        editorialPicks: editorialPickItems,
                                        onOpenItem: { item in
                                            selectedItem = item
                                        }
                                    )
                                }

                                if isAdmin {
                                    ShopAdminControlsPanel(
                                        colorScheme: colorScheme,
                                        isUpdatingStoreState: viewModel.isUpdatingStoreState,
                                        isStoreOpen: viewModel.isStoreOpen,
                                        isSyncingCatalog: viewModel.isSyncingCatalog,
                                        onToggleStore: {
                                            Task { await viewModel.toggleStoreOpen() }
                                        },
                                        onSyncShopify: {
                                            Task { await viewModel.syncShopifyCatalog() }
                                        }
                                    )
                                }

                                if let errorMessage = viewModel.errorMessage {
                                    if authManager.userSession == nil, viewModel.merchandiseItems.isEmpty {
                                        let benefit = AppLocalized.text(
                                            "auth.merch.login.banner_subtitle",
                                            fallback: "Follow shipments and keep purchase history in one calm view."
                                        )
                                        ShopInfoCard(
                                            colorScheme: colorScheme,
                                            title: AppLocalized.text("auth.merch.login.banner_title", fallback: "Orders & account"),
                                            message: [errorMessage, benefit].joined(separator: "\n\n"),
                                            actionTitle: AppLocalized.text("auth.merch.login.cta", fallback: "Continue with account"),
                                            action: onOpenLogin
                                        )
                                    } else {
                                        ShopInfoCard(
                                            colorScheme: colorScheme,
                                            title: AppLocalized.text("shop.error.title", fallback: "Store unavailable"),
                                            message: errorMessage,
                                            actionTitle: AppLocalized.text("shop.error.reload", fallback: "Try again"),
                                            action: {
                                                viewModel.fetchData()
                                            }
                                        )
                                    }
                                }

                                if !viewModel.isStoreOpen && !isAdmin {
                                    ShopInfoCard(
                                        colorScheme: colorScheme,
                                        title: AppLocalized.text("shop.paused.title", fallback: "Checkout is paused"),
                                        message: AppLocalized.text("shop.paused.body", fallback: "You can still explore products.")
                                    )
                                }

                                if viewModel.merchandiseItems.isEmpty, viewModel.errorMessage == nil {
                                    ShopInfoCard(
                                        colorScheme: colorScheme,
                                        title: viewModel.isSyncingCatalog
                                            ? AppLocalized.text("shop.empty.title.loading", fallback: "Opening the shelf")
                                            : AppLocalized.text("shop.empty.title.idle", fallback: "The shelf is quiet"),
                                        message: isAdmin
                                            ? (viewModel.isSyncingCatalog
                                                ? AppLocalized.text("shop.empty.body.loading.admin", fallback: "Updating catalog.")
                                                : AppLocalized.text("shop.empty.body.idle.guest", fallback: "When new pieces go live, they show up here."))
                                            : (viewModel.isSyncingCatalog
                                                ? AppLocalized.text("shop.empty.body.loading.guest", fallback: "We are loading products.")
                                                : AppLocalized.text("shop.empty.body.idle.guest", fallback: "When new pieces go live, they show up here."))
                                    )
                                } else if layout.prefersTwoColumn {
                                    HStack(alignment: .top, spacing: layout.sectionSpacing) {
                                        MerchandiseCollabSidebar(
                                            lanes: merchCollabLanes,
                                            selectedLaneID: selectedCollabLaneID,
                                            colorScheme: colorScheme
                                        ) { lane in
                                            selectedCollabLaneID = lane.id
                                        }
                                        .frame(width: sidebarWidth)

                                        VStack(alignment: .leading, spacing: layout.sectionSpacing) {
                                            MerchandiseCollabSelectionCard(
                                                selectedLane: selectedCollabLane,
                                                totalItemCount: viewModel.merchandiseItems.count,
                                                colorScheme: colorScheme
                                            )

                                            if filteredMerchandiseItems.isEmpty {
                                                ShopInfoCard(
                                                    colorScheme: colorScheme,
                                                    title: AppLocalized.text("shop.filter.empty.title", fallback: "Nothing in this view"),
                                                    message: String(
                                                        format: AppLocalized.text(
                                                            "shop.filter.empty.body",
                                                            fallback: "There is no visible product in “%@” right now."
                                                        ),
                                                        shopLaneTitle(selectedCollabLane)
                                                    ),
                                                    actionTitle: selectedCollabLaneID == MerchandiseCollabLane.allID
                                                        ? nil
                                                        : AppLocalized.text("shop.lane.all_drops_title", fallback: "All pieces"),
                                                    action: selectedCollabLaneID == MerchandiseCollabLane.allID ? nil : {
                                                        selectedCollabLaneID = MerchandiseCollabLane.allID
                                                    }
                                                )
                                            } else {
                                                LazyVStack(alignment: .leading, spacing: 9) {
                                                    ForEach(0..<filteredMerchandiseItems.count, id: \.self) { index in
                                                        let item = filteredMerchandiseItems[index]
                                                        MerchandiseRowView(
                                                            item: item,
                                                            environmentColorScheme: colorScheme,
                                                            shelfSpotlight: index < 2,
                                                            shelfSettled: index > 2
                                                        ) {
                                                            selectedItem = $0
                                                        }
                                                        .padding(.top, index == 2 ? 18 : 0)
                                                    }
                                                }
                                            }
                                        }
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                    }
                                } else {
                                    VStack(alignment: .leading, spacing: layout.sectionSpacing) {
                                        MerchandiseCollabCarousel(
                                            lanes: merchCollabLanes,
                                            selectedLaneID: $selectedCollabLaneID,
                                            totalItemCount: viewModel.merchandiseItems.count,
                                            colorScheme: colorScheme
                                        )

                                        if merchCollabLanes.count > 1 {
                                            MerchandiseCollabQuickGrid(
                                                lanes: merchCollabLanes,
                                                selectedLaneID: selectedCollabLaneID,
                                                colorScheme: colorScheme
                                            ) { lane in
                                                selectedCollabLaneID = lane.id
                                            }
                                        }
                                    }

                                    if filteredMerchandiseItems.isEmpty {
                                        ShopInfoCard(
                                            colorScheme: colorScheme,
                                            title: AppLocalized.text("shop.filter.empty.title", fallback: "Nothing in this view"),
                                            message: String(
                                                format: AppLocalized.text(
                                                    "shop.filter.empty.body",
                                                    fallback: "There is no visible product in “%@” right now."
                                                ),
                                                shopLaneTitle(selectedCollabLane)
                                            ),
                                            actionTitle: selectedCollabLaneID == MerchandiseCollabLane.allID
                                                ? nil
                                                : AppLocalized.text("shop.lane.all_drops_title", fallback: "All pieces"),
                                            action: selectedCollabLaneID == MerchandiseCollabLane.allID ? nil : {
                                                selectedCollabLaneID = MerchandiseCollabLane.allID
                                            }
                                        )
                                    } else {
                                        LazyVStack(alignment: .leading, spacing: 9) {
                                            ForEach(0..<filteredMerchandiseItems.count, id: \.self) { index in
                                                let item = filteredMerchandiseItems[index]
                                                MerchandiseRowView(
                                                    item: item,
                                                    environmentColorScheme: colorScheme,
                                                    shelfSpotlight: index < 2,
                                                    shelfSettled: index > 2
                                                ) {
                                                    selectedItem = $0
                                                }
                                                .padding(.top, index == 2 ? 18 : 0)
                                            }
                                        }
                                    }
                                }
                            }
                            .frame(maxWidth: contentWidth, alignment: .leading)
                            .padding(.horizontal, layout.horizontalPadding)
                            .padding(.top, SkydownLayout.screenTopPadding)
                            .padding(.bottom, SkydownLayout.screenBottomPadding)
                            .frame(maxWidth: .infinity, alignment: .top)
                        }
                        .accessibilityIdentifier("shop.root")
                        .scrollIndicators(.hidden)
                        .refreshable {
                            viewModel.fetchData()
                        }
                    }
                }
                .background(
                    AppColors.screenGradient(
                        for: colorScheme,
                        secondaryAccent: AppColors.accentMystic(for: colorScheme)
                    )
                    .overlay {
                        SkydownAtmosphereBackdrop(colorScheme: colorScheme)
                    }
                    .ignoresSafeArea()
                )
                .navigationBarTitleDisplayMode(.inline)
                .skydownNavigationChrome(colorScheme: colorScheme)
                .toolbar {
                    ToolbarItem(placement: .principal) {
                        VStack(spacing: 1) {
                            Text("Shop")
                                .font(.headline)
                            Text(AppLocalized.text("shop.topbar.subtitle", fallback: "Curated drops & collections"))
                                .font(.caption2)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        HStack(spacing: 10) {
                            AppSessionToolbarActions(
                                onOpenCart: onOpenCart,
                                onOpenProfile: onOpenProfile,
                                onOpenSettings: onOpenSettings,
                                onGuestSignIn: onGuestSignIn
                            )
                        }
                    }
                }
                .task {
                    if viewModel.merchandiseItems.isEmpty {
                        viewModel.fetchData()
                    }
                }
                .onChange(of: laneSignature) { _, _ in
                    if !merchCollabLanes.contains(where: { $0.id == selectedCollabLaneID }) {
                        selectedCollabLaneID = MerchandiseCollabLane.allID
                    }
                }
                .sheet(item: $selectedItem) { item in
                    NavigationStack {
                        MerchandiseDetailView(
                            item: item,
                            storeIsOpen: viewModel.isStoreOpen || isAdmin,
                            onOpenCart: onOpenCart
                        )
                            .background(AppColors.primaryBackground(for: colorScheme))
                    }
                }
                .sheet(item: $openedCollectionLane) { lane in
                    ShopCollectionLanePage(
                        lane: lane,
                        allItems: viewModel.merchandiseItems,
                        colorScheme: colorScheme,
                        onOpenItem: { item in
                            selectedItem = item
                        },
                        onDismiss: {
                            openedCollectionLane = nil
                        }
                    )
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
    }
}

// Home components moved to Views/Home/*.

// Remaining Home media components moved to Views/Home/HomeMediaCluster.swift.

private struct ShopHeroCard: View {
    let colorScheme: ColorScheme
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared
    let itemCount: Int
    let laneCount: Int
    let isStoreOpen: Bool
    let isLoggedIn: Bool
    let isUpdatingStoreState: Bool
    let isSyncingCatalog: Bool
    let onSurfaceTap: () -> Void

    private var computedDetail: String {
        if isSyncingCatalog {
            return AppLocalized.text("shop.hero.detail.updating", fallback: "Refreshing the catalog.")
        }
        return isStoreOpen
            ? AppLocalized.text("shop.hero.detail.checkout_on", fallback: "You can finish purchases from the cart when you are ready.")
            : AppLocalized.text("shop.hero.detail.checkout_off", fallback: "Browsing stays on — checkout is paused for now.")
    }

    private var heroDetail: String? {
        let r = screenHeaderSettingsStore.settings.resolvedShopDetail
        if let r, !r.isEmpty { return r }
        return computedDetail
    }

    var body: some View {
        BrandHeroSurface(
            colorScheme: colorScheme,
            eyebrow: screenHeaderSettingsStore.settings.resolvedShopEyebrow ?? "SKY OS",
            title: screenHeaderSettingsStore.settings.resolvedShopTitle ?? "Merch",
            subtitle: screenHeaderSettingsStore.settings.resolvedShopSubtitle
                ?? AppLocalized.text("shop.hero.subtitle.fallback", fallback: "Drops, collabs, and the live catalog."),
            detail: heroDetail,
            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedShopImageURL,
            accent: AppColors.accentHighlight(for: colorScheme),
            secondaryAccent: AppColors.accentMystic(for: colorScheme),
            marks: [],
            onSurfaceTap: onSurfaceTap
        ) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ShopBadge(
                        text: (isSyncingCatalog || isUpdatingStoreState)
                            ? AppLocalized.text("shop.hero.pill.updating", fallback: "Updating")
                            : (itemCount == 1
                                ? AppLocalized.text("shop.hero.pill.product_one", fallback: "1 product")
                                : String(
                                    format: AppLocalized.text("shop.hero.pill.product_other", fallback: "%d products"),
                                    itemCount
                                )),
                        colorScheme: colorScheme
                    )
                    ShopBadge(
                        text: isStoreOpen
                            ? AppLocalized.text("shop.hero.pill.checkout_on", fallback: "Checkout on")
                            : AppLocalized.text("shop.hero.pill.checkout_off", fallback: "Checkout paused"),
                        colorScheme: colorScheme
                    )
                    if laneCount > 0 {
                        ShopBadge(
                            text: laneCount == 1
                                ? AppLocalized.text("shop.hero.pill.collection_one", fallback: "1 collection")
                                : String(
                                    format: AppLocalized.text("shop.hero.pill.collection_other", fallback: "%d collections"),
                                    laneCount
                                ),
                            colorScheme: colorScheme
                        )
                    }
                    ShopBadge(
                        text: isLoggedIn
                            ? AppLocalized.text("shop.hero.pill.account", fallback: "Signed in")
                            : AppLocalized.text("shop.hero.pill.guest", fallback: "Browsing as guest"),
                        colorScheme: colorScheme
                    )
                }
            }

            HStack(spacing: 10) {
                ShopHeroMetricTile(
                    label: AppLocalized.text("shop.hero.metric.catalog", fallback: "Catalog"),
                    value: (isSyncingCatalog || isUpdatingStoreState)
                        ? AppLocalized.text("shop.hero.metric.value.updating", fallback: "…")
                        : "\(itemCount)",
                    colorScheme: colorScheme,
                    isActive: isSyncingCatalog || isUpdatingStoreState || itemCount > 0
                )
                ShopHeroMetricTile(
                    label: AppLocalized.text("shop.hero.metric.store", fallback: "Checkout"),
                    value: isStoreOpen
                        ? AppLocalized.text("shop.hero.metric.value.open", fallback: "Open")
                        : AppLocalized.text("shop.hero.metric.value.closed", fallback: "Paused"),
                    colorScheme: colorScheme,
                    isActive: isStoreOpen
                )
                ShopHeroMetricTile(
                    label: AppLocalized.text("shop.hero.metric.account", fallback: "Account"),
                    value: isLoggedIn
                        ? AppLocalized.text("shop.hero.metric.signed_in", fallback: "You")
                        : AppLocalized.text("shop.hero.metric.guest", fallback: "Guest"),
                    colorScheme: colorScheme,
                    isActive: isLoggedIn
                )
            }
        }
    }
}

private struct ShopWelcomeQuickEntryCard: View {
    let colorScheme: ColorScheme
    let lanes: [MerchandiseCollabLane]
    let selectedLaneID: String
    let onSelectLane: (MerchandiseCollabLane) -> Void
    let onOpenLane: (MerchandiseCollabLane) -> Void

    private var quickLanes: [MerchandiseCollabLane] {
        lanes
    }

    private var entryLanes: [MerchandiseCollabLane] {
        lanes.filter { $0.id != MerchandiseCollabLane.allID }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 8) {
                Image(systemName: "sparkles")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                VStack(alignment: .leading, spacing: 3) {
                    Text("Willkommen im Store")
                        .font(AppTypography.sectionHeadline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("Starte ueber den Katalog und waehle direkt deinen Bereich.")
                        .font(AppTypography.bodyCaption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                Spacer(minLength: 0)
                Text("Katalog")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accent(for: colorScheme).opacity(0.12))
                    )
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Direkter Einstieg")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                HomeActionButton(
                    title: "Alle Drops",
                    subtitle: "Kompletter Katalog",
                    icon: "bag.fill",
                    colorScheme: colorScheme,
                    brand: .neutral,
                    isPrimary: true
                ) {
                    if let lane = lanes.first(where: { $0.id == MerchandiseCollabLane.allID }) {
                        onOpenLane(lane)
                    }
                }

                HStack(spacing: 8) {
                    ForEach(Array(entryLanes.prefix(2)), id: \.id) { lane in
                        HomeActionButton(
                            title: lane.title,
                            subtitle: lane.subtitle,
                            icon: "square.grid.2x2.fill",
                            colorScheme: colorScheme,
                            brand: .neutral,
                            isPrimary: false
                        ) {
                            onOpenLane(lane)
                        }
                    }
                }
            }

            VStack(spacing: 8) {
                ForEach(quickLanes, id: \.id) { lane in
                    Button(action: { onOpenLane(lane) }) {
                        HStack(spacing: 8) {
                            Image(systemName: lane.id == selectedLaneID ? "checkmark.circle.fill" : "circle")
                                .font(.caption.weight(.bold))
                                .foregroundColor(AppColors.accent(for: colorScheme).opacity(lane.id == selectedLaneID ? 0.9 : 0.45))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(lane.title)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(AppColors.text(for: colorScheme))
                                    .lineLimit(1)
                                Text(lane.subtitle)
                                    .font(.caption)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    .lineLimit(1)
                            }
                            Spacer(minLength: 0)
                            Image(systemName: "chevron.right")
                                .font(.caption2.weight(.bold))
                                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.68))
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(
                                    lane.id == selectedLaneID
                                        ? AppColors.accent(for: colorScheme).opacity(0.12)
                                        : AppColors.secondaryBackground(for: colorScheme)
                                )
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(AppColors.accent(for: colorScheme).opacity(lane.id == selectedLaneID ? 0.32 : 0.14), lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                }
            }
        }
        .padding(14)
        .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.accent(for: colorScheme))
    }
}

private struct ShopCollectionLanePage: View {
    let lane: MerchandiseCollabLane
    let allItems: [MerchandiseItem]
    let colorScheme: ColorScheme
    let onOpenItem: (MerchandiseItem) -> Void
    let onDismiss: () -> Void

    private var laneItems: [MerchandiseItem] {
        if lane.id == MerchandiseCollabLane.allID {
            return allItems
        }
        return allItems.filter { $0.belongsToLane(id: lane.id) }
    }

    private var laneTitle: String {
        if lane.id == MerchandiseCollabLane.allID {
            return AppLocalized.text("shop.lane.all_drops_title", fallback: "All pieces")
        }
        return lane.title
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    ShopInfoCard(
                        colorScheme: colorScheme,
                        title: laneTitle,
                        message: lane.subtitle
                    )
                    if laneItems.isEmpty {
                        ShopInfoCard(
                            colorScheme: colorScheme,
                            title: AppLocalized.text("shop.filter.empty.title", fallback: "Nothing in this view"),
                            message: AppLocalized.text("shop.filter.empty.body", fallback: "There is no visible product in this collection right now.")
                        )
                    } else {
                        LazyVStack(alignment: .leading, spacing: 9) {
                            ForEach(0..<laneItems.count, id: \.self) { index in
                                let item = laneItems[index]
                                MerchandiseRowView(
                                    item: item,
                                    environmentColorScheme: colorScheme,
                                    shelfSpotlight: index < 2,
                                    shelfSettled: index > 2
                                ) {
                                    onOpenItem($0)
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 16)
            }
            .background(
                AppColors.screenGradient(for: colorScheme, secondaryAccent: AppColors.accentMystic(for: colorScheme))
                    .overlay {
                        SkydownAtmosphereBackdrop(colorScheme: colorScheme)
                    }
                    .ignoresSafeArea()
            )
            .navigationTitle(laneTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Zurueck", action: onDismiss)
                }
            }
        }
    }
}

private struct ShopHeroMetricTile: View {
    let label: String
    let value: String
    let colorScheme: ColorScheme
    let isActive: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(label.uppercased())
                .font(.caption2.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Text(value)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 11)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(
                    isActive
                        ? AppColors.cardBackground(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme).opacity(0.8)
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1)
        )
    }
}

private struct ShopAdminControlsPanel: View {
    let colorScheme: ColorScheme
    let isUpdatingStoreState: Bool
    let isStoreOpen: Bool
    let isSyncingCatalog: Bool
    let onToggleStore: () -> Void
    let onSyncShopify: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 8) {
                Image(systemName: "arrow.triangle.2.circlepath")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                VStack(alignment: .leading, spacing: 3) {
                    Text(AppLocalized.text("shop.admin.title", fallback: "Store control"))
                        .font(AppTypography.sectionHeadline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(AppLocalized.text("shop.admin.subtitle", fallback: "Store status and refresh."))
                        .font(AppTypography.bodyCaption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                Spacer(minLength: 0)
                Text(AppLocalized.text("shop.admin.tag", fallback: "ADMIN"))
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                    )
            }

            HStack(spacing: 10) {
                HomeActionButton(
                    title: isUpdatingStoreState
                        ? AppLocalized.text("shop.admin.store.updating", fallback: "Updating store…")
                        : (isStoreOpen
                            ? AppLocalized.text("shop.admin.store.close", fallback: "Close store")
                            : AppLocalized.text("shop.admin.store.open", fallback: "Open store")),
                    subtitle: isStoreOpen
                        ? AppLocalized.text("shop.admin.store.close.hint", fallback: "Pause orders.")
                        : AppLocalized.text("shop.admin.store.open.hint", fallback: "Enable orders."),
                    icon: isStoreOpen ? "pause.fill" : "play.fill",
                    colorScheme: colorScheme,
                    brand: .neutral,
                    isPrimary: true
                ) { onToggleStore() }
                .disabled(isUpdatingStoreState)

                HomeActionButton(
                    title: isSyncingCatalog
                        ? AppLocalized.text("shop.admin.syncing", fallback: "Syncing…")
                        : AppLocalized.text("shop.admin.sync", fallback: "Sync"),
                    subtitle: AppLocalized.text("shop.admin.sync.hint", fallback: "Sync catalog from Shopify."),
                    icon: "arrow.triangle.2.circlepath",
                    colorScheme: colorScheme,
                    brand: .neutral,
                    isPrimary: false
                ) { onSyncShopify() }
                .disabled(isSyncingCatalog)
            }
        }
        .padding(14)
        .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.accent(for: colorScheme))
    }
}

private struct ShopMerchOpeningBlock: View {
    let showFeatured: Bool
    let colorScheme: ColorScheme
    let featuredItem: MerchandiseItem?
    let editorialPicks: [MerchandiseItem]
    let onOpenItem: (MerchandiseItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            if showFeatured {
                ShopLandingCuratedModule(
                    colorScheme: colorScheme,
                    featuredItem: featuredItem,
                    editorialPicks: editorialPicks,
                    onOpenItem: onOpenItem
                )
            }
            ShopBrowseEntryHint(showConnectorLine: showFeatured, colorScheme: colorScheme)
        }
        .padding(.vertical, 4)
    }
}

private struct ShopBrowseEntryHint: View {
    let showConnectorLine: Bool
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if showConnectorLine {
                Spacer()
                    .frame(height: 4)
                RoundedRectangle(cornerRadius: 1, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                .clear,
                                AppColors.text(for: colorScheme).opacity(0.07),
                                .clear,
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .frame(maxWidth: .infinity)
                    .frame(height: 36)
            }
            Text(AppLocalized.text("shop.browse.headline", fallback: "Browse the shelf"))
                .font(.title3.weight(.medium))
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(AppLocalized.text("shop.browse.subline", fallback: "Choose a collection, then open a product."))
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
        }
        .padding(.top, 4)
    }
}

private struct ShopInfoCard: View {
    let colorScheme: ColorScheme
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "info.circle.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
                Text("Info")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                    .padding(.horizontal, 7)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accent(for: colorScheme).opacity(0.11))
                    )
            }

            Text(message)
                .font(.caption.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let actionTitle, let action {
                HStack {
                    Spacer(minLength: 0)
                    Button(action: action) {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.right.circle.fill")
                                .font(.caption2.weight(.bold))
                            Text(actionTitle)
                                .font(.caption.weight(.bold))
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 7)
                        .foregroundColor(AppColors.accent(for: colorScheme))
                        .background(
                            Capsule(style: .continuous)
                                .fill(AppColors.accent(for: colorScheme).opacity(colorScheme == .dark ? 0.16 : 0.11))
                        )
                        .overlay(
                            Capsule(style: .continuous)
                                .stroke(AppColors.accent(for: colorScheme).opacity(0.22), lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownInteractiveFeedback()
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.085), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .transition(.opacity.combined(with: .move(edge: .top)))
        .animation(SkydownMotion.statusTransition, value: message)
    }
}

private struct ShopLandingCuratedModule: View {
    let colorScheme: ColorScheme
    let featuredItem: MerchandiseItem?
    let editorialPicks: [MerchandiseItem]
    let onOpenItem: (MerchandiseItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 10) {
                    Text(AppLocalized.text("shop.featured.chip", fallback: "Staff picks"))
                        .font(.caption2.weight(.bold))
                        .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(
                            Capsule(style: .continuous)
                                .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                        )
                    Text(AppLocalized.text("shop.featured.title", fallback: "Featured for you"))
                        .font(.title2.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Spacer(minLength: 0)
                }

                Text(AppLocalized.text("shop.featured.hint", fallback: "Start with the highlight, then pick a collection below to narrow the shelf."))
                    .font(.subheadline)
                    .lineSpacing(3)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .fixedSize(horizontal: false, vertical: true)
            }

            if let featuredItem {
                let primaryImageString: String = {
                    if !featuredItem.customImageOverride.isEmpty {
                        return featuredItem.customImageOverride
                    }
                    return featuredItem.imageURLs.first ?? ""
                }()
                let displayImageURL = URL(string: primaryImageString)

                Button {
                    onOpenItem(featuredItem)
                } label: {
                    VStack(alignment: .leading, spacing: 0) {
                        ZStack(alignment: .bottom) {
                            if let displayImageURL {
                                AsyncImage(url: displayImageURL) { phase in
                                    switch phase {
                                    case .success(let image):
                                        image
                                            .resizable()
                                            .scaledToFill()
                                    default:
                                        AppColors.secondaryBackground(for: colorScheme)
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .frame(height: 288)
                                .clipped()
                            } else {
                                RoundedRectangle(cornerRadius: 0, style: .continuous)
                                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.1))
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 160)
                            }

                            LinearGradient(
                                colors: [
                                    .clear,
                                    Color.black.opacity(0.22)
                                ],
                                startPoint: .center,
                                endPoint: .bottom
                            )
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text(AppLocalized.text("shop.featured.badge", fallback: "Highlight"))
                                .font(.caption.weight(.bold))
                                .foregroundColor(AppColors.accent(for: colorScheme))
                                .tracking(0.6)
                            Text(featuredItem.name)
                                .font(.title.weight(.semibold))
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .multilineTextAlignment(.leading)
                                .lineLimit(2)
                            Text("\(featuredItem.currency) \(featuredItem.price, specifier: "%.2f")")
                                .font(.headline)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                        .padding(.horizontal, 22)
                        .padding(.vertical, 20)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 32, style: .continuous)
                            .fill(AppColors.cardBackground(for: colorScheme).opacity(0.32))
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
                }
                .buttonStyle(.plain)
                .skydownInteractiveFeedback()
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(Array(editorialPicks.enumerated()), id: \.offset) { _, item in
                        Button {
                            onOpenItem(item)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.name)
                                    .font(.caption2.weight(.medium))
                                    .foregroundColor(AppColors.text(for: colorScheme).opacity(0.92))
                                    .lineLimit(2)
                                Text("\(item.currency) \(item.price, specifier: "%.2f")")
                                    .font(.caption2)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.7))
                            }
                            .frame(width: 132, alignment: .leading)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 9)
                            .background(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .fill(AppColors.cardBackground(for: colorScheme).opacity(0.45))
                            )
                        }
                        .buttonStyle(.plain)
                        .skydownInteractiveFeedback()
                    }
                }
            }
            .padding(.top, 2)
        }
    }
}

private struct ShopBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.accent(for: colorScheme)
        )
    }
}

#Preview {
    let services = AppServices()

    ShopView(
        authManager: services.authManager,
        merchandiseService: services.merchandiseService
    )
}
