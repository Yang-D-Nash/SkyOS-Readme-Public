import SwiftUI
import PhotosUI

struct EditableImageField: View {
    let title: String
    @Binding var imageURL: String
    @Binding var selection: PhotosPickerItem?
    let colorScheme: ColorScheme
    let buttonTitle: String

    init(
        title: String,
        imageURL: Binding<String>,
        selection: Binding<PhotosPickerItem?>,
        colorScheme: ColorScheme,
        buttonTitle: String = "Vom iPhone waehlen"
    ) {
        self.title = title
        self._imageURL = imageURL
        self._selection = selection
        self.colorScheme = colorScheme
        self.buttonTitle = buttonTitle
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
                    Text(imageURL.isEmpty ? "Noch kein Bild" : "Bild aktiv")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(.white.opacity(0.92))
                    Text(imageURL.isEmpty ? "Wird nach dem Upload direkt uebernommen." : "Die App dunkelt es automatisch ab.")
                        .font(.caption2)
                        .foregroundColor(.white.opacity(0.72))
                }
                .padding(12)
            }

            HStack(spacing: 10) {
                PhotosPicker(selection: $selection, matching: .images) {
                    Label(buttonTitle, systemImage: "photo.badge.plus")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(AppColors.accentMystic(for: colorScheme))
            }

            if !imageURL.isEmpty {
                Button("Bild entfernen", role: .destructive) {
                    imageURL = ""
                }
                .font(.footnote.weight(.semibold))
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
