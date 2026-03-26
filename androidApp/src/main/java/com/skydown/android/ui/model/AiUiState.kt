package com.skydown.android.ui.model

data class AiUiState(
    val messages: List<AiMessage> = listOf(
        AiMessage(
            role = AiMessageRole.Assistant,
            text = "Ich bin der Skydown x 22 Bot. Frag mich nach Captions, Release-Ideen, Merch-Texten oder kurzen Kampagnenkonzepten.",
        ),
    ),
    val draft: String = "",
    val isSending: Boolean = false,
    val isAiEnabled: Boolean = true,
    val errorMessage: String? = null,
    val quickPrompts: List<String> = listOf(
        "Schreib eine Instagram Caption fuer einen neuen Skydown x 22 Release.",
        "Gib mir drei Merch-Ideen fuer den naechsten Skydown x 22 Drop.",
        "Formuliere eine kurze Presseankundigung fuer einen Skydown x 22 Artist-Launch.",
        "Brainstorme eine Hook fuer einen Skydown x 22 Song-Teaser auf TikTok.",
    ),
)
