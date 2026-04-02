package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctionsException
import com.skydown.android.data.AgentHistoryTurn
import com.skydown.android.data.AiConversationHistorySource
import com.skydown.android.data.AiConversationHistoryStore
import com.skydown.android.data.AiUsageAuthorizationKind
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.shared.model.User
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.resolvedAiHistoryRetentionDays
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
    private val aiUsageAuthorizationClient = AppContainer.aiUsageAuthorizationClient
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()
    private var currentUserKey: String? = null

    init {
        viewModelScope.launch {
            AppContainer.aiEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isAgentEnabled = isEnabled) }
            }
        }

        viewModelScope.launch {
            AppContainer.currentUser.collect { user ->
                AiConversationHistoryStore.updateRetentionDays(
                    user?.resolvedAiHistoryRetentionDays ?: UserRole.User.defaultAiHistoryRetentionDays,
                )
                val userKey = normalizeUserKey(user)
                if (userKey != currentUserKey) {
                    currentUserKey = userKey
                    restoreHistory()
                }
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
        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        viewModelScope.launch {
            var assistantMessageId: String? = null
            runCatching {
                val authorization = aiUsageAuthorizationClient.authorize(AiUsageAuthorizationKind.Agent)
                AiConversationHistoryStore.updateRetentionDays(authorization.historyRetentionDays)

                val userMessage = AgentMessage(
                    role = AgentMessageRole.User,
                    text = trimmedPrompt,
                )
                val assistantMessage = AgentMessage(
                    role = AgentMessageRole.Assistant,
                    text = "",
                    isStreaming = true,
                )
                assistantMessageId = assistantMessage.id
                val history = buildHistory(_uiState.value.messages + userMessage)

                _uiState.update {
                    it.copy(
                        draft = "",
                        isSending = true,
                        errorMessage = null,
                        messages = it.messages + userMessage + assistantMessage,
                    )
                }

                agentClient.sendMessage(
                    prompt = trimmedPrompt,
                    history = history,
                )
            }.onSuccess { reply ->
                assistantMessageId?.let { messageId ->
                    updateAssistantMessage(
                        messageId = messageId,
                        text = reply,
                        isStreaming = false,
                    )
                    AiConversationHistoryStore.saveEntry(
                        userKey = currentUserKey,
                        source = AiConversationHistorySource.Agent,
                        prompt = trimmedPrompt,
                        response = reply,
                    )
                }
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { error ->
                val errorMessage = userFacingErrorMessage(error)
                assistantMessageId?.let { messageId ->
                    updateAssistantMessage(
                        messageId = messageId,
                        text = errorMessage,
                        isStreaming = false,
                    )
                    AiConversationHistoryStore.saveEntry(
                        userKey = currentUserKey,
                        source = AiConversationHistorySource.Agent,
                        prompt = trimmedPrompt,
                        response = errorMessage,
                    )
                }
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = errorMessage,
                    )
                }
            }
        }
    }

    fun resetConversation() {
        AiConversationHistoryStore.clearEntries(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Agent,
        )
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
            .map { message ->
                AgentHistoryTurn(
                    role = if (message.role == AgentMessageRole.User) "user" else "assistant",
                    text = message.text,
                )
            }

    private fun restoreHistory() {
        val restoredMessages = AiConversationHistoryStore.entriesFor(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Agent,
        ).asReversed().flatMap { entry ->
            listOf(
                AgentMessage(
                    role = AgentMessageRole.User,
                    text = entry.prompt,
                ),
                AgentMessage(
                    role = AgentMessageRole.Assistant,
                    text = entry.response,
                ),
            )
        }

        _uiState.update { currentState ->
            currentState.copy(messages = restoredMessages)
        }
    }

    private fun normalizeUserKey(user: User?): String? {
        return user?.id?.takeIf { it.isNotBlank() }
            ?: user?.email?.takeIf { it.isNotBlank() }
    }

    private fun userFacingErrorMessage(error: Throwable): String = when (error) {
        is FirebaseFunctionsException -> when (error.code) {
            FirebaseFunctionsException.Code.NOT_FOUND,
            FirebaseFunctionsException.Code.UNIMPLEMENTED,
            -> "Der Skydown x 22 Agent ist fuer diese Funktion gerade noch nicht verfuegbar."
            FirebaseFunctionsException.Code.UNAVAILABLE -> "Der Skydown x 22 Agent ist gerade nicht erreichbar."
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Der Skydown x 22 Agent hat zu lange fuer die Antwort gebraucht."
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Dein heutiges Agent-Limit ist erreicht."
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> "Die Anfrage konnte so nicht verarbeitet werden."
            FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Der Agent ist fuer dein Konto gerade nicht freigeschaltet."
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Bitte melde dich erneut an und versuch es noch einmal."
            else -> "Der Skydown x 22 Agent konnte gerade nicht antworten."
        }

        else -> "Der Skydown x 22 Agent konnte gerade nicht antworten."
    }
}
