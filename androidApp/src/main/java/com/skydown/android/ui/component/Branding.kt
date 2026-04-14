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
        label = "22xSky",
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
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(SkydownUiTokens.heroCornerRadius)
    val imageShape = RoundedCornerShape(22.dp)
    val imageAspectRatio = 2.08f
    val hasBackgroundImage = !backgroundImageUrl.isNullOrBlank()
    val titleColor = if (hasBackgroundImage) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (hasBackgroundImage) Color.White.copy(alpha = 0.84f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    val detailColor = if (hasBackgroundImage) Color.White.copy(alpha = 0.96f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        accent.copy(alpha = 0.10f),
                        secondaryAccent.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.16f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.26f),
                        secondaryAccent.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                    ),
                ),
                shape = shape,
            ),
    ) {
        if (hasBackgroundImage) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
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
                            .background(Color.Black.copy(alpha = 0.38f)),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.18f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.48f),
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
                                        accent.copy(alpha = 0.10f),
                                        Color.Transparent,
                                        secondaryAccent.copy(alpha = 0.10f),
                                    ),
                                ),
                            ),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(84.dp)
                        .blur(24.dp)
                        .background(accent.copy(alpha = 0.04f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(62.dp)
                        .blur(20.dp)
                        .background(secondaryAccent.copy(alpha = 0.04f), CircleShape),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = titleColor,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!detail.isNullOrBlank()) {
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.labelMedium,
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
                                .width(if (marks.size == 1) 82.dp else 72.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
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
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DexBlueDeep.copy(alpha = 0.82f),
                        accent.copy(alpha = if (isActive) 0.22f else 0.10f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (isActive) 0.30f else 0.16f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        .background(accent.copy(alpha = if (isActive) 0.20f else 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) accent else Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.74f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = if (isActive) 0.96f else 0.72f),
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
                style = MaterialTheme.typography.titleSmall,
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
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.82f),
                modifier = Modifier.size(14.dp),
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
    val shape = RoundedCornerShape(if (compact) 16.dp else 18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val contentPadding = if (compact) {
        PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    } else {
        PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    }
    val filledContentColor = if (accent.perceivedBrightness() < 0.58f) Color.White else DexBlueDeep
    val iconTint = if (filled) filledContentColor else accent

    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier
                .shadow(
                    elevation = if (enabled) 10.dp else 0.dp,
                    shape = shape,
                    ambientColor = accent.copy(alpha = 0.18f),
                    spotColor = accent.copy(alpha = 0.22f),
                )
                .skydownPressable(interactionSource, pressedScale = if (compact) 0.984f else 0.98f),
            enabled = enabled && !isLoading,
            shape = shape,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = filledContentColor,
                disabledContainerColor = accent.copy(alpha = 0.34f),
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
                modifier = Modifier.padding(start = if (icon != null || isLoading) 8.dp else 0.dp),
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .skydownPressable(interactionSource, pressedScale = if (compact) 0.986f else 0.982f),
            enabled = enabled && !isLoading,
            shape = shape,
            interactionSource = interactionSource,
            border = BorderStroke(
                width = 1.2.dp,
                color = if (enabled) accent.copy(alpha = 0.58f) else accent.copy(alpha = 0.26f),
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = accent.copy(alpha = 0.16f),
                contentColor = accent,
                disabledContainerColor = accent.copy(alpha = 0.08f),
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

    Box(
        modifier = pillModifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = tint.copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = if (onClick != null) 12.dp else 10.dp, vertical = 6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = FontWeight.SemiBold,
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = tint.copy(alpha = 0.82f),
                    modifier = Modifier.size(12.dp),
                )
            }
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
            .height(if (isFeatured) 74.dp else 60.dp)
            .clip(RoundedCornerShape(SkydownUiTokens.cardCornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = mark.drawableRes),
            contentDescription = mark.label,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFeatured) 38.dp else 30.dp)
                .clip(RoundedCornerShape(SkydownUiTokens.buttonCornerRadius)),
            contentScale = ContentScale.Fit,
        )
    }
}
