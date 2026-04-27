package com.nash.skyos.data

import android.content.Context

object PushTokenRegistry {
    private const val prefsName = "skydown.push.tokens"
    private const val tokenKey = "push.token.raw"

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun cacheToken(token: String) {
        val normalized = token.trim()
        if (normalized.isEmpty()) return
        val context = appContext ?: return
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(tokenKey, normalized)
            .apply()
    }

    fun currentTokenOrNull(): String? {
        val context = appContext ?: return null
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(tokenKey, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
