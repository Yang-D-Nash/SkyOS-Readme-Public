package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val defaultLegalBrandName = "22xSky"
private const val defaultLegalOperatorName = "Ngoc Anh Nguyen (Yang D. Nash - Skydown)"
private const val defaultLegalRightsHolderName = "Ngoc Anh Nguyen (Yang D. Nash - Skydown)"
private const val defaultLegalSupportEmail = "skydownent@gmail.com"
private const val defaultLegalLastUpdatedLabel = "15. April 2026"
private const val defaultLegalImprintReference = "Anbieterkennzeichnung: Ngoc Anh Nguyen, Yang D. Nash - Skydown, Erich-Plate-Weg 44, 22419 Hamburg, Deutschland. Kontakt: skydownent@gmail.com."
private const val defaultMasterNumberMeaning = "Die Meisterzahl 22 gilt als Master Builder: visionaer, praktisch und umsetzungsstark. Sie verbindet Inspiration mit Disziplin und macht aus Ideen reale, belastbare Strukturen."
private const val defaultBrandManifesto = "Dort, wo der Himmel faellt, beginnt unser Denken.\nWas zerbricht, offenbart Tiefe - nicht Verlust.\nWir hoeren auf das, was nicht laut ist: Wandel, Stille, Sinn.\nUnser Handeln wurzelt im Inneren, wo Klarheit entsteht.\nNicht im Machen liegt unsere Kraft, sondern im Verstehen.\nDenn wir glauben: Der Himmel faellt nicht auf uns - er oeffnet sich in uns."
private const val defaultSymbolicNumericCode = "1337-514-731"
private const val defaultSymbolicLeetCode = "7H3_F4LL_0F_H34/3N"
private const val defaultSymbolicCodeExplanation = "7H3 steht fuer THE, F4LL fuer FALL, 0F fuer OF und H34/3N fuer HEAVEN. Der Code symbolisiert den Fall des Himmels als innere Oeffnung - wie ein Schluessel zu verborgener Erkenntnis. Alternative Codes: 731-4177-0V3R-H34/3N oder 1337-514-731."

data class LegalContentSettings(
    val brandName: String = defaultLegalBrandName,
    val operatorName: String = defaultLegalOperatorName,
    val rightsHolderName: String = defaultLegalRightsHolderName,
    val supportEmail: String = defaultLegalSupportEmail,
    val lastUpdatedLabel: String = defaultLegalLastUpdatedLabel,
    val imprintReference: String = defaultLegalImprintReference,
    val masterNumberMeaning: String = defaultMasterNumberMeaning,
    val brandManifesto: String = defaultBrandManifesto,
    val symbolicNumericCode: String = defaultSymbolicNumericCode,
    val symbolicLeetCode: String = defaultSymbolicLeetCode,
    val symbolicCodeExplanation: String = defaultSymbolicCodeExplanation,
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

    val resolvedMasterNumberMeaning: String
        get() = masterNumberMeaning.trim().ifBlank { defaultMasterNumberMeaning }

    val resolvedBrandManifesto: String
        get() = brandManifesto.trim().ifBlank { defaultBrandManifesto }

    val resolvedSymbolicNumericCode: String
        get() = symbolicNumericCode.trim().ifBlank { defaultSymbolicNumericCode }

    val resolvedSymbolicLeetCode: String
        get() = symbolicLeetCode.trim().ifBlank { defaultSymbolicLeetCode }

    val resolvedSymbolicCodeExplanation: String
        get() = symbolicCodeExplanation.trim().ifBlank { defaultSymbolicCodeExplanation }
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
        masterNumberMeaning = this["masterNumberMeaning"] as? String ?: defaultMasterNumberMeaning,
        brandManifesto = this["brandManifesto"] as? String ?: defaultBrandManifesto,
        symbolicNumericCode = this["symbolicNumericCode"] as? String ?: defaultSymbolicNumericCode,
        symbolicLeetCode = this["symbolicLeetCode"] as? String ?: defaultSymbolicLeetCode,
        symbolicCodeExplanation = this["symbolicCodeExplanation"] as? String ?: defaultSymbolicCodeExplanation,
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
        "masterNumberMeaning" to resolvedMasterNumberMeaning,
        "brandManifesto" to resolvedBrandManifesto,
        "symbolicNumericCode" to resolvedSymbolicNumericCode,
        "symbolicLeetCode" to resolvedSymbolicLeetCode,
        "symbolicCodeExplanation" to resolvedSymbolicCodeExplanation,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}
