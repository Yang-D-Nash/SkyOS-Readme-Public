import Foundation
import FirebaseFirestore

enum TaskPriority: String, CaseIterable {
    case low
    case medium
    case high

    var localizedLabel: String {
        switch self {
        case .low: return "Low"
        case .medium: return "Medium"
        case .high: return "High"
        }
    }
}

enum TaskStatus: String {
    case open
    case completed
}

struct TaskItem: Identifiable, Equatable {
    let id: String
    let title: String
    let description: String
    let priority: TaskPriority
    let dueAt: Date?
    let status: TaskStatus
    let createdAt: Date?
}

extension TaskItem {
    static func from(document: QueryDocumentSnapshot) -> TaskItem? {
        let data = document.data()
        let rawTitle = (data["title"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !rawTitle.isEmpty else { return nil }

        let rawPriority = (data["priority"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
        let rawStatus = (data["status"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""

        return TaskItem(
            id: document.documentID,
            title: rawTitle,
            description: (data["description"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
            priority: TaskPriority(rawValue: rawPriority) ?? .medium,
            dueAt: (data["dueAt"] as? Timestamp)?.dateValue(),
            status: TaskStatus(rawValue: rawStatus) ?? .open,
            createdAt: (data["createdAt"] as? Timestamp)?.dateValue()
        )
    }
}
