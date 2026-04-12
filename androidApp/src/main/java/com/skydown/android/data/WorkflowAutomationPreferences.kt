package com.skydown.android.data
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class WorkflowAutomationSettings(
    val provider: String = "n8n",
    val isEnabled: Boolean = false,
    val sendsUserContext: Boolean = true,
    val workflowName: String = "Skydown Automation",
    val baseUrl: String = "",
    val webhookPath: String = "",
    val authHeaderName: String = "X-Skydown-Automation-Key",
    val authHeaderValue: String = "",
    val knowledgeContext: String = "",
) {
    val resolvedWebhookUrl: String?
        get() {
            val normalizedBaseUrl = normalizeAutomationBaseUrl(baseUrl) ?: return null
            val trimmedPath = webhookPath.trim().trim('/')
            return if (trimmedPath.isEmpty()) normalizedBaseUrl else "$normalizedBaseUrl/$trimmedPath"
        }

    val isPrepared: Boolean
        get() = isEnabled && resolvedWebhookUrl != null
}

object WorkflowAutomationPreferences {
    private const val collectionName = "adminConfig"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance("us-central1")
    private val _settings = MutableStateFlow(WorkflowAutomationSettings())
    val settings: StateFlow<WorkflowAutomationSettings> = _settings.asStateFlow()

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
            _settings.value = WorkflowAutomationSettings()
        }
    }

    suspend fun saveSettings(settings: WorkflowAutomationSettings): Result<Unit> {
        return runCatching {
            val userId = requireNotNull(currentUserId) { "Keine User-UID fuer n8n-Konfiguration verfuegbar." }
            firestore.collection(collectionName).document(documentName(userId)).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
            _settings.value = settings
        }
    }

    suspend fun triggerTest(): Result<String> {
        return runCatching {
            val userId = requireNotNull(currentUserId) { "Keine User-UID fuer n8n-Test verfuegbar." }
            val result = functions
                .getHttpsCallable("triggerWorkflowAutomation")
                .call(
                    mapOf(
                        "trigger" to "admin_settings_test",
                        "source" to "android_settings",
                        "userId" to userId,
                    ),
                )
                .await()

            when (val data = result.data) {
                is String -> data
                is Map<*, *> -> data["message"] as? String
                else -> null
            }?.takeIf { it.isNotBlank() } ?: "Test an n8n gesendet."
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

                if (snapshot != null && snapshot.exists()) {
                    _settings.value = snapshot.data.orEmpty().toWorkflowAutomationSettings()
                } else {
                    _settings.value = WorkflowAutomationSettings()
                }
            }
    }

    private fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun documentName(userId: String): String = "automationN8n_$userId"
}

private fun Map<String, Any>.toWorkflowAutomationSettings(): WorkflowAutomationSettings {
    return WorkflowAutomationSettings(
        provider = (this["provider"] as? String).orEmpty().trim().ifBlank { "n8n" },
        isEnabled = this["isEnabled"] as? Boolean ?: false,
        sendsUserContext = this["sendsUserContext"] as? Boolean ?: true,
        workflowName = (this["workflowName"] as? String).orEmpty().trim().ifBlank { "Skydown Automation" },
        baseUrl = normalizeAutomationBaseUrl(this["baseURL"] as? String).orEmpty(),
        webhookPath = normalizeAutomationWebhookPath(this["webhookPath"] as? String).orEmpty(),
        authHeaderName = (this["authHeaderName"] as? String).orEmpty().trim(),
        authHeaderValue = (this["authHeaderValue"] as? String).orEmpty().trim(),
        knowledgeContext = (this["knowledgeContext"] as? String).orEmpty().trim(),
    )
}

private fun WorkflowAutomationSettings.toMap(): Map<String, Any> {
    return mapOf(
        "provider" to "n8n",
        "isEnabled" to isEnabled,
        "sendsUserContext" to sendsUserContext,
        "workflowName" to workflowName.trim().ifBlank { "Skydown Automation" },
        "baseURL" to normalizeAutomationBaseUrl(baseUrl).orEmpty(),
        "webhookPath" to normalizeAutomationWebhookPath(webhookPath).orEmpty(),
        "authHeaderName" to authHeaderName.trim(),
        "authHeaderValue" to authHeaderValue.trim(),
        "knowledgeContext" to knowledgeContext.trim(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

private fun normalizeAutomationBaseUrl(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return null
    }

    val normalized = if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
        trimmed
    } else {
        "https://$trimmed"
    }

    return normalized.trimEnd('/')
}

private fun normalizeAutomationWebhookPath(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return null
    }

    val withoutDomain = if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
        trimmed.substringAfter("://").substringAfter("/", "")
    } else {
        trimmed
    }

    return withoutDomain.trim('/').ifBlank { null }
}
