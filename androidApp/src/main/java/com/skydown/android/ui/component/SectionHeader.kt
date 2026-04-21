package com.skydown.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skydown.android.ui.theme.SkydownBodyCaptionTextStyle
import com.skydown.android.ui.theme.SkydownHeroEyebrowTextStyle
import com.skydown.android.ui.theme.skydownAccent
import com.skydown.android.ui.theme.skydownIsDarkPalette
import com.skydown.android.ui.theme.skydownSecondaryText
import com.skydown.android.ui.theme.skydownText

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val resolvedAccent = accent ?: colorScheme.skydownAccent()

    Row(
        modifier = modifier.padding(top = 2.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (subtitle.isNullOrBlank()) 24.dp else 36.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            resolvedAccent.copy(alpha = if (isDarkPalette) 0.92f else 0.78f),
                            resolvedAccent.copy(alpha = if (isDarkPalette) 0.28f else 0.20f),
                        ),
                    ),
                    RoundedCornerShape(999.dp),
                ),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(if (subtitle.isNullOrBlank()) 0.dp else 3.dp),
        ) {
            Text(
                text = title.uppercase(),
                style = SkydownHeroEyebrowTextStyle,
                color = colorScheme.skydownText().copy(alpha = 0.94f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            subtitle?.takeIf { it.isNotBlank() }?.let { copy ->
                Text(
                    text = copy,
                    style = SkydownBodyCaptionTextStyle,
                    color = colorScheme.skydownSecondaryText().copy(alpha = 0.86f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
