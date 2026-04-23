package com.skydown.android.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class AiMembershipRuntimeConfig(
    val androidProMonthlySku: String = "",
    val androidProYearlySku: String = "",
    val androidCreatorMonthlySku: String = "",
    val androidCreatorYearlySku: String = "",
    val annualDiscountCopy: String = "Mit Jahresplan sparst du und bleibst im Flow.",
    val planOrder: List<String> = listOf("free", "pro", "creator"),
    val defaultAnnualToggle: Boolean = false,
    val highlightedPlan: String = "creator",
    val warningThresholdPercent: Int = 70,
    val criticalThresholdPercent: Int = 90,
)

class AiMembershipRuntimeConfigRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observe(onChange: (Result<AiMembershipRuntimeConfig>) -> Unit): ListenerRegistration {
        return firestore.collection("appConfig").document("paymentMethods")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onChange(Result.failure(error))
                    return@addSnapshotListener
                }
                onChange(Result.success(snapshot?.data.orEmpty().toMembershipRuntimeConfig()))
            }
    }
}

private fun Map<String, Any>.toMembershipRuntimeConfig(): AiMembershipRuntimeConfig {
    val subscription = (this["aiSubscriptions"] as? Map<*, *>).orEmpty()
    fun string(key: String, fallback: String): String = (subscription[key] as? String)?.trim()?.ifEmpty { fallback } ?: fallback
    fun bool(key: String, fallback: Boolean): Boolean = subscription[key] as? Boolean ?: fallback
    fun int(key: String, fallback: Int): Int = (subscription[key] as? Number)?.toInt() ?: fallback
    val planOrder = (subscription["planOrder"] as? List<*>)?.mapNotNull { (it as? String)?.trim()?.lowercase() }?.filter { it.isNotEmpty() }
        ?: listOf("free", "pro", "creator")

    return AiMembershipRuntimeConfig(
        androidProMonthlySku = string("androidCreatorProductId", ""),
        androidProYearlySku = string("androidCreatorYearlyProductId", ""),
        androidCreatorMonthlySku = string("androidStudioProductId", ""),
        androidCreatorYearlySku = string("androidStudioYearlyProductId", ""),
        annualDiscountCopy = string("annualDiscountCopy", "Mit Jahresplan sparst du und bleibst im Flow."),
        planOrder = planOrder,
        defaultAnnualToggle = bool("defaultAnnualToggle", false),
        highlightedPlan = string("highlightedPlan", "creator").lowercase(),
        warningThresholdPercent = int("warningThresholdPercent", 70).coerceIn(1, 99),
        criticalThresholdPercent = int("criticalThresholdPercent", 90).coerceIn(1, 100),
    )
}
