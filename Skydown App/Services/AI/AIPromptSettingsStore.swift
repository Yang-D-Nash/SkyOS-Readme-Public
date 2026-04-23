import Foundation
import FirebaseFirestore

struct AIPromptSettings: Equatable {
    var textInstruction: String = defaultTextInstruction
    var visualInstruction: String = defaultVisualInstruction
    var agentSystemInstruction: String = defaultAgentSystemInstruction
    var faqInstruction: String = defaultFAQInstruction
    var faqKnowledgeBase: String = defaultFAQKnowledgeBase
    var assetLibraryLink: String = ""
    var assetReferenceNotes: String = ""

    static let `default` = AIPromptSettings()
}

enum AIRuntimeAgentProvider: String, CaseIterable, Equatable {
    case gemini
    case grok
    case manus

    var displayTitle: String {
        switch self {
        case .gemini:
            return "Gemini"
        case .grok:
            return "Grok"
        case .manus:
            return "Manus"
        }
    }
}

struct AIRuntimeKindLimits: Equatable {
    var text: Int
    var visual: Int
    var agent: Int

    static let hardDefaults = AIRuntimeKindLimits(text: 120, visual: 20, agent: 40)
    static let globalDefaults = AIRuntimeKindLimits(text: 1500, visual: 180, agent: 350)
}

struct AIRuntimeManusSettings: Equatable {
    var isEnabled: Bool = false
    var requestTimeoutMs: Int = 12_000
    var pollIntervalMs: Int = 1_500
    var maxPollAttempts: Int = 18
    var listMessagesLimit: Int = 30
    var maxPromptChars: Int = 2_400
    var maxHistoryTurns: Int = 12
    var autoStopOnWaiting: Bool = true
    var blockHighCreditEvents: Bool = true
    var includeVerboseEvents: Bool = false
}

struct AIRuntimeBotModelPolicy: Equatable {
    var textPrimaryModel: String = "gemini-2.5-flash-lite"
    var textFallbackModel: String = "gemini-2.5-flash-lite"
    var visualPrimaryModel: String = "gemini-2.5-flash-image"
    var visualFallbackModel: String = "imagen-3.0-generate-002"
}

struct AIRuntimeBotCostGuard: Equatable {
    var enabled: Bool = true
    var preferBriefAnswersWhenCritical: Bool = true
    var shortAnswerMaxOutputTokens: Int = 240
    var standardAnswerMaxOutputTokens: Int = 768
}

struct AIRuntimeBotRoutingPolicy: Equatable {
    var preferFaqWhenTopicMatched: Bool = true
    var preferProductGuideForNewUsers: Bool = true
    var allowVisualGeneration: Bool = true
}

struct AIRuntimeBotFallbackPolicy: Equatable {
    var allowTextFallback: Bool = true
    var allowVisualFallback: Bool = true
    var exposeFallbackReason: Bool = true
}

struct AIRuntimeBotSafetyPolicy: Equatable {
    var safeModeEnabled: Bool = true
    var strictUnknownHandling: Bool = true
    var blockSpeculativeFaqAnswers: Bool = true
}

struct AIRuntimeBotActionLayer: Equatable {
    var proactiveHintsEnabled: Bool = true
    var triggerAiLimitNearEnabled: Bool = true
    var triggerRestoreAvailableEnabled: Bool = true
    var triggerOrderShippedEnabled: Bool = true
    var triggerPaymentMethodsChangedEnabled: Bool = true
    var triggerUsageBasedUpgradeEnabled: Bool = true
    var warningThresholdPercent: Int = 70
    var criticalThresholdPercent: Int = 90
    var upgradeHintFreeToProText: String = "Deine Nutzung ist hoch. Ein Upgrade auf Pro reduziert Abbrueche durch Limits."
    var upgradeHintProToCreatorText: String = "Deine Nutzung ist hoch. Creator kann dir mehr Workflow-Tiefe und Reserve geben."
    var faqPriorityMode: String = "live_owner_generic"
    var promptVersionAlias: String = "bot-max-v1"
}

struct AIRuntimeBotSettings: Equatable {
    var promptVersion: String = "bot-max-v1"
    var qualityMode: String = "balanced"
    var faqMode: String = "auto"
    var ownerMode: String = "standard"
    var answerLength: String = "adaptive"
    var personalityStyle: String = "calm_precise"
    var loggingLevel: String = "standard"
    var diagnosticsMode: String = "owner_only"
    var killSwitchEnabled: Bool = false
    var modelPolicy: AIRuntimeBotModelPolicy = AIRuntimeBotModelPolicy()
    var costGuard: AIRuntimeBotCostGuard = AIRuntimeBotCostGuard()
    var routingPolicy: AIRuntimeBotRoutingPolicy = AIRuntimeBotRoutingPolicy()
    var fallbackPolicy: AIRuntimeBotFallbackPolicy = AIRuntimeBotFallbackPolicy()
    var safetyPolicy: AIRuntimeBotSafetyPolicy = AIRuntimeBotSafetyPolicy()
    var actionLayer: AIRuntimeBotActionLayer = AIRuntimeBotActionLayer()
}

struct AIRuntimeSettings: Equatable {
    var costGuardEnabled: Bool = true
    var agentProvider: AIRuntimeAgentProvider = .grok
    var fallbackAgentProvider: AIRuntimeAgentProvider = .gemini
    var hardDailyCaps: AIRuntimeKindLimits = .hardDefaults
    var globalDailyCaps: AIRuntimeKindLimits = .globalDefaults
    var manus: AIRuntimeManusSettings = AIRuntimeManusSettings()
    var bot: AIRuntimeBotSettings = AIRuntimeBotSettings()

    static let `default` = AIRuntimeSettings()
}

protocol AIPromptSettingsServicing {
    func observeSettings(
        _ onChange: @escaping @MainActor (Result<AIPromptSettings, Error>) -> Void
    ) -> () -> Void
    func updateSettings(_ settings: AIPromptSettings) async throws
}

protocol AIRuntimeSettingsServicing {
    func observeSettings(
        _ onChange: @escaping @MainActor (Result<AIRuntimeSettings, Error>) -> Void
    ) -> () -> Void
    func updateSettings(_ settings: AIRuntimeSettings) async throws
}

final class FirestoreAIPromptSettingsService: AIPromptSettingsServicing {
    private let firestore: Firestore
    private let collectionName = "adminConfig"
    private let documentName = "aiPromptSettings"

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func observeSettings(
        _ onChange: @escaping @MainActor (Result<AIPromptSettings, Error>) -> Void
    ) -> () -> Void {
        let listener = firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                onChange(.success(Self.decode(snapshot?.data() ?? [:])))
            }
        }

        return {
            listener.remove()
        }
    }

    func updateSettings(_ settings: AIPromptSettings) async throws {
        try await firestore.collection(collectionName).document(documentName).setData(Self.encode(settings), merge: true)
    }

    private static func decode(_ data: [String: Any]) -> AIPromptSettings {
        AIPromptSettings(
            textInstruction: normalizeInstruction(
                data["textInstruction"] as? String,
                fallback: defaultTextInstruction
            ),
            visualInstruction: normalizeInstruction(
                data["visualInstruction"] as? String,
                fallback: defaultVisualInstruction
            ),
            agentSystemInstruction: normalizeInstruction(
                data["agentSystemInstruction"] as? String,
                fallback: defaultAgentSystemInstruction
            ),
            faqInstruction: normalizeInstruction(
                data["faqInstruction"] as? String,
                fallback: defaultFAQInstruction
            ),
            faqKnowledgeBase: normalizeInstruction(
                data["faqKnowledgeBase"] as? String,
                fallback: defaultFAQKnowledgeBase
            ),
            assetLibraryLink: normalizePromptLink(data["assetLibraryLink"] as? String) ?? "",
            assetReferenceNotes: normalizeInstruction(
                data["assetReferenceNotes"] as? String,
                fallback: ""
            )
        )
    }

    private static func encode(_ settings: AIPromptSettings) -> [String: Any] {
        [
            "textInstruction": normalizeInstruction(
                settings.textInstruction,
                fallback: defaultTextInstruction
            ),
            "visualInstruction": normalizeInstruction(
                settings.visualInstruction,
                fallback: defaultVisualInstruction
            ),
            "agentSystemInstruction": normalizeInstruction(
                settings.agentSystemInstruction,
                fallback: defaultAgentSystemInstruction
            ),
            "faqInstruction": normalizeInstruction(
                settings.faqInstruction,
                fallback: defaultFAQInstruction
            ),
            "faqKnowledgeBase": normalizeInstruction(
                settings.faqKnowledgeBase,
                fallback: defaultFAQKnowledgeBase
            ),
            "assetLibraryLink": normalizePromptLink(settings.assetLibraryLink) ?? "",
            "assetReferenceNotes": normalizeInstruction(
                settings.assetReferenceNotes,
                fallback: ""
            ),
            "updatedAt": FieldValue.serverTimestamp()
        ]
    }
}

final class FirestoreAIRuntimeSettingsService: AIRuntimeSettingsServicing {
    private let firestore: Firestore
    private let collectionName = "adminConfig"
    private let documentName = "aiRuntime"

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func observeSettings(
        _ onChange: @escaping @MainActor (Result<AIRuntimeSettings, Error>) -> Void
    ) -> () -> Void {
        let listener = firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                onChange(.success(Self.decode(snapshot?.data() ?? [:])))
            }
        }

        return {
            listener.remove()
        }
    }

    func updateSettings(_ settings: AIRuntimeSettings) async throws {
        try await firestore.collection(collectionName).document(documentName).setData(Self.encode(settings), merge: true)
    }

    private static func decode(_ data: [String: Any]) -> AIRuntimeSettings {
        let manusData = data["manus"] as? [String: Any] ?? [:]
        let botData = data["bot"] as? [String: Any] ?? [:]
        let botModelPolicy = botData["modelPolicy"] as? [String: Any] ?? [:]
        let botCostGuard = botData["costGuard"] as? [String: Any] ?? [:]
        let botRouting = botData["routingPolicy"] as? [String: Any] ?? [:]
        let botFallback = botData["fallbackPolicy"] as? [String: Any] ?? [:]
        let botSafety = botData["safetyPolicy"] as? [String: Any] ?? [:]
        let botActionLayer = botData["actionLayer"] as? [String: Any] ?? [:]
        return AIRuntimeSettings(
            costGuardEnabled: data["costGuardEnabled"] as? Bool ?? true,
            agentProvider: decodeProvider(data["agentProvider"], fallback: .grok),
            fallbackAgentProvider: decodeProvider(data["fallbackAgentProvider"], fallback: .gemini),
            hardDailyCaps: decodeLimits(
                data["hardDailyCaps"],
                fallback: .hardDefaults
            ),
            globalDailyCaps: decodeLimits(
                data["globalDailyCaps"],
                fallback: .globalDefaults
            ),
            manus: AIRuntimeManusSettings(
                isEnabled: manusData["isEnabled"] as? Bool ?? false,
                requestTimeoutMs: clampInt(
                    manusData["requestTimeoutMs"],
                    fallback: AIRuntimeSettings.default.manus.requestTimeoutMs,
                    min: 3_000,
                    max: 30_000
                ),
                pollIntervalMs: clampInt(
                    manusData["pollIntervalMs"],
                    fallback: AIRuntimeSettings.default.manus.pollIntervalMs,
                    min: 500,
                    max: 5_000
                ),
                maxPollAttempts: clampInt(
                    manusData["maxPollAttempts"],
                    fallback: AIRuntimeSettings.default.manus.maxPollAttempts,
                    min: 2,
                    max: 60
                ),
                listMessagesLimit: clampInt(
                    manusData["listMessagesLimit"],
                    fallback: AIRuntimeSettings.default.manus.listMessagesLimit,
                    min: 5,
                    max: 100
                ),
                maxPromptChars: clampInt(
                    manusData["maxPromptChars"],
                    fallback: AIRuntimeSettings.default.manus.maxPromptChars,
                    min: 300,
                    max: 12_000
                ),
                maxHistoryTurns: clampInt(
                    manusData["maxHistoryTurns"],
                    fallback: AIRuntimeSettings.default.manus.maxHistoryTurns,
                    min: 0,
                    max: 24
                ),
                autoStopOnWaiting: manusData["autoStopOnWaiting"] as? Bool ?? true,
                blockHighCreditEvents: manusData["blockHighCreditEvents"] as? Bool ?? true,
                includeVerboseEvents: manusData["includeVerboseEvents"] as? Bool ?? false
            ),
            bot: AIRuntimeBotSettings(
                promptVersion: normalizeShortString(
                    botData["promptVersion"] as? String,
                    fallback: AIRuntimeSettings.default.bot.promptVersion
                ),
                qualityMode: normalizeAllowedString(
                    botData["qualityMode"] as? String,
                    allowed: ["balanced", "high"],
                    fallback: AIRuntimeSettings.default.bot.qualityMode
                ),
                faqMode: normalizeAllowedString(
                    botData["faqMode"] as? String,
                    allowed: ["off", "auto", "prefer_faq"],
                    fallback: AIRuntimeSettings.default.bot.faqMode
                ),
                ownerMode: normalizeAllowedString(
                    botData["ownerMode"] as? String,
                    allowed: ["standard", "diagnostic"],
                    fallback: AIRuntimeSettings.default.bot.ownerMode
                ),
                answerLength: normalizeAllowedString(
                    botData["answerLength"] as? String,
                    allowed: ["adaptive", "short", "detailed"],
                    fallback: AIRuntimeSettings.default.bot.answerLength
                ),
                personalityStyle: normalizeShortString(
                    botData["personalityStyle"] as? String,
                    fallback: AIRuntimeSettings.default.bot.personalityStyle,
                    maxLength: 160
                ),
                loggingLevel: normalizeShortString(
                    botData["loggingLevel"] as? String,
                    fallback: AIRuntimeSettings.default.bot.loggingLevel,
                    maxLength: 80
                ),
                diagnosticsMode: normalizeAllowedString(
                    botData["diagnosticsMode"] as? String,
                    allowed: ["off", "owner_only", "verbose"],
                    fallback: AIRuntimeSettings.default.bot.diagnosticsMode
                ),
                killSwitchEnabled: botData["killSwitchEnabled"] as? Bool ?? false,
                modelPolicy: AIRuntimeBotModelPolicy(
                    textPrimaryModel: normalizeShortString(
                        botModelPolicy["textPrimaryModel"] as? String,
                        fallback: AIRuntimeSettings.default.bot.modelPolicy.textPrimaryModel
                    ),
                    textFallbackModel: normalizeShortString(
                        botModelPolicy["textFallbackModel"] as? String,
                        fallback: AIRuntimeSettings.default.bot.modelPolicy.textFallbackModel
                    ),
                    visualPrimaryModel: normalizeShortString(
                        botModelPolicy["visualPrimaryModel"] as? String,
                        fallback: AIRuntimeSettings.default.bot.modelPolicy.visualPrimaryModel
                    ),
                    visualFallbackModel: normalizeShortString(
                        botModelPolicy["visualFallbackModel"] as? String,
                        fallback: AIRuntimeSettings.default.bot.modelPolicy.visualFallbackModel
                    )
                ),
                costGuard: AIRuntimeBotCostGuard(
                    enabled: botCostGuard["enabled"] as? Bool ?? true,
                    preferBriefAnswersWhenCritical: botCostGuard["preferBriefAnswersWhenCritical"] as? Bool ?? true,
                    shortAnswerMaxOutputTokens: clampInt(
                        botCostGuard["shortAnswerMaxOutputTokens"],
                        fallback: AIRuntimeSettings.default.bot.costGuard.shortAnswerMaxOutputTokens,
                        min: 80,
                        max: 1_200
                    ),
                    standardAnswerMaxOutputTokens: clampInt(
                        botCostGuard["standardAnswerMaxOutputTokens"],
                        fallback: AIRuntimeSettings.default.bot.costGuard.standardAnswerMaxOutputTokens,
                        min: 120,
                        max: 2_400
                    )
                ),
                routingPolicy: AIRuntimeBotRoutingPolicy(
                    preferFaqWhenTopicMatched: botRouting["preferFaqWhenTopicMatched"] as? Bool ?? true,
                    preferProductGuideForNewUsers: botRouting["preferProductGuideForNewUsers"] as? Bool ?? true,
                    allowVisualGeneration: botRouting["allowVisualGeneration"] as? Bool ?? true
                ),
                fallbackPolicy: AIRuntimeBotFallbackPolicy(
                    allowTextFallback: botFallback["allowTextFallback"] as? Bool ?? true,
                    allowVisualFallback: botFallback["allowVisualFallback"] as? Bool ?? true,
                    exposeFallbackReason: botFallback["exposeFallbackReason"] as? Bool ?? true
                ),
                safetyPolicy: AIRuntimeBotSafetyPolicy(
                    safeModeEnabled: botSafety["safeModeEnabled"] as? Bool ?? true,
                    strictUnknownHandling: botSafety["strictUnknownHandling"] as? Bool ?? true,
                    blockSpeculativeFaqAnswers: botSafety["blockSpeculativeFaqAnswers"] as? Bool ?? true
                ),
                actionLayer: AIRuntimeBotActionLayer(
                    proactiveHintsEnabled: botActionLayer["proactiveHintsEnabled"] as? Bool ?? true,
                    triggerAiLimitNearEnabled: botActionLayer["triggerAiLimitNearEnabled"] as? Bool ?? true,
                    triggerRestoreAvailableEnabled: botActionLayer["triggerRestoreAvailableEnabled"] as? Bool ?? true,
                    triggerOrderShippedEnabled: botActionLayer["triggerOrderShippedEnabled"] as? Bool ?? true,
                    triggerPaymentMethodsChangedEnabled: botActionLayer["triggerPaymentMethodsChangedEnabled"] as? Bool ?? true,
                    triggerUsageBasedUpgradeEnabled: botActionLayer["triggerUsageBasedUpgradeEnabled"] as? Bool ?? true,
                    warningThresholdPercent: clampInt(
                        botActionLayer["warningThresholdPercent"],
                        fallback: AIRuntimeSettings.default.bot.actionLayer.warningThresholdPercent,
                        min: 50,
                        max: 99
                    ),
                    criticalThresholdPercent: clampInt(
                        botActionLayer["criticalThresholdPercent"],
                        fallback: AIRuntimeSettings.default.bot.actionLayer.criticalThresholdPercent,
                        min: 60,
                        max: 100
                    ),
                    upgradeHintFreeToProText: normalizeShortString(
                        botActionLayer["upgradeHintFreeToProText"] as? String,
                        fallback: AIRuntimeSettings.default.bot.actionLayer.upgradeHintFreeToProText,
                        maxLength: 220
                    ),
                    upgradeHintProToCreatorText: normalizeShortString(
                        botActionLayer["upgradeHintProToCreatorText"] as? String,
                        fallback: AIRuntimeSettings.default.bot.actionLayer.upgradeHintProToCreatorText,
                        maxLength: 220
                    ),
                    faqPriorityMode: normalizeAllowedString(
                        botActionLayer["faqPriorityMode"] as? String,
                        allowed: ["live_owner_generic", "owner_live_generic", "balanced"],
                        fallback: AIRuntimeSettings.default.bot.actionLayer.faqPriorityMode
                    ),
                    promptVersionAlias: normalizeShortString(
                        botActionLayer["promptVersionAlias"] as? String,
                        fallback: AIRuntimeSettings.default.bot.actionLayer.promptVersionAlias
                    )
                )
            )
        )
    }

    private static func encode(_ settings: AIRuntimeSettings) -> [String: Any] {
        [
            "costGuardEnabled": settings.costGuardEnabled,
            "agentProvider": settings.agentProvider.rawValue,
            "fallbackAgentProvider": settings.fallbackAgentProvider.rawValue,
            "hardDailyCaps": [
                "text": max(1, settings.hardDailyCaps.text),
                "visual": max(1, settings.hardDailyCaps.visual),
                "agent": max(1, settings.hardDailyCaps.agent)
            ],
            "globalDailyCaps": [
                "text": max(1, settings.globalDailyCaps.text),
                "visual": max(1, settings.globalDailyCaps.visual),
                "agent": max(1, settings.globalDailyCaps.agent)
            ],
            "manus": [
                "isEnabled": settings.manus.isEnabled,
                "requestTimeoutMs": max(3_000, settings.manus.requestTimeoutMs),
                "pollIntervalMs": max(500, settings.manus.pollIntervalMs),
                "maxPollAttempts": max(2, settings.manus.maxPollAttempts),
                "listMessagesLimit": max(5, settings.manus.listMessagesLimit),
                "maxPromptChars": max(300, settings.manus.maxPromptChars),
                "maxHistoryTurns": max(0, settings.manus.maxHistoryTurns),
                "autoStopOnWaiting": settings.manus.autoStopOnWaiting,
                "blockHighCreditEvents": settings.manus.blockHighCreditEvents,
                "includeVerboseEvents": settings.manus.includeVerboseEvents
            ],
            "bot": [
                "promptVersion": normalizeShortString(
                    settings.bot.promptVersion,
                    fallback: AIRuntimeSettings.default.bot.promptVersion
                ),
                "qualityMode": normalizeAllowedString(
                    settings.bot.qualityMode,
                    allowed: ["balanced", "high"],
                    fallback: AIRuntimeSettings.default.bot.qualityMode
                ),
                "faqMode": normalizeAllowedString(
                    settings.bot.faqMode,
                    allowed: ["off", "auto", "prefer_faq"],
                    fallback: AIRuntimeSettings.default.bot.faqMode
                ),
                "ownerMode": normalizeAllowedString(
                    settings.bot.ownerMode,
                    allowed: ["standard", "diagnostic"],
                    fallback: AIRuntimeSettings.default.bot.ownerMode
                ),
                "answerLength": normalizeAllowedString(
                    settings.bot.answerLength,
                    allowed: ["adaptive", "short", "detailed"],
                    fallback: AIRuntimeSettings.default.bot.answerLength
                ),
                "personalityStyle": normalizeShortString(
                    settings.bot.personalityStyle,
                    fallback: AIRuntimeSettings.default.bot.personalityStyle,
                    maxLength: 160
                ),
                "loggingLevel": normalizeShortString(
                    settings.bot.loggingLevel,
                    fallback: AIRuntimeSettings.default.bot.loggingLevel,
                    maxLength: 80
                ),
                "diagnosticsMode": normalizeAllowedString(
                    settings.bot.diagnosticsMode,
                    allowed: ["off", "owner_only", "verbose"],
                    fallback: AIRuntimeSettings.default.bot.diagnosticsMode
                ),
                "killSwitchEnabled": settings.bot.killSwitchEnabled,
                "modelPolicy": [
                    "textPrimaryModel": normalizeShortString(
                        settings.bot.modelPolicy.textPrimaryModel,
                        fallback: AIRuntimeSettings.default.bot.modelPolicy.textPrimaryModel
                    ),
                    "textFallbackModel": normalizeShortString(
                        settings.bot.modelPolicy.textFallbackModel,
                        fallback: AIRuntimeSettings.default.bot.modelPolicy.textFallbackModel
                    ),
                    "visualPrimaryModel": normalizeShortString(
                        settings.bot.modelPolicy.visualPrimaryModel,
                        fallback: AIRuntimeSettings.default.bot.modelPolicy.visualPrimaryModel
                    ),
                    "visualFallbackModel": normalizeShortString(
                        settings.bot.modelPolicy.visualFallbackModel,
                        fallback: AIRuntimeSettings.default.bot.modelPolicy.visualFallbackModel
                    )
                ],
                "costGuard": [
                    "enabled": settings.bot.costGuard.enabled,
                    "preferBriefAnswersWhenCritical": settings.bot.costGuard.preferBriefAnswersWhenCritical,
                    "shortAnswerMaxOutputTokens": max(80, settings.bot.costGuard.shortAnswerMaxOutputTokens),
                    "standardAnswerMaxOutputTokens": max(120, settings.bot.costGuard.standardAnswerMaxOutputTokens)
                ],
                "routingPolicy": [
                    "preferFaqWhenTopicMatched": settings.bot.routingPolicy.preferFaqWhenTopicMatched,
                    "preferProductGuideForNewUsers": settings.bot.routingPolicy.preferProductGuideForNewUsers,
                    "allowVisualGeneration": settings.bot.routingPolicy.allowVisualGeneration
                ],
                "fallbackPolicy": [
                    "allowTextFallback": settings.bot.fallbackPolicy.allowTextFallback,
                    "allowVisualFallback": settings.bot.fallbackPolicy.allowVisualFallback,
                    "exposeFallbackReason": settings.bot.fallbackPolicy.exposeFallbackReason
                ],
                "safetyPolicy": [
                    "safeModeEnabled": settings.bot.safetyPolicy.safeModeEnabled,
                    "strictUnknownHandling": settings.bot.safetyPolicy.strictUnknownHandling,
                    "blockSpeculativeFaqAnswers": settings.bot.safetyPolicy.blockSpeculativeFaqAnswers
                ],
                "actionLayer": [
                    "proactiveHintsEnabled": settings.bot.actionLayer.proactiveHintsEnabled,
                    "triggerAiLimitNearEnabled": settings.bot.actionLayer.triggerAiLimitNearEnabled,
                    "triggerRestoreAvailableEnabled": settings.bot.actionLayer.triggerRestoreAvailableEnabled,
                    "triggerOrderShippedEnabled": settings.bot.actionLayer.triggerOrderShippedEnabled,
                    "triggerPaymentMethodsChangedEnabled": settings.bot.actionLayer.triggerPaymentMethodsChangedEnabled,
                    "triggerUsageBasedUpgradeEnabled": settings.bot.actionLayer.triggerUsageBasedUpgradeEnabled,
                    "warningThresholdPercent": Swift.max(50, Swift.min(99, settings.bot.actionLayer.warningThresholdPercent)),
                    "criticalThresholdPercent": Swift.max(60, Swift.min(100, settings.bot.actionLayer.criticalThresholdPercent)),
                    "upgradeHintFreeToProText": normalizeShortString(
                        settings.bot.actionLayer.upgradeHintFreeToProText,
                        fallback: AIRuntimeSettings.default.bot.actionLayer.upgradeHintFreeToProText,
                        maxLength: 220
                    ),
                    "upgradeHintProToCreatorText": normalizeShortString(
                        settings.bot.actionLayer.upgradeHintProToCreatorText,
                        fallback: AIRuntimeSettings.default.bot.actionLayer.upgradeHintProToCreatorText,
                        maxLength: 220
                    ),
                    "faqPriorityMode": normalizeAllowedString(
                        settings.bot.actionLayer.faqPriorityMode,
                        allowed: ["live_owner_generic", "owner_live_generic", "balanced"],
                        fallback: AIRuntimeSettings.default.bot.actionLayer.faqPriorityMode
                    ),
                    "promptVersionAlias": normalizeShortString(
                        settings.bot.actionLayer.promptVersionAlias,
                        fallback: AIRuntimeSettings.default.bot.actionLayer.promptVersionAlias
                    )
                ]
            ],
            "updatedAt": FieldValue.serverTimestamp()
        ]
    }

    private static func decodeProvider(_ value: Any?, fallback: AIRuntimeAgentProvider) -> AIRuntimeAgentProvider {
        guard let rawValue = (value as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased(),
              let resolved = AIRuntimeAgentProvider(rawValue: rawValue) else {
            return fallback
        }
        return resolved
    }

    private static func decodeLimits(_ value: Any?, fallback: AIRuntimeKindLimits) -> AIRuntimeKindLimits {
        let map = value as? [String: Any] ?? [:]
        return AIRuntimeKindLimits(
            text: clampInt(map["text"], fallback: fallback.text, min: 1, max: 100_000),
            visual: clampInt(map["visual"], fallback: fallback.visual, min: 1, max: 100_000),
            agent: clampInt(map["agent"], fallback: fallback.agent, min: 1, max: 100_000)
        )
    }

    private static func clampInt(_ value: Any?, fallback: Int, min: Int, max: Int) -> Int {
        let numericValue: Int?
        if let number = value as? NSNumber {
            numericValue = number.intValue
        } else if let string = value as? String {
            numericValue = Int(string)
        } else {
            numericValue = nil
        }

        guard let numericValue else {
            return fallback
        }

        return Swift.max(min, Swift.min(max, numericValue))
    }

    private static func normalizeAllowedString(
        _ value: String?,
        allowed: [String],
        fallback: String
    ) -> String {
        let normalized = value?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() ?? ""
        return allowed.contains(normalized) ? normalized : fallback
    }

    private static func normalizeShortString(
        _ value: String?,
        fallback: String,
        maxLength: Int = 120
    ) -> String {
        let normalized = value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !normalized.isEmpty else { return fallback }
        return String(normalized.prefix(maxLength))
    }
}

@MainActor
final class AIPromptSettingsStore: ObservableObject {
    static let shared = AIPromptSettingsStore()

    @Published private(set) var settings: AIPromptSettings = .default
    @Published private(set) var lastErrorMessage: String?

    private let service: AIPromptSettingsServicing
    private var stopObserving: (() -> Void)?
    private var isObservationEnabled = false

    init(service: AIPromptSettingsServicing = FirestoreAIPromptSettingsService()) {
        self.service = service
    }

    func setObservationEnabled(_ isEnabled: Bool) {
        guard isEnabled != isObservationEnabled else { return }
        isObservationEnabled = isEnabled

        if isEnabled {
            startObserving()
        } else {
            stopObserving?()
            stopObserving = nil
            settings = .default
            lastErrorMessage = nil
        }
    }

    func save(_ settings: AIPromptSettings) async throws {
        try await service.updateSettings(settings)
    }

    private func startObserving() {
        stopObserving?()
        stopObserving = service.observeSettings { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let settings):
                self.settings = settings
                self.lastErrorMessage = nil
            case .failure(let error):
                self.lastErrorMessage = error.localizedDescription
            }
        }
    }

    deinit {
        stopObserving?()
    }
}

@MainActor
final class AIRuntimeSettingsStore: ObservableObject {
    static let shared = AIRuntimeSettingsStore()

    @Published private(set) var settings: AIRuntimeSettings = .default
    @Published private(set) var lastErrorMessage: String?

    private let service: AIRuntimeSettingsServicing
    private var stopObserving: (() -> Void)?
    private var isObservationEnabled = false

    init(service: AIRuntimeSettingsServicing = FirestoreAIRuntimeSettingsService()) {
        self.service = service
    }

    func setObservationEnabled(_ isEnabled: Bool) {
        guard isEnabled != isObservationEnabled else { return }
        isObservationEnabled = isEnabled

        if isEnabled {
            startObserving()
        } else {
            stopObserving?()
            stopObserving = nil
            settings = .default
            lastErrorMessage = nil
        }
    }

    func save(_ settings: AIRuntimeSettings) async throws {
        try await service.updateSettings(settings)
    }

    private func startObserving() {
        stopObserving?()
        stopObserving = service.observeSettings { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let settings):
                self.settings = settings
                self.lastErrorMessage = nil
            case .failure(let error):
                self.lastErrorMessage = error.localizedDescription
            }
        }
    }

    deinit {
        stopObserving?()
    }
}

private func normalizeInstruction(_ value: String?, fallback: String) -> String {
    let normalized = value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    if normalized.isEmpty {
        return fallback
    }
    return String(normalized.prefix(12000))
}

private func normalizePromptLink(_ value: String?) -> String? {
    let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    guard !trimmed.isEmpty else { return nil }
    let prefixed: String
    if trimmed.hasPrefix("https://") || trimmed.hasPrefix("http://") {
        prefixed = trimmed
    } else {
        prefixed = "https://\(trimmed)"
    }
    return String(prefixed.prefix(2000)).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
}

private let defaultTextInstruction = """
Du bist der SkyOS Bot, der kreative Copy- und Content-Assistent fuer Skydown.
Markenkontext:
- Skydown kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
- Die App verbindet Musik, Videos, Merch und Creator-Tools.
- Yang D. Nash ist Kern der Marke und Entwickler der App.

Antworte auf Deutsch.
Sei direkt nutzbar, markentauglich, modern und nicht generisch.
Keine langen Vorreden, keine Erklaerungen ueber deinen Prozess.
Schreibe lieber Ergebnisse als Theorie.
Wenn die Anfrage nach Caption, Hook, Claim, Reel oder Post klingt, liefere echte copy-pastebare Optionen.
Wenn die Anfrage eher nach Planung, Freigaben, Briefing oder To-dos klingt, antworte kurz hilfreich, verweise aber auf den Agent fuer die tiefe Struktur.
"""

private let defaultVisualInstruction = """
Du bist der SkyOS Bot und generierst genau ein starkes Key-Visual fuer Skydown.
Markenkontext:
- Skydown kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
- Die Marke lebt von Musik, Videos, Street-Culture und Premium-Underground-Aesthetik.
- Yang D. Nash ist Kern der Marke und Entwickler der App.

Erzeuge ein modernes, hochwertiges Visual mit klarer Stimmung.
Stil: cinematic, urban, moody, premium, nicht kitschig, nicht generisch.
Wenn das Motiv wie ein Foto, Filmstill oder Editorial-Frame gedacht ist, arbeite mit praeziser Kamera-, Lens- und Lichtsprache statt mit vagen Stilwoertern.
Bevorzuge bei Foto-Motiven echte Kamera-Anmutung statt Illustration, CGI oder generischem AI-Look.
Nutze nur sehr wenig Text im Bild. Wenn Text im Motiv vorkommt, dann maximal eine kurze Headline.
Liefere neben dem Bild nur eine kurze Ein-Zeilen-Beschreibung des Looks.
Antworte auf Deutsch.
"""

private let defaultAgentSystemInstruction = """
Du bist SkyOS Agent, der umsetzungsorientierte Assistent fuer Skydown und 22.
Markenkontext:
- Skydown kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
- Die Marke arbeitet in Musik, Videos, Merch und App-Releases.
- Yang D. Nash ist Kern der Marke und Entwickler der App.

Antworte auf Deutsch, klar, modern und konkret.
Du hilfst bei Release-Planung, Briefings, Content-Strategie, Videography, Merch-Drops, Kampagnenideen, To-dos, Freigaben und naechsten Schritten.
Arbeite pragmatisch statt generisch.
Keine langen Vorreden. Keine leeren Motivationssaetze.
Wenn du planen sollst, liefere eine umsetzbare Struktur.
Wenn du ein Briefing schreiben sollst, liefere ein copy-pastebares Briefing.
Wenn Infos fehlen, triff sinnvolle Annahmen und kennzeichne sie kurz. Frage nur dann gezielt nach, wenn ohne die Info ein schlechter Plan entstehen wuerde.
Bevorzuge kurze klare Abschnitte wie Ziel, Deliverables, Schritte, Timing, Assets, Risiken, Naechste Schritte.
"""

private let defaultFAQInstruction = """
Du bist der SkyOS FAQ Core.
Antworte ruhig, klar und ehrlich.
Nutze nur bekannte Produktfakten und sage offen, wenn etwas nicht sicher vorliegt.
Erfinde keine Membership-, Versand-, Account- oder Support-Regeln.
Wenn die Antwort kurz sein kann, antworte kurz. Wenn mehr Tiefe gefragt ist, strukturiere sie sauber.
Prioritaet fuer Fakten: 1) Live Facts aus dem System, 2) Owner Knowledge, 3) vorsichtige generische Hilfe.
Antworte auf Deutsch.
"""

private let defaultFAQKnowledgeBase = """
SkyOS FAQ Knowledge Base v2

Grundprinzip:
- Ziel ist echte Hilfe, nicht Marketing-Blabla.
- Keine erfundenen Preise, Fristen, Versandversprechen, Rechtsaussagen oder Entitlements.
- Wenn ein Fakt fehlt: klar sagen, dass er nicht sicher vorliegt, und den naechsten sinnvollen Schritt nennen.
- Bei Membership, Checkout, Restore, Orders, AI-Limits und Legal immer zuerst Live Facts nutzen.

Schnellfakten:
- SkyOS verbindet Home, AI, Music, Video, Shop, Profile und Settings.
- Bot: schnelle Hilfe, FAQ, Copy und Ideen.
- Agent: strukturierte Aufgaben, Planung, Briefings und Workflows.
- Membership ist faehigkeitsbasiert und kein Token-Shop.
- Kaeufe und Restore koennen kurze Synchronisierungszeit brauchen.
- Support: skydownent@gmail.com
- Betreiberhinweis: Skydown OS, Erich-Plate-Weg 44, 22419 Hamburg, Deutschland.

Kernfragen mit Zielantworten:

[Einstieg / Getting Started]
Q1: Was ist SkyOS in einem Satz?
A1: SkyOS ist eine Creator-App, die AI, Media und Commerce in einem Flow verbindet.
Q2: Wie starte ich am besten als neuer Nutzer?
A2: Melde dich zuerst an, oeffne Home zur Orientierung und starte dann mit einer klaren Frage im Bot.
Q3: Wo sehe ich, was ich als naechstes tun soll?
A3: Nutze Home fuer Einstieg und wechsel dann gezielt in AI, Shop, Music oder Video.
Q4: Ist SkyOS eher fuer Creator oder normale Nutzer?
A4: Beides ist moeglich; Creator profitieren besonders von AI- und Workflow-Funktionen.

[Login / Account]
Q5: Ich komme nicht rein. Was pruefe ich zuerst?
A5: Verbindung pruefen, App neu oeffnen, erneut anmelden und danach Login-Methode kontrollieren.
Q6: Was tun, wenn Login weiter fehlschlaegt?
A6: Support mit Konto-E-Mail, Plattform, Uhrzeit und Screenshot kontaktieren.
Q7: Warum sehe ich manche Bereiche nicht?
A7: Meist fehlen Rolle, Freigabe oder ein aktives Entitlement.
Q8: Wird mein Verlauf beim Account-Wechsel behalten?
A8: Verlauf ist kontoabhaengig; bei Wechsel ist anderer Kontext sichtbar.

[Membership / Abo / Restore]
Q9: Welche Membership habe ich?
A9: Der Plan soll aus Live Entitlements gelesen werden, nicht aus rein lokalem UI-Status.
Q10: Welche Membership passt zu mir?
A10: Free fuer Einstieg, Pro fuer regelmaessigen Creator-Flow, Creator fuer tiefere Workflows und Prioritaet.
Q11: Warum lohnt sich ein Upgrade?
A11: Ein Upgrade reduziert Reibung bei Limits und schaltet staerkere AI-Nutzung fuer echte Produktionsarbeit frei.
Q12: Wie restore ich mein Abo?
A12: Restore im Membership-Bereich ausloesen und kurz auf Synchronisierung warten.
Q13: Restore klappt nicht. Was dann?
A13: Store-Account pruefen, App neu starten, erneut Restore; danach Support mit Konto und Zeitstempel.
Q14: Wie kuendige ich mein Abo?
A14: Kuendigung laeuft ueber den jeweiligen Store-/Abo-Manager, nicht direkt im Chat.
Q15: Wird beim Upgrade sofort umgestellt?
A15: In der Regel ja, aber die Entitlement-Sync kann kurz dauern.

[AI Features / Limits / Freischaltung]
Q16: Warum ist AI gesperrt?
A16: Haeufige Gruende sind fehlende Freigabe, Rolle, Entitlement oder ein aktiver Sicherheits-/Runtime-Block.
Q17: Warum kann ich gerade nichts mehr senden?
A17: Wahrscheinlich ist ein Tageslimit oder Cost Guard erreicht; Bot soll den Grund lesbar nennen.
Q18: Was ist der Unterschied zwischen Bot und Agent?
A18: Bot fuer schnelle Antworten, Agent fuer strukturierte laengere Aufgaben.
Q19: Warum ist eine Antwort kuerzer als erwartet?
A19: Bei aktivem Cost Guard oder knappem Limit kann die Antwort bewusst verkuerzt werden.
Q20: Welche Prompts geben bessere Ergebnisse?
A20: Klare Ziele, gewuenschter Stil, Format, Plattform und Tiefe in einem Satz helfen am meisten.

[Merch / Bestellung / Versand]
Q21: Wo ist meine Bestellung?
A21: Im Order-Bereich den Live-Status pruefen; ohne Orderdaten keine Versandprognose erfinden.
Q22: Wann kommt meine Bestellung an?
A22: Nur konkrete ETA nennen, wenn Live Versanddaten verfuegbar sind.
Q23: Ist Versand kostenlos?
A23: Nur beantworten, wenn Commerce-/Checkout-Facts es klar zeigen; sonst offen als unbekannt markieren.
Q24: Kann ich Bestellung oder Adresse nachtraeglich aendern?
A24: Das haengt vom Orderstatus und Shop-Prozess ab; ohne Fakt keine feste Zusage geben.
Q25: Warum sehe ich keine Trackingnummer?
A25: Tracking erscheint erst, wenn Versanddaten vom Fulfillment vorliegen.

[Zahlungsarten / Checkout]
Q26: Welche Zahlungsarten gibt es?
A26: Nur die aktuell verfuegbaren Payment Methods aus Live Facts nennen.
Q27: Checkout ist fehlgeschlagen. Was jetzt?
A27: Nicht mehrfach triggern, kurz warten, dann erneut versuchen und bei Fehlercode Support kontaktieren.
Q28: Wurde ich doppelt belastet?
A28: Erst Order-/Payment-Status pruefen; bei Unsicherheit Zahlungsreferenz und Zeitstempel an Support senden.
Q29: Warum ist meine Zahlung pending?
A29: Je nach Provider kann Autorisierung und Bestaetigung verzoegert eintreffen.

[Datenschutz / AGB / Hilfe]
Q30: Welche Daten speichert SkyOS?
A30: Nur bestaetigte Datenkategorien nennen; falls Legal Content fehlt, klar als nicht sicher markieren.
Q31: Wo finde ich Datenschutz und AGB?
A31: Im Legal-/Settings-Bereich; bei fehlender Anzeige Support kontaktieren.
Q32: Gibt SkyOS Daten an Dritte weiter?
A32: Nur nach legal bestaetigten Inhalten beantworten, keine Vermutungen.
Q33: Welche Daten soll ich nicht in den Bot schreiben?
A33: Keine Passwoerter, Private Keys, volle Kartendaten oder sensible Fremddaten.

[App Nutzung / Features]
Q34: Wie nutze ich SkyOS effizient im Alltag?
A34: Mit klarem Tagesziel starten, Bot fuer schnelle Aufgaben nutzen und Agent fuer tiefe Ausarbeitung.
Q35: Kann ich SkyOS auch nur fuer Content-Ideen nutzen?
A35: Ja, Bot eignet sich genau fuer schnelle Hooks, Captions und kreative Varianten.
Q36: Warum unterscheidet sich Antwortqualitaet manchmal?
A36: Modus, Promptqualitaet, Runtime-Limits und verfuegbare Fakten beeinflussen die Ausgabe.

[Creator / Owner]
Q37: Wie kann Owner eigenes FAQ-Wissen hinterlegen?
A37: Ueber FAQ / Owner Knowledge in den Prompt-Settings, damit Bot auf reale Owner-Fakten zugreift.
Q38: Was passiert ohne Owner-Eintrag?
A38: Dann nutzt der Bot nur Standardwissen und Live Facts, ohne Owner-Regeln zu erfinden.
Q39: Kann ich FAQ-Antworten markenspezifisch steuern?
A39: Ja, ueber FAQ Instruction und Owner Knowledge, solange Fakten korrekt bleiben.

[Vertrauen / Sicherheit]
Q40: Warum sollte ich der FAQ vertrauen?
A40: Weil sie unbekannte Punkte offen kennzeichnet und keine Policies halluziniert.
Q41: Wann soll ich direkt Support kontaktieren?
A41: Bei Login-Blockern, Restore-Problemen, fehlgeschlagenem Checkout, unklaren Orders oder rechtlichen Fragen.
Q42: Was braucht Support fuer schnelle Loesung?
A42: Konto-E-Mail, Plattform, betroffener Bereich, Zeitpunkt, Screenshot und ggf. Referenznummer.

[Revenue-orientierte, aber faire Hilfe]
Q43: Warum lohnt sich Creator oder Pro ohne Hard-Sell?
A43: Wenn AI Teil deines Workflows ist, sparen hoehere Plaene Zeit, Abbrueche und Kontextwechsel.
Q44: Ich bin unsicher beim Upgrade - was ist die sichere Empfehlung?
A44: Mit dem kleineren passenden Plan starten, Nutzung beobachten und bei Bedarf spaeter hochstufen.
Q45: Wie antworte ich auf "zu teuer" fair?
A45: Transparent auf Nutzen und Arbeitsersparnis verweisen, nie Druck aufbauen.

[Owner definierte Fragen]
- Owner-spezifisches FAQ-Wissen kann zusaetzlich im Feld FAQ / Owner Knowledge hinterlegt werden.
- Wenn dort nichts hinterlegt ist, darf der Bot keine erfundenen Owner-Regeln behaupten.
"""
