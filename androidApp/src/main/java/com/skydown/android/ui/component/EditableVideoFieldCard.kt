package com.skydown.android.ui.component

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EditableVideoFieldCard(
    title: String,
    videoUrl: String,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveVideo: (() -> Unit)? = null,
    buttonLabel: String = "Video vom Handy waehlen",
    isUploading: Boolean = false,
    enabled: Boolean = true,
    uploadStatusText: String = "Video wird vorbereitet und hochgeladen.",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                .clip(RoundedCornerShape(18.dp))
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                )
                Text(
                    text = if (videoUrl.isBlank()) "Noch kein Hero-Video" else "Hero-Video aktiv",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (videoUrl.isBlank()) {
                        "Ein kurzes Motion-Video macht die Artist-Stage deutlich lebendiger."
                    } else {
                        "Das Video wird auf der Artist-Seite direkt als Motion-Stage abgespielt."
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = uploadStatusText,
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
            Text(buttonLabel)
        }

        if (videoUrl.isNotBlank()) {
            TextButton(
                onClick = { onRemoveVideo?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && !isUploading,
            ) {
                Text("Video entfernen")
            }
        }
    }
}
