package com.skydown.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.model.AgentMessage
import com.skydown.android.ui.model.AgentMessageRole
import com.skydown.android.ui.viewmodel.AgentViewModel
import kotlinx.coroutines.delay

@Composable
fun AgentScreen(
    viewModel: AgentViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.isAgentEnabled && uiState.messages.isNotEmpty()) {
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

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (uiState.isAgentEnabled) {
                AgentComposerBar(
                    draft = uiState.draft,
                    isSending = uiState.isSending,
                    onDraftChanged = viewModel::updateDraft,
                    onSend = viewModel::sendDraft,
                    onReset = viewModel::resetConversation,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(innerPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    AgentHeroCard(
                        badges = if (uiState.isAgentEnabled) {
                            listOf("Genkit Flow", "Server-side", "Workflow Beta")
                        } else {
                            listOf("Genkit Flow", "Temporarily Off", "Workflow Beta")
                        },
                    )
                }

                item {
                    AgentExplainCard()
                }

                if (uiState.isAgentEnabled) {
                    item {
                        AgentQuickPromptCard(
                            prompts = uiState.quickPrompts,
                            onPromptSelected = viewModel::sendPrompt,
                        )
                    }

                    items(uiState.messages, key = { it.id }) { message ->
                        AgentMessageBubble(message = message)
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                } else {
                    item {
                        AgentDisabledCard()
                    }
                }
            }

            ToastHost(
                message = uiState.errorMessage,
                type = ToastType.Error,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (uiState.isAgentEnabled) 104.dp else 32.dp),
            )
        }
    }
}

@Composable
private fun AgentHeroCard(
    badges: List<String>,
) {
    SkydownCard(contentPadding = PaddingValues(20.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Skydown Agent",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Der Agent laeuft ueber Cloud Functions und Genkit, damit Regeln, Prompting und Beobachtung zentral bleiben.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )
            }

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            badges.forEach { badge ->
                AgentBadge(badge)
            }
        }
    }
}

@Composable
private fun AgentExplainCard() {
    SkydownCard {
        SectionHeader("Wofuer der Agent gedacht ist")
        Text(
            text = "Hier geht es weniger um lose Ideen und mehr um umsetzbare Briefings, Launch-Plaene, naechste Schritte und spaetere serverseitige Flows.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AgentDisabledCard() {
    SkydownCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Agent ist pausiert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Remote Switch aktiv",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Text(
            text = "Der Agent haengt aktuell am gleichen zentralen AI-Schalter. Sobald `ai_enabled` wieder auf `true` steht, ist auch der Genkit-Weg wieder offen.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun AgentQuickPromptCard(
    prompts: List<String>,
    onPromptSelected: (String) -> Unit,
) {
    SkydownCard {
        SectionHeader("Schnell starten")
        Text(
            text = "Die Vorschlaege schicken direkt eine workflow-orientierte Anfrage an den Agenten.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(prompts, key = { it }) { prompt ->
                AssistChip(
                    onClick = { onPromptSelected(prompt) },
                    label = {
                        Text(
                            text = prompt,
                            maxLines = 2,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(
    message: AgentMessage,
) {
    val isUser = message.role == AgentMessageRole.User
    val bubbleShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = if (isUser) 24.dp else 8.dp,
        bottomEnd = if (isUser) 8.dp else 24.dp,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = if (isUser) "Du" else "Skydown Agent",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary,
            )

            if (message.isStreaming && message.text.isBlank()) {
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
                        text = "Agent baut gerade die Antwort...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            } else {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AgentComposerBar(
    draft: String,
    isSending: Boolean,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onReset: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        SkydownCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Agent-Aufgabe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Klare Ziele, Rollen und naechste Schritte funktionieren hier am besten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                }

                Row {
                    IconButton(
                        onClick = onReset,
                        enabled = !isSending,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Agent zuruecksetzen",
                        )
                    }
                }
            }

            androidx.compose.material3.OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                placeholder = {
                    Text("Zum Beispiel: Bau mir ein Release-Briefing fuer Freitag auf.")
                },
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isSending) "Skydown Agent plant..." else "Bereit fuer den Workflow",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    textAlign = TextAlign.End,
                )

                FilledIconButton(
                    onClick = onSend,
                    enabled = draft.isNotBlank() && !isSending,
                    modifier = Modifier.padding(start = 12.dp),
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

@Composable
private fun AgentBadge(
    text: String,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}
