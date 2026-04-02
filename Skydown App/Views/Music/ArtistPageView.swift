import SwiftUI

struct ArtistPageView: View {
    @ObservedObject private var authManager: AuthManager
    @ObservedObject private var store: ArtistPagesStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL

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

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    heroCard
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
        .onAppear {
            syncDrafts()
        }
        .onChange(of: page) { _, _ in
            if !isEditing {
                syncDrafts()
            }
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            ZStack(alignment: .bottomLeading) {
                heroVisual
                    .frame(height: 220)
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))

                LinearGradient(
                    colors: [.clear, Color.black.opacity(0.72)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))

                HStack(alignment: .bottom, spacing: 14) {
                    ArtistPageAvatar(
                        imageURL: page.profileImageURL,
                        fallbackText: page.artistName,
                        size: 82,
                        colorScheme: colorScheme
                    )

                    VStack(alignment: .leading, spacing: 6) {
                        Text(page.artistName)
                            .font(.system(size: 28, weight: .black, design: .rounded))
                            .foregroundColor(.white)

                        Text(page.tagline ?? "\(brand.displayTitle) Artist")
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(.white.opacity(0.86))
                    }
                }
                .padding(20)
            }

            Text(page.bio ?? "Noch keine Artist-Seite hinterlegt. Owner oder zugewiesene Editoren koennen hier eine repraesentative Kurzbeschreibung anlegen.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if !page.editorUids.isEmpty {
                ArtistPageBadge(
                    text: "\(page.editorUids.count) Editor\(page.editorUids.count == 1 ? "" : "en")",
                    colorScheme: colorScheme
                )
            }
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
                        if let url = URL(string: link.url) {
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
                    .background(AppColors.secondaryBackground(for: colorScheme))
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

            ArtistPageInputField(
                title: "Profilbild-URL",
                text: $profileImageURLDraft,
                placeholder: "https://...",
                colorScheme: colorScheme
            )

            ArtistPageInputField(
                title: "Hero-Bild-URL",
                text: $heroImageURLDraft,
                placeholder: "https://...",
                colorScheme: colorScheme
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
                    title: "Instagram",
                    subtitle: page.artistName,
                    url: instagramURL,
                    systemImage: "camera.fill",
                    tint: AppColors.instagramStart(for: colorScheme)
                )
            )
        }

        if let spotifyURL = page.spotifyURL?.trimmingCharacters(in: .whitespacesAndNewlines), !spotifyURL.isEmpty {
            links.append(
                ArtistPageSocialLink(
                    title: "Spotify",
                    subtitle: "Artist Profil",
                    url: spotifyURL,
                    systemImage: "music.note",
                    tint: AppColors.spotify(for: colorScheme)
                )
            )
        }

        if let youtubeURL = page.youtubeURL?.trimmingCharacters(in: .whitespacesAndNewlines), !youtubeURL.isEmpty {
            links.append(
                ArtistPageSocialLink(
                    title: "YouTube",
                    subtitle: "Videos & Releases",
                    url: youtubeURL,
                    systemImage: "play.rectangle.fill",
                    tint: AppColors.youtube(for: colorScheme)
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

private struct ArtistPageSocialLink: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let url: String
    let systemImage: String
    let tint: Color
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
