package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctionsException
import com.skydown.android.data.AgentHistoryTurn
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.ui.model.AgentMessage
import com.skydown.android.ui.model.AgentMessageRole
import com.skydown.android.ui.model.AgentUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgentViewModel : ViewModel() {
    private val agentClient = AppContainer.agentClient
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.aiEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isAgentEnabled = isEnabled) }
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
        if (!AppFeatureFlagsStore.allowsAiAccess(AppContainer.currentUser.value)) {
            _uiState.update {
                it.copy(
                    errorMessage = AppFeatureFlagsStore.accessDeniedMessage(AppContainer.currentUser.value),
                )
            }
            return
        }
        if (!_uiState.value.isAgentEnabled) {
            _uiState.update {
                it.copy(errorMessage = "Der Skydown x 22 Agent ist gerade deaktiviert.")
            }
            return
        }
        if (trimmedPrompt.isBlank() || _uiState.value.isSending) return

        val userMessage = AgentMessage(
            role = AgentMessageRole.User,
            text = trimmedPrompt,
        )
        val assistantMessage = AgentMessage(
            role = AgentMessageRole.Assistant,
            text = "",
            isStreaming = true,
        )
        val history = buildHistory(_uiState.value.messages + userMessage)

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
                agentClient.sendMessage(
                    prompt = trimmedPrompt,
                    history = history,
                )
            }.onSuccess { reply ->
                updateAssistantMessage(
                    messageId = assistantMessage.id,
                    text = reply,
                    isStreaming = false,
                )
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { error ->
                updateAssistantMessage(
                    messageId = assistantMessage.id,
                    text = userFacingErrorMessage(error),
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
        _uiState.update { currentState ->
            AgentUiState(
                draft = currentState.draft,
                isAgentEnabled = currentState.isAgentEnabled,
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

    private fun buildHistory(messages: List<AgentMessage>): List<AgentHistoryTurn> =
        messages
            .filterNot { message ->
                message.role == AgentMessageRole.Assistant &&
                    message.text == INTRO_MESSAGE
            }
            .map { message ->
                AgentHistoryTurn(
                    role = if (message.role == AgentMessageRole.User) "user" else "assistant",
                    text = message.text,
                )
            }

    private fun userFacingErrorMessage(error: Throwable): String = when (error) {
        is FirebaseFunctionsException -> when (error.code) {
            FirebaseFunctionsException.Code.NOT_FOUND,
            FirebaseFunctionsException.Code.UNIMPLEMENTED,
            -> "Der Skydown x 22 Agent ist fuer diese Funktion gerade noch nicht verfuegbar."
            FirebaseFunctionsException.Code.UNAVAILABLE -> "Der Skydown x 22 Agent ist gerade nicht erreichbar."
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Der Skydown x 22 Agent hat zu lange fuer die Antwort gebraucht."
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> "Der Skydown x 22 Agent ist gerade ausgelastet."
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> "Die Anfrage konnte so nicht verarbeitet werden."
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "Der Skydown x 22 Agent ist aktuell nur fuer Admins freigeschaltet."
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Bitte melde dich erneut an und versuch es noch einmal."
            else -> "Der Skydown x 22 Agent konnte gerade nicht antworten."
        }

        else -> "Der Skydown x 22 Agent konnte gerade nicht antworten."
    }

    private companion object {
        const val INTRO_MESSAGE =
            "Ich bin der Skydown x 22 Agent. Ich baue dir Briefings, Release-Plaene, Content-Strukturen, Checklisten und naechste Schritte. Fuer schnelle Hooks oder Captions nimm den Bot."
    }
}
