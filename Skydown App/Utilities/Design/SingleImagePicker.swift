import PhotosUI
import SwiftUI
import UniformTypeIdentifiers

struct SingleImagePicker: View {
    let onSelection: @MainActor (URL?) -> Void

    var body: some View {
        if SkydownPlatform.isDesktop {
            DesktopSingleImagePicker(onSelection: onSelection)
        } else {
            LegacySingleImagePicker(onSelection: onSelection)
        }
    }
}

private struct LegacySingleImagePicker: UIViewControllerRepresentable {
    let onSelection: @MainActor (URL?) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onSelection: onSelection)
    }

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration(photoLibrary: .shared())
        configuration.filter = .images
        configuration.selectionLimit = 1
        configuration.preferredAssetRepresentationMode = .compatible

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

            Task {
                let fileURL = try? await PickedImageUploadPreparation.stableTemporaryImageURL(from: provider)
                await MainActor.run {
                    onSelection(fileURL)
                }
            }
        }
    }
}

private struct DesktopSingleImagePicker: View {
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
                allowedContentTypes: [.image],
                allowsMultipleSelection: false
            ) { result in
                let selectedURL = try? result.get().first
                Task { @MainActor in
                    onSelection(selectedURL)
                }
            }
    }
}
