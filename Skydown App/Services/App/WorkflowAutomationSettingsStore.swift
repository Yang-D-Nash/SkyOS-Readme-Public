import Foundation
import FirebaseFirestore
import FirebaseFunctions

struct WorkflowAutomationSettings: Equatable {
    var provider: String = "n8n"
    var isEnabled: Bool = false
    var sendsUserContext: Bool = true
    var workflowName: String = "Skydown Automation"
    var baseURL: String = ""
    var webhookPath: String = ""
    var authHeaderName: String = "X-Skydown-Automation-Key"
    var authHeaderValue: String = ""
    var knowledgeContext: String = ""

    static let `default` = WorkflowAutomationSettings()

    var resolvedWebhookURL: String? {
        guard let normalizedBaseURL = normalizeAutomationBaseURL(baseURL) else {
            return nil
        }

        let trimmedPath = webhookPath
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))

        guard !trimmedPath.isEmpty else {
            return normalizedBaseURL
        }

        return "\(normalizedBaseURL)/\(trimmedPath)"
    }

    var isPrepared: Bool {
        isEnabled && resolvedWebhookURL != nil
    }
}

protocol WorkflowAutomationSettingsServicing {
    func observeSettings(
        userID: String,
        _ onChange: @escaping @MainActor (Result<WorkflowAutomationSettings, Error>) -> Void
    ) -> () -> Void
    func updateSettings(_ settings: WorkflowAutomationSettings, userID: String) async throws
    func triggerTest(userID: String) async throws -> String
}

final class FirestoreAutomationSettingsService: WorkflowAutomationSettingsServicing {
    private let firestore: Firestore
    private let functions: Functions
    private let collectionName = "adminConfig"

    init(
        firestore: Firestore = Firestore.firestore(),
        functions: Functions = Functions.functions(region: "us-central1")
    ) {
        self.firestore = firestore
        self.functions = functions
    }

    func observeSettings(
        userID: String,
        _ onChange: @escaping @MainActor (Result<WorkflowAutomationSettings, Error>) -> Void
    ) -> () -> Void {
        let listener = firestore.collection(collectionName).document(documentName(for: userID)).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                if let snapshot, snapshot.exists {
                    onChange(.success(Self.decode(snapshot.data() ?? [:])))
                    return
                }

                onChange(.success(.default))
            }
        }

        return {
            listener.remove()
        }
    }

    func updateSettings(_ settings: WorkflowAutomationSettings, userID: String) async throws {
        try await firestore.collection(collectionName).document(documentName(for: userID)).setData(Self.encode(settings), merge: true)
    }

    func triggerTest(userID: String) async throws -> String {
        let isOnline = await MainActor.run { NetworkStatusMonitor.shared.isOnline }
        guard isOnline else {
            throw NSError(
                domain: "WorkflowAutomationSettingsStore",
                code: -1009,
                userInfo: [NSLocalizedDescriptionKey: "Du bist offline. Der n8n-Test braucht eine aktive Internetverbindung."]
            )
        }

        let result = try await functions
            .httpsCallable("triggerWorkflowAutomation")
            .call([
                "trigger": "admin_settings_test",
                "source": "ios_settings",
                "userId": userID
            ])

        if let payload = result.data as? [String: Any],
           let message = payload["message"] as? String,
           !message.isEmpty {
            return message
        }

        if let message = result.data as? String, !message.isEmpty {
            return message
        }

        return "Test an n8n gesendet."
    }

    private func documentName(for userID: String) -> String {
        "automationN8n_\(userID)"
    }

    private static func decode(_ data: [String: Any]) -> WorkflowAutomationSettings {
        WorkflowAutomationSettings(
            provider: (data["provider"] as? String)?.trimmedNonEmpty ?? "n8n",
            isEnabled: data["isEnabled"] as? Bool ?? false,
            sendsUserContext: data["sendsUserContext"] as? Bool ?? true,
            workflowName: (data["workflowName"] as? String)?.trimmedNonEmpty ?? "Skydown Automation",
            baseURL: normalizeAutomationBaseURL(data["baseURL"] as? String) ?? "",
            webhookPath: normalizeAutomationWebhookPath(data["webhookPath"] as? String) ?? "",
            authHeaderName: (data["authHeaderName"] as? String)?.trimmed ?? "",
            authHeaderValue: (data["authHeaderValue"] as? String)?.trimmed ?? "",
            knowledgeContext: (data["knowledgeContext"] as? String)?.trimmed ?? ""
        )
    }

    private static func encode(_ settings: WorkflowAutomationSettings) -> [String: Any] {
        [
            "provider": "n8n",
            "isEnabled": settings.isEnabled,
            "sendsUserContext": settings.sendsUserContext,
            "workflowName": settings.workflowName.trimmedNonEmpty ?? "Skydown Automation",
            "baseURL": normalizeAutomationBaseURL(settings.baseURL) ?? "",
            "webhookPath": normalizeAutomationWebhookPath(settings.webhookPath) ?? "",
            "authHeaderName": settings.authHeaderName.trimmed,
            "authHeaderValue": settings.authHeaderValue.trimmed,
            "knowledgeContext": settings.knowledgeContext.trimmed,
            "updatedAt": FieldValue.serverTimestamp()
        ]
    }
}

@MainActor
final class WorkflowAutomationSettingsStore: ObservableObject {
    static let shared = WorkflowAutomationSettingsStore()

    @Published private(set) var settings: WorkflowAutomationSettings = .default
    @Published private(set) var lastErrorMessage: String?

    private let service: WorkflowAutomationSettingsServicing
    private var stopObserving: (() -> Void)?
    private var isObserving = false
    private var currentUserID: String?

    init(service: WorkflowAutomationSettingsServicing = FirestoreAutomationSettingsService()) {
        self.service = service
    }

    func configureObservation(isEnabled: Bool, userID: String?) {
        guard isEnabled, let userID, !userID.isEmpty else {
            stopObserving?()
            stopObserving = nil
            isObserving = false
            currentUserID = nil
            settings = .default
            lastErrorMessage = nil
            return
        }

        if currentUserID != userID {
            stopObserving?()
            stopObserving = nil
            isObserving = false
            currentUserID = userID
        }

        if isEnabled {
            startObservingIfNeeded()
        }
    }

    func save(_ settings: WorkflowAutomationSettings) async throws {
        guard let currentUserID else {
            throw NSError(domain: "WorkflowAutomationSettingsStore", code: 401, userInfo: [NSLocalizedDescriptionKey: "Keine User-UID fuer n8n-Konfiguration verfuegbar."])
        }

        try await service.updateSettings(settings, userID: currentUserID)
    }

    func triggerTest() async throws -> String {
        guard let currentUserID else {
            throw NSError(domain: "WorkflowAutomationSettingsStore", code: 401, userInfo: [NSLocalizedDescriptionKey: "Keine User-UID fuer n8n-Test verfuegbar."])
        }

        return try await service.triggerTest(userID: currentUserID)
    }

    private func startObservingIfNeeded() {
        guard !isObserving else { return }
        isObserving = true
        stopObserving?()
        guard let currentUserID else { return }
        stopObserving = service.observeSettings(userID: currentUserID) { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let settings):
                self.settings = settings
                self.lastErrorMessage = nil
            case .failure(let error):
                self.lastErrorMessage = error.localizedDescription
            }
        }
    }

    deinit {
        stopObserving?()
    }
}

private func normalizeAutomationBaseURL(_ value: String?) -> String? {
    guard let value else { return nil }
    let trimmed = value.trimmed
    guard !trimmed.isEmpty else { return nil }

    if let url = URL(string: trimmed), let scheme = url.scheme, !scheme.isEmpty {
        return url.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    if let url = URL(string: "https://\(trimmed)") {
        return url.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    return nil
}

private func normalizeAutomationWebhookPath(_ value: String?) -> String? {
    guard let value else { return nil }
    let trimmed = value.trimmed
    guard !trimmed.isEmpty else { return nil }

    if let url = URL(string: trimmed), let scheme = url.scheme, !scheme.isEmpty {
        let withoutScheme = url.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        return withoutScheme.isEmpty ? nil : withoutScheme
    }

    return trimmed.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var trimmedNonEmpty: String? {
        let value = trimmed
        return value.isEmpty ? nil : value
    }
}
