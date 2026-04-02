package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AiConversationHistorySource
import com.skydown.android.data.AiConversationHistoryStore
import com.skydown.android.data.AiUsageAuthorizationKind
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.data.AiVisualReferenceLibraryPreferences
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ai.type.FinishReason
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.ResponseStoppedException
import com.skydown.shared.model.User
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.resolvedAiHistoryRetentionDays
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
    private val aiUsageAuthorizationClient = AppContainer.aiUsageAuthorizationClient
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
                it.copy(errorMessage = "Der Skydown x 22 Bot ist gerade deaktiviert.")
            }
            return
        }
        if (trimmedPrompt.isBlank() || _uiState.value.isSending) return
        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        viewModelScope.launch {
            val responseBuffer = StringBuilder()
            val assistantMessage = AiMessage(
                role = AiMessageRole.Assistant,
                text = "",
                isStreaming = true,
            )
            val assistantMessageId = assistantMessage.id

            runCatching {
                val authorization = aiUsageAuthorizationClient.authorize(AiUsageAuthorizationKind.Text)
                AiConversationHistoryStore.updateRetentionDays(authorization.historyRetentionDays)

                val userMessage = AiMessage(
                    role = AiMessageRole.User,
                    text = trimmedPrompt,
                )
                val history = buildHistoryContext()

                _uiState.update {
                    it.copy(
                        draft = "",
                        isSending = true,
                        errorMessage = null,
                        messages = it.messages + userMessage + assistantMessage,
                    )
                }

                aiChatClient.createChat().sendMessageStream(
                    buildPrompt(
                        userPrompt = trimmedPrompt,
                        history = history,
                    ),
                ).collect { response ->
                    val chunk = response.text.orEmpty()
                    if (chunk.isNotBlank()) {
                        responseBuffer.append(chunk)
                        updateAssistantMessage(
                            messageId = assistantMessageId,
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
                    messageId = assistantMessageId,
                    text = finalText,
                    isStreaming = false,
                )
                AiConversationHistoryStore.saveEntry(
                    userKey = currentUserKey,
                    source = AiConversationHistorySource.Bot,
                    prompt = trimmedPrompt,
                    response = finalText,
                )
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { error ->
                val partialText = responseBuffer.toString().trim()
                val assistantText = assistantMessageText(
                    error = error,
                    partialText = partialText,
                )
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
                        isSending = false,
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
                it.copy(errorMessage = "Der Skydown x 22 Bot ist gerade deaktiviert.")
            }
            return
        }
        if (trimmedPrompt.isBlank() || _uiState.value.isSending) return
        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        viewModelScope.launch {
            val assistantMessage = AiMessage(
                role = AiMessageRole.Assistant,
                text = "",
                isStreaming = true,
            )
            val assistantMessageId = assistantMessage.id
            runCatching {
                val authorization = aiUsageAuthorizationClient.authorize(AiUsageAuthorizationKind.Visual)
                AiConversationHistoryStore.updateRetentionDays(authorization.historyRetentionDays)

                val userMessage = AiMessage(
                    role = AiMessageRole.User,
                    text = trimmedPrompt,
                )

                _uiState.update {
                    it.copy(
                        draft = "",
                        isSending = true,
                        errorMessage = null,
                        messages = it.messages + userMessage + assistantMessage,
                    )
                }

                aiImageClient.generateVisual(buildVisualPrompt(trimmedPrompt))
            }.onSuccess { result ->
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
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { error ->
                val assistantText = assistantMessageText(
                    error = error,
                    partialText = "",
                )
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
                        isSending = false,
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
    private fun buildPrompt(userPrompt: String, history: String): String {
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

        Bisheriger Verlauf:
        $history

        Ausgabeformat:
        $formatHint

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
        val referenceContext = AiVisualReferenceLibraryPreferences.promptContext()
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

            ${referenceContext ?: ""}

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
        is FirebaseFunctionsException -> when (error.code) {
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Dein heutiges KI-Limit ist erreicht."
            FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Die KI ist fuer dein Konto gerade nicht freigeschaltet."
            FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                "Bitte melde dich erneut an und versuch es noch einmal."
            FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                "Die KI-Anfrage konnte so nicht gestartet werden."
            else ->
                "Der Skydown x 22 Bot ist gerade nicht verfuegbar."
        }
        is ResponseStoppedException -> finishReasonMessage(
            error.response.candidates.firstOrNull()?.finishReason,
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
