package com.nash.skyos.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nash.skyos.R
import com.nash.skyos.data.AppCartStore

private object TopBarPreset {
    fun useDense(compactLayout: Boolean, dense: Boolean): Boolean = dense && compactLayout
    fun useMiniMode(compactLayout: Boolean, miniModeOnCompact: Boolean): Boolean = miniModeOnCompact && compactLayout
    fun sessionHorizontalPadding(compactLayout: Boolean, dense: Boolean) = when {
        dense && compactLayout -> 6.dp
        dense -> 7.dp
        compactLayout -> 9.dp
        else -> 10.dp
    }
    fun sessionVerticalPadding(compactLayout: Boolean, dense: Boolean) = when {
        dense && compactLayout -> 2.dp
        dense -> 4.dp
        compactLayout -> 5.dp
        else -> 6.dp
    }
    fun sessionSpacing(compactLayout: Boolean, dense: Boolean) = when {
        dense && compactLayout -> 4.dp
        compactLayout -> 6.dp
        dense -> 5.dp
        else -> 8.dp
    }
    fun avatarSize(compactLayout: Boolean, dense: Boolean) = when {
        dense && compactLayout -> 18.dp
        dense -> 20.dp
        compactLayout -> 26.dp
        else -> 28.dp
    }
    fun actionIconInset(compactLayout: Boolean, dense: Boolean) = when {
        dense && compactLayout -> 5.dp
        dense -> 4.dp
        compactLayout -> 3.dp
        else -> 2.dp
    }
}

@Composable
fun RowScope.AppTopBarSessionActions(
    onOpenCart: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    onGuestSignIn: (() -> Unit)? = null,
    dense: Boolean = true,
    collapseIdentityOnCompact: Boolean = true,
    miniModeOnCompact: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    // Balanced preset:
    // - compact (phone): dense controls + mini menu
    // - regular (tablet/desktop): readable identity + direct action buttons
    val currentUser = LocalSessionUser.current
    val compactLayout = rememberIsCompactAppLayout()
    val balancedDense = TopBarPreset.useDense(compactLayout = compactLayout, dense = dense)
    val balancedMiniMode = TopBarPreset.useMiniMode(
        compactLayout = compactLayout,
        miniModeOnCompact = miniModeOnCompact,
    )
    val guestName = stringResource(R.string.app_topbar_guest_name)
    val displayName = currentUser?.username?.trim().takeUnless { it.isNullOrBlank() } ?: guestName
    val initials = displayName.firstOrNull()?.uppercase() ?: guestName.firstOrNull()?.uppercase().orEmpty()
    val sessionAccent = if (currentUser == null) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val sessionInteractionSource = remember { MutableInteractionSource() }
    val showIdentityLabel = !balancedDense && !(collapseIdentityOnCompact && compactLayout)
    val useMiniMode = balancedMiniMode
    val showGuestSignIn = currentUser == null && onGuestSignIn != null && showIdentityLabel
    val cartItems by AppCartStore.items.collectAsState()
    val cartCount = cartItems.size
    var menuExpanded by remember { mutableStateOf(false) }

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
            .testTag("app.topbar.session")
            .padding(
                horizontal = TopBarPreset.sessionHorizontalPadding(
                    compactLayout = compactLayout,
                    dense = balancedDense,
                ),
                vertical = TopBarPreset.sessionVerticalPadding(
                    compactLayout = compactLayout,
                    dense = balancedDense,
                ),
            ),
        horizontalArrangement = Arrangement.spacedBy(TopBarPreset.sessionSpacing(compactLayout = compactLayout, dense = balancedDense)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(TopBarPreset.avatarSize(compactLayout = compactLayout, dense = balancedDense))
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
                        contentDescription = stringResource(R.string.profile_avatar_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelSmall,
                        color = sessionAccent,
                        fontWeight = FontWeight.SemiBold,
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

        if (showIdentityLabel) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingHairline),
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
    }

    if (showGuestSignIn && !useMiniMode) {
        Spacer(modifier = Modifier.width(4.dp))
        val guestInteractionSource = remember { MutableInteractionSource() }
        Text(
            text = stringResource(R.string.auth_session_sign_in),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
            modifier = Modifier
                .clip(RoundedCornerShape(SkydownUiTokens.microCorner))
                .clickable(
                    interactionSource = guestInteractionSource,
                    indication = null,
                    onClick = onGuestSignIn,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }

    trailingContent()

    if (useMiniMode) {
        SessionIconAction(
            onClick = { menuExpanded = true },
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = stringResource(R.string.app_topbar_more_cd),
            testTag = "app.topbar.more",
            compactLayout = compactLayout,
            dense = balancedDense,
            accentColor = MaterialTheme.colorScheme.tertiary,
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            if (onOpenCart != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.cart_title)) },
                    onClick = {
                        menuExpanded = false
                        onOpenCart()
                    },
                )
            }
            if (currentUser != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_title)) },
                    onClick = {
                        menuExpanded = false
                        (onOpenProfile ?: onOpenSettings).invoke()
                    },
                )
            }
            if (currentUser == null && onGuestSignIn != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.auth_session_sign_in)) },
                    onClick = {
                        menuExpanded = false
                        onGuestSignIn()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.app_topbar_settings_label)) },
                onClick = {
                    menuExpanded = false
                    onOpenSettings()
                },
            )
        }
        return
    }

    if (onOpenCart != null) {
        SessionIconAction(
            onClick = onOpenCart,
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = stringResource(R.string.cart_title),
            testTag = "app.topbar.cart",
            compactLayout = compactLayout,
            dense = balancedDense,
            accentColor = MaterialTheme.colorScheme.secondary,
            badgeCount = cartCount,
        )
    }

    SessionIconAction(
        onClick = onOpenSettings,
        imageVector = Icons.Default.Settings,
        contentDescription = stringResource(R.string.app_topbar_settings_label),
        testTag = "app.topbar.settings",
        compactLayout = compactLayout,
        dense = balancedDense,
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
    badgeCount: Int = 0,
) {
    Box {
        SkydownPremiumIconAction(
            icon = imageVector,
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = Modifier.testTag(testTag),
            accent = accentColor.copy(alpha = 0.94f),
            size = 40.dp,
            iconSize = if (TopBarPreset.useDense(compactLayout, dense)) 17.dp else 19.dp,
            shape = CircleShape,
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
                    .background(accentColor.copy(alpha = 0.96f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.surface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
