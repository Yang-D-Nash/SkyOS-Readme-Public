import Foundation
import Combine
import FirebaseFunctions
import Security
import UniformTypeIdentifiers

struct AgentHistoryTurn {
    let role: String
    let text: String
}

/// Inline file payload for `skydownAgent` (base64). Max 5 items × 256 KiB each, enforced client-side.
struct AgentOutboundAttachment: Codable, Equatable, Identifiable {
    var id: String
    let name: String
    let kind: String
    let mimeType: String
    let source: String
    let inlineBase64: String

    init(
        id: String = UUID().uuidString,
        name: String,
        kind: String,
        mimeType: String,
        source: String = "inline",
        inlineBase64: String
    ) {
        self.id = id
        self.name = name
        self.kind = kind
        self.mimeType = mimeType
        self.source = source
        self.inlineBase64 = inlineBase64
    }

    func asDictionary() -> [String: Any] {
        [
            "id": id,
            "name": name,
            "kind": kind,
            "mimeType": mimeType,
            "source": source,
            "inlineBase64": inlineBase64
        ]
    }

    private static let maxBytes = 256 * 1024

    static func loadInline(fileURL: URL) throws -> AgentOutboundAttachment {
        let accessing = fileURL.startAccessingSecurityScopedResource()
        defer {
            if accessing {
                fileURL.stopAccessingSecurityScopedResource()
            }
        }
        let data = try Data(contentsOf: fileURL)
        guard !data.isEmpty else {
            throw AgentServiceError.attachmentUnreadable
        }
        guard data.count <= maxBytes else {
            throw AgentServiceError.attachmentTooLarge
        }
        let displayName = fileURL.lastPathComponent.isEmpty ? "attachment" : fileURL.lastPathComponent
        return AgentOutboundAttachment(
            id: String(fileURL.absoluteString.prefix(200)),
            name: String(displayName.prefix(255)),
            kind: wireKind(forFileURL: fileURL),
            mimeType: mimeTypeForFileURL(fileURL),
            inlineBase64: data.base64EncodedString()
        )
    }

    static func batchInline(fileURLs: [URL], limit: Int = 5) -> [AgentOutboundAttachment] {
        fileURLs.prefix(limit).compactMap { url in
            try? loadInline(fileURL: url)
        }
    }

    private static func mimeTypeForFileURL(_ url: URL) -> String {
        let ext = url.pathExtension.lowercased()
        guard !ext.isEmpty,
              let type = UTType(filenameExtension: ext),
              let mime = type.preferredMIMEType else {
            return "application/octet-stream"
        }
        return mime
    }

    private static func wireKind(forFileURL url: URL) -> String {
        let ext = url.pathExtension.lowercased()
        if ["txt", "md", "rtf", "json", "csv", "xml", "html"].contains(ext) {
            return "text"
        }
        if ["mp4", "mov", "m4v", "avi", "mkv", "webm"].contains(ext) {
            return "video"
        }
        if ["mp3", "wav", "m4a", "aac", "flac", "aiff"].contains(ext) {
            return "audio"
        }
        if ["png", "jpg", "jpeg", "webp", "heic", "gif"].contains(ext) {
            return "image"
        }
        if ["pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx"].contains(ext) {
            return "document"
        }
        return "file"
    }
}

struct AgentChatResponse {
    let reply: String
    let historyRetentionDays: Int
    let automationTriggered: Bool
    let automationAttempted: Bool
    let automationMessage: String
    let workflowName: String
    /// Optional contract version reported by the external workflow JSON (`schemaVersion` / `automationSchemaVersion`).
    let automationSchemaVersion: String
    let agentProvider: String
    let providerFallbackUsed: Bool
    let providerNotice: String
    /// Firestore `users/{uid}/agentRuns/{id}` document id when the run was recorded server-side.
    let agentRunId: String
    let resultType: String
    let results: [AgentResultEntry]
    let usage: AIUsageSnapshot?
    let decision: AgentDecision?
    /// True when the server skipped a second external webhook for the same idempotency key (TTL).
    let automationIdempotentReplay: Bool
}

struct AgentResultEntry: Identifiable, Equatable {
    let id: String
    let type: String
    let text: String
    let url: String
    let title: String
    let mimeType: String
    let fileName: String
    let html: String
    let columns: [String]
    let rows: [[String]]
    let workflowName: String
    let status: String
    let summary: String
    let runId: String

    init(
        id: String = UUID().uuidString,
        type: String,
        text: String = "",
        url: String = "",
        title: String = "",
        mimeType: String = "",
        fileName: String = "",
        html: String = "",
        columns: [String] = [],
        rows: [[String]] = [],
        workflowName: String = "",
        status: String = "",
        summary: String = "",
        runId: String = ""
    ) {
        self.id = id
        self.type = type
        self.text = text
        self.url = url
        self.title = title
        self.mimeType = mimeType
        self.fileName = fileName
        self.html = html
        self.columns = columns
        self.rows = rows
        self.workflowName = workflowName
        self.status = status
        self.summary = summary
        self.runId = runId
    }
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

struct AgentRunStatus: Equatable {
    let runId: String
    let state: String
    let automationAttempted: Bool
    let automationTriggered: Bool
    let workflowName: String
    let automationMessage: String
    let provider: String
    let automationSchemaVersion: String
    let progressPercent: Int
    let step: String
    let etaSeconds: Int
    let details: String
}

struct AgentSocialSetupInput: Codable, Equatable {
    var instagramEnabled: Bool
    var instagramHandle: String
    var tiktokEnabled: Bool
    var tiktokHandle: String
    var youtubeEnabled: Bool
    var youtubeHandle: String

    static let empty = AgentSocialSetupInput(
        instagramEnabled: false,
        instagramHandle: "",
        tiktokEnabled: false,
        tiktokHandle: "",
        youtubeEnabled: false,
        youtubeHandle: ""
    )

    var hasAnySelection: Bool {
        instagramEnabled || tiktokEnabled || youtubeEnabled
    }

    func asDictionary() -> [String: Any] {
        [
            "instagramEnabled": instagramEnabled,
            "instagramHandle": instagramHandle,
            "tiktokEnabled": tiktokEnabled,
            "tiktokHandle": tiktokHandle,
            "youtubeEnabled": youtubeEnabled,
            "youtubeHandle": youtubeHandle
        ]
    }
}

protocol AgentChatServicing {
    func sendMessage(
        prompt: String,
        history: [AgentHistoryTurn],
        mode: String,
        aiLevel: String,
        executeAutomation: Bool,
        automationScope: String,
        manusApiKeyOverride: String?,
        idempotencyKey: String?,
        attachments: [AgentOutboundAttachment],
        socialSetup: AgentSocialSetupInput?
    ) async throws -> AgentChatResponse
    func fetchRunStatus(runId: String) async throws -> AgentRunStatus
}

enum AgentServiceError: LocalizedError {
    case invalidResponse
    case attachmentTooLarge
    case attachmentUnreadable

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Der SkyOS Agent hat keine Antwort geliefert."
        case .attachmentTooLarge:
            return "Eine angehaengte Datei ist zu gross (max. 256 KB pro Datei, max. 5 Dateien)."
        case .attachmentUnreadable:
            return "Eine angehaengte Datei konnte nicht gelesen werden."
        }
    }
}

struct FirebaseFunctionsAgentService: AgentChatServicing {
    private let functions: Functions

    init(functions: Functions = Functions.functions(region: "us-central1")) {
        self.functions = functions
    }

    func fetchRunStatus(runId: String) async throws -> AgentRunStatus {
        let trimmedRunId = runId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedRunId.isEmpty else {
            throw AgentServiceError.invalidResponse
        }
        let result = try await functions.invokeCallable("getAgentRunStatus", payload: [
            "runId": trimmedRunId
        ])
        guard let payload = result.data as? [String: Any] else {
            throw AgentServiceError.invalidResponse
        }
        return AgentRunStatus(
            runId: (payload["runId"] as? String) ?? trimmedRunId,
            state: ((payload["state"] as? String) ?? "completed")
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased(),
            automationAttempted: payload["automationAttempted"] as? Bool ?? false,
            automationTriggered: payload["automationTriggered"] as? Bool ?? false,
            workflowName: (payload["workflowName"] as? String) ?? "",
            automationMessage: (payload["automationMessage"] as? String) ?? "",
            provider: (payload["provider"] as? String) ?? "",
            automationSchemaVersion: (payload["automationSchemaVersion"] as? String) ?? "",
            progressPercent: min(100, max(0, (payload["workflowProgressPercent"] as? NSNumber)?.intValue ?? 0)),
            step: (payload["workflowStep"] as? String) ?? "",
            etaSeconds: max(0, (payload["workflowEtaSeconds"] as? NSNumber)?.intValue ?? 0),
            details: (payload["workflowDetails"] as? String) ?? ""
        )
    }

    func sendMessage(
        prompt: String,
        history: [AgentHistoryTurn],
        mode: String,
        aiLevel: String,
        executeAutomation: Bool,
        automationScope: String,
        manusApiKeyOverride: String?,
        idempotencyKey: String? = nil,
        attachments: [AgentOutboundAttachment] = [],
        socialSetup: AgentSocialSetupInput? = nil
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
            "aiLevel": aiLevel,
            "executeAutomation": executeAutomation,
            "automationScope": automationScope
        ]
        if let manusApiKeyOverride,
           !manusApiKeyOverride.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            payload["manusApiKeyOverride"] = manusApiKeyOverride
        }
        if !attachments.isEmpty {
            payload["attachments"] = attachments.map { $0.asDictionary() }
        }
        if let idempotencyKey = idempotencyKey?.trimmingCharacters(in: .whitespacesAndNewlines),
           idempotencyKey.count >= 8 {
            payload["idempotencyKey"] = idempotencyKey
        }
        if let socialSetup, socialSetup.hasAnySelection {
            payload["socialSetup"] = socialSetup.asDictionary()
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
                automationSchemaVersion: "",
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
                decision: nil,
                automationIdempotentReplay: false
            )
        }

        if let payload = result.data as? [String: Any] {
            let reply = ((payload["reply"] as? String) ?? (payload["message"] as? String) ?? "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let parsedResults = parseAgentResults(payload["results"] as? [Any])
            guard !reply.isEmpty || !parsedResults.isEmpty else {
                throw AgentServiceError.invalidResponse
            }
            let resolvedReply = reply.isEmpty ? "Dein Content ist fertig." : reply
            return AgentChatResponse(
                reply: resolvedReply,
                historyRetentionDays: (payload["historyRetentionDays"] as? NSNumber)?.intValue
                    ?? UserRole.user.defaultAIHistoryRetentionDays,
                automationTriggered: payload["automationTriggered"] as? Bool ?? false,
                automationAttempted: payload["automationAttempted"] as? Bool ?? false,
                automationMessage: (payload["automationMessage"] as? String) ?? "",
                workflowName: (payload["workflowName"] as? String) ?? "",
                automationSchemaVersion: (payload["automationSchemaVersion"] as? String) ?? "",
                agentProvider: (payload["agentProvider"] as? String) ?? "",
                providerFallbackUsed: payload["providerFallbackUsed"] as? Bool ?? false,
                providerNotice: (payload["providerNotice"] as? String) ?? "",
                agentRunId: (payload["agentRunId"] as? String) ?? "",
                resultType: (payload["resultType"] as? String) ?? "text",
                results: parsedResults.isEmpty ? [AgentResultEntry(type: "text", text: resolvedReply)] : parsedResults,
                usage: parseUsage(payload["usage"] as? [String: Any]),
                decision: parseDecision(payload["agentDecision"] as? [String: Any]),
                automationIdempotentReplay: payload["automationIdempotentReplay"] as? Bool ?? false
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

    private func parseAgentResults(_ payload: [Any]?) -> [AgentResultEntry] {
        guard let payload else { return [] }
        return payload.enumerated().compactMap { index, rawEntry in
            if let text = rawEntry as? String {
                let trimmedText = text.trimmingCharacters(in: .whitespacesAndNewlines)
                return trimmedText.isEmpty ? nil : AgentResultEntry(
                    id: "result_\(index + 1)",
                    type: "text",
                    text: trimmedText
                )
            }
            guard let entry = rawEntry as? [String: Any] else { return nil }
            let type = ((entry["type"] as? String) ?? "text")
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased()
            let columns = parseStringList(entry["columns"])
            let rows = parseTableRows(entry["rows"], columns: columns)
            return AgentResultEntry(
                id: ((entry["id"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines)).flatMap { $0.isEmpty ? nil : $0 } ?? "result_\(index + 1)",
                type: type.isEmpty ? "text" : type,
                text: (entry["text"] as? String) ?? "",
                url: (entry["url"] as? String) ?? "",
                title: (entry["title"] as? String) ?? "",
                mimeType: (entry["mimeType"] as? String) ?? "",
                fileName: ((entry["fileName"] as? String) ?? (entry["filename"] as? String) ?? ""),
                html: (entry["html"] as? String) ?? "",
                columns: columns,
                rows: rows,
                workflowName: (entry["workflowName"] as? String) ?? "",
                status: (entry["status"] as? String) ?? "",
                summary: (entry["summary"] as? String) ?? "",
                runId: (entry["runId"] as? String) ?? ""
            )
        }
    }

    private func parseStringList(_ value: Any?) -> [String] {
        guard let values = value as? [Any] else { return [] }
        return values.compactMap { item in
            if let string = item as? String {
                let trimmed = string.trimmingCharacters(in: .whitespacesAndNewlines)
                return trimmed.isEmpty ? nil : trimmed
            }
            if let map = item as? [String: Any] {
                let label = (map["title"] as? String) ??
                    (map["label"] as? String) ??
                    (map["name"] as? String) ??
                    (map["key"] as? String) ??
                    ""
                let trimmed = label.trimmingCharacters(in: .whitespacesAndNewlines)
                return trimmed.isEmpty ? nil : trimmed
            }
            return nil
        }
    }

    private func parseTableRows(_ value: Any?, columns: [String]) -> [[String]] {
        guard let rows = value as? [Any] else { return [] }
        return rows.compactMap { row in
            if let rowValues = row as? [Any] {
                let cells = rowValues.map { stringifyResultValue($0) }
                return cells.contains(where: { !$0.isEmpty }) ? cells : nil
            }
            if let rowMap = row as? [String: Any] {
                let keys = columns.isEmpty ? Array(rowMap.keys.prefix(8)) : columns
                let cells = keys.map { stringifyResultValue(rowMap[$0]) }
                return cells.contains(where: { !$0.isEmpty }) ? cells : nil
            }
            let text = stringifyResultValue(row)
            return text.isEmpty ? nil : [text]
        }
    }

    private func stringifyResultValue(_ value: Any?) -> String {
        switch value {
        case let value as String:
            return value.trimmingCharacters(in: .whitespacesAndNewlines)
        case let value as Bool:
            return value ? "true" : "false"
        case let value as NSNumber:
            return value.stringValue
        case .none:
            return ""
        default:
            return "\(value ?? "")"
        }
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
