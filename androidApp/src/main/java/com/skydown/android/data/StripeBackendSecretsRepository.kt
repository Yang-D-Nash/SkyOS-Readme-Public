package com.skydown.android.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

data class StripeBackendSecretsStatus(
    val hasSecretKey: Boolean = false,
    val hasWebhookSecret: Boolean = false,
    val updatedAtEpochMillis: Long? = null,
) {
    val isReady: Boolean
        get() = hasSecretKey && hasWebhookSecret
}

class StripeBackendSecretsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    fun observeStatus(onChange: (Result<StripeBackendSecretsStatus>) -> Unit): ListenerRegistration {
        return firestore.collection("adminConfig").document("stripeCheckoutSecrets").addSnapshotListener { snapshot, error ->
            if (error != null) {
                onChange(Result.failure(error))
                return@addSnapshotListener
            }

            val data = snapshot?.data.orEmpty()
            onChange(Result.success(data.toStripeBackendSecretsStatus()))
        }
    }

    suspend fun saveSecrets(
        stripeSecretKey: String,
        stripeWebhookSecret: String,
    ): Result<StripeBackendSecretsStatus> {
        return runCatching {
            val response = functions
                .getHttpsCallable("configureStripeBackendSecrets")
                .call(
                    mapOf(
                        "stripeSecretKey" to stripeSecretKey.trim(),
                        "stripeWebhookSecret" to stripeWebhookSecret.trim(),
                    ),
                )
                .await()

            val data = response.data as? Map<*, *>
            val status = data?.get("status") as? Map<*, *>
            status.toStripeBackendSecretsStatus()
        }
    }
}

private fun Map<*, *>?.toStripeBackendSecretsStatus(): StripeBackendSecretsStatus {
    val data = this ?: emptyMap<Any, Any>()
    return StripeBackendSecretsStatus(
        hasSecretKey = data["hasSecretKey"] as? Boolean ?: false,
        hasWebhookSecret = data["hasWebhookSecret"] as? Boolean ?: false,
        updatedAtEpochMillis = (data["updatedAt"] as? Timestamp)?.toDate()?.time,
    )
}
