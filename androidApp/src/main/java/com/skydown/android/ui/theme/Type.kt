package com.skydown.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.skydown.android.R

private val AwergyFontFamily = FontFamily(
    Font(R.font.awergy_regular, FontWeight.Normal),
    Font(R.font.awergy_regular, FontWeight.Medium),
    Font(R.font.awergy_regular, FontWeight.SemiBold),
    Font(R.font.awergy_regular, FontWeight.Bold),
    Font(R.font.awergy_regular, FontWeight.ExtraBold),
    Font(R.font.awergy_regular, FontWeight.Black),
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = AwergyFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = AwergyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = AwergyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = AwergyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = AwergyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AwergyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = AwergyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    ),
)
