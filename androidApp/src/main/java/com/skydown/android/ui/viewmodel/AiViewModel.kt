package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctionsException
import com.skydown.android.data.AiConversationHistorySource
import com.skydown.android.data.AiConversationHistoryStore
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.data.AiVisualReferenceLibraryPreferences
import com.skydown.shared.model.User
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.resolvedAiHistoryRetentionDays
import com.skydown.android.ui.model.BotInteractionPhase
import com.skydown.android.ui.model.AiComposerMode
import com.skydown.android.ui.model.AiMessage
import com.skydown.android.ui.model.AiMessageRole
import com.skydown.android.ui.model.AiTextMode
import com.skydown.android.ui.model.AiUiState
import com.skydown.android.ui.model.aiQuickPromptsFor
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
    private var currentUserKey: String? = null

    init {
        viewModelScope.launch {
            AppContainer.aiEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isAiEnabled = isEnabled) }
            }
        }

        viewModelScope.launch {
            AppContainer.currentUser.collect { user ->
                AiConversationHistoryStore.updateRetentionDays(
                    user?.resolvedAiHistoryRetentionDays ?: UserRole.User.defaultAiHistoryRetentionDays,
                )
                val userKey = normalizeUserKey(user)
                val planLabel = when (user?.quotaPlan?.lowercase()) {
                    "studio" -> "Creator"
                    "creator" -> "Pro"
                    else -> "Free"
                }
                _uiState.update { it.copy(planLabel = planLabel) }
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

    fun updateComposerMode(mode: AiComposerMode) {
        _uiState.update { it.copy(composerMode = mode) }
    }

    fun updateTextMode(mode: AiTextMode) {
        _uiState.update {
            it.copy(
                textMode = mode,
                quickPrompts = aiQuickPromptsFor(mode),
            )
        }
    }

    fun sendDraft() {
        when (_uiState.value.composerMode) {
            AiComposerMode.Text -> sendPrompt(_uiState.value.draft)
            AiComposerMode.Visual -> generateVisual(_uiState.value.draft)
        }
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
        if (!_uiState.value.isAiEnabled) {
            _uiState.update {
                it.copy(errorMessage = "Der SkyOS Bot ist gerade pausiert.")
            }
            return
        }
        if (trimmedPrompt.isBlank() || _uiState.value.botPhase.isBusy) return
        _uiState.update { it.copy(botPhase = BotInteractionPhase.GeneratingText, errorMessage = null) }

        viewModelScope.launch {
            val assistantMessage = AiMessage(
                role = AiMessageRole.Assistant,
                text = "",
                isStreaming = true,
            )
            val assistantMessageId = assistantMessage.id

            runCatching {
                val userMessage = AiMessage(
                    role = AiMessageRole.User,
                    text = trimmedPrompt,
                )
                val history = buildHistoryContext()

                _uiState.update {
                    it.copy(
                        draft = "",
                        botPhase = BotInteractionPhase.GeneratingText,
                        errorMessage = null,
                        messages = it.messages + userMessage + assistantMessage,
                    )
                }

                aiChatClient.generateText(
                    prompt = buildPrompt(
                        userPrompt = trimmedPrompt,
                        history = history,
                    ),
                    mode = _uiState.value.textMode.rawValue,
                )
            }.onSuccess { result ->
                AiConversationHistoryStore.updateRetentionDays(result.historyRetentionDays)
                _uiState.update { it.copy(usageSnapshot = result.usage) }
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = result.text,
                    isStreaming = false,
                )
                AiConversationHistoryStore.saveEntry(
                    userKey = currentUserKey,
                    source = AiConversationHistorySource.Bot,
                    prompt = trimmedPrompt,
                    response = result.text,
                )
                _uiState.update { it.copy(botPhase = BotInteractionPhase.Idle) }
            }.onFailure { error ->
                val assistantText = userFacingErrorMessage(error)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = assistantText,
                    isStreaming = false,
                )
                AiConversationHistoryStore.saveEntry(
                    userKey = currentUserKey,
                    source = AiConversationHistorySource.Bot,
                    prompt = trimmedPrompt,
                    response = assistantText,
                )
                _uiState.update {
                    it.copy(
                        botPhase = BotInteractionPhase.Idle,
                        errorMessage = userFacingErrorMessage(error),
                    )
                }
            }
        }
    }

    fun generateVisual(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (!AppFeatureFlagsStore.allowsAiAccess(AppContainer.currentUser.value)) {
            _uiState.update {
                it.copy(
                    errorMessage = AppFeatureFlagsStore.accessDeniedMessage(AppContainer.currentUser.value),
                )
            }
            return
        }
        if (!_uiState.value.isAiEnabled) {
            _uiState.update {
                it.copy(errorMessage = "Der SkyOS Bot ist gerade pausiert.")
            }
            return
        }
        if (trimmedPrompt.isBlank() || _uiState.value.botPhase.isBusy) return
        _uiState.update { it.copy(botPhase = BotInteractionPhase.GeneratingVisual, errorMessage = null) }

        viewModelScope.launch {
            val assistantMessage = AiMessage(
                role = AiMessageRole.Assistant,
                text = "",
                isStreaming = true,
            )
            val assistantMessageId = assistantMessage.id
            runCatching {
                val userMessage = AiMessage(
                    role = AiMessageRole.User,
                    text = trimmedPrompt,
                )

                _uiState.update {
                    it.copy(
                        draft = "",
                        botPhase = BotInteractionPhase.GeneratingVisual,
                        errorMessage = null,
                        messages = it.messages + userMessage + assistantMessage,
                    )
                }

                aiImageClient.generateVisual(buildVisualPrompt(trimmedPrompt))
            }.onSuccess { result ->
                AiConversationHistoryStore.updateRetentionDays(result.historyRetentionDays)
                _uiState.update { it.copy(usageSnapshot = result.usage) }
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = result.text,
                    isStreaming = false,
                    imageBytes = result.imageBytes,
                    imageMimeType = result.mimeType,
                )
                AiConversationHistoryStore.saveEntry(
                    userKey = currentUserKey,
                    source = AiConversationHistorySource.Bot,
                    prompt = trimmedPrompt,
                    response = result.text,
                )
                _uiState.update { it.copy(botPhase = BotInteractionPhase.Idle) }
            }.onFailure { error ->
                val assistantText = userFacingErrorMessage(error)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = assistantText,
                    isStreaming = false,
                )
                AiConversationHistoryStore.saveEntry(
                    userKey = currentUserKey,
                    source = AiConversationHistorySource.Bot,
                    prompt = trimmedPrompt,
                    response = assistantText,
                )
                _uiState.update {
                    it.copy(
                        botPhase = BotInteractionPhase.Idle,
                        errorMessage = userFacingErrorMessage(error),
                    )
                }
            }
        }
    }

    fun resetConversation() {
        AiConversationHistoryStore.clearEntries(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Bot,
        )
        _uiState.update { currentState ->
            AiUiState(
                draft = currentState.draft,
                isAiEnabled = currentState.isAiEnabled,
                composerMode = currentState.composerMode,
                textMode = currentState.textMode,
                quickPrompts = currentState.quickPrompts,
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

    private fun buildPrompt(userPrompt: String, history: String): String {
        return """
            Bisheriger Verlauf:
            $history

            Nutzeranfrage:
            $userPrompt
        """.trimIndent()
    }

    private fun restoreHistory() {
        val restoredMessages = AiConversationHistoryStore.entriesFor(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Bot,
        ).asReversed().flatMap { entry ->
            listOf(
                AiMessage(
                    role = AiMessageRole.User,
                    text = entry.prompt,
                ),
                AiMessage(
                    role = AiMessageRole.Assistant,
                    text = entry.response,
                ),
            )
        }

        _uiState.update { currentState ->
            currentState.copy(messages = restoredMessages)
        }
    }

    private fun buildHistoryContext(): String {
        val relevantMessages = _uiState.value.messages.takeLast(12)
        if (relevantMessages.isEmpty()) {
            return "Noch kein Verlauf."
        }

        return relevantMessages.joinToString(separator = "\n") { message ->
            val prefix = if (message.role == AiMessageRole.User) "Nutzer" else "Bot"
            "$prefix: ${message.text}"
        }
    }

    private fun normalizeUserKey(user: User?): String? {
        return user?.id?.takeIf { it.isNotBlank() }
            ?: user?.email?.takeIf { it.isNotBlank() }
    }

    private fun buildVisualPrompt(userPrompt: String): String {
        val referenceContext = AiVisualReferenceLibraryPreferences.promptContext()?.trim()
        return if (referenceContext.isNullOrBlank()) {
            userPrompt
        } else {
            """
                $referenceContext

                $userPrompt
            """.trimIndent()
        }
    }

    private fun userFacingErrorMessage(error: Throwable): String = when (error) {
        is FirebaseFunctionsException -> when (error.code) {
            FirebaseFunctionsException.Code.NOT_FOUND,
            FirebaseFunctionsException.Code.UNIMPLEMENTED,
            -> "Dieser Bot-Bereich wird gerade vorbereitet."
            FirebaseFunctionsException.Code.UNAVAILABLE ->
                "Der SkyOS Bot ist gerade kurz nicht erreichbar."
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                "Die Antwort dauert gerade laenger als gewohnt."
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Dein heutiges KI-Limit ist erreicht."
            FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Die KI ist fuer dein Konto gerade nicht freigeschaltet."
            FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                "Bitte melde dich erneut an und versuch es noch einmal."
            FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                if (error.localizedMessage?.contains("App Check", ignoreCase = true) == true) {
                    "Der Sicherheitscheck laeuft noch. Bitte kurz erneut versuchen."
                } else {
                    error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Die KI ist noch nicht voll bereit."
                }
            FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                "Die Anfrage braucht noch etwas mehr Kontext."
            FirebaseFunctionsException.Code.INTERNAL ->
                if (error.localizedMessage?.contains("server responded with an error", ignoreCase = true) == true) {
                    "Der Visual-Server antwortet gerade unruhig. Bitte kurz erneut versuchen."
                } else {
                    error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Der Visual-Server antwortet gerade unruhig. Bitte kurz erneut versuchen."
                }
            else ->
                "Der SkyOS Bot ist gerade kurz pausiert."
        }
        else -> {
            val message = error.message?.takeIf { it.isNotBlank() }
            if (message?.contains("server responded with an error", ignoreCase = true) == true) {
                "Der Visual-Server antwortet gerade unruhig. Bitte kurz erneut versuchen."
            } else {
                message ?: "Der SkyOS Bot ist gerade kurz pausiert."
            }
        }
    }
}
