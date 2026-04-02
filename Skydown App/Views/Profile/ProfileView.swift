import PhotosUI
import SwiftUI

struct ProfileView: View {
    @ObservedObject private var authManager: AuthManager
    @StateObject private var viewModel: UserProfileViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @State private var avatarPickerItem: PhotosPickerItem?
    @State private var galleryImagePickerItem: PhotosPickerItem?

    init(
        authManager: AuthManager,
        startsInEditMode: Bool = false
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        _viewModel = StateObject(
            wrappedValue: UserProfileViewModel(
                authManager: authManager,
                startsInEditMode: startsInEditMode
            )
        )
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    profileHeader
                    mediaSection

                    if viewModel.isEditing && viewModel.canEditCurrentProfile {
                        editSection
                    }
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding)
            }
            .background(AppColors.screenGradient(for: colorScheme).ignoresSafeArea())
            .navigationTitle("Profil")
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Schliessen") {
                        dismiss()
                    }
                }

                if viewModel.canEditCurrentProfile {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(viewModel.isEditing ? "Fertig" : "Bearbeiten") {
                            viewModel.setEditing(!viewModel.isEditing)
                        }
                    }
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .task {
            viewModel.configure(user: authManager.userSession)
        }
        .onReceive(authManager.$userSession) { user in
            viewModel.configure(user: user)
        }
        .onChange(of: avatarPickerItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    await viewModel.uploadAvatar(data: data)
                }
                avatarPickerItem = nil
            }
        }
        .onChange(of: galleryImagePickerItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    await viewModel.uploadGalleryImage(data: data)
                }
                galleryImagePickerItem = nil
            }
        }
    }

    private var profileHeader: some View {
        let isUploadingAvatar = viewModel.isUploadingAvatar

        return VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 16) {
                ZStack(alignment: .bottomTrailing) {
                    ProfileAvatarView(
                        imageURL: viewModel.currentUser?.profileImageURL,
                        fallbackText: viewModel.currentUser?.username ?? "G",
                        size: 94,
                        colorScheme: colorScheme
                    )

                    if viewModel.canEditCurrentProfile {
                        PhotosPicker(
                            selection: $avatarPickerItem,
                            matching: .images
                        ) {
                            ZStack {
                                Circle()
                                    .fill(AppColors.accent(for: colorScheme))
                                    .frame(width: 30, height: 30)

                                if isUploadingAvatar {
                                    ProgressView()
                                        .tint(.white)
                                } else {
                                    Image(systemName: "camera.fill")
                                        .font(.caption.weight(.bold))
                                        .foregroundColor(.white)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text(viewModel.currentUser?.username ?? "Profil")
                        .font(.system(size: 28, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    if let tagline = viewModel.currentUser?.profileTagline, !tagline.isEmpty {
                        Text(tagline)
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }

                    if let bio = viewModel.currentUser?.profileBio, !bio.isEmpty {
                        Text(bio)
                            .font(.footnote.weight(.medium))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }

                    if let handle = viewModel.currentUser?.instagramHandle, !handle.isEmpty {
                        let normalizedHandle = handle.trimmingCharacters(in: .whitespacesAndNewlines)
                            .trimmingPrefix("@")
                        Button {
                            if let url = URL(string: "https://www.instagram.com/\(normalizedHandle)") {
                                openURL(url)
                            }
                        } label: {
                            Label("@\(normalizedHandle)", systemImage: "camera.fill")
                                .font(.footnote.weight(.semibold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(
                                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                                        .fill(
                                            LinearGradient(
                                                colors: [
                                                    AppColors.instagramStart(for: colorScheme),
                                                    AppColors.instagramEnd(for: colorScheme)
                                                ],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                        )
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                                        .stroke(Color.white.opacity(0.16), lineWidth: 1)
                                )
                        }
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                    }
                }

                Spacer()
            }

            ProfileStatPill(
                title: "Bilder",
                value: viewModel.imageCount,
                colorScheme: colorScheme
            )

        }
        .padding(SkydownLayout.heroPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accent(for: colorScheme),
            cornerRadius: SkydownLayout.heroCornerRadius,
            shadowRadius: 14,
            shadowYOffset: 8
        )
    }

    private var mediaSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Galerie")
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Spacer()
            }

            HStack(spacing: 10) {
                ForEach(ProfileMediaType.allCases) { type in
                    Button {
                        viewModel.selectedMediaType = type
                    } label: {
                        Label(type.title, systemImage: type.systemImage)
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(
                                viewModel.selectedMediaType == type ? .white : AppColors.text(for: colorScheme)
                            )
                            .padding(.horizontal, 12)
                            .padding(.vertical, 9)
                            .frame(maxWidth: .infinity)
                            .background(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .fill(
                                        viewModel.selectedMediaType == type
                                            ? AppColors.accent(for: colorScheme)
                                            : AppColors.secondaryBackground(for: colorScheme)
                                    )
                            )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                }
            }

            if viewModel.canEditCurrentProfile {
                uploadBar
            }

            if viewModel.filteredItems.isEmpty {
                Text("Noch nichts drin.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .padding(.vertical, 6)
            } else {
                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3),
                    spacing: 10
                ) {
                    ForEach(viewModel.filteredItems) { item in
                        ProfileMediaGridTile(item: item, colorScheme: colorScheme)
                    }
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentHighlight(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 12,
            shadowYOffset: 6
        )
    }

    private var uploadBar: some View {
        let isUploadingMedia = viewModel.isUploadingMedia
        let selectedMediaType = viewModel.selectedMediaType

        return HStack(spacing: 10) {
            PhotosPicker(
                selection: $galleryImagePickerItem,
                matching: .images
            ) {
                ProfileUploadButtonLabel(
                    title: isUploadingMedia && selectedMediaType == .image ? "Laedt" : "Bild",
                    systemImage: "photo.fill",
                    colorScheme: colorScheme
                )
            }
            .buttonStyle(.plain)
        }
    }

    private var editSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Profil bearbeiten")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            VStack(spacing: 12) {
                TextField("Benutzername", text: $viewModel.usernameDraft)
                    .textInputAutocapitalization(.never)
                    .padding(14)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                TextField("Kurzinfo", text: $viewModel.taglineDraft)
                    .padding(14)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                TextField("Instagram", text: $viewModel.instagramDraft)
                    .textInputAutocapitalization(.never)
                    .padding(14)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                TextField("WhatsApp", text: $viewModel.whatsAppDraft)
                    .padding(14)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                TextField("Bio", text: $viewModel.bioDraft, axis: .vertical)
                    .lineLimit(4...6)
                    .padding(14)
                    .background(AppColors.cardBackground(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }

            Button {
                Task {
                    await viewModel.saveProfile()
                }
            } label: {
                HStack {
                    Spacer()
                    if viewModel.isSavingProfile {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Text("Speichern")
                            .font(.headline.weight(.bold))
                    }
                    Spacer()
                }
                .padding(.vertical, 14)
                .background(AppColors.accent(for: colorScheme))
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(viewModel.isSavingProfile)
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentMystic(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 12,
            shadowYOffset: 6
        )
    }
}

private extension String {
    func trimmingPrefix(_ prefix: String) -> String {
        hasPrefix(prefix) ? String(dropFirst(prefix.count)) : self
    }
}

private struct ProfileAvatarView: View {
    let imageURL: String?
    let fallbackText: String
    let size: CGFloat
    let colorScheme: ColorScheme

    var body: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [
                            AppColors.accent(for: colorScheme).opacity(0.24),
                            AppColors.accentMystic(for: colorScheme).opacity(0.20)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            if let imageURL, let url = URL(string: imageURL) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    Text(String(fallbackText.prefix(1)).uppercased())
                        .font(.system(size: size * 0.34, weight: .black, design: .rounded))
                        .foregroundColor(AppColors.text(for: colorScheme))
                }
                .clipShape(Circle())
            } else {
                Text(String(fallbackText.prefix(1)).uppercased())
                    .font(.system(size: size * 0.34, weight: .black, design: .rounded))
                    .foregroundColor(AppColors.text(for: colorScheme))
            }
        }
        .frame(width: size, height: size)
        .overlay(
            Circle()
                .stroke(AppColors.accent(for: colorScheme).opacity(0.22), lineWidth: 1)
        )
    }
}

private struct ProfileStatPill: View {
    let title: String
    let value: Int
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("\(value)")
                .font(.headline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct ProfileUploadButtonLabel: View {
    let title: String
    let systemImage: String
    let colorScheme: ColorScheme

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: systemImage)
            Text(title)
        }
        .font(.footnote.weight(.semibold))
        .foregroundColor(AppColors.text(for: colorScheme))
        .frame(maxWidth: .infinity)
        .padding(.vertical, 11)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct ProfileMediaGridTile: View {
    let item: ProfileGalleryItem
    let colorScheme: ColorScheme
    @Environment(\.openURL) private var openURL

    var body: some View {
        Button {
            if let url = URL(string: item.mediaURL) {
                openURL(url)
            }
        } label: {
            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(AppColors.secondaryBackground(for: colorScheme))
                    .aspectRatio(1, contentMode: .fit)

                if item.mediaType == .image,
                   let thumb = URL(string: item.thumbnailURL ?? item.mediaURL) {
                    AsyncImage(url: thumb) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        tileFallback
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                } else {
                    tileFallback
                }

                LinearGradient(
                    colors: [.clear, .black.opacity(0.55)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                HStack(spacing: 6) {
                    Image(systemName: item.mediaType.systemImage)
                    Text(item.title)
                        .lineLimit(1)
                }
                .font(.caption2.weight(.bold))
                .foregroundColor(.white)
                .padding(8)
            }
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }

    private var tileFallback: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            AppColors.accent(for: colorScheme).opacity(0.18),
                            AppColors.accentMystic(for: colorScheme).opacity(0.16)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            Image(systemName: item.mediaType.systemImage)
                .font(.title2.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
        }
    }
}

private struct ProfileAudioRow: View {
    let item: ProfileGalleryItem
    let colorScheme: ColorScheme
    @Environment(\.openURL) private var openURL

    var body: some View {
        Button {
            if let url = URL(string: item.mediaURL) {
                openURL(url)
            }
        } label: {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                        .frame(width: 42, height: 42)
                    Image(systemName: "waveform")
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(item.caption ?? "Direkt aus deinem Profil.")
                        .font(.caption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                Image(systemName: "arrow.up.right.square")
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
            .padding(12)
            .background(AppColors.secondaryBackground(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}
