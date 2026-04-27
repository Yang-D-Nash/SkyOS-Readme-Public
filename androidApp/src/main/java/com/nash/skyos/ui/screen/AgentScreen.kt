package com.nash.skyos.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.core.text.HtmlCompat
import com.nash.skyos.data.AiMembershipCoordinator
import com.nash.skyos.data.AiMembershipUiState
import com.nash.skyos.data.AgentOutboundAttachment
import com.nash.skyos.data.AgentResultEntry
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.MembershipOpenReason
import com.nash.skyos.R
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.BrandStatusChip
import com.nash.skyos.ui.component.AiConversationSessionStrip
import com.nash.skyos.ui.component.AiConversationSessionsSheet
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.rememberIsCompactAppLayout
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.theme.skydownAccentMystic
import com.nash.skyos.ui.model.AgentAutomationScope
import com.nash.skyos.ui.model.AgentInteractionPhase
import com.nash.skyos.ui.model.AgentExecutionMode
import com.nash.skyos.ui.model.AiExperienceLevel
import com.nash.skyos.ui.model.AgentMessage
import com.nash.skyos.ui.model.AgentMessageRole
import com.nash.skyos.ui.model.AgentResultType
import com.nash.skyos.ui.viewmodel.AgentViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    viewModel: AgentViewModel = viewModel(),
    showTopBar: Boolean = true,
    immersiveInTools: Boolean = false,
) {
    DisposableEffect(immersiveInTools) {
        onDispose { }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val compactLayout = rememberIsCompactAppLayout()
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    val listVerticalSpacing = if (showTopBar) sectionSpacing else if (compactLayout) 8.dp else 10.dp
    val contentMaxWidth = if (compactLayout) 620.dp else 1040.dp
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val composeScope = rememberCoroutineScope()
    val membershipCoordinator = remember(context) { AiMembershipCoordinator(context.applicationContext) }
    val membershipState by membershipCoordinator.uiState.collectAsStateWithLifecycle()
    var inputAttachments by remember { mutableStateOf<List<AgentInputAttachment>>(emptyList()) }
    var showPromptComposer by rememberSaveable { mutableStateOf(false) }
    var showSessionsSheet by rememberSaveable { mutableStateOf(false) }
    var renameDraft by rememberSaveable { mutableStateOf("") }
    val promptSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeSessionSummary = uiState.sessions.firstOrNull { it.sessionId == uiState.activeSessionId }
    val activeSessionSubtitle = when (activeSessionSummary?.promptCount ?: 0) {
        0 -> stringResource(R.string.ai_session_count_new)
        1 -> stringResource(R.string.ai_session_count_one)
        else -> stringResource(R.string.ai_session_count_many, activeSessionSummary?.promptCount ?: 0)
    }
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val mappedAttachments = uris.map { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            resolveAgentInputAttachment(context, uri)
        }
        inputAttachments = (inputAttachments + mappedAttachments)
            .distinctBy { it.id }
            .takeLast(12)
    }
    var localFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var localFeedbackType by remember { mutableStateOf(ToastType.Info) }
    var hasAutoUpgradePrompted by rememberSaveable { mutableStateOf(false) }
    val messageRenderToken = uiState.messages.lastOrNull()?.let { message ->
        val workflowToken = message.workflowSummary?.let { summary ->
            listOf(
                summary.workflowName,
                summary.statusText,
                summary.runId.orEmpty(),
            ).joinToString("|")
        } ?: "no-workflow"
        listOf(
            message.id,
            message.isStreaming.toString(),
            message.resultType.name,
            message.text,
            workflowToken,
            message.results.joinToString("|") { result ->
                listOf(
                    result.type,
                    result.url,
                    result.title,
                    result.text,
                ).joinToString(":")
            },
        ).joinToString("|")
    } ?: "agent-empty"
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }

    LaunchedEffect(messageRenderToken) {
        if (uiState.isAgentEnabled && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAvailability()
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (!message.isNullOrBlank()) {
            if (
                message.contains("Abo", ignoreCase = true) ||
                message.contains("Membership", ignoreCase = true) ||
                message.contains("Pro oder Creator", ignoreCase = true)
            ) {
                membershipCoordinator.trackUpgradeAfterDeny("agent_chat")
                membershipCoordinator.openMembership(MembershipOpenReason.FeatureLocked, surface = "agent_chat")
            }
            delay(3500)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(localFeedbackMessage) {
        if (!localFeedbackMessage.isNullOrBlank()) {
            delay(3000)
            localFeedbackMessage = null
        }
    }
    LaunchedEffect(uiState.activeSessionId, uiState.activeSessionTitle) {
        renameDraft = uiState.activeSessionTitle
    }
    LaunchedEffect(uiState.usageSnapshot?.warningLevel) {
        if (!hasAutoUpgradePrompted && uiState.usageSnapshot?.warningLevel == "critical") {
            hasAutoUpgradePrompted = true
            membershipCoordinator.openMembership(MembershipOpenReason.CriticalUsage, surface = "agent_chat")
        }
    }

    Scaffold(
        modifier = if (showTopBar) {
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier.fillMaxSize()
        }.testTag("agent.screen.root"),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = if (showTopBar) {
            {
                TopAppBar(
                    title = {
                        SkydownTopBarTitle(
                            title = stringResource(R.string.agent_topbar_title),
                            subtitle = stringResource(R.string.agent_topbar_subtitle),
                        )
                    },
                    colors = skydownTopBarColors(),
                    scrollBehavior = scrollBehavior,
                )
            }
        } else {
            {}
        },
        floatingActionButton = {
            if (uiState.isAgentEnabled) {
                AgentPromptFab(
                    isWorking = uiState.agentPhase.shouldBlockComposerChrome,
                    onOpen = { showPromptComposer = true },
                )
            }
        },
    ) { innerPadding ->
        val safeContentPadding = if (showTopBar) {
            skydownContentPadding(innerPadding)
        } else {
            PaddingValues(
                start = SkydownUiTokens.screenHorizontalPadding,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = SkydownUiTokens.screenHorizontalPadding,
                bottom = innerPadding.calculateBottomPadding() + 8.dp,
            )
        }
        val sessionHeaderTopPadding = safeContentPadding.calculateTopPadding()
        val sessionHeaderReservedHeight = 72.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .skydownAtmosphereBackground(
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.skydownAccentMystic(),
                    primaryAlpha = 0.016f,
                    secondaryAlpha = 0.012f,
                )
        ) {
            if (uiState.isAgentEnabled && uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = contentMaxWidth)
                        .fillMaxSize()
                        .padding(
                            start = safeContentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            top = sessionHeaderTopPadding + sessionHeaderReservedHeight,
                            end = safeContentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            bottom = safeContentPadding.calculateBottomPadding() + 78.dp,
                        ),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = contentMaxWidth)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = safeContentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        top = safeContentPadding.calculateTopPadding() + if (uiState.isAgentEnabled) sessionHeaderReservedHeight else 0.dp,
                        end = safeContentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = safeContentPadding.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(listVerticalSpacing),
                ) {
                    if (!uiState.isAgentEnabled) {
                        item {
                            AgentDisabledCard()
                        }
                    } else {
                        items(uiState.messages, key = { it.id }) { message ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 3.dp),
                            ) {
                                AgentMessageBubble(
                                    message = message,
                                    compactLayout = compactLayout,
                                    onFeedback = { messageText, type ->
                                        localFeedbackMessage = messageText
                                        localFeedbackType = type
                                    },
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(78.dp))
                        }
                    }
                }
            }

            if (uiState.isAgentEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = contentMaxWidth)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                ),
                            ),
                        )
                        .padding(
                            start = safeContentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            top = sessionHeaderTopPadding,
                            end = safeContentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            bottom = 10.dp,
                        ),
                ) {
                    AiConversationSessionStrip(
                        title = uiState.activeSessionTitle,
                        subtitle = activeSessionSubtitle,
                        accent = MaterialTheme.colorScheme.tertiary,
                        enabled = !uiState.agentPhase.shouldBlockComposerChrome,
                        canDelete = uiState.activeSessionId != null,
                        showsManagementActions = true,
                        onOpenSessions = { showSessionsSheet = true },
                        onCreateNewChat = viewModel::startNewConversation,
                        onRefreshChat = viewModel::refreshActiveConversation,
                        onDeleteChat = viewModel::deleteActiveConversation,
                    )
                }
            }

            ToastHost(
                message = localFeedbackMessage ?: uiState.errorMessage,
                type = if (localFeedbackMessage != null) localFeedbackType else ToastType.Error,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (uiState.isAgentEnabled) {
                        if (compactLayout) 64.dp else 76.dp
                    } else {
                        28.dp
                    }),
            )

            if (showPromptComposer) {
                ModalBottomSheet(
                    onDismissRequest = { showPromptComposer = false },
                    sheetState = promptSheetState,
                ) {
                    AgentPromptComposerSheet(
                        draft = uiState.draft,
                        selectedMode = uiState.selectedMode,
                        selectedAutomationScope = uiState.selectedAutomationScope,
                        canTriggerAutomation = uiState.canTriggerAutomation,
                        shouldTriggerAutomation = uiState.shouldTriggerAutomation,
                        agentPhase = uiState.agentPhase,
                        attachments = inputAttachments,
                        quickPrompts = uiState.quickPrompts,
                        onDismiss = { showPromptComposer = false },
                        onDraftChanged = viewModel::updateDraft,
                        onModeChanged = viewModel::updateMode,
                        onAutomationScopeChanged = viewModel::updateAutomationScope,
                        onToggleAutomation = viewModel::toggleAutomation,
                        onAddFiles = {
                            showPromptComposer = false
                            attachmentPicker.launch(arrayOf("*/*"))
                        },
                        onRemoveAttachment = { attachment ->
                            inputAttachments = inputAttachments.filterNot { it.id == attachment.id }
                        },
                        onClearAttachments = { inputAttachments = emptyList() },
                        onSend = {
                            val pairs = inputAttachments.map { att ->
                                Uri.parse(att.id) to (att.name to att.kind.toWireKind())
                            }
                            composeScope.launch {
                                val encoded = AgentOutboundAttachment.batchFromUris(context, pairs)
                                viewModel.sendDraft(encoded)
                                inputAttachments = emptyList()
                                dismissKeyboard()
                                showPromptComposer = false
                            }
                        },
                        onReset = {
                            viewModel.startNewConversation()
                            dismissKeyboard()
                            showPromptComposer = false
                        },
                    )
                }
            }

            if (showSessionsSheet) {
                AiConversationSessionsSheet(
                    title = stringResource(R.string.agent_sessions_title),
                    sessions = uiState.sessions,
                    activeSessionId = uiState.activeSessionId,
                    renameDraft = renameDraft,
                    accent = MaterialTheme.colorScheme.tertiary,
                    enabled = !uiState.agentPhase.shouldBlockComposerChrome,
                    onRenameDraftChanged = { renameDraft = it },
                    onDismiss = { showSessionsSheet = false },
                    onSelectSession = { sessionId ->
                        viewModel.openConversation(sessionId)
                        showSessionsSheet = false
                    },
                    onCreateNewChat = {
                        viewModel.startNewConversation()
                        showSessionsSheet = false
                    },
                    onRenameActiveSession = {
                        viewModel.renameActiveConversation(renameDraft)
                        showSessionsSheet = false
                    },
                    onDeleteActiveSession = {
                        viewModel.deleteActiveConversation()
                        showSessionsSheet = false
                    },
                )
            }
        }
        if (membershipState.isOpen) {
            AiMembershipSheet(
                state = membershipState,
                onDismiss = { membershipCoordinator.closeMembership() },
                onToggleAnnual = { membershipCoordinator.setAnnualOption(it) },
                onUpgradePro = {
                    val activity = context as? android.app.Activity ?: return@AiMembershipSheet
                    membershipCoordinator.purchaseSelectedPlan(activity, "Pro") {
                        AppContainer.refreshCurrentUser()
                    }
                },
                onUpgradeCreator = {
                    val activity = context as? android.app.Activity ?: return@AiMembershipSheet
                    membershipCoordinator.purchaseSelectedPlan(activity, "Creator") {
                        AppContainer.refreshCurrentUser()
                    }
                },
                onRestore = {
                    membershipCoordinator.restore { AppContainer.refreshCurrentUser() }
                },
            )
        }
    }
}

private fun agentAiLevelSubtitleResId(level: AiExperienceLevel): Int = when (level) {
    AiExperienceLevel.Standard -> R.string.ai_level_standard_subtitle
    AiExperienceLevel.Advanced -> R.string.ai_level_advanced_subtitle
    AiExperienceLevel.Pro -> R.string.ai_level_pro_subtitle
}

@Composable
private fun AgentRevenueUsageCard(
    usage: com.nash.skyos.data.AiUsageSnapshot?,
    planLabel: String,
    onOpenMembership: () -> Unit,
) {
    SkydownCard(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier.clickable(onClick = onOpenMembership),
    ) {
        Text(
            text = stringResource(R.string.agent_membership_title_short),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = if (usage == null) "${stringResource(R.string.membership_current_plan)}: $planLabel · ${stringResource(R.string.ai_membership_tiers)}" else "${stringResource(R.string.membership_current_plan)}: $planLabel",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (usage == null) {
            Text(
                text = stringResource(R.string.agent_membership_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                modifier = Modifier.padding(top = 6.dp),
            )
            return@SkydownCard
        }
        val used = (usage.limitForKind - usage.remainingForKind).coerceAtLeast(0)
        val progress = if (usage.limitForKind > 0) {
            used.toFloat() / usage.limitForKind.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)
        LinearUsageBar(
            progress = progress,
            isCritical = usage.warningLevel == "critical",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Text(
            text = stringResource(
                R.string.ai_usage_remaining_of_limit,
                usage.remainingForKind,
                usage.limitForKind,
                stringResource(R.string.ai_open),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            modifier = Modifier.padding(top = 4.dp),
        )
        if (usage.warningLevel != "ok") {
            Text(
                text = if (usage.warningLevel == "critical") {
                    stringResource(R.string.agent_warning_critical)
                } else {
                    stringResource(R.string.agent_warning_high)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
        if (usage.userFacingReason.isNotBlank()) {
            Text(
                text = usage.userFacingReason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (usage.lowerCostOption.isNotBlank()) {
            Text(
                text = usage.lowerCostOption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (usage.retryAfterSeconds > 0) {
            Text(
                text = stringResource(R.string.ai_retry_in_seconds, usage.retryAfterSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
        if (usage.suggestedUpgrade.isNotBlank()) {
            Text(
                text = stringResource(R.string.ai_upgrade_hint_value, usage.suggestedUpgrade.uppercase()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (usage.resetHint.isNotBlank()) {
            Text(
                text = usage.resetHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun LinearUsageBar(
    progress: Float,
    isCritical: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(RoundedCornerShape(999.dp))
                .background(if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiMembershipSheet(
    state: AiMembershipUiState,
    onDismiss: () -> Unit,
    onToggleAnnual: (Boolean) -> Unit,
    onUpgradePro: () -> Unit,
    onUpgradeCreator: () -> Unit,
    onRestore: () -> Unit,
) {
    val proProduct = state.products.firstOrNull { it.planLabel == "Pro" }
    val creatorProduct = state.products.firstOrNull { it.planLabel == "Creator" }
    val proAvailable = proProduct?.let { it.monthly != null || it.yearly != null } == true
    val creatorAvailable = creatorProduct?.let { it.monthly != null || it.yearly != null } == true

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.ai_membership_sheet_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.ai_membership_sheet_subtitle), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.agent_membership_trigger, state.reason.name.lowercase()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(stringResource(R.string.ai_membership_free_detail), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.ai_membership_pro_detail), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.ai_membership_creator_detail), style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.membership_annual_option), style = MaterialTheme.typography.labelMedium)
                androidx.compose.material3.Switch(
                    checked = state.selectedAnnual,
                    onCheckedChange = onToggleAnnual,
                )
            }
            if (state.annualDiscountCopy.isNotBlank()) {
                Text(state.annualDiscountCopy, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            }
            Button(onClick = onUpgradePro, modifier = Modifier.fillMaxWidth(), enabled = proAvailable && !state.isPurchasing) {
                Text(stringResource(R.string.membership_pro_activate))
            }
            Button(onClick = onUpgradeCreator, modifier = Modifier.fillMaxWidth(), enabled = creatorAvailable && !state.isPurchasing) {
                Text(stringResource(R.string.membership_creator_activate))
            }
            OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.membership_restore))
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.membership_decide_later))
            }
            if (!state.isLoading && state.products.isEmpty() && state.errorMessage.isBlank()) {
                Text(
                    text = stringResource(R.string.agent_membership_billing_unavailable),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.successMessage.isNotBlank()) {
                Text(state.successMessage, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            if (state.errorMessage.isNotBlank()) {
                Text(state.errorMessage, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AgentEmptyStateHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.agent_empty_header_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.agent_empty_header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun AgentDisabledCard() {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.agent_paused_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            BrandStatusChip(
                text = stringResource(R.string.common_status_idle),
                accent = MaterialTheme.colorScheme.tertiary,
                isActive = true,
            )
        }
        Text(
            text = stringResource(R.string.agent_paused_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun AgentPromptFab(
    isWorking: Boolean,
    onOpen: () -> Unit,
) {
    FloatingActionButton(
        onClick = onOpen,
        modifier = Modifier
            .widthIn(min = 132.dp)
            .height(58.dp),
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isWorking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = stringResource(R.string.agent_prompt_open),
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = if (isWorking) {
                    stringResource(R.string.agent_fab_working)
                } else {
                    stringResource(R.string.agent_topbar_title)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            if (!isWorking) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentPromptComposerSheet(
    draft: String,
    selectedMode: AgentExecutionMode,
    selectedAutomationScope: AgentAutomationScope,
    canTriggerAutomation: Boolean,
    shouldTriggerAutomation: Boolean,
    agentPhase: AgentInteractionPhase,
    attachments: List<AgentInputAttachment>,
    quickPrompts: List<String>,
    onDismiss: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onModeChanged: (AgentExecutionMode) -> Unit,
    onAutomationScopeChanged: (AgentAutomationScope) -> Unit,
    onToggleAutomation: () -> Unit,
    onAddFiles: () -> Unit,
    onRemoveAttachment: (AgentInputAttachment) -> Unit,
    onClearAttachments: () -> Unit,
    onSend: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom))
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.agent_prompt_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.agent_prompt_sheet_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BrandStatusChip(
                text = selectedMode.title,
                accent = MaterialTheme.colorScheme.tertiary,
                isActive = true,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.agent_prompt_close),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        agentPhase.composerStatusLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Text(
            text = stringResource(R.string.agent_prompt_options),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.tertiary,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AgentModeMenu(
                selectedMode = selectedMode,
                enabled = !agentPhase.shouldBlockComposerChrome,
                onModeChanged = onModeChanged,
            )
            if (canTriggerAutomation) {
                AgentAutomationScopeMenu(
                    selectedScope = selectedAutomationScope,
                    enabled = !agentPhase.shouldBlockComposerChrome,
                    onScopeChanged = onAutomationScopeChanged,
                )
                AgentAutomationTriggerButton(
                    isEnabled = shouldTriggerAutomation,
                    onToggle = onToggleAutomation,
                )
            }
        }

        AgentQuickPromptCard(
            prompts = quickPrompts,
            onPromptSelected = onDraftChanged,
        )

        Text(
            text = stringResource(R.string.agent_prompt_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.tertiary,
        )

        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(selectedMode.placeholder)
            },
            minLines = 4,
            maxLines = 8,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                cursorColor = MaterialTheme.colorScheme.tertiary,
            ),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAddFiles) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = stringResource(R.string.agent_files_add),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = if (attachments.isEmpty()) {
                    stringResource(R.string.agent_files_none)
                } else {
                    stringResource(R.string.agent_files_count, attachments.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
        }

        if (attachments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                attachments.forEach { attachment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = attachment.kind.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = attachment.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = { onRemoveAttachment(attachment) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.common_remove),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                TextButton(onClick = onClearAttachments) {
                    Text(stringResource(R.string.agent_files_remove_all))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onReset,
                enabled = !agentPhase.shouldBlockComposerChrome,
            ) {
                Text(stringResource(R.string.agent_action_new_chat))
            }
            BrandActionButton(
                text = stringResource(R.string.agent_action_send),
                onClick = onSend,
                accent = MaterialTheme.colorScheme.tertiary,
                icon = Icons.AutoMirrored.Filled.Send,
                filled = true,
                compact = true,
                enabled = draft.isNotBlank() && !agentPhase.shouldBlockSend,
                isLoading = agentPhase.shouldBlockComposerChrome,
            )
        }
    }
}

@Composable
private fun AgentQuickPromptCard(
    prompts: List<String>,
    onPromptSelected: (String) -> Unit,
) {
    if (prompts.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandStatusChip(
                text = stringResource(R.string.agent_prompts_chip),
                accent = MaterialTheme.colorScheme.tertiary,
                isActive = true,
            )
            Text(
                text = stringResource(R.string.agent_prompts_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(prompts, key = { it }) { prompt ->
                Column(
                    modifier = Modifier
                        .widthIn(min = 220.dp, max = 260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .clickable { onPromptSelected(prompt) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BrandStatusChip(
                        text = stringResource(R.string.agent_prompts_apply),
                        accent = MaterialTheme.colorScheme.tertiary,
                        isActive = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentModeMenu(
    selectedMode: AgentExecutionMode,
    enabled: Boolean,
    onModeChanged: (AgentExecutionMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Text(selectedMode.title)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AgentExecutionMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.title) },
                    onClick = {
                        onModeChanged(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AgentLevelMenu(
    selectedLevel: AiExperienceLevel,
    enabled: Boolean,
    onLevelChanged: (AiExperienceLevel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Text(selectedLevel.title)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AiExperienceLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.title) },
                    onClick = {
                        onLevelChanged(level)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AgentAutomationScopeMenu(
    selectedScope: AgentAutomationScope,
    enabled: Boolean,
    onScopeChanged: (AgentAutomationScope) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Text(selectedScope.title)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AgentAutomationScope.entries.forEach { scope ->
                DropdownMenuItem(
                    text = { Text(scope.title) },
                    onClick = {
                        onScopeChanged(scope)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AgentAutomationTriggerButton(
    isEnabled: Boolean,
    onToggle: () -> Unit,
) {
    val accent = if (isEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(22.dp)
    OutlinedButton(
        onClick = onToggle,
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp),
        shape = shape,
        modifier = Modifier.height(54.dp),
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = if (isEnabled) {
                    stringResource(R.string.agent_workflow_active)
                } else {
                    stringResource(R.string.agent_workflow_start)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
            )
            Text(
                text = if (isEnabled) {
                    stringResource(R.string.agent_workflow_attached)
                } else {
                    stringResource(R.string.agent_workflow_provider)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                maxLines = 1,
            )
        }
    }
}

private enum class AgentInputAttachmentKind(
    val icon: ImageVector,
) {
    Text(Icons.Default.Bolt),
    Video(Icons.Default.Movie),
    Audio(Icons.Default.MusicNote),
    Image(Icons.Default.Photo),
    Document(Icons.Default.Refresh),
    File(Icons.Default.Refresh),
}

private fun AgentInputAttachmentKind.toWireKind(): String =
    when (this) {
        AgentInputAttachmentKind.Text -> "text"
        AgentInputAttachmentKind.Video -> "video"
        AgentInputAttachmentKind.Audio -> "audio"
        AgentInputAttachmentKind.Image -> "image"
        AgentInputAttachmentKind.Document -> "document"
        AgentInputAttachmentKind.File -> "file"
    }

private data class AgentInputAttachment(
    val id: String,
    val name: String,
    val kind: AgentInputAttachmentKind,
)

private fun resolveAgentInputAttachment(
    context: Context,
    uri: Uri,
): AgentInputAttachment {
    val displayName = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment.orEmpty().ifBlank { "file" }
    val mimeType = context.contentResolver.getType(uri).orEmpty()
    val extension = displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    val kind = when {
        mimeType.startsWith("video/") || extension in setOf("mp4", "mov", "m4v", "avi", "mkv", "webm") -> AgentInputAttachmentKind.Video
        mimeType.startsWith("audio/") || extension in setOf("mp3", "wav", "m4a", "aac", "flac", "aiff") -> AgentInputAttachmentKind.Audio
        mimeType.startsWith("image/") || extension in setOf("png", "jpg", "jpeg", "webp", "heic", "gif") -> AgentInputAttachmentKind.Image
        mimeType.startsWith("text/") || extension in setOf("txt", "md", "rtf", "json", "csv", "xml", "html") -> AgentInputAttachmentKind.Text
        extension in setOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx") -> AgentInputAttachmentKind.Document
        else -> AgentInputAttachmentKind.File
    }
    return AgentInputAttachment(
        id = uri.toString(),
        name = displayName,
        kind = kind,
    )
}

@Composable
private fun AgentMessageBubble(
    message: AgentMessage,
    compactLayout: Boolean,
    onFeedback: (String, ToastType) -> Unit,
) {
    val context = LocalContext.current
    val isUser = message.role == AgentMessageRole.User
    val bubbleShape = RoundedCornerShape(
        topStart = SkydownUiTokens.cardCornerRadius,
        topEnd = SkydownUiTokens.cardCornerRadius,
        bottomStart = if (isUser) SkydownUiTokens.cardCornerRadius else 18.dp,
        bottomEnd = if (isUser) 18.dp else SkydownUiTokens.cardCornerRadius,
    )
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    } else {
        Color.Transparent
    }
    val bubbleBorderColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    val copiedFeedback = stringResource(R.string.agent_feedback_copied)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .border(
                    width = if (isUser) 1.dp else 0.dp,
                    color = bubbleBorderColor,
                    shape = bubbleShape,
                )
                .padding(
                    horizontal = if (compactLayout) 12.dp else 14.dp,
                    vertical = if (compactLayout) 10.dp else 12.dp,
                ),
        ) {
            Text(
                text = if (isUser) {
                    stringResource(R.string.ai_user_label_you)
                } else {
                    stringResource(R.string.agent_workspace_hero_title)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )

            if (message.resultType == AgentResultType.Progress || (message.isStreaming && message.text.isBlank())) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(R.string.agent_streaming_status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            } else {
                if (!isUser && message.workflowSummary != null) {
                    AgentWorkflowResultCard(
                        summary = message.workflowSummary,
                        isError = message.resultType == AgentResultType.Error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (!isUser) {
                    AgentStructuredResults(
                        results = message.results,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (!isUser && !message.isStreaming) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                copyAiText(context, "SkyOS Agent", message.text)
                                onFeedback(copiedFeedback, ToastType.Success)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.agent_action_copy))
                        }

                        OutlinedButton(
                            onClick = {
                                shareAiText(context, "SkyOS Agent", message.text)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.agent_action_share))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentStructuredResults(
    results: List<AgentResultEntry>,
    modifier: Modifier = Modifier,
) {
    val visibleResults = results.filter { result ->
        val kind = result.agentOutputKind()
        kind != "text" && kind != "workflow" && result.hasVisibleAgentOutput()
    }
    if (visibleResults.isNotEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            visibleResults.forEach { result ->
                when (result.agentOutputKind()) {
                    "image" -> AgentImageResultCard(result)
                    "video" -> AgentVideoResultCard(result)
                    "audio" -> AgentAudioResultCard(result)
                    "file" -> AgentFileResultCard(result)
                    "link" -> AgentLinkResultCard(result)
                    "table" -> AgentTableResultCard(result)
                    "html" -> AgentHtmlResultCard(result)
                    else -> AgentFallbackResultCard(result)
                }
            }
        }
    }
}

@Composable
private fun AgentResultShell(
    result: AgentResultEntry,
    fallbackTitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(15.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = result.agentDisplayTitle(fallbackTitle),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = result.agentSubtitle(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        content()
    }
}

@Composable
private fun AgentImageResultCard(result: AgentResultEntry) {
    AgentResultShell(
        result = result,
        fallbackTitle = stringResource(R.string.agent_result_image),
        icon = Icons.Default.Photo,
    ) {
        val url = result.url.trim()
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = result.agentDisplayTitle(stringResource(R.string.agent_result_image)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                contentScale = ContentScale.Crop,
            )
        } else {
            AgentFallbackResultText(result)
        }
    }
}

@Composable
private fun AgentVideoResultCard(result: AgentResultEntry) {
    AgentResultShell(
        result = result,
        fallbackTitle = stringResource(R.string.agent_result_video),
        icon = Icons.Default.Movie,
    ) {
        val url = result.url.trim()
        if (url.isNotBlank()) {
            AgentInlineMediaPlayer(
                url = url,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp)),
            )
        } else {
            AgentFallbackResultText(result)
        }
    }
}

@Composable
private fun AgentAudioResultCard(result: AgentResultEntry) {
    AgentResultShell(
        result = result,
        fallbackTitle = stringResource(R.string.agent_result_audio),
        icon = Icons.Default.MusicNote,
    ) {
        val url = result.url.trim()
        if (url.isNotBlank()) {
            AgentInlineMediaPlayer(
                url = url,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
        } else {
            AgentFallbackResultText(result)
        }
    }
}

@Composable
private fun AgentInlineMediaPlayer(
    url: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }
    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { playerContext ->
            PlayerView(playerContext).apply {
                useController = true
                this.player = player
            }
        },
        update = { view ->
            view.player = player
        },
    )
}

@Composable
private fun AgentFileResultCard(result: AgentResultEntry) {
    AgentResultShell(
        result = result,
        fallbackTitle = stringResource(R.string.agent_result_file),
        icon = Icons.Default.AttachFile,
    ) {
        AgentOpenResultButton(result = result, label = stringResource(R.string.agent_action_open))
    }
}

@Composable
private fun AgentLinkResultCard(result: AgentResultEntry) {
    AgentResultShell(
        result = result,
        fallbackTitle = stringResource(R.string.agent_result_link),
        icon = Icons.Default.Link,
    ) {
        AgentOpenResultButton(
            result = result,
            label = result.text.trim().ifBlank { stringResource(R.string.agent_action_open_link) },
        )
    }
}

@Composable
private fun AgentOpenResultButton(
    result: AgentResultEntry,
    label: String,
) {
    val context = LocalContext.current
    val url = result.url.trim()
    Button(
        onClick = { openExternalLink(context, url) },
        enabled = url.isNotBlank(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun AgentTableResultCard(result: AgentResultEntry) {
    val columnCount = maxOf(
        result.columns.size,
        result.rows.maxOfOrNull { it.size } ?: 0,
        1,
    )
    val columns = result.columns.ifEmpty {
        List(columnCount.coerceAtMost(8)) { index ->
            stringResource(R.string.agent_table_column_fallback, index + 1)
        }
    }

    AgentResultShell(
        result = result,
        fallbackTitle = stringResource(R.string.agent_result_table),
        icon = Icons.Default.TableChart,
    ) {
        if (result.rows.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                    ),
            ) {
                AgentTableResultRow(cells = columns, isHeader = true)
                result.rows.forEach { row ->
                    AgentTableResultRow(cells = row, isHeader = false)
                }
            }
        } else {
            AgentFallbackResultText(result)
        }
    }
}

@Composable
private fun AgentTableResultRow(
    cells: List<String>,
    isHeader: Boolean,
) {
    Row {
        cells.forEach { cell ->
            Text(
                text = cell.ifBlank { "-" },
                style = if (isHeader) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(122.dp)
                    .background(
                        if (isHeader) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
                        },
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun AgentHtmlResultCard(result: AgentResultEntry) {
    AgentResultShell(
        result = result,
        fallbackTitle = stringResource(R.string.agent_result_html),
        icon = Icons.Default.Code,
    ) {
        val html = result.html.trim()
        if (html.isNotBlank()) {
            val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
                    .padding(10.dp),
                factory = { viewContext ->
                    TextView(viewContext).apply {
                        setTextColor(textColor)
                        textSize = 14f
                    }
                },
                update = { textView ->
                    textView.setTextColor(textColor)
                    textView.text = HtmlCompat.fromHtml(
                        html.replace(Regex("(?is)<script.*?</script>"), ""),
                        HtmlCompat.FROM_HTML_MODE_COMPACT,
                    )
                },
            )
        } else {
            AgentFallbackResultText(result)
        }
    }
}

@Composable
private fun AgentFallbackResultCard(result: AgentResultEntry) {
    AgentResultShell(
        result = result,
        fallbackTitle = stringResource(R.string.agent_result_output),
        icon = Icons.Default.Bolt,
    ) {
        AgentFallbackResultText(result)
    }
}

@Composable
private fun AgentFallbackResultText(result: AgentResultEntry) {
    Text(
        text = result.agentFallbackText(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .padding(10.dp),
    )
}

private fun AgentResultEntry.agentOutputKind(): String {
    return when (type.trim().lowercase()) {
        "url", "button" -> "link"
        "pdf", "document", "download" -> "file"
        "" -> "text"
        else -> type.trim().lowercase()
    }
}

private fun AgentResultEntry.hasVisibleAgentOutput(): Boolean {
    return url.isNotBlank() ||
        text.isNotBlank() ||
        title.isNotBlank() ||
        fileName.isNotBlank() ||
        html.isNotBlank() ||
        rows.isNotEmpty()
}

private fun AgentResultEntry.agentDisplayTitle(fallback: String): String {
    return listOf(title, fileName, workflowName, url)
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?: fallback
}

private fun AgentResultEntry.agentSubtitle(): String {
    if (mimeType.isNotBlank()) return mimeType.trim()
    val host = runCatching { Uri.parse(url).host }.getOrNull()
    if (!host.isNullOrBlank()) return host
    return agentOutputKind().uppercase()
}

private fun AgentResultEntry.agentFallbackText(): String {
    return listOf(text, summary, url)
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?: agentDisplayTitle("Output ready.")
}

@Composable
private fun AgentWorkflowResultCard(
    summary: com.nash.skyos.ui.model.AgentWorkflowSummary?,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    if (summary == null) return
    val accent = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Refresh else Icons.Default.Bolt,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = summary.workflowName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = summary.statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
        )
        summary.progressPercent?.let { progress ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = progress.coerceIn(0, 100) / 100f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
            }
        }
        if (summary.step.isNotBlank()) {
            Text(
                text = "Step: ${summary.step}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )
        }
        summary.etaSeconds?.takeIf { it > 0 }?.let { eta ->
            Text(
                text = "ETA: ${eta}s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )
        }
        if (summary.details.isNotBlank()) {
            Text(
                text = summary.details,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        summary.runId?.takeIf { it.isNotBlank() }?.let { runId ->
            Text(
                text = stringResource(R.string.agent_workflow_run_id, runId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}
