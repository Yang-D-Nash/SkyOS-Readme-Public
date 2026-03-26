package com.skydown.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = SkyLight,
    primaryContainer = SkyLightContainer,
    onPrimaryContainer = TextLight,
    secondary = MysticLight,
    secondaryContainer = MysticLightContainer,
    onSecondaryContainer = TextLight,
    tertiary = AuroraLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceLight,
    onTertiary = Color.White,
    onPrimary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight,
)

private val DarkColors = darkColorScheme(
    primary = SkyDark,
    primaryContainer = SkyDarkContainer,
    onPrimaryContainer = TextDark,
    secondary = MysticDark,
    secondaryContainer = MysticDarkContainer,
    onSecondaryContainer = TextDark,
    tertiary = AuroraDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDark,
    onTertiary = BackgroundDark,
    onPrimary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
)

@Composable
fun SkydownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
