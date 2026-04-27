package com.nash.skyos.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nash.skyos.R
import com.nash.skyos.ui.theme.SkydownBodyCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownCardTitleTextStyle
import com.nash.skyos.ui.theme.SkydownCompactHeroTitleTextStyle
import com.nash.skyos.ui.theme.SkydownEditorialCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownHeroEyebrowTextStyle
import com.nash.skyos.ui.theme.SkydownHeroSubtitleTextStyle
import com.nash.skyos.ui.theme.SkydownHeroTitleTextStyle
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownCinematicShadow
import com.nash.skyos.ui.theme.skydownDeepInk
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownLuminanceLift
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownSecondaryText
import com.nash.skyos.ui.theme.skydownText
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.Role
import java.util.Locale

enum class BrandArtwork(
    @param:DrawableRes val drawableRes: Int,
    @param:StringRes val labelRes: Int,
) {
    SkyOS(
        drawableRes = R.drawable.skyos_brand_logo,
        labelRes = R.string.brand_system_name,
    ),
    Sky22(
        drawableRes = R.drawable.sky22_brand_logo,
        labelRes = R.string.brand_music_name,
    ),
    Skydown(
        drawableRes = R.drawable.skydown_brand_logo,
        labelRes = R.string.brand_product_name,
    ),
    Zweizwei(
        drawableRes = R.drawable.zweizwei_brand_logo,
        labelRes = R.string.brand_music_name,
    ),
    Combined(
        drawableRes = R.drawable.skydown_x22_brand_logo,
        labelRes = R.string.brand_merch_name,
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
    /** Flache Unterkante, kein Karten-Schatten — für Home-Gesamtraum. */
    immersive: Boolean = false,
    /** Volle Header-Buehne statt Card im gepolsterten Scroll-Stack. */
    edgeToEdge: Boolean = false,
    /** Zusaetzlicher Abstand fuer Inhalte, wenn die Buehne hinter eine transparente Topbar laeuft. */
    topContentPadding: Dp = 0.dp,
    /** Tap auf Titelzeile (ohne Footer mit Pills) — gleiche Fläche wie iOS `onSurfaceTap`. */
    onSurfaceClick: (() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val autoCompactDensity = compactVisualDensity || rememberUsesCompactVisualDensity()
    val colorScheme = MaterialTheme.colorScheme
    val r = SkydownUiTokens.heroCornerRadius
    val shape = when {
        edgeToEdge -> RoundedCornerShape(r)
        immersive -> RoundedCornerShape(topStart = r, topEnd = r, bottomStart = 0.dp, bottomEnd = 0.dp)
        else -> RoundedCornerShape(r)
    }
    val hasBackgroundImage = !backgroundImageUrl.isNullOrBlank()
    val titleColor = if (hasBackgroundImage) Color.White else colorScheme.skydownText()
    val subtitleColor = if (hasBackgroundImage) {
        Color.White.copy(alpha = 0.84f)
    } else {
        colorScheme.skydownSecondaryText().copy(alpha = 0.96f)
    }
    val detailColor = if (hasBackgroundImage) {
        Color.White.copy(alpha = 0.86f)
    } else {
        colorScheme.skydownText().copy(alpha = 0.86f)
    }
    val contentHorizontalPadding = if (autoCompactDensity) 16.dp else SkydownUiTokens.heroPadding
    val contentVerticalPadding = if (autoCompactDensity) 15.dp else SkydownUiTokens.heroPadding + 1.dp
    val contentSpacing = if (autoCompactDensity) 11.dp else 14.dp
    val eyebrowStyle = SkydownHeroEyebrowTextStyle
    val titleStyle = if (autoCompactDensity) SkydownCompactHeroTitleTextStyle else SkydownHeroTitleTextStyle
    val subtitleStyle = SkydownHeroSubtitleTextStyle
    val detailStyle = SkydownEditorialCaptionTextStyle
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val shouldShowEyebrow = eyebrow.isNotBlank() && !title.startsWith(eyebrow, ignoreCase = true)
    val normalizedSubtitle = subtitle.normalizedHeroComparisonText()
    val normalizedDetail = detail?.normalizedHeroComparisonText().orEmpty()
    val shouldShowDetail = !detail.isNullOrBlank() &&
        normalizedDetail.isNotEmpty() &&
        normalizedDetail != normalizedSubtitle
    val heroTextShadow = if (hasBackgroundImage) {
        androidx.compose.ui.graphics.Shadow(
            color = Color.Black.copy(alpha = if (isDarkPalette) 0.34f else 0.28f),
            offset = Offset(0f, 8f),
            blurRadius = 22f,
        )
    } else {
        null
    }
    val surfaceHeaderInteraction = remember { MutableInteractionSource() }
    val backgroundImageModifier = if (topContentPadding > 0.dp) {
        Modifier.padding(top = topContentPadding)
    } else {
        Modifier
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        val stageModifier = Modifier.fillMaxWidth()

        Box(
            modifier = stageModifier
                .then(
                    if (immersive) {
                        Modifier
                    } else {
                        Modifier.shadow(
                            elevation = when {
                                edgeToEdge -> if (autoCompactDensity) 7.dp else 9.dp
                                autoCompactDensity -> 13.dp
                                else -> 18.dp
                            },
                            shape = shape,
                            ambientColor = secondaryAccent.copy(alpha = if (edgeToEdge) 0.025f else if (isDarkPalette) 0.04f else 0.07f),
                            spotColor = Color.Black.copy(alpha = if (edgeToEdge) 0.08f else if (isDarkPalette) 0.12f else 0.14f),
                        )
                    },
                )
                .clip(shape)
                .drawWithContent {
                    val s = when {
                        immersive -> 0.74f
                        edgeToEdge -> 0.86f
                        else -> 1f
                    }
                    val baseBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (if (isDarkPalette) 0.16f else 0.28f) * s),
                            colorScheme.skydownCardBackground().copy(
                                alpha = (if (isDarkPalette) 0.99f else 0.975f) * s,
                            ),
                            colorScheme.skydownSecondaryBackground().copy(
                                alpha = (if (isDarkPalette) 0.36f else 0.28f) * s,
                            ),
                            accent.copy(alpha = 0.05f * s),
                            secondaryAccent.copy(alpha = 0.04f * s),
                            Color.Black.copy(alpha = (if (isDarkPalette) 0.015f else 0.010f) * s),
                        ),
                        start = Offset(size.width * 0.08f, 0f),
                        end = Offset(size.width, size.height),
                    )
                    val upperBloom = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (if (isDarkPalette) 0.07f else 0.14f) * s),
                            accent.copy(alpha = (if (isDarkPalette) 0.08f else 0.10f) * s),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.16f, size.height * 0.12f),
                        radius = size.width * 0.94f,
                    )
                    val lowerMist = Brush.radialGradient(
                        colors = listOf(
                            secondaryAccent.copy(alpha = (if (isDarkPalette) 0.06f else 0.08f) * s),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.86f, size.height * 0.84f),
                        radius = size.width * 1.08f,
                    )

                    drawRect(baseBrush)
                    drawRect(lowerMist)
                    drawRect(upperBloom, blendMode = BlendMode.Screen)
                    drawContent()
                }
                .then(
                    if (immersive) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = if (edgeToEdge) 0.55.dp else 0.9.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (edgeToEdge) 0.10f else if (isDarkPalette) 0.18f else 0.24f),
                                    accent.copy(alpha = if (edgeToEdge) 0.055f else 0.12f),
                                    secondaryAccent.copy(alpha = if (edgeToEdge) 0.04f else if (isDarkPalette) 0.05f else 0.08f),
                                ),
                            ),
                            shape = shape,
                        )
                    },
                )
                .then(
                    if (immersive) {
                        Modifier.skydownSheen(
                            accent = MaterialTheme.colorScheme.tertiary,
                            alpha = if (isDarkPalette) 0.012f else 0.010f,
                        )
                    } else {
                        Modifier.skydownSheen(
                            accent = MaterialTheme.colorScheme.tertiary,
                            alpha = if (isDarkPalette) {
                                if (edgeToEdge) 0.020f else 0.034f
                            } else {
                                if (edgeToEdge) 0.018f else 0.032f
                            },
                        )
                    },
                ),
        ) {
            if (hasBackgroundImage) {
                AsyncImage(
                    model = backgroundImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .then(backgroundImageModifier)
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.08f),
                                accent.copy(alpha = if (isDarkPalette) 0.08f else 0.07f),
                                secondaryAccent.copy(alpha = if (isDarkPalette) 0.06f else 0.05f),
                            ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = if (isDarkPalette) 0.10f else 0.08f)),
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDarkPalette) 0.08f else 0.14f),
                                accent.copy(alpha = if (isDarkPalette) 0.08f else 0.10f),
                                Color.Transparent,
                            ),
                            center = Offset.Zero,
                            radius = 980f,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = contentHorizontalPadding)
                .padding(top = contentVerticalPadding + topContentPadding, bottom = contentVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (autoCompactDensity) 58.dp else 84.dp)
                        .background(accent.copy(alpha = if (isDarkPalette) 0.03f else 0.05f), CircleShape)
                        .blur(if (autoCompactDensity) 12.dp else 20.dp),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(if (autoCompactDensity) 42.dp else 58.dp)
                        .background(secondaryAccent.copy(alpha = if (isDarkPalette) 0.03f else 0.05f), CircleShape)
                        .blur(if (autoCompactDensity) 10.dp else 16.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onSurfaceClick != null) {
                                Modifier
                                    .skydownPressable(surfaceHeaderInteraction)
                                    .clickable(
                                        interactionSource = surfaceHeaderInteraction,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = onSurfaceClick,
                                    )
                            } else {
                                Modifier
                            },
                        ),
                    horizontalArrangement = Arrangement.spacedBy(if (autoCompactDensity) 10.dp else 14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(if (autoCompactDensity) 6.dp else 8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (shouldShowEyebrow) {
                                Text(
                                    text = eyebrow.uppercase(),
                                    style = if (heroTextShadow != null) eyebrowStyle.copy(shadow = heroTextShadow) else eyebrowStyle,
                                    color = accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(if (autoCompactDensity) 34.dp else 46.dp)
                                    .height(3.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkPalette) 0.34f else 0.80f),
                                                accent.copy(alpha = 0.86f),
                                                secondaryAccent.copy(alpha = 0.30f),
                                            ),
                                        ),
                                        shape = RoundedCornerShape(999.dp),
                                    ),
                            )
                        }
                        Text(
                            text = title,
                            style = if (heroTextShadow != null) titleStyle.copy(shadow = heroTextShadow) else titleStyle,
                            color = titleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = if (heroTextShadow != null) {
                                subtitleStyle.copy(shadow = heroTextShadow.copy(blurRadius = 18f, offset = Offset(0f, 6f)))
                            } else {
                                subtitleStyle
                            },
                            color = subtitleColor,
                            maxLines = if (autoCompactDensity) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (shouldShowDetail) {
                            Text(
                                text = detail,
                                style = if (heroTextShadow != null) {
                                    detailStyle.copy(shadow = heroTextShadow.copy(blurRadius = 16f, offset = Offset(0f, 5f)))
                                } else {
                                    detailStyle
                                },
                                color = detailColor,
                                maxLines = if (autoCompactDensity) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (marks.isNotEmpty() && !autoCompactDensity) {
                        Column(
                            modifier = Modifier
                                .width(
                                    if (marks.size == 1) 118.dp else 96.dp,
                                ),
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

            Column(modifier = Modifier.padding(top = 4.dp)) {
                footer()
            }
        }
    }
    }
}

private fun String.normalizedHeroComparisonText(): String {
    return lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
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
    val resolvedAccent = if (isActive) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
    Column(
        modifier = modifier
            .skydownPanelSurface(
                accent = resolvedAccent,
                cornerRadius = 18.dp,
                shadowRadius = 10.dp,
                shadowYOffset = 5.dp,
            )
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(resolvedAccent.copy(alpha = if (isActive) 0.14f else 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    modifier = Modifier.size(12.dp),
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
            style = MaterialTheme.typography.labelLarge,
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
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .skydownPanelSurface(
                        accent = accent,
                        cornerRadius = 15.dp,
                        shadowRadius = 6.dp,
                        shadowYOffset = 3.dp,
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = SkydownCardTitleTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let { copy ->
                Text(
                    text = copy,
                    style = SkydownBodyCaptionTextStyle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        tag?.takeIf { it.isNotBlank() }?.let { label ->
            Box(
                modifier = Modifier
                    .skydownCapsuleSurface(accent = accent)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
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
    val interactive = onClick != null
    val clickHandler = onClick
    val interactionSource = remember { MutableInteractionSource() }
    val chipModifier = if (clickHandler != null) {
        modifier
            .skydownPressable(
                interactionSource = interactionSource,
                pressedScale = 0.992f,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = clickHandler,
            )
    } else {
        modifier
    }

    Row(
        modifier = chipModifier
            .skydownCapsuleSurface(
                accent = if (isActive) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
            )
            .padding(horizontal = 13.dp, vertical = 9.dp),
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
        if (interactive) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.82f),
                    modifier = Modifier.size(11.dp),
                )
            }
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
    val shape = RoundedCornerShape(if (compact) 15.dp else 18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val buttonEnabled = enabled && !isLoading
    val contentPadding = if (compact) {
        PaddingValues(horizontal = 14.dp, vertical = 9.dp)
    } else {
        PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    }
    val filledContentColor = if (accent.perceivedBrightness() < 0.58f) Color.White else MaterialTheme.colorScheme.skydownDeepInk()
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
                .heightIn(min = if (compact) 40.dp else 46.dp)
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
                .heightIn(min = if (compact) 40.dp else 46.dp)
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
    val interactive = onClick != null
    val clickHandler = onClick
    val interactionSource = remember { MutableInteractionSource() }
    val pillModifier = if (clickHandler != null) {
        Modifier
            .skydownPressable(
                interactionSource = interactionSource,
                pressedScale = 0.993f,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = clickHandler,
            )
    } else {
        Modifier
    }

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
            .padding(
                horizontal = if (interactive) 14.dp else 12.dp,
                vertical = 8.dp,
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (interactive) {
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
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
    val label = stringResource(id = mark.labelRes)
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isFeatured) 124.dp else 98.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
                ambientColor = accent.copy(alpha = if (isDarkPalette) 0.16f else 0.08f),
                spotColor = accent.copy(alpha = if (isDarkPalette) 0.16f else 0.08f),
            )
            .clip(RoundedCornerShape(SkydownUiTokens.cardCornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDarkPalette) 0.08f else 0.40f),
                        accent.copy(alpha = if (isDarkPalette) 0.20f else 0.10f),
                        colorScheme.skydownSecondaryBackground().copy(alpha = if (isDarkPalette) 0.86f else 0.98f),
                        Color.Black.copy(alpha = if (isDarkPalette) 0.24f else 0.04f),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.26f),
                        Color.White.copy(alpha = if (isDarkPalette) 0.10f else 0.24f),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
            )
            .padding(if (isFeatured) 11.dp else 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(id = mark.drawableRes),
                contentDescription = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFeatured) 76.dp else 52.dp)
                    .clip(RoundedCornerShape(SkydownUiTokens.buttonCornerRadius)),
                contentScale = ContentScale.Fit,
            )

            Text(
                text = label,
                style = if (isFeatured) SkydownEditorialCaptionTextStyle else MaterialTheme.typography.labelSmall,
                color = colorScheme.skydownText().copy(alpha = 0.88f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
