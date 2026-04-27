package com.nash.skyos.data

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.FirebaseApp

class PushTokenSyncClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    @Volatile
    private var lastSyncedFingerprint: String? = null

    suspend fun syncIfPossible(uid: String?) {
        val normalizedUid = uid?.trim().orEmpty()
        if (normalizedUid.isEmpty()) return
        val token = PushTokenRegistry.currentTokenOrNull() ?: return
        val appVersion = resolveAppVersion()
        val fingerprint = "$normalizedUid|$token|$appVersion"
        if (lastSyncedFingerprint == fingerprint) return

        runCatching {
            functions.callWithAppCheckRetry(
                functionName = "upsertPushToken",
                payload = mapOf(
                    "uid" to normalizedUid,
                    "token" to token,
                    "platform" to "android",
                    "appVersion" to appVersion,
                ),
            )
        }.onSuccess {
            lastSyncedFingerprint = fingerprint
        }
    }

    private fun resolveAppVersion(): String {
        return runCatching {
            val context = FirebaseApp.getInstance().applicationContext
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName.orEmpty().trim()
        }.getOrDefault("")
    }
}
