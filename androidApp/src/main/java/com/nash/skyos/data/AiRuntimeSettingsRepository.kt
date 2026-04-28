package com.nash.skyos.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

enum class AiRuntimeAgentProvider(val rawValue: String) {
    Gemini("gemini"),
    Grok("grok"),
    Manus("manus"),
    ;

    val displayTitle: String
        get() = when (this) {
            Gemini -> "Gemini"
            Grok -> "Grok"
            Manus -> "Manus"
        }

    companion object {
        fun resolve(rawValue: String?): AiRuntimeAgentProvider {
            val normalized = rawValue?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.rawValue == normalized } ?: Grok
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

data class AiRuntimeKnowledgeGoogleDriveSettings(
    val isEnabled: Boolean = false,
    val strictSourceMode: Boolean = true,
    val requireSourceCitations: Boolean = true,
    val allowedSharedDriveIds: List<String> = emptyList(),
    val allowedFolderIds: List<String> = emptyList(),
)

data class AiRuntimeKnowledgeSettings(
    val googleDrive: AiRuntimeKnowledgeGoogleDriveSettings = AiRuntimeKnowledgeGoogleDriveSettings(),
)

data class AiRuntimeBotModelPolicy(
    val textPrimaryModel: String = "gemini-2.5-flash-lite",
    val textFallbackModel: String = "gemini-2.5-flash-lite",
    val visualPrimaryModel: String = "gemini-2.5-flash-image",
    val visualFallbackModel: String = "imagen-3.0-generate-002",
)

data class AiRuntimeBotCostGuard(
    val enabled: Boolean = true,
    val preferBriefAnswersWhenCritical: Boolean = true,
    val shortAnswerMaxOutputTokens: Int = 240,
    val standardAnswerMaxOutputTokens: Int = 768,
)

data class AiRuntimeBotRoutingPolicy(
    val preferFaqWhenTopicMatched: Boolean = true,
    val preferProductGuideForNewUsers: Boolean = true,
    val allowVisualGeneration: Boolean = true,
)

data class AiRuntimeBotFallbackPolicy(
    val allowTextFallback: Boolean = true,
    val allowVisualFallback: Boolean = true,
    val exposeFallbackReason: Boolean = true,
)

data class AiRuntimeBotSafetyPolicy(
    val safeModeEnabled: Boolean = true,
    val strictUnknownHandling: Boolean = true,
    val blockSpeculativeFaqAnswers: Boolean = true,
)

data class AiRuntimeBotActionLayer(
    val proactiveHintsEnabled: Boolean = true,
    val triggerAiLimitNearEnabled: Boolean = true,
    val triggerRestoreAvailableEnabled: Boolean = true,
    val triggerOrderShippedEnabled: Boolean = true,
    val triggerPaymentMethodsChangedEnabled: Boolean = true,
    val triggerUsageBasedUpgradeEnabled: Boolean = true,
    val warningThresholdPercent: Int = 70,
    val criticalThresholdPercent: Int = 90,
    val upgradeHintFreeToProText: String = "Deine Nutzung ist hoch. Ein Upgrade auf Pro reduziert Abbrueche durch Limits.",
    val upgradeHintProToCreatorText: String = "Deine Nutzung ist hoch. Creator kann dir mehr Workflow-Tiefe und Reserve geben.",
    val faqPriorityMode: String = "live_owner_generic",
    val promptVersionAlias: String = "bot-max-v1",
)

data class AiRuntimeBotAgentCore(
    val allowedTasks: List<String> = listOf("support_recovery", "commerce_order", "owner_ops"),
    val blockedTasks: List<String> = emptyList(),
    val allowedTools: List<String> = listOf("knowledge_lookup", "order_lookup", "membership_lookup", "owner_runtime"),
    val allowWorkflowAutomation: Boolean = true,
    val requireConfirmationForCommerce: Boolean = true,
    val requireConfirmationForOwnerOps: Boolean = true,
    val blockWhenKillSwitchEnabled: Boolean = true,
    val blockUnknownTasks: Boolean = true,
    val activepiecesEnabled: Boolean = true,
    val n8nEnabled: Boolean = true,
    val manusEnabled: Boolean = true,
    val allowedExternalTaskTypes: List<String> = listOf("support_recovery", "commerce_order", "owner_ops"),
    val providerPriority: List<String> = listOf("activepieces", "n8n"),
    val maxExternalCallsPerRequest: Int = 1,
    val externalTimeoutMs: Int = 12000,
    val externalRetryAttempts: Int = 2,
    val diagnosticsMode: String = "owner_only",
    val ownerMode: String = "standard",
    val killSwitch: Boolean = false,
)

data class AiRuntimeBotSettings(
    val promptVersion: String = "bot-max-v1",
    val qualityMode: String = "balanced",
    val faqMode: String = "auto",
    val ownerMode: String = "standard",
    val answerLength: String = "adaptive",
    val personalityStyle: String = "calm_precise",
    val loggingLevel: String = "standard",
    val diagnosticsMode: String = "owner_only",
    val killSwitchEnabled: Boolean = false,
    val modelPolicy: AiRuntimeBotModelPolicy = AiRuntimeBotModelPolicy(),
    val costGuard: AiRuntimeBotCostGuard = AiRuntimeBotCostGuard(),
    val routingPolicy: AiRuntimeBotRoutingPolicy = AiRuntimeBotRoutingPolicy(),
    val fallbackPolicy: AiRuntimeBotFallbackPolicy = AiRuntimeBotFallbackPolicy(),
    val safetyPolicy: AiRuntimeBotSafetyPolicy = AiRuntimeBotSafetyPolicy(),
    val actionLayer: AiRuntimeBotActionLayer = AiRuntimeBotActionLayer(),
    val agentCore: AiRuntimeBotAgentCore = AiRuntimeBotAgentCore(),
)

data class AiRuntimeSettings(
    val costGuardEnabled: Boolean = true,
    val agentProvider: AiRuntimeAgentProvider = AiRuntimeAgentProvider.Grok,
    val fallbackAgentProvider: AiRuntimeAgentProvider = AiRuntimeAgentProvider.Gemini,
    val hardDailyCaps: AiRuntimeKindLimits = AiRuntimeKindLimits.hardDefaults,
    val globalDailyCaps: AiRuntimeKindLimits = AiRuntimeKindLimits.globalDefaults,
    val manus: AiRuntimeManusSettings = AiRuntimeManusSettings(),
    val knowledge: AiRuntimeKnowledgeSettings = AiRuntimeKnowledgeSettings(),
    val bot: AiRuntimeBotSettings = AiRuntimeBotSettings(),
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
    val manusMap = this["manus"].asStringKeyedMap()
    val knowledgeMap = this["knowledge"].asStringKeyedMap()
    val googleDriveMap = knowledgeMap["googleDrive"].asStringKeyedMap()
    val botMap = this["bot"].asStringKeyedMap()
    val botModelPolicyMap = botMap["modelPolicy"].asStringKeyedMap()
    val botCostGuardMap = botMap["costGuard"].asStringKeyedMap()
    val botRoutingMap = botMap["routingPolicy"].asStringKeyedMap()
    val botFallbackMap = botMap["fallbackPolicy"].asStringKeyedMap()
    val botSafetyMap = botMap["safetyPolicy"].asStringKeyedMap()
    val botActionLayerMap = botMap["actionLayer"].asStringKeyedMap()
    val botAgentCoreMap = botMap["agentCore"].asStringKeyedMap()
    val botAgentToolPolicyMap = botAgentCoreMap["toolPolicy"].asStringKeyedMap()
    val botAgentConfirmationPolicyMap = botAgentCoreMap["confirmationPolicy"].asStringKeyedMap()
    val botAgentSafetyPolicyMap = botAgentCoreMap["safetyPolicy"].asStringKeyedMap()
    val botAgentExternalPolicyMap = botAgentCoreMap["externalPolicy"].asStringKeyedMap()
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
        knowledge = AiRuntimeKnowledgeSettings(
            googleDrive = AiRuntimeKnowledgeGoogleDriveSettings(
                isEnabled = googleDriveMap["isEnabled"] as? Boolean ?: false,
                strictSourceMode = googleDriveMap["strictSourceMode"] as? Boolean ?: true,
                requireSourceCitations = googleDriveMap["requireSourceCitations"] as? Boolean ?: true,
                allowedSharedDriveIds = normalizeRuntimeStringList(
                    googleDriveMap["allowedSharedDriveIds"],
                    fallback = emptyList(),
                ),
                allowedFolderIds = normalizeRuntimeStringList(
                    googleDriveMap["allowedFolderIds"],
                    fallback = emptyList(),
                ),
            ),
        ),
        bot = AiRuntimeBotSettings(
            promptVersion = normalizeRuntimeString(
                botMap["promptVersion"] as? String,
                fallback = AiRuntimeBotSettings().promptVersion,
            ),
            qualityMode = normalizeAllowedRuntimeString(
                botMap["qualityMode"] as? String,
                allowed = setOf("balanced", "high"),
                fallback = AiRuntimeBotSettings().qualityMode,
            ),
            faqMode = normalizeAllowedRuntimeString(
                botMap["faqMode"] as? String,
                allowed = setOf("off", "auto", "prefer_faq"),
                fallback = AiRuntimeBotSettings().faqMode,
            ),
            ownerMode = normalizeAllowedRuntimeString(
                botMap["ownerMode"] as? String,
                allowed = setOf("standard", "diagnostic"),
                fallback = AiRuntimeBotSettings().ownerMode,
            ),
            answerLength = normalizeAllowedRuntimeString(
                botMap["answerLength"] as? String,
                allowed = setOf("adaptive", "short", "detailed"),
                fallback = AiRuntimeBotSettings().answerLength,
            ),
            personalityStyle = normalizeRuntimeString(
                botMap["personalityStyle"] as? String,
                fallback = AiRuntimeBotSettings().personalityStyle,
                maxLength = 160,
            ),
            loggingLevel = normalizeRuntimeString(
                botMap["loggingLevel"] as? String,
                fallback = AiRuntimeBotSettings().loggingLevel,
                maxLength = 80,
            ),
            diagnosticsMode = normalizeAllowedRuntimeString(
                botMap["diagnosticsMode"] as? String,
                allowed = setOf("off", "owner_only", "verbose"),
                fallback = AiRuntimeBotSettings().diagnosticsMode,
            ),
            killSwitchEnabled = botMap["killSwitchEnabled"] as? Boolean ?: false,
            modelPolicy = AiRuntimeBotModelPolicy(
                textPrimaryModel = normalizeRuntimeString(
                    botModelPolicyMap["textPrimaryModel"] as? String,
                    fallback = AiRuntimeBotModelPolicy().textPrimaryModel,
                ),
                textFallbackModel = normalizeRuntimeString(
                    botModelPolicyMap["textFallbackModel"] as? String,
                    fallback = AiRuntimeBotModelPolicy().textFallbackModel,
                ),
                visualPrimaryModel = normalizeRuntimeString(
                    botModelPolicyMap["visualPrimaryModel"] as? String,
                    fallback = AiRuntimeBotModelPolicy().visualPrimaryModel,
                ),
                visualFallbackModel = normalizeRuntimeString(
                    botModelPolicyMap["visualFallbackModel"] as? String,
                    fallback = AiRuntimeBotModelPolicy().visualFallbackModel,
                ),
            ),
            costGuard = AiRuntimeBotCostGuard(
                enabled = botCostGuardMap["enabled"] as? Boolean ?: true,
                preferBriefAnswersWhenCritical = botCostGuardMap["preferBriefAnswersWhenCritical"] as? Boolean ?: true,
                shortAnswerMaxOutputTokens = parseRuntimeInt(
                    botCostGuardMap["shortAnswerMaxOutputTokens"],
                    fallback = AiRuntimeBotCostGuard().shortAnswerMaxOutputTokens,
                    min = 80,
                    max = 1200,
                ),
                standardAnswerMaxOutputTokens = parseRuntimeInt(
                    botCostGuardMap["standardAnswerMaxOutputTokens"],
                    fallback = AiRuntimeBotCostGuard().standardAnswerMaxOutputTokens,
                    min = 120,
                    max = 2400,
                ),
            ),
            routingPolicy = AiRuntimeBotRoutingPolicy(
                preferFaqWhenTopicMatched = botRoutingMap["preferFaqWhenTopicMatched"] as? Boolean ?: true,
                preferProductGuideForNewUsers = botRoutingMap["preferProductGuideForNewUsers"] as? Boolean ?: true,
                allowVisualGeneration = botRoutingMap["allowVisualGeneration"] as? Boolean ?: true,
            ),
            fallbackPolicy = AiRuntimeBotFallbackPolicy(
                allowTextFallback = botFallbackMap["allowTextFallback"] as? Boolean ?: true,
                allowVisualFallback = botFallbackMap["allowVisualFallback"] as? Boolean ?: true,
                exposeFallbackReason = botFallbackMap["exposeFallbackReason"] as? Boolean ?: true,
            ),
            safetyPolicy = AiRuntimeBotSafetyPolicy(
                safeModeEnabled = botSafetyMap["safeModeEnabled"] as? Boolean ?: true,
                strictUnknownHandling = botSafetyMap["strictUnknownHandling"] as? Boolean ?: true,
                blockSpeculativeFaqAnswers = botSafetyMap["blockSpeculativeFaqAnswers"] as? Boolean ?: true,
            ),
            actionLayer = AiRuntimeBotActionLayer(
                proactiveHintsEnabled = botActionLayerMap["proactiveHintsEnabled"] as? Boolean ?: true,
                triggerAiLimitNearEnabled = botActionLayerMap["triggerAiLimitNearEnabled"] as? Boolean ?: true,
                triggerRestoreAvailableEnabled = botActionLayerMap["triggerRestoreAvailableEnabled"] as? Boolean ?: true,
                triggerOrderShippedEnabled = botActionLayerMap["triggerOrderShippedEnabled"] as? Boolean ?: true,
                triggerPaymentMethodsChangedEnabled = botActionLayerMap["triggerPaymentMethodsChangedEnabled"] as? Boolean ?: true,
                triggerUsageBasedUpgradeEnabled = botActionLayerMap["triggerUsageBasedUpgradeEnabled"] as? Boolean ?: true,
                warningThresholdPercent = parseRuntimeInt(
                    botActionLayerMap["warningThresholdPercent"],
                    fallback = AiRuntimeBotActionLayer().warningThresholdPercent,
                    min = 50,
                    max = 99,
                ),
                criticalThresholdPercent = parseRuntimeInt(
                    botActionLayerMap["criticalThresholdPercent"],
                    fallback = AiRuntimeBotActionLayer().criticalThresholdPercent,
                    min = 60,
                    max = 100,
                ),
                upgradeHintFreeToProText = normalizeRuntimeString(
                    botActionLayerMap["upgradeHintFreeToProText"] as? String,
                    fallback = AiRuntimeBotActionLayer().upgradeHintFreeToProText,
                    maxLength = 220,
                ),
                upgradeHintProToCreatorText = normalizeRuntimeString(
                    botActionLayerMap["upgradeHintProToCreatorText"] as? String,
                    fallback = AiRuntimeBotActionLayer().upgradeHintProToCreatorText,
                    maxLength = 220,
                ),
                faqPriorityMode = normalizeAllowedRuntimeString(
                    botActionLayerMap["faqPriorityMode"] as? String,
                    allowed = setOf("live_owner_generic", "owner_live_generic", "balanced"),
                    fallback = AiRuntimeBotActionLayer().faqPriorityMode,
                ),
                promptVersionAlias = normalizeRuntimeString(
                    botActionLayerMap["promptVersionAlias"] as? String,
                    fallback = AiRuntimeBotActionLayer().promptVersionAlias,
                ),
            ),
            agentCore = AiRuntimeBotAgentCore(
                allowedTasks = normalizeRuntimeStringList(
                    botAgentCoreMap["allowedTasks"],
                    fallback = AiRuntimeBotAgentCore().allowedTasks,
                ),
                blockedTasks = normalizeRuntimeStringList(
                    botAgentCoreMap["blockedTasks"],
                    fallback = emptyList(),
                ),
                allowedTools = normalizeRuntimeStringList(
                    botAgentToolPolicyMap["allowedTools"],
                    fallback = AiRuntimeBotAgentCore().allowedTools,
                ),
                allowWorkflowAutomation = botAgentToolPolicyMap["allowWorkflowAutomation"] as? Boolean ?: true,
                requireConfirmationForCommerce = botAgentConfirmationPolicyMap["requireConfirmationForCommerce"] as? Boolean ?: true,
                requireConfirmationForOwnerOps = botAgentConfirmationPolicyMap["requireConfirmationForOwnerOps"] as? Boolean ?: true,
                blockWhenKillSwitchEnabled = botAgentSafetyPolicyMap["blockWhenKillSwitchEnabled"] as? Boolean ?: true,
                blockUnknownTasks = botAgentSafetyPolicyMap["blockUnknownTasks"] as? Boolean ?: true,
                activepiecesEnabled = botAgentExternalPolicyMap["activepiecesEnabled"] as? Boolean ?: true,
                n8nEnabled = botAgentExternalPolicyMap["n8nEnabled"] as? Boolean ?: true,
                manusEnabled = botAgentExternalPolicyMap["manusEnabled"] as? Boolean ?: true,
                allowedExternalTaskTypes = normalizeRuntimeStringList(
                    botAgentExternalPolicyMap["allowedExternalTaskTypes"],
                    fallback = AiRuntimeBotAgentCore().allowedExternalTaskTypes,
                ),
                providerPriority = normalizeRuntimeStringList(
                    botAgentExternalPolicyMap["providerPriority"],
                    fallback = AiRuntimeBotAgentCore().providerPriority,
                ),
                maxExternalCallsPerRequest = parseRuntimeInt(
                    botAgentExternalPolicyMap["maxExternalCallsPerRequest"],
                    fallback = AiRuntimeBotAgentCore().maxExternalCallsPerRequest,
                    min = 0,
                    max = 3,
                ),
                externalTimeoutMs = parseRuntimeInt(
                    botAgentExternalPolicyMap["externalTimeoutMs"],
                    fallback = AiRuntimeBotAgentCore().externalTimeoutMs,
                    min = 2000,
                    max = 30000,
                ),
                externalRetryAttempts = parseRuntimeInt(
                    botAgentExternalPolicyMap["externalRetryAttempts"],
                    fallback = AiRuntimeBotAgentCore().externalRetryAttempts,
                    min = 0,
                    max = 4,
                ),
                diagnosticsMode = normalizeAllowedRuntimeString(
                    botAgentCoreMap["diagnosticsMode"] as? String,
                    allowed = setOf("off", "owner_only", "verbose"),
                    fallback = AiRuntimeBotAgentCore().diagnosticsMode,
                ),
                ownerMode = normalizeAllowedRuntimeString(
                    botAgentCoreMap["ownerMode"] as? String,
                    allowed = setOf("standard", "diagnostic"),
                    fallback = AiRuntimeBotAgentCore().ownerMode,
                ),
                killSwitch = botAgentCoreMap["killSwitch"] as? Boolean ?: false,
            ),
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
        "knowledge" to mapOf(
            "googleDrive" to mapOf(
                "isEnabled" to knowledge.googleDrive.isEnabled,
                "strictSourceMode" to knowledge.googleDrive.strictSourceMode,
                "requireSourceCitations" to knowledge.googleDrive.requireSourceCitations,
                "allowedSharedDriveIds" to normalizeRuntimeStringList(
                    knowledge.googleDrive.allowedSharedDriveIds,
                    fallback = emptyList(),
                ),
                "allowedFolderIds" to normalizeRuntimeStringList(
                    knowledge.googleDrive.allowedFolderIds,
                    fallback = emptyList(),
                ),
            ),
        ),
        "bot" to mapOf(
            "promptVersion" to normalizeRuntimeString(bot.promptVersion, AiRuntimeBotSettings().promptVersion),
            "qualityMode" to normalizeAllowedRuntimeString(bot.qualityMode, setOf("balanced", "high"), AiRuntimeBotSettings().qualityMode),
            "faqMode" to normalizeAllowedRuntimeString(bot.faqMode, setOf("off", "auto", "prefer_faq"), AiRuntimeBotSettings().faqMode),
            "ownerMode" to normalizeAllowedRuntimeString(bot.ownerMode, setOf("standard", "diagnostic"), AiRuntimeBotSettings().ownerMode),
            "answerLength" to normalizeAllowedRuntimeString(bot.answerLength, setOf("adaptive", "short", "detailed"), AiRuntimeBotSettings().answerLength),
            "personalityStyle" to normalizeRuntimeString(bot.personalityStyle, AiRuntimeBotSettings().personalityStyle, maxLength = 160),
            "loggingLevel" to normalizeRuntimeString(bot.loggingLevel, AiRuntimeBotSettings().loggingLevel, maxLength = 80),
            "diagnosticsMode" to normalizeAllowedRuntimeString(bot.diagnosticsMode, setOf("off", "owner_only", "verbose"), AiRuntimeBotSettings().diagnosticsMode),
            "killSwitchEnabled" to bot.killSwitchEnabled,
            "modelPolicy" to mapOf(
                "textPrimaryModel" to normalizeRuntimeString(bot.modelPolicy.textPrimaryModel, AiRuntimeBotModelPolicy().textPrimaryModel),
                "textFallbackModel" to normalizeRuntimeString(bot.modelPolicy.textFallbackModel, AiRuntimeBotModelPolicy().textFallbackModel),
                "visualPrimaryModel" to normalizeRuntimeString(bot.modelPolicy.visualPrimaryModel, AiRuntimeBotModelPolicy().visualPrimaryModel),
                "visualFallbackModel" to normalizeRuntimeString(bot.modelPolicy.visualFallbackModel, AiRuntimeBotModelPolicy().visualFallbackModel),
            ),
            "costGuard" to mapOf(
                "enabled" to bot.costGuard.enabled,
                "preferBriefAnswersWhenCritical" to bot.costGuard.preferBriefAnswersWhenCritical,
                "shortAnswerMaxOutputTokens" to bot.costGuard.shortAnswerMaxOutputTokens.coerceAtLeast(80),
                "standardAnswerMaxOutputTokens" to bot.costGuard.standardAnswerMaxOutputTokens.coerceAtLeast(120),
            ),
            "routingPolicy" to mapOf(
                "preferFaqWhenTopicMatched" to bot.routingPolicy.preferFaqWhenTopicMatched,
                "preferProductGuideForNewUsers" to bot.routingPolicy.preferProductGuideForNewUsers,
                "allowVisualGeneration" to bot.routingPolicy.allowVisualGeneration,
            ),
            "fallbackPolicy" to mapOf(
                "allowTextFallback" to bot.fallbackPolicy.allowTextFallback,
                "allowVisualFallback" to bot.fallbackPolicy.allowVisualFallback,
                "exposeFallbackReason" to bot.fallbackPolicy.exposeFallbackReason,
            ),
            "safetyPolicy" to mapOf(
                "safeModeEnabled" to bot.safetyPolicy.safeModeEnabled,
                "strictUnknownHandling" to bot.safetyPolicy.strictUnknownHandling,
                "blockSpeculativeFaqAnswers" to bot.safetyPolicy.blockSpeculativeFaqAnswers,
            ),
            "actionLayer" to mapOf(
                "proactiveHintsEnabled" to bot.actionLayer.proactiveHintsEnabled,
                "triggerAiLimitNearEnabled" to bot.actionLayer.triggerAiLimitNearEnabled,
                "triggerRestoreAvailableEnabled" to bot.actionLayer.triggerRestoreAvailableEnabled,
                "triggerOrderShippedEnabled" to bot.actionLayer.triggerOrderShippedEnabled,
                "triggerPaymentMethodsChangedEnabled" to bot.actionLayer.triggerPaymentMethodsChangedEnabled,
                "triggerUsageBasedUpgradeEnabled" to bot.actionLayer.triggerUsageBasedUpgradeEnabled,
                "warningThresholdPercent" to bot.actionLayer.warningThresholdPercent.coerceIn(50, 99),
                "criticalThresholdPercent" to bot.actionLayer.criticalThresholdPercent.coerceIn(60, 100),
                "upgradeHintFreeToProText" to normalizeRuntimeString(
                    bot.actionLayer.upgradeHintFreeToProText,
                    fallback = AiRuntimeBotActionLayer().upgradeHintFreeToProText,
                    maxLength = 220,
                ),
                "upgradeHintProToCreatorText" to normalizeRuntimeString(
                    bot.actionLayer.upgradeHintProToCreatorText,
                    fallback = AiRuntimeBotActionLayer().upgradeHintProToCreatorText,
                    maxLength = 220,
                ),
                "faqPriorityMode" to normalizeAllowedRuntimeString(
                    bot.actionLayer.faqPriorityMode,
                    allowed = setOf("live_owner_generic", "owner_live_generic", "balanced"),
                    fallback = AiRuntimeBotActionLayer().faqPriorityMode,
                ),
                "promptVersionAlias" to normalizeRuntimeString(
                    bot.actionLayer.promptVersionAlias,
                    fallback = AiRuntimeBotActionLayer().promptVersionAlias,
                ),
            ),
            "agentCore" to mapOf(
                "allowedTasks" to normalizeRuntimeStringList(
                    bot.agentCore.allowedTasks,
                    fallback = AiRuntimeBotAgentCore().allowedTasks,
                ),
                "blockedTasks" to normalizeRuntimeStringList(
                    bot.agentCore.blockedTasks,
                    fallback = emptyList(),
                ),
                "toolPolicy" to mapOf(
                    "allowedTools" to normalizeRuntimeStringList(
                        bot.agentCore.allowedTools,
                        fallback = AiRuntimeBotAgentCore().allowedTools,
                    ),
                    "allowWorkflowAutomation" to bot.agentCore.allowWorkflowAutomation,
                ),
                "confirmationPolicy" to mapOf(
                    "requireConfirmationForCommerce" to bot.agentCore.requireConfirmationForCommerce,
                    "requireConfirmationForOwnerOps" to bot.agentCore.requireConfirmationForOwnerOps,
                ),
                "safetyPolicy" to mapOf(
                    "blockWhenKillSwitchEnabled" to bot.agentCore.blockWhenKillSwitchEnabled,
                    "blockUnknownTasks" to bot.agentCore.blockUnknownTasks,
                ),
                "externalPolicy" to mapOf(
                    "activepiecesEnabled" to bot.agentCore.activepiecesEnabled,
                    "n8nEnabled" to bot.agentCore.n8nEnabled,
                    "manusEnabled" to bot.agentCore.manusEnabled,
                    "allowedExternalTaskTypes" to normalizeRuntimeStringList(
                        bot.agentCore.allowedExternalTaskTypes,
                        fallback = AiRuntimeBotAgentCore().allowedExternalTaskTypes,
                    ),
                    "providerPriority" to normalizeRuntimeStringList(
                        bot.agentCore.providerPriority,
                        fallback = AiRuntimeBotAgentCore().providerPriority,
                    ),
                    "maxExternalCallsPerRequest" to bot.agentCore.maxExternalCallsPerRequest.coerceIn(0, 3),
                    "externalTimeoutMs" to bot.agentCore.externalTimeoutMs.coerceIn(2000, 30000),
                    "externalRetryAttempts" to bot.agentCore.externalRetryAttempts.coerceIn(0, 4),
                ),
                "diagnosticsMode" to normalizeAllowedRuntimeString(
                    bot.agentCore.diagnosticsMode,
                    allowed = setOf("off", "owner_only", "verbose"),
                    fallback = AiRuntimeBotAgentCore().diagnosticsMode,
                ),
                "ownerMode" to normalizeAllowedRuntimeString(
                    bot.agentCore.ownerMode,
                    allowed = setOf("standard", "diagnostic"),
                    fallback = AiRuntimeBotAgentCore().ownerMode,
                ),
                "killSwitch" to bot.agentCore.killSwitch,
            ),
        ),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

private fun decodeRuntimeLimits(
    rawValue: Any?,
    fallback: AiRuntimeKindLimits,
): AiRuntimeKindLimits {
    val map = rawValue.asStringKeyedMap()
    return AiRuntimeKindLimits(
        text = parseRuntimeInt(map["text"], fallback = fallback.text, min = 1, max = 100000),
        visual = parseRuntimeInt(map["visual"], fallback = fallback.visual, min = 1, max = 100000),
        agent = parseRuntimeInt(map["agent"], fallback = fallback.agent, min = 1, max = 100000),
    )
}

private fun Any?.asStringKeyedMap(): Map<String, Any?> {
    val rawMap = this as? Map<*, *> ?: return emptyMap()
    return rawMap.entries.mapNotNull { (key, value) ->
        (key as? String)?.let { typedKey -> typedKey to value }
    }.toMap()
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

private fun normalizeAllowedRuntimeString(
    value: String?,
    allowed: Set<String>,
    fallback: String,
): String {
    val normalized = value?.trim()?.lowercase().orEmpty()
    return if (allowed.contains(normalized)) normalized else fallback
}

private fun normalizeRuntimeString(
    value: String?,
    fallback: String,
    maxLength: Int = 120,
): String {
    val normalized = value?.trim().orEmpty()
    return if (normalized.isBlank()) fallback else normalized.take(maxLength)
}

private fun normalizeRuntimeStringList(
    value: Any?,
    fallback: List<String>,
): List<String> {
    val entries = when (value) {
        is List<*> -> value.mapNotNull { it as? String }
        else -> return fallback
    }
    val normalized = entries
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.take(80) }
        .distinct()
        .sorted()
    return if (normalized.isEmpty()) fallback else normalized
}
