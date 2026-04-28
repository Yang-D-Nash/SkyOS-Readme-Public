import Foundation
import FirebaseFirestore

@MainActor
final class TaskStore: ObservableObject {
    static let shared = TaskStore()

    @Published private(set) var tasks: [TaskItem] = []
    @Published private(set) var lastErrorMessage: String?

    private let firestore: Firestore
    private var listener: ListenerRegistration?
    private var observedUID: String?

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    deinit {
        listener?.remove()
    }

    func observeTasks(for uid: String?) {
        let normalizedUID = uid?.trimmingCharacters(in: .whitespacesAndNewlines)
        guard normalizedUID != observedUID else { return }

        listener?.remove()
        listener = nil
        observedUID = normalizedUID
        tasks = []
        lastErrorMessage = nil

        guard let normalizedUID, !normalizedUID.isEmpty else { return }

        listener = firestore
            .collection("users")
            .document(normalizedUID)
            .collection("tasks")
            .addSnapshotListener { [weak self] snapshot, error in
                guard let self else { return }
                Task { @MainActor in
                    if let error {
                        self.lastErrorMessage = error.localizedDescription
                        return
                    }
                    let mapped = (snapshot?.documents ?? []).compactMap(TaskItem.from(document:))
                    self.tasks = Self.sortedTasks(mapped)
                    self.lastErrorMessage = nil
                }
            }
    }

    func refresh() async {
        guard let uid = observedUID, !uid.isEmpty else { return }
        do {
            let snapshot = try await firestore
                .collection("users")
                .document(uid)
                .collection("tasks")
                .getDocuments()
            let mapped = snapshot.documents.compactMap(TaskItem.from(document:))
            tasks = Self.sortedTasks(mapped)
            lastErrorMessage = nil
        } catch {
            lastErrorMessage = error.localizedDescription
        }
    }

    func markCompleted(taskID: String) async throws {
        guard let uid = observedUID, !uid.isEmpty else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("tasks")
            .document(taskID)
            .setData([
                "status": TaskStatus.completed.rawValue,
                "updatedAt": FieldValue.serverTimestamp()
            ], merge: true)
    }

    func markOpen(taskID: String) async throws {
        guard let uid = observedUID, !uid.isEmpty else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("tasks")
            .document(taskID)
            .setData([
                "status": TaskStatus.open.rawValue,
                "updatedAt": FieldValue.serverTimestamp()
            ], merge: true)
    }

    func delete(taskID: String) async throws {
        guard let uid = observedUID, !uid.isEmpty else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("tasks")
            .document(taskID)
            .delete()
    }

    func create(title: String, details: String = "") async throws {
        guard let uid = observedUID, !uid.isEmpty else { return }
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedDetails = details.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty else { return }

        if let existingOpenTask = tasks.first(where: {
            $0.status == .open && Self.normalizedTaskDedupKey($0.title) == Self.normalizedTaskDedupKey(normalizedTitle)
        }) {
            var mergedDescription = existingOpenTask.description.trimmingCharacters(in: .whitespacesAndNewlines)
            if !normalizedDetails.isEmpty, mergedDescription != normalizedDetails {
                mergedDescription = mergedDescription.isEmpty ? normalizedDetails : "\(mergedDescription)\n\n\(normalizedDetails)"
            }
            try await firestore
                .collection("users")
                .document(uid)
                .collection("tasks")
                .document(existingOpenTask.id)
                .setData([
                    "title": normalizedTitle,
                    "description": String(mergedDescription.prefix(5000)),
                    "updatedAt": FieldValue.serverTimestamp()
                ], merge: true)
            return
        }

        try await firestore
            .collection("users")
            .document(uid)
            .collection("tasks")
            .addDocument(data: [
                "title": normalizedTitle,
                "description": normalizedDetails,
                "status": TaskStatus.open.rawValue,
                "priority": TaskPriority.medium.rawValue,
                "source": "manual",
                "createdAt": FieldValue.serverTimestamp(),
                "updatedAt": FieldValue.serverTimestamp()
            ])
    }

    private static func normalizedTaskDedupKey(_ value: String) -> String {
        let lowered = value
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        let scalars = lowered.unicodeScalars.map { scalar -> Character in
            let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: " _-"))
            return allowed.contains(scalar) ? Character(scalar) : " "
        }
        let collapsed = String(scalars)
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return String(collapsed.prefix(180))
    }

    private static func sortedTasks(_ tasks: [TaskItem]) -> [TaskItem] {
        tasks.sorted { lhs, rhs in
            if lhs.status != rhs.status {
                return lhs.status == .open
            }

            switch (lhs.dueAt, rhs.dueAt) {
            case let (l?, r?):
                if l != r { return l < r }
            case (_?, nil):
                return true
            case (nil, _?):
                return false
            case (nil, nil):
                break
            }

            let lCreated = lhs.createdAt ?? .distantPast
            let rCreated = rhs.createdAt ?? .distantPast
            if lCreated != rCreated {
                return lCreated > rCreated
            }
            return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
        }
    }
}
