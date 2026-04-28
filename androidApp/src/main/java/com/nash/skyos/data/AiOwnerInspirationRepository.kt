package com.nash.skyos.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class AiOwnerInspirationEntry(
    val id: String,
    val title: String,
    val details: String,
    val tags: List<String>,
    val isPublished: Boolean,
) {
    companion object {
        fun empty(): AiOwnerInspirationEntry = AiOwnerInspirationEntry(
            id = java.util.UUID.randomUUID().toString().lowercase(),
            title = "",
            details = "",
            tags = emptyList(),
            isPublished = false,
        )
    }
}

class AiOwnerInspirationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "adminConfig"
    private val documentName = "aiStudioOwnerInspiration"

    fun observeEntries(onChange: (Result<List<AiOwnerInspirationEntry>>) -> Unit): ListenerRegistration {
        return firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onChange(Result.failure(error))
                return@addSnapshotListener
            }
            onChange(Result.success(snapshot?.data.orEmpty().toOwnerInspirationEntries()))
        }
    }

    suspend fun updateEntries(entries: List<AiOwnerInspirationEntry>): Result<Unit> {
        return runCatching {
            firestore.collection(collectionName).document(documentName).set(
                mapOf(
                    "entries" to entries.toFirestoreEntries(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }
}

private fun Map<String, Any>.toOwnerInspirationEntries(): List<AiOwnerInspirationEntry> {
    val rawEntries = this["entries"] as? List<*> ?: return emptyList()
    val seen = linkedSetOf<String>()
    val normalized = mutableListOf<AiOwnerInspirationEntry>()
    rawEntries.forEachIndexed { index, raw ->
        val map = raw as? Map<*, *> ?: return@forEachIndexed
        val title = normalizeOwnerInspirationText(map["title"] as? String).take(MAX_OWNER_INSPIRATION_LENGTH)
        val details = normalizeOwnerInspirationText(map["details"] as? String).take(MAX_OWNER_INSPIRATION_LENGTH)
        if (title.isBlank() || details.isBlank()) return@forEachIndexed
        val id = normalizeOwnerInspirationId(map["id"] as? String, title, index)
        if (!seen.add(id)) return@forEachIndexed
        normalized += AiOwnerInspirationEntry(
            id = id,
            title = title,
            details = details,
            tags = normalizeOwnerInspirationTags(map["tags"]),
            isPublished = map["isPublished"] as? Boolean ?: false,
        )
        if (normalized.size >= 120) return@forEachIndexed
    }
    return normalized
}

private fun List<AiOwnerInspirationEntry>.toFirestoreEntries(): List<Map<String, Any>> {
    return this.asSequence()
        .take(120)
        .mapNotNull { entry ->
            val title = normalizeOwnerInspirationText(entry.title).take(MAX_OWNER_INSPIRATION_LENGTH)
            val details = normalizeOwnerInspirationText(entry.details).take(MAX_OWNER_INSPIRATION_LENGTH)
            if (title.isBlank() || details.isBlank()) return@mapNotNull null
            mapOf(
                "id" to normalizeOwnerInspirationId(entry.id, title, 0),
                "title" to title,
                "details" to details,
                "tags" to normalizeOwnerInspirationTags(entry.tags),
                "isPublished" to entry.isPublished,
            )
        }
        .toList()
}

private fun normalizeOwnerInspirationId(value: String?, fallback: String, index: Int): String {
    val raw = value?.trim().orEmpty().ifBlank { fallback.ifBlank { "entry-${index + 1}" } }
    val normalized = raw.lowercase()
        .replace(Regex("[^a-z0-9_-]+"), "-")
        .replace(Regex("-{2,}"), "-")
        .trim('-', '_')
        .take(80)
    return if (normalized.isBlank()) "entry-${index + 1}" else normalized
}

private fun normalizeOwnerInspirationTags(raw: Any?): List<String> {
    val source = when (raw) {
        is List<*> -> raw.mapNotNull { it as? String }
        else -> emptyList()
    }
    return source.map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .map { it.take(40) }
        .distinct()
        .take(12)
}

private fun normalizeOwnerInspirationText(value: String?): String {
    return value?.trim().orEmpty().take(MAX_OWNER_INSPIRATION_LENGTH)
}

private const val MAX_OWNER_INSPIRATION_LENGTH = 12000
