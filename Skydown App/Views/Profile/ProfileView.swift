import SwiftUI

struct ProfileView: View {
    @ObservedObject private var authManager: AuthManager
    @StateObject private var viewModel: UserProfileViewModel
    @ObservedObject private var legalContentStore = LegalContentStore.shared
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @State private var pendingImagePickerTarget: ProfileImagePickerTarget?
    @State private var galleryPreviewTarget: ProfileGalleryPreviewTarget?
    private let isUITestMode = ProcessInfo.processInfo.arguments.contains("-ui_test")
        || ProcessInfo.processInfo.arguments.contains("-ui_test_role_matrix")
        || ProcessInfo.processInfo.arguments.contains("-ui_test_profile_crud")
    private let onOpenSettings: (() -> Void)?

    init(
        authManager: AuthManager,
        startsInEditMode: Bool = false,
        onOpenSettings: (() -> Void)? = nil
    ) {
        _authManager = ObservedObject(wrappedValue: authManager)
        _viewModel = StateObject(
            wrappedValue: UserProfileViewModel(
                authManager: authManager,
                startsInEditMode: startsInEditMode
            )
        )
        self.onOpenSettings = onOpenSettings
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    profileHeader
                    personalDashboardSection
                    quickActionsSection
                    personalHistorySection

                    if let uploadStatus = currentUploadStatus {
                        ProfileUploadStatusCard(
                            title: uploadStatus.title,
                            detail: uploadStatus.detail,
                            colorScheme: colorScheme
                        )
                    }

                    mediaSection
                    trustSection

                    if viewModel.isEditing && viewModel.canEditCurrentProfile {
                        editSection
                    }

                    if isUITestMode && viewModel.canEditCurrentProfile {
                        uiTestActionsSection
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
        .fullScreenCover(item: $galleryPreviewTarget) { target in
            ProfileGalleryMediaViewerSheet(
                item: target.item
            )
        }
    }

    private func handlePickedImageProvider(
        _ temporaryFileURL: URL?,
        for target: ProfileImagePickerTarget
    ) {
        pendingImagePickerTarget = nil

        guard let temporaryFileURL else {
            return
        }

        Task {
            do {
                defer { try? FileManager.default.removeItem(at: temporaryFileURL) }
                let data = try await PickedImageUploadPreparation.normalizedJPEGData(fromTemporaryFileURL: temporaryFileURL)
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

    private var currentUploadStatus: (title: String, detail: String)? {
        if viewModel.isUploadingAvatar {
            return (
                "Profilbild wird hochgeladen",
                "Dein Avatar wird gerade vorbereitet, hochgeladen und direkt im Profil uebernommen."
            )
        }

        if viewModel.isUploadingMedia {
            return (
                "Galeriebild wird hochgeladen",
                "Das Bild landet gleich in deiner Galerie und wird danach automatisch angezeigt."
            )
        }

        return nil
    }

    private func localized(_ key: String, _ fallback: String) -> String {
        AppLocalized.text(key, fallback: fallback)
    }

    private var aiSystemStatusTitle: String {
        guard let user = viewModel.currentUser else { return "Gastmodus" }
        if !user.aiAccessEnabled { return "KI pausiert" }
        if user.hasActiveAISubscription { return "Premium aktiv" }
        return "Basis aktiv"
    }

    private var membershipStatusTitle: String {
        guard let user = viewModel.currentUser else { return "Nicht angemeldet" }
        if let plan = user.resolvedAISubscriptionPlan {
            return "Plan \(plan.rawValue.replacingOccurrences(of: "_", with: " ").capitalized)"
        }
        return "Plan \(planTitle)"
    }

    private var latestGalleryDateLabel: String {
        guard let latest = viewModel.filteredItems.sorted(by: { $0.createdAt > $1.createdAt }).first else {
            return "Noch keine Aktivitaet"
        }
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        formatter.locale = Locale(identifier: "de_DE")
        return formatter.string(from: latest.createdAt)
    }

    private var registrationDateLabel: String {
        guard let user = viewModel.currentUser else { return "-" }
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        formatter.locale = Locale(identifier: "de_DE")
        return formatter.string(from: user.registrationDate)
    }

    private var profileHeader: some View {
        let isUploadingAvatar = viewModel.isUploadingAvatar
        let isUploadingMedia = viewModel.isUploadingMedia
        let isUploadingImageFlow = isUploadingAvatar || isUploadingMedia

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
                        .accessibilityIdentifier("profile.role_pill")
                    ProfileMetaPill(title: "Plan", value: planTitle)
                        .accessibilityIdentifier("profile.plan_pill")
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
                    .skydownTactileAction()
                    .modifier(
                        ProfileActionCapsuleModifier(
                            tint: AppColors.accentMystic(for: colorScheme),
                            textColor: .white
                        )
                    )
                        .accessibilityIdentifier("profile.edit.toggle")

                    Button {
                        pendingImagePickerTarget = .avatar
                    } label: {
                        ProfileActionCapsuleLabel(
                            title: isUploadingAvatar ? "Avatar..." : "Avatar",
                            systemImage: "camera.fill"
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .modifier(
                        ProfileActionCapsuleModifier(
                            tint: AppColors.accent(for: colorScheme),
                            textColor: .white
                        )
                    )
                    .disabled(isUploadingImageFlow)
                        .accessibilityIdentifier("profile.avatar.upload")

                    if let currentAvatar = viewModel.currentUser?.profileImageURL?.trimmingCharacters(in: .whitespacesAndNewlines),
                       !currentAvatar.isEmpty {
                        Button {
                            Task {
                                await viewModel.deleteAvatar()
                            }
                        } label: {
                            ProfileActionCapsuleLabel(
                                title: isUploadingAvatar ? "Entferne..." : "Avatar loeschen",
                                systemImage: "trash.fill"
                            )
                        }
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                        .modifier(
                            ProfileActionCapsuleModifier(
                                tint: Color.red.opacity(0.84),
                                textColor: .white
                            )
                        )
                        .disabled(isUploadingImageFlow)
                            .accessibilityIdentifier("profile.avatar.delete")
                    }

                    Button {
                        pendingImagePickerTarget = .gallery
                    } label: {
                        ProfileActionCapsuleLabel(
                            title: isUploadingMedia ? "Laedt..." : "Bild",
                            systemImage: "photo.badge.plus"
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .modifier(
                        ProfileActionCapsuleModifier(
                            tint: AppColors.cardBackground(for: colorScheme).opacity(0.92),
                            textColor: .white
                        )
                    )
                    .disabled(isUploadingImageFlow)
                        .accessibilityIdentifier("profile.gallery.upload")
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
                    .disabled(isUploadingMedia || viewModel.isUploadingAvatar)
                    .accessibilityIdentifier("profile.gallery.add")
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
                        ProfileMediaGridTile(
                            item: item,
                            colorScheme: colorScheme,
                            canDelete: viewModel.canEditCurrentProfile,
                            onOpen: {
                                galleryPreviewTarget = ProfileGalleryPreviewTarget(item: item)
                            }
                        ) {
                            Task {
                                await viewModel.deleteGalleryItem(item)
                            }
                        }
                    }
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentHighlight(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 9,
            shadowYOffset: 4
        )
    }

    private var personalDashboardSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(localized("profile.dashboard.title", "Personal Dashboard"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(localized("profile.dashboard.subtitle", "Your current system status at a glance."))
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 10) {
                ProfileMetricCard(title: localized("profile.dashboard.membership", "Membership"), value: membershipStatusTitle, colorScheme: colorScheme)
                ProfileMetricCard(title: localized("profile.dashboard.ai", "AI"), value: aiSystemStatusTitle, colorScheme: colorScheme)
            }
            HStack(spacing: 10) {
                ProfileMetricCard(title: localized("profile.dashboard.gallery", "Gallery"), value: "\(viewModel.imageCount) \(localized("profile.images", "images"))", colorScheme: colorScheme)
                ProfileMetricCard(title: localized("profile.dashboard.role", "Role"), value: roleTitle, colorScheme: colorScheme)
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accent(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 8,
            shadowYOffset: 4
        )
    }

    private var quickActionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(localized("profile.quick_actions.title", "Quick Actions"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(localized("profile.quick_actions.subtitle", "Your most important personal actions in one calm block."))
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 10) {
                quickActionButton(
                    title: viewModel.isEditing ? localized("profile.done", "Done") : localized("profile.edit", "Edit profile"),
                    systemImage: "square.and.pencil",
                    emphasis: .primary
                ) {
                    viewModel.setEditing(!viewModel.isEditing)
                }
                quickActionButton(
                    title: localized("profile.upload_image", "Upload image"),
                    systemImage: "photo.badge.plus",
                    emphasis: .primary
                ) {
                    pendingImagePickerTarget = .gallery
                }
            }

            HStack(spacing: 10) {
                quickActionButton(
                    title: localized("profile.membership", "Membership"),
                    systemImage: "sparkles",
                    emphasis: .secondary
                ) {
                    onOpenSettings?()
                    dismiss()
                }
                quickActionButton(
                    title: localized("profile.settings", "Settings"),
                    systemImage: "gearshape.fill",
                    emphasis: .secondary
                ) {
                    onOpenSettings?()
                    dismiss()
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentMystic(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 8,
            shadowYOffset: 4
        )
    }

    private var personalHistorySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(localized("profile.history.title", "Personal History"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(localized("profile.history.subtitle", "A compact view of your latest profile and content activity."))
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ProfileHistoryRow(title: localized("profile.history.latest_created", "Latest created"), value: latestGalleryDateLabel, colorScheme: colorScheme)
            ProfileHistoryRow(title: localized("profile.history.member_since", "Member since"), value: registrationDateLabel, colorScheme: colorScheme)
            ProfileHistoryRow(title: localized("profile.history.current_plan", "Current plan"), value: membershipStatusTitle, colorScheme: colorScheme)
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentHighlight(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 8,
            shadowYOffset: 4
        )
    }

    private var trustSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(localized("profile.trust.title", "Trust / Account"))
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))
            Text(localized("profile.trust.subtitle", "Support, privacy and account controls in one place."))
                .font(.footnote.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Button {
                let support = legalContentStore.settings.resolvedSupportEmail.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !support.isEmpty, let url = URL(string: "mailto:\(support)") else { return }
                openURL(url)
            } label: {
                Label(localized("profile.support_contact", "Contact support"), systemImage: "envelope.fill")
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.bordered)
            .skydownInteractiveFeedback()

            quickActionButton(
                title: localized("profile.privacy_account", "Privacy & account"),
                systemImage: "lock.shield.fill",
                emphasis: .secondary
            ) {
                onOpenSettings?()
                dismiss()
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentMystic(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 8,
            shadowYOffset: 4
        )
    }

    private enum QuickActionEmphasis {
        case primary
        case secondary
    }

    private func quickActionButton(
        title: String,
        systemImage: String,
        emphasis: QuickActionEmphasis,
        action: @escaping () -> Void
    ) -> some View {
        Group {
            if emphasis == .primary {
                Button(action: action) {
                    Label(title, systemImage: systemImage)
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
                .skydownInteractiveFeedback()
            } else {
                Button(action: action) {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 9)
                }
                .buttonStyle(.bordered)
                .tint(AppColors.accent(for: colorScheme))
                .skydownInteractiveFeedback()
            }
        }
    }

    private var editSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Profil bearbeiten")
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text("Name, Kurzinfo, Kontakt — live im Profil.")
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

                Toggle(isOn: $viewModel.aiAccessEnabledDraft) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("KI fuer mein Konto aktiv")
                            .font(.subheadline.weight(.semibold))
                        Text("Aus = Bot, Visuals, Agent pausiert.")
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }
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
            .skydownTactileAction()
            .disabled(viewModel.isSavingProfile)
            .accessibilityIdentifier("profile.edit.save")
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentMystic(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 9,
            shadowYOffset: 4
        )
    }

    private var uiTestActionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("UI Test")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Nur sichtbar bei UI-Tests.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 10) {
                Button {
                    Task {
                        await viewModel.uploadAvatar(data: ProfileUITestFixtures.sampleJPEG)
                    }
                } label: {
                    Text("Avatar Fixture")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
                .accessibilityIdentifier("ui_test.profile.upload_avatar_fixture")
                .disabled(viewModel.isUploadingAvatar || viewModel.isUploadingMedia)

                Button {
                    Task {
                        await viewModel.uploadGalleryImage(data: ProfileUITestFixtures.sampleJPEG)
                    }
                } label: {
                    Text("Gallery Fixture")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(AppColors.accentMystic(for: colorScheme))
                .accessibilityIdentifier("ui_test.profile.upload_gallery_fixture")
                .disabled(viewModel.isUploadingAvatar || viewModel.isUploadingMedia)
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentHighlight(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 10,
            shadowYOffset: 6
        )
        .accessibilityIdentifier("ui_test.profile.section")
    }
}

private enum ProfileUITestFixtures {
    // 1x1 JPEG, tiny payload for upload tests.
    static let sampleJPEG: Data = Data(base64Encoded:
        "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAALCAAQABABAREA/8QAFQABAQAAAAAAAAAAAAAAAAAAAAb/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAGoAP/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAQUCcf/EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQMBAT8BIf/EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQIBAT8BIf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEABj8Cf//Z"
    ) ?? Data()
}

private enum ProfileImagePickerTarget: String, Identifiable {
    case avatar
    case gallery

    var id: String { rawValue }
}

private struct ProfileUploadStatusCard: View {
    let title: String
    let detail: String
    let colorScheme: ColorScheme

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
            ProgressView()
                .tint(AppColors.accent(for: colorScheme))

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                Text(detail)
                    .font(.footnote)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer(minLength: 0)
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: AppColors.accentHighlight(for: colorScheme),
            cornerRadius: SkydownLayout.cardCornerRadius,
            shadowRadius: 10,
            shadowYOffset: 6
        )
    }
}

private struct ProfileMetricCard: View {
    let title: String
    let value: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Text(value)
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(AppColors.cardBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct ProfileHistoryRow: View {
    let title: String
    let value: String
    let colorScheme: ColorScheme

    var body: some View {
        HStack {
            Text(title)
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Spacer()
            Text(value)
                .font(.footnote.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
        }
        .padding(.vertical, 4)
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
    var canDelete: Bool = false
    let onOpen: () -> Void
    var onDelete: (() -> Void)?

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Button {
                onOpen()
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
            .accessibilityIdentifier("profile.gallery.item.\(item.id ?? "unknown")")

            if canDelete, let onDelete {
                Button(action: onDelete) {
                    Image(systemName: "trash.fill")
                        .font(.caption.weight(.bold))
                        .foregroundColor(.white)
                        .padding(10)
                        .background(Color.black.opacity(0.58))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .skydownTactileAction()
                .padding(10)
                .accessibilityIdentifier("profile.gallery.delete.\(item.id ?? "unknown")")
            }
        }
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

private struct ProfileGalleryPreviewTarget: Identifiable {
    let id = UUID()
    let item: ProfileGalleryItem
}

private struct ProfileGalleryMediaViewerSheet: View {
    let item: ProfileGalleryItem
    @Environment(\.dismiss) private var dismiss

    private var imageURL: URL? {
        URL(string: item.mediaURL)
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .top) {
                Color.black
                    .ignoresSafeArea()

                if let imageURL {
                    AsyncImage(url: imageURL) { image in
                        image
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .padding(.horizontal, 16)
                            .padding(.top, max(proxy.safeAreaInsets.top + 72, 96))
                            .padding(.bottom, max(proxy.safeAreaInsets.bottom + 24, 40))
                    } placeholder: {
                        ProgressView()
                            .tint(.white)
                    }
                } else {
                    VStack(spacing: 10) {
                        Image(systemName: "photo")
                            .font(.system(size: 44, weight: .bold))
                            .foregroundColor(.white.opacity(0.74))
                        Text("Bild konnte nicht geladen werden.")
                            .font(.headline)
                            .foregroundColor(.white.opacity(0.82))
                    }
                }

                VStack(spacing: 10) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.title)
                                .font(.headline.weight(.bold))
                                .foregroundColor(.white)
                                .lineLimit(2)

                            if let caption = item.caption, !caption.isEmpty {
                                Text(caption)
                                    .font(.subheadline)
                                    .foregroundColor(.white.opacity(0.72))
                                    .lineLimit(2)
                            }
                        }

                        Spacer()

                        Button(action: { dismiss() }, label: {
                            Label("Schliessen", systemImage: "xmark")
                                .font(.subheadline.weight(.bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 14)
                                .frame(height: 48)
                                .background(Color.black.opacity(0.42))
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(Color.white.opacity(0.22), lineWidth: 1)
                                )
                        })
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, max(proxy.safeAreaInsets.top, 12))
                    .zIndex(10)

                    Spacer()
                }
            }
        }
    }
}
