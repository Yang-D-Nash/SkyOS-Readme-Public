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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.SkydownCard

private enum class AiHubMode {
    Bot,
    Agent,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(
    onOpenSettings: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AiHubMode.Bot) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "KI",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Bot fuer schnelle Ideen, Agent fuer Struktur und Planung.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    AppTopBarSessionActions(onOpenSettings = onOpenSettings)
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
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SkydownCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Text(
                        text = "KI Modus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Wechsle direkt zwischen kreativem Bot und strukturiertem Agent, ohne dafuer einen eigenen Haupttab zu verbrauchen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (mode == AiHubMode.Bot) {
                            Button(
                                onClick = { mode = AiHubMode.Bot },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                )
                                Text(
                                    text = "Bot",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { mode = AiHubMode.Bot },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                )
                                Text(
                                    text = "Bot",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }

                        if (mode == AiHubMode.Agent) {
                            Button(
                                onClick = { mode = AiHubMode.Agent },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                )
                                Text(
                                    text = "Agent",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { mode = AiHubMode.Agent },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                )
                                Text(
                                    text = "Agent",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
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
