import SwiftUI

struct ProfileView: View {
    @ObservedObject private var authManager: AuthManager
    @StateObject private var viewModel: UserProfileViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @State private var pendingImagePickerTarget: ProfileImagePickerTarget?

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
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .task {
            await authManager.refreshCurrentUser()
            viewModel.configure(user: authManager.userSession)
        }
        .onReceive(authManager.$userSession) { user in
            viewModel.configure(user: user)
        }
        .sheet(item: $pendingImagePickerTarget) { target in
            SingleImagePicker { provider in
                handlePickedImageProvider(provider, for: target)
            }
        }
    }

    private func handlePickedImageProvider(
        _ provider: NSItemProvider?,
        for target: ProfileImagePickerTarget
    ) {
        guard let provider else {
            pendingImagePickerTarget = nil
            return
        }

        Task {
            do {
                let data = try await PickedImageUploadPreparation.normalizedJPEGData(from: provider)
                switch target {
                case .avatar:
                    await viewModel.uploadAvatar(data: data)
                case .gallery:
                    await viewModel.uploadGalleryImage(data: data)
                }
            } catch {
                await MainActor.run {
                    viewModel.reportUploadError(error)
                }
            }

            await MainActor.run {
                pendingImagePickerTarget = nil
            }
        }
    }

    private var profileBackdropURL: URL? {
        if let value = viewModel.currentUser?.profileImageURL?.trimmingCharacters(in: .whitespacesAndNewlines),
           let url = URL(string: value),
           !value.isEmpty {
            return url
        }

        if let fallback = viewModel.filteredItems.first?.thumbnailURL ?? viewModel.filteredItems.first?.mediaURL,
           let url = URL(string: fallback) {
            return url
        }

        return nil
    }

    private var instagramURL: URL? {
        guard let handle = viewModel.currentUser?.instagramHandle?.trimmingCharacters(in: .whitespacesAndNewlines),
              !handle.isEmpty else {
            return nil
        }

        if handle.contains("instagram.com"), let url = URL(string: handle) {
            return url
        }

        return URL(string: "https://www.instagram.com/\(handle.trimmingPrefix("@"))")
    }

    private var whatsAppURL: URL? {
        guard let raw = viewModel.currentUser?.whatsApp?.trimmingCharacters(in: .whitespacesAndNewlines),
              !raw.isEmpty else {
            return nil
        }

        if raw.lowercased().hasPrefix("http"), let url = URL(string: raw) {
            return url
        }

        let digits = raw.filter(\.isNumber)
        guard !digits.isEmpty else { return nil }
        return URL(string: "https://wa.me/\(digits)")
    }

    private var roleTitle: String {
        switch viewModel.currentUser?.resolvedRole {
        case .owner:
            return "Owner"
        case .admin:
            return "Admin"
        case .subadmin:
            return "Creator"
        default:
            return "User"
        }
    }

    private var planTitle: String {
        switch viewModel.currentUser?.resolvedQuotaPlan {
        case .ownerUnlimited:
            return "Unlimited"
        case .internalTeam:
            return "Team"
        case .creator:
            return "Creator"
        case .studio:
            return "Studio"
        default:
            return "Free"
        }
    }

    private var profileHeader: some View {
        let isUploadingAvatar = viewModel.isUploadingAvatar
        let isUploadingMedia = viewModel.isUploadingMedia

        return ZStack(alignment: .topTrailing) {
            RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            AppColors.cardBackground(for: colorScheme),
                            AppColors.secondaryBackground(for: colorScheme)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            if let profileBackdropURL {
                AsyncImage(url: profileBackdropURL) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    Color.clear
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))
            }

            RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color.black.opacity(0.18),
                            Color.black.opacity(0.54),
                            Color.black.opacity(0.82)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )

            VStack(alignment: .leading, spacing: 18) {
                HStack(spacing: 10) {
                    ProfileMetaPill(title: "Rolle", value: roleTitle)
                    ProfileMetaPill(title: "Plan", value: planTitle)
                }

                Spacer(minLength: 44)

                HStack(alignment: .bottom, spacing: 16) {
                    ProfileAvatarView(
                        imageURL: viewModel.currentUser?.profileImageURL,
                        fallbackText: viewModel.currentUser?.username ?? "G",
                        size: 96,
                        colorScheme: colorScheme
                    )

                    VStack(alignment: .leading, spacing: 8) {
                        Text(viewModel.currentUser?.username ?? "Profil")
                            .font(.system(size: 30, weight: .black, design: .rounded))
                            .foregroundColor(.white)

                        if let tagline = viewModel.currentUser?.profileTagline, !tagline.isEmpty {
                            Text(tagline)
                                .font(.subheadline.weight(.bold))
                                .foregroundColor(.white.opacity(0.88))
                        }

                        if let bio = viewModel.currentUser?.profileBio, !bio.isEmpty {
                            Text(bio)
                                .font(.footnote.weight(.medium))
                                .foregroundColor(.white.opacity(0.72))
                                .lineLimit(3)
                        }

                        HStack(spacing: 8) {
                            if let instagramURL {
                                ProfileLinkPill(
                                    title: "Instagram",
                                    systemImage: "camera.fill",
                                    colors: [
                                        AppColors.instagramStart(for: colorScheme),
                                        AppColors.instagramEnd(for: colorScheme)
                                    ]
                                ) {
                                    openURL(instagramURL)
                                }
                            }

                            if let whatsAppURL {
                                ProfileLinkPill(
                                    title: "WhatsApp",
                                    systemImage: "message.fill",
                                    colors: [
                                        AppColors.spotify(for: colorScheme),
                                        AppColors.accentMystic(for: colorScheme)
                                    ]
                                ) {
                                    openURL(whatsAppURL)
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                HStack(spacing: 10) {
                    ProfileMetaPill(title: "Bilder", value: "\(viewModel.imageCount)")
                    ProfileMetaPill(title: "Links", value: "\(instagramURL == nil ? 0 : 1 + (whatsAppURL == nil ? 0 : 1))")
                    ProfileMetaPill(title: "Status", value: viewModel.canEditCurrentProfile ? "Live" : "Public")
                }
            }
            .padding(22)
            .frame(maxWidth: .infinity, minHeight: 286, alignment: .bottomLeading)

            if viewModel.canEditCurrentProfile {
                VStack(alignment: .trailing, spacing: 10) {
                    Button(viewModel.isEditing ? "Fertig" : "Bearbeiten") {
                        viewModel.setEditing(!viewModel.isEditing)
                    }
                    .buttonStyle(.plain)
                    .modifier(
                        ProfileActionCapsuleModifier(
                            tint: AppColors.accentMystic(for: colorScheme),
                            textColor: .white
                        )
                    )

                    Button {
                        pendingImagePickerTarget = .avatar
                    } label: {
                        ProfileActionCapsuleLabel(
                            title: isUploadingAvatar ? "Avatar..." : "Avatar",
                            systemImage: "camera.fill"
                        )
                    }
                    .buttonStyle(.plain)
                    .modifier(
                        ProfileActionCapsuleModifier(
                            tint: AppColors.accent(for: colorScheme),
                            textColor: .white
                        )
                    )

                    Button {
                        pendingImagePickerTarget = .gallery
                    } label: {
                        ProfileActionCapsuleLabel(
                            title: isUploadingMedia ? "Laedt..." : "Bild",
                            systemImage: "photo.badge.plus"
                        )
                    }
                    .buttonStyle(.plain)
                    .modifier(
                        ProfileActionCapsuleModifier(
                            tint: AppColors.cardBackground(for: colorScheme).opacity(0.92),
                            textColor: .white
                        )
                    )
                }
                .padding(18)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius, style: .continuous)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.22), radius: 18, y: 12)
    }

    private var mediaSection: some View {
        let isUploadingMedia = viewModel.isUploadingMedia

        return VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Galerie")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text("Persoenliche Bilder direkt aus deinem Profil.")
                        .font(.footnote.weight(.medium))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                if viewModel.canEditCurrentProfile {
                    Button {
                        pendingImagePickerTarget = .gallery
                    } label: {
                        ProfileCompactActionPill(
                            title: isUploadingMedia ? "Laedt..." : "Bild hinzufuegen",
                            systemImage: "photo.badge.plus",
                            colorScheme: colorScheme
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                }
            }

            if viewModel.filteredItems.isEmpty {
                ProfileGalleryEmptyState(
                    canEdit: viewModel.canEditCurrentProfile,
                    colorScheme: colorScheme
                )
            } else {
                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 2),
                    spacing: 12
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

    private var editSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Profil bearbeiten")
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text("Name, Kurzinfo und Kontaktpunkte wirken hier direkt auf dein Profil.")
                    .font(.footnote.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

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

private enum ProfileImagePickerTarget: String, Identifiable {
    case avatar
    case gallery

    var id: String { rawValue }
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

private struct ProfileMetaPill: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value)
                .font(.headline.weight(.bold))
                .foregroundColor(.white)
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(.white.opacity(0.68))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.white.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
    }
}

private struct ProfileLinkPill: View {
    let title: String
    let systemImage: String
    let colors: [Color]
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(.footnote.weight(.semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing))
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

private struct ProfileActionCapsuleLabel: View {
    let title: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .font(.headline.weight(.bold))
            Text(title)
                .font(.subheadline.weight(.bold))
        }
    }
}

private struct ProfileActionCapsuleModifier: ViewModifier {
    let tint: Color
    let textColor: Color

    func body(content: Content) -> some View {
        content
            .foregroundColor(textColor)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(tint)
            .clipShape(Capsule())
            .shadow(color: tint.opacity(0.22), radius: 14, y: 8)
    }
}

private struct ProfileCompactActionPill: View {
    let title: String
    let systemImage: String
    let colorScheme: ColorScheme

    var body: some View {
        Label(title, systemImage: systemImage)
            .font(.footnote.weight(.bold))
            .foregroundColor(.white)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(
                Capsule()
                    .fill(AppColors.accent(for: colorScheme))
            )
            .shadow(color: AppColors.accent(for: colorScheme).opacity(0.2), radius: 12, y: 8)
    }
}

private struct ProfileGalleryEmptyState: View {
    let canEdit: Bool
    let colorScheme: ColorScheme

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: "sparkles.rectangle.stack")
                .font(.system(size: 26, weight: .bold))
                .foregroundColor(AppColors.accent(for: colorScheme))
            Text(canEdit ? "Deine ersten Bilder machen das Profil direkt lebendig." : "Hier erscheinen Bilder aus dem Profil.")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .multilineTextAlignment(.center)
            Text(canEdit ? "Nutze den Upload oben rechts und fuelle dein Profil mit echten Eindruecken." : "Sobald Bilder hinterlegt sind, wird die Galerie hier sichtbar.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, 22)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
                    .aspectRatio(0.92, contentMode: .fit)

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
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.title)
                            .lineLimit(1)
                        if let caption = item.caption, !caption.isEmpty {
                            Text(caption)
                                .lineLimit(1)
                                .foregroundColor(.white.opacity(0.72))
                        }
                    }
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
