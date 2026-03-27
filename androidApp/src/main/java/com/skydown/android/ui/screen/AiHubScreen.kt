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
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.SkydownUiTokens
import com.skydown.android.ui.component.skydownScreenBrush

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
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { SkydownTopBarTitle(title = "KI") },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenSettings = onOpenSettings,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                ),
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
                        onOpenLogin = onOpenLogin,
                        modifier = Modifier.padding(
                            horizontal = SkydownUiTokens.screenHorizontalPadding,
                            vertical = SkydownUiTokens.screenTopPadding,
                        ),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = SkydownUiTokens.screenHorizontalPadding,
                                vertical = SkydownUiTokens.screenTopPadding,
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AiHubWelcomeCard(mode = mode)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AiHubModeButton(
                                mode = AiHubMode.Bot,
                                isSelected = mode == AiHubMode.Bot,
                                onClick = { mode = AiHubMode.Bot },
                                modifier = Modifier.weight(1f),
                            )
                            AiHubModeButton(
                                mode = AiHubMode.Agent,
                                isSelected = mode == AiHubMode.Agent,
                                onClick = { mode = AiHubMode.Agent },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

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
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = "KI nur mit Konto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Melde dich an und starte direkt mit Hooks, Captions, Briefings, Release-Plaenen oder Visual-Ideen in einem gemeinsamen Bereich.",
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
private fun AiHubWelcomeCard(
    mode: AiHubMode,
) {
    val isAgent = mode == AiHubMode.Agent
    val accent = if (isAgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val icon = if (isAgent) Icons.Default.Bolt else Icons.Default.AutoAwesome
    val title = if (isAgent) {
        "Briefings, To-dos und Release-Plaene mit Struktur."
    } else {
        "Hooks, Captions und Visual-Ideen ohne Reibung."
    }
    val message = if (isAgent) {
        "Der Agent baut dir klare Naechste-Schritte-Listen, Kampagnenplaene und Briefings fuer Releases."
    } else {
        "Der Bot hilft dir schnell mit kreativen Varianten, Claims, Captions und ersten Bildrichtungen."
    }
    val badges = if (isAgent) {
        listOf("Briefing", "Plan", "Checkliste")
    } else {
        listOf("Hooks", "Captions", "Visuals")
    }

    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.heroPadding)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    badges.forEach { badge ->
                        AiHubBadge(text = badge, isAgent = isAgent)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                )
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
