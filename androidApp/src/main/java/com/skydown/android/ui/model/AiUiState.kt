package com.skydown.android.ui.model

data class AiUiState(
    val messages: List<AiMessage> = listOf(
        AiMessage(
            role = AiMessageRole.Assistant,
            text = "Ich bin Skydown AI. Frag mich nach Captions, Release-Ideen, Merch-Texten oder schnellen Konzepten.",
        ),
    ),
    val draft: String = "",
    val isSending: Boolean = false,
    val isAiEnabled: Boolean = true,
    val errorMessage: String? = null,
    val quickPrompts: List<String> = listOf(
        "Schreib eine Instagram Caption fuer einen neuen Skydown Release.",
        "Gib mir drei Merch-Ideen fuer den naechsten Drop.",
        "Formuliere eine kurze Presseankundigung fuer einen Artist-Launch.",
        "Brainstorme eine Hook fuer einen Song-Teaser auf TikTok.",
    ),
)
