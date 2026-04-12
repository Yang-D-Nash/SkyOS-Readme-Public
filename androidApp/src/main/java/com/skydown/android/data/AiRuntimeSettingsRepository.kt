package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

enum class AiRuntimeAgentProvider(val rawValue: String) {
    Gemini("gemini"),
    Manus("manus"),
    ;

    val displayTitle: String
        get() = when (this) {
            Gemini -> "Gemini"
            Manus -> "Manus"
        }

    companion object {
        fun resolve(rawValue: String?): AiRuntimeAgentProvider {
            val normalized = rawValue?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.rawValue == normalized } ?: Gemini
        }
    }
}

data class AiRuntimeKindLimits(
    val text: Int,
    val visual: Int,
    val agent: Int,
) {
    companion object {
        val hardDefaults = AiRuntimeKindLimits(
            text = 120,
            visual = 20,
            agent = 40,
        )
        val globalDefaults = AiRuntimeKindLimits(
            text = 1500,
            visual = 180,
            agent = 350,
        )
    }
}

data class AiRuntimeManusSettings(
    val isEnabled: Boolean = false,
    val requestTimeoutMs: Int = 12000,
    val pollIntervalMs: Int = 1500,
    val maxPollAttempts: Int = 18,
    val listMessagesLimit: Int = 30,
    val maxPromptChars: Int = 2400,
    val maxHistoryTurns: Int = 12,
    val autoStopOnWaiting: Boolean = true,
    val blockHighCreditEvents: Boolean = true,
    val includeVerboseEvents: Boolean = false,
)

data class AiRuntimeSettings(
    val costGuardEnabled: Boolean = true,
    val agentProvider: AiRuntimeAgentProvider = AiRuntimeAgentProvider.Gemini,
    val fallbackAgentProvider: AiRuntimeAgentProvider = AiRuntimeAgentProvider.Gemini,
    val hardDailyCaps: AiRuntimeKindLimits = AiRuntimeKindLimits.hardDefaults,
    val globalDailyCaps: AiRuntimeKindLimits = AiRuntimeKindLimits.globalDefaults,
    val manus: AiRuntimeManusSettings = AiRuntimeManusSettings(),
)

class AiRuntimeSettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "adminConfig"
    private val documentName = "aiRuntime"

    fun observeSettings(onChange: (Result<AiRuntimeSettings>) -> Unit): ListenerRegistration {
        return firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onChange(Result.failure(error))
                return@addSnapshotListener
            }

            onChange(Result.success(snapshot?.data.orEmpty().toAiRuntimeSettings()))
        }
    }

    suspend fun updateSettings(settings: AiRuntimeSettings): Result<Unit> {
        return runCatching {
            firestore.collection(collectionName).document(documentName).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
        }
    }
}

private fun Map<String, Any>.toAiRuntimeSettings(): AiRuntimeSettings {
    val manusMap = this["manus"] as? Map<String, Any> ?: emptyMap()
    return AiRuntimeSettings(
        costGuardEnabled = this["costGuardEnabled"] as? Boolean ?: true,
        agentProvider = AiRuntimeAgentProvider.resolve(this["agentProvider"] as? String),
        fallbackAgentProvider = AiRuntimeAgentProvider.resolve(this["fallbackAgentProvider"] as? String),
        hardDailyCaps = decodeRuntimeLimits(
            rawValue = this["hardDailyCaps"],
            fallback = AiRuntimeKindLimits.hardDefaults,
        ),
        globalDailyCaps = decodeRuntimeLimits(
            rawValue = this["globalDailyCaps"],
            fallback = AiRuntimeKindLimits.globalDefaults,
        ),
        manus = AiRuntimeManusSettings(
            isEnabled = manusMap["isEnabled"] as? Boolean ?: false,
            requestTimeoutMs = parseRuntimeInt(
                manusMap["requestTimeoutMs"],
                fallback = AiRuntimeManusSettings().requestTimeoutMs,
                min = 3000,
                max = 30000,
            ),
            pollIntervalMs = parseRuntimeInt(
                manusMap["pollIntervalMs"],
                fallback = AiRuntimeManusSettings().pollIntervalMs,
                min = 500,
                max = 5000,
            ),
            maxPollAttempts = parseRuntimeInt(
                manusMap["maxPollAttempts"],
                fallback = AiRuntimeManusSettings().maxPollAttempts,
                min = 2,
                max = 60,
            ),
            listMessagesLimit = parseRuntimeInt(
                manusMap["listMessagesLimit"],
                fallback = AiRuntimeManusSettings().listMessagesLimit,
                min = 5,
                max = 100,
            ),
            maxPromptChars = parseRuntimeInt(
                manusMap["maxPromptChars"],
                fallback = AiRuntimeManusSettings().maxPromptChars,
                min = 300,
                max = 12000,
            ),
            maxHistoryTurns = parseRuntimeInt(
                manusMap["maxHistoryTurns"],
                fallback = AiRuntimeManusSettings().maxHistoryTurns,
                min = 0,
                max = 24,
            ),
            autoStopOnWaiting = manusMap["autoStopOnWaiting"] as? Boolean ?: true,
            blockHighCreditEvents = manusMap["blockHighCreditEvents"] as? Boolean ?: true,
            includeVerboseEvents = manusMap["includeVerboseEvents"] as? Boolean ?: false,
        ),
    )
}

private fun AiRuntimeSettings.toMap(): Map<String, Any> {
    return mapOf(
        "costGuardEnabled" to costGuardEnabled,
        "agentProvider" to agentProvider.rawValue,
        "fallbackAgentProvider" to fallbackAgentProvider.rawValue,
        "hardDailyCaps" to mapOf(
            "text" to hardDailyCaps.text.coerceAtLeast(1),
            "visual" to hardDailyCaps.visual.coerceAtLeast(1),
            "agent" to hardDailyCaps.agent.coerceAtLeast(1),
        ),
        "globalDailyCaps" to mapOf(
            "text" to globalDailyCaps.text.coerceAtLeast(1),
            "visual" to globalDailyCaps.visual.coerceAtLeast(1),
            "agent" to globalDailyCaps.agent.coerceAtLeast(1),
        ),
        "manus" to mapOf(
            "isEnabled" to manus.isEnabled,
            "requestTimeoutMs" to manus.requestTimeoutMs.coerceAtLeast(3000),
            "pollIntervalMs" to manus.pollIntervalMs.coerceAtLeast(500),
            "maxPollAttempts" to manus.maxPollAttempts.coerceAtLeast(2),
            "listMessagesLimit" to manus.listMessagesLimit.coerceAtLeast(5),
            "maxPromptChars" to manus.maxPromptChars.coerceAtLeast(300),
            "maxHistoryTurns" to manus.maxHistoryTurns.coerceAtLeast(0),
            "autoStopOnWaiting" to manus.autoStopOnWaiting,
            "blockHighCreditEvents" to manus.blockHighCreditEvents,
            "includeVerboseEvents" to manus.includeVerboseEvents,
        ),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

private fun decodeRuntimeLimits(
    rawValue: Any?,
    fallback: AiRuntimeKindLimits,
): AiRuntimeKindLimits {
    val map = rawValue as? Map<String, Any> ?: emptyMap()
    return AiRuntimeKindLimits(
        text = parseRuntimeInt(map["text"], fallback = fallback.text, min = 1, max = 100000),
        visual = parseRuntimeInt(map["visual"], fallback = fallback.visual, min = 1, max = 100000),
        agent = parseRuntimeInt(map["agent"], fallback = fallback.agent, min = 1, max = 100000),
    )
}

private fun parseRuntimeInt(
    value: Any?,
    fallback: Int,
    min: Int,
    max: Int,
): Int {
    val parsed = when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    } ?: return fallback

    return parsed.coerceIn(min, max)
}
