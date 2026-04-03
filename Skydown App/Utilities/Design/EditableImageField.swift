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
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: 18)
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
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                    .overlay(
                        LinearGradient(
                            colors: [
                                Color.black.opacity(0.02),
                                Color.black.opacity(0.58)
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                    )
                } else {
                    placeholder
                }

                VStack(alignment: .leading, spacing: 4) {
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
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.black.opacity(0.62))

                    VStack(spacing: 10) {
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

            HStack(spacing: 10) {
                Button(action: onPickImage) {
                    Label(buttonTitle, systemImage: "photo.badge.plus")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(AppColors.accentMystic(for: colorScheme))
                .disabled(isUploading)
            }

            if !imageURL.isEmpty {
                Button("Bild entfernen", role: .destructive) {
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
        VStack(spacing: 10) {
            Image(systemName: "photo")
                .font(.system(size: 28, weight: .semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
            Text("Bild waehlen")
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var previewURL: URL? {
        URL(string: imageURL.trimmingCharacters(in: .whitespacesAndNewlines))
    }
}
