package com.skydown.android.ui.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
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
import com.skydown.android.ui.component.rememberSkydownScreenSectionSpacing
import com.skydown.android.ui.component.rememberUsesCompactVisualDensity
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.theme.SkydownPanelTitleTextStyle

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
    val sectionSpacing = rememberSkydownScreenSectionSpacing()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Atelier",
                        subtitle = "KI, Agenten, Workflows.",
                        accent = MaterialTheme.colorScheme.tertiary,
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
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(skydownContentPadding(innerPadding)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
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
                        onSelectMode = { selectedMode ->
                            onHideWorkflow()
                            mode = selectedMode
                        },
                        onToggleWorkflow = onToggleWorkflow,
                        compactVisualDensity = compactVisualDensity,
                        modifier = Modifier
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
                                    modifier = Modifier,
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
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius)
    SkydownCard(
        modifier = modifier.border(
            width = 1.dp,
            color = accent.copy(alpha = 0.14f),
            shape = shape,
        ),
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
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AiHubBadge(text = "Bot", accent = MaterialTheme.colorScheme.primary)
            AiHubBadge(text = "Agent", accent = MaterialTheme.colorScheme.tertiary)
            AiHubBadge(text = "Visuals", accent = MaterialTheme.colorScheme.secondary)
        }
        BrandActionButton(
            text = "Jetzt anmelden",
            onClick = onOpenLogin,
            accent = accent,
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
    val accent = MaterialTheme.colorScheme.tertiary
    val shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius)
    SkydownCard(
        modifier = modifier.border(
            width = 1.dp,
            color = accent.copy(alpha = 0.14f),
            shape = shape,
        ),
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = "KI derzeit gesperrt",
            style = SkydownPanelTitleTextStyle,
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
            AiHubBadge(text = "Bot", accent = MaterialTheme.colorScheme.primary)
            AiHubBadge(text = "Agent", accent = MaterialTheme.colorScheme.secondary)
            AiHubBadge(text = "Visuals", accent = MaterialTheme.colorScheme.tertiary)
        }
        BrandActionButton(
            text = "Einstellungen",
            onClick = onOpenSettings,
            accent = accent,
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
    val shellShape = RoundedCornerShape(18.dp)
    val accent = if (mode == AiHubMode.Agent) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shellShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            accent.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f),
                            accent.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                        ),
                    ),
                    shape = shellShape,
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
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
            AiHubWorkflowButton(
                showsWorkflowWorkspace = showsWorkflowWorkspace,
                onClick = onToggleWorkflow,
                compactVisualDensity = compactVisualDensity,
            )
        }
    }
}

@Composable
private fun WorkflowWorkspaceCard(
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius)
    SkydownCard(
        modifier = modifier.border(
            width = 1.dp,
            color = accent.copy(alpha = 0.14f),
            shape = shape,
        ),
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = "Workflow Cockpit",
            style = SkydownPanelTitleTextStyle,
        )
        Text(
            text = "Verbinde Agent, Kontext und Aktionen so, dass auch Laien sofort wissen, was passiert.",
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
                body = "Der Agent macht aus einer Idee einen klaren Plan.",
            )
            WorkflowSignalRow(
                index = "02",
                title = "Aktion",
                body = "Optional wird dein n8n-Workflow mit User-Kontext gestartet.",
            )
            WorkflowSignalRow(
                index = "03",
                title = "Rueckweg",
                body = "Du bleibst in der App und kannst direkt weiterarbeiten.",
            )
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AiHubBadge(text = "n8n", accent = MaterialTheme.colorScheme.secondary)
            AiHubBadge(text = "Agent", accent = MaterialTheme.colorScheme.primary)
            AiHubBadge(text = "Kontext", accent = MaterialTheme.colorScheme.tertiary)
        }

        BrandActionButton(
            text = "Workflow einrichten",
            onClick = onOpenSettings,
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        BrandActionButton(
            text = "Zur KI zurueck",
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
    val accent = if (mode == AiHubMode.Agent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val icon = if (mode == AiHubMode.Agent) Icons.Default.Bolt else Icons.Default.AutoAwesome
    val label = if (mode == AiHubMode.Agent) "Agent" else "Bot"
    val shape = RoundedCornerShape(16.dp)
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(start = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AiHubWorkflowButton(
    showsWorkflowWorkspace: Boolean,
    onClick: () -> Unit,
    compactVisualDensity: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(16.dp)
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
            text = if (showsWorkflowWorkspace) "Zur KI" else "Workflow",
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
