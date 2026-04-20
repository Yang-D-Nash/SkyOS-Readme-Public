package com.skydown.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.skydown.android.R

private val SyneTitleFontFamily = FontFamily(
    Font(R.font.syne_regular, FontWeight.Normal),
    Font(R.font.syne_medium, FontWeight.Medium),
    Font(R.font.syne_semibold, FontWeight.SemiBold),
    Font(R.font.syne_bold, FontWeight.Bold),
    Font(R.font.syne_extrabold, FontWeight.ExtraBold),
    Font(R.font.syne_extrabold, FontWeight.Black),
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 31.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.7.sp,
    ),
)
