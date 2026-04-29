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
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            ZStack {
                RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
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

                VStack(spacing: SkydownLayout.stackSpacingPill) {
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
                }
            }

            SkydownBrandActionButton(
                title: buttonTitle,
                systemImage: "video.badge.plus",
                accent: AppColors.accentMystic(for: colorScheme),
                colorScheme: colorScheme,
                role: .muted,
                isEnabled: !isUploading,
                isLoading: isUploading,
                font: .subheadline.weight(.semibold),
                cornerRadius: SkydownLayout.denseRadius,
                verticalPadding: 11,
                action: onPickVideo
            )
            .accessibilityIdentifier(accessibilityIDPrefix.map { "\($0).pick" } ?? "")

            if videoURL.trimmedNilIfEmpty != nil {
                Button(AppLocalized.text("media.remove_video", fallback: "Remove video"), role: .destructive) {
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
