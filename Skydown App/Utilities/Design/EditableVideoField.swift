import SwiftUI

struct EditableVideoField: View {
    let title: String
    @Binding var videoURL: String
    let colorScheme: ColorScheme
    let buttonTitle: String
    let isUploading: Bool
    let uploadStatusText: String
    let onPickVideo: () -> Void
    let onRemoveVideo: (() -> Void)?
    let accessibilityIDPrefix: String?

    init(
        title: String,
        videoURL: Binding<String>,
        colorScheme: ColorScheme,
        buttonTitle: String = "Vom iPhone waehlen",
        isUploading: Bool = false,
        uploadStatusText: String = "Video wird vorbereitet und hochgeladen.",
        accessibilityIDPrefix: String? = nil,
        onPickVideo: @escaping () -> Void,
        onRemoveVideo: (() -> Void)? = nil
    ) {
        self.title = title
        self._videoURL = videoURL
        self.colorScheme = colorScheme
        self.buttonTitle = buttonTitle
        self.isUploading = isUploading
        self.uploadStatusText = uploadStatusText
        self.accessibilityIDPrefix = accessibilityIDPrefix
        self.onPickVideo = onPickVideo
        self.onRemoveVideo = onRemoveVideo
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            ZStack {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                AppColors.accent(for: colorScheme).opacity(0.20),
                                AppColors.accentMystic(for: colorScheme).opacity(0.14),
                                AppColors.secondaryBackground(for: colorScheme)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(height: 148)

                VStack(spacing: 10) {
                    Image(systemName: "play.rectangle.fill")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(videoURL.trimmedNilIfEmpty == nil ? "Noch kein Hero-Video" : "Hero-Video aktiv")
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(
                        videoURL.trimmedNilIfEmpty == nil
                            ? "Ein kurzes Motion-Video macht die Artist-Stage deutlich lebendiger."
                            : "Das Video wird auf der Artist-Seite direkt als Motion-Stage abgespielt."
                    )
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                }

                if isUploading {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
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
                }
            }

            Button(action: onPickVideo) {
                Label(buttonTitle, systemImage: "video.badge.plus")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .tint(AppColors.accentMystic(for: colorScheme))
            .disabled(isUploading)
            .accessibilityIdentifier(accessibilityIDPrefix.map { "\($0).pick" } ?? "")

            if videoURL.trimmedNilIfEmpty != nil {
                Button("Video entfernen", role: .destructive) {
                    if let onRemoveVideo {
                        onRemoveVideo()
                    } else {
                        videoURL = ""
                    }
                }
                .font(.footnote.weight(.semibold))
                .disabled(isUploading)
                .accessibilityIdentifier(accessibilityIDPrefix.map { "\($0).remove" } ?? "")
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
