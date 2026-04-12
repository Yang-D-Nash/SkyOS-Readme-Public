package com.skydown.android.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

private const val COMPACT_HEIGHT_THRESHOLD_DP = 760f
private const val COMPACT_WIDTH_THRESHOLD_DP = 392f

@Composable
fun rememberIsCompactHeightLayout(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val heightDp = with(density) { windowInfo.containerSize.height.toDp().value }
    return heightDp <= COMPACT_HEIGHT_THRESHOLD_DP
}

@Composable
fun rememberIsCompactWidthLayout(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp().value }
    return widthDp <= COMPACT_WIDTH_THRESHOLD_DP
}

@Composable
fun rememberIsCompactAppLayout(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp().value }
    val heightDp = with(density) { windowInfo.containerSize.height.toDp().value }
    return heightDp <= COMPACT_HEIGHT_THRESHOLD_DP || widthDp <= COMPACT_WIDTH_THRESHOLD_DP
}
