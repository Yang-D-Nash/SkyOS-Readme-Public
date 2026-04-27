package com.nash.skyos.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.nash.skyos.R
import com.nash.skyos.data.resolveYouTubeEmbedUrl
import com.nash.skyos.data.resolveYouTubeExternalUrl
import com.nash.skyos.ui.model.VideoYouTubeItem

@Composable
fun YouTubePlayerDialog(
    item: VideoYouTubeItem,
    onDismiss: () -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    val embedUrl = androidx.compose.runtime.remember(item.url) { resolveYouTubeEmbedUrl(item.url) }
    val externalUrl = androidx.compose.runtime.remember(item.url) { resolveYouTubeExternalUrl(item.url) ?: item.url }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SkydownCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SkydownUiTokens.screenHorizontalPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                    if (item.highlight.isNotBlank()) {
                        Text(
                            text = item.highlight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                        )
                    }
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                    )
                }
            }

            if (embedUrl != null) {
                ExternalVideoWebPlayer(
                    url = item.url,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(SkydownUiTokens.cardCornerRadius)),
                )
            } else {
                Text(
                    text = stringResource(R.string.youtube_player_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
            ) {
                FilledTonalButton(
                    onClick = { onOpenExternal(externalUrl) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.youtube_watch))
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}
