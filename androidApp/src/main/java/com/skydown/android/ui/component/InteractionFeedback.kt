package com.skydown.android.ui.component

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

enum class SkydownHapticKind {
    Press,
    Success,
    Error,
    Warning,
    Info,
}

fun View.performSkydownHaptic(kind: SkydownHapticKind) {
    val feedbackConstant = when (kind) {
        SkydownHapticKind.Press -> HapticFeedbackConstants.KEYBOARD_TAP
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
        SkydownHapticKind.Info -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                HapticFeedbackConstants.CONTEXT_CLICK
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
        }
    }

    performHapticFeedback(feedbackConstant)
}
