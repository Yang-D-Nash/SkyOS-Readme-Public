package com.skydown.android.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object AppFeatureFlagsStore {
    private const val aiEnabledKey = "ai_enabled"

    private val remoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val _isAiEnabled = MutableStateFlow(true)
    val isAiEnabled: StateFlow<Boolean> = _isAiEnabled.asStateFlow()

    fun initialize() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()

        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(mapOf(aiEnabledKey to true))
        _isAiEnabled.value = true
    }

    suspend fun refresh() {
        runCatching { remoteConfig.fetchAndActivate().await() }
        _isAiEnabled.value = remoteConfig.getBoolean(aiEnabledKey)
    }
}
