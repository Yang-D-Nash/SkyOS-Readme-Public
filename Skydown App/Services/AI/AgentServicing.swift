import Foundation
import Combine
import FirebaseFunctions
import Security

struct AgentHistoryTurn {
    let role: String
    let text: String
}

struct AgentChatResponse {
    let reply: String
    let historyRetentionDays: Int
    let automationTriggered: Bool
    let automationAttempted: Bool
    let automationMessage: String
    let workflowName: String
    let agentProvider: String
    let providerFallbackUsed: Bool
    let providerNotice: String
    /// Firestore `users/{uid}/agentRuns/{id}` document id when the run was recorded server-side.
    let agentRunId: String
    let resultType: String
    let results: [AgentResultEntry]
    let usage: AIUsageSnapshot?
    let decision: AgentDecision?
}

struct AgentResultEntry {
    let type: String
    let text: String
    let workflowName: String
    let status: String
    let summary: String
    let runId: String
}

struct AgentDecision: Equatable {
    let state: String
    let requestedTask: String
    let route: String
    let selectedExternal: String
    let allowedTasks: [String]
    let blockedTasks: [String]
    let allowedTools: [String]
    let policy: String
    let diagnosticsMode: String
    let ownerMode: String
    let killSwitch: Bool
    let blocked: Bool
    let blockReason: String
    let retryable: Bool
    let retryReason: String
    let confirmationRequired: Bool
    let confirmationReason: String
    let summary: String
    let ownerDiagnosticActive: Bool
}

protocol AgentChatServicing {
    func sendMessage(
        prompt: String,
        history: [AgentHistoryTurn],
        mode: String,
        executeAutomation: Bool,
        manusApiKeyOverride: String?
    ) async throws -> AgentChatResponse
}

enum AgentServiceError: LocalizedError {
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Der SkyOS Agent hat keine Antwort geliefert."
        }
    }
}

struct FirebaseFunctionsAgentService: AgentChatServicing {
    private let functions: Functions

    init(functions: Functions = Functions.functions(region: "us-central1")) {
        self.functions = functions
    }

    func sendMessage(
        prompt: String,
        history: [AgentHistoryTurn],
        mode: String,
        executeAutomation: Bool,
        manusApiKeyOverride: String?
    ) async throws -> AgentChatResponse {
        try await ensureConnectivity()
        var payload: [String: Any] = [
            "prompt": prompt,
            "history": history.map { turn in
                [
                    "role": turn.role,
                    "text": turn.text
                ]
            },
            "mode": mode,
            "executeAutomation": executeAutomation
        ]
        if let manusApiKeyOverride,
           !manusApiKeyOverride.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            payload["manusApiKeyOverride"] = manusApiKeyOverride
        }

        let result = try await functions.invokeCallable("skydownAgent", payload: payload)

        if let reply = result.data as? String, !reply.isEmpty {
            return AgentChatResponse(
                reply: reply,
                historyRetentionDays: UserRole.user.defaultAIHistoryRetentionDays,
                automationTriggered: false,
                automationAttempted: false,
                automationMessage: "",
                workflowName: "",
                agentProvider: "",
                providerFallbackUsed: false,
                providerNotice: "",
                agentRunId: "",
                resultType: "text",
                results: [
                    AgentResultEntry(
                        type: "text",
                        text: reply,
                        workflowName: "",
                        status: "",
                        summary: "",
                        runId: ""
                    )
                ],
                usage: nil,
                decision: nil
            )
        }

        if let payload = result.data as? [String: Any],
           let reply = payload["reply"] as? String,
           !reply.isEmpty {
            return AgentChatResponse(
                reply: reply,
                historyRetentionDays: (payload["historyRetentionDays"] as? NSNumber)?.intValue
                    ?? UserRole.user.defaultAIHistoryRetentionDays,
                automationTriggered: payload["automationTriggered"] as? Bool ?? false,
                automationAttempted: payload["automationAttempted"] as? Bool ?? false,
                automationMessage: (payload["automationMessage"] as? String) ?? "",
                workflowName: (payload["workflowName"] as? String) ?? "",
                agentProvider: (payload["agentProvider"] as? String) ?? "",
                providerFallbackUsed: payload["providerFallbackUsed"] as? Bool ?? false,
                providerNotice: (payload["providerNotice"] as? String) ?? "",
                agentRunId: (payload["agentRunId"] as? String) ?? "",
                resultType: (payload["resultType"] as? String) ?? "text",
                results: (payload["results"] as? [[String: Any]] ?? []).map { entry in
                    AgentResultEntry(
                        type: (entry["type"] as? String) ?? "text",
                        text: (entry["text"] as? String) ?? "",
                        workflowName: (entry["workflowName"] as? String) ?? "",
                        status: (entry["status"] as? String) ?? "",
                        summary: (entry["summary"] as? String) ?? "",
                        runId: (entry["runId"] as? String) ?? ""
                    )
                },
                usage: parseUsage(payload["usage"] as? [String: Any]),
                decision: parseDecision(payload["agentDecision"] as? [String: Any])
            )
        }

        throw AgentServiceError.invalidResponse
    }

    private func parseUsage(_ payload: [String: Any]?) -> AIUsageSnapshot? {
        guard let payload else { return nil }
        let hints = payload["guardrailHints"] as? [String: Any]
        return AIUsageSnapshot(
            remainingForKind: (payload["remainingForKind"] as? NSNumber)?.intValue ?? 0,
            limitForKind: (payload["limitForKind"] as? NSNumber)?.intValue ?? 0,
            warningLevel: (payload["warningLevel"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? "ok",
            userFacingReason: (hints?["userFacingReason"] as? String) ?? "",
            suggestedUpgrade: (hints?["suggestedUpgrade"] as? String) ?? "",
            resetHint: (hints?["resetHint"] as? String) ?? "",
            retryAfterSeconds: (hints?["retryAfterSeconds"] as? NSNumber)?.intValue ?? 0,
            lowerCostOption: (hints?["lowerCostOption"] as? String) ?? ""
        )
    }

    private func parseDecision(_ payload: [String: Any]?) -> AgentDecision? {
        guard let payload else { return nil }
        return AgentDecision(
            state: (payload["state"] as? String) ?? "completed",
            requestedTask: (payload["requestedTask"] as? String) ?? "",
            route: (payload["route"] as? String) ?? "internal",
            selectedExternal: (payload["selectedExternal"] as? String) ?? "",
            allowedTasks: payload["allowedTasks"] as? [String] ?? [],
            blockedTasks: payload["blockedTasks"] as? [String] ?? [],
            allowedTools: payload["allowedTools"] as? [String] ?? [],
            policy: (payload["policy"] as? String) ?? "",
            diagnosticsMode: (payload["diagnosticsMode"] as? String) ?? "",
            ownerMode: (payload["ownerMode"] as? String) ?? "",
            killSwitch: payload["killSwitch"] as? Bool ?? false,
            blocked: payload["blocked"] as? Bool ?? false,
            blockReason: (payload["blockReason"] as? String) ?? "",
            retryable: payload["retryable"] as? Bool ?? false,
            retryReason: (payload["retryReason"] as? String) ?? "",
            confirmationRequired: payload["confirmationRequired"] as? Bool ?? false,
            confirmationReason: (payload["confirmationReason"] as? String) ?? "",
            summary: (payload["summary"] as? String) ?? "",
            ownerDiagnosticActive: payload["ownerDiagnosticActive"] as? Bool ?? false
        )
    }

    private func ensureConnectivity() async throws {
        let isOnline = await MainActor.run { NetworkStatusMonitor.shared.isOnline }
        guard isOnline else {
            throw NSError(
                domain: "AgentServicing",
                code: -1009,
                userInfo: [NSLocalizedDescriptionKey: "Du bist offline. Der Agent arbeitet weiter, sobald wieder eine Verbindung besteht."]
            )
        }
    }
}

struct ManusBYOSSettings: Equatable {
    var isEnabled: Bool = false
    var hasAPIKey: Bool = false

    static let `default` = ManusBYOSSettings()
}

@MainActor
final class ManusBYOSStore: ObservableObject {
    static let shared = ManusBYOSStore()

    @Published private(set) var settings: ManusBYOSSettings = .default

    private let defaults: UserDefaults
    private var currentUserID: String?

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func setUserMode(userID: String?) {
        let normalizedUserID = Self.normalizeUserID(userID)
        guard normalizedUserID != currentUserID else { return }
        currentUserID = normalizedUserID
        settings = readSettings(for: normalizedUserID)
    }

    func updateEnabled(_ isEnabled: Bool) throws {
        guard let userID = currentUserID else {
            throw NSError(
                domain: "ManusBYOSStore",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "Keine User-UID fuer Manus BYOS verfuegbar."]
            )
        }

        let hasAPIKey = hasStoredAPIKey(for: userID)
        if isEnabled && !hasAPIKey {
            throw NSError(
                domain: "ManusBYOSStore",
                code: 422,
                userInfo: [NSLocalizedDescriptionKey: "Bitte hinterlege zuerst einen Manus API Key."]
            )
        }

        defaults.set(isEnabled && hasAPIKey, forKey: enabledKey(for: userID))
        settings = ManusBYOSSettings(
            isEnabled: isEnabled && hasAPIKey,
            hasAPIKey: hasAPIKey
        )
    }

    func saveAPIKey(_ rawAPIKey: String) throws {
        guard let userID = currentUserID else {
            throw NSError(
                domain: "ManusBYOSStore",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "Keine User-UID fuer Manus BYOS verfuegbar."]
            )
        }

        let apiKey = rawAPIKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !apiKey.isEmpty else {
            throw NSError(
                domain: "ManusBYOSStore",
                code: 422,
                userInfo: [NSLocalizedDescriptionKey: "Bitte gib einen Manus API Key ein."]
            )
        }

        guard let data = apiKey.data(using: .utf8) else {
            throw NSError(
                domain: "ManusBYOSStore",
                code: 422,
                userInfo: [NSLocalizedDescriptionKey: "Der Manus API Key konnte nicht verarbeitet werden."]
            )
        }

        let keychainStore = ManusAPIKeyKeychainStore(
            service: keychainService,
            account: apiKeyAccount(for: userID)
        )
        guard keychainStore.saveAPIKeyData(data) else {
            throw NSError(
                domain: "ManusBYOSStore",
                code: 500,
                userInfo: [NSLocalizedDescriptionKey: "Manus API Key konnte nicht im Keychain gespeichert werden."]
            )
        }

        defaults.set(true, forKey: enabledKey(for: userID))
        settings = ManusBYOSSettings(
            isEnabled: true,
            hasAPIKey: true
        )
    }

    func clearAPIKey() {
        guard let userID = currentUserID else { return }
        let keychainStore = ManusAPIKeyKeychainStore(
            service: keychainService,
            account: apiKeyAccount(for: userID)
        )
        keychainStore.deleteAPIKey()
        defaults.set(false, forKey: enabledKey(for: userID))
        settings = .default
    }

    func currentAPIKeyOrNil() -> String? {
        guard let userID = currentUserID else { return nil }
        guard settings.isEnabled, settings.hasAPIKey else { return nil }

        let keychainStore = ManusAPIKeyKeychainStore(
            service: keychainService,
            account: apiKeyAccount(for: userID)
        )
        guard let data = keychainStore.loadAPIKeyData(),
              let key = String(data: data, encoding: .utf8)?
                .trimmingCharacters(in: .whitespacesAndNewlines),
              !key.isEmpty else {
            return nil
        }
        return key
    }

    private func readSettings(for userID: String?) -> ManusBYOSSettings {
        guard let userID else { return .default }
        let hasAPIKey = hasStoredAPIKey(for: userID)
        let isEnabled = defaults.bool(forKey: enabledKey(for: userID)) && hasAPIKey
        return ManusBYOSSettings(
            isEnabled: isEnabled,
            hasAPIKey: hasAPIKey
        )
    }

    private func hasStoredAPIKey(for userID: String) -> Bool {
        let keychainStore = ManusAPIKeyKeychainStore(
            service: keychainService,
            account: apiKeyAccount(for: userID)
        )
        return keychainStore.loadAPIKeyData() != nil
    }

    private func enabledKey(for userID: String) -> String {
        "manus.byos.enabled.\(userID)"
    }

    private func apiKeyAccount(for userID: String) -> String {
        "manus_api_key_\(userID)"
    }

    private static func normalizeUserID(_ userID: String?) -> String? {
        let trimmed = userID?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !trimmed.isEmpty else { return nil }
        let sanitized = trimmed
            .lowercased()
            .replacingOccurrences(of: "[^a-z0-9_.-]", with: "_", options: .regularExpression)
        return sanitized.isEmpty ? nil : sanitized
    }

    private let keychainService = "com.skydown.manus.byos"
}

private struct ManusAPIKeyKeychainStore {
    let service: String
    let account: String

    func loadAPIKeyData() -> Data? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)

        switch status {
        case errSecSuccess:
            return item as? Data
        case errSecItemNotFound:
            return nil
        default:
            return nil
        }
    }

    @discardableResult
    func saveAPIKeyData(_ data: Data) -> Bool {
        var query = baseQuery
        query[kSecValueData as String] = data
        query[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly

        let addStatus = SecItemAdd(query as CFDictionary, nil)
        if addStatus == errSecSuccess {
            return true
        }

        guard addStatus == errSecDuplicateItem else {
            return false
        }

        let updateAttributes: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        let updateStatus = SecItemUpdate(baseQuery as CFDictionary, updateAttributes as CFDictionary)
        return updateStatus == errSecSuccess
    }

    func deleteAPIKey() {
        SecItemDelete(baseQuery as CFDictionary)
    }

    private var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }
}
