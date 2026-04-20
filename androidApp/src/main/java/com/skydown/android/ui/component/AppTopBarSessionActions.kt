package com.skydown.android.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import coil3.compose.AsyncImage

@Composable
fun RowScope.AppTopBarSessionActions(
    onOpenCart: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    dense: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val currentUser = LocalSessionUser.current
    val compactLayout = rememberIsCompactAppLayout()
    val displayName = currentUser?.username?.trim().takeUnless { it.isNullOrBlank() } ?: "Gast"
    val initials = displayName.firstOrNull()?.uppercase() ?: "G"
    val sessionLabel = if (currentUser == null) "Gastmodus" else "Session"
    val showsCompactIdentity = dense
    val sessionAccent = if (currentUser == null) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
        shape = RoundedCornerShape(if (dense) 18.dp else 20.dp),
        border = BorderStroke(1.dp, sessionAccent.copy(alpha = 0.20f)),
        tonalElevation = 6.dp,
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
                horizontal = when {
                    dense && compactLayout -> 6.dp
                    dense -> 8.dp
                    compactLayout -> 9.dp
                    else -> 12.dp
                },
                vertical = when {
                    dense && compactLayout -> 3.dp
                    dense -> 4.dp
                    compactLayout -> 6.dp
                    else -> 8.dp
                },
            ),
            horizontalArrangement = Arrangement.spacedBy(
                when {
                    dense && compactLayout -> 4.dp
                    compactLayout -> 6.dp
                    dense -> 5.dp
                    else -> 8.dp
                },
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    when {
                        dense && compactLayout -> 4.dp
                        compactLayout -> 6.dp
                        dense -> 5.dp
                        else -> 8.dp
                    },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(
                            when {
                                dense && compactLayout -> 20.dp
                                dense -> 22.dp
                                compactLayout -> 26.dp
                                else -> 28.dp
                            },
                        )
                        .clip(CircleShape)
                        .background(sessionAccent.copy(alpha = 0.16f)),
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
                            color = sessionAccent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    if (!showsCompactIdentity) {
                        Text(
                            text = sessionLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = sessionAccent.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (showsCompactIdentity || !compactLayout) {
                        Text(
                            text = displayName,
                            style = if (showsCompactIdentity) {
                                MaterialTheme.typography.labelMedium
                            } else {
                                MaterialTheme.typography.labelMedium
                            },
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (showsCompactIdentity) 0.90f else 1f),
                            fontWeight = if (showsCompactIdentity) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
            dense = dense,
            accentColor = MaterialTheme.colorScheme.secondary,
        )
    }

    SessionIconAction(
        onClick = onOpenSettings,
        imageVector = Icons.Default.Settings,
        contentDescription = "Einstellungen",
        compactLayout = compactLayout,
        dense = dense,
        accentColor = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun SessionIconAction(
    onClick: () -> Unit,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    compactLayout: Boolean,
    dense: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
        shape = CircleShape,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f)),
        tonalElevation = 6.dp,
        modifier = Modifier.skydownPressable(interactionSource),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(
                when {
                    dense && compactLayout -> 30.dp
                    dense -> 32.dp
                    compactLayout -> 38.dp
                    else -> 40.dp
                },
            ),
            interactionSource = interactionSource,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = accentColor.copy(alpha = 0.94f),
            )
        }
    }
}
