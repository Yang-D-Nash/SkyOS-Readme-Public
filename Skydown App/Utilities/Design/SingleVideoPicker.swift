import PhotosUI
import SwiftUI
import UniformTypeIdentifiers

struct SingleVideoPicker: View {
    let onSelection: @MainActor (URL?) -> Void

    var body: some View {
        if SkydownPlatform.isDesktop {
            DesktopSingleVideoPicker(onSelection: onSelection)
        } else {
            LegacySingleVideoPicker(onSelection: onSelection)
        }
    }
}

private struct LegacySingleVideoPicker: UIViewControllerRepresentable {
    let onSelection: @MainActor (URL?) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onSelection: onSelection)
    }

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration(photoLibrary: .shared())
        configuration.filter = .videos
        configuration.selectionLimit = 1
        configuration.preferredAssetRepresentationMode = .current

        let controller = PHPickerViewController(configuration: configuration)
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        private let onSelection: @MainActor (URL?) -> Void
        private var didCompleteSelection = false

        init(onSelection: @escaping @MainActor (URL?) -> Void) {
            self.onSelection = onSelection
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            guard !didCompleteSelection else { return }
            didCompleteSelection = true

            guard let provider = results.first?.itemProvider else {
                Task { @MainActor in
                    onSelection(nil)
                }
                return
            }

            let typeIdentifier = provider.registeredTypeIdentifiers.first {
                UTType($0)?.conforms(to: .movie) == true
            } ?? UTType.movie.identifier

            provider.loadFileRepresentation(forTypeIdentifier: typeIdentifier) { [self] url, _ in
                let stableURL: URL?
                if let url {
                    stableURL = try? stagePickedVideoFile(from: url)
                } else {
                    stableURL = nil
                }

                Task { @MainActor in
                    self.onSelection(stableURL)
                }
            }
        }
    }
}

private struct DesktopSingleVideoPicker: View {
    let onSelection: @MainActor (URL?) -> Void
    @State private var isImporterPresented = false

    var body: some View {
        Color.clear
            .frame(width: 1, height: 1)
            .onAppear {
                guard !isImporterPresented else { return }
                isImporterPresented = true
            }
            .fileImporter(
                isPresented: $isImporterPresented,
                allowedContentTypes: supportedVideoContentTypes,
                allowsMultipleSelection: false
            ) { result in
                let selectedURL = try? result.get().first
                Task { @MainActor in
                    onSelection(selectedURL)
                }
            }
    }
}

private func stagePickedVideoFile(from sourceURL: URL) throws -> URL {
    let fileExtension = sourceURL.pathExtension
    let directory = FileManager.default.temporaryDirectory
        .appendingPathComponent("single-video-picker", isDirectory: true)
    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)

    let destinationURL = if fileExtension.isEmpty {
        directory.appendingPathComponent(UUID().uuidString)
    } else {
        directory.appendingPathComponent(UUID().uuidString).appendingPathExtension(fileExtension)
    }

    if FileManager.default.fileExists(atPath: destinationURL.path) {
        try FileManager.default.removeItem(at: destinationURL)
    }

    try FileManager.default.copyItem(at: sourceURL, to: destinationURL)
    return destinationURL
}
