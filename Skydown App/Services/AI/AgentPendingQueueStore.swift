import Foundation

struct AgentPendingQueueTurn: Codable {
    let role: String
    let text: String
}

struct AgentPendingQueueEntry: Codable {
    let userKey: String
    let prompt: String
    let history: [AgentPendingQueueTurn]
    let mode: String
    let executeAutomation: Bool
    let assistantMessageID: String
    let createdAt: Date
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

    func entries(for userKey: String?) -> [AgentPendingQueueEntry] {
        let normalized = normalizeUserKey(userKey)
        return readEntries()
            .filter { $0.userKey == normalized }
            .sorted { $0.createdAt < $1.createdAt }
    }

    func saveEntries(_ entries: [AgentPendingQueueEntry], for userKey: String?) {
        let normalized = normalizeUserKey(userKey)
        let others = readEntries().filter { $0.userKey != normalized }
        let sanitized = entries
            .filter { !$0.prompt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .sorted { $0.createdAt < $1.createdAt }

        var merged = others + sanitized
        merged.sort { $0.createdAt > $1.createdAt }
        merged = Array(merged.prefix(maximumEntries))
        writeEntries(merged)
        pruneExpiredEntries()
    }

    func clearEntries(for userKey: String?) {
        saveEntries([], for: userKey)
    }

    private func pruneExpiredEntries() {
        let cutoff = Calendar.current.date(byAdding: .day, value: -maximumAgeDays, to: .now) ?? .distantPast
        let filtered = readEntries().filter { $0.createdAt >= cutoff }
        if filtered.count != readEntries().count {
            writeEntries(filtered)
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
}
