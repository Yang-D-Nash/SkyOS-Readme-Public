import SwiftUI
import UIKit
import UniformTypeIdentifiers

struct AIShareSheet: View {
    let activityItems: [Any]

    var body: some View {
        if SkydownPlatform.isDesktop {
            DesktopAIShareSheet(activityItems: activityItems)
        } else {
            LegacyAIShareSheet(activityItems: activityItems)
        }
    }
}

private struct LegacyAIShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(
            activityItems: activityItems,
            applicationActivities: nil
        )
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

private struct DesktopAIShareSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    let activityItems: [Any]
    @State private var showingExporter = false
    @State private var exportDocument: ShareExportDocument?
    @State private var exportFilename = "SkyOS-Share"
    @State private var statusMessage: String?

    private var sharedText: String? {
        let values = activityItems.compactMap { $0 as? String }.filter { !$0.isEmpty }
        guard !values.isEmpty else { return nil }
        return values.joined(separator: "\n\n")
    }

    private var sharedImage: UIImage? {
        activityItems.compactMap { $0 as? UIImage }.first
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingComfortable) {
                Text(AppLocalized.text("share.heading", fallback: "Share"))
                    .font(.title2.weight(.bold))

                Text(AppLocalized.text("share.desktop.hint", fallback: "On a Mac, you can copy content or save files locally."))
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                if let sharedText {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("share.copy_text", fallback: "Copy text"),
                        systemImage: "doc.on.doc",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 11,
                        action: {
                            UIPasteboard.general.string = sharedText
                            statusMessage = AppLocalized.text("share.text_copied", fallback: "Text copied")
                        }
                    )

                    ShareLink(item: sharedText) {
                        HStack(spacing: SkydownLayout.stackSpacingMicro) {
                            Image(systemName: "square.and.arrow.up")
                                .font(.subheadline.weight(.semibold))
                            Text(AppLocalized.text("share.share_text", fallback: "Share text"))
                                .font(.subheadline.weight(.semibold))
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .overlay(
                            RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous)
                                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.denseRadius, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .skydownInteractiveFeedback()
                    .skydownTactileAction()
                }

                if let sharedImage, let pngData = sharedImage.pngData() {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("share.save_image", fallback: "Save image"),
                        systemImage: "square.and.arrow.down",
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 11,
                        action: {
                            exportDocument = ShareExportDocument(data: pngData, contentType: .png)
                            exportFilename = "SkyOS-Image"
                            showingExporter = true
                        }
                    )
                }

                if sharedText == nil && sharedImage == nil {
                    Text(AppLocalized.text("share.cannot_export", fallback: "This content cannot be exported on the Mac right now."))
                        .foregroundColor(.secondary)
                }

                if let statusMessage {
                    Text(statusMessage)
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(.secondary)
                }

                Spacer()
            }
            .padding(20)
            .navigationTitle(AppLocalized.text("share.title", fallback: "Share"))
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("share.close", fallback: "Close"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: { dismiss() }
                    )
                    .skydownInteractiveFeedback()
                }
            }
        }
        .fileExporter(
            isPresented: $showingExporter,
            document: exportDocument,
            contentType: exportDocument?.contentType ?? .data,
            defaultFilename: exportFilename
        ) { result in
            switch result {
            case .success:
                statusMessage = AppLocalized.text("share.file_saved", fallback: "File saved")
            case .failure:
                statusMessage = AppLocalized.text("share.export_failed", fallback: "Export failed")
            }
        }
    }
}

private struct ShareExportDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.data, .png] }

    let data: Data
    let contentType: UTType

    init(data: Data, contentType: UTType) {
        self.data = data
        self.contentType = contentType
    }

    init(configuration: ReadConfiguration) throws {
        self.data = configuration.file.regularFileContents ?? Data()
        self.contentType = configuration.contentType
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}
