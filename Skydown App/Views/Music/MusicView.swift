//
//  MusicView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI

enum MusicExperienceBrand {
    case skydown
    case zweizwei

    var navigationTitle: String {
        switch self {
        case .skydown:
            return "Music"
        case .zweizwei:
            return "Music"
        }
    }

    var heroTitle: String {
        navigationTitle
    }

    var heroSubtitle: String {
        switch self {
        case .skydown:
            return "Katalog · Spotify"
        case .zweizwei:
            return "Releases · Tracks · Studio"
        }
    }

    var artists: [String] {
        switch self {
        case .skydown:
            return ["Yang D. Nash", "ThaDude", "MAVE", "JANNO", "TANGAJOE007"]
        case .zweizwei:
            return ["JANNO", "Yang D. Nash", "ThaDude", "MAVE", "TANGAJOE007"]
        }
    }

    var fallbackArtistName: String {
        switch self {
        case .skydown:
            return "Skydown"
        case .zweizwei:
            return "22"
        }
    }

    var workflowTitle: String? {
        switch self {
        case .skydown:
            return nil
        case .zweizwei:
            return "Studio Services"
        }
    }

    var workflowSubtitle: String? {
        switch self {
        case .skydown:
            return nil
        case .zweizwei:
            return "Record · Mix · Master"
        }
    }

    var showsArtistPages: Bool {
        switch self {
        case .skydown:
            return false
        case .zweizwei:
            return true
        }
    }

    var artistPageBrand: ArtistPageBrand {
        switch self {
        case .skydown:
            return .skydown
        case .zweizwei:
            return .zweizwei
        }
    }
}

struct MusicView: View {
    @StateObject private var viewModel = MusicViewModel()
    @ObservedObject private var artistPagesStore = ArtistPagesStore.shared
    @ObservedObject private var screenHeaderSettingsStore = ScreenHeaderSettingsStore.shared
    @State private var selectedArtist: String
    @State private var sheetPresentation = SkydownQueuedPresentation<MusicPresentedSheet>()
    @State private var hasAutoPresentedArtistPage = false
    @State private var stageMotionTrigger = 0
    @State private var listMotionRevealed = false
    @EnvironmentObject private var services: AppServices
    @Environment(\.colorScheme) private var colorScheme

    let brand: MusicExperienceBrand
    let onBack: (() -> Void)?
    let onOpenCart: (() -> Void)?
    let onOpenProfile: (() -> Void)?
    let onOpenSettings: (() -> Void)?
    let onGuestSignIn: (() -> Void)?
    let onArtistContextChange: ((String) -> Void)?
    private let autoPresentArtistPageOnAppear: Bool

    init(
        brand: MusicExperienceBrand = .skydown,
        initialArtist: String? = nil,
        autoPresentArtistPageOnAppear: Bool = false,
        onBack: (() -> Void)? = nil,
        onArtistContextChange: ((String) -> Void)? = nil,
        onOpenCart: (() -> Void)? = nil,
        onOpenProfile: (() -> Void)? = nil,
        onOpenSettings: (() -> Void)? = nil,
        onGuestSignIn: (() -> Void)? = nil
    ) {
        self.brand = brand
        self.onBack = onBack
        self.autoPresentArtistPageOnAppear = autoPresentArtistPageOnAppear
        self.onArtistContextChange = onArtistContextChange
        self.onOpenCart = onOpenCart
        self.onOpenProfile = onOpenProfile
        self.onOpenSettings = onOpenSettings
        self.onGuestSignIn = onGuestSignIn
        let resolvedInitialArtist = initialArtist.flatMap { requestedArtist in
            brand.artists.contains(requestedArtist) ? requestedArtist : nil
        } ?? brand.artists.first ?? "Yang D. Nash"
        _selectedArtist = State(initialValue: resolvedInitialArtist)
    }

    private var artists: [String] {
        brand.artists
    }

    var body: some View {
        GeometryReader { proxy in
            let layout = SkydownResponsiveLayout(availableWidth: proxy.size.width)
            let contentWidth = min(
                layout.contentMaxWidth,
                max(proxy.size.width - (layout.horizontalPadding * 2), 0)
            )

            NavigationStack {
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        catalogHeroBanner
                        artistsCard
                    }
                    .frame(maxWidth: contentWidth, alignment: .leading)
                    .padding(.horizontal, layout.horizontalPadding)
                    .padding(.top, SkydownLayout.screenTopPadding)
                    .padding(.bottom, SkydownLayout.screenBottomPadding + 24)
                    .frame(maxWidth: .infinity)
                    .skydownSceneMotion(trigger: stageMotionTrigger, axis: .vertical, travel: 20, blurRadius: 3.2)
                    .skydownSelectionFeedback(trigger: selectedArtist)
                }
                .accessibilityIdentifier("music.catalog.root")
                .scrollIndicators(.hidden)
                .background(musicBackground)
                .navigationTitle(brand.navigationTitle)
                .navigationBarTitleDisplayMode(.inline)
                .skydownNavigationChrome(colorScheme: colorScheme)
                .toolbar {
                    if let onBack {
                        ToolbarItem(placement: .topBarLeading) {
                            Button(action: onBack) {
                                Image(systemName: "chevron.left")
                                    .font(.headline.weight(.bold))
                            }
                        }
                    }

                    if let onOpenSettings {
                        ToolbarItem(placement: .topBarTrailing) {
                            AppSessionToolbarActions(
                                onOpenCart: onOpenCart,
                                onOpenProfile: onOpenProfile,
                                onOpenSettings: onOpenSettings,
                                onGuestSignIn: onGuestSignIn
                            )
                        }
                    } else if viewModel.isSpotifyConnected {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(role: .destructive) {
                                viewModel.disconnectSpotify()
                            } label: {
                                Image(systemName: "rectangle.portrait.and.arrow.right")
                                    .font(.subheadline.weight(.bold))
                            }
                        }
                    }
                }
                .onChange(of: selectedArtist) { _, newArtist in
                    onArtistContextChange?(newArtist)
                }
                .onAppear {
                    stageMotionTrigger += 1
                    withAnimation(
                        .spring(response: 0.52, dampingFraction: 0.88, blendDuration: 0.1)
                        .delay(0.05)
                    ) {
                        listMotionRevealed = true
                    }
                    if autoPresentArtistPageOnAppear && brand.showsArtistPages && !hasAutoPresentedArtistPage {
                        presentSheet(.artistPage)
                        hasAutoPresentedArtistPage = true
                    }
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .sheet(item: activePresentedSheetBinding) { sheet in
            switch sheet {
            case .artistPage:
                ArtistPageView(
                    authManager: services.authManager,
                    store: artistPagesStore,
                    brand: brand.artistPageBrand,
                    artistName: selectedArtist
                )
            }
        }
    }

    private var activePresentedSheetBinding: Binding<MusicPresentedSheet?> {
        Binding(
            get: { sheetPresentation.activeItem },
            set: { sheetPresentation.updatePresentedItem($0) }
        )
    }

    private func presentSheet(_ sheet: MusicPresentedSheet) {
        sheetPresentation.request(sheet)
    }

    private var musicBackground: some View {
        AppColors.screenGradient(
            for: colorScheme,
            secondaryAccent: AppColors.spotify(for: colorScheme)
        )
        .overlay {
            SkydownAtmosphereBackdrop(colorScheme: colorScheme)
        }
        .ignoresSafeArea()
    }

    private func musicCardBackground(
        accent: Color,
        secondaryAccent: Color? = nil,
        cornerRadius: CGFloat = SkydownLayout.cardCornerRadius
    ) -> some View {
        let secondary = secondaryAccent ?? AppColors.accentHighlight(for: colorScheme)

        return RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(
                LinearGradient(
                    colors: [
                        AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.985 : 0.995),
                        AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.76 : 0.68),
                        accent.opacity(colorScheme == .dark ? 0.12 : 0.07),
                        secondary.opacity(colorScheme == .dark ? 0.08 : 0.05)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(colorScheme == .dark ? 0.06 : 0.14),
                                Color.clear
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottom
                        )
                    )
                    .blendMode(.screen)
            }
    }

    private func musicCardStroke(
        accent: Color,
        secondaryAccent: Color? = nil,
        cornerRadius: CGFloat = SkydownLayout.cardCornerRadius
    ) -> some View {
        let secondary = secondaryAccent ?? AppColors.accentHighlight(for: colorScheme)

        return RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .stroke(
                LinearGradient(
                    colors: [
                        accent.opacity(colorScheme == .dark ? 0.20 : 0.14),
                        secondary.opacity(colorScheme == .dark ? 0.14 : 0.10),
                        Color.white.opacity(colorScheme == .dark ? 0.06 : 0.12)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                lineWidth: 1
            )
    }

    @ViewBuilder
    private var catalogHeroBanner: some View {
        let resolvedEyebrow = screenHeaderSettingsStore.settings.resolvedMusicHubEyebrow ?? "SkyOS"
        let resolvedTitle = screenHeaderSettingsStore.settings.resolvedMusicHubTitle ?? brand.navigationTitle
        let resolvedSubtitle = screenHeaderSettingsStore.settings.resolvedMusicHubSubtitle
            ?? "Ein Hub · drei Wege."
        let resolvedDetail = screenHeaderSettingsStore.settings.resolvedMusicHubDetail
            ?? "Katalog, Releases, Studio."
        Group {
            BrandHeroSurface(
                colorScheme: colorScheme,
                eyebrow: resolvedEyebrow,
                title: resolvedTitle,
                subtitle: resolvedSubtitle,
                detail: resolvedDetail,
                backgroundImageURL: screenHeaderSettingsStore.settings.resolvedMusicHubImageURL,
                accent: AppColors.spotify(for: colorScheme),
                secondaryAccent: AppColors.accent(for: colorScheme),
                marks: brand == .zweizwei ? [.zweizwei] : [.skydown],
                edgeToEdge: true,
                onSurfaceTap: brand.showsArtistPages
                    ? { presentSheet(.artistPage) }
                    : nil
            ) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        MusicBadge(
                            text: selectedArtist,
                            isAccent: true,
                            accentOverride: AppColors.spotify(for: colorScheme),
                            accessibilityIdentifier: brand.showsArtistPages
                                ? "music.hero.open_artist_page"
                                : nil
                        ) {
                            if brand.showsArtistPages {
                                presentSheet(.artistPage)
                            }
                        }
                    }
                }
            }
        }
        .accessibilityIdentifier("music.catalog.hero")
        .animation(SkydownMotion.statusTransition, value: selectedArtist)
    }

    @ViewBuilder
    private var artistsCard: some View {
        if brand.showsArtistPages {
            artistEinstiegSection
        } else {
            VStack(alignment: .leading, spacing: 12) {
                Text("Kuenstler")
                    .font(.headline)

                ForEach(artists, id: \.self) { artist in
                    artistButton(for: artist)
                }
            }
            .padding(SkydownLayout.cardPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                musicCardBackground(
                    accent: AppColors.accentHighlight(for: colorScheme),
                    secondaryAccent: AppColors.spotify(for: colorScheme)
                )
            )
            .overlay(
                musicCardStroke(
                    accent: AppColors.accentHighlight(for: colorScheme),
                    secondaryAccent: AppColors.spotify(for: colorScheme)
                )
            )
        }
    }

    private var artistEinstiegSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Direkter Einstieg")
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .opacity(listMotionRevealed ? 1 : 0.4)
                .offset(y: listMotionRevealed ? 0 : 4)
                .animation(
                    .spring(response: 0.44, dampingFraction: 0.9),
                    value: listMotionRevealed
                )
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(artists.enumerated()), id: \.element) { index, artist in
                    artistToPageButton(artist: artist, rowIndex: index)
                }
            }
        }
    }

    private func catalogEntryAccent(for artist: String) -> Color {
        switch artist {
        case "JANNO":
            return AppColors.accent(for: colorScheme)
        case "Yang D. Nash":
            return AppColors.accentHighlight(for: colorScheme)
        case "MAVE":
            return AppColors.accentMystic(for: colorScheme)
        case "ThaDude":
            return AppColors.accent(for: colorScheme)
        case "TANGAJOE007":
            return AppColors.spotify(for: colorScheme)
        default:
            return AppColors.spotify(for: colorScheme)
        }
    }

    private func artistToPageButton(artist: String, rowIndex: Int) -> some View {
        let accent = catalogEntryAccent(for: artist)
        let isSelected = selectedArtist == artist
        let stagger = Double(rowIndex) * SkydownMotion.listStaggerDelay
        return Button {
            withAnimation(SkydownMotion.emphasizedTransition) {
                selectedArtist = artist
            }
            presentSheet(.artistPage)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: isSelected ? "arrow.up.right.circle.fill" : "arrow.up.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(
                        isSelected
                        ? accent
                        : AppColors.text(for: colorScheme)
                    )
                Text(artist)
                    .font(AppTypography.musicArtistName)
                    .tracking(0.28)
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .foregroundColor(AppColors.text(for: colorScheme))
            .padding(.horizontal, 12)
            .padding(.vertical, 11)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(
                        isSelected
                        ? accent.opacity(colorScheme == .dark ? 0.22 : 0.14)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(
                        accent.opacity(isSelected ? 0.55 : 0.35),
                        lineWidth: 1
                    )
            )
            .skydownLuminousSweep(cornerRadius: 14, accent: accent, alpha: colorScheme == .dark ? 0.1 : 0.07)
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .accessibilityIdentifier("music.artist.open_page.\(artist)")
        .opacity(listMotionRevealed ? 1 : 0)
        .offset(y: listMotionRevealed ? 0 : 10)
        .scaleEffect(listMotionRevealed ? 1 : 0.985, anchor: .topLeading)
        .animation(
            .spring(response: 0.48, dampingFraction: 0.86, blendDuration: 0.05)
            .delay(stagger),
            value: listMotionRevealed
        )
        .animation(SkydownMotion.statusTransition, value: isSelected)
    }

    private func artistButton(for artist: String) -> some View {
        let isSelected = selectedArtist == artist

        return Button {
            selectedArtist = artist
        } label: {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "music.mic")
                    .font(.title3)

                VStack(alignment: .leading, spacing: 2) {
                    Text(artist)
                        .font(.headline)
                        .lineLimit(1)

                    Text(isSelected ? "Aktiv" : "Waehlen")
                        .font(.caption)
                        .foregroundColor(
                            isSelected
                            ? Color.white.opacity(0.82)
                            : AppColors.secondaryText(for: colorScheme)
                        )
                }

                Spacer()
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                    .stroke(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.accent(for: colorScheme).opacity(0.18),
                        lineWidth: 1
                    )
            )
            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private enum MusicPresentedSheet: String, Identifiable, Equatable {
    case artistPage

    var id: String { rawValue }
}

#Preview {
    MusicView()
}
