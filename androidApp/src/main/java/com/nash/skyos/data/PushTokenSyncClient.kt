package com.nash.skyos.data

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class PushTokenSyncClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    @Volatile
    private var lastSyncedFingerprint: String? = null

    suspend fun syncIfPossible(uid: String?) {
        val normalizedUid = uid?.trim().orEmpty()
        if (normalizedUid.isEmpty()) return
        val token = PushTokenRegistry.currentTokenOrNull() ?: fetchAndCacheFirebaseMessagingToken() ?: return
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

    private suspend fun fetchAndCacheFirebaseMessagingToken(): String? {
        val token = runCatching {
            FirebaseMessaging.getInstance().token.await()
        }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        PushTokenRegistry.cacheToken(token)
        return token
    }
}
