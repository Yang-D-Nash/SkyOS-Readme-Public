import Foundation
import FirebaseFirestore

@MainActor
final class NoteStore: ObservableObject {
    static let shared = NoteStore()

    @Published private(set) var notes: [NoteItem] = []
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

    func observeNotes(for uid: String?) {
        let normalizedUID = uid?.trimmingCharacters(in: .whitespacesAndNewlines)
        guard normalizedUID != observedUID else { return }

        listener?.remove()
        listener = nil
        observedUID = normalizedUID
        notes = []
        lastErrorMessage = nil

        guard let normalizedUID, !normalizedUID.isEmpty else { return }

        listener = firestore
            .collection("users")
            .document(normalizedUID)
            .collection("notes")
            .addSnapshotListener { [weak self] snapshot, error in
                guard let self else { return }
                Task { @MainActor in
                    if let error {
                        self.lastErrorMessage = error.localizedDescription
                        return
                    }
                    let mapped = (snapshot?.documents ?? []).compactMap(NoteItem.from(document:))
                    self.notes = Self.sortedNotes(mapped)
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
                .collection("notes")
                .getDocuments()
            notes = Self.sortedNotes(snapshot.documents.compactMap(NoteItem.from(document:)))
            lastErrorMessage = nil
        } catch {
            lastErrorMessage = error.localizedDescription
        }
    }

    func update(noteID: String, title: String, content: String) async throws {
        guard let uid = observedUID, !uid.isEmpty else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("notes")
            .document(noteID)
            .setData([
                "title": title.trimmingCharacters(in: .whitespacesAndNewlines),
                "content": content.trimmingCharacters(in: .whitespacesAndNewlines),
                "updatedAt": FieldValue.serverTimestamp()
            ], merge: true)
    }

    func delete(noteID: String) async throws {
        guard let uid = observedUID, !uid.isEmpty else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("notes")
            .document(noteID)
            .delete()
    }

    func create(title: String, content: String) async throws {
        guard let uid = observedUID, !uid.isEmpty else { return }
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedContent = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty || !normalizedContent.isEmpty else { return }

        try await firestore
            .collection("users")
            .document(uid)
            .collection("notes")
            .addDocument(data: [
                "title": normalizedTitle,
                "content": normalizedContent,
                "createdAt": FieldValue.serverTimestamp(),
                "updatedAt": FieldValue.serverTimestamp()
            ])
    }

    private static func sortedNotes(_ notes: [NoteItem]) -> [NoteItem] {
        notes.sorted { lhs, rhs in
            let lUpdated = lhs.updatedAt ?? lhs.createdAt ?? .distantPast
            let rUpdated = rhs.updatedAt ?? rhs.createdAt ?? .distantPast
            if lUpdated != rUpdated {
                return lUpdated > rUpdated
            }
            return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
        }
    }
}
