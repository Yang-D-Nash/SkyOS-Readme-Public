package com.skydown.android.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun rememberIsCompactHeightLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp <= 760
}

@Composable
fun rememberIsCompactWidthLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp <= 392
}

@Composable
fun rememberIsCompactAppLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp <= 760 || configuration.screenWidthDp <= 392
}
