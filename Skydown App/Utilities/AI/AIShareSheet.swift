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
                    Button(AppLocalized.text("share.copy_text", fallback: "Copy text")) {
                        UIPasteboard.general.string = sharedText
                        statusMessage = AppLocalized.text("share.text_copied", fallback: "Text copied")
                    }
                    .buttonStyle(.borderedProminent)

                    ShareLink(item: sharedText) {
                        Label(
                            AppLocalized.text("share.share_text", fallback: "Share text"),
                            systemImage: "square.and.arrow.up"
                        )
                    }
                    .buttonStyle(.bordered)
                }

                if let sharedImage, let pngData = sharedImage.pngData() {
                    Button(AppLocalized.text("share.save_image", fallback: "Save image")) {
                        exportDocument = ShareExportDocument(data: pngData, contentType: .png)
                        exportFilename = "SkyOS-Image"
                        showingExporter = true
                    }
                    .buttonStyle(.borderedProminent)
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
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppLocalized.text("share.close", fallback: "Close")) {
                        dismiss()
                    }
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
