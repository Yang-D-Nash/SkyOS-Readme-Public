package com.skydown.android.ui.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydown.android.data.AiAccessMode
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.BrandActionButton
import com.skydown.android.ui.component.LocalSessionUser
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.SkydownUiTokens
import com.skydown.android.ui.component.rememberUsesCompactVisualDensity
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors

private enum class AiHubMode {
    Bot,
    Agent,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(
    showsWorkflowWorkspace: Boolean,
    onToggleWorkflow: () -> Unit,
    onHideWorkflow: () -> Unit,
    onOpenLogin: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenAutomationSettings: () -> Unit = onOpenSettings,
) {
    var mode by rememberSaveable { mutableStateOf(AiHubMode.Bot) }
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val workspaceMaxWidth = if (compactVisualDensity) 620.dp else Dp.Unspecified
    val currentUser = LocalSessionUser.current
    val aiAccessMode by AppFeatureFlagsStore.aiAccessMode.collectAsStateWithLifecycle()
    val hasAiAccess = AppFeatureFlagsStore.allowsAiAccess(
        user = currentUser,
        accessMode = aiAccessMode,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Atelier",
                        subtitle = "KI, Agenten, Workflows.",
                    )
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings,
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
                        secondaryColor = MaterialTheme.colorScheme.tertiary,
                        primaryAlpha = 0.060f,
                        secondaryAlpha = 0.038f,
                    ),
                )
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (currentUser == null) {
                    AiHubLoginCard(
                        title = if (aiAccessMode == AiAccessMode.AdminOnly) {
                            "KI nur fuer freigegebene Konten"
                        } else {
                            "KI nur mit Konto"
                        },
                        message = AppFeatureFlagsStore.accessDeniedMessage(
                            user = currentUser,
                            accessMode = aiAccessMode,
                        ),
                        onOpenLogin = onOpenLogin,
                        modifier = Modifier.padding(
                            start = SkydownUiTokens.screenHorizontalPadding,
                            end = SkydownUiTokens.screenHorizontalPadding,
                            bottom = 8.dp,
                        )
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
                        modifier = Modifier.padding(
                            start = SkydownUiTokens.screenHorizontalPadding,
                            end = SkydownUiTokens.screenHorizontalPadding,
                            bottom = 8.dp,
                        )
                            .widthIn(max = workspaceMaxWidth)
                            .fillMaxWidth(),
                    )
                } else {
                    AiHubCompactHeader(
                        mode = mode,
                        showsWorkflowWorkspace = showsWorkflowWorkspace,
                        onSelectMode = { selectedMode ->
                            onHideWorkflow()
                            mode = selectedMode
                        },
                        onToggleWorkflow = onToggleWorkflow,
                        compactVisualDensity = compactVisualDensity,
                        modifier = Modifier.padding(
                            start = SkydownUiTokens.screenHorizontalPadding,
                            top = SkydownUiTokens.screenTopPadding + 2.dp,
                            end = SkydownUiTokens.screenHorizontalPadding,
                            bottom = 8.dp,
                        )
                            .widthIn(max = workspaceMaxWidth)
                            .fillMaxWidth(),
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
                                    modifier = Modifier.padding(horizontal = SkydownUiTokens.screenHorizontalPadding),
                                )
                            } else {
                                when (mode) {
                                    AiHubMode.Bot -> AiScreen(showTopBar = false)
                                    AiHubMode.Agent -> AgentScreen(showTopBar = false)
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
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AiHubBadge(text = "Bot", isAgent = false)
            AiHubBadge(text = "Agent", isAgent = true)
            AiHubBadge(text = "Visuals", isAgent = false)
        }
        BrandActionButton(
            text = "Jetzt anmelden",
            onClick = onOpenLogin,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Composable
private fun AiHubRestrictedCard(
    message: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = "KI derzeit gesperrt",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AiHubBadge(text = "Bot", isAgent = false)
            AiHubBadge(text = "Agent", isAgent = true)
            AiHubBadge(text = "Visuals", isAgent = false)
        }
        BrandActionButton(
            text = "Einstellungen",
            onClick = onOpenSettings,
            accent = MaterialTheme.colorScheme.tertiary,
            filled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Composable
private fun AiHubCompactHeader(
    mode: AiHubMode,
    showsWorkflowWorkspace: Boolean,
    onSelectMode: (AiHubMode) -> Unit,
    onToggleWorkflow: () -> Unit,
    compactVisualDensity: Boolean,
    modifier: Modifier = Modifier,
) {
    val shellShape = RoundedCornerShape(if (compactVisualDensity) 22.dp else 26.dp)

    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(if (compactVisualDensity) 7.dp else 9.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shellShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                        ),
                    ),
                    shape = shellShape,
                )
                .padding(if (compactVisualDensity) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactVisualDensity) 8.dp else 10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sky Intelligence",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Bot, Agent und Workflows in einem Raum.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AiHubBadge(
                    text = if (showsWorkflowWorkspace) "Workflow" else if (mode == AiHubMode.Agent) "Agent" else "Bot",
                    isAgent = mode == AiHubMode.Agent || showsWorkflowWorkspace,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AiHubModeButton(
                    mode = AiHubMode.Bot,
                    isSelected = mode == AiHubMode.Bot && !showsWorkflowWorkspace,
                    onClick = { onSelectMode(AiHubMode.Bot) },
                    compactVisualDensity = compactVisualDensity,
                    modifier = Modifier.weight(1f),
                )
                AiHubModeButton(
                    mode = AiHubMode.Agent,
                    isSelected = mode == AiHubMode.Agent && !showsWorkflowWorkspace,
                    onClick = { onSelectMode(AiHubMode.Agent) },
                    compactVisualDensity = compactVisualDensity,
                    modifier = Modifier.weight(1f),
                )

                BrandActionButton(
                    text = if (showsWorkflowWorkspace) "KI" else "Workflow",
                    onClick = onToggleWorkflow,
                    accent = MaterialTheme.colorScheme.secondary,
                    filled = showsWorkflowWorkspace,
                    compact = true,
                    modifier = Modifier.widthIn(min = 112.dp),
                )
            }
        }
    }
}

@Composable
private fun WorkflowWorkspaceCard(
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = "Workflow Cockpit",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Vom Briefing zur Aktion, ohne die App zu verlassen.",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WorkflowSignalRow(
                index = "01",
                title = "Briefing",
                body = "Aus Idee wird Plan.",
            )
            WorkflowSignalRow(
                index = "02",
                title = "Aktion",
                body = "n8n startet mit Kontext.",
            )
            WorkflowSignalRow(
                index = "03",
                title = "Rueckweg",
                body = "Zurueck in SkyOs.",
            )
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AiHubBadge(text = "n8n", isAgent = false)
            AiHubBadge(text = "Agent", isAgent = true)
            AiHubBadge(text = "Kontext", isAgent = false)
        }

        BrandActionButton(
            text = "Workflow einrichten",
            onClick = onOpenSettings,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        BrandActionButton(
            text = "Zur KI zurueck",
            onClick = onClose,
            accent = MaterialTheme.colorScheme.tertiary,
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
    isSelected: Boolean,
    onClick: () -> Unit,
    compactVisualDensity: Boolean,
    modifier: Modifier = Modifier,
) {
    val isAgent = mode == AiHubMode.Agent
    val accent = if (isAgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val icon = if (isAgent) Icons.Default.Bolt else Icons.Default.AutoAwesome
    val label = if (isAgent) "Agent" else "Bot"

    BrandActionButton(
        text = label,
        onClick = onClick,
        accent = accent,
        icon = icon,
        filled = isSelected,
        compact = compactVisualDensity,
        modifier = modifier,
    )
}

@Composable
private fun AiHubBadge(
    text: String,
    isAgent: Boolean,
) {
    val accent = if (isAgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
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
