package com.nash.skyos.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

enum class TaskStatus(val rawValue: String) {
    Open("open"),
    Completed("completed");

    companion object {
        fun resolve(raw: String?): TaskStatus = entries.firstOrNull { it.rawValue == raw?.trim()?.lowercase() } ?: Open
    }
}

enum class TaskPriority(val rawValue: String) {
    Low("low"),
    Medium("normal"),
    High("high");

    companion object {
        fun resolve(raw: String?): TaskPriority {
            val normalized = raw?.trim()?.lowercase()
            if (normalized == "medium") return Medium
            return entries.firstOrNull { it.rawValue == normalized } ?: Medium
        }
    }
}

data class TaskItem(
    val id: String,
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val dueAt: Date?,
    val status: TaskStatus,
    val createdAt: Date?,
)

class TaskRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observeTasks(uid: String, onChange: (Result<List<TaskItem>>) -> Unit): ListenerRegistration {
        return firestore.collection("users")
            .document(uid)
            .collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onChange(Result.failure(error))
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val title = document.getString("title")?.trim().orEmpty()
                    if (title.isBlank()) return@mapNotNull null
                    TaskItem(
                        id = document.id,
                        title = title,
                        description = document.getString("description")?.trim().orEmpty(),
                        priority = TaskPriority.resolve(document.getString("priority")),
                        dueAt = (document.get("dueAt") as? Timestamp)?.toDate(),
                        status = TaskStatus.resolve(document.getString("status")),
                        createdAt = (document.get("createdAt") as? Timestamp)?.toDate(),
                    )
                }.sortedWith(taskComparator)
                onChange(Result.success(tasks))
            }
    }

    suspend fun refreshTasks(uid: String): List<TaskItem> {
        val snapshot = firestore.collection("users").document(uid).collection("tasks").get().await()
        return snapshot.documents.mapNotNull { document ->
            val title = document.getString("title")?.trim().orEmpty()
            if (title.isBlank()) return@mapNotNull null
            TaskItem(
                id = document.id,
                title = title,
                description = document.getString("description")?.trim().orEmpty(),
                priority = TaskPriority.resolve(document.getString("priority")),
                dueAt = (document.get("dueAt") as? Timestamp)?.toDate(),
                status = TaskStatus.resolve(document.getString("status")),
                createdAt = (document.get("createdAt") as? Timestamp)?.toDate(),
            )
        }.sortedWith(taskComparator)
    }

    suspend fun markCompleted(uid: String, taskId: String) {
        firestore.collection("users").document(uid).collection("tasks").document(taskId)
            .set(mapOf("status" to TaskStatus.Completed.rawValue, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    suspend fun markOpen(uid: String, taskId: String) {
        firestore.collection("users").document(uid).collection("tasks").document(taskId)
            .set(mapOf("status" to TaskStatus.Open.rawValue, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    suspend fun delete(uid: String, taskId: String) {
        firestore.collection("users").document(uid).collection("tasks").document(taskId).delete().await()
    }

    suspend fun create(uid: String, title: String, description: String) {
        val normalizedTitle = title.trim()
        val normalizedDescription = description.trim()
        if (normalizedTitle.isBlank()) return

        val openTasksSnapshot = firestore.collection("users")
            .document(uid)
            .collection("tasks")
            .whereEqualTo("status", TaskStatus.Open.rawValue)
            .limit(60)
            .get()
            .await()
        val dedupKey = normalizedTaskDedupKey(normalizedTitle)
        val existing = openTasksSnapshot.documents.firstOrNull { document ->
            normalizedTaskDedupKey(document.getString("title").orEmpty()) == dedupKey
        }
        if (existing != null) {
            val currentDescription = existing.getString("description")?.trim().orEmpty()
            val mergedDescription = when {
                normalizedDescription.isBlank() -> currentDescription
                currentDescription.isBlank() -> normalizedDescription
                currentDescription == normalizedDescription -> currentDescription
                else -> "$currentDescription\n\n$normalizedDescription"
            }.take(5000)
            existing.reference.set(
                mapOf(
                    "title" to normalizedTitle,
                    "description" to mergedDescription,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
            return
        }

        firestore.collection("users").document(uid).collection("tasks")
            .document()
            .set(
                mapOf(
                    "title" to normalizedTitle,
                    "description" to normalizedDescription,
                    "status" to TaskStatus.Open.rawValue,
                    "priority" to TaskPriority.Medium.rawValue,
                    "source" to "manual",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            .await()
    }

    private fun normalizedTaskDedupKey(value: String): String {
        return value.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\s_-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(180)
    }

    companion object {
        private val taskComparator = compareBy<TaskItem>(
            { it.status != TaskStatus.Open },
            { it.dueAt ?: Date(Long.MAX_VALUE) },
            { -(it.createdAt?.time ?: 0L) },
            { it.title.lowercase() },
        )
    }
}
