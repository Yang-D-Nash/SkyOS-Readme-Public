package com.skydown.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydown.android.data.AiAccessMode
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.SkydownUiTokens
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors

private enum class AiHubMode {
    Bot,
    Agent,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(
    onOpenLogin: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    onOpenSettings: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AiHubMode.Bot) }
    var hasPreparedWorkflowTrigger by rememberSaveable { mutableStateOf(false) }
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()
    val aiAccessMode by AppFeatureFlagsStore.aiAccessMode.collectAsStateWithLifecycle()
    val hasAiAccess = AppFeatureFlagsStore.allowsAiAccess(
        user = currentUser,
        accessMode = aiAccessMode,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Tools",
                        subtitle = "AI und Workflow global fuer die ganze App.",
                    )
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenSettings = onOpenSettings,
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
                        primaryAlpha = 0.08f,
                        secondaryAlpha = 0.05f,
                    ),
                )
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (currentUser == null) {
                    AiHubLoginCard(
                        title = if (aiAccessMode == AiAccessMode.AdminOnly) {
                            "KI nur fuer Admins"
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
                            bottom = 10.dp,
                        ),
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
                            bottom = 10.dp,
                        ),
                    )
                } else {
                    AiHubCompactHeader(
                        mode = mode,
                        hasPreparedWorkflowTrigger = hasPreparedWorkflowTrigger,
                        onSelectMode = { selectedMode -> mode = selectedMode },
                        onTrigger = { hasPreparedWorkflowTrigger = true },
                        modifier = Modifier.padding(
                            start = SkydownUiTokens.screenHorizontalPadding,
                            top = SkydownUiTokens.screenTopPadding + 4.dp,
                            end = SkydownUiTokens.screenHorizontalPadding,
                            bottom = 12.dp,
                        ),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
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
        Button(
            onClick = onOpenLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Jetzt anmelden")
        }
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
            AiHubBadge(text = "Admin Only", isAgent = true)
            AiHubBadge(text = "Bot", isAgent = false)
            AiHubBadge(text = "Agent", isAgent = true)
        }
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Einstellungen")
        }
    }
}

@Composable
private fun AiHubCompactHeader(
    mode: AiHubMode,
    hasPreparedWorkflowTrigger: Boolean,
    onSelectMode: (AiHubMode) -> Unit,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAgent = mode == AiHubMode.Agent
    val accent = if (isAgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AiHubModeButton(
                mode = AiHubMode.Bot,
                isSelected = mode == AiHubMode.Bot,
                onClick = { onSelectMode(AiHubMode.Bot) },
                modifier = Modifier.weight(1f),
            )
            AiHubModeButton(
                mode = AiHubMode.Agent,
                isSelected = mode == AiHubMode.Agent,
                onClick = { onSelectMode(AiHubMode.Agent) },
                modifier = Modifier.weight(1f),
            )

            OutlinedButton(
                onClick = onTrigger,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(if (hasPreparedWorkflowTrigger) "Workflow" else "Trigger")
            }
        }
    }
}

@Composable
private fun AiHubModeButton(
    mode: AiHubMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAgent = mode == AiHubMode.Agent
    val accent = if (isAgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val icon = if (isAgent) Icons.Default.Bolt else Icons.Default.AutoAwesome
    val label = if (isAgent) "Agent" else "Bot"

    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                color = accent,
            )
        }
    }
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
