package com.nash.skyos.data
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
    val provider: String = "activepieces",
    val scope: String = "owner_global",
    val isEnabled: Boolean = false,
    val sendsUserContext: Boolean = true,
    val workflowName: String = "SkyOS Owner Activepieces Flow",
    val baseUrl: String = "",
    val webhookPath: String = "",
    val authHeaderName: String = "X-SkyOS-Automation-Key",
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
    private const val ownerDocumentName = "ownerActivepiecesFlow"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance("us-central1")
    private val _settings = MutableStateFlow(WorkflowAutomationSettings())
    val settings: StateFlow<WorkflowAutomationSettings> = _settings.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var currentUserId: String? = null
    private var currentScope: String = "user_personal"

    fun setUserMode(userId: String?, isOwner: Boolean) {
        val nextUserId = userId?.takeIf { it.isNotBlank() }
        val nextScope = if (isOwner) "owner_global" else "user_personal"
        if (nextUserId == currentUserId && nextScope == currentScope) {
            return
        }

        currentUserId = nextUserId
        currentScope = nextScope
        if (currentUserId != null) {
            startListening()
        } else {
            stopListening()
            _settings.value = WorkflowAutomationSettings()
        }
    }

    suspend fun saveSettings(settings: WorkflowAutomationSettings): Result<Unit> {
        return runCatching {
            requireNotNull(currentUserId) { "Bitte melde dich an, um Workflow-Einstellungen zu speichern." }
            firestore.collection(collectionName).document(documentName()).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
            _settings.value = settings
        }
    }

    suspend fun triggerTest(): Result<String> {
        return runCatching {
            requireNotNull(currentUserId) { "Bitte melde dich an, um den Workflow zu testen." }
            require(AppNetworkMonitor.isOnline.value) {
                "Du bist offline. Der Workflow-Test braucht eine aktive Internetverbindung."
            }
            val result = functions
                .getHttpsCallable("triggerWorkflowAutomation")
                .call(
                    mapOf(
                        "trigger" to "admin_settings_test",
                        "source" to "android_settings",
                        "automationScope" to if (currentScope == "user_personal") "personal" else "owner",
                    ),
                )
                .await()

            when (val data = result.data) {
                is String -> data
                is Map<*, *> -> data["message"] as? String
                else -> null
            }?.takeIf { it.isNotBlank() } ?: "Test an externen Workflow gesendet."
        }
    }

    private fun startListening() {
        listenerRegistration?.remove()
        listenerRegistration = firestore.collection(collectionName).document(documentName())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    _settings.value = snapshot.data.orEmpty().toWorkflowAutomationSettings(currentScope)
                } else {
                    _settings.value = emptySettingsForCurrentScope()
                }
            }
    }

    private fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
    private fun documentName(): String {
        val userId = currentUserId.orEmpty()
        return if (currentScope == "user_personal") "automationN8n_$userId" else ownerDocumentName
    }

    private fun emptySettingsForCurrentScope(): WorkflowAutomationSettings {
        return WorkflowAutomationSettings(
            provider = "activepieces",
            scope = currentScope,
            workflowName = if (currentScope == "user_personal") "Persoenlicher Workflow" else "SkyOS Owner Activepieces Flow",
        )
    }
}

private fun Map<String, Any>.toWorkflowAutomationSettings(fallbackScope: String = "user_personal"): WorkflowAutomationSettings {
    val scope = (this["scope"] as? String).orEmpty().trim().ifBlank { fallbackScope }
    val provider = (this["provider"] as? String).orEmpty().trim()
    return WorkflowAutomationSettings(
        provider = if (scope == "user_personal" && provider == "n8n") "n8n" else "activepieces",
        scope = scope,
        isEnabled = this["isEnabled"] as? Boolean ?: false,
        sendsUserContext = this["sendsUserContext"] as? Boolean ?: true,
        workflowName = (this["workflowName"] as? String).orEmpty().trim().ifBlank { "SkyOS Owner Activepieces Flow" },
        baseUrl = normalizeAutomationBaseUrl(this["baseURL"] as? String).orEmpty(),
        webhookPath = normalizeAutomationWebhookPath(this["webhookPath"] as? String).orEmpty(),
        authHeaderName = (this["authHeaderName"] as? String).orEmpty().trim(),
        authHeaderValue = (this["authHeaderValue"] as? String).orEmpty().trim(),
        knowledgeContext = (this["knowledgeContext"] as? String).orEmpty().trim(),
    )
}

private fun WorkflowAutomationSettings.toMap(): Map<String, Any> {
    val normalizedScope = if (scope == "owner_global") "owner_global" else "user_personal"
    val normalizedProvider = if (normalizedScope == "user_personal" && provider == "n8n") "n8n" else "activepieces"
    return mapOf(
        "provider" to normalizedProvider,
        "scope" to normalizedScope,
        "isEnabled" to isEnabled,
        "sendsUserContext" to sendsUserContext,
        "workflowName" to workflowName.trim().ifBlank {
            if (normalizedScope == "user_personal") "Persoenlicher Workflow" else "SkyOS Owner Activepieces Flow"
        },
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
