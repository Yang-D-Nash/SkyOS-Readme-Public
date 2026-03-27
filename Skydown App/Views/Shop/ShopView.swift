//
//  ShopView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    @Environment(\.colorScheme) private var colorScheme
    let onOpenCart: () -> Void
    let onOpenSettings: () -> Void

    init(
        onOpenCart: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {}
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenSettings = onOpenSettings
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    HomeHeroIntroCard(colorScheme: colorScheme)
                    HomeLatestReleaseCard(viewModel: viewModel, colorScheme: colorScheme)
                    HomeLatestVideoCard(viewModel: viewModel, colorScheme: colorScheme)
                    HomeStoryCard(colorScheme: colorScheme)
                }
                .padding(.horizontal, 20)
                .padding(.top, 20)
                .padding(.bottom, 28)
            }
            .scrollIndicators(.hidden)
            .refreshable {
                viewModel.refresh()
            }
            .background(homeBackgroundGradient.ignoresSafeArea())
            .navigationTitle("Skydown x 22")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    AppSessionToolbarActions(
                        onOpenCart: onOpenCart,
                        onOpenSettings: onOpenSettings
                    )
                }
            }
            .task {
                viewModel.refresh()
            }
        }
    }

    private var homeBackgroundGradient: LinearGradient {
        LinearGradient(
            colors: [
                AppColors.primaryBackground(for: colorScheme),
                AppColors.accent(for: colorScheme).opacity(0.14),
                AppColors.accentMystic(for: colorScheme).opacity(0.08),
                AppColors.primaryBackground(for: colorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

struct ShopView: View {
    @ObservedObject private var authManager: AuthManager
    @StateObject private var viewModel: MerchandiseViewModel
    private let onOpenLogin: () -> Void
    private let onOpenCart: () -> Void
    private let onOpenSettings: () -> Void
    @State private var showingAddSheet = false
    @State private var editingItem: MerchandiseItem?
    @State private var selectedItem: MerchandiseItem?
    @State private var itemToDelete: MerchandiseItem?
    @Environment(\.colorScheme) private var colorScheme

    init(
        authManager: AuthManager,
        onOpenLogin: @escaping () -> Void = {},
        onOpenCart: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService()
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        self.onOpenLogin = onOpenLogin
        self.onOpenCart = onOpenCart
        self.onOpenSettings = onOpenSettings
        _viewModel = StateObject(
            wrappedValue: MerchandiseViewModel(
                merchandiseService: merchandiseService,
                authManager: authManager
            )
        )
    }

    private var isAdmin: Bool {
        authManager.userSession?.isAdmin == true
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView("Artikel werden geladen...")
                        .tint(AppColors.accent(for: colorScheme))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 16) {
                            ShopHeroCard(
                                colorScheme: colorScheme,
                                itemCount: viewModel.merchandiseItems.count,
                                isLoggedIn: authManager.userSession != nil,
                                isAdmin: isAdmin
                            )

                            if let errorMessage = viewModel.errorMessage, !isAdmin {
                                ShopInfoCard(
                                    colorScheme: colorScheme,
                                    title: "Anmeldung",
                                    message: errorMessage,
                                    actionTitle: "Anmelden",
                                    action: onOpenLogin
                                )
                            }

                            if viewModel.merchandiseItems.isEmpty {
                                ShopInfoCard(
                                    colorScheme: colorScheme,
                                    title: "Noch keine Artikel",
                                    message: "Sobald neuer Merch live ist, taucht er hier direkt als Card auf."
                                )
                            }

                            ForEach(viewModel.merchandiseItems) { item in
                                MerchandiseRowView(
                                    item: item,
                                    isAdmin: isAdmin,
                                    environmentColorScheme: colorScheme,
                                    onTap: {
                                        if isAdmin {
                                            editingItem = $0
                                        } else {
                                            selectedItem = $0
                                        }
                                    },
                                    onEdit: { editingItem = $0 },
                                    onDelete: { itemToDelete = $0 }
                                )
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 20)
                        .padding(.bottom, 28)
                    }
                    .scrollIndicators(.hidden)
                    .refreshable {
                        viewModel.fetchData()
                    }
                }
            }
            .background(backgroundGradient.ignoresSafeArea())
            .navigationTitle("Skydown Merch")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    HStack(spacing: 10) {
                        if isAdmin {
                            Button {
                                showingAddSheet = true
                            } label: {
                                Image(systemName: "plus")
                                    .font(.headline)
                            }
                        }

                        AppSessionToolbarActions(
                            onOpenCart: onOpenCart,
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
            .sheet(isPresented: $showingAddSheet) {
                MerchEditView(viewModel: viewModel)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
                    .background(AppColors.primaryBackground(for: colorScheme))
            }
            .sheet(item: $editingItem) { item in
                MerchEditView(viewModel: viewModel, merchandiseItem: item)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
                    .background(AppColors.primaryBackground(for: colorScheme))
            }
            .sheet(item: $selectedItem) { item in
                NavigationStack {
                    ContactFormView(item: item)
                        .background(AppColors.primaryBackground(for: colorScheme))
                }
            }
            .confirmationDialog(
                "Soll dieser Artikel wirklich geloescht werden?",
                isPresented: Binding(
                    get: { itemToDelete != nil },
                    set: { if !$0 { itemToDelete = nil } }
                ),
                titleVisibility: .visible
            ) {
                Button("Loeschen", role: .destructive) {
                    if let item = itemToDelete {
                        Task {
                            await viewModel.deleteItem(item)
                            itemToDelete = nil
                        }
                    }
                }
                Button("Abbrechen", role: .cancel) {
                    itemToDelete = nil
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
    }

    private var backgroundGradient: LinearGradient {
        LinearGradient(
            colors: [
                AppColors.primaryBackground(for: colorScheme),
                AppColors.accent(for: colorScheme).opacity(0.14),
                AppColors.accentMystic(for: colorScheme).opacity(0.08),
                AppColors.primaryBackground(for: colorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

private struct HomeHeroIntroCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Skydown x 22")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Hip Hop, Music, Video.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "house.fill")
                    .font(.title2)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(20)
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
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 10) {
                ShopBadge(text: "Hip Hop", colorScheme: colorScheme)
                ShopBadge(text: "22 Hamburg", colorScheme: colorScheme)
                ShopBadge(text: "Music & Video", colorScheme: colorScheme)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
    }
}

private struct HomeLatestReleaseCard: View {
    @ObservedObject var viewModel: HomeViewModel
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Neuester Release")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if let track = viewModel.featuredTrack {
                HStack(spacing: 14) {
                    AsyncImage(url: URL(string: track.artworkUrl100 ?? "")) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        RoundedRectangle(cornerRadius: 22)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    }
                    .frame(width: 82, height: 82)
                    .clipShape(RoundedRectangle(cornerRadius: 22))

                    VStack(alignment: .leading, spacing: 6) {
                        Text(track.trackName)
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(track.artistName ?? "Skydown x 22")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        Text(homeReleaseLine(for: track))
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }

                    Spacer()
                }

                HStack(spacing: 10) {
                    ShopBadge(text: "Neuester Song", colorScheme: colorScheme)
                    ShopBadge(
                        text: track.previewUrl != nil
                            ? "Preview in App"
                            : (track.externalURL != nil ? "Spotify Suche" : "Release"),
                        colorScheme: colorScheme
                    )
                }
            } else {
                Text(viewModel.homeTrackMessage ?? "Neuer Song erscheint hier.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private func homeReleaseLine(for track: Track) -> String {
        let collection = track.collectionName ?? "Spotify Release"
        if let releaseDate = track.releaseDate, !releaseDate.isEmpty {
            return "\(collection) • \(releaseDate)"
        }
        return collection
    }
}

private struct HomeLatestVideoCard: View {
    @ObservedObject var viewModel: HomeViewModel
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Video Highlight")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if let video = viewModel.featuredVideo {
                HStack(alignment: .top, spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 22)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                            .frame(width: 82, height: 82)

                        Image(systemName: "video.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.accent(for: colorScheme))
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(video.title)
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(video.projectName)
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        if !video.notes.isEmpty {
                            Text(video.notes)
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    }

                    Spacer()
                }

                HStack(spacing: 10) {
                    ShopBadge(text: "Videography", colorScheme: colorScheme)
                    ShopBadge(text: "Home Auswahl", colorScheme: colorScheme)
                }
            } else {
                Text(viewModel.homeVideoMessage ?? "Neues Video erscheint hier.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct HomeStoryCard: View {
    let colorScheme: ColorScheme
    @Environment(\.openURL) private var openURL

    private let developerInstagramURL = URL(string: "https://www.instagram.com/y.d.nash/")
    private let skydownInstagramURL = URL(string: "https://www.instagram.com/skydown_entertainment/")
    private let contactEmailURL = URL(string: "mailto:skydownent@gmail.com?subject=Skydown%20x%2022%20Kontakt")

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Developer")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            HStack(spacing: 10) {
                ShopBadge(text: "Yang D. Nash", colorScheme: colorScheme)
                ShopBadge(text: "Skydown", colorScheme: colorScheme)
                ShopBadge(text: "Entwickler", colorScheme: colorScheme)
            }

            Text("Yang D. Nash entwickelt die App und bildet den Kern von Skydown x 22. Skydown Entertainment verbindet Hip Hop, Music und Video mit einer eigenen mobilen Plattform.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(spacing: 10) {
                HomeActionButton(
                    title: "Yang D. Nash auf Instagram",
                    icon: "camera.fill",
                    colorScheme: colorScheme,
                    isPrimary: true
                ) {
                    if let developerInstagramURL {
                        openURL(developerInstagramURL)
                    }
                }

                HomeActionButton(
                    title: "Skydown auf Instagram",
                    icon: "sparkles.tv.fill",
                    colorScheme: colorScheme,
                    isPrimary: false
                ) {
                    if let skydownInstagramURL {
                        openURL(skydownInstagramURL)
                    }
                }

                HomeActionButton(
                    title: "Kontakt per E-Mail",
                    icon: "envelope.fill",
                    colorScheme: colorScheme,
                    isPrimary: false
                ) {
                    if let contactEmailURL {
                        openURL(contactEmailURL)
                    }
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
    }
}

private struct HomeActionButton: View {
    let title: String
    let icon: String
    let colorScheme: ColorScheme
    let isPrimary: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.subheadline.weight(.bold))
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Spacer()
            }
            .foregroundColor(isPrimary ? .white : AppColors.text(for: colorScheme))
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(
                        isPrimary
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(
                        isPrimary
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.accent(for: colorScheme).opacity(0.16),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

private struct ShopHeroCard: View {
    let colorScheme: ColorScheme
    let itemCount: Int
    let isLoggedIn: Bool
    let isAdmin: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Skydown Merch")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Merch & Drops.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "bag.fill")
                    .font(.title2)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 10) {
                ShopBadge(text: "\(itemCount) Produkte", colorScheme: colorScheme)
                ShopBadge(text: isLoggedIn ? "Konto aktiv" : "Gast", colorScheme: colorScheme)
                if isAdmin {
                    ShopBadge(text: "Admin", colorScheme: colorScheme)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
    }

    private var cardBackground: some View {
        LinearGradient(
            colors: [
                AppColors.cardBackground(for: colorScheme),
                AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

private struct ShopInfoCard: View {
    let colorScheme: ColorScheme
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(message)
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let actionTitle, let action {
                Button(action: action) {
                    Text(actionTitle)
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct ShopBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(AppColors.accent(for: colorScheme).opacity(0.12))
            .foregroundColor(AppColors.accent(for: colorScheme))
            .clipShape(Capsule())
    }
}

#Preview {
    let services = AppServices()

    ShopView(
        authManager: services.authManager,
        merchandiseService: services.merchandiseService
    )
}
