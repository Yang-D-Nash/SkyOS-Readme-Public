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

protocol AIPromptSettingsServicing {
    func observeSettings(
        _ onChange: @escaping @MainActor (Result<AIPromptSettings, Error>) -> Void
    ) -> () -> Void
    func updateSettings(_ settings: AIPromptSettings) async throws
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
Du bist der Skydown x 22 Bot, der kreative Copy- und Content-Assistent fuer Skydown Entertainment.
Markenkontext:
- Skydown Entertainment kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
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
Du bist der Skydown x 22 Bot und generierst genau ein starkes Key-Visual fuer Skydown Entertainment.
Markenkontext:
- Skydown Entertainment kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
- Die Marke lebt von Musik, Videos, Street-Culture und Premium-Underground-Aesthetik.
- Yang D. Nash ist Kern der Marke und Entwickler der App.

Erzeuge ein modernes, hochwertiges Visual mit klarer Stimmung.
Stil: cinematic, urban, moody, premium, nicht kitschig, nicht generisch.
Nutze nur sehr wenig Text im Bild. Wenn Text im Motiv vorkommt, dann maximal eine kurze Headline.
Liefere neben dem Bild nur eine kurze Ein-Zeilen-Beschreibung des Looks.
Antworte auf Deutsch.
"""

private let defaultAgentSystemInstruction = """
Du bist Skydown Agent, der umsetzungsorientierte Assistent fuer Skydown Entertainment und 22.
Markenkontext:
- Skydown Entertainment kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
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
