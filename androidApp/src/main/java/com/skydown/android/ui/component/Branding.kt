package com.skydown.android.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.skydown.android.R

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
        label = "Zweizwei",
    ),
    Combined(
        drawableRes = R.drawable.skydown_x22_brand_logo,
        label = "Sky²²",
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
    val hasHeaderImage = !backgroundImageUrl.isNullOrBlank()

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
            )
            .skydownSheen(accent = accent, alpha = 0.14f),
    ) {
        Column(
            modifier = Modifier.padding(SkydownUiTokens.heroPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (hasHeaderImage) {
                BrandHeaderImagePanel(
                    imageUrl = backgroundImageUrl.orEmpty(),
                    accent = accent,
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(128.dp)
                        .blur(34.dp)
                        .background(accent.copy(alpha = 0.05f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(96.dp)
                        .blur(30.dp)
                        .background(secondaryAccent.copy(alpha = 0.05f), CircleShape),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                        )
                        if (!detail.isNullOrBlank()) {
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    when {
                        !hasHeaderImage && marks.isNotEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .width(if (marks.size == 1) 118.dp else 96.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
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
            }

            footer()
        }
    }
}

@Composable
private fun BrandHeaderImagePanel(
    imageUrl: String,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(SkydownUiTokens.cardCornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                        accent.copy(alpha = 0.10f),
                        Color.Black.copy(alpha = 0.20f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
            )
            .padding(10.dp),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .align(Alignment.Center),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun BrandPill(
    text: String,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = tint.copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BrandArtworkTile(
    mark: BrandArtwork,
    accent: Color,
    isFeatured: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(id = mark.drawableRes),
            contentDescription = mark.label,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFeatured) 84.dp else 56.dp)
                .clip(RoundedCornerShape(SkydownUiTokens.buttonCornerRadius)),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = mark.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
