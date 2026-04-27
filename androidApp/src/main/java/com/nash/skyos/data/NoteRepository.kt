package com.nash.skyos.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

data class NoteItem(
    val id: String,
    val title: String,
    val content: String,
    val updatedAt: Date?,
    val createdAt: Date?,
)

class NoteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observeNotes(uid: String, onChange: (Result<List<NoteItem>>) -> Unit): ListenerRegistration {
        return firestore.collection("users")
            .document(uid)
            .collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onChange(Result.failure(error))
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val title = document.getString("title")?.trim().orEmpty()
                    val content = document.getString("content")?.trim().orEmpty()
                    if (title.isBlank() && content.isBlank()) return@mapNotNull null
                    NoteItem(
                        id = document.id,
                        title = if (title.isBlank()) "Untitled" else title,
                        content = content,
                        updatedAt = (document.get("updatedAt") as? Timestamp)?.toDate(),
                        createdAt = (document.get("createdAt") as? Timestamp)?.toDate(),
                    )
                }.sortedWith(compareByDescending<NoteItem> { it.updatedAt ?: it.createdAt ?: Date(0) }.thenBy { it.title.lowercase() })
                onChange(Result.success(notes))
            }
    }

    suspend fun refreshNotes(uid: String): List<NoteItem> {
        val snapshot = firestore.collection("users").document(uid).collection("notes").get().await()
        return snapshot.documents.mapNotNull { document ->
            val title = document.getString("title")?.trim().orEmpty()
            val content = document.getString("content")?.trim().orEmpty()
            if (title.isBlank() && content.isBlank()) return@mapNotNull null
            NoteItem(
                id = document.id,
                title = if (title.isBlank()) "Untitled" else title,
                content = content,
                updatedAt = (document.get("updatedAt") as? Timestamp)?.toDate(),
                createdAt = (document.get("createdAt") as? Timestamp)?.toDate(),
            )
        }.sortedWith(compareByDescending<NoteItem> { it.updatedAt ?: it.createdAt ?: Date(0) }.thenBy { it.title.lowercase() })
    }

    suspend fun updateNote(uid: String, noteId: String, title: String, content: String) {
        val normalizedTitle = title.trim()
        val normalizedContent = content.trim()
        val safeTitle = normalizedTitle.ifBlank { "Untitled" }
        firestore.collection("users").document(uid).collection("notes").document(noteId)
            .set(
                mapOf(
                    "title" to safeTitle,
                    "content" to normalizedContent,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun delete(uid: String, noteId: String) {
        firestore.collection("users").document(uid).collection("notes").document(noteId).delete().await()
    }

    suspend fun createNote(uid: String, title: String, content: String) {
        val normalizedTitle = title.trim()
        val normalizedContent = content.trim()
        if (normalizedTitle.isBlank() && normalizedContent.isBlank()) return
        val safeTitle = normalizedTitle.ifBlank { "Untitled" }

        firestore.collection("users").document(uid).collection("notes")
            .document()
            .set(
                mapOf(
                    "title" to safeTitle,
                    "content" to normalizedContent,
                    "source" to "manual",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            .await()
    }
}
