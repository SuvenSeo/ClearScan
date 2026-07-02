package com.ardeno.clearscan.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Haptic feedback types used throughout ClearScan.
 *
 * Maps to [HapticFeedbackConstants] backed by [android.view.View.performHapticFeedback].
 * Compose's built-in [HapticFeedback][androidx.compose.ui.hapticfeedback.HapticFeedback]
 * only exposes LongPress and TextHandleMove, so the View API is used for richer
 * feedback granularity.
 */
enum class ClearScanHaptic {
    /** Subtle tap for press-down feedback (e.g. animated clickable). */
    LightTap,

    /** Short tick for toggle / selection changes. */
    Selection,

    /** Positive confirmation (e.g. action completed). */
    Confirm,

    /** Negative confirmation (e.g. swipe-to-delete). */
    Reject
}

/**
 * Remember a lambda that performs a [ClearScanHaptic] using the platform haptic API.
 *
 * Usage:
 * ```
 * val haptic = rememberClearScanHaptics()
 * haptic(ClearScanHaptic.LightTap)
 * ```
 */
@Composable
fun rememberClearScanHaptics(): (ClearScanHaptic) -> Unit {
    val view = LocalView.current
    return remember(view) {
        haptic@{ type: ClearScanHaptic ->
            if (!view.isAttachedToWindow) return@haptic

            val feedbackConstant = type.toHapticFeedbackConstant()
            view.performHapticFeedback(feedbackConstant)
        }
    }
}

private fun ClearScanHaptic.toHapticFeedbackConstant(): Int = when (this) {
    ClearScanHaptic.LightTap -> HapticFeedbackConstants.KEYBOARD_TAP
    ClearScanHaptic.Selection -> HapticFeedbackConstants.CLOCK_TICK
    ClearScanHaptic.Confirm -> HapticFeedbackConstants.CONFIRM
    ClearScanHaptic.Reject -> HapticFeedbackConstants.REJECT
}
