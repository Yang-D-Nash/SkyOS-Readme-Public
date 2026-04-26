import Foundation
import FirebaseFirestore
import FirebaseFunctions

struct WorkflowAutomationSettings: Equatable {
    var provider: String = "activepieces"
    var scope: String = "owner_global"
    var isEnabled: Bool = false
    var sendsUserContext: Bool = true
    var workflowName: String = "SkyOS Owner Activepieces Flow"
    var baseURL: String = ""
    var webhookPath: String = ""
    var authHeaderName: String = "X-SkyOS-Automation-Key"
    var authHeaderValue: String = ""
    var knowledgeContext: String = ""

    static let `default` = WorkflowAutomationSettings()

    var isOwnerGlobal: Bool {
        scope == "owner_global"
    }

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
        scope: String,
        _ onChange: @escaping @MainActor (Result<WorkflowAutomationSettings, Error>) -> Void
    ) -> () -> Void
    func updateSettings(_ settings: WorkflowAutomationSettings, userID: String, scope: String) async throws
    func triggerTest(userID: String, scope: String) async throws -> String
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
        scope: String,
        _ onChange: @escaping @MainActor (Result<WorkflowAutomationSettings, Error>) -> Void
    ) -> () -> Void {
        let listener = firestore.collection(collectionName).document(documentName(for: userID, scope: scope)).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                if let snapshot, snapshot.exists {
                    onChange(.success(Self.decode(snapshot.data() ?? [:], fallbackScope: scope)))
                    return
                }

                    onChange(.success(Self.emptySettings(scope: scope)))
            }
        }

        return {
            listener.remove()
        }
    }

    func updateSettings(_ settings: WorkflowAutomationSettings, userID: String, scope: String) async throws {
        try await firestore.collection(collectionName).document(documentName(for: userID, scope: scope)).setData(Self.encode(settings, scope: scope), merge: true)
    }

    func triggerTest(userID: String, scope: String) async throws -> String {
        let isOnline = await MainActor.run { NetworkStatusMonitor.shared.isOnline }
        guard isOnline else {
            throw NSError(
                domain: "WorkflowAutomationSettingsStore",
                code: -1009,
                userInfo: [NSLocalizedDescriptionKey: "Du bist offline. Der Workflow-Test braucht eine aktive Internetverbindung."]
            )
        }

        let result = try await functions.invokeCallable("triggerWorkflowAutomation", payload: [
                "trigger": "admin_settings_test",
                "source": "ios_settings",
                "automationScope": scope == "user_personal" ? "personal" : "owner",
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

        return "Test an externen Workflow gesendet."
    }

    private func documentName(for userID: String, scope: String) -> String {
        scope == "user_personal" ? "automationN8n_\(userID)" : "ownerActivepiecesFlow"
    }

    private static func emptySettings(scope: String) -> WorkflowAutomationSettings {
        WorkflowAutomationSettings(
            provider: "activepieces",
            scope: scope == "user_personal" ? "user_personal" : "owner_global",
            workflowName: scope == "user_personal" ? "Persoenlicher Workflow" : "SkyOS Owner Activepieces Flow"
        )
    }

    private static func decode(_ data: [String: Any], fallbackScope: String = "owner_global") -> WorkflowAutomationSettings {
        let scope = (data["scope"] as? String)?.trimmedNonEmpty ?? fallbackScope
        let isPersonalScope = scope == "user_personal"
        let provider = (data["provider"] as? String)?.trimmedNonEmpty ?? "activepieces"
        return WorkflowAutomationSettings(
            provider: isPersonalScope && ["activepieces", "n8n"].contains(provider) ? provider : "activepieces",
            scope: scope,
            isEnabled: data["isEnabled"] as? Bool ?? false,
            sendsUserContext: data["sendsUserContext"] as? Bool ?? true,
            workflowName: (data["workflowName"] as? String)?.trimmedNonEmpty ?? (isPersonalScope ? "Persoenlicher Workflow" : "SkyOS Owner Activepieces Flow"),
            baseURL: normalizeAutomationBaseURL(data["baseURL"] as? String) ?? "",
            webhookPath: normalizeAutomationWebhookPath(data["webhookPath"] as? String) ?? "",
            authHeaderName: (data["authHeaderName"] as? String)?.trimmed ?? "",
            authHeaderValue: (data["authHeaderValue"] as? String)?.trimmed ?? "",
            knowledgeContext: (data["knowledgeContext"] as? String)?.trimmed ?? ""
        )
    }

    private static func encode(_ settings: WorkflowAutomationSettings, scope: String) -> [String: Any] {
        let isPersonalScope = scope == "user_personal"
        let provider = isPersonalScope && settings.provider == "n8n" ? "n8n" : "activepieces"
        return [
            "provider": provider,
            "scope": isPersonalScope ? "user_personal" : "owner_global",
            "isEnabled": settings.isEnabled,
            "sendsUserContext": settings.sendsUserContext,
            "workflowName": settings.workflowName.trimmedNonEmpty ?? (isPersonalScope ? "Persoenlicher Workflow" : "SkyOS Owner Activepieces Flow"),
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
    private var currentScope = "owner_global"

    init(service: WorkflowAutomationSettingsServicing = FirestoreAutomationSettingsService()) {
        self.service = service
    }

    func configureObservation(isEnabled: Bool, userID: String?, scope: String = "owner_global") {
        guard isEnabled, let userID, !userID.isEmpty else {
            stopObserving?()
            stopObserving = nil
            isObserving = false
            currentUserID = nil
            currentScope = "owner_global"
            settings = .default
            lastErrorMessage = nil
            return
        }

        if currentUserID != userID || currentScope != scope {
            stopObserving?()
            stopObserving = nil
            isObserving = false
            currentUserID = userID
            currentScope = scope
        }

        if isEnabled {
            startObservingIfNeeded()
        }
    }

    func save(_ settings: WorkflowAutomationSettings) async throws {
        guard let currentUserID else {
            throw NSError(domain: "WorkflowAutomationSettingsStore", code: 401, userInfo: [NSLocalizedDescriptionKey: "Keine User-UID fuer Workflow-Konfiguration verfuegbar."])
        }

        try await service.updateSettings(settings, userID: currentUserID, scope: currentScope)
    }

    func triggerTest() async throws -> String {
        guard let currentUserID else {
            throw NSError(domain: "WorkflowAutomationSettingsStore", code: 401, userInfo: [NSLocalizedDescriptionKey: "Keine User-UID fuer Workflow-Test verfuegbar."])
        }

        return try await service.triggerTest(userID: currentUserID, scope: currentScope)
    }

    private func startObservingIfNeeded() {
        guard !isObserving else { return }
        isObserving = true
        stopObserving?()
        guard let currentUserID else { return }
        stopObserving = service.observeSettings(userID: currentUserID, scope: currentScope) { [weak self] result in
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
