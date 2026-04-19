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
    let imageFileName: String?
    let createdAt: Date

    init(
        id: UUID = UUID(),
        userKey: String,
        source: AIScriptHistorySource,
        prompt: String,
        response: String,
        imageFileName: String? = nil,
        createdAt: Date = .now
    ) {
        self.id = id
        self.userKey = userKey
        self.source = source
        self.prompt = prompt
        self.response = response
        self.imageFileName = imageFileName
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
    private let visualImageDirectoryName = "ai_visual_history"
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
        response: String,
        imageData: Data? = nil
    ) {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedResponse = response.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPrompt.isEmpty, !trimmedResponse.isEmpty else { return }

        let entryID = UUID()
        let imageFileName = imageData.flatMap { storeImageData($0, for: entryID) }
        var updatedEntries = entries
        updatedEntries.insert(
            AIScriptHistoryEntry(
                id: entryID,
                userKey: normalizeUserKey(userKey),
                source: source,
                prompt: trimmedPrompt,
                response: trimmedResponse,
                imageFileName: imageFileName
            ),
            at: 0
        )
        let trimmedEntries = Array(updatedEntries.prefix(maximumEntries))
        let overflowEntries = Array(updatedEntries.dropFirst(maximumEntries))
        removeStoredImages(for: overflowEntries)
        entries = trimmedEntries
        persistEntries()
        pruneExpiredEntries()
    }

    func deleteEntry(_ entry: AIScriptHistoryEntry) {
        entries.removeAll { $0.id == entry.id }
        removeStoredImage(for: entry)
        persistEntries()
    }

    func clearEntries(userKey: String?, source: AIScriptHistorySource? = nil) {
        let normalizedUserKey = normalizeUserKey(userKey)
        let removedEntries = entries.filter { entry in
            entry.userKey == normalizedUserKey && (source == nil || entry.source == source)
        }
        entries.removeAll { entry in
            entry.userKey == normalizedUserKey && (source == nil || entry.source == source)
        }
        removeStoredImages(for: removedEntries)
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

    func imageData(for entry: AIScriptHistoryEntry) -> Data? {
        guard let imageFileName = entry.imageFileName,
              let imageURL = storedImageURL(for: imageFileName) else { return nil }
        return try? Data(contentsOf: imageURL)
    }

    private func pruneExpiredEntries() {
        let cutoff = Calendar.current.date(byAdding: .day, value: -retention.rawValue, to: .now) ?? .distantPast
        let retainedEntries = entries
            .filter { $0.createdAt >= cutoff }
            .sorted { $0.createdAt > $1.createdAt }
        if retainedEntries != entries {
            let removedEntries = entries.filter { existingEntry in
                retainedEntries.contains(where: { $0.id == existingEntry.id }) == false
            }
            removeStoredImages(for: removedEntries)
            entries = retainedEntries
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

    private func storeImageData(_ imageData: Data, for entryID: UUID) -> String? {
        let fileName = "\(entryID.uuidString).img"
        guard let imageURL = storedImageURL(for: fileName, createDirectory: true) else { return nil }
        do {
            try imageData.write(to: imageURL, options: .atomic)
            return fileName
        } catch {
            return nil
        }
    }

    private func removeStoredImages(for entries: [AIScriptHistoryEntry]) {
        entries.forEach(removeStoredImage(for:))
    }

    private func removeStoredImage(for entry: AIScriptHistoryEntry) {
        guard let imageFileName = entry.imageFileName,
              let imageURL = storedImageURL(for: imageFileName) else { return }
        try? FileManager.default.removeItem(at: imageURL)
    }

    private func storedImageURL(for fileName: String, createDirectory: Bool = false) -> URL? {
        guard let directoryURL = visualImageDirectoryURL(createIfNeeded: createDirectory) else { return nil }
        return directoryURL.appendingPathComponent(fileName)
    }

    private func visualImageDirectoryURL(createIfNeeded: Bool) -> URL? {
        guard let baseURL = try? FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: createIfNeeded
        ) else {
            return nil
        }

        let directoryURL = baseURL.appendingPathComponent(visualImageDirectoryName, isDirectory: true)
        if createIfNeeded, FileManager.default.fileExists(atPath: directoryURL.path) == false {
            try? FileManager.default.createDirectory(at: directoryURL, withIntermediateDirectories: true)
        }
        return directoryURL
    }
}
