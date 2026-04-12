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
import androidx.compose.material.icons.filled.Photo
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun EditableImageFieldCard(
    title: String,
    imageUrl: String,
    onPickImage: () -> Unit,
    onImageUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveImage: (() -> Unit)? = null,
    buttonLabel: String = "Vom Handy waehlen",
    isUploading: Boolean = false,
    enabled: Boolean = true,
    uploadStatusText: String = "Bild wird vorbereitet und hochgeladen.",
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
                .height(136.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)),
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.06f),
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f),
                                ),
                            ),
                        ),
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = null,
                    )
                    Text(
                        text = "Noch kein Bild",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (isUploading) "Upload laeuft" else if (imageUrl.isBlank()) "Noch kein Bild" else "Bild aktiv",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (isUploading) {
                        uploadStatusText
                    } else if (imageUrl.isBlank()) {
                        "Wird nach dem Upload direkt gesetzt."
                    } else {
                        "Die App dunkelt es automatisch fuer Text ab."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
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
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !isUploading,
        ) {
            Text(buttonLabel)
        }

        if (imageUrl.isNotBlank()) {
            TextButton(
                onClick = {
                    if (onRemoveImage != null) {
                        onRemoveImage()
                    } else {
                        onImageUrlChange("")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && !isUploading,
            ) {
                Text("Bild entfernen")
            }
        }
    }
}
