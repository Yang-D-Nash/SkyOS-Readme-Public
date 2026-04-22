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
    @State private var selectedItem: MerchandiseItem?
    @State private var selectedCollabLaneID = MerchandiseCollabLane.allID
    @Environment(\.colorScheme) private var colorScheme

    init(
        authManager: AuthManager,
        onOpenLogin: @escaping () -> Void = {},
        onOpenCart: @escaping () -> Void = {},
        onOpenProfile: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService()
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        self.onOpenLogin = onOpenLogin
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
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
                        ProgressView("Shop wird geladen...")
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
                                    isAdmin: isAdmin,
                                    isUpdatingStoreState: viewModel.isUpdatingStoreState,
                                    isSyncingCatalog: viewModel.isSyncingCatalog,
                                    onToggleStore: isAdmin ? {
                                        Task {
                                            await viewModel.toggleStoreOpen()
                                        }
                                    } : nil,
                                    onSyncShopify: isAdmin ? {
                                        Task {
                                            await viewModel.syncShopifyCatalog()
                                        }
                                    } : nil
                                )

                                if !editorialPickItems.isEmpty {
                                    ShopLandingCuratedModule(
                                        colorScheme: colorScheme,
                                        featuredItem: featuredDropItem,
                                        editorialPicks: editorialPickItems,
                                        onOpenItem: { item in
                                            selectedItem = item
                                        }
                                    )
                                }

                                if let errorMessage = viewModel.errorMessage {
                                    ShopInfoCard(
                                        colorScheme: colorScheme,
                                        title: "Shop nicht erreichbar",
                                        message: errorMessage,
                                        actionTitle: "Erneut laden",
                                        action: {
                                            viewModel.fetchData()
                                        }
                                    )
                                }

                                if !viewModel.isStoreOpen && !isAdmin {
                                    ShopInfoCard(
                                        colorScheme: colorScheme,
                                        title: "Store pausiert",
                                        message: "Produkte sichtbar. Checkout pausiert."
                                    )
                                }

                                if viewModel.merchandiseItems.isEmpty, viewModel.errorMessage == nil {
                                    ShopInfoCard(
                                        colorScheme: colorScheme,
                                        title: viewModel.isSyncingCatalog ? "Shopify laedt" : "Noch kein Merch",
                                        message: isAdmin
                                            ? (viewModel.isSyncingCatalog
                                               ? "Der Katalog wird neu aufgebaut."
                                               : "Die App zieht den Shopify-Katalog automatisch nach.")
                                            : "Neuer Merch taucht hier direkt auf."
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
                                                    title: "Noch keine Pieces",
                                                    message: "In \(selectedCollabLane.title) ist gerade noch kein sichtbarer Merch.",
                                                    actionTitle: selectedCollabLaneID == MerchandiseCollabLane.allID ? nil : "All Drops",
                                                    action: selectedCollabLaneID == MerchandiseCollabLane.allID ? nil : {
                                                        selectedCollabLaneID = MerchandiseCollabLane.allID
                                                    }
                                                )
                                            } else {
                                                LazyVStack(alignment: .leading, spacing: layout.sectionSpacing) {
                                                    ForEach(filteredMerchandiseItems) { item in
                                                        MerchandiseRowView(
                                                            item: item,
                                                            environmentColorScheme: colorScheme
                                                        ) {
                                                            selectedItem = $0
                                                        }
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
                                            title: "Noch keine Pieces",
                                            message: "In \(selectedCollabLane.title) ist gerade noch kein sichtbarer Merch.",
                                            actionTitle: selectedCollabLaneID == MerchandiseCollabLane.allID ? nil : "All Drops",
                                            action: selectedCollabLaneID == MerchandiseCollabLane.allID ? nil : {
                                                selectedCollabLaneID = MerchandiseCollabLane.allID
                                            }
                                        )
                                    } else {
                                        LazyVStack(alignment: .leading, spacing: layout.sectionSpacing) {
                                            ForEach(filteredMerchandiseItems) { item in
                                                MerchandiseRowView(
                                                    item: item,
                                                    environmentColorScheme: colorScheme
                                                ) {
                                                    selectedItem = $0
                                                }
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
                        secondaryAccent: AppColors.accentHighlight(for: colorScheme)
                    )
                    .ignoresSafeArea()
                )
                .navigationTitle("Shop")
                .navigationBarTitleDisplayMode(.inline)
                .skydownNavigationChrome(colorScheme: colorScheme)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        HStack(spacing: 10) {
                            AppSessionToolbarActions(
                                onOpenCart: onOpenCart,
                                onOpenProfile: onOpenProfile,
                                onOpenSettings: onOpenSettings
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
    let isAdmin: Bool
    let isUpdatingStoreState: Bool
    let isSyncingCatalog: Bool
    let onToggleStore: (() -> Void)?
    let onSyncShopify: (() -> Void)?

    var body: some View {
        BrandHeroSurface(
            colorScheme: colorScheme,
            eyebrow: screenHeaderSettingsStore.settings.resolvedShopEyebrow ?? "Store",
            title: screenHeaderSettingsStore.settings.resolvedShopTitle ?? "Shop",
            subtitle: screenHeaderSettingsStore.settings.resolvedShopSubtitle ?? "Merch in-app.",
            detail: screenHeaderSettingsStore.settings.resolvedShopDetail
                ?? (isStoreOpen ? "Checkout offen." : "Nur Ansicht."),
            backgroundImageURL: screenHeaderSettingsStore.settings.resolvedShopImageURL,
            accent: AppColors.accentHighlight(for: colorScheme),
            secondaryAccent: AppColors.accentMystic(for: colorScheme),
            marks: [.skydownX22]
        ) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ShopBadge(text: "\(itemCount) Produkte", colorScheme: colorScheme)
                    ShopBadge(text: isStoreOpen ? "Store offen" : "Store pausiert", colorScheme: colorScheme)
                    if laneCount > 0 {
                        ShopBadge(
                            text: laneCount == 1 ? "1 Lane" : "\(laneCount) Lanes",
                            colorScheme: colorScheme
                        )
                    }
                    ShopBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
                }
            }

            if onToggleStore != nil || onSyncShopify != nil {
                HStack(spacing: 10) {
                    if let onToggleStore {
                        HomeActionButton(
                            title: isUpdatingStoreState ? "Update..." : (isStoreOpen ? "Schliessen" : "Oeffnen"),
                            subtitle: isStoreOpen ? "Bestellungen pausieren." : "Bestellungen aktivieren.",
                            icon: isStoreOpen ? "pause.fill" : "play.fill",
                            colorScheme: colorScheme,
                            brand: .neutral,
                            isPrimary: true
                        ) {
                            onToggleStore()
                        }
                        .disabled(isUpdatingStoreState)
                    }

                    if let onSyncShopify {
                        HomeActionButton(
                            title: isSyncingCatalog ? "Laedt..." : "Sync",
                            subtitle: "Katalog syncen.",
                            icon: "arrow.triangle.2.circlepath",
                            colorScheme: colorScheme,
                            brand: .neutral,
                            isPrimary: false
                        ) {
                            onSyncShopify()
                        }
                        .disabled(isSyncingCatalog)
                    }
                }
            }

            EmptyView()
        }
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
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
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

    private let moodAreas = ["SkyOS Drops", "Studio Picks", "Tech Selects", "Limited Finds"]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Text("Today’s Selection")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                    )
                Text("Kuratierte Picks")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Spacer(minLength: 0)
            }

            if let featuredItem {
                Button {
                    onOpenItem(featuredItem)
                } label: {
                    VStack(alignment: .leading, spacing: 7) {
                        Text("Featured Drop")
                            .font(.caption2.weight(.bold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                        Text(featuredItem.name)
                            .font(.headline.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .multilineTextAlignment(.leading)
                        Text("EUR \(featuredItem.price, specifier: "%.2f")")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 11)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .skydownInteractiveFeedback()
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(editorialPicks.enumerated()), id: \.offset) { _, item in
                        Button {
                            onOpenItem(item)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.name)
                                    .font(.caption.weight(.semibold))
                                    .foregroundColor(AppColors.text(for: colorScheme))
                                    .lineLimit(1)
                                Text("EUR \(item.price, specifier: "%.2f")")
                                    .font(.caption2.weight(.medium))
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            }
                            .frame(width: 156, alignment: .leading)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(AppColors.primaryBackground(for: colorScheme).opacity(0.84))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                        .skydownInteractiveFeedback()
                    }
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 7) {
                    ForEach(moodAreas, id: \.self) { mood in
                        Text(mood)
                            .font(.caption2.weight(.semibold))
                            .foregroundColor(AppColors.accentMystic(for: colorScheme))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 5)
                            .background(
                                Capsule(style: .continuous)
                                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.11))
                            )
                    }
                }
            }
        }
        .padding(.vertical, 2)
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
