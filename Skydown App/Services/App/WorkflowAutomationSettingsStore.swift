import Foundation
import Combine

struct WorkflowAutomationSettings: Codable, Equatable {
    var keepsGoogleSeparate: Bool = true
    var isPrepared: Bool = false
    var googleAccountHint: String = ""
    var googleScopeHint: String = "Drive, Sheets, Calendar"
}

@MainActor
final class WorkflowAutomationSettingsStore: ObservableObject {
    static let shared = WorkflowAutomationSettingsStore()

    @Published private(set) var settings: WorkflowAutomationSettings

    private let defaults: UserDefaults
    private let storageKey = "workflowAutomationSettings"

    private init(defaults: UserDefaults = .standard) {
        self.defaults = defaults

        if let data = defaults.data(forKey: storageKey),
           let decoded = try? JSONDecoder().decode(WorkflowAutomationSettings.self, from: data) {
            settings = decoded
        } else {
            settings = WorkflowAutomationSettings()
        }
    }

    func update(_ transform: (inout WorkflowAutomationSettings) -> Void) {
        var updated = settings
        transform(&updated)
        settings = updated
        persist(updated)
    }

    private func persist(_ settings: WorkflowAutomationSettings) {
        guard let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: storageKey)
    }
}
