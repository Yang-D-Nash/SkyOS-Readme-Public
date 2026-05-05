package com.nash.skyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctionsException
import com.nash.skyos.R
import com.nash.skyos.data.AiConversationHistorySource
import com.nash.skyos.data.AiConversationHistoryStore
import com.nash.skyos.data.AiConversationHistorySaveResult
import com.nash.skyos.data.AiConversationSyncRepository
import com.nash.skyos.data.AiVisualReferenceLibraryPreferences
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.AppFeatureFlagsStore
import com.nash.skyos.data.AppTextResolver
import com.nash.skyos.ui.model.AiComposerMode
import com.nash.skyos.ui.model.AiExperienceLevel
import com.nash.skyos.ui.model.AiMessage
import com.nash.skyos.ui.model.AiMessageRole
import com.nash.skyos.ui.model.AiTextMode
import com.nash.skyos.ui.model.AiUiState
import com.nash.skyos.ui.model.BotInteractionPhase
import com.nash.skyos.ui.model.aiQuickPromptsFor
import com.skydown.shared.model.User
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.resolvedAiHistoryRetentionDays
import com.skydown.shared.model.resolvedQuotaPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiViewModel : ViewModel() {
    private val aiChatClient = AppContainer.aiChatClient
    private val aiImageClient = AppContainer.aiImageClient
    private val syncRepository = AiConversationSyncRepository()
    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var currentUserKey: String? = null
    private var currentQuotaPlan: UserQuotaPlan = UserQuotaPlan.Free
    private var currentHistoryRetentionDays: Int = UserRole.User.defaultAiHistoryRetentionDays
    private var currentSessionId: String? = null
    private var conversationRevision = 0
    private var activeRequestJob: Job? = null
    private var remoteHydrationJob: Job? = null
    private var remoteHistoryListener: ListenerRegistration? = null
    private var activeRequestContext: InFlightRequestContext? = null

    private data class InFlightRequestContext(
        val requestId: String,
        val conversationRevision: Int,
        val userKeyAtSend: String?,
        val sessionIdAtSend: String,
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
                currentHistoryRetentionDays =
                    user?.resolvedAiHistoryRetentionDays ?: UserRole.User.defaultAiHistoryRetentionDays
                AiConversationHistoryStore.updateRetentionDays(currentHistoryRetentionDays)
                val userKey = normalizeUserKey(user)
                val userId = user?.id?.takeIf { it.isNotBlank() }
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
                if (userKey != currentUserKey || userId != currentUserId) {
                    invalidateConversation(cancelActiveRequest = true)
                    remoteHistoryListener?.remove()
                    remoteHistoryListener = null
                    currentUserId = userId
                    currentUserKey = userKey
                    currentSessionId = null
                    restoreHistory()
                    hydrateRemoteHistoryIfNeeded(currentSessionId)
                    startRemoteHistoryObservation()
                    // After app relaunch (new process) or account switch: empty composer; past threads stay in the history sheet.
                    if (!userId.isNullOrBlank()) {
                        startNewConversation()
                    }
                } else {
                    refreshSessionState()
                    pruneRemoteHistoryIfNeeded()
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

    fun sendDraftInNewConversation(): Boolean {
        val prompt = _uiState.value.draft
        val trimmedPrompt = prompt.trim()
        if (!isAiRequestAllowed()) return false
        if (trimmedPrompt.isBlank() || _uiState.value.botPhase.isBusy) return false
        val levelAtSend = _uiState.value.selectedLevel
        if (!levelAtSend.isAvailableFor(currentQuotaPlan)) {
            _uiState.update { it.copy(errorMessage = AppTextResolver.string(R.string.ai_level_unavailable)) }
            return false
        }
        startNewConversation()
        when (_uiState.value.composerMode) {
            AiComposerMode.Text -> sendPrompt(prompt)
            AiComposerMode.Visual -> generateVisual(prompt)
        }
        return true
    }

    fun sendTextFollowUp() {
        sendPrompt(_uiState.value.draft)
    }

    fun canSendTextFollowUp(): Boolean =
        _uiState.value.draft.isNotBlank() &&
            !_uiState.value.botPhase.isBusy &&
            _uiState.value.selectedLevel.isAvailableFor(currentQuotaPlan)

    fun sendPrompt(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (!isAiRequestAllowed()) return
        if (trimmedPrompt.isBlank() || _uiState.value.botPhase.isBusy) return

        val levelAtSend = _uiState.value.selectedLevel
        if (!levelAtSend.isAvailableFor(currentQuotaPlan)) {
            _uiState.update { it.copy(errorMessage = AppTextResolver.string(R.string.ai_level_unavailable)) }
            return
        }

        val activeSessionId = ensureCurrentSessionId()
        val userMessage = AiMessage(role = AiMessageRole.User, text = trimmedPrompt)
        val assistantMessage = AiMessage(role = AiMessageRole.Assistant, text = "", isStreaming = true)
        val assistantMessageId = assistantMessage.id
        val history = buildHistoryContext()
        val requestContext = makeRequestContext(levelAtSend, activeSessionId)

        _uiState.update {
            it.copy(
                draft = "",
                botPhase = BotInteractionPhase.Streaming,
                errorMessage = null,
                lastDecision = null,
                messages = it.messages + userMessage + assistantMessage,
            )
        }

        activeRequestContext = requestContext
        activeRequestJob?.cancel()
        activeRequestJob = viewModelScope.launch {
            try {
                val result = aiChatClient.generateText(
                    prompt = buildPrompt(trimmedPrompt, history),
                    mode = _uiState.value.textMode.rawValue,
                    aiLevel = levelAtSend.rawValue,
                )
                if (!isRequestContextActive(requestContext)) return@launch

                AiConversationHistoryStore.updateRetentionDays(result.historyRetentionDays)
                val savedResult = AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Bot,
                    sessionId = requestContext.sessionIdAtSend,
                    prompt = trimmedPrompt,
                    response = result.text,
                )
                savedResult?.let(::syncSavedEntryToRemote)
                refreshSessionState(requestContext.sessionIdAtSend)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = result.text,
                    isStreaming = false,
                )
                _uiState.update {
                    it.copy(
                        usageSnapshot = result.usage,
                        lastDecision = result.decision,
                        botPhase = resolveTerminalPhase(result.decision),
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException || !isRequestContextActive(requestContext)) {
                    clearActiveRequestIfNeeded(requestContext)
                    return@launch
                }

                val assistantText = userFacingErrorMessage(error)
                val decision = decisionFor(error)
                val savedResult = AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Bot,
                    sessionId = requestContext.sessionIdAtSend,
                    prompt = trimmedPrompt,
                    response = assistantText,
                )
                savedResult?.let(::syncSavedEntryToRemote)
                refreshSessionState(requestContext.sessionIdAtSend)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = assistantText,
                    isStreaming = false,
                )
                _uiState.update {
                    it.copy(
                        botPhase = resolveTerminalPhase(decision),
                        errorMessage = assistantText,
                        lastDecision = decision,
                    )
                }
            }
            clearActiveRequestIfNeeded(requestContext)
        }
    }

    fun generateVisual(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (!isAiRequestAllowed()) return
        if (trimmedPrompt.isBlank() || _uiState.value.botPhase.isBusy) return

        val levelAtSend = _uiState.value.selectedLevel
        if (!levelAtSend.isAvailableFor(currentQuotaPlan)) {
            _uiState.update { it.copy(errorMessage = AppTextResolver.string(R.string.ai_level_unavailable)) }
            return
        }

        val activeSessionId = ensureCurrentSessionId()
        val userMessage = AiMessage(role = AiMessageRole.User, text = trimmedPrompt)
        val assistantMessage = AiMessage(role = AiMessageRole.Assistant, text = "", isStreaming = true)
        val assistantMessageId = assistantMessage.id
        val requestContext = makeRequestContext(levelAtSend, activeSessionId)

        _uiState.update {
            it.copy(
                draft = "",
                botPhase = BotInteractionPhase.ToolPending,
                errorMessage = null,
                lastDecision = null,
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
                val savedResult = AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Bot,
                    sessionId = requestContext.sessionIdAtSend,
                    prompt = trimmedPrompt,
                    response = result.text,
                    imageBytes = result.imageBytes,
                    imageMimeType = result.mimeType,
                )
                savedResult?.let(::syncSavedEntryToRemote)
                refreshSessionState(requestContext.sessionIdAtSend)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = result.text,
                    isStreaming = false,
                    imageBytes = result.imageBytes,
                    imageMimeType = result.mimeType,
                )
                _uiState.update {
                    it.copy(
                        usageSnapshot = result.usage,
                        lastDecision = result.decision,
                        botPhase = resolveTerminalPhase(result.decision),
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException || !isRequestContextActive(requestContext)) {
                    clearActiveRequestIfNeeded(requestContext)
                    return@launch
                }

                val assistantText = userFacingErrorMessage(error)
                val decision = decisionFor(error)
                val savedResult = AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Bot,
                    sessionId = requestContext.sessionIdAtSend,
                    prompt = trimmedPrompt,
                    response = assistantText,
                )
                savedResult?.let(::syncSavedEntryToRemote)
                refreshSessionState(requestContext.sessionIdAtSend)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = assistantText,
                    isStreaming = false,
                )
                _uiState.update {
                    it.copy(
                        botPhase = resolveTerminalPhase(decision),
                        errorMessage = assistantText,
                        lastDecision = decision,
                    )
                }
            }
            clearActiveRequestIfNeeded(requestContext)
        }
    }

    fun resetConversation() {
        startNewConversation()
    }

    fun startNewConversation() {
        invalidateConversation(cancelActiveRequest = true)
        val newSession = AiConversationHistoryStore.createSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Bot,
        )
        currentSessionId = newSession.id
        refreshSessionState(newSession.id)
        syncSessionToRemote(newSession)
        _uiState.update { currentState ->
            currentState.copy(
                messages = emptyList(),
                botPhase = if (currentState.draft.isBlank()) BotInteractionPhase.Idle else BotInteractionPhase.Typing,
                errorMessage = null,
                lastDecision = null,
            )
        }
    }

    fun openConversation(sessionId: String) {
        if (sessionId == currentSessionId) return
        invalidateConversation(cancelActiveRequest = true)
        currentSessionId = sessionId
        restoreHistory(sessionId)
    }

    fun refreshActiveConversation() {
        if (_uiState.value.botPhase.isBusy) return
        invalidateConversation(cancelActiveRequest = true)
        restoreHistory(currentSessionId)
    }

    fun renameActiveConversation(title: String) {
        AiConversationHistoryStore.renameSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Bot,
            sessionId = currentSessionId,
            title = title,
        )?.let(::syncSessionToRemote)
        refreshSessionState()
    }

    fun deleteActiveConversation() {
        val sessionId = currentSessionId ?: return
        invalidateConversation(cancelActiveRequest = true)
        AiConversationHistoryStore.deleteSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Bot,
            sessionId = sessionId,
        )
        deleteSessionFromRemote(sessionId)
        currentSessionId = null
        restoreHistory()
    }

    fun refreshAvailability() {
        viewModelScope.launch {
            AppFeatureFlagsStore.refresh()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        remoteHistoryListener?.remove()
        remoteHistoryListener = null
        super.onCleared()
    }

    private fun ensureCurrentSessionId(): String {
        val session = AiConversationHistoryStore.ensureSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Bot,
            preferredSessionId = currentSessionId,
        )
        currentSessionId = session.id
        refreshSessionState(session.id)
        return session.id
    }

    private fun refreshSessionState(preferredSessionId: String? = currentSessionId) {
        val activeSession = AiConversationHistoryStore.ensureSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Bot,
            preferredSessionId = preferredSessionId,
        )
        currentSessionId = activeSession.id
        val sessions = AiConversationHistoryStore.sessionSnapshotsFor(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Bot,
        )
        val activeSnapshot = sessions.firstOrNull { it.sessionId == activeSession.id }
        _uiState.update {
            it.copy(
                sessions = sessions,
                activeSessionId = activeSession.id,
                activeSessionTitle = activeSnapshot?.title ?: activeSession.title,
            )
        }
    }

    private fun makeRequestContext(
        levelAtSend: AiExperienceLevel,
        sessionIdAtSend: String,
    ): InFlightRequestContext = InFlightRequestContext(
        requestId = java.util.UUID.randomUUID().toString(),
        conversationRevision = conversationRevision,
        userKeyAtSend = currentUserKey,
        sessionIdAtSend = sessionIdAtSend,
        aiLevelAtSend = levelAtSend,
    )

    private fun isRequestContextActive(context: InFlightRequestContext): Boolean =
        activeRequestContext == context &&
            context.conversationRevision == conversationRevision &&
            context.userKeyAtSend == currentUserKey &&
            context.sessionIdAtSend == currentSessionId &&
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
            remoteHydrationJob?.cancel()
            remoteHydrationJob = null
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

    private fun restoreHistory(preferredSessionId: String? = currentSessionId) {
        refreshSessionState(preferredSessionId)
        val restoredMessages = AiConversationHistoryStore.entriesForSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Bot,
            sessionId = currentSessionId,
        ).flatMap { entry ->
            listOf(
                AiMessage(role = AiMessageRole.User, text = entry.prompt),
                AiMessage(
                    role = AiMessageRole.Assistant,
                    text = entry.response,
                    imageBytes = AiConversationHistoryStore.imageBytesFor(entry),
                    imageMimeType = entry.imageMimeType.takeIf { it.isNotBlank() },
                ),
            )
        }

        _uiState.update { currentState ->
            currentState.copy(
                messages = restoredMessages,
                botPhase = if (restoredMessages.isEmpty() && currentState.draft.isNotBlank()) {
                    BotInteractionPhase.Typing
                } else {
                    BotInteractionPhase.Idle
                },
                errorMessage = null,
                lastDecision = null,
            )
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

    private fun hydrateRemoteHistoryIfNeeded(preferredSessionId: String?) {
        remoteHydrationJob?.cancel()
        val userId = currentUserId ?: return
        val userKey = currentUserKey
        val localSessions = AiConversationHistoryStore.sessionsFor(
            userKey = userKey,
            source = AiConversationHistorySource.Bot,
        )
        val localEntries = AiConversationHistoryStore.entriesFor(
            userKey = userKey,
            source = AiConversationHistorySource.Bot,
        )
        remoteHydrationJob = viewModelScope.launch {
            runCatching {
                syncRepository.migrateLocalHistoryIfRemoteEmpty(
                    userId = userId,
                    source = AiConversationHistorySource.Bot,
                    localSessions = localSessions,
                    localEntries = localEntries,
                )
                syncRepository.pruneHistory(
                    userId = userId,
                    source = AiConversationHistorySource.Bot,
                    retentionDays = currentHistoryRetentionDays,
                )
                syncRepository.fetchSnapshot(userId, AiConversationHistorySource.Bot)
            }.onSuccess { snapshot ->
                if (currentUserId != userId || currentUserKey != userKey) {
                    return@onSuccess
                }
                AiConversationHistoryStore.replaceRemoteState(
                    userKey = userKey,
                    source = AiConversationHistorySource.Bot,
                    sessions = snapshot.sessions,
                    entries = snapshot.entries,
                )
                restoreHistory(preferredSessionId)
            }
        }
    }

    private fun startRemoteHistoryObservation() {
        remoteHistoryListener?.remove()
        val userId = currentUserId ?: return
        val userKey = currentUserKey
        remoteHistoryListener = syncRepository.observeSnapshot(
            userId = userId,
            source = AiConversationHistorySource.Bot,
        ) { result ->
            result.onSuccess { snapshot ->
                viewModelScope.launch {
                    if (currentUserId != userId || currentUserKey != userKey) {
                        return@launch
                    }
                    applyRemoteSnapshot(snapshot, userKey)
                }
            }
        }
    }

    private fun applyRemoteSnapshot(
        snapshot: com.nash.skyos.data.AiConversationRemoteSnapshot,
        userKey: String?,
    ) {
        AiConversationHistoryStore.replaceRemoteState(
            userKey = userKey,
            source = AiConversationHistorySource.Bot,
            sessions = snapshot.sessions,
            entries = snapshot.entries,
        )

        if (_uiState.value.botPhase.isBusy) {
            refreshSessionState(currentSessionId)
        } else {
            restoreHistory(currentSessionId)
        }
    }

    private fun syncSessionToRemote(session: com.nash.skyos.data.AiConversationHistorySession) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            runCatching {
                syncRepository.upsertSession(userId, session)
                syncRepository.pruneHistory(
                    userId = userId,
                    source = AiConversationHistorySource.Bot,
                    retentionDays = currentHistoryRetentionDays,
                )
            }
        }
    }

    private fun syncSavedEntryToRemote(saveResult: AiConversationHistorySaveResult) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            runCatching {
                syncRepository.upsertSession(userId, saveResult.session)
                syncRepository.upsertEntry(userId, saveResult.entry)
                syncRepository.pruneHistory(
                    userId = userId,
                    source = AiConversationHistorySource.Bot,
                    retentionDays = currentHistoryRetentionDays,
                )
            }
        }
    }

    private fun deleteSessionFromRemote(sessionId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            runCatching {
                syncRepository.deleteSession(userId, sessionId)
            }
        }
    }

    private fun pruneRemoteHistoryIfNeeded() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            runCatching {
                syncRepository.pruneHistory(
                    userId = userId,
                    source = AiConversationHistorySource.Bot,
                    retentionDays = currentHistoryRetentionDays,
                )
            }
        }
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

    private fun resolveTerminalPhase(decision: com.nash.skyos.data.AiBotDecision?): BotInteractionPhase {
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

    private fun decisionFor(error: Throwable): com.nash.skyos.data.AiBotDecision {
        val description = userFacingErrorMessage(error)
        val isRetryable = error is FirebaseFunctionsException && when (error.code) {
            FirebaseFunctionsException.Code.UNAVAILABLE,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
            FirebaseFunctionsException.Code.INTERNAL,
            -> true

            else -> false
        }

        return com.nash.skyos.data.AiBotDecision(
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

    private fun isAiRequestAllowed(): Boolean {
        val currentUser = AppContainer.currentUser.value
        if (!AppFeatureFlagsStore.allowsAiAccess(currentUser)) {
            _uiState.update { it.copy(errorMessage = AppFeatureFlagsStore.accessDeniedMessage(currentUser)) }
            return false
        }
        if (!_uiState.value.isAiEnabled) {
            _uiState.update { it.copy(errorMessage = "Der SkyOS Bot ist gerade pausiert.") }
            return false
        }
        return true
    }
}
