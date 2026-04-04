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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextOverflow
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
            )
            .skydownSheen(accent = accent, alpha = 0.14f),
    ) {
        if (hasBackgroundImage) {
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
                            style = MaterialTheme.typography.titleLarge,
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
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
