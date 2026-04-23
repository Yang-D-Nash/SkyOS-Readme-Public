package com.skydown.android.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
    val sessionAccent = if (currentUser == null) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val sessionInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .skydownCapsuleSurface(accent = sessionAccent)
            .skydownPressable(sessionInteractionSource)
            .clickable(
                interactionSource = sessionInteractionSource,
                indication = null,
            ) {
                if (currentUser != null) {
                    (onOpenProfile ?: onOpenSettings).invoke()
                } else {
                    onOpenSettings()
                }
            }
            .padding(
                horizontal = when {
                    dense && compactLayout -> 7.dp
                    dense -> 8.dp
                    compactLayout -> 9.dp
                    else -> 10.dp
                },
                vertical = when {
                    dense && compactLayout -> 3.dp
                    dense -> 4.dp
                    compactLayout -> 5.dp
                    else -> 6.dp
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
            Box(
                modifier = Modifier.fillMaxSize(),
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
                        style = MaterialTheme.typography.labelSmall,
                        color = sessionAccent,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(sessionAccent)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape,
                        ),
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                modifier = Modifier.size(16.dp),
            )
        }
    }

    trailingContent()

    if (onOpenCart != null) {
        SessionIconAction(
            onClick = onOpenCart,
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Warenkorb",
            testTag = "app.topbar.cart",
            compactLayout = compactLayout,
            dense = dense,
            accentColor = MaterialTheme.colorScheme.secondary,
        )
    }

    SessionIconAction(
        onClick = onOpenSettings,
        imageVector = Icons.Default.Settings,
        contentDescription = "Einstellungen",
        testTag = "app.topbar.settings",
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
    testTag: String,
    compactLayout: Boolean,
    dense: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .skydownCapsuleSurface(accent = accentColor, shape = CircleShape)
            .skydownPressable(interactionSource),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(
                    when {
                        dense && compactLayout -> 30.dp
                        dense -> 32.dp
                        compactLayout -> 34.dp
                        else -> 36.dp
                    },
                )
                .testTag(testTag),
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
