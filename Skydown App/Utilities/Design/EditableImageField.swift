import SwiftUI

struct EditableImageField: View {
    let title: String
    @Binding var imageURL: String
    let colorScheme: ColorScheme
    let buttonTitle: String
    let isUploading: Bool
    let uploadStatusText: String
    let onPickImage: () -> Void
    let onRemoveImage: (() -> Void)?

    init(
        title: String,
        imageURL: Binding<String>,
        colorScheme: ColorScheme,
        buttonTitle: String = "Vom iPhone waehlen",
        isUploading: Bool = false,
        uploadStatusText: String = "Bild wird vorbereitet und hochgeladen.",
        onPickImage: @escaping () -> Void,
        onRemoveImage: (() -> Void)? = nil
    ) {
        self.title = title
        self._imageURL = imageURL
        self.colorScheme = colorScheme
        self.buttonTitle = buttonTitle
        self.isUploading = isUploading
        self.uploadStatusText = uploadStatusText
        self.onPickImage = onPickImage
        self.onRemoveImage = onRemoveImage
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                    .fill(AppColors.secondaryBackground(for: colorScheme))
                    .frame(height: 140)

                if let previewURL, !imageURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    AsyncImage(url: previewURL) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                        case .empty:
                            ProgressView()
                                .tint(AppColors.text(for: colorScheme))
                        default:
                            placeholder
                        }
                    }
                    .frame(height: 140)
                    .frame(maxWidth: .infinity)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                    .overlay(
                        LinearGradient(
                            colors: [
                                Color.black.opacity(0.02),
                                Color.black.opacity(0.58)
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous))
                    )
                } else {
                    placeholder
                }

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(isUploading ? "Upload laeuft" : imageURL.isEmpty ? "Noch kein Bild" : "Bild aktiv")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(.white.opacity(0.92))
                    Text(
                        isUploading
                            ? uploadStatusText
                            : imageURL.isEmpty
                                ? "Wird nach dem Upload direkt uebernommen."
                                : "Die App dunkelt es automatisch ab."
                    )
                        .font(.caption2)
                        .foregroundColor(.white.opacity(0.72))
                }
                .padding(12)

                if isUploading {
                    RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                        .fill(Color.black.opacity(0.62))

                    VStack(spacing: SkydownLayout.stackSpacingPill) {
                        ProgressView()
                            .tint(.white)
                        Text(uploadStatusText)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.white.opacity(0.92))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }

            SkydownBrandActionButton(
                title: buttonTitle,
                systemImage: "photo.badge.plus",
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                role: .muted,
                isEnabled: !isUploading,
                isLoading: isUploading,
                font: .subheadline.weight(.semibold),
                cornerRadius: SkydownLayout.denseRadius,
                verticalPadding: 11,
                action: onPickImage
            )

            if !imageURL.isEmpty {
                Button(AppLocalized.text("media.remove_image", fallback: "Remove image"), role: .destructive) {
                    if let onRemoveImage {
                        onRemoveImage()
                    } else {
                        imageURL = ""
                    }
                }
                .font(.footnote.weight(.semibold))
                .disabled(isUploading)
            }
        }
    }

    @ViewBuilder
    private var placeholder: some View {
        VStack(spacing: SkydownLayout.stackSpacingPill) {
            Image(systemName: "photo")
                .font(.system(size: 28, weight: .semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Text(AppLocalized.text("media.choose_image", fallback: "Choose image"))
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var previewURL: URL? {
        URL(string: imageURL.trimmingCharacters(in: .whitespacesAndNewlines))
    }
}
