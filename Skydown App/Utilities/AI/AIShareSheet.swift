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
            VStack(alignment: .leading, spacing: 16) {
                Text("Teilen")
                    .font(.title2.weight(.bold))

                Text("Auf dem Mac kannst du Inhalte direkt kopieren oder Dateien lokal sichern.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                if let sharedText {
                    Button("Text kopieren") {
                        UIPasteboard.general.string = sharedText
                        statusMessage = "Text kopiert"
                    }
                    .buttonStyle(.borderedProminent)

                    ShareLink(item: sharedText) {
                        Label("Text teilen", systemImage: "square.and.arrow.up")
                    }
                    .buttonStyle(.bordered)
                }

                if let sharedImage, let pngData = sharedImage.pngData() {
                    Button("Bild sichern") {
                        exportDocument = ShareExportDocument(data: pngData, contentType: .png)
                        exportFilename = "SkyOS-Image"
                        showingExporter = true
                    }
                    .buttonStyle(.borderedProminent)
                }

                if sharedText == nil && sharedImage == nil {
                    Text("Dieser Inhalt kann auf dem Mac gerade nicht exportiert werden.")
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
            .navigationTitle("Share")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Schliessen") {
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
                statusMessage = "Datei gesichert"
            case .failure:
                statusMessage = "Export fehlgeschlagen"
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
