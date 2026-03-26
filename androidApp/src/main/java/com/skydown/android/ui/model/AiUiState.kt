package com.skydown.android.ui.model

data class AiUiState(
    val messages: List<AiMessage> = listOf(
        AiMessage(
            role = AiMessageRole.Assistant,
            text = "Ich bin der Skydown x 22 Bot. Ich bin fuer schnelle Ideen, Captions, Hooks und erste Texte da. Wenn du Struktur, To-dos oder einen Plan brauchst, nimm den Agent.",
        ),
    ),
    val draft: String = "",
    val isSending: Boolean = false,
    val isAiEnabled: Boolean = true,
    val errorMessage: String? = null,
    val quickPrompts: List<String> = listOf(
        "Schreib mir nur eine starke Instagram Caption fuer einen neuen Skydown x 22 Release.",
        "Gib mir drei schnelle Hook-Ideen fuer einen Skydown x 22 Song-Teaser.",
        "Formuliere einen kurzen Merch-Claim fuer den naechsten Skydown x 22 Drop.",
        "Brainstorme drei knappe Kampagnenideen ohne langen Plan.",
    ),
)
