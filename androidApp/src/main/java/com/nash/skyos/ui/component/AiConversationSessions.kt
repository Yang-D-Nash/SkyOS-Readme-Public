package com.nash.skyos.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nash.skyos.data.AiConversationHistorySessionSnapshot

@Composable
fun AiConversationSessionStrip(
    title: String,
    subtitle: String,
    accent: Color,
    enabled: Boolean,
    canDelete: Boolean = false,
    showsManagementActions: Boolean = false,
    onOpenSessions: () -> Unit,
    onCreateNewChat: () -> Unit,
    onRefreshChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onOpenSessions,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Chats oeffnen",
                tint = accent,
            )
        }
        if (showsManagementActions) {
            FilledIconButton(
                onClick = onRefreshChat,
                enabled = enabled,
                modifier = Modifier.size(46.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Chat aktualisieren",
                )
            }
        }
        FilledIconButton(
            onClick = onCreateNewChat,
            enabled = enabled,
            modifier = Modifier.size(46.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Neuer Chat",
            )
        }
        if (showsManagementActions) {
            FilledIconButton(
                onClick = onDeleteChat,
                enabled = enabled && canDelete,
                modifier = Modifier.size(46.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Chat loeschen",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConversationSessionsSheet(
    title: String,
    sessions: List<AiConversationHistorySessionSnapshot>,
    activeSessionId: String?,
    renameDraft: String,
    accent: Color,
    enabled: Boolean,
    onRenameDraftChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectSession: (String) -> Unit,
    onCreateNewChat: () -> Unit,
    onRenameActiveSession: () -> Unit,
    onDeleteActiveSession: () -> Unit,
) {
    val activeSession = sessions.firstOrNull { it.sessionId == activeSessionId }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Weiter im Verlauf oder einen frischen Chat starten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            OutlinedButton(
                onClick = onCreateNewChat,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text("Neuer Chat")
            }

            if (activeSession != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Aktiver Chat",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = renameDraft,
                        onValueChange = onRenameDraftChanged,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        placeholder = {
                            Text(activeSession.title)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onDeleteActiveSession,
                            enabled = enabled,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Loeschen")
                        }
                        OutlinedButton(
                            onClick = onRenameActiveSession,
                            enabled = enabled && renameDraft.trim().isNotBlank(),
                        ) {
                            Text("Umbenennen")
                        }
                    }
                }
            }

            Text(
                text = "Chats",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )

            if (sessions.isEmpty()) {
                Text(
                    text = "Noch kein Verlauf.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sessions.forEach { session ->
                        val selected = session.sessionId == activeSessionId
                        val countLabel = when (session.promptCount) {
                            0 -> "Neu"
                            1 -> "1 Anfrage"
                            else -> "${session.promptCount} Anfragen"
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (selected) {
                                        accent.copy(alpha = 0.10f)
                                    } else {
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                )
                                .clickable(enabled = enabled) {
                                    onSelectSession(session.sessionId)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = session.preview.ifBlank { "Noch keine Antwort in diesem Chat." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = countLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(4.dp))
        }
    }
}
