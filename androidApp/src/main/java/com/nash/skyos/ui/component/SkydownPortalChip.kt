package com.nash.skyos.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nash.skyos.ui.theme.skydownSecondaryBackground

/**
 * Compact portal / lane chip (icon + label) used for horizontal home shortcuts and similar flows.
 * Pairs with the SwiftUI `SkydownPortalChip`; spacing and radii follow `SkydownUiTokens`.
 */
@Composable
fun SkydownPortalChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    Surface(
        onClick = {
            view.performSkydownHaptic(SkydownHapticKind.Selection)
            onClick()
        },
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
        color = colorScheme.skydownSecondaryBackground().copy(alpha = 0.72f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = tint,
                )
            }
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = tint,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
