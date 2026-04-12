package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class AiPromptSettings(
    val textInstruction: String = DEFAULT_TEXT_INSTRUCTION,
    val visualInstruction: String = DEFAULT_VISUAL_INSTRUCTION,
    val agentSystemInstruction: String = DEFAULT_AGENT_SYSTEM_INSTRUCTION,
    val assetLibraryLink: String = "",
    val assetReferenceNotes: String = "",
)

class AiPromptSettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "adminConfig"
    private val documentName = "aiPromptSettings"

    fun observeSettings(onChange: (Result<AiPromptSettings>) -> Unit): ListenerRegistration {
        return firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onChange(Result.failure(error))
                return@addSnapshotListener
            }

            onChange(Result.success(snapshot?.data.orEmpty().toAiPromptSettings()))
        }
    }

    suspend fun updateSettings(settings: AiPromptSettings): Result<Unit> {
        return runCatching {
            firestore.collection(collectionName).document(documentName).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
        }
    }
}

private fun Map<String, Any>.toAiPromptSettings(): AiPromptSettings {
    return AiPromptSettings(
        textInstruction = normalizeAiPromptInstruction(
            value = this["textInstruction"] as? String,
            fallback = DEFAULT_TEXT_INSTRUCTION,
        ),
        visualInstruction = normalizeAiPromptInstruction(
            value = this["visualInstruction"] as? String,
            fallback = DEFAULT_VISUAL_INSTRUCTION,
        ),
        agentSystemInstruction = normalizeAiPromptInstruction(
            value = this["agentSystemInstruction"] as? String,
            fallback = DEFAULT_AGENT_SYSTEM_INSTRUCTION,
        ),
        assetLibraryLink = normalizePromptLink(this["assetLibraryLink"] as? String).orEmpty(),
        assetReferenceNotes = normalizeAiPromptInstruction(
            value = this["assetReferenceNotes"] as? String,
            fallback = "",
        ),
    )
}

private fun AiPromptSettings.toMap(): Map<String, Any> {
    return mapOf(
        "textInstruction" to normalizeAiPromptInstruction(
            value = textInstruction,
            fallback = DEFAULT_TEXT_INSTRUCTION,
        ),
        "visualInstruction" to normalizeAiPromptInstruction(
            value = visualInstruction,
            fallback = DEFAULT_VISUAL_INSTRUCTION,
        ),
        "agentSystemInstruction" to normalizeAiPromptInstruction(
            value = agentSystemInstruction,
            fallback = DEFAULT_AGENT_SYSTEM_INSTRUCTION,
        ),
        "assetLibraryLink" to normalizePromptLink(assetLibraryLink).orEmpty(),
        "assetReferenceNotes" to normalizeAiPromptInstruction(
            value = assetReferenceNotes,
            fallback = "",
        ),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

private fun normalizeAiPromptInstruction(value: String?, fallback: String): String {
    val normalized = value?.trim().orEmpty()
    return if (normalized.isBlank()) {
        fallback
    } else {
        normalized.take(MAX_PROMPT_LENGTH)
    }
}

private fun normalizePromptLink(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return null
    }
    val prefixed = if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
    return prefixed.trimEnd('/').take(2000)
}

private const val MAX_PROMPT_LENGTH = 12000

private val DEFAULT_TEXT_INSTRUCTION = """
    Du bist der 22xSky Bot, der kreative Copy- und Content-Assistent fuer Skydown Entertainment.
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
""".trimIndent()

private val DEFAULT_VISUAL_INSTRUCTION = """
    Du bist der 22xSky Bot und generierst genau ein starkes Key-Visual fuer Skydown Entertainment.
    Markenkontext:
    - Skydown Entertainment kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
    - Die Marke lebt von Musik, Videos, Street-Culture und Premium-Underground-Aesthetik.
    - Yang D. Nash ist Kern der Marke und Entwickler der App.

    Erzeuge ein modernes, hochwertiges Visual mit klarer Stimmung.
    Stil: cinematic, urban, moody, premium, nicht kitschig, nicht generisch.
    Nutze nur sehr wenig Text im Bild. Wenn Text im Motiv vorkommt, dann maximal eine kurze Headline.
    Liefere neben dem Bild nur eine kurze Ein-Zeilen-Beschreibung des Looks.
    Antworte auf Deutsch.
""".trimIndent()

private val DEFAULT_AGENT_SYSTEM_INSTRUCTION = """
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
""".trimIndent()
