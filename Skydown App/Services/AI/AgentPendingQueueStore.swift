import Foundation

struct AgentPendingQueueTurn: Codable, Equatable {
    let role: String
    let text: String
}

struct AgentPendingQueueEntry: Codable, Equatable {
    let userKey: String
    let sessionID: String
    let prompt: String
    let history: [AgentPendingQueueTurn]
    let mode: String
    let aiLevel: String
    let executeAutomation: Bool
    let automationScope: String
    let assistantMessageID: String
    let createdAt: Date
    let attachments: [AgentOutboundAttachment]

    init(
        userKey: String,
        sessionID: String,
        prompt: String,
        history: [AgentPendingQueueTurn],
        mode: String,
        aiLevel: String,
        executeAutomation: Bool,
        automationScope: String = "owner",
        assistantMessageID: String,
        createdAt: Date,
        attachments: [AgentOutboundAttachment] = []
    ) {
        self.userKey = userKey
        self.sessionID = sessionID
        self.prompt = prompt
        self.history = history
        self.mode = mode
        self.aiLevel = aiLevel
        self.executeAutomation = executeAutomation
        self.automationScope = automationScope
        self.assistantMessageID = assistantMessageID
        self.createdAt = createdAt
        self.attachments = attachments
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        userKey = try container.decode(String.self, forKey: .userKey)
        sessionID = try container.decodeIfPresent(String.self, forKey: .sessionID) ?? ""
        prompt = try container.decode(String.self, forKey: .prompt)
        history = try container.decode([AgentPendingQueueTurn].self, forKey: .history)
        mode = try container.decode(String.self, forKey: .mode)
        aiLevel = try container.decodeIfPresent(String.self, forKey: .aiLevel) ?? AIExperienceLevel.standard.rawValue
        executeAutomation = try container.decode(Bool.self, forKey: .executeAutomation)
        automationScope = try container.decodeIfPresent(String.self, forKey: .automationScope) ?? "owner"
        assistantMessageID = try container.decode(String.self, forKey: .assistantMessageID)
        createdAt = try container.decode(Date.self, forKey: .createdAt)
        attachments = try container.decodeIfPresent([AgentOutboundAttachment].self, forKey: .attachments) ?? []
    }

    private enum CodingKeys: String, CodingKey {
        case userKey
        case sessionID
        case prompt
        case history
        case mode
        case aiLevel
        case executeAutomation
        case automationScope
        case assistantMessageID
        case createdAt
        case attachments
    }
}

@MainActor
final class AgentPendingQueueStore {
    static let shared = AgentPendingQueueStore()

    private let key = "agent_pending_queue_entries"
    private let maximumEntries = 80
    private let maximumAgeDays = 14
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        pruneExpiredEntries()
    }

    func entries(for userKey: String?, sessionID: String?) -> [AgentPendingQueueEntry] {
        let normalizedUserKey = normalizeUserKey(userKey)
        let normalizedSessionID = normalizeSessionID(sessionID)
        return readEntries()
            .filter {
                $0.userKey == normalizedUserKey &&
                normalizeSessionID($0.sessionID) == normalizedSessionID
            }
            .sorted { $0.createdAt < $1.createdAt }
    }

    func saveEntries(_ entries: [AgentPendingQueueEntry], for userKey: String?, sessionID: String?) {
        let normalizedUserKey = normalizeUserKey(userKey)
        let normalizedSessionID = normalizeSessionID(sessionID)
        let others = readEntries().filter {
            !($0.userKey == normalizedUserKey && normalizeSessionID($0.sessionID) == normalizedSessionID)
        }
        let sanitized = entries
            .filter { !$0.prompt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .map {
                AgentPendingQueueEntry(
                    userKey: normalizedUserKey,
                    sessionID: normalizedSessionID,
                    prompt: $0.prompt,
                    history: $0.history,
                    mode: $0.mode,
                    aiLevel: $0.aiLevel,
                    executeAutomation: $0.executeAutomation,
                    automationScope: $0.automationScope,
                    assistantMessageID: $0.assistantMessageID,
                    createdAt: $0.createdAt,
                    attachments: $0.attachments
                )
            }
            .sorted { $0.createdAt < $1.createdAt }

        var merged = others + sanitized
        merged.sort { $0.createdAt > $1.createdAt }
        merged = Array(merged.prefix(maximumEntries))
        writeEntries(merged)
        pruneExpiredEntries()
    }

    func clearEntries(for userKey: String?, sessionID: String?) {
        saveEntries([], for: userKey, sessionID: sessionID)
    }

    func migrateLegacyEntries(for userKey: String?, to sessionID: String?) {
        let normalizedUserKey = normalizeUserKey(userKey)
        let normalizedSessionID = normalizeSessionID(sessionID)
        guard !normalizedSessionID.isEmpty else { return }

        let migratedEntries = readEntries().map { entry in
            guard entry.userKey == normalizedUserKey,
                  normalizeSessionID(entry.sessionID).isEmpty else {
                return entry
            }

            return AgentPendingQueueEntry(
                userKey: normalizedUserKey,
                sessionID: normalizedSessionID,
                prompt: entry.prompt,
                history: entry.history,
                mode: entry.mode,
                aiLevel: entry.aiLevel,
                executeAutomation: entry.executeAutomation,
                automationScope: entry.automationScope,
                assistantMessageID: entry.assistantMessageID,
                createdAt: entry.createdAt,
                attachments: entry.attachments
            )
        }

        if migratedEntries != readEntries() {
            writeEntries(migratedEntries)
        }
    }

    private func pruneExpiredEntries() {
        let cutoff = Calendar.current.date(byAdding: .day, value: -maximumAgeDays, to: .now) ?? .distantPast
        let existingEntries = readEntries()
        let filteredEntries = existingEntries.filter { $0.createdAt >= cutoff }
        if filteredEntries != existingEntries {
            writeEntries(filteredEntries)
        }
    }

    private func readEntries() -> [AgentPendingQueueEntry] {
        guard let data = defaults.data(forKey: key) else { return [] }
        return (try? JSONDecoder().decode([AgentPendingQueueEntry].self, from: data)) ?? []
    }

    private func writeEntries(_ entries: [AgentPendingQueueEntry]) {
        guard let data = try? JSONEncoder().encode(entries) else { return }
        defaults.set(data, forKey: key)
    }

    private func normalizeUserKey(_ userKey: String?) -> String {
        let trimmed = userKey?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? "guest" : trimmed.lowercased()
    }

    private func normalizeSessionID(_ sessionID: String?) -> String {
        sessionID?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() ?? ""
    }
}
