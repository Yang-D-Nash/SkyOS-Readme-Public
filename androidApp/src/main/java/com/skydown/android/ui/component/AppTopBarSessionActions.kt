package com.skydown.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydown.android.data.AppContainer

@Composable
fun RowScope.AppTopBarSessionActions(
    onOpenCart: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val currentUser = AppContainer.currentUser.collectAsStateWithLifecycle().value
    val compactLayout = rememberIsCompactAppLayout()
    val displayName = currentUser?.username?.trim().takeUnless { it.isNullOrBlank() } ?: "Gast"
    val initials = displayName.firstOrNull()?.uppercase() ?: "G"

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compactLayout) 8.dp else 10.dp,
                vertical = if (compactLayout) 6.dp else 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(if (compactLayout) 22.dp else 24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (!compactLayout) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelLarge,
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
        IconButton(onClick = onOpenCart) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Warenkorb",
            )
        }
    }

    IconButton(onClick = onOpenSettings) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Einstellungen",
        )
    }
}
