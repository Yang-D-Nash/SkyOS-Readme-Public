import AVFoundation
import SwiftUI
import UIKit

struct ArtistPageView: View {
    @ObservedObject private var authManager: AuthManager
    @ObservedObject private var store: ArtistPagesStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @StateObject private var tracksViewModel = ArtistPageTracksViewModel()
    @StateObject private var audioManager = AudioPlayerManager()

    let brand: ArtistPageBrand
    let artistName: String
    let onBack: (() -> Void)?
    let onNicmaProfileChange: ((String) -> Void)?

    @State private var isEditing = false
    @State private var taglineDraft = ""
    @State private var bioDraft = ""
    @State private var profileImageURLDraft = ""
    @State private var heroImageURLDraft = ""
    @State private var heroVideoURLDraft = ""
    @State private var instagramURLDraft = ""
    @State private var spotifyURLDraft = ""
    @State private var youtubeURLDraft = ""
    @State private var studioPriceListDraft = ""
    @State private var isSaving = false
    @State private var toastMessage = ""
    @State private var showToast = false
    @State private var toastStyle: ToastStyle = .success
    @State private var selectedTrackID: Int?
    @State private var isMiniBarDismissed = false
    @State private var sheetPresentation = SkydownQueuedPresentation<ArtistPagePresentedSheet>()
    @State private var activeImageUploadTarget: ArtistPageEditableImageTarget?
    @State private var isUploadingHeroVideo = false
    @State private var editingBaseProfileImageURL = ""
    @State private var editingBaseHeroImageURL = ""
    @State private var editingBaseHeroVideoURL = ""
    @State private var temporaryUploadedAssetURLs: [String] = []
    @State private var heroVideoPlayer = AVPlayer()
    private let editableImageUploadService = EditableImageAssetUploadService()
    private let isUITestMode = ProcessInfo.processInfo.arguments.contains("-ui_test")

    init(
        authManager: AuthManager,
        store: ArtistPagesStore,
        brand: ArtistPageBrand,
        artistName: String,
        onBack: (() -> Void)? = nil,
        onNicmaProfileChange: ((String) -> Void)? = nil
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        _store = ObservedObject(wrappedValue: store)
        self.brand = brand
        self.artistName = artistName
        self.onBack = onBack
        self.onNicmaProfileChange = onNicmaProfileChange
    }

    private var page: ArtistPage {
        store.page(for: brand, artistName: artistName)
    }

    private var routeArtistName: String {
        artistName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var allowsStudioPriceEditing: Bool {
        brand == .nicma && routeArtistName.caseInsensitiveCompare("NICMA STUDIO") == .orderedSame
    }

    private var isNicmaStudioPage: Bool {
        allowsStudioPriceEditing
    }

    private var showsNicmaProfileSwitch: Bool {
        brand == .nicma && onNicmaProfileChange != nil
    }

    private var studioFallbackBio: String {
        "Preise, Production, Recording."
    }

    /// Kurz halten: `BrandSurface` blendet die Eyebrow aus, sobald der Titel mit derselben Zeichenkette beginnt
    /// (lange Labels wie "NICMA STUDIO" würden bei Titel "NICMA STUDIO" komplett fehlen).
    private var nicmaHeroEyebrow: String {
        if brand == .nicma {
            if routeArtistName.caseInsensitiveCompare("NICMA STUDIO") == .orderedSame { return "STUDIO" }
            if routeArtistName.caseInsensitiveCompare("NICMA MUSIC") == .orderedSame { return "PRODUCER" }
        }
        return brand.displayTitle
    }

    /// Plain copy line — no capsule „chips“ that read as tappable highlights.
    private var heroMetaLine: String {
        var parts: [String] = [nicmaHeroEyebrow]
        if displayPage.hasCustomPresentation { parts.append("Live") }
        if !tracksViewModel.tracks.isEmpty { parts.append("\(tracksViewModel.tracks.count) Songs") }
        return parts.joined(separator: " · ")
    }

    private var displayPage: ArtistPage {
        guard isEditing else { return page }
        let stableId = artistPageDocumentID(brand: page.brand, artistName: routeArtistName)
        return ArtistPage(
            id: stableId,
            brand: page.brand,
            artistName: routeArtistName,
            tagline: taglineDraft.trimmedNilIfEmpty,
            bio: bioDraft.trimmedNilIfEmpty,
            profileImageURL: profileImageURLDraft.trimmedNilIfEmpty,
            heroImageURL: heroImageURLDraft.trimmedNilIfEmpty,
            heroVideoURL: heroVideoURLDraft.trimmedNilIfEmpty,
            instagramURL: instagramURLDraft.trimmedNilIfEmpty,
            spotifyURL: spotifyURLDraft.trimmedNilIfEmpty,
            youtubeURL: youtubeURLDraft.trimmedNilIfEmpty,
            studioPriceList: allowsStudioPriceEditing ? parseStudioPriceItems(from: studioPriceListDraft) : page.studioPriceList,
            editorUids: page.editorUids,
            createdAt: page.createdAt,
            updatedAt: page.updatedAt,
            isPlaceholder: false
        )
    }

    /// Lese: Music+STUDIO merge, oeffentliche Default-URLs wenn Firestore leer; beim Bearbeiten unveraendert.
    private var pageForConnect: ArtistPage {
        if isEditing { return displayPage }
        if brand == .nicma, routeArtistName.caseInsensitiveCompare("NICMA MUSIC") == .orderedSame {
            return displayPage
                .mergedNicmaConnectFromStudio(
                    store.page(for: .nicma, artistName: "NICMA STUDIO")
                )
                .withNicmaMusicPublicLinkDefaults()
        }
        return displayPage
    }

    private var resolvedStudioPackages: [NicmaProducerPackage] {
        if displayPage.studioPriceList.isEmpty {
            return nicmaProducerPackages
        }
        return displayPage.studioPriceList.map {
            NicmaProducerPackage(title: $0.title, detail: $0.detail, price: $0.price)
        }
    }

    private var canEdit: Bool {
        store.canEdit(page, user: authManager.userSession)
    }

    private var topTracks: [Track] {
        Array(tracksViewModel.tracks.prefix(3))
    }

    /// Leer, wenn der Banner allein reicht (Tag zeigt N LIVE).
    private var topSongsBannerSubtitle: String? {
        if tracksViewModel.isLoading { return "Lade …" }
        if !topTracks.isEmpty { return nil }
        if tracksViewModel.errorMessage != nil { return "Kurz warten, dann erneut" }
        return "Folgt dem Feed"
    }

    private var latestReleaseText: String? {
        tracksViewModel.tracks
            .compactMap { track in
                track.releaseDate.map { String($0.prefix(10)) }
            }
            .max()
    }

    private var spotlightTrack: Track? {
        tracksViewModel.tracks.first { $0.trackId == selectedTrackID } ?? tracksViewModel.tracks.first
    }

    private var nowPlayingTrack: Track? {
        guard let currentId = audioManager.currentlyPlayingId else { return nil }
        return tracksViewModel.tracks.first { $0.trackId == currentId }
    }

    private var miniBarTrack: Track? {
        if isMiniBarDismissed {
            return nowPlayingTrack
        }
        return nowPlayingTrack ?? spotlightTrack
    }

    private var linkCount: Int {
        socialLinks.count
    }

    private var artistStateLabel: String {
        displayPage.hasCustomPresentation ? "Live" : "Draft"
    }

    private var artistSoundLabel: String {
        if tracksViewModel.isLoading {
            return "Sync"
        }
        return topTracks.isEmpty ? "Leer" : "\(topTracks.count) Songs"
    }

    private var artistReachLabel: String {
        linkCount == 0 ? "Offline" : "\(linkCount) Links"
    }

    private var artistAccent: Color {
        switch brand {
        case .zweizwei:
            return AppColors.spotify(for: colorScheme)
        case .skydown:
            return AppColors.accent(for: colorScheme)
        case .nicma:
            return AppColors.accentHighlight(for: colorScheme)
        }
    }

    private var artistSecondaryAccent: Color {
        switch brand {
        case .zweizwei:
            return AppColors.accent(for: colorScheme)
        case .skydown:
            return AppColors.accentMystic(for: colorScheme)
        case .nicma:
            return AppColors.accent(for: colorScheme)
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    if showsNicmaProfileSwitch {
                        nicmaProfileSelector
                    }

                    if isNicmaStudioPage {
                        studioHeroCard
                        studioPriceListCard
                        linksCard
                    } else {
                        heroCard
                        spotlightCard
                        topTracksCard
                        linksCard
                    }

                    if canEdit && isEditing {
                        editorCard
                    }
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding)
            }
            .background(AppColors.screenGradient(for: colorScheme).ignoresSafeArea())
            .safeAreaInset(edge: .bottom) {
                if let miniBarTrack {
                    nowPlayingMiniBar(
                        track: miniBarTrack,
                        isPlaying: audioManager.currentlyPlayingId == miniBarTrack.trackId
                    )
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        .padding(.bottom, 54)
                }
            }
            .navigationTitle(displayPage.artistName)
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(onBack == nil ? "Schliessen" : "Zurueck") {
                        if let onBack {
                            onBack()
                        } else {
                            dismiss()
                        }
                    }
                }

                if canEdit {
                    ToolbarItem(placement: .topBarTrailing) {
                        if isEditing {
                            HStack(spacing: SkydownLayout.stackSpacingPill) {
                                Button("Abbrechen") {
                                    discardEditing()
                                }
                                .disabled(isSaving || isUploadingHeroVideo || activeImageUploadTarget != nil)
                                .accessibilityIdentifier("artist.page.edit.cancel")

                                Button(isSaving ? "Speichert..." : "Speichern") {
                                    Task { await savePage() }
                                }
                                .disabled(isSaving || isUploadingHeroVideo || activeImageUploadTarget != nil)
                                .accessibilityIdentifier("artist.page.edit.save")
                            }
                        } else {
                            Button {
                                beginEditing()
                            } label: {
                                Image(systemName: "pencil")
                            }
                            .accessibilityLabel("Bearbeiten")
                            .accessibilityIdentifier("artist.page.edit.open")
                        }
                    }
                }
            }
        }
        .fancyToast(
            isPresented: $showToast,
            message: toastMessage,
            style: toastStyle
        )
        .onAppear {
            syncDrafts()
            configureHeroVideoPlayer(for: displayPage.heroVideoURL)
        }
        .task(id: artistName) {
            await tracksViewModel.loadTracks(for: artistName)
        }
        .onChange(of: page) { _, _ in
            if !isEditing {
                syncDrafts()
            }
        }
        .onChange(of: displayPage.heroVideoURL) { _, newValue in
            configureHeroVideoPlayer(for: newValue)
        }
        .onChange(of: tracksViewModel.tracks.map(\.trackId)) { _, _ in
            let tracks = tracksViewModel.tracks
            guard !tracks.isEmpty else {
                selectedTrackID = nil
                return
            }

            if selectedTrackID == nil || !tracks.contains(where: { $0.trackId == selectedTrackID }) {
                selectedTrackID = tracks.first?.trackId
            }
        }
        .sheet(item: activePresentedSheetBinding) { sheet in
            switch sheet {
            case .youTube(let item):
                YouTubeEmbedPlayerView(item: item)
            case .editableImage(let target):
                SingleImagePicker { provider in
                    handleEditableImageProvider(provider, for: target)
                }
            case .editableVideo:
                SingleVideoPicker { url in
                    handleEditableVideoFile(url)
                }
            }
        }
        .onDisappear {
            audioManager.stop()
            heroVideoPlayer.pause()
        }
        .onReceive(
            NotificationCenter.default.publisher(
                for: .AVPlayerItemDidPlayToEndTime,
                object: heroVideoPlayer.currentItem
            )
        ) { _ in
            heroVideoPlayer.seek(to: .zero)
            heroVideoPlayer.play()
        }
    }

    private func nowPlayingMiniBar(track: Track, isPlaying: Bool) -> some View {
        HStack(spacing: SkydownLayout.stackSpacingCompact) {
            ArtistEqualizerPulse(
                isActive: isPlaying,
                tint: AppColors.spotify(for: colorScheme)
            )

            VStack(alignment: .leading, spacing: 2) {
                Text("Now Playing")
                    .font(.caption2.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                Text(track.trackName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)
            }

            Spacer(minLength: 0)

            if let spotifyURL = spotifyURL(for: track) {
                Link(destination: spotifyURL) {
                    Image(systemName: "arrow.up.forward")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.spotify(for: colorScheme))
                        .padding(8)
                        .background(
                            Circle()
                                .fill(AppColors.spotifySurface(for: colorScheme))
                        )
                }
                .buttonStyle(.plain)
            }

            Button {
                isMiniBarDismissed = false
                selectedTrackID = track.trackId
                audioManager.playPreview(for: track)
            } label: {
                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.spotify(for: colorScheme))
                    .padding(8)
                    .background(
                        Circle()
                            .fill(AppColors.spotifySurface(for: colorScheme))
                    )
            }
            .buttonStyle(.plain)

            Button {
                isMiniBarDismissed = true
                audioManager.stop()
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .padding(8)
                    .background(
                        Circle()
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    )
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            AppColors.cardBackground(for: colorScheme).opacity(0.94),
                            AppColors.secondaryBackground(for: colorScheme).opacity(0.82),
                            AppColors.spotifySurface(for: colorScheme).opacity(0.38)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous)
                .stroke(AppColors.spotify(for: colorScheme).opacity(0.20), lineWidth: 1)
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [
                            .white.opacity(colorScheme == .dark ? 0.20 : 0.30),
                            AppColors.spotify(for: colorScheme).opacity(0.24)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 0.8
                )
        )
    }

    private func spotifyURL(for track: Track) -> URL? {
        if let trackID = track.spotifyTrackID?.trimmingCharacters(in: .whitespacesAndNewlines), !trackID.isEmpty {
            return URL(string: "https://open.spotify.com/track/\(trackID)")
        }
        if let artistID = track.spotifyArtistID?.trimmingCharacters(in: .whitespacesAndNewlines), !artistID.isEmpty {
            return URL(string: "https://open.spotify.com/artist/\(artistID)")
        }
        if let external = track.externalURL?.trimmingCharacters(in: .whitespacesAndNewlines), external.contains("spotify.com") {
            return URL(string: external)
        }
        return nil
    }

    private struct ArtistEqualizerPulse: View {
        let isActive: Bool
        let tint: Color
        @State private var animate = false

        var body: some View {
            HStack(spacing: 2) {
                ForEach(0..<3, id: \.self) { index in
                    Capsule(style: .continuous)
                        .fill(tint.opacity(isActive ? 0.9 : 0.45))
                        .frame(width: 3, height: 12)
                        .scaleEffect(
                            y: isActive
                                ? (animate ? 1.2 - CGFloat(index) * 0.15 : 0.55 + CGFloat(index) * 0.1)
                                : 0.65
                        )
                        .animation(
                            isActive
                                ? .easeInOut(duration: 0.45).repeatForever(autoreverses: true).delay(Double(index) * 0.08)
                                : .easeOut(duration: 0.2),
                            value: animate
                        )
                }
            }
            .frame(width: 16, height: 14, alignment: .center)
            .onAppear { animate = true }
        }
    }

    private var nicmaProfileSelector: some View {
        HStack(spacing: SkydownLayout.stackSpacingPill) {
            ForEach(["NICMA MUSIC", "NICMA STUDIO"], id: \.self) { profile in
                let selected = routeArtistName.caseInsensitiveCompare(profile) == .orderedSame
                Button {
                    onNicmaProfileChange?(profile)
                } label: {
                    VStack(spacing: 4) {
                        Text(profile)
                            .font(.caption.weight(.semibold))
                            .lineLimit(1)
                        if selected {
                            Circle()
                                .fill(AppColors.spotify(for: colorScheme))
                                .frame(width: 5, height: 5)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
                .buttonStyle(.plain)
                .foregroundColor(AppColors.text(for: colorScheme))
                .background(
                    RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous)
                        .fill(
                            selected
                            ? AppColors.secondaryBackground(for: colorScheme).opacity(0.88)
                            : AppColors.cardBackground(for: colorScheme)
                        )
                )
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius, style: .continuous)
                        .stroke(
                            (selected ? AppColors.spotify(for: colorScheme) : AppColors.accentMystic(for: colorScheme))
                                .opacity(selected ? 0.42 : 0.16),
                            lineWidth: selected ? 1.4 : 1
                        )
                )
                .skydownTactileAction()
                .accessibilityIdentifier("nicma.profile.\(profile.replacingOccurrences(of: " ", with: "_").lowercased())")
            }
        }
    }

    private var studioHeroCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSection) {
            BrandHeroSurface(
                colorScheme: colorScheme,
                eyebrow: "NICMA",
                title: displayPage.artistName,
                subtitle: displayPage.tagline ?? "Studio",
                detail: displayPage.bio ?? studioFallbackBio,
                backgroundImageURL: displayPage.heroImageURL,
                accent: AppColors.accentMystic(for: colorScheme),
                secondaryAccent: AppColors.spotify(for: colorScheme),
                marks: [.zweizwei],
                edgeToEdge: true
            ) {
                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                    MusicBadge(text: "Mix", isAccent: true)
                    MusicBadge(text: "Master", isAccent: false)
                    MusicBadge(text: "Rec", isAccent: false)
                }
            }

            Text(displayPage.bio ?? studioFallbackBio)
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .padding(SkydownLayout.panelPadding)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(AppColors.cardBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                        .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.12), lineWidth: 1)
                )
        }
    }

    private var studioPriceListCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            ArtistSectionBanner(
                title: "Preisliste",
                subtitle: "Production, Recording, Mixing",
                icon: "slider.horizontal.3",
                colorScheme: colorScheme,
                accent: AppColors.accentMystic(for: colorScheme),
                tag: "\(resolvedStudioPackages.count) ITEMS"
            )

            ForEach(resolvedStudioPackages) { package in
                NicmaProducerPriceCard(
                    package: package,
                    colorScheme: colorScheme
                )
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentMystic(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius
        )
    }

    private var activePresentedSheetBinding: Binding<ArtistPagePresentedSheet?> {
        Binding(
            get: { sheetPresentation.activeItem },
            set: { sheetPresentation.updatePresentedItem($0) }
        )
    }

    private func presentSheet(_ sheet: ArtistPagePresentedSheet) {
        sheetPresentation.request(sheet)
    }

    private func beginEditing() {
        editingBaseProfileImageURL = page.profileImageURL ?? ""
        editingBaseHeroImageURL = page.heroImageURL ?? ""
        editingBaseHeroVideoURL = page.heroVideoURL ?? ""
        temporaryUploadedAssetURLs.removeAll()
        syncDrafts()
        isEditing = true
    }

    private func discardEditing() {
        let cleanupURLs = temporaryUploadedAssetURLs
        temporaryUploadedAssetURLs.removeAll()
        syncDrafts()
        isEditing = false
        isUploadingHeroVideo = false

        guard !cleanupURLs.isEmpty else {
            showToast("Aenderungen verworfen.", style: .info)
            return
        }

        Task {
            for url in cleanupURLs {
                try? await editableImageUploadService.deleteAsset(at: url)
            }

            await MainActor.run {
                showToast("Aenderungen verworfen.", style: .info)
            }
        }
    }

    private func handleEditableImageProvider(
        _ temporaryFileURL: URL?,
        for target: ArtistPageEditableImageTarget
    ) {
        activePresentedSheetBinding.wrappedValue = nil

        guard let temporaryFileURL else {
            return
        }

        Task {
            await MainActor.run {
                activeImageUploadTarget = target
            }
            do {
                let previousURL = currentEditableImageURL(for: target)
                defer { try? FileManager.default.removeItem(at: temporaryFileURL) }
                let data = try await PickedImageUploadPreparation.normalizedJPEGData(fromTemporaryFileURL: temporaryFileURL)
                let url = try await editableImageUploadService.uploadImageData(data)
                await MainActor.run {
                    registerTemporaryAsset(previousURL: previousURL, newURL: url)
                    switch target {
                    case .profile:
                        profileImageURLDraft = url
                    case .hero:
                        heroImageURLDraft = url
                    }
                    showToast("Bild hochgeladen und uebernommen.", style: .success)
                }
            } catch {
                await MainActor.run {
                    showToast("Bild konnte nicht hochgeladen werden: \(error.localizedDescription)", style: .error)
                }
            }

            await MainActor.run {
                activeImageUploadTarget = nil
            }
        }
    }

    private func handleEditableVideoFile(_ temporaryFileURL: URL?) {
        activePresentedSheetBinding.wrappedValue = nil

        guard let temporaryFileURL else {
            return
        }

        Task {
            await MainActor.run {
                isUploadingHeroVideo = true
            }

            do {
                let fileName = temporaryFileURL.lastPathComponent
                let mimeType = (try? temporaryFileURL.resourceValues(forKeys: [.contentTypeKey]).contentType?.preferredMIMEType)
                    ?? "video/mp4"
                let previousURL = heroVideoURLDraft
                let url = try await editableImageUploadService.uploadVideoFile(
                    from: temporaryFileURL,
                    fileName: fileName,
                    mimeType: mimeType
                )

                await MainActor.run {
                    registerTemporaryAsset(previousURL: previousURL, newURL: url)
                    heroVideoURLDraft = url
                    showToast("Hero-Video hochgeladen und uebernommen.", style: .success)
                }
            } catch {
                await MainActor.run {
                    showToast("Hero-Video konnte nicht hochgeladen werden: \(error.localizedDescription)", style: .error)
                }
            }

            try? FileManager.default.removeItem(at: temporaryFileURL)

            await MainActor.run {
                isUploadingHeroVideo = false
            }
        }
    }

    private func currentEditableImageURL(for target: ArtistPageEditableImageTarget) -> String {
        switch target {
        case .profile:
            return profileImageURLDraft
        case .hero:
            return heroImageURLDraft
        }
    }

    private func removeEditableImage(for target: ArtistPageEditableImageTarget) {
        let previousURL = currentEditableImageURL(for: target)
        switch target {
        case .profile:
            profileImageURLDraft = ""
        case .hero:
            heroImageURLDraft = ""
        }

        if temporaryUploadedAssetURLs.contains(previousURL) {
            temporaryUploadedAssetURLs.removeAll { $0 == previousURL }
            Task {
                try? await editableImageUploadService.deleteAsset(at: previousURL)
            }
        }

        showToast("Bild entfernt.", style: .success)
    }

    private func removeHeroVideo() {
        let previousURL = heroVideoURLDraft
        heroVideoURLDraft = ""

        if temporaryUploadedAssetURLs.contains(previousURL) {
            temporaryUploadedAssetURLs.removeAll { $0 == previousURL }
            Task {
                try? await editableImageUploadService.deleteAsset(at: previousURL)
            }
        }

        showToast("Hero-Video entfernt.", style: .success)
    }

    private func registerTemporaryAsset(previousURL: String, newURL: String) {
        guard !newURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        if previousURL != newURL {
            temporaryUploadedAssetURLs.removeAll { url in
                if url == previousURL {
                    Task {
                        try? await editableImageUploadService.deleteAsset(at: previousURL)
                    }
                    return true
                }
                return false
            }
        }

        if !temporaryUploadedAssetURLs.contains(newURL) {
            temporaryUploadedAssetURLs.append(newURL)
        }
    }

    private func configureHeroVideoPlayer(for urlString: String?) {
        let trimmedURL = urlString?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard let url = URL(string: trimmedURL), !trimmedURL.isEmpty else {
            heroVideoPlayer.pause()
            heroVideoPlayer.replaceCurrentItem(with: nil)
            return
        }

        let currentURL = (heroVideoPlayer.currentItem?.asset as? AVURLAsset)?.url
        if currentURL != url {
            heroVideoPlayer.replaceCurrentItem(with: AVPlayerItem(url: url))
        }
        heroVideoPlayer.isMuted = true
        heroVideoPlayer.play()
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSection) {
            ZStack(alignment: .bottomLeading) {
                heroVisual
                    .frame(height: 286)
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))

                LinearGradient(
                    colors: [
                        Color.black.opacity(0.18),
                        Color.clear,
                        Color.black.opacity(0.84)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
                    Text(heroMetaLine)
                        .font(.caption.weight(.semibold))
                        .foregroundColor(.white.opacity(0.88))
                        .shadow(color: .black.opacity(0.2), radius: 4, y: 2)

                    Spacer(minLength: 0)

                    HStack(alignment: .bottom, spacing: SkydownLayout.stackSpacingComfortable) {
                        ArtistPageAvatar(
                            imageURL: displayPage.profileImageURL,
                            fallbackText: displayPage.artistName,
                            size: 96,
                            colorScheme: colorScheme
                        )
                        .shadow(color: .black.opacity(0.22), radius: 18, y: 10)

                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                            Text(displayPage.artistName)
                                .font(.system(size: 30, weight: .black, design: .rounded))
                                .foregroundColor(.white)
                                .shadow(color: .black.opacity(0.28), radius: 14, y: 6)

                            Text(displayPage.tagline ?? "\(brand.displayTitle) Profil")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(.white.opacity(0.86))
                                .lineLimit(2)
                                .shadow(color: .black.opacity(0.18), radius: 8, y: 3)

                            if let latestReleaseText {
                                Text("Release: \(latestReleaseText)")
                                    .font(.caption.weight(.bold))
                                    .foregroundColor(.white.opacity(0.76))
                                    .shadow(color: .black.opacity(0.16), radius: 6, y: 2)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding(20)
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                Text(displayPage.bio ?? "Noch keine Beschreibung.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                artistHeroMotionStage

                artistSessionDeck

                if !displayPage.editorUids.isEmpty {
                    Text(
                        "\(displayPage.editorUids.count) Editor\(displayPage.editorUids.count == 1 ? "" : "en")"
                    )
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
                }
            }
            .padding(SkydownLayout.panelPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.cardBackground(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1)
            )
        }
    }

    private var heroVisual: some View {
        ZStack {
            if let heroImageURL = displayPage.heroImageURL, let url = URL(string: heroImageURL) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    fallbackHero
                }
            } else {
                fallbackHero
            }
        }
    }

    private var artistHeroMotionStage: some View {
        let hasHeroVideo = displayPage.heroVideoURL?.trimmedNilIfEmpty != nil

        return ZStack(alignment: .bottomLeading) {
            Group {
                if hasHeroVideo {
                    ArtistHeroVideoSurface(player: heroVideoPlayer)
                } else if let heroImageURL = displayPage.heroImageURL?.trimmedNilIfEmpty, let url = URL(string: heroImageURL) {
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        fallbackHero
                    }
                } else {
                    fallbackHero
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .overlay(
                LinearGradient(
                    colors: [
                        Color.white.opacity(0.06),
                        Color.clear,
                        Color.black.opacity(0.72)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )

        }
        .frame(height: 200)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous))
    }

    private var fallbackHero: some View {
        LinearGradient(
            colors: [
                AppColors.spotify(for: colorScheme).opacity(0.84),
                AppColors.accent(for: colorScheme).opacity(0.78),
                AppColors.primaryBackground(for: colorScheme)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private var artistSessionDeck: some View {
        HStack(alignment: .firstTextBaseline, spacing: SkydownLayout.stackSpacingDense) {
            Text(artistStateLabel)
            Text("·")
            Text(artistSoundLabel)
            Text("·")
            Text(artistReachLabel)
        }
        .font(.caption.weight(.medium))
        .foregroundColor(AppColors.secondaryText(for: colorScheme).opacity(0.9))
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Status \(artistStateLabel), Katalog \(artistSoundLabel), \(artistReachLabel)")
    }

    private var spotlightCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            ArtistSectionBanner(
                title: "Spotlight",
                subtitle: spotlightTrack == nil
                    ? "Folgt dem Feed"
                    : "Tippen startet die Preview",
                icon: "sparkles",
                colorScheme: colorScheme,
                accent: artistAccent,
                tag: spotlightTrack == nil ? "PROFILE" : "LIVE"
            )

            Text(displayPage.tagline ?? "\(displayPage.artistName) auf \(brand.displayTitle) entdecken.")
                .font(.title3.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            if let spotlightTrack {
                HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
                    AsyncImage(url: URL(string: spotlightTrack.artworkUrl100 ?? "")) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        RoundedRectangle(cornerRadius: SkydownLayout.microCorner, style: .continuous)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    }
                    .frame(width: 64, height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.microCorner, style: .continuous))

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                        Text(spotlightTrack.trackName)
                            .font(.headline.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(spotlightTrack.collectionName ?? displayPage.artistName)
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    isMiniBarDismissed = false
                    selectedTrackID = spotlightTrack.trackId
                    withAnimation(.easeInOut(duration: 0.20)) {
                        audioManager.playPreview(for: spotlightTrack)
                    }
                }
            } else {
                Text(
                    displayPage.bio
                        ?? "Kein Track im Feed."
                )
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.spotify(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius
        )
    }

    private var topTracksCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            ArtistSectionBanner(
                title: "Top Songs",
                subtitle: topSongsBannerSubtitle,
                icon: "waveform",
                colorScheme: colorScheme,
                accent: AppColors.spotify(for: colorScheme),
                tag: tracksViewModel.isLoading ? "SYNC" : topTracks.isEmpty ? "EMPTY" : "\(topTracks.count) LIVE"
            )

            if tracksViewModel.isLoading {
                ArtistSupportMessage(
                    message: "Lade …",
                    colorScheme: colorScheme,
                    accent: AppColors.spotify(for: colorScheme)
                )
            } else if let errorMessage = tracksViewModel.errorMessage {
                ArtistSupportMessage(
                    message: errorMessage,
                    colorScheme: colorScheme,
                    accent: AppColors.youtube(for: colorScheme)
                )
            } else if topTracks.isEmpty {
                ArtistSupportMessage(
                    message: "Noch keine Songs.",
                    colorScheme: colorScheme,
                    accent: AppColors.accentMystic(for: colorScheme)
                )
            } else {
                ForEach(Array(topTracks.enumerated()), id: \.1.trackId) { index, track in
                    minimalTrackRow(track: track)
                    if index < topTracks.count - 1 {
                        Divider()
                            .overlay(AppColors.secondaryText(for: colorScheme).opacity(0.16))
                    }
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accent(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius
        )
    }

    private func minimalTrackRow(track: Track) -> some View {
        let isSelected = selectedTrackID == track.trackId
        return Button {
            isMiniBarDismissed = false
            selectedTrackID = track.trackId
            audioManager.playPreview(for: track)
        } label: {
            HStack(spacing: SkydownLayout.stackSpacingCompact) {
                AsyncImage(url: URL(string: track.artworkUrl100 ?? "")) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    RoundedRectangle(cornerRadius: SkydownLayout.microCorner, style: .continuous)
                        .fill(AppColors.secondaryBackground(for: colorScheme))
                }
                .frame(width: 44, height: 44)
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.microCorner, style: .continuous))

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
                    Text(track.trackName)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(1)
                    Text(track.collectionName ?? displayPage.artistName)
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
                Image(systemName: isSelected ? "speaker.wave.2.fill" : "play.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.spotify(for: colorScheme).opacity(isSelected ? 1 : 0.8))
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }

    private var linksCard: some View {
        let primaryConnectLinks = socialLinks.filter { $0.kind == .instagram || $0.kind == .spotify }
        let secondaryConnectLinks = socialLinks.filter { $0.kind != .instagram && $0.kind != .spotify }

        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            ArtistSectionBanner(
                title: "Connect",
                subtitle: "Instagram, Spotify, YouTube",
                icon: "arrow.up.forward.square",
                colorScheme: colorScheme,
                accent: artistSecondaryAccent,
                tag: socialLinks.isEmpty ? "OFFLINE" : "\(socialLinks.count) LIVE"
            )

            if socialLinks.isEmpty {
                ArtistSupportMessage(
                    message: "Noch keine Links im Profil.",
                    colorScheme: colorScheme,
                    accent: artistSecondaryAccent
                )
            } else {
                if !primaryConnectLinks.isEmpty {
                    HStack(spacing: SkydownLayout.stackSpacingPill) {
                        ForEach(primaryConnectLinks) { link in
                            Button {
                                if let url = URL(string: link.url) {
                                    openURL(url)
                                }
                            } label: {
                                HStack(spacing: SkydownLayout.stackSpacingTick) {
                                    Image(systemName: link.systemImage)
                                        .font(.caption2.weight(.semibold))
                                    Text(link.title)
                                        .font(.caption2.weight(.bold))
                                        .lineLimit(1)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 7)
                                .padding(.horizontal, 9)
                                .background(
                                    Capsule(style: .continuous)
                                        .fill(
                                            LinearGradient(
                                                colors: [
                                                    AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.78 : 0.66),
                                                    link.backgroundColor.opacity(colorScheme == .dark ? 0.24 : 0.16),
                                                    link.tint.opacity(colorScheme == .dark ? 0.12 : 0.08)
                                                ],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                        )
                                )
                                .overlay(
                                    Capsule(style: .continuous)
                                        .stroke(link.tint.opacity(0.30), lineWidth: 1)
                                )
                                .overlay(
                                    Capsule(style: .continuous)
                                        .stroke(
                                            LinearGradient(
                                                colors: [
                                                    .white.opacity(colorScheme == .dark ? 0.16 : 0.28),
                                                    link.tint.opacity(0.18)
                                                ],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            ),
                                            lineWidth: 0.8
                                        )
                                )
                            }
                            .buttonStyle(.plain)
                            .foregroundColor(link.tint)
                            .skydownTactileAction()
                        }
                    }
                }

                ForEach(secondaryConnectLinks) { link in
                    Button {
                        if link.kind == .youtube {
                            presentSheet(.youTube(SkydownYouTubeVideoItem(
                                id: "artist-\(displayPage.slug)-links-youtube",
                                title: displayPage.artistName,
                                subtitle: link.subtitle,
                                urlString: link.url
                            )))
                        } else if let url = URL(string: link.url) {
                            openURL(url)
                        }
                    } label: {
                        HStack(spacing: SkydownLayout.stackSpacingCompact) {
                            Image(systemName: link.systemImage)
                                .font(.headline.weight(.bold))
                                .foregroundColor(link.tint)
                                .frame(width: 28)

                            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
                                Text(link.title)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(AppColors.text(for: colorScheme))
                                Text(link.subtitle)
                                    .font(.footnote)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    .lineLimit(1)
                            }

                            Spacer()

                            Image(systemName: "arrow.up.forward.square")
                                .font(.footnote.weight(.bold))
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .padding(14)
                    .background(
                        LinearGradient(
                            colors: [
                                AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.68 : 0.52),
                                link.backgroundColor.opacity(colorScheme == .dark ? 0.22 : 0.16),
                                link.tint.opacity(colorScheme == .dark ? 0.10 : 0.08)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                            .stroke(link.tint.opacity(0.22), lineWidth: 1)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                            .stroke(
                                LinearGradient(
                                    colors: [
                                        .white.opacity(colorScheme == .dark ? 0.16 : 0.26),
                                        link.tint.opacity(0.16)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 0.8
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accent(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius
        )
    }

    private var editorCard: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            ArtistSectionBanner(
                title: "Artist Page bearbeiten",
                subtitle: "Vorschau bis Speichern",
                icon: "slider.horizontal.3",
                colorScheme: colorScheme,
                accent: artistSecondaryAccent,
                tag: "ADMIN"
            )

            ArtistPageInputField(
                title: "Kurzzeile",
                text: $taglineDraft,
                placeholder: "z. B. Rap, Melodic, 22",
                colorScheme: colorScheme
            )

            ArtistPageMultilineInput(
                title: "Bio",
                text: $bioDraft,
                placeholder: "Kurze repraesentative Beschreibung fuer den Artist.",
                colorScheme: colorScheme
            )

            EditableImageField(
                title: "Profilbild",
                imageURL: $profileImageURLDraft,
                colorScheme: colorScheme,
                isUploading: activeImageUploadTarget == .profile,
                uploadStatusText: "Profilbild wird fuer die Artist-Seite uebernommen.",
                onPickImage: { presentSheet(.editableImage(.profile)) },
                onRemoveImage: { removeEditableImage(for: .profile) }
            )

            EditableImageField(
                title: "Hero-Bild",
                imageURL: $heroImageURLDraft,
                colorScheme: colorScheme,
                isUploading: activeImageUploadTarget == .hero,
                uploadStatusText: "Hero-Bild wird fuer die Artist-Seite uebernommen.",
                onPickImage: { presentSheet(.editableImage(.hero)) },
                onRemoveImage: { removeEditableImage(for: .hero) }
            )

            EditableVideoField(
                title: "Hero-Video",
                videoURL: $heroVideoURLDraft,
                colorScheme: colorScheme,
                isUploading: isUploadingHeroVideo,
                uploadStatusText: "Hero-Video wird als Motion-Stage vorbereitet.",
                accessibilityIDPrefix: "artist.page.hero_video",
                onPickVideo: { presentSheet(.editableVideo) },
                onRemoveVideo: removeHeroVideo
            )

            if isUITestMode {
                Button {
                    Task { await uploadHeroVideoFixture() }
                } label: {
                    Label("UI Test: Hero-Video Fixture", systemImage: "video.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(artistSecondaryAccent)
                .disabled(isUploadingHeroVideo || isSaving)
                .accessibilityIdentifier("ui_test.artist.hero_video.upload_fixture")
            }

            ArtistPageInputField(
                title: "Instagram",
                text: $instagramURLDraft,
                placeholder: "https://instagram.com/...",
                colorScheme: colorScheme
            )

            ArtistPageInputField(
                title: "Spotify",
                text: $spotifyURLDraft,
                placeholder: "https://open.spotify.com/artist/...",
                colorScheme: colorScheme
            )

            ArtistPageInputField(
                title: "YouTube",
                text: $youtubeURLDraft,
                placeholder: "https://youtube.com/...",
                colorScheme: colorScheme
            )

            if allowsStudioPriceEditing {
                ArtistPageMultilineInput(
                    title: "Studio-Preisliste",
                    text: $studioPriceListDraft,
                    placeholder: "Je Zeile: Titel | Detail | Preis",
                    colorScheme: colorScheme
                )
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.spotify(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius
        )
    }

    private func uploadHeroVideoFixture() async {
        guard canEdit else { return }
        isUploadingHeroVideo = true
        defer { isUploadingHeroVideo = false }

        if UITestRuntime.usesIsolatedAuthService {
            let previousURL = heroVideoURLDraft
            let url = "https://ui-tests.skydown.local/assets/artist-hero-video-fixture.mp4"
            registerTemporaryAsset(previousURL: previousURL, newURL: url)
            heroVideoURLDraft = url
            showToast("Hero-Video Fixture hochgeladen.", style: .success)
            return
        }

        let tempURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("ui-test-hero-video")
            .appendingPathExtension("mp4")

        do {
            try ArtistPageUITestFixtures.sampleMP4.write(to: tempURL, options: [.atomic])
            let previousURL = heroVideoURLDraft
            let url = try await editableImageUploadService.uploadVideoFile(
                from: tempURL,
                fileName: "fixture.mp4",
                mimeType: "video/mp4"
            )
            registerTemporaryAsset(previousURL: previousURL, newURL: url)
            heroVideoURLDraft = url
            showToast("Hero-Video Fixture hochgeladen.", style: .success)
        } catch {
            showToast("Fixture Upload fehlgeschlagen: \(error.localizedDescription)", style: .error)
        }

        try? FileManager.default.removeItem(at: tempURL)
    }

    private var socialLinks: [ArtistPageSocialLink] {
        var links: [ArtistPageSocialLink] = []

        let resolvedInstagramURL = pageForConnect.instagramURL?.trimmingCharacters(in: .whitespacesAndNewlines)
        let instagramURL = (resolvedInstagramURL?.isEmpty == false)
            ? resolvedInstagramURL
            : (isNicmaStudioPage ? nicmaInstagramDestination.urlString : nil)

        if let instagramURL, !instagramURL.isEmpty {
            links.append(
                ArtistPageSocialLink(
                    kind: .instagram,
                    title: "Instagram",
                    subtitle: pageForConnect.artistName,
                    url: instagramURL,
                    systemImage: "camera.circle.fill",
                    tint: AppColors.instagramStart(for: colorScheme),
                    backgroundColor: AppColors.instagramStart(for: colorScheme),
                    foregroundColor: .white
                )
            )
        }

        if let spotifyURL = pageForConnect.spotifyURL?.trimmingCharacters(in: .whitespacesAndNewlines), !spotifyURL.isEmpty {
            links.append(
                ArtistPageSocialLink(
                    kind: .spotify,
                    title: "Spotify",
                    subtitle: "Artist Profil",
                    url: spotifyURL,
                    systemImage: "music.note",
                    tint: AppColors.spotify(for: colorScheme),
                    backgroundColor: AppColors.spotifySurface(for: colorScheme),
                    foregroundColor: AppColors.spotify(for: colorScheme)
                )
            )
        }

        if let youtubeURL = pageForConnect.youtubeURL?.trimmingCharacters(in: .whitespacesAndNewlines), !youtubeURL.isEmpty {
            links.append(
                ArtistPageSocialLink(
                    kind: .youtube,
                    title: "YouTube",
                    subtitle: "Videos & Releases",
                    url: youtubeURL,
                    systemImage: "play.rectangle.fill",
                    tint: AppColors.youtube(for: colorScheme),
                    backgroundColor: AppColors.youtube(for: colorScheme),
                    foregroundColor: .white
                )
            )
        }

        return links
    }

    private func syncDrafts() {
        taglineDraft = page.tagline ?? ""
        bioDraft = page.bio ?? ""
        profileImageURLDraft = page.profileImageURL ?? ""
        heroImageURLDraft = page.heroImageURL ?? ""
        heroVideoURLDraft = page.heroVideoURL ?? ""
        instagramURLDraft = page.instagramURL ?? ""
        spotifyURLDraft = page.spotifyURL ?? ""
        youtubeURLDraft = page.youtubeURL ?? ""
        studioPriceListDraft = page.studioPriceList
            .map { "\($0.title) | \($0.detail) | \($0.price)" }
            .joined(separator: "\n")
    }

    private func savePage() async {
        guard canEdit else { return }
        isSaving = true
        defer { isSaving = false }

        do {
            let documentId = artistPageDocumentID(brand: page.brand, artistName: routeArtistName)
            let updatedPage = ArtistPage(
                id: documentId,
                brand: page.brand,
                artistName: routeArtistName,
                tagline: taglineDraft.trimmedNilIfEmpty,
                bio: bioDraft.trimmedNilIfEmpty,
                profileImageURL: profileImageURLDraft.trimmedNilIfEmpty,
                heroImageURL: heroImageURLDraft.trimmedNilIfEmpty,
                heroVideoURL: heroVideoURLDraft.trimmedNilIfEmpty,
                instagramURL: instagramURLDraft.trimmedNilIfEmpty,
                spotifyURL: spotifyURLDraft.trimmedNilIfEmpty,
                youtubeURL: youtubeURLDraft.trimmedNilIfEmpty,
                studioPriceList: allowsStudioPriceEditing ? parseStudioPriceItems(from: studioPriceListDraft) : page.studioPriceList,
                editorUids: page.editorUids,
                createdAt: page.createdAt,
                updatedAt: .now,
                isPlaceholder: false
            )

            try await store.save(updatedPage)

            let savedAssetURLs = Set([
                updatedPage.profileImageURL,
                updatedPage.heroImageURL,
                updatedPage.heroVideoURL
            ].compactMap { $0?.trimmedNilIfEmpty })

            let cleanupURLs = Set(
                [
                    editingBaseProfileImageURL.trimmedNilIfEmpty,
                    editingBaseHeroImageURL.trimmedNilIfEmpty,
                    editingBaseHeroVideoURL.trimmedNilIfEmpty
                ]
                .compactMap { $0 }
                .filter { !savedAssetURLs.contains($0) }
                + temporaryUploadedAssetURLs.filter { !savedAssetURLs.contains($0) }
            )

            for url in cleanupURLs {
                try? await editableImageUploadService.deleteAsset(at: url)
            }

            temporaryUploadedAssetURLs.removeAll()
            editingBaseProfileImageURL = updatedPage.profileImageURL ?? ""
            editingBaseHeroImageURL = updatedPage.heroImageURL ?? ""
            editingBaseHeroVideoURL = updatedPage.heroVideoURL ?? ""
            isEditing = false
            showToast("Artist-Seite gespeichert.", style: .success)
        } catch {
            showToast(error.localizedDescription, style: .error)
        }
    }

    private func showToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

}

private func parseStudioPriceItems(from rawValue: String) -> [StudioPriceItem] {
    rawValue
        .split(whereSeparator: \.isNewline)
        .compactMap { line -> StudioPriceItem? in
            let parts = line
                .split(separator: "|", omittingEmptySubsequences: false)
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            guard parts.count >= 3 else { return nil }
            let title = parts[0]
            let detail = parts[1]
            let price = parts.dropFirst(2).joined(separator: " | ")
            guard !title.isEmpty, !detail.isEmpty, !price.isEmpty else { return nil }
            return StudioPriceItem(title: title, detail: detail, price: price)
        }
}

private enum ArtistPageEditableImageTarget: String, Identifiable, Equatable {
    case profile
    case hero

    var id: String { rawValue }
}

private enum ArtistPagePresentedSheet: Identifiable, Equatable {
    case youTube(SkydownYouTubeVideoItem)
    case editableImage(ArtistPageEditableImageTarget)
    case editableVideo

    var id: String {
        switch self {
        case .youTube(let item):
            return "youtube-\(item.id)"
        case .editableImage(let target):
            return "editable-image-\(target.rawValue)"
        case .editableVideo:
            return "editable-video"
        }
    }
}

private struct ArtistPageSocialLink: Identifiable {
    enum Kind {
        case instagram
        case spotify
        case youtube
    }

    let id = UUID()
    let kind: Kind
    let title: String
    let subtitle: String
    let url: String
    let systemImage: String
    let tint: Color
    let backgroundColor: Color
    let foregroundColor: Color
}

private struct ArtistPageAvatar: View {
    let imageURL: String?
    let fallbackText: String
    let size: CGFloat
    let colorScheme: ColorScheme

    var body: some View {
        ZStack {
            Circle()
                .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                .frame(width: size, height: size)

            if let imageURL, let url = URL(string: imageURL) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    fallbackLabel
                }
                .frame(width: size, height: size)
                .clipShape(Circle())
            } else {
                fallbackLabel
            }
        }
    }

    private var fallbackLabel: some View {
        Text(String(fallbackText.prefix(1)).uppercased())
            .font(.system(size: size * 0.34, weight: .black, design: .rounded))
            .foregroundColor(AppColors.accent(for: colorScheme))
    }
}

private struct ArtistPageBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.text(for: colorScheme)
        )
    }
}

private struct ArtistSectionBanner: View {
    let title: String
    var subtitle: String? = nil
    let icon: String
    let colorScheme: ColorScheme
    let accent: Color
    let tag: String

    var body: some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
            ZStack {
                RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                    .fill(accent.opacity(colorScheme == .dark ? 0.20 : 0.14))
                    .frame(width: 44, height: 44)

                Image(systemName: icon)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(accent)
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                Text(title)
                    .font(.title3.weight(.black))
                    .foregroundColor(AppColors.text(for: colorScheme))

                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.footnote.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineSpacing(2)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            SkydownMetaLabel(
                text: tag,
                tint: accent
            )
        }
    }
}

private struct ArtistSupportMessage: View {
    let message: String
    let colorScheme: ColorScheme
    let accent: Color

    var body: some View {
        HStack(alignment: .top, spacing: SkydownLayout.stackSpacingCompact) {
            Circle()
                .fill(accent.opacity(colorScheme == .dark ? 0.24 : 0.16))
                .frame(width: 34, height: 34)
                .overlay(
                    Image(systemName: "sparkles")
                        .font(.caption.weight(.bold))
                        .foregroundColor(accent)
                )

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSubtle) {
                Text("In Arbeit")
                    .font(.caption.weight(.bold))
                    .foregroundColor(accent)
                    .tracking(0.5)

                Text(message)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineSpacing(2)
            }

            Spacer(minLength: 0)
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(accent.opacity(0.18), lineWidth: 1)
        )
    }
}

private struct ArtistHeroVideoSurface: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> ArtistHeroVideoPlayerView {
        let view = ArtistHeroVideoPlayerView()
        view.playerLayer.videoGravity = .resizeAspectFill
        view.playerLayer.player = player
        return view
    }

    func updateUIView(_ uiView: ArtistHeroVideoPlayerView, context: Context) {
        uiView.playerLayer.player = player
    }
}

private final class ArtistHeroVideoPlayerView: UIView {
    override class var layerClass: AnyClass {
        AVPlayerLayer.self
    }

    var playerLayer: AVPlayerLayer {
        layer as! AVPlayerLayer
    }
}

private struct ArtistInfoMetric: View {
    let title: String
    let value: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
            Text(title.uppercased())
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(value)
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
    }
}

private struct ArtistPageInputField: View {
    let title: String
    @Binding var text: String
    let placeholder: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(placeholder, text: $text)
                .textInputAutocapitalization(.never)
                .padding(.horizontal, 14)
                .padding(.vertical, 14)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
        }
    }
}

private struct ArtistPageMultilineInput: View {
    let title: String
    @Binding var text: String
    let placeholder: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            ZStack(alignment: .topLeading) {
                if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(placeholder)
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .padding(.horizontal, 18)
                        .padding(.vertical, SkydownLayout.cardPadding)
                }

                TextEditor(text: $text)
                    .scrollContentBackground(.hidden)
                    .frame(minHeight: 120)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
            }
        }
    }
}

private extension String {
    var trimmedNilIfEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

private enum ArtistPageUITestFixtures {
    // Minimal MP4 bytes. Signature validity is not required for storage upload tests.
    static let sampleMP4: Data = Data(
        base64Encoded: "AAAAHGZ0eXBpc29tAAAAAGlzb21pc28yYXZjMW1wNDEAAAAIZnJlZQAAABxtZGF0AAAAAA=="
    ) ?? Data([0x00])
}

@MainActor
private final class ArtistPageTracksViewModel: ObservableObject {
    @Published private(set) var tracks: [Track] = []
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?

    private let service: MusicServicing

    init(service: MusicServicing = SpotifyMusicService()) {
        self.service = service
    }

    func loadTracks(for artist: String) async {
        let trimmedArtist = artist.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedArtist.isEmpty else {
            tracks = []
            errorMessage = nil
            return
        }

        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            tracks = try await service.fetchTracks(for: trimmedArtist)
        } catch {
            tracks = []
            errorMessage = "Songs fuer \(trimmedArtist) konnten gerade nicht geladen werden."
        }
    }
}
