package com.nash.skyos.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nash.skyos.R
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
    onRefreshChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onOpenSessions,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(SkydownUiTokens.sheetHeroRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = accent.copy(alpha = if (enabled) 0.38f else 0.18f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingHairline),
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
                    contentDescription = stringResource(R.string.ai_sessions_open),
                    tint = accent,
                )
            }
        }
        if (showsManagementActions) {
            Surface(
                onClick = onRefreshChat,
                enabled = enabled,
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.ai_session_refresh),
                    )
                }
            }
        }
        if (showsManagementActions) {
            Surface(
                onClick = onDeleteChat,
                enabled = enabled && canDelete,
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.ai_session_delete),
                    )
                }
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
    onRenameActiveSession: () -> Unit,
    onDeleteActiveSession: () -> Unit,
) {
    val activeSession = sessions.firstOrNull { it.sessionId == activeSessionId }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingTick)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stringResource(R.string.ai_sessions_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            if (activeSession != null) {
                Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                    Text(
                        text = stringResource(R.string.ai_active_chat),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                    SkydownPremiumTextField(
                        value = renameDraft,
                        onValueChange = onRenameDraftChanged,
                        label = stringResource(R.string.ai_active_chat),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        singleLine = true,
                        supportingText = activeSession.title,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BrandActionButton(
                            text = stringResource(R.string.ai_action_delete),
                            onClick = onDeleteActiveSession,
                            accent = MaterialTheme.colorScheme.error,
                            icon = Icons.Default.Delete,
                            filled = false,
                            compact = true,
                            enabled = enabled,
                        )
                        BrandActionButton(
                            text = stringResource(R.string.ai_action_rename),
                            onClick = onRenameActiveSession,
                            accent = accent,
                            compact = true,
                            enabled = enabled && renameDraft.trim().isNotBlank(),
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.ai_sessions_label),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )

            if (sessions.isEmpty()) {
                Text(
                    text = stringResource(R.string.ai_sessions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                    sessions.forEach { session ->
                        val selected = session.sessionId == activeSessionId
                        val countLabel = when (session.promptCount) {
                            0 -> stringResource(R.string.ai_session_count_new)
                            1 -> stringResource(R.string.ai_session_count_one)
                            else -> stringResource(R.string.ai_session_count_many, session.promptCount)
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
                                    shape = RoundedCornerShape(SkydownUiTokens.elevatedPanelRadius),
                                )
                                .clickable(enabled = enabled) {
                                    onSelectSession(session.sessionId)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
                        ) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = session.preview.ifBlank { stringResource(R.string.ai_session_preview_empty) },
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
