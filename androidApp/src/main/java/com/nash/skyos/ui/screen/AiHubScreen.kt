package com.nash.skyos.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nash.skyos.R
import com.nash.skyos.data.AiAccessMode
import com.nash.skyos.data.AppFeatureFlagsStore
import com.nash.skyos.ui.component.AppTopBarSessionActions
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.LocalSessionUser
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownPremiumIconAction
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.rememberUsesCompactVisualDensity
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownScreenBrush
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.theme.SkydownPanelTitleTextStyle
import com.nash.skyos.ui.theme.skydownAccentMystic

private enum class AiHubMode {
    Bot,
    Agent,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(
    immersiveMode: Boolean = false,
    showsWorkflowWorkspace: Boolean,
    pendingAgentPrefillPrompt: String? = null,
    onConsumePendingAgentPrefillPrompt: () -> Unit = {},
    onToggleWorkflow: () -> Unit,
    onHideWorkflow: () -> Unit,
    onExitImmersive: (() -> Unit)? = null,
    onOpenLogin: () -> Unit = {},
    onGuestSignIn: (() -> Unit)? = null,
    onOpenCart: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenAutomationSettings: () -> Unit = onOpenSettings,
    onOpenHomeProductivityFromAgent: (String) -> Unit = {},
) {
    @Composable
    fun AiHubMode.displayTitle(): String = when (this) {
        AiHubMode.Bot -> stringResource(R.string.ai_hub_mode_core_ai)
        AiHubMode.Agent -> stringResource(R.string.ai_hub_mode_agent)
    }
    @Composable
    fun AiHubMode.displaySubtitle(): String = when (this) {
        AiHubMode.Bot -> stringResource(R.string.ai_hub_mode_core_ai_subtitle)
        AiHubMode.Agent -> stringResource(R.string.ai_hub_mode_agent_subtitle)
    }
    var mode by rememberSaveable { mutableStateOf(AiHubMode.Bot) }
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val workspaceMaxWidth = if (compactVisualDensity) Dp.Unspecified else 1040.dp
    val currentUser = LocalSessionUser.current
    val aiAccessMode by AppFeatureFlagsStore.aiAccessMode.collectAsStateWithLifecycle()
    val hasAiAccess = AppFeatureFlagsStore.allowsAiAccess(
        user = currentUser,
        accessMode = aiAccessMode,
    )
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    val botModeTitle = stringResource(R.string.ai_hub_mode_core_ai)
    val agentModeTitle = stringResource(R.string.ai_hub_mode_agent)
    val botModeSubtitle = stringResource(R.string.ai_hub_mode_core_ai_subtitle)
    val agentModeSubtitle = stringResource(R.string.ai_hub_mode_agent_subtitle)

    LaunchedEffect(pendingAgentPrefillPrompt, currentUser?.id, hasAiAccess) {
        val trimmed = pendingAgentPrefillPrompt?.trim().orEmpty()
        if (trimmed.isNotEmpty() && currentUser != null && hasAiAccess) {
            onHideWorkflow()
            mode = AiHubMode.Agent
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = stringResource(R.string.tabs_tools),
                        subtitle = stringResource(R.string.ai_hub_topbar_subtitle),
                        accent = MaterialTheme.colorScheme.tertiary,
                    )
                },
                navigationIcon = if (immersiveMode && onExitImmersive != null) {
                    {
                        SkydownPremiumIconAction(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            onClick = onExitImmersive,
                            modifier = Modifier.padding(start = 4.dp),
                            accent = MaterialTheme.colorScheme.tertiary,
                            size = 40.dp,
                            iconSize = 19.dp,
                        )
                    }
                } else {
                    {}
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings,
                        onGuestSignIn = onGuestSignIn,
                        dense = compactVisualDensity,
                    )
                },
                colors = skydownTopBarColors(),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(
                        secondaryColor = MaterialTheme.colorScheme.skydownAccentMystic(),
                        primaryAlpha = 0.028f,
                        secondaryAlpha = 0.018f,
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(skydownContentPadding(innerPadding))
                    .testTag("ai.hub.root"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                if (currentUser == null) {
                    val aiLoginTitle = if (aiAccessMode == AiAccessMode.AdminOnly) {
                        stringResource(R.string.auth_ai_login_title_staff)
                    } else {
                        stringResource(R.string.auth_ai_login_title)
                    }
                    val aiLoginMessage = if (aiAccessMode == AiAccessMode.SignedIn) {
                        stringResource(R.string.auth_ai_login_hint_signed_in)
                    } else {
                        AppFeatureFlagsStore.accessDeniedMessage(
                            user = currentUser,
                            accessMode = aiAccessMode,
                        )
                    }
                    AiHubLoginCard(
                        title = aiLoginTitle,
                        message = aiLoginMessage,
                        ctaLabel = stringResource(R.string.auth_continue_with_account),
                        onOpenLogin = onOpenLogin,
                        modifier = Modifier
                            .widthIn(max = workspaceMaxWidth)
                            .fillMaxWidth(),
                    )
                } else if (!hasAiAccess) {
                    AiHubRestrictedCard(
                        message = AppFeatureFlagsStore.accessDeniedMessage(
                            user = currentUser,
                            accessMode = aiAccessMode,
                        ),
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier
                            .widthIn(max = workspaceMaxWidth)
                            .fillMaxWidth(),
                    )
                } else {
                    AiHubCompactHeader(
                        mode = mode,
                        showsWorkflowWorkspace = showsWorkflowWorkspace,
                                workflowTitle = if (showsWorkflowWorkspace) {
                                    stringResource(R.string.ai_hub_workflow_return_intelligence)
                                } else {
                                    stringResource(R.string.ai_hub_workflow_automation_studio)
                                },
                                modeTitle = { selectedMode ->
                                    if (selectedMode == AiHubMode.Bot) botModeTitle else agentModeTitle
                                },
                                modeSubtitle = { selectedMode ->
                                    if (selectedMode == AiHubMode.Bot) botModeSubtitle else agentModeSubtitle
                                },
                        onSelectMode = { selectedMode ->
                            onHideWorkflow()
                            mode = selectedMode
                        },
                        onToggleWorkflow = onToggleWorkflow,
                        compactVisualDensity = compactVisualDensity,
                        modifier = Modifier
                            .widthIn(max = workspaceMaxWidth)
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )

                    Text(
                        text = stringResource(R.string.ai_legal_disclosure_short),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier
                            .widthIn(max = workspaceMaxWidth)
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = workspaceMaxWidth)
                                .fillMaxWidth()
                                .fillMaxHeight(),
                        ) {
                            if (showsWorkflowWorkspace) {
                                WorkflowWorkspaceCard(
                                    onOpenSettings = onOpenAutomationSettings,
                                    onClose = onHideWorkflow,
                                    modifier = Modifier,
                                )
                            } else {
                                when (mode) {
                                    AiHubMode.Bot -> AiScreen(
                                        showTopBar = false,
                                        immersiveInTools = immersiveMode,
                                    )
                                    AiHubMode.Agent -> AgentScreen(
                                        showTopBar = false,
                                        immersiveInTools = immersiveMode,
                                        prefilledPrompt = pendingAgentPrefillPrompt,
                                        onConsumePrefilledPrompt = onConsumePendingAgentPrefillPrompt,
                                        onOpenHomeProductivity = onOpenHomeProductivityFromAgent,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiHubLoginCard(
    title: String,
    message: String,
    ctaLabel: String,
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = title,
            style = SkydownPanelTitleTextStyle,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        BrandActionButton(
            text = ctaLabel,
            onClick = onOpenLogin,
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

@Composable
private fun AiHubRestrictedCard(
    message: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.tertiary
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = stringResource(R.string.ai_hub_restricted_title),
            style = SkydownPanelTitleTextStyle,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        BrandActionButton(
            text = stringResource(R.string.app_topbar_settings_label),
            onClick = onOpenSettings,
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

@Composable
private fun AiHubCompactHeader(
    mode: AiHubMode,
    showsWorkflowWorkspace: Boolean,
    workflowTitle: String,
    modeTitle: (AiHubMode) -> String,
    modeSubtitle: (AiHubMode) -> String,
    onSelectMode: (AiHubMode) -> Unit,
    onToggleWorkflow: () -> Unit,
    compactVisualDensity: Boolean,
    modifier: Modifier = Modifier,
) {
    val shellShape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius)
    val accent = if (mode == AiHubMode.Agent) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shellShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.11f),
                        accent.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                    ),
                ),
            )
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        accent.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    ),
                ),
                shape = shellShape,
            )
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingChrome),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingChrome),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AiHubModeButton(
                mode = AiHubMode.Bot,
                title = modeTitle(AiHubMode.Bot),
                subtitle = modeSubtitle(AiHubMode.Bot),
                isSelected = mode == AiHubMode.Bot && !showsWorkflowWorkspace,
                onClick = { onSelectMode(AiHubMode.Bot) },
                compactVisualDensity = compactVisualDensity,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai.hub.mode.bot"),
            )
            AiHubModeButton(
                mode = AiHubMode.Agent,
                title = modeTitle(AiHubMode.Agent),
                subtitle = modeSubtitle(AiHubMode.Agent),
                isSelected = mode == AiHubMode.Agent && !showsWorkflowWorkspace,
                onClick = { onSelectMode(AiHubMode.Agent) },
                compactVisualDensity = compactVisualDensity,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai.hub.mode.agent"),
            )
        }
        AiHubWorkflowButton(
            showsWorkflowWorkspace = showsWorkflowWorkspace,
            title = workflowTitle,
            onClick = onToggleWorkflow,
            compactVisualDensity = compactVisualDensity,
            modifier = if (compactVisualDensity) {
                Modifier
                    .fillMaxWidth()
                    .testTag("ai.hub.workflow")
            } else {
                Modifier.testTag("ai.hub.workflow")
            },
        )
    }
}

@Composable
private fun WorkflowWorkspaceCard(
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.secondary
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = stringResource(R.string.ai_hub_workflow_title),
            style = SkydownPanelTitleTextStyle,
        )
        Text(
            text = stringResource(R.string.ai_hub_workflow_subtitle),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        ) {
            WorkflowSignalRow(
                index = "01",
                title = stringResource(R.string.ai_hub_workflow_step_briefing_title),
                body = stringResource(R.string.ai_hub_workflow_step_briefing_body),
            )
            WorkflowSignalRow(
                index = "02",
                title = stringResource(R.string.ai_hub_workflow_step_action_title),
                body = stringResource(R.string.ai_hub_workflow_step_action_body),
            )
            WorkflowSignalRow(
                index = "03",
                title = stringResource(R.string.ai_hub_workflow_step_continue_title),
                body = stringResource(R.string.ai_hub_workflow_step_continue_body),
            )
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        ) {
            AiHubBadge(text = stringResource(R.string.ai_hub_badge_n8n), accent = MaterialTheme.colorScheme.secondary)
            AiHubBadge(text = stringResource(R.string.ai_hub_badge_agent), accent = MaterialTheme.colorScheme.primary)
            AiHubBadge(text = stringResource(R.string.ai_hub_badge_context), accent = MaterialTheme.colorScheme.tertiary)
        }

        BrandActionButton(
            text = stringResource(R.string.ai_hub_workflow_setup),
            onClick = onOpenSettings,
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        BrandActionButton(
            text = stringResource(R.string.ai_hub_back_to_ai),
            onClick = onClose,
            accent = MaterialTheme.colorScheme.primary,
            filled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}

@Composable
private fun WorkflowSignalRow(
    index: String,
    title: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingHairline)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
            )
        }
    }
}

@Composable
private fun AiHubModeButton(
    mode: AiHubMode,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    compactVisualDensity: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (mode == AiHubMode.Agent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val icon = if (mode == AiHubMode.Agent) Icons.Default.Bolt else Icons.Default.AutoAwesome
    val shape = RoundedCornerShape(SkydownUiTokens.denseRadius)
    val backgroundColor = if (isSelected) accent else MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val horizontalPadding = if (compactVisualDensity) 10.dp else 12.dp

    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    accent.copy(alpha = 0.28f)
                } else {
                    accent.copy(alpha = 0.14f)
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color.White else accent,
            modifier = Modifier.size(16.dp),
        )
        Column(
            modifier = Modifier.padding(start = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    Color.White.copy(alpha = 0.84f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AiHubWorkflowButton(
    showsWorkflowWorkspace: Boolean,
    title: String,
    onClick: () -> Unit,
    compactVisualDensity: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(SkydownUiTokens.denseRadius)
    val horizontalPadding = if (compactVisualDensity) 10.dp else 12.dp

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.22f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = if (showsWorkflowWorkspace) Icons.Default.Close else Icons.Default.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AiHubBadge(
    text: String,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
    }
}
