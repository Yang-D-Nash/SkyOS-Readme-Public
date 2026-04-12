package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val defaultLegalBrandName = "Skydown x 22"
private const val defaultLegalOperatorName = "Yang D. Nash - Skydown"
private const val defaultLegalRightsHolderName = "Yang D. Nash - Skydown"
private const val defaultLegalSupportEmail = "skydownent@gmail.com"
private const val defaultLegalLastUpdatedLabel = "12. April 2026"
private const val defaultLegalImprintReference = "Die vollstaendige Anbieterkennzeichnung einschliesslich ladungsfaehiger Anschrift ist im Impressum, Store-Eintrag oder Anbieterprofil bereitzuhalten."

data class LegalContentSettings(
    val brandName: String = defaultLegalBrandName,
    val operatorName: String = defaultLegalOperatorName,
    val rightsHolderName: String = defaultLegalRightsHolderName,
    val supportEmail: String = defaultLegalSupportEmail,
    val lastUpdatedLabel: String = defaultLegalLastUpdatedLabel,
    val imprintReference: String = defaultLegalImprintReference,
) {
    val resolvedBrandName: String
        get() = brandName.trim().ifBlank { defaultLegalBrandName }

    val resolvedOperatorName: String
        get() = operatorName.trim().ifBlank { defaultLegalOperatorName }

    val resolvedRightsHolderName: String
        get() = rightsHolderName.trim().ifBlank { defaultLegalRightsHolderName }

    val resolvedSupportEmail: String
        get() = supportEmail.trim().ifBlank { defaultLegalSupportEmail }

    val resolvedLastUpdatedLabel: String
        get() = lastUpdatedLabel.trim().ifBlank { defaultLegalLastUpdatedLabel }

    val resolvedImprintReference: String
        get() = imprintReference.trim().ifBlank { defaultLegalImprintReference }
}

class LegalContentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "appConfig"
    private val documentName = "legalContent"
    private val _settings = MutableStateFlow(LegalContentSettings())
    val settings: StateFlow<LegalContentSettings> = _settings.asStateFlow()
    private var listener: ListenerRegistration? = null

    init {
        startObserving()
    }

    suspend fun updateSettings(settings: LegalContentSettings): Result<Unit> {
        return runCatching {
            firestore.collection(collectionName).document(documentName).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
        }
    }

    private fun startObserving() {
        listener?.remove()
        listener = firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            _settings.value = snapshot?.data.orEmpty().toLegalContentSettings()
        }
    }
}

private fun Map<String, Any>.toLegalContentSettings(): LegalContentSettings {
    return LegalContentSettings(
        brandName = this["brandName"] as? String ?: defaultLegalBrandName,
        operatorName = this["operatorName"] as? String ?: defaultLegalOperatorName,
        rightsHolderName = this["rightsHolderName"] as? String ?: defaultLegalRightsHolderName,
        supportEmail = this["supportEmail"] as? String ?: defaultLegalSupportEmail,
        lastUpdatedLabel = this["lastUpdatedLabel"] as? String ?: defaultLegalLastUpdatedLabel,
        imprintReference = this["imprintReference"] as? String ?: defaultLegalImprintReference,
    )
}

private fun LegalContentSettings.toMap(): Map<String, Any> {
    return mapOf(
        "brandName" to resolvedBrandName,
        "operatorName" to resolvedOperatorName,
        "rightsHolderName" to resolvedRightsHolderName,
        "supportEmail" to resolvedSupportEmail,
        "lastUpdatedLabel" to resolvedLastUpdatedLabel,
        "imprintReference" to resolvedImprintReference,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}
