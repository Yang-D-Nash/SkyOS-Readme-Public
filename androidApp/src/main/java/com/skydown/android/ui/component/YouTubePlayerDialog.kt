package com.skydown.android.ui.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.skydown.android.data.resolveYouTubeEmbedUrl
import com.skydown.android.data.resolveYouTubeExternalUrl
import com.skydown.android.ui.model.VideoYouTubeItem

@androidx.compose.runtime.Composable
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
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                        contentDescription = "Dialog schliessen",
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
                        .clip(RoundedCornerShape(22.dp)),
                )
            } else {
                Text(
                    text = "Der YouTube Player konnte fuer dieses Video nicht aufgebaut werden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = { onOpenExternal(externalUrl) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("YouTube ansehen")
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Schliessen")
                }
            }
        }
    }
}
