package com.ardeno.clearscan.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class ClearScanHaptic {
    LightTap,
    Selection,
    Confirm,
    Reject
}

@Composable
fun rememberClearScanHaptics(): (ClearScanHaptic) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { type: ClearScanHaptic ->
            val feedback = when (type) {
                ClearScanHaptic.LightTap -> HapticFeedbackConstants.KEYBOARD_TAP
                ClearScanHaptic.Selection -> HapticFeedbackConstants.CLOCK_TICK
                ClearScanHaptic.Confirm -> HapticFeedbackConstants.CONFIRM
                ClearScanHaptic.Reject -> HapticFeedbackConstants.REJECT
            }
            view.performHapticFeedback(feedback)
        }
    }
}
