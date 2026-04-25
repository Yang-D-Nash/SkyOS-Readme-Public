package com.nash.skyos.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.functions.FirebaseFunctions

class MembershipAnalyticsTracker(
    context: Context,
) {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val functions = FirebaseFunctions.getInstance("us-central1")

    fun track(
        event: String,
        platform: String = "android",
        reason: String? = null,
        plan: String? = null,
        annual: Boolean? = null,
        surface: String? = null,
        currentPlan: String? = null,
    ) {
        val params = Bundle().apply {
            putString("platform", platform)
            reason?.let { putString("reason", it) }
            plan?.let { putString("plan", it) }
            annual?.let { putString("annual", it.toString()) }
            surface?.let { putString("surface", it) }
            currentPlan?.let { putString("currentPlan", it) }
        }
        analytics.logEvent(event, params)
        val payload = hashMapOf<String, Any>(
            "eventName" to event,
            "platform" to platform,
        )
        reason?.let { payload["reason"] = it }
        plan?.let { payload["plan"] = it }
        annual?.let { payload["annual"] = it }
        surface?.let { payload["surface"] = it }
        currentPlan?.let { payload["currentPlan"] = it }
        functions
            .getHttpsCallable("recordAiMembershipEvent")
            .call(payload)
    }
}
