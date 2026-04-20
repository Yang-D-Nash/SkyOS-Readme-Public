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
        fun from(rawValue: String): AiAccessMode = entries.firstOrNull { it.rawValue == rawValue } ?: SignedIn
    }
}

object AppFeatureFlagsStore {
    private const val aiEnabledKey = "ai_enabled"
    private const val aiAccessModeKey = "ai_access_mode"

    private val remoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val _isAiEnabled = MutableStateFlow(true)
    private val _aiAccessMode = MutableStateFlow(AiAccessMode.SignedIn)
    @Volatile
    private var uiTestAiEnabledOverride: Boolean? = null
    @Volatile
    private var uiTestAiAccessModeOverride: AiAccessMode? = null
    val isAiEnabled: StateFlow<Boolean> = _isAiEnabled.asStateFlow()
    val aiAccessMode: StateFlow<AiAccessMode> = _aiAccessMode.asStateFlow()

    fun configureUiTestOverrides(
        aiEnabled: Boolean?,
        aiAccessMode: AiAccessMode?,
    ) {
        uiTestAiEnabledOverride = aiEnabled
        uiTestAiAccessModeOverride = aiAccessMode
    }

    fun initialize() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()

        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                aiEnabledKey to true,
                aiAccessModeKey to AiAccessMode.SignedIn.rawValue,
            ),
        )
        _isAiEnabled.value = uiTestAiEnabledOverride ?: true
        _aiAccessMode.value = uiTestAiAccessModeOverride ?: AiAccessMode.SignedIn
    }

    suspend fun refresh() {
        if (uiTestAiEnabledOverride != null || uiTestAiAccessModeOverride != null) {
            _isAiEnabled.value = uiTestAiEnabledOverride ?: true
            _aiAccessMode.value = uiTestAiAccessModeOverride ?: AiAccessMode.SignedIn
            return
        }

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
                "Bitte melde dich an. Die KI ist aktuell nur fuer freigegebene Konten sichtbar."
            user?.aiAccessEnabled == false ->
                "Der KI-Zugriff ist fuer dein Konto derzeit deaktiviert."
            user == null ->
                "Bitte melde dich an, um die KI zu nutzen."
            accessMode == AiAccessMode.AdminOnly && !user.hasStaffAccess ->
                "Die KI ist gerade nur fuer Staff-Konten freigeschaltet."
            else ->
                "Die KI ist gerade nicht verfuegbar."
        }
    }
}
