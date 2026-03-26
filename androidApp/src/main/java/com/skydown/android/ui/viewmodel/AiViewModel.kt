package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.google.firebase.ai.type.FinishReason
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.ResponseStoppedException
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

    fun sendDraft() {
        sendPrompt(_uiState.value.draft)
    }

    fun sendPrompt(prompt: String) {
        val trimmedPrompt = prompt.trim()
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

    fun resetConversation() {
        chat = aiChatClient.createChat()
        _uiState.update { currentState ->
            AiUiState(
                draft = currentState.draft,
                isAiEnabled = currentState.isAiEnabled,
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
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == messageId) {
                        message.copy(
                            text = text,
                            isStreaming = isStreaming,
                        )
                    } else {
                        message
                    }
                },
            )
        }
    }

    // Keep the assistant grounded in the app and in concise creator-focused output.
    private fun buildPrompt(userPrompt: String): String = """
        Du bist der Skydown x 22 Bot, der kreative Assistent fuer die Skydown x 22 App.
        Antworte auf Deutsch, direkt, modern und hilfreich.
        Wenn sinnvoll, liefere kompakte Listen, Hooks, Captions oder kurze Konzepte.
        Bleib markentauglich und kreativ statt generisch.

        Nutzeranfrage:
        $userPrompt
    """.trimIndent()

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
