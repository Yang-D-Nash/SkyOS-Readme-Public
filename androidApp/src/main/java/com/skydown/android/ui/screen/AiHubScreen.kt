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
    var hasPreparedN8NTrigger by rememberSaveable { mutableStateOf(false) }
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Tools",
                        subtitle = "AI und N8N global fuer die ganze App.",
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
                ToolsHubCard(
                    hasPreparedN8NTrigger = hasPreparedN8NTrigger,
                    onPrepareTrigger = { hasPreparedN8NTrigger = true },
                    modifier = Modifier.padding(
                        start = SkydownUiTokens.screenHorizontalPadding,
                        top = SkydownUiTokens.screenTopPadding + 4.dp,
                        end = SkydownUiTokens.screenHorizontalPadding,
                        bottom = 12.dp,
                    ),
                )

                if (currentUser == null) {
                    AiHubLoginCard(
                        onOpenLogin = onOpenLogin,
                        modifier = Modifier.padding(
                            start = SkydownUiTokens.screenHorizontalPadding,
                            end = SkydownUiTokens.screenHorizontalPadding,
                            bottom = 10.dp,
                        ),
                    )
                } else {
                    AiHubControlCard(
                        mode = mode,
                        onSelectMode = { selectedMode -> mode = selectedMode },
                        modifier = Modifier.padding(
                            start = SkydownUiTokens.screenHorizontalPadding,
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
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = "AI nur mit Konto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Melde dich an und starte direkt mit Hooks, Captions, Briefings, Release-Plaenen oder Visual-Ideen im globalen Tools-Bereich.",
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
private fun ToolsHubCard(
    hasPreparedN8NTrigger: Boolean,
    onPrepareTrigger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.cardPadding),
    ) {
        Text(
            text = "Global Tools",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (hasPreparedN8NTrigger) {
                "Der Trigger-Slot ist vorbereitet. Hier kann als naechstes dein direkter N8N-Webhook global andocken."
            } else {
                "AI und Automationen bleiben hier neutral fuer die ganze App. So haengt der spaetere N8N-Flow nicht an Musik, Video oder Merch."
            },
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AiHubBadge(text = "N8N", isAgent = false)
            AiHubBadge(text = "Bot", isAgent = false)
            AiHubBadge(text = "Agent", isAgent = true)
        }
        Button(
            onClick = onPrepareTrigger,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (hasPreparedN8NTrigger) "Trigger Slot bereit" else "Trigger Slot aktivieren")
        }
    }
}

@Composable
private fun AiHubControlCard(
    mode: AiHubMode,
    onSelectMode: (AiHubMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAgent = mode == AiHubMode.Agent
    val accent = if (isAgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val icon = if (isAgent) Icons.Default.Bolt else Icons.Default.AutoAwesome
    val title = if (isAgent) {
        "Agent fuer klare Struktur"
    } else {
        "Bot fuer schnelle Ideen"
    }
    val message = if (isAgent) {
        "Briefings, To-dos und Release-Plaene in einem Flow."
    } else {
        "Hooks, Captions und Visual-Ideen ohne Umwege."
    }

    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
