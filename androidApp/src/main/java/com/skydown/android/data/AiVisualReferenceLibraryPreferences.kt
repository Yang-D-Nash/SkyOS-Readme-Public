package com.skydown.android.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiVisualReferenceLibrarySettings(
    val isEnabled: Boolean = false,
    val storageLink: String = "",
    val namingPrefix: String = "",
    val referenceHints: List<String> = List(5) { "" },
) {
    private val trimmedStorageLink: String
        get() = storageLink.trim()

    private val trimmedNamingPrefix: String
        get() = namingPrefix.trim()

    private val activeReferenceHints: List<String>
        get() = referenceHints.map(String::trim).filter(String::isNotBlank)

    fun promptContext(): String? {
        if (!isEnabled) return null
        if (trimmedStorageLink.isBlank() &&
            trimmedNamingPrefix.isBlank() &&
            activeReferenceHints.isEmpty()
        ) {
            return null
        }

        return buildString {
            appendLine("Referenzbibliothek fuer Visual-Generierung:")
            if (trimmedStorageLink.isNotBlank()) {
                appendLine("- Asset-Ziel / Library: $trimmedStorageLink")
            }
            if (trimmedNamingPrefix.isNotBlank()) {
                appendLine("- Benennungs-Praefix fuer neue Visuals: $trimmedNamingPrefix")
            }
            if (activeReferenceHints.isNotEmpty()) {
                appendLine("- Referenzhinweise fuer Stil, Charakter oder Elemente:")
                activeReferenceHints.forEachIndexed { index, hint ->
                    appendLine("  ${index + 1}. $hint")
                }
            }
            append("- Nutze die Referenzen als gestalterische Richtung, ohne vorhandene Bilder direkt zu kopieren.")
        }
    }
}

object AiVisualReferenceLibraryPreferences {
    private const val prefsName = "skydown_preferences"
    private const val enabledKey = "ai_visual_reference_enabled"
    private const val storageLinkKey = "ai_visual_reference_storage_link"
    private const val namingPrefixKey = "ai_visual_reference_naming_prefix"
    private const val referenceHintPrefix = "ai_visual_reference_hint_"
    private const val referenceHintCount = 5

    private lateinit var appContext: Context

    private val _settings = MutableStateFlow(AiVisualReferenceLibrarySettings())
    val settings: StateFlow<AiVisualReferenceLibrarySettings> = _settings.asStateFlow()

    fun initialize(context: Context) {
        if (::appContext.isInitialized) return

        appContext = context.applicationContext
        _settings.value = readSettings()
    }

    fun updateEnabled(isEnabled: Boolean) {
        updateSettings { it.copy(isEnabled = isEnabled) }
    }

    fun updateStorageLink(storageLink: String) {
        updateSettings { it.copy(storageLink = storageLink) }
    }

    fun updateNamingPrefix(namingPrefix: String) {
        updateSettings { it.copy(namingPrefix = namingPrefix) }
    }

    fun updateReferenceHint(index: Int, value: String) {
        updateSettings { current ->
            if (!current.referenceHints.indices.contains(index)) return@updateSettings current
            current.copy(
                referenceHints = current.referenceHints.toMutableList().apply {
                    this[index] = value
                },
            )
        }
    }

    fun promptContext(): String? = settings.value.promptContext()

    private fun updateSettings(transform: (AiVisualReferenceLibrarySettings) -> AiVisualReferenceLibrarySettings) {
        check(::appContext.isInitialized) {
            "AiVisualReferenceLibraryPreferences.initialize(context) must be called before updating settings."
        }

        val nextSettings = transform(_settings.value)
        writeSettings(nextSettings)
        _settings.value = nextSettings
    }

    private fun readSettings(): AiVisualReferenceLibrarySettings {
        val preferences = preferences()
        return AiVisualReferenceLibrarySettings(
            isEnabled = preferences.getBoolean(enabledKey, false),
            storageLink = preferences.getString(storageLinkKey, "").orEmpty(),
            namingPrefix = preferences.getString(namingPrefixKey, "").orEmpty(),
            referenceHints = List(referenceHintCount) { index ->
                preferences.getString("$referenceHintPrefix$index", "").orEmpty()
            },
        )
    }

    private fun writeSettings(settings: AiVisualReferenceLibrarySettings) {
        preferences().edit().apply {
            putBoolean(enabledKey, settings.isEnabled)
            putString(storageLinkKey, settings.storageLink)
            putString(namingPrefixKey, settings.namingPrefix)
            repeat(referenceHintCount) { index ->
                putString(
                    "$referenceHintPrefix$index",
                    settings.referenceHints.getOrNull(index).orEmpty(),
                )
            }
        }.apply()
    }

    private fun preferences() = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
}
