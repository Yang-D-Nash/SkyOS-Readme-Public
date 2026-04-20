package com.skydown.android.ui.model

enum class AiComposerMode {
    Text,
    Visual,
}

enum class AiTextMode(val rawValue: String, val title: String, val placeholder: String) {
    General("general", "Allgemein", "Zum Beispiel: Copy fuer den naechsten Drop."),
    Caption("caption", "Captions", "Zum Beispiel: Caption fuer den neuen Track."),
    ReleasePlan("release_plan", "Release", "Zum Beispiel: 7-Tage-Plan fuer den Release."),
    Briefing("briefing", "Briefing", "Zum Beispiel: Briefing fuer Foto- oder Video-Team."),
    MerchCopy("merch_copy", "Merch", "Zum Beispiel: Copy fuer einen neuen Merch-Drop."),
    VideoConcept("video_concept", "Video", "Zum Beispiel: Konzept fuer Reel oder Musikvideo."),
}

data class AiVisualPrompt(
    val label: String,
    val prompt: String,
)

data class AiUiState(
    val messages: List<AiMessage> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val isAiEnabled: Boolean = true,
    val errorMessage: String? = null,
    val composerMode: AiComposerMode = AiComposerMode.Text,
    val textMode: AiTextMode = AiTextMode.General,
    val quickPrompts: List<String> = aiQuickPromptsFor(AiTextMode.General),
    val visualPrompts: List<AiVisualPrompt> = listOf(
        AiVisualPrompt(
            label = "Artist Foto",
            prompt = "Generiere ein cineastisches Artist-Foto fuer SkyOs wie ein hochwertiger ARRI-Frame: 35mm Prime, offene Blende um f/1.4, organisches Bokeh, natuerliche Tiefenstaffelung, moody Licht, realistisches Editorial-Foto, keine Illustration, kein CGI und kein generischer AI-Look.",
        ),
        AiVisualPrompt(
            label = "Cover Art",
            prompt = "Generiere ein quadratisches Cover-Art fuer einen dunklen Hip-Hop-Release von SkyOs mit cineastischer Nachtstimmung und starkem Fokus auf Mood statt Schrift.",
        ),
        AiVisualPrompt(
            label = "Release Poster",
            prompt = "Generiere ein vertikales Release-Poster fuer SkyOs, urban, premium, moody, mit Platz fuer einen kuenftigen Tracktitel.",
        ),
        AiVisualPrompt(
            label = "Story Visual",
            prompt = "Generiere ein starkes 9:16 Story-Visual fuer einen neuen SkyOs Drop, street, cinematic, klarer Fokus und wenig Text im Bild.",
        ),
    ),
)

fun aiQuickPromptsFor(mode: AiTextMode): List<String> = when (mode) {
    AiTextMode.General -> listOf(
        "Schreib 3 starke Copy-Ideen fuer einen neuen Skydown-Post.",
        "Gib mir eine markentaugliche Ansage fuer einen Story-Slide.",
        "Mach mir eine klare Promo-Line fuer einen neuen Drop.",
        "Ueberarbeite diesen Text moderner und druckvoller.",
    )
    AiTextMode.Caption -> listOf(
        "Schreib 3 Instagram Captions fuer einen neuen Track mit CTA und 5 Hashtags.",
        "Mach mir 4 Story-Captions fuer einen dunklen Teaser mit kurzer Hook.",
        "Schreib eine Caption fuer ein Studio-Snippet mit Hamburg-Vibe.",
        "Formuliere einen Release-Post, kurz, druckvoll und nicht generisch.",
    )
    AiTextMode.ReleasePlan -> listOf(
        "Baue mir einen 7-Tage-Release-Plan fuer einen neuen Track mit Assets und Deadlines.",
        "Strukturiere den Launch fuer Freitag von Teaser bis Post-Release.",
        "Mach einen Mini-Plan fuer Song, Story, Reel und CTA.",
        "Welche Assets brauche ich fuer einen sauberen Track-Release?",
    )
    AiTextMode.Briefing -> listOf(
        "Schreib ein Briefing fuer einen Fotografen mit Mood, Shots und Deliverables.",
        "Mach ein Briefing fuer einen Cover-Designer, urban und cinematic.",
        "Erstelle ein kompaktes Creator-Briefing fuer einen Collab-Post.",
        "Formuliere ein Team-Briefing fuer einen Promo-Dreh.",
    )
    AiTextMode.MerchCopy -> listOf(
        "Formuliere einen Merch-Drop-Post mit Headline, Caption und Story-CTA.",
        "Schreib Copy fuer ein limitiertes Hoodie-Release.",
        "Gib mir 3 Shop-Claims fuer einen Premium-Streetwear-Drop.",
        "Mach eine kurze Produktbeschreibung fuer Shirt und Hoodie.",
    )
    AiTextMode.VideoConcept -> listOf(
        "Gib mir 5 kurze Hook-Ideen fuer einen dunklen Song-Teaser.",
        "Mach mir ein 15-Sekunden Reel-Skript mit Hook, Shots und On-Screen-Text.",
        "Baue ein Storyboard fuer ein moody Performance-Visual.",
        "Mach ein Konzept fuer ein vertikales Promo-Video mit 3 Szenen.",
    )
}
