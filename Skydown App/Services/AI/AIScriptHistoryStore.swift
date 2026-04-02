import Foundation

enum AIScriptHistorySource: String, Codable, CaseIterable, Identifiable {
    case bot
    case agent

    var id: String { rawValue }

    var title: String {
        switch self {
        case .bot:
            return "Bot"
        case .agent:
            return "Agent"
        }
    }
}

enum AIScriptHistoryRetention: Int, CaseIterable, Identifiable {
    case oneDay = 1
    case threeDays = 3
    case sevenDays = 7
    case thirtyDays = 30

    var id: Int { rawValue }

    var title: String {
        switch self {
        case .oneDay:
            return "1 Tag"
        case .threeDays:
            return "3 Tage"
        case .sevenDays:
            return "7 Tage"
        case .thirtyDays:
            return "30 Tage"
        }
    }
}

struct AIScriptHistoryEntry: Identifiable, Codable, Equatable {
    let id: UUID
    let userKey: String
    let source: AIScriptHistorySource
    let prompt: String
    let response: String
    let createdAt: Date

    init(
        id: UUID = UUID(),
        userKey: String,
        source: AIScriptHistorySource,
        prompt: String,
        response: String,
        createdAt: Date = .now
    ) {
        self.id = id
        self.userKey = userKey
        self.source = source
        self.prompt = prompt
        self.response = response
        self.createdAt = createdAt
    }
}

@MainActor
final class AIScriptHistoryStore: ObservableObject {
    static let shared = AIScriptHistoryStore()

    @Published private(set) var entries: [AIScriptHistoryEntry] = []
    @Published private(set) var retention: AIScriptHistoryRetention = .threeDays

    private let entriesKey = "ai_script_history_entries"
    private let retentionKey = "ai_script_history_retention"
    private let maximumEntries = 120

    private init() {
        retention = loadRetention()
        entries = loadEntries()
        pruneExpiredEntries()
    }

    func entries(for userKey: String?, source: AIScriptHistorySource? = nil) -> [AIScriptHistoryEntry] {
        let normalizedUserKey = normalizeUserKey(userKey)
        return entries
            .filter { entry in
                entry.userKey == normalizedUserKey && (source == nil || entry.source == source)
            }
            .sorted { $0.createdAt > $1.createdAt }
    }

    func saveEntry(
        userKey: String?,
        source: AIScriptHistorySource,
        prompt: String,
        response: String
    ) {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedResponse = response.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !trimmedResponse.isEmpty else { return }

        var updatedEntries = entries
        updatedEntries.insert(
            AIScriptHistoryEntry(
                userKey: normalizeUserKey(userKey),
                source: source,
                prompt: trimmedPrompt,
                response: trimmedResponse
            ),
            at: 0
        )
        entries = Array(updatedEntries.prefix(maximumEntries))
        persistEntries()
        pruneExpiredEntries()
    }

    func deleteEntry(_ entry: AIScriptHistoryEntry) {
        entries.removeAll { $0.id == entry.id }
        persistEntries()
    }

    func clearEntries(userKey: String?, source: AIScriptHistorySource? = nil) {
        let normalizedUserKey = normalizeUserKey(userKey)
        entries.removeAll { entry in
            entry.userKey == normalizedUserKey && (source == nil || entry.source == source)
        }
        persistEntries()
    }

    func updateRetention(_ retention: AIScriptHistoryRetention) {
        self.retention = retention
        UserDefaults.standard.set(retention.rawValue, forKey: retentionKey)
        pruneExpiredEntries()
    }

    func updateRetentionDays(_ days: Int) {
        updateRetention(AIScriptHistoryRetention(rawValue: days) ?? .threeDays)
    }

    private func pruneExpiredEntries() {
        let cutoff = Calendar.current.date(byAdding: .day, value: -retention.rawValue, to: .now) ?? .distantPast
        let prunedEntries = entries
            .filter { $0.createdAt >= cutoff }
            .sorted { $0.createdAt > $1.createdAt }
        if prunedEntries != entries {
            entries = prunedEntries
            persistEntries()
        }
    }

    private func loadEntries() -> [AIScriptHistoryEntry] {
        guard let data = UserDefaults.standard.data(forKey: entriesKey) else { return [] }
        return (try? JSONDecoder().decode([AIScriptHistoryEntry].self, from: data)) ?? []
    }

    private func persistEntries() {
        guard let data = try? JSONEncoder().encode(entries) else { return }
        UserDefaults.standard.set(data, forKey: entriesKey)
    }

    private func loadRetention() -> AIScriptHistoryRetention {
        let rawValue = UserDefaults.standard.integer(forKey: retentionKey)
        return AIScriptHistoryRetention(rawValue: rawValue) ?? .threeDays
    }

    private func normalizeUserKey(_ userKey: String?) -> String {
        let trimmed = userKey?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? "guest" : trimmed.lowercased()
    }
}
