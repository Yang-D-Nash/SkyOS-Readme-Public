package com.skydown.android.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WorkflowAutomationSettings(
    val keepsGoogleSeparate: Boolean = true,
    val isPrepared: Boolean = false,
    val googleAccountHint: String = "",
    val googleScopeHint: String = "Drive, Sheets, Calendar",
)

object WorkflowAutomationPreferences {
    private const val preferencesName = "workflow_automation_preferences"
    private const val keyKeepsGoogleSeparate = "keeps_google_separate"
    private const val keyIsPrepared = "is_prepared"
    private const val keyGoogleAccountHint = "google_account_hint"
    private const val keyGoogleScopeHint = "google_scope_hint"

    private lateinit var sharedPreferences: SharedPreferences
    private val _settings = MutableStateFlow(WorkflowAutomationSettings())
    val settings: StateFlow<WorkflowAutomationSettings> = _settings.asStateFlow()

    fun initialize(context: Context) {
        if (::sharedPreferences.isInitialized) return
        sharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        _settings.value = readSettings()
    }

    fun updateKeepsGoogleSeparate(value: Boolean) {
        updateSettings { it.copy(keepsGoogleSeparate = value) }
    }

    fun updatePrepared(value: Boolean) {
        updateSettings { it.copy(isPrepared = value) }
    }

    fun updateGoogleAccountHint(value: String) {
        updateSettings { it.copy(googleAccountHint = value) }
    }

    fun updateGoogleScopeHint(value: String) {
        updateSettings { it.copy(googleScopeHint = value) }
    }

    private fun updateSettings(transform: (WorkflowAutomationSettings) -> WorkflowAutomationSettings) {
        check(::sharedPreferences.isInitialized) {
            "WorkflowAutomationPreferences.initialize(context) must be called before updating settings."
        }

        val updated = transform(_settings.value)
        writeSettings(updated)
        _settings.value = updated
    }

    private fun readSettings(): WorkflowAutomationSettings {
        check(::sharedPreferences.isInitialized) {
            "WorkflowAutomationPreferences.initialize(context) must be called before reading settings."
        }

        return WorkflowAutomationSettings(
            keepsGoogleSeparate = sharedPreferences.getBoolean(keyKeepsGoogleSeparate, true),
            isPrepared = sharedPreferences.getBoolean(keyIsPrepared, false),
            googleAccountHint = sharedPreferences.getString(keyGoogleAccountHint, "").orEmpty(),
            googleScopeHint = sharedPreferences.getString(keyGoogleScopeHint, "Drive, Sheets, Calendar").orEmpty(),
        )
    }

    private fun writeSettings(settings: WorkflowAutomationSettings) {
        sharedPreferences.edit()
            .putBoolean(keyKeepsGoogleSeparate, settings.keepsGoogleSeparate)
            .putBoolean(keyIsPrepared, settings.isPrepared)
            .putString(keyGoogleAccountHint, settings.googleAccountHint)
            .putString(keyGoogleScopeHint, settings.googleScopeHint)
            .apply()
    }
}
