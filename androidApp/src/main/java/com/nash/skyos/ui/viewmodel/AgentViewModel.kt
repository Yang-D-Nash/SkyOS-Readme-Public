package com.nash.skyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctionsException
import com.nash.skyos.R
import com.nash.skyos.data.AgentHistoryTurn
import com.nash.skyos.data.AgentPendingQueueEntry
import com.nash.skyos.data.AgentPendingQueueStore
import com.nash.skyos.data.AgentResultEntry
import com.nash.skyos.data.AiConversationHistorySource
import com.nash.skyos.data.AiConversationHistorySaveResult
import com.nash.skyos.data.AiConversationHistoryStore
import com.nash.skyos.data.AiConversationRemoteSnapshot
import com.nash.skyos.data.AiConversationSyncRepository
import com.nash.skyos.data.AiRuntimeAgentProvider
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.AppFeatureFlagsStore
import com.nash.skyos.data.AppNetworkMonitor
import com.nash.skyos.data.AppTextResolver
import com.nash.skyos.data.ManusByosPreferences
import com.nash.skyos.ui.model.AgentAutomationScope
import com.nash.skyos.ui.model.AgentExecutionMode
import com.nash.skyos.ui.model.AgentInteractionPhase
import com.nash.skyos.ui.model.AgentMessage
import com.nash.skyos.ui.model.AgentMessageRole
import com.nash.skyos.ui.model.AgentResultType
import com.nash.skyos.ui.model.AgentUiState
import com.nash.skyos.ui.model.AgentWorkflowSummary
import com.nash.skyos.ui.model.AiExperienceLevel
import com.nash.skyos.ui.model.agentQuickPromptsFor
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

class AgentViewModel : ViewModel() {
    private data class PendingAgentRequest(
        val sessionId: String,
        val prompt: String,
        val history: List<AgentHistoryTurn>,
        val mode: String,
        val aiLevel: String,
        val executeAutomation: Boolean,
        val automationScope: String,
        val assistantMessageId: String,
        val createdAtEpochMillis: Long,
    )

    private data class InFlightRequestContext(
        val requestId: String,
        val conversationRevision: Int,
        val userKeyAtSend: String?,
        val sessionIdAtSend: String,
        val aiLevelAtSend: AiExperienceLevel,
        val requiresLevelMatch: Boolean,
    )

    private val agentClient = AppContainer.agentClient
    private val syncRepository = AiConversationSyncRepository()
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var currentUserKey: String? = null
    private var currentQuotaPlan: UserQuotaPlan = UserQuotaPlan.Free
    private var currentHistoryRetentionDays: Int = UserRole.User.defaultAiHistoryRetentionDays
    private var currentSessionId: String? = null
    private val pendingRequests = ArrayDeque<PendingAgentRequest>()
    private var conversationRevision = 0
    private var activeRequestJob: Job? = null
    private var pendingRetryJob: Job? = null
    private var remoteHydrationJob: Job? = null
    private var remoteHistoryListener: ListenerRegistration? = null
    private var activeRequestContext: InFlightRequestContext? = null

    init {
        viewModelScope.launch {
            AppContainer.aiEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isAgentEnabled = isEnabled) }
            }
        }

        viewModelScope.launch {
            AppNetworkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    retryPendingMessages()
                }
            }
        }

        viewModelScope.launch {
            AppContainer.currentUser.collect { user ->
                currentHistoryRetentionDays =
                    user?.resolvedAiHistoryRetentionDays ?: UserRole.User.defaultAiHistoryRetentionDays
                AiConversationHistoryStore.updateRetentionDays(currentHistoryRetentionDays)
                _uiState.update {
                    val canTriggerAutomation = user != null
                    it.copy(
                        canTriggerAutomation = canTriggerAutomation,
                        shouldTriggerAutomation = when {
                            !canTriggerAutomation -> false
                            it.selectedMode == AgentExecutionMode.Automation -> true
                            else -> it.shouldTriggerAutomation
                        },
                    )
                }

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
                        selectedLevel = resolvedAgentExperienceLevel(quotaPlan),
                    )
                }

                ManusByosPreferences.setUserMode(user?.id)
                if (userKey != currentUserKey || userId != currentUserId) {
                    invalidateConversation(cancelActiveWork = true)
                    remoteHistoryListener?.remove()
                    remoteHistoryListener = null
                    currentUserId = userId
                    currentUserKey = userKey
                    currentSessionId = null
                    restoreHistory()
                    hydrateRemoteHistoryIfNeeded(currentSessionId)
                    startRemoteHistoryObservation()
                    if (AppNetworkMonitor.isOnline.value) {
                        retryPendingMessages()
                    }
                } else {
                    refreshSessionState()
                    pruneRemoteHistoryIfNeeded()
                }
            }
        }
    }

    fun updateDraft(draft: String) {
        _uiState.update { it.copy(draft = draft) }
    }

    fun updateMode(mode: AgentExecutionMode) {
        _uiState.update {
            it.copy(
                selectedMode = mode,
                shouldTriggerAutomation = if (mode == AgentExecutionMode.Automation && it.canTriggerAutomation) {
                    true
                } else {
                    it.shouldTriggerAutomation
                },
                quickPrompts = agentQuickPromptsFor(mode),
            )
        }
    }

    fun updateLevel(level: AiExperienceLevel) {
        _uiState.update { state ->
            if (state.agentPhase.shouldBlockComposerChrome || !level.isAvailableFor(currentQuotaPlan)) {
                state
            } else {
                state.copy(selectedLevel = level)
            }
        }
    }

    fun updateAutomationScope(scope: AgentAutomationScope) {
        _uiState.update { state ->
            if (state.agentPhase.shouldBlockComposerChrome) state else state.copy(selectedAutomationScope = scope)
        }
    }

    fun toggleAutomation() {
        _uiState.update { currentState ->
            if (!currentState.canTriggerAutomation) {
                currentState
            } else {
                currentState.copy(shouldTriggerAutomation = !currentState.shouldTriggerAutomation)
            }
        }
    }

    fun sendDraft() {
        sendPrompt(_uiState.value.draft)
    }

    fun sendPrompt(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (!AppFeatureFlagsStore.allowsAiAccess(AppContainer.currentUser.value)) {
            _uiState.update {
                it.copy(errorMessage = AppFeatureFlagsStore.accessDeniedMessage(AppContainer.currentUser.value))
            }
            return
        }
        if (!_uiState.value.isAgentEnabled) {
            _uiState.update { it.copy(errorMessage = "Der SkyOS Agent ist gerade pausiert.") }
            return
        }
        if (trimmedPrompt.isBlank() || _uiState.value.agentPhase.shouldBlockSend) return

        val levelAtSend = _uiState.value.selectedLevel
        if (!levelAtSend.isAvailableFor(currentQuotaPlan)) {
            _uiState.update { it.copy(errorMessage = AppTextResolver.string(R.string.ai_level_unavailable)) }
            return
        }
        if (!AppNetworkMonitor.isOnline.value) {
            enqueuePromptForRetry(trimmedPrompt)
            return
        }

        val activeSessionId = ensureCurrentSessionId()
        val modeAtSend = _uiState.value.selectedMode.rawValue
        val levelRawAtSend = levelAtSend.rawValue
        val executeAutomationAtSend = _uiState.value.canTriggerAutomation && _uiState.value.shouldTriggerAutomation
        val automationScopeAtSend = _uiState.value.selectedAutomationScope.rawValue
        val userMessage = AgentMessage(role = AgentMessageRole.User, text = trimmedPrompt)
        val assistantMessage = AgentMessage(
            role = AgentMessageRole.Assistant,
            text = "",
            isStreaming = true,
            resultType = AgentResultType.Progress,
        )
        val assistantMessageId = assistantMessage.id
        val historyAtSend = buildHistory(_uiState.value.messages + userMessage)
        val requestContext = makeRequestContext(
            aiLevelAtSend = levelAtSend,
            sessionIdAtSend = activeSessionId,
        )

        _uiState.update {
            it.copy(
                draft = "",
                agentPhase = AgentInteractionPhase.Executing,
                errorMessage = null,
                messages = it.messages + userMessage + assistantMessage,
            )
        }

        activeRequestContext = requestContext
        activeRequestJob?.cancel()
        activeRequestJob = viewModelScope.launch {
            try {
                val result = agentClient.sendMessage(
                    prompt = trimmedPrompt,
                    history = historyAtSend,
                    mode = modeAtSend,
                    aiLevel = levelRawAtSend,
                    executeAutomation = executeAutomationAtSend,
                    automationScope = automationScopeAtSend,
                    manusApiKeyOverride = ManusByosPreferences.currentManusApiKeyOrNull(),
                )
                if (!isRequestContextActive(requestContext)) return@launch

                AiConversationHistoryStore.updateRetentionDays(result.historyRetentionDays)
                val savedResult = AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Agent,
                    sessionId = requestContext.sessionIdAtSend,
                    prompt = trimmedPrompt,
                    response = augmentedReply(result),
                )
                savedResult?.let(::syncSavedEntryToRemote)
                refreshSessionState(requestContext.sessionIdAtSend)
                _uiState.update { it.copy(usageSnapshot = result.usage) }
                val replyText = augmentedReply(result)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = replyText,
                    isStreaming = false,
                    resultType = if (result.automationTriggered || result.automationAttempted) {
                        AgentResultType.Workflow
                    } else {
                        AgentResultType.Text
                    },
                    workflowSummary = buildWorkflowSummary(result),
                    results = result.results,
                )
                val trimmedNotice = result.providerNotice.trim()
                val effectiveNotice = when {
                    trimmedNotice.isNotEmpty() -> trimmedNotice
                    result.providerFallbackUsed -> "Provider-Fallback aktiv."
                    else -> ""
                }
                _uiState.update { state ->
                    val nextError = when {
                        result.automationTriggered -> null
                        result.automationAttempted && result.automationMessage.isNotBlank() -> result.automationMessage
                        else -> null
                    }
                    state.copy(
                        agentPhase = resolveTerminalPhase(result.decision, pendingRequests.isNotEmpty()),
                        errorMessage = nextError,
                        lastAgentProvider = AiRuntimeAgentProvider.resolve(result.agentProvider),
                        lastProviderNotice = effectiveNotice,
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException || !isRequestContextActive(requestContext)) {
                    clearActiveRequestIfNeeded(requestContext)
                    return@launch
                }

                if (isOfflineError(error)) {
                    pendingRequests.addLast(
                        PendingAgentRequest(
                            sessionId = requestContext.sessionIdAtSend,
                            prompt = trimmedPrompt,
                            history = historyAtSend,
                            mode = modeAtSend,
                            aiLevel = levelRawAtSend,
                            executeAutomation = executeAutomationAtSend,
                            automationScope = automationScopeAtSend,
                            assistantMessageId = assistantMessageId,
                            createdAtEpochMillis = System.currentTimeMillis(),
                        ),
                    )
                    persistPendingRequests()
                    updateAssistantMessage(
                        messageId = assistantMessageId,
                        text = AppTextResolver.string(R.string.agent_queue_placeholder),
                        isStreaming = false,
                        resultType = AgentResultType.Progress,
                    )
                    _uiState.update {
                        it.copy(agentPhase = AgentInteractionPhase.WaitingReconnect, errorMessage = null)
                    }
                    clearActiveRequestIfNeeded(requestContext)
                    return@launch
                }

                val errorMessage = userFacingErrorMessage(error)
                val savedResult = AiConversationHistoryStore.saveEntry(
                    userKey = requestContext.userKeyAtSend,
                    source = AiConversationHistorySource.Agent,
                    sessionId = requestContext.sessionIdAtSend,
                    prompt = trimmedPrompt,
                    response = errorMessage,
                )
                savedResult?.let(::syncSavedEntryToRemote)
                refreshSessionState(requestContext.sessionIdAtSend)
                updateAssistantMessage(
                    messageId = assistantMessageId,
                    text = errorMessage,
                    isStreaming = false,
                    resultType = AgentResultType.Error,
                )
                _uiState.update {
                    it.copy(
                        agentPhase = AgentInteractionPhase.Idle,
                        errorMessage = errorMessage,
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
        invalidateConversation(cancelActiveWork = true)
        pendingRequests.clear()
        val newSession = AiConversationHistoryStore.createSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Agent,
        )
        currentSessionId = newSession.id
        refreshSessionState(newSession.id)
        syncSessionToRemote(newSession)
        _uiState.update { currentState ->
            currentState.copy(
                messages = emptyList(),
                agentPhase = AgentInteractionPhase.Idle,
                errorMessage = null,
            )
        }
    }

    fun openConversation(sessionId: String) {
        if (sessionId == currentSessionId) return
        invalidateConversation(cancelActiveWork = true)
        currentSessionId = sessionId
        restoreHistory()
        if (AppNetworkMonitor.isOnline.value) {
            retryPendingMessages()
        }
    }

    fun refreshActiveConversation() {
        if (_uiState.value.agentPhase.shouldBlockComposerChrome) return
        invalidateConversation(cancelActiveWork = true)
        restoreHistory()
        if (AppNetworkMonitor.isOnline.value) {
            retryPendingMessages()
        }
    }

    fun renameActiveConversation(title: String) {
        AiConversationHistoryStore.renameSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Agent,
            sessionId = currentSessionId,
            title = title,
        )?.let(::syncSessionToRemote)
        refreshSessionState()
    }

    fun deleteActiveConversation() {
        val sessionId = currentSessionId ?: return
        invalidateConversation(cancelActiveWork = true)
        pendingRequests.clear()
        AgentPendingQueueStore.clearEntriesForSession(currentUserKey, sessionId)
        AiConversationHistoryStore.deleteSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Agent,
            sessionId = sessionId,
        )
        deleteSessionFromRemote(sessionId)
        currentSessionId = null
        restoreHistory()
        if (AppNetworkMonitor.isOnline.value) {
            retryPendingMessages()
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

    override fun onCleared() {
        remoteHistoryListener?.remove()
        remoteHistoryListener = null
        super.onCleared()
    }

    private fun enqueuePromptForRetry(trimmedPrompt: String) {
        val activeSessionId = ensureCurrentSessionId()
        val modeAtSend = _uiState.value.selectedMode.rawValue
        val executeAutomationAtSend = _uiState.value.canTriggerAutomation && _uiState.value.shouldTriggerAutomation
        val automationScopeAtSend = _uiState.value.selectedAutomationScope.rawValue
        val userMessage = AgentMessage(role = AgentMessageRole.User, text = trimmedPrompt)
        val assistantMessage = AgentMessage(
            role = AgentMessageRole.Assistant,
            text = AppTextResolver.string(R.string.agent_queue_placeholder),
            isStreaming = false,
            resultType = AgentResultType.Progress,
        )
        val history = buildHistory(_uiState.value.messages + userMessage)
        val levelAtSend = _uiState.value.selectedLevel

        pendingRequests.addLast(
            PendingAgentRequest(
                sessionId = activeSessionId,
                prompt = trimmedPrompt,
                history = history,
                mode = modeAtSend,
                aiLevel = levelAtSend.rawValue,
                executeAutomation = executeAutomationAtSend,
                automationScope = automationScopeAtSend,
                assistantMessageId = assistantMessage.id,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        persistPendingRequests()

        _uiState.update {
            it.copy(
                draft = "",
                agentPhase = AgentInteractionPhase.WaitingReconnect,
                errorMessage = null,
                messages = it.messages + userMessage + assistantMessage,
            )
        }
    }

    private fun retryPendingMessages() {
        if (pendingRetryJob != null || pendingRequests.isEmpty() || activeRequestJob != null) {
            return
        }

        val retryConversationRevision = conversationRevision
        val retryUserKey = currentUserKey
        pendingRetryJob = viewModelScope.launch {
            if (!isConversationSnapshotValid(retryConversationRevision, retryUserKey)) {
                pendingRetryJob = null
                return@launch
            }

            _uiState.update { it.copy(agentPhase = AgentInteractionPhase.Executing) }
            while (AppNetworkMonitor.isOnline.value && pendingRequests.isNotEmpty()) {
                if (!isConversationSnapshotValid(retryConversationRevision, retryUserKey)) {
                    pendingRetryJob = null
                    return@launch
                }

                val request = pendingRequests.removeFirst()
                val requestContext = makeRequestContext(
                    conversationRevision = retryConversationRevision,
                    userKeyAtSend = retryUserKey,
                    sessionIdAtSend = request.sessionId,
                    aiLevelAtSend = AiExperienceLevel.resolve(request.aiLevel),
                    requiresLevelMatch = false,
                )
                activeRequestContext = requestContext

                try {
                    val result = agentClient.sendMessage(
                        prompt = request.prompt,
                        history = request.history,
                        mode = request.mode,
                        aiLevel = request.aiLevel,
                        executeAutomation = request.executeAutomation,
                        automationScope = request.automationScope,
                        manusApiKeyOverride = ManusByosPreferences.currentManusApiKeyOrNull(),
                    )
                    if (!isRequestContextActive(requestContext)) {
                        pendingRetryJob = null
                        return@launch
                    }

                    AiConversationHistoryStore.updateRetentionDays(result.historyRetentionDays)
                    val savedResult = AiConversationHistoryStore.saveEntry(
                        userKey = requestContext.userKeyAtSend,
                        source = AiConversationHistorySource.Agent,
                        sessionId = requestContext.sessionIdAtSend,
                        prompt = request.prompt,
                        response = augmentedReply(result),
                    )
                    savedResult?.let(::syncSavedEntryToRemote)
                    refreshSessionState(requestContext.sessionIdAtSend)
                    _uiState.update { it.copy(usageSnapshot = result.usage) }
                    val replyText = augmentedReply(result)
                    updateAssistantMessage(
                        messageId = request.assistantMessageId,
                        text = replyText,
                        isStreaming = false,
                        resultType = if (result.automationTriggered || result.automationAttempted) {
                            AgentResultType.Workflow
                        } else {
                            AgentResultType.Text
                        },
                        workflowSummary = buildWorkflowSummary(result),
                        results = result.results,
                    )
                    val trimmedNotice = result.providerNotice.trim()
                    val effectiveNotice = when {
                        trimmedNotice.isNotEmpty() -> trimmedNotice
                        result.providerFallbackUsed -> "Provider-Fallback aktiv."
                        else -> ""
                    }
                    val hasMorePendingRequests = pendingRequests.isNotEmpty()
                    _uiState.update { state ->
                        val nextError = when {
                            result.automationTriggered -> null
                            result.automationAttempted && result.automationMessage.isNotBlank() -> result.automationMessage
                            else -> null
                        }
                        state.copy(
                            agentPhase = if (hasMorePendingRequests) {
                                AgentInteractionPhase.Executing
                            } else {
                                resolveTerminalPhase(result.decision, hasPendingQueue = false)
                            },
                            errorMessage = nextError,
                            lastAgentProvider = AiRuntimeAgentProvider.resolve(result.agentProvider),
                            lastProviderNotice = effectiveNotice,
                        )
                    }
                    persistPendingRequests()
                    activeRequestContext = null
                } catch (error: Throwable) {
                    if (error is CancellationException) {
                        activeRequestContext = null
                        pendingRetryJob = null
                        return@launch
                    }
                    if (!isRequestContextActive(requestContext)) {
                        pendingRetryJob = null
                        return@launch
                    }

                    if (isOfflineError(error) || !AppNetworkMonitor.isOnline.value) {
                        pendingRequests.addFirst(request)
                        persistPendingRequests()
                        updateAssistantMessage(
                            messageId = request.assistantMessageId,
                            text = AppTextResolver.string(R.string.agent_queue_placeholder),
                            isStreaming = false,
                            resultType = AgentResultType.Progress,
                        )
                        activeRequestContext = null
                        break
                    }

                    val message = userFacingErrorMessage(error)
                    val savedResult = AiConversationHistoryStore.saveEntry(
                        userKey = requestContext.userKeyAtSend,
                        source = AiConversationHistorySource.Agent,
                        sessionId = requestContext.sessionIdAtSend,
                        prompt = request.prompt,
                        response = message,
                    )
                    savedResult?.let(::syncSavedEntryToRemote)
                    refreshSessionState(requestContext.sessionIdAtSend)
                    updateAssistantMessage(
                        messageId = request.assistantMessageId,
                        text = message,
                        isStreaming = false,
                        resultType = AgentResultType.Error,
                    )
                    _uiState.update { it.copy(errorMessage = message) }
                    persistPendingRequests()
                    activeRequestContext = null
                }
            }

            activeRequestContext = null
            pendingRetryJob = null
            if (!isConversationSnapshotValid(retryConversationRevision, retryUserKey)) {
                return@launch
            }
            _uiState.update {
                it.copy(
                    agentPhase = if (pendingRequests.isEmpty()) {
                        if (it.agentPhase == AgentInteractionPhase.Executing) {
                            AgentInteractionPhase.Idle
                        } else {
                            it.agentPhase
                        }
                    } else {
                        AgentInteractionPhase.WaitingReconnect
                    },
                )
            }
        }
    }

    private fun ensureCurrentSessionId(): String {
        val session = AiConversationHistoryStore.ensureSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Agent,
            preferredSessionId = currentSessionId,
        )
        currentSessionId = session.id
        refreshSessionState(session.id)
        return session.id
    }

    private fun refreshSessionState(preferredSessionId: String? = currentSessionId) {
        val activeSession = AiConversationHistoryStore.ensureSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Agent,
            preferredSessionId = preferredSessionId,
        )
        currentSessionId = activeSession.id
        val sessions = AiConversationHistoryStore.sessionSnapshotsFor(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Agent,
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
        conversationRevision: Int = this.conversationRevision,
        userKeyAtSend: String? = currentUserKey,
        sessionIdAtSend: String = currentSessionId.orEmpty(),
        aiLevelAtSend: AiExperienceLevel = _uiState.value.selectedLevel,
        requiresLevelMatch: Boolean = true,
    ): InFlightRequestContext = InFlightRequestContext(
        requestId = java.util.UUID.randomUUID().toString(),
        conversationRevision = conversationRevision,
        userKeyAtSend = userKeyAtSend,
        sessionIdAtSend = sessionIdAtSend,
        aiLevelAtSend = aiLevelAtSend,
        requiresLevelMatch = requiresLevelMatch,
    )

    private fun isRequestContextActive(context: InFlightRequestContext): Boolean =
        activeRequestContext == context &&
            context.conversationRevision == conversationRevision &&
            context.userKeyAtSend == currentUserKey &&
            context.sessionIdAtSend == currentSessionId &&
            (!context.requiresLevelMatch || context.aiLevelAtSend == _uiState.value.selectedLevel)

    private fun isConversationSnapshotValid(
        conversationRevision: Int,
        userKeyAtSend: String?,
    ): Boolean = this.conversationRevision == conversationRevision && currentUserKey == userKeyAtSend

    private fun clearActiveRequestIfNeeded(context: InFlightRequestContext) {
        if (activeRequestContext == context) {
            activeRequestContext = null
            activeRequestJob = null
        }
    }

    private fun invalidateConversation(cancelActiveWork: Boolean) {
        if (cancelActiveWork) {
            activeRequestJob?.cancel()
            activeRequestJob = null
            pendingRetryJob?.cancel()
            pendingRetryJob = null
            remoteHydrationJob?.cancel()
            remoteHydrationJob = null
            activeRequestContext = null
        }
        conversationRevision += 1
    }

    private fun isOfflineError(error: Throwable): Boolean {
        if (error is FirebaseFunctionsException && error.code == FirebaseFunctionsException.Code.UNAVAILABLE) {
            return true
        }
        val normalizedMessage = error.message.orEmpty().lowercase()
        return normalizedMessage.contains("offline") ||
            normalizedMessage.contains("internet")
    }

    private fun updateAssistantMessage(
        messageId: String,
        text: String,
        isStreaming: Boolean,
        resultType: AgentResultType = AgentResultType.Text,
        workflowSummary: AgentWorkflowSummary? = null,
        results: List<AgentResultEntry> = emptyList(),
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == messageId) {
                        message.copy(
                            text = text,
                            isStreaming = isStreaming,
                            resultType = resultType,
                            workflowSummary = workflowSummary,
                            results = results,
                        )
                    } else {
                        message
                    }
                },
            )
        }
    }

    private fun buildHistory(messages: List<AgentMessage>): List<AgentHistoryTurn> =
        messages.map { message ->
            AgentHistoryTurn(
                role = if (message.role == AgentMessageRole.User) "user" else "assistant",
                text = message.text,
            )
        }

    private fun restoreHistory() {
        refreshSessionState()
        val restoredHistoryMessages = AiConversationHistoryStore.entriesForSession(
            userKey = currentUserKey,
            source = AiConversationHistorySource.Agent,
            sessionId = currentSessionId,
        ).flatMap { entry ->
            listOf(
                AgentMessage(role = AgentMessageRole.User, text = entry.prompt),
                AgentMessage(role = AgentMessageRole.Assistant, text = entry.response),
            )
        }

        pendingRequests.clear()
        pendingRequests.addAll(
            AgentPendingQueueStore.entriesFor(currentUserKey, currentSessionId).map { entry ->
                PendingAgentRequest(
                    sessionId = entry.sessionId,
                    prompt = entry.prompt,
                    history = entry.history,
                    mode = entry.mode,
                    aiLevel = entry.aiLevel,
                    executeAutomation = entry.executeAutomation,
                    automationScope = entry.automationScope,
                    assistantMessageId = entry.assistantMessageId,
                    createdAtEpochMillis = entry.createdAtEpochMillis,
                )
            },
        )
        val pendingMessages = pendingRequests.flatMap { request ->
            listOf(
                AgentMessage(role = AgentMessageRole.User, text = request.prompt),
                AgentMessage(
                    id = request.assistantMessageId,
                    role = AgentMessageRole.Assistant,
                    text = AppTextResolver.string(R.string.agent_queue_placeholder),
                    resultType = AgentResultType.Progress,
                ),
            )
        }

        _uiState.update { currentState ->
            currentState.copy(
                messages = restoredHistoryMessages + pendingMessages,
                agentPhase = if (pendingRequests.isEmpty()) {
                    AgentInteractionPhase.Idle
                } else {
                    AgentInteractionPhase.WaitingReconnect
                },
            )
        }
    }

    private fun persistPendingRequests() {
        val entries = pendingRequests.map { request ->
            AgentPendingQueueEntry(
                userKey = currentUserKey.orEmpty(),
                sessionId = request.sessionId,
                prompt = request.prompt,
                history = request.history,
                mode = request.mode,
                aiLevel = request.aiLevel,
                executeAutomation = request.executeAutomation,
                automationScope = request.automationScope,
                assistantMessageId = request.assistantMessageId,
                createdAtEpochMillis = request.createdAtEpochMillis,
            )
        }
        AgentPendingQueueStore.saveEntriesForSession(currentUserKey, currentSessionId, entries)
    }

    private fun hydrateRemoteHistoryIfNeeded(preferredSessionId: String?) {
        remoteHydrationJob?.cancel()
        val userId = currentUserId ?: return
        val userKey = currentUserKey
        val localSessions = AiConversationHistoryStore.sessionsFor(
            userKey = userKey,
            source = AiConversationHistorySource.Agent,
        )
        val localEntries = AiConversationHistoryStore.entriesFor(
            userKey = userKey,
            source = AiConversationHistorySource.Agent,
        )
        remoteHydrationJob = viewModelScope.launch {
            runCatching {
                syncRepository.migrateLocalHistoryIfRemoteEmpty(
                    userId = userId,
                    source = AiConversationHistorySource.Agent,
                    localSessions = localSessions,
                    localEntries = localEntries,
                )
                syncRepository.pruneHistory(
                    userId = userId,
                    source = AiConversationHistorySource.Agent,
                    retentionDays = currentHistoryRetentionDays,
                )
                syncRepository.fetchSnapshot(userId, AiConversationHistorySource.Agent)
            }.onSuccess { snapshot ->
                if (currentUserId != userId || currentUserKey != userKey) {
                    return@onSuccess
                }
                AiConversationHistoryStore.replaceRemoteState(
                    userKey = userKey,
                    source = AiConversationHistorySource.Agent,
                    sessions = snapshot.sessions,
                    entries = snapshot.entries,
                )
                if (preferredSessionId != null) {
                    currentSessionId = preferredSessionId
                }
                restoreHistory()
                if (AppNetworkMonitor.isOnline.value) {
                    retryPendingMessages()
                }
            }
        }
    }

    private fun startRemoteHistoryObservation() {
        remoteHistoryListener?.remove()
        val userId = currentUserId ?: return
        val userKey = currentUserKey
        remoteHistoryListener = syncRepository.observeSnapshot(
            userId = userId,
            source = AiConversationHistorySource.Agent,
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
        snapshot: AiConversationRemoteSnapshot,
        userKey: String?,
    ) {
        AiConversationHistoryStore.replaceRemoteState(
            userKey = userKey,
            source = AiConversationHistorySource.Agent,
            sessions = snapshot.sessions,
            entries = snapshot.entries,
        )

        if (_uiState.value.agentPhase.shouldBlockComposerChrome) {
            refreshSessionState(currentSessionId)
        } else {
            restoreHistory()
            if (AppNetworkMonitor.isOnline.value) {
                retryPendingMessages()
            }
        }
    }

    private fun syncSessionToRemote(session: com.nash.skyos.data.AiConversationHistorySession) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            runCatching {
                syncRepository.upsertSession(userId, session)
                syncRepository.pruneHistory(
                    userId = userId,
                    source = AiConversationHistorySource.Agent,
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
                    source = AiConversationHistorySource.Agent,
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
                    source = AiConversationHistorySource.Agent,
                    retentionDays = currentHistoryRetentionDays,
                )
            }
        }
    }

    private fun normalizeUserKey(user: User?): String? {
        return user?.id?.takeIf { it.isNotBlank() }
            ?: user?.email?.takeIf { it.isNotBlank() }
    }

    private fun augmentedReply(result: com.nash.skyos.data.AgentResponse): String {
        if (!result.automationTriggered) {
            return result.reply
        }
        val workflowLabel = result.workflowName.trim().ifBlank { "Workflow" }
        val automationMessage = result.automationMessage.trim().ifBlank { "An $workflowLabel uebergeben." }
        return "${result.reply}\n\nWorkflow:\n$automationMessage"
    }

    private fun buildWorkflowSummary(result: com.nash.skyos.data.AgentResponse): AgentWorkflowSummary? {
        val structuredWorkflow = result.results.firstOrNull { it.type == "workflow" }
        if (!result.automationTriggered && !result.automationAttempted && structuredWorkflow == null) {
            return null
        }
        val workflowLabel = (structuredWorkflow?.workflowName ?: result.workflowName).trim().ifBlank { "External Workflow" }
        val structuredSummary = structuredWorkflow?.summary.orEmpty().trim()
        val statusText = when {
            structuredSummary.isNotEmpty() -> structuredSummary
            result.automationMessage.isNotBlank() -> result.automationMessage.trim()
            result.automationTriggered -> "Workflow wurde gestartet."
            else -> "Workflow konnte nicht gestartet werden."
        }
        val runId = (structuredWorkflow?.runId ?: result.agentRunId).trim().ifBlank { null }
        return AgentWorkflowSummary(
            workflowName = workflowLabel,
            statusText = statusText,
            runId = runId,
        )
    }

    private fun userFacingErrorMessage(error: Throwable): String = when (error) {
        is FirebaseFunctionsException -> when (error.code) {
            FirebaseFunctionsException.Code.NOT_FOUND,
            FirebaseFunctionsException.Code.UNIMPLEMENTED,
            -> "Dieser Agent-Bereich wird gerade vorbereitet."
            FirebaseFunctionsException.Code.UNAVAILABLE -> "Der SkyOS Agent ist gerade kurz nicht erreichbar."
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Die Agent-Antwort dauert gerade laenger als gewohnt."
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                AppTextResolver.string(R.string.ai_level_limit_reached)
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> "Die Anfrage konnte so nicht verarbeitet werden."
            FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                if (error.localizedMessage?.contains("App Check", ignoreCase = true) == true) {
                    "Der Sicherheitscheck laeuft noch. Bitte kurz erneut versuchen."
                } else {
                    error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Der Agent ist noch nicht voll bereit."
                }
            FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                AppTextResolver.string(R.string.ai_level_unavailable)
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Bitte melde dich erneut an und versuch es noch einmal."
            else -> "Der SkyOS Agent ist gerade kurz pausiert."
        }
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "Der SkyOS Agent ist gerade kurz pausiert."
    }
}

private fun AiExperienceLevel.isAvailableFor(plan: UserQuotaPlan): Boolean = when (this) {
    AiExperienceLevel.Standard,
    AiExperienceLevel.Advanced,
    -> true
    AiExperienceLevel.Pro -> plan != UserQuotaPlan.Free
}

private fun resolvedAgentExperienceLevel(plan: UserQuotaPlan): AiExperienceLevel = when (plan) {
    UserQuotaPlan.Free -> AiExperienceLevel.Standard
    UserQuotaPlan.Creator,
    UserQuotaPlan.Studio,
    UserQuotaPlan.InternalTeam,
    UserQuotaPlan.OwnerUnlimited,
    -> AiExperienceLevel.Pro
}

private fun resolveTerminalPhase(
    decision: com.nash.skyos.data.AgentDecision?,
    hasPendingQueue: Boolean,
): AgentInteractionPhase {
    if (decision?.ownerDiagnosticActive == true) {
        return AgentInteractionPhase.OwnerDiagnostic
    }
    return when (decision?.state.orEmpty()) {
        "planning" -> AgentInteractionPhase.Planning
        "webhook_pending" -> AgentInteractionPhase.WebhookPending
        "external_running" -> AgentInteractionPhase.ExternalRunning
        "awaiting_external_auth" -> AgentInteractionPhase.AwaitingExternalAuth
        "external_failed" -> AgentInteractionPhase.ExternalFailed
        "external_completed" -> AgentInteractionPhase.ExternalCompleted
        "fallback_internal" -> AgentInteractionPhase.FallbackInternal
        "awaiting_confirmation" -> AgentInteractionPhase.AwaitingConfirmation
        "executing" -> AgentInteractionPhase.Executing
        "tool_pending" -> AgentInteractionPhase.ToolPending
        "completed" -> AgentInteractionPhase.Completed
        "partial" -> AgentInteractionPhase.Partial
        "blocked" -> AgentInteractionPhase.Blocked
        "failed" -> AgentInteractionPhase.Failed
        "retryable" -> AgentInteractionPhase.Retryable
        "cancelled" -> AgentInteractionPhase.Cancelled
        "idle" -> if (hasPendingQueue) AgentInteractionPhase.WaitingReconnect else AgentInteractionPhase.Idle
        else -> if (hasPendingQueue) AgentInteractionPhase.WaitingReconnect else AgentInteractionPhase.Completed
    }
}
