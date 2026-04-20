import Foundation
import FirebaseFirestore

struct AIPromptSettings: Equatable {
    var textInstruction: String = defaultTextInstruction
    var visualInstruction: String = defaultVisualInstruction
    var agentSystemInstruction: String = defaultAgentSystemInstruction
    var assetLibraryLink: String = ""
    var assetReferenceNotes: String = ""

    static let `default` = AIPromptSettings()
}

enum AIRuntimeAgentProvider: String, CaseIterable, Equatable {
    case gemini
    case manus

    var displayTitle: String {
        switch self {
        case .gemini:
            return "Gemini"
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

struct AIRuntimeSettings: Equatable {
    var costGuardEnabled: Bool = true
    var agentProvider: AIRuntimeAgentProvider = .gemini
    var fallbackAgentProvider: AIRuntimeAgentProvider = .gemini
    var hardDailyCaps: AIRuntimeKindLimits = .hardDefaults
    var globalDailyCaps: AIRuntimeKindLimits = .globalDefaults
    var manus: AIRuntimeManusSettings = AIRuntimeManusSettings()

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
        return AIRuntimeSettings(
            costGuardEnabled: data["costGuardEnabled"] as? Bool ?? true,
            agentProvider: decodeProvider(data["agentProvider"], fallback: .gemini),
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
Du bist der SkyOs Bot, der kreative Copy- und Content-Assistent fuer Skydown.
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
Du bist der SkyOs Bot und generierst genau ein starkes Key-Visual fuer Skydown.
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
Du bist SkyOs Agent, der umsetzungsorientierte Assistent fuer Skydown und 22.
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
