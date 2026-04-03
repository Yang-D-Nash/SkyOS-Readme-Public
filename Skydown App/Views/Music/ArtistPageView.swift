import SwiftUI

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

    @State private var isEditing = false
    @State private var taglineDraft = ""
    @State private var bioDraft = ""
    @State private var profileImageURLDraft = ""
    @State private var heroImageURLDraft = ""
    @State private var instagramURLDraft = ""
    @State private var spotifyURLDraft = ""
    @State private var youtubeURLDraft = ""
    @State private var isSaving = false
    @State private var toastMessage = ""
    @State private var showToast = false
    @State private var toastStyle: ToastStyle = .success
    @State private var selectedTrackID: Int?
    @State private var selectedYouTubeItem: SkydownYouTubeVideoItem?
    @State private var pendingImageTarget: ArtistPageEditableImageTarget?
    @State private var activeImageUploadTarget: ArtistPageEditableImageTarget?
    private let editableImageUploadService = EditableImageAssetUploadService()

    init(
        authManager: AuthManager,
        store: ArtistPagesStore,
        brand: ArtistPageBrand,
        artistName: String
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        _store = ObservedObject(wrappedValue: store)
        self.brand = brand
        self.artistName = artistName
    }

    private var page: ArtistPage {
        store.page(for: brand, artistName: artistName)
    }

    private var canEdit: Bool {
        store.canEdit(page, user: authManager.userSession)
    }

    private var topTracks: [Track] {
        Array(tracksViewModel.tracks.prefix(5))
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

    private var linkCount: Int {
        socialLinks.count
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    heroCard
                    spotlightCard
                    topTracksCard
                    linksCard

                    if canEdit && isEditing {
                        editorCard
                    }
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding)
            }
            .background(AppColors.screenGradient(for: colorScheme).ignoresSafeArea())
            .navigationTitle(page.artistName)
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Schliessen") {
                        dismiss()
                    }
                }

                if canEdit {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(isEditing ? "Fertig" : "Bearbeiten") {
                            if isEditing {
                                Task { await savePage() }
                            } else {
                                syncDrafts()
                                isEditing = true
                            }
                        }
                        .disabled(isSaving)
                    }
                }
            }
        }
        .fancyToast(
            isPresented: $showToast,
            message: toastMessage,
            style: toastStyle
        )
        .sheet(item: $selectedYouTubeItem) { item in
            YouTubeEmbedPlayerView(item: item)
        }
        .onAppear {
            syncDrafts()
        }
        .task(id: artistName) {
            await tracksViewModel.loadTracks(for: artistName)
        }
        .onChange(of: page) { _, _ in
            if !isEditing {
                syncDrafts()
            }
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
        .sheet(item: $pendingImageTarget) { target in
            SingleImagePicker { provider in
                handleEditableImageProvider(provider, for: target)
            }
        }
        .onDisappear {
            audioManager.stop()
        }
    }

    private func handleEditableImageProvider(
        _ provider: NSItemProvider?,
        for target: ArtistPageEditableImageTarget
    ) {
        pendingImageTarget = nil

        guard let provider else {
            return
        }

        Task {
            await MainActor.run {
                activeImageUploadTarget = target
            }
            do {
                let previousURL = currentEditableImageURL(for: target)
                let data = try await PickedImageUploadPreparation.normalizedJPEGData(from: provider)
                let url = try await editableImageUploadService.uploadImageData(data)
                if previousURL != url {
                    try? await editableImageUploadService.deleteImage(at: previousURL)
                }
                await MainActor.run {
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

        Task {
            do {
                try await editableImageUploadService.deleteImage(at: previousURL)
                await MainActor.run {
                    showToast("Bild entfernt.", style: .success)
                }
            } catch {
                await MainActor.run {
                    showToast("Bild wurde entfernt. Alter Upload konnte nicht geloescht werden: \(error.localizedDescription)", style: .error)
                }
            }
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 18) {
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

                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 8) {
                        ArtistHeroTag(
                            text: brand.displayTitle,
                            background: .white.opacity(0.14)
                        )

                        if page.hasCustomPresentation {
                            ArtistHeroTag(
                                text: "Live",
                                background: AppColors.spotify(for: colorScheme).opacity(0.84)
                            )
                        }

                        Spacer(minLength: 0)

                        if !tracksViewModel.tracks.isEmpty {
                            ArtistHeroTag(
                                text: "\(tracksViewModel.tracks.count) Songs",
                                background: .black.opacity(0.32)
                            )
                        }
                    }

                    Spacer(minLength: 0)

                    HStack(alignment: .bottom, spacing: 16) {
                        ArtistPageAvatar(
                            imageURL: page.profileImageURL,
                            fallbackText: page.artistName,
                            size: 96,
                            colorScheme: colorScheme
                        )
                        .shadow(color: .black.opacity(0.22), radius: 18, y: 10)

                        VStack(alignment: .leading, spacing: 6) {
                            Text(page.artistName)
                                .font(.system(size: 30, weight: .black, design: .rounded))
                                .foregroundColor(.white)
                                .shadow(color: .black.opacity(0.28), radius: 14, y: 6)

                            Text(page.tagline ?? "\(brand.displayTitle) Profil")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(.white.opacity(0.86))
                                .lineLimit(2)

                            if let latestReleaseText {
                                Text("Latest \(latestReleaseText)")
                                    .font(.caption.weight(.bold))
                                    .foregroundColor(.white.opacity(0.76))
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding(20)
            }

            VStack(alignment: .leading, spacing: 14) {
                Text(page.bio ?? "Noch keine Artist-Seite hinterlegt. Owner oder zugewiesene Editoren koennen hier eine repraesentative Kurzbeschreibung anlegen.")
                    .font(.body)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                artistHighlightsRow

                if !socialLinks.isEmpty {
                    artistCTAButtons
                }

                if !page.editorUids.isEmpty {
                    ArtistPageBadge(
                        text: "\(page.editorUids.count) Editor\(page.editorUids.count == 1 ? "" : "en")",
                        colorScheme: colorScheme
                    )
                }
            }
            .padding(18)
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
            if let heroImageURL = page.heroImageURL, let url = URL(string: heroImageURL) {
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

    private var artistCTAButtons: some View {
        HStack(spacing: 10) {
            ForEach(socialLinks.prefix(3)) { link in
                Button {
                    if link.kind == .youtube {
                        selectedYouTubeItem = SkydownYouTubeVideoItem(
                            id: "artist-\(page.slug)-hero-youtube",
                            title: page.artistName,
                            subtitle: "Videos & Releases",
                            urlString: link.url
                        )
                    } else if let url = URL(string: link.url) {
                        openURL(url)
                    }
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: link.systemImage)
                            .font(.subheadline.weight(.bold))

                        Text(link.title)
                            .font(.subheadline.weight(.semibold))
                            .lineLimit(1)
                    }
                    .foregroundColor(link.foregroundColor)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(link.backgroundColor)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var artistHighlightsRow: some View {
        HStack(spacing: 10) {
            ArtistInfoMetric(
                title: "Artist",
                value: brand.displayTitle,
                colorScheme: colorScheme
            )

            if !tracksViewModel.tracks.isEmpty {
                ArtistInfoMetric(
                    title: "Songs",
                    value: "\(tracksViewModel.tracks.count)",
                    colorScheme: colorScheme
                )
            }

            if let latestReleaseText {
                ArtistInfoMetric(
                    title: "Latest",
                    value: latestReleaseText,
                    colorScheme: colorScheme
                )
            }

            if linkCount > 0 {
                ArtistInfoMetric(
                    title: "Links",
                    value: "\(linkCount)",
                    colorScheme: colorScheme
                )
            }
        }
    }

    private var spotlightCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Spotlight")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(page.tagline ?? "\(page.artistName) auf \(brand.displayTitle) entdecken.")
                .font(.title3.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))

            HStack(spacing: 8) {
                ArtistPageBadge(
                    text: "\(tracksViewModel.tracks.count) Song\(tracksViewModel.tracks.count == 1 ? "" : "s")",
                    colorScheme: colorScheme
                )
                if let latestReleaseText {
                    ArtistPageBadge(
                        text: latestReleaseText,
                        colorScheme: colorScheme
                    )
                }
                if !socialLinks.isEmpty {
                    ArtistPageBadge(
                        text: "\(socialLinks.count) Links",
                        colorScheme: colorScheme
                    )
                }
            }

            if let spotlightTrack {
                HStack(alignment: .top, spacing: 14) {
                    AsyncImage(url: URL(string: spotlightTrack.artworkUrl100 ?? "")) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    }
                    .frame(width: 82, height: 82)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Jetzt antesten")
                            .font(.caption.weight(.bold))
                            .foregroundColor(AppColors.spotify(for: colorScheme))

                        Text(spotlightTrack.trackName)
                            .font(.headline.weight(.bold))
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(spotlightTrack.collectionName ?? page.artistName)
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        Text("Direkt unten mit Preview oder Spotify Player weiter.")
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
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
        VStack(alignment: .leading, spacing: 14) {
            Text("Top Songs")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if tracksViewModel.isLoading {
                ProgressView("Songs werden geladen ...")
            } else if let errorMessage = tracksViewModel.errorMessage {
                Text(errorMessage)
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else if topTracks.isEmpty {
                Text("Fuer \(page.artistName) sind gerade noch keine Songs hinterlegt.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else {
                Text("Direkt mit Preview oder Spotify Player in den Sound rein.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))

                ForEach(topTracks) { track in
                    TrackView(
                        track: track,
                        audioManager: audioManager,
                        isSelected: selectedTrackID == track.trackId
                    ) {
                        selectedTrackID = track.trackId
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

    private var linksCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Links")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if socialLinks.isEmpty {
                Text("Noch keine Links hinterlegt.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            } else {
                ForEach(socialLinks) { link in
                    Button {
                        if link.kind == .youtube {
                            selectedYouTubeItem = SkydownYouTubeVideoItem(
                                id: "artist-\(page.slug)-links-youtube",
                                title: page.artistName,
                                subtitle: link.subtitle,
                                urlString: link.url
                            )
                        } else if let url = URL(string: link.url) {
                            openURL(url)
                        }
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: link.systemImage)
                                .font(.headline.weight(.bold))
                                .foregroundColor(link.tint)
                                .frame(width: 28)

                            VStack(alignment: .leading, spacing: 3) {
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
                    .padding(14)
                    .background(link.backgroundColor.opacity(colorScheme == .dark ? 0.18 : 0.12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(link.tint.opacity(0.18), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
        VStack(alignment: .leading, spacing: 12) {
            Text("Artist-Seite bearbeiten")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Bilder und Links kannst du hier neu anlegen, ersetzen oder entfernen. Live wird die Artist-Seite erst, wenn du oben auf `Fertig` tippst.")
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ArtistPageInputField(
                title: "Kurzzeile",
                text: $taglineDraft,
                placeholder: "z. B. Rap, Melodic, ZweiZwei",
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
                onPickImage: { pendingImageTarget = .profile },
                onRemoveImage: { removeEditableImage(for: .profile) }
            )

            EditableImageField(
                title: "Hero-Bild",
                imageURL: $heroImageURLDraft,
                colorScheme: colorScheme,
                isUploading: activeImageUploadTarget == .hero,
                uploadStatusText: "Hero-Bild wird fuer die Artist-Seite uebernommen.",
                onPickImage: { pendingImageTarget = .hero },
                onRemoveImage: { removeEditableImage(for: .hero) }
            )

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
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.spotify(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius
        )
    }

    private var socialLinks: [ArtistPageSocialLink] {
        var links: [ArtistPageSocialLink] = []

        if let instagramURL = page.instagramURL?.trimmingCharacters(in: .whitespacesAndNewlines), !instagramURL.isEmpty {
            links.append(
                ArtistPageSocialLink(
                    kind: .instagram,
                    title: "Instagram",
                    subtitle: page.artistName,
                    url: instagramURL,
                    systemImage: "camera.fill",
                    tint: AppColors.instagramStart(for: colorScheme),
                    backgroundColor: AppColors.instagramStart(for: colorScheme),
                    foregroundColor: .white
                )
            )
        }

        if let spotifyURL = page.spotifyURL?.trimmingCharacters(in: .whitespacesAndNewlines), !spotifyURL.isEmpty {
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

        if let youtubeURL = page.youtubeURL?.trimmingCharacters(in: .whitespacesAndNewlines), !youtubeURL.isEmpty {
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
        instagramURLDraft = page.instagramURL ?? ""
        spotifyURLDraft = page.spotifyURL ?? ""
        youtubeURLDraft = page.youtubeURL ?? ""
    }

    private func savePage() async {
        guard canEdit else { return }
        isSaving = true
        defer { isSaving = false }

        do {
            try await store.save(
                ArtistPage(
                    id: page.slug,
                    brand: page.brand,
                    artistName: page.artistName,
                    tagline: taglineDraft.trimmedNilIfEmpty,
                    bio: bioDraft.trimmedNilIfEmpty,
                    profileImageURL: profileImageURLDraft.trimmedNilIfEmpty,
                    heroImageURL: heroImageURLDraft.trimmedNilIfEmpty,
                    instagramURL: instagramURLDraft.trimmedNilIfEmpty,
                    spotifyURL: spotifyURLDraft.trimmedNilIfEmpty,
                    youtubeURL: youtubeURLDraft.trimmedNilIfEmpty,
                    editorUids: page.editorUids,
                    createdAt: page.createdAt,
                    updatedAt: .now,
                    isPlaceholder: false
                )
            )
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

private enum ArtistPageEditableImageTarget: String, Identifiable {
    case profile
    case hero

    var id: String { rawValue }
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
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundColor(AppColors.text(for: colorScheme))
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(
                Capsule()
                    .fill(AppColors.secondaryBackground(for: colorScheme))
            )
    }
}

private struct ArtistHeroTag: View {
    let text: String
    let background: Color

    var body: some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundColor(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .background(background, in: Capsule())
    }
}

private struct ArtistInfoMetric: View {
    let title: String
    let value: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
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
            RoundedRectangle(cornerRadius: 16, style: .continuous)
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
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(placeholder, text: $text)
                .textInputAutocapitalization(.never)
                .padding(.horizontal, 14)
                .padding(.vertical, 14)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }
}

private struct ArtistPageMultilineInput: View {
    let title: String
    @Binding var text: String
    let placeholder: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            ZStack(alignment: .topLeading) {
                if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(placeholder)
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .padding(.horizontal, 18)
                        .padding(.vertical, 16)
                }

                TextEditor(text: $text)
                    .scrollContentBackground(.hidden)
                    .frame(minHeight: 120)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(AppColors.secondaryBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
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
