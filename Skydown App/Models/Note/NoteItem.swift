import Foundation
import FirebaseFirestore

struct NoteItem: Identifiable, Equatable {
    let id: String
    let title: String
    let content: String
    let updatedAt: Date?
    let createdAt: Date?
}

extension NoteItem {
    static func from(document: QueryDocumentSnapshot) -> NoteItem? {
        let data = document.data()
        let title = (data["title"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let content = (data["content"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !title.isEmpty || !content.isEmpty else { return nil }
        return NoteItem(
            id: document.documentID,
            title: title.isEmpty ? "Untitled" : title,
            content: content,
            updatedAt: (data["updatedAt"] as? Timestamp)?.dateValue(),
            createdAt: (data["createdAt"] as? Timestamp)?.dateValue()
        )
    }
}
