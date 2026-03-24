package com.skydown.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = SkyLight,
    secondary = MysticLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceLight,
    onPrimary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight,
)

private val DarkColors = darkColorScheme(
    primary = SkyDark,
    secondary = MysticDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDark,
    onPrimary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
)

@Composable
fun SkydownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
