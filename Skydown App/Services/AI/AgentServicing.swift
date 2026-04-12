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
            return "Der Skydown x 22 Agent hat keine Antwort geliefert."
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

        let result = try await functions
            .httpsCallable("skydownAgent")
            .call(payload)

        if let reply = result.data as? String, !reply.isEmpty {
            return AgentChatResponse(
                reply: reply,
                historyRetentionDays: UserRole.user.defaultAIHistoryRetentionDays,
                automationTriggered: false,
                automationAttempted: false,
                automationMessage: "",
                workflowName: ""
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
                workflowName: (payload["workflowName"] as? String) ?? ""
            )
        }

        throw AgentServiceError.invalidResponse
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
