package com.nash.skyos.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class PaymentProviderSettings(
    val connected: Boolean = false,
    val enabled: Boolean = false,
    val accountHint: String = "",
)

data class BankTransferSettings(
    val enabled: Boolean = false,
    val accountHolder: String = "",
    val iban: String = "",
    val bic: String = "",
    val bankName: String = "",
    val paymentInstructions: String = "",
) {
    val isConfigured: Boolean
        get() = accountHolder.isNotBlank() && iban.isNotBlank() && bankName.isNotBlank()
}

data class PaymentMethodsSettings(
    val stripe: PaymentProviderSettings = PaymentProviderSettings(),
    val paypal: PaymentProviderSettings = PaymentProviderSettings(),
    val klarna: PaymentProviderSettings = PaymentProviderSettings(),
    val bankTransfer: BankTransferSettings = BankTransferSettings(),
) {
    val checkoutMethodLabels: List<String>
        get() = buildList {
            if (stripe.connected && stripe.enabled) add("Stripe")
            if (paypal.connected && paypal.enabled) add("PayPal")
            if (klarna.connected && klarna.enabled) add("Klarna")
            if (bankTransfer.enabled && bankTransfer.isConfigured) add("Bankueberweisung")
        }
}

class PaymentMethodsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "appConfig"
    private val documentName = "paymentMethods"

    fun observeSettings(onChange: (Result<PaymentMethodsSettings>) -> Unit): ListenerRegistration {
        return firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onChange(Result.failure(error))
                return@addSnapshotListener
            }

            val data = snapshot?.data.orEmpty()
            onChange(Result.success(data.toPaymentMethodsSettings()))
        }
    }

    suspend fun updateSettings(settings: PaymentMethodsSettings): Result<Unit> {
        return runCatching {
            firestore.collection(collectionName).document(documentName).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
        }
    }
}

private fun Map<String, Any>.toPaymentMethodsSettings(): PaymentMethodsSettings {
    val stripe = (this["stripe"] as? Map<*, *>).orEmpty()
    val paypal = (this["paypal"] as? Map<*, *>).orEmpty()
    val klarna = (this["klarna"] as? Map<*, *>).orEmpty()
    val bankTransfer = (this["bankTransfer"] as? Map<*, *>).orEmpty()

    return PaymentMethodsSettings(
        stripe = PaymentProviderSettings(
            connected = stripe["connected"] as? Boolean ?: false,
            enabled = stripe["enabled"] as? Boolean ?: false,
            accountHint = stripe["accountHint"] as? String ?: "",
        ),
        paypal = PaymentProviderSettings(
            connected = paypal["connected"] as? Boolean ?: false,
            enabled = paypal["enabled"] as? Boolean ?: false,
            accountHint = paypal["accountHint"] as? String ?: "",
        ),
        klarna = PaymentProviderSettings(
            connected = klarna["connected"] as? Boolean ?: false,
            enabled = klarna["enabled"] as? Boolean ?: false,
            accountHint = klarna["accountHint"] as? String ?: "",
        ),
        bankTransfer = BankTransferSettings(
            enabled = bankTransfer["enabled"] as? Boolean ?: false,
            accountHolder = bankTransfer["accountHolder"] as? String ?: "",
            iban = bankTransfer["iban"] as? String ?: "",
            bic = bankTransfer["bic"] as? String ?: "",
            bankName = bankTransfer["bankName"] as? String ?: "",
            paymentInstructions = bankTransfer["paymentInstructions"] as? String ?: "",
        ),
    )
}

private fun PaymentMethodsSettings.toMap(): Map<String, Any> {
    return mapOf(
        "stripe" to mapOf(
            "connected" to stripe.connected,
            "enabled" to stripe.enabled,
            "accountHint" to stripe.accountHint.trim(),
        ),
        "paypal" to mapOf(
            "connected" to paypal.connected,
            "enabled" to paypal.enabled,
            "accountHint" to paypal.accountHint.trim(),
        ),
        "klarna" to mapOf(
            "connected" to klarna.connected,
            "enabled" to klarna.enabled,
            "accountHint" to klarna.accountHint.trim(),
        ),
        "bankTransfer" to mapOf(
            "enabled" to bankTransfer.enabled,
            "accountHolder" to bankTransfer.accountHolder.trim(),
            "iban" to bankTransfer.iban.trim(),
            "bic" to bankTransfer.bic.trim(),
            "bankName" to bankTransfer.bankName.trim(),
            "paymentInstructions" to bankTransfer.paymentInstructions.trim(),
        ),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}
