package com.nash.skyos.ui.component

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

enum class SkydownHapticKind {
    Press,
    Selection,
    Success,
    Error,
    Warning,
    Info,
}

fun View.performSkydownHaptic(kind: SkydownHapticKind) {
    val feedbackConstant = when (kind) {
        SkydownHapticKind.Press -> HapticFeedbackConstants.KEYBOARD_TAP
        SkydownHapticKind.Selection -> HapticFeedbackConstants.CLOCK_TICK
        SkydownHapticKind.Success -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
        }
        SkydownHapticKind.Error -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
        }
        SkydownHapticKind.Warning -> HapticFeedbackConstants.LONG_PRESS
        SkydownHapticKind.Info -> HapticFeedbackConstants.CONTEXT_CLICK
    }

    performHapticFeedback(feedbackConstant)
}

@Composable
@Stable
fun <T> Modifier.skydownSelectionFeedback(
    trigger: T,
    kind: SkydownHapticKind = SkydownHapticKind.Selection,
): Modifier = composed {
    val view = LocalView.current
    var hasSeenInitialValue by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (hasSeenInitialValue) {
            view.performSkydownHaptic(kind)
        } else {
            hasSeenInitialValue = true
        }
    }

    this
}
