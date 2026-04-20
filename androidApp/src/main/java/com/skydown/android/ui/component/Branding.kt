package com.skydown.android.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.skydown.android.R
import com.skydown.android.ui.theme.DexBlueDeep
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.Role

enum class BrandArtwork(
    @param:DrawableRes val drawableRes: Int,
    val label: String,
) {
    Sky22(
        drawableRes = R.drawable.sky22_brand_logo,
        label = "22",
    ),
    Skydown(
        drawableRes = R.drawable.skydown_brand_logo,
        label = "Skydown",
    ),
    Zweizwei(
        drawableRes = R.drawable.sky22_brand_logo,
        label = "22",
    ),
    Combined(
        drawableRes = R.drawable.skydown_x22_brand_logo,
        label = "SkyOs",
    ),
}

@Composable
fun BrandHeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    detail: String? = null,
    backgroundImageUrl: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    secondaryAccent: Color = MaterialTheme.colorScheme.secondary,
    marks: List<BrandArtwork> = emptyList(),
    compactVisualDensity: Boolean = false,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(SkydownUiTokens.heroCornerRadius)
    val imageShape = RoundedCornerShape(if (compactVisualDensity) 20.dp else 22.dp)
    val imageAspectRatio = if (compactVisualDensity) 2.72f else 2.32f
    val hasBackgroundImage = !backgroundImageUrl.isNullOrBlank()
    val titleColor = if (hasBackgroundImage) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (hasBackgroundImage) Color.White.copy(alpha = 0.84f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    val detailColor = if (hasBackgroundImage) Color.White.copy(alpha = 0.96f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    val outerImageHorizontalPadding = if (compactVisualDensity) 7.dp else 9.dp
    val outerImageVerticalPadding = if (compactVisualDensity) 5.dp else 7.dp
    val contentHorizontalPadding = if (compactVisualDensity) 14.dp else 16.dp
    val contentVerticalPadding = if (compactVisualDensity) 12.dp else 14.dp
    val contentSpacing = if (compactVisualDensity) 6.dp else 8.dp
    val titleStyle = if (compactVisualDensity) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall
    val subtitleStyle = if (compactVisualDensity) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall
    val detailStyle = if (compactVisualDensity) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (compactVisualDensity) 10.dp else 14.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.12f),
                spotColor = accent.copy(alpha = 0.22f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.995f),
                        accent.copy(alpha = 0.18f),
                        secondaryAccent.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f),
                        Color.Black.copy(alpha = 0.08f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        accent.copy(alpha = 0.34f),
                        secondaryAccent.copy(alpha = 0.24f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    ),
                ),
                shape = shape,
            )
            .skydownSheen(accent = accent, alpha = 0.10f),
    ) {
        if (hasBackgroundImage) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = outerImageHorizontalPadding, vertical = outerImageVerticalPadding),
            ) {
                val imageFrameHeight = maxWidth / imageAspectRatio

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageFrameHeight)
                        .clip(imageShape),
                ) {
                    AsyncImage(
                        model = backgroundImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.20f)),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.06f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.30f),
                                    ),
                                ),
                            ),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.16f),
                                        Color.Transparent,
                                        secondaryAccent.copy(alpha = 0.14f),
                                    ),
                                ),
                            ),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (compactVisualDensity) 88.dp else 102.dp)
                        .blur(if (compactVisualDensity) 24.dp else 30.dp)
                        .background(accent.copy(alpha = 0.08f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(if (compactVisualDensity) 64.dp else 76.dp)
                        .blur(if (compactVisualDensity) 18.dp else 24.dp)
                        .background(secondaryAccent.copy(alpha = 0.08f), CircleShape),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(if (compactVisualDensity) 6.dp else 8.dp),
                    ) {
                        Text(
                            text = eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = title,
                            style = titleStyle,
                            color = titleColor,
                            fontWeight = FontWeight.Black,
                            maxLines = if (compactVisualDensity) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = subtitleStyle,
                            color = subtitleColor,
                                maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!detail.isNullOrBlank()) {
                            Text(
                                text = detail,
                                style = detailStyle,
                                color = detailColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (marks.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .width(
                                    if (compactVisualDensity) {
                                        if (marks.size == 1) 82.dp else 72.dp
                                    } else {
                                        if (marks.size == 1) 90.dp else 78.dp
                                    },
                                ),
                            verticalArrangement = Arrangement.spacedBy(if (compactVisualDensity) 6.dp else 8.dp),
                        ) {
                            marks.take(2).forEach { mark ->
                                BrandArtworkTile(
                                    mark = mark,
                                    accent = accent,
                                    isFeatured = marks.size == 1,
                                )
                            }
                        }
                    }
                }
            }

            footer()
        }
    }
}

@Composable
fun BrandHeroMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = true,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.995f),
                        accent.copy(alpha = if (isActive) 0.22f else 0.10f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        accent.copy(alpha = if (isActive) 0.28f else 0.16f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = if (isActive) 0.18f else 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isActive) 0.96f else 0.72f),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun BrandSectionBanner(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tag: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.24f),
                                accent.copy(alpha = 0.10f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let { copy ->
                Text(
                    text = copy,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            }
        }

        tag?.takeIf { it.isNotBlank() }?.let { label ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.20f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun BrandStatusChip(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val contentColor = if (isActive) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
    val interactionSource = remember(onClick) { MutableInteractionSource() }
    val chipModifier = if (onClick != null) {
        modifier
            .skydownPressable(
                interactionSource = interactionSource,
                pressedScale = 0.992f,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
    } else {
        modifier
    }

    if (onClick != null) {
        val backgroundBrush = if (isActive) {
            Brush.linearGradient(
                colors = listOf(
                    accent.copy(alpha = 0.18f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
                ),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    DexBlueDeep.copy(alpha = 0.08f),
                ),
            )
        }

        Row(
            modifier = chipModifier
                .clip(RoundedCornerShape(999.dp))
                .background(backgroundBrush)
                .border(
                    width = 1.dp,
                    color = if (isActive) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(999.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.82f),
                modifier = Modifier.size(14.dp),
            )
        }
    } else {
        Row(
            modifier = chipModifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.88f),
                    modifier = Modifier.size(15.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = contentColor.copy(alpha = if (isActive) 0.76f else 0.42f),
                            shape = CircleShape,
                        ),
                )
            }
            Text(
                text = text,
                color = contentColor.copy(alpha = if (isActive) 0.92f else 0.74f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun BrandActionButton(
    text: String,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    filled: Boolean = true,
    compact: Boolean = false,
    isLoading: Boolean = false,
) {
    val shape = RoundedCornerShape(if (compact) 17.dp else 20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val buttonEnabled = enabled && !isLoading
    val contentPadding = if (compact) {
        PaddingValues(horizontal = 15.dp, vertical = 10.dp)
    } else {
        PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    }
    val filledContentColor = if (accent.perceivedBrightness() < 0.58f) Color.White else DexBlueDeep
    val iconTint = if (filled) filledContentColor else accent
    val filledBrush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = if (buttonEnabled) 1f else 0.42f),
            accent.copy(alpha = if (buttonEnabled) 0.88f else 0.30f),
            Color.White.copy(alpha = if (buttonEnabled) 0.12f else 0.04f),
        ),
    )
    val filledBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (buttonEnabled) 0.28f else 0.10f),
            accent.copy(alpha = if (buttonEnabled) 0.42f else 0.20f),
        ),
    )
    val outlineBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (buttonEnabled) 0.18f else 0.06f),
            accent.copy(alpha = if (buttonEnabled) 0.16f else 0.08f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
    )

    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = if (compact) 42.dp else 50.dp)
                .shadow(
                    elevation = if (enabled) 10.dp else 0.dp,
                    shape = shape,
                    ambientColor = accent.copy(alpha = 0.18f),
                    spotColor = accent.copy(alpha = 0.22f),
                )
                .clip(shape)
                .background(filledBrush)
                .border(
                    width = 1.dp,
                    brush = filledBorderBrush,
                    shape = shape,
                )
                .skydownPressable(interactionSource, pressedScale = if (compact) 0.984f else 0.98f),
            enabled = buttonEnabled,
            shape = shape,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = filledContentColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = filledContentColor.copy(alpha = 0.72f),
            ),
            contentPadding = contentPadding,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                    strokeWidth = 2.dp,
                    color = filledContentColor,
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = if (icon != null || isLoading) 8.dp else 0.dp),
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = if (compact) 42.dp else 50.dp)
                .clip(shape)
                .background(outlineBrush)
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (buttonEnabled) 0.22f else 0.08f),
                            accent.copy(alpha = if (buttonEnabled) 0.44f else 0.18f),
                        ),
                    ),
                    shape = shape,
                )
                .skydownPressable(interactionSource, pressedScale = if (compact) 0.986f else 0.982f),
            enabled = buttonEnabled,
            shape = shape,
            interactionSource = interactionSource,
            border = BorderStroke(
                width = 0.dp,
                color = Color.Transparent,
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = accent,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = accent.copy(alpha = 0.42f),
            ),
            contentPadding = contentPadding,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                    strokeWidth = 2.dp,
                    color = accent,
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = if (icon != null || isLoading) 8.dp else 0.dp),
            )
        }
    }
}

private fun Color.perceivedBrightness(): Float = (red * 0.299f) + (green * 0.587f) + (blue * 0.114f)

@Composable
fun BrandPill(
    text: String,
    tint: Color,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember(onClick) { MutableInteractionSource() }
    val pillModifier = if (onClick != null) {
        Modifier
            .skydownPressable(
                interactionSource = interactionSource,
                pressedScale = 0.993f,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
    } else {
        Modifier
    }

    if (onClick != null) {
        Box(
            modifier = pillModifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            tint.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            tint.copy(alpha = 0.28f),
                        ),
                    ),
                    shape = RoundedCornerShape(999.dp),
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = tint.copy(alpha = 0.82f),
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(4.dp)
                    .background(
                        color = tint.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(999.dp),
                    ),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = tint.copy(alpha = 0.92f),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun BrandArtworkTile(
    mark: BrandArtwork,
    accent: Color,
    isFeatured: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isFeatured) 90.dp else 72.dp)
            .clip(RoundedCornerShape(SkydownUiTokens.cardCornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        accent.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                        Color.Black.copy(alpha = 0.12f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        accent.copy(alpha = 0.22f),
                    ),
                ),
                shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Image(
                painter = painterResource(id = mark.drawableRes),
                contentDescription = mark.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFeatured) 42.dp else 30.dp)
                    .clip(RoundedCornerShape(SkydownUiTokens.buttonCornerRadius)),
                contentScale = ContentScale.Fit,
            )

            Text(
                text = mark.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
