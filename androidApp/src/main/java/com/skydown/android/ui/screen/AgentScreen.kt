package com.skydown.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.data.AiMembershipCoordinator
import com.skydown.android.data.AiMembershipUiState
import com.skydown.android.data.AppContainer
import com.skydown.android.data.MembershipOpenReason
import com.skydown.android.R
import com.skydown.android.ui.component.BrandStatusChip
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.SkydownUiTokens
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.rememberSkydownScreenSectionSpacing
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.rememberIsCompactAppLayout
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.theme.skydownAccentMystic
import com.skydown.android.ui.model.AgentInteractionPhase
import com.skydown.android.ui.model.AgentExecutionMode
import com.skydown.android.ui.model.AiExperienceLevel
import com.skydown.android.ui.model.AgentMessage
import com.skydown.android.ui.model.AgentMessageRole
import com.skydown.android.ui.model.AgentResultType
import com.skydown.android.ui.viewmodel.AgentViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    viewModel: AgentViewModel = viewModel(),
    showTopBar: Boolean = true,
    immersiveInTools: Boolean = false,
) {
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
    val membershipCoordinator = remember(context) { AiMembershipCoordinator(context.applicationContext) }
    val membershipState by membershipCoordinator.uiState.collectAsStateWithLifecycle()
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
        ).joinToString("|")
    } ?: "agent-empty"
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }

    LaunchedEffect(messageRenderToken) {
        if (uiState.isAgentEnabled && uiState.messages.isNotEmpty()) {
            // usage + workspace & thread lead + messages; scroll to end spacer
            listState.animateScrollToItem(uiState.messages.lastIndex + 3)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAvailability()
    }

    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrBlank()) {
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
        bottomBar = {
            if (uiState.isAgentEnabled) {
                AgentComposerBar(
                    draft = uiState.draft,
                    selectedMode = uiState.selectedMode,
                    selectedLevel = uiState.selectedLevel,
                    canTriggerAutomation = uiState.canTriggerAutomation,
                    shouldTriggerAutomation = uiState.shouldTriggerAutomation,
                    agentPhase = uiState.agentPhase,
                    compactLayout = compactLayout,
                    contentMaxWidth = contentMaxWidth,
                    embeddedInTools = !showTopBar,
                    showDockClearance = !showTopBar && !immersiveInTools,
                    applyBottomSystemInset = showTopBar || immersiveInTools,
                    onDraftChanged = viewModel::updateDraft,
                    onModeChanged = viewModel::updateMode,
                    onLevelChanged = viewModel::updateLevel,
                    onToggleAutomation = viewModel::toggleAutomation,
                    onSend = {
                        viewModel.sendDraft()
                        dismissKeyboard()
                    },
                    onReset = {
                        viewModel.resetConversation()
                        dismissKeyboard()
                    },
                    onDismissKeyboard = dismissKeyboard,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.skydownAccentMystic(),
                        primaryAlpha = 0.028f,
                        secondaryAlpha = 0.018f,
                    ),
                )
        ) {
            if (uiState.isAgentEnabled && uiState.messages.isEmpty()) {
                val agentStatusLine = stringResource(
                    if (uiState.lastProviderNotice.trim().isNotEmpty()) {
                        R.string.agent_workspace_status_line_adjusted
                    } else {
                        R.string.agent_workspace_status_line_ready
                    },
                )
                Column(
                    modifier = Modifier
                        .widthIn(max = contentMaxWidth)
                        .fillMaxSize()
                        .padding(safeContentPadding),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    AgentWorkspaceHeroCard(
                        isEnabled = true,
                        statusLine = agentStatusLine,
                    )
                    AgentEmptyStateHeader()
                    AgentRevenueUsageCard(
                        usage = uiState.usageSnapshot,
                        planLabel = uiState.planLabel,
                        onOpenMembership = {
                            if (uiState.usageSnapshot?.userFacingReason?.isNotBlank() == true) {
                                membershipCoordinator.trackUpgradeAfterDeny(surface = "agent_empty")
                            }
                            membershipCoordinator.openMembership(MembershipOpenReason.Manual, surface = "agent_empty")
                        },
                    )
                    AgentQuickPromptCard(
                        prompts = uiState.quickPrompts,
                        onPromptSelected = { prompt ->
                            dismissKeyboard()
                            viewModel.sendPrompt(prompt)
                        },
                        compactLayout = compactLayout,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .widthIn(max = contentMaxWidth)
                        .fillMaxSize(),
                    contentPadding = safeContentPadding,
                    verticalArrangement = Arrangement.spacedBy(listVerticalSpacing),
                ) {
                    if (!uiState.isAgentEnabled) {
                        item {
                            AgentDisabledCard()
                        }
                    } else {
                        item {
                            AgentRevenueUsageCard(
                                usage = uiState.usageSnapshot,
                                planLabel = uiState.planLabel,
                                onOpenMembership = {
                                    if (uiState.usageSnapshot?.userFacingReason?.isNotBlank() == true) {
                                        membershipCoordinator.trackUpgradeAfterDeny(surface = "agent_chat")
                                    }
                                    membershipCoordinator.openMembership(MembershipOpenReason.Manual, surface = "agent_chat")
                                },
                            )
                        }
                        item {
                            val line = stringResource(
                                if (uiState.lastProviderNotice.trim().isNotEmpty()) {
                                    R.string.agent_workspace_status_line_adjusted
                                } else {
                                    R.string.agent_workspace_status_line_ready
                                },
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                AgentWorkspaceContextCard(
                                    selectedMode = uiState.selectedMode,
                                    messageCount = uiState.messages.size,
                                    agentPhase = uiState.agentPhase,
                                    providerLine = line,
                                )
                                AgentConversationLeadIn(
                                    showHairline = true,
                                )
                            }
                        }
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
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
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
    usage: com.skydown.android.data.AiUsageSnapshot?,
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
            text = "${usage.remainingForKind}/${usage.limitForKind} ${stringResource(R.string.ai_open)}",
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
                    .clip(RoundedCornerShape(10.dp))
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
                text = "${stringResource(R.string.ai_retry_in)} ${usage.retryAfterSeconds}s.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
        if (usage.suggestedUpgrade.isNotBlank()) {
            Text(
                text = "${stringResource(R.string.ai_upgrade_hint)}: ${usage.suggestedUpgrade.uppercase()}",
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
                "Ausloeser: ${state.reason.name.lowercase()}",
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
                    text = "Play Billing ist fuer dieses Build noch nicht live konfiguriert.",
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
            text = stringResource(R.string.agent_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.agent_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Text(
            text = stringResource(R.string.agent_empty_first_step),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun AgentConversationLeadIn(
    showHairline: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showHairline) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colorScheme.tertiary.copy(alpha = 0.16f),
                                colorScheme.tertiary.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
        Text(
            text = stringResource(R.string.agent_section_conversation),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.agent_section_conversation_sub),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.64f),
        )
    }
}

@Composable
private fun AgentWorkspaceHeroCard(
    isEnabled: Boolean,
    statusLine: String,
) {
    val scheme = MaterialTheme.colorScheme
    val heroTint = if (isEnabled) scheme.tertiary else scheme.onSurface.copy(alpha = 0.5f)
    SkydownCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                heroTint.copy(alpha = 0.42f),
                                heroTint.copy(alpha = 0.12f),
                            ),
                            center = Offset(25f, 25f),
                            radius = 36f,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = heroTint,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.agent_workspace_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isEnabled) {
                        stringResource(R.string.agent_workspace_hero_status_on)
                    } else {
                        stringResource(R.string.agent_workspace_hero_status_off)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isEnabled) {
                        scheme.tertiary
                    } else {
                        scheme.onSurface.copy(alpha = 0.65f)
                    },
                )
                Text(
                    text = stringResource(R.string.agent_workspace_hero_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Text(
                    text = statusLine,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.92f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentWorkspaceContextCard(
    selectedMode: AgentExecutionMode,
    messageCount: Int,
    agentPhase: AgentInteractionPhase,
    providerLine: String,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isBusy = agentPhase.shouldBlockComposerChrome
    val isError = when (agentPhase) {
        AgentInteractionPhase.Failed, AgentInteractionPhase.ExternalFailed, AgentInteractionPhase.Blocked -> true
        else -> false
    }
    val isSettledSuccess = when (agentPhase) {
        AgentInteractionPhase.Completed, AgentInteractionPhase.ExternalCompleted -> true
        else -> false
    }
    val (phaseAccent, phaseIsActive) = when (agentPhase) {
        AgentInteractionPhase.Failed, AgentInteractionPhase.ExternalFailed, AgentInteractionPhase.Blocked -> {
            colorScheme.error to true
        }
        AgentInteractionPhase.Partial, AgentInteractionPhase.Retryable, AgentInteractionPhase.Cancelled,
        -> colorScheme.tertiary to true
        AgentInteractionPhase.Idle -> colorScheme.tertiary to false
        else -> colorScheme.tertiary to true
    }
    val phaseText = agentPhase.composerStatusLabel
        ?: stringResource(R.string.agent_workspace_status_ready)
    val borderWidth = when {
        isError || isBusy -> 1.5.dp
        isSettledSuccess -> 1.dp
        else -> 1.dp
    }
    val borderColor = when {
        isError -> colorScheme.error.copy(alpha = 0.5f)
        isBusy -> colorScheme.tertiary.copy(alpha = 0.52f)
        isSettledSuccess -> colorScheme.tertiary.copy(alpha = 0.32f)
        else -> colorScheme.tertiary.copy(alpha = 0.10f)
    }
    val barBrush = when {
        isError -> Brush.verticalGradient(
            listOf(colorScheme.error, colorScheme.error.copy(alpha = 0.25f)),
        )
        isBusy -> Brush.verticalGradient(
            listOf(colorScheme.tertiary, colorScheme.tertiary.copy(alpha = 0.35f)),
        )
        isSettledSuccess -> Brush.verticalGradient(
            listOf(colorScheme.tertiary.copy(alpha = 0.85f), colorScheme.tertiary.copy(alpha = 0.2f)),
        )
        agentPhase == AgentInteractionPhase.Partial -> Brush.verticalGradient(
            listOf(colorScheme.tertiary.copy(alpha = 0.6f), colorScheme.tertiary.copy(alpha = 0.15f)),
        )
        else -> Brush.verticalGradient(
            listOf(colorScheme.tertiary.copy(alpha = 0.3f), colorScheme.tertiary.copy(alpha = 0.1f)),
        )
    }
    val cardShape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius)
    SkydownCard(
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.border(borderWidth, borderColor, cardShape),
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(barBrush),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.agent_workspace_context_title).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = providerLine,
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.tertiary.copy(alpha = 0.92f),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BrandStatusChip(
                        text = stringResource(R.string.agent_workspace_mode_format, selectedMode.title),
                        accent = colorScheme.tertiary,
                        isActive = true,
                    )
                    BrandStatusChip(
                        text = stringResource(R.string.agent_workspace_session_format, messageCount),
                        accent = colorScheme.tertiary,
                        isActive = true,
                    )
                    BrandStatusChip(
                        text = phaseText,
                        accent = phaseAccent,
                        isActive = phaseIsActive,
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentDisabledCard() {
    val shape = RoundedCornerShape(16.dp)
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
private fun AgentQuickPromptCard(
    prompts: List<String>,
    onPromptSelected: (String) -> Unit,
    compactLayout: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandStatusChip(
                text = "Agent",
                accent = MaterialTheme.colorScheme.tertiary,
                isActive = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Direkt starten",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Starte mit einer klaren Aufgabe und hole dir sofort Struktur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(prompts) { prompt ->
                OutlinedButton(
                    onClick = { onPromptSelected(prompt) },
                    modifier = Modifier.widthIn(min = 190.dp, max = 236.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = if (compactLayout) 8.dp else 10.dp,
                    ),
                    shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                ) {
                    Text(
                        text = prompt,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
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
        bottomStart = if (isUser) SkydownUiTokens.cardCornerRadius else 10.dp,
        bottomEnd = if (isUser) 10.dp else SkydownUiTokens.cardCornerRadius,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(bubbleShape)
                .background(
                    if (isUser) {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                            ),
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            ),
                        )
                    },
                )
                .padding(
                    horizontal = if (compactLayout) 12.dp else 14.dp,
                    vertical = if (compactLayout) 10.dp else 12.dp,
                ),
        ) {
            Text(
                text = if (isUser) "Du" else "SkyOS Agent",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary,
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
                        text = "SkyOS Agent strukturiert gerade die Antwort...",
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

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )

                if (!isUser && !message.isStreaming) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                copyAiText(context, "SkyOS Agent", message.text)
                                onFeedback("Antwort kopiert.", ToastType.Success)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("Kopieren")
                        }

                        OutlinedButton(
                            onClick = {
                                shareAiText(context, "SkyOS Agent", message.text)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("Teilen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentWorkflowResultCard(
    summary: com.skydown.android.ui.model.AgentWorkflowSummary?,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    if (summary == null) return
    val accent = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
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
        summary.runId?.takeIf { it.isNotBlank() }?.let { runId ->
            Text(
                text = "Run: $runId",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentComposerBar(
    draft: String,
    selectedMode: AgentExecutionMode,
    selectedLevel: AiExperienceLevel,
    canTriggerAutomation: Boolean,
    shouldTriggerAutomation: Boolean,
    agentPhase: AgentInteractionPhase,
    compactLayout: Boolean,
    contentMaxWidth: Dp,
    embeddedInTools: Boolean,
    showDockClearance: Boolean,
    applyBottomSystemInset: Boolean,
    onDraftChanged: (String) -> Unit,
    onModeChanged: (AgentExecutionMode) -> Unit,
    onLevelChanged: (AiExperienceLevel) -> Unit,
    onToggleAutomation: () -> Unit,
    onSend: () -> Unit,
    onReset: () -> Unit,
    onDismissKeyboard: () -> Unit,
) {
    val outerVerticalPadding = when {
        embeddedInTools -> 0.dp
        compactLayout -> 6.dp
        else -> 8.dp
    }
    val dockClearancePadding = when {
        showDockClearance && compactLayout -> 72.dp
        showDockClearance -> 76.dp
        else -> 0.dp
    }
    val cardVerticalPadding = when {
        embeddedInTools -> 4.dp
        compactLayout -> 8.dp
        else -> 10.dp
    }
    val sectionSpacing = if (embeddedInTools) 6.dp else if (compactLayout) 8.dp else 10.dp
    val fieldMaxLines = if (embeddedInTools || compactLayout) 3 else 4

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .windowInsetsPadding(
                (if (applyBottomSystemInset) {
                    WindowInsets.navigationBars.union(WindowInsets.ime)
                } else {
                    WindowInsets.ime
                })
                    .only(WindowInsetsSides.Bottom),
            )
            .padding(
                start = if (compactLayout) 8.dp else 10.dp,
                top = outerVerticalPadding,
                end = if (compactLayout) 8.dp else 10.dp,
                bottom = outerVerticalPadding + dockClearancePadding,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            SkydownCard(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = if (compactLayout) 10.dp else 12.dp,
                    vertical = cardVerticalPadding,
                ),
            ) {
            agentPhase.composerStatusLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.92f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                )
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp),
            ) {
                items(AgentExecutionMode.entries, key = { it.rawValue }) { mode ->
                    val isSelected = mode == selectedMode
                    if (isSelected) {
                        Button(
                            onClick = { onModeChanged(mode) },
                            shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(mode.title)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onModeChanged(mode) },
                            shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(mode.title)
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = sectionSpacing),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp),
            ) {
                items(AiExperienceLevel.entries, key = { it.rawValue }) { level ->
                    val isSelected = level == selectedLevel
                    BrandStatusChip(
                        text = level.title,
                        accent = if (isSelected) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        },
                        isActive = isSelected,
                        onClick = if (agentPhase.shouldBlockComposerChrome) null else ({ onLevelChanged(level) }),
                    )
                }
            }

            Text(
                text = stringResource(agentAiLevelSubtitleResId(selectedLevel)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (canTriggerAutomation) {
                if (shouldTriggerAutomation) {
                    Button(
                        onClick = onToggleAutomation,
                        modifier = Modifier.padding(top = sectionSpacing),
                        shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Aktion aktiv")
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleAutomation,
                        modifier = Modifier.padding(top = sectionSpacing),
                        shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Aktion ausfuehren")
                    }
                }
            }

            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = sectionSpacing),
                placeholder = {
                    Text(selectedMode.placeholder)
                },
                minLines = 1,
                maxLines = fieldMaxLines,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onSend()
                        onDismissKeyboard()
                    },
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = sectionSpacing),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDismissKeyboard,
                    modifier = Modifier.size(if (embeddedInTools) 32.dp else 36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Tastatur ausblenden",
                    )
                }

                IconButton(
                    onClick = onReset,
                    enabled = !agentPhase.shouldBlockComposerChrome,
                    modifier = Modifier.size(if (embeddedInTools) 32.dp else 36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Agent zuruecksetzen",
                    )
                }

                FilledIconButton(
                    onClick = {
                        onSend()
                        onDismissKeyboard()
                    },
                    enabled = draft.isNotBlank() && !agentPhase.shouldBlockSend,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Senden",
                    )
                }
            }
            }
        }
    }
}
