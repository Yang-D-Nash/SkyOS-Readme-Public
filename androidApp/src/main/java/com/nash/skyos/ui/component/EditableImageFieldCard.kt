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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nash.skyos.R

@Composable
fun EditableImageFieldCard(
    title: String,
    imageUrl: String,
    onPickImage: () -> Unit,
    onImageUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveImage: (() -> Unit)? = null,
    buttonLabel: String = "",
    isUploading: Boolean = false,
    enabled: Boolean = true,
    uploadStatusText: String = "",
) {
    val resolvedButtonLabel = buttonLabel.ifBlank { stringResource(R.string.editable_image_pick_from_device) }
    val resolvedUploadStatusText = uploadStatusText.ifBlank { stringResource(R.string.editable_image_uploading) }
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
                .height(136.dp)
                .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
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
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(R.string.editable_image_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
            ) {
                Text(
                    text = if (isUploading) {
                        stringResource(R.string.editable_image_upload_in_progress)
                    } else if (imageUrl.isBlank()) {
                        stringResource(R.string.editable_image_none)
                    } else {
                        stringResource(R.string.editable_image_active)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (isUploading) {
                        resolvedUploadStatusText
                    } else if (imageUrl.isBlank()) {
                        stringResource(R.string.editable_image_pending_apply)
                    } else {
                        stringResource(R.string.editable_image_auto_dimmed)
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
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !isUploading,
        ) {
            Text(resolvedButtonLabel)
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
                Text(stringResource(R.string.editable_image_remove))
            }
        }
    }
}
