package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

private const val defaultInvoiceCompanyName = "Skydown OS"
private const val legacyInvoiceCompanyName = "Skydown"

data class CommerceShippingSettings(
    val domesticCost: Double = 4.90,
    val euCost: Double = 6.90,
    val internationalCost: Double = 11.90,
    val freeShippingThreshold: Double = 89.0,
    val shippingNotes: String = "",
)

data class CommerceInvoiceSettings(
    val companyName: String = defaultInvoiceCompanyName,
    val companyAddress: String = "",
    val taxNumber: String = "",
    val vatId: String = "",
    val taxRate: Double = 19.0,
    val invoicePrefix: String = "SD",
    val supportEmail: String = "skydownent@gmail.com",
)

data class CommerceSettings(
    val shipping: CommerceShippingSettings = CommerceShippingSettings(),
    val invoice: CommerceInvoiceSettings = CommerceInvoiceSettings(),
)

class CommerceSettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "appConfig"
    private val documentName = "commerceSettings"

    fun observeSettings(onChange: (Result<CommerceSettings>) -> Unit): ListenerRegistration {
        return firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onChange(Result.failure(error))
                return@addSnapshotListener
            }

            onChange(Result.success(snapshot?.data.orEmpty().toCommerceSettings()))
        }
    }

    suspend fun updateSettings(settings: CommerceSettings): Result<Unit> {
        return runCatching {
            firestore.collection(collectionName).document(documentName).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
        }
    }
}

private fun Map<String, Any>.toCommerceSettings(): CommerceSettings {
    val shipping = (this["shipping"] as? Map<*, *>).orEmpty()
    val invoice = (this["invoice"] as? Map<*, *>).orEmpty()

    return CommerceSettings(
        shipping = CommerceShippingSettings(
            domesticCost = shipping["domesticCost"].asDouble(4.90),
            euCost = shipping["euCost"].asDouble(
                shipping["internationalCost"].asDouble(6.90),
            ),
            internationalCost = shipping["internationalCost"].asDouble(11.90),
            freeShippingThreshold = shipping["freeShippingThreshold"].asDouble(89.0),
            shippingNotes = shipping["shippingNotes"] as? String ?: "",
        ),
        invoice = CommerceInvoiceSettings(
            companyName = normalizeInvoiceCompanyName(invoice["companyName"] as? String),
            companyAddress = invoice["companyAddress"] as? String ?: "",
            taxNumber = invoice["taxNumber"] as? String ?: "",
            vatId = invoice["vatId"] as? String ?: "",
            taxRate = invoice["taxRate"].asDouble(19.0),
            invoicePrefix = invoice["invoicePrefix"] as? String ?: "SD",
            supportEmail = invoice["supportEmail"] as? String ?: "skydownent@gmail.com",
        ),
    )
}

private fun CommerceSettings.toMap(): Map<String, Any> {
    return mapOf(
        "shipping" to mapOf(
            "domesticCost" to shipping.domesticCost,
            "euCost" to shipping.euCost,
            "internationalCost" to shipping.internationalCost,
            "freeShippingThreshold" to shipping.freeShippingThreshold,
            "shippingNotes" to shipping.shippingNotes.trim(),
        ),
        "invoice" to mapOf(
            "companyName" to invoice.companyName.trim(),
            "companyAddress" to invoice.companyAddress.trim(),
            "taxNumber" to invoice.taxNumber.trim(),
            "vatId" to invoice.vatId.trim(),
            "taxRate" to invoice.taxRate,
            "invoicePrefix" to invoice.invoicePrefix.trim(),
            "supportEmail" to invoice.supportEmail.trim(),
        ),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

private fun normalizeInvoiceCompanyName(value: String?): String {
    val trimmed = value?.trim().orEmpty()
    return when {
        trimmed.isEmpty() -> defaultInvoiceCompanyName
        trimmed == legacyInvoiceCompanyName -> defaultInvoiceCompanyName
        else -> trimmed
    }
}

private fun Any?.asDouble(fallback: Double): Double {
    return when (this) {
        is Number -> this.toDouble()
        is String -> this.toDoubleOrNull() ?: fallback
        else -> fallback
    }
}
