package com.skydown.android.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.skydown.android.data.AppContainer

@Composable
fun RowScope.AppTopBarSessionActions(
    onOpenCart: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val currentUser = AppContainer.currentUser.collectAsStateWithLifecycle().value
    val compactLayout = rememberIsCompactAppLayout()
    val displayName = currentUser?.username?.trim().takeUnless { it.isNullOrBlank() } ?: "Gast"
    val initials = displayName.firstOrNull()?.uppercase() ?: "G"

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        tonalElevation = 4.dp,
        modifier = Modifier.clickable {
            if (currentUser != null) {
                (onOpenProfile ?: onOpenSettings).invoke()
            } else {
                onOpenSettings()
            }
        },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compactLayout) 8.dp else 10.dp,
                vertical = if (compactLayout) 5.dp else 7.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compactLayout) 22.dp else 23.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!currentUser?.profileImageURL.isNullOrBlank()) {
                        AsyncImage(
                            model = currentUser.profileImageURL,
                            contentDescription = "Profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (!compactLayout) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }

    trailingContent()

    if (onOpenCart != null) {
        SessionIconAction(
            onClick = onOpenCart,
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Warenkorb",
            compactLayout = compactLayout,
        )
    }

    SessionIconAction(
        onClick = onOpenSettings,
        imageVector = Icons.Default.Settings,
        contentDescription = "Einstellungen",
        compactLayout = compactLayout,
    )
}

@Composable
private fun SessionIconAction(
    onClick: () -> Unit,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    compactLayout: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        tonalElevation = 4.dp,
        modifier = Modifier.skydownPressable(interactionSource),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(if (compactLayout) 36.dp else 38.dp),
            interactionSource = interactionSource,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
            )
        }
    }
}
