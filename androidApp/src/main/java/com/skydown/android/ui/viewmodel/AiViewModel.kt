package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.R
import com.google.firebase.functions.FirebaseFunctionsException
import com.skydown.android.data.AiConversationHistorySource
import com.skydown.android.data.AiConversationHistoryStore
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.data.AppTextResolver
import com.skydown.android.data.AiVisualReferenceLibraryPreferences
import com.skydown.shared.model.User
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.resolvedAiHistoryRetentionDays
import com.skydown.android.ui.model.BotInteractionPhase
import com.skydown.android.ui.model.AiComposerMode
import com.skydown.android.ui.model.AiExperienceLevel
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.resolvedQuotaPlan

class AiViewModel : ViewModel() {
    private val aiChatClient = AppContainer.aiChatClient
    private val aiImageClient = AppContainer.aiImageClient
    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()
    private var currentUserKey: String? = null
    private var currentQuotaPlan: UserQuotaPlan = UserQuotaPlan.Free
    private var conversationRevision = 0
    private var activeRequestJob: Job? = null
    private var activeRequestContext: InFlightRequestContext? = null

    private data class InFlightRequestContext(
        val requestId: String,
        val conversationRevision: Int,
        val userKeyAtSend: String?,
        val aiLevelAtSend: AiExperienceLevel,
    )

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
                val quotaPlan = user?.resolvedQuotaPlan ?: UserQuotaPlan.Free
                currentQuotaPlan = quotaPlan
                val planLabel = when (quotaPlan) {
                    UserQuotaPlan.Studio -> "Creator"
                    UserQuotaPlan.Creator -> "Pro"
                    UserQuotaPlan.InternalTeam -> "Team"
                    UserQuotaPlan.OwnerUnlimited -> "Owner"
                    UserQuotaPlan.Free -> "Free"
                }
                _uiState.update {
                    it.copy(
                        planLabel = planLabel,
                        selectedLevel = if (it.selectedLevel.isAvailableFor(quotaPlan)) {
                            it.selectedLevel
                        } else {
                            AiExperienceLevel.Standard
                        },
                    )
                }
                if (userKey != currentUserKey) {
                    invalidateConversation(cancelActiveRequest = true)
                    currentUserKey = userKey
                    restoreHistory()
                }
            }
        }
    }

    fun updateDraft(draft: String) {
        _uiState.update { state ->
            state.copy(
                draft = draft,
                botPhase = when {
                    state.botPhase.isBusy -> state.botPhase
                    draft.trim().isBlank() && state.botPhase == BotInteractionPhase.Typing -> BotInteractionPhase.Idle
                    draft.trim().isNotBlank() -> BotInteractionPhase.Typing
                    else -> state.botPhase
                },
            )
        }
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

    fun updateLevel(level: AiExperienceLevel) {
        _uiState.update { state ->
            if (state.botPhase.isBusy || !level.isAvailableFor(currentQuotaPlan)) {
                state
            } else {
                state.copy(selectedLevel = level)
            }
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
        val levelAtSend = _uiState.value.selectedLevel
        if (!levelAtSend.isAvailableFor(currentQuotaPlan)) {
            _uiState.update { it.copy(errorMessage = AppTextResolver.string(R.string.ai_level_unavailable)) }
            return
        }
        _uiState.update {
            it.copy(
                botPhase = BotInteractionPhase.Sending,
                errorMessage = null,
                lastDecision = null,
            )
        }

        val userMessage = AiMessage(
            role = AiMessageRole.User,
            text = trimmedPrompt,
        )
        val assistantMessage = AiMessage(
            role = AiMessageRole.Assistant,
            text = "",
            isStreaming = true,
        )
        val assistantMessageId = assistantMessage.id
        val history = buildHistoryContext()
        val requestContext = makeRequestContext(levelAtSend)

        _uiState.update {
            it.copy(
                draft = "",
                botPhase = BotInteractionPhase.Streaming,
                errorMessage = null,
                messages = it.messages + userMessage + assistantMessage,
            )
        }

        activeRequestContext = requestContext
        activeRequestJob?.cancel()
        activeRequestJob = viewModelScope.launch {
            try {
                val result = aiChatClient.generateText(
                    prompt = buildPrompt(
                        userPrompt = trimmedPrompt,
                        history = history,
                    ),
                    mode = _uiState.value.textMode.rawValue,
                    aiLevel = levelAtSend.rawValue,
                )
                if (!isRequestContextActive(requestContext)) return@launch

                AiConversationHistoryStore.updateRetentionDays(result.historyRetentionDays)
                _uiState.update {
                    it.copy(
                        usageSnapshot = result.usage,
                        lastDecision = result.decision,
                    )
                }
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = result.text,
                    isStreaming = false,
                )
                AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Bot,
                    prompt = trimmedPrompt,
                    response = result.text,
                )
                _uiState.update {
                    it.copy(botPhase = resolveTerminalPhase(result.decision))
                }
            } catch (error: Throwable) {
                if (error is CancellationException || !isRequestContextActive(requestContext)) {
                    clearActiveRequestIfNeeded(requestContext)
                    return@launch
                }

                val assistantText = userFacingErrorMessage(error)
                val decision = decisionFor(error)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = assistantText,
                    isStreaming = false,
                )
                AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Bot,
                    prompt = trimmedPrompt,
                    response = assistantText,
                )
                _uiState.update {
                    it.copy(
                        botPhase = resolveTerminalPhase(decision),
                        errorMessage = userFacingErrorMessage(error),
                        lastDecision = decision,
                    )
                }
            }
            clearActiveRequestIfNeeded(requestContext)
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
        val levelAtSend = _uiState.value.selectedLevel
        if (!levelAtSend.isAvailableFor(currentQuotaPlan)) {
            _uiState.update { it.copy(errorMessage = AppTextResolver.string(R.string.ai_level_unavailable)) }
            return
        }
        _uiState.update {
            it.copy(
                botPhase = BotInteractionPhase.Sending,
                errorMessage = null,
                lastDecision = null,
            )
        }

        val userMessage = AiMessage(
            role = AiMessageRole.User,
            text = trimmedPrompt,
        )
        val assistantMessage = AiMessage(
            role = AiMessageRole.Assistant,
            text = "",
            isStreaming = true,
        )
        val assistantMessageId = assistantMessage.id
        val requestContext = makeRequestContext(levelAtSend)

        _uiState.update {
            it.copy(
                draft = "",
                botPhase = BotInteractionPhase.ToolPending,
                errorMessage = null,
                messages = it.messages + userMessage + assistantMessage,
            )
        }

        activeRequestContext = requestContext
        activeRequestJob?.cancel()
        activeRequestJob = viewModelScope.launch {
            try {
                val result = aiImageClient.generateVisual(
                    prompt = buildVisualPrompt(trimmedPrompt),
                    aiLevel = levelAtSend.rawValue,
                )
                if (!isRequestContextActive(requestContext)) return@launch

                AiConversationHistoryStore.updateRetentionDays(result.historyRetentionDays)
                _uiState.update {
                    it.copy(
                        usageSnapshot = result.usage,
                        lastDecision = result.decision,
                    )
                }
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = result.text,
                    isStreaming = false,
                    imageBytes = result.imageBytes,
                    imageMimeType = result.mimeType,
                )
                AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Bot,
                    prompt = trimmedPrompt,
                    response = result.text,
                )
                _uiState.update {
                    it.copy(botPhase = resolveTerminalPhase(result.decision))
                }
            } catch (error: Throwable) {
                if (error is CancellationException || !isRequestContextActive(requestContext)) {
                    clearActiveRequestIfNeeded(requestContext)
                    return@launch
                }

                val assistantText = userFacingErrorMessage(error)
                val decision = decisionFor(error)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = assistantText,
                    isStreaming = false,
                )
                AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Bot,
                    prompt = trimmedPrompt,
                    response = assistantText,
                )
                _uiState.update {
                    it.copy(
                        botPhase = resolveTerminalPhase(decision),
                        errorMessage = userFacingErrorMessage(error),
                        lastDecision = decision,
                    )
                }
            }
            clearActiveRequestIfNeeded(requestContext)
        }
    }

    fun resetConversation() {
        invalidateConversation(cancelActiveRequest = true)
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
                selectedLevel = currentState.selectedLevel,
                quickPrompts = currentState.quickPrompts,
                planLabel = currentState.planLabel,
                lastDecision = null,
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

    private fun makeRequestContext(levelAtSend: AiExperienceLevel): InFlightRequestContext =
        InFlightRequestContext(
            requestId = java.util.UUID.randomUUID().toString(),
            conversationRevision = conversationRevision,
            userKeyAtSend = currentUserKey,
            aiLevelAtSend = levelAtSend,
        )

    private fun isRequestContextActive(context: InFlightRequestContext): Boolean =
        activeRequestContext == context &&
            context.conversationRevision == conversationRevision &&
            context.userKeyAtSend == currentUserKey &&
            context.aiLevelAtSend == _uiState.value.selectedLevel

    private fun clearActiveRequestIfNeeded(context: InFlightRequestContext) {
        if (activeRequestContext == context) {
            activeRequestContext = null
            activeRequestJob = null
        }
    }

    private fun invalidateConversation(cancelActiveRequest: Boolean) {
        if (cancelActiveRequest) {
            activeRequestJob?.cancel()
            activeRequestJob = null
            activeRequestContext = null
        }
        conversationRevision += 1
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
                AppTextResolver.string(R.string.ai_level_limit_reached)
            FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                AppTextResolver.string(R.string.ai_level_unavailable)
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

    private fun resolveTerminalPhase(decision: com.skydown.android.data.AiBotDecision?): BotInteractionPhase {
        if (decision?.ownerDiagnosticActive == true) {
            return BotInteractionPhase.OwnerDiagnostic
        }
        return when (decision?.state) {
            "faq_answer" -> BotInteractionPhase.FaqAnswer
            "degraded" -> BotInteractionPhase.Degraded
            "blocked" -> BotInteractionPhase.Blocked
            "retryable" -> BotInteractionPhase.Retryable
            else -> BotInteractionPhase.Complete
        }
    }

    private fun decisionFor(error: Throwable): com.skydown.android.data.AiBotDecision {
        val description = userFacingErrorMessage(error)
        val isRetryable = error is FirebaseFunctionsException && when (error.code) {
            FirebaseFunctionsException.Code.UNAVAILABLE,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
            FirebaseFunctionsException.Code.INTERNAL,
            -> true

            else -> false
        }

        return com.skydown.android.data.AiBotDecision(
            state = if (isRetryable) "retryable" else "blocked",
            route = if (_uiState.value.composerMode == AiComposerMode.Visual) "visual" else "assistant",
            topic = "",
            summary = if (isRetryable) "Antwort ist aktuell retrybar." else "Antwort wurde blockiert oder gestoppt.",
            promptVersion = "",
            qualityMode = "",
            faqMode = "",
            ownerMode = "",
            answerLength = "",
            personalityStyle = "",
            loggingLevel = "",
            diagnosticsMode = "",
            ownerDiagnosticActive = false,
            selectedModel = "",
            selectedProvider = "",
            fallbackActivated = false,
            fallbackReason = "",
            responseLimited = false,
            responseLimitReason = "",
            blocked = !isRetryable,
            blockReason = if (isRetryable) "" else description,
            retryable = isRetryable,
            retryReason = if (isRetryable) description else "",
            trace = listOf(description),
        )
    }

    private fun AiExperienceLevel.isAvailableFor(plan: UserQuotaPlan): Boolean = when (this) {
        AiExperienceLevel.Standard,
        AiExperienceLevel.Advanced,
        -> true
        AiExperienceLevel.Pro -> plan != UserQuotaPlan.Free
    }
}
