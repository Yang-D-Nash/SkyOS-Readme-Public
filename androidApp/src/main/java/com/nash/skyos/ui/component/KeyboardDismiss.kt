package com.nash.skyos.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.dismissKeyboardOnTap(
    onDismissKeyboard: () -> Unit,
): Modifier = pointerInput(onDismissKeyboard) {
    awaitEachGesture {
        awaitFirstDown(pass = PointerEventPass.Initial)
        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
        if (up != null) {
            onDismissKeyboard()
        }
    }
}
