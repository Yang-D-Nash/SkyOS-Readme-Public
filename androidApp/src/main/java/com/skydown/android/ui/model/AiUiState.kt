package com.skydown.android.ui.model

enum class AiComposerMode {
    Text,
    Visual,
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
    val quickPrompts: List<String> = listOf(
        "Schreib 3 Instagram Captions fuer einen neuen Track mit CTA und 5 Hashtags.",
        "Gib mir 5 kurze Hook-Ideen fuer einen dunklen Song-Teaser.",
        "Mach mir ein 15-Sekunden Reel-Skript mit Hook, Shots und On-Screen-Text.",
        "Formuliere einen Merch-Drop-Post mit Headline, Caption und Story-CTA.",
    ),
    val visualPrompts: List<AiVisualPrompt> = listOf(
        AiVisualPrompt(
            label = "Cover Art",
            prompt = "Generiere ein quadratisches Cover-Art fuer einen dunklen Hip-Hop-Release von Skydown x 22 mit cineastischer Nachtstimmung und starkem Fokus auf Mood statt Schrift.",
        ),
        AiVisualPrompt(
            label = "Release Poster",
            prompt = "Generiere ein vertikales Release-Poster fuer Skydown x 22, urban, premium, moody, mit Platz fuer einen kuenftigen Tracktitel.",
        ),
        AiVisualPrompt(
            label = "Story Visual",
            prompt = "Generiere ein starkes 9:16 Story-Visual fuer einen neuen Skydown x 22 Drop, street, cinematic, klarer Fokus und wenig Text im Bild.",
        ),
    ),
)
