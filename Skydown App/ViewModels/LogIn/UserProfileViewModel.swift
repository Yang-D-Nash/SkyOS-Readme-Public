import Foundation
import FirebaseFirestore

@MainActor
final class UserProfileViewModel: ObservableObject {
    @Published private(set) var currentUser: User?
    @Published private(set) var galleryItems: [ProfileGalleryItem] = []
    @Published var selectedMediaType: ProfileMediaType = .image
    @Published var isEditing: Bool
    @Published var usernameDraft = ""
    @Published var whatsAppDraft = ""
    @Published var taglineDraft = ""
    @Published var bioDraft = ""
    @Published var instagramDraft = ""
    @Published var isSavingProfile = false
    @Published var isUploadingAvatar = false
    @Published var isUploadingMedia = false
    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastStyle: ToastStyle = .success

    private let authManager: AuthManager
    private let service: UserProfileServicing
    private var galleryListener: ListenerRegistration?

    init(
        authManager: AuthManager,
        service: UserProfileServicing = FirebaseUserProfileService(),
        startsInEditMode: Bool = false
    ) {
        self.authManager = authManager
        self.service = service
        self.isEditing = startsInEditMode
        configure(user: authManager.userSession)
    }

    deinit {
        galleryListener?.remove()
    }

    var filteredItems: [ProfileGalleryItem] {
        galleryItems.filter { $0.mediaType == selectedMediaType }
    }

    var imageCount: Int {
        galleryItems.filter { $0.mediaType == .image }.count
    }

    var canEditCurrentProfile: Bool {
        currentUser?.id == authManager.userSession?.id
    }

    func configure(user: User?) {
        currentUser = user
        syncDrafts(from: user)
        observeGallery(for: user?.id)
    }

    func setEditing(_ value: Bool) {
        isEditing = value
        if value {
            syncDrafts(from: currentUser)
        }
    }

    func saveProfile() async {
        guard canEditCurrentProfile else { return }

        let trimmedUsername = usernameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedUsername.isEmpty else {
            showError("Bitte gib einen Benutzernamen ein.")
            return
        }

        isSavingProfile = true
        defer { isSavingProfile = false }

        do {
            try await authManager.updateProfile(
                username: trimmedUsername,
                whatsApp: whatsAppDraft,
                profileTagline: taglineDraft,
                profileBio: bioDraft,
                instagramHandle: instagramDraft
            )
            currentUser = authManager.userSession
            syncDrafts(from: authManager.userSession)
            isEditing = false
            showSuccess("Profil gespeichert.")
        } catch {
            showError(error.localizedDescription)
        }
    }

    func uploadAvatar(data: Data) async {
        guard let userId = currentUser?.id, canEditCurrentProfile else { return }
        isUploadingAvatar = true
        defer { isUploadingAvatar = false }

        do {
            _ = try await service.uploadAvatar(userId: userId, imageData: data)
            await authManager.refreshCurrentUser()
            currentUser = authManager.userSession
            showSuccess("Profilbild aktualisiert.")
        } catch {
            showError(error.localizedDescription)
        }
    }

    func uploadGalleryImage(data: Data) async {
        await uploadGalleryData(
            data,
            type: .image,
            fileExtension: "jpg",
            mimeType: "image/jpeg",
            title: defaultTitle(for: .image)
        )
    }

    private func uploadGalleryData(
        _ data: Data,
        type: ProfileMediaType,
        fileExtension: String,
        mimeType: String?,
        title: String
    ) async {
        guard let userId = currentUser?.id, canEditCurrentProfile else { return }
        isUploadingMedia = true
        defer { isUploadingMedia = false }

        do {
            try await service.uploadGalleryData(
                userId: userId,
                data: data,
                type: type,
                fileExtension: fileExtension,
                mimeType: mimeType,
                title: title,
                caption: nil
            )
            selectedMediaType = type
            showSuccess(uploadSuccessMessage(for: type))
        } catch {
            showError(error.localizedDescription)
        }
    }

    private func observeGallery(for userId: String?) {
        galleryListener?.remove()
        galleryItems = []

        guard let userId, !userId.isEmpty else { return }
        galleryListener = service.observeGallery(for: userId) { [weak self] result in
            guard let self else { return }
            Task { @MainActor in
                switch result {
                case .success(let items):
                    self.galleryItems = items
                case .failure(let error):
                    self.showError(error.localizedDescription)
                }
            }
        }
    }

    private func syncDrafts(from user: User?) {
        usernameDraft = user?.username ?? ""
        whatsAppDraft = user?.whatsApp ?? ""
        taglineDraft = user?.profileTagline ?? ""
        bioDraft = user?.profileBio ?? ""
        instagramDraft = user?.instagramHandle ?? ""
    }

    private func defaultTitle(for type: ProfileMediaType) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM HH:mm"
        switch type {
        case .image:
            return "Bild \(formatter.string(from: .now))"
        }
    }

    private func showSuccess(_ message: String) {
        toastMessage = message
        toastStyle = .success
        showToast = true
    }

    private func uploadSuccessMessage(for type: ProfileMediaType) -> String {
        switch type {
        case .image:
            return "Bild hochgeladen."
        }
    }

    private func showError(_ message: String) {
        toastMessage = message
        toastStyle = .error
        showToast = true
    }
}
