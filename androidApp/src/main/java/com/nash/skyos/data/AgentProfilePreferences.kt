package com.nash.skyos.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class AgentProfileSettings(
    val isEnabled: Boolean = false,
    val roleLabel: String = "",
    val skillProfile: String = "",
    val outputFormat: String = "",
    val guardrails: String = "",
    val knowledgeContext: String = "",
) {
    val isConfigured: Boolean
        get() = isEnabled && listOf(
            roleLabel,
            skillProfile,
            outputFormat,
            guardrails,
            knowledgeContext,
        ).any { it.isNotBlank() }
}

object AgentProfilePreferences {
    private const val collectionName = "adminConfig"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val _settings = MutableStateFlow(AgentProfileSettings())
    val settings: StateFlow<AgentProfileSettings> = _settings.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var currentUserId: String? = null

    fun setUserMode(userId: String?) {
        if (userId == currentUserId) {
            return
        }

        currentUserId = userId?.takeIf { it.isNotBlank() }
        if (currentUserId != null) {
            startListening()
        } else {
            stopListening()
            _settings.value = AgentProfileSettings()
        }
    }

    suspend fun saveSettings(settings: AgentProfileSettings): Result<Unit> {
        return runCatching {
            val userId = requireNotNull(currentUserId) { "Keine User-UID fuer Agent-Profil verfuegbar." }
            val sanitizedSettings = settings.sanitized()
            firestore.collection(collectionName).document(documentName(userId)).set(
                sanitizedSettings.toMap(),
                SetOptions.merge(),
            ).await()
            _settings.value = sanitizedSettings
        }
    }

    private fun startListening() {
        listenerRegistration?.remove()
        val userId = currentUserId ?: return
        listenerRegistration = firestore.collection(collectionName).document(documentName(userId))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                _settings.value = if (snapshot != null && snapshot.exists()) {
                    snapshot.data.orEmpty().toAgentProfileSettings()
                } else {
                    AgentProfileSettings()
                }
            }
    }

    private fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun documentName(userId: String): String = "agentProfile_$userId"
}

private fun Map<String, Any>.toAgentProfileSettings(): AgentProfileSettings {
    return AgentProfileSettings(
        isEnabled = this["isEnabled"] as? Boolean ?: false,
        roleLabel = normalizeAgentProfileText(this["roleLabel"], maxLength = MAX_ROLE_LABEL_LENGTH),
        skillProfile = normalizeAgentProfileText(this["skillProfile"], maxLength = MAX_SKILL_PROFILE_LENGTH),
        outputFormat = normalizeAgentProfileText(this["outputFormat"], maxLength = MAX_OUTPUT_FORMAT_LENGTH),
        guardrails = normalizeAgentProfileText(this["guardrails"], maxLength = MAX_GUARDRAILS_LENGTH),
        knowledgeContext = normalizeAgentProfileText(this["knowledgeContext"], maxLength = MAX_KNOWLEDGE_CONTEXT_LENGTH),
    )
}

private fun AgentProfileSettings.toMap(): Map<String, Any> {
    return mapOf(
        "isEnabled" to isEnabled,
        "roleLabel" to roleLabel,
        "skillProfile" to skillProfile,
        "outputFormat" to outputFormat,
        "guardrails" to guardrails,
        "knowledgeContext" to knowledgeContext,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

private fun AgentProfileSettings.sanitized(): AgentProfileSettings {
    return copy(
        roleLabel = normalizeAgentProfileText(roleLabel, maxLength = MAX_ROLE_LABEL_LENGTH),
        skillProfile = normalizeAgentProfileText(skillProfile, maxLength = MAX_SKILL_PROFILE_LENGTH),
        outputFormat = normalizeAgentProfileText(outputFormat, maxLength = MAX_OUTPUT_FORMAT_LENGTH),
        guardrails = normalizeAgentProfileText(guardrails, maxLength = MAX_GUARDRAILS_LENGTH),
        knowledgeContext = normalizeAgentProfileText(knowledgeContext, maxLength = MAX_KNOWLEDGE_CONTEXT_LENGTH),
    )
}

private fun normalizeAgentProfileText(value: Any?, maxLength: Int): String {
    return (value as? String)
        .orEmpty()
        .trim()
        .take(maxLength)
}

private const val MAX_ROLE_LABEL_LENGTH = 240
private const val MAX_SKILL_PROFILE_LENGTH = 12000
private const val MAX_OUTPUT_FORMAT_LENGTH = 4000
private const val MAX_GUARDRAILS_LENGTH = 4000
private const val MAX_KNOWLEDGE_CONTEXT_LENGTH = 4000
