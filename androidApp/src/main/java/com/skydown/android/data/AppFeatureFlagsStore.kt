package com.skydown.android.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.skydown.shared.model.User
import com.skydown.shared.model.hasStaffAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

enum class AiAccessMode(val rawValue: String) {
    Off("off"),
    AdminOnly("admin_only"),
    SignedIn("signed_in");

    companion object {
        fun from(rawValue: String): AiAccessMode = entries.firstOrNull { it.rawValue == rawValue } ?: AdminOnly
    }
}

object AppFeatureFlagsStore {
    private const val aiEnabledKey = "ai_enabled"
    private const val aiAccessModeKey = "ai_access_mode"

    private val remoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val _isAiEnabled = MutableStateFlow(true)
    private val _aiAccessMode = MutableStateFlow(AiAccessMode.AdminOnly)
    val isAiEnabled: StateFlow<Boolean> = _isAiEnabled.asStateFlow()
    val aiAccessMode: StateFlow<AiAccessMode> = _aiAccessMode.asStateFlow()

    fun initialize() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()

        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                aiEnabledKey to true,
                aiAccessModeKey to AiAccessMode.AdminOnly.rawValue,
            ),
        )
        _isAiEnabled.value = true
        _aiAccessMode.value = AiAccessMode.AdminOnly
    }

    suspend fun refresh() {
        runCatching { remoteConfig.fetchAndActivate().await() }
        _isAiEnabled.value = remoteConfig.getBoolean(aiEnabledKey)
        _aiAccessMode.value = AiAccessMode.from(remoteConfig.getString(aiAccessModeKey))
    }

    fun allowsAiAccess(
        user: User?,
        accessMode: AiAccessMode = aiAccessMode.value,
        isEnabled: Boolean = isAiEnabled.value,
    ): Boolean {
        if (!isEnabled) return false
        val resolvedUser = user ?: return false
        if (!resolvedUser.aiAccessEnabled) return false

        return when (accessMode) {
            AiAccessMode.Off -> false
            AiAccessMode.AdminOnly -> resolvedUser.hasStaffAccess
            AiAccessMode.SignedIn -> true
        }
    }

    fun accessDeniedMessage(
        user: User?,
        accessMode: AiAccessMode = aiAccessMode.value,
        isEnabled: Boolean = isAiEnabled.value,
    ): String {
        if (!isEnabled || accessMode == AiAccessMode.Off) {
            return "Die KI ist gerade deaktiviert."
        }

        return when {
            user == null && accessMode == AiAccessMode.AdminOnly ->
                "Melde dich an, dann sagen wir dir Bescheid, sobald die KI fuer dein Konto verfuegbar ist."
            user?.aiAccessEnabled == false ->
                "Die KI ist fuer dein Konto gerade pausiert."
            user == null ->
                "Bitte melde dich an, um die KI zu nutzen."
            accessMode == AiAccessMode.AdminOnly && !user.hasStaffAccess ->
                "Die KI ist gerade nur fuer Staff-Konten freigeschaltet."
            else ->
                "Die KI ist gerade nicht verfuegbar."
        }
    }
}
