package com.nash.skyos.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nash.skyos.R

@Composable
fun EditableVideoFieldCard(
    title: String,
    videoUrl: String,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveVideo: (() -> Unit)? = null,
    buttonLabel: String = "",
    isUploading: Boolean = false,
    enabled: Boolean = true,
    uploadStatusText: String = "",
) {
    val resolvedButtonLabel = buttonLabel.ifBlank { stringResource(R.string.artist_hero_video_pick_button) }
    val resolvedUploadStatusText = uploadStatusText.ifBlank { stringResource(R.string.artist_hero_video_upload_status) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                )
                Text(
                    text = if (videoUrl.isBlank()) {
                        stringResource(R.string.artist_hero_video_empty_title)
                    } else {
                        stringResource(R.string.artist_hero_video_active_title)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (videoUrl.isBlank()) {
                        stringResource(R.string.artist_hero_video_empty_hint)
                    } else {
                        stringResource(R.string.artist_hero_video_active_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f)),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = resolvedUploadStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onPickVideo,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !isUploading,
        ) {
            Text(resolvedButtonLabel)
        }

        if (videoUrl.isNotBlank()) {
            TextButton(
                onClick = { onRemoveVideo?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && !isUploading,
            ) {
                Text(stringResource(R.string.common_remove))
            }
        }
    }
}
