import Foundation

struct AIVisualReferenceLibrarySettings: Codable, Equatable {
    var isEnabled = false
    var storageLink = ""
    var namingPrefix = ""
    var referenceHints = Array(repeating: "", count: 5)

    var trimmedStorageLink: String {
        storageLink.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var trimmedNamingPrefix: String {
        namingPrefix.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var activeReferenceHints: [String] {
        referenceHints
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }

    var isActive: Bool {
        isEnabled && (
            !trimmedStorageLink.isEmpty ||
            !trimmedNamingPrefix.isEmpty ||
            !activeReferenceHints.isEmpty
        )
    }

    var promptContext: String? {
        guard isActive else { return nil }

        var lines = ["Referenzbibliothek fuer Visual-Generierung:"]

        if !trimmedStorageLink.isEmpty {
            lines.append("- Asset-Ziel / Library: \(trimmedStorageLink)")
        }

        if !trimmedNamingPrefix.isEmpty {
            lines.append("- Benennungs-Praefix fuer neue Visuals: \(trimmedNamingPrefix)")
        }

        if !activeReferenceHints.isEmpty {
            lines.append("- Referenzhinweise fuer Stil, Charakter oder Elemente:")
            for (index, hint) in activeReferenceHints.enumerated() {
                lines.append("  \(index + 1). \(hint)")
            }
        }

        lines.append("- Nutze die Referenzen als gestalterische Richtung, ohne vorhandene Bilder direkt zu kopieren.")
        return lines.joined(separator: "\n")
    }
}

@MainActor
final class AIVisualReferenceLibraryStore: ObservableObject {
    static let shared = AIVisualReferenceLibraryStore()

    @Published var settings: AIVisualReferenceLibrarySettings {
        didSet {
            persist()
        }
    }

    private let defaults: UserDefaults
    private let storageKey = "ai_visual_reference_library_settings"

    private init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let data = defaults.data(forKey: storageKey),
           let decoded = try? JSONDecoder().decode(AIVisualReferenceLibrarySettings.self, from: data) {
            settings = decoded
        } else {
            settings = AIVisualReferenceLibrarySettings()
        }
    }

    static func promptContext() -> String? {
        shared.settings.promptContext
    }

    func update(_ mutate: (inout AIVisualReferenceLibrarySettings) -> Void) {
        var nextSettings = settings
        mutate(&nextSettings)
        settings = nextSettings
    }

    private func persist() {
        if let encoded = try? JSONEncoder().encode(settings) {
            defaults.set(encoded, forKey: storageKey)
        }
    }
}
