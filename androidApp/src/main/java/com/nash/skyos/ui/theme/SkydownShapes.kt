package com.nash.skyos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material3 shapes aligned with [com.nash.skyos.ui.component.SkydownUiTokens] —
 * quiet radii on a 4dp grid, no generic “fully rounded” medium slots.
 */
val SkydownShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
