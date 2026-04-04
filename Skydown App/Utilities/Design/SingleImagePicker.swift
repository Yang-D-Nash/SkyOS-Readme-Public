import PhotosUI
import SwiftUI

struct SingleImagePicker: UIViewControllerRepresentable {
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
