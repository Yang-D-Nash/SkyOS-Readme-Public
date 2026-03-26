package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.google.firebase.ai.type.FinishReason
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.ResponseStoppedException
import com.skydown.android.ui.model.AiComposerMode
import com.skydown.android.ui.model.AiMessage
import com.skydown.android.ui.model.AiMessageRole
import com.skydown.android.ui.model.AiUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiViewModel : ViewModel() {
    private val aiChatClient = AppContainer.aiChatClient
    private val aiImageClient = AppContainer.aiImageClient
    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private var chat = aiChatClient.createChat()

    init {
        viewModelScope.launch {
            AppContainer.aiEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isAiEnabled = isEnabled) }
            }
        }
    }

    fun updateDraft(draft: String) {
        _uiState.update { it.copy(draft = draft) }
    }

    fun updateComposerMode(mode: AiComposerMode) {
        _uiState.update { it.copy(composerMode = mode) }
    }

    fun sendDraft() {
        when (_uiState.value.composerMode) {
            AiComposerMode.Text -> sendPrompt(_uiState.value.draft)
            AiComposerMode.Visual -> generateVisual(_uiState.value.draft)
        }
    }

    fun sendPrompt(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (AppContainer.currentUser.value == null) {
            _uiState.update {
                it.copy(errorMessage = "Bitte melde dich an, um den Skydown x 22 Bot zu nutzen.")
            }
            return
        }
        if (!_uiState.value.isAiEnabled) {
            _uiState.update {
                it.copy(errorMessage = "Der Skydown x 22 Bot ist gerade deaktiviert.")
            }
            return
        }
        if (trimmedPrompt.isBlank() || _uiState.value.isSending) return

        val userMessage = AiMessage(
            role = AiMessageRole.User,
            text = trimmedPrompt,
        )
        val assistantMessage = AiMessage(
            role = AiMessageRole.Assistant,
            text = "",
            isStreaming = true,
        )

        _uiState.update {
            it.copy(
                draft = "",
                isSending = true,
                errorMessage = null,
                messages = it.messages + userMessage + assistantMessage,
            )
        }

        viewModelScope.launch {
            val responseBuffer = StringBuilder()

            runCatching {
                chat.sendMessageStream(buildPrompt(trimmedPrompt)).collect { response ->
                    val chunk = response.text.orEmpty()
                    if (chunk.isNotBlank()) {
                        responseBuffer.append(chunk)
                        updateAssistantMessage(
                            messageId = assistantMessage.id,
                            text = responseBuffer.toString(),
                            isStreaming = true,
                        )
                    }
                }
            }.onSuccess {
                val finalText = responseBuffer.toString().ifBlank {
                    "Ich habe gerade keine Antwort erhalten. Versuch es bitte noch einmal."
                }
                updateAssistantMessage(
                    messageId = assistantMessage.id,
                    text = finalText,
                    isStreaming = false,
                )
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { error ->
                val partialText = responseBuffer.toString().trim()
                val assistantText = assistantMessageText(
                    error = error,
                    partialText = partialText,
                )
                updateAssistantMessage(
                    messageId = assistantMessage.id,
                    text = assistantText,
                    isStreaming = false,
                )
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = userFacingErrorMessage(error),
                    )
                }
            }
        }
    }

    fun generateVisual(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (AppContainer.currentUser.value == null) {
            _uiState.update {
                it.copy(errorMessage = "Bitte melde dich an, um Visuals mit dem Skydown x 22 Bot zu generieren.")
            }
            return
        }
        if (!_uiState.value.isAiEnabled) {
            _uiState.update {
                it.copy(errorMessage = "Der Skydown x 22 Bot ist gerade deaktiviert.")
            }
            return
        }
        if (trimmedPrompt.isBlank() || _uiState.value.isSending) return

        val userMessage = AiMessage(
            role = AiMessageRole.User,
            text = trimmedPrompt,
        )
        val assistantMessage = AiMessage(
            role = AiMessageRole.Assistant,
            text = "",
            isStreaming = true,
        )

        _uiState.update {
            it.copy(
                draft = "",
                isSending = true,
                errorMessage = null,
                messages = it.messages + userMessage + assistantMessage,
            )
        }

        viewModelScope.launch {
            runCatching {
                aiImageClient.generateVisual(buildVisualPrompt(trimmedPrompt))
            }.onSuccess { result ->
                updateAssistantMessage(
                    messageId = assistantMessage.id,
                    text = result.text,
                    isStreaming = false,
                    imageBytes = result.imageBytes,
                    imageMimeType = result.mimeType,
                )
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { error ->
                updateAssistantMessage(
                    messageId = assistantMessage.id,
                    text = assistantMessageText(
                        error = error,
                        partialText = "",
                    ),
                    isStreaming = false,
                )
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = userFacingErrorMessage(error),
                    )
                }
            }
        }
    }

    fun resetConversation() {
        chat = aiChatClient.createChat()
        _uiState.update { currentState ->
            AiUiState(
                draft = currentState.draft,
                isAiEnabled = currentState.isAiEnabled,
                composerMode = currentState.composerMode,
            )
        }
    }

    fun refreshAvailability() {
        viewModelScope.launch {
            AppFeatureFlagsStore.refresh()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun updateAssistantMessage(
        messageId: String,
        text: String,
        isStreaming: Boolean,
        imageBytes: ByteArray? = null,
        imageMimeType: String? = null,
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == messageId) {
                        message.copy(
                            text = text,
                            isStreaming = isStreaming,
                            imageBytes = imageBytes ?: message.imageBytes,
                            imageMimeType = imageMimeType ?: message.imageMimeType,
                        )
                    } else {
                        message
                    }
                },
            )
        }
    }

    // Keep the assistant grounded in the brand and force directly usable outputs.
    private fun buildPrompt(userPrompt: String): String {
        val formatHint = when {
            userPrompt.containsAnyKeyword("caption", "captions", "instagram", "post", "story", "claim", "headline") ->
                "Liefere zuerst die beste Version, danach 3 weitere Varianten und am Ende optional 5 passende Hashtags."
            userPrompt.containsAnyKeyword("hook", "hooks", "teaser", "intro") ->
                "Liefere 5 kurze Hook-Optionen mit maximal 10 Woertern pro Option."
            userPrompt.containsAnyKeyword("reel", "tiktok", "skript", "script", "video") ->
                "Liefere die Antwort als Hook, Ablauf in 3 bis 5 Beats, On-Screen-Text und Caption."
            userPrompt.containsAnyKeyword("merch", "drop") ->
                "Liefere die Antwort als Headline, Hauptcaption, Story-CTA und 3 kurze Zusatzvarianten."
            else ->
                "Liefere eine direkt nutzbare Hauptantwort und wenn passend 3 starke Varianten."
        }

        return """
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

            Ausgabeformat:
            $formatHint

            Nutzeranfrage:
            $userPrompt
        """.trimIndent()
    }

    private fun buildVisualPrompt(userPrompt: String): String {
        return """
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

            Nutzeranfrage:
            $userPrompt
        """.trimIndent()
    }

    private fun String.containsAnyKeyword(vararg keywords: String): Boolean {
        val lower = lowercase()
        return keywords.any { keyword -> lower.contains(keyword) }
    }

    private fun assistantMessageText(
        error: Throwable,
        partialText: String,
    ): String {
        if (partialText.isNotBlank()) {
            return buildString {
                append(partialText)
                append("\n\n")
                append(userFacingErrorMessage(error))
            }
        }

        return when (error) {
            is PromptBlockedException -> {
                error.response?.promptFeedback?.blockReasonMessage
                    ?.takeIf { it.isNotBlank() }
                    ?: "Die Anfrage konnte so nicht verarbeitet werden. Versuch es etwas neutraler oder konkreter."
            }
            else -> userFacingErrorMessage(error)
        }
    }

    private fun userFacingErrorMessage(error: Throwable): String = when (error) {
        is ResponseStoppedException -> finishReasonMessage(
            error.response?.candidates?.firstOrNull()?.finishReason,
        )
        is PromptBlockedException -> {
            error.response?.promptFeedback?.blockReasonMessage
                ?.takeIf { it.isNotBlank() }
                ?: "Die Anfrage konnte so nicht verarbeitet werden."
        }
        else -> "Der Skydown x 22 Bot ist gerade nicht verfuegbar."
    }

    private fun finishReasonMessage(reason: FinishReason?): String = when (reason) {
        FinishReason.MAX_TOKENS -> "Die Antwort wurde wegen des Antwortlimits gekuerzt."
        FinishReason.SAFETY,
        FinishReason.PROHIBITED_CONTENT,
        FinishReason.BLOCKLIST,
        FinishReason.SPII
        -> "Die Antwort wurde aus Sicherheitsgruenden gestoppt."
        FinishReason.RECITATION -> "Die Antwort wurde wegen Zitat-Schutz gestoppt."
        FinishReason.MALFORMED_FUNCTION_CALL -> "Die Antwort konnte nicht sauber abgeschlossen werden. Versuch es bitte noch einmal."
        else -> "Die Antwort wurde vorzeitig beendet."
    }
}
