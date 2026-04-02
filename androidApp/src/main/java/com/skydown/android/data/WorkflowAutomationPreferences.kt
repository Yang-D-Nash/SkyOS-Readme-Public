package com.skydown.android.data

import android.content.Context
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
    private const val legacyDocumentName = "automationN8n"

    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance("us-central1")
    private val _settings = MutableStateFlow(WorkflowAutomationSettings())
    val settings: StateFlow<WorkflowAutomationSettings> = _settings.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var currentAdminId: String? = null

    fun initialize(context: Context) {
        // Legacy no-op. The Automation section is now Firestore-backed.
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    fun setAdminMode(adminId: String?) {
        if (adminId == currentAdminId) {
            return
        }

        currentAdminId = adminId?.takeIf { it.isNotBlank() }
        if (currentAdminId != null) {
            startListening()
        } else {
            stopListening()
            _settings.value = WorkflowAutomationSettings()
        }
    }

    suspend fun saveSettings(settings: WorkflowAutomationSettings): Result<Unit> {
        return runCatching {
            val adminId = requireNotNull(currentAdminId) { "Keine Admin-UID fuer n8n-Konfiguration verfuegbar." }
            firestore.collection(collectionName).document(documentName(adminId)).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
            _settings.value = settings
        }
    }

    suspend fun triggerTest(): Result<String> {
        return runCatching {
            val adminId = requireNotNull(currentAdminId) { "Keine Admin-UID fuer n8n-Test verfuegbar." }
            val result = functions
                .getHttpsCallable("triggerWorkflowAutomation")
                .call(
                    mapOf(
                        "trigger" to "admin_settings_test",
                        "source" to "android_settings",
                        "userId" to adminId,
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
        val adminId = currentAdminId ?: return
        listenerRegistration = firestore.collection(collectionName).document(documentName(adminId))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    _settings.value = snapshot.data.orEmpty().toWorkflowAutomationSettings()
                } else {
                    firestore.collection(collectionName).document(legacyDocumentName).get()
                        .addOnSuccessListener { fallback ->
                            _settings.value = fallback.data.orEmpty().toWorkflowAutomationSettings()
                        }
                }
            }
    }

    private fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun documentName(adminId: String): String = "automationN8n_$adminId"
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
