package com.skydown.android.data

import android.content.Context
import com.skydown.android.ui.theme.AppearanceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppearancePreferences {
    private const val prefsName = "skydown_preferences"
    private const val appearanceKey = "appearance_mode"

    private lateinit var appContext: Context

    private val _appearanceMode = MutableStateFlow(AppearanceMode.System)
    val appearanceMode: StateFlow<AppearanceMode> = _appearanceMode.asStateFlow()

    fun initialize(context: Context) {
        if (::appContext.isInitialized) return

        appContext = context.applicationContext
        val savedValue = preferences().getString(appearanceKey, AppearanceMode.System.storageValue)
        _appearanceMode.value = AppearanceMode.fromStorageValue(savedValue)
    }

    fun updateAppearanceMode(mode: AppearanceMode) {
        check(::appContext.isInitialized) {
            "AppearancePreferences.initialize(context) must be called before updating the theme."
        }

        preferences().edit().putString(appearanceKey, mode.storageValue).apply()
        _appearanceMode.value = mode
    }

    private fun preferences() = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
}
